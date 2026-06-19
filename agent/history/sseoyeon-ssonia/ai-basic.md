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

### AI 질의 인증 오류 처리 (Codex)
- **User Intent**: Swagger에서 `POST /api/ai/queries` 요청 시 500 응답이 발생하여 원인 확인과 수정 요청
- **Agent Context**: `AiQueryController`가 `@AuthenticationPrincipal`로 받은 `CustomUserDetails`를 null 확인 없이 사용하여, 인증 principal이 주입되지 않은 경우 NPE가 전역 핸들러에서 500으로 변환될 수 있었다. 인증 누락은 서버 오류가 아니라 401 응답이어야 하므로 명시적 인증 예외로 변경
- **Key Decisions**:
  - 컨트롤러 진입부에서 `userDetails == null`을 검사 — API 계층에서 인증 컨텍스트 누락을 빠르게 차단하여 도메인 서비스로 잘못된 userId가 전달되지 않도록 함
  - 기존 `ErrorCode.UNAUTHORIZED`와 `BusinessException` 사용 — 프로젝트 전역 예외 처리 규칙을 따르고 응답 포맷을 `ApiResponse.fail()`로 유지
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/AiQueryController.java` (+5/-0) — 인증 principal 누락 시 401 예외 처리 추가
  - **Deleted**:
    - 없음

  </details>

### AI 질의 안정성 개선 (Codex)
- **User Intent**: AI NL2SQL 백엔드에서 스키마 조회 실패가 500으로 이어질 수 있는 문제, Few-shot YAML 반복 파싱, 모호한 질문에 대한 재질문 부재를 먼저 개선해달라는 요청
- **Agent Context**: 기존 `AiQueryServiceImpl`는 스키마 컨텍스트 로딩을 예외 처리 밖에서 수행하고, `FewShotPromptService`는 매 요청마다 YAML을 읽으며, 데이터 질문이 모호해도 곧바로 SQL 생성을 시도했다. 스키마 실패를 히스토리 상태로 저장하고, Few-shot은 메모리 캐시로 재사용하며, 규칙 기반 후보 감지 후 후보일 때만 LLM 재질문 생성을 수행하도록 변경
- **Key Decisions**:
  - `SCHEMA_LOAD_FAILED`, `CLARIFICATION_REQUIRED` 상태를 추가 — SQL 생성 실패와 스키마 로딩 실패, 사용자 추가 입력 필요 상태를 구분하여 API 응답과 히스토리에서 원인을 추적 가능하게 유지
  - Few-shot은 TTL 없는 메모리 캐시로 유지 — YAML 리소스는 배포 산출물이라 런타임 변경 가능성이 낮고, 요청마다 파싱하는 비용을 제거하는 단순 캐시가 적합
  - 재질문 판단은 규칙 기반 후보 감지 후 LLM 호출 — 모든 데이터 질문마다 LLM 호출을 추가하지 않고, 불량률·추이·비교·재고처럼 모호성 가능성이 높은 질문에만 재질문 생성 비용을 사용
  - 재질문 LLM 실패는 `notRequired`로 fallback — 재질문 생성 실패 때문에 전체 데이터 질의가 막히지 않도록 SQL 생성 단계로 진행
- **Affected Files**: <details><summary>12개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ClarificationAssistant.java` (+37/-0) — 재질문 필요 여부와 질문 문구를 JSON으로 생성하는 AI 인터페이스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ClarificationCandidateService.java` (+75/-0) — 불량률·추이·비교·재고 질문의 모호성 후보 규칙 감지
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ClarificationResult.java` (+26/-0) — 재질문 판단 결과 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/ClarificationService.java` (+47/-0) — 후보 감지, LLM 호출, JSON 파싱, fallback 조립 서비스
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` (+97/-0) — 스키마 실패와 재질문 분기 테스트
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/ClarificationCandidateServiceTest.java` (+35/-0) — 규칙 기반 모호성 후보 감지 테스트
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/ClarificationServiceTest.java` (+59/-0) — 재질문 LLM 호출/파싱/fallback 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/ai/dto/AiQueryResponse.java` (+2/-0) — 재질문 필요 여부와 재질문 문구 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/entity/AiQueryHistory.java` (+2/-0) — 스키마 실패와 재질문 필요 실행 상태 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+71/-4) — 스키마 실패 처리, Few-shot 로딩 실패 처리, 재질문 분기 연결
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/FewShotPromptService.java` (+16/-1) — YAML Few-shot 프롬프트 메모리 캐시와 evict 메서드 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/FewShotPromptServiceTest.java` (+10/-0) — 캐시 evict 후 재로드 가능성 테스트
  - **Deleted**:
    - 없음

  </details>

