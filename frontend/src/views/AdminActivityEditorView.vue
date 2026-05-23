<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ArrowLeft, ImagePlus, LoaderCircle } from 'lucide-vue-next'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import DateTimeField from '@/components/DateTimeField.vue'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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
  isOvernight: boolean
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
  isOvernight: false,
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
  isEditing.value
    ? currentActivity.value?.title || 'Edit activity details'
    : 'Draft a new activity',
)
const editorSubheading = computed(() =>
  isEditing.value
    ? 'Fine-tune copy, schedule, pricing or image without leaving the editor.'
    : 'Block out the essentials here, then come back to publish when it is ready.',
)
const submitLabel = computed(() =>
  isEditing.value ? 'Save changes' : 'Create draft',
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
  form.isOvernight = activity?.isOvernight ?? false
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
    isOvernight: form.isOvernight,
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
  <section class="editor-shell">
    <RouterLink to="/admin/activities" class="back-pill">
      <ArrowLeft class="h-4 w-4" />
      <span>back to the desk</span>
    </RouterLink>

    <div class="editor-grid">
      <!-- Left: titles -->
      <div class="editor-intro">
        <h1 class="editor-title">
          <span class="display-text">{{ editorHeading }}</span>
        </h1>
        <p class="editor-sub">{{ editorSubheading }}</p>

        <div class="hint-row">
          <div class="hint-card">
            <p class="hint-label">workflow</p>
            <p class="hint-text">
              {{
                isEditing
                  ? 'Refine details and keep the draft current.'
                  : 'Start with the essentials and save a draft first.'
              }}
            </p>
          </div>
          <div class="hint-card">
            <p class="hint-label hint-label-ochre">card image</p>
            <p class="hint-text">
              A clear landscape shot reads best on the activity card.
            </p>
          </div>
        </div>
      </div>

      <!-- Right: preview -->
      <div class="preview-card">
        <div v-if="isActivityLoading" class="preview-loading">
          <LoaderCircle class="h-5 w-5 animate-spin" />
          <span>Fetching the activity…</span>
        </div>

        <Alert v-else-if="activityQuery.isError.value" variant="destructive">
          {{ mapActivitiesError(activityQuery.error.value) }}
        </Alert>

        <template v-else>
          <div class="preview-tags">
            <span class="craft-tag craft-tag-coral preview-tag-rot">
              {{ isEditing ? (currentActivity?.status ?? 'draft').toLowerCase() : 'new draft' }}
            </span>
            <span class="craft-pill">{{ formatTicketPrice(form.ticketPrice || '0') }}</span>
            <span class="craft-pill">{{ form.visibility.toLowerCase() }}</span>
            <span v-if="form.isOvernight" class="craft-pill">overnight</span>
          </div>

          <div v-if="previewImageUrl" class="preview-image-wrap">
            <img
              :src="previewImageUrl"
              :alt="form.title || 'Activity preview'"
              class="preview-image"
            />
          </div>
          <div v-else class="preview-placeholder">
            <ImagePlus class="h-7 w-7" />
            <p class="preview-placeholder-title">Preview your card image</p>
            <p class="preview-placeholder-sub">
              Add an image to check how the activity will read on the listings page.
            </p>
          </div>

          <div class="preview-meta">
            <p>{{ formatDateRange(form.startAt || null, form.endAt || null) }}</p>
            <p>{{ previewLocationLabel }}</p>
            <p>{{ form.capacity ? `${form.capacity} spaces planned` : 'open capacity' }}</p>
            <div v-if="isEditing && currentActivity" class="preview-links">
              <RouterLink
                class="preview-link"
                :to="{
                  name: 'admin-activity-reservations',
                  params: { activityId: currentActivity.id },
                }"
              >
                View reservations →
              </RouterLink>
              <RouterLink
                class="preview-link"
                :to="{ name: 'activity-detail', params: { activityId: currentActivity.id } }"
              >
                Preview page →
              </RouterLink>
            </div>
          </div>
        </template>
      </div>
    </div>

    <Alert v-if="successMessage" class="success-alert">
      {{ successMessage }}
    </Alert>

    <section v-if="!isEditing || currentActivity" class="form-card">
      <form
        class="editor-form"
        @submit.prevent="saveMutation.mutate()"
      >
        <Alert v-if="formError" variant="destructive">
          {{ formError }}
        </Alert>

        <div class="field-row">
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Title</span>
              <span class="field-label-hint">keep it punchy</span>
            </span>
            <input v-model="form.title" class="craft-field" maxlength="120" required />
          </label>
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Ticket price</span>
              <span class="field-label-hint">in euro, 0 is free</span>
            </span>
            <input
              v-model="form.ticketPrice"
              class="craft-field"
              min="0"
              step="0.01"
              type="number"
            />
          </label>
        </div>

        <label class="field">
          <span class="field-label">
            <span class="field-label-text">Description</span>
            <span class="field-label-hint">what's the vibe?</span>
          </span>
          <textarea
            v-model="form.description"
            class="craft-field craft-field-textarea"
            maxlength="5000"
          />
        </label>

        <div class="field-row">
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Start</span>
              <span class="field-label-hint">when we kick off</span>
            </span>
            <DateTimeField v-model="form.startAt" placeholder="Select the activity start" />
          </label>
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">End</span>
              <span class="field-label-hint">all wrapped up</span>
            </span>
            <DateTimeField v-model="form.endAt" placeholder="Select the activity end" />
          </label>
        </div>

        <label class="check-row">
          <input v-model="form.isOvernight" type="checkbox" />
          <span>
            <span class="check-title">Overnight activity</span>
            <span class="check-hint">we'll sleep somewhere not home</span>
          </span>
        </label>

        <div class="field-row">
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Location name</span>
              <span class="field-label-hint">e.g. Glendalough</span>
            </span>
            <input v-model="form.locationName" class="craft-field" maxlength="160" />
          </label>
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Address</span>
              <span class="field-label-hint">full address or meeting point</span>
            </span>
            <input v-model="form.locationAddress" class="craft-field" maxlength="255" />
          </label>
        </div>

        <div class="field-row">
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Capacity</span>
              <span class="field-label-hint">empty = no cap</span>
            </span>
            <input v-model="form.capacity" class="craft-field" min="1" type="number" />
          </label>
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Visibility</span>
              <span class="field-label-hint">who can find it</span>
            </span>
            <Select v-model="form.visibility">
              <SelectTrigger aria-label="Visibility">
                <SelectValue placeholder="Select visibility" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="option in visibilityOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </label>
        </div>

        <div class="field-row">
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Reservations open</span>
              <span class="field-label-hint">go-live moment</span>
            </span>
            <DateTimeField
              v-model="form.reservationOpensAt"
              placeholder="Select the opening time"
            />
          </label>
          <label class="field">
            <span class="field-label">
              <span class="field-label-text">Reservations close</span>
              <span class="field-label-hint">final call</span>
            </span>
            <DateTimeField
              v-model="form.reservationClosesAt"
              placeholder="Select the closing time"
            />
          </label>
        </div>

        <label class="field">
          <span class="field-label">
            <span class="field-label-text">Activity image</span>
            <span class="field-label-hint">jpg / png / webp</span>
          </span>
          <input
            :key="fileInputKey"
            accept="image/jpeg,image/png,image/webp"
            class="craft-field"
            type="file"
            @change="handleImageChange"
          />
        </label>

        <div class="form-actions">
          <Button size="lg" type="button" variant="outline" @click="clearLocalChanges">
            {{ isEditing ? 'Reset changes' : 'Clear form' }}
          </Button>
          <Button
            :disabled="saveMutation.isPending.value || isActivityLoading"
            class="form-submit"
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

