<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import {
  CalendarRange,
  Eye,
  LoaderCircle,
  MoreHorizontal,
  PencilLine,
  Plus,
  Rocket,
  Search,
  Trash2,
  Users,
  X,
} from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { Alert } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  cancelAdminActivity,
  deleteAdminActivity,
  getAdminActivities,
  publishAdminActivity,
} from '@/lib/api/activities'
import { mapActivitiesError } from '@/lib/api/errors'
import type { ActivityResponse } from '@/lib/api/types'
import { formatAvailability, formatDateStart, formatTicketPrice } from '@/lib/formatters'
import { cn } from '@/lib/utils'

type StatusFilter = 'ALL' | ActivityResponse['status']
type SortKey = 'date-asc' | 'date-desc' | 'created-desc' | 'title-asc'

const queryClient = useQueryClient()

const activitiesQuery = useQuery(() => ({
  queryKey: ['admin-activities'],
  queryFn: () => getAdminActivities(),
}))

const activities = computed(() => activitiesQuery.data.value?.content ?? [])

const searchTerm = ref('')
const statusFilter = ref<StatusFilter>('ALL')
const sortKey = ref<SortKey>('date-asc')

const counts = computed(() => {
  const all = activities.value.length
  const drafts = activities.value.filter((a) => a.status === 'DRAFT').length
  const published = activities.value.filter((a) => a.status === 'PUBLISHED').length
  const cancelled = activities.value.filter((a) => a.status === 'CANCELLED').length
  const completed = activities.value.filter((a) => a.status === 'COMPLETED').length
  return { all, drafts, published, cancelled, completed }
})

const filterChips = computed(() => [
  { key: 'ALL' as const, label: 'All', count: counts.value.all, tone: 'ink' as const },
  { key: 'DRAFT' as const, label: 'Drafts', count: counts.value.drafts, tone: 'ochre' as const },
  {
    key: 'PUBLISHED' as const,
    label: 'Published',
    count: counts.value.published,
    tone: 'leaf' as const,
  },
  {
    key: 'CANCELLED' as const,
    label: 'Cancelled',
    count: counts.value.cancelled,
    tone: 'coral' as const,
  },
  {
    key: 'COMPLETED' as const,
    label: 'Completed',
    count: counts.value.completed,
    tone: 'ink' as const,
  },
])

const filtered = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  let list = activities.value.slice()

  if (statusFilter.value !== 'ALL') {
    list = list.filter((a) => a.status === statusFilter.value)
  }

  if (term) {
    list = list.filter(
      (a) =>
        a.title.toLowerCase().includes(term) ||
        (a.locationName ?? '').toLowerCase().includes(term) ||
        (a.description ?? '').toLowerCase().includes(term),
    )
  }

  switch (sortKey.value) {
    case 'date-asc':
      list.sort((a, b) => compareDates(a.startAt, b.startAt))
      break
    case 'date-desc':
      list.sort((a, b) => compareDates(b.startAt, a.startAt))
      break
    case 'created-desc':
      list.sort((a, b) => compareDates(b.createdAt, a.createdAt))
      break
    case 'title-asc':
      list.sort((a, b) => a.title.localeCompare(b.title))
      break
  }

  return list
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

type ConfirmIntent = {
  kind: 'cancel' | 'delete'
  activity: ActivityResponse
}
const confirmIntent = ref<ConfirmIntent | null>(null)
const deleteTypeBuffer = ref('')

function requestCancel(activity: ActivityResponse): void {
  confirmIntent.value = { kind: 'cancel', activity }
  deleteTypeBuffer.value = ''
}

function requestDelete(activity: ActivityResponse): void {
  confirmIntent.value = { kind: 'delete', activity }
  deleteTypeBuffer.value = ''
}

function dismissConfirm(): void {
  confirmIntent.value = null
  deleteTypeBuffer.value = ''
}

async function executeConfirm(): Promise<void> {
  const intent = confirmIntent.value
  if (!intent) {
    return
  }

  if (intent.kind === 'cancel') {
    await cancelMutation.mutateAsync(intent.activity.id)
  } else {
    await deleteMutation.mutateAsync(intent.activity.id)
  }

  dismissConfirm()
}

const deleteConfirmReady = computed(() => {
  const intent = confirmIntent.value
  if (!intent || intent.kind !== 'delete') {
    return true
  }
  return deleteTypeBuffer.value.trim() === intent.activity.title.trim()
})

