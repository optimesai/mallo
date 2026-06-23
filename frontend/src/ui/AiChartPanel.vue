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

interface LinePoint extends ChartPoint {
  x: number
  y: number
  shortLabel: string
}

interface StackedPoint {
  label: string
  total: number
  segments: {
    key: string
    label: string
    value: number
    percent: number
    color: string
  }[]
}

const primaryYKey = computed(() => props.chart?.yKeys?.[0])
const isModalOpen = ref(false)
const donutColors = ['#2563eb', '#16a34a', '#f97316', '#dc2626', '#7c3aed', '#0891b2', '#64748b', '#ca8a04']
const lineChart = {
  width: 620,
  height: 280,
  left: 68,
  right: 28,
  top: 34,
  bottom: 64
}

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
  const xKey = props.chart?.labelKey || props.chart?.xKey
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
const lineMinValue = computed(() => Math.min(0, ...points.value.map(point => point.value)))
const lineMaxValue = computed(() => Math.max(0, ...points.value.map(point => point.value)))
const lineValueRange = computed(() => Math.max(lineMaxValue.value - lineMinValue.value, 1))
const lineInnerWidth = lineChart.width - lineChart.left - lineChart.right
const lineInnerHeight = lineChart.height - lineChart.top - lineChart.bottom

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
  if (linePoints.value.length === 0) return ''
  return linePoints.value
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`)
    .join(' ')
})

const areaPath = computed(() => {
  if (linePoints.value.length === 0 || !linePath.value) return ''
  const firstPoint = linePoints.value[0]
  const lastPoint = linePoints.value[linePoints.value.length - 1]
  const baseline = lineChart.top + lineInnerHeight
  return `${linePath.value} L ${lastPoint.x.toFixed(1)} ${baseline} L ${firstPoint.x.toFixed(1)} ${baseline} Z`
})

const linePoints = computed<LinePoint[]>(() => {
  if (points.value.length === 0) return []
  const gap = points.value.length === 1 ? 0 : lineInnerWidth / (points.value.length - 1)

  return points.value.map((point, index) => {
    const x = points.value.length === 1 ? lineChart.left + lineInnerWidth / 2 : lineChart.left + gap * index
    const y = lineChart.top + ((lineMaxValue.value - point.value) / lineValueRange.value) * lineInnerHeight
    return {
      ...point,
      x,
      y,
      shortLabel: formatAxisLabel(point.label)
    }
  })
})

const lineYTicks = computed(() => {
  const max = lineMaxValue.value
  const min = lineMinValue.value
  const middle = min + (max - min) / 2
  return [max, middle, min].map(value => ({
    value,
    y: lineChart.top + ((max - value) / lineValueRange.value) * lineInnerHeight
  }))
})

const statValue = computed(() => {
  if (points.value.length > 0) {
    return points.value.reduce((sum, point) => sum + point.value, 0)
  }
  const yKey = primaryYKey.value
  if (!yKey) return 0
  return props.rows.reduce((sum, row) => sum + toNumber(row[yKey]), 0)
})

const stackedPoints = computed<StackedPoint[]>(() => {
  const xKey = props.chart?.labelKey || props.chart?.xKey
  const yKeys = props.chart?.yKeys ?? []
  if (!xKey || yKeys.length === 0) return []

  return props.rows.slice(0, 10).map(row => {
    const values = yKeys.map((key, index) => ({
      key,
      label: formatMetricLabel(key),
      value: Math.max(toNumber(row[key]), 0),
      color: donutColors[index % donutColors.length]
    }))
    const total = values.reduce((sum, item) => sum + item.value, 0)
    return {
      label: formatLabel(row[xKey]),
      total,
      segments: values.map(item => ({
        ...item,
        percent: total > 0 ? (item.value / total) * 100 : 0
      }))
    }
  })
})

const paretoPoints = computed(() => {
  const total = points.value.reduce((sum, point) => sum + Math.max(point.value, 0), 0)
  let cumulative = 0
  return points.value.map(point => {
    cumulative += Math.max(point.value, 0)
    return {
      ...point,
      cumulativePercent: total > 0 ? (cumulative / total) * 100 : 0
    }
  })
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

function formatAxisLabel(value: unknown) {
  const label = formatLabel(value)
  const normalized = label.includes('T') ? label.split('T')[0] : label
  return normalized.length > 12 ? `${normalized.slice(0, 12)}...` : normalized
}

function formatMultilineLabel(value: unknown) {
  return formatLabel(value).split('\n')
}

function formatMetricLabel(key?: string) {
  if (!key) return '값'
  return props.chart?.yLabels?.[key] || key
}

function formatXLabel() {
  return props.chart?.xLabel || props.chart?.xKey || 'x축'
}

function barWidth(value: number, max: number) {
  return max > 0 ? `${Math.max((value / max) * 100, 2)}%` : '0%'
}

function formatChartValue(value: number) {
  return Number.isInteger(value) ? value.toLocaleString() : value.toLocaleString(undefined, { maximumFractionDigits: 2 })
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
            <p class="app-type-sm app-table-main">표 형식이 가장 적합합니다.</p>
            <p class="mt-1 app-type-xs leading-5 app-table-muted">{{ chart.reason || '목록 또는 상세 데이터는 그래프보다 표로 확인하는 편이 정확합니다.' }}</p>
          </div>
        </div>
        <div class="overflow-hidden rounded-lg border" style="border-color: var(--color-border);">
          <table class="w-full app-type-xs">
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
          <p class="app-stat-label">{{ formatMetricLabel(primaryYKey) }}</p>
          <p class="app-stat-value">{{ statValue.toLocaleString() }}</p>
        </div>
      </div>

      <div v-else-if="chart.type === 'BAR' || chart.type === 'HORIZONTAL_BAR'" class="space-y-3">
        <div v-for="point in points" :key="point.label" class="grid grid-cols-[8rem_1fr_5rem] items-center gap-3 app-type-xs">
          <span class="whitespace-pre-line break-words app-table-strong">{{ point.label }}</span>
          <div class="h-6 rounded app-bg-muted">
            <div
              class="h-6 rounded"
              style="background-color: var(--color-primary);"
              :style="{ width: barWidth(point.value, maxValue) }"
            ></div>
          </div>
          <span class="text-right tabular-nums app-table-number">{{ point.value.toLocaleString() }}</span>
        </div>
      </div>

      <div v-else-if="chart.type === 'STACKED_BAR'" class="space-y-3">
        <div v-for="point in stackedPoints" :key="point.label" class="grid grid-cols-[8rem_1fr_5rem] items-center gap-3 app-type-xs">
          <span class="whitespace-pre-line break-words app-table-strong">{{ point.label }}</span>
          <div class="flex h-6 overflow-hidden rounded app-bg-muted">
            <div
              v-for="segment in point.segments"
              :key="segment.key"
              class="h-6"
              :style="{ width: `${segment.percent}%`, backgroundColor: segment.color }"
            ></div>
          </div>
          <span class="text-right tabular-nums app-table-number">{{ point.total.toLocaleString() }}</span>
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
          <div v-for="segment in donutSegments.slice(0, 6)" :key="segment.label" class="flex items-center justify-between gap-3 app-type-xs">
            <span class="flex min-w-0 items-center gap-2">
              <span class="h-2.5 w-2.5 shrink-0 rounded-full" :style="{ backgroundColor: segment.color }"></span>
              <span class="truncate app-table-strong">{{ segment.label }}</span>
            </span>
            <span class="tabular-nums app-table-number">{{ segment.percent.toFixed(1) }}%</span>
          </div>
        </div>
      </div>

      <div v-else-if="chart.type === 'LINE' || chart.type === 'AREA' || chart.type === 'COMBO'" class="space-y-4">
        <svg :viewBox="`0 0 ${lineChart.width} ${lineChart.height}`" class="h-64 w-full overflow-visible">
          <g class="app-table-muted" font-size="11">
            <line
              :x1="lineChart.left"
              :y1="lineChart.top"
              :x2="lineChart.left"
              :y2="lineChart.top + lineInnerHeight"
              stroke="var(--color-border)"
            />
            <line
              :x1="lineChart.left"
              :y1="lineChart.top + lineInnerHeight"
              :x2="lineChart.left + lineInnerWidth"
              :y2="lineChart.top + lineInnerHeight"
              stroke="var(--color-border)"
            />
            <g v-for="(tick, tickIndex) in lineYTicks" :key="tickIndex">
              <line
                :x1="lineChart.left"
                :y1="tick.y"
                :x2="lineChart.left + lineInnerWidth"
                :y2="tick.y"
                stroke="var(--color-border-muted)"
                stroke-dasharray="4 4"
              />
              <text :x="lineChart.left - 10" :y="tick.y + 4" text-anchor="end" fill="currentColor">
                {{ formatChartValue(tick.value) }}
              </text>
            </g>
            <text
              :x="lineChart.left + lineInnerWidth / 2"
              :y="lineChart.height - 12"
              text-anchor="middle"
              fill="currentColor"
            >
              {{ formatXLabel() }}
            </text>
            <text
              :x="18"
              :y="lineChart.top + lineInnerHeight / 2"
              text-anchor="middle"
              fill="currentColor"
              transform="rotate(-90 18 125)"
            >
              {{ formatMetricLabel(primaryYKey) }}
            </text>
          </g>
          <path
            v-if="chart.type === 'AREA'"
            :d="areaPath"
            fill="var(--color-primary)"
            opacity="0.16"
          />
          <g v-if="chart.type === 'COMBO'">
            <rect
              v-for="point in linePoints"
              :key="`bar-${point.label}`"
              :x="point.x - 10"
              :y="point.y"
              width="20"
              :height="lineChart.top + lineInnerHeight - point.y"
              rx="3"
              fill="var(--color-primary)"
              opacity="0.25"
            />
          </g>
          <path
            :d="linePath"
            fill="none"
            stroke="var(--color-primary)"
            stroke-width="3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <g v-for="point in linePoints" :key="point.label">
            <circle :cx="point.x" :cy="point.y" r="4" fill="var(--color-primary)" stroke="var(--color-surface)" stroke-width="2" />
            <text :x="point.x" :y="point.y - 10" text-anchor="middle" class="app-table-number" font-size="11" font-weight="700">
              {{ formatChartValue(point.value) }}
            </text>
            <text :x="point.x" :y="lineChart.top + lineInnerHeight + 20" text-anchor="middle" class="app-table-muted" font-size="10">
              {{ point.shortLabel }}
            </text>
          </g>
        </svg>
      </div>

      <div v-else-if="chart.type === 'PARETO'" class="space-y-3">
        <div v-for="point in paretoPoints" :key="point.label" class="grid grid-cols-[8rem_1fr_5rem] items-center gap-3 app-type-xs">
          <span class="whitespace-pre-line break-words app-table-strong">{{ point.label }}</span>
          <div class="h-6 rounded app-bg-muted">
            <div
              class="h-6 rounded"
              style="background-color: var(--color-primary);"
              :style="{ width: barWidth(point.value, maxValue) }"
            ></div>
          </div>
          <span class="text-right tabular-nums app-table-number">{{ point.cumulativePercent.toFixed(1) }}%</span>
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
            <h3 class="flex items-center gap-2 app-type-sm font-bold">
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
                  <p class="app-type-sm app-table-main">표 형식이 가장 적합합니다.</p>
                  <p class="mt-1 app-type-xs leading-5 app-table-muted">{{ chart.reason || '목록 또는 상세 데이터는 그래프보다 표로 확인하는 편이 정확합니다.' }}</p>
                </div>
              </div>
              <div class="overflow-auto rounded-lg border" style="border-color: var(--color-border);">
                <table class="w-full min-w-[720px] app-type-xs">
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
	                <p class="app-stat-label">{{ formatMetricLabel(primaryYKey) }}</p>
                <p class="app-type-5xl app-table-main">{{ statValue.toLocaleString() }}</p>
              </div>
            </div>

	            <div v-else-if="chart.type === 'BAR' || chart.type === 'HORIZONTAL_BAR'" class="space-y-4">
	              <div v-for="point in points" :key="point.label" class="grid grid-cols-[12rem_1fr_7rem] items-center gap-4 app-type-sm">
	                <span class="whitespace-pre-line break-words app-table-strong">{{ point.label }}</span>
	                <div class="h-9 rounded app-bg-muted">
	                  <div
	                    class="h-9 rounded"
	                    style="background-color: var(--color-primary);"
	                    :style="{ width: barWidth(point.value, maxValue) }"
	                  ></div>
	                </div>
	                <span class="text-right tabular-nums app-table-number">{{ point.value.toLocaleString() }}</span>
	              </div>
	            </div>

	            <div v-else-if="chart.type === 'STACKED_BAR'" class="space-y-4">
	              <div class="flex flex-wrap gap-3 app-type-xs">
	                <span v-for="segment in stackedPoints[0]?.segments ?? []" :key="segment.key" class="flex items-center gap-2 app-table-muted">
	                  <span class="h-2.5 w-2.5 rounded-full" :style="{ backgroundColor: segment.color }"></span>
	                  {{ segment.label }}
	                </span>
	              </div>
	              <div v-for="point in stackedPoints" :key="point.label" class="grid grid-cols-[12rem_1fr_7rem] items-center gap-4 app-type-sm">
	                <span class="whitespace-pre-line break-words app-table-strong">{{ point.label }}</span>
	                <div class="flex h-9 overflow-hidden rounded app-bg-muted">
	                  <div
	                    v-for="segment in point.segments"
	                    :key="segment.key"
	                    class="h-9"
	                    :title="`${segment.label}: ${formatChartValue(segment.value)}`"
	                    :style="{ width: `${segment.percent}%`, backgroundColor: segment.color }"
	                  ></div>
	                </div>
	                <span class="text-right tabular-nums app-table-number">{{ point.total.toLocaleString() }}</span>
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
                <div v-for="segment in donutSegments" :key="segment.label" class="flex items-center justify-between gap-4 app-type-sm">
                  <span class="flex min-w-0 items-center gap-2">
                    <span class="h-3 w-3 shrink-0 rounded-full" :style="{ backgroundColor: segment.color }"></span>
                    <span class="truncate app-table-strong">{{ segment.label }}</span>
                  </span>
                  <span class="tabular-nums app-table-number">{{ segment.value.toLocaleString() }} ({{ segment.percent.toFixed(1) }}%)</span>
                </div>
              </div>
            </div>

	            <div v-else-if="chart.type === 'LINE' || chart.type === 'AREA' || chart.type === 'COMBO'" class="space-y-5">
	              <svg :viewBox="`0 0 ${lineChart.width} ${lineChart.height}`" class="h-96 w-full overflow-visible">
                <g class="app-table-muted" font-size="11">
                  <line
                    :x1="lineChart.left"
                    :y1="lineChart.top"
                    :x2="lineChart.left"
                    :y2="lineChart.top + lineInnerHeight"
                    stroke="var(--color-border)"
                  />
                  <line
                    :x1="lineChart.left"
                    :y1="lineChart.top + lineInnerHeight"
                    :x2="lineChart.left + lineInnerWidth"
                    :y2="lineChart.top + lineInnerHeight"
                    stroke="var(--color-border)"
                  />
                  <g v-for="(tick, tickIndex) in lineYTicks" :key="tickIndex">
                    <line
                      :x1="lineChart.left"
                      :y1="tick.y"
                      :x2="lineChart.left + lineInnerWidth"
                      :y2="tick.y"
                      stroke="var(--color-border-muted)"
                      stroke-dasharray="4 4"
                    />
                    <text :x="lineChart.left - 10" :y="tick.y + 4" text-anchor="end" fill="currentColor">
                      {{ formatChartValue(tick.value) }}
                    </text>
                  </g>
                  <text
                    :x="lineChart.left + lineInnerWidth / 2"
                    :y="lineChart.height - 12"
                    text-anchor="middle"
                    fill="currentColor"
                  >
	                    {{ formatXLabel() }}
                  </text>
                  <text
                    :x="18"
                    :y="lineChart.top + lineInnerHeight / 2"
                    text-anchor="middle"
                    fill="currentColor"
                    transform="rotate(-90 18 125)"
                  >
	                    {{ formatMetricLabel(primaryYKey) }}
	                  </text>
	                </g>
	                <path
	                  v-if="chart.type === 'AREA'"
	                  :d="areaPath"
	                  fill="var(--color-primary)"
	                  opacity="0.16"
	                />
	                <g v-if="chart.type === 'COMBO'">
	                  <rect
	                    v-for="point in linePoints"
	                    :key="`modal-bar-${point.label}`"
	                    :x="point.x - 12"
	                    :y="point.y"
	                    width="24"
	                    :height="lineChart.top + lineInnerHeight - point.y"
	                    rx="4"
	                    fill="var(--color-primary)"
	                    opacity="0.25"
	                  />
	                </g>
	                <path
	                  :d="linePath"
                  fill="none"
                  stroke="var(--color-primary)"
                  stroke-width="3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <g v-for="point in linePoints" :key="point.label">
                  <circle :cx="point.x" :cy="point.y" r="4" fill="var(--color-primary)" stroke="var(--color-surface)" stroke-width="2" />
                  <text :x="point.x" :y="point.y - 10" text-anchor="middle" class="app-table-number" font-size="11" font-weight="700">
                    {{ formatChartValue(point.value) }}
                  </text>
                  <text :x="point.x" :y="lineChart.top + lineInnerHeight + 20" text-anchor="middle" class="app-table-muted" font-size="10">
                    {{ point.shortLabel }}
                  </text>
                </g>
	              </svg>
	            </div>

	            <div v-else-if="chart.type === 'PARETO'" class="space-y-4">
	              <div v-for="point in paretoPoints" :key="point.label" class="grid grid-cols-[12rem_1fr_7rem] items-center gap-4 app-type-sm">
	                <span class="whitespace-pre-line break-words app-table-strong">{{ point.label }}</span>
	                <div class="h-9 rounded app-bg-muted">
	                  <div
	                    class="h-9 rounded"
	                    style="background-color: var(--color-primary);"
	                    :style="{ width: barWidth(point.value, maxValue) }"
	                  ></div>
	                </div>
	                <span class="text-right tabular-nums app-table-number">{{ point.cumulativePercent.toFixed(1) }}%</span>
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
