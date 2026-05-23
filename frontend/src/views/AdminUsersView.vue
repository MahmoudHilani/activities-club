<script setup lang="ts">
import type { DateValue } from 'reka-ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { CalendarDate, parseDate } from '@internationalized/date'
import { format } from 'date-fns'
import { CalendarDays, Shield, ShieldOff, UserRoundX, UserRoundCheck } from 'lucide-vue-next'
import { computed, ref, watch } from 'vue'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { getApiMessage, mapUsersError } from '@/lib/api/errors'
import {
  getAdminUsers,
  updateAdminUserAccess,
  updateUserApprovalStatus,
  updateUserDateOfBirth,
} from '@/lib/api/users'
import type { ApprovalStatus, UserResponse } from '@/lib/api/types'
import { useSessionStore } from '@/stores/session'

const queryClient = useQueryClient()
const sessionStore = useSessionStore()
const actionError = ref('')

const dateOfBirthByUserId = ref<Record<number, string>>({})
const dateOfBirthPlaceholderByUserId = ref<Record<number, CalendarDate>>({})
const dateOfBirthPopoverOpenByUserId = ref<Record<number, boolean>>({})
const todayDate = new Date()
const maxDateOfBirth = new CalendarDate(
  todayDate.getFullYear(),
  todayDate.getMonth() + 1,
  todayDate.getDate(),
)
const minDateOfBirth = new CalendarDate(todayDate.getFullYear() - 100, 1, 1)
const monthOptions = [
  { value: '1', label: 'January' },
  { value: '2', label: 'February' },
  { value: '3', label: 'March' },
  { value: '4', label: 'April' },
  { value: '5', label: 'May' },
  { value: '6', label: 'June' },
  { value: '7', label: 'July' },
  { value: '8', label: 'August' },
  { value: '9', label: 'September' },
  { value: '10', label: 'October' },
  { value: '11', label: 'November' },
  { value: '12', label: 'December' },
]
const yearOptions = Array.from({ length: 101 }, (_, index) =>
  String(todayDate.getFullYear() - index),
)

const usersQuery = useQuery(() => ({
  queryKey: ['admin-users'],
  queryFn: () => getAdminUsers(),
}))

const users = computed(() =>
  [...(usersQuery.data.value ?? [])].sort((left, right) => {
    const approvalOrder = (status: ApprovalStatus) => (status === 'PENDING' ? 0 : 1)
    const orderDifference = approvalOrder(left.approvalStatus) - approvalOrder(right.approvalStatus)

    if (orderDifference !== 0) {
      return orderDifference
    }

    return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
  }),
)

const summaryStats = computed(() => {
  const list = usersQuery.data.value ?? []
  return [
    { label: 'Total', value: list.length, tone: 'ink' as const },
    {
      label: 'Pending',
      value: list.filter((u) => u.approvalStatus === 'PENDING').length,
      tone: 'ochre' as const,
    },
    {
      label: 'Approved',
      value: list.filter((u) => u.approvalStatus === 'APPROVED').length,
      tone: 'leaf' as const,
    },
    {
      label: 'Denied',
      value: list.filter((u) => u.approvalStatus === 'DENIED').length,
      tone: 'coral' as const,
    },
  ]
})

watch(
  () => usersQuery.data.value,
  (currentUsers) => {
    for (const user of currentUsers ?? []) {
      if (user.userType === 'STUDENT' && dateOfBirthByUserId.value[user.id] === undefined) {
        dateOfBirthByUserId.value[user.id] = user.dateOfBirth ?? ''
        dateOfBirthPlaceholderByUserId.value[user.id] =
          parseDateOfBirth(user.dateOfBirth) ?? defaultDateOfBirthPlaceholder()
      }
    }
  },
  { immediate: true },
)

function replaceCachedUser(updatedUser: UserResponse): void {
  queryClient.setQueryData<UserResponse[]>(['admin-users'], (currentUsers = []) =>
    currentUsers.map((user) => (user.id === updatedUser.id ? updatedUser : user)),
  )
}

