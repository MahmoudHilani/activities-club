<script setup lang="ts">
import type { DateValue } from 'reka-ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { CalendarDate, parseDate } from '@internationalized/date'
import { format } from 'date-fns'
import {
  CalendarDays,
  LoaderCircle,
  MoreHorizontal,
  Search,
  Shield,
  ShieldOff,
  UserRoundCheck,
  UserRoundX,
  X,
} from 'lucide-vue-next'
import { computed, ref, watch } from 'vue'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Input } from '@/components/ui/input'
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
import type { ApprovalStatus, UserResponse, UserType } from '@/lib/api/types'
import { cn } from '@/lib/utils'
import { useSessionStore } from '@/stores/session'

type StatusFilter = 'ALL' | ApprovalStatus
type TypeFilter = 'ALL' | UserType
type SortKey = 'pending-first' | 'created-desc' | 'created-asc' | 'username-asc'
type ConfirmIntent =
  | { kind: 'grant-admin' | 'remove-admin'; user: UserResponse }
  | { kind: 'deny'; user: UserResponse }

const queryClient = useQueryClient()
const sessionStore = useSessionStore()
const actionError = ref('')

const searchTerm = ref('')
const statusFilter = ref<StatusFilter>('PENDING')
const typeFilter = ref<TypeFilter>('ALL')
const sortKey = ref<SortKey>('pending-first')
const confirmIntent = ref<ConfirmIntent | null>(null)

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

const users = computed(() => usersQuery.data.value ?? [])

const counts = computed(() => {
  const list = users.value
  return {
    all: list.length,
    pending: list.filter((user) => user.approvalStatus === 'PENDING').length,
    approved: list.filter((user) => user.approvalStatus === 'APPROVED').length,
    denied: list.filter((user) => user.approvalStatus === 'DENIED').length,
  }
})

const filterChips = computed(() => [
  { key: 'ALL' as const, label: 'All', count: counts.value.all, tone: 'ink' as const },
  {
    key: 'PENDING' as const,
    label: 'Pending',
    count: counts.value.pending,
    tone: 'ochre' as const,
  },
  {
    key: 'APPROVED' as const,
    label: 'Approved',
    count: counts.value.approved,
    tone: 'leaf' as const,
  },
  {
    key: 'DENIED' as const,
    label: 'Denied',
    count: counts.value.denied,
    tone: 'coral' as const,
  },
])

const filteredUsers = computed(() => {
  const term = searchTerm.value.trim().toLowerCase()
  let list = users.value.slice()

  if (statusFilter.value !== 'ALL') {
    list = list.filter((user) => user.approvalStatus === statusFilter.value)
  }

  if (typeFilter.value !== 'ALL') {
    list = list.filter((user) => user.userType === typeFilter.value)
  }

  if (term) {
    list = list.filter((user) =>
      [user.username, user.email, user.studentNumber, user.phoneNumber].some((value) =>
        (value ?? '').toLowerCase().includes(term),
      ),
    )
  }

  switch (sortKey.value) {
    case 'pending-first':
      list.sort((left, right) => {
        const statusDifference =
          pendingRank(left.approvalStatus) - pendingRank(right.approvalStatus)
        return statusDifference || compareDates(right.createdAt, left.createdAt)
      })
      break
    case 'created-desc':
      list.sort((left, right) => compareDates(right.createdAt, left.createdAt))
      break
    case 'created-asc':
      list.sort((left, right) => compareDates(left.createdAt, right.createdAt))
      break
    case 'username-asc':
      list.sort((left, right) => left.username.localeCompare(right.username))
      break
  }

  return list
})

const approvedAdminCount = computed(
  () =>
    users.value.filter((user) => user.approvalStatus === 'APPROVED' && user.isAdmin).length,
)

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

function replaceCachedUser(updatedUser: UserResponse): void {
  queryClient.setQueryData<UserResponse[]>(['admin-users'], (currentUsers = []) =>
    currentUsers.map((user) => (user.id === updatedUser.id ? updatedUser : user)),
  )
}

function requestAdminToggle(user: UserResponse): void {
  confirmIntent.value = {
    kind: user.isAdmin ? 'remove-admin' : 'grant-admin',
    user,
  }
}

function requestDeny(user: UserResponse): void {
  confirmIntent.value = { kind: 'deny', user }
}

function dismissConfirm(): void {
  confirmIntent.value = null
}

