package com.xuyan.fm.dao;

import com.xuyan.fm.model.ReceiveModel;
import org.apache.ibatis.annotations.*;

import java.util.List;
@Mapper
public interface ReceiveDao {
    // 获取所有管理员留言
    @Select("SELECT * FROM sh_admin_message")
    List<ReceiveModel> getAllAdminMessage();

    // 根据ID获取管理员留言
    @Select("SELECT * FROM sh_admin_message WHERE id = #{id}")
    ReceiveModel getAdminMessageById(Long id);

    // 创建管理员留言
    @Insert("INSERT INTO sh_admin_message (adminMessage, user_id) VALUES (#{adminMessage}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createAdminMessage(ReceiveModel receiveModel);

    // 更新管理员留言
    @Update("UPDATE sh_admin_message SET adminMessage = #{adminMessage}, user_id = #{userId} WHERE id = #{id}")
    void updateAdminMessage(ReceiveModel receiveModel);

    // 删除管理员留言
    @Delete("DELETE FROM sh_admin_message WHERE id = #{id}")
    void deleteAdminMessage(@Param("id") Long id);

}
