package com.xuyan.fm.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.common.utils.PromptInjectionGuard;
import com.xuyan.fm.config.AiRateLimiter;
import com.xuyan.fm.dao.AiChatDao;
import com.xuyan.fm.dto.AiChatRequest;
import com.xuyan.fm.dto.AiChatResponse;
import com.xuyan.fm.model.AiChatMessage;
import com.xuyan.fm.model.AiUserMemory;
import com.xuyan.fm.model.KnowledgeChunk;
import com.xuyan.fm.service.AiChatService;
import com.xuyan.fm.service.KnowledgeService;
import com.xuyan.fm.service.tool.AiChatTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 对话助手实现（M0 持久会话 + M1 长期记忆）。
 *
 * <p>流程：写库(用户消息) → 从库按会话取最近历史重建上下文 → 注入该用户长期画像 →
 * 模型生成(可调用平台工具/联网) → 回复写库 → 消息达到阈值时增量摘要用户画像入库。
 *
 * <p>归属安全：所有读消息都带 conversation_id + user_id 双重条件；会话 id 由服务端签发/复用。
 */
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /** 每次重建上下文携带的最近历史条数（硬上限，防止极端情况） */
    private static final int CONTEXT_LIMIT = 20;
    /** 历史消息 token 阈值：超过此值触发中间历史摘要压缩（给系统提示词和工具结果留空间） */
    private static final int HISTORY_TOKEN_THRESHOLD = 2000;
    /** 压缩后保留的最近消息条数（最相关的上下文不压缩） */
    private static final int KEEP_RECENT_COUNT = 5;
    /** 一次增量摘要最多喂给模型的旧消息数 */
    private static final int MEMORY_FETCH_LIMIT = 60;
    /** 单次对话中工具调用的最大轮数（防模型反复调工具导致死循环/费用失控） */
    private static final int MAX_TOOL_ROUNDS = 6;
    /** 流式推送的超时时间（毫秒）：qwen-max 联网场景较慢，给足 3 分钟 */
    private static final long STREAM_TIMEOUT_MS = 180_000L;
    /** 流式对话共享执行器：限流已把每用户频率压住，4 个并发足够，超出的排队 */
    private static final ExecutorService STREAM_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "ai-chat-stream");
        t.setDaemon(true);
        return t;
    });

    private final String apiKey;
    private final String chatModel;
    private final String fallbackModel;
    private final boolean webSearchFallback;
    private final int summarizeAfter;
    private final ChatClient chatClient;
    private final AiChatTool aiChatTool;
    private final AiRateLimiter chatRateLimiter;
    private final AiChatDao aiChatDao;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    public AiChatServiceImpl(@Value("${spring.ai.openai.api-key:}") String apiKey,
                             @Value("${ai.chat-model:qwen-plus}") String chatModel,
                             @Value("${ai.fallback-model:qwen-plus}") String fallbackModel,
                             @Value("${ai.web-search.dashscope:false}") boolean webSearchFallback,
                             @Value("${ai.memory.summarize-after:8}") int summarizeAfter,
                             ChatClient.Builder chatClientBuilder,
                             AiChatTool aiChatTool,
                             @Qualifier("aiChatRateLimiter") AiRateLimiter chatRateLimiter,
                             AiChatDao aiChatDao,
                             KnowledgeService knowledgeService,
                             ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.fallbackModel = fallbackModel;
        this.webSearchFallback = webSearchFallback;
        this.summarizeAfter = Math.max(1, summarizeAfter);
        this.chatClient = chatClientBuilder.build();
        this.aiChatTool = aiChatTool;
        this.chatRateLimiter = chatRateLimiter;
        this.aiChatDao = aiChatDao;
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiChatResponse chat(Long userId, AiChatRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorMsg.COOKIE_ERROR);
        }
        chatRateLimiter.check(userId);

        String message = request == null || request.getMessage() == null
                ? "" : request.getMessage().trim();
        if (message.isEmpty()) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }
        // Prompt 注入防护：检测到试图覆盖系统指令/角色扮演/泄露提示词的输入，直接拒绝
        if (PromptInjectionGuard.isSuspicious(message)) {
            log.warn("AI 对话检测到 Prompt 注入嫌疑|userId={}|pattern={}|message={}",
                    userId, PromptInjectionGuard.detectPattern(message),
                    message.length() > 100 ? message.substring(0, 100) + "..." : message);
            return new AiChatResponse("检测到可能的指令注入，已拒绝该请求。请用正常方式提问～",
                    normalizeConversationId(request.getConversationId()));
        }
        if (apiKey == null || apiKey.isBlank() || "sk-no-key-configured".equals(apiKey)) {
            throw new BusinessException(ErrorMsg.AI_NOT_CONFIGURED);
        }

        // 会话 id：空则新开会话（服务端签发）；已有则沿用
        String conversationId = normalizeConversationId(request.getConversationId());
        boolean useWeb = request.getWebSearch() != null ? request.getWebSearch() : webSearchFallback;

        // M0：用户消息先落库（得到真实 id，供后续增量摘要边界）
        aiChatDao.insertMessage(new AiChatMessage(conversationId, userId, "user", message));

        // 从库重建上下文（token 感知：超阈值时中间历史摘要压缩，保留最近原文）
        List<Message> messages = buildCompressedContext(userId, conversationId);
        // 读取长期画像摘要注入系统提示词
        String memory = readMemorySummary(userId);
        // RAG：检索平台知识库（公告/规则）相关片段，注入系统提示词，减少幻觉
        List<KnowledgeChunk> knowledge = knowledgeService.retrieve(message, 3);

        String reply;
        try {
            long start = System.currentTimeMillis();
            ChatResponse response = callOnce(messages, memory, knowledge, useWeb, chatModel);
            reply = extractContent(response);
            logUsage("AI 对话", userId, conversationId, messages.size(), start, useWeb, chatModel, response);
        } catch (Exception e) {
            // 模型自动降级：主模型异常时用降级模型重试一次（避免 qwen-max 偶发 5xx 中断用户体验）
            if (!chatModel.equals(fallbackModel)) {
                try {
                    long start = System.currentTimeMillis();
                    log.warn("AI 对话主模型 {} 失败，降级用 {} 重试: {}", chatModel, fallbackModel, e.getMessage());
                    ChatResponse response = callOnce(messages, memory, knowledge, useWeb, fallbackModel);
                    reply = extractContent(response);
                    logUsage("AI 对话(降级)", userId, conversationId, messages.size(), start, useWeb, fallbackModel, response);
                } catch (Exception e2) {
                    log.warn("AI 对话降级模型 {} 也失败: {}", fallbackModel, e2.getMessage());
                    throw new BusinessException(ErrorMsg.AI_SERVICE_ERROR);
                }
            } else {
                log.warn("AI 对话调用失败: {}", e.getMessage());
                throw new BusinessException(ErrorMsg.AI_SERVICE_ERROR);
            }
        }

        // 回复落库
        aiChatDao.insertMessage(new AiChatMessage(conversationId, userId, "assistant", reply));

        // M1：达到阈值则增量更新用户长期画像
        maybeSummarizeUserMemory(userId);

        return new AiChatResponse(reply, conversationId);
    }

    // ==================== 流式对话（SSE） ====================

    @Override
    public SseEmitter chatStream(Long userId, AiChatRequest request) {
        // —— 同步校验：失败会抛 BusinessException，由全局异常处理器返回 JSON（非 SSE），
        //    前端 fetch 需区分 text/event-stream 与 JSON 两种响应 ——
        if (userId == null) {
            throw new BusinessException(ErrorMsg.COOKIE_ERROR);
        }
        chatRateLimiter.check(userId);

        String message = request == null || request.getMessage() == null
                ? "" : request.getMessage().trim();
        if (message.isEmpty()) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }
        // Prompt 注入防护（与 chat() 一致）
        if (PromptInjectionGuard.isSuspicious(message)) {
            log.warn("AI 流式对话检测到 Prompt 注入嫌疑|userId={}|pattern={}",
                    userId, PromptInjectionGuard.detectPattern(message));
            throw new BusinessException("检测到可能的指令注入，已拒绝该请求");
        }
        if (apiKey == null || apiKey.isBlank() || "sk-no-key-configured".equals(apiKey)) {
            throw new BusinessException(ErrorMsg.AI_NOT_CONFIGURED);
        }

        String conversationId = normalizeConversationId(request.getConversationId());
        boolean useWeb = request.getWebSearch() != null ? request.getWebSearch() : webSearchFallback;

        // M0：用户消息先落库；随后重建上下文（与 chat() 完全一致，token 感知压缩）
        aiChatDao.insertMessage(new AiChatMessage(conversationId, userId, "user", message));
        List<Message> messages = buildCompressedContext(userId, conversationId);
        String memory = readMemorySummary(userId);
        // RAG：检索平台知识库相关片段
        List<KnowledgeChunk> knowledge = knowledgeService.retrieve(message, 3);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        STREAM_EXECUTOR.execute(() -> {
            // 异步线程没有 AuthInterceptor 写入的上下文，手动恢复（AiChatTool 内部用 UserContext 取 userId）
            UserContext.set(new UserContext.LoginUser(userId, null, UserContext.ROLE_USER));
            try {
                long start = System.currentTimeMillis();
                String fullReply = streamWithTools(messages, memory, knowledge, useWeb, emitter);
                // 完整回复落库（流结束后一次性写，保证历史里是一条完整消息）
                aiChatDao.insertMessage(new AiChatMessage(conversationId, userId, "assistant", fullReply));
                maybeSummarizeUserMemory(userId);
                log.info("AI 流式对话|userId={}|conversation={}|costMs={}|web={}",
                        userId, conversationId, System.currentTimeMillis() - start, useWeb);
                // 结束事件：携带 conversationId，前端据此持久化会话
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"conversationId\":\"" + conversationId + "\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.warn("AI 流式对话失败|userId={}|conversation={}: {}", userId, conversationId, e.getMessage());
                safeError(emitter, "AI 服务异常，请稍后重试");
            } finally {
                UserContext.clear();
            }
        });
        return emitter;
    }

    /**
     * 流式生成正文，并自动处理「模型要求调用平台工具」的循环：
     * 工具调用轮 → 服务端执行工具 → 结果追加进上下文 → 重新请求，直到模型输出纯文本。
     * 文本块逐段通过 SSE delta 事件推给前端。
     */
    private String streamWithTools(List<Message> messages, String memory, List<KnowledgeChunk> knowledge,
                                    boolean useWeb, SseEmitter emitter) throws Exception {
        StringBuilder full = new StringBuilder();
        int rounds = 0;
        int toolCallCount = 0;
        ChatResponse lastResp = null; // 跟踪最后一个响应（用于取 usage 埋点）
        while (rounds++ < MAX_TOOL_ROUNDS) {
            List<AssistantMessage.ToolCall> calls = new ArrayList<>();
            Iterator<ChatResponse> it = chatClient.prompt()
                    .system(buildSystemPrompt(memory, knowledge))
                    .messages(messages)
                    .tools(aiChatTool)
                    .options(buildChatOptions(useWeb))
                    .stream()
                    .chatResponse()
                    .toStream()
                    .iterator();
            while (it.hasNext()) {
                ChatResponse resp = it.next();
                lastResp = resp;
                if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) {
                    continue;
                }
                AssistantMessage am = resp.getResult().getOutput();
                if (am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
                    calls.addAll(am.getToolCalls());
                }
                String text = am.getText();
                if (text != null && !text.isEmpty()) {
                    full.append(text);
                    emitter.send(SseEmitter.event().name("delta").data(text));
                }
            }
            if (calls.isEmpty()) {
                // 纯文本轮：结束，记录 token 埋点
                Usage usage = (lastResp != null && lastResp.getMetadata() != null)
                        ? lastResp.getMetadata().getUsage() : null;
                log.info("AI 流式对话|rounds={}|toolCalls={}|replyLen={}|promptTokens={}|completionTokens={}|totalTokens={}",
                        rounds, toolCallCount, full.length(),
                        usage != null ? usage.getPromptTokens() : null,
                        usage != null ? usage.getCompletionTokens() : null,
                        usage != null ? usage.getTotalTokens() : null);
                return full.toString();
            }
            // 工具调用轮：记录工具调用轨迹（埋点），把 assistant tool-call + 工具结果追加进上下文
            messages.add(AssistantMessage.builder().content("").toolCalls(calls).build());
            for (AssistantMessage.ToolCall call : calls) {
                toolCallCount++;
                String args = call.arguments();
                log.info("AI 工具调用|tool={}|args={}", call.name(),
                        args != null && args.length() > 200 ? args.substring(0, 200) + "..." : args);
                String result = executeTool(call.name(), call.arguments());
                messages.add(ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse(call.id(), call.name(), result)))
                        .build());
            }
        }
        // 工具轮数超限：说明模型一直想调工具，终止并报错（不消耗无底洞）
        throw new BusinessException(ErrorMsg.AI_SERVICE_ERROR);
    }

    /**
     * 按工具名分派执行 AiChatTool 的方法（方法签名固定，switch 比反射更稳、可编译期校验）。
     * 参数从模型返回的 arguments JSON 中按已知参数名取值，类型转换失败按缺省处理。
     */
    private String executeTool(String name, String argumentsJson) throws Exception {
        Map<String, Object> args = parseArgs(argumentsJson);
        switch (name) {
            case "listCategories":
                return aiChatTool.listCategories();
            case "searchOnSaleItems":
                return aiChatTool.searchOnSaleItems(
                        str(args.get("keyword")),
                        intVal(args.get("labelId")),
                        dec(args.get("minPrice")),
                        dec(args.get("maxPrice")));
            case "searchMyPublishedItems":
                return aiChatTool.searchMyPublishedItems();
            case "recentDealStats":
                return aiChatTool.recentDealStats(intVal(args.get("labelId")));
            case "getMyStatistics":
                return aiChatTool.getMyStatistics();
            case "getMyOrders":
                return aiChatTool.getMyOrders();
            case "getNotices":
                return aiChatTool.getNotices();
            case "getMyFavorites":
                return aiChatTool.getMyFavorites();
            default:
                log.warn("AI 工具未知: {}", name);
                return "（未知工具：" + name + "）";
        }
    }

    private Map<String, Object> parseArgs(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("AI 工具参数解析失败: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Integer intVal(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal dec(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 推送错误事件并结束流（尽力而为，失败不影响主流程） */
    private void safeError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(msg));
        } catch (Exception ignore) {
            // 连接可能已断开
        }
        try {
            emitter.complete();
        } catch (Exception ignore) {
            // 已结束
        }
    }

    @Override
    public List<AiChatMessage> history(Long userId, String conversationId, int limit) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        int n = Math.max(1, Math.min(limit, 100));
        List<AiChatMessage> recent = aiChatDao.recentMessages(conversationId, userId, n);
        // recent 是按 id 倒序，翻转为时间升序返回
        List<AiChatMessage> asc = new ArrayList<>(recent.size());
        for (int i = recent.size() - 1; i >= 0; i--) {
            asc.add(recent.get(i));
        }
        return asc;
    }

    // ==================== 对话历史 token 感知压缩 ====================

    /**
     * 构建带 token 感知压缩的上下文消息列表。
     *
     * <p>策略：取最近 CONTEXT_LIMIT 条历史，估算总 token；
     * 若超过 HISTORY_TOKEN_THRESHOLD，则保留最近 KEEP_RECENT_COUNT 条原文，
     * 中间历史用模型摘要压缩成一段注入上下文，从而在有限 token 窗口内保留更长程的对话记忆。
     *
     * <p>与 M1 长期记忆（用户画像）的区别：M1 是跨会话的用户偏好画像，
     * 本方法是单会话内的对话历史摘要，保留具体讨论过的商品/订单等上下文。
     */
    private List<Message> buildCompressedContext(Long userId, String conversationId) {
        List<AiChatMessage> history = history(userId, conversationId, CONTEXT_LIMIT);
        if (history.isEmpty()) {
            return new ArrayList<>();
        }
        // 估算总 token
        int totalTokens = 0;
        for (AiChatMessage m : history) {
            totalTokens += estimateTokens(m.getContent());
        }
        // 未超阈值：全部原文
        if (totalTokens <= HISTORY_TOKEN_THRESHOLD || history.size() <= KEEP_RECENT_COUNT) {
            return toMessages(history);
        }
        // 超阈值：中间历史摘要 + 最近 KEEP_RECENT_COUNT 条原文
        int recentCount = Math.min(KEEP_RECENT_COUNT, history.size());
        int midEnd = history.size() - recentCount;
        List<AiChatMessage> midHistory = history.subList(0, midEnd);
        List<AiChatMessage> recentHistory = history.subList(midEnd, history.size());

        List<Message> messages = new ArrayList<>();
        try {
            String summary = summarizeHistory(midHistory);
            // 摘要作为一条 user 消息注入（标记为历史摘要）
            messages.add(new UserMessage("【 earlier conversation summary 】\n" + summary));
        } catch (Exception e) {
            // 摘要失败：降级为只保留最近原文（不丢上下文）
            log.warn("对话历史摘要失败，降级为仅保留最近 {} 条: {}", recentCount, e.getMessage());
        }
        messages.addAll(toMessages(recentHistory));
        return messages;
    }

    /** 把 AiChatMessage 列表转为 Spring AI Message 列表 */
    private List<Message> toMessages(List<AiChatMessage> history) {
        List<Message> messages = new ArrayList<>(history.size());
        for (AiChatMessage m : history) {
            if ("user".equalsIgnoreCase(m.getRole())) {
                messages.add(new UserMessage(m.getContent()));
            } else {
                messages.add(new AssistantMessage(m.getContent()));
            }
        }
        return messages;
    }

    /** 用模型把中间历史摘要成一段（不接工具/不联网，低温度） */
    private String summarizeHistory(List<AiChatMessage> history) {
        StringBuilder transcript = new StringBuilder();
        for (AiChatMessage m : history) {
            transcript.append(m.getRole()).append(": ")
                    .append(m.getContent() == null ? "" : m.getContent()).append('\n');
        }
        String system = "你是对话历史摘要器。请把下面的对话历史压缩成一段简洁摘要（300 字内），"
                + "保留关键信息：讨论过的商品名称/价格/订单、用户的核心诉求和已达成的结论。"
                + "不要复述无关寒暄，不要添加对话中没有的信息。";
        return chatClient.prompt()
                .system(system)
                .user("请摘要以下对话历史：\n\n" + transcript)
                .options(OpenAiChatOptions.builder().model(chatModel).temperature(0.2).build())
                .call()
                .content();
    }

    /**
     * 粗略估算 token 数（无需精确，用于判断是否触发压缩）。
     * 规则：中文字符约 1.5 字/token，英文/数字约 4 字符/token。
     */
    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int chinese = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chinese++;
            } else {
                other++;
            }
        }
        return (int) (chinese / 1.5 + other / 4.0) + 1;
    }

    // ==================== 长期记忆（M1） ====================

    private String readMemorySummary(Long userId) {
        AiUserMemory memory = aiChatDao.findMemory(userId);
        return memory == null ? null : memory.getSummary();
    }

    /** 当用户新增消息数 >= summarizeAfter 时，用模型把“旧画像 + 这批新消息”压缩成最新画像入库 */
    private void maybeSummarizeUserMemory(Long userId) {
        AiUserMemory memory = aiChatDao.findMemory(userId);
        long fromId = memory == null ? 0L
                : (memory.getSummarizedUptoId() == null ? 0L : memory.getSummarizedUptoId());
        int newCount = aiChatDao.countNewMessages(userId, fromId);
        if (newCount < summarizeAfter) {
            return;
        }
        try {
            List<AiChatMessage> fresh = aiChatDao.newMessages(
                    userId, fromId, Math.min(newCount, MEMORY_FETCH_LIMIT));
            StringBuilder transcript = new StringBuilder();
            for (AiChatMessage m : fresh) {
                transcript.append(m.getRole()).append(": ")
                        .append(m.getContent() == null ? "" : m.getContent()).append('\n');
            }
            String oldSummary = memory == null ? null : memory.getSummary();
            String updated = summarizeMemory(oldSummary, transcript.toString());

            AiUserMemory save = new AiUserMemory();
            save.setUserId(userId);
            save.setSummary(updated);
            save.setSummarizedUptoId(aiChatDao.maxMessageId(userId));
            aiChatDao.upsertMemory(save);
            log.info("AI 长期记忆已更新|userId={}|newMsgs={}", userId, fresh.size());
        } catch (Exception e) {
            // 记忆摘要失败不影响对话，仅记日志
            log.warn("AI 长期记忆更新失败|userId={}: {}", userId, e.getMessage());
        }
    }

    /** 用一次文本调用把“旧画像 + 新对话”合并成最新用户画像（不接工具/不联网） */
    private String summarizeMemory(String oldSummary, String transcript) {
        String system = "你是用户画像整理器。根据用户的聊天内容，提取稳定的偏好/意图："
                + "常关注的商品类别、价格区间、是在卖还是买、常用语气、特殊要求等。"
                + "输出一小段简洁中文画像（200 字内），只保留有长期价值的信息，不要复述单次问答。";
        String user = (oldSummary == null || oldSummary.isBlank() ? "（无旧画像）" : "旧画像：" + oldSummary)
                + "\n\n最近对话：\n" + transcript
                + "\n\n请给出合并后的最新用户画像。";
        return chatClient.prompt()
                .system(system)
                .user(user)
                .options(OpenAiChatOptions.builder().model(chatModel).temperature(0.3).build())
                .call()
                .content();
    }

    // ==================== 提示词 / 参数 ====================

    private String buildSystemPrompt(String memory, List<KnowledgeChunk> knowledge) {
        StringBuilder head = new StringBuilder();
        head.append("今天是").append(currentTimeText())
                .append("。凡是问“今天日期/几号/星期几/现在几点”的问题，一律以上面的当前时间为准回答，不要用你自己的训练知识猜测日期。\n\n");
        if (memory != null && !memory.isBlank()) {
            head.append("关于该用户的长期画像（可能随对话更新，仅供参考）：").append(memory).append("\n\n");
        }
        // RAG：注入检索到的平台知识库片段，要求模型基于真实知识回答并标注来源
        if (knowledge != null && !knowledge.isEmpty()) {
            head.append("【平台知识库检索结果】以下是从平台公告和规则库中检索到的相关内容，请优先基于这些内容回答，并在回答中标注来源（如“根据平台公告”“根据平台规则”）：\n");
            for (KnowledgeChunk k : knowledge) {
                String src = "notice".equals(k.getSourceType()) ? "平台公告" : "平台规则";
                head.append("- [").append(src).append("] ").append(k.getContent()).append("\n");
            }
            head.append("如果检索结果与用户问题无关，请忽略；不要编造检索结果中没有的信息。\n\n");
        }
        return head.toString() + """
                你是"校园二手交易平台"的智能助手，帮助用户了解平台和打理自己的闲置交易。

                你可以调用工具查询平台真实数据：
                - 平台分类、在售商品（关键字/分类/价格区间筛选）、你自己的发布、你的商品统计、你的买卖订单概览、你的收藏、平台公告、某分类的真实成交行情。
                用户问这类问题时，请先调用工具、用真实数据回答；查不到就如实说"平台暂未查到"，严禁编造商品、价格、销量、库存、订单、公告或统计数字。
                你只能查询公开在售数据与"当前登录用户本人"的发布/订单/收藏，不要把别人未公开的信息说成你的数据。
                你只做"查询和解答"，不做任何写操作：发布、下架、上架、改价、删商品、下单、支付等一律由用户在页面操作按钮上确认执行，你只引导用户怎么操作。

                用户问与平台无关的问题时，可以简单用你的常识作答，但不要长篇大论或越界承诺。
                语气友好、简洁；涉及交易提醒当面验货、谨防私下转账诈骗。

                强制规则（防止编造，务必遵守）：
                1. 用户有找货意图（"在卖/在售/有什么手机/便宜点/推荐/帮我查商品"）→ 必须调用 searchOnSaleItems；
                   若用户提到分类或价格范围，应同时传入 labelId（先通过 listCategories 确定分类 id）与 minPrice/maxPrice；
                   只能引用工具返回里出现的名称与价格；若工具返回"没有找到"，直接说"平台暂未查到在售的此类商品"，禁止凭空列举型号或价格。
                2. 用户问"我发布/我上架/我的商品" → 必须调用 searchMyPublishedItems，并按返回逐条告诉用户；返回"还没有发布"就如实说明。
                3. 用户问"xx 最近成交价/行情/能卖多少" → 先通过 listCategories 确定分类 id，再调用 recentDealStats；工具说没有成交样本就如实说没有，不要编区间。
                4. 回答里出现的商品名称、价格、库存、销量数字都必须能在本次工具返回中找到出处；找不到出处就不写。
                5. 用户问"我的统计/卖出多少/成交多少" → 必须调用 getMyStatistics，引用其返回的件数与金额。
                6. 用户问"我的订单/买到的/卖出的" → 必须调用 getMyOrders，只引用返回的笔数。
                7. 用户问"公告/通知/平台消息" → 必须调用 getNotices，逐条转述返回的公告内容。
                8. 用户问"我的收藏" → 必须调用 getMyFavorites，只引用返回的商品名称与价格。
                9. 回答中凡提到平台商品，商品名称后必须紧跟标注"【id=数字】"，数字严格取自工具返回的 id，格式严格为：商品名称【id=数字】，例如"九成新游戏本【id=3】"。禁止省略标注、禁止编造 id、禁止改用其他括号或符号代替。
                10. 用户要求查实时新闻、汇率、天气等联网信息时：若联网搜索未返回可用结果，必须明确回复"联网搜索暂未返回可用结果"，禁止编造带日期、来源或具体数字的新闻与数据。
                """;
    }

    /**
     * 组装聊天参数：文本模型 + 千问内置联网开关（DashScope 的 enable_search=true，按次传入）。
     */
    private OpenAiChatOptions buildChatOptions(boolean useWeb) {
        return buildChatOptions(useWeb, chatModel);
    }

    /** 指定模型的聊天参数（用于降级重试） */
    private OpenAiChatOptions buildChatOptions(boolean useWeb, String model) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.4);
        if (useWeb) {
            Map<String, Object> extraBody = new HashMap<>();
            extraBody.put("enable_search", true);
            builder.extraBody(extraBody);
        }
        return builder.build();
    }

    // ==================== 模型调用 / 埋点 / 降级 ====================

    /** 单次模型调用（非流式，带工具），返回完整 ChatResponse（含 usage 元数据） */
    private ChatResponse callOnce(List<Message> messages, String memory, List<KnowledgeChunk> knowledge,
                                   boolean useWeb, String model) {
        return chatClient.prompt()
                .system(buildSystemPrompt(memory, knowledge))
                .messages(messages)
                .tools(aiChatTool)
                .options(buildChatOptions(useWeb, model))
                .call()
                .chatResponse();
    }

    /** 从 ChatResponse 提取文本内容 */
    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text.trim();
    }

    /** 记录 AI 调用埋点：耗时 + token 用量（prompt/completion/total）+ 模型 + 工具轨迹 */
    private void logUsage(String prefix, Long userId, String conversationId, int msgCount,
                          long startMs, boolean useWeb, String model, ChatResponse response) {
        Usage usage = (response != null && response.getMetadata() != null)
                ? response.getMetadata().getUsage() : null;
        Integer promptTokens = usage != null ? usage.getPromptTokens() : null;
        Integer completionTokens = usage != null ? usage.getCompletionTokens() : null;
        Integer totalTokens = usage != null ? usage.getTotalTokens() : null;
        log.info("{}|userId={}|conversation={}|model={}|msgs={}|costMs={}|web={}|promptTokens={}|completionTokens={}|totalTokens={}",
                prefix, userId, conversationId, model, msgCount,
                System.currentTimeMillis() - startMs, useWeb,
                promptTokens, completionTokens, totalTokens);
    }

    /** 服务器当前时间文本，注入提示词，让模型知道“今天几号/几点”，避免用训练知识瞎猜日期 */
    private String currentTimeText() {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        return date + " " + weekdayText(now.getDayOfWeek()) + " " + time + "（服务器时间）";
    }

    private String weekdayText(DayOfWeek dow) {
        switch (dow) {
            case MONDAY: return "星期一";
            case TUESDAY: return "星期二";
            case WEDNESDAY: return "星期三";
            case THURSDAY: return "星期四";
            case FRIDAY: return "星期五";
            case SATURDAY: return "星期六";
            default: return "星期日";
        }
    }

    private String normalizeConversationId(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String trimmed = raw.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
