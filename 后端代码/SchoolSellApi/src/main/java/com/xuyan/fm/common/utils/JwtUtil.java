package com.xuyan.fm.common.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.xuyan.fm.common.context.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 工具类（Spring Bean，密钥与有效期由配置文件注入）。
 *
 * 整改点：
 * 1. 原实现密钥硬编码为 "common" 且写死 12 小时过期 —— 任何人都能用公开的密钥伪造 token；
 *    现改为从 application.yml 读取（生产环境可用环境变量覆盖）。
 * 2. claims 结构化：userId / accountNumber / role 分离，支撑用户与管理员双角色鉴权。
 * 3. parseToken 失败时抛出异常，由调用方（拦截器）统一处理，不再静默吞掉。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours:12}")
    private long expireHours;

    /**
     * 签发 token
     */
    public String generateToken(Long userId, String accountNumber, String role) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireHours * 3600_000L);
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("accountNumber", accountNumber)
                .withClaim("role", role)
                .withIssuedAt(now)
                .withExpiresAt(expireAt)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 校验并解析 token，非法/过期 token 抛出 JWTVerificationException
     */
    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
        return verifier.verify(token);
    }

    /**
     * 解析 token 为登录用户上下文对象
     */
    public UserContext.LoginUser parseUser(String token) throws JWTVerificationException {
        DecodedJWT jwt = verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        String accountNumber = jwt.getClaim("accountNumber").asString();
        String role = jwt.getClaim("role").asString();
        if (userId == null) {
            throw new JWTVerificationException("invalid token: missing userId");
        }
        return new UserContext.LoginUser(userId, accountNumber,
                role == null ? UserContext.ROLE_USER : role);
    }
}
