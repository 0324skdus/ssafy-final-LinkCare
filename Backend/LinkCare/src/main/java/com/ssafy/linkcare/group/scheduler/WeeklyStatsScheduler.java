package com.ssafy.linkcare.group.scheduler;

import com.ssafy.linkcare.group.service.WeeklyStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyStatsScheduler {

    private final WeeklyStatsService weeklyStatsService;

    /**
     * 매주 월요일 자정에 실행
     * cron = "초 분 시 일 월 요일"
     * 0 0 0 * * MON = 매주 월요일 0시 0분 0초
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void generateWeeklyGroupStats() {
        log.info("========================================");
        log.info("주간 그룹 통계 생성 배치 시작");
        log.info("========================================");

        try {
            weeklyStatsService.generateAllGroupWeeklyStats();
            log.info("주간 그룹 통계 생성 완료!");
        } catch (Exception e) {
            log.error("주간 그룹 통계 생성 실패", e);
        }

        log.info("========================================");
    }
}
