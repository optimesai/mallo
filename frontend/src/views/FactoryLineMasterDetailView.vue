<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Edit3, Factory, FileSpreadsheet, Loader2, Route, Trash2, X } from '@lucide/vue'
import { useAuthStore } from '@/state/authStore'
import { useFactoryRoutingStore } from '@/state/factoryRoutingStore'
import { formatDateTime } from '@/utils/dateFormat'
import type { FactoryRoutingRequest, FactoryRoutingResponse, FactoryRoutingStatus } from '@/api/factoryRoutingApi'

type DetailTab = 'operation' | 'line' | 'flow' | 'system'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const factoryRoutingStore = useFactoryRoutingStore()

const activeDetailTab = ref<DetailTab>('operation')
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isFormOpen = ref(false)
const formError = ref<string | null>(null)

const form = reactive<FactoryRoutingRequest>({
  factoryName: '',
  lineName: '',
  operationSeq: 1,
  operationName: ''
})

const routingId = computed(() => Number(route.params.id))
const routing = computed(() => factoryRoutingStore.selectedRouting)
const usage = computed(() => factoryRoutingStore.selectedUsage)
const canManageMasterData = computed(() => authStore.canManageMasterData)
const canDeleteRouting = computed(() => authStore.isAdmin)

const lineOperations = computed(() => {
  if (!routing.value) return []
  return factoryRoutingStore.routings
    .filter((item) => item.factoryName === routing.value?.factoryName && item.lineName === routing.value?.lineName)
    .sort((a, b) => a.operationSeq - b.operationSeq)
})

const lineSummary = computed(() => {
  const operations = lineOperations.value
  return {
    count: operations.length,
    firstOperation: operations[0]?.operationName || '-',
    lastOperation: operations[operations.length - 1]?.operationName || '-',
    latestCreatedAt: operations.map((operation) => operation.createdAt).sort().reverse()[0] || ''
  }
})

const hasOperationSeqConflict = computed(() => {
  if (!routing.value) return false
  const payload = normalizeForm()
  if (!payload.factoryName || !payload.lineName || !payload.operationSeq) return false
  return lineOperations.value.some((operation) => (
    operation.factoryName === payload.factoryName
    && operation.lineName === payload.lineName
    && operation.operationSeq === payload.operationSeq
    && operation.routingId !== routing.value?.routingId
  ))
})

onMounted(() => {
  loadDetail()
})

watch(() => route.params.id, () => {
  activeDetailTab.value = 'operation'
  loadDetail()
})

