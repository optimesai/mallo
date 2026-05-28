<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
}>()
</script>

<template>
  <div class="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex flex-col justify-between">
    <span class="text-xs font-semibold text-slate-400 uppercase">{{ label }}</span>
    <span class="text-2xl font-bold text-slate-800 mt-2">{{ value }}</span>
  </div>
</template>
