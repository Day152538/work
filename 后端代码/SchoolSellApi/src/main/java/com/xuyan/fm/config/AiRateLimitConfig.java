package com.xuyan.fm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 各功能独立的限流器 Bean。
 *
 * <p>AiRateLimiter 是 @Component（默认 bean 名 aiRateLimiter，供 预填/合规 共用），
 * 这里再注册一个配额更宽松的 aiChatRateLimiter 给对话助手，
 * 避免用户聊几句就把发布预填的每分钟 10 次额度耗尽。
 */
@Configuration
public class AiRateLimitConfig {

    @Bean
    public AiRateLimiter aiChatRateLimiter(@Value("${ai.chat-rate-limit-per-minute:30}") int maxPerMinute) {
        return new AiRateLimiter(maxPerMinute);
    }
}
