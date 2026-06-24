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

### 대시보드 최종 생산량 카드 수정 (Codex)
- **User Intent**: 대시보드 카드의 `라인 생산량`이 공정/라인별 실적을 각각 더해 최종 생산량 1개가 라인 5개일 때 5개로 보이는 문제만 수정 요청
- **Agent Context**: 기존 수불 입고 기준보다 마지막 공정 실적의 양품 수량이 “모든 공정을 거친 최종 생산량” 정의에 더 직접적으로 부합한다고 판단. 대시보드 카드 값만 마지막 공정 실적 합계로 변경하고, 라인별 분석 차트는 이번 범위에서 제외.
- **Key Decisions**:
  - 최종 생산량은 같은 공장/라인의 최대 `operationSeq` 라우팅에 등록된 `ProductionExecution.goodQty`만 합산 — 중간 공정 실적 중복 카운트 방지
  - 대시보드 카드 서비스만 신규 repository 쿼리를 호출 — 기존 API 응답 구조와 프론트엔드 변경 없이 카드 값만 수정
  - 미사용 수불 기반 생산량 합계 쿼리는 제거 — 카드 기준을 하나로 유지하고 혼동 방지
- **Affected Files**: <details><summary>4개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/dashboard/service/DashboardServiceImpl.java` (+1/-1) — 생산량 카드 값을 마지막 공정 양품 합계로 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/ProductionExecutionRepository.java` (+14/-0) — 마지막 공정 양품 합계 쿼리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InventoryTransactionHistoryRepository.java` (+0/-11) — 미사용 생산 입고 합계 쿼리 제거
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

### AI SQL 스키마 요약 리소스 추가 (Codex)
- **User Intent**: AI가 SQL을 생성할 때 DDL 또는 스키마 정보를 먼저 참조하는 항목이 프로젝트에 반영되어 있는지 확인하고, 없으면 생성해달라고 요청.
- **Agent Context**: 기존 AI SQL 흐름에는 `DatabaseSchemaService`가 `information_schema` 기반 스키마 프롬프트를 생성하여 `SqlAssistant`에 전달하는 런타임 반영이 존재함. 다만 정적 참조용 DDL/schema summary 파일은 없어 `backend/src/main/resources/ai/schema-summary.md`를 추가함.
- **Key Decisions**:
  - 정적 참조 파일은 `backend/src/main/resources/ai/`에 배치 — AI 관련 리소스인 `few-shot-examples.yml`과 같은 위치를 사용하여 기존 리소스 구조를 따름.
  - 실행 코드는 수정하지 않음 — 이미 `DatabaseSchemaService`, `SchemaPromptBuilder`, `SqlAssistant` 흐름이 스키마 선참조를 수행하므로 중복 로딩 로직을 추가하지 않음.
  - schema summary에는 허용 테이블, 관계, 날짜 기준, 비즈니스 의미 규칙을 포함 — `agent/project/backend.md`의 AI 도메인 및 MySQL 기반 SQL 생성 맥락에 맞춰 SQL 생성 오류를 줄이는 정보를 문서화함.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - `backend/src/main/resources/ai/schema-summary.md` — AI SQL 생성 전 참조 가능한 정적 스키마 요약 리소스
  - **Modified**:
  - **Deleted**:

  </details>

### 대시보드 생산 집계 기준 수정 (Codex)
- **User Intent**: 대시보드의 라인 생산량 합계와 다차원 분석 지표 생산 합계가 잘못 표시되고, 라인명이 라인명만 표기되어 공장 구분이 되지 않는 문제 수정 요청. 모든 공정 단계가 각각 생산량으로 합산되는 것이 아니라 작업 마감 기준 실제 생산 수량으로 집계되어야 한다는 기준을 제시함.
- **Agent Context**: 기존 `aggregateProductionByLine`은 모든 생산 실적 공정을 합산하여 한 작업이 여러 공정을 통과하면 생산량이 중복 집계될 수 있었음. 대시보드 생산 집계를 마감된 작업지시의 마지막 공정 양품 수량으로 제한하고, 표시 라벨을 `공장/라인` 형식으로 변경함.
- **Key Decisions**:
  - 생산량 집계는 `WorkOrder.Status.CLOSE` 작업지시와 라인별 마지막 `operationSeq` 실적만 대상으로 제한 — 작업 마감 후 최종 공정 수량을 생산량으로 보는 도메인 기준을 반영함.
  - `totalQty`는 `goodQty`만 사용 — 불량 수량은 생산 완료 수량에 더하지 않고 별도 지표로 유지하여 대시보드 카드 합계와 차트 합계의 기준을 일치시킴.
  - 라인 라벨은 `factoryName/lineName`으로 구성 — 같은 라인명이 여러 공장에 존재할 때 구분 가능하도록 대시보드 표시값을 백엔드 응답 단계에서 통일함.
  - repository 테스트를 추가 — 중간 공정 실적과 RUN 작업지시가 대시보드 생산 집계에 섞이지 않는지 회귀 검증함.
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Created**:
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/repository/ProductionExecutionRepositoryTest.java` — 대시보드 생산 집계 기준 회귀 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/dashboard/service/DashboardServiceImpl.java` (+3/-3) — 생산 지표 문구와 `totalQty` 산정 기준 수정
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/ProductionExecutionRepository.java` (+15/-5) — 마감 작업 마지막 공정 기준 집계 및 공장/라인 라벨 적용
  - **Deleted**:

  </details>

