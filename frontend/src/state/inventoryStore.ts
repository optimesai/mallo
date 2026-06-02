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

  async function loadInventories() {
    isLoading.value = true
    error.value = null
    try {
      if (inboundStore.items.length === 0) {
        await inboundStore.loadItems()
      }
      inventories.value = await inventoryService.getInventories()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '현재고 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadHistories() {
    isLoading.value = true
    error.value = null
    try {
      histories.value = await inventoryService.getTransactionHistories()
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
    isLoading,
    error,
    loadInventories,
    loadHistories,
    getSafetyStock,
    getTotalAvailableQty
  }
})
