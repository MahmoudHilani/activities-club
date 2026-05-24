<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { LoaderCircle } from 'lucide-vue-next'
import { useForm } from 'vee-validate'
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

import { Alert } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { submitSportsClubSignup } from '@/lib/api/sportsClubSignups'
import type { Gender, SportsClub } from '@/lib/api/types'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const sessionStore = useSessionStore()
const serverError = ref('')

const sportsClubOptions: ReadonlyArray<{ value: SportsClub; label: string; tag: string }> = [
  { value: 'FOOTBALL', label: 'Football', tag: 'tag-coral' },
  { value: 'BASKETBALL', label: 'Basketball', tag: 'tag-ochre' },
  { value: 'BADMINTON', label: 'Badminton', tag: 'tag-leaf' },
  { value: 'VOLLEYBALL', label: 'Volleyball', tag: 'tag-coral' },
  { value: 'CRICKET', label: 'Cricket', tag: 'tag-leaf' },
  { value: 'BILLIARDS', label: 'Billiards', tag: 'tag-ochre' },
  { value: 'DANCE', label: 'Dance', tag: 'tag-coral' },
  { value: 'YOGA', label: 'Yoga', tag: 'tag-leaf' },
  { value: 'PILATES', label: 'Pilates', tag: 'tag-ochre' },
  { value: 'BOXERCISE', label: 'Boxercise', tag: 'tag-coral' },
]

const genderOptions: ReadonlyArray<{ value: Gender; label: string }> = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
]

const signupSchema = toTypedSchema(
  z.object({
    name: z
      .string()
      .trim()
      .min(1, 'Name and surname is required')
      .max(120, 'Must be 120 characters or less'),
    email: z
      .string()
      .trim()
      .min(1, 'Email is required')
      .max(120, 'Must be 120 characters or less')
      .email('Enter a valid email address'),
    phoneNumber: z
      .string()
      .trim()
      .min(1, 'Phone number is required')
      .max(30, 'Must be 30 characters or less'),
    studentNumber: z
      .string()
      .trim()
      .min(1, 'Student number is required')
      .max(30, 'Must be 30 characters or less'),
    course: z
      .string()
      .trim()
      .min(1, 'Course (major) is required')
      .max(120, 'Must be 120 characters or less'),
    gender: z.enum(['MALE', 'FEMALE', 'OTHER'], { message: 'Choose one' }),
    sportsClubs: z
      .array(
        z.enum([
          'FOOTBALL',
          'BASKETBALL',
          'BADMINTON',
          'VOLLEYBALL',
          'CRICKET',
          'BILLIARDS',
          'DANCE',
          'YOGA',
          'PILATES',
          'BOXERCISE',
        ]),
      )
      .min(1, 'Pick at least one sports club'),
  }),
)

const prefillUser = sessionStore.user
const submitAttempted = ref(false)
const { defineField, errors, handleSubmit, isSubmitting, values, setFieldValue } = useForm({
  validationSchema: signupSchema,
  initialValues: {
    name: prefillUser?.username ?? '',
    email: prefillUser?.email ?? '',
    phoneNumber: prefillUser?.phoneNumber ?? '',
    studentNumber: prefillUser?.studentNumber ?? '',
    course: '',
    gender: undefined,
    sportsClubs: [],
  },
})

const [name, nameAttrs] = defineField('name')
const [email, emailAttrs] = defineField('email')
const [phoneNumber, phoneNumberAttrs] = defineField('phoneNumber')
const [studentNumber, studentNumberAttrs] = defineField('studentNumber')
const [course, courseAttrs] = defineField('course')
const [gender] = defineField('gender')

watch(
  () => sessionStore.user,
  (user) => {
    if (!user) return
    if (!values.name) setFieldValue('name', user.username)
    if (!values.email) setFieldValue('email', user.email)
    if (!values.phoneNumber && user.phoneNumber) setFieldValue('phoneNumber', user.phoneNumber)
    if (!values.studentNumber && user.studentNumber)
      setFieldValue('studentNumber', user.studentNumber)
  },
)

function toggleSport(sport: SportsClub): void {
  const current = values.sportsClubs ?? []
  const next = current.includes(sport)
    ? current.filter((value) => value !== sport)
    : [...current, sport]
  setFieldValue('sportsClubs', next)
}

function isSportSelected(sport: SportsClub): boolean {
  return (values.sportsClubs ?? []).includes(sport)
}

function markSubmitAttempted(): void {
  submitAttempted.value = true
}

const onSubmit = handleSubmit(async (formValues) => {
  serverError.value = ''
  try {
    await submitSportsClubSignup({
      name: formValues.name.trim(),
      email: formValues.email.trim(),
      phoneNumber: formValues.phoneNumber.trim(),
      studentNumber: formValues.studentNumber.trim(),
      course: formValues.course.trim(),
      gender: formValues.gender,
      sportsClubs: formValues.sportsClubs,
    })
    await router.push({ name: 'home', query: { signup: 'success' } })
  } catch (error) {
    const message =
      error instanceof Error && error.message
        ? error.message
        : 'Something went wrong submitting your signup. Please try again.'
    serverError.value = message
  }
})
</script>

