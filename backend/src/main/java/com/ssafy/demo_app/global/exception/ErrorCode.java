package com.ssafy.demo_app.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    DUPLICATE_EMPLOYEE_NO(HttpStatus.CONFLICT, "이미 사용 중인 사번입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "품목을 찾을 수 없습니다."),
    ITEM_CODE_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 품목 코드입니다."),
    ITEM_HAS_REFERENCES(HttpStatus.CONFLICT, "다른 기능에서 참조 중인 품목입니다. 삭제 확인 후 force=true로 다시 요청하세요."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "거래처를 찾을 수 없습니다."),
    PARTNER_CODE_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 거래처 코드입니다."),
    PARTNER_HAS_REFERENCES(HttpStatus.CONFLICT, "입고 또는 출하 이력에서 참조 중인 거래처입니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "로케이션을 찾을 수 없습니다."),
    INBOUND_NOT_FOUND(HttpStatus.NOT_FOUND, "입고 정보를 찾을 수 없습니다."),
    INBOUND_STATUS_INVALID(HttpStatus.BAD_REQUEST, "입고 상태가 올바르지 않습니다."),
    INBOUND_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "완료된 입고는 삭제할 수 없습니다."),
    LOCATION_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "로케이션 적재 용량을 초과했습니다."),
    LOCATION_CODE_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 로케이션 코드입니다."),
    LOCATION_HAS_INVENTORY(HttpStatus.BAD_REQUEST, "재고가 존재하는 로케이션은 삭제할 수 없습니다."),
    SHIPPING_NOT_FOUND(HttpStatus.NOT_FOUND, "출하 지시 정보를 찾을 수 없습니다."),
    SHIPPING_NO_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 출하 지시 번호입니다."),
    SHIPPING_STATUS_INVALID(HttpStatus.BAD_REQUEST, "출하 상태가 올바르지 않습니다."),
    WORK_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "작업 지시를 찾을 수 없습니다."),
    WORK_ORDER_STATUS_INVALID(HttpStatus.BAD_REQUEST, "작업 지시 상태가 올바르지 않습니다."),
    WORK_ORDER_INVALID_ITEM_TYPE(HttpStatus.BAD_REQUEST, "작업 지시는 반제품 또는 완제품에만 발행할 수 있습니다."),
    WORK_ORDER_HAS_EXECUTIONS(HttpStatus.CONFLICT, "생산 실적이 존재하는 작업 지시는 수정 또는 삭제할 수 없습니다."),
    WORK_ORDER_HAS_ISSUES(HttpStatus.CONFLICT, "자재 불출 이력이 존재하는 작업 지시는 수정 또는 삭제할 수 없습니다."),
    WORK_ORDER_CLOSE_UNDER_TARGET(HttpStatus.BAD_REQUEST, "목표 수량 미달 작업 지시는 허용 값이 있어야 마감할 수 있습니다."),
    PRODUCTION_EXECUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "생산 실적을 찾을 수 없습니다."),
    PRODUCTION_EXECUTION_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "생산 실적 수량이 올바르지 않습니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "자재 창고의 재고가 부족합니다."),
    BOM_NOT_FOUND(HttpStatus.NOT_FOUND, "BOM 구성 정보가 존재하지 않습니다."),
    BOM_DUPLICATE(HttpStatus.CONFLICT, "동일한 부모 품목, 자식 품목, 버전의 BOM이 이미 존재합니다."),
    BOM_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "BOM 소요량은 0보다 커야 합니다."),
    BOM_INVALID_ITEM_TYPE(HttpStatus.BAD_REQUEST, "BOM 품목 유형이 올바르지 않습니다."),
    BOM_SELF_REFERENCE(HttpStatus.BAD_REQUEST, "부모 품목과 자식 품목은 같을 수 없습니다."),
    BOM_CYCLE_DETECTED(HttpStatus.BAD_REQUEST, "BOM 계층에 순환 참조가 발생합니다."),
    ROUTING_NOT_FOUND(HttpStatus.NOT_FOUND, "라우팅 정보를 찾을 수 없습니다."),
    ROUTING_DUPLICATE(HttpStatus.CONFLICT, "동일한 공장/라인/공정 순서의 라우팅이 이미 존재합니다."),
    ROUTING_HAS_WORK_ORDER(HttpStatus.CONFLICT, "작업 지시에서 참조 중인 라우팅은 삭제할 수 없습니다."),
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
