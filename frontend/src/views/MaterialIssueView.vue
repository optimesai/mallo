<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  FileText,
  Play,
  CheckCircle2,
  AlertTriangle,
  Search,
  RefreshCw,
  SlidersHorizontal,
  Package,
  Layers,
  ArrowRight,
  ShieldAlert,
  Loader2,
  ChevronDown
} from '@lucide/vue'
import { useWorkOrderStore } from '@/state/workOrderStore'
import type { WorkOrderResponse } from '@/api/workOrderApi'

const workOrderStore = useWorkOrderStore()

// State
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSearchExpanded = ref(true)
const isSubmitting = ref(false)

// Filter states
const filterOrderNo = ref('')
const filterStatus = ref<'ALL' | 'READY' | 'RUN' | 'HOLD'>('ALL')

// Selected work order (Master-detail)
const selectedOrder = ref<WorkOrderResponse | null>(null)

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await workOrderStore.loadWorkOrders()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터를 불러오는데 실패했습니다.'
  }
}

function showToast(msg: string) {
  successToast.value = msg
  setTimeout(() => {
    successToast.value = null
  }, 4000)
}

async function handleRefresh() {
  await fetchPageData()
  showToast('작업 지시 및 재고 데이터가 새로고침되었습니다.')
  selectedOrder.value = null
}

function resetFilters() {
  filterOrderNo.value = ''
  filterStatus.value = 'ALL'
}

// Filtered work orders list
const filteredOrders = computed(() => {
  return workOrderStore.workOrders.filter(order => {
    if (filterStatus.value !== 'ALL' && order.status !== filterStatus.value) {
      return false
    }
    if (filterOrderNo.value.trim() !== '') {
      const keyword = filterOrderNo.value.toLowerCase()
      if (!order.orderNo.toLowerCase().includes(keyword) && !order.itemName.toLowerCase().includes(keyword)) {
        return false
      }
    }
    return true
  })
})

// Calculate BOM requirements & available stock comparison for selected work order
const selectedOrderBOMStatus = computed(() => {
  if (!selectedOrder.value || workOrderStore.bomRequirements.length === 0) return []

  return workOrderStore.bomRequirements.map(req => {
    const remainingQty = Math.max(req.requiredQty - req.issuedQty, 0)
    const availableQty = req.availableQty
    const isSufficient = availableQty >= remainingQty

    return {
      ...req,
      remainingQty,
      availableQty,
      isSufficient
    }
  })
})

// Overall check if all required materials are sufficient
const isStockSufficient = computed(() => {
  if (selectedOrderBOMStatus.value.length === 0) return false
  return selectedOrderBOMStatus.value.every(mat => mat.isSufficient)
})

// Trigger actual POST API call for issuing materials
async function handleIssueMaterials() {
  if (!selectedOrder.value) return
  if (!isStockSufficient.value) {
    alert('가용 자재 재고가 부족합니다. 출고를 실행할 수 없습니다.')
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    await workOrderStore.issueMaterials(selectedOrder.value.orderNo)
    showToast(`작업 지시 [${selectedOrder.value.orderNo}]에 대한 자재 불출이 완료되었습니다.`)
    selectedOrder.value = workOrderStore.selectedDetail?.workOrder ?? selectedOrder.value
  } catch (err) {
    const errMsg = err instanceof Error ? err.message : '출고 처리에 실패했습니다.'
    pageError.value = `[자재 불출 실행 반려] ${errMsg}`
  } finally {
    isSubmitting.value = false
  }
}

