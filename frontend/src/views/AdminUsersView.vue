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

function canApprove(user: UserResponse): boolean {
  return !approvalMutation.isPending.value
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
  <section class="flex flex-1 flex-col gap-8">
    <h1 class="font-serif text-4xl font-bold tracking-tight text-foreground sm:text-5xl">
      User management
    </h1>

    <Alert v-if="usersQuery.isError.value" variant="destructive">
      {{ mapUsersError(usersQuery.error.value) }}
    </Alert>

    <Alert v-else-if="actionError" variant="destructive">
      {{ actionError }}
    </Alert>

    <div
      v-if="usersQuery.isPending.value"
      class="surface-panel rounded-[2rem] border border-white/70 p-8 text-muted-foreground"
    >
      Loading user management...
    </div>

    <div
      v-else-if="users.length === 0"
      class="surface-panel rounded-[2rem] border border-white/70 px-6 py-10 text-center"
    >
      <p class="text-lg font-semibold text-foreground">No users have registered yet.</p>
    </div>

    <div v-else class="grid gap-5">
      <article
        v-for="user in users"
        :key="user.id"
        class="surface-panel rounded-[1.85rem] border border-white/70 p-5 sm:p-6"
      >
        <div class="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="break-words text-xl font-bold text-foreground">
                {{ user.username }}
              </h2>
              <span
                v-if="user.userType === 'STAFF'"
                class="rounded-full bg-secondary px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-secondary-foreground"
              >
                Staff
              </span>
            </div>

            <div class="mt-4 grid gap-2 text-sm text-muted-foreground sm:grid-cols-2">
              <p><span class="font-semibold text-foreground">Email:</span> {{ user.email }}</p>
              <p>
                <span class="font-semibold text-foreground">Student number:</span>
                {{ contactValue(user.studentNumber) }}
              </p>
              <p>
                <span class="font-semibold text-foreground">Phone number:</span>
                {{ contactValue(user.phoneNumber) }}
              </p>
              <p v-if="user.userType !== 'STUDENT'">
                <span class="font-semibold text-foreground">Date of birth:</span>
                {{ contactValue(user.dateOfBirth) }}
              </p>
            </div>
          </div>

          <div class="flex w-full flex-col gap-2 xl:min-w-[14rem] xl:max-w-[14rem]">
            <div v-if="user.userType === 'STUDENT'" class="space-y-2">
              <span class="text-sm font-semibold text-foreground">Date of birth</span>
              <Popover v-model:open="dateOfBirthPopoverOpenByUserId[user.id]">
                <PopoverTrigger as-child>
                  <Button
                    :aria-label="`Date of birth for ${user.username}`"
                    :disabled="dateOfBirthMutation.isPending.value"
                    class="h-10 w-full justify-between rounded-2xl border border-border bg-white/80 px-3 text-left text-sm font-medium text-foreground shadow-none hover:bg-white"
                    variant="outline"
                  >
                    <span class="flex min-w-0 items-center gap-2">
                      <CalendarDays class="h-4 w-4 shrink-0 text-primary" />
                      <span
                        class="truncate"
                        :class="
                          dateOfBirthByUserId[user.id] ? 'text-foreground' : 'text-muted-foreground'
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
                      class="h-8 rounded-full px-3 text-muted-foreground shadow-none hover:bg-secondary/70 hover:text-foreground"
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
                class="w-full justify-center"
                size="sm"
                @click="updateApprovalStatus(user, 'APPROVED')"
              >
                <UserRoundCheck class="h-4 w-4" />
                Approve
              </Button>
              <Button
                :disabled="approvalMutation.isPending.value"
                class="w-full justify-center"
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
              class="w-full justify-center"
              size="sm"
              :variant="user.isAdmin ? 'outline' : 'default'"
              @click="toggleAdminAccess(user)"
            >
              <ShieldOff v-if="user.isAdmin" class="h-4 w-4" />
              <Shield v-else class="h-4 w-4" />
              {{ user.isAdmin ? 'Remove admin' : 'Grant admin' }}
            </Button>

            <div
              v-else
              class="rounded-2xl border border-dashed border-rose-200 bg-rose-50/70 px-4 py-3 text-sm text-rose-700"
            >
              This registration was denied. A new signup with the same email will reopen it.
            </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
