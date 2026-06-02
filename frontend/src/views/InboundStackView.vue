<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useInboundStore } from '@/state/inboundStore'

const inboundStore = useInboundStore()

// 검색 및 로딩 상태
const searchKeyword = ref('')
const pageError = ref<string | null>(null)

// 적재 모달 상태
const isStackModalOpen = ref(false)
const selectedInbound = ref<any>(null)
const targetLocationCode = ref('')
const stackError = ref<string | null>(null)
const isStackSubmitting = ref(false)

// 성공 토스트 상태
const successMessage = ref<string | null>(null)

// 적재 가능한 입고 완료(COMPLETED) 건만 필터링
const completedInbounds = computed(() => {
  return inboundStore.inbounds.filter(item => {
    // 적재 대상은 COMPLETED 상태인 경우만 해당
    if (item.status !== 'COMPLETED') {
      return false
    }

    if (searchKeyword.value.trim() !== '') {
      const keyword = searchKeyword.value.toLowerCase()
      const matchItemCode = item.itemCode?.toLowerCase().includes(keyword)
      const matchItemName = item.itemName?.toLowerCase().includes(keyword)
      const matchPartnerCode = item.partnerCode?.toLowerCase().includes(keyword)
      const matchPartnerName = item.partnerName?.toLowerCase().includes(keyword)
      const matchLocationCode = item.locationCode?.toLowerCase().includes(keyword)
      return matchItemCode || matchItemName || matchPartnerCode || matchPartnerName || matchLocationCode
    }
    return true
  })
})

