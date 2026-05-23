<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { ChevronLeft, ChevronRight, LoaderCircle } from 'lucide-vue-next'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import ActivityCard from '@/components/ActivityCard.vue'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { getPublicActivities } from '@/lib/api/activities'
import { mapActivitiesError } from '@/lib/api/errors'

const PAGE_SIZE = 12

const route = useRoute()
const router = useRouter()

function parsePage(value: unknown): number {
  const parsed = Number(Array.isArray(value) ? value[0] : value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 1
}

const currentPage = computed(() => parsePage(route.query.page))

const activitiesQuery = useQuery(() => ({
  queryKey: ['public-activities', currentPage.value],
  queryFn: () =>
    getPublicActivities({
      page: currentPage.value - 1,
      size: PAGE_SIZE,
    }),
  placeholderData: (previousData) => previousData,
}))

const pageData = computed(() => activitiesQuery.data.value)
const activities = computed(() => pageData.value?.content ?? [])
const totalPages = computed(() => Math.max(pageData.value?.totalPages ?? 1, 1))
const isInitialLoading = computed(
  () => activitiesQuery.isPending.value && activities.value.length === 0,
)
const isError = computed(() => activitiesQuery.isError.value)
const isFetching = computed(() => activitiesQuery.isFetching.value)
const isPending = computed(() => activitiesQuery.isPending.value)
const hasPrevious = computed(() => currentPage.value > 1)
const hasNext = computed(() => currentPage.value < totalPages.value && !pageData.value?.last)

async function setPage(page: number): Promise<void> {
  if (page < 1 || page > totalPages.value) {
    return
  }

  await router.replace({
    name: 'activities',
    query: page === 1 ? {} : { page: String(page) },
  })
}
</script>

<template>
  <section class="page-shell">
    <header class="page-hero">
      <h1 class="page-title">
        <span class="display-text">Everything</span>
        <span class="hand-text"> on this term</span>
      </h1>
    </header>

    <div v-if="isInitialLoading" class="activity-grid">
      <div v-for="placeholder in 8" :key="placeholder" class="skeleton-card">
        <div class="skeleton-photo" />
        <div class="skeleton-body">
          <div class="skeleton-line skeleton-line-md" />
          <div class="skeleton-line skeleton-line-lg" />
          <div class="skeleton-line skeleton-line-sm" />
          <div class="skeleton-line skeleton-line-full" />
        </div>
      </div>
    </div>

    <Alert
      v-else-if="isError && activities.length === 0"
      class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between"
      variant="destructive"
    >
      <span>{{ mapActivitiesError(activitiesQuery.error) }}</span>
      <Button size="sm" variant="outline" @click="activitiesQuery.refetch()"> Try again </Button>
    </Alert>

    <div v-else-if="activities.length === 0" class="empty-state">
      <div class="empty-blob" aria-hidden="true" />
      <p class="empty-title">No activities published yet</p>
      <p class="empty-sub">
        When organisers publish an activity, it'll land here. Check back soon — or follow us on
        the WhatsApp group.
      </p>
    </div>

    <template v-else>
      <div class="activity-grid">
        <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
      </div>

      <nav v-if="totalPages > 1" class="pager" aria-label="Activities pagination">
        <div class="pager-meta">
          <LoaderCircle v-if="isFetching && !isPending" class="h-4 w-4 animate-spin" />
          <span class="pager-text">
            page <strong>{{ currentPage }}</strong> of {{ totalPages }}
          </span>
        </div>

        <div class="pager-controls">
          <Button
            :disabled="!hasPrevious || isFetching"
            size="sm"
            variant="outline"
            @click="setPage(currentPage - 1)"
          >
            <ChevronLeft class="h-4 w-4" />
            Previous
          </Button>
          <Button
            :disabled="!hasNext || isFetching"
            size="sm"
            variant="outline"
            @click="setPage(currentPage + 1)"
          >
            Next
            <ChevronRight class="h-4 w-4" />
          </Button>
        </div>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.page-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 2.5rem;
}

.page-hero {
  position: relative;
  text-align: left;
  padding-bottom: 0.5rem;
}
.page-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(28px, 5vw, 64px);
  line-height: 0.98;
  color: var(--primary);
  white-space: nowrap;
}
.page-title .hand-text {
  font-size: 1.12em;
}

.activity-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 28px;
}
@media (min-width: 768px) {
  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (min-width: 1280px) {
  .activity-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (min-width: 1600px) {
  .activity-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* Skeleton */
.skeleton-card {
  background: white;
  border: 2px solid color-mix(in srgb, var(--primary) 18%, white);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 4px 4px 0 color-mix(in srgb, var(--primary) 14%, white);
}
.skeleton-photo {
  aspect-ratio: 4 / 3;
  background: color-mix(in srgb, var(--primary) 8%, white);
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}
.skeleton-body {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.skeleton-line {
  height: 14px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary) 10%, white);
  animation: skeleton-pulse 1.6s ease-in-out infinite;
}
.skeleton-line-md {
  width: 60%;
}
.skeleton-line-lg {
  width: 80%;
  height: 26px;
}
.skeleton-line-sm {
  width: 40%;
}
.skeleton-line-full {
  width: 100%;
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

.empty-state {
  position: relative;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 32px;
  padding: 56px 32px;
  text-align: center;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
  overflow: hidden;
}
.empty-blob {
  position: absolute;
  inset: auto auto -20px -30px;
  width: 220px;
  height: 220px;
  background: var(--color-ochre);
  opacity: 0.2;
  clip-path: polygon(22% 8%, 78% 5%, 92% 28%, 95% 72%, 84% 92%, 28% 95%, 8% 78%, 5% 32%);
}
.empty-title {
  position: relative;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 32px;
  color: var(--primary);
  margin: 0 0 12px;
}
.empty-sub {
  position: relative;
  max-width: 48ch;
  margin: 0 auto;
  font-size: 16px;
  color: var(--muted-foreground);
  line-height: 1.55;
}

.pager {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: center;
  justify-content: space-between;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 999px;
  padding: 14px 22px;
  box-shadow: 4px 4px 0 var(--color-coral);
}
@media (min-width: 640px) {
  .pager {
    flex-direction: row;
  }
}
.pager-meta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--muted-foreground);
}
.pager-text {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 20px;
  color: var(--primary);
}
.pager-text strong {
  color: var(--color-coral);
}
.pager-controls {
  display: inline-flex;
  gap: 10px;
}
</style>
