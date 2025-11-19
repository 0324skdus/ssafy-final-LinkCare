package com.ssafy.linkcare.group.scheduler;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupMember;
import com.ssafy.linkcare.group.entity.WeeklyGroupGoals;
import com.ssafy.linkcare.group.entity.WeeklyGroupStats;
import com.ssafy.linkcare.group.enums.MetricType;
import com.ssafy.linkcare.group.repository.GroupMemberRepository;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.group.repository.WeeklyGroupGoalsRepository;
import com.ssafy.linkcare.group.repository.WeeklyGroupStatsRepository;
import com.ssafy.linkcare.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyGoalRewardScheduler {

    private final GroupRepository groupRepository;
    private final WeeklyGroupGoalsRepository weeklyGroupGoalsRepository;
    private final WeeklyGroupStatsRepository weeklyGroupStatsRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PointService pointService;

    private static final int GOAL_ACHIEVEMENT_REWARD = 10; // 목표 달성 시 지급 포인트

    /**
     * 매주 월요일 자정에 실행
     * 지난 주 그룹 목표 달성 여부를 확인하고 포인트 지급
     * cron = "초 분 시 일 월 요일"
     * 0 5 0 * * MON = 매주 월요일 0시 5분 0초 (통계 생성 이후 실행)
     *
     * 🧪 테스트용: @Scheduled(cron = "0 * * * * *") // 1분마다 실행
     */
    @Scheduled(cron = "0 5 0 * * MON")
    @Transactional
    public void checkGoalAchievementAndReward() {
        log.info("========================================");
        log.info("=== 주간 목표 달성 체크 및 포인트 지급 시작 ===");
        log.info("========================================");

        // 지난 주 월요일 계산
        LocalDate lastWeekMonday = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .minusWeeks(1);

        log.info("체크 대상 주차: {}", lastWeekMonday);

        List<Group> groups = groupRepository.findAll();
        log.info("전체 그룹 수: {}개", groups.size());

        int totalChecked = 0;
        int goalAchieved = 0;
        int goalNotAchieved = 0;
        int noGoalSet = 0;
        int noStatsData = 0;
        int totalPointsRewarded = 0;

        for (Group group : groups) {
            try {
                totalChecked++;
                RewardResult result = checkAndRewardGroup(group, lastWeekMonday);

                switch (result.status) {
                    case ACHIEVED -> {
                        goalAchieved++;
                        totalPointsRewarded += result.pointsRewarded;
                    }
                    case NOT_ACHIEVED -> goalNotAchieved++;
                    case NO_GOAL_SET -> noGoalSet++;
                    case NO_STATS_DATA -> noStatsData++;
                }

            } catch (Exception e) {
                log.error("그룹 {} 목표 달성 체크 실패: {}", group.getGroupSeq(), e.getMessage(), e);
            }
        }

        log.info("========================================");
        log.info("=== 주간 목표 달성 체크 및 포인트 지급 완료 ===");
        log.info("체크 완료: {}개", totalChecked);
        log.info("  - 목표 달성: {}개 ({}포인트 지급)", goalAchieved, totalPointsRewarded);
        log.info("  - 목표 미달성: {}개", goalNotAchieved);
        log.info("  - 목표 미설정: {}개", noGoalSet);
        log.info("  - 통계 데이터 없음: {}개", noStatsData);
        log.info("========================================");
    }

    /**
     * 개별 그룹의 목표 달성 체크 및 포인트 지급
     */
    private RewardResult checkAndRewardGroup(Group group, LocalDate lastWeekMonday) {
        Long groupSeq = group.getGroupSeq();
        log.info("=== 그룹 {} 체크 시작 ===", groupSeq);

        // 1. 지난 주 목표 조회
        WeeklyGroupGoals goal = weeklyGroupGoalsRepository
                .findByGroup_GroupSeqAndWeekStart(groupSeq, lastWeekMonday)
                .orElse(null);

        if (goal == null) {
            log.info("그룹 {} - 지난 주 목표 없음 (스킵)", groupSeq);
            return new RewardResult(RewardStatus.NO_GOAL_SET, 0);
        }

        // 2. selectedMetricType 확인
        MetricType selectedMetric = goal.getSelectedMetricType();
        if (selectedMetric == null) {
            log.info("그룹 {} - 메트릭 타입 미선택 (스킵)", groupSeq);
            return new RewardResult(RewardStatus.NO_GOAL_SET, 0);
        }

        log.info("그룹 {} - 메트릭: {}, 목표: {}", groupSeq, selectedMetric, getGoalValue(goal, selectedMetric));

        // 3. 지난 주 실제 활동량 조회
        WeeklyGroupStats stats = weeklyGroupStatsRepository
                .findByGroup_GroupSeqAndWeekStart(groupSeq, lastWeekMonday)
                .orElse(null);

        if (stats == null) {
            log.info("그룹 {} - 지난 주 통계 데이터 없음 (스킵)", groupSeq);
            return new RewardResult(RewardStatus.NO_STATS_DATA, 0);
        }

        log.info("그룹 {} - 실제 활동량: {}", groupSeq, getActualValue(stats, selectedMetric));

        // 4. 목표 달성 여부 체크
        boolean achieved = isGoalAchieved(goal, stats, selectedMetric);

        if (!achieved) {
            log.info("그룹 {} - 목표 미달성 (타입: {}, 목표: {}, 실제: {})",
                    groupSeq, selectedMetric, getGoalValue(goal, selectedMetric), getActualValue(stats, selectedMetric));
            return new RewardResult(RewardStatus.NOT_ACHIEVED, 0);
        }

        // 5. 목표 달성! 그룹 멤버에게 포인트 지급
        log.info("그룹 {} - 🎉 목표 달성! (타입: {}, 목표: {}, 실제: {})",
                groupSeq, selectedMetric, getGoalValue(goal, selectedMetric), getActualValue(stats, selectedMetric));

        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        int memberCount = members.size();
        int totalReward = 0;

        log.info("그룹 {} - 멤버 수: {}명", groupSeq, memberCount);

        for (GroupMember member : members) {
            try {
                Long userPk = member.getUser().getUserPk();
                log.info("  - 사용자 {}에게 포인트 지급 시도...", userPk);
                pointService.addPoints(userPk, GOAL_ACHIEVEMENT_REWARD);
                totalReward += GOAL_ACHIEVEMENT_REWARD;
                log.info("  - 사용자 {} 에게 {}포인트 지급 완료 ✅", userPk, GOAL_ACHIEVEMENT_REWARD);
            } catch (Exception e) {
                log.error("사용자 {} 포인트 지급 실패: {}", member.getUser().getUserPk(), e.getMessage(), e);
            }
        }

        log.info("그룹 {} - 총 {}명에게 {}포인트 지급 완료", groupSeq, memberCount, totalReward);
        return new RewardResult(RewardStatus.ACHIEVED, totalReward);
    }

    /**
     * 목표 달성 여부 확인
     */
    private boolean isGoalAchieved(WeeklyGroupGoals goal, WeeklyGroupStats stats, MetricType metricType) {
        return switch (metricType) {
            case STEPS -> stats.getGroupStepsTotal() >= goal.getGoalSteps();
            case KCAL -> stats.getGroupKcalTotal() >= goal.getGoalKcal();
            case DURATION -> stats.getGroupDurationTotal() >= goal.getGoalDuration();
            case DISTANCE -> stats.getGroupDistanceTotal() >= goal.getGoalDistance();
        };
    }

    /**
     * 목표값 가져오기 (로깅용)
     */
    private Number getGoalValue(WeeklyGroupGoals goal, MetricType metricType) {
        return switch (metricType) {
            case STEPS -> goal.getGoalSteps();
            case KCAL -> goal.getGoalKcal();
            case DURATION -> goal.getGoalDuration();
            case DISTANCE -> goal.getGoalDistance();
        };
    }

    /**
     * 실제값 가져오기 (로깅용)
     */
    private Number getActualValue(WeeklyGroupStats stats, MetricType metricType) {
        return switch (metricType) {
            case STEPS -> stats.getGroupStepsTotal();
            case KCAL -> stats.getGroupKcalTotal();
            case DURATION -> stats.getGroupDurationTotal();
            case DISTANCE -> stats.getGroupDistanceTotal();
        };
    }

    /**
     * 보상 결과 내부 클래스
     */
    private record RewardResult(RewardStatus status, int pointsRewarded) {}

    /**
     * 보상 상태
     */
    private enum RewardStatus {
        ACHIEVED,       // 목표 달성
        NOT_ACHIEVED,   // 목표 미달성
        NO_GOAL_SET,    // 목표 미설정
        NO_STATS_DATA   // 통계 데이터 없음
    }
}
