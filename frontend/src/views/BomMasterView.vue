<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Boxes,
  ChevronDown,
  GitBranch,
  Layers3,
  Loader2,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X
} from '@lucide/vue'
import { useAuthStore } from '@/state/authStore'
import { useBomMasterStore } from '@/state/bomMasterStore'
import BomTreeList from '@/ui/BomTreeList.vue'
import type { BomGroupResponse, BomMasterRequest, BomMasterResponse, BomTreeNode } from '@/api/bomMasterApi'
import type { ItemMasterResponse } from '@/api/itemMasterApi'

const bomStore = useBomMasterStore()
const authStore = useAuthStore()
const router = useRouter()

const pageError = ref<string | null>(null)
const successToast = ref<string | null>(null)
const isSearchExpanded = ref(true)
const activeTab = ref<'list' | 'downward' | 'reverse'>('list')

const filterParent = ref('')
const filterChild = ref('')
const filterVersion = ref('')
const page = ref(0)
const isFilterParentPickerOpen = ref(false)
const isFilterChildPickerOpen = ref(false)

const isFormOpen = ref(false)
const editingBomId = ref<number | null>(null)
const formError = ref<string | null>(null)
const form = ref({
  parentItemId: null as number | null,
  childItemId: null as number | null,
  quantity: 1,
  bomVersion: 'v1.0'
})
const formLines = ref<Array<{
  childItemId: number
  childItemCode: string
  childItemName: string
  childItemType: string
  quantity: number
}>>([])
const formParentKeyword = ref('')
const formChildKeyword = ref('')
const isFormParentPickerOpen = ref(false)
const isFormChildPickerOpen = ref(false)

const downwardKeyword = ref('')
const downwardVersion = ref('')
const downwardBaseQuantity = ref(1)
const selectedDownwardItem = ref<ItemMasterResponse | null>(null)
const isDownwardPickerOpen = ref(false)
const hasSearchedDownward = ref(false)
const reverseKeyword = ref('')
const reverseVersion = ref('')
const selectedReverseItem = ref<ItemMasterResponse | null>(null)
const isReversePickerOpen = ref(false)
const hasSearchedReverse = ref(false)

const canWrite = computed(() => ['ADMIN', 'MANAGER'].includes(authStore.user?.role || ''))
const canDelete = computed(() => authStore.user?.role === 'ADMIN')
const pageStart = computed(() => {
  if (bomStore.bomGroups.totalElements === 0) return 0
  return bomStore.bomGroups.page * bomStore.bomGroups.size + 1
})
const pageEnd = computed(() => {
  return Math.min((bomStore.bomGroups.page + 1) * bomStore.bomGroups.size, bomStore.bomGroups.totalElements)
})

interface BomParentGroup extends BomGroupResponse {
  key: string
}

const bomParentGroups = computed<BomParentGroup[]>(() => {
  return bomStore.bomGroups.content.map((group) => ({
    ...group,
    key: `${group.parentItemId}:${group.bomVersion}`
  }))
})

const stats = computed(() => {
  const groups = bomStore.bomGroupStats
  const parents = new Set(groups.map((bom) => bom.parentItemId)).size
  const versions = new Set(groups.map((bom) => bom.bomVersion)).size
  const children = groups.reduce((sum, group) => sum + group.childCount, 0)
  return {
    total: bomStore.bomGroups.totalElements,
    versions,
    parents,
    children
  }
})

const selectedParent = computed(() => {
  return bomStore.parentItems.find((item) => item.itemId === form.value.parentItemId) || null
})

const selectedChild = computed(() => {
  return bomStore.childItems.find((item) => item.itemId === form.value.childItemId) || null
})

const filteredDownwardItems = computed(() => {
  return filterItemCandidates(bomStore.parentItems, downwardKeyword.value)
})

const filteredParentFilterItems = computed(() => {
  return filterItemCandidates(bomStore.parentItems, filterParent.value)
})

const filteredChildFilterItems = computed(() => {
  return filterItemCandidates(bomStore.childItems, filterChild.value)
})

const filteredReverseItems = computed(() => {
  return filterItemCandidates(bomStore.childItems, reverseKeyword.value)
})

const filteredFormParentItems = computed(() => {
  return filterItemCandidates(bomStore.parentItems, formParentKeyword.value)
})

const filteredFormChildItems = computed(() => {
  return filterItemCandidates(bomStore.childItems, formChildKeyword.value)
})

const displayedDownwardTree = computed(() => {
  if (!hasSearchedDownward.value) return []
  if (bomStore.parentTree.length > 0) return bomStore.parentTree
  if (!selectedDownwardItem.value) return []

  return [buildLocalDownwardTree(selectedDownwardItem.value, new Set<number>())]
})

onMounted(async () => {
  bomStore.clearTrees()
  await fetchPageData()
})

async function fetchPageData() {
  try {
    pageError.value = null
    closeFilterPickers()
    const searchParams = {
      parentKeyword: filterParent.value || undefined,
      childKeyword: filterChild.value || undefined,
      bomVersion: filterVersion.value || undefined
    }
    await Promise.all([
      bomStore.loadBomGroups({
        page: page.value,
        size: 10,
        ...searchParams
      }),
      bomStore.loadBomGroupStats(searchParams),
      bomStore.loadParentItems(),
      bomStore.loadChildItems()
    ])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 데이터를 불러오지 못했습니다.'
  }
}

function resetFilters() {
  filterParent.value = ''
  filterChild.value = ''
  filterVersion.value = ''
  page.value = 0
  closeFilterPickers()
}

async function selectGroup(group: BomParentGroup) {
  await router.push({
    name: 'bom-master-detail',
    params: { parentItemId: group.parentItemId },
    query: { bomVersion: group.bomVersion }
  })
}

async function changePage(nextPage: number) {
  if (nextPage < 0 || nextPage >= bomStore.bomGroups.totalPages) return
  page.value = nextPage
  await fetchPageData()
}

function filterItemCandidates(items: ItemMasterResponse[], keyword: string) {
  const normalizedKeyword = keyword.trim().toLowerCase()
  if (!normalizedKeyword) return []

  const candidates = items.filter((item) => {
    return item.itemCode.toLowerCase().includes(normalizedKeyword)
      || item.itemName.toLowerCase().includes(normalizedKeyword)
      || String(item.itemId) === normalizedKeyword
  })

  return candidates.slice(0, 8)
}

async function refreshParentCandidates(keyword: string) {
  try {
    await bomStore.loadParentItems(keyword.trim() || undefined)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '상위 품목 목록을 불러오지 못했습니다.'
  }
}

async function refreshChildCandidates(keyword: string) {
  try {
    await bomStore.loadChildItems(keyword.trim() || undefined)
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '구성 품목 목록을 불러오지 못했습니다.'
  }
}

function selectParentFilterItem(item: ItemMasterResponse) {
  filterParent.value = item.itemCode
  isFilterParentPickerOpen.value = false
}

function selectChildFilterItem(item: ItemMasterResponse) {
  filterChild.value = item.itemCode
  isFilterChildPickerOpen.value = false
}

