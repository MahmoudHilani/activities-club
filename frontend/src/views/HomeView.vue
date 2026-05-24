<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { Search } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import ActivityCard from '@/components/ActivityCard.vue'
import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { getPublicActivities } from '@/lib/api/activities'
import { mapActivitiesError } from '@/lib/api/errors'

const HOME_ACTIVITY_COUNT = 3

const router = useRouter()
const route = useRoute()
const query = ref('')

const signupToast = ref(false)
const showSignupToast = computed(() => signupToast.value)

onMounted(() => {
  if (route.query.signup === 'success') {
    signupToast.value = true
    router.replace({ name: 'home', query: {} })
    window.setTimeout(() => {
      signupToast.value = false
    }, 6000)
  }
})

function submitSearch(): void {
  router.push({
    name: 'activities',
    query: query.value.trim() ? { q: query.value.trim() } : {},
  })
}

const activitiesQuery = useQuery(() => ({
  queryKey: ['public-activities', 'home'],
  queryFn: () => getPublicActivities({ page: 0, size: HOME_ACTIVITY_COUNT }),
}))

const activities = computed(() => activitiesQuery.data.value?.content ?? [])
const totalActivities = computed(() => activitiesQuery.data.value?.totalElements ?? 0)
const isInitialLoading = computed(
  () => activitiesQuery.isPending.value && activities.value.length === 0,
)
const isActivitiesError = computed(() => activitiesQuery.isError.value)
const activitiesError = computed(() => activitiesQuery.error.value)
const activityCountLabel = computed(() => {
  if (isInitialLoading.value) {
    return 'Finding activities...'
  }

  if (isActivitiesError.value) {
    return 'Find an activity to try'
  }

  return `${totalActivities.value} ${totalActivities.value === 1 ? 'activity' : 'activities'} to explore`
})
</script>

