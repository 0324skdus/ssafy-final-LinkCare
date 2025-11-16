package com.ssafy.linkcare.security.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ssafy.linkcare.background.service.BackgroundService;
import com.ssafy.linkcare.character.service.CharacterService;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final CharacterService characterService;
    private final PointService pointService;
    private final BackgroundService backgroundService;

    @Value("${google.client-id}")
    private String googleClientId;

    /*
        * Google ID Token을 검증하고 사용자 정보를 반환
        * @param idToken Android 앱에서 받은 Google ID Token
        * @return User 엔티티 (신규 가입 or 기존 회원)
        * @throws CustomException ID Token이 유효하지 않거나 검증 실패 시
    */
    @Transactional
    public User verifyGoogleTokenAndGetUser(String idToken) {
        try {
            // Google ID Token 검증기 생성
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // ID Token 검증
            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                log.error("Google ID Token 검증 실패: 유효하지 않은 토큰");
                throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }

            // 사용자 정보 추출
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String providerId = payload.getSubject();        // Google 고유 ID
            String email = payload.getEmail();               // 이메일
            String name = (String) payload.get("name");      // 이름

            log.info("Google 로그인 시도 - email: {}, providerId: {}", email, providerId);

            // 기존 회원 조회 (provider + providerId로)
            return userRepository.findByProviderAndProviderId("GOOGLE", providerId)
                    .orElseGet(() -> {
                        // 신규 회원 자동 가입
                        log.info("신규 Google 회원 자동 가입 - email: {}", email);

                        User newUser = User.builder()
                                .email(email)
                                .password(null) // 소셜 로그인은 비밀번호 없음
                                .name(name)
                                .provider("GOOGLE")
                                .providerId(providerId)
                                .build();

                        userRepository.save(newUser);
                        pointService.createPointForNewUser(newUser);
                        backgroundService.assignDefaultBackground(newUser);
                        return newUser;
                    });

        } catch (CustomException e) {
            // CustomException은 그대로 전파
            throw e;
        } catch (Exception e) {
            // 기타 예외는 GOOGLE_TOKEN_VERIFICATION_FAILED로 래핑
            log.error("Google ID Token 검증 중 예외 발생", e);
            throw new CustomException(ErrorCode.GOOGLE_TOKEN_VERIFICATION_FAILED);
        }
    }
}