async function handleFilterParentInput() {
  isFilterParentPickerOpen.value = filterParent.value.trim().length > 0
  if (isFilterParentPickerOpen.value) await refreshParentCandidates(filterParent.value)
}

async function handleFilterChildInput() {
  isFilterChildPickerOpen.value = filterChild.value.trim().length > 0
  if (isFilterChildPickerOpen.value) await refreshChildCandidates(filterChild.value)
}

function closeFilterPickers() {
  isFilterParentPickerOpen.value = false
  isFilterChildPickerOpen.value = false
}

function buildLocalDownwardTree(item: ItemMasterResponse, visitedItemIds: Set<number>): BomTreeNode {
  if (visitedItemIds.has(item.itemId)) {
    return {
      itemId: item.itemId,
      itemCode: item.itemCode,
      itemName: item.itemName,
      itemType: item.itemType,
      unit: item.unit,
      quantity: 1,
      bomVersion: null,
      children: []
    }
  }

  const nextVisitedItemIds = new Set(visitedItemIds)
  nextVisitedItemIds.add(item.itemId)

  const children = bomStore.boms
    .filter((bom) => bom.parentItemId === item.itemId)
    .filter((bom) => !downwardVersion.value || bom.bomVersion === downwardVersion.value)
    .map((bom) => {
      const childItem = bomStore.childItems.find((candidate) => candidate.itemId === bom.childItemId)
      return {
        itemId: bom.childItemId,
        itemCode: bom.childItemCode,
        itemName: bom.childItemName,
        itemType: bom.childItemType,
        unit: bom.childUnit,
        quantity: bom.quantity,
        bomVersion: bom.bomVersion,
        children: childItem ? buildLocalDownwardTree(childItem, nextVisitedItemIds).children : []
      }
    })

  return {
    itemId: item.itemId,
    itemCode: item.itemCode,
    itemName: item.itemName,
    itemType: item.itemType,
    unit: item.unit,
    quantity: 1,
    bomVersion: null,
    children
  }
}

async function selectDownwardItem(item: ItemMasterResponse) {
  selectedDownwardItem.value = item
  downwardKeyword.value = `${item.itemCode} / ${item.itemName}`
  isDownwardPickerOpen.value = false
  downwardVersion.value = ''
  await loadDownwardVersions()
}

function clearDownwardItem() {
  selectedDownwardItem.value = null
  downwardKeyword.value = ''
  downwardVersion.value = ''
  downwardBaseQuantity.value = 1
  hasSearchedDownward.value = false
  bomStore.clearParentSearchState()
}

async function selectReverseItem(item: ItemMasterResponse) {
  selectedReverseItem.value = item
  reverseKeyword.value = `${item.itemCode} / ${item.itemName}`
  isReversePickerOpen.value = false
  reverseVersion.value = ''
  await loadReverseVersions()
}

function clearReverseItem() {
  selectedReverseItem.value = null
  reverseKeyword.value = ''
  reverseVersion.value = ''
  hasSearchedReverse.value = false
  bomStore.clearChildSearchState()
}

async function handleDownwardKeywordInput() {
  selectedDownwardItem.value = null
  downwardVersion.value = ''
  hasSearchedDownward.value = false
  bomStore.clearParentSearchState()
  isDownwardPickerOpen.value = downwardKeyword.value.trim().length > 0
  if (isDownwardPickerOpen.value) await refreshParentCandidates(downwardKeyword.value)
}

async function handleReverseKeywordInput() {
  selectedReverseItem.value = null
  reverseVersion.value = ''
  hasSearchedReverse.value = false
  bomStore.clearChildSearchState()
  isReversePickerOpen.value = reverseKeyword.value.trim().length > 0
  if (isReversePickerOpen.value) await refreshChildCandidates(reverseKeyword.value)
}

function selectFormParentItem(item: ItemMasterResponse) {
  form.value.parentItemId = item.itemId
  formParentKeyword.value = `${item.itemCode} / ${item.itemName}`
  isFormParentPickerOpen.value = false
}

function clearFormParentItem() {
  form.value.parentItemId = null
  formParentKeyword.value = ''
  isFormParentPickerOpen.value = false
}

async function handleFormParentKeywordInput() {
  form.value.parentItemId = null
  isFormParentPickerOpen.value = formParentKeyword.value.trim().length > 0
  if (isFormParentPickerOpen.value) await refreshParentCandidates(formParentKeyword.value)
}

function selectFormChildItem(item: ItemMasterResponse) {
  form.value.childItemId = item.itemId
  formChildKeyword.value = `${item.itemCode} / ${item.itemName}`
  isFormChildPickerOpen.value = false
}

function clearFormChildItem() {
  form.value.childItemId = null
  formChildKeyword.value = ''
  isFormChildPickerOpen.value = false
}

async function handleFormChildKeywordInput() {
  form.value.childItemId = null
  isFormChildPickerOpen.value = formChildKeyword.value.trim().length > 0
  if (isFormChildPickerOpen.value) await refreshChildCandidates(formChildKeyword.value)
}

function openCreateForm() {
  if (!canWrite.value) return
  editingBomId.value = null
  form.value = {
    parentItemId: null,
    childItemId: null,
    quantity: 1,
    bomVersion: 'v1.0'
  }
  formLines.value = []
  formParentKeyword.value = ''
  formChildKeyword.value = ''
  isFormParentPickerOpen.value = false
  isFormChildPickerOpen.value = false
  formError.value = null
  isFormOpen.value = true
}

function openEditForm(bom: BomMasterResponse) {
  if (!canWrite.value) return
  editingBomId.value = bom.bomId
  form.value = {
    parentItemId: bom.parentItemId,
    childItemId: bom.childItemId,
    quantity: Number(bom.quantity),
    bomVersion: bom.bomVersion
  }
  formLines.value = []
  formParentKeyword.value = `${bom.parentItemCode} / ${bom.parentItemName}`
  formChildKeyword.value = `${bom.childItemCode} / ${bom.childItemName}`
  isFormParentPickerOpen.value = false
  isFormChildPickerOpen.value = false
  formError.value = null
  isFormOpen.value = true
}

function openAddChildForm(group: BomParentGroup) {
  if (!canWrite.value) return
  const parentItem = bomStore.parentItems.find((item) => item.itemId === group.parentItemId)
  editingBomId.value = null
  form.value = {
    parentItemId: group.parentItemId,
    childItemId: null,
    quantity: 1,
    bomVersion: group.bomVersion
  }
  formLines.value = []
  formParentKeyword.value = parentItem
    ? `${parentItem.itemCode} / ${parentItem.itemName}`
    : `${group.parentItemCode} / ${group.parentItemName}`
  formChildKeyword.value = ''
  isFormParentPickerOpen.value = false
  isFormChildPickerOpen.value = false
  formError.value = null
  isFormOpen.value = true
}

function closeForm() {
  isFormOpen.value = false
  formError.value = null
}

