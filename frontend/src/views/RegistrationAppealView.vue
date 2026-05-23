<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { LoaderCircle } from 'lucide-vue-next'
import { useForm } from 'vee-validate'
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { z } from 'zod'

import { Alert } from '@/components/ui/alert'
import { Button, buttonVariants } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { getApiMessage } from '@/lib/api/errors'
import { useSessionStore } from '@/stores/session'

const userTypeOptions = [
  { label: 'Student', value: 'STUDENT' },
  { label: 'Staff', value: 'STAFF' },
] as const

const sessionStore = useSessionStore()
const serverError = ref('')
const submittedMessage = ref('')

const appealSchema = toTypedSchema(
  z
    .object({
      username: z
        .string()
        .trim()
        .min(1, 'Name and surname is required')
        .max(30, 'Name and surname must be 30 characters or less'),
      userType: z.enum(['STUDENT', 'STAFF'], {
        message: 'Choose whether you are registering as a student or staff member',
      }),
      studentNumber: z.string().trim().max(30, 'Student number must be 30 characters or less'),
      phoneNumber: z.string().trim().max(30, 'Phone number must be 30 characters or less'),
    })
    .superRefine((values, context) => {
      if (values.userType !== 'STUDENT') {
        return
      }

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
    }),
)

const { defineField, errors, handleSubmit, isSubmitting } = useForm({
  validationSchema: appealSchema,
  initialValues: {
    username: '',
    userType: 'STUDENT',
    studentNumber: '',
    phoneNumber: '',
  },
})

const [username, usernameAttrs] = defineField('username')
const [userType] = defineField('userType')
const [studentNumber, studentNumberAttrs] = defineField('studentNumber')
const [phoneNumber, phoneNumberAttrs] = defineField('phoneNumber')
const isStudent = computed(() => userType.value === 'STUDENT')

watch(userType, (value) => {
  if (value === 'STAFF') {
    studentNumber.value = ''
    phoneNumber.value = ''
  }
})

const onSubmit = handleSubmit(async (values) => {
  serverError.value = ''

  try {
    const response = await sessionStore.submitRegistrationAppeal({
      username: values.username,
      userType: values.userType,
      studentNumber: values.userType === 'STUDENT' ? values.studentNumber.trim() : null,
      phoneNumber: values.userType === 'STUDENT' ? values.phoneNumber.trim() : null,
    })
    submittedMessage.value = response.message
  } catch (error) {
    serverError.value = getApiMessage(error) ?? 'We could not submit your appeal right now.'
  }
})
</script>

<template>
  <section class="appeal-page">
    <div class="appeal-card">
      <div v-if="submittedMessage" class="awaiting-card">
        <span class="craft-tag craft-tag-ochre awaiting-stamp">on the list</span>
        <h1 class="awaiting-title">
          <span class="display-text">Awaiting</span>
          <span class="hand-text"> approval</span>
        </h1>
        <p class="appeal-sub">{{ submittedMessage }}</p>
        <RouterLink :class="[buttonVariants({ variant: 'outline' }), 'appeal-link']" to="/auth">
          Back to login
        </RouterLink>
      </div>

      <template v-else-if="sessionStore.appealToken">
        <h1 class="appeal-title">
          <span class="display-text">Update your</span>
          <span class="hand-text"> registration</span>
        </h1>
        <p class="appeal-sub">
          Correct your registration details and send them back for admin review. Your login email
          and password stay unchanged.
        </p>

        <form class="appeal-form" novalidate @submit.prevent="onSubmit">
          <Alert v-if="serverError" variant="destructive">
            {{ serverError }}
          </Alert>

          <div class="field">
            <label class="field-label" for="appeal-user-type">Student or staff</label>
            <Select v-model="userType">
              <SelectTrigger id="appeal-user-type" aria-label="User type">
                <SelectValue placeholder="Select user type" />
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
            <label class="field-label" for="appeal-username">Name and surname</label>
            <Input id="appeal-username" v-model="username" v-bind="usernameAttrs" />
            <p v-if="errors.username" class="field-error">{{ errors.username }}</p>
          </div>

          <div v-if="isStudent" class="field">
            <label class="field-label" for="appeal-student-number">Student number</label>
            <Input id="appeal-student-number" v-model="studentNumber" v-bind="studentNumberAttrs" />
            <p v-if="errors.studentNumber" class="field-error">{{ errors.studentNumber }}</p>
          </div>

          <div v-if="isStudent" class="field">
            <label class="field-label" for="appeal-phone-number">Phone number</label>
            <Input id="appeal-phone-number" v-model="phoneNumber" v-bind="phoneNumberAttrs" type="tel" />
            <p v-if="errors.phoneNumber" class="field-error">{{ errors.phoneNumber }}</p>
          </div>

          <Button class="submit-btn" :disabled="isSubmitting" size="lg" type="submit">
            <LoaderCircle v-if="isSubmitting" class="h-4 w-4 animate-spin" />
            Submit appeal
          </Button>
        </form>
      </template>

      <template v-else>
        <h1 class="appeal-title">
          <span class="display-text">Appeal link</span>
          <span class="hand-text"> expired</span>
        </h1>
        <p class="appeal-sub">Sign in again to access your denied registration securely.</p>
        <RouterLink :class="[buttonVariants(), 'appeal-link']" to="/auth">Back to login</RouterLink>
      </template>
    </div>
  </section>
</template>

<style scoped>
.appeal-page {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 1.5rem 0 4rem;
}
.appeal-card {
  width: 100%;
  max-width: 34rem;
  border: 2px solid var(--primary);
  border-radius: 32px;
  background: white;
  box-shadow:
    5px 5px 0 var(--color-coral),
    10px 10px 0 var(--color-ochre);
  padding: 36px;
}
.appeal-title,
.awaiting-title {
  margin: 0 0 12px;
  color: var(--primary);
  font-family: var(--font-display);
  font-size: clamp(34px, 5vw, 48px);
  font-weight: 400;
  line-height: 1;
}
.appeal-title .hand-text,
.awaiting-title .hand-text {
  font-size: 1.1em;
}
.appeal-sub {
  margin: 0 0 24px;
  color: var(--muted-foreground);
  font-size: 15px;
  line-height: 1.55;
}
.appeal-form,
.field,
.awaiting-card {
  display: flex;
  flex-direction: column;
}
.appeal-form {
  gap: 18px;
}
.field {
  gap: 8px;
}
.field-label {
  color: var(--primary);
  font-family: var(--font-display);
  font-size: 22px;
}
.field-error {
  margin: 0;
  color: var(--color-coral);
  font-size: 13px;
  font-weight: 600;
}
.submit-btn,
.appeal-link {
  width: 100%;
}
.awaiting-card {
  position: relative;
  gap: 12px;
}
.awaiting-stamp {
  position: absolute;
  top: -50px;
  right: 0;
  transform: rotate(6deg);
  box-shadow: 2px 2px 0 var(--primary);
}
.awaiting-title {
  font-size: 32px;
}
</style>
