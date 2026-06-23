<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertTriangle,
  CheckCircle2,
  ClipboardList,
  FileText,
  Loader2,
  Package,
  Pause,
  Pencil,
  Play,
  RefreshCw,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Trash2,
  XCircle
} from '@lucide/vue'
import { useWorkOrderStore } from '@/state/workOrderStore'
import { useAuthStore } from '@/state/authStore'
import { useItemMasterStore } from '@/state/itemMasterStore'
import { useFactoryRoutingStore } from '@/state/factoryRoutingStore'
import { useBomMasterStore } from '@/state/bomMasterStore'
import { formatDateTime } from '@/utils/dateFormat'
import type { ItemMasterResponse, ItemType } from '@/api/itemMasterApi'
import type { FactoryRoutingResponse } from '@/api/factoryRoutingApi'
import type { WorkOrderRequest, WorkOrderResponse, WorkOrderStatus } from '@/api/workOrderApi'

type WorkOrderTab = 'register' | 'list'

const workOrderStore = useWorkOrderStore()
const authStore = useAuthStore()
const itemMasterStore = useItemMasterStore()
const factoryRoutingStore = useFactoryRoutingStore()
const bomMasterStore = useBomMasterStore()
const router = useRouter()

const productionItems = ref<ItemMasterResponse[]>([])
const routings = ref<FactoryRoutingResponse[]>([])
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const activeTab = ref<WorkOrderTab>('register')
const isSearchExpanded = ref(true)
const isEditing = ref(false)
const editingOrderKey = ref<string | null>(null)
const allowUnderTargetClose = ref(false)
const itemQuery = ref('')
const isItemSuggestOpen = ref(false)
const formFactoryName = ref('')
const formLineName = ref('')
const bomVersions = ref<string[]>([])
const keywordQuery = ref('')
const itemKeywordQuery = ref('')
const isKeywordSuggestOpen = ref(false)
const sortField = ref<keyof WorkOrderResponse>('planDate')
const sortDirection = ref<'asc' | 'desc'>('desc')

const filters = reactive({
  keyword: '',
  itemKeyword: '',
  status: '' as WorkOrderStatus | '',
  planDate: '',
  fromDate: '',
  toDate: '',
  factoryName: '',
  lineName: '',
  operationName: ''
})

const form = reactive<Omit<WorkOrderRequest, 'targetQty'> & { targetQty: number | null }>({
  itemCode: '',
  routingId: 0,
  targetQty: null,
  bomVersion: 'v1.0',
  planDate: ''
})


const selectedOrder = computed(() => workOrderStore.selectedDetail?.workOrder ?? null)
const materialRequirements = computed(() => workOrderStore.selectedDetail?.materialRequirements ?? [])
const selectedItem = computed(() => productionItems.value.find((item) => item.itemCode === form.itemCode) ?? null)
const selectedRouting = computed(() => routings.value.find((routing) => routing.routingId === Number(form.routingId)) ?? null)
const detailCanIssue = computed(() => selectedOrder.value?.canIssueMaterials && materialRequirements.value.length > 0)
const detailHasStockShortage = computed(() => materialRequirements.value.some((item) => item.availableQty < item.requiredQty - item.issuedQty))

const formFactories = computed(() => [...new Set(routings.value.map((routing) => routing.factoryName))])
const formLines = computed(() => [...new Set(routings.value
  .filter((routing) => !formFactoryName.value || routing.factoryName === formFactoryName.value)
  .map((routing) => routing.lineName))])
const formOperations = computed(() => routings.value.filter((routing) => {
  if (formFactoryName.value && routing.factoryName !== formFactoryName.value) return false
  if (formLineName.value && routing.lineName !== formLineName.value) return false
  return true
}).sort((a, b) => a.operationSeq - b.operationSeq))
const selectedLineOperationText = computed(() => {
  if (!formFactoryName.value || !formLineName.value) return '공장/라인 미선택'
  if (formOperations.value.length === 0) return '등록된 공정 없음'
  return formOperations.value.map((routing) => `${routing.operationSeq}. ${routing.operationName}`).join(' → ')
})

