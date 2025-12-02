package com.ssafy.linkcare.character.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CharacterStatusDto {
    private Long characterId;
    private String name;
    private String description;
    private String baseImageUrl;
    private String animatedImageUrl;
    private boolean isUnlocked;
    private boolean isMain;
}
