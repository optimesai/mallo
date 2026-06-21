import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { dashboardService } from '@/services/dashboardService'
import type {
  DashboardMetricCategory,
  DashboardMetricView,
  DashboardPeriod,
  DashboardSnapshot
} from '@/api/dashboardApi'

export const useDashboardStore = defineStore('dashboard-main', () => {
  const selectedPeriod = ref<DashboardPeriod>('7d')
  const selectedMetricId = ref<DashboardMetricCategory>('production')
  const snapshot = ref<DashboardSnapshot | null>(null)
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  const summaryCards = computed(() => snapshot.value?.summaryCards ?? [])
  const metricViews = computed(() => snapshot.value?.metricViews ?? [])
  const insights = computed(() => snapshot.value?.insights ?? [])

  const selectedMetric = computed<DashboardMetricView | null>(() => {
    return metricViews.value.find(metric => metric.id === selectedMetricId.value) ?? metricViews.value[0] ?? null
  })

  async function loadDashboard(period = selectedPeriod.value) {
    isLoading.value = true
    error.value = null

    try {
      selectedPeriod.value = period
      const data = await dashboardService.getSnapshot(period)
      snapshot.value = data

      if (!data.metricViews.some(metric => metric.id === selectedMetricId.value)) {
        selectedMetricId.value = data.metricViews[0]?.id ?? 'production'
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '대시보드 데이터를 불러오지 못했습니다.'
    } finally {
      isLoading.value = false
    }
  }

  function selectMetric(metricId: DashboardMetricCategory) {
    selectedMetricId.value = metricId
  }

  return {
    selectedPeriod,
    selectedMetricId,
    snapshot,
    summaryCards,
    metricViews,
    insights,
    selectedMetric,
    isLoading,
    error,
    loadDashboard,
    selectMetric
  }
})
