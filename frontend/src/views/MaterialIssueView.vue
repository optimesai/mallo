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
      class="p-4 app-bg-danger-soft border app-border app-text-danger rounded-xl flex items-start gap-3"
    >
      <AlertTriangle class="w-5 h-5 app-text-danger shrink-0 mt-0.5" />
      <div>
        <h4 class="app-alert-title">작업 처리 중 주의사항</h4>
        <p class="app-alert-text">{{ pageError }}</p>
      </div>
    </div>

    <!-- 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="app-page-title">BOM 기반 생산 자재 출고(불출)</h1>
        <p class="app-page-subtitle">대기(READY) 중인 생산 작업 지시의 BOM 구성 소요량을 연산하고 자재 창고 재고를 차감합니다.</p>
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
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <!-- 1) 지시번호 검색 -->
          <div>
            <label class="app-label mb-1.5">작업 지시 번호 / 생산품목</label>
            <div class="relative">
              <input
                v-model="filterOrderNo"
                type="text"
                placeholder="지시번호 또는 완제품명 입력"
                class="app-control app-control-search"
              />
              <Search class="app-search-icon" />
            </div>
          </div>

          <!-- 2) 지시 상태 필터 -->
          <div>
            <label class="app-label mb-1.5">지시 상태</label>
            <select
              v-model="filterStatus"
              class="app-control"
            >
              <option value="ALL">전체 상태</option>
              <option value="READY">지시 대기 (READY)</option>
              <option value="RUN">지시 실행 중 (RUN)</option>
              <option value="HOLD">작업 보류 (HOLD)</option>
            </select>
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

    <!-- 마스터 테이블: 작업 지시 목록 -->
    <div class="app-panel">
      <div class="app-panel-head">
        <span class="app-panel-title">생산 작업 지시 목록 (총 {{ filteredOrders.length }}건)</span>
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
            <tr class="app-bg-muted border-b app-border app-type-xs app-font-strong app-muted uppercase tracking-wider">
              <th class="px-5 py-3">ID</th>
              <th class="px-5 py-3">작업 지시 번호</th>
              <th class="px-5 py-3">생산 품목코드</th>
              <th class="px-5 py-3">생산 품목명</th>
              <th class="px-5 py-3 text-right">목표 생산 수량</th>
              <th class="px-5 py-3 text-center">지시 상태</th>
              <th class="px-5 py-3">계획 일자</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 app-type-sm">
            <tr
              v-for="order in filteredOrders"
              :key="order.orderId"
              @click="selectRow(order)"
              class="app-hover-muted cursor-pointer transition select-none"
              :class="{ 'app-bg-primary-soft': selectedOrder?.orderId === order.orderId }"
            >
              <!-- ID -->
              <td class="px-5 py-4 font-mono app-type-xs app-text-muted">#{{ order.orderId }}</td>
              <!-- 작업 지시 번호 -->
              <td class="px-5 py-4 app-font-strong app-text-strong">{{ order.orderNo }}</td>
              <!-- 생산 품목코드 -->
              <td class="px-5 py-4 font-mono app-type-xs app-muted">{{ order.itemCode }}</td>
              <!-- 생산 품목명 -->
              <td class="px-5 py-4 app-font-label app-text-soft truncate" :title="order.itemName">
                {{ order.itemName }}
              </td>
              <!-- 목표 생산 수량 -->
              <td class="px-5 py-4 text-right app-font-emphasis app-text-strong">
                {{ order.targetQty.toLocaleString() }}
              </td>
              <!-- 지시 상태 -->
              <td class="px-5 py-4 text-center">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full app-type-xs app-font-strong border"
                  :class="{
                    'app-bg-warning-soft app-border app-text-warning': order.status === 'READY',
                    'app-bg-primary-soft app-border app-accent': order.status === 'RUN',
                    'app-bg-muted app-border app-muted': order.status === 'CLOSE'
                  }"
                >
                  <span
                    class="w-1.5 h-1.5 rounded-full"
                    :class="{
                      'app-bg-warning': order.status === 'READY',
                      'app-accent-bg': order.status === 'RUN',
                      'app-bg-success': order.status === 'CLOSE'
                    }"
                  ></span>
                  {{ order.status === 'READY' ? '지시대기' : order.status === 'RUN' ? '진행중' : '마감' }}
                </span>
              </td>
              <!-- 계획 일자 -->
              <td class="px-5 py-4 app-muted font-mono">{{ order.planDate }}</td>
            </tr>
            <tr v-if="filteredOrders.length === 0">
              <td colspan="7" class="px-5 py-12 text-center app-text-muted">
                <FileText class="w-8 h-8 app-text-subtle mx-auto mb-2" />
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
      class="app-panel animate-slide-up"
    >
      <div class="px-5 py-4 app-bg-strong app-text-inverse flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Package class="w-5 h-5 app-accent" />
          <h3 class="app-font-emphasis app-type-sm">BOM 부품명세 및 가용재고 대조 검증 (작업 지시: {{ selectedOrder.orderNo }})</h3>
        </div>
        <button
          @click="selectedOrder = null"
          class="app-text-muted app-type-xs app-font-strong app-bg-muted px-2.5 py-1 rounded"
        >
          패널 닫기
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- 메타 요약 정보 -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 p-4 app-bg-muted border app-border-muted rounded-xl app-type-sm">
          <div>
            <span class="app-type-xs app-text-muted app-font-strong block">생산 품목</span>
            <span class="app-font-strong app-text-strong mt-1 block">{{ selectedOrder.itemName }} ({{ selectedOrder.itemCode }})</span>
          </div>
          <div>
            <span class="app-type-xs app-text-muted app-font-strong block">목표 생산량</span>
            <span class="app-font-emphasis app-text-strong mt-1 block">{{ selectedOrder.targetQty.toLocaleString() }} EA</span>
          </div>
          <div>
            <span class="app-type-xs app-text-muted app-font-strong block">출고 적합 여부</span>
            <span class="mt-1 block">
              <span
                v-if="isStockSufficient"
                class="app-text-success app-font-emphasis flex items-center gap-1"
              >
                <CheckCircle2 class="w-4 h-4 app-text-success" /> 출고 가능 (재고 충족)
              </span>
              <span v-else class="app-text-danger app-font-emphasis flex items-center gap-1">
                <AlertTriangle class="w-4 h-4 app-text-danger" /> 출고 불가 (재고 부족)
              </span>
            </span>
          </div>
          <div>
            <span class="app-type-xs app-text-muted app-font-strong block">지시 처리 상태</span>
            <span class="app-font-strong app-accent mt-1 block uppercase">{{ selectedOrder.status }}</span>
          </div>
        </div>

        <!-- BOM 대조 테이블 -->
        <div class="border app-border rounded-lg overflow-hidden">
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
              <tr class="app-bg-muted border-b app-border app-type-xs app-font-strong app-muted uppercase tracking-wider">
                <th class="px-5 py-3">자재 코드</th>
                <th class="px-5 py-3">자재명</th>
                <th class="px-5 py-3">단위</th>
                <th class="px-5 py-3 text-right">단위 소요 비율</th>
                <th class="px-5 py-3 text-right">총 필요 수량</th>
                <th class="px-5 py-3 text-right">창고 가용 재고</th>
                <th class="px-5 py-3 text-center">충족 상태</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 app-type-sm">
              <tr
                v-for="mat in selectedOrderBOMStatus"
                :key="mat.itemCode"
                class="app-hover-muted/50"
                :class="{ 'app-bg-danger-soft/10': !mat.isSufficient }"
              >
                <!-- 자재 코드 -->
                <td class="px-5 py-4 font-mono app-type-xs app-font-strong app-text-strong">{{ mat.itemCode }}</td>
                <!-- 자재명 -->
                <td class="px-5 py-4 app-font-label app-text-soft truncate" :title="mat.itemName">
                  {{ mat.itemName }}
                </td>
                <!-- 단위 -->
                <td class="px-5 py-4 app-muted app-font-strong uppercase">{{ mat.unit }}</td>
                <!-- 단위 소요 비율 -->
                <td class="px-5 py-4 text-right font-mono app-muted">
                  {{ Number(mat.bomQuantity).toLocaleString() }}
                </td>
                <!-- 총 필요 수량 -->
                <td class="px-5 py-4 text-right app-font-strong app-text-strong">
                  {{ mat.requiredQty.toLocaleString() }}
                </td>
                <!-- 창고 가용 재고 -->
                <td class="px-5 py-4 text-right app-font-emphasis" :class="mat.isSufficient ? 'app-text-strong' : 'app-text-danger'">
                  {{ mat.availableQty.toLocaleString() }}
                </td>
                <!-- 충족 상태 -->
                <td class="px-5 py-4 text-center">
                  <span
                    v-if="mat.isSufficient"
                    class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full app-type-xs app-font-strong app-bg-success-soft app-text-success border app-border"
                  >
                    충족
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full app-type-xs app-font-strong app-bg-danger-soft app-text-danger border app-border"
                  >
                    부족
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 실행 액션 툴바 -->
        <div class="flex items-center justify-between pt-4 border-t app-border-muted">
          <div class="app-type-xs app-muted">
            <span v-if="selectedOrder.status !== 'READY'">
              이미 불출 처리되었거나 마감된 작업 지시 건입니다. (불출 실행 비활성화)
            </span>
            <span v-else-if="!isStockSufficient" class="app-text-danger app-font-strong flex items-center gap-1">
              <AlertTriangle class="w-3.5 h-3.5" /> 자재 창고 재고 부족으로 인해 불출 실행 버튼이 활성화되지 않습니다.
            </span>
            <span v-else class="app-text-success app-font-strong flex items-center gap-1">
              <CheckCircle2 class="w-3.5 h-3.5" /> 원자재 가용 재고가 충분하여 생산라인으로 안전하게 불출 처리가 가능합니다.
            </span>
          </div>

          <div class="flex gap-2">
            <button
              :disabled="selectedOrder.status !== 'READY' || !isStockSufficient || isSubmitting"
              @click="handleIssueMaterials"
              class="h-10 px-5 app-type-xs app-font-strong app-text-inverse app-accent-bg app-hover-muted disabled:opacity-50 disabled:cursor-not-allowed rounded-lg shadow-md flex items-center gap-2 transition"
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
