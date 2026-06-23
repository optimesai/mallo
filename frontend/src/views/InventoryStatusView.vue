<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  Package,
  Home,
  AlertTriangle,
  Search,
  RefreshCw,
  SlidersHorizontal,
  Layers,
  ShieldAlert,
  Calendar,
  FileSpreadsheet,
  ChevronDown,
  CheckCircle2
} from '@lucide/vue'
import { useInventoryStore } from '@/state/inventoryStore'
import { useInboundStore } from '@/state/inboundStore'

const inventoryStore = useInventoryStore()
const inboundStore = useInboundStore()

// State
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSearchExpanded = ref(true)

// Filter states
const filterWarehouse = ref('')
const filterItem = ref('')
const filterWarningOnly = ref(false)
const sortField = ref('itemCode')
const sortDirection = ref<'asc' | 'desc'>('asc')

// Master-detail state
const selectedInventory = ref<any>(null)

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await Promise.all([
      inventoryStore.loadInventories({
        page: inventoryStore.invPage,
        size: 20,
        keyword: filterItem.value || undefined,
      }),
      inboundStore.loadItems()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '재고 데이터를 불러오는데 실패했습니다.'
  }
}

function goToPage(page: number) {
  inventoryStore.invPage = page
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
  showToast('재고 정보가 새로고침되었습니다.')
  selectedInventory.value = null
}

function resetFilters() {
  filterWarehouse.value = ''
  filterItem.value = ''
  filterWarningOnly.value = false
}

function compareValues(aValue: unknown, bValue: unknown) {
  if (aValue == null && bValue == null) return 0
  if (aValue == null) return sortDirection.value === 'asc' ? -1 : 1
  if (bValue == null) return sortDirection.value === 'asc' ? 1 : -1

  let result = 0
  if (typeof aValue === 'number' && typeof bValue === 'number') {
    result = aValue - bValue
  } else {
    result = String(aValue).localeCompare(String(bValue), 'ko', { numeric: true })
  }
  return sortDirection.value === 'asc' ? result : -result
}

function changeSort(field: string) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
}

function getSortMark(field: string) {
  if (sortField.value !== field) return ''
  return sortDirection.value === 'asc' ? '▲' : '▼'
}

// Map inventory items with safety stock from item master
const inventoryItemsWithSafety = computed(() => {
  return inventoryStore.inventories.map(inv => {
    const safetyStock = inventoryStore.getSafetyStock(inv.itemCode)
    const isUnderSafety = inv.currentQty < safetyStock
    return {
      ...inv,
      safetyStock,
      isUnderSafety
    }
  })
})

// Filtered inventories list
const filteredInventories = computed(() => {
  return inventoryItemsWithSafety.value.filter(inv => {
    // 1) Warehouse filter
    if (filterWarehouse.value) {
      if (!inv.warehouseName.includes(filterWarehouse.value)) return false
    }
    // 2) Item code/name search
    if (filterItem.value.trim() !== '') {
      const keyword = filterItem.value.toLowerCase()
      const matchCode = inv.itemCode.toLowerCase().includes(keyword)
      const matchName = inv.itemName.toLowerCase().includes(keyword)
      if (!matchCode && !matchName) return false
    }
    // 3) Safety warning filter
    if (filterWarningOnly.value && !inv.isUnderSafety) {
      return false
    }
    return true
  })
})

const sortedInventories = computed(() => {
  return [...filteredInventories.value].sort((a, b) => compareValues(a[sortField.value], b[sortField.value]))
})

// Statistics calculations
const stats = computed(() => {
  const list = inventoryItemsWithSafety.value
  const totalItems = new Set(list.map(i => i.itemCode)).size
  const totalStockQty = list.reduce((sum, curr) => sum + curr.currentQty, 0)
  const warningCount = list.filter(i => i.isUnderSafety).length
  return { totalItems, totalStockQty, warningCount }
})

// Details selection mapping
const selectedItemDetail = computed(() => {
  if (!inventoryStore.selectedDetail) return null
  return inboundStore.items.find(i => i.itemCode === inventoryStore.selectedDetail.itemCode)
})

const isDetailUnderSafety = computed(() => {
  if (!inventoryStore.selectedDetail) return false
  const safety = inventoryStore.getSafetyStock(inventoryStore.selectedDetail.itemCode)
  return inventoryStore.selectedDetail.currentQty < safety
})

const detailSafetyStock = computed(() => {
  if (!inventoryStore.selectedDetail) return 0
  return inventoryStore.getSafetyStock(inventoryStore.selectedDetail.itemCode)
})