const searchFactories = computed(() => [...new Set(routings.value.map((routing) => routing.factoryName))])
const searchLines = computed(() => [...new Set(routings.value
  .filter((routing) => !filters.factoryName || routing.factoryName === filters.factoryName)
  .map((routing) => routing.lineName))])


const itemSuggestions = computed(() => {
  const query = itemQuery.value.trim().toLowerCase()
  if (!query) return productionItems.value.slice(0, 8)
  return productionItems.value
    .filter((item) => `${item.itemCode} ${item.itemName} ${item.itemType}`.toLowerCase().includes(query))
    .slice(0, 8)
})

const keywordSuggestions = computed(() => {
  const query = keywordQuery.value.trim().toLowerCase()
  if (!query) return []
  const candidates = workOrderStore.workOrders.flatMap((order) => [
    { label: order.orderNo, meta: `${order.itemCode} · ${order.itemName}` }
  ])
  const unique = new Map<string, { label: string; meta: string }>()
  candidates.forEach((candidate) => {
    if (candidate.label.toLowerCase().includes(query) && !unique.has(candidate.label)) {
      unique.set(candidate.label, candidate)
    }
  })
  return [...unique.values()].slice(0, 8)
})

const recentOrders = computed(() => workOrderStore.workOrders.slice(0, 6))
const hasNoActiveRoutings = computed(() => routings.value.length === 0)

onMounted(async () => {
  await loadInitialData()
})

watch(() => filters.factoryName, () => {
  if (filters.lineName && !searchLines.value.includes(filters.lineName)) filters.lineName = ''
})

watch(formFactoryName, () => {
  if (formLineName.value && !formLines.value.includes(formLineName.value)) formLineName.value = ''
  if (form.routingId && !formOperations.value.some((routing) => routing.routingId === form.routingId)) form.routingId = 0
  syncRepresentativeRouting()
})

