<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, GitBranch, Layers3, Loader2, RefreshCw, Trash2 } from '@lucide/vue'
import { useAuthStore } from '@/state/authStore'
import { useBomMasterStore } from '@/state/bomMasterStore'
import BomTreeList from '@/ui/BomTreeList.vue'
import type { BomMasterResponse } from '@/api/bomMasterApi'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const bomStore = useBomMasterStore()

const activeTab = ref<'lines' | 'tree' | 'system'>('lines')
const pageError = ref<string | null>(null)
const toast = ref<string | null>(null)
const isDeleteOpen = ref(false)

const parentItemId = computed(() => Number(route.params.parentItemId))
const bomVersion = computed(() => String(route.query.bomVersion || 'v1.0'))
const lines = computed(() => bomStore.boms)
const representative = computed(() => lines.value[0] || null)
const canWrite = computed(() => ['ADMIN', 'MANAGER'].includes(authStore.user?.role || ''))
const canDelete = computed(() => authStore.user?.role === 'ADMIN')
const activeLineCount = computed(() => lines.value.filter((line) => line.bomStatus === 'ACTIVE').length)
const inactiveLineCount = computed(() => lines.value.filter((line) => line.bomStatus === 'INACTIVE').length)
const groupStatus = computed(() => {
  if (lines.value.length === 0) return '-'
  return activeLineCount.value > 0 ? 'ACTIVE' : 'INACTIVE'
})
const treeNodes = computed(() => {
  if (!representative.value) return []
  return bomStore.parentTree
})

onMounted(() => {
  loadDetail()
})

