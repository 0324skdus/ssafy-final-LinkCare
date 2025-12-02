package com.ssafy.linkcare.alarm.service;

import com.ssafy.linkcare.alarm.dto.AlarmResponseDto;
import com.ssafy.linkcare.alarm.dto.AlarmSaveRequestDto;
import com.ssafy.linkcare.alarm.entity.Alarm;
import com.ssafy.linkcare.alarm.enums.MessageType;
import com.ssafy.linkcare.alarm.repository.AlarmRepository;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.notification.fcm.FCMService;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final FCMService fcmService;

    @Transactional
    public void saveAlarm(AlarmSaveRequestDto requestDto, User sender) {
        User receiver = userRepository.findById(requestDto.getReceiverUserPk())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Group group = groupRepository.findById(requestDto.getGroupSeq())
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        Alarm alarm = Alarm.builder()
                .sender(sender)
                .receiver(receiver)
                .group(group)
                .messageType(requestDto.getMessageType())
                .content(requestDto.getContent())
                .build();

        alarmRepository.save(alarm);

        // FCM 알림 전송 로직
        sendNotification(sender, receiver, alarm);
    }

    private void sendNotification(User sender, User receiver, Alarm alarm) {
        String title = sender.getName();
        String body;

        if (alarm.getMessageType() == MessageType.POKE) {
            body = alarm.getContent();
        } else if (alarm.getMessageType() == MessageType.LETTER) {
            body = "편지가 도착했어요";
        } else {
            // 다른 타입이 추가될 경우를 대비한 기본 처리
            body = "새로운 알림이 도착했습니다.";
        }

        fcmService.sendPushNotification(receiver, title, body);
    }

    public List<AlarmResponseDto> getAllAlarms(User user) {
        List<Alarm> AllAlarms = alarmRepository.findByReceiverOrderBySentAtDesc(user);
        return AllAlarms.stream()
                .map(AlarmResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void readAlarm(Long alarmId, Long userId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALARM_NOT_FOUND));

        // 알림의 수신자가 현재 로그인한 사용자인지 확인
        if (!alarm.getReceiver().getUserPk().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        alarm.read(); // isRead를 true로 변경
    }

    @Transactional
    public void deleteAlarm(Long userId, Long alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALARM_NOT_FOUND));

        // 알림의 수신자가 현재 로그인한 사용자인지 확인
        if (!alarm.getReceiver().getUserPk().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        alarmRepository.delete(alarm);
    }


}
