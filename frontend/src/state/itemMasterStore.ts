import { defineStore } from 'pinia'
import { ref } from 'vue'
import { itemMasterService } from '@/services/itemMasterService'
import type {
  ItemDuplicateCheckParams,
  ItemDuplicateCheckResponse,
  ItemMasterRequest,
  ItemMasterResponse,
  ItemMasterSearchParams,
  ItemMasterUpdateRequest,
  ItemReferenceResponse,
  ItemStatus,
  ItemUsageResponse
} from '@/api/itemMasterApi'

export const useItemMasterStore = defineStore('itemMaster', () => {
  const items = ref<ItemMasterResponse[]>([])
  const selectedItem = ref<ItemMasterResponse | null>(null)
  const itemReferences = ref<ItemReferenceResponse | null>(null)
  const itemUsages = ref<ItemUsageResponse | null>(null)
  const duplicateCheck = ref<ItemDuplicateCheckResponse | null>(null)
  const suggestions = ref<ItemMasterResponse[]>([])
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)
  const page = ref(0)
  const size = ref(10)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const sort = ref('createdAt,desc')
  const lastSearchParams = ref<ItemMasterSearchParams>({})

  async function loadItems(params: ItemMasterSearchParams = {}) {
    isLoading.value = true
    error.value = null
    const requestParams = {
      ...lastSearchParams.value,
      ...params,
      page: params.page ?? page.value,
      size: params.size ?? size.value,
      sort: params.sort ?? sort.value
    }
    try {
      const response = await itemMasterService.getItems(requestParams)
      items.value = response.content
      page.value = response.page
      size.value = response.size
      totalElements.value = response.totalElements
      totalPages.value = response.totalPages
      sort.value = requestParams.sort ?? sort.value
      lastSearchParams.value = requestParams
      return response
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadItem(id: number) {
    isLoading.value = true
    error.value = null
    try {
      selectedItem.value = await itemMasterService.getItem(id)
      return selectedItem.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 상세 정보를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadItemReferences(id: number) {
    itemReferences.value = await itemMasterService.getItemReferences(id)
    return itemReferences.value
  }

  async function loadItemUsages(id: number) {
    itemUsages.value = await itemMasterService.getItemUsages(id)
    return itemUsages.value
  }

  async function checkDuplicates(params: ItemDuplicateCheckParams) {
    duplicateCheck.value = await itemMasterService.checkDuplicates(params)
    return duplicateCheck.value
  }

  async function loadSuggestions(keyword: string) {
    if (!keyword.trim()) {
      suggestions.value = []
      return suggestions.value
    }
    const response = await itemMasterService.getItems({
      keyword: keyword.trim(),
      page: 0,
      size: 8,
      sort: 'itemCode,asc'
    })
    suggestions.value = response.content
    return suggestions.value
  }

  async function createItem(request: ItemMasterRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await itemMasterService.createItem(request)
      selectedItem.value = created
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateItem(id: number, request: ItemMasterUpdateRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await itemMasterService.updateItem(id, request)
      const index = items.value.findIndex((item) => item.itemId === id)
      if (index !== -1) items.value[index] = updated
      selectedItem.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 수정에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateItemStatus(id: number, itemStatus: ItemStatus) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await itemMasterService.updateItemStatus(id, itemStatus)
      const index = items.value.findIndex((item) => item.itemId === id)
      if (index !== -1) items.value[index] = updated
      selectedItem.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 상태 변경에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function deleteItem(id: number) {
    isSaving.value = true
    error.value = null
    try {
      await itemMasterService.deleteItem(id)
      items.value = items.value.filter((item) => item.itemId !== id)
      if (selectedItem.value?.itemId === id) selectedItem.value = null
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 삭제에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  function selectItem(item: ItemMasterResponse | null) {
    selectedItem.value = item
  }

  return {
    items,
    selectedItem,
    itemReferences,
    itemUsages,
    duplicateCheck,
    suggestions,
    isLoading,
    isSaving,
    error,
    page,
    size,
    totalElements,
    totalPages,
    sort,
    lastSearchParams,
    loadItems,
    loadItem,
    loadItemReferences,
    loadItemUsages,
    checkDuplicates,
    loadSuggestions,
    createItem,
    updateItem,
    updateItemStatus,
    deleteItem,
    selectItem
  }
})
