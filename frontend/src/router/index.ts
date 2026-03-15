import { createRouter, createWebHistory } from 'vue-router'

import { pinia } from '@/lib/pinia'
import { useSessionStore } from '@/stores/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: { name: 'activities' },
    },
    {
      path: '/activities',
      name: 'activities',
      component: () => import('@/views/ActivitiesView.vue'),
      meta: {
        title: 'Public activities',
      },
    },
    {
      path: '/auth',
      name: 'auth',
      component: () => import('@/views/AuthView.vue'),
      meta: {
        title: 'Login or register',
      },
    },
  ],
})

router.beforeEach(async (to) => {
  const sessionStore = useSessionStore(pinia)

  if (!sessionStore.isHydrated) {
    await sessionStore.hydrate()
  }

  if (to.name === 'auth' && sessionStore.isAuthenticated) {
    return { name: 'activities' }
  }

  return true
})

router.afterEach((to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : 'Activities Club'
  document.title = `${pageTitle} | Activities Club`
})

export default router
