### AI clarification 연결성 개선 (Codex)
- **User Intent**: 같은 AI 채팅방에서 질문 B가 추가 조건을 요구한 뒤 사용자가 짧은 조건만 입력하면, 해당 조건이 질문 B가 아니라 과거 질문 A에 잘못 붙는 문제 개선 요청
- **Agent Context**: 기존 프론트는 pending ID를 보관했지만 서버가 최신 pending clarification을 authoritative하게 판별하지 않았고, 후속 답변 처리 후 원본 pending 상태도 계속 남아 재연결 위험이 있었다. 백엔드에서 최신 pending 조회, 조건형 후속 입력 결합, 명확한 새 질문 입력 시 pending 취소, 후속 처리 완료 시 pending 완료 상태 전환을 구현했다.
- **Key Decisions**:
  - 백엔드가 같은 conversation의 최신 `CLARIFICATION_REQUIRED` 이력을 우선 조회하도록 구성 — agent/project/backend.md의 API → Domain 계층 흐름을 유지하면서 연결성 판정을 서비스 계층에 집중
  - 짧은 조건형 입력은 pending 원본 질문에 결합하고 명확한 새 질문만 기존 pending을 `CANCELLED` 처리 — 사용자가 제시한 핵심 목표인 “조건만 입력한 경우 반드시 pending 질문에 연결”을 우선
  - 후속 답변 처리 후 원본 pending을 `CLARIFICATION_ANSWERED`로 전환 — DB에 남은 과거 clarification이 다음 메시지의 최신 pending으로 다시 잡히지 않도록 방지
  - 프론트는 서버 응답의 `pendingClarificationQueryId`를 기준으로 상태 갱신 — agent/project/frontend.md의 Store 계층 책임에 맞춰 화면 내 transient AI 상태만 보관
  - 프롬프트는 보조 안전장치로만 보강 — 구현 로직을 우선하고 LLM에는 resolved question 처리 규칙을 명시
- **Affected Files**: <details><summary>11개 파일</summary>

  - **Created**:
    - `agent/history/sseoyeon-ssonia/improve-ai.md` — 작업 히스토리 로그
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiQueryResponse.java` (+1/-0) — 서버 기준 pending clarification ID 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/entity/AiQueryHistory.java` (+2/-0) — clarification 완료/취소 상태 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/repository/AiQueryHistoryRepository.java` (+6/-0) — conversation 최신 pending 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+82/-14) — 최신 pending 판별, 후속 조건 결합, 새 질문 취소, parent 완료 처리 구현
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/ClarificationAssistant.java` (+4/-0) — pending context clarification 규칙 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/IntentClassifier.java` (+4/-0) — pending context 분류 규칙 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/SqlAssistant.java` (+7/-0) — resolved question SQL 생성 규칙 추가
    - `frontend/src/api/aiApi.ts` (+3/-0) — AI 응답 타입과 실행 상태 타입 보강
    - `frontend/src/state/aiStore.ts` (+2/-1) — 서버 기준 pending ID로 채팅 상태 갱신
  - **Deleted**:
    - 없음

  </details>

### 리스트 정렬 동작 재점검 및 서버 정렬 연결 보정 (Codex)
- **User Intent**: 추가한 정렬 기능이 제대로 동작하지 않고, 특히 BOM 등 버전 컬럼이 있는 목록에서 버전 정렬이 안 되는 문제 재점검 및 수정 요청
- **Agent Context**: BOM, 입고, 재고, 출하처럼 서버 페이지네이션을 사용하는 화면에서 현재 페이지 배열만 프론트 정렬하고 있어 전체 목록 기준 정렬이 되지 않았고, 일부 컬럼은 화면 표시 필드와 정렬 키가 불일치했음
- **Key Decisions**:
  - 서버 페이지네이션 목록은 기존 API `sort` 파라미터에 정렬 컬럼/방향을 연결하고, 헤더 클릭 시 1페이지부터 재조회하도록 보정
  - BOM 목록의 `bomVersion` 정렬도 `sort=bomVersion,asc|desc`로 조회되도록 수정
  - 수불 이력의 `변동 사유`, `작업자` 정렬 키를 실제 응답 필드인 `reasonDesc`, `workerName`으로 수정
  - 입고/창고 적재 처리 후 목록 재조회도 화면의 정렬 상태를 유지하는 `fetchPageData()` 경로로 통일
