package com.ssafy.linkcare.alarm.dto;

import com.ssafy.linkcare.alarm.entity.Alarm;
import com.ssafy.linkcare.alarm.enums.MessageType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AlarmResponseDto {
    private Long alarmId;
    private SenderInfoDto senderInfo;
    private GroupInfoDto groupInfo;
    private MessageType messageType;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;

    public AlarmResponseDto(Alarm alarm) {
        this.alarmId = alarm.getId();
        this.senderInfo = new SenderInfoDto(alarm.getSender());
        this.groupInfo = new GroupInfoDto(alarm.getGroup());
        this.messageType = alarm.getMessageType();
        this.content = alarm.getContent();
        this.sentAt = alarm.getSentAt();
        this.isRead = alarm.isRead();
    }
}
