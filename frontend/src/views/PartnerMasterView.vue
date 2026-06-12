<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Building2,
  Edit3,
  FileSpreadsheet,
  Loader2,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Truck,
  Users,
  X
} from '@lucide/vue'
import { usePartnerMasterStore } from '@/state/partnerMasterStore'
import type { PartnerMasterRequest, PartnerMasterResponse, PartnerStatus, PartnerType } from '@/api/partnerMasterApi'

const partnerMasterStore = usePartnerMasterStore()

const partnerTypeOptions: Array<{ value: PartnerType; label: string; description: string }> = [
  { value: 'SUPPLIER', label: '공급사', description: '입고 등록에서 납품 공급처로 사용됩니다.' },
  { value: 'CUSTOMER', label: '고객사', description: '출하 지시에서 출하 대상 고객사로 사용됩니다.' }
]

const filterKeyword = ref('')
const filterPartnerType = ref<'ALL' | PartnerType>('ALL')
const filterPartnerStatus = ref<'ALL' | PartnerStatus>('ALL')
const filterHasBusinessNo = ref<'ALL' | 'YES' | 'NO'>('ALL')
const sortOption = ref('createdAt,desc')
const mainTab = ref<'list' | 'detail'>('list')
const detailTab = ref<'profile' | 'system' | 'history'>('profile')
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSuggestOpen = ref(false)
let suggestionTimer: number | undefined

const isFormOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formError = ref<string | null>(null)
const editingPartnerId = ref<number | null>(null)
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

const selectedPartner = computed(() => partnerMasterStore.selectedPartner)
const selectedUsage = computed(() => partnerMasterStore.selectedUsage)

const stats = computed(() => ({
  total: partnerMasterStore.totalElements,
  supplier: partnerMasterStore.partners.filter((partner) => partner.partnerType === 'SUPPLIER').length,
  customer: partnerMasterStore.partners.filter((partner) => partner.partnerType === 'CUSTOMER').length,
  withBusinessNo: partnerMasterStore.partners.filter((partner) => Boolean(partner.businessNo)).length,
  inactive: partnerMasterStore.partners.filter((partner) => partner.partnerStatus === 'INACTIVE').length
}))

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

