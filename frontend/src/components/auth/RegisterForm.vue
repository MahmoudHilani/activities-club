<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { Check, LoaderCircle } from 'lucide-vue-next'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { mapRegisterError } from '@/lib/api/errors'
import { resolveRedirectPath } from '@/lib/redirect'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const serverError = ref('')

const registerSchema = toTypedSchema(
  z
    .object({
      username: z
        .string()
        .trim()
        .min(1, 'Username is required')
        .max(30, 'Username must be 30 characters or less'),
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
      confirmPassword: z.string().min(1, 'Confirm your password'),
      isAdmin: z.boolean(),
    })
    .refine((values) => values.password === values.confirmPassword, {
      path: ['confirmPassword'],
      message: 'Passwords must match',
    }),
)

const { defineField, errors, handleSubmit, isSubmitting } = useForm({
  validationSchema: registerSchema,
  initialValues: {
    isAdmin: false,
  },
})

const [username, usernameAttrs] = defineField('username')
const [email, emailAttrs] = defineField('email')
const [password, passwordAttrs] = defineField('password')
const [confirmPassword, confirmPasswordAttrs] = defineField('confirmPassword')
const [isAdmin, isAdminAttrs] = defineField('isAdmin')

const onSubmit = handleSubmit(async (values) => {
  serverError.value = ''

  try {
    await sessionStore.register({
      username: values.username,
      email: values.email,
      password: values.password,
      isAdmin: values.isAdmin,
    })
    await router.push(resolveRedirectPath(route.query.redirect))
  } catch (error) {
    serverError.value = mapRegisterError(error)
  }
})
</script>

<template>
  <form class="space-y-5" novalidate @submit.prevent="onSubmit">
    <Alert v-if="serverError" variant="destructive">
      {{ serverError }}
    </Alert>

    <div class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-username">Username</label>
      <Input
        id="register-username"
        v-model="username"
        v-bind="usernameAttrs"
        autocomplete="username"
        placeholder="Pick a display name"
      />
      <p v-if="errors.username" class="text-sm text-destructive">{{ errors.username }}</p>
    </div>

    <div class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-email">Email</label>
      <Input
        id="register-email"
        v-model="email"
        v-bind="emailAttrs"
        autocomplete="email"
        placeholder="you@campus.edu"
        type="email"
      />
      <p v-if="errors.email" class="text-sm text-destructive">{{ errors.email }}</p>
    </div>

    <div class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-password">Password</label>
      <Input
        id="register-password"
        v-model="password"
        v-bind="passwordAttrs"
        autocomplete="new-password"
        placeholder="Use at least 8 characters"
        type="password"
      />
      <p class="text-xs text-muted-foreground">Use 8 to 72 characters.</p>
      <p v-if="errors.password" class="text-sm text-destructive">{{ errors.password }}</p>
    </div>

    <div class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-confirm-password">
        Confirm password
      </label>
      <Input
        id="register-confirm-password"
        v-model="confirmPassword"
        v-bind="confirmPasswordAttrs"
        autocomplete="new-password"
        placeholder="Repeat your password"
        type="password"
      />
      <p v-if="errors.confirmPassword" class="text-sm text-destructive">
        {{ errors.confirmPassword }}
      </p>
    </div>

    <label class="auth-checkbox" for="register-is-admin">
      <input
        id="register-is-admin"
        v-model="isAdmin"
        v-bind="isAdminAttrs"
        class="peer sr-only"
        type="checkbox"
      />
      <span
        class="flex h-5 w-5 shrink-0 items-center justify-center rounded-md border border-border bg-white text-primary-foreground transition-all peer-checked:border-primary peer-checked:bg-primary peer-focus-visible:ring-4 peer-focus-visible:ring-ring"
      >
        <Check class="h-3.5 w-3.5 opacity-0 transition peer-checked:opacity-100" />
      </span>
      <span class="min-w-0">
        <span class="block text-sm font-semibold text-foreground">Register as admin</span>
        <span class="block text-xs text-muted-foreground">
          Enable management access for this account.
        </span>
      </span>
    </label>

    <Button class="mt-1 w-full" :disabled="isSubmitting" size="lg" type="submit">
      <LoaderCircle v-if="isSubmitting" class="h-4 w-4 animate-spin" />
      Create account
    </Button>
  </form>
</template>