function canPublish(activity: ActivityResponse): boolean {
  return activity.status === 'DRAFT' || activity.status === 'CANCELLED'
}

function canCancel(activity: ActivityResponse): boolean {
  return activity.status !== 'CANCELLED' && activity.status !== 'COMPLETED'
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

function compareDates(a: string | null, b: string | null): number {
  if (!a && !b) return 0
  if (!a) return 1
  if (!b) return -1
  return new Date(a).getTime() - new Date(b).getTime()
}

function resetFilters(): void {
  searchTerm.value = ''
  statusFilter.value = 'ALL'
  sortKey.value = 'date-asc'
}

async function invalidateActivityQueries(): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-activities'] }),
    queryClient.invalidateQueries({ queryKey: ['public-activities'] }),
  ])
}
</script>

<template>
  <section class="proto-shell">
    <header class="proto-hero">
      <div class="proto-hero-text">
        <h1 class="proto-title">
          <span class="display-text">The</span>
          <span class="hand-text"> activity </span>
          <span class="display-text">desk</span>
        </h1>
      </div>
      <RouterLink
        :class="[buttonVariants({ size: 'lg' }), 'proto-new-btn']"
        :to="{ name: 'admin-activity-create' }"
      >
        <Plus class="h-5 w-5" />
        New activity
      </RouterLink>
    </header>

    <Alert v-if="activitiesQuery.isError.value" variant="destructive">
      {{ mapActivitiesError(activitiesQuery.error.value) }}
    </Alert>

    <!-- Toolbar -->
    <div class="proto-toolbar">
      <div class="proto-search">
        <Search class="proto-search-icon h-4 w-4" />
        <Input
          v-model="searchTerm"
          placeholder="Search by title, location, or description…"
          class="proto-search-input"
        />
        <button
          v-if="searchTerm"
          class="proto-search-clear"
          aria-label="Clear search"
          @click="searchTerm = ''"
        >
          <X class="h-4 w-4" />
        </button>
      </div>

      <div class="proto-sort">
        <label class="proto-sort-label">Sort</label>
        <Select v-model="sortKey">
          <SelectTrigger class="proto-sort-trigger">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="date-asc">Date — soonest first</SelectItem>
            <SelectItem value="date-desc">Date — latest first</SelectItem>
            <SelectItem value="created-desc">Recently created</SelectItem>
            <SelectItem value="title-asc">Title (A–Z)</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>

    <!-- Filter chips (clickable, double as counters) -->
    <div class="proto-chips" role="tablist" aria-label="Filter by status">
      <button
        v-for="chip in filterChips"
        :key="chip.key"
        role="tab"
        :aria-selected="statusFilter === chip.key"
        :class="
          cn(
            'proto-chip',
            `proto-chip-${chip.tone}`,
            statusFilter === chip.key && 'proto-chip-active',
          )
        "
        @click="statusFilter = chip.key"
      >
        <span class="proto-chip-label">{{ chip.label }}</span>
        <span class="proto-chip-count">{{ chip.count }}</span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="activitiesQuery.isPending.value" class="proto-state">
      <LoaderCircle class="h-5 w-5 animate-spin" />
      <span>Pulling the latest list…</span>
    </div>

    <!-- Empty (no data at all) -->
    <div v-else-if="activities.length === 0" class="proto-state proto-state-empty">
      <p class="proto-state-title">
        <span class="display-text">No activities</span>
        <span class="hand-text"> yet</span>
      </p>
      <p class="proto-state-sub">
        Start with a draft — you can always come back and publish later.
      </p>
      <RouterLink
        :class="[buttonVariants({ size: 'sm' }), 'mt-3']"
        :to="{ name: 'admin-activity-create' }"
      >
        <Plus class="h-4 w-4" />
        New activity
      </RouterLink>
    </div>

    <!-- Empty (filtered) -->
    <div
      v-else-if="filtered.length === 0"
      class="proto-state proto-state-empty"
    >
      <p class="proto-state-title">
        <span class="display-text">Nothing</span>
        <span class="hand-text"> matches</span>
      </p>
      <p class="proto-state-sub">
        Try a different search term or reset the filters.
      </p>
      <Button variant="outline" size="sm" class="mt-3" @click="resetFilters">Reset filters</Button>
    </div>

    <!-- List header (desktop only) -->
    <div v-else class="proto-table">
      <div class="proto-row proto-row-head" aria-hidden="true">
        <span class="proto-col-thumb"></span>
        <span class="proto-col-title">Activity</span>
        <span class="proto-col-date">Starts</span>
        <span class="proto-col-stats">Reservations</span>
        <span class="proto-col-actions"></span>
      </div>

      <article
        v-for="activity in filtered"
        :key="activity.id"
        class="proto-row proto-row-data"
      >
        <div class="proto-col-thumb">
          <img :src="activity.imageUrl" :alt="activity.title" class="proto-thumb" />
        </div>

        <div class="proto-col-title">
          <div class="proto-title-line">
            <RouterLink
              :to="{ name: 'admin-activity-edit', params: { activityId: activity.id } }"
              class="proto-title-link"
            >
              {{ activity.title }}
            </RouterLink>
            <span
              class="proto-status"
              :class="`proto-status-${statusTone(activity.status)}`"
            >
              {{ activity.status.toLowerCase() }}
            </span>
          </div>
          <div class="proto-meta">
            <span>{{ formatTicketPrice(activity.ticketPrice) }}</span>
            <span v-if="activity.locationName" class="proto-meta-sep">·</span>
            <span v-if="activity.locationName">{{ activity.locationName }}</span>
          </div>
        </div>

        <div class="proto-col-date">
          <CalendarRange class="h-4 w-4 proto-icon" />
          <span>{{ formatDateStart(activity.startAt, activity.endAt) }}</span>
        </div>

        <div class="proto-col-stats">
          <Users class="h-4 w-4 proto-icon" />
          <span>
            <strong>{{ activity.confirmedReservationCount }}</strong>
            <span class="proto-stats-secondary">
              confirmed
              <template v-if="activity.waitlistCount > 0">
                · {{ activity.waitlistCount }} waitlist
              </template>
              · {{ formatAvailability(activity.availableSpots, activity.atCapacity) }}
            </span>
          </span>
        </div>

        <div class="proto-col-actions">
          <RouterLink
            :class="[buttonVariants({ size: 'sm' }), 'proto-primary-action']"
            :to="{ name: 'admin-activity-edit', params: { activityId: activity.id } }"
          >
            <PencilLine class="h-4 w-4" />
            Edit
          </RouterLink>

          <Popover>
            <PopoverTrigger as-child>
              <Button
                variant="outline"
                size="sm"
                class="proto-overflow-btn"
                aria-label="More actions"
              >
                <MoreHorizontal class="h-4 w-4" />
              </Button>
            </PopoverTrigger>
            <PopoverContent align="end" class="proto-menu">
              <RouterLink
                class="proto-menu-item"
                :to="{ name: 'activity-detail', params: { activityId: activity.id } }"
              >
                <Eye class="h-4 w-4" />
                Preview as student
              </RouterLink>
              <RouterLink
                class="proto-menu-item"
                :to="{ name: 'admin-activity-reservations', params: { activityId: activity.id } }"
              >
                <Users class="h-4 w-4" />
                View reservations
              </RouterLink>

              <div class="proto-menu-divider" />

              <button
                v-if="canPublish(activity)"
                class="proto-menu-item"
                :disabled="publishMutation.isPending.value"
                @click="publishMutation.mutate(activity.id)"
              >
                <Rocket class="h-4 w-4" />
                Publish
              </button>
              <button
                v-if="canCancel(activity)"
                class="proto-menu-item"
                @click="requestCancel(activity)"
              >
                <X class="h-4 w-4" />
                Cancel activity
              </button>
              <button
                v-if="canDelete(activity)"
                class="proto-menu-item proto-menu-item-danger"
                @click="requestDelete(activity)"
              >
                <Trash2 class="h-4 w-4" />
                Delete
              </button>
            </PopoverContent>
          </Popover>
        </div>
      </article>
    </div>

    <!-- Confirmation modal -->
    <div
      v-if="confirmIntent"
      class="proto-modal-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="activity-confirm-title"
      @click.self="dismissConfirm"
    >
      <div class="proto-modal">
        <h2 id="activity-confirm-title" class="proto-modal-title">
          <template v-if="confirmIntent.kind === 'cancel'">
            Cancel "{{ confirmIntent.activity.title }}"?
          </template>
          <template v-else>
            Delete "{{ confirmIntent.activity.title }}"?
          </template>
        </h2>
        <p class="proto-modal-body">
          <template v-if="confirmIntent.kind === 'cancel'">
            The activity will be marked cancelled. Confirmed and waitlisted reservations
            ({{ confirmIntent.activity.confirmedReservationCount }} confirmed,
            {{ confirmIntent.activity.waitlistCount }} waitlisted) will stay on file
            but the activity won't be visible to students.
          </template>
          <template v-else>
            This permanently removes the activity. It can only be deleted because it has
            no reservations.
          </template>
        </p>

        <div v-if="confirmIntent.kind === 'delete'" class="proto-modal-field">
          <label class="proto-modal-label">
            Type <strong>{{ confirmIntent.activity.title }}</strong> to confirm:
          </label>
          <Input v-model="deleteTypeBuffer" />
        </div>

        <div class="proto-modal-actions">
          <Button variant="outline" size="sm" @click="dismissConfirm">Keep it</Button>
          <Button
            :disabled="!deleteConfirmReady ||
              cancelMutation.isPending.value ||
              deleteMutation.isPending.value"
            :variant="confirmIntent.kind === 'delete' ? 'destructive' : 'default'"
            size="sm"
            @click="executeConfirm"
          >
            <LoaderCircle
              v-if="cancelMutation.isPending.value || deleteMutation.isPending.value"
              class="h-4 w-4 animate-spin"
            />
            <template v-if="confirmIntent.kind === 'cancel'">Cancel activity</template>
            <template v-else>Delete forever</template>
          </Button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.proto-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 1.5rem;
}

