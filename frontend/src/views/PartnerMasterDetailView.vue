<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Copy, Edit3, Trash2, X } from '@lucide/vue'
import { usePartnerMasterStore } from '@/state/partnerMasterStore'
import type { PartnerMasterRequest, PartnerMasterResponse, PartnerStatus, PartnerType } from '@/api/partnerMasterApi'

const route = useRoute()
const router = useRouter()
const partnerMasterStore = usePartnerMasterStore()

const partnerTypeOptions: Array<{ value: PartnerType; label: string; description: string }> = [
  { value: 'SUPPLIER', label: '공급사', description: '입고 등록에서 납품 공급처로 사용됩니다.' },
  { value: 'CUSTOMER', label: '고객사', description: '출하 지시에서 출하 대상 고객사로 사용됩니다.' }
]

const partnerId = computed(() => Number(route.params.id))
const partner = computed(() => partnerMasterStore.selectedPartner)
const usage = computed(() => partnerMasterStore.selectedUsage)
const activeTab = ref<'profile' | 'system' | 'history'>('profile')
const pageError = ref<string | null>(null)
const toast = ref<string | null>(null)
const isFormOpen = ref(false)
const isDeleteOpen = ref(false)
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

const referenceItems = computed(() => {
  const currentUsage = usage.value
  if (!currentUsage) return []
  return [
    { label: '입고 참조', count: currentUsage.inboundCount },
    { label: '출하 참조', count: currentUsage.shippingCount }
  ].filter((item) => item.count > 0)
})

onMounted(() => {
  loadDetail()
})

async function loadDetail() {
  try {
    pageError.value = null
    const cachedPartner = partner.value?.partnerId === partnerId.value ? partner.value : null
    const partners = await partnerMasterStore.searchPartners(String(partnerId.value))
    const found = partners.find((item) => item.partnerId === partnerId.value) || partners[0]
    if (!found) throw new Error('거래처 정보를 찾을 수 없습니다.')
    partnerMasterStore.selectPartner({
      ...found,
      note: found.note ?? cachedPartner?.note ?? null
    })
    await loadPartnerContext(found)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 상세 정보를 불러오지 못했습니다.'
  }
}

async function loadPartnerContext(selected: PartnerMasterResponse) {
  await partnerMasterStore.loadPartnerUsage(selected.partnerId)
  await loadPartnerHistory(selected)
}

async function loadPartnerHistory(selected: PartnerMasterResponse) {
  try {
    if (selected.partnerType === 'SUPPLIER') {
      await partnerMasterStore.loadSuppliedItems(selected.partnerId)
    } else {
      await partnerMasterStore.loadShippedItems(selected.partnerId)
    }
  } catch (err) {
    if (selected.partnerType === 'SUPPLIER') {
      partnerMasterStore.suppliedItems = []
    } else {
      partnerMasterStore.shippedItems = []
    }
  }
}

function openEditForm() {
  if (!partner.value) return
  form.partnerCode = partner.value.partnerCode
  form.partnerName = partner.value.partnerName
  form.partnerType = partner.value.partnerType
  form.businessNo = partner.value.businessNo || ''
  form.representative = partner.value.representative || ''
  form.contactPhone = partner.value.contactPhone || ''
  form.contactEmail = partner.value.contactEmail || ''
  form.note = partner.value.note || ''
  formError.value = null
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  formError.value = null
}

