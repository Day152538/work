package com.xuyan.fm.dto;

import java.util.List;

/**
 * AI 识图预填请求体。
 *
 * 前端发布页把「已上传的商品图文件名 + 用户已填标题 + 当前所选分类」打包发来，
 * 后端据此调用视觉大模型生成发布草稿。注意：图片不重新上传，直接复用
 * 服务端 pic 目录里已有的文件（文件名由 /file 上传接口签发，天然安全）。
 *
 * <p>精修模式（2026-09 新增）：当 instructions（用户的修改要求）非空时，表示用户对
 * 上一版草稿不满意、要求按规则重写；此时 draft 携带上一版草稿（前端拼好），
 * title 仍作为原始锚点标题。
 */
public class AiPrefillRequest {

    /** 用户已填写的商品标题（可能为空，为空且无图则直接拒绝） */
    private String title;

    /** 用户当前选择的分类 id（idle_label），模型拿不准分类时后端回退到它 */
    private Long labelId;

    /** 已上传的商品图文件名，最多取前 3 张参与识别 */
    private List<String> imageNames;

    /** 精修要求：如“加上可小刀”“改成口语化”“突出续航”，非空即走重写模式 */
    private String instructions;

    /** 上一版草稿（前端把 name/details/分类/价格等拼成一段文字），精修模式使用 */
    private String draft;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getLabelId() {
        return labelId;
    }

    public void setLabelId(Long labelId) {
        this.labelId = labelId;
    }

    public List<String> getImageNames() {
        return imageNames;
    }

    public void setImageNames(List<String> imageNames) {
        this.imageNames = imageNames;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }
}
