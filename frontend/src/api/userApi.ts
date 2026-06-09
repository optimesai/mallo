import { apiClient } from '@/api/client'
import type { ApiResponse, UserResponse, UserUpdateRequest } from '@/api/authApi'

export const userApi = {
  async getMyInfo() {
    const response = await apiClient.get<ApiResponse<UserResponse>>('/api/users/me')
    return response.data
  },

  async updateMyInfo(request: UserUpdateRequest) {
    const response = await apiClient.patch<ApiResponse<UserResponse>>('/api/users/me', request)
    return response.data
  }
}
