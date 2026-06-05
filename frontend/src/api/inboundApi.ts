import axios from 'axios'
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
  createdAt: string
}

export interface PartnerResponse {
  partnerId: number
  partnerCode: string
  partnerName: string
  partnerType: 'SUPPLIER' | 'CUSTOMER'
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
}

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const inboundApi = {
  async getInbounds(params: InboundListParams = {}) {
    const response = await axios.get<ApiResponse<PageResponse<InboundReceiptResponse>>>('/api/inbounds', {
      headers: getAuthHeaders(),
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
    const response = await axios.get<ApiResponse<InboundReceiptResponse>>(`/api/inbounds/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async registerInbound(request: InboundCreateRequest) {
    const response = await axios.post<ApiResponse<InboundReceiptResponse>>('/api/inbounds', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async completeInbound(id: number) {
    const response = await axios.put<ApiResponse<InboundReceiptResponse>>(`/api/inbounds/${id}/complete`, null, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async stackInventory(id: number, request: InventoryStackRequest) {
    const response = await axios.post<ApiResponse<void>>(`/api/inbounds/${id}/stack`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async deleteInbound(id: number) {
    const response = await axios.delete<ApiResponse<void>>(`/api/inbounds/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getItems(itemType?: string, keyword?: string) {
    const response = await axios.get<ApiResponse<ItemResponse[]>>('/api/items', {
      headers: getAuthHeaders(),
      params: { itemType, keyword }
    })
    return response.data
  },

  async getPartners(partnerType?: string, keyword?: string) {
    const response = await axios.get<ApiResponse<PartnerResponse[]>>('/api/partners', {
      headers: getAuthHeaders(),
      params: { partnerType, keyword }
    })
    return response.data
  },

  async getLocations() {
    const response = await axios.get<ApiResponse<LocationResponse[]>>('/api/locations', {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
