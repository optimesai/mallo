import { AxiosError } from 'axios'
import { partnerMasterApi } from '@/api/partnerMasterApi'
import type {
  PartnerMasterRequest,
  PartnerMasterResponse,
  PartnerMasterSearchParams
} from '@/api/partnerMasterApi'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

export const partnerMasterService = {
  async getPartners(params: PartnerMasterSearchParams = {}): Promise<PartnerMasterResponse[]> {
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

  async deletePartner(id: number): Promise<void> {
    try {
      await partnerMasterApi.deletePartner(id)
    } catch (error) {
      throw new Error(getErrorMessage(error, '거래처 삭제에 실패했습니다.'))
    }
  }
}
