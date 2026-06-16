# Frontend

> 프론트엔드 기술 스택, 계층 아키텍처, 라우트, 인증.

---

# 기술 스택

| 영역 | 기술 |
|------|------|
| 프레임워크 | Vue 3.5 (Composition API, `<script setup>`) |
| 언어 | TypeScript |
| 빌드 | Vite |
| 상태 관리 | Pinia |
| 라우터 | Vue Router |
| HTTP 클라이언트 | Axios |
| 스타일링 | Tailwind CSS 4 |
| 아이콘 | Lucide Vue |
| 패키지 매니저 | npm |

---

# 코드 컨벤션

- **실제 코드 우선**: 이 문서의 규칙과 실제 코드가 다를 경우 실제 코드를 정답으로 간주한다. 문서 오류 발견 시 작업과 함께 수정한다.

| 규칙 | 설명 |
|------|------|
| 들여쓰기 | 스페이스 2칸 |
| Vue 컴포넌트 | Composition API + `<script setup lang="ts">`만 사용. Options API 절대 금지 |
| 컴포넌트 파일명 | PascalCase (예: `UserList.vue`, `DataTable.vue`) |
| TypeScript 파일명 | camelCase (예: `userApi.ts`, `authService.ts`) |
| 타입 정의 | `interface` 사용, API 관련 타입은 해당 api 파일에 위치 |
| import alias | `@/` → `src/` |
| Store | Composition API 스타일 `defineStore` + `setup` 함수 |

---

# 디렉토리 구조

```
frontend/
├── index.html
├── package.json
├── vite.config.ts              # Vite 설정 (프록시: /api/* → localhost:8080, alias)
├── tsconfig.json
├── env.d.ts
└── src/
    ├── main.ts                 # 앱 진입점 (createApp → Pinia → Router)
    ├── main.css                # 전역 스타일 (Tailwind)
    ├── App.vue                 # 루트 컴포넌트
    ├── api/                    # axios 호출 계층
    │   ├── authApi.ts
    │   ├── inboundApi.ts       # 입고 + 품목/거래처/로케이션 API 포함
    │   ├── inventoryApi.ts
    │   ├── shippingApi.ts
    │   ├── userApi.ts
    │   ├── workOrderApi.ts
    │   └── mockApi.ts
    ├── services/               # 비즈니스 로직 + 에러 핸들링
    │   ├── authService.ts
    │   ├── inboundService.ts
    │   ├── inventoryService.ts
    │   ├── shippingService.ts
    │   ├── userService.ts
    │   ├── workOrderService.ts
    │   └── mockService.ts
    ├── state/                  # Pinia stores
    │   ├── authStore.ts
    │   ├── inboundStore.ts
    │   ├── inventoryStore.ts
    │   ├── shippingStore.ts
    │   ├── workOrderStore.ts
    │   └── mockStore.ts
    ├── router/
    │   ├── index.ts            # 라우트 정의 + 내비게이션 가드
    │   └── navigation.ts       # 사이드바 메뉴 구성
    ├── layouts/
    │   ├── DefaultLayout.vue   # 메인 레이아웃 (Sidebar + Header + Content)
    │   └── components/
    │       ├── AppSidebar.vue  # 좌측 네비게이션 (256px)
    │       └── AppHeader.vue   # 상단 헤더
    ├── views/                  # 페이지 컴포넌트
    │   ├── HomeView.vue
    │   ├── LoginView.vue
    │   ├── SignupView.vue
    │   ├── MyInfoView.vue
    │   ├── InboundReceiptView.vue
    │   ├── InboundStackView.vue
    │   ├── InventoryStatusView.vue
    │   ├── InventoryHistoryView.vue
    │   ├── MaterialIssueView.vue
    │   ├── ShippingOrderView.vue
    │   └── PickingView.vue
    ├── ui/                     # 공통 재사용 UI 컴포넌트
    │   ├── DataTable.vue
    │   └── StatsCard.vue
    └── assets/                 # 정적 파일
        └── logo.svg
```

---

# 계층 아키텍처

## 4계층

모든 도메인 로직은 다음 4계층을 통해 흘러야 한다. 계층을 건너뛰거나 우회해서는 절대 안 된다.

```
View (views/*.vue)
  → Store (state/*Store.ts)
    → Service (services/*.ts)
      → API (api/*.ts)
```

## 각 계층의 책임

| 계층 | 책임 | 금지 |
|------|------|------|
| **API** | axios HTTP 호출만 수행. 서버 응답을 `ApiResponse<T>`로 반환. 인증 헤더는 `getAuthHeaders()` 공통 함수로 생성. | 비즈니스 로직, 에러 처리, 직접 DOM 조작 |
| **Service** | API 응답 가공, `AxiosError` → `Error` 변환. 한글 폴백 메시지 제공. | HTTP 직접 호출, 상태 관리 |
| **Store** | Pinia 전역 상태, 비동기 액션, 로딩/에러 상태 관리. `ref`로 상태 관리. | HTTP 직접 호출, localStorage 직접 접근 |
| **View** | UI 렌더링, 사용자 인터랙션 처리. `onMounted`에서 데이터 로딩. | Store 우회, API/Service 직접 호출, 비즈니스 로직 |

## 타입 공유

