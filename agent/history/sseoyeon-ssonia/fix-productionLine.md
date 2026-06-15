### 공장 생산 라우팅 개선 (Codex)
- **User Intent**: 기준정보 > 공장/생산 페이지 개선 목록을 바탕으로 사용자 권한 체감, 참조 영향 안내, 삭제 실패 사유, 신규 등록 기본값, 상태값, 백엔드 권한/무결성, 프론트 UX, 테스트 보강을 순서대로 반영하도록 요청
- **Agent Context**: 공장/생산 페이지가 `FactoryRouting` 마스터를 관리하고 작업지시/생산실적이 이를 참조하는 구조로 진단하여, 서버 권한/무결성 정책을 먼저 고정한 뒤 상태/usage API와 프론트 표시 로직을 연결
- **Key Decisions**:
  - 참조 중 라우팅은 수정/삭제를 제한하고 비활성화로 운영 중단을 표현 — 기존 작업지시/실적 표시값이 마스터 현재값을 참조하는 구조에서 과거 데이터 의미가 바뀌는 것을 방지
  - `/api/routings/**` 변경 API는 Manager/Admin 또는 Admin 권한으로 제한 — 기준정보 변경은 관리자성 업무라는 프로젝트 권한 정책을 백엔드에서 우선 강제
  - 작업지시 목록 검색은 Repository JPQL로 이전 — `findAll().stream()` 필터링의 확장성 문제를 줄이고 DB에서 상태/기간/키워드/공장/라인 조건을 처리
  - 프론트는 auth store의 역할 computed를 기준으로 메뉴와 버튼을 제어 — 화면별 임의 권한 판단을 줄이고 사용자 역할에 맞는 UI를 일관되게 제공
  - 날짜 포맷은 공통 유틸로 분리 — 화면마다 `new Date()` 파싱에 의존하는 방식을 줄여 LocalDateTime 문자열 표시를 안정화
