import { screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import AdminActivitiesView from '@/views/AdminActivitiesView.vue'
import AdminActivityReservationsView from '@/views/AdminActivityReservationsView.vue'
import {
  buildActivity,
  buildAdminActivityReservationsResponse,
  buildAdminReservationEntry,
  buildPage,
} from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

describe('AdminActivityReservationsView', () => {
  it('navigates from the admin dashboard to the reservations page', async () => {
    server.use(
      http.get('http://localhost:8080/api/admin/activities', () =>
        HttpResponse.json(buildPage([buildActivity()])),
      ),
      http.get('http://localhost:8080/api/admin/activities/1/reservations', () =>
        HttpResponse.json(buildAdminActivityReservationsResponse()),
      ),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/admin/activities',
      adminComponent: AdminActivitiesView,
      adminReservationsComponent: AdminActivityReservationsView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()

    await user.click(screen.getByRole('link', { name: /View reservations/i }))

    await waitFor(() => {
      expect(router.currentRoute.value.fullPath).toBe('/admin/activities/1/reservations')
    })
    expect(await screen.findByRole('heading', { name: 'Chess Night' })).toBeTruthy()
  })

  it('renders grouped reservation sections with username and email', async () => {
    server.use(
      http.get('http://localhost:8080/api/admin/activities/1/reservations', () =>
        HttpResponse.json(
          buildAdminActivityReservationsResponse({
            reservations: [
              buildAdminReservationEntry({
                id: 1,
                status: 'RESERVED',
                user: {
                  id: 1,
                  username: 'bob',
                  email: 'bob@example.com',
                },
              }),
              buildAdminReservationEntry({
                id: 2,
                status: 'WAITLISTED',
                user: {
                  id: 2,
                  username: 'cara',
                  email: 'cara@example.com',
                },
              }),
              buildAdminReservationEntry({
                id: 3,
                status: 'CANCELLED',
                cancelledAt: '2026-03-20T19:30:00Z',
                user: {
                  id: 3,
                  username: 'alice',
                  email: 'alice@example.com',
                },
              }),
            ],
          }),
        ),
      ),
    )

    await renderRoute({
      route: '/admin/activities/1/reservations',
      adminReservationsComponent: AdminActivityReservationsView,
    })

    expect(await screen.findByRole('heading', { name: 'Confirmed' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Waitlisted' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Cancelled' })).toBeTruthy()
    expect(screen.getByText('bob@example.com')).toBeTruthy()
    expect(screen.getByText('cara@example.com')).toBeTruthy()
    expect(screen.getByText('alice@example.com')).toBeTruthy()
  })

  it('renders an empty state when the activity has no reservations', async () => {
    server.use(
      http.get('http://localhost:8080/api/admin/activities/1/reservations', () =>
        HttpResponse.json(
          buildAdminActivityReservationsResponse({
            reservations: [],
          }),
        ),
      ),
    )

    await renderRoute({
      route: '/admin/activities/1/reservations',
      adminReservationsComponent: AdminActivityReservationsView,
    })

    expect(await screen.findByText('No reservation history yet.')).toBeTruthy()
  })

  it('renders an error state when loading the roster fails', async () => {
    server.use(
      http.get('http://localhost:8080/api/admin/activities/1/reservations', () =>
        new HttpResponse(null, { status: 500 }),
      ),
    )

    await renderRoute({
      route: '/admin/activities/1/reservations',
      adminReservationsComponent: AdminActivityReservationsView,
    })

    expect(await screen.findByText('We could not load activities right now.')).toBeTruthy()
  })
})
