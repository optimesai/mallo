import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { PageResponse } from '@/api/types'

export interface InboundReceiptResponse {
  inboundId: number
  itemCode: string
  itemName: string
  partnerCode: string
  partnerName: string
  locationCode: string
  inboundQty: number
  inboundDate: string
  status: 'READY' | 'COMPLETED' | 'STACKED'
  workerName?: string
  createdAt: string
}

export interface InboundCreateRequest {
  itemCode: string
  partnerCode: string
  locationCode: string
  inboundQty: number
  inboundDate: string
}

export interface InventoryStackRequest {
  targetLocationCode: string
}

export interface InboundListParams {
  page?: number
  size?: number
  sort?: string
  status?: string
  keyword?: string
  startDate?: string
  endDate?: string
}

export interface ItemResponse {
  itemId: number
  itemCode: string
  itemName: string
  spec: string
  unit: string
  itemType: 'RAW' | 'HALF' | 'FG'
  safetyStock: number
  itemStatus: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

export interface PartnerResponse {
  partnerId: number
  partnerCode: string
  partnerName: string
  partnerType: 'SUPPLIER' | 'CUSTOMER'
  partnerStatus: 'ACTIVE' | 'INACTIVE'
  businessNo: string
  representative: string
  contactPhone: string
  createdAt: string
}

export interface LocationResponse {
  locationId: number
  locationCode: string
  warehouseName: string
  rackRow: string
  rackColumn: string
  productionReceiptDefault: boolean
}

export const inboundApi = {
  async getInbounds(params: InboundListParams = {}) {
    const response = await apiClient.get<ApiResponse<PageResponse<InboundReceiptResponse>>>('/api/v2/inbounds', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: params.sort ?? 'createdAt,desc',
        status: params.status || undefined,
        keyword: params.keyword || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
      }
    })
    return response.data
  },

  async getInbound(id: number) {
    const response = await apiClient.get<ApiResponse<InboundReceiptResponse>>(`/api/v2/inbounds/${id}`)
    return response.data
  },

  async registerInbound(request: InboundCreateRequest) {
    const response = await apiClient.post<ApiResponse<InboundReceiptResponse>>('/api/v2/inbounds', request)
    return response.data
  },

  async completeInbound(id: number) {
    const response = await apiClient.put<ApiResponse<InboundReceiptResponse>>(`/api/v2/inbounds/${id}/complete`, null)
    return response.data
  },

  async stackInventory(id: number, request: InventoryStackRequest) {
    const response = await apiClient.post<ApiResponse<void>>(`/api/v2/inbounds/${id}/stack`, request)
    return response.data
  },

  async deleteInbound(id: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/v2/inbounds/${id}`)
    return response.data
  },

  async getItems(itemType?: string, keyword?: string) {
    const response = await apiClient.get<ApiResponse<PageResponse<ItemResponse>>>('/api/items', {
      params: { itemType, keyword, itemStatus: 'ACTIVE', page: 0, size: 100, sort: 'itemCode,asc' }
    })
    return response.data
  },

  async getPartners(partnerType?: string, keyword?: string) {
    const response = await apiClient.get<ApiResponse<PageResponse<PartnerResponse>>>('/api/partners', {
      params: { partnerType, keyword, partnerStatus: 'ACTIVE', page: 0, size: 100, sort: 'partnerName,asc' }
    })
    return {
      ...response.data,
      data: response.data.data.content
    }
  },

  async getLocations() {
    const response = await apiClient.get<ApiResponse<PageResponse<LocationResponse>>>('/api/locations', {
      params: { page: 0, size: 100, sort: 'locationCode,asc' }
    })
    return {
      ...response.data,
      data: response.data.data.content
    }
  }
}
