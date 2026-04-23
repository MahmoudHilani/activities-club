import { createRouter, createWebHistory } from 'vue-router'

import { resolveRedirectPath } from '@/lib/redirect'
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
      path: '/activities/:activityId',
      name: 'activity-detail',
      component: () => import('@/views/ActivityDetailView.vue'),
      meta: {
        title: 'Activity details',
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
    {
      path: '/admin/activities',
      name: 'admin-activities',
      component: () => import('@/views/AdminActivitiesView.vue'),
      meta: {
        title: 'Activity management',
        requiresAdmin: true,
      },
    },
    {
      path: '/admin/activities/new',
      name: 'admin-activity-create',
      component: () => import('@/views/AdminActivityEditorView.vue'),
      meta: {
        title: 'Create activity',
        requiresAdmin: true,
      },
    },
    {
      path: '/admin/activities/:activityId/edit',
      name: 'admin-activity-edit',
      component: () => import('@/views/AdminActivityEditorView.vue'),
      meta: {
        title: 'Edit activity',
        requiresAdmin: true,
      },
    },
    {
      path: '/admin/activities/:activityId/reservations',
      name: 'admin-activity-reservations',
      component: () => import('@/views/AdminActivityReservationsView.vue'),
      meta: {
        title: 'Activity reservations',
        requiresAdmin: true,
      },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/AdminUsersView.vue'),
      meta: {
        title: 'User management',
        requiresAdmin: true,
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
    return resolveRedirectPath(to.query.redirect)
  }

  if (to.meta.requiresAdmin) {
    if (!sessionStore.isAuthenticated) {
      return { name: 'auth', query: { mode: 'login' } }
    }

    if (!sessionStore.user?.isAdmin) {
      return { name: 'activities' }
    }
  }

  return true
})

router.afterEach((to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : 'Activities Club'
  document.title = `${pageTitle} | Activities Club`
})

export default router
