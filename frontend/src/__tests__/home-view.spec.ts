import { screen } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import HomeView from '@/views/HomeView.vue'
import { buildActivity, buildPage } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

describe('HomeView', () => {
  it('shows public activities from the backend and links to their details', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', ({ request }) => {
        const query = new URL(request.url).searchParams

        expect(query.get('page')).toBe('0')
        expect(query.get('size')).toBe('3')

        return HttpResponse.json(
          buildPage([buildActivity({ id: 7, title: 'Climbing Workshop' })], {
            totalElements: 5,
          }),
        )
      }),
    )

    await renderRoute({
      route: '/',
      homeComponent: HomeView,
    })

    expect(await screen.findByText('Climbing Workshop')).toBeTruthy()
    expect(screen.getByText('5 activities to explore')).toBeTruthy()
    expect(screen.getByRole('link', { name: /Climbing Workshop/i }).getAttribute('href')).toBe(
      '/activities/7',
    )
    expect(screen.queryByText('Sunrise Spinc loop')).toBeNull()
  })

  it('shows an empty state when there are no published activities', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(buildPage([])),
      ),
    )

    await renderRoute({
      route: '/',
      homeComponent: HomeView,
    })

    expect(await screen.findByText('No public activities are published yet.')).toBeTruthy()
  })

  it('sends a homepage search to the activities page', async () => {
    server.use(
      http.get('http://localhost:8080/api/activities/public', () =>
        HttpResponse.json(buildPage([])),
      ),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/',
      homeComponent: HomeView,
    })

    await user.type(screen.getByRole('textbox', { name: 'Search activities' }), '  yoga  ')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(router.currentRoute.value.name).toBe('activities')
    expect(router.currentRoute.value.query.q).toBe('yoga')
  })

  it('shows a retry action when homepage activities cannot be loaded', async () => {
    server.use(
      http.get(
        'http://localhost:8080/api/activities/public',
        () => new HttpResponse(null, { status: 500 }),
      ),
    )

    await renderRoute({
      route: '/',
      homeComponent: HomeView,
    })

    expect(await screen.findByText('We could not load activities right now.')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeTruthy()
  })
})
