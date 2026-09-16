package com.xuyan.fm.service;

import java.util.Map;

/**
 * 图形验证码服务。
 * - 生成四位随机字符（数字 + 大写字母，去掉易混淆的 0/O、1/I）
 * - 以 captchaId 为键暂存于内存，5 分钟过期，一次性校验（校验后即作废）
 * - 校验不区分大小写
 */
public interface CaptchaService {

    /**
     * 生成一张验证码图片，返回 { captchaId, imgBase64 }
     * imgBase64 为可直接放 <img src="data:image/png;base64,..."> 的字符串
     */
    Map<String, String> createCaptcha();

    /**
     * 校验验证码（不区分大小写）。无论对错，校验后该 captchaId 都会失效，
     * 防止同一验证码被反复尝试。
     */
    boolean verify(String captchaId, String input);
}
