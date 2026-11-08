import { http } from './request'
import type { SystemSettings } from '@/types/system'

export function getSettings() {
  return http.get<SystemSettings>('/settings')
}

export function updateSettings(payload: SystemSettings) {
  return http.put<void>('/settings', payload)
}

export function resetSettings() {
  return http.post<void>('/settings/reset')
}

function uploadImage(url: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<{ url: string }>(url, formData)
}

export function uploadLogo(file: File) {
  return uploadImage('/files/image', file)
}

export function uploadAvatar(file: File) {
  return uploadImage('/files/avatar', file)
}
