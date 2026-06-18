import { AxiosError } from 'axios'
import { inventoryApi } from '@/api/inventoryApi'
import type {
  CurrentInventoryResponse,
  TransactionHistoryResponse,
  InventoryListParams,
  HistoryListParams
} from '@/api/inventoryApi'
import type { PageResponse } from '@/api/types'

export const inventoryService = {
  async getInventories(params: InventoryListParams = {}): Promise<PageResponse<CurrentInventoryResponse>> {
    try {
      const response = await inventoryApi.getInventories(params)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '현재고 목록을 불러오지 못했습니다.')
      }
      throw new Error('현재고 목록을 불러오지 못했습니다.')
    }
  },

  async getInventory(id: number): Promise<CurrentInventoryResponse> {
    try {
      const response = await inventoryApi.getInventory(id)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '재고 상세 정보를 불러오지 못했습니다.')
      }
      throw new Error('재고 상세 정보를 불러오지 못했습니다.')
    }
  },

  async getTransactionHistories(params: HistoryListParams = {}): Promise<PageResponse<TransactionHistoryResponse>> {
    try {
      const response = await inventoryApi.getTransactionHistories(params)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '수불 이력을 불러오지 못했습니다.')
      }
      throw new Error('수불 이력을 불러오지 못했습니다.')
    }
  }
}
