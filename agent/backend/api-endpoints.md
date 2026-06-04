# Backend API Endpoints

## 기본 정보

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **인증**: `Authorization: Bearer <token>` 헤더 (일부 엔드포인트 제외)
- **Swagger**: `/swagger-ui.html`
- **응답 형식**: `ApiResponse<T>` = `{ success, message, data }`

## 엔드포인트 목록

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

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/boms` | 인증 | BOM 목록/트리 조회 |
| GET | `/api/boms/{id}` | 인증 | BOM 단건 조회 |
| POST | `/api/boms` | ADMIN/MANAGER | BOM 등록 |
| PUT | `/api/boms/{id}` | ADMIN/MANAGER | BOM 수정 |
| DELETE | `/api/boms/{id}` | ADMIN/MANAGER | BOM 삭제 |

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

## 권한 요약

| 역할 | 권한 범위 |
|------|-----------|
| `WORKER` | 기본 CRUD (입고, 출하, 재고, 생산 실적) |
| `MANAGER` | WORKER 권한 + 기준정보 등록/수정 (품목, 거래처, BOM, 라우팅) |
| `ADMIN` | MANAGER 권한 + 사용자 관리 |
