### 거래처 출하 목록 서버 오류 수정 (Codex)
- **User Intent**: 거래처 마스터 관리 화면에서 통계는 보이지만 서버 오류 배너가 표시되고, 출하 지시 및 피킹/상차 관리 화면에서는 목록이 뜨지 않으며 서버 오류가 발생하는 문제 수정 요청
- **Agent Context**: 거래처 화면은 `GET /api/partners`, 출하 지시와 피킹/상차 화면은 `GET /api/shippings` 및 등록 기준정보 호출 흐름에서 오류가 전파되는 구조로 진단. 출하 목록은 native projection으로 조회해 enum 매핑 오류 영향을 줄이고, 프론트는 상태 타입과 로딩 오류 표시를 분리.
- **Key Decisions**:
  - 출하 목록 조회는 엔티티 직접 로딩 대신 native projection을 사용 — 기존 DB에 예상 밖 상태값이 섞여도 목록 화면 전체가 500으로 실패하지 않도록 방어
  - 거래처 최근 출하일은 native 집계 조회로 계산 — 거래처 목록에서 출하 엔티티 enum 변환 실패가 전파되지 않도록 최소 변경
  - 프론트 출하 상태 타입은 백엔드 enum 전체와 일치 — `PACKING`, `INSPECTING`, `PARTIALLY_SHIPPED`, `CANCELED` 응답을 안전하게 렌더링
  - 출하 지시 화면의 목록 로딩과 등록 기준정보 로딩을 분리 — 고객사/품목 기준정보 실패가 출하 목록 실패처럼 표시되지 않도록 사용자 오류 지점 명확화
- **Affected Files**: <details><summary>9개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/repository/ShippingListProjection.java` — 출하 목록 native query 결과 projection 정의
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/shipping/dto/ShippingResponse.java` (+25/-0) — 출하 목록 projection 변환 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceImpl.java` (+41/-12) — 거래처 목록 정렬 검증, nullable 검색 보정, 최근 출하일 native 집계 사용
    - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/repository/OutboundShippingRepository.java` (+64/-0) — 출하 목록 native projection 조회와 거래처 최근 출하일 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/service/OutboundShippingServiceImpl.java` (+30/-28) — 출하 목록 projection 조회 전환, 상태 파라미터 검증, native pageable 정렬 보정
    - `backend/src/test/java/com/ssafy/demo_app/domain/shipping/service/OutboundShippingServiceTest.java` (+7/-2) — 프론트와 동일한 `createdAt,desc` Pageable 조회 회귀 테스트 추가
    - `frontend/src/api/shippingApi.ts` (+3/-1) — 출하 상태 타입을 백엔드 enum 전체로 확장
    - `frontend/src/views/PickingView.vue` (+3/-2) — 작업 대상 필터를 READY/PICKING으로 제한
    - `frontend/src/views/ShippingOrderView.vue` (+86/-26) — 출하 목록 로딩과 등록 기준정보 로딩 오류 분리, 전체 출하 상태 표시 보강
  - **Deleted**:
    - 없음

  </details>

### 현재고 0개 품목 안전재고 표시 수정 (Codex)
- **User Intent**: 현재고 현황 화면에서 안전재고 미달 품목만 표시해도 현재고가 0인 품목이 나오지 않는 문제를 수정 요청. 품목 마스터에 등록된 품목이면 재고 행이 없어도 현재고 0으로 보고 안전재고 미달 대상에 포함해야 한다는 요구.
- **Agent Context**: 기존 `GET /api/inventory`는 `current_inventory` 행 기준으로만 조회하여 품목 마스터에는 있지만 재고 행이 없는 품목을 응답할 수 없었다. 현재고 목록을 품목 마스터 기준 left join 집계 조회로 전환하고, 재고 행이 없는 품목은 `currentQty = 0` 및 nullable 위치 정보로 응답하도록 수정.
- **Key Decisions**:
  - 현재고 목록을 품목 마스터 기준 projection 조회로 전환 — 안전재고가 품목 단위 속성이므로 재고 행 기준보다 품목별 총 현재고 기준이 도메인 의미에 부합
  - 안전재고 미달 품목을 우선 정렬 — 프론트의 기존 클라이언트 필터 구조에서 경고 품목이 첫 페이지에 밀려나지 않도록 최소 변경
  - 재고 ID가 없는 0재고 품목은 상세 API 호출 없이 목록 응답을 상세 패널에 사용 — 존재하지 않는 `current_inventory` ID 조회를 방지
- **Affected Files**: <details><summary>7개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InventorySummaryProjection.java` — 품목 기준 현재고 요약 projection 정의
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/inventory/dto/CurrentInventoryResponse.java` (+13/-0) — 현재고 요약 projection 변환 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/CurrentInventoryRepository.java` (+40/-0) — 품목 마스터 기준 현재고 left join 집계 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/service/InventoryServiceImpl.java` (+19/-16) — 현재고 목록 조회를 projection 기반 집계 조회로 전환
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` (+23/-0) — 현재고 행이 없는 품목의 0재고 조회 회귀 테스트 추가
    - `frontend/src/api/inventoryApi.ts` (+4/-4) — 재고 ID, 위치, 수정일 nullable 타입 반영
    - `frontend/src/views/InventoryStatusView.vue` (+16/-9) — 0재고 품목 표시와 상세 패널 선택 처리 보정
  - **Deleted**:
    - 없음

  </details>

