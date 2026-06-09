
### 사용자 관리 인증 UX 보강 (Codex)
- **User Intent**: 탭을 나가면 자동 로그인 상태가 유지되지 않도록 하고, 비관리자 사용자에게 사용자 및 권한 메뉴 접근 불가 안내를 표시하며, 회원가입 시 비밀번호를 두 번 확인하도록 요청.
- **Agent Context**: 기존 인증 초기화는 refresh token cookie만 있으면 새 access token을 발급받아 로그인 상태를 복원했음. `sessionStorage` 기반 탭 세션 플래그와 `pagehide` 로그아웃 요청을 추가하고, 사용자 관리 화면은 비관리자도 진입 후 안내 메시지를 볼 수 있게 라우트 리다이렉트를 사용하지 않는 방식으로 구성.
- **Key Decisions**:
  - 탭 세션 플래그를 `sessionStorage`에 저장 — 탭을 닫고 새로 열었을 때 자동 refresh를 막아 탭 단위 로그인 세션으로 제한하기 위함.
  - `pagehide`에서 `keepalive` 로그아웃 요청 전송 — 탭 종료 시 백엔드 refresh token 폐기를 시도해 서버 측 세션도 정리하기 위함.
  - 비관리자 접근 차단은 라우터 리다이렉트가 아닌 화면 안내로 처리 — 사용자가 왜 접근할 수 없는지 직접 확인할 수 있게 하기 위함.
  - 회원가입 비밀번호 확인은 프론트 제출 전 검증으로 처리 — 서버 요청 전에 오입력을 차단해 사용자 피드백을 즉시 제공하기 위함.
- **Affected Files**: <details><summary>10개 파일</summary>

  - **Created**:
    - `frontend/src/state/userStore.ts` — 사용자 관리 상태와 액션 추가
    - `frontend/src/views/UserManagementView.vue` — 사용자 및 권한 관리 화면 추가
  - **Modified**:
    - `frontend/src/api/authApi.ts` (+8/-0) — 탭 종료 로그아웃 요청 추가
    - `frontend/src/api/userApi.ts` (+26/-0) — 관리자용 사용자 API 추가
    - `frontend/src/main.ts` (+3/-0) — 탭 종료 로그아웃 리스너 등록
    - `frontend/src/router/index.ts` (+5/-0) — 사용자 및 권한 라우트 추가
    - `frontend/src/router/navigation.ts` (+1/-1) — 시스템 관리 메뉴 경로 연결
    - `frontend/src/services/userService.ts` (+45/-11) — 사용자 관리 서비스 추가
    - `frontend/src/state/authStore.ts` (+32/-1) — 탭 세션 기반 인증 초기화와 종료 로그아웃 처리
    - `frontend/src/views/SignupView.vue` (+22/-0) — 회원가입 비밀번호 확인 필드와 검증 추가
  - **Deleted**:
    - 없음

  </details>

### 탭 종료 로그아웃 부작용 수정 (Codex)
- **User Intent**: 로그인 직후 쿠키가 보이다가 다른 페이지로 이동하면 `refreshToken` 쿠키가 사라지는 현상이 반복된다는 문제 제기.
- **Agent Context**: `pagehide` 이벤트에서 `/api/auth/logout`을 `keepalive`로 호출하면서 탭 종료뿐 아니라 문서 이동/새로고침성 전환에서도 백엔드 로그아웃이 실행되어 쿠키가 삭제되는 것이 원인. 자동 서버 로그아웃 트리거를 제거하고 `sessionStorage` 기반 자동 refresh 차단만 유지.
- **Key Decisions**:
  - `pagehide` 로그아웃 제거 — 브라우저 이벤트만으로 탭 닫기와 문서 이동을 안정적으로 구분할 수 없어 정상 페이지 이동 중 쿠키 삭제를 유발하기 때문.
  - `sessionStorage` 세션 플래그 유지 — 탭을 닫고 새로 열었을 때 refresh token cookie가 남아 있어도 프론트가 자동 refresh를 호출하지 않도록 하기 위함.
