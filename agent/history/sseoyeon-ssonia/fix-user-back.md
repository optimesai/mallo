
### 회원 인증 토큰 관리 개선 (Codex)
- **User Intent**: Access Token 단독 JWT와 localStorage 저장 구조가 로그아웃 무효화, 토큰 관리, 추가 보안 흐름 측면에서 부족하다는 피드백을 받아 Access Token + Refresh Token 구조로 변경 요청
- **Agent Context**: 기존 구현은 서버 세션 없이 Access Token만 발급하고 프론트 localStorage에서 보관했으므로 로그아웃 시 서버가 토큰을 무효화할 수 없었음. Access Token은 Vue 메모리에만 저장하고 Refresh Token은 HttpOnly Cookie와 DB 해시 저장소로 관리하는 방식으로 변경.
- **Key Decisions**:
  - Access Token 수명은 30분, Refresh Token 수명은 7일로 분리 — agent/project/backend.md의 Spring Security + JWT Stateless 구조를 유지하면서 탈취 시 노출 시간을 줄이기 위한 설정
  - Refresh Token Rotation과 Access Token 블랙리스트는 적용하지 않음 — 사용자 요청 범위를 준수하고 별도 Redis 없이 MySQL/JPA 기반 Refresh Token 폐기로 로그아웃 무효화를 처리하기 위한 결정
  - Refresh Token 원문은 DB에 저장하지 않고 SHA-256 해시만 저장 — 토큰 저장소 유출 시 원문 재사용 위험을 줄이기 위한 보안 결정
  - 프론트 API 호출은 공통 Axios client로 통합 — agent/project/frontend.md의 API 계층 인증 헤더 공통 생성 원칙을 따르고 localStorage 직접 접근을 제거하기 위한 결정
