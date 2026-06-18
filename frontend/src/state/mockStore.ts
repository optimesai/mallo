import { defineStore } from 'pinia'
import { ref } from 'vue'
import { mockService } from '@/services/mockService'

export interface MockStat {
  label: string
  value: string
}

export interface TableColumn {
  key: string
  label: string
}

export const useMockStore = defineStore('dashboard', () => {
  const title = ref<string>('')
  const subtitle = ref<string>('')
  const stats = ref<MockStat[]>([])
  const tableTitle = ref<string>('')
  const tableMoreText = ref<string>('')
  const columns = ref<TableColumn[]>([])
  const tableData = ref<Record<string, any>[]>([])
  const isLoading = ref<boolean>(false)

  async function loadMockData() {
    isLoading.value = true
    try {
      const data = await mockService.fetchMock()
      title.value = data.title
      subtitle.value = data.subtitle
      stats.value = data.stats
      tableTitle.value = data.tableTitle
      tableMoreText.value = data.tableMoreText
      columns.value = data.columns
      tableData.value = data.tableData
    } catch (error) {
      console.error('Failed to update state:', error)
    } finally {
      isLoading.value = false
    }
  }

  return {
    title,
    subtitle,
    stats,
    tableTitle,
    tableMoreText,
    columns,
    tableData,
    isLoading,
    loadMockData
  } 
})
