<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  AlertTriangle,
  CheckCircle2,
  ClipboardCheck,
  FileText,
  Gauge,
  Loader2,
  Lock,
  Package,
  RefreshCw,
  Search,
  Trash2
} from '@lucide/vue'
import { useWorkOrderStore } from '@/state/workOrderStore'
import { useAuthStore } from '@/state/authStore'
import { useFactoryRoutingStore } from '@/state/factoryRoutingStore'
import { useInboundStore } from '@/state/inboundStore'
import { formatDateTime } from '@/utils/dateFormat'
import type { FactoryRoutingResponse } from '@/api/factoryRoutingApi'
import type { LocationResponse } from '@/api/inboundApi'
import type { ProductionExecutionCreateRequest, WorkOrderOperationProgressResponse, WorkOrderResponse, WorkOrderSearchParams, WorkOrderStatus } from '@/api/workOrderApi'

type ProductionExecutionTab = 'register' | 'lookup'
type ExecutionSortOrder = 'latest' | 'oldest'
type ExecutionQtyFilter = 'ALL' | 'GOOD' | 'DEFECT'

const workOrderStore = useWorkOrderStore()
const authStore = useAuthStore()
const factoryRoutingStore = useFactoryRoutingStore()
const inboundStore = useInboundStore()

const routings = ref<FactoryRoutingResponse[]>([])
const locations = ref<LocationResponse[]>([])
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const activeTab = ref<ProductionExecutionTab>('register')
const selectedOrderKey = ref<string | null>(null)
const keywordInput = ref('')
const statusInput = ref<WorkOrderStatus | 'ALL'>('RUN')
const factoryInput = ref('')
const lineInput = ref('')
const routingInput = ref<number | ''>('')
const appliedKeyword = ref('')
const appliedStatus = ref<WorkOrderStatus | 'ALL'>('RUN')
const appliedFactory = ref('')
const appliedLine = ref('')
const appliedRoutingId = ref<number | ''>('')
const isKeywordSuggestOpen = ref(false)
const isExecutionHistoryOpen = ref(true)
const isMaterialRequirementOpen = ref(true)
const executionSortOrder = ref<ExecutionSortOrder>('latest')
const executionOperationFilter = ref<number | 'ALL'>('ALL')
const executionWorkerFilter = ref<string>('ALL')
const executionQtyFilter = ref<ExecutionQtyFilter>('ALL')
const orderSortField = ref<keyof WorkOrderResponse>('planDate')
const orderSortDirection = ref<'asc' | 'desc'>('desc')

const form = reactive<Omit<ProductionExecutionCreateRequest, 'orderKey'>>({
  routingId: 0,
  goodQty: 0,
  defectQty: 0,
  defectType: '',
  defectReason: '',
  reworkable: false,
  receiptLocationCode: '',
  manHoursMinutes: 0
})

const selectedDetail = computed(() => workOrderStore.selectedDetail)
const selectedOrder = computed(() => selectedDetail.value?.workOrder ?? null)
const executions = computed(() => selectedDetail.value?.executions ?? [])
const materialRequirements = computed(() => selectedDetail.value?.materialRequirements ?? [])
const operationProgresses = computed(() => selectedDetail.value?.operationProgresses ?? [])
const canSubmitExecution = computed(() => Boolean(selectedOrder.value?.canRegisterExecution))
const hasStockShortage = computed(() => materialRequirements.value.some((item) => item.availableQty < getRemainingQty(item.requiredQty, item.issuedQty)))
const executionValidationMessages = computed(() => {
  const messages: string[] = []

  if (!selectedOrder.value) messages.push('작업 지시를 먼저 선택해주세요.')
  else if (!canSubmitExecution.value) messages.push('진행(RUN) 상태의 작업 지시에만 실적을 등록할 수 있습니다.')

  if (!form.routingId) messages.push('실제 수행 공정을 선택해주세요.')
  if (selectedOrder.value && executionRoutingOptions.value.length === 0) messages.push('현재 등록 가능한 공정이 없습니다.')
  if (form.goodQty + form.defectQty <= 0) messages.push('양품과 불량 수량의 합계는 1 이상이어야 합니다.')
  if (selectedOperationProgress.value && form.goodQty + form.defectQty > getRemainingOperationQty(selectedOperationProgress.value)) messages.push('선택 공정의 잔여 수량을 초과할 수 없습니다.')
  if (form.defectQty > 0 && !form.defectReason?.trim()) messages.push('불량 수량이 있으면 불량 사유를 입력해주세요.')
  if (form.manHoursMinutes <= 0) messages.push('총 소요 시간은 1분 이상이어야 합니다.')

  return messages
})
const hasExecutionValidationError = computed(() => executionValidationMessages.value.length > 0)

const filteredOrders = computed(() => workOrderStore.workOrders)

