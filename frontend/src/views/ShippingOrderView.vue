<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  Truck,
  Plus,
  Search,
  RefreshCw,
  SlidersHorizontal,
  Package,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  ChevronDown,
  X,
  FileText,
  Calendar,
  User,
  MapPin
} from '@lucide/vue'
import { useShippingStore } from '@/state/shippingStore'
import { useInboundStore } from '@/state/inboundStore'
import type { ShippingStatus } from '@/api/shippingApi'

const shippingStore = useShippingStore()
const inboundStore = useInboundStore()

// State
const pageError = ref<string | null>(null)
const referenceDataError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSearchExpanded = ref(true)
const isSubmitting = ref(false)
const selectedShipping = ref<any>(null)

// Modal State
const isRegisterModalOpen = ref(false)
const formShippingNo = ref('')
const formPartnerCode = ref('')
const formItemCode = ref('')
const formRequestQty = ref(1)

// Detail Action State
const vehicleInput = ref('')

// Filter states
const filterShippingNo = ref('')
const filterPartnerName = ref('')
const filterStatus = ref<'ALL' | ShippingStatus>('ALL')

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  await Promise.all([
    fetchShippingList(),
    loadReferenceData()
  ])
}

async function fetchShippingList() {
  try {
    pageError.value = null
    await shippingStore.loadShippings({
      page: shippingStore.page,
      size: 20,
      status: filterStatus.value !== 'ALL' ? filterStatus.value : undefined,
      keyword: filterShippingNo.value || filterPartnerName.value || undefined,
    })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '출하 지시 목록을 불러오는데 실패했습니다.'
  }
}

