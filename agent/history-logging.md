# History Logging — 에이전트 작업 히스토리 자동 로깅

> 모든 작업 완료 시점에 반드시 준수해야 하는 히스토리 기록 정책.
> 이 정책은 2인 이상 협업 환경에서 서로 다른 AI 에이전트 간 작업 파편화를 방지하고 변경 사항을 투명하게 추적하기 위해 존재한다.

---

## 목적

AI 에이전트 간의 작업 파편화를 막고 변경 사항을 투명하게 추적하기 위해, 모든 작업(기능 구현, 리팩토링, 버그 수정 등) 완료 시점에 반드시 지정된 형식으로 작업 히스토리를 기록해야 한다.

---

## 엄격한 규칙

다음 규칙은 어떠한 경우에도 절대 위반해서는 안 된다:

### 과거 히스토리 읽기 금지 (컨텍스트 절약)

- 사용자의 명시적인 요청이 없는 한, `agent/history/` 하위 경로에 있는 기존 파일들을 먼저 읽거나 탐색(`read`, `cat`, `grep`, `find` 등)해서는 절대 안 된다.
- 이는 불필요한 프롬프트 캐시 무효화 및 토큰 낭비를 원천 차단하기 위함이다.
- 히스토리는 "기록 전용"이다. 추적이 필요할 때만 명시적으로 열람한다.

### Git 충돌 방지

- 반드시 현재 Git 환경의 사용자 이름(`git config user.name`)을 기반으로 지정된 독립 디렉토리에만 파일을 생성/수정해야 한다.
- 서로 다른 사용자가 동일한 파일에 기록하여 충돌이 발생하는 것을 방지한다.

### 기록 방식 (Append-Only)

- 오늘 날짜의 파일이 없다면 새로 생성한다.
- 이미 존재한다면 기존 내용을 절대 수정하거나 삭제하지 않고, **가장 최하단에 내용을 추가(Append)** 한다.

---

## 기록 경로 및 파일명 패턴

```
agent/history/{git_username}/YYYY-MM-DD.md
```

| 구성요소 | 설명 |
|----------|------|
| `{git_username}` | `git config user.name` 명령어로 확인된 Git 사용자 이름 (소문자, 공백은 하이픈으로 치환) |
| `YYYY-MM-DD` | 작업 완료 시점의 현재 날짜 |

**사용자명 획득 명령어**:
```bash
git config user.name
```

---

## 히스토리 로깅 템플릿

작업을 마치고 해당 날짜 파일에 로그를 기록할 때, 아래 마크다운 템플릿을 **엄격하게 준수**해야 한다.
`[...]`로 표시된 부분은 동적으로 수집하고 요약한 내용으로 대체한다.

```markdown
### [HH:MM] [TASK-XX] 작업의 핵심 요약 제목 (사용 에이전트명)
- **Commit Hash**: `[방금 생성된 Git 커밋의 Short SHA 또는 Hash]`
- **User Intent**: [증상 + 요청 — 무슨 일이 일어나서, 무엇을 해달라고 했는지. 구체적 증상 기술, 1~2줄]
- **Agent Context**: [진단 + 접근 — 실제 근본 원인과 해결 방식. 사용자 요청과 다른 접근을 취했다면 그 이유, 1~2줄]
- **Key Decisions**:
  - [핵심 설계/추론 요약 1] — 왜 이 방식을 선택했는지 기술스택 컨벤션 근거와 함께 1줄로 명시
  - [핵심 설계/추론 요약 2] — 변환 방식, 영속성 전이, 성능 최적화 등 판단 근거 기술
- **Affected Files**:
  - `[프로젝트 루트 기준 상대 경로]` (new)
  - `[프로젝트 루트 기준 상대 경로]` (+N/-M)
  - `[프로젝트 루트 기준 상대 경로]` (remove)
```

---

## 각 필드별 작성 규칙

### 작업 제목 (Title)
- 작업의 핵심을 1문장으로 압축한다.
- **반드시 `[TASK-XX]` prefix를 포함**해야 한다. 이슈 트래킹 연동에 필수적이며, `git log --grep="TASK-XX"`로 해당 작업의 모든 커밋과 히스토리를 연결할 수 있다.
- 한글로 작성하며, 너무 길지 않게 (15자 내외 권장).
- 예시: `[TASK-49] 재고 마스터 페이징 조회 API 구현`, `[TASK-53] JWT 토큰 갱신 로직 리팩토링`

### Commit Hash
- 방금 생성된 Git 커밋의 short SHA(7자리)를 기입한다.
- 커밋하지 않은 작업의 경우 `(unstaged)` 또는 `(pending)`으로 표기한다.
- 복수 커밋일 경우 대표 커밋 하나만 기입한다.

