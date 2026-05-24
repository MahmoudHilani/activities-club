<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ArrowLeft, CalendarDays, LoaderCircle, MapPin } from 'lucide-vue-next'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
const ctaStrip = ref<HTMLElement | null>(null)
const ctaFooterOffset = ref(0)
let ctaPositionFrame: number | null = null

function parseActivityId(value: unknown): number | null {
  const parsed = Number(Array.isArray(value) ? value[0] : value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

const activityId = computed(() => parseActivityId(route.params.activityId))
const detailPath = computed(() => route.fullPath)
const isAdmin = computed(() => sessionStore.user?.isAdmin === true)
const detailQueryKey = computed(() => [
  'activity-detail',
  activityId.value,
  sessionStore.user?.id ?? 'anonymous',
  sessionStore.user?.isAdmin ? 'admin' : 'member',
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
const locationFullLabel = computed(() =>
  formatLocation(activity.value?.locationName, activity.value?.locationAddress),
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

function updateCtaPosition(): void {
  ctaPositionFrame = null

  const strip = ctaStrip.value
  const footer = document.querySelector<HTMLElement>('.app-footer')
  if (!strip || !footer) {
    ctaFooterOffset.value = 0
    return
  }

  const bottomOffset = Number.parseFloat(window.getComputedStyle(strip).bottom) || 0
  const footerClearance = 64
  const stripBottom = window.innerHeight - bottomOffset + footerClearance

  ctaFooterOffset.value = Math.max(0, Math.ceil(stripBottom - footer.getBoundingClientRect().top))
}

function scheduleCtaPosition(): void {
  if (ctaPositionFrame !== null) {
    return
  }

  ctaPositionFrame = window.requestAnimationFrame(updateCtaPosition)
}

watch(ctaStrip, (strip) => {
  if (strip) {
    scheduleCtaPosition()
  }
})

onMounted(() => {
  scheduleCtaPosition()
  window.addEventListener('scroll', scheduleCtaPosition, { passive: true })
  window.addEventListener('resize', scheduleCtaPosition)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', scheduleCtaPosition)
  window.removeEventListener('resize', scheduleCtaPosition)

  if (ctaPositionFrame !== null) {
    window.cancelAnimationFrame(ctaPositionFrame)
  }
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
  <section class="detail-shell">
    <div v-if="isInitialLoading" class="detail-grid">
      <div class="detail-skel-col">
        <div class="skel-chip" />
        <div class="skel-title" />
        <div class="skel-meta-row">
          <div v-for="i in 3" :key="i" class="skel-meta" />
        </div>
        <div class="skel-block" />
      </div>
      <div class="skel-photo" />
    </div>

    <div v-else-if="isNotFound" class="not-found-card">
      <h1 class="not-found-title">
        <span class="display-text">This activity</span>
        <span class="hand-text"> wandered off</span>
      </h1>
      <p class="not-found-blurb">
        It might have been unpublished, cancelled, or the link might be a tad wrong. No bother —
        head back to the listings.
      </p>
      <div>
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

    <article v-else-if="activity" class="detail-article">
      <RouterLink :to="{ name: 'activities' }" class="back-pill">
        <ArrowLeft class="h-4 w-4" />
        <span>back to the calendar</span>
      </RouterLink>

      <div class="detail-grid">
        <!-- Left: text column -->
        <div class="detail-left">
          <div class="title-stack">
            <span class="craft-tag craft-tag-coral title-tag">{{ scheduleHeadline }}</span>
            <h1 class="detail-title">{{ activity.title }}</h1>
            <p class="detail-sub">
              <MapPin class="h-4 w-4" />
              <span>{{ locationFullLabel }}</span>
            </p>
          </div>

          <Alert v-if="isAdminPreview" class="admin-preview-alert">
            This activity isn't publicly available yet — you're seeing the admin preview.
          </Alert>

          <div class="story-card">
            <div class="story-header">
              <span class="story-eyebrow">the story</span>
              <h2 class="story-title">
                <span class="display-text">What to</span>
                <span class="hand-text"> expect</span>
              </h2>
            </div>
            <p class="story-body">{{ descriptionText }}</p>
          </div>
        </div>

        <!-- Right: photo and sidebar -->
        <div class="detail-right">
          <div class="photo-frame">
            <img :src="activity.imageUrl" :alt="activity.title" class="photo-frame-img" />
            <span class="photo-sticker">save the date</span>
          </div>

          <aside class="side-card">
            <div class="side-row">
              <div class="side-ico ico-coral">
                <CalendarDays class="h-5 w-5" />
              </div>
              <div>
                <p class="side-eyebrow">date and time</p>
                <p class="side-value">{{ scheduleRange }}</p>
              </div>
            </div>
            <div class="dashed-divider side-divider" />
            <div class="side-row">
              <div class="side-ico ico-ochre">
                <MapPin class="h-5 w-5" />
              </div>
              <div>
                <p class="side-eyebrow">location</p>
                <p class="side-value">{{ activity.locationName || 'TBC' }}</p>
                <p class="side-sub">
                  {{ activity.locationAddress || 'The organiser will share venue details soon.' }}
                </p>
              </div>
            </div>
            <div class="dashed-divider side-divider" />
            <dl class="side-dl">
              <div class="side-dl-row">
                <dt>Price</dt>
                <dd>{{ ticketLabel }}</dd>
              </div>
              <div class="side-dl-row">
                <dt>Spots</dt>
                <dd>{{ availabilityLabel }}</dd>
              </div>
              <div v-if="reservationWindowLabel" class="side-dl-row">
                <dt>Window</dt>
                <dd>{{ reservationWindowLabel }}</dd>
              </div>
            </dl>
          </aside>
        </div>
      </div>

      <!-- Sticky reservation CTA strip -->
      <div
        ref="ctaStrip"
        class="cta-strip"
        role="region"
        aria-label="Reservation actions"
        :style="{ transform: `translateY(-${ctaFooterOffset}px)` }"
      >
        <div class="cta-strip-inner">
          <div class="cta-info">
            <p class="cta-when">{{ scheduleHeadline }}</p>
            <h2 class="cta-title">{{ activity.title }}</h2>
            <p class="cta-status">
              <span class="cta-spots">{{ availabilityLabel }}</span>
            </p>
          </div>
          <div class="cta-actions">
            <Alert
              v-if="reservationError"
              class="cta-alert"
              variant="destructive"
            >
              {{ reservationError }}
            </Alert>
            <div class="cta-buttons">
              <span class="cta-price">{{ ticketLabel }}</span>
              <Button
                class="cta-primary"
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
    </article>
  </section>
</template>

<style scoped>
.detail-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 2rem;
  padding-bottom: 9rem;
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
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
  width: max-content;
}
.back-pill:hover {
  transform: translate(-2px, -2px);
  box-shadow: 4px 4px 0 var(--color-coral);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;
  align-items: start;
}
@media (min-width: 1100px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr) 30rem;
    gap: 40px;
  }
}

.detail-left {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.title-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.title-tag {
  align-self: flex-start;
  box-shadow: 2px 2px 0 var(--primary);
  transform: rotate(-2deg);
}
.detail-title {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(40px, 6vw, 68px);
  line-height: 0.98;
  letter-spacing: -0.02em;
  color: var(--primary);
  text-wrap: balance;
}
.detail-sub {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 22px;
  color: var(--color-coral);
}

.admin-preview-alert {
  border-color: var(--color-ochre) !important;
  background: color-mix(in srgb, var(--color-ochre) 16%, white) !important;
  color: var(--primary) !important;
}

.story-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 28px 26px 30px;
  box-shadow: 5px 5px 0 var(--color-leaf);
}
.story-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 16px;
}
.story-eyebrow {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-weight: 700;
  color: var(--muted-foreground);
}
.story-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 36px;
  line-height: 1;
  color: var(--primary);
}
.story-title .hand-text {
  font-size: 1.1em;
}
.story-body {
  margin: 0;
  white-space: pre-line;
  font-size: 16px;
  line-height: 1.7;
  color: var(--primary);
}

.detail-right {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.photo-frame {
  position: relative;
  border: 2px solid var(--primary);
  background: white;
  border-radius: 28px;
  padding: 10px;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
}
.photo-frame-img {
  display: block;
  width: 100%;
  aspect-ratio: 5 / 4;
  object-fit: cover;
  border-radius: 20px;
}
.photo-sticker {
  position: absolute;
  bottom: -12px;
  right: 18px;
  background: var(--color-coral);
  color: white;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 19px;
  padding: 5px 16px;
  border-radius: 999px;
  box-shadow: 2px 2px 0 var(--primary);
  transform: rotate(4deg);
}

.side-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 26px;
  padding: 24px;
  box-shadow: 4px 4px 0 var(--primary);
}
@media (min-width: 1100px) {
  .side-card {
    position: sticky;
    top: 6rem;
  }
}
.side-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.side-ico {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  border: 2px solid var(--primary);
  flex-shrink: 0;
}
.side-ico svg {
  color: var(--primary);
}
.ico-coral {
  background: color-mix(in srgb, var(--color-coral) 22%, white);
}
.ico-ochre {
  background: color-mix(in srgb, var(--color-ochre) 22%, white);
}
.side-eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted-foreground);
}
.side-value {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 22px;
  line-height: 1.1;
  color: var(--primary);
}
.side-sub {
  margin: 4px 0 0;
  font-size: 13.5px;
  color: var(--muted-foreground);
  line-height: 1.5;
}
.side-divider {
  margin: 18px 0;
}
.side-dl {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.side-dl-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}
.side-dl-row dt {
  color: var(--muted-foreground);
  font-weight: 600;
}
.side-dl-row dd {
  margin: 0;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 19px;
  color: var(--color-coral);
  text-align: right;
}

