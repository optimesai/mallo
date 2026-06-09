import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { configureAuthClient } from '@/api/client'
import { authService } from '@/services/authService'
import type { LoginResponse, UserResponse } from '@/api/authApi'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<UserResponse | null>(null)
  const isInitialized = ref(false)

  const employeeId = computed(() => user.value?.employeeNo ?? null)
  const isLoggedIn = computed(() => Boolean(accessToken.value))

  configureAuthClient({
    getAccessToken: () => accessToken.value,
    refreshAccessToken,
    handleUnauthorized: clearAuth
  })

  async function login(inputEmployeeId: string, inputPassword: string) {
    const response = await authService.login({
      employeeNo: inputEmployeeId.trim(),
      password: inputPassword
    })

    setAuth(response.data)
    isInitialized.value = true
  }

  async function initializeAuth() {
    if (isInitialized.value) {
      return
    }

    try {
      await refreshAccessToken()
    } finally {
      isInitialized.value = true
    }
  }

  async function refreshAccessToken() {
    try {
      const response = await authService.refresh()
      setAuth(response.data)
      return true
    } catch {
      clearAuth()
      return false
    }
  }

  async function logout() {
    try {
      await authService.logout()
    } finally {
      clearAuth()
      isInitialized.value = true
    }
  }

  function setUser(nextUser: UserResponse) {
    user.value = nextUser
  }

  function setAuth(loginData: LoginResponse) {
    accessToken.value = loginData.token.accessToken
    user.value = loginData.user
  }

  function clearAuth() {
    accessToken.value = null
    user.value = null
  }

  return {
    accessToken,
    employeeId,
    isInitialized,
    isLoggedIn,
    user,
    initializeAuth,
    login,
    logout,
    refreshAccessToken,
    setUser
  }
})