const adminToggleMutation = useMutation(() => ({
  mutationFn: ({ userId, isAdmin }: { userId: number; isAdmin: boolean }) =>
    updateAdminUserAccess(userId, isAdmin),
  onSuccess: async (updatedUser) => {
    actionError.value = ''
    replaceCachedUser(updatedUser)
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] })
  },
  onError: (error) => {
    actionError.value = getApiMessage(error) ?? 'We could not update admin access right now.'
  },
}))

const approvalMutation = useMutation(() => ({
  mutationFn: ({
    userId,
    approvalStatus,
    dateOfBirth,
  }: {
    userId: number
    approvalStatus: Exclude<ApprovalStatus, 'PENDING'>
    dateOfBirth?: string | null
  }) => updateUserApprovalStatus(userId, approvalStatus, dateOfBirth),
  onSuccess: async (updatedUser) => {
    actionError.value = ''
    replaceCachedUser(updatedUser)
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] })
  },
  onError: (error) => {
    actionError.value =
      getApiMessage(error) ?? 'We could not update registration approval right now.'
  },
}))

const dateOfBirthMutation = useMutation(() => ({
  mutationFn: ({ userId, dateOfBirth }: { userId: number; dateOfBirth: string | null }) =>
    updateUserDateOfBirth(userId, dateOfBirth),
  onSuccess: async (updatedUser) => {
    actionError.value = ''
    dateOfBirthByUserId.value[updatedUser.id] = updatedUser.dateOfBirth ?? ''
    dateOfBirthPlaceholderByUserId.value[updatedUser.id] =
      parseDateOfBirth(updatedUser.dateOfBirth) ?? defaultDateOfBirthPlaceholder()
    replaceCachedUser(updatedUser)
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] })
  },
  onError: (error) => {
    actionError.value = getApiMessage(error) ?? 'We could not update the date of birth right now.'
  },
}))

function isCurrentUser(user: UserResponse): boolean {
  return sessionStore.user?.id === user.id
}

function toggleAdminAccess(user: UserResponse): void {
  adminToggleMutation.mutate({
    userId: user.id,
    isAdmin: !user.isAdmin,
  })
}

function updateApprovalStatus(
  user: UserResponse,
  approvalStatus: Exclude<ApprovalStatus, 'PENDING'>,
): void {
  const dateOfBirth =
    approvalStatus === 'APPROVED' && user.userType === 'STUDENT'
      ? (dateOfBirthByUserId.value[user.id] ?? '').trim() || null
      : null

  approvalMutation.mutate({
    userId: user.id,
    approvalStatus,
    dateOfBirth,
  })
}

function contactValue(value: string | null): string {
  return value ?? 'Not provided'
}

function canApprove(_user: UserResponse): boolean {
  return !approvalMutation.isPending.value
}

function approvalTone(status: ApprovalStatus): 'leaf' | 'ochre' | 'coral' {
  switch (status) {
    case 'APPROVED':
      return 'leaf'
    case 'PENDING':
      return 'ochre'
    case 'DENIED':
      return 'coral'
  }
}

function approvalLabel(status: ApprovalStatus): string {
  switch (status) {
    case 'APPROVED':
      return 'approved'
    case 'PENDING':
      return 'pending'
    case 'DENIED':
      return 'denied'
  }
}

function updateDateOfBirthFromCalendar(user: UserResponse, nextDate: DateValue | undefined): void {
  if (!nextDate) {
    return
  }

  const dateOfBirth = new CalendarDate(nextDate.year, nextDate.month, nextDate.day).toString()
  dateOfBirthByUserId.value[user.id] = dateOfBirth
  dateOfBirthPlaceholderByUserId.value[user.id] = new CalendarDate(
    nextDate.year,
    nextDate.month,
    nextDate.day,
  )
  dateOfBirthPopoverOpenByUserId.value[user.id] = false

  if (dateOfBirth === user.dateOfBirth) {
    return
  }

  dateOfBirthMutation.mutate({
    userId: user.id,
    dateOfBirth,
  })
}

function clearDateOfBirth(user: UserResponse): void {
  dateOfBirthByUserId.value[user.id] = ''
  dateOfBirthPopoverOpenByUserId.value[user.id] = false

  if (user.dateOfBirth === null) {
    return
  }

  dateOfBirthMutation.mutate({
    userId: user.id,
    dateOfBirth: null,
  })
}

