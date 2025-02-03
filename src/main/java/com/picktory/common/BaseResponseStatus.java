package com.picktory.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BaseResponseStatus {
    /**
     * 200: 성공 응답
     */
    SUCCESS(true, 200, "요청에 성공하였습니다."),

    /**
     * 400: Client 오류 (잘못된 요청)
     */
    INVALID_JWT(false, 401, "유효하지 않은 JWT입니다."),
    INVALID_USER_JWT(false, 403, "권한이 없는 유저의 접근입니다."),
    USER_NOT_FOUND(false, 404, "존재하지 않는 유저입니다."),
    ALREADY_DELETED_USER(false, 410, "이미 탈퇴한 유저입니다."),

    /**
     * 500: Server 오류
     */
    DATABASE_ERROR(false, 503, "데이터베이스 연결에 실패하였습니다."),
    SERVER_ERROR(false, 500, "서버와의 연결에 실패하였습니다."),
    KAKAO_API_ERROR(false, 502, "카카오 API 호출 중 오류가 발생했습니다.");

    private final boolean isSuccess;
    private final int code;
    private final String message;
}