import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface UserResponse {
  userId: number
  employeeNo: string
  userName: string
  department: string
  role: 'WORKER' | 'MANAGER' | 'ADMIN'
  createdAt: string
  updatedAt: string
}

export interface SignupRequest {
  employeeNo: string
  userName: string
  department: string
  password: string
}

export interface LoginRequest {
  employeeNo: string
  password: string
}

export interface TokenResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
}

export interface LoginResponse {
  token: TokenResponse
  user: UserResponse
}

export interface UserUpdateRequest {
  userName?: string
  department?: string
  password?: string
}

export const authApi = {
  async signup(request: SignupRequest) {
    const response = await axios.post<ApiResponse<UserResponse>>('/api/auth/signup', request)
    return response.data
  },

  async login(request: LoginRequest) {
    const response = await axios.post<ApiResponse<LoginResponse>>('/api/auth/login', request, {
      withCredentials: true
    })
    return response.data
  },

  async refresh() {
    const response = await axios.post<ApiResponse<LoginResponse>>('/api/auth/refresh', null, {
      withCredentials: true
    })
    return response.data
  },

  async logout() {
    const response = await axios.post<ApiResponse<void>>('/api/auth/logout', null, {
      withCredentials: true
    })
    return response.data
  }
}
