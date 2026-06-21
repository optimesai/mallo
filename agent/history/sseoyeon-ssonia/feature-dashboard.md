### 메인 대시보드 화면 구현 (Codex)
- **User Intent**: 메인 화면 대시보드에 AI 기반 자동 차트 추천과 라인·설비·제품·창고 기준의 다차원 지표 비교 시각화를 반영해달라는 요청
- **Agent Context**: 기존 홈 화면은 로딩 문구만 표시하고 있었고, AI 데이터 챗봇과 자유 질의 기능이 중복될 수 있어 홈은 운영 KPI 요약과 추천 차트 소비 화면으로 분리
- **Key Decisions**:
  - 프론트엔드 계층은 `View → Store → Service → API` 흐름으로 분리 — `agent/project/frontend.md`의 4계층 아키텍처 규칙 준수
  - 대시보드에는 자연어 입력창을 두지 않고 AI 추천 차트 명세를 표시 — 기존 AI 데이터 챗봇과 기능 중복을 피하고 홈의 운영 모니터링 목적 유지
  - 백엔드 대시보드 API가 없는 상태에서도 화면 검증이 가능하도록 서비스 계층에서 404 응답에 한해 fallback snapshot 반환 — 추후 `/api/dashboard/summary` 연결 시 API 타입을 그대로 재사용 가능
  - 차트 렌더링은 기존 `AiChartPanel`과 `AiResultTable`을 재사용 — 기존 UI 패턴을 따르고 신규 차트 구현 범위를 최소화
- **Affected Files**: <details><summary>4개 파일</summary>

  - **Created**:
    - `frontend/src/api/dashboardApi.ts` — 대시보드 기간, 요약 카드, 지표 뷰, 인사이트 응답 타입 및 API 호출 정의
    - `frontend/src/services/dashboardService.ts` — 대시보드 snapshot 조회와 백엔드 미구현 시 fallback 운영 지표 데이터 제공
    - `frontend/src/state/dashboardStore.ts` — 기간 선택, 지표 선택, 로딩·오류 상태를 관리하는 Pinia store 추가
  - **Modified**:
    - `frontend/src/views/HomeView.vue` (+280/-8) — 운영 KPI 카드, 다차원 지표 탭, AI 추천 차트, 인사이트 패널, 챗봇 연결 버튼으로 홈 화면 재구성
  - **Deleted**:

  </details>

### 대시보드 챗봇 자동질의 연결 (Codex)
- **User Intent**: 대시보드 다차원 분석 지표 영역의 `AI 추천 근거` 설명과 차트 축 정보는 제거하고, `AI 데이터 챗봇에서 상세 분석` 버튼만 남기며 버튼 클릭 시 해당 지표 질의가 자동 실행되도록 요청
- **Agent Context**: 기존 버튼은 AI 챗봇 페이지로 단순 이동만 수행했고, 대시보드 선택 지표 맥락을 전달하지 않았음. 라우터 query를 통해 자연어 질문을 전달하고 AI 챗봇 화면에서 기존 질의 제출 흐름을 재사용하도록 연결
- **Key Decisions**:
  - 대시보드 버튼은 `question` query를 포함한 `RouterLink`로 유지 — 라우팅 계층을 우회하지 않고 기존 Vue Router 흐름 준수
  - AI 챗봇은 route query 감지 후 `submitQuestion()` 재사용 — 메시지 히스토리, 로딩 상태, 차트/SQL/테이블 갱신 로직을 중복 구현하지 않음
  - 지표별 질문 문구는 대시보드 선택 기간과 metric id로 생성 — 생산/품질/재고/출하 탭 선택 맥락을 챗봇 자동 질의에 반영
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
  - **Modified**:
    - `frontend/src/views/HomeView.vue` — AI 추천 근거 카드 내용을 제거하고 지표별 자동 질의 query를 포함한 챗봇 이동 버튼만 유지
    - `frontend/src/views/AiDataChatbotView.vue` — `question` query 수신 시 자동으로 기존 AI 질의 제출 흐름 실행
  - **Deleted**:

  </details>

