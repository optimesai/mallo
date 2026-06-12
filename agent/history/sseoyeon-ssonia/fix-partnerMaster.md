
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
