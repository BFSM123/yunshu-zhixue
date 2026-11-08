export interface FieldErrorItem {
  field: string
  reason: string
}

export interface FileErrorItem {
  row: number
  field: string
  reason: string
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  fieldErrors?: FieldErrorItem[] | null
  fileErrors?: FileErrorItem[] | null
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface AdminLoginResult {
  accessToken: string
  refreshToken: string
  permissions?: string[]
  roleCode?: string
  username?: string
  nickname?: string
}

export interface AdminProfile {
  id: number
  username: string
  nickname?: string
  roleCode?: string
  permissions: string[]
}
