
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
