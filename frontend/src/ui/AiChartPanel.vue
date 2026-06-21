<script setup lang="ts">
import { computed, ref } from 'vue'
import { BarChart3, Maximize2, Table2, X } from '@lucide/vue'
import type { AiChartResponse } from '@/api/aiApi'

const props = defineProps<{
  chart?: AiChartResponse
  rows: Record<string, unknown>[]
}>()

interface ChartPoint {
  label: string
  value: number
}

const primaryYKey = computed(() => props.chart?.yKeys?.[0])
const isModalOpen = ref(false)
const donutColors = ['#2563eb', '#16a34a', '#f97316', '#dc2626', '#7c3aed', '#0891b2', '#64748b', '#ca8a04']

const columns = computed(() => {
  const keys = new Set<string>()
  props.rows.forEach(row => {
    Object.keys(row).forEach(key => keys.add(key))
  })
  return Array.from(keys)
})

const previewRows = computed(() => props.rows.slice(0, 5))
const modalRows = computed(() => props.rows.slice(0, 20))

const points = computed<ChartPoint[]>(() => {
  const xKey = props.chart?.xKey
  const yKey = primaryYKey.value
  if (!xKey || !yKey) return []

  return props.rows
    .map(row => ({
      label: formatLabel(row[xKey]),
      value: toNumber(row[yKey])
    }))
    .filter(point => Number.isFinite(point.value))
    .slice(0, 12)
})

const maxValue = computed(() => Math.max(...points.value.map(point => point.value), 0))
const donutTotal = computed(() => points.value.reduce((sum, point) => sum + Math.max(point.value, 0), 0))

const donutSegments = computed(() => {
  let offset = 25
  return points.value
    .filter(point => point.value > 0 && donutTotal.value > 0)
    .map((point, index) => {
      const percent = (point.value / donutTotal.value) * 100
      const segment = {
        ...point,
        percent,
        color: donutColors[index % donutColors.length],
        dashArray: `${percent} ${100 - percent}`,
        dashOffset: offset
      }
      offset -= percent
      return segment
    })
})

