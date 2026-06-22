<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/state/authStore'
import { useItemMasterStore } from '@/state/itemMasterStore'
import type { ItemMasterUpdateRequest, ItemType, ItemUnit } from '@/api/itemMasterApi'
import {ArrowLeft} from "@lucide/vue";

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const itemMasterStore = useItemMasterStore()

const itemTypeOptions: Array<{ value: ItemType; label: string }> = [
  { value: 'RAW', label: '원자재' },
  { value: 'HALF', label: '반제품' },
  { value: 'FG', label: '완제품' }
]
const unitOptions: ItemUnit[] = ['ea', 'kg', 'box', 'L']

const activeTab = ref<'basic' | 'usage' | 'bom' | 'history' | 'system'>('basic')
const pageError = ref<string | null>(null)
const toast = ref<string | null>(null)
const isEditOpen = ref(false)
const formError = ref<string | null>(null)
const isReferenceWarningOpen = ref(false)
const isDeleteOpen = ref(false)
const pendingPayload = ref<ItemMasterUpdateRequest | null>(null)

const form = reactive<ItemMasterUpdateRequest>({
  itemName: '',
  spec: '',
  unit: 'ea',
  itemType: 'RAW',
  safetyStock: 0,
  confirmReferenceWarning: false
})

const itemId = computed(() => Number(route.params.id))
const item = computed(() => itemMasterStore.selectedItem)
const references = computed(() => itemMasterStore.itemReferences)
const usages = computed(() => itemMasterStore.itemUsages)
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const canWrite = computed(() => ['ADMIN', 'MANAGER'].includes(authStore.user?.role || ''))
const referenceItems = computed(() => {
  const ref = references.value
  if (!ref) return []
  return [
    { label: 'BOM 부모 품목', count: ref.bomParentCount },
    { label: 'BOM 자식 품목', count: ref.bomChildCount },
    { label: '현재고', count: ref.inventoryCount },
    { label: '입고', count: ref.inboundCount },
    { label: '수불 이력', count: ref.transactionHistoryCount },
    { label: '작업지시', count: ref.workOrderCount },
    { label: '출하지시', count: ref.shippingCount }
  ].filter((item) => item.count > 0)
})

onMounted(() => {
  loadDetail()
})

