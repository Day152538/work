package com.xuyan.fm.model;

import java.time.LocalDateTime;

/**
 * RAG 知识库切块。
 * 每条记录是一个可检索的知识片段（公告/平台规则/FAQ），附带 embedding 向量。
 */
public class KnowledgeChunk {
    private Long id;
    /** 来源类型：notice / rule / faq */
    private String sourceType;
    /** 来源 ID（公告 id 等） */
    private String sourceId;
    private String title;
    /** 切块内容 */
    private String content;
    /** 向量（逗号分隔 float，1024 维） */
    private String embedding;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
