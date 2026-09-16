package com.xuyan.fm.controller;

import com.xuyan.fm.common.interceptor.RequireAdmin;
import com.xuyan.fm.model.TypeModel;
import com.xuyan.fm.service.TypeService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

/**
 * 商品分类接口。
 * 整改点（对应分析报告 P0-2-8）：原增删改接口完全无鉴权，
 * 任何游客都可以增删改全站分类。现写操作要求管理员角色（@RequireAdmin），
 * 分类列表保持公开供游客浏览。
 */
@RestController
@RequestMapping("/type")
public class TypeController {

    private final TypeService typeService;

    public TypeController(TypeService typeService) {
        this.typeService = typeService;
    }

    /**
     * 分类列表（公开，游客可浏览商品分类）
     */
    @GetMapping("/listByCondition")
    public ResultVo listByCondition(@RequestParam(required = false) Integer begin,
                                    @RequestParam(required = false) Integer size) {
        int b = begin == null ? 0 : Math.max(begin, 0);
        int s = size == null ? 50 : Math.max(size, 1);
        return ResultVo.success(typeService.listByCondition(b, s));
    }

    @RequireAdmin
    @PostMapping("/add")
    public ResultVo addType(@RequestBody TypeModel typeModel) {
        typeService.addType(typeModel);
        return ResultVo.success();
    }

    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    public ResultVo deleteType(@PathVariable Long id) {
        typeService.deleteType(id);
        return ResultVo.success();
    }

    @RequireAdmin
    @PostMapping("/update")
    public ResultVo updateType(@RequestBody TypeModel typeModel) {
        typeService.updateType(typeModel);
        return ResultVo.success();
    }
}