function validateForm() {
  if (!form.value.parentItemId) return '상위 품목을 선택해주세요.'
  if (editingBomId.value) {
    if (!form.value.childItemId) return '구성 품목을 선택해주세요.'
    if (form.value.parentItemId === form.value.childItemId) return '상위 품목과 구성 품목은 같을 수 없습니다.'
    if (!Number.isInteger(form.value.quantity) || form.value.quantity <= 0) return '소요량은 1 이상의 정수여야 합니다.'
  } else if (buildFormLines().length === 0) {
    return '구성 품목을 1개 이상 추가해주세요.'
  }
  if ((form.value.bomVersion || '').length > 20) return 'BOM 버전은 20자 이하여야 합니다.'
  if (selectedParent.value?.itemType === 'RAW') return '상위 품목은 원자재가 될 수 없습니다.'
  if (editingBomId.value && selectedChild.value?.itemType === 'FG') return '구성 품목은 완제품이 될 수 없습니다.'
  return null
}

function buildFormLines() {
  const lines = [...formLines.value]
  if (form.value.childItemId && selectedChild.value) {
    lines.push({
      childItemId: form.value.childItemId,
      childItemCode: selectedChild.value.itemCode,
      childItemName: selectedChild.value.itemName,
      childItemType: selectedChild.value.itemType,
      quantity: normalizeQuantity(form.value.quantity)
    })
  }
  return lines
}

function addFormLine() {
  if (!selectedChild.value || !form.value.childItemId) {
    formError.value = '구성 품목을 선택해주세요.'
    return
  }
  if (form.value.parentItemId === form.value.childItemId) {
    formError.value = '상위 품목과 구성 품목은 같을 수 없습니다.'
    return
  }
  if (selectedChild.value.itemType === 'FG') {
    formError.value = '구성 품목은 완제품이 될 수 없습니다.'
    return
  }
  if (formLines.value.some((line) => line.childItemId === form.value.childItemId)) {
    formError.value = '이미 추가한 구성 품목입니다.'
    return
  }
  formLines.value.push({
    childItemId: form.value.childItemId,
    childItemCode: selectedChild.value.itemCode,
    childItemName: selectedChild.value.itemName,
    childItemType: selectedChild.value.itemType,
    quantity: normalizeQuantity(form.value.quantity)
  })
  clearFormChildItem()
  form.value.quantity = 1
  formError.value = null
}

function removeFormLine(childItemId: number) {
  formLines.value = formLines.value.filter((line) => line.childItemId !== childItemId)
}

function normalizeQuantity(value: number) {
  return Math.max(1, Math.trunc(Number(value) || 1))
}

function formatQuantity(value: number) {
  return normalizeQuantity(value).toLocaleString()
}

async function submitForm() {
  const validationMessage = validateForm()
  if (validationMessage) {
    formError.value = validationMessage
    return
  }

  try {
    formError.value = null
    if (editingBomId.value) {
      const request: BomMasterRequest = {
        parentItemId: form.value.parentItemId as number,
        childItemId: form.value.childItemId as number,
        quantity: normalizeQuantity(form.value.quantity),
        bomVersion: form.value.bomVersion.trim() || undefined
      }
      await bomStore.updateBom(editingBomId.value, request)
      showToast('BOM이 수정되었습니다.')
    } else {
      const lines = buildFormLines()
      const childItemIds = new Set(lines.map((line) => line.childItemId))
      if (childItemIds.size !== lines.length) {
        formError.value = '구성 품목이 중복되었습니다.'
        return
      }
      await bomStore.createBoms({
        parentItemId: form.value.parentItemId as number,
        bomVersion: form.value.bomVersion.trim() || undefined,
        lines: lines.map((line) => ({
          childItemId: line.childItemId,
          quantity: line.quantity
        }))
      })
      showToast('BOM이 일괄 등록되었습니다.')
    }
    closeForm()
    page.value = 0
    await fetchPageData()
  } catch (err) {
    formError.value = err instanceof Error ? err.message : 'BOM 저장에 실패했습니다.'
  }
}

async function deleteBom(bom: BomMasterResponse) {
  if (!canDelete.value) return
  if (!confirm(`${bom.parentItemCode} > ${bom.childItemCode} BOM을 비활성화하시겠습니까?`)) return

  try {
    await bomStore.deleteBom(bom.bomId)
    await fetchPageData()
    showToast('BOM이 비활성화되었습니다.')
  } catch (err) {
    alert(err instanceof Error ? err.message : 'BOM 비활성화에 실패했습니다.')
  }
}

async function toggleBomStatus(bom: BomMasterResponse) {
  if (!canWrite.value) return
  const nextStatus = bom.bomStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await bomStore.updateBomStatus(bom.bomId, nextStatus)
    await fetchPageData()
    showToast(`BOM 상태가 ${nextStatus === 'ACTIVE' ? '활성' : '비활성'}으로 변경되었습니다.`)
  } catch (err) {
    alert(err instanceof Error ? err.message : 'BOM 상태 변경에 실패했습니다.')
  }
}

async function loadDownwardVersions() {
  if (!selectedDownwardItem.value) {
    bomStore.clearParentSearchState()
    downwardVersion.value = ''
    return
  }

  try {
    await bomStore.loadParentVersions(selectedDownwardItem.value.itemCode)
    if (!bomStore.parentVersions.includes(downwardVersion.value)) downwardVersion.value = ''
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 버전 목록을 불러오지 못했습니다.'
  }
}

async function loadReverseVersions() {
  if (!selectedReverseItem.value) {
    bomStore.clearChildSearchState()
    reverseVersion.value = ''
    return
  }

  try {
    await bomStore.loadChildVersions(selectedReverseItem.value.itemCode)
    if (!bomStore.childVersions.includes(reverseVersion.value)) reverseVersion.value = ''
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 버전 목록을 불러오지 못했습니다.'
  }
}

async function searchDownwardTree() {
  if (!selectedDownwardItem.value) {
    bomStore.clearParentSearchState()
    hasSearchedDownward.value = false
    pageError.value = '정전개할 상위 품목을 선택해주세요.'
    return
  }

  try {
    pageError.value = null
    await bomStore.loadParentTree(selectedDownwardItem.value.itemCode, downwardVersion.value || undefined)
    hasSearchedDownward.value = true
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 정전개 데이터를 불러오지 못했습니다.'
  }
}

async function searchReverse() {
  if (!selectedReverseItem.value) {
    bomStore.clearChildSearchState()
    hasSearchedReverse.value = false
    pageError.value = '역전개할 구성 품목을 선택해주세요.'
    return
  }

  try {
    pageError.value = null
    await bomStore.loadChildParentTree(selectedReverseItem.value.itemCode, reverseVersion.value || undefined)
    await bomStore.loadChildParents(selectedReverseItem.value.itemCode, reverseVersion.value || undefined)
    hasSearchedReverse.value = true
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : 'BOM 역전개 데이터를 불러오지 못했습니다.'
  }
}

function showToast(message: string) {
  successToast.value = message
  setTimeout(() => {
    successToast.value = null
  }, 3000)
}

