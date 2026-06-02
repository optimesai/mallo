<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
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
const filterStatus = ref<'ALL' | 'READY' | 'COMPLETED'>('ALL')
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

// 통계 데이터 계산
const stats = computed(() => {
  const total = inboundStore.inbounds.length
  const ready = inboundStore.inbounds.filter(i => i.status === 'READY').length
  const completed = inboundStore.inbounds.filter(i => i.status === 'COMPLETED').length
  return { total, ready, completed }
})

// 필터링된 입고 목록 계산
const filteredInbounds = computed(() => {
  return inboundStore.inbounds.filter(item => {
    // 1) 상태 필터
    if (filterStatus.value !== 'ALL' && item.status !== filterStatus.value) {
      return false
    }
    // 2) 품목 검색 (코드 or 명)
    if (filterItem.value.trim() !== '') {
      const keyword = filterItem.value.toLowerCase()
      const matchCode = item.itemCode?.toLowerCase().includes(keyword)
      const matchName = item.itemName?.toLowerCase().includes(keyword)
      if (!matchCode && !matchName) return false
    }
    // 3) 거래처 검색 (코드 or 명)
    if (filterPartner.value.trim() !== '') {
      const keyword = filterPartner.value.toLowerCase()
      const matchCode = item.partnerCode?.toLowerCase().includes(keyword)
      const matchName = item.partnerName?.toLowerCase().includes(keyword)
      if (!matchCode && !matchName) return false
    }
    // 4) 기간 필터
    if (filterDateStart.value) {
      if (item.inboundDate < filterDateStart.value) return false
    }
    if (filterDateEnd.value) {
      if (item.inboundDate > filterDateEnd.value) return false
    }

    return true
  })
})

