import axios from 'axios'
import type { ApiResponse } from '@/api/authApi'

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
  executedQty: number
  workerId: number | null
  workerEmployeeNo: string | null
  workerName: string | null
  manHoursMinutes: number
  createdAt: string
}

export interface WorkOrderDetailResponse {
  workOrder: WorkOrderResponse
  materialRequirements: WorkOrderMaterialRequirementResponse[]
  executions: ProductionExecutionResponse[]
}

export interface WorkOrderRequest {
  itemCode: string
  routingId: number
  targetQty: number
  planDate: string
}

export interface WorkOrderSearchParams {
  status?: WorkOrderStatus | ''
  planDate?: string
  fromDate?: string
  toDate?: string
  keyword?: string
  factoryName?: string
  lineName?: string
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
  manHoursMinutes: number
}

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  return {
    Authorization: `Bearer ${token}`
  }
}

function cleanParams(params: WorkOrderSearchParams) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  )
}

export const workOrderApi = {
  async getWorkOrders(params: WorkOrderSearchParams = {}) {
    const response = await axios.get<ApiResponse<WorkOrderResponse[]>>('/api/work-orders', {
      headers: getAuthHeaders(),
      params: cleanParams(params)
    })
    return response.data
  },

  async getWorkOrder(orderKey: string | number) {
    const response = await axios.get<ApiResponse<WorkOrderDetailResponse>>(`/api/work-orders/${orderKey}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async createWorkOrder(request: WorkOrderRequest) {
    const response = await axios.post<ApiResponse<WorkOrderResponse>>('/api/work-orders', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async updateWorkOrder(orderKey: string | number, request: WorkOrderRequest) {
    const response = await axios.put<ApiResponse<WorkOrderResponse>>(`/api/work-orders/${orderKey}`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async deleteWorkOrder(orderKey: string | number) {
    const response = await axios.delete<ApiResponse<void>>(`/api/work-orders/${orderKey}`, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async updateStatus(orderKey: string | number, request: WorkOrderStatusUpdateRequest) {
    const response = await axios.patch<ApiResponse<WorkOrderResponse>>(`/api/work-orders/${orderKey}/status`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async closeWorkOrder(orderKey: string | number, request: WorkOrderCloseRequest = {}) {
    const response = await axios.put<ApiResponse<WorkOrderResponse>>(`/api/work-orders/${orderKey}/close`, request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async issueMaterials(orderKey: string | number) {
    const response = await axios.post<ApiResponse<void>>(`/api/work-orders/${orderKey}/issue-materials`, null, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async getExecutions(orderKey: string | number) {
    const response = await axios.get<ApiResponse<ProductionExecutionResponse[]>>('/api/production-executions', {
      headers: getAuthHeaders(),
      params: { orderKey }
    })
    return response.data
  },

  async createExecution(request: ProductionExecutionCreateRequest) {
    const response = await axios.post<ApiResponse<ProductionExecutionResponse>>('/api/production-executions', request, {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async deleteExecution(executionId: number) {
    const response = await axios.delete<ApiResponse<void>>(`/api/production-executions/${executionId}`, {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
