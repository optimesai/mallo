<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
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

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await inventoryStore.loadHistories()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '수불 이력을 불러오는데 실패했습니다.'
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
  showToast('수불 이력이 새로고침되었습니다.')
}

function resetFilters() {
  filterItem.value = ''
  filterType.value = 'ALL'
  filterDateStart.value = ''
  filterDateEnd.value = ''
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

    <!-- 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-black tracking-tight text-slate-900">재고 수불(변동) 이력 추적</h1>
        <p class="text-sm text-slate-500 mt-1">창고 내에서 발생한 입고, 생산 출고 등 모든 양적 변화의 Audit 타임라인을 조회합니다.</p>
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

    <!-- 수불 통계 카드 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- 카드 1: 총 변동 횟수 -->
      <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 bg-indigo-50 rounded-xl text-indigo-600">
          <Clock class="w-6 h-6" />
        </div>
        <div>
          <span class="text-xs font-medium text-slate-400 block">총 변동 이력 수</span>
          <span class="text-2xl font-extrabold text-slate-900 mt-0.5 block">{{ stats.totalCount }} 건</span>
        </div>
      </div>

      <!-- 카드 2: 입고 누적량 -->
      <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 bg-emerald-50 rounded-xl text-emerald-600">
          <ArrowDownLeft class="w-6 h-6" />
        </div>
        <div>
          <span class="text-xs font-medium text-slate-400 block">검수완료 입고량 (INBOUND)</span>
          <span class="text-2xl font-extrabold text-slate-900 mt-0.5 block">+{{ stats.inboundQty.toLocaleString() }} EA</span>
        </div>
      </div>

      <!-- 카드 3: 생산 출고량 -->
      <div class="bg-white rounded-xl border border-slate-200 p-5 shadow-sm flex items-center gap-4">
        <div class="p-3.5 bg-rose-50 rounded-xl text-rose-600">
          <ArrowUpRight class="w-6 h-6" />
        </div>
        <div>
          <span class="text-xs font-medium text-slate-400 block">생산 자재 불출량 (ISSUE)</span>
          <span class="text-2xl font-extrabold text-slate-900 mt-0.5 block">-{{ stats.issueQty.toLocaleString() }} EA</span>
        </div>
      </div>
    </div>

    <!-- 검색 및 필터 -->
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
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <!-- 1) 변동 유형 필터 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">수불 유형</label>
            <select
              v-model="filterType"
              class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">전체 유형</option>
              <option value="INBOUND">입고 적재 (INBOUND)</option>
              <option value="PRODUCTION_ISSUE">생산 불출 (PRODUCTION_ISSUE)</option>
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

          <!-- 3) 시작 기간 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">기간 설정 (시작)</label>
            <div class="relative">
              <input
                v-model="filterDateStart"
                type="date"
                class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <!-- 4) 종료 기간 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">기간 설정 (종료)</label>
            <div class="relative">
              <input
                v-model="filterDateEnd"
                type="date"
                class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>
        </div>

        <!-- 필터 초기화 -->
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

    <!-- 타임라인 데이터 테이블 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div class="px-5 py-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <span class="text-sm font-bold text-slate-700">수불(변동) 타임라인 이력 (총 {{ filteredHistories.length }}건)</span>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1200px] w-full text-left border-collapse table-fixed">
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
            <tr class="bg-slate-50/50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase tracking-wider">
              <th class="px-5 py-3">번호</th>
              <th class="px-5 py-3">품목 코드</th>
              <th class="px-5 py-3">품목명</th>
              <th class="px-5 py-3">수불 유형</th>
              <th class="px-5 py-3">적재 위치</th>
              <th class="px-5 py-3 text-right">변동 수량</th>
              <th class="px-5 py-3">변동 사유</th>
              <th class="px-5 py-3">작업자</th>
              <th class="px-5 py-3">발생 일시</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-sm">
            <tr
              v-for="item in filteredHistories"
              :key="item.transactionId"
              class="hover:bg-slate-50 transition select-none"
            >
              <!-- 번호 -->
              <td class="px-5 py-4 font-mono text-xs text-slate-400">#{{ item.transactionId }}</td>
              <!-- 품목 코드 -->
              <td class="px-5 py-4 font-bold text-slate-900">{{ item.itemCode }}</td>
              <!-- 품목명 -->
              <td class="px-5 py-4 font-medium text-slate-700 truncate" :title="item.itemName">
                {{ item.itemName }}
              </td>
              <!-- 수불 유형 -->
              <td class="px-5 py-4">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold border"
                  :class="{
                    'bg-emerald-100 border-emerald-200 text-emerald-800': item.transactionType === 'INBOUND',
                    'bg-rose-100 border-rose-200 text-rose-800': item.transactionType === 'PRODUCTION_ISSUE'
                  }"
                >
                  <span
                    class="w-1.5 h-1.5 rounded-full"
                    :class="{
                      'bg-emerald-500': item.transactionType === 'INBOUND',
                      'bg-rose-500': item.transactionType === 'PRODUCTION_ISSUE'
                    }"
                  ></span>
                  {{ item.transactionType === 'INBOUND' ? '입고적재' : '생산불출' }}
                </span>
              </td>
              <!-- 적재 위치 -->
              <td class="px-5 py-4 font-mono text-xs text-slate-500">{{ item.locationCode }}</td>
              <!-- 변동 수량 -->
              <td class="px-5 py-4 text-right font-extrabold" :class="item.transactionType === 'INBOUND' ? 'text-emerald-600' : 'text-rose-600'">
                {{ item.transactionType === 'INBOUND' ? '+' : '-' }}{{ item.quantity.toLocaleString() }}
              </td>
              <!-- 변동 사유 -->
              <td class="px-5 py-4 text-xs text-slate-500 truncate" :title="item.reasonDesc">
                {{ item.reasonDesc }}
              </td>
              <!-- 작업자 -->
              <td class="px-5 py-4 text-slate-600">
                <span class="inline-flex items-center gap-1">
                  <User class="w-3.5 h-3.5 text-slate-400" />
                  {{ item.workerName || '-' }}
                </span>
              </td>
              <!-- 발생 일시 -->
              <td class="px-5 py-4 text-xs text-slate-400 font-mono">
                {{ item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 19) : '-' }}
              </td>
            </tr>
            <tr v-if="filteredHistories.length === 0">
              <td colspan="9" class="px-5 py-12 text-center text-slate-400">
                <Clock class="w-8 h-8 text-slate-300 mx-auto mb-2" />
                조회 조건에 만족하는 수불 변동 이력이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
