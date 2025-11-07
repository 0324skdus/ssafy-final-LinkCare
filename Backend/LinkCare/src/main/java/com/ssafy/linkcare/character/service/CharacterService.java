package com.ssafy.linkcare.character.service;

import com.ssafy.linkcare.character.dto.CharacterResponseDto;
import com.ssafy.linkcare.character.entity.Character;
import com.ssafy.linkcare.character.entity.UserCharacter;
import com.ssafy.linkcare.character.repository.CharacterRepository;
import com.ssafy.linkcare.character.repository.UserCharacterRepository;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;

    @Transactional
    public void assignDefaultCharacter(User user) {
        // 기본 캐릭터 조회 (이름이 '기본'이라고 가정)
        Character defaultCharacter = characterRepository.findByName("기본")
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "기본 캐릭터를 찾을 수 없습니다."));

        // UserCharacter 생성 및 저장
        UserCharacter userCharacter = UserCharacter.builder()
                .user(user)
                .character(defaultCharacter)
                .build();
        userCharacterRepository.save(userCharacter);

        // 사용자의 캐릭터 목록에 추가
        user.getCharacters().add(userCharacter);

        // 대표 캐릭터로 설정
        user.updateMainCharacter(userCharacter);

        // 사용자 정보 업데이트
        userRepository.save(user);
    }

    public List<CharacterResponseDto> getOwnedCharacters(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return userCharacterRepository.findByUser(user).stream()
                .map(userCharacter -> CharacterResponseDto.builder().character(userCharacter.getCharacter()).build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void changeMainCharacter(Long userPk, Long userCharacterId) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserCharacter newMainCharacter = userCharacterRepository.findByUserAndUserCharacterId(user, userCharacterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND, "사용자가 보유하지 않은 캐릭터입니다."));

        user.updateMainCharacter(newMainCharacter);
        userRepository.save(user);
    }

    @Transactional
    public void unlockCharacter(Long userPk, Long characterId) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Character characterToUnlock = characterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND, "해당 캐릭터를 찾을 수 없습니다."));

        // 이미 보유하고 있는지 확인
        boolean alreadyOwned = user.getCharacters().stream()
                .anyMatch(uc -> uc.getCharacter().getCharacterId().equals(characterId));

        if (alreadyOwned) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 보유하고 있는 캐릭터입니다.");
        }

        UserCharacter userCharacter = UserCharacter.builder()
                .user(user)
                .character(characterToUnlock)
                .build();
        userCharacterRepository.save(userCharacter);

        user.getCharacters().add(userCharacter);
        userRepository.save(user);
    }
}
