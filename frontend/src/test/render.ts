import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { render } from '@testing-library/vue'
import { createPinia } from 'pinia'
import type { Component } from 'vue'
import { createRouter, createMemoryHistory } from 'vue-router'

const PlaceholderView = { template: '<div />' }

interface RenderRouteOptions {
  route: string
  authComponent?: Component
  activitiesComponent?: Component
  adminComponent?: Component
  adminEditorComponent?: Component
  adminReservationsComponent?: Component
}

export async function renderRoute({
  route,
  authComponent = PlaceholderView,
  activitiesComponent = PlaceholderView,
  adminComponent = PlaceholderView,
  adminEditorComponent = PlaceholderView,
  adminReservationsComponent = PlaceholderView,
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
        path: '/activities',
        name: 'activities',
        component: activitiesComponent,
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
