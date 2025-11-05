package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.StepDto;
import com.ssafy.linkcare.health.entity.Step;
import com.ssafy.linkcare.health.repository.StepRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
                        .startTime(dto.getStartTime())
                        .end_time(dto.getEndTime())
                        .count(dto.getCount())
                        .goal(dto.getGoal())
                        .user(user)
                        .build()
                ).toList();

        stepRepository.saveAll(list);
    }
}
