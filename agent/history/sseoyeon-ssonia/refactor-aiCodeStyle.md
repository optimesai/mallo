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

### 입고 API v2 연결 전환 (Codex)
- **User Intent**: 입고 관리 화면이 재고 관리 구현이 아닌 inbound 구현을 타도록 연결하고, 창고 적재 중복 처리 문제를 방지하도록 요청
- **Agent Context**: 프론트 `inboundApi.ts`가 `/api/inbounds`를 호출해 백엔드 `InventoryServiceImpl`로 연결되고 있었음. 중복 적재 방지 상태 변경이 구현된 `InboundServiceImpl`을 사용하도록 `/api/v2/inbounds`로 엔드포인트를 전환함
- **Key Decisions**:
  - 프론트 URL만 변경 — 백엔드에는 이미 `InboundApi`/`InboundController`/`InboundServiceImpl` 경로가 존재하고, `stackInventory()`에서 `STACKED` 상태 변경을 수행하므로 최소 변경으로 의도한 도메인 경계를 맞춤
  - 품목/거래처/로케이션 조회는 유지 — 입고 보조 조회 API는 각각 `/api/items`, `/api/partners`, `/api/locations`를 계속 사용해야 하므로 입고 오더 CRUD/상태 변경 URL만 `/api/v2/inbounds`로 변경
  - 검증 분리 수행 — 병렬 `npm run build`가 출력 없이 지연되어 동일 검증을 `npm run type-check`와 `npm run build-only`로 나눠 통과 확인
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/api/inboundApi.ts` (+6/-6) — 입고 관리 CRUD/검수/적재 API 호출 URL을 `/api/inbounds`에서 `/api/v2/inbounds`로 변경
  - **Deleted**:
    - 없음

  </details>
### 공정 실적 수량 흐름 정리 (Codex)
- **User Intent**: 작업 지시 자재 출고 후 공정 실적을 순서대로 등록할 때 각 공정마다 자재가 중복 차감되고 제품 재고/진행률이 공정 수만큼 과대 반영되는 문제를 수정 요청
- **Agent Context**: 기존 `ProductionExecutionServiceImpl`이 실적 등록마다 BOM 자재 차감과 제품 입고를 수행하는 구조였기 때문에, 자재 출고는 작업지시 단위 1회로 고정하고 마지막 공정 완료 수량만 제품 입고/진행률에 반영하도록 변경
- **Key Decisions**:
  - 작업지시 등록 화면은 공장/라인만 받되 기존 스키마의 `routingId`는 해당 라인의 첫 공정 대표값으로 유지 — DB 마이그레이션 없이 프론트 요구사항을 우선 반영
  - 공정 실적 등록은 첫 공정은 출고 수량, 이후 공정은 직전 공정 양품 수량을 초과할 수 없도록 제한 — 공정 불량품이 다음 공정 투입 가능 수량에 포함되지 않도록 처리
  - 진행률과 작업지시 누적 실적은 라인의 마지막 공정 실적만 기준으로 산정 — 공정 단계 수만큼 생산량과 진행률이 중복 누적되는 문제 방지
  - 프론트 공정 실적 화면은 백엔드가 내려주는 공정별 진행 상태를 기준으로 선택 가능한 공정만 노출 — View가 API/Service를 우회하지 않는 기존 계층 규칙 유지
- **Affected Files**: <details><summary>9개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderOperationProgressResponse.java` (+46/-0) — 작업지시 상세의 공정별 처리 가능/완료/잔여 수량 응답 DTO 추가
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderDetailResponse.java` (+1/-0) — 작업지시 상세 응답에 공정별 진행 정보 목록 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/ProductionExecutionServiceImpl.java` (+52/-69) — 실적별 자재 추가 차감 제거, 공정 순서/가능 수량 검증, 마지막 공정만 제품 입고 처리
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` (+88/-2) — 마지막 공정 기준 실적 요약 및 공정별 진행 상태 계산 추가
    - `backend/src/main/java/com/ssafy/demo_app/global/exception/ErrorCode.java` (+2/-0) — 출고 수량 초과와 이전 공정 부족 오류 코드 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` (+46/-14) — 새 자재 출고→공정 실적→마지막 공정 입고 흐름에 맞춰 생산 테스트 갱신
    - `frontend/src/api/workOrderApi.ts` (+16/-0) — 공정별 진행 응답 타입 추가
    - `frontend/src/views/ProductionExecutionView.vue` (+49/-14) — 공정별 진행 현황 표시 및 등록 가능 공정 선택 제한
    - `frontend/src/views/WorkOrderView.vue` (+21/-13) — 작업지시 등록에서 공정 선택 제거, 공장/라인 선택 시 대표 라우팅 자동 지정
  - **Deleted**:
    - 없음

  </details>
