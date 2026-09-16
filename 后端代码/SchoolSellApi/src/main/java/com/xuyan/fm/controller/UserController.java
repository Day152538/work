package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.utils.JwtUtil;
import com.xuyan.fm.common.utils.LoginRateLimiter;
import com.xuyan.fm.common.utils.ParamCheck;
import com.xuyan.fm.model.UserModel;
import com.xuyan.fm.service.CaptchaService;
import com.xuyan.fm.service.UserService;
import com.xuyan.fm.vo.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户接口。
 *
 * 整改点（对应分析报告 P0-1/P0-2/P0-4/P0-5）：
 * 1. 登录改为 POST + JWT：不再下发可伪造的明文 shUserId Cookie，
 *    身份凭证改为服务端签名的 token（12 小时有效，可配置）。
 * 2. 修改资料/改密码从“信任 Cookie 里的 userId”改为 UserContext（JWT 解出），
 *    用户只能改自己的资料，越权面被彻底关闭。
 * 3. 删除了两个完全无鉴权的危险接口：
 *    - DELETE /user/delete/{id}（任何人可删任意用户）→ 移到 /admin/user/delete/{id}
 *    - POST  /user/reset1-password（任何人可改任意用户密码）→ 移到 /admin/user/reset-password
 * 4. 修改密码接口由 GET 改为 POST，避免密码出现在 URL/访问日志里。
 * 5. /user/me 保留用于前端刷新后恢复登录态，鉴权统一交给 AuthInterceptor。
 * 6. 新增 /user/vip/subscribe：会员开通改为后端写入 vip_expire_time，
 *    修复前端“假支付直接改 userStatus=3”的问题。
 */
