### 생산 도메인 고도화 구현 (Codex)
- **User Intent**: 생산 도메인의 작업지시 목록/상세, 자재 불출, 실적 삭제, 생산 입고, 불량 등록, 상태 전이, 공정 순서, 권한, 동시성 항목을 기능 기준으로 개선해달라는 요청
- **Agent Context**: 기존 생산 도메인은 작업지시/자재불출/공정실적 화면과 API가 분리되어 있었고, 자재 불출 이력이 `reasonDesc` 문자열로만 연결되어 재고 복원과 안정적 조회에 취약했다. 백엔드 모델/API/서비스를 먼저 확장하고 프론트 목록/상세/실적 화면을 변경했다.
- **Key Decisions**:
  - 작업지시 목록은 기존 `List` 응답 대신 프로젝트 공통 `PageResponse`를 사용 — `agent/project/backend.md`의 API 응답 래퍼와 기존 재고/기준정보 페이징 패턴 준수
  - 자재 불출 이력은 `InventoryTransactionHistory`에 `WorkOrder`와 `ProductionExecution` 연관관계를 추가 — 문자열 검색 기반 연결을 제거하고 작업지시별 수불 이력 조회 안정성 확보
  - 생산 실적 삭제는 연결 수불 이력을 역처리한 뒤 실적 참조를 해제 — 감사 이력은 보존하면서 FK 제약과 재고 불일치를 방지
  - 작업지시 상세는 별도 라우트 `production/work-orders/:id`로 분리 — 사용자가 요청한 페이지 분리 유지와 기존 기준정보 상세 페이지 연결 방식 준수
  - 생산 API 권한은 `SecurityConfig`에서 HTTP 메서드별로 제한 — 작업지시 관리성 액션은 관리자/매니저, 실적 등록과 불출은 인증 사용자 기준으로 분리
