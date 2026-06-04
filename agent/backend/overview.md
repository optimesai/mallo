# Backend Overview

## 기술 스택

| 영역 | 기술 |
|------|------|
| 런타임 | Java 21 |
| 프레임워크 | Spring Boot 4.0.6 |
| 보안 | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA (Hibernate) |
| DB | MySQL 8.0 (Docker, port 3316), H2 (테스트) |
| API 문서 | SpringDoc OpenAPI 2.8.5 (Swagger) |
| 빌드 | Gradle, 멀티모듈 (`:lib:web-starter`) |
| 배포 | Docker Compose (`backend/docker-compose.yml`) |

## 멀티모듈 구조

```
rootProject: demo-app
├── :lib:web-starter   # 내부 라이브러리 (WebProperties, SwaggerStarterConfig, WebCorsConfig)
└── (main)             # 메인 애플리케이션
```

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

- `api/` 계층은 도메인별로 분리 (예: `api/inventory/`, `api/production/`)
- `domain/` 계층도 동일한 도메인 단위로 분리
- API 인터페이스(`{Domain}Api.java`)는 Swagger 문서화용으로 일부 도메인만 사용

## API 응답 형식

모든 API 응답은 `ApiResponse<T>`로 래핑된다:

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

## 코드 컨벤션

### Java

- 들여쓰기: 탭
- Lombok `@Getter`/`@Setter` 사용, `@Builder`/`@AllArgsConstructor`는 사용하지 않음
- Service는 Interface + Impl 패턴 (예: `InventoryService` + `InventoryServiceImpl`)
- 생성자 주입 (`@RequiredArgsConstructor`)
- 트랜잭션: 클래스 레벨 `@Transactional(readOnly=true)`, 쓰기 메서드에만 `@Transactional`

### Entity

- `BaseTimeEntity` (createdAt + updatedAt) 또는 `BaseCreatedTimeEntity` (createdAt only) 상속
- 모든 연관관계 `FetchType.LAZY`
- `@ManyToOne` 중심, `@OneToMany`는 사용하지 않음
- ID 생성: `GenerationType.IDENTITY`
- Enum: `@Enumerated(EnumType.STRING)`
- Setter 기반 생성: `new Entity()` 후 `setXxx()` 패턴

### 테스트

- 테스트 위치: `backend/src/test/java/...`
- 테스트 DB: H2 인메모리 (의존성: `testRuntimeOnly 'com.h2database:h2'`)

## 도메인 목록

| 도메인 디렉토리 | 설명 | Entity |
|-----------------|------|--------|
| `auth` | 인증 | (User 엔티티 사용) |
| `user` | 사용자 관리 | `User` |
| `item` | 품목 마스터 | `ItemMaster` |
| `partner` | 거래처 마스터 | `PartnerMaster` |
| `bom` | BOM(부품명세서) | `BomStructure` |
| `routing` | 공장/라인/공정 라우팅 | `FactoryRouting` |
| `production` | 생산 관리 | `WorkOrder`, `ProductionExecution` |
| `inventory` | 재고/입고/로케이션 | `InboundReceipt`, `CurrentInventory`, `WarehouseLocation`, `InventoryTransactionHistory` |
| `shipping` | 출하 관리 | `OutboundShipping` |
| `ai` | AI 기능 (스켈레톤) | `AiQueryHistory`, `DynamicBatchSchedule` |