function formatDateTime(value: string) {
  if (!value) return '-'
  const date = new Date(value)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<template>
  <div class="bom-page">
    <div class="bom-heading">
      <div>
        <h1 class="bom-title">
          <span class="bom-title-mark"></span>
          BOM Master
        </h1>
        <p class="bom-subtitle">제품별 부품명세서를 등록하고, BOM 정전개와 역전개 구조를 확인합니다.</p>
      </div>
      <div v-if="successToast" class="bom-toast">
        <span class="bom-toast-dot"></span>
        {{ successToast }}
      </div>
    </div>

    <div v-if="pageError" class="bom-alert">
      <span>{{ pageError }}</span>
      <button type="button" class="bom-alert-close" @click="pageError = null">×</button>
    </div>

    <div class="bom-stats-grid">
      <article class="bom-stat-card">
        <div class="bom-stat-icon"><Boxes /></div>
        <div>
          <p class="bom-stat-label">전체 BOM</p>
          <p class="bom-stat-value">{{ stats.total }} 건</p>
        </div>
      </article>
      <article class="bom-stat-card">
        <div class="bom-stat-icon bom-stat-icon-primary"><Layers3 /></div>
        <div>
          <p class="bom-stat-label">상위 품목</p>
          <p class="bom-stat-value">{{ stats.parents }} 개</p>
        </div>
      </article>
      <article class="bom-stat-card">
        <div class="bom-stat-icon bom-stat-icon-success"><GitBranch /></div>
        <div>
          <p class="bom-stat-label">구성 품목</p>
          <p class="bom-stat-value">{{ stats.children }} 개</p>
        </div>
      </article>
      <article class="bom-stat-card">
        <div class="bom-stat-icon bom-stat-icon-warning"><RefreshCw /></div>
        <div>
          <p class="bom-stat-label">BOM 버전</p>
          <p class="bom-stat-value">{{ stats.versions }} 개</p>
        </div>
      </article>
    </div>

    <div class="bom-tabs">
      <button type="button" class="bom-tab" :class="{ 'is-active': activeTab === 'list' }" @click="activeTab = 'list'">BOM 목록</button>
      <button type="button" class="bom-tab" :class="{ 'is-active': activeTab === 'downward' }" @click="activeTab = 'downward'">정전개</button>
      <button type="button" class="bom-tab" :class="{ 'is-active': activeTab === 'reverse' }" @click="activeTab = 'reverse'">역전개</button>
    </div>

    <section v-if="activeTab === 'list'" class="bom-section">
      <div class="bom-panel bom-query-panel">
        <div class="bom-panel-header">
          <span class="bom-panel-title"><Search /> 조회 검색 조건</span>
          <button type="button" class="bom-icon-button" @click="isSearchExpanded = !isSearchExpanded">
            <ChevronDown :class="{ 'is-folded': !isSearchExpanded }" />
          </button>
        </div>
        <div v-show="isSearchExpanded" class="bom-filter-grid">
          <label class="bom-field bom-picker">
            <span class="bom-label">상위 품목</span>
            <input
              v-model="filterParent"
              class="bom-input"
              placeholder="품목 ID, 코드, 이름"
              @focus="isFilterParentPickerOpen = filterParent.trim().length > 0"
              @input="handleFilterParentInput"
              @keyup.enter="fetchPageData"
            >
            <div v-if="isFilterParentPickerOpen && filterParent.trim().length > 0" class="bom-picker-menu">
              <button
                v-for="item in filteredParentFilterItems"
                :key="item.itemId"
                type="button"
                class="bom-picker-option"
                @click="selectParentFilterItem(item)"
              >
                <span>{{ item.itemCode }}</span>
                <strong>{{ item.itemName }}</strong>
                <em>{{ item.itemType }}</em>
              </button>
              <div v-if="filteredParentFilterItems.length === 0" class="bom-picker-empty">검색된 상위 품목이 없습니다.</div>
            </div>
          </label>
          <label class="bom-field bom-picker">
            <span class="bom-label">구성 품목</span>
            <input
              v-model="filterChild"
              class="bom-input"
              placeholder="품목 ID, 코드, 이름"
              @focus="isFilterChildPickerOpen = filterChild.trim().length > 0"
              @input="handleFilterChildInput"
              @keyup.enter="fetchPageData"
            >
            <div v-if="isFilterChildPickerOpen && filterChild.trim().length > 0" class="bom-picker-menu">
              <button
                v-for="item in filteredChildFilterItems"
                :key="item.itemId"
                type="button"
                class="bom-picker-option"
                @click="selectChildFilterItem(item)"
              >
                <span>{{ item.itemCode }}</span>
                <strong>{{ item.itemName }}</strong>
                <em>{{ item.itemType }}</em>
              </button>
              <div v-if="filteredChildFilterItems.length === 0" class="bom-picker-empty">검색된 구성 품목이 없습니다.</div>
            </div>
          </label>
          <label class="bom-field">
            <span class="bom-label">BOM 버전</span>
            <input v-model="filterVersion" class="bom-input" placeholder="v1.0" @keyup.enter="fetchPageData">
          </label>
          <div class="bom-filter-actions">
            <button type="button" class="bom-button bom-button-muted" @click="resetFilters">초기화</button>
            <button type="button" class="bom-button bom-button-primary" @click="fetchPageData">조회</button>
          </div>
        </div>
      </div>

      <div class="bom-panel">
        <div class="bom-toolbar">
          <div class="bom-toolbar-meta">조회 결과 <span>{{ bomStore.bomGroups.totalElements }}</span>개 BOM</div>
          <div class="bom-toolbar-actions">
            <button type="button" class="bom-button bom-button-muted" @click="fetchPageData">
              <RefreshCw /> 새로고침
            </button>
            <button v-if="canWrite" type="button" class="bom-button bom-button-primary" @click="openCreateForm">
              <Plus /> BOM 등록
            </button>
          </div>
        </div>

        <div class="bom-table-wrap">
          <table class="bom-table">
            <thead>
              <tr>
                <th>No</th>
                <th>상위 품목</th>
                <th>유형</th>
                <th class="bom-center">구성 품목 수</th>
                <th>버전</th>
                <th>상태</th>
                <th>등록일</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="bomStore.isLoading">
                <td colspan="7" class="bom-empty"><Loader2 class="bom-spin" /> 데이터를 가져오고 있습니다...</td>
              </tr>
              <tr v-else-if="bomParentGroups.length === 0">
                <td colspan="7" class="bom-empty">조회된 BOM 데이터가 없습니다.</td>
              </tr>
              <tr
                v-for="(group, index) in bomParentGroups"
                v-else
                :key="group.key"
                class="bom-row"
                @click="selectGroup(group)"
              >
                <td>{{ bomStore.bomGroups.page * bomStore.bomGroups.size + index + 1 }}</td>
                <td>
                  <div class="bom-item-main">{{ group.parentItemCode }}</div>
                  <div class="bom-item-sub">{{ group.parentItemName }}</div>
                </td>
                <td><span class="bom-badge">{{ group.parentItemType }}</span></td>
                <td class="bom-center">{{ group.childCount }} 개</td>
                <td><span class="bom-version">{{ group.bomVersion }}</span></td>
                <td>
                  <span class="bom-status" :class="group.bomStatus === 'ACTIVE' ? 'is-active' : 'is-inactive'">
                    {{ group.bomStatus === 'ACTIVE' ? '활성' : '비활성' }}
                  </span>
                </td>
                <td>{{ formatDateTime(group.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="flex flex-col gap-3 border-t app-border-muted px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <p class="app-type-sm app-font-strong app-text-muted">
            총 {{ bomStore.bomGroups.totalElements.toLocaleString() }}건 · {{ pageStart.toLocaleString() }}-{{ pageEnd.toLocaleString() }} 표시 · {{ bomStore.bomGroups.size }}건씩 · {{ bomStore.bomGroups.page + 1 }} / {{ Math.max(bomStore.bomGroups.totalPages, 1) }} 페이지
          </p>
          <div class="flex gap-2">
            <button class="rounded-xl app-bg-muted px-4 py-2 app-type-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="bomStore.bomGroups.page === 0" @click="changePage(0)">처음</button>
            <button class="rounded-xl app-bg-muted px-4 py-2 app-type-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="bomStore.bomGroups.page === 0" @click="changePage(bomStore.bomGroups.page - 1)">이전</button>
            <button class="rounded-xl app-bg-muted px-4 py-2 app-type-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="bomStore.bomGroups.page >= bomStore.bomGroups.totalPages - 1" @click="changePage(bomStore.bomGroups.page + 1)">다음</button>
            <button class="rounded-xl app-bg-muted px-4 py-2 app-type-sm app-font-emphasis disabled:opacity-40" type="button" :disabled="bomStore.bomGroups.page >= bomStore.bomGroups.totalPages - 1" @click="changePage(bomStore.bomGroups.totalPages - 1)">마지막</button>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'downward'" class="bom-section">
      <div class="bom-panel bom-query-panel">
        <div class="bom-query-row">
          <div class="bom-field bom-query-field bom-picker">
            <span class="bom-label">상위 품목 검색</span>
            <div class="bom-picker-control">
              <input
                v-model="downwardKeyword"
                class="bom-input"
                placeholder="완제품/반제품 코드 또는 이름 검색"
                @focus="isDownwardPickerOpen = downwardKeyword.trim().length > 0"
                @input="handleDownwardKeywordInput"
                @keyup.enter="searchDownwardTree"
              >
              <button v-if="selectedDownwardItem" type="button" class="bom-picker-clear" @click="clearDownwardItem">선택 해제</button>
            </div>
            <div v-if="selectedDownwardItem" class="bom-selected-item">
              {{ selectedDownwardItem.itemCode }} / {{ selectedDownwardItem.itemName }} / {{ selectedDownwardItem.itemType }}
            </div>
            <div v-if="isDownwardPickerOpen && !selectedDownwardItem && downwardKeyword.trim().length > 0" class="bom-picker-menu">
              <button
                v-for="item in filteredDownwardItems"
                :key="item.itemId"
                type="button"
                class="bom-picker-option"
                @click="selectDownwardItem(item)"
              >
                <span>{{ item.itemCode }}</span>
                <strong>{{ item.itemName }}</strong>
                <em>{{ item.itemType }}</em>
              </button>
              <div v-if="filteredDownwardItems.length === 0" class="bom-picker-empty">검색된 상위 품목이 없습니다.</div>
            </div>
          </div>
          <label class="bom-field">
            <span class="bom-label">버전</span>
            <select v-model="downwardVersion" class="bom-input" :disabled="!selectedDownwardItem" @focus="loadDownwardVersions">
              <option value="">전체 버전</option>
              <option v-for="version in bomStore.parentVersions" :key="version" :value="version">{{ version }}</option>
            </select>
          </label>
          <label class="bom-field">
            <span class="bom-label">기준 수량</span>
            <input v-model.number="downwardBaseQuantity" type="number" min="1" step="1" class="bom-input">
          </label>
          <div class="bom-query-actions">
            <button type="button" class="bom-button bom-button-muted" @click="clearDownwardItem">초기화</button>
            <button type="button" class="bom-button bom-button-primary" :disabled="!selectedDownwardItem" @click="searchDownwardTree">정전개 조회</button>
          </div>
        </div>
      </div>
      <div class="bom-panel bom-tree-panel">
        <BomTreeList
          :nodes="displayedDownwardTree"
          relation-key="children"
          :empty-text="hasSearchedDownward ? '조회된 정전개 결과가 없습니다.' : '상위 품목을 선택하면 하위 자재 구조가 표시됩니다.'"
          show-cumulative
          :base-quantity="downwardBaseQuantity || 1"
        />
      </div>
    </section>

    <section v-else class="bom-section">
      <div class="bom-panel bom-query-panel">
        <div class="bom-query-row">
          <div class="bom-field bom-query-field bom-picker">
            <span class="bom-label">구성 품목 검색</span>
            <div class="bom-picker-control">
              <input
                v-model="reverseKeyword"
                class="bom-input"
                placeholder="원자재/반제품 코드 또는 이름 검색"
                @focus="isReversePickerOpen = reverseKeyword.trim().length > 0"
                @input="handleReverseKeywordInput"
                @keyup.enter="searchReverse"
              >
              <button v-if="selectedReverseItem" type="button" class="bom-picker-clear" @click="clearReverseItem">선택 해제</button>
            </div>
            <div v-if="selectedReverseItem" class="bom-selected-item">
              {{ selectedReverseItem.itemCode }} / {{ selectedReverseItem.itemName }} / {{ selectedReverseItem.itemType }}
            </div>
            <div v-if="isReversePickerOpen && !selectedReverseItem && reverseKeyword.trim().length > 0" class="bom-picker-menu">
              <button
                v-for="item in filteredReverseItems"
                :key="item.itemId"
                type="button"
                class="bom-picker-option"
                @click="selectReverseItem(item)"
              >
                <span>{{ item.itemCode }}</span>
                <strong>{{ item.itemName }}</strong>
                <em>{{ item.itemType }}</em>
              </button>
              <div v-if="filteredReverseItems.length === 0" class="bom-picker-empty">검색된 구성 품목이 없습니다.</div>
            </div>
          </div>
          <label class="bom-field">
            <span class="bom-label">버전</span>
            <select v-model="reverseVersion" class="bom-input" :disabled="!selectedReverseItem" @focus="loadReverseVersions">
              <option value="">전체 버전</option>
              <option v-for="version in bomStore.childVersions" :key="version" :value="version">{{ version }}</option>
            </select>
          </label>
          <div class="bom-query-actions">
            <button type="button" class="bom-button bom-button-muted" @click="clearReverseItem">초기화</button>
            <button type="button" class="bom-button bom-button-primary" :disabled="!selectedReverseItem" @click="searchReverse">역전개 조회</button>
          </div>
        </div>
      </div>

      <div class="bom-panel bom-tree-panel">
        <BomTreeList
          :nodes="hasSearchedReverse ? bomStore.childParentTree : []"
          relation-key="parents"
          empty-text="구성 품목을 선택하면 상위 제품 구조가 표시됩니다."
        />
      </div>

      <div v-if="hasSearchedReverse" class="bom-panel">
        <div class="bom-panel-header">
          <span class="bom-panel-title">상위 사용처 목록</span>
          <span class="bom-toolbar-meta">{{ bomStore.childParents.length }}건</span>
        </div>
        <div class="bom-table-wrap">
          <table class="bom-table bom-detail-table">
            <thead>
              <tr>
                <th>상위 품목</th>
                <th>유형</th>
                <th>소요량</th>
                <th>버전</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="parent in bomStore.childParents" :key="parent.bomId">
                <td>
                  <div class="bom-item-main">{{ parent.parentItemCode }}</div>
                  <div class="bom-item-sub">{{ parent.parentItemName }}</div>
                </td>
                <td><span class="bom-badge">{{ parent.parentItemType }}</span></td>
                <td class="bom-number">{{ formatQuantity(parent.quantity) }}</td>
                <td><span class="bom-version">{{ parent.bomVersion }}</span></td>
              </tr>
              <tr v-if="bomStore.childParents.length === 0">
                <td colspan="4" class="bom-empty">상위 사용처가 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <div v-if="isFormOpen" class="bom-modal-backdrop">
      <form class="bom-modal" @submit.prevent="submitForm">
        <div class="bom-modal-header">
          <div>
            <p class="bom-detail-kicker">{{ editingBomId ? 'BOM 수정' : 'BOM 등록' }}</p>
            <h2 class="bom-modal-title">품목 구성 정보</h2>
          </div>
          <button type="button" class="bom-icon-button" @click="closeForm"><X /></button>
        </div>

        <div v-if="formError" class="bom-form-error">{{ formError }}</div>

        <div class="bom-field bom-picker">
          <span class="bom-label">상위 품목</span>
          <div class="bom-picker-control">
            <input
              v-model="formParentKeyword"
              class="bom-input"
              placeholder="완제품/반제품 코드 또는 이름 검색"
              @focus="isFormParentPickerOpen = formParentKeyword.trim().length > 0"
              @input="handleFormParentKeywordInput"
            >
            <button v-if="form.parentItemId" type="button" class="bom-picker-clear" @click="clearFormParentItem">선택 해제</button>
          </div>
          <div v-if="selectedParent" class="bom-selected-item">
            {{ selectedParent.itemCode }} / {{ selectedParent.itemName }} / {{ selectedParent.itemType }}
          </div>
          <div v-if="isFormParentPickerOpen && !form.parentItemId && formParentKeyword.trim().length > 0" class="bom-picker-menu bom-modal-picker-menu">
            <button
              v-for="item in filteredFormParentItems"
              :key="item.itemId"
              type="button"
              class="bom-picker-option"
              @click="selectFormParentItem(item)"
            >
              <span>{{ item.itemCode }}</span>
              <strong>{{ item.itemName }}</strong>
              <em>{{ item.itemType }}</em>
            </button>
            <div v-if="filteredFormParentItems.length === 0" class="bom-picker-empty">검색된 상위 품목이 없습니다.</div>
          </div>
        </div>

        <div class="bom-field bom-picker">
          <span class="bom-label">구성 품목</span>
          <div class="bom-picker-control">
            <input
              v-model="formChildKeyword"
              class="bom-input"
              placeholder="원자재/반제품 코드 또는 이름 검색"
              @focus="isFormChildPickerOpen = formChildKeyword.trim().length > 0"
              @input="handleFormChildKeywordInput"
            >
            <button v-if="form.childItemId" type="button" class="bom-picker-clear" @click="clearFormChildItem">선택 해제</button>
          </div>
          <div v-if="selectedChild" class="bom-selected-item">
            {{ selectedChild.itemCode }} / {{ selectedChild.itemName }} / {{ selectedChild.itemType }}
          </div>
          <div v-if="isFormChildPickerOpen && !form.childItemId && formChildKeyword.trim().length > 0" class="bom-picker-menu bom-modal-picker-menu">
            <button
              v-for="item in filteredFormChildItems"
              :key="item.itemId"
              type="button"
              class="bom-picker-option"
              @click="selectFormChildItem(item)"
            >
              <span>{{ item.itemCode }}</span>
              <strong>{{ item.itemName }}</strong>
              <em>{{ item.itemType }}</em>
            </button>
            <div v-if="filteredFormChildItems.length === 0" class="bom-picker-empty">검색된 구성 품목이 없습니다.</div>
          </div>
        </div>

        <div v-if="!editingBomId" class="bom-form-lines">
          <button type="button" class="bom-button bom-button-muted" @click="addFormLine">
            <Plus /> 구성 품목 추가
          </button>
          <div v-if="formLines.length > 0" class="bom-table-wrap">
            <table class="bom-table bom-detail-table">
              <thead>
                <tr>
                  <th>구성 품목</th>
                  <th>유형</th>
                  <th>소요량</th>
                  <th>작업</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="line in formLines" :key="line.childItemId">
                  <td>
                    <div class="bom-item-main">{{ line.childItemCode }}</div>
                    <div class="bom-item-sub">{{ line.childItemName }}</div>
                  </td>
                  <td><span class="bom-badge">{{ line.childItemType }}</span></td>
                  <td class="bom-number">{{ formatQuantity(line.quantity) }}</td>
                  <td>
                    <button type="button" class="bom-table-button is-danger" @click="removeFormLine(line.childItemId)">
                      <Trash2 />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="bom-form-grid">
          <label class="bom-field">
            <span class="bom-label">소요량</span>
            <input v-model.number="form.quantity" type="number" min="1" step="1" class="bom-input" @blur="form.quantity = normalizeQuantity(form.quantity)">
          </label>
          <label class="bom-field">
            <span class="bom-label">BOM 버전</span>
            <input v-model="form.bomVersion" maxlength="20" class="bom-input" placeholder="v1.0">
          </label>
        </div>

        <div class="bom-modal-actions">
          <button type="button" class="bom-button bom-button-muted" @click="closeForm">취소</button>
          <button type="submit" class="bom-button bom-button-primary" :disabled="bomStore.isSaving">
            <Loader2 v-if="bomStore.isSaving" class="bom-spin" />
            저장
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.bom-page {
  --bom-color-surface: var(--color-surface);
  --bom-color-surface-muted: var(--color-page);
  --bom-color-surface-strong: var(--color-border-muted);
  --bom-color-border: var(--color-border);
  --bom-color-border-strong: var(--color-border-strong);
  --bom-color-text-primary: var(--color-text);
  --bom-color-text-secondary: var(--color-text-soft);
  --bom-color-text-muted: var(--color-text-muted);
  --bom-color-primary: var(--color-primary);
  --bom-color-primary-hover: var(--color-primary-hover);
  --bom-color-primary-soft: var(--color-primary-soft);
  --bom-color-success: var(--color-success);
  --bom-color-success-soft: var(--color-success-soft);
  --bom-color-warning: var(--color-warning);
  --bom-color-warning-soft: var(--color-warning-soft);
  --bom-color-danger: var(--color-danger);
  --bom-color-danger-soft: var(--color-danger-soft);
  --bom-radius-panel: var(--radius-section);
  --bom-radius-control: var(--radius-control);
  --bom-radius-pill: var(--radius-pill);
  --bom-shadow-panel: var(--shadow-panel);
  --bom-font-size-2xs: 0.625rem;
  --bom-font-size-xs: 0.75rem;
  --bom-font-size-sm: 0.875rem;
  --bom-font-size-lg: 1.25rem;
  --bom-font-size-xl: 1.5rem;
  --bom-font-weight-title: var(--font-weight-title);
  --bom-font-weight-label: var(--font-weight-label);
  --bom-font-weight-strong: var(--font-weight-title);
  display: grid;
  gap: 1.5rem;
  padding-bottom: 3rem;
}

.bom-heading,
.bom-toolbar,
.bom-query-row,
.bom-modal-header,
.bom-modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.bom-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--bom-color-text-primary);
  font-size: var(--bom-font-size-xl);
  font-weight: var(--bom-font-weight-title);
  letter-spacing: -0.025em;
}

.bom-title-mark {
  width: 0.375rem;
  height: 1.5rem;
  background: var(--bom-color-primary);
  border-radius: 0.125rem;
}

.bom-subtitle,
.bom-detail-kicker,
.bom-stat-label,
.bom-item-sub,
.bom-readonly {
  color: var(--bom-color-text-muted);
  font-size: var(--bom-font-size-xs);
}

.bom-toast,
.bom-alert,
.bom-form-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: var(--bom-radius-control);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-label);
}

