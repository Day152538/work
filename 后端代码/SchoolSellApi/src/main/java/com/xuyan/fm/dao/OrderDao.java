package com.xuyan.fm.dao;

import com.xuyan.fm.model.OrderModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDao {

    int deleteByPrimaryKey(Long id);

    int insert(OrderModel record);

    int insertSelective(OrderModel record);

    OrderModel selectByPrimaryKey(Long id);

    List<OrderModel> getMyOrder(@Param("userId") Long userId);

    List<OrderModel> getAllOrder(@Param("begin") int begin, @Param("nums") int nums);

    int countAllOrder();

    List<OrderModel> findOrderByIdleIdList(@Param("idleIdList") List<Long> idleIdList);

    int updateByPrimaryKeySelective(OrderModel record);

    int updateByPrimaryKey(OrderModel record);

    /**
     * 条件更新订单状态（乐观锁语义）：仅当当前状态等于 fromStatus 时才更新为 toStatus。
     * 整改点：原 updateOrder 先查后改，支付与超时取消并发时存在竞态（查到未支付、改时已支付），
     * 现把状态前置条件放进 WHERE 子句，由数据库保证原子性。
     */
    int updateOrderStatusIfCurrent(@Param("id") Long id,
                                   @Param("fromStatus") Byte fromStatus,
                                   @Param("toStatus") Byte toStatus);

    /**
     * 条件支付：仅当订单未支付时才写入支付信息（防止已取消订单被补单支付）
     */
    int markPaidIfUnpaid(@Param("id") Long id,
                         @Param("paymentWay") String paymentWay,
                         @Param("paymentTime") Date paymentTime);

    /**
     * 查询超时未支付的订单（供定时取消任务使用）。
     * 整改点：原“内存延迟队列”方案存在两个致命缺陷——消费线程从未被启动（功能完全失效），
     * 且任务存于内存，服务重启即丢失。改为数据库扫描，任务可持久、幂等。
     */
    List<OrderModel> selectTimeoutUnpaidOrders(@Param("deadline") Date deadline,
                                               @Param("limit") int limit);

    /**
     * AI 定价依据：按商品分类统计平台内已支付订单的成交行情
     * （笔数 / 最低 / 平均 / 最高价）。供 @Tool 查询，模型据此给出有依据的参考价。
     *
     * @return 形如 {cnt, minPrice, avgPrice, maxPrice} 的 Map（无成交时为 0 / null）
     */
    Map<String, Object> selectRecentDealStatsByLabel(@Param("labelId") int labelId);
}
