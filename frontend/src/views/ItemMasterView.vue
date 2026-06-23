<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CheckCircle2, Package, RefreshCw, Search } from '@lucide/vue'
import { useAuthStore } from '@/state/authStore'
import { useItemMasterStore } from '@/state/itemMasterStore'
import type { ItemMasterRequest, ItemMasterResponse, ItemStatus, ItemType, ItemUnit } from '@/api/itemMasterApi'

const router = useRouter()
const authStore = useAuthStore()
const itemMasterStore = useItemMasterStore()

const itemTypeOptions: Array<{ value: ItemType; label: string }> = [
  { value: 'RAW', label: '원자재' },
  { value: 'HALF', label: '반제품' },
  { value: 'FG', label: '완제품' }
]

const unitOptions: ItemUnit[] = ['ea', 'kg', 'box', 'L']
const sortableFields = [
  { field: 'itemCode', label: '품목코드' },
  { field: 'itemName', label: '품목명' },
  { field: 'itemType', label: '분류' },
  { field: 'unit', label: '단위' },
  { field: 'safetyStock', label: '안전재고' },
  { field: 'createdAt', label: '등록일' }
]

const keyword = ref('')
const itemType = ref<'ALL' | ItemType>('ALL')
const itemStatus = ref<'ALL' | ItemStatus>('ALL')
const sortField = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')
const pageError = ref<string | null>(null)
const toast = ref<string | null>(null)
const isCreateOpen = ref(false)
const isDuplicateOpen = ref(false)
const isSuggestOpen = ref(false)
const pendingPayload = ref<ItemMasterRequest | null>(null)
const formError = ref<string | null>(null)
let suggestionTimer: number | undefined

const form = reactive<ItemMasterRequest>({
  itemCode: '',
  itemName: '',
  spec: '',
  unit: 'ea',
  itemType: 'RAW',
  safetyStock: 0
})

const stats = computed(() => {
  const items = itemMasterStore.items
  return {
    total: itemMasterStore.totalElements,
    active: items.filter((item) => item.itemStatus === 'ACTIVE').length,
    inactive: items.filter((item) => item.itemStatus === 'INACTIVE').length
  }
})
const canWrite = computed(() => authStore.canManageMasterData)
const pageStart = computed(() => {
  if (itemMasterStore.totalElements === 0) return 0
  return itemMasterStore.page * itemMasterStore.size + 1
})
const pageEnd = computed(() => Math.min((itemMasterStore.page + 1) * itemMasterStore.size, itemMasterStore.totalElements))

const keywordSuggestions = computed(() => {
  const searchKeyword = keyword.value.trim().toLowerCase()
  if (!searchKeyword) return []

  const unique = new Map<number, ItemMasterResponse>()
  const candidates = [...itemMasterStore.items, ...itemMasterStore.suggestions]

  candidates.forEach((item) => {
    if (unique.has(item.itemId)) return
    if (isKeywordMatched(item, searchKeyword)) unique.set(item.itemId, item)
  })

  return [...unique.values()].slice(0, 8)
})

onMounted(() => {
  fetchItems(0)
})

async function fetchItems(page = itemMasterStore.page) {
  try {
    pageError.value = null
    await itemMasterStore.loadItems({
      page,
      size: 10,
      sort: `${sortField.value},${sortDirection.value}`,
      keyword: keyword.value.trim() || undefined,
      itemType: itemType.value === 'ALL' ? undefined : itemType.value,
      itemStatus: itemStatus.value === 'ALL' ? undefined : itemStatus.value
    })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '품목 목록을 불러오지 못했습니다.'
  }
}

function resetFilters() {
  keyword.value = ''
  itemType.value = 'ALL'
  itemStatus.value = 'ALL'
  sortField.value = 'createdAt'
  sortDirection.value = 'desc'
  fetchItems(0)
}

function changeSort(field: string) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
  fetchItems(0)
}

function goToDetail(item: ItemMasterResponse) {
  router.push({ name: 'item-master-detail', params: { id: item.itemId } })
}

function handleKeywordInput() {
  window.clearTimeout(suggestionTimer)
  if (!keyword.value.trim()) {
    itemMasterStore.suggestions = []
    isSuggestOpen.value = false
    return
  }
  isSuggestOpen.value = true
  suggestionTimer = window.setTimeout(async () => {
    try {
      await itemMasterStore.loadSuggestions(keyword.value)
    } catch (err) {
      itemMasterStore.suggestions = []
    }
    isSuggestOpen.value = keywordSuggestions.value.length > 0
  }, 180)
}

function selectSuggestion(item: ItemMasterResponse) {
  keyword.value = `${item.itemCode} ${item.itemName}`
  isSuggestOpen.value = false
  goToDetail(item)
}

