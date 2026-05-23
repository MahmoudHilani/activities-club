import { fireEvent, screen, waitFor, within } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import AdminUsersView from '@/views/AdminUsersView.vue'
import { useSessionStore } from '@/stores/session'
import { sampleUser } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'
import type { UserResponse } from '@/lib/api/types'

function buildUser(overrides: Partial<UserResponse>): UserResponse {
  return {
    ...sampleUser,
    ...overrides,
  }
}

async function renderUsers(users: UserResponse[]) {
  server.use(
    http.get('http://localhost:8080/api/admin/users', () => HttpResponse.json(users)),
  )

  const result = await renderRoute({
    route: '/admin/users',
    adminUsersComponent: AdminUsersView,
  })

  const sessionStore = useSessionStore(result.pinia)
  sessionStore.isHydrated = true
  sessionStore.token = 'token'
  sessionStore.user = buildUser({
    id: 99,
    username: 'admin',
    email: 'admin@example.com',
    userType: 'STAFF',
    studentNumber: null,
    phoneNumber: null,
    approvalStatus: 'APPROVED',
    isAdmin: true,
  })

  return result
}

describe('AdminUsersView', () => {
  it('searches pending users by username, email, student number, and phone number', async () => {
    await renderUsers([
      buildUser({
        id: 2,
        username: 'Niamh Murphy',
        email: 'niamh@example.com',
        studentNumber: 'S1111111',
        phoneNumber: '+353111111',
        approvalStatus: 'PENDING',
        createdAt: '2026-03-16T10:00:00Z',
      }),
      buildUser({
        id: 3,
        username: 'Oscar Byrne',
        email: 'oscar@example.com',
        studentNumber: 'S2222222',
        phoneNumber: '+353222222',
        approvalStatus: 'PENDING',
        createdAt: '2026-03-15T10:00:00Z',
      }),
    ])

    expect(await screen.findByText('Niamh Murphy')).toBeTruthy()
    expect(screen.getByText('Oscar Byrne')).toBeTruthy()

    const search = screen.getByRole('searchbox', { name: 'Search users' })

    for (const term of ['niamh', 'oscar@example.com', 'S1111111', '+353222222']) {
      await fireEvent.update(search, term)

      await waitFor(() => {
        expect(screen.queryByText(term === 'niamh' || term === 'S1111111' ? 'Oscar Byrne' : 'Niamh Murphy')).toBeNull()
      })

      await fireEvent.update(search, '')
    }
  })

  it('switches status chips and shows per-status actions', async () => {
    await renderUsers([
      buildUser({
        id: 2,
        username: 'pending-student',
        approvalStatus: 'PENDING',
        dateOfBirth: null,
      }),
      buildUser({
        id: 3,
        username: 'approved-staff',
        email: 'approved@example.com',
        userType: 'STAFF',
        studentNumber: null,
        phoneNumber: null,
        approvalStatus: 'APPROVED',
        isAdmin: false,
      }),
      buildUser({
        id: 4,
        username: 'denied-student',
        email: 'denied@example.com',
        approvalStatus: 'DENIED',
      }),
    ])

    expect(await screen.findByText('pending-student')).toBeTruthy()
    expect(screen.queryByText('approved-staff')).toBeNull()

    await userEvent.click(screen.getByRole('tab', { name: /Approved 1/i }))
    const approvedRow = await screen.findByText('approved-staff')
    const approvedArticle = approvedRow.closest('article')
    expect(approvedArticle).not.toBeNull()
    expect(within(approvedArticle as HTMLElement).getByRole('button', { name: 'Grant admin' })).toBeTruthy()
    expect(within(approvedArticle as HTMLElement).queryByText('Not required')).toBeNull()

    await userEvent.click(screen.getByRole('tab', { name: /Denied 1/i }))
    expect(await screen.findByText('denied-student')).toBeTruthy()
    expect(screen.getByText('Denied - appeal required to return this registration to review.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Approve' })).toBeNull()
  })

  it('shows an empty pending queue without a reset action', async () => {
    await renderUsers([
      buildUser({
        id: 3,
        username: 'approved-staff',
        userType: 'STAFF',
        approvalStatus: 'APPROVED',
      }),
    ])

    expect(await screen.findByText('Nothing')).toBeTruthy()
    expect(screen.getByText('Try a different search or choose another status.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Reset filters' })).toBeNull()
  })

  it('requires confirmation before granting admin access', async () => {
    let staff = buildUser({
      id: 2,
      username: 'staff-member',
      email: 'staff@example.com',
      userType: 'STAFF',
      studentNumber: null,
      phoneNumber: null,
      approvalStatus: 'APPROVED',
      isAdmin: false,
    })
    let adminPatchCount = 0

    server.use(
      http.get('http://localhost:8080/api/admin/users', () => HttpResponse.json([staff])),
      http.patch('http://localhost:8080/api/admin/users/2/admin', async ({ request }) => {
        adminPatchCount += 1
        const body = (await request.json()) as { isAdmin: boolean }
        staff = {
          ...staff,
          isAdmin: body.isAdmin,
        }
        return HttpResponse.json(staff)
      }),
    )

    await renderRoute({
      route: '/admin/users',
      adminUsersComponent: AdminUsersView,
    })

    await userEvent.click(await screen.findByRole('tab', { name: /Approved 1/i }))
    await userEvent.click(screen.getByRole('button', { name: 'Grant admin' }))

    expect(adminPatchCount).toBe(0)
    expect(screen.getByRole('dialog')).toBeTruthy()
    expect(screen.getByText(/staff-member will become an admin/i)).toBeTruthy()

    await userEvent.click(screen.getByRole('button', { name: 'Grant admin access' }))

    await waitFor(() => {
      expect(adminPatchCount).toBe(1)
      expect(screen.queryByRole('dialog')).toBeNull()
    })
  })

  it('requires confirmation before denying a pending registration', async () => {
    let status = 'PENDING'

    server.use(
      http.get('http://localhost:8080/api/admin/users', () =>
        HttpResponse.json([
          buildUser({
            id: 2,
            username: 'pending-member',
            approvalStatus: status as UserResponse['approvalStatus'],
          }),
        ]),
      ),
      http.patch('http://localhost:8080/api/admin/users/2/approval', async ({ request }) => {
        const body = (await request.json()) as { approvalStatus: string }
        status = body.approvalStatus
        return HttpResponse.json(
          buildUser({
            id: 2,
            username: 'pending-member',
            approvalStatus: 'DENIED',
          }),
        )
      }),
    )

    await renderRoute({
      route: '/admin/users',
      adminUsersComponent: AdminUsersView,
    })

    await userEvent.click(await screen.findByRole('button', { name: 'More actions' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Deny' }))

    expect(status).toBe('PENDING')
    expect(screen.getByRole('heading', { name: 'Deny this registration?' })).toBeTruthy()

    await userEvent.click(screen.getByRole('button', { name: 'Deny registration' }))

    await waitFor(() => {
      expect(status).toBe('DENIED')
      expect(screen.queryByText('pending-member')).toBeNull()
    })
  })

  it('disables removing the only remaining admin', async () => {
    await renderUsers([
      buildUser({
        id: 2,
        username: 'director',
        email: 'director@example.com',
        userType: 'STAFF',
        studentNumber: null,
        phoneNumber: null,
        approvalStatus: 'APPROVED',
        isAdmin: true,
      }),
    ])

    await userEvent.click(await screen.findByRole('tab', { name: /Approved 1/i }))

    const removeButton = await screen.findByRole('button', { name: 'Remove admin' })
    expect(removeButton.getAttribute('disabled')).not.toBeNull()
    expect(screen.getByText('Only admin remaining')).toBeTruthy()
  })

  it('labels the current admin as you', async () => {
    await renderUsers([
      buildUser({
        id: 99,
        username: 'admin',
        email: 'admin@example.com',
        userType: 'STAFF',
        studentNumber: null,
        phoneNumber: null,
        approvalStatus: 'APPROVED',
        isAdmin: true,
      }),
    ])

    await userEvent.click(await screen.findByRole('tab', { name: /Approved 1/i }))

    expect(screen.getByText('You')).toBeTruthy()
    expect(screen.queryByText('Current session')).toBeNull()
  })

  it('keeps the date of birth editor saving inline', async () => {
    let student = buildUser({
      id: 2,
      username: 'student-member',
      email: 'student@example.com',
      dateOfBirth: null,
      approvalStatus: 'PENDING',
    })

    server.use(
      http.get('http://localhost:8080/api/admin/users', () => HttpResponse.json([student])),
      http.patch('http://localhost:8080/api/admin/users/2/date-of-birth', async ({ request }) => {
        const body = (await request.json()) as { dateOfBirth: string | null }
        expect(body.dateOfBirth).toBe('2008-01-01')
        student = {
          ...student,
          dateOfBirth: body.dateOfBirth,
        }
        return HttpResponse.json(student)
      }),
    )

    await renderRoute({
      route: '/admin/users',
      adminUsersComponent: AdminUsersView,
    })

    expect(await screen.findByText('student-member')).toBeTruthy()
    await fireEvent.click(screen.getByRole('button', { name: 'Date of birth for student-member' }))
    await fireEvent.click(screen.getByRole('button', { name: /January 1, 2008/ }))

    await waitFor(() => {
      expect(screen.getByText('Jan 1, 2008')).toBeTruthy()
    })
  })
})