- **Affected Files**: <details><summary>26개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/api/routing/dto/FactoryRoutingStatusUpdateRequest.java` (+17) — 라우팅 활성/비활성 상태 변경 요청 DTO 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/routing/dto/FactoryRoutingUsageResponse.java` (+20) — 작업지시/생산실적 참조 현황 응답 DTO 추가
    - `frontend/src/utils/dateFormat.ts` (+10) — 화면 공통 날짜/시간 포맷 유틸 추가
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/routing/FactoryRoutingApi.java` (+19/-1) — 상태 필터, 상태 변경, 참조 현황 API 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/routing/FactoryRoutingController.java` (+24/-2) — 확장된 라우팅 API 구현 연결
    - `backend/src/main/java/com/ssafy/demo_app/api/routing/dto/FactoryRoutingResponse.java` (+2/-0) — 라우팅 상태 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/ProductionExecutionRepository.java` (+4/-0) — 라우팅 참조 실적 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` (+32/-0) — 라우팅 참조 조회와 작업지시 DB 필터링 쿼리 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/ProductionExecutionServiceImpl.java` (+9/-0) — 작업지시 공장/라인과 다른 라우팅 실적 등록 차단
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceImpl.java` (+15/-12) — DB 필터링 쿼리 사용 및 비활성 라우팅 신규 작업지시 차단
    - `backend/src/main/java/com/ssafy/demo_app/domain/routing/entity/FactoryRouting.java` (+9/-0) — `ACTIVE/INACTIVE` 라우팅 상태 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/routing/repository/FactoryRoutingRepository.java` (+4/-0) — 상태별 라우팅 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/routing/service/FactoryRoutingService.java` (+12/-1) — 상태 필터, 상태 변경, 참조 현황 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/routing/service/FactoryRoutingServiceImpl.java` (+70/-6) — 참조 중 수정/삭제 차단, usage 계산, 상태 변경, DB 중복 예외 매핑 추가
    - `backend/src/main/java/com/ssafy/demo_app/global/exception/ErrorCode.java` (+3/-0) — 라우팅 참조/비활성/실적 라우팅 불일치 에러 추가
    - `backend/src/main/java/com/ssafy/demo_app/infrastructure/security/config/SecurityConfig.java` (+5/-0) — 라우팅 조회/변경/삭제 권한 규칙 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/production/service/WorkOrderServiceTest.java` (+40/-0) — 비활성 라우팅 및 실적 라우팅 불일치 테스트 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/routing/service/FactoryRoutingServiceTest.java` (+107/-2) — 참조 차단, 상태 변경, usage, DB 중복 충돌 테스트 추가
    - `frontend/src/api/factoryRoutingApi.ts` (+29/-0) — 라우팅 상태/usage 타입과 API 함수 추가
    - `frontend/src/layouts/components/AppSidebar.vue` (+19/-1) — 역할 기반 시스템/기준정보 메뉴 표시 제어
    - `frontend/src/services/factoryRoutingService.ts` (+21/-1) — 상태 변경과 usage 서비스 함수 추가
    - `frontend/src/state/authStore.ts` (+6/-0) — 관리자/매니저/기준정보 관리 권한 computed 추가
    - `frontend/src/state/factoryRoutingStore.ts` (+46/-2) — usage 상태, 상태 변경, lines 초기화 액션 추가
    - `frontend/src/views/FactoryLineMasterView.vue` (+124/-23) — 권한 기반 버튼, 상태 필터/변경, 참조 현황, 중복 사전 검증, 기본 순서 개선
    - `frontend/src/views/ProductionExecutionView.vue` (+12/-7) — 라우팅 기준정보 없음 안내와 날짜 포맷 유틸 적용
    - `frontend/src/views/WorkOrderView.vue` (+34/-12) — 활성 라우팅만 신규 선택, 라우팅 없음 안내, 품목 페이지 수집, 날짜 포맷 유틸 적용
  - **Deleted**:
    - 없음

  </details>

### Worker 기준정보 메뉴 조회 허용 (Codex)
- **User Intent**: Worker 계정으로 접속했을 때 기준정보 관리 메뉴가 아예 보이지 않는 문제를 해결하고, 접근 가능한 기능은 조회 중심으로 제한 요청
- **Agent Context**: `AppSidebar.vue`에서 기준정보 관리 그룹을 `canManageMasterData` 기준으로 통째로 숨기는 것이 원인이었음. 메뉴 노출 제한은 제거하고, 변경성 기능은 각 화면의 Manager/Admin 조건과 백엔드 권한 규칙으로 제한하는 방식으로 정리.
- **Key Decisions**:
  - 기준정보 메뉴는 인증 사용자에게 노출 — 프론트엔드 계층 구조상 사이드바는 탐색을 담당하고, 기능 권한은 화면 버튼 및 API 권한에서 제어하는 편이 사용자 요구와 일치
  - 품목 신규 등록 버튼에 `canManageMasterData` 조건 추가 — 품목 마스터 목록만 Worker에게 신규 등록 버튼이 보일 수 있어 거래처/BOM/공장라인과 동일한 기준으로 정렬
  - 백엔드 권한 규칙은 유지 — `GET`은 인증 사용자, 생성/수정/상태변경은 Manager/Admin, 삭제는 Admin 정책이 이미 적용되어 있어 서버 측 제한을 변경하지 않음
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/layouts/components/AppSidebar.vue` (+16/-1) — Worker에게 기준정보 관리 메뉴 그룹을 숨기던 조건 제거
    - `frontend/src/views/ItemMasterView.vue` (+5/-1) — 품목 신규 등록 버튼과 열기 함수에 Manager/Admin 권한 조건 추가
  - **Deleted**:
    - 없음

  </details>

### 공장 생산 최근 작업 일시 표시 (Codex)
- **User Intent**: 공장 및 생산 라인 목록에서 등록일시 옆에 단순한 최근 작업 일시를 추가 요청
- **Agent Context**: 새 DB 구조 없이 기존 작업지시 `updatedAt`과 생산실적 `createdAt` 중 더 최신 시각을 라우팅 응답에 계산 필드로 추가하고, 목록 테이블에 표시하도록 구현
- **Key Decisions**:
  - `lastActivityAt`은 작업지시/생산실적 기반 계산 필드로 처리 — 기준정보 감사 로그가 아닌 생산 작업 최근성을 표시하는 요구라 별도 테이블을 만들지 않음
  - 목록 정렬 필드에 `lastActivityAt`을 포함 — 등록일시와 동일하게 컬럼 클릭 정렬 UX를 유지
- **Affected Files**: <details><summary>6개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/routing/dto/FactoryRoutingResponse.java` — `lastActivityAt` 응답 필드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` — 라우팅별 최신 작업지시 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/ProductionExecutionRepository.java` — 라우팅별 최신 생산실적 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/routing/service/FactoryRoutingServiceImpl.java` — 최근 작업 일시 계산 로직 추가
    - `frontend/src/api/factoryRoutingApi.ts` — 라우팅 응답 타입에 `lastActivityAt` 추가
    - `frontend/src/views/FactoryLineMasterView.vue` — 최근 작업 일시 컬럼과 정렬 추가
  - **Deleted**:
    - 없음

  </details>

