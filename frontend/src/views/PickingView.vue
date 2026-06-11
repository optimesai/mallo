<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  Truck,
  Search,
  RefreshCw,
  SlidersHorizontal,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  ChevronDown,
  MapPin,
  Package,
  Calendar,
  User,
  ClipboardList,
  ArrowRight
} from '@lucide/vue'
import { useShippingStore } from '@/state/shippingStore'

const shippingStore = useShippingStore()

// State
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSearchExpanded = ref(true)
const isSubmitting = ref(false)
const selectedShipping = ref<any>(null)

// Detail action state
const vehicleInput = ref('')

// Filter states
const filterShippingNo = ref('')
const filterItemName = ref('')
const filterStatus = ref<'WORK' | 'READY' | 'PICKING'>('WORK')

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await shippingStore.loadShippings({
      page: shippingStore.page,
      size: 20,
      status: filterStatus.value !== 'WORK' ? filterStatus.value : undefined,
      keyword: filterShippingNo.value || filterItemName.value || undefined,
    })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '출하 데이터를 불러오는데 실패했습니다.'
  }
}

function goToPage(page: number) {
  shippingStore.page = page
  fetchPageData()
}

function showToast(msg: string) {
  successToast.value = msg
  setTimeout(() => {
    successToast.value = null
  }, 4000)
}

async function handleRefresh() {
  await fetchPageData()
  showToast('피킹/상차 작업 목록이 새로고침되었습니다.')
  selectedShipping.value = null
}

function resetFilters() {
  filterShippingNo.value = ''
  filterItemName.value = ''
  filterStatus.value = 'WORK'
}

// 작업 대상 통계 카드
const readyCount = computed(() => shippingStore.shippings.filter((s) => s.status === 'READY').length)
const pickingCount = computed(() => shippingStore.shippings.filter((s) => s.status === 'PICKING').length)

// 필터링: 기본으로 READY + PICKING 건만 표시(작업 대상)
const filteredShippings = computed(() => {
  return shippingStore.shippings.filter((s) => {
    // 상태 필터
    if (filterStatus.value === 'WORK') {
      if (s.status === 'SHIPPED') return false
    } else {
      if (s.status !== filterStatus.value) return false
    }
    // 출하번호 검색
    if (filterShippingNo.value.trim() !== '') {
      if (!s.shippingNo.toLowerCase().includes(filterShippingNo.value.trim().toLowerCase())) {
        return false
      }
    }
    // 품목명 검색
    if (filterItemName.value.trim() !== '') {
      const kw = filterItemName.value.trim().toLowerCase()
      if (!s.itemName?.toLowerCase().includes(kw) && !s.itemCode?.toLowerCase().includes(kw)) {
        return false
      }
    }
    return true
  })
})

function selectRow(shipping: any) {
  if (selectedShipping.value?.shippingId === shipping.shippingId) {
    selectedShipping.value = null
  } else {
    selectedShipping.value = shipping
    vehicleInput.value = ''
    pageError.value = null
  }
}

