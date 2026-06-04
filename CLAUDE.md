# CLAUDE.md

> Claude Code 진입점 문서. 세부 지시사항은 `agent/RULES.md`에 작성한다.

## 프로젝트 컨텍스트 정책

- 세션 최초 시작 시, `agent/RULES.md`(SSoT)를 시스템 컨텍스트로 읽어들인다.
- 이후 명시적 요청이 없다면 해당 지침 파일을 다시 읽는(cat/read) 행위를 금지한다.

---

## Claude Code 특화

- caveman 모드 활성 (`/caveman lite|full|ultra` 조절)
- Agent, Workflow, Skill 도구 사용 가능
