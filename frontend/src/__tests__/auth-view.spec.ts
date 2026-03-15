import { screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import AuthView from '@/views/AuthView.vue'
import { sampleUser } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

function getInput(id: string): HTMLInputElement {
  const element = document.getElementById(id)

  if (!(element instanceof HTMLInputElement)) {
    throw new Error(`Expected input with id "${id}" to exist.`)
  }

  return element
}

describe('AuthView', () => {
  it('validates the register form when passwords do not match', async () => {
    const user = userEvent.setup()

    await renderRoute({
      route: '/auth?mode=register',
      authComponent: AuthView,
    })

    await user.type(getInput('register-username'), 'alice')
    await user.type(getInput('register-email'), 'alice@example.com')
    await user.type(getInput('register-password'), 'password123')
    await user.type(getInput('register-confirm-password'), 'password999')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Passwords must match')).toBeTruthy()
  })

  it('shows a helpful error on invalid login', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/login', () => new HttpResponse(null, { status: 401 })),
    )

    const user = userEvent.setup()

    await renderRoute({
      route: '/auth',
      authComponent: AuthView,
    })

    await user.type(getInput('login-email'), 'alice@example.com')
    await user.type(getInput('login-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Email or password is incorrect.')).toBeTruthy()
  })

  it('redirects to activities after a successful login', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/login', () =>
        HttpResponse.json({ token: 'jwt-token' }),
      ),
      http.get('http://localhost:8080/api/users/me', () => HttpResponse.json(sampleUser)),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/auth',
      authComponent: AuthView,
    })

    await user.type(getInput('login-email'), 'alice@example.com')
    await user.type(getInput('login-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => {
      expect(router.currentRoute.value.name).toBe('activities')
    })
  })
})
