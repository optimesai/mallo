import axios from 'axios'
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

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const itemMasterApi = {
  async getItems(params: ItemMasterSearchParams = {}) {
    const response = await axios.get<ApiResponse<ItemMasterResponse[]>>('/api/items', {
      headers: getAuthHeaders(),
      params
    })
    return response.data
  },

  async getItem(id: number) {
    const response = await axios.get<ApiResponse<ItemMasterResponse>>(`/api/items/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async createItem(request: ItemMasterRequest) {
    const response = await axios.post<ApiResponse<ItemMasterResponse>>('/api/items', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async updateItem(id: number, request: ItemMasterRequest) {
    const response = await axios.put<ApiResponse<ItemMasterResponse>>(`/api/items/${id}`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async deleteItem(id: number, force = false) {
    const response = await axios.delete<ApiResponse<void>>(`/api/items/${id}`, {
      headers: getAuthHeaders(),
      params: { force }
    })
    return response.data
  }
}
