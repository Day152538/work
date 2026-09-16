package com.xuyan.fm.dao;

import com.xuyan.fm.model.AdminFeedbackVO;
import com.xuyan.fm.model.UserMessageModel;
import org.apache.ibatis.annotations.*;

import java.util.List;
@Mapper
public interface UserMessageDao {
    @Select("SELECT * FROM sh_user_message WHERE user_id = #{userId}")
    @Results({
            @Result(property = "userMessage", column = "text"),
            // 其他映射关系...
    })
    List<UserMessageModel> listByUserId(Long userId);

    /** 管理端：列出所有用户的反馈，联查用户昵称/账号 */
    @Select("SELECT um.id, um.text AS adminMessage, u.nickname, u.account_number AS accountNumber " +
            "FROM sh_user_message um LEFT JOIN sh_user u ON u.id = um.user_id " +
            "ORDER BY um.id DESC")
    List<AdminFeedbackVO> listAllForAdmin();

    /** 管理端：按 id 删除任意用户的反馈 */
    @Delete("DELETE FROM sh_user_message WHERE id = #{id}")
    int deleteByIdForAdmin(@Param("id") Long id);

    @Insert("INSERT INTO sh_user_message (text, user_id) VALUES (#{userMessage}, #{userId})")
    void addUserMessage(UserMessageModel userMessageModel);

    @Delete("DELETE FROM sh_user_message WHERE id = #{id}")
    void deleteUserMessage(Long id);

    @Update("UPDATE sh_user_message SET text = #{userMessage}, user_id = #{userId} WHERE id = #{id}")
    void updateUserMessage(UserMessageModel userMessageModel);

    /** 整改点：带属主条件的删除，WHERE 中同时匹配 user_id，防止水平越权删除他人留言 */
    @Delete("DELETE FROM sh_user_message WHERE id = #{id} AND user_id = #{userId}")
    boolean deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /** 整改点：带属主条件的更新，仅允许修改本人留言内容，且不允许改绑 user_id */
    @Update("UPDATE sh_user_message SET text = #{userMessage} WHERE id = #{id} AND user_id = #{userId}")
    boolean updateByIdAndUser(UserMessageModel userMessageModel);
}
