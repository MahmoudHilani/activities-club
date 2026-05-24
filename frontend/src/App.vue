<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import AdminCreateActivityFab from '@/components/admin/AdminCreateActivityFab.vue'
import AppFooter from '@/components/AppFooter.vue'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const showFooter = computed(() => !route.path.startsWith('/admin'))
</script>

<template>
  <div class="relative flex min-h-screen flex-col overflow-x-hidden">
    <div
      class="pointer-events-none absolute inset-x-0 top-[-22rem] h-[52rem] bg-[radial-gradient(circle_at_top,rgba(26,86,219,0.2),rgba(26,86,219,0.08)_34%,transparent_72%)]"
    />
    <div
      class="pointer-events-none absolute right-[-10rem] top-40 h-72 w-72 rounded-full bg-[radial-gradient(circle,rgba(245,158,11,0.22),transparent_70%)] blur-3xl"
    />

    <AppHeader />
    <AdminCreateActivityFab />

    <main
      class="relative mx-auto flex w-full max-w-7xl flex-1 flex-col px-4 pb-16 pt-8 sm:px-6 lg:px-8 lg:pt-10"
    >
      <RouterView />
    </main>

    <AppFooter v-if="showFooter" />
  </div>
</template>
