### AI 질의 정확도 개선 (Codex)
- **User Intent**: AI 챗봇이 자연어 질의 분류, NL2SQL 생성, SQL 실행 결과 요약, 차트 추천, 운영 이슈 요약 흐름을 유지하면서 답변 정확도를 높이도록 요청
- **Agent Context**: 기존 아키텍처를 QueryPlan 중심으로 바꾸지 않고, 현재 단계별 흐름에 업무 규칙 프롬프트, 구조화된 분류 결과, SQL 의미 검증, 1회 재생성, 요약 프롬프트 보강을 덧붙이는 방식으로 구현
- **Key Decisions**:
  - 기존 `AiQueryServiceImpl` 오케스트레이션 유지 — 프로젝트 백엔드 계층 규칙의 Service 중심 도메인 로직 흐름을 유지하면서 품질 보강 범위를 AI 도메인 내부로 제한
  - 업무 규칙을 별도 서비스로 분리 — 스키마/예시만으로 추론하던 현재고, 안전재고, 불량률, 출하 대기 기준을 재사용 가능한 프롬프트 입력으로 고정
  - SQL 의미 검증을 보안 검증 뒤에 추가 — 기존 `SqlValidationService`의 SELECT/허용 테이블 검증을 유지하고, 질문 의도와 SQL 핵심 컬럼 불일치만 별도 방어막으로 처리
  - few-shot은 패턴 중심으로 보강 — 질문별 예시를 늘리는 대신 출하 대기, 입고 추이, 작업지시 상태, 창고별 현재고, 수불 이력, 운영 이슈 패턴을 추가하여 오버피팅 위험을 낮춤
- **Affected Files**: <details><summary>18개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/classification/AiIntentResult.java` (+26/-0) — 구조화된 자연어 질의 분류 결과 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/classification/IntentClassificationService.java` (+66/-0) — JSON 분류 결과 파싱 및 기존 YES/NO 응답 호환 래퍼
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/rule/BusinessRulePromptService.java` (+37/-0) — AI SQL 생성/요약에 주입할 업무 규칙 프롬프트
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/sql/SqlSemanticValidationService.java` (+164/-0) — 질문 의도와 생성 SQL의 의미 일치 검증
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/rule/BusinessRulePromptServiceTest.java` (+22/-0) — 핵심 업무 규칙 포함 검증
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/sql/SqlSemanticValidationServiceTest.java` (+68/-0) — 의미 검증 성공/실패 케이스 검증
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/entity/AiQueryHistory.java` (+1/-0) — 의미 검증 실패 상태 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+195/-26) — 업무 규칙, 구조화 분류, 의미 검증, SQL 1회 재생성, 강화된 답변 생성 입력 연결
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/AnswerGenerator.java` (+9/-0) — SQL/업무 규칙/분류 결과 기반 답변 프롬프트 보강
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/ChartRecommendationGenerator.java` (+2/-0) — 결과 컬럼 기반 차트 추천 제약 강화
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/IntentClassifier.java` (+24/-5) — YES/NO 분류를 도메인/의도/명확화 JSON 분류로 확장
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/SqlAssistant.java` (+19/-1) — 업무 규칙, 분류 결과, 재시도 사유를 반영한 NL2SQL 프롬프트 강화
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/prompt/DataQuestionCandidateService.java` (+6/-1) — 운영 이슈/위험/대기 키워드 후보 감지 보강
    - `backend/src/main/resources/ai/few-shot-examples.yml` (+95/-0) — SQL 패턴 중심 few-shot 예시 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` (+48/-7) — 새 의존성 반영 및 의미 검증 재생성 흐름 테스트
    - `frontend/src/api/aiApi.ts` (+11/-1) — 백엔드 실행 상태 enum과 프론트 타입 동기화
    - `frontend/src/ui/AiAnswerSummary.vue` (+1/-1) — 세분화된 실패 상태 표시 분기 수정
  - **Deleted**:
    - 없음

  </details>

### 의미 검증 실패 안내 개선 (Codex)
- **User Intent**: `질문 의도와 일치하는 SQL을 생성하지 못했습니다.`라는 안내가 무책임하게 느껴지므로, 사용자가 일자/범위/집계 기준 등을 구체화해 다시 질문할 수 있도록 안내 문구 개선 요청
- **Agent Context**: 의미 검증 실패 사유는 히스토리 `errorLog`에 남기되, 사용자에게는 실패 선언이 아니라 다음 액션을 제시하는 답변을 내려야 하는 문제로 진단. `SEMANTIC_VALIDATION_FAILED` 사용자-facing 답변 상수를 구체화 요청 문구로 교체.
- **Key Decisions**:
  - 상태 enum과 실패 추적은 유지 — 기존 `SEMANTIC_VALIDATION_FAILED` 상태와 내부 실패 사유 저장 흐름은 그대로 두어 운영 진단 가능성 보존
  - 사용자 답변만 행동 가능 문구로 변경 — 조회 기간, 집계 기준, 상태값, 대상 품목/거래처를 예시로 제시해 재질문 방향을 명확히 안내
  - 서비스 테스트에 실패 안내 문구를 고정 — 재생성 후에도 의미 검증 실패 시 구체화 안내가 반환되는지 회귀 검증
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+1/-1) — 의미 검증 실패 사용자 안내 문구 개선
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` (+30/-0) — 의미 검증 최종 실패 시 구체화 안내 반환 테스트 추가
  - **Deleted**:
    - 없음

  </details>

