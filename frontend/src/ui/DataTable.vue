<script setup lang="ts">
import { computed } from 'vue'

interface TableColumn {
  key: string
  label: string
}

const props = defineProps<{
  title: string
  columns: TableColumn[]
  data: Record<string, any>[]
  moreText?: string
  page?: number
  totalPages?: number
  totalElements?: number
  pageSize?: number
}>()

const emit = defineEmits<{
  (e: 'clickMore'): void
  (e: 'pageChange', page: number): void
}>()

function goToPage(page: number) {
  if (page >= 0 && props.totalPages != null && page < props.totalPages) {
    emit('pageChange', page)
  }
}

function pageRange() {
  if (props.totalPages == null || props.totalPages <= 1) return []
  const current = props.page ?? 0
  const total = props.totalPages
  const start = Math.max(0, Math.min(current - 2, total - 5))
  const end = Math.min(total, start + 5)
  const pages: number[] = []
  for (let i = start; i < end; i++) {
    pages.push(i)
  }
  return pages
}

const showPagination = computed(() =>
  props.totalPages != null && props.totalPages > 0 && props.totalElements != null
)
</script>

<template>
  <div class="app-panel">
    <div class="app-panel-head px-6">
      <h3 class="app-panel-title">{{ title }}</h3>
      <span
        v-if="moreText"
        class="app-accent cursor-pointer app-type-xs hover:underline"
        @click="emit('clickMore')"
      >
        {{ moreText }}
      </span>
    </div>
    <table class="app-table">
      <thead>
        <tr>
          <th v-for="col in columns" :key="col.key" class="px-6">
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, idx) in data" :key="idx">
          <td v-for="col in columns" :key="col.key" class="px-6">
            {{ row[col.key] }}
          </td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="columns.length" class="app-empty py-8 app-type-xs">
            데이터가 없습니다.
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Pagination -->
    <div
      v-if="showPagination"
      class="app-pagination px-6"
    >
      <span>
        총 <span class="app-count-strong">{{ totalElements?.toLocaleString() }}</span>건
        ({{ (page ?? 0) + 1 }} / {{ totalPages }} 페이지)
      </span>
      <div class="app-pagination-actions">
        <button
          @click="goToPage(0)"
          :disabled="(page ?? 0) === 0"
          class="app-page-button"
        >
          처음
        </button>
        <button
          @click="goToPage((page ?? 1) - 1)"
          :disabled="(page ?? 0) === 0"
          class="app-page-button"
        >
          이전
        </button>
        <button
          v-for="p in pageRange()"
          :key="p"
          @click="goToPage(p)"
          class="app-page-number"
          :class="{ 'is-active': p === (page ?? 0) }"
        >
          {{ p + 1 }}
        </button>
        <button
          @click="goToPage((page ?? 0) + 1)"
          :disabled="(page ?? 0) >= (totalPages ?? 1) - 1"
          class="app-page-button"
        >
          다음
        </button>
        <button
          @click="goToPage((totalPages ?? 1) - 1)"
          :disabled="(page ?? 0) >= (totalPages ?? 1) - 1"
          class="app-page-button"
        >
          마지막
        </button>
      </div>
    </div>
  </div>
</template>