/* Hero */
.proto-hero {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
}
.proto-hero-text {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 280px;
}
.proto-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(36px, 5vw, 56px);
  line-height: 1;
  color: var(--primary);
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
}
.proto-title .hand-text {
  font-size: 1.12em;
}
.proto-new-btn {
  flex-shrink: 0;
}

/* Toolbar */
.proto-toolbar {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
@media (min-width: 720px) {
  .proto-toolbar {
    grid-template-columns: 1fr auto;
    align-items: center;
  }
}
.proto-search {
  position: relative;
  display: flex;
  align-items: center;
}
.proto-search-icon {
  position: absolute;
  left: 14px;
  color: var(--muted-foreground);
  pointer-events: none;
}
.proto-search-input {
  padding-left: 40px;
  padding-right: 40px;
}
.proto-search-clear {
  position: absolute;
  right: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 0;
  background: transparent;
  color: var(--muted-foreground);
  cursor: pointer;
}
.proto-search-clear:hover {
  background: color-mix(in srgb, var(--primary) 8%, transparent);
  color: var(--primary);
}
.proto-sort {
  display: flex;
  align-items: center;
  gap: 10px;
}
.proto-sort-label {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 16px;
  color: var(--primary);
}
.proto-sort-trigger {
  min-width: 200px;
}

/* Filter chips */
.proto-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.proto-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: white;
  border: 2px solid var(--primary);
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 15px;
  color: var(--primary);
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}
.proto-chip:hover {
  transform: translate(-1px, -1px);
  box-shadow: 2px 2px 0 var(--primary);
}
.proto-chip-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  font-size: 12px;
  font-weight: 700;
}
.proto-chip-active {
  color: white;
  background: var(--primary);
  box-shadow: 3px 3px 0 var(--color-coral);
}
.proto-chip-active .proto-chip-count {
  background: color-mix(in srgb, white 25%, transparent);
  color: white;
}
.proto-chip-ochre.proto-chip-active {
  background: var(--color-ochre);
  border-color: var(--color-ochre);
}
.proto-chip-leaf.proto-chip-active {
  background: var(--color-leaf);
  border-color: var(--color-leaf);
}
.proto-chip-coral.proto-chip-active {
  background: var(--color-coral);
  border-color: var(--color-coral);
}

