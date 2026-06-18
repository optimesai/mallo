
### 사용자 권한 관리 화면 구현 (Codex)
- **User Intent**: `시스템 관리 > 사용자 및 권한` 메뉴에 백엔드 사용자 관리 기능을 연결하고 프론트 화면을 구현해 달라는 요청.
- **Agent Context**: 기존 메뉴는 `/` 홈으로 연결되어 실제 사용자 관리 화면이 없었고, `userApi`/`userService`는 `/api/users/me`만 지원했음. 관리자용 전체 사용자 조회, 상세 조회, 권한 변경, 삭제 API를 4계층 흐름으로 연결하고 전용 화면을 추가.
- **Key Decisions**:
  - `View → Store → Service → API` 계층을 모두 추가 — `agent/project/frontend.md`의 4계층 아키텍처를 지키기 위해 화면에서 API를 직접 호출하지 않음.
  - 메뉴 경로는 `/system/users`로 분리 — 라우트는 소문자 케밥과 리소스 계층 구조를 따르고 기존 홈 라우트와 사용자 관리 책임을 분리하기 위함.
  - 프론트에서도 `ADMIN` 전용 라우트 가드를 추가 — 백엔드 권한 차단과 별개로 사용자가 접근 불가 화면에 들어가지 않도록 UX 수준에서 1차 차단하기 위함.
  - 본인 계정 및 마지막 관리자 삭제/강등 제약을 UI에 반영 — 백엔드 보호 정책과 동일한 사용 금지 조건을 버튼/셀렉트 비활성화로 먼저 안내하기 위함.
- **Affected Files**: <details><summary>6개 파일</summary>

  - **Created**:
    - `frontend/src/state/userStore.ts` — 사용자 관리 목록/상세/권한 변경/삭제 상태 관리
    - `frontend/src/views/UserManagementView.vue` — 사용자 및 권한 관리 화면
  - **Modified**:
    - `frontend/src/api/userApi.ts` (+26/-0) — 관리자용 사용자 관리 API 호출 추가
    - `frontend/src/services/userService.ts` (+46/-11) — 관리자용 사용자 관리 서비스와 공통 에러 메시지 처리 추가
    - `frontend/src/router/index.ts` (+12/-0) — `/system/users` 라우트와 `ADMIN` 접근 가드 추가
    - `frontend/src/router/navigation.ts` (+1/-1) — 시스템 관리 메뉴 경로 변경
  - **Deleted**:
    - 없음

  </details>

### 사용자 권한 관리 UX 개선 (Codex)
- **User Intent**: 사용자 및 권한 화면에서 검색어 입력 즉시 목록이 바뀌는 동작을 중단하고, 검색 버튼 또는 Enter로만 목록이 갱신되게 변경 요청. 키워드 추천을 추가하고, 목록/검색과 상세/권한 변경/삭제가 한 화면에 같이 있어 UX가 떨어지는 문제를 페이지 흐름으로 분리 요청.
- **Agent Context**: 기존 `UserManagementView.vue`는 `keyword` 입력값을 곧바로 `filteredUsers` 계산에 사용하고 목록과 상세 패널을 좌우 2단으로 동시에 표시했음. 입력값과 적용된 검색어를 분리하고, 목록 모드와 상세 모드를 같은 라우트 안에서 전환하도록 재구성.
- **Key Decisions**:
  - `keywordInput`과 `appliedKeyword`를 분리 — 검색 버튼 또는 Enter 제출 전까지 목록 결과가 바뀌지 않도록 사용자 의도 기반 검색 흐름을 보장하기 위함.
  - 키워드 추천은 로컬 사용자 목록에서 사번, 이름, 부서를 후보로 생성 — 추가 API 없이 기존 전체 사용자 조회 데이터로 작업지시 등록 검색과 유사한 자동완성 경험을 제공하기 위함.
  - 목록/검색과 상세/권한 변경/삭제를 `pageMode`로 분리 — 한 화면 스크롤에 모든 기능을 넣는 대신 선택 후 상세 작업으로 넘어가는 관리 흐름을 명확히 하기 위함.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/UserManagementView.vue` — 검색 제출 제어, 키워드 추천, 목록/상세 모드 분리
  - **Deleted**:
    - 없음

  </details>

### 사용자 권한 코드 표시 보강 (Codex)
- **User Intent**: 권한 라벨만으로 `ADMIN`, `MANAGER`, `WORKER`의 의미가 헷갈릴 수 있어 한글 권한명과 코드 값을 함께 보여달라는 요청.
- **Agent Context**: 권한 표시는 `roleOptions`의 `label`을 통해 목록 배지, 상세, 추천, 권한 변경 select에 공통 적용되고 있었음. 공통 라벨만 수정해 화면 전체 표시를 일관되게 변경.
- **Key Decisions**:
  - `roleOptions.label`에 코드 값을 함께 표기 — 중복 템플릿 수정을 피하고 모든 권한 표시 위치에 동일한 문구를 적용하기 위함.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/UserManagementView.vue` — 권한 라벨을 `시스템 관리자 [ADMIN]` 형식으로 변경
  - **Deleted**:
    - 없음

  </details>

### 사용자 권한 표시 및 이동 보완 (Codex)
- **User Intent**: 상단 통계 카드와 권한 필터에도 권한 코드 값을 함께 표시하고, 권한 변경 후 목록 화면으로 이어지게 해달라는 요청.
- **Agent Context**: 기존 변경은 공통 `roleOptions` 기반 표시에는 반영됐지만 상단 통계 카드와 필터 select는 정적 문구를 사용하고 있었음. 권한 저장 성공 후에도 상세 화면에 머물러 있어 후속 목록 확인 흐름이 끊겼음.
- **Key Decisions**:
  - 통계 카드와 필터 option에도 코드 값을 명시 — 사용자가 화면 어느 위치에서든 한글 권한명과 시스템 권한 코드를 함께 인지하도록 하기 위함.
  - 권한 저장 성공 시 `pageMode`를 `list`로 전환 — 수정 결과를 목록에서 바로 확인하는 관리 흐름을 만들기 위함.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/UserManagementView.vue` — 상단 통계/필터 권한 코드 표시 및 권한 변경 후 목록 전환
  - **Deleted**:
    - 없음

  </details>