<template>
  <div class="home">
    <div class="home-bg" aria-hidden="true" />

    <section class="hero">
      <div class="hero-grid">
        <div class="cutout cutout-left" aria-hidden="true">
          <div class="cutout-blob cutout-blob-coral" />
          <div
            class="cutout-photo"
            :style="{
              backgroundImage: 'url(https://picsum.photos/seed/group-hike-friends/500/640)',
            }"
          />
          <div class="cutout-sticker cutout-sticker-coral">hike club ✿</div>
          <div class="cutout-flourish cutout-flourish-bottom">♪</div>
        </div>

        <div class="hero-center">
          <div class="hero-eyebrow"><span class="hero-eyebrow-dot" /> {{ activityCountLabel }}</div>

          <h1 class="hero-headline">
            <span class="ink">Find</span> your <span class="scribble">people</span>,<br />
            <span class="ink">find</span> your <span class="leaf">hill ⛰</span>.
          </h1>

          <p class="hero-deck">
            Hiking, climbing, sea-swimming, slow yoga and the very occasional 5k. Run by students,
            open to everyone at Griffith — no fitness required, snacks usually involved.
          </p>

          <div class="hero-cta">
            <RouterLink :to="{ name: 'activities' }" class="hero-cta-btn">
              See all activities
            </RouterLink>
            <span class="hero-cta-note">
              <svg class="hero-cta-arrow" aria-hidden="true" viewBox="0 0 54 28" fill="none">
                <path
                  d="M51 8.5C41.5 21 21.5 24.5 6 15"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                />
                <path
                  d="M15 14.5L5.5 15L10 23"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              …or just have a wander
            </span>
          </div>
        </div>

        <div class="cutout cutout-right" aria-hidden="true">
          <div class="cutout-blob cutout-blob-ochre" />
          <div
            class="cutout-photo"
            :style="{
              backgroundImage: 'url(https://picsum.photos/seed/yoga-mat-warm-light/500/640)',
            }"
          />
          <div class="cutout-sticker cutout-sticker-ochre">every tue 18:30</div>
          <div class="cutout-flourish cutout-flourish-top">✦</div>
        </div>
      </div>

      <form class="search" @submit.prevent="submitSearch">
        <span class="search-floater-emoji" aria-hidden="true">👀</span>
        <span class="search-floater-label">looking for something?</span>
        <div class="search-icon"><Search class="h-5 w-5" /></div>
        <input
          v-model="query"
          type="text"
          aria-label="Search activities"
        />
        <button type="submit" class="search-button">Search</button>
      </form>
    </section>

    <section class="happenings">
      <div class="section-title">
        <h2>Activities <em>to explore</em></h2>
        <RouterLink :to="{ name: 'activities' }" class="see-all">Browse everything →</RouterLink>
      </div>

      <div
        v-if="isInitialLoading"
        class="happen-grid"
        aria-label="Loading activities"
        aria-busy="true"
      >
        <div
          v-for="placeholder in HOME_ACTIVITY_COUNT"
          :key="placeholder"
          class="happen-skeleton"
        />
      </div>

      <Alert
        v-else-if="isActivitiesError && activities.length === 0"
        class="happen-alert"
        variant="destructive"
      >
        <span>{{ mapActivitiesError(activitiesError) }}</span>
        <Button size="sm" variant="outline" @click="activitiesQuery.refetch()">Try again</Button>
      </Alert>

      <div v-else-if="activities.length === 0" class="happen-empty" role="status">
        No public activities are published yet.
      </div>

      <div v-else class="happen-grid">
        <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
      </div>
    </section>

    <section class="cta-strip">
      <div class="cta-strip-inner">
        <div>
          <h3>
            Join a <em>sports club</em><br />
            at the college.
          </h3>
          <RouterLink :to="{ name: 'sports-club-signup' }" class="cta-strip-btn">
            Sign up →
          </RouterLink>
        </div>
        <div class="cta-strip-right">
          <span class="cta-sport-chip cta-sport-chip-coral" aria-label="Football">
            <svg
              class="cta-sport-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7l3.5 2.5L14 13.5h-4L8.5 9.5z" />
              <path d="M12 7V3" />
              <path d="M15.5 9.5l4-1" />
              <path d="M14 13.5l2.5 3.5" />
              <path d="M10 13.5l-2.5 3.5" />
              <path d="M8.5 9.5l-4-1" />
            </svg>
          </span>
          <span class="cta-sport-chip cta-sport-chip-ochre" aria-label="Basketball">
            <svg
              class="cta-sport-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="9" />
              <path d="M12 3v18" />
              <path d="M3 12h18" />
              <path d="M5.5 5.5Q12 12 5.5 18.5" />
              <path d="M18.5 5.5Q12 12 18.5 18.5" />
            </svg>
          </span>
          <span class="cta-sport-chip cta-sport-chip-leaf" aria-label="Badminton">
            <svg
              class="cta-sport-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M4 6L9.5 17h5L20 6" />
              <path d="M4 6Q12 8 20 6" />
              <path d="M8 6l2 11" />
              <path d="M16 6l-2 11" />
              <ellipse cx="12" cy="19" rx="3" ry="1.6" />
            </svg>
          </span>
          <span class="cta-sport-chip cta-sport-chip-coral" aria-label="Volleyball">
            <svg
              class="cta-sport-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="9" />
              <path d="M12 3C7 8 7 16 12 21" />
              <path d="M3 12C8 7 16 7 21 12" />
              <path d="M3 12C8 17 16 17 21 12" />
            </svg>
          </span>
        </div>
      </div>
    </section>

    <Transition name="signup-toast">
      <div v-if="showSignupToast" class="signup-toast" role="status" aria-live="polite">
        <span class="signup-toast-tag">Thanks!</span>
        <span class="signup-toast-message">
          Your sports club signup is in. We'll be in touch.
        </span>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.home {
  position: relative;
  margin: -2rem -1rem 0;
}

@media (min-width: 640px) {
  .home {
    margin-left: -1.5rem;
    margin-right: -1.5rem;
  }
}
@media (min-width: 1024px) {
  .home {
    margin-left: -2rem;
    margin-right: -2rem;
    margin-top: -2.5rem;
  }
}

