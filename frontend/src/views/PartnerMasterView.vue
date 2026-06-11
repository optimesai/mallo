<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Building2,
  ChevronDown,
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
import type { PartnerMasterRequest, PartnerMasterResponse, PartnerType } from '@/api/partnerMasterApi'

const partnerMasterStore = usePartnerMasterStore()

const partnerTypeOptions: Array<{ value: PartnerType; label: string; description: string }> = [
  { value: 'SUPPLIER', label: '공급사', description: '입고 등록에서 납품 공급처로 사용됩니다.' },
  { value: 'CUSTOMER', label: '고객사', description: '출하 지시에서 출하 대상 고객사로 사용됩니다.' }
]

const isSearchExpanded = ref(true)
const filterKeyword = ref('')
const filterPartnerType = ref<'ALL' | PartnerType>('ALL')
const activeDetailTab = ref<'profile' | 'system'>('profile')
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)

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
  contactPhone: ''
})

const selectedPartner = computed(() => partnerMasterStore.selectedPartner)

const stats = computed(() => {
  const partners = partnerMasterStore.partners
  return {
    total: partners.length,
    supplier: partners.filter((partner) => partner.partnerType === 'SUPPLIER').length,
    customer: partners.filter((partner) => partner.partnerType === 'CUSTOMER').length,
    withBusinessNo: partners.filter((partner) => Boolean(partner.businessNo)).length
  }
})

const selectedTypeOption = computed(() => {
  if (!selectedPartner.value) return null
  return partnerTypeOptions.find((option) => option.value === selectedPartner.value?.partnerType) || null
})

onMounted(async () => {
  await fetchPartners()
})

async function fetchPartners() {
  try {
    pageError.value = null
    await partnerMasterStore.loadPartners({
      partnerType: filterPartnerType.value === 'ALL' ? undefined : filterPartnerType.value,
      keyword: filterKeyword.value.trim() || undefined
    })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 목록을 불러오지 못했습니다.'
  }
}

function resetFilters() {
  filterKeyword.value = ''
  filterPartnerType.value = 'ALL'
}

async function handleSearch() {
  partnerMasterStore.selectPartner(null)
  await fetchPartners()
}

function selectRow(partner: PartnerMasterResponse) {
  if (selectedPartner.value?.partnerId === partner.partnerId) {
    partnerMasterStore.selectPartner(null)
    return
  }
  partnerMasterStore.selectPartner(partner)
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
      await partnerMasterStore.createPartner(payload)
      showToast('신규 거래처 마스터가 등록되었습니다.')
    } else if (editingPartnerId.value !== null) {
      await partnerMasterStore.updatePartner(editingPartnerId.value, payload)
      showToast('거래처 마스터 정보가 수정되었습니다.')
    }
    closeForm()
    await fetchPartners()
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
    contactPhone: normalizeOptionalText(form.contactPhone)
  }
}

function normalizeOptionalText(value: string | null | undefined) {
  const trimmedValue = value?.trim() || ''
  return trimmedValue.length > 0 ? trimmedValue : null
}

function validateForm(payload: PartnerMasterRequest) {
  if (!payload.partnerCode) return '거래처 코드를 입력해주세요.'
  if (payload.partnerCode.length > 50) return '거래처 코드는 50자 이하여야 합니다.'
  if (!payload.partnerName) return '거래처명을 입력해주세요.'
  if (payload.partnerName.length > 100) return '거래처명은 100자 이하여야 합니다.'
  if (payload.businessNo && payload.businessNo.length > 50) return '사업자등록번호는 50자 이하여야 합니다.'
  if (payload.representative && payload.representative.length > 50) return '대표자명은 50자 이하여야 합니다.'
  if (payload.contactPhone && payload.contactPhone.length > 50) return '담당자 연락처는 50자 이하여야 합니다.'
  return null
}

async function requestDelete(partner: PartnerMasterResponse) {
  if (!confirm(`[${partner.partnerCode}] ${partner.partnerName} 거래처를 삭제하시겠습니까?`)) return

  try {
    pageError.value = null
    await partnerMasterStore.deletePartner(partner.partnerId)
    showToast('거래처 마스터가 삭제되었습니다.')
    await fetchPartners()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 삭제에 실패했습니다.'
  }
}

