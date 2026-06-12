<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Building2, CheckCircle2, Loader2, RefreshCw, Search, Users, X } from '@lucide/vue'
import { useAuthStore } from '@/state/authStore'
import { usePartnerMasterStore } from '@/state/partnerMasterStore'
import type { PartnerMasterRequest, PartnerMasterResponse, PartnerStatus, PartnerType } from '@/api/partnerMasterApi'

const authStore = useAuthStore()
const partnerMasterStore = usePartnerMasterStore()
const router = useRouter()

const partnerTypeOptions: Array<{ value: PartnerType; label: string; description: string }> = [
  { value: 'SUPPLIER', label: '공급사', description: '입고 등록에서 납품 공급처로 사용됩니다.' },
  { value: 'CUSTOMER', label: '고객사', description: '출하 지시에서 출하 대상 고객사로 사용됩니다.' }
]

const filterKeyword = ref('')
const filterPartnerType = ref<'ALL' | PartnerType>('ALL')
const filterPartnerStatus = ref<'ALL' | PartnerStatus>('ALL')
const filterHasBusinessNo = ref<'ALL' | 'YES' | 'NO'>('ALL')
const sortField = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSuggestOpen = ref(false)
let suggestionTimer: number | undefined

const isFormOpen = ref(false)
const formError = ref<string | null>(null)
const form = reactive<PartnerMasterRequest>({
  partnerCode: '',
  partnerName: '',
  partnerType: 'SUPPLIER',
  businessNo: '',
  representative: '',
  contactPhone: '',
  contactEmail: '',
  note: ''
})

const stats = computed(() => ({
  total: partnerMasterStore.totalElements,
  active: partnerMasterStore.partners.filter((partner) => partner.partnerStatus === 'ACTIVE').length,
  inactive: partnerMasterStore.partners.filter((partner) => partner.partnerStatus === 'INACTIVE').length
}))

const canWrite = computed(() => ['ADMIN', 'MANAGER'].includes(authStore.user?.role || ''))

const keywordSuggestions = computed(() => {
  const keyword = filterKeyword.value.trim().toLowerCase()
  if (!keyword) return []
  const unique = new Map<number, PartnerMasterResponse>()
  ;[...partnerMasterStore.partners, ...partnerMasterStore.suggestions].forEach((partner) => {
    if (unique.has(partner.partnerId)) return
    if (isKeywordMatched(partner, keyword)) unique.set(partner.partnerId, partner)
  })
  return [...unique.values()].slice(0, 8)
})

onMounted(async () => {
  await fetchPartners(0)
})

async function fetchPartners(page = partnerMasterStore.page) {
  try {
    pageError.value = null
    await partnerMasterStore.loadPartners({
      page,
      size: partnerMasterStore.size,
      sort: `${sortField.value},${sortDirection.value}`,
      partnerType: filterPartnerType.value === 'ALL' ? undefined : filterPartnerType.value,
      partnerStatus: filterPartnerStatus.value === 'ALL' ? undefined : filterPartnerStatus.value,
      hasBusinessNo: filterHasBusinessNo.value === 'ALL' ? undefined : filterHasBusinessNo.value === 'YES',
      keyword: filterKeyword.value.trim() || undefined
    })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 목록을 불러오지 못했습니다.'
  }
}

async function resetFilters() {
  filterKeyword.value = ''
  filterPartnerType.value = 'ALL'
  filterPartnerStatus.value = 'ALL'
  filterHasBusinessNo.value = 'ALL'
  sortField.value = 'createdAt'
  sortDirection.value = 'desc'
  isSuggestOpen.value = false
  partnerMasterStore.selectPartner(null)
  await fetchPartners(0)
}

async function handleSearch() {
  partnerMasterStore.selectPartner(null)
  isSuggestOpen.value = false
  await fetchPartners(0)
}

