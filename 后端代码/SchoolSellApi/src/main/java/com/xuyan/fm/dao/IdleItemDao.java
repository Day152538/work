package com.xuyan.fm.dao;

import com.xuyan.fm.model.IdleItemModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface IdleItemDao {

    boolean deleteByPrimaryKey(Long id);

    /**
     * 整改点：删除操作带属主条件（where id=? and user_id=?），
     * 修复原接口任何人可删他人商品的水平越权问题。
     */
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int insert(IdleItemModel record);

    int insertSelective(IdleItemModel record);

    IdleItemModel selectByPrimaryKey(Long id);

    List<IdleItemModel> getAllIdleItem(@Param("userId") Long userId);

    int countIdleItem(@Param("findValue") String findValue);

    int countIdleItemByLable(@Param("idleLabel") int idleLabel);

    int countIdleItemByStatus(@Param("status") int status);

    List<IdleItemModel> findIdleItem(@Param("findValue") String findValue,
                                     @Param("begin") int begin, @Param("nums") int nums);

    /**
     * AI 工具用：在售商品按「关键字 + 分类 + 价格区间」组合筛选（条件均可选，最多 nums 条）。
     */
    List<IdleItemModel> findOnSaleByFilter(@Param("findValue") String findValue,
                                           @Param("labelId") Integer labelId,
                                           @Param("minPrice") BigDecimal minPrice,
                                           @Param("maxPrice") BigDecimal maxPrice,
                                           @Param("begin") int begin, @Param("nums") int nums);

    List<IdleItemModel> findIdleItemByLable(@Param("idleLabel") int idleLabel,
                                            @Param("begin") int begin, @Param("nums") int nums);

    List<IdleItemModel> getIdleItemByStatus(@Param("status") int status,
                                            @Param("begin") int begin, @Param("nums") int nums);

    /**
     * 整改点：更新操作带属主条件，用户只能改自己的商品；
     * 原 updateByPrimaryKeySelective 只按 id 更新，恶意请求可篡改任意商品。
     */
    int updateByIdAndUserSelective(IdleItemModel record);

    int updateByPrimaryKeySelective(IdleItemModel record);

    int updateByPrimaryKey(IdleItemModel record);

    List<IdleItemModel> findIdleByList(@Param("idList") List<Long> idList);

    /**
     * 条件状态流转（乐观锁语义）：仅当商品处于 fromStatus 时才更新为 toStatus。
     * 用于下单锁定商品（1 上架 -> 2 已锁定）、取消订单重新上架（2 -> 1），
     * 替代原先“代码里查状态再更新”的竞态写法。
     */
    int updateIdleStatusIfCurrent(@Param("id") Long id,
                                  @Param("fromStatus") Byte fromStatus,
                                  @Param("toStatus") Byte toStatus);

    /**
     * 下单预占库存：原子扣减 1 件（拍下即占，防并发超卖）。
     * 仅当商品“在售(1)且有库存”时可预占，返回受影响行数（=0 表示已售罄/已下架/被抢先）。
     */
    int reserveStock(@Param("id") Long id);

    /**
     * 预占后剩余库存为 0 则自动下架(1->2)，用于“整件售罄”时从首页消失。
     */
    int downIfNoStock(@Param("id") Long id);

    /**
     * 取消/放弃未支付订单时释放预占的 1 件库存。
     */
    int releaseStock(@Param("id") Long id);

    /**
     * 释放库存后若仍有在售库存(>0)且当前下架(2)，则自动重新上架(2->1)。
     */
    int relistIfInStock(@Param("id") Long id);

    /**
     * 支付成功：销量 +1（库存在下单时已预占，此处不再扣）。
     */
    int increaseSalesCount(@Param("id") Long id);
}
