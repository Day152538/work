package com.xuyan.fm.dao;

import com.xuyan.fm.model.UserModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDao {

    int deleteByPrimaryKey(Long id);

    int insert(UserModel record);

    int insertSelective(UserModel record);

    /**
     * 按账号查询用户（含密码哈希），用于登录校验与注册查重。
     * 整改点：原 userLogin 直接在 SQL 里比对明文密码，现改为查出哈希后在服务层用 BCrypt 校验。
     */
    UserModel selectByAccountNumber(@Param("accountNumber") String accountNumber);

    UserModel selectByPrimaryKey(Long id);

    List<UserModel> getUserList();

    List<UserModel> findUserByList(List<Long> idList);

    List<UserModel> getNormalUser(@Param("begin") int begin, @Param("nums") int nums);

    List<UserModel> getBanUser(@Param("begin") int begin, @Param("nums") int nums);

    int countNormalUser();

    int countBanUser();

    int updateByPrimaryKeySelective(UserModel record);

    int updateByPrimaryKey(UserModel record);

    /**
     * 修改密码（新密码已是 BCrypt 哈希，由服务层生成）
     */
    int updatePassword(@Param("newPassword") String newPassword, @Param("id") Long id);

    UserModel findByPhoneNumberAndEmail(@Param("accountNumber") String accountNumber,
                                        @Param("userEmail") String userEmail);
}