.bom-toast {
  background: var(--bom-color-success-soft);
  color: var(--bom-color-success);
  border: 1px solid var(--color-success-border);
}

.bom-toast-dot {
  width: 0.5rem;
  height: 0.5rem;
  background: var(--bom-color-success);
  border-radius: var(--bom-radius-pill);
}

.bom-alert,
.bom-form-error {
  justify-content: space-between;
  background: var(--bom-color-danger-soft);
  color: var(--bom-color-danger);
  border: 1px solid var(--color-danger-border);
}

.bom-alert-close,
.bom-icon-button,
.bom-table-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.bom-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1.25rem;
}

.bom-stat-card,
.bom-panel,
.bom-detail-panel,
.bom-modal {
  background: var(--bom-color-surface);
  border: 1px solid var(--bom-color-border);
  border-radius: var(--bom-radius-panel);
  box-shadow: var(--bom-shadow-panel);
}

.bom-stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
}

.bom-stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 3rem;
  height: 3rem;
  color: var(--bom-color-text-secondary);
  background: var(--bom-color-surface-strong);
  border-radius: var(--bom-radius-control);
}

.bom-stat-icon svg,
.bom-panel-title svg,
.bom-button svg,
.bom-table-button svg,
.bom-icon-button svg {
  width: 1rem;
  height: 1rem;
}

