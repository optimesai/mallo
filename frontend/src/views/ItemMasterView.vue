<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  AlertTriangle,
  Boxes,
  ChevronDown,
  ClipboardList,
  Edit3,
  FileSpreadsheet,
  Loader2,
  PackageCheck,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X
} from '@lucide/vue'
import { useItemMasterStore } from '@/state/itemMasterStore'
import type { ItemMasterRequest, ItemMasterResponse, ItemType, ItemUnit } from '@/api/itemMasterApi'

const itemMasterStore = useItemMasterStore()

const itemTypeOptions: Array<{ value: ItemType; label: string; description: string }> = [
  { value: 'RAW', label: '원자재', description: '생산 투입 자재' },
  { value: 'HALF', label: '반제품', description: '공정 중간 산출품' },
  { value: 'FG', label: '완제품', description: '출하 대상 품목' }
]

const unitOptions: Array<{ value: ItemUnit; label: string }> = [
  { value: 'ea', label: 'ea' },
  { value: 'kg', label: 'kg' },
  { value: 'box', label: 'box' },
  { value: 'L', label: 'L' }
]

const isSearchExpanded = ref(true)
const filterKeyword = ref('')
const filterItemType = ref<'ALL' | ItemType>('ALL')
const activeDetailTab = ref<'spec' | 'system'>('spec')
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)

const isFormOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formError = ref<string | null>(null)
const editingItemId = ref<number | null>(null)
const form = reactive<ItemMasterRequest>({
  itemName: '',
  spec: '',
  unit: 'ea',
  itemType: 'RAW',
  safetyStock: 0
})

const pendingDeleteItem = ref<ItemMasterResponse | null>(null)
const isForceDeleteOpen = ref(false)
const forceDeleteMessage = ref('')

const selectedItem = computed(() => itemMasterStore.selectedItem)

const stats = computed(() => {
  const items = itemMasterStore.items
  return {
    total: items.length,
    raw: items.filter((item) => item.itemType === 'RAW').length,
    half: items.filter((item) => item.itemType === 'HALF').length,
    fg: items.filter((item) => item.itemType === 'FG').length,
    safetyStockTotal: items.reduce((sum, item) => sum + item.safetyStock, 0)
  }
})

const selectedTypeOption = computed(() => {
  if (!selectedItem.value) return null
  return itemTypeOptions.find((option) => option.value === selectedItem.value?.itemType) || null
})

onMounted(async () => {
  await fetchItems()
})

async function fetchItems() {
  try {
    pageError.value = null
    await itemMasterStore.loadItems({
      itemType: filterItemType.value === 'ALL' ? undefined : filterItemType.value,
      keyword: filterKeyword.value.trim() || undefined
    })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '품목 목록을 불러오지 못했습니다.'
  }
}

function resetFilters() {
  filterKeyword.value = ''
  filterItemType.value = 'ALL'
}

async function handleSearch() {
  itemMasterStore.selectItem(null)
  await fetchItems()
}

function selectRow(item: ItemMasterResponse) {
  if (selectedItem.value?.itemId === item.itemId) {
    itemMasterStore.selectItem(null)
    return
  }
  itemMasterStore.selectItem(item)
}

function openCreateForm() {
  formMode.value = 'create'
  editingItemId.value = null
  resetForm()
  formError.value = null
  isFormOpen.value = true
}

function openEditForm(item: ItemMasterResponse) {
  formMode.value = 'edit'
  editingItemId.value = item.itemId
  form.itemName = item.itemName
  form.spec = item.spec || ''
  form.unit = item.unit
  form.itemType = item.itemType
  form.safetyStock = item.safetyStock
  formError.value = null
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  formError.value = null
}

function resetForm() {
  form.itemName = ''
  form.spec = ''
  form.unit = 'ea'
  form.itemType = 'RAW'
  form.safetyStock = 0
}

