<script setup lang="ts">
import type { SelectTriggerProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import { SelectIcon, SelectTrigger, useForwardProps } from 'reka-ui'

import { cn } from '@/lib/utils'

const props = defineProps<
  SelectTriggerProps & {
    class?: HTMLAttributes['class']
  }
>()

const forwardedProps = useForwardProps(props)
</script>

<template>
  <SelectTrigger
    v-bind="forwardedProps"
    data-slot="select-trigger"
    :class="
      cn(
        'flex h-12 w-full items-center justify-between rounded-2xl border-2 border-primary bg-white px-4 py-3 text-sm font-medium text-primary outline-none transition-all duration-150 focus-visible:border-[var(--color-coral)] focus-visible:shadow-[3px_3px_0_var(--color-coral)] disabled:cursor-not-allowed disabled:opacity-50 data-[placeholder]:text-muted-foreground data-[placeholder]:font-normal',
        props.class,
      )
    "
  >
    <slot />
    <SelectIcon as-child>
      <ChevronDown class="h-4 w-4 text-muted-foreground" />
    </SelectIcon>
  </SelectTrigger>
</template>
