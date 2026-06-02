import axios from 'axios'
import type { ApiResponse } from '@/api/authApi'

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

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const inventoryApi = {
  async getInventories() {
    const response = await axios.get<ApiResponse<CurrentInventoryResponse[]>>('/api/inventory', {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getInventory(id: number) {
    const response = await axios.get<ApiResponse<CurrentInventoryResponse>>(`/api/inventory/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getTransactionHistories() {
    const response = await axios.get<ApiResponse<TransactionHistoryResponse[]>>('/api/inventory/history', {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
