package com.ssafy.linkcare.shop.controller;

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

    @PostMapping("/buy/character/{characterId}")
    public ResponseEntity<Void> buyCharacter(
            Authentication authentication,
            @PathVariable Long characterId) {
        Long userPk = Long.parseLong(authentication.getName());
        shopService.buyCharacter(userPk, characterId);
        return ResponseEntity.ok().build();
    }
}
