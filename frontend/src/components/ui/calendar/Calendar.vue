<script setup lang="ts">
import type { CalendarRootEmits, CalendarRootProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'
import {
  CalendarCell,
  CalendarCellTrigger,
  CalendarGrid,
  CalendarGridBody,
  CalendarGridHead,
  CalendarGridRow,
  CalendarHeadCell,
  CalendarHeader,
  CalendarHeading,
  CalendarNext,
  CalendarPrev,
  CalendarRoot,
  useForwardPropsEmits,
} from 'reka-ui'

import { buttonVariants } from '@/components/ui/button'
import { cn } from '@/lib/utils'

defineOptions({
  name: 'UiCalendar',
})

const props = withDefaults(
  defineProps<
    CalendarRootProps & {
      class?: HTMLAttributes['class']
    }
  >(),
  {
    fixedWeeks: true,
    class: undefined,
  },
)

const emits = defineEmits<CalendarRootEmits>()
const forwardedProps = useForwardPropsEmits(props, emits)
</script>

<template>
  <CalendarRoot v-slot="{ grid, weekDays }" v-bind="forwardedProps" data-slot="calendar" :class="cn('p-3', props.class)">
    <CalendarHeader class="relative flex items-center justify-center px-10 pb-4">
      <CalendarPrev
        :class="
          cn(
            buttonVariants({ variant: 'outline', size: 'sm' }),
            'absolute left-0 h-8 w-8 rounded-full bg-transparent p-0 opacity-70 shadow-none hover:bg-white hover:opacity-100',
          )
        "
      >
        <ChevronLeft class="h-4 w-4" />
      </CalendarPrev>

      <CalendarHeading class="text-sm font-semibold tracking-tight text-foreground" />

      <CalendarNext
        :class="
          cn(
            buttonVariants({ variant: 'outline', size: 'sm' }),
            'absolute right-0 h-8 w-8 rounded-full bg-transparent p-0 opacity-70 shadow-none hover:bg-white hover:opacity-100',
          )
        "
      >
        <ChevronRight class="h-4 w-4" />
      </CalendarNext>
    </CalendarHeader>

    <div class="space-y-4">
      <CalendarGrid v-for="month in grid" :key="month.value.toString()" class="w-full border-collapse">
        <CalendarGridHead>
          <CalendarGridRow class="flex w-full">
            <CalendarHeadCell
              v-for="day in weekDays"
              :key="day"
              class="flex h-9 flex-1 items-center justify-center text-[0.72rem] font-semibold uppercase tracking-[0.12em] text-muted-foreground"
            >
              {{ day }}
            </CalendarHeadCell>
          </CalendarGridRow>
        </CalendarGridHead>

        <CalendarGridBody>
          <CalendarGridRow
            v-for="(weekDates, index) in month.rows"
            :key="`week-${index}`"
            class="mt-2 flex w-full"
          >
            <CalendarCell
              v-for="weekDate in weekDates"
              :key="weekDate.toString()"
              :date="weekDate"
              class="relative flex h-9 flex-1 items-center justify-center p-0 text-center text-sm"
            >
              <CalendarCellTrigger
                :day="weekDate"
                :month="month.value"
                :class="
                  cn(
                    buttonVariants({ variant: 'ghost', size: 'sm' }),
                    'h-9 w-9 rounded-full p-0 font-medium shadow-none hover:bg-secondary/80 hover:text-foreground',
                    'data-[selected]:bg-primary data-[selected]:text-primary-foreground data-[selected]:hover:bg-primary',
                    'data-[disabled]:opacity-35 data-[disabled]:text-muted-foreground',
                    'data-[outside-view]:text-muted-foreground data-[outside-view]:opacity-60',
                    '[&[data-today]:not([data-selected])]:border [&[data-today]:not([data-selected])]:border-primary/35 [&[data-today]:not([data-selected])]:bg-primary/8',
                  )
                "
              />
            </CalendarCell>
          </CalendarGridRow>
        </CalendarGridBody>
      </CalendarGrid>
    </div>
  </CalendarRoot>
</template>
