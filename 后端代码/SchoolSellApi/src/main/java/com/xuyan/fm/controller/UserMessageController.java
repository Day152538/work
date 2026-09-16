package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.model.UserMessageModel;
import com.xuyan.fm.service.UserMessageService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

/**
 * 用户留言板接口。
 *
 * 整改点（对应分析报告 P0-2-7）：
 * 原接口完全无鉴权：任何人可查看任意用户留言（listByUserId 传谁查谁）、
 * 任意增删改任意用户的留言。现：
 * 1. 查询/新增/修改/删除全部要求登录，且操作对象只能是当前用户自己的数据；
 * 2. userId 一律取自 JWT 上下文，不信任请求参数。
 */
@RestController
@RequestMapping("/userMessage")
public class UserMessageController {

    private final UserMessageService userMessageService;

    public UserMessageController(UserMessageService userMessageService) {
        this.userMessageService = userMessageService;
    }

    @GetMapping("/listByUserId")
    public ResultVo listByUserId(@RequestParam(required = false) Long userId) {
        // 只能查自己的留言：忽略请求参数，强制使用 JWT 身份
        return ResultVo.success(userMessageService.listByUserId(UserContext.getUserId()));
    }

    @PostMapping("/add")
    public ResultVo addUserMessage(@RequestBody UserMessageModel userMessageModel) {
        userMessageModel.setUserId(UserContext.getUserId());
        userMessageService.addUserMessage(userMessageModel);
        return ResultVo.success();
    }

    @DeleteMapping("/delete/{id}")
    public ResultVo deleteUserMessage(@PathVariable Long id) {
        if (userMessageService.deleteUserMessage(UserContext.getUserId(), id)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.NO_PERMISSION);
    }

    @PostMapping("/update")
    public ResultVo updateUserMessage(@RequestBody UserMessageModel userMessageModel) {
        userMessageModel.setUserId(UserContext.getUserId());
        if (userMessageService.updateUserMessage(userMessageModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.NO_PERMISSION);
    }
}
