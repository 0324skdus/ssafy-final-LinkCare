package com.ssafy.linkcare.character.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CharacterStatusDto {
    private Long characterId;
    private String name;
    private String description;
    private String baseImageUrl; // 대표 이미지 URL
    private List<String> imageUrls; // 애니메이션 이미지 URL 목록
    private boolean isUnlocked;
    private boolean isMain;
}
