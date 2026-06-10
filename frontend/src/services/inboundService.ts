import { AxiosError } from 'axios'
import { inboundApi } from '@/api/inboundApi'
import type {
  InboundReceiptResponse,
  InboundCreateRequest,
  InventoryStackRequest,
  InboundListParams,
  ItemResponse,
  PartnerResponse,
  LocationResponse
} from '@/api/inboundApi'
import type { PageResponse } from '@/api/types'

export const inboundService = {
  async getInbounds(params: InboundListParams = {}): Promise<PageResponse<InboundReceiptResponse>> {
    try {
      const response = await inboundApi.getInbounds(params)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '입고 목록을 불러오지 못했습니다.')
      }
      throw new Error('입고 목록을 불러오지 못했습니다.')
    }
  },

  async getInbound(id: number): Promise<InboundReceiptResponse> {
    try {
      const response = await inboundApi.getInbound(id)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '입고 상세 정보를 불러오지 못했습니다.')
      }
      throw new Error('입고 상세 정보를 불러오지 못했습니다.')
    }
  },

  async registerInbound(request: InboundCreateRequest): Promise<InboundReceiptResponse> {
    try {
      const response = await inboundApi.registerInbound(request)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '입고 예정 등록에 실패했습니다.')
      }
      throw new Error('입고 예정 등록에 실패했습니다.')
    }
  },

  async completeInbound(id: number): Promise<InboundReceiptResponse> {
    try {
      const response = await inboundApi.completeInbound(id)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '입고 완료 처리에 실패했습니다.')
      }
      throw new Error('입고 완료 처리에 실패했습니다.')
    }
  },

  async stackInventory(id: number, request: InventoryStackRequest): Promise<void> {
    try {
      await inboundApi.stackInventory(id, request)
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '창고 적재에 실패했습니다.')
      }
      throw new Error('창고 적재에 실패했습니다.')
    }
  },

  async deleteInbound(id: number): Promise<void> {
    try {
      await inboundApi.deleteInbound(id)
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '입고 삭제에 실패했습니다.')
      }
      throw new Error('입고 삭제에 실패했습니다.')
    }
  },

  async getItems(itemType?: string, keyword?: string): Promise<ItemResponse[]> {
    try {
      const response = await inboundApi.getItems(itemType, keyword)
      return response.data.content
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '품목 목록을 불러오지 못했습니다.')
      }
      throw new Error('품목 목록을 불러오지 못했습니다.')
    }
  },

  async getPartners(partnerType?: string, keyword?: string): Promise<PartnerResponse[]> {
    try {
      const response = await inboundApi.getPartners(partnerType, keyword)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '거래처 목록을 불러오지 못했습니다.')
      }
      throw new Error('거래처 목록을 불러오지 못했습니다.')
    }
  },

  async getLocations(): Promise<LocationResponse[]> {
    try {
      const response = await inboundApi.getLocations()
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '로케이션 목록을 불러오지 못했습니다.')
      }
      throw new Error('로케이션 목록을 불러오지 못했습니다.')
    }
  }
}