API 응답 타입(`InboundReceiptResponse` 등)은 api 계층에 정의하고, Service/Store 계층에서 재사용.

---

# 라우트

## 설계 규칙

| 규칙 | 설명 |
|------|------|
| **소문자 + 케밥** | camelCase, PascalCase 금지 |
| **계층 구조** | `/`로 리소스 계층 표현 (예: `/inventory/history`) |
| **명사 기반** | URL은 리소스 명사로 구성. 동사 금지 |

## 라우트 테이블

### 인증 불필요 (guestOnly)

| 경로 | 이름 | 페이지 |
|------|------|--------|
| `/login` | `login` | `LoginView` |
| `/signup` | `signup` | `SignupView` |

### 인증 필요 (requiresAuth)

`DefaultLayout` (Sidebar + Header) 내에서 렌더링.

| 경로 | 이름 | 페이지 | 설명 |
|------|------|--------|------|
| `/` | `home` | `HomeView` | 대시보드 |
| `/me` | `my-info` | `MyInfoView` | 내 정보 |
| `/master/boms` | `bom-master` | `BomMasterView` | BOM 그룹 목록 |
| `/master/boms/:parentItemId` | `bom-master-detail` | `BomMasterDetailView` | BOM 그룹 상세 |
| `/inbound/receipt` | `inbound-receipt` | `InboundReceiptView` | 입고 등록 |
| `/inbound/stack` | `inbound-stack` | `InboundStackView` | 창고 적재 |
| `/inventory/status` | `inventory-status` | `InventoryStatusView` | 현재고 현황 |
| `/inventory/history` | `inventory-history` | `InventoryHistoryView` | 수불 이력 조회 |
| `/production/issue` | `production-issue` | `MaterialIssueView` | 자재 불출 처리 |
| `/shipping/order` | `shipping-order` | `ShippingOrderView` | 출하 지시 |
| `/shipping/picking` | `shipping-picking` | `PickingView` | 피킹/상차 |

## 내비게이션 가드

```typescript
router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) return { name: 'login' }
  if (to.meta.guestOnly && authStore.isLoggedIn) return { name: 'home' }
  return true
})
```

## 사이드바 메뉴 구성

`router/navigation.ts`의 `menuGroups` 배열에서 정의.

| 카테고리 | 메뉴 항목 | 연결 경로 |
|----------|-----------|-----------|
| 시스템 관리 | 사용자 및 권한 | (라우트 미지정) |
| 기준정보 관리 | 품목 마스터, 거래처 마스터, 공장·라인, BOM | (라우트 미지정) |
| 입고 관리 | 입고 등록, 창고 적재 | `/inbound/receipt`, `/inbound/stack` |
| 재고 관리 | 현재고 현황, 수불 이력 조회 | `/inventory/status`, `/inventory/history` |
| 생산 관리 | 작업 지시, 자재 불출, 공정 실적 | `/production/issue` (일부 미지정) |
| 출고 관리 | 출하 지시, 피킹/상차 | `/shipping/order`, `/shipping/picking` |
| AI 분석 | AI 데이터 챗봇 | (라우트 미지정) |

> `navigation.ts`의 7개 메뉴 항목이 아직 라우트 연결되지 않음. 신규 페이지 추가 시 `router/index.ts` + `navigation.ts` 양쪽 업데이트 필수.

## 레이아웃 구조

```
App.vue
  └── RouterView
        ├── /login, /signup  → 단독 페이지 (레이아웃 없음)
        └── / (인증 필요)     → DefaultLayout.vue
              ├── AppSidebar.vue     (좌측 256px)
              ├── AppHeader.vue      (상단)
              └── RouterView         (컨텐츠 영역)
```

---

# 인증 흐름

## 로그인

```
LoginView → authStore.login(employeeNo, password)
  → authService.login(request)
    → authApi.login(request)       // POST /api/auth/login
  ← { token: { accessToken }, user: { ... } }
  → accessToken, user → ref
  → localStorage 저장 (키: ssafy-pjt-access-token, ssafy-pjt-auth-user)
```

## 인증 상태 유지

- 페이지 새로고침: `authStore` 초기화 시 `localStorage`에서 토큰/사용자 복원
- API 호출: 각 api 함수에서 `getAuthHeaders()`로 토큰 첨부
- 라우트 가드: `router.beforeEach`에서 `requiresAuth` / `guestOnly` 확인

## 로그아웃

```
authStore.logout()
  → accessToken = null, user = null
  → localStorage.removeItem(...)
```

---

# 토큰 스토리지

localStorage 키는 한 곳에서 중앙 관리한다:

| 키 | 용도 |
|----|------|
| `ssafy-pjt-access-token` | JWT 액세스 토큰 |
| `ssafy-pjt-auth-user` | 로그인 사용자 정보 (JSON) |

---

# 주의사항

- **전역 axios 인터셉터 없음**: 토큰 첨부가 각 API 파일의 `getAuthHeaders()`에 분산되어 있음. 모든 API 파일이 동일한 패턴을 따라야 한다.
- **`inboundApi.ts` 역할 혼재**: 입고 API 외에 `/api/items`, `/api/partners`, `/api/locations` 호출도 포함. 도메인 분리가 완전하지 않음.
- **localStorage 직접 접근 금지**: View나 Service 계층에서 localStorage에 접근해서는 안 된다. authStore로 제한.
