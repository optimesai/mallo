<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'
</script>

<template>
  <div class="app-layout font-sans">
    <!-- Modular Sidebar -->
    <AppSidebar />

    <!-- Main Container -->
    <div class="app-main">
      <!-- Modular Header -->
      <AppHeader />

      <!-- Content Area (라우터의 자식 컴포넌트들이 위치할 공간) -->
      <main class="app-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>
