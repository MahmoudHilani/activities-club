<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { format } from 'date-fns'
import { ArrowLeft, Mail, Ticket, UserRound } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { Alert } from '@/components/ui/alert'
import { buttonVariants } from '@/components/ui/button'
import { getAdminActivityReservations } from '@/lib/api/activities'
import { mapActivitiesError } from '@/lib/api/errors'
import type {
  AdminActivityReservationEntryResponse,
  ReservationStatus,
} from '@/lib/api/types'
import {
  formatAvailability,
  formatDateRange,
  formatLocation,
  formatTicketPrice,
} from '@/lib/formatters'

interface ReservationSection {
  status: ReservationStatus
  label: string
  entries: AdminActivityReservationEntryResponse[]
}

const STATUS_ORDER: ReservationStatus[] = [
  'RESERVED',
  'WAITLISTED',
  'CANCELLED',
  'ATTENDED',
  'NO_SHOW',
]

const route = useRoute()
const activityId = computed(() => Number(route.params.activityId))

const reservationsQuery = useQuery(() => ({
  queryKey: ['admin-activity-reservations', activityId.value],
  queryFn: () => getAdminActivityReservations(activityId.value),
}))

const activity = computed(() => reservationsQuery.data.value?.activity ?? null)
const reservations = computed(() => reservationsQuery.data.value?.reservations ?? [])
const statusCounts = computed(() =>
  STATUS_ORDER.map((status) => ({
    status,
    label: toStatusLabel(status),
    count: reservations.value.filter((entry) => entry.status === status).length,
  })),
)
const sections = computed<ReservationSection[]>(() =>
  STATUS_ORDER.map((status) => ({
    status,
    label: toStatusLabel(status),
    entries: reservations.value.filter((entry) => entry.status === status),
  })).filter((section) => section.entries.length > 0),
)

function toStatusLabel(status: ReservationStatus): string {
  switch (status) {
    case 'RESERVED':
      return 'Confirmed'
    case 'WAITLISTED':
      return 'Waitlisted'
    case 'CANCELLED':
      return 'Cancelled'
    case 'ATTENDED':
      return 'Attended'
    case 'NO_SHOW':
      return 'No-show'
  }
}

function formatTimestamp(value?: string | null): string {
  if (!value) {
    return 'Not recorded'
  }

  return format(new Date(value), 'MMM d, yyyy, p')
}
</script>

<template>
  <section class="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-8">
    <div class="space-y-5">
      <RouterLink
        :class="buttonVariants({ variant: 'outline', size: 'sm' })"
        :to="{ name: 'admin-activities' }"
      >
        <ArrowLeft class="h-4 w-4" />
        Back to admin dashboard
      </RouterLink>

      <Alert v-if="reservationsQuery.isError.value" variant="destructive">
        {{ mapActivitiesError(reservationsQuery.error.value) }}
      </Alert>

      <div v-if="activity" class="surface-panel rounded-[2rem] border border-white/70 p-6 sm:p-8">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div class="space-y-3">
            <p class="text-sm font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Activity reservations
            </p>
            <div class="flex flex-wrap items-center gap-3">
              <h1 class="headline-balance font-serif text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
                {{ activity.title }}
              </h1>
              <span class="rounded-full bg-secondary px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-secondary-foreground">
                {{ activity.status }}
              </span>
              <span class="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                {{ formatTicketPrice(activity.ticketPrice) }}
              </span>
            </div>
            <p class="max-w-3xl text-base leading-7 text-muted-foreground">
              {{ activity.description || 'Review the current reservation roster for this activity.' }}
            </p>
            <div class="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
              <p>{{ formatDateRange(activity.startAt, activity.endAt) }}</p>
              <p>{{ formatLocation(activity.locationName, activity.locationAddress) }}</p>
              <p>
                {{ activity.confirmedReservationCount }} confirmed, {{ activity.waitlistCount }} waitlisted
              </p>
              <p>{{ formatAvailability(activity.availableSpots, activity.atCapacity) }}</p>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:w-[24rem]">
            <div
              v-for="summary in statusCounts"
              :key="summary.status"
              class="rounded-[1.4rem] border border-white/70 bg-white/70 px-4 py-3"
            >
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                {{ summary.label }}
              </p>
              <p class="mt-2 text-2xl font-bold text-foreground">{{ summary.count }}</p>
            </div>
          </div>
        </div>
      </div>

      <div
        v-else-if="reservationsQuery.isPending.value"
        class="surface-panel rounded-[2rem] border border-white/70 p-8 text-muted-foreground"
      >
        Loading reservations...
      </div>
    </div>

    <div
      v-if="!reservationsQuery.isPending.value && !reservationsQuery.isError.value && reservations.length === 0"
      class="surface-panel rounded-[2rem] border border-white/70 p-8"
    >
      <h2 class="text-2xl font-bold text-foreground">No reservation history yet.</h2>
      <p class="mt-3 max-w-2xl text-muted-foreground">
        Nobody has reserved, joined the waitlist, or cancelled for this activity yet.
      </p>
    </div>

    <div
      v-else-if="!reservationsQuery.isPending.value && !reservationsQuery.isError.value"
      class="grid gap-6"
    >
      <section
        v-for="section in sections"
        :key="section.status"
        class="surface-panel rounded-[2rem] border border-white/70 p-6"
      >
        <div class="flex items-center justify-between gap-4">
          <div>
            <h2 class="text-2xl font-bold text-foreground">{{ section.label }}</h2>
            <p class="text-sm text-muted-foreground">
              {{ section.entries.length }} {{ section.entries.length === 1 ? 'person' : 'people' }}
            </p>
          </div>
        </div>

        <div class="mt-5 grid gap-4">
          <article
            v-for="entry in section.entries"
            :key="entry.id"
            class="rounded-[1.5rem] border border-white/70 bg-white/70 p-5"
          >
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div class="space-y-3">
                <div class="flex flex-wrap items-center gap-3">
                  <div class="flex items-center gap-2 text-lg font-semibold text-foreground">
                    <UserRound class="h-4 w-4 text-primary" />
                    <span>{{ entry.user.username }}</span>
                  </div>
                  <span class="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                    {{ toStatusLabel(entry.status) }}
                  </span>
                </div>
                <div class="flex items-center gap-2 text-sm text-muted-foreground">
                  <Mail class="h-4 w-4" />
                  <span>{{ entry.user.email }}</span>
                </div>
              </div>

              <div class="grid gap-2 text-sm text-muted-foreground sm:min-w-[18rem]">
                <p class="flex items-center gap-2">
                  <Ticket class="h-4 w-4" />
                  <span>Reserved {{ formatTimestamp(entry.reservedAt) }}</span>
                </p>
                <p v-if="entry.status === 'CANCELLED'" class="flex items-center gap-2">
                  <Ticket class="h-4 w-4" />
                  <span>Cancelled {{ formatTimestamp(entry.cancelledAt) }}</span>
                </p>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>