async function loadDetail() {
  try {
    pageError.value = null
    await itemMasterStore.loadItem(itemId.value)
    await Promise.all([
      itemMasterStore.loadItemReferences(itemId.value),
      itemMasterStore.loadItemUsages(itemId.value)
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '품목 상세 정보를 불러오지 못했습니다.'
  }
}

function openEdit() {
  if (!item.value) return
  form.itemName = item.value.itemName
  form.spec = item.value.spec || ''
  form.unit = item.value.unit
  form.itemType = item.value.itemType
  form.safetyStock = item.value.safetyStock
  form.confirmReferenceWarning = false
  formError.value = null
  isEditOpen.value = true
}

function closeEdit() {
  isEditOpen.value = false
  formError.value = null
}

async function submitEdit(confirmReferenceWarning = false) {
  if (!item.value) return
  const payload = normalizeForm(confirmReferenceWarning)
  if (!payload) return

  const typeChanged = payload.itemType !== item.value.itemType
  if (typeChanged && references.value?.hasReferences && !confirmReferenceWarning) {
    pendingPayload.value = payload
    isReferenceWarningOpen.value = true
    return
  }

  try {
    await itemMasterStore.updateItem(item.value.itemId, payload)
    showToast('품목 정보가 수정되었습니다.')
    closeEdit()
    isReferenceWarningOpen.value = false
    pendingPayload.value = null
    await loadDetail()
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '품목 수정에 실패했습니다.'
  }
}

async function submitReferenceConfirmed() {
  if (!pendingPayload.value || !item.value) return
  try {
    await itemMasterStore.updateItem(item.value.itemId, {
      ...pendingPayload.value,
      confirmReferenceWarning: true
    })
    showToast('참조 경고 확인 후 품목 정보가 수정되었습니다.')
    closeEdit()
    isReferenceWarningOpen.value = false
    pendingPayload.value = null
    await loadDetail()
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '품목 수정에 실패했습니다.'
    isReferenceWarningOpen.value = false
  }
}

async function toggleStatus() {
  if (!item.value) return
  const nextStatus = item.value.itemStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  if (!confirm(`품목을 ${nextStatus === 'ACTIVE' ? '활성화' : '비활성화'}하시겠습니까?`)) return
  try {
    await itemMasterStore.updateItemStatus(item.value.itemId, nextStatus)
    showToast('품목 상태가 변경되었습니다.')
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '품목 상태 변경에 실패했습니다.'
  }
}

async function requestDelete() {
  if (!item.value) return
  await itemMasterStore.loadItemReferences(item.value.itemId)
  isDeleteOpen.value = true
}

async function deleteItem() {
  if (!item.value) return
  if (!confirm('품목을 삭제하시겠습니까?')) return
  try {
    await itemMasterStore.deleteItem(item.value.itemId)
    showToast('품목이 삭제되었습니다.')
    await router.push({ name: 'item-master' })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '품목 삭제에 실패했습니다.'
  }
}

async function deactivateFromDeleteModal() {
  if (!item.value) return
  if (item.value.itemStatus === 'INACTIVE') {
    isDeleteOpen.value = false
    return
  }
  try {
    await itemMasterStore.updateItemStatus(item.value.itemId, 'INACTIVE')
    showToast('참조 중인 품목을 비활성화했습니다.')
    isDeleteOpen.value = false
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '품목 상태 변경에 실패했습니다.'
  }
}

function normalizeForm(confirmReferenceWarning: boolean): ItemMasterUpdateRequest | null {
  const itemName = form.itemName.trim()
  const spec = form.spec?.trim() || null

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
    itemName,
    spec,
    unit: form.unit,
    itemType: form.itemType,
    safetyStock: Number(form.safetyStock),
    confirmReferenceWarning
  }
}

function getTypeLabel(value?: ItemType) {
  return itemTypeOptions.find((option) => option.value === value)?.label || value || '-'
}

function getStatusLabel(value?: string) {
  return value === 'ACTIVE' ? '활성' : '비활성'
}

function formatDate(value?: string) {
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
        <button class="mb-5 inline-flex items-center gap-2 rounded-xl border app-border px-4 py-2 app-type-sm app-font-emphasis app-text-soft transition app-hover-muted" type="button" @click="router.push({ name: 'item-master' })"><ArrowLeft class="h-4 w-4" />목록으로</button>
        <h1 class="app-type-2xl app-font-emphasis tracking-tight app-text-strong">{{ item?.itemName || '품목 상세' }}</h1>
        <p class="mt-1 font-mono app-type-sm app-font-strong app-text-muted">{{ item?.itemCode || '-' }}</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <div v-if="toast" class="rounded-xl border app-border app-bg-success-soft px-4 py-2 app-type-sm app-font-strong app-text-success">{{ toast }}</div>
        <button v-if="canWrite && item" class="rounded-xl app-bg-strong px-4 py-2 app-type-sm app-font-emphasis app-text-inverse" type="button" @click="openEdit">수정</button>
        <button v-if="canWrite && item" class="rounded-xl app-bg-warning px-4 py-2 app-type-sm app-font-emphasis app-text-inverse" type="button" @click="toggleStatus">
          {{ item.itemStatus === 'ACTIVE' ? '비활성화' : '활성화' }}
        </button>
        <button v-if="isAdmin && item" class="rounded-xl app-bg-danger px-4 py-2 app-type-sm app-font-emphasis app-text-inverse" type="button" @click="requestDelete">삭제</button>
      </div>
    </div>

    <div v-if="pageError" class="rounded-2xl border app-border app-bg-danger-soft p-4 app-type-sm app-font-strong app-text-danger">{{ pageError }}</div>

    <div v-if="item" class="grid grid-cols-1 gap-4 md:grid-cols-4">
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="app-type-xs app-font-emphasis uppercase tracking-widest app-text-muted">분류</p>
        <strong class="mt-2 block app-type-xl app-text-strong">{{ getTypeLabel(item.itemType) }}</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="app-type-xs app-font-emphasis uppercase tracking-widest app-text-muted">상태</p>
        <strong class="mt-2 block app-type-xl" :class="item.itemStatus === 'ACTIVE' ? 'app-text-success' : 'app-text-warning'">{{ getStatusLabel(item.itemStatus) }}</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="app-type-xs app-font-emphasis uppercase tracking-widest app-text-muted">현재고 합계</p>
        <strong class="mt-2 block app-type-xl app-text-strong">{{ usages?.currentQtyTotal?.toLocaleString() || 0 }} {{ item.unit }}</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="app-type-xs app-font-emphasis uppercase tracking-widest app-text-muted">안전재고</p>
        <strong class="mt-2 block app-type-xl app-text-strong">{{ item.safetyStock.toLocaleString() }} {{ item.unit }}</strong>
      </div>
    </div>

    <section class="overflow-hidden rounded-3xl border app-border app-bg-surface shadow-sm">
      <div class="flex flex-wrap gap-2 border-b app-border-muted p-4">
        <button class="rounded-xl px-4 py-2 app-type-sm app-font-emphasis" :class="activeTab === 'basic' ? 'is-active' : ''" type="button" @click="activeTab = 'basic'">기본정보</button>
        <button class="rounded-xl px-4 py-2 app-type-sm app-font-emphasis" :class="activeTab === 'usage' ? 'is-active' : ''" type="button" @click="activeTab = 'usage'">활용정보</button>
        <button class="rounded-xl px-4 py-2 app-type-sm app-font-emphasis" :class="activeTab === 'bom' ? 'is-active' : ''" type="button" @click="activeTab = 'bom'">BOM 사용처</button>
        <button class="rounded-xl px-4 py-2 app-type-sm app-font-emphasis" :class="activeTab === 'history' ? 'is-active' : ''" type="button" @click="activeTab = 'history'">최근 수불</button>
        <button class="rounded-xl px-4 py-2 app-type-sm app-font-emphasis" :class="activeTab === 'system' ? 'is-active' : ''" type="button" @click="activeTab = 'system'">시스템</button>
      </div>

      <div class="p-6">
        <div v-if="!item" class="py-10 text-center app-font-strong app-text-muted">품목 상세 정보를 불러오는 중입니다.</div>

        <dl v-else-if="activeTab === 'basic'" class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div class="rounded-2xl app-bg-muted p-4"><dt class="app-type-xs app-font-emphasis app-text-muted">품목 코드</dt><dd class="mt-1 font-mono app-font-emphasis">{{ item.itemCode }}</dd></div>
          <div class="rounded-2xl app-bg-muted p-4"><dt class="app-type-xs app-font-emphasis app-text-muted">품목명</dt><dd class="mt-1 app-font-strong">{{ item.itemName }}</dd></div>
          <div class="rounded-2xl app-bg-muted p-4"><dt class="app-type-xs app-font-emphasis app-text-muted">규격</dt><dd class="mt-1 app-font-strong">{{ item.spec || '미지정' }}</dd></div>
          <div class="rounded-2xl app-bg-muted p-4"><dt class="app-type-xs app-font-emphasis app-text-muted">단위</dt><dd class="mt-1 app-font-strong">{{ item.unit }}</dd></div>
        </dl>

        <div v-else-if="activeTab === 'usage'" class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div class="rounded-2xl border app-border-muted p-4"><p class="app-type-xs app-font-emphasis app-text-muted">현재고 레코드</p><strong class="mt-1 block app-type-2xl">{{ references?.inventoryCount || 0 }}</strong></div>
          <div class="rounded-2xl border app-border-muted p-4"><p class="app-type-xs app-font-emphasis app-text-muted">작업지시 참조</p><strong class="mt-1 block app-type-2xl">{{ references?.workOrderCount || 0 }}</strong></div>
          <div class="rounded-2xl border app-border-muted p-4"><p class="app-type-xs app-font-emphasis app-text-muted">출하지시 참조</p><strong class="mt-1 block app-type-2xl">{{ references?.shippingCount || 0 }}</strong></div>
        </div>

        <div v-else-if="activeTab === 'bom'" class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div>
            <h3 class="mb-3 app-font-emphasis app-text-strong">이 품목으로 구성되는 BOM</h3>
            <div v-if="!usages?.asParentBoms.length" class="rounded-2xl app-bg-muted p-4 app-type-sm app-font-strong app-text-muted">등록된 하위 BOM이 없습니다.</div>
            <div v-for="bom in usages?.asParentBoms" :key="`p-${bom.bomId}`" class="mb-2 rounded-2xl border app-border-muted p-4">
              <p class="app-font-strong">{{ bom.childItemCode }} · {{ bom.childItemName }}</p>
              <p class="app-type-sm app-font-label app-text-muted">소요량 {{ bom.quantity }} {{ bom.childUnit }} · {{ bom.bomVersion }}</p>
            </div>
          </div>
          <div>
            <h3 class="mb-3 app-font-emphasis app-text-strong">이 품목이 투입되는 제품</h3>
            <div v-if="!usages?.asChildBoms.length" class="rounded-2xl app-bg-muted p-4 app-type-sm app-font-strong app-text-muted">상위 BOM 사용처가 없습니다.</div>
            <div v-for="bom in usages?.asChildBoms" :key="`c-${bom.bomId}`" class="mb-2 rounded-2xl border app-border-muted p-4">
              <p class="app-font-strong">{{ bom.parentItemCode }} · {{ bom.parentItemName }}</p>
              <p class="app-type-sm app-font-label app-text-muted">투입량 {{ bom.quantity }} {{ item.unit }} · {{ bom.bomVersion }}</p>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'history'" class="space-y-2">
          <div v-if="!usages?.recentTransactions.length" class="rounded-2xl app-bg-muted p-4 app-type-sm app-font-strong app-text-muted">최근 수불 이력이 없습니다.</div>
          <div v-for="history in usages?.recentTransactions" :key="history.transactionId" class="rounded-2xl border app-border-muted p-4">
            <p class="app-font-emphasis">{{ history.transactionType }} · {{ history.quantity.toLocaleString() }}</p>
            <p class="app-type-sm app-font-label app-text-muted">{{ history.locationCode }} · {{ history.reasonDesc }} · {{ formatDate(history.createdAt) }}</p>
          </div>
        </div>

        <dl v-else class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div class="rounded-2xl app-bg-muted p-4"><dt class="app-type-xs app-font-emphasis app-text-muted">품목 ID</dt><dd class="mt-1 font-mono app-font-emphasis">{{ item.itemId }}</dd></div>
          <div class="rounded-2xl app-bg-muted p-4"><dt class="app-type-xs app-font-emphasis app-text-muted">등록일</dt><dd class="mt-1 app-font-strong">{{ formatDate(item.createdAt) }}</dd></div>
        </dl>
      </div>
    </section>

    <div v-if="isEditOpen && item" class="fixed inset-0 z-50 flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-2xl rounded-3xl app-bg-surface shadow-2xl">
        <div class="border-b app-border-muted p-6">
          <h2 class="app-type-lg app-font-emphasis app-text-strong">품목 수정</h2>
          <div class="mt-3 inline-flex items-center rounded-2xl border app-border app-bg-primary-soft px-4 py-2 font-mono app-type-lg app-font-emphasis app-accent">
            {{ item.itemCode }}
          </div>
        </div>
        <form class="space-y-4 p-6" @submit.prevent="submitEdit(false)">
          <div v-if="formError" class="rounded-2xl border app-border app-bg-danger-soft p-3 app-type-sm app-font-strong app-text-danger">{{ formError }}</div>
          <label class="block app-type-sm app-font-emphasis app-text-soft">품목명 *<input v-model="form.itemName" required class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none"></label>
          <label class="block app-type-sm app-font-emphasis app-text-soft">규격<input v-model="form.spec" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none"></label>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <label class="block app-type-sm app-font-emphasis app-text-soft">분류<select v-model="form.itemType" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none"><option v-for="option in itemTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
            <label class="block app-type-sm app-font-emphasis app-text-soft">단위<select v-model="form.unit" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none"><option v-for="unit in unitOptions" :key="unit" :value="unit">{{ unit }}</option></select></label>
            <label class="block app-type-sm app-font-emphasis app-text-soft">안전재고<input v-model.number="form.safetyStock" min="0" type="number" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 app-type-sm outline-none"></label>
          </div>
          <div class="flex justify-end gap-2 border-t app-border-muted pt-4">
            <button class="rounded-2xl app-bg-muted px-5 py-2.5 app-type-sm app-font-emphasis" type="button" @click="closeEdit">취소</button>
            <button class="rounded-2xl app-accent-bg px-5 py-2.5 app-type-sm app-font-emphasis app-text-inverse" type="submit" :disabled="itemMasterStore.isSaving">저장</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="isReferenceWarningOpen" class="fixed inset-0 z-[60] flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-xl rounded-3xl app-bg-surface p-6 shadow-2xl">
        <h2 class="app-type-lg app-font-emphasis app-text-strong">참조 중인 품목입니다</h2>
        <p class="mt-2 app-type-sm app-font-label app-text-muted">BOM, 재고, 입고, 작업지시, 출하에서 참조 중인 품목의 분류를 변경하려고 합니다.</p>
        <p class="mt-3 rounded-2xl app-bg-warning-soft p-4 app-type-sm app-font-strong app-text-warning">품목 코드는 변경되지 않지만 업무 문서의 분류 해석이 달라질 수 있습니다.</p>
        <div class="mt-4 rounded-2xl border app-border-muted app-bg-surface p-4">
          <p class="mb-2 app-type-sm app-font-emphasis app-text-strong">참조 항목</p>
          <ul class="space-y-1 app-type-sm app-font-strong app-text-soft">
            <li v-for="refItem in referenceItems" :key="refItem.label">- {{ refItem.label }} {{ refItem.count.toLocaleString() }}건</li>
          </ul>
        </div>
        <div class="mt-5 flex justify-end gap-2">
          <button class="rounded-2xl app-bg-muted px-5 py-2.5 app-type-sm app-font-emphasis" type="button" @click="isReferenceWarningOpen = false">취소</button>
          <button class="rounded-2xl app-bg-warning px-5 py-2.5 app-type-sm app-font-emphasis app-text-inverse" type="button" @click="submitReferenceConfirmed">확인 후 수정</button>
        </div>
      </div>
    </div>

    <div v-if="isDeleteOpen" class="fixed inset-0 z-[60] flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-xl rounded-3xl app-bg-surface p-6 shadow-2xl">
        <h2 class="app-type-lg app-font-emphasis app-text-strong">품목 삭제 확인</h2>
        <div class="mt-4 rounded-2xl border app-border-muted p-4">
          <p class="mb-2 app-type-sm app-font-emphasis app-text-strong">참조 항목</p>
          <ul v-if="referenceItems.length > 0" class="space-y-1 app-type-sm app-font-strong app-text-soft">
            <li v-for="refItem in referenceItems" :key="refItem.label">- {{ refItem.label }} {{ refItem.count.toLocaleString() }}건</li>
          </ul>
          <p v-else class="app-type-sm app-font-strong app-text-muted">참조 중인 항목이 없습니다.</p>
        </div>
        <p v-if="references?.hasReferences" class="mt-4 rounded-2xl app-bg-warning-soft p-4 app-type-sm app-font-strong app-text-warning">참조 중인 품목은 삭제할 수 없습니다. 더 이상 사용하지 않는 품목은 비활성화로 전환하세요.</p>
        <p v-else class="mt-4 rounded-2xl app-bg-danger-soft p-4 app-type-sm app-font-strong app-text-danger">참조 데이터가 없습니다. 삭제 후 복구할 수 없습니다.</p>
        <div class="mt-5 flex justify-end gap-2">
          <button class="rounded-2xl app-bg-muted px-5 py-2.5 app-type-sm app-font-emphasis" type="button" @click="isDeleteOpen = false">닫기</button>
          <button v-if="references?.hasReferences" class="rounded-2xl app-bg-warning px-5 py-2.5 app-type-sm app-font-emphasis app-text-inverse disabled:opacity-50" type="button" :disabled="itemMasterStore.isSaving || item?.itemStatus === 'INACTIVE'" @click="deactivateFromDeleteModal">
            {{ item?.itemStatus === 'INACTIVE' ? '이미 비활성화됨' : '비활성화' }}
          </button>
          <button v-else class="rounded-2xl app-bg-danger px-5 py-2.5 app-type-sm app-font-emphasis app-text-inverse" type="button" @click="deleteItem">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>
