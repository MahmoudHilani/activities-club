import { screen, waitFor, within } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import router from '@/router'
import AdminActivitiesView from '@/views/AdminActivitiesPrototypeView.vue'
import AdminActivityEditorView from '@/views/AdminActivityEditorView.vue'
import { buildActivity, buildPage } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

describe('AdminActivitiesView', () => {
  it('uses the promoted activities prototype for the main admin route', async () => {
    const componentLoader = router.resolve('/admin/activities').matched[0]?.components
      ?.default as () => Promise<{ default: unknown }>

    const component = await componentLoader()

    expect(component.default).toBe(AdminActivitiesView)
  })

  it('filters activities using the management search', async () => {

    server.use(
      http.get('http://localhost:8080/api/admin/activities', () =>
        HttpResponse.json(
          buildPage([
            buildActivity({ id: 1, title: 'Chess Night' }),
            buildActivity({ id: 2, title: 'Open Mic' }),
          ]),
        ),
      ),
    )

    const user = userEvent.setup()
    await renderRoute({
      route: '/admin/activities',
      adminComponent: AdminActivitiesView,
    })

    expect(await screen.findByText('Chess Night')).toBeTruthy()
    expect(screen.getByText('Open Mic')).toBeTruthy()
    expect(screen.queryByText('prototype')).toBeNull()

    await user.type(screen.getByPlaceholderText(/Search by title/i), 'open')

    expect(screen.queryByText('Chess Night')).toBeNull()
    expect(screen.getByText('Open Mic')).toBeTruthy()
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

    await user.click(screen.getByRole('link', { name: /^Edit$/i }))

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
    await user.click(screen.getByRole('button', { name: /More actions/i }))
    expect(screen.getByRole('button', { name: /^Publish$/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /^Delete$/i })).toBeTruthy()

    await user.click(screen.getByRole('button', { name: /Cancel activity/i }))
    await user.click(
      within(screen.getByRole('dialog', { name: /Cancel "Chess Night"/i })).getByRole(
        'button',
        { name: /Cancel activity/i },
      ),
    )

    expect(await screen.findByText('cancelled')).toBeTruthy()
    await user.click(screen.getByRole('button', { name: /More actions/i }))
    expect(screen.getByRole('button', { name: /^Publish$/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /^Delete$/i })).toBeTruthy()
  })
})