- **Affected Files**: <details><summary>9개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/BomMasterView.vue` — BOM 목록 정렬을 서버 `sort` 파라미터와 연결, 버전 정렬 보정
    - `frontend/src/views/InboundReceiptView.vue` — 입고 예정 목록 정렬을 서버 `sort` 파라미터와 연결, 작업 후 정렬 상태 유지
    - `frontend/src/views/InboundStackView.vue` — 창고 적재 대기 목록 정렬을 서버 `sort` 파라미터와 연결, 작업 후 정렬 상태 유지
    - `frontend/src/views/InventoryStatusView.vue` — 현재고 목록 정렬을 서버 `sort` 파라미터와 연결
    - `frontend/src/views/InventoryHistoryView.vue` — 수불 이력 목록 정렬을 서버 `sort` 파라미터와 연결, 잘못된 정렬 키 보정
    - `frontend/src/views/ShippingOrderView.vue` — 출하 지시 목록 정렬을 서버 `sort` 파라미터와 연결
    - `frontend/src/views/PickingView.vue` — 피킹/상차 작업 목록 정렬을 서버 `sort` 파라미터와 연결
    - `frontend/src/views/WorkOrderView.vue` — 초기 작업지시 목록 조회에도 현재 정렬 상태 반영
    - `agent/history/sseoyeon-ssonia/improve-ai.md` — 작업 히스토리 append-only 기록
  - **Deleted**:
    - 없음

  </details>

### 주요 리스트 컬럼 정렬 기능 보강 (Codex)
- **User Intent**: BOM 목록, 입고 예정 목록, 창고 적재 대기 목록, 생산 작업지시 목록, 작업지시 목록, 현재고 목록, 수불 이력, 공정 실적 작업지시 목록, 출하 지시 목록, 피킹/상차 작업 목록에 품목 리스트처럼 컬럼 헤더 클릭 정렬 기능 추가 요청
- **Agent Context**: 백엔드 변경 없이 화면별 기존 조회/필터 흐름을 유지하면서, 프론트 표시 배열 정렬 또는 기존 API sort 파라미터 연결 방식으로 각 목록에 정렬 상태와 정렬 마크를 추가
- **Key Decisions**:
  - `app-sortable-header`, `app-sort-mark` 공통 클래스를 모든 대상 목록 헤더에 적용
  - 서버 sort 파라미터가 이미 있는 작업지시/공정실적 목록은 헤더 클릭 시 정렬 조건을 바꿔 재조회
  - 그 외 목록은 현재 화면에 표시되는 필터 결과 배열을 프론트에서 정렬하여 백엔드 계약을 변경하지 않음
- **Affected Files**: <details><summary>11개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/BomMasterView.vue` — BOM 목록 컬럼 헤더 정렬 추가
    - `frontend/src/views/InboundReceiptView.vue` — 입고 예정 목록 컬럼 헤더 정렬 추가
    - `frontend/src/views/InboundStackView.vue` — 창고 적재 대기 목록 컬럼 헤더 정렬 추가
    - `frontend/src/views/MaterialIssueView.vue` — 생산 작업지시 목록 컬럼 헤더 정렬 및 페이지네이션 표시 기준 정렬 배열 연결
    - `frontend/src/views/WorkOrderView.vue` — 작업지시 목록 API sort 파라미터를 컬럼 헤더 클릭과 연결
    - `frontend/src/views/InventoryStatusView.vue` — 현재고 목록 컬럼 헤더 정렬 추가
    - `frontend/src/views/InventoryHistoryView.vue` — 수불 이력 목록 컬럼 헤더 정렬 추가
    - `frontend/src/views/ProductionExecutionView.vue` — 공정 실적 작업지시 목록 API sort 파라미터를 컬럼 헤더 클릭과 연결
    - `frontend/src/views/ShippingOrderView.vue` — 출하 지시 목록 컬럼 헤더 정렬 추가
    - `frontend/src/views/PickingView.vue` — 피킹/상차 작업 목록 컬럼 헤더 정렬 추가
    - `agent/history/sseoyeon-ssonia/improve-ai.md` — 작업 히스토리 append-only 기록
  - **Deleted**:
    - 없음

  </details>

### 사용자 권한 화면 배경 정합성 보정 (Codex)
- **User Intent**: 사용자 권한 페이지 전체가 회색 배경으로 칠해진 것처럼 보여 다른 페이지와 같은 레이아웃으로 수정 요청
- **Agent Context**: 사용자 권한 화면 루트가 `app-bg-muted p-6`을 직접 사용하고 있어 다른 마스터 화면의 `app-page` 컨테이너와 배경 계층이 달랐고, 목록 패널도 자체 `rounded-3xl` 구조를 사용하고 있었음
- **Key Decisions**:
  - 화면 루트를 `app-page`로 변경 — 다른 페이지와 동일한 페이지 여백/배경 계층을 사용
  - 사용자 목록 패널을 `app-panel`과 `app-list-head` 구조로 변경 — 앞선 리스트 제목 공통화 기준과 같은 레이아웃 적용
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/UserManagementView.vue` — 루트 배경 제거 및 사용자 목록 패널을 공통 레이아웃으로 변경
    - `agent/history/sseoyeon-ssonia/improve-ai.md` — 작업 히스토리 append-only 기록
  - **Deleted**:
    - 없음

  </details>

### 리스트 제목 공통화 보강 (Codex)
- **User Intent**: 리스트가 존재하는 페이지에서 목록 타이틀이 있는 경우와 없는 경우가 섞여 있으므로, 리스트가 있으면 무조건 타이틀을 표시하도록 수정 요청
- **Agent Context**: 기존에 `수불(변동) 타임라인 이력`, `실시간 현재고 목록`처럼 제목이 있는 목록은 유지하고, 단순 액션 툴바 또는 탭 내부 테이블만 있는 화면에 공통 `app-list-*` 제목 영역을 추가
- **Key Decisions**:
  - 전역 `app-list-head`, `app-list-title`, `app-list-meta` 클래스를 기준으로 목록 제목/건수 표기를 통일
  - 이미 패널 제목이 목록명을 명확히 담고 있는 화면은 중복 제목을 만들지 않고 유지
  - 상세 탭 내부 테이블도 실제 리스트 역할을 하는 경우 제목을 추가하여 목록 영역의 의미를 명확히 함
- **Affected Files**: <details><summary>11개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/main.css` — 목록 제목 공통 클래스 `app-list-head`, `app-list-title`, `app-list-meta` 추가
    - `frontend/src/views/ItemMasterView.vue` — 품목 목록 제목/건수 추가
    - `frontend/src/views/PartnerMasterView.vue` — 거래처 목록 제목/건수 추가
    - `frontend/src/views/InboundReceiptView.vue` — 입고 예정 목록 제목/건수 추가
    - `frontend/src/views/InboundStackView.vue` — 창고 적재 대기 목록 제목/건수 추가
    - `frontend/src/views/FactoryLineMasterView.vue` — 공장 및 생산라인 목록 제목 추가
    - `frontend/src/views/ProductionExecutionView.vue` — 작업 지시 목록 제목/건수 추가
    - `frontend/src/views/PartnerMasterDetailView.vue` — 거래 품목 이력 제목/건수 추가
    - `frontend/src/views/BomMasterDetailView.vue`, `frontend/src/views/BomMasterView.vue`, `frontend/src/views/MaterialIssueView.vue` — 구성 품목/BOM 소요 자재 목록 제목/건수 추가
  - **Deleted**:
    - 없음

  </details>

