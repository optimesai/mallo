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
  <header class="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-8 shadow-sm shrink-0">
    <div class="flex items-center gap-4">
      <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
        공장 가동 중 (Normal)
      </span>
    </div>
    <div class="relative flex items-center gap-4">
      <div class="text-sm font-medium text-slate-700">{{ displayName }}님 환영합니다</div>
      <button
        class="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-sm font-bold text-slate-600"
        type="button"
        aria-label="사용자 메뉴"
        @click="toggleMenu"
      >
        {{ userInitials }}
      </button>

      <div
        v-if="isMenuOpen"
        class="absolute right-0 top-11 w-36 overflow-hidden rounded-md border border-slate-200 bg-white shadow-lg"
      >
        <RouterLink
          class="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          to="/me"
          @click="isMenuOpen = false"
        >
          내 정보
        </RouterLink>
        <button
          class="block w-full px-4 py-2 text-left text-sm text-slate-700 hover:bg-slate-50"
          type="button"
          @click="handleLogout"
        >
          로그아웃
        </button>
      </div>
    </div>
  </header>
</template>