function parseDateOfBirth(dateOfBirth: string | null): CalendarDate | null {
  if (!dateOfBirth) {
    return null
  }

  try {
    const date = parseDate(dateOfBirth)
    return new CalendarDate(date.year, date.month, date.day)
  } catch {
    return null
  }
}

function defaultDateOfBirthPlaceholder(): CalendarDate {
  return new CalendarDate(todayDate.getFullYear() - 18, 1, 1)
}

function getDateOfBirthCalendarValue(userId: number): CalendarDate | null {
  return parseDateOfBirth(dateOfBirthByUserId.value[userId] || null)
}

function getDateOfBirthPlaceholder(userId: number): CalendarDate {
  return dateOfBirthPlaceholderByUserId.value[userId] ?? defaultDateOfBirthPlaceholder()
}

function updateDateOfBirthPlaceholder(userId: number, nextDate: DateValue): void {
  dateOfBirthPlaceholderByUserId.value[userId] = new CalendarDate(
    nextDate.year,
    nextDate.month,
    nextDate.day,
  )
}

function updateDateOfBirthMonth(userId: number, value: string): void {
  const current = getDateOfBirthPlaceholder(userId)
  dateOfBirthPlaceholderByUserId.value[userId] = current.set({
    month: Number(value),
  }) as CalendarDate
}

function updateDateOfBirthYear(userId: number, value: string): void {
  const current = getDateOfBirthPlaceholder(userId)
  dateOfBirthPlaceholderByUserId.value[userId] = current.set({
    year: Number(value),
  }) as CalendarDate
}

function getDateOfBirthDisplay(user: UserResponse): string {
  const dateOfBirth = dateOfBirthByUserId.value[user.id]
  return dateOfBirth ? format(new Date(`${dateOfBirth}T00:00:00`), 'MMM d, yyyy') : 'Select date'
}
</script>

