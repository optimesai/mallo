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
  }
}
