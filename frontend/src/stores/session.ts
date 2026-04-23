import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getCurrentUser, login as loginRequest, register as registerRequest } from '@/lib/api/auth'
import type { LoginPayload, RegisterPayload } from '@/lib/api/auth'
import type { RegistrationResponse, UserResponse } from '@/lib/api/types'
import {
  clearStoredToken,
  getStoredToken,
  setStoredToken,
} from '@/lib/session-storage'

export const useSessionStore = defineStore('session', () => {
  const token = ref<string | null>(getStoredToken())
  const user = ref<UserResponse | null>(null)
  const isHydrated = ref(false)

  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const status = computed<'hydrating' | 'authenticated' | 'anonymous'>(() => {
    if (!isHydrated.value) {
      return 'hydrating'
    }

    return isAuthenticated.value ? 'authenticated' : 'anonymous'
  })

  async function hydrate(): Promise<void> {
    if (isHydrated.value) {
      return
    }

    token.value = getStoredToken()

    if (!token.value) {
      isHydrated.value = true
      return
    }

    try {
      user.value = await getCurrentUser()
    } catch {
      clearSession()
      return
    }

    isHydrated.value = true
  }

  async function login(payload: LoginPayload): Promise<void> {
    const response = await loginRequest(payload)
    await establishSession(response.token)
  }

  async function register(payload: RegisterPayload): Promise<RegistrationResponse> {
    return registerRequest(payload)
  }

  function logout(): void {
    clearSession()
  }

  async function establishSession(nextToken: string): Promise<void> {
    token.value = nextToken
    setStoredToken(nextToken)

    try {
      user.value = await getCurrentUser()
    } catch (error) {
      clearSession()
      throw error
    }

    isHydrated.value = true
  }

  function clearSession(): void {
    token.value = null
    user.value = null
    clearStoredToken()
    isHydrated.value = true
  }

  return {
    token,
    user,
    isHydrated,
    isAuthenticated,
    status,
    hydrate,
    login,
    register,
    logout,
    clearSession,
  }
})
