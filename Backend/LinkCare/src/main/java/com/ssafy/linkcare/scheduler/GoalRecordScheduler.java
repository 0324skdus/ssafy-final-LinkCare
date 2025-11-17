package com.ssafy.linkcare.scheduler;

import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.group.service.GoalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * 주간 목표 달성 기록 자동 생성 스케줄러
 * - 매주 일요일 23:59:00에 실행
 * - 모든 그룹의 이번 주 목표 달성 기록을 비동기 큐에 추가
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GoalRecordScheduler {

    private final GroupRepository groupRepository;
    private final GoalRecordService goalRecordService;

    /**
     * 주간 목표 달성 기록 생성 스케줄
     * - 실행 시점: 매주 일요일 23:59:00
     * - 처리 대상: 일요일이 속한 주의 목표 (월요일~일요일)
     * - 처리 방식: 비동기 큐에 작업 추가 (즉시 반환)
     */
    @Scheduled(cron = "0 59 23 * * SUN", zone = "Asia/Seoul")
    public void scheduleWeeklyGoalRecords() {
        LocalDate today = LocalDate.now();  // 일요일
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);  // 이번 주 월요일

        log.info("===== 주간 목표 달성 기록 생성 시작 =====");
        log.info("실행 일자: {} (일요일)", today);
        log.info("대상 주차: {} ~ {}", weekStart, today);

        try {
            // 모든 그룹 ID만 조회 (가벼운 쿼리)
            List<Long> groupSeqs = groupRepository.findAllGroupSeqs();

            log.info("총 {}개 그룹을 처리 큐에 추가", groupSeqs.size());

            // 비동기 큐에 작업 추가 (즉시 반환)
            for (Long groupSeq : groupSeqs) {
                goalRecordService.processGroupAsync(groupSeq, weekStart);
            }

            log.info("모든 그룹이 처리 큐에 추가 완료. 백그라운드에서 순차 처리됩니다.");
            log.info("===== 주간 목표 달성 기록 스케줄링 완료 =====");

        } catch (Exception e) {
            log.error("주간 목표 달성 기록 스케줄링 실패", e);
        }
    }
}
