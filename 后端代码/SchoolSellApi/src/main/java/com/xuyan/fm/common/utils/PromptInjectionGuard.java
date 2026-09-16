package com.xuyan.fm.common.utils;

import java.util.regex.Pattern;

/**
 * Prompt 注入防护检测器。
 *
 * <p>检测用户输入中是否包含试图覆盖系统提示词、诱导模型泄露系统指令、
 * 或扮演其他角色的注入模式（中英文）。检测到则标记为可疑，
 * 由调用方决定拒绝回答或剥离可疑部分。
 *
 * <p>这是纵深防御的一层：系统提示词本身已规定"只做查询不做写操作"，
 * 工具调用有白名单，用户数据有归属隔离；本类在入口处再拦一道。
 */
public final class PromptInjectionGuard {

    private PromptInjectionGuard() {}

    /** 注入模式正则（中英文，不区分大小写） */
    private static final Pattern[] INJECTION_PATTERNS = {
            // 中文：忽略/覆盖系统指令
            Pattern.compile("忽略(以上|之前|前面|所有)?(系统)?指令", Pattern.CASE_INSENSITIVE),
            Pattern.compile("无视(以上|之前|所有)?指令", Pattern.CASE_INSENSITIVE),
            Pattern.compile("忘掉(之前|前面|以上)的(指令|设定|规则)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("不要遵守(之前|以上|系统)的(规则|指令|设定)", Pattern.CASE_INSENSITIVE),
            // 中文：角色扮演/身份覆盖
            Pattern.compile("(从现在开始|现在|接下来)(你)?(是|作为|扮演)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("你现在是(一个|一名|一位)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("假装(你)?是", Pattern.CASE_INSENSITIVE),
            Pattern.compile("扮演(一个|一名|一位)?", Pattern.CASE_INSENSITIVE),
            // 中文：泄露系统提示词
            Pattern.compile("(把|将|输出|显示|重复|告诉我)(你的)?(系统)?(提示词|指令|规则|prompt)(全部|完整)?(输出|显示|告诉我|重复|写出来)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(系统|初始)(提示词|prompt|指令)是什么", Pattern.CASE_INSENSITIVE),
            // 英文：ignore/disregard previous instructions
            Pattern.compile("ignore\\s+(all\\s+)?(previous|prior|above|above-mentioned)?\\s*(instructions|system\\s*instructions|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(all\\s+)?(previous|prior|above)\\s*(instructions|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget\\s+(everything|all)\\s+(before|above|previous)", Pattern.CASE_INSENSITIVE),
            // 英文：role override
            Pattern.compile("(from\\s+now\\s+on|now)\\s+you\\s+(are|act\\s+as|be)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\s+(a|an|the)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pretend\\s+(you\\s+)?are", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s+as\\s+(a|an|the)", Pattern.CASE_INSENSITIVE),
            // 英文：leak system prompt
            Pattern.compile("(repeat|show|output|print|reveal)\\s+(your\\s+)?(system\\s+)?(prompt|instructions|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("what\\s+(is|are)\\s+(your\\s+)?(system\\s+)?(prompt|instructions)", Pattern.CASE_INSENSITIVE),
            // 通用：system: 前缀注入
            Pattern.compile("^\\s*system\\s*:", Pattern.CASE_INSENSITIVE),
    };

    /**
     * 检测输入是否包含 Prompt 注入模式。
     * @return true 表示检测到注入嫌疑
     */
    public static boolean isSuspicious(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测并返回匹配到的注入模式描述（用于日志/提示）。
     * @return 匹配到的模式描述，未匹配返回 null
     */
    public static String detectPattern(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(input).find()) {
                return p.pattern();
            }
        }
        return null;
    }
}
