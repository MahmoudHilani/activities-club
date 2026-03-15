<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { LoaderCircle } from 'lucide-vue-next'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { mapLoginError } from '@/lib/api/errors'
import { useSessionStore } from '@/stores/session'

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
    await sessionStore.login(values)
    await router.push({ name: 'activities' })
  } catch (error) {
    serverError.value = mapLoginError(error)
  }
})
</script>

<template>
  <form class="space-y-4" novalidate @submit.prevent="onSubmit">
    <Alert v-if="serverError" variant="destructive">
      {{ serverError }}
    </Alert>

    <div class="space-y-2">
      <label class="text-sm font-semibold text-foreground" for="login-email">Email</label>
      <Input
        id="login-email"
        v-model="email"
        v-bind="emailAttrs"
        autocomplete="email"
        placeholder="you@campus.edu"
        type="email"
      />
      <p v-if="errors.email" class="text-sm text-destructive">{{ errors.email }}</p>
    </div>

    <div class="space-y-2">
      <label class="text-sm font-semibold text-foreground" for="login-password">Password</label>
      <Input
        id="login-password"
        v-model="password"
        v-bind="passwordAttrs"
        autocomplete="current-password"
        placeholder="Enter your password"
        type="password"
      />
      <p v-if="errors.password" class="text-sm text-destructive">{{ errors.password }}</p>
    </div>

    <Button class="w-full" :disabled="isSubmitting" size="lg" type="submit">
      <LoaderCircle v-if="isSubmitting" class="h-4 w-4 animate-spin" />
      Sign in
    </Button>
  </form>
</template>
