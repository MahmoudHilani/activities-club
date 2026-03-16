<script setup lang="ts">
import { Plus } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const sessionStore = useSessionStore()

const isAdmin = computed(() => sessionStore.user?.role === 'ADMIN')
const isCreateRoute = computed(() => route.name === 'admin-activity-create')
</script>

<template>
  <RouterLink
    v-if="isAdmin"
    aria-label="Create activity"
    :aria-current="isCreateRoute ? 'page' : undefined"
    :class="
      cn(
        buttonVariants({ size: 'lg' }),
        'fixed bottom-6 right-4 z-20 h-14 w-14 rounded-full p-0 shadow-[0_18px_40px_rgba(38,70,173,0.28)] sm:bottom-8 sm:right-8 sm:h-14 sm:w-auto sm:px-5',
        isCreateRoute && 'bg-accent text-accent-foreground hover:translate-y-0 hover:brightness-100',
      )
    "
    :to="{ name: 'admin-activity-create' }"
  >
    <Plus class="h-5 w-5 shrink-0" />
    <span class="hidden sm:inline">New activity</span>
  </RouterLink>
</template>
