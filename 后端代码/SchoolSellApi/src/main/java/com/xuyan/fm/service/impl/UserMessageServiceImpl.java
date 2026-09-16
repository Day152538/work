package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.UserMessageDao;
import com.xuyan.fm.model.AdminFeedbackVO;
import com.xuyan.fm.model.UserMessageModel;
import com.xuyan.fm.service.UserMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户留言板服务实现。
 * 整改点：删除/修改改为带属主条件的 SQL，只能操作自己的留言（原接口无任何校验）。
 */
@Service
public class UserMessageServiceImpl implements UserMessageService {

    private final UserMessageDao userMessageDao;

    public UserMessageServiceImpl(UserMessageDao userMessageDao) {
        this.userMessageDao = userMessageDao;
    }

    @Override
    public List<UserMessageModel> listByUserId(Long userId) {
        return userMessageDao.listByUserId(userId);
    }

    @Override
    public void addUserMessage(UserMessageModel userMessageModel) {
        userMessageDao.addUserMessage(userMessageModel);
    }

    @Override
    public boolean deleteUserMessage(Long userId, Long id) {
        return userMessageDao.deleteByIdAndUser(id, userId);
    }

    @Override
    public boolean updateUserMessage(UserMessageModel userMessageModel) {
        return userMessageDao.updateByIdAndUser(userMessageModel);
    }

    @Override
    public List<AdminFeedbackVO> listAllForAdmin() {
        return userMessageDao.listAllForAdmin();
    }

    @Override
    public boolean deleteByIdForAdmin(Long id) {
        return userMessageDao.deleteByIdForAdmin(id) > 0;
    }
}
