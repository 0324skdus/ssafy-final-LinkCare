package com.ssafy.linkcare.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyHeaderResponse {
    private String headerMessage;  // 이번 주 목표 한줄 문구
}
