<script setup lang="ts">
import { RouterView } from 'vue-router'

import AdminCreateActivityFab from '@/components/admin/AdminCreateActivityFab.vue'
import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <div class="relative min-h-screen overflow-x-hidden">
    <div
      class="pointer-events-none absolute inset-x-0 top-[-22rem] h-[52rem] bg-[radial-gradient(circle_at_top,rgba(26,86,219,0.2),rgba(26,86,219,0.08)_34%,transparent_72%)]"
    />
    <div
      class="pointer-events-none absolute right-[-10rem] top-40 h-72 w-72 rounded-full bg-[radial-gradient(circle,rgba(245,158,11,0.22),transparent_70%)] blur-3xl"
    />

    <AppHeader />
    <AdminCreateActivityFab />

    <main
      class="relative mx-auto flex min-h-[calc(100vh-5rem)] w-full max-w-7xl flex-col px-4 pb-16 pt-8 sm:px-6 lg:px-8 lg:pt-10"
    >
      <RouterView />
    </main>
  </div>
</template>
