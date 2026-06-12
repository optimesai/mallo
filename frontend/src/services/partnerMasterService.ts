import { AxiosError } from 'axios'
import { partnerMasterApi } from '@/api/partnerMasterApi'
import type {
  PartnerMasterRequest,
  PartnerMasterResponse,
  PartnerMasterSearchParams,
  PartnerStatus,
  PartnerSuppliedItemResponse,
  PartnerUsageResponse
} from '@/api/partnerMasterApi'
import type { PageResponse } from '@/api/types'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

export const partnerMasterService = {
  async getPartners(params: PartnerMasterSearchParams = {}): Promise<PageResponse<PartnerMasterResponse>> {
    try {
      const response = await partnerMasterApi.getPartners(params)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 목록을 불러오지 못했습니다.'))
    }
  },

  async searchPartners(searchValue: string): Promise<PartnerMasterResponse[]> {
    try {
      const response = await partnerMasterApi.searchPartners(searchValue)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 상세 정보를 불러오지 못했습니다.'))
    }
  },

  async createPartner(request: PartnerMasterRequest): Promise<PartnerMasterResponse> {
    try {
      const response = await partnerMasterApi.createPartner(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 등록에 실패했습니다.'))
    }
  },

  async updatePartner(id: number, request: PartnerMasterRequest): Promise<PartnerMasterResponse> {
    try {
      const response = await partnerMasterApi.updatePartner(id, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 수정에 실패했습니다.'))
    }
  },

  async updatePartnerStatus(id: number, partnerStatus: PartnerStatus): Promise<PartnerMasterResponse> {
    try {
      const response = await partnerMasterApi.updatePartnerStatus(id, partnerStatus)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 상태 변경에 실패했습니다.'))
    }
  },

  async getPartnerUsage(id: number): Promise<PartnerUsageResponse> {
    try {
      const response = await partnerMasterApi.getPartnerUsage(id)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 사용 현황을 불러오지 못했습니다.'))
    }
  },

  async getSuppliedItems(id: number): Promise<PartnerSuppliedItemResponse[]> {
    try {
      const response = await partnerMasterApi.getSuppliedItems(id)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '공급 품목 이력을 불러오지 못했습니다.'))
    }
  },

  async checkDuplicate(partnerCode: string): Promise<boolean> {
    try {
      const response = await partnerMasterApi.checkDuplicate(partnerCode)
      return response.data.duplicated
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 코드 중복 확인에 실패했습니다.'))
    }
  },

  async deletePartner(id: number): Promise<void> {
    try {
      await partnerMasterApi.deletePartner(id)
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 삭제에 실패했습니다.'))
    }
  }
}
