import { render, screen } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import AdminCreateActivityFab from '@/components/admin/AdminCreateActivityFab.vue'
import { useSessionStore } from '@/stores/session'
import { sampleUser } from '@/test/fixtures'

describe('AdminCreateActivityFab', () => {
  it('renders the floating create shortcut for admins', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/activities',
          name: 'activities',
          component: { template: '<div />' },
        },
        {
          path: '/admin/activities/new',
          name: 'admin-activity-create',
          component: { template: '<div />' },
        },
      ],
    })

    const sessionStore = useSessionStore(pinia)
    sessionStore.isHydrated = true
    sessionStore.token = 'token'
    sessionStore.user = {
      ...sampleUser,
      role: 'ADMIN',
    }

    await router.push('/activities')
    await router.isReady()

    render(AdminCreateActivityFab, {
      global: {
        plugins: [pinia, router],
      },
    })

    const createLink = screen.getByRole('link', { name: 'Create activity' })
    expect(createLink.getAttribute('href')).toBe('/admin/activities/new')
  })
})
