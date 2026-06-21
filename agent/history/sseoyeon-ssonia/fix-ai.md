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
