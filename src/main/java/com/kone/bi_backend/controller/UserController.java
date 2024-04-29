package com.kone.bi_backend.controller;

import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.utils.ThrowUtils;
import com.kone.bi_backend.common.response.BaseResponse;
import com.kone.bi_backend.common.response.ErrorCode;
import com.kone.bi_backend.common.utils.ResultUtils;
import com.kone.bi_backend.model.dto.user.*;
import com.kone.bi_backend.model.entity.User;
import com.kone.bi_backend.model.vo.LoginUserVO;
import com.kone.bi_backend.service.AvatarService;
import com.kone.bi_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private AvatarService avatarService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册请求体
     * @return 用户的信息
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        String nickname = userRegisterRequest.getNickname();
        String userEmail = userRegisterRequest.getUserEmail();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String captcha = userRegisterRequest.getCaptcha();
        if (StringUtils.isAnyBlank(nickname)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "昵称为空");
        }
        if (StringUtils.isAnyBlank(userEmail)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱为空");
        }
        if (StringUtils.isAnyBlank(userPassword, checkPassword)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱或密码为空");
        }
        if (StringUtils.isAnyBlank(captcha)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "验证码为空");
        }
        long result = userService.userRegister(nickname, userEmail, userPassword, checkPassword, captcha);
        return ResultUtils.success(result);
    }

    /**
     * 用户忘记密码
     *
     * @param userForgetRequest 忘记密码请求体
     * @return 用户的信息
     */
    @PostMapping("/forget")
    public BaseResponse<Boolean> userRegister(@RequestBody UserForgetRequest userForgetRequest) {
        if (userForgetRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        String userEmail = userForgetRequest.getUserEmail();
        String userPassword = userForgetRequest.getUserPassword();
        String checkPassword = userForgetRequest.getCheckPassword();
        String captcha = userForgetRequest.getCaptcha();
        if (StringUtils.isAnyBlank(userEmail)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱为空");
        }
        if (StringUtils.isAnyBlank(userPassword, checkPassword)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱或密码为空");
        }
        if (StringUtils.isAnyBlank(captcha)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "验证码为空");
        }
        boolean result = userService.userForget(userEmail, userPassword, checkPassword, captcha);
        return ResultUtils.success(result);
    }

    /**
     * 邮箱注册验证码接口
     *
     * @param userCaptchaRequest
     * @return
     */
    @PostMapping("/register/captcha")
    public BaseResponse<Boolean> sendRegisterCaptcha(@RequestBody UserCaptchaRequest userCaptchaRequest) {
        String userEmail = userCaptchaRequest.getUserEmail();
        boolean result = userService.sendRegisterCaptcha(userEmail);
        return ResultUtils.success(result);
    }

    /**
     * 忘记密码验证码接口
     *
     * @param userCaptchaRequest
     * @return
     */
    @PostMapping("/forget/captcha")
    public BaseResponse<Boolean> sendForgetCaptcha(@RequestBody UserCaptchaRequest userCaptchaRequest) {
        String userEmail = userCaptchaRequest.getUserEmail();
        boolean result = userService.sendForgetCaptcha(userEmail);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求参数
     * @param request          请求
     * @return 用户登录状态
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserEmail();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 成功信息
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 当前登录信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    /**
     * 更新头像
     *
     * @param multipartFile 头像文件流
     * @param request       请求
     * @return 成功信息
     */
    @PostMapping("/update/myAvatar")
    public BaseResponse<Boolean> updateMyAvatar(@RequestPart("file") MultipartFile multipartFile, HttpServletRequest request) {
        if (multipartFile == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        String userAvatar = avatarService.upload(multipartFile);
        user.setId(loginUser.getId());
        user.setUserAvatar(userAvatar);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest 更新请求参数
     * @param request             请求
     * @return 成功信息
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
