package com.ssafy.linkcare.email.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMAIL_VERIFICATION_PREFIX = "EV:";  // Email Verification

    @Value("${email.verification.code.expiration}")
    private long codeExpiration;  // 5분

    // 인증 코드 생성 (6자리 숫자)
    public String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(1000000);  // 0 ~ 999999
        return String.format("%06d", code);  // 6자리로 포맷 (앞에 0 채움)
    }

    // 인증 코드 저장 (Redis)
    public void saveVerificationCode(String email, String code) {
        String key = EMAIL_VERIFICATION_PREFIX + email;

        // Redis에 저장 (5분 유효)
        redisTemplate.opsForValue().set(key, code, codeExpiration, TimeUnit.MILLISECONDS);
    }

    /**
     * 인증 코드 검증
     * @param email 이메일
     * @param code 사용자가 입력한 코드
     * @return 일치 여부
     */
    public boolean verifyCode(String email, String code) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        // 인증 코드 없음 또는 만료됨
        if (storedCode == null) {
            log.warn("인증 코드 없음 또는 만료됨: email={}", email);
            throw new CustomException(ErrorCode.VERIFICATION_CODE_NOT_FOUND);
        }

        // 인증 일치 여부 확인
        if (!storedCode.equals(code)) {
            throw new CustomException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 인증 성공 시 코드 삭제 (재사용 방지)
        redisTemplate.delete(key);
        log.info("인증 성공 및 코드 삭제: email={}", email);

        return true;
    }

    /**
     * 인증 코드 삭제
     * @param email 이메일
     */
    public void deleteVerificationCode(String email) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        redisTemplate.delete(key);
        log.info("인증 코드 삭제: email={}", email);
    }
}