# Backend

> 백엔드 기술 스택, 모듈 구조, 계층 아키텍처, API, 보안.

---

# 기술 스택

| 영역 | 기술 |
|------|------|
| 런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.0.6 |
| 빌드 | Gradle (멀티모듈: `:lib:web-starter`) |
| ORM | Spring Data JPA (Hibernate) |
| DB | MySQL 8.0 (Docker, port 3316), H2 (테스트) |
| 보안 | Spring Security + JWT (jjwt 0.12.6) |
| API 문서 | SpringDoc OpenAPI 2.8.5 (Swagger) |

---

# 모듈 구조

횡단 관심사는 `lib/` 아래 인터페이스 모듈(`-core`)과 구현 모듈(`-starter`)로 분리한다:

- **-core**: 순수 인터페이스, POJO. Spring 의존 없음. 다른 -core 모듈만 의존.
- **-starter**: Auto-Configuration, Bean 등록. 대응 -core + Spring Boot 의존.

실제 구조:
```
rootProject: demo-app
├── :lib:web-starter   # WebProperties, SwaggerStarterConfig, WebCorsConfig
└── (main)             # 메인 애플리케이션
```

---

# 계층 아키텍처

## 4계층

| 계층 | 책임 |
|------|------|
| **API** | HTTP 요청 수신, 입력 검증, 응답 직렬화 |
| **Domain** | 비즈니스 로직, 도메인 규칙, 데이터 접근 |
| **Global** | BaseEntity, 공통 응답 래퍼, 전역 핸들러, 웹/CORS 설정 |
| **Infrastructure** | 인증 필터, 토큰 프로바이더, 외부 연동 |

계층을 건너뛰거나 우회하는 것은 금지된다.

## 의존성 방향

```
API ──→ Domain ←── Infrastructure
          ↓
        Global
```

- API → Domain. 역방향 금지.
- Infrastructure → Domain. 역방향 금지.
- Global은 모든 계층에서 참조 가능.
- Domain은 외부 계층에 의존하지 않는다.

## 패키지 구조

```
com.ssafy.demo_app
├── api/{domain}/           # Controller + 전용 DTO
│   ├── {Domain}Api.java    # Swagger 인터페이스 (선택적)
│   ├── {Domain}Controller.java
│   └── dto/                # Request/Response DTO
├── domain/{domain}/
│   ├── entity/             # JPA Entity
│   ├── repository/         # Spring Data Repository
│   └── service/            # Service Interface + Impl
├── global/                 # 공통 모듈
│   ├── common/             # BaseTimeEntity, BaseCreatedTimeEntity
│   ├── config/             # JpaConfig
│   ├── exception/          # ErrorCode(enum), BusinessException, GlobalExceptionHandler
│   └── response/           # ApiResponse<T>
└── infrastructure/security/
    ├── config/             # SecurityConfig
    ├── details/            # CustomUserDetails, CustomUserDetailsService
    ├── handler/            # CustomAccessDeniedHandler, CustomAuthenticationEntryPoint
    └── jwt/                # JwtTokenProvider, JwtAuthenticationFilter
```

---

# 코드 컨벤션

- **실제 코드 우선**: 이 문서의 규칙과 실제 코드가 다를 경우 실제 코드를 정답으로 간주한다. 문서 오류 발견 시 작업과 함께 수정한다.

## Java

| 규칙 | 설명 |
|------|------|
| 들여쓰기 | 스페이스 4칸 |
| Lombok | `@Getter`/`@Setter`만 사용. `@Builder`/`@AllArgsConstructor` 절대 금지 |
| Service | Interface + Impl 패턴 (예: `InventoryService` + `InventoryServiceImpl`) |
| 생성자 주입 | `@RequiredArgsConstructor` 사용 |
| 트랜잭션 | 클래스 레벨 `@Transactional(readOnly=true)`, 쓰기 메서드에만 `@Transactional` |

