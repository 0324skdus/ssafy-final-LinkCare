package com.ssafy.linkcare.background.controller;

import com.ssafy.linkcare.background.dto.BackgroundStatusDto;
import com.ssafy.linkcare.background.service.BackgroundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backgrounds")
@RequiredArgsConstructor
public class BackgroundController {

    private final BackgroundService backgroundService;

    @Operation(summary = "보유 배경 목록 조회", description = "사용자가 보유한 모든 배경 목록과 잠금 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배경 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public ResponseEntity<List<BackgroundStatusDto>> getBackgroundStatuses(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        List<BackgroundStatusDto> backgrounds = backgroundService.getBackgroundStatuses(userPk);
        return ResponseEntity.ok(backgrounds);
    }

    @Operation(summary = "메인 배경 조회", description = "사용자의 현재 메인 배경을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메인 배경 조회 성공"),
            @ApiResponse(responseCode = "204", description = "메인 배경이 설정되지 않음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/main")
    public ResponseEntity<BackgroundStatusDto> getMainBackground(Authentication authentication) {
        Long userPk = Long.parseLong(authentication.getName());
        BackgroundStatusDto mainBackground = backgroundService.getMainBackground(userPk);
        if (mainBackground == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(mainBackground);
    }

    @Operation(summary = "메인 배경 변경", description = "사용자의 메인 배경을 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메인 배경 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 배경을 찾을 수 없음")
    })
    @PostMapping("/main/{backgroundId}")
    public ResponseEntity<Void> changeMainBackground(Authentication authentication, @PathVariable Long backgroundId) {
        Long userPk = Long.parseLong(authentication.getName());
        backgroundService.changeMainBackground(userPk, backgroundId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "배경 구매(잠금 해제)", description = "포인트를 사용하여 배경을 구매(잠금 해제)합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배경 구매 성공"),
            @ApiResponse(responseCode = "400", description = "포인트 부족 또는 이미 보유한 배경"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 배경을 찾을 수 없음")
    })
    @PostMapping("/unlock/{backgroundId}")
    public ResponseEntity<Void> unlockBackground(Authentication authentication, @PathVariable Long backgroundId) {
        Long userPk = Long.parseLong(authentication.getName());
        backgroundService.unlockBackground(userPk, backgroundId);
        return ResponseEntity.ok().build();
    }
}
