package com.ssafy.linkcare.user.service;

import com.ssafy.linkcare.character.entity.Character;
import com.ssafy.linkcare.character.entity.UserCharacter;
import com.ssafy.linkcare.character.repository.CharacterRepository;
import com.ssafy.linkcare.character.repository.UserCharacterRepository;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
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

        if (selectedCharacter.getPrice() != 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "무료 캐릭터만 선택할 수 있습니다.");
        }

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
            request.exerciseStartYear()
        );

        log.info("프로필 업데이트 완료: userId={}, birth={}", userPk, request.birth());
    }

    public User getUserById(Long userPk) {
        return userRepository.findById(userPk)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfile(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(
                user.getName(),
                user.getBirth(),
                user.getHeight(),
                user.getWeight(),
                user.getGender(),
                user.getExerciseStartYear()
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

        user.updateFcmToken(fcmToken);

        log.info("✅ FCM 토큰 업데이트 완료: userId={}, userName={}", userId, user.getName());
    }
}
