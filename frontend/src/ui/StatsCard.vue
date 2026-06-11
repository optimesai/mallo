<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
}>()
</script>

<template>
  <div class="app-stat-card">
    <span class="app-stat-label">{{ label }}</span>
    <span class="app-stat-value">{{ value }}</span>
  </div>
</template>