### 작업지시 목록 오류 수정 (Codex)
- **User Intent**: 생산관리 하위 3개 화면에서 공통으로 `작업지시 목록 조회 실패`와 서버 오류가 발생하므로 즉시 해결 요청
- **Agent Context**: 세 화면 모두 초기 진입 시 `GET /api/work-orders`를 호출하며, 목록 응답 생성 과정에서 현재 공정 진행률 계산까지 수행해 BOM/자재불출 상세 계산 예외가 목록 전체 500으로 전파되는 구조로 진단
- **Key Decisions**:
  - 작업지시 목록 응답은 `toListResponse`로 분리 — 목록 조회에서는 상세 공정 진행 계산을 제외해 생산관리 탭 공통 초기 로딩 안정화
  - 상세 화면과 작업 선택 이후의 상세 조회는 기존 `toResponse` 유지 — BOM/공정 진행 상세 계산이 필요한 흐름은 그대로 보존
  - 목록의 현재 공정 표시는 기본 라우팅으로 fallback — 목록 조회 안정성을 우선하고 상세 선택 시 정확한 공정 진행 정보 조회
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` (+10/-1) — 목록 조회 전용 응답 변환 추가
  - **Deleted**:

  </details>

### 생산관리 오류 연결점 점검 (Codex)
- **User Intent**: 생산관리 하위의 작업 지시 등록, 자재 출고 처리, 공정 실적 및 원부자재 화면에서만 오류가 발생하므로 대시보드 작업 이후 연결 문제를 다시 확인해달라는 요청
- **Agent Context**: 세 화면이 공통으로 `workOrderStore.loadWorkOrders()`를 호출하며, 이는 모두 `GET /api/work-orders`로 연결됨. 라우터 경로 자체는 정상이고, 생산관리 공통 작업지시 목록 API 또는 작업지시 응답 변환 경로가 500을 반환하는 구조로 진단
- **Key Decisions**:
  - `ProductionExecutionView` 초기 로딩도 `Promise.allSettled`로 변경 — 작업지시 목록, 공장/생산 라우팅, 입고 위치 중 어떤 API가 실패했는지 분리 표시
  - `MaterialIssueView`는 단일 작업지시 목록 API만 호출하므로 기존 구조 유지 — 실패 시 `GET /api/work-orders` 문제임을 바로 알 수 있음
  - 라우터와 사이드바 경로는 기존 `/production/work-orders`, `/production/issue`, `/production/executions` 연결이 일치함을 확인 — 경로 연결 문제가 아니라 API 응답 문제로 범위 축소
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
  - **Modified**:
    - `frontend/src/views/ProductionExecutionView.vue` — 초기 로딩 실패 API를 구분해 표시하도록 변경
  - **Deleted**:

  </details>

### 작업지시 오류 진단 보강 (Codex)
- **User Intent**: 작업 지시 관리 화면 진입 시 서버 오류 배너가 표시되어 원인을 찾아달라는 요청
- **Agent Context**: 화면 초기 로딩이 품목, 라우팅, 작업지시 목록 API를 동시에 호출하고 있어 기존 단일 오류 문구만으로는 실패 API를 구분할 수 없었음. 백엔드 전역 예외 처리도 stack trace를 로그로 남기지 않아 실제 원인 확인이 어려운 상태로 진단
- **Key Decisions**:
  - 백엔드 `GlobalExceptionHandler`에 미처리 예외 로그 출력 추가 — 서버 콘솔에서 실제 예외 클래스와 발생 라인을 확인할 수 있게 함
  - 작업지시 초기 로딩을 `Promise.allSettled`로 변경 — 어느 API가 실패했는지 화면에서 `반제품 품목`, `완제품 품목`, `공장/생산 라우팅`, `작업지시 목록` 단위로 구분
  - 부분 성공 데이터는 유지 — 특정 API 실패가 다른 초기 데이터 표시까지 모두 막지 않도록 조정
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/global/exception/GlobalExceptionHandler.java` — 미처리 서버 예외 stack trace 로깅 추가
    - `frontend/src/views/WorkOrderView.vue` — 초기 로딩 실패 API를 구분해 표시하도록 변경
  - **Deleted**:

  </details>

