<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useInboundStore } from '@/state/inboundStore'
import type { InboundCreateRequest } from '@/api/inboundApi'

const inboundStore = useInboundStore()
const router = useRouter()

// 필터 및 검색 상태
const searchKeyword = ref('')
const statusFilter = ref<'ALL' | 'READY' | 'COMPLETED'>('ALL')

// 등록 모달 상태
const isRegisterModalOpen = ref(false)
const selectedItemCode = ref('')
const selectedPartnerCode = ref('')
const selectedLocationCode = ref('')
const inboundQty = ref<number>(1)
const inboundDate = ref(new Date().toISOString().split('T')[0])
const registerError = ref<string | null>(null)
const isRegisterSubmitting = ref(false)

// 로딩 및 일반 에러 상태
const pageError = ref<string | null>(null)

// 통계 데이터 계산
const stats = computed(() => {
  const total = inboundStore.inbounds.length
  const ready = inboundStore.inbounds.filter(i => i.status === 'READY').length
  const completed = inboundStore.inbounds.filter(i => i.status === 'COMPLETED').length
  return { total, ready, completed }
})

// 필터링된 입고 목록
const filteredInbounds = computed(() => {
  return inboundStore.inbounds.filter(item => {
    // 상태 필터링
    if (statusFilter.value !== 'ALL' && item.status !== statusFilter.value) {
      return false
    }
    // 키워드 필터링 (품목코드, 품목명, 거래처코드, 거래처명)
    if (searchKeyword.value.trim() !== '') {
      const keyword = searchKeyword.value.toLowerCase()
      const matchItemCode = item.itemCode?.toLowerCase().includes(keyword)
      const matchItemName = item.itemName?.toLowerCase().includes(keyword)
      const matchPartnerCode = item.partnerCode?.toLowerCase().includes(keyword)
      const matchPartnerName = item.partnerName?.toLowerCase().includes(keyword)
      return matchItemCode || matchItemName || matchPartnerCode || matchPartnerName
    }
    return true
  })
})

