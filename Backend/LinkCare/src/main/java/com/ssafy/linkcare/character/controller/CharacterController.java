package com.ssafy.linkcare.character.controller;

import com.ssafy.linkcare.character.dto.CharacterStatusDto;
import com.ssafy.linkcare.character.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public ResponseEntity<List<CharacterStatusDto>> getCharacterStatuses(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        List<CharacterStatusDto> characters = characterService.getCharacterStatuses(userPk);
        return ResponseEntity.ok(characters);
    }

    @GetMapping("/main")
    public ResponseEntity<CharacterStatusDto> getMainCharacter(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        CharacterStatusDto mainCharacter = characterService.getMainCharacter(userPk);
        if (mainCharacter == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(mainCharacter);
    }

    @PostMapping("/main/{characterId}")
    public ResponseEntity<Void> changeMainCharacter(Authentication authentication, @PathVariable Long characterId) {
        Long userPk = Long.parseLong(authentication.getName());
        characterService.changeMainCharacter(userPk, characterId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unlock/{characterId}")
    public ResponseEntity<Void> unlockCharacter(Authentication authentication, @PathVariable Long characterId) {
        Long userPk = Long.parseLong(authentication.getName());
        characterService.unlockCharacter(userPk, characterId);
        return ResponseEntity.ok().build();
    }
}
