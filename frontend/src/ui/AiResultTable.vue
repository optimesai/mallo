<script setup lang="ts">
import { computed } from 'vue'
import { Table2 } from '@lucide/vue'

const props = defineProps<{
  rows: Record<string, unknown>[]
}>()

const columns = computed(() => {
  const keys = new Set<string>()
  props.rows.forEach(row => {
    Object.keys(row).forEach(key => keys.add(key))
  })
  return Array.from(keys)
})

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
    <div class="app-panel-head">
      <h3 class="app-panel-title">
        <Table2 class="app-panel-icon" />
        조회 결과
      </h3>
      <span class="app-type-xs app-table-muted">{{ rows.length.toLocaleString() }}건</span>
    </div>

    <div class="app-table-wrap">
      <table class="app-table">
        <thead>
          <tr>
            <th v-for="column in columns" :key="column">
              {{ column }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, rowIndex) in rows" :key="rowIndex">
            <td v-for="column in columns" :key="column">
              {{ formatCell(row[column]) }}
            </td>
          </tr>
          <tr v-if="rows.length === 0">
            <td :colspan="Math.max(columns.length, 1)" class="app-empty">
              조회된 데이터가 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
