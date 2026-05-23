<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import {
  CalendarRange,
  LoaderCircle,
  PencilLine,
  Rocket,
  Trash2,
} from 'lucide-vue-next'
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

const ACTIVITY_DESCRIPTION_PREVIEW_LIMIT = 280

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
    { label: 'Total', value: activities.value.length, tone: 'ink' as const, hint: 'on the books' },
    { label: 'Drafts', value: drafts, tone: 'ochre' as const, hint: 'still cooking' },
    { label: 'Published', value: published, tone: 'leaf' as const, hint: 'live & loud' },
    { label: 'Cancelled', value: cancelled, tone: 'coral' as const, hint: 'rain stopped play' },
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

function canPublish(activity: ActivityResponse): boolean {
  return activity.status === 'DRAFT' || activity.status === 'CANCELLED'
}

function canDelete(activity: ActivityResponse): boolean {
  return (
    (activity.status === 'DRAFT' || activity.status === 'CANCELLED') &&
    activity.confirmedReservationCount === 0 &&
    activity.waitlistCount === 0
  )
}

function statusTone(status: ActivityResponse['status']): 'coral' | 'ochre' | 'leaf' | 'ink' {
  switch (status) {
    case 'PUBLISHED':
      return 'leaf'
    case 'DRAFT':
      return 'ochre'
    case 'CANCELLED':
      return 'coral'
    default:
      return 'ink'
  }
}

function formatDescriptionPreview(description?: string | null): string {
  if (!description) {
    return 'No description added yet.'
  }

  if (description.length <= ACTIVITY_DESCRIPTION_PREVIEW_LIMIT) {
    return description
  }

  return `${description.slice(0, ACTIVITY_DESCRIPTION_PREVIEW_LIMIT).trimEnd()}...`
}

async function invalidateActivityQueries(): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-activities'] }),
    queryClient.invalidateQueries({ queryKey: ['public-activities'] }),
  ])
}
</script>

<template>
  <section class="admin-shell">
    <header class="admin-hero">
      <h1 class="admin-title">
        <span class="display-text">The</span>
        <span class="hand-text"> activity </span>
        <span class="display-text">desk</span>
      </h1>
      <p class="admin-lede">
        Draft, publish, cancel, snoop on numbers. The plus button in the corner is for new
        adventures.
      </p>
    </header>

    <Alert v-if="activitiesQuery.isError.value" variant="destructive">
      {{ mapActivitiesError(activitiesQuery.error.value) }}
    </Alert>

    <div class="stat-grid">
      <div
        v-for="card in summaryCards"
        :key="card.label"
        class="stat-card"
        :class="`stat-${card.tone}`"
      >
        <p class="stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value }}</p>
        <p class="stat-hint">{{ card.hint }}</p>
      </div>
    </div>

    <div v-if="activitiesQuery.isPending.value" class="state-card">
      <LoaderCircle class="h-5 w-5 animate-spin" />
      <span>Pulling the latest list…</span>
    </div>

    <div v-else-if="activities.length === 0" class="state-card state-card-empty">
      <p class="state-title">
        <span class="display-text">No activities</span>
        <span class="hand-text"> yet</span>
      </p>
      <p class="state-sub">
        Tap the floating plus to draft the first one. You can always come back and publish later.
      </p>
    </div>

    <div v-else class="admin-list">
      <article
        v-for="activity in activities"
        :key="activity.id"
        class="admin-card"
      >
        <div class="admin-card-inner">
          <div class="admin-card-media">
            <img :src="activity.imageUrl" :alt="activity.title" class="admin-thumb" />
            <span class="craft-tag" :class="`craft-tag-${statusTone(activity.status)}`">
              {{ activity.status.toLowerCase() }}
            </span>
          </div>

          <div class="admin-card-body">
            <div class="admin-card-headline">
              <h2 class="admin-card-title">{{ activity.title }}</h2>
              <div class="admin-card-pills">
                <span class="craft-pill">{{ formatTicketPrice(activity.ticketPrice) }}</span>
              </div>
            </div>

            <p class="admin-card-desc">
              {{ formatDescriptionPreview(activity.description) }}
            </p>

            <div class="admin-card-stats">
              <p>
                <CalendarRange class="h-4 w-4" />
                <span>{{ formatDateRange(activity.startAt, activity.endAt) }}</span>
              </p>
              <p>
                <Rocket class="h-4 w-4" />
                <span>
                  <strong>{{ activity.confirmedReservationCount }}</strong> confirmed,
                  <strong>{{ activity.waitlistCount }}</strong> waitlisted,
                  {{ formatAvailability(activity.availableSpots, activity.atCapacity) }}
                </span>
              </p>
            </div>
          </div>

          <div class="admin-card-actions">
            <RouterLink
              :class="[buttonVariants({ size: 'sm' }), 'admin-action']"
              :to="{ name: 'admin-activity-edit', params: { activityId: activity.id } }"
            >
              <PencilLine class="h-4 w-4" />
              Edit
            </RouterLink>
            <RouterLink
              :class="[buttonVariants({ variant: 'outline', size: 'sm' }), 'admin-action']"
              :to="{ name: 'activity-detail', params: { activityId: activity.id } }"
            >
              Preview
            </RouterLink>
            <RouterLink
              :class="[buttonVariants({ variant: 'outline', size: 'sm' }), 'admin-action']"
              :to="{ name: 'admin-activity-reservations', params: { activityId: activity.id } }"
            >
              View reservations
            </RouterLink>
            <Button
              v-if="canPublish(activity)"
              :disabled="publishMutation.isPending.value"
              class="admin-action"
              size="sm"
              variant="secondary"
              @click="publishMutation.mutate(activity.id)"
            >
              <LoaderCircle v-if="publishMutation.isPending.value" class="h-4 w-4 animate-spin" />
              Publish
            </Button>
            <Button
              v-if="activity.status !== 'CANCELLED' && activity.status !== 'COMPLETED'"
              :disabled="cancelMutation.isPending.value"
              class="admin-action"
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
              class="admin-action"
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

