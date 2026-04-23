<script setup lang="ts">
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { Shield, ShieldOff, UserRoundX, UserRoundCheck } from 'lucide-vue-next'
import { computed, ref } from 'vue'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { getApiMessage, mapUsersError } from '@/lib/api/errors'
import { getAdminUsers, updateAdminUserAccess, updateUserApprovalStatus } from '@/lib/api/users'
import type { ApprovalStatus, UserResponse } from '@/lib/api/types'
import { useSessionStore } from '@/stores/session'

const queryClient = useQueryClient()
const sessionStore = useSessionStore()
const actionError = ref('')

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
  }: {
    userId: number
    approvalStatus: Exclude<ApprovalStatus, 'PENDING'>
  }) => updateUserApprovalStatus(userId, approvalStatus),
  onSuccess: async (updatedUser) => {
    actionError.value = ''
    replaceCachedUser(updatedUser)
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] })
  },
  onError: (error) => {
    actionError.value = getApiMessage(error) ?? 'We could not update registration approval right now.'
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
  approvalMutation.mutate({
    userId: user.id,
    approvalStatus,
  })
}

function contactValue(value: string | null): string {
  return value ?? 'Not provided'
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
            </div>
          </div>

          <div class="flex w-full flex-col gap-2 xl:min-w-[14rem] xl:max-w-[14rem]">
            <template v-if="user.approvalStatus === 'PENDING'">
              <Button
                :disabled="approvalMutation.isPending.value"
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