async function executeConfirm(): Promise<void> {
  const intent = confirmIntent.value
  if (!intent) {
    return
  }

  if (intent.kind === 'deny') {
    await updateApprovalStatus(intent.user, 'DENIED')
  } else {
    await adminToggleMutation.mutateAsync({
      userId: intent.user.id,
      isAdmin: intent.kind === 'grant-admin',
    })
  }

  dismissConfirm()
}

function updateApprovalStatus(
  user: UserResponse,
  approvalStatus: Exclude<ApprovalStatus, 'PENDING'>,
): Promise<UserResponse> {
  const dateOfBirth =
    approvalStatus === 'APPROVED' && user.userType === 'STUDENT'
      ? (dateOfBirthByUserId.value[user.id] ?? '').trim() || null
      : null

  return approvalMutation.mutateAsync({
    userId: user.id,
    approvalStatus,
    dateOfBirth,
  })
}

function canRemoveAdmin(user: UserResponse): boolean {
  return !user.isAdmin || approvedAdminCount.value > 1
}

function adminDisabledReason(user: UserResponse): string {
  if (isCurrentUser(user)) {
    return 'You'
  }

  if (!canRemoveAdmin(user)) {
    return 'Only admin remaining'
  }

  return ''
}

function isCurrentUser(user: UserResponse): boolean {
  return sessionStore.user?.id === user.id
}

function contactValue(value: string | null): string {
  return value ?? 'Not provided'
}

function pendingRank(status: ApprovalStatus): number {
  return status === 'PENDING' ? 0 : 1
}

