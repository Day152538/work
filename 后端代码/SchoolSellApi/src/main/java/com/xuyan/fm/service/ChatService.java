package com.xuyan.fm.service;

import com.xuyan.fm.dto.ChatSendRequest;
import com.xuyan.fm.model.PrivateMessage;

import java.util.List;
import java.util.Map;

/**
 * 买卖双方私信服务（按商品建立会话，下单前后都可聊）。
 */
public interface ChatService {

    /** 发送私信：买家→卖家可直接发起；卖家→买家仅能回复“已有咨询”的买家 */
    PrivateMessage send(Long userId, ChatSendRequest request);

    /** 取某商品上“我与某人”的会话消息（升序），仅双方可见；打开时标已读 */
    List<PrivateMessage> messages(Long userId, long itemId, long otherUserId, int limit);

    /**
     * 我参与的会话列表（按“商品 + 对方”聚合，含商品/对方摘要与未读数）。
     * 返回结构：itemId,itemName,itemPrice,itemImg,otherUserId,otherNickname,otherAvatar,lastContent,lastTime,unread
     */
    List<Map<String, Object>> conversations(Long userId);
}
