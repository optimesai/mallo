<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  AlertTriangle,
  Clock,
  Search,
  RefreshCw,
  SlidersHorizontal,
  ArrowDownLeft,
  ArrowUpRight,
  User,
  Calendar,
  FileSpreadsheet,
  ChevronDown
} from '@lucide/vue'
import { useInventoryStore } from '@/state/inventoryStore'

const inventoryStore = useInventoryStore()

// State
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSearchExpanded = ref(true)

// Filter states
const filterItem = ref('')
const filterType = ref<'ALL' | 'INBOUND' | 'PRODUCTION_ISSUE'>('ALL')
const filterDateStart = ref('')
const filterDateEnd = ref('')
const sortField = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await inventoryStore.loadHistories({
      page: inventoryStore.histPage,
      size: 20,
      sort: `${sortField.value},${sortDirection.value}`,
      transactionType: filterType.value !== 'ALL' ? filterType.value : undefined,
      startDate: filterDateStart.value || undefined,
      endDate: filterDateEnd.value || undefined,
    })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '수불 이력을 불러오는데 실패했습니다.'
  }
}

function goToPage(page: number) {
  inventoryStore.histPage = page
  fetchPageData()
}

function showToast(msg: string) {
  successToast.value = msg
  setTimeout(() => {
    successToast.value = null
  }, 3000)
}

async function handleRefresh() {
  await fetchPageData()
  showToast('수불 이력이 새로고침되었습니다.')
}

function resetFilters() {
  filterItem.value = ''
  filterType.value = 'ALL'
  filterDateStart.value = ''
  filterDateEnd.value = ''
}

function compareValues(aValue: unknown, bValue: unknown) {
  if (aValue == null && bValue == null) return 0
  if (aValue == null) return sortDirection.value === 'asc' ? -1 : 1
  if (bValue == null) return sortDirection.value === 'asc' ? 1 : -1

  let result = 0
  if (typeof aValue === 'number' && typeof bValue === 'number') {
    result = aValue - bValue
  } else if (sortField.value === 'transactionType') {
    result = getTransactionTypeLabel(String(aValue)).localeCompare(getTransactionTypeLabel(String(bValue)), 'ko', { numeric: true })
  } else {
    result = String(aValue).localeCompare(String(bValue), 'ko', { numeric: true })
  }
  return sortDirection.value === 'asc' ? result : -result
}

function getTransactionTypeLabel(transactionType: string) {
  if (transactionType === 'INBOUND') return '입고적재'
  if (transactionType === 'PRODUCTION_ISSUE') return '생산불출'
  if (transactionType === 'OUTBOUND') return '출고적재'
  if (transactionType === 'RESERVATION') return '출고적재'
  if (transactionType === 'PRODUCTION_RECEIPT') return '생산입고'
  if (transactionType === 'PRODUCTION_ISSUE_CANCEL') return '불출취소'
  if (transactionType === 'PRODUCTION_RECEIPT_CANCEL') return '입고취소'
  if (transactionType === 'TRANSFER_OUT') return '이동출고'
  if (transactionType === 'TRANSFER_IN') return '이동입고'
  if (transactionType === 'SCRAP') return '폐기'
  if (transactionType === 'ADJUSTMENT') return '재고조정'
  if (transactionType === 'RETURN') return '반품'
  return transactionType
}

function getDisplayQuantity(item: { transactionType: string; quantity: number }) {
  if (['PRODUCTION_ISSUE', 'OUTBOUND', 'RESERVATION'].includes(item.transactionType)) {
    return -Math.abs(item.quantity)
  }
  return item.quantity
}

function getQuantityText(item: { transactionType: string; quantity: number }) {
  const quantity = getDisplayQuantity(item)
  const sign = quantity > 0 ? '+' : ''
  return `${sign}${quantity.toLocaleString()}`
}

function isInboundType(transactionType: string) {
  return ['INBOUND', 'PRODUCTION_RECEIPT', 'PRODUCTION_ISSUE_CANCEL', 'TRANSFER_IN', 'RETURN'].includes(transactionType)
}

