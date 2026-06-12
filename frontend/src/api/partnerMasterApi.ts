import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { PageResponse } from '@/api/types'

export type PartnerType = 'SUPPLIER' | 'CUSTOMER'
export type PartnerStatus = 'ACTIVE' | 'INACTIVE'

export interface PartnerMasterResponse {
  partnerId: number
  partnerCode: string
  partnerName: string
  partnerType: PartnerType
  partnerStatus: PartnerStatus
  businessNo: string | null
  representative: string | null
  contactPhone: string | null
  contactEmail: string | null
  note: string | null
  createdAt: string
  lastUsedAt: string | null
  inboundCount: number
  shippingCount: number
  usageCount: number
}

export interface PartnerMasterRequest {
  partnerCode: string
  partnerName: string
  partnerType: PartnerType
  businessNo?: string | null
  representative?: string | null
  contactPhone?: string | null
  contactEmail?: string | null
  note?: string | null
}

export interface PartnerMasterSearchParams {
  page?: number
  size?: number
  sort?: string
  partnerType?: PartnerType
  partnerStatus?: PartnerStatus
  hasBusinessNo?: boolean
  keyword?: string
}

export interface PartnerDuplicateCheckResponse {
  duplicated: boolean
}

export interface PartnerUsageResponse {
  partnerId: number
  inboundCount: number
  shippingCount: number
  lastInboundAt: string | null
  lastShippingAt: string | null
  lastUsedAt: string | null
  canDelete: boolean
  deleteBlockedReason: string | null
}

export interface PartnerSuppliedItemResponse {
  itemCode: string
  itemName: string
  itemType: 'RAW' | 'HALF' | 'FG'
  unit: string
  totalInboundQty: number
  inboundCount: number
  lastInboundDate: string | null
}

export interface PartnerShippedItemResponse {
  itemCode: string
  itemName: string
  itemType: 'RAW' | 'HALF' | 'FG'
  unit: string
  totalShippingQty: number
  shippingCount: number
  lastShippingAt: string | null
}

export const partnerMasterApi = {
  async getPartners(params: PartnerMasterSearchParams = {}) {
    const response = await apiClient.get<ApiResponse<PageResponse<PartnerMasterResponse>>>('/api/partners', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: params.sort ?? 'createdAt,desc',
        partnerType: params.partnerType,
        partnerStatus: params.partnerStatus,
        hasBusinessNo: params.hasBusinessNo,
        keyword: params.keyword || undefined,
      }
    })
    return response.data
  },

  async checkDuplicate(partnerCode: string) {
    const response = await apiClient.get<ApiResponse<PartnerDuplicateCheckResponse>>('/api/partners/duplicates', {
      params: { partnerCode }
    })
    return response.data
  },

  async searchPartners(searchValue: string) {
    const response = await apiClient.get<ApiResponse<PartnerMasterResponse[]>>(`/api/partners/${searchValue}`)
    return response.data
  },

  async createPartner(request: PartnerMasterRequest) {
    const response = await apiClient.post<ApiResponse<PartnerMasterResponse>>('/api/partners', request)
    return response.data
  },

  async updatePartner(id: number, request: PartnerMasterRequest) {
    const response = await apiClient.put<ApiResponse<PartnerMasterResponse>>(`/api/partners/${id}`, request)
    return response.data
  },

  async updatePartnerStatus(id: number, partnerStatus: PartnerStatus) {
    const response = await apiClient.patch<ApiResponse<PartnerMasterResponse>>(`/api/partners/${id}/status`, {
      partnerStatus
    })
    return response.data
  },

  async getPartnerUsage(id: number) {
    const response = await apiClient.get<ApiResponse<PartnerUsageResponse>>(`/api/partners/${id}/usage`)
    return response.data
  },

  async getSuppliedItems(id: number) {
    const response = await apiClient.get<ApiResponse<PartnerSuppliedItemResponse[]>>(`/api/partners/${id}/supplied-items`)
    return response.data
  },

  async getShippedItems(id: number) {
    const response = await apiClient.get<ApiResponse<PartnerShippedItemResponse[]>>(`/api/partners/${id}/shipped-items`)
    return response.data
  },

  async deletePartner(id: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/partners/${id}`)
    return response.data
  }
}
