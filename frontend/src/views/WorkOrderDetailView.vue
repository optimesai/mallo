<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Loader2,
  Package,
  Pause,
  Play,
  RotateCcw,
  XCircle
} from '@lucide/vue'
import { useWorkOrderStore } from '@/state/workOrderStore'
import { formatDateTime } from '@/utils/dateFormat'
import type { WorkOrderResponse, WorkOrderStatus } from '@/api/workOrderApi'

const route = useRoute()
const router = useRouter()
const workOrderStore = useWorkOrderStore()

const pageError = ref<string | null>(null)
const toast = ref<string | null>(null)
const allowUnderTargetClose = ref(false)

const orderKey = computed(() => String(route.params.id))
const detail = computed(() => workOrderStore.selectedDetail)
const order = computed(() => detail.value?.workOrder ?? null)
const materialRequirements = computed(() => detail.value?.materialRequirements ?? [])
const executions = computed(() => detail.value?.executions ?? [])
const issueHistories = computed(() => detail.value?.issueHistories ?? [])
const hasStockShortage = computed(() => materialRequirements.value.some((item) => item.availableQty < getRemainingQty(item.requiredQty, item.issuedQty)))
const canIssue = computed(() => Boolean(order.value?.canIssueMaterials) && materialRequirements.value.length > 0 && !hasStockShortage.value)
const canCancelIssue = computed(() => Boolean(order.value?.canCancelIssue))

onMounted(() => {
  loadDetail()
})

async function loadDetail() {
  pageError.value = null
  try {
    await workOrderStore.loadWorkOrder(orderKey.value)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업지시 상세 정보를 불러오지 못했습니다.'
  }
}

function showToast(message: string) {
  toast.value = message
  setTimeout(() => {
    toast.value = null
  }, 3500)
}

function getStatusLabel(status: WorkOrderStatus) {
  const labels: Record<WorkOrderStatus, string> = {
    READY: '대기',
    RUN: '진행',
    HOLD: '보류',
    CLOSE: '마감'
  }
  return labels[status]
}

function formatNumber(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString()
}

function getRemainingQty(requiredQty: number, issuedQty: number) {
  return Math.max(requiredQty - issuedQty, 0)
}

function getTransactionTypeLabel(transactionType: string) {
  const labels: Record<string, string> = {
    PRODUCTION_ISSUE: '자재 불출',
    PRODUCTION_ISSUE_CANCEL: '자재 불출 취소',
    PRODUCTION_RECEIPT: '생산 입고',
    PRODUCTION_RECEIPT_CANCEL: '생산 입고 취소'
  }
  return labels[transactionType] ?? transactionType
}

function getTransactionQuantityLabel(transactionType: string, quantity: number) {
  const prefix = transactionType.endsWith('_CANCEL') ? '취소 ' : ''
  return `${prefix}${formatNumber(Math.abs(quantity))}`
}

function getOrderKey(target: WorkOrderResponse) {
  return target.orderNo || target.orderId
}

async function changeStatus(target: WorkOrderResponse, status: Exclude<WorkOrderStatus, 'CLOSE'>) {
  pageError.value = null
  try {
    const updated = await workOrderStore.updateStatus(getOrderKey(target), { status })
    showToast(`작업지시 [${updated.orderNo}] 상태가 변경되었습니다.`)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '상태 변경에 실패했습니다.'
  }
}

async function issueMaterials(target: WorkOrderResponse) {
  pageError.value = null
  try {
    await workOrderStore.issueMaterials(getOrderKey(target))
    showToast(`작업지시 [${target.orderNo}] 자재 불출이 완료되었습니다.`)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '자재 불출에 실패했습니다.'
  }
}

async function cancelIssueMaterials(target: WorkOrderResponse) {
  if (!confirm(`[${target.orderNo}] 자재 불출을 취소하고 현재고를 복원하시겠습니까?`)) return
  pageError.value = null
  try {
    await workOrderStore.cancelIssueMaterials(getOrderKey(target))
    showToast(`작업지시 [${target.orderNo}] 자재 불출이 취소되었습니다.`)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '자재 불출 취소에 실패했습니다.'
  }
}

async function closeOrder(target: WorkOrderResponse) {
  pageError.value = null
  try {
    const updated = await workOrderStore.closeWorkOrder(getOrderKey(target), {
      allowUnderTargetClose: allowUnderTargetClose.value
    })
    showToast(`작업지시 [${updated.orderNo}]가 마감되었습니다.`)
    allowUnderTargetClose.value = false
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '작업지시 마감에 실패했습니다.'
  }
}

