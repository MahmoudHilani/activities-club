<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { ChevronLeft, ChevronRight, Compass, LoaderCircle } from 'lucide-vue-next'
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
const isInitialLoading = computed(() => activitiesQuery.isPending.value && activities.value.length === 0)
const isError = computed(() => activitiesQuery.isError.value)
const isFetching = computed(() => activitiesQuery.isFetching.value)
const isPending = computed(() => activitiesQuery.isPending.value)
const hasPrevious = computed(() => currentPage.value > 1)
const hasNext = computed(
  () => currentPage.value < totalPages.value && !pageData.value?.last,
)

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
  <section class="flex flex-1 flex-col gap-8">
    <div class="grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-end">
      <div class="space-y-5">
        <p class="inline-flex rounded-full bg-white/80 px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          Public activities
        </p>
        <h1 class="headline-balance max-w-3xl font-serif text-5xl font-bold tracking-tight text-foreground sm:text-6xl">
          Discover what the club is hosting next.
        </h1>
        <p class="max-w-2xl text-lg leading-8 text-muted-foreground">
          Browse published activities, see who is organizing them, and get the timing before you commit.
        </p>
      </div>

      <div class="surface-panel rounded-[1.75rem] border border-white/70 p-6">
        <div class="flex items-start gap-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
            <Compass class="h-5 w-5" />
          </div>
          <div>
            <p class="text-sm font-semibold uppercase tracking-[0.2em] text-muted-foreground">
              Feed summary
            </p>
            <p class="mt-2 text-2xl font-bold tracking-tight text-foreground">
              {{ pageData?.totalElements ?? 0 }} published public activities
            </p>
            <p class="mt-2 text-sm leading-7 text-muted-foreground">
              Results are loaded directly from <code>/api/activities/public</code> with backend pagination.
            </p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isInitialLoading" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <div
        v-for="placeholder in 6"
        :key="placeholder"
        class="surface-panel animate-pulse rounded-[1.75rem] border border-white/70 p-6"
      >
        <div class="h-5 w-24 rounded-full bg-muted" />
        <div class="mt-5 h-8 w-3/4 rounded-full bg-muted" />
        <div class="mt-4 h-4 w-full rounded-full bg-muted" />
        <div class="mt-2 h-4 w-5/6 rounded-full bg-muted" />
        <div class="mt-8 h-4 w-1/2 rounded-full bg-muted" />
      </div>
    </div>

    <Alert
      v-else-if="isError && activities.length === 0"
      class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between"
      variant="destructive"
    >
      <span>{{ mapActivitiesError(activitiesQuery.error) }}</span>
      <Button size="sm" variant="outline" @click="activitiesQuery.refetch()">
        Try again
      </Button>
    </Alert>

    <div
      v-else-if="activities.length === 0"
      class="surface-panel rounded-[1.75rem] border border-white/70 px-6 py-10 text-center"
    >
      <p class="text-lg font-semibold text-foreground">No public activities are published yet.</p>
      <p class="mt-2 text-sm text-muted-foreground">
        When organizers publish events, they will appear here automatically.
      </p>
    </div>

    <template v-else>
      <div class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <ActivityCard
          v-for="activity in activities"
          :key="activity.id"
          :activity="activity"
        />
      </div>

      <div class="flex flex-col gap-4 rounded-[1.5rem] border border-white/60 bg-white/50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-3 text-sm text-muted-foreground">
          <LoaderCircle
            v-if="isFetching && !isPending"
            class="h-4 w-4 animate-spin"
          />
          <span>Page {{ currentPage }} of {{ totalPages }}</span>
        </div>

        <div class="flex items-center gap-3">
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
      </div>
    </template>
  </section>
</template>
