<script setup lang="ts">
import type { PopoverContentEmits, PopoverContentProps } from 'reka-ui'
import type { HTMLAttributes } from 'vue'
import { PopoverContent, PopoverPortal, useForwardPropsEmits } from 'reka-ui'

import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<
    PopoverContentProps & {
      class?: HTMLAttributes['class']
    }
  >(),
  {
    align: 'center',
    sideOffset: 8,
    class: undefined,
  },
)

const emits = defineEmits<PopoverContentEmits>()
const forwardedProps = useForwardPropsEmits(props, emits)
</script>

<template>
  <PopoverPortal>
    <PopoverContent
      v-bind="{ ...$attrs, ...forwardedProps }"
      data-slot="popover-content"
      :class="
        cn(
          'z-50 rounded-[1.5rem] border border-white/70 bg-[color:var(--card)] text-popover-foreground shadow-[0_18px_40px_rgba(34,45,66,0.16)] backdrop-blur-sm outline-none data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95 data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2',
          props.class,
        )
      "
    >
      <slot />
    </PopoverContent>
  </PopoverPortal>
</template>