<template>
  <section class="users-shell">
    <header class="users-hero">
      <h1 class="users-title">
        <span class="display-text">The</span>
        <span class="hand-text"> people </span>
        <span class="display-text">behind the club</span>
      </h1>
      <p class="users-lede">
        Approve newcomers, grant admin access, and keep dates of birth on file for overnight
        eligibility.
      </p>
    </header>

    <Alert v-if="usersQuery.isError.value" variant="destructive">
      {{ mapUsersError(usersQuery.error.value) }}
    </Alert>

    <Alert v-else-if="actionError" variant="destructive">
      {{ actionError }}
    </Alert>

    <div class="stat-grid">
      <div
        v-for="card in summaryStats"
        :key="card.label"
        class="stat-card"
        :class="`stat-${card.tone}`"
      >
        <p class="stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value }}</p>
      </div>
    </div>

    <div v-if="usersQuery.isPending.value" class="state-card">
      Loading user management…
    </div>

    <div v-else-if="users.length === 0" class="empty-card">
      <h2 class="empty-title">
        <span class="display-text">Nobody's</span>
        <span class="hand-text"> signed up yet</span>
      </h2>
      <p class="empty-sub">When students or staff register, you'll see them appear here.</p>
    </div>

    <div v-else class="user-list">
      <article v-for="user in users" :key="user.id" class="user-card">
        <div class="user-card-inner">
          <div class="user-info">
            <div class="user-headline">
              <h2 class="user-name">{{ user.username }}</h2>
              <span
                class="craft-tag"
                :class="`craft-tag-${approvalTone(user.approvalStatus)}`"
              >
                {{ approvalLabel(user.approvalStatus) }}
              </span>
              <span
                v-if="user.userType === 'STAFF'"
                class="craft-pill"
              >Staff</span>
            </div>

            <dl class="user-fields">
              <div class="user-field">
                <dt>Email</dt>
                <dd>{{ user.email }}</dd>
              </div>
              <div class="user-field">
                <dt>Student #</dt>
                <dd>{{ contactValue(user.studentNumber) }}</dd>
              </div>
              <div class="user-field">
                <dt>Phone</dt>
                <dd>{{ contactValue(user.phoneNumber) }}</dd>
              </div>
              <div v-if="user.userType !== 'STUDENT'" class="user-field">
                <dt>Date of birth</dt>
                <dd>{{ contactValue(user.dateOfBirth) }}</dd>
              </div>
            </dl>
          </div>

          <div class="user-actions">
            <div v-if="user.userType === 'STUDENT'" class="dob-block">
              <span class="dob-label">Date of birth</span>
              <Popover v-model:open="dateOfBirthPopoverOpenByUserId[user.id]">
                <PopoverTrigger as-child>
                  <Button
                    :aria-label="`Date of birth for ${user.username}`"
                    :disabled="dateOfBirthMutation.isPending.value"
                    class="dob-trigger"
                    variant="outline"
                  >
                    <span class="dob-trigger-inner">
                      <CalendarDays class="h-4 w-4" />
                      <span
                        :class="
                          dateOfBirthByUserId[user.id] ? 'dob-trigger-has' : 'dob-trigger-empty'
                        "
                      >
                        {{ getDateOfBirthDisplay(user) }}
                      </span>
                    </span>
                  </Button>
                </PopoverTrigger>

                <PopoverContent align="end" class="w-[min(22rem,calc(100vw-2rem))] p-0">
                  <div class="grid grid-cols-2 gap-2 border-b border-border/70 p-3">
                    <Select
                      :model-value="String(getDateOfBirthPlaceholder(user.id).month)"
                      @update:model-value="
                        (value) => updateDateOfBirthMonth(user.id, String(value ?? ''))
                      "
                    >
                      <SelectTrigger class="h-10 rounded-xl">
                        <SelectValue placeholder="Month" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem
                          v-for="month in monthOptions"
                          :key="month.value"
                          :value="month.value"
                        >
                          {{ month.label }}
                        </SelectItem>
                      </SelectContent>
                    </Select>

                    <Select
                      :model-value="String(getDateOfBirthPlaceholder(user.id).year)"
                      @update:model-value="
                        (value) => updateDateOfBirthYear(user.id, String(value ?? ''))
                      "
                    >
                      <SelectTrigger class="h-10 rounded-xl">
                        <SelectValue placeholder="Year" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem v-for="year in yearOptions" :key="year" :value="year">
                          {{ year }}
                        </SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div class="p-3 pb-2">
                    <Calendar
                      :calendar-label="`Choose date of birth for ${user.username}`"
                      :max-value="maxDateOfBirth"
                      :min-value="minDateOfBirth"
                      :model-value="getDateOfBirthCalendarValue(user.id) ?? undefined"
                      :placeholder="getDateOfBirthPlaceholder(user.id)"
                      @update:model-value="(date) => updateDateOfBirthFromCalendar(user, date)"
                      @update:placeholder="(date) => updateDateOfBirthPlaceholder(user.id, date)"
                    />
                  </div>

                  <div
                    v-if="dateOfBirthByUserId[user.id]"
                    class="flex justify-end border-t border-border/70 px-4 py-3"
                  >
                    <Button
                      class="h-8 rounded-full px-3 text-muted-foreground shadow-none"
                      size="sm"
                      variant="ghost"
                      @click="clearDateOfBirth(user)"
                    >
                      Clear
                    </Button>
                  </div>
                </PopoverContent>
              </Popover>
            </div>

            <template v-if="user.approvalStatus === 'PENDING'">
              <Button
                :disabled="!canApprove(user)"
                class="user-action"
                size="sm"
                @click="updateApprovalStatus(user, 'APPROVED')"
              >
                <UserRoundCheck class="h-4 w-4" />
                Approve
              </Button>
              <Button
                :disabled="approvalMutation.isPending.value"
                class="user-action"
                size="sm"
                variant="outline"
                @click="updateApprovalStatus(user, 'DENIED')"
              >
                <UserRoundX class="h-4 w-4" />
                Deny
              </Button>
            </template>

            <Button
              v-else-if="user.approvalStatus === 'APPROVED'"
              :disabled="adminToggleMutation.isPending.value || isCurrentUser(user)"
              class="user-action"
              size="sm"
              :variant="user.isAdmin ? 'outline' : 'default'"
              @click="toggleAdminAccess(user)"
            >
              <ShieldOff v-if="user.isAdmin" class="h-4 w-4" />
              <Shield v-else class="h-4 w-4" />
              {{ user.isAdmin ? 'Remove admin' : 'Grant admin' }}
            </Button>

            <div v-else class="denied-note">
              This registration was denied. A fresh signup with the same email will reopen it.
            </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.users-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 1.75rem;
}

