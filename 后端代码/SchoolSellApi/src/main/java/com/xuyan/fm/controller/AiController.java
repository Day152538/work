package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.dto.AiChatRequest;
import com.xuyan.fm.dto.AiChatResponse;
import com.xuyan.fm.dto.AiComplianceRequest;
import com.xuyan.fm.dto.AiComplianceResponse;
import com.xuyan.fm.dto.AiPrefillRequest;
import com.xuyan.fm.dto.AiPrefillResponse;
import com.xuyan.fm.model.AiChatMessage;
import com.xuyan.fm.service.AiChatService;
import com.xuyan.fm.service.AiComplianceService;
import com.xuyan.fm.service.AiPrefillService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 能力控制器。
 *
 * 鉴权说明：路径 /ai/** 不在 WebMvcConfig 白名单中，AuthInterceptor 会先校验 JWT，
 * 未登录请求直接 401；登录后 userId 从 UserContext 读取（服务端取，不信任前端）。
 *
 * 三个能力都是“AI 辅助”：预填 / 合规自检 / 对话查询，均不代替用户做最终决策，
 * 发布主流程 /idle/add 未做任何改动。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiPrefillService aiPrefillService;
    private final AiComplianceService aiComplianceService;
    private final AiChatService aiChatService;

    public AiController(AiPrefillService aiPrefillService,
                        AiComplianceService aiComplianceService,
                        AiChatService aiChatService) {
        this.aiPrefillService = aiPrefillService;
        this.aiComplianceService = aiComplianceService;
        this.aiChatService = aiChatService;
    }

    /**
     * 识图预填 / 按意见精修：{ title, labelId, imageNames[], instructions?, draft? }
     * → { name, details, labelId, suggestPrice, priceReason }
     */
    @PostMapping("/prefill")
    public ResultVo prefill(@RequestBody AiPrefillRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorMsg.MISSING_PARAMETER);
        }
        AiPrefillResponse result = aiPrefillService.prefill(UserContext.getUserId(), request);
        return ResultVo.success(result);
    }

    /**
     * 发布前合规质检：{ title, details, imageNames[] } → { riskLevel, reasons[] }
     */
    @PostMapping("/compliance")
    public ResultVo compliance(@RequestBody AiComplianceRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorMsg.MISSING_PARAMETER);
        }
        AiComplianceResponse result = aiComplianceService.check(UserContext.getUserId(), request);
        return ResultVo.success(result);
    }

    /**
     * AI 对话助手（持久会话 + 平台工具 + 可选联网）：{ message, conversationId?, webSearch? } → { reply, conversationId }
     */
    @PostMapping("/chat")
    public ResultVo chat(@RequestBody AiChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorMsg.MISSING_PARAMETER);
        }
        AiChatResponse result = aiChatService.chat(UserContext.getUserId(), request);
        return ResultVo.success(result);
    }

    /**
     * AI 对话助手（流式 SSE 版）：{ message, conversationId?, webSearch? } → text/event-stream。
     * 事件：delta（文本增量）/ done（{"conversationId":"..."}）/ error（错误文本）。
     * 未登录/缺参数/未配置 Key 等校验失败在创建 SseEmitter 前抛出，返回 JSON 而非 SSE。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorMsg.MISSING_PARAMETER);
        }
        return aiChatService.chatStream(UserContext.getUserId(), request);
    }

    /**
     * 取某会话历史（升序，仅本人可见），供“刷新页面/换设备后恢复对话”使用。
     */
    @GetMapping("/chat/history")
    public ResultVo chatHistory(@RequestParam("conversationId") String conversationId,
                                @RequestParam(value = "limit", required = false, defaultValue = "20") int limit) {
        List<AiChatMessage> history = aiChatService.history(UserContext.getUserId(), conversationId, limit);
        return ResultVo.success(history);
    }
}
