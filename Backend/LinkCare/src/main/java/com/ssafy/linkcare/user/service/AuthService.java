package com.ssafy.linkcare.user.service;

import com.ssafy.linkcare.security.jwt.JwtUtil;
import com.ssafy.linkcare.user.dto.LoginRequest;
import com.ssafy.linkcare.user.dto.LoginResponse;
import com.ssafy.linkcare.user.dto.SignupRequest;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 이메일 중복 확인
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    // 회원가입
    @Transactional
    public void signup(SignupRequest request) {

        // 1. 이메일 중복 체크
        if (checkEmailDuplicate(request.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. User 엔티티 생성
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .birth(request.birth())
                .build();

        // 4. DB 저장
        userRepository.save(user);

        log.info("회원가입 완료: {}", request.email());
    }

    // 로그인
    public LoginResponse login(LoginRequest request) {

        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다"));

        // 2. 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다");
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUserPk().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserPk().toString());

        log.info("로그인 성공: {}", request.email());

        // 4. 응답 반환
        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getUserPk(),
                user.getEmail(),
                user.getName()
        );
    }
}
