package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.interceptor.RequireAdmin;
import com.xuyan.fm.model.ReceiveModel;
import com.xuyan.fm.service.ReceiveService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户给管理员的留言接口。
 *
 * 整改点（对应分析报告 P0-2-6）：
 * 原 PUT/DELETE 接口完全无鉴权，任何人可改/删任何留言。
 * 现权限模型：登录用户只能提交留言（POST）；
 * 查看/修改/删除由管理员（@RequireAdmin）操作。
 */
@RestController
@RequestMapping("/receive")
public class ReceiveController {

    private final ReceiveService receiveService;

    public ReceiveController(ReceiveService receiveService) {
        this.receiveService = receiveService;
    }

    /**
     * 全部留言（管理端处理留言用）
     */
    @RequireAdmin
    @GetMapping("/all")
    public ResultVo<List<ReceiveModel>> getAllAdminMessage() {
        return ResultVo.success(receiveService.getAllAdminMessage());
    }

    @RequireAdmin
    @GetMapping("/{id}")
    public ResultVo<ReceiveModel> getAdminMessageById(@PathVariable Long id) {
        ReceiveModel receiveModel = receiveService.getAdminMessageById(id);
        if (receiveModel != null) {
            return ResultVo.success(receiveModel);
        }
        return ResultVo.fail(ErrorMsg.PARAM_ERROR);
    }

    /**
     * 登录用户给管理员提交留言
     */
    @PostMapping
    public ResultVo<ReceiveModel> createNotice(@RequestBody ReceiveModel receiveModel) {
        receiveModel.setUserId(UserContext.getUserId());
        ReceiveModel created = receiveService.createAdminMessage(receiveModel);
        return ResultVo.success(created);
    }

    @RequireAdmin
    @PutMapping("/{id}")
    public ResultVo<ReceiveModel> updateAdminMessage(@PathVariable Long id, @RequestBody ReceiveModel receiveModel) {
        ReceiveModel updated = receiveService.updateAdminMessage(id, receiveModel);
        if (updated != null) {
            return ResultVo.success(updated);
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    public ResultVo deleteAdminMessage(@PathVariable Long id) {
        receiveService.deleteAdminMessage(id);
        return ResultVo.success();
    }
}
