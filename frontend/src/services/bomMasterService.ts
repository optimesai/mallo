import { AxiosError } from 'axios'
import { bomMasterApi } from '@/api/bomMasterApi'
import type { ItemMasterResponse, ItemType } from '@/api/itemMasterApi'
import type {
  BomBulkRequest,
  BomGroupResponse,
  BomGroupSearchParams,
  BomMasterRequest,
  BomMasterResponse,
  BomMasterSearchParams,
  BomReverseResponse,
  BomTreeNode
} from '@/api/bomMasterApi'
import type { PageResponse } from '@/api/types'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

export const bomMasterService = {
  async getBomGroups(params: BomGroupSearchParams = {}): Promise<PageResponse<BomGroupResponse>> {
    try {
      const response = await bomMasterApi.getBomGroups(params)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 목록을 불러오지 못했습니다.'))
    }
  },

  async getBomGroup(parentItemId: number, bomVersion: string): Promise<BomMasterResponse[]> {
    try {
      const response = await bomMasterApi.getBomGroup(parentItemId, bomVersion)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 상세를 불러오지 못했습니다.'))
    }
  },

  async getBoms(params: BomMasterSearchParams = {}): Promise<BomMasterResponse[]> {
    try {
      const response = await bomMasterApi.getBoms(params)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 목록을 불러오지 못했습니다.'))
    }
  },

  async createBom(request: BomMasterRequest): Promise<BomMasterResponse> {
    try {
      const response = await bomMasterApi.createBom(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 등록에 실패했습니다.'))
    }
  },

  async createBoms(request: BomBulkRequest): Promise<BomMasterResponse[]> {
    try {
      const response = await bomMasterApi.createBoms(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 일괄 등록에 실패했습니다.'))
    }
  },

  async updateBom(bomId: number, request: BomMasterRequest): Promise<BomMasterResponse> {
    try {
      const response = await bomMasterApi.updateBom(bomId, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 수정에 실패했습니다.'))
    }
  },

  async deleteBom(bomId: number): Promise<void> {
    try {
      await bomMasterApi.deleteBom(bomId)
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 삭제에 실패했습니다.'))
    }
  },

  async updateBomStatus(bomId: number, bomStatus: 'ACTIVE' | 'INACTIVE'): Promise<BomMasterResponse> {
    try {
      const response = await bomMasterApi.updateBomStatus(bomId, bomStatus)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 상태 변경에 실패했습니다.'))
    }
  },

  async getParentVersions(keyword: string): Promise<string[]> {
    try {
      const response = await bomMasterApi.getParentVersions(keyword)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 버전 목록을 불러오지 못했습니다.'))
    }
  },

  async getChildVersions(keyword: string): Promise<string[]> {
    try {
      const response = await bomMasterApi.getChildVersions(keyword)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 버전 목록을 불러오지 못했습니다.'))
    }
  },

  async getParentTree(keyword: string, bomVersion?: string): Promise<BomTreeNode[]> {
    try {
      const response = await bomMasterApi.getParentTree(keyword, bomVersion)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '하향 BOM 트리를 불러오지 못했습니다.'))
    }
  },

  async getChildParentTree(keyword: string, bomVersion?: string): Promise<BomTreeNode[]> {
    try {
      const response = await bomMasterApi.getChildParentTree(keyword, bomVersion)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '상향 BOM 트리를 불러오지 못했습니다.'))
    }
  },

  async getChildParents(keyword: string, bomVersion?: string): Promise<BomReverseResponse[]> {
    try {
      const response = await bomMasterApi.getChildParents(keyword, bomVersion)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, 'BOM 역조회 목록을 불러오지 못했습니다.'))
    }
  },

  async getItems(itemType?: ItemType, keyword?: string): Promise<ItemMasterResponse[]> {
    try {
      const firstResponse = await bomMasterApi.getItems(itemType, keyword)
      const firstPage = firstResponse.data
      if (firstPage.totalPages <= 1) return firstPage.content

      const restPages = await Promise.all(
        Array.from({ length: firstPage.totalPages - 1 }, (_, index) => bomMasterApi.getItems(itemType, keyword, index + 1))
      )
      return [
        firstPage.content,
        ...restPages.map((response) => response.data.content)
      ].flat()
    } catch (error) {
      throw new Error(getErrorMessage(error, '품목 목록을 불러오지 못했습니다.'))
    }
  }
}
