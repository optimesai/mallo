<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  AlertTriangle,
  CheckCircle2,
  ClipboardCheck,
  FileText,
  Loader2,
  Package,
  RefreshCw,
  Search,
  Trash2
} from '@lucide/vue'
import { factoryRoutingService } from '@/services/factoryRoutingService'
import { useWorkOrderStore } from '@/state/workOrderStore'
import { useAuthStore } from '@/state/authStore'
import { formatDateTime } from '@/utils/dateFormat'
import type { FactoryRoutingResponse } from '@/api/factoryRoutingApi'
import type { ProductionExecutionCreateRequest, WorkOrderStatus } from '@/api/workOrderApi'

type ProductionExecutionTab = 'register' | 'lookup'
type ExecutionSortOrder = 'latest' | 'oldest'
type ExecutionQtyFilter = 'ALL' | 'GOOD' | 'DEFECT'

const workOrderStore = useWorkOrderStore()
const authStore = useAuthStore()

const routings = ref<FactoryRoutingResponse[]>([])
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

const form = reactive<Omit<ProductionExecutionCreateRequest, 'orderKey'>>({
  routingId: 0,
  goodQty: 0,
  defectQty: 0,
  manHoursMinutes: 0
})

const selectedDetail = computed(() => workOrderStore.selectedDetail)
const selectedOrder = computed(() => selectedDetail.value?.workOrder ?? null)
const executions = computed(() => selectedDetail.value?.executions ?? [])
const materialRequirements = computed(() => selectedDetail.value?.materialRequirements ?? [])
const canSubmitExecution = computed(() => Boolean(selectedOrder.value?.canRegisterExecution))
const hasStockShortage = computed(() => materialRequirements.value.some((item) => item.availableQty < getRemainingQty(item.requiredQty, item.issuedQty)))
const executionValidationMessages = computed(() => {
  const messages: string[] = []

  if (!selectedOrder.value) messages.push('작업 지시를 먼저 선택해주세요.')
  else if (!canSubmitExecution.value) messages.push('진행(RUN) 상태의 작업 지시에만 실적을 등록할 수 있습니다.')

  if (!form.routingId) messages.push('실제 수행 공정을 선택해주세요.')
  if (form.goodQty + form.defectQty <= 0) messages.push('양품과 불량 수량의 합계는 1 이상이어야 합니다.')
  if (form.manHoursMinutes <= 0) messages.push('총 소요 시간은 1분 이상이어야 합니다.')

  return messages
})
const hasExecutionValidationError = computed(() => executionValidationMessages.value.length > 0)

const filteredOrders = computed(() => {
  const query = appliedKeyword.value.trim().toLowerCase()
  return workOrderStore.workOrders.filter((order) => {
    if (appliedStatus.value !== 'ALL' && order.status !== appliedStatus.value) return false
    if (appliedFactory.value && order.factoryName !== appliedFactory.value) return false
    if (appliedLine.value && order.lineName !== appliedLine.value) return false
    if (appliedRoutingId.value && order.routingId !== appliedRoutingId.value) return false
    if (!query) return true
    return [order.orderNo, order.itemCode, order.itemName, order.factoryName, order.lineName, order.operationName]
      .some((value) => value.toLowerCase().includes(query))
  })
})

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
  if (!selectedOrder.value) return []
  return routings.value.filter((routing) => (
    routing.factoryName === selectedOrder.value?.factoryName
    && routing.lineName === selectedOrder.value?.lineName
  ))
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
      routingText: `${order.factoryName} / ${order.lineName} / ${order.operationSeq}. ${order.operationName}`
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
  form.routingId = order && executionRoutingOptions.value.some((routing) => routing.routingId === order.routingId)
    ? order.routingId
    : 0
  form.goodQty = 0
  form.defectQty = 0
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

function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString()
}

function getRemainingQty(requiredQty: number, issuedQty: number) {
  return Math.max(requiredQty - issuedQty, 0)
}

async function loadInitialData() {
  pageError.value = null
  try {
    const [, routingList] = await Promise.all([
      workOrderStore.loadWorkOrders(),
      factoryRoutingService.getRoutings()
    ])
    routings.value = routingList
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '생산 실적 데이터를 불러오지 못했습니다.'
  }
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
  if (form.goodQty < 0 || form.defectQty < 0) return '양품/불량 수량은 0 이상이어야 합니다.'
  if (form.goodQty + form.defectQty <= 0) return '양품과 불량 수량의 합계는 1 이상이어야 합니다.'
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
      manHoursMinutes: Number(form.manHoursMinutes)
    })
    showToast(`작업 지시 [${selectedOrder.value.orderNo}] 실적이 등록되었습니다.`)
    form.goodQty = 0
    form.defectQty = 0
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

