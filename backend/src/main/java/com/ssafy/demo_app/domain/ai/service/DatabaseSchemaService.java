package com.ssafy.demo_app.domain.ai.service;

import org.springframework.stereotype.Service;

@Service
public class DatabaseSchemaService {

    public String getSchemaDescription() {
        return """
                Table: item_master
                Description: 품목 마스터. 제품, 반제품, 원자재의 기준정보.
                Columns:
                - item_id: 품목 ID
                - item_code: 품목 코드
                - item_name: 품목명
                - spec: 규격
                - unit: 단위
                - item_type: 품목 유형
                - item_status: 품목 상태
                - safety_stock: 안전 재고

                Table: partner_master
                Description: 거래처 마스터. 공급처와 고객사 기준정보.
                Columns:
                - partner_id: 거래처 ID
                - partner_code: 거래처 코드
                - partner_name: 거래처명
                - business_no: 사업자등록번호
                - partner_type: 거래처 유형
                - partner_status: 거래처 상태

                Table: inbound_receipt
                Description: 입고 내역.
                Columns:
                - inbound_id: 입고 ID
                - item_id: 품목 ID
                - partner_id: 거래처 ID
                - location_id: 로케이션 ID
                - worker_id: 작업자 ID
                - inbound_qty: 입고 수량
                - inbound_date: 입고 일자
                - status: 입고 상태
                - created_at: 생성 일시

                Table: current_inventory
                Description: 현재고 현황.
                Columns:
                - inventory_id: 재고 ID
                - item_id: 품목 ID
                - location_id: 로케이션 ID
                - lot_number: LOT 번호
                - current_qty: 현재 수량
                - first_inbound_date: 최초 입고일
                - updated_at: 수정 일시

                Table: inventory_transaction_history
                Description: 재고 수불 이력.
                Columns:
                - transaction_id: 수불 ID
                - item_id: 품목 ID
                - location_id: 로케이션 ID
                - worker_id: 작업자 ID
                - transaction_type: 수불 유형
                - quantity: 수량
                - reason_desc: 수불 사유
                - work_order_id: 작업 지시 ID
                - production_execution_id: 생산 실적 ID
                - original_transaction_id: 원본 수불 ID
                - created_at: 수불 생성 일시

                Table: warehouse_location
                Description: 창고 로케이션.
                Columns:
                - location_id: 로케이션 ID
                - location_code: 로케이션 코드
                - warehouse_name: 창고명
                - max_capacity: 최대 적재 용량
                - rack_row: 랙 행
                - rack_column: 랙 열
                - production_receipt_default: 생산 입고 기본 로케이션 여부

                Table: outbound_shipping
                Description: 출하 지시와 출하 처리 내역.
                Columns:
                - shipping_id: 출하 ID
                - shipping_no: 출하 번호
                - item_id: 품목 ID
                - partner_id: 거래처 ID
                - worker_id: 작업자 ID
                - request_qty: 요청 수량
                - shipped_qty: 출하 수량
                - shipping_type: 출하 유형
                - picking_location_id: 피킹 로케이션 ID
                - estimated_delivery: 예상 배송일
                - status: 출하 상태
                - shipped_at: 출하 일시
                - created_at: 생성 일시

                Table: work_order
                Description: 생산 작업 지시.
                Columns:
                - order_id: 작업 지시 ID
                - order_no: 작업 지시 번호
                - item_id: 품목 ID
                - routing_id: 라우팅 ID
                - target_qty: 목표 수량
                - bom_version: BOM 버전
                - status: 작업 지시 상태
                - plan_date: 계획일
                - created_at: 생성 일시
                - updated_at: 수정 일시

                Table: production_execution
                Description: 생산 실적.
                Columns:
                - execution_id: 생산 실적 ID
                - order_id: 작업 지시 ID
                - routing_id: 라우팅 ID
                - good_qty: 양품 수량
                - defect_qty: 불량 수량
                - defect_type: 불량 유형
                - defect_reason: 불량 사유
                - reworkable: 재작업 가능 여부
                - worker_id: 작업자 ID
                - man_hours_minutes: 작업 시간(분)
                - created_at: 실적 등록 일시

                Table: factory_routing
                Description: 공장, 라인, 공정 라우팅.
                Columns:
                - routing_id: 라우팅 ID
                - factory_name: 공장명
                - line_name: 라인명
                - operation_seq: 공정 순서
                - operation_name: 공정명
                - routing_status: 라우팅 상태

                Table: bom_structure
                Description: BOM 부품 구성.
                Columns:
                - bom_id: BOM ID
                - parent_item_id: 부모 품목 ID
                - child_item_id: 자식 품목 ID
                - quantity: 소요 수량
                - bom_version: BOM 버전
                - bom_status: BOM 상태
                """;
    }
}
