<script setup lang="ts">
import { computed, ref } from 'vue'
import { BarChart3, Maximize2, X } from '@lucide/vue'
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
