<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/state/authStore'
import { useUserStore } from '@/state/userStore'

const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()

const userName = ref('')
const department = ref('')
const password = ref('')
const passwordConfirm = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)

const employeeNo = computed(() => authStore.user?.employeeNo ?? '-')
const role = computed(() => authStore.user?.role ?? '-')

onMounted(async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const user = await userStore.loadMyInfo()
    authStore.setUser(user)
    userName.value = user.userName
    department.value = user.department
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '내 정보를 불러오지 못했습니다.'
  } finally {
    isLoading.value = false
  }
})

async function handleSubmit() {
  errorMessage.value = ''
  successMessage.value = ''

  if (password.value && password.value.length < 8) {
    errorMessage.value = '비밀번호는 8자 이상 입력해 주세요.'
    return
  }

  if (password.value !== passwordConfirm.value) {
    errorMessage.value = '비밀번호 확인이 일치하지 않습니다.'
    return
  }

  isSubmitting.value = true

  try {
    const isPasswordChanged = Boolean(password.value)
    const user = await userStore.updateMyInfo({
      userName: userName.value.trim(),
      department: department.value.trim(),
      ...(password.value ? { password: password.value } : {})
    })

    if (isPasswordChanged) {
      await authStore.logout()
      await router.push({ name: 'login' })
      return
    }

    authStore.setUser(user)
    password.value = ''
    passwordConfirm.value = ''
    successMessage.value = '내 정보가 수정되었습니다.'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '내 정보를 수정하지 못했습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="profile-page">
    <div class="profile-heading">
      <div>
        <h1 class="profile-title">내 정보</h1>
        <p class="profile-subtitle">로그인 계정의 이름, 부서, 비밀번호를 수정합니다.</p>
      </div>
    </div>

    <div class="profile-panel">
      <div v-if="isLoading" class="profile-state">
        내 정보를 불러오는 중입니다.
      </div>

      <form v-else class="profile-form" @submit.prevent="handleSubmit">
        <div class="profile-grid">
          <div class="profile-field">
            <label class="profile-label" for="employee-no">사번</label>
            <input
              id="employee-no"
              class="profile-input"
              :value="employeeNo"
              disabled
            >
          </div>

          <div class="profile-field">
            <label class="profile-label" for="role">권한</label>
            <input
              id="role"
              class="profile-input"
              :value="role"
              disabled
            >
          </div>

          <div class="profile-field">
            <label class="profile-label" for="user-name">이름</label>
            <input
              id="user-name"
              v-model="userName"
              class="profile-input"
              maxlength="50"
              required
            >
          </div>

          <div class="profile-field">
            <label class="profile-label" for="department">부서</label>
            <input
              id="department"
              v-model="department"
              class="profile-input"
              maxlength="100"
              required
            >
          </div>

          <div class="profile-field">
            <label class="profile-label" for="password">새 비밀번호</label>
            <input
              id="password"
              v-model="password"
              class="profile-input"
              type="password"
              minlength="8"
              maxlength="100"
              autocomplete="new-password"
            >
            <p class="profile-help">변경하지 않으려면 비워두세요.</p>
          </div>

          <div class="profile-field">
            <label class="profile-label" for="password-confirm">새 비밀번호 확인</label>
            <input
              id="password-confirm"
              v-model="passwordConfirm"
              class="profile-input"
              type="password"
              minlength="8"
              maxlength="100"
              autocomplete="new-password"
            >
          </div>
        </div>

        <p class="profile-error" role="alert">{{ errorMessage }}</p>
        <p class="profile-success" role="status">{{ successMessage }}</p>

        <div class="profile-actions">
          <button class="profile-submit" type="submit" :disabled="isSubmitting">
            저장
          </button>
        </div>
      </form>
    </div>
  </section>
</template>
