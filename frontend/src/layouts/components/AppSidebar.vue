<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { menuGroups } from '@/router/navigation'
</script>

<template>
  <aside class="w-64 bg-slate-900 text-slate-300 flex flex-col shrink-0">
    <div class="h-16 flex items-center px-6 border-b border-slate-800 gap-2 shrink-0">
      <svg class="w-6 h-6 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
      <span class="font-bold text-white tracking-wider text-sm">Logistics Control</span>
    </div>

    <nav class="flex-grow p-4 space-y-6 overflow-y-auto max-h-[calc(100vh-8rem)]">
      <div v-for="group in menuGroups" :key="group.category" class="space-y-1">
        <div class="flex items-center gap-2 px-2 py-1 text-slate-500">
          <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" :d="group.iconPath" />
          </svg>
          <span class="text-xs font-bold uppercase tracking-wider">{{ group.category }}</span>
        </div>
        <div class="space-y-0.5 pl-6">
          <RouterLink
            v-for="item in group.items"
            :key="item.name"
            :to="item.path"
            class="flex items-center px-3 py-1.5 rounded-md hover:bg-slate-800 hover:text-slate-100 text-xs transition-colors"
          >
            {{ item.name }}
          </RouterLink>
        </div>
      </div>
    </nav>

    <div class="p-4 border-t border-slate-800 text-xs text-slate-500 shrink-0">
      <div>System Version: v0.0.0</div>
    </div>
  </aside>
</template>