### AI SQL 해석력 범용 보강 (Codex)
- **User Intent**: BOM 소요량 질의를 특정 도메인 직접 실행으로 고정하지 않고, 프롬프팅, few-shot 예시, SQL 검토 AI 계층을 통해 BOM뿐 아니라 재고, 거래처, 입고, 출고, 생산 등 전체 데이터 질의의 해석력을 높이도록 요청.
- **Agent Context**: BOM 질의 실패 원인은 질의 해석 자체보다 해석 슬롯이 SQL 생성 단계에서 충분히 강제되지 않고, 생성 SQL을 실행 전 의미 기준으로 재검토하는 계층이 없다는 점으로 진단. BOM 전용 직접 실행 경로는 배제하고, 해석 결과를 classification context에 포함한 뒤 SQL 생성 직후 검토 AI가 누락 조건을 판정하고 재생성 지시를 주는 흐름으로 전환했다.
- **Key Decisions**:
  - SQL 검토 AI를 생성 직후, 정적 SQL 검증 직전에 배치 — agent/project/backend.md의 Domain 서비스 계층 흐름을 유지하면서 LLM 생성물의 의미 누락을 실행 전에 보정.
  - 검토 실패 시 같은 SQL 생성 assistant에 검토 사유와 재생성 지시를 retry reason으로 전달 — 특정 도메인 하드코딩 대신 프롬프트 계약을 강화하는 방식으로 범용성을 유지.
  - classification context에 규칙 기반 해석 결과를 함께 직렬화 — SQL assistant와 reviewer가 동일한 도메인, 의도, 지표, 수량, 버전 정책 정보를 기준으로 판단하도록 구성.
  - few-shot 예시는 BOM에만 고정하지 않고 재고와 입고 예시를 함께 추가 — 도메인별 필터링 패턴을 일반화하고 품목/거래처 free-form 키워드를 코드, 이름, ID 후보로 해석하도록 유도.
