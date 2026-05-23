<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { format } from 'date-fns'
import { ArrowLeft, Mail, Ticket, UserRound } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { Alert } from '@/components/ui/alert'
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
    tone: statusTone(status),
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

function statusTone(status: ReservationStatus): 'leaf' | 'ochre' | 'coral' | 'ink' {
  switch (status) {
    case 'RESERVED':
    case 'ATTENDED':
      return 'leaf'
    case 'WAITLISTED':
      return 'ochre'
    case 'CANCELLED':
    case 'NO_SHOW':
      return 'coral'
    default:
      return 'ink'
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
  <section class="res-shell">
    <RouterLink to="/admin/activities" class="back-pill">
      <ArrowLeft class="h-4 w-4" />
      <span>back to the desk</span>
    </RouterLink>

    <Alert v-if="reservationsQuery.isError.value" variant="destructive">
      {{ mapActivitiesError(reservationsQuery.error.value) }}
    </Alert>

    <div v-if="activity" class="res-hero">
      <div class="res-hero-left">
        <div class="res-headline">
          <h1 class="res-title">{{ activity.title }}</h1>
          <div class="res-pills">
            <span class="craft-tag" :class="`craft-tag-${statusTone('RESERVED')}`">
              {{ activity.status.toLowerCase() }}
            </span>
            <span class="craft-pill">{{ formatTicketPrice(activity.ticketPrice) }}</span>
          </div>
        </div>
        <p class="res-blurb">
          {{ activity.description || 'Review the reservation roster for this activity.' }}
        </p>
        <div class="res-meta">
          <p>{{ formatDateRange(activity.startAt, activity.endAt) }}</p>
          <p>{{ formatLocation(activity.locationName, activity.locationAddress) }}</p>
          <p>
            <strong>{{ activity.confirmedReservationCount }}</strong> confirmed ·
            <strong>{{ activity.waitlistCount }}</strong> waitlisted
          </p>
          <p>{{ formatAvailability(activity.availableSpots, activity.atCapacity) }}</p>
        </div>
      </div>

      <div class="res-summary">
        <div
          v-for="summary in statusCounts"
          :key="summary.status"
          class="summary-card"
          :class="`summary-${summary.tone}`"
        >
          <p class="summary-label">{{ summary.label }}</p>
          <p class="summary-value">{{ summary.count }}</p>
        </div>
      </div>
    </div>

    <div
      v-else-if="reservationsQuery.isPending.value"
      class="state-card"
    >
      Loading reservations…
    </div>

    <div
      v-if="
        !reservationsQuery.isPending.value &&
        !reservationsQuery.isError.value &&
        reservations.length === 0
      "
      class="empty-card"
    >
      <span class="craft-tag craft-tag-ochre empty-stamp">quiet for now</span>
      <h2 class="empty-title">No reservation history yet.</h2>
      <p class="empty-sub">
        Nobody has reserved, joined the waitlist, or cancelled for this activity yet.
      </p>
    </div>

    <div
      v-else-if="!reservationsQuery.isPending.value && !reservationsQuery.isError.value"
      class="res-sections"
    >
      <section
        v-for="section in sections"
        :key="section.status"
        class="res-section"
        :class="`res-section-${statusTone(section.status)}`"
      >
        <header class="res-section-head">
          <h2 class="res-section-title">{{ section.label }}</h2>
          <p class="res-section-sub">
            {{ section.entries.length }} {{ section.entries.length === 1 ? 'person' : 'people' }}
          </p>
        </header>

        <div class="res-entries">
          <article
            v-for="entry in section.entries"
            :key="entry.id"
            class="res-entry"
          >
            <div class="res-entry-grid">
              <div class="res-entry-info">
                <div class="res-entry-name">
                  <UserRound class="h-4 w-4" />
                  <span>{{ entry.user.username }}</span>
                  <span class="craft-pill res-entry-status">
                    {{ toStatusLabel(entry.status) }}
                  </span>
                </div>
                <div class="res-entry-email">
                  <Mail class="h-4 w-4" />
                  <span>{{ entry.user.email }}</span>
                </div>
              </div>

              <div class="res-entry-times">
                <p>
                  <Ticket class="h-4 w-4" />
                  <span>Reserved {{ formatTimestamp(entry.reservedAt) }}</span>
                </p>
                <p v-if="entry.status === 'CANCELLED'">
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

<style scoped>
.res-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 1.75rem;
  max-width: 80rem;
  margin: 0 auto;
  width: 100%;
}

.back-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: white;
  border: 2px solid var(--primary);
  color: var(--primary);
  font-weight: 700;
  font-size: 13px;
  padding: 8px 16px 8px 12px;
  border-radius: 999px;
  box-shadow: 2px 2px 0 var(--color-coral);
  width: max-content;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.back-pill:hover {
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0 var(--color-coral);
}

.res-hero {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 28px;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
  display: grid;
  gap: 24px;
  grid-template-columns: 1fr;
}
@media (min-width: 1000px) {
  .res-hero {
    grid-template-columns: 1.2fr 0.9fr;
  }
}
.res-hero-left {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}
.res-headline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}
.res-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(34px, 4.5vw, 52px);
  line-height: 0.98;
  letter-spacing: -0.01em;
  color: var(--primary);
  word-break: break-word;
}
.res-pills {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}
.res-blurb {
  margin: 0;
  font-size: 15px;
  line-height: 1.55;
  color: var(--muted-foreground);
  max-width: 60ch;
}
.res-meta {
  display: grid;
  grid-template-columns: 1fr;
  gap: 4px;
  font-size: 14px;
  color: var(--primary);
  border-top: 1.5px dashed color-mix(in srgb, var(--primary) 25%, transparent);
  padding-top: 12px;
}
@media (min-width: 640px) {
  .res-meta {
    grid-template-columns: repeat(2, 1fr);
  }
}
.res-meta p {
  margin: 0;
}
.res-meta strong {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-coral);
}

