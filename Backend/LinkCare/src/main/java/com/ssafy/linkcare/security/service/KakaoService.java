package com.ssafy.linkcare.security.service;

import com.ssafy.linkcare.background.service.BackgroundService;
import com.ssafy.linkcare.character.service.CharacterService;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.user.dto.KakaoUserInfo;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    private final UserRepository userRepository;
    private final WebClient webClient;
    private final CharacterService characterService;
    private final PointService pointService;
    private final BackgroundService backgroundService;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    // Access Token으로 카카오 사용자 정보 조회
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            KakaoUserInfo userInfo = webClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .block();

            if (userInfo == null) {
                throw new CustomException(ErrorCode.KAKAO_USER_INFO_REQUEST_FAILED);
            }

            log.info("카카오 사용자 정보 조회 성공: id={}", userInfo.getId());
            return userInfo;

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new CustomException(ErrorCode.KAKAO_USER_INFO_REQUEST_FAILED);
        }
    }

    // 카카오 사용자 정보로 User 조회 또는 생성
    @Transactional
    public User getOrCreateUser(KakaoUserInfo kakaoUserInfo) {
        String providerId = kakaoUserInfo.getId().toString();
        String email = kakaoUserInfo.getKakaoAccount().getEmail();
        String nickname = kakaoUserInfo.getKakaoAccount().getProfile().getNickname();

        log.info("카카오 로그인 시도 - email: {}, providerId: {}", email, providerId);

        // 기존 회원 조회 또는 생성
        return userRepository.findByProviderAndProviderId("KAKAO", providerId)
                .orElseGet(() -> {
                    log.info("신규 카카오 회원 자동 가입 - email: {}", email);

                    User newUser = User.builder()
                            .email(email)
                            .password(null)  // 소셜 로그인은 비밀번호 없음
                            .name(nickname)
                            .provider("KAKAO")
                            .providerId(providerId)
                            .build();

                    userRepository.save(newUser);
                    pointService.createPointForNewUser(newUser);
                    backgroundService.assignDefaultBackground(newUser);
                    return newUser;
                });
    }
}
