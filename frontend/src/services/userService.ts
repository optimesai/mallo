import { AxiosError } from 'axios'
import { userApi } from '@/api/userApi'
import type { ApiResponse, UserResponse, UserUpdateRequest } from '@/api/authApi'

export const userService = {
  async getMyInfo(): Promise<ApiResponse<UserResponse>> {
    try {
      return await userApi.getMyInfo()
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '내 정보를 불러오지 못했습니다.')
      }

      throw new Error('내 정보를 불러오지 못했습니다.')
    }
  },

  async updateMyInfo(request: UserUpdateRequest): Promise<ApiResponse<UserResponse>> {
    try {
      return await userApi.updateMyInfo(request)
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '내 정보를 수정하지 못했습니다.')
      }

      throw new Error('내 정보를 수정하지 못했습니다.')
    }
  }
}
