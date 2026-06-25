<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChevronDown,
  Factory,
  GitBranch,
  Layers3,
  Loader2,
  Plus,
  RefreshCw,
  Route,
  Search,
  X
} from '@lucide/vue'
import { useFactoryRoutingStore } from '@/state/factoryRoutingStore'
import { useAuthStore } from '@/state/authStore'
import { formatDateTime } from '@/utils/dateFormat'
import type { FactoryRoutingRequest, FactoryRoutingResponse, FactoryRoutingStatus } from '@/api/factoryRoutingApi'

const factoryRoutingStore = useFactoryRoutingStore()
const authStore = useAuthStore()
const router = useRouter()

const isSearchExpanded = ref(true)
const filterFactoryName = ref('')
const filterLineName = ref('')
const filterRoutingStatus = ref<FactoryRoutingStatus | ''>('')
const appliedFactoryName = ref('')
const appliedLineName = ref('')
const appliedRoutingStatus = ref<FactoryRoutingStatus | ''>('')
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const pageSize = 10
const currentPage = ref(0)
const sortField = ref<keyof FactoryRoutingResponse>('factoryName')
const sortDirection = ref<'asc' | 'desc'>('asc')

const isFormOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formError = ref<string | null>(null)
const editingRoutingId = ref<number | null>(null)
const form = reactive<FactoryRoutingRequest>({
  factoryName: '',
  lineName: '',
  operationSeq: 1,
  operationName: ''
})

const selectedRouting = computed(() => factoryRoutingStore.selectedRouting)
const canManageMasterData = computed(() => authStore.canManageMasterData)
const totalPages = computed(() => Math.max(Math.ceil(sortedRoutings.value.length / pageSize), 1))

const stats = computed(() => {
  const factoryNames = new Set(factoryRoutingStore.routings.map((routing) => routing.factoryName))
  const lineKeys = new Set(factoryRoutingStore.routings.map((routing) => `${routing.factoryName}::${routing.lineName}`))
  const inactiveCount = factoryRoutingStore.routings.filter((routing) => routing.routingStatus === 'INACTIVE').length
  return {
    factories: factoryNames.size,
    lines: lineKeys.size,
    operations: factoryRoutingStore.routings.length,
    inactive: inactiveCount
  }
})

const filteredRoutingTree = computed(() => {
  return factoryRoutingStore.routingTree
    .filter((factory) => {
      if (!appliedFactoryName.value) return true
      return factory.factoryName === appliedFactoryName.value
    })
    .map((factory) => ({
      ...factory,
      lines: factory.lines.filter((line) => {
        if (!appliedLineName.value) return true
        return line.lineName === appliedLineName.value
      })
    }))
    .filter((factory) => factory.lines.length > 0)
})

const sortedRoutings = computed(() => {
  return [...factoryRoutingStore.routings].sort((a, b) => {
    const aValue = a[sortField.value]
    const bValue = b[sortField.value]
    let result = 0

    if (typeof aValue === 'number' && typeof bValue === 'number') {
      result = aValue - bValue
    } else {
      result = String(aValue ?? '').localeCompare(String(bValue ?? ''), 'ko-KR', { numeric: true })
    }

    return sortDirection.value === 'asc' ? result : -result
  })
})

const pagedRoutings = computed(() => {
  const start = currentPage.value * pageSize
  return sortedRoutings.value.slice(start, start + pageSize)
})

watch(filterFactoryName, async (factoryName) => {
  filterLineName.value = ''
  if (!factoryName) {
    factoryRoutingStore.clearLines()
    return
  }

  try {
    await factoryRoutingStore.loadLines(factoryName)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '라인 목록을 불러오지 못했습니다.'
  }
})

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await Promise.all([
      factoryRoutingStore.loadRoutings({
        factoryName: appliedFactoryName.value || undefined,
        lineName: appliedLineName.value || undefined,
        routingStatus: appliedRoutingStatus.value || undefined
      }),
      factoryRoutingStore.loadFactories(),
      factoryRoutingStore.loadRoutingTree()
    ])
    normalizeCurrentPage()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '공장/생산라인 기준정보를 불러오지 못했습니다.'
  }
}

async function handleSearch() {
  appliedFactoryName.value = filterFactoryName.value
  appliedLineName.value = filterLineName.value
  appliedRoutingStatus.value = filterRoutingStatus.value
  factoryRoutingStore.selectRouting(null)
  currentPage.value = 0
  await fetchPageData()
}

async function handleRefresh() {
  await fetchPageData()
  showToast('공장 및 생산라인 기준정보가 새로고침되었습니다.')
}

function resetFilters() {
  filterFactoryName.value = ''
  filterLineName.value = ''
  filterRoutingStatus.value = ''
  sortField.value = 'factoryName'
  sortDirection.value = 'asc'
  currentPage.value = 0
}