## Entity

| 규칙 | 설명 |
|------|------|
| BaseEntity | `BaseTimeEntity`(createdAt+updatedAt) 또는 `BaseCreatedTimeEntity`(createdAt only) 상속 |
| FetchType | 모든 연관관계 `FetchType.LAZY`, `EAGER` 절대 금지 |
| 관계 매핑 | `@ManyToOne` 중심, `@OneToMany` 사용 금지 |
| ID 생성 | `GenerationType.IDENTITY` |
| Enum 매핑 | `@Enumerated(EnumType.STRING)`, `ORDINAL` 절대 금지 |
| 객체 생성 | `new Entity()` 후 `setXxx()` 패턴. `@Builder` 사용 금지 |

---

# API 응답 형식

모든 API 응답은 `ApiResponse<T>`로 감싼다.

```json
{
  "success": true,
  "message": "success",
  "data": { ... }
}
```

- `ApiResponse.success(data)` — 성공 + 데이터
- `ApiResponse.success(message, data)` — 성공 + 메시지 + 데이터
- `ApiResponse.success(message)` — 성공 + 메시지 (데이터 없음)
- `ApiResponse.fail(message)` — 실패

에러는 `BusinessException(ErrorCode)` → `GlobalExceptionHandler`가 `ApiResponse.fail()`로 변환.

---

# API 설계

## URL 규칙

| 규칙 | 설명 |
|------|------|
| **복수형 명사** | `/api/users`, `/api/items` |
| **소문자 + 케밥** | camelCase, snake_case 금지 |
| **Restful** | 리소스 행위는 HTTP 메서드로만 표현. 비즈니스 액션은 예외적으로 `PATCH /api/inbounds/{id}/complete` 허용 |

## HTTP 메서드

| 메서드 | 용도 |
|--------|------|
| `GET` | 목록/단건 조회 |
| `POST` | 신규 생성 |
| `PUT` | 전체 수정 |
| `PATCH` | 부분 수정, 상태 변경 |
| `DELETE` | 삭제 |

---

# 보안

## 인증 흐름

1. `POST /api/auth/login` → `{ token: { accessToken }, user: {...} }` 반환
2. 이후 요청: `Authorization: Bearer <token>` 헤더
3. `JwtAuthenticationFilter`(`OncePerRequestFilter`)에서 토큰 검증 → `SecurityContextHolder` 설정
4. 인증 실패: `CustomAuthenticationEntryPoint`(401) / `CustomAccessDeniedHandler`(403)

## 보안 설정 우선순위

| 우선순위 | 대상 | 권한 |
|----------|------|------|
| 1 | CORS preflight (`OPTIONS /**`) | 모두 허용 |
| 2 | 공개 엔드포인트 (`/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`) | 모두 허용 |
| 3 | 내 정보 (`GET/PATCH /api/users/me`) | 인증 |
| 4 | 사용자 관리 (`/api/users/**`) | ADMIN |
| 5 | BOM 조회 (`GET /api/boms/**`) | 인증 |
| 6 | BOM 등록/수정/상태 변경 (`POST/PUT/PATCH /api/boms/**`) | ADMIN, MANAGER |
| 7 | BOM 삭제/비활성화 (`DELETE /api/boms/**`) | ADMIN |
| 8 | 기본 (`/**`) | 인증 |

## 토큰

| 컴포넌트 | 책임 |
|----------|------|
| `JwtTokenProvider` | 토큰 생성, 검증, userId 추출 |
| `JwtAuthenticationFilter` | 요청마다 토큰 검증 및 사용자 컨텍스트 설정 |
| `CustomUserDetailsService` | userId로 DB에서 사용자 조회 → `UserDetails` 반환 |

- 라이브러리: jjwt 0.12.6
- 세션: `SessionCreationPolicy.STATELESS` (서버 세션 없음)
- 패스워드: BCrypt 인코딩
- CSRF: 비활성화 (API 전용 서버)

