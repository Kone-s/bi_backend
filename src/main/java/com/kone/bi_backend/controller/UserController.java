package com.kone.bi_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kone.bi_backend.common.constant.CommonConstant;
import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.utils.ThrowUtils;
import com.kone.bi_backend.common.response.BaseResponse;
import com.kone.bi_backend.common.response.ErrorCode;
import com.kone.bi_backend.common.utils.ResultUtils;
import com.kone.bi_backend.model.dto.user.*;
import com.kone.bi_backend.model.entity.Score;
import com.kone.bi_backend.model.entity.User;
import com.kone.bi_backend.model.vo.LoginUserVO;
import com.kone.bi_backend.service.AvatarService;
import com.kone.bi_backend.service.ScoreService;
import com.kone.bi_backend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private AvatarService avatarService;

    @Resource
    private ScoreService scoreService;

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

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        String encryptPassword = DigestUtils.md5DigestAsHex((CommonConstant.SALT + userUpdateRequest.getUserPassword()).getBytes());
        userUpdateRequest.setUserPassword(encryptPassword);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 创建用户
     *
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR);
        }
        String encryptPassword = DigestUtils.md5DigestAsHex((CommonConstant.SALT + userUpdateRequest.getUserPassword()).getBytes());
        userUpdateRequest.setUserPassword(encryptPassword);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.save(user);
        //注册成功后往Score表插入数据
        Score score = new Score();
        //未签到
        score.setIsSign(0);
        score.setScoreTotal(10L);
        score.setUserId(user.getId());
        boolean scoreResult = scoreService.save(score);
        ThrowUtils.throwIf(!scoreResult, ErrorCode.OPERATION_ERROR, "注册积分异常");
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }
}