function handleKeywordInput() {
  window.clearTimeout(suggestionTimer)
  const keyword = filterKeyword.value.trim()
  if (!keyword) {
    partnerMasterStore.suggestions = []
    isSuggestOpen.value = false
    return
  }
  isSuggestOpen.value = true
  suggestionTimer = window.setTimeout(async () => {
    await partnerMasterStore.loadSuggestions(keyword)
    isSuggestOpen.value = keywordSuggestions.value.length > 0
  }, 180)
}

function selectSuggestion(partner: PartnerMasterResponse) {
  filterKeyword.value = `${partner.partnerCode} ${partner.partnerName}`
  isSuggestOpen.value = false
  goToDetail(partner)
}

function openSuggestions() {
  isSuggestOpen.value = keywordSuggestions.value.length > 0
}

function isKeywordMatched(partner: PartnerMasterResponse, keyword: string) {
  return [
    String(partner.partnerId),
    partner.partnerCode,
    partner.partnerName,
    getPartnerTypeLabel(partner.partnerType),
    partner.partnerType,
    partner.partnerStatus,
    partner.businessNo || '',
    partner.representative || '',
    partner.contactPhone || '',
    partner.contactEmail || '',
    partner.note || ''
  ].some((value) => value.toLowerCase().includes(keyword))
}

async function goToDetail(partner: PartnerMasterResponse) {
  partnerMasterStore.selectPartner(partner)
  await router.push({ name: 'partner-master-detail', params: { id: partner.partnerId } })
}

function openCreateForm() {
  if (!canWrite.value) return
  resetForm()
  formError.value = null
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  formError.value = null
}

function resetForm() {
  form.partnerCode = ''
  form.partnerName = ''
  form.partnerType = 'SUPPLIER'
  form.businessNo = ''
  form.representative = ''
  form.contactPhone = ''
  form.contactEmail = ''
  form.note = ''
}

function handleFormTypeChange() {
  if (form.partnerCode === 'SUP-' || form.partnerCode === 'CUS-') form.partnerCode = ''
}

async function submitForm() {
  const payload = normalizeForm()
  const validationError = validateForm(payload)
  if (validationError) {
    formError.value = validationError
    return
  }

  try {
    formError.value = null
    if (!isAutoGeneratedCodeRequest(payload)) {
      const duplicated = await partnerMasterStore.checkDuplicate(payload.partnerCode)
      if (duplicated) {
        formError.value = '이미 사용 중인 거래처 코드입니다.'
        return
      }
    }
    const created = await partnerMasterStore.createPartner(payload)
    showToast('신규 거래처 마스터가 등록되었습니다.')
    closeForm()
    await fetchPartners(0)
    await goToDetail(created)
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '거래처 저장에 실패했습니다.'
  }
}

function normalizeForm(): PartnerMasterRequest {
  return {
    partnerCode: form.partnerCode.trim(),
    partnerName: form.partnerName.trim(),
    partnerType: form.partnerType,
    businessNo: normalizeOptionalText(form.businessNo),
    representative: normalizeOptionalText(form.representative),
    contactPhone: normalizeOptionalText(form.contactPhone),
    contactEmail: normalizeOptionalText(form.contactEmail),
    note: normalizeOptionalText(form.note)
  }
}

function normalizeOptionalText(value: string | null | undefined) {
  const trimmedValue = value?.trim() || ''
  return trimmedValue.length > 0 ? trimmedValue : null
}