function getSuggestionMeta(item: ItemMasterResponse) {
  return `${item.itemType} / ${item.unit} / 안전재고 ${item.safetyStock.toLocaleString()}`
}

function openSuggestions() {
  isSuggestOpen.value = keywordSuggestions.value.length > 0
}

function isKeywordMatched(item: ItemMasterResponse, searchKeyword: string) {
  return [
    String(item.itemId),
    item.itemCode,
    item.itemName,
    item.spec || '',
    item.itemType,
    getTypeLabel(item.itemType),
    item.unit
  ].some((value) => value.toLowerCase().includes(searchKeyword))
}

function openCreate() {
  if (!canWrite.value) return
  form.itemCode = ''
  form.itemName = ''
  form.spec = ''
  form.unit = 'ea'
  form.itemType = 'RAW'
  form.safetyStock = 0
  formError.value = null
  isCreateOpen.value = true
}

function closeCreate() {
  isCreateOpen.value = false
  formError.value = null
}

async function submitCreate(skipDuplicateCheck = false) {
  const payload = normalizeForm()
  if (!payload) return

  if (!skipDuplicateCheck) {
    const duplicateResult = await itemMasterStore.checkDuplicates({
      itemName: payload.itemName,
      spec: payload.spec,
      unit: payload.unit
    })
    if (duplicateResult.hasDuplicates) {
      pendingPayload.value = payload
      isDuplicateOpen.value = true
      return
    }
  }

  try {
    const created = await itemMasterStore.createItem(payload)
    showToast('품목이 등록되었습니다.')
    closeCreate()
    isDuplicateOpen.value = false
    pendingPayload.value = null
    await router.push({ name: 'item-master-detail', params: { id: created.itemId } })
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '품목 등록에 실패했습니다.'
  }
}

async function submitDuplicateConfirmed() {
  if (!pendingPayload.value) return
  try {
    const created = await itemMasterStore.createItem(pendingPayload.value)
    showToast('중복 경고 확인 후 품목이 등록되었습니다.')
    closeCreate()
    isDuplicateOpen.value = false
    pendingPayload.value = null
    await router.push({ name: 'item-master-detail', params: { id: created.itemId } })
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '품목 등록에 실패했습니다.'
    isDuplicateOpen.value = false
  }
}

function normalizeForm(): ItemMasterRequest | null {
  const itemCode = form.itemCode?.trim() || null
  const itemName = form.itemName.trim()
  const spec = form.spec?.trim() || null

  if (itemCode && !/^[A-Za-z0-9-]+$/.test(itemCode)) {
    formError.value = '품목 코드는 영문, 숫자, 하이픈만 사용할 수 있습니다.'
    return null
  }
  if (!itemName) {
    formError.value = '품목명을 입력해주세요.'
    return null
  }
  if (form.safetyStock < 0) {
    formError.value = '안전재고는 0 이상이어야 합니다.'
    return null
  }

  formError.value = null
  return {
    itemCode,
    itemName,
    spec,
    unit: form.unit,
    itemType: form.itemType,
    safetyStock: Number(form.safetyStock)
  }
}

function getTypeLabel(value: ItemType) {
  return itemTypeOptions.find((option) => option.value === value)?.label || value
}

function getStatusLabel(value: ItemStatus) {
  return value === 'ACTIVE' ? '활성' : '비활성'
}

function formatDate(value: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}

function showToast(message: string) {
  toast.value = message
  window.setTimeout(() => {
    toast.value = null
  }, 2200)
}
</script>