<style scoped>
.admin-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 2rem;
}

.admin-hero {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}
.admin-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(40px, 6vw, 68px);
  line-height: 0.98;
  color: var(--primary);
  letter-spacing: -0.01em;
}
.admin-title .hand-text {
  font-size: 1.12em;
}
.admin-lede {
  max-width: 60ch;
  font-size: 16px;
  line-height: 1.55;
  color: var(--muted-foreground);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
@media (min-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
.stat-card {
  position: relative;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 22px;
  padding: 18px 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-ink {
  box-shadow: 4px 4px 0 var(--primary);
}
.stat-ochre {
  box-shadow: 4px 4px 0 var(--color-ochre);
}
.stat-leaf {
  box-shadow: 4px 4px 0 var(--color-leaf);
}
.stat-coral {
  box-shadow: 4px 4px 0 var(--color-coral);
}
.stat-label {
  margin: 0;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 20px;
  color: var(--color-coral);
}
.stat-ochre .stat-label {
  color: var(--color-ochre);
}
.stat-leaf .stat-label {
  color: var(--color-leaf);
}
.stat-ink .stat-label {
  color: var(--primary);
}
.stat-value {
  margin: 2px 0 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 42px;
  line-height: 1;
  color: var(--primary);
}
.stat-hint {
  margin: 4px 0 0;
  font-size: 12.5px;
  color: var(--muted-foreground);
  font-weight: 600;
}

.state-card {
  background: white;
  border: 2px dashed color-mix(in srgb, var(--primary) 35%, white);
  border-radius: 24px;
  padding: 28px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--muted-foreground);
  font-weight: 600;
}
.state-card-empty {
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  text-align: left;
}
.state-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 28px;
  color: var(--primary);
}
.state-sub {
  margin: 0;
  font-size: 15px;
  color: var(--muted-foreground);
}

.admin-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.admin-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 22px;
  box-shadow: 5px 5px 0 var(--color-coral);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}
.admin-card:hover {
  transform: translate(-2px, -2px);
  box-shadow: 8px 8px 0 var(--color-coral);
}

.admin-card-inner {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}
@media (min-width: 760px) {
  .admin-card-inner {
    grid-template-columns: 160px 1fr;
  }
}
@media (min-width: 1180px) {
  .admin-card-inner {
    grid-template-columns: 160px 1fr 16rem;
  }
}

.admin-card-media {
  position: relative;
}
.admin-thumb {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 22px;
  object-fit: cover;
  border: 2px solid var(--primary);
}
.admin-card-media .craft-tag {
  position: absolute;
  top: -8px;
  left: -8px;
  box-shadow: 2px 2px 0 var(--primary);
}

.admin-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.admin-card-headline {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}
.admin-card-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 26px;
  line-height: 1.05;
  color: var(--primary);
  flex: 1;
  min-width: 0;
  word-break: break-word;
}
.admin-card-pills {
  display: inline-flex;
  gap: 8px;
}
.admin-card-desc {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.55;
  color: var(--muted-foreground);
}
.admin-card-stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-top: 1.5px dashed color-mix(in srgb, var(--primary) 25%, transparent);
  padding-top: 12px;
}
.admin-card-stats p {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  font-size: 14px;
  color: var(--primary);
}
.admin-card-stats p strong {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-coral);
}
.admin-card-stats svg {
  color: var(--color-coral);
  margin-top: 2px;
  flex-shrink: 0;
}

.admin-card-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.admin-action {
  width: 100%;
  justify-content: center;
}
</style>
