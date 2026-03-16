import { screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import AdminActivitiesView from '@/views/AdminActivitiesView.vue'
import { buildActivity, buildPage } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

describe('AdminActivitiesView', () => {
  it('submits visibility changes through the shadcn select', async () => {
    server.use(
      http.get('http://localhost:8080/api/admin/activities', () =>
        HttpResponse.json(buildPage([])),
      ),
      http.post('http://localhost:8080/api/admin/activities', () => {
        return HttpResponse.json(
          buildActivity({
            id: 2,
            title: 'Members Night',
            status: 'DRAFT',
            visibility: 'PRIVATE',
          }),
        )
      }),
    )

    const user = userEvent.setup()

    await renderRoute({
      route: '/admin/activities',
      adminComponent: AdminActivitiesView,
    })

    await screen.findByRole('heading', { name: 'New activity draft' })

    await user.type(screen.getByLabelText('Title'), 'Members Night')
    await user.upload(
      screen.getByLabelText('Activity image'),
      new File(['image-bytes'], 'members-night.png', { type: 'image/png' }),
    )

    await user.click(screen.getByRole('combobox', { name: 'Visibility' }))
    await user.click(await screen.findByRole('option', { name: 'Private' }))
    expect(screen.getByRole('combobox', { name: 'Visibility' }).textContent).toContain('Private')

    await user.click(screen.getByRole('button', { name: 'Create activity draft' }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Members Night' })).toBeTruthy()
      expect(screen.getByRole('combobox', { name: 'Visibility' }).textContent).toContain('Private')
    })
  })
})