### 입고 추이 의미 검증 회귀 수정 (Codex)
- **User Intent**: `최근 7일 입고 수량 추이를 알려줘`처럼 이전에 정상 답변하던 질의가 최근 AI 정확도 개선 이후 실패하여 원인 확인 및 복구 요청
- **Agent Context**: 새로 추가한 `SqlSemanticValidationService`가 추이 질의에 `DATE(`, `YEAR(`, `MONTH(`, `DATE_FORMAT(` 함수형 표현만 허용해, 정상적인 `inbound_receipt.inbound_date` 기준 집계 SQL을 오탐 차단하는 것으로 진단. 또한 분류 모델이 입고 질의를 넓은 `inventory` 도메인으로 분류할 경우 `inbound_receipt`가 도메인 검증에서 제외될 수 있어 함께 완화.
- **Key Decisions**:
  - 함수형 날짜 표현만 강제하지 않고 도메인 날짜 컬럼을 허용 — `inbound_date`, `created_at`, `plan_date` 등 실제 스키마의 날짜 기준 집계를 정상 경로로 인정
  - `inventory` 도메인 검증에 `inbound_receipt`를 포함 — 자연어 분류가 `inbound` 대신 `inventory`로 넓게 잡히는 경우에도 입고 집계 SQL을 차단하지 않도록 조정
  - 회귀 질문을 테스트로 고정 — 동일한 질문과 정상 SQL 패턴이 다시 막히지 않도록 의미 검증 단위 테스트 추가
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/sql/SqlSemanticValidationService.java` (+18/-2) — 추이 질의 날짜 기준 허용 범위와 inventory 도메인 허용 테이블 보정
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/sql/SqlSemanticValidationServiceTest.java` (+23/-0) — 최근 7일 입고 수량 추이 회귀 테스트 추가
  - **Deleted**:
    - 없음

  </details>

### AI 결과 표현 추천 개선 (Codex)
- **User Intent**: AI 챗봇이 모든 SQL 결과를 억지로 그래프화하지 않고, 라우팅/목록/상세 조회는 표로 보여주며 질의 성격에 따라 바, 라인, 도넛, 통계 표시를 적절히 추천하도록 요청
- **Agent Context**: 기존 차트 추천은 `STAT/BAR/LINE/DONUT/NONE`만 지원하고 숫자 y축을 강제해 식별자성 숫자까지 그래프 지표로 오인할 수 있었다. API 계약을 최소 확장해 `TABLE` 타입을 추가하고, 룰 기반 사전 추천과 프론트 렌더링을 보강.
- **Key Decisions**:
  - `AiChartResponse`에 `TABLE` 타입만 추가 — 기존 AI 응답 구조를 유지하면서 “표가 주 표현”이라는 의도를 전달하기 위한 최소 API 확장
  - 명백한 표현 방식은 LLM 호출 전에 룰로 결정 — 라우팅/목록/상세 조회와 추이/비중/비교/KPI 질의를 안정적으로 구분해 억지 그래프 추천을 방지
  - 숫자 컬럼 중 식별자성 컬럼은 metric에서 제외 — `routing_id`, `operation_seq`, `item_id`처럼 그래프 y축으로 부적합한 값을 차단
  - 프론트는 기존 `AiChartPanel`을 확장 — View → Store → Service → API 계층 구조를 유지하고, 표시 컴포넌트 안에서 `TABLE` 안내와 `DONUT` 렌더링을 추가
- **Affected Files**: <details><summary>9개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiChartResponse.java` (+11/-0) — `TABLE` 타입과 table 응답 팩토리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/ChartRecommendationGenerator.java` (+5/-1) — 표/그래프 추천 기준과 식별자 숫자 제외 규칙 보강
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/chart/ChartRecommendationServiceImpl.java` (+210/-0) — 룰 기반 사전 추천, TABLE/STAT/LINE/DONUT/BAR 판단 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/chart/ChartSpecValidationService.java` (+34/-1) — TABLE 허용 및 식별자성 yKey 차단
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/chart/ChartRecommendationServiceImplTest.java` (+92/-5) — 라우팅 표, 추이 라인, 상태 분포 도넛, 단일 KPI 통계 테스트 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/chart/ChartSpecValidationServiceTest.java` (+24/-0) — TABLE 검증과 식별자 yKey 차단 테스트 추가
    - `frontend/src/api/aiApi.ts` (+1/-1) — 프론트 차트 타입에 `TABLE` 추가
    - `frontend/src/ui/AiChartPanel.vue` (+150/-1) — TABLE 안내/미리보기와 DONUT 렌더링 추가
    - `frontend/src/views/AiDataChatbotView.vue` (+9/-1) — TABLE 추천 시 결과 표를 차트 안내보다 우선 배치
  - **Deleted**:
    - 없음

  </details>