async function submitForm() {
  if (!partner.value) return
  const payload = normalizeForm()
  const validationError = validateForm(payload)
  if (validationError) {
    formError.value = validationError
    return
  }

  try {
    formError.value = null
    await partnerMasterStore.updatePartner(partner.value.partnerId, payload)
    showToast('거래처 마스터 정보가 수정되었습니다.')
    closeForm()
    await loadDetail()
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
  if (!payload.partnerName) return '거래처명을 입력해주세요.'
  if (payload.contactPhone && !/^[0-9+()\-\s]{7,50}$/.test(payload.contactPhone)) return '담당자 연락처 형식이 올바르지 않습니다.'
  if (payload.contactEmail && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(payload.contactEmail)) return '담당자 이메일 형식이 올바르지 않습니다.'
  if (payload.note && payload.note.length > 1000) return '비고는 1000자 이하여야 합니다.'
  return null
}

async function updateStatus(nextStatus: PartnerStatus) {
  if (!partner.value) return
  try {
    pageError.value = null
    await partnerMasterStore.updatePartnerStatus(partner.value.partnerId, nextStatus)
    showToast(nextStatus === 'ACTIVE' ? '거래처가 활성화되었습니다.' : '거래처가 비활성화되었습니다.')
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 상태 변경에 실패했습니다.'
  }
}

async function requestDelete() {
  if (!partner.value) return
  try {
    pageError.value = null
    await partnerMasterStore.loadPartnerUsage(partner.value.partnerId)
    isDeleteOpen.value = true
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 사용 현황을 불러오지 못했습니다.'
  }
}

async function deletePartner() {
  if (!partner.value) return
  if (!confirm(`[${partner.value.partnerCode}] ${partner.value.partnerName} 거래처를 삭제하시겠습니까?`)) return

  try {
    await partnerMasterStore.deletePartner(partner.value.partnerId)
    showToast('거래처 마스터가 삭제되었습니다.')
    await router.push({ name: 'partner-master' })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 삭제에 실패했습니다.'
  }
}

async function deactivateFromDeleteModal() {
  if (!partner.value) return
  if (partner.value.partnerStatus === 'INACTIVE') {
    isDeleteOpen.value = false
    return
  }
  try {
    await partnerMasterStore.updatePartnerStatus(partner.value.partnerId, 'INACTIVE')
    showToast('참조 중인 거래처를 비활성화했습니다.')
    isDeleteOpen.value = false
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '거래처 상태 변경에 실패했습니다.'
  }
}

async function copyText(value: string | null, label: string) {
  if (!value) return
  await navigator.clipboard.writeText(value)
  showToast(`${label}이 복사되었습니다.`)
}

function getPartnerTypeLabel(partnerType?: PartnerType) {
  return partnerTypeOptions.find((option) => option.value === partnerType)?.label || partnerType || '-'
}

function getPartnerTypeDescription(partnerType?: PartnerType) {
  return partnerTypeOptions.find((option) => option.value === partnerType)?.description || '-'
}

function formatDateTime(dateTimeStr?: string | null) {
  if (!dateTimeStr) return '-'
  return new Date(dateTimeStr).toLocaleString('ko-KR')
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
        <button class="mb-5 inline-flex items-center gap-2 rounded-xl border app-border px-4 py-2 text-sm app-font-emphasis app-text-soft transition app-hover-muted" type="button" @click="router.push({ name: 'partner-master' })">
          <ArrowLeft class="h-4 w-4" />
          목록으로
        </button>
        <h1 class="text-2xl app-font-emphasis tracking-tight app-text-strong">{{ partner?.partnerName || '거래처 상세' }}</h1>
        <p class="mt-1 font-mono text-sm app-font-strong app-text-muted">{{ partner?.partnerCode || '-' }}</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <div v-if="toast" class="rounded-xl border app-border app-bg-success-soft px-4 py-2 text-sm app-font-strong app-text-success">{{ toast }}</div>
        <button v-if="partner" class="rounded-xl app-bg-strong px-4 py-2 text-sm app-font-emphasis app-text-inverse" type="button" @click="openEditForm">수정</button>
        <button v-if="partner" class="rounded-xl app-bg-warning px-4 py-2 text-sm app-font-emphasis app-text-inverse" type="button" @click="updateStatus(partner.partnerStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')">
          {{ partner.partnerStatus === 'ACTIVE' ? '비활성화' : '활성화' }}
        </button>
        <button v-if="partner" class="rounded-xl app-bg-danger px-4 py-2 text-sm app-font-emphasis app-text-inverse" type="button" @click="requestDelete">삭제</button>
      </div>
    </div>

    <div v-if="pageError" class="rounded-2xl border app-border app-bg-danger-soft p-4 text-sm app-font-strong app-text-danger">{{ pageError }}</div>

    <div v-if="partner" class="grid grid-cols-1 gap-4 md:grid-cols-4">
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">거래처 구분</p>
        <strong class="mt-2 block text-xl app-text-strong">{{ getPartnerTypeLabel(partner.partnerType) }}</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">상태</p>
        <strong class="mt-2 block text-xl" :class="partner.partnerStatus === 'ACTIVE' ? 'app-text-success' : 'app-text-warning'">{{ partner.partnerStatus === 'ACTIVE' ? '활성' : '비활성' }}</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">사용 건수</p>
        <strong class="mt-2 block text-xl app-text-strong">{{ partner.usageCount.toLocaleString() }}건</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">최근 거래</p>
        <strong class="mt-2 block text-sm app-text-strong">{{ formatDateTime(partner.lastUsedAt) }}</strong>
      </div>
    </div>

    <div v-if="partner" class="rounded-3xl border app-border app-bg-surface shadow-sm">
      <div class="flex flex-wrap gap-2 border-b app-border-muted p-4">
        <button class="rounded-2xl px-4 py-2 text-sm app-font-emphasis" :class="activeTab === 'profile' ? 'app-bg-strong app-text-inverse' : 'app-text-soft app-hover-muted'" type="button" @click="activeTab = 'profile'">기본 정보</button>
        <button class="rounded-2xl px-4 py-2 text-sm app-font-emphasis" :class="activeTab === 'system' ? 'app-bg-strong app-text-inverse' : 'app-text-soft app-hover-muted'" type="button" @click="activeTab = 'system'">사용 현황</button>
        <button class="rounded-2xl px-4 py-2 text-sm app-font-emphasis" :class="activeTab === 'history' ? 'app-bg-strong app-text-inverse' : 'app-text-soft app-hover-muted'" type="button" @click="activeTab = 'history'">
          {{ partner.partnerType === 'SUPPLIER' ? '공급 이력' : '출하 이력' }}
        </button>
      </div>

      <div v-if="activeTab === 'profile'" class="grid gap-4 p-5 lg:grid-cols-3">
        <section class="rounded-2xl border app-border-muted p-4">
          <h2 class="text-sm app-font-emphasis app-text-strong">기본 식별 정보</h2>
          <dl class="mt-4 space-y-3 text-sm">
            <div><dt class="app-text-muted">거래처 코드</dt><dd class="mt-1 font-mono app-font-emphasis app-text-strong">{{ partner.partnerCode }}</dd></div>
            <div><dt class="app-text-muted">거래처명</dt><dd class="mt-1 app-font-strong app-text-strong">{{ partner.partnerName }}</dd></div>
            <div><dt class="app-text-muted">거래처 구분</dt><dd class="mt-1 app-text-soft">{{ getPartnerTypeLabel(partner.partnerType) }}</dd></div>
          </dl>
        </section>
        <section class="rounded-2xl border app-border-muted p-4">
          <h2 class="text-sm app-font-emphasis app-text-strong">사업자 및 연락처</h2>
          <dl class="mt-4 space-y-3 text-sm">
            <div><dt class="app-text-muted">사업자등록번호</dt><dd class="mt-1 font-mono app-text-soft">{{ partner.businessNo || '미등록' }}</dd></div>
            <div><dt class="app-text-muted">대표자</dt><dd class="mt-1 app-text-soft">{{ partner.representative || '-' }}</dd></div>
            <div>
              <dt class="app-text-muted">담당자 연락처</dt>
              <dd class="mt-1 flex items-center gap-2 app-text-soft">
                {{ partner.contactPhone || '-' }}
                <button v-if="partner.contactPhone" class="rounded-lg border app-border p-1 app-hover-muted" type="button" @click="copyText(partner.contactPhone, '담당자 연락처')"><Copy class="h-3.5 w-3.5" /></button>
              </dd>
            </div>
            <div>
              <dt class="app-text-muted">담당자 이메일</dt>
              <dd class="mt-1 flex items-center gap-2 app-text-soft">
                {{ partner.contactEmail || '-' }}
                <button v-if="partner.contactEmail" class="rounded-lg border app-border p-1 app-hover-muted" type="button" @click="copyText(partner.contactEmail, '담당자 이메일')"><Copy class="h-3.5 w-3.5" /></button>
              </dd>
            </div>
          </dl>
        </section>
        <section class="rounded-2xl border app-border-muted p-4">
          <h2 class="text-sm app-font-emphasis app-text-strong">비고</h2>
          <p class="mt-4 whitespace-pre-line text-sm app-text-muted">{{ partner.note || '등록된 비고가 없습니다.' }}</p>
        </section>
      </div>

      <div v-else-if="activeTab === 'system'" class="grid gap-4 p-5 lg:grid-cols-2">
        <section class="rounded-2xl border app-border-muted p-4">
          <h2 class="text-sm app-font-emphasis app-text-strong">사용 현황</h2>
          <dl class="mt-4 space-y-3 text-sm">
            <div><dt class="app-text-muted">입고 참조</dt><dd class="mt-1 app-font-strong app-text-strong">{{ usage?.inboundCount ?? partner.inboundCount }}건</dd></div>
            <div><dt class="app-text-muted">출하 참조</dt><dd class="mt-1 app-font-strong app-text-strong">{{ usage?.shippingCount ?? partner.shippingCount }}건</dd></div>
            <div><dt class="app-text-muted">최근 거래 일시</dt><dd class="mt-1 app-text-soft">{{ formatDateTime(usage?.lastUsedAt || partner.lastUsedAt) }}</dd></div>
            <div><dt class="app-text-muted">삭제 가능 여부</dt><dd class="mt-1 app-text-soft">{{ usage?.canDelete ? '삭제 가능' : (usage?.deleteBlockedReason || '참조 현황 확인 필요') }}</dd></div>
          </dl>
        </section>
        <section class="rounded-2xl border app-border-muted p-4">
          <h2 class="text-sm app-font-emphasis app-text-strong">업무 연계</h2>
          <p class="mt-4 text-sm app-text-muted">{{ getPartnerTypeDescription(partner.partnerType) }}</p>
          <p class="mt-2 text-sm app-text-muted">{{ partner.partnerType === 'SUPPLIER' ? '입고 등록 화면의 공급사 선택 목록에서 활성 거래처만 사용할 수 있습니다.' : '출하 지시 화면의 고객사 선택 목록에서 활성 거래처만 사용할 수 있습니다.' }}</p>
        </section>
      </div>

      <div v-else class="overflow-x-auto p-5">
        <table class="w-full min-w-[760px] text-left text-sm">
          <thead class="app-bg-muted text-xs uppercase tracking-widest app-text-muted">
            <tr>
              <th class="px-4 py-3">품목 코드</th>
              <th class="px-4 py-3">품목명</th>
              <th class="px-4 py-3">구분</th>
              <th class="px-4 py-3 text-right">누적 수량</th>
              <th class="px-4 py-3 text-right">건수</th>
              <th class="px-4 py-3">최근 일시</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="partner.partnerType === 'SUPPLIER' && partnerMasterStore.isSuppliedItemsLoading">
              <td colspan="6" class="px-4 py-8 text-center app-font-strong app-text-muted">공급 이력을 불러오고 있습니다.</td>
            </tr>
            <tr v-else-if="partner.partnerType === 'CUSTOMER' && partnerMasterStore.isShippedItemsLoading">
              <td colspan="6" class="px-4 py-8 text-center app-font-strong app-text-muted">출하 이력을 불러오고 있습니다.</td>
            </tr>
            <tr v-else-if="partner.partnerType === 'SUPPLIER' && partnerMasterStore.suppliedItems.length === 0">
              <td colspan="6" class="px-4 py-8 text-center app-font-strong app-text-muted">공급 이력이 없습니다.</td>
            </tr>
            <tr v-else-if="partner.partnerType === 'CUSTOMER' && partnerMasterStore.shippedItems.length === 0">
              <td colspan="6" class="px-4 py-8 text-center app-font-strong app-text-muted">출하 이력이 없습니다.</td>
            </tr>
            <tr v-for="item in partner.partnerType === 'SUPPLIER' ? partnerMasterStore.suppliedItems : partnerMasterStore.shippedItems" v-else :key="item.itemCode" class="border-t app-border-muted">
              <td class="px-4 py-3 font-mono app-font-emphasis app-text-strong">{{ item.itemCode }}</td>
              <td class="px-4 py-3 app-font-strong app-text-strong">{{ item.itemName }}</td>
              <td class="px-4 py-3 app-text-muted">{{ item.itemType }}</td>
              <td class="px-4 py-3 text-right app-text-soft">{{ 'totalInboundQty' in item ? item.totalInboundQty : item.totalShippingQty }} {{ item.unit }}</td>
              <td class="px-4 py-3 text-right app-text-soft">{{ 'inboundCount' in item ? item.inboundCount : item.shippingCount }}건</td>
              <td class="px-4 py-3 app-text-muted">{{ formatDateTime('lastInboundDate' in item ? item.lastInboundDate : item.lastShippingAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="isFormOpen" class="fixed inset-0 z-50 flex items-center justify-center app-backdrop p-4">
      <div class="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-3xl app-bg-surface p-6 shadow-2xl">
        <div class="mb-5 flex items-start justify-between gap-4 border-b app-border-muted pb-4">
          <div>
            <h2 class="text-lg app-font-emphasis app-text-strong">거래처 수정</h2>
            <p class="mt-1 text-sm app-font-label app-text-muted">거래처 코드는 수정할 수 없습니다.</p>
          </div>
          <button class="rounded-full p-2 app-hover-muted" type="button" @click="closeForm"><X class="h-5 w-5" /></button>
        </div>

        <form class="grid gap-4 md:grid-cols-2" @submit.prevent="submitForm">
          <div v-if="formError" class="md:col-span-2 rounded-2xl border app-border app-bg-danger-soft p-3 text-sm app-font-strong app-text-danger">{{ formError }}</div>
          <label class="block text-sm app-font-emphasis app-text-soft">
            거래처 구분
            <select v-model="form.partnerType" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
              <option v-for="option in partnerTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
          <label class="block text-sm app-font-emphasis app-text-soft">
            거래처 코드
            <input v-model="form.partnerCode" disabled class="mt-1 h-11 w-full rounded-2xl border app-border bg-slate-100 px-4 font-mono text-sm outline-none">
          </label>
          <label class="block text-sm app-font-emphasis app-text-soft">
            거래처명
            <input v-model="form.partnerName" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
          </label>
          <label class="block text-sm app-font-emphasis app-text-soft">
            사업자등록번호
            <input v-model="form.businessNo" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
          </label>
          <label class="block text-sm app-font-emphasis app-text-soft">
            대표자명
            <input v-model="form.representative" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
          </label>
          <label class="block text-sm app-font-emphasis app-text-soft">
            담당자 연락처
            <input v-model="form.contactPhone" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
          </label>
          <label class="md:col-span-2 block text-sm app-font-emphasis app-text-soft">
            담당자 이메일
            <input v-model="form.contactEmail" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
          </label>
          <label class="md:col-span-2 block text-sm app-font-emphasis app-text-soft">
            비고
            <textarea v-model="form.note" rows="4" class="mt-1 w-full resize-none rounded-2xl border app-border px-4 py-3 text-sm outline-none"></textarea>
          </label>
          <div class="md:col-span-2 flex justify-end gap-2 border-t app-border-muted pt-4">
            <button class="rounded-2xl app-bg-muted px-5 py-2.5 text-sm app-font-emphasis" type="button" @click="closeForm">취소</button>
            <button class="rounded-2xl app-accent-bg px-5 py-2.5 text-sm app-font-emphasis app-text-inverse" type="submit" :disabled="partnerMasterStore.isSaving">저장</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="isDeleteOpen" class="fixed inset-0 z-[60] flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-xl rounded-3xl app-bg-surface p-6 shadow-2xl">
        <h2 class="text-lg app-font-emphasis app-text-strong">거래처 삭제 확인</h2>
        <div class="mt-4 rounded-2xl border app-border-muted p-4">
          <p class="mb-2 text-sm app-font-emphasis app-text-strong">참조 항목</p>
          <ul v-if="referenceItems.length > 0" class="space-y-1 text-sm app-font-strong app-text-soft">
            <li v-for="refItem in referenceItems" :key="refItem.label">- {{ refItem.label }} {{ refItem.count.toLocaleString() }}건</li>
          </ul>
          <p v-else class="text-sm app-font-strong app-text-muted">참조 중인 항목이 없습니다.</p>
        </div>
        <p v-if="usage && !usage.canDelete" class="mt-4 rounded-2xl app-bg-warning-soft p-4 text-sm app-font-strong app-text-warning">
          참조 중인 거래처는 삭제할 수 없습니다. 더 이상 사용하지 않는 거래처는 비활성화로 전환하세요.
        </p>
        <p v-else class="mt-4 rounded-2xl app-bg-danger-soft p-4 text-sm app-font-strong app-text-danger">
          참조 데이터가 없습니다. 삭제 후 복구할 수 없습니다.
        </p>
        <div class="mt-5 flex justify-end gap-2">
          <button class="rounded-2xl app-bg-muted px-5 py-2.5 text-sm app-font-emphasis" type="button" @click="isDeleteOpen = false">닫기</button>
          <button
            v-if="usage && !usage.canDelete"
            class="rounded-2xl app-bg-warning px-5 py-2.5 text-sm app-font-emphasis app-text-inverse disabled:opacity-50"
            type="button"
            :disabled="partnerMasterStore.isSaving || partner?.partnerStatus === 'INACTIVE'"
            @click="deactivateFromDeleteModal"
          >
            {{ partner?.partnerStatus === 'INACTIVE' ? '이미 비활성화됨' : '비활성화' }}
          </button>
          <button v-else class="rounded-2xl app-bg-danger px-5 py-2.5 text-sm app-font-emphasis app-text-inverse" type="button" @click="deletePartner">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>