### User Intent (사용자 의도)
- **증상 + 요청 구조**로 1~2줄 작성한다: 사용자가 관찰한 현상과 요청한 해결 방향을 구체적으로 기술.
- "무엇이 일어나서, 무엇을 해달라고 했는지"가 2주 후에 봐도 맥락이 복원될 수준이어야 한다.
- 추상적 표현("느리다", "안 된다") 대신 구체적 증상("타임아웃 발생", "화이트 스크린")을 기록한다.
- 올바른 예: `재고 현황 화면에서 대량 데이터 로딩 시 타임아웃이 발생하여, 페이지네이션과 검색 필터를 적용한 API 개선 요청`
- 잘못된 예: `재고 화면이 너무 느려서 페이지네이션과 검색 필터가 필요하다고 요청` ← "너무 느려서"는 구체적 증상이 아님

### Agent Context (에이전트 맥락)
- **진단 + 접근 구조**로 1~2줄 작성한다: 실제 근본 원인이 무엇이었고, 어떤 방식으로 해결했는지.
- User Intent와 실제 작업 간의 간극이 있었다면 그 차이를 반드시 드러낸다.
- 사용자가 요청한 해결책과 다른 접근을 취했다면 그 이유를 밝힌다.
- 예시: `3천 건 이상의 입고 데이터가 쌓이며 DB 풀스캔이 발생하는 것이 근본 원인으로 진단. Pageable + Specification 조합으로 동적 쿼리와 페이지네이션을 동시에 적용하여 쿼리당 20건으로 제한.`

### Key Decisions
- "무엇을 했는지"가 아닌 "왜 이 방식을 선택했는지"를 기술한다.
- 기술스택 컨벤션 문서(`agent/backend/overview.md`, `agent/frontend/overview.md` 등)를 근거로 인용한다.
- 각 항목은 1줄로 작성하며, 불가피한 경우에만 2줄까지 허용한다.
- 예시:
  - `N+1 문제 방지를 위해 FetchType.LAZY 적용 및 DTO 직접 매핑 — agent/backend/overview.md 엔티티 규칙 준수`
  - `Pinia setup store 패턴으로 마이그레이션 — agent/frontend/overview.md Store 규칙 준수`

### Affected Files
- 프로젝트 루트 기준 상대 경로로 기입한다.
- **각 파일명 뒤에 반드시 변경 유형을 표기**한다. `git diff --stat`에서 확인 가능:
  - 신규 생성: `(new)`
  - 수정: `(+N/-M)`
  - 삭제: `(remove)`
- 주요 변경 파일만 나열한다 (3~7개 권장, 10개 초과 시 대표 파일만).
- 예시:
  - `` `backend/.../api/inbound/InboundController.java` (new) ``
  - `` `frontend/src/views/InboundReceiptView.vue` (+42/-18) ``
  - `` `agent/RULES.md` (remove) ``
  - `` other n files `(0 new / 0 removed)` ``

---

## 사용 에이전트명 표기

| 에이전트 | 표기 |
|----------|------|
| Claude Code (Claude 모델) | `Claude Code` |
| Cursor (Claude 모델) | `Cursor` |
| Cursor (GPT 모델) | `Cursor (GPT)` |
| GitHub Copilot | `Copilot` |
| Codex CLI | `Codex` |
| 기타 | 실제 에이전트명 그대로 |

---

## 완전한 예시

```markdown
### [14:30] [TASK-63] 재고 마스터 페이징 조회 API 구현 (Claude Code)
- **Commit Hash**: `a1b2c3d`
- **User Intent**: 재고 현황 화면에서 대량 데이터 로딩 시 타임아웃이 발생하여, 페이지네이션과 검색 필터를 적용한 API 개선 요청
- **Agent Context**: 3천 건 이상의 입고 데이터가 쌓이며 DB 풀스캔이 발생하는 것이 근본 원인으로 진단. Pageable + Specification 조합으로 동적 쿼리와 페이지네이션을 동시에 적용하여 쿼리당 20건으로 제한.
- **Key Decisions**:
  - JPQL이 아닌 Spring Data JPA `Pageable` 인터페이스 사용 — agent/backend/overview.md Repository 규칙 준수
  - 검색 조건은 QueryDSL 대신 Specification으로 구현하여 동적 쿼리 구성의 복잡도 억제
  - 프론트엔드 페이지 크기는 20으로 고정 — 모바일 대응 시나리오 고려
- **Affected Files**:
  - `backend/.../api/inventory/InventoryController.java` (+35/-12)
  - `backend/.../domain/inventory/repository/InventoryRepository.java` (+28/-5)
  - `backend/.../domain/inventory/service/InventoryServiceImpl.java` (+42/-8)
  - `frontend/src/api/inventoryApi.ts` (+15/-4)
  - `frontend/src/views/InventoryStatusView.vue` (+42/-18)
```
