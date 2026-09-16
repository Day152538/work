package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.utils.RedisUtil;
import com.xuyan.fm.service.CaptchaService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码实现（数字 + 大写字母，校验不区分大小写）。
 *
 * <p>存储策略：Redis 优先（key=captcha:{id}, TTL=5 分钟），支持多实例部署；
 * Redis 不可用时自动降级为内存 ConcurrentHashMap（单机可用）。两种模式对外行为一致。
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** 字符集：去掉易混淆的 0/O/1/I，降低识别歧义 */
    private static final char[] CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 4;
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final int WIDTH = 130;
    private static final int HEIGHT = 44;

    /** 内存降级存储：captchaId -> CodeEntry */
    private final Map<String, CodeEntry> memoryStore = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    @Resource
    private RedisUtil redisUtil;

    private static final class CodeEntry {
        final String code;
        final long expireAt;
        CodeEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    @Override
    public Map<String, String> createCaptcha() {
        // 顺带清理过期条目（内存模式），避免内存无限增长
        long now = System.currentTimeMillis();
        memoryStore.entrySet().removeIf(e -> e.getValue().expireAt < now);

        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        String code = sb.toString();
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // Redis 优先；失败则降级内存
        boolean stored = redisUtil.set("captcha:" + captchaId, code, TTL_MILLIS, TimeUnit.MILLISECONDS);
        if (!stored) {
            memoryStore.put(captchaId, new CodeEntry(code, now + TTL_MILLIS));
        }

        Map<String, String> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("imgBase64", renderBase64(code));
        return data;
    }

    @Override
    public boolean verify(String captchaId, String input) {
        if (captchaId == null || input == null) {
            return false;
        }
        String key = "captcha:" + captchaId;
        // 先查 Redis（一次性：无论对错都删除）
        String code = redisUtil.getString(key);
        if (code != null) {
            redisUtil.delete(key);
            return code.equalsIgnoreCase(input.trim());
        }
        // Redis 无此 key → 查内存降级（一次性删除）
        CodeEntry entry = memoryStore.remove(captchaId);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            return false;
        }
        return entry.code.equalsIgnoreCase(input.trim());
    }

    /** 渲染验证码图片并转 Base64（PNG） */
    private String renderBase64(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景
        g.setColor(new Color(240, 244, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        g.setColor(new Color(200, 210, 220));
        for (int i = 0; i < 6; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 干扰噪点
        g.setColor(new Color(180, 190, 200));
        for (int i = 0; i < 60; i++) {
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }

        // 逐字符绘制（每个字符颜色略有差异）
        Font font = new Font("SansSerif", Font.BOLD, 28);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int step = (WIDTH - 20) / CODE_LENGTH;
        for (int i = 0; i < CODE_LENGTH; i++) {
            Color color = new Color(
                    20 + random.nextInt(120),
                    20 + random.nextInt(120),
                    20 + random.nextInt(120));
            g.setColor(color);
            int x = 10 + i * step + random.nextInt(6);
            int y = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }
        g.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", baos);
        } catch (Exception e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
