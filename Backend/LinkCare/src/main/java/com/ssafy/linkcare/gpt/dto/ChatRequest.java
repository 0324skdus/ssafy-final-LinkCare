package com.ssafy.linkcare.gpt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
}
