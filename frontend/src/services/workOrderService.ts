import { AxiosError } from 'axios'
import { workOrderApi } from '@/api/workOrderApi'
import type {
  ProductionExecutionCreateRequest,
  ProductionExecutionResponse,
  WorkOrderCloseRequest,
  WorkOrderDetailResponse,
  WorkOrderRequest,
  WorkOrderResponse,
  WorkOrderSearchParams,
  WorkOrderStatusUpdateRequest
} from '@/api/workOrderApi'
import type { PageResponse } from '@/api/types'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }
  return fallback
}

export const workOrderService = {
  async getWorkOrders(params: WorkOrderSearchParams = {}): Promise<PageResponse<WorkOrderResponse>> {
    try {
      const response = await workOrderApi.getWorkOrders(params)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '작업 지시 목록을 불러오지 못했습니다.'))
    }
  },

  async getWorkOrder(orderKey: string | number): Promise<WorkOrderDetailResponse> {
    try {
      const response = await workOrderApi.getWorkOrder(orderKey)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '작업 지시 상세 정보를 불러오지 못했습니다.'))
    }
  },

  async createWorkOrder(request: WorkOrderRequest): Promise<WorkOrderResponse> {
    try {
      const response = await workOrderApi.createWorkOrder(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '작업 지시 등록에 실패했습니다.'))
    }
  },

  async updateWorkOrder(orderKey: string | number, request: WorkOrderRequest): Promise<WorkOrderResponse> {
    try {
      const response = await workOrderApi.updateWorkOrder(orderKey, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '작업 지시 수정에 실패했습니다.'))
    }
  },

  async deleteWorkOrder(orderKey: string | number): Promise<void> {
    try {
      await workOrderApi.deleteWorkOrder(orderKey)
    } catch (error) {
      throw new Error(getErrorMessage(error, '작업 지시 삭제에 실패했습니다.'))
    }
  },

  async updateStatus(orderKey: string | number, request: WorkOrderStatusUpdateRequest): Promise<WorkOrderResponse> {
    try {
      const response = await workOrderApi.updateStatus(orderKey, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '작업 지시 상태 변경에 실패했습니다.'))
    }
  },

  async closeWorkOrder(orderKey: string | number, request: WorkOrderCloseRequest = {}): Promise<WorkOrderResponse> {
    try {
      const response = await workOrderApi.closeWorkOrder(orderKey, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '작업 지시 마감에 실패했습니다.'))
    }
  },

  async issueMaterials(orderKey: string | number): Promise<void> {
    try {
      await workOrderApi.issueMaterials(orderKey)
    } catch (error) {
      throw new Error(getErrorMessage(error, '자재 출고 처리에 실패했습니다.'))
    }
  },

  async cancelIssueMaterials(orderKey: string | number): Promise<void> {
    try {
      await workOrderApi.cancelIssueMaterials(orderKey)
    } catch (error) {
      throw new Error(getErrorMessage(error, '자재 출고 취소에 실패했습니다.'))
    }
  },

  async getExecutions(orderKey: string | number): Promise<ProductionExecutionResponse[]> {
    try {
      const response = await workOrderApi.getExecutions(orderKey)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '생산 실적 목록을 불러오지 못했습니다.'))
    }
  },

  async createExecution(request: ProductionExecutionCreateRequest): Promise<ProductionExecutionResponse> {
    try {
      const response = await workOrderApi.createExecution(request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '생산 실적 등록에 실패했습니다.'))
    }
  },

  async deleteExecution(executionId: number): Promise<void> {
    try {
      await workOrderApi.deleteExecution(executionId)
    } catch (error) {
      throw new Error(getErrorMessage(error, '생산 실적 삭제에 실패했습니다.'))
    }
  }
}
