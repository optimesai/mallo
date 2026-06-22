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