### 사이드바 통합 스크롤 적용 (Codex)
- **User Intent**: 메인 화면을 스크롤할 때 왼쪽 사이드바도 함께 내려가도록 하고, 사이드바 자체 스크롤 기능을 제거해달라는 요청.
- **Agent Context**: 기존 레이아웃은 `app-content`에 `overflow-y-auto`, `app-sidebar-nav`에 `max-height`와 `overflow-y-auto`가 적용되어 메인 콘텐츠와 사이드바가 별도 스크롤 컨테이너로 동작했음. 전역 레이아웃 CSS에서 내부 스크롤 설정을 제거하여 브라우저 페이지 스크롤 하나로 통합함.
- **Key Decisions**:
  - `DefaultLayout.vue` 구조는 유지 — 프론트엔드 계층 구조를 변경하지 않고 공통 레이아웃 CSS만 수정하여 영향 범위를 최소화함.
  - `app-content`의 `overflow-y-auto` 제거 — 메인 콘텐츠가 별도 스크롤 영역을 만들지 않고 문서 흐름에 따라 페이지 전체 높이를 확장하도록 함.
  - `app-sidebar-nav`의 `max-height`와 `overflow-y-auto` 제거 — 사이드바 내부 스크롤을 없애고 사이드바 전체가 페이지 스크롤과 함께 이동하도록 함.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
  - **Modified**:
    - `frontend/src/main.css` (+2/-2) — 메인 콘텐츠와 사이드바 nav의 내부 스크롤 제거
  - **Deleted**:

  </details>

### 대시보드 생산량 집계 수정 (Codex)
- **User Intent**: 오늘 생산량이 주문 수량과 맞지 않고 공정 단계별 실적이 단순 합산되어 11개/1개/1개 주문에서 생산량이 68개로 표시되는 문제 수정 요청
- **Agent Context**: 대시보드 생산량이 `production_execution` 공정 실적을 기준으로 집계되어 중간 공정 수량까지 생산량에 포함되는 것이 원인으로 진단. 마지막 공정 완료 시 생성되는 `PRODUCTION_RECEIPT` 재고 이력을 생산량 기준으로 변경.
- **Key Decisions**:
  - 대시보드 생산량 집계를 `InventoryTransactionHistoryRepository`로 이동 — 생산 완료 시점에 생성되는 생산 입고 이력이 실제 생산량 증가 이벤트이므로 Backend 문서의 Domain Repository 책임에 맞게 집계 소스를 조정
  - `PRODUCTION_RECEIPT_CANCEL` 원거래가 존재하는 입고 이력은 제외 — 삭제/취소된 마지막 공정 실적이 대시보드 생산량에 남지 않도록 실제 재고 이력의 취소 관계를 반영
  - 기존 `ProductionExecutionRepository`의 대시보드용 집계 메서드는 제거 — 공정별 실적 Repository가 생산량 대시보드 집계에 재사용되어 같은 버그가 재발하지 않도록 책임을 분리
- **Affected Files**: <details><summary>4개 파일</summary>

  - **Created**:
    - `backend/src/test/java/com/ssafy/demo_app/domain/inventory/repository/InventoryTransactionHistoryRepositoryTest.java` (+180/-0) — 생산 입고 이력 기반 대시보드 생산량 집계 회귀 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/dashboard/service/DashboardServiceImpl.java` (+5/-5) — 대시보드 생산량 집계 소스를 생산 입고 이력으로 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InventoryTransactionHistoryRepository.java` (+32/-0) — 생산 입고 이력 라인별/총량 집계 JPQL 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/ProductionExecutionRepository.java` (+0/-24) — 공정 실적 기반 대시보드 생산량 집계 메서드 제거
  - **Deleted**:
    - 없음

  </details>
