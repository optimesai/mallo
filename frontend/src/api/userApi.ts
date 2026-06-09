import { apiClient } from '@/api/client'
import type { ApiResponse, UserResponse, UserUpdateRequest } from '@/api/authApi'

export type UserRole = 'WORKER' | 'MANAGER' | 'ADMIN'

export interface UserRoleUpdateRequest {
  role: UserRole
}

export const userApi = {
  async getMyInfo() {
    const response = await apiClient.get<ApiResponse<UserResponse>>('/api/users/me')
    return response.data
  },

  async updateMyInfo(request: UserUpdateRequest) {
    const response = await apiClient.patch<ApiResponse<UserResponse>>('/api/users/me', request)
    return response.data
  },

  async getUsers() {
    const response = await apiClient.get<ApiResponse<UserResponse[]>>('/api/users')
    return response.data
  },

  async getUser(userId: number) {
    const response = await apiClient.get<ApiResponse<UserResponse>>(`/api/users/${userId}`)
    return response.data
  },

  async updateUserRole(userId: number, request: UserRoleUpdateRequest) {
    const response = await apiClient.patch<ApiResponse<UserResponse>>(`/api/users/${userId}/role`, request)
    return response.data
  },

  async deleteUser(userId: number) {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/users/${userId}`)
    return response.data
  }
}
