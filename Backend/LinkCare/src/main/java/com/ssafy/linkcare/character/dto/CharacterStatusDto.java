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
    private List<String> imageUrls; // Pre-signed URLs
    private boolean isUnlocked;
    private boolean isMain;
}
