package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.common.utils.JwtUtil;
import com.xuyan.fm.common.utils.LoginRateLimiter;
import com.xuyan.fm.model.AdminFeedbackVO;
import com.xuyan.fm.model.AdminModel;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.model.UserModel;
import com.xuyan.fm.service.AdminService;
import com.xuyan.fm.service.CarouselService;
import com.xuyan.fm.service.IdleItemService;
import com.xuyan.fm.service.OrderService;
import com.xuyan.fm.service.UserMessageService;
import com.xuyan.fm.service.UserService;
import com.xuyan.fm.vo.ResultVo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端接口。
 *
 * 整改点（对应分析报告 P0-2 系列越权漏洞）：
 * 1. 原登录用 HttpSession 记录 admin，其余接口各自手写 session 判空，
 *    其中 /admin/orderList 漏写校验、/admin/carouselList 校验被注释 —— 全部裸奔。
 *    现统一由 AuthInterceptor 校验 /admin/** 路径（登录除外）必须携带 admin 角色 JWT，
 *    Controller 里不再出现任何手写鉴权代码。
 * 2. 原用户模块两个无鉴权危险接口（删用户、重置任意用户密码）收编到
 *    /admin/user/delete 与 /admin/user/reset-password，纳入统一管理端鉴权。
 * 3. 登录改为 POST + JWT（role=admin），管理端与用户端共用一套令牌机制。
 */
@RestController
@RequestMapping("admin")
public class AdminController {

    private final AdminService adminService;
    private final IdleItemService idleItemService;
    private final OrderService orderService;
    private final UserService userService;
    private final CarouselService carouselService;
    private final UserMessageService userMessageService;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter loginRateLimiter;

    public AdminController(AdminService adminService,
                           IdleItemService idleItemService,
                           OrderService orderService,
                           UserService userService,
                           CarouselService carouselService,
                           UserMessageService userMessageService,
                           JwtUtil jwtUtil,
                           LoginRateLimiter loginRateLimiter) {
        this.adminService = adminService;
        this.idleItemService = idleItemService;
        this.orderService = orderService;
        this.userService = userService;
        this.carouselService = carouselService;
        this.userMessageService = userMessageService;
        this.jwtUtil = jwtUtil;
        this.loginRateLimiter = loginRateLimiter;
    }

    /**
     * 管理员登录（POST + JWT，role=admin）
     * 频率限制：按「账号+IP」连续失败 5 次锁定 10 分钟，防暴力破解（管理员账号无验证码，尤为重要）。
     */
    @PostMapping("login")
    public ResultVo login(@RequestParam("accountNumber") @NotNull @NotEmpty String accountNumber,
                          @RequestParam("adminPassword") @NotNull @NotEmpty String adminPassword,
                          jakarta.servlet.http.HttpServletRequest request) {
        String limiterKey = accountNumber.trim() + "|" + clientIp(request);
        if (loginRateLimiter.isLocked(limiterKey)) {
            return ResultVo.fail(ErrorMsg.LOGIN_TOO_MANY_ATTEMPTS);
        }
        AdminModel adminModel = adminService.login(accountNumber.trim(), adminPassword);
        if (null == adminModel) {
            loginRateLimiter.recordFail(limiterKey);
            return ResultVo.fail(ErrorMsg.EMAIL_LOGIN_ERROR);
        }
        loginRateLimiter.clear(limiterKey);
        String token = jwtUtil.generateToken(adminModel.getId(), adminModel.getAccountNumber(), UserContext.ROLE_ADMIN);
        Map<String, Object> data = new HashMap<>();
        data.put("admin", adminModel);
        data.put("token", token);
        return ResultVo.success(data);
    }

    /**
     * 退出登录（JWT 无状态，前端丢弃 token 即可）
     */
    @PostMapping("loginOut")
    public ResultVo loginOut() {
        return ResultVo.success();
    }

    /**
     * 管理员列表（以下接口均由拦截器保证 admin 角色）
     */
    @GetMapping("list")
    public ResultVo getAdminList(@RequestParam(value = "page", required = false) Integer page,
                                 @RequestParam(value = "nums", required = false) Integer nums) {
        int p = page == null ? 1 : Math.max(page, 1);
        int n = nums == null ? 8 : Math.max(nums, 1);
        return ResultVo.success(adminService.getAdminList(p, n));
    }

