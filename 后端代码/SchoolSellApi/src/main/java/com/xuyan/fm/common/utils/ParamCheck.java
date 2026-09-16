package com.xuyan.fm.common.utils;

import java.util.regex.Pattern;

/**
 * 参数格式统一校验（服务端权威校验，规则与前端保持一致，防止绕过前端直接调接口）。
 *
 * 规则（2026-09 统一口径）：
 *  - 手机号：1 开头，第二位 3-9，共 11 位数字（覆盖 13x-19x 全部号段）
 *  - 密码：6-16 位，必须同时包含字母和数字（允许纯大小写字母与数字组合）
 *  - 邮箱：常规 xxx@yyy.zz 结构
 *  - 昵称：去首尾空格后非空，且不超过 30 个字符
 */
public final class ParamCheck {

    private ParamCheck() {
    }

    /** 手机号：1[3-9] + 9 位数字 */
    private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

    /** 密码：6-16 位字母+数字组合，至少包含一个字母和一个数字 */
    private static final Pattern PASSWORD = Pattern.compile("^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$");

    /** 邮箱 */
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    /** 昵称最大长度 */
    private static final int NICKNAME_MAX_LEN = 30;

    public static boolean validPhone(String s) {
        return s != null && PHONE.matcher(s.trim()).matches();
    }

    public static boolean validPassword(String s) {
        return s != null && PASSWORD.matcher(s).matches();
    }

    public static boolean validEmail(String s) {
        return s != null && EMAIL.matcher(s.trim()).matches();
    }

    /** 昵称：去首尾空格后非空且不超过 30 字符 */
    public static boolean validNickname(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim();
        return !t.isEmpty() && t.length() <= NICKNAME_MAX_LEN;
    }
}
