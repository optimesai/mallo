import { AxiosError } from 'axios'
import { dashboardApi } from '@/api/dashboardApi'
import type { DashboardPeriod, DashboardSnapshot } from '@/api/dashboardApi'

export const dashboardService = {
  async getSnapshot(period: DashboardPeriod): Promise<DashboardSnapshot> {
    try {
      const response = await dashboardApi.getSnapshot(period)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '대시보드 데이터를 불러오지 못했습니다.')
      }
      throw new Error('대시보드 데이터를 불러오지 못했습니다.')
    }
  }
}
