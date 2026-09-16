package com.xuyan.fm.service;

import com.xuyan.fm.dto.AiChatRequest;
import com.xuyan.fm.dto.AiChatResponse;
import com.xuyan.fm.model.AiChatMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 对话助手服务（M0 持久会话 + M1 用户长期记忆）。
 *
 * M0：消息持久化到 ai_chat_message，服务端按 conversation_id + user_id 重建上下文，刷新不丢、可跨端。
 * M1：把该用户的长期画像摘要存 ai_user_memory，开场自动注入；对话达到阈值后用模型增量摘要。
 * 联网仍由前端按钮按次控制（默认关）。
 */
public interface AiChatService {

    /**
     * 发一条消息并返回回复。
     *
     * @param userId  当前登录用户（限流 + 归属校验）
     * @param request 消息 + 会话 id（可空则新建）+ 是否联网
     * @return 回复文本 + 会话 id（前端保存，用于续聊/取历史）
     */
    AiChatResponse chat(Long userId, AiChatRequest request);

    /**
     * 流式对话（SSE）：逐块推送回复文本，结束时下发 conversationId。
     *
     * <p>与 chat() 同一套会话/工具/联网逻辑，只是输出改为事件流：
     * <ul>
     *   <li>event=delta：回复文本增量块（data 为纯文本）</li>
     *   <li>event=done：data 为 {"conversationId":"..."}，随后流结束</li>
     *   <li>event=error：data 为错误消息文本，随后流结束</li>
     * </ul>
     * 校验失败（未登录/缺参数/未配置 Key）会在创建 SseEmitter 之前抛出，
     * 由全局异常处理器返回 JSON；前端需区分 SSE 与 JSON 两种响应。
     *
     * @param userId  当前登录用户（限流 + 归属校验）
     * @param request 消息 + 会话 id + 是否联网
     * @return 已开始推送的 SSE emitter（由 Spring MVC 以 text/event-stream 输出）
     */
    SseEmitter chatStream(Long userId, AiChatRequest request);

    /**
     * 取某会话最近的历史（按时间升序），仅本人可见。
     */
    List<AiChatMessage> history(Long userId, String conversationId, int limit);
}
