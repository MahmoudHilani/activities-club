<script setup lang="ts">
import type { HTMLAttributes } from 'vue'

import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '@/lib/utils'

const alertVariants = cva(
  'relative rounded-[1.25rem] border-2 px-5 py-4 text-sm font-medium leading-6',
  {
    variants: {
      variant: {
        default:
          'border-primary bg-white text-primary shadow-[3px_3px_0_var(--color-leaf)]',
        destructive:
          'border-[var(--color-coral)] bg-[color:color-mix(in_srgb,var(--color-coral)_12%,white)] text-[var(--color-coral)] shadow-[3px_3px_0_var(--primary)]',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
)

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