function validateForm(payload: PartnerMasterRequest) {
  const expectedPrefix = payload.partnerType === 'SUPPLIER' ? 'SUP-' : 'CUS-'
  if (isAutoGeneratedCodeRequest(payload)) return null
  if (!payload.partnerCode.startsWith(expectedPrefix)) return `거래처 코드는 ${expectedPrefix}로 시작해야 합니다.`
  if (!/^[A-Z0-9-]+$/.test(payload.partnerCode)) return '거래처 코드는 대문자 영문, 숫자, 하이픈만 사용할 수 있습니다.'
  if (payload.partnerCode.length > 50) return '거래처 코드는 50자 이하여야 합니다.'
  if (!payload.partnerName) return '거래처명을 입력해주세요.'
  if (payload.partnerName.length > 100) return '거래처명은 100자 이하여야 합니다.'
  if (payload.businessNo && payload.businessNo.length > 50) return '사업자등록번호는 50자 이하여야 합니다.'
  if (payload.representative && payload.representative.length > 50) return '대표자명은 50자 이하여야 합니다.'
  if (payload.contactPhone && !/^[0-9+()\-\s]{7,50}$/.test(payload.contactPhone)) return '담당자 연락처 형식이 올바르지 않습니다.'
  if (payload.contactEmail && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(payload.contactEmail)) return '담당자 이메일 형식이 올바르지 않습니다.'
  if (payload.note && payload.note.length > 1000) return '비고는 1000자 이하여야 합니다.'
  return null
}

function isAutoGeneratedCodeRequest(payload: PartnerMasterRequest) {
  const expectedPrefix = payload.partnerType === 'SUPPLIER' ? 'SUP-' : 'CUS-'
  return !payload.partnerCode || payload.partnerCode === expectedPrefix
}

async function changeSort(field: string) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDirection.value = field === 'partnerName' ? 'asc' : 'desc'
  }
  await fetchPartners(0)
}

async function goToPage(nextPage: number) {
  const safePage = Math.max(0, Math.min(nextPage, Math.max(partnerMasterStore.totalPages - 1, 0)))
  await fetchPartners(safePage)
}

function getPartnerTypeLabel(partnerType: PartnerType) {
  return partnerTypeOptions.find((option) => option.value === partnerType)?.label || partnerType
}

function getSuggestionMeta(partner: PartnerMasterResponse) {
  return `${getPartnerTypeLabel(partner.partnerType)} / ${partner.partnerStatus === 'ACTIVE' ? '활성' : '비활성'} / 사용 ${partner.usageCount}건`
}

