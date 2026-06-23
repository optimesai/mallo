<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
}>()
</script>

<template>
  <div class="app-news-card">
    <div>
      <span class="app-news-label">{{ label }}</span>
      <span class="app-news-value app-text-strong">{{ value }}</span>
    </div>
  </div>
</template>
