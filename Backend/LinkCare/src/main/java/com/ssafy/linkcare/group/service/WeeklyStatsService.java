package com.ssafy.linkcare.group.service;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupMember;
import com.ssafy.linkcare.group.entity.WeeklyGroupStats;
import com.ssafy.linkcare.group.enums.GroupType;
import com.ssafy.linkcare.group.repository.GroupMemberRepository;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.group.repository.WeeklyGroupStatsRepository;
import com.ssafy.linkcare.health.dto.ActivitySummaryStaticsResponse;
import com.ssafy.linkcare.health.dto.ExerciseStatisticsResponse;
import com.ssafy.linkcare.health.dto.StepStatisticsResponse;
import com.ssafy.linkcare.health.service.ActivitySummaryService;
import com.ssafy.linkcare.health.service.ExerciseService;
import com.ssafy.linkcare.health.service.StepService;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyStatsService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final WeeklyGroupStatsRepository weeklyGroupStatsRepository;
    private final ActivitySummaryService activitySummaryService;
    private final StepService stepService;
    private final ExerciseService exerciseService;

    private final TimeStampUtil timeStampUtil;

    /**
     * 모든 HEALTH 그룹의 주간 통계 생성
     */
    public void generateAllGroupWeeklyStats() {

        // 헬스 그룹 찾기
        List<Group> healthGroups = groupRepository.findByType(GroupType.HEALTH);
        log.info("HEALTH 그룹 {}개 발견", healthGroups.size());

        // 지난 주 날짜 계산
        LocalDate lastWeekMonday = timeStampUtil.getLastWeekMonday();
        LocalDate lastWeekSunday = timeStampUtil.getLastWeekSunday();

        log.info("집계 기간: {} ~ {}", lastWeekMonday, lastWeekSunday);

        // 각 그룹별 통계 생성
        int successCount = 0;
        for (Group group : healthGroups) {
            try {
                generateGroupWeeklyStats(group, lastWeekMonday, lastWeekSunday);
                successCount++;
                log.info("그룹 '{}' 통계 생성 완료", group.getGroupName());
            } catch (Exception e) {
                log.error("그룹 '{}' 통계 생성 실패", group.getGroupName(), e);
            }
        }
        log.info("총 {}/{}개 그룹 통계 생성 완료", successCount, healthGroups.size());
    }

    private void generateGroupWeeklyStats(Group group, LocalDate lastWeekMonday, LocalDate lastWeekSunday) {
        // 그룹 멤버 조회
        List<GroupMember> members = groupMemberRepository.findByGroup(group);

        if (members.isEmpty()) {
            log.warn("그룹 '{}'에 멤버가 없습니다.", group.getGroupName());
            return;
        }

        // 통계 데이터 계산
        int memberCount = members.size();
        float avgAge = calculateAvgAge(members);
        float avgBmi = calculateAvgBmi(members);

        // 각 멤버의 건강 데이터 수집
        List<MemberWeeklyData> memberDataList = members.stream()
                .map(member -> {
                    int userSeq = Math.toIntExact(member.getUser().getUserPk());

                    ActivitySummaryStaticsResponse activityResponse = activitySummaryService
                            .getActivitySummaryStatsByDate(userSeq, lastWeekMonday, lastWeekSunday);

                    StepStatisticsResponse stepResponse = stepService
                            .getStepStatisticsByDate(userSeq, lastWeekMonday, lastWeekSunday);

                    ExerciseStatisticsResponse exerciseResponse = exerciseService
                            .getExerciseStatisticsByDate(userSeq, lastWeekMonday, lastWeekSunday);

                    return new MemberWeeklyData(activityResponse, stepResponse, exerciseResponse);
                })
                .toList();

        // 그룹 활동 데이터 집계
        long groupStepsTotal = memberDataList.stream()
                .mapToLong(data -> data.stepResponse.getTotalSteps() != null ? data.stepResponse.getTotalSteps() : 0L)
                .sum();

        float groupKcalTotal = (float) memberDataList.stream()
                .mapToDouble(data -> data.activityResponse.getTotalCalories() != null ? data.activityResponse.getTotalCalories() : 0.0)
                .sum();

        int groupDurationTotal = (int) memberDataList.stream()
                .mapToLong(data -> data.exerciseResponse.getTotalDuration() != null ? data.exerciseResponse.getTotalDuration() : 0L)
                .sum();

        float groupDistanceTotal = (float) memberDataList.stream()
                .mapToDouble(data -> data.activityResponse.getTotalDistance() != null ? data.activityResponse.getTotalDistance() : 0.0)
                .sum();

        // 걸음수 분산 계산
        List<Long> steps = memberDataList.stream()
                .map(data -> data.stepResponse.getTotalSteps() != null ? data.stepResponse.getTotalSteps() : 0L)
                .toList();

        float groupStepsVar = stepService.calculateStdDev(steps);

        // WeeklyGroupStats 생성 및 저장
        WeeklyGroupStats stats = WeeklyGroupStats.builder()
                .group(group)
                .weekStart(lastWeekMonday)
                .weekEnd(lastWeekSunday)
                .memberCount(memberCount)
                .avgAge(avgAge)
                .avgBmi(avgBmi)
                .groupStepsTotal(groupStepsTotal)
                .groupKcalTotal(groupKcalTotal)
                .groupDurationTotal(groupDurationTotal)
                .groupDistanceTotal(groupDistanceTotal)
                .memberStepsVar(groupStepsVar)
                .createdAt(System.currentTimeMillis())
                .build();

        weeklyGroupStatsRepository.save(stats);
    }

    // 내부 헬퍼 클래스 (private static)
    private static class MemberWeeklyData {
        final ActivitySummaryStaticsResponse activityResponse;
        final StepStatisticsResponse stepResponse;
        final ExerciseStatisticsResponse exerciseResponse;

        MemberWeeklyData(ActivitySummaryStaticsResponse activityResponse,
                         StepStatisticsResponse stepResponse,
                         ExerciseStatisticsResponse exerciseResponse) {
            this.activityResponse = activityResponse;
            this.stepResponse = stepResponse;
            this.exerciseResponse = exerciseResponse;
        }
    }

    private float calculateAvgBmi(List<GroupMember> members) {
        if (members == null || members.isEmpty()) return 0f;

        double totalBmi = members.stream()
                .map(member -> member.getUser())
                .filter(user -> user.getHeight() != null && user.getWeight() != null && user.getHeight() > 0)
                .mapToDouble(user -> {
                    float heightInMeters = user.getHeight() / 100f;
                    return user.getWeight() / (heightInMeters * heightInMeters);
                })
                .sum();

        long validCount = members.stream()
                .map(member -> member.getUser())
                .filter(user -> user.getHeight() != null && user.getWeight() != null && user.getHeight() > 0)
                .count();

        return validCount > 0 ? (float) (totalBmi / validCount) : 0f;
    }

    private float calculateAvgAge(List<GroupMember> members) {
        if (members == null || members.isEmpty()) return 0f;

        LocalDate today = LocalDate.now();

        double totalAge = members.stream()
                .map(member -> member.getUser().getBirth())
                .filter(birth -> birth != null)
                .mapToLong(birth -> ChronoUnit.YEARS.between(birth, today) + 1)
                .sum();

        long validCount = members.stream()
                .map(member -> member.getUser().getBirth())
                .filter(birth -> birth != null)
                .count();

        return validCount > 0 ? (float) (totalAge / validCount) : 0f;
    }
}
