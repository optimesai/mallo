# Commit Convention

> 프로젝트 커밋 메시지 작성 규칙. 기본적으로 Conventional Commit Guide를 따르며, 모든 AI 에이전트가 공통으로 준수한다.

---

## 형식

```
[TASK-{번호}] {type}: {한글 설명}
```

| 구성요소 | 필수 | 설명 |
|----------|------|------|
| `[TASK-{번호}]` | ✅ | 작업 태스크 번호. 복수일 경우 `[TASK-{번호1}/{번호2}..]` |
| `{type}:` | ✅ | 커밋 타입 (아래 표 참조) |
| `{한글 설명}` | ✅ | 한글 자연어, 간결하게. 마침표 없음 |

## 타입

### 기능

| 타입 | 사용 상황 | 예시 |
|------|----------|------|
| `feat` | 기능 구현, 화면/API/서비스 추가 | `feat: 입고 등록 및 검수 관리 화면 구현` |
| `init` | 프로젝트/모듈 최초 생성 | `init: Vue 프로젝트 초기화` |

### 유지보수

| 타입 | 사용 상황 | 예시 |
|------|----------|------|
| `chore` | 설정, 의존성, 빌드, 태스크 시작 | `chore: start task-n` |
| `chore(deps)` | 의존성 추가/변경 | `chore(deps): @lucide/vue 의존성 추가` |
| `refactor` | 동작 변경 없는 코드 정리 | `refactor: 패키지명 리팩토링` |
| `refactor({domain})` | 도메인 한정 리팩토링 | `refactor(Inventory): API 명세, 컨트롤러 분리` |
| `clean` | 불필요 파일/클래스 제거 | `clean: remove redundant WebConfig class` |
| `docs` | 문서 작성/수정 | `docs: API 명세 업데이트` |
| `style` | 포매팅, 린트 | `style: 린트 규칙 적용` |

### 버그 및 검증

| 타입 | 사용 상황 | 예시 |
|------|----------|------|
| `fix` | 버그 수정 | `fix: JPA Auditing이 정상적으로 수행되지 않던 문제 수정` |
| `test` | 테스트 작성/수정 | `test: InventoryService 통합 테스트 작성 및 검증 완료` |

## 제목 작성 규칙

### 기본 원칙

1. **한글**: 모든 제목은 한글로 작성 (예외: 기술 용어, 클래스명)
2. **간결**: 불필요한 조사 생략, 핵심 동작만 기술
3. **명사형 종결**: "구현", "추가", "수정", "작성", "제거", "분리", "마이그레이션", "교체" 등
4. **마침표 없음**: 제목 끝에 `.` 사용하지 않음

### 권장 동사

| 패턴 | 예시 |
|------|------|
| `XX 구현` | `입고 등록 및 검수 관리 화면 구현` |
| `XX 추가` | `로케이션 CRUD API 추가` |
| `XX 수정` | `빌드 오류 수정` |
| `XX 구성` | `frontend CI 파이프라인 구성` |
| `XX 작성` | `BaseEntity 작성` |
| `XX 제거` | `사용하지 않는 컴포넌트 제거` |
| `start task-XX` | `start task-42` |

### API 엔드포인트 포함 시

API 관련 커밋은 제목 끝에 HTTP 메서드와 경로를 괄호로 표기:

```
feat: 입고 완료 처리 API 구현 (PUT /api/inbounds/{id}/complete)
feat: 출하 지시 조회 API 구현 (GET /api/shippings, /{id})
```

## 태스크 관리

```
[TASK-42] chore: start task-42
[TASK-42] feat: 현재고 현황 및 자재 불출 처리 메뉴 라우팅 등록
[TASK-42] feat: 현재고 현황, 수불 이력, 자재 불출 처리 화면 뷰 구현
  ...
[TASK-42] feat: 실시간 재고 및 작업 지시 API 호출 함수 정의
```

- 태스크 시작 시 반드시 `chore: start task-{번호}` 커밋
- 동일 태스크의 모든 커밋은 같은 `[TASK-XX]` prefix로 통일
- 브랜치명: `task/{번호}`

## 금지 패턴

| 패턴 | 예시 |
|------|------|
| 영어 제목만 사용 | `setup core dependencies` |
| 타입(`type:`) 누락 | `CI 스크립트 작성` |
| PascalCase 제목 | `Add README.md` |
| TASK prefix 누락 | `feat: XX 구현` |

> ⚠️ `[TASK-{번호}]` prefix는 항상 포함한다. 이슈 트래킹 연동에 필수.
