package com.ssafy.linkcare.gpt.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.gpt.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

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
    public HealthSummaryResponse generateHealthSummary(String healthDataText) {
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
                
                4. 한줄평은 30자 이내
                
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
            당신은 건강 그룹의 동기부여를 돕는 AI 코치입니다.
            지난 주 그룹원들의 건강 데이터를 분석하여 이번 주 목표 문구를 만들어주세요.
            
            응답 형식:
            [20자 이내의 동기부여 문구]
            
            예시:
            걷기에 더 신경써 보아요
            """;

        String response = getChatCompletion(groupHealthDataText, systemPrompt);

        return new WeeklyHeaderResponse(response.trim());
    }

    /**
     * GPT 응답을 HealthSummaryResponse로 파싱
     */
    private HealthSummaryResponse parseHealthSummary(String response) {
        try {
            String[] lines = response.split("\n");
            String summary = "";
            HealthSummaryResponse.HealthStatus status = HealthSummaryResponse.HealthStatus.GOOD;

            for (String line : lines) {
                if (line.contains("한줄평:")) {
                    summary = line.replace("한줄평:", "").trim();
                } else if (line.contains("건강상태:")) {
                    String statusText = line.replace("건강상태:", "").trim();
                    if (statusText.contains("완벽")) {
                        status = HealthSummaryResponse.HealthStatus.PERFECT;
                    } else if (statusText.contains("주의")) {
                        status = HealthSummaryResponse.HealthStatus.CAUTION;
                    } else {
                        status = HealthSummaryResponse.HealthStatus.GOOD;
                    }
                }
            }

            return new HealthSummaryResponse(summary, status);

        } catch (Exception e) {
            log.error("GPT 응답 파싱 실패: {}", response, e);
            return new HealthSummaryResponse("건강 데이터를 분석했습니다.", HealthSummaryResponse.HealthStatus.GOOD);
        }
    }
}
