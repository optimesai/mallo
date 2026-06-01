import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { authService } from '@/services/authService'
import type { UserResponse } from '@/api/authApi'

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'
const AUTH_USER_KEY = 'ssafy-pjt-auth-user'

function loadStoredUser() {
  const storedUser = localStorage.getItem(AUTH_USER_KEY)

  if (!storedUser) {
    return null
  }

  try {
    return JSON.parse(storedUser) as UserResponse
  } catch {
    localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(AUTH_TOKEN_KEY))
  const user = ref<UserResponse | null>(loadStoredUser())

  const employeeId = computed(() => user.value?.employeeNo ?? null)

  const isLoggedIn = computed(() => Boolean(accessToken.value))

  async function login(inputEmployeeId: string, inputPassword: string) {
    const response = await authService.login({
      employeeNo: inputEmployeeId.trim(),
      password: inputPassword
    })

    const loginData = response.data

    accessToken.value = loginData.token.accessToken
    user.value = loginData.user
    localStorage.setItem(AUTH_TOKEN_KEY, loginData.token.accessToken)
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(loginData.user))
  }

  function logout() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USER_KEY)
  }

  function setUser(nextUser: UserResponse) {
    user.value = nextUser
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser))
  }

  return {
    accessToken,
    employeeId,
    isLoggedIn,
    user,
    login,
    logout,
    setUser
  }
})
