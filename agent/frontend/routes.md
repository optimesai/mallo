# Frontend Routes & Navigation

## 라우트 테이블

### 인증 불필요 (guestOnly)

| 경로 | 이름 | 페이지 |
|------|------|--------|
| `/login` | `login` | `LoginView` |
| `/signup` | `signup` | `SignupView` |

### 인증 필요 (requiresAuth)

`DefaultLayout` (Sidebar + Header) 내에서 렌더링됨.

| 경로 | 이름 | 페이지 | 설명 |
|------|------|--------|------|
| `/` | `home` | `HomeView` | 대시보드 |
| `/me` | `my-info` | `MyInfoView` | 내 정보 |
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

- `requiresAuth`: 로그인 안 되어 있으면 `/login`으로 리다이렉트
- `guestOnly`: 로그인되어 있으면 `/`로 리다이렉트

## 사이드바 메뉴 구성

`router/navigation.ts`의 `menuGroups` 배열에서 정의됨.

| 카테고리 | 메뉴 항목 | 연결 경로 |
|----------|-----------|-----------|
| 시스템 관리 | 사용자 및 권한 | (라우트 미지정) |
| 기준정보 관리 | 품목 마스터 | (라우트 미지정) |
| | 거래처 마스터 | (라우트 미지정) |
| | 공장 및 생산 라인 | (라우트 미지정) |
| | BOM (부품명세서) | (라우트 미지정) |
| 입고 관리 | 입고 등록 | `/inbound/receipt` |
| | 창고 적재 | `/inbound/stack` |
| 재고 관리 | 현재고 현황 | `/inventory/status` |
| | 수불 이력 조회 | `/inventory/history` |
| 생산 관리 | 작업 지시 등록 | (라우트 미지정) |
| | 자재 출고(불출) 처리 | `/production/issue` |
| | 공정 실적 및 원부자재 | (라우트 미지정) |
| 출고 관리 | 출하 지시 | `/shipping/order` |
| | 피킹/상차 | `/shipping/picking` |
| 지능형 분석 서비스 | AI 데이터 챗봇 | (라우트 미지정) |

## 레이아웃 구조

```
App.vue
  └── RouterView
        ├── /login, /signup  → 단독 페이지
        └── / (인증 필요)     → DefaultLayout.vue
              ├── AppSidebar.vue     (좌측 256px, 슬레이트 계열)
              ├── AppHeader.vue      (상단)
              └── RouterView         (컨텐츠 영역)
```

## 주의사항

- `navigation.ts`에 정의된 7개 메뉴 항목이 라우트가 연결되지 않은 상태(`/`). 해당 페이지 컴포넌트가 아직 생성되지 않음.
- 라우트는 `router/index.ts`에 정의되고, 메뉴는 `router/navigation.ts`에 별도 정의됨 — 신규 페이지 추가 시 양쪽 모두 업데이트 필요.
