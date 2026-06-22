<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown, Code2 } from '@lucide/vue'

defineProps<{
  sql?: string
}>()

const isOpen = ref(false)
</script>

<template>
  <div class="app-panel">
    <button
      type="button"
      class="app-panel-head w-full"
      @click="isOpen = !isOpen"
    >
      <h3 class="app-panel-title">
        <Code2 class="app-panel-icon" />
        생성 SQL
      </h3>
      <ChevronDown
        class="h-4 w-4 transition"
        :class="{ 'rotate-180': isOpen }"
      />
    </button>

    <div v-if="isOpen" class="border-t p-5" style="border-color: var(--color-border-muted);">
      <pre class="overflow-x-auto rounded-lg p-4 text-xs leading-6" style="background-color: var(--color-surface-strong); color: var(--color-text-inverse);">{{ sql || '생성된 SQL이 없습니다.' }}</pre>
    </div>
  </div>
</template>
