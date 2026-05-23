<script setup lang="ts">
import { Plus } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const sessionStore = useSessionStore()

const isAdmin = computed(() => sessionStore.user?.isAdmin === true)
const isCreateRoute = computed(() => route.name === 'admin-activity-create')
const isActivityDetailRoute = computed(() => route.name === 'activity-detail')
const shouldShowFab = computed(() => isAdmin.value && !isActivityDetailRoute.value)
</script>

<template>
  <RouterLink
    v-if="shouldShowFab"
    aria-label="Create activity"
    :aria-current="isCreateRoute ? 'page' : undefined"
    :class="
      cn(
        buttonVariants({ size: 'lg' }),
        'fixed bottom-6 right-4 z-20 h-16 w-16 rounded-full p-0 sm:bottom-8 sm:right-8 sm:h-16 sm:w-auto sm:px-6 sm:gap-3',
        isCreateRoute && '!bg-[var(--color-coral)] !text-white',
      )
    "
    :to="{ name: 'admin-activity-create' }"
  >
    <Plus class="h-6 w-6 shrink-0" />
    <span class="hidden font-bold tracking-tight sm:inline">New activity</span>
  </RouterLink>
</template>
