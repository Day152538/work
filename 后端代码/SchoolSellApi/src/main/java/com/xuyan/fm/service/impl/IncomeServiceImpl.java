package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.IncomeDao;
import com.xuyan.fm.service.IncomeService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IncomeServiceImpl implements IncomeService {
    @Resource
    private IncomeDao incomeDao;

    public Map<String,Object> getChart() {
        Map<String, Object> chartMap = new HashMap<>();
        //查询每个分类及其收入
        List<Map<String, Object>> categoryIncomes = incomeDao.selectCategoryIncome();
        //查询总收入
        BigDecimal sumIncome = incomeDao.selectSumIncome();
        //放入HashMap中并返回
        chartMap.put("categoryIncomes",categoryIncomes);
        chartMap.put("sumIncome",sumIncome);
        return chartMap;
    }
}
