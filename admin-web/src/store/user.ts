import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AdminProfile } from '@/types/api'
import { ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY } from '@/utils/token'

export const useAdminUserStore = defineStore('admin-user', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  const profile = ref<AdminProfile | null>(null)

  const isLoggedIn = computed(() => Boolean(accessToken.value))

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem(ACCESS_TOKEN_KEY, access)
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  }

  function setProfile(value: AdminProfile | null) {
    profile.value = value
  }

  function clear() {
    accessToken.value = ''
    refreshToken.value = ''
    profile.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  return {
    accessToken,
    refreshToken,
    profile,
    isLoggedIn,
    setTokens,
    setProfile,
    clear
  }
})
