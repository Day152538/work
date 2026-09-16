package com.xuyan.fm.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具（带优雅降级）。
 *
 * <p>所有写操作均 try-catch：Redis 不可用时返回 false/null，调用方可据此降级为内存模式。
 * 读操作同理。这样未安装 Redis 的开发环境也能启动并正常工作（单机内存模式），
 * 生产环境安装 Redis 后自动切换为分布式模式，无需改代码。
 *
 * <p>典型用法：
 * <pre>
 *   if (!redisUtil.setIfAbsent(key, "1", 300)) {
 *       // Redis 不可用或 key 已存在 → 降级内存判断
 *   }
 * </pre>
 */
@Component
public class RedisUtil {

    private static final Logger log = LoggerFactory.getLogger(RedisUtil.class);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /** 探测 Redis 是否可用（ping），不可用返回 false */
    public boolean isAvailable() {
        try {
            return Boolean.TRUE.equals(redisTemplate.execute(
                    new DefaultRedisScript<>("return 1", Long.class),
                    Collections.emptyList())) ;
        } catch (Exception e) {
            return false;
        }
    }

    /** SET key value（带过期时间），成功返回 true，Redis 不可用返回 false */
    public boolean set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.debug("Redis set 失败（降级）: key={}, err={}", key, e.getMessage());
            return false;
        }
    }

    /** SETNX（key 不存在才设置），成功返回 true；key 已存在或 Redis 不可用返回 false */
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        try {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.debug("Redis setIfAbsent 失败（降级）: key={}, err={}", key, e.getMessage());
            return false;
        }
    }

    /** GET key，不存在或 Redis 不可用返回 null */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.debug("Redis get 失败（降级）: key={}, err={}", key, e.getMessage());
            return null;
        }
    }

    /** GET 并强转为 String */
    public String getString(String key) {
        Object v = get(key);
        return v == null ? null : String.valueOf(v);
    }

    /** DEL key，成功返回 true */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.debug("Redis delete 失败（降级）: key={}, err={}", key, e.getMessage());
            return false;
        }
    }

    /** INCR key（原子自增），返回自增后的值；Redis 不可用返回 -1 */
    public long increment(String key) {
        try {
            Long v = redisTemplate.opsForValue().increment(key);
            return v == null ? -1 : v;
        } catch (Exception e) {
            log.debug("Redis increment 失败（降级）: key={}, err={}", key, e.getMessage());
            return -1;
        }
    }

    /** EXPIRE key（设置过期时间），成功返回 true */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            log.debug("Redis expire 失败（降级）: key={}, err={}", key, e.getMessage());
            return false;
        }
    }

    /** EXISTS key */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }
}
