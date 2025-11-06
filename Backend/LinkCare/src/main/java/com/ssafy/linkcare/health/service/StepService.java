package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.StepDto;
import com.ssafy.linkcare.health.entity.Step;
import com.ssafy.linkcare.health.repository.StepRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StepService {

    private final UserRepository userRepository;
    private final StepRepository stepRepository;

    @Transactional
    public void saveAllStepData(List<StepDto> stepList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


        List<Step> list = stepList.stream()
                .map(dto -> Step.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .count(dto.getCount())
                        .goal(dto.getGoal())
                        .user(user)
                        .build()
                ).toList();

        stepRepository.saveAll(list);
    }

    @Transactional
    public void saveDailyStepData(StepDto dto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // TODO: 걸음수 업데이트 로직

        stepRepository.save(Step.builder()
                .deviceId(dto.getDeviceId())
                .deviceType(dto.getDeviceType())
                .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .count(dto.getCount())
                .goal(dto.getGoal())
                .user(user)
                .build()
        );
    }
}
