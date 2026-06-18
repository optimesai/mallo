import { defineStore } from 'pinia'
import { ref } from 'vue'
import { inventoryService } from '@/services/inventoryService'
import { useInboundStore } from '@/state/inboundStore'
import type { CurrentInventoryResponse, TransactionHistoryResponse } from '@/api/inventoryApi'

export const useInventoryStore = defineStore('inventory', () => {
  const inventories = ref<CurrentInventoryResponse[]>([])
  const histories = ref<TransactionHistoryResponse[]>([])
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  const inboundStore = useInboundStore()

  const selectedDetail = ref<CurrentInventoryResponse | null>(null)

  // Pagination state
  const invPage = ref<number>(0)
  const invTotalPages = ref<number>(0)
  const invTotalElements = ref<number>(0)
  const histPage = ref<number>(0)
  const histTotalPages = ref<number>(0)
  const histTotalElements = ref<number>(0)

  async function loadInventories(params: { page?: number; size?: number; sort?: string; keyword?: string } = {}) {
    isLoading.value = true
    error.value = null
    try {
      if (inboundStore.items.length === 0) {
        await inboundStore.loadItems()
      }
      const pageResponse = await inventoryService.getInventories(params)
      inventories.value = pageResponse.content
      invPage.value = pageResponse.page
      invTotalPages.value = pageResponse.totalPages
      invTotalElements.value = pageResponse.totalElements
    } catch (err) {
      error.value = err instanceof Error ? err.message : '현재고 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadInventoryDetail(id: number) {
    isLoading.value = true
    error.value = null
    try {
      selectedDetail.value = await inventoryService.getInventory(id)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '재고 상세 정보를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadHistories(params: { page?: number; size?: number; sort?: string; transactionType?: string; startDate?: string; endDate?: string } = {}) {
    isLoading.value = true
    error.value = null
    try {
      const pageResponse = await inventoryService.getTransactionHistories(params)
      histories.value = pageResponse.content
      histPage.value = pageResponse.page
      histTotalPages.value = pageResponse.totalPages
      histTotalElements.value = pageResponse.totalElements
    } catch (err) {
      error.value = err instanceof Error ? err.message : '수불 이력을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  function getSafetyStock(itemCode: string): number {
    const item = inboundStore.items.find(i => i.itemCode === itemCode)
    return item ? item.safetyStock : 0
  }

  function getTotalAvailableQty(itemCode: string): number {
    return inventories.value
      .filter(i => i.itemCode === itemCode)
      .reduce((sum, i) => sum + i.currentQty, 0)
  }

  return {
    inventories,
    histories,
    selectedDetail,
    isLoading,
    error,
    invPage,
    invTotalPages,
    invTotalElements,
    histPage,
    histTotalPages,
    histTotalElements,
    loadInventories,
    loadInventoryDetail,
    loadHistories,
    getSafetyStock,
    getTotalAvailableQty
  }
})
