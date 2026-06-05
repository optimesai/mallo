import axios from 'axios'
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

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

function encodePath(value: string) {
  return encodeURIComponent(value)
}

export const factoryRoutingApi = {
  async getRoutings(params: FactoryRoutingSearchParams = {}) {
    const response = await axios.get<ApiResponse<FactoryRoutingResponse[]>>('/api/routings', {
      headers: getAuthHeaders(),
      params
    })
    return response.data
  },

  async getRouting(id: number) {
    const response = await axios.get<ApiResponse<FactoryRoutingResponse>>(`/api/routings/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async createRouting(request: FactoryRoutingRequest) {
    const response = await axios.post<ApiResponse<FactoryRoutingResponse>>('/api/routings', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async updateRouting(id: number, request: FactoryRoutingRequest) {
    const response = await axios.put<ApiResponse<FactoryRoutingResponse>>(`/api/routings/${id}`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async deleteRouting(id: number) {
    const response = await axios.delete<ApiResponse<void>>(`/api/routings/${id}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getFactories() {
    const response = await axios.get<ApiResponse<string[]>>('/api/routings/factories', {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getLines(factoryName: string) {
    const response = await axios.get<ApiResponse<string[]>>(`/api/routings/factories/${encodePath(factoryName)}/lines`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getOperations(factoryName: string, lineName: string) {
    const response = await axios.get<ApiResponse<FactoryRoutingResponse[]>>(
      `/api/routings/factories/${encodePath(factoryName)}/lines/${encodePath(lineName)}/operations`,
      {
        headers: getAuthHeaders()
      }
    )
    return response.data
  },

  async getRoutingTree() {
    const response = await axios.get<ApiResponse<FactoryRoutingTreeResponse[]>>('/api/routings/tree', {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