.users-hero {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: flex-start;
}
.users-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(40px, 6vw, 64px);
  line-height: 0.98;
  color: var(--primary);
  letter-spacing: -0.01em;
}
.users-title .hand-text {
  font-size: 1.12em;
}
.users-lede {
  max-width: 60ch;
  font-size: 16px;
  color: var(--muted-foreground);
  line-height: 1.55;
  margin: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
@media (min-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
.stat-card {
  position: relative;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 22px;
  padding: 18px;
}
.stat-ink {
  box-shadow: 4px 4px 0 var(--primary);
}
.stat-ochre {
  box-shadow: 4px 4px 0 var(--color-ochre);
}
.stat-leaf {
  box-shadow: 4px 4px 0 var(--color-leaf);
}
.stat-coral {
  box-shadow: 4px 4px 0 var(--color-coral);
}
.stat-label {
  margin: 0;
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 20px;
  color: var(--color-coral);
}
.stat-ochre .stat-label {
  color: var(--color-ochre);
}
.stat-leaf .stat-label {
  color: var(--color-leaf);
}
.stat-ink .stat-label {
  color: var(--primary);
}
.stat-value {
  margin: 4px 0 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 42px;
  line-height: 1;
  color: var(--primary);
}

.state-card,
.empty-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 28px;
}
.state-card {
  border-style: dashed;
  border-color: color-mix(in srgb, var(--primary) 35%, white);
  color: var(--muted-foreground);
  font-weight: 600;
}
.empty-card {
  box-shadow:
    4px 4px 0 var(--color-coral),
    8px 8px 0 var(--primary);
}
.empty-title {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 30px;
  line-height: 1;
  color: var(--primary);
}
.empty-sub {
  margin: 0;
  font-size: 15px;
  color: var(--muted-foreground);
  line-height: 1.55;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.user-card {
  background: white;
  border: 2px solid var(--primary);
  border-radius: 26px;
  padding: 22px;
  box-shadow: 4px 4px 0 var(--color-leaf);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}
.user-card:hover {
  transform: translate(-2px, -2px);
  box-shadow: 7px 7px 0 var(--color-leaf);
}

.user-card-inner {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
}
@media (min-width: 1024px) {
  .user-card-inner {
    grid-template-columns: 1fr 16rem;
  }
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.user-headline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.user-name {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 26px;
  line-height: 1.05;
  color: var(--primary);
  word-break: break-word;
}

.user-fields {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  margin: 0;
}
@media (min-width: 640px) {
  .user-fields {
    grid-template-columns: repeat(2, 1fr);
  }
}
.user-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  background: color-mix(in srgb, white 92%, #f4efe4 8%);
  border: 1.5px solid color-mix(in srgb, var(--primary) 18%, white);
  border-radius: 14px;
}
.user-field dt {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 15px;
  color: var(--color-coral);
  line-height: 1;
}
.user-field dd {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--primary);
  font-weight: 600;
  word-break: break-word;
}

.user-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.user-action {
  width: 100%;
  justify-content: center;
}

.dob-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 4px;
}
.dob-label {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 16px;
  color: var(--color-coral);
}
.dob-trigger {
  height: 2.75rem;
  width: 100%;
  justify-content: flex-start;
  font-weight: 600;
}
.dob-trigger-inner {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.dob-trigger-inner svg {
  color: var(--color-coral);
  flex-shrink: 0;
}
.dob-trigger-has {
  color: var(--primary);
}
.dob-trigger-empty {
  color: var(--muted-foreground);
  font-weight: 500;
}

.denied-note {
  background: color-mix(in srgb, var(--color-coral) 10%, white);
  border: 2px dashed var(--color-coral);
  color: var(--color-coral);
  border-radius: 18px;
  padding: 12px 14px;
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.45;
}
</style>
