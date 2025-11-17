package com.ssafy.linkcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 * - 주간 목표 달성 기록 생성 시 그룹별 순차 처리를 위한 스레드 풀
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 목표 기록 생성 전용 스레드 풀
     * - 동시 처리: 3개 그룹
     * - 최대 처리: 5개 그룹
     * - 큐 크기: 10,000개 (대량 그룹 대비)
     */
    @Bean(name = "goalRecordExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);           // 기본 스레드 3개
        executor.setMaxPoolSize(5);            // 최대 스레드 5개
        executor.setQueueCapacity(10000);      // 큐 크기 10,000
        executor.setThreadNamePrefix("goal-record-");
        executor.setWaitForTasksToCompleteOnShutdown(true);  // 종료 시 작업 완료 대기
        executor.setAwaitTerminationSeconds(60);             // 최대 60초 대기
        executor.initialize();
        return executor;
    }

    @Bean(name = "healthFeedbackExecutor")
    public Executor healthFeedbackExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("health-feedback-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "headerTaskExecutor")
    public Executor headerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("header-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}
