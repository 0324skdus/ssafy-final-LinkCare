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
public class TokenUsage {
    @JsonProperty("prompt_tokens")
    private Integer promptTokens; // 질문 토큰수

    @JsonProperty("completion_tokens")
    private Integer completionTokens; // gpt가 생성한 답변의 토큰수

    @JsonProperty("total_tokens")
    private Integer totalTokens; // 전체 토큰수
}
