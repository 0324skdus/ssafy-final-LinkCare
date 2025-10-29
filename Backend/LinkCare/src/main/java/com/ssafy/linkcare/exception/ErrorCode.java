package com.ssafy.linkcare.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다"),

    // 이메일 인증 관련
    VERIFICATION_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었거나 존재하지 않습니다"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다"),

    // 401 Unauthorized
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),

    // 409 Conflict
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다"),

    // Google OAuth2
    INVALID_GOOGLE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 Google 토큰입니다"),
    GOOGLE_TOKEN_VERIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Google 토큰 검증에 실패했습니다"),

    // Kakao 로그인 에러
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 토큰 발급에 실패했습니다"),
    KAKAO_USER_INFO_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 사용자 정보 조회에 실패했습니다"),

    //  그룹 관련 에러 (Group)

    // 404 Not Found
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다"),
    GROUP_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹 멤버를 찾을 수 없습니다"),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "가입 신청을 찾을 수 없습니다"),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 링크를 찾을 수 없습니다"),

    // 403 Forbidden
    NOT_GROUP_LEADER(HttpStatus.FORBIDDEN, "그룹 방장 권한이 없습니다"),
    ALREADY_GROUP_MEMBER(HttpStatus.FORBIDDEN, "이미 그룹 멤버입니다"),

    // 409 Conflict
    GROUP_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "그룹 정원이 초과되었습니다"),
    DUPLICATE_JOIN_REQUEST(HttpStatus.CONFLICT, "이미 가입 신청한 그룹입니다"),

    // 400 Bad Request
    INVALID_INVITATION_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 링크입니다"),
    INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 초대 링크입니다"),
    JOIN_REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 가입 신청입니다");

    private final HttpStatus status;
    private final String message;
}