.home-bg {
  position: absolute;
  top: -10rem;
  bottom: 0;
  left: 50%;
  width: 100vw;
  transform: translateX(-50%);
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(680px 480px at 8% 6%, rgba(255, 122, 89, 0.16), transparent 60%),
    radial-gradient(760px 560px at 92% 10%, rgba(245, 176, 66, 0.16), transparent 60%),
    radial-gradient(840px 480px at 50% calc(100% - 320px), rgba(91, 165, 114, 0.12), transparent 60%);
}

.hero,
.happenings,
.cta-strip {
  position: relative;
  z-index: 1;
  padding-left: 1.5rem;
  padding-right: 1.5rem;
  max-width: 80rem;
  margin: 0 auto;
}
@media (min-width: 1024px) {
  .hero,
  .happenings,
  .cta-strip {
    padding-left: 2rem;
    padding-right: 2rem;
  }
}

.hero {
  padding-top: 3rem;
  padding-bottom: 4rem;
}

.hero-grid {
  display: grid;
  grid-template-columns: 1fr;
  align-items: center;
  gap: 2rem;
}
@media (min-width: 1024px) {
  .hero-grid {
    grid-template-columns: 280px 1fr 280px;
    gap: 2rem;
  }
}

/* Cutouts */
.cutout {
  position: relative;
  max-width: 280px;
  margin: 0 auto;
  width: 100%;
}
.cutout-blob {
  position: absolute;
  inset: -14px;
  z-index: 0;
  opacity: 0.55;
  clip-path: polygon(22% 8%, 78% 5%, 92% 28%, 95% 72%, 84% 92%, 28% 95%, 8% 78%, 5% 32%);
}
.cutout-blob-coral {
  background: var(--color-coral);
}
.cutout-blob-ochre {
  background: var(--color-ochre);
  clip-path: polygon(20% 6%, 80% 4%, 95% 28%, 97% 76%, 82% 94%, 22% 96%, 6% 80%, 3% 30%);
}
.cutout-photo {
  position: relative;
  z-index: 1;
  width: 100%;
  aspect-ratio: 4 / 5;
  background-size: cover;
  background-position: center;
  clip-path: polygon(18% 4%, 82% 1%, 96% 22%, 99% 76%, 88% 96%, 24% 99%, 4% 82%, 1% 28%);
}
.cutout-sticker {
  position: absolute;
  z-index: 2;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 18px;
  padding: 4px 14px;
  border-radius: 999px;
  box-shadow: 2px 2px 0 var(--background);
  transform: rotate(-6deg);
}
.cutout-sticker-coral {
  background: var(--color-coral);
  color: white;
  bottom: 18px;
  left: -10px;
}
.cutout-sticker-ochre {
  background: var(--color-ochre);
  color: var(--primary);
  bottom: 28px;
  right: -10px;
  transform: rotate(6deg);
}
.cutout-flourish {
  position: absolute;
  z-index: 2;
  font-family: var(--font-display);
  font-size: 36px;
  color: var(--color-leaf);
}
.cutout-flourish-bottom {
  bottom: -10px;
  right: -6px;
  transform: rotate(15deg);
}
.cutout-flourish-top {
  top: -10px;
  left: -6px;
  transform: rotate(-15deg);
}

/* Hero center */
.hero-center {
  text-align: center;
}
.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: color-mix(in srgb, white 78%, #f4efe4 22%);
  color: var(--primary);
  padding: 8px 18px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.04em;
  border: 1.5px solid var(--primary);
  box-shadow: 3px 3px 0 var(--primary);
  margin-bottom: 28px;
  transform: rotate(-1.5deg);
}
.hero-eyebrow-dot {
  width: 8px;
  height: 8px;
  background: var(--color-coral);
  border-radius: 50%;
  animation: home-pulse 1.4s ease-in-out infinite;
}
@keyframes home-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.4);
    opacity: 0.6;
  }
}

