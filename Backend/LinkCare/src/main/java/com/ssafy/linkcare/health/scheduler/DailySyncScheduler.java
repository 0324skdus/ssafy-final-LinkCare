package com.ssafy.linkcare.health.scheduler;

import com.ssafy.linkcare.health.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailySyncScheduler {

    private final HealthService healthService;

    /**
     * 매일 자정에 모든 사용자에게 동기화 요청
     * cron = "초 분 시 일 월 요일"
     * 0 0 0 * * * = 매일 00시 00분 00초
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailySync() {
        log.info("┌─────────────────────────────────────┐");
        log.info("│   일일 건강 데이터 동기화 배치 시작     │");
        log.info("└─────────────────────────────────────┘");

        try {
            healthService.syncAllUsersHealthData();
            log.info("일일 동기화 배치 완료");
        } catch (Exception e) {
            log.error("일일 동기화 배치 실패", e);
        }

        log.info("┌─────────────────────────────────────┐");
        log.info("│   일일 건강 데이터 동기화 배치 종료     │");
        log.info("└─────────────────────────────────────┘\n");
    }
}