package com.xuyan.fm.config;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 接口的每用户限流器（滑动窗口）。
 *
 * <p>相比固定窗口，滑动窗口不会出现“窗口边界瞬间放行 2 倍”的毛刺：
 * 记录每个请求的时间戳，任意的最近 60 秒内最多 maxPerMinute 次。
 *
 * <p>背景：大模型调用按 token 计费且有并发上限，单用户高频点击会造成成本失控。
 * 项目无 Redis（整改时未引入），单实例部署下用内存滑动窗口足够；若水平扩容到多实例，
 * 应改为 Redis 计数或网关限流（注释里保留演进说明）。
 */
@Component
public class AiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimiter.class);

    /** 滑动窗口长度（毫秒） */
    private static final long WINDOW_MS = 60_000L;

    /** 每用户每分钟最大调用次数（可由 ai.rate-limit-per-minute 覆盖） */
    private final int maxPerMinute;

    /** userId → 该用户最近请求时间戳队列（升序） */
    private final Map<Long, Deque<Long>> windows = new ConcurrentHashMap<>();

    public AiRateLimiter(@Value("${ai.rate-limit-per-minute:10}") int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    /**
     * 校验并占用一次配额；超限抛出业务异常（前端提示「操作频繁，稍后重试」）。
     */
    public void check(long userId) {
        Deque<Long> deque = windows.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (deque) {
            long now = System.currentTimeMillis();
            // 弹出窗口外的过期时间戳（队首最旧，队尾最新）
            while (!deque.isEmpty() && now - deque.peekFirst() >= WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= maxPerMinute) {
                throw new BusinessException(ErrorMsg.OPERAT_FREQUENCY);
            }
            deque.addLast(now);
        }
    }

    /**
     * 定时清理：移除“窗口内无任何请求”的用户队列，避免 Map 无限增长。
     */
    @Scheduled(fixedRate = 120_000)
    public void sweepExpiredWindows() {
        long now = System.currentTimeMillis();
        int before = windows.size();
        windows.entrySet().removeIf(e -> {
            Deque<Long> dq = e.getValue();
            synchronized (dq) {
                while (!dq.isEmpty() && now - dq.peekFirst() >= WINDOW_MS) {
                    dq.pollFirst();
                }
                return dq.isEmpty();
            }
        });
        if (windows.size() != before) {
            log.debug("AI 限流器清理过期窗口: {} -> {}", before, windows.size());
        }
    }
}
