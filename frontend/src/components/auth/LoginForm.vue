<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { LoaderCircle } from 'lucide-vue-next'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { mapLoginError } from '@/lib/api/errors'
import { resolveRedirectPath } from '@/lib/redirect'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const serverError = ref('')

const loginSchema = toTypedSchema(
  z.object({
    email: z
      .string()
      .trim()
      .min(1, 'Email is required')
      .max(120, 'Email must be 120 characters or less')
      .email('Enter a valid email address'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(72, 'Password must be 72 characters or less'),
  }),
)

const { defineField, errors, handleSubmit, isSubmitting } = useForm({
  validationSchema: loginSchema,
})

const [email, emailAttrs] = defineField('email')
const [password, passwordAttrs] = defineField('password')

const onSubmit = handleSubmit(async (values) => {
  serverError.value = ''

  try {
    const destination = await sessionStore.login(values)
    if (destination === 'appeal') {
      await router.push({ name: 'registration-appeal' })
      return
    }

    await router.push(resolveRedirectPath(route.query.redirect))
  } catch (error) {
    serverError.value = mapLoginError(error)
  }
})
</script>

<template>
  <form class="auth-form" novalidate @submit.prevent="onSubmit">
    <Alert v-if="serverError" variant="destructive">
      {{ serverError }}
    </Alert>

    <div class="field">
      <label class="field-label" for="login-email">
        <span class="field-label-text">Email</span>
        <span class="field-label-hint">the one you signed up with</span>
      </label>
      <Input
        id="login-email"
        v-model="email"
        v-bind="emailAttrs"
        autocomplete="email"
        placeholder="you@campus.edu"
        type="email"
      />
      <p v-if="errors.email" class="field-error">{{ errors.email }}</p>
    </div>

    <div class="field">
      <label class="field-label" for="login-password">
        <span class="field-label-text">Password</span>
        <span class="field-label-hint">eight characters or more</span>
      </label>
      <Input
        id="login-password"
        v-model="password"
        v-bind="passwordAttrs"
        autocomplete="current-password"
        placeholder="••••••••"
        type="password"
      />
      <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
    </div>

    <Button class="submit-btn" :disabled="isSubmitting" size="lg" type="submit">
      <LoaderCircle v-if="isSubmitting" class="h-4 w-4 animate-spin" />
      Sign in
    </Button>
  </form>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-label {
  display: flex;
  align-items: baseline;
  gap: 10px;
  justify-content: space-between;
}
.field-label-text {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 22px;
  line-height: 1;
  color: var(--primary);
}
.field-label-hint {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 16px;
  color: var(--color-coral);
}
.field-error {
  font-size: 13px;
  color: var(--color-coral);
  font-weight: 600;
  margin: 0;
}
.submit-btn {
  margin-top: 6px;
  width: 100%;
}
</style>
