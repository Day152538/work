package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.AdminDao;
import com.xuyan.fm.model.AdminModel;
import com.xuyan.fm.service.AdminService;
import com.xuyan.fm.vo.PageVo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 管理员服务实现。
 * 整改点：登录改为 BCrypt 哈希校验（兼容存量明文并自动升级）；
 * 新增管理员时密码哈希入库，不再明文存储。
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final String BCRYPT_PREFIX = "$2";

    @Resource
    private AdminDao adminDao;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public AdminModel login(String accountNumber, String adminPassword) {
        AdminModel admin = adminDao.selectByAccountNumber(accountNumber);
        if (admin == null || admin.getAdminPassword() == null) {
            return null;
        }
        String stored = admin.getAdminPassword();
        if (stored.startsWith(BCRYPT_PREFIX)) {
            if (!passwordEncoder.matches(adminPassword, stored)) {
                return null;
            }
        } else {
            // 存量明文：比对成功后自动升级为 BCrypt
            if (!stored.equals(adminPassword)) {
                return null;
            }
            AdminModel update = new AdminModel();
            update.setId(admin.getId());
            update.setAdminPassword(passwordEncoder.encode(adminPassword));
            adminDao.updateAdmin(update);
        }
        admin.setAdminPassword(null);
        return admin;
    }

    @Override
    public PageVo<AdminModel> getAdminList(int page, int nums) {
        List<AdminModel> list = adminDao.getList((page - 1) * nums, nums);
        int count = adminDao.getCount();
        return new PageVo<>(list, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addAdmin(AdminModel adminModel) {
        if (adminDao.selectByAccountNumber(adminModel.getAccountNumber()) != null) {
            return false;
        }
        adminModel.setAdminPassword(passwordEncoder.encode(adminModel.getAdminPassword()));
        return adminDao.insert(adminModel) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAdmin(AdminModel adminModel) {
        // 有密码则更新为哈希，无密码则只更新其他字段
        if (adminModel.getAdminPassword() != null && !adminModel.getAdminPassword().isEmpty()) {
            adminModel.setAdminPassword(passwordEncoder.encode(adminModel.getAdminPassword()));
        } else {
            adminModel.setAdminPassword(null);
        }
        return adminDao.updateAdmin(adminModel) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAdmin(Long id) {
        return adminDao.deleteAdmin(id) == 1;
    }
}
