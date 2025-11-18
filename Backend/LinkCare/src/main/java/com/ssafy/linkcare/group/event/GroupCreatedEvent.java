package com.ssafy.linkcare.group.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupCreatedEvent {
    private Long groupSeq;
}