async function submitForm() {
  const normalizedName = form.itemName.trim()
  const normalizedSpec = form.spec?.trim() || null

  if (!normalizedName) {
    formError.value = '품목명을 입력해주세요.'
    return
  }
  if (normalizedName.length > 100) {
    formError.value = '품목명은 100자 이하여야 합니다.'
    return
  }
  if (normalizedSpec && normalizedSpec.length > 100) {
    formError.value = '규격은 100자 이하여야 합니다.'
    return
  }
  if (form.safetyStock < 0) {
    formError.value = '안전 재고량은 0 이상이어야 합니다.'
    return
  }

  const payload: ItemMasterRequest = {
    itemName: normalizedName,
    spec: normalizedSpec,
    unit: form.unit,
    itemType: form.itemType,
    safetyStock: Number(form.safetyStock)
  }

  try {
    formError.value = null
    if (formMode.value === 'create') {
      await itemMasterStore.createItem(payload)
      showToast('신규 품목 마스터가 등록되었습니다.')
    } else if (editingItemId.value !== null) {
      await itemMasterStore.updateItem(editingItemId.value, payload)
      showToast('품목 마스터 정보가 수정되었습니다.')
    }
    closeForm()
    await fetchItems()
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '품목 저장에 실패했습니다.'
  }
}

async function requestDelete(item: ItemMasterResponse) {
  if (!confirm(`[${item.itemCode}] ${item.itemName} 품목을 삭제하시겠습니까?`)) return
  pendingDeleteItem.value = item
  await deleteItem(false)
}

async function deleteItem(force: boolean) {
  if (!pendingDeleteItem.value) return

  try {
    pageError.value = null
    await itemMasterStore.deleteItem(pendingDeleteItem.value.itemId, force)
    showToast(force ? '참조 데이터와 함께 품목이 삭제되었습니다.' : '품목이 삭제되었습니다.')
    pendingDeleteItem.value = null
    isForceDeleteOpen.value = false
    await fetchItems()
  } catch (err) {
    const message = err instanceof Error ? err.message : '품목 삭제에 실패했습니다.'
    if (!force && message.includes('force=true')) {
      forceDeleteMessage.value = message
      isForceDeleteOpen.value = true
      return
    }
    pageError.value = message
  }
}

function cancelForceDelete() {
  pendingDeleteItem.value = null
  isForceDeleteOpen.value = false
  forceDeleteMessage.value = ''
}

