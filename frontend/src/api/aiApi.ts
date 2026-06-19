import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'

export interface AiQueryRequest {
  question: string
}

export type AiExecutionStatus = 'SUCCESS' | 'FAILED' | 'CLARIFICATION_REQUIRED'

export type AiChartType = 'NONE' | 'STAT' | 'BAR' | 'LINE' | 'DONUT'

export interface AiChartResponse {
  enabled: boolean
  type: AiChartType
  xKey?: string
  yKeys: string[]
  title?: string
  reason?: string
}

export interface AiQueryResponse {
  queryId: number
  question: string
  generatedSql: string
  rows: Record<string, unknown>[]
  rowCount: number
  answer: string
  executionStatus: AiExecutionStatus
  chart: AiChartResponse
  clarificationRequired: boolean
  clarificationQuestion?: string
}

export const aiApi = {
  async ask(request: AiQueryRequest) {
    const response = await apiClient.post<ApiResponse<AiQueryResponse>>('/api/ai/queries', request)
    return response.data
  }
}
