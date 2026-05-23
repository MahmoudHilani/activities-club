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
  <div v-if="submittedMessage" class="awaiting-card">
    <span class="craft-tag craft-tag-ochre awaiting-stamp">on the list</span>
    <h2 class="awaiting-title">
      <span class="display-text">Awaiting</span>
      <span class="hand-text"> approval</span>
    </h2>
    <p class="awaiting-body">{{ submittedMessage }}</p>
    <Button class="awaiting-back" type="button" variant="outline" @click="goToLogin">
      Back to login
    </Button>
  </div>

  <form v-else class="auth-form" novalidate @submit.prevent="onSubmit">
    <Alert v-if="serverError" variant="destructive">
      {{ serverError }}
    </Alert>

    <div class="field">
      <label class="field-label" for="register-user-type">
        <span class="field-label-text">I'm a…</span>
        <span class="field-label-hint">student or staff</span>
      </label>
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
      <p v-if="errors.userType" class="field-error">{{ errors.userType }}</p>
    </div>

    <div class="field">
      <label class="field-label" for="register-username">
        <span class="field-label-text">Name and surname</span>
        <span class="field-label-hint">the one on your card</span>
      </label>
      <Input
        id="register-username"
        v-model="username"
        v-bind="usernameAttrs"
        autocomplete="username"
        placeholder="e.g. Niamh Murphy"
      />
      <p v-if="errors.username" class="field-error">{{ errors.username }}</p>
    </div>

    <div class="field">
      <label class="field-label" for="register-email">
        <span class="field-label-text">Email</span>
        <span class="field-label-hint">no spam, promise</span>
      </label>
      <Input
        id="register-email"
        v-model="email"
        v-bind="emailAttrs"
        autocomplete="email"
        placeholder="you@campus.edu"
        type="email"
      />
      <p v-if="errors.email" class="field-error">{{ errors.email }}</p>
    </div>

    <div v-if="isStudent" class="field">
      <label class="field-label" for="register-student-number">
        <span class="field-label-text">Student number</span>
        <span class="field-label-hint">on your ID card</span>
      </label>
      <Input
        id="register-student-number"
        v-model="studentNumber"
        v-bind="studentNumberAttrs"
        autocomplete="off"
        placeholder="e.g. 3209412"
      />
      <p v-if="errors.studentNumber" class="field-error">{{ errors.studentNumber }}</p>
    </div>

    <div v-if="isStudent" class="field">
      <label class="field-label" for="register-phone-number">
        <span class="field-label-text">Phone number</span>
        <span class="field-label-hint">for trip day stuff</span>
      </label>
      <Input
        id="register-phone-number"
        v-model="phoneNumber"
        v-bind="phoneNumberAttrs"
        autocomplete="tel"
        placeholder="+353…"
        type="tel"
      />
      <p v-if="errors.phoneNumber" class="field-error">{{ errors.phoneNumber }}</p>
    </div>

    <div class="field">
      <label class="field-label" for="register-password">
        <span class="field-label-text">Password</span>
        <span class="field-label-hint">8–72 characters</span>
      </label>
      <Input
        id="register-password"
        v-model="password"
        v-bind="passwordAttrs"
        autocomplete="new-password"
        placeholder="••••••••"
        type="password"
      />
      <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
    </div>

    <div class="field">
      <label class="field-label" for="register-confirm-password">
        <span class="field-label-text">Confirm password</span>
        <span class="field-label-hint">once more</span>
      </label>
      <Input
        id="register-confirm-password"
        v-model="confirmPassword"
        v-bind="confirmPasswordAttrs"
        autocomplete="new-password"
        placeholder="••••••••"
        type="password"
      />
      <p v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</p>
    </div>

    <Button class="submit-btn" :disabled="isSubmitting" size="lg" type="submit">
      <LoaderCircle v-if="isSubmitting" class="h-4 w-4 animate-spin" />
      Create account
    </Button>
  </form>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

.awaiting-card {
  position: relative;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 28px 26px 24px;
  box-shadow:
    4px 4px 0 var(--color-leaf),
    8px 8px 0 var(--primary);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.awaiting-stamp {
  position: absolute;
  top: -14px;
  right: 22px;
  box-shadow: 2px 2px 0 var(--primary);
  transform: rotate(6deg);
}
.awaiting-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: 32px;
  line-height: 1;
  color: var(--primary);
}
.awaiting-body {
  margin: 0;
  font-size: 15px;
  color: var(--muted-foreground);
  line-height: 1.55;
}
.awaiting-back {
  margin-top: 8px;
  width: 100%;
}
</style>