- **Affected Files**: <details><summary>23개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/ProductionIssueHistoryResponse.java` — 작업지시 상세의 연결 수불 이력 응답 DTO
    - `frontend/src/views/WorkOrderDetailView.vue` — 작업지시 별도 상세 페이지
    - `agent/history/sseoyeon-ssonia/fix-production.md` — 작업 히스토리 로그
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/production/WorkOrderApi.java` (+14/-4) — 작업지시 페이징 조회와 불출 취소 API 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/WorkOrderController.java` (+18/-5) — 페이징 조회와 불출 취소 컨트롤러 구현
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/ProductionExecutionCreateRequest.java` (+26/-0) — 불량 정보와 생산 입고 로케이션 요청 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/ProductionExecutionResponse.java` (+6/-0) — 불량 정보 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderDetailResponse.java` (+1/-0) — 연결 수불 이력 응답 포함
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/entity/InventoryTransactionHistory.java` (+10/-0) — 작업지시/생산실적 연관관계 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/entity/TransactionType.java` (+3/-0) — 생산 불출 취소와 생산 입고/입고 취소 유형 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/CurrentInventoryRepository.java` (+7/-0) — 재고 차감용 잠금 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InventoryTransactionHistoryRepository.java` (+5/-0) — 작업지시/생산실적 기준 이력 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/entity/ProductionExecution.java` (+9/-0) — 불량 유형/사유/재작업 가능 여부 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` (+36/-3) — 페이징 검색과 잠금 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/ProductionExecutionServiceImpl.java` (+199/-19) — 실적 등록 자동 불출/입고, 삭제 시 재고 복원, 공정 순서 검증 구현
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderService.java` (+7/-3) — 페이징 조회와 불출 취소 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` (+176/-65) — 작업지시 페이징, 명시적 불출 이력, 불출 취소/복원 구현
    - `backend/src/main/java/com/ssafy/demo_app/infrastructure/security/config/SecurityConfig.java` (+10/-0) — 생산 API 권한 세분화
    - `frontend/src/api/workOrderApi.ts` (+47/-3) — 페이징 타입, 불량/이력/불출 취소 API 계약 반영
    - `frontend/src/router/index.ts` (+5/-0) — 작업지시 상세 라우트 추가
    - `frontend/src/services/workOrderService.ts` (+10/-1) — 페이징 응답과 불출 취소 서비스 추가
    - `frontend/src/state/workOrderStore.ts` (+32/-1) — 작업지시 페이지 상태와 불출 취소 액션 추가
    - `frontend/src/views/ProductionExecutionView.vue` (+49/-2) — 불량 정보와 생산 입고 로케이션 입력 추가
    - `frontend/src/views/WorkOrderView.vue` (+31/-67) — 목록 10건 페이징, 서버 검색, 상세 페이지 링크 방식으로 변경
  - **Deleted**:
    - 없음

  </details>

### 생산 관리 안정성 보강 구현 (Codex)
- **User Intent**: 작업지시 검색/상세 버튼 조건/채번/재고 잠금/생산입고 기본 로케이션/취소 이력 추적/실적 화면 서버 검색/이력 라벨을 기존 생산 관리 구현에 합쳐 전체 코드로 구현해달라고 요청
- **Agent Context**: 기존 구현은 명시적 수불 연결과 상세 화면 분리는 되어 있었지만 품목 전용 검색, 서버 기준 액션 가능 여부, 날짜별 채번 race 방지, item+location 재고 잠금, 생산입고 기본 로케이션 정책, 원거래 추적, 실적 등록 화면 서버 검색이 부족했다.
- **Key Decisions**:
  - 작업지시 채번은 `WorkOrderSequence` 날짜별 sequence row와 서비스 메서드 동기화를 함께 사용 — 동시 생성 시 `orderNo` 중복 가능성을 줄이고 JPA 엔티티 중심 패턴을 유지
  - 생산 입고 기본 로케이션은 `WarehouseLocation.productionReceiptDefault` 플래그로 관리 — `findAll().first()` 같은 암묵적 기본값을 제거하고 운영자가 명시한 로케이션만 기본값으로 사용
  - 재고 입고/복원/취소는 `findByItemAndLocationForUpdate` 잠금 조회를 사용 — 같은 품목/로케이션 동시 갱신 시 lost update 위험을 줄임
  - 취소 수불 이력은 `originalTransaction` 자기 참조를 저장 — 실적 삭제 후 `productionExecution` 참조를 해제해도 원거래 추적이 가능하도록 감사성을 보강
  - 실적 등록 화면은 클라이언트 필터링 대신 작업지시 서버 검색/페이징을 호출 — 최초 10건 밖의 작업지시가 실적 등록 후보에서 누락되는 문제를 방지
- **Affected Files**: <details><summary>36개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/entity/WorkOrderSequence.java` — 계획일별 작업지시 채번 sequence 엔티티
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderSequenceRepository.java` — 채번 row 비관적 잠금 조회 repository
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/inventory/dto/LocationRequest.java` — 생산입고 기본 로케이션 요청 필드와 기존 테스트 호환 생성자 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/inventory/dto/LocationResponse.java` — 생산입고 기본 로케이션 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/WorkOrderApi.java` — 품목 전용 검색 파라미터 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/WorkOrderController.java` — 품목 전용 검색 파라미터 서비스 전달
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/ProductionExecutionResponse.java` — 실적별 삭제 가능 여부 응답 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/ProductionIssueHistoryResponse.java` — 원거래 수불 ID 응답 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderResponse.java` — 서버 기준 불출 취소/실적 삭제 가능 여부 응답 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/entity/InventoryTransactionHistory.java` — 원거래 수불 이력 자기 참조 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/entity/WarehouseLocation.java` — 생산입고 기본 로케이션 플래그 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/CurrentInventoryRepository.java` — item+location 비관적 잠금 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/WarehouseLocationRepository.java` — 생산입고 기본 로케이션 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/service/InventoryServiceImpl.java` — 생산입고 기본 로케이션 단일화 처리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/item/service/ItemServiceImpl.java` — 변경된 작업지시 응답 변환 시그니처 반영
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` — 품목 전용 검색 조건 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/ProductionExecutionServiceImpl.java` — 불량 사유 검증, 기본 입고 로케이션, 잠금 재고 갱신, 원거래 추적 취소 이력 반영
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderService.java` — 품목 전용 검색 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` — 기간 검증, 날짜별 채번, 서버 기준 can 필드, 잠금 복원, 원거래 추적 반영
    - `backend/src/main/java/com/ssafy/demo_app/infrastructure/security/config/SecurityConfig.java` — 불출/실적 등록을 WORKER 이상 권한으로 명시
    - `backend/src/main/resources/data.sql` — 초기 생산입고 기본 로케이션 지정
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` — 생산입고 기본 로케이션과 불량 사유 필수 정책 반영
    - `frontend/src/api/inboundApi.ts` — 로케이션 페이징 응답 처리와 기본 로케이션 필드 타입 반영
    - `frontend/src/api/workOrderApi.ts` — 품목 검색, 서버 can 필드, 원거래 ID, 실적 삭제 가능 여부 타입 반영
    - `frontend/src/views/ProductionExecutionView.vue` — 작업지시 선택 목록 서버 검색/페이징 전환과 기본 로케이션 표시
    - `frontend/src/views/WorkOrderDetailView.vue` — 서버 can 필드 기반 버튼 조건과 사용자 친화적 수불 유형/수량 라벨 적용
    - `frontend/src/views/WorkOrderView.vue` — 품목 전용 검색 입력 추가
  - **Deleted**:
    - 없음

  </details>

### 공정 실적 검색 오류 수정 (Codex)
- **User Intent**: 공정 실적 화면에서 자동완성으로 올바른 작업 지시를 선택한 뒤 조회하면 조건에 맞는 작업 지시가 없다고 표시되는 오류 수정을 요청했다.
- **Agent Context**: 공정 실적 화면이 하나의 검색어를 `keyword`와 `itemKeyword`에 동시에 전달했고, 백엔드 검색 쿼리는 두 조건을 AND로 묶어 작업지시번호가 품목 코드/명에도 동시에 매칭되어야 하는 상태였다.
- **Key Decisions**:
  - 공정 실적 화면에서는 통합 검색어를 `keyword`로만 전달하도록 수정했다 — 프론트엔드 4계층 흐름에서 View의 검색 파라미터 구성 오류를 최소 범위로 보정했다.
  - 백엔드 일반 `keyword` 검색 범위를 작업지시번호, 품목, 공장, 라인, 공정명으로 확장했다 — 화면 placeholder의 “작업지시번호, 품목, 공정 검색” 의미와 API 검색 동작을 일치시켰다.
  - 작업지시번호 keyword 검색 회귀 테스트를 추가했다 — 동일 증상이 재발하면 서비스 통합 테스트에서 탐지되도록 했다.
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ProductionExecutionView.vue` (+102/-19) — 공정 실적 검색에서 `itemKeyword` 중복 전송 제거
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` (+50/-4) — 일반 keyword 검색 범위 확장
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` (+38/-8) — 작업지시번호 검색 회귀 테스트 추가
  - **Deleted**:
    - 없음

  </details>