/* Sticky CTA */
.cta-strip {
  position: fixed;
  inset: auto 1rem 1rem 1rem;
  z-index: 40;
  pointer-events: none;
}
@media (min-width: 640px) {
  .cta-strip {
    inset: auto 1.5rem 1.25rem 1.5rem;
  }
}
@media (min-width: 1024px) {
  .cta-strip {
    inset: auto 2rem 1.5rem 2rem;
  }
}
.cta-strip-inner {
  pointer-events: auto;
  margin: 0 auto;
  max-width: 80rem;
  background: var(--primary);
  color: var(--primary-foreground);
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 18px 24px;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
@media (min-width: 900px) {
  .cta-strip-inner {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}
.cta-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.cta-when {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 20px;
  color: var(--color-ochre);
  margin: 0;
}
.cta-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 24px;
  line-height: 1.05;
  letter-spacing: -0.01em;
}
.cta-status {
  margin: 0;
  display: inline-flex;
  align-items: center;
}
.cta-spots {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 19px;
  color: var(--color-ochre);
  line-height: 1;
}
.cta-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}
@media (min-width: 900px) {
  .cta-actions {
    align-items: flex-end;
  }
}
.cta-buttons {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}
.cta-price {
  display: inline-flex;
  align-items: center;
  background: white;
  color: var(--primary);
  border: 2px solid var(--primary-foreground);
  border-radius: 999px;
  padding: 8px 16px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 20px;
}
.cta-primary {
  background: var(--color-coral) !important;
  color: white !important;
  border: 2px solid var(--primary-foreground) !important;
  box-shadow:
    3px 3px 0 var(--color-ochre),
    6px 6px 0 var(--primary-foreground) !important;
}
.cta-primary:hover:not(:disabled) {
  box-shadow:
    5px 5px 0 var(--color-ochre),
    8px 8px 0 var(--primary-foreground) !important;
}
.cta-alert {
  background: white !important;
  color: var(--color-coral) !important;
}

/* not found */
.not-found-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 32px;
  padding: 48px 36px;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
  max-width: 60ch;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.not-found-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(34px, 5vw, 52px);
  line-height: 1;
  color: var(--primary);
}
.not-found-title .hand-text {
  font-size: 1.1em;
}
.not-found-blurb {
  font-size: 16px;
  color: var(--muted-foreground);
  line-height: 1.55;
  margin: 0;
}

/* Skeletons */
.detail-skel-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.skel-chip {
  height: 32px;
  width: 8rem;
  background: color-mix(in srgb, var(--primary) 10%, white);
  border-radius: 999px;
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}
.skel-title {
  height: 64px;
  width: 80%;
  background: color-mix(in srgb, var(--primary) 10%, white);
  border-radius: 24px;
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}
.skel-meta-row {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, 1fr);
}
.skel-meta {
  height: 110px;
  background: color-mix(in srgb, var(--primary) 10%, white);
  border-radius: 22px;
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}
.skel-block {
  height: 220px;
  background: color-mix(in srgb, var(--primary) 10%, white);
  border-radius: 28px;
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}
.skel-photo {
  aspect-ratio: 5 / 4;
  background: color-mix(in srgb, var(--primary) 10%, white);
  border-radius: 28px;
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}
</style>
