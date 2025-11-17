package com.ssafy.linkcare.health.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.DailyHealthData;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthSyncService {

    private final ActivitySummaryService activitySummaryService;
    private final BloodPressureService bloodPressureService;
    private final ExerciseService exerciseService;
    private final HeartRateService heartRateService;
    private final StepService stepService;
    private final WaterIntakeService waterIntakeService;
    private final SleepService sleepService;
    private final UserHealthFeedbackService userHealthFeedbackService;


    private final FirebaseMessaging firebaseMessaging;
    private final UserRepository userRepository;

    // 데일리 데이터 동기화 (매일 1회)
    public void syncDailyHealthData(DailyHealthData dto, int userSeq) {
        LocalDate date = LocalDate.now();

        activitySummaryService.saveDailyActivitySummaryData(dto.getActivitySummary(), userSeq);
        bloodPressureService.saveDailyBloodPressureData(dto.getBloodPressure(), userSeq);
        exerciseService.saveDailyExerciseData(dto.getExercise(), userSeq);
        heartRateService.saveDailyHeartRateData(dto.getHeartRate(), userSeq);
        stepService.saveDailyStepData(dto.getStep(), userSeq);
        waterIntakeService.saveDailyWaterIntakeData(dto.getWaterIntake(), userSeq);
        sleepService.saveDailySleepData(dto.getSleep(), userSeq);

        // 마지막 동기화 시간 업데이트
        updateUserLastSyncTime(userSeq);

        userHealthFeedbackService.generateFeedbackAsync(userSeq, date);

    }

    /**
     * 워치 운동 저장 후 즉시 동기화 요청 (병합 포함)
     */
    public void requestExerciseSyncWithWatchMerge(int userSeq) {
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
                    .putData("type", "EXERCISE_SYNC_WITH_MERGE")  // 타입 구분!
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("✓ 사용자 {}에게 운동 동기화 요청 전송 (워치 병합)", userSeq);

        } catch (Exception e) {
            log.error("✗ 사용자 {} 운동 동기화 FCM 전송 실패 (워치 병합)", userSeq, e);
        }
    }

    /**
     * 정기 동기화 요청 (워치 병합 없음)
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
                    .putData("type", "EXERCISE_SYNC")  // 기존 타입
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

    private void updateUserLastSyncTime(int userSeq) {
        User user = validateUser(userSeq);

        user.updateLastSyncTime();
        userRepository.save(user);

        log.debug("사용자 {} 마지막 동기화 시간 업데이트: {}", userSeq, user.getLastSyncTime());
    }

    // validate
    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
