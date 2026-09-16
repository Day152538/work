package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.dto.ChatSendRequest;
import com.xuyan.fm.model.PrivateMessage;
import com.xuyan.fm.service.ChatService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 买卖双方私信接口（商品级会话，下单前/后都能聊）。
 * 路径不在白名单 → 需登录。
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 发送私信：{ itemId, orderId?, toUserId?, content } */
    @PostMapping("/send")
    public ResultVo send(@RequestBody ChatSendRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorMsg.MISSING_PARAMETER);
        }
        PrivateMessage message = chatService.send(UserContext.getUserId(), request);
        return ResultVo.success(message);
    }

    /** 取某商品上“我与对方”的会话消息（升序），并标记已读 */
    @GetMapping("/messages")
    public ResultVo messages(@RequestParam("itemId") long itemId,
                             @RequestParam("otherUserId") long otherUserId,
                             @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
        List<PrivateMessage> list = chatService.messages(UserContext.getUserId(), itemId, otherUserId, limit);
        return ResultVo.success(list);
    }

    /** 我参与的会话列表（聚合展示用） */
    @GetMapping("/conversations")
    public ResultVo conversations() {
        List<Map<String, Object>> list = chatService.conversations(UserContext.getUserId());
        return ResultVo.success(list);
    }
}
