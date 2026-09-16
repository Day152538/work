package com.xuyan.fm.service;

import com.xuyan.fm.model.AdminFeedbackVO;
import com.xuyan.fm.model.UserMessageModel;

import java.util.List;

public interface UserMessageService {
    List<UserMessageModel> listByUserId(Long userId);
    void addUserMessage(UserMessageModel userMessageModel);
    boolean deleteUserMessage(Long userId, Long id);
    boolean updateUserMessage(UserMessageModel userMessageModel);

    /** 管理端：列出所有用户的反馈（联查昵称/账号） */
    List<AdminFeedbackVO> listAllForAdmin();

    /** 管理端：按 id 删除任意用户的反馈 */
    boolean deleteByIdForAdmin(Long id);
}