function selectRow(routing: FactoryRoutingResponse) {
  goToDetail(routing.routingId)
}

function selectTreeOperation(routingId: number) {
  goToDetail(routingId)
}

function goToDetail(routingId: number) {
  router.push({ name: 'factory-line-master-detail', params: { id: routingId } })
}

function openCreateForm() {
  formMode.value = 'create'
  editingRoutingId.value = null
  resetForm()
  formError.value = null
  isFormOpen.value = true
}

function openEditForm(routing: FactoryRoutingResponse) {
  formMode.value = 'edit'
  editingRoutingId.value = routing.routingId
  form.factoryName = routing.factoryName
  form.lineName = routing.lineName
  form.operationSeq = routing.operationSeq
  form.operationName = routing.operationName
  formError.value = null
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  formError.value = null
}

function resetForm() {
  form.factoryName = ''
  form.lineName = ''
  if (selectedRouting.value) {
    form.factoryName = selectedRouting.value.factoryName
    form.lineName = selectedRouting.value.lineName
  } else if (filterFactoryName.value || filterLineName.value) {
    form.factoryName = filterFactoryName.value
    form.lineName = filterLineName.value
  }
  form.operationSeq = nextOperationSeq.value
  form.operationName = ''
}

const nextOperationSeq = computed(() => {
  const factoryName = selectedRouting.value?.factoryName || filterFactoryName.value
  const lineName = selectedRouting.value?.lineName || filterLineName.value
  if (!factoryName || !lineName) return 1
  const maxSeq = factoryRoutingStore.routings
    .filter((routing) => routing.factoryName === factoryName && routing.lineName === lineName)
    .reduce((max, routing) => Math.max(max, routing.operationSeq), 0)
  return maxSeq + 1
})

const hasOperationSeqConflict = computed(() => {
  const payload = normalizeForm()
  if (!payload.factoryName || !payload.lineName || !payload.operationSeq) return false
  return factoryRoutingStore.routings.some((routing) => (
    routing.factoryName === payload.factoryName
    && routing.lineName === payload.lineName
    && routing.operationSeq === payload.operationSeq
    && routing.routingId !== editingRoutingId.value
  ))
})

async function submitForm() {
  const payload = normalizeForm()
  const validationError = validateForm(payload)
  if (validationError) {
    formError.value = validationError
    return
  }
  if (hasOperationSeqConflict.value) {
    formError.value = '동일한 공장/라인/공정 순서의 라우팅이 이미 존재합니다.'
    return
  }

  try {
    formError.value = null
    if (formMode.value === 'create') {
      await factoryRoutingStore.createRouting(payload)
      showToast('신규 공장/라인 라우팅이 등록되었습니다.')
    } else if (editingRoutingId.value !== null) {
      await factoryRoutingStore.updateRouting(editingRoutingId.value, payload)
      showToast('공장/라인 라우팅 정보가 수정되었습니다.')
    }
    closeForm()
    await fetchPageData()
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '라우팅 저장에 실패했습니다.'
  }
}

function normalizeForm(): FactoryRoutingRequest {
  return {
    factoryName: form.factoryName.trim(),
    lineName: form.lineName.trim(),
    operationSeq: Number(form.operationSeq),
    operationName: form.operationName.trim()
  }
}

function validateForm(payload: FactoryRoutingRequest) {
  if (!payload.factoryName) return '공장명을 입력해주세요.'
  if (!payload.lineName) return '생산 라인명을 입력해주세요.'
  if (!payload.operationName) return '세부 공정명을 입력해주세요.'
  if (payload.factoryName.length > 50) return '공장명은 50자 이하여야 합니다.'
  if (payload.lineName.length > 50) return '생산 라인명은 50자 이하여야 합니다.'
  if (payload.operationName.length > 50) return '세부 공정명은 50자 이하여야 합니다.'
  if (!Number.isInteger(payload.operationSeq) || payload.operationSeq < 1) return '공정 순서는 1 이상의 정수여야 합니다.'
  return null
}

function getRoutingStatusLabel(status: FactoryRoutingStatus) {
  return status === 'ACTIVE' ? '활성' : '비활성'
}

function changeSort(field: keyof FactoryRoutingResponse) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
  currentPage.value = 0
}

function getSortMark(field: keyof FactoryRoutingResponse) {
  if (sortField.value !== field) return ''
  return sortDirection.value === 'asc' ? '▲' : '▼'
}

function fetchPage(page: number) {
  currentPage.value = Math.min(Math.max(page, 0), totalPages.value - 1)
}

function normalizeCurrentPage() {
  if (currentPage.value > totalPages.value - 1) {
    currentPage.value = totalPages.value - 1
  }
}

function showToast(message: string) {
  successToast.value = message
  window.setTimeout(() => {
    successToast.value = null
  }, 2200)
}
</script>

