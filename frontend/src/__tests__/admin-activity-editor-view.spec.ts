import { fireEvent, screen, waitFor } from '@testing-library/vue'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'

import AdminActivityEditorView from '@/views/AdminActivityEditorView.vue'
import { buildActivity } from '@/test/fixtures'
import { renderRoute } from '@/test/render'
import { server } from '@/test/server'

describe('AdminActivityEditorView', () => {
  it('submits visibility changes through the separate create route', async () => {
    const createdActivity = buildActivity({
      id: 2,
      title: 'Members Night',
      status: 'DRAFT',
      visibility: 'PRIVATE',
    })
    let createRequestCount = 0

    server.use(
      http.post('http://localhost:8080/api/admin/activities', () => {
        createRequestCount += 1
        return HttpResponse.json(createdActivity)
      }),
      http.get('http://localhost:8080/api/admin/activities/2', () => HttpResponse.json(createdActivity)),
    )

    const user = userEvent.setup()
    const { router } = await renderRoute({
      route: '/admin/activities/new',
      adminEditorComponent: AdminActivityEditorView,
    })

    await screen.findByRole('heading', { name: 'Create a new activity draft' })

    await user.type(screen.getByLabelText('Title'), 'Members Night')
    await user.upload(
      screen.getByLabelText('Activity image'),
      new File(['image-bytes'], 'members-night.png', { type: 'image/png' }),
    )

    await user.click(screen.getByRole('combobox', { name: 'Visibility' }))
    await user.click(await screen.findByRole('option', { name: 'Private' }))
    expect(screen.getByRole('combobox', { name: 'Visibility' }).textContent).toContain('Private')

    const submitButton = screen.getByRole('button', { name: 'Create activity draft' })
    const form = submitButton.closest('form')

    expect(form).toBeTruthy()
    await fireEvent.submit(form as HTMLFormElement)

    await waitFor(() => {
      expect(createRequestCount).toBe(1)
      expect(router.currentRoute.value.fullPath).toBe('/admin/activities/2/edit')
    })

    expect(await screen.findByRole('heading', { name: 'Members Night' })).toBeTruthy()
    expect(screen.getByRole('combobox', { name: 'Visibility' }).textContent).toContain('Private')
  })
})
