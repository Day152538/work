package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.service.IdleItemService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 闲置商品接口。
 *
 * 整改点（对应分析报告 P0-2-5）：
 * 1. 身份来源改为 JWT 上下文；
 * 2. 删除接口增加属主校验 —— 原接口任何登录用户可删任意商品；
 * 3. 更新接口改走带属主条件的 SQL —— 原接口可篡改任意商品信息。
 */
@RestController
@RequestMapping("idle")
public class IdleItemController {

    private final IdleItemService idleItemService;

    public IdleItemController(IdleItemService idleItemService) {
        this.idleItemService = idleItemService;
    }

    /**
     * 发布闲置
     */
    @PostMapping("add")
    public ResultVo addIdleItem(@RequestBody IdleItemModel idleItemModel) {
        idleItemModel.setUserId(UserContext.getUserId());
        idleItemModel.setIdleStatus((byte) 3);
        idleItemModel.setReleaseTime(new Date());
        if (idleItemService.addIdleItem(idleItemModel)) {
            return ResultVo.success(idleItemModel);
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 商品详情（游客可看，已在拦截器白名单）
     */
    @GetMapping("info")
    public ResultVo getIdleItem(@RequestParam Long id) {
        IdleItemModel item = idleItemService.getIdleItem(id);
        if (item == null) {
            return ResultVo.fail(ErrorMsg.PARAM_ERROR);
        }
        return ResultVo.success(item);
    }

    /**
     * 我发布的所有闲置
     */
    @GetMapping("all")
    public ResultVo getAllIdleItem() {
        return ResultVo.success(idleItemService.getAllIdelItem(UserContext.getUserId()));
    }

    /**
     * 关键字搜索（游客可用）
     */
    @GetMapping("find")
    public ResultVo findIdleItem(@RequestParam(value = "findValue", required = false) String findValue,
                                 @RequestParam(value = "page", required = false) Integer page,
                                 @RequestParam(value = "nums", required = false) Integer nums) {
        String keyword = findValue == null ? "" : findValue;
        int p = page == null ? 1 : Math.max(page, 1);
        int n = nums == null ? 8 : Math.max(nums, 1);
        return ResultVo.success(idleItemService.findIdleItem(keyword, p, n));
    }

    /**
     * 分类查询（游客可用）
     */
    @GetMapping("lable")
    public ResultVo findIdleItemByLable(@RequestParam("idleLabel") Integer idleLabel,
                                        @RequestParam(value = "page", required = false) Integer page,
                                        @RequestParam(value = "nums", required = false) Integer nums) {
        int p = page == null ? 1 : Math.max(page, 1);
        int n = nums == null ? 8 : Math.max(nums, 1);
        return ResultVo.success(idleItemService.findIdleItemByLable(idleLabel, p, n));
    }

    /**
     * 更新自己的商品
     */
    @PostMapping("update")
    public ResultVo updateIdleItem(@RequestBody IdleItemModel idleItemModel) {
        if (idleItemModel.getId() == null) {
            return ResultVo.fail(ErrorMsg.PARAM_ERROR);
        }
        // 属主来自 JWT 上下文，请求体里的 userId 不被信任
        idleItemModel.setUserId(UserContext.getUserId());
        if (idleItemService.updateIdleItem(idleItemModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 删除自己的商品（服务层带属主条件，删别人的返回失败）
     */
    @DeleteMapping("delete/{id}")
    public ResultVo deleteIdleItem(@PathVariable Long id) {
        if (idleItemService.deleteIdleItemById(UserContext.getUserId(), id)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.NO_PERMISSION);
    }
}
