
package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.gpt.dto.HealthSummaryResponse;
import com.ssafy.linkcare.gpt.service.GptService;
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
    public HealthSummaryResponse getHealthFeedback(int userSeq) {
        User user = validateUser(userSeq);

        UserHealthFeedback feedback = feedbackRepository.findByUser(user)
                .orElse(null);

        if (feedback == null) {
            // 최초 조회 시 - 분석 중 상태 반환하고 비동기로 생성 시작
            generateFeedbackAsync(userSeq);
            return HealthSummaryResponse.analyzing();
        }

        return HealthSummaryResponse.builder()
                .status(HealthSummaryResponse.HealthStatus.valueOf(feedback.getHealthStatus()))
                .summary(feedback.getContent())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    /**
     * 건강 데이터 동기화 시 피드백 갱신
     * - 기존 피드백을 먼저 반환
     * - 백그라운드에서 새 피드백 생성
     */
    public HealthSummaryResponse refreshHealthFeedback(int userSeq) {
        // 1. 기존 피드백 즉시 반환
        HealthSummaryResponse currentFeedback = getHealthFeedback(userSeq);

        // 2. 백그라운드에서 새로운 피드백 생성 시작
        generateFeedbackAsync(userSeq);

        return currentFeedback;
    }

    /**
     * 비동기로 건강 피드백 생성/업데이트
     */
    @Async("healthFeedbackExecutor")
    @Transactional
    public void generateFeedbackAsync(int userSeq) {
        try {
            log.info("사용자 {} 건강 피드백 생성 시작", userSeq);

            User user = validateUser(userSeq);

            // 오늘의 건강 데이터 조회
            DailyHealthResponse dailyHealthResponse = healthService.getTodayHealthData(userSeq);
            String healthData = dailyHealthResponse.toString();

            // GPT로 건강 한줄평 생성 (시간 소요)
            HealthSummaryResponse gptResponse = gptService.generateHealthSummary(healthData);

            // DB에 저장 또는 업데이트
            UserHealthFeedback feedback = feedbackRepository.findByUser(user)
                    .orElse(null);

            if (feedback == null) {
                // 새로 생성
                feedback = UserHealthFeedback.builder()
                        .user(user)
                        .healthStatus(String.valueOf(gptResponse.getStatus()))
                        .content(gptResponse.getSummary())
                        .build();
                feedbackRepository.save(feedback);
            } else {
                // 기존 것 업데이트
                feedback.updateFeedback(
                        String.valueOf(gptResponse.getStatus()),
                        gptResponse.getSummary()
                );

                feedbackRepository.save(feedback);  // 명시적 save
                feedbackRepository.flush();  // 즉시 DB 반영
            }

            log.info("사용자 {} 건강 피드백 생성 완료", userSeq);

        } catch (Exception e) {
            log.error("사용자 {} 건강 피드백 생성 실패", userSeq, e);
        }
    }

    /**
     * 동기 방식 (즉시 생성 - 테스트용)
     */
    @Transactional
    public HealthSummaryResponse generateFeedbackSync(int userSeq) {
        User user = validateUser(userSeq);

        DailyHealthResponse dailyHealthResponse = healthService.getTodayHealthData(userSeq);
        String healthData = dailyHealthResponse.toString();

        HealthSummaryResponse gptResponse = gptService.generateHealthSummary(healthData);

        UserHealthFeedback feedback = feedbackRepository.findByUser(user)
                .orElse(UserHealthFeedback.builder()
                        .user(user)
                        .build());

        feedback.updateFeedback(String.valueOf(gptResponse.getStatus()), gptResponse.getSummary());
        feedbackRepository.save(feedback);

        return gptResponse;
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
