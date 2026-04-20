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
  <section class="flex flex-1 flex-col gap-8">
    <div v-if="isInitialLoading" class="grid gap-6 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
      <div
        v-for="placeholder in 8"
        :key="placeholder"
        class="overflow-hidden rounded-[2rem] border border-white/70 bg-white/80 animate-pulse shadow-[0_18px_40px_rgba(15,23,42,0.06)]"
      >
        <div class="aspect-[1.38/1] w-full bg-muted" />
        <div class="space-y-4 p-6">
          <div class="h-3 w-2/3 rounded-full bg-muted" />
          <div class="h-8 w-5/6 rounded-2xl bg-muted" />
          <div class="h-4 w-3/5 rounded-full bg-muted" />
          <div class="h-4 w-full rounded-full bg-muted" />
          <div class="h-4 w-4/5 rounded-full bg-muted" />
          <div class="h-10 w-32 rounded-full bg-muted" />
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

    <div
      v-else-if="activities.length === 0"
      class="rounded-[2rem] border border-white/70 bg-white/80 px-6 py-12 text-center shadow-[0_20px_50px_rgba(15,23,42,0.08)]"
    >
      <p class="text-lg font-semibold text-foreground">No public activities are published yet.</p>
      <p class="mt-2 text-sm text-muted-foreground">
        When organizers publish events, they will appear here automatically.
      </p>
    </div>

    <template v-else>
      <div class="grid gap-6 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
        <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
      </div>

      <div
        v-if="totalPages > 1"
        class="flex flex-col gap-4 rounded-[1.5rem] border border-white/70 bg-white/70 px-5 py-4 shadow-[0_16px_35px_rgba(15,23,42,0.05)] sm:flex-row sm:items-center sm:justify-between"
      >
        <div class="flex items-center gap-3 text-sm text-muted-foreground">
          <LoaderCircle v-if="isFetching && !isPending" class="h-4 w-4 animate-spin" />
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
