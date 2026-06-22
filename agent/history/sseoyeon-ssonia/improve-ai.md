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
    - `agent/plans/ai-clarification-connectivity.md` — AI clarification 연결성 개선 구현 기획서
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
    - `agent/plans/frontend-typography-tailwind-refactor.md` — 직접 글자 크기 Tailwind 사용 위치와 리팩토링 정책 정리
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
