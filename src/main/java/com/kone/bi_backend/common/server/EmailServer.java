package com.kone.bi_backend.common.server;

import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.response.ErrorCode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.util.Arrays;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class EmailServer {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    TemplateEngine templateEngine;

    // 获取发件人邮箱
    @Value("${spring.mail.username}")
    private String sender;

    // 获取发件人昵称
    @Value("${spring.mail.nickname}")
    private String nickname;

    /**
     * 发送注册邮件
     *
     * @param userEmail 用户邮箱
     * @param captcha   随机验证码
     */
    public void sendRegisterEmailCaptcha(String userEmail, String captcha) {
        Context context = new Context();
        context.setVariable("verifyCode", Arrays.asList(captcha.split("")));
        String emailContent = templateEngine.process("RegisterEmail", context);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(sender);
            helper.setFrom(nickname + '<' + sender + '>');
            helper.setTo(userEmail);
            helper.setSubject("欢迎访问 BI 平台");
            helper.setText(emailContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "邮箱发送失败");
        }
    }

    /**
     * 发送注册邮件
     *
     * @param userEmail 用户邮箱
     * @param captcha   随机验证码
     */
    public void sendForgetEmailCaptcha(String userEmail, String captcha) {
        Context context = new Context();
        context.setVariable("verifyCode", Arrays.asList(captcha.split("")));
        String emailContent = templateEngine.process("ForgetEmail", context);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(sender);
            helper.setFrom(nickname + '<' + sender + '>');
            helper.setTo(userEmail);
            helper.setSubject("欢迎访问 BI 平台");
            helper.setText(emailContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new CustomizeException(ErrorCode.SYSTEM_ERROR, "邮箱发送失败");
        }
    }
}
