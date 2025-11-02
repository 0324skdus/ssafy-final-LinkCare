package com.ssafy.linkcare.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Map<String, String> validation;  // Validation 에러용

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .message(message)
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> validation) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .message(errorCode.getMessage())
                .validation(validation)
                .build();
    }
}
