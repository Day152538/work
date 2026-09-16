package com.xuyan.fm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置。
 *
 * 整改点（对应分析报告 P0-3）：
 * 原项目密码明文入库、登录用 SQL 直接比对（user_password=#{password}），
 * 数据库一旦泄露所有账号密码全部暴露。现统一使用 BCrypt 加盐哈希存储，
 * 仅引入 spring-security-crypto 模块（不引入整套 Security 过滤链，保持轻量）。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
