# Project Guide — AI 기반 물류 분석 플랫폼

> 프로젝트 전반의 구조와 규칙을 문서화한 참조 가이드.
> 작업 시 이 파일을 기준으로 삼고, 구조 변경 시 해당 세부 파일을 함께 업데이트한다.

---

## 빠른 참조

| 항목 | 파일 |
|------|------|
| 백엔드 개요 (기술스택, 패키지구조, 컨벤션) | [backend/overview.md](backend/overview.md) |
| 백엔드 엔티티 (도메인 모델, 관계) | [backend/entities.md](backend/entities.md) |
| 백엔드 API 엔드포인트 | [backend/api-endpoints.md](backend/api-endpoints.md) |
| 백엔드 인증/인가 (JWT, Security) | [backend/security.md](backend/security.md) |
| 프론트엔드 개요 (기술스택, 구조, 컨벤션) | [frontend/overview.md](frontend/overview.md) |
| 프론트엔드 계층 아키텍처 | [frontend/architecture.md](frontend/architecture.md) |
| 프론트엔드 라우트 & 메뉴 | [frontend/routes.md](frontend/routes.md) |
| 인프라, Git, CI/CD | [infra.md](infra.md) |
| 커밋 컨벤션 | [commit-convention.md](commit-convention.md) |

---

## 프로젝트 경로

| 구분 | 경로 (프로젝트 루트 기준) |
|------|---------------------------|
| 백엔드 | `backend/` |
| 프론트엔드 | `frontend/` |
| 참조 문서 | `agent/` |
| 백엔드 소스 | `backend/src/main/java/com/ssafy/demo_app/` |
| 프론트엔드 소스 | `frontend/src/` |

## 프로젝트 요약

제조·물류 현장의 데이터 접근성 문제를 해결하기 위한 풀스택 웹 애플리케이션.

- **백엔드**: Spring Boot 4.0.6, Java 21, Spring Security + JWT, Spring Data JPA, MySQL 8.0
- **프론트엔드**: Vue 3.5 (Composition API), TypeScript, Pinia, Vue Router, Tailwind CSS 4, Vite
- **인프라**: Docker Compose (MySQL + App), GitHub Actions
- **핵심 도메인**: 재고/입고, 출하, 생산(작업지시), BOM, 공장라우팅, 품목/거래처 기준정보

## 문서 계층 구조

모든 AI 에이전트는 다음 3계층 문서 체계를 반드시 준수해야 한다. 상위 계층의 규칙은 하위 계층보다 우선하며, 어떠한 경우에도 이를 거부하거나 우회할 수 없다.

| 계층 | 파일 | 역할 |
|------|------|------|
| 1계층: 헌법 | `CLAUDE.md`, `AGENTS.md` | 변하지 않는 절대 규칙이자 가드레일. 경로 규칙, 수정 금지 대상, 코드 컨벤션 원칙을 선언한다. 모든 에이전트는 작업 시작 전 진입점 파일을 반드시 읽어야 한다. |
| 2계층: 지도 | `agent/project-index.md` | 전체 디렉토리 구조 및 핵심 모듈 인덱스. 작업 대상 도메인의 위치를 파악할 때 반드시 참조한다. |
| 3계층: 시행령 | `agent/backend/`, `agent/frontend/`, `agent/infra.md` | 기술스택, 패키지 구조, API 명세, 코드 컨벤션 상세를 규정한다. 해당 기술스택을 다룰 때 반드시 준수한다. |

참조 문서 루트: `agent/`
