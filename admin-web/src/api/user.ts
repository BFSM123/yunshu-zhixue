import { http } from './request'
import type { PageResult } from '@/types/api'
import type {
  CreateUserPayload,
  UpdateUserPayload,
  UserDetail,
  UserVO
} from '@/types/system'

export interface UserQuery {
  keyword?: string
  roleId?: number
  status?: number
  page: number
  pageSize: number
}

export function getUsers(params: UserQuery) {
  return http.get<PageResult<UserVO>>('/users', params)
}

export function getUserDetail(id: number) {
  return http.get<UserDetail>(`/users/${id}/detail`)
}

export function createUser(payload: CreateUserPayload) {
  return http.post<UserVO>('/users', payload)
}

export function updateUser(id: number, payload: UpdateUserPayload) {
  return http.put<UserVO>(`/users/${id}`, payload)
}

export function updateUserStatusBatch(ids: number[], status: number) {
  return http.put<void>('/users/batch-status', { ids, status })
}

export function deleteUser(id: number) {
  return http.delete<void>(`/users/${id}`)
}

export function batchDeleteUsers(ids: number[]) {
  return http.delete<void>('/users/batch', { data: { ids } })
}

export function resetUserPassword(id: number) {
  return http.post<{ password: string }>(`/users/${id}/reset-password`)
}