- **Affected Files**: <details><summary>13개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/SqlReviewAssistant.java` — 생성 SQL 의미 검토 AI 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/sql/SqlReviewService.java` — SQL 검토 응답 파싱 및 fallback 처리 서비스
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/sql/SqlReviewServiceTest.java` — SQL 검토 결과 파싱과 fallback 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` — 해석 결과 포함 context 생성, SQL 검토 및 재생성 흐름 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/SqlAssistant.java` — 해석 슬롯 우선 적용, 품목/거래처 키워드 매칭, BOM 최신 활성 버전 규칙 보강
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/rule/BusinessRulePromptService.java` — BOM 소요량, 최신 BOM, 품목/거래처 후보 매칭 비즈니스 규칙 추가
    - `backend/src/main/resources/ai/few-shot-examples.yml` — BOM 최신 활성 버전, 품목 키워드 재고, 거래처 키워드 입고 예시 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` — SQL 검토 실패 시 재생성 및 재검토하는 서비스 흐름 테스트 추가
  - **Deleted**:
    - 없음

  </details>

### 프론트 타입 스케일 중앙화 (Codex)
- **User Intent**: 프론트 글씨 크기를 변경할 때 각 화면의 Tailwind `text-*` 클래스를 직접 찾아 고치지 않도록, 직접 사용 위치를 정리하고 `main.css`에서 한 번에 관리하도록 리팩토링 요청
- **Agent Context**: 글자 크기 변경 목적에 맞춰 레이아웃 유틸리티는 유지하고, 글자 크기 Tailwind 유틸리티만 전역 타입 스케일로 분리했다. `frontend/src` 기준 직접 `text-xs`, `text-sm`, `text-[11px]` 등 글자 크기 유틸리티 검색 결과가 없도록 치환했다.
- **Key Decisions**:
  - 글자 크기만 `app-type-*`로 중앙화 — agent/project/frontend.md의 Tailwind CSS 사용 방식을 유지하면서 변경 범위를 typography로 제한
  - 실제 크기 값은 `:root`의 `--app-font-size-*`, `--app-line-height-*` 변수에 배치 — 이후 크기 조정 시 `main.css` 한 곳에서 관리 가능
  - 화면 마크업은 `app-type-*` 클래스로 치환하고 scoped CSS는 CSS 변수로 치환 — Tailwind `@apply`에서 커스텀 클래스 적용 실패 가능성을 피함
  - 레이아웃 유틸리티는 유지 — 글자 크기 변경 요청을 넘어서는 화면 구조 리팩토링을 피하고 영향 범위를 통제
- **Affected Files**: <details><summary>27개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/main.css` (+295/-74) — 전역 타입 스케일 변수와 `app-type-*` 유틸리티 추가, 공통 컴포넌트 글자 크기 변수화
    - `frontend/src/layouts/components/AppHeader.vue` (+2/-2) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/ui/AiAnswerSummary.vue` (+2/-2) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/ui/AiChartPanel.vue` (+17/-17) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/ui/AiResultTable.vue` (+1/-1) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/ui/AiSqlPanel.vue` (+1/-1) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/ui/DataTable.vue` (+2/-2) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/AiDataChatbotView.vue` (+6/-6) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/BomMasterDetailView.vue` (+40/-40) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/BomMasterView.vue` (+5/-5) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/FactoryLineMasterDetailView.vue` (+38/-38) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/FactoryLineMasterView.vue` (+88/-30) — 템플릿 글자 크기를 `app-type-*`로 치환하고 scoped CSS 글자 크기를 변수화
    - `frontend/src/views/HomeView.vue` (+9/-9) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/InboundReceiptView.vue` (+26/-26) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/InboundStackView.vue` (+29/-29) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/InventoryHistoryView.vue` (+16/-16) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/InventoryStatusView.vue` (+22/-22) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/ItemMasterDetailView.vue` (+62/-62) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/ItemMasterView.vue` (+52/-52) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/MaterialIssueView.vue` (+21/-21) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/PartnerMasterDetailView.vue` (+62/-62) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/PartnerMasterView.vue` (+51/-51) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/PickingView.vue` (+31/-31) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/ShippingOrderView.vue` (+31/-31) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
    - `frontend/src/views/UserManagementView.vue` (+50/-50) — 직접 글자 크기 유틸리티를 `app-type-*`로 치환
  - **Deleted**:
    - 없음

  </details>

### AI 질의 해석 계층 구현 (Codex)
- **User Intent**: 개선 구현안 1, 6, 7을 실제로 추가하되, 질의 해석 계층을 AI 처리 흐름의 맨 앞에 두고 BOM뿐 아니라 재고, 거래처, 입고, 출고, 생산 등 주요 도메인의 질의 해석까지 반영해 구현 시작 요청.
- **Agent Context**: 기존 `AiQueryServiceImpl`이 LLM 의도 분류와 SQL 생성에 바로 진입하는 구조라, 규칙 기반 `interpretation` 계층을 앞단에 추가하고 해석 결과로 LLM 분류 결과를 보정하도록 구현. 실패/보완 UX를 위해 응답 DTO와 프론트 요약 카드에 해석 요약 및 추천 후속 질문을 추가.
- **Key Decisions**:
  - 해석 계층은 `domain/ai/service/interpretation`에 배치 — agent/project/backend.md의 AI 도메인 경계를 유지하고 API 계층 역방향 의존 없이 Domain 내부 서비스로 구성.
  - 해석 결과는 DB 스키마 확장 없이 응답 메타데이터로 제공 — 기존 `AiQueryHistory` 저장 계약을 건드리지 않아 마이그레이션 리스크를 줄임.
  - LLM 결과를 대체하지 않고 보정하는 방식 채택 — 기존 SQL 생성, SQL 검증, 차트 추천 흐름을 유지하면서 도메인/의도/지표/차트 힌트가 비어 있거나 `unknown`일 때만 규칙 기반 결과로 보완.
  - 프론트 추천 후속 질문은 `AiAnswerSummary` emit → View → Store 기존 제출 흐름으로 연결 — agent/project/frontend.md의 View → Store → Service → API 계층 흐름을 유지.
- **Affected Files**: <details><summary>10개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/interpretation/AiQuestionInterpretation.java` — 해석 결과 모델
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/interpretation/AiQuestionInterpretationService.java` — BOM, 재고, 거래처, 입고, 출고, 생산 등 도메인 규칙 기반 해석 서비스
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/interpretation/AiQuestionInterpretationServiceTest.java` — 도메인별 대표 질의 해석 회귀 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiQueryResponse.java` (+5/-0) — 해석 도메인, 의도, 요약, 응답 타입, 추천 질문 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+103/-14) — 질의 해석 계층 호출, LLM 분류 보정, 응답 메타데이터 매핑 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` (+35/-0) — 해석 계층이 비데이터 LLM 분류를 보정하는 서비스 연결 테스트 추가
    - `frontend/src/api/aiApi.ts` (+12/-0) — AI 응답 타입에 해석/추천 질문 필드 추가
    - `frontend/src/ui/AiAnswerSummary.vue` (+46/-1) — 질의 해석 요약, 해석 도메인, 추천 후속 질문 버튼 표시
    - `frontend/src/views/AiDataChatbotView.vue` (+9/-1) — 추천 후속 질문 클릭 시 기존 질문 제출 흐름으로 연결
  - **Deleted**:
    - 없음

  </details>

### BOM 소요량 직접 실행 구현 (Codex)
- **User Intent**: BOM 소요량 질의가 `testparent`를 `item_code`로만 매칭하는 SQL을 생성해 0건으로 끝나는 문제를 해결하기 위해, 해석된 품목/수량/최신 BOM 조건을 직접 실행 경로에 연결하도록 수정 요청.
- **Agent Context**: 실패 원인은 질의 해석 실패가 아니라 해석된 슬롯이 SQL 생성 단계에 강제 반영되지 않는 구조로 진단. BOM 소요량 질의는 LLM SQL 생성을 건너뛰고 품목 후보 해소, 활성 최신 BOM 버전 결정, 기존 `BomService.calculateMaterialRequirements()` 호출로 처리하도록 직접 실행 서비스를 추가.
- **Key Decisions**:
  - BOM 소요량은 AI 도메인 직접 실행 서비스로 분기 — LLM 생성 SQL의 `item_code` 단독 매칭, 비활성 최신 버전 선택, 다단계 BOM 누락 리스크를 제거하기 위함.
  - 품목 해소는 `item_code`, `item_name`, `item_id` 정확 매칭을 우선하고 부분 매칭 후보는 clarification으로 반환 — 사용자가 입력한 `testparent`가 코드가 아니라 이름이어도 계산 가능하게 하기 위함.
  - 최신 BOM 버전은 활성 BOM 버전만 대상으로 숫자 토큰 기반 비교 — `v10.0`이 `v2.0`보다 오래된 것으로 처리되는 문자열 정렬 문제를 방지.
  - 소요량 계산은 기존 `BomService.calculateMaterialRequirements()` 재사용 — backend.md의 도메인 서비스 경계와 기존 BOM 재귀/순환 검증 정책을 유지.
