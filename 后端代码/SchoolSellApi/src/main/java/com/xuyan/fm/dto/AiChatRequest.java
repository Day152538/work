package com.xuyan.fm.dto;

import java.util.List;

/**
 * AI 对话助手请求体。
 *
 * 无状态多轮：前端保留最近若干轮对话并随请求带回（history），
 * 服务端每次按“系统提示词 + 历史 + 最新消息”一次性生成，便于重启/多实例也不丢上下文。
 */
public class AiChatRequest {

    /** 用户最新一条消息 */
    private String message;

    /** 会话 id：新对话可传空（后端生成并返回），续聊传回上次的 id */
    private String conversationId;

    /** 历史对话（可选，最多建议携带最近 8~10 轮）：{ role: "user"|"assistant", content } */
    private List<Turn> history;

    /**
     * 本轮是否开启联网搜索（true/false）。
     * null 时回退到后端配置 ai.web-search.dashscope。
     */
    private Boolean webSearch;

    /**
     * 是否使用流式输出（SSE）。true 时请调用 /ai/chat/stream 端点；
     * 该字段仅用于标识，普通 /ai/chat 忽略。
     */
    private Boolean stream;

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public Boolean getWebSearch() {
        return webSearch;
    }

    public void setWebSearch(Boolean webSearch) {
        this.webSearch = webSearch;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<Turn> getHistory() {
        return history;
    }

    public void setHistory(List<Turn> history) {
        this.history = history;
    }

    /** 一轮历史消息 */
    public static class Turn {
        private String role;
        private String content;

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
    }
}