.bom-stat-icon-primary {
  color: var(--bom-color-primary);
  background: var(--bom-color-primary-soft);
}

.bom-stat-icon-success {
  color: var(--bom-color-success);
  background: var(--bom-color-success-soft);
}

.bom-stat-icon-warning {
  color: var(--bom-color-warning);
  background: var(--bom-color-warning-soft);
}

.bom-stat-value {
  margin-top: 0.25rem;
  color: var(--bom-color-text-primary);
  font-size: var(--bom-font-size-lg);
  font-weight: var(--bom-font-weight-title);
}

.bom-tabs {
  display: inline-flex;
  width: fit-content;
  padding: 0.25rem;
  background: var(--bom-color-surface-strong);
  border: 1px solid var(--bom-color-border);
  border-radius: var(--bom-radius-panel);
}

.bom-tab {
  min-width: 6rem;
  padding: 0.625rem 1rem;
  color: var(--bom-color-text-secondary);
  border: 0;
  border-radius: var(--bom-radius-control);
  background: transparent;
  cursor: pointer;
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-label);
}

.bom-tab.is-active {
  color: var(--bom-color-primary);
  background: var(--bom-color-surface);
  box-shadow: var(--bom-shadow-panel);
}

.bom-section {
  display: grid;
  gap: 1rem;
}

