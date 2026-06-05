import { defineStore } from 'pinia'
import { ref } from 'vue'
import { workOrderService } from '@/services/workOrderService'
import type {
  ProductionExecutionCreateRequest,
  WorkOrderCloseRequest,
  WorkOrderDetailResponse,
  WorkOrderMaterialRequirementResponse,
  WorkOrderRequest,
  WorkOrderResponse,
  WorkOrderSearchParams,
  WorkOrderStatusUpdateRequest
} from '@/api/workOrderApi'

export const useWorkOrderStore = defineStore('workOrder', () => {
  const workOrders = ref<WorkOrderResponse[]>([])
  const selectedDetail = ref<WorkOrderDetailResponse | null>(null)
  const bomRequirements = ref<WorkOrderMaterialRequirementResponse[]>([])
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)

  function upsertWorkOrder(order: WorkOrderResponse) {
    const index = workOrders.value.findIndex((item) => item.orderId === order.orderId)
    if (index === -1) {
      workOrders.value = [order, ...workOrders.value]
    } else {
      workOrders.value[index] = order
    }
    if (selectedDetail.value?.workOrder.orderId === order.orderId) {
      selectedDetail.value = {
        ...selectedDetail.value,
        workOrder: order
      }
    }
  }

  async function loadWorkOrders(params: WorkOrderSearchParams = {}) {
    isLoading.value = true
    error.value = null
    try {
      workOrders.value = await workOrderService.getWorkOrders(params)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadWorkOrder(orderKey: string | number) {
    isLoading.value = true
    error.value = null
    try {
      selectedDetail.value = await workOrderService.getWorkOrder(orderKey)
      bomRequirements.value = selectedDetail.value.materialRequirements
      upsertWorkOrder(selectedDetail.value.workOrder)
      return selectedDetail.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 상세 정보를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadBOMRequirements(orderKey: string | number) {
    const detail = await loadWorkOrder(orderKey)
    return detail.materialRequirements
  }

  async function createWorkOrder(request: WorkOrderRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await workOrderService.createWorkOrder(request)
      upsertWorkOrder(created)
      await loadWorkOrder(created.orderNo)
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateWorkOrder(orderKey: string | number, request: WorkOrderRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await workOrderService.updateWorkOrder(orderKey, request)
      upsertWorkOrder(updated)
      await loadWorkOrder(updated.orderNo)
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 수정에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function deleteWorkOrder(orderKey: string | number) {
    isSaving.value = true
    error.value = null
    try {
      await workOrderService.deleteWorkOrder(orderKey)
      workOrders.value = workOrders.value.filter((order) => order.orderId !== Number(orderKey) && order.orderNo !== String(orderKey))
      if (selectedDetail.value?.workOrder.orderId === Number(orderKey) || selectedDetail.value?.workOrder.orderNo === String(orderKey)) {
        selectedDetail.value = null
        bomRequirements.value = []
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 삭제에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateStatus(orderKey: string | number, request: WorkOrderStatusUpdateRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await workOrderService.updateStatus(orderKey, request)
      upsertWorkOrder(updated)
      await loadWorkOrder(updated.orderNo)
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 상태 변경에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function closeWorkOrder(orderKey: string | number, request: WorkOrderCloseRequest = {}) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await workOrderService.closeWorkOrder(orderKey, request)
      upsertWorkOrder(updated)
      await loadWorkOrder(updated.orderNo)
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '작업 지시 마감에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function issueMaterials(orderKey: string | number) {
    isSaving.value = true
    error.value = null
    try {
      await workOrderService.issueMaterials(orderKey)
      await loadWorkOrder(orderKey)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '자재 출고 처리에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function createExecution(request: ProductionExecutionCreateRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await workOrderService.createExecution(request)
      await loadWorkOrder(request.orderKey)
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : '생산 실적 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function deleteExecution(executionId: number) {
    isSaving.value = true
    error.value = null
    try {
      await workOrderService.deleteExecution(executionId)
      if (selectedDetail.value) {
        await loadWorkOrder(selectedDetail.value.workOrder.orderNo)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '생산 실적 삭제에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  function clearSelection() {
    selectedDetail.value = null
    bomRequirements.value = []
  }

  return {
    workOrders,
    selectedDetail,
    bomRequirements,
    isLoading,
    isSaving,
    error,
    loadWorkOrders,
    loadWorkOrder,
    loadBOMRequirements,
    createWorkOrder,
    updateWorkOrder,
    deleteWorkOrder,
    updateStatus,
    closeWorkOrder,
    issueMaterials,
    createExecution,
    deleteExecution,
    clearSelection
  }
})
