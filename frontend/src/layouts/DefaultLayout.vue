<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'
</script>

<template>
  <div class="min-h-screen bg-slate-100 flex font-sans">
    <!-- Modular Sidebar -->
    <AppSidebar />

    <!-- Main Container -->
    <div class="flex-grow flex flex-col min-w-0">
      <!-- Modular Header -->
      <AppHeader />

      <!-- Content Area (라우터의 자식 컴포넌트들이 위치할 공간) -->
      <main class="flex-grow p-8 overflow-y-auto">
        <RouterView />
      </main>
    </div>
  </div>
</template>
