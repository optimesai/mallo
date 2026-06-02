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

// Master-detail state
const selectedInventory = ref<any>(null)

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await Promise.all([
      inventoryStore.loadInventories(),
      inboundStore.loadItems()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '재고 데이터를 불러오는데 실패했습니다.'
  }
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
  if (selectedInventory.value?.inventoryId === item.inventoryId) {
    selectedInventory.value = null
    inventoryStore.selectedDetail = null
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
  <div class="space-y-6 max-w-[1600px] mx-auto p-4 md:p-6 text-slate-800">
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
        class="fixed top-5 right-5 z-50 flex items-center gap-3 bg-slate-900/90 backdrop-blur-md text-white px-5 py-3.5 rounded-xl shadow-2xl border border-slate-700/50"
      >
        <span class="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
        <p class="text-sm font-semibold">{{ successToast }}</p>
      </div>
    </Transition>

    <!-- 에러 배너 -->
    <div
      v-if="pageError"
      class="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl flex items-start gap-3"
    >
      <AlertTriangle class="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
      <div>
        <h4 class="font-bold text-sm">오류가 발생했습니다</h4>
        <p class="text-xs text-rose-700/90 mt-0.5">{{ pageError }}</p>
      </div>
    </div>

    <!-- 안전재고 부족 긴급 배너 -->
    <div
      v-if="stats.warningCount > 0"
      class="p-4 bg-gradient-to-r from-rose-500 to-rose-600 text-white rounded-xl shadow-lg flex items-center justify-between gap-4 animate-pulse-subtle"
    >
      <div class="flex items-center gap-3">
        <div class="p-2 bg-white/20 rounded-lg">
          <ShieldAlert class="w-6 h-6 text-white" />
        </div>
        <div>
          <h4 class="font-bold text-base">안전 재고 부족 경고 발령 ({{ stats.warningCount }}건)</h4>
          <p class="text-xs text-white/80 mt-0.5">품목별 안전 재고량 미만으로 떨어진 자재가 존재합니다. 즉시 발주 및 입고 처리를 진행하십시오.</p>
        </div>
      </div>
      <button
        @click="filterWarningOnly = true"
        class="px-4 py-2 bg-white text-rose-700 hover:bg-rose-50 font-bold text-xs rounded-lg transition shadow-sm shrink-0"
      >
        경고 대상 필터링
      </button>
    </div>

    <!-- 상단 대시보드 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-black tracking-tight text-slate-900">실시간 현재고 현황 모니터링</h1>
        <p class="text-sm text-slate-500 mt-1">창고 및 세부 로케이션별 실시간 재고량과 안전 재고 도달 상태를 감시합니다.</p>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click="handleRefresh"
          class="h-10 px-4 text-xs font-bold bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 hover:border-slate-300 rounded-lg shadow-sm flex items-center gap-2 transition"
        >
          <RefreshCw class="w-4 h-4" /> 새로고침
        </button>
      </div>
    </div>

    <!-- 현황 통계 카드 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- 카드 1: 총 재고 품목 -->
      <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 bg-indigo-50 rounded-xl text-indigo-600">
          <Package class="w-6 h-6" />
        </div>
        <div>
          <span class="text-xs font-medium text-slate-400 block">재고 품목 종류</span>
          <span class="text-2xl font-extrabold text-slate-900 mt-0.5 block">{{ stats.totalItems }} 종</span>
        </div>
      </div>

      <!-- 카드 2: 총 재고 수량 -->
      <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 bg-emerald-50 rounded-xl text-emerald-600">
          <Layers class="w-6 h-6" />
        </div>
        <div>
          <span class="text-xs font-medium text-slate-400 block">전체 재고 수량</span>
          <span class="text-2xl font-extrabold text-slate-900 mt-0.5 block">{{ stats.totalStockQty.toLocaleString() }} EA</span>
        </div>
      </div>

      <!-- 카드 3: 안전 재고 미만 경고 -->
      <div
        class="bg-white rounded-xl border p-5 shadow-sm flex items-center gap-4 transition"
        :class="stats.warningCount > 0 ? 'border-rose-300 bg-rose-50/20' : 'border-slate-200'"
      >
        <div
          class="p-3.5 rounded-xl transition"
          :class="stats.warningCount > 0 ? 'bg-rose-100 text-rose-600' : 'bg-slate-50 text-slate-500'"
        >
          <AlertTriangle class="w-6 h-6" />
        </div>
        <div>
          <span class="text-xs font-medium text-slate-400 block">안전재고 미달 품목</span>
          <span
            class="text-2xl font-extrabold mt-0.5 block"
            :class="stats.warningCount > 0 ? 'text-rose-600 animate-pulse-subtle' : 'text-slate-900'"
          >
            {{ stats.warningCount }} 건
          </span>
        </div>
      </div>
    </div>

    <!-- 검색 및 필터 패널 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div
        @click="isSearchExpanded = !isSearchExpanded"
        class="px-5 py-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between cursor-pointer select-none"
      >
        <div class="flex items-center gap-2 text-slate-700">
          <SlidersHorizontal class="w-4.5 h-4.5 text-slate-500" />
          <span class="text-sm font-bold">검색 필터 상세조회</span>
        </div>
        <ChevronDown
          class="w-4.5 h-4.5 text-slate-500 transition-transform duration-200"
          :class="{ 'rotate-180': isSearchExpanded }"
        />
      </div>

      <div v-show="isSearchExpanded" class="p-5 space-y-4 border-t border-slate-100">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- 1) 창고명 필터 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">창고명</label>
            <select
              v-model="filterWarehouse"
              class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
            >
              <option value="">전체 창고</option>
              <option value="원자재 창고">원자재 창고</option>
              <option value="반제품 창고">반제품 창고</option>
              <option value="완제품 창고">완제품 창고</option>
            </select>
          </div>

          <!-- 2) 품목 검색 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">품목 검색 (코드/명)</label>
            <div class="relative">
              <input
                v-model="filterItem"
                type="text"
                placeholder="품목 코드 또는 품목명 입력"
                class="w-full h-10 pl-9 pr-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
              />
              <Search class="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            </div>
          </div>

          <!-- 3) 안전재고 경고 필터 -->
          <div class="flex items-end pb-1">
            <label class="flex items-center gap-2 cursor-pointer select-none">
              <input
                v-model="filterWarningOnly"
                type="checkbox"
                class="w-4.5 h-4.5 rounded text-rose-600 focus:ring-rose-500 border-slate-300"
              />
              <span class="text-sm font-semibold text-rose-700">안전재고 미달 품목만 표시</span>
            </label>
          </div>
        </div>

        <!-- 필터 액션 버튼들 -->
        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button
            @click="resetFilters"
            class="h-9 px-4 text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition"
          >
            필터 초기화
          </button>
        </div>
      </div>
    </div>

    <!-- 메인 그리드 테이블 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div class="px-5 py-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <span class="text-sm font-bold text-slate-700">실시간 현재고 목록 (총 {{ filteredInventories.length }}건)</span>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1200px] w-full text-left border-collapse table-fixed">
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
            <tr class="bg-slate-50/50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase tracking-wider">
              <th class="px-5 py-3">ID</th>
              <th class="px-5 py-3">품목 코드</th>
              <th class="px-5 py-3">품목명</th>
              <th class="px-5 py-3">적재 창고</th>
              <th class="px-5 py-3">로케이션 코드</th>
              <th class="px-5 py-3 text-right">현재고 수량</th>
              <th class="px-5 py-3 text-right">안전 재고량</th>
              <th class="px-5 py-3 text-center">안전재고 상태</th>
              <th class="px-5 py-3">최종 수정일시</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-sm">
            <tr
              v-for="item in filteredInventories"
              :key="item.inventoryId"
              @click="selectRow(item)"
              class="hover:bg-slate-50 cursor-pointer transition select-none"
              :class="{
                'bg-indigo-50/30': selectedInventory?.inventoryId === item.inventoryId,
                'bg-rose-50/10': item.isUnderSafety
              }"
            >
              <!-- ID -->
              <td class="px-5 py-4 font-mono text-xs text-slate-400">#{{ item.inventoryId }}</td>
              <!-- 품목 코드 -->
              <td class="px-5 py-4 font-bold text-slate-900">{{ item.itemCode }}</td>
              <!-- 품목명 -->
              <td class="px-5 py-4 font-medium text-slate-700 truncate" :title="item.itemName">
                {{ item.itemName }}
              </td>
              <!-- 적재 창고 -->
              <td class="px-5 py-4 text-slate-600">
                <span class="inline-flex items-center gap-1.5">
                  <Home class="w-3.5 h-3.5 text-slate-400" />
                  {{ item.warehouseName }}
                </span>
              </td>
              <!-- 로케이션 코드 -->
              <td class="px-5 py-4 font-mono text-xs text-slate-500">{{ item.locationCode }}</td>
              <!-- 현재고 수량 -->
              <td class="px-5 py-4 text-right font-extrabold" :class="item.isUnderSafety ? 'text-rose-600' : 'text-slate-900'">
                {{ item.currentQty.toLocaleString() }}
              </td>
              <!-- 안전 재고량 -->
              <td class="px-5 py-4 text-right font-semibold text-slate-500">
                {{ item.safetyStock.toLocaleString() }}
              </td>
              <!-- 안전재고 상태 -->
              <td class="px-5 py-4 text-center">
                <span
                  v-if="item.isUnderSafety"
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800 border border-rose-200 animate-pulse-subtle shadow-sm"
                >
                  <AlertTriangle class="w-3.5 h-3.5 text-rose-600" /> 부족
                </span>
                <span
                  v-else
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200"
                >
                  정상
                </span>
              </td>
              <!-- 최종 수정일시 -->
              <td class="px-5 py-4 text-xs text-slate-400 font-mono">
                {{ item.updatedAt ? item.updatedAt.replace('T', ' ').substring(0, 19) : '-' }}
              </td>
            </tr>
            <tr v-if="filteredInventories.length === 0">
              <td colspan="9" class="px-5 py-12 text-center text-slate-400">
                <Package class="w-8 h-8 text-slate-300 mx-auto mb-2" />
                조회 조건에 만족하는 실시간 재고 정보가 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 마스터-디테일 상세 레이아웃 -->
    <div
      v-if="inventoryStore.selectedDetail"
      class="bg-white rounded-xl border border-slate-200 shadow-lg overflow-hidden animate-slide-up"
    >
      <div class="px-5 py-4 bg-slate-900 text-white flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Package class="w-5 h-5 text-indigo-400" />
          <h3 class="font-extrabold text-sm">재고 단건 상세 정보 (ID: {{ inventoryStore.selectedDetail.inventoryId }})</h3>
        </div>
        <button
          @click="selectedInventory = null; inventoryStore.selectedDetail = null"
          class="text-slate-400 hover:text-white text-xs font-bold bg-slate-800 px-2.5 py-1 rounded"
        >
          패널 닫기
        </button>
      </div>

      <div class="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- 품목 상세 영역 -->
        <div class="space-y-4">
          <h4 class="text-xs font-black uppercase text-slate-400 tracking-wider flex items-center gap-1">
            <span class="w-1 h-3.5 bg-indigo-600 rounded-full"></span> 품목 마스터 세부 정보
          </h4>
          <div class="bg-slate-50 border border-slate-100 rounded-xl p-4 space-y-3 text-sm">
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">품목 코드</span>
              <span class="col-span-2 font-mono font-bold text-slate-800">{{ inventoryStore.selectedDetail.itemCode }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">품목명</span>
              <span class="col-span-2 text-slate-800 font-semibold">{{ inventoryStore.selectedDetail.itemName }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">규격 (Spec)</span>
              <span class="col-span-2 text-slate-800">{{ selectedItemDetail?.spec || '-' }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">관리 단위</span>
              <span class="col-span-2 text-slate-800 uppercase font-bold">{{ selectedItemDetail?.unit || 'EA' }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">품목 유형</span>
              <span class="col-span-2">
                <span
                  class="px-2 py-0.5 rounded text-xs font-extrabold border"
                  :class="{
                    'bg-amber-100 border-amber-200 text-amber-800': selectedItemDetail?.itemType === 'RAW',
                    'bg-blue-100 border-blue-200 text-blue-800': selectedItemDetail?.itemType === 'HALF',
                    'bg-purple-100 border-purple-200 text-purple-800': selectedItemDetail?.itemType === 'FG'
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
          <h4 class="text-xs font-black uppercase text-slate-400 tracking-wider flex items-center gap-1">
            <span class="w-1 h-3.5 bg-indigo-600 rounded-full"></span> 재고 및 보관 정보
          </h4>
          <div class="bg-slate-50 border border-slate-100 rounded-xl p-4 space-y-3 text-sm">
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">적재 창고</span>
              <span class="col-span-2 text-slate-800 font-bold">{{ inventoryStore.selectedDetail.warehouseName }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">로케이션 주소</span>
              <span class="col-span-2 text-slate-800 font-mono">{{ inventoryStore.selectedDetail.locationCode }}</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">현재고 수량</span>
              <span class="col-span-2 font-extrabold text-slate-900">{{ inventoryStore.selectedDetail.currentQty.toLocaleString() }} EA</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">안전 재고량</span>
              <span class="col-span-2 font-semibold text-slate-500">{{ detailSafetyStock.toLocaleString() }} EA</span>
            </div>
            <div class="grid grid-cols-3">
              <span class="text-slate-400 font-bold">보관 상태 분석</span>
              <span class="col-span-2">
                <span
                  v-if="isDetailUnderSafety"
                  class="text-rose-600 font-extrabold flex items-center gap-1.5"
                >
                  <AlertTriangle class="w-4 h-4 text-rose-500" />
                  안전재고 미달 ({{ (detailSafetyStock - inventoryStore.selectedDetail.currentQty).toLocaleString() }} EA 부족)
                </span>
                <span v-else class="text-emerald-600 font-bold flex items-center gap-1.5">
                  <CheckCircle2 class="w-4 h-4 text-emerald-500" />
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
