package com.ssafy.linkcare.shop.controller;

import com.ssafy.linkcare.shop.dto.ShopBackgroundResponseDto;
import com.ssafy.linkcare.shop.dto.ShopCharacterResponseDto;
import com.ssafy.linkcare.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/characters")
    public ResponseEntity<ShopCharacterResponseDto> getShopCharacterList(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        ShopCharacterResponseDto response = shopService.getShopCharacterList(userPk);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/backgrounds")
    public ResponseEntity<ShopBackgroundResponseDto> getShopBackgroundList(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        ShopBackgroundResponseDto response = shopService.getShopBackgroundList(userPk);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/buy/character/{characterId}")
    public ResponseEntity<Void> buyCharacter(
            Authentication authentication,
            @PathVariable Long characterId) {
        Long userPk = Long.parseLong(authentication.getName());
        shopService.buyCharacter(userPk, characterId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/buy/background/{backgroundId}")
    public ResponseEntity<Void> buyBackground(
            Authentication authentication,
            @PathVariable Long backgroundId) {
        Long userPk = Long.parseLong(authentication.getName());
        shopService.buyBackground(userPk, backgroundId);
        return ResponseEntity.ok().build();
    }
}
