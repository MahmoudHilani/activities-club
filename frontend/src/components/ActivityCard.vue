<script setup lang="ts">
import { CalendarDays, MapPin } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import type { ActivityResponse } from '@/lib/api/types'
import { formatDateStart, formatLocation } from '@/lib/formatters'

const LOCATION_LABEL_LIMIT = 42

const props = defineProps<{
  activity: ActivityResponse
}>()

const scheduleLabel = computed(() => formatDateStart(props.activity.startAt, props.activity.endAt))
const fullLocationLabel = computed(() =>
  formatLocation(props.activity.locationName, props.activity.locationAddress),
)
const locationLabel = computed(() => {
  if (fullLocationLabel.value.length <= LOCATION_LABEL_LIMIT) {
    return fullLocationLabel.value
  }

  return `${fullLocationLabel.value.slice(0, LOCATION_LABEL_LIMIT).trimEnd()}...`
})

const accentTone = computed<'coral' | 'ochre' | 'leaf'>(() => {
  const tones: ['coral', 'ochre', 'leaf'] = ['coral', 'ochre', 'leaf']
  return tones[((props.activity.id % tones.length) + tones.length) % tones.length] as
    | 'coral'
    | 'ochre'
    | 'leaf'
})

</script>

<template>
  <RouterLink
    :to="{ name: 'activity-detail', params: { activityId: activity.id } }"
    class="group block h-full focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-ring/80 focus-visible:ring-offset-4 focus-visible:ring-offset-background"
  >
    <article class="activity-card" :class="`activity-card-${accentTone}`">
      <div class="card-photo-wrap">
        <img :src="activity.imageUrl" :alt="activity.title" class="card-photo" />
        <span v-if="activity.isOvernight" class="craft-tag" :class="`craft-tag-${accentTone}`">
          <svg class="tag-icon" viewBox="0 0 16 16" aria-hidden="true">
            <path
              fill="currentColor"
              d="M10.7 1.2a6.8 6.8 0 1 0 4.05 10.6A5.15 5.15 0 1 1 10.7 1.2Z"
            />
          </svg>
          overnight
        </span>
      </div>

      <div class="card-body">
        <h3 class="card-title">{{ activity.title }}</h3>
        <div class="card-when">
          <CalendarDays class="h-4 w-4" />
          <span>{{ scheduleLabel }}</span>
        </div>
        <p class="card-blurb">
          {{ activity.description || 'More details coming soon — keep an eye out!' }}
        </p>

        <div class="card-foot">
          <div class="card-place" :title="fullLocationLabel">
            <MapPin class="h-4 w-4" />
            <span>{{ locationLabel }}</span>
          </div>
          <span class="card-arrow" aria-hidden="true">→</span>
        </div>
      </div>
    </article>
  </RouterLink>
</template>

<style scoped>
.activity-card {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  overflow: hidden;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
  box-shadow:
    4px 4px 0 var(--color-coral),
    8px 8px 0 var(--primary);
}
.activity-card-ochre {
  box-shadow:
    4px 4px 0 var(--color-ochre),
    8px 8px 0 var(--primary);
}
.activity-card-leaf {
  box-shadow:
    4px 4px 0 var(--color-leaf),
    8px 8px 0 var(--primary);
}
.group:hover .activity-card,
.group:focus-visible .activity-card {
  transform: translate(-3px, -3px);
  box-shadow:
    6px 6px 0 var(--color-coral),
    10px 10px 0 var(--primary);
}
.group:hover .activity-card-ochre,
.group:focus-visible .activity-card-ochre {
  box-shadow:
    6px 6px 0 var(--color-ochre),
    10px 10px 0 var(--primary);
}
.group:hover .activity-card-leaf,
.group:focus-visible .activity-card-leaf {
  box-shadow:
    6px 6px 0 var(--color-leaf),
    10px 10px 0 var(--primary);
}

.card-photo-wrap {
  position: relative;
  overflow: hidden;
  border-bottom: 2px solid var(--primary);
}
.card-photo {
  display: block;
  aspect-ratio: 4 / 3;
  width: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}
.group:hover .card-photo {
  transform: scale(1.04);
}
.craft-tag {
  position: absolute;
  top: 14px;
  left: 14px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  box-shadow: 2px 2px 0 var(--primary);
}
.tag-icon {
  width: 15px;
  height: 15px;
  flex: none;
}

.card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 22px 22px 20px;
  gap: 10px;
}
.card-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 28px;
  line-height: 1.04;
  color: var(--primary);
  letter-spacing: -0.01em;
}
.card-when {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 19px;
  color: var(--color-coral);
}
.card-blurb {
  margin: 4px 0 0;
  font-size: 14.5px;
  color: var(--muted-foreground);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-foot {
  margin-top: auto;
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-top: 1.5px dashed color-mix(in srgb, var(--primary) 25%, transparent);
}
.card-place {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  min-width: 0;
}
.card-place span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-arrow {
  font-family: var(--font-display);
  font-size: 26px;
  color: var(--color-coral);
  line-height: 1;
  transition: transform 0.2s ease;
}
.group:hover .card-arrow {
  transform: translateX(3px);
}
</style>
