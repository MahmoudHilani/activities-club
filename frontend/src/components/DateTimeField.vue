<script setup lang="ts">
import type { DateValue } from 'reka-ui'
import { CalendarDateTime, parseDateTime, toCalendarDate } from '@internationalized/date'
import { format } from 'date-fns'
import { CalendarDays, Clock3 } from 'lucide-vue-next'
import { computed, ref } from 'vue'

import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const modelValue = defineModel<string>({ default: '' })

const props = withDefaults(
  defineProps<{
    placeholder?: string
  }>(),
  {
    placeholder: 'Select date and time',
  },
)

const open = ref(false)

const hourOptions = Array.from({ length: 24 }, (_, hour) => ({
  label: hour.toString().padStart(2, '0'),
  value: hour.toString().padStart(2, '0'),
}))

const minuteOptions = Array.from({ length: 60 }, (_, minute) => ({
  label: minute.toString().padStart(2, '0'),
  value: minute.toString().padStart(2, '0'),
}))

const parsedValue = computed(() => {
  if (!modelValue.value) {
    return null
  }

  try {
    return parseDateTime(modelValue.value)
  } catch {
    return null
  }
})

const displayValue = computed(() => {
  if (!parsedValue.value) {
    return props.placeholder
  }

  return format(new Date(`${modelValue.value}:00`), "EEE, MMM d, yyyy 'at' HH:mm")
})

const hourValue = computed(() => (parsedValue.value ? pad(parsedValue.value.hour) : ''))
const minuteValue = computed(() => (parsedValue.value ? pad(parsedValue.value.minute) : ''))

function updateDatePart(nextDate: DateValue | undefined): void {
  if (!nextDate) {
    modelValue.value = ''
    return
  }

  const calendarDate = toCalendarDate(nextDate)
  const current = parsedValue.value
  const next = new CalendarDateTime(
    calendarDate.year,
    calendarDate.month,
    calendarDate.day,
    current?.hour ?? 0,
    current?.minute ?? 0,
  )

  modelValue.value = toModelString(next)
}

function updateHour(value: string): void {
  if (!parsedValue.value || !value) {
    return
  }

  modelValue.value = toModelString(
    parsedValue.value.set({
      hour: Number(value),
      second: 0,
      millisecond: 0,
    }),
  )
}

function updateMinute(value: string): void {
  if (!parsedValue.value || !value) {
    return
  }

  modelValue.value = toModelString(
    parsedValue.value.set({
      minute: Number(value),
      second: 0,
      millisecond: 0,
    }),
  )
}

function clearValue(): void {
  modelValue.value = ''
  open.value = false
}

function toModelString(value: CalendarDateTime): string {
  return value.toString().slice(0, 16)
}

function pad(value: number): string {
  return value.toString().padStart(2, '0')
}
</script>

<template>
  <Popover v-model:open="open">
    <PopoverTrigger as-child>
      <Button
        class="h-12 w-full justify-between rounded-2xl border border-border bg-white/80 px-4 py-3 text-left font-medium text-foreground shadow-none hover:bg-white"
        variant="outline"
      >
        <span class="flex min-w-0 items-center gap-3">
          <CalendarDays class="h-4 w-4 shrink-0 text-primary" />
          <span class="truncate" :class="parsedValue ? 'text-foreground' : 'text-muted-foreground'">
            {{ displayValue }}
          </span>
        </span>
      </Button>
    </PopoverTrigger>

    <PopoverContent align="start" class="w-[min(24rem,calc(100vw-2rem))] p-0">
      <div class="p-3 pb-2">
        <Calendar
          :model-value="parsedValue ?? undefined"
          @update:model-value="updateDatePart"
        />
      </div>

      <div class="border-t border-border/70 px-4 py-4">
        <div class="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
          <Clock3 class="h-4 w-4 text-primary" />
          Time
        </div>

        <div class="grid gap-3 sm:grid-cols-2">
          <Select :disabled="!parsedValue" :model-value="hourValue" @update:model-value="(value) => updateHour(String(value ?? ''))">
            <SelectTrigger>
              <SelectValue placeholder="Hour" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="option in hourOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </SelectItem>
            </SelectContent>
          </Select>

          <Select :disabled="!parsedValue" :model-value="minuteValue" @update:model-value="(value) => updateMinute(String(value ?? ''))">
            <SelectTrigger>
              <SelectValue placeholder="Minute" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="option in minuteOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div v-if="parsedValue" class="mt-4 flex justify-end">
          <Button
            class="h-9 rounded-full px-4 text-muted-foreground shadow-none hover:bg-secondary/70 hover:text-foreground"
            variant="ghost"
            @click="clearValue"
          >
            Clear
          </Button>
        </div>
      </div>
    </PopoverContent>
  </Popover>
</template>