async function deleteExecution(executionId: number) {
  if (!confirm('선택한 생산 실적을 삭제하고 연결된 재고 이력을 복원하시겠습니까?')) return
  pageError.value = null
  try {
    await workOrderStore.deleteExecution(executionId)
    showToast('생산 실적이 삭제되고 재고가 복원되었습니다.')
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '생산 실적 삭제에 실패했습니다.'
  }
}
</script>

<template>
  <div class="wo-page">
    <Transition name="wo-toast">
      <div v-if="toast" class="wo-toast">
        <span class="wo-toast-dot"></span>
        <span>{{ toast }}</span>
      </div>
    </Transition>

    <div v-if="pageError" class="wo-alert wo-alert-danger">
      <AlertTriangle class="wo-icon" />
      <span>{{ pageError }}</span>
    </div>

    <header class="wo-header">
      <div>
        <button class="wo-button wo-button-subtle" type="button" @click="router.push({ name: 'production-work-orders' })">
          <ArrowLeft class="wo-button-icon" />
          목록
        </button>
        <h1 class="wo-title">작업지시 상세</h1>
      </div>
      <button class="wo-button wo-button-subtle" :disabled="workOrderStore.isLoading" @click="loadDetail">
        <Loader2 v-if="workOrderStore.isLoading" class="wo-button-icon wo-spin" />
        새로고침
      </button>
    </header>

    <section v-if="order" class="wo-tab-section">
      <div class="wo-panel wo-detail-panel">
        <div class="wo-detail-head">
          <div>
            <p class="wo-kicker">WORK ORDER</p>
            <h2 class="wo-section-title">{{ order.orderNo }}</h2>
          </div>
          <span class="wo-status" :data-status="order.status">{{ getStatusLabel(order.status) }}</span>
        </div>
        <div class="wo-metric-grid">
          <div class="wo-metric"><span>목표 수량</span><strong>{{ formatNumber(order.targetQty) }}</strong></div>
          <div class="wo-metric"><span>양품</span><strong>{{ formatNumber(order.totalGoodQty) }}</strong></div>
          <div class="wo-metric"><span>불량</span><strong>{{ formatNumber(order.totalDefectQty) }}</strong></div>
          <div class="wo-metric"><span>진행률</span><strong>{{ order.progressRate }}%</strong></div>
        </div>
        <div class="wo-info-list">
          <div><span>생산 품목</span><strong>{{ order.itemCode }} · {{ order.itemName }}</strong></div>
          <div><span>BOM 버전</span><strong>{{ order.bomVersion }}</strong></div>
          <div><span>라우팅</span><strong>{{ order.factoryName }} / {{ order.lineName }} / {{ order.operationSeq }}. {{ order.operationName }}</strong></div>
          <div><span>계획일</span><strong>{{ order.planDate }}</strong></div>
          <div><span>등록일</span><strong>{{ formatDateTime(order.createdAt) }}</strong></div>
          <div><span>수정일</span><strong>{{ formatDateTime(order.updatedAt) }}</strong></div>
        </div>
        <div class="wo-action-grid">
          <button class="wo-button wo-button-primary" :disabled="!canIssue || workOrderStore.isSaving" @click="issueMaterials(order)">
            <Package class="wo-button-icon" />
            자재 불출
          </button>
          <button class="wo-button wo-button-subtle" :disabled="!canCancelIssue || workOrderStore.isSaving" @click="cancelIssueMaterials(order)">
            <RotateCcw class="wo-button-icon" />
            불출 취소
          </button>
          <button class="wo-button wo-button-primary" :disabled="!order.canStart || workOrderStore.isSaving" @click="changeStatus(order, 'RUN')">
            <Play class="wo-button-icon" />
            작업 착수
          </button>
          <button class="wo-button wo-button-subtle" :disabled="!order.canHold || workOrderStore.isSaving" @click="changeStatus(order, 'HOLD')">
            <Pause class="wo-button-icon" />
            보류
          </button>
          <button class="wo-button wo-button-subtle" :disabled="order.status !== 'HOLD' || workOrderStore.isSaving" @click="changeStatus(order, 'RUN')">
            <Play class="wo-button-icon" />
            재개
          </button>
        </div>
        <label class="wo-check"><input v-model="allowUnderTargetClose" type="checkbox" />목표 미달 마감 허용</label>
        <button class="wo-button wo-button-danger" :disabled="!order.canClose || workOrderStore.isSaving" @click="closeOrder(order)">
          <CheckCircle2 class="wo-button-icon" />
          작업지시 마감
        </button>
      </div>

      <div class="wo-panel">
        <div class="wo-panel-head">
          <h2 class="wo-section-title">BOM 자재 소요량</h2>
          <span v-if="hasStockShortage" class="wo-badge-danger">재고 부족</span>
        </div>
        <div class="wo-table-wrap">
          <table class="wo-table wo-table-compact">
            <thead><tr><th>자재</th><th>소요</th><th>필요</th><th>불출</th><th>잔여</th><th>가용</th></tr></thead>
            <tbody>
              <tr v-for="item in materialRequirements" :key="item.itemId">
                <td><strong>{{ item.itemName }}</strong><span>{{ item.itemCode }}</span></td>
                <td class="wo-number">{{ item.bomQuantity }}</td>
                <td class="wo-number">{{ formatNumber(item.requiredQty) }}</td>
                <td class="wo-number">{{ formatNumber(item.issuedQty) }}</td>
                <td class="wo-number">{{ formatNumber(getRemainingQty(item.requiredQty, item.issuedQty)) }}</td>
                <td class="wo-number" :class="{ 'wo-danger-text': item.availableQty < getRemainingQty(item.requiredQty, item.issuedQty) }">{{ formatNumber(item.availableQty) }}</td>
              </tr>
              <tr v-if="materialRequirements.length === 0"><td colspan="6" class="wo-empty">BOM 자재 정보가 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="wo-panel">
        <div class="wo-panel-head">
          <h2 class="wo-section-title">수불 연결 이력</h2>
          <span class="wo-count">총 {{ issueHistories.length }}건</span>
        </div>
        <div class="wo-table-wrap">
          <table class="wo-table wo-table-compact">
            <thead><tr><th>일시</th><th>유형</th><th>품목</th><th>로케이션</th><th>수량</th><th>작업자</th></tr></thead>
            <tbody>
              <tr v-for="history in issueHistories" :key="history.transactionId">
                <td class="wo-mono">{{ formatDateTime(history.createdAt) }}</td>
                <td><strong>{{ getTransactionTypeLabel(history.transactionType) }}</strong><span v-if="history.originalTransactionId">원거래 #{{ history.originalTransactionId }}</span></td>
                <td><strong>{{ history.itemName }}</strong><span>{{ history.itemCode }}</span></td>
                <td class="wo-mono">{{ history.locationCode }}</td>
                <td class="wo-number">{{ getTransactionQuantityLabel(history.transactionType, history.quantity) }}</td>
                <td><strong>{{ history.workerName ?? '-' }}</strong><span>{{ history.workerEmployeeNo ?? '-' }}</span></td>
              </tr>
              <tr v-if="issueHistories.length === 0"><td colspan="6" class="wo-empty">연결된 수불 이력이 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="wo-panel">
        <div class="wo-panel-head">
          <h2 class="wo-section-title">생산 실적 이력</h2>
          <span class="wo-count">총 {{ executions.length }}건</span>
        </div>
        <div class="wo-table-wrap">
          <table class="wo-table">
            <thead><tr><th>일시</th><th>공정</th><th>양품</th><th>불량</th><th>불량 사유</th><th>공수</th><th>작업자</th><th>액션</th></tr></thead>
            <tbody>
              <tr v-for="execution in executions" :key="execution.executionId">
                <td class="wo-mono">{{ formatDateTime(execution.createdAt) }}</td>
                <td><strong>{{ execution.operationName ?? '-' }}</strong><span>{{ execution.factoryName ?? '-' }} / {{ execution.lineName ?? '-' }}</span></td>
                <td class="wo-number">{{ formatNumber(execution.goodQty) }}</td>
                <td class="wo-number">{{ formatNumber(execution.defectQty) }}</td>
                <td><strong>{{ execution.defectType ?? '-' }}</strong><span>{{ execution.defectReason ?? '-' }} · {{ execution.reworkable ? '재작업 가능' : '재작업 불가' }}</span></td>
                <td class="wo-number">{{ formatNumber(execution.manHoursMinutes) }}분</td>
                <td><strong>{{ execution.workerName ?? '-' }}</strong><span>{{ execution.workerEmployeeNo ?? '-' }}</span></td>
                <td>
                  <button class="wo-icon-button wo-icon-danger" :disabled="!execution.canDelete || workOrderStore.isSaving" @click="deleteExecution(execution.executionId)">
                    <XCircle class="wo-button-icon" />
                  </button>
                </td>
              </tr>
              <tr v-if="executions.length === 0"><td colspan="8" class="wo-empty">생산 실적이 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section v-else-if="workOrderStore.isLoading" class="wo-panel wo-empty-panel">
      <Loader2 class="wo-empty-icon wo-spin" />
      <span>작업지시 상세 정보를 불러오고 있습니다.</span>
    </section>
  </div>
</template>