function compareDates(a: string, b: string): number {
  return new Date(a).getTime() - new Date(b).getTime()
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
  return status.toLowerCase()
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
  <section class="proto-shell">
    <header class="proto-hero">
      <div class="proto-hero-text">
        <h1 class="proto-title">
          <span class="display-text">The</span>
          <span class="hand-text"> people </span>
          <span class="display-text">desk</span>
        </h1>
        <p class="proto-lede">
          Review pending registrations, keep student dates of birth current, and grant admin
          access with explicit confirmation.
        </p>
      </div>
    </header>

    <Alert v-if="usersQuery.isError.value" variant="destructive">
      {{ mapUsersError(usersQuery.error.value) }}
    </Alert>

    <Alert v-else-if="actionError" variant="destructive">
      {{ actionError }}
    </Alert>

    <div class="proto-toolbar">
      <div class="proto-search">
        <Search class="proto-search-icon h-4 w-4" />
        <Input
          v-model="searchTerm"
          aria-label="Search users"
          class="proto-search-input"
          placeholder="Search username, email, student #, phone..."
          type="search"
        />
        <button
          v-if="searchTerm"
          class="proto-search-clear"
          aria-label="Clear search"
          @click="searchTerm = ''"
        >
          <X class="h-4 w-4" />
        </button>
      </div>

      <div class="proto-type-filter" aria-label="Filter by user type">
        <button
          :class="cn('proto-type-chip', typeFilter === 'ALL' && 'proto-type-chip-active')"
          @click="typeFilter = 'ALL'"
        >
          All types
        </button>
        <button
          :class="cn('proto-type-chip', typeFilter === 'STUDENT' && 'proto-type-chip-active')"
          @click="typeFilter = 'STUDENT'"
        >
          Students
        </button>
        <button
          :class="cn('proto-type-chip', typeFilter === 'STAFF' && 'proto-type-chip-active')"
          @click="typeFilter = 'STAFF'"
        >
          Staff
        </button>
      </div>

      <div class="proto-sort">
        <label class="proto-sort-label">Sort</label>
        <Select v-model="sortKey">
          <SelectTrigger class="proto-sort-trigger">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="pending-first">Pending first</SelectItem>
            <SelectItem value="created-desc">Most recent</SelectItem>
            <SelectItem value="created-asc">Oldest first</SelectItem>
            <SelectItem value="username-asc">Username A-Z</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>

    <div class="proto-chips" role="tablist" aria-label="Filter by approval status">
      <button
        v-for="chip in filterChips"
        :key="chip.key"
        role="tab"
        :aria-selected="statusFilter === chip.key"
        :class="
          cn(
            'proto-chip',
            `proto-chip-${chip.tone}`,
            statusFilter === chip.key && 'proto-chip-active',
          )
        "
        @click="statusFilter = chip.key"
      >
        <span>{{ chip.label }}</span>
        <span class="proto-chip-count">{{ chip.count }}</span>
      </button>
    </div>

    <div v-if="usersQuery.isPending.value" class="proto-state">
      <LoaderCircle class="h-5 w-5 animate-spin" />
      <span>Loading user management...</span>
    </div>

    <div v-else-if="users.length === 0" class="proto-state proto-state-empty">
      <p class="proto-state-title">
        <span class="display-text">Nobody's</span>
        <span class="hand-text"> signed up yet</span>
      </p>
      <p class="proto-state-sub">When students or staff register, they will appear here.</p>
    </div>

    <div v-else-if="filteredUsers.length === 0" class="proto-state proto-state-empty">
      <p class="proto-state-title">
        <span class="display-text">Nothing</span>
        <span class="hand-text"> matches</span>
      </p>
      <p class="proto-state-sub">Try a different search or choose another status.</p>
    </div>

    <div v-else class="proto-table">
      <div class="proto-row proto-row-head" aria-hidden="true">
        <span>User</span>
        <span>Contact</span>
        <span>Date of birth</span>
        <span></span>
      </div>

      <article v-for="user in filteredUsers" :key="user.id" class="proto-row proto-row-data">
        <div class="proto-col-user">
          <div class="proto-title-line">
            <h2 class="proto-user-name">{{ user.username }}</h2>
            <span class="proto-status" :class="`proto-status-${approvalTone(user.approvalStatus)}`">
              {{ approvalLabel(user.approvalStatus) }}
            </span>
            <span v-if="user.userType === 'STAFF'" class="proto-staff-pill">Staff</span>
          </div>
          <p v-if="adminDisabledReason(user)" class="proto-row-note">
            {{ adminDisabledReason(user) }}
          </p>
        </div>

        <div class="proto-col-contact">
          <span>{{ user.email }}</span>
          <span>Student #: {{ contactValue(user.studentNumber) }}</span>
          <span>Phone: {{ contactValue(user.phoneNumber) }}</span>
        </div>

        <div class="proto-col-dob">
          <template v-if="user.userType === 'STUDENT' && user.approvalStatus !== 'DENIED'">
            <Popover v-model:open="dateOfBirthPopoverOpenByUserId[user.id]">
              <PopoverTrigger as-child>
                <Button
                  :aria-label="`Date of birth for ${user.username}`"
                  :disabled="dateOfBirthMutation.isPending.value"
                  class="dob-trigger"
                  variant="outline"
                >
                  <CalendarDays class="h-4 w-4" />
                  <span :class="dateOfBirthByUserId[user.id] ? 'dob-has' : 'dob-empty'">
                    {{ getDateOfBirthDisplay(user) }}
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
          </template>
          <span v-else-if="user.approvalStatus === 'DENIED'" class="proto-denied-note">
            Denied - appeal required to return this registration to review.
          </span>
        </div>

        <div class="proto-col-actions">
          <template v-if="user.approvalStatus === 'PENDING'">
            <Button
              :disabled="approvalMutation.isPending.value"
              class="proto-primary-action"
              size="sm"
              @click="updateApprovalStatus(user, 'APPROVED')"
            >
              <UserRoundCheck class="h-4 w-4" />
              Approve
            </Button>

            <Popover>
              <PopoverTrigger as-child>
                <Button
                  aria-label="More actions"
                  class="proto-overflow-btn"
                  size="sm"
                  variant="outline"
                >
                  <MoreHorizontal class="h-4 w-4" />
                </Button>
              </PopoverTrigger>
              <PopoverContent align="end" class="proto-menu">
                <button class="proto-menu-item proto-menu-item-danger" @click="requestDeny(user)">
                  <UserRoundX class="h-4 w-4" />
                  Deny
                </button>
              </PopoverContent>
            </Popover>
          </template>

          <Button
            v-else-if="user.approvalStatus === 'APPROVED'"
            :disabled="
              adminToggleMutation.isPending.value ||
              isCurrentUser(user) ||
              (user.isAdmin && !canRemoveAdmin(user))
            "
            class="proto-primary-action"
            size="sm"
            :variant="user.isAdmin ? 'outline' : 'default'"
            @click="requestAdminToggle(user)"
          >
            <ShieldOff v-if="user.isAdmin" class="h-4 w-4" />
            <Shield v-else class="h-4 w-4" />
            {{ user.isAdmin ? 'Remove admin' : 'Grant admin' }}
          </Button>
        </div>
      </article>
    </div>

    <div
      v-if="confirmIntent"
      class="proto-modal-backdrop"
      role="dialog"
      aria-modal="true"
      @click.self="dismissConfirm"
    >
      <div class="proto-modal">
        <h2 class="proto-modal-title">
          <template v-if="confirmIntent.kind === 'grant-admin'">
            Grant admin access?
          </template>
          <template v-else-if="confirmIntent.kind === 'remove-admin'">
            Remove admin access?
          </template>
          <template v-else>
            Deny this registration?
          </template>
        </h2>
        <p class="proto-modal-body">
          <template v-if="confirmIntent.kind === 'grant-admin'">
            {{ confirmIntent.user.username }} will become an admin and can see everything in
            user and activity management.
          </template>
          <template v-else-if="confirmIntent.kind === 'remove-admin'">
            {{ confirmIntent.user.username }} will lose access to admin management screens.
          </template>
          <template v-else>
            {{ confirmIntent.user.username }} will be denied. They will need a registration
            appeal to return to the pending queue.
          </template>
        </p>

        <div class="proto-modal-actions">
          <Button size="sm" variant="outline" @click="dismissConfirm">Keep reviewing</Button>
          <Button
            :disabled="adminToggleMutation.isPending.value || approvalMutation.isPending.value"
            :variant="confirmIntent.kind === 'deny' ? 'destructive' : 'default'"
            size="sm"
            @click="executeConfirm"
          >
            <LoaderCircle
              v-if="adminToggleMutation.isPending.value || approvalMutation.isPending.value"
              class="h-4 w-4 animate-spin"
            />
            <template v-if="confirmIntent.kind === 'grant-admin'">Grant admin access</template>
            <template v-else-if="confirmIntent.kind === 'remove-admin'">Remove admin access</template>
            <template v-else>Deny registration</template>
          </Button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.proto-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 1.5rem;
}

