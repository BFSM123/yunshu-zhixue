export const ACCESS_TOKEN_KEY = 'yszx_portal_access_token'
export const REFRESH_TOKEN_KEY = 'yszx_portal_refresh_token'

export function clearStoredTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}