function getItemTypeLabel(itemType: ItemType) {
  return itemTypeOptions.find((option) => option.value === itemType)?.label || itemType
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
  <div class="item-master-page">
    <div class="item-master-header">
      <div>
        <h1 class="item-master-title">
          <span class="item-master-title-mark"></span>
          품목 마스터 관리
        </h1>
        <p class="item-master-subtitle">원자재, 반제품, 완제품 기준정보를 등록하고 생산/재고/출하 업무의 공통 품목 기준을 관리합니다.</p>
      </div>
      <div v-if="successToast" class="item-master-toast">
        <span class="item-master-toast-dot"></span>
        {{ successToast }}
      </div>
    </div>

    <div v-if="pageError" class="item-master-error">
      <span>{{ pageError }}</span>
      <button class="item-master-icon-button" type="button" @click="pageError = null">
        <X class="item-master-icon-sm" />
      </button>
    </div>

    <div class="item-master-stats-grid">
      <div class="item-master-stat-card">
        <div class="item-master-stat-icon item-master-stat-icon-neutral">
          <ClipboardList class="item-master-icon-md" />
        </div>
        <div>
          <p class="item-master-stat-label">전체 품목</p>
          <p class="item-master-stat-value">{{ stats.total }} 종</p>
        </div>
      </div>
      <div class="item-master-stat-card">
        <div class="item-master-stat-icon item-master-stat-icon-primary">
          <Boxes class="item-master-icon-md" />
        </div>
        <div>
          <p class="item-master-stat-label">원자재 / 반제품</p>
          <p class="item-master-stat-value">{{ stats.raw }} / {{ stats.half }} 종</p>
        </div>
      </div>
      <div class="item-master-stat-card">
        <div class="item-master-stat-icon item-master-stat-icon-success">
          <PackageCheck class="item-master-icon-md" />
        </div>
        <div>
          <p class="item-master-stat-label">완제품 / 안전재고 합계</p>
          <p class="item-master-stat-value">{{ stats.fg }} 종 / {{ stats.safetyStockTotal.toLocaleString() }}</p>
        </div>
      </div>
    </div>

    <section class="item-master-panel">
      <div class="item-master-panel-heading">
        <span class="item-master-panel-title">
          <Search class="item-master-icon-sm" />
          조회 검색 조건
        </span>
        <button class="item-master-icon-button" type="button" @click="isSearchExpanded = !isSearchExpanded">
          <ChevronDown class="item-master-icon-sm item-master-chevron" :class="{ 'item-master-chevron-collapsed': !isSearchExpanded }" />
        </button>
      </div>

      <div v-show="isSearchExpanded" class="item-master-search-grid">
        <div class="item-master-field">
          <label class="item-master-label" for="item-keyword">통합 검색</label>
          <input
            id="item-keyword"
            v-model="filterKeyword"
            class="item-master-input"
            placeholder="품목 ID, 품목 코드, 품목명"
            @keyup.enter="handleSearch"
          >
        </div>
        <div class="item-master-field">
          <label class="item-master-label" for="item-type-filter">품목 분류</label>
          <select id="item-type-filter" v-model="filterItemType" class="item-master-input">
            <option value="ALL">전체 분류</option>
            <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }} ({{ option.value }})
            </option>
          </select>
        </div>
        <div class="item-master-search-actions">
          <button class="item-master-secondary-button" type="button" @click="resetFilters">초기화</button>
          <button class="item-master-primary-button" type="button" @click="handleSearch">조회</button>
        </div>
      </div>
    </section>

    <section class="item-master-panel">
      <div class="item-master-toolbar">
        <div class="item-master-toolbar-copy">
          <span>서버 기준 자동 품목코드 생성</span>
          <span>분류 변경 시 코드가 재생성될 수 있습니다.</span>
        </div>
        <div class="item-master-toolbar-actions">
          <button class="item-master-secondary-button" type="button" @click="fetchItems">
            <RefreshCw class="item-master-icon-sm" />
            새로고침
          </button>
          <button class="item-master-primary-button" type="button" @click="openCreateForm">
            <Plus class="item-master-icon-sm" />
            신규 품목 등록
          </button>
        </div>
      </div>

      <div class="item-master-table-wrap">
        <table class="item-master-table">
          <thead>
            <tr>
              <th class="item-master-cell-center">No</th>
              <th>품목코드</th>
              <th>품목명</th>
              <th class="item-master-cell-center">분류</th>
              <th>규격</th>
              <th class="item-master-cell-center">단위</th>
              <th class="item-master-cell-right">안전재고</th>
              <th class="item-master-cell-center">등록일시</th>
              <th class="item-master-cell-center">액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="itemMasterStore.isLoading">
              <td colspan="9" class="item-master-empty-cell">
                <Loader2 class="item-master-spinner" />
                품목 데이터를 가져오고 있습니다...
              </td>
            </tr>
            <tr v-else-if="itemMasterStore.items.length === 0">
              <td colspan="9" class="item-master-empty-cell">조회된 품목 마스터가 없습니다.</td>
            </tr>
            <tr
              v-for="(item, index) in itemMasterStore.items"
              v-else
              :key="item.itemId"
              class="item-master-table-row"
              :class="{ 'item-master-table-row-selected': selectedItem?.itemId === item.itemId }"
              @click="selectRow(item)"
            >
              <td class="item-master-cell-center">{{ index + 1 }}</td>
              <td class="item-master-code">{{ item.itemCode }}</td>
              <td class="item-master-name">{{ item.itemName }}</td>
              <td class="item-master-cell-center">
                <span class="item-master-type-badge" :data-type="item.itemType">
                  {{ getItemTypeLabel(item.itemType) }}
                </span>
              </td>
              <td class="item-master-spec">{{ item.spec || '미지정' }}</td>
              <td class="item-master-cell-center item-master-code">{{ item.unit }}</td>
              <td class="item-master-cell-right item-master-stock">{{ item.safetyStock.toLocaleString() }}</td>
              <td class="item-master-cell-center item-master-date">{{ formatDateTime(item.createdAt) }}</td>
              <td class="item-master-cell-center" @click.stop>
                <div class="item-master-row-actions">
                  <button class="item-master-table-button" type="button" @click="openEditForm(item)">
                    <Edit3 class="item-master-icon-xs" />
                    수정
                  </button>
                  <button class="item-master-table-danger-button" type="button" @click="requestDelete(item)">
                    <Trash2 class="item-master-icon-xs" />
                    삭제
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="4">현재 조회 목록: {{ itemMasterStore.items.length }}건</td>
              <td colspan="5" class="item-master-cell-right">안전재고 합계 {{ stats.safetyStockTotal.toLocaleString() }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>

    <section class="item-master-panel">
      <div class="item-master-detail-heading">
        <div class="item-master-detail-title">
          <FileSpreadsheet class="item-master-icon-sm" />
          선택 품목 세부 정보
        </div>
        <div v-if="selectedItem" class="item-master-detail-heading-actions">
          <div class="item-master-detail-code">ITEM-ID {{ selectedItem.itemId }}</div>
          <button class="item-master-table-button" type="button" @click="openEditForm(selectedItem)">
            <Edit3 class="item-master-icon-xs" />
            수정
          </button>
          <button class="item-master-table-danger-button" type="button" @click="requestDelete(selectedItem)">
            <Trash2 class="item-master-icon-xs" />
            삭제
          </button>
        </div>
      </div>

      <div class="item-master-detail-body">
        <div v-if="!selectedItem" class="item-master-empty-detail">
          <FileSpreadsheet class="item-master-empty-icon" />
          <span>상단 품목 목록에서 행을 선택하면 기준정보 상세 속성이 표시됩니다.</span>
        </div>
        <template v-else>
          <div class="item-master-detail-tabs">
            <button
              type="button"
              class="item-master-tab-button"
              :class="{ 'item-master-tab-button-active': activeDetailTab === 'spec' }"
              @click="activeDetailTab = 'spec'"
            >
              품목 스펙
            </button>
            <button
              type="button"
              class="item-master-tab-button"
              :class="{ 'item-master-tab-button-active': activeDetailTab === 'system' }"
              @click="activeDetailTab = 'system'"
            >
              시스템 정보
            </button>
          </div>

          <div v-if="activeDetailTab === 'spec'" class="item-master-detail-grid">
            <div class="item-master-detail-section">
              <h3>기본 식별 정보</h3>
              <dl class="item-master-description-list">
                <dt>품목 코드</dt>
                <dd class="item-master-code">{{ selectedItem.itemCode }}</dd>
                <dt>품목명</dt>
                <dd>{{ selectedItem.itemName }}</dd>
                <dt>규격</dt>
                <dd>{{ selectedItem.spec || '미지정' }}</dd>
              </dl>
            </div>
            <div class="item-master-detail-section">
              <h3>운영 기준</h3>
              <dl class="item-master-description-list">
                <dt>분류</dt>
                <dd>{{ selectedTypeOption?.label }} ({{ selectedItem.itemType }})</dd>
                <dt>설명</dt>
                <dd>{{ selectedTypeOption?.description }}</dd>
                <dt>단위 / 안전재고</dt>
                <dd>{{ selectedItem.unit }} / {{ selectedItem.safetyStock.toLocaleString() }}</dd>
              </dl>
            </div>
          </div>

          <div v-else class="item-master-detail-grid">
            <div class="item-master-detail-section">
              <h3>등록 이력</h3>
              <dl class="item-master-description-list">
                <dt>등록일시</dt>
                <dd>{{ formatDateTime(selectedItem.createdAt) }}</dd>
                <dt>품목 ID</dt>
                <dd>{{ selectedItem.itemId }}</dd>
                <dt>코드 생성 정책</dt>
                <dd>품목 분류 기준 서버 자동 채번</dd>
              </dl>
            </div>
            <div class="item-master-detail-section item-master-warning-box">
              <h3>삭제 영향 범위</h3>
              <p>BOM, 현재고, 입고, 수불 이력, 작업 지시, 출하 지시에서 참조 중인 품목은 기본 삭제가 차단됩니다.</p>
            </div>
          </div>
        </template>
      </div>
    </section>

    <div v-if="isFormOpen" class="item-master-modal" role="dialog" aria-modal="true">
      <div class="item-master-modal-backdrop" @click="closeForm"></div>
      <div class="item-master-modal-shell">
        <div class="item-master-modal-card">
          <div class="item-master-modal-heading">
            <h2>{{ formMode === 'create' ? '신규 품목 마스터 등록' : '품목 마스터 수정' }}</h2>
            <button class="item-master-icon-button" type="button" @click="closeForm">
              <X class="item-master-icon-sm" />
            </button>
          </div>

          <form class="item-master-form" @submit.prevent="submitForm">
            <div v-if="formError" class="item-master-form-error">{{ formError }}</div>

            <div v-if="formMode === 'edit'" class="item-master-form-notice">
              품목 분류를 변경하면 서버에서 새 분류 기준 품목코드를 다시 생성합니다.
            </div>

            <div class="item-master-field">
              <label class="item-master-label" for="item-name">품목명 *</label>
              <input id="item-name" v-model="form.itemName" class="item-master-input" maxlength="100" required>
            </div>

            <div class="item-master-field">
              <label class="item-master-label" for="item-spec">규격</label>
              <input id="item-spec" v-model="form.spec" class="item-master-input" maxlength="100" placeholder="예: 2.0T * 1219 * 2438">
            </div>

            <div class="item-master-form-grid">
              <div class="item-master-field">
                <label class="item-master-label" for="item-type">품목 분류 *</label>
                <select id="item-type" v-model="form.itemType" class="item-master-input" required>
                  <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">
                    {{ option.label }} ({{ option.value }})
                  </option>
                </select>
              </div>

              <div class="item-master-field">
                <label class="item-master-label" for="item-unit">기본 단위 *</label>
                <select id="item-unit" v-model="form.unit" class="item-master-input" required>
                  <option v-for="option in unitOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>

            <div class="item-master-field">
              <label class="item-master-label" for="item-safety-stock">안전 재고량 *</label>
              <input
                id="item-safety-stock"
                v-model.number="form.safetyStock"
                class="item-master-input"
                type="number"
                min="0"
                required
              >
            </div>

            <div class="item-master-modal-actions">
              <button class="item-master-secondary-button" type="button" @click="closeForm">취소</button>
              <button class="item-master-primary-button" type="submit" :disabled="itemMasterStore.isSaving">
                <Loader2 v-if="itemMasterStore.isSaving" class="item-master-spinner-inline" />
                {{ formMode === 'create' ? '품목 등록' : '수정 저장' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <div v-if="isForceDeleteOpen" class="item-master-modal" role="dialog" aria-modal="true">
      <div class="item-master-modal-backdrop"></div>
      <div class="item-master-modal-shell">
        <div class="item-master-modal-card item-master-danger-modal-card">
          <div class="item-master-danger-heading">
            <AlertTriangle class="item-master-danger-icon" />
            <div>
              <h2>참조 데이터가 있는 품목입니다</h2>
              <p>{{ forceDeleteMessage }}</p>
            </div>
          </div>
          <div class="item-master-danger-body">
            <p v-if="pendingDeleteItem">
              [{{ pendingDeleteItem.itemCode }}] {{ pendingDeleteItem.itemName }} 품목과 연결된 BOM, 재고, 입고, 수불 이력, 작업 지시, 출하 지시 데이터가 함께 삭제될 수 있습니다.
            </p>
          </div>
          <div class="item-master-modal-actions">
            <button class="item-master-secondary-button" type="button" @click="cancelForceDelete">취소</button>
            <button class="item-master-danger-button" type="button" :disabled="itemMasterStore.isSaving" @click="deleteItem(true)">
              <Loader2 v-if="itemMasterStore.isSaving" class="item-master-spinner-inline" />
              참조 데이터 포함 삭제
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "../main.css";

.item-master-page {
  --item-color-page: #f8fafc;
  --item-color-surface: #ffffff;
  --item-color-surface-muted: #f1f5f9;
  --item-color-surface-soft: #eff6ff;
  --item-color-border: #e2e8f0;
  --item-color-border-strong: #cbd5e1;
  --item-color-text-primary: #1e293b;
  --item-color-text-secondary: #475569;
  --item-color-text-muted: #94a3b8;
  --item-color-primary: #1428a0;
  --item-color-primary-hover: #102180;
  --item-color-primary-soft: #eef2ff;
  --item-color-primary-border: #c7d2fe;
  --item-color-primary-hover-soft: #e0e7ff;
  --item-color-success: #047857;
  --item-color-success-soft: #ecfdf5;
  --item-color-success-border: #bbf7d0;
  --item-color-warning: #b45309;
  --item-color-warning-soft: #fffbeb;
  --item-color-warning-border: #fde68a;
  --item-color-danger: #dc2626;
  --item-color-danger-hover: #b91c1c;
  --item-color-danger-soft: #fff1f2;
  --item-color-danger-border: #fecdd3;
  --item-color-danger-hover-soft: #ffe4e6;
  --item-color-table-divider: #f1f5f9;
  --item-font-weight-title: 700;
  --item-font-weight-label: 700;
  --item-font-weight-strong: 800;
  --item-font-weight-body: 500;
  --item-font-weight-muted: 600;
  --item-radius-panel: 0.75rem;
  --item-radius-control: 0.5rem;
  --item-shadow-panel: 0 1px 2px rgb(15 23 42 / 0.06);

  @apply space-y-6 pb-12;
  background: var(--item-color-page);
}

.item-master-header,
.item-master-toolbar,
.item-master-detail-heading,
.item-master-panel-heading,
.item-master-modal-heading,
.item-master-modal-actions,
.item-master-danger-heading {
  @apply flex items-center justify-between gap-4;
}

.item-master-title {
  @apply flex items-center gap-2 text-2xl tracking-tight;
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-title);
}

.item-master-title-mark {
  @apply h-6 w-1.5 rounded-sm;
  background-color: var(--item-color-primary);
}

.item-master-subtitle {
  @apply mt-1.5 text-xs;
  color: var(--item-color-text-muted);
  font-weight: var(--item-font-weight-body);
}

.item-master-toast {
  @apply flex items-center gap-2 rounded-lg border px-4 py-2 text-xs shadow-sm;
  background-color: var(--item-color-success-soft);
  border-color: var(--item-color-success-border);
  color: var(--item-color-success);
  font-weight: var(--item-font-weight-label);
  animation: itemMasterFadeIn 0.2s ease-out forwards;
}

.item-master-toast-dot {
  @apply h-2 w-2 rounded-full;
  background-color: var(--item-color-success);
}

.item-master-error,
.item-master-form-error {
  @apply flex items-center justify-between rounded-lg border px-4 py-3 text-xs;
  background-color: var(--item-color-danger-soft);
  border-color: var(--item-color-danger-border);
  color: var(--item-color-danger);
  font-weight: var(--item-font-weight-label);
}

.item-master-stats-grid {
  @apply grid grid-cols-1 gap-5 sm:grid-cols-3;
}

.item-master-stat-card {
  @apply flex items-center gap-4 rounded-xl border p-6 transition-shadow hover:shadow-md;
  background-color: var(--item-color-surface);
  border-color: var(--item-color-border);
  box-shadow: var(--item-shadow-panel);
}

.item-master-stat-icon {
  @apply rounded-lg p-3;
}

.item-master-stat-icon-neutral {
  background-color: var(--item-color-surface-muted);
  color: var(--item-color-text-secondary);
}

.item-master-stat-icon-primary {
  background-color: var(--item-color-primary-soft);
  color: var(--item-color-primary);
}

.item-master-stat-icon-success {
  background-color: var(--item-color-success-soft);
  color: var(--item-color-success);
}

.item-master-stat-label {
  @apply text-[10px] uppercase tracking-wider;
  color: var(--item-color-text-muted);
  font-weight: var(--item-font-weight-label);
}

.item-master-stat-value {
  @apply mt-1 text-xl;
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-strong);
}

.item-master-panel {
  @apply overflow-hidden rounded-xl border;
  background-color: var(--item-color-surface);
  border-color: var(--item-color-border);
  box-shadow: var(--item-shadow-panel);
}

.item-master-panel-heading,
.item-master-detail-heading {
  @apply border-b px-5 py-3.5;
  background-color: var(--item-color-surface-muted);
  border-color: var(--item-color-border);
}

.item-master-panel-title,
.item-master-detail-title {
  @apply flex items-center gap-2 text-xs;
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-label);
}

