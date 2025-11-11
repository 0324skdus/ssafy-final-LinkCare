package com.ssafy.linkcare.point.controller;

import com.ssafy.linkcare.point.dto.PointBalanceResponseDto;
import com.ssafy.linkcare.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    public ResponseEntity<PointBalanceResponseDto> getPointBalance(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        int balance = pointService.getPointBalance(userPk);
        return ResponseEntity.ok(new PointBalanceResponseDto(balance));
    }

    @PostMapping("/add")
    public ResponseEntity<PointBalanceResponseDto> addPoints(
            Authentication authentication,
            @RequestParam("amount") int amount) {
        Long userPk = Long.parseLong(authentication.getName());
        int newBalance = pointService.addPoints(userPk, amount);
        return ResponseEntity.ok(new PointBalanceResponseDto(newBalance));
    }
}
