package com.xuyan.fm.dto;

/**
 * AI 对话助手响应。
 *
 * @param reply          助手的文本回复
 * @param conversationId 本次会话 id（前端保存，续聊/刷新后凭它取回历史）
 */
public record AiChatResponse(String reply, String conversationId) {
}
