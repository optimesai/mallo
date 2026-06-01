import axios from 'axios'
import type { ApiResponse, UserResponse, UserUpdateRequest } from '@/api/authApi'

const AUTH_TOKEN_KEY = 'ssafy-pjt-access-token'

function getAuthHeaders() {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)

  return {
    Authorization: `Bearer ${token}`
  }
}

export const userApi = {
  async getMyInfo() {
    const response = await axios.get<ApiResponse<UserResponse>>('/api/users/me', {
      headers: getAuthHeaders()
    })
    return response.data
  },

  async updateMyInfo(request: UserUpdateRequest) {
    const response = await axios.patch<ApiResponse<UserResponse>>('/api/users/me', request, {
      headers: getAuthHeaders()
    })
    return response.data
  }
}