---

# 권한 체계

| 역할 | 권한 범위 |
|------|-----------|
| `WORKER` | 기본 CRUD (입고, 출하, 재고, 생산 실적) |
| `MANAGER` | WORKER 권한 + 기준정보 등록/수정 (품목, 거래처, BOM, 라우팅) |
| `ADMIN` | MANAGER 권한 + 사용자 관리 |

---

# 도메인 목록

| 도메인 디렉토리 | 설명 | 주요 Entity |
|-----------------|------|-------------|
| `auth` | 인증 | `User` |
| `user` | 사용자 관리 | `User` |
| `item` | 품목 마스터 | `ItemMaster` |
| `partner` | 거래처 마스터 | `PartnerMaster` |
| `bom` | BOM(부품명세서) | `BomStructure` |
| `routing` | 공장/라인/공정 라우팅 | `FactoryRouting` |
| `production` | 생산 관리 | `WorkOrder`, `ProductionExecution` |
| `inventory` | 재고/입고/로케이션 | `InboundReceipt`, `CurrentInventory`, `WarehouseLocation`, `InventoryTransactionHistory` |
| `shipping` | 출하 관리 | `OutboundShipping` |
| `ai` | AI 기능 (스켈레톤) | `AiQueryHistory`, `DynamicBatchSchedule` |

---

# 엔티티 관계도

```
User ──┬── InboundReceipt (worker)
       ├── OutboundShipping (worker)
       └── InventoryTransactionHistory (worker)

ItemMaster ──┬── BomStructure (parent/child)
              ├── InboundReceipt
              ├── OutboundShipping
              ├── WorkOrder
              ├── CurrentInventory
              └── InventoryTransactionHistory

PartnerMaster ──┬── InboundReceipt
                └── OutboundShipping

FactoryRouting ─── WorkOrder ─── ProductionExecution

WarehouseLocation ──┬── InboundReceipt
                    ├── OutboundShipping (picking)
                    ├── CurrentInventory
                    └── InventoryTransactionHistory
```

---

# 엔드포인트 목록

### 인증 (`/api/auth`)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/auth/signup` | 불필요 | 회원가입 |
| POST | `/api/auth/login` | 불필요 | 로그인 → JWT 발급 |

### 사용자 (`/api/users`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/users/me` | 인증 | 내 정보 조회 |
| PATCH | `/api/users/me` | 인증 | 내 정보 수정 |
| CRUD | `/api/users/**` | ADMIN | 사용자 관리 |

### 품목 (`/api/items`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/items` | 인증 | 품목 목록 (필터: `itemType`, `keyword`) |
| GET | `/api/items/{id}` | 인증 | 품목 단건 조회 |
| POST | `/api/items` | MANAGER/ADMIN | 품목 등록 |
| PUT | `/api/items/{id}` | MANAGER/ADMIN | 품목 수정 |
| DELETE | `/api/items/{id}` | MANAGER/ADMIN | 품목 삭제 |

### 거래처 (`/api/partners`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/partners` | 인증 | 거래처 목록 (필터: `partnerType`, `keyword`) |
| GET | `/api/partners/{id}` | 인증 | 거래처 단건 조회 |
| POST | `/api/partners` | MANAGER/ADMIN | 거래처 등록 |
| PUT | `/api/partners/{id}` | MANAGER/ADMIN | 거래처 수정 |
| DELETE | `/api/partners/{id}` | MANAGER/ADMIN | 거래처 삭제 |

### BOM (`/api/boms`)