### 자재 출고 안내 문구 제거 (Codex)
- **User Intent**: BOM 기반 생산 자재 출고 화면 상단에 표시되는 실제 작업지시 기반 자재 불출 안내 카드가 불필요하여 삭제 요청
- **Agent Context**: 첨부 이미지와 `MaterialIssueView` 템플릿을 확인해 안내 카드 블록과 해당 카드에서만 사용하던 아이콘 import가 제거 대상임을 확인
- **Key Decisions**:
  - 기능 로직과 API 호출 흐름은 수정하지 않고 안내 카드 UI만 제거 — 사용자가 요청한 표시 문구 삭제 범위 준수
  - `ShieldAlert` import도 함께 제거 — Vue 타입체크에서 미사용 import 문제가 없도록 정리
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/MaterialIssueView.vue` (+0/-16) — BOM 기반 생산 자재 출고 화면 안내 카드 삭제 및 미사용 아이콘 import 제거
  - **Deleted**:
    - 없음

  </details>
### 공정 목록 현재 단계 표시 (Codex)
- **User Intent**: 공정 실적 및 원부자재 화면의 작업지시 목록에서 공정 컬럼이 대표 공정이 아니라 현재 진행 가능한 가장 앞 단계 공정을 보여주도록 수정 요청
- **Agent Context**: 목록 응답에는 기존 대표 라우팅의 `operationSeq/operationName`만 포함되어 선택 전에는 공정별 진행 상태를 알 수 없었음. 백엔드 목록 응답에 현재 공정 필드를 추가하고 프론트 목록 표시만 해당 필드를 우선 사용하도록 변경
- **Key Decisions**:
  - `WorkOrderResponse`에 `currentOperationRoutingId`, `currentOperationSeq`, `currentOperationName`을 추가 — 기존 대표 라우팅 필드를 유지해 다른 화면 영향 최소화
  - 현재 공정은 공정별 진행 상태 중 `currentOperation=true`인 항목을 우선 사용 — 이전에 구현한 공정 순서/가능 수량 계산 기준과 동일하게 유지
  - 현재 진행 가능 공정이 없으면 앞쪽 미완료 공정, 모두 완료되면 마지막 공정을 fallback으로 사용 — READY/CLOSE 등 비등록 상태에서도 목록 컬럼이 비지 않도록 처리
  - BOM 없는 테스트 작업지시는 표시용 현재 공정 계산에서 대표 라우팅으로 fallback — 목록 조회가 BOM 검증 예외로 실패하지 않도록 방어
- **Affected Files**: <details><summary>4개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/production/dto/WorkOrderResponse.java` (+16/-0) — 현재 진행 가능 공정 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` (+29/-6) — 작업지시 목록 응답 생성 시 현재 공정 계산 및 BOM 없음 fallback 추가
    - `frontend/src/api/workOrderApi.ts` (+3/-0) — 현재 공정 응답 타입 필드 추가
    - `frontend/src/views/ProductionExecutionView.vue` (+9/-3) — 목록 공정 컬럼과 검색 제안 문구가 현재 공정을 우선 표시하도록 변경
  - **Deleted**:
    - 없음

  </details>