const runnableOrders = computed(() => workOrderStore.workOrders.filter((order) => order.status === 'RUN').length)
const closeLockedOrders = computed(() => workOrderStore.workOrders.filter((order) => order.status === 'CLOSE').length)
const totalDefectRate = computed(() => {
  if (!selectedOrder.value || selectedOrder.value.totalExecutedQty === 0) return 0
  return Math.round((selectedOrder.value.totalDefectQty * 10000) / selectedOrder.value.totalExecutedQty) / 100
})
const hasNoRoutings = computed(() => routings.value.length === 0)
const factoryOptions = computed(() => [...new Set(routings.value.map((routing) => routing.factoryName))])
const lineOptions = computed(() => [...new Set(routings.value
  .filter((routing) => !factoryInput.value || routing.factoryName === factoryInput.value)
  .map((routing) => routing.lineName))])
const operationOptions = computed(() => routings.value.filter((routing) => {
  if (factoryInput.value && routing.factoryName !== factoryInput.value) return false
  if (lineInput.value && routing.lineName !== lineInput.value) return false
  return true
}))
const executionRoutingOptions = computed(() => {
  return operationProgresses.value
})
const currentOperationProgress = computed(() => {
  return operationProgresses.value.find((progress) => progress.currentOperation)
    ?? operationProgresses.value[0]
    ?? null
})
const selectedOperationProgress = computed(() => {
  return operationProgresses.value.find((progress) => progress.routingId === form.routingId) ?? null
})
const keywordSuggestions = computed(() => {
  const query = keywordInput.value.trim().toLowerCase()
  if (!query) return []

  return workOrderStore.workOrders
    .filter((order) => [
      order.orderNo,
      order.itemCode,
      order.itemName,
      order.factoryName,
      order.lineName,
      order.operationName
    ].some((value) => value.toLowerCase().includes(query)))
    .slice(0, 8)
    .map((order) => ({
      orderNo: order.orderNo,
      itemName: order.itemName,
      itemCode: order.itemCode,
      status: order.status,
      routingText: `${order.factoryName} / ${order.lineName} / ${getOrderOperationLabel(order)}`
    }))
})
const executionOperationOptions = computed(() => {
  const unique = new Map<number, string>()
  executions.value.forEach((execution) => {
    if (execution.routingId && execution.operationName) {
      unique.set(execution.routingId, `${execution.operationSeq ?? '-'}. ${execution.operationName}`)
    }
  })
  return [...unique.entries()].map(([routingId, label]) => ({ routingId, label }))
})
const executionWorkerOptions = computed(() => {
  const unique = new Map<string, string>()
  executions.value.forEach((execution) => {
    const key = execution.workerEmployeeNo ?? execution.workerName ?? ''
    if (!key) return
    unique.set(key, `${execution.workerName ?? '-'} · ${execution.workerEmployeeNo ?? '-'}`)
  })
  return [...unique.entries()].map(([key, label]) => ({ key, label }))
})
const filteredExecutions = computed(() => {
  return [...executions.value]
    .filter((execution) => executionOperationFilter.value === 'ALL' || execution.routingId === executionOperationFilter.value)
    .filter((execution) => {
      if (executionWorkerFilter.value === 'ALL') return true
      return execution.workerEmployeeNo === executionWorkerFilter.value || execution.workerName === executionWorkerFilter.value
    })
    .filter((execution) => {
      if (executionQtyFilter.value === 'GOOD') return execution.goodQty > 0
      if (executionQtyFilter.value === 'DEFECT') return execution.defectQty > 0
      return true
    })
    .sort((a, b) => {
      const aTime = new Date(a.createdAt).getTime()
      const bTime = new Date(b.createdAt).getTime()
      if (Number.isNaN(aTime) || Number.isNaN(bTime)) return 0
      return executionSortOrder.value === 'latest' ? bTime - aTime : aTime - bTime
    })
})

watch(selectedOrder, (order) => {
  form.routingId = order ? currentOperationProgress.value?.routingId ?? 0 : 0
  form.goodQty = 0
  form.defectQty = 0
  form.defectType = ''
  form.defectReason = ''
  form.reworkable = false
  form.receiptLocationCode = ''
  form.manHoursMinutes = 0
  executionSortOrder.value = 'latest'
  executionOperationFilter.value = 'ALL'
  executionWorkerFilter.value = 'ALL'
  executionQtyFilter.value = 'ALL'
})

watch(factoryInput, () => {
  if (lineInput.value && !lineOptions.value.includes(lineInput.value)) lineInput.value = ''
  if (routingInput.value && !operationOptions.value.some((routing) => routing.routingId === routingInput.value)) routingInput.value = ''
})

watch(lineInput, () => {
  if (routingInput.value && !operationOptions.value.some((routing) => routing.routingId === routingInput.value)) routingInput.value = ''
})

onMounted(async () => {
  await loadInitialData()
})

function showToast(message: string) {
  successToast.value = message
  setTimeout(() => {
    successToast.value = null
  }, 3500)
}

function getStatusLabel(status: WorkOrderStatus) {
  const labels: Record<WorkOrderStatus, string> = {
    READY: '대기',
    RUN: '진행',
    HOLD: '보류',
    CLOSE: '마감'
  }
  return labels[status]
}

function getStatusClass(status: WorkOrderStatus) {
  if (status === 'READY') return 'app-status-warning'
  if (status === 'RUN') return 'app-status-progress'
  if (status === 'CLOSE') return 'app-status-success'
  return 'app-status-neutral'
}

function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString()
}

function getRemainingQty(requiredQty: number, issuedQty: number) {
  return Math.max(requiredQty - issuedQty, 0)
}

