<script setup lang="ts">
import { CalendarDays, Clock3, MapPin, Users } from 'lucide-vue-next'
import { computed } from 'vue'

import type { ActivityResponse } from '@/lib/api/types'
import {
  formatCapacity,
  formatDateRange,
  formatLocation,
  formatReservationWindow,
} from '@/lib/formatters'

const props = defineProps<{
  activity: ActivityResponse
}>()

const scheduleLabel = computed(() => formatDateRange(props.activity.startAt, props.activity.endAt))
const locationLabel = computed(() =>
  formatLocation(props.activity.locationName, props.activity.locationAddress),
)
const reservationLabel = computed(() =>
  formatReservationWindow(props.activity.reservationOpensAt, props.activity.reservationClosesAt),
)
const capacityLabel = computed(() => formatCapacity(props.activity.capacity))
const organizerLabel = computed(() => props.activity.organizer?.username ?? 'Community host')
</script>

<template>
  <article class="surface-panel flex h-full flex-col rounded-[1.75rem] border border-white/70 p-6">
    <div class="flex items-start justify-between gap-4">
      <div>
        <p class="mb-3 inline-flex rounded-full bg-secondary px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-secondary-foreground">
          Public activity
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
          <dd>{{ capacityLabel }}</dd>
        </div>
      </div>

      <div v-if="reservationLabel" class="flex items-start gap-3">
        <Clock3 class="mt-0.5 h-4 w-4 text-primary" />
        <div>
          <dt class="font-semibold text-foreground">Reservations</dt>
          <dd>{{ reservationLabel }}</dd>
        </div>
      </div>
    </dl>
  </article>
</template>
