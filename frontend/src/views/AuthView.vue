<script setup lang="ts">
import { computed } from 'vue'
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

function buildAuthQuery(mode: AuthMode, redirect: string) {
  return {
    ...(mode === 'login' ? {} : { mode }),
    ...(redirect ? { redirect } : {}),
  }
}

const currentTab = computed<AuthMode>({
  get: () => resolveMode(route.query.mode),
  set: (value) => {
    if (value === resolveMode(route.query.mode)) {
      return
    }

    const redirectPath = resolveRedirectPath(route.query.redirect, '')

    void router.replace({
      name: 'auth',
      query: buildAuthQuery(value, redirectPath),
    })
  },
})
</script>

<template>
  <section class="flex flex-1 items-center justify-center py-6 sm:py-10">
    <div class="auth-shell w-full max-w-lg">
      <div class="text-center">
        <h1 class="headline-balance font-serif text-4xl font-bold tracking-tight text-foreground">
          Login or create an account
        </h1>
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