- **Affected Files**: <details><summary>6개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/execution/DirectQueryResult.java` — 직접 실행 결과 모델
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/execution/BomRequirementDirectQueryService.java` — BOM 소요량 직접 실행 서비스
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/execution/BomRequirementDirectQueryServiceTest.java` — 품목명 매칭과 활성 최신 버전 선택 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` — 해석 직후 BOM 직접 실행 경로 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/repository/BomStructureRepository.java` — 상위 품목 기준 활성 BOM 버전 조회 메서드 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` — 직접 실행 시 LLM/SQL 경로를 건너뛰는 테스트 추가
  - **Deleted**:
    - 없음

  </details>
### 프론트 리스트 UI 통일 (Codex)
- **User Intent**: 사용자 및 권한 목록을 10개 단위로 페이지네이션하고, 사용자 및 권한/품목 마스터/거래처 마스터/공장 및 생산 라인/BOM/입고/재고/생산/출고 화면의 페이지네이션, 카드뉴스, 검색 탭, 리스트 정렬, 폰트 계층을 프론트에서만 통일 요청
- **Agent Context**: 백엔드 API 계약은 변경하지 않고, 기존 Vue 3 Composition API 화면 구조와 Tailwind 기반 `app-*` 전역 토큰을 확장하여 공통 UI 클래스를 만들고 주요 목록 화면이 해당 클래스를 사용하도록 정리
- **Key Decisions**:
  - `frontend/src/main.css`의 전역 `app-*` 컴포넌트 클래스를 확장 — `agent/project/frontend.md`의 Tailwind CSS 4 및 실제 코드 우선 규칙에 맞춰 화면별 하드코딩보다 기존 전역 스타일 체계를 재사용
  - 사용자 및 권한 페이지네이션은 클라이언트 computed slice로 구현 — 백엔드를 건드리지 말라는 요청을 준수하고 기존 `userStore.loadUsers()` 전체 목록 조회 흐름을 유지
  - 대표 디자인 기준은 거래처 마스터/품목 마스터로 유지 — 페이지네이션·카드뉴스는 거래처 마스터 형태, 검색·테이블·정렬은 품목 마스터 형태를 공통 클래스에 반영
