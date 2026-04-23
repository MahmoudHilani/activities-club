<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { LoaderCircle } from 'lucide-vue-next'
import { useForm } from 'vee-validate'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { mapRegisterError } from '@/lib/api/errors'
import { useSessionStore } from '@/stores/session'

const userTypeOptions = [
  { label: 'Student', value: 'STUDENT' },
  { label: 'Staff', value: 'STAFF' },
] as const

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const serverError = ref('')
const submittedMessage = ref('')

const registerSchema = toTypedSchema(
  z
    .object({
      username: z
        .string()
        .trim()
        .min(1, 'Name and surname is required')
        .max(30, 'Name and surname must be 30 characters or less'),
      email: z
        .string()
        .trim()
        .min(1, 'Email is required')
        .max(120, 'Email must be 120 characters or less')
        .email('Enter a valid email address'),
      userType: z.enum(['STUDENT', 'STAFF'], {
        message: 'Choose whether you are registering as a student or staff member',
      }),
      studentNumber: z
        .string()
        .trim()
        .max(30, 'Student number must be 30 characters or less'),
      phoneNumber: z
        .string()
        .trim()
        .max(30, 'Phone number must be 30 characters or less'),
      password: z
        .string()
        .min(8, 'Password must be at least 8 characters')
        .max(72, 'Password must be 72 characters or less'),
      confirmPassword: z.string().min(1, 'Confirm your password'),
    })
    .superRefine((values, context) => {
      if (values.password !== values.confirmPassword) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['confirmPassword'],
          message: 'Passwords must match',
        })
      }

      if (values.userType === 'STUDENT') {
        if (!values.studentNumber.trim()) {
          context.addIssue({
            code: z.ZodIssueCode.custom,
            path: ['studentNumber'],
            message: 'Student number is required',
          })
        }

        if (!values.phoneNumber.trim()) {
          context.addIssue({
            code: z.ZodIssueCode.custom,
            path: ['phoneNumber'],
            message: 'Phone number is required',
          })
        }
      }
    }),
)

const { defineField, errors, handleSubmit, isSubmitting, resetForm } = useForm({
  validationSchema: registerSchema,
  initialValues: {
    username: '',
    email: '',
    userType: 'STUDENT',
    studentNumber: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
  },
})

const [username, usernameAttrs] = defineField('username')
const [email, emailAttrs] = defineField('email')
const [userType] = defineField('userType')
const [studentNumber, studentNumberAttrs] = defineField('studentNumber')
const [phoneNumber, phoneNumberAttrs] = defineField('phoneNumber')
const [password, passwordAttrs] = defineField('password')
const [confirmPassword, confirmPasswordAttrs] = defineField('confirmPassword')
const isStudent = computed(() => userType.value === 'STUDENT')

watch(userType, (value) => {
  if (value === 'STAFF') {
    studentNumber.value = ''
    phoneNumber.value = ''
  }
})

async function goToLogin(): Promise<void> {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : undefined

  await router.replace({
    name: 'auth',
    query: redirect ? { redirect } : {},
  })
}

const onSubmit = handleSubmit(async (values) => {
  serverError.value = ''

  try {
    const response = await sessionStore.register({
      username: values.username,
      email: values.email,
      userType: values.userType,
      studentNumber: values.userType === 'STUDENT' ? values.studentNumber.trim() : null,
      phoneNumber: values.userType === 'STUDENT' ? values.phoneNumber.trim() : null,
      password: values.password,
    })
    submittedMessage.value = response.message
    resetForm()
  } catch (error) {
    serverError.value = mapRegisterError(error)
  }
})
</script>

<template>
  <div v-if="submittedMessage" class="space-y-5 rounded-[1.85rem] border border-white/70 bg-white/65 p-6">
    <div class="space-y-2">
      <h2 class="text-xl font-bold text-foreground">Awaiting approval</h2>
      <p class="text-sm text-muted-foreground">
        {{ submittedMessage }}
      </p>
    </div>
    <Button class="w-full" type="button" variant="outline" @click="goToLogin">
      Back to login
    </Button>
  </div>

  <form v-else class="space-y-5" novalidate @submit.prevent="onSubmit">
    <Alert v-if="serverError" variant="destructive">
      {{ serverError }}
    </Alert>

    <div class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-user-type">User type</label>
      <Select v-model="userType">
        <SelectTrigger id="register-user-type" aria-label="User type">
          <SelectValue placeholder="Select your user type" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem v-for="option in userTypeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </SelectItem>
        </SelectContent>
      </Select>
      <p v-if="errors.userType" class="text-sm text-destructive">{{ errors.userType }}</p>
    </div>

    <div class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-username">Name and surname</label>
      <Input
        id="register-username"
        v-model="username"
        v-bind="usernameAttrs"
        autocomplete="username"
        placeholder="Enter your name and surname"
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

    <div v-if="isStudent" class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-student-number">
        Student number
      </label>
      <Input
        id="register-student-number"
        v-model="studentNumber"
        v-bind="studentNumberAttrs"
        autocomplete="off"
        placeholder="Enter your student number"
      />
      <p v-if="errors.studentNumber" class="text-sm text-destructive">
        {{ errors.studentNumber }}
      </p>
    </div>

    <div v-if="isStudent" class="space-y-2.5">
      <label class="text-sm font-semibold text-foreground" for="register-phone-number">
        Phone number
      </label>
      <Input
        id="register-phone-number"
        v-model="phoneNumber"
        v-bind="phoneNumberAttrs"
        autocomplete="tel"
        placeholder="Enter your phone number"
        type="tel"
      />
      <p v-if="errors.phoneNumber" class="text-sm text-destructive">{{ errors.phoneNumber }}</p>
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

    <Button class="mt-1 w-full" :disabled="isSubmitting" size="lg" type="submit">
      <LoaderCircle v-if="isSubmitting" class="h-4 w-4 animate-spin" />
      Create account
    </Button>
  </form>
</template>
