package com.kone.bi_backend.service.impl;


import static com.kone.bi_backend.common.constant.UserConstant.USER_LOGIN_STATE;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kone.bi_backend.common.constant.CommonConstant;
import com.kone.bi_backend.common.utils.ThrowUtils;
import com.kone.bi_backend.common.response.ErrorCode;
import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.server.EmailServer;
import com.kone.bi_backend.common.utils.CaptchaGenerateUtil;
import com.kone.bi_backend.common.utils.SqlUtils;
import com.kone.bi_backend.mapper.UserMapper;
import com.kone.bi_backend.model.dto.user.UserQueryRequest;
import com.kone.bi_backend.model.entity.Score;
import com.kone.bi_backend.model.entity.User;
import com.kone.bi_backend.model.vo.LoginUserVO;
import com.kone.bi_backend.model.vo.UserVO;
import com.kone.bi_backend.service.ScoreService;
import com.kone.bi_backend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private ScoreService scoreService;

    @Resource
    private EmailServer emailServer;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public long userRegister(String nickname, String userEmail, String userPassword, String checkPassword, String captcha) {
        // 1. 校验
        if (StringUtils.isAnyBlank(nickname, userEmail, userPassword, checkPassword)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "昵称为空");
        }
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$");
        Matcher matcher = pattern.matcher(userEmail);
        if (!matcher.find()) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userEmail.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_email", userEmail);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new CustomizeException(ErrorCode.PARAMS_ERROR, "已注册，如忘记密码请找回！");
            }

            // 从redis获取到验证码进行校验
            ValueOperations<String, String> redisOperations = redisTemplate.opsForValue();
            String redisKey = "RegisterCaptcha:" + userEmail;
            String redisCaptcha = redisOperations.get(redisKey);
            if (captcha == null || !captcha.equals(redisCaptcha)) {
                throw new CustomizeException(ErrorCode.PARAMS_ERROR, "验证码错误");
            }

            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((CommonConstant.SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setNickname(nickname);
            user.setUserEmail(userEmail);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            //注册成功后往Score表插入数据
            Score score = new Score();
            //未签到
            score.setIsSign(0);
            score.setScoreTotal(10L);
            score.setUserId(user.getId());
            boolean scoreResult = scoreService.save(score);
            ThrowUtils.throwIf(!scoreResult, ErrorCode.OPERATION_ERROR, "注册积分异常");
            return user.getId();
        }
    }

    @Override
    public boolean userForget(String userEmail, String userPassword, String checkPassword, String captcha) {
        // 1. 校验
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$");
        Matcher matcher = pattern.matcher(userEmail);
        if (!matcher.find()) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userEmail.intern()) {
            // 从redis获取到验证码进行校验
            ValueOperations<String, String> redisOperations = redisTemplate.opsForValue();
            String redisKey = "ForgetCaptcha:" + userEmail;
            String redisCaptcha = redisOperations.get(redisKey);
            if (captcha == null || !captcha.equals(redisCaptcha)) {
                throw new CustomizeException(ErrorCode.PARAMS_ERROR, "验证码错误");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((CommonConstant.SALT + userPassword).getBytes());
            // 3. 更新数据
            User user = new User();
            user.setUserPassword(encryptPassword); // 设置新的加密密码

            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("user_password", encryptPassword) // 设置要更新的字段和值
                    .eq("user_email", userEmail); // 添加更新条件

            // 调用update方法更新用户密码
            boolean updateResult = this.update(user, updateWrapper); // 注意这里传递了两个参数
            if (!updateResult) {
                throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "修改密码失败，数据库错误");
            }
            return true;

        }
    }

    @Override
    public LoginUserVO userLogin(String userEmail, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userEmail, userPassword)) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$");
        Matcher matcher = pattern.matcher(userEmail);
        if (!matcher.find()) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if (userPassword.length() < 8) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((CommonConstant.SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_email", userEmail);
        queryWrapper.eq("user_password", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new CustomizeException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new CustomizeException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }


    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new CustomizeException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public boolean sendRegisterCaptcha(String userEmail) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_email", userEmail);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
        }
        String captcha = CaptchaGenerateUtil.generateVerCode();
        try {
            long timeOut = 1000 * 60 * 5;
            ValueOperations<String, String> redisOperations = redisTemplate.opsForValue();
            String redisKey = "RegisterCaptcha:" + userEmail;
            redisOperations.set(redisKey, captcha, timeOut, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "缓存失败");
        }
        //发送验证码
        emailServer.sendRegisterEmailCaptcha(userEmail, captcha);
        return true;
    }

    @Override
    public boolean sendForgetCaptcha(String userEmail) {
        String captcha = CaptchaGenerateUtil.generateVerCode();
        try {
            long timeOut = 1000 * 60 * 5;
            ValueOperations<String, String> redisOperations = redisTemplate.opsForValue();
            String redisKey = "ForgetCaptcha:" + userEmail;
            redisOperations.set(redisKey, captcha, timeOut, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "缓存失败");
        }
        //发送验证码
        emailServer.sendForgetEmailCaptcha(userEmail, captcha);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new CustomizeException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String nickname = userQueryRequest.getNickname();
        String userEmail = userQueryRequest.getUserEmail();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.like(StringUtils.isNotBlank(nickname), "nickname", nickname);
        queryWrapper.like(StringUtils.isNotBlank(userEmail), "user_email", userEmail);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }
}
