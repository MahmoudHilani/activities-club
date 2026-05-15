<script setup lang="ts">
import { Search } from 'lucide-vue-next'
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

type DemoActivity = {
  id: string
  title: string
  when: string
  category: { label: string; tone: 'coral' | 'ochre' | 'leaf' }
  photo: string
  blurb: string
  going: number
  avatars: string[]
  price: string
  isFree?: boolean
}

const router = useRouter()
const query = ref('')

function submitSearch(): void {
  router.push({
    name: 'activities',
    query: query.value.trim() ? { q: query.value.trim() } : {},
  })
}

const categories: Array<{ emoji: string; label: string; tone?: 'coral' | 'ochre' | 'leaf' }> = [
  { emoji: '⛰', label: 'Hiking', tone: 'coral' },
  { emoji: '🧗', label: 'Climbing', tone: 'ochre' },
  { emoji: '🌊', label: 'Wild swim', tone: 'leaf' },
  { emoji: '🧘', label: 'Yoga' },
  { emoji: '🏃', label: 'Running' },
  { emoji: '🚲', label: 'Cycling' },
  { emoji: '🛶', label: 'Kayak' },
  { emoji: '✨', label: 'Show all 47' },
]

const activities: DemoActivity[] = [
  {
    id: 'spinc',
    title: 'Sunrise Spinc loop',
    when: 'sat 14 mar · 06:00 bus',
    category: { label: '⛰ adventure', tone: 'coral' },
    photo: 'https://picsum.photos/seed/spinc-friendly/700/520',
    blurb:
      "Six glorious hours above Glendalough's upper lake. We bring the bus, you bring the boots and breakfast bar.",
    going: 23,
    avatars: [
      'https://i.pravatar.cc/56?img=11',
      'https://i.pravatar.cc/56?img=32',
      'https://i.pravatar.cc/56?img=47',
    ],
    price: '€12',
  },
  {
    id: 'yoga',
    title: 'Slow yoga with Niamh',
    when: 'every tue · 18:30',
    category: { label: '✿ wellness', tone: 'ochre' },
    photo: 'https://picsum.photos/seed/yoga-friendly/700/520',
    blurb:
      'Restorative practice in the South Circular studio. Bolsters and blankets supplied, no pretzel poses required.',
    going: 14,
    avatars: [
      'https://i.pravatar.cc/56?img=15',
      'https://i.pravatar.cc/56?img=23',
      'https://i.pravatar.cc/56?img=5',
    ],
    price: 'free',
    isFree: true,
  },
  {
    id: 'kayak',
    title: 'Sea-kayak around Howth',
    when: 'sun 22 mar · 09:30',
    category: { label: '🌊 adventure', tone: 'leaf' },
    photo: 'https://picsum.photos/seed/sea-kayak-friendly/700/520',
    blurb:
      'Half-day paddle around the cliffs. All gear included, plus a steaming hot chocolate when we land.',
    going: 9,
    avatars: [
      'https://i.pravatar.cc/56?img=20',
      'https://i.pravatar.cc/56?img=44',
      'https://i.pravatar.cc/56?img=8',
    ],
    price: '€28',
  },
]
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
              backgroundImage:
                'url(https://picsum.photos/seed/group-hike-friends/500/640)',
            }"
          />
          <div class="cutout-sticker cutout-sticker-coral">hike club ✿</div>
          <div class="cutout-flourish cutout-flourish-bottom">♪</div>
        </div>

        <div class="hero-center">
          <div class="hero-eyebrow">
            <span class="hero-eyebrow-dot" /> 47 things happening this week
          </div>

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
              See what's on this week →
            </RouterLink>
            <span class="hero-cta-note">…or just have a wander</span>
          </div>
        </div>

        <div class="cutout cutout-right" aria-hidden="true">
          <div class="cutout-blob cutout-blob-ochre" />
          <div
            class="cutout-photo"
            :style="{
              backgroundImage:
                'url(https://picsum.photos/seed/yoga-mat-warm-light/500/640)',
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
          placeholder="try 'hike', 'yoga', 'wild swim'…"
          aria-label="Search activities"
        />
        <button type="submit" class="search-button">Search</button>
      </form>

      <div class="cats">
        <RouterLink
          v-for="cat in categories"
          :key="cat.label"
          :to="{ name: 'activities' }"
          class="cat-pill"
          :class="cat.tone ? `cat-pill-${cat.tone}` : ''"
        >
          <span class="cat-pill-emoji">{{ cat.emoji }}</span>
          {{ cat.label }}
        </RouterLink>
      </div>
    </section>

    <section class="happenings">
      <div class="section-title">
        <h2>
          This <em>week</em>, in case you're free
        </h2>
        <RouterLink :to="{ name: 'activities' }" class="see-all">Browse everything →</RouterLink>
      </div>

      <div class="happen-grid">
        <article v-for="activity in activities" :key="activity.id" class="happen-card">
          <div class="happen-photo" :style="{ backgroundImage: `url(${activity.photo})` }">
            <span class="happen-tag" :class="`happen-tag-${activity.category.tone}`">
              {{ activity.category.label }}
            </span>
          </div>
          <div class="happen-body">
            <h3>{{ activity.title }}</h3>
            <div class="happen-when">{{ activity.when }}</div>
            <p>{{ activity.blurb }}</p>
            <div class="happen-foot">
              <div class="happen-going">
                <div class="avatars">
                  <div
                    v-for="(avatar, idx) in activity.avatars"
                    :key="idx"
                    class="avatar"
                    :style="{ backgroundImage: `url(${avatar})` }"
                  />
                </div>
                <span class="going-text">
                  <strong>{{ activity.going }}</strong> going
                </span>
              </div>
              <div class="happen-price" :class="{ 'happen-price-free': activity.isFree }">
                {{ activity.price }}
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="cta-strip">
      <div class="cta-strip-inner">
        <div>
          <h3>
            One <em>tiny</em> fee.<br />
            A whole year of weekends.
          </h3>
          <p>
            Membership unlocks discounted trips, drop-in weekly classes, the social calendar, and
            the WhatsApp group where the actual fun is planned.
          </p>
          <RouterLink :to="{ name: 'auth', query: { mode: 'register' } }" class="cta-strip-btn">
            Become a member →
          </RouterLink>
        </div>
        <div class="cta-strip-right">
          <div class="cta-strip-price">€40</div>
          <div class="cta-strip-per">for the whole academic year</div>
          <div class="cta-strip-arrow">↑ a single oat flat white a month, basically</div>
        </div>
      </div>
    </section>
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
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(680px 480px at 8% 6%, rgba(255, 122, 89, 0.16), transparent 60%),
    radial-gradient(760px 560px at 92% 10%, rgba(245, 176, 66, 0.16), transparent 60%),
    radial-gradient(840px 600px at 50% 92%, rgba(91, 165, 114, 0.12), transparent 60%);
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
  background:
    url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 8' width='100' height='8'><path d='M2 5 Q 16 1, 32 5 T 64 5 T 98 5' stroke='%23ff7a59' stroke-width='2.5' fill='none' stroke-linecap='round'/></svg>")
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
  position: relative;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 22px;
  color: var(--primary);
  padding-left: 50px;
}
.hero-cta-note::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 50%;
  width: 38px;
  height: 22px;
  transform: translateY(-50%) rotate(-12deg);
  background:
    url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 24' width='40' height='24'><path d='M38 12 Q 22 22 6 12' stroke='%2316306b' stroke-width='2' fill='none' stroke-linecap='round'/><path d='M14 7 L 6 12 L 14 17' stroke='%2316306b' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>")
    no-repeat center / contain;
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

