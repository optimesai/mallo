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

### 품목 통계 서버 오류 방지 (Codex)
- **User Intent**: 품목마스터에서만 서버 오류가 발생하고 이전 수정사항이 반영되지 않은 것처럼 보여, 원인 재확인과 수정 요청
- **Agent Context**: 새로 추가한 `/api/items/stats` 호출이 실행 중인 구버전 백엔드에서 `/api/items/{id}`로 오인되어 문자열 `stats`를 숫자 ID로 변환하다 500 처리될 수 있는 경로를 확인. 별도 신규 엔드포인트 의존을 제거하고 기존 품목 목록 API의 `totalElements`로 전체 기준 통계를 계산하도록 변경.
- **Key Decisions**:
  - 품목 통계는 `GET /api/items`를 전체/활성/비활성 조건으로 조회해 계산 — 기존 운영 중인 품목 목록 API만 사용하여 서버 재시작 전후 라우팅 불일치 위험 축소
  - 품목 단건 조회 경로는 숫자 ID만 받도록 제한 — `/stats`, `/duplicates` 같은 고정 경로가 단건 조회로 오인되는 재발 방지
  - 대시보드 최종 생산량, READY 출하 대기, 현재고 활성 품목 조건, 인사이트 지표명 표시가 현재 코드에 남아 있음을 grep으로 재확인 — 사용자 요청 범위의 반영 상태 검증
- **Affected Files**: <details><summary>9개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemApi.java` (+1/-6) — 품목 통계 엔드포인트 제거 및 숫자 ID 경로 제한
    - `backend/src/main/java/com/ssafy/demo_app/api/item/ItemController.java` (+0/-6) — 품목 통계 컨트롤러 제거
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/repository/ItemMasterRepository.java` (+0/-1) — 미사용 상태별 count 메서드 제거
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemService.java` (+0/-3) — 미사용 품목 통계 서비스 계약 제거
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemServiceImpl.java` (+0/-10) — 미사용 품목 통계 구현 제거
    - `frontend/src/api/itemMasterApi.ts` (+0/-5) — `/api/items/stats` 호출 제거
    - `frontend/src/services/itemMasterService.ts` (+10/-2) — 기존 목록 API 3회 조회로 전체/활성/비활성 통계 계산
  - **Deleted**:
    - `backend/src/main/java/com/ssafy/demo_app/api/item/dto/ItemStatsResponse.java` — 서버 전용 통계 DTO 제거

  </details>
