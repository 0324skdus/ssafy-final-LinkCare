package com.ssafy.linkcare.group.controller;

import com.ssafy.linkcare.group.scheduler.WeeklyGoalRewardScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 스케줄러 테스트용 컨트롤러
 * ⚠️ 주의: 프로덕션 환경에서는 제거하거나 관리자 권한 체크 필요!
 */
@Slf4j
@RestController
@RequestMapping("/api/test/schedulers")
@RequiredArgsConstructor
public class GroupSchedulerTestController {

    private final WeeklyGoalRewardScheduler weeklyGoalRewardScheduler;

    /**
     * 주간 목표 달성 체크 수동 실행
     * POST http://localhost:9090/api/test/schedulers/weekly-goal-reward
     */
    @PostMapping("/weekly-goal-reward")
    public ResponseEntity<?> triggerWeeklyGoalReward() {
        log.info("=== 수동 트리거: 주간 목표 달성 체크 시작 ===");

        try {
            weeklyGoalRewardScheduler.checkGoalAchievementAndReward();
            return ResponseEntity.ok(Map.of(
                    "message", "주간 목표 달성 체크 완료",
                    "success", true
            ));
        } catch (Exception e) {
            log.error("스케줄러 실행 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "스케줄러 실행 실패: " + e.getMessage(),
                    "success", false
            ));
        }
    }
}
