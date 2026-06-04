import { AxiosError } from 'axios'
import { itemMasterApi } from '@/api/itemMasterApi'
import type {
  ItemMasterRequest,
  ItemMasterResponse,
  ItemMasterSearchParams
} from '@/api/itemMasterApi'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

export const itemMasterService = {
  async getItems(params: ItemMasterSearchParams = {}): Promise<ItemMasterResponse[]> {
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

  async createItem(request: ItemMasterRequest): Promise<ItemMasterResponse> {
    try {
      const response = await itemMasterApi.createItem(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 등록에 실패했습니다.'))
    }
  },

  async updateItem(id: number, request: ItemMasterRequest): Promise<ItemMasterResponse> {
    try {
      const response = await itemMasterApi.updateItem(id, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 수정에 실패했습니다.'))
    }
  },

  async deleteItem(id: number, force = false): Promise<void> {
    try {
      await itemMasterApi.deleteItem(id, force)
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 삭제에 실패했습니다.'))
    }
  }
}
