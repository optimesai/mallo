<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search,
  ChevronDown,
  Boxes,
  RefreshCw,
  Plus,
  Loader2,
  Inbox
} from '@lucide/vue'
import { useInboundStore } from '@/state/inboundStore'

const inboundStore = useInboundStore()
const router = useRouter()

// 에러 & 성공 토스트 메시지
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)

// 1. 접이식 검색 조건 상태
const isSearchExpanded = ref(true)
const filterItem = ref('')
const filterPartner = ref('')
const filterCurrentLocation = ref('')

// 2. 체크박스 다중 선택 상태
const selectedIds = ref<number[]>([])

// 3. 마스터-디테일 상태
const selectedInbound = ref<any>(null)
const activeTab = ref<'item-partner' | 'location'>('item-partner')

// 4. 단일 적재 모달 상태
const isStackModalOpen = ref(false)
const targetLocationCode = ref('')
const stackError = ref<string | null>(null)
const isStackSubmitting = ref(false)

// 5. 일괄 적재 모달 상태
const isBatchStackModalOpen = ref(false)
const batchTargetLocationCode = ref('')
const batchStackError = ref<string | null>(null)
const isBatchStackSubmitting = ref(false)

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

// 적재 가능한 입고 완료(COMPLETED) 건만 필터링 (적재 완료된 STACKED 건은 백엔드에서 자동 제외)
const completedInbounds = computed(() => {
  return inboundStore.inbounds.filter(item => {
    if (item.status !== 'COMPLETED') {
      return false
    }
    // 품목 검색
    if (filterItem.value.trim() !== '') {
      const keyword = filterItem.value.toLowerCase()
      const matchCode = item.itemCode?.toLowerCase().includes(keyword)
      const matchName = item.itemName?.toLowerCase().includes(keyword)
      if (!matchCode && !matchName) return false
    }
    // 거래처 검색
    if (filterPartner.value.trim() !== '') {
      const keyword = filterPartner.value.toLowerCase()
      const matchCode = item.partnerCode?.toLowerCase().includes(keyword)
      const matchName = item.partnerName?.toLowerCase().includes(keyword)
      if (!matchCode && !matchName) return false
    }
    // 현재 로케이션 검색
    if (filterCurrentLocation.value.trim() !== '') {
      const keyword = filterCurrentLocation.value.toLowerCase()
      if (!item.locationCode?.toLowerCase().includes(keyword)) return false
    }
    return true
  })
})

// 총 적재 대기 수량 합계 계산
const totalStackQty = computed(() => {
  return completedInbounds.value.reduce((acc, curr) => acc + (curr.inboundQty || 0), 0)
})

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await Promise.all([
      inboundStore.loadInbounds({
        page: inboundStore.page,
        size: 20,
        status: 'COMPLETED',
      }),
      inboundStore.loadLocations()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터 로드에 실패했습니다.'
  }
}

function goToPage(page: number) {
  inboundStore.page = page
  fetchPageData()
}

function resetFilters() {
  filterItem.value = ''
  filterPartner.value = ''
  filterCurrentLocation.value = ''
}

// 다중 선택 관리
const isAllSelected = computed(() => {
  if (completedInbounds.value.length === 0) return false
  return completedInbounds.value.every(item => selectedIds.value.includes(item.inboundId))
})

