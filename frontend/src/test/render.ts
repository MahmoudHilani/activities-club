import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { render } from '@testing-library/vue'
import { createPinia } from 'pinia'
import type { Component } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'

const PlaceholderView = { template: '<div />' }

interface RenderRouteOptions {
  route: string
  authComponent?: Component
  registrationAppealComponent?: Component
  activitiesComponent?: Component
  activityDetailComponent?: Component
  adminComponent?: Component
  adminEditorComponent?: Component
  adminReservationsComponent?: Component
  adminUsersComponent?: Component
}

export async function renderRoute({
  route,
  authComponent = PlaceholderView,
  registrationAppealComponent = PlaceholderView,
  activitiesComponent = PlaceholderView,
  activityDetailComponent = PlaceholderView,
  adminComponent = PlaceholderView,
  adminEditorComponent = PlaceholderView,
  adminReservationsComponent = PlaceholderView,
  adminUsersComponent = PlaceholderView,
}: RenderRouteOptions) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/auth',
        name: 'auth',
        component: authComponent,
      },
      {
        path: '/auth/appeal',
        name: 'registration-appeal',
        component: registrationAppealComponent,
      },
      {
        path: '/activities',
        name: 'activities',
        component: activitiesComponent,
      },
      {
        path: '/activities/:activityId',
        name: 'activity-detail',
        component: activityDetailComponent,
      },
      {
        path: '/admin/activities',
        name: 'admin-activities',
        component: adminComponent,
      },
      {
        path: '/admin/activities/new',
        name: 'admin-activity-create',
        component: adminEditorComponent,
      },
      {
        path: '/admin/activities/:activityId/edit',
        name: 'admin-activity-edit',
        component: adminEditorComponent,
      },
      {
        path: '/admin/activities/:activityId/reservations',
        name: 'admin-activity-reservations',
        component: adminReservationsComponent,
      },
      {
        path: '/admin/users',
        name: 'admin-users',
        component: adminUsersComponent,
      },
    ],
  })

  const pinia = createPinia()
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  await router.push(route)
  await router.isReady()

  const result = render(
    {
      template: '<RouterView />',
    },
    {
      global: {
        plugins: [pinia, [VueQueryPlugin, { queryClient }], router],
      },
    },
  )

  return {
    ...result,
    router,
    pinia,
    queryClient,
  }
}
