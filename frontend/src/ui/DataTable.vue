<script setup lang="ts">
import { ChevronLeft, ChevronRight } from 'lucide-vue'

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

import { computed } from 'vue'
</script>

<template>
  <div class="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="px-6 py-4 border-b border-slate-200 bg-slate-50 flex items-center justify-between">
      <h3 class="font-bold text-slate-700 text-sm">{{ title }}</h3>
      <span
        v-if="moreText"
        class="text-xs text-indigo-600 font-semibold cursor-pointer hover:underline"
        @click="emit('clickMore')"
      >
        {{ moreText }}
      </span>
    </div>
    <table class="w-full text-left text-sm text-slate-600">
      <thead class="bg-slate-100 text-slate-500 text-xs font-semibold uppercase">
        <tr>
          <th v-for="col in columns" :key="col.key" class="px-6 py-3">
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody class="divide-y divide-slate-200">
        <tr v-for="(row, idx) in data" :key="idx">
          <td v-for="col in columns" :key="col.key" class="px-6 py-4">
            {{ row[col.key] }}
          </td>
        </tr>
        <tr v-if="data.length === 0">
          <td :colspan="columns.length" class="px-6 py-8 text-center text-slate-400 text-xs">
            데이터가 없습니다.
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Pagination -->
    <div
      v-if="showPagination"
      class="px-6 py-3 border-t border-slate-200 bg-slate-50 flex items-center justify-between text-xs"
    >
      <span class="text-slate-500">
        총 <span class="font-bold text-slate-700">{{ totalElements?.toLocaleString() }}</span>건
        ({{ (page ?? 0) + 1 }} / {{ totalPages }} 페이지)
      </span>
      <div class="flex items-center gap-1">
        <button
          @click="goToPage(0)"
          :disabled="(page ?? 0) === 0"
          class="px-2 py-1 rounded text-slate-500 hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition"
        >
          <ChevronLeft class="w-3.5 h-3.5" />
          <ChevronLeft class="w-3.5 h-3.5 -ml-2" />
        </button>
        <button
          @click="goToPage((page ?? 1) - 1)"
          :disabled="(page ?? 0) === 0"
          class="px-2 py-1 rounded text-slate-500 hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition"
        >
          <ChevronLeft class="w-3.5 h-3.5" />
        </button>
        <button
          v-for="p in pageRange()"
          :key="p"
          @click="goToPage(p)"
          class="w-7 h-7 rounded text-xs font-bold transition"
          :class="p === (page ?? 0)
            ? 'bg-[#1428A0] text-white shadow-sm'
            : 'text-slate-600 hover:bg-slate-200'"
        >
          {{ p + 1 }}
        </button>
        <button
          @click="goToPage((page ?? 0) + 1)"
          :disabled="(page ?? 0) >= (totalPages ?? 1) - 1"
          class="px-2 py-1 rounded text-slate-500 hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition"
        >
          <ChevronRight class="w-3.5 h-3.5" />
        </button>
        <button
          @click="goToPage((totalPages ?? 1) - 1)"
          :disabled="(page ?? 0) >= (totalPages ?? 1) - 1"
          class="px-2 py-1 rounded text-slate-500 hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition"
        >
          <ChevronRight class="w-3.5 h-3.5" />
          <ChevronRight class="w-3.5 h-3.5 -ml-2" />
        </button>
      </div>
    </div>
  </div>
</template>
