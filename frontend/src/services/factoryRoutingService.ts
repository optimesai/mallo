import { AxiosError } from 'axios'
import { factoryRoutingApi } from '@/api/factoryRoutingApi'
import type {
  FactoryRoutingRequest,
  FactoryRoutingResponse,
  FactoryRoutingSearchParams,
  FactoryRoutingTreeResponse
} from '@/api/factoryRoutingApi'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

export const factoryRoutingService = {
  async getRoutings(params: FactoryRoutingSearchParams = {}): Promise<FactoryRoutingResponse[]> {
    try {
      const response = await factoryRoutingApi.getRoutings(params)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '라우팅 목록을 불러오지 못했습니다.'))
    }
  },

  async getRouting(id: number): Promise<FactoryRoutingResponse> {
    try {
      const response = await factoryRoutingApi.getRouting(id)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '라우팅 상세 정보를 불러오지 못했습니다.'))
    }
  },

  async createRouting(request: FactoryRoutingRequest): Promise<FactoryRoutingResponse> {
    try {
      const response = await factoryRoutingApi.createRouting(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '라우팅 등록에 실패했습니다.'))
    }
  },

  async updateRouting(id: number, request: FactoryRoutingRequest): Promise<FactoryRoutingResponse> {
    try {
      const response = await factoryRoutingApi.updateRouting(id, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '라우팅 수정에 실패했습니다.'))
    }
  },

  async deleteRouting(id: number): Promise<void> {
    try {
      await factoryRoutingApi.deleteRouting(id)
    } catch (error) {
      throw new Error(getErrorMessage(error, '라우팅 삭제에 실패했습니다.'))
    }
  },

  async getFactories(): Promise<string[]> {
    try {
      const response = await factoryRoutingApi.getFactories()
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '공장 목록을 불러오지 못했습니다.'))
    }
  },

  async getLines(factoryName: string): Promise<string[]> {
    try {
      const response = await factoryRoutingApi.getLines(factoryName)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '라인 목록을 불러오지 못했습니다.'))
    }
  },

  async getOperations(factoryName: string, lineName: string): Promise<FactoryRoutingResponse[]> {
    try {
      const response = await factoryRoutingApi.getOperations(factoryName, lineName)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '공정 목록을 불러오지 못했습니다.'))
    }
  },

  async getRoutingTree(): Promise<FactoryRoutingTreeResponse[]> {
    try {
      const response = await factoryRoutingApi.getRoutingTree()
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '라우팅 트리를 불러오지 못했습니다.'))
    }
  }
}
