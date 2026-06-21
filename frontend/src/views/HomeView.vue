<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import {
  AlertTriangle,
  BarChart3,
  Bot,
  CalendarDays,
  CheckCircle2,
  Database,
  Factory,
  PackageSearch,
  RefreshCw,
  Route,
  Truck
} from '@lucide/vue'
import { useDashboardStore } from '@/state/dashboardStore'
import AiChartPanel from '@/ui/AiChartPanel.vue'
import AiResultTable from '@/ui/AiResultTable.vue'
import type {
  DashboardMetricCategory,
  DashboardPeriod,
  DashboardSeverity
} from '@/api/dashboardApi'

const dashboardStore = useDashboardStore()

const periodOptions: { label: string; value: DashboardPeriod }[] = [
  { label: '오늘', value: 'today' },
  { label: '7일', value: '7d' },
  { label: '30일', value: '30d' }
]

const metricIcons: Record<DashboardMetricCategory, typeof Factory> = {
  production: Factory,
  quality: CheckCircle2,
  inventory: PackageSearch,
  shipping: Truck
}

const generatedAtText = computed(() => {
  const generatedAt = dashboardStore.snapshot?.generatedAt
  if (!generatedAt) return '-'

  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(generatedAt))
})

onMounted(() => {
  dashboardStore.loadDashboard()
})

function severityClass(severity: DashboardSeverity) {
  return {
    success: 'app-status-success',
    warning: 'app-status-warning',
    danger: 'app-status-danger',
    neutral: 'app-status-neutral'
  }[severity]
}

function severityLabel(severity: DashboardSeverity) {
  return {
    success: '정상',
    warning: '주의',
    danger: '위험',
    neutral: '관찰'
  }[severity]
}

function selectPeriod(period: DashboardPeriod) {
  dashboardStore.loadDashboard(period)
}

function getDashboardAnalysisQuestion(metricId: DashboardMetricCategory) {
  const periodLabel = periodOptions.find(option => option.value === dashboardStore.selectedPeriod)?.label ?? '선택 기간'
  const questions: Record<DashboardMetricCategory, string> = {
    production: `${periodLabel} 기준 라인별 생산량을 비교해서 보여줘`,
    quality: `${periodLabel} 기준 제품별 불량률을 비교해서 보여줘`,
    inventory: `${periodLabel} 기준 창고별 재고 회전율을 비교해서 보여줘`,
    shipping: `${periodLabel} 기준 거래처별 출하 대기 물량을 비교해서 보여줘`
  }
  return questions[metricId]
}
</script>

