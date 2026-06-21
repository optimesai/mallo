import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'

export interface AiQueryRequest {
  question: string
}

export type AiExecutionStatus =
  | 'SUCCESS'
  | 'NOT_DATA_QUESTION'
  | 'SQL_GENERATION_FAILED'
  | 'BLOCKED_UNSAFE_SQL'
  | 'SQL_EXECUTION_FAILED'
  | 'ANSWER_GENERATION_FAILED'
  | 'SCHEMA_LOAD_FAILED'
  | 'CLARIFICATION_REQUIRED'
  | 'SEMANTIC_VALIDATION_FAILED'
  | 'TIMEOUT'

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