onMounted(async () => {
  try {
    pageError.value = null
    await Promise.all([
      inboundStore.loadInbounds(),
      inboundStore.loadLocations() // 적재 대상 로케이션 목록 로드
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '데이터 로드에 실패했습니다.'
  }
})

// 적재 모달 열기
function openStackModal(item: any) {
  selectedInbound.value = item
  targetLocationCode.value = ''
  stackError.value = null
  isStackModalOpen.value = true
}

// 적재 모달 닫기
function closeStackModal() {
  isStackModalOpen.value = false
  selectedInbound.value = null
}

// 적재 제출 처리
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
    
    // 리스트 리로드하여 재고 및 로케이션 변경 확인
    await inboundStore.loadInbounds()

    successMessage.value = `[ID: ${inboundId}] ${selectedInbound.value.itemName} 자재가 ${targetLocationCode.value} 로케이션에 성공적으로 적재되었습니다.`
    setTimeout(() => {
      successMessage.value = null
    }, 4000)

    closeStackModal()
  } catch (err) {
    stackError.value = err instanceof Error ? err.message : '로케이션 적재 처리에 실패했습니다.'
  } finally {
    isStackSubmitting.value = false
  }
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
    <div>
      <h1 class="text-2xl font-bold text-slate-800 tracking-tight">창고 적재 및 로케이션 배치</h1>
      <p class="text-sm text-slate-500 mt-1">검수가 완료된(COMPLETED) 자재를 가용 창고의 상세 렉(Rack) 위치로 매핑하고 실시간 창고 재고를 증가시킵니다.</p>
    </div>

    <!-- 에러 메시지 배너 -->
    <div v-if="pageError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex items-center justify-between">
      <span>{{ pageError }}</span>
      <button @click="pageError = null" class="text-red-500 hover:text-red-700 font-bold">×</button>
    </div>

    <!-- 성공 알림 토스트 -->
    <Transition name="fade">
      <div v-if="successMessage" class="bg-emerald-50 border border-emerald-200 text-emerald-800 px-4 py-3.5 rounded-lg text-xs font-semibold flex items-center gap-3 shadow-sm">
        <svg class="w-5 h-5 text-emerald-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>{{ successMessage }}</span>
      </div>
    </Transition>

    <!-- 메인 대기 목록 그리드/패널 -->
    <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <!-- 검색 필터 헤더 -->
      <div class="p-5 border-b border-slate-200 bg-slate-50 flex flex-col sm:flex-row sm:items-center gap-4 justify-between">
        <div class="flex items-center gap-3">
          <span class="text-sm font-semibold text-slate-700">적재 대기 원자재 목록</span>
          <span class="px-2.5 py-0.5 bg-indigo-50 text-indigo-700 text-xs font-bold rounded-full border border-indigo-150">
            {{ completedInbounds.length }}개 대기중
          </span>
        </div>
        <div>
          <!-- 키워드 검색 -->
          <div class="relative">
            <input
              v-model="searchKeyword"
              placeholder="품목명, 코드, 거래처, 로케이션 검색..."
              class="w-full sm:w-72 h-9 pl-8 pr-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
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
              <th class="px-5 py-3.5">품목 정보</th>
              <th class="px-5 py-3.5">공급처 정보</th>
              <th class="px-5 py-3.5 text-center">현재 위치 (임시)</th>
              <th class="px-5 py-3.5 text-right w-24">입고 수량</th>
              <th class="px-5 py-3.5 text-center w-40">검수 완료일시</th>
              <th class="px-5 py-3.5 text-center w-24">담당자</th>
              <th class="px-5 py-3.5 text-center w-36">액션</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-if="inboundStore.isLoading" class="hover:bg-slate-50">
              <td colspan="8" class="px-6 py-10 text-center text-slate-400 text-xs">
                <div class="flex items-center justify-center gap-2">
                  <svg class="animate-spin h-5 w-5 text-indigo-500" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>창고 정보를 불러오고 있습니다...</span>
                </div>
              </td>
            </tr>
            <tr v-else-if="completedInbounds.length === 0" class="hover:bg-slate-50">
              <td colspan="8" class="px-6 py-12 text-center text-slate-400 text-xs">
                적재 대기 중인 입고 완료 자재가 없습니다.
              </td>
            </tr>
            <tr v-for="item in completedInbounds" :key="item.inboundId" class="hover:bg-slate-50/80 transition-colors">
              <td class="px-5 py-4 text-center font-semibold text-slate-500 text-xs">{{ item.inboundId }}</td>
              <td class="px-5 py-4">
                <div class="font-bold text-slate-800 text-xs">{{ item.itemName }}</div>
                <div class="text-[10px] text-slate-400 mt-0.5 tracking-wider">{{ item.itemCode }}</div>
              </td>
              <td class="px-5 py-4">
                <div class="font-semibold text-slate-700 text-xs">{{ item.partnerName }}</div>
                <div class="text-[10px] text-slate-400 mt-0.5">{{ item.partnerCode }}</div>
              </td>
              <td class="px-5 py-4 text-center">
                <span class="font-mono text-xs px-2 py-1 bg-slate-100 rounded text-slate-600 border border-slate-200">
                  {{ item.locationCode }}
                </span>
              </td>
              <td class="px-5 py-4 text-right font-extrabold text-slate-800 text-xs">
                {{ item.inboundQty.toLocaleString() }}
              </td>
              <td class="px-5 py-4 text-center text-xs text-slate-400 font-mono">
                {{ formatDateTime(item.createdAt) }}
              </td>
              <td class="px-5 py-4 text-center text-xs text-slate-600 font-medium">
                {{ item.workerName || '-' }}
              </td>
              <td class="px-5 py-4 text-center">
                <button
                  @click="openStackModal(item)"
                  class="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded text-xs font-bold transition shadow-sm inline-flex items-center gap-1"
                >
                  <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  렉 적재 처리
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 적재 로케이션 매핑 모달 (Modal) -->
    <div v-if="isStackModalOpen" class="fixed inset-0 z-50 overflow-y-auto" role="dialog" aria-modal="true">
      <!-- 배경 -->
      <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity" @click="closeStackModal"></div>

      <!-- 모달 박스 -->
      <div class="flex min-h-screen items-center justify-center p-4">
        <div class="relative w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden transform transition-all duration-300 scale-100">
          <!-- 모달 헤더 -->
          <div class="px-6 py-4 bg-slate-50 border-b border-slate-150 flex items-center justify-between">
            <h3 class="text-base font-bold text-slate-800">실시간 창고 적재(바인딩)</h3>
            <button @click="closeStackModal" class="text-slate-400 hover:text-slate-600 font-bold text-lg">×</button>
          </div>

          <!-- 모달 폼 본문 -->
          <form @submit.prevent="handleStack" class="p-6 space-y-4">
            <!-- 에러 메시지 -->
            <div v-if="stackError" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded-lg text-xs font-medium">
              {{ stackError }}
            </div>

            <!-- 선택 품목 정보 요약 패널 -->
            <div class="p-4 bg-slate-50 border border-slate-150 rounded-xl space-y-2">
              <div class="flex justify-between items-center text-xs">
                <span class="text-slate-400 font-medium">품목명 / 코드</span>
                <span class="font-bold text-slate-800 text-right">
                  {{ selectedInbound?.itemName }}
                  <span class="block text-[10px] text-slate-400 font-normal tracking-wide">{{ selectedInbound?.itemCode }}</span>
                </span>
              </div>
              <div class="flex justify-between items-center text-xs">
                <span class="text-slate-400 font-medium">적재 대상 수량</span>
                <span class="font-extrabold text-indigo-600">{{ selectedInbound?.inboundQty.toLocaleString() }} 개</span>
              </div>
              <div class="flex justify-between items-center text-xs">
                <span class="text-slate-400 font-medium">현재 하역 위치</span>
                <span class="font-mono text-slate-600">{{ selectedInbound?.locationCode }}</span>
              </div>
            </div>

            <!-- 실제 적재할 로케이션 렉 위치 선택 -->
            <div class="space-y-1.5">
              <label class="block text-xs font-bold text-slate-700" for="modal-target-location">최종 적재 로케이션(렉) 지정 *</label>
              <select
                id="modal-target-location"
                v-model="targetLocationCode"
                class="w-full h-10 px-3 text-xs bg-white border border-slate-300 rounded-lg outline-none focus:border-indigo-500 transition"
                required
              >
                <option value="">적재 위치를 선택해 주세요</option>
                <!-- 동일 품종 보관용 가급적 적재 구역 제안 -->
                <option v-for="loc in inboundStore.locations" :key="loc.locationId" :value="loc.locationCode">
                  [{{ loc.locationCode }}] {{ loc.warehouseName }} - {{ loc.rackRow }}{{ loc.rackColumn }}
                </option>
              </select>
              <p class="text-[10px] text-slate-400 leading-normal mt-1">※ 지정한 렉 로케이션의 실시간 재고가 자동으로 합산 증가하며, WMS 입고 수불 이력이 즉시 남게 됩니다.</p>
            </div>

            <!-- 모달 푸터 버튼 -->
            <div class="flex justify-end gap-3 pt-4 border-t border-slate-100">
              <button
                type="button"
                @click="closeStackModal"
                class="h-9 px-4 text-xs font-bold text-slate-500 bg-slate-100 hover:bg-slate-200 rounded-lg transition"
              >
                취소
              </button>
              <button
                type="submit"
                :disabled="isStackSubmitting"
                class="h-9 px-5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-70 rounded-lg transition flex items-center justify-center gap-1.5"
              >
                <span v-if="isStackSubmitting" class="animate-spin h-3.5 w-3.5 border-2 border-white border-t-transparent rounded-full"></span>
                적재 완료
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
