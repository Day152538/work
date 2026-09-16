package com.xuyan.fm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 调用方的 HTTP 客户端配置（P0-3）。
 *
 * <p>Spring AI OpenAI 的自动配置（OpenAiChatAutoConfiguration）会通过
 * ObjectProvider&lt;RestClient.Builder&gt; 消费这里定义的 Builder，
 * 因此统一在此设置连接/读取超时，避免模型侧长时间无响应时占用 Tomcat 线程。
 *
 * <p>说明：HTTP 层的重试策略不由这里处理，走框架自带的 spring.ai.retry.*（见 application.yml），
 * 业务层（AiPrefillServiceImpl）只负责“结构化输出解析失败”的那一次重试。
 */
@Configuration
public class AiHttpConfig {

    @Bean
    public RestClient.Builder aiRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 连接超时 10s：多数失败发生在建立连接阶段，快速失败优于长时间悬挂
        requestFactory.setConnectTimeout(10_000);
        // 读取超时 90s：多模态大图 + 工具调用链可能较慢，给足但不无限等待
        requestFactory.setReadTimeout(90_000);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
