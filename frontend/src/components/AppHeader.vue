<script setup lang="ts">
import { LogOut } from 'lucide-vue-next'
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { useSessionStore } from '@/stores/session'

const route = useRoute()
const sessionStore = useSessionStore()

const currentUserLabel = computed(() =>
  sessionStore.user ? `Signed in as ${sessionStore.user.username}` : '',
)
const isAdmin = computed(() => sessionStore.user?.isAdmin === true)
const isAdminActivitiesRoute = computed(() => route.path.startsWith('/admin/activities'))
const isAdminUsersRoute = computed(() => route.path.startsWith('/admin/users'))
</script>

<template>
  <header class="app-header">
    <div
      class="mx-auto flex h-20 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8"
    >
      <RouterLink class="brand" :to="{ name: 'home' }">
        <span class="brand-mark" aria-hidden="true">g</span>
        <span class="brand-text">
          <span class="brand-name">Activities Club</span>
          <span class="brand-tagline">sport · adventure · wellness ✿</span>
        </span>
      </RouterLink>

      <nav class="hidden items-center gap-1 md:flex" aria-label="Primary">
        <RouterLink
          v-if="isAdmin"
          class="nav-link"
          :class="{ 'is-active': isAdminActivitiesRoute }"
          :to="{ name: 'admin-activities' }"
        >
          Activities
        </RouterLink>
        <RouterLink
          v-if="isAdmin"
          class="nav-link"
          :class="{ 'is-active': isAdminUsersRoute }"
          :to="{ name: 'admin-users' }"
        >
          Users
        </RouterLink>
      </nav>

      <div class="flex items-center gap-3">
        <p
          v-if="sessionStore.isAuthenticated"
          class="hidden text-sm text-muted-foreground lg:block"
        >
          {{ currentUserLabel }}
        </p>

        <template v-if="sessionStore.isAuthenticated">
          <button type="button" class="btn-ghost" @click="sessionStore.logout">
            <LogOut class="h-4 w-4" />
            Logout
          </button>
        </template>

        <template v-else>
          <RouterLink
            v-if="route.name !== 'auth'"
            class="btn-primary"
            :to="{ name: 'auth' }"
          >
            Login / Register
          </RouterLink>
          <span v-else class="btn-current">Signing in…</span>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: relative;
  z-index: 10;
  border-bottom: 1px solid color-mix(in srgb, var(--primary) 14%, transparent);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(16px);
}

/* Brand */
.brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  text-decoration: none;
  color: var(--primary);
}
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--primary-foreground);
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1;
  letter-spacing: -0.02em;
  padding-top: 2px;
  box-shadow:
    3px 3px 0 var(--color-coral),
    6px 6px 0 var(--color-ochre);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.brand:hover .brand-mark {
  transform: translate(-2px, -2px);
  box-shadow:
    5px 5px 0 var(--color-coral),
    8px 8px 0 var(--color-ochre);
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1;
}
.brand-name {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 22px;
  letter-spacing: -0.005em;
  color: var(--primary);
}
.brand-tagline {
  margin-top: 4px;
  font-family: var(--font-hand);
  font-weight: 500;
  font-size: 16px;
  line-height: 1;
  color: var(--color-coral);
}

/* Nav */
.nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 10px 14px 14px;
  border-radius: 999px;
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  line-height: 1;
  transition: background 0.15s ease;
}
.nav-link:hover {
  background: color-mix(in srgb, var(--primary) 8%, transparent);
}
.nav-link.is-active {
  color: var(--primary);
}
.nav-link.is-active::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 4px;
  height: 8px;
  background: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 8' width='80' height='8'><path d='M2 5 Q 14 1, 28 5 T 54 5 T 78 5' stroke='%23ff7a59' stroke-width='2.5' fill='none' stroke-linecap='round'/></svg>")
    repeat-x;
  background-size: 80px 8px;
}

/* Buttons */
.btn-primary {
  display: inline-block;
  background: var(--primary);
  color: var(--primary-foreground);
  font-weight: 700;
  font-size: 14px;
  padding: 11px 22px;
  border-radius: 999px;
  text-decoration: none;
  line-height: 1.2;
  box-shadow:
    3px 3px 0 var(--color-coral),
    6px 6px 0 var(--color-ochre);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.btn-primary:hover {
  transform: translate(-2px, -2px);
  box-shadow:
    5px 5px 0 var(--color-coral),
    8px 8px 0 var(--color-ochre);
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 1.5px solid var(--primary);
  color: var(--primary);
  font-weight: 600;
  font-size: 13px;
  padding: 9px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition:
    background 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.btn-ghost:hover {
  background: var(--primary);
  color: var(--primary-foreground);
  transform: translate(-1px, -1px);
  box-shadow: 3px 3px 0 var(--color-coral);
}

.btn-current {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-coral);
  padding: 0 6px;
}
</style>
