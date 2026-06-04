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
      inboundStore.loadInbounds(),
      inboundStore.loadLocations()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터 로드에 실패했습니다.'
  }
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
  <div class="space-y-6 pb-12 font-sans">
    <!-- 헤더 영역 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-2">
          <span class="w-1.5 h-6 bg-[#1428A0] rounded-sm"></span>
          창고 적재 및 로케이션 배치
        </h1>
        <p class="text-xs text-slate-500 mt-1.5 font-medium">검수 완료(COMPLETED)된 오더들을 최종 창고 렉(Rack)에 바인딩하여 실시간 가용 재고로 반영합니다. 적재 완료 시 상태가 STACKED로 변경되며 중복 적재가 방지됩니다.</p>
      </div>
      <!-- 성공 토스트 -->
      <div v-if="successToast" class="bg-emerald-50 border border-emerald-200 text-emerald-800 px-4 py-2 rounded-lg text-xs font-semibold flex items-center gap-2 shadow-sm animate-fade-in">
        <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
        {{ successToast }}
      </div>
    </div>

    <!-- 에러 배너 -->
    <div v-if="pageError" class="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-2.5 rounded-lg text-xs flex items-center justify-between">
      <span>{{ pageError }}</span>
      <button @click="pageError = null" class="text-rose-500 hover:text-rose-700 font-bold">×</button>
    </div>

    <!-- 접이식 검색 조건 패널 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div class="px-5 py-3.5 border-b border-slate-250 bg-slate-50 flex items-center justify-between">
        <span class="text-xs font-bold text-slate-700 flex items-center gap-1.5">
          <Search class="w-4 h-4 text-slate-400" />
          적재 대상 검색 조건
        </span>
        <button
          @click="isSearchExpanded = !isSearchExpanded"
          class="p-1 hover:bg-slate-200 rounded text-slate-500 transition-colors"
        >
          <ChevronDown
            class="w-4 h-4 transform transition-transform duration-200"
            :class="{ 'rotate-180': !isSearchExpanded }"
          />
        </button>
      </div>

      <!-- 확장 시 필터 폼 -->
      <div v-show="isSearchExpanded" class="p-5 border-t border-slate-200 bg-white grid grid-cols-1 sm:grid-cols-3 gap-5 text-xs">
        <div class="space-y-1.5">
          <label class="block font-bold text-slate-600">원자재 품목</label>
          <input
            v-model="filterItem"
            placeholder="품목명 또는 품목 코드 입력"
            class="w-full h-9 px-3 bg-white border border-slate-300 rounded-lg outline-none focus:border-[#1428A0] focus:ring-1 focus:ring-[#1428A0] transition shadow-sm"
          >
        </div>
        <div class="space-y-1.5">
          <label class="block font-bold text-slate-600">공급처(거래처)</label>
          <input
            v-model="filterPartner"
            placeholder="거래처명 또는 거래처 코드 입력"
            class="w-full h-9 px-3 bg-white border border-slate-300 rounded-lg outline-none focus:border-[#1428A0] focus:ring-1 focus:ring-[#1428A0] transition shadow-sm"
          >
        </div>
        <div class="space-y-1.5">
          <label class="block font-bold text-slate-600">현재 대기 로케이션</label>
          <input
            v-model="filterCurrentLocation"
            placeholder="현재 임시 지정된 로케이션 코드"
            class="w-full h-9 px-3 bg-white border border-slate-300 rounded-lg outline-none focus:border-[#1428A0] focus:ring-1 focus:ring-[#1428A0] transition shadow-sm"
          >
        </div>

        <div class="col-span-1 sm:col-span-3 flex justify-end gap-2 pt-3 border-t border-slate-100">
          <button
            @click="resetFilters"
            class="h-9 px-4 bg-slate-100 hover:bg-slate-200 text-slate-600 font-bold rounded-lg transition"
          >
            초기화
          </button>
          <button
            @click="fetchPageData"
            class="h-9 px-5 bg-[#1428A0] hover:bg-[#102180] text-white font-bold rounded-lg shadow-sm transition"
          >
            조회
          </button>
        </div>
      </div>
    </div>

    <!-- 메인 그리드 및 액션 툴바 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <!-- 액션 버튼 툴바 -->
      <div class="px-5 py-4 bg-slate-50 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <button
            @click="openBatchStackModal"
            :disabled="selectedIds.length === 0"
            class="h-9 px-4 text-xs bg-[#1428A0] hover:bg-[#102180] text-white font-bold rounded-lg shadow-sm transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <Boxes class="w-4 h-4" />
            선택 일괄 적재
          </button>
          <span class="text-xs text-slate-500 ml-2 font-bold bg-slate-100 px-2 py-1 rounded-md" v-if="selectedIds.length > 0">
            선택: <span class="text-[#1428A0]">{{ selectedIds.length }}</span>개
          </span>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="fetchPageData"
            class="h-9 px-4 text-xs bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 font-bold rounded-lg shadow-sm transition flex items-center gap-2"
          >
            <RefreshCw class="w-4 h-4" />
            새로고침
          </button>
          <button
            @click="router.push('/inbound/receipt?register=true')"
            class="h-9 px-4 text-xs bg-white border border-slate-300 hover:bg-slate-50 text-slate-700 font-bold rounded-lg shadow-sm transition flex items-center gap-2"
          >
            <Plus class="w-4 h-4" />
            입고 예정 등록
          </button>
        </div>
      </div>

      <!-- 그리드 테이블 -->
      <div class="overflow-x-auto">
        <table class="w-full min-w-[1200px] text-left text-xs text-slate-650 border-collapse">
          <thead class="bg-slate-50 text-slate-700 font-bold uppercase border-b border-slate-200">
            <tr class="whitespace-nowrap">
              <th class="px-4 py-3 text-center w-12 border-r border-slate-200 bg-slate-100/70">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="toggleSelectAll"
                  class="rounded text-[#1428A0] focus:ring-[#1428A0] w-4 h-4 cursor-pointer"
                >
              </th>
              <th class="px-4 py-3 text-center w-12 border-r border-slate-200">No</th>
              <th class="px-4 py-3 w-32 border-r border-slate-200 font-bold">품목코드</th>
              <th class="px-4 py-3 w-64 border-r border-slate-200">품목명</th>
              <th class="px-4 py-3 w-56 border-r border-slate-200">공급처명</th>
              <th class="px-4 py-3 text-center w-36 border-r border-slate-200">현재 대기 로케이션</th>
              <th class="px-4 py-3 text-right w-24 border-r border-slate-200">입고 수량</th>
              <th class="px-4 py-3 text-center w-36 border-r border-slate-200">검수 완료일시</th>
              <th class="px-4 py-3 text-center w-24 border-r border-slate-200">담당자</th>
              <th class="px-4 py-3 text-center">액션</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-if="inboundStore.isLoading">
              <td colspan="10" class="px-4 py-12 text-center text-slate-400">
                <div class="flex items-center justify-center gap-2">
                  <Loader2 class="animate-spin h-5 w-5 text-[#1428A0]" />
                  <span>데이터를 가져오고 있습니다...</span>
                </div>
              </td>
            </tr>
            <tr v-else-if="completedInbounds.length === 0">
              <td colspan="10" class="px-4 py-12 text-center text-slate-400">
                적재 대기 중인 입고 완료 자재가 없습니다.
              </td>
            </tr>
            <tr
              v-for="(item, idx) in completedInbounds"
              :key="item.inboundId"
              @click="selectRow(item)"
              class="hover:bg-slate-50/80 cursor-pointer transition-colors whitespace-nowrap"
              :class="{
                'bg-blue-50/70': selectedInbound?.inboundId === item.inboundId,
                'bg-slate-50/20': idx % 2 === 1 && selectedInbound?.inboundId !== item.inboundId
              }"
            >
              <td class="px-4 py-3 text-center border-r border-slate-100" @click.stop>
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(item.inboundId)"
                  @change="toggleSelect(item.inboundId)"
                  class="rounded text-[#1428A0] focus:ring-[#1428A0] w-4 h-4 cursor-pointer"
                >
              </td>
              <td class="px-4 py-3 text-center font-medium text-slate-400 border-r border-slate-100">{{ idx + 1 }}</td>
              <td class="px-4 py-3 font-mono text-slate-800 font-bold border-r border-slate-100">{{ item.itemCode }}</td>
              <td class="px-4 py-3 text-slate-700 border-r border-slate-100 font-medium">{{ item.itemName }}</td>
              <td class="px-4 py-3 text-slate-700 border-r border-slate-100 font-medium">{{ item.partnerName }}</td>
              <td class="px-4 py-3 text-center border-r border-slate-100">
                <span class="font-mono text-[11px] px-2.5 py-0.5 bg-slate-100 rounded text-slate-500 border border-slate-200">
                  {{ item.locationCode }}
                </span>
              </td>
              <td class="px-4 py-3 text-right border-r border-slate-100 font-extrabold text-slate-800">{{ item.inboundQty.toLocaleString() }}</td>
              <td class="px-4 py-3 text-center border-r border-slate-100 text-slate-400 font-mono text-[11px]">{{ formatDateTime(item.createdAt) }}</td>
              <td class="px-4 py-3 text-center border-r border-slate-100 font-semibold text-slate-600">{{ item.workerName || '-' }}</td>
              <td class="px-4 py-3 text-center" @click.stop>
                <button
                  @click="openStackModal(item)"
                  class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded font-bold transition shadow-xs text-[10px] inline-flex items-center gap-1"
                >
                  <Boxes class="w-3.5 h-3.5" />
                  렉 적재
                </button>
              </td>
            </tr>
          </tbody>

          <!-- 테이블 합계 행 (Summary Row) -->
          <tfoot class="bg-blue-50/50 text-slate-800 font-bold border-t border-slate-200">
            <tr class="whitespace-nowrap">
              <td class="px-4 py-3 border-r border-slate-150 text-center">합계</td>
              <td colspan="5" class="px-4 py-3 border-r border-slate-150 text-left text-slate-400 font-normal">
                현재 적재 대기 목록: 총 <span class="font-bold text-slate-800">{{ completedInbounds.length }}</span>건의 자재
              </td>
              <td class="px-4 py-3 text-right border-r border-slate-150 text-[#1428A0] font-extrabold text-sm">
                {{ totalStackQty.toLocaleString() }}
              </td>
              <td colspan="3" class="px-4 py-3"></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <!-- 하단 상세정보 마스터-디테일 패널 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <!-- 탭 헤더 -->
      <div class="px-5 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <div class="flex items-center gap-4 text-xs font-bold text-slate-700">
          <span class="flex items-center gap-1.5 text-[#1428A0] uppercase tracking-wider">
            <Boxes class="w-4.5 h-4.5" />
            적재 자재 정보 상세
          </span>
          <div class="flex border-l border-slate-300 pl-4 space-x-1.5">
            <button
              @click="activeTab = 'item-partner'"
              class="px-3 py-1.5 rounded-md text-xs font-bold transition"
              :class="activeTab === 'item-partner' ? 'bg-[#1428A0] text-white shadow-xs' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100'"
            >
              자재 마스터 스펙
            </button>
            <button
              @click="activeTab = 'location'"
              class="px-3 py-1.5 rounded-md text-xs font-bold transition"
              :class="activeTab === 'location' ? 'bg-[#1428A0] text-white shadow-xs' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100'"
            >
              대기 위치 상세
            </button>
          </div>
        </div>
        <div class="text-[11px] font-mono text-slate-400" v-if="selectedInbound">
          오더 ID: <span class="font-bold text-[#1428A0]">{{ selectedInbound.inboundId }}</span>
        </div>
      </div>

      <!-- 탭 바디 (자연스럽게 늘어남) -->
      <div class="p-6 text-xs bg-white min-h-[140px]">
        <div v-if="!selectedInbound" class="py-8 flex items-center justify-center text-slate-400 font-medium">
          <div class="text-center">
            <Inbox class="w-8 h-8 text-slate-300 mx-auto mb-2" />
            <span>상단 대기 목록에서 행을 선택하시면 해당 입고건의 품목 규격 및 하역 세부 렉 위치가 매핑되어 노출됩니다.</span>
          </div>
        </div>

        <!-- 자재 마스터 정보 -->
        <div v-else-if="activeTab === 'item-partner'" class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="space-y-3.5">
            <h4 class="font-bold text-slate-800 border-b border-slate-100 pb-2 flex items-center gap-2 text-xs">
              <span class="w-1.5 h-3.5 bg-blue-600 rounded-sm"></span>
              자재 규격 상세
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="text-slate-450 font-medium">자재 코드</span>
              <span class="col-span-2 font-mono text-slate-700 font-extrabold">{{ selectedItemDetail?.itemCode || selectedInbound.itemCode }}</span>
              <span class="text-slate-450 font-medium">자재 명칭</span>
              <span class="col-span-2 text-slate-700 font-bold">{{ selectedItemDetail?.itemName || selectedInbound.itemName }}</span>
              <span class="text-slate-450 font-medium">규격 (Spec)</span>
              <span class="col-span-2 text-slate-600 font-mono">{{ selectedItemDetail?.spec || '미지정' }}</span>
              <span class="text-slate-450 font-medium">단위 / 품목 분류</span>
              <span class="col-span-2 text-slate-700 font-semibold">{{ selectedItemDetail?.unit }} / {{ selectedItemDetail?.itemType }}</span>
            </div>
          </div>

          <div class="space-y-3.5">
            <h4 class="font-bold text-slate-800 border-b border-slate-100 pb-2 flex items-center gap-2 text-xs">
              <span class="w-1.5 h-3.5 bg-blue-600 rounded-sm"></span>
              납품사(공급원) 정보
            </h4>
            <div class="grid grid-cols-3 gap-y-2.5 gap-x-2">
              <span class="text-slate-450 font-medium">사업자 정보</span>
              <span class="col-span-2 font-mono text-slate-700 font-semibold">{{ selectedPartnerDetail?.businessNo || '확인 필요' }}</span>
              <span class="text-slate-450 font-medium">공급처명</span>
              <span class="col-span-2 text-slate-700 font-bold">{{ selectedPartnerDetail?.partnerName || selectedInbound.partnerName }}</span>
              <span class="text-slate-450 font-medium">대표 및 연락처</span>
              <span class="col-span-2 text-slate-650 font-semibold">
                {{ selectedPartnerDetail?.representative || '-' }}
                <span v-if="selectedPartnerDetail?.contactPhone" class="font-mono text-slate-500 font-normal"> ({{ selectedPartnerDetail.contactPhone }})</span>
              </span>
            </div>
          </div>
        </div>

        <!-- 대기 위치 상세 정보 -->
        <div v-else-if="activeTab === 'location'" class="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div class="space-y-3.5 col-span-2">
            <h4 class="font-bold text-slate-800 border-b border-slate-100 pb-2 flex items-center gap-2 text-xs">
              <span class="w-1.5 h-3.5 bg-indigo-550 rounded-sm"></span>
              현재 대기(하역) 구역 상세
            </h4>
            <div class="grid grid-cols-4 gap-y-2.5 gap-x-2">
              <span class="text-slate-455 font-medium">로케이션 코드</span>
              <span class="font-mono font-bold text-[#1428A0]">{{ selectedLocationDetail?.locationCode || selectedInbound.locationCode }}</span>
              <span class="text-slate-455 font-medium">임시 창고명</span>
              <span class="text-slate-700 font-semibold">{{ selectedLocationDetail?.warehouseName || '미지정 구역' }}</span>
              <span class="text-slate-455 font-medium">배치 세부 렉(Rack)</span>
              <span class="col-span-3 text-slate-650 font-semibold">
                {{ selectedLocationDetail?.rackRow || '-' }}열 - {{ selectedLocationDetail?.rackColumn || '-' }}단
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 단일 적재 처리 모달 (Modal) -->
    <div v-if="isStackModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity" @click="closeStackModal"></div>

      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden transform transition-all duration-300 scale-100">
          <div class="px-6 py-4.5 bg-slate-50 border-b border-slate-150 flex items-center justify-between">
            <h3 class="text-sm font-bold text-slate-800 flex items-center gap-2">
              <span class="w-1.5 h-4.5 bg-[#1428A0] rounded-sm"></span>
              WMS 창고 렉 로케이션 적재
            </h3>
            <button @click="closeStackModal" class="text-slate-400 hover:text-slate-600 font-bold text-lg">×</button>
          </div>

          <form @submit.prevent="handleStack" class="p-6 space-y-4.5 text-xs">
            <div v-if="stackError" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-xs font-semibold">
              {{ stackError }}
            </div>

            <div class="p-4 bg-slate-50 border border-slate-150 rounded-xl space-y-2">
              <div class="flex justify-between items-center">
                <span class="text-slate-400 font-medium">적재 자재명</span>
                <span class="font-bold text-slate-800 text-right">
                  {{ selectedInbound?.itemName }}
                  <span class="block text-[10px] text-slate-400 font-mono font-normal tracking-wide">{{ selectedInbound?.itemCode }}</span>
                </span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-slate-400 font-medium">적재 수량</span>
                <span class="font-extrabold text-[#1428A0] text-sm">{{ selectedInbound?.inboundQty.toLocaleString() }} EA</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-slate-400 font-medium">현재 하역 로케이션</span>
                <span class="font-mono text-slate-500 bg-slate-100 border border-slate-200 px-1.5 py-0.5 rounded">{{ selectedInbound?.locationCode }}</span>
              </div>
            </div>

            <div class="space-y-1.5">
              <label class="block font-bold text-slate-700" for="modal-target-location">최종 적치 렉 로케이션 코드 *</label>
              <select
                id="modal-target-location"
                v-model="targetLocationCode"
                class="w-full h-10 px-3 bg-white border border-slate-300 rounded-lg outline-none focus:border-[#1428A0] focus:ring-1 focus:ring-[#1428A0] transition"
                required
              >
                <option value="">적재할 로케이션 코드를 선택하세요</option>
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} - {{ loc.rackRow }}{{ loc.rackColumn }}
                </option>
              </select>
              <p class="text-[10px] text-slate-400 leading-normal mt-1">※ 지정한 렉 로케이션의 가용 수량이 실시간으로 증가하며 수불 이력에 로그가 적재됩니다.</p>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <button
                type="button"
                @click="closeStackModal"
                class="h-9 px-4 text-slate-500 bg-slate-100 hover:bg-slate-200 rounded-lg font-bold transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isStackSubmitting"
                class="h-9 px-5 text-white bg-[#1428A0] hover:bg-[#102180] disabled:opacity-70 rounded-lg font-bold transition flex items-center justify-center gap-2"
              >
                <Loader2 v-if="isStackSubmitting" class="animate-spin h-3.5 w-3.5 text-white" />
                적재 확인
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 일괄 적재 처리 모달 (Modal) -->
    <div v-if="isBatchStackModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity" @click="closeBatchStackModal"></div>

      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden transform transition-all duration-300 scale-100">
          <div class="px-6 py-4.5 bg-slate-50 border-b border-slate-150 flex items-center justify-between">
            <h3 class="text-sm font-bold text-slate-800 flex items-center gap-2">
              <span class="w-1.5 h-4.5 bg-[#1428A0] rounded-sm"></span>
              WMS 일괄 창고 적재
            </h3>
            <button @click="closeBatchStackModal" class="text-slate-400 hover:text-slate-600 font-bold text-lg">×</button>
          </div>

          <form @submit.prevent="handleBatchStack" class="p-6 space-y-4.5 text-xs">
            <div v-if="batchStackError" class="p-3 bg-red-50 border border-red-200 text-red-650 rounded-lg font-bold">
              {{ batchStackError }}
            </div>

            <div class="p-4 bg-indigo-50/50 border border-indigo-100 rounded-xl">
              <div class="text-xs font-semibold text-slate-700">
                일괄 적재 대상 품목 건수: <span class="font-extrabold text-[#1428A0] text-sm">{{ selectedIds.length }}</span>건
              </div>
              <p class="text-[10px] text-slate-400 mt-1 leading-normal">
                선택하신 여러 오더의 원자재들을 하나의 대상 렉 로케이션에 동시에 일괄 적재 및 바인딩 처리를 수행합니다.
              </p>
            </div>

            <div class="space-y-1.5">
              <label class="block font-bold text-slate-700" for="modal-batch-target-location">일괄 적치 렉 로케이션 코드 지정 *</label>
              <select
                id="modal-batch-target-location"
                v-model="batchTargetLocationCode"
                class="w-full h-10 px-3 bg-white border border-slate-300 rounded-lg outline-none focus:border-[#1428A0] focus:ring-1 focus:ring-[#1428A0] transition"
                required
              >
                <option value="">일괄 적재할 로케이션 코드를 선택하세요</option>
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} - {{ loc.rackRow }}{{ loc.rackColumn }}
                </option>
              </select>
              <p class="text-[10px] text-slate-400 leading-normal mt-1">※ 선택된 모든 자재들이 해당 렉으로 바인딩 처리되며, 각각의 재고가 일괄 합산됩니다.</p>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <button
                type="button"
                @click="closeBatchStackModal"
                class="h-9 px-4 text-slate-500 bg-slate-100 hover:bg-slate-200 rounded-lg font-bold transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isBatchStackSubmitting"
                class="h-9 px-5 text-white bg-[#1428A0] hover:bg-[#102180] disabled:opacity-70 rounded-lg font-bold transition flex items-center justify-center gap-2"
              >
                <Loader2 v-if="isBatchStackSubmitting" class="animate-spin h-3.5 w-3.5 text-white" />
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