- **Affected Files**: <details><summary>3개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/api/authApi.ts` — unload 로그아웃 fetch 제거
    - `frontend/src/state/authStore.ts` — `pagehide` 리스너와 unload 로그아웃 호출 제거
    - `frontend/src/main.ts` — 탭 종료 로그아웃 리스너 등록 제거
  - **Deleted**:
    - 없음

  </details>

### 회원가입 사번 중복 안내 구현 (Codex)
- **User Intent**: 회원가입 화면에서 `사번 중복 여부 확인` 문구만 표시되는 상태를 개선하고, 사번 입력 중 이미 존재하는 값이면 해당 위치에 중복 안내를 표시해 달라는 요청.
- **Agent Context**: 중복 여부는 프론트 단독으로 알 수 없으므로 공개 인증 API에 사번 존재 여부 조회 엔드포인트를 추가하고, 회원가입 화면에서 입력값 변경 시 디바운스 호출로 사용 가능/중복/확인 중/오류 상태를 안내하도록 연결.
- **Key Decisions**:
  - 중복 확인 API를 `/api/auth/employee-no/exists`에 추가 — 회원가입 전 공개 검증이므로 인증이 필요한 사용자 관리 API가 아닌 인증 도메인 공개 API로 배치.
  - 프론트 입력 감지는 350ms 디바운스로 처리 — 매 키 입력마다 즉시 네트워크 요청을 보내지 않고 사용자 입력이 잠시 멈춘 뒤 확인하기 위함.
  - 중복 확인 중 또는 중복 상태에서는 회원가입 제출을 차단 — 백엔드 중복 예외와 별개로 사용자에게 즉시 수정 기회를 제공하기 위함.
- **Affected Files**: <details><summary>6개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/auth/AuthController.java` (+7/-0) — 사번 중복 확인 API 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/auth/service/AuthService.java` (+2/-0) — 사번 존재 여부 조회 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/auth/service/AuthServiceImpl.java` (+5/-0) — 사번 존재 여부 조회 구현
    - `frontend/src/api/authApi.ts` (+7/-0) — 사번 중복 확인 API 호출 추가
    - `frontend/src/services/authService.ts` (+14/-0) — 사번 중복 확인 서비스 추가
    - `frontend/src/views/SignupView.vue` (+97/-2) — 입력 중 중복 확인 안내와 제출 차단 로직 추가
  - **Deleted**:
    - 없음

  </details>

### 회원가입 사번 중복확인 제출 방식 변경 (Codex)
- **User Intent**: 사번 입력 중 자동 중복 확인이 아니라 Enter 또는 중복확인 버튼을 눌렀을 때만 확인하고, 중복확인을 완료해야 회원가입을 진행하도록 요청.
- **Agent Context**: 기존 구현은 입력 변경 시 디바운스로 중복 확인 API를 호출했으나 서버 실행 상태에 따라 입력 중 오류 문구가 노출될 수 있었음. 명시적 사용자 액션 기반으로 중복 확인을 수행하고, 확인한 사번과 현재 입력 사번이 일치할 때만 제출을 허용하도록 변경.
- **Key Decisions**:
  - `checkedEmployeeNo`를 별도 저장 — 중복확인 후 사번을 수정한 상태로 회원가입을 우회하지 못하도록 하기 위함.
  - Enter와 중복확인 버튼에만 중복 확인 API 호출 연결 — 사용자가 원할 때만 서버 요청을 보내고 입력 중 불필요한 오류 노출을 줄이기 위함.
  - 회원가입 제출 전 중복확인 여부를 검사 — 중복확인을 하지 않았거나 확인 후 사번이 바뀐 경우 명확한 안내 문구를 제공하기 위함.
- **Affected Files**: <details><summary>1개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `frontend/src/views/SignupView.vue` — 사번 중복확인 버튼/Enter 방식 및 제출 전 확인 필수 처리
  - **Deleted**:
    - 없음

  </details>
