import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'

export type PartnerType = 'SUPPLIER' | 'CUSTOMER'

export interface PartnerMasterResponse {
  partnerId: number
  partnerCode: string
  partnerName: string
  partnerType: PartnerType
  businessNo: string | null
  representative: string | null
  contactPhone: string | null
  createdAt: string
}

export interface PartnerMasterRequest {
  partnerCode: string
  partnerName: string
  partnerType: PartnerType
  businessNo?: string | null
  representative?: string | null
  contactPhone?: string | null
}

export interface PartnerMasterSearchParams {
  partnerType?: PartnerType
  keyword?: string
}

export const partnerMasterApi = {
  async getPartners(params: PartnerMasterSearchParams = {}) {
    const response = await apiClient.get<ApiResponse<PartnerMasterResponse[]>>('/api/partners', {
      params
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

  async deletePartner(id: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/partners/${id}`)
    return response.data
  }
}
