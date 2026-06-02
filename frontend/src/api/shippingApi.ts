import axios from 'axios'
import type { ApiResponse } from '@/api/authApi'

export interface ShippingResponse {
  shippingId: number
  shippingNo: string
  partnerCode: string
  partnerName: string
  itemCode: string
  itemName: string
  requestQty: number
  pickingLocationCode: string | null
  vehicleNo: string | null
  status: 'READY' | 'PICKING' | 'SHIPPED'
  workerName: string | null
  shippedAt: string | null
}

export interface ShippingCreateRequest {
  shippingNo: string
  partnerCode: string
  itemCode: string
  requestQty: number
}

export interface PickingAssignRequest {
  vehicleNo: string
}

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

export const shippingApi = {
  async getShippings() {
    const response = await axios.get<ApiResponse<ShippingResponse[]>>('/api/shippings', {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getShipping(id: number) {
    const response = await axios.get<ApiResponse<ShippingResponse>>(`/api/shippings/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async registerShipping(request: ShippingCreateRequest) {
    const response = await axios.post<ApiResponse<ShippingResponse>>('/api/shippings', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async assignPicking(id: number, request: PickingAssignRequest) {
    const response = await axios.put<ApiResponse<ShippingResponse>>(`/api/shippings/${id}/assign-picking`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async completeShipping(id: number) {
    const response = await axios.put<ApiResponse<void>>(`/api/shippings/${id}/complete`, null, {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
