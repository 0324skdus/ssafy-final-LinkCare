package com.ssafy.linkcare.alarm.dto;

import com.ssafy.linkcare.alarm.enums.MessageType;
import lombok.Getter;

@Getter
public class AlarmSaveRequestDto {
    private Long receiverUserPk;
    private Long groupSeq;
    private MessageType messageType;
    private String content;
}
