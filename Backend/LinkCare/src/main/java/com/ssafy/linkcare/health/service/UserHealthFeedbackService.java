
package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.gpt.dto.HealthSummaryRequest;
import com.ssafy.linkcare.gpt.dto.HealthSummaryResponse;
import com.ssafy.linkcare.gpt.service.GptService;
import com.ssafy.linkcare.health.dto.DailyHealthDetailResponse;
import com.ssafy.linkcare.health.dto.DailyHealthResponse;
import com.ssafy.linkcare.health.entity.UserHealthFeedback;
import com.ssafy.linkcare.health.repository.UserHealthFeedbackRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserHealthFeedbackService {

    private final UserHealthFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final HealthService healthService;
    private final GptService gptService;

    /**
     * 건강 피드백 조회 - 기존 데이터 즉시 반환
     */
//    public HealthSummaryResponse getHealthFeedback(int userSeq, LocalDate date) {
//        User user = validateUser(userSeq);
//
//        UserHealthFeedback feedback = feedbackRepository.findByUser(user)
//                .orElse(null);
//
//        if (feedback == null) {
//            // 최초 조회 시 - 분석 중 상태 반환하고 비동기로 생성 시작
//            generateFeedbackAsync(userSeq, date);
//            return HealthSummaryResponse.analyzing();
//        }
//
//        return HealthSummaryResponse.builder()
//                .status(HealthSummaryResponse.HealthStatus.valueOf(feedback.getHealthStatus()))
//                .summary(feedback.getContent())
//                .updatedAt(feedback.getUpdatedAt())
//                .build();
//    }

    /**
     * 특정 날짜의 건강 피드백 조회
     *
     * @param userSeq 사용자 ID
     * @param date 조회할 날짜 (null이면 오늘)
     */
    public HealthSummaryResponse getHealthFeedbackByDate(int userSeq, LocalDate date) {
        User user = validateUser(userSeq);

        // 날짜가 null이면 오늘 날짜 사용
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // 해당 날짜의 피드백 조회
        UserHealthFeedback feedback = findFeedbackByDate(user, targetDate);

        if (feedback == null) {
            // 오늘 날짜인 경우에만 비동기 생성
            if (targetDate.equals(LocalDate.now())) {
                log.info("사용자 {}의 {} 피드백이 없습니다. 비동기 생성을 시작합니다.",
                        userSeq, targetDate);
                generateFeedbackAsync(userSeq, targetDate);
                return HealthSummaryResponse.analyzing();
            } else {
                // 과거 날짜는 없으면 null 반환
                log.info("사용자 {}의 {} 피드백이 없습니다.", userSeq, targetDate);
                return null;
            }
        }

        // 피드백 반환
        return HealthSummaryResponse.builder()
                .status(HealthSummaryResponse.HealthStatus.valueOf(feedback.getHealthStatus()))
                .summary(feedback.getContent())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    /**
     * 특정 날짜의 피드백 찾기
     */
    private UserHealthFeedback findFeedbackByDate(User user, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return feedbackRepository.findByUserAndDate(user, startOfDay, endOfDay)
                .orElse(null);
    }

    /**
     * 건강 데이터 동기화 시 피드백 갱신
     * - 기존 피드백을 먼저 반환
     * - 백그라운드에서 새 피드백 생성
     */
    public HealthSummaryResponse refreshHealthFeedback(int userSeq, LocalDate date) {
        // 1. 기존 피드백 즉시 반환
        HealthSummaryResponse currentFeedback = getHealthFeedback(userSeq, date);

        // 2. 백그라운드에서 새로운 피드백 생성 시작
        generateFeedbackAsync(userSeq, date);

        return currentFeedback;
    }

    /**
     * 비동기로 건강 피드백 생성/업데이트
     *
     * @param userSeq 사용자 ID
     * @param date 생성할 날짜 (null이면 오늘)
     */
    @Async("healthFeedbackExecutor")
    @Transactional
    public void generateFeedbackAsync(int userSeq, LocalDate date) {
        try {
            LocalDate targetDate = (date != null) ? date : LocalDate.now();
            log.info("사용자 {} {} 건강 피드백 생성 시작", userSeq, targetDate);

            User user = validateUser(userSeq);

            // 해당 날짜의 피드백이 있는지 확인
            UserHealthFeedback feedback = findFeedbackByDate(user, targetDate);

            if (feedback != null) {
                // 해당 날짜 피드백이 있으면 -> 업데이트
                log.info("사용자 {}의 {} 피드백이 이미 존재합니다. 업데이트를 진행합니다.",
                        userSeq, targetDate);
                updateExistingFeedback(feedback, userSeq, targetDate);
            } else {
                // 해당 날짜 피드백이 없으면 -> 새로 생성
                log.info("사용자 {}의 {} 피드백이 없습니다. 새로 생성합니다.",
                        userSeq, targetDate);
                createNewFeedback(user, userSeq, targetDate);
            }

            log.info("사용자 {} {} 건강 피드백 생성 완료", userSeq, targetDate);

        } catch (Exception e) {
            log.error("사용자 {} 건강 피드백 생성 실패", userSeq, e);
        }
    }


    /**
     * 동기 방식 (즉시 생성 - 테스트용)
     */
    @Transactional
    public HealthSummaryRequest generateFeedbackSync(int userSeq) {
        User user = validateUser(userSeq);

        DailyHealthResponse dailyHealthResponse = healthService.getTodayHealthData(userSeq);
        String healthData = dailyHealthResponse.toString();

        HealthSummaryRequest gptResponse = gptService.generateHealthSummary(healthData);

        UserHealthFeedback feedback = feedbackRepository.findByUser(user)
                .orElse(UserHealthFeedback.builder()
                        .user(user)
                        .build());

        feedback.updateFeedback(String.valueOf(gptResponse.getStatus()), gptResponse.getSummary());
        feedbackRepository.save(feedback);

        return gptResponse;
    }

    /**
     * 오늘 피드백 생성 (편의 메서드)
     */
    public void generateTodayFeedbackAsync(int userSeq) {
        generateFeedbackAsync(userSeq, null);
    }

    /**
     * 새로운 피드백 생성
     */
    private void createNewFeedback(User user, int userSeq, LocalDate date) {
        // 해당 날짜의 건강 데이터 조회
        DailyHealthDetailResponse dailyHealthResponse = healthService.getDailyHealthDetailByDate(userSeq, date);
        String healthData = dailyHealthResponse.toString();

        HealthSummaryRequest gptResponse = gptService.generateHealthSummary(healthData);

        UserHealthFeedback feedback = UserHealthFeedback.builder()
                .user(user)
                .healthStatus(String.valueOf(gptResponse.getStatus()))
                .content(gptResponse.getSummary())
                .createdAt(date.atStartOfDay())
                .build();

        feedbackRepository.save(feedback);
        log.info("사용자 {}의 {} 피드백 생성 완료", userSeq, date);
    }

    /**
     * 특정 날짜의 건강 피드백 조회
     */
    public HealthSummaryResponse getHealthFeedback(int userSeq, LocalDate date) {
        User user = validateUser(userSeq);
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        UserHealthFeedback feedback = findFeedbackByDate(user, targetDate);

        if (feedback == null) {
//            if (targetDate.equals(LocalDate.now())) {
                generateFeedbackAsync(userSeq, targetDate);
                return HealthSummaryResponse.analyzing();
//            }
        }

        // ✅ 엔티티에서 날짜 정보 가져와서 응답 생성
        return HealthSummaryResponse.builder()
                .status(HealthSummaryResponse.HealthStatus.valueOf(feedback.getHealthStatus()))
                .summary(feedback.getContent())
                .createdAt(feedback.getCreatedAt())  // 엔티티의 생성일
                .updatedAt(feedback.getUpdatedAt())  // 엔티티의 수정일
                .build();
    }

    /**
     * 기존 피드백 업데이트 (오늘 날짜에 이미 피드백이 있을 때)
     */
    private void updateExistingFeedback(UserHealthFeedback feedback, int userSeq, LocalDate date) {
        //건강 데이터 조회
        DailyHealthDetailResponse dailyHealthDetailResponse = healthService.getDailyHealthDetailByDate(userSeq, date);
        String healthData = dailyHealthDetailResponse.toString();

        log.info("건강 데이터 정보: {}", healthData);

        // GPT로 건강 한줄평 생성
        HealthSummaryRequest gptResponse = gptService.generateHealthSummary(healthData);

        // 기존 피드백 업데이트
        feedback.updateFeedback(
                String.valueOf(gptResponse.getStatus()),
                gptResponse.getSummary()
        );

        // ✅ 기존 피드백 업데이트 (updatedAt은 자동 갱신됨)
        feedback.updateFeedback(
                String.valueOf(gptResponse.getStatus()),
                gptResponse.getSummary()
        );

        feedbackRepository.flush();
        log.info("사용자 {}의 기존 피드백 업데이트 완료", userSeq);
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
