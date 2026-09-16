package com.xuyan.fm.service;

import com.xuyan.fm.model.KnowledgeChunk;

import java.util.List;

/**
 * RAG 检索增强服务。
 */
public interface KnowledgeService {
    /** 检索 top-k 相关知识片段 */
    List<KnowledgeChunk> retrieve(String query, int topK);
    /** 检索默认 top-k（3）相关知识片段 */
    List<KnowledgeChunk> retrieve(String query);
    /** 重建知识库（公告 + 平台规则 → 切块 → embedding → 入库） */
    int rebuild();
    /** 知识库条目数 */
    int count();
}
