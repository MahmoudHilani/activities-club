<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { format } from 'date-fns'
import { LoaderCircle, Search } from 'lucide-vue-next'
import { computed, ref } from 'vue'

import { Alert } from '@/components/ui/alert'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { getAdminSportsClubSignups } from '@/lib/api/sportsClubSignups'
import type { SportsClub, SportsClubSignupResponse } from '@/lib/api/types'

type SportFilter = 'ALL' | SportsClub

const searchTerm = ref('')
const sportFilter = ref<SportFilter>('ALL')

const signupsQuery = useQuery(() => ({
  queryKey: ['admin-sports-club-signups'],
  queryFn: () => getAdminSportsClubSignups({ page: 0, size: 200 }),
}))

const sportFilterOptions: ReadonlyArray<{ value: SportFilter; label: string }> = [
  { value: 'ALL', label: 'All sports' },
  { value: 'FOOTBALL', label: 'Football' },
  { value: 'BASKETBALL', label: 'Basketball' },
  { value: 'BADMINTON', label: 'Badminton' },
  { value: 'VOLLEYBALL', label: 'Volleyball' },
  { value: 'CRICKET', label: 'Cricket' },
  { value: 'BILLIARDS', label: 'Billiards' },
  { value: 'DANCE', label: 'Dance' },
  { value: 'YOGA', label: 'Yoga' },
  { value: 'PILATES', label: 'Pilates' },
  { value: 'BOXERCISE', label: 'Boxercise' },
]

const sportLabel: Record<SportsClub, string> = {
  FOOTBALL: 'Football',
  BASKETBALL: 'Basketball',
  BADMINTON: 'Badminton',
  VOLLEYBALL: 'Volleyball',
  CRICKET: 'Cricket',
  BILLIARDS: 'Billiards',
  DANCE: 'Dance',
  YOGA: 'Yoga',
  PILATES: 'Pilates',
  BOXERCISE: 'Boxercise',
}

const filteredSignups = computed<SportsClubSignupResponse[]>(() => {
  const all = signupsQuery.data.value?.content ?? []
  const search = searchTerm.value.trim().toLowerCase()
  const sport = sportFilter.value

  return all.filter((signup) => {
    if (sport !== 'ALL' && !signup.sportsClubs.includes(sport)) return false
    if (!search) return true
    const haystack = [
      signup.name,
      signup.email,
      signup.course,
      signup.phoneNumber,
      signup.studentNumber,
    ]
      .join(' ')
      .toLowerCase()
    return haystack.includes(search)
  })
})

function formatCreatedAt(value: string): string {
  return format(new Date(value), "d MMM yyyy 'at' HH:mm")
}

function genderLabel(value: 'MALE' | 'FEMALE' | 'OTHER'): string {
  switch (value) {
    case 'MALE':
      return 'Male'
    case 'FEMALE':
      return 'Female'
    case 'OTHER':
      return 'Other'
  }
}
</script>

<template>
  <div class="admin-page">
    <header class="admin-header">
      <span class="craft-tag craft-tag-ochre">admin</span>
      <h1 class="admin-title">
        <span class="display-text">Sports club</span>
        <span class="hand-text"> signups</span>
      </h1>
      <p class="admin-lede">People who filled in the homepage sports club form.</p>
    </header>

    <section class="filters">
      <label class="filter-field">
        <span class="filter-label">Search</span>
        <div class="search-input">
          <Search class="h-4 w-4 search-icon" aria-hidden="true" />
          <Input
            v-model="searchTerm"
            placeholder="Name, email, course…"
            class="search-input-field"
          />
        </div>
      </label>

      <label class="filter-field">
        <span class="filter-label">Sport</span>
        <Select v-model="sportFilter">
          <SelectTrigger aria-label="Filter by sport">
            <SelectValue placeholder="All sports" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="option in sportFilterOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </SelectItem>
          </SelectContent>
        </Select>
      </label>
    </section>

    <Alert v-if="signupsQuery.isError.value" variant="destructive">
      We couldn't load sports club signups right now.
    </Alert>

    <div v-if="signupsQuery.isPending.value" class="loading-row">
      <LoaderCircle class="h-5 w-5 animate-spin" />
      Loading signups…
    </div>

    <div v-else-if="filteredSignups.length === 0" class="empty-state">
      No signups match your filters yet.
    </div>

    <ul v-else class="signup-list">
      <li v-for="signup in filteredSignups" :key="signup.id" class="signup-card">
        <div class="signup-card-head">
          <h3 class="signup-name">{{ signup.name }}</h3>
          <span class="signup-date">{{ formatCreatedAt(signup.createdAt) }}</span>
        </div>
        <dl class="signup-details">
          <div class="detail">
            <dt>Email</dt>
            <dd>
              <a :href="`mailto:${signup.email}`">{{ signup.email }}</a>
            </dd>
          </div>
          <div class="detail">
            <dt>Phone</dt>
            <dd>{{ signup.phoneNumber }}</dd>
          </div>
          <div class="detail">
            <dt>Student number</dt>
            <dd>{{ signup.studentNumber }}</dd>
          </div>
          <div class="detail">
            <dt>Course</dt>
            <dd>{{ signup.course }}</dd>
          </div>
          <div class="detail">
            <dt>Gender</dt>
            <dd>{{ genderLabel(signup.gender) }}</dd>
          </div>
          <div class="detail">
            <dt>Account</dt>
            <dd>{{ signup.userId ? `#${signup.userId}` : 'Anonymous' }}</dd>
          </div>
        </dl>
        <ul class="sport-tags">
          <li v-for="sport in signup.sportsClubs" :key="sport" class="sport-pill">
            {{ sportLabel[sport] }}
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 64px;
}
.admin-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.admin-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(32px, 5vw, 48px);
  line-height: 1;
  color: var(--primary);
}
.admin-title .hand-text {
  font-family: var(--font-hand);
  font-weight: 700;
  color: var(--color-coral);
}
.admin-lede {
  margin: 0;
  color: var(--muted-foreground);
}
.filters {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}
@media (min-width: 720px) {
  .filters {
    grid-template-columns: 2fr 1fr;
  }
}
.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.filter-label {
  font-family: var(--font-display);
  color: var(--primary);
  font-size: 18px;
}
.search-input {
  position: relative;
}
.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--muted-foreground);
}
.search-input-field {
  padding-left: 36px;
}
.loading-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted-foreground);
  padding: 16px 0;
}
.empty-state {
  padding: 28px;
  text-align: center;
  color: var(--muted-foreground);
  border: 2px dashed var(--primary);
  border-radius: 24px;
}
.signup-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 16px;
}
.signup-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 24px;
  padding: 20px 22px;
  box-shadow: 4px 4px 0 var(--primary);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.signup-card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.signup-name {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 24px;
  color: var(--primary);
}
.signup-date {
  font-family: var(--font-hand);
  color: var(--color-coral);
  font-size: 16px;
}
.signup-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px 18px;
  margin: 0;
}
.detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 14px;
}
.detail dt {
  font-family: var(--font-hand);
  color: var(--muted-foreground);
  font-size: 14px;
}
.detail dd {
  margin: 0;
  color: var(--primary);
  font-weight: 600;
}
.detail dd a {
  color: var(--primary);
  text-decoration: underline;
}
.sport-tags {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.sport-pill {
  background: var(--color-ochre);
  color: var(--primary);
  padding: 4px 12px;
  border-radius: 999px;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 15px;
  transform: rotate(-2deg);
}
</style>
