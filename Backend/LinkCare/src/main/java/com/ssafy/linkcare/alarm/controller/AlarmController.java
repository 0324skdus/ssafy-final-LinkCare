package com.ssafy.linkcare.alarm.controller;

import com.ssafy.linkcare.alarm.dto.AlarmResponseDto;
import com.ssafy.linkcare.alarm.dto.AlarmSaveRequestDto;
import com.ssafy.linkcare.alarm.service.AlarmService;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
@Tag(name = "알림", description = "그룹 알림 관련 API")
public class AlarmController {

    private final AlarmService alarmService;
    private final UserRepository userRepository;

    @Operation(summary = "알림 저장", description = "새로운 그룹 알림을 생성하고 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "알림 생성 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 그룹을 찾을 수 없음")
    })
    @PostMapping("/save")
    public ResponseEntity<Void> saveAlarm(
            Authentication authentication,
            @RequestBody AlarmSaveRequestDto requestDto) {

        Long senderId = Long.parseLong(authentication.getName());
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("알림 저장 API 호출: senderUserPk={}, receiverUserPk={}, groupSeq={}",
                sender.getUserPk(), requestDto.getReceiverUserPk(), requestDto.getGroupSeq());

        alarmService.saveAlarm(requestDto, sender);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "삭제되지 않는 모든 알림 목록 조회", description = "현재 로그인한 사용자의 삭제되지 않는 모든 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/all")
    public ResponseEntity<List<AlarmResponseDto>> getAllAlarms(
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        log.info("삭제되지 않는 모든 알림 목록 조회 API 호출: userPk={}", user.getUserPk());

        List<AlarmResponseDto> response = alarmService.getAllAlarms(user);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "403", description = "해당 알림을 읽을 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PatchMapping("/{alarmId}/read")
    public ResponseEntity<String> readAlarm(
            @PathVariable Long alarmId,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("알림 읽음 처리 API 호출: alarmId={}, userId={}", alarmId, userId);

        alarmService.readAlarm(alarmId, userId);

        return ResponseEntity.ok("알림을 성공적으로 읽음 처리했습니다.");
    }


    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "해당 알림을 삭제할 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping("/{alarmId}")
    public ResponseEntity<Void> deleteAlarm(
            @PathVariable Long alarmId,
            Authentication authentication) {

        Long userId = Long.parseLong(authentication.getName());
        alarmService.deleteAlarm(userId, alarmId);

        log.info("콕/편지쓰기 삭제 API 호출: userId={}, alarmId={}", userId, alarmId);
        return ResponseEntity.ok().build();
    }
}
