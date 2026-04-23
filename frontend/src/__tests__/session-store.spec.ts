import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'

import { AUTH_TOKEN_STORAGE_KEY } from '@/lib/session-storage'
import { useSessionStore } from '@/stores/session'
import { sampleUser } from '@/test/fixtures'
import { server } from '@/test/server'

describe('session store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('clears the session when token hydration fails', async () => {
    window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, 'expired-token')

    server.use(
      http.get('http://localhost:8080/api/users/me', () => new HttpResponse(null, { status: 401 })),
    )

    const sessionStore = useSessionStore()

    await sessionStore.hydrate()

    expect(sessionStore.isHydrated).toBe(true)
    expect(sessionStore.isAuthenticated).toBe(false)
    expect(sessionStore.user).toBeNull()
    expect(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('stores the token and current user after login', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/login', async ({ request }) => {
        const body = (await request.json()) as { email: string; password: string }

        expect(body).toEqual({
          email: 'alice@example.com',
          password: 'password123',
        })

        return HttpResponse.json({ token: 'jwt-token' })
      }),
      http.get('http://localhost:8080/api/users/me', ({ request }) => {
        expect(request.headers.get('authorization')).toBe('Bearer jwt-token')
        return HttpResponse.json(sampleUser)
      }),
    )

    const sessionStore = useSessionStore()

    await sessionStore.login({
      email: 'alice@example.com',
      password: 'password123',
    })

    expect(sessionStore.isAuthenticated).toBe(true)
    expect(sessionStore.user?.username).toBe('alice')
    expect(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBe('jwt-token')
  })

  it('submits registration without establishing a session', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/register', async ({ request }) => {
        const body = (await request.json()) as {
          username: string
          email: string
          userType: string
          studentNumber: string | null
          phoneNumber: string | null
          password: string
        }

        expect(body.email).toBe('alice@example.com')
        return HttpResponse.json(
          {
            approvalStatus: 'PENDING',
            message: 'Registration submitted for admin approval.',
          },
          { status: 202 },
        )
      }),
    )

    const sessionStore = useSessionStore()

    const response = await sessionStore.register({
      username: 'alice',
      email: 'alice@example.com',
      userType: 'STUDENT',
      studentNumber: 'S1234567',
      phoneNumber: '+3531234567',
      password: 'password123',
    })

    expect(response.approvalStatus).toBe('PENDING')
    expect(sessionStore.isAuthenticated).toBe(false)
    expect(sessionStore.user).toBeNull()
    expect(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)).toBeNull()
  })
})
