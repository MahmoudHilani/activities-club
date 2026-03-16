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
})
