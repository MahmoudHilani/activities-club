<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { LoaderCircle, PencilLine, Plus, Trash2 } from 'lucide-vue-next'
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import DateTimeField from '@/components/DateTimeField.vue'
import { Alert } from '@/components/ui/alert'
import { buttonVariants, Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  cancelAdminActivity,
  createAdminActivity,
  deleteAdminActivity,
  getAdminActivities,
  publishAdminActivity,
  updateAdminActivity,
  type ActivityUpsertPayload,
} from '@/lib/api/activities'
import { mapActivitiesError, mapAdminActivityError } from '@/lib/api/errors'
import type { ActivityResponse } from '@/lib/api/types'
import { formatAvailability, formatDateRange, formatTicketPrice } from '@/lib/formatters'

interface ActivityFormState {
  title: string
  description: string
  startAt: string
  endAt: string
  locationName: string
  locationAddress: string
  capacity: string
  ticketPrice: string
  visibility: 'PUBLIC' | 'PRIVATE'
  reservationOpensAt: string
  reservationClosesAt: string
  imageFile: File | null
}

const queryClient = useQueryClient()
const selectedActivity = ref<ActivityResponse | null>(null)
const formError = ref('')
const previewUrl = ref('')
const form = reactive<ActivityFormState>({
  title: '',
  description: '',
  startAt: '',
  endAt: '',
  locationName: '',
  locationAddress: '',
  capacity: '',
  ticketPrice: '0',
  visibility: 'PUBLIC',
  reservationOpensAt: '',
  reservationClosesAt: '',
  imageFile: null,
})

const visibilityOptions = [
  { label: 'Public', value: 'PUBLIC' },
  { label: 'Private', value: 'PRIVATE' },
] as const

const activitiesQuery = useQuery(() => ({
  queryKey: ['admin-activities'],
  queryFn: () => getAdminActivities(),
}))

const activities = computed(() => activitiesQuery.data.value?.content ?? [])
const isEditing = computed(() => Boolean(selectedActivity.value))

const saveMutation = useMutation(() => ({
  mutationFn: async () => {
    const payload = toPayload()

    if (selectedActivity.value) {
      return updateAdminActivity(selectedActivity.value.id, payload, form.imageFile)
    }
    if (!form.imageFile) {
      throw new Error('Activity image is required')
    }

    return createAdminActivity(payload, form.imageFile)
  },
  onSuccess: async (activity) => {
    formError.value = ''
    resetForm(activity)
    await invalidateActivityQueries()
  },
  onError: (error) => {
    formError.value =
      error instanceof Error && error.message === 'Activity image is required'
        ? error.message
        : mapAdminActivityError(error)
  },
}))

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
  onSuccess: async (_, activityId) => {
    if (selectedActivity.value?.id === activityId) {
      clearSelection()
    }
    await invalidateActivityQueries()
  },
}))

function resetForm(activity?: ActivityResponse | null): void {
  selectedActivity.value = activity ?? null
  form.title = activity?.title ?? ''
  form.description = activity?.description ?? ''
  form.startAt = toDateTimeInput(activity?.startAt)
  form.endAt = toDateTimeInput(activity?.endAt)
  form.locationName = activity?.locationName ?? ''
  form.locationAddress = activity?.locationAddress ?? ''
  form.capacity = activity?.capacity ? String(activity.capacity) : ''
  form.ticketPrice = activity?.ticketPrice ?? '0'
  form.visibility = activity?.visibility ?? 'PUBLIC'
  form.reservationOpensAt = toDateTimeInput(activity?.reservationOpensAt)
  form.reservationClosesAt = toDateTimeInput(activity?.reservationClosesAt)
  form.imageFile = null
  setPreviewUrl('')
  formError.value = ''
}

function clearSelection(): void {
  resetForm(null)
}

function editActivity(activity: ActivityResponse): void {
  resetForm(activity)
}

function handleImageChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  form.imageFile = file

  if (!file) {
    setPreviewUrl('')
    return
  }

  setPreviewUrl(URL.createObjectURL(file))
}

function setPreviewUrl(url: string): void {
  if (previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }

  previewUrl.value = url
}

function canDelete(activity: ActivityResponse): boolean {
  return activity.status === 'DRAFT' && activity.confirmedReservationCount === 0 && activity.waitlistCount === 0
}

function toPayload(): ActivityUpsertPayload {
  return {
    title: form.title.trim(),
    description: nullableText(form.description),
    startAt: toIsoString(form.startAt),
    endAt: toIsoString(form.endAt),
    locationName: nullableText(form.locationName),
    locationAddress: nullableText(form.locationAddress),
    capacity: form.capacity ? Number(form.capacity) : null,
    ticketPrice: Number(form.ticketPrice || '0'),
    visibility: form.visibility,
    reservationOpensAt: toIsoString(form.reservationOpensAt),
    reservationClosesAt: toIsoString(form.reservationClosesAt),
  }
}

async function invalidateActivityQueries(): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-activities'] }),
    queryClient.invalidateQueries({ queryKey: ['public-activities'] }),
  ])
}

function toDateTimeInput(value?: string | null): string {
  if (!value) {
    return ''
  }

  return new Date(value).toISOString().slice(0, 16)
}

function toIsoString(value: string): string | null {
  return value ? new Date(value).toISOString() : null
}

