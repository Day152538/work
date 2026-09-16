package com.xuyan.fm.model;

import java.util.Date;

/**
 * AI 对话消息（持久化于 ai_chat_message）。
 * 归属校验：读取时必须按 conversation_id + user_id 双重条件，用户只能查到自己会话里的消息。
 */
public class AiChatMessage {

    private Long id;
    private String conversationId;
    private Long userId;
    private String role;
    private String content;
    private Date createTime;

    public AiChatMessage() {
    }

    public AiChatMessage(String conversationId, Long userId, String role, String content) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
