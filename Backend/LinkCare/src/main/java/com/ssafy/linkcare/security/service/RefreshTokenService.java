package com.ssafy.linkcare.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "RT:";  // Redis Key 접두사

    /**
        * Refresh Token 저장
        * @param userId 사용자 ID
        * @param refreshToken Refresh Token
        * @param expirationMs 만료 시간 (밀리초)
     */
    public void saveRefreshToken(String userId, String refreshToken, long expirationMs) {
        String key = REFRESH_TOKEN_PREFIX + userId;

        // Redis에 저장 (TTL 설정)
        redisTemplate.opsForValue().set(key, refreshToken, expirationMs, TimeUnit.MILLISECONDS);

        log.info("Refresh Token 저장 완료: userId={}, TTL={}ms", userId, expirationMs);
    }

    /**
        * Refresh Token 조회
        * @param userId 사용자 ID
        * @return Refresh Token (없으면 null)
     */
    public String getRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        String token = redisTemplate.opsForValue().get(key);

        log.info("Refresh Token 조회: userId={}, exists={}", userId, token != null);
        return token;
    }

    /**
        * Refresh Token 검증
        * @param userId 사용자 ID
        * @param refreshToken 검증할 Refresh Token
        * @return 일치 여부
     */
    public boolean validateRefreshToken(String userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);

        if (storedToken == null) {
            log.warn("Refresh Token 없음: userId={}", userId);
            return false;
        }

        boolean isValid = storedToken.equals(refreshToken);
        log.info("Refresh Token 검증: userId={}, valid={}", userId, isValid);

        return isValid;
    }

    /**
        * Refresh Token 삭제 (로그아웃)
        * @param userId 사용자 ID
     */
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;

        Boolean deleted = redisTemplate.delete(key);
        log.info("Refresh Token 삭제: userId={}, deleted={}", userId, deleted);
    }
}