function selectRunnableFilter() {
  statusInput.value = 'RUN'
  appliedStatus.value = 'RUN'
}

function applyOrderSearch() {
  appliedKeyword.value = keywordInput.value
  appliedStatus.value = statusInput.value
  appliedFactory.value = factoryInput.value
  appliedLine.value = lineInput.value
  appliedRoutingId.value = routingInput.value
  isKeywordSuggestOpen.value = false
}

function resetOrderSearch() {
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
        <h1 class="wo-title">공정 실적 및 원부자재</h1>
        <p class="wo-subtitle">진행 중인 작업 지시를 기준으로 공정별 생산 실적을 입력하고 BOM 투입량 변화를 확인합니다.</p>
      </div>
      <button class="wo-button wo-button-subtle" :disabled="workOrderStore.isLoading" @click="refreshData">
        <RefreshCw class="wo-button-icon" />
        새로고침
      </button>
    </header>

    <div v-if="hasNoRoutings" class="wo-alert wo-alert-danger">
      <AlertTriangle class="wo-icon" />
      <span>등록된 공장/생산 라우팅 기준정보가 없어 공정 실적을 등록할 수 없습니다.</span>
      <RouterLink v-if="authStore.canManageMasterData" class="wo-button wo-button-subtle" to="/master/factory-lines">
        기준정보 등록
      </RouterLink>
    </div>

    <section class="wo-metric-grid wo-panel pe-summary-panel">
      <div class="wo-metric">
        <span>전체 작업 지시</span>
        <strong>{{ formatNumber(workOrderStore.workOrders.length) }}</strong>
      </div>
      <div class="wo-metric">
        <span>실적 등록 가능</span>
        <strong>{{ formatNumber(runnableOrders) }}</strong>
      </div>
      <div class="wo-metric">
        <span>마감 작업</span>
        <strong>{{ formatNumber(closeLockedOrders) }}</strong>
      </div>
      <div class="wo-metric">
        <span>선택 작업 불량률</span>
        <strong>{{ totalDefectRate }}%</strong>
      </div>
    </section>

    <nav class="wo-tabs" aria-label="공정 실적 기능 탭">
      <button class="wo-tab" :class="{ 'is-active': activeTab === 'register' }" @click="activeTab = 'register'">실적 등록</button>
      <button class="wo-tab" :class="{ 'is-active': activeTab === 'lookup' }" @click="activeTab = 'lookup'">실적/투입 조회</button>
    </nav>

    <section class="pe-layout">
      <aside class="wo-panel pe-order-panel">
        <div class="wo-panel-head">
          <div class="wo-head-inline">
            <FileText class="wo-icon" />
            <h2 class="wo-section-title">작업 지시 선택</h2>
          </div>
          <button class="wo-button wo-button-subtle" @click="selectRunnableFilter">RUN 보기</button>
        </div>
        <div class="pe-filter-box">
          <div class="pe-filter-row pe-filter-row-main">
            <label class="wo-field wo-autocomplete">
              <span class="wo-label">검색어</span>
              <div class="wo-search-box">
                <Search class="wo-search-icon" />
                <input
                  v-model="keywordInput"
                  class="wo-control wo-control-search"
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
            <label class="wo-field">
              <span class="wo-label">상태</span>
              <select v-model="statusInput" class="wo-control">
                <option value="ALL">전체</option>
                <option value="RUN">진행</option>
                <option value="HOLD">보류</option>
                <option value="CLOSE">마감</option>
                <option value="READY">대기</option>
              </select>
            </label>
          </div>
          <div class="pe-filter-row pe-filter-row-routing">
            <label class="wo-field">
              <span class="wo-label">공장</span>
              <select v-model="factoryInput" class="wo-control">
                <option value="">전체 공장</option>
                <option v-for="factory in factoryOptions" :key="factory" :value="factory">{{ factory }}</option>
              </select>
            </label>
            <label class="wo-field">
              <span class="wo-label">라인</span>
              <select v-model="lineInput" class="wo-control" :disabled="!factoryInput">
                <option value="">전체 라인</option>
                <option v-for="line in lineOptions" :key="line" :value="line">{{ line }}</option>
              </select>
            </label>
            <label class="wo-field">
              <span class="wo-label">공정</span>
              <select v-model.number="routingInput" class="wo-control">
                <option value="">전체 공정</option>
                <option v-for="routing in operationOptions" :key="routing.routingId" :value="routing.routingId">
                  {{ routing.operationSeq }}. {{ routing.operationName }}
                </option>
              </select>
            </label>
          </div>
          <div class="wo-toolbar wo-toolbar-end pe-filter-actions">
            <button class="wo-button wo-button-subtle" type="button" @click="resetOrderSearch">초기화</button>
            <button class="wo-button wo-button-primary" type="button" @click="applyOrderSearch">
              <Search class="wo-button-icon" />
              조회
            </button>
          </div>
        </div>
        <div class="pe-order-list">
          <button
            v-for="order in filteredOrders"
            :key="order.orderId"
            class="pe-order-card"
            :class="{ 'is-active': selectedOrder?.orderId === order.orderId }"
            type="button"
            @click="selectOrder(order.orderNo)"
          >
            <span class="pe-order-top">
              <strong>{{ order.orderNo }}</strong>
              <span class="wo-status" :data-status="order.status">{{ getStatusLabel(order.status) }}</span>
            </span>
            <span class="pe-order-name">{{ order.itemName }}</span>
            <span class="pe-order-meta">{{ order.factoryName }} / {{ order.lineName }} / {{ order.operationSeq }}. {{ order.operationName }}</span>
            <span class="pe-order-progress">
              <span class="wo-progress"><span :style="{ width: `${Math.min(order.progressRate, 100)}%` }"></span></span>
              <span>{{ order.progressRate }}%</span>
            </span>
          </button>
          <div v-if="!workOrderStore.isLoading && filteredOrders.length === 0" class="wo-empty-panel">
            <FileText class="wo-empty-icon" />
            <span>조건에 맞는 작업 지시가 없습니다.</span>
          </div>
          <div v-if="workOrderStore.isLoading" class="wo-empty-panel">
            <Loader2 class="wo-empty-icon wo-spin" />
            <span>작업 지시를 불러오고 있습니다.</span>
          </div>
        </div>
      </aside>

      <main class="pe-main">
        <section v-if="selectedOrder" class="wo-panel">
          <div class="wo-detail-head">
            <div>
              <p class="wo-kicker">SELECTED WORK ORDER</p>
              <h2 class="wo-section-title">{{ selectedOrder.orderNo }}</h2>
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
            <div><span>수행 라우팅</span><strong>{{ selectedOrder.factoryName }} / {{ selectedOrder.lineName }} / {{ selectedOrder.operationName }}</strong></div>
            <div><span>계획일</span><strong>{{ selectedOrder.planDate }}</strong></div>
            <div><span>진행률</span><strong>{{ selectedOrder.progressRate }}% · 총 실적 {{ formatNumber(selectedOrder.totalExecutedQty) }}</strong></div>
          </div>
        </section>

        <section v-else class="wo-panel wo-empty-panel">
          <ClipboardCheck class="wo-empty-icon" />
          <strong>작업 지시를 선택해주세요.</strong>
          <span>실적 등록과 투입량 확인은 작업 지시 상세 조회 후 가능합니다.</span>
        </section>

        <section v-if="selectedOrder && activeTab === 'register'" class="wo-tab-section">
          <div class="wo-panel">
            <div class="wo-panel-head">
              <div class="wo-head-inline">
                <ClipboardCheck class="wo-icon" />
                <h2 class="wo-section-title">생산 실적 등록</h2>
              </div>
              <span v-if="!canSubmitExecution" class="wo-badge-danger">RUN 상태 필요</span>
            </div>
            <div class="wo-form-grid wo-form-grid-execution">
              <label class="wo-field">
                <span class="wo-label">실제 수행 공정</span>
                <select v-model.number="form.routingId" class="wo-control" :disabled="!canSubmitExecution">
                  <option :value="0">공정 선택</option>
                  <option v-for="routing in executionRoutingOptions" :key="routing.routingId" :value="routing.routingId">
                    {{ routing.factoryName }} / {{ routing.lineName }} / {{ routing.operationSeq }}. {{ routing.operationName }}
                  </option>
                </select>
                <span class="wo-field-help">선택한 작업 지시의 공장/라인에 등록된 공정만 표시됩니다.</span>
              </label>
              <label class="wo-field">
                <span class="wo-label">양품 수량</span>
                <input v-model.number="form.goodQty" class="wo-control" type="number" min="0" :disabled="!canSubmitExecution" />
              </label>
              <label class="wo-field">
                <span class="wo-label">불량 수량</span>
                <input v-model.number="form.defectQty" class="wo-control" type="number" min="0" :disabled="!canSubmitExecution" />
              </label>
              <label class="wo-field">
                <span class="wo-label">총 소요 시간(분)</span>
                <input v-model.number="form.manHoursMinutes" class="wo-control" type="number" min="1" :disabled="!canSubmitExecution" />
              </label>
            </div>
            <div class="wo-preview pe-preview-note">
              <div>
                <span class="wo-preview-label">작업자 기록</span>
                <strong>로그인 사용자 기준 자동 저장</strong>
              </div>
              <div>
                <span class="wo-preview-label">투입량 처리</span>
                <strong>BOM 기준 추가 필요량 자동 차감</strong>
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
              <button class="wo-button wo-button-primary" :disabled="hasExecutionValidationError || workOrderStore.isSaving" @click="submitExecution">
                <Loader2 v-if="workOrderStore.isSaving" class="wo-button-icon wo-spin" />
                <CheckCircle2 v-else class="wo-button-icon" />
                실적 등록
              </button>
            </div>
          </div>
        </section>

        <section v-if="selectedOrder && activeTab === 'lookup'" class="wo-tab-section">
          <div class="wo-panel wo-execution-list-panel">
            <div class="wo-panel-head">
              <h2 class="wo-section-title">생산 실적 이력</h2>
              <div class="wo-head-inline">
                <span class="wo-count">총 {{ filteredExecutions.length }}건</span>
                <button class="wo-button wo-button-subtle" type="button" @click="isExecutionHistoryOpen = !isExecutionHistoryOpen">
                  {{ isExecutionHistoryOpen ? '접기' : '펼치기' }}
                </button>
              </div>
            </div>
            <div v-show="isExecutionHistoryOpen" class="pe-lookup-filter-box">
              <label class="wo-field">
                <span class="wo-label">등록일시 정렬</span>
                <select v-model="executionSortOrder" class="wo-control">
                  <option value="latest">최신순</option>
                  <option value="oldest">오래된순</option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">공정</span>
                <select v-model.number="executionOperationFilter" class="wo-control">
                  <option value="ALL">전체 공정</option>
                  <option v-for="operation in executionOperationOptions" :key="operation.routingId" :value="operation.routingId">
                    {{ operation.label }}
                  </option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">작업자</span>
                <select v-model="executionWorkerFilter" class="wo-control">
                  <option value="ALL">전체 작업자</option>
                  <option v-for="worker in executionWorkerOptions" :key="worker.key" :value="worker.key">
                    {{ worker.label }}
                  </option>
                </select>
              </label>
              <label class="wo-field">
                <span class="wo-label">양품/불량</span>
                <select v-model="executionQtyFilter" class="wo-control">
                  <option value="ALL">전체</option>
                  <option value="GOOD">양품 수량 있음</option>
                  <option value="DEFECT">불량 수량 있음</option>
                </select>
              </label>
            </div>
            <div v-show="isExecutionHistoryOpen" class="wo-table-wrap">
              <table class="wo-table">
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

          <div class="wo-panel">
            <div class="wo-panel-head">
              <div class="wo-head-inline">
                <Package class="wo-icon" />
                <h2 class="wo-section-title">BOM 투입량 확인</h2>
              </div>
              <div class="wo-head-inline">
                <span v-if="hasStockShortage" class="wo-badge-danger">재고 부족</span>
                <button class="wo-button wo-button-subtle" type="button" @click="isMaterialRequirementOpen = !isMaterialRequirementOpen">
                  {{ isMaterialRequirementOpen ? '접기' : '펼치기' }}
                </button>
              </div>
            </div>
            <div v-show="isMaterialRequirementOpen" class="wo-table-wrap">
              <table class="wo-table wo-table-compact">
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
      </main>
    </section>
  </div>
</template>
