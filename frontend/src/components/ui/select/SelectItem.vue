<script setup lang="ts">
import type { SelectItemProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { Check } from 'lucide-vue-next'
import { SelectItem, SelectItemIndicator, SelectItemText, useForwardProps } from 'reka-ui'

import { cn } from '@/lib/utils'

const props = defineProps<
  SelectItemProps & {
    class?: HTMLAttributes['class']
  }
>()

const forwardedProps = useForwardProps(props)
</script>

<template>
  <SelectItem
    v-bind="forwardedProps"
    data-slot="select-item"
    :class="
      cn(
        'relative flex w-full cursor-default items-center rounded-xl px-3 py-2 text-sm font-medium text-foreground outline-none select-none focus:bg-secondary/80 data-[disabled]:pointer-events-none data-[disabled]:opacity-50',
        props.class,
      )
    "
  >
    <span class="absolute right-3 flex h-4 w-4 items-center justify-center">
      <SelectItemIndicator>
        <Check class="h-4 w-4" />
      </SelectItemIndicator>
    </span>

    <SelectItemText>
      <slot />
    </SelectItemText>
  </SelectItem>
</template>
