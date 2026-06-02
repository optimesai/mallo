import { defineStore } from 'pinia'
import { ref } from 'vue'
import { workOrderService } from '@/services/workOrderService'
import type { MockWorkOrder, MockBomStructure } from '@/api/workOrderApi'

export const useWorkOrderStore = defineStore('workOrder', () => {
  const workOrders = ref<MockWorkOrder[]>([])
  const bomRequirements = ref<MockBomStructure[]>([])
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  async function loadWorkOrders() {
    isLoading.value = true
    error.value = null
    try {
      workOrders.value = await workOrderService.getWorkOrders()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  // Load material requirements for a parent item asynchronously
  async function loadBOMRequirements(itemCode: string) {
    isLoading.value = true
    error.value = null
    try {
      bomRequirements.value = await workOrderService.getMaterialsRequirements(itemCode)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 구조를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  // Call actual issue-materials POST API
  async function issueMaterials(orderId: number) {
    isLoading.value = true
    error.value = null
    try {
      await workOrderService.issueMaterials(orderId)
      // On success, update the status of the local work order to RUN
      const index = workOrders.value.findIndex(wo => wo.orderId === orderId)
      if (index !== -1) {
        workOrders.value[index].status = 'RUN'
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '자재 출고 처리에 실패했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  return {
    workOrders,
    bomRequirements,
    isLoading,
    error,
    loadWorkOrders,
    loadBOMRequirements,
    issueMaterials
  }
})
