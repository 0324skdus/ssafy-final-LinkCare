package com.ssafy.linkcare.user.service;

import com.ssafy.linkcare.background.entity.UserBackground;
import com.ssafy.linkcare.character.entity.Character;
import com.ssafy.linkcare.character.entity.UserCharacter;
import com.ssafy.linkcare.character.repository.CharacterRepository;
import com.ssafy.linkcare.character.repository.UserCharacterRepository;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.s3.S3Service;
import com.ssafy.linkcare.user.dto.MyPageResponseDto;
import com.ssafy.linkcare.user.dto.ProfileUpdateRequest;
import com.ssafy.linkcare.user.dto.UserProfileResponse;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final PointService pointService;
    private final S3Service s3Service;

    public MyPageResponseDto getMyPageInfo(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 포인트 조회
        int points = pointService.getPointBalance(userPk);

        // 메인 캐릭터 이미지 URL 조회
        String mainCharacterImageUrl = null;
        UserCharacter mainCharacter = user.getMainCharacter();
        if (mainCharacter != null && mainCharacter.getCharacter() != null) {
            mainCharacterImageUrl = s3Service.generatePresignedGetUrl(mainCharacter.getCharacter().getBaseImageS3Key());
        }

        // 메인 배경 이미지 URL 조회
        String mainBackgroundImageUrl = null;
        UserBackground mainBackground = user.getMainBackground();
        if (mainBackground != null && mainBackground.getBackground() != null) {
            mainBackgroundImageUrl = s3Service.generatePresignedGetUrl(mainBackground.getBackground().getS3Key());
        }

        return MyPageResponseDto.builder()
                .name(user.getName())
                .points(points)
                .mainCharacterImageUrl(mainCharacterImageUrl)
                .mainBackgroundImageUrl(mainBackgroundImageUrl)
                .build();
    }

    @Transactional
    public void completeInitialSetup(Long userPk, Long characterId, String petName) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 펫 이름 유효성 검사
        if (!StringUtils.hasText(petName)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "펫 이름은 비워둘 수 없습니다.");
        }

        // 2. 이미 초기 설정이 완료되었는지 확인 (캐릭터, 펫이름 둘 다)
        if (user.getMainCharacter() != null || StringUtils.hasText(user.getPetName())) {
            throw new CustomException(ErrorCode.ALREADY_COMPLETED, "이미 초기 설정이 완료되었습니다.");
        }

        // 3. 캐릭터 선택 로직
        Character selectedCharacter = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        UserCharacter userCharacter = UserCharacter.builder()
                .user(user)
                .character(selectedCharacter)
                .build();
        userCharacterRepository.save(userCharacter);

        user.getCharacters().add(userCharacter);
        user.setMainCharacter(userCharacter);
        user.setPetName(petName);

        userRepository.save(user);
        log.info("초기 설정 완료: userId={}, characterId={}, petName={}", userPk, characterId, petName);
    }


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
            request.exerciseStartYear(),
            request.petName()
        );

        log.info("프로필 업데이트 완료: userId={}, birth={}", userPk, request.birth());
    }

    public UserProfileResponse getUserProfile(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(
                user.getName(),
                user.getBirth(),
                user.getHeight(),
                user.getWeight(),
                user.getGender(),
                user.getExerciseStartYear(),
                user.getPetName()
        );
    }

    /*
        * FCM 토큰 업데이트
            * - 안드로이드 앱이 FCM 토큰을 받으면 서버에 저장
    */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("FCM 토큰 업데이트 요청: userId={}, userName={}, 토큰길이={}",
                userId, user.getName(), fcmToken != null ? fcmToken.length() : 0);

        if (fcmToken != null && !fcmToken.isBlank()) {
            // 1️⃣ 같은 토큰을 쓰고 있던 "다른 유저들"의 토큰을 전부 null 처리
            userRepository.clearFcmTokenFromOtherUsers(fcmToken, userId);
        }

        // 2️⃣ 내 토큰 저장
        user.updateFcmToken(fcmToken);

        log.info("✅ FCM 토큰 업데이트 완료: userId={}, userName={}", userId, user.getName());
    }
}
