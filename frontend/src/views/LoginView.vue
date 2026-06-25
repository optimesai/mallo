<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/state/authStore'

const router = useRouter()
const authStore = useAuthStore()

const employeeId = ref('')
const password = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)

async function handleSubmit() {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await authStore.login(employeeId.value, password.value)
    await router.push({ name: 'home' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '로그인에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="login-title">
      <div class="auth-brand">
        <div class="auth-logo-box">
          <span class="auth-logo-mark"></span>
        </div>
        <div>
          <h1 id="login-title" class="auth-title">
            MALLO
          </h1>
        </div>
      </div>

      <div class="auth-divider"></div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <div class="auth-field">
          <label class="auth-label" for="employee-id">사번</label>
          <input
            id="employee-id"
            v-model="employeeId"
            class="auth-input"
            name="employeeId"
            autocomplete="username"
            required
          >
        </div>

        <div class="auth-field">
          <label class="auth-label" for="password">비밀번호</label>
          <input
            id="password"
            v-model="password"
            class="auth-input"
            type="password"
            name="password"
            autocomplete="current-password"
            required
          >
        </div>

        <p class="auth-error" role="alert">
          {{ errorMessage }}
        </p>

        <button class="auth-submit" type="submit" :disabled="isSubmitting">
          로그인
        </button>
      </form>

      <RouterLink class="auth-link" to="/signup">
        회원가입 이동
      </RouterLink>
    </section>
  </main>
</template>