- BOM 소요량(`quantity`)은 단위(`ea`, `kg`, `box`, `l`, `m`)와 무관하게 `Integer` 정수로만 입력/저장/계산한다.
- 작업지시 목표 수량, 생산 실적 수량, 재고/불출 수량도 정수 기준으로 계산한다.

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/boms/groups` | 인증 | 상위 품목 + BOM 버전 기준 그룹 목록 페이지 조회 |
| GET | `/api/boms/groups/{parentItemId}?bomVersion={version}` | 인증 | BOM 그룹 상세 구성 품목 조회 |
| GET | `/api/boms` | 인증 | BOM 목록/트리 조회 |
| GET | `/api/boms/{id}` | 인증 | BOM 단건 조회 |
| POST | `/api/boms` | ADMIN/MANAGER | BOM 등록 |
| POST | `/api/boms/bulk` | ADMIN/MANAGER | 상위 품목 1개 + 구성 품목 다건 BOM 일괄 등록 |
| PUT | `/api/boms/{id}` | ADMIN/MANAGER | BOM 수정 |
| PATCH | `/api/boms/{id}/status` | ADMIN/MANAGER | BOM 상태 변경 |
| DELETE | `/api/boms/{id}` | ADMIN | BOM 비활성화 |

### 입고 (`/api/inbounds`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/inbounds` | 인증 | 입고 목록 |
| GET | `/api/inbounds/{id}` | 인증 | 입고 단건 |
| POST | `/api/inbounds` | 인증 | 입고 등록 |
| PUT | `/api/inbounds/{id}/complete` | 인증 | 입고 완료 처리 |
| POST | `/api/inbounds/{id}/stack` | 인증 | 창고 적재 |
| DELETE | `/api/inbounds/{id}` | 인증 | 입고 삭제 |

### 로케이션 (`/api/locations`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/locations` | 인증 | 로케이션 목록 |
| GET | `/api/locations/{id}` | 인증 | 로케이션 단건 |
| POST | `/api/locations` | 인증 | 로케이션 등록 |
| PUT | `/api/locations/{id}` | 인증 | 로케이션 수정 |
| DELETE | `/api/locations/{id}` | 인증 | 로케이션 삭제 |

### 재고 (`/api/inventories`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/inventories` | 인증 | 현재고 목록 |
| GET | `/api/inventories/{id}` | 인증 | 현재고 단건 |

### 수불 이력 (`/api/transaction-histories`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/transaction-histories` | 인증 | 수불 이력 목록 |

### 작업 지시 (`/api/work-orders`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/work-orders` | 인증 | 작업 지시 목록 |
| GET | `/api/work-orders/{id}` | 인증 | 작업 지시 상세 |
| POST | `/api/work-orders` | 인증 | 작업 지시 등록 |
| PUT | `/api/work-orders/{id}` | 인증 | 작업 지시 수정 |
| PATCH | `/api/work-orders/{id}/status` | 인증 | 상태 변경 |
| PATCH | `/api/work-orders/{id}/close` | 인증 | 작업 지시 마감 |
| GET | `/api/work-orders/{id}/material-requirements` | 인증 | 소요 자재 조회 |

### 생산 실적 (`/api/production-executions`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/production-executions` | 인증 | 생산 실적 목록 |
| GET | `/api/production-executions/{id}` | 인증 | 생산 실적 단건 |
| POST | `/api/production-executions` | 인증 | 생산 실적 등록 |

### 출하 (`/api/shippings`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/shippings` | 인증 | 출하 목록 |
| GET | `/api/shippings/{id}` | 인증 | 출하 단건 |
| POST | `/api/shippings` | 인증 | 출하 등록 |
| POST | `/api/shippings/{id}/picking` | 인증 | 피킹 할당 |
| PUT | `/api/shippings/{id}/ship` | 인증 | 상차 처리 |

### 공장 라우팅 (`/api/routings`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/routings` | 인증 | 라우팅 목록/트리 |
| GET | `/api/routings/{id}` | 인증 | 라우팅 단건 |
| POST | `/api/routings` | 인증 | 라우팅 등록 |
| PUT | `/api/routings/{id}` | 인증 | 라우팅 수정 |
| DELETE | `/api/routings/{id}` | 인증 | 라우팅 삭제 |