function isOutboundType(transactionType: string) {
  return ['PRODUCTION_ISSUE', 'OUTBOUND', 'RESERVATION', 'PRODUCTION_RECEIPT_CANCEL', 'TRANSFER_OUT', 'SCRAP'].includes(transactionType)
}

async function changeSort(field: string) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
  inventoryStore.histPage = 0
  await fetchPageData()
}

function getSortMark(field: string) {
  if (sortField.value !== field) return ''
  return sortDirection.value === 'asc' ? '▲' : '▼'
}

// Filtered transaction history list
const filteredHistories = computed(() => {
  return inventoryStore.histories.filter(history => {
    // 1) Transaction Type Filter
    if (filterType.value !== 'ALL' && history.transactionType !== filterType.value) {
      return false
    }
    // 2) Item search (code or name)
    if (filterItem.value.trim() !== '') {
      const keyword = filterItem.value.toLowerCase()
      const matchCode = history.itemCode?.toLowerCase().includes(keyword)
      const matchName = history.itemName?.toLowerCase().includes(keyword)
      if (!matchCode && !matchName) return false
    }
    // 3) Start Date Filter
    if (filterDateStart.value) {
      const startDateTime = new Date(filterDateStart.value + 'T00:00:00').getTime()
      const itemTime = new Date(history.createdAt).getTime()
      if (itemTime < startDateTime) return false
    }
    // 4) End Date Filter
    if (filterDateEnd.value) {
      const endDateTime = new Date(filterDateEnd.value + 'T23:59:59').getTime()
      const itemTime = new Date(history.createdAt).getTime()
      if (itemTime > endDateTime) return false
    }
    return true
  })
})

const sortedHistories = computed(() => {
  return [...filteredHistories.value].sort((a, b) => {
    if (sortField.value === 'quantity') {
      return compareValues(getDisplayQuantity(a), getDisplayQuantity(b))
    }
    return compareValues(a[sortField.value], b[sortField.value])
  })
})

