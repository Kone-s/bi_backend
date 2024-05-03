package com.kone.bi_backend.controller;

import com.kone.bi_backend.common.response.BaseResponse;
import com.kone.bi_backend.common.utils.ResultUtils;
import com.kone.bi_backend.model.dto.user.UserCaptchaRequest;
import com.kone.bi_backend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 验证码接口
 *
 * @Author Kone
 * @Date 2024/5/3
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {
    @Resource
    private UserService userService;

    /**
     * 邮箱注册验证码接口
     *
     * @param userCaptchaRequest 获取验证码请求
     */
    @PostMapping("/register")
    public BaseResponse<Boolean> sendRegisterCaptcha(@RequestBody UserCaptchaRequest userCaptchaRequest) {
        String userEmail = userCaptchaRequest.getUserEmail();
        boolean result = userService.sendRegisterCaptcha(userEmail);
        return ResultUtils.success(result);
    }

    /**
     * 忘记密码验证码接口
     *
     * @param userCaptchaRequest 获取验证码请求
     */
    @PostMapping("/forget")
    public BaseResponse<Boolean> sendForgetCaptcha(@RequestBody UserCaptchaRequest userCaptchaRequest) {
        String userEmail = userCaptchaRequest.getUserEmail();
        boolean result = userService.sendForgetCaptcha(userEmail);
        return ResultUtils.success(result);
    }
}
