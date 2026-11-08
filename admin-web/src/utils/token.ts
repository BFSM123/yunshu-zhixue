export const ACCESS_TOKEN_KEY = 'yszx_admin_access_token'
export const REFRESH_TOKEN_KEY = 'yszx_admin_refresh_token'
export const PERMISSIONS_KEY = 'yszx_admin_permissions'
export const ROLE_KEY = 'yszx_admin_role'

export function clearStoredAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(PERMISSIONS_KEY)
  localStorage.removeItem(ROLE_KEY)
}
