import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/authApi'
import type { AiChartResponse } from '@/api/aiApi'

export type DashboardPeriod = 'today' | '7d' | '30d'
export type DashboardMetricCategory = 'production' | 'quality' | 'inventory' | 'shipping'
export type DashboardSeverity = 'success' | 'warning' | 'danger' | 'neutral'

export interface DashboardSummaryCard {
  id: string
  label: string
  value: string
  caption: string
  severity: DashboardSeverity
}

export interface DashboardMetricView {
  id: DashboardMetricCategory
  label: string
  title: string
  subtitle: string
  chart: AiChartResponse
  rows: Record<string, unknown>[]
}

export interface DashboardInsight {
  id: string
  title: string
  description: string
  severity: DashboardSeverity
  source: string
}

export interface DashboardSnapshot {
  generatedAt: string
  period: DashboardPeriod
  summaryCards: DashboardSummaryCard[]
  metricViews: DashboardMetricView[]
  insights: DashboardInsight[]
}

export const dashboardApi = {
  async getSnapshot(period: DashboardPeriod) {
    const response = await apiClient.get<ApiResponse<DashboardSnapshot>>('/api/dashboard/summary', {
      params: { period }
    })
    return response.data
  }
}