.bom-panel {
  overflow: hidden;
}

.bom-query-panel {
  position: relative;
  z-index: 30;
  overflow: visible;
}

.bom-panel-header,
.bom-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.25rem;
  background: var(--bom-color-surface-muted);
  border-bottom: 1px solid var(--bom-color-border);
}

.bom-panel-title,
.bom-toolbar-meta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--bom-color-text-primary);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-strong);
}

.bom-toolbar-meta span {
  color: var(--bom-color-primary);
}

.bom-icon-button {
  width: 2rem;
  height: 2rem;
  color: var(--bom-color-text-secondary);
  border-radius: var(--bom-radius-control);
}

.bom-icon-button:hover {
  background: var(--bom-color-surface-strong);
}

.bom-icon-button .is-folded {
  transform: rotate(180deg);
}

.bom-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  padding: 1.25rem;
}

.bom-field {
  display: grid;
  gap: 0.375rem;
}

.bom-label {
  color: var(--bom-color-text-secondary);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-label);
}

.bom-input {
  width: 100%;
  height: 2.5rem;
  padding: 0 0.75rem;
  color: var(--bom-color-text-primary);
  background: var(--bom-color-surface);
  border: 1px solid var(--bom-color-border-strong);
  border-radius: var(--bom-radius-control);
  outline: none;
  font-size: var(--bom-font-size-xs);
}

.bom-input:focus {
  border-color: var(--bom-color-primary);
  box-shadow: 0 0 0 1px var(--bom-color-primary);
}

.bom-filter-actions,
.bom-toolbar-actions,
.bom-row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}

.bom-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  height: 2.5rem;
  padding: 0 1rem;
  border-radius: var(--bom-radius-control);
  cursor: pointer;
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-strong);
  transition: background-color 0.15s, color 0.15s, border-color 0.15s;
}

.bom-button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.bom-button-primary {
  color: var(--bom-color-surface);
  background: var(--bom-color-primary);
  border: 1px solid var(--bom-color-primary);
}

.bom-button-primary:hover {
  background: var(--bom-color-primary-hover);
}

.bom-button-muted {
  color: var(--bom-color-text-secondary);
  background: var(--bom-color-surface);
  border: 1px solid var(--bom-color-border-strong);
}

.bom-button-muted:hover {
  background: var(--bom-color-surface-strong);
}

.bom-table-wrap {
  overflow-x: auto;
}

.bom-form-lines {
  display: grid;
  gap: 0.75rem;
}

.bom-table {
  width: 100%;
  min-width: 1120px;
  border-collapse: collapse;
  color: var(--bom-color-text-secondary);
  font-size: var(--bom-font-size-xs);
  text-align: left;
}

