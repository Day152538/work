package com.xuyan.fm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员实体。
 * 整改点：密码字段加 @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)，不再随接口下发。
 */
@Data
public class AdminModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 自增主键 */
    private Long id;

    /** 管理员账号 */
    @NotEmpty
    @NotNull
    private String accountNumber;

    /** 密码（BCrypt 哈希，永不下发前端） */
    @NotNull
    @NotEmpty
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String adminPassword;

    /** 管理员名字 */
    private String adminName;
}