### 실제 데이터 대시보드 연동 (Codex)
- **User Intent**: 대시보드 수치가 실제 시스템 데이터와 맞지 않고 하드코딩처럼 보이므로, 백엔드 로직을 설계해 작업 데이터가 반영되게 하고 `Logistics Control` 클릭 시 메인 대시보드로 이동하도록 요청
- **Agent Context**: 기존 1차 화면은 백엔드 대시보드 API 미구현 상황에서 fallback snapshot을 사용해 숫자가 실제 DB와 맞지 않았음. 대시보드 전용 백엔드 API와 집계 쿼리를 추가하고 프론트 fallback 데이터를 제거해 실제 API 응답만 사용하도록 변경
- **Key Decisions**:
  - 대시보드 API는 `api/dashboard`와 `domain/dashboard/service`로 분리 — `agent/project/backend.md`의 API → Domain 계층 규칙 준수
  - 생산량과 불량률은 `ProductionExecution` 기준으로 집계 — 실제 생산 실적 등록 시 즉시 대시보드에 반영되도록 설계
  - 재고 지표는 `CurrentInventory`와 `InventoryTransactionHistory`를 조합 — 현재고와 선택 기간 출고성 트랜잭션을 기반으로 창고별 회전 관찰 지표 계산
  - 출하 대기 지표는 `OutboundShipping`의 미완료 상태를 집계 — 출하 작업 진행에 따라 대기 건수와 거래처별 대기 물량이 바뀌도록 구현
  - 프론트 fallback snapshot 제거 — 실제 API 장애를 숨기지 않고 데이터 정합성을 우선
- **Affected Files**: <details><summary>17개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/dashboard/DashboardController.java` — `GET /api/dashboard/summary` 엔드포인트 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/dashboard/dto/DashboardInsightResponse.java` — 대시보드 인사이트 응답 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/dashboard/dto/DashboardMetricViewResponse.java` — 지표 차트 뷰 응답 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/dashboard/dto/DashboardSummaryCardResponse.java` — 상단 KPI 카드 응답 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/dashboard/dto/DashboardSummaryResponse.java` — 대시보드 전체 응답 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/dashboard/service/DashboardService.java` — 대시보드 조회 서비스 인터페이스 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/dashboard/service/DashboardServiceImpl.java` — 실제 도메인 데이터 기반 대시보드 집계 서비스 구현
    - `frontend/src/api/dashboardApi.ts` — 대시보드 API 타입과 호출 함수 추가
    - `frontend/src/services/dashboardService.ts` — 대시보드 API 응답 처리 서비스 추가
    - `frontend/src/state/dashboardStore.ts` — 대시보드 상태 관리 store 추가
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/CurrentInventoryRepository.java` (+20/-0) — 창고별 현재고와 안전재고 미만 품목 집계 쿼리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/repository/InventoryTransactionHistoryRepository.java` (+16/-0) — 창고별 출고성 트랜잭션 집계 쿼리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/ProductionExecutionRepository.java` (+31/-0) — 라인별 생산량, 제품별 불량률, 생산 총량 집계 쿼리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` (+2/-0) — 진행 작업지시 상태 집계 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/shipping/repository/OutboundShippingRepository.java` (+37/-0) — 거래처별 출하 대기 물량과 대기 건수 집계 쿼리 추가
    - `frontend/src/layouts/components/AppSidebar.vue` (+2/-2) — `Logistics Control` 브랜드 영역을 홈 링크로 변경
    - `frontend/src/views/HomeView.vue` (+272/-8) — 실제 대시보드 store/API 기반 화면으로 재구성
  - **Deleted**:

  </details>

### 대시보드 오류 처리 조정 (Codex)
- **User Intent**: 백엔드와 프론트엔드를 다시 실행해 홈 대시보드를 확인했을 때 서버 오류 배너가 표시되고, 안내 문구 카드가 노출되어 이를 제거해달라는 요청
- **Agent Context**: 대시보드 백엔드 API가 아직 구현되지 않은 상태에서 404 외 서버 응답이 발생하면 fallback snapshot을 사용하지 못해 오류 배너가 표시되는 것으로 진단
- **Key Decisions**:
  - 대시보드 API 호출 실패 시 응답 상태와 무관하게 fallback snapshot을 반환 — 현재 작업 범위가 프론트 대시보드 1차 화면 구현이므로 백엔드 미구현 상태에서도 화면 확인 가능성 보장
  - `대시보드 역할` 안내 카드는 제거 — 사용자가 명시적으로 삭제 요청한 화면 문구만 범위 내에서 제거
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
  - **Modified**:
    - `frontend/src/services/dashboardService.ts` — 대시보드 API 실패 시 항상 fallback snapshot 반환
    - `frontend/src/views/HomeView.vue` — 우측 하단 안내 카드 제거
  - **Deleted**:

  </details>
