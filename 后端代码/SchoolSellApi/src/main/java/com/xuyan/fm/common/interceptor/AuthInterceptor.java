package com.xuyan.fm.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.utils.JwtUtil;
import com.xuyan.fm.vo.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 统一鉴权拦截器。
 *
 * 整改点（对应分析报告 P0-1/P0-2/P0-8）：
 * 1. 原项目鉴权依赖未签名的明文 Cookie shUserId，任何人改一下 Cookie 即可冒充他人；
 *    现统一校验请求头 Authorization 中的 JWT 签名，通过后把用户信息写入 ThreadLocal 上下文。
 * 2. 原拦截器被整体注释掉、从未生效；现由 WebMvcConfig 注册并配置白名单。
 * 3. /admin/** 路径与标注 @RequireAdmin 的方法要求 role=admin，修复原管理端
 *    大量接口（删商品、删留言、公告/轮播增删改、/admin/orderList 等）完全裸奔的问题。
 * 4. 请求结束后清理 ThreadLocal，避免线程池复用导致用户信息串号。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 CORS 预检请求
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || token.isEmpty()) {
            return reject(response, ErrorMsg.COOKIE_ERROR);
        }

        UserContext.LoginUser loginUser;
        try {
            loginUser = jwtUtil.parseUser(token);
        } catch (Exception e) {
            // 无效/过期/被篡改的 token 一律按未登录处理
            return reject(response, ErrorMsg.COOKIE_ERROR);
        }

        // 管理端接口（/admin/**，登录除外）与管理员注解接口要求 admin 角色
        boolean isAdminPath = request.getRequestURI().startsWith("/admin")
                && !request.getRequestURI().startsWith("/admin/login");
        boolean requireAdminAnnotation = false;
        if (handler instanceof HandlerMethod handlerMethod) {
            requireAdminAnnotation = handlerMethod.hasMethodAnnotation(RequireAdmin.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(RequireAdmin.class);
        }
        if ((isAdminPath || requireAdminAnnotation) && !UserContext.ROLE_ADMIN.equals(loginUser.getRole())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeJson(response, ResultVo.fail(ErrorMsg.NO_PERMISSION));
            return false;
        }

        UserContext.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 支持 "Authorization: <token>" 与 "Authorization: Bearer <token>" 两种格式
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        if (header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return header.trim();
    }

    private boolean reject(HttpServletResponse response, ErrorMsg errorMsg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        writeJson(response, ResultVo.fail(errorMsg));
        return false;
    }

    private void writeJson(HttpServletResponse response, ResultVo<?> resultVo) throws Exception {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(resultVo));
    }
}
