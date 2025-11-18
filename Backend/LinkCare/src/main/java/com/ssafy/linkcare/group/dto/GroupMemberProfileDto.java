package com.ssafy.linkcare.group.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupMemberProfileDto {
    private Long userId;
    private String userName;
    private String petName;
    private boolean isLeader;
    private String mainCharacterImageUrl;
    private String mainBackgroundImageUrl;
}
