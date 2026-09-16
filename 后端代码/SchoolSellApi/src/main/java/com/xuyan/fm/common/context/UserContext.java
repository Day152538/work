package com.xuyan.fm.common.context;

/**
 * 当前登录用户上下文（ThreadLocal）。
 *
 * 由 AuthInterceptor 在请求进入 Controller 前写入、请求结束时清理，
 * 业务代码通过 UserContext.getUserId() 获取当前登录用户，
 * 彻底替代原先“从 Cookie 里读明文 shUserId”的不安全做法。
 */
public class UserContext {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static String getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息（来自 JWT claims，签名保证不可伪造）
     */
    public static class LoginUser {
        private final Long userId;
        private final String accountNumber;
        private final String role;

        public LoginUser(Long userId, String accountNumber, String role) {
            this.userId = userId;
            this.accountNumber = accountNumber;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public String getRole() {
            return role;
        }
    }
}
