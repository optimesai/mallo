<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  FileText,
  Clock,
  CheckCircle2,
  Search,
  ChevronDown,
  CheckSquare,
  Trash2,
  Plus,
  Loader2,
  ArrowRight,
  FileSpreadsheet,
  RefreshCw,
  Boxes,
  ChevronLeft,
  ChevronRight
} from '@lucide/vue'
import { useInboundStore } from '@/state/inboundStore'
import type { InboundCreateRequest } from '@/api/inboundApi'

const inboundStore = useInboundStore()
const router = useRouter()

// 페이지 에러 & 토스트 메시지
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)

// 1. 접이식 검색 필터 상태
const isSearchExpanded = ref(true)
const filterItem = ref('')
const filterPartner = ref('')
const filterStatus = ref<'ALL' | 'READY' | 'COMPLETED' | 'STACKED'>('ALL')
const filterDateStart = ref('')
const filterDateEnd = ref('')

// 2. 체크박스 다중 선택 상태
const selectedIds = ref<number[]>([])

// 3. 마스터-디테일 (선택된 행) 상태
const selectedInbound = ref<any>(null)
const activeTab = ref<'item-partner' | 'management'>('item-partner')

// 4. 등록 모달 상태
const isRegisterModalOpen = ref(false)
const selectedItemCode = ref('')
const selectedPartnerCode = ref('')
const selectedLocationCode = ref('')
const inboundQty = ref<number>(1)
const inboundDate = ref(new Date().toISOString().split('T')[0])
const registerError = ref<string | null>(null)
const isRegisterSubmitting = ref(false)

// 상세 정보 매핑 (로컬 스토어 데이터에서 매칭)
const selectedItemDetail = computed(() => {
  if (!selectedInbound.value) return null
  return inboundStore.items.find(i => i.itemCode === selectedInbound.value.itemCode)
})

const selectedPartnerDetail = computed(() => {
  if (!selectedInbound.value) return null
  return inboundStore.partners.find(p => p.partnerCode === selectedInbound.value.partnerCode)
})

const selectedLocationDetail = computed(() => {
  if (!selectedInbound.value) return null
  return inboundStore.locations.find(l => l.locationCode === selectedInbound.value.locationCode)
})

// 통계 데이터 계산 (total은 서버 totalElements, 상태별 건수는 현재 페이지 기준)
const stats = computed(() => {
  const total = inboundStore.totalElements
  const ready = inboundStore.inbounds.filter(i => i.status === 'READY').length
  const completed = inboundStore.inbounds.filter(i => i.status === 'COMPLETED').length
  const stacked = inboundStore.inbounds.filter(i => i.status === 'STACKED').length
  return { total, ready, completed, stacked }
})

// 서버 측 필터링 사용 — inboundStore.inbounds 그대로 표시
const filteredInbounds = computed(() => {
  return inboundStore.inbounds
})

// 합계 계산 (필터링된 수량 총합)
const totalInboundQty = computed(() => {
  return filteredInbounds.value.reduce((acc, curr) => acc + (curr.inboundQty || 0), 0)
})

const route = useRoute()

onMounted(async () => {
  await fetchPageData()
  if (route.query.register === 'true') {
    openRegisterModal()
  }
})