// 합계 계산 (필터링된 수량 총합)
const totalInboundQty = computed(() => {
  return filteredInbounds.value.reduce((acc, curr) => acc + (curr.inboundQty || 0), 0)
})

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await Promise.all([
      inboundStore.loadInbounds(),
      inboundStore.loadItems('RAW'),
      inboundStore.loadPartners('SUPPLIER'),
      inboundStore.loadLocations()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터를 불러오는데 실패했습니다.'
  }
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
    // 선택된 상세 객체 업데이트
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
  <div class="flex flex-col space-y-5 h-full">
    <!-- 헤더 영역 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 shrink-0">
      <div>
        <h1 class="text-xl font-extrabold text-slate-800 tracking-tight flex items-center gap-2">
          <span class="w-1.5 h-6 bg-[#1428A0] rounded-sm"></span>
          입고 등록 및 검수 관리
        </h1>
        <p class="text-xs text-slate-500 mt-0.5 font-medium">원자재 입고 예정 오더를 전산화하고 검수 완료(COMPLETED) 및 로케이션 바인딩 대기 상태로 이관합니다.</p>
      </div>
      <!-- 토스트 메시지 -->
      <div v-if="successToast" class="bg-emerald-50 border border-emerald-200 text-emerald-800 px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-2 shadow-sm animate-fade-in">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
        {{ successToast }}
      </div>
    </div>

    <!-- 에러 배너 -->
    <div v-if="pageError" class="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-2.5 rounded-lg text-xs flex items-center justify-between shrink-0">
      <span>{{ pageError }}</span>
      <button @click="pageError = null" class="text-rose-500 hover:text-rose-700 font-bold">×</button>
    </div>

    <!-- 접이식 검색 조건 패널 (Hansol WMS 스타일) -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden shrink-0">
      <div class="px-5 py-3 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
        <span class="text-xs font-bold text-slate-700 flex items-center gap-1.5">
          <svg class="w-4 h-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
          </svg>
          조회 검색 조건
        </span>
        <button
          @click="isSearchExpanded = !isSearchExpanded"
          class="p-1 hover:bg-slate-200 rounded text-slate-500 transition-colors"
        >
          <svg
            class="w-4 h-4 transform transition-transform duration-250"
            :class="{ 'rotate-180': !isSearchExpanded }"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2.5"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      <!-- 확장 시 필터 폼 -->
      <div v-show="isSearchExpanded" class="p-5 border-t border-slate-150 bg-white grid grid-cols-1 md:grid-cols-4 gap-4 text-xs">
        <div class="space-y-1.5">
          <label class="block font-bold text-slate-600">원자재 품목</label>
          <input
            v-model="filterItem"
            placeholder="품목명 또는 코드 검색"
            class="w-full h-8 px-2.5 bg-slate-50 border border-slate-200 rounded outline-none focus:border-[#1428A0] focus:bg-white transition"
          >
        </div>
        <div class="space-y-1.5">
          <label class="block font-bold text-slate-600">공급처(거래처)</label>
          <input
            v-model="filterPartner"
            placeholder="거래처명 또는 코드 검색"
            class="w-full h-8 px-2.5 bg-slate-50 border border-slate-200 rounded outline-none focus:border-[#1428A0] focus:bg-white transition"
          >
        </div>
        <div class="space-y-1.5">
          <label class="block font-bold text-slate-600">진행 상태</label>
          <select
            v-model="filterStatus"
            class="w-full h-8 px-2.5 bg-slate-50 border border-slate-200 rounded outline-none focus:border-[#1428A0] focus:bg-white transition"
          >
            <option value="ALL">전체 상태</option>
            <option value="READY">입고 대기 (READY)</option>
            <option value="COMPLETED">검수 완료 (COMPLETED)</option>
          </select>
        </div>
        <div class="space-y-1.5">
          <label class="block font-bold text-slate-600">입고 예정일자</label>
          <div class="flex items-center gap-2">
            <input
              type="date"
              v-model="filterDateStart"
              class="w-full h-8 px-2 bg-slate-50 border border-slate-200 rounded outline-none focus:border-[#1428A0] focus:bg-white text-[11px]"
            >
            <span class="text-slate-400">~</span>
            <input
              type="date"
              v-model="filterDateEnd"
              class="w-full h-8 px-2 bg-slate-50 border border-slate-200 rounded outline-none focus:border-[#1428A0] focus:bg-white text-[11px]"
            >
          </div>
        </div>

        <div class="md:col-span-4 flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button
            @click="resetFilters"
            class="h-8 px-3.5 bg-slate-100 hover:bg-slate-200 text-slate-600 font-bold rounded transition"
          >
            초기화
          </button>
          <button
            @click="fetchPageData"
            class="h-8 px-4 bg-[#1428A0] hover:bg-[#102180] text-white font-bold rounded shadow-sm transition"
          >
            조회
          </button>
        </div>
      </div>
    </div>

    <!-- 메인 그리드 및 액션 툴바 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col flex-grow min-h-[300px] overflow-hidden">
      <!-- 액션 버튼 툴바 -->
      <div class="px-5 py-3 bg-slate-50 border-b border-slate-200 flex items-center justify-between shrink-0">
        <!-- 다중 선택 액션 -->
        <div class="flex items-center gap-2">
          <button
            @click="handleBatchComplete"
            :disabled="selectedIds.length === 0"
            class="h-8 px-3 text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded shadow-sm transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1.5"
          >
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            선택 검수완료
          </button>
          <button
            @click="handleBatchDelete"
            :disabled="selectedIds.length === 0"
            class="h-8 px-3 text-xs bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 font-bold rounded transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1.5"
          >
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            선택 삭제
          </button>
          <span class="text-xs text-slate-400 ml-1 font-semibold" v-if="selectedIds.length > 0">
            총 {{ selectedIds.length }}개 선택됨
          </span>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="openRegisterModal"
            class="h-8 px-3.5 bg-[#1428A0] hover:bg-[#102180] text-white text-xs font-bold rounded shadow-sm transition flex items-center gap-1.5"
          >
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            입고 예정 등록
          </button>
        </div>
      </div>

      <!-- 고밀도 데이터 테이블 영역 (Hansol WMS 스타일) -->
      <div class="flex-grow overflow-y-auto">
        <table class="w-full text-left text-xs text-slate-600 border-collapse">
          <thead class="bg-slate-100 text-slate-500 font-bold uppercase border-b border-slate-200 sticky top-0 z-10">
            <tr>
              <th class="px-4 py-2.5 text-center w-12 bg-slate-100 border-r border-slate-200">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  @change="toggleSelectAll"
                  class="rounded text-[#1428A0] focus:ring-[#1428A0] w-3.5 h-3.5 cursor-pointer"
                >
              </th>
              <th class="px-4 py-2.5 text-center w-12 border-r border-slate-200">No</th>
              <th class="px-4 py-2.5 text-center w-24 border-r border-slate-200">상태</th>
              <th class="px-4 py-2.5 w-32 border-r border-slate-200">품목코드</th>
              <th class="px-4 py-2.5 w-56 border-r border-slate-200">품목명</th>
              <th class="px-4 py-2.5 w-44 border-r border-slate-200">공급처명</th>
              <th class="px-4 py-2.5 text-center w-28 border-r border-slate-200">예정 로케이션</th>
              <th class="px-4 py-2.5 text-right w-24 border-r border-slate-200">예정 수량</th>
              <th class="px-4 py-2.5 text-center w-28 border-r border-slate-200">입고 예정일</th>
              <th class="px-4 py-2.5 text-center w-36 border-r border-slate-200">등록 일시</th>
              <th class="px-4 py-2.5 text-center w-20 border-r border-slate-200">작업자</th>
              <th class="px-4 py-2.5 text-center w-36">액션</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <!-- 로딩 상태 -->
            <tr v-if="inboundStore.isLoading">
              <td colspan="12" class="px-4 py-12 text-center text-slate-400">
                <div class="flex items-center justify-center gap-2">
                  <svg class="animate-spin h-4 w-4 text-[#1428A0]" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>입고 오더 정보를 연동 중입니다...</span>
                </div>
              </td>
            </tr>
            <!-- 빈 데이터 -->
            <tr v-else-if="filteredInbounds.length === 0">
              <td colspan="12" class="px-4 py-12 text-center text-slate-400 font-medium">
                조회된 입고 데이터 오더가 없습니다.
              </td>
            </tr>
            <!-- 데이터 반복 렌더링 -->
            <tr
              v-for="(item, idx) in filteredInbounds"
              :key="item.inboundId"
              @click="selectRow(item)"
              class="hover:bg-slate-50 cursor-pointer transition-colors"
              :class="{
                'bg-slate-50 border-l-4 border-l-[#1428A0]': selectedInbound?.inboundId === item.inboundId,
                'bg-slate-50/40': idx % 2 === 1 && selectedInbound?.inboundId !== item.inboundId
              }"
            >
              <!-- 체크박스 -->
              <td class="px-4 py-2 text-center border-r border-slate-200" @click.stop>
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(item.inboundId)"
                  @change="toggleSelect(item.inboundId)"
                  class="rounded text-[#1428A0] focus:ring-[#1428A0] w-3.5 h-3.5 cursor-pointer"
                >
              </td>
              <!-- 번호 -->
              <td class="px-4 py-2 text-center font-medium text-slate-400 border-r border-slate-200">{{ idx + 1 }}</td>
              <!-- 상태 뱃지 -->
              <td class="px-4 py-2 text-center border-r border-slate-200">
                <span
                  v-if="item.status === 'READY'"
                  class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-extrabold bg-blue-50 text-blue-700 border border-blue-150"
                >
                  <span class="w-1.5 h-1.5 mr-1 rounded-full bg-blue-500 animate-ping"></span>
                  대기 (READY)
                </span>
                <span
                  v-else
                  class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-150"
                >
                  완료 (COMPLETED)
                </span>
              </td>
              <!-- 품목코드 -->
              <td class="px-4 py-2 font-mono text-slate-800 font-semibold border-r border-slate-200">{{ item.itemCode }}</td>
              <!-- 품목명 -->
              <td class="px-4 py-2 text-slate-700 border-r border-slate-200 truncate max-w-[220px]" :title="item.itemName">{{ item.itemName }}</td>
              <!-- 공급처명 -->
              <td class="px-4 py-2 text-slate-700 border-r border-slate-200 truncate max-w-[170px]" :title="item.partnerName">{{ item.partnerName }}</td>
              <!-- 예정 로케이션 -->
              <td class="px-4 py-2 text-center border-r border-slate-200 font-mono text-slate-600">{{ item.locationCode }}</td>
              <!-- 예정 수량 -->
              <td class="px-4 py-2 text-right border-r border-slate-200 font-bold text-slate-800">{{ item.inboundQty.toLocaleString() }}</td>
              <!-- 예정일 -->
              <td class="px-4 py-2 text-center border-r border-slate-200 text-slate-500 font-medium">{{ formatDate(item.inboundDate) }}</td>
              <!-- 등록 일시 -->
              <td class="px-4 py-2 text-center border-r border-slate-200 text-slate-400 font-mono text-[11px]">{{ formatDateTime(item.createdAt) }}</td>
              <!-- 작업자 -->
              <td class="px-4 py-2 text-center border-r border-slate-200 font-medium text-slate-600">{{ item.workerName || '-' }}</td>
              <!-- 액션 -->
              <td class="px-4 py-2 text-center" @click.stop>
                <div class="flex items-center justify-center gap-1.5">
                  <template v-if="item.status === 'READY'">
                    <button
                      @click="handleComplete(item.inboundId)"
                      class="px-2 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-[10px] font-bold shadow-sm transition"
                    >
                      검수 완료
                    </button>
                    <button
                      @click="handleDelete(item.inboundId)"
                      class="px-2 py-1 bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 rounded text-[10px] font-bold transition"
                    >
                      삭제
                    </button>
                  </template>
                  <template v-else>
                    <button
                      @click="navigateToStack"
                      class="px-2 py-1 bg-indigo-50 hover:bg-indigo-100 text-[#1428A0] border border-indigo-200 rounded text-[10px] font-bold transition inline-flex items-center gap-1"
                    >
                      창고 적재
                      <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                      </svg>
                    </button>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>

          <!-- 테이블 합계 행 (Summary Row) -->
          <tfoot class="bg-blue-50/50 text-slate-800 font-bold border-t border-slate-200 sticky bottom-0 z-10">
            <tr>
              <td class="px-4 py-2.5 border-r border-slate-200 text-center">합계</td>
              <td colspan="6" class="px-4 py-2.5 border-r border-slate-200 text-left text-slate-400 font-normal">
                현재 목록: 총 <span class="font-bold text-slate-800">{{ filteredInbounds.length }}</span>건의 오더
              </td>
              <td class="px-4 py-2.5 text-right border-r border-slate-200 text-[#1428A0] font-extrabold text-[13px]">
                {{ totalInboundQty.toLocaleString() }}
              </td>
              <td colspan="4" class="px-4 py-2.5"></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <!-- 하단 상세정보 마스터-디테일 패널 (WMS 스타일) -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col h-60 shrink-0 overflow-hidden">
      <!-- 상세 탭 헤더 -->
      <div class="px-5 py-2.5 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <div class="flex items-center gap-4 text-xs font-bold text-slate-700">
          <span class="flex items-center gap-1.5 text-[#1428A0]">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8 7v8a2 2 0 002 2h6M8 7V5a2 2 0 012-2h4a2 2 0 012 2v2M8 7H6a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V9a2 2 0 00-2-2h-2" />
            </svg>
            오더 세부내역
          </span>
          <div class="flex border-l border-slate-300 pl-4 space-x-1">
            <button
              @click="activeTab = 'item-partner'"
              class="px-3 py-1 rounded text-xs transition"
              :class="activeTab === 'item-partner' ? 'bg-[#1428A0] text-white' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100'"
            >
              품목 & 거래처 정보
            </button>
            <button
              @click="activeTab = 'management'"
              class="px-3 py-1 rounded text-xs transition"
              :class="activeTab === 'management' ? 'bg-[#1428A0] text-white' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-100'"
            >
              관리 정보
            </button>
          </div>
        </div>
        <div class="text-[10px] text-slate-400" v-if="selectedInbound">
          선택 오더 ID: <span class="font-bold text-slate-600 font-mono">{{ selectedInbound.inboundId }}</span>
        </div>
      </div>

      <!-- 상세정보 바디 -->
      <div class="flex-grow p-5 overflow-y-auto text-xs">
        <div v-if="!selectedInbound" class="h-full flex items-center justify-center text-slate-400">
          <div class="text-center">
            <svg class="w-8 h-8 text-slate-300 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122" />
            </svg>
            <span>상단 목록에서 입고 오더 행을 선택하시면 렉 정보 및 기준 상세 내용이 노출됩니다.</span>
          </div>
        </div>

        <!-- 품목 & 거래처 정보 탭 -->
        <div v-else-if="activeTab === 'item-partner'" class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-3">
            <h4 class="font-bold text-slate-800 border-b border-slate-100 pb-1.5 flex items-center gap-1.5">
              <span class="w-1 h-3 bg-blue-500 rounded-sm"></span>
              자재 마스터 스펙
            </h4>
            <div class="grid grid-cols-3 gap-y-2 gap-x-1">
              <span class="text-slate-400">품목코드</span>
              <span class="col-span-2 font-mono text-slate-700 font-bold">{{ selectedItemDetail?.itemCode || selectedInbound.itemCode }}</span>
              <span class="text-slate-400">품목명</span>
              <span class="col-span-2 text-slate-700 font-semibold">{{ selectedItemDetail?.itemName || selectedInbound.itemName }}</span>
              <span class="text-slate-400">규격 (Spec)</span>
              <span class="col-span-2 text-slate-600 font-mono">{{ selectedItemDetail?.spec || '미지정' }}</span>
              <span class="text-slate-400">관리 단위 / 분류</span>
              <span class="col-span-2 text-slate-700 font-medium">{{ selectedItemDetail?.unit }} / {{ selectedItemDetail?.itemType }}</span>
              <span class="text-slate-400">안전 재고 수량</span>
              <span class="col-span-2 text-rose-600 font-extrabold">{{ selectedItemDetail?.safetyStock.toLocaleString() }} EA</span>
            </div>
          </div>

          <div class="space-y-3">
            <h4 class="font-bold text-slate-800 border-b border-slate-100 pb-1.5 flex items-center gap-1.5">
              <span class="w-1 h-3 bg-blue-500 rounded-sm"></span>
              공급업체 상세정보
            </h4>
            <div class="grid grid-cols-3 gap-y-2 gap-x-1">
              <span class="text-slate-400">사업자등록번호</span>
              <span class="col-span-2 font-mono text-slate-700 font-semibold">{{ selectedPartnerDetail?.businessNo || '확인 대기' }}</span>
              <span class="text-slate-400">거래처명</span>
              <span class="col-span-2 text-slate-700 font-bold">{{ selectedPartnerDetail?.partnerName || selectedInbound.partnerName }}</span>
              <span class="text-slate-400">대표자 성명</span>
              <span class="col-span-2 text-slate-700">{{ selectedPartnerDetail?.representative || '-' }}</span>
              <span class="text-slate-400">담당 연락처</span>
              <span class="col-span-2 text-slate-600 font-mono">{{ selectedPartnerDetail?.contactPhone || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 관리 정보 탭 -->
        <div v-else-if="activeTab === 'management'" class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-3">
            <h4 class="font-bold text-slate-800 border-b border-slate-100 pb-1.5 flex items-center gap-1.5">
              <span class="w-1 h-3 bg-indigo-400 rounded-sm"></span>
              시스템 및 처리 상태
            </h4>
            <div class="grid grid-cols-3 gap-y-2 gap-x-1">
              <span class="text-slate-400">전산 오더 번호</span>
              <span class="col-span-2 font-mono text-[#1428A0] font-bold">WMS-INB-{{ selectedInbound.inboundId }}</span>
              <span class="text-slate-400">작업 진행 상태</span>
              <span class="col-span-2 font-semibold">
                {{ selectedInbound.status === 'READY' ? '입고 대기 및 실물 검수 전' : '실물 입고 완료 및 창고 적재 대기' }}
              </span>
              <span class="text-slate-400">입고 전산 등록</span>
              <span class="col-span-2 font-mono text-slate-600">{{ formatDateTime(selectedInbound.createdAt) }}</span>
            </div>
          </div>

          <div class="space-y-3">
            <h4 class="font-bold text-slate-800 border-b border-slate-100 pb-1.5 flex items-center gap-1.5">
              <span class="w-1 h-3 bg-indigo-400 rounded-sm"></span>
              창고 임시 배치 정보
            </h4>
            <div class="grid grid-cols-3 gap-y-2 gap-x-1">
              <span class="text-slate-400">임시 로케이션 코드</span>
              <span class="col-span-2 font-mono font-bold text-slate-700">{{ selectedLocationDetail?.locationCode || selectedInbound.locationCode }}</span>
              <span class="text-slate-400">창고 및 구역명</span>
              <span class="col-span-2 text-slate-700 font-semibold">{{ selectedLocationDetail?.warehouseName || '미지정 임시 구역' }}</span>
              <span class="text-slate-400">창고 상세 렉 정보</span>
              <span class="col-span-2 text-slate-600">
                열: {{ selectedLocationDetail?.rackRow || '대기' }} / 단: {{ selectedLocationDetail?.rackColumn || '대기' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 입고 예정 등록 모달창 (Modal) -->
    <div v-if="isRegisterModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity" @click="closeRegisterModal"></div>

      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden transform transition-all duration-300 scale-100">
          <div class="px-6 py-4 bg-slate-50 border-b border-slate-150 flex items-center justify-between">
            <h3 class="text-sm font-bold text-slate-800 flex items-center gap-2">
              <span class="w-1 h-4 bg-[#1428A0] rounded-sm"></span>
              신규 원자재 입고 오더 예정 등록
            </h3>
            <button @click="closeRegisterModal" class="text-slate-400 hover:text-slate-600 font-bold text-lg">×</button>
          </div>

          <form @submit.prevent="handleRegister" class="p-6 space-y-4 text-xs">
            <div v-if="registerError" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-xs font-semibold">
              {{ registerError }}
            </div>

            <div class="space-y-1.5">
              <label class="block font-bold text-slate-700" for="modal-item">입고 대상 원자재 품목 *</label>
              <select
                id="modal-item"
                v-model="selectedItemCode"
                class="w-full h-9 px-3 bg-white border border-slate-300 rounded outline-none focus:border-[#1428A0] transition"
                required
              >
                <option value="">품목을 지정해주세요</option>
                <option v-for="item in inboundStore.items" :key="item.itemId" :value="item.itemCode">
                  [{{ item.itemCode }}] {{ item.itemName }} (단위: {{ item.unit }} / 규격: {{ item.spec }})
                </option>
              </select>
            </div>

            <div class="space-y-1.5">
              <label class="block font-bold text-slate-700" for="modal-partner">납품 공급사 (거래처) *</label>
              <select
                id="modal-partner"
                v-model="selectedPartnerCode"
                class="w-full h-9 px-3 bg-white border border-slate-300 rounded outline-none focus:border-[#1428A0] transition"
                required
              >
                <option value="">공급사를 지정해주세요</option>
                <option v-for="p in inboundStore.partners" :key="p.partnerId" :value="p.partnerCode">
                  [{{ p.partnerCode }}] {{ p.partnerName }} (대표: {{ p.representative }})
                </option>
              </select>
            </div>

            <div class="space-y-1.5">
              <label class="block font-bold text-slate-700" for="modal-location">하역 및 입고대기 임시 로케이션 *</label>
              <select
                id="modal-location"
                v-model="selectedLocationCode"
                class="w-full h-9 px-3 bg-white border border-slate-300 rounded outline-none focus:border-[#1428A0] transition"
                required
              >
                <option value="">하역장 또는 임시 로케이션을 선택하세요</option>
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} - {{ loc.rackRow }}{{ loc.rackColumn }}
                </option>
              </select>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1.5">
                <label class="block font-bold text-slate-700" for="modal-qty">입고 요청 수량 (QTY) *</label>
                <input
                  id="modal-qty"
                  type="number"
                  v-model="inboundQty"
                  min="1"
                  placeholder="예: 100"
                  class="w-full h-9 px-3 bg-white border border-slate-300 rounded outline-none focus:border-[#1428A0] transition"
                  required
                >
              </div>

              <div class="space-y-1.5">
                <label class="block font-bold text-slate-700" for="modal-date">입고 예정일자 *</label>
                <input
                  id="modal-date"
                  type="date"
                  v-model="inboundDate"
                  class="w-full h-9 px-3 bg-white border border-slate-300 rounded outline-none focus:border-[#1428A0] transition"
                  required
                >
              </div>
            </div>

            <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <button
                type="button"
                @click="closeRegisterModal"
                class="h-8 px-4 text-slate-500 bg-slate-100 hover:bg-slate-200 rounded font-bold transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isRegisterSubmitting"
                class="h-8 px-5 text-white bg-[#1428A0] hover:bg-[#102180] disabled:opacity-70 rounded font-bold transition flex items-center justify-center gap-1.5"
              >
                <span v-if="isRegisterSubmitting" class="animate-spin h-3 w-3 border-2 border-white border-t-transparent rounded-full"></span>
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
