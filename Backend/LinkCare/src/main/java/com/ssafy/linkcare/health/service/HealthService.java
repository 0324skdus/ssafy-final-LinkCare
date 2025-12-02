package com.ssafy.linkcare.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupMember;
import com.ssafy.linkcare.group.repository.GroupMemberRepository;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.entity.BloodPressure;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private final ActivitySummaryService activitySummaryService;
    private final BloodPressureService bloodPressureService;
    private final ExerciseService exerciseService;
    private final HeartRateService heartRateService;
    private final StepService stepService;
    private final WaterIntakeService waterIntakeService;
    private final SleepService sleepService;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final FirebaseMessaging firebaseMessaging;

    private final ObjectMapper objectMapper;
    private final TimeStampUtil timeStampUtil;

    // 전체 데이터 동기화 (초기 1회)
    public void syncAllHealthData(AllHealthDataDto dto, int userSeq) {

        activitySummaryService.saveAllActivitySummaryData(dto.getActivitySummary(), userSeq);
        bloodPressureService.saveAllBloodPressureData(dto.getBloodPressure(), userSeq);
        exerciseService.saveAllExerciseData(dto.getExercise(), userSeq);
        sleepService.saveAllSleepData(dto.getSleep(), userSeq);
        heartRateService.saveAllHeartRateData(dto.getHeartRate(), userSeq);
        stepService.saveAllStepData(dto.getStep(), userSeq);
        waterIntakeService.saveAllWaterIntakeData(dto.getWaterIntake(), userSeq);
    }

    // 데일리 데이터 동기화 (매일 1회)
