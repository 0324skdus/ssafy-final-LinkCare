package com.ssafy.linkcare.character.controller;

import com.ssafy.linkcare.character.dto.CharacterResponseDto;
import com.ssafy.linkcare.character.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    // 소유 캐릭터 리스트 반환
    @GetMapping
    public ResponseEntity<List<CharacterResponseDto>> getOwnedCharacters(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        List<CharacterResponseDto> characters = characterService.getOwnedCharacters(userPk);
        return ResponseEntity.ok(characters);
    }

    // 메인 캐릭터 변경
    @PostMapping("/main/{userCharacterId}")
    public ResponseEntity<Void> changeMainCharacter(Authentication authentication, @PathVariable Long userCharacterId) {
        Long userPk = Long.parseLong(authentication.getName());
        characterService.changeMainCharacter(userPk, userCharacterId);
        return ResponseEntity.ok().build();
    }

    // 캐릭터 잠금 해제 (
    @PostMapping("/unlock/{characterId}")
    public ResponseEntity<Void> unlockCharacter(Authentication authentication, @PathVariable Long characterId) {
        Long userPk = Long.parseLong(authentication.getName());
        characterService.unlockCharacter(userPk, characterId);
        return ResponseEntity.ok().build();
    }
}
