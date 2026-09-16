package com.xuyan.fm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。
 * 整改点：启用 @EnableScheduling，订单超时取消定时任务才能真正跑起来
 * （原项目的延迟队列消费线程从未被启动）。
 */
@SpringBootApplication
@EnableScheduling
public class MarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketApplication.class, args);
    }
}
