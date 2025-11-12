package com.ssafy.linkcare.gpt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatChoice {
    private ChatMessage message;

    @JsonProperty("finish_reason")
    private String finishReason;
}
