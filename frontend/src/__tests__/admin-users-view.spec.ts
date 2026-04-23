import { screen, waitFor, within } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import AdminUsersView from '@/views/AdminUsersView.vue'
import { useSessionStore } from '@/stores/session'
import { sampleUser } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

describe('AdminUsersView', () => {
  it('loads users and toggles admin access', async () => {
    let managedUser = {
      ...sampleUser,
      id: 2,
      username: 'staff-member',
      email: 'staff@example.com',
      userType: 'STAFF' as const,
      studentNumber: null,
      phoneNumber: null,
      isAdmin: false,
    }

    server.use(
      http.get('http://localhost:8080/api/admin/users', () =>
        HttpResponse.json([
          managedUser,
          {
            ...sampleUser,
            id: 99,
            username: 'admin',
            email: 'admin@example.com',
            userType: 'STAFF',
            studentNumber: null,
            phoneNumber: null,
            isAdmin: true,
          },
        ]),
      ),
      http.patch('http://localhost:8080/api/admin/users/2/admin', async ({ request }) => {
        const body = (await request.json()) as { isAdmin: boolean }
        managedUser = {
          ...managedUser,
          isAdmin: body.isAdmin,
        }
        return HttpResponse.json(managedUser)
      }),
    )

    const user = userEvent.setup()
    const { pinia } = await renderRoute({
      route: '/admin/users',
      adminUsersComponent: AdminUsersView,
    })

    const sessionStore = useSessionStore(pinia)
    sessionStore.isHydrated = true
    sessionStore.token = 'token'
    sessionStore.user = {
      ...sampleUser,
      id: 99,
      username: 'admin',
      email: 'admin@example.com',
      userType: 'STAFF',
      studentNumber: null,
      phoneNumber: null,
      isAdmin: true,
    }

    expect(await screen.findByText('staff-member')).toBeTruthy()

    await user.click(screen.getByRole('button', { name: 'Grant admin' }))

    await waitFor(() => {
      const managedUserCard = screen.getByText('staff-member').closest('article')
      if (!managedUserCard) {
        throw new Error('Expected the managed user card to exist.')
      }

      expect(within(managedUserCard).getByRole('button', { name: 'Remove admin' })).toBeTruthy()
    })
  })

  it('disables admin changes for the current session user', async () => {
    server.use(
      http.get('http://localhost:8080/api/admin/users', () =>
        HttpResponse.json([
          {
            ...sampleUser,
            id: 99,
            username: 'admin',
            email: 'admin@example.com',
            userType: 'STAFF',
            studentNumber: null,
            phoneNumber: null,
            isAdmin: true,
          },
        ]),
      ),
    )

    const { pinia } = await renderRoute({
      route: '/admin/users',
      adminUsersComponent: AdminUsersView,
    })

    const sessionStore = useSessionStore(pinia)
    sessionStore.isHydrated = true
    sessionStore.token = 'token'
    sessionStore.user = {
      ...sampleUser,
      id: 99,
      username: 'admin',
      email: 'admin@example.com',
      userType: 'STAFF',
      studentNumber: null,
      phoneNumber: null,
      isAdmin: true,
    }

    const button = await screen.findByRole('button', { name: 'Remove admin' })
    expect(button.getAttribute('disabled')).not.toBeNull()
    expect(screen.getByText('Current session')).toBeTruthy()
  })
})
