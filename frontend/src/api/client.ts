import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

let accessTokenProvider: (() => string | null) | null = null
let refreshTokenHandler: (() => Promise<boolean>) | null = null
let unauthorizedHandler: (() => void) | null = null
let refreshPromise: Promise<boolean> | null = null

export const apiClient = axios.create({
  withCredentials: true
})

export function configureAuthClient(options: {
  getAccessToken: () => string | null
  refreshAccessToken: () => Promise<boolean>
  handleUnauthorized: () => void
}) {
  accessTokenProvider = options.getAccessToken
  refreshTokenHandler = options.refreshAccessToken
  unauthorizedHandler = options.handleUnauthorized
}

apiClient.interceptors.request.use((config) => {
  const token = accessTokenProvider?.()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryableRequestConfig | undefined

    if (error.response?.status !== 401 || !config || config._retry || isAuthRequest(config.url)) {
      return Promise.reject(error)
    }

    config._retry = true
    const refreshed = await refreshAccessTokenOnce()

    if (!refreshed) {
      unauthorizedHandler?.()
      return Promise.reject(error)
    }

    return apiClient(config)
  }
)

async function refreshAccessTokenOnce() {
  if (!refreshTokenHandler) {
    return false
  }

  if (!refreshPromise) {
    refreshPromise = refreshTokenHandler().finally(() => {
      refreshPromise = null
    })
  }

  return refreshPromise
}

function isAuthRequest(url?: string) {
  return Boolean(url?.startsWith('/api/auth/'))
}
