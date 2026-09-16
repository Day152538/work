package com.xuyan.fm.model;

import lombok.Data;

/**
 * 管理端「反馈信息」列表展示对象：
 * sh_user_message 与 sh_user 联查，带出留言内容与留言用户昵称/账号。
 */
@Data
public class AdminFeedbackVO {
    private Long id;
    /** 反馈内容（对应 sh_user_message.text） */
    private String adminMessage;
    private String nickname;
    private String accountNumber;
}
