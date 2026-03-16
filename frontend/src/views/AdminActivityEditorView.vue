<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ArrowLeft, ImagePlus, LoaderCircle } from 'lucide-vue-next'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import DateTimeField from '@/components/DateTimeField.vue'
import { Alert } from '@/components/ui/alert'
import { buttonVariants, Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  createAdminActivity,
  getAdminActivity,
  updateAdminActivity,
  type ActivityUpsertPayload,
} from '@/lib/api/activities'
import { mapActivitiesError, mapAdminActivityError } from '@/lib/api/errors'
import type { ActivityResponse } from '@/lib/api/types'
import { formatDateRange, formatTicketPrice } from '@/lib/formatters'

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

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const formError = ref('')
const successMessage = ref('')
const previewUrl = ref('')
const fileInputKey = ref(0)
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

const isEditing = computed(() => route.name === 'admin-activity-edit')
const activityId = computed(() => Number(route.params.activityId))

const activityQuery = useQuery(() => ({
  enabled: isEditing.value && Number.isInteger(activityId.value) && activityId.value > 0,
  queryKey: ['admin-activity', activityId.value],
  queryFn: () => getAdminActivity(activityId.value),
}))

const currentActivity = computed(() => activityQuery.data.value ?? null)
const isActivityLoading = computed(() => isEditing.value && activityQuery.isPending.value)
const previewImageUrl = computed(() => previewUrl.value || currentActivity.value?.imageUrl || '')
const previewLocationLabel = computed(() => {
  if (!form.locationName && !form.locationAddress) {
    return 'Location pending'
  }

  return `${form.locationName || 'Location name pending'}${form.locationAddress ? `, ${form.locationAddress}` : ''}`
})
const editorHeading = computed(() =>
  isEditing.value ? currentActivity.value?.title || 'Edit activity details' : 'Create a new activity draft',
)
const editorSubheading = computed(() =>
  isEditing.value
    ? 'Update the activity copy, schedule, pricing, or image without leaving the editor.'
    : 'Build the activity draft here, then return to management when it is ready to publish.',
)
const submitLabel = computed(() =>
  isEditing.value ? 'Save activity changes' : 'Create activity draft',
)

const saveMutation = useMutation(() => ({
  mutationFn: async () => {
    const payload = toPayload()

    if (isEditing.value) {
      if (!activityId.value) {
        throw new Error('Activity not found')
      }

      return updateAdminActivity(activityId.value, payload, form.imageFile)
    }

    if (!form.imageFile) {
      throw new Error('Activity image is required')
    }

    return createAdminActivity(payload, form.imageFile)
  },
  onSuccess: async (activity) => {
    const wasEditing = isEditing.value

    formError.value = ''
    successMessage.value = wasEditing
      ? 'Activity changes saved.'
      : 'Activity draft created. You can keep editing here or return to management to publish later.'
    queryClient.setQueryData(['admin-activity', activity.id], activity)
    resetForm(activity)

    if (!wasEditing) {
      await router.replace(`/admin/activities/${activity.id}/edit`)
    }

    await invalidateActivityQueries(activity.id)
  },
  onError: (error) => {
    formError.value =
      error instanceof Error &&
      (error.message === 'Activity image is required' || error.message === 'Activity not found')
        ? error.message
        : mapAdminActivityError(error)
    successMessage.value = ''
  },
}))

watch(
  [isEditing, currentActivity],
  ([editing, activity]) => {
    if (!editing) {
      resetForm(null)
      return
    }

    if (activity) {
      resetForm(activity)
    }
  },
  { immediate: true },
)