    @PostMapping("add")
    public ResultVo addAdmin(@RequestBody AdminModel adminModel) {
        if (adminService.addAdmin(adminModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.PARAM_ERROR);
    }

    @PostMapping("update")
    public ResultVo updateAdmin(@RequestBody AdminModel adminModel) {
        if (adminService.updateAdmin(adminModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.PARAM_ERROR);
    }

    @DeleteMapping("delete/{id}")
    public ResultVo deleteAdmin(@PathVariable Long id) {
        if (adminService.deleteAdmin(id)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.PARAM_ERROR);
    }

    /**
     * 商品列表（按状态）
     */
    @GetMapping("idleList")
    public ResultVo idleList(@RequestParam("status") @NotNull Integer status,
                             @RequestParam(value = "page", required = false) Integer page,
                             @RequestParam(value = "nums", required = false) Integer nums) {
        int p = page == null ? 1 : Math.max(page, 1);
        int n = nums == null ? 8 : Math.max(nums, 1);
        return ResultVo.success(idleItemService.adminGetIdleList(status, p, n));
    }

    /**
     * 上下架/审核商品（reason：审核未通过/违规下架原因，可选；填写后卖家可见）
     */
    @PostMapping("updateIdleStatus")
    public ResultVo updateIdleStatus(@RequestParam("id") @NotNull Long id,
                                     @RequestParam("status") @NotNull Integer status,
                                     @RequestParam(value = "reason", required = false) String reason) {
        IdleItemModel idleItemModel = new IdleItemModel();
        idleItemModel.setId(id);
        idleItemModel.setIdleStatus(status.byteValue());
        idleItemModel.setIdleReason(reason);
        if (idleItemService.adminUpdateIdleItem(idleItemModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 订单列表（修复：原接口漏写 session 校验，任何人可看全平台订单）
     */
    @GetMapping("orderList")
    public ResultVo orderList(@RequestParam(value = "page", required = false) Integer page,
                              @RequestParam(value = "nums", required = false) Integer nums) {
        int p = page == null ? 1 : Math.max(page, 1);
        int n = nums == null ? 8 : Math.max(nums, 1);
        return ResultVo.success(orderService.getAllOrder(p, n));
    }

    @PostMapping("deleteOrder")
    public ResultVo deleteOrder(@RequestParam("id") @NotNull Long id) {
        if (orderService.deleteOrder(id)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 用户列表（按状态）
     */
    @GetMapping("userList")
    public ResultVo userList(@RequestParam(value = "page", required = false) Integer page,
                             @RequestParam(value = "nums", required = false) Integer nums,
                             @RequestParam("status") @NotNull Integer status) {
        int p = page == null ? 1 : Math.max(page, 1);
        int n = nums == null ? 8 : Math.max(nums, 1);
        return ResultVo.success(userService.getUserByStatus(status, p, n));
    }

    /**
     * 轮播图列表
     */
    @GetMapping("carouselList")
    public ResultVo carouselList() {
        return ResultVo.success(carouselService.getAllCarousels());
    }

    /**
     * 反馈信息列表：所有用户提交的反馈（sh_user_message + 用户昵称/账号）。
     * 说明：用户在前台“信息反馈”写入的是 sh_user_message（/userMessage/add），
     * 管理员从这里统一查看（原“反馈信息”误读 sh_admin_message，用户反馈进不到这张表）。
     */
    @GetMapping("userMessage/list")
    public ResultVo<List<AdminFeedbackVO>> userMessageList() {
        return ResultVo.success(userMessageService.listAllForAdmin());
    }

    /**
     * 删除某条用户反馈（管理端，按 id）
     */
    @DeleteMapping("userMessage/delete/{id}")
    public ResultVo deleteUserMessage(@PathVariable Long id) {
        if (userMessageService.deleteByIdForAdmin(id)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 封禁/解封用户
     */
    @PostMapping("updateUserStatus")
    public ResultVo updateUserStatus(@RequestParam("id") @NotNull Long id,
                                     @RequestParam("status") @NotNull Integer status) {
        UserModel userModel = new UserModel();
        userModel.setId(id);
        userModel.setUserStatus(status.byteValue());
        if (userService.updateUserInfo(userModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    /**
     * 管理员重置用户密码（原 /user/reset1-password 无任何鉴权，已收编至此）
     */
    @PostMapping("user/reset-password")
    public ResultVo adminResetUserPassword(@RequestParam("id") @NotNull Long id,
                                           @RequestParam("newPassword") @NotEmpty @NotNull String newPassword) {
        if (userService.adminResetPassword(id, newPassword)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.PASSWORD_RESET_ERROR);
    }

    /**
     * 管理员删除用户（原 /user/delete/{id} 无任何鉴权，已收编至此）
     */
    @DeleteMapping("user/delete/{id}")
    public ResultVo adminDeleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResultVo.success();
    }

    /**
     * 提取客户端 IP（兼容常见反向代理头，取第一个非 unknown 值）
     */
    private String clientIp(jakarta.servlet.http.HttpServletRequest request) {
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
