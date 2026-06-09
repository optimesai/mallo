import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'

export interface FactoryRoutingResponse {
  routingId: number
  factoryName: string
  lineName: string
  operationSeq: number
  operationName: string
  createdAt: string
}

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