.item-master-icon-button {
  @apply rounded p-1 transition-colors;
  color: var(--item-color-text-muted);
}

.item-master-icon-button:hover {
  background-color: var(--item-color-border);
  color: var(--item-color-text-secondary);
}

.item-master-search-grid {
  @apply grid grid-cols-1 gap-5 border-t p-5 text-xs sm:grid-cols-2 lg:grid-cols-4;
  border-color: var(--item-color-border);
}

.item-master-field {
  @apply space-y-1.5;
}

.item-master-label {
  @apply block;
  color: var(--item-color-text-secondary);
  font-weight: var(--item-font-weight-label);
}

.item-master-input {
  @apply h-9 w-full rounded-lg border px-3 text-xs outline-none transition;
  background-color: var(--item-color-surface);
  border-color: var(--item-color-border-strong);
  color: var(--item-color-text-primary);
}

.item-master-input:focus {
  border-color: var(--item-color-primary);
  box-shadow: 0 0 0 1px var(--item-color-primary);
}

.item-master-search-actions {
  @apply flex items-end justify-end gap-2 sm:col-span-2 lg:col-span-2;
}

.item-master-primary-button,
.item-master-secondary-button,
.item-master-danger-button {
  @apply inline-flex h-9 items-center justify-center gap-2 rounded-lg px-4 text-xs transition disabled:cursor-not-allowed disabled:opacity-60;
  font-weight: var(--item-font-weight-label);
}

