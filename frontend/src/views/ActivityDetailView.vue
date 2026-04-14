<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ArrowLeft, CalendarDays, LoaderCircle, MapPin, Ticket, Users } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  cancelReservation,
  getAdminActivity,
  getPublicActivity,
  reserveActivity,
} from '@/lib/api/activities'
import { getApiStatus, mapActivitiesError, mapReservationError } from '@/lib/api/errors'
import {
  formatAvailability,
  formatDateRange,
  formatDateStart,
  formatLocation,
  formatReservationWindow,
  formatTicketPrice,
} from '@/lib/formatters'
import { useSessionStore } from '@/stores/session'

type ReservationAction = 'reserve' | 'cancel'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const sessionStore = useSessionStore()
const reservationError = ref('')

function parseActivityId(value: unknown): number | null {
  const parsed = Number(Array.isArray(value) ? value[0] : value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

const activityId = computed(() => parseActivityId(route.params.activityId))
const detailPath = computed(() => route.fullPath)
const isAdmin = computed(() => sessionStore.user?.role === 'ADMIN')
const detailQueryKey = computed(() => [
  'activity-detail',
  activityId.value,
  sessionStore.user?.id ?? 'anonymous',
  sessionStore.user?.role ?? 'anonymous',
] as const)

const activityQuery = useQuery(() => ({
  enabled: activityId.value !== null,
  queryKey: detailQueryKey.value,
  queryFn: async () => {
    const nextActivityId = activityId.value ?? 0

    try {
      return await getPublicActivity(nextActivityId)
    } catch (error) {
      if (isAdmin.value && getApiStatus(error) === 404) {
        return getAdminActivity(nextActivityId)
      }

      throw error
    }
  },
}))

const activity = computed(() => activityQuery.data.value ?? null)
const isInvalidRoute = computed(() => activityId.value === null)
const isNotFound = computed(
  () => isInvalidRoute.value || getApiStatus(activityQuery.error.value) === 404,
)
const isInitialLoading = computed(
  () => activityQuery.isPending.value && activity.value === null && !isInvalidRoute.value,
)
const scheduleHeadline = computed(() =>
  formatDateStart(activity.value?.startAt, activity.value?.endAt),
)
const scheduleRange = computed(() =>
  formatDateRange(activity.value?.startAt, activity.value?.endAt),
)
const ticketLabel = computed(() => formatTicketPrice(activity.value?.ticketPrice))
const availabilityLabel = computed(() =>
  formatAvailability(activity.value?.availableSpots, activity.value?.atCapacity),
)
const reservationWindowLabel = computed(() =>
  formatReservationWindow(activity.value?.reservationOpensAt, activity.value?.reservationClosesAt),
)
const isAdminPreview = computed(() => {
  if (!activity.value || !isAdmin.value) {
    return false
  }

  return activity.value.status !== 'PUBLISHED' || activity.value.visibility !== 'PUBLIC'
})
const isCtaDisabled = computed(() => isAdmin.value)
const descriptionText = computed(
  () =>
    activity.value?.description ||
    'More details are coming soon. Reserve your place to get the latest updates from the organizer.',
)
const currentReservationStatus = computed(
  () => activity.value?.currentUserReservationStatus ?? null,
)
const isCancelAction = computed(
  () =>
    currentReservationStatus.value === 'RESERVED' ||
    currentReservationStatus.value === 'WAITLISTED',
)
const ctaLabel = computed(() => {
  if (!activity.value) {
    return 'Attend'
  }

  if (isAdmin.value) {
    return 'Admin preview only'
  }

  if (!sessionStore.isAuthenticated) {
    return 'Log in to attend'
  }

  if (currentReservationStatus.value === 'RESERVED') {
    return 'Cancel reservation'
  }

  if (currentReservationStatus.value === 'WAITLISTED') {
    return 'Leave waitlist'
  }

  return activity.value.atCapacity ? 'Join waitlist' : 'Attend'
})
const reservationStatusLabel = computed(() => {
  if (currentReservationStatus.value === 'RESERVED') {
    return 'You have a confirmed spot.'
  }

  if (currentReservationStatus.value === 'WAITLISTED') {
    return 'You are currently on the waitlist.'
  }

  if (isAdmin.value) {
    return 'Previewing this page as an admin.'
  }

  if (activity.value?.atCapacity) {
    return 'Reservations are open through the waitlist.'
  }

  return 'Reservations are currently open.'
})

const reservationMutation = useMutation({
  mutationFn: async (action: ReservationAction) => {
    if (activityId.value === null) {
      throw new Error('Activity id is missing')
    }

    return action === 'cancel'
      ? cancelReservation(activityId.value)
      : reserveActivity(activityId.value)
  },
})

async function handlePrimaryAction(): Promise<void> {
  if (!activity.value || isCtaDisabled.value) {
    return
  }

  reservationError.value = ''

  if (!sessionStore.isAuthenticated) {
    await router.push({
      name: 'auth',
      query: {
        mode: 'login',
        redirect: detailPath.value,
      },
    })
    return
  }

  try {
    const result = await reservationMutation.mutateAsync(
      isCancelAction.value ? 'cancel' : 'reserve',
    )

    const currentActivity = activity.value
    if (currentActivity && activityId.value !== null) {
      queryClient.setQueryData(detailQueryKey.value, {
        ...currentActivity,
        confirmedReservationCount: result.confirmedReservationCount,
        waitlistCount: result.waitlistCount,
        availableSpots: result.availableSpots,
        atCapacity: result.atCapacity,
        currentUserReservationStatus: result.status === 'CANCELLED' ? null : result.status,
      })
    }

    await queryClient.invalidateQueries({ queryKey: ['public-activities'] })
  } catch (error) {
    reservationError.value = mapReservationError(error)

    if (getApiStatus(error) === 401) {
      sessionStore.clearSession()
      await router.push({
        name: 'auth',
        query: {
          mode: 'login',
          redirect: detailPath.value,
        },
      })
    }
  }
}
</script>

<template>
  <section class="flex flex-1 flex-col gap-8 pb-36 sm:pb-40">
    <div v-if="isInitialLoading" class="space-y-8">
      <div class="grid gap-8 xl:grid-cols-[minmax(0,1fr)_34rem]">
        <div class="space-y-5">
          <div class="h-8 w-36 rounded-full bg-muted animate-pulse" />
          <div class="h-16 w-4/5 rounded-[2rem] bg-muted animate-pulse" />
          <div class="h-6 w-2/3 rounded-full bg-muted animate-pulse" />
          <div class="grid gap-4 sm:grid-cols-3">
            <div
              v-for="placeholder in 3"
              :key="placeholder"
              class="h-28 rounded-[1.75rem] bg-muted animate-pulse"
            />
          </div>
        </div>

        <div class="aspect-[1.28/1] rounded-[2.25rem] bg-muted animate-pulse" />
      </div>

      <div class="grid gap-8 xl:grid-cols-[minmax(0,1fr)_34rem]">
        <div class="space-y-5">
          <div
            v-for="placeholder in 3"
            :key="placeholder"
            class="h-40 rounded-[2rem] bg-muted animate-pulse"
          />
        </div>
        <div class="h-72 rounded-[2rem] bg-muted animate-pulse" />
      </div>
    </div>

    <div
      v-else-if="isNotFound"
      class="surface-panel rounded-[2rem] border border-white/70 px-6 py-10 shadow-[0_20px_50px_rgba(15,23,42,0.08)] sm:px-8"
    >
      <p class="text-sm font-semibold uppercase tracking-[0.22em] text-muted-foreground">
        Activity not found
      </p>
      <h1 class="mt-3 max-w-xl font-serif text-4xl font-bold tracking-tight text-foreground">
        This activity is no longer available.
      </h1>
      <p class="mt-4 max-w-2xl text-base leading-8 text-muted-foreground">
        It may have been unpublished, cancelled, or the link may be incorrect.
      </p>

      <div class="mt-8">
        <RouterLink :to="{ name: 'activities' }">
          <Button variant="outline">
            <ArrowLeft class="h-4 w-4" />
            Back to activities
          </Button>
        </RouterLink>
      </div>
    </div>

    <Alert
      v-else-if="activityQuery.isError.value"
      class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between"
      variant="destructive"
    >
      <span>{{ mapActivitiesError(activityQuery.error.value) }}</span>
      <Button size="sm" variant="outline" @click="activityQuery.refetch()"> Try again </Button>
    </Alert>

    <article v-else-if="activity">
      <div class="grid gap-8 xl:grid-cols-[minmax(0,1fr)_34rem] xl:items-start">
        <div class="space-y-6">
          <div class="space-y-6">
            <RouterLink
              :to="{ name: 'activities' }"
              class="inline-flex items-center gap-2 rounded-full bg-white/80 px-4 py-2 text-sm font-semibold text-muted-foreground transition hover:bg-white hover:text-foreground"
            >
              <ArrowLeft class="h-4 w-4" />
              Back to activities
            </RouterLink>

            <div class="space-y-5">
              <h1
                class="headline-balance max-w-3xl font-serif text-5xl font-bold tracking-[-0.04em] text-foreground sm:text-6xl"
              >
                {{ activity.title }}
              </h1>
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-3">
            <div class="surface-panel rounded-[1.6rem] border border-white/65 p-5">
              <CalendarDays class="h-5 w-5 text-primary" />
              <p
                class="mt-4 text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground"
              >
                Date
              </p>
              <p class="mt-2 text-lg font-bold text-foreground">
                {{ scheduleHeadline }}
              </p>
            </div>

            <div class="surface-panel rounded-[1.6rem] border border-white/65 p-5">
              <Users class="h-5 w-5 text-primary" />
              <p
                class="mt-4 text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground"
              >
                Availability
              </p>
              <p class="mt-2 text-lg font-bold text-foreground">
                {{ availabilityLabel }}
              </p>
            </div>

            <div class="surface-panel rounded-[1.6rem] border border-white/65 p-5">
              <Ticket class="h-5 w-5 text-primary" />
              <p
                class="mt-4 text-sm font-semibold uppercase tracking-[0.18em] text-muted-foreground"
              >
                Reservation
              </p>
              <p class="mt-2 text-lg font-bold text-foreground">
                {{ reservationStatusLabel }}
              </p>
            </div>
          </div>
          <Alert v-if="isAdminPreview" class="border-primary/25 bg-primary/8 text-foreground">
            This activity is not publicly available yet. You are seeing an admin-only preview of the
            detail page.
          </Alert>

          <div
            class="surface-panel rounded-[2rem] border border-white/70 p-6 shadow-[0_20px_50px_rgba(15,23,42,0.08)] sm:p-8"
          >
            <p class="text-sm font-semibold uppercase tracking-[0.22em] text-muted-foreground">
              Details
            </p>
            <h2 class="mt-3 text-3xl font-bold tracking-tight text-foreground">What to expect</h2>
            <p class="mt-5 whitespace-pre-line text-base leading-8 text-slate-700">
              {{ descriptionText }}
            </p>
          </div>
        </div>

        <div class="space-y-8">
          <div
            class="surface-panel overflow-hidden rounded-[2.5rem] border border-white/70 p-3 shadow-[0_28px_60px_rgba(15,23,42,0.1)]"
          >
            <img
              :src="activity.imageUrl"
              :alt="activity.title"
              class="aspect-[1.2/1] w-full rounded-[2rem] object-cover"
            />
          </div>

          <aside class="w-full xl:sticky xl:top-28">
            <div
              class="surface-panel w-full rounded-[2rem] border border-white/70 p-6 shadow-[0_20px_50px_rgba(15,23,42,0.08)]"
            >
              <div class="flex items-start gap-4">
                <div
                  class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-rose-100 text-rose-600"
                >
                  <CalendarDays class="h-6 w-6" />
                </div>
                <div>
                  <p class="text-sm font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                    Date and time
                  </p>
                  <p class="mt-2 text-xl font-bold leading-tight text-foreground">
                    {{ scheduleRange }}
                  </p>
                </div>
              </div>

              <div class="my-6 h-px bg-border/80" />

              <div class="flex items-start gap-4">
                <div
                  class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-amber-100 text-amber-700"
                >
                  <MapPin class="h-6 w-6" />
                </div>
                <div>
                  <p class="text-sm font-semibold uppercase tracking-[0.2em] text-muted-foreground">
                    Location
                  </p>
                  <p class="mt-2 text-xl font-bold leading-tight text-foreground">
                    {{ activity.locationName || 'Location to be announced' }}
                  </p>
                  <p class="mt-2 text-sm leading-7 text-muted-foreground">
                    {{ activity.locationAddress || 'The organizer will share venue details soon.' }}
                  </p>
                </div>
              </div>

              <div class="my-6 h-px bg-border/80" />

              <dl class="space-y-4 text-sm">
                <div class="flex items-center justify-between gap-3">
                  <dt class="text-muted-foreground">Price</dt>
                  <dd class="font-semibold text-foreground">{{ ticketLabel }}</dd>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <dt class="text-muted-foreground">Spots</dt>
                  <dd class="font-semibold text-foreground">{{ availabilityLabel }}</dd>
                </div>
                <div v-if="reservationWindowLabel" class="flex items-start justify-between gap-3">
                  <dt class="text-muted-foreground">Reservation window</dt>
                  <dd class="max-w-[12rem] text-right font-semibold text-foreground">
                    {{ reservationWindowLabel }}
                  </dd>
                </div>
              </dl>
            </div>
          </aside>
        </div>
      </div>

      <div class="fixed inset-x-4 bottom-4 z-30 sm:inset-x-6 lg:inset-x-8">
        <div class="mx-auto max-w-7xl">
          <div
            class="surface-panel rounded-[2.2rem] border border-white/70 px-5 py-4 shadow-[0_28px_60px_rgba(15,23,42,0.12)] sm:px-7"
          >
            <div class="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div class="space-y-2">
                <p class="text-sm font-semibold text-muted-foreground">
                  {{ scheduleHeadline }}
                </p>
                <h2
                  class="headline-balance text-2xl font-extrabold tracking-[-0.03em] text-foreground"
                >
                  {{ activity.title }}
                </h2>
                <p class="text-sm text-muted-foreground">
                  {{ ticketLabel }} - {{ reservationStatusLabel }}
                </p>
              </div>

              <div class="flex flex-col gap-3 lg:items-end">
                <Alert
                  v-if="reservationError"
                  class="w-full max-w-md border-destructive/40 bg-destructive/10 text-left text-destructive"
                  variant="destructive"
                >
                  {{ reservationError }}
                </Alert>

                <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
                  <div
                    class="rounded-full border border-border bg-white/75 px-4 py-2 text-sm font-semibold text-foreground"
                  >
                    {{ ticketLabel }}
                  </div>
                  <Button
                    class="min-w-44 shadow-[0_18px_42px_rgba(15,23,42,0.18)]"
                    size="lg"
                    :disabled="reservationMutation.isPending.value || isCtaDisabled"
                    @click="handlePrimaryAction"
                  >
                    <LoaderCircle
                      v-if="reservationMutation.isPending.value"
                      class="h-4 w-4 animate-spin"
                    />
                    {{ ctaLabel }}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>
