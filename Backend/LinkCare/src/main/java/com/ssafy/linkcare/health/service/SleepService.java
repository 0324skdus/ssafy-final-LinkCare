package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.SleepDataDto;
import com.ssafy.linkcare.health.dto.SleepSessionDto;
import com.ssafy.linkcare.health.entity.Sleep;
import com.ssafy.linkcare.health.entity.SleepSession;
import com.ssafy.linkcare.health.repository.SleepRepository;
import com.ssafy.linkcare.health.repository.SleepSessionRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SleepService {

    private final SleepRepository sleepRepository;
    private final SleepSessionRepository sleepSessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveAllSleepData(List<SleepDataDto> sleepList, int userSeq) {
        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        sleepList.forEach(dto -> {
            // 1. Sleep 저장
            Sleep savedSleep = sleepRepository.save(
                    Sleep.builder()
                            .deviceId(dto.getDeviceId())
                            .deviceType(dto.getDeviceType())
                            .uid(dto.getUid())
                            .zoneOffset(dto.getZoneOffset())
                            .startTime(dto.getStartTime())
                            .endTime(dto.getEndTime())
                            .dataSource(dto.getDataSource())
                            .duration(dto.getDuration())
                            .user(user)
                            .build()
            );

            // 2. Session 저장
            if(dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                List<SleepSession> sessions = dto.getSessions().stream()
                        .map(sessionDto -> SleepSession.builder()
                                .sleep(savedSleep)
                                .startTime(sessionDto.getStartTime())
                                .endTime(sessionDto.getEndTime())
                                .duration(sessionDto.getDuration())
                                .build())
                        .toList();

                sleepSessionRepository.saveAll(sessions);
            }
        });
    }

}
