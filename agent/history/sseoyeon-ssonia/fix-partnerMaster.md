
### 거래처 마스터 UI 레이아웃 정리 (Codex)
- **User Intent**: 거래처 마스터 화면의 현재 UI가 품목 마스터와 비교해 사용성이 낮고 시각적으로 일관되지 않아, 품목 마스터와 동일한 레이아웃 흐름으로 재구성 요청
- **Agent Context**: 기존 거래처 화면은 전용 CSS 클래스와 탭 중심 구조가 섞여 있어 기준정보 화면 간 패턴이 달랐음. 거래처 기능은 유지하되 `ItemMasterView.vue`의 카드형 검색, 목록 테이블, 상세 카드, 모달 패턴에 맞춰 단일 화면 구조로 재작성함.
- **Key Decisions**:
  - Vue 3 Composition API와 기존 Store 흐름 유지 — `agent/project/frontend.md`의 View → Store → Service → API 계층 규칙을 지키기 위해 데이터 호출 계층은 변경하지 않음
  - 품목 마스터와 동일한 `app-*` 표면 스타일과 Tailwind 유틸리티 클래스 사용 — 기준정보 화면 간 사용자 경험을 맞추고 거래처 전용 CSS 의존도를 제거함
  - 전체 목록에서는 행 선택만 제공하고 수정·삭제·비활성화는 상세 카드에서만 제공 — 사용자가 대상 거래처의 사용 현황을 확인한 뒤 위험 작업을 수행하도록 흐름을 유지함
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/PartnerMasterView.vue` (+361/-869) — 품목 마스터 기준의 검색 카드, 목록 테이블, 상세 카드, 등록·수정 모달 레이아웃으로 재구성
  - **Deleted**:
    - 없음

  </details>

### 거래처 목록 스타일 변수 통일 (Codex)
- **User Intent**: 품목 마스터와 비교했을 때 거래처 마스터 목록에 검정 행 구분선과 별도 정렬 버튼이 보여 UI가 다르므로, 품목 마스터와 같은 프론트 변수와 목록 정렬 흐름으로 통일 요청
- **Agent Context**: 거래처 마스터는 이전 수정 후에도 `divide-y`, `bg-slate-900` 정렬 버튼, 직접 색상 클래스가 남아 있어 품목 마스터의 회색 테이블 헤더/행 경계와 달랐음. 검색·목록 영역을 품목 마스터의 `app-*` 변수 기반 클래스와 헤더 클릭 정렬 방식으로 맞춤.
- **Key Decisions**:
  - 정렬 버튼 그룹 제거 — 품목 마스터처럼 정렬 가능한 컬럼 헤더를 클릭해 정렬하도록 변경하여 목록 안에서 선택하는 흐름으로 통일함
  - `app-border-muted`, `app-bg-muted`, `app-hover-muted`, `app-font-*` 클래스 사용 — 직접 색상/진한 테두리 대신 기존 디자인 토큰을 사용해 기준정보 화면 간 일관성을 맞춤
  - 검색 폼 높이와 버튼 스타일을 품목 마스터와 같은 `h-11`, `app-bg-strong`, `app-bg-muted`, `app-accent-bg` 조합으로 변경 — 상단 필터 영역의 시각적 밀도와 버튼 색상을 통일함
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/PartnerMasterView.vue` (+83/-120) — 검색 폼, 정렬 방식, 테이블 헤더/행/페이지네이션 스타일을 품목 마스터 변수 기준으로 정리
  - **Deleted**:
    - 없음

  </details>

### 거래처 상세 화면 라우팅 분리 (Codex)
- **User Intent**: 거래처 마스터 진입 시 검색, 전체 목록, 상세가 세로 스크롤로 이어지는 구조를 제거하고, 품목 마스터처럼 목록 또는 검색에서 거래처를 선택하면 상세 화면으로 이동하도록 요청
- **Agent Context**: 기존 거래처 목록 화면 내부에 상세 카드와 수정·삭제·비활성화 액션이 같이 렌더링되어 품목 마스터의 목록/상세 라우트 분리 흐름과 달랐음. 목록 화면은 검색·목록·신규 등록만 담당하게 줄이고, 상세 조회/수정/상태 변경/삭제/이력 탭은 별도 상세 뷰로 이동함.
- **Key Decisions**:
  - `/master/partners/:id` 라우트 추가 — 품목 마스터의 `/master/items/:id` 구조와 동일하게 목록과 상세의 책임을 분리함
  - `PartnerMasterDetailView.vue` 신규 생성 — 거래처 사용 현황, 공급/출하 이력, 수정, 삭제, 비활성화 액션을 상세 화면으로 이동해 목록 화면 스크롤을 줄임
  - 목록 행 클릭과 검색 추천 선택을 `router.push`로 변경 — 사용자가 목록 또는 검색 결과에서 거래처를 선택하면 상세 화면으로 링킹되도록 함
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Created**:
    - `frontend/src/views/PartnerMasterDetailView.vue` — 거래처 상세 조회, 수정, 상태 변경, 삭제, 사용 현황, 공급/출하 이력 전용 화면
  - **Modified**:
    - `frontend/src/router/index.ts` (+5/-0) — 거래처 상세 라우트 `/master/partners/:id` 추가
    - `frontend/src/views/PartnerMasterView.vue` (+22/-227) — 인라인 상세 섹션 제거, 행/검색 추천/등록 완료 시 상세 라우트 이동으로 변경
  - **Deleted**:
    - 없음

  </details>

