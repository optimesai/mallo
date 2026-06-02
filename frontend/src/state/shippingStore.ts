import { defineStore } from 'pinia'
import { ref } from 'vue'
import { shippingService } from '@/services/shippingService'
import type {
  ShippingResponse,
  ShippingCreateRequest,
  PickingAssignRequest
} from '@/api/shippingApi'

export const useShippingStore = defineStore('shipping', () => {
  const shippings = ref<ShippingResponse[]>([])
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  async function loadShippings() {
    isLoading.value = true
    error.value = null
    try {
      shippings.value = await shippingService.getShippings()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '출하 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function registerShipping(request: ShippingCreateRequest) {
    isLoading.value = true
    error.value = null
    try {
      const newShipping = await shippingService.registerShipping(request)
      shippings.value.unshift(newShipping)
      return newShipping
    } catch (err) {
      error.value = err instanceof Error ? err.message : '출하 지시 등록에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function assignPicking(id: number, request: PickingAssignRequest) {
    isLoading.value = true
    error.value = null
    try {
      const updated = await shippingService.assignPicking(id, request)
      const index = shippings.value.findIndex((s) => s.shippingId === id)
      if (index !== -1) {
        shippings.value[index] = updated
      }
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '피킹 배정에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function completeShipping(id: number) {
    isLoading.value = true
    error.value = null
    try {
      await shippingService.completeShipping(id)
      // 완료 후 상태를 현행화하기 위해 목록 다시 불러오기
      await loadShippings()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '출하 완료 처리에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  return {
    shippings,
    isLoading,
    error,
    loadShippings,
    registerShipping,
    assignPicking,
    completeShipping
  }
})