### 공정 실적 선택지 개선 (Codex)
- **User Intent**: 공정 실적 등록 화면의 실제 수행 공정 드롭다운에서 사진 1, 2처럼 선택지가 비어 있는 작업건이 있어, 사진 3처럼 해당 작업건의 당시 상태에서 선택 가능한 모든 공정을 선택할 수 있도록 수정 요청
- **Agent Context**: `frontend/src/views/ProductionExecutionView.vue`에서 `executionRoutingOptions`가 잔여 수량이 0보다 큰 공정만 필터링해 드롭다운 선택지를 축소하는 것이 원인으로 진단. 백엔드의 공정별 처리 가능 수량 검증은 유지하고, 프론트 선택지만 작업건의 전체 공정 진행 목록으로 확장.
- **Key Decisions**:
  - 프론트 View 계층에서 선택지 산식만 조정 — `agent/project/frontend.md`의 View → Store → Service → API 흐름을 유지하고 API 스키마 변경을 피하기 위함
  - 잔여 수량이 0인 공정도 드롭다운에 표시 — 사용자가 요청한 해당 작업건의 모든 공정 선택지 노출 요구를 충족하기 위함
  - 입력 수량이 선택 공정의 잔여 수량을 초과하면 프론트에서 먼저 차단 — 서버 검증을 우회하지 않으면서 사용자 피드백을 명확히 하기 위함
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ProductionExecutionView.vue` (+16/-5) — 실제 수행 공정 드롭다운을 작업건 전체 공정 목록 기반으로 변경하고 잔여 수량 초과 검증 추가
  - **Deleted**:
    - 없음

  </details>

### 출하 완료 확정 오류 수정 (Codex)
- **User Intent**: 차량 번호 입력 후 배정 및 피킹 지시를 실행하면 상태가 `PICKING`으로 바뀌고 재고가 차감되지만, 이후 출하 지시와 피킹/상차 화면의 `최종 출하 완료 처리` 버튼 실행 시 서버 오류가 발생하여 출하 완료 상태로 전환되지 않는 문제 수정 요청
- **Agent Context**: 피킹 배정 단계에서 `shippedQty`가 `0`으로 저장되는데 완료 단계는 `null`일 때만 요청 수량으로 보정하여, 정상 피킹 후 완료 흐름에서 완료 수량 확정이 누락되는 것으로 진단. 완료 단계는 재고 재차감 없이 요청 수량 전체를 확정하고 `SHIPPED` 상태로 전이하도록 수정.
- **Key Decisions**:
  - `completeShipping()`에서 `shippedQty`를 `requestQty`로 확정 — 피킹 단계에서 이미 요청 수량만큼 FIFO 재고를 예약/차감하는 현재 서비스 정책과 일치시키기 위함
  - 완료 상태를 `SHIPPED`로 직접 저장 — `최종 출하 완료 처리` 버튼의 의미가 부분 출하가 아닌 전체 출하 확정이기 때문
  - 피킹 배정 후 완료까지의 통합 테스트 추가 — 사용자 재현 경로를 테스트로 고정하여 회귀를 방지하기 위함
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/service/OutboundShippingServiceImpl.java` (+34/-35) — 완료 처리 시 요청 수량을 출하 완료 수량으로 확정하고 `SHIPPED` 상태 저장
    - `backend/src/test/java/com/ssafy/demo_app/domain/shipping/service/OutboundShippingServiceTest.java` (+61/-2) — 피킹 배정 후 완료 처리 통합 흐름 테스트 추가
  - **Deleted**:
    - 없음

  </details>
