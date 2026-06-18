import { defineStore } from 'pinia'
import { ref } from 'vue'
import { factoryRoutingService } from '@/services/factoryRoutingService'
import type {
  FactoryRoutingRequest,
  FactoryRoutingResponse,
  FactoryRoutingSearchParams,
  FactoryRoutingStatusUpdateRequest,
  FactoryRoutingTreeResponse,
  FactoryRoutingUsageResponse
} from '@/api/factoryRoutingApi'

export const useFactoryRoutingStore = defineStore('factoryRouting', () => {
  const routings = ref<FactoryRoutingResponse[]>([])
  const routingTree = ref<FactoryRoutingTreeResponse[]>([])
  const factories = ref<string[]>([])
  const lines = ref<string[]>([])
  const selectedRouting = ref<FactoryRoutingResponse | null>(null)
  const selectedUsage = ref<FactoryRoutingUsageResponse | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)

  async function loadRoutings(params: FactoryRoutingSearchParams = {}) {
    isLoading.value = true
    error.value = null
    try {
      routings.value = await factoryRoutingService.getRoutings(params)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadRouting(id: number) {
    isLoading.value = true
    error.value = null
    try {
      selectedRouting.value = await factoryRoutingService.getRouting(id)
      return selectedRouting.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 상세 정보를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadRoutingTree() {
    error.value = null
    try {
      routingTree.value = await factoryRoutingService.getRoutingTree()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 트리를 불러오지 못했습니다.'
      throw err
    }
  }

  async function loadFactories() {
    error.value = null
    try {
      factories.value = await factoryRoutingService.getFactories()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '공장 목록을 불러오지 못했습니다.'
      throw err
    }
  }

  async function loadLines(factoryName: string) {
    error.value = null
    if (!factoryName) {
      lines.value = []
      return
    }
    try {
      lines.value = await factoryRoutingService.getLines(factoryName)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라인 목록을 불러오지 못했습니다.'
      throw err
    }
  }

  async function createRouting(request: FactoryRoutingRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await factoryRoutingService.createRouting(request)
      routings.value = [created, ...routings.value]
      selectedRouting.value = created
      await refreshReferences()
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateRouting(id: number, request: FactoryRoutingRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await factoryRoutingService.updateRouting(id, request)
      const index = routings.value.findIndex((routing) => routing.routingId === id)
      if (index !== -1) {
        routings.value[index] = updated
      }
      selectedRouting.value = updated
      await refreshReferences()
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 수정에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function deleteRouting(id: number) {
    isSaving.value = true
    error.value = null
    try {
      await factoryRoutingService.deleteRouting(id)
      routings.value = routings.value.filter((routing) => routing.routingId !== id)
      if (selectedRouting.value?.routingId === id) {
        selectedRouting.value = null
        selectedUsage.value = null
      }
      await refreshReferences()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 삭제에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateRoutingStatus(id: number, request: FactoryRoutingStatusUpdateRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await factoryRoutingService.updateRoutingStatus(id, request)
      const index = routings.value.findIndex((routing) => routing.routingId === id)
      if (index !== -1) {
        routings.value[index] = updated
      }
      selectedRouting.value = updated
      await refreshReferences()
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 상태 변경에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function loadRoutingUsage(id: number) {
    error.value = null
    try {
      selectedUsage.value = await factoryRoutingService.getRoutingUsage(id)
      return selectedUsage.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '라우팅 참조 현황을 불러오지 못했습니다.'
      throw err
    }
  }

  async function refreshReferences() {
    await Promise.all([
      loadFactories(),
      loadRoutingTree()
    ])
  }

  function selectRouting(routing: FactoryRoutingResponse | null) {
    selectedRouting.value = routing
    selectedUsage.value = null
  }

  function clearLines() {
    lines.value = []
  }

  return {
    routings,
    routingTree,
    factories,
    lines,
    selectedRouting,
    selectedUsage,
    isLoading,
    isSaving,
    error,
    loadRoutings,
    loadRouting,
    loadRoutingTree,
    loadFactories,
    loadLines,
    createRouting,
    updateRouting,
    deleteRouting,
    updateRoutingStatus,
    loadRoutingUsage,
    selectRouting,
    clearLines
  }
})
