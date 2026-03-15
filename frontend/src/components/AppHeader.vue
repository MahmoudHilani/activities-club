<script setup lang="ts">
import { LogOut, Sparkles } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { buttonVariants, Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const sessionStore = useSessionStore()

const currentUserLabel = computed(() =>
  sessionStore.user ? `Signed in as ${sessionStore.user.username}` : 'Member access',
)
const isAdmin = computed(() => sessionStore.user?.role === 'ADMIN')

function navClass(isActive: boolean): string {
  return cn(
    'rounded-full px-4 py-2 text-sm font-semibold transition-colors',
    isActive ? 'bg-white/80 text-foreground shadow-sm' : 'text-muted-foreground hover:bg-white/65',
  )
}
</script>

<template>
  <header class="relative z-10 border-b border-white/55 bg-white/45 backdrop-blur-xl">
    <div class="mx-auto flex h-20 w-full max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
      <div class="flex items-center gap-3">
        <RouterLink class="flex items-center gap-3" :to="{ name: 'activities' }">
          <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-primary text-primary-foreground shadow-[0_12px_26px_rgba(38,70,173,0.28)]">
            <Sparkles class="h-5 w-5" />
          </div>
          <div>
            <p class="font-serif text-xl font-bold tracking-tight">Activities Club</p>
            <p class="text-xs font-medium uppercase tracking-[0.24em] text-muted-foreground">
              Campus moments
            </p>
          </div>
        </RouterLink>
      </div>

      <div class="hidden items-center gap-2 md:flex">
        <RouterLink :class="navClass(route.name === 'activities')" :to="{ name: 'activities' }">
          Activities
        </RouterLink>
        <RouterLink
          v-if="isAdmin"
          :class="navClass(route.name === 'admin-activities')"
          :to="{ name: 'admin-activities' }"
        >
          Admin
        </RouterLink>
      </div>

      <div class="flex items-center gap-3">
        <p class="hidden text-sm text-muted-foreground lg:block">{{ currentUserLabel }}</p>

        <template v-if="sessionStore.isAuthenticated">
          <Button size="sm" variant="outline" @click="sessionStore.logout">
            <LogOut class="h-4 w-4" />
            Logout
          </Button>
        </template>

        <template v-else>
          <RouterLink
            :class="buttonVariants({ variant: route.name === 'auth' ? 'secondary' : 'default', size: 'sm' })"
            :to="{ name: 'auth' }"
          >
            Login / Register
          </RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>