.item-master-primary-button {
  background-color: var(--item-color-primary);
  color: var(--item-color-surface);
}

.item-master-primary-button:hover {
  background-color: var(--item-color-primary-hover);
}

.item-master-secondary-button {
  background-color: var(--item-color-surface-muted);
  color: var(--item-color-text-secondary);
}

.item-master-secondary-button:hover {
  background-color: var(--item-color-border);
}

.item-master-danger-button {
  background-color: var(--item-color-danger);
  color: var(--item-color-surface);
}

.item-master-danger-button:hover {
  background-color: var(--item-color-danger-hover);
}

.item-master-toolbar {
  @apply flex-wrap border-b px-5 py-4;
  background-color: var(--item-color-surface-muted);
  border-color: var(--item-color-border);
}

.item-master-toolbar-copy {
  @apply flex flex-col gap-1 text-xs;
  color: var(--item-color-text-muted);
  font-weight: var(--item-font-weight-muted);
}

.item-master-toolbar-actions,
.item-master-row-actions,
.item-master-detail-heading-actions {
  @apply flex items-center gap-2;
}

.item-master-table-wrap {
  @apply overflow-x-auto;
}

.item-master-table {
  @apply w-full min-w-[1180px] border-collapse text-left text-xs;
  color: var(--item-color-text-secondary);
}

