### AI NL2SQL 백엔드 기반 구현 (Codex)
- **User Intent**: GMS OpenAI 호환 프록시를 사용하는 LangChain4j 기반 NL2SQL 백엔드를 현재 Spring Boot 시스템에 먼저 붙여달라는 요청
- **Agent Context**: 예제 프로젝트는 MyBatis와 단일 PoC 구조였으나, 현재 시스템은 JPA 중심 계층 구조이므로 LangChain4j AI 인터페이스만 가져오고 SQL 실행은 `JdbcTemplate`으로 대체
- **Key Decisions**:
  - `Spring AI`가 아닌 `LangChain4j` 의존성을 사용 — 사용자가 팀 기준으로 LangChain 사용을 확정했고 예제도 `@AiService` 기반으로 구성되어 있음
  - MyBatis mapper 대신 `JdbcTemplate` 실행 서비스를 추가 — 기존 프로젝트 ORM/DB 접근 컨벤션에 MyBatis가 없고 사용자가 MyBatis 미도입을 명시함
  - `AiQueryHistory`를 NL2SQL 이력의 중심 엔티티로 확장 — 기존 `domain/ai` 스켈레톤을 유지하면서 질문, SQL, 결과, 답변, 상태를 추적할 수 있음
  - SQL 검증을 별도 서비스로 분리 — 생성 SQL 실행 전 SELECT 제한, 위험 키워드 차단, 허용 테이블 검증, 기본 LIMIT 적용을 독립적으로 테스트하기 위함
- **Affected Files**: <details><summary>23개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/AiQueryController.java` — AI 질의 API 컨트롤러
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiQueryRequest.java` — AI 질의 요청 DTO
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiQueryResponse.java` — AI 질의 응답 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/repository/AiQueryHistoryRepository.java` — AI 질의 이력 Repository
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryService.java` — AI 질의 서비스 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` — NL2SQL 파이프라인 조립 서비스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AnswerGenerator.java` — 결과 기반 자연어 답변 생성 AI 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/DatabaseSchemaService.java` — 허용 스키마 설명 서비스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/IntentClassifier.java` — 데이터 조회 의도 분류 AI 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/SqlAssistant.java` — 자연어 SQL 생성 AI 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/SqlExecutionService.java` — 검증된 SQL 실행 서비스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/SqlSanitizer.java` — SQL 응답 정제 서비스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/SqlValidationResult.java` — SQL 검증 결과 객체
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/SqlValidationService.java` — SQL 보안 검증 서비스
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/SqlSanitizerTest.java` — SQL 정제 단위 테스트
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/SqlValidationServiceTest.java` — SQL 보안 검증 단위 테스트
  - **Modified**:
    - `backend/.secret.env.example` — GMS OpenAI 호환 API 환경변수 템플릿 추가
    - `backend/build.gradle` — LangChain4j 및 JDBC 의존성 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/entity/AiQueryHistory.java` — NL2SQL 답변, 결과, 실행 시간, 모델명 필드와 상태값 확장
    - `backend/src/main/java/com/ssafy/demo_app/infrastructure/security/config/SecurityConfig.java` — AI 질의 API 인증 정책 추가
    - `backend/src/main/resources/application.yml` — LangChain4j OpenAI 호환 설정 추가
    - `backend/src/test/resources/application.yml` — 테스트 컨텍스트용 LangChain4j 더미 설정 추가
  - **Deleted**:
    - 없음

  </details>

### AI 차트 추천 백엔드 구현 (Codex)
- **User Intent**: NL2SQL 조회 결과를 기반으로 그래프를 만들 수 있도록, 백엔드에서 AI가 차트 스펙을 추천하고 검증한 뒤 내려주는 구조 구현 요청
- **Agent Context**: 기존 NL2SQL 성공 응답 뒤에 차트 추천 단계를 추가하되, AI 추천 결과를 그대로 신뢰하지 않고 백엔드에서 지원 타입과 실제 row 컬럼을 검증하는 방식으로 구현
- **Key Decisions**:
  - 차트 추천은 `SUCCESS` 상태에서만 실행 — SQL 조회 실패와 차트 추천 실패를 분리하여 데이터 질의 UX를 안정적으로 유지
  - `NONE`, `STAT`, `BAR`, `LINE`, `DONUT`만 지원 — 프론트에서 구현할 렌더링 범위를 명확히 제한하고 AI가 임의 차트 타입을 반환하지 못하게 검증
  - AI 추천 결과를 `ChartSpecValidationService`에서 검증 — 실제 rows에 없는 `xKey`/`yKeys`, 숫자가 아닌 y축 컬럼, 차트 타입별 제약 위반을 차단
  - `AiQueryHistory.chartSpecJson`에 추천 결과 저장 — 질의 이력에서 어떤 차트 스펙이 반환됐는지 추적 가능
- **Affected Files**: <details><summary>11개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiChartResponse.java` — 차트 추천 응답 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ChartRecommendationGenerator.java` — 차트 스펙 추천 AI 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ChartRecommendationService.java` — 차트 추천 서비스 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ChartRecommendationServiceImpl.java` — AI 추천 호출 및 검증 조립 서비스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ChartSpecValidationResult.java` — 차트 스펙 검증 결과 객체
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ChartSpecValidationService.java` — 차트 스펙 검증 서비스
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/ChartRecommendationServiceImplTest.java` — 차트 추천 서비스 테스트
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/ChartSpecValidationServiceTest.java` — 차트 스펙 검증 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiQueryResponse.java` — AI 질의 응답에 chart 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/entity/AiQueryHistory.java` — 차트 스펙 JSON 저장 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` — 성공 응답에 차트 추천 단계 연결
  - **Deleted**:
    - 없음

  </details>
