package com.ssafy.linkcare.group.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MyGroupDetailResponseDto {
    private Long groupId;
    private String groupName;
    private List<GroupMemberProfileDto> members;
}
