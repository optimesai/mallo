import { defineStore } from 'pinia'
import { ref } from 'vue'
import { itemMasterService } from '@/services/itemMasterService'
import type {
  ItemMasterRequest,
  ItemMasterResponse,
  ItemMasterSearchParams
} from '@/api/itemMasterApi'

export const useItemMasterStore = defineStore('itemMaster', () => {
  const items = ref<ItemMasterResponse[]>([])
  const selectedItem = ref<ItemMasterResponse | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)

  async function loadItems(params: ItemMasterSearchParams = {}) {
    isLoading.value = true
    error.value = null
    try {
      items.value = await itemMasterService.getItems(params)
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

  async function createItem(request: ItemMasterRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await itemMasterService.createItem(request)
      items.value = [created, ...items.value]
      selectedItem.value = created
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateItem(id: number, request: ItemMasterRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await itemMasterService.updateItem(id, request)
      const index = items.value.findIndex((item) => item.itemId === id)
      if (index !== -1) {
        items.value[index] = updated
      }
      selectedItem.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '품목 수정에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function deleteItem(id: number, force = false) {
    isSaving.value = true
    error.value = null
    try {
      await itemMasterService.deleteItem(id, force)
      items.value = items.value.filter((item) => item.itemId !== id)
      if (selectedItem.value?.itemId === id) {
        selectedItem.value = null
      }
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
    isLoading,
    isSaving,
    error,
    loadItems,
    loadItem,
    createItem,
    updateItem,
    deleteItem,
    selectItem
  }
})
