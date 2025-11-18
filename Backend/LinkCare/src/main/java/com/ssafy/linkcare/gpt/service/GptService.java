package com.ssafy.linkcare.gpt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.gpt.dto.*;
import com.ssafy.linkcare.health.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GptService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    private WebClient webClient;
    private final ObjectMapper objectMapper;

    private WebClient getWebClient() {
        if(webClient == null) {
            webClient = WebClient.builder()
//                    .baseUrl(apiUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return webClient;
    }

    /**
     * GPT API 호출하여 응답 받기
     */
    public String getChatCompletion(String prompt) {
        return getChatCompletion(prompt, null);
    }

    public String getChatCompletion(String prompt, String systemPrompt) {
         List<ChatMessage> messages = new ArrayList<>();

         // 시스템 프롬프트
        if(systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new ChatMessage("system", systemPrompt));
        }

        // 사용자 프롬프트 추가
        messages.add(new ChatMessage("user", prompt));

        // ChatRequest 생성
        ChatRequest request = new ChatRequest(model, messages, 1.0);

        // 상세 로그
        log.info("=== GPT API 요청 ===");
        log.info("URL: {}", apiUrl);
        log.info("API Key (앞 10자): {}", apiKey.substring(0, Math.min(10, apiKey.length())));
        log.info("Model: {}", model);
        log.info("Request Body: {}", request);

        try {
            ChatResponse chatResponse = getWebClient()
                    .post()
                    .uri(apiUrl)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> {
                                        log.error("400 에러 응답: {}", body);
                                        return new RuntimeException("API 에러: " + body);
                                    })
                    )
                    .bodyToMono(ChatResponse.class)
                    .block();

            if(chatResponse == null || chatResponse.getChoices().isEmpty()) {
                throw new CustomException(ErrorCode.AI_RESPONSE_NO_CONTENT);
            }

            return chatResponse.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("GPT API 호출 실패", e);
            throw new CustomException(ErrorCode.AI_PROVIDER_CALL_FAILED);
        }

    }

    /**
     * 개인 건강 데이터 한줄평 생성
     */
    public HealthSummaryRequest generateHealthSummary(String healthDataText) {
        String systemPrompt = """
                당신은 친근하고 따뜻한 건강 코치입니다.
                사용자의 하루 건강 데이터를 보고 **구체적인 격려나 조언**을 담은 한줄평을 작성해주세요.
                
                중요 규칙:
                1. **기록된 데이터를 우선적으로 분석**하세요
                   - 수면, 심박수, 혈압, 운동, 물 섭취 등 실제 기록된 항목에 집중
                   - 없는 데이터는 언급하지 말고, 있는 데이터로만 평가
                   - 만약 모든 데이터가 없다면 그때만 "오늘은 기록이 없네요" 언급
                
                2. 건강상태(완벽/양호/주의)는 기록된 데이터의 수치로만 판단
                   - 완벽: 모든 기록된 수치가 이상적
                   - 양호: 기록된 수치가 정상 범위
                   - 주의: 기록된 수치 중 개선 필요한 항목 있음
                
                3. 친근하고 격려하는 말투 (존댓말, 이모티콘 금지)
                
                4. 한줄평은 10자 이내
                
                응답 형식:
                한줄평: [기록된 데이터 기반 조언]
                건강상태: [완벽/양호/주의]
                
                예시:
                - 수면 7시간, 걸음수 없음:
                  한줄평: 수면은 충분해요. 잘하고 있어요
                  건강상태: 양호
                
                - 심박수 85bpm, 혈압 정상:
                  한줄평: 심박수가 약간 높네요. 휴식이 필요해요
                  건강상태: 주의
                
                - 운동 30분, 물 2L:
                  한줄평: 운동과 수분 섭취 완벽해요. 최고예요
                  건강상태: 완벽
                
                - 모든 데이터 없음:
                  한줄평: 오늘은 기록이 없네요. 내일부터 시작해봐요
                  건강상태: 양호
            """;

        String response = getChatCompletion(healthDataText, systemPrompt);

        // 응답 파싱
        return parseHealthSummary(response);
    }

    /**
     * 그룹 주간 헤더 문구 생성
     */
    public WeeklyHeaderResponse generateWeeklyHeader(String groupHealthDataText) {
        String systemPrompt = """
            당신은 건강 그룹의 데이터 분석가이자 응원단장입니다.
            지난 주 그룹원들의 건강 데이터를 분석하여, 가장 부족했던 부분을 자연스럽게 개선하도록 격려하는 문구를 만들어주세요.
            
            [분석 방법]
            1. 그룹 전체의 데이터에서 가장 부족한 영역 찾기 (걸음수, 수면, 운동, 수분 등)
            2. 그 부분을 이번 주에 개선할 수 있도록 부드럽게 유도
            
            [중요한 톤 가이드]
            - "부족해요", "못했어요" 같은 부정적 표현 금지
            - 명령형(~하세요) 대신 제안형(~어때요?)이나 긍정형(~해봐요)
            - (~요)체를 활용하여 친근하게
            - 대괄호 [] 사용하지 말 것
            - 20자 이내로 간결하게
            
            [상황별 예시]
            
            걸음수가 부족했다면:
            ✅ "오늘은 가볍게 걸어볼까요? 🚶"
            ✅ "산책하기 좋은 날씨네요! ☀️"
            ✅ "한 정거장 먼저 내려볼까요? 💪"
            
            수면이 부족했다면:
            ✅ "충분한 휴식도 건강이에요 💤"
            ✅ "오늘은 일찍 자봐요! 🌙"
            ✅ "푹 자고 활력 충전해요 ⚡"
            
            운동이 부족했다면:
            ✅ "가벼운 스트레칭 어때요? 🤸"
            ✅ "10분 운동으로 시작해볼까요? 💪"
            ✅ "몸을 움직일 시간이에요! 🏃"
            
            수분섭취가 부족했다면:
            ✅ "물 한 잔의 여유 어때요? 💧"
            ✅ "수분 충전 잊지 마세요! 💦"
            ✅ "오늘은 물 자주 마셔봐요 🥤"
            
            나쁜 예시:
            ❌ "지난주 걸음수가 부족했어요" (부정적, 과거)
            ❌ "[운동 더 하기!]" (명령형, 대괄호)
            ❌ "이번 주는 열심히 해봐요" (모호함)
            
            응답 형식: 부족한 부분을 개선하도록 유도하는 20자 이내 격려 문구
            """;

        String response = getChatCompletion(groupHealthDataText, systemPrompt);

        return new WeeklyHeaderResponse(response.trim(), LocalDateTime.now());
    }

    /**
     * GPT 응답을 HealthSummaryResponse로 파싱
     */
    /**
     * GPT 응답을 HealthSummaryRequest로 파싱
     */
    private HealthSummaryRequest parseHealthSummary(String response) {
        try {
            String[] lines = response.split("\n");
            String summary = "";
            HealthSummaryRequest.HealthStatus status = HealthSummaryRequest.HealthStatus.GOOD;

            for (String line : lines) {
                if (line.contains("한줄평:")) {
                    summary = line.replace("한줄평:", "").trim();
                } else if (line.contains("건강상태:")) {
                    String statusText = line.replace("건강상태:", "").trim();
                    if (statusText.contains("완벽")) {
                        status = HealthSummaryRequest.HealthStatus.PERFECT;
                    } else if (statusText.contains("주의")) {
                        status = HealthSummaryRequest.HealthStatus.CAUTION;
                    } else {
                        status = HealthSummaryRequest.HealthStatus.GOOD;
                    }
                }
            }

            return new HealthSummaryRequest(summary, status);

        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패: {}", response, e);
            return new HealthSummaryRequest(
                    "(더미) 건강 데이터 분석 오류 발생.",
                    HealthSummaryRequest.HealthStatus.GOOD
            );
        }
    }

    public String formatHealthDataForGPT(List<HealthStaticsResponse> responses) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 그룹 건강 데이터 요약 ===\n");
        sb.append("총 멤버 수: ").append(responses.size()).append("명\n\n");

        for (int i = 0; i < responses.size(); i++) {
            HealthStaticsResponse data = responses.get(i);
            sb.append(String.format("【멤버 %d】\n", i + 1));

            if (data.getStepStats() != null) {
                sb.append("- 걸음 수: ").append(data.getStepStats()).append("\n");
            }
            if (data.getSleepStats() != null) {
                sb.append("- 수면: ").append(data.getSleepStats()).append("\n");
            }
            if (data.getExerciseStats() != null) {
                sb.append("- 운동: ").append(data.getExerciseStats()).append("\n");
            }
            if (data.getHeartRateStats() != null) {
                sb.append("- 심박수: ").append(data.getHeartRateStats()).append("\n");
            }
            if (data.getBloodPressureStats() != null) {
                sb.append("- 혈압: ").append(data.getBloodPressureStats()).append("\n");
            }
            if (data.getWaterIntakeStats() != null) {
                sb.append("- 수분 섭취: ").append(data.getWaterIntakeStats()).append("\n");
            }
            if (data.getWaterIntakeStats() != null) {
                sb.append("- 혈압: ").append(data.getBloodPressureStats()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 오늘의 건강 데이터 한줄평 생성
     */
    public TodayHealthSummaryResponse generateTodayHealthReviews(TodayHealthSummary healthData) {
        log.info("GPT 한줄평 생성 시작...");

        return TodayHealthSummaryResponse.builder()
                .dailyActivityReview(generateDailyActivityReview(
                        healthData.getActivitySummary(),
                        healthData.getExercise(),
                        healthData.getStep()
                ))
                .bloodPressureReview(generateBloodPressureReview(
                        healthData.getBloodPressure()
                ))
                .waterIntakeReview(generateWaterIntakeReview(
                        healthData.getWaterIntake()
                ))
                .sleepReview(generateSleepReview(
                        healthData.getSleep()
                ))
                .heartRateReview(generateHeartRateReview(
                        healthData.getHeartRate()
                ))
                .build();
    }

    /**
     * 일일 활동 리뷰 (ActivitySummary + Exercise + Step 통합)
     */
    private String generateDailyActivityReview(
            ActivitySummaryStaticsResponse activitySummary,
            ExerciseStatisticsResponse exercise,
            int step) {

        String dataJson = createDailyActivityJson(activitySummary, exercise, step);
        log.info("DailyActivity 데이터:\n{}", dataJson);

        String systemPrompt = """
            운동 및 활동 데이터를 한 줄로 요약해주세요.
            
            [요약 방식]
            - 격려나 평가가 아닌 데이터 그 자체를 요약
            - 걸음수, 운동시간 등 주요 수치 포함
            - 객관적인 상태 표현
            
            좋은 예시:
            - "평균 5000보의 적정한 활동을 진행 중"
            - "오늘 3000보 활동 부족"
            - "운동 30분, 8000보 활발한 활동"
            
            나쁜 예시:
            - "오늘 정말 잘했어요!" ❌ (격려형)
            - "조금 더 움직여볼까요?" ❌ (권유형)
            
            응답 형식: 25자 이내 데이터 요약 (이모지 없이)
            """;

        String review = getChatCompletion(dataJson, systemPrompt).trim();
        log.info("DailyActivity 리뷰: {}", review);
        return review;
    }

    /**
     * 혈압 리뷰
     */
    private String generateBloodPressureReview(BloodPressureStaticsResponse bloodPressure) {
        String dataJson = toJson(bloodPressure);
        log.info("BloodPressure 데이터:\n{}", dataJson);

        String systemPrompt = """
            혈압 데이터를 한 줄로 요약해주세요.
            
            [요약 방식]
            - 수치를 포함한 객관적 요약
            - 정상/높음/낮음 등의 상태 표현
            
            좋은 예시:
            - "정상 혈압 120/80mmH 기록"
            - "평균 혈압 135/85mmH 약간 높음"
            - "수축기 110mmH 안정적 수치"
            
            나쁜 예시:
            - "혈압 관리 잘하고 계세요!" ❌ (격려형)
            
            응답 형식: 20자 이내 데이터 요약 (이모지 없이)
            """;

        String review = getChatCompletion(dataJson, systemPrompt).trim();
        log.info("BloodPressure 리뷰: {}", review);
        return review;
    }

    /**
     * 수분 섭취 리뷰
     */
    private String generateWaterIntakeReview(WaterIntakeStatisticsResponse waterIntake) {
        String dataJson = toJson(waterIntake);
        log.info("WaterIntake 데이터:\n{}", dataJson);

        String systemPrompt = """
            수분 섭취 데이터를 한 줄로 요약해주세요.
            
            [요약 방식]
            - 섭취량과 상태를 명시
            
            좋은 예시:
            - "하루 1L 섭취 기록"
            - "목표 2L 중 1.5L 섭취"
            - "오늘 800ml 섭취 부족"
            
            나쁜 예시:
            - "물 조금만 더 마셔봐요" ❌ (권유형)
            
            응답 형식: 20자 이내 데이터 요약 (이모지 없이)
            """;

        String review = getChatCompletion(dataJson, systemPrompt).trim();
        log.info("WaterIntake 리뷰: {}", review);
        return review;
    }

    /**
     * 수면 리뷰
     */
    private String generateSleepReview(SleepStatisticsResponse sleep) {
        String dataJson = toJson(sleep);
        log.info("Sleep 데이터:\n{}", dataJson);

        String systemPrompt = """
            수면 데이터를 한 줄로 요약해주세요.
            
            [요약 방식]
            - 수면 시간과 상태를 포함
            
            좋은 예시:
            - "평균 7.2시간의 적정한 수면을 유지 중"
            - "6시간 수면으로 부족한 상태"
            - "8시간 충분한 수면 기록"
            
            나쁜 예시:
            - "숙면하셨네요!" ❌ (평가형)
            
            응답 형식: 25자 이내 데이터 요약 (이모지 없이)
            """;

        String review = getChatCompletion(dataJson, systemPrompt).trim();
        log.info("Sleep 리뷰: {}", review);
        return review;
    }

    /**
     * 심박수 리뷰
     */
    private String generateHeartRateReview(HeartRateStaticsResponse heartRate) {
        String dataJson = toJson(heartRate);
        log.info("HeartRate 데이터:\n{}", dataJson);

        String systemPrompt = """
            심박수 데이터를 한 줄로 요약해주세요.
            
            [요약 방식]
            - 평균 심박수와 상태를 명시
            
            좋은 예시:
            - "평균 심박수 72bpm 정상 범위"
            - "안정 심박수 65bpm 양호"
            - "평균 85bpm 약간 높은 수치"
            
            나쁜 예시:
            - "심박수 안정적이에요!" ❌ (평가형)
            
            응답 형식: 20자 이내 데이터 요약 (이모지 없이)
            """;

        String review = getChatCompletion(dataJson, systemPrompt).trim();
        log.info("HeartRate 리뷰: {}", review);
        return review;
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * DailyActivity 통합 JSON 생성
     */
    private String createDailyActivityJson(
            ActivitySummaryStaticsResponse activity,
            ExerciseStatisticsResponse exercise,
            int step) {

        Map<String, Object> combined = new LinkedHashMap<>();
        combined.put("활동요약", activity != null ? activity : "데이터 없음");
        combined.put("운동", exercise != null ? exercise : "데이터 없음");
        combined.put("걸음수", step != 0 ? step : "데이터 없음");

        return toJson(combined);
    }

    /**
     * 객체를 JSON 문자열로 변환
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return "{\"message\": \"데이터 없음\"}";
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 실패: {}", obj.getClass().getSimpleName(), e);
            return "{\"error\": \"데이터 변환 실패\"}";
        }
    }
}