onMounted(async () => {
  try {
    pageError.value = null
    await Promise.all([
      inboundStore.loadInbounds(),
      inboundStore.loadItems('RAW'), // 입고 자재는 주로 원자재(RAW)
      inboundStore.loadPartners('SUPPLIER'), // 거래처는 공급업체(SUPPLIER)
      inboundStore.loadLocations() // 입고 예정 로케이션
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터 로드에 실패했습니다.'
  }
})

// 입고 예정 등록 처리
async function handleRegister() {
  registerError.value = null
  if (!selectedItemCode.value) {
    registerError.value = '품목을 선택해주세요.'
    return
  }
  if (!selectedPartnerCode.value) {
    registerError.value = '거래처를 선택해주세요.'
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
    closeRegisterModal()
  } catch (err) {
    registerError.value = err instanceof Error ? err.message : '등록에 실패했습니다.'
  } finally {
    isRegisterSubmitting.value = false
  }
}

// 입고 완료 처리 (검수 완료)
async function handleComplete(id: number) {
  if (!confirm('해당 건의 검수를 완료하고 입고 확정하시겠습니까?')) return
  try {
    pageError.value = null
    await inboundStore.completeInbound(id)
  } catch (err) {
    alert(err instanceof Error ? err.message : '입고 완료 처리에 실패했습니다.')
  }
}

// 입고 예정 삭제 처리
async function handleDelete(id: number) {
  if (!confirm('해당 입고 예정 내역을 삭제하시겠습니까?')) return
  try {
    pageError.value = null
    await inboundStore.deleteInbound(id)
  } catch (err) {
    alert(err instanceof Error ? err.message : '입고 삭제에 실패했습니다.')
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
  <div class="space-y-6">
    <!-- 헤더 영역 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-slate-800 tracking-tight">입고 등록 및 검수 관리</h1>
        <p class="text-sm text-slate-500 mt-1">공급사로부터의 입고 예정 건을 등록하고 실재고 적재 대기(COMPLETED) 상태로 검수 처리합니다.</p>
      </div>
      <div>
        <button
          @click="openRegisterModal"
          class="inline-flex items-center px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors duration-150 gap-2"
        >
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          입고 예정 등록
        </button>
      </div>
    </div>

    <!-- 에러 메시지 배너 -->
    <div v-if="pageError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex items-center justify-between">
      <span>{{ pageError }}</span>
      <button @click="pageError = null" class="text-red-500 hover:text-red-700 font-bold">×</button>
    </div>

    <!-- 요약 통계 카드 섹션 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-5">
      <!-- 전체 카드 -->
      <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4 transition-all duration-200 hover:shadow-md">
        <div class="p-3 bg-slate-100 rounded-lg text-slate-600">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
        </div>
        <div>
          <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">전체 입고 요청</p>
          <p class="text-2xl font-extrabold text-slate-800 mt-1">{{ stats.total }} 건</p>
        </div>
      </div>
      <!-- 대기 카드 -->
      <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4 transition-all duration-200 hover:shadow-md">
        <div class="p-3 bg-indigo-50 rounded-lg text-indigo-600">
          <svg class="w-6 h-6 animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div>
          <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">입고 대기 (READY)</p>
          <p class="text-2xl font-extrabold text-indigo-600 mt-1">{{ stats.ready }} 건</p>
        </div>
      </div>
      <!-- 완료 카드 -->
      <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-sm flex items-center gap-4 transition-all duration-200 hover:shadow-md">
        <div class="p-3 bg-emerald-50 rounded-lg text-emerald-600">
          <svg class="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div>
          <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">검수 완료 (COMPLETED)</p>
          <p class="text-2xl font-extrabold text-emerald-600 mt-1">{{ stats.completed }} 건</p>
        </div>
      </div>
    </div>

    <!-- 필터 및 데이터 테이블 영역 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <!-- 검색 및 상태 필터 바 -->
      <div class="p-5 border-b border-slate-200 bg-slate-50 flex flex-col sm:flex-row sm:items-center gap-4 justify-between">
        <div class="flex items-center gap-3">
          <span class="text-sm font-semibold text-slate-700">입고 내역 목록</span>
          <span class="px-2.5 py-0.5 bg-slate-200 text-slate-800 text-xs font-bold rounded-full">
            {{ filteredInbounds.length }}개 검색됨
          </span>
        </div>
        <div class="flex flex-col sm:flex-row gap-3">
          <!-- 상태 필터 -->
          <select
            v-model="statusFilter"
            class="h-9 px-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
          >
            <option value="ALL">전체 상태</option>
            <option value="READY">입고 대기 (READY)</option>
            <option value="COMPLETED">검수 완료 (COMPLETED)</option>
          </select>
          <!-- 키워드 검색 -->
          <div class="relative">
            <input
              v-model="searchKeyword"
              placeholder="품목명, 코드, 거래처 검색..."
              class="w-full sm:w-64 h-9 pl-8 pr-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
            >
            <span class="absolute left-2.5 top-2.5 text-slate-400">
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </span>
          </div>
        </div>
      </div>

      <!-- 테이블 본문 -->
      <div class="overflow-x-auto">
        <table class="w-full text-left text-sm text-slate-600 border-collapse">
          <thead class="bg-slate-100 text-slate-500 text-xs font-bold uppercase tracking-wider border-b border-slate-200">
            <tr>
              <th class="px-5 py-3.5 text-center w-16">ID</th>
              <th class="px-5 py-3.5 text-center w-28">상태</th>
              <th class="px-5 py-3.5">품목 정보</th>
              <th class="px-5 py-3.5">거래처 정보</th>
              <th class="px-5 py-3.5 text-center">예정 로케이션</th>
              <th class="px-5 py-3.5 text-right w-24">예정 수량</th>
              <th class="px-5 py-3.5 text-center w-32">입고 예정일</th>
              <th class="px-5 py-3.5 text-center w-40">등록 일시</th>
              <th class="px-5 py-3.5 text-center w-24">작업자</th>
              <th class="px-5 py-3.5 text-center w-40">액션</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-if="inboundStore.isLoading" class="hover:bg-slate-50">
              <td colspan="10" class="px-6 py-10 text-center text-slate-400 text-xs">
                <div class="flex items-center justify-center gap-2">
                  <svg class="animate-spin h-5 w-5 text-indigo-500" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>입고 데이터를 불러오고 있습니다...</span>
                </div>
              </td>
            </tr>
            <tr v-else-if="filteredInbounds.length === 0" class="hover:bg-slate-50">
              <td colspan="10" class="px-6 py-12 text-center text-slate-400 text-xs">
                검색 조건에 맞는 입고 내역이 존재하지 않습니다.
              </td>
            </tr>
            <tr v-for="item in filteredInbounds" :key="item.inboundId" class="hover:bg-slate-50/80 transition-colors">
              <td class="px-5 py-4 text-center font-semibold text-slate-500 text-xs">{{ item.inboundId }}</td>
              <td class="px-5 py-4 text-center">
                <span
                  v-if="item.status === 'READY'"
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-600 border border-indigo-100 ring-2 ring-indigo-500/10"
                >
                  <span class="w-1.5 h-1.5 mr-1.5 rounded-full bg-indigo-500 animate-ping"></span>
                  대기 (READY)
                </span>
                <span
                  v-else
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-600 border border-emerald-100"
                >
                  완료 (COMPLETED)
                </span>
              </td>
              <td class="px-5 py-4">
                <div class="font-bold text-slate-800 text-xs">{{ item.itemName }}</div>
                <div class="text-[10px] text-slate-400 mt-0.5 tracking-wider">{{ item.itemCode }}</div>
              </td>
              <td class="px-5 py-4">
                <div class="font-semibold text-slate-700 text-xs">{{ item.partnerName }}</div>
                <div class="text-[10px] text-slate-400 mt-0.5">{{ item.partnerCode }}</div>
              </td>
              <td class="px-5 py-4 text-center">
                <span class="font-mono text-xs px-2 py-1 bg-slate-100 rounded text-slate-700 border border-slate-200">
                  {{ item.locationCode }}
                </span>
              </td>
              <td class="px-5 py-4 text-right font-extrabold text-slate-800 text-xs">
                {{ item.inboundQty.toLocaleString() }}
              </td>
              <td class="px-5 py-4 text-center text-xs text-slate-500 font-medium">
                {{ formatDate(item.inboundDate) }}
              </td>
              <td class="px-5 py-4 text-center text-xs text-slate-400 font-mono">
                {{ formatDateTime(item.createdAt) }}
              </td>
              <td class="px-5 py-4 text-center text-xs text-slate-600 font-medium">
                {{ item.workerName || '-' }}
              </td>
              <td class="px-5 py-4 text-center">
                <div class="flex items-center justify-center gap-2">
                  <template v-if="item.status === 'READY'">
                    <button
                      @click="handleComplete(item.inboundId)"
                      class="px-2.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-xs font-bold transition shadow-sm"
                    >
                      검수 완료
                    </button>
                    <button
                      @click="handleDelete(item.inboundId)"
                      class="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 rounded text-xs font-bold transition"
                    >
                      삭제
                    </button>
                  </template>
                  <template v-else>
                    <button
                      @click="navigateToStack"
                      class="px-2.5 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 border border-indigo-200 rounded text-xs font-bold transition flex items-center gap-1"
                    >
                      창고 적재하러 가기
                      <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                      </svg>
                    </button>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 입고 예정 등록 모달창 (Modal) -->
    <div v-if="isRegisterModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <!-- 어두운 반투명 배경 -->
      <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity" @click="closeRegisterModal"></div>

      <!-- 모달 컨텐츠 박스 -->
      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden transform transition-all duration-300 scale-100">
          <!-- 모달 헤더 -->
          <div class="px-6 py-4 bg-slate-50 border-b border-slate-150 flex items-center justify-between">
            <h3 class="text-base font-bold text-slate-800">신규 입고 예정 등록</h3>
            <button @click="closeRegisterModal" class="text-slate-400 hover:text-slate-600 font-bold text-lg">×</button>
          </div>

          <!-- 모달 폼 본문 -->
          <form @submit.prevent="handleRegister" class="p-6 space-y-4">
            <!-- 에러 경고 메시지 -->
            <div v-if="registerError" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-xs font-medium">
              {{ registerError }}
            </div>

            <!-- 품목 선택 -->
            <div class="space-y-1.5">
              <label class="block text-xs font-bold text-slate-700" for="modal-item">원자재 품목 선택 *</label>
              <select
                id="modal-item"
                v-model="selectedItemCode"
                class="w-full h-10 px-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
                required
              >
                <option value="">품목을 선택해 주세요</option>
                <option v-for="item in inboundStore.items" :key="item.itemId" :value="item.itemCode">
                  [{{ item.itemCode }}] {{ item.itemName }} (규격: {{ item.spec }})
                </option>
              </select>
            </div>

            <!-- 거래처 선택 -->
            <div class="space-y-1.5">
              <label class="block text-xs font-bold text-slate-700" for="modal-partner">공급사(거래처) 선택 *</label>
              <select
                id="modal-partner"
                v-model="selectedPartnerCode"
                class="w-full h-10 px-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
                required
              >
                <option value="">거래처를 선택해 주세요</option>
                <option v-for="p in inboundStore.partners" :key="p.partnerId" :value="p.partnerCode">
                  [{{ p.partnerCode }}] {{ p.partnerName }}
                </option>
              </select>
            </div>

            <!-- 예정 로케이션 선택 -->
            <div class="space-y-1.5">
              <label class="block text-xs font-bold text-slate-700" for="modal-location">하역 예정 로케이션 선택 *</label>
              <select
                id="modal-location"
                v-model="selectedLocationCode"
                class="w-full h-10 px-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
                required
              >
                <option value="">임시 로케이션을 선택해 주세요</option>
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} ({{ loc.rackRow }} / {{ loc.rackColumn }})
                </option>
              </select>
            </div>

            <!-- 수량 & 날짜 그리드 -->
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-1.5">
                <label class="block text-xs font-bold text-slate-700" for="modal-qty">입고 예정 수량 *</label>
                <input
                  id="modal-qty"
                  type="number"
                  v-model="inboundQty"
                  min="1"
                  placeholder="예: 100"
                  class="w-full h-10 px-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
                  required
                >
              </div>

              <div class="space-y-1.5">
                <label class="block text-xs font-bold text-slate-700" for="modal-date">입고 예정일 *</label>
                <input
                  id="modal-date"
                  type="date"
                  v-model="inboundDate"
                  class="w-full h-10 px-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
                  required
                >
              </div>
            </div>

            <!-- 모달 푸터 버튼 -->
            <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <button
                type="button"
                @click="closeRegisterModal"
                class="h-9 px-4 text-xs font-bold text-slate-500 bg-slate-100 hover:bg-slate-200 rounded-lg transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isRegisterSubmitting"
                class="h-9 px-5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-70 rounded-lg transition flex items-center justify-center gap-1.5"
              >
                <span v-if="isRegisterSubmitting" class="animate-spin h-3.5 w-3.5 border-2 border-white border-t-transparent rounded-full"></span>
                입고 예정 등록
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
