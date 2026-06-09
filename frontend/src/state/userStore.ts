import { defineStore } from 'pinia'
import { ref } from 'vue'
import { userService } from '@/services/userService'
import type { UserResponse } from '@/api/authApi'
import type { UserRole } from '@/api/userApi'

export const useUserStore = defineStore('user', () => {
  const users = ref<UserResponse[]>([])
  const selectedUser = ref<UserResponse | null>(null)
  const isLoading = ref(false)
  const isSaving = ref(false)
  const error = ref<string | null>(null)

  async function loadUsers() {
    isLoading.value = true
    error.value = null
    try {
      users.value = await userService.getUsers()
    } catch (err) {
      error.value = err instanceof Error ? err.message : '사용자 목록을 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function loadUser(userId: number) {
    isLoading.value = true
    error.value = null
    try {
      selectedUser.value = await userService.getUser(userId)
      return selectedUser.value
    } catch (err) {
      error.value = err instanceof Error ? err.message : '사용자 상세 정보를 불러오지 못했습니다.'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  async function updateUserRole(userId: number, role: UserRole) {
    isSaving.value = true
    error.value = null
    try {
      const updated = await userService.updateUserRole(userId, { role })
      const index = users.value.findIndex((user) => user.userId === userId)
      if (index !== -1) users.value[index] = updated
      selectedUser.value = updated
      return updated
    } catch (err) {
      error.value = err instanceof Error ? err.message : '사용자 권한을 수정하지 못했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  async function deleteUser(userId: number) {
    isSaving.value = true
    error.value = null
    try {
      await userService.deleteUser(userId)
      users.value = users.value.filter((user) => user.userId !== userId)
      if (selectedUser.value?.userId === userId) selectedUser.value = null
    } catch (err) {
      error.value = err instanceof Error ? err.message : '사용자를 삭제하지 못했습니다.'
      throw err
    } finally {
      isSaving.value = false
    }
  }

  return {
    users,
    selectedUser,
    isLoading,
    isSaving,
    error,
    loadUsers,
    loadUser,
    updateUserRole,
    deleteUser
  }
})
