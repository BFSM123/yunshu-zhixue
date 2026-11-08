export interface UserVO {
  id: number
  username: string
  mobile: string | null
  email: string | null
  roleId: number
  roleName: string | null
  roleCode: string | null
  status: number
  isBuiltin: boolean
  lastLoginAt: string | null
  createdAt: string | null
}

export interface UserDetail {
  user: UserVO
  recentOperations: OperationLogVO[]
}

export interface CreateUserPayload {
  username: string
  mobile: string
  email?: string
  roleId: number
  password: string
  status: number
}

export interface UpdateUserPayload {
  username: string
  mobile: string
  email?: string
  roleId: number
  newPassword?: string
  status: number
}

export interface RoleOption {
  id: number
  name: string
  code: string
}

export interface RoleVO {
  id: number
  name: string
  code: string
  description: string | null
  userCount: number
  permissionLabels: string[]
}

export interface PermissionNode {
  id: number
  name: string
  code: string
  checked: boolean
  children: PermissionNode[]
}

export interface OperationLogVO {
  id: number
  userId: number
  username: string
  action: string
  targetType: string
  targetName: string
  ip: string
  createdAt: string
}

export interface OperationLogQuery {
  keyword?: string
  operationType?: string
  startDate?: string
  endDate?: string
  page: number
  pageSize: number
}

export interface SystemSettings {
  siteName: string
  siteDescription: string
  logoUrl: string
  defaultAvatar: string
}
