import { screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import ActivitiesView from '@/views/ActivitiesView.vue'
import ActivityDetailView from '@/views/ActivityDetailView.vue'
import { buildActivity, buildPage } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

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
    expect(screen.getByText('Schedule to be announced')).toBeTruthy()
    expect(screen.getByText('Location to be announced')).toBeTruthy()
    expect(screen.getByRole('link', { name: /Chess Night/i }).getAttribute('href')).toBe('/activities/1')
  })

  it('truncates long location labels on activity cards', async () => {
    const locationName = 'Student Union Building'
    const locationAddress = 'Room AAD;OGFHWGUQWHGN EHRG90WH 1234567890'
    const fullLocation = `${locationName} | ${locationAddress}`
    const truncatedLocation = `${fullLocation.slice(0, 42).trimEnd()}...`

    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(
          buildPage([
            buildActivity({
              locationName,
              locationAddress,
            }),
          ]),
        ),
      ),
    )

    await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    const locationLabel = await screen.findByTitle(fullLocation)

    expect(locationLabel.textContent).toBe(truncatedLocation)
    expect(screen.queryByText(fullLocation)).toBeNull()
  })

  it('only shows a card badge for overnight activities', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(
          buildPage([
            buildActivity({ id: 1, title: 'Chess Night', isOvernight: false }),
            buildActivity({ id: 2, title: 'Surf Weekend', isOvernight: true }),
          ]),
        ),
      ),
    )

    await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()
    expect(document.querySelectorAll('.craft-tag')).toHaveLength(1)
    const overnightBadge = screen.getByText(/overnight/i)
    expect(overnightBadge.querySelector('svg')).toBeTruthy()
    expect(overnightBadge.textContent?.trim()).toBe('overnight')
  })

  it('hides pagination controls when all activities fit on one page', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(
          buildPage([
            buildActivity({ id: 1, title: 'Chess Night' }),
            buildActivity({ id: 2, title: 'Open Mic' }),
          ]),
        ),
      ),
    )

    await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()
    expect(screen.getByText('Open Mic')).toBeTruthy()
    expect(screen.queryByText(/Page 1 of 1/i)).toBeNull()
    expect(screen.queryByRole('button', { name: /Previous/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Next/i })).toBeNull()
  })

  it('navigates to the activity detail page when a card is clicked', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(buildPage([buildActivity({ id: 7, title: 'Open Mic' })])),
      ),
      http.get('http://localhost:8080/api/activities/7', () =>
        HttpResponse.json(buildActivity({ id: 7, title: 'Open Mic' })),
      ),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
      activityDetailComponent: ActivityDetailView,
    })

    await user.click(await screen.findByRole('link', { name: /Open Mic/i }))

    await waitFor(() => {
      expect(router.currentRoute.value.name).toBe('activity-detail')
      expect(router.currentRoute.value.params.activityId).toBe('7')
    })
    expect(await screen.findByText('Date and time')).toBeTruthy()
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

  it('loads a homepage search from the route and sends it to the public feed', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', ({ request }) => {
        const query = new URL(request.url).searchParams

        expect(query.get('q')).toBe('yoga')
        return HttpResponse.json(buildPage([buildActivity({ title: 'Slow Yoga' })]))
      }),
    )

    await renderRoute({
      route: '/activities?q=yoga',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByDisplayValue('yoga')).toBeTruthy()
    expect(await screen.findByText('Slow Yoga')).toBeTruthy()
  })

  it('submits a new search on the activities page and resets pagination', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', ({ request }) => {
        const query = new URL(request.url).searchParams
        const search = query.get('q')

        if (search === 'climb') {
          expect(query.get('page')).toBe('0')
          return HttpResponse.json(buildPage([buildActivity({ title: 'Climbing Workshop' })]))
        }

        return HttpResponse.json(
          buildPage([buildActivity()], {
            totalPages: 2,
            last: false,
          }),
        )
      }),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/activities?page=2',
      activitiesComponent: ActivitiesView,
    })

    const input = await screen.findByRole('searchbox', { name: 'Search activities' })
    await user.type(input, 'climb')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await waitFor(() => {
      expect(router.currentRoute.value.query.q).toBe('climb')
      expect(router.currentRoute.value.query.page).toBeUndefined()
    })
    expect(await screen.findByText('Climbing Workshop')).toBeTruthy()
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
      http.get(
        'http://localhost:8080/api/activities/public',
        () => new HttpResponse(null, { status: 500 }),
      ),
    )

    await renderRoute({
      route: '/activities',
      activitiesComponent: ActivitiesView,
    })

    expect(await screen.findByText('We could not load activities right now.')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeTruthy()
  })
})
