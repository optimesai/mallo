import { AxiosError } from 'axios'
import { itemMasterApi } from '@/api/itemMasterApi'
import type { PageResponse } from '@/api/types'
import type {
  ItemDuplicateCheckParams,
  ItemDuplicateCheckResponse,
  ItemMasterRequest,
  ItemMasterResponse,
  ItemMasterSearchParams,
  ItemStatsResponse,
  ItemMasterUpdateRequest,
  ItemReferenceResponse,
  ItemStatus,
  ItemUsageResponse
} from '@/api/itemMasterApi'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

export const itemMasterService = {
  async getItems(params: ItemMasterSearchParams = {}): Promise<PageResponse<ItemMasterResponse>> {
    try {
      const response = await itemMasterApi.getItems(params)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 목록을 불러오지 못했습니다.'))
    }
  },

  async getItem(id: number): Promise<ItemMasterResponse> {
    try {
      const response = await itemMasterApi.getItem(id)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 상세 정보를 불러오지 못했습니다.'))
    }
  },

  async getItemStats(): Promise<ItemStatsResponse> {
    try {
      const [total, active, inactive] = await Promise.all([
        itemMasterApi.getItems({ page: 0, size: 1 }),
        itemMasterApi.getItems({ page: 0, size: 1, itemStatus: 'ACTIVE' }),
        itemMasterApi.getItems({ page: 0, size: 1, itemStatus: 'INACTIVE' })
      ])
      return {
        totalCount: total.data.totalElements,
        activeCount: active.data.totalElements,
        inactiveCount: inactive.data.totalElements
      }
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 통계를 불러오지 못했습니다.'))
    }
  },

  async getItemReferences(id: number): Promise<ItemReferenceResponse> {
    try {
      const response = await itemMasterApi.getItemReferences(id)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 참조 현황을 불러오지 못했습니다.'))
    }
  },

  async getItemUsages(id: number): Promise<ItemUsageResponse> {
    try {
      const response = await itemMasterApi.getItemUsages(id)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 활용 정보를 불러오지 못했습니다.'))
    }
  },

  async checkDuplicates(params: ItemDuplicateCheckParams): Promise<ItemDuplicateCheckResponse> {
    try {
      const response = await itemMasterApi.checkDuplicates(params)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '중복 품목 검증에 실패했습니다.'))
    }
  },

  async createItem(request: ItemMasterRequest): Promise<ItemMasterResponse> {
    try {
      const response = await itemMasterApi.createItem(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 등록에 실패했습니다.'))
    }
  },

  async updateItem(id: number, request: ItemMasterUpdateRequest): Promise<ItemMasterResponse> {
    try {
      const response = await itemMasterApi.updateItem(id, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 수정에 실패했습니다.'))
    }
  },

  async updateItemStatus(id: number, itemStatus: ItemStatus): Promise<ItemMasterResponse> {
    try {
      const response = await itemMasterApi.updateItemStatus(id, itemStatus)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 상태 변경에 실패했습니다.'))
    }
  },

  async deleteItem(id: number): Promise<void> {
    try {
      await itemMasterApi.deleteItem(id)
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 삭제에 실패했습니다.'))
    }
  }
}
