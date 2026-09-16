package com.xuyan.fm.service;

import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.vo.PageVo;

import java.util.List;

public interface IdleItemService {

    boolean addIdleItem(IdleItemModel idleItemModel);

    IdleItemModel getIdleItem(Long id);

    List<IdleItemModel> getAllIdelItem(Long userId);

    PageVo<IdleItemModel> findIdleItem(String findValue, int page, int nums);

    PageVo<IdleItemModel> findIdleItemByLable(int idleLabel, int page, int nums);

    /**
     * 用户更新自己的商品（属主校验在服务层完成）
     */
    boolean updateIdleItem(IdleItemModel idleItemModel);

    /**
     * 管理端更新商品（上下架/审核），不限制属主
     */
    boolean adminUpdateIdleItem(IdleItemModel idleItemModel);

    PageVo<IdleItemModel> adminGetIdleList(int status, int page, int nums);

    /**
     * 整改点：删除商品带属主校验，用户只能删自己的
     */
    boolean deleteIdleItemById(Long userId, Long id);
}
