<script setup lang="ts">
import { onMounted } from 'vue'
import { useMockStore } from '@/state/mockStore'
import StatsCard from '@/ui/StatsCard.vue'
import DataTable from '@/ui/DataTable.vue'

const mockStore = useMockStore()

onMounted(() => {
  mockStore.loadMockData()
})
</script>

<template>
  <div v-if="mockStore.isLoading" class="text-slate-500 text-sm">
    Loading dashboard data...
  </div>
</template>
