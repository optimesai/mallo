<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@lucide/vue'
import { useItemMasterStore } from '@/state/itemMasterStore'
import type { ItemMasterRequest, ItemMasterResponse, ItemStatus, ItemType, ItemUnit } from '@/api/itemMasterApi'

const router = useRouter()
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
        <h1 class="text-2xl font-black tracking-tight text-slate-950">품목 마스터</h1>
        <p class="mt-1 text-sm font-medium text-slate-500">검색 후 목록에서 품목을 선택하면 상세 화면으로 이동합니다.</p>
      </div>
      <div v-if="toast" class="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-bold text-emerald-700">
        {{ toast }}
      </div>
    </div>

    <div v-if="pageError" class="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-bold text-rose-700">
      {{ pageError }}
    </div>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <div class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
        <p class="text-xs font-black uppercase tracking-widest text-slate-400">전체 품목</p>
        <strong class="mt-2 block text-3xl text-slate-950">{{ stats.total.toLocaleString() }}</strong>
      </div>
      <div class="rounded-3xl border border-emerald-200 bg-emerald-50 p-5 shadow-sm">
        <p class="text-xs font-black uppercase tracking-widest text-emerald-600">활성 상태 품목</p>
        <strong class="mt-2 block text-3xl text-emerald-800">{{ stats.active.toLocaleString() }}</strong>
      </div>
      <div class="rounded-3xl border border-amber-200 bg-amber-50 p-5 shadow-sm">
        <p class="text-xs font-black uppercase tracking-widest text-amber-600">비활성 상태 품목</p>
        <strong class="mt-2 block text-3xl text-amber-800">{{ stats.inactive.toLocaleString() }}</strong>
      </div>
    </div>

    <section class="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div class="grid grid-cols-1 gap-3 lg:grid-cols-[1.5fr_0.8fr_0.8fr_auto]">
        <div class="relative">
          <Search class="pointer-events-none absolute left-4 top-3.5 z-10 h-4 w-4 text-slate-400" />
          <input
            v-model="keyword"
            class="h-11 w-full rounded-xl border border-slate-200 bg-white pl-11 pr-4 text-sm font-semibold outline-none transition focus:border-blue-700 focus:ring-2 focus:ring-blue-100"
            placeholder="품목 ID, 품목코드, 품목명, 규격 검색"
            @input="handleKeywordInput"
            @focus="openSuggestions"
            @keyup.enter="fetchItems(0); isSuggestOpen = false"
          >
          <div v-if="isSuggestOpen && keywordSuggestions.length > 0" class="absolute left-0 right-0 top-[3.1rem] z-30 max-h-80 overflow-y-auto rounded-xl border border-slate-200 bg-white py-2 shadow-xl">
            <button
              v-for="suggestion in keywordSuggestions"
              :key="suggestion.itemId"
              class="block w-full px-4 py-2.5 text-left transition hover:bg-slate-50"
              type="button"
              @mousedown.prevent="selectSuggestion(suggestion)"
            >
              <span class="block text-sm font-black leading-5 text-slate-950">{{ suggestion.itemCode }} · {{ suggestion.itemName }}</span>
              <span class="mt-0.5 block text-xs font-bold text-slate-400">{{ getSuggestionMeta(suggestion) }}</span>
            </button>
          </div>
        </div>
        <select v-model="itemType" class="h-11 rounded-2xl border border-slate-200 px-4 text-sm font-semibold outline-none">
          <option value="ALL">전체 분류</option>
          <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <select v-model="itemStatus" class="h-11 rounded-2xl border border-slate-200 px-4 text-sm font-semibold outline-none">
          <option value="ALL">전체 상태</option>
          <option value="ACTIVE">활성</option>
          <option value="INACTIVE">비활성</option>
        </select>
        <div class="flex gap-2">
          <button class="h-11 rounded-2xl bg-slate-950 px-5 text-sm font-black text-white" type="button" @click="fetchItems(0)">조회</button>
          <button class="h-11 rounded-2xl bg-slate-100 px-5 text-sm font-black text-slate-700" type="button" @click="resetFilters">초기화</button>
          <button class="h-11 rounded-2xl bg-blue-600 px-5 text-sm font-black text-white" type="button" @click="openCreate">신규 등록</button>
        </div>
      </div>
    </section>

    <section class="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full min-w-[980px] text-left text-sm">
          <thead class="bg-slate-50 text-xs uppercase tracking-widest text-slate-500">
            <tr>
              <th class="px-4 py-3">No</th>
              <th v-for="column in sortableFields" :key="column.field" class="cursor-pointer px-4 py-3" @click="changeSort(column.field)">
                {{ column.label }}
                <span v-if="sortField === column.field">{{ sortDirection === 'asc' ? '▲' : '▼' }}</span>
              </th>
              <th class="px-4 py-3">상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="itemMasterStore.isLoading">
              <td class="px-4 py-10 text-center font-bold text-slate-400" colspan="8">품목 목록을 불러오는 중입니다.</td>
            </tr>
            <tr v-else-if="itemMasterStore.items.length === 0">
              <td class="px-4 py-10 text-center font-bold text-slate-400" colspan="8">조회된 품목이 없습니다.</td>
            </tr>
            <tr
              v-for="(item, index) in itemMasterStore.items"
              v-else
              :key="item.itemId"
              class="cursor-pointer border-t border-slate-100 transition hover:bg-slate-50"
              @click="goToDetail(item)"
            >
              <td class="px-4 py-3 font-mono text-xs text-slate-400">{{ itemMasterStore.page * itemMasterStore.size + index + 1 }}</td>
              <td class="px-4 py-3 font-mono font-black text-slate-950">{{ item.itemCode }}</td>
              <td class="px-4 py-3 font-bold text-slate-800">{{ item.itemName }}</td>
              <td class="px-4 py-3">{{ getTypeLabel(item.itemType) }}</td>
              <td class="px-4 py-3 font-mono text-xs text-slate-500">{{ item.unit }}</td>
              <td class="px-4 py-3 text-right font-bold">{{ item.safetyStock.toLocaleString() }}</td>
              <td class="px-4 py-3 text-xs text-slate-500">{{ formatDate(item.createdAt) }}</td>
              <td class="px-4 py-3">
                <span class="rounded-full px-2.5 py-1 text-xs font-black" :class="item.itemStatus === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'">
                  {{ getStatusLabel(item.itemStatus) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="flex flex-col gap-3 border-t border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-sm font-bold text-slate-500">
          총 {{ itemMasterStore.totalElements.toLocaleString() }}건 · {{ itemMasterStore.page + 1 }} / {{ Math.max(itemMasterStore.totalPages, 1) }} 페이지
        </p>
        <div class="flex gap-2">
          <button class="rounded-xl bg-slate-100 px-4 py-2 text-sm font-black disabled:opacity-40" type="button" :disabled="itemMasterStore.page === 0" @click="fetchItems(0)">처음</button>
          <button class="rounded-xl bg-slate-100 px-4 py-2 text-sm font-black disabled:opacity-40" type="button" :disabled="itemMasterStore.page === 0" @click="fetchItems(itemMasterStore.page - 1)">이전</button>
          <button class="rounded-xl bg-slate-100 px-4 py-2 text-sm font-black disabled:opacity-40" type="button" :disabled="itemMasterStore.page >= itemMasterStore.totalPages - 1" @click="fetchItems(itemMasterStore.page + 1)">다음</button>
        </div>
      </div>
    </section>

    <div v-if="isCreateOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4">
      <div class="w-full max-w-2xl rounded-3xl bg-white shadow-2xl">
        <div class="border-b border-slate-100 p-6">
          <h2 class="text-lg font-black text-slate-950">신규 품목 등록</h2>
          <p class="mt-1 text-sm font-semibold text-slate-500">품목 코드는 미입력 시 ITEM-____ 형식으로 자동 생성됩니다.</p>
        </div>
        <form class="space-y-4 p-6" @submit.prevent="submitCreate(false)">
          <div v-if="formError" class="rounded-2xl border border-rose-200 bg-rose-50 p-3 text-sm font-bold text-rose-700">{{ formError }}</div>
          <label class="block text-sm font-black text-slate-700">
            품목 코드
            <input v-model="form.itemCode" class="mt-1 h-11 w-full rounded-2xl border border-slate-200 px-4 font-mono text-sm outline-none" placeholder="미입력 시 자동 생성">
            <span class="mt-1 block text-xs font-bold text-slate-400">영문, 숫자, 하이픈만 사용할 수 있습니다.</span>
          </label>
          <label class="block text-sm font-black text-slate-700">
            품목명 *
            <input v-model="form.itemName" required class="mt-1 h-11 w-full rounded-2xl border border-slate-200 px-4 text-sm outline-none">
          </label>
          <label class="block text-sm font-black text-slate-700">
            규격
            <input v-model="form.spec" class="mt-1 h-11 w-full rounded-2xl border border-slate-200 px-4 text-sm outline-none">
          </label>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <label class="block text-sm font-black text-slate-700">
              분류
              <select v-model="form.itemType" class="mt-1 h-11 w-full rounded-2xl border border-slate-200 px-4 text-sm outline-none">
                <option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
            </label>
            <label class="block text-sm font-black text-slate-700">
              단위
              <select v-model="form.unit" class="mt-1 h-11 w-full rounded-2xl border border-slate-200 px-4 text-sm outline-none">
                <option v-for="unit in unitOptions" :key="unit" :value="unit">{{ unit }}</option>
              </select>
            </label>
            <label class="block text-sm font-black text-slate-700">
              안전재고
              <input v-model.number="form.safetyStock" min="0" type="number" class="mt-1 h-11 w-full rounded-2xl border border-slate-200 px-4 text-sm outline-none">
            </label>
          </div>
          <div class="flex justify-end gap-2 border-t border-slate-100 pt-4">
            <button class="rounded-2xl bg-slate-100 px-5 py-2.5 text-sm font-black" type="button" @click="closeCreate">취소</button>
            <button class="rounded-2xl bg-blue-600 px-5 py-2.5 text-sm font-black text-white" type="submit" :disabled="itemMasterStore.isSaving">저장</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="isDuplicateOpen" class="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/70 p-4">
      <div class="w-full max-w-xl rounded-3xl bg-white p-6 shadow-2xl">
        <h2 class="text-lg font-black text-slate-950">유사 품목이 존재합니다</h2>
        <p class="mt-2 text-sm font-semibold text-slate-500">품목명, 규격, 단위가 같은 품목이 있습니다. 등록하시겠습니까?</p>
        <div class="mt-4 max-h-64 overflow-y-auto rounded-2xl border border-slate-100">
          <div v-for="item in itemMasterStore.duplicateCheck?.items" :key="item.itemId" class="border-b border-slate-100 p-3 last:border-b-0">
            <p class="font-mono text-sm font-black text-slate-950">{{ item.itemCode }}</p>
            <p class="text-sm font-bold text-slate-700">{{ item.itemName }} · {{ item.spec || '규격 없음' }} · {{ item.unit }}</p>
          </div>
        </div>
        <div class="mt-5 flex justify-end gap-2">
          <button class="rounded-2xl bg-slate-100 px-5 py-2.5 text-sm font-black" type="button" @click="isDuplicateOpen = false">취소</button>
          <button class="rounded-2xl bg-amber-600 px-5 py-2.5 text-sm font-black text-white" type="button" @click="submitDuplicateConfirmed">등록</button>
        </div>
      </div>
    </div>
  </div>
</template>