### 생산 입고 로케이션 보강 (Codex)
- **User Intent**: 공정 실적 등록 시 생산 입고 로케이션을 기본으로 두면 로케이션을 찾을 수 없고, 렉을 직접 지정하면 서버 오류가 발생하는 문제의 원인 확인 및 보강을 요청했다.
- **Agent Context**: 기본 선택은 `receiptLocationCode`가 비어 서버 기본 생산 입고 로케이션 조회로 이어지는데, 운영 DB에 `productionReceiptDefault=true` 로케이션이 없으면 `LOCATION_NOT_FOUND`가 발생했다. 지정 렉은 동일 품목/로케이션에 lot별 재고가 여러 개 있을 때 단건 재고 조회가 비고유 결과가 되어 500 오류로 번질 수 있었다.
- **Key Decisions**:
  - 기본 생산 입고 로케이션이 없으면 첫 로케이션으로 fallback하도록 했다 — 기본 선택 UI가 빈 값을 전송해도 등록 흐름이 끊기지 않게 하는 최소 보강이다.
  - 생산 입고 재고는 lot 없는 재고 행만 잠금 조회하도록 분리했다 — lot별 기존 재고가 있는 지정 렉에서도 생산 입고용 집계 재고를 안정적으로 생성/갱신하기 위한 조치다.
  - 기본 로케이션 미설정과 lot 재고 혼재 케이스를 테스트로 추가했다 — 두 증상이 다시 발생하면 생산 서비스 통합 테스트에서 탐지되도록 했다.
- **Affected Files**: <details><summary>4개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/WarehouseLocationRepository.java` (+4/-0) — 기본 생산 입고 로케이션 fallback 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/CurrentInventoryRepository.java` (+19/-0) — lot 없는 품목/로케이션 재고 잠금 조회 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/ProductionExecutionServiceImpl.java` (+222/-19) — 생산 입고 로케이션 fallback 및 lot 없는 재고 갱신 사용
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` (+108/-8) — 기본 로케이션 미설정 및 lot 재고 혼재 회귀 테스트 추가
  - **Deleted**:
    - 없음

  </details>