- **Affected Files**: <details><summary>15개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/main.css` (+109/-19) — 검색 패널, 카드뉴스, 정렬 헤더, 페이지네이션 공통 클래스 추가 및 페이지 버튼 스타일 통일
    - `frontend/src/ui/DataTable.vue` (+5/-9) — 공통 테이블 페이지네이션 버튼을 처음/이전/다음/마지막 텍스트 양식으로 변경
    - `frontend/src/ui/StatsCard.vue` (+6/-2) — 통계 카드를 공통 카드뉴스 클래스 기반으로 변경
    - `frontend/src/views/UserManagementView.vue` (+73/-25) — 사용자 목록 10건 단위 클라이언트 페이지네이션 추가 및 카드/검색/표/페이지네이션 스타일 통일
    - `frontend/src/views/ItemMasterView.vue` (+52/-42) — 품목 마스터 카드/검색/정렬 테이블/페이지네이션을 공통 클래스 기반으로 정리
    - `frontend/src/views/PartnerMasterView.vue` (+71/-71) — 거래처 마스터 카드/검색/정렬 테이블/페이지네이션을 공통 클래스 기반으로 정리
    - `frontend/src/views/BomMasterView.vue` (+7/-7) — BOM 목록 페이지네이션 버튼 양식 통일
    - `frontend/src/views/FactoryLineMasterView.vue` (+7/-4) — 공장 및 생산 라인 페이지네이션 버튼 양식 통일 및 마지막 버튼 추가
    - `frontend/src/views/WorkOrderView.vue` (+22/-4) — 작업 지시 목록 페이지네이션 버튼 양식 통일 및 처음/마지막 버튼 추가
    - `frontend/src/views/InboundReceiptView.vue` (+4/-4) — 입고 등록 페이지네이션 기호 버튼을 텍스트 버튼으로 변경
    - `frontend/src/views/InboundStackView.vue` (+4/-4) — 창고 적재 페이지네이션 기호 버튼을 텍스트 버튼으로 변경
    - `frontend/src/views/InventoryStatusView.vue` (+5/-5) — 현재고 현황 페이지네이션 기호 버튼을 텍스트 버튼으로 변경
    - `frontend/src/views/InventoryHistoryView.vue` (+5/-5) — 수불 이력 조회 페이지네이션 기호 버튼을 텍스트 버튼으로 변경
    - `frontend/src/views/ShippingOrderView.vue` (+5/-5) — 출하 지시 페이지네이션 기호 버튼을 텍스트 버튼으로 변경
    - `frontend/src/views/PickingView.vue` (+5/-5) — 피킹 상차 페이지네이션 기호 버튼을 텍스트 버튼으로 변경
  - **Deleted**:
    - 없음

  </details>

### 프론트 공통 UI 클래스 적용 확대 (Codex)
- **User Intent**: 이전 변경이 각 화면별 클래스를 유지한 채 값만 맞춘 것처럼 보여 통일감이 부족하므로, 모든 페이지가 `main.css`의 공통 `app-*` 클래스를 실제로 가져다 쓰는 방향으로 계속 작업 요청
- **Agent Context**: BOM, 공장/생산라인, 작업지시, 공정 실적 화면에 남아 있던 `bom-*`, `factory-master-*`, `wo-*` 기반 검색/표/버튼/입력 클래스를 공통 `app-*` 클래스로 치환하고, 모든 뷰 테이블이 `app-table`을 사용하도록 정리
- **Key Decisions**:
  - 화면별 CSS 값을 맞추는 방식 대신 템플릿 마크업에서 `app-panel`, `app-search-panel`, `app-control`, `app-button`, `app-table`, `app-status`를 직접 사용 — 같은 디자인은 같은 공통 클래스를 참조해야 한다는 사용자 의도 반영
  - 업무 고유 구조 클래스는 일부 유지 — 진행률, 트리, 모달 레이아웃처럼 화면 고유 동작/배치에 필요한 클래스까지 제거하면 범위 초과 리팩토링이 되므로 공통 UI 요소 위주로 치환
  - scoped CSS 안의 `app-*` 선택자 오염을 제거 — 전역 공통 클래스는 `frontend/src/main.css`에서만 디자인 값을 갖도록 유지
- **Affected Files**: <details><summary>12개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/main.css` — 기존 `app-stat-*` 카드도 카드뉴스 공통 디자인과 같은 크기/폰트/간격을 사용하도록 조정
    - `frontend/src/views/BomMasterView.vue` — 카드뉴스, 조회 조건, 목록/상세 테이블, 정전개/역전개 입력·버튼을 공통 `app-*` 클래스 기반으로 변경
    - `frontend/src/views/FactoryLineMasterView.vue` — 통계 카드, 조회 조건, 목록 테이블, 모달 입력·버튼을 공통 `app-*` 클래스 기반으로 변경
    - `frontend/src/views/WorkOrderView.vue` — 등록/목록 탭의 패널, 버튼, 입력, 테이블, 상태 배지를 공통 `app-*` 클래스 기반으로 변경
    - `frontend/src/views/ProductionExecutionView.vue` — 작업 지시 선택/실적 조회의 검색, 입력, 표, 페이지네이션을 공통 `app-*` 클래스 기반으로 변경
    - `frontend/src/views/WorkOrderDetailView.vue` — 상세 테이블과 버튼/패널 계열을 공통 `app-*` 클래스 기반으로 변경
    - `frontend/src/views/BomMasterDetailView.vue` — 상세 테이블을 공통 `app-table` 기반으로 변경
    - `frontend/src/views/PartnerMasterDetailView.vue` — 상세 테이블을 공통 `app-table` 기반으로 변경
    - `frontend/src/views/InboundReceiptView.vue` — 입고 등록 테이블을 공통 `app-table` 기반으로 변경
    - `frontend/src/views/InboundStackView.vue` — 창고 적재 테이블을 공통 `app-table` 기반으로 변경
    - `frontend/src/views/InventoryStatusView.vue`, `frontend/src/views/InventoryHistoryView.vue`, `frontend/src/views/MaterialIssueView.vue`, `frontend/src/views/ShippingOrderView.vue`, `frontend/src/views/PickingView.vue` — 재고/생산/출고 목록 테이블을 공통 `app-table` 기반으로 변경
  - **Deleted**:
    - 없음

  </details>

### 카드뉴스와 리스트 시각 정합성 보정 (Codex)
- **User Intent**: 카드뉴스의 디자인, 폰트 크기, 내부 요소 크기, 아이콘 사용 여부와 크기가 화면마다 다르고, 공장 및 생산라인 구조도 겹침, 동일 컴포넌트 폰트 차이, 상태 태그 줄바꿈, 작업 지시 등록 줄 겹침 문제를 첨부 이미지 기준으로 수정 요청
- **Agent Context**: 첨부 이미지에서 거래처 카드뉴스를 기준으로 삼고, BOM/품목/사용자/공장 카드뉴스 마크업과 전역 카드 스타일을 보정. 상태 태그와 테이블 기본 텍스트 계층은 `main.css` 공통 클래스에서 해결하고, 공장 구조도 및 작업지시 등록은 해당 화면 CSS에서 레이아웃 안정성을 보정
- **Key Decisions**:
  - 상태 태그 줄바꿈은 `app-status` 전역 클래스에서 `whitespace-nowrap`와 최소 폭으로 처리 — 각 화면별 태그를 개별 수정하지 않고 같은 컴포넌트 역할의 공통 클래스로 해결
  - 카드뉴스는 `app-news-card` 내부 구조를 텍스트 왼쪽, 아이콘 오른쪽으로 통일 — 거래처 마스터 기준 디자인을 모든 대표 카드뉴스에 적용
  - 공장 구조도는 업무 고유 트리 클래스는 유지하되 grid auto-column, truncate, min-height로 겹침 방지 — 공통 카드/표 클래스와 달리 구조도는 고유 레이아웃이 필요하기 때문
  - 작업지시 등록 줄 겹침은 `wo-form-panel`이 공통 `app-panel-head`를 사용할 때의 여백과 입력 높이를 보정 — 공통 클래스 적용 후 깨진 보조 레이아웃을 정합성 있게 재조정
