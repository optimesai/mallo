import { AxiosError } from 'axios'
import { userApi } from '@/api/userApi'
import type { UserRoleUpdateRequest } from '@/api/userApi'
import type { ApiResponse, UserResponse, UserUpdateRequest } from '@/api/authApi'

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || fallback
  }

  return fallback
}

export const userService = {
  async getMyInfo(): Promise<ApiResponse<UserResponse>> {
    try {
      return await userApi.getMyInfo()
    } catch (error) {
      throw new Error(getErrorMessage(error, '내 정보를 불러오지 못했습니다.'))
    }
  },

  async updateMyInfo(request: UserUpdateRequest): Promise<ApiResponse<UserResponse>> {
    try {
      return await userApi.updateMyInfo(request)
    } catch (error) {
      throw new Error(getErrorMessage(error, '내 정보를 수정하지 못했습니다.'))
    }
  },

  async getUsers(): Promise<UserResponse[]> {
    try {
      const response = await userApi.getUsers()
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '사용자 목록을 불러오지 못했습니다.'))
    }
  },

  async getUser(userId: number): Promise<UserResponse> {
    try {
      const response = await userApi.getUser(userId)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '사용자 상세 정보를 불러오지 못했습니다.'))
    }
  },

  async updateUserRole(userId: number, request: UserRoleUpdateRequest): Promise<UserResponse> {
    try {
      const response = await userApi.updateUserRole(userId, request)
      return response.data
    } catch (error) {
      throw new Error(getErrorMessage(error, '사용자 권한을 수정하지 못했습니다.'))
    }
  },

  async deleteUser(userId: number): Promise<void> {
    try {
      await userApi.deleteUser(userId)
    } catch (error) {
      throw new Error(getErrorMessage(error, '사용자를 삭제하지 못했습니다.'))
    }
  }
}
