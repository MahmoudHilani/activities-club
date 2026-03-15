<script setup lang="ts">
import { CheckCircle2, ShieldCheck, Ticket } from 'lucide-vue-next'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import LoginForm from '@/components/auth/LoginForm.vue'
import RegisterForm from '@/components/auth/RegisterForm.vue'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

type AuthMode = 'login' | 'register'

const route = useRoute()
const router = useRouter()

function resolveMode(value: unknown): AuthMode {
  return value === 'register' ? 'register' : 'login'
}

const currentTab = ref<AuthMode>(resolveMode(route.query.mode))

watch(
  () => route.query.mode,
  (value) => {
    currentTab.value = resolveMode(value)
  },
)

watch(currentTab, async (value) => {
  if (route.query.mode === value) {
    return
  }

  await router.replace({
    name: 'auth',
    query: value === 'login' ? {} : { mode: value },
  })
})
</script>

<template>
  <section class="grid flex-1 items-center gap-8 lg:grid-cols-[1.05fr_0.95fr]">
    <div class="space-y-8">
      <div class="max-w-2xl space-y-5">
        <p class="inline-flex rounded-full bg-white/80 px-4 py-2 text-xs font-semibold uppercase tracking-[0.25em] text-muted-foreground">
          Join the club
        </p>
        <h1 class="headline-balance max-w-xl font-serif text-5xl font-bold tracking-tight text-foreground sm:text-6xl">
          Find the next thing worth showing up for.
        </h1>
        <p class="max-w-2xl text-lg leading-8 text-muted-foreground">
          Sign in to keep your campus identity in one place, then move straight into the public activity feed.
        </p>
      </div>

      <div class="grid gap-4 sm:grid-cols-3">
        <div class="surface-panel rounded-[1.5rem] border border-white/65 p-5">
          <ShieldCheck class="h-5 w-5 text-primary" />
          <h2 class="mt-4 text-base font-bold text-foreground">Secure access</h2>
          <p class="mt-2 text-sm text-muted-foreground">
            JWT-backed sessions with auto-restore on refresh.
          </p>
        </div>
        <div class="surface-panel rounded-[1.5rem] border border-white/65 p-5">
          <Ticket class="h-5 w-5 text-primary" />
          <h2 class="mt-4 text-base font-bold text-foreground">Activity-first</h2>
          <p class="mt-2 text-sm text-muted-foreground">
            Land back on the public feed the moment auth succeeds.
          </p>
        </div>
        <div class="surface-panel rounded-[1.5rem] border border-white/65 p-5">
          <CheckCircle2 class="h-5 w-5 text-primary" />
          <h2 class="mt-4 text-base font-bold text-foreground">Ready for v1</h2>
          <p class="mt-2 text-sm text-muted-foreground">
            Clean validation, clear errors, no hidden flows.
          </p>
        </div>
      </div>
    </div>

    <div class="surface-panel rounded-[2rem] border border-white/70 p-6 shadow-[0_30px_60px_rgba(31,41,55,0.1)] sm:p-8">
      <div class="mb-6">
        <p class="text-sm font-semibold uppercase tracking-[0.2em] text-muted-foreground">
          Member access
        </p>
        <h2 class="mt-2 text-3xl font-bold tracking-tight text-foreground">
          Login or create an account
        </h2>
      </div>

      <Tabs v-model="currentTab" class="w-full">
        <TabsList>
          <TabsTrigger value="login">Login</TabsTrigger>
          <TabsTrigger value="register">Register</TabsTrigger>
        </TabsList>

        <TabsContent value="login">
          <LoginForm />
        </TabsContent>

        <TabsContent value="register">
          <RegisterForm />
        </TabsContent>
      </Tabs>
    </div>
  </section>
</template>