- **Affected Files**: <details><summary>6개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/main.css` — 카드뉴스 최소 높이, 상태 태그 nowrap/min-width, 테이블 내부 텍스트 기본 계층, 작업지시 폼 간격/입력 높이 보정
    - `frontend/src/views/BomMasterView.vue` — BOM 카드뉴스를 텍스트 왼쪽/아이콘 오른쪽 구조로 통일
    - `frontend/src/views/FactoryLineMasterView.vue` — 공장 카드뉴스 구조 통일 및 구조도 카드/공정 버튼 겹침 방지 CSS 적용
    - `frontend/src/views/ItemMasterView.vue` — 품목 카드뉴스에 공통 아이콘 박스 추가
    - `frontend/src/views/UserManagementView.vue` — 사용자 권한 카드뉴스에 공통 아이콘 박스 추가
    - `frontend/src/views/PartnerMasterView.vue` — 거래처 구분 셀 줄바꿈 방지
  - **Deleted**:
    - 없음

  </details>

### 대시보드 빈 라우트 보정 (Codex)
- **User Intent**: 대시보드 화면에서 사이드바와 헤더만 보이고 본문 내용이 사라진 문제 확인 및 복구 요청
- **Agent Context**: 라우터에서 실제 대시보드는 `/` 자식 라우트에만 연결되어 있고 `/dashboard`, `/home` 또는 잘못된 하위 경로는 `DefaultLayout`만 렌더링한 채 자식 `RouterView`가 비는 구조로 확인. 대시보드 연결이 끊긴 것이 아니라 라우트 별칭/리다이렉트 부재로 판단
- **Key Decisions**:
  - `/dashboard`와 `/home`을 `home` 라우트로 redirect — 사용자가 대시보드 의미로 접근하는 경로가 빈 화면이 되지 않도록 처리
  - 레이아웃 내부 catch-all을 `home`으로 redirect — 인증 레이아웃은 유지되지만 본문이 비는 라우터 상태를 방지
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/router/index.ts` — 대시보드 별칭 경로와 레이아웃 내부 fallback redirect 추가
  - **Deleted**:
    - 없음

  </details>

### 자재 출고 페이징 및 구조도 보정 (Codex)
- **User Intent**: 공정 실적 및 원부자재 카드뉴스를 현재고 현황/입고 등록과 같은 형태로 맞추고, 공장/라인/공정 구조 타이틀이 레이아웃에 잡아먹히는 문제와 BOM 기반 자재 출고 목록의 페이지네이션 부재 및 리스트 디자인 불일치를 수정 요청
- **Agent Context**: 공정 실적 카드는 기존 `wo-metric` 요약 패널을 사용해 카드뉴스 기준과 달랐고, 공장 구조도는 존재하지 않는 `app-panel-heading` 클래스 때문에 헤더 스타일이 적용되지 않았으며, 자재 출고 화면은 필터 결과 전체를 렌더링해 페이지네이션이 없었다.
- **Key Decisions**:
  - 공정 실적 요약을 `app-news-card` 기반으로 교체 — 카드뉴스 공통 디자인을 실제 마크업에서 사용하도록 통일
  - 공장 구조도 헤더를 `app-panel-head`로 수정하고 구조도 패널을 flex column/min-height로 보강 — 타이틀 영역이 스크롤 콘텐츠에 밀리지 않도록 처리
  - 자재 출고 목록은 클라이언트 페이지네이션 적용 — 백엔드를 건드리지 않고 기존 `workOrderStore.loadWorkOrders()` 결과를 10건 단위로 표시
  - 자재 출고 테이블은 `app-table`, `app-status`, `app-pagination`을 사용 — 다른 탭의 리스팅과 동일한 표/상태/페이징 양식으로 맞춤