// Statistics calculations
const stats = computed(() => {
  const list = filteredHistories.value
  const totalCount = list.length
  const inboundQty = list
    .filter(h => h.transactionType === 'INBOUND')
    .reduce((sum, h) => sum + h.quantity, 0)
  const issueQty = list
    .filter(h => h.transactionType === 'PRODUCTION_ISSUE')
    .reduce((sum, h) => sum + h.quantity, 0)
  return { totalCount, inboundQty, issueQty }
})
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

    <!-- 에러 배너 -->
    <div
      v-if="pageError"
      class="p-4 app-bg-danger-soft border app-border app-text-danger rounded-xl flex items-start gap-3"
    >
      <AlertTriangle class="w-5 h-5 app-text-danger shrink-0 mt-0.5" />
      <div>
        <h4 class="app-alert-title">오류가 발생했습니다</h4>
        <p class="app-type-xs app-text-danger/90 mt-0.5">{{ pageError }}</p>
      </div>
    </div>

    <!-- 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="app-page-title">재고 수불(변동) 이력 추적</h1>
        <p class="app-page-subtitle">창고 내에서 발생한 입고, 생산 출고 등 모든 양적 변화의 Audit 타임라인을 조회합니다.</p>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click="handleRefresh"
          class="h-10 px-4 app-type-xs app-font-strong app-bg-surface border app-border app-text-soft app-hover-muted rounded-lg shadow-sm flex items-center gap-2 transition"
        >
          <RefreshCw class="w-4 h-4" /> 새로고침
        </button>
      </div>
    </div>

    <!-- 수불 통계 카드 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- 카드 1: 총 변동 횟수 -->
      <div class="app-bg-surface rounded-xl border app-border p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 app-bg-primary-soft rounded-xl app-accent">
          <Clock class="w-6 h-6" />
        </div>
        <div>
          <span class="app-type-xs app-font-label app-text-muted block">총 변동 이력 수</span>
          <span class="app-type-2xl app-font-emphasis app-text-strong mt-0.5 block">{{ stats.totalCount }} 건</span>
        </div>
      </div>

      <!-- 카드 2: 입고 누적량 -->
      <div class="app-bg-surface rounded-xl border app-border p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 app-bg-success-soft rounded-xl app-text-success">
          <ArrowDownLeft class="w-6 h-6" />
        </div>
        <div>
          <span class="app-type-xs app-font-label app-text-muted block">검수완료 입고량 (INBOUND)</span>
          <span class="app-type-2xl app-font-emphasis app-text-strong mt-0.5 block">+{{ stats.inboundQty.toLocaleString() }} EA</span>
        </div>
      </div>

      <!-- 카드 3: 생산 출고량 -->
      <div class="app-bg-surface rounded-xl border app-border p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 app-bg-danger-soft rounded-xl app-text-danger">
          <ArrowUpRight class="w-6 h-6" />
        </div>
        <div>
          <span class="app-type-xs app-font-label app-text-muted block">생산 자재 불출량 (ISSUE)</span>
          <span class="app-type-2xl app-font-emphasis app-text-strong mt-0.5 block">-{{ stats.issueQty.toLocaleString() }} EA</span>
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
          <SlidersHorizontal class="w-4.5 h-4.5 app-muted" />
          <span class="app-panel-title">검색 필터 상세조회</span>
        </div>
        <ChevronDown
          class="w-4.5 h-4.5 app-muted transition-transform duration-200"
          :class="{ 'rotate-180': isSearchExpanded }"
        />
      </div>

      <div v-show="isSearchExpanded" class="app-filter-body">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <!-- 1) 변동 유형 필터 -->
          <div>
            <label class="app-label mb-1.5">수불 유형</label>
            <select
              v-model="filterType"
              class="app-control"
            >
              <option value="ALL">전체 유형</option>
              <option value="INBOUND">입고 적재 (INBOUND)</option>
              <option value="PRODUCTION_ISSUE">생산 불출 (PRODUCTION_ISSUE)</option>
            </select>
          </div>

          <!-- 2) 품목 검색 -->
          <div>
            <label class="app-label mb-1.5">품목 검색 (코드/명)</label>
            <div class="relative">
              <input
                v-model="filterItem"
                type="text"
                placeholder="품목 코드 또는 품목명 입력"
                class="app-control app-control-search"
              />
              <Search class="app-search-icon" />
            </div>
          </div>

          <!-- 3) 시작 기간 -->
          <div>
            <label class="app-label mb-1.5">기간 설정 (시작)</label>
            <div class="relative">
              <input
                v-model="filterDateStart"
                type="date"
                class="app-control"
              />
            </div>
          </div>

          <!-- 4) 종료 기간 -->
          <div>
            <label class="app-label mb-1.5">기간 설정 (종료)</label>
            <div class="relative">
              <input
                v-model="filterDateEnd"
                type="date"
                class="app-control"
              />
            </div>
          </div>
        </div>

        <!-- 필터 초기화 -->
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

    <!-- 타임라인 데이터 테이블 -->
    <div class="app-panel">
      <div class="app-panel-head">
        <span class="app-panel-title">수불(변동) 타임라인 이력</span>
      </div>

      <div class="overflow-x-auto">
        <table class="app-table min-w-[1200px] table-fixed">
          <colgroup>
            <col class="w-[80px]" />
            <col class="w-[180px]" />
            <col class="w-[280px]" />
            <col class="w-[160px]" />
            <col class="w-[160px]" />
            <col class="w-[120px]" />
            <col class="w-[300px]" />
            <col class="w-[120px]" />
            <col class="w-[180px]" />
          </colgroup>
          <thead>
            <tr class="app-bg-muted border-b app-border app-type-xs app-font-strong app-muted uppercase tracking-wider">
              <th class="app-sortable-header px-5 py-3" @click="changeSort('transactionId')">번호 <span class="app-sort-mark">{{ getSortMark('transactionId') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('itemCode')">품목 코드 <span class="app-sort-mark">{{ getSortMark('itemCode') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('itemName')">품목명 <span class="app-sort-mark">{{ getSortMark('itemName') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('transactionType')">수불 유형 <span class="app-sort-mark">{{ getSortMark('transactionType') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('locationCode')">적재 위치 <span class="app-sort-mark">{{ getSortMark('locationCode') }}</span></th>
              <th class="app-sortable-header px-5 py-3 text-right" @click="changeSort('quantity')">변동 수량 <span class="app-sort-mark">{{ getSortMark('quantity') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('reasonDesc')">변동 사유 <span class="app-sort-mark">{{ getSortMark('reasonDesc') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('workerName')">작업자 <span class="app-sort-mark">{{ getSortMark('workerName') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('createdAt')">발생 일시 <span class="app-sort-mark">{{ getSortMark('createdAt') }}</span></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 app-type-sm">
            <tr
              v-for="item in sortedHistories"
              :key="item.transactionId"
              class="app-hover-muted transition select-none"
            >
              <!-- 번호 -->
              <td class="px-5 py-4 font-mono app-type-xs app-text-muted">#{{ item.transactionId }}</td>
              <!-- 품목 코드 -->
              <td class="px-5 py-4 app-font-strong app-text-strong">{{ item.itemCode }}</td>
              <!-- 품목명 -->
              <td class="px-5 py-4 app-font-label app-text-soft truncate" :title="item.itemName">
                {{ item.itemName }}
              </td>
              <!-- 수불 유형 -->
              <td class="px-5 py-4">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full app-type-xs app-font-strong border"
                  :class="{
                    'app-bg-success-soft app-border app-text-success': isInboundType(item.transactionType),
                    'app-bg-danger-soft app-border app-text-danger': isOutboundType(item.transactionType)
                  }"
                >
                  <span
                    class="w-1.5 h-1.5 rounded-full"
                    :class="{
                      'app-accent-bg': isInboundType(item.transactionType),
                      'app-bg-danger-soft0': isOutboundType(item.transactionType)
                    }"
                  ></span>
                  {{ getTransactionTypeLabel(item.transactionType) }}
                </span>
              </td>
              <!-- 적재 위치 -->
              <td class="px-5 py-4 font-mono app-type-xs app-muted">{{ item.locationCode }}</td>
              <!-- 변동 수량 -->
              <td class="px-5 py-4 text-right app-font-emphasis" :class="getDisplayQuantity(item) >= 0 ? 'app-text-success' : 'app-text-danger'">
                {{ getQuantityText(item) }}
              </td>
              <!-- 변동 사유 -->
              <td class="px-5 py-4 app-type-xs app-muted truncate" :title="item.reasonDesc">
                {{ item.reasonDesc }}
              </td>
              <!-- 작업자 -->
              <td class="px-5 py-4 app-text-soft">
                <span class="inline-flex items-center gap-1">
                  <User class="w-3.5 h-3.5 app-text-muted" />
                  {{ item.workerName || '-' }}
                </span>
              </td>
              <!-- 발생 일시 -->
              <td class="px-5 py-4 app-type-xs app-text-muted font-mono">
                {{ item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 19) : '-' }}
              </td>
            </tr>
            <tr v-if="filteredHistories.length === 0">
              <td colspan="9" class="px-5 py-12 text-center app-text-muted">
                <Clock class="w-8 h-8 app-text-subtle mx-auto mb-2" />
                조회 조건에 만족하는 수불 변동 이력이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이지네이션 -->
      <div v-if="inventoryStore.histTotalPages > 0"
        class="app-pagination">
        <span class="app-muted">
          총 <span class="app-count-strong">{{ inventoryStore.histTotalElements.toLocaleString() }}</span>건
          ({{ inventoryStore.histPage + 1 }} / {{ inventoryStore.histTotalPages }} 페이지)
        </span>
        <div class="app-pagination-actions">
          <button @click="goToPage(0)" :disabled="inventoryStore.histPage === 0"
            class="app-page-button">처음</button>
          <button @click="goToPage(inventoryStore.histPage - 1)" :disabled="inventoryStore.histPage === 0"
            class="app-page-button">이전</button>
          <button @click="goToPage(inventoryStore.histPage + 1)" :disabled="inventoryStore.histPage >= inventoryStore.histTotalPages - 1"
            class="app-page-button">다음</button>
          <button @click="goToPage(inventoryStore.histTotalPages - 1)" :disabled="inventoryStore.histPage >= inventoryStore.histTotalPages - 1"
            class="app-page-button">마지막</button>
        </div>
      </div>
    </div>
  </div>
</template>