.res-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  align-content: start;
}
@media (min-width: 540px) {
  .res-summary {
    grid-template-columns: repeat(3, 1fr);
  }
}
.summary-card {
  background: color-mix(in srgb, white 92%, #f4efe4 8%);
  border: 2px solid var(--primary);
  border-radius: 18px;
  padding: 14px;
}
.summary-leaf {
  box-shadow: 3px 3px 0 var(--color-leaf);
}
.summary-ochre {
  box-shadow: 3px 3px 0 var(--color-ochre);
}
.summary-coral {
  box-shadow: 3px 3px 0 var(--color-coral);
}
.summary-ink {
  box-shadow: 3px 3px 0 var(--primary);
}
.summary-label {
  margin: 0;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 17px;
  color: var(--color-coral);
}
.summary-ochre .summary-label {
  color: var(--color-ochre);
}
.summary-leaf .summary-label {
  color: var(--color-leaf);
}
.summary-ink .summary-label {
  color: var(--primary);
}
.summary-value {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 32px;
  line-height: 1;
  color: var(--primary);
}

.state-card {
  background: white;
  border: 2px dashed color-mix(in srgb, var(--primary) 35%, white);
  border-radius: 24px;
  padding: 28px;
  color: var(--muted-foreground);
  font-weight: 600;
}

.empty-card {
  position: relative;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 36px;
  box-shadow:
    4px 4px 0 var(--color-coral),
    8px 8px 0 var(--primary);
}
.empty-stamp {
  position: absolute;
  top: -14px;
  right: 26px;
  box-shadow: 2px 2px 0 var(--primary);
  transform: rotate(5deg);
}
.empty-title {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 32px;
  line-height: 1;
  color: var(--primary);
}
.empty-sub {
  margin: 0;
  font-size: 15px;
  line-height: 1.55;
  color: var(--muted-foreground);
  max-width: 60ch;
}

.res-sections {
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.res-section {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 24px;
}
.res-section-leaf {
  box-shadow: 4px 4px 0 var(--color-leaf);
}
.res-section-ochre {
  box-shadow: 4px 4px 0 var(--color-ochre);
}
.res-section-coral {
  box-shadow: 4px 4px 0 var(--color-coral);
}
.res-section-ink {
  box-shadow: 4px 4px 0 var(--primary);
}
.res-section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1.5px dashed color-mix(in srgb, var(--primary) 25%, transparent);
  margin-bottom: 14px;
}
.res-section-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 28px;
  line-height: 1;
  color: var(--primary);
}
.res-section-sub {
  margin: 0;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-coral);
}

.res-entries {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.res-entry {
  background: color-mix(in srgb, white 96%, #f4efe4 4%);
  border: 2px solid color-mix(in srgb, var(--primary) 50%, white);
  border-radius: 20px;
  padding: 18px 20px;
}
.res-entry-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
@media (min-width: 760px) {
  .res-entry-grid {
    grid-template-columns: 1fr auto;
    align-items: start;
  }
}
.res-entry-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.res-entry-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--primary);
  line-height: 1;
  flex-wrap: wrap;
}
.res-entry-name svg {
  color: var(--color-coral);
}
.res-entry-status {
  font-size: 11px;
  padding: 4px 10px;
}
.res-entry-email {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--muted-foreground);
}
.res-entry-times {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13.5px;
  color: var(--muted-foreground);
}
.res-entry-times p {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}
.res-entry-times svg {
  color: var(--color-coral);
  flex-shrink: 0;
}
</style>
