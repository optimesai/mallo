import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { PageParams, PageResponse } from '@/api/types'

export type WorkOrderStatus = 'READY' | 'RUN' | 'HOLD' | 'CLOSE'

export interface WorkOrderResponse {
  orderId: number
  orderNo: string
  itemId: number
  itemCode: string
  itemName: string
  itemType: string
  routingId: number
  factoryName: string
  lineName: string
  operationSeq: number
  operationName: string
  targetQty: number
  bomVersion: string
  status: WorkOrderStatus
  planDate: string
  createdAt: string
  updatedAt: string
  totalGoodQty: number
  totalDefectQty: number
  totalExecutedQty: number
  totalManHoursMinutes: number
  progressRate: number
  canIssueMaterials: boolean
  canStart: boolean
  canHold: boolean
  canClose: boolean
  canRegisterExecution: boolean
  canCancelIssue: boolean
  canDeleteExecution: boolean
  canUpdate: boolean
  canDelete: boolean
}

export interface WorkOrderMaterialRequirementResponse {
  itemId: number
  itemCode: string
  itemName: string
  itemType: string
  unit: string
  bomQuantity: number
  requiredQty: number
  issuedQty: number
  availableQty: number
}

export interface ProductionExecutionResponse {
  executionId: number
  orderId: number
  orderNo: string
  routingId: number | null
  factoryName: string | null
  lineName: string | null
  operationSeq: number | null
  operationName: string | null
  goodQty: number
  defectQty: number
  defectType: string | null
  defectReason: string | null
  reworkable: boolean | null
  executedQty: number
  workerId: number | null
  workerEmployeeNo: string | null
  workerName: string | null
  manHoursMinutes: number
  canDelete: boolean
  createdAt: string
}

export interface ProductionIssueHistoryResponse {
  transactionId: number
  itemId: number
  itemCode: string
  itemName: string
  locationId: number
  locationCode: string
  transactionType: string
  quantity: number
  reasonDesc: string
  workerId: number | null
  workerEmployeeNo: string | null
  workerName: string | null
  executionId: number | null
  originalTransactionId: number | null
  createdAt: string
}

export interface WorkOrderOperationProgressResponse {
  routingId: number
  factoryName: string
  lineName: string
  operationSeq: number
  operationName: string
  targetQty: number
  availableQty: number
  completedGoodQty: number
  completedDefectQty: number
  completedQty: number
  currentOperation: boolean
  completed: boolean
}

export interface WorkOrderDetailResponse {
  workOrder: WorkOrderResponse
  materialRequirements: WorkOrderMaterialRequirementResponse[]
  operationProgresses: WorkOrderOperationProgressResponse[]
  executions: ProductionExecutionResponse[]
  issueHistories: ProductionIssueHistoryResponse[]
}

export interface WorkOrderRequest {
  itemCode: string
  routingId: number
  targetQty: number
  bomVersion?: string
  planDate: string
}

export interface WorkOrderSearchParams extends PageParams {
  status?: WorkOrderStatus | ''
  planDate?: string
  fromDate?: string
  toDate?: string
  keyword?: string
  itemKeyword?: string
  factoryName?: string
  lineName?: string
  operationName?: string
}

export interface WorkOrderStatusUpdateRequest {
  status: Exclude<WorkOrderStatus, 'CLOSE'>
}

export interface WorkOrderCloseRequest {
  allowUnderTargetClose?: boolean
}

export interface ProductionExecutionCreateRequest {
  orderKey: string
  routingId: number
  goodQty: number
  defectQty: number
  defectType?: string
  defectReason?: string
  reworkable?: boolean
  receiptLocationCode?: string
  manHoursMinutes: number
}

function cleanParams(params: WorkOrderSearchParams) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

export const workOrderApi = {
  async getWorkOrders(params: WorkOrderSearchParams = {}) {
    const response = await apiClient.get<ApiResponse<PageResponse<WorkOrderResponse>>>('/api/work-orders', {
      params: cleanParams({
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'planDate,desc',
        status: params.status,
        planDate: params.planDate,
        fromDate: params.fromDate,
        toDate: params.toDate,
        keyword: params.keyword,
        itemKeyword: params.itemKeyword,
        factoryName: params.factoryName,
        lineName: params.lineName,
        operationName: params.operationName
      })
    })
    return response.data
  },

  async getWorkOrder(orderKey: string | number) {
    const response = await apiClient.get<ApiResponse<WorkOrderDetailResponse>>(`/api/work-orders/${orderKey}`)
    return response.data
  },

  async createWorkOrder(request: WorkOrderRequest) {
    const response = await apiClient.post<ApiResponse<WorkOrderResponse>>('/api/work-orders', request)
    return response.data
  },

  async updateWorkOrder(orderKey: string | number, request: WorkOrderRequest) {
    const response = await apiClient.put<ApiResponse<WorkOrderResponse>>(`/api/work-orders/${orderKey}`, request)
    return response.data
  },

  async deleteWorkOrder(orderKey: string | number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/work-orders/${orderKey}`)
    return response.data
  },

  async updateStatus(orderKey: string | number, request: WorkOrderStatusUpdateRequest) {
    const response = await apiClient.patch<ApiResponse<WorkOrderResponse>>(`/api/work-orders/${orderKey}/status`, request)
    return response.data
  },

  async closeWorkOrder(orderKey: string | number, request: WorkOrderCloseRequest = {}) {
    const response = await apiClient.put<ApiResponse<WorkOrderResponse>>(`/api/work-orders/${orderKey}/close`, request)
    return response.data
  },

  async issueMaterials(orderKey: string | number) {
    const response = await apiClient.post<ApiResponse<void>>(`/api/work-orders/${orderKey}/issue-materials`, null)
    return response.data
  },

  async cancelIssueMaterials(orderKey: string | number) {
    const response = await apiClient.post<ApiResponse<void>>(`/api/work-orders/${orderKey}/issue-materials/cancel`, null)
    return response.data
  },

  async getExecutions(orderKey: string | number) {
    const response = await apiClient.get<ApiResponse<ProductionExecutionResponse[]>>('/api/production-executions', {
      params: { orderKey }
    })
    return response.data
  },

  async createExecution(request: ProductionExecutionCreateRequest) {
    const response = await apiClient.post<ApiResponse<ProductionExecutionResponse>>('/api/production-executions', request)
    return response.data
  },

  async deleteExecution(executionId: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/production-executions/${executionId}`)
    return response.data
  }
}
