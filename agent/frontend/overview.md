# Frontend Overview

## 기술 스택

| 영역 | 기술 |
|------|------|
| 프레임워크 | Vue 3.5 (Composition API, `<script setup>`) |
| 언어 | TypeScript |
| 빌드 | Vite |
| 상태 관리 | Pinia |
| 라우터 | Vue Router |
| HTTP | Axios |
| CSS | Tailwind CSS 4 |
| 아이콘 | Lucide Vue |
| 패키지 매니저 | npm |

## 디렉토리 구조

```
frontend/
├── index.html
├── package.json
├── vite.config.ts              # Vite 설정 (프록시, alias)
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
    │       ├── AppSidebar.vue  # 좌측 네비게이션
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

## 코드 컨벤션

- 들여쓰기: 스페이스 2칸
- Vue: Composition API + `<script setup lang="ts">`
- 타입 정의: `interface` 사용, api 파일에 위치
- import alias: `@/` → `src/`
- 컴포넌트: PascalCase 파일명
- TypeScript 파일: camelCase 파일명

## Vite 개발 서버

- 프록시: `/api/*` → `http://localhost:8080`
- 실행: `npm run dev`

## 토큰 키 상수

프론트엔드에서 사용하는 localStorage 키:

| 키 | 용도 | 참조 위치 |
|----|------|-----------|
| `ssafy-pjt-access-token` | JWT 액세스 토큰 | `authStore.ts`, `inboundApi.ts` |
| `ssafy-pjt-auth-user` | 로그인 사용자 정보 (JSON) | `authStore.ts` |