### AI 스키마 프롬프트 개선 (Codex)
- **User Intent**: AI NL2SQL 기능의 2번 항목에서 하드코딩된 스키마 설명과 Few-shot 부재를 개선하여 실제 DB 스키마 기반 SQL 생성 품질을 높이고 싶다는 요청
- **Agent Context**: 기존 `DatabaseSchemaService`가 정적 문자열만 반환하고 `SqlAssistant`가 예시 없이 스키마만 받아 SQL을 생성하는 구조였으므로, `information_schema` 기반 메타데이터 조회와 30분 메모리 캐시, YAML 기반 Few-shot 주입 구조로 교체
- **Key Decisions**:
  - 스키마 노출 테이블과 SQL 검증 whitelist를 `AiAllowedSchema`로 공통화 — 백엔드 도메인 서비스 계층 내에서 AI가 볼 수 있는 테이블과 실행 가능한 테이블 기준이 어긋나지 않도록 유지
  - `information_schema` 조회 결과를 DTO로 구조화한 뒤 `SchemaPromptBuilder`에서 문자열 생성 — 하드코딩 문자열을 제거하면서도 LLM 프롬프트 포맷을 테스트 가능한 단위로 분리
  - 핵심 JOIN 관계는 `SchemaRelationshipProvider`에서 수동 보강 — 실제 DB FK 존재 여부에 의존하지 않고 제조·물류 도메인의 주요 조인 경로를 안정적으로 프롬프트에 제공
  - Few-shot 예시는 `backend/src/main/resources/ai/few-shot-examples.yml`로 분리 — SQL 예시를 block scalar로 관리해 Java 문자열보다 리뷰와 수정이 쉬운 리소스 형태로 유지
- **Affected Files**: <details><summary>19개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiAllowedSchema.java` (+23/-0) — AI 허용 테이블 whitelist 공통 상수
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/FewShotExample.java` (+15/-0) — Few-shot YAML 항목 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/FewShotPromptService.java` (+71/-0) — YAML Few-shot 로딩 및 프롬프트 변환 서비스
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/schema/AiSchemaColumn.java` (+15/-0) — 스키마 컬럼 메타데이터 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/schema/AiSchemaRelationship.java` (+28/-0) — 조인 관계 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/schema/AiSchemaTable.java` (+16/-0) — 스키마 테이블 메타데이터 DTO
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/schema/DatabaseSchemaMetadataReader.java` (+68/-0) — `information_schema` 기반 허용 테이블 메타데이터 조회 컴포넌트
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/schema/SchemaPromptBuilder.java` (+67/-0) — 스키마 DTO와 관계 정보를 LLM 프롬프트 문자열로 변환
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/schema/SchemaRelationshipProvider.java` (+32/-0) — 핵심 도메인 조인 관계 제공 컴포넌트
    - `backend/src/main/resources/ai/few-shot-examples.yml` (+88/-0) — 생산·재고·BOM 대표 NL2SQL Few-shot 예시
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/DatabaseSchemaServiceTest.java` (+68/-0) — 스키마 캐시 재사용 및 갱신 테스트
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/FewShotPromptServiceTest.java` (+20/-0) — YAML Few-shot 로딩 테스트
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/schema/SchemaPromptBuilderTest.java` (+57/-0) — 스키마 프롬프트 생성 테스트
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/schema/SchemaRelationshipProviderTest.java` (+27/-0) — 핵심 조인 관계 제공 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+3/-1) — SQL 생성 시 Few-shot 프롬프트 전달
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/DatabaseSchemaService.java` (+36/-133) — 하드코딩 스키마 문자열을 메타데이터 조회 기반 캐시 구조로 교체
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/SqlAssistant.java` (+5/-0) — Few-shot 예시와 관계 기반 JOIN 우선 규칙 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/SqlValidationService.java` (+1/-15) — 허용 테이블 목록을 공통 whitelist로 대체
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