async function selectRow(item: any) {
  if (selectedInventory.value?.itemCode === item.itemCode) {
    selectedInventory.value = null
    inventoryStore.selectedDetail = null
  } else if (!item.inventoryId) {
    selectedInventory.value = item
    inventoryStore.selectedDetail = item
  } else {
    selectedInventory.value = item
    try {
      await inventoryStore.loadInventoryDetail(item.inventoryId)
    } catch (err) {
      pageError.value = err instanceof Error ? err.message : '상세 정보를 조회하는데 실패했습니다.'
    }
  }
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



    <!-- 상단 대시보드 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="app-page-title">현재고 모니터링</h1>
        <p class="app-page-subtitle">창고 및 세부 로케이션별 실시간 재고량과 안전 재고 도달 상태를 감시합니다.</p>
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

    <!-- 현황 통계 카드 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- 카드 1: 총 재고 품목 -->
      <div class="app-bg-surface rounded-xl border app-border p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 app-bg-primary-soft rounded-xl app-accent">
          <Package class="w-6 h-6" />
        </div>
        <div>
          <span class="app-type-xs app-font-label app-text-muted block">재고 품목 종류</span>
          <span class="app-type-2xl app-font-emphasis app-text-strong mt-0.5 block">{{ stats.totalItems }} 종</span>
        </div>
      </div>

      <!-- 카드 2: 총 재고 수량 -->
      <div class="app-bg-surface rounded-xl border app-border p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 app-bg-success-soft rounded-xl app-text-success">
          <Layers class="w-6 h-6" />
        </div>
        <div>
          <span class="app-type-xs app-font-label app-text-muted block">전체 재고 수량</span>
          <span class="app-type-2xl app-font-emphasis app-text-strong mt-0.5 block">{{ stats.totalStockQty.toLocaleString() }} EA</span>
        </div>
      </div>

      <!-- 카드 3: 안전 재고 미만 경고 -->
      <div
        class="app-bg-surface rounded-xl border p-5 shadow-sm flex items-center gap-4 transition"
        :class="stats.warningCount > 0 ? 'app-border app-bg-danger-soft' : 'app-border'"
      >
        <div
          class="p-3.5 rounded-xl transition"
          :class="stats.warningCount > 0 ? 'app-bg-danger-soft app-text-danger' : 'app-bg-muted app-muted'"
        >
          <AlertTriangle class="w-6 h-6" />
        </div>
        <div>
          <span class="app-type-xs app-font-label app-text-muted block">안전재고 미달 품목</span>
          <span
            class="app-type-2xl app-font-emphasis mt-0.5 block"
            :class="stats.warningCount > 0 ? 'app-text-danger animate-pulse-subtle' : 'app-text-strong'"
          >
            {{ stats.warningCount }} 건
          </span>
        </div>
      </div>
    </div>

    <!-- 검색 및 필터 패널 -->
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
          <!-- 1) 창고명 필터 -->
          <div>
            <label class="app-label mb-1.5">창고명</label>
            <select
              v-model="filterWarehouse"
              class="app-control"
            >
              <option value="">전체 창고</option>
              <option value="원자재 창고">원자재 창고</option>
              <option value="반제품 창고">반제품 창고</option>
              <option value="완제품 창고">완제품 창고</option>
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

          <!-- 3) 안전재고 경고 필터 -->
          <div class="flex items-end pb-1">
            <label class="flex items-center gap-2 cursor-pointer select-none">
              <input
                v-model="filterWarningOnly"
                type="checkbox"
                class="w-4.5 h-4.5 rounded app-text-danger app-border-strong"
              />
              <span class="app-type-sm app-font-label app-text-danger">안전재고 미달 품목만 표시</span>
            </label>
          </div>
        </div>

        <!-- 필터 액션 버튼들 -->
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

    <!-- 메인 그리드 테이블 -->
    <div class="app-panel">
      <div class="app-panel-head">
        <span class="app-panel-title">현재고 목록</span>
      </div>

      <div class="overflow-x-auto">
        <table class="app-table min-w-[1200px] table-fixed">
          <colgroup>
            <col class="w-[80px]" />
            <col class="w-[180px]" />
            <col class="w-[300px]" />
            <col class="w-[160px]" />
            <col class="w-[160px]" />
            <col class="w-[120px]" />
            <col class="w-[120px]" />
            <col class="w-[140px]" />
            <col class="w-[140px]" />
          </colgroup>
          <thead>
            <tr class="app-bg-muted border-b app-border app-type-xs app-font-strong app-muted uppercase tracking-wider">
              <th class="app-sortable-header px-5 py-3" @click="changeSort('inventoryId')">ID <span class="app-sort-mark">{{ getSortMark('inventoryId') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('itemCode')">품목 코드 <span class="app-sort-mark">{{ getSortMark('itemCode') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('itemName')">품목명 <span class="app-sort-mark">{{ getSortMark('itemName') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('warehouseName')">적재 창고 <span class="app-sort-mark">{{ getSortMark('warehouseName') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('locationCode')">로케이션 코드 <span class="app-sort-mark">{{ getSortMark('locationCode') }}</span></th>
              <th class="app-sortable-header px-5 py-3 text-right" @click="changeSort('currentQty')">현재고 수량 <span class="app-sort-mark">{{ getSortMark('currentQty') }}</span></th>
              <th class="app-sortable-header px-5 py-3 text-right" @click="changeSort('safetyStock')">안전 재고량 <span class="app-sort-mark">{{ getSortMark('safetyStock') }}</span></th>
              <th class="app-sortable-header px-5 py-3 text-center" @click="changeSort('isUnderSafety')">안전재고 상태 <span class="app-sort-mark">{{ getSortMark('isUnderSafety') }}</span></th>
              <th class="app-sortable-header px-5 py-3" @click="changeSort('updatedAt')">최종 수정일시 <span class="app-sort-mark">{{ getSortMark('updatedAt') }}</span></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 app-type-sm">
            <tr
              v-for="item in sortedInventories"
              :key="item.itemCode"
              @click="selectRow(item)"
              class="app-hover-muted cursor-pointer transition select-none"
              :class="{
                'app-bg-primary-soft': selectedInventory?.itemCode === item.itemCode,
                'app-bg-danger-soft/10': item.isUnderSafety
              }"
            >
              <!-- ID -->
              <td class="px-5 py-4 font-mono app-type-xs app-text-muted">{{ item.inventoryId ? `#${item.inventoryId}` : '-' }}</td>
              <!-- 품목 코드 -->
              <td class="px-5 py-4 app-font-strong app-text-strong">{{ item.itemCode }}</td>
              <!-- 품목명 -->
              <td class="px-5 py-4 app-font-label app-text-soft truncate" :title="item.itemName">
                {{ item.itemName }}
              </td>
              <!-- 적재 창고 -->
              <td class="px-5 py-4 app-text-soft">
                <span class="inline-flex items-center gap-1.5">
                  <Home class="w-3.5 h-3.5 app-text-muted" />
                  {{ item.warehouseName || '-' }}
                </span>
              </td>
              <!-- 로케이션 코드 -->
              <td class="px-5 py-4 font-mono app-type-xs app-muted">{{ item.locationCode || '-' }}</td>
              <!-- 현재고 수량 -->
              <td class="px-5 py-4 text-right app-font-emphasis" :class="item.isUnderSafety ? 'app-text-danger' : 'app-text-strong'">
                {{ item.currentQty.toLocaleString() }}
              </td>
              <!-- 안전 재고량 -->
              <td class="px-5 py-4 text-right app-font-label app-muted">
                {{ item.safetyStock.toLocaleString() }}
              </td>
              <!-- 안전재고 상태 -->
              <td class="px-5 py-4 text-center">
                <span
                  v-if="item.isUnderSafety"
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full app-type-xs app-font-strong app-bg-danger-soft app-text-danger border app-border animate-pulse-subtle shadow-sm"
                >
                  <AlertTriangle class="w-3.5 h-3.5 app-text-danger" /> 부족
                </span>
                <span
                  v-else
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full app-type-xs app-font-strong app-bg-success-soft app-text-success border app-border"
                >
                  정상
                </span>
              </td>
              <!-- 최종 수정일시 -->
              <td class="px-5 py-4 app-type-xs app-text-muted font-mono">
                {{ item.updatedAt ? item.updatedAt.replace('T', ' ').substring(0, 19) : '-' }}
              </td>
            </tr>
            <tr v-if="filteredInventories.length === 0">
              <td colspan="9" class="px-5 py-12 text-center app-text-muted">
                <Package class="w-8 h-8 app-text-subtle mx-auto mb-2" />
                조회 조건에 만족하는 실시간 재고 정보가 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 페이지네이션 -->
      <div v-if="inventoryStore.invTotalPages > 0"
        class="app-pagination">
        <span class="app-muted">
          총 <span class="app-count-strong">{{ inventoryStore.invTotalElements.toLocaleString() }}</span>건
          ({{ inventoryStore.invPage + 1 }} / {{ inventoryStore.invTotalPages }} 페이지)
        </span>
        <div class="app-pagination-actions">
          <button @click="goToPage(0)" :disabled="inventoryStore.invPage === 0"
            class="app-page-button">처음</button>
          <button @click="goToPage(inventoryStore.invPage - 1)" :disabled="inventoryStore.invPage === 0"
            class="app-page-button">이전</button>
          <button @click="goToPage(inventoryStore.invPage + 1)" :disabled="inventoryStore.invPage >= inventoryStore.invTotalPages - 1"
            class="app-page-button">다음</button>
          <button @click="goToPage(inventoryStore.invTotalPages - 1)" :disabled="inventoryStore.invPage >= inventoryStore.invTotalPages - 1"
            class="app-page-button">마지막</button>
        </div>
      </div>
    </div>

    <!-- 마스터-디테일 상세 레이아웃 -->
    <div
      v-if="inventoryStore.selectedDetail"
      class="app-panel animate-slide-up"
    >
      <div class="px-5 py-4 app-bg-strong app-text-inverse flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Package class="w-5 h-5 app-accent" />
          <h3 class="app-font-emphasis app-type-sm">
            재고 상세 정보
            <span v-if="inventoryStore.selectedDetail.inventoryId">(ID: {{ inventoryStore.selectedDetail.inventoryId }})</span>
            <span v-else>(품목 기준)</span>
          </h3>
        </div>
        <button
          @click="selectedInventory = null; inventoryStore.selectedDetail = null"
          class="app-text-muted app-type-xs app-font-strong app-bg-muted px-2.5 py-1 rounded"
        >
          패널 닫기
        </button>
      </div>

      <div class="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- 품목 상세 영역 -->
        <div class="space-y-4">
          <h4 class="app-section-kicker">
            품목 마스터 세부 정보
          </h4>
          <div class="app-bg-muted border app-border-muted rounded-xl p-4 space-y-3 app-type-sm">
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">품목 코드</span>
              <span class="col-span-2 font-mono app-font-strong app-text-strong">{{ inventoryStore.selectedDetail.itemCode }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">품목명</span>
              <span class="col-span-2 app-text-strong app-font-label">{{ inventoryStore.selectedDetail.itemName }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">규격 (Spec)</span>
              <span class="col-span-2 app-text-strong">{{ selectedItemDetail?.spec || '-' }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">관리 단위</span>
              <span class="col-span-2 app-text-strong uppercase app-font-strong">{{ selectedItemDetail?.unit || 'EA' }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">품목 유형</span>
              <span class="col-span-2">
                <span
                  class="px-2 py-0.5 rounded app-type-xs app-font-emphasis border"
                  :class="{
                    'app-bg-warning-soft app-border app-text-warning': selectedItemDetail?.itemType === 'RAW',
                    'app-bg-primary-soft app-border app-accent': selectedItemDetail?.itemType === 'HALF',
                    'app-status-info': selectedItemDetail?.itemType === 'FG'
                  }"
                >
                  {{ selectedItemDetail?.itemType || '-' }}
                </span>
              </span>
            </div>
          </div>
        </div>

        <!-- 로케이션 및 경고 영역 -->
        <div class="space-y-4">
          <h4 class="app-section-kicker">
            재고 및 보관 정보
          </h4>
          <div class="app-bg-muted border app-border-muted rounded-xl p-4 space-y-3 app-type-sm">
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">적재 창고</span>
              <span class="col-span-2 app-text-strong app-font-strong">{{ inventoryStore.selectedDetail.warehouseName || '-' }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">로케이션 주소</span>
              <span class="col-span-2 app-text-strong font-mono">{{ inventoryStore.selectedDetail.locationCode || '-' }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">현재고 수량</span>
              <span class="col-span-2 app-font-emphasis app-text-strong">{{ inventoryStore.selectedDetail.currentQty.toLocaleString() }} EA</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">안전 재고량</span>
              <span class="col-span-2 app-font-label app-muted">{{ detailSafetyStock.toLocaleString() }} EA</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="app-text-muted app-font-strong">보관 상태 분석</span>
              <span class="col-span-2">
                <span
                  v-if="isDetailUnderSafety"
                  class="app-text-danger app-font-emphasis flex items-center gap-1.5"
                >
                  <AlertTriangle class="w-4 h-4 app-text-danger" />
                  안전재고 미달 ({{ (detailSafetyStock - inventoryStore.selectedDetail.currentQty).toLocaleString() }} EA 부족)
                </span>
                <span v-else class="app-text-success app-font-strong flex items-center gap-1.5">
                  <CheckCircle2 class="w-4 h-4 app-text-success" />
                  안전재고 충족 (적정 재고 수준 유지 중)
                </span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-pulse-subtle {
  animation: pulse-subtle 2s infinite ease-in-out;
}

@keyframes pulse-subtle {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.85;
  }
}

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