.bom-table th {
  padding: 0.875rem 1rem;
  color: var(--bom-color-text-primary);
  background: var(--bom-color-surface-muted);
  border-bottom: 1px solid var(--bom-color-border);
  font-weight: var(--bom-font-weight-strong);
  white-space: nowrap;
}

.bom-table td {
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--bom-color-border);
  vertical-align: middle;
}

.bom-row {
  cursor: pointer;
}

.bom-row:hover,
.bom-row.is-selected {
  background: var(--bom-color-primary-soft);
}

.bom-item-main {
  color: var(--bom-color-text-primary);
  font-weight: var(--bom-font-weight-label);
}

.bom-number {
  text-align: right;
  color: var(--bom-color-text-primary);
  font-weight: var(--bom-font-weight-label);
}

.bom-center {
  text-align: center;
}

.bom-badge,
.bom-version,
.bom-status {
  display: inline-flex;
  align-items: center;
  padding: 0.1875rem 0.5rem;
  color: var(--bom-color-text-secondary);
  background: var(--bom-color-surface-strong);
  border-radius: var(--bom-radius-pill);
  font-size: var(--bom-font-size-2xs);
  font-weight: var(--bom-font-weight-label);
}

.bom-status.is-active {
  color: var(--bom-color-success);
  background: var(--bom-color-success-soft);
}

.bom-status.is-inactive {
  color: var(--bom-color-text-muted);
  background: var(--bom-color-surface-muted);
}

.bom-table-button {
  min-width: 2rem;
  height: 2rem;
  padding: 0 0.5rem;
  color: var(--bom-color-primary);
  background: var(--bom-color-primary-soft);
  border-radius: var(--bom-radius-control);
}

.bom-table-button.is-danger {
  color: var(--bom-color-danger);
  background: var(--bom-color-danger-soft);
}

.bom-empty {
  padding: 3rem 1rem;
  color: var(--bom-color-text-muted);
  text-align: center;
}

.bom-spin {
  display: inline-block;
  animation: bom-spin 0.9s linear infinite;
}

.bom-detail-panel {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
}

.bom-detail-title,
.bom-modal-title {
  color: var(--bom-color-text-primary);
  font-size: var(--bom-font-size-lg);
  font-weight: var(--bom-font-weight-title);
}

.bom-detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.bom-detail-grid div {
  display: grid;
  gap: 0.25rem;
  padding: 1rem;
  background: var(--bom-color-surface-muted);
  border-radius: var(--bom-radius-control);
}

.bom-detail-grid span {
  color: var(--bom-color-text-muted);
  font-size: var(--bom-font-size-2xs);
}

.bom-detail-grid strong {
  color: var(--bom-color-text-primary);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-label);
}

.bom-detail-lines {
  display: grid;
  gap: 0.75rem;
}

.bom-detail-lines-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  color: var(--bom-color-text-secondary);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-strong);
}

.bom-detail-lines-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.bom-detail-table {
  min-width: 720px;
  border: 1px solid var(--bom-color-border);
  border-radius: var(--bom-radius-control);
}

.bom-query-row {
  align-items: end;
  padding: 1.25rem;
}

.bom-query-field {
  flex: 1;
}

.bom-query-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.bom-picker {
  position: relative;
}

.bom-picker-control {
  position: relative;
}

.bom-picker-clear {
  position: absolute;
  top: 50%;
  right: 0.5rem;
  height: 1.625rem;
  padding: 0 0.5rem;
  color: var(--bom-color-text-secondary);
  background: var(--bom-color-surface-strong);
  border: 1px solid var(--bom-color-border);
  border-radius: var(--bom-radius-control);
  cursor: pointer;
  font-size: var(--bom-font-size-2xs);
  font-weight: var(--bom-font-weight-label);
  transform: translateY(-50%);
}

.bom-selected-item {
  width: fit-content;
  padding: 0.375rem 0.625rem;
  color: var(--bom-color-primary);
  background: var(--bom-color-primary-soft);
  border: 1px solid var(--color-info-border);
  border-radius: var(--bom-radius-pill);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-label);
}

.bom-picker-menu {
  position: absolute;
  top: calc(100% + 0.375rem);
  left: 0;
  z-index: 20;
  display: grid;
  gap: 0.25rem;
  width: 100%;
  max-height: 18rem;
  padding: 0.5rem;
  overflow-y: auto;
  background: var(--bom-color-surface);
  border: 1px solid var(--bom-color-border-strong);
  border-radius: var(--bom-radius-control);
  box-shadow: 0 18px 40px rgb(15 23 42 / 0.12);
}

.bom-modal-picker-menu {
  z-index: 60;
  max-height: 14rem;
}

.bom-picker-option {
  display: grid;
  grid-template-columns: 8rem minmax(0, 1fr) 4rem;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.625rem 0.75rem;
  color: var(--bom-color-text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--bom-radius-control);
  cursor: pointer;
  text-align: left;
}

.bom-picker-option:hover {
  background: var(--bom-color-primary-soft);
}

.bom-picker-option span {
  color: var(--bom-color-primary);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-strong);
}

.bom-picker-option strong {
  min-width: 0;
  overflow: hidden;
  color: var(--bom-color-text-primary);
  font-size: var(--bom-font-size-xs);
  font-weight: var(--bom-font-weight-label);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bom-picker-option em,
.bom-picker-empty {
  color: var(--bom-color-text-muted);
  font-size: var(--bom-font-size-2xs);
  font-style: normal;
  font-weight: var(--bom-font-weight-label);
}

.bom-picker-empty {
  padding: 1rem;
  text-align: center;
}

.bom-tree-panel {
  padding: 1.25rem;
}

.bom-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 1rem;
  overflow-y: auto;
  background: rgb(15 23 42 / 0.45);
}

.bom-modal {
  display: grid;
  gap: 1rem;
  width: min(36rem, 100%);
  max-height: calc(100vh - 2rem);
  padding: 1.5rem;
  overflow-y: auto;
}

.bom-modal-header {
  position: sticky;
  top: -1.5rem;
  z-index: 2;
  margin: -1.5rem -1.5rem 0;
  padding: 1.5rem 1.5rem 1rem;
  background: var(--bom-color-surface);
  border-bottom: 1px solid var(--bom-color-border);
}

.bom-modal-actions {
  position: sticky;
  bottom: -1.5rem;
  z-index: 2;
  margin: 0 -1.5rem -1.5rem;
  padding: 1rem 1.5rem 1.5rem;
  background: var(--bom-color-surface);
  border-top: 1px solid var(--bom-color-border);
}

.bom-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

@keyframes bom-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1024px) {
  .bom-stats-grid,
  .bom-filter-grid,
  .bom-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bom-query-row {
    align-items: stretch;
    flex-direction: column;
  }

  .bom-query-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .bom-heading,
  .bom-toolbar,
  .bom-modal-header,
  .bom-modal-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .bom-stats-grid,
  .bom-filter-grid,
  .bom-detail-grid,
  .bom-form-grid {
    grid-template-columns: 1fr;
  }

  .bom-tabs {
    width: 100%;
  }

  .bom-tab {
    flex: 1;
  }

  .bom-query-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
