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

const headlineCopy = computed(() =>
  currentTab.value === 'register'
    ? { display: 'Hop in, the', hand: ' kettle is on' }
    : { display: 'Welcome', hand: ' back ✿' },
)
const subCopy = computed(() =>
  currentTab.value === 'register'
    ? 'Sign up once, then you can reserve spots, jump on waitlists and join the WhatsApp group.'
    : "Sign in to reserve, manage your spots, and see what the gang is up to this week.",
)
</script>

<template>
  <section class="auth-page">
    <div class="auth-decor auth-decor-left" aria-hidden="true">
      <div class="auth-decor-blob auth-decor-blob-coral" />
      <div class="auth-decor-sticker auth-decor-sticker-coral">drop in ✿</div>
    </div>
    <div class="auth-decor auth-decor-right" aria-hidden="true">
      <div class="auth-decor-blob auth-decor-blob-ochre" />
      <div class="auth-decor-sticker auth-decor-sticker-ochre">no fitness req.</div>
    </div>

    <div class="auth-card">
      <h1 class="auth-headline">
        <span class="display-text">{{ headlineCopy.display }}</span>
        <span class="hand-text">{{ headlineCopy.hand }}</span>
      </h1>

      <p class="auth-sub">{{ subCopy }}</p>

      <Tabs v-model="currentTab" class="auth-tabs">
        <TabsList>
          <TabsTrigger value="login">Login</TabsTrigger>
          <TabsTrigger value="register">Register</TabsTrigger>
        </TabsList>

        <TabsContent value="login" class="auth-tab-body">
          <LoginForm />
        </TabsContent>

        <TabsContent value="register" class="auth-tab-body">
          <RegisterForm />
        </TabsContent>
      </Tabs>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  position: relative;
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 1.5rem 0 4rem;
}

.auth-decor {
  position: absolute;
  width: 220px;
  pointer-events: none;
  z-index: 0;
}
.auth-decor-left {
  top: 8%;
  left: -1rem;
  transform: rotate(-8deg);
}
.auth-decor-right {
  bottom: 10%;
  right: -1rem;
  transform: rotate(6deg);
}
.auth-decor-blob {
  width: 100%;
  height: 220px;
  opacity: 0.45;
  clip-path: polygon(22% 8%, 78% 5%, 92% 28%, 95% 72%, 84% 92%, 28% 95%, 8% 78%, 5% 32%);
}
.auth-decor-blob-coral {
  background: var(--color-coral);
}
.auth-decor-blob-ochre {
  background: var(--color-ochre);
}
.auth-decor-sticker {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-3deg);
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 22px;
  padding: 6px 18px;
  border-radius: 999px;
  box-shadow: 2px 2px 0 var(--primary);
}
.auth-decor-sticker-coral {
  background: var(--color-coral);
  color: white;
}
.auth-decor-sticker-ochre {
  background: var(--color-ochre);
  color: var(--primary);
}

@media (max-width: 900px) {
  .auth-decor {
    display: none;
  }
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 34rem;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 32px;
  padding: 40px 36px 32px;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-headline {
  margin: 14px 0 8px;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(36px, 5vw, 56px);
  line-height: 0.98;
  color: var(--primary);
  letter-spacing: -0.01em;
}
.auth-headline .hand-text {
  font-size: 1.12em;
}

.auth-sub {
  margin: 0 0 22px;
  font-size: 15.5px;
  color: var(--muted-foreground);
  line-height: 1.55;
}

.auth-tabs {
  margin-top: 4px;
}
.auth-tab-body {
  margin-top: 22px;
}
</style>
