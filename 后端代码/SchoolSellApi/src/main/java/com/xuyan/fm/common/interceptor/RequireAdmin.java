package com.xuyan.fm.common.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要管理员权限的接口。
 *
 * 加在 Controller 方法上，由 AuthInterceptor 统一校验，
 * 替代原先散落在各个方法里的 session.getAttribute("admin") 判空校验。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
