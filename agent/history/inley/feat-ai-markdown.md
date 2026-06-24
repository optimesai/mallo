### NL2SQL 챗봇 응답 마크다운 렌더러 도입 (Claude Code)
- **User Intent**: LLM이 생성하는 응답 텍스트에 마크다운 형식(볼드, 리스트, 코드 블록 등)이 포함되어 있으나 현재 일반 텍스트로 출력 중이어서 가독성이 떨어짐. 안전하고 완성도 높은 UI 표현을 위해 마크다운 렌더러 도입 요청.
- **Agent Context**: 프로젝트가 Vue 3 + TypeScript + Vite 환경임을 확인. `marked` + `dompurify` 조합을 선택하여 GFM 스펙 지원과 XSS 방지를 동시에 달성. 기존 메시지 버블은 `whitespace-pre-line`으로 일반 텍스트 렌더링 중이었으며, 어시스턴트 메시지에만 마크다운 렌더러를 적용하여 변경 범위 최소화.
- **Key Decisions**:
  - `marked` + `dompurify` 조합 선택 — 프로젝트의 Vue 환경에 적합하며(Option B), `marked.parse()` 후 `DOMPurify.sanitize()`로 XSS 차단. tailwind typography 플러그인 대신 CSS 변수 기반 직접 스타일링으로 프로젝트 디자인 시스템과 일치
  - GFM(gfm: true) + 줄바꿈(breaks: true) 활성화 — SQL 코드 블록, 테이블 렌더링, 리스트 등 LLM이 자주 생성하는 포맷을 모두 지원
  - 어시스턴트 메시지만 마크다운 렌더링, 사용자 메시지는 기존 plain text 유지 — 사용자 입력은 의도적으로 마크다운 파싱할 필요가 없으므로 변경 범위 최소화
  - `v-html` 사용 시 DOMPurify로 허용 태그/속성만 명시적 화이트리스트 처리하여 보안 위험 제로화
- **Affected Files**: <details><summary>4개 파일</summary>

  - **Created**:
    - `frontend/src/ui/AiMarkdownRenderer.vue` — 마크다운 파싱(marked) + 새니타이징(DOMPurify) + 프로젝트 CSS 변수 기반 스타일링을 포함한 렌더러 컴포넌트
  - **Modified**:
    - `frontend/src/views/AiDataChatbotView.vue` (+2/-1) — 어시스턴트 메시지 영역에 AiMarkdownRenderer 적용
    - `frontend/package.json` (+2/-0) — marked, dompurify 의존성 추가
    - `frontend/package-lock.json` — 의존성 잠금 파일 업데이트

  </details>
