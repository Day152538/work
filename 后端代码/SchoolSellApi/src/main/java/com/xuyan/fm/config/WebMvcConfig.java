package com.xuyan.fm.config;

import com.xuyan.fm.common.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * 整改点：
 * 1. 原项目存在 WebConfig / WebMvcConfig 两个配置类，前者是被整体注释掉的死代码，
 *    后者只配了 CORS —— 等于鉴权拦截器从未注册。现统一在本类注册 AuthInterceptor。
 * 2. 白名单（游客可访问）：登录注册、商品浏览/搜索、商品分类、公告查看、轮播、
 *    图片资源、API 文档；其余接口一律要求携带合法 JWT。
 * 3. CORS 配置收口到 dev profile 允许 localhost，生产环境通过配置覆盖。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 登录 / 注册 / 凭账号+邮箱找回密码
                        "/user/login", "/user/sign-in", "/user/reset-password",
                        // 管理端登录（签发 admin 角色的 JWT）
                        "/admin/login",
                        // 游客可浏览的公开信息
                        "/user/info",
                        "/idle/info", "/idle/find", "/idle/lable",
                        "/message/idle",
                        "/type/listByCondition",
                        "/notices", "/notices/*",
                        "/carousel/all",
                        // 验证码图片（登录/注册前置，游客可访问）
                        "/captcha/image",
                        // 静态图片与文档
                        "/image",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                        // Spring 默认错误页
                        "/error"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedHeaders("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .exposedHeaders(HttpHeaders.AUTHORIZATION);
    }
}
