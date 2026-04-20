import { screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import AdminActivitiesView from '@/views/AdminActivitiesView.vue'
import AdminActivityEditorView from '@/views/AdminActivityEditorView.vue'
import { buildActivity, buildPage } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

describe('AdminActivitiesView', () => {
  it('truncates long activity descriptions in the management cards', async () => {
    const longDescription = 'A'.repeat(320)

    server.use(
      http.get('http://localhost:8080/api/admin/activities', () =>
        HttpResponse.json(
          buildPage([
            buildActivity({
              description: longDescription,
            }),
          ]),
        ),
      ),
    )

    await renderRoute({
      route: '/admin/activities',
      adminComponent: AdminActivitiesView,
    })

    const truncatedDescription = `${'A'.repeat(280)}...`

    expect(await screen.findByText(truncatedDescription)).toBeTruthy()
    expect(screen.queryByText(longDescription)).toBeNull()
  })

  it('navigates from activity management to the edit page', async () => {
    server.use(
      http.get('http://localhost:8080/api/admin/activities', () =>
        HttpResponse.json(buildPage([buildActivity()])),
      ),
      http.get('http://localhost:8080/api/admin/activities/1', () =>
        HttpResponse.json(buildActivity()),
      ),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/admin/activities',
      adminComponent: AdminActivitiesView,
      adminEditorComponent: AdminActivityEditorView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()

    await user.click(screen.getByRole('link', { name: /Edit details/i }))

    await waitFor(() => {
      expect(router.currentRoute.value.fullPath).toBe('/admin/activities/1/edit')
    })

    expect(await screen.findByRole('heading', { name: 'Chess Night' })).toBeTruthy()
  })

  it('shows publish and delete again after cancelling a draft activity', async () => {
    let activity = buildActivity({
      status: 'DRAFT',
      confirmedReservationCount: 0,
      waitlistCount: 0,
      availableSpots: 20,
    })

    server.use(
      http.get('http://localhost:8080/api/admin/activities', () =>
        HttpResponse.json(buildPage([activity])),
      ),
      http.patch('http://localhost:8080/api/admin/activities/1/cancel', () => {
        activity = {
          ...activity,
          status: 'CANCELLED',
        }

        return HttpResponse.json(activity)
      }),
    )

    const user = userEvent.setup()
    await renderRoute({
      route: '/admin/activities',
      adminComponent: AdminActivitiesView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()
    expect(screen.getByRole('button', { name: /^Publish$/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /^Delete$/i })).toBeTruthy()

    await user.click(screen.getByRole('button', { name: /^Cancel$/i }))

    expect(await screen.findByText('CANCELLED')).toBeTruthy()
    expect(screen.getByRole('button', { name: /^Publish$/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /^Delete$/i })).toBeTruthy()
  })
})
