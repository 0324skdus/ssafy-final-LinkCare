package com.ssafy.linkcare.user.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.user.dto.ProfileUpdateRequest;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateProfile(Long userPk, ProfileUpdateRequest request) {

        // 1. 사용자 조회
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 프로필 업데이트 (기존 updateProfile 메서드 사용!)
        user.updateProfile(
            user.getName(),
            request.height(),
            request.weight(),
            request.birth(),
                request.gender(),
            request.exerciseStartYear()
        );

        log.info("프로필 업데이트 완료: userId={}, birth={}", userPk, request.birth());
    }

    public User getUserById(Long userPk) {
        return userRepository.findById(userPk)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /*
        * FCM 토큰 업데이트
            * - 안드로이드 앱이 FCM 토큰을 받으면 서버에 저장
    */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateFcmToken(fcmToken);

        log.info("FCM 토큰 업데이트 완료: userId={}", userId);
    }
}