package com.ssafy.linkcare.character.dto;

import com.ssafy.linkcare.character.entity.Character;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class CharacterResponseDto {
    private Long characterId;
    private String name;
    private String description;
    private List<String> imageS3Keys;

    @Builder
    public CharacterResponseDto(Character character) {
        this.characterId = character.getCharacterId();
        this.name = character.getName();
        this.description = character.getDescription();
        this.imageS3Keys = character.getImageS3Keys();
    }
}
