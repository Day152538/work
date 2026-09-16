package com.xuyan.fm.dao;

import com.xuyan.fm.model.PrivateMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 买卖双方私信 DAO（private_message）。
 *
 * 会话以「商品 item_id + 双方(me, other)」为线索，可下单前咨询；order_id 仅作冗余关联。
 * 读取安全：一切按 (item_id, userA, userB) 对过滤，任何一方都只能看到“自己参与的线程”。
 */
@Mapper
public interface ChatDao {

    @Insert("insert into private_message (from_user, to_user, order_id, item_id, content, read_flag, create_time) "
            + "values (#{fromUser}, #{toUser}, #{orderId}, #{itemId}, #{content}, 0, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PrivateMessage message);

    /** 某商品下、我与某人之间的消息（倒序取最近 N 条） */
    @Select("select id, from_user as fromUser, to_user as toUser, order_id as orderId, item_id as itemId, "
            + "content, read_flag as readFlag, create_time as createTime "
            + "from private_message "
            + "where item_id = #{itemId} "
            + "  and ((from_user = #{a} and to_user = #{b}) or (from_user = #{b} and to_user = #{a})) "
            + "order by id desc limit #{limit}")
    List<PrivateMessage> threadMessages(@Param("itemId") long itemId,
                                        @Param("a") long a,
                                        @Param("b") long b,
                                        @Param("limit") int limit);

    /** 某商品线路上双方是否已经有过往来（卖家侧“回复已有咨询”用） */
    @Select("select count(*) from private_message where item_id = #{itemId} "
            + "and ((from_user = #{a} and to_user = #{b}) or (from_user = #{b} and to_user = #{a}))")
    int countThread(@Param("itemId") long itemId, @Param("a") long a, @Param("b") long b);

    /** 打开线程时把发给我的未读标为已读 */
    @Update("update private_message set read_flag = 1 "
            + "where item_id = #{itemId} and to_user = #{toUser} and read_flag = 0")
    int markRead(@Param("itemId") long itemId, @Param("toUser") long toUser);

    /** 我参与过的最近消息（倒序，供前端聚合会话列表；数据量小可直接聚合） */
    @Select("select id, from_user as fromUser, to_user as toUser, order_id as orderId, item_id as itemId, "
            + "content, read_flag as readFlag, create_time as createTime "
            + "from private_message where from_user = #{userId} or to_user = #{userId} "
            + "order by id desc limit #{limit}")
    List<PrivateMessage> recentMine(@Param("userId") long userId, @Param("limit") int limit);
}
