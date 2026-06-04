# Backend Security

## 개요

- **방식**: Stateless JWT 인증
- **세션**: `SessionCreationPolicy.STATELESS` (서버 세션 없음)
- **패스워드**: BCrypt 인코딩

## JWT 인증 흐름

1. 클라이언트: `POST /api/auth/login` → 서버가 `{ token: { accessToken }, user: {...} }` 반환
2. 클라이언트: 이후 모든 요청에 `Authorization: Bearer <token>` 헤더 첨부
3. `JwtAuthenticationFilter` (`OncePerRequestFilter`)
   - `Authorization` 헤더에서 Bearer 토큰 추출
   - 토큰 검증 후 `SecurityContextHolder`에 인증 정보 설정
   - 예외: `/api/auth/**`, `OPTIONS` 요청은 필터 스킵
4. 인증 실패 시 `CustomAuthenticationEntryPoint` (401) / `CustomAccessDeniedHandler` (403)

## SecurityConfig 규칙 (우선순위 순)

| 규칙 | 대상 | 권한 |
|------|------|------|
| OPTIONS 요청 | `/**` | 모두 허용 |
| 인증 예외 | `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**` | 모두 허용 |
| 내 정보 | `GET/PATCH /api/users/me` | 인증 |
| 사용자 관리 | `/api/users/**` | ADMIN |
| BOM 조회 | `GET /api/boms/**` | 인증 |
| BOM 변경 | `/api/boms/**` | ADMIN, MANAGER |
| 기타 모든 요청 | `/**` | 인증 |

## JWT 토큰 상세

- **라이브러리**: jjwt 0.12.6
- **Provider**: `JwtTokenProvider` — 토큰 생성, 검증, userId 추출
- **토큰 페이로드**: `userId` 포함
- **CustomUserDetailsService**: `userId`로 DB에서 사용자 조회 → `UserDetails` 반환

## 에러 코드 (인증 관련)

| ErrorCode | HTTP Status | 메시지 |
|-----------|-------------|--------|
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `ACCESS_DENIED` | 403 | 접근 권한이 없습니다. |
| `INVALID_PASSWORD` | 401 | 비밀번호가 일치하지 않습니다. |

## 주의사항

- CSRF 보호 비활성화 (`AbstractHttpConfigurer::disable`), API 전용 서버이므로 정상
- CORS 허용 (커스텀 `WebCorsConfig`가 `:lib:web-starter`에 위치)
- `JwtAuthenticationFilter`가 `UsernamePasswordAuthenticationFilter` 전에 실행됨
