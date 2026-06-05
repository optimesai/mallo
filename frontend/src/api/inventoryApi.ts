import axios from 'axios'
import type { ApiResponse } from '@/api/authApi'
import type { PageResponse } from '@/api/types'

export interface CurrentInventoryResponse {
  inventoryId: number
  itemCode: string
  itemName: string
  locationCode: string
  warehouseName: string
  currentQty: number
  updatedAt: string
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

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const inventoryApi = {
  async getInventories(params: InventoryListParams = {}) {
    const response = await axios.get<ApiResponse<PageResponse<CurrentInventoryResponse>>>('/api/inventory', {
      headers: getAuthHeaders(),
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
    const response = await axios.get<ApiResponse<CurrentInventoryResponse>>(`/api/inventory/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getTransactionHistories(params: HistoryListParams = {}) {
    const response = await axios.get<ApiResponse<PageResponse<TransactionHistoryResponse>>>('/api/inventory/history', {
      headers: getAuthHeaders(),
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
