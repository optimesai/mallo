<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ChevronDown,
  Edit3,
  Factory,
  FileSpreadsheet,
  GitBranch,
  Layers3,
  Loader2,
  Plus,
  RefreshCw,
  Route,
  Search,
  Trash2,
  X
} from '@lucide/vue'
import { useFactoryRoutingStore } from '@/state/factoryRoutingStore'
import type { FactoryRoutingRequest, FactoryRoutingResponse } from '@/api/factoryRoutingApi'

const factoryRoutingStore = useFactoryRoutingStore()

const isSearchExpanded = ref(true)
const filterFactoryName = ref('')
const filterLineName = ref('')
const appliedFactoryName = ref('')
const appliedLineName = ref('')
const activeDetailTab = ref<'operation' | 'line' | 'flow' | 'system'>('operation')
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)

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

const stats = computed(() => {
  const factoryNames = new Set(factoryRoutingStore.routings.map((routing) => routing.factoryName))
  const lineKeys = new Set(factoryRoutingStore.routings.map((routing) => `${routing.factoryName}::${routing.lineName}`))
  return {
    factories: factoryNames.size,
    lines: lineKeys.size,
    operations: factoryRoutingStore.routings.length
  }
})

const selectedFactoryTree = computed(() => {
  if (!selectedRouting.value) return null
  return factoryRoutingStore.routingTree.find((factory) => factory.factoryName === selectedRouting.value?.factoryName) || null
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

const selectedLineOperationCount = computed(() => {
  if (!selectedRouting.value || !selectedFactoryTree.value) return 0
  const line = selectedFactoryTree.value.lines.find((item) => item.lineName === selectedRouting.value?.lineName)
  return line?.operations.length || 0
})

const selectedLineOperations = computed(() => {
  if (!selectedRouting.value) return []
  return factoryRoutingStore.routings
    .filter((routing) => (
      routing.factoryName === selectedRouting.value?.factoryName
      && routing.lineName === selectedRouting.value?.lineName
    ))
    .sort((a, b) => a.operationSeq - b.operationSeq)
})

const selectedLineSummary = computed(() => {
  const operations = selectedLineOperations.value
  return {
    firstOperation: operations[0]?.operationName || '-',
    lastOperation: operations[operations.length - 1]?.operationName || '-',
    oldestCreatedAt: operations
      .map((operation) => operation.createdAt)
      .sort()[0] || '',
    latestCreatedAt: operations
      .map((operation) => operation.createdAt)
      .sort()
      .reverse()[0] || ''
  }
})

watch(filterFactoryName, async (factoryName) => {
  filterLineName.value = ''
  if (!factoryName) {
    factoryRoutingStore.lines = []
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
        lineName: appliedLineName.value || undefined
      }),
      factoryRoutingStore.loadFactories(),
      factoryRoutingStore.loadRoutingTree()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '공장/생산라인 기준정보를 불러오지 못했습니다.'
  }
}

async function handleSearch() {
  appliedFactoryName.value = filterFactoryName.value
  appliedLineName.value = filterLineName.value
  factoryRoutingStore.selectRouting(null)
  await fetchPageData()
}

async function handleRefresh() {
  await fetchPageData()
  showToast('공장 및 생산라인 기준정보가 새로고침되었습니다.')
}

function resetFilters() {
  filterFactoryName.value = ''
  filterLineName.value = ''
}

function selectRow(routing: FactoryRoutingResponse) {
  if (selectedRouting.value?.routingId === routing.routingId) {
    factoryRoutingStore.selectRouting(null)
    return
  }
  factoryRoutingStore.selectRouting(routing)
}

async function selectTreeOperation(routingId: number) {
  const routing = factoryRoutingStore.routings.find((item) => item.routingId === routingId)
  if (routing) {
    factoryRoutingStore.selectRouting(routing)
    return
  }

  try {
    const loaded = await factoryRoutingStore.loadRouting(routingId)
    factoryRoutingStore.selectRouting(loaded)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '라우팅 상세 정보를 불러오지 못했습니다.'
  }
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
  form.operationSeq = nextOperationSeq.value
  form.operationName = ''
}

const nextOperationSeq = computed(() => {
  if (!filterFactoryName.value || !filterLineName.value) return 1
  const maxSeq = factoryRoutingStore.routings
    .filter((routing) => routing.factoryName === filterFactoryName.value && routing.lineName === filterLineName.value)
    .reduce((max, routing) => Math.max(max, routing.operationSeq), 0)
  return maxSeq + 1
})

async function submitForm() {
  const payload = normalizeForm()
  const validationError = validateForm(payload)
  if (validationError) {
    formError.value = validationError
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

async function requestDelete(routing: FactoryRoutingResponse) {
  const message = `[${routing.factoryName} / ${routing.lineName} / ${routing.operationSeq}공정] ${routing.operationName} 라우팅을 삭제하시겠습니까?`
  if (!confirm(message)) return

  try {
    pageError.value = null
    await factoryRoutingStore.deleteRouting(routing.routingId)
    showToast('라우팅 기준정보가 삭제되었습니다.')
    await fetchPageData()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '라우팅 삭제에 실패했습니다.'
  }
}

function formatDateTime(dateTimeStr: string) {
  if (!dateTimeStr) return '-'
  const date = new Date(dateTimeStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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
        <h1 class="factory-master-title">
          <span class="factory-master-title-mark"></span>
          공장 및 생산라인 Master
        </h1>
        <p class="factory-master-subtitle">공장, 생산라인, 공정 순서 기준을 등록하고 작업지시에서 사용할 라우팅 기준정보를 관리합니다.</p>
      </div>
      <div v-if="successToast" class="factory-master-toast">
        <span class="factory-master-toast-dot"></span>
        {{ successToast }}
      </div>
    </div>

    <div v-if="pageError" class="factory-master-error">
      <span>{{ pageError }}</span>
      <button class="factory-master-icon-button" type="button" @click="pageError = null">
        <X class="factory-master-icon-sm" />
      </button>
    </div>

    <div class="factory-master-stats-grid">
      <div class="factory-master-stat-card">
        <div class="factory-master-stat-icon factory-master-stat-icon-primary">
          <Factory class="factory-master-icon-md" />
        </div>
        <div>
          <p class="factory-master-stat-label">등록 공장</p>
          <p class="factory-master-stat-value">{{ stats.factories }} 개</p>
        </div>
      </div>
      <div class="factory-master-stat-card">
        <div class="factory-master-stat-icon factory-master-stat-icon-success">
          <GitBranch class="factory-master-icon-md" />
        </div>
        <div>
          <p class="factory-master-stat-label">생산 라인</p>
          <p class="factory-master-stat-value">{{ stats.lines }} 개</p>
        </div>
      </div>
      <div class="factory-master-stat-card">
        <div class="factory-master-stat-icon factory-master-stat-icon-neutral">
          <Route class="factory-master-icon-md" />
        </div>
        <div>
          <p class="factory-master-stat-label">세부 공정</p>
          <p class="factory-master-stat-value">{{ stats.operations }} 건</p>
        </div>
      </div>
    </div>

    <section class="factory-master-panel">
      <div class="factory-master-panel-heading">
        <span class="factory-master-panel-title">
          <Search class="factory-master-icon-sm" />
          조회 검색 조건
        </span>
        <button class="factory-master-icon-button" type="button" @click="isSearchExpanded = !isSearchExpanded">
          <ChevronDown class="factory-master-icon-sm factory-master-chevron" :class="{ 'factory-master-chevron-collapsed': !isSearchExpanded }" />
        </button>
      </div>

      <div v-show="isSearchExpanded" class="factory-master-search-grid">
        <div class="factory-master-field">
          <label class="factory-master-label" for="factory-filter">공장</label>
          <select id="factory-filter" v-model="filterFactoryName" class="factory-master-input">
            <option value="">전체 공장</option>
            <option v-for="factory in factoryRoutingStore.factories" :key="factory" :value="factory">
              {{ factory }}
            </option>
          </select>
        </div>
        <div class="factory-master-field">
          <label class="factory-master-label" for="line-filter">생산 라인</label>
          <select id="line-filter" v-model="filterLineName" class="factory-master-input" :disabled="!filterFactoryName">
            <option value="">전체 라인</option>
            <option v-for="line in factoryRoutingStore.lines" :key="line" :value="line">
              {{ line }}
            </option>
          </select>
        </div>
        <div class="factory-master-search-actions">
          <button class="factory-master-secondary-button" type="button" @click="resetFilters">초기화</button>
          <button class="factory-master-primary-button" type="button" @click="handleSearch">조회</button>
        </div>
      </div>
    </section>

    <div class="factory-master-content-grid">
      <section class="factory-master-panel factory-master-tree-panel">
        <div class="factory-master-panel-heading">
          <span class="factory-master-panel-title">
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
                  :class="{ 'factory-master-tree-operation-active': selectedRouting?.routingId === operation.routingId }"
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

      <section class="factory-master-panel factory-master-list-panel">
        <div class="factory-master-toolbar">
          <div class="factory-master-toolbar-copy">
            <span>공장명 + 라인명 + 공정 순서 조합은 중복 등록할 수 없습니다.</span>
            <span>작업지시에서 참조 중인 라우팅은 삭제가 차단됩니다.</span>
          </div>
          <div class="factory-master-toolbar-actions">
            <button class="factory-master-secondary-button" type="button" @click="handleRefresh">
              <RefreshCw class="factory-master-icon-sm" />
              새로고침
            </button>
            <button class="factory-master-primary-button" type="button" @click="openCreateForm">
              <Plus class="factory-master-icon-sm" />
              신규 라우팅 등록
            </button>
          </div>
        </div>

        <div class="factory-master-table-wrap">
          <table class="factory-master-table">
            <thead>
              <tr>
                <th class="factory-master-cell-center">No</th>
                <th>공장명</th>
                <th>생산 라인</th>
                <th class="factory-master-cell-center">공정 순서</th>
                <th>세부 공정명</th>
                <th class="factory-master-cell-center">등록일시</th>
                <th class="factory-master-cell-center">액션</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="factoryRoutingStore.isLoading">
                <td colspan="7" class="factory-master-empty-cell">
                  <Loader2 class="factory-master-spinner" />
                  라우팅 기준정보를 가져오고 있습니다...
                </td>
              </tr>
              <tr v-else-if="factoryRoutingStore.routings.length === 0">
                <td colspan="7" class="factory-master-empty-cell">조회된 공장/생산라인 기준정보가 없습니다.</td>
              </tr>
              <tr
                v-for="(routing, index) in factoryRoutingStore.routings"
                v-else
                :key="routing.routingId"
                class="factory-master-table-row"
                :class="{ 'factory-master-table-row-selected': selectedRouting?.routingId === routing.routingId }"
                @click="selectRow(routing)"
              >
                <td class="factory-master-cell-center">{{ index + 1 }}</td>
                <td class="factory-master-name">{{ routing.factoryName }}</td>
                <td class="factory-master-code">{{ routing.lineName }}</td>
                <td class="factory-master-cell-center">
                  <span class="factory-master-seq-badge">{{ routing.operationSeq }}</span>
                </td>
                <td class="factory-master-name">{{ routing.operationName }}</td>
                <td class="factory-master-cell-center factory-master-date">{{ formatDateTime(routing.createdAt) }}</td>
                <td class="factory-master-cell-center" @click.stop>
                  <div class="factory-master-row-actions">
                    <button class="factory-master-table-button" type="button" @click="openEditForm(routing)">
                      <Edit3 class="factory-master-icon-xs" />
                      수정
                    </button>
                    <button class="factory-master-table-danger-button" type="button" @click="requestDelete(routing)">
                      <Trash2 class="factory-master-icon-xs" />
                      삭제
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="3">현재 조회 목록: {{ factoryRoutingStore.routings.length }}건</td>
                <td colspan="4" class="factory-master-cell-right">공장 {{ stats.factories }}개 / 라인 {{ stats.lines }}개</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>
    </div>

    <section class="factory-master-panel">
      <div class="factory-master-detail-heading">
        <div class="factory-master-detail-title">
          <FileSpreadsheet class="factory-master-icon-sm" />
          선택 라우팅 세부 정보
        </div>
        <div v-if="selectedRouting" class="factory-master-detail-heading-actions">
          <div class="factory-master-detail-code">ROUTING-ID {{ selectedRouting.routingId }}</div>
          <button class="factory-master-table-button" type="button" @click="openEditForm(selectedRouting)">
            <Edit3 class="factory-master-icon-xs" />
            수정
          </button>
          <button class="factory-master-table-danger-button" type="button" @click="requestDelete(selectedRouting)">
            <Trash2 class="factory-master-icon-xs" />
            삭제
          </button>
        </div>
      </div>

      <div class="factory-master-detail-body">
        <div v-if="!selectedRouting" class="factory-master-empty-detail">
          <FileSpreadsheet class="factory-master-empty-icon" />
          <span>라우팅 목록 또는 구조 트리에서 행을 선택하면 공정 기준정보가 표시됩니다.</span>
        </div>
        <template v-else>
          <div class="factory-master-detail-tabs">
            <button
              type="button"
              class="factory-master-tab-button"
              :class="{ 'factory-master-tab-button-active': activeDetailTab === 'operation' }"
              @click="activeDetailTab = 'operation'"
            >
              공정 기준
            </button>
            <button
              type="button"
              class="factory-master-tab-button"
              :class="{ 'factory-master-tab-button-active': activeDetailTab === 'line' }"
              @click="activeDetailTab = 'line'"
            >
              라인 상세
            </button>
            <button
              type="button"
              class="factory-master-tab-button"
              :class="{ 'factory-master-tab-button-active': activeDetailTab === 'flow' }"
              @click="activeDetailTab = 'flow'"
            >
              공정 플로우
            </button>
            <button
              type="button"
              class="factory-master-tab-button"
              :class="{ 'factory-master-tab-button-active': activeDetailTab === 'system' }"
              @click="activeDetailTab = 'system'"
            >
              시스템 정보
            </button>
          </div>

          <div v-if="activeDetailTab === 'operation'" class="factory-master-detail-grid">
            <div class="factory-master-detail-section">
              <h3>라우팅 식별 정보</h3>
              <dl class="factory-master-description-list">
                <dt>공장명</dt>
                <dd>{{ selectedRouting.factoryName }}</dd>
                <dt>생산 라인</dt>
                <dd>{{ selectedRouting.lineName }}</dd>
                <dt>공정명</dt>
                <dd>{{ selectedRouting.operationName }}</dd>
              </dl>
            </div>
            <div class="factory-master-detail-section">
              <h3>운영 기준</h3>
              <dl class="factory-master-description-list">
                <dt>공정 순서</dt>
                <dd>{{ selectedRouting.operationSeq }} 번째</dd>
                <dt>라인 공정 수</dt>
                <dd>{{ selectedLineOperationCount }} 건</dd>
                <dt>등록 구조</dt>
                <dd>공장 &gt; 생산라인 &gt; 세부 공정</dd>
              </dl>
            </div>
          </div>

          <div v-else-if="activeDetailTab === 'line'" class="factory-master-detail-grid">
            <div class="factory-master-detail-section">
              <h3>라인 요약</h3>
              <dl class="factory-master-description-list">
                <dt>공장명</dt>
                <dd>{{ selectedRouting.factoryName }}</dd>
                <dt>생산 라인</dt>
                <dd>{{ selectedRouting.lineName }}</dd>
                <dt>공정 수</dt>
                <dd>{{ selectedLineOperations.length }} 건</dd>
              </dl>
            </div>
            <div class="factory-master-detail-section">
              <h3>라인 시작/종료</h3>
              <dl class="factory-master-description-list">
                <dt>첫 공정</dt>
                <dd>{{ selectedLineSummary.firstOperation }}</dd>
                <dt>마지막 공정</dt>
                <dd>{{ selectedLineSummary.lastOperation }}</dd>
                <dt>최근 등록</dt>
                <dd>{{ formatDateTime(selectedLineSummary.latestCreatedAt) }}</dd>
              </dl>
            </div>
          </div>

          <div v-else-if="activeDetailTab === 'flow'" class="factory-master-flow-wrap">
            <div
              v-for="(operation, index) in selectedLineOperations"
              :key="operation.routingId"
              class="factory-master-flow-item"
            >
              <button
                type="button"
                class="factory-master-flow-card"
                :class="{ 'factory-master-flow-card-active': selectedRouting.routingId === operation.routingId }"
                @click="selectRow(operation)"
              >
                <span class="factory-master-flow-seq">{{ operation.operationSeq }}</span>
                <span class="factory-master-flow-name">{{ operation.operationName }}</span>
                <span class="factory-master-flow-code">ROUTING-ID {{ operation.routingId }}</span>
              </button>
              <div v-if="index < selectedLineOperations.length - 1" class="factory-master-flow-arrow">→</div>
            </div>
          </div>

          <div v-else class="factory-master-detail-grid">
            <div class="factory-master-detail-section">
              <h3>등록 이력</h3>
              <dl class="factory-master-description-list">
                <dt>라우팅 ID</dt>
                <dd>{{ selectedRouting.routingId }}</dd>
                <dt>등록일시</dt>
                <dd>{{ formatDateTime(selectedRouting.createdAt) }}</dd>
                <dt>중복 기준</dt>
                <dd>공장명 + 라인명 + 공정 순서</dd>
              </dl>
            </div>
            <div class="factory-master-detail-section factory-master-warning-box">
              <h3>삭제 제한</h3>
              <p>작업지시에서 참조 중인 라우팅은 삭제할 수 없습니다. 삭제 실패 시 작업지시 연결 여부를 확인해야 합니다.</p>
            </div>
          </div>
        </template>
      </div>
    </section>

    <div v-if="isFormOpen" class="factory-master-modal" role="dialog" aria-modal="true">
      <div class="factory-master-modal-backdrop" @click="closeForm"></div>
      <div class="factory-master-modal-shell">
        <div class="factory-master-modal-card">
          <div class="factory-master-modal-heading">
            <h2>{{ formMode === 'create' ? '신규 공장/생산라인 라우팅 등록' : '공장/생산라인 라우팅 수정' }}</h2>
            <button class="factory-master-icon-button" type="button" @click="closeForm">
              <X class="factory-master-icon-sm" />
            </button>
          </div>

          <form class="factory-master-form" @submit.prevent="submitForm">
            <div v-if="formError" class="factory-master-form-error">{{ formError }}</div>

            <div class="factory-master-field">
              <label class="factory-master-label" for="factory-name">공장명 *</label>
              <input id="factory-name" v-model="form.factoryName" class="factory-master-input" maxlength="50" placeholder="예: 창원제1공장" required>
            </div>

            <div class="factory-master-form-grid">
              <div class="factory-master-field">
                <label class="factory-master-label" for="line-name">생산 라인명 *</label>
                <input id="line-name" v-model="form.lineName" class="factory-master-input" maxlength="50" placeholder="예: A라인" required>
              </div>
              <div class="factory-master-field">
                <label class="factory-master-label" for="operation-seq">공정 순서 *</label>
                <input id="operation-seq" v-model.number="form.operationSeq" class="factory-master-input" type="number" min="1" required>
              </div>
            </div>

            <div class="factory-master-field">
              <label class="factory-master-label" for="operation-name">세부 공정명 *</label>
              <input id="operation-name" v-model="form.operationName" class="factory-master-input" maxlength="50" placeholder="예: SMD 표면실장 공정" required>
            </div>

            <div class="factory-master-form-notice">
              동일한 공장/라인/공정 순서 조합은 서버에서 중복 등록이 차단됩니다.
            </div>

            <div class="factory-master-modal-actions">
              <button class="factory-master-secondary-button" type="button" @click="closeForm">취소</button>
              <button class="factory-master-primary-button" type="submit" :disabled="factoryRoutingStore.isSaving">
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
  --factory-color-page: #f8fafc;
  --factory-color-surface: #ffffff;
  --factory-color-surface-muted: #f1f5f9;
  --factory-color-surface-soft: #eff6ff;
  --factory-color-border: #e2e8f0;
  --factory-color-border-strong: #cbd5e1;
  --factory-color-text-primary: #1e293b;
  --factory-color-text-secondary: #475569;
  --factory-color-text-muted: #94a3b8;
  --factory-color-primary: #1428a0;
  --factory-color-primary-hover: #102180;
  --factory-color-primary-soft: #eef2ff;
  --factory-color-primary-border: #c7d2fe;
  --factory-color-primary-hover-soft: #e0e7ff;
  --factory-color-success: #047857;
  --factory-color-success-soft: #ecfdf5;
  --factory-color-success-border: #bbf7d0;
  --factory-color-warning: #b45309;
  --factory-color-warning-soft: #fffbeb;
  --factory-color-warning-border: #fde68a;
  --factory-color-danger: #dc2626;
  --factory-color-danger-soft: #fff1f2;
  --factory-color-danger-border: #fecdd3;
  --factory-color-danger-hover-soft: #ffe4e6;
  --factory-color-table-divider: #f1f5f9;
  --factory-font-weight-title: 700;
  --factory-font-weight-label: 700;
  --factory-font-weight-strong: 800;
  --factory-font-weight-body: 500;
  --factory-font-weight-muted: 600;
  --factory-radius-panel: 0.75rem;
  --factory-shadow-panel: 0 1px 2px rgb(15 23 42 / 0.06);

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
  @apply flex items-center gap-2 text-2xl tracking-tight;
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-title);
}

.factory-master-title-mark {
  @apply h-6 w-1.5 rounded-sm;
  background-color: var(--factory-color-primary);
}

.factory-master-subtitle {
  @apply mt-1.5 text-xs;
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-body);
}

.factory-master-toast {
  @apply flex items-center gap-2 rounded-lg border px-4 py-2 text-xs shadow-sm;
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
  @apply flex items-center justify-between rounded-lg border px-4 py-3 text-xs;
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
  @apply text-[10px] uppercase tracking-wider;
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-stat-value {
  @apply mt-1 text-xl;
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
  @apply flex items-center gap-2 text-xs;
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
  @apply grid grid-cols-1 gap-5 border-t p-5 text-xs sm:grid-cols-2 lg:grid-cols-4;
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
  @apply h-9 w-full rounded-lg border px-3 text-xs outline-none transition disabled:cursor-not-allowed disabled:opacity-60;
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
  @apply inline-flex h-9 items-center justify-center gap-2 rounded-lg px-4 text-xs transition disabled:cursor-not-allowed disabled:opacity-60;
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

.factory-master-tree-body {
  @apply flex gap-5 overflow-x-auto overflow-y-hidden p-5 text-xs;
  scroll-snap-type: x proximity;
}

.factory-master-tree-factory {
  @apply flex min-w-[320px] max-w-[360px] shrink-0 flex-col space-y-3 rounded-xl border p-3;
  background-color: var(--factory-color-surface);
  border-color: var(--factory-color-border);
  scroll-snap-align: start;
}

.factory-master-tree-factory-name {
  @apply flex items-center gap-2 rounded-lg border px-3 py-2;
  background-color: var(--factory-color-primary-soft);
  border-color: var(--factory-color-primary-border);
  color: var(--factory-color-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-tree-lines {
  @apply max-h-[360px] space-y-4 overflow-y-auto overflow-x-hidden pr-2 pl-4;
}

.factory-master-tree-line {
  @apply border-l pl-3;
  border-color: var(--factory-color-border-strong);
}

.factory-master-tree-line-name {
  @apply mb-2 text-[11px];
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-tree-operation {
  @apply mb-1.5 flex w-full items-center gap-2 rounded-lg border px-3 py-2 text-left transition-colors;
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
  @apply flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px];
  background-color: var(--factory-color-primary);
  color: var(--factory-color-surface);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-tree-operation strong {
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-toolbar {
  @apply flex-wrap border-b px-5 py-4;
  background-color: var(--factory-color-surface-muted);
  border-color: var(--factory-color-border);
}

.factory-master-toolbar-copy {
  @apply flex flex-col gap-1 text-xs;
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
  @apply w-full min-w-[980px] border-collapse text-left text-xs;
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
  @apply inline-flex rounded border px-2.5 py-0.5 text-[10px];
  background-color: var(--factory-color-warning-soft);
  border-color: var(--factory-color-warning-border);
  color: var(--factory-color-warning);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-table-button,
.factory-master-table-danger-button {
  @apply inline-flex items-center gap-1 rounded border px-3 py-1.5 text-[10px] transition-colors;
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
  @apply font-mono text-[11px];
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-detail-body {
  @apply min-h-[160px] p-6 text-xs;
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
  @apply rounded-md px-3 py-1.5 text-xs transition-colors;
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
  @apply border-b pb-2 text-xs;
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
  @apply flex h-7 w-7 items-center justify-center rounded-full text-xs;
  background-color: var(--factory-color-primary);
  color: var(--factory-color-surface);
  font-weight: var(--factory-font-weight-strong);
}

.factory-master-flow-name {
  @apply mt-3 text-sm leading-5;
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-flow-code {
  @apply mt-3 font-mono text-[10px];
  color: var(--factory-color-text-muted);
  font-weight: var(--factory-font-weight-muted);
}

.factory-master-flow-arrow {
  @apply text-xl;
  color: var(--factory-color-primary);
  font-weight: var(--factory-font-weight-strong);
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
  @apply text-sm;
  color: var(--factory-color-text-primary);
  font-weight: var(--factory-font-weight-label);
}

.factory-master-form {
  @apply space-y-4 p-6 text-xs;
}

.factory-master-form-grid {
  @apply grid grid-cols-1 gap-4 sm:grid-cols-2;
}

.factory-master-form-notice {
  @apply rounded-lg border px-4 py-3 text-xs;
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
