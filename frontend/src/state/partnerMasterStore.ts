import { defineStore } from 'pinia'
import { ref } from 'vue'
import { partnerMasterService } from '@/services/partnerMasterService'
import type {
  PartnerMasterRequest,
  PartnerMasterResponse,
  PartnerMasterSearchParams,
  PartnerShippedItemResponse,
  PartnerStatsResponse,
  PartnerStatus,
  PartnerSuppliedItemResponse,
  PartnerUsageResponse
} from '@/api/partnerMasterApi'

export const usePartnerMasterStore = defineStore('partnerMaster', () => {
  const partners = ref<PartnerMasterResponse[]>([])
  const selectedPartner = ref<PartnerMasterResponse | null>(null)
  const selectedUsage = ref<PartnerUsageResponse | null>(null)
  const suppliedItems = ref<PartnerSuppliedItemResponse[]>([])
  const shippedItems = ref<PartnerShippedItemResponse[]>([])
  const suggestions = ref<PartnerMasterResponse[]>([])
  const stats = ref<PartnerStatsResponse>({
    totalCount: 0,
    activeCount: 0,
    inactiveCount: 0
  })
  const page = ref(0)
  const size = ref(10)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const sort = ref('createdAt,desc')
  const isLoading = ref(false)
  const isSaving = ref(false)
  const isUsageLoading = ref(false)
  const isSuppliedItemsLoading = ref(false)
  const isShippedItemsLoading = ref(false)
  const error = ref<string | null>(null)

  async function loadPartners(params: PartnerMasterSearchParams = {}) {
    isLoading.value = true
    error.value = null
    try {
      const response = await partnerMasterService.getPartners(params)
      partners.value = response.content
      page.value = response.page
      size.value = response.size
      totalElements.value = response.totalElements
      totalPages.value = response.totalPages
      sort.value = response.sort
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadPartnerStats() {
    try {
      stats.value = await partnerMasterService.getPartnerStats()
      return stats.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 통계를 불러오지 못했습니다.'
      throw err
    }
  }

  async function searchPartners(searchValue: string) {
    isLoading.value = true
    error.value = null
    try {
      partners.value = await partnerMasterService.searchPartners(searchValue)
      return partners.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 상세 정보를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadSuggestions(keyword: string) {
    try {
      const response = await partnerMasterService.getPartners({
        page: 0,
        size: 8,
        sort: 'partnerName,asc',
        keyword
      })
      suggestions.value = response.content
    } catch (err) {
      suggestions.value = []
    }
  }

  async function createPartner(request: PartnerMasterRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await partnerMasterService.createPartner(request)
      partners.value = [created, ...partners.value]
      selectedPartner.value = created
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updatePartner(id: number, request: PartnerMasterRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await partnerMasterService.updatePartner(id, request)
      const index = partners.value.findIndex((partner) => partner.partnerId === id)
      if (index !== -1) {
        partners.value[index] = updated
      }
      selectedPartner.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 수정에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updatePartnerStatus(id: number, partnerStatus: PartnerStatus) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await partnerMasterService.updatePartnerStatus(id, partnerStatus)
      const index = partners.value.findIndex((partner) => partner.partnerId === id)
      if (index !== -1) {
        partners.value[index] = updated
      }
      selectedPartner.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 상태 변경에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function loadPartnerUsage(id: number) {
    isUsageLoading.value = true
    error.value = null
    try {
      selectedUsage.value = await partnerMasterService.getPartnerUsage(id)
      return selectedUsage.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 사용 현황을 불러오지 못했습니다.'
      throw err
    } finally {
      isUsageLoading.value = false
    }
  }

  async function loadSuppliedItems(id: number) {
    isSuppliedItemsLoading.value = true
    error.value = null
    try {
      suppliedItems.value = await partnerMasterService.getSuppliedItems(id)
      return suppliedItems.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '공급 품목 이력을 불러오지 못했습니다.'
      throw err
    } finally {
      isSuppliedItemsLoading.value = false
    }
  }

  async function loadShippedItems(id: number) {
    isShippedItemsLoading.value = true
    error.value = null
    try {
      shippedItems.value = await partnerMasterService.getShippedItems(id)
      return shippedItems.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '출하 품목 이력을 불러오지 못했습니다.'
      throw err
    } finally {
      isShippedItemsLoading.value = false
    }
  }

  async function checkDuplicate(partnerCode: string) {
    return partnerMasterService.checkDuplicate(partnerCode)
  }

  async function deletePartner(id: number) {
    isSaving.value = true
    error.value = null
    try {
      await partnerMasterService.deletePartner(id)
      partners.value = partners.value.filter((partner) => partner.partnerId !== id)
      if (selectedPartner.value?.partnerId === id) {
        selectedPartner.value = null
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 삭제에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  function selectPartner(partner: PartnerMasterResponse | null) {
    selectedPartner.value = partner
    selectedUsage.value = null
    suppliedItems.value = []
    shippedItems.value = []
  }

  return {
    partners,
    selectedPartner,
    selectedUsage,
    suppliedItems,
    shippedItems,
    suggestions,
    stats,
    page,
    size,
    totalElements,
    totalPages,
    sort,
    isLoading,
    isSaving,
    isUsageLoading,
    isSuppliedItemsLoading,
    isShippedItemsLoading,
    error,
    loadPartners,
    loadPartnerStats,
    searchPartners,
    loadSuggestions,
    createPartner,
    updatePartner,
    updatePartnerStatus,
    loadPartnerUsage,
    loadSuppliedItems,
    loadShippedItems,
    checkDuplicate,
    deletePartner,
    selectPartner
  }
})
