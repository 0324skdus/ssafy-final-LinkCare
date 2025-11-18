package com.ssafy.linkcare.group.provider;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupMember;
import com.ssafy.linkcare.group.repository.GroupMemberRepository;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.group.service.GroupService;
import com.ssafy.linkcare.health.dto.HealthStaticsResponse;
import com.ssafy.linkcare.health.service.HealthService;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupHealthDataProvider {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TimeStampUtil timeStampUtil;
    private final HealthService healthService;

    /**
     * 그룹의 지난 주 건강 데이터 조회
     */
    public List<HealthStaticsResponse> getLastWeekGroupHealthStats(
            Long groupSeq,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Group group = validateGroup(groupSeq);

        List<GroupMember> members = groupMemberRepository
                .findByGroupWithUser(group);  // fetch join user

        return members.stream()
                .map(member -> {
                    try {
                        return healthService.getHealthStaticsData(
                                Math.toIntExact(member.getUser().getUserPk()),
                                startDate,
                                endDate
                        );
                    } catch (Exception e) {
                        log.error("Failed to get health data for user: {}",
                                member.getUser().getUserPk(), e);
                        return null;  // 또는 기본값
                    }
                })
                .filter(Objects::nonNull)  // null 제거
                .toList();
    }

    private Group validateGroup(Long groupSeq) {
        return groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }
}
