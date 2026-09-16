package com.xuyan.fm.service;

import com.xuyan.fm.dto.AiPrefillRequest;
import com.xuyan.fm.dto.AiPrefillResponse;

/**
 * AI 识图预填服务。
 *
 * 只做「表单预填器」：根据标题 + 已上传图片生成 { 标题, 描述, 分类, 建议价 } 草稿，
 * 发布主流程 /idle/add 完全不经过本服务，用户确认后仍走原有发布链路。
 */
public interface AiPrefillService {

    /**
     * 生成发布草稿。
     *
     * @param userId  当前登录用户（用于限流）
     * @param request 标题 / 用户当前所选分类 / 已上传图片文件名列表
     * @return 清洗、兜底后的预填结果（name/details 一定非空，labelId/suggestPrice 可能为 null）
     */
    AiPrefillResponse prefill(Long userId, AiPrefillRequest request);
}