function getPartnerTypeLabel(partnerType: PartnerType) {
  return partnerTypeOptions.find((option) => option.value === partnerType)?.label || partnerType
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
  <div class="partner-master-page">
    <div class="partner-master-header">
      <div>
        <h1 class="partner-master-title">
          <span class="partner-master-title-mark"></span>
          거래처 마스터 관리
        </h1>
        <p class="partner-master-subtitle">입고 공급사와 출하 고객사의 기준정보를 등록하고 업무 화면에서 공통으로 사용하는 거래처 원장을 관리합니다.</p>
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
        <div class="partner-master-stat-icon partner-master-stat-icon-neutral">
          <Building2 class="partner-master-icon-md" />
        </div>
        <div>
          <p class="partner-master-stat-label">전체 거래처</p>
          <p class="partner-master-stat-value">{{ stats.total }} 사</p>
        </div>
      </div>
      <div class="partner-master-stat-card">
        <div class="partner-master-stat-icon partner-master-stat-icon-primary">
          <Truck class="partner-master-icon-md" />
        </div>
        <div>
          <p class="partner-master-stat-label">공급사 / 고객사</p>
          <p class="partner-master-stat-value">{{ stats.supplier }} / {{ stats.customer }} 사</p>
        </div>
      </div>
      <div class="partner-master-stat-card">
        <div class="partner-master-stat-icon partner-master-stat-icon-success">
          <FileSpreadsheet class="partner-master-icon-md" />
        </div>
        <div>
          <p class="partner-master-stat-label">사업자번호 등록</p>
          <p class="partner-master-stat-value">{{ stats.withBusinessNo }} 사</p>
        </div>
      </div>
    </div>

    <section class="partner-master-panel">
      <div class="partner-master-panel-heading">
        <span class="partner-master-panel-title">
          <Search class="partner-master-icon-sm" />
          조회 검색 조건
        </span>
        <button class="partner-master-icon-button" type="button" @click="isSearchExpanded = !isSearchExpanded">
          <ChevronDown class="partner-master-icon-sm partner-master-chevron" :class="{ 'partner-master-chevron-collapsed': !isSearchExpanded }" />
        </button>
      </div>

      <div v-show="isSearchExpanded" class="partner-master-search-grid">
        <div class="partner-master-field">
          <label class="partner-master-label" for="partner-keyword">통합 검색</label>
          <input
            id="partner-keyword"
            v-model="filterKeyword"
            class="partner-master-input"
            placeholder="거래처 ID, 코드, 거래처명, 사업자등록번호"
            @keyup.enter="handleSearch"
          >
        </div>
        <div class="partner-master-field">
          <label class="partner-master-label" for="partner-type-filter">거래처 구분</label>
          <select id="partner-type-filter" v-model="filterPartnerType" class="partner-master-input">
            <option value="ALL">전체 거래처</option>
            <option v-for="option in partnerTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }} ({{ option.value }})
            </option>
          </select>
        </div>
        <div class="partner-master-search-actions">
          <button class="partner-master-secondary-button" type="button" @click="resetFilters">초기화</button>
          <button class="partner-master-primary-button" type="button" @click="handleSearch">조회</button>
        </div>
      </div>
    </section>

    <section class="partner-master-panel">
      <div class="partner-master-toolbar">
        <div></div>
        <div class="partner-master-toolbar-actions">
          <button class="partner-master-secondary-button" type="button" @click="fetchPartners">
            <RefreshCw class="partner-master-icon-sm" />
            새로고침
          </button>
          <button class="partner-master-primary-button" type="button" @click="openCreateForm">
            <Plus class="partner-master-icon-sm" />
            신규 거래처 등록
          </button>
        </div>
      </div>

      <div class="partner-master-table-wrap">
        <table class="partner-master-table">
          <thead>
            <tr>
              <th class="partner-master-cell-center">No</th>
              <th>거래처코드</th>
              <th>거래처명</th>
              <th class="partner-master-cell-center">구분</th>
              <th>사업자등록번호</th>
              <th>대표자</th>
              <th>담당자 연락처</th>
              <th class="partner-master-cell-center">등록일시</th>
              <th class="partner-master-cell-center">액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="partnerMasterStore.isLoading">
              <td colspan="9" class="partner-master-empty-cell">
                <Loader2 class="partner-master-spinner" />
                거래처 데이터를 가져오고 있습니다...
              </td>
            </tr>
            <tr v-else-if="partnerMasterStore.partners.length === 0">
              <td colspan="9" class="partner-master-empty-cell">조회된 거래처 마스터가 없습니다.</td>
            </tr>
            <tr
              v-for="(partner, index) in partnerMasterStore.partners"
              v-else
              :key="partner.partnerId"
              class="partner-master-table-row"
              :class="{ 'partner-master-table-row-selected': selectedPartner?.partnerId === partner.partnerId }"
              @click="selectRow(partner)"
            >
              <td class="partner-master-cell-center">{{ index + 1 }}</td>
              <td class="partner-master-code">{{ partner.partnerCode }}</td>
              <td class="partner-master-name">{{ partner.partnerName }}</td>
              <td class="partner-master-cell-center">
                <span class="partner-master-type-badge" :data-type="partner.partnerType">
                  {{ getPartnerTypeLabel(partner.partnerType) }}
                </span>
              </td>
              <td class="partner-master-code-muted">{{ partner.businessNo || '미등록' }}</td>
              <td>{{ partner.representative || '-' }}</td>
              <td class="partner-master-code-muted">{{ partner.contactPhone || '-' }}</td>
              <td class="partner-master-cell-center partner-master-date">{{ formatDateTime(partner.createdAt) }}</td>
              <td class="partner-master-cell-center" @click.stop>
                <div class="partner-master-row-actions">
                  <button class="partner-master-table-button" type="button" @click="openEditForm(partner)">
                    <Edit3 class="partner-master-icon-xs" />
                    수정
                  </button>
                  <button class="partner-master-table-danger-button" type="button" @click="requestDelete(partner)">
                    <Trash2 class="partner-master-icon-xs" />
                    삭제
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <td colspan="4">현재 조회 목록: {{ partnerMasterStore.partners.length }}건</td>
              <td colspan="5" class="partner-master-cell-right">공급사 {{ stats.supplier }}건 / 고객사 {{ stats.customer }}건</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>

    <section class="partner-master-panel">
      <div class="partner-master-detail-heading">
        <div class="partner-master-detail-title">
          <FileSpreadsheet class="partner-master-icon-sm" />
          선택 거래처 세부 정보
        </div>
        <div v-if="selectedPartner" class="partner-master-detail-heading-actions">
          <div class="partner-master-detail-code">PARTNER-ID {{ selectedPartner.partnerId }}</div>
          <button class="partner-master-table-button" type="button" @click="openEditForm(selectedPartner)">
            <Edit3 class="partner-master-icon-xs" />
            수정
          </button>
          <button class="partner-master-table-danger-button" type="button" @click="requestDelete(selectedPartner)">
            <Trash2 class="partner-master-icon-xs" />
            삭제
          </button>
        </div>
      </div>

      <div class="partner-master-detail-body">
        <div v-if="!selectedPartner" class="partner-master-empty-detail">
          <FileSpreadsheet class="partner-master-empty-icon" />
          <span>상단 거래처 목록에서 행을 선택하면 기준정보 상세 속성이 표시됩니다.</span>
        </div>
        <template v-else>
          <div class="partner-master-detail-tabs">
            <button
              type="button"
              class="partner-master-tab-button"
              :class="{ 'partner-master-tab-button-active': activeDetailTab === 'profile' }"
              @click="activeDetailTab = 'profile'"
            >
              거래처 프로필
            </button>
            <button
              type="button"
              class="partner-master-tab-button"
              :class="{ 'partner-master-tab-button-active': activeDetailTab === 'system' }"
              @click="activeDetailTab = 'system'"
            >
              업무 연계
            </button>
          </div>

          <div v-if="activeDetailTab === 'profile'" class="partner-master-detail-grid">
            <div class="partner-master-detail-section">
              <h3>기본 식별 정보</h3>
              <dl class="partner-master-description-list">
                <dt>거래처 코드</dt>
                <dd class="partner-master-code">{{ selectedPartner.partnerCode }}</dd>
                <dt>거래처명</dt>
                <dd>{{ selectedPartner.partnerName }}</dd>
                <dt>거래처 구분</dt>
                <dd>{{ selectedTypeOption?.label }} ({{ selectedPartner.partnerType }})</dd>
              </dl>
            </div>
            <div class="partner-master-detail-section">
              <h3>사업자 및 연락처</h3>
              <dl class="partner-master-description-list">
                <dt>사업자등록번호</dt>
                <dd class="partner-master-code">{{ selectedPartner.businessNo || '미등록' }}</dd>
                <dt>대표자</dt>
                <dd>{{ selectedPartner.representative || '-' }}</dd>
                <dt>담당자 연락처</dt>
                <dd class="partner-master-code">{{ selectedPartner.contactPhone || '-' }}</dd>
              </dl>
            </div>
          </div>

          <div v-else class="partner-master-detail-grid">
            <div class="partner-master-detail-section">
              <h3>업무 사용 기준</h3>
              <dl class="partner-master-description-list">
                <dt>사용 업무</dt>
                <dd>{{ selectedPartner.partnerType === 'SUPPLIER' ? '입고 등록 공급사' : '출하 지시 고객사' }}</dd>
                <dt>설명</dt>
                <dd>{{ selectedTypeOption?.description }}</dd>
                <dt>등록일시</dt>
                <dd>{{ formatDateTime(selectedPartner.createdAt) }}</dd>
              </dl>
            </div>
          </div>
        </template>
      </div>
    </section>

    <div v-if="isFormOpen" class="partner-master-modal" role="dialog" aria-modal="true">
      <div class="partner-master-modal-backdrop" @click="closeForm"></div>
      <div class="partner-master-modal-shell">
        <div class="partner-master-modal-card">
          <div class="partner-master-modal-heading">
            <h2>{{ formMode === 'create' ? '신규 거래처 마스터 등록' : '거래처 마스터 수정' }}</h2>
            <button class="partner-master-icon-button" type="button" @click="closeForm">
              <X class="partner-master-icon-sm" />
            </button>
          </div>

          <form class="partner-master-form" @submit.prevent="submitForm">
            <div v-if="formError" class="partner-master-form-error">{{ formError }}</div>

            <div class="partner-master-form-notice">
              거래처 코드는 입고/출하 업무에서 조회 키로 사용되므로 중복 없이 관리해야 합니다.
            </div>

            <div class="partner-master-form-grid">
              <div class="partner-master-field">
                <label class="partner-master-label" for="partner-code">거래처 코드 *</label>
                <input id="partner-code" v-model="form.partnerCode" class="partner-master-input" maxlength="50" placeholder="예: SUP-POSCO-01" required>
              </div>
              <div class="partner-master-field">
                <label class="partner-master-label" for="partner-type">거래처 구분 *</label>
                <select id="partner-type" v-model="form.partnerType" class="partner-master-input" required>
                  <option v-for="option in partnerTypeOptions" :key="option.value" :value="option.value">
                    {{ option.label }} ({{ option.value }})
                  </option>
                </select>
              </div>
            </div>

            <div class="partner-master-field">
              <label class="partner-master-label" for="partner-name">거래처명 *</label>
              <input id="partner-name" v-model="form.partnerName" class="partner-master-input" maxlength="100" placeholder="예: (주)포스코 인터내셔널" required>
            </div>

            <div class="partner-master-field">
              <label class="partner-master-label" for="partner-business-no">사업자등록번호</label>
              <input id="partner-business-no" v-model="form.businessNo" class="partner-master-input" maxlength="50" placeholder="예: 123-45-67890">
            </div>

            <div class="partner-master-form-grid">
              <div class="partner-master-field">
                <label class="partner-master-label" for="partner-representative">대표자명</label>
                <input id="partner-representative" v-model="form.representative" class="partner-master-input" maxlength="50" placeholder="예: 홍길동">
              </div>
              <div class="partner-master-field">
                <label class="partner-master-label" for="partner-contact-phone">담당자 연락처</label>
                <input id="partner-contact-phone" v-model="form.contactPhone" class="partner-master-input" maxlength="50" placeholder="예: 02-3457-1114">
              </div>
            </div>

            <div class="partner-master-modal-actions">
              <button class="partner-master-secondary-button" type="button" @click="closeForm">취소</button>
              <button class="partner-master-primary-button" type="submit" :disabled="partnerMasterStore.isSaving">
                <Loader2 v-if="partnerMasterStore.isSaving" class="partner-master-spinner-inline" />
                {{ formMode === 'create' ? '거래처 등록' : '수정 저장' }}
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