/* States */
.proto-state {
  background: white;
  border: 2px dashed color-mix(in srgb, var(--primary) 35%, white);
  border-radius: 22px;
  padding: 28px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--muted-foreground);
  font-weight: 600;
}
.proto-state-empty {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  text-align: left;
}
.proto-state-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 28px;
  color: var(--primary);
}
.proto-state-sub {
  margin: 0;
  font-size: 14.5px;
  color: var(--muted-foreground);
}

/* Table */
.proto-table {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 22px;
  box-shadow: 5px 5px 0 var(--color-coral);
  overflow: hidden;
}
.proto-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid color-mix(in srgb, var(--primary) 12%, transparent);
}
.proto-row:last-child {
  border-bottom: 0;
}
.proto-row-head {
  display: none;
  background: color-mix(in srgb, var(--primary) 5%, white);
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--muted-foreground);
}
@media (min-width: 980px) {
  .proto-row {
    grid-template-columns: 64px minmax(0, 1.6fr) minmax(0, 1fr) minmax(0, 1.2fr) auto;
    align-items: center;
    gap: 16px;
  }
  .proto-row-head {
    display: grid;
    padding: 10px 18px;
  }
}
.proto-row-data {
  transition: background 0.12s ease;
}
.proto-row-data:hover {
  background: color-mix(in srgb, var(--primary) 4%, transparent);
}