.proto-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}
.proto-hero-text {
  display: flex;
  min-width: 280px;
  flex: 1;
  flex-direction: column;
  gap: 8px;
}
.proto-title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin: 0;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: clamp(36px, 5vw, 56px);
  font-weight: 400;
  line-height: 1;
}
.proto-title .hand-text {
  font-size: 1.12em;
}
.proto-lede {
  max-width: 64ch;
  margin: 0;
  color: var(--muted-foreground);
  font-size: 15px;
  line-height: 1.5;
}

.proto-toolbar {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
@media (min-width: 900px) {
  .proto-toolbar {
    grid-template-columns: minmax(0, 1fr) auto auto;
    align-items: center;
  }
}
.proto-search {
  position: relative;
  display: flex;
  align-items: center;
}
.proto-search-icon {
  position: absolute;
  left: 14px;
  color: var(--muted-foreground);
  pointer-events: none;
}
.proto-search-input {
  padding-right: 40px;
  padding-left: 40px;
}
.proto-search-clear {
  position: absolute;
  right: 12px;
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--muted-foreground);
  cursor: pointer;
}
.proto-type-filter,
.proto-sort {
  display: flex;
  align-items: center;
  gap: 8px;
}
.proto-type-chip {
  border: 2px solid color-mix(in srgb, var(--primary) 22%, white);
  border-radius: 999px;
  background: white;
  color: var(--primary);
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.proto-type-chip-active {
  border-color: var(--primary);
  background: var(--primary);
  color: white;
}
.proto-sort-label {
  color: var(--primary);
  font-family: var(--font-hand);
  font-size: 16px;
  font-weight: 700;
}
.proto-sort-trigger {
  min-width: 190px;
}

.proto-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.proto-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 2px solid var(--primary);
  border-radius: 999px;
  background: white;
  color: var(--primary);
  padding: 8px 14px;
  font-family: var(--font-hand);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
.proto-chip-count {
  display: inline-flex;
  min-width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  padding: 0 6px;
  font-size: 12px;
}
.proto-chip-active {
  background: var(--primary);
  color: white;
  box-shadow: 3px 3px 0 var(--color-coral);
}
.proto-chip-active .proto-chip-count {
  background: color-mix(in srgb, white 25%, transparent);
  color: white;
}
.proto-chip-ochre.proto-chip-active {
  border-color: var(--color-ochre);
  background: var(--color-ochre);
}
.proto-chip-leaf.proto-chip-active {
  border-color: var(--color-leaf);
  background: var(--color-leaf);
}
.proto-chip-coral.proto-chip-active {
  border-color: var(--color-coral);
  background: var(--color-coral);
}

.proto-state {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 2px dashed color-mix(in srgb, var(--primary) 35%, white);
  border-radius: 22px;
  background: white;
  color: var(--muted-foreground);
  padding: 28px;
  font-weight: 600;
}
.proto-state-empty {
  align-items: flex-start;
  flex-direction: column;
  gap: 6px;
}
.proto-state-title {
  margin: 0;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
}
.proto-state-sub {
  margin: 0;
  color: var(--muted-foreground);
  font-size: 14.5px;
}

.proto-table {
  overflow: hidden;
  border: 2px solid var(--primary);
  border-radius: 22px;
  background: white;
  box-shadow: 5px 5px 0 var(--color-coral);
}
.proto-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  border-bottom: 1px solid color-mix(in srgb, var(--primary) 12%, transparent);
  padding: 14px 18px;
}
.proto-row:last-child {
  border-bottom: 0;
}
.proto-row-head {
  display: none;
  background: color-mix(in srgb, var(--primary) 5%, white);
  color: var(--muted-foreground);
  font-family: var(--font-hand);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}
