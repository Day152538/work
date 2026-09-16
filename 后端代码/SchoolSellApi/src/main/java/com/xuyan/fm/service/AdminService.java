package com.xuyan.fm.service;

import com.xuyan.fm.model.AdminModel;
import com.xuyan.fm.vo.PageVo;

public interface AdminService {

    /**
     * 管理员登录（BCrypt 校验，兼容历史明文并自动升级）
     */
    AdminModel login(String accountNumber, String adminPassword);

    PageVo<AdminModel> getAdminList(int page, int nums);

    boolean addAdmin(AdminModel adminModel);

    boolean updateAdmin(AdminModel adminModel);

    boolean deleteAdmin(Long id);
}
