### AI 서비스 패키지 구조 정리 (Codex)
- **User Intent**: AI 관련 코드가 `domain/ai/service` 루트에 과도하게 몰려 있어 파일 구조가 지저분하므로, 성능 저하 없이 다른 도메인 구조와 통일감 있게 정리 요청
- **Agent Context**: `AiQueryServiceImpl`의 호출 순서와 스키마 캐시, SQL 검증, DB 실행, LLM 호출 흐름은 그대로 유지하고, 협력 객체만 책임별 하위 패키지로 이동하는 방식으로 정리
- **Key Decisions**:
  - `service` 루트에는 `AiQueryService`, `AiQueryServiceImpl`만 유지 — `backend.md`의 도메인별 `entity/repository/service` 계층 구조를 유지하면서 AI 도메인의 내부 복잡도만 하위 패키지로 분리
  - LangChain4j `@AiService` 인터페이스는 병합하지 않음 — 프록시 구성과 LLM 호출 경계를 변경하지 않아 성능 및 동작 리스크를 최소화
  - SQL 검증/실행, 스키마 캐시, 차트 검증, 명확화 판단 로직은 내용 변경 없이 이동 — 기존 테스트를 유지해 동작 보존을 확인
- **Affected Files**: <details><summary>36개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/assistant/` — LangChain4j AI 인터페이스 하위 패키지
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/chart/` — 차트 추천 및 검증 하위 패키지
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/clarification/` — 질문 명확화 하위 패키지
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/prompt/` — 프롬프트 보조 및 후보 판별 하위 패키지
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/sql/` — SQL 생성 후처리, 검증, 실행 하위 패키지
    - `agent/history/sseoyeon-ssonia/refactor-aiCodeStyle.md` — 작업 히스토리 로그
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+13/-0) — 이동된 협력 객체 import 갱신
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/schema/DatabaseSchemaMetadataReader.java` (+0/-2) — `AiAllowedSchema` 이동에 따른 import 정리
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` (+12/-0) — 이동된 협력 객체 import 갱신
  - **Deleted**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/*.java` — AI 루트 서비스 패키지에 혼재하던 협력 객체를 책임별 하위 패키지로 이동
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/*Test.java` — AI 루트 테스트 패키지에 혼재하던 테스트를 책임별 하위 패키지로 이동

  </details>

### AI 서비스 전용 타입 파일 축소 (Codex)
- **User Intent**: AI 서비스 구조 정리 이후에도 성능 저하 없이 파일 개수를 더 줄일 수 있는지 검토하고, 안전한 범위에서 축소 작업 진행 요청
- **Agent Context**: LLM `@AiService` 인터페이스와 DB/캐시/SQL 검증 경계는 유지하고, 서비스 내부에서만 사용하는 결과 및 로딩 모델 타입을 내부 static class로 흡수
- **Key Decisions**:
  - `@AiService` 인터페이스는 병합하지 않음 — LangChain4j 프록시 생성 단위와 LLM 호출 경계를 유지해 동작 및 성능 리스크를 차단
  - SQL/차트/명확화 결과 객체는 각 검증 서비스 내부 타입으로 이동 — 필드와 getter/setter 구조는 유지하면서 별도 파일만 제거
  - Few-shot YAML 로딩 모델은 `FewShotPromptService` private 내부 타입으로 이동 — 외부 참조가 없는 순수 로딩 전용 모델이므로 캐시와 프롬프트 생성 성능에 영향 없음
- **Affected Files**: <details><summary>14개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImpl.java` (+2/-2) — 내부 타입 import 경로 갱신
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/chart/ChartRecommendationServiceImpl.java` (+1/-0) — 차트 검증 결과 내부 타입 참조
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/chart/ChartSpecValidationService.java` (+26/-0) — 차트 검증 결과 타입 내부화
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/clarification/ClarificationService.java` (+24/-0) — 명확화 결과 타입 내부화
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/prompt/FewShotPromptService.java` (+13/-0) — Few-shot 로딩 모델 내부화
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/sql/SqlValidationService.java` (+26/-0) — SQL 검증 결과 타입 내부화
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/AiQueryServiceImplTest.java` (+1/-1) — 내부 타입 import 경로 갱신
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/chart/ChartSpecValidationServiceTest.java` (+1/-0) — 내부 타입 import 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/clarification/ClarificationServiceTest.java` (+1/-0) — 내부 타입 import 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/ai/service/sql/SqlValidationServiceTest.java` (+1/-0) — 내부 타입 import 추가
  - **Deleted**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/chart/ChartSpecValidationResult.java` — 차트 검증 서비스 내부 타입으로 이동
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/clarification/ClarificationResult.java` — 명확화 서비스 내부 타입으로 이동
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/prompt/FewShotExample.java` — Few-shot 프롬프트 서비스 내부 타입으로 이동
    - `backend/src/main/java/com/ssafy/demo_app/domain/ai/service/sql/SqlValidationResult.java` — SQL 검증 서비스 내부 타입으로 이동

  </details>
