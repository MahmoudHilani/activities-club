<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import LoginForm from '@/components/auth/LoginForm.vue'
import RegisterForm from '@/components/auth/RegisterForm.vue'
import { resolveRedirectPath } from '@/lib/redirect'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

type AuthMode = 'login' | 'register'

const route = useRoute()
const router = useRouter()

function resolveMode(value: unknown): AuthMode {
  return value === 'register' ? 'register' : 'login'
}

const currentTab = ref<AuthMode>(resolveMode(route.query.mode))
const redirectPath = computed(() => resolveRedirectPath(route.query.redirect, ''))

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
    query: {
      ...(value === 'login' ? {} : { mode: value }),
      ...(redirectPath.value ? { redirect: redirectPath.value } : {}),
    },
  })
})
</script>

<template>
  <section class="flex flex-1 items-center justify-center py-6 sm:py-10">
    <div class="auth-shell w-full max-w-lg">
      <div class="space-y-3 text-center">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-muted-foreground">
          Member access
        </p>
        <h1 class="headline-balance font-serif text-4xl font-bold tracking-tight text-foreground">
          Login or create an account
        </h1>
        <p class="mx-auto max-w-md text-sm leading-6 text-muted-foreground sm:text-base">
          Sign in to continue with Activities Club or create an account to get started.
        </p>
        <p v-if="redirectPath" class="auth-note">
          You&apos;ll return to your selected page after signing in.
        </p>
      </div>

      <Tabs v-model="currentTab" class="mt-8 w-full">
        <TabsList class="h-11 rounded-2xl bg-muted/80">
          <TabsTrigger value="login">Login</TabsTrigger>
          <TabsTrigger value="register">Register</TabsTrigger>
        </TabsList>

        <TabsContent value="login" class="mt-6">
          <LoginForm />
        </TabsContent>

        <TabsContent value="register" class="mt-6">
          <RegisterForm />
        </TabsContent>
      </Tabs>
    </div>
  </section>
</template>
