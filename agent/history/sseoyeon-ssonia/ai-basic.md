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
