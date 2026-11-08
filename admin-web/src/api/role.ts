import { http } from './request'
import type { PermissionNode, RoleOption, RoleVO } from '@/types/system'

export function getRoles() {
  return http.get<RoleVO[]>('/roles')
}

export function createRole(payload: { name: string; code: string; description?: string }) {
  return http.post<RoleVO>('/roles', payload)
}

export function deleteRole(id: number) {
  return http.delete<void>(`/roles/${id}`)
}

export function getPermissionTree(roleId?: number) {
  return http.get<PermissionNode[]>('/permissions/tree', roleId ? { roleId } : undefined)
}

export function saveRolePermissions(id: number, permissionIds: number[]) {
  return http.put<void>(`/roles/${id}/permissions`, { permissionIds })
}

export function getRoleOptions() {
  return http.get<RoleOption[]>('/roles/options')
}