// 차량 배정 및 피킹 지시 실행
async function handleAssignPicking() {
  if (!selectedShipping.value) return
  if (!vehicleInput.value.trim()) {
    alert('배정할 차량 번호를 입력해 주세요.')
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    const updated = await shippingStore.assignPicking(selectedShipping.value.shippingId, {
      vehicleNo: vehicleInput.value.trim()
    })
    selectedShipping.value = updated
    showToast('차량 배정 및 피킹 로케이션 자동 지정이 완료되었습니다.')
    vehicleInput.value = ''
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '차량 배정 및 피킹 지시에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

// 상차 완료 및 최종 출하 확정
async function handleCompleteShipping() {
  if (!selectedShipping.value) return
  if (!confirm(`[${selectedShipping.value.shippingNo}] 건의 최종 상차 완료 및 출하 확정을 진행하시겠습니까?\n\n완제품 재고가 전산 차감됩니다.`)) {
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    const shippingId = selectedShipping.value.shippingId
    await shippingStore.completeShipping(shippingId)
    showToast('상차 완료 및 전산 재고 감산이 성공적으로 처리되었습니다.')
    selectedShipping.value = null
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '출하 완료 처리에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<template>
  <div class="app-page">
    <!-- 토스트 알림 -->
    <Transition
      enter-active-class="transform ease-out duration-300 transition"
      enter-from-class="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-2"
      enter-to-class="translate-y-0 opacity-100 sm:translate-x-0"
      leave-active-class="transition ease-in duration-100"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="successToast"
        class="app-toast app-toast-top-right"
      >
        <span class="app-toast-dot animate-ping"></span>
        <p class="text-sm app-font-label">{{ successToast }}</p>
      </div>
    </Transition>

    <!-- 에러 배너 -->
    <div
      v-if="pageError"
      class="app-alert app-alert-danger"
    >
      <AlertTriangle class="w-5 h-5 app-text-danger shrink-0 mt-0.5" />
      <div>
        <h4 class="app-alert-title">작업 처리 오류</h4>
        <p class="app-alert-text">{{ pageError }}</p>
      </div>
    </div>

    <!-- 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="app-page-title">피킹 및 상차 관리</h1>
        <p class="app-page-subtitle">배송 차량을 등록하고 최적의 완제품 피킹 위치를 자동 배정합니다. 실물 상차 완료 후 출하를 확정하여 전산 재고를 감산합니다.</p>
      </div>
      <button
        @click="handleRefresh"
        class="app-button app-button-muted self-start sm:self-auto"
      >
        <RefreshCw class="w-4 h-4" /> 새로고침
      </button>
    </div>

    <!-- 작업 현황 통계 카드 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <!-- 피킹 대기 -->
      <div
        class="app-bg-surface rounded-xl border app-border shadow-sm p-5 flex items-center gap-4 cursor-pointer transition hover:shadow-md"
        :class="{ 'ring-2 app-ring-warning': filterStatus === 'READY' }"
        @click="filterStatus = filterStatus === 'READY' ? 'WORK' : 'READY'"
      >
        <div class="p-3 app-bg-warning-soft rounded-xl">
          <ClipboardList class="w-6 h-6 app-text-warning" />
        </div>
        <div>
          <p class="text-xs app-font-strong app-text-warning uppercase tracking-wider">피킹 대기 (READY)</p>
          <p class="app-metric-value">{{ readyCount }} <span class="app-metric-unit">건</span></p>
          <p class="text-xs app-text-muted mt-1">차량 배정 및 피킹 지시 대기 중</p>
        </div>
        <div class="ml-auto">
          <span class="text-xs app-text-warning app-font-strong">클릭하여 필터</span>
        </div>
      </div>

      <!-- 상차 진행 -->
      <div
        class="app-bg-surface rounded-xl border app-border shadow-sm p-5 flex items-center gap-4 cursor-pointer transition hover:shadow-md"
        :class="{ 'ring-2 app-ring-primary': filterStatus === 'PICKING' }"
        @click="filterStatus = filterStatus === 'PICKING' ? 'WORK' : 'PICKING'"
      >
        <div class="p-3 app-bg-primary-soft rounded-xl">
          <Truck class="w-6 h-6 app-accent" />
        </div>
        <div>
          <p class="text-xs app-font-strong app-accent uppercase tracking-wider">상차 진행 (PICKING)</p>
          <p class="app-metric-value">{{ pickingCount }} <span class="app-metric-unit">건</span></p>
          <p class="text-xs app-text-muted mt-1">피킹 배정 완료, 상차 진행 중</p>
        </div>
        <div class="ml-auto">
          <span class="text-xs app-accent app-font-strong">클릭하여 필터</span>
        </div>
      </div>
    </div>

    <!-- 검색 및 필터 -->
    <div class="app-panel">
      <div
        @click="isSearchExpanded = !isSearchExpanded"
        class="app-panel-head cursor-pointer select-none"
      >
        <div class="app-panel-title">
          <SlidersHorizontal class="app-panel-icon" />
          <span class="app-panel-title">검색 필터</span>
        </div>
        <ChevronDown
          class="app-panel-icon transition-transform duration-200"
          :class="{ 'rotate-180': isSearchExpanded }"
        />
      </div>

      <div v-show="isSearchExpanded" class="app-filter-body">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- 출하번호 -->
          <div>
            <label class="app-label mb-1.5">출하 지시 번호</label>
            <div class="relative">
              <input
                v-model="filterShippingNo"
                type="text"
                placeholder="지시번호 입력"
                class="app-control app-control-search"
              />
              <Search class="app-search-icon" />
            </div>
          </div>

          <!-- 품목 -->
          <div>
            <label class="app-label mb-1.5">품목명 / 코드</label>
            <div class="relative">
              <input
                v-model="filterItemName"
                type="text"
                placeholder="완제품 품목명 또는 코드 입력"
                class="app-control app-control-search"
              />
              <Search class="app-search-icon" />
            </div>
          </div>

          <!-- 상태 필터 -->
          <div>
            <label class="app-label mb-1.5">작업 상태</label>
            <select
              v-model="filterStatus"
              class="app-control"
            >
              <option value="WORK">작업 대상 전체 (READY + PICKING)</option>
              <option value="READY">피킹 대기 (READY)</option>
              <option value="PICKING">상차 진행 (PICKING)</option>
            </select>
          </div>
        </div>

        <div class="flex justify-end gap-2 pt-2 border-t app-border-muted">
          <button
            @click="resetFilters"
            class="app-button app-button-subtle h-9"
          >
            필터 초기화
          </button>
        </div>
      </div>
    </div>

    <!-- 작업 테이블 -->
    <div class="app-panel">
      <div class="app-panel-head">
        <span class="app-panel-title">피킹/상차 작업 목록 ({{ filteredShippings.length }}건)</span>
        <span v-if="shippingStore.isLoading" class="flex items-center gap-1.5 text-xs app-text-muted">
          <Loader2 class="w-3.5 h-3.5 animate-spin" /> 로딩 중...
        </span>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1200px] w-full text-left border-collapse table-fixed">
          <colgroup>
            <col class="w-[80px]" />
            <col class="w-[180px]" />
            <col class="w-[200px]" />
            <col class="w-[260px]" />
            <col class="w-[100px]" />
            <col class="w-[120px]" />
            <col class="w-[200px]" />
            <col class="w-[200px]" />
            <col class="w-[120px]" />
          </colgroup>
          <thead>
            <tr class="app-bg-muted border-b app-border text-xs app-font-strong app-muted uppercase tracking-wider">
              <th class="px-5 py-3">ID</th>
              <th class="px-5 py-3">출하 지시 번호</th>
              <th class="px-5 py-3">고객사</th>
              <th class="px-5 py-3">출하 품목명</th>
              <th class="px-5 py-3 text-right">요청 수량</th>
              <th class="px-5 py-3 text-center">상태</th>
              <th class="px-5 py-3">배정 차량 번호</th>
              <th class="px-5 py-3">피킹 로케이션</th>
              <th class="px-5 py-3 text-center">다음 작업</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-sm">
            <tr
              v-for="shipping in filteredShippings"
              :key="shipping.shippingId"
              @click="selectRow(shipping)"
              class="app-hover-muted cursor-pointer transition select-none"
              :class="{
                'app-bg-primary-soft': selectedShipping?.shippingId === shipping.shippingId,
                'border-l-4 app-border-status-warning': shipping.status === 'READY',
                'border-l-4 app-border-status-primary': shipping.status === 'PICKING'
              }"
            >
              <!-- ID -->
              <td class="px-5 py-4 font-mono text-xs app-text-muted">#{{ shipping.shippingId }}</td>
              <!-- 출하 지시 번호 -->
              <td class="px-5 py-4 app-font-strong app-text-strong whitespace-nowrap">{{ shipping.shippingNo }}</td>
              <!-- 고객사 -->
              <td class="px-5 py-4 app-text-soft truncate whitespace-nowrap" :title="shipping.partnerName">
                {{ shipping.partnerName || shipping.partnerCode }}
              </td>
              <!-- 품목명 -->
              <td class="px-5 py-4 app-font-label app-text-soft truncate whitespace-nowrap" :title="shipping.itemName">
                {{ shipping.itemName }}
              </td>
              <!-- 요청 수량 -->
              <td class="px-5 py-4 text-right app-font-emphasis app-text-strong whitespace-nowrap">
                {{ shipping.requestQty?.toLocaleString() }} EA
              </td>
              <!-- 상태 배지 -->
              <td class="px-5 py-4 text-center whitespace-nowrap">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs app-font-strong border"
                  :class="{
                    'app-bg-warning-soft app-border app-text-warning': shipping.status === 'READY',
                    'app-bg-primary-soft app-border app-accent': shipping.status === 'PICKING'
                  }"
                >
                  <span
                    class="w-1.5 h-1.5 rounded-full"
                    :class="{
                      'app-accent-bg': shipping.status === 'READY',
                      'app-accent-bg animate-pulse': shipping.status === 'PICKING'
                    }"
                  ></span>
                  {{ shipping.status === 'READY' ? '피킹대기' : '상차진행' }}
                </span>
              </td>
              <!-- 배정 차량 -->
              <td class="px-5 py-4 whitespace-nowrap">
                <span v-if="shipping.vehicleNo" class="app-font-strong app-text-strong">{{ shipping.vehicleNo }}</span>
                <span v-else class="app-text-subtle text-xs app-font-label">미배정</span>
              </td>
              <!-- 피킹 로케이션 -->
              <td class="px-5 py-4 whitespace-nowrap">
                <span v-if="shipping.pickingLocationCode" class="font-mono app-font-strong app-accent flex items-center gap-1">
                  <MapPin class="w-3.5 h-3.5" />{{ shipping.pickingLocationCode }}
                </span>
                <span v-else class="app-text-subtle text-xs app-font-label">미배정</span>
              </td>
              <!-- 다음 작업 -->
              <td class="px-5 py-4 text-center whitespace-nowrap">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs app-font-strong"
                  :class="{
                    'app-bg-warning-soft app-text-warning': shipping.status === 'READY',
                    'app-bg-success-soft app-text-success': shipping.status === 'PICKING'
                  }"
                >
                  <ArrowRight class="w-3 h-3" />
                  {{ shipping.status === 'READY' ? '차량 배정' : '상차 완료' }}
                </span>
              </td>
            </tr>
            <tr v-if="filteredShippings.length === 0 && !shippingStore.isLoading">
              <td colspan="9" class="px-5 py-16 text-center">
                <Truck class="w-10 h-10 app-text-subtle mx-auto mb-3" />
                <p class="app-text-muted app-font-label text-sm">작업 대상 피킹/상차 건이 없습니다.</p>
                <p class="app-text-subtle text-xs mt-1">출하 지시 화면에서 신규 출하 지시를 먼저 등록해주세요.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이지네이션 -->
      <div v-if="shippingStore.totalPages > 0"
        class="app-pagination">
        <span class="app-muted">
          총 <span class="app-count-strong">{{ shippingStore.totalElements.toLocaleString() }}</span>건
          ({{ shippingStore.page + 1 }} / {{ shippingStore.totalPages }} 페이지)
        </span>
        <div class="flex items-center gap-1">
          <button @click="goToPage(0)" :disabled="shippingStore.page === 0"
            class="app-page-button">««</button>
          <button @click="goToPage(shippingStore.page - 1)" :disabled="shippingStore.page === 0"
            class="app-page-button">«</button>
          <button @click="goToPage(shippingStore.page + 1)" :disabled="shippingStore.page >= shippingStore.totalPages - 1"
            class="app-page-button">»</button>
          <button @click="goToPage(shippingStore.totalPages - 1)" :disabled="shippingStore.page >= shippingStore.totalPages - 1"
            class="app-page-button">»»</button>
        </div>
      </div>
    </div>

    <!-- 디테일 패널 -->
    <div
      v-if="selectedShipping"
      class="app-panel animate-slide-up"
    >
      <div
        class="px-5 py-4 app-text-inverse flex items-center justify-between"
        :class="{
          'app-bg-warning': selectedShipping.status === 'READY',
          'app-accent-bg': selectedShipping.status === 'PICKING'
        }"
      >
        <div class="flex items-center gap-2">
          <Truck class="w-5 h-5 opacity-80" />
          <h3 class="app-font-emphasis text-sm">
            {{ selectedShipping.status === 'READY' ? '차량 배정 및 피킹 지시' : '상차 현황 및 출하 완료 확정' }}
            — {{ selectedShipping.shippingNo }}
          </h3>
        </div>
        <button
          @click="selectedShipping = null"
          class="app-text-inverse text-xs app-font-strong app-bg-muted px-2.5 py-1 rounded"
        >
          패널 닫기
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- 출하 정보 그리드 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="p-4 app-bg-muted border app-border-muted rounded-xl">
            <span class="text-xs app-text-muted app-font-strong block">고객사</span>
            <span class="app-font-strong app-text-strong block mt-1">{{ selectedShipping.partnerName }}</span>
            <span class="text-xs font-mono app-text-muted block">{{ selectedShipping.partnerCode }}</span>
          </div>
          <div class="p-4 app-bg-muted border app-border-muted rounded-xl">
            <span class="text-xs app-text-muted app-font-strong block">출하 품목</span>
            <span class="app-font-strong app-text-strong block mt-1 truncate" :title="selectedShipping.itemName">{{ selectedShipping.itemName }}</span>
            <span class="text-xs font-mono app-text-muted block">{{ selectedShipping.itemCode }}</span>
          </div>
          <div class="p-4 app-bg-muted border app-border-muted rounded-xl">
            <span class="text-xs app-text-muted app-font-strong block">출하 요청 수량</span>
            <span class="app-font-emphasis app-text-strong text-xl block mt-1">{{ selectedShipping.requestQty?.toLocaleString() }} <span class="text-sm app-font-label app-text-muted">EA</span></span>
          </div>
          <div class="p-4 app-bg-muted border app-border-muted rounded-xl">
            <span class="text-xs app-text-muted app-font-strong block">현재 상태</span>
            <span class="mt-1 block">
              <span
                class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs app-font-strong border"
                :class="{
                  'app-bg-warning-soft app-border app-text-warning': selectedShipping.status === 'READY',
                  'app-bg-primary-soft app-border app-accent': selectedShipping.status === 'PICKING'
                }"
              >
                {{ selectedShipping.status === 'READY' ? '피킹 대기 (차량 배정 전)' : '상차 진행 중' }}
              </span>
            </span>
          </div>
        </div>

        <!-- 피킹 및 차량 정보 -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 app-bg-muted border app-border rounded-xl text-sm">
          <div class="space-y-2">
            <div class="flex items-center gap-2">
              <MapPin class="w-4 h-4 app-text-muted shrink-0" />
              <span class="app-muted app-font-label">배정된 피킹 위치:</span>
              <span
                class="font-mono app-font-emphasis"
                :class="selectedShipping.pickingLocationCode ? 'app-accent' : 'app-text-subtle'"
              >
                {{ selectedShipping.pickingLocationCode || '미배정 (차량 지정 시 자동 배정됨)' }}
              </span>
            </div>
            <div class="flex items-center gap-2">
              <Truck class="w-4 h-4 app-text-muted shrink-0" />
              <span class="app-muted app-font-label">배정 차량 번호:</span>
              <span class="app-font-strong app-text-strong">{{ selectedShipping.vehicleNo || '미배정' }}</span>
            </div>
          </div>
          <div class="space-y-2">
            <div class="flex items-center gap-2">
              <Calendar class="w-4 h-4 app-text-muted shrink-0" />
              <span class="app-muted app-font-label">출하 완료 일시:</span>
              <span class="app-count-strong">{{ formatDate(selectedShipping.shippedAt) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <User class="w-4 h-4 app-text-muted shrink-0" />
              <span class="app-muted app-font-label">담당 작업자:</span>
              <span class="app-count-strong">{{ selectedShipping.workerName || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 액션 영역 -->
        <div class="pt-4 border-t app-border-muted">
          <!-- READY 상태: 차량 배정 입력 -->
          <div v-if="selectedShipping.status === 'READY'" class="space-y-3">
            <p class="text-xs app-font-strong app-text-warning flex items-center gap-1.5">
              <AlertTriangle class="w-4 h-4" />
              배송 차량 번호를 등록하면 창고 내 가용 완제품 재고가 있는 최적의 피킹 위치가 자동으로 배정됩니다.
            </p>
            <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
              <div class="flex-1 max-w-md">
                <input
                  v-model="vehicleInput"
                  type="text"
                  placeholder="배정할 차량 번호 입력 (예: 서울 88 가 1234)"
                  class="w-full h-11 px-4 app-bg-surface border app-border rounded-lg text-sm focus:outline-none focus:ring-1 app-focus-ring-warning"
                />
              </div>
              <button
                :disabled="isSubmitting"
                @click="handleAssignPicking"
                class="h-11 px-6 app-panel-title app-text-inverse app-accent-bg hover:app-accent-bg rounded-lg shadow-md flex items-center justify-center gap-2 transition disabled:opacity-50"
              >
                <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
                <Package v-else class="w-4 h-4" />
                차량 배정 및 피킹 지시 실행
              </button>
            </div>
          </div>

          <!-- PICKING 상태: 상차 완료 확정 -->
          <div v-else-if="selectedShipping.status === 'PICKING'" class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div class="text-xs app-accent app-font-strong flex items-start sm:items-center gap-2">
              <CheckCircle2 class="w-5 h-5 shrink-0 mt-0.5 sm:mt-0" />
              <div>
                <p>피킹 배정 완료 — 피킹 로케이션 <strong class="font-mono app-accent">{{ selectedShipping.pickingLocationCode }}</strong> 에서 실물 피킹 및 차량 <strong>{{ selectedShipping.vehicleNo }}</strong> 에 상차를 진행하세요.</p>
                <p class="app-text-muted mt-0.5 font-normal">상차가 완전히 끝나면 아래 버튼을 클릭하여 출하를 확정합니다.</p>
              </div>
            </div>
            <button
              :disabled="isSubmitting"
              @click="handleCompleteShipping"
              class="shrink-0 h-11 px-6 app-panel-title app-text-inverse app-accent-bg app-hover-muted rounded-lg shadow-md flex items-center gap-2 transition disabled:opacity-50"
            >
              <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
              <CheckCircle2 v-else class="w-4 h-4" />
              상차 완료 및 출하 확정 (재고 차감)
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-slide-up {
  animation: slide-up 0.25s ease-out;
}

@keyframes slide-up {
  from {
    transform: translateY(12px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>
