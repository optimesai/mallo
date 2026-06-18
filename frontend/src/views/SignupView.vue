<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authService } from '@/services/authService'

const router = useRouter()

const employeeNo = ref('')
const userName = ref('')
const department = ref('')
const password = ref('')
const passwordConfirm = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)
const isCheckingEmployeeNo = ref(false)
const isEmployeeNoDuplicated = ref(false)
const employeeNoCheckMessage = ref('중복확인 버튼 또는 Enter로 사번 중복 여부를 확인해 주세요.')
const employeeNoCheckStatus = ref<'idle' | 'checking' | 'available' | 'duplicated' | 'error' | 'required'>('idle')
const checkedEmployeeNo = ref('')

let employeeNoCheckSequence = 0

function handleEmployeeNoInput() {
  isEmployeeNoDuplicated.value = false
  checkedEmployeeNo.value = ''
  employeeNoCheckStatus.value = employeeNo.value.trim() ? 'idle' : 'idle'
  employeeNoCheckMessage.value = '중복확인 버튼 또는 Enter로 사번 중복 여부를 확인해 주세요.'
}

async function checkEmployeeNo(targetEmployeeNo: string) {
  const sequence = ++employeeNoCheckSequence

  try {
    const exists = await authService.existsByEmployeeNo(targetEmployeeNo)
    if (sequence !== employeeNoCheckSequence || employeeNo.value.trim() !== targetEmployeeNo) return

    isEmployeeNoDuplicated.value = exists
    checkedEmployeeNo.value = exists ? '' : targetEmployeeNo
    employeeNoCheckStatus.value = exists ? 'duplicated' : 'available'
    employeeNoCheckMessage.value = exists
      ? '이미 사용 중인 사번입니다.'
      : '사용 가능한 사번입니다.'
  } catch (error) {
    if (sequence !== employeeNoCheckSequence) return
    isEmployeeNoDuplicated.value = false
    checkedEmployeeNo.value = ''
    employeeNoCheckStatus.value = 'error'
    employeeNoCheckMessage.value = error instanceof Error ? error.message : '사번 중복 여부를 확인하지 못했습니다.'
  } finally {
    if (sequence === employeeNoCheckSequence) {
      isCheckingEmployeeNo.value = false
    }
  }
}

async function handleEmployeeNoCheck() {
  errorMessage.value = ''

  const trimmedEmployeeNo = employeeNo.value.trim()
  if (!trimmedEmployeeNo) {
    checkedEmployeeNo.value = ''
    isEmployeeNoDuplicated.value = false
    employeeNoCheckStatus.value = 'required'
    employeeNoCheckMessage.value = '사번을 입력한 뒤 중복확인을 진행해 주세요.'
    return
  }

  employeeNoCheckMessage.value = '사번 중복 여부를 확인하는 중입니다.'
  employeeNoCheckStatus.value = 'checking'
  isCheckingEmployeeNo.value = true
  await checkEmployeeNo(trimmedEmployeeNo)
}

async function handleSubmit() {
  errorMessage.value = ''

  if (isCheckingEmployeeNo.value) {
    errorMessage.value = '사번 중복 확인이 끝난 뒤 다시 시도해 주세요.'
    return
  }

  if (isEmployeeNoDuplicated.value) {
    errorMessage.value = '이미 사용 중인 사번입니다.'
    return
  }

  if (!checkedEmployeeNo.value || checkedEmployeeNo.value !== employeeNo.value.trim()) {
    employeeNoCheckStatus.value = 'required'
    employeeNoCheckMessage.value = '회원가입 전 사번 중복확인을 진행해 주세요.'
    errorMessage.value = '사번 중복확인을 진행해 주세요.'
    return
  }

  if (password.value.length < 8) {
    errorMessage.value = '비밀번호는 8자 이상 입력해 주세요.'
    return
  }

  if (password.value !== passwordConfirm.value) {
    errorMessage.value = '비밀번호 확인이 일치하지 않습니다.'
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
      <div class="auth-brand">
        <div class="auth-logo-box">
          <span class="auth-logo-mark"></span>
        </div>
        <div>
          <h1 id="signup-title" class="auth-title">
            회원가입
          </h1>
          <p class="auth-subtitle">
            물류·생산 관리 시스템 접근을 위한 계정 정보를 등록합니다.
          </p>
        </div>
      </div>

      <div class="auth-divider"></div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <div class="auth-field">
          <label class="auth-label" for="employee-no">사번</label>
          <div class="flex gap-2">
            <input
              id="employee-no"
              v-model="employeeNo"
              class="auth-input"
              name="employeeNo"
              placeholder="EMP001"
              autocomplete="username"
              maxlength="50"
              required
              @input="handleEmployeeNoInput"
              @keydown.enter.prevent="handleEmployeeNoCheck"
            >
            <button
              class="app-button app-button-primary h-11 shrink-0 disabled:opacity-70"
              type="button"
              :disabled="isCheckingEmployeeNo"
              @click="handleEmployeeNoCheck"
            >
              중복확인
            </button>
          </div>
          <p
            class="auth-help"
            :class="{
              'app-text-danger': employeeNoCheckStatus === 'duplicated' || employeeNoCheckStatus === 'error' || employeeNoCheckStatus === 'required',
              'app-text-success': employeeNoCheckStatus === 'available'
            }"
          >
            {{ employeeNoCheckMessage }}
          </p>
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
        </div>

        <div class="auth-field">
          <label class="auth-label" for="signup-password-confirm">비밀번호 확인</label>
          <input
            id="signup-password-confirm"
            v-model="passwordConfirm"
            class="auth-input"
            type="password"
            name="passwordConfirm"
            autocomplete="new-password"
            minlength="8"
            maxlength="100"
            required
          >
          <p class="auth-help">동일한 비밀번호를 한 번 더 입력해 주세요</p>
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
