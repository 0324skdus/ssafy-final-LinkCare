package com.ssafy.linkcare.character.service;

import com.ssafy.linkcare.character.dto.CharacterStatusDto;
import com.ssafy.linkcare.character.entity.Character;
import com.ssafy.linkcare.character.entity.UserCharacter;
import com.ssafy.linkcare.character.repository.CharacterRepository;
import com.ssafy.linkcare.character.repository.UserCharacterRepository;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.s3.S3Service;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final S3Service s3Service;
    private final PointService pointService;

    @Transactional
    public List<CharacterStatusDto> getCharacterStatuses(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Character> allCharacters = characterRepository.findAll();
        Set<Long> unlockedCharacterIds = user.getCharacters().stream()
                .map(uc -> uc.getCharacter().getCharacterId())
                .collect(Collectors.toSet());

        Long mainCharacterId = user.getMainCharacter() != null ? user.getMainCharacter().getCharacter().getCharacterId() : null;

        return allCharacters.stream().map(character -> {
            boolean isUnlocked = unlockedCharacterIds.contains(character.getCharacterId());
            boolean isMain = character.getCharacterId().equals(mainCharacterId);
            String baseImageUrl = s3Service.generatePresignedGetUrl(character.getBaseImageS3Key());
            String animatedImageUrl = s3Service.generatePresignedGetUrl(character.getAnimatedImageS3Key());

            return CharacterStatusDto.builder()
                    .characterId(character.getCharacterId())
                    .name(character.getName())
                    .description(character.getDescription())
                    .baseImageUrl(baseImageUrl)
                    .animatedImageUrl(animatedImageUrl)
                    .isUnlocked(isUnlocked)
                    .isMain(isMain)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public CharacterStatusDto getMainCharacter(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserCharacter mainUserCharacter = user.getMainCharacter();
        if (mainUserCharacter == null) {
            return null; // 대표 캐릭터가 없는 경우
        }

        Character mainCharacter = mainUserCharacter.getCharacter();
        String baseImageUrl = s3Service.generatePresignedGetUrl(mainCharacter.getBaseImageS3Key());
        String animatedImageUrl = s3Service.generatePresignedGetUrl(mainCharacter.getAnimatedImageS3Key());

        return CharacterStatusDto.builder()
                .characterId(mainCharacter.getCharacterId())
                .name(mainCharacter.getName())
                .description(mainCharacter.getDescription())
                .baseImageUrl(baseImageUrl)
                .animatedImageUrl(animatedImageUrl)
                .isUnlocked(true)
                .isMain(true)
                .build();
    }

    @Transactional
    public void changeMainCharacter(Long userPk, Long characterId) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserCharacter newMainCharacter = user.getCharacters().stream()
                .filter(uc -> uc.getCharacter().getCharacterId().equals(characterId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND, "사용자가 보유하지 않은 캐릭터입니다."));

        user.setMainCharacter(newMainCharacter);
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

        // 포인트 차감
        pointService.usePoints(userPk, characterToUnlock.getPrice());

        UserCharacter userCharacter = UserCharacter.builder()
                .user(user)
                .character(characterToUnlock)
                .build();
        userCharacterRepository.save(userCharacter);

        user.getCharacters().add(userCharacter);
        userRepository.save(user);
    }
}
