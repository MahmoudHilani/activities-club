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
        'flex h-12 w-full items-center justify-between rounded-2xl border border-border bg-white/80 px-4 py-3 text-sm font-medium text-foreground shadow-[inset_0_1px_0_rgba(255,255,255,0.45)] outline-none transition-colors focus-visible:border-primary focus-visible:ring-4 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 data-[placeholder]:text-muted-foreground',
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