<style scoped>
.editor-shell {
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

.editor-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 28px;
  align-items: start;
}
@media (min-width: 1100px) {
  .editor-grid {
    grid-template-columns: 0.95fr 1.05fr;
  }
}

.editor-intro {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}
.editor-title {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(36px, 5vw, 56px);
  line-height: 0.98;
  color: var(--primary);
  letter-spacing: -0.01em;
  word-break: break-word;
}
.editor-sub {
  max-width: 50ch;
  font-size: 16px;
  color: var(--muted-foreground);
  line-height: 1.55;
  margin: 0;
}

.hint-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  width: 100%;
}
@media (min-width: 640px) {
  .hint-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
.hint-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 22px;
  padding: 16px;
  box-shadow: 3px 3px 0 var(--color-leaf);
}
.hint-label {
  margin: 0 0 6px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 19px;
  color: var(--color-leaf);
}
.hint-label-ochre {
  color: var(--color-ochre);
}
.hint-text {
  margin: 0;
  font-size: 14.5px;
  color: var(--primary);
  font-weight: 600;
  line-height: 1.45;
}

.preview-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 24px;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.preview-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted-foreground);
  font-weight: 600;
}
.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.preview-tag-rot {
  transform: rotate(-3deg);
}
.preview-image-wrap {
  border: 2px solid var(--primary);
  border-radius: 22px;
  overflow: hidden;
  padding: 0;
}
.preview-image {
  display: block;
  width: 100%;
  height: 18rem;
  object-fit: cover;
}
.preview-placeholder {
  border: 2px dashed color-mix(in srgb, var(--primary) 30%, white);
  border-radius: 22px;
  background: color-mix(in srgb, var(--color-ochre) 8%, white);
  padding: 36px 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  text-align: center;
}
.preview-placeholder svg {
  color: var(--color-coral);
}
.preview-placeholder-title {
  margin: 6px 0 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 22px;
  color: var(--primary);
}
.preview-placeholder-sub {
  margin: 0;
  font-size: 14px;
  color: var(--muted-foreground);
  max-width: 40ch;
  line-height: 1.5;
}