- **Affected Files**: <details><summary>4개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/ProductionExecutionView.vue` — 공정 실적 및 원부자재 요약 카드를 `app-news-card` 기반 카드뉴스로 변경
    - `frontend/src/views/FactoryLineMasterView.vue` — 구조도 헤더 클래스 오류 수정 및 구조도 패널 높이/스크롤 레이아웃 보강
    - `frontend/src/views/MaterialIssueView.vue` — 작업 지시 목록 10건 단위 페이지네이션 추가, 테이블/상태/버튼 스타일을 공통 `app-*` 클래스 기반으로 정리
    - `agent/history/sseoyeon-ssonia/improve-ai.md` — 작업 히스토리 append-only 기록
  - **Deleted**:
    - 없음

  </details>

### 정렬 서버 오류 수정 (Codex)
- **User Intent**: BOM master, 입고 등록 및 검수 관리, 창고 적재 및 로케이션 배치, 재고 수불 이력 추적, 작업 지시 관리 화면에서 정렬 클릭 시 서버 500 오류가 발생하여 해결 요청
- **Agent Context**: 화면 DTO 필드 정렬 키가 서버 `Pageable.sort`로 전달되며 JPA 엔티티에 없는 필드 또는 집계 필드 정렬로 해석되는 것이 원인으로 진단. 서버 요청 정렬은 안전한 고정값으로 제한하고 화면 정렬은 클라이언트 computed 정렬로 처리.
- **Key Decisions**:
  - 프론트엔드 View 계층에서 정렬 요청 파라미터를 제어 — `agent/project/frontend.md`의 View → Store → Service → API 흐름을 유지하면서 사용자 인터랙션 범위에서 최소 변경
  - DTO 전용 정렬 키를 서버로 보내지 않도록 고정 정렬값 사용 — JPA 엔티티 경로와 응답 DTO 필드 간 불일치로 인한 500 오류 차단
  - 작업 지시 목록에 클라이언트 정렬 computed 추가 — 기존 다른 화면의 computed 정렬 패턴과 일관성 유지
  - 누락된 `AlertTriangle` import 추가 — Lucide Vue 아이콘 사용 컨벤션 유지
- **Affected Files**: <details><summary>7개 파일</summary>

  - **Created**:
    - `agent/implementation-plan-sort-500.md` — 정렬 서버 오류 수정 구현 계획서
  - **Modified**:
    - `frontend/src/views/BomMasterView.vue` (+5/-2) — BOM 그룹 조회 시 DTO 정렬 키 서버 전달 제거
    - `frontend/src/views/InboundReceiptView.vue` (+6/-2) — 입고 목록 서버 정렬 고정값 적용
    - `frontend/src/views/InboundStackView.vue` (+7/-3) — 적재 대기 입고 목록 서버 정렬 고정값 적용
    - `frontend/src/views/InventoryHistoryView.vue` (+8/-3) — 수불 이력 서버 정렬 고정값 및 `AlertTriangle` import 적용
    - `frontend/src/views/WorkOrderView.vue` (+21/-3) — 작업 지시 서버 정렬 고정값 및 클라이언트 정렬 computed 추가
  - **Deleted**:
    - 없음

  </details>

### 전체 목록 정렬 개선 (Codex)
- **User Intent**: 정렬이 현재 페이지 내부에서만 적용되어 전체 목록 기준으로 재정렬되지 않는 문제와, BOM 버전 및 수불 유형 배지 값이 화면 표시 기준으로 제대로 정렬되지 않는 문제 해결 요청
- **Agent Context**: 이전 서버 500 회피 과정에서 프론트가 정렬 파라미터를 고정값으로 보내고 있었고, 일부 화면은 현재 페이지 배열만 computed 정렬하고 있었음. DTO 표시 필드와 JPA 엔티티 필드가 불일치하는 항목은 백엔드 서비스에서 안전한 정렬 매핑 또는 DTO 전체 정렬 후 페이지 분할로 처리.
- **Key Decisions**:
  - 프론트엔드는 클릭한 정렬 키와 방향을 다시 서버에 전달 — `agent/project/frontend.md`의 View → Store → Service → API 흐름 유지
  - BOM 그룹과 작업지시는 DTO 계산/집계 필드가 있어 서비스에서 전체 DTO 목록을 정렬한 뒤 페이지를 구성 — JPA 엔티티 경로에 없는 필드 정렬로 인한 500 오류 방지
  - 입고 목록은 DTO 정렬 키를 엔티티 경로로 매핑한 `Pageable`로 변환 — 서버 페이지네이션과 전체 목록 정렬 동시 유지
  - 수불 유형 정렬은 화면 배지 라벨인 `생산불출`, `입고적재` 기준으로 처리 — enum 문자열 순서와 사용자 표시 순서의 불일치 제거
- **Affected Files**: <details><summary>10개 파일</summary>

  - **Created**:
    - `agent/implementation-plan-global-sort.md` — 전체 목록 정렬 개선 구현 계획서
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/bom/service/BomServiceImpl.java` — BOM 그룹 DTO 전체 정렬 및 자연 버전 정렬 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/service/InboundServiceImpl.java` — 입고 DTO 정렬 키를 엔티티 경로로 매핑
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/service/InventoryServiceImpl.java` — 입고/수불 이력 정렬 매핑 및 수불 유형 라벨 순서 정렬 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` — 작업지시 DTO 전체 정렬 및 계산 필드 정렬 추가
    - `frontend/src/views/BomMasterView.vue` — BOM 목록 정렬 파라미터 서버 전달 복구
    - `frontend/src/views/InboundReceiptView.vue` — 입고 목록 정렬 파라미터 서버 전달 복구
    - `frontend/src/views/InboundStackView.vue` — 적재 목록 정렬 파라미터 서버 전달 복구
    - `frontend/src/views/InventoryHistoryView.vue` — 수불 이력 정렬 파라미터 서버 전달 및 배지 라벨 비교 보정
    - `frontend/src/views/WorkOrderView.vue` — 작업지시 목록 서버 정렬 사용 복구
  - **Deleted**:
    - 없음

  </details>

### 수불 이력 표시 수량 보정 (Codex)
- **User Intent**: 수불 이력의 변동 수량 정렬에서 음수 값이 제대로 계산되지 않고, 출고성 이력이 모두 생산불출로 표시되는 문제 수정 요청
- **Agent Context**: 출고/피킹/생산불출 일부 이력은 DB 저장 수량이 양수지만 재고 영향은 음수이고, 프론트가 `INBOUND` 외 모든 유형을 `생산불출`로 표시하고 있었음. 표시 수량과 서버 정렬 수량을 같은 재고 증감 기준으로 보정.
- **Key Decisions**:
  - `PRODUCTION_ISSUE`, `OUTBOUND`, `RESERVATION`은 표시 수량을 `-abs(quantity)`로 계산 — 저장 방식과 재고 영향 방향의 불일치 보정
  - 수불 유형 라벨을 enum별 매핑 함수로 분리 — 생산 이슈는 `생산불출`, 정상 출고/피킹성 이력은 `출고적재`로 표시
  - 서버 `quantity` 정렬도 표시 수량 계산식으로 정렬 — 페이지네이션 전체 목록 정렬과 화면 표시 순서 일치
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/inventory/service/InventoryServiceImpl.java` — 수불 이력 수량 정렬을 표시 수량 기준으로 보정
    - `frontend/src/views/InventoryHistoryView.vue` — 수불 유형 라벨, 배지 색상, 표시 수량 및 현재 페이지 보조 정렬 보정
    - `agent/history/sseoyeon-ssonia/improve-ai.md` — 작업 히스토리 append-only 기록
  - **Deleted**:
    - 없음

  </details>
