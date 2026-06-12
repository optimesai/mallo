package com.ssafy.demo_app.domain.inventory.entity;

public enum TransactionType {
    RESERVATION,        // 재고 예약 (출하 PICKING 시 차감)
    INBOUND,            // 입고
    OUTBOUND,           // 출고
    PRODUCTION_ISSUE,   // 생산 투입
    ADJUSTMENT,         // 재고 조정 (실사 차이)
    TRANSFER_OUT,       // 로케이션 이동 - 출발
    TRANSFER_IN,        // 로케이션 이동 - 도착
    SCRAP,              // 폐기/불량 처리
    RETURN              // 거래처 반품
}
