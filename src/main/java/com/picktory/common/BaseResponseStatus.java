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
    FORBIDDEN(false, 403, "이 리소스에 대한 접근 권한이 없습니다."),
    ALREADY_DELETED_USER(false, 410, "이미 탈퇴한 유저입니다."),

    BUNDLE_DAILY_LIMIT_EXCEEDED(false, 400, "하루에 최대 10개의 보따리만 생성할 수 있습니다."),
    BUNDLE_NAME_REQUIRED(false, 400, "보따리 이름을 입력하세요."),
    BUNDLE_DESIGN_REQUIRED(false, 400, "보따리 디자인을 선택하세요."),
    BUNDLE_MINIMUM_GIFTS_REQUIRED(false, 400, "보따리는 최소 2개의 선물을 포함해야 합니다."),


    BUNDLE_ACCESS_DENIED(false, 403, "보따리 수정 권한이 없습니다."),
    BUNDLE_NOT_FOUND(false, 404, "보따리를 찾을 수 없습니다."),
    INVALID_BUNDLE_STATUS(false, 400, "이미 배달이 시작된 보따리입니다."),
    INVALID_CHARACTER_TYPE(false, 400, "유효하지 않은 배달부 캐릭터입니다."),
    INVALID_LINK(false, 400, "유효하지 않은 배달 링크입니다."),
    INVALID_BUNDLE_STATUS_FOR_COMPLETE(false, 400, "PUBLISHED 상태에서만 COMPLETED로 변경 가능합니다."),
    BUNDLE_ACCESS_DENIED(false, 403, "보따리 수정 권한이 없습니다."),



    /**
     * 500: Server 오류
     */
    DATABASE_ERROR(false, 503, "데이터베이스 연결에 실패하였습니다."),
    SERVER_ERROR(false, 500, "서버와의 연결에 실패하였습니다."),
    KAKAO_API_ERROR(false, 502, "카카오 API 호출 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(false, 500, "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    private final boolean isSuccess;
    private final int code;
    private final String message;
}