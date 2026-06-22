import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { PageResponse } from '@/api/types'

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
  status: ShippingStatus
  workerName: string | null
  shippedAt: string | null
}

export type ShippingStatus = 'READY' | 'PICKING' | 'PACKING' | 'INSPECTING' | 'SHIPPED' | 'PARTIALLY_SHIPPED' | 'CANCELED'

export interface ShippingCreateRequest {
  shippingNo: string
  partnerCode: string
  itemCode: string
  requestQty: number
}

export interface PickingAssignRequest {
  vehicleNo: string
}

export interface ShippingListParams {
  page?: number
  size?: number
  sort?: string
  status?: string
  keyword?: string
}

export const shippingApi = {
  async getShippings(params: ShippingListParams = {}) {
    const response = await apiClient.get<ApiResponse<PageResponse<ShippingResponse>>>('/api/shippings', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? 20,
        sort: params.sort ?? 'createdAt,desc',
        status: params.status || undefined,
        keyword: params.keyword || undefined,
      }
    })
    return response.data
  },

  async getShipping(id: number) {
    const response = await apiClient.get<ApiResponse<ShippingResponse>>(`/api/shippings/${id}`)
    return response.data
  },

  async registerShipping(request: ShippingCreateRequest) {
    const response = await apiClient.post<ApiResponse<ShippingResponse>>('/api/shippings', request)
    return response.data
  },

  async assignPicking(id: number, request: PickingAssignRequest) {
    const response = await apiClient.put<ApiResponse<ShippingResponse>>(`/api/shippings/${id}/assign-picking`, request)
    return response.data
  },

  async completeShipping(id: number) {
    const response = await apiClient.put<ApiResponse<void>>(`/api/shippings/${id}/complete`, null)
    return response.data
  }
}
