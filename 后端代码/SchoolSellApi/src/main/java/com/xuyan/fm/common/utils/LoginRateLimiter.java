package com.xuyan.fm.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败限流（Redis 优先 + 内存降级）。
 *
 * <p>策略：同一账号/IP 连续失败 5 次锁定 15 分钟。
 * Redis 模式用 INCR + EXPIRE 原子计数，支持多实例部署；
 * Redis 不可用时降级为内存 ConcurrentHashMap（单机可用）。
 *
 * <p>原实现为静态工具类 + 内存 Map，多实例部署时各节点计数独立，
 * 攻击者可通过负载均衡绕过限流；改造后 Redis 为权威计数源。
 */
@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    /** 连续失败阈值 */
    private static final int MAX_FAIL = 5;
    /** 锁定时长（分钟） */
    private static final int LOCK_MINUTES = 15;

    /** 内存降级：key -> FailCount */
    private final Map<String, FailCount> memoryStore = new ConcurrentHashMap<>();

    @Resource
    private RedisUtil redisUtil;

    private static final class FailCount {
        int count;
        long firstFailAt;
        FailCount(int count, long firstFailAt) {
            this.count = count;
            this.firstFailAt = firstFailAt;
        }
    }

    /** 是否被锁定（失败次数 >= 阈值且在锁定窗口内） */
    public boolean isLocked(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String redisKey = "login:fail:" + key;
        // Redis 优先
        String val = redisUtil.getString(redisKey);
        if (val != null) {
            try {
                return Integer.parseInt(val) >= MAX_FAIL;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        // Redis 无此 key → 查内存降级
        FailCount fc = memoryStore.get(key);
        if (fc == null) {
            return false;
        }
        // 锁定窗口已过 → 自动清除
        if (System.currentTimeMillis() - fc.firstFailAt > LOCK_MINUTES * 60_000L) {
            memoryStore.remove(key);
            return false;
        }
        return fc.count >= MAX_FAIL;
    }

    /** 记录一次失败（计数 +1，首次失败时设置过期时间） */
    public void recordFail(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        String redisKey = "login:fail:" + key;
        // Redis 优先：INCR + 首次设置 EXPIRE
        long count = redisUtil.increment(redisKey);
        if (count > 0) {
            if (count == 1) {
                redisUtil.expire(redisKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            return;
        }
        // Redis 不可用 → 内存降级
        memoryStore.compute(key, (k, old) -> {
            long now = System.currentTimeMillis();
            if (old == null || now - old.firstFailAt > LOCK_MINUTES * 60_000L) {
                return new FailCount(1, now);
            }
            old.count++;
            return old;
        });
    }

    /** 登录成功后清除失败计数 */
    public void clear(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        redisUtil.delete("login:fail:" + key);
        memoryStore.remove(key);
    }

    /** 剩余可尝试次数（用于前端提示） */
    public int remainingAttempts(String key) {
        if (key == null || key.isBlank()) {
            return MAX_FAIL;
        }
        String val = redisUtil.getString("login:fail:" + key);
        if (val != null) {
            try {
                return Math.max(0, MAX_FAIL - Integer.parseInt(val));
            } catch (NumberFormatException e) {
                return MAX_FAIL;
            }
        }
        FailCount fc = memoryStore.get(key);
        if (fc == null) {
            return MAX_FAIL;
        }
        return Math.max(0, MAX_FAIL - fc.count);
    }
}
