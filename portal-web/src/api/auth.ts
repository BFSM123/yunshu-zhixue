import { http } from './request'
import type { LoginResult, SmsCodeResult } from '@/types/api'

export type SmsScene = 'register' | 'login' | 'reset'

export function sendSmsCode(payload: { mobile: string; type: SmsScene }) {
  return http.post<SmsCodeResult>('/auth/sms-code', payload)
}

export function register(payload: { mobile: string; code: string; password: string }) {
  return http.post<LoginResult>('/auth/register', payload)
}

export function loginByPassword(payload: { account: string; password: string }) {
  return http.post<LoginResult>('/auth/login/password', payload)
}

export function loginBySms(payload: { mobile: string; code: string }) {
  return http.post<LoginResult>('/auth/login/sms', payload)
}

export function verifyResetCode(payload: { mobile: string; code: string }) {
  return http.post<{ resetToken: string }>('/auth/reset/verify', payload)
}

export function resetPassword(payload: { resetToken: string; newPassword: string }) {
  return http.post<void>('/auth/reset', payload)
}

export function refreshToken(refreshTokenValue: string) {
  return http.post<LoginResult>('/auth/refresh', undefined, {
    headers: { 'X-Refresh-Token': refreshTokenValue }
  })
}

export function logout(refreshTokenValue: string) {
  return http.post<void>('/auth/logout', undefined, {
    headers: { 'X-Refresh-Token': refreshTokenValue }
  })
}
