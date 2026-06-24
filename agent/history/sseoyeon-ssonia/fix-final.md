### 대시보드 및 재고 집계 개선 (Codex)
- **User Intent**: 품목 마스터 카드가 현재 페이지 기준으로 활성/비활성 품목을 세고, 대시보드 생산/출하 카드와 현재고 모니터링 목록이 현업 기준과 다르게 집계되는 문제를 수정 요청
- **Agent Context**: 품목 통계는 거래처 마스터의 전체 통계 API 패턴을 따라 분리하고, 대시보드 생산량은 최종 생산 입고 이력 기준, 출하 대기는 `READY` 상태 기준, 현재고 목록은 활성 품목 마스터 기준으로 재정의
- **Key Decisions**:
  - 품목 카드는 프론트엔드 페이지 배열 필터링 대신 `GET /api/items/stats`를 사용 — 프론트엔드 4계층 흐름과 거래처 마스터 기존 패턴 준수
  - 최종 생산량은 `PRODUCTION_RECEIPT`와 `PRODUCTION_RECEIPT_CANCEL` 수불 이력의 순합으로 계산 — 마지막 공정에서만 생산 입고 이력이 생성되는 도메인 규칙 반영
  - 출하 대기 카드는 `OutboundShipping.ShippingStatus.READY`만 카운트 — 출하 지시 화면의 `출하 대기 (READY)` 라벨과 동일 기준 적용
  - 현재고 목록은 `item_master` left join 구조를 유지하고 `item_status = 'ACTIVE'` 조건만 추가 — 재고 0개 품목 포함과 비활성 품목 제외 요구를 동시에 만족
- **Affected Files**: <details><summary>17개 파일</summary>

  - **Created**:
    - `agent/implementation-plan-dashboard-inventory-fix.md` (+30/-0) — 구현 기획서
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemStatsResponse.java` (+15/-0) — 품목 전체 통계 응답 DTO
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemApi.java` (+5/-0) — 품목 통계 API 명세 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemController.java` (+6/-0) — 품목 통계 컨트롤러 구현
    - `backend/src/main/java/com/ssafy/demo_app/domain/dashboard/service/DashboardServiceImpl.java` (+6/-8) — 대시보드 카드 집계 기준 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/CurrentInventoryRepository.java` (+4/-2) — 활성 품목 기준 현재고 요약 조회
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InventoryTransactionHistoryRepository.java` (+11/-0) — 순 생산 입고 수량 합계 쿼리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/repository/ItemMasterRepository.java` (+1/-0) — 품목 상태별 카운트 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemService.java` (+3/-0) — 품목 통계 서비스 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemServiceImpl.java` (+10/-0) — 품목 전체/활성/비활성 통계 구현
    - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/repository/OutboundShippingRepository.java` (+1/-0) — 출하 상태별 카운트 메서드 추가
    - `frontend/src/api/itemMasterApi.ts` (+11/-0) — 품목 통계 API 타입 및 호출 추가
    - `frontend/src/services/itemMasterService.ts` (+10/-0) — 품목 통계 서비스 추가
    - `frontend/src/state/itemMasterStore.ts` (+18/-0) — 품목 통계 상태 및 액션 추가
    - `frontend/src/views/HomeView.vue` (+10/-1) — 인사이트 원천을 한국어 지표명으로 표시
    - `frontend/src/views/ItemMasterView.vue` (+18/-6) — 품목 카드 전체 통계 연동
  - **Deleted**:
    - 없음

  </details>
