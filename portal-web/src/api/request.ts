import axios, { type AxiosRequestConfig, type AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { useUserStore } from '@/store/user'
import { ACCESS_TOKEN_KEY, clearStoredTokens } from '@/utils/token'

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/portal/v1',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new ApiError(body.code, body.message))
    }
    return response.data
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data

    if (status === 401) {
      useUserStore().clear()
      clearStoredTokens()
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
    }

    const message = body?.message || error.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return service.get(url, { params }) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown): Promise<T> {
    return service.put(url, data) as unknown as Promise<T>
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config) as unknown as Promise<T>
  }
}

export default service
