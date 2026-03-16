import { screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import ActivitiesView from '@/views/ActivitiesView.vue'
import { sampleUser, buildActivity, buildPage } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'
import { useSessionStore } from '@/stores/session'

describe('ActivitiesView', () => {
  it('renders fallback text for nullable activity fields', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(
          buildPage([
            buildActivity({
              description: null,
              organizer: null,
              startAt: null,
              endAt: null,
              locationName: null,
              locationAddress: null,
              capacity: null,
              ticketPrice: '0',
              availableSpots: null,
              atCapacity: false,
              reservationOpensAt: null,
              reservationClosesAt: null,
            }),
          ]),
        ),
      ),
    )

    await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()
    expect(screen.getByText('More details are coming soon for this activity.')).toBeTruthy()
    expect(screen.getByText(/Hosted by Community host/)).toBeTruthy()
    expect(screen.getByText('Schedule to be announced')).toBeTruthy()
    expect(screen.getByText('Location to be announced')).toBeTruthy()
    expect(screen.getByText('Open capacity')).toBeTruthy()
  })

  it('supports pagination through the route query', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? '0')

        if (page === 1) {
          return HttpResponse.json(
            buildPage([buildActivity({ id: 2, title: 'Open Mic' })], {
              number: 1,
              totalPages: 2,
              totalElements: 2,
              first: false,
              last: true,
            }),
          )
        }

        return HttpResponse.json(
          buildPage([buildActivity({ id: 1, title: 'Chess Night' })], {
            number: 0,
            totalPages: 2,
            totalElements: 2,
            first: true,
            last: false,
          }),
        )
      }),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()

    await user.click(screen.getByRole('button', { name: /Next/i }))

    await waitFor(() => {
      expect(router.currentRoute.value.query.page).toBe('2')
    })
    expect(await screen.findByText('Open Mic')).toBeTruthy()
  })

  it('shows the empty state when no activities are returned', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(
          buildPage([], {
            totalPages: 0,
            totalElements: 0,
            empty: true,
          }),
        ),
      ),
    )

    await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByText('No public activities are published yet.')).toBeTruthy()
  })

  it('shows a retryable error state when the feed fails', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () => new HttpResponse(null, { status: 500 })),
    )

    await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByText('We could not load activities right now.')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeTruthy()
  })

  it('clears the session and redirects to login when reservation returns 401', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(buildPage([buildActivity()])),
      ),
      http.post('http://localhost:8080/api/activities/1/reservations', () =>
        new HttpResponse(null, { status: 401 }),
      ),
    )

    const user = userEvent.setup()
    const { pinia, router } = await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    const sessionStore = useSessionStore(pinia)
    sessionStore.token = 'expired-token'
    sessionStore.user = sampleUser
    sessionStore.isHydrated = true

    await user.click(await screen.findByRole('button', { name: 'Reserve seat' }))

    await waitFor(() => {
      expect(sessionStore.isAuthenticated).toBe(false)
      expect(router.currentRoute.value.name).toBe('auth')
    })
  })
})
