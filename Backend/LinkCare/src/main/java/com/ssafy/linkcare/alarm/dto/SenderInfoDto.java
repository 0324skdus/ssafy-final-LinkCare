package com.ssafy.linkcare.alarm.dto;

import com.ssafy.linkcare.user.entity.User;
import lombok.Getter;

@Getter
public class SenderInfoDto {
    private Long userPk;
    private String nickname;

    public SenderInfoDto(User sender) {
        this.userPk = sender.getUserPk();
        this.nickname = sender.getName(); // User 엔티티의 name 필드를 nickname으로 사용
    }
}
