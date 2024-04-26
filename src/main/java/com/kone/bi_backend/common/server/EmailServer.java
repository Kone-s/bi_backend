package com.kone.bi_backend.common.server;

import com.kone.bi_backend.common.exception.CustomizeException;
import com.kone.bi_backend.common.response.ErrorCode;
import org.springframework.mail.SimpleMailMessage;
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

    public void sendEmailCaptcha(String userEmail, String captcha) {
        //SimpleMailMessage message = new SimpleMailMessage();
        //message.setFrom(nickname + '<' + sender + '>');
        //message.setTo(userEmail);
        //message.setSubject("欢迎访问 BI 平台");
        //String content = "【验证码】您的验证码为：" + captcha + " 。 验证码五分钟内有效，逾期作废。";
        //message.setText(content);
        //mailSender.send(message);

        Context context = new Context();
        context.setVariable("verifyCode", Arrays.asList(captcha.split("")));
        String emailContent = templateEngine.process("email", context);
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
