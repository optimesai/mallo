import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'

export interface FactoryRoutingResponse {
  routingId: number
  factoryName: string
  lineName: string
  operationSeq: number
  operationName: string
  routingStatus: FactoryRoutingStatus
  createdAt: string
  lastExecutionAt: string | null
}

export type FactoryRoutingStatus = 'ACTIVE' | 'INACTIVE'

export interface FactoryRoutingRequest {
  factoryName: string
  lineName: string
  operationSeq: number
  operationName: string
}

export interface FactoryRoutingTreeResponse {
  factoryName: string
  lines: FactoryRoutingLineResponse[]
}

export interface FactoryRoutingLineResponse {
  lineName: string
  operations: FactoryRoutingOperationResponse[]
}

export interface FactoryRoutingOperationResponse {
  routingId: number
  operationSeq: number
  operationName: string
}

export interface FactoryRoutingSearchParams {
  factoryName?: string
  lineName?: string
  routingStatus?: FactoryRoutingStatus
}

export interface FactoryRoutingStatusUpdateRequest {
  routingStatus: FactoryRoutingStatus
}

export interface FactoryRoutingUsageResponse {
  routingId: number
  workOrderCount: number
  executionCount: number
  workOrderNos: string[]
  executionIds: number[]
  canUpdate: boolean
  canDelete: boolean
  recommendedAction: string
}

function encodePath(value: string) {
  return encodeURIComponent(value)
}

export const factoryRoutingApi = {
  async getRoutings(params: FactoryRoutingSearchParams = {}) {
    const response = await apiClient.get<ApiResponse<FactoryRoutingResponse[]>>('/api/routings', {
      params
    })
    return response.data
  },

  async getRouting(id: number) {
    const response = await apiClient.get<ApiResponse<FactoryRoutingResponse>>(`/api/routings/${id}`)
    return response.data
  },

  async createRouting(request: FactoryRoutingRequest) {
    const response = await apiClient.post<ApiResponse<FactoryRoutingResponse>>('/api/routings', request)
    return response.data
  },

  async updateRouting(id: number, request: FactoryRoutingRequest) {
    const response = await apiClient.put<ApiResponse<FactoryRoutingResponse>>(`/api/routings/${id}`, request)
    return response.data
  },

  async deleteRouting(id: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/routings/${id}`)
    return response.data
  },

  async updateRoutingStatus(id: number, request: FactoryRoutingStatusUpdateRequest) {
    const response = await apiClient.patch<ApiResponse<FactoryRoutingResponse>>(`/api/routings/${id}/status`, request)
    return response.data
  },

  async getRoutingUsage(id: number) {
    const response = await apiClient.get<ApiResponse<FactoryRoutingUsageResponse>>(`/api/routings/${id}/usage`)
    return response.data
  },

  async getFactories() {
    const response = await apiClient.get<ApiResponse<string[]>>('/api/routings/factories')
    return response.data
  },

  async getLines(factoryName: string) {
    const response = await apiClient.get<ApiResponse<string[]>>(`/api/routings/factories/${encodePath(factoryName)}/lines`)
    return response.data
  },

  async getOperations(factoryName: string, lineName: string) {
    const response = await apiClient.get<ApiResponse<FactoryRoutingResponse[]>>(
      `/api/routings/factories/${encodePath(factoryName)}/lines/${encodePath(lineName)}/operations`,
      {
      }
    )
    return response.data
  },

  async getRoutingTree() {
    const response = await apiClient.get<ApiResponse<FactoryRoutingTreeResponse[]>>('/api/routings/tree')
    return response.data
  }
}
