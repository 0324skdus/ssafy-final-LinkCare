package com.ssafy.linkcare.background.service;

import com.ssafy.linkcare.background.entity.Background;
import com.ssafy.linkcare.background.entity.UserBackground;
import com.ssafy.linkcare.background.repository.BackgroundRepository;
import com.ssafy.linkcare.background.repository.UserBackgroundRepository;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BackgroundService {

    private final UserRepository userRepository;
    private final BackgroundRepository backgroundRepository;
    private final UserBackgroundRepository userBackgroundRepository;
    private final PointService pointService;

    @Transactional
    public void assignDefaultBackground(User user) {
        // ID가 1인 배경을 기본 배경으로 조회
        Background defaultBackground = backgroundRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "기본 배경(ID: 1)을 찾을 수 없습니다."));

        // UserBackground 생성 및 저장
        UserBackground userBackground = UserBackground.builder()
                .user(user)
                .background(defaultBackground)
                .build();
        userBackgroundRepository.save(userBackground);

        // 사용자의 배경 목록에 추가 및 대표 배경으로 설정
        user.getBackgrounds().add(userBackground);
        user.setMainBackground(userBackground);
        userRepository.save(user);
    }

    @Transactional
    public void unlockBackground(Long userPk, Long backgroundId) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Background backgroundToUnlock = backgroundRepository.findById(backgroundId)
                .orElseThrow(() -> new CustomException(ErrorCode.BACKGROUND_NOT_FOUND, "해당 배경을 찾을 수 없습니다."));

        // 이미 보유하고 있는지 확인
        boolean alreadyOwned = user.getBackgrounds().stream()
                .anyMatch(ub -> ub.getBackground().getBackgroundId().equals(backgroundId));

        if (alreadyOwned) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 보유하고 있는 배경입니다.");
        }

        // 포인트 차감
        pointService.usePoints(userPk, backgroundToUnlock.getPrice());

        UserBackground userBackground = UserBackground.builder()
                .user(user)
                .background(backgroundToUnlock)
                .build();
        userBackgroundRepository.save(userBackground);

        user.getBackgrounds().add(userBackground);
        userRepository.save(user);
    }
}
