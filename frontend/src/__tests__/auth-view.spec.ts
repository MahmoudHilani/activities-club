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
  it('renders the minimal auth shell with login selected by default', async () => {
    await renderRoute({
      route: '/auth',
      authComponent: AuthView,
    })

    expect(
      screen.getByRole('heading', { name: 'Login or create an account' }),
    ).toBeTruthy()
    expect(screen.getByRole('tab', { name: 'Login' }).getAttribute('aria-selected')).toBe('true')
    expect(screen.getByRole('tab', { name: 'Register' }).getAttribute('aria-selected')).toBe(
      'false',
    )
  })

  it('selects the register tab from the mode query parameter', async () => {
    await renderRoute({
      route: '/auth?mode=register',
      authComponent: AuthView,
    })

    expect(screen.getByRole('tab', { name: 'Register' }).getAttribute('aria-selected')).toBe(
      'true',
    )
  })

  it('validates the register form when passwords do not match', async () => {
    const user = userEvent.setup()

    await renderRoute({
      route: '/auth?mode=register',
      authComponent: AuthView,
    })

    await user.type(getInput('register-username'), 'alice')
    await user.type(getInput('register-email'), 'alice@example.com')
    await user.type(getInput('register-student-number'), 'S1234567')
    await user.type(getInput('register-phone-number'), '+3531234567')
    await user.type(getInput('register-password'), 'password123')
    await user.type(getInput('register-confirm-password'), 'password999')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Passwords must match')).toBeTruthy()
  })

  it('requires student number and phone number during registration', async () => {
    const user = userEvent.setup()

    await renderRoute({
      route: '/auth?mode=register',
      authComponent: AuthView,
    })

    await user.type(getInput('register-username'), 'alice')
    await user.type(getInput('register-email'), 'alice@example.com')
    await user.type(getInput('register-password'), 'password123')
    await user.type(getInput('register-confirm-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Student number is required')).toBeTruthy()
    expect(await screen.findByText('Phone number is required')).toBeTruthy()
  })

  it('submits staff registration without student number or phone number', async () => {
    let capturedPayload: Record<string, unknown> | null = null

    server.use(
      http.post('http://localhost:8080/api/auth/register', async ({ request }) => {
        capturedPayload = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(
          {
            approvalStatus: 'PENDING',
            message: 'Registration submitted for admin approval.',
          },
          { status: 202 },
        )
      }),
    )

    const user = userEvent.setup()

    await renderRoute({
      route: '/auth?mode=register',
      authComponent: AuthView,
    })

    await user.click(screen.getByRole('combobox', { name: 'User type' }))
    await user.click(await screen.findByRole('option', { name: 'Staff' }))

    expect(screen.queryByLabelText('Student number')).toBeNull()
    expect(screen.queryByLabelText('Phone number')).toBeNull()

    await user.type(getInput('register-username'), 'staff')
    await user.type(getInput('register-email'), 'staff@example.com')
    await user.type(getInput('register-password'), 'password123')
    await user.type(getInput('register-confirm-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => {
      expect(capturedPayload).toMatchObject({
        username: 'staff',
        email: 'staff@example.com',
        userType: 'STAFF',
        studentNumber: null,
        phoneNumber: null,
      })
    })

    expect(await screen.findByText('Awaiting approval')).toBeTruthy()
    expect(screen.getByText('Registration submitted for admin approval.')).toBeTruthy()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('shows a helpful error on invalid login', async () => {
    server.use(
      http.post(
        'http://localhost:8080/api/auth/login',
        () => new HttpResponse(null, { status: 401 }),
      ),
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

  it('shows the approval-pending message on blocked login', async () => {
    server.use(
      http.post(
        'http://localhost:8080/api/auth/login',
        () =>
          HttpResponse.json(
            { detail: 'Your registration is awaiting admin approval.' },
            { status: 403 },
          ),
      ),
    )

    const user = userEvent.setup()

    await renderRoute({
      route: '/auth',
      authComponent: AuthView,
    })

    await user.type(getInput('login-email'), 'alice@example.com')
    await user.type(getInput('login-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Your registration is awaiting admin approval.')).toBeTruthy()
  })

  it('shows the denial message on blocked login', async () => {
    server.use(
      http.post(
        'http://localhost:8080/api/auth/login',
        () =>
          HttpResponse.json(
            {
              detail:
                'Your registration request was denied. Please contact an admin before trying again.',
            },
            { status: 403 },
          ),
      ),
    )

    const user = userEvent.setup()

    await renderRoute({
      route: '/auth',
      authComponent: AuthView,
    })

    await user.type(getInput('login-email'), 'alice@example.com')
    await user.type(getInput('login-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(
      await screen.findByText(
        'Your registration request was denied. Please contact an admin before trying again.',
      ),
    ).toBeTruthy()
  })

  it('redirects to the requested detail page after a successful login', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/login', () =>
        HttpResponse.json({ token: 'jwt-token' }),
      ),
      http.get('http://localhost:8080/api/users/me', () => HttpResponse.json(sampleUser)),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/auth?redirect=/activities/7',
      authComponent: AuthView,
    })

    expect(screen.getByRole('heading', { name: 'Login or create an account' })).toBeTruthy()

    await user.type(getInput('login-email'), 'alice@example.com')
    await user.type(getInput('login-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => {
      expect(router.currentRoute.value.fullPath).toBe('/activities/7')
    })
  })

  it('keeps the user on auth and offers login after registration', async () => {
    server.use(
      http.post('http://localhost:8080/api/auth/register', () =>
        HttpResponse.json(
          {
            approvalStatus: 'PENDING',
            message: 'Registration submitted for admin approval.',
          },
          { status: 202 },
        ),
      ),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/auth?mode=register&redirect=/activities/9',
      authComponent: AuthView,
    })

    await user.type(getInput('register-username'), 'alice')
    await user.type(getInput('register-email'), 'alice@example.com')
    await user.type(getInput('register-student-number'), 'S1234567')
    await user.type(getInput('register-phone-number'), '+3531234567')
    await user.type(getInput('register-password'), 'password123')
    await user.type(getInput('register-confirm-password'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => {
      expect(router.currentRoute.value.fullPath).toBe('/auth?mode=register&redirect=/activities/9')
    })

    expect(await screen.findByRole('button', { name: 'Back to login' })).toBeTruthy()

    await user.click(screen.getByRole('button', { name: 'Back to login' }))

    await waitFor(() => {
      expect(router.currentRoute.value.fullPath).toBe('/auth?redirect=/activities/9')
    })
  })
})