<template>
  <div class="signup-page">
    <header class="signup-header">
      <span class="craft-tag craft-tag-ochre signup-tag">join us</span>
      <h1 class="signup-title">
        <span class="display-text">Sign up to a</span>
        <span class="hand-text"> sports club</span>
      </h1>
    </header>

    <form
      class="signup-form"
      novalidate
      @submit.prevent="
        () => {
          markSubmitAttempted()
          onSubmit()
        }
      "
    >
      <Alert v-if="serverError" variant="destructive">{{ serverError }}</Alert>

      <div class="field">
        <label class="field-label" for="signup-name">
          <span class="field-label-text">Name and surname</span>
          <span class="field-label-hint">the one on your card</span>
        </label>
        <Input
          id="signup-name"
          v-model="name"
          v-bind="nameAttrs"
          autocomplete="name"
          placeholder="e.g. Niamh Murphy"
        />
        <p v-if="errors.name" class="field-error">{{ errors.name }}</p>
      </div>

      <div class="field">
        <label class="field-label" for="signup-email">
          <span class="field-label-text">Email</span>
          <span class="field-label-hint">we'll be in touch</span>
        </label>
        <Input
          id="signup-email"
          v-model="email"
          v-bind="emailAttrs"
          autocomplete="email"
          placeholder="you@campus.edu"
          type="email"
        />
        <p v-if="errors.email" class="field-error">{{ errors.email }}</p>
      </div>

      <div class="field-row">
        <div class="field">
          <label class="field-label" for="signup-phone">
            <span class="field-label-text">Phone number</span>
          </label>
          <Input
            id="signup-phone"
            v-model="phoneNumber"
            v-bind="phoneNumberAttrs"
            autocomplete="tel"
            placeholder="+353…"
            type="tel"
          />
          <p v-if="errors.phoneNumber" class="field-error">{{ errors.phoneNumber }}</p>
        </div>

        <div class="field">
          <label class="field-label" for="signup-student-number">
            <span class="field-label-text">Student number</span>
          </label>
          <Input
            id="signup-student-number"
            v-model="studentNumber"
            v-bind="studentNumberAttrs"
            autocomplete="off"
            placeholder="e.g. 3209412"
          />
          <p v-if="errors.studentNumber" class="field-error">{{ errors.studentNumber }}</p>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label class="field-label" for="signup-course">
            <span class="field-label-text">Course</span>
          </label>
          <Input
            id="signup-course"
            v-model="course"
            v-bind="courseAttrs"
            autocomplete="off"
            placeholder="e.g. Computer Science"
          />
          <p v-if="errors.course" class="field-error">{{ errors.course }}</p>
        </div>

        <div class="field">
          <label class="field-label" for="signup-gender">
            <span class="field-label-text">Gender</span>
          </label>
          <Select v-model="gender">
            <SelectTrigger id="signup-gender" aria-label="Gender">
              <SelectValue placeholder="Select your gender" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="option in genderOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.gender" class="field-error">{{ errors.gender }}</p>
        </div>
      </div>

      <fieldset class="field sports-fieldset">
        <legend class="field-label sports-legend">
          <span class="field-label-text">Sports clubs</span>
          <span class="field-label-hint">pick one or more</span>
        </legend>
        <div class="sports-grid">
          <label
            v-for="option in sportsClubOptions"
            :key="option.value"
            class="sport-card"
            :class="{ 'sport-card-selected': isSportSelected(option.value) }"
          >
            <input
              type="checkbox"
              class="sport-checkbox"
              :checked="isSportSelected(option.value)"
              @change="toggleSport(option.value)"
            />
            <span class="sport-tag" :class="option.tag">{{ option.label }}</span>
          </label>
        </div>
        <p v-if="submitAttempted && errors.sportsClubs" class="field-error">
          {{ errors.sportsClubs }}
        </p>
      </fieldset>

      <Button class="submit-btn" :disabled="isSubmitting" size="lg" type="submit">
        <LoaderCircle v-if="isSubmitting" class="h-4 w-4 animate-spin" />
        Sign me up
      </Button>
    </form>
  </div>
</template>

<style scoped>
.signup-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px 8px 64px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}
.signup-header {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}
.signup-tag {
  align-self: flex-start;
}
.signup-title {
  margin: 0;
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(36px, 6vw, 56px);
  line-height: 1;
  color: var(--primary);
}
.signup-title .hand-text {
  font-family: var(--font-hand);
  font-weight: 700;
  color: var(--color-coral);
}
.signup-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 28px;
  padding: 28px 26px 26px;
  box-shadow:
    4px 4px 0 var(--color-leaf),
    8px 8px 0 var(--primary);
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
}
@media (min-width: 560px) {
  .field-row {
    grid-template-columns: 1fr 1fr;
  }
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
.sports-fieldset {
  border: none;
  margin: 0;
  padding: 0;
}
.sports-legend {
  width: 100%;
  padding: 0;
  margin-bottom: 10px;
}
.sports-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.sport-card {
  position: relative;
  border: 2px solid var(--primary);
  background: white;
  border-radius: 18px;
  padding: 18px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s,
    background 0.15s;
  box-shadow: 3px 3px 0 var(--primary);
}
.sport-card:hover {
  transform: translate(-1px, -1px);
  box-shadow: 5px 5px 0 var(--primary);
}
.sport-card-selected {
  background: var(--color-ochre);
}
.sport-checkbox {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.sport-tag {
  font-family: var(--font-hand);
  font-weight: 700;
  font-size: 22px;
  padding: 6px 16px;
  border-radius: 999px;
  color: white;
  transform: rotate(-2deg);
}
.sport-tag.tag-coral {
  background: var(--color-coral);
}
.sport-tag.tag-ochre {
  background: var(--color-ochre);
  color: var(--primary);
}
.sport-tag.tag-leaf {
  background: var(--color-leaf);
}
.submit-btn {
  margin-top: 6px;
  width: 100%;
}
</style>
