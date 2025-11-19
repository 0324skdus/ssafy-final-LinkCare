package com.ssafy.linkcare.background.service;

import com.ssafy.linkcare.background.dto.BackgroundStatusDto;
import com.ssafy.linkcare.background.entity.Background;
import com.ssafy.linkcare.background.entity.UserBackground;
import com.ssafy.linkcare.background.repository.BackgroundRepository;
import com.ssafy.linkcare.background.repository.UserBackgroundRepository;
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
public class BackgroundService {

    private final UserRepository userRepository;
    private final BackgroundRepository backgroundRepository;
    private final UserBackgroundRepository userBackgroundRepository;
    private final PointService pointService;
    private final S3Service s3Service;

    public List<BackgroundStatusDto> getBackgroundStatuses(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Background> allBackgrounds = backgroundRepository.findAll();
        Set<Long> unlockedBackgroundIds = user.getBackgrounds().stream()
                .map(ub -> ub.getBackground().getBackgroundId())
                .collect(Collectors.toSet());

        Long mainBackgroundId = user.getMainBackground() != null ? user.getMainBackground().getBackground().getBackgroundId() : null;

        return allBackgrounds.stream().map(background -> {
            boolean isUnlocked = unlockedBackgroundIds.contains(background.getBackgroundId());
            boolean isMain = background.getBackgroundId().equals(mainBackgroundId);
            String imageUrl = s3Service.generatePresignedGetUrl(background.getS3Key());

            return BackgroundStatusDto.builder()
                    .backgroundId(background.getBackgroundId())
                    .name(background.getName())
                    .description(background.getDescription())
                    .imageUrl(imageUrl)
                    .isUnlocked(isUnlocked)
                    .isMain(isMain)
                    .build();
        }).collect(Collectors.toList());
    }

    public BackgroundStatusDto getMainBackground(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserBackground mainUserBackground = user.getMainBackground();
        if (mainUserBackground == null) {
            return null;
        }

        Background mainBackground = mainUserBackground.getBackground();
        String imageUrl = s3Service.generatePresignedGetUrl(mainBackground.getS3Key());

        return BackgroundStatusDto.builder()
                .backgroundId(mainBackground.getBackgroundId())
                .name(mainBackground.getName())
                .description(mainBackground.getDescription())
                .imageUrl(imageUrl)
                .isUnlocked(true)
                .isMain(true)
                .build();
    }

    @Transactional
    public void changeMainBackground(Long userPk, Long backgroundId) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserBackground newMainBackground = user.getBackgrounds().stream()
                .filter(ub -> ub.getBackground().getBackgroundId().equals(backgroundId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.BACKGROUND_NOT_FOUND, "사용자가 보유하지 않은 배경입니다."));

        user.setMainBackground(newMainBackground);
    }

    @Transactional
    public void assignDefaultBackground(User user) {
        Background defaultBackground = backgroundRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "기본 배경(ID: 1)을 찾을 수 없습니다."));

        UserBackground userBackground = UserBackground.builder()
                .user(user)
                .background(defaultBackground)
                .build();
        userBackgroundRepository.save(userBackground);

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

        boolean alreadyOwned = user.getBackgrounds().stream()
                .anyMatch(ub -> ub.getBackground().getBackgroundId().equals(backgroundId));

        if (alreadyOwned) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE, "이미 보유하고 있는 배경입니다.");
        }

        pointService.usePoints(userPk, backgroundToUnlock.getPrice());

        UserBackground userBackground = UserBackground.builder()
                .user(user)
                .background(backgroundToUnlock)
                .build();
        userBackgroundRepository.save(userBackground);

        user.getBackgrounds().add(userBackground);
        userRepository.save(user);
    }

    public List<Background> getAllBackgrounds() {
        return backgroundRepository.findAll();
    }
}