const selectedTypeOption = computed(() => {
  if (!selectedPartner.value) return null
  return partnerTypeOptions.find((option) => option.value === selectedPartner.value?.partnerType) || null
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
      sort: sortOption.value,
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
  sortOption.value = 'createdAt,desc'
  isSuggestOpen.value = false
  partnerMasterStore.selectPartner(null)
  mainTab.value = 'list'
  await fetchPartners(0)
}

async function handleSearch() {
  partnerMasterStore.selectPartner(null)
  mainTab.value = 'list'
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
  selectPartner(partner)
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

async function selectPartner(partner: PartnerMasterResponse) {
  partnerMasterStore.selectPartner(partner)
  mainTab.value = 'detail'
  detailTab.value = 'profile'
  await loadSelectedPartnerContext(partner)
}

function openCreateForm() {
  formMode.value = 'create'
  editingPartnerId.value = null
  resetForm()
  formError.value = null
  isFormOpen.value = true
}

function openEditForm(partner: PartnerMasterResponse) {
  formMode.value = 'edit'
  editingPartnerId.value = partner.partnerId
  form.partnerCode = partner.partnerCode
  form.partnerName = partner.partnerName
  form.partnerType = partner.partnerType
  form.businessNo = partner.businessNo || ''
  form.representative = partner.representative || ''
  form.contactPhone = partner.contactPhone || ''
  form.contactEmail = partner.contactEmail || ''
  form.note = partner.note || ''
  formError.value = null
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  formError.value = null
}

function resetForm() {
  form.partnerCode = 'SUP-'
  form.partnerName = ''
  form.partnerType = 'SUPPLIER'
  form.businessNo = ''
  form.representative = ''
  form.contactPhone = ''
  form.contactEmail = ''
  form.note = ''
}

function handleFormTypeChange() {
  if (formMode.value === 'edit') return
  const nextPrefix = form.partnerType === 'SUPPLIER' ? 'SUP-' : 'CUS-'
  const previousPrefix = form.partnerType === 'SUPPLIER' ? 'CUS-' : 'SUP-'
  if (!form.partnerCode || form.partnerCode === previousPrefix || form.partnerCode === 'SUP-' || form.partnerCode === 'CUS-') {
    form.partnerCode = nextPrefix
  }
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
    if (formMode.value === 'create') {
      const duplicated = await partnerMasterStore.checkDuplicate(payload.partnerCode)
      if (duplicated) {
        formError.value = '이미 사용 중인 거래처 코드입니다.'
        return
      }
      const created = await partnerMasterStore.createPartner(payload)
      showToast('신규 거래처 마스터가 등록되었습니다.')
      closeForm()
      await fetchPartners(0)
      await selectPartner(created)
      return
    }

    if (editingPartnerId.value !== null) {
      const updated = await partnerMasterStore.updatePartner(editingPartnerId.value, payload)
      showToast('거래처 마스터 정보가 수정되었습니다.')
      closeForm()
      await fetchPartners(partnerMasterStore.page)
      await selectPartner(updated)
    }
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
  if (!payload.partnerCode) return '거래처 코드를 입력해주세요.'
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

async function requestDelete(partner: PartnerMasterResponse) {
  const usage = await partnerMasterStore.loadPartnerUsage(partner.partnerId)
  if (!usage.canDelete) {
    if (!confirm(`${usage.deleteBlockedReason}\n삭제할 수 없으므로 비활성화로 변경하시겠습니까?`)) return
    await updateStatus(partner, 'INACTIVE')
    return
  }

  if (!confirm(`[${partner.partnerCode}] ${partner.partnerName} 거래처를 삭제하시겠습니까?`)) return

  try {
    pageError.value = null
    await partnerMasterStore.deletePartner(partner.partnerId)
    showToast('거래처 마스터가 삭제되었습니다.')
    mainTab.value = 'list'
    await fetchPartners(partnerMasterStore.page)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 삭제에 실패했습니다.'
  }
}

async function updateStatus(partner: PartnerMasterResponse, partnerStatus: PartnerStatus) {
  try {
    pageError.value = null
    const updated = await partnerMasterStore.updatePartnerStatus(partner.partnerId, partnerStatus)
    showToast(partnerStatus === 'ACTIVE' ? '거래처가 활성화되었습니다.' : '거래처가 비활성화되었습니다.')
    await fetchPartners(partnerMasterStore.page)
    await selectPartner(updated)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 상태 변경에 실패했습니다.'
  }
}

async function loadSelectedPartnerContext(partner: PartnerMasterResponse) {
  try {
    await partnerMasterStore.loadPartnerUsage(partner.partnerId)
    if (partner.partnerType === 'SUPPLIER') {
      await partnerMasterStore.loadSuppliedItems(partner.partnerId)
    } else {
      await partnerMasterStore.loadShippedItems(partner.partnerId)
    }
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 상세 정보를 불러오지 못했습니다.'
  }
}

async function changeSort() {
  await fetchPartners(0)
}

async function goToPage(nextPage: number) {
  const safePage = Math.max(0, Math.min(nextPage, Math.max(partnerMasterStore.totalPages - 1, 0)))
  await fetchPartners(safePage)
}

async function copyText(value: string | null, label: string) {
  if (!value) return
  await navigator.clipboard.writeText(value)
  showToast(`${label}이 복사되었습니다.`)
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
  <div class="partner-master-page">
    <div class="partner-master-header">
      <div>
        <h1 class="partner-master-title">
          <span class="partner-master-title-mark"></span>
          거래처 마스터 관리
        </h1>
        <p class="partner-master-subtitle">검색 후 전체 목록에서 거래처를 선택하면 세부 정보 탭으로 이동합니다.</p>
      </div>
      <div v-if="successToast" class="partner-master-toast">
        <span class="partner-master-toast-dot"></span>
        {{ successToast }}
      </div>
    </div>

    <div v-if="pageError" class="partner-master-error">
      <span>{{ pageError }}</span>
      <button class="partner-master-icon-button" type="button" @click="pageError = null">
        <X class="partner-master-icon-sm" />
      </button>
    </div>

    <div class="partner-master-stats-grid">
      <div class="partner-master-stat-card">
        <div class="partner-master-stat-icon partner-master-stat-icon-neutral"><Building2 class="partner-master-icon-md" /></div>
        <div><p class="partner-master-stat-label">전체 거래처</p><p class="partner-master-stat-value">{{ stats.total }} 사</p></div>
      </div>
      <div class="partner-master-stat-card">
        <div class="partner-master-stat-icon partner-master-stat-icon-primary"><Truck class="partner-master-icon-md" /></div>
        <div><p class="partner-master-stat-label">공급사 / 고객사</p><p class="partner-master-stat-value">{{ stats.supplier }} / {{ stats.customer }} 사</p></div>
      </div>
      <div class="partner-master-stat-card">
        <div class="partner-master-stat-icon partner-master-stat-icon-success"><FileSpreadsheet class="partner-master-icon-md" /></div>
        <div><p class="partner-master-stat-label">사업자번호 등록</p><p class="partner-master-stat-value">{{ stats.withBusinessNo }} 사</p></div>
      </div>
      <div class="partner-master-stat-card">
        <div class="partner-master-stat-icon partner-master-stat-icon-neutral"><Users class="partner-master-icon-md" /></div>
        <div><p class="partner-master-stat-label">비활성 거래처</p><p class="partner-master-stat-value">{{ stats.inactive }} 사</p></div>
      </div>
    </div>

    <section class="partner-master-panel">
      <div class="partner-master-panel-heading">
        <span class="partner-master-panel-title"><Search class="partner-master-icon-sm" />거래처 검색</span>
        <button class="partner-master-primary-button" type="button" @click="openCreateForm"><Plus class="partner-master-icon-sm" />신규 거래처 등록</button>
      </div>
      <div class="partner-master-search-grid">
        <div class="partner-master-field" style="position: relative;">
          <label class="partner-master-label" for="partner-keyword">통합 검색</label>
          <input
            id="partner-keyword"
            v-model="filterKeyword"
            class="partner-master-input"
            placeholder="거래처 ID, 코드, 거래처명, 사업자번호, 담당자 검색"
            @input="handleKeywordInput"
            @focus="openSuggestions"
            @keyup.enter="handleSearch"
          >
          <div v-if="isSuggestOpen && keywordSuggestions.length > 0" class="partner-master-suggestion-list">
            <button
              v-for="suggestion in keywordSuggestions"
              :key="suggestion.partnerId"
              class="partner-master-suggestion-item"
              type="button"
              @mousedown.prevent="selectSuggestion(suggestion)"
            >
              <span>{{ suggestion.partnerCode }} · {{ suggestion.partnerName }}</span>
              <small>{{ getSuggestionMeta(suggestion) }}</small>
            </button>
          </div>
        </div>
        <div class="partner-master-field">
          <label class="partner-master-label" for="partner-type-filter">거래처 구분</label>
          <select id="partner-type-filter" v-model="filterPartnerType" class="partner-master-input">
            <option value="ALL">전체 거래처</option>
            <option v-for="option in partnerTypeOptions" :key="option.value" :value="option.value">{{ option.label }} ({{ option.value }})</option>
          </select>
        </div>
        <div class="partner-master-field">
          <label class="partner-master-label" for="partner-status-filter">거래처 상태</label>
          <select id="partner-status-filter" v-model="filterPartnerStatus" class="partner-master-input">
            <option value="ALL">전체 상태</option>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
          </select>
        </div>
        <div class="partner-master-field">
          <label class="partner-master-label" for="partner-business-filter">사업자번호</label>
          <select id="partner-business-filter" v-model="filterHasBusinessNo" class="partner-master-input">
            <option value="ALL">전체</option>
            <option value="YES">등록</option>
            <option value="NO">미등록</option>
          </select>
        </div>
        <div class="partner-master-search-actions">
          <button class="partner-master-secondary-button" type="button" @click="resetFilters">초기화</button>
          <button class="partner-master-primary-button" type="button" @click="handleSearch">조회</button>
        </div>
      </div>
    </section>

    <section class="partner-master-panel">
      <div class="partner-master-detail-tabs">
        <button type="button" class="partner-master-tab-button" :class="{ 'partner-master-tab-button-active': mainTab === 'list' }" @click="mainTab = 'list'">전체 목록</button>
        <button type="button" class="partner-master-tab-button" :class="{ 'partner-master-tab-button-active': mainTab === 'detail' }" :disabled="!selectedPartner" @click="mainTab = 'detail'">세부 정보</button>
      </div>

      <template v-if="mainTab === 'list'">
        <div class="partner-master-toolbar">
          <div class="partner-master-field partner-master-sort-field">
            <label class="partner-master-label" for="partner-sort">정렬</label>
            <select id="partner-sort" v-model="sortOption" class="partner-master-input" @change="changeSort">
              <option value="createdAt,desc">등록일 최신순</option>
              <option value="createdAt,asc">등록일 오래된순</option>
              <option value="lastUsedAt,desc">최근 거래 최신순</option>
              <option value="lastUsedAt,asc">최근 거래 오래된순</option>
              <option value="partnerName,asc">거래처명 오름차순</option>
              <option value="partnerName,desc">거래처명 내림차순</option>
              <option value="usageCount,desc">사용 건수 많은순</option>
              <option value="usageCount,asc">사용 건수 적은순</option>
            </select>
          </div>
          <button class="partner-master-secondary-button" type="button" @click="fetchPartners(partnerMasterStore.page)"><RefreshCw class="partner-master-icon-sm" />새로고침</button>
        </div>

        <div class="partner-master-table-wrap">
          <table class="partner-master-table">
            <thead>
              <tr>
                <th class="partner-master-cell-center">No</th>
                <th>거래처코드</th>
                <th>거래처명</th>
                <th class="partner-master-cell-center">구분</th>
                <th class="partner-master-cell-center">상태</th>
                <th>사업자등록번호</th>
                <th>담당자</th>
                <th class="partner-master-cell-center">사용 건수</th>
                <th class="partner-master-cell-center">최근 거래 일시</th>
                <th class="partner-master-cell-center">등록일시</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="partnerMasterStore.isLoading"><td colspan="10" class="partner-master-empty-cell"><Loader2 class="partner-master-spinner" />거래처 데이터를 가져오고 있습니다...</td></tr>
              <tr v-else-if="partnerMasterStore.partners.length === 0"><td colspan="10" class="partner-master-empty-cell">조회된 거래처 마스터가 없습니다.</td></tr>
              <tr
                v-for="(partner, index) in partnerMasterStore.partners"
                v-else
                :key="partner.partnerId"
                class="partner-master-table-row"
                @click="selectPartner(partner)"
              >
                <td class="partner-master-cell-center">{{ partnerMasterStore.page * partnerMasterStore.size + index + 1 }}</td>
                <td class="partner-master-code">{{ partner.partnerCode }}</td>
                <td class="partner-master-name">{{ partner.partnerName }}</td>
                <td class="partner-master-cell-center"><span class="partner-master-type-badge" :data-type="partner.partnerType">{{ getPartnerTypeLabel(partner.partnerType) }}</span></td>
                <td class="partner-master-cell-center"><span class="partner-master-type-badge" :data-type="partner.partnerStatus">{{ partner.partnerStatus === 'ACTIVE' ? '활성' : '비활성' }}</span></td>
                <td class="partner-master-code-muted">{{ partner.businessNo || '미등록' }}</td>
                <td>{{ partner.representative || '-' }}</td>
                <td class="partner-master-cell-center">{{ partner.usageCount }}건</td>
                <td class="partner-master-cell-center partner-master-date">{{ formatDateTime(partner.lastUsedAt) }}</td>
                <td class="partner-master-cell-center partner-master-date">{{ formatDateTime(partner.createdAt) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="5">현재 페이지: {{ partnerMasterStore.partners.length }}건 / 전체 {{ partnerMasterStore.totalElements }}건</td>
                <td colspan="5" class="partner-master-cell-right">행을 선택하면 세부 정보로 이동합니다.</td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div class="partner-master-toolbar">
          <div class="partner-master-detail-code">{{ partnerMasterStore.page + 1 }} / {{ Math.max(partnerMasterStore.totalPages, 1) }} 페이지</div>
          <div class="partner-master-toolbar-actions">
            <button class="partner-master-secondary-button" type="button" :disabled="partnerMasterStore.page === 0" @click="goToPage(0)">처음</button>
            <button class="partner-master-secondary-button" type="button" :disabled="partnerMasterStore.page === 0" @click="goToPage(partnerMasterStore.page - 1)">이전</button>
            <button class="partner-master-secondary-button" type="button" :disabled="partnerMasterStore.page >= partnerMasterStore.totalPages - 1" @click="goToPage(partnerMasterStore.page + 1)">다음</button>
            <button class="partner-master-secondary-button" type="button" :disabled="partnerMasterStore.page >= partnerMasterStore.totalPages - 1" @click="goToPage(partnerMasterStore.totalPages - 1)">마지막</button>
          </div>
        </div>
      </template>

      <template v-else>
        <div v-if="!selectedPartner" class="partner-master-empty-detail">
          <FileSpreadsheet class="partner-master-empty-icon" />
          <span>전체 목록에서 거래처를 선택해주세요.</span>
        </div>
        <div v-else class="partner-master-detail-body">
          <div class="partner-master-detail-heading">
            <div class="partner-master-detail-title"><FileSpreadsheet class="partner-master-icon-sm" />{{ selectedPartner.partnerName }}</div>
            <div class="partner-master-detail-heading-actions">
              <div class="partner-master-detail-code">{{ selectedPartner.partnerCode }}</div>
              <button class="partner-master-table-button" type="button" @click="openEditForm(selectedPartner)"><Edit3 class="partner-master-icon-xs" />수정</button>
              <button class="partner-master-table-button" type="button" @click="updateStatus(selectedPartner, selectedPartner.partnerStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')">
                {{ selectedPartner.partnerStatus === 'ACTIVE' ? '비활성화' : '활성화' }}
              </button>
              <button class="partner-master-table-danger-button" type="button" @click="requestDelete(selectedPartner)"><Trash2 class="partner-master-icon-xs" />삭제</button>
            </div>
          </div>

          <div class="partner-master-detail-tabs">
            <button type="button" class="partner-master-tab-button" :class="{ 'partner-master-tab-button-active': detailTab === 'profile' }" @click="detailTab = 'profile'">거래처 프로필</button>
            <button type="button" class="partner-master-tab-button" :class="{ 'partner-master-tab-button-active': detailTab === 'system' }" @click="detailTab = 'system'">업무 연계</button>
            <button type="button" class="partner-master-tab-button" :class="{ 'partner-master-tab-button-active': detailTab === 'history' }" @click="detailTab = 'history'">
              {{ selectedPartner.partnerType === 'SUPPLIER' ? '공급 이력' : '출하 이력' }}
            </button>
          </div>

          <div v-if="detailTab === 'profile'" class="partner-master-detail-grid">
            <div class="partner-master-detail-section">
              <h3>기본 식별 정보</h3>
              <dl class="partner-master-description-list">
                <dt>거래처 코드</dt><dd class="partner-master-code">{{ selectedPartner.partnerCode }}</dd>
                <dt>거래처명</dt><dd>{{ selectedPartner.partnerName }}</dd>
                <dt>거래처 구분</dt><dd>{{ selectedTypeOption?.label }} ({{ selectedPartner.partnerType }})</dd>
                <dt>거래처 상태</dt><dd>{{ selectedPartner.partnerStatus === 'ACTIVE' ? '활성' : '비활성' }} ({{ selectedPartner.partnerStatus }})</dd>
              </dl>
            </div>
            <div class="partner-master-detail-section">
              <h3>사업자 및 연락처</h3>
              <dl class="partner-master-description-list">
                <dt>사업자등록번호</dt><dd class="partner-master-code">{{ selectedPartner.businessNo || '미등록' }}</dd>
                <dt>대표자</dt><dd>{{ selectedPartner.representative || '-' }}</dd>
                <dt>담당자 연락처</dt>
                <dd class="partner-master-code">{{ selectedPartner.contactPhone || '-' }} <button v-if="selectedPartner.contactPhone" class="partner-master-table-button" type="button" @click="copyText(selectedPartner.contactPhone, '담당자 연락처')">복사</button></dd>
                <dt>담당자 이메일</dt>
                <dd class="partner-master-code">{{ selectedPartner.contactEmail || '-' }} <button v-if="selectedPartner.contactEmail" class="partner-master-table-button" type="button" @click="copyText(selectedPartner.contactEmail, '담당자 이메일')">복사</button></dd>
              </dl>
            </div>
            <div class="partner-master-detail-section">
              <h3>비고</h3>
              <p class="partner-master-subtitle">{{ selectedPartner.note || '등록된 비고가 없습니다.' }}</p>
            </div>
          </div>

          <div v-else-if="detailTab === 'system'" class="partner-master-detail-grid">
            <div class="partner-master-detail-section">
              <h3>업무 사용 기준</h3>
              <dl class="partner-master-description-list">
                <dt>사용 업무</dt><dd>{{ selectedPartner.partnerType === 'SUPPLIER' ? '입고 등록 공급사' : '출하 지시 고객사' }}</dd>
                <dt>설명</dt><dd>{{ selectedTypeOption?.description }}</dd>
                <dt>입고 참조</dt><dd>{{ selectedUsage?.inboundCount ?? selectedPartner.inboundCount }}건</dd>
                <dt>출하 참조</dt><dd>{{ selectedUsage?.shippingCount ?? selectedPartner.shippingCount }}건</dd>
                <dt>최근 거래 일시</dt><dd>{{ selectedUsage?.lastUsedAt ? formatDateTime(selectedUsage.lastUsedAt) : '-' }}</dd>
                <dt>삭제 가능 여부</dt><dd>{{ selectedUsage?.canDelete ? '삭제 가능' : (selectedUsage?.deleteBlockedReason || '참조 현황 확인 필요') }}</dd>
                <dt>등록일시</dt><dd>{{ formatDateTime(selectedPartner.createdAt) }}</dd>
              </dl>
            </div>
            <div class="partner-master-detail-section">
              <h3>업무 화면 안내</h3>
              <dl class="partner-master-description-list">
                <dt>신규 업무 투입</dt><dd>{{ selectedPartner.partnerStatus === 'ACTIVE' ? '입고/출하 선택 목록에 표시됩니다.' : '비활성 상태라 신규 업무 선택 목록에서 제외됩니다.' }}</dd>
                <dt>연계 화면</dt><dd>{{ selectedPartner.partnerType === 'SUPPLIER' ? '입고 등록 화면' : '출하 지시 화면' }}</dd>
              </dl>
            </div>
          </div>

          <div v-else class="partner-master-detail-grid">
            <div class="partner-master-detail-section">
              <h3>{{ selectedPartner.partnerType === 'SUPPLIER' ? '실제 공급 품목 이력' : '실제 출하 품목 이력' }}</h3>
              <template v-if="selectedPartner.partnerType === 'SUPPLIER'">
                <div v-if="partnerMasterStore.isSuppliedItemsLoading" class="partner-master-empty-detail">공급 이력을 불러오고 있습니다...</div>
                <div v-else-if="partnerMasterStore.suppliedItems.length === 0" class="partner-master-empty-detail">입고 이력 기준 실제 공급 품목이 없습니다.</div>
                <table v-else class="partner-master-table">
                  <thead><tr><th>품목코드</th><th>품목명</th><th class="partner-master-cell-center">구분</th><th class="partner-master-cell-center">누적 입고</th><th class="partner-master-cell-center">입고 건수</th><th class="partner-master-cell-center">최근 입고일</th></tr></thead>
                  <tbody>
                    <tr v-for="item in partnerMasterStore.suppliedItems" :key="item.itemCode"><td class="partner-master-code">{{ item.itemCode }}</td><td>{{ item.itemName }}</td><td class="partner-master-cell-center">{{ item.itemType }}</td><td class="partner-master-cell-center">{{ item.totalInboundQty }} {{ item.unit }}</td><td class="partner-master-cell-center">{{ item.inboundCount }}건</td><td class="partner-master-cell-center">{{ item.lastInboundDate || '-' }}</td></tr>
                  </tbody>
                </table>
              </template>
              <template v-else>
                <div v-if="partnerMasterStore.isShippedItemsLoading" class="partner-master-empty-detail">출하 이력을 불러오고 있습니다...</div>
                <div v-else-if="partnerMasterStore.shippedItems.length === 0" class="partner-master-empty-detail">출하 이력 기준 실제 출하 품목이 없습니다.</div>
                <table v-else class="partner-master-table">
                  <thead><tr><th>품목코드</th><th>품목명</th><th class="partner-master-cell-center">구분</th><th class="partner-master-cell-center">누적 출하</th><th class="partner-master-cell-center">출하 건수</th><th class="partner-master-cell-center">최근 출하일</th></tr></thead>
                  <tbody>
                    <tr v-for="item in partnerMasterStore.shippedItems" :key="item.itemCode"><td class="partner-master-code">{{ item.itemCode }}</td><td>{{ item.itemName }}</td><td class="partner-master-cell-center">{{ item.itemType }}</td><td class="partner-master-cell-center">{{ item.totalShippingQty }} {{ item.unit }}</td><td class="partner-master-cell-center">{{ item.shippingCount }}건</td><td class="partner-master-cell-center">{{ formatDateTime(item.lastShippingAt) }}</td></tr>
                  </tbody>
                </table>
              </template>
            </div>
          </div>
        </div>
      </template>
    </section>

    <div v-if="isFormOpen" class="partner-master-modal" role="dialog" aria-modal="true">
      <div class="partner-master-modal-backdrop" @click="closeForm"></div>
      <div class="partner-master-modal-shell">
        <div class="partner-master-modal-card">
          <div class="partner-master-modal-heading">
            <h2>{{ formMode === 'create' ? '신규 거래처 마스터 등록' : '거래처 마스터 수정' }}</h2>
            <button class="partner-master-icon-button" type="button" @click="closeForm"><X class="partner-master-icon-sm" /></button>
          </div>

          <form class="partner-master-form" @submit.prevent="submitForm">
            <div v-if="formError" class="partner-master-form-error">{{ formError }}</div>
            <div class="partner-master-form-notice">거래처 코드는 {{ form.partnerType === 'SUPPLIER' ? 'SUP-' : 'CUS-' }}로 시작해야 하며, 수정 시 변경할 수 없습니다.</div>

            <div class="partner-master-form-grid">
              <div class="partner-master-field">
                <label class="partner-master-label" for="partner-type">거래처 구분 *</label>
                <select id="partner-type" v-model="form.partnerType" class="partner-master-input" required :disabled="formMode === 'edit'" @change="handleFormTypeChange">
                  <option v-for="option in partnerTypeOptions" :key="option.value" :value="option.value">{{ option.label }} ({{ option.value }})</option>
                </select>
              </div>
              <div class="partner-master-field">
                <label class="partner-master-label" for="partner-code">거래처 코드 *</label>
                <input id="partner-code" v-model="form.partnerCode" class="partner-master-input" maxlength="50" placeholder="예: SUP-POSCO-01" required :disabled="formMode === 'edit'">
              </div>
            </div>

            <div class="partner-master-field"><label class="partner-master-label" for="partner-name">거래처명 *</label><input id="partner-name" v-model="form.partnerName" class="partner-master-input" maxlength="100" placeholder="예: (주)포스코 인터내셔널" required></div>
            <div class="partner-master-field"><label class="partner-master-label" for="partner-business-no">사업자등록번호</label><input id="partner-business-no" v-model="form.businessNo" class="partner-master-input" maxlength="50" placeholder="예: 123-45-67890"></div>
            <div class="partner-master-form-grid">
              <div class="partner-master-field"><label class="partner-master-label" for="partner-representative">대표자명</label><input id="partner-representative" v-model="form.representative" class="partner-master-input" maxlength="50" placeholder="예: 홍길동"></div>
              <div class="partner-master-field"><label class="partner-master-label" for="partner-contact-phone">담당자 연락처</label><input id="partner-contact-phone" v-model="form.contactPhone" class="partner-master-input" maxlength="50" placeholder="예: 02-3457-1114"></div>
            </div>
            <div class="partner-master-field"><label class="partner-master-label" for="partner-contact-email">담당자 이메일</label><input id="partner-contact-email" v-model="form.contactEmail" class="partner-master-input" maxlength="100" placeholder="예: partner@example.com"></div>
            <div class="partner-master-field"><label class="partner-master-label" for="partner-note">비고</label><textarea id="partner-note" v-model="form.note" class="partner-master-input" maxlength="1000" rows="4" placeholder="거래처 관련 메모를 입력하세요."></textarea></div>

            <div class="partner-master-modal-actions">
              <button class="partner-master-secondary-button" type="button" @click="closeForm">취소</button>
              <button class="partner-master-primary-button" type="submit" :disabled="partnerMasterStore.isSaving"><Loader2 v-if="partnerMasterStore.isSaving" class="partner-master-spinner-inline" />{{ formMode === 'create' ? '거래처 등록' : '수정 저장' }}</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped>
@reference "../main.css";

.partner-master-page {
  --partner-color-page: var(--color-page);
  --partner-color-surface: var(--color-surface);
  --partner-color-surface-muted: var(--color-border-muted);
  --partner-color-surface-soft: var(--color-surface-soft);
  --partner-color-border: var(--color-border);
  --partner-color-border-strong: var(--color-border-strong);
  --partner-color-text-primary: var(--color-text);
  --partner-color-text-secondary: var(--color-text-soft);
  --partner-color-text-muted: var(--color-text-muted);
  --partner-color-primary: var(--color-primary);
  --partner-color-primary-hover: var(--color-primary-hover);
  --partner-color-primary-soft: var(--color-primary-soft);
  --partner-color-primary-border: var(--color-primary-border);
  --partner-color-primary-hover-soft: var(--color-primary-muted);
  --partner-color-success: var(--color-success);
  --partner-color-success-soft: var(--color-success-soft);
  --partner-color-success-border: var(--color-success-border);
  --partner-color-warning: var(--color-warning);
  --partner-color-warning-soft: var(--color-warning-soft);
  --partner-color-warning-border: var(--color-warning-border);
  --partner-color-danger: var(--color-danger);
  --partner-color-danger-hover: var(--color-danger-hover);
  --partner-color-danger-soft: var(--color-danger-soft);
  --partner-color-danger-border: var(--color-danger-border);
  --partner-color-danger-hover-soft: var(--color-danger-muted);
  --partner-color-table-divider: var(--color-border-muted);
  --partner-font-weight-title: var(--font-weight-title);
  --partner-font-weight-label: var(--font-weight-title);
  --partner-font-weight-strong: var(--font-weight-emphasis);
  --partner-font-weight-body: 500;
  --partner-font-weight-muted: var(--font-weight-label);
  --partner-radius-panel: var(--radius-section);
  --partner-radius-control: var(--radius-control);
  --partner-shadow-panel: var(--shadow-panel);

  @apply space-y-6 pb-12;
  background: var(--partner-color-page);
}

.partner-master-header,
.partner-master-toolbar,
.partner-master-detail-heading,
.partner-master-panel-heading,
.partner-master-modal-heading,
.partner-master-modal-actions {
  @apply flex items-center justify-between gap-4;
}

.partner-master-title {
  @apply flex items-center gap-2 text-2xl tracking-tight;
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-title);
}

.partner-master-title-mark {
  @apply h-6 w-1.5 rounded-sm;
  background-color: var(--partner-color-primary);
}

.partner-master-subtitle {
  @apply mt-1.5 text-xs;
  color: var(--partner-color-text-muted);
  font-weight: var(--partner-font-weight-body);
}

.partner-master-toast {
  @apply flex items-center gap-2 rounded-lg border px-4 py-2 text-xs shadow-sm;
  background-color: var(--partner-color-success-soft);
  border-color: var(--partner-color-success-border);
  color: var(--partner-color-success);
  font-weight: var(--partner-font-weight-label);
  animation: partnerMasterFadeIn 0.2s ease-out forwards;
}

.partner-master-toast-dot {
  @apply h-2 w-2 rounded-full;
  background-color: var(--partner-color-success);
}

.partner-master-error,
.partner-master-form-error {
  @apply flex items-center justify-between rounded-lg border px-4 py-3 text-xs;
  background-color: var(--partner-color-danger-soft);
  border-color: var(--partner-color-danger-border);
  color: var(--partner-color-danger);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-stats-grid {
  @apply grid grid-cols-1 gap-5 sm:grid-cols-3;
}

.partner-master-stat-card {
  @apply flex items-center gap-4 rounded-xl border p-6 transition-shadow hover:shadow-md;
  background-color: var(--partner-color-surface);
  border-color: var(--partner-color-border);
  box-shadow: var(--partner-shadow-panel);
}

.partner-master-stat-icon {
  @apply rounded-lg p-3;
}

.partner-master-stat-icon-neutral {
  background-color: var(--partner-color-surface-muted);
  color: var(--partner-color-text-secondary);
}

.partner-master-stat-icon-primary {
  background-color: var(--partner-color-primary-soft);
  color: var(--partner-color-primary);
}

.partner-master-stat-icon-success {
  background-color: var(--partner-color-success-soft);
  color: var(--partner-color-success);
}

.partner-master-stat-label {
  @apply text-[10px] uppercase tracking-wider;
  color: var(--partner-color-text-muted);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-stat-value {
  @apply mt-1 text-xl;
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-strong);
}

.partner-master-panel {
  @apply overflow-hidden rounded-xl border;
  background-color: var(--partner-color-surface);
  border-color: var(--partner-color-border);
  box-shadow: var(--partner-shadow-panel);
}

.partner-master-panel-heading,
.partner-master-detail-heading {
  @apply border-b px-5 py-3.5;
  background-color: var(--partner-color-surface-muted);
  border-color: var(--partner-color-border);
}

.partner-master-panel-title,
.partner-master-detail-title {
  @apply flex items-center gap-2 text-xs;
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-icon-button {
  @apply rounded p-1 transition-colors;
  color: var(--partner-color-text-muted);
}

.partner-master-icon-button:hover {
  background-color: var(--partner-color-border);
  color: var(--partner-color-text-secondary);
}

.partner-master-search-grid {
  @apply grid grid-cols-1 gap-5 border-t p-5 text-xs sm:grid-cols-2 lg:grid-cols-4;
  border-color: var(--partner-color-border);
}

.partner-master-field {
  @apply space-y-1.5;
}

.partner-master-label {
  @apply block;
  color: var(--partner-color-text-secondary);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-input {
  @apply h-9 w-full rounded-lg border px-3 text-xs outline-none transition;
  background-color: var(--partner-color-surface);
  border-color: var(--partner-color-border-strong);
  color: var(--partner-color-text-primary);
}

.partner-master-input:focus {
  border-color: var(--partner-color-primary);
  box-shadow: 0 0 0 1px var(--partner-color-primary);
}

.partner-master-search-actions {
  @apply flex items-end justify-end gap-2 sm:col-span-2 lg:col-span-2;
}

.partner-master-primary-button,
.partner-master-secondary-button,
.partner-master-danger-button {
  @apply inline-flex h-9 items-center justify-center gap-2 rounded-lg px-4 text-xs transition disabled:cursor-not-allowed disabled:opacity-60;
  font-weight: var(--partner-font-weight-label);
}

.partner-master-primary-button {
  background-color: var(--partner-color-primary);
  color: var(--partner-color-surface);
}

.partner-master-primary-button:hover {
  background-color: var(--partner-color-primary-hover);
}

.partner-master-secondary-button {
  background-color: var(--partner-color-surface-muted);
  color: var(--partner-color-text-secondary);
}

.partner-master-secondary-button:hover {
  background-color: var(--partner-color-border);
}

.partner-master-danger-button {
  background-color: var(--partner-color-danger);
  color: var(--partner-color-surface);
}

.partner-master-danger-button:hover {
  background-color: var(--partner-color-danger-hover);
}

.partner-master-toolbar {
  @apply flex-wrap border-b px-5 py-4;
  background-color: var(--partner-color-surface-muted);
  border-color: var(--partner-color-border);
}

.partner-master-toolbar-actions,
.partner-master-row-actions,
.partner-master-detail-heading-actions {
  @apply flex items-center gap-2;
}

.partner-master-table-wrap {
  @apply overflow-x-auto;
}

.partner-master-table {
  @apply w-full min-w-[1180px] border-collapse text-left text-xs;
  color: var(--partner-color-text-secondary);
}

.partner-master-table th {
  @apply whitespace-nowrap border-b border-r px-4 py-3 uppercase last:border-r-0;
  background-color: var(--partner-color-surface-muted);
  border-color: var(--partner-color-border);
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-table td {
  @apply whitespace-nowrap border-r px-4 py-3 last:border-r-0;
  border-color: var(--partner-color-table-divider);
}

.partner-master-table tbody tr {
  @apply border-b;
  border-color: var(--partner-color-border);
}

.partner-master-table-row {
  @apply cursor-pointer transition-colors;
}

.partner-master-table-row:hover,
.partner-master-table-row-selected {
  background-color: var(--partner-color-surface-soft);
}

.partner-master-cell-center {
  @apply text-center;
}

.partner-master-cell-right {
  @apply text-right;
}

.partner-master-code,
.partner-master-code-muted,
.partner-master-date {
  @apply font-mono;
}

.partner-master-code,
.partner-master-name {
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-code-muted,
.partner-master-date {
  color: var(--partner-color-text-muted);
}

.partner-master-type-badge {
  @apply inline-flex rounded border px-2.5 py-0.5 text-[10px];
  font-weight: var(--partner-font-weight-strong);
}

.partner-master-type-badge[data-type='SUPPLIER'] {
  background-color: var(--partner-color-primary-soft);
  border-color: var(--partner-color-primary-border);
  color: var(--partner-color-primary);
}

.partner-master-type-badge[data-type='CUSTOMER'] {
  background-color: var(--partner-color-success-soft);
  border-color: var(--partner-color-success-border);
  color: var(--partner-color-success);
}

.partner-master-table-button,
.partner-master-table-danger-button {
  @apply inline-flex items-center gap-1 rounded border px-3 py-1.5 text-[10px] transition-colors;
  font-weight: var(--partner-font-weight-label);
}

.partner-master-table-button {
  background-color: var(--partner-color-primary-soft);
  border-color: var(--partner-color-primary-border);
  color: var(--partner-color-primary);
}

.partner-master-table-button:hover {
  background-color: var(--partner-color-primary-hover-soft);
}

.partner-master-table-danger-button {
  background-color: var(--partner-color-danger-soft);
  border-color: var(--partner-color-danger-border);
  color: var(--partner-color-danger);
}

.partner-master-table-danger-button:hover {
  background-color: var(--partner-color-danger-hover-soft);
}

.partner-master-empty-cell {
  @apply py-12 text-center;
  color: var(--partner-color-text-muted);
}

.partner-master-table tfoot {
  background-color: var(--partner-color-surface-soft);
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-detail-code {
  @apply font-mono text-[11px];
  color: var(--partner-color-text-muted);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-detail-body {
  @apply min-h-[160px] p-6 text-xs;
}

.partner-master-empty-detail {
  @apply flex flex-col items-center justify-center gap-2 py-8 text-center;
  color: var(--partner-color-text-muted);
  font-weight: var(--partner-font-weight-body);
}

.partner-master-empty-icon {
  @apply h-8 w-8;
  color: var(--partner-color-border-strong);
}

.partner-master-detail-tabs {
  @apply mb-5 flex gap-1.5;
}

.partner-master-tab-button {
  @apply rounded-md px-3 py-1.5 text-xs transition-colors;
  color: var(--partner-color-text-muted);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-tab-button:hover {
  background-color: var(--partner-color-surface-muted);
  color: var(--partner-color-text-primary);
}

.partner-master-tab-button-active {
  background-color: var(--partner-color-primary);
  color: var(--partner-color-surface);
}

.partner-master-detail-grid {
  @apply grid grid-cols-1 gap-8 md:grid-cols-2;
}

.partner-master-detail-section {
  @apply space-y-3.5;
}

.partner-master-detail-section h3 {
  @apply border-b pb-2 text-xs;
  border-color: var(--partner-color-border);
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-description-list {
  @apply grid grid-cols-3 gap-x-2 gap-y-2.5;
}

.partner-master-description-list dt {
  color: var(--partner-color-text-muted);
  font-weight: var(--partner-font-weight-body);
}

.partner-master-description-list dd {
  @apply col-span-2;
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-muted);
}

.partner-master-modal {
  @apply fixed inset-0 z-50 overflow-y-auto;
}

.partner-master-modal-backdrop {
  @apply fixed inset-0 backdrop-blur-sm;
  background-color: rgb(15 23 42 / 0.6);
}

.partner-master-modal-shell {
  @apply flex min-h-screen items-center justify-center p-4;
}

.partner-master-modal-card {
  @apply relative w-full max-w-lg overflow-hidden rounded-2xl border shadow-xl;
  background-color: var(--partner-color-surface);
  border-color: var(--partner-color-border);
}

.partner-master-modal-heading {
  @apply border-b px-6 py-4;
  background-color: var(--partner-color-surface-muted);
  border-color: var(--partner-color-border);
}

.partner-master-modal-heading h2 {
  @apply text-sm;
  color: var(--partner-color-text-primary);
  font-weight: var(--partner-font-weight-label);
}

.partner-master-form {
  @apply space-y-4 p-6 text-xs;
}

.partner-master-form-grid {
  @apply grid grid-cols-1 gap-4 sm:grid-cols-2;
}

.partner-master-form-notice {
  @apply rounded-lg border px-4 py-3 text-xs;
  background-color: var(--partner-color-primary-soft);
  border-color: var(--partner-color-primary-border);
  color: var(--partner-color-primary);
  font-weight: var(--partner-font-weight-muted);
}

.partner-master-modal-actions {
  @apply border-t pt-4;
  border-color: var(--partner-color-border);
}

.partner-master-icon-xs {
  @apply h-3 w-3;
}

.partner-master-icon-sm {
  @apply h-4 w-4;
}

.partner-master-icon-md {
  @apply h-6 w-6;
}

.partner-master-chevron {
  @apply transition-transform duration-200;
}

.partner-master-chevron-collapsed {
  @apply rotate-180;
}

.partner-master-spinner,
.partner-master-spinner-inline {
  @apply animate-spin;
  color: var(--partner-color-primary);
}

.partner-master-spinner {
  @apply mr-2 inline h-5 w-5;
}

.partner-master-spinner-inline {
  @apply h-3.5 w-3.5;
  color: currentColor;
}

@keyframes partnerMasterFadeIn {
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

<style scoped>
.partner-master-suggestion-list {
  position: absolute;
  left: 0;
  right: 0;
  top: 4.8rem;
  z-index: 40;
  max-height: 20rem;
  overflow-y: auto;
  border: 1px solid var(--partner-color-border);
  border-radius: 0.9rem;
  background: var(--partner-color-surface);
  box-shadow: var(--partner-shadow-panel);
  padding: 0.4rem;
}

.partner-master-suggestion-item {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 0.2rem;
  border-radius: 0.7rem;
  padding: 0.75rem 0.9rem;
  text-align: left;
  color: var(--partner-color-text-primary);
}

.partner-master-suggestion-item:hover {
  background: var(--partner-color-surface-muted);
}

.partner-master-suggestion-item span {
  font-size: 0.85rem;
  font-weight: var(--partner-font-weight-strong);
}

.partner-master-suggestion-item small {
  color: var(--partner-color-text-muted);
  font-size: 0.72rem;
  font-weight: var(--partner-font-weight-body);
}

.partner-master-sort-field {
  min-width: 14rem;
}
</style>
