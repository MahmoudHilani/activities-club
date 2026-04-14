<script setup lang="ts">
import { CalendarDays, MapPin } from 'lucide-vue-next'
import { computed } from 'vue'
import type { ActivityResponse } from '@/lib/api/types'
import { formatDateStart, formatLocation } from '@/lib/formatters'

const props = defineProps<{
  activity: ActivityResponse
}>()

const scheduleLabel = computed(() => formatDateStart(props.activity.startAt, props.activity.endAt))
const locationLabel = computed(() =>
  formatLocation(props.activity.locationName, props.activity.locationAddress),
)
</script>

<template>
  <article
    class="group flex h-full flex-col overflow-hidden rounded-[2rem] border border-black/5 bg-white shadow-[0_20px_50px_rgba(15,23,42,0.08)] transition duration-300 hover:-translate-y-1 hover:shadow-[0_28px_60px_rgba(15,23,42,0.12)]"
  >
    <div class="relative overflow-hidden">
      <img
        :src="activity.imageUrl"
        :alt="activity.title"
        class="aspect-[1.38/1] w-full object-cover transition duration-500 group-hover:scale-[1.03]"
      />
      <div class="absolute inset-0 bg-gradient-to-t from-black/38 via-transparent to-transparent" />
    </div>

    <div class="flex flex-1 flex-col p-5 sm:p-6">
      <div
        class="flex flex-wrap items-center gap-x-4 gap-y-2 text-[0.78rem] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
      >
        <span class="inline-flex items-center gap-2">
          <CalendarDays class="h-3.5 w-3.5 text-primary" />
          {{ scheduleLabel }}
        </span>
        <span class="inline-flex items-center gap-2">
          <MapPin class="h-3.5 w-3.5 text-primary" />
          {{ locationLabel }}
        </span>
      </div>

      <h3
        class="headline-balance mt-4 line-clamp-2 text-[1.9rem] leading-tight font-extrabold tracking-[-0.03em] text-slate-900"
      >
        {{ activity.title }}
      </h3>

      <p class="mt-4 line-clamp-2 text-sm leading-7 text-slate-700">
        {{ activity.description || 'More details are coming soon for this activity.' }}
      </p>
    </div>
  </article>
</template>
