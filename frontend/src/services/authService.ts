import { AxiosError } from 'axios'
import {
  authApi,
  type ApiResponse,
  type LoginRequest,
  type LoginResponse,
  type SignupRequest,
  type UserResponse
} from '@/api/authApi'

export const authService = {
  async existsByEmployeeNo(employeeNo: string): Promise<boolean> {
    try {
      const response = await authApi.existsByEmployeeNo(employeeNo)
      return response.data
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '사번 중복 여부를 확인하지 못했습니다.')
      }

      throw new Error('사번 중복 여부를 확인하지 못했습니다.')
    }
  },

  async signup(request: SignupRequest): Promise<ApiResponse<UserResponse>> {
    try {
      return await authApi.signup(request)
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '회원가입에 실패했습니다.')
      }

      throw new Error('회원가입에 실패했습니다.')
    }
  },

  async login(request: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    try {
      return await authApi.login(request)
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '로그인에 실패했습니다.')
      }

      throw new Error('로그인에 실패했습니다.')
    }
  },

  async refresh(): Promise<ApiResponse<LoginResponse>> {
    try {
      return await authApi.refresh()
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '토큰 재발급에 실패했습니다.')
      }

      throw new Error('토큰 재발급에 실패했습니다.')
    }
  },

  async logout(): Promise<ApiResponse<void>> {
    try {
      return await authApi.logout()
    } catch (error) {
      if (error instanceof AxiosError) {
        const message = error.response?.data?.message
        throw new Error(message || '로그아웃에 실패했습니다.')
      }

      throw new Error('로그아웃에 실패했습니다.')
    }
  }
}
