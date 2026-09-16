package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.model.MessageModel;
import com.xuyan.fm.service.MessageService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 商品留言接口。
 * 整改点：删除留言带属主校验（原接口任何登录用户可删任意留言，含他人留言）。
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public ResultVo sendMessage(@RequestBody MessageModel messageModel) {
        messageModel.setUserId(UserContext.getUserId());
        messageModel.setCreateTime(new Date());
        if (messageService.addMessage(messageModel)) {
            return ResultVo.success(messageModel);
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    @GetMapping("/info")
    public ResultVo getMessage(@RequestParam Long id) {
        return ResultVo.success(messageService.getMessage(id));
    }

    /**
     * 商品下的留言（游客可看，已在拦截器白名单）
     */
    @GetMapping("/idle")
    public ResultVo getAllIdleMessage(@RequestParam Long idleId) {
        return ResultVo.success(messageService.getAllIdleMessage(idleId));
    }

    @GetMapping("/my")
    public ResultVo getAllMyMessage() {
        return ResultVo.success(messageService.getAllMyMessage(UserContext.getUserId()));
    }

    /**
     * 删除自己发的留言
     */
    @DeleteMapping("/delete/{id}")
    public ResultVo deleteMessage(@PathVariable Long id) {
        if (messageService.deleteMessage(UserContext.getUserId(), id)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.NO_PERMISSION);
    }
}
