import { defineStore } from 'pinia'
import { ref } from 'vue'
import { bomMasterService } from '@/services/bomMasterService'
import type { ItemMasterResponse, ItemType } from '@/api/itemMasterApi'
import type {
  BomBulkRequest,
  BomGroupResponse,
  BomGroupSearchParams,
  BomMasterRequest,
  BomMasterResponse,
  BomMasterSearchParams,
  BomReverseResponse,
  BomTreeNode
} from '@/api/bomMasterApi'
import type { PageResponse } from '@/api/types'

export const useBomMasterStore = defineStore('bomMaster', () => {
  const boms = ref<BomMasterResponse[]>([])
  const bomGroups = ref<PageResponse<BomGroupResponse>>({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    sort: 'UNSORTED'
  })
  const bomGroupStats = ref<BomGroupResponse[]>([])
  const parentItems = ref<ItemMasterResponse[]>([])
  const childItems = ref<ItemMasterResponse[]>([])
  const parentVersions = ref<string[]>([])
  const childVersions = ref<string[]>([])
  const parentTree = ref<BomTreeNode[]>([])
  const childParentTree = ref<BomTreeNode[]>([])
  const childParents = ref<BomReverseResponse[]>([])
  const selectedBom = ref<BomMasterResponse | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)

  async function loadBomGroups(params: BomGroupSearchParams = {}) {
    isLoading.value = true
    error.value = null
    try {
      bomGroups.value = await bomMasterService.getBomGroups({
        page: 0,
        size: 10,
        ...params
      })
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadBomGroupStats(params: BomGroupSearchParams = {}) {
    const firstPage = await bomMasterService.getBomGroups({
      ...params,
      page: 0,
      size: 1
    })
    if (firstPage.totalElements <= firstPage.content.length) {
      bomGroupStats.value = firstPage.content
      return
    }

    const allGroups = await bomMasterService.getBomGroups({
      ...params,
      page: 0,
      size: firstPage.totalElements
    })
    bomGroupStats.value = allGroups.content
  }

  async function loadBomGroup(parentItemId: number, bomVersion: string) {
    isLoading.value = true
    error.value = null
    try {
      boms.value = await bomMasterService.getBomGroup(parentItemId, bomVersion)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 상세를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadBoms(params: BomMasterSearchParams = {}) {
    isLoading.value = true
    error.value = null
    try {
      boms.value = await bomMasterService.getBoms(params)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadParentItems(keyword?: string) {
    const [halfItems, fgItems] = await Promise.all([
      bomMasterService.getItems('HALF', keyword),
      bomMasterService.getItems('FG', keyword)
    ])
    parentItems.value = [...halfItems, ...fgItems].sort((a, b) => a.itemCode.localeCompare(b.itemCode))
  }

  async function loadChildItems(keyword?: string) {
    const [rawItems, halfItems] = await Promise.all([
      bomMasterService.getItems('RAW', keyword),
      bomMasterService.getItems('HALF', keyword)
    ])
    childItems.value = [...rawItems, ...halfItems].sort((a, b) => a.itemCode.localeCompare(b.itemCode))
  }

  async function createBom(request: BomMasterRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await bomMasterService.createBom(request)
      boms.value = [created, ...boms.value]
      selectedBom.value = created
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function createBoms(request: BomBulkRequest) {
    isSaving.value = true
    error.value = null
    try {
      const created = await bomMasterService.createBoms(request)
      boms.value = [...created, ...boms.value]
      selectedBom.value = created[0] || null
      return created
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 일괄 등록에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateBom(bomId: number, request: BomMasterRequest) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await bomMasterService.updateBom(bomId, request)
      const index = boms.value.findIndex((bom) => bom.bomId === bomId)
      if (index !== -1) boms.value[index] = updated
      selectedBom.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 수정에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function deleteBom(bomId: number) {
    isSaving.value = true
    error.value = null
    try {
      await bomMasterService.deleteBom(bomId)
      boms.value = boms.value.filter((bom) => bom.bomId !== bomId)
      if (selectedBom.value?.bomId === bomId) selectedBom.value = null
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 삭제에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function updateBomStatus(bomId: number, bomStatus: 'ACTIVE' | 'INACTIVE') {
    isSaving.value = true
    error.value = null
    try {
      const updated = await bomMasterService.updateBomStatus(bomId, bomStatus)
      const index = boms.value.findIndex((bom) => bom.bomId === bomId)
      if (index !== -1) boms.value[index] = updated
      selectedBom.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 상태 변경에 실패했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function loadParentVersions(keyword: string) {
    parentVersions.value = await bomMasterService.getParentVersions(keyword)
    return parentVersions.value
  }

  async function loadChildVersions(keyword: string) {
    childVersions.value = await bomMasterService.getChildVersions(keyword)
    return childVersions.value
  }

  async function loadParentTree(keyword: string, bomVersion?: string) {
    isLoading.value = true
    error.value = null
    try {
      parentTree.value = await bomMasterService.getParentTree(keyword, bomVersion)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '하향 BOM 트리를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadChildParentTree(keyword: string, bomVersion?: string) {
    isLoading.value = true
    error.value = null
    try {
      childParentTree.value = await bomMasterService.getChildParentTree(keyword, bomVersion)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '상향 BOM 트리를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadChildParents(keyword: string, bomVersion?: string) {
    isLoading.value = true
    error.value = null
    try {
      childParents.value = await bomMasterService.getChildParents(keyword, bomVersion)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'BOM 역조회 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  function selectBom(bom: BomMasterResponse | null) {
    selectedBom.value = bom
  }

  function clearTrees() {
    parentTree.value = []
    childParentTree.value = []
    childParents.value = []
  }

  function clearParentSearchState() {
    parentVersions.value = []
    parentTree.value = []
  }

  function clearChildSearchState() {
    childVersions.value = []
    childParentTree.value = []
    childParents.value = []
  }

  return {
    boms,
    bomGroups,
    bomGroupStats,
    parentItems,
    childItems,
    parentVersions,
    childVersions,
    parentTree,
    childParentTree,
    childParents,
    selectedBom,
    isLoading,
    isSaving,
    error,
    loadBomGroups,
    loadBomGroupStats,
    loadBomGroup,
    loadBoms,
    loadParentItems,
    loadChildItems,
    createBom,
    createBoms,
    updateBom,
    deleteBom,
    updateBomStatus,
    loadParentVersions,
    loadChildVersions,
    loadParentTree,
    loadChildParentTree,
    loadChildParents,
    selectBom,
    clearTrees,
    clearParentSearchState,
    clearChildSearchState
  }
})