### 고객사 상세 출하 이력 오류 수정 (Codex)
- **User Intent**: 공급사 상세는 정상인데 고객사 상세로 들어가면 서버 오류가 표시되는 문제를 해결 요청
- **Agent Context**: 고객사 상세 진입 시에만 호출되는 출하 품목 이력 API에서 `OutboundShipping.createdAt`이 null인 기존 데이터에 대해 `isAfter`를 직접 호출해 500 오류가 발생할 수 있었음. 출하 기준 시간을 `shippedAt` 우선, 없으면 `createdAt`, 둘 다 없으면 null로 처리하도록 방어 로직을 추가함.
- **Key Decisions**:
  - 백엔드 서비스에서 널 안전 처리 — 프론트에서 오류를 숨기지 않고 API가 정상 응답을 보장하도록 도메인 집계 로직을 수정함
  - 출하 기준 일시는 `shippedAt`을 우선 사용 — 실제 출하 완료 시간이 있으면 업무적으로 더 정확하고, 없을 때만 생성 시간을 fallback으로 사용함
  - null 출하 일시 케이스 테스트 추가 — 기존 데이터나 테스트 데이터의 감사 시간이 비어 있어도 고객사 상세가 500으로 깨지지 않도록 회귀를 방지함
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceImpl.java` (+13/-5) — 고객사 출하 이력 집계 시 출하 기준 일시 널 안전 처리
    - `backend/src/test/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceTest.java` (+32/-0) — 출하 일시가 없는 고객사 출하 이력 조회 테스트 추가
  - **Deleted**:
    - 없음

  </details>

### 고객사 상세 이력 실패 격리 (Codex)
- **User Intent**: 고객사 상세에서 출하 이력이 null이거나 불완전하면 서버 오류로 표시하지 말고 “이력이 없습니다” 상태로 끝내도록 요청
- **Agent Context**: 고객사 상세 로딩은 기본 정보, 사용 현황, 출하 이력을 한 흐름에서 처리해 출하 이력 API 실패가 상세 전체 오류로 승격될 수 있었음. 백엔드에서 불완전 출하 데이터를 건너뛰고, 프론트에서 이력 조회 실패를 빈 이력으로 격리함.
- **Key Decisions**:
  - 프론트 상세 화면에서 이력 조회 실패를 catch 처리 — 고객사 기본 정보와 사용 현황은 유지하고 출하 이력 테이블만 빈 상태로 표시하기 위함
  - 백엔드 출하 이력 집계에서 품목이 없는 출하 레코드 건너뛰기 — 비정상/레거시 데이터가 있어도 API가 500으로 실패하지 않도록 함
  - 출하 수량 null은 0으로 집계 — 집계 응답 생성 자체를 막지 않고 사용자에게 이력 없음 또는 안전한 수치를 제공함
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceImpl.java` (+12/-1) — 출하 이력 집계의 품목/수량 null 방어 처리
    - `backend/src/test/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceTest.java` (+17/-0) — 품목 정보가 없는 출하 이력은 빈 목록으로 처리하는 테스트 추가
    - `frontend/src/views/PartnerMasterDetailView.vue` (+20/-4) — 출하/공급 이력 조회 실패를 상세 전체 오류가 아닌 빈 이력 상태로 격리
  - **Deleted**:
    - 없음

  </details>