### 생산 수불 타입 컬럼 보강 (Codex)
- **User Intent**: 공정 실적 등록 시 Docker 로그와 개발자도구 Network에서 500 오류가 발생하는 원인을 확인하고 수정해달라고 요청했다.
- **Agent Context**: Docker 로그의 `Data truncated for column transaction_type` 경고를 근거로, 새 수불 타입 `PRODUCTION_RECEIPT`가 기존 MySQL `transaction_type` 컬럼 정의에 들어가지 못해 서버 오류로 포장되는 문제로 진단했다.
- **Key Decisions**:
  - `InventoryTransactionHistory.transactionType` 컬럼을 `varchar(50)`로 명시했다 — enum 문자열이 늘어나도 MySQL 컬럼 길이/네이티브 enum 제약에 막히지 않게 하기 위한 조치다.
  - 관련 생산 서비스 통합 테스트를 재실행했다 — 엔티티 컬럼 정의 변경이 테스트 스키마 생성과 생산 실적 흐름을 깨지 않는지 확인했다.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/entity/InventoryTransactionHistory.java` (+15/-1) — `transaction_type` 컬럼을 `varchar(50)`로 고정
  - **Deleted**:
    - 없음

  </details>

### 공정 실적 화면 세로 레이아웃 정리 (Codex)
- **User Intent**: 공정 실적 화면의 실적 등록/실적 투입 조회 탭에서 좌우 배치된 작업 지시 선택과 상세 영역을 세로 흐름으로 바꾸고, 조회/선택 영역을 위에 둔 뒤 상세 정보와 등록/조회 패널이 아래에 나오도록 요청했다.
- **Agent Context**: 기존 화면은 `pe-layout`이 XL 이상에서 좌측 작업 지시 선택, 우측 상세 영역의 2열 grid로 동작했다. 프론트만 수정하라는 요청에 맞춰 API/백엔드 로직은 건드리지 않고 템플릿 래퍼와 CSS 전용 레이아웃만 조정했다.
- **Key Decisions**:
  - `pe-layout`을 단일 컬럼 grid로 변경했다 — 두 탭 모두 작업 지시 선택 패널이 항상 상단에 오도록 하기 위한 최소 변경이다.
  - 작업 지시 목록을 상단 영역에 맞는 카드 그리드로 변경했다 — 세로형 페이지에서 긴 단일 리스트가 화면을 과도하게 차지하지 않도록 하기 위한 조치다.
  - 실적 등록 패널 제목을 실제 생산실적 등록으로 변경했다 — 사용자가 요청한 하단 정보/등록 영역의 의미를 화면 라벨에 맞춘 것이다.
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ProductionExecutionView.vue` (+107/-24) — 작업 지시 선택 패널을 상단 섹션으로 두고 하단 상세/등록/조회 영역과 분리
    - `frontend/src/main.css` (+3/-3) — 공정 실적 레이아웃 단일 컬럼 및 작업 지시 카드 그리드 적용
  - **Deleted**:
    - 없음

  </details>

### 공정 실적 작업지시 목록형 전환 (Codex)
- **User Intent**: 공정 실적의 실적 등록 탭과 실적/투입 조회 탭에서 작업 지시 목록이 카드뉴스식으로 표시되는 것을 기준정보 관리 목록처럼 테이블 목록 형태로 바꾸고, 10개 단위 페이지네이션을 유지해달라고 요청했다.
- **Agent Context**: 검색 및 필터 영역은 유지하고, `filteredOrders` 렌더링 영역만 카드 버튼 목록에서 `wo-table` 기반 행 목록으로 교체했다. 기존 API 조회는 이미 `size: 10`을 사용하므로 프론트 표시와 페이지 이동 UI를 명확히 보강했다.
- **Key Decisions**:
  - 작업 지시 결과를 `wo-table` 기반 테이블로 전환했다 — 기준정보 관리 탭들의 목록형 스캔 경험과 맞추기 위한 변경이다.
  - 행 클릭으로 작업 지시를 선택하도록 유지했다 — 기존 카드 선택 동작을 보존하면서 표시 방식만 바꾸기 위한 조치다.
  - 하단 페이지네이션에 총 건수, 표시 범위, 10건씩, 처음/이전/다음/마지막 버튼을 추가했다 — 10개 단위 페이지 이동 상태가 화면에서 명확히 보이도록 하기 위한 변경이다.
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ProductionExecutionView.vue` (+181/-51) — 작업 지시 카드 목록을 테이블 목록과 10개 단위 페이지네이션 UI로 교체
    - `frontend/src/main.css` (+10/-32) — 카드 목록 스타일 제거 및 작업 지시 테이블/페이지네이션 스타일 추가
  - **Deleted**:
    - 없음

  </details>
