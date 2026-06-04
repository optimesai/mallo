# AGENTS.md

> Codex 계열 에이전트 진입점 문서. 세부 지시사항은 `agent/RULES.md`에 작성한다.

## 프로젝트 컨텍스트 정책

- 세션 최초 시작 시, `agent/RULES.md`(SSoT)를 1회 Read하여 시스템 컨텍스트에 탑재한다.
- 이후 명시적 요청이 없다면 해당 지침 파일을 다시 읽는(cat/read) 행위를 금지한다.

---

## Codex 특화

- TypeScript/Vue 3 최적화 지원 적극 활용
- `@workspace` 컨텍스트 인식 기능 사용 가능
