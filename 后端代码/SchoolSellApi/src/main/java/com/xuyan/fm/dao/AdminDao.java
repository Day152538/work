package com.xuyan.fm.dao;

import com.xuyan.fm.model.AdminModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminDao {

    int deleteByPrimaryKey(Long id);

    int insert(AdminModel record);

    int insertSelective(AdminModel record);

    AdminModel selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AdminModel record);

    int updateByPrimaryKey(AdminModel record);

    /**
     * 按账号查询管理员（含密码哈希），用于登录校验与账号查重。
     * 整改点：原 login 在 SQL 里明文比对密码，现查出哈希后由服务层 BCrypt 校验。
     */
    AdminModel selectByAccountNumber(@Param("accountNumber") String accountNumber);

    /**
     * 分页查询管理员列表
     */
    List<AdminModel> getList(@Param("begin") int begin, @Param("nums") int nums);

    int getCount();

    int updateAdmin(AdminModel adminModel);

    int deleteAdmin(Long id);
}
