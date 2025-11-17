package com.ssafy.linkcare.group.scheduler;

import com.ssafy.linkcare.gpt.dto.WeeklyHeaderResponse;
import com.ssafy.linkcare.gpt.service.GptService;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.provider.GroupHealthDataProvider;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.group.service.GroupService;
import com.ssafy.linkcare.health.dto.HealthStaticsResponse;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupHeaderScheduler {

    private final GroupRepository groupRepository;
    private final GroupHealthDataProvider groupHealthDataProvider;
    private final GptService gptService;
    private final TimeStampUtil timeStampUtil;

    /**
     * 매주 월요일 자정에 모든 그룹의 헤더 업데이트
     * (단, 이번 주에 생성된 그룹은 제외)
     */
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void updateAllGroupHeaders() {
        log.info("========================================");
        log.info("=== 주간 헤더 일괄 업데이트 시작 ===");
        log.info("========================================");

        List<Group> groups = groupRepository.findAll();
        log.info("전체 그룹 수: {}개", groups.size());

        // 이번 주에 생성되지 않은 그룹만 필터링
        List<Group> targetGroups = groups.stream()
                .filter(group -> !group.isCreatedThisWeek())
                .toList();

        log.info("헤더 업데이트 대상 그룹 수: {}개 (이번 주 신규 그룹 제외)", targetGroups.size());

        int successCount = 0;
        int failCount = 0;
        int skipCount = groups.size() - targetGroups.size();

        for (Group group : targetGroups) {
            try {
                updateGroupHeader(group.getGroupSeq());
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("그룹 {} 헤더 업데이트 실패: {}", group.getGroupSeq(), e.getMessage(), e);
            }
        }

        log.info("========================================");
        log.info("=== 주간 헤더 일괄 업데이트 완료 ===");
        log.info("성공: {}개, 실패: {}개, 스킵: {}개 (신규 그룹)", successCount, failCount, skipCount);
        log.info("========================================");
    }

    /**
     * 특정 그룹의 헤더 업데이트
     */
    @Transactional
    public WeeklyHeaderResponse updateGroupHeader(Long groupSeq) {
        log.info("=== 그룹 {} 헤더 생성 시작 ===", groupSeq);

        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new EntityNotFoundException("그룹을 찾을 수 없습니다"));

        // 이번 주 생성된 그룹이면 스킵
//        if (group.isCreatedThisWeek()) {
//            log.warn("그룹 {} - 이번 주 생성된 그룹, 헤더 업데이트 스킵", groupSeq);
//            return WeeklyHeaderResponse.builder()
//                    .headerMessage(group.getWeeklyHeaderMessage())
//                    .generatedAt(group.getHeaderGeneratedAt())
//                    .build();
//        }

        // 1. 지난 주 데이터 조회
        LocalDate lastWeekStart = timeStampUtil.getLastWeekMonday();
        LocalDate lastWeekEnd = timeStampUtil.getLastWeekSunday();

        log.info("조회 기간: {} ~ {}", lastWeekStart, lastWeekEnd);

        List<HealthStaticsResponse> healthStats = groupHealthDataProvider.getLastWeekGroupHealthStats(
                groupSeq,
                lastWeekStart,
                lastWeekEnd
        );

        log.info("조회된 멤버 수: {}명", healthStats.size());

        // 2. GPT 포맷 변환
        String groupHealthData = gptService.formatHealthDataForGPT(healthStats);
        log.info("=== GPT 전달 데이터 ===\n{}\n=== 데이터 끝 ===", groupHealthData);

        // 3. GPT 호출
        log.info("GPT API 호출 시작...");
        WeeklyHeaderResponse gptResponse = gptService.generateWeeklyHeader(groupHealthData);
        log.info("GPT 생성 결과: {}", gptResponse.getHeaderMessage());

        // 4. DB 저장
//        LocalDate thisWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
//        LocalDate thisWeekSunday = thisWeekMonday.plusDays(6);

        group.updateWeeklyHeader(
                gptResponse.getHeaderMessage(),
                LocalDateTime.now()
        );

        groupRepository.save(group);

        log.info("=== 그룹 {} 헤더 생성 완료 ===", groupSeq);

        return WeeklyHeaderResponse.builder()
                .headerMessage(group.getWeeklyHeaderMessage())
                .generatedAt(group.getHeaderGeneratedAt())
                .build();
    }
}