### 공장 생산 목록 액션 제거 (Codex)
- **User Intent**: 공장 및 생산 라인 목록에서 수정/비활성화/삭제가 바로 가능해 보이는 상태를 막고, 해당 관리 기능은 상세 페이지에서만 가능하도록 요청
- **Agent Context**: 목록 화면은 탐색과 검색/정렬/페이지네이션에 집중하고, 변경성 액션은 신규 상세 페이지에 이미 존재하므로 목록 테이블의 액션 컬럼과 관련 함수/아이콘을 제거
- **Key Decisions**:
  - 목록에서는 행 클릭 상세 이동만 유지 — 품목/거래처처럼 목록은 탐색, 상세는 관리 액션 담당으로 역할을 분리
  - 상세 페이지 액션은 유지 — 기존 권한/참조 제한 정책을 그대로 사용하면서 사용자가 명시적으로 상세를 확인한 뒤 변경하도록 구성
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/FactoryLineMasterView.vue` — 목록 테이블 액션 컬럼, 수정/비활성화/삭제 버튼, 목록 전용 변경 함수 제거
  - **Deleted**:
    - 없음

  </details>

### 공장 생산 라우팅 상세 분리 (Codex)
- **User Intent**: 공장 및 생산 라인 목록 화면이 검색-구조도-목록-상세가 한 페이지에 이어져 스크롤이 길어지므로, 구조도/목록에서 선택한 공정을 품목/거래처처럼 별도 상세 페이지로 이동시키고 목록을 10개 단위 페이지네이션과 정렬로 개선 요청
- **Agent Context**: 현재 데이터 모델은 공장/라인 별도 ID가 없고 `routingId`가 실제 상세 식별자이므로, 기존 `FactoryRouting` 구조를 유지하면서 `/master/factory-lines/:id` 상세 라우트를 추가하고 목록 화면에는 검색/구조도/페이지네이션 목록만 남김
- **Key Decisions**:
  - 상세 페이지는 `routingId` 기반으로 구성 — 백엔드 모델 변경 없이 품목/거래처 상세 페이지와 같은 라우팅 패턴을 적용
  - 페이지네이션과 정렬은 프론트 화면 상태로 처리 — 기존 `/api/routings` 응답 구조를 변경하지 않고 사용자 스크롤 문제를 우선 해결
  - 상세 페이지에서 단건/참조/동일 라인 공정 목록을 다시 조회 — 목록 페이지 선택 상태에 의존하지 않아 새로고침과 직접 URL 접근이 가능하도록 구성
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Created**:
    - `frontend/src/views/FactoryLineMasterDetailView.vue` — 라우팅 상세, 라인 요약, 공정 플로우, 참조 현황, 수정/상태/삭제 액션 페이지
  - **Modified**:
    - `frontend/src/router/index.ts` (+5/-0) — `/master/factory-lines/:id` 상세 라우트 추가
    - `frontend/src/views/FactoryLineMasterView.vue` (+208/-230) — 하단 상세 제거, 구조도/목록 상세 이동, 10개 페이지네이션, 컬럼 정렬 추가
  - **Deleted**:
    - 없음

  </details>

### 공장 생산 최근 실적 일시 통일 (Codex)
- **User Intent**: 공장 및 생산 라인 목록의 최근 작업 일시 기준을 작업지시 수정일이 아닌 최근 생산 실적 일시로 통일하고, 값이 없으면 `-`로 표시 요청
- **Agent Context**: 기존 구현은 작업지시 `updatedAt`과 생산실적 `createdAt` 중 최신값을 `lastActivityAt`으로 표시했으나, 사용자가 생산실적 기준으로 명확히 단일화 요청
- **Key Decisions**:
  - `WorkOrder.updatedAt`은 제외 — 목록의 최근 일시는 작업지시 변경 이력이 아니라 실제 생산 실적 발생 시각만 의미하도록 변경
  - 응답 필드는 `lastExecutionAt`으로 명명 — 화면 컬럼명 `최근 생산 실적 일시`와 의미를 맞춤
  - 생산실적이 없는 경우 `null`을 내려주고 프론트 공통 날짜 포맷터로 `-` 표시
- **Affected Files**: <details><summary>5개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/routing/dto/FactoryRoutingResponse.java` — 최근 일시 필드를 `lastExecutionAt`으로 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/production/repository/WorkOrderRepository.java` — 라우팅별 최신 작업지시 조회 메서드 제거
    - `backend/src/main/java/com/ssafy/demo_app/domain/routing/service/FactoryRoutingServiceImpl.java` — 최근 생산실적 `createdAt`만 응답하도록 계산 로직 단순화
    - `frontend/src/api/factoryRoutingApi.ts` — 라우팅 응답 타입을 `lastExecutionAt`으로 변경
    - `frontend/src/views/FactoryLineMasterView.vue` — 컬럼명/정렬/표시값을 최근 생산 실적 일시 기준으로 변경
  - **Deleted**:
    - 없음

  </details>
