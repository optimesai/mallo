# RULES.md — 프로젝트 지시사항 (SSoT)

> 모든 AI 에이전트 공통 프로젝트 규칙. 선언형 가드레일만 기술.
> 동적 workflow 지시 없음. Read-once 레퍼런스.

---
## 수정 금지

다음 파일은 읽거나 수정하지 않는다. 민감 정보를 포함할 수 있다.
- `.env`
- `.secret.env`

---

## 경로 작성 규칙

- 모든 경로는 **프로젝트 루트 기준 상대경로**로 작성 (예: `backend/src/...`, `frontend/src/...`)

---

## 작업 방법론

### 1. 코드 컨벤션

- 기존 코드의 패턴, 네이밍, 들여쓰기, 주석 스타일을 그대로 따름
- 새 파일 생성 시 같은 도메인의 기존 파일을 템플릿으로 삼음

### 백엔드 (Java / Spring Boot)
- 들여쓰기: 탭
- Lombok `@Getter`/`@Setter` 사용. `@Builder`/`@AllArgsConstructor` 미사용
- Service: Interface + Impl 패턴
- 생성자 주입: `@RequiredArgsConstructor`
- 트랜잭션: 클래스 레벨 `@Transactional(readOnly=true)`, 쓰기만 `@Transactional`
- Entity: `BaseTimeEntity` 상속, `FetchType.LAZY`, `@ManyToOne` 중심, `GenerationType.IDENTITY`
- Enum: `@Enumerated(EnumType.STRING)`

### 프론트엔드 (Vue 3 / TypeScript)
- 들여쓰기: 스페이스 2칸
- Composition API + `<script setup lang="ts">`
- 컴포넌트: PascalCase, TypeScript: camelCase
- import alias: `@/` → `src/`

---

## 프로젝트 참조 문서

| 항목 | 파일 |
|------|------|
| 프로젝트 인덱스 | `agent/project-index.md` |
| 커밋 컨벤션 | `agent/commit-convention.md` |
| 백엔드 개요 (기술스택, 패키지, 컨벤션) | `agent/backend/overview.md` |
| 엔티티-테이블 매핑 | `agent/backend/entities.md` |
| API 엔드포인트 | `agent/backend/api-endpoints.md` |
| 인증/인가 (JWT, Security) | `agent/backend/security.md` |
| 프론트엔드 개요 (기술스택, 디렉토리, 컨벤션) | `agent/frontend/overview.md` |
| 프론트엔드 계층 아키텍처 | `agent/frontend/architecture.md` |
| 프론트엔드 라우트 & 메뉴 | `agent/frontend/routes.md` |
| 인프라, Git, CI/CD | `agent/infra.md` |

- 구조 변경 시 해당 agent 문서 업데이트.
- `agent/` 문서에 없는 세부사항은 소스 코드 직접 탐색.

---

<!-- 새 규칙은 이 아래에 추가. workflow 지시가 아닌 선언형 가드레일만. -->