async function loadDetail() {
  try {
    pageError.value = null
    await Promise.all([
      bomStore.loadBomGroup(parentItemId.value, bomVersion.value),
      bomStore.loadParentTree(String(parentItemId.value), bomVersion.value)
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 상세 정보를 불러오지 못했습니다.'
  }
}

async function updateGroupStatus() {
  if (!canWrite.value || lines.value.length === 0) return
  const nextStatus = groupStatus.value === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  const actionLabel = nextStatus === 'ACTIVE' ? '활성화' : '비활성화'
  if (!confirm(`현재 BOM 그룹의 구성 ${lines.value.length}건을 모두 ${actionLabel}하시겠습니까?`)) return

  try {
    await Promise.all(lines.value.map((line) => bomStore.updateBomStatus(line.bomId, nextStatus)))
    showToast(`BOM 그룹이 ${nextStatus === 'ACTIVE' ? '활성화' : '비활성화'}되었습니다.`)
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 그룹 상태 변경에 실패했습니다.'
  }
}

async function deactivateGroup() {
  if (!canDelete.value || lines.value.length === 0) return

  try {
    await Promise.all(lines.value.map((line) => bomStore.deleteBom(line.bomId)))
    showToast('BOM 그룹이 비활성화되었습니다.')
    isDeleteOpen.value = false
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 그룹 비활성화에 실패했습니다.'
  }
}

async function toggleBomStatus(line: BomMasterResponse) {
  if (!canWrite.value) return
  const nextStatus = line.bomStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  if (!confirm(`${line.childItemCode} 구성 정보를 ${nextStatus === 'ACTIVE' ? '활성화' : '비활성화'}하시겠습니까?`)) return
  try {
    await bomStore.updateBomStatus(line.bomId, nextStatus)
    showToast('BOM 상태가 변경되었습니다.')
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 상태 변경에 실패했습니다.'
  }
}

async function deleteBom(line: BomMasterResponse) {
  if (!canDelete.value) return
  if (!confirm(`${line.parentItemCode} > ${line.childItemCode} BOM 구성을 비활성화하시겠습니까?`)) return
  try {
    await bomStore.deleteBom(line.bomId)
    showToast('BOM 구성이 비활성화되었습니다.')
    await loadDetail()
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 비활성화에 실패했습니다.'
  }
}

function getStatusLabel(value?: string) {
  return value === 'ACTIVE' ? '활성' : value === 'INACTIVE' ? '비활성' : '-'
}

function formatQuantity(value: number) {
  return Math.trunc(Number(value) || 0).toLocaleString()
}

function formatDateTime(value?: string) {
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
        <button class="mb-5 inline-flex items-center gap-2 rounded-xl border app-border px-4 py-2 text-sm app-font-emphasis app-text-soft transition app-hover-muted" type="button" @click="router.push({ name: 'bom-master' })">
          <ArrowLeft class="h-4 w-4" />
          목록으로
        </button>
        <h1 class="text-2xl app-font-emphasis tracking-tight app-text-strong">
          {{ representative?.parentItemName || 'BOM 상세' }}
        </h1>
        <p class="mt-1 font-mono text-sm app-font-strong app-text-muted">
          {{ representative?.parentItemCode || `ITEM-${parentItemId}` }} · {{ bomVersion }}
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <div v-if="toast" class="rounded-xl border app-border app-bg-success-soft px-4 py-2 text-sm app-font-strong app-text-success">{{ toast }}</div>
        <button class="inline-flex items-center gap-2 rounded-xl border app-border px-4 py-2 text-sm app-font-emphasis app-text-soft transition app-hover-muted" type="button" @click="loadDetail">
          <RefreshCw class="h-4 w-4" />
          새로고침
        </button>
        <button
          v-if="canWrite && lines.length > 0"
          class="rounded-xl app-bg-warning px-4 py-2 text-sm app-font-emphasis app-text-inverse disabled:opacity-50"
          type="button"
          :disabled="bomStore.isSaving"
          @click="updateGroupStatus"
        >
          {{ groupStatus === 'ACTIVE' ? '비활성화' : '활성화' }}
        </button>
        <button
          v-if="canDelete && lines.length > 0"
          class="rounded-xl app-bg-danger px-4 py-2 text-sm app-font-emphasis app-text-inverse disabled:opacity-50"
          type="button"
          :disabled="bomStore.isSaving"
          @click="isDeleteOpen = true"
        >
          삭제
        </button>
      </div>
    </div>

    <div v-if="pageError" class="rounded-2xl border app-border app-bg-danger-soft p-4 text-sm app-font-strong app-text-danger">{{ pageError }}</div>

    <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">상위 품목 유형</p>
        <strong class="mt-2 block text-xl app-text-strong">{{ representative?.parentItemType || '-' }}</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">상태</p>
        <strong class="mt-2 block text-xl" :class="groupStatus === 'ACTIVE' ? 'app-text-success' : 'app-text-warning'">{{ getStatusLabel(groupStatus) }}</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">구성 품목</p>
        <strong class="mt-2 block text-xl app-text-strong">{{ lines.length.toLocaleString() }}개</strong>
      </div>
      <div class="rounded-3xl border app-border app-bg-surface p-5 shadow-sm">
        <p class="text-xs app-font-emphasis uppercase tracking-widest app-text-muted">BOM 버전</p>
        <strong class="mt-2 block text-xl app-text-strong">{{ bomVersion }}</strong>
      </div>
    </div>

    <section class="overflow-hidden rounded-3xl border app-border app-bg-surface shadow-sm">
      <div class="flex flex-wrap gap-2 border-b app-border-muted p-4">
        <button class="rounded-2xl px-4 py-2 text-sm app-font-emphasis" :class="activeTab === 'lines' ? 'app-bg-strong app-text-inverse' : 'app-text-soft app-hover-muted'" type="button" @click="activeTab = 'lines'">구성 품목</button>
        <button class="rounded-2xl px-4 py-2 text-sm app-font-emphasis" :class="activeTab === 'tree' ? 'app-bg-strong app-text-inverse' : 'app-text-soft app-hover-muted'" type="button" @click="activeTab = 'tree'">정전개 트리</button>
        <button class="rounded-2xl px-4 py-2 text-sm app-font-emphasis" :class="activeTab === 'system' ? 'app-bg-strong app-text-inverse' : 'app-text-soft app-hover-muted'" type="button" @click="activeTab = 'system'">시스템</button>
      </div>

      <div v-if="activeTab === 'lines'" class="overflow-x-auto p-5">
        <table class="w-full min-w-[920px] text-left text-sm">
          <thead class="app-bg-muted text-xs uppercase tracking-widest app-text-muted">
            <tr>
              <th class="px-4 py-3">No</th>
              <th class="px-4 py-3">구성 품목</th>
              <th class="px-4 py-3">유형</th>
              <th class="px-4 py-3 text-right">소요량</th>
              <th class="px-4 py-3">단위</th>
              <th class="px-4 py-3">상태</th>
              <th class="px-4 py-3">등록일</th>
              <th class="px-4 py-3 text-right">작업</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="bomStore.isLoading">
              <td colspan="8" class="px-4 py-10 text-center app-font-strong app-text-muted">
                <span class="inline-flex items-center gap-2"><Loader2 class="h-4 w-4 animate-spin" />데이터를 가져오고 있습니다.</span>
              </td>
            </tr>
            <tr v-else-if="lines.length === 0">
              <td colspan="8" class="px-4 py-10 text-center app-font-strong app-text-muted">구성 품목이 없습니다.</td>
            </tr>
            <tr v-for="(line, index) in lines" v-else :key="line.bomId" class="border-t app-border-muted">
              <td class="px-4 py-3 app-text-muted">{{ index + 1 }}</td>
              <td class="px-4 py-3">
                <p class="font-mono app-font-emphasis app-text-strong">{{ line.childItemCode }}</p>
                <p class="mt-1 text-xs app-font-label app-text-muted">{{ line.childItemName }}</p>
              </td>
              <td class="px-4 py-3"><span class="rounded-full app-bg-muted px-2 py-1 text-xs app-font-emphasis app-text-soft">{{ line.childItemType }}</span></td>
              <td class="px-4 py-3 text-right tabular-nums app-font-emphasis app-text-strong">{{ formatQuantity(line.quantity) }}</td>
              <td class="px-4 py-3 app-text-soft">{{ line.childUnit }}</td>
              <td class="px-4 py-3">
                <span class="rounded-full px-2 py-1 text-xs app-font-emphasis" :class="line.bomStatus === 'ACTIVE' ? 'app-bg-success-soft app-text-success' : 'app-bg-warning-soft app-text-warning'">
                  {{ getStatusLabel(line.bomStatus) }}
                </span>
              </td>
              <td class="px-4 py-3 app-text-muted">{{ formatDateTime(line.createdAt) }}</td>
              <td class="px-4 py-3">
                <div class="flex justify-end gap-2">
                  <button v-if="canWrite" class="rounded-xl border app-border px-3 py-1.5 text-xs app-font-emphasis app-text-soft transition app-hover-muted" type="button" :disabled="bomStore.isSaving" @click="toggleBomStatus(line)">
                    {{ line.bomStatus === 'ACTIVE' ? '중지' : '활성' }}
                  </button>
                  <button v-if="canDelete" class="rounded-xl border px-3 py-1.5 text-xs app-font-emphasis app-text-danger transition app-hover-muted" type="button" :disabled="bomStore.isSaving" @click="deleteBom(line)">
                    <Trash2 class="h-3.5 w-3.5" />
                  </button>
                  <span v-if="!canWrite" class="text-xs app-font-strong app-text-muted">조회 전용</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'tree'" class="p-5">
        <div class="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 class="flex items-center gap-2 text-sm app-font-emphasis app-text-strong">
              <GitBranch class="h-4 w-4" />
              정전개 트리
            </h2>
            <p class="mt-1 text-sm app-font-label app-text-muted">상위 품목 1개 기준으로 하위 구성과 누적 소요량을 확인합니다.</p>
          </div>
          <span class="rounded-full app-bg-muted px-3 py-1 text-xs app-font-emphasis app-text-soft">{{ bomVersion }}</span>
        </div>
        <BomTreeList
          :nodes="treeNodes"
          relation-key="children"
          empty-text="정전개 결과가 없습니다."
          show-cumulative
          :base-quantity="1"
        />
      </div>

      <div v-else class="grid gap-4 p-5 lg:grid-cols-2">
        <section class="rounded-2xl border app-border-muted p-4">
          <h2 class="flex items-center gap-2 text-sm app-font-emphasis app-text-strong">
            <Layers3 class="h-4 w-4" />
            BOM 식별 정보
          </h2>
          <dl class="mt-4 space-y-3 text-sm">
            <div><dt class="app-text-muted">상위 품목 ID</dt><dd class="mt-1 font-mono app-font-emphasis app-text-strong">{{ parentItemId }}</dd></div>
            <div><dt class="app-text-muted">상위 품목 코드</dt><dd class="mt-1 font-mono app-font-emphasis app-text-strong">{{ representative?.parentItemCode || '-' }}</dd></div>
            <div><dt class="app-text-muted">상위 품목명</dt><dd class="mt-1 app-font-strong app-text-strong">{{ representative?.parentItemName || '-' }}</dd></div>
            <div><dt class="app-text-muted">BOM 버전</dt><dd class="mt-1 app-font-strong app-text-soft">{{ bomVersion }}</dd></div>
          </dl>
        </section>
        <section class="rounded-2xl border app-border-muted p-4">
          <h2 class="text-sm app-font-emphasis app-text-strong">상태 현황</h2>
          <dl class="mt-4 space-y-3 text-sm">
            <div><dt class="app-text-muted">전체 구성</dt><dd class="mt-1 app-font-strong app-text-strong">{{ lines.length.toLocaleString() }}건</dd></div>
            <div><dt class="app-text-muted">활성 구성</dt><dd class="mt-1 app-font-strong app-text-success">{{ activeLineCount.toLocaleString() }}건</dd></div>
            <div><dt class="app-text-muted">비활성 구성</dt><dd class="mt-1 app-font-strong app-text-warning">{{ inactiveLineCount.toLocaleString() }}건</dd></div>
            <div><dt class="app-text-muted">최초 등록일</dt><dd class="mt-1 app-text-soft">{{ formatDateTime(representative?.createdAt) }}</dd></div>
          </dl>
        </section>
      </div>
    </section>

    <div v-if="isDeleteOpen" class="fixed inset-0 z-[60] flex items-center justify-center app-backdrop p-4">
      <div class="w-full max-w-xl rounded-3xl app-bg-surface p-6 shadow-2xl">
        <h2 class="text-lg app-font-emphasis app-text-strong">BOM 삭제 확인</h2>
        <div class="mt-4 rounded-2xl border app-border-muted p-4">
          <p class="text-sm app-font-emphasis app-text-strong">{{ representative?.parentItemCode || '-' }} · {{ representative?.parentItemName || '-' }}</p>
          <p class="mt-1 text-sm app-font-label app-text-muted">{{ bomVersion }} · 구성 {{ lines.length.toLocaleString() }}건</p>
        </div>
        <p class="mt-4 rounded-2xl app-bg-warning-soft p-4 text-sm app-font-strong app-text-warning">
          BOM 삭제는 현재 정책상 구성 전체를 비활성화합니다. 비활성화된 구성은 생산 자재 산출과 정전개 조회에서 제외됩니다.
        </p>
        <div class="mt-5 flex justify-end gap-2">
          <button class="rounded-2xl app-bg-muted px-5 py-2.5 text-sm app-font-emphasis" type="button" @click="isDeleteOpen = false">닫기</button>
          <button class="rounded-2xl app-bg-danger px-5 py-2.5 text-sm app-font-emphasis app-text-inverse disabled:opacity-50" type="button" :disabled="bomStore.isSaving || activeLineCount === 0" @click="deactivateGroup">
            {{ activeLineCount === 0 ? '이미 비활성화됨' : '비활성화' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
