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

### 프론트 스타일 계층 정리 (Codex)
- **User Intent**: 프론트/백 코드 스타일 및 파일 구성 검토 후, auth 제외 프론트 작업 중 View-Service 직접 의존 제거와 Tailwind 색상/스타일 중앙화 작업을 먼저 진행 요청
- **Agent Context**: 프론트 지식 베이스의 `View → Store → Service → API` 흐름과 Tailwind 중앙 스타일 원칙을 기준으로, 기존 Store를 확장해 View 직접 Service 호출을 제거하고 `main.css`의 공통 `.app-*` 토큰/유틸리티를 보강함
- **Key Decisions**:
  - 기존 Store 재사용 — 신규 계층을 만들지 않고 `userStore`, `itemMasterStore`, `factoryRoutingStore`, `bomMasterStore`, `inboundStore`에 필요한 반환값과 액션만 보강해 프론트 계층 규칙을 준수
  - 공통 CSS 토큰 확장 — 직접 Tailwind 팔레트 색상 대신 `frontend/src/main.css`의 `--color-*` 기반 `.app-*` 클래스를 사용해 중앙 변경 가능성을 높임
  - auth 제외 범위 유지 — `SignupView.vue`의 authService 직접 호출은 사용자가 제외한 auth 범위로 판단하여 수정하지 않음
- **Affected Files**: <details><summary>13개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/main.css` (+36/-0) — 공통 alert, modal backdrop, disabled background, focus ring, text/background 별칭 클래스 추가
    - `frontend/src/state/bomMasterStore.ts` (+2/-0) — BOM 버전 로딩 액션의 결과 반환 추가
    - `frontend/src/state/factoryRoutingStore.ts` (+1/-0) — 라우팅 목록 로딩 액션의 결과 반환 추가
    - `frontend/src/state/inboundStore.ts` (+1/-0) — 로케이션 로딩 액션의 결과 반환 추가
    - `frontend/src/state/userStore.ts` (+34/-0) — 내 정보 조회/수정 Store 액션 추가
    - `frontend/src/ui/AiChartPanel.vue` (+4/-4) — 직접 slate/white 색상 클래스를 공통 클래스 기반으로 교체
    - `frontend/src/views/InboundReceiptView.vue` (+1/-1) — 등록 오류 색상 클래스를 공통 danger 스타일로 교체
    - `frontend/src/views/InboundStackView.vue` (+2/-2) — 적재 오류 색상 클래스를 공통 danger 스타일로 교체
    - `frontend/src/views/MyInfoView.vue` (+8/-7) — userService 직접 호출을 userStore 액션 호출로 변경
    - `frontend/src/views/PartnerMasterDetailView.vue` (+3/-3) — disabled 입력 배경을 공통 disabled 스타일로 교체
    - `frontend/src/views/PartnerMasterView.vue` (+13/-13) — 모달/폼/버튼의 직접 slate/white 색상 클래스를 공통 클래스 기반으로 교체
    - `frontend/src/views/ProductionExecutionView.vue` (+9/-7) — factoryRoutingService/inboundService 직접 호출을 Store 호출로 변경
    - `frontend/src/views/WorkOrderView.vue` (+12/-9) — item/factoryRouting/bom Service 직접 호출을 Store 호출로 변경
  - **Deleted**:
    - 없음

  </details>
