package com.xuyan.fm.controller;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.interceptor.RequireAdmin;
import com.xuyan.fm.model.NoticeModel;
import com.xuyan.fm.service.NoticeService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告接口。
 * 整改点：原增删改无鉴权，任何游客可篡改全站公告。
 * 现查看保持公开，写操作要求管理员。
 */
@RestController
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ResultVo<List<NoticeModel>> getAllNotices() {
        return ResultVo.success(noticeService.getAllNotices());
    }

    @GetMapping("/{id}")
    public ResultVo<NoticeModel> getNoticeById(@PathVariable Long id) {
        NoticeModel notice = noticeService.getNoticeById(id);
        if (notice != null) {
            return ResultVo.success(notice);
        }
        return ResultVo.fail(ErrorMsg.PARAM_ERROR);
    }

    @RequireAdmin
    @PostMapping
    public ResultVo<NoticeModel> createNotice(@RequestBody NoticeModel notice) {
        return ResultVo.success(noticeService.createNotice(notice));
    }

    @RequireAdmin
    @PutMapping("/{id}")
    public ResultVo<NoticeModel> updateNotice(@PathVariable Long id, @RequestBody NoticeModel notice) {
        NoticeModel updated = noticeService.updateNotice(id, notice);
        if (updated != null) {
            return ResultVo.success(updated);
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    @RequireAdmin
    @DeleteMapping("/{id}")
    public ResultVo<Void> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResultVo.success();
    }
}
