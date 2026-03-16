<script setup lang="ts">
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { CalendarDays, Clock3, LoaderCircle, MapPin, Ticket, Users } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { cancelReservation, reserveActivity } from '@/lib/api/activities'
import { getApiStatus, mapReservationError } from '@/lib/api/errors'
import type { ActivityResponse } from '@/lib/api/types'
import {
  formatAvailability,
  formatCapacity,
  formatDateRange,
  formatLocation,
  formatReservationWindow,
  formatTicketPrice,
} from '@/lib/formatters'
import { useSessionStore } from '@/stores/session'

const props = defineProps<{
  activity: ActivityResponse
}>()

const queryClient = useQueryClient()
const sessionStore = useSessionStore()
const router = useRouter()

const scheduleLabel = computed(() => formatDateRange(props.activity.startAt, props.activity.endAt))
const locationLabel = computed(() =>
  formatLocation(props.activity.locationName, props.activity.locationAddress),
)
const reservationLabel = computed(() =>
  formatReservationWindow(props.activity.reservationOpensAt, props.activity.reservationClosesAt),
)
const capacityLabel = computed(() => formatCapacity(props.activity.capacity))
const organizerLabel = computed(() => props.activity.organizer?.username ?? 'Community host')
const availabilityLabel = computed(() =>
  formatAvailability(props.activity.availableSpots, props.activity.atCapacity),
)
const ticketPriceLabel = computed(() => formatTicketPrice(props.activity.ticketPrice))
const capacitySummaryLabel = computed(() =>
  availabilityLabel.value === capacityLabel.value
    ? capacityLabel.value
    : `${capacityLabel.value} | ${availabilityLabel.value}`,
)
const isAuthenticatedStudent = computed(
  () => sessionStore.isAuthenticated && sessionStore.user?.role === 'STUDENT',
)
const reservationStatus = computed(() => props.activity.currentUserReservationStatus)

const reserveMutation = useMutation(() => ({
  mutationFn: () => reserveActivity(props.activity.id),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['public-activities'] })
  },
  onError: handleUnauthorizedReservationError,
}))

const cancelMutation = useMutation(() => ({
  mutationFn: () => cancelReservation(props.activity.id),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['public-activities'] })
  },
  onError: handleUnauthorizedReservationError,
}))

const reservationError = computed(() => {
  if (reserveMutation.error.value) {
    return mapReservationError(reserveMutation.error.value)
  }

  if (cancelMutation.error.value) {
    return mapReservationError(cancelMutation.error.value)
  }

  return ''
})

const reserveActionLabel = computed(() => (props.activity.atCapacity ? 'Join waitlist' : 'Reserve seat'))
const statusBadgeLabel = computed(() => {
  if (reservationStatus.value === 'RESERVED') {
    return 'Reservation confirmed'
  }

  if (reservationStatus.value === 'WAITLISTED') {
    return 'On the waitlist'
  }

  return ''
})

async function handleUnauthorizedReservationError(error: unknown): Promise<void> {
  if (getApiStatus(error) !== 401) {
    return
  }

  sessionStore.clearSession()
  await queryClient.invalidateQueries({ queryKey: ['public-activities'] })
  await router.push({ name: 'auth', query: { mode: 'login' } })
}
</script>

<template>
  <article class="surface-panel flex h-full flex-col overflow-hidden rounded-[1.75rem] border border-white/70">
    <img :src="activity.imageUrl" :alt="activity.title" class="h-52 w-full object-cover" />

    <div class="flex flex-1 flex-col p-6">
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="mb-3 inline-flex rounded-full bg-secondary px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-secondary-foreground">
            {{ ticketPriceLabel }}
          </p>
          <h3 class="headline-balance text-2xl font-bold tracking-tight text-foreground">
            {{ activity.title }}
          </h3>
          <p class="mt-2 text-sm font-medium text-muted-foreground">
            Hosted by {{ organizerLabel }}
          </p>
        </div>
      </div>

      <p class="mt-5 text-sm leading-7 text-foreground/80">
        {{ activity.description || 'More details are coming soon for this activity.' }}
      </p>

      <dl class="mt-6 grid gap-4 text-sm text-muted-foreground">
        <div class="flex items-start gap-3">
          <CalendarDays class="mt-0.5 h-4 w-4 text-primary" />
          <div>
            <dt class="font-semibold text-foreground">When</dt>
            <dd>{{ scheduleLabel }}</dd>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <MapPin class="mt-0.5 h-4 w-4 text-primary" />
          <div>
            <dt class="font-semibold text-foreground">Where</dt>
            <dd>{{ locationLabel }}</dd>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <Users class="mt-0.5 h-4 w-4 text-primary" />
          <div>
            <dt class="font-semibold text-foreground">Capacity</dt>
            <dd>{{ capacitySummaryLabel }}</dd>
          </div>
        </div>

        <div v-if="reservationLabel" class="flex items-start gap-3">
          <Clock3 class="mt-0.5 h-4 w-4 text-primary" />
          <div>
            <dt class="font-semibold text-foreground">Window</dt>
            <dd>{{ reservationLabel }}</dd>
          </div>
        </div>

        <div class="flex items-start gap-3">
          <Ticket class="mt-0.5 h-4 w-4 text-primary" />
          <div>
            <dt class="font-semibold text-foreground">Reservations</dt>
            <dd>{{ activity.confirmedReservationCount }} confirmed, {{ activity.waitlistCount }} waitlisted</dd>
          </div>
        </div>
      </dl>

      <Alert v-if="reservationError" class="mt-5" variant="destructive">
        {{ reservationError }}
      </Alert>

      <div class="mt-6 flex flex-wrap items-center gap-3">
        <span
          v-if="statusBadgeLabel"
          class="rounded-full bg-primary/12 px-3 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-primary"
        >
          {{ statusBadgeLabel }}
        </span>

        <template v-if="!sessionStore.isAuthenticated">
          <RouterLink
            class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
            :to="{ name: 'auth', query: { mode: 'login' } }"
          >
            Login to reserve
          </RouterLink>
        </template>

        <template v-else-if="!isAuthenticatedStudent">
          <span class="rounded-full bg-white/70 px-3 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            Admin view only
          </span>
        </template>

        <template v-else-if="reservationStatus === 'RESERVED' || reservationStatus === 'WAITLISTED'">
          <Button
            :disabled="cancelMutation.isPending.value"
            size="sm"
            variant="outline"
            @click="cancelMutation.mutate()"
          >
            <LoaderCircle v-if="cancelMutation.isPending.value" class="h-4 w-4 animate-spin" />
            Cancel reservation
          </Button>
        </template>

        <template v-else>
          <Button
            :disabled="reserveMutation.isPending.value || activity.status !== 'PUBLISHED'"
            size="sm"
            @click="reserveMutation.mutate()"
          >
            <LoaderCircle v-if="reserveMutation.isPending.value" class="h-4 w-4 animate-spin" />
            {{ reserveActionLabel }}
          </Button>
        </template>
      </div>
    </div>
  </article>
</template>