.preview-meta {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
  font-size: 14px;
  color: var(--primary);
  border-top: 1.5px dashed color-mix(in srgb, var(--primary) 25%, transparent);
  padding-top: 14px;
}
@media (min-width: 640px) {
  .preview-meta {
    grid-template-columns: repeat(2, 1fr);
  }
}
.preview-meta p {
  margin: 0;
  word-break: break-word;
}
.preview-links {
  display: flex;
  gap: 14px;
  grid-column: 1 / -1;
  margin-top: 4px;
}
.preview-link {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-coral);
  text-decoration: none;
}

.success-alert {
  background: color-mix(in srgb, var(--color-leaf) 14%, white) !important;
  border-color: var(--color-leaf) !important;
  color: var(--primary) !important;
}

.form-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 32px;
  padding: 28px;
  box-shadow:
    4px 4px 0 var(--color-leaf),
    8px 8px 0 var(--primary);
}
@media (min-width: 640px) {
  .form-card {
    padding: 36px;
  }
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
}
@media (min-width: 640px) {
  .field-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-label {
  display: flex;
  align-items: baseline;
  gap: 10px;
  justify-content: space-between;
}
.field-label-text {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 20px;
  color: var(--primary);
  line-height: 1;
}
.field-label-hint {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 15px;
  color: var(--color-coral);
}

.check-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: color-mix(in srgb, var(--color-ochre) 12%, white);
  border: 2px solid var(--primary);
  border-radius: 22px;
  padding: 14px 16px;
  cursor: pointer;
}
.check-row input[type='checkbox'] {
  appearance: none;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  border: 2px solid var(--primary);
  background: white;
  display: inline-grid;
  place-content: center;
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 1px;
}
.check-row input[type='checkbox']:checked {
  background: var(--color-coral);
}
.check-row input[type='checkbox']:checked::after {
  content: '';
  width: 10px;
  height: 6px;
  border-left: 2.5px solid white;
  border-bottom: 2.5px solid white;
  transform: rotate(-45deg) translate(1px, -1px);
}
.check-title {
  display: block;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 20px;
  color: var(--primary);
  line-height: 1.1;
}
.check-hint {
  display: block;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 16px;
  color: var(--color-coral);
}

.form-actions {
  display: flex;
  flex-direction: column-reverse;
  gap: 12px;
  padding-top: 8px;
  border-top: 1.5px dashed color-mix(in srgb, var(--primary) 25%, transparent);
}
@media (min-width: 640px) {
  .form-actions {
    flex-direction: row;
    justify-content: flex-end;
  }
}
.form-submit {
  min-width: 14rem;
}
</style>