function formatDateTime(dateTimeStr: string | null) {
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
  <div class="space-y-6">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div>
        <h1 class="text-3xl font-bold app-text-primary">거래처 마스터 관리</h1>
        <p class="mt-2 app-text-muted">거래처를 등록하고 상태, 사용 현황, 입고·출하 이력을 관리합니다.</p>
      </div>
      <div v-if="successToast" class="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">
        <CheckCircle2 class="h-4 w-4" />
        {{ successToast }}
      </div>
    </div>

    <div v-if="pageError" class="flex items-center justify-between rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">
      <span>{{ pageError }}</span>
      <button type="button" class="rounded-full p-1 hover:bg-rose-100" @click="pageError = null">
        <X class="h-4 w-4" />
      </button>
    </div>

    <div class="grid gap-4 md:grid-cols-3">
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm app-text-muted">전체 거래처</p>
            <p class="mt-2 text-2xl font-bold app-text-primary">{{ stats.total }} 사</p>
          </div>
          <div class="rounded-2xl bg-slate-100 p-3 text-slate-700">
            <Building2 class="h-6 w-6" />
          </div>
        </div>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm app-text-muted">활성 거래처</p>
            <p class="mt-2 text-2xl font-bold text-emerald-700">{{ stats.active }} 사</p>
          </div>
          <div class="rounded-2xl bg-emerald-50 p-3 text-emerald-700">
            <Users class="h-6 w-6" />
          </div>
        </div>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm app-text-muted">비활성 거래처</p>
            <p class="mt-2 text-2xl font-bold text-amber-700">{{ stats.inactive }} 사</p>
          </div>
          <div class="rounded-2xl bg-amber-50 p-3 text-amber-700">
            <RefreshCw class="h-6 w-6" />
          </div>
        </div>
      </div>
    </div>

    <section class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
      <div class="grid grid-cols-1 gap-3 lg:grid-cols-[1.5fr_0.8fr_0.8fr_0.8fr_auto]">
        <div class="relative">
          <Search class="pointer-events-none absolute left-4 top-3.5 z-10 h-4 w-4 app-text-muted" />
          <input
            id="partner-keyword"
            v-model="filterKeyword"
            class="h-11 w-full rounded-xl border app-border app-bg-surface pl-11 pr-4 text-sm app-font-label outline-none transition focus:ring-2"
            placeholder="거래처 ID, 거래처코드, 거래처명, 사업자번호 검색"
            @input="handleKeywordInput"
            @focus="openSuggestions"
            @keyup.enter="handleSearch"
          >
          <div v-if="isSuggestOpen && keywordSuggestions.length > 0" class="absolute left-0 right-0 top-[3.1rem] z-30 max-h-80 overflow-y-auto rounded-xl border app-border app-bg-surface py-2 shadow-xl">
            <button
              v-for="suggestion in keywordSuggestions"
              :key="suggestion.partnerId"
              type="button"
              class="block w-full px-4 py-2.5 text-left transition app-hover-muted"
              @mousedown.prevent="selectSuggestion(suggestion)"
            >
              <span class="block text-sm app-font-emphasis leading-5 app-text-strong">{{ suggestion.partnerCode }} · {{ suggestion.partnerName }}</span>
              <span class="mt-0.5 block text-xs app-font-strong app-text-muted">{{ getSuggestionMeta(suggestion) }}</span>
            </button>
          </div>
        </div>

        <select id="partner-type-filter" v-model="filterPartnerType" class="h-11 rounded-2xl border app-border px-4 text-sm app-font-label outline-none">
          <option value="ALL">전체 구분</option>
          <option v-for="option in partnerTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>

        <select id="partner-status-filter" v-model="filterPartnerStatus" class="h-11 rounded-2xl border app-border px-4 text-sm app-font-label outline-none">
          <option value="ALL">전체 상태</option>
          <option value="ACTIVE">활성</option>
          <option value="INACTIVE">비활성</option>
        </select>

        <select id="partner-business-filter" v-model="filterHasBusinessNo" class="h-11 rounded-2xl border app-border px-4 text-sm app-font-label outline-none">
          <option value="ALL">사업자번호 전체</option>
          <option value="YES">사업자번호 등록</option>
          <option value="NO">사업자번호 미등록</option>
        </select>

        <div class="flex gap-2">
          <button class="h-11 rounded-2xl app-bg-strong px-5 text-sm app-font-emphasis app-text-inverse" type="button" @click="handleSearch">조회</button>
          <button class="h-11 rounded-2xl app-bg-muted px-5 text-sm app-font-emphasis app-text-soft" type="button" @click="resetFilters">초기화</button>
          <button v-if="canWrite" class="h-11 rounded-2xl app-accent-bg px-5 text-sm app-font-emphasis app-text-inverse" type="button" @click="openCreateForm">신규 등록</button>
        </div>
      </div>
    </section>

    <section class="overflow-hidden rounded-3xl border app-border app-bg-surface shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full min-w-[1120px] text-left text-sm">
          <thead class="app-bg-muted text-xs uppercase tracking-widest app-text-muted">
            <tr>
              <th class="px-4 py-3">No</th>
              <th class="cursor-pointer px-4 py-3" @click="changeSort('partnerCode')">
                거래처 코드
                <span v-if="sortField === 'partnerCode'">{{ sortDirection === 'asc' ? '▲' : '▼' }}</span>
              </th>
              <th class="cursor-pointer px-4 py-3" @click="changeSort('partnerName')">
                거래처명
                <span v-if="sortField === 'partnerName'">{{ sortDirection === 'asc' ? '▲' : '▼' }}</span>
              </th>
              <th class="px-4 py-3">구분</th>
              <th class="px-5 py-3">상태</th>
              <th class="px-4 py-3">사업자등록번호</th>
              <th class="px-4 py-3">담당자</th>
              <th class="cursor-pointer px-4 py-3 text-right" @click="changeSort('usageCount')">
                사용 건수
                <span v-if="sortField === 'usageCount'">{{ sortDirection === 'asc' ? '▲' : '▼' }}</span>
              </th>
              <th class="cursor-pointer px-4 py-3" @click="changeSort('lastUsedAt')">
                최근 거래 일시
                <span v-if="sortField === 'lastUsedAt'">{{ sortDirection === 'asc' ? '▲' : '▼' }}</span>
              </th>
              <th class="cursor-pointer px-4 py-3" @click="changeSort('createdAt')">
                등록 일시
                <span v-if="sortField === 'createdAt'">{{ sortDirection === 'asc' ? '▲' : '▼' }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="partnerMasterStore.isLoading">
              <td colspan="10" class="px-4 py-10 text-center app-font-strong app-text-muted">
                <Loader2 class="mx-auto mb-2 h-5 w-5 animate-spin" />
                거래처 데이터를 가져오고 있습니다.
              </td>
            </tr>
            <tr v-else-if="partnerMasterStore.partners.length === 0">
              <td colspan="10" class="px-4 py-10 text-center app-font-strong app-text-muted">조회된 거래처가 없습니다.</td>
            </tr>
            <tr
              v-for="(partner, index) in partnerMasterStore.partners"
              v-else
              :key="partner.partnerId"
              class="cursor-pointer border-t app-border-muted transition app-hover-muted"
              @click="goToDetail(partner)"
            >
              <td class="px-4 py-3 font-mono text-xs app-text-muted">{{ partnerMasterStore.page * partnerMasterStore.size + index + 1 }}</td>
              <td class="px-4 py-3 font-mono app-font-emphasis app-text-strong">{{ partner.partnerCode }}</td>
              <td class="px-4 py-3 app-font-strong app-text-strong">{{ partner.partnerName }}</td>
              <td class="px-4 py-3">
                {{ getPartnerTypeLabel(partner.partnerType) }}
              </td>
              <td class="px-4 py-3">
                <span class="rounded-full px-2.5 py-1 text-xs app-font-emphasis" :class="partner.partnerStatus === 'ACTIVE' ? 'app-status-success' : 'app-status-warning'">
                  {{ partner.partnerStatus === 'ACTIVE' ? '활성' : '비활성' }}
                </span>
              </td>
              <td class="px-4 py-3 font-mono text-xs app-text-muted">{{ partner.businessNo || '미등록' }}</td>
              <td class="px-4 py-3 app-text-muted">{{ partner.representative || '-' }}</td>
              <td class="px-4 py-3 text-right app-font-strong app-text-strong">{{ partner.usageCount }}건</td>
              <td class="px-4 py-3 text-xs app-text-muted">{{ formatDateTime(partner.lastUsedAt) }}</td>
              <td class="px-4 py-3 text-xs app-text-muted">{{ formatDateTime(partner.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex flex-col gap-3 border-t app-border-muted px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-sm app-font-strong app-text-muted">총 {{ partnerMasterStore.totalElements.toLocaleString() }}건 · {{ partnerMasterStore.page + 1 }} / {{ Math.max(partnerMasterStore.totalPages, 1) }} 페이지</p>
        <div class="flex gap-2">
          <button class="rounded-xl app-bg-muted px-4 py-2 text-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="partnerMasterStore.page === 0" @click="goToPage(0)">처음</button>
          <button class="rounded-xl app-bg-muted px-4 py-2 text-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="partnerMasterStore.page === 0" @click="goToPage(partnerMasterStore.page - 1)">이전</button>
          <button class="rounded-xl app-bg-muted px-4 py-2 text-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="partnerMasterStore.page >= partnerMasterStore.totalPages - 1" @click="goToPage(partnerMasterStore.page + 1)">다음</button>
          <button class="rounded-xl app-bg-muted px-4 py-2 text-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="partnerMasterStore.page >= partnerMasterStore.totalPages - 1" @click="goToPage(partnerMasterStore.totalPages - 1)">마지막</button>
        </div>
      </div>
    </section>

    <div v-if="isFormOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4">
      <div class="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-3xl border app-border app-bg-surface p-6 shadow-2xl">
        <div class="mb-5 flex items-start justify-between gap-4">
          <div>
            <h2 class="text-2xl font-bold app-text-primary">신규 거래처 등록</h2>
            <p class="mt-1 text-sm app-text-muted">거래처 코드는 미입력 시 구분에 따라 SUP-____ 또는 CUS-____ 형식으로 자동 생성됩니다.</p>
          </div>
          <button type="button" class="rounded-full p-2 hover:bg-slate-100" @click="closeForm">
            <X class="h-5 w-5" />
          </button>
        </div>

        <div v-if="formError" class="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-semibold text-rose-700">
          {{ formError }}
        </div>

        <form class="grid gap-4 md:grid-cols-2" @submit.prevent="submitForm">
          <div>
            <label for="partner-form-type" class="mb-2 block text-sm font-semibold app-text-primary">거래처 구분</label>
            <select id="partner-form-type" v-model="form.partnerType" class="w-full rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" @change="handleFormTypeChange">
              <option v-for="option in partnerTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </div>
          <div>
            <label for="partner-form-code" class="mb-2 block text-sm font-semibold app-text-primary">거래처 코드</label>
            <input id="partner-form-code" v-model="form.partnerCode" class="w-full rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" placeholder="미입력 시 자동 생성">
          </div>
          <div>
            <label for="partner-form-name" class="mb-2 block text-sm font-semibold app-text-primary">거래처명</label>
            <input id="partner-form-name" v-model="form.partnerName" class="w-full rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" placeholder="거래처명 입력">
          </div>
          <div>
            <label for="partner-form-business" class="mb-2 block text-sm font-semibold app-text-primary">사업자등록번호</label>
            <input id="partner-form-business" v-model="form.businessNo" class="w-full rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" placeholder="선택 입력">
          </div>
          <div>
            <label for="partner-form-representative" class="mb-2 block text-sm font-semibold app-text-primary">대표자명</label>
            <input id="partner-form-representative" v-model="form.representative" class="w-full rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" placeholder="선택 입력">
          </div>
          <div>
            <label for="partner-form-phone" class="mb-2 block text-sm font-semibold app-text-primary">담당자 연락처</label>
            <input id="partner-form-phone" v-model="form.contactPhone" class="w-full rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" placeholder="010-0000-0000">
          </div>
          <div class="md:col-span-2">
            <label for="partner-form-email" class="mb-2 block text-sm font-semibold app-text-primary">담당자 이메일</label>
            <input id="partner-form-email" v-model="form.contactEmail" class="w-full rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" placeholder="partner@example.com">
          </div>
          <div class="md:col-span-2">
            <label for="partner-form-note" class="mb-2 block text-sm font-semibold app-text-primary">비고</label>
            <textarea id="partner-form-note" v-model="form.note" rows="4" class="w-full resize-none rounded-2xl border app-border app-bg-card px-4 py-3 text-sm app-text-primary outline-none focus:border-slate-400 focus:ring-4 focus:ring-slate-100" placeholder="업무 메모를 입력하세요."></textarea>
          </div>
          <div class="md:col-span-2 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button type="button" class="rounded-2xl border app-border px-5 py-3 text-sm font-semibold app-text-primary hover:bg-slate-50" @click="closeForm">취소</button>
            <button type="submit" class="rounded-2xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-50" :disabled="partnerMasterStore.isSaving">
              {{ partnerMasterStore.isSaving ? '저장 중...' : '저장' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