function getRemainingOperationQty(progress: WorkOrderOperationProgressResponse) {
  return Math.max(progress.availableQty - progress.completedQty, 0)
}

function getOperationProgressStatusLabel(progress: WorkOrderOperationProgressResponse) {
  if (progress.currentOperation) return '진행 가능'
  if (progress.completed) return '완료'
  return '대기'
}

function getOrderOperationLabel(order: WorkOrderResponse) {
  const operationSeq = order.currentOperationSeq ?? order.operationSeq
  const operationName = order.currentOperationName ?? order.operationName
  return `${operationSeq}. ${operationName}`
}

async function loadInitialData() {
  pageError.value = null
  const results = await Promise.allSettled([
    loadExecutionOrders(0),
    factoryRoutingStore.loadRoutings(),
    inboundStore.loadLocations()
  ])

  if (results[1].status === 'fulfilled') {
    routings.value = [...factoryRoutingStore.routings]
  }
  if (results[2].status === 'fulfilled') {
    locations.value = [...inboundStore.locations]
  }

  const labels = ['작업지시 목록', '공장/생산 라우팅', '입고 위치']
  const failedMessages = results
    .map((result, index) => {
      if (result.status === 'fulfilled') return ''
      const message = result.reason instanceof Error ? result.reason.message : '서버 오류가 발생했습니다.'
      return `${labels[index]} 조회 실패: ${message}`
    })
    .filter(Boolean)

  if (failedMessages.length > 0) {
    pageError.value = failedMessages.join('\n')
  }
}

function buildOrderSearchParams(page = 0): WorkOrderSearchParams {
  const operationName = appliedRoutingId.value
    ? routings.value.find((routing) => routing.routingId === appliedRoutingId.value)?.operationName
    : ''
  const keyword = appliedKeyword.value.trim()
  return {
    page,
    size: 10,
    sort: `${String(orderSortField.value)},${orderSortDirection.value}`,
    status: appliedStatus.value === 'ALL' ? '' : appliedStatus.value,
    keyword,
    factoryName: appliedFactory.value,
    lineName: appliedLine.value,
    operationName
  }
}

async function loadExecutionOrders(page = 0) {
  pageError.value = null
  try {
    await workOrderStore.loadWorkOrders(buildOrderSearchParams(page))
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업 지시 목록 조회에 실패했습니다.'
    throw err
  }
}

async function changeOrderSort(field: keyof WorkOrderResponse) {
  if (orderSortField.value === field) {
    orderSortDirection.value = orderSortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    orderSortField.value = field
    orderSortDirection.value = 'asc'
  }
  await loadExecutionOrders(0)
}

function getOrderSortMark(field: keyof WorkOrderResponse) {
  if (orderSortField.value !== field) return ''
  return orderSortDirection.value === 'asc' ? '▲' : '▼'
}

async function refreshData() {
  await loadInitialData()
  if (selectedOrderKey.value) {
    await selectOrder(selectedOrderKey.value)
  }
  showToast('생산 실적 데이터가 새로고침되었습니다.')
}

async function selectOrder(orderKey: string) {
  pageError.value = null
  try {
    selectedOrderKey.value = orderKey
    await workOrderStore.loadWorkOrder(orderKey)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업 지시 상세 조회에 실패했습니다.'
  }
}

function validateExecutionForm() {
  if (!selectedOrder.value) return '작업 지시를 먼저 선택해주세요.'
  if (!canSubmitExecution.value) return '진행(RUN) 상태의 작업 지시에만 실적을 등록할 수 있습니다.'
  if (!form.routingId) return '실제 수행 공정을 선택해주세요.'
  if (!executionRoutingOptions.value.some((progress) => progress.routingId === form.routingId)) return '선택 작업건의 공정을 선택해주세요.'
  if (form.goodQty < 0 || form.defectQty < 0) return '양품/불량 수량은 0 이상이어야 합니다.'
  if (form.goodQty + form.defectQty <= 0) return '양품과 불량 수량의 합계는 1 이상이어야 합니다.'
  if (selectedOperationProgress.value && form.goodQty + form.defectQty > getRemainingOperationQty(selectedOperationProgress.value)) return '선택 공정의 잔여 수량을 초과할 수 없습니다.'
  if (form.defectQty > 0 && !form.defectReason?.trim()) return '불량 수량이 있으면 불량 사유를 입력해주세요.'
  if (form.manHoursMinutes < 1) return '총 소요 시간은 1분 이상이어야 합니다.'
  return null
}

async function submitExecution() {
  const message = validateExecutionForm()
  if (message) {
    pageError.value = message
    return
  }
  if (!selectedOrder.value) return

  pageError.value = null
  try {
    await workOrderStore.createExecution({
      orderKey: selectedOrder.value.orderNo,
      routingId: Number(form.routingId),
      goodQty: Number(form.goodQty),
      defectQty: Number(form.defectQty),
      defectType: form.defectType?.trim() || undefined,
      defectReason: form.defectReason?.trim() || undefined,
      reworkable: Boolean(form.reworkable),
      receiptLocationCode: form.receiptLocationCode || undefined,
      manHoursMinutes: Number(form.manHoursMinutes)
    })
    showToast(`작업 지시 [${selectedOrder.value.orderNo}] 실적이 등록되었습니다.`)
    form.goodQty = 0
    form.defectQty = 0
    form.defectType = ''
    form.defectReason = ''
    form.reworkable = false
    form.receiptLocationCode = ''
    form.manHoursMinutes = 0
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '생산 실적 등록에 실패했습니다.'
  }
}

