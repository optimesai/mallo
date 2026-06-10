import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { PageParams, PageResponse } from '@/api/types'
import type { BomMasterResponse } from '@/api/bomMasterApi'
import type { TransactionHistoryResponse } from '@/api/inventoryApi'
import type { WorkOrderResponse } from '@/api/workOrderApi'

export type ItemType = 'RAW' | 'HALF' | 'FG'
export type ItemUnit = 'ea' | 'kg' | 'box' | 'L'
export type ItemStatus = 'ACTIVE' | 'INACTIVE'

export interface ItemMasterResponse {
  itemId: number
  itemCode: string
  itemName: string
  spec: string | null
  unit: ItemUnit
  itemType: ItemType
  safetyStock: number
  itemStatus: ItemStatus
  createdAt: string
}

export interface ItemMasterRequest {
  itemCode?: string | null
  itemName: string
  spec?: string | null
  unit: ItemUnit
  itemType: ItemType
  safetyStock: number
}

export interface ItemMasterUpdateRequest {
  itemName: string
  spec?: string | null
  unit: ItemUnit
  itemType: ItemType
  safetyStock: number
  confirmReferenceWarning?: boolean
}

export interface ItemMasterSearchParams extends PageParams {
  itemType?: ItemType | ''
  itemStatus?: ItemStatus | ''
  keyword?: string
}

export interface ItemReferenceResponse {
  itemId: number
  itemCode: string
  bomParentCount: number
  bomChildCount: number
  inventoryCount: number
  inboundCount: number
  transactionHistoryCount: number
  workOrderCount: number
  shippingCount: number
  hasReferences: boolean
  deletable: boolean
}

export interface ItemUsageResponse {
  itemId: number
  itemCode: string
  currentQtyTotal: number
  asParentBoms: BomMasterResponse[]
  asChildBoms: BomMasterResponse[]
  workOrders: WorkOrderResponse[]
  shippingCount: number
  recentTransactions: TransactionHistoryResponse[]
}

export interface ItemDuplicateCheckResponse {
  hasDuplicates: boolean
  items: ItemMasterResponse[]
}

export interface ItemDuplicateCheckParams {
  itemName: string
  spec?: string | null
  unit: ItemUnit
}

export const itemMasterApi = {
  async getItems(params: ItemMasterSearchParams = {}) {
    const response = await apiClient.get<ApiResponse<PageResponse<ItemMasterResponse>>>('/api/items', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'createdAt,desc',
        itemType: params.itemType || undefined,
        itemStatus: params.itemStatus || undefined,
        keyword: params.keyword || undefined
      }
    })
    return response.data
  },

  async getItem(id: number) {
    const response = await apiClient.get<ApiResponse<ItemMasterResponse>>(`/api/items/${id}`)
    return response.data
  },

  async getItemReferences(id: number) {
    const response = await apiClient.get<ApiResponse<ItemReferenceResponse>>(`/api/items/${id}/references`)
    return response.data
  },

  async getItemUsages(id: number) {
    const response = await apiClient.get<ApiResponse<ItemUsageResponse>>(`/api/items/${id}/usages`)
    return response.data
  },

  async checkDuplicates(params: ItemDuplicateCheckParams) {
    const response = await apiClient.get<ApiResponse<ItemDuplicateCheckResponse>>('/api/items/duplicates', {
      params
    })
    return response.data
  },

  async createItem(request: ItemMasterRequest) {
    const response = await apiClient.post<ApiResponse<ItemMasterResponse>>('/api/items', request)
    return response.data
  },

  async updateItem(id: number, request: ItemMasterUpdateRequest) {
    const response = await apiClient.put<ApiResponse<ItemMasterResponse>>(`/api/items/${id}`, request)
    return response.data
  },

  async updateItemStatus(id: number, itemStatus: ItemStatus) {
    const response = await apiClient.patch<ApiResponse<ItemMasterResponse>>(`/api/items/${id}/status`, {
      itemStatus
    })
    return response.data
  },

  async deleteItem(id: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/items/${id}`)
    return response.data
  }
}
