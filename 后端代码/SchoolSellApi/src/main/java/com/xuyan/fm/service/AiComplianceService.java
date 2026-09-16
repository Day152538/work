package com.xuyan.fm.service;

import com.xuyan.fm.dto.AiComplianceRequest;
import com.xuyan.fm.dto.AiComplianceResponse;

/**
 * AI 发布前图文合规质检服务（2026-09 新增）。
 *
 * 对标题 + 描述 + 图片做一次多模态内容安全初审，返回 { riskLevel: LOW/MEDIUM/HIGH, reasons[] }。
 * 只用于提示卖家 / 与平台「待审核」流程衔接，不直接替用户决定是否发帖。
 */
public interface AiComplianceService {

    /**
     * @param userId  当前登录用户（用于限流）
     * @param request 标题 / 描述 / 已上传图片文件名
     * @return 风险等级与原因（后端兜底，模型故障时返回 LOW，避免误伤正常发布）
     */
    AiComplianceResponse check(Long userId, AiComplianceRequest request);
}