async function deleteExecution(executionId: number) {
  if (!selectedOrder.value) return
  if (!confirm('선택한 생산 실적을 삭제하시겠습니까?')) return

  pageError.value = null
  try {
    await workOrderStore.deleteExecution(executionId)
    showToast('생산 실적이 삭제되었습니다.')
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '생산 실적 삭제에 실패했습니다.'
  }
}

async function selectRunnableFilter() {
  statusInput.value = 'RUN'
  appliedStatus.value = 'RUN'
  await loadExecutionOrders(0)
}

async function applyOrderSearch() {
  appliedKeyword.value = keywordInput.value
  appliedStatus.value = statusInput.value
  appliedFactory.value = factoryInput.value
  appliedLine.value = lineInput.value
  appliedRoutingId.value = routingInput.value
  isKeywordSuggestOpen.value = false
  await loadExecutionOrders(0)
}

async function resetOrderSearch() {
  keywordInput.value = ''
  statusInput.value = 'RUN'
  factoryInput.value = ''
  lineInput.value = ''
  routingInput.value = ''
  appliedKeyword.value = ''
  appliedStatus.value = 'RUN'
  appliedFactory.value = ''
  appliedLine.value = ''
  appliedRoutingId.value = ''
  isKeywordSuggestOpen.value = false
  await loadExecutionOrders(0)
}

function selectKeywordSuggestion(orderNo: string) {
  keywordInput.value = orderNo
  isKeywordSuggestOpen.value = false
}
</script>