watch(formLineName, () => {
  if (form.routingId && !formOperations.value.some((routing) => routing.routingId === form.routingId)) form.routingId = 0
  syncRepresentativeRouting()
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

function getOrderKey(order: WorkOrderResponse) {
  return order.orderNo || order.orderId
}

function syncRoutingFields(routingId: number) {
  const routing = routings.value.find((item) => item.routingId === Number(routingId))
  if (!routing) return
  formFactoryName.value = routing.factoryName
  formLineName.value = routing.lineName
  form.routingId = routing.routingId
}

function syncRepresentativeRouting() {
  if (!formFactoryName.value || !formLineName.value) {
    form.routingId = 0
    return
  }
  const representativeRouting = formOperations.value[0]
  form.routingId = representativeRouting?.routingId ?? 0
}

async function selectProductionItem(item: ItemMasterResponse) {
  form.itemCode = item.itemCode
  itemQuery.value = `${item.itemCode} · ${item.itemName}`
  isItemSuggestOpen.value = false
  await loadBomVersions(item.itemCode)
}

function selectKeywordSuggestion(keyword: string) {
  filters.keyword = keyword
  keywordQuery.value = keyword
  isKeywordSuggestOpen.value = false
}


async function loadInitialData() {
  pageError.value = null
  const results = await Promise.allSettled([
    loadProductionItems('HALF'),
    loadProductionItems('FG'),
    factoryRoutingStore.loadRoutings({ routingStatus: 'ACTIVE' }),
    workOrderStore.loadWorkOrders({ page: 0, size: 10 })
  ])

  const labels = ['반제품 품목', '완제품 품목', '공장/생산 라우팅', '작업지시 목록']
  const failedMessages = results
    .map((result, index) => {
      if (result.status === 'fulfilled') return ''
      const message = result.reason instanceof Error ? result.reason.message : '서버 오류가 발생했습니다.'
      return `${labels[index]} 조회 실패: ${message}`
    })
    .filter(Boolean)

  if (results[0].status === 'fulfilled' && results[1].status === 'fulfilled') {
    productionItems.value = [...results[0].value, ...results[1].value]
  }
  if (results[2].status === 'fulfilled') {
    routings.value = [...factoryRoutingStore.routings]
  }

  if (failedMessages.length > 0) {
    pageError.value = failedMessages.join('\n')
  }
}

async function loadProductionItems(itemType: ItemType) {
  const size = 100
  const firstPage = await itemMasterStore.loadItems({ itemType, itemStatus: 'ACTIVE', page: 0, size, sort: 'itemCode,asc' })
  if (firstPage.totalPages <= 1) return firstPage.content

  const restPages = await Promise.all(
    Array.from({ length: firstPage.totalPages - 1 }, (_, index) => itemMasterStore.loadItems({
      itemType,
      itemStatus: 'ACTIVE',
      page: index + 1,
      size,
      sort: 'itemCode,asc'
    }))
  )
  return [firstPage, ...restPages].flatMap((page) => page.content)
}

async function loadBomVersions(itemCode: string) {
  try {
    bomVersions.value = await bomMasterStore.loadParentVersions(itemCode)
    if (bomVersions.value.length === 0) {
      form.bomVersion = ''
      return
    }
    if (!form.bomVersion || !bomVersions.value.includes(form.bomVersion)) {
      form.bomVersion = bomVersions.value[0]
    }
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 버전 목록을 불러오지 못했습니다.'
  }
}

async function searchWorkOrders(page = 0) {
  pageError.value = null
  filters.keyword = keywordQuery.value.trim()
  filters.itemKeyword = itemKeywordQuery.value.trim()
  try {
    await workOrderStore.loadWorkOrders({ ...filters, page, size: 10, sort: `${String(sortField.value)},${sortDirection.value}` })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업지시 목록 조회에 실패했습니다.'
  }
}

async function changeSort(field: keyof WorkOrderResponse) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
  await searchWorkOrders(0)
}

function getSortMark(field: keyof WorkOrderResponse) {
  if (sortField.value !== field) return ''
  return sortDirection.value === 'asc' ? '▲' : '▼'
}

function resetFilters() {
  filters.keyword = ''
  keywordQuery.value = ''
  filters.itemKeyword = ''
  itemKeywordQuery.value = ''
  filters.status = ''
  filters.planDate = ''
  filters.fromDate = ''
  filters.toDate = ''
  filters.factoryName = ''
  filters.lineName = ''
  filters.operationName = ''
}

function resetForm() {
  form.itemCode = ''
  itemQuery.value = ''
  formFactoryName.value = ''
  formLineName.value = ''
  form.routingId = 0
  form.targetQty = null
  form.bomVersion = 'v1.0'
  bomVersions.value = []
  form.planDate = ''
  isEditing.value = false
  editingOrderKey.value = null
}

async function fillEditForm(order: WorkOrderResponse) {
  const item = productionItems.value.find((candidate) => candidate.itemCode === order.itemCode)
  if (item) await selectProductionItem(item)
  else {
    form.itemCode = order.itemCode
    itemQuery.value = `${order.itemCode} · ${order.itemName}`
    await loadBomVersions(order.itemCode)
  }
  syncRoutingFields(order.routingId)
  form.targetQty = order.targetQty
  form.bomVersion = order.bomVersion
  form.planDate = order.planDate
  isEditing.value = true
  editingOrderKey.value = order.orderNo
  activeTab.value = 'register'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function submitWorkOrder() {
  if (!form.itemCode || !form.routingId || !form.bomVersion || !form.planDate || !form.targetQty || form.targetQty < 1) {
    pageError.value = '생산 품목, BOM 버전, 공장/라인, 목표 수량, 계획일을 모두 올바르게 입력해주세요.'
    return
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(form.planDate)) {
    pageError.value = '계획일은 예) 2026-06-05 형식으로 입력해주세요.'
    return
  }
  pageError.value = null
  try {
    const payload = { ...form, routingId: Number(form.routingId), targetQty: Number(form.targetQty) }
    const result = isEditing.value && editingOrderKey.value
      ? await workOrderStore.updateWorkOrder(editingOrderKey.value, payload)
      : await workOrderStore.createWorkOrder(payload)
    showToast(`작업지시 [${result.orderNo}]가 ${isEditing.value ? '수정' : '등록'}되었습니다.`)
    resetForm()
    await searchWorkOrders()
    await workOrderStore.loadWorkOrder(result.orderNo)
    activeTab.value = 'list'
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업지시 저장에 실패했습니다.'
  }
}

async function selectOrder(order: WorkOrderResponse) {
  await router.push({ name: 'production-work-order-detail', params: { id: getOrderKey(order) } })
}

async function deleteOrder(order: WorkOrderResponse) {
  if (!confirm(`[${order.orderNo}] 작업지시를 삭제하시겠습니까?`)) return
  pageError.value = null
  try {
    await workOrderStore.deleteWorkOrder(getOrderKey(order))
    showToast(`작업지시 [${order.orderNo}]가 삭제되었습니다.`)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업지시 삭제에 실패했습니다.'
  }
}

async function changeStatus(order: WorkOrderResponse, status: Exclude<WorkOrderStatus, 'CLOSE'>) {
  pageError.value = null
  try {
    const updated = await workOrderStore.updateStatus(getOrderKey(order), { status })
    showToast(`작업지시 [${updated.orderNo}] 상태가 ${getStatusLabel(updated.status)} 상태로 변경되었습니다.`)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '상태 변경에 실패했습니다.'
  }
}

async function issueMaterials(order: WorkOrderResponse) {
  if (detailHasStockShortage.value) {
    pageError.value = '가용 재고가 부족한 자재가 있어 불출할 수 없습니다.'
    return
  }
  pageError.value = null
  try {
    await workOrderStore.issueMaterials(getOrderKey(order))
    showToast(`작업지시 [${order.orderNo}] 자재 불출이 완료되었습니다.`)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '자재 불출에 실패했습니다.'
  }
}

async function closeOrder(order: WorkOrderResponse) {
  pageError.value = null
  try {
    const updated = await workOrderStore.closeWorkOrder(getOrderKey(order), {
      allowUnderTargetClose: allowUnderTargetClose.value
    })
    showToast(`작업지시 [${updated.orderNo}]가 마감되었습니다.`)
    allowUnderTargetClose.value = false
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업지시 마감에 실패했습니다.'
  }
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
        <h1 class="wo-title">작업 지시 관리</h1>
        <p class="wo-subtitle">작업지시 생성, 조회, 상태 액션, 마감을 중심으로 생산 작업 흐름을 관리합니다.</p>
      </div>
      <button class="app-button app-button-muted" @click="loadInitialData">
        <RefreshCw class="wo-button-icon" />
        새로고침
      </button>
    </header>

    <nav class="wo-tabs" aria-label="작업지시 기능 탭">
      <button class="wo-tab" :class="{ 'is-active': activeTab === 'register' }" @click="activeTab = 'register'">작업지시 등록</button>
      <button class="wo-tab" :class="{ 'is-active': activeTab === 'list' }" @click="activeTab = 'list'">목록 / 검색 / 상세</button>
    </nav>

    <section v-if="activeTab === 'register'" class="wo-tab-section">
      <div v-if="hasNoActiveRoutings" class="wo-alert wo-alert-danger">
        <AlertTriangle class="wo-icon" />
        <span>활성 공장/생산 라우팅 기준정보가 없어 작업지시를 등록할 수 없습니다.</span>
        <RouterLink v-if="authStore.canManageMasterData" class="app-button app-button-muted" to="/master/factory-lines">
          기준정보 등록
        </RouterLink>
      </div>

      <div class="app-panel wo-form-panel">
        <div class="app-panel-head">
          <div>
            <p class="wo-kicker">WORK ORDER</p>
            <h2 class="app-panel-title">{{ isEditing ? '작업지시 수정' : '작업지시 등록' }}</h2>
          </div>
          <button v-if="isEditing" class="app-button app-button-muted" @click="resetForm">
            <XCircle class="wo-button-icon" />
            수정 취소
          </button>
        </div>

        <div class="wo-form-grid">
          <label class="wo-field wo-autocomplete">
            <span class="wo-label">생산 품목</span>
            <div class="wo-search-box">
              <Search class="wo-search-icon" />
              <input
                v-model="itemQuery"
                class="app-control app-control-search"
                placeholder="예) SM-PCB-ASSY 또는 제어보드 PCB 조립체"
                @focus="isItemSuggestOpen = true"
                @input="isItemSuggestOpen = true"
              />
            </div>
            <div v-if="isItemSuggestOpen" class="wo-suggest-list">
              <button
                v-for="item in itemSuggestions"
                :key="item.itemId"
                type="button"
                class="wo-suggest-item"
                @mousedown.prevent="selectProductionItem(item)"
              >
                <strong>{{ item.itemCode }} · {{ item.itemName }}</strong>
                <span>{{ item.itemType }} / {{ item.unit }} / 안전재고 {{ formatNumber(item.safetyStock) }}</span>
              </button>
              <div v-if="itemSuggestions.length === 0" class="wo-suggest-empty">관련 품목이 없습니다.</div>
            </div>
          </label>

          <label class="wo-field">
            <span class="wo-label">BOM 버전</span>
            <select v-model="form.bomVersion" class="app-control" :disabled="!form.itemCode || bomVersions.length === 0">
              <option value="">BOM 버전 선택</option>
              <option v-for="version in bomVersions" :key="version" :value="version">{{ version }}</option>
            </select>
          </label>

          <label class="wo-field">
            <span class="wo-label">공장</span>
            <select v-model="formFactoryName" class="app-control">
              <option value="">공장 선택</option>
              <option v-for="factory in formFactories" :key="factory" :value="factory">{{ factory }}</option>
            </select>
          </label>
          <label class="wo-field">
            <span class="wo-label">라인</span>
            <select v-model="formLineName" class="app-control" :disabled="!formFactoryName">
              <option value="">라인 선택</option>
              <option v-for="line in formLines" :key="line" :value="line">{{ line }}</option>
            </select>
          </label>
          <label class="wo-field">
            <span class="wo-label">목표 수량</span>
            <input v-model.number="form.targetQty" class="app-control" type="number" min="1" placeholder="예) 100" />
          </label>
          <label class="wo-field">
            <span class="wo-label">계획일</span>
            <input v-model="form.planDate" class="app-control" type="date" />
            <span class="wo-field-help">예) 2026-06-05</span>
          </label>
        </div>

        <div class="wo-preview">
          <div>
            <span class="wo-preview-label">선택 품목</span>
            <strong>{{ selectedItem ? `${selectedItem.itemCode} · ${selectedItem.itemName}` : '품목 미선택' }}</strong>
          </div>
          <div>
            <span class="wo-preview-label">BOM 버전</span>
            <strong>{{ form.bomVersion || '버전 미선택' }}</strong>
          </div>
          <div>
            <span class="wo-preview-label">선택 라인 공정</span>
            <strong>{{ selectedRouting ? `${selectedRouting.factoryName} / ${selectedRouting.lineName}` : '공장/라인 미선택' }}</strong>
            <span>{{ selectedLineOperationText }}</span>
          </div>
        </div>

        <div class="wo-toolbar wo-toolbar-end">
          <button class="app-button app-button-muted" @click="resetForm">
            <RotateCcw class="wo-button-icon" />
            초기화
          </button>
          <button class="app-button app-button-primary" :disabled="workOrderStore.isSaving" @click="submitWorkOrder">
            <Loader2 v-if="workOrderStore.isSaving" class="wo-button-icon wo-spin" />
            <ClipboardList v-else class="wo-button-icon" />
            {{ isEditing ? '수정 저장' : '작업지시 등록' }}
          </button>
        </div>
      </div>

      <div class="app-panel wo-table-panel">
        <div class="app-panel-head">
          <h2 class="app-panel-title">최근 작업지시</h2>
          <button class="app-button app-button-muted" @click="activeTab = 'list'">전체 목록 보기</button>
        </div>
        <div class="wo-table-wrap">
          <table class="app-table wo-table-compact">
            <thead>
              <tr>
                <th>작업지시번호</th>
                <th>품목</th>
                <th>라우팅</th>
                <th>BOM</th>
                <th>상태</th>
                <th>계획일</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in recentOrders" :key="order.orderId" @click="selectOrder(order); activeTab = 'list'">
                <td class="wo-mono">{{ order.orderNo }}</td>
                <td><strong>{{ order.itemName }}</strong><span>{{ order.itemCode }}</span></td>
                <td><strong>{{ order.factoryName }} / {{ order.lineName }}</strong><span>{{ order.operationName }}</span></td>
                <td class="wo-mono">{{ order.bomVersion }}</td>
                <td class="wo-status-cell"><span class="wo-status" :data-status="order.status">{{ getStatusLabel(order.status) }}</span></td>
                <td class="wo-mono">{{ order.planDate }}</td>
              </tr>
              <tr v-if="recentOrders.length === 0">
                <td colspan="6" class="wo-empty">등록된 작업지시가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'list'" class="wo-tab-section">
      <section class="app-search-panel">
        <button class="app-panel-head -mx-5 -mt-5 mb-5 w-[calc(100%+2.5rem)] rounded-t-[var(--radius-panel)] text-left" @click="isSearchExpanded = !isSearchExpanded">
          <div class="app-panel-title">
            <SlidersHorizontal class="app-panel-icon" />
            <span>검색 조건</span>
          </div>
        </button>
        <div v-show="isSearchExpanded" class="app-filter-grid">
          <label class="app-field wo-autocomplete">
            <span class="app-label">키워드</span>
            <div class="app-search-box">
              <Search class="app-search-icon" />
              <input
                v-model="keywordQuery"
                class="app-control app-control-search"
                placeholder="예) WO-20260605-001 또는 SM-PCB-ASSY"
                @focus="isKeywordSuggestOpen = true"
                @input="isKeywordSuggestOpen = true"
              />
            </div>
            <div v-if="isKeywordSuggestOpen && keywordSuggestions.length > 0" class="wo-suggest-list">
              <button
                v-for="candidate in keywordSuggestions"
                :key="`${candidate.label}-${candidate.meta}`"
                type="button"
                class="wo-suggest-item"
                @mousedown.prevent="selectKeywordSuggestion(candidate.label)"
              >
                <strong>{{ candidate.label }}</strong>
                <span>{{ candidate.meta }}</span>
              </button>
            </div>
          </label>
          <label class="app-field">
            <span class="app-label">품목</span>
            <input
              v-model="itemKeywordQuery"
              class="app-control"
              placeholder="품목코드 또는 품목명"
              @keyup.enter="searchWorkOrders(0)"
            />
          </label>
          <label class="app-field">
            <span class="app-label">상태</span>
            <select v-model="filters.status" class="app-control">
              <option value="">전체</option>
              <option value="READY">대기</option>
              <option value="RUN">진행</option>
              <option value="HOLD">보류</option>
              <option value="CLOSE">마감</option>
            </select>
          </label>
          <label class="app-field"><span class="app-label">계획일</span><input v-model="filters.planDate" class="app-control" type="date" /></label>
          <label class="app-field"><span class="app-label">시작일</span><input v-model="filters.fromDate" class="app-control" type="date" /></label>
          <label class="app-field"><span class="app-label">종료일</span><input v-model="filters.toDate" class="app-control" type="date" /></label>
          <label class="app-field">
            <span class="app-label">공장</span>
            <select v-model="filters.factoryName" class="app-control">
              <option value="">전체</option>
              <option v-for="factory in searchFactories" :key="factory" :value="factory">{{ factory }}</option>
            </select>
          </label>
          <label class="app-field">
            <span class="app-label">라인</span>
            <select v-model="filters.lineName" class="app-control">
              <option value="">전체</option>
              <option v-for="line in searchLines" :key="line" :value="line">{{ line }}</option>
            </select>
          </label>
          <label class="app-field">
            <span class="app-label">공정</span>
            <input v-model="filters.operationName" class="app-control" placeholder="공정명 입력" />
          </label>
        </div>
        <div v-show="isSearchExpanded" class="mt-4 app-actions-end">
          <button class="app-button app-button-muted" @click="resetFilters">조건 초기화</button>
          <button class="app-button app-button-primary" @click="searchWorkOrders(0)">조회</button>
        </div>
      </section>

      <section class="app-panel">
        <div class="app-panel-head">
          <h2 class="app-panel-title">작업지시 목록</h2>
          <span class="app-type-xs app-font-strong app-text-muted">총 {{ formatNumber(workOrderStore.totalElements) }}건</span>
        </div>
        <div class="app-table-wrap">
          <table class="app-table min-w-[1200px]">
            <thead>
              <tr>
                <th class="app-sortable-header" @click="changeSort('orderNo')">작업지시번호 <span class="app-sort-mark">{{ getSortMark('orderNo') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('itemName')">품목 <span class="app-sort-mark">{{ getSortMark('itemName') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('factoryName')">라우팅 <span class="app-sort-mark">{{ getSortMark('factoryName') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('bomVersion')">BOM <span class="app-sort-mark">{{ getSortMark('bomVersion') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('targetQty')">목표 <span class="app-sort-mark">{{ getSortMark('targetQty') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('totalExecutedQty')">실적 <span class="app-sort-mark">{{ getSortMark('totalExecutedQty') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('progressRate')">진행률 <span class="app-sort-mark">{{ getSortMark('progressRate') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('status')">상태 <span class="app-sort-mark">{{ getSortMark('status') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('planDate')">계획일 <span class="app-sort-mark">{{ getSortMark('planDate') }}</span></th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="workOrderStore.isLoading">
                <td colspan="10" class="app-empty"><Loader2 class="mx-auto mb-2 h-5 w-5 app-spin" />데이터를 불러오고 있습니다.</td>
              </tr>
              <tr
                v-for="order in workOrderStore.workOrders"
                :key="order.orderId"
                class="app-table-row"
                :class="{ 'app-table-row-selected': selectedOrder?.orderId === order.orderId }"
                @click="selectOrder(order)"
              >
                <td class="app-table-id">{{ order.orderNo }}</td>
                <td><strong class="app-table-main">{{ order.itemName }}</strong><span class="app-table-muted">{{ order.itemCode }}</span></td>
                <td><strong class="app-table-main">{{ order.factoryName }} / {{ order.lineName }}</strong><span class="app-table-muted">{{ order.operationSeq }}. {{ order.operationName }}</span></td>
                <td class="app-table-id">{{ order.bomVersion }}</td>
                <td class="app-table-number">{{ formatNumber(order.targetQty) }}</td>
                <td class="app-table-number">{{ formatNumber(order.totalExecutedQty) }}</td>
                <td><div class="wo-progress"><span :style="{ width: `${Math.min(order.progressRate, 100)}%` }"></span></div><span class="wo-progress-label">{{ order.progressRate }}%</span></td>
                <td><span class="app-status" :class="getStatusClass(order.status)">{{ getStatusLabel(order.status) }}</span></td>
                <td class="app-table-id">{{ order.planDate }}</td>
                <td>
                  <div class="wo-actions" @click.stop>
                    <button class="wo-icon-button" :disabled="!order.canUpdate" @click="fillEditForm(order)"><Pencil class="wo-button-icon" /></button>
                    <button class="wo-icon-button wo-icon-danger" :disabled="!order.canDelete" @click="deleteOrder(order)"><Trash2 class="wo-button-icon" /></button>
                  </div>
                </td>
              </tr>
              <tr v-if="!workOrderStore.isLoading && workOrderStore.workOrders.length === 0">
                <td colspan="10" class="app-empty"><FileText class="mx-auto mb-2 h-7 w-7" />조회된 작업지시가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="app-pagination">
          <p>
            총 {{ formatNumber(workOrderStore.totalElements) }}건 · {{ workOrderStore.page + 1 }} / {{ Math.max(workOrderStore.totalPages, 1) }} 페이지
          </p>
          <div class="app-pagination-actions">
          <button
            class="app-page-button"
            :disabled="workOrderStore.page <= 0 || workOrderStore.isLoading"
            @click="searchWorkOrders(0)"
          >
            처음
          </button>
          <button
            class="app-page-button"
            :disabled="workOrderStore.page <= 0 || workOrderStore.isLoading"
            @click="searchWorkOrders(workOrderStore.page - 1)"
          >
            이전
          </button>
          <button
            class="app-page-button"
            :disabled="workOrderStore.page + 1 >= workOrderStore.totalPages || workOrderStore.isLoading"
            @click="searchWorkOrders(workOrderStore.page + 1)"
          >
            다음
          </button>
          <button
            class="app-page-button"
            :disabled="workOrderStore.page + 1 >= workOrderStore.totalPages || workOrderStore.isLoading"
            @click="searchWorkOrders(workOrderStore.totalPages - 1)"
          >
            마지막
          </button>
          </div>
        </div>
      </section>
    </section>

  </div>
</template>