function nullableText(value: string): string | null {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

onBeforeUnmount(() => {
  if (previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<template>
  <section class="grid flex-1 gap-8 xl:grid-cols-[1.02fr_0.98fr]">
    <div class="space-y-6">
      <div class="space-y-4">
        <p class="inline-flex rounded-full bg-white/80 px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          Admin dashboard
        </p>
        <h1 class="headline-balance max-w-3xl font-serif text-5xl font-bold tracking-tight text-foreground sm:text-6xl">
          Manage the activity pipeline and reservation load.
        </h1>
        <p class="max-w-2xl text-lg leading-8 text-muted-foreground">
          Create activities, upload the card image, adjust pricing and windows, then publish when ready.
        </p>
      </div>

      <Alert v-if="activitiesQuery.isError.value" variant="destructive">
        {{ mapActivitiesError(activitiesQuery.error.value) }}
      </Alert>

      <div class="grid gap-4">
        <article
          v-for="activity in activities"
          :key="activity.id"
          class="surface-panel rounded-[1.75rem] border border-white/70 p-5"
        >
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div class="flex gap-4">
              <img :src="activity.imageUrl" :alt="activity.title" class="h-24 w-24 rounded-2xl object-cover" />
              <div>
                <div class="flex flex-wrap items-center gap-2">
                  <h2 class="text-xl font-bold text-foreground">{{ activity.title }}</h2>
                  <span class="rounded-full bg-secondary px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-secondary-foreground">
                    {{ activity.status }}
                  </span>
                  <span class="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                    {{ formatTicketPrice(activity.ticketPrice) }}
                  </span>
                </div>
                <p class="mt-2 text-sm leading-6 text-muted-foreground">
                  {{ activity.description || 'No description added yet.' }}
                </p>
                <p class="mt-3 text-sm text-foreground">{{ formatDateRange(activity.startAt, activity.endAt) }}</p>
                <p class="mt-1 text-sm text-muted-foreground">
                  {{ activity.confirmedReservationCount }} confirmed |
                  {{ activity.waitlistCount }} waitlisted |
                  {{ formatAvailability(activity.availableSpots, activity.atCapacity) }}
                </p>
              </div>
            </div>

            <div class="flex flex-wrap gap-2">
              <RouterLink
                :class="buttonVariants({ variant: 'outline', size: 'sm' })"
                :to="{ name: 'admin-activity-reservations', params: { activityId: activity.id } }"
              >
                View reservations
              </RouterLink>
              <Button size="sm" variant="outline" @click="editActivity(activity)">
                <PencilLine class="h-4 w-4" />
                Edit
              </Button>
              <Button
                v-if="activity.status === 'DRAFT'"
                :disabled="publishMutation.isPending.value"
                size="sm"
                @click="publishMutation.mutate(activity.id)"
              >
                Publish
              </Button>
              <Button
                v-if="activity.status !== 'CANCELLED' && activity.status !== 'COMPLETED'"
                :disabled="cancelMutation.isPending.value"
                size="sm"
                variant="outline"
                @click="cancelMutation.mutate(activity.id)"
              >
                Cancel
              </Button>
              <Button
                v-if="canDelete(activity)"
                :disabled="deleteMutation.isPending.value"
                size="sm"
                variant="outline"
                @click="deleteMutation.mutate(activity.id)"
              >
                <Trash2 class="h-4 w-4" />
                Delete
              </Button>
            </div>
          </div>
        </article>
      </div>
    </div>

    <section class="surface-panel rounded-[2rem] border border-white/70 p-6 sm:p-8">
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.2em] text-muted-foreground">
            {{ isEditing ? 'Edit activity' : 'Create activity' }}
          </p>
          <h2 class="mt-2 text-3xl font-bold tracking-tight text-foreground">
            {{ isEditing ? selectedActivity?.title : 'New activity draft' }}
          </h2>
        </div>

        <Button size="sm" variant="outline" @click="clearSelection">
          <Plus class="h-4 w-4" />
          New
        </Button>
      </div>

      <Alert v-if="formError" class="mt-5" variant="destructive">
        {{ formError }}
      </Alert>

      <form class="mt-6 space-y-4" @submit.prevent="saveMutation.mutate()">
        <div class="grid gap-4 sm:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Title</span>
            <input v-model="form.title" class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3" maxlength="120" required />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Ticket price</span>
            <input v-model="form.ticketPrice" class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3" min="0" step="0.01" type="number" />
          </label>
        </div>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-foreground">Description</span>
          <textarea v-model="form.description" class="min-h-32 w-full rounded-2xl border border-border bg-white/70 px-4 py-3" maxlength="5000" />
        </label>

        <div class="grid gap-4 sm:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Start time</span>
            <DateTimeField v-model="form.startAt" placeholder="Select the activity start" />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">End time</span>
            <DateTimeField v-model="form.endAt" placeholder="Select the activity end" />
          </label>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Location name</span>
            <input v-model="form.locationName" class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3" maxlength="160" />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Location address</span>
            <input v-model="form.locationAddress" class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3" maxlength="255" />
          </label>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Capacity</span>
            <input v-model="form.capacity" class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3" min="1" type="number" />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Visibility</span>
            <Select v-model="form.visibility">
              <SelectTrigger aria-label="Visibility">
                <SelectValue placeholder="Select visibility" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="option in visibilityOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </label>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Reservations open</span>
            <DateTimeField v-model="form.reservationOpensAt" placeholder="Select the opening time" />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Reservations close</span>
            <DateTimeField v-model="form.reservationClosesAt" placeholder="Select the closing time" />
          </label>
        </div>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-foreground">Activity image</span>
          <input accept="image/jpeg,image/png,image/webp" class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3" type="file" @change="handleImageChange" />
        </label>

        <div v-if="previewUrl || selectedActivity?.imageUrl" class="overflow-hidden rounded-[1.5rem] border border-white/70">
          <img :src="previewUrl || selectedActivity?.imageUrl" :alt="form.title || 'Activity preview'" class="h-56 w-full object-cover" />
        </div>

        <Button class="w-full" :disabled="saveMutation.isPending.value" size="lg" type="submit">
          <LoaderCircle v-if="saveMutation.isPending.value" class="h-4 w-4 animate-spin" />
          {{ isEditing ? 'Save activity changes' : 'Create activity draft' }}
        </Button>
      </form>
    </section>
  </section>
</template>
