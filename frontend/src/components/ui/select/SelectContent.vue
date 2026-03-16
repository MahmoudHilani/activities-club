<script setup lang="ts">
import type { SelectContentEmits, SelectContentProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { SelectContent, SelectPortal, SelectViewport, useForwardPropsEmits } from 'reka-ui'

import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<
    SelectContentProps & {
      class?: HTMLAttributes['class']
    }
  >(),
  {
    position: 'popper',
    class: undefined,
  },
)

const emits = defineEmits<SelectContentEmits>()
const forwardedProps = useForwardPropsEmits(props, emits)
</script>

<template>
  <SelectPortal>
    <SelectContent
      v-bind="{ ...$attrs, ...forwardedProps }"
      data-slot="select-content"
      :class="
        cn(
          'z-50 max-h-72 w-[var(--reka-select-trigger-width)] min-w-[var(--reka-select-trigger-width)] overflow-hidden rounded-[1.25rem] border border-white/70 bg-[color:var(--card)] text-popover-foreground shadow-[0_18px_40px_rgba(34,45,66,0.16)] backdrop-blur-sm outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95 data-[side=bottom]:translate-y-1 data-[side=left]:-translate-x-1 data-[side=right]:translate-x-1 data-[side=top]:-translate-y-1',
          props.class,
        )
      "
    >
      <SelectViewport class="max-h-72 p-1.5">
        <slot />
      </SelectViewport>
    </SelectContent>
  </SelectPortal>
</template>
