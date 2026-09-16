package com.xuyan.fm.controller;

import com.xuyan.fm.common.interceptor.RequireAdmin;
import com.xuyan.fm.service.IncomeService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 营收统计接口。
 * 整改点：原接口无鉴权，任何游客可看全平台营收数据。现要求管理员。
 */
@RestController
@RequestMapping("/api/income")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @RequireAdmin
    @GetMapping("/chart")
    public ResultVo getChart() {
        return ResultVo.success(incomeService.getChart());
    }
}