.item-master-table th {
  @apply whitespace-nowrap border-b border-r px-4 py-3 uppercase last:border-r-0;
  background-color: var(--item-color-surface-muted);
  border-color: var(--item-color-border);
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-label);
}

.item-master-table td {
  @apply whitespace-nowrap border-r px-4 py-3 last:border-r-0;
  border-color: var(--item-color-table-divider);
}

.item-master-table tbody tr {
  @apply border-b;
  border-color: var(--item-color-border);
}

.item-master-table-row {
  @apply cursor-pointer transition-colors;
}

.item-master-table-row:hover,
.item-master-table-row-selected {
  background-color: var(--item-color-surface-soft);
}

.item-master-cell-center {
  @apply text-center;
}

.item-master-cell-right {
  @apply text-right;
}

.item-master-code,
.item-master-date {
  @apply font-mono;
}

.item-master-code,
.item-master-name,
.item-master-stock {
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-label);
}

.item-master-spec,
.item-master-date {
  color: var(--item-color-text-muted);
}

.item-master-type-badge {
  @apply inline-flex rounded border px-2.5 py-0.5 text-[10px];
  font-weight: var(--item-font-weight-strong);
}

.item-master-type-badge[data-type='RAW'] {
  background-color: var(--item-color-warning-soft);
  border-color: var(--item-color-warning-border);
  color: var(--item-color-warning);
}

