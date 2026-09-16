package com.xuyan.fm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体。
 *
 * 整改点：
 * 1. 密码字段加 @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) —— 原代码把（明文）密码原样序列化返回给前端，
 *    任何登录/查询接口的响应里都带着密码。
 * 2. 新增 vipExpireTime（会员到期时间），替代原先把 userStatus 改成 3
 *    来“开通会员”的错误设计（user_status 只表达 0=正常 / 1=封禁）。
 */
@Data
public class UserModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    private Long id;

    /** 账号（手机号） */
    private String accountNumber;

    /** 登录密码（BCrypt 哈希，永不下发前端） */
    @NotEmpty
    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String userPassword;

    /** 邮箱（用于找回密码） */
    private String userEmail;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 注册时间 */
    private Date signInTime;

    /** 更新时间 */
    private Date updateTime;

    /** 0=正常 1=封禁 */
    private Byte userStatus;

    /** 会员到期时间，NULL 表示非会员 */
    private Date vipExpireTime;
}
