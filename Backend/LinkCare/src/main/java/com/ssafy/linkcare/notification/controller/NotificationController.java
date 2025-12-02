package com.ssafy.linkcare.notification.controller;

import com.ssafy.linkcare.notification.dto.NotificationResponse;
import com.ssafy.linkcare.notification.dto.UnreadCountResponse;
import com.ssafy.linkcare.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림 관련 API")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회",
            description = "알림함에서 내 알림 목록을 조회합니다. category 파라미터로 ALL/GROUP 필터링 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "ALL") String category) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("내 알림 목록 조회 요청: userId={}, category={}", userId, category);

        List<NotificationResponse> response = notificationService.getMyNotifications(userId, category);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "안 읽은 알림 개수 조회",
            description = "전체 안 읽은 알림 개수와 그룹 카테고리 안 읽은 알림 개수를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "안 읽은 알림 개수 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("안 읽은 알림 개수 조회 요청: userId={}", userId);

        UnreadCountResponse response = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 읽음 처리",
            description = "특정 알림을 읽음 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "본인의 알림이 아님"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId) {

        Long userId = Long.parseLong(authentication.getName());
        notificationService.markAsRead(userId, notificationId);

        log.info("알림 읽음 처리 API 호출: userId={}, notificationId={}", userId, notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "전체 알림 읽음 처리",
            description = "내 모든 알림을 읽음 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전체 알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        notificationService.markAllAsRead(userId);

        log.info("전체 알림 읽음 처리 API 호출: userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 삭제",
            description = "특정 알림을 삭제합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "본인의 알림이 아님"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            Authentication authentication,
            @PathVariable Long notificationId) {

        Long userId = Long.parseLong(authentication.getName());
        notificationService.deleteNotification(userId, notificationId);

        log.info("알림 삭제 API 호출: userId={}, notificationId={}", userId, notificationId);
        return ResponseEntity.ok().build();
    }
}