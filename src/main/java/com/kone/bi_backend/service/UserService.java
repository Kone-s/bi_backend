package com.kone.bi_backend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kone.bi_backend.model.dto.user.UserQueryRequest;
import com.kone.bi_backend.model.entity.User;
import com.kone.bi_backend.model.vo.LoginUserVO;
import com.kone.bi_backend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务类
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param nickname      昵称
     * @param userEmail     邮箱
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String nickname, String userEmail, String userPassword, String checkPassword, String captcha);


    /**
     * 忘记密码
     *
     * @param userEmail     邮箱
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param captcha       验证码
     * @return 是否更新成功
     */
    boolean userForget(String userEmail, String userPassword, String checkPassword, String captcha);

    /**
     * 用户登录
     *
     * @param userEmail    邮箱
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userEmail, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);


    /**
     * 获取注册校验码
     *
     * @param userEmail
     */
    boolean sendRegisterCaptcha(String userEmail);

    /**
     * 获取忘记密码校验码
     *
     * @param userEmail
     */
    boolean sendForgetCaptcha(String userEmail);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

}
