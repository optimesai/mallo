import axios from 'axios'
import type { ApiResponse } from '@/api/authApi'
import type { ItemMasterResponse, ItemType } from '@/api/itemMasterApi'

export interface BomMasterResponse {
  bomId: number
  parentItemId: number
  parentItemCode: string
  parentItemName: string
  parentItemType: ItemType
  childItemId: number
  childItemCode: string
  childItemName: string
  childItemType: ItemType
  childUnit: string
  quantity: number
  bomVersion: string
  createdAt: string
}

export interface BomReverseResponse {
  bomId: number
  childItemId: number
  childItemCode: string
  childItemName: string
  childItemType: ItemType
  parentItemId: number
  parentItemCode: string
  parentItemName: string
  parentItemType: ItemType
  quantity: number
  bomVersion: string
}

export interface BomTreeNode {
  itemId: number
  itemCode: string
  itemName: string
  itemType: ItemType
  unit: string
  quantity: number
  bomVersion: string | null
  children?: BomTreeNode[]
  parents?: BomTreeNode[]
}

export interface BomMasterRequest {
  parentItemId: number
  childItemId: number
  quantity: number
  bomVersion?: string
}

export interface BomMasterSearchParams {
  parentKeyword?: string
  childKeyword?: string
  bomVersion?: string
}

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const bomMasterApi = {
  async getBoms(params: BomMasterSearchParams = {}) {
    const response = await axios.get<ApiResponse<BomMasterResponse[]>>('/api/boms', {
      headers: getAuthHeaders(),
      params
    })
    return response.data
  },

  async getBom(bomId: number) {
    const response = await axios.get<ApiResponse<BomMasterResponse>>(`/api/boms/details/${bomId}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async createBom(request: BomMasterRequest) {
    const response = await axios.post<ApiResponse<BomMasterResponse>>('/api/boms', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async updateBom(bomId: number, request: BomMasterRequest) {
    const response = await axios.put<ApiResponse<BomMasterResponse>>(`/api/boms/${bomId}`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async deleteBom(bomId: number) {
    const response = await axios.delete<ApiResponse<void>>(`/api/boms/${bomId}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getParentVersions(keyword: string) {
    const response = await axios.get<ApiResponse<string[]>>('/api/boms/parents/versions', {
      headers: getAuthHeaders(),
      params: { keyword }
    })
    return response.data
  },

  async getChildVersions(keyword: string) {
    const response = await axios.get<ApiResponse<string[]>>('/api/boms/children/versions', {
      headers: getAuthHeaders(),
      params: { keyword }
    })
    return response.data
  },

  async getParentTree(keyword: string, bomVersion?: string) {
    const response = await axios.get<ApiResponse<BomTreeNode[]>>('/api/boms/parents/tree', {
      headers: getAuthHeaders(),
      params: { keyword, bomVersion }
    })
    return response.data
  },

  async getChildParentTree(keyword: string, bomVersion?: string) {
    const response = await axios.get<ApiResponse<BomTreeNode[]>>('/api/boms/children/parents/tree', {
      headers: getAuthHeaders(),
      params: { keyword, bomVersion }
    })
    return response.data
  },

  async getChildParents(keyword: string, bomVersion?: string) {
    const response = await axios.get<ApiResponse<BomReverseResponse[]>>('/api/boms/children/parents', {
      headers: getAuthHeaders(),
      params: { keyword, bomVersion }
    })
    return response.data
  },

  async getItems(itemType?: ItemType, keyword?: string) {
    const response = await axios.get<ApiResponse<ItemMasterResponse[]>>('/api/items', {
      headers: getAuthHeaders(),
      params: { itemType, keyword }
    })
    return response.data
  }
}