//    public void syncDailyHealthData(DailyHealthData dto, int userSeq) {
//        activitySummaryService.saveDailyActivitySummaryData(dto.getActivitySummary(), userSeq);
//        bloodPressureService.saveDailyBloodPressureData(dto.getBloodPressure(), userSeq);
//        exerciseService.saveDailyExerciseData(dto.getExercise(), userSeq);
//        heartRateService.saveDailyHeartRateData(dto.getHeartRate(), userSeq);
//        stepService.saveDailyStepData(dto.getStep(), userSeq);
//        waterIntakeService.saveDailyWaterIntakeData(dto.getWaterIntake(), userSeq);
//        sleepService.saveDailySleepData(dto.getSleep(), userSeq);
//
//        // 마지막 동기화 시간 업데이트
//        updateUserLastSyncTime(userSeq);
//
//        userHealthFeedbackService.generateFeedbackAsync(userSeq);
//
//    }

    // 당일 건강 데이터 조회
    public DailyHealthResponse getTodayHealthData(int userSeq) {
        return DailyHealthResponse.builder().activitySummary(activitySummaryService.getTodayActivitySummary(userSeq))
                .bloodPressure(bloodPressureService.getTodayBloodPressures(userSeq))
                .exercise(exerciseService.getTodayExercises(userSeq))
                .heartRate(heartRateService.getTodayHeartRate(userSeq))
                .step(stepService.getTodayStepCount(userSeq))
                .exercise(exerciseService.getTodayExercises(userSeq))
                .waterIntake(waterIntakeService.getTodayWaterIntakes(userSeq))
                .sleep(sleepService.getTodaySleeps(userSeq))
                .build();
    }


    // 건강 통계 데이터 조회
    public HealthStaticsResponse getHealthStaticsData(int userSeq, LocalDate startDate, LocalDate endDate) {
        return HealthStaticsResponse.builder()
                .activitySummaryStats(activitySummaryService.getActivitySummaryStatsByDate(userSeq, startDate, endDate))
                .bloodPressureStats(bloodPressureService.getBloodPressureStatsByPeriod(userSeq,startDate,endDate))
                .exerciseStats(exerciseService.getExerciseStatisticsByDate(userSeq, startDate, endDate))
                .heartRateStats(heartRateService.getHeartRateStatsByDate(userSeq, startDate, endDate))
                .stepStats(stepService.getWeekStepStatisticsByDate(userSeq, startDate, endDate))
                .exerciseStats(exerciseService.getExerciseStatisticsByDate(userSeq, startDate, endDate))
                .waterIntakeStats(waterIntakeService.getWaterIntakeStatisticsByDate(userSeq, startDate, endDate))
                .sleepStats(sleepService.getSleepStatisticsByDate(userSeq,startDate,endDate))
                .build();
    }

    public TodayHealthSummary getTodayHealthSummary(int userSeq) {
        LocalDate today = LocalDate.now();

        return TodayHealthSummary.builder()
                .activitySummary(activitySummaryService.getActivitySummaryStatsToday(userSeq))
                .exercise(exerciseService.getTodayExerciseStatistics(userSeq))
                .step(stepService.getTodayStepCount(userSeq))
                .heartRate(heartRateService.getHeartRateStatsByDate(userSeq, today, today))
                .bloodPressure(bloodPressureService.getBloodPressureStatsByPeriod(userSeq, today, today))
                .waterIntake(waterIntakeService.getTodayWaterIntakeStatistics(userSeq))
                .sleep(sleepService.getSleepStatisticsByDate(userSeq, today, today))
                .build();
    }

    /**
     * 기간별 총 소모 칼로리, 총 걸음수, 총 운동 시간 조회
     * @param userSeq
     * @param startDate
     * @param endDate
     * @return TotalActivityStatisticsResponse
     */
    public TotalActivityStatisticsResponse getTotalActivityStatistics(int userSeq, LocalDate startDate, LocalDate endDate) {
        Long startDateTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endDate);

        ActivitySummaryStaticsResponse activitySummaryStaticsResponse = activitySummaryService.getActivitySummaryStatsByDate(userSeq, startDate, endDate);
        ExerciseStatisticsResponse exerciseStatisticsResponse = exerciseService.getExerciseStatisticsByDate(userSeq, startDate, endDate);
        Long totalSteps = stepService.getStepStatisticsByDate(userSeq, startDate, endDate).getTotalSteps();

        return TotalActivityStatisticsResponse.builder()
                .totalCalories(activitySummaryStaticsResponse.getTotalCalories())
                .totalDuration(exerciseStatisticsResponse.getTotalDuration())
                .totalSteps(totalSteps)
                .build();
    }

    public HealthActualActivityResponse getHealthActualActivity(int userSeq, LocalDate startDate, LocalDate endDate) {
        Long startDateTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endDate);

        ActivitySummaryStaticsResponse activitySummaryStaticsResponse = activitySummaryService.getActivitySummaryStatsByDate(userSeq, startDate, endDate);
        ExerciseStatisticsResponse exerciseStatisticsResponse = exerciseService.getExerciseStatisticsByDate(userSeq, startDate, endDate);
        Long totalSteps = stepService.getStepStatisticsByDate(userSeq, startDate, endDate).getTotalSteps();

        return HealthActualActivityResponse.builder()
                .totalCalories(activitySummaryStaticsResponse.getTotalCalories())
                .totalDuration(exerciseStatisticsResponse.getTotalDuration())
                .totalDistances(exerciseStatisticsResponse.getTotalDistance())
                .totalSteps(totalSteps)
                .build();
    }

    public DailyHealthDetailResponse getDailyHealthDetail(int userSeq) {
        LocalDate today = LocalDate.now();

        List<BloodPressureResponse> bloodPressureResponses = bloodPressureService.getBloodPressureByDate(userSeq, today, today);
        List<SleepResponse> sleepResponse = sleepService.getSleepsByDate(userSeq, today, today);
        List<WaterIntakeResponse> waterIntakeResponses = waterIntakeService.getWaterIntakeByDate(userSeq, today, today);
        List<HeartRateResponse> heartRateResponses = heartRateService.getHeartRatesByPeriod(userSeq, today, today);
        List<ExerciseSessionResponse> exerciseSessionResponses = exerciseService.getExerciseSessionsByDate(userSeq, today, today);


        DailyActivitySummaryResponse dailyActivitySummaryResponse = DailyActivitySummaryResponse.builder()
                .exercises(exerciseSessionResponses)
                .steps(stepService.getTodayStepCount(userSeq))
                .build();

        return DailyHealthDetailResponse.builder()
                .bloodPressures(bloodPressureResponses)
                .sleeps(sleepResponse)
                .waterIntakes(waterIntakeResponses)
                .heartRates(heartRateResponses)
                .dailyActivitySummary(dailyActivitySummaryResponse)
                .build();
    }

    public DailyHealthDetailResponse getDailyHealthDetailByDate(int userSeq, LocalDate date) {

        List<BloodPressureResponse> bloodPressureResponses = bloodPressureService.getBloodPressureByDate(userSeq, date, date);
        List<SleepResponse> sleepResponse = sleepService.getSleepsByDate(userSeq, date, date);
        List<WaterIntakeResponse> waterIntakeResponses = waterIntakeService.getWaterIntakeByDate(userSeq, date, date);
        List<HeartRateResponse> heartRateResponses = heartRateService.getHeartRatesByPeriod(userSeq, date, date);
        List<ExerciseSessionResponse> exerciseSessionResponses = exerciseService.getExerciseSessionsByDate(userSeq, date, date);


        DailyActivitySummaryResponse dailyActivitySummaryResponse = DailyActivitySummaryResponse.builder()
                .exercises(exerciseSessionResponses)
                .steps(stepService.getStepsByDate(userSeq, date))
                .build();

        return DailyHealthDetailResponse.builder()
                .bloodPressures(bloodPressureResponses)
                .sleeps(sleepResponse)
                .waterIntakes(waterIntakeResponses)
                .heartRates(heartRateResponses)
                .dailyActivitySummary(dailyActivitySummaryResponse)
                .build();
    }

    public DailyActivitySummaryResponse getDailyActivitySummary(int userSeq) {
        LocalDate today = LocalDate.now();
        List<ExerciseSessionResponse> exerciseSessionResponses = exerciseService.getExerciseSessionsByDate(userSeq, today, today);

        return DailyActivitySummaryResponse.builder()
                .exercises(exerciseSessionResponses)
                .steps(stepService.getTodayStepCount(userSeq))
                .build();
    }

    public DailyActivitySummaryResponse getDailyActivitySummaryByDate(int userSeq, LocalDate date) {
        List<ExerciseSessionResponse> exerciseSessionResponses = exerciseService.getExerciseSessionsByDate(userSeq, date, date);

        return DailyActivitySummaryResponse.builder()
                .exercises(exerciseSessionResponses)
                .steps(stepService.getStepsByDate(userSeq, date))
                .build();
    }


    public TodayStatisticsResponse getTodayStatisticsForDialogs(int userSeq) {
        LocalDate today = LocalDate.now();
        String lastBloodPressure = "데이터 없음";
        TodayHealthSummary todayHealthSummary = getTodayHealthSummary(userSeq);
        BloodPressureResponse bloodPressureResponse = bloodPressureService.getLastBloodPressureByDate(userSeq, today,today);

        if(bloodPressureResponse != null) {
            lastBloodPressure = (int) bloodPressureResponse.getSystolic() + "/" + (int) bloodPressureResponse.getDiastolic();
        }

        return TodayStatisticsResponse.builder()
                .totalSteps(todayHealthSummary.getStep())
                .totalCalories(todayHealthSummary.getActivitySummary().getTotalCalories())
                .totalDuration(todayHealthSummary.getExercise().getTotalDuration())
                .totalDistances(todayHealthSummary.getActivitySummary().getTotalDistance())
                .avgHeartRates(todayHealthSummary.getHeartRate().getAvgHeartRate())
                .sleepDuration(todayHealthSummary.getSleep().getTotalDuration())
                .totalWaterIntakes(todayHealthSummary.getWaterIntake().getTotalAmount())
                .lastBloodPressure(lastBloodPressure)
                .build();
    }



    // ================== 건강 데이터 동기화 ==================

    /**
     * 단일 사용자에게 FCM으로 동기화 요청
     */
    private void sendSyncRequestToUser(int userSeq) {
        try {
            User user = validateUser(userSeq);

            String fcmToken = user.getFcmToken();
            if (fcmToken == null || fcmToken.isEmpty()) {
                log.warn("사용자 {}의 FCM 토큰이 없습니다.", userSeq);
                return;
            }

            // FCM 메시지 생성 및 전송
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putData("type", "DAILY_SYNC")
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .build();

            String response = firebaseMessaging.send(message);
            log.debug("✓ 사용자 {}에게 동기화 요청 전송", userSeq);

        } catch (Exception e) {
            log.error("✗ 사용자 {} FCM 전송 실패", userSeq, e);
        }
    }

    /**
     * 특정 그룹의 모든 멤버 데이터 동기화 (그룹 페이지 진입 시)
     */
    public void syncGroupMembersHealthData(Long groupSeq) {
        log.info("========================================");
        log.info("그룹 {}의 멤버 데이터 동기화 시작", groupSeq);
        log.info("========================================");

        // 1. 그룹 검증
        Group group = validateGroup(groupSeq);

        log.info("그룹명: {}", group.getGroupName());

        // 2. 그룹 멤버 조회
        List<GroupMember> members = groupMemberRepository.findByGroupWithUser(group);
        log.info("그룹 멤버 수: {}명", members.size());

        if (members.isEmpty()) {
            log.info("그룹에 멤버가 없습니다.");
            return;
        }

        // 3. 각 멤버에게 FCM 전송
        int successCount = 0;
        int skipCount = 0;
        for (GroupMember member : members) {
            User user = member.getUser();
            LocalDateTime lastSync = user.getLastSyncTime();

            // 3분 이내 동기화 데이터는 스킵
            if (lastSync != null &&
                    Duration.between(lastSync, LocalDateTime.now()).toMinutes() < 3) {
                log.debug("사용자 {} 최근 동기화됨, 스킵", user.getUserPk());
                skipCount++;
                continue;
            }

            sendSyncRequestToUser(Math.toIntExact(user.getUserPk()));
            successCount++;
        }

        log.info("동기화 요청 완료: {}/{}명", successCount, members.size());
        log.info("========================================");
    }

    /**
     * 모든 활성 사용자에게 동기화 요청 (배치용)
     */
    public void syncAllUsersHealthData() {
        log.info("========================================");
        log.info("전체 사용자 데일리 동기화 요청 시작");
        log.info("========================================");

        List<User> users = userRepository.findAll();
        log.info("전체 사용자 수: {}명", users.size());

        int successCount = 0;
        int noTokenCount = 0;

        for (User user : users) {
            try {
                if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
                    noTokenCount++;
                    continue;
                }

                sendSyncRequestToUser(Math.toIntExact(user.getUserPk()));
                successCount++;

            } catch (Exception e) {
                log.error("사용자 {} 동기화 요청 실패", user.getUserPk(), e);
            }
        }

        log.info("동기화 요청 완료:");
        log.info("  - 성공: {}명", successCount);
        log.info("  - FCM 토큰 없음: {}명", noTokenCount);
        log.info("  - 실패: {}명", users.size() - successCount - noTokenCount);
        log.info("========================================");
    }


    /**
     * 특정 사용자에게 운동 데이터만 동기화 요청
     */
    public void requestExerciseSyncForUser(int userSeq) {
        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

        String fcmToken = user.getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("사용자 {}의 FCM 토큰이 없습니다.", userSeq);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putData("type", "EXERCISE_SYNC")  // 운동만!
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("✓ 사용자 {}에게 운동 동기화 요청 전송", userSeq);

        } catch (Exception e) {
            log.error("✗ 사용자 {} 운동 동기화 FCM 전송 실패", userSeq, e);
        }
    }

    // validate
    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    // DailyActivity 데이터 통합 메서드
    private String createDailyActivityDataForGPT(
            ActivitySummaryStaticsResponse activitySummary,
            ExerciseStatisticsResponse exercise,
            int todayStep) {

        try {
            Map<String, Object> dailyActivity = new HashMap<>();
            dailyActivity.put("activitySummary", activitySummary);
            dailyActivity.put("exercise", exercise);
            dailyActivity.put("steps", todayStep);

            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dailyActivity);
        } catch (JsonProcessingException e) {
            log.error("DailyActivity 데이터 변환 실패", e);
            return "{}";
        }
    }

    private Group validateGroup(Long groupSeq) {
        return groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    private void updateUserLastSyncTime(int userSeq) {
        User user = validateUser(userSeq);

        user.updateLastSyncTime();
        userRepository.save(user);

        log.debug("사용자 {} 마지막 동기화 시간 업데이트: {}", userSeq, user.getLastSyncTime());
    }
}
