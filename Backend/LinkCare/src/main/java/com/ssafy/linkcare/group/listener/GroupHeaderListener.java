package com.ssafy.linkcare.group.listener;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.event.GroupCreatedEvent;
import com.ssafy.linkcare.group.event.GroupMemberChangedEvent;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.group.scheduler.GroupHeaderScheduler;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupHeaderListener {

    private final GroupRepository groupRepository;
    private final GroupHeaderScheduler groupHeaderScheduler;

    /**
     * 그룹 생성 이벤트 처리 - 즉시 헤더 생성
     */
    @EventListener
    @Async("headerTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleGroupCreated(GroupCreatedEvent event) {
        log.info("==================================================");
        log.info("그룹 생성 이벤트 수신: 그룹={}", event.getGroupSeq());
        log.info("신규 그룹은 기본 메시지 '함께 건강해져봐요!'로 시작");
        log.info("다음 주 월요일부터 GPT 헤더가 생성됩니다");
        log.info("==================================================");
    }

    /**
     * 그룹원 변경 이벤트 처리 - 이번 주 헤더 없으면 재생성
     */
    @EventListener
    @Async("headerTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleGroupMemberChanged(GroupMemberChangedEvent event) {
        log.info("==================================================");
        log.info("그룹원 변경 이벤트 수신: 그룹={}, 타입={}, 유저={}",
                event.getGroupSeq(), event.getChangeType(), event.getUserSeq());
        log.info("==================================================");

        try {
            Group group = validateGroup(event.getGroupSeq());

            if (shouldRegenerateHeader(group)) {
                log.info("그룹 {} - 이번 주 헤더 미생성 확인, 재생성 시작", event.getGroupSeq());
                groupHeaderScheduler.updateGroupHeader(event.getGroupSeq());
                log.info("그룹 {} 헤더 재생성 완료", event.getGroupSeq());
            } else {
                log.info("그룹 {} - 이번 주 헤더 이미 존재, 재생성 스킵", event.getGroupSeq());
            }
        } catch (Exception e) {
            log.error("그룹 {} 헤더 재생성 실패 (그룹원 변경): {}",
                    event.getGroupSeq(), e.getMessage(), e);
        }

        log.info("==================================================");
    }

    /**
     * 헤더 재생성이 필요한지 판단
     *
     * 재생성 조건:
     * 1. 그룹이 생성된 주가 아니어야 함 (신규 그룹은 기본 메시지 유지)
     * 2. 이번 주에 헤더가 생성되지 않았어야 함
     */
    private boolean shouldRegenerateHeader(Group group) {
        // 조건 1: 그룹이 이번 주에 생성되었으면 재생성 안함
        if (group.isCreatedThisWeek()) {
            log.debug("그룹 {} - 이번 주 생성된 그룹, 기본 메시지 유지 (재생성 안함)",
                    group.getGroupSeq());
            return false;
        }

        // 조건 2: 헤더가 아예 없는 경우 (이론상 발생하면 안되지만 안전장치)
        if (group.getHeaderGeneratedAt() == null) {
            log.debug("그룹 {} - 헤더 없음, 생성 필요", group.getGroupSeq());
            return true;
        }

        // 조건 3: 이번 주 월요일 이전에 생성된 헤더인 경우
        LocalDate thisWeekMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDateTime thisWeekStart = thisWeekMonday.atStartOfDay();

        boolean isOld = group.getHeaderGeneratedAt().isBefore(thisWeekStart);

        if (isOld) {
            log.debug("그룹 {} - 마지막 생성: {}, 이번 주 시작: {}, 재생성 필요",
                    group.getGroupSeq(),
                    group.getHeaderGeneratedAt(),
                    thisWeekStart);
        } else {
            log.debug("그룹 {} - 이번 주 헤더 이미 생성됨, 재생성 불필요",
                    group.getGroupSeq());
        }

        return isOld;
    }

    private Group validateGroup(Long groupSeq) {
        return groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

}
