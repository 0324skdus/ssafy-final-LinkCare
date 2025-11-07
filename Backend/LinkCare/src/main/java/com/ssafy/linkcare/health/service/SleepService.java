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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

        for (SleepDataDto dto : sleepList) {// 1. Sleep 저장
            Sleep savedSleep = sleepRepository.save(
                    Sleep.builder()
                            .deviceId(dto.getDeviceId())
                            .deviceType(dto.getDeviceType())
                            .uid(dto.getUid())
                            .zoneOffset(dto.getZoneOffset())
                            .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .dataSource(dto.getDataSource())
                            .duration(dto.getDuration())
                            .user(user)
                            .build()
            );

            // 2. Session 저장
            if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                List<SleepSession> sessions = dto.getSessions().stream()
                        .map(sessionDto -> SleepSession.builder()
                                .sleep(savedSleep)
                                .startTime(sessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .endTime(sessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .duration(sessionDto.getDuration())
                                .build())
                        .toList();

                sleepSessionRepository.saveAll(sessions);
            }
        }
    }

    @Transactional
    public void saveDailySleepData(List<SleepDataDto> sleepList, int userSeq) {
        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(sleepList == null || sleepList.isEmpty()) {
            return;
        }

        // sleep 삭제 확인
        // 삼성헬스에서 받은 uid 리스트
        Set<String> sleepUids = sleepList.stream().map(SleepDataDto::getUid).collect(Collectors.toSet());
        LocalDate syncDate = sleepList.get(0).getEndTime().toLocalDate();

        // db 에 있는 수면 리스트
        List<Sleep> existingSleeps = sleepRepository.findByUserAndEndDate(user, syncDate);
        Set<String> existingUids = existingSleeps.stream().map(Sleep::getUid).collect(Collectors.toSet());

        // 삭제된 uid
        Set<String> uidsToDelete = existingUids.stream()
                .filter(uid -> !sleepUids.contains(uid))
                .collect(Collectors.toSet());

        if(!uidsToDelete.isEmpty()) {
            List<Sleep> sleepToDelete = existingSleeps.stream()
                            .filter(sleep -> uidsToDelete.contains(sleep.getUid())).toList();
            sleepRepository.deleteAll(sleepToDelete);
        }

        // 수정, 삭제 가능
        sleepList.forEach(dto -> {
            // 수면 데이터가 이미 있으면 교체
            Optional<Sleep> existingData = sleepRepository.findByUserAndUid(user, dto.getUid());
            if(existingData.isPresent()) {
                Sleep sleep = existingData.get();
                sleep.updateDuration(dto.getDuration());

                if(dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                    // 삼성헬스에서 받은 session 정보
                    Set<String> syncedSessionKeys = dto.getSessions().stream()
                            .map(session -> session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond()
                                    + "_" + session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .collect(Collectors.toSet());

                    // db에 저장된 session 정보
                    List<SleepSession> existingSessions = sleepSessionRepository.findBySleep(sleep);
                    Set<String> existingSessionKeys = existingSessions.stream()
                            .map(session -> session.getStartTime() + "_" + session.getEndTime())
                            .collect(Collectors.toSet());

                    // db에는 있지만 삼성헬스에는 없는 세션
                    Set<String> sessionKeysToDelete = existingSessionKeys.stream()
                            .filter(key -> !syncedSessionKeys.contains(key))
                            .collect(Collectors.toSet());

                    // 삭제된 세션들 삭제
                    if(!sessionKeysToDelete.isEmpty()) {
                        existingSessions.stream()
                                .filter(session -> sessionKeysToDelete.contains(
                                        session.getStartTime() + "_" + session.getEndTime()))
                                .forEach(sleepSessionRepository::delete);
                    }

                    dto.getSessions().forEach(sleepSessionDto -> {
                        Optional<SleepSession> existingSession = sleepSessionRepository.findBySleepAndStartTimeAndEndTime(sleep,
                                sleepSessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond(),
                                sleepSessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond());
                        if(existingSession.isPresent()) {
                            SleepSession session = existingSession.get();
                            session.updateDuration(sleepSessionDto.getDuration());
                        } else {
                            // 없으면 저장
                            sleepSessionRepository.save(SleepSession.builder()
                                    .startTime(sleepSessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .endTime(sleepSessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .duration(sleepSessionDto.getDuration())
                                    .build());
                        }
                    });
                } else {
                    sleepSessionRepository.deleteBySleep(sleep);
                }

                long remainingSessionCount = sleepSessionRepository.countBySleep(sleep);
                if(remainingSessionCount == 0) {
                    // 세션이 하나도 없으면 Sleep도 삭제
                    sleepRepository.delete(sleep);
                }
            }

            // 수면 데이터가 없다면 저장
            else {
                // 1. Sleep 저장
                Sleep savedSleep = sleepRepository.save(
                        Sleep.builder()
                                .deviceId(dto.getDeviceId())
                                .deviceType(dto.getDeviceType())
                                .uid(dto.getUid())
                                .zoneOffset(dto.getZoneOffset())
                                .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
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
                                    .startTime(sessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .endTime(sessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .duration(sessionDto.getDuration())
                                    .build())
                            .toList();

                    sleepSessionRepository.saveAll(sessions);
                }
            }
        });
    }
}