async function fetchPageData() {
  try {
    pageError.value = null
    await Promise.all([
      inboundStore.loadInbounds({
        page: inboundStore.page,
        size: 20,
        status: filterStatus.value !== 'ALL' ? filterStatus.value : undefined,
        keyword: filterItem.value || filterPartner.value || undefined,
        startDate: filterDateStart.value || undefined,
        endDate: filterDateEnd.value || undefined,
      }),
      inboundStore.loadItems('RAW'),
      inboundStore.loadPartners('SUPPLIER'),
      inboundStore.loadLocations()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터를 불러오는데 실패했습니다.'
  }
}

function goToPage(page: number) {
  inboundStore.page = page
  fetchPageData()
}

// 필터 초기화
function resetFilters() {
  filterItem.value = ''
  filterPartner.value = ''
  filterStatus.value = 'ALL'
  filterDateStart.value = ''
  filterDateEnd.value = ''
}

// 다중 선택 관리
const isAllSelected = computed(() => {
  if (filteredInbounds.value.length === 0) return false
  return filteredInbounds.value.every(item => selectedIds.value.includes(item.inboundId))
})

function toggleSelectAll(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    const newSelected = [...selectedIds.value]
    filteredInbounds.value.forEach(item => {
      if (!newSelected.includes(item.inboundId)) {
        newSelected.push(item.inboundId)
      }
    })
    selectedIds.value = newSelected
  } else {
    const filteredIds = filteredInbounds.value.map(item => item.inboundId)
    selectedIds.value = selectedIds.value.filter(id => !filteredIds.includes(id))
  }
}

function toggleSelect(id: number) {
  const index = selectedIds.value.indexOf(id)
  if (index === -1) {
    selectedIds.value.push(id)
  } else {
    selectedIds.value.splice(index, 1)
  }
}

// 행 클릭 시 마스터-디테일 선택
function selectRow(item: any) {
  if (selectedInbound.value?.inboundId === item.inboundId) {
    selectedInbound.value = null // 토글 해제
  } else {
    selectedInbound.value = item
  }
}

// 단일 검수 완료
async function handleComplete(id: number) {
  try {
    pageError.value = null
    await inboundStore.completeInbound(id)
    showToast('검수 완료 처리되었습니다.')
    if (selectedInbound.value?.inboundId === id) {
      selectedInbound.value.status = 'COMPLETED'
    }
  } catch (err) {
    alert(err instanceof Error ? err.message : '입고 완료 처리에 실패했습니다.')
  }
}

// 단일 입고 삭제
async function handleDelete(id: number) {
  if (!confirm('해당 입고 예정 내역을 삭제하시겠습니까?')) return
  try {
    pageError.value = null
    await inboundStore.deleteInbound(id)
    showToast('성공적으로 삭제되었습니다.')
    if (selectedInbound.value?.inboundId === id) {
      selectedInbound.value = null
    }
    selectedIds.value = selectedIds.value.filter(item => item !== id)
  } catch (err) {
    alert(err instanceof Error ? err.message : '입고 삭제에 실패했습니다.')
  }
}

// 다중 선택 일괄 검수완료
async function handleBatchComplete() {
  const readySelected = filteredInbounds.value.filter(
    item => selectedIds.value.includes(item.inboundId) && item.status === 'READY'
  )
  if (readySelected.length === 0) {
    alert('대기(READY) 상태인 선택 건이 없습니다.')
    return
  }

  if (!confirm(`선택한 ${readySelected.length}건의 자재를 일괄 검수 완료 처리하시겠습니까?`)) return

  try {
    pageError.value = null
    let successCount = 0
    for (const item of readySelected) {
      await inboundStore.completeInbound(item.inboundId)
      successCount++
    }
    showToast(`${successCount}건이 검수 완료 처리되었습니다.`)
    selectedIds.value = []
    await inboundStore.loadInbounds()
    if (selectedInbound.value) {
      const updated = inboundStore.inbounds.find(i => i.inboundId === selectedInbound.value.inboundId)
      if (updated) selectedInbound.value = updated
    }
  } catch (err) {
    alert(err instanceof Error ? err.message : '일괄 처리에 실패했습니다.')
  }
}

// 다중 선택 일괄 삭제
async function handleBatchDelete() {
  const readySelected = filteredInbounds.value.filter(
    item => selectedIds.value.includes(item.inboundId) && item.status === 'READY'
  )
  if (readySelected.length === 0) {
    alert('삭제 가능한 대기(READY) 상태의 선택 건이 없습니다.\n(완료 건은 삭제할 수 없습니다.)')
    return
  }

  if (!confirm(`선택한 ${readySelected.length}건의 입고 예정을 삭제하시겠습니까?`)) return

  try {
    pageError.value = null
    let successCount = 0
    for (const item of readySelected) {
      await inboundStore.deleteInbound(item.inboundId)
      successCount++
    }
    showToast(`${successCount}건이 삭제되었습니다.`)
    selectedIds.value = selectedIds.value.filter(id => !readySelected.some(item => item.inboundId === id))
    if (selectedInbound.value && readySelected.some(item => item.inboundId === selectedInbound.value.inboundId)) {
      selectedInbound.value = null
    }
  } catch (err) {
    alert(err instanceof Error ? err.message : '일괄 삭제에 실패했습니다.')
  }
}

// 입고 예정 등록
async function handleRegister() {
  registerError.value = null
  if (!selectedItemCode.value) {
    registerError.value = '품목을 선택해주세요.'
    return
  }
  if (!selectedPartnerCode.value) {
    registerError.value = '공급사(거래처)를 선택해주세요.'
    return
  }
  if (!selectedLocationCode.value) {
    registerError.value = '예정 로케이션을 선택해주세요.'
    return
  }
  if (!inboundQty.value || inboundQty.value < 1) {
    registerError.value = '입고 수량은 1개 이상이어야 합니다.'
    return
  }
  if (!inboundDate.value) {
    registerError.value = '입고 예정일을 선택해주세요.'
    return
  }

  isRegisterSubmitting.value = true
  const request: InboundCreateRequest = {
    itemCode: selectedItemCode.value,
    partnerCode: selectedPartnerCode.value,
    locationCode: selectedLocationCode.value,
    inboundQty: inboundQty.value,
    inboundDate: inboundDate.value
  }

  try {
    await inboundStore.registerInbound(request)
    showToast('새 입고 예정 건이 등록되었습니다.')
    closeRegisterModal()
  } catch (err) {
    registerError.value = err instanceof Error ? err.message : '등록에 실패했습니다.'
  } finally {
    isRegisterSubmitting.value = false
  }
}

function openRegisterModal() {
  selectedItemCode.value = ''
  selectedPartnerCode.value = ''
  selectedLocationCode.value = ''
  inboundQty.value = 1
  inboundDate.value = new Date().toISOString().split('T')[0]
  registerError.value = null
  isRegisterModalOpen.value = true
}

function closeRegisterModal() {
  isRegisterModalOpen.value = false
}

function showToast(msg: string) {
  successToast.value = msg
  setTimeout(() => {
    successToast.value = null
  }, 3000)
}

function navigateToStack() {
  router.push('/inbound/stack')
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

function formatDateTime(dateTimeStr: string) {
  if (!dateTimeStr) return '-'
  const d = new Date(dateTimeStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<template>
  <div class="app-page-plain pb-12 font-sans">
    <!-- 헤더 영역 -->
    <div class="app-page-header">
      <div>
        <h1 class="app-page-title">
          입고 등록 및 검수 관리
        </h1>
        <p class="app-page-subtitle">공급사로부터의 원자재 입고 예정 오더를 전산화하고 실물 검수 완료(COMPLETED) 처리합니다.</p>
      </div>
      <!-- 토스트 메시지 -->
      <div v-if="successToast" class="app-status app-status-success animate-fade-in">
        <span class="app-toast-dot"></span>
        {{ successToast }}
      </div>
    </div>

    <!-- 에러 배너 -->
    <div v-if="pageError" class="app-alert app-alert-danger items-center justify-between text-xs">
      <span>{{ pageError }}</span>
      <button @click="pageError = null" class="app-icon-button">×</button>
    </div>

    <!-- 요약 통계 카드 섹션 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
      <div class="app-stat-row-card">
        <div class="app-stat-icon">
          <FileText class="w-6 h-6" />
        </div>
        <div>
          <p class="app-stat-label-compact">전체 입고 오더</p>
          <p class="app-stat-value-compact">{{ stats.total }} 건</p>
        </div>
      </div>
      <div class="app-stat-row-card">
        <div class="app-stat-icon app-stat-icon-primary">
          <Clock class="w-6 h-6 animate-pulse" />
        </div>
        <div>
          <p class="app-stat-label-compact">입고 대기 (READY)</p>
          <p class="app-stat-value-compact app-stat-value-primary">{{ stats.ready }} 건</p>
        </div>
      </div>
      <div class="app-stat-row-card">
        <div class="app-stat-icon app-stat-icon-success">
          <CheckCircle2 class="w-6 h-6" />
        </div>
        <div>
          <p class="app-stat-label-compact">검수 완료 (COMPLETED)</p>
          <p class="app-stat-value-compact app-stat-value-success">{{ stats.completed }} 건</p>
        </div>
      </div>
      <div class="app-stat-row-card">
        <div class="app-stat-icon app-stat-icon-primary">
          <Boxes class="w-6 h-6" />
        </div>
        <div>
          <p class="app-stat-label-compact">적재 완료 (STACKED)</p>
          <p class="app-stat-value-compact app-stat-value-primary">{{ stats.stacked }} 건</p>
        </div>
      </div>
    </div>

    <!-- 접이식 검색 조건 패널 (Hansol WMS 스타일) -->
    <div class="app-panel">
      <div class="app-panel-head py-3.5">
        <span class="app-panel-title text-xs">
          <Search class="app-panel-icon" />
          조회 검색 조건
        </span>
        <button
          @click="isSearchExpanded = !isSearchExpanded"
          class="app-icon-button"
        >
          <ChevronDown
            class="w-4 h-4 transform transition-transform duration-200"
            :class="{ 'rotate-180': !isSearchExpanded }"
          />
        </button>
      </div>

      <!-- 확장 시 필터 폼 (반응형 Grid로 크래시 방지) -->
      <div v-show="isSearchExpanded" class="app-filter-body app-filter-grid text-xs">
        <div class="app-field">
          <label class="app-label">원자재 품목</label>
          <input
            v-model="filterItem"
            placeholder="품목명 또는 코드 검색"
            class="app-control h-9"
          >
        </div>
        <div class="app-field">
          <label class="app-label">공급처(거래처)</label>
          <input
            v-model="filterPartner"
            placeholder="거래처명 또는 코드 검색"
            class="app-control h-9"
          >
        </div>
        <div class="app-field">
          <label class="app-label">진행 상태</label>
          <select
            v-model="filterStatus"
            class="app-control h-9"
          >
            <option value="ALL">전체 상태</option>
            <option value="READY">입고 대기 (READY)</option>
            <option value="COMPLETED">검수 완료 (COMPLETED)</option>
            <option value="STACKED">적재 완료 (STACKED)</option>
          </select>
        </div>
        <div class="app-field">
          <label class="app-label">입고 예정일자</label>
          <div class="flex items-center gap-2">
            <input
              type="date"
              v-model="filterDateStart"
              class="app-control h-9 px-2 text-[11px]"
            >
            <span class="app-text-muted">~</span>
            <input
              type="date"
              v-model="filterDateEnd"
              class="app-control h-9 px-2 text-[11px]"
            >
          </div>
        </div>

        <div class="col-span-1 sm:col-span-2 lg:col-span-4 flex justify-end gap-2 pt-3 border-t app-border-muted">
          <button
            @click="resetFilters"
            class="app-button app-button-subtle h-9"
          >
            초기화
          </button>
          <button
            @click="fetchPageData"
            class="app-button app-button-primary h-9"
          >
            조회
          </button>
        </div>
      </div>
    </div>

    <!-- 메인 그리드 및 액션 툴바 -->
    <div class="app-panel">
      <!-- 액션 버튼 툴바 (패딩 및 마진 강화) -->
      <div class="px-5 py-4 app-bg-muted border-b app-border flex flex-wrap items-center justify-between gap-3">
        <!-- 다중 선택 액션 -->
        <div class="flex items-center gap-2">
          <button
            @click="handleBatchComplete"
            :disabled="selectedIds.length === 0"
            class="app-button app-button-primary h-9 disabled:opacity-50"
          >
            <CheckSquare class="w-4 h-4" />
            선택 검수완료
          </button>
          <button
            @click="handleBatchDelete"
            :disabled="selectedIds.length === 0"
            class="app-button app-button-muted h-9 disabled:opacity-50"
          >
            <Trash2 class="w-4 h-4" />
            선택 삭제
          </button>
          <span class="text-xs app-text-muted ml-2 app-font-strong app-bg-muted px-2 py-1 rounded-md" v-if="selectedIds.length > 0">
            선택: <span class="app-accent">{{ selectedIds.length }}</span>개
          </span>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="fetchPageData"
            class="app-button app-button-muted h-9"
          >
            <RefreshCw class="w-4 h-4" />
            새로고침
          </button>
          <button
            @click="openRegisterModal"
            class="app-button app-button-primary h-9"
          >
            <Plus class="w-4 h-4" />
            입고 예정 등록
          </button>
        </div>
      </div>

      <!-- 고밀도 데이터 테이블 영역 (Spacious border & alignment) -->
      <div class="overflow-x-auto">
        <table class="w-full min-w-[1200px] text-left text-xs app-text-soft border-collapse">
          <thead class="app-bg-muted app-text-soft app-font-strong uppercase border-b app-border">
            <tr class="whitespace-nowrap">
              <th class="px-4 py-3 text-center w-12 border-r app-border app-bg-muted">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="toggleSelectAll"
                  class="app-checkbox"
                >
              </th>
              <th class="px-4 py-3 text-center w-12 border-r app-border">No</th>
              <th class="px-4 py-3 text-center w-28 border-r app-border">상태</th>
              <th class="px-4 py-3 w-32 border-r app-border app-font-strong">품목코드</th>
              <th class="px-4 py-3 w-64 border-r app-border">품목명</th>
              <th class="px-4 py-3 w-56 border-r app-border">공급처명</th>
              <th class="px-4 py-3 text-center w-28 border-r app-border">예정 로케이션</th>
              <th class="px-4 py-3 text-right w-24 border-r app-border">예정 수량</th>
              <th class="px-4 py-3 text-center w-28 border-r app-border">입고 예정일</th>
              <th class="px-4 py-3 text-center w-36 border-r app-border">등록 일시</th>
              <th class="px-4 py-3 text-center w-20 border-r app-border">작업자</th>
              <th class="px-4 py-3 text-center w-36">액션</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-if="inboundStore.isLoading">
              <td colspan="12" class="px-4 py-12 text-center app-text-muted">
                <div class="flex items-center justify-center gap-2">
                  <Loader2 class="animate-spin h-5 w-5 app-loader" />
                  <span>데이터를 가져오고 있습니다...</span>
                </div>
              </td>
            </tr>
            <tr v-else-if="filteredInbounds.length === 0">
              <td colspan="12" class="px-4 py-12 text-center app-text-muted">
                조회된 입고 데이터가 없습니다.
              </td>
            </tr>
            <tr
              v-for="(item, idx) in filteredInbounds"
              :key="item.inboundId"
              @click="selectRow(item)"
              class="app-hover-muted/80 cursor-pointer transition-colors whitespace-nowrap"
              :class="{
                'app-bg-primary-soft': selectedInbound?.inboundId === item.inboundId,
                'app-bg-muted': idx % 2 === 1 && selectedInbound?.inboundId !== item.inboundId
              }"
            >
              <td class="px-4 py-3 text-center border-r app-border-muted" @click.stop>
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(item.inboundId)"
                  @change="toggleSelect(item.inboundId)"
                  class="app-checkbox"
                >
              </td>
              <td class="px-4 py-3 text-center app-font-label app-text-muted border-r app-border-muted">{{ idx + 1 }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted">
                <span
                  v-if="item.status === 'READY'"
                  class="app-status app-status-ready text-[10px]"
                >
                  <span class="w-1.5 h-1.5 mr-1 rounded-full app-bg-primary-soft0 animate-ping"></span>
                  대기 (READY)
                </span>
                <span
                  v-else-if="item.status === 'STACKED'"
                  class="app-status app-status-stacked text-[10px]"
                >
                  적재 완료 (STACKED)
                </span>
                <span
                  v-else
                  class="app-status app-status-completed text-[10px]"
                >
                  완료 (COMPLETED)
                </span>
              </td>
              <td class="px-4 py-3 font-mono app-text-strong app-font-strong border-r app-border-muted">{{ item.itemCode }}</td>
              <td class="px-4 py-3 app-text-soft border-r app-border-muted app-font-label">{{ item.itemName }}</td>
              <td class="px-4 py-3 app-text-soft border-r app-border-muted app-font-label">{{ item.partnerName }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted font-mono app-text-soft">{{ item.locationCode }}</td>
              <td class="px-4 py-3 text-right border-r app-border-muted app-font-emphasis app-text-strong">{{ item.inboundQty.toLocaleString() }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted app-text-muted app-font-label">{{ formatDate(item.inboundDate) }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted app-text-muted font-mono text-[11px]">{{ formatDateTime(item.createdAt) }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted app-font-label app-text-soft">{{ item.workerName || '-' }}</td>
              <td class="px-4 py-3 text-center" @click.stop>
                <div class="flex items-center justify-center gap-1.5">
                  <template v-if="item.status === 'READY'">
                    <button
                      @click="handleComplete(item.inboundId)"
                      class="app-button app-button-primary h-8 text-[10px]"
                    >
                      검수 완료
                    </button>
                    <button
                      @click="handleDelete(item.inboundId)"
                      class="app-button app-button-muted h-8 text-[10px]"
                    >
                      삭제
                    </button>
                  </template>
                  <template v-else-if="item.status === 'COMPLETED'">
                    <button
                      @click="navigateToStack"
                      class="app-button app-button-muted h-8 text-[10px]"
                    >
                      창고 적재
                      <ArrowRight class="w-3 h-3" />
                    </button>
                  </template>
                  <template v-else>
                    <span class="text-[10px] app-text-subtle app-font-label">처리 완료</span>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>

          <!-- 테이블 합계 행 (Summary Row) -->
          <tfoot class="app-bg-primary-soft app-text-strong app-font-strong border-t app-border">
            <tr class="whitespace-nowrap">
              <td class="px-4 py-3 border-r app-border-muted text-center">합계</td>
              <td colspan="5" class="px-4 py-3 border-r app-border-muted text-left app-text-muted font-normal">
                현재 검색 목록: 총 <span class="app-font-strong app-text-strong">{{ filteredInbounds.length }}</span>건의 입고
              </td>
              <td class="px-4 py-3 text-right border-r app-border-muted app-accent app-font-emphasis text-sm">
                {{ totalInboundQty.toLocaleString() }}
              </td>
              <td colspan="4" class="px-4 py-3"></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- 페이지네이션 -->
      <div
        v-if="inboundStore.totalPages > 0"
        class="app-pagination"
      >
        <span class="app-text-muted">
          총 <span class="app-font-strong app-text-soft">{{ inboundStore.totalElements.toLocaleString() }}</span>건
          ({{ inboundStore.page + 1 }} / {{ inboundStore.totalPages }} 페이지)
        </span>
        <div class="app-pagination-actions">
          <button
            @click="goToPage(0)"
            :disabled="inboundStore.page === 0"
            class="app-page-button"
          >««</button>
          <button
            @click="goToPage(inboundStore.page - 1)"
            :disabled="inboundStore.page === 0"
            class="app-page-button"
          >«</button>
          <button
            v-for="p in Math.min(inboundStore.totalPages, 5)"
            :key="p"
            @click="goToPage(Math.max(0, inboundStore.page - 2) + p - 1)"
            class="app-page-number"
            :class="(Math.max(0, inboundStore.page - 2) + p - 1) === inboundStore.page
              ? 'is-active'
              : ''"
          >
            {{ Math.max(0, inboundStore.page - 2) + p }}
          </button>
          <button
            @click="goToPage(inboundStore.page + 1)"
            :disabled="inboundStore.page >= inboundStore.totalPages - 1"
            class="app-page-button"
          >»</button>
          <button
            @click="goToPage(inboundStore.totalPages - 1)"
            :disabled="inboundStore.page >= inboundStore.totalPages - 1"
            class="app-page-button"
          >»»</button>
        </div>
      </div>
    </div>

    <!-- 하단 상세정보 마스터-디테일 패널 (충분한 패딩 부여) -->
    <div class="app-panel">
      <!-- 상세 탭 헤더 -->
      <div class="px-5 py-3 app-bg-muted border-b app-border flex items-center justify-between">
        <div class="flex items-center gap-4 text-xs app-font-strong app-text-soft">
          <span class="flex items-center gap-1.5 app-accent uppercase tracking-wider">
            <FileSpreadsheet class="w-4.5 h-4.5" />
            선택 오더 세부 내역
          </span>
          <div class="flex border-l app-border-strong pl-4 space-x-1.5">
            <button
              @click="activeTab = 'item-partner'"
              class="app-tab"
              :class="activeTab === 'item-partner' ? 'is-active' : ''"
            >
              품목 & 거래처 정보
            </button>
            <button
              @click="activeTab = 'management'"
              class="app-tab"
              :class="activeTab === 'management' ? 'is-active' : ''"
            >
              관리 정보
            </button>
          </div>
        </div>
        <div class="text-[11px] font-mono app-text-muted" v-if="selectedInbound">
          오더 ID: <span class="app-font-strong app-accent">{{ selectedInbound.inboundId }}</span>
        </div>
      </div>

      <!-- 상세정보 바디 (자연스럽게 늘어남) -->
      <div class="p-6 text-xs app-bg-surface min-h-[140px]">
        <div v-if="!selectedInbound" class="py-8 flex items-center justify-center app-text-muted app-font-label">
          <div class="text-center">
            <FileSpreadsheet class="w-8 h-8 app-text-subtle mx-auto mb-2" />
            <span>상단 그리드 목록에서 오더 행을 선택하시면 품목별 세부 속성 및 로케이션 렉 상세 정보가 동기화됩니다.</span>
          </div>
        </div>

        <!-- 품목 & 거래처 정보 탭 -->
        <div v-else-if="activeTab === 'item-partner'" class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="space-y-3.5">
            <h4 class="app-font-strong app-text-strong border-b app-border-muted pb-2 flex items-center gap-2 text-xs">
              <span class="app-section-mark"></span>
              자재 마스터 스펙
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="app-text-muted app-font-label">품목코드</span>
              <span class="col-span-2 font-mono app-text-soft app-font-emphasis">{{ selectedItemDetail?.itemCode || selectedInbound.itemCode }}</span>
              <span class="app-text-muted app-font-label">품목명</span>
              <span class="col-span-2 app-text-soft app-font-strong">{{ selectedItemDetail?.itemName || selectedInbound.itemName }}</span>
              <span class="app-text-muted app-font-label">규격 (Spec)</span>
              <span class="col-span-2 app-text-soft font-mono">{{ selectedItemDetail?.spec || '미지정' }}</span>
              <span class="app-text-muted app-font-label">관리 단위 / 분류</span>
              <span class="col-span-2 app-text-soft app-font-label">{{ selectedItemDetail?.unit }} / {{ selectedItemDetail?.itemType }}</span>
              <span class="app-text-muted app-font-label">안전 재고 수량</span>
              <span class="col-span-2 app-text-danger app-font-emphasis">{{ selectedItemDetail?.safetyStock.toLocaleString() }} EA</span>
            </div>
          </div>

          <div class="space-y-3.5">
            <h4 class="app-font-strong app-text-strong border-b app-border-muted pb-2 flex items-center gap-2 text-xs">
              <span class="app-section-mark"></span>
              공급업체 상세정보
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="app-text-muted app-font-label">사업자등록번호</span>
              <span class="col-span-2 font-mono app-text-soft app-font-label">{{ selectedPartnerDetail?.businessNo || '확인 대기' }}</span>
              <span class="app-text-muted app-font-label">거래처명</span>
              <span class="col-span-2 app-text-soft app-font-strong">{{ selectedPartnerDetail?.partnerName || selectedInbound.partnerName }}</span>
              <span class="app-text-muted app-font-label">대표자 성명</span>
              <span class="col-span-2 app-text-soft app-font-label">{{ selectedPartnerDetail?.representative || '-' }}</span>
              <span class="app-text-muted app-font-label">담당 연락처</span>
              <span class="col-span-2 app-text-soft font-mono app-font-label">{{ selectedPartnerDetail?.contactPhone || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 관리 정보 탭 -->
        <div v-else-if="activeTab === 'management'" class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="space-y-3.5">
            <h4 class="app-font-strong app-text-strong border-b app-border-muted pb-2 flex items-center gap-2 text-xs">
              <span class="w-1.5 h-3.5 app-accent-bg rounded-sm"></span>
              시스템 및 처리 상태
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="app-text-muted app-font-label">전산 오더 번호</span>
              <span class="col-span-2 font-mono app-accent app-font-emphasis">WMS-INB-{{ selectedInbound.inboundId }}</span>
              <span class="app-text-muted app-font-label">작업 진행 상태</span>
              <span class="col-span-2 app-font-strong app-text-soft">
                {{ selectedInbound.status === 'READY' ? '입고 대기 및 실물 검수 전' : selectedInbound.status === 'STACKED' ? '창고 렉 적재 완료 및 가용 재고 반영됨' : '실물 입고 완료 및 창고 적재 대기' }}
              </span>
              <span class="app-text-muted app-font-label">입고 전산 등록</span>
              <span class="col-span-2 font-mono app-text-muted app-font-label">{{ formatDateTime(selectedInbound.createdAt) }}</span>
            </div>
          </div>

          <div class="space-y-3.5">
            <h4 class="app-font-strong app-text-strong border-b app-border-muted pb-2 flex items-center gap-2 text-xs">
              <span class="w-1.5 h-3.5 app-accent-bg rounded-sm"></span>
              창고 임시 배치 정보
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="app-text-muted app-font-label">임시 로케이션 코드</span>
              <span class="col-span-2 font-mono app-font-strong app-text-soft">{{ selectedLocationDetail?.locationCode || selectedInbound.locationCode }}</span>
              <span class="app-text-muted app-font-label">창고 및 구역명</span>
              <span class="col-span-2 app-text-soft app-font-label">{{ selectedLocationDetail?.warehouseName || '미지정 임시 구역' }}</span>
              <span class="app-text-muted app-font-label">창고 상세 렉 정보</span>
              <span class="col-span-2 app-text-soft app-font-label">
                열: {{ selectedLocationDetail?.rackRow || '대기' }} / 단: {{ selectedLocationDetail?.rackColumn || '대기' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 입고 예정 등록 모달창 (Modal) -->
    <div v-if="isRegisterModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <div class="fixed inset-0 app-bg-muted backdrop-blur-sm transition-opacity" @click="closeRegisterModal"></div>

      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-lg app-bg-surface rounded-2xl shadow-xl border app-border overflow-hidden transform transition-all duration-300 scale-100">
          <div class="px-6 py-4.5 app-bg-muted border-b app-border-muted flex items-center justify-between">
            <h3 class="text-sm app-font-strong app-text-strong flex items-center gap-2">
              <span class="w-1.5 h-4.5 app-accent-bg rounded-sm"></span>
              신규 원자재 입고 오더 예정 등록
            </h3>
            <button @click="closeRegisterModal" class="app-text-muted app-font-strong text-lg">×</button>
          </div>

          <form @submit.prevent="handleRegister" class="p-6 space-y-4.5 text-xs">
            <div v-if="registerError" class="p-3 app-bg-danger-soft border app-border app-text-danger rounded-lg app-font-strong">
              {{ registerError }}
            </div>

            <div class="app-field">
              <label class="block app-font-strong app-text-soft" for="modal-item">입고 대상 원자재 품목 *</label>
              <select
                id="modal-item"
                v-model="selectedItemCode"
                class="app-control"
                required
              >
                <option value="">품목을 지정해주세요</option>
                <option v-for="item in inboundStore.items" :key="item.itemId" :value="item.itemCode">
                  [{{ item.itemCode }}] {{ item.itemName }} (단위: {{ item.unit }} / 규격: {{ item.spec }})
                </option>
              </select>
            </div>

            <div class="app-field">
              <label class="block app-font-strong app-text-soft" for="modal-partner">납품 공급사 (거래처) *</label>
              <select
                id="modal-partner"
                v-model="selectedPartnerCode"
                class="app-control"
                required
              >
                <option value="">공급사를 지정해주세요</option>
                <option v-for="p in inboundStore.partners" :key="p.partnerId" :value="p.partnerCode">
                  [{{ p.partnerCode }}] {{ p.partnerName }} (대표: {{ p.representative }})
                </option>
              </select>
            </div>

            <div class="app-field">
              <label class="block app-font-strong app-text-soft" for="modal-location">하역 및 입고대기 임시 로케이션 *</label>
              <select
                id="modal-location"
                v-model="selectedLocationCode"
                class="app-control"
                required
              >
                <option value="">하역장 또는 임시 로케이션을 선택하세요</option>
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} - {{ loc.rackRow }}{{ loc.rackColumn }}
                </option>
              </select>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="app-field">
                <label class="block app-font-strong app-text-soft" for="modal-qty">입고 요청 수량 (QTY) *</label>
                <input
                  id="modal-qty"
                  type="number"
                  v-model="inboundQty"
                  min="1"
                  placeholder="예: 100"
                  class="app-control"
                  required
                >
              </div>

              <div class="app-field">
                <label class="block app-font-strong app-text-soft" for="modal-date">입고 예정일자 *</label>
                <input
                  id="modal-date"
                  type="date"
                  v-model="inboundDate"
                  class="app-control"
                  required
                >
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t app-border-muted">
              <button
                type="button"
                @click="closeRegisterModal"
                class="h-9 px-4 app-text-muted app-bg-muted app-hover-muted rounded-lg app-font-strong transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isRegisterSubmitting"
                class="app-button app-button-primary h-9 disabled:opacity-70"
              >
                <Loader2 v-if="isRegisterSubmitting" class="animate-spin h-3.5 w-3.5 app-text-inverse" />
                입고오더 등록
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.2s ease-out forwards;
}

@keyframes fadeIn {
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
