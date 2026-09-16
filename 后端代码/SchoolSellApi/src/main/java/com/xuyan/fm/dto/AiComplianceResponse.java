package com.xuyan.fm.dto;

import java.util.List;

/**
 * AI 合规质检的结构化输出目标类型。
 *
 * @param riskLevel LOW / MEDIUM / HIGH（后端兜底默认 LOW，绝不因模型故障误伤正常发布）
 * @param reasons   简短原因，最多 3 条（LOW 时可为空）
 */
public record AiComplianceResponse(String riskLevel, List<String> reasons) {
}
