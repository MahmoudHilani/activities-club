<script setup lang="ts">
import type { HTMLAttributes } from 'vue'

import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '@/lib/utils'

const alertVariants = cva('rounded-2xl border px-4 py-3 text-sm', {
  variants: {
    variant: {
      default: 'border-border bg-white/75 text-foreground',
      destructive:
        'border-[color:color-mix(in_srgb,var(--destructive)_34%,white)] bg-[color:color-mix(in_srgb,var(--destructive)_12%,white)] text-destructive',
    },
  },
  defaultVariants: {
    variant: 'default',
  },
})

const props = withDefaults(
  defineProps<{
    class?: HTMLAttributes['class']
    variant?: VariantProps<typeof alertVariants>['variant']
  }>(),
  {
    class: undefined,
    variant: 'default',
  },
)
</script>

<template>
  <div :class="cn(alertVariants({ variant: props.variant }), props.class)" role="alert">
    <slot />
  </div>
</template>