async function loadReferenceData() {
  try {
    referenceDataError.value = null
    await Promise.all([
      inboundStore.loadItems('FG'),
      inboundStore.loadPartners('CUSTOMER')
    ])
  } catch (err) {
    referenceDataError.value = err instanceof Error ? err.message : '출하 등록 기준정보를 불러오는데 실패했습니다.'
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
  showToast('출하 지시 데이터가 새로고침되었습니다.')
  selectedShipping.value = null
}

function resetFilters() {
  filterShippingNo.value = ''
  filterPartnerName.value = ''
  filterStatus.value = 'ALL'
}

// Filtered Shippings
const filteredShippings = computed(() => {
  return shippingStore.shippings.filter((s) => {
    if (filterStatus.value !== 'ALL' && s.status !== filterStatus.value) {
      return false
    }
    if (filterShippingNo.value.trim() !== '') {
      if (!s.shippingNo.toLowerCase().includes(filterShippingNo.value.trim().toLowerCase())) {
        return false
      }
    }
    if (filterPartnerName.value.trim() !== '') {
      const keyword = filterPartnerName.value.trim().toLowerCase()
      const matchName = s.partnerName?.toLowerCase().includes(keyword)
      const matchCode = s.partnerCode?.toLowerCase().includes(keyword)
      if (!matchName && !matchCode) {
        return false
      }
    }
    return true
  })
})

function openRegisterModal() {
  if (inboundStore.items.length === 0 || inboundStore.partners.length === 0) {
    loadReferenceData()
  }
  // Generate random shipping no
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const random = String(Math.floor(1000 + Math.random() * 9000))
  formShippingNo.value = `SH-${year}${month}-${random}`
  formPartnerCode.value = ''
  formItemCode.value = ''
  formRequestQty.value = 1
  isRegisterModalOpen.value = true
}

async function handleRegisterShipping() {
  if (!formShippingNo.value || !formPartnerCode.value || !formItemCode.value || formRequestQty.value < 1) {
    alert('모든 필수 정보를 입력해 주세요.')
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    const request = {
      shippingNo: formShippingNo.value,
      partnerCode: formPartnerCode.value,
      itemCode: formItemCode.value,
      requestQty: formRequestQty.value
    }
    await shippingStore.registerShipping(request)
    showToast('신규 출하 지시가 성공적으로 등록되었습니다.')
    isRegisterModalOpen.value = false
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '출하 지시 등록에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

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
    showToast('배송 차량 및 피킹 로케이션 배정이 완료되었습니다.')
    vehicleInput.value = ''
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '차량 배정에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

async function handleCompleteShipping() {
  if (!selectedShipping.value) return

  if (!confirm('해당 출하 지시의 최종 출하 완료 처리를 진행하시겠습니까? (완제품 재고가 차감됩니다)')) {
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    const shippingId = selectedShipping.value.shippingId
    await shippingStore.completeShipping(shippingId)
    showToast('출하 완료 및 전산 재고 감산이 성공적으로 완료되었습니다.')
    selectedShipping.value = null
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '출하 완료 처리에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

function selectRow(shipping: any) {
  if (selectedShipping.value?.shippingId === shipping.shippingId) {
    selectedShipping.value = null
  } else {
    selectedShipping.value = shipping
    vehicleInput.value = ''
  }
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function getShippingStatusLabel(status: ShippingStatus) {
  const labels: Record<ShippingStatus, string> = {
    READY: '출하대기',
    PICKING: '피킹중',
    PACKING: '포장중',
    INSPECTING: '검수중',
    SHIPPED: '출하완료',
    PARTIALLY_SHIPPED: '부분출하',
    CANCELED: '취소'
  }
  return labels[status] || status
}

function getShippingStatusBadgeClass(status: ShippingStatus) {
  if (status === 'READY') return 'app-bg-warning-soft app-border app-text-warning'
  if (status === 'PICKING' || status === 'PACKING' || status === 'INSPECTING') return 'app-bg-primary-soft app-border app-accent'
  if (status === 'SHIPPED') return 'app-bg-success-soft app-border app-text-success'
  return 'app-bg-muted app-border app-text-muted'
}

function getShippingStatusDotClass(status: ShippingStatus) {
  if (status === 'READY') return 'app-bg-warning'
  if (status === 'SHIPPED') return 'app-bg-success'
  return 'app-accent-bg'
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
        <p class="app-type-sm app-font-label">{{ successToast }}</p>
      </div>
    </Transition>

    <!-- 경고 / 에러 배너 -->
    <div
      v-if="pageError"
      class="app-alert app-alert-danger"
    >
      <AlertTriangle class="w-5 h-5 app-text-danger shrink-0 mt-0.5" />
      <div>
        <h4 class="app-alert-title">출하 처리 중 주의사항 (에러 발생)</h4>
        <p class="app-alert-text">{{ pageError }}</p>
      </div>
    </div>

    <div
      v-if="referenceDataError"
      class="app-alert app-alert-danger"
    >
      <AlertTriangle class="w-5 h-5 app-text-danger shrink-0 mt-0.5" />
      <div>
        <h4 class="app-alert-title">출하 등록 기준정보 오류</h4>
        <p class="app-alert-text">{{ referenceDataError }}</p>
      </div>
    </div>

    <!-- 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="app-page-title">완제품 출하 지시 관리</h1>
        <p class="app-page-subtitle">고객사 주문 사양에 맞춰 출하 지시를 신규 등록하고, 피킹 로케이션 배정 및 출하 완료 처리를 관리합니다.</p>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click="handleRefresh"
          class="h-10 px-4 app-type-xs app-font-strong app-bg-surface border app-border app-text-soft app-hover-muted rounded-lg shadow-sm flex items-center gap-2 transition"
        >
          <RefreshCw class="w-4 h-4" /> 새로고침
        </button>
        <button
          @click="openRegisterModal"
          class="h-10 px-4 app-type-xs app-font-strong app-accent-bg app-hover-muted app-text-inverse rounded-lg shadow-md flex items-center gap-2 transition"
        >
          <Plus class="w-4 h-4" /> 출하 지시 등록
        </button>
      </div>
    </div>

    <!-- 검색 및 필터 -->
    <div class="app-panel">
      <div
        @click="isSearchExpanded = !isSearchExpanded"
        class="app-panel-head cursor-pointer select-none"
      >
        <div class="app-panel-title">
          <SlidersHorizontal class="w-4.5 h-4.5 app-muted" />
          <span class="app-panel-title">검색 필터 상세조회</span>
        </div>
        <ChevronDown
          class="w-4.5 h-4.5 app-muted transition-transform duration-200"
          :class="{ 'rotate-180': isSearchExpanded }"
        />
      </div>

      <div v-show="isSearchExpanded" class="app-filter-body">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- 1) 출하 지시 번호 -->
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

          <!-- 2) 고객사명 / 코드 -->
          <div>
            <label class="app-label mb-1.5">고객사명 / 코드</label>
            <div class="relative">
              <input
                v-model="filterPartnerName"
                type="text"
                placeholder="고객사명 또는 코드 입력"
                class="app-control app-control-search"
              />
              <Search class="app-search-icon" />
            </div>
          </div>

          <!-- 3) 출하 상태 -->
          <div>
            <label class="app-label mb-1.5">출하 상태</label>
            <select
              v-model="filterStatus"
              class="app-control"
            >
              <option value="ALL">전체 상태</option>
              <option value="READY">출하 대기 (READY)</option>
              <option value="PICKING">차량/피킹배정 (PICKING)</option>
              <option value="PACKING">포장 중 (PACKING)</option>
              <option value="INSPECTING">검수 중 (INSPECTING)</option>
              <option value="PARTIALLY_SHIPPED">부분 출하 (PARTIALLY_SHIPPED)</option>
              <option value="SHIPPED">출하 완료 (SHIPPED)</option>
              <option value="CANCELED">취소 (CANCELED)</option>
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

    <!-- 마스터 테이블: 출하 지시 목록 -->
    <div class="app-panel">
      <div class="app-panel-head">
        <span class="app-panel-title">출하 지시 목록 (총 {{ filteredShippings.length }}건)</span>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1200px] w-full text-left border-collapse table-fixed">
          <colgroup>
            <col class="w-[80px]" />
            <col class="w-[180px]" />
            <col class="w-[200px]" />
            <col class="w-[180px]" />
            <col class="w-[260px]" />
            <col class="w-[120px]" />
            <col class="w-[140px]" />
            <col class="w-[150px]" />
            <col class="w-[120px]" />
          </colgroup>
          <thead>
            <tr class="app-bg-muted border-b app-border app-type-xs app-font-strong app-muted uppercase tracking-wider">
              <th class="px-5 py-3">ID</th>
              <th class="px-5 py-3">출하 지시 번호</th>
              <th class="px-5 py-3">고객사</th>
              <th class="px-5 py-3">완제품 코드</th>
              <th class="px-5 py-3">완제품명</th>
              <th class="px-5 py-3 text-right">요청 수량</th>
              <th class="px-5 py-3 text-center">출하 상태</th>
              <th class="px-5 py-3">배정 차량</th>
              <th class="px-5 py-3">피킹 위치</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 app-type-sm">
            <tr
              v-for="shipping in filteredShippings"
              :key="shipping.shippingId"
              @click="selectRow(shipping)"
              class="app-hover-muted cursor-pointer transition select-none"
              :class="{ 'app-bg-primary-soft': selectedShipping?.shippingId === shipping.shippingId }"
            >
              <!-- ID -->
              <td class="px-5 py-4 font-mono app-type-xs app-text-muted">#{{ shipping.shippingId }}</td>
              <!-- 출하 지시 번호 -->
              <td class="px-5 py-4 app-font-strong app-text-strong">{{ shipping.shippingNo }}</td>
              <!-- 고객사 -->
              <td class="px-5 py-4 app-text-soft truncate" :title="shipping.partnerName">
                {{ shipping.partnerName || shipping.partnerCode }}
              </td>
              <!-- 완제품 코드 -->
              <td class="px-5 py-4 font-mono app-type-xs app-muted">{{ shipping.itemCode }}</td>
              <!-- 완제품명 -->
              <td class="px-5 py-4 app-font-label app-text-soft truncate" :title="shipping.itemName">
                {{ shipping.itemName }}
              </td>
              <!-- 요청 수량 -->
              <td class="px-5 py-4 text-right app-font-emphasis app-text-strong">
                {{ shipping.requestQty?.toLocaleString() }} EA
              </td>
              <!-- 출하 상태 -->
              <td class="px-5 py-4 text-center">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full app-type-xs app-font-strong border"
                  :class="getShippingStatusBadgeClass(shipping.status)"
                >
                  <span
                    class="w-1.5 h-1.5 rounded-full"
                    :class="getShippingStatusDotClass(shipping.status)"
                  ></span>
                  {{ getShippingStatusLabel(shipping.status) }}
                </span>
              </td>
              <!-- 배정 차량 -->
              <td class="px-5 py-4 app-font-label app-text-soft">
                {{ shipping.vehicleNo || '-' }}
              </td>
              <!-- 피킹 위치 -->
              <td class="px-5 py-4 font-mono app-type-xs app-accent app-font-strong">
                {{ shipping.pickingLocationCode || '-' }}
              </td>
            </tr>
            <tr v-if="filteredShippings.length === 0">
              <td colspan="9" class="px-5 py-12 text-center app-text-muted">
                <Truck class="w-8 h-8 app-text-subtle mx-auto mb-2" />
                조건에 맞는 출하 지시 내역이 존재하지 않습니다.
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

    <!-- 디테일 패널: 출하 지시 상세 분석 & 처리 -->
    <div
      v-if="selectedShipping"
      class="app-panel animate-slide-up"
    >
      <div class="px-5 py-4 app-bg-strong app-text-inverse flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Truck class="w-5 h-5 app-accent" />
          <h3 class="app-font-emphasis app-type-sm">출하 지시 상세 정보 및 처리 작업 (번호: {{ selectedShipping.shippingNo }})</h3>
        </div>
        <button
          @click="selectedShipping = null"
          class="app-text-muted app-type-xs app-font-strong app-bg-muted px-2.5 py-1 rounded"
        >
          패널 닫기
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- 4컬럼 정보 그리드 -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div class="p-4 app-bg-muted border app-border-muted rounded-xl space-y-2">
            <span class="app-type-xs app-text-muted app-font-strong block">고객사 정보</span>
            <span class="app-font-strong app-text-strong block app-type-base">{{ selectedShipping.partnerName }}</span>
            <span class="app-type-xs font-mono app-muted block">{{ selectedShipping.partnerCode }}</span>
          </div>

          <div class="p-4 app-bg-muted border app-border-muted rounded-xl space-y-2">
            <span class="app-type-xs app-text-muted app-font-strong block">출하 대상 품목</span>
            <span class="app-font-strong app-text-strong block app-type-base">{{ selectedShipping.itemName }}</span>
            <span class="app-type-xs font-mono app-muted block">{{ selectedShipping.itemCode }}</span>
          </div>

          <div class="p-4 app-bg-muted border app-border-muted rounded-xl space-y-2">
            <span class="app-type-xs app-text-muted app-font-strong block">출하 요청 수량</span>
            <span class="app-font-emphasis app-text-strong block app-type-xl">{{ selectedShipping.requestQty?.toLocaleString() }} EA</span>
          </div>

          <div class="p-4 app-bg-muted border app-border-muted rounded-xl space-y-2">
            <span class="app-type-xs app-text-muted app-font-strong block">진행 단계</span>
            <span class="mt-1 block">
              <span
                class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full app-type-xs app-font-emphasis border"
                :class="getShippingStatusBadgeClass(selectedShipping.status)"
              >
                {{ getShippingStatusLabel(selectedShipping.status) }}
              </span>
            </span>
          </div>
        </div>

        <!-- 추가 메타데이터 정보 (로케이션, 차량, 날짜, 담당자) -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 app-type-sm app-bg-muted p-4 border app-border rounded-xl">
          <div class="space-y-2">
            <div class="flex items-center gap-2">
              <MapPin class="w-4 h-4 app-text-muted" />
              <span class="app-muted app-font-label">배정 피킹 로케이션:</span>
              <span class="font-mono app-font-strong app-accent">{{ selectedShipping.pickingLocationCode || '미배정 (차량 지정 시 자동 배정)' }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Truck class="w-4 h-4 app-text-muted" />
              <span class="app-muted app-font-label">상차 배송 차량번호:</span>
              <span class="app-count-strong">{{ selectedShipping.vehicleNo || '미배정' }}</span>
            </div>
          </div>
          <div class="space-y-2">
            <div class="flex items-center gap-2">
              <Calendar class="w-4 h-4 app-text-muted" />
              <span class="app-muted app-font-label">최종 출하 완료 일시:</span>
              <span class="app-count-strong">{{ formatDate(selectedShipping.shippedAt) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <User class="w-4 h-4 app-text-muted" />
              <span class="app-muted app-font-label">담당 작업자명:</span>
              <span class="app-count-strong">{{ selectedShipping.workerName || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 단계별 액션 폼 -->
        <div class="pt-4 border-t app-border-muted flex flex-col md:flex-row md:items-center justify-between gap-4">
          <!-- READY 상태: 차량 배정 입력 폼 -->
          <div v-if="selectedShipping.status === 'READY'" class="w-full flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <div class="flex-1 max-w-md">
              <input
                v-model="vehicleInput"
                type="text"
                placeholder="배정할 차량 번호 입력 (예: 서울 88 가 1234)"
                class="w-full h-10 px-3.5 app-bg-surface border app-border rounded-lg app-type-sm focus:outline-none "
              />
            </div>
            <button
              :disabled="isSubmitting"
              @click="handleAssignPicking"
              class="h-10 px-5 app-type-xs app-font-strong app-text-inverse app-accent-bg app-hover-muted rounded-lg shadow-md flex items-center justify-center gap-2 transition disabled:opacity-50"
            >
              <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
              차량 배정 및 피킹 지시 실행
            </button>
          </div>

          <!-- PICKING 상태: 최종 출하 완료 단추 -->
          <div v-else-if="selectedShipping.status === 'PICKING'" class="w-full flex items-center justify-between">
            <div class="app-type-xs app-text-warning app-font-strong flex items-center gap-1">
              <AlertTriangle class="w-4 h-4" /> 피킹 및 차량 상차가 완료되면 "최종 출하 완료 처리"를 실행하여 재고를 전산 감산하십시오.
            </div>
            <button
              :disabled="isSubmitting"
              @click="handleCompleteShipping"
              class="h-10 px-6 app-type-xs app-font-strong app-text-inverse app-accent-bg app-hover-muted rounded-lg shadow-md flex items-center gap-2 transition disabled:opacity-50"
            >
              <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
              최종 출하 완료 처리
            </button>
          </div>

          <!-- SHIPPED 상태: 안내 텍스트 -->
          <div v-else class="app-type-xs app-text-muted app-font-strong">
            현재 상태에서는 이 화면에서 실행할 수 있는 작업이 없습니다.
          </div>
        </div>
      </div>
    </div>

    <!-- 출하 지시 등록 모달 -->
    <div
      v-if="isRegisterModalOpen"
      class="fixed inset-0 z-50 flex items-center justify-center app-backdrop backdrop-blur-sm p-4 animate-fade-in"
    >
      <div class="app-bg-surface rounded-2xl border app-border-muted shadow-2xl w-full max-w-lg overflow-hidden animate-scale-up">
        <!-- 모달 헤더 -->
        <div class="px-6 py-4 app-bg-strong app-text-inverse flex items-center justify-between">
          <div class="flex items-center gap-2">
            <Plus class="w-5 h-5 app-accent" />
            <h3 class="app-font-emphasis app-type-sm">신규 완제품 출하 지시 등록</h3>
          </div>
          <button @click="isRegisterModalOpen = false" class="app-text-muted transition">
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- 모달 바디 -->
        <div class="p-6 space-y-4">
          <div
            v-if="referenceDataError"
            class="app-alert app-alert-danger"
          >
            <AlertTriangle class="w-5 h-5 app-text-danger shrink-0 mt-0.5" />
            <div>
              <h4 class="app-alert-title">기준정보 로딩 실패</h4>
              <p class="app-alert-text">{{ referenceDataError }}</p>
            </div>
          </div>

          <!-- 1) 출하 지시 번호 (자동생성 / 수정가능) -->
          <div>
            <label class="app-label mb-1.5">출하 지시 번호 <span class="app-text-danger">*</span></label>
            <input
              v-model="formShippingNo"
              type="text"
              class="w-full h-10 px-3 app-bg-muted border app-border rounded-lg app-type-sm font-mono focus:outline-none "
            />
          </div>

          <!-- 2) 고객사 선택 -->
          <div>
            <label class="app-label mb-1.5">고객사 선택 <span class="app-text-danger">*</span></label>
            <select
              v-model="formPartnerCode"
              class="app-control"
            >
              <option value="">고객사를 선택하세요</option>
              <option
                v-for="partner in inboundStore.partners"
                :key="partner.partnerCode"
                :value="partner.partnerCode"
              >
                {{ partner.partnerName }} ({{ partner.partnerCode }})
              </option>
            </select>
          </div>

          <!-- 3) 품목 선택 -->
          <div>
            <label class="app-label mb-1.5">완제품 품목 선택 <span class="app-text-danger">*</span></label>
            <select
              v-model="formItemCode"
              class="app-control"
            >
              <option value="">품목을 선택하세요</option>
              <option
                v-for="item in inboundStore.items"
                :key="item.itemCode"
                :value="item.itemCode"
              >
                {{ item.itemName }} ({{ item.itemCode }})
              </option>
            </select>
          </div>

          <!-- 4) 요청 수량 -->
          <div>
            <label class="app-label mb-1.5">출하 요청 수량 (EA) <span class="app-text-danger">*</span></label>
            <input
              v-model.number="formRequestQty"
              type="number"
              min="1"
              class="w-full h-10 px-3 app-bg-surface border app-border rounded-lg app-panel-title focus:outline-none "
            />
          </div>
        </div>

        <!-- 모달 푸터 -->
        <div class="px-6 py-4 app-bg-muted border-t app-border-muted flex justify-end gap-2">
          <button
            @click="isRegisterModalOpen = false"
            class="h-10 px-4 app-type-xs app-font-strong app-bg-surface border app-border app-hover-muted app-text-soft rounded-lg transition"
          >
            취소
          </button>
          <button
            :disabled="isSubmitting"
            @click="handleRegisterShipping"
            class="h-10 px-5 app-type-xs app-font-strong app-text-inverse app-accent-bg app-hover-muted rounded-lg shadow-md flex items-center gap-2 transition disabled:opacity-50"
          >
            <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
            지시 등록 완료
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-slide-up {
  animation: slide-up 0.25s ease-out;
}

.animate-fade-in {
  animation: fade-in 0.2s ease-out;
}

.animate-scale-up {
  animation: scale-up 0.2s ease-out;
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

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scale-up {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
