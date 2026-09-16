package com.xuyan.fm.common.enums;


/**
 * @author rabbiter
 * 错误信息枚举类
 */
public enum ErrorMsg {

    ACCOUNT_EXIT("用户已存在"),
    ACCOUNT_Ban("账号已被封禁"),
    ACCOUNT_NOT_EXIT("用户不存在"),
    PASSWORD_IS_NOT_SAME("密码不一致"),
    PASSWORD_RESET_ERROR("修改密码失败"),
    EMAIL_SEND_ERROR("邮件发送失败 请重试"),
    PARAM_ERROR("参数错误"),
    SYSTEM_ERROR("系统错误"),
    REGISTER_ERROR("注册失败"),
    FILE_TYPE_ERROR("文件类型错误 请选择.jpg或.png"),
    FILE_UPLOAD_ERROR("文件上传失败"),
    FILE_NOT_EXIT("文件不存在"),
    FILE_DOWNLOAD_ERROR("文件下载异常"),
    FILE_SIZE_ERROR("文件过大"),
    OPERAT_FREQUENCY("操作频繁 稍后重试"),
    MISSING_PARAMETER("缺少参数"),
    COOKIE_ERROR("请重新登录"),
    NO_PERMISSION("无权限操作该资源"),
    VIP_SUBSCRIBE_ERROR("会员开通失败"),
    EMAIL_LOGIN_ERROR("登录失败 账号或密码错误"),
    JSON_READ_ERROR("json参数解析错误"),
    FORM_NUMBER_ERROR("表单id错误"),
    REPEAT_COMMIT_ERROR("请勿重复提交"),
    COMMIT_FAIL_ERROR("提交失败"),
    FAVORITE_EXIT("收藏已存在"),
    IDLE_ITEM_LABEL_EXIST("该分类下存在闲置商品"),
    TYPE_HAS_EXIST("分类名称已存在"),
    ORDER_NOT_EXIST("订单不存在"),
    CAPTCHA_ERROR("验证码错误或已过期"),
    SOLD_OUT_ERROR("商品已售罄"),
    AI_NOT_CONFIGURED("AI 智能预填暂不可用：服务端未配置模型 API Key"),
    AI_SERVICE_ERROR("AI 服务调用失败，请稍后重试"),
    AI_RATE_LIMITED("AI 服务繁忙或额度受限，请稍后再试"),
    AI_IMAGE_ERROR("图片读取失败，请重新上传后再试"),
    PHONE_FORMAT_ERROR("手机号码格式不正确"),
    PASSWORD_FORMAT_ERROR("密码需为6-16位，且必须同时包含字母和数字"),
    EMAIL_FORMAT_ERROR("邮箱格式不正确"),
    NICKNAME_FORMAT_ERROR("昵称不能为空且不能超过30个字符"),
    LOGIN_TOO_MANY_ATTEMPTS("登录失败次数过多，请10分钟后再试");

    private String msg;

    ErrorMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