### 거래처 삭제 비활성화 유도 모달 적용 (Codex)
- **User Intent**: 참조 중인 거래처 삭제 시 상단 알림이 아니라 품목 마스터의 비활성화 유도처럼 팝업 모달로 안내되도록 요청
- **Agent Context**: 거래처 상세는 참조 중인 거래처 삭제 시 브라우저 confirm과 상단 toast로 흐름이 이어져 품목 마스터의 삭제 확인 UX와 달랐음. 삭제 버튼 클릭 시 사용 현황을 조회하고, 참조 항목과 삭제 불가 안내를 모달에서 보여주도록 변경함.
- **Key Decisions**:
  - 품목 마스터의 삭제 확인 모달 패턴 재사용 — `app-backdrop`, `app-bg-warning-soft`, `app-bg-warning` 디자인 토큰을 동일하게 적용해 기준정보 상세 화면 간 UX를 통일함
  - 참조 항목을 모달 안에 표시 — 입고/출하 참조 건수를 먼저 보여줘 사용자가 삭제 불가 이유를 즉시 이해하도록 함
  - 참조 중인 경우 삭제 버튼 대신 비활성화 버튼 제공 — 기존 정책인 “삭제 대신 비활성화”를 명시적 선택 흐름으로 바꿈
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/PartnerMasterDetailView.vue` (+67/-5) — 거래처 삭제 확인 모달, 참조 항목 표시, 비활성화 유도 버튼 추가
  - **Deleted**:
    - 없음

  </details>

### 거래처 상세 비고 표시 보존 (Codex)
- **User Intent**: 거래처 등록 시 비고를 입력했는데 상세 페이지에서 등록한 비고가 보이지 않는 문제 해결 요청
- **Agent Context**: 백엔드 DTO/엔티티는 비고를 저장·응답하도록 연결되어 있었으나, 상세 화면 진입 시 재조회 응답이 비고를 누락하면 등록 직후 캐시된 비고가 덮어써질 수 있었음. 상세 재조회 시 등록 직후 캐시된 비고를 fallback으로 보존하고, 백엔드 저장 응답 테스트를 추가함.
- **Key Decisions**:
  - 상세 화면에서 `found.note ?? cachedPartner?.note` fallback 적용 — 등록 직후 상세 이동 시 재조회 응답 누락으로 비고가 사라지는 화면 문제를 방지함
  - 백엔드 서비스 테스트에 비고 저장 케이스 추가 — `PartnerRequest.note`가 trim되어 `PartnerResponse.note`로 반환되는 계약을 검증함
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/PartnerMasterDetailView.vue` (+5/-1) — 상세 재조회 시 캐시된 비고를 fallback으로 보존
    - `backend/src/test/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceTest.java` (+26/-0) — 거래처 등록 비고 저장 응답 테스트 추가
  - **Deleted**:
    - 없음

  </details>

### 거래처 코드 자동 생성 적용 (Codex)
- **User Intent**: 거래처 신규 등록 시 코드 미입력 또는 `SUP-`/`CUS-`만 입력해도 그대로 저장되는 문제를 막고, 품목 마스터처럼 미입력 시 자동 코드가 생성되도록 요청
- **Agent Context**: 프론트가 신규 등록 기본값으로 `SUP-`를 넣고 있었고, 백엔드는 접두어만 있는 코드도 유효 코드로 저장하고 있었음. 품목 마스터의 `resolveItemCode`/`generateItemCode` 흐름을 거래처에 맞게 적용해 공급사와 고객사 시퀀스를 분리함.
- **Key Decisions**:
  - 백엔드에서 자동 생성 책임 처리 — 프론트 조작과 무관하게 빈 값 또는 접두어만 입력된 값은 서버에서 안전하게 자동 생성되도록 함
  - 공급사/고객사 prefix별 독립 시퀀스 사용 — `SUP-0001`, `CUS-0001` 형태로 각각 최신 번호를 찾아 다음 번호를 생성함
  - 프론트 기본값 제거 — 품목 마스터와 동일하게 코드 입력란은 비워두고 “미입력 시 자동 생성” 안내만 표시함
- **Affected Files**: <details><summary>5개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/partner/dto/PartnerRequest.java` (+2/-3) — 거래처 코드 미입력 허용 및 자동 생성 설명 반영
    - `backend/src/main/java/com/ssafy/demo_app/domain/partner/repository/PartnerMasterRepository.java` (+1/-0) — prefix 기반 거래처 코드 조회 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceImpl.java` (+47/-8) — 거래처 코드 자동 생성 및 prefix별 번호 추출 로직 추가
    - `backend/src/test/java/com/ssafy/demo_app/domain/partner/service/PartnerServiceTest.java` (+56/-0) — 공급사/고객사 자동 코드 생성 테스트 추가
    - `frontend/src/views/PartnerMasterView.vue` (+13/-12) — 신규 등록 코드 기본값 제거, 빈 코드 중복확인 생략, 자동 생성 안내 문구 반영
  - **Deleted**:
    - 없음

  </details>

### 거래처 접두어 자동 생성 요청 보강 (Codex)
- **User Intent**: 거래처 코드를 입력하지 않으면 백엔드에서 거래처 구분에 따라 `SUP-` 또는 `CUS-`로 시작하는 코드를 자동 생성하도록 요청
- **Agent Context**: 백엔드는 빈 값과 접두어만 입력된 값을 자동 생성 대상으로 처리하고 있었으나, 프론트에서 `SUP-`/`CUS-`만 있는 값을 직접 입력 코드처럼 중복확인할 여지가 있었음. 접두어만 남은 상태도 미입력과 동일한 자동 생성 요청으로 처리하도록 보강함.
- **Key Decisions**:
  - 프론트 제출 전 `isAutoGeneratedCodeRequest` 도입 — 빈 코드와 접두어만 있는 코드를 같은 자동 생성 요청으로 분류해 중복확인을 건너뜀
  - 검증 로직도 동일 헬퍼 사용 — 사용자가 직접 코드를 입력한 경우에만 prefix/형식 검증을 수행하도록 일관화함
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/PartnerMasterView.vue` (+7/-3) — `SUP-`/`CUS-`만 입력된 상태를 백엔드 자동 생성 요청으로 처리
  - **Deleted**:
    - 없음

  </details>
