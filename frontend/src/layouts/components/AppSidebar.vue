<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { menuGroups } from '@/router/navigation'
import { useAuthStore } from '@/state/authStore'

const authStore = useAuthStore()

const visibleMenuGroups = computed(() => {
  return menuGroups
    .map((group) => {
      if (group.category === '시스템 관리' && !authStore.isAdmin) {
        return { ...group, items: [] }
      }
      return group
    })
    .filter((group) => group.items.length > 0)
})
</script>

<template>
  <aside class="app-sidebar">
    <RouterLink to="/" class="app-sidebar-brand">
      <svg class="app-sidebar-brand-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
      <span class="app-sidebar-brand-text">MALLO</span>
    </RouterLink>

    <nav class="app-sidebar-nav">
      <div v-for="group in visibleMenuGroups" :key="group.category" class="app-sidebar-group">
        <div class="app-sidebar-category">
          <svg class="app-sidebar-category-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" :d="group.iconPath" />
          </svg>
          <span class="app-sidebar-category-text">{{ group.category }}</span>
        </div>
        <div class="app-sidebar-link-list">
          <RouterLink
            v-for="item in group.items"
            :key="item.name"
            :to="item.path"
            class="app-sidebar-link"
          >
            {{ item.name }}
          </RouterLink>
        </div>
      </div>
    </nav>

    <div class="app-sidebar-footer">
      <div>System Version: v0.0.0</div>
    </div>
  </aside>
</template>
