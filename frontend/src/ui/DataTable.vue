<script setup lang="ts">
interface TableColumn {
  key: string
  label: string
}

defineProps<{
  title: string
  columns: TableColumn[]
  data: Record<string, any>[]
  moreText?: string
}>()

const emit = defineEmits<{
  (e: 'clickMore'): void
}>()
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
      </tbody>
    </table>
  </div>
</template>