.proto-col-thumb {
  display: none;
}
@media (min-width: 980px) {
  .proto-col-thumb {
    display: block;
  }
}
.proto-thumb {
  display: block;
  width: 64px;
  height: 64px;
  border-radius: 14px;
  object-fit: cover;
  border: 2px solid var(--primary);
}

.proto-col-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.proto-title-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.proto-title-link {
  font-family: var(--font-display);
  font-size: 20px;
  line-height: 1.1;
  color: var(--primary);
  text-decoration: none;
}
.proto-title-link:hover {
  text-decoration: underline;
  text-decoration-color: var(--color-coral);
  text-decoration-thickness: 2px;
}
.proto-status {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 13px;
  text-transform: capitalize;
  background: white;
  border: 1.5px solid var(--primary);
}
.proto-status-leaf {
  background: color-mix(in srgb, var(--color-leaf) 18%, white);
  border-color: var(--color-leaf);
  color: color-mix(in srgb, var(--color-leaf) 60%, var(--primary));
}
.proto-status-ochre {
  background: color-mix(in srgb, var(--color-ochre) 22%, white);
  border-color: var(--color-ochre);
  color: color-mix(in srgb, var(--color-ochre) 50%, var(--primary));
}
.proto-status-coral {
  background: color-mix(in srgb, var(--color-coral) 18%, white);
  border-color: var(--color-coral);
  color: var(--color-coral);
}
.proto-status-ink {
  background: color-mix(in srgb, var(--primary) 10%, white);
}
.proto-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;
  color: var(--muted-foreground);
}
.proto-meta-sep {
  opacity: 0.6;
}

.proto-col-date,
.proto-col-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--primary);
  min-width: 0;
}
.proto-col-stats strong {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 17px;
  color: var(--color-coral);
  margin-right: 4px;
}
.proto-stats-secondary {
  color: var(--muted-foreground);
  font-size: 13px;
}
.proto-icon {
  color: var(--color-coral);
  flex-shrink: 0;
}

.proto-col-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-start;
}
@media (min-width: 980px) {
  .proto-col-actions {
    justify-content: flex-end;
  }
}
.proto-primary-action {
  white-space: nowrap;
}
.proto-overflow-btn {
  width: 36px;
  padding: 0;
}

/* Overflow menu */
.proto-menu {
  display: flex;
  flex-direction: column;
  padding: 6px;
  min-width: 220px;
}
.proto-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 10px;
  background: transparent;
  border: 0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary);
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}
.proto-menu-item:hover:not(:disabled) {
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}
.proto-menu-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.proto-menu-item-danger {
  color: var(--color-coral);
}
.proto-menu-item-danger:hover:not(:disabled) {
  background: color-mix(in srgb, var(--color-coral) 12%, transparent);
}
.proto-menu-divider {
  height: 1px;
  background: color-mix(in srgb, var(--primary) 12%, transparent);
  margin: 4px 0;
}

/* Confirmation modal */
.proto-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: color-mix(in srgb, var(--primary) 50%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.proto-modal {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 22px;
  box-shadow: 6px 6px 0 var(--color-coral);
  padding: 24px;
  max-width: 440px;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.proto-modal-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 22px;
  color: var(--primary);
  line-height: 1.2;
  word-break: break-word;
}
.proto-modal-body {
  margin: 0;
  font-size: 14.5px;
  line-height: 1.5;
  color: var(--muted-foreground);
}
.proto-modal-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.proto-modal-label {
  font-size: 13px;
  color: var(--primary);
}
.proto-modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 6px;
}
</style>
