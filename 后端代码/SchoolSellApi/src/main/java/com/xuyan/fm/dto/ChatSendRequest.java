package com.xuyan.fm.dto;

/**
 * 私信发送请求体（商品级会话，下单前/下单后均可）。
 * - 买家给某商品的卖家发消息：只需 itemId（后端推导卖家）。
 * - 卖家回复某买家：需 itemId + toUserId，且要求该买家已在本商品上发过咨询。
 * - orderId 可选：成交后继续同窗沟通时可带上（冗余关联）。
 */
public class ChatSendRequest {

    private Long itemId;
    private Long orderId;
    private Long toUserId;
    private String content;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
