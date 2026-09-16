package com.xuyan.fm.service.impl;

import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.exception.BusinessException;
import com.xuyan.fm.dao.UserDao;
import com.xuyan.fm.model.UserModel;
import com.xuyan.fm.service.UserService;
import com.xuyan.fm.vo.PageVo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 用户服务实现。
 *
 * 整改点：
 * 1. P0-3 密码安全：注册即 BCrypt 哈希；登录不再 SQL 明文比对，改为哈希校验；
 *    对存量明文密码做兼容（首次登录成功后自动升级为哈希，实现平滑迁移）。
 * 2. 注册查重：原代码 insert 撞唯一索引直接抛异常返回“注册失败”，
 *    现先按账号查询给出明确提示（账号已存在）。
 * 3. 会员功能：subscribeVip 在后端写入 vip_expire_time（30 天），
 *    修复原前端“点确认支付=直接改自己的 userStatus=3”的假支付漏洞
 *    （该字段同时承担封禁标记，用户改 3 还会顺带“解封”自己）。
 * 4. 修改密码：旧密码在服务层校验，DAO 只按主键更新新哈希。
 */
@Service
public class UserServiceImpl implements UserService {

    /** BCrypt 哈希固定以 $2 开头，用于区分存量明文密码 */
    private static final String BCRYPT_PREFIX = "$2";

    /** 会员时长：30 天 */
    private static final int VIP_DAYS = 30;

    @Resource
    private UserDao userDao;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public UserModel getUser(Long id) {
        return userDao.selectByPrimaryKey(id);
    }

    @Override
    public UserModel userLogin(String accountNumber, String userPassword) {
        UserModel user = userDao.selectByAccountNumber(accountNumber);
        if (user == null || user.getUserPassword() == null) {
            return null;
        }
        String stored = user.getUserPassword();
        if (stored.startsWith(BCRYPT_PREFIX)) {
            // 新格式：BCrypt 校验
            if (!passwordEncoder.matches(userPassword, stored)) {
                return null;
            }
        } else {
            // 存量明文密码：比对成功后立即升级为 BCrypt（一次性迁移）
            if (!stored.equals(userPassword)) {
                return null;
            }
            userDao.updatePassword(passwordEncoder.encode(userPassword), user.getId());
        }
        // 密码不出服务层
        user.setUserPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean userSignIn(UserModel userModel) {
        if (userDao.selectByAccountNumber(userModel.getAccountNumber()) != null) {
            throw new BusinessException(ErrorMsg.ACCOUNT_EXIT);
        }
        // 密码哈希后入库
        userModel.setUserPassword(passwordEncoder.encode(userModel.getUserPassword()));
        return userDao.insert(userModel) == 1;
    }

    @Override
    public boolean updateUserInfo(UserModel userModel) {
        return userDao.updateByPrimaryKeySelective(userModel) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(String newPassword, String oldPassword, Long id) {
        UserModel user = userDao.selectByPrimaryKey(id);
        if (user == null) {
            return false;
        }
        // 按账号取哈希做校验（selectByPrimaryKey 不查密码列）
        UserModel withPassword = userDao.selectByAccountNumber(user.getAccountNumber());
        String stored = withPassword.getUserPassword();
        boolean oldMatch;
        if (stored.startsWith(BCRYPT_PREFIX)) {
            oldMatch = passwordEncoder.matches(oldPassword, stored);
        } else {
            oldMatch = stored.equals(oldPassword);
        }
        if (!oldMatch) {
            return false;
        }
        return userDao.updatePassword(passwordEncoder.encode(newPassword), id) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean subscribeVip(Long userId) {
        UserModel user = userDao.selectByPrimaryKey(userId);
        if (user == null) {
            throw new BusinessException(ErrorMsg.ACCOUNT_NOT_EXIT);
        }
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        // 已是会员则从当前到期时间续期，否则从现在起算
        if (user.getVipExpireTime() != null && user.getVipExpireTime().after(now)) {
            calendar.setTime(user.getVipExpireTime());
        }
        calendar.add(Calendar.DAY_OF_MONTH, VIP_DAYS);

        UserModel update = new UserModel();
        update.setId(userId);
        update.setVipExpireTime(calendar.getTime());
        update.setUpdateTime(now);
        return userDao.updateByPrimaryKeySelective(update) == 1;
    }

    @Override
    public PageVo<UserModel> getUserByStatus(int status, int page, int nums) {
        List<UserModel> list;
        int count;
        if (status == 0) {
            count = userDao.countNormalUser();
            list = userDao.getNormalUser((page - 1) * nums, nums);
        } else {
            count = userDao.countBanUser();
            list = userDao.getBanUser((page - 1) * nums, nums);
        }
        return new PageVo<>(list, count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String accountNumber, String userEmail, String newPassword) {
        UserModel user = userDao.findByPhoneNumberAndEmail(accountNumber, userEmail);
        if (user == null) {
            return false;
        }
        return userDao.updatePassword(passwordEncoder.encode(newPassword), user.getId()) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adminResetPassword(Long id, String newPassword) {
        UserModel user = userDao.selectByPrimaryKey(id);
        if (user == null) {
            return false;
        }
        return userDao.updatePassword(passwordEncoder.encode(newPassword), id) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        userDao.deleteByPrimaryKey(id);
    }
}
