package com.ssafy.linkcare.shop.service;

import com.ssafy.linkcare.background.service.BackgroundService;
import com.ssafy.linkcare.character.entity.Character;
import com.ssafy.linkcare.character.repository.CharacterRepository;
import com.ssafy.linkcare.character.service.CharacterService;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.s3.S3Service;
import com.ssafy.linkcare.shop.dto.ShopCharacterDto;
import com.ssafy.linkcare.shop.dto.ShopResponseDto;
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
public class ShopService {

    private final CharacterService characterService;
    private final BackgroundService backgroundService;
    private final PointService pointService;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final S3Service s3Service;

    @Transactional
    public void buyCharacter(Long userPk, Long characterId) {
        characterService.unlockCharacter(userPk, characterId);
    }

    @Transactional
    public void buyBackground(Long userPk, Long backgroundId) {
        backgroundService.unlockBackground(userPk, backgroundId);
    }

    public ShopResponseDto getShopCharacterList(Long userPk) {
        // 1. 사용자 조회
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 사용자 포인트 조회
        int userPoints = pointService.getPointBalance(userPk);

        // 3. 모든 캐릭터 목록 조회
        List<Character> allCharacters = characterRepository.findAll();

        // 4. 사용자가 보유한 캐릭터 ID 목록 조회
        Set<Long> unlockedCharacterIds = user.getCharacters().stream()
                .map(uc -> uc.getCharacter().getCharacterId())
                .collect(Collectors.toSet());

        // 5. ShopCharacterDto 목록 생성
        List<ShopCharacterDto> shopCharacters = allCharacters.stream()
                .map(character -> {
                    String baseImageUrl = s3Service.generatePresignedGetUrl(character.getBaseImageS3Key());
                    List<String> imageUrls = character.getImageS3Keys().stream()
                            .map(s3Service::generatePresignedGetUrl)
                            .collect(Collectors.toList());

                    return ShopCharacterDto.builder()
                            .characterId(character.getCharacterId())
                            .name(character.getName())
                            .description(character.getDescription())
                            .baseImageUrl(baseImageUrl)
                            .imageUrls(imageUrls)
                            .price(character.getPrice())
                            .isUnlocked(unlockedCharacterIds.contains(character.getCharacterId()))
                            .build();
                })
                .collect(Collectors.toList());

        // 6. 최종 응답 DTO 생성 및 반환
        return new ShopResponseDto(userPoints, shopCharacters);
    }
}
