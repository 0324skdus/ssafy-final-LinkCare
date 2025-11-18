package com.ssafy.linkcare.shop.service;

import com.ssafy.linkcare.background.entity.Background;
import com.ssafy.linkcare.background.service.BackgroundService;
import com.ssafy.linkcare.character.entity.Character;
import com.ssafy.linkcare.character.repository.CharacterRepository;
import com.ssafy.linkcare.character.service.CharacterService;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.service.PointService;
import com.ssafy.linkcare.s3.S3Service;
import com.ssafy.linkcare.shop.dto.ShopBackgroundDto;
import com.ssafy.linkcare.shop.dto.ShopBackgroundResponseDto;
import com.ssafy.linkcare.shop.dto.ShopCharacterDto;
import com.ssafy.linkcare.shop.dto.ShopCharacterResponseDto;
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

    public ShopCharacterResponseDto getShopCharacterList(Long userPk) {
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
                    String animatedImageUrl = s3Service.generatePresignedGetUrl(character.getAnimatedImageS3Key());

                    return ShopCharacterDto.builder()
                            .characterId(character.getCharacterId())
                            .name(character.getName())
                            .description(character.getDescription())
                            .baseImageUrl(baseImageUrl)
                            .animatedImageUrl(animatedImageUrl)
                            .price(character.getPrice())
                            .isUnlocked(unlockedCharacterIds.contains(character.getCharacterId()))
                            .build();
                })
                .collect(Collectors.toList());

        // 6. 최종 응답 DTO 생성 및 반환
        return new ShopCharacterResponseDto(userPoints, shopCharacters);
    }

    public ShopBackgroundResponseDto getShopBackgroundList(Long userPk) {
        // 1. 사용자 조회
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 사용자 포인트 조회
        int userPoints = pointService.getPointBalance(userPk);
        
        // 3. 모든 배경 목록 조회
        List<Background> allBackgrounds = backgroundService.getAllBackgrounds();

        // 4. 사용자가 보유한 배경 ID 목록 조회
        Set<Long> unlockedBackgroundIds = user.getBackgrounds().stream()
                .map(ub -> ub.getBackground().getBackgroundId())
                .collect(Collectors.toSet());

        // 5. ShopBackgroundDto 목록 생성
        List<ShopBackgroundDto> shopBackgrounds = allBackgrounds.stream()
                .map(background -> {
                    String imageUrl = s3Service.generatePresignedGetUrl(background.getS3Key());
                    return ShopBackgroundDto.builder()
                            .backgroundId(background.getBackgroundId())
                            .name(background.getName())
                            .description(background.getDescription())
                            .imageUrl(imageUrl)
                            .price(background.getPrice())
                            .isUnlocked(unlockedBackgroundIds.contains(background.getBackgroundId()))
                            .build();
                })
                .collect(Collectors.toList());

        // 6. 최종 응답 DTO 생성 및 반환
        return new ShopBackgroundResponseDto(userPoints, shopBackgrounds);
    }
}
