import { AxiosError } from 'axios'
import { shippingApi } from '@/api/shippingApi'
import type {
  ShippingResponse,
  ShippingCreateRequest,
  PickingAssignRequest
} from '@/api/shippingApi'

export const shippingService = {
  async getShippings(): Promise<ShippingResponse[]> {
    try {
      const response = await shippingApi.getShippings()
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '출하 목록을 불러오지 못했습니다.')
      }
      throw new Error('출하 목록을 불러오지 못했습니다.')
    }
  },

  async getShipping(id: number): Promise<ShippingResponse> {
    try {
      const response = await shippingApi.getShipping(id)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '출하 상세 정보를 불러오지 못했습니다.')
      }
      throw new Error('출하 상세 정보를 불러오지 못했습니다.')
    }
  },

  async registerShipping(request: ShippingCreateRequest): Promise<ShippingResponse> {
    try {
      const response = await shippingApi.registerShipping(request)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '출하 지시 등록에 실패했습니다.')
      }
      throw new Error('출하 지시 등록에 실패했습니다.')
    }
  },

  async assignPicking(id: number, request: PickingAssignRequest): Promise<ShippingResponse> {
    try {
      const response = await shippingApi.assignPicking(id, request)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '차량 배정 및 피킹 지시에 실패했습니다.')
      }
      throw new Error('차량 배정 및 피킹 지시에 실패했습니다.')
    }
  },

  async completeShipping(id: number): Promise<void> {
    try {
      await shippingApi.completeShipping(id)
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        // 백엔드 비즈니스 룰 예외(INSUFFICIENT_STOCK 등) 전달을 위해 그대로 메시지 반환
        throw new Error(message || '출하 완료 처리에 실패했습니다.')
      }
      throw new Error('출하 완료 처리에 실패했습니다.')
    }
  }
}
