<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { CalendarRange, ClipboardList, LoaderCircle, PencilLine, Rocket, Trash2 } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import { Alert } from '@/components/ui/alert'
import { buttonVariants, Button } from '@/components/ui/button'
import {
  cancelAdminActivity,
  deleteAdminActivity,
  getAdminActivities,
  publishAdminActivity,
} from '@/lib/api/activities'
import { mapActivitiesError } from '@/lib/api/errors'
import type { ActivityResponse } from '@/lib/api/types'
import { formatAvailability, formatDateRange, formatTicketPrice } from '@/lib/formatters'

const queryClient = useQueryClient()

const activitiesQuery = useQuery(() => ({
  queryKey: ['admin-activities'],
  queryFn: () => getAdminActivities(),
}))

const activities = computed(() => activitiesQuery.data.value?.content ?? [])
const summaryCards = computed(() => {
  const drafts = activities.value.filter((activity) => activity.status === 'DRAFT').length
  const published = activities.value.filter((activity) => activity.status === 'PUBLISHED').length
  const cancelled = activities.value.filter((activity) => activity.status === 'CANCELLED').length

  return [
    { label: 'Total activities', value: activities.value.length },
    { label: 'Drafts', value: drafts },
    { label: 'Published', value: published },
    { label: 'Cancelled', value: cancelled },
  ]
})

const publishMutation = useMutation(() => ({
  mutationFn: (activityId: number) => publishAdminActivity(activityId),
  onSuccess: invalidateActivityQueries,
}))

const cancelMutation = useMutation(() => ({
  mutationFn: (activityId: number) => cancelAdminActivity(activityId),
  onSuccess: invalidateActivityQueries,
}))

const deleteMutation = useMutation(() => ({
  mutationFn: (activityId: number) => deleteAdminActivity(activityId),
  onSuccess: invalidateActivityQueries,
}))

function canDelete(activity: ActivityResponse): boolean {
  return activity.status === 'DRAFT' && activity.confirmedReservationCount === 0 && activity.waitlistCount === 0
}

async function invalidateActivityQueries(): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-activities'] }),
    queryClient.invalidateQueries({ queryKey: ['public-activities'] }),
  ])
}
</script>

<template>
  <section class="flex flex-1 flex-col gap-8">
    <div class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr] xl:items-end">
      <div class="space-y-5">
        <p class="inline-flex rounded-full bg-white/80 px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          Activity management
        </p>
        <h1 class="headline-balance max-w-3xl font-serif text-5xl font-bold tracking-tight text-foreground sm:text-6xl">
          Keep drafts, publishing, and reservation access under control.
        </h1>
        <p class="max-w-2xl text-lg leading-8 text-muted-foreground">
          Review every activity in one place, then jump into the dedicated editor when details need work.
        </p>
      </div>

      <div class="surface-panel rounded-[1.75rem] border border-white/70 p-6">
        <div class="flex items-start gap-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
            <ClipboardList class="h-5 w-5" />
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-sm font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Admin workspace
            </p>
            <p class="mt-2 text-2xl font-bold tracking-tight text-foreground">
              Separate creation and management flows
            </p>
            <p class="mt-2 text-sm leading-7 text-muted-foreground">
              Use the floating plus button from anywhere, or open the full editor here.
            </p>
            <RouterLink
              :class="buttonVariants({ size: 'sm' })"
              :to="{ name: 'admin-activity-create' }"
            >
              Open activity editor
            </RouterLink>
          </div>
        </div>
      </div>
    </div>

    <Alert v-if="activitiesQuery.isError.value" variant="destructive">
      {{ mapActivitiesError(activitiesQuery.error.value) }}
    </Alert>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div
        v-for="card in summaryCards"
        :key="card.label"
        class="surface-panel rounded-[1.6rem] border border-white/70 p-5"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
          {{ card.label }}
        </p>
        <p class="mt-3 text-3xl font-bold tracking-tight text-foreground">{{ card.value }}</p>
      </div>
    </div>

    <div
      v-if="activitiesQuery.isPending.value"
      class="surface-panel rounded-[2rem] border border-white/70 p-8 text-muted-foreground"
    >
      Loading activity management...
    </div>

    <div
      v-else-if="activities.length === 0"
      class="surface-panel rounded-[2rem] border border-white/70 px-6 py-10 text-center"
    >
      <p class="text-lg font-semibold text-foreground">No activities have been created yet.</p>
      <p class="mt-2 text-sm text-muted-foreground">
        Start the first draft from the floating plus button or the editor shortcut above.
      </p>
    </div>

    <div v-else class="grid gap-5">
      <article
        v-for="activity in activities"
        :key="activity.id"
        class="surface-panel rounded-[1.85rem] border border-white/70 p-5 sm:p-6"
      >
        <div class="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
          <div class="flex min-w-0 flex-1 gap-4">
            <img
              :src="activity.imageUrl"
              :alt="activity.title"
              class="h-24 w-24 shrink-0 rounded-2xl object-cover sm:h-28 sm:w-28"
            />

            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2">
                <h2 class="break-words text-xl font-bold text-foreground sm:text-2xl">
                  {{ activity.title }}
                </h2>
                <span class="rounded-full bg-secondary px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-secondary-foreground">
                  {{ activity.status }}
                </span>
                <span class="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                  {{ formatTicketPrice(activity.ticketPrice) }}
                </span>
              </div>

              <p class="mt-3 break-words text-sm leading-6 text-muted-foreground">
                {{ activity.description || 'No description added yet.' }}
              </p>

              <div class="mt-4 grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
                <p class="flex items-start gap-2 break-words">
                  <CalendarRange class="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                  <span>{{ formatDateRange(activity.startAt, activity.endAt) }}</span>
                </p>
                <p class="flex items-start gap-2 break-words">
                  <Rocket class="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                  <span>
                    {{ activity.confirmedReservationCount }} confirmed, {{ activity.waitlistCount }} waitlisted,
                    {{ formatAvailability(activity.availableSpots, activity.atCapacity) }}
                  </span>
                </p>
              </div>
            </div>
          </div>

          <div class="flex w-full flex-wrap gap-2 xl:max-w-[21rem] xl:justify-end">
            <RouterLink
              :class="buttonVariants({ size: 'sm' })"
              :to="{ name: 'admin-activity-edit', params: { activityId: activity.id } }"
            >
              <PencilLine class="h-4 w-4" />
              Edit details
            </RouterLink>
            <RouterLink
              :class="buttonVariants({ variant: 'outline', size: 'sm' })"
              :to="{ name: 'admin-activity-reservations', params: { activityId: activity.id } }"
            >
              View reservations
            </RouterLink>
            <Button
              v-if="activity.status === 'DRAFT'"
              :disabled="publishMutation.isPending.value"
              size="sm"
              variant="outline"
              @click="publishMutation.mutate(activity.id)"
            >
              <LoaderCircle v-if="publishMutation.isPending.value" class="h-4 w-4 animate-spin" />
              Publish
            </Button>
            <Button
              v-if="activity.status !== 'CANCELLED' && activity.status !== 'COMPLETED'"
              :disabled="cancelMutation.isPending.value"
              size="sm"
              variant="outline"
              @click="cancelMutation.mutate(activity.id)"
            >
              <LoaderCircle v-if="cancelMutation.isPending.value" class="h-4 w-4 animate-spin" />
              Cancel
            </Button>
            <Button
              v-if="canDelete(activity)"
              :disabled="deleteMutation.isPending.value"
              size="sm"
              variant="destructive"
              @click="deleteMutation.mutate(activity.id)"
            >
              <Trash2 class="h-4 w-4" />
              Delete
            </Button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