- **Affected Files**: <details><summary>28개 파일</summary>

  - **Created**:
    - `backend/src/main/java/com/ssafy/demo_app/domain/auth/entity/RefreshToken.java` — Refresh Token 해시와 만료/폐기 상태를 저장하는 JPA 엔티티
    - `backend/src/main/java/com/ssafy/demo_app/domain/auth/repository/RefreshTokenRepository.java` — Refresh Token 조회, 폐기, 사용자별 삭제 Repository
    - `backend/src/main/java/com/ssafy/demo_app/domain/auth/service/AuthTokenResult.java` — 로그인 응답과 Cookie 발급용 Refresh Token을 분리 전달하는 서비스 결과 객체
    - `frontend/src/api/client.ts` — 메모리 Access Token 첨부와 401 재발급 재시도를 담당하는 공통 Axios client
  - **Modified**:
    - `backend/.secret.env.example` (+5/-1) — JWT 만료 시간과 Refresh Cookie Secure 설정 예시 추가
    - `backend/src/main/java/com/ssafy/demo_app/api/auth/AuthController.java` (+61/-3) — login Cookie 발급, refresh, logout API 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/auth/service/AuthService.java` (+5/-1) — login 결과 타입 변경 및 refresh/logout 계약 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/auth/service/AuthServiceImpl.java` (+72/-1) — Refresh Token 생성, 해시 저장, 검증, 폐기 로직 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/user/service/UserServiceImpl.java` (+20/-5) — 비밀번호/권한 변경 시 Refresh Token 폐기와 사용자 삭제 시 토큰 삭제 연결
    - `backend/src/main/java/com/ssafy/demo_app/global/exception/ErrorCode.java` (+2/-0) — Refresh Token 오류 코드 추가
    - `backend/src/main/java/com/ssafy/demo_app/infrastructure/security/jwt/JwtTokenProvider.java` (+35/-4) — Access/Refresh Token 생성과 검증 분리
    - `backend/src/main/resources/application.yml` (+3/-1) — Access 30분, Refresh 7일, Cookie Secure 설정 추가
    - `backend/src/test/resources/application.yml` (+3/-1) — 테스트 JWT 설정 분리
    - `frontend/src/api/authApi.ts` (+17/-1) — login/refresh/logout Cookie credentials 적용
    - `frontend/src/api/bomMasterApi.ts` (+12/-36) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/factoryRoutingApi.ts` (+10/-35) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/inboundApi.ts` (+10/-34) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/inventoryApi.ts` (+4/-17) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/itemMasterApi.ts` (+6/-23) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/partnerMasterApi.ts` (+6/-24) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/shippingApi.ts` (+6/-24) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/userApi.ts` (+3/-17) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/api/workOrderApi.ts` (+12/-41) — localStorage 인증 헤더 제거 및 공통 client 적용
    - `frontend/src/layouts/components/AppHeader.vue` (+1/-1) — 서버 로그아웃 API 완료 대기 처리
    - `frontend/src/router/index.ts` (+5/-1) — 라우트 진입 전 Refresh Token 기반 인증 초기화 추가
    - `frontend/src/services/authService.ts` (+26/-0) — refresh/logout 서비스 에러 처리 추가
    - `frontend/src/state/authStore.ts` (+55/-33) — Access Token localStorage 저장 제거 및 메모리 토큰/초기화 흐름 추가
    - `frontend/src/views/MyInfoView.vue` (+9/-0) — 비밀번호 변경 후 세션 종료와 로그인 이동 처리
  - **Deleted**:
    - 없음

  </details>

### Docker 인증 빌드 경로 수정 (Codex)
- **User Intent**: 최신 인증 코드로 재빌드했는데도 로그인 응답에 Set-Cookie가 없고 expiresIn이 86400으로 남아 있어 실제 실행 서버가 최신 코드인지 확인 및 수정 요청
- **Agent Context**: Dockerfile이 최신 소스가 아니라 오래된 `build/extracted` 레이어를 복사하고 있어 `docker compose up --build app`만 실행하면 예전 인증 코드가 계속 배포되는 것이 원인으로 확인됨. Docker 빌드 단계에서 직접 `bootJar`를 실행하도록 멀티스테이지 Dockerfile로 교체.
- **Key Decisions**:
  - `build/extracted` 의존을 제거하고 Docker 빌드 내부에서 `./gradlew bootJar -x test --no-daemon` 실행 — stale artifact로 예전 인증 코드가 배포되는 문제를 구조적으로 방지
  - `.dockerignore`는 Gradle 설정과 소스만 build context에 포함하도록 변경 — `.env`, `.secret.env`가 Docker build context에 포함되지 않게 하여 보안 정책을 강화
- **Affected Files**: <details><summary>2개 파일</summary>

  - **Created**:
    - 없음
  - **Modified**:
    - `backend/Dockerfile` (+13/-6) — 오래된 extracted 레이어 복사 방식에서 최신 소스 기반 멀티스테이지 JAR 빌드 방식으로 변경
    - `backend/.dockerignore` (+9/-8) — `build/extracted` 및 환경 파일 포함을 제거하고 Gradle/소스 파일만 포함하도록 변경
  - **Deleted**:
    - 없음

  </details>

### 사용자 권한 관리 안전장치 보강 (Codex)
- **User Intent**: `시스템 관리 > 사용자 및 권한` 메뉴 구현 전 백엔드부터 작업해 달라는 요청. 관리자 화면에서 사용할 사용자 목록/상세/권한 변경/삭제 API의 운영 안정성을 먼저 확보해야 하는 상황.
- **Agent Context**: 기존 백엔드는 관리자 API는 존재했지만 자기 계정 삭제/권한 강등, 마지막 관리자 삭제/강등을 막는 도메인 규칙이 없었음. 프론트 구현 전 관리자 권한 관리 API가 시스템 관리자 계정을 소실시키지 않도록 서비스 계층에 보호 규칙을 추가.
- **Key Decisions**:
  - 관리자 자기 계정 권한 변경과 삭제를 차단 — `agent/project/backend.md`의 `ADMIN` 사용자 관리 권한 모델에서 관리자 권한 상실로 인한 운영 불능을 방지해야 하기 때문.
  - 마지막 `ADMIN` 계정의 강등과 삭제를 차단 — 사용자 관리 기능이 최소 1개 관리자 계정을 유지해야 프론트 관리자 화면과 후속 운영이 가능하기 때문.
  - 검증 규칙은 Controller가 아닌 Service 계층에 배치 — `agent/project/backend.md`의 API → Domain 계층 분리 규칙에 맞춰 비즈니스 규칙을 도메인 서비스에서 일관되게 적용하기 위함.
- **Affected Files**: <details><summary>6개 파일</summary>

  - **Created**:
    - `backend/src/test/java/com/ssafy/demo_app/domain/user/service/UserServiceTest.java` — 사용자 권한 변경/삭제 보호 정책 단위 테스트
  - **Modified**:
    - `backend/src/main/java/com/ssafy/demo_app/api/user/UserController.java` (+7/-3) — 권한 변경/삭제 요청에 현재 관리자 ID 전달
    - `backend/src/main/java/com/ssafy/demo_app/domain/user/repository/UserRepository.java` (+2/-0) — 관리자 수 집계 메서드 추가
    - `backend/src/main/java/com/ssafy/demo_app/domain/user/service/UserService.java` (+2/-2) — 관리자 ID를 받도록 권한 변경/삭제 시그니처 변경
    - `backend/src/main/java/com/ssafy/demo_app/domain/user/service/UserServiceImpl.java` (+28/-2) — 자기 계정 및 마지막 관리자 보호 규칙 추가
    - `backend/src/main/java/com/ssafy/demo_app/global/exception/ErrorCode.java` (+3/-0) — 사용자 관리 보호 정책 에러 코드 추가
  - **Deleted**:
    - 없음

  </details>
