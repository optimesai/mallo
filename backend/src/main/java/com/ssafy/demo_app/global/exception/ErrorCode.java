package com.ssafy.demo_app.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    DUPLICATE_EMPLOYEE_NO(HttpStatus.CONFLICT, "이미 사용 중인 사번입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "로케이션을 찾을 수 없습니다."),
    INBOUND_NOT_FOUND(HttpStatus.NOT_FOUND, "입고 정보를 찾을 수 없습니다."),
    INBOUND_STATUS_INVALID(HttpStatus.BAD_REQUEST, "입고 상태가 올바르지 않습니다."),
    INBOUND_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "완료된 입고는 삭제할 수 없습니다."),
    LOCATION_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "로케이션 적재 용량을 초과했습니다."),
    LOCATION_CODE_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 로케이션 코드입니다."),
    LOCATION_HAS_INVENTORY(HttpStatus.BAD_REQUEST, "재고가 존재하는 로케이션은 삭제할 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