<template>
  <div class="factory-master-page">
    <div class="factory-master-header">
      <div>
        <h1 class="app-page-title">
          공장 및 생산 라인 마스터
        </h1>
        <p class="app-page-subtitle">공장, 생산라인, 공정 순서 기준을 등록하고 작업지시에서 사용할 라우팅 기준정보를 관리합니다.</p>
      </div>
      <div v-if="successToast" class="factory-master-toast">
        <span class="factory-master-toast-dot"></span>
        {{ successToast }}
      </div>
    </div>

    <div v-if="pageError" class="factory-master-error">
      <span>{{ pageError }}</span>
      <button class="app-icon-button" type="button" @click="pageError = null">
        <X class="factory-master-icon-sm" />
      </button>
    </div>

    <div class="app-news-grid">
      <div class="app-news-card">
        <div>
          <p class="app-news-label">등록 공장</p>
          <p class="app-news-value">{{ stats.factories }} 개</p>
        </div>
        <div class="app-news-icon app-bg-primary-soft app-accent">
          <Factory />
        </div>
      </div>
      <div class="app-news-card">
        <div>
          <p class="app-news-label">생산 라인</p>
          <p class="app-news-value app-text-success">{{ stats.lines }} 개</p>
        </div>
        <div class="app-news-icon app-bg-success-soft app-text-success">
          <GitBranch />
        </div>
      </div>
      <div class="app-news-card">
        <div>
          <p class="app-news-label">세부 공정</p>
          <p class="app-news-value app-text-strong">{{ stats.operations }} 건</p>
          <p class="app-news-label">비활성 {{ stats.inactive }} 건</p>
        </div>
        <div class="app-news-icon">
          <Route />
        </div>
      </div>
    </div>

    <section class="app-search-panel">
      <div class="app-panel-head -mx-5 -mt-5 mb-5 rounded-t-[var(--radius-panel)]">
        <span class="app-panel-title">
          <Search class="app-panel-icon" />
          조회 검색 조건
        </span>
        <button class="app-icon-button" type="button" @click="isSearchExpanded = !isSearchExpanded">
          <ChevronDown class="h-4 w-4" :class="{ 'factory-master-chevron-collapsed': !isSearchExpanded }" />
        </button>
      </div>

      <div v-show="isSearchExpanded" class="app-filter-grid">
        <div class="app-field">
          <label class="app-label" for="factory-filter">공장</label>
          <select id="factory-filter" v-model="filterFactoryName" class="app-control app-control-lg">
            <option value="">전체 공장</option>
            <option v-for="factory in factoryRoutingStore.factories" :key="factory" :value="factory">
              {{ factory }}
            </option>
          </select>
        </div>
        <div class="app-field">
          <label class="app-label" for="line-filter">생산 라인</label>
          <select id="line-filter" v-model="filterLineName" class="app-control app-control-lg" :disabled="!filterFactoryName">
            <option value="">전체 라인</option>
            <option v-for="line in factoryRoutingStore.lines" :key="line" :value="line">
              {{ line }}
            </option>
          </select>
        </div>
        <div class="app-field">
          <label class="app-label" for="status-filter">상태</label>
          <select id="status-filter" v-model="filterRoutingStatus" class="app-control app-control-lg">
            <option value="">전체 상태</option>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
          </select>
        </div>
        <div class="app-search-actions items-end">
          <button class="app-button app-button-lg app-button-muted" type="button" @click="resetFilters">초기화</button>
          <button class="app-button app-button-lg app-button-primary" type="button" @click="handleSearch">조회</button>
        </div>
      </div>
    </section>

    <div class="factory-master-content-grid">
      <section class="app-panel factory-master-tree-panel">
        <div class="app-panel-head">
          <span class="app-panel-title">
            <Layers3 class="factory-master-icon-sm" />
            공장 / 라인 / 공정 구조
          </span>
        </div>
        <div class="factory-master-tree-body">
          <div v-if="filteredRoutingTree.length === 0" class="factory-master-empty-detail">
            <Layers3 class="factory-master-empty-icon" />
            <span>등록된 라우팅 구조가 없습니다.</span>
          </div>
          <div v-for="factory in filteredRoutingTree" v-else :key="factory.factoryName" class="factory-master-tree-factory">
            <div class="factory-master-tree-factory-name">
              <Factory class="factory-master-icon-sm" />
              {{ factory.factoryName }}
            </div>
            <div class="factory-master-tree-lines">
              <div v-for="line in factory.lines" :key="line.lineName" class="factory-master-tree-line">
                <div class="factory-master-tree-line-name">{{ line.lineName }}</div>
                <button
                  v-for="operation in line.operations"
                  :key="operation.routingId"
                  class="factory-master-tree-operation"
                  type="button"
                  @click="selectTreeOperation(operation.routingId)"
                >
                  <span>{{ operation.operationSeq }}</span>
                  <strong>{{ operation.operationName }}</strong>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="app-panel factory-master-list-panel">
        <div class="app-panel-head">
          <div class="factory-master-toolbar-copy">
            <span class="app-list-title">공장 및 생산라인 목록</span>
          </div>
          <div class="app-actions">
            <button class="app-button app-button-muted" type="button" @click="handleRefresh">
              <RefreshCw class="factory-master-icon-sm" />
              새로고침
            </button>
            <button v-if="canManageMasterData" class="app-button app-button-primary" type="button" @click="openCreateForm">
              <Plus class="factory-master-icon-sm" />
              신규 라우팅 등록
            </button>
          </div>
        </div>

        <div class="app-table-wrap">
          <table class="app-table">
            <thead>
              <tr>
                <th class="text-center">No</th>
                <th class="app-sortable-header" @click="changeSort('factoryName')">공장명 <span class="app-sort-mark">{{ getSortMark('factoryName') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('lineName')">생산 라인 <span class="app-sort-mark">{{ getSortMark('lineName') }}</span></th>
                <th class="app-sortable-header text-center" @click="changeSort('operationSeq')">공정 순서 <span class="app-sort-mark">{{ getSortMark('operationSeq') }}</span></th>
                <th class="app-sortable-header" @click="changeSort('operationName')">세부 공정명 <span class="app-sort-mark">{{ getSortMark('operationName') }}</span></th>
                <th class="app-sortable-header text-center" @click="changeSort('routingStatus')">상태 <span class="app-sort-mark">{{ getSortMark('routingStatus') }}</span></th>
                <th class="app-sortable-header text-center" @click="changeSort('createdAt')">등록일시 <span class="app-sort-mark">{{ getSortMark('createdAt') }}</span></th>
                <th class="app-sortable-header text-center" @click="changeSort('lastExecutionAt')">최근 생산 실적 일시 <span class="app-sort-mark">{{ getSortMark('lastExecutionAt') }}</span></th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="factoryRoutingStore.isLoading">
                <td colspan="8" class="app-empty">
                  <Loader2 class="factory-master-spinner" />
                  라우팅 기준정보를 가져오고 있습니다...
                </td>
              </tr>
              <tr v-else-if="sortedRoutings.length === 0">
                <td colspan="8" class="app-empty">조회된 공장/생산라인 기준정보가 없습니다.</td>
              </tr>
              <tr
                v-for="(routing, index) in pagedRoutings"
                v-else
                :key="routing.routingId"
                class="app-table-row"
                @click="selectRow(routing)"
              >
                <td class="text-center app-table-id">{{ currentPage * pageSize + index + 1 }}</td>
                <td class="app-table-main">{{ routing.factoryName }}</td>
                <td class="app-table-strong">{{ routing.lineName }}</td>
                <td class="text-center">
                  <span class="app-status app-status-neutral">{{ routing.operationSeq }}</span>
                </td>
                <td class="app-table-main">{{ routing.operationName }}</td>
                <td class="text-center">
                  <span class="app-status" :class="routing.routingStatus === 'ACTIVE' ? 'app-status-success' : 'app-status-warning'">{{ getRoutingStatusLabel(routing.routingStatus) }}</span>
                </td>
                <td class="text-center app-table-id">{{ formatDateTime(routing.createdAt) }}</td>
                <td class="text-center app-table-id">{{ formatDateTime(routing.lastExecutionAt) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="3">현재 조회 목록: {{ sortedRoutings.length }}건</td>
                <td colspan="5" class="factory-master-cell-right">공장 {{ stats.factories }}개 / 라인 {{ stats.lines }}개</td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div class="app-pagination">
          <p>총 {{ sortedRoutings.length }}건 · {{ currentPage + 1 }} / {{ totalPages }} 페이지</p>
          <div class="app-pagination-actions">
            <button class="app-page-button" type="button" :disabled="currentPage === 0" @click="fetchPage(0)">처음</button>
            <button class="app-page-button" type="button" :disabled="currentPage === 0" @click="fetchPage(currentPage - 1)">이전</button>
            <button class="app-page-button" type="button" :disabled="currentPage >= totalPages - 1" @click="fetchPage(currentPage + 1)">다음</button>
            <button class="app-page-button" type="button" :disabled="currentPage >= totalPages - 1" @click="fetchPage(totalPages - 1)">마지막</button>
          </div>
        </div>
      </section>
    </div>

    <div v-if="isFormOpen" class="factory-master-modal" role="dialog" aria-modal="true">
      <div class="factory-master-modal-backdrop" @click="closeForm"></div>
      <div class="factory-master-modal-shell">
        <div class="factory-master-modal-card">
          <div class="factory-master-modal-heading">
            <h2>{{ formMode === 'create' ? '신규 공장/생산라인 라우팅 등록' : '공장/생산라인 라우팅 수정' }}</h2>
            <button class="app-icon-button" type="button" @click="closeForm">
              <X class="factory-master-icon-sm" />
            </button>
          </div>

          <form class="factory-master-form" @submit.prevent="submitForm">
            <div v-if="formError" class="factory-master-form-error">{{ formError }}</div>

            <div class="factory-master-field">
              <label class="factory-master-label" for="factory-name">공장명 *</label>
              <input id="factory-name" v-model="form.factoryName" class="app-control app-control-lg" maxlength="50" placeholder="예: 창원제1공장" required>
            </div>

            <div class="factory-master-form-grid">
              <div class="factory-master-field">
                <label class="factory-master-label" for="line-name">생산 라인명 *</label>
                <input id="line-name" v-model="form.lineName" class="app-control app-control-lg" maxlength="50" placeholder="예: A라인" required>
              </div>
              <div class="factory-master-field">
                <label class="factory-master-label" for="operation-seq">공정 순서 *</label>
                <input id="operation-seq" v-model.number="form.operationSeq" class="app-control app-control-lg" type="number" min="1" required>
              </div>
            </div>

            <div class="factory-master-field">
              <label class="factory-master-label" for="operation-name">세부 공정명 *</label>
              <input id="operation-name" v-model="form.operationName" class="app-control app-control-lg" maxlength="50" placeholder="예: SMD 표면실장 공정" required>
            </div>

            <div class="factory-master-form-notice">
              동일한 공장/라인/공정 순서 조합은 저장 전에 확인하며, 서버에서도 중복 등록이 차단됩니다.
              <span v-if="hasOperationSeqConflict">현재 입력한 공정 순서는 이미 사용 중입니다.</span>
            </div>

            <div class="factory-master-modal-actions">
              <button class="app-button app-button-lg app-button-muted" type="button" @click="closeForm">취소</button>
              <button class="app-button app-button-lg app-button-primary" type="submit" :disabled="factoryRoutingStore.isSaving">
                <Loader2 v-if="factoryRoutingStore.isSaving" class="factory-master-spinner-inline" />
                {{ formMode === 'create' ? '라우팅 등록' : '수정 저장' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "../main.css";

.factory-master-page {
  --factory-color-page: var(--color-page);
  --factory-color-surface: var(--color-surface);
  --factory-color-surface-muted: var(--color-border-muted);
  --factory-color-surface-soft: var(--color-surface-soft);
  --factory-color-border: var(--color-border);
  --factory-color-border-strong: var(--color-border-strong);
  --factory-color-text-primary: var(--color-text);
  --factory-color-text-secondary: var(--color-text-soft);
  --factory-color-text-muted: var(--color-text-muted);
  --factory-color-primary: var(--color-primary);
  --factory-color-primary-hover: var(--color-primary-hover);
  --factory-color-primary-soft: var(--color-primary-soft);
  --factory-color-primary-border: var(--color-primary-border);
  --factory-color-primary-hover-soft: var(--color-primary-muted);
  --factory-color-success: var(--color-success);
  --factory-color-success-soft: var(--color-success-soft);
  --factory-color-success-border: var(--color-success-border);
  --factory-color-warning: var(--color-warning);
  --factory-color-warning-soft: var(--color-warning-soft);
  --factory-color-warning-border: var(--color-warning-border);
  --factory-color-danger: var(--color-danger);
  --factory-color-danger-soft: var(--color-danger-soft);
  --factory-color-danger-border: var(--color-danger-border);
  --factory-color-danger-hover-soft: var(--color-danger-muted);
  --factory-color-table-divider: var(--color-border-muted);
  --factory-font-weight-title: var(--font-weight-title);
  --factory-font-weight-label: var(--font-weight-title);
  --factory-font-weight-strong: var(--font-weight-emphasis);
  --factory-font-weight-body: 500;
  --factory-font-weight-muted: var(--font-weight-label);
  --factory-radius-panel: var(--radius-section);
  --factory-shadow-panel: var(--shadow-panel);

  @apply space-y-6 pb-12;
  background: var(--factory-color-page);
}

.factory-master-header,
.factory-master-toolbar,
.factory-master-detail-heading,
.factory-master-panel-heading,
.factory-master-modal-heading,
.factory-master-modal-actions {
  @apply flex items-center justify-between gap-4;
}

.factory-master-title {
    @apply flex items-center gap-2 tracking-tight;
  font-size: var(--app-font-size-2xl);
  line-height: var(--app-line-height-2xl);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-title);
}

.factory-master-title-mark {
  @apply h-6 w-1.5 rounded-sm;
  background-color: var(--factory-color-primary);
}

.factory-master-subtitle {
    @apply mt-1.5;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-body);
}

.factory-master-toast {
    @apply flex items-center gap-2 rounded-lg border px-4 py-2 shadow-sm;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  background-color: var(--factory-color-success-soft);
  border-color: var(--factory-color-success-border);
  color: var(--factory-color-success);
  font-weight: var(--factory-font-weight-label);
  animation: factoryMasterFadeIn 0.2s ease-out forwards;
}

.factory-master-toast-dot {
  @apply h-2 w-2 rounded-full;
  background-color: var(--factory-color-success);
}

.factory-master-error,
.factory-master-form-error {
    @apply flex items-center justify-between rounded-lg border px-4 py-3;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  background-color: var(--factory-color-danger-soft);
  border-color: var(--factory-color-danger-border);
  color: var(--factory-color-danger);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-stats-grid {
  @apply grid grid-cols-1 gap-5 sm:grid-cols-3;
}

.factory-master-stat-card {
  @apply flex items-center gap-4 rounded-xl border p-6 transition-shadow hover:shadow-md;
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border);
  box-shadow: var(--factory-shadow-panel);
}

.factory-master-stat-icon {
  @apply rounded-lg p-3;
}

.factory-master-stat-icon-neutral {
  background-color: var(--factory-color-surface-muted);
  color: var(--factory-color-text-secondary);
}

.factory-master-stat-icon-primary {
  background-color: var(--factory-color-primary-soft);
  color: var(--factory-color-primary);
}

.factory-master-stat-icon-success {
  background-color: var(--factory-color-success-soft);
  color: var(--factory-color-success);
}

.factory-master-stat-label {
    @apply uppercase tracking-wider;
  font-size: var(--app-font-size-2xs);
  line-height: var(--app-line-height-2xs);
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-stat-value {
    @apply mt-1;
  font-size: var(--app-font-size-xl);
  line-height: var(--app-line-height-xl);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-panel {
  @apply overflow-hidden rounded-xl border;
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border);
  box-shadow: var(--factory-shadow-panel);
}

.factory-master-panel-heading,
.factory-master-detail-heading {
  @apply border-b px-5 py-3.5;
  background-color: var(--factory-color-surface-muted);
  border-color: var(--factory-color-border);
}

.factory-master-panel-title,
.factory-master-detail-title {
    @apply flex items-center gap-2;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-icon-button {
  @apply rounded p-1 transition-colors;
  color: var(--factory-color-text-muted);
}

.factory-master-icon-button:hover {
  background-color: var(--factory-color-border);
  color: var(--factory-color-text-secondary);
}

.factory-master-search-grid {
    @apply grid grid-cols-1 gap-5 border-t p-5 sm:grid-cols-2 lg:grid-cols-4;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  border-color: var(--factory-color-border);
}

.factory-master-field {
  @apply space-y-1.5;
}

.factory-master-label {
  @apply block;
  color: var(--factory-color-text-secondary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-input {
    @apply h-9 w-full rounded-lg border px-3 outline-none transition disabled:cursor-not-allowed disabled:opacity-60;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border-strong);
  color: var(--factory-color-text-primary);
}

.factory-master-input:focus {
  border-color: var(--factory-color-primary);
  box-shadow: 0 0 0 1px var(--factory-color-primary);
}

.factory-master-search-actions {
  @apply flex items-end justify-end gap-2 sm:col-span-2 lg:col-span-2;
}

.factory-master-primary-button,
.factory-master-secondary-button {
    @apply inline-flex h-9 items-center justify-center gap-2 rounded-lg px-4 transition disabled:cursor-not-allowed disabled:opacity-60;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-primary-button {
  background-color: var(--factory-color-primary);
  color: var(--factory-color-surface);
}

.factory-master-primary-button:hover {
  background-color: var(--factory-color-primary-hover);
}

.factory-master-secondary-button {
  background-color: var(--factory-color-surface-muted);
  color: var(--factory-color-text-secondary);
}

.factory-master-secondary-button:hover {
  background-color: var(--factory-color-border);
}

.factory-master-content-grid {
  @apply grid grid-cols-1 gap-6;
}

.factory-master-tree-panel {
  @apply flex min-h-[28rem] min-w-0 flex-col;
}

.factory-master-tree-body {
  @apply grid min-h-0 flex-1 grid-flow-col auto-cols-[minmax(18rem,20rem)] gap-5 overflow-x-auto overflow-y-hidden p-5;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  scroll-snap-type: x proximity;
}

.factory-master-tree-factory {
  @apply flex min-w-0 shrink-0 flex-col space-y-3 rounded-xl border p-3;
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border);
  scroll-snap-align: start;
}

.factory-master-tree-factory-name {
  @apply flex min-h-10 items-center gap-2 truncate rounded-lg border px-3 py-2;
  background-color: var(--factory-color-primary-soft);
  border-color: var(--factory-color-primary-border);
  color: var(--factory-color-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-tree-lines {
  @apply max-h-[360px] space-y-4 overflow-y-auto overflow-x-hidden pl-4 pr-2;
}

.factory-master-tree-line {
  @apply border-l pl-3;
  border-color: var(--factory-color-border-strong);
}

.factory-master-tree-line-name {
  @apply mb-2 truncate;
  font-size: var(--app-font-size-11);
  line-height: var(--app-line-height-11);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-tree-operation {
  @apply mb-1.5 flex min-h-10 w-full min-w-0 items-center gap-2 rounded-lg border px-3 py-2 text-left transition-colors;
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border);
  color: var(--factory-color-text-secondary);
}

.factory-master-tree-operation:hover,
.factory-master-tree-operation-active {
  background-color: var(--factory-color-surface-soft);
  border-color: var(--factory-color-primary-border);
}

.factory-master-tree-operation span {
  @apply flex h-6 w-6 shrink-0 items-center justify-center rounded-full;
  font-size: var(--app-font-size-2xs);
  line-height: var(--app-line-height-2xs);
  background-color: var(--factory-color-primary);
  color: var(--factory-color-surface);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-tree-operation strong {
  @apply block min-w-0 truncate;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-toolbar {
  @apply flex-wrap border-b px-5 py-4;
  background-color: var(--factory-color-surface-muted);
  border-color: var(--factory-color-border);
}

.factory-master-toolbar-copy {
    @apply flex flex-col gap-1;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-toolbar-actions,
.factory-master-row-actions,
.factory-master-detail-heading-actions {
  @apply flex items-center gap-2;
}

.factory-master-table-wrap {
  @apply overflow-x-auto;
}

.factory-master-table {
    @apply w-full min-w-[980px] border-collapse text-left;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  color: var(--factory-color-text-secondary);
}

.factory-master-table th {
  @apply whitespace-nowrap border-b border-r px-4 py-3 uppercase last:border-r-0;
  background-color: var(--factory-color-surface-muted);
  border-color: var(--factory-color-border);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-table td {
  @apply whitespace-nowrap border-r px-4 py-3 last:border-r-0;
  border-color: var(--factory-color-table-divider);
}

.factory-master-table tbody tr {
  @apply border-b;
  border-color: var(--factory-color-border);
}

.factory-master-table-row {
  @apply cursor-pointer transition-colors;
}

.factory-master-table-row:hover,
.factory-master-table-row-selected {
  background-color: var(--factory-color-surface-soft);
}

.factory-master-cell-center {
  @apply text-center;
}

.factory-master-cell-right {
  @apply text-right;
}

.factory-master-code,
.factory-master-date {
  @apply font-mono;
}

.factory-master-code,
.factory-master-name {
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-date {
  color: var(--factory-color-text-muted);
}

.factory-master-seq-badge {
    @apply inline-flex rounded border px-2.5 py-0.5;
  font-size: var(--app-font-size-2xs);
  line-height: var(--app-line-height-2xs);
  background-color: var(--factory-color-warning-soft);
  border-color: var(--factory-color-warning-border);
  color: var(--factory-color-warning);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-table-button,
.factory-master-table-danger-button {
    @apply inline-flex items-center gap-1 rounded border px-3 py-1.5 transition-colors;
  font-size: var(--app-font-size-2xs);
  line-height: var(--app-line-height-2xs);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-table-button {
  background-color: var(--factory-color-primary-soft);
  border-color: var(--factory-color-primary-border);
  color: var(--factory-color-primary);
}

.factory-master-table-button:hover {
  background-color: var(--factory-color-primary-hover-soft);
}

.factory-master-table-danger-button {
  background-color: var(--factory-color-danger-soft);
  border-color: var(--factory-color-danger-border);
  color: var(--factory-color-danger);
}

.factory-master-table-danger-button:hover {
  background-color: var(--factory-color-danger-hover-soft);
}

.factory-master-empty-cell {
  @apply py-12 text-center;
  color: var(--factory-color-text-muted);
}

.factory-master-table tfoot {
  background-color: var(--factory-color-surface-soft);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-detail-code {
    @apply font-mono;
  font-size: var(--app-font-size-11);
  line-height: var(--app-line-height-11);
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-detail-body {
    @apply min-h-[160px] p-6;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
}

.factory-master-empty-detail {
  @apply flex flex-col items-center justify-center gap-2 py-8 text-center;
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-body);
}

.factory-master-empty-icon {
  @apply h-8 w-8;
  color: var(--factory-color-border-strong);
}

.factory-master-detail-tabs {
  @apply mb-5 flex gap-1.5;
}

.factory-master-tab-button {
    @apply rounded-md px-3 py-1.5 transition-colors;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-tab-button:hover {
  background-color: var(--factory-color-surface-muted);
  color: var(--factory-color-text-primary);
}

.factory-master-tab-button-active {
  background-color: var(--factory-color-primary);
  color: var(--factory-color-surface);
}

.factory-master-detail-grid {
  @apply grid grid-cols-1 gap-8 md:grid-cols-2;
}

.factory-master-detail-section {
  @apply space-y-3.5;
}

.factory-master-detail-section h3 {
    @apply border-b pb-2;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  border-color: var(--factory-color-border);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-description-list {
  @apply grid grid-cols-3 gap-x-2 gap-y-2.5;
}

.factory-master-description-list dt {
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-body);
}

.factory-master-description-list dd {
  @apply col-span-2;
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-warning-box {
  @apply rounded-lg border p-4;
  background-color: var(--factory-color-warning-soft);
  border-color: var(--factory-color-warning-border);
  color: var(--factory-color-warning);
}

.factory-master-warning-box p {
  @apply leading-5;
  font-weight: var(--factory-font-weight-body);
}

.factory-master-flow-wrap {
  @apply flex min-h-[180px] items-center gap-3 overflow-x-auto rounded-xl border p-5;
  background-color: var(--factory-color-surface-muted);
  border-color: var(--factory-color-border);
}

.factory-master-flow-item {
  @apply flex shrink-0 items-center gap-3;
}

.factory-master-flow-card {
  @apply flex min-h-28 w-56 flex-col items-start justify-between rounded-xl border p-4 text-left transition-colors;
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border);
  color: var(--factory-color-text-secondary);
}

.factory-master-flow-card:hover,
.factory-master-flow-card-active {
  background-color: var(--factory-color-surface-soft);
  border-color: var(--factory-color-primary-border);
}

.factory-master-flow-seq {
    @apply flex h-7 w-7 items-center justify-center rounded-full;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  background-color: var(--factory-color-primary);
  color: var(--factory-color-surface);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-flow-name {
    @apply mt-3 leading-5;
  font-size: var(--app-font-size-sm);
  line-height: var(--app-line-height-sm);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-flow-code {
    @apply mt-3 font-mono;
  font-size: var(--app-font-size-2xs);
  line-height: var(--app-line-height-2xs);
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-flow-arrow {
  font-size: var(--app-font-size-xl);
  line-height: var(--app-line-height-xl);
  color: var(--factory-color-primary);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-sortable-header {
  @apply cursor-pointer select-none transition-colors;
}

.factory-master-sortable-header:hover {
  color: var(--factory-color-primary);
}

.factory-master-sortable-header span {
    @apply ml-1;
  font-size: var(--app-font-size-2xs);
  line-height: var(--app-line-height-2xs);
}

.factory-master-pagination {
    @apply flex flex-col gap-3 border-t px-5 py-4 sm:flex-row sm:items-center sm:justify-between;
  font-size: var(--app-font-size-sm);
  line-height: var(--app-line-height-sm);
  border-color: var(--factory-color-border);
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-pagination-actions {
  @apply flex flex-wrap gap-2;
}

.factory-master-modal {
  @apply fixed inset-0 z-50 overflow-y-auto;
}

.factory-master-modal-backdrop {
  @apply fixed inset-0 backdrop-blur-sm;
  background-color: rgb(15 23 42 / 0.6);
}

.factory-master-modal-shell {
  @apply flex min-h-screen items-center justify-center p-4;
}

.factory-master-modal-card {
  @apply relative w-full max-w-lg overflow-hidden rounded-2xl border shadow-xl;
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border);
}

.factory-master-modal-heading {
  @apply border-b px-6 py-4;
  background-color: var(--factory-color-surface-muted);
  border-color: var(--factory-color-border);
}

.factory-master-modal-heading h2 {
  font-size: var(--app-font-size-sm);
  line-height: var(--app-line-height-sm);
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-form {
    @apply space-y-4 p-6;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
}

.factory-master-form-grid {
  @apply grid grid-cols-1 gap-4 sm:grid-cols-2;
}

.factory-master-form-notice {
    @apply rounded-lg border px-4 py-3;
  font-size: var(--app-font-size-xs);
  line-height: var(--app-line-height-xs);
  background-color: var(--factory-color-primary-soft);
  border-color: var(--factory-color-primary-border);
  color: var(--factory-color-primary);
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-modal-actions {
  @apply border-t pt-4;
  border-color: var(--factory-color-border);
}

.factory-master-icon-xs {
  @apply h-3 w-3;
}

.factory-master-icon-sm {
  @apply h-4 w-4;
}

.factory-master-icon-md {
  @apply h-6 w-6;
}

.factory-master-chevron {
  @apply transition-transform duration-200;
}

.factory-master-chevron-collapsed {
  @apply rotate-180;
}

.factory-master-spinner,
.factory-master-spinner-inline {
  @apply animate-spin;
  color: var(--factory-color-primary);
}

.factory-master-spinner {
  @apply mr-2 inline h-5 w-5;
}

.factory-master-spinner-inline {
  @apply h-3.5 w-3.5;
  color: currentColor;
}

@keyframes factoryMasterFadeIn {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