@RestController
@RequestMapping("user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CaptchaService captchaService;
    private final LoginRateLimiter loginRateLimiter;

    public UserController(UserService userService, JwtUtil jwtUtil, CaptchaService captchaService,
                          LoginRateLimiter loginRateLimiter) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.captchaService = captchaService;
        this.loginRateLimiter = loginRateLimiter;
    }

    /**
     * 注册账号（需要先通过图形验证码，验证码不区分大小写、一次性）
     * 格式校验（手机号/密码/昵称）在服务端权威执行，与前端规则一致，防绕过。
     */
    @PostMapping("sign-in")
    public ResultVo signIn(@RequestParam("accountNumber") @NotEmpty @NotNull String accountNumber,
                           @RequestParam("userPassword") @NotEmpty @NotNull String userPassword,
                           @RequestParam("nickname") @NotEmpty @NotNull String nickname,
                           @RequestParam("captchaId") @NotEmpty @NotNull String captchaId,
                           @RequestParam("captchaCode") @NotEmpty @NotNull String captchaCode,
                           HttpServletRequest request) {
        // 注册同样按「IP」限流：连续失败 5 次锁 10 分钟，防止脚本批量刷号
        String limiterKey = "reg|" + clientIp(request);
        if (loginRateLimiter.isLocked(limiterKey)) {
            return ResultVo.fail(ErrorMsg.LOGIN_TOO_MANY_ATTEMPTS);
        }
        // 服务端格式校验（前端可绕过，后端是最终防线）
        if (!ParamCheck.validPhone(accountNumber)) {
            return ResultVo.fail(ErrorMsg.PHONE_FORMAT_ERROR);
        }
        if (!ParamCheck.validPassword(userPassword)) {
            return ResultVo.fail(ErrorMsg.PASSWORD_FORMAT_ERROR);
        }
        if (!ParamCheck.validNickname(nickname)) {
            return ResultVo.fail(ErrorMsg.NICKNAME_FORMAT_ERROR);
        }
        if (!captchaService.verify(captchaId, captchaCode)) {
            loginRateLimiter.recordFail(limiterKey);
            return ResultVo.fail(ErrorMsg.CAPTCHA_ERROR);
        }
        UserModel userModel = new UserModel();
        userModel.setAccountNumber(accountNumber.trim());
        userModel.setUserPassword(userPassword);
        userModel.setNickname(nickname.trim());
        userModel.setSignInTime(new Timestamp(System.currentTimeMillis()));
        if (userModel.getAvatar() == null || userModel.getAvatar().isEmpty()) {
            userModel.setAvatar("/image?imageName=background01.jpg");
        }
        if (userService.userSignIn(userModel)) {
            // 注册成功即返回 token，前端无需再登录一次
            loginRateLimiter.clear(limiterKey);
            Map<String, Object> data = new HashMap<>();
            data.put("user", userModel);
            data.put("token", jwtUtil.generateToken(userModel.getId(), userModel.getAccountNumber(), UserContext.ROLE_USER));
            return ResultVo.success(data);
        }
        loginRateLimiter.recordFail(limiterKey);
        return ResultVo.fail(ErrorMsg.REGISTER_ERROR);
    }

    /**
     * 登录（POST + 账号密码 + 验证码，返回 JWT；验证码不区分大小写、一次性）
     * 频率限制：按「账号+IP」连续失败 5 次锁定 10 分钟，防暴力破解。
     */
    @PostMapping("login")
    public ResultVo login(@RequestParam("accountNumber") @NotEmpty @NotNull String accountNumber,
                          @RequestParam("userPassword") @NotEmpty @NotNull String userPassword,
                          @RequestParam("captchaId") @NotEmpty @NotNull String captchaId,
                          @RequestParam("captchaCode") @NotEmpty @NotNull String captchaCode,
                          HttpServletRequest request) {
        if (!ParamCheck.validPhone(accountNumber)) {
            return ResultVo.fail(ErrorMsg.PHONE_FORMAT_ERROR);
        }
        String limiterKey = accountNumber.trim() + "|" + clientIp(request);
        if (loginRateLimiter.isLocked(limiterKey)) {
            return ResultVo.fail(ErrorMsg.LOGIN_TOO_MANY_ATTEMPTS);
        }
        if (!captchaService.verify(captchaId, captchaCode)) {
            loginRateLimiter.recordFail(limiterKey);
            return ResultVo.fail(ErrorMsg.CAPTCHA_ERROR);
        }
        UserModel userModel = userService.userLogin(accountNumber.trim(), userPassword);
        if (null == userModel) {
            loginRateLimiter.recordFail(limiterKey);
            return ResultVo.fail(ErrorMsg.EMAIL_LOGIN_ERROR);
        }
        if (userModel.getUserStatus() != null && userModel.getUserStatus().equals((byte) 1)) {
            // 账号被禁：不计入密码错误次数，但也不清零
            return ResultVo.fail(ErrorMsg.ACCOUNT_Ban);
        }
        // 登录成功：清零失败计数并下发 token
        loginRateLimiter.clear(limiterKey);
        String token = jwtUtil.generateToken(userModel.getId(), userModel.getAccountNumber(), UserContext.ROLE_USER);
        Map<String, Object> data = new HashMap<>();
        data.put("user", userModel);
        data.put("token", token);
        return ResultVo.success(data);
    }

    /**
     * 凭 token 获取当前登录用户（前端刷新后恢复登录态）
     */
    @GetMapping("me")
    public ResultVo me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ResultVo.fail(ErrorMsg.COOKIE_ERROR);
        }
        UserModel user = userService.getUser(userId);
        if (user == null) {
            return ResultVo.fail(ErrorMsg.COOKIE_ERROR);
        }
        if (user.getUserStatus() != null && user.getUserStatus().equals((byte) 1)) {
            return ResultVo.fail(ErrorMsg.ACCOUNT_Ban);
        }
        return ResultVo.success(user);
    }

    /**
     * 退出登录：JWT 无状态，前端丢弃 token 即可，服务端无需处理
     */
    @PostMapping("logout")
    public ResultVo logout() {
        return ResultVo.success();
    }

    /**
     * 获取用户公开信息（昵称、头像等，供商品详情页展示卖家）
     */
    @GetMapping("info")
    public ResultVo getOneUser(@RequestParam Long id) {
        return ResultVo.success(userService.getUser(id));
    }

    /**
     * 修改当前用户公开信息（只能改自己的）
     */
    @PostMapping("/info")
    public ResultVo updateUserPublicInfo(@RequestBody UserModel userModel) {
        // 昵称若被提交，服务端同样做格式校验（trim 后非空且 ≤30）
        if (userModel.getNickname() != null && !ParamCheck.validNickname(userModel.getNickname())) {
            return ResultVo.fail(ErrorMsg.NICKNAME_FORMAT_ERROR);
        }
        if (userModel.getNickname() != null) {
            userModel.setNickname(userModel.getNickname().trim());
        }
        Long userId = UserContext.getUserId();
        // 关键：id 取自 JWT 上下文而非请求体，防止越权改他人资料
        userModel.setId(userId);
        userModel.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        // userStatus（封禁标记）不允许用户自己改
        userModel.setUserStatus(null);
        if (userService.updateUserInfo(userModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 修改密码（POST，避免密码出现在 URL；旧密码服务层校验；新密码服务端强度校验）
     */
    @PostMapping("/password")
    public ResultVo updateUserPassword(@RequestParam("oldPassword") @NotEmpty @NotNull String oldPassword,
                                       @RequestParam("newPassword") @NotEmpty @NotNull String newPassword) {
        if (!ParamCheck.validPassword(newPassword)) {
            return ResultVo.fail(ErrorMsg.PASSWORD_FORMAT_ERROR);
        }
        Long userId = UserContext.getUserId();
        if (userService.updatePassword(newPassword, oldPassword, userId)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.PASSWORD_RESET_ERROR);
    }

    /**
     * 凭 账号+邮箱 找回密码（未登录场景，已在拦截器白名单中）
     * 服务端校验手机号/邮箱格式与新密码强度，防绕过。
     */
    @PostMapping("/reset-password")
    public ResultVo resetPasswordByPhoneOrEmail(@RequestParam("accountNumber") @NotEmpty @NotNull String accountNumber,
                                                @RequestParam("userEmail") @NotEmpty @NotNull String userEmail,
                                                @RequestParam("newPassword") @NotEmpty @NotNull String newPassword) {
        if (!ParamCheck.validPhone(accountNumber)) {
            return ResultVo.fail(ErrorMsg.PHONE_FORMAT_ERROR);
        }
        if (!ParamCheck.validEmail(userEmail)) {
            return ResultVo.fail(ErrorMsg.EMAIL_FORMAT_ERROR);
        }
        if (!ParamCheck.validPassword(newPassword)) {
            return ResultVo.fail(ErrorMsg.PASSWORD_FORMAT_ERROR);
        }
        if (userService.resetPassword(accountNumber.trim(), userEmail.trim(), newPassword)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.PASSWORD_RESET_ERROR);
    }

    /**
     * 开通/续费会员（30 天）。原先前端直接改 userStatus=3 模拟支付，
     * 现由后端写入 vip_expire_time，且不触碰封禁字段。
     */
    @PostMapping("/vip/subscribe")
    public ResultVo subscribeVip() {
        Long userId = UserContext.getUserId();
        if (userService.subscribeVip(userId)) {
            return ResultVo.success(userService.getUser(userId));
        }
        return ResultVo.fail(ErrorMsg.VIP_SUBSCRIBE_ERROR);
    }

    /**
     * 提取客户端 IP（兼容常见反向代理头，取第一个非 unknown 值）
     */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
