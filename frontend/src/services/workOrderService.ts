import { AxiosError } from 'axios'
import { workOrderApi } from '@/api/workOrderApi'
import type { MockWorkOrder, MockBomStructure } from '@/api/workOrderApi'

export const workOrderService = {
  async getWorkOrders(): Promise<MockWorkOrder[]> {
    try {
      return await workOrderApi.getWorkOrders()
    } catch (error) {
      throw new Error('작업 지시 목록을 불러오는데 실패했습니다.')
    }
  },

  async getMaterialsRequirements(itemCode: string): Promise<MockBomStructure[]> {
    try {
      return await workOrderApi.getMaterialsRequirements(itemCode)
    } catch (error) {
      throw new Error('BOM 구조를 불러오는데 실패했습니다.')
    }
  },

  async issueMaterials(orderId: number): Promise<void> {
    try {
      await workOrderApi.issueMaterials(orderId)
    } catch (error) {
      if (error instanceof AxiosError) {
        const errorCode = error.response?.data?.errorCode
        const message = error.response?.data?.message
        if (errorCode === 'INSUFFICIENT_STOCK') {
          throw new Error('INSUFFICIENT_STOCK: 자재 창고의 가용 재고가 부족하여 출고 처리가 중단되었습니다.')
        }
        throw new Error(message || '자재 출고 처리에 실패했습니다.')
      }
      throw new Error('자재 출고 처리에 실패했습니다.')
    }
  }
}