<template>
  <div class="app-page">
    <div
      v-if="dashboardStore.error"
      class="app-alert app-alert-danger"
    >
      <AlertTriangle class="h-5 w-5 shrink-0" />
      <div>
        <h4 class="app-alert-title">대시보드 오류</h4>
        <p class="app-alert-text">{{ dashboardStore.error }}</p>
      </div>
    </div>

    <div class="app-page-header">
      <div>
        <h1 class="app-page-title">운영 대시보드</h1>
        <p class="app-page-subtitle">생산, 품질, 재고, 출하 지표를 비교하고 AI 추천 차트로 현장 이상 징후를 확인합니다.</p>
      </div>

      <div class="flex flex-wrap items-center gap-2">
        <span class="app-status app-status-neutral">
          <CalendarDays class="h-3.5 w-3.5" />
          {{ generatedAtText }}
        </span>
        <div class="flex rounded-lg border p-1" style="border-color: var(--color-border); background-color: var(--color-surface);">
          <button
            v-for="period in periodOptions"
            :key="period.value"
            type="button"
            class="app-tab"
            :class="{ 'is-active': dashboardStore.selectedPeriod === period.value }"
            :disabled="dashboardStore.isLoading"
            @click="selectPeriod(period.value)"
          >
            {{ period.label }}
          </button>
        </div>
        <button
          type="button"
          class="app-button app-button-muted"
          :disabled="dashboardStore.isLoading"
          @click="dashboardStore.loadDashboard()"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': dashboardStore.isLoading }" />
          새로고침
        </button>
      </div>
    </div>

    <div v-if="dashboardStore.isLoading && !dashboardStore.snapshot" class="app-empty">
      대시보드 데이터를 불러오고 있습니다.
    </div>

    <template v-else>
      <section class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div
          v-for="card in dashboardStore.summaryCards"
          :key="card.id"
          class="app-stat-row-card"
        >
          <div
            class="app-stat-icon"
            :class="{
              'app-stat-icon-success': card.severity === 'success',
              'app-stat-icon-warning': card.severity === 'warning',
              'app-stat-icon-primary': card.severity === 'neutral'
            }"
            :style="card.severity === 'danger' ? 'background-color: var(--color-danger-soft); color: var(--color-danger);' : undefined"
          >
            <BarChart3 class="h-5 w-5" />
          </div>
          <div class="min-w-0">
            <p class="app-stat-label">{{ card.label }}</p>
            <p class="app-stat-value">{{ card.value }}</p>
            <p class="mt-1 truncate text-xs app-table-muted">{{ card.caption }}</p>
          </div>
        </div>
      </section>

      <section class="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.65fr)]">
        <div class="space-y-6">
          <div class="app-panel">
            <div class="app-panel-head">
              <h2 class="app-panel-title">
                <Route class="app-panel-icon" />
                다차원 분석 지표
              </h2>
              <span class="app-status app-status-info">AI 차트 추천 적용</span>
            </div>

            <div class="space-y-5 p-5">
              <div class="grid grid-cols-2 gap-2 lg:grid-cols-4">
                <button
                  v-for="metric in dashboardStore.metricViews"
                  :key="metric.id"
                  type="button"
                  class="flex min-h-24 flex-col items-start justify-between rounded-lg border p-4 text-left transition"
                  :class="dashboardStore.selectedMetricId === metric.id ? 'shadow-sm' : 'hover:shadow-sm'"
                  :style="dashboardStore.selectedMetricId === metric.id
                    ? 'border-color: var(--color-primary); background-color: var(--color-primary-soft);'
                    : 'border-color: var(--color-border); background-color: var(--color-surface);'"
                  @click="dashboardStore.selectMetric(metric.id)"
                >
                  <component
                    :is="metricIcons[metric.id]"
                    class="h-5 w-5"
                    :style="dashboardStore.selectedMetricId === metric.id ? 'color: var(--color-primary);' : 'color: var(--color-text-muted);'"
                  />
                  <div>
                    <p class="text-sm app-table-main">{{ metric.label }}</p>
                    <p class="mt-1 text-xs app-table-muted">{{ metric.chart.type }} 추천</p>
                  </div>
                </button>
              </div>

              <div v-if="dashboardStore.selectedMetric" class="grid grid-cols-1 gap-6 2xl:grid-cols-[minmax(0,1fr)_22rem]">
                <div class="space-y-4">
                  <div>
                    <h3 class="text-lg app-table-main">{{ dashboardStore.selectedMetric.title }}</h3>
                    <p class="mt-1 text-sm app-table-muted">{{ dashboardStore.selectedMetric.subtitle }}</p>
                  </div>

                  <AiChartPanel
                    :chart="dashboardStore.selectedMetric.chart"
                    :rows="dashboardStore.selectedMetric.rows"
                  />
                </div>

                <div class="app-card flex items-center">
                  <RouterLink
                    class="app-button app-button-primary w-full"
                    :to="{
                      name: 'ai-queries',
                      query: {
                        question: getDashboardAnalysisQuestion(dashboardStore.selectedMetric.id)
                      }
                    }"
                  >
                    <Bot class="h-4 w-4" />
                    AI 데이터 챗봇에서 상세 분석
                  </RouterLink>
                </div>
              </div>
            </div>
          </div>

          <AiResultTable :rows="dashboardStore.selectedMetric?.rows ?? []" />
        </div>

        <aside class="space-y-6">
          <div class="app-panel">
            <div class="app-panel-head">
              <h2 class="app-panel-title">
                <AlertTriangle class="app-panel-icon" />
                우선 확인 인사이트
              </h2>
              <span class="text-xs app-table-muted">{{ dashboardStore.insights.length }}건</span>
            </div>

            <div class="divide-y" style="border-color: var(--color-border-muted);">
              <div
                v-for="insight in dashboardStore.insights"
                :key="insight.id"
                class="space-y-3 p-5"
              >
                <div class="flex items-center justify-between gap-3">
                  <span class="app-status" :class="severityClass(insight.severity)">
                    {{ severityLabel(insight.severity) }}
                  </span>
                  <span class="flex items-center gap-1 text-xs app-table-muted">
                    <Database class="h-3.5 w-3.5" />
                    {{ insight.source }}
                  </span>
                </div>
                <div>
                  <h3 class="text-sm app-table-main">{{ insight.title }}</h3>
                  <p class="mt-1 text-sm leading-6 app-table-muted">{{ insight.description }}</p>
                </div>
              </div>
            </div>
          </div>

        </aside>
      </section>
    </template>
  </div>
</template>
