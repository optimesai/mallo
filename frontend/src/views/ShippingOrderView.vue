<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  Truck,
  Plus,
  Search,
  RefreshCw,
  SlidersHorizontal,
  Package,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  ChevronDown,
  X,
  FileText,
  Calendar,
  User,
  MapPin
} from '@lucide/vue'
import { useShippingStore } from '@/state/shippingStore'
import { useInboundStore } from '@/state/inboundStore'

const shippingStore = useShippingStore()
const inboundStore = useInboundStore()

// State
const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSearchExpanded = ref(true)
const isSubmitting = ref(false)
const selectedShipping = ref<any>(null)

// Modal State
const isRegisterModalOpen = ref(false)
const formShippingNo = ref('')
const formPartnerCode = ref('')
const formItemCode = ref('')
const formRequestQty = ref(1)

// Detail Action State
const vehicleInput = ref('')

// Filter states
const filterShippingNo = ref('')
const filterPartnerName = ref('')
const filterStatus = ref<'ALL' | 'READY' | 'PICKING' | 'SHIPPED'>('ALL')

onMounted(async () => {
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    await Promise.all([
      shippingStore.loadShippings(),
      inboundStore.loadItems('FG'), // 완제품 위주 조회
      inboundStore.loadPartners('CUSTOMER') // 고객사 위주 조회
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터를 불러오는데 실패했습니다.'
  }
}

function showToast(msg: string) {
  successToast.value = msg
  setTimeout(() => {
    successToast.value = null
  }, 4000)
}

async function handleRefresh() {
  await fetchPageData()
  showToast('출하 지시 데이터가 새로고침되었습니다.')
  selectedShipping.value = null
}

function resetFilters() {
  filterShippingNo.value = ''
  filterPartnerName.value = ''
  filterStatus.value = 'ALL'
}

// Filtered Shippings
const filteredShippings = computed(() => {
  return shippingStore.shippings.filter((s) => {
    if (filterStatus.value !== 'ALL' && s.status !== filterStatus.value) {
      return false
    }
    if (filterShippingNo.value.trim() !== '') {
      if (!s.shippingNo.toLowerCase().includes(filterShippingNo.value.trim().toLowerCase())) {
        return false
      }
    }
    if (filterPartnerName.value.trim() !== '') {
      const keyword = filterPartnerName.value.trim().toLowerCase()
      const matchName = s.partnerName?.toLowerCase().includes(keyword)
      const matchCode = s.partnerCode?.toLowerCase().includes(keyword)
      if (!matchName && !matchCode) {
        return false
      }
    }
    return true
  })
})

function openRegisterModal() {
  // Generate random shipping no
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const random = String(Math.floor(1000 + Math.random() * 9000))
  formShippingNo.value = `SH-${year}${month}-${random}`
  formPartnerCode.value = ''
  formItemCode.value = ''
  formRequestQty.value = 1
  isRegisterModalOpen.value = true
}

async function handleRegisterShipping() {
  if (!formShippingNo.value || !formPartnerCode.value || !formItemCode.value || formRequestQty.value < 1) {
    alert('모든 필수 정보를 입력해 주세요.')
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    const request = {
      shippingNo: formShippingNo.value,
      partnerCode: formPartnerCode.value,
      itemCode: formItemCode.value,
      requestQty: formRequestQty.value
    }
    await shippingStore.registerShipping(request)
    showToast('신규 출하 지시가 성공적으로 등록되었습니다.')
    isRegisterModalOpen.value = false
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '출하 지시 등록에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

async function handleAssignPicking() {
  if (!selectedShipping.value) return
  if (!vehicleInput.value.trim()) {
    alert('배정할 차량 번호를 입력해 주세요.')
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    const updated = await shippingStore.assignPicking(selectedShipping.value.shippingId, {
      vehicleNo: vehicleInput.value.trim()
    })
    selectedShipping.value = updated
    showToast('배송 차량 및 피킹 로케이션 배정이 완료되었습니다.')
    vehicleInput.value = ''
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '차량 배정에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

async function handleCompleteShipping() {
  if (!selectedShipping.value) return

  if (!confirm('해당 출하 지시의 최종 출하 완료 처리를 진행하시겠습니까? (완제품 재고가 차감됩니다)')) {
    return
  }

  isSubmitting.value = true
  pageError.value = null
  try {
    const shippingId = selectedShipping.value.shippingId
    await shippingStore.completeShipping(shippingId)
    showToast('출하 완료 및 전산 재고 감산이 성공적으로 완료되었습니다.')
    selectedShipping.value = null
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '출하 완료 처리에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

function selectRow(shipping: any) {
  if (selectedShipping.value?.shippingId === shipping.shippingId) {
    selectedShipping.value = null
  } else {
    selectedShipping.value = shipping
    vehicleInput.value = ''
  }
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<template>
  <div class="space-y-6 max-w-[1600px] mx-auto p-4 md:p-6 text-slate-800">
    <!-- 토스트 알림 -->
    <Transition
      enter-active-class="transform ease-out duration-300 transition"
      enter-from-class="translate-y-2 opacity-0 sm:translate-y-0 sm:translate-x-2"
      enter-to-class="translate-y-0 opacity-100 sm:translate-x-0"
      leave-active-class="transition ease-in duration-100"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="successToast"
        class="fixed top-5 right-5 z-50 flex items-center gap-3 bg-slate-900/90 backdrop-blur-md text-white px-5 py-3.5 rounded-xl shadow-2xl border border-slate-700/50"
      >
        <span class="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
        <p class="text-sm font-semibold">{{ successToast }}</p>
      </div>
    </Transition>

    <!-- 경고 / 에러 배너 -->
    <div
      v-if="pageError"
      class="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-xl flex items-start gap-3 shadow-sm"
    >
      <AlertTriangle class="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
      <div>
        <h4 class="font-bold text-sm">출하 처리 중 주의사항 (에러 발생)</h4>
        <p class="text-xs text-rose-700/90 mt-0.5 whitespace-pre-line">{{ pageError }}</p>
      </div>
    </div>

    <!-- 타이틀 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-black tracking-tight text-slate-900">완제품 출하 지시 관리</h1>
        <p class="text-sm text-slate-500 mt-1">고객사 주문 사양에 맞춰 출하 지시를 신규 등록하고, 피킹 로케이션 배정 및 출하 완료 처리를 관리합니다.</p>
      </div>
      <div class="flex items-center gap-2">
        <button
          @click="handleRefresh"
          class="h-10 px-4 text-xs font-bold bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 hover:border-slate-300 rounded-lg shadow-sm flex items-center gap-2 transition"
        >
          <RefreshCw class="w-4 h-4" /> 새로고침
        </button>
        <button
          @click="openRegisterModal"
          class="h-10 px-4 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg shadow-md flex items-center gap-2 transition"
        >
          <Plus class="w-4 h-4" /> 출하 지시 등록
        </button>
      </div>
    </div>

    <!-- 검색 및 필터 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div
        @click="isSearchExpanded = !isSearchExpanded"
        class="px-5 py-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between cursor-pointer select-none"
      >
        <div class="flex items-center gap-2 text-slate-700">
          <SlidersHorizontal class="w-4.5 h-4.5 text-slate-500" />
          <span class="text-sm font-bold">검색 필터 상세조회</span>
        </div>
        <ChevronDown
          class="w-4.5 h-4.5 text-slate-500 transition-transform duration-200"
          :class="{ 'rotate-180': isSearchExpanded }"
        />
      </div>

      <div v-show="isSearchExpanded" class="p-5 space-y-4 border-t border-slate-100">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- 1) 출하 지시 번호 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">출하 지시 번호</label>
            <div class="relative">
              <input
                v-model="filterShippingNo"
                type="text"
                placeholder="지시번호 입력"
                class="w-full h-10 pl-9 pr-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
              />
              <Search class="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            </div>
          </div>

          <!-- 2) 고객사명 / 코드 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">고객사명 / 코드</label>
            <div class="relative">
              <input
                v-model="filterPartnerName"
                type="text"
                placeholder="고객사명 또는 코드 입력"
                class="w-full h-10 pl-9 pr-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
              />
              <Search class="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            </div>
          </div>

          <!-- 3) 출하 상태 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">출하 상태</label>
            <select
              v-model="filterStatus"
              class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
            >
              <option value="ALL">전체 상태</option>
              <option value="READY">출하 대기 (READY)</option>
              <option value="PICKING">차량/피킹배정 (PICKING)</option>
              <option value="SHIPPED">출하 완료 (SHIPPED)</option>
            </select>
          </div>
        </div>

        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button
            @click="resetFilters"
            class="h-9 px-4 text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition"
          >
            필터 초기화
          </button>
        </div>
      </div>
    </div>

    <!-- 마스터 테이블: 출하 지시 목록 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div class="px-5 py-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
        <span class="text-sm font-bold text-slate-700">출하 지시 목록 (총 {{ filteredShippings.length }}건)</span>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-[1200px] w-full text-left border-collapse table-fixed">
          <colgroup>
            <col class="w-[80px]" />
            <col class="w-[180px]" />
            <col class="w-[200px]" />
            <col class="w-[180px]" />
            <col class="w-[260px]" />
            <col class="w-[120px]" />
            <col class="w-[140px]" />
            <col class="w-[150px]" />
            <col class="w-[120px]" />
          </colgroup>
          <thead>
            <tr class="bg-slate-50/50 border-b border-slate-200 text-xs font-bold text-slate-500 uppercase tracking-wider">
              <th class="px-5 py-3">ID</th>
              <th class="px-5 py-3">출하 지시 번호</th>
              <th class="px-5 py-3">고객사</th>
              <th class="px-5 py-3">완제품 코드</th>
              <th class="px-5 py-3">완제품명</th>
              <th class="px-5 py-3 text-right">요청 수량</th>
              <th class="px-5 py-3 text-center">출하 상태</th>
              <th class="px-5 py-3">배정 차량</th>
              <th class="px-5 py-3">피킹 위치</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-sm">
            <tr
              v-for="shipping in filteredShippings"
              :key="shipping.shippingId"
              @click="selectRow(shipping)"
              class="hover:bg-slate-50 cursor-pointer transition select-none"
              :class="{ 'bg-indigo-50/30': selectedShipping?.shippingId === shipping.shippingId }"
            >
              <!-- ID -->
              <td class="px-5 py-4 font-mono text-xs text-slate-400">#{{ shipping.shippingId }}</td>
              <!-- 출하 지시 번호 -->
              <td class="px-5 py-4 font-bold text-slate-900">{{ shipping.shippingNo }}</td>
              <!-- 고객사 -->
              <td class="px-5 py-4 text-slate-700 truncate" :title="shipping.partnerName">
                {{ shipping.partnerName || shipping.partnerCode }}
              </td>
              <!-- 완제품 코드 -->
              <td class="px-5 py-4 font-mono text-xs text-slate-500">{{ shipping.itemCode }}</td>
              <!-- 완제품명 -->
              <td class="px-5 py-4 font-semibold text-slate-700 truncate" :title="shipping.itemName">
                {{ shipping.itemName }}
              </td>
              <!-- 요청 수량 -->
              <td class="px-5 py-4 text-right font-extrabold text-slate-900">
                {{ shipping.requestQty?.toLocaleString() }} EA
              </td>
              <!-- 출하 상태 -->
              <td class="px-5 py-4 text-center">
                <span
                  class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold border"
                  :class="{
                    'bg-amber-100 border-amber-200 text-amber-800': shipping.status === 'READY',
                    'bg-indigo-100 border-indigo-200 text-indigo-800': shipping.status === 'PICKING',
                    'bg-emerald-100 border-emerald-200 text-emerald-800': shipping.status === 'SHIPPED'
                  }"
                >
                  <span
                    class="w-1.5 h-1.5 rounded-full"
                    :class="{
                      'bg-amber-500': shipping.status === 'READY',
                      'bg-indigo-500': shipping.status === 'PICKING',
                      'bg-emerald-500': shipping.status === 'SHIPPED'
                    }"
                  ></span>
                  {{ shipping.status === 'READY' ? '출하대기' : shipping.status === 'PICKING' ? '피킹중' : '출하완료' }}
                </span>
              </td>
              <!-- 배정 차량 -->
              <td class="px-5 py-4 font-semibold text-slate-700">
                {{ shipping.vehicleNo || '-' }}
              </td>
              <!-- 피킹 위치 -->
              <td class="px-5 py-4 font-mono text-xs text-indigo-600 font-bold">
                {{ shipping.pickingLocationCode || '-' }}
              </td>
            </tr>
            <tr v-if="filteredShippings.length === 0">
              <td colspan="9" class="px-5 py-12 text-center text-slate-400">
                <Truck class="w-8 h-8 text-slate-300 mx-auto mb-2" />
                조건에 맞는 출하 지시 내역이 존재하지 않습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 디테일 패널: 출하 지시 상세 분석 & 처리 -->
    <div
      v-if="selectedShipping"
      class="bg-white rounded-xl border border-slate-200 shadow-lg overflow-hidden animate-slide-up"
    >
      <div class="px-5 py-4 bg-slate-900 text-white flex items-center justify-between">
        <div class="flex items-center gap-2">
          <Truck class="w-5 h-5 text-indigo-400" />
          <h3 class="font-extrabold text-sm">출하 지시 상세 정보 및 처리 작업 (번호: {{ selectedShipping.shippingNo }})</h3>
        </div>
        <button
          @click="selectedShipping = null"
          class="text-slate-400 hover:text-white text-xs font-bold bg-slate-800 px-2.5 py-1 rounded"
        >
          패널 닫기
        </button>
      </div>

      <div class="p-6 space-y-6">
        <!-- 4컬럼 정보 그리드 -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div class="p-4 bg-slate-50 border border-slate-100 rounded-xl space-y-2">
            <span class="text-xs text-slate-400 font-bold block">고객사 정보</span>
            <span class="font-bold text-slate-800 block text-base">{{ selectedShipping.partnerName }}</span>
            <span class="text-xs font-mono text-slate-500 block">{{ selectedShipping.partnerCode }}</span>
          </div>

          <div class="p-4 bg-slate-50 border border-slate-100 rounded-xl space-y-2">
            <span class="text-xs text-slate-400 font-bold block">출하 대상 품목</span>
            <span class="font-bold text-slate-800 block text-base">{{ selectedShipping.itemName }}</span>
            <span class="text-xs font-mono text-slate-500 block">{{ selectedShipping.itemCode }}</span>
          </div>

          <div class="p-4 bg-slate-50 border border-slate-100 rounded-xl space-y-2">
            <span class="text-xs text-slate-400 font-bold block">출하 요청 수량</span>
            <span class="font-extrabold text-slate-900 block text-xl">{{ selectedShipping.requestQty?.toLocaleString() }} EA</span>
          </div>

          <div class="p-4 bg-slate-50 border border-slate-100 rounded-xl space-y-2">
            <span class="text-xs text-slate-400 font-bold block">진행 단계</span>
            <span class="mt-1 block">
              <span
                class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-extrabold border"
                :class="{
                  'bg-amber-100 border-amber-200 text-amber-800': selectedShipping.status === 'READY',
                  'bg-indigo-100 border-indigo-200 text-indigo-800': selectedShipping.status === 'PICKING',
                  'bg-emerald-100 border-emerald-200 text-emerald-800': selectedShipping.status === 'SHIPPED'
                }"
              >
                {{ selectedShipping.status === 'READY' ? '출하 지시 대기' : selectedShipping.status === 'PICKING' ? '피킹 및 상차 진행 중' : '출하 완료 및 재고 차감됨' }}
              </span>
            </span>
          </div>
        </div>

        <!-- 추가 메타데이터 정보 (로케이션, 차량, 날짜, 담당자) -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm bg-slate-50/50 p-4 border border-slate-200/50 rounded-xl">
          <div class="space-y-2">
            <div class="flex items-center gap-2">
              <MapPin class="w-4 h-4 text-slate-400" />
              <span class="text-slate-500 font-medium">배정 피킹 로케이션:</span>
              <span class="font-mono font-bold text-indigo-600">{{ selectedShipping.pickingLocationCode || '미배정 (차량 지정 시 자동 배정)' }}</span>
            </div>
            <div class="flex items-center gap-2">
              <Truck class="w-4 h-4 text-slate-400" />
              <span class="text-slate-500 font-medium">상차 배송 차량번호:</span>
              <span class="font-bold text-slate-700">{{ selectedShipping.vehicleNo || '미배정' }}</span>
            </div>
          </div>
          <div class="space-y-2">
            <div class="flex items-center gap-2">
              <Calendar class="w-4 h-4 text-slate-400" />
              <span class="text-slate-500 font-medium">최종 출하 완료 일시:</span>
              <span class="font-bold text-slate-700">{{ formatDate(selectedShipping.shippedAt) }}</span>
            </div>
            <div class="flex items-center gap-2">
              <User class="w-4 h-4 text-slate-400" />
              <span class="text-slate-500 font-medium">담당 작업자명:</span>
              <span class="font-bold text-slate-700">{{ selectedShipping.workerName || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 단계별 액션 폼 -->
        <div class="pt-4 border-t border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <!-- READY 상태: 차량 배정 입력 폼 -->
          <div v-if="selectedShipping.status === 'READY'" class="w-full flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <div class="flex-1 max-w-md">
              <input
                v-model="vehicleInput"
                type="text"
                placeholder="배정할 차량 번호 입력 (예: 서울 88 가 1234)"
                class="w-full h-10 px-3.5 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
              />
            </div>
            <button
              :disabled="isSubmitting"
              @click="handleAssignPicking"
              class="h-10 px-5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-md flex items-center justify-center gap-2 transition disabled:opacity-50"
            >
              <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
              차량 배정 및 피킹 지시 실행
            </button>
          </div>

          <!-- PICKING 상태: 최종 출하 완료 단추 -->
          <div v-else-if="selectedShipping.status === 'PICKING'" class="w-full flex items-center justify-between">
            <div class="text-xs text-amber-600 font-bold flex items-center gap-1">
              <AlertTriangle class="w-4 h-4" /> 피킹 및 차량 상차가 완료되면 "최종 출하 완료 처리"를 실행하여 재고를 전산 감산하십시오.
            </div>
            <button
              :disabled="isSubmitting"
              @click="handleCompleteShipping"
              class="h-10 px-6 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg shadow-md flex items-center gap-2 transition disabled:opacity-50"
            >
              <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
              최종 출하 완료 처리 (재고 차감)
            </button>
          </div>

          <!-- SHIPPED 상태: 안내 텍스트 -->
          <div v-else class="text-xs text-slate-400 font-bold">
            이 출하 건은 이미 성공적으로 완결 처리되었습니다.
          </div>
        </div>
      </div>
    </div>

    <!-- 출하 지시 등록 모달 -->
    <div
      v-if="isRegisterModalOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4 animate-fade-in"
    >
      <div class="bg-white rounded-2xl border border-slate-100 shadow-2xl w-full max-w-lg overflow-hidden animate-scale-up">
        <!-- 모달 헤더 -->
        <div class="px-6 py-4 bg-slate-900 text-white flex items-center justify-between">
          <div class="flex items-center gap-2">
            <Plus class="w-5 h-5 text-indigo-400" />
            <h3 class="font-extrabold text-sm">신규 완제품 출하 지시 등록</h3>
          </div>
          <button @click="isRegisterModalOpen = false" class="text-slate-400 hover:text-white transition">
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- 모달 바디 -->
        <div class="p-6 space-y-4">
          <!-- 1) 출하 지시 번호 (자동생성 / 수정가능) -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">출하 지시 번호 <span class="text-rose-500">*</span></label>
            <input
              v-model="formShippingNo"
              type="text"
              class="w-full h-10 px-3 bg-slate-50 border border-slate-200 rounded-lg text-sm font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          <!-- 2) 고객사 선택 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">고객사 선택 <span class="text-rose-500">*</span></label>
            <select
              v-model="formPartnerCode"
              class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
            >
              <option value="">고객사를 선택하세요</option>
              <option
                v-for="partner in inboundStore.partners"
                :key="partner.partnerCode"
                :value="partner.partnerCode"
              >
                {{ partner.partnerName }} ({{ partner.partnerCode }})
              </option>
            </select>
          </div>

          <!-- 3) 품목 선택 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">완제품 품목 선택 <span class="text-rose-500">*</span></label>
            <select
              v-model="formItemCode"
              class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm focus:outline-none focus:border-indigo-500"
            >
              <option value="">품목을 선택하세요</option>
              <option
                v-for="item in inboundStore.items"
                :key="item.itemCode"
                :value="item.itemCode"
              >
                {{ item.itemName }} ({{ item.itemCode }})
              </option>
            </select>
          </div>

          <!-- 4) 요청 수량 -->
          <div>
            <label class="block text-xs font-bold text-slate-600 mb-1.5">출하 요청 수량 (EA) <span class="text-rose-500">*</span></label>
            <input
              v-model.number="formRequestQty"
              type="number"
              min="1"
              class="w-full h-10 px-3 bg-white border border-slate-200 rounded-lg text-sm font-bold focus:outline-none focus:border-indigo-500"
            />
          </div>
        </div>

        <!-- 모달 푸터 -->
        <div class="px-6 py-4 bg-slate-50 border-t border-slate-100 flex justify-end gap-2">
          <button
            @click="isRegisterModalOpen = false"
            class="h-10 px-4 text-xs font-bold bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-lg transition"
          >
            취소
          </button>
          <button
            :disabled="isSubmitting"
            @click="handleRegisterShipping"
            class="h-10 px-5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-md flex items-center gap-2 transition disabled:opacity-50"
          >
            <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin" />
            지시 등록 완료
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-slide-up {
  animation: slide-up 0.25s ease-out;
}

.animate-fade-in {
  animation: fade-in 0.2s ease-out;
}

.animate-scale-up {
  animation: scale-up 0.2s ease-out;
}

@keyframes slide-up {
  from {
    transform: translateY(12px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scale-up {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
