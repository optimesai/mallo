import axios from 'axios'
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

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const partnerMasterApi = {
  async getPartners(params: PartnerMasterSearchParams = {}) {
    const response = await axios.get<ApiResponse<PartnerMasterResponse[]>>('/api/partners', {
      headers: getAuthHeaders(),
      params
    })
    return response.data
  },

  async searchPartners(searchValue: string) {
    const response = await axios.get<ApiResponse<PartnerMasterResponse[]>>(`/api/partners/${searchValue}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async createPartner(request: PartnerMasterRequest) {
    const response = await axios.post<ApiResponse<PartnerMasterResponse>>('/api/partners', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async updatePartner(id: number, request: PartnerMasterRequest) {
    const response = await axios.put<ApiResponse<PartnerMasterResponse>>(`/api/partners/${id}`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async deletePartner(id: number) {
    const response = await axios.delete<ApiResponse<void>>(`/api/partners/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
