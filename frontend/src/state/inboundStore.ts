import { defineStore } from 'pinia'
import { ref } from 'vue'
import { inboundService } from '@/services/inboundService'
import type {
  InboundReceiptResponse,
  InboundCreateRequest,
  ItemResponse,
  PartnerResponse,
  LocationResponse
} from '@/api/inboundApi'

export const useInboundStore = defineStore('inbound', () => {
  const inbounds = ref<InboundReceiptResponse[]>([])
  const items = ref<ItemResponse[]>([])
  const partners = ref<PartnerResponse[]>([])
  const locations = ref<LocationResponse[]>([])
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  async function loadInbounds() {
    isLoading.value = true
    error.value = null
    try {
      inbounds.value = await inboundService.getInbounds()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '입고 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadItems(itemType?: string, keyword?: string) {
    isLoading.value = true
    error.value = null
    try {
      items.value = await inboundService.getItems(itemType, keyword)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadPartners(partnerType?: string, keyword?: string) {
    isLoading.value = true
    error.value = null
    try {
      partners.value = await inboundService.getPartners(partnerType, keyword)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '거래처 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadLocations() {
    isLoading.value = true
    error.value = null
    try {
      locations.value = await inboundService.getLocations()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '로케이션 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function registerInbound(request: InboundCreateRequest) {
    isLoading.value = true
    error.value = null
    try {
      const newInbound = await inboundService.registerInbound(request)
      inbounds.value.unshift(newInbound)
      return newInbound
    } catch (err) {
      error.value = err instanceof Error ? err.message : '입고 등록에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function completeInbound(id: number) {
    isLoading.value = true
    error.value = null
    try {
      const updated = await inboundService.completeInbound(id)
      const index = inbounds.value.findIndex((item) => item.inboundId === id)
      if (index !== -1) {
        inbounds.value[index] = updated
      }
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '입고 완료 처리에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function stackInventory(id: number, targetLocationCode: string) {
    isLoading.value = true
    error.value = null
    try {
      await inboundService.stackInventory(id, { targetLocationCode })
      // 로컬 목록에서도 로케이션 업데이트 (백엔드 로직에 맞게 로케이션 변경 반영)
      const index = inbounds.value.findIndex((item) => item.inboundId === id)
      if (index !== -1) {
        inbounds.value[index].locationCode = targetLocationCode
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '창고 적재에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function deleteInbound(id: number) {
    isLoading.value = true
    error.value = null
    try {
      await inboundService.deleteInbound(id)
      inbounds.value = inbounds.value.filter((item) => item.inboundId !== id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '입고 삭제에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  return {
    inbounds,
    items,
    partners,
    locations,
    isLoading,
    error,
    loadInbounds,
    loadItems,
    loadPartners,
    loadLocations,
    registerInbound,
    completeInbound,
    stackInventory,
    deleteInbound
  }
})