.item-master-type-badge[data-type='HALF'] {
  background-color: var(--item-color-primary-soft);
  border-color: var(--item-color-primary-border);
  color: var(--item-color-primary);
}

.item-master-type-badge[data-type='FG'] {
  background-color: var(--item-color-success-soft);
  border-color: var(--item-color-success-border);
  color: var(--item-color-success);
}

.item-master-table-button,
.item-master-table-danger-button {
  @apply inline-flex items-center gap-1 rounded border px-3 py-1.5 text-[10px] transition-colors;
  font-weight: var(--item-font-weight-label);
}

.item-master-table-button {
  background-color: var(--item-color-primary-soft);
  border-color: var(--item-color-primary-border);
  color: var(--item-color-primary);
}

.item-master-table-button:hover {
  background-color: var(--item-color-primary-hover-soft);
}

.item-master-table-danger-button {
  background-color: var(--item-color-danger-soft);
  border-color: var(--item-color-danger-border);
  color: var(--item-color-danger);
}

.item-master-table-danger-button:hover {
  background-color: var(--item-color-danger-hover-soft);
}

.item-master-empty-cell {
  @apply py-12 text-center;
  color: var(--item-color-text-muted);
}

.item-master-table tfoot {
  background-color: var(--item-color-surface-soft);
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-label);
}

.item-master-detail-code {
  @apply font-mono text-[11px];
  color: var(--item-color-text-muted);
  font-weight: var(--item-font-weight-label);
}

.item-master-detail-body {
  @apply min-h-[160px] p-6 text-xs;
}

.item-master-empty-detail {
  @apply flex flex-col items-center justify-center gap-2 py-8 text-center;
  color: var(--item-color-text-muted);
  font-weight: var(--item-font-weight-body);
}

