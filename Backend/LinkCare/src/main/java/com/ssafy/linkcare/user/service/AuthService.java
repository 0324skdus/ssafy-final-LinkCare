package com.ssafy.linkcare.user.service;

import com.ssafy.linkcare.background.service.BackgroundService;
import com.ssafy.linkcare.character.service.CharacterService;
import com.ssafy.linkcare.email.service.EmailService;
import com.ssafy.linkcare.email.service.EmailVerificationService;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.security.jwt.JwtUtil;
import com.ssafy.linkcare.security.service.KakaoService;
import com.ssafy.linkcare.security.service.OAuth2Service;
import com.ssafy.linkcare.security.service.RefreshTokenService;
import com.ssafy.linkcare.user.dto.KakaoUserInfo;
import com.ssafy.linkcare.user.dto.LoginRequest;
import com.ssafy.linkcare.user.dto.LoginResponse;
import com.ssafy.linkcare.user.dto.SignupRequest;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final KakaoService kakaoService;
    private final OAuth2Service oauth2Service;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final CharacterService characterService;
    private final PointService pointService;
    private final BackgroundService backgroundService;


    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // 이메일 중복 확인
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }


    // 이메일 인증 코드 발송
    @Transactional
    public void sendVerificationCode(String email) {

        // 1. 이메일 중복 확인
        if (checkEmailDuplicate(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 인증 코드 생성 (6자리)
        String code = emailVerificationService.generateVerificationCode();

        // 3. Redis에 저장 (5분 유효)
        emailVerificationService.saveVerificationCode(email, code);

        // 4. 이메일 발송
        emailService.sendVerificationEmail(email, code);

        log.info("인증 코드 발송 완료: email={}", email);
    }

    // 인증 코드 검증
    public void verifyCode(String email, String code) {
        emailVerificationService.verifyCode(email, code);
        log.info("인증 코드 검증 성공: email={}", email);
    }

    // 회원가입
    @Transactional
    public void signup(SignupRequest request) {
        // 1. 이메일 중복 체크
        if (checkEmailDuplicate(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. User 엔티티 생성
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .provider("LOCAL")
                .providerId(null)
                .build();

        // 4. DB 저장
        userRepository.save(user);

        // 5. 포인트 계정 생성
        pointService.createPointForNewUser(user);

        // 6. 기본 배경 할당
        backgroundService.assignDefaultBackground(user);

        log.info("회원가입 완료: {}", request.email());
    }

    // 로그인
    public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 소셜 로그인 사용자는 일반 로그인 불가
        if (user.getPassword() == null) {
            String provider = user.getProvider();
            if ("GOOGLE".equals(provider)) {
                throw new CustomException(ErrorCode.INVALID_CREDENTIALS, "Google 로그인을 사용해주세요");
            } else if ("KAKAO".equals(provider)) {
                throw new CustomException(ErrorCode.INVALID_CREDENTIALS, "Kakao 로그인을 사용해주세요");
            }
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS, "소셜 로그인으로 가입된 계정입니다");
        }

        // 3. 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 토큰 생성 및 응답
        return generateTokenResponse(user);
    }

    // Token 재발급
    public LoginResponse refreshAccessToken(String refreshToken) {

        // 1. Refresh Token 검증 (JWT 자체 검증)
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Token에서 userId 추출
        String userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 3. Redis에 저장된 Refresh Token과 비교
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Refresh Token입니다");
        }

        // 4. 사용자 정보 조회
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("Token 재발급 완료: userId={}", userId);

        // 5. 토큰 생성 및 응답
        return generateTokenResponse(user);
    }

    // 로그아웃
    public void logout(String userId) {
        // Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userId);
        log.info("로그아웃 완료: userId={}", userId);
    }

    /**
     * 토큰 생성 및 LoginResponse 반환 (공통 함수)
     * @param user 사용자 정보
     * @return LoginResponse
     */
    private LoginResponse generateTokenResponse(User user) {
        String userId = user.getUserPk().toString();

        // 1. JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        // 2. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken, refreshTokenExpiration);

        // 3. 프로필/캐릭터/펫이름 선택 필요 여부 확인
        boolean needsProfileCompletion = (user.getBirth() == null
                || user.getMainCharacter() == null
                || !StringUtils.hasText(user.getPetName()));

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getUserPk(),
                user.getEmail(),
                user.getName(),
                needsProfileCompletion
        );
    }


    /*
     * Google 소셜 로그인
     * @param idToken Android 앱에서 받은 Google ID Token
     * @return LoginResponse (JWT 토큰 포함)
     */
    @Transactional
    public LoginResponse googleLogin(String idToken) {

        // 1. OAuth2Service로 Google ID Token 검증 및 사용자 정보 가져오기
        User user = oauth2Service.verifyGoogleTokenAndGetUser(idToken);

        log.info("Google 로그인 성공: email={}", user.getEmail());

        // 2. 토큰 생성 및 응답 (기존 공통 메서드 재사용!)
        return generateTokenResponse(user);
    }

    /*
     * 카카오 소셜 로그인
     * @param code Android 앱에서 받은 인가 코드
     * @return LoginResponse (JWT 토큰 포함)
     */
    @Transactional
    public LoginResponse kakaoLogin(String accessToken) {
        // 1. Access Token으로 사용자 정보 직접 조회 (토큰 발급 단계 생략!)
        KakaoUserInfo kakaoUserInfo = kakaoService.getUserInfo(accessToken);

        // 2. User 엔티티 조회 또는 생성
        User user = kakaoService.getOrCreateUser(kakaoUserInfo);

        log.info("카카오 로그인 성공: email={}", user.getEmail());

        // 3. JWT 토큰 생성 및 응답
        return generateTokenResponse(user);
    }

}