<template>
  <div class="wo-page">
    <Transition name="wo-toast">
      <div v-if="successToast" class="wo-toast">
        <span class="wo-toast-dot"></span>
        <span>{{ successToast }}</span>
      </div>
    </Transition>

    <div v-if="pageError" class="wo-alert wo-alert-danger">
      <AlertTriangle class="wo-icon" />
      <span>{{ pageError }}</span>
    </div>

    <header class="wo-header">
      <div>
        <h1 class="app-page-title">
          공정 실적 관리
        </h1>
        <p class="app-page-subtitle">진행 중인 작업 지시를 기준으로 공정별 생산 실적을 입력하고 BOM 투입량 변화를 확인합니다.</p>
      </div>
      <button class="app-button app-button-muted" :disabled="workOrderStore.isLoading" @click="refreshData">
        <RefreshCw class="wo-button-icon" />
        새로고침
      </button>
    </header>

    <div v-if="hasNoRoutings" class="wo-alert wo-alert-danger">
      <AlertTriangle class="wo-icon" />
      <span>등록된 공장/생산 라우팅 기준정보가 없어 공정 실적을 등록할 수 없습니다.</span>
      <RouterLink v-if="authStore.canManageMasterData" class="app-button app-button-muted" to="/master/factory-lines">
        기준정보 등록
      </RouterLink>
    </div>

    <section class="app-news-grid md:grid-cols-4">
      <div class="app-news-card">
        <div>
          <p class="app-news-label">전체 작업 지시</p>
          <strong class="app-news-value">{{ formatNumber(workOrderStore.workOrders.length) }}</strong>
        </div>
        <div class="app-news-icon"><FileText /></div>
      </div>
      <div class="app-news-card">
        <div>
          <p class="app-news-label app-text-success">실적 등록 가능</p>
          <strong class="app-news-value app-text-success">{{ formatNumber(runnableOrders) }}</strong>
        </div>
        <div class="app-news-icon app-bg-success-soft app-text-success"><ClipboardCheck /></div>
      </div>
      <div class="app-news-card">
        <div>
          <p class="app-news-label app-text-warning">마감 작업</p>
          <strong class="app-news-value app-text-warning">{{ formatNumber(closeLockedOrders) }}</strong>
        </div>
        <div class="app-news-icon app-bg-warning-soft app-text-warning"><Lock /></div>
      </div>
      <div class="app-news-card">
        <div>
          <p class="app-news-label">선택 작업 불량률</p>
          <strong class="app-news-value app-text-strong">{{ totalDefectRate }}%</strong>
        </div>
        <div class="app-news-icon"><Gauge /></div>
      </div>
    </section>

    <nav class="wo-tabs" aria-label="공정 실적 기능 탭">
      <button class="wo-tab" :class="{ 'is-active': activeTab === 'register' }" @click="activeTab = 'register'">실적 등록</button>
      <button class="wo-tab" :class="{ 'is-active': activeTab === 'lookup' }" @click="activeTab = 'lookup'">실적/투입 조회</button>
    </nav>

    <section class="pe-layout">
      <section class="app-panel pe-order-panel">
        <div class="app-panel-head">
          <div class="app-panel-title">
            <FileText class="app-panel-icon" />
            <h2>작업 지시 선택</h2>
          </div>
          <button class="app-button app-button-muted" @click="selectRunnableFilter">RUN 보기</button>
        </div>
        <div class="app-filter-body">
          <div class="pe-filter-row pe-filter-row-main">
            <label class="app-field wo-autocomplete">
              <span class="app-label">검색어</span>
              <div class="app-search-box">
                <Search class="app-search-icon" />
                <input
                  v-model="keywordInput"
                  class="app-control app-control-search"
                  placeholder="작업지시번호, 품목, 공정 검색"
                  @focus="isKeywordSuggestOpen = true"
                  @input="isKeywordSuggestOpen = true"
                  @keyup.enter="applyOrderSearch"
                />
              </div>
              <div v-if="isKeywordSuggestOpen && keywordSuggestions.length > 0" class="wo-suggest-list">
                <button
                  v-for="candidate in keywordSuggestions"
                  :key="candidate.orderNo"
                  type="button"
                  class="wo-suggest-item"
                  @mousedown.prevent="selectKeywordSuggestion(candidate.orderNo)"
                >
                  <strong>{{ candidate.orderNo }}</strong>
                  <span>{{ candidate.itemName }} · {{ candidate.itemCode }} · {{ candidate.routingText }} · {{ getStatusLabel(candidate.status) }}</span>
                </button>
              </div>
            </label>
            <label class="app-field">
              <span class="app-label">상태</span>
              <select v-model="statusInput" class="app-control">
                <option value="ALL">전체</option>
                <option value="RUN">진행</option>
                <option value="HOLD">보류</option>
                <option value="CLOSE">마감</option>
                <option value="READY">대기</option>
              </select>
            </label>
          </div>
          <div class="pe-filter-row pe-filter-row-routing">
            <label class="app-field">
              <span class="app-label">공장</span>
              <select v-model="factoryInput" class="app-control">
                <option value="">전체 공장</option>
                <option v-for="factory in factoryOptions" :key="factory" :value="factory">{{ factory }}</option>
              </select>
            </label>
            <label class="app-field">
              <span class="app-label">라인</span>
              <select v-model="lineInput" class="app-control" :disabled="!factoryInput">
                <option value="">전체 라인</option>
                <option v-for="line in lineOptions" :key="line" :value="line">{{ line }}</option>
              </select>
            </label>
            <label class="app-field">
              <span class="app-label">공정</span>
              <select v-model.number="routingInput" class="app-control">
                <option value="">전체 공정</option>
                <option v-for="routing in operationOptions" :key="routing.routingId" :value="routing.routingId">
                  {{ routing.operationSeq }}. {{ routing.operationName }}
                </option>
              </select>
            </label>
          </div>
          <div class="app-actions-end pe-filter-actions">
            <button class="app-button app-button-muted" type="button" @click="resetOrderSearch">초기화</button>
            <button class="app-button app-button-primary" type="button" @click="applyOrderSearch">
              <Search class="wo-button-icon" />
              조회
            </button>
          </div>
        </div>
        <div class="pe-order-list app-table-wrap">
          <div class="app-list-head">
            <span class="app-list-title">작업 지시 목록</span>
          </div>
          <table class="app-table pe-order-table">
            <thead>
              <tr>
                <th class="app-sortable-header text-center" @click="changeOrderSort('orderId')">No <span class="app-sort-mark">{{ getOrderSortMark('orderId') }}</span></th>
                <th class="app-sortable-header" @click="changeOrderSort('orderNo')">작업지시번호 <span class="app-sort-mark">{{ getOrderSortMark('orderNo') }}</span></th>
                <th class="app-sortable-header" @click="changeOrderSort('itemName')">품목 <span class="app-sort-mark">{{ getOrderSortMark('itemName') }}</span></th>
                <th class="app-sortable-header" @click="changeOrderSort('factoryName')">공장 <span class="app-sort-mark">{{ getOrderSortMark('factoryName') }}</span></th>
                <th class="app-sortable-header" @click="changeOrderSort('lineName')">라인 <span class="app-sort-mark">{{ getOrderSortMark('lineName') }}</span></th>
                <th class="app-sortable-header" @click="changeOrderSort('operationSeq')">공정 <span class="app-sort-mark">{{ getOrderSortMark('operationSeq') }}</span></th>
                <th class="app-sortable-header text-center" @click="changeOrderSort('status')">상태 <span class="app-sort-mark">{{ getOrderSortMark('status') }}</span></th>
                <th class="app-sortable-header" @click="changeOrderSort('progressRate')">진행률 <span class="app-sort-mark">{{ getOrderSortMark('progressRate') }}</span></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="workOrderStore.isLoading">
                <td colspan="8" class="app-empty">
                  <Loader2 class="mx-auto mb-2 h-5 w-5 app-spin" />
                  작업 지시를 불러오고 있습니다.
                </td>
              </tr>
              <tr v-else-if="filteredOrders.length === 0">
                <td colspan="8" class="app-empty">
                  <FileText class="mx-auto mb-2 h-7 w-7" />
                  조건에 맞는 작업 지시가 없습니다.
                </td>
              </tr>
              <template v-else>
                <tr
                  v-for="(order, idx) in filteredOrders"
                  :key="order.orderId"
                  class="app-table-row"
                  :class="{ 'app-table-row-selected': selectedOrder?.orderId === order.orderId }"
                  @click="selectOrder(order.orderNo)"
                >
                  <td class="text-center app-table-id">{{ workOrderStore.page * workOrderStore.size + idx + 1 }}</td>
                  <td><strong class="app-table-id">{{ order.orderNo }}</strong></td>
                  <td><strong class="app-table-main">{{ order.itemName }}</strong><span class="app-table-muted">{{ order.itemCode }}</span></td>
                  <td>{{ order.factoryName }}</td>
                  <td>{{ order.lineName }}</td>
                  <td><strong class="app-table-main">{{ getOrderOperationLabel(order) }}</strong></td>
                  <td class="text-center">
                    <span class="app-status" :class="getStatusClass(order.status)">{{ getStatusLabel(order.status) }}</span>
                  </td>
                  <td>
                    <div class="pe-order-progress-cell">
                      <span class="wo-progress"><span :style="{ width: `${Math.min(order.progressRate, 100)}%` }"></span></span>
                      <span class="wo-progress-label">{{ order.progressRate }}%</span>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <div class="app-pagination">
          <span>
            총 {{ formatNumber(workOrderStore.totalElements) }}건
            · {{ workOrderStore.totalElements === 0 ? 0 : workOrderStore.page * workOrderStore.size + 1 }}-{{ Math.min((workOrderStore.page + 1) * workOrderStore.size, workOrderStore.totalElements) }} 표시
            · 10건씩
            · {{ workOrderStore.page + 1 }} / {{ Math.max(workOrderStore.totalPages, 1) }} 페이지
          </span>
          <div class="app-pagination-actions">
            <button
              class="app-page-button"
              :disabled="workOrderStore.page <= 0 || workOrderStore.isLoading"
              @click="loadExecutionOrders(0)"
            >
              처음
            </button>
            <button
              class="app-page-button"
              :disabled="workOrderStore.page <= 0 || workOrderStore.isLoading"
              @click="loadExecutionOrders(workOrderStore.page - 1)"
            >
              이전
            </button>
            <button
              class="app-page-button"
              :disabled="workOrderStore.page + 1 >= workOrderStore.totalPages || workOrderStore.isLoading"
              @click="loadExecutionOrders(workOrderStore.page + 1)"
            >
              다음
            </button>
            <button
              class="app-page-button"
              :disabled="workOrderStore.page + 1 >= workOrderStore.totalPages || workOrderStore.isLoading"
              @click="loadExecutionOrders(workOrderStore.totalPages - 1)"
            >
              마지막
            </button>
          </div>
        </div>
      </section>

      <section class="pe-main">
        <section v-if="selectedOrder" class="app-panel">
          <div class="wo-detail-head">
            <div>
              <p class="wo-kicker">SELECTED WORK ORDER</p>
              <h2 class="app-panel-title">{{ selectedOrder.orderNo }}</h2>
            </div>
            <span class="wo-status" :data-status="selectedOrder.status">{{ getStatusLabel(selectedOrder.status) }}</span>
          </div>
          <div class="wo-metric-grid">
            <div class="wo-metric"><span>목표 수량</span><strong>{{ formatNumber(selectedOrder.targetQty) }}</strong></div>
            <div class="wo-metric"><span>누적 양품</span><strong>{{ formatNumber(selectedOrder.totalGoodQty) }}</strong></div>
            <div class="wo-metric"><span>누적 불량</span><strong>{{ formatNumber(selectedOrder.totalDefectQty) }}</strong></div>
            <div class="wo-metric"><span>총 공수</span><strong>{{ formatNumber(selectedOrder.totalManHoursMinutes) }}분</strong></div>
          </div>
          <div class="wo-info-list">
            <div><span>생산 품목</span><strong>{{ selectedOrder.itemCode }} · {{ selectedOrder.itemName }}</strong></div>
            <div><span>생산 라인</span><strong>{{ selectedOrder.factoryName }} / {{ selectedOrder.lineName }}</strong></div>
            <div><span>계획일</span><strong>{{ selectedOrder.planDate }}</strong></div>
            <div><span>진행률</span><strong>{{ selectedOrder.progressRate }}% · 총 실적 {{ formatNumber(selectedOrder.totalExecutedQty) }}</strong></div>
          </div>
        </section>

        <section v-else class="app-panel wo-empty-panel">
          <ClipboardCheck class="wo-empty-icon" />
          <strong>작업 지시를 선택해주세요.</strong>
          <span>실적 등록과 투입량 확인은 작업 지시 상세 조회 후 가능합니다.</span>
        </section>

        <section v-if="selectedOrder && activeTab === 'register'" class="wo-tab-section">
          <div class="app-panel">
            <div class="app-panel-head">
              <div class="wo-head-inline">
                <ClipboardCheck class="wo-icon" />
                <h2 class="app-panel-title">실제 생산실적 등록</h2>
              </div>
              <span v-if="!canSubmitExecution" class="wo-badge-danger">RUN 상태 필요</span>
            </div>
            <div class="wo-table-wrap">
              <table class="app-table wo-table-compact">
                <thead>
                  <tr>
                    <th>공정</th>
                    <th>처리 가능</th>
                    <th>완료</th>
                    <th>잔여</th>
                    <th>상태</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="progress in operationProgresses" :key="progress.routingId" :class="{ 'wo-row-selected': progress.currentOperation }">
                    <td><strong>{{ progress.operationSeq }}. {{ progress.operationName }}</strong></td>
                    <td class="wo-number">{{ formatNumber(progress.availableQty) }}</td>
                    <td class="wo-number">{{ formatNumber(progress.completedGoodQty) }} / 불량 {{ formatNumber(progress.completedDefectQty) }}</td>
                    <td class="wo-number">{{ formatNumber(getRemainingOperationQty(progress)) }}</td>
                    <td>
                      <span v-if="progress.currentOperation" class="wo-status" data-status="RUN">진행 가능</span>
                      <span v-else-if="progress.completed" class="wo-status" data-status="CLOSE">완료</span>
                      <span v-else class="wo-status" data-status="READY">대기</span>
                    </td>
                  </tr>
                  <tr v-if="operationProgresses.length === 0">
                    <td colspan="5" class="wo-empty">등록된 라인 공정 정보가 없습니다.</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="wo-form-grid wo-form-grid-execution">
              <label class="wo-field">
                <span class="wo-label">실제 수행 공정</span>
                <select v-model.number="form.routingId" class="app-control" :disabled="!canSubmitExecution">
                  <option :value="0">공정 선택</option>
                  <option v-for="progress in executionRoutingOptions" :key="progress.routingId" :value="progress.routingId">
                    {{ progress.operationSeq }}. {{ progress.operationName }} · 잔여 {{ formatNumber(getRemainingOperationQty(progress)) }} · {{ getOperationProgressStatusLabel(progress) }}
                  </option>
                </select>
                <span class="wo-field-help">선택 작업건의 라인 공정을 모두 표시하며, 등록 가능 수량은 서버에서 최종 검증됩니다.</span>
              </label>
              <label class="wo-field">
                <span class="wo-label">양품 수량</span>
                <input v-model.number="form.goodQty" class="app-control" type="number" min="0" :disabled="!canSubmitExecution" />
              </label>
              <label class="wo-field">
                <span class="wo-label">불량 수량</span>
                <input v-model.number="form.defectQty" class="app-control" type="number" min="0" :disabled="!canSubmitExecution" />
              </label>
              <label class="wo-field">
                <span class="wo-label">불량 유형</span>
                <input v-model="form.defectType" class="app-control" placeholder="예) 치수, 외관, 조립" :disabled="!canSubmitExecution || form.defectQty <= 0" />
              </label>
              <label class="wo-field">
                <span class="wo-label">불량 사유</span>
                <input v-model="form.defectReason" class="app-control" placeholder="불량 원인 입력" :disabled="!canSubmitExecution || form.defectQty <= 0" />
              </label>
              <label class="wo-field">
                <span class="wo-label">재작업 여부</span>
                <select v-model="form.reworkable" class="app-control" :disabled="!canSubmitExecution || form.defectQty <= 0">
                  <option :value="false">재작업 불가</option>
                  <option :value="true">재작업 가능</option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">생산 입고 로케이션</span>
                <select v-model="form.receiptLocationCode" class="app-control" :disabled="!canSubmitExecution || form.goodQty <= 0">
                  <option value="">기본 로케이션</option>
                  <option v-for="location in locations" :key="location.locationId" :value="location.locationCode">
                    {{ location.locationCode }} · {{ location.warehouseName }}{{ location.productionReceiptDefault ? ' · 기본' : '' }}
                  </option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">총 소요 시간(분)</span>
                <input v-model.number="form.manHoursMinutes" class="app-control" type="number" min="1" :disabled="!canSubmitExecution" />
              </label>
            </div>
            <div class="wo-preview pe-preview-note">
              <div>
                <span class="wo-preview-label">작업자 기록</span>
                <strong>로그인 사용자 기준 자동 저장</strong>
              </div>
              <div>
                <span class="wo-preview-label">투입량 처리</span>
                <strong>자재 출고 처리된 수량 안에서 공정 실적 등록</strong>
              </div>
            </div>
            <div class="pe-validation-box" :class="{ 'is-clear': !hasExecutionValidationError }">
              <AlertTriangle v-if="hasExecutionValidationError" class="wo-icon" />
              <CheckCircle2 v-else class="wo-icon" />
              <div>
                <strong>{{ hasExecutionValidationError ? '실적 등록 전 확인 필요' : '실적 등록 입력값이 유효합니다.' }}</strong>
                <span v-if="!hasExecutionValidationError">등록 버튼을 누르면 선택 작업 지시에 실적이 반영됩니다.</span>
                <template v-else>
                  <span v-for="message in executionValidationMessages" :key="message">{{ message }}</span>
                </template>
              </div>
            </div>
            <div class="wo-toolbar wo-toolbar-end">
              <button class="app-button app-button-primary" :disabled="hasExecutionValidationError || workOrderStore.isSaving" @click="submitExecution">
                <Loader2 v-if="workOrderStore.isSaving" class="wo-button-icon wo-spin" />
                <CheckCircle2 v-else class="wo-button-icon" />
                실적 등록
              </button>
            </div>
          </div>
        </section>

        <section v-if="selectedOrder && activeTab === 'lookup'" class="wo-tab-section">
          <div class="app-panel wo-execution-list-panel">
            <div class="app-panel-head">
              <h2 class="app-panel-title">생산 실적 이력</h2>
              <div class="wo-head-inline">
                <span class="wo-count">총 {{ filteredExecutions.length }}건</span>
                <button class="app-button app-button-muted" type="button" @click="isExecutionHistoryOpen = !isExecutionHistoryOpen">
                  {{ isExecutionHistoryOpen ? '접기' : '펼치기' }}
                </button>
              </div>
            </div>
            <div v-show="isExecutionHistoryOpen" class="pe-lookup-filter-box">
              <label class="wo-field">
                <span class="wo-label">등록일시 정렬</span>
                <select v-model="executionSortOrder" class="app-control">
                  <option value="latest">최신순</option>
                  <option value="oldest">오래된순</option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">공정</span>
                <select v-model.number="executionOperationFilter" class="app-control">
                  <option value="ALL">전체 공정</option>
                  <option v-for="operation in executionOperationOptions" :key="operation.routingId" :value="operation.routingId">
                    {{ operation.label }}
                  </option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">작업자</span>
                <select v-model="executionWorkerFilter" class="app-control">
                  <option value="ALL">전체 작업자</option>
                  <option v-for="worker in executionWorkerOptions" :key="worker.key" :value="worker.key">
                    {{ worker.label }}
                  </option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">양품/불량</span>
                <select v-model="executionQtyFilter" class="app-control">
                  <option value="ALL">전체</option>
                  <option value="GOOD">양품 수량 있음</option>
                  <option value="DEFECT">불량 수량 있음</option>
                </select>
              </label>
            </div>
            <div v-show="isExecutionHistoryOpen" class="wo-table-wrap">
              <table class="app-table">
                <thead>
                  <tr>
                    <th>등록일시</th>
                    <th>공정</th>
                    <th>양품</th>
                    <th>불량</th>
                    <th>총 생산</th>
                    <th>공수</th>
                    <th>작업자</th>
                    <th>액션</th>
                  </tr>
              </thead>
              <tbody>
                  <tr v-for="execution in filteredExecutions" :key="execution.executionId">
                    <td class="wo-mono">{{ formatDateTime(execution.createdAt) }}</td>
                    <td><strong>{{ execution.operationName ?? '-' }}</strong><span>{{ execution.factoryName ?? '-' }} / {{ execution.lineName ?? '-' }}</span></td>
                    <td class="wo-number">{{ formatNumber(execution.goodQty) }}</td>
                    <td class="wo-number">{{ formatNumber(execution.defectQty) }}</td>
                    <td class="wo-number">{{ formatNumber(execution.executedQty) }}</td>
                    <td class="wo-number">{{ formatNumber(execution.manHoursMinutes) }}분</td>
                    <td><strong>{{ execution.workerName ?? '-' }}</strong><span>{{ execution.workerEmployeeNo ?? '-' }}</span></td>
                    <td>
                      <button class="wo-icon-button wo-icon-danger" :disabled="selectedOrder.status === 'CLOSE' || workOrderStore.isSaving" @click="deleteExecution(execution.executionId)">
                        <Trash2 class="wo-button-icon" />
                      </button>
                    </td>
                  </tr>
                  <tr v-if="filteredExecutions.length === 0"><td colspan="8" class="wo-empty">조건에 맞는 생산 실적이 없습니다.</td></tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="app-panel">
            <div class="app-panel-head">
              <div class="wo-head-inline">
                <Package class="wo-icon" />
                <h2 class="app-panel-title">BOM 투입량 확인</h2>
              </div>
              <div class="wo-head-inline">
                <span v-if="hasStockShortage" class="wo-badge-danger">재고 부족</span>
                <button class="app-button app-button-muted" type="button" @click="isMaterialRequirementOpen = !isMaterialRequirementOpen">
                  {{ isMaterialRequirementOpen ? '접기' : '펼치기' }}
                </button>
              </div>
            </div>
            <div v-show="isMaterialRequirementOpen" class="wo-table-wrap">
              <table class="app-table wo-table-compact">
                <thead>
                  <tr><th>자재</th><th>단위</th><th>필요</th><th>불출</th><th>잔여</th><th>가용</th></tr>
                </thead>
                <tbody>
                  <tr v-for="item in materialRequirements" :key="item.itemId">
                    <td><strong>{{ item.itemName }}</strong><span>{{ item.itemCode }} · BOM {{ item.bomQuantity }}</span></td>
                    <td>{{ item.unit }}</td>
                    <td class="wo-number">{{ formatNumber(item.requiredQty) }}</td>
                    <td class="wo-number">{{ formatNumber(item.issuedQty) }}</td>
                    <td class="wo-number">{{ formatNumber(getRemainingQty(item.requiredQty, item.issuedQty)) }}</td>
                    <td class="wo-number" :class="{ 'wo-danger-text': item.availableQty < getRemainingQty(item.requiredQty, item.issuedQty) }">{{ formatNumber(item.availableQty) }}</td>
                  </tr>
                  <tr v-if="materialRequirements.length === 0"><td colspan="6" class="wo-empty">BOM 투입량 정보가 없습니다.</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </section>
    </section>
  </div>
</template>
