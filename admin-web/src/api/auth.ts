import { http } from './request'
import type { AdminLoginResult } from '@/types/api'

export function adminLogin(payload: { account: string; password: string }) {
  return http.post<AdminLoginResult>('/auth/login', payload)
}

export function adminLogout(refreshToken: string) {
  return http.post<void>('/auth/logout', undefined, {
    headers: { 'X-Refresh-Token': refreshToken }
  })
}
