import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { PageResponse } from '@/api/types'

export interface CurrentInventoryResponse {
  inventoryId: number | null
  itemCode: string
  itemName: string
  locationCode: string | null
  warehouseName: string | null
  currentQty: number
  updatedAt: string | null
}

export interface TransactionHistoryResponse {
  transactionId: number
  itemCode: string
  itemName: string
  locationCode: string
  transactionType: string
  quantity: number
  reasonDesc: string
  workerName?: string
  createdAt: string
}

export interface InventoryListParams {
  page?: number
  size?: number
  sort?: string
  keyword?: string
}

export interface HistoryListParams {
  page?: number
  size?: number
  sort?: string
  transactionType?: string
  startDate?: string
  endDate?: string
}

export const inventoryApi = {
  async getInventories(params: InventoryListParams = {}) {
    const response = await apiClient.get<ApiResponse<PageResponse<CurrentInventoryResponse>>>('/api/inventory', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: params.sort ?? 'updatedAt,desc',
        keyword: params.keyword || undefined,
      }
    })
    return response.data
  },

  async getInventory(id: number) {
    const response = await apiClient.get<ApiResponse<CurrentInventoryResponse>>(`/api/inventory/${id}`)
    return response.data
  },

  async getTransactionHistories(params: HistoryListParams = {}) {
    const response = await apiClient.get<ApiResponse<PageResponse<TransactionHistoryResponse>>>('/api/inventory/history', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: params.sort ?? 'createdAt,desc',
        transactionType: params.transactionType || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
      }
    })
    return response.data
  }
}