/* Categories */
.cats {
  margin-top: 32px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}
.cat-pill {
  background: white;
  border: 1.5px solid var(--primary);
  color: var(--primary);
  padding: 8px 16px;
  border-radius: 999px;
  font-weight: 600;
  font-size: 13px;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition:
    transform 0.15s,
    box-shadow 0.15s,
    background 0.15s;
}
.cat-pill:hover {
  transform: translateY(-2px);
  box-shadow: 3px 3px 0 var(--primary);
}
.cat-pill-emoji {
  font-size: 14px;
}
.cat-pill-coral {
  background: var(--color-coral);
  color: white;
  border-color: var(--color-coral);
}
.cat-pill-ochre {
  background: var(--color-ochre);
  color: var(--primary);
  border-color: var(--color-ochre);
}
.cat-pill-leaf {
  background: var(--color-leaf);
  color: white;
  border-color: var(--color-leaf);
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

.happen-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  cursor: pointer;
  box-shadow: 4px 4px 0 var(--primary);
}
.happen-card:hover {
  transform: translate(-2px, -2px);
  box-shadow: 8px 8px 0 var(--primary);
}
.happen-photo {
  aspect-ratio: 4 / 3;
  background-size: cover;
  background-position: center;
  border-bottom: 2px solid var(--primary);
  position: relative;
}
.happen-tag {
  position: absolute;
  top: 14px;
  left: 14px;
  color: white;
  padding: 6px 14px;
  border-radius: 999px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 17px;
  transform: rotate(-3deg);
}
.happen-tag-coral {
  background: var(--color-coral);
}
.happen-tag-ochre {
  background: var(--color-ochre);
  color: var(--primary);
}
.happen-tag-leaf {
  background: var(--color-leaf);
}
.happen-body {
  padding: 22px 22px 20px;
}
.happen-body h3 {
  margin: 0 0 6px;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 26px;
  line-height: 1.1;
  color: var(--primary);
}
.happen-when {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-coral);
  margin-bottom: 12px;
}
.happen-body p {
  margin: 0 0 18px;
  font-size: 14.5px;
  color: var(--muted-foreground);
  line-height: 1.55;
}
.happen-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1.5px dashed color-mix(in srgb, var(--primary) 20%, transparent);
}
.happen-going {
  display: flex;
  align-items: center;
}
.avatars {
  display: flex;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid white;
  margin-left: -8px;
  background-size: cover;
  background-position: center;
}
.avatar:first-child {
  margin-left: 0;
}
.going-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted-foreground);
  margin-left: 10px;
}
.going-text strong {
  color: var(--primary);
}
.happen-price {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--primary);
}
.happen-price-free {
  color: var(--color-leaf);
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
  text-align: center;
}
.cta-strip-price {
  font-family: var(--font-display);
  font-size: 96px;
  line-height: 1;
  color: var(--color-ochre);
}
.cta-strip-per {
  font-family: var(--font-hand);
  font-size: 28px;
  margin-top: -8px;
}
.cta-strip-arrow {
  font-family: var(--font-hand);
  font-size: 20px;
  opacity: 0.9;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .cutout {
    max-width: 260px;
  }
  .hero {
    padding-top: 1.5rem;
  }
}
</style>
