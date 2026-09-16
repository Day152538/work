package com.xuyan.fm.model;

import java.util.Date;

/**
 * AI 用户长期记忆（ai_user_memory）：该用户的画像/偏好摘要。
 * 开场自动注入到系统提示词；summarizedUptoId 记录“已摘要到 ai_chat_message 哪条”，实现增量摘要。
 */
public class AiUserMemory {

    private Long userId;
    /** 长期画像摘要（可空） */
    private String summary;
    /** 已摘要到的消息最大 id */
    private Long summarizedUptoId;
    private Date updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getSummarizedUptoId() {
        return summarizedUptoId;
    }

    public void setSummarizedUptoId(Long summarizedUptoId) {
        this.summarizedUptoId = summarizedUptoId;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
