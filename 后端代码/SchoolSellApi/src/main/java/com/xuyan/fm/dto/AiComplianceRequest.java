package com.xuyan.fm.dto;

import java.util.List;

/**
 * AI 发布前合规质检请求体。
 *
 * 发布页把「标题 + 描述 + 已上传图文件名」打包发来，后端调用多模态模型做一次内容安全初审，
 * 返回风险等级与原因；只用于提示卖家 / 结合平台审核流程，不直接拦截发布（人工确认仍在）。
 */
public class AiComplianceRequest {

    /** 商品标题（可能为空） */
    private String title;

    /** 商品描述（可能为空） */
    private String details;

    /** 已上传的商品图文件名，最多取前 3 张 */
    private List<String> imageNames;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<String> getImageNames() {
        return imageNames;
    }

    public void setImageNames(List<String> imageNames) {
        this.imageNames = imageNames;
    }
}
