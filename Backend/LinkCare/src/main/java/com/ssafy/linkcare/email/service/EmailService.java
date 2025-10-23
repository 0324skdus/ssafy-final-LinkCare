package com.ssafy.linkcare.email.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /*
        * 이메일 인증 코드 발송
        * @param toEmail 수신자 이메일
        * @param verificationCode 인증 코드
     */
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 수신자
            helper.setTo(toEmail);

            // 제목
            helper.setSubject("[LinkCare] 이메일 인증 코드");

            // 내용 (HTML)
            String htmlContent = buildEmailContent(verificationCode);
            helper.setText(htmlContent, true);

            // 발송
            mailSender.send(message);

            log.info("이메일 발송 완료: {}", toEmail);

        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    // 이메일 HTML 내용 생성
    private String buildEmailContent(String verificationCode) {
        return """
                <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>
                    <h2 style='color: #ff5757;'>LinkCare 이메일 인증</h2>
                    <p>안녕하세요,</p>
                    <p>LinkCare 회원가입을 위한 인증 코드입니다.</p>

                    <div style='background-color: #f5f5f5; padding: 20px; margin: 20px 0; text-align: center;'>
                        <h1 style='color: #ff5757; margin: 0; letter-spacing: 5px;'>%s</h1>
                    </div>

                    <p>위 인증 코드를 입력하여 회원가입을 완료해주세요.</p>
                    <p style='color: #999; font-size: 12px;'>
                      * 이 인증 코드는 5분간 유효합니다.<br>
                      * 본인이 요청하지 않았다면 이 이메일을 무시해주세요.
                    </p>

                    <hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>
                    <p style='color: #999; font-size: 12px; text-align: center;'>
                        © 2025 LinkCare. All rights reserved.
                    </p>
                </div>
                """.formatted(verificationCode);
    }
}