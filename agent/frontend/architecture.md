# Frontend Layer Architecture

## 계층 구조

```
View (views/*.vue)
  → Store (state/*Store.ts)         // Pinia: 상태 관리 + 액션
     → Service (services/*.ts)      // 에러 변환, 비즈니스 로직
        → API (api/*.ts)            // axios HTTP 호출
```

## 각 계층의 역할

### API 계층 (`api/*.ts`)

- **책임**: axios HTTP 호출만 수행
- **패턴**:
  - `getAuthHeaders()` 함수로 `Authorization: Bearer <token>` 헤더 생성
  - 서버 응답 타입을 그대로 반환 (`ApiResponse<T>`)
  - `ApiResponse<T>` 제네릭 타입은 `authApi.ts`에 정의됨
  - 도메인별로 파일 분리 (단, `inboundApi.ts`에 품목/거래처/로케이션 API도 혼재)

```typescript
// 예시 (inboundApi.ts)
function getAuthHeaders() {
  const token = localStorage.getItem('ssafy-pjt-access-token')
  return { Authorization: `Bearer ${token}` }
}

export const inboundApi = {
  async getInbounds() {
    const response = await axios.get<ApiResponse<InboundReceiptResponse[]>>('/api/inbounds', {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
```

### Service 계층 (`services/*.ts`)

- **책임**: 비즈니스 로직 + 에러 변환
- **패턴**:
  - API 계층 함수 호출 → `response.data` 추출 반환
  - `AxiosError`를 `Error`로 변환, 서버 메시지를 한글 폴백과 함께 제공

```typescript
// 예시 (inboundService.ts)
export const inboundService = {
  async getInbounds(): Promise<InboundReceiptResponse[]> {
    try {
      const response = await inboundApi.getInbounds()
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '입고 목록을 불러오지 못했습니다.')
      }
      throw new Error('입고 목록을 불러오지 못했습니다.')
    }
  }
}
```

### Store 계층 (`state/*Store.ts`)

- **책임**: Pinia 상태 관리, 비동기 액션, 로딩/에러 상태
- **패턴**:
  - Composition API 스타일 (`defineStore` + `setup` 함수)
  - `ref`로 상태 관리: 데이터 목록, `isLoading`, `error`
  - 액션 메서드가 Service 계층 호출 → 상태 업데이트
  - 에러는 `error.value`에 저장하고 `throw`로 전파

```typescript
// 예시 (inboundStore.ts)
export const useInboundStore = defineStore('inbound', () => {
  const inbounds = ref<InboundReceiptResponse[]>([])
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  async function loadInbounds() {
    isLoading.value = true
    error.value = null
    try {
      inbounds.value = await inboundService.getInbounds()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '입고 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  return { inbounds, isLoading, error, loadInbounds }
})
```

### View 계층 (`views/*.vue`)

- **책임**: UI 렌더링, 사용자 인터랙션
- **패턴**:
  - `<script setup lang="ts">` 사용
  - Store의 상태와 액션을 직접 사용
  - `onMounted`에서 데이터 로딩

## 인증 흐름 상세

### 로그인

```
LoginView → authStore.login(employeeNo, password)
  → authService.login(request)
    → authApi.login(request)       // POST /api/auth/login
  ← { token: { accessToken }, user: { ... } }
  → accessToken, user → ref
  → localStorage 저장 (키: ssafy-pjt-access-token, ssafy-pjt-auth-user)
```

### 인증 상태 유지

- 페이지 새로고침 시: `authStore` 초기화 시 `localStorage`에서 토큰/사용자 복원
- API 호출 시: 각 api 함수에서 `getAuthHeaders()`로 토큰 첨부
- 라우트 가드: `router.beforeEach`에서 `requiresAuth` / `guestOnly` 확인

### 로그아웃

```
authStore.logout()
  → accessToken = null, user = null
  → localStorage.removeItem(...)
```

## 주의사항

- **전역 axios 인터셉터 없음**: 토큰 첨부가 각 API 함수에 분산되어 있음. `inboundApi.ts`의 `getAuthHeaders()`와 다른 api 파일들의 구현을 확인 필요.
- **`inboundApi.ts` 역할 혼재**: 입고 API 외에 `/api/items`, `/api/partners`, `/api/locations` 호출도 포함되어 있어, 도메인 분리가 완전하지 않음.
- **타입 공유**: API 응답 타입(`InboundReceiptResponse` 등)이 api 계층에 정의되고, Service/Store 계층에서 재사용됨.
