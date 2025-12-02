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
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),

    // 이메일 인증 관련
    VERIFICATION_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었거나 존재하지 않습니다"),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다"),

    // 401 Unauthorized
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "캐릭터를 찾을 수 없습니다"),
    BACKGROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "배경을 찾을 수 없습니다"),
    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"), // 추가

    // 409 Conflict
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다"),
    ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다"),

    // Google OAuth2
    INVALID_GOOGLE_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 Google 토큰입니다"),
    GOOGLE_TOKEN_VERIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Google 토큰 검증에 실패했습니다"),

    // Kakao 로그인 에러
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 토큰 발급에 실패했습니다"),
    KAKAO_USER_INFO_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 사용자 정보 조회에 실패했습니다"),

    // 파일 업로드 관련 에러
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다"),

    //  그룹 관련 에러 (Group)

    // 404 Not Found
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다"),
    GROUP_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹 멤버를 찾을 수 없습니다"),
    JOIN_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "가입 신청을 찾을 수 없습니다"),
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 링크를 찾을 수 없습니다"),

    // 403 Forbidden
    NOT_GROUP_LEADER(HttpStatus.FORBIDDEN, "그룹 방장 권한이 없습니다"),
    ALREADY_GROUP_MEMBER(HttpStatus.FORBIDDEN, "이미 그룹 멤버임"),
    CANNOT_KICK_LEADER(HttpStatus.FORBIDDEN, "그룹장은 내보낼 수 없습니다"),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"), // 추가

    // 409 Conflict
    GROUP_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "그룹 정원이 초과되었습니다"),
    DUPLICATE_JOIN_REQUEST(HttpStatus.CONFLICT, "이미 가입 신청한 그룹입니다"),
    GROUP_CAPACITY_FULL(HttpStatus.CONFLICT, "그룹 정원이 가득 차서 초대 링크를 생성할 수 없습니다"),
    GROUP_REQUIRED_PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹 요구 권한을 찾을 수 없습니다"),

    // 400 Bad Request
    INVALID_INVITATION_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 링크입니다"),
    INVITATION_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 초대 링크입니다"),
    JOIN_REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 가입 신청입니다"),

    PERMISSION_AGREEMENT_INVALID(HttpStatus.BAD_REQUEST, "권한 동의 정보가 유효하지 않습니다"),
    HEALTH_PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "건강 정보 권한 설정을 찾을 수 없습니다"),

    // 알림 관련 에러 (Notification)
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),

    // 헬스 데이터 관련 에러
    HEALTH_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "건강 데이터를 찾을 수 없습니다"),

    // gpt
    AI_RESPONSE_NO_CONTENT(HttpStatus.NO_CONTENT, "Chat GPT 응답이 비어있습니다."),
    AI_PROVIDER_CALL_FAILED(HttpStatus.BAD_GATEWAY, "AI 호출 실패"),

    // AI 목표 생성 관련 에러
    INSUFFICIENT_DATA(HttpStatus.CONFLICT, "데이터가 부족합니다. 최근 3주 데이터가 필요합니다"),
    GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "이번 주 목표를 찾을 수 없습니다"),

    // AI 코멘트 관련 에러
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "한줄평을 찾을 수 없습니다"),
    NOT_GROUP_MEMBER_FOR_COMMENT(HttpStatus.FORBIDDEN, "해당 그룹의 멤버만 한줄평을 작성할 수 있습니다"),

    // 그룹 헤더 관련 에러
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요"),

    // 그룹 탈퇴/삭제 관련 에러
    LEADER_CANNOT_LEAVE_WITH_MEMBERS(HttpStatus.BAD_REQUEST, "그룹장은 다른 멤버가 있을 때 탈퇴할 수 없습니다. 먼저 그룹장을 위임하거나 그룹을 삭제하세요");

    private final HttpStatus status;
    private final String message;
}