function resetForm(activity?: ActivityResponse | null): void {
  formError.value = ''
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
  fileInputKey.value += 1
  setPreviewUrl('')
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

function clearLocalChanges(): void {
  successMessage.value = ''
  formError.value = ''
  resetForm(isEditing.value ? currentActivity.value : null)
}

function setPreviewUrl(url: string): void {
  if (previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }

  previewUrl.value = url
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

async function invalidateActivityQueries(savedActivityId: number): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-activities'] }),
    queryClient.invalidateQueries({ queryKey: ['admin-activity', savedActivityId] }),
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
  <section class="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-8">
    <div class="space-y-5">
      <RouterLink
        :class="buttonVariants({ variant: 'outline', size: 'sm' })"
        :to="{ name: 'admin-activities' }"
      >
        <ArrowLeft class="h-4 w-4" />
        Back to activity management
      </RouterLink>

      <div class="grid gap-6 xl:grid-cols-[0.92fr_1.08fr] xl:items-start">
        <div class="space-y-4">
          <p class="inline-flex rounded-full bg-white/80 px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
            Activity editor
          </p>
          <h1 class="headline-balance max-w-3xl break-words font-serif text-5xl font-bold tracking-tight text-foreground sm:text-6xl">
            {{ editorHeading }}
          </h1>
          <p class="max-w-2xl text-lg leading-8 text-muted-foreground">
            {{ editorSubheading }}
          </p>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="surface-panel rounded-[1.6rem] border border-white/70 p-5">
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Workflow
              </p>
              <p class="mt-2 text-base font-semibold text-foreground">
                {{ isEditing ? 'Refine details and keep the draft current.' : 'Start with the essentials and save a draft first.' }}
              </p>
            </div>

            <div class="surface-panel rounded-[1.6rem] border border-white/70 p-5">
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
                Card image
              </p>
              <p class="mt-2 text-base font-semibold text-foreground">
                Upload a clear landscape image to keep cards readable across the site.
              </p>
            </div>
          </div>
        </div>

        <div class="surface-panel rounded-[2rem] border border-white/70 p-6 sm:p-8">
          <div v-if="isActivityLoading" class="space-y-3 text-muted-foreground">
            <p class="text-sm font-semibold uppercase tracking-[0.18em]">Loading activity</p>
            <p>Fetching the latest activity details before opening the editor.</p>
          </div>

          <Alert v-else-if="activityQuery.isError.value" variant="destructive">
            {{ mapActivitiesError(activityQuery.error.value) }}
          </Alert>

          <div v-else class="space-y-4">
            <div class="flex flex-wrap items-center gap-3">
              <span class="rounded-full bg-secondary px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-secondary-foreground">
                {{ isEditing ? currentActivity?.status ?? 'DRAFT' : 'NEW DRAFT' }}
              </span>
              <span class="rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                {{ formatTicketPrice(form.ticketPrice || '0') }}
              </span>
              <span class="rounded-full bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                {{ form.visibility }}
              </span>
            </div>

            <div v-if="previewImageUrl" class="overflow-hidden rounded-[1.5rem] border border-white/70">
              <img
                :src="previewImageUrl"
                :alt="form.title || 'Activity preview'"
                class="h-64 w-full object-cover"
              />
            </div>
            <div
              v-else
              class="flex min-h-64 items-center justify-center rounded-[1.5rem] border border-dashed border-border bg-white/55 px-6 text-center text-muted-foreground"
            >
              <div class="max-w-sm">
                <ImagePlus class="mx-auto h-6 w-6 text-primary" />
                <p class="mt-3 text-base font-semibold text-foreground">Preview the activity card image here.</p>
                <p class="mt-2 text-sm leading-6">
                  Add an image to check how the activity will read before it appears in the feed.
                </p>
              </div>
            </div>

            <div class="grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
              <p class="break-words">{{ formatDateRange(form.startAt || null, form.endAt || null) }}</p>
              <p class="break-words">{{ previewLocationLabel }}</p>
              <p>{{ form.capacity ? `${form.capacity} spaces planned` : 'Open capacity' }}</p>
              <RouterLink
                v-if="isEditing && currentActivity"
                class="font-semibold text-primary"
                :to="{ name: 'admin-activity-reservations', params: { activityId: currentActivity.id } }"
              >
                View reservations
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Alert v-if="successMessage" class="max-w-4xl">
      {{ successMessage }}
    </Alert>

    <section class="surface-panel rounded-[2rem] border border-white/70 p-6 sm:p-8">
      <form
        v-if="!isEditing || currentActivity"
        class="space-y-6"
        @submit.prevent="saveMutation.mutate()"
      >
        <Alert v-if="formError" variant="destructive">
          {{ formError }}
        </Alert>

        <div class="grid gap-4 sm:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Title</span>
            <input
              v-model="form.title"
              class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3"
              maxlength="120"
              required
            />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Ticket price</span>
            <input
              v-model="form.ticketPrice"
              class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3"
              min="0"
              step="0.01"
              type="number"
            />
          </label>
        </div>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-foreground">Description</span>
          <textarea
            v-model="form.description"
            class="min-h-36 w-full rounded-2xl border border-border bg-white/70 px-4 py-3"
            maxlength="5000"
          />
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
            <input
              v-model="form.locationName"
              class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3"
              maxlength="160"
            />
          </label>
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Location address</span>
            <input
              v-model="form.locationAddress"
              class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3"
              maxlength="255"
            />
          </label>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <label class="space-y-2">
            <span class="text-sm font-semibold text-foreground">Capacity</span>
            <input
              v-model="form.capacity"
              class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3"
              min="1"
              type="number"
            />
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
          <input
            :key="fileInputKey"
            accept="image/jpeg,image/png,image/webp"
            class="w-full rounded-2xl border border-border bg-white/70 px-4 py-3"
            type="file"
            @change="handleImageChange"
          />
        </label>

        <div class="flex flex-col-reverse gap-3 border-t border-white/70 pt-2 sm:flex-row sm:justify-end">
          <Button
            size="lg"
            type="button"
            variant="outline"
            @click="clearLocalChanges"
          >
            {{ isEditing ? 'Reset changes' : 'Clear form' }}
          </Button>
            <Button
            :disabled="saveMutation.isPending.value || isActivityLoading"
            class="sm:min-w-56"
            size="lg"
            type="button"
            @click="saveMutation.mutate()"
          >
            <LoaderCircle v-if="saveMutation.isPending.value" class="h-4 w-4 animate-spin" />
            {{ submitLabel }}
          </Button>
        </div>
      </form>
    </section>
  </section>
</template>