.item-master-empty-icon {
  @apply h-8 w-8;
  color: var(--item-color-border-strong);
}

.item-master-detail-tabs {
  @apply mb-5 flex gap-1.5;
}

.item-master-tab-button {
  @apply rounded-md px-3 py-1.5 text-xs transition-colors;
  color: var(--item-color-text-muted);
  font-weight: var(--item-font-weight-label);
}

.item-master-tab-button:hover {
  background-color: var(--item-color-surface-muted);
  color: var(--item-color-text-primary);
}

.item-master-tab-button-active {
  background-color: var(--item-color-primary);
  color: var(--item-color-surface);
}

.item-master-detail-grid {
  @apply grid grid-cols-1 gap-8 md:grid-cols-2;
}

.item-master-detail-section {
  @apply space-y-3.5;
}

.item-master-detail-section h3 {
  @apply border-b pb-2 text-xs;
  border-color: var(--item-color-border);
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-label);
}

.item-master-description-list {
  @apply grid grid-cols-3 gap-x-2 gap-y-2.5;
}

.item-master-description-list dt {
  color: var(--item-color-text-muted);
  font-weight: var(--item-font-weight-body);
}

.item-master-description-list dd {
  @apply col-span-2;
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-muted);
}

.item-master-warning-box {
  @apply rounded-lg border p-4;
  background-color: var(--item-color-warning-soft);
  border-color: var(--item-color-warning-border);
  color: var(--item-color-warning);
}

.item-master-warning-box p {
  @apply leading-5;
  font-weight: var(--item-font-weight-body);
}

.item-master-modal {
  @apply fixed inset-0 z-50 overflow-y-auto;
}

.item-master-modal-backdrop {
  @apply fixed inset-0 backdrop-blur-sm;
  background-color: rgb(15 23 42 / 0.6);
}

.item-master-modal-shell {
  @apply flex min-h-screen items-center justify-center p-4;
}

.item-master-modal-card {
  @apply relative w-full max-w-lg overflow-hidden rounded-2xl border shadow-xl;
  background-color: var(--item-color-surface);
  border-color: var(--item-color-border);
}

.item-master-modal-heading {
  @apply border-b px-6 py-4;
  background-color: var(--item-color-surface-muted);
  border-color: var(--item-color-border);
}

.item-master-modal-heading h2,
.item-master-danger-heading h2 {
  @apply text-sm;
  color: var(--item-color-text-primary);
  font-weight: var(--item-font-weight-label);
}

.item-master-form {
  @apply space-y-4 p-6 text-xs;
}

.item-master-form-grid {
  @apply grid grid-cols-1 gap-4 sm:grid-cols-2;
}

.item-master-form-notice {
  @apply rounded-lg border px-4 py-3 text-xs;
  background-color: var(--item-color-primary-soft);
  border-color: var(--item-color-primary-border);
  color: var(--item-color-primary);
  font-weight: var(--item-font-weight-muted);
}

.item-master-modal-actions {
  @apply border-t pt-4;
  border-color: var(--item-color-border);
}

.item-master-danger-modal-card {
  @apply max-w-xl;
}

.item-master-danger-heading {
  @apply items-start border-b p-6;
  background-color: var(--item-color-danger-soft);
  border-color: var(--item-color-danger-border);
}

.item-master-danger-heading p {
  @apply mt-1 text-xs leading-5;
  color: var(--item-color-danger);
  font-weight: var(--item-font-weight-muted);
}

.item-master-danger-icon {
  @apply h-6 w-6 shrink-0;
  color: var(--item-color-danger);
}

.item-master-danger-body {
  @apply p-6 text-xs leading-5;
  color: var(--item-color-text-secondary);
  font-weight: var(--item-font-weight-body);
}

.item-master-icon-xs {
  @apply h-3 w-3;
}

.item-master-icon-sm {
  @apply h-4 w-4;
}

.item-master-icon-md {
  @apply h-6 w-6;
}

.item-master-chevron {
  @apply transition-transform duration-200;
}

.item-master-chevron-collapsed {
  @apply rotate-180;
}

.item-master-spinner,
.item-master-spinner-inline {
  @apply animate-spin;
  color: var(--item-color-primary);
}

.item-master-spinner {
  @apply mr-2 inline h-5 w-5;
}

.item-master-spinner-inline {
  @apply h-3.5 w-3.5;
  color: currentColor;
}

@keyframes itemMasterFadeIn {
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
