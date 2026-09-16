package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.config.AiRateLimiter;
import com.xuyan.fm.dto.AiComplianceRequest;
import com.xuyan.fm.dto.AiComplianceResponse;
import com.xuyan.fm.service.AiComplianceService;
import com.xuyan.fm.service.ai.AiMediaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AI 合规质检实现。
 *
 * 复用与预填相同的通道：JWT 用户 + 限流（与预填共享同一份每用户配额）+ AiMediaLoader 读图 +
 * ChatClient 结构化输出。质检结果再做一次规则兜底，模型故障/网络异常时返回 LOW，
 * 保证“宁可放行让平台人工审核，也不因 AI 故障误伤正常发帖”。
 */
@Service
public class AiComplianceServiceImpl implements AiComplianceService {

    private static final Logger log = LoggerFactory.getLogger(AiComplianceServiceImpl.class);

    /** 规则层禁止词（与模型审查构成双保险；命中直接判 HIGH） */
    private static final List<String> FORBIDDEN_WORDS = List.of(
            "代开发票", "刷单", "外挂", "毒品", "枪支", "违禁品", "办证", "博彩",
            "加微信", "加我微信", "引流", "假货", "原单", "高仿");

    private final String apiKey;
    private final ChatClient chatClient;
    private final AiMediaLoader aiMediaLoader;
    private final AiRateLimiter rateLimiter;

    public AiComplianceServiceImpl(@Value("${spring.ai.openai.api-key:}") String apiKey,
                                   ChatClient.Builder chatClientBuilder,
                                   AiMediaLoader aiMediaLoader,
                                   @Qualifier("aiRateLimiter") AiRateLimiter rateLimiter) {
        this.apiKey = apiKey;
        this.chatClient = chatClientBuilder.build();
        this.aiMediaLoader = aiMediaLoader;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public AiComplianceResponse check(Long userId, AiComplianceRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorMsg.COOKIE_ERROR);
        }
        rateLimiter.check(userId);

        String title = request == null ? "" : trimToEmpty(request.getTitle());
        String details = request == null ? "" : trimToEmpty(request.getDetails());
        List<String> imageNames = request == null || request.getImageNames() == null
                ? List.of()
                : request.getImageNames().stream()
                        .filter(n -> n != null && !n.isBlank())
                        .distinct()
                        .limit(AiMediaLoader.MAX_IMAGES)
                        .toList();
        if (title.isEmpty() && details.isEmpty() && imageNames.isEmpty()) {
            throw new BusinessException(ErrorMsg.PARAM_ERROR);
        }

        // 规则层初筛：命中禁止词直接判 HIGH，不给模型放过的机会
        if (containsForbidden(title) || containsForbidden(details)) {
            return new AiComplianceResponse("HIGH", List.of("文案命中平台禁止用词，请修改后再发布（规则拦截）"));
        }

        if (apiKey == null || apiKey.isBlank() || "sk-no-key-configured".equals(apiKey)) {
            throw new BusinessException(ErrorMsg.AI_NOT_CONFIGURED);
        }

        // 组装多模态消息并调用模型（HTTP 层失败由框架 spring.ai.retry.* 处理）
        List<Media> medias = aiMediaLoader.load(imageNames);
        AiComplianceResponse raw;
        try {
            raw = chatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(u -> {
                        u.text(buildUserText(title, details, !medias.isEmpty()));
                        for (Media media : medias) {
                            u.media(media);
                        }
                    })
                    .call()
                    .entity(AiComplianceResponse.class);
        } catch (Exception e) {
            // 合规质检失败不阻塞发帖：记录并返回“无风险”，由平台审核兜底
            log.warn("AI 合规质检调用失败，按 LOW 放行: {}", e.getMessage());
            return new AiComplianceResponse("LOW", List.of());
        }

        return sanitize(raw);
    }

    private String buildSystemPrompt() {
        return """
                你是"校园二手交易平台"的内容安全审查员，对用户即将发布的一条二手商品信息（可能包含文字与图片）做初审。
                只审查下面这些方面，不要延伸发挥：
                1. 违禁/危险品：毒品、枪支、管制刀具、爆炸物、野生动物制品等；
                2. 假冒伪劣：明显高仿/假货、无授权的品牌代购承诺；
                3. 违规营销：刷单、外挂、代开发票、博彩、站外引流、留个人联系方式诱导线下交易；
                4. 夸大承诺：虚假疗效/绝对化用语/虚构交易记录；
                5. 隐私泄露：图中或文案中出现他人姓名、手机号、证件、住址等可识别私人信息；
                6. 色情低俗、暴力、人身攻击。

                输出要求：
                - riskLevel 只能是 LOW / MEDIUM / HIGH 三者之一：明确违规给 HIGH；疑似但不确定给 MEDIUM；
                  明显正常给 LOW。拿不准一律 LOW，宁可信其正常，再由人工复核，不要误伤普通二手转让。
                - reasons：给到最多 3 条简短原因（每条 20 字内）；LOW 时可给空数组。
                只输出这两个字段，不要解释过程。
                """;
    }

    private String buildUserText(String title, String details, boolean hasImage) {
        StringBuilder sb = new StringBuilder();
        sb.append("【待发布商品标题】").append(title.isEmpty() ? "（空）" : title).append("\n");
        sb.append("【待发布商品描述】").append(details.isEmpty() ? "（空）" : details).append("\n");
        sb.append(hasImage ? "【商品图片】已随本消息附上。" : "【商品图片】无。");
        sb.append("\n请按你的审查规则输出 riskLevel 与 reasons。");
        return sb.toString();
    }

    private AiComplianceResponse sanitize(AiComplianceResponse raw) {
        String level = raw == null || raw.riskLevel() == null ? "LOW"
                : raw.riskLevel().toUpperCase(Locale.ROOT);
        if (!"LOW".equals(level) && !"MEDIUM".equals(level) && !"HIGH".equals(level)) {
            level = "LOW";
        }
        List<String> reasons = new ArrayList<>();
        if (raw != null && raw.reasons() != null) {
            Set<String> seen = new LinkedHashSet<>();
            for (String r : raw.reasons()) {
                if (r == null || r.isBlank()) {
                    continue;
                }
                String clean = r.trim();
                if (clean.length() > 80) {
                    clean = clean.substring(0, 80);
                }
                if (seen.add(clean) && reasons.size() < 3) {
                    reasons.add(clean);
                }
            }
        }
        return new AiComplianceResponse(level, reasons);
    }

    private boolean containsForbidden(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String compact = text.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}\\u3000]+", "");
        for (String word : FORBIDDEN_WORDS) {
            if (compact.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
