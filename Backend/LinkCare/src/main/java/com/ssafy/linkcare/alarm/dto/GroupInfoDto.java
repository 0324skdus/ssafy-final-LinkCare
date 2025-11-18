package com.ssafy.linkcare.alarm.dto;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.enums.GroupType;
import lombok.Getter;

@Getter
public class GroupInfoDto {
    private Long groupSeq;
    private String groupName;
    private GroupType groupType;

    public GroupInfoDto(Group group) {
        this.groupSeq = group.getGroupSeq();
        this.groupName = group.getGroupName();
        this.groupType = group.getType();
    }
}
