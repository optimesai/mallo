<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authService } from '@/services/authService'

const router = useRouter()

const employeeNo = ref('')
const userName = ref('')
const department = ref('')
const password = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)

async function handleSubmit() {
  errorMessage.value = ''

  if (password.value.length < 8) {
    errorMessage.value = '비밀번호는 8자 이상 입력해 주세요.'
    return
  }

  isSubmitting.value = true

  try {
    await authService.signup({
      employeeNo: employeeNo.value.trim(),
      userName: userName.value.trim(),
      department: department.value.trim(),
      password: password.value
    })

    await router.push({ name: 'login' })
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '회원가입에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel auth-panel-signup" aria-labelledby="signup-title">
      <h1 id="signup-title" class="auth-title">
        회원가입
      </h1>

      <div class="auth-divider"></div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <div class="auth-field">
          <label class="auth-label" for="employee-no">사번</label>
          <input
            id="employee-no"
            v-model="employeeNo"
            class="auth-input"
            name="employeeNo"
            placeholder="EMP001"
            autocomplete="username"
            maxlength="50"
            required
          >
          <p class="auth-help">사번 중복 여부 확인</p>
        </div>

        <div class="auth-field">
          <label class="auth-label" for="user-name">이름</label>
          <input
            id="user-name"
            v-model="userName"
            class="auth-input"
            name="userName"
            maxlength="50"
            required
          >
        </div>

        <div class="auth-field">
          <label class="auth-label" for="department">부서</label>
          <input
            id="department"
            v-model="department"
            class="auth-input"
            name="department"
            maxlength="100"
            required
          >
        </div>

        <div class="auth-field">
          <label class="auth-label" for="signup-password">비밀번호</label>
          <input
            id="signup-password"
            v-model="password"
            class="auth-input"
            type="password"
            name="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="100"
            required
          >
          <p class="auth-help">비밀번호는 암호화되어 저장됩니다</p>
        </div>

        <p class="auth-error" role="alert">
          {{ errorMessage }}
        </p>

        <button class="auth-submit" type="submit" :disabled="isSubmitting">
          회원가입
        </button>
      </form>



      <div class="auth-divider auth-divider-compact"></div>

      <RouterLink class="auth-link" to="/login">
        로그인으로 이동
      </RouterLink>
    </section>
  </main>
</template>