<template>
  <div class="space-y-6 pb-12">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 class="app-type-2xl app-font-emphasis tracking-tight app-text-strong">품목 마스터</h1>
        <p class="mt-1 app-type-sm app-font-label app-text-muted">검색 후 목록에서 품목을 선택하면 상세 화면으로 이동합니다.</p>
      </div>
      <div v-if="toast" class="rounded-xl border app-border app-bg-success-soft px-4 py-2 app-type-sm app-font-strong app-text-success">
        {{ toast }}
      </div>
    </div>

    <div v-if="pageError" class="rounded-2xl border app-border app-bg-danger-soft p-4 app-type-sm app-font-strong app-text-danger">
      {{ pageError }}
    </div>

    <div class="app-news-grid">
      <div class="app-news-card">
        <div>
          <p class="app-news-label">전체 품목</p>
          <strong class="app-news-value app-text-strong">{{ stats.total.toLocaleString() }}</strong>
        </div>
        <div class="app-news-icon">
          <Package />
        </div>
      </div>
      <div class="app-news-card">
        <div>
          <p class="app-news-label app-text-success">활성 상태 품목</p>
          <strong class="app-news-value app-text-success">{{ stats.active.toLocaleString() }}</strong>
        </div>
        <div class="app-news-icon app-bg-success-soft app-text-success">
          <CheckCircle2 />
        </div>
      </div>
      <div class="app-news-card">
        <div>
          <p class="app-news-label app-text-warning">비활성 상태 품목</p>
          <strong class="app-news-value app-text-warning">{{ stats.inactive.toLocaleString() }}</strong>
        </div>
        <div class="app-news-icon app-bg-warning-soft app-text-warning">
          <RefreshCw />
        </div>
      </div>
    </div>

    <section class="app-search-panel">
      <div class="grid grid-cols-1 gap-3 lg:grid-cols-[1.5fr_0.8fr_0.8fr_auto]">
        <div class="relative">
          <Search class="pointer-events-none absolute left-4 top-3.5 z-10 h-4 w-4 app-text-muted" />
          <input
            v-model="keyword"
            class="app-control app-control-lg app-control-search"
            placeholder="품목 ID, 품목코드, 품목명, 규격 검색"
            @input="handleKeywordInput"
            @focus="openSuggestions"
            @keyup.enter="fetchItems(0); isSuggestOpen = false"
          >
          <div v-if="isSuggestOpen && keywordSuggestions.length > 0" class="absolute left-0 right-0 top-[3.1rem] z-30 max-h-80 overflow-y-auto rounded-xl border app-border app-bg-surface py-2 shadow-xl">
            <button
              v-for="suggestion in keywordSuggestions"
              :key="suggestion.itemId"
              class="block w-full px-4 py-2.5 text-left transition app-hover-muted"
              type="button"
              @mousedown.prevent="selectSuggestion(suggestion)"
            >
              <span class="block app-type-sm app-font-emphasis leading-5 app-text-strong">{{ suggestion.itemCode }} · {{ suggestion.itemName }}</span>
              <span class="mt-0.5 block app-type-xs app-font-strong app-text-muted">{{ getSuggestionMeta(suggestion) }}</span>
            </button>
          </div>
        </div>
        <select v-model="itemType" class="app-control app-control-lg">
          <option value="ALL">전체 분류</option>
          <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <select v-model="itemStatus" class="app-control app-control-lg">
          <option value="ALL">전체 상태</option>
          <option value="ACTIVE">활성</option>
          <option value="INACTIVE">비활성</option>
        </select>
        <div class="app-search-actions">
          <button class="app-button app-button-lg app-button-primary" type="button" @click="fetchItems(0)">조회</button>
          <button class="app-button app-button-lg app-button-muted" type="button" @click="resetFilters">초기화</button>
          <button v-if="canWrite" class="app-button app-button-lg app-button-primary" type="button" @click="openCreate">신규 등록</button>
        </div>
      </div>
    </section>

    <section class="overflow-hidden rounded-3xl border app-border app-bg-surface shadow-sm">
      <div class="app-list-head">
        <span class="app-list-title">품목 목록</span>
      </div>
      <div class="overflow-x-auto">
        <table class="app-table min-w-[980px]">
          <thead>
            <tr>
              <th>No</th>
              <th v-for="column in sortableFields" :key="column.field" class="app-sortable-header" @click="changeSort(column.field)">
                {{ column.label }}
                <span v-if="sortField === column.field" class="app-sort-mark">{{ sortDirection === 'asc' ? '▲' : '▼' }}</span>
              </th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="itemMasterStore.isLoading">
              <td class="px-4 py-10 text-center app-font-strong app-text-muted" colspan="8">품목 목록을 불러오는 중입니다.</td>
            </tr>
            <tr v-else-if="itemMasterStore.items.length === 0">
              <td class="px-4 py-10 text-center app-font-strong app-text-muted" colspan="8">조회된 품목이 없습니다.</td>
            </tr>
            <tr
              v-for="(item, index) in itemMasterStore.items"
              v-else
              :key="item.itemId"
              class="app-table-row"
              @click="goToDetail(item)"
            >
              <td class="app-table-id">{{ itemMasterStore.page * itemMasterStore.size + index + 1 }}</td>
              <td class="app-table-main font-mono">{{ item.itemCode }}</td>
              <td class="app-table-main">{{ item.itemName }}</td>
              <td>{{ getTypeLabel(item.itemType) }}</td>
              <td class="app-table-id">{{ item.unit }}</td>
              <td class="app-table-number">{{ item.safetyStock.toLocaleString() }}</td>
              <td class="app-table-id">{{ formatDate(item.createdAt) }}</td>
              <td>
                <span class="app-status" :class="item.itemStatus === 'ACTIVE' ? 'app-status-success' : 'app-status-warning'">
                  {{ getStatusLabel(item.itemStatus) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="app-pagination">
        <p>
          총 {{ itemMasterStore.totalElements.toLocaleString() }}건 · {{ pageStart.toLocaleString() }}-{{ pageEnd.toLocaleString() }} 표시 · {{ itemMasterStore.size }}건씩 · {{ itemMasterStore.page + 1 }} / {{ Math.max(itemMasterStore.totalPages, 1) }} 페이지
        </p>
        <div class="app-pagination-actions">
          <button class="app-page-button" type="button" :disabled="itemMasterStore.page === 0" @click="fetchItems(0)">처음</button>
          <button class="app-page-button" type="button" :disabled="itemMasterStore.page === 0" @click="fetchItems(itemMasterStore.page - 1)">이전</button>
          <button class="app-page-button" type="button" :disabled="itemMasterStore.page >= itemMasterStore.totalPages - 1" @click="fetchItems(itemMasterStore.page + 1)">다음</button>
          <button class="app-page-button" type="button" :disabled="itemMasterStore.page >= itemMasterStore.totalPages - 1" @click="fetchItems(itemMasterStore.totalPages - 1)">마지막</button>
        </div>
      </div>
    </section>

    <div v-if="isCreateOpen" class="fixed inset-0 z-50 flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-2xl rounded-3xl app-bg-surface shadow-2xl">
        <div class="border-b app-border-muted p-6">
          <h2 class="app-type-lg app-font-emphasis app-text-strong">신규 품목 등록</h2>
          <p class="mt-1 app-type-sm app-font-label app-text-muted">품목 코드는 미입력 시 ITEM-____ 형식으로 자동 생성됩니다.</p>
        </div>
        <form class="space-y-4 p-6" @submit.prevent="submitCreate(false)">
          <div v-if="formError" class="rounded-2xl border app-border app-bg-danger-soft p-3 app-type-sm app-font-strong app-text-danger">{{ formError }}</div>
          <label class="block app-type-sm app-font-emphasis app-text-soft">
            품목 코드
            <input v-model="form.itemCode" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 font-mono app-type-sm outline-none" placeholder="미입력 시 자동 생성">
            <span class="mt-1 block app-type-xs app-font-strong app-text-muted">영문, 숫자, 하이픈만 사용할 수 있습니다.</span>
          </label>
          <label class="block app-type-sm app-font-emphasis app-text-soft">
            품목명 *
            <input v-model="form.itemName" required class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none">
          </label>
          <label class="block app-type-sm app-font-emphasis app-text-soft">
            규격
            <input v-model="form.spec" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none">
          </label>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <label class="block app-type-sm app-font-emphasis app-text-soft">
              분류
              <select v-model="form.itemType" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none">
                <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
            <label class="block app-type-sm app-font-emphasis app-text-soft">
              단위
              <select v-model="form.unit" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none">
                <option v-for="unit in unitOptions" :key="unit" :value="unit">{{ unit }}</option>
              </select>
            </label>
            <label class="block app-type-sm app-font-emphasis app-text-soft">
              안전재고
              <input v-model.number="form.safetyStock" min="0" type="number" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none">
            </label>
          </div>
          <div class="flex justify-end gap-2 border-t app-border-muted pt-4">
            <button class="rounded-2xl app-bg-muted px-5 py-2.5 app-type-sm app-font-emphasis" type="button" @click="closeCreate">취소</button>
            <button class="rounded-2xl app-accent-bg px-5 py-2.5 app-type-sm app-font-emphasis app-text-inverse" type="submit" :disabled="itemMasterStore.isSaving">저장</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="isDuplicateOpen" class="fixed inset-0 z-[60] flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-xl rounded-3xl app-bg-surface p-6 shadow-2xl">
        <h2 class="app-type-lg app-font-emphasis app-text-strong">유사 품목이 존재합니다</h2>
        <p class="mt-2 app-type-sm app-font-label app-text-muted">품목명, 규격, 단위가 같은 품목이 있습니다. 등록하시겠습니까?</p>
        <div class="mt-4 max-h-64 overflow-y-auto rounded-2xl border app-border-muted">
          <div v-for="item in itemMasterStore.duplicateCheck?.items" :key="item.itemId" class="border-b app-border-muted p-3 last:border-b-0">
            <p class="font-mono app-type-sm app-font-emphasis app-text-strong">{{ item.itemCode }}</p>
            <p class="app-type-sm app-font-strong app-text-soft">{{ item.itemName }} · {{ item.spec || '규격 없음' }} · {{ item.unit }}</p>
          </div>
        </div>
        <div class="mt-5 flex justify-end gap-2">
          <button class="rounded-2xl app-bg-muted px-5 py-2.5 app-type-sm app-font-emphasis" type="button" @click="isDuplicateOpen = false">취소</button>
          <button class="rounded-2xl app-bg-warning px-5 py-2.5 app-type-sm app-font-emphasis app-text-inverse" type="button" @click="submitDuplicateConfirmed">등록</button>
        </div>
      </div>
    </div>
  </div>
</template>
