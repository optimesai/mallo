import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'

export type ItemType = 'RAW' | 'HALF' | 'FG'
export type ItemUnit = 'ea' | 'kg' | 'box' | 'L'

export interface ItemMasterResponse {
  itemId: number
  itemCode: string
  itemName: string
  spec: string | null
  unit: ItemUnit
  itemType: ItemType
  safetyStock: number
  createdAt: string
}

export interface ItemMasterRequest {
  itemName: string
  spec?: string | null
  unit: ItemUnit
  itemType: ItemType
  safetyStock: number
}

export interface ItemMasterSearchParams {
  itemType?: ItemType
  keyword?: string
}

export const itemMasterApi = {
  async getItems(params: ItemMasterSearchParams = {}) {
    const response = await apiClient.get<ApiResponse<ItemMasterResponse[]>>('/api/items', {
      params
    })
    return response.data
  },

  async getItem(id: number) {
    const response = await apiClient.get<ApiResponse<ItemMasterResponse>>(`/api/items/${id}`)
    return response.data
  },

  async createItem(request: ItemMasterRequest) {
    const response = await apiClient.post<ApiResponse<ItemMasterResponse>>('/api/items', request)
    return response.data
  },

  async updateItem(id: number, request: ItemMasterRequest) {
    const response = await apiClient.put<ApiResponse<ItemMasterResponse>>(`/api/items/${id}`, request)
    return response.data
  },

  async deleteItem(id: number, force = false) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/items/${id}`, {
      params: { force }
    })
    return response.data
  }
}
