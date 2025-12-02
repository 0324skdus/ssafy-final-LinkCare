package com.ssafy.linkcare.group.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupMemberChangedEvent {
    private Long groupSeq;
    private String changeType; // "ADDED", "REMOVED"
    private Long userSeq;
}
