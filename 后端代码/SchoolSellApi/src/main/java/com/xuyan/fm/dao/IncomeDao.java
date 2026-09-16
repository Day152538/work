package com.xuyan.fm.dao;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@Mapper
public interface IncomeDao {
    @MapKey("idleLabel")
    List<Map<String, Object>> selectCategoryIncome();
    BigDecimal selectSumIncome();
}