const linePath = computed(() => {
  if (points.value.length === 0 || maxValue.value <= 0) return ''
  const width = 520
  const height = 180
  const gap = points.value.length === 1 ? 0 : width / (points.value.length - 1)
  return points.value
    .map((point, index) => {
      const x = points.value.length === 1 ? width / 2 : gap * index
      const y = height - (point.value / maxValue.value) * height
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`
    })
    .join(' ')
})

const statValue = computed(() => {
  if (points.value.length > 0) {
    return points.value.reduce((sum, point) => sum + point.value, 0)
  }
  const yKey = primaryYKey.value
  if (!yKey) return 0
  return props.rows.reduce((sum, row) => sum + toNumber(row[yKey]), 0)
})

function toNumber(value: unknown) {
  if (typeof value === 'number') return value
  if (typeof value === 'string') {
    const parsed = Number(value.replace(/,/g, ''))
    return Number.isNaN(parsed) ? 0 : parsed
  }
  return 0
}

function formatLabel(value: unknown) {
  if (value === null || value === undefined) return '-'
  return String(value)
}

function formatCell(value: unknown) {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'number') return value.toLocaleString()
  if (typeof value === 'boolean') return value ? 'Y' : 'N'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
</script>

<template>
  <div class="app-panel">
    <button
      type="button"
      class="app-panel-head w-full"
      @click="isModalOpen = true"
    >
      <h3 class="app-panel-title">
        <BarChart3 class="app-panel-icon" />
        {{ chart?.title || '차트 추천' }}
      </h3>
      <span class="flex items-center gap-2">
        <span class="app-status app-status-neutral">{{ chart?.type || 'NONE' }}</span>
        <Maximize2 class="h-4 w-4 app-table-muted" />
      </span>
    </button>

    <button
      type="button"
      class="block w-full p-5 text-left"
      @click="isModalOpen = true"
    >
      <div v-if="!chart?.enabled || chart.type === 'NONE'" class="app-empty py-10">
        {{ chart?.reason || '추천된 차트가 없습니다.' }}
      </div>

      <div v-else-if="chart.type === 'TABLE'" class="space-y-4">
        <div class="flex items-start gap-3 rounded-lg border p-4" style="border-color: var(--color-border-muted); background-color: var(--color-surface-muted);">
          <Table2 class="mt-0.5 h-5 w-5 app-table-muted" />
          <div>
            <p class="text-sm app-table-main">표 형식이 가장 적합합니다.</p>
            <p class="mt-1 text-xs leading-5 app-table-muted">{{ chart.reason || '목록 또는 상세 데이터는 그래프보다 표로 확인하는 편이 정확합니다.' }}</p>
          </div>
        </div>
        <div class="overflow-hidden rounded-lg border" style="border-color: var(--color-border);">
          <table class="w-full text-xs">
            <thead style="background-color: var(--color-surface-muted);">
              <tr>
                <th v-for="column in columns.slice(0, 4)" :key="column" class="px-3 py-2 text-left app-table-muted">
                  {{ column }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rowIndex) in previewRows" :key="rowIndex" class="border-t" style="border-color: var(--color-border-muted);">
                <td v-for="column in columns.slice(0, 4)" :key="column" class="px-3 py-2 app-table-main">
                  {{ formatCell(row[column]) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-else-if="chart.type === 'STAT'" class="app-stat-row-card">
        <div class="app-stat-icon app-stat-icon-primary">
          <BarChart3 class="h-5 w-5" />
        </div>
        <div>
          <p class="app-stat-label">{{ primaryYKey || '합계' }}</p>
          <p class="app-stat-value">{{ statValue.toLocaleString() }}</p>
        </div>
      </div>

      <div v-else-if="chart.type === 'BAR'" class="space-y-3">
        <div v-for="point in points" :key="point.label" class="grid grid-cols-[8rem_1fr_5rem] items-center gap-3 text-xs">
          <span class="truncate app-table-strong">{{ point.label }}</span>
          <div class="h-6 rounded app-bg-muted">
            <div
              class="h-6 rounded"
              style="background-color: var(--color-primary);"
              :style="{ width: maxValue > 0 ? `${Math.max((point.value / maxValue) * 100, 2)}%` : '0%' }"
            ></div>
          </div>
          <span class="text-right tabular-nums app-table-number">{{ point.value.toLocaleString() }}</span>
        </div>
      </div>

      <div v-else-if="chart.type === 'DONUT'" class="grid grid-cols-[9rem_1fr] items-center gap-5">
        <svg viewBox="0 0 42 42" class="h-36 w-36 -rotate-90">
          <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="var(--color-border-muted)" stroke-width="5" />
          <circle
            v-for="segment in donutSegments"
            :key="segment.label"
            cx="21"
            cy="21"
            r="15.915"
            fill="transparent"
            :stroke="segment.color"
            stroke-width="5"
            :stroke-dasharray="segment.dashArray"
            :stroke-dashoffset="segment.dashOffset"
          />
        </svg>
        <div class="space-y-2">
          <div v-for="segment in donutSegments.slice(0, 6)" :key="segment.label" class="flex items-center justify-between gap-3 text-xs">
            <span class="flex min-w-0 items-center gap-2">
              <span class="h-2.5 w-2.5 shrink-0 rounded-full" :style="{ backgroundColor: segment.color }"></span>
              <span class="truncate app-table-strong">{{ segment.label }}</span>
            </span>
            <span class="tabular-nums app-table-number">{{ segment.percent.toFixed(1) }}%</span>
          </div>
        </div>
      </div>

      <div v-else-if="chart.type === 'LINE'" class="space-y-4">
        <svg viewBox="0 0 520 180" class="h-52 w-full overflow-visible">
          <path
            :d="linePath"
            fill="none"
            stroke="var(--color-primary)"
            stroke-width="3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <div class="flex justify-between gap-2 text-[10px] app-table-muted">
          <span v-for="point in points" :key="point.label" class="max-w-20 truncate">{{ point.label }}</span>
        </div>
      </div>

      <div v-else class="app-empty py-10">
        {{ chart.type }} 차트는 표 형태로 확인해 주세요.
      </div>
    </button>

    <Teleport to="body">
      <div
        v-if="isModalOpen"
        class="app-modal-backdrop app-modal-backdrop-loose"
        @click.self="isModalOpen = false"
      >
        <div class="app-modal-shell w-full max-w-5xl">
          <div class="app-modal-head">
            <h3 class="flex items-center gap-2 text-sm font-bold">
              <BarChart3 class="h-4 w-4" />
              {{ chart?.title || '차트 추천 크게 보기' }}
            </h3>
            <button
              type="button"
              class="app-icon-button app-hover-inverse-muted"
              style="color: var(--color-text-inverse);"
              @click="isModalOpen = false"
            >
              <X class="h-4 w-4" />
            </button>
          </div>

          <div class="max-h-[76vh] overflow-y-auto p-6">
            <div v-if="!chart?.enabled || chart.type === 'NONE'" class="app-empty py-16">
              {{ chart?.reason || '추천된 차트가 없습니다.' }}
            </div>

            <div v-else-if="chart.type === 'TABLE'" class="space-y-5">
              <div class="flex items-start gap-3 rounded-lg border p-5" style="border-color: var(--color-border-muted); background-color: var(--color-surface-muted);">
                <Table2 class="mt-0.5 h-5 w-5 app-table-muted" />
                <div>
                  <p class="text-sm app-table-main">표 형식이 가장 적합합니다.</p>
                  <p class="mt-1 text-xs leading-5 app-table-muted">{{ chart.reason || '목록 또는 상세 데이터는 그래프보다 표로 확인하는 편이 정확합니다.' }}</p>
                </div>
              </div>
              <div class="overflow-auto rounded-lg border" style="border-color: var(--color-border);">
                <table class="w-full min-w-[720px] text-xs">
                  <thead style="background-color: var(--color-surface-muted);">
                    <tr>
                      <th v-for="column in columns" :key="column" class="px-3 py-2 text-left app-table-muted">
                        {{ column }}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, rowIndex) in modalRows" :key="rowIndex" class="border-t" style="border-color: var(--color-border-muted);">
                      <td v-for="column in columns" :key="column" class="px-3 py-2 app-table-main">
                        {{ formatCell(row[column]) }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div v-else-if="chart.type === 'STAT'" class="app-stat-row-card">
              <div class="app-stat-icon app-stat-icon-primary">
                <BarChart3 class="h-7 w-7" />
              </div>
              <div>
                <p class="app-stat-label">{{ primaryYKey || '합계' }}</p>
                <p class="text-5xl app-table-main">{{ statValue.toLocaleString() }}</p>
              </div>
            </div>

            <div v-else-if="chart.type === 'BAR'" class="space-y-4">
              <div v-for="point in points" :key="point.label" class="grid grid-cols-[12rem_1fr_7rem] items-center gap-4 text-sm">
                <span class="truncate app-table-strong">{{ point.label }}</span>
                <div class="h-9 rounded app-bg-muted">
                  <div
                    class="h-9 rounded"
                    style="background-color: var(--color-primary);"
                    :style="{ width: maxValue > 0 ? `${Math.max((point.value / maxValue) * 100, 2)}%` : '0%' }"
                  ></div>
                </div>
                <span class="text-right tabular-nums app-table-number">{{ point.value.toLocaleString() }}</span>
              </div>
            </div>

            <div v-else-if="chart.type === 'DONUT'" class="grid grid-cols-[18rem_1fr] items-center gap-8">
              <svg viewBox="0 0 42 42" class="h-72 w-72 -rotate-90">
                <circle cx="21" cy="21" r="15.915" fill="transparent" stroke="var(--color-border-muted)" stroke-width="5" />
                <circle
                  v-for="segment in donutSegments"
                  :key="segment.label"
                  cx="21"
                  cy="21"
                  r="15.915"
                  fill="transparent"
                  :stroke="segment.color"
                  stroke-width="5"
                  :stroke-dasharray="segment.dashArray"
                  :stroke-dashoffset="segment.dashOffset"
                />
              </svg>
              <div class="space-y-3">
                <div v-for="segment in donutSegments" :key="segment.label" class="flex items-center justify-between gap-4 text-sm">
                  <span class="flex min-w-0 items-center gap-2">
                    <span class="h-3 w-3 shrink-0 rounded-full" :style="{ backgroundColor: segment.color }"></span>
                    <span class="truncate app-table-strong">{{ segment.label }}</span>
                  </span>
                  <span class="tabular-nums app-table-number">{{ segment.value.toLocaleString() }} ({{ segment.percent.toFixed(1) }}%)</span>
                </div>
              </div>
            </div>

            <div v-else-if="chart.type === 'LINE'" class="space-y-5">
              <svg viewBox="0 0 520 180" class="h-96 w-full overflow-visible">
                <path
                  :d="linePath"
                  fill="none"
                  stroke="var(--color-primary)"
                  stroke-width="3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              <div class="flex justify-between gap-2 text-xs app-table-muted">
                <span v-for="point in points" :key="point.label" class="max-w-28 truncate">{{ point.label }}</span>
              </div>
            </div>

            <div v-else class="app-empty py-16">
              {{ chart.type }} 차트는 표 형태로 확인해 주세요.
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
