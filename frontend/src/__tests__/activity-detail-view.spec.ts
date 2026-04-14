import { screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import ActivityDetailView from '@/views/ActivityDetailView.vue'
import { buildActivity, sampleUser } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'
import { useSessionStore } from '@/stores/session'

describe('ActivityDetailView', () => {
  it('renders the hero and sticky detail card content with fallback text', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/1', () =>
        HttpResponse.json(
          buildActivity({
            description: null,
            organizer: null,
            startAt: null,
            endAt: null,
            locationName: null,
            locationAddress: null,
            reservationOpensAt: null,
            reservationClosesAt: null,
          }),
        ),
      ),
    )

    await renderRoute({
      route: '/activities/1',
      activityDetailComponent: ActivityDetailView,
    })

    expect((await screen.findAllByText('Chess Night')).length).toBeGreaterThan(0)
    expect(screen.getByText('Date and time')).toBeTruthy()
    expect(screen.getAllByText('Schedule to be announced').length).toBeGreaterThan(0)
    expect(screen.getByText('Location to be announced')).toBeTruthy()
    expect(
      screen.getByText(
        'More details are coming soon. Reserve your place to get the latest updates from the organizer.',
      ),
    ).toBeTruthy()
  })

  it('sends anonymous users to auth with a redirect back to the detail page', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/1', () => HttpResponse.json(buildActivity())),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/activities/1',
      activityDetailComponent: ActivityDetailView,
    })

    await user.click(await screen.findByRole('button', { name: 'Log in to attend' }))

    await waitFor(() => {
      expect(router.currentRoute.value.name).toBe('auth')
      expect(router.currentRoute.value.query.mode).toBe('login')
      expect(router.currentRoute.value.query.redirect).toBe('/activities/1')
    })
  })

  it('updates reservation state after attend and cancel actions', async () => {
    let currentStatus: 'RESERVED' | null = null
    let confirmedReservationCount = 1
    let waitlistCount = 0
    let availableSpots = 1
    let atCapacity = false

    server.use(
      http.get('http://localhost:8080/api/activities/1', () =>
        HttpResponse.json(
          buildActivity({
            confirmedReservationCount,
            waitlistCount,
            availableSpots,
            atCapacity,
            currentUserReservationStatus: currentStatus,
          }),
        ),
      ),
      http.post('http://localhost:8080/api/activities/1/reservations', () => {
        currentStatus = 'RESERVED'
        confirmedReservationCount = 2
        availableSpots = 0
        atCapacity = true

        return HttpResponse.json({
          activityId: 1,
          status: 'RESERVED',
          confirmedReservationCount,
          waitlistCount,
          availableSpots,
          atCapacity,
        })
      }),
      http.delete('http://localhost:8080/api/activities/1/reservations/me', () => {
        currentStatus = null
        confirmedReservationCount = 1
        availableSpots = 1
        atCapacity = false

        return HttpResponse.json({
          activityId: 1,
          status: 'CANCELLED',
          confirmedReservationCount,
          waitlistCount,
          availableSpots,
          atCapacity,
        })
      }),
    )

    const user = userEvent.setup()
    const { pinia } = await renderRoute({
      route: '/activities/1',
      activityDetailComponent: ActivityDetailView,
    })

    const sessionStore = useSessionStore(pinia)
    sessionStore.token = 'valid-token'
    sessionStore.user = sampleUser
    sessionStore.isHydrated = true

    await user.click(await screen.findByRole('button', { name: 'Attend' }))

    expect(await screen.findByRole('button', { name: 'Cancel reservation' })).toBeTruthy()
    expect((await screen.findAllByText('Capacity reached')).length).toBeGreaterThan(0)

    await user.click(screen.getByRole('button', { name: 'Cancel reservation' }))

    expect(await screen.findByRole('button', { name: 'Attend' })).toBeTruthy()
    expect((await screen.findAllByText('1 spot left')).length).toBeGreaterThan(0)
  })

  it('clears the session and redirects to auth when reservation returns 401', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/1', () => HttpResponse.json(buildActivity())),
      http.post(
        'http://localhost:8080/api/activities/1/reservations',
        () => new HttpResponse(null, { status: 401 }),
      ),
    )

    const user = userEvent.setup()
    const { pinia, router } = await renderRoute({
      route: '/activities/1',
      activityDetailComponent: ActivityDetailView,
    })

    const sessionStore = useSessionStore(pinia)
    sessionStore.token = 'expired-token'
    sessionStore.user = sampleUser
    sessionStore.isHydrated = true

    await user.click(await screen.findByRole('button', { name: 'Attend' }))

    await waitFor(() => {
      expect(sessionStore.isAuthenticated).toBe(false)
      expect(router.currentRoute.value.name).toBe('auth')
      expect(router.currentRoute.value.query.redirect).toBe('/activities/1')
    })
  })

  it('shows a not-found state for invalid activity ids', async () => {
    await renderRoute({
      route: '/activities/not-a-number',
      activityDetailComponent: ActivityDetailView,
    })

    expect(await screen.findByText('Activity not found')).toBeTruthy()
    expect(screen.getByText('This activity is no longer available.')).toBeTruthy()
  })

  it('shows a not-found state when the activity api returns 404', async () => {
    server.use(
      http.get(
        'http://localhost:8080/api/activities/99',
        () => new HttpResponse(null, { status: 404 }),
      ),
    )

    await renderRoute({
      route: '/activities/99',
      activityDetailComponent: ActivityDetailView,
    })

    expect(await screen.findByText('Activity not found')).toBeTruthy()
    expect(screen.getByText('This activity is no longer available.')).toBeTruthy()
  })
})
