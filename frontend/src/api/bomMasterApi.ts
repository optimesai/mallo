import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { PageResponse } from '@/api/types'
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

export const bomMasterApi = {
  async getBoms(params: BomMasterSearchParams = {}) {
    const response = await apiClient.get<ApiResponse<BomMasterResponse[]>>('/api/boms', {
      params
    })
    return response.data
  },

  async getBom(bomId: number) {
    const response = await apiClient.get<ApiResponse<BomMasterResponse>>(`/api/boms/details/${bomId}`)
    return response.data
  },

  async createBom(request: BomMasterRequest) {
    const response = await apiClient.post<ApiResponse<BomMasterResponse>>('/api/boms', request)
    return response.data
  },

  async updateBom(bomId: number, request: BomMasterRequest) {
    const response = await apiClient.put<ApiResponse<BomMasterResponse>>(`/api/boms/${bomId}`, request)
    return response.data
  },

  async deleteBom(bomId: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/boms/${bomId}`)
    return response.data
  },

  async getParentVersions(keyword: string) {
    const response = await apiClient.get<ApiResponse<string[]>>('/api/boms/parents/versions', {
      params: { keyword }
    })
    return response.data
  },

  async getChildVersions(keyword: string) {
    const response = await apiClient.get<ApiResponse<string[]>>('/api/boms/children/versions', {
      params: { keyword }
    })
    return response.data
  },

  async getParentTree(keyword: string, bomVersion?: string) {
    const response = await apiClient.get<ApiResponse<BomTreeNode[]>>('/api/boms/parents/tree', {
      params: { keyword, bomVersion }
    })
    return response.data
  },

  async getChildParentTree(keyword: string, bomVersion?: string) {
    const response = await apiClient.get<ApiResponse<BomTreeNode[]>>('/api/boms/children/parents/tree', {
      params: { keyword, bomVersion }
    })
    return response.data
  },

  async getChildParents(keyword: string, bomVersion?: string) {
    const response = await apiClient.get<ApiResponse<BomReverseResponse[]>>('/api/boms/children/parents', {
      params: { keyword, bomVersion }
    })
    return response.data
  },

  async getItems(itemType?: ItemType, keyword?: string) {
    const response = await apiClient.get<ApiResponse<PageResponse<ItemMasterResponse>>>('/api/items', {
      params: { itemType, keyword, itemStatus: 'ACTIVE', page: 0, size: 100, sort: 'itemCode,asc' }
    })
    return response.data
  }
}
