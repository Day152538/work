package com.xuyan.fm.controller;

import com.xuyan.fm.service.CaptchaService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 图形验证码（公开接口，登录/注册前调用一次换取验证码）。
 * 生成结果只返回 captchaId 与图片，验证码文本仅在服务端留存，不对外下发。
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /** 获取一张验证码图片：{ captchaId, imgBase64 } */
    @GetMapping("/image")
    public ResultVo<Map<String, String>> image() {
        return ResultVo.success(captchaService.createCaptcha());
    }
}