.hero-headline {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(44px, 6.5vw, 88px);
  line-height: 0.96;
  margin: 0 auto 12px;
  max-width: 800px;
  color: var(--primary);
}
.hero-headline .ink {
  color: var(--primary);
}
.hero-headline .scribble {
  position: relative;
  display: inline-block;
  font-family: var(--font-hand);
  font-weight: 700;
  color: var(--color-coral);
  font-size: 1.15em;
  line-height: 0.9;
  padding: 0 6px;
}
.hero-headline .scribble::after {
  content: '';
  position: absolute;
  left: -2px;
  right: -2px;
  bottom: 4px;
  height: 8px;
  background: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 8' width='100' height='8'><path d='M2 5 Q 16 1, 32 5 T 64 5 T 98 5' stroke='%23ff7a59' stroke-width='2.5' fill='none' stroke-linecap='round'/></svg>")
    repeat-x;
}
.hero-headline .leaf {
  color: var(--color-leaf);
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 1.1em;
}

.hero-deck {
  margin: 20px auto 32px;
  max-width: 540px;
  font-size: 18px;
  line-height: 1.55;
  color: var(--muted-foreground);
}

.hero-cta {
  display: inline-flex;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
}
.hero-cta-btn {
  background: var(--primary);
  color: var(--primary-foreground);
  font-weight: 700;
  font-size: 15px;
  padding: 14px 26px;
  border-radius: 999px;
  text-decoration: none;
  display: inline-block;
  line-height: 1.2;
  box-shadow:
    3px 3px 0 var(--color-coral),
    6px 6px 0 var(--color-ochre);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.hero-cta-btn:hover {
  transform: translate(-2px, -2px);
  box-shadow:
    5px 5px 0 var(--color-coral),
    8px 8px 0 var(--color-ochre);
}
.hero-cta-note {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 22px;
  color: var(--primary);
}
.hero-cta-arrow {
  width: 46px;
  height: 24px;
  flex: 0 0 auto;
  transform: translateY(5px) rotate(-8deg);
}

/* Search */
.search {
  position: relative;
  max-width: 720px;
  margin: 56px auto 0;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 999px;
  padding: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
}
.search-floater-emoji {
  position: absolute;
  top: -34px;
  left: 32px;
  font-size: 28px;
  transform: rotate(-8deg);
}
.search-floater-label {
  position: absolute;
  top: -22px;
  left: 70px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 20px;
  color: var(--primary);
}
.search-icon {
  width: 50px;
  height: 50px;
  background: color-mix(in srgb, white 78%, #f4efe4 22%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  flex-shrink: 0;
}
.search input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  padding: 10px 14px;
  font-family: var(--font-sans);
  font-size: 17px;
  color: var(--primary);
  outline: 0;
}
.search input::placeholder {
  color: var(--muted-foreground);
}
.search-button {
  background: var(--primary);
  color: var(--primary-foreground);
  border: 0;
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 15px;
  padding: 14px 26px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}
.search-button:hover {
  background: var(--color-coral);
}

/* Happenings */
.happenings {
  padding-top: 64px;
  padding-bottom: 48px;
}
.section-title {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 36px;
  flex-wrap: wrap;
}
.section-title h2 {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(32px, 4.5vw, 52px);
  line-height: 1;
  color: var(--primary);
}
.section-title h2 em {
  font-family: var(--font-hand);
  font-weight: 700;
  color: var(--color-coral);
  font-style: normal;
  font-size: 1.05em;
}
.see-all {
  color: var(--primary);
  text-decoration: none;
  font-weight: 700;
  border-bottom: 2px solid var(--primary);
  padding-bottom: 2px;
}
.see-all:hover {
  color: var(--color-coral);
  border-color: var(--color-coral);
}

.happen-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 28px;
}
@media (min-width: 768px) {
  .happen-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (min-width: 1024px) {
  .happen-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.happen-skeleton {
  min-height: 420px;
  border-radius: 28px;
  border: 2px solid color-mix(in srgb, var(--primary) 18%, white);
  background: color-mix(in srgb, var(--primary) 8%, white);
  animation: happen-pulse 1.6s ease-in-out infinite;
}
@keyframes happen-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}
.happen-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.happen-empty {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  box-shadow: 4px 4px 0 var(--color-ochre);
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 24px;
  padding: 44px 24px;
  text-align: center;
}

/* CTA strip */
.cta-strip {
  position: relative;
  margin: 24px auto 80px;
  padding: 56px 40px;
  background: var(--primary);
  color: var(--primary-foreground);
  border-radius: 36px;
  overflow: hidden;
}
.cta-strip::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(400px 300px at 12% 30%, rgba(255, 122, 89, 0.4), transparent 60%),
    radial-gradient(400px 300px at 90% 70%, rgba(245, 176, 66, 0.35), transparent 60%);
}
.cta-strip-inner {
  position: relative;
  display: grid;
  grid-template-columns: 1fr;
  gap: 32px;
  align-items: center;
}
@media (min-width: 900px) {
  .cta-strip-inner {
    grid-template-columns: 1.4fr 1fr;
    gap: 40px;
  }
}
.cta-strip h3 {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(32px, 4.5vw, 56px);
  line-height: 1;
}
.cta-strip h3 em {
  font-family: var(--font-hand);
  font-weight: 700;
  color: var(--color-ochre);
  font-style: normal;
  font-size: 1.1em;
}
.cta-strip p {
  margin: 0 0 22px;
  font-size: 17px;
  opacity: 0.85;
  max-width: 50ch;
}
.cta-strip-btn {
  display: inline-block;
  background: var(--color-coral);
  color: white;
  padding: 16px 28px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 15px;
  text-decoration: none;
  box-shadow: 4px 4px 0 var(--primary-foreground);
  transition: transform 0.15s;
}
.cta-strip-btn:hover {
  transform: translate(-2px, -2px);
}
.cta-strip-right {
  position: relative;
  width: 100%;
  min-height: 280px;
}
.cta-sport-chip {
  position: absolute;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 5px 5px 0 var(--primary-foreground);
}
.cta-sport-icon {
  width: 60%;
  height: 60%;
}
.cta-sport-chip-coral {
  background: var(--color-coral);
}
.cta-sport-chip-ochre {
  background: var(--color-ochre);
  color: var(--primary);
}
.cta-sport-chip-leaf {
  background: var(--color-leaf);
}
.cta-sport-chip:nth-child(1) {
  width: 124px;
  height: 124px;
  top: 4px;
  left: 6%;
  transform: rotate(-9deg);
}
.cta-sport-chip:nth-child(2) {
  width: 108px;
  height: 108px;
  top: 32px;
  right: 16%;
  transform: rotate(7deg);
}
.cta-sport-chip:nth-child(3) {
  width: 116px;
  height: 116px;
  bottom: 8px;
  left: 26%;
  transform: rotate(-4deg);
}
.cta-sport-chip:nth-child(4) {
  width: 100px;
  height: 100px;
  bottom: 24px;
  right: 4%;
  transform: rotate(12deg);
}
@media (max-width: 540px) {
  .cta-strip-right {
    min-height: 220px;
  }
  .cta-sport-chip:nth-child(1) { width: 96px; height: 96px; }
  .cta-sport-chip:nth-child(2) { width: 84px; height: 84px; }
  .cta-sport-chip:nth-child(3) { width: 92px; height: 92px; }
  .cta-sport-chip:nth-child(4) { width: 80px; height: 80px; }
}

@media (max-width: 900px) {
  .cutout {
    max-width: 260px;
  }
  .hero {
    padding-top: 1.5rem;
  }
}

.signup-toast {
  position: fixed;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  z-index: 60;
  background: var(--primary);
  color: var(--primary-foreground);
  border-radius: 999px;
  padding: 14px 22px;
  box-shadow: 4px 4px 0 var(--color-leaf);
  display: flex;
  align-items: center;
  gap: 12px;
  max-width: calc(100vw - 32px);
}
.signup-toast-tag {
  background: var(--color-coral);
  color: white;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 17px;
  padding: 4px 12px;
  border-radius: 999px;
  transform: rotate(-3deg);
}
.signup-toast-message {
  font-weight: 600;
  font-size: 15px;
}
.signup-toast-enter-active,
.signup-toast-leave-active {
  transition:
    opacity 0.25s,
    transform 0.25s;
}
.signup-toast-enter-from,
.signup-toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 16px);
}
</style>