@media (min-width: 980px) {
  .proto-row {
    grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.4fr) minmax(12rem, 0.8fr) auto;
    align-items: center;
    gap: 16px;
  }
  .proto-row-head {
    display: grid;
    padding: 10px 18px;
  }
}
.proto-title-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.proto-user-name {
  margin: 0;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 400;
  line-height: 1.1;
}
.proto-status,
.proto-staff-pill {
  display: inline-flex;
  align-items: center;
  border: 1.5px solid var(--primary);
  border-radius: 999px;
  padding: 2px 10px;
  font-family: var(--font-hand);
  font-size: 13px;
  font-weight: 700;
  text-transform: capitalize;
}
.proto-staff-pill {
  background: color-mix(in srgb, var(--primary) 10%, white);
  text-transform: none;
}
.proto-status-leaf {
  border-color: var(--color-leaf);
  background: color-mix(in srgb, var(--color-leaf) 18%, white);
  color: color-mix(in srgb, var(--color-leaf) 60%, var(--primary));
}
.proto-status-ochre {
  border-color: var(--color-ochre);
  background: color-mix(in srgb, var(--color-ochre) 22%, white);
  color: color-mix(in srgb, var(--color-ochre) 50%, var(--primary));
}
.proto-status-coral {
  border-color: var(--color-coral);
  background: color-mix(in srgb, var(--color-coral) 18%, white);
  color: var(--color-coral);
}
.proto-row-note {
  margin: 4px 0 0;
  color: var(--muted-foreground);
  font-size: 12px;
  font-weight: 700;
}
.proto-col-contact {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  color: var(--primary);
  font-size: 13.5px;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.proto-col-contact span:not(:first-child),
.proto-muted {
  color: var(--muted-foreground);
  font-weight: 500;
}
.dob-trigger {
  width: 100%;
  justify-content: flex-start;
  gap: 8px;
  font-weight: 600;
}
.dob-trigger svg {
  color: var(--color-coral);
}
.dob-empty {
  color: var(--muted-foreground);
  font-weight: 500;
}
.dob-has {
  color: var(--primary);
}
.proto-denied-note {
  display: block;
  border: 2px dashed var(--color-coral);
  border-radius: 14px;
  background: color-mix(in srgb, var(--color-coral) 10%, white);
  color: var(--color-coral);
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
}
.proto-col-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
@media (min-width: 980px) {
  .proto-col-actions {
    justify-content: flex-end;
  }
}
.proto-primary-action {
  white-space: nowrap;
}
.proto-overflow-btn {
  width: 36px;
  padding: 0;
}
.proto-menu {
  min-width: 160px;
  padding: 6px;
}
.proto-menu-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--primary);
  padding: 8px 10px;
  font-size: 14px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}
.proto-menu-item-danger {
  color: var(--color-coral);
}

.proto-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--primary) 50%, transparent);
  padding: 20px;
}
.proto-modal {
  display: flex;
  width: 100%;
  max-width: 440px;
  flex-direction: column;
  gap: 12px;
  border: 2px solid var(--primary);
  border-radius: 22px;
  background: white;
  box-shadow: 6px 6px 0 var(--color-coral);
  padding: 24px;
}
.proto-modal-title {
  margin: 0;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 400;
  line-height: 1.2;
}
.proto-modal-body {
  margin: 0;
  color: var(--muted-foreground);
  font-size: 14.5px;
  line-height: 1.5;
}
.proto-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}
</style>
