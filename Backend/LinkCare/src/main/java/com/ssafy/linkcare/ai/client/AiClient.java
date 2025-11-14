package com.ssafy.linkcare.ai.client;

import com.ssafy.linkcare.ai.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * FastAPI AI 서버와 통신하는 클라이언트
 * 4가지 메트릭(Steps, Kcal, Duration, Distance)에 대한 성장률 예측
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiClient {

    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;

    /**
     * Steps 성장률 예측
     */
    public AiPredictResponse predictSteps(StepsPredictRequest request) {
        log.info("AI 서버 호출: Steps 성장률 예측 - memberCount={}, avgAge={}, avgBmi={}",
                request.getMember_count(), request.getAvg_age(), request.getAvg_bmi());

        return aiWebClient.post()
                .uri("/predict/steps")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiPredictResponse.class)
                .block();
    }

    /**
     * Kcal 성장률 예측
     */
    public AiPredictResponse predictKcal(KcalPredictRequest request) {
        log.info("AI 서버 호출: Kcal 성장률 예측 - memberCount={}, avgAge={}, avgBmi={}",
                request.getMember_count(), request.getAvg_age(), request.getAvg_bmi());

        return aiWebClient.post()
                .uri("/predict/kcal")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiPredictResponse.class)
                .block();
    }

    /**
     * Duration 성장률 예측
     */
    public AiPredictResponse predictDuration(DurationPredictRequest request) {
        log.info("AI 서버 호출: Duration 성장률 예측 - memberCount={}, avgAge={}, avgBmi={}",
                request.getMember_count(), request.getAvg_age(), request.getAvg_bmi());

        return aiWebClient.post()
                .uri("/predict/duration")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiPredictResponse.class)
                .block();
    }

    /**
     * Distance 성장률 예측
     */
    public AiPredictResponse predictDistance(DistancePredictRequest request) {
        log.info("AI 서버 호출: Distance 성장률 예측 - memberCount={}, avgAge={}, avgBmi={}",
                request.getMember_count(), request.getAvg_age(), request.getAvg_bmi());

        return aiWebClient.post()
                .uri("/predict/distance")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiPredictResponse.class)
                .block();
    }
}