async function selectRow(order: WorkOrderResponse) {
  if (selectedOrder.value?.orderId === order.orderId) {
    selectedOrder.value = null
    workOrderStore.bomRequirements = []
  } else {
    selectedOrder.value = order
    await workOrderStore.loadBOMRequirements(order.orderNo)
    selectedOrder.value = workOrderStore.selectedDetail?.workOrder ?? order
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

    <!-- 경고 / 에러 배너 -->
    <div
      v-if="pageError"
      class="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl flex items-start gap-3"
    >
      <AlertTriangle class="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
      <div>
        <h4 class="font-bold text-sm">작업 처리 중 주의사항</h4>
        <p class="text-xs text-rose-700/90 mt-0.5 whitespace-pre-line">{{ pageError }}</p>
      </div>
    </div>

    <!-- 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-black tracking-tight text-slate-900">BOM 기반 생산 자재 출고(불출)</h1>
        <p class="text-sm text-slate-500 mt-1">대기(READY) 중인 생산 작업 지시의 BOM 구성 소요량을 연산하고 자재 창고 재고를 차감합니다.</p>
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

    <!-- 안내 카드 -->
    <div class="bg-indigo-50/50 border border-indigo-100 rounded-xl p-5 shadow-sm flex gap-4">
      <div class="p-3 bg-indigo-600 text-white rounded-lg shrink-0 h-fit">
        <ShieldAlert class="w-5 h-5" />
      </div>
      <div class="space-y-1">
        <h4 class="text-sm font-bold text-indigo-900">실제 작업지시 기반 자재 불출 안내</h4>
        <p class="text-xs text-indigo-700/90 leading-relaxed">
          - **작업지시 조회**: 실제 백엔드 `GET /api/work-orders` 목록을 사용합니다.<br />
          - **자재 소요량**: 선택한 작업지시 상세의 BOM 기준 필요 수량, 불출 수량, 가용 재고를 표시합니다.<br />
          - **출고 실행**: "자재 불출 실행" 버튼 클릭 시 `POST /api/work-orders/{orderNo}/issue-materials`를 호출합니다.
        </p>
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
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- 1) 지시번호 검색 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">작업 지시 번호 / 생산품목</label>
            <div class="relative">
              <input
                v-model="filterOrderNo"
                type="text"
                placeholder="지시번호 또는 완제품명 입력"
                class="w-full h-10 pl-9 pr-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
              />
              <Search class="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            </div>
          </div>

          <!-- 2) 지시 상태 필터 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">지시 상태</label>
            <select
              v-model="filterStatus"
              class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">전체 상태</option>
              <option value="READY">지시 대기 (READY)</option>
              <option value="RUN">지시 실행 중 (RUN)</option>
              <option value="HOLD">작업 보류 (HOLD)</option>
            </select>
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

    <!-- 마스터 테이블: 작업 지시 목록 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div class="px-5 py-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <span class="text-sm font-bold text-slate-700">생산 작업 지시 목록 (총 {{ filteredOrders.length }}건)</span>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1200px] w-full text-left border-collapse table-fixed">
          <colgroup>
            <col class="w-[80px]" />
            <col class="w-[180px]" />
            <col class="w-[180px]" />
            <col class="w-[300px]" />
            <col class="w-[140px]" />
            <col class="w-[140px]" />
            <col class="w-[180px]" />
          </colgroup>
          <thead>
            <tr class="bg-slate-50/50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase tracking-wider">
              <th class="px-5 py-3">ID</th>
              <th class="px-5 py-3">작업 지시 번호</th>
              <th class="px-5 py-3">생산 품목코드</th>
              <th class="px-5 py-3">생산 품목명</th>
              <th class="px-5 py-3 text-right">목표 생산 수량</th>
              <th class="px-5 py-3 text-center">지시 상태</th>
              <th class="px-5 py-3">계획 일자</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-sm">
            <tr
              v-for="order in filteredOrders"
              :key="order.orderId"
              @click="selectRow(order)"
              class="hover:bg-slate-50 cursor-pointer transition select-none"
              :class="{ 'bg-indigo-50/30': selectedOrder?.orderId === order.orderId }"
            >
              <!-- ID -->
              <td class="px-5 py-4 font-mono text-xs text-slate-400">#{{ order.orderId }}</td>
              <!-- 작업 지시 번호 -->
              <td class="px-5 py-4 font-bold text-slate-900">{{ order.orderNo }}</td>
              <!-- 생산 품목코드 -->
              <td class="px-5 py-4 font-mono text-xs text-slate-500">{{ order.itemCode }}</td>
              <!-- 생산 품목명 -->
              <td class="px-5 py-4 font-semibold text-slate-700 truncate" :title="order.itemName">
                {{ order.itemName }}
              </td>
              <!-- 목표 생산 수량 -->
              <td class="px-5 py-4 text-right font-extrabold text-slate-900">
                {{ order.targetQty.toLocaleString() }}
              </td>
              <!-- 지시 상태 -->
              <td class="px-5 py-4 text-center">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold border"
                  :class="{
                    'bg-amber-100 border-amber-200 text-amber-800': order.status === 'READY',
                    'bg-indigo-100 border-indigo-200 text-indigo-800': order.status === 'RUN',
                    'bg-slate-100 border-slate-200 text-slate-500': order.status === 'CLOSE'
                  }"
                >
                  <span
                    class="w-1.5 h-1.5 rounded-full"
                    :class="{
                      'bg-amber-500': order.status === 'READY',
                      'bg-indigo-500': order.status === 'RUN',
                      'bg-slate-400': order.status === 'CLOSE'
                    }"
                  ></span>
                  {{ order.status === 'READY' ? '지시대기' : order.status === 'RUN' ? '진행중' : '마감' }}
                </span>
              </td>
              <!-- 계획 일자 -->
              <td class="px-5 py-4 text-slate-500 font-mono">{{ order.planDate }}</td>
            </tr>
            <tr v-if="filteredOrders.length === 0">
              <td colspan="7" class="px-5 py-12 text-center text-slate-400">
                <FileText class="w-8 h-8 text-slate-300 mx-auto mb-2" />
                조건에 맞는 작업 지시가 존재하지 않습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 디테일 패널: BOM 소요자재 대조 현황 -->
    <div
      v-if="selectedOrder"
      class="bg-white rounded-xl border border-slate-200 shadow-lg overflow-hidden animate-slide-up"
    >
      <div class="px-5 py-4 bg-slate-900 text-white flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Package class="w-5 h-5 text-indigo-400" />
          <h3 class="font-extrabold text-sm">BOM 부품명세 및 가용재고 대조 검증 (작업 지시: {{ selectedOrder.orderNo }})</h3>
        </div>
        <button
          @click="selectedOrder = null"
          class="text-slate-400 hover:text-white text-xs font-bold bg-slate-800 px-2.5 py-1 rounded"
        >
          패널 닫기
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- 메타 요약 정보 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 p-4 bg-slate-50 border border-slate-100 rounded-xl text-sm">
          <div>
            <span class="text-xs text-slate-400 font-bold block">생산 품목</span>
            <span class="font-bold text-slate-800 mt-1 block">{{ selectedOrder.itemName }} ({{ selectedOrder.itemCode }})</span>
          </div>
          <div>
            <span class="text-xs text-slate-400 font-bold block">목표 생산량</span>
            <span class="font-extrabold text-slate-900 mt-1 block">{{ selectedOrder.targetQty.toLocaleString() }} EA</span>
          </div>
          <div>
            <span class="text-xs text-slate-400 font-bold block">출고 적합 여부</span>
            <span class="mt-1 block">
              <span
                v-if="isStockSufficient"
                class="text-emerald-600 font-extrabold flex items-center gap-1"
              >
                <CheckCircle2 class="w-4 h-4 text-emerald-500" /> 출고 가능 (재고 충족)
              </span>
              <span v-else class="text-rose-600 font-extrabold flex items-center gap-1">
                <AlertTriangle class="w-4 h-4 text-rose-500" /> 출고 불가 (재고 부족)
              </span>
            </span>
          </div>
          <div>
            <span class="text-xs text-slate-400 font-bold block">지시 처리 상태</span>
            <span class="font-bold text-indigo-700 mt-1 block uppercase">{{ selectedOrder.status }}</span>
          </div>
        </div>

        <!-- BOM 대조 테이블 -->
        <div class="border border-slate-200 rounded-lg overflow-hidden">
          <table class="w-full text-left border-collapse table-fixed">
            <colgroup>
              <col class="w-[180px]" />
              <col class="w-[280px]" />
              <col class="w-[100px]" />
              <col class="w-[140px]" />
              <col class="w-[140px]" />
              <col class="w-[140px]" />
              <col class="w-[160px]" />
            </colgroup>
            <thead>
              <tr class="bg-slate-50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase tracking-wider">
                <th class="px-5 py-3">자재 코드</th>
                <th class="px-5 py-3">자재명</th>
                <th class="px-5 py-3">단위</th>
                <th class="px-5 py-3 text-right">단위 소요 비율</th>
                <th class="px-5 py-3 text-right">총 필요 수량</th>
                <th class="px-5 py-3 text-right">창고 가용 재고</th>
                <th class="px-5 py-3 text-center">충족 상태</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 text-sm">
              <tr
                v-for="mat in selectedOrderBOMStatus"
                :key="mat.itemCode"
                class="hover:bg-slate-50/50"
                :class="{ 'bg-rose-50/10': !mat.isSufficient }"
              >
                <!-- 자재 코드 -->
                <td class="px-5 py-4 font-mono text-xs font-bold text-slate-900">{{ mat.itemCode }}</td>
                <!-- 자재명 -->
                <td class="px-5 py-4 font-medium text-slate-700 truncate" :title="mat.itemName">
                  {{ mat.itemName }}
                </td>
                <!-- 단위 -->
                <td class="px-5 py-4 text-slate-500 font-bold uppercase">{{ mat.unit }}</td>
                <!-- 단위 소요 비율 -->
                <td class="px-5 py-4 text-right font-mono text-slate-500">
                  {{ Number(mat.bomQuantity).toFixed(2) }}
                </td>
                <!-- 총 필요 수량 -->
                <td class="px-5 py-4 text-right font-bold text-slate-800">
                  {{ mat.requiredQty.toLocaleString() }}
                </td>
                <!-- 창고 가용 재고 -->
                <td class="px-5 py-4 text-right font-extrabold" :class="mat.isSufficient ? 'text-slate-900' : 'text-rose-600'">
                  {{ mat.availableQty.toLocaleString() }}
                </td>
                <!-- 충족 상태 -->
                <td class="px-5 py-4 text-center">
                  <span
                    v-if="mat.isSufficient"
                    class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200"
                  >
                    충족
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200"
                  >
                    부족
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 실행 액션 툴바 -->
        <div class="flex items-center justify-between pt-4 border-t border-slate-100">
          <div class="text-xs text-slate-500">
            <span v-if="selectedOrder.status !== 'READY'">
              이미 불출 처리되었거나 마감된 작업 지시 건입니다. (불출 실행 비활성화)
            </span>
            <span v-else-if="!isStockSufficient" class="text-rose-600 font-bold flex items-center gap-1">
              <AlertTriangle class="w-3.5 h-3.5" /> 자재 창고 재고 부족으로 인해 불출 실행 버튼이 활성화되지 않습니다.
            </span>
            <span v-else class="text-emerald-600 font-bold flex items-center gap-1">
              <CheckCircle2 class="w-3.5 h-3.5" /> 원자재 가용 재고가 충분하여 생산라인으로 안전하게 불출 처리가 가능합니다.
            </span>
          </div>

          <div class="flex gap-2">
            <button
              :disabled="selectedOrder.status !== 'READY' || !isStockSufficient || isSubmitting"
              @click="handleIssueMaterials"
              class="h-10 px-5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg shadow-md flex items-center gap-2 transition"
            >
              <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
              <Play v-else class="w-4 h-4 fill-current" />
              생산 자재 불출 실행
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