function toggleSelectAll(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    const newSelected = [...selectedIds.value]
    completedInbounds.value.forEach(item => {
      if (!newSelected.includes(item.inboundId)) {
        newSelected.push(item.inboundId)
      }
    })
    selectedIds.value = newSelected
  } else {
    const filteredIds = completedInbounds.value.map(item => item.inboundId)
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

function selectRow(item: any) {
  if (selectedInbound.value?.inboundId === item.inboundId) {
    selectedInbound.value = null
  } else {
    selectedInbound.value = item
  }
}

// 단일 적재 모달 제어
function openStackModal(item: any) {
  selectedInbound.value = item
  targetLocationCode.value = ''
  stackError.value = null
  isStackModalOpen.value = true
}

// 단일 적재 실행
async function handleStack() {
  stackError.value = null
  if (!selectedInbound.value) return
  if (!targetLocationCode.value) {
    stackError.value = '적재할 대상 로케이션을 선택해주세요.'
    return
  }

  isStackSubmitting.value = true
  try {
    const inboundId = selectedInbound.value.inboundId
    await inboundStore.stackInventory(inboundId, targetLocationCode.value)
    
    showToast(`[ID: ${inboundId}] 자재가 ${targetLocationCode.value} 로케이션에 적재 완료되었습니다.`)
    closeStackModal()
    
    // 리스트 및 세부정보 상태 업데이트
    selectedInbound.value = null
    selectedIds.value = selectedIds.value.filter(id => id !== inboundId)
    await inboundStore.loadInbounds()
  } catch (err) {
    stackError.value = err instanceof Error ? err.message : '로케이션 적재 처리에 실패했습니다.'
  } finally {
    isStackSubmitting.value = false
  }
}

function closeStackModal() {
  isStackModalOpen.value = false
}

// 일괄 적재 모달 제어
function openBatchStackModal() {
  batchTargetLocationCode.value = ''
  batchStackError.value = null
  isBatchStackModalOpen.value = true
}

function closeBatchStackModal() {
  isBatchStackModalOpen.value = false
}

// 일괄 적재 실행
async function handleBatchStack() {
  batchStackError.value = null
  const selectedItems = completedInbounds.value.filter(item => selectedIds.value.includes(item.inboundId))
  if (selectedItems.length === 0) {
    alert('선택된 적재 오더가 없습니다.')
    return
  }
  if (!batchTargetLocationCode.value) {
    batchStackError.value = '일괄 적재할 대상 로케이션을 지정해주세요.'
    return
  }

  if (!confirm(`선택한 ${selectedItems.length}건의 자재를 모두 [${batchTargetLocationCode.value}] 로케이션으로 일괄 적재 처리하시겠습니까?`)) return

  isBatchStackSubmitting.value = true
  try {
    let successCount = 0
    for (const item of selectedItems) {
      await inboundStore.stackInventory(item.inboundId, batchTargetLocationCode.value)
      successCount++
    }
    showToast(`총 ${successCount}건의 자재가 ${batchTargetLocationCode.value} 로케이션에 일괄 적재 완료되었습니다.`)
    closeBatchStackModal()

    // 상태 초기화
    selectedIds.value = []
    selectedInbound.value = null
    await inboundStore.loadInbounds()
  } catch (err) {
    batchStackError.value = err instanceof Error ? err.message : '일괄 적재 처리에 실패했습니다.'
  } finally {
    isBatchStackSubmitting.value = false
  }
}

function showToast(msg: string) {
  successToast.value = msg
  setTimeout(() => {
    successToast.value = null
  }, 4000)
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
          창고 적재 및 로케이션 배치
        </h1>
        <p class="app-page-subtitle">검수 완료(COMPLETED)된 오더들을 최종 창고 렉(Rack)에 바인딩하여 실시간 가용 재고로 반영합니다. 적재 완료 시 상태가 STACKED로 변경되며 중복 적재가 방지됩니다.</p>
      </div>
      <!-- 성공 토스트 -->
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

    <!-- 접이식 검색 조건 패널 -->
    <div class="app-panel">
      <div class="app-panel-head py-3.5">
        <span class="app-panel-title text-xs">
          <Search class="app-panel-icon" />
          적재 대상 검색 조건
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

      <!-- 확장 시 필터 폼 -->
      <div v-show="isSearchExpanded" class="app-filter-body grid grid-cols-1 gap-5 text-xs sm:grid-cols-3">
        <div class="app-field">
          <label class="app-label">원자재 품목</label>
          <input
            v-model="filterItem"
            placeholder="품목명 또는 품목 코드 입력"
            class="app-control h-9"
          >
        </div>
        <div class="app-field">
          <label class="app-label">공급처(거래처)</label>
          <input
            v-model="filterPartner"
            placeholder="거래처명 또는 거래처 코드 입력"
            class="app-control h-9"
          >
        </div>
        <div class="app-field">
          <label class="app-label">현재 대기 로케이션</label>
          <input
            v-model="filterCurrentLocation"
            placeholder="현재 임시 지정된 로케이션 코드"
            class="app-control h-9"
          >
        </div>

        <div class="col-span-1 sm:col-span-3 flex justify-end gap-2 pt-3 border-t app-border-muted">
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
      <!-- 액션 버튼 툴바 -->
      <div class="px-5 py-4 app-bg-muted border-b app-border flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <button
            @click="openBatchStackModal"
            :disabled="selectedIds.length === 0"
            class="app-button app-button-primary h-9 disabled:opacity-50"
          >
            <Boxes class="w-4 h-4" />
            선택 일괄 적재
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
            @click="router.push('/inbound/receipt?register=true')"
            class="app-button app-button-muted h-9"
          >
            <Plus class="w-4 h-4" />
            입고 예정 등록
          </button>
        </div>
      </div>

      <!-- 그리드 테이블 -->
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
              <th class="px-4 py-3 w-32 border-r app-border app-font-strong">품목코드</th>
              <th class="px-4 py-3 w-64 border-r app-border">품목명</th>
              <th class="px-4 py-3 w-56 border-r app-border">공급처명</th>
              <th class="px-4 py-3 text-center w-36 border-r app-border">현재 대기 로케이션</th>
              <th class="px-4 py-3 text-right w-24 border-r app-border">입고 수량</th>
              <th class="px-4 py-3 text-center w-36 border-r app-border">검수 완료일시</th>
              <th class="px-4 py-3 text-center w-24 border-r app-border">담당자</th>
              <th class="px-4 py-3 text-center">액션</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-if="inboundStore.isLoading">
              <td colspan="10" class="px-4 py-12 text-center app-text-muted">
                <div class="flex items-center justify-center gap-2">
                  <Loader2 class="animate-spin h-5 w-5 app-loader" />
                  <span>데이터를 가져오고 있습니다...</span>
                </div>
              </td>
            </tr>
            <tr v-else-if="completedInbounds.length === 0">
              <td colspan="10" class="px-4 py-12 text-center app-text-muted">
                적재 대기 중인 입고 완료 자재가 없습니다.
              </td>
            </tr>
            <tr
              v-for="(item, idx) in completedInbounds"
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
              <td class="px-4 py-3 font-mono app-text-strong app-font-strong border-r app-border-muted">{{ item.itemCode }}</td>
              <td class="px-4 py-3 app-text-soft border-r app-border-muted app-font-label">{{ item.itemName }}</td>
              <td class="px-4 py-3 app-text-soft border-r app-border-muted app-font-label">{{ item.partnerName }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted">
                <span class="font-mono text-[11px] px-2.5 py-0.5 app-bg-muted rounded app-text-muted border app-border">
                  {{ item.locationCode }}
                </span>
              </td>
              <td class="px-4 py-3 text-right border-r app-border-muted app-font-emphasis app-text-strong">{{ item.inboundQty.toLocaleString() }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted app-text-muted font-mono text-[11px]">{{ formatDateTime(item.createdAt) }}</td>
              <td class="px-4 py-3 text-center border-r app-border-muted app-font-label app-text-soft">{{ item.workerName || '-' }}</td>
              <td class="px-4 py-3 text-center" @click.stop>
                <button
                  @click="openStackModal(item)"
                  class="app-button app-button-primary h-8 text-[10px]"
                >
                  <Boxes class="w-3.5 h-3.5" />
                  렉 적재
                </button>
              </td>
            </tr>
          </tbody>

          <!-- 테이블 합계 행 (Summary Row) -->
          <tfoot class="app-bg-primary-soft app-text-strong app-font-strong border-t app-border">
            <tr class="whitespace-nowrap">
              <td class="px-4 py-3 border-r app-border-muted text-center">합계</td>
              <td colspan="5" class="px-4 py-3 border-r app-border-muted text-left app-text-muted font-normal">
                현재 적재 대기 목록: 총 <span class="app-font-strong app-text-strong">{{ completedInbounds.length }}</span>건의 자재
              </td>
              <td class="px-4 py-3 text-right border-r app-border-muted app-accent app-font-emphasis text-sm">
                {{ totalStackQty.toLocaleString() }}
              </td>
              <td colspan="3" class="px-4 py-3"></td>
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
          <button @click="goToPage(0)" :disabled="inboundStore.page === 0"
            class="app-page-button">««</button>
          <button @click="goToPage(inboundStore.page - 1)" :disabled="inboundStore.page === 0"
            class="app-page-button">«</button>
          <button @click="goToPage(inboundStore.page + 1)" :disabled="inboundStore.page >= inboundStore.totalPages - 1"
            class="app-page-button">»</button>
          <button @click="goToPage(inboundStore.totalPages - 1)" :disabled="inboundStore.page >= inboundStore.totalPages - 1"
            class="app-page-button">»»</button>
        </div>
      </div>
    </div>

    <!-- 하단 상세정보 마스터-디테일 패널 -->
    <div class="app-panel">
      <!-- 탭 헤더 -->
      <div class="px-5 py-3 app-bg-muted border-b app-border flex items-center justify-between">
        <div class="flex items-center gap-4 text-xs app-font-strong app-text-soft">
          <span class="flex items-center gap-1.5 app-accent uppercase tracking-wider">
            <Boxes class="w-4.5 h-4.5" />
            적재 자재 정보 상세
          </span>
          <div class="flex border-l app-border-strong pl-4 space-x-1.5">
            <button
              @click="activeTab = 'item-partner'"
              class="app-tab"
              :class="activeTab === 'item-partner' ? 'is-active' : ''"
            >
              자재 마스터 스펙
            </button>
            <button
              @click="activeTab = 'location'"
              class="app-tab"
              :class="activeTab === 'location' ? 'is-active' : ''"
            >
              대기 위치 상세
            </button>
          </div>
        </div>
        <div class="text-[11px] font-mono app-text-muted" v-if="selectedInbound">
          오더 ID: <span class="app-font-strong app-accent">{{ selectedInbound.inboundId }}</span>
        </div>
      </div>

      <!-- 탭 바디 (자연스럽게 늘어남) -->
      <div class="p-6 text-xs app-bg-surface min-h-[140px]">
        <div v-if="!selectedInbound" class="py-8 flex items-center justify-center app-text-muted app-font-label">
          <div class="text-center">
            <Inbox class="w-8 h-8 app-text-subtle mx-auto mb-2" />
            <span>상단 대기 목록에서 행을 선택하시면 해당 입고건의 품목 규격 및 하역 세부 렉 위치가 매핑되어 노출됩니다.</span>
          </div>
        </div>

        <!-- 자재 마스터 정보 -->
        <div v-else-if="activeTab === 'item-partner'" class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="space-y-3.5">
            <h4 class="app-font-strong app-text-strong border-b app-border-muted pb-2 flex items-center gap-2 text-xs">
              <span class="app-section-mark"></span>
              자재 규격 상세
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="app-text-muted app-font-label">자재 코드</span>
              <span class="col-span-2 font-mono app-text-soft app-font-emphasis">{{ selectedItemDetail?.itemCode || selectedInbound.itemCode }}</span>
              <span class="app-text-muted app-font-label">자재 명칭</span>
              <span class="col-span-2 app-text-soft app-font-strong">{{ selectedItemDetail?.itemName || selectedInbound.itemName }}</span>
              <span class="app-text-muted app-font-label">규격 (Spec)</span>
              <span class="col-span-2 app-text-soft font-mono">{{ selectedItemDetail?.spec || '미지정' }}</span>
              <span class="app-text-muted app-font-label">단위 / 품목 분류</span>
              <span class="col-span-2 app-text-soft app-font-label">{{ selectedItemDetail?.unit }} / {{ selectedItemDetail?.itemType }}</span>
            </div>
          </div>

          <div class="space-y-3.5">
            <h4 class="app-font-strong app-text-strong border-b app-border-muted pb-2 flex items-center gap-2 text-xs">
              <span class="app-section-mark"></span>
              납품사(공급원) 정보
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="app-text-muted app-font-label">사업자 정보</span>
              <span class="col-span-2 font-mono app-text-soft app-font-label">{{ selectedPartnerDetail?.businessNo || '확인 필요' }}</span>
              <span class="app-text-muted app-font-label">공급처명</span>
              <span class="col-span-2 app-text-soft app-font-strong">{{ selectedPartnerDetail?.partnerName || selectedInbound.partnerName }}</span>
              <span class="app-text-muted app-font-label">대표 및 연락처</span>
              <span class="col-span-2 app-text-soft app-font-label">
                {{ selectedPartnerDetail?.representative || '-' }}
                <span v-if="selectedPartnerDetail?.contactPhone" class="font-mono app-text-muted font-normal"> ({{ selectedPartnerDetail.contactPhone }})</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 대기 위치 상세 정보 -->
        <div v-else-if="activeTab === 'location'" class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="space-y-3.5 col-span-2">
            <h4 class="app-font-strong app-text-strong border-b app-border-muted pb-2 flex items-center gap-2 text-xs">
              <span class="app-section-mark"></span>
              현재 대기(하역) 구역 상세
            </h4>
            <div class="grid grid-cols-4 gap-y-2.5 gap-x-2">
              <span class="app-text-muted app-font-label">로케이션 코드</span>
              <span class="font-mono app-font-strong app-accent">{{ selectedLocationDetail?.locationCode || selectedInbound.locationCode }}</span>
              <span class="app-text-muted app-font-label">임시 창고명</span>
              <span class="app-text-soft app-font-label">{{ selectedLocationDetail?.warehouseName || '미지정 구역' }}</span>
              <span class="app-text-muted app-font-label">배치 세부 렉(Rack)</span>
              <span class="col-span-3 app-text-soft app-font-label">
                {{ selectedLocationDetail?.rackRow || '-' }}열 - {{ selectedLocationDetail?.rackColumn || '-' }}단
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 단일 적재 처리 모달 (Modal) -->
    <div v-if="isStackModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <div class="fixed inset-0 app-bg-muted backdrop-blur-sm transition-opacity" @click="closeStackModal"></div>

      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-md app-bg-surface rounded-2xl shadow-xl border app-border overflow-hidden transform transition-all duration-300 scale-100">
          <div class="px-6 py-4.5 app-bg-muted border-b app-border-muted flex items-center justify-between">
            <h3 class="text-sm app-font-strong app-text-strong flex items-center gap-2">
              <span class="w-1.5 h-4.5 app-accent-bg rounded-sm"></span>
              WMS 창고 렉 로케이션 적재
            </h3>
            <button @click="closeStackModal" class="app-text-muted app-font-strong text-lg">×</button>
          </div>

          <form @submit.prevent="handleStack" class="p-6 space-y-4.5 text-xs">
            <div v-if="stackError" class="p-3 bg-red-50 border app-border text-red-600 rounded-lg text-xs app-font-label">
              {{ stackError }}
            </div>

            <div class="p-4 app-bg-muted border app-border-muted rounded-xl space-y-2">
              <div class="flex justify-between items-center">
                <span class="app-text-muted app-font-label">적재 자재명</span>
                <span class="app-font-strong app-text-strong text-right">
                  {{ selectedInbound?.itemName }}
                  <span class="block text-[10px] app-text-muted font-mono font-normal tracking-wide">{{ selectedInbound?.itemCode }}</span>
                </span>
              </div>
              <div class="flex justify-between items-center">
                <span class="app-text-muted app-font-label">적재 수량</span>
                <span class="app-font-emphasis app-accent text-sm">{{ selectedInbound?.inboundQty.toLocaleString() }} EA</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="app-text-muted app-font-label">현재 하역 로케이션</span>
                <span class="font-mono app-text-muted app-bg-muted border app-border px-1.5 py-0.5 rounded">{{ selectedInbound?.locationCode }}</span>
              </div>
            </div>

            <div class="app-field">
              <label class="block app-font-strong app-text-soft" for="modal-target-location">최종 적치 렉 로케이션 코드 *</label>
              <select
                id="modal-target-location"
                v-model="targetLocationCode"
                class="app-control"
                required
              >
                <option value="">적재할 로케이션 코드를 선택하세요</option>
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} - {{ loc.rackRow }}{{ loc.rackColumn }}
                </option>
              </select>
              <p class="text-[10px] app-text-muted leading-normal mt-1">※ 지정한 렉 로케이션의 가용 수량이 실시간으로 증가하며 수불 이력에 로그가 적재됩니다.</p>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t app-border-muted">
              <button
                type="button"
                @click="closeStackModal"
                class="h-9 px-4 app-text-muted app-bg-muted app-hover-muted rounded-lg app-font-strong transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isStackSubmitting"
                class="app-button app-button-primary h-9 disabled:opacity-70"
              >
                <Loader2 v-if="isStackSubmitting" class="animate-spin h-3.5 w-3.5 app-text-inverse" />
                적재 확인
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 일괄 적재 처리 모달 (Modal) -->
    <div v-if="isBatchStackModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <div class="fixed inset-0 app-bg-muted backdrop-blur-sm transition-opacity" @click="closeBatchStackModal"></div>

      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-md app-bg-surface rounded-2xl shadow-xl border app-border overflow-hidden transform transition-all duration-300 scale-100">
          <div class="px-6 py-4.5 app-bg-muted border-b app-border-muted flex items-center justify-between">
            <h3 class="text-sm app-font-strong app-text-strong flex items-center gap-2">
              <span class="w-1.5 h-4.5 app-accent-bg rounded-sm"></span>
              WMS 일괄 창고 적재
            </h3>
            <button @click="closeBatchStackModal" class="app-text-muted app-font-strong text-lg">×</button>
          </div>

          <form @submit.prevent="handleBatchStack" class="p-6 space-y-4.5 text-xs">
            <div v-if="batchStackError" class="p-3 bg-red-50 border app-border text-red-650 rounded-lg app-font-strong">
              {{ batchStackError }}
            </div>

            <div class="p-4 app-bg-primary-soft/50 border app-border-muted rounded-xl">
              <div class="text-xs app-font-label app-text-soft">
                일괄 적재 대상 품목 건수: <span class="app-font-emphasis app-accent text-sm">{{ selectedIds.length }}</span>건
              </div>
              <p class="text-[10px] app-text-muted mt-1 leading-normal">
                선택하신 여러 오더의 원자재들을 하나의 대상 렉 로케이션에 동시에 일괄 적재 및 바인딩 처리를 수행합니다.
              </p>
            </div>

            <div class="app-field">
              <label class="block app-font-strong app-text-soft" for="modal-batch-target-location">일괄 적치 렉 로케이션 코드 지정 *</label>
              <select
                id="modal-batch-target-location"
                v-model="batchTargetLocationCode"
                class="app-control"
                required
              >
                <option value="">일괄 적재할 로케이션 코드를 선택하세요</option>
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} - {{ loc.rackRow }}{{ loc.rackColumn }}
                </option>
              </select>
              <p class="text-[10px] app-text-muted leading-normal mt-1">※ 선택된 모든 자재들이 해당 렉으로 바인딩 처리되며, 각각의 재고가 일괄 합산됩니다.</p>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t app-border-muted">
              <button
                type="button"
                @click="closeBatchStackModal"
                class="h-9 px-4 app-text-muted app-bg-muted app-hover-muted rounded-lg app-font-strong transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isBatchStackSubmitting"
                class="app-button app-button-primary h-9 disabled:opacity-70"
              >
                <Loader2 v-if="isBatchStackSubmitting" class="animate-spin h-3.5 w-3.5 app-text-inverse" />
                일괄 적재 확인
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
