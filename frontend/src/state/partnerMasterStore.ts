import { defineStore } from 'pinia'
import { ref } from 'vue'
import { partnerMasterService } from '@/services/partnerMasterService'
import type {
  PartnerMasterRequest,
  PartnerMasterResponse,
  PartnerMasterSearchParams
} from '@/api/partnerMasterApi'

export const usePartnerMasterStore = defineStore('partnerMaster', () => {
  const partners = ref<PartnerMasterResponse[]>([])
  const selectedPartner = ref<PartnerMasterResponse | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)

  async function loadPartners(params: PartnerMasterSearchParams = {}) {
    isLoading.value = true
    error.value = null
    try {
      partners.value = await partnerMasterService.getPartners(params)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
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
  }

  return {
    partners,
    selectedPartner,
    isLoading,
    isSaving,
    error,
    loadPartners,
    searchPartners,
    createPartner,
    updatePartner,
    deletePartner,
    selectPartner
  }
})
