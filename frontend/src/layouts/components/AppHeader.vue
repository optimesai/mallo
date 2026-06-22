<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/state/authStore'

const router = useRouter()
const authStore = useAuthStore()
const isMenuOpen = ref(false)

const displayName = computed(() => authStore.user?.userName || '사용자')
const userInitials = computed(() => displayName.value.slice(0, 2).toUpperCase())

function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value
}

async function handleLogout() {
  await authStore.logout()
  isMenuOpen.value = false
  await router.push({ name: 'login' })
}
</script>

<template>
  <header class="app-header">
    <div class="flex items-center gap-4">
      <span class="app-status app-status-success">
        <span class="app-toast-dot animate-pulse"></span>
        공장 가동 중 (Normal)
      </span>
    </div>
    <div class="relative flex items-center gap-4">
      <div class="app-table-strong app-type-sm">{{ displayName }}님 환영합니다</div>
      <button
        class="app-icon-button app-button-subtle rounded-full app-type-sm"
        type="button"
        aria-label="사용자 메뉴"
        @click="toggleMenu"
      >
        {{ userInitials }}
      </button>

      <div
        v-if="isMenuOpen"
        class="app-modal-shell absolute right-0 top-11 w-36"
      >
        <RouterLink
          class="app-header-menu-item"
          to="/me"
          @click="isMenuOpen = false"
        >
          내 정보
        </RouterLink>
        <button
          class="app-header-menu-item w-full text-left"
          type="button"
          @click="handleLogout"
        >
          로그아웃
        </button>
      </div>
    </div>
  </header>
</template>
