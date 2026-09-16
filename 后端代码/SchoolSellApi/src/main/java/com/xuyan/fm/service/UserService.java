package com.xuyan.fm.service;

import com.xuyan.fm.model.UserModel;
import com.xuyan.fm.vo.PageVo;

public interface UserService {

    UserModel getUser(Long id);

    /**
     * 登录：按账号查出用户后用 BCrypt 校验密码（兼容历史明文密码并自动升级为哈希）
     */
    UserModel userLogin(String accountNumber, String userPassword);

    boolean userSignIn(UserModel userModel);

    boolean updateUserInfo(UserModel userModel);

    boolean updatePassword(String newPassword, String oldPassword, Long id);

    /**
     * 开通/续费会员（30 天），替代原先前端直接把 userStatus 改成 3 的假支付
     */
    boolean subscribeVip(Long userId);

    PageVo<UserModel> getUserByStatus(int status, int page, int nums);

    boolean resetPassword(String accountNumber, String userEmail, String newPassword);

    /**
     * 管理员重置指定用户密码（仅管理端调用）
     */
    boolean adminResetPassword(Long id, String newPassword);

    void deleteUser(Long id);
}
