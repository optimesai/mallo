### 프론트 스타일 변수 정리 (Codex)
- **User Intent**: 프론트 코드에 선언되어 있지만 사용되지 않는 Tailwind/CSS 변수들이 있어, 코드 전체를 탐색한 뒤 미사용이면 제거해달라는 요청.
- **Agent Context**: `frontend/src/main.css`의 CSS custom property 선언을 기준으로 `frontend/` 전체 참조를 대조했고, `--tw-ring-color`는 Tailwind `ring-*` 유틸리티와 함께 쓰이는 상태 클래스라 제거 대상에서 제외.
- **Key Decisions**:
  - 전역 스타일 토큰만 제거 — `agent/project/frontend.md`의 실제 코드 우선 원칙과 기존 `main.css` 중심 스타일 구조를 유지하기 위함.
  - `var(--*)` 참조가 0건인 토큰만 제거 — 사용 중인 테마 토큰과 Tailwind ring 상태 클래스 동작을 보존하기 위함.
  - 기존 사이드바 글자 크기 조정 diff는 보존 — 작업 전 이미 존재하던 변경을 되돌리지 않고 미사용 변수 정리만 추가하기 위함.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/main.css` (+3/-24) — 미사용 색상/테마 CSS custom property 제거
  - **Deleted**:
    - 없음

  </details>