async function loadDetail() {
  if (!Number.isFinite(routingId.value)) {
    pageError.value = '라우팅 ID가 올바르지 않습니다.'
    return
  }

  try {
    pageError.value = null
    const loaded = await factoryRoutingStore.loadRouting(routingId.value)
    await Promise.all([
      factoryRoutingStore.loadRoutingUsage(routingId.value),
      factoryRoutingStore.loadRoutings({
        factoryName: loaded.factoryName,
        lineName: loaded.lineName
      })
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '라우팅 상세 정보를 불러오지 못했습니다.'
  }
}

function openEditForm() {
  if (!routing.value) return
  form.factoryName = routing.value.factoryName
  form.lineName = routing.value.lineName
  form.operationSeq = routing.value.operationSeq
  form.operationName = routing.value.operationName
  formError.value = null
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  formError.value = null
}

async function submitForm() {
  if (!routing.value) return
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
    await factoryRoutingStore.updateRouting(routing.value.routingId, payload)
    showToast('공장/라인 라우팅 정보가 수정되었습니다.')
    closeForm()
    await loadDetail()
  } catch (err) {
    formError.value = err instanceof Error ? err.message : '라우팅 수정에 실패했습니다.'
  }
}

async function toggleRoutingStatus() {
  if (!routing.value) return
  const nextStatus: FactoryRoutingStatus = routing.value.routingStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  const label = nextStatus === 'ACTIVE' ? '활성화' : '비활성화'
  if (!confirm(`[${routing.value.factoryName} / ${routing.value.lineName}] ${routing.value.operationName} 라우팅을 ${label}하시겠습니까?`)) return

  try {
    pageError.value = null
    await factoryRoutingStore.updateRoutingStatus(routing.value.routingId, { routingStatus: nextStatus })
    showToast(`라우팅이 ${label}되었습니다.`)
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '라우팅 상태 변경에 실패했습니다.'
  }
}

async function requestDelete() {
  if (!routing.value) return
  if (!canDeleteRouting.value) {
    pageError.value = '라우팅 삭제는 시스템 관리자만 가능합니다.'
    return
  }
  if (usage.value && !usage.value.canDelete) {
    pageError.value = `작업지시 ${usage.value.workOrderCount}건, 생산 실적 ${usage.value.executionCount}건에서 참조 중이므로 삭제할 수 없습니다.`
    return
  }
  if (!confirm(`[${routing.value.factoryName} / ${routing.value.lineName} / ${routing.value.operationSeq}공정] ${routing.value.operationName} 라우팅을 삭제하시겠습니까?`)) return

  try {
    pageError.value = null
    await factoryRoutingStore.deleteRouting(routing.value.routingId)
    await router.push({ name: 'factory-line-master' })
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '라우팅 삭제에 실패했습니다.'
  }
}

function goToRoutingDetail(nextRouting: FactoryRoutingResponse) {
  if (nextRouting.routingId === routing.value?.routingId) return
  router.push({ name: 'factory-line-master-detail', params: { id: nextRouting.routingId } })
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

function showToast(message: string) {
  successToast.value = message
  window.setTimeout(() => {
    successToast.value = null
  }, 2200)
}
</script>

<template>
  <div class="space-y-6 pb-12">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <button class="mb-4 inline-flex items-center gap-2 rounded-xl app-bg-muted px-4 py-2 text-sm app-font-emphasis app-text-soft" type="button" @click="router.push({ name: 'factory-line-master' })">
          <ArrowLeft class="h-4 w-4" />
          목록으로
        </button>
        <h1 class="flex items-center gap-2 text-2xl app-font-emphasis tracking-tight app-text-strong">
          <Factory class="h-6 w-6" />
          공장 및 생산라인 상세
        </h1>
        <p class="mt-1 text-sm app-font-label app-text-muted">선택한 공장/라인/공정 라우팅 기준정보와 참조 현황을 확인합니다.</p>
      </div>
      <div v-if="successToast" class="rounded-xl border app-border app-bg-success-soft px-4 py-2 text-sm app-font-strong app-text-success">
        {{ successToast }}
      </div>
    </div>

    <div v-if="pageError" class="rounded-2xl border app-border app-bg-danger-soft p-4 text-sm app-font-strong app-text-danger">
      {{ pageError }}
    </div>

    <div v-if="factoryRoutingStore.isLoading && !routing" class="rounded-3xl border app-border app-bg-surface p-10 text-center app-font-strong app-text-muted">
      <Loader2 class="mx-auto mb-3 h-6 w-6 animate-spin" />
      라우팅 상세 정보를 불러오는 중입니다.
    </div>

    <template v-else-if="routing">
      <section class="rounded-3xl border app-border app-bg-surface p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">ROUTING-ID {{ routing.routingId }}</p>
            <h2 class="mt-2 text-2xl app-font-emphasis app-text-strong">{{ routing.factoryName }} / {{ routing.lineName }}</h2>
            <p class="mt-1 app-font-strong app-text-soft">{{ routing.operationSeq }}. {{ routing.operationName }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <span class="rounded-full px-3 py-2 text-sm app-font-emphasis" :class="routing.routingStatus === 'ACTIVE' ? 'app-status-success' : 'app-status-warning'">
              {{ getRoutingStatusLabel(routing.routingStatus) }}
            </span>
            <button v-if="canManageMasterData" class="rounded-xl app-bg-strong px-4 py-2 text-sm app-font-emphasis app-text-inverse" type="button" @click="openEditForm">
              <Edit3 class="mr-1 inline h-4 w-4" />
              수정
            </button>
            <button v-if="canManageMasterData" class="rounded-xl app-bg-muted px-4 py-2 text-sm app-font-emphasis app-text-soft" type="button" @click="toggleRoutingStatus">
              {{ routing.routingStatus === 'ACTIVE' ? '비활성화' : '활성화' }}
            </button>
            <button v-if="canDeleteRouting" class="rounded-xl app-bg-danger px-4 py-2 text-sm app-font-emphasis app-text-inverse" type="button" @click="requestDelete">
              <Trash2 class="mr-1 inline h-4 w-4" />
              삭제
            </button>
          </div>
        </div>
      </section>

      <section class="rounded-3xl border app-border app-bg-surface shadow-sm">
        <div class="flex flex-wrap gap-2 border-b app-border-muted p-4">
          <button class="rounded-xl px-4 py-2 text-sm app-font-emphasis" :class="activeDetailTab === 'operation' ? 'app-bg-strong app-text-inverse' : 'app-bg-muted app-text-soft'" type="button" @click="activeDetailTab = 'operation'">공정 기준</button>
          <button class="rounded-xl px-4 py-2 text-sm app-font-emphasis" :class="activeDetailTab === 'line' ? 'app-bg-strong app-text-inverse' : 'app-bg-muted app-text-soft'" type="button" @click="activeDetailTab = 'line'">라인 상세</button>
          <button class="rounded-xl px-4 py-2 text-sm app-font-emphasis" :class="activeDetailTab === 'flow' ? 'app-bg-strong app-text-inverse' : 'app-bg-muted app-text-soft'" type="button" @click="activeDetailTab = 'flow'">공정 플로우</button>
          <button class="rounded-xl px-4 py-2 text-sm app-font-emphasis" :class="activeDetailTab === 'system' ? 'app-bg-strong app-text-inverse' : 'app-bg-muted app-text-soft'" type="button" @click="activeDetailTab = 'system'">시스템 정보</button>
        </div>

        <div class="p-6">
          <div v-if="activeDetailTab === 'operation'" class="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div class="rounded-2xl app-bg-muted p-5">
              <h3 class="mb-4 flex items-center gap-2 app-font-emphasis app-text-strong"><FileSpreadsheet class="h-4 w-4" />라우팅 식별 정보</h3>
              <dl class="grid grid-cols-[8rem_1fr] gap-3 text-sm">
                <dt class="app-font-emphasis app-text-muted">공장명</dt><dd class="app-font-strong app-text-strong">{{ routing.factoryName }}</dd>
                <dt class="app-font-emphasis app-text-muted">생산 라인</dt><dd class="app-font-strong app-text-strong">{{ routing.lineName }}</dd>
                <dt class="app-font-emphasis app-text-muted">공정명</dt><dd class="app-font-strong app-text-strong">{{ routing.operationName }}</dd>
              </dl>
            </div>
            <div class="rounded-2xl app-bg-muted p-5">
              <h3 class="mb-4 flex items-center gap-2 app-font-emphasis app-text-strong"><Route class="h-4 w-4" />운영 기준</h3>
              <dl class="grid grid-cols-[8rem_1fr] gap-3 text-sm">
                <dt class="app-font-emphasis app-text-muted">공정 순서</dt><dd class="app-font-strong app-text-strong">{{ routing.operationSeq }} 번째</dd>
                <dt class="app-font-emphasis app-text-muted">라인 공정 수</dt><dd class="app-font-strong app-text-strong">{{ lineSummary.count }} 건</dd>
                <dt class="app-font-emphasis app-text-muted">상태</dt><dd class="app-font-strong app-text-strong">{{ getRoutingStatusLabel(routing.routingStatus) }}</dd>
                <dt class="app-font-emphasis app-text-muted">등록 구조</dt><dd class="app-font-strong app-text-strong">공장 &gt; 생산라인 &gt; 세부 공정</dd>
              </dl>
            </div>
          </div>

          <div v-else-if="activeDetailTab === 'line'" class="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div class="rounded-2xl app-bg-muted p-5">
              <h3 class="mb-4 app-font-emphasis app-text-strong">라인 요약</h3>
              <dl class="grid grid-cols-[8rem_1fr] gap-3 text-sm">
                <dt class="app-font-emphasis app-text-muted">공장명</dt><dd class="app-font-strong app-text-strong">{{ routing.factoryName }}</dd>
                <dt class="app-font-emphasis app-text-muted">생산 라인</dt><dd class="app-font-strong app-text-strong">{{ routing.lineName }}</dd>
                <dt class="app-font-emphasis app-text-muted">공정 수</dt><dd class="app-font-strong app-text-strong">{{ lineSummary.count }} 건</dd>
              </dl>
            </div>
            <div class="rounded-2xl app-bg-muted p-5">
              <h3 class="mb-4 app-font-emphasis app-text-strong">라인 시작/종료</h3>
              <dl class="grid grid-cols-[8rem_1fr] gap-3 text-sm">
                <dt class="app-font-emphasis app-text-muted">첫 공정</dt><dd class="app-font-strong app-text-strong">{{ lineSummary.firstOperation }}</dd>
                <dt class="app-font-emphasis app-text-muted">마지막 공정</dt><dd class="app-font-strong app-text-strong">{{ lineSummary.lastOperation }}</dd>
                <dt class="app-font-emphasis app-text-muted">최근 등록</dt><dd class="app-font-strong app-text-strong">{{ formatDateTime(lineSummary.latestCreatedAt) }}</dd>
              </dl>
            </div>
          </div>

          <div v-else-if="activeDetailTab === 'flow'" class="flex flex-wrap items-center gap-3">
            <template v-for="(operation, index) in lineOperations" :key="operation.routingId">
              <button
                class="rounded-2xl border app-border px-4 py-3 text-left transition app-hover-muted"
                :class="routing.routingId === operation.routingId ? 'app-bg-primary-soft app-text-primary' : 'app-bg-surface app-text-soft'"
                type="button"
                @click="goToRoutingDetail(operation)"
              >
                <span class="block text-xs app-font-emphasis app-text-muted">{{ operation.operationSeq }} 공정</span>
                <strong class="mt-1 block app-font-emphasis">{{ operation.operationName }}</strong>
                <span class="mt-1 block text-xs">ROUTING-ID {{ operation.routingId }}</span>
              </button>
              <span v-if="index < lineOperations.length - 1" class="app-font-emphasis app-text-muted">→</span>
            </template>
          </div>

          <div v-else class="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div class="rounded-2xl app-bg-muted p-5">
              <h3 class="mb-4 app-font-emphasis app-text-strong">등록 이력</h3>
              <dl class="grid grid-cols-[8rem_1fr] gap-3 text-sm">
                <dt class="app-font-emphasis app-text-muted">라우팅 ID</dt><dd class="app-font-strong app-text-strong">{{ routing.routingId }}</dd>
                <dt class="app-font-emphasis app-text-muted">등록일시</dt><dd class="app-font-strong app-text-strong">{{ formatDateTime(routing.createdAt) }}</dd>
                <dt class="app-font-emphasis app-text-muted">중복 기준</dt><dd class="app-font-strong app-text-strong">공장명 + 라인명 + 공정 순서</dd>
                <dt class="app-font-emphasis app-text-muted">참조 작업지시</dt><dd class="app-font-strong app-text-strong">{{ usage?.workOrderCount ?? 0 }} 건</dd>
                <dt class="app-font-emphasis app-text-muted">참조 생산 실적</dt><dd class="app-font-strong app-text-strong">{{ usage?.executionCount ?? 0 }} 건</dd>
              </dl>
            </div>
            <div class="rounded-2xl app-bg-warning-soft p-5">
              <h3 class="mb-4 app-font-emphasis app-text-warning">참조 현황</h3>
              <p class="text-sm app-font-strong app-text-warning">{{ usage?.recommendedAction || '참조 현황을 불러오고 있습니다.' }}</p>
              <p v-if="usage?.workOrderNos.length" class="mt-3 text-sm app-font-strong app-text-soft">작업지시: {{ usage.workOrderNos.join(', ') }}</p>
              <p v-if="usage?.executionIds.length" class="mt-2 text-sm app-font-strong app-text-soft">실적 ID: {{ usage.executionIds.join(', ') }}</p>
            </div>
          </div>
        </div>
      </section>
    </template>

    <div v-if="isFormOpen" class="fixed inset-0 z-50 flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-2xl rounded-3xl app-bg-surface shadow-2xl">
        <div class="flex items-center justify-between border-b app-border-muted p-6">
          <h2 class="text-lg app-font-emphasis app-text-strong">공장/생산라인 라우팅 수정</h2>
          <button class="rounded-xl app-bg-muted p-2" type="button" @click="closeForm">
            <X class="h-4 w-4" />
          </button>
        </div>
        <form class="space-y-4 p-6" @submit.prevent="submitForm">
          <div v-if="formError" class="rounded-2xl border app-border app-bg-danger-soft p-3 text-sm app-font-strong app-text-danger">{{ formError }}</div>
          <label class="block text-sm app-font-emphasis app-text-soft">
            공장명 *
            <input v-model="form.factoryName" maxlength="50" required class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
          </label>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <label class="block text-sm app-font-emphasis app-text-soft">
              생산 라인명 *
              <input v-model="form.lineName" maxlength="50" required class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
            </label>
            <label class="block text-sm app-font-emphasis app-text-soft">
              공정 순서 *
              <input v-model.number="form.operationSeq" min="1" required type="number" class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
            </label>
          </div>
          <label class="block text-sm app-font-emphasis app-text-soft">
            세부 공정명 *
            <input v-model="form.operationName" maxlength="50" required class="mt-1 h-11 w-full rounded-2xl border app-border px-4 text-sm outline-none">
          </label>
          <div class="rounded-2xl app-bg-muted p-3 text-sm app-font-strong app-text-muted">
            동일한 공장/라인/공정 순서 조합은 저장 전에 확인하며, 서버에서도 중복 등록이 차단됩니다.
          </div>
          <div class="flex justify-end gap-2 border-t app-border-muted pt-4">
            <button class="rounded-2xl app-bg-muted px-5 py-2.5 text-sm app-font-emphasis" type="button" @click="closeForm">취소</button>
            <button class="rounded-2xl app-bg-strong px-5 py-2.5 text-sm app-font-emphasis app-text-inverse" type="submit" :disabled="factoryRoutingStore.isSaving">수정 저장</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
