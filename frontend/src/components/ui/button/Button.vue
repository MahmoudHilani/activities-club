<script setup lang="ts">
import type { VariantProps } from 'class-variance-authority'
import type { ButtonHTMLAttributes, HTMLAttributes } from 'vue'

import { cn } from '@/lib/utils'

import { buttonVariants } from './buttonVariants'

defineOptions({
  inheritAttrs: false,
})

type ButtonVariant = VariantProps<typeof buttonVariants>['variant']
type ButtonSize = VariantProps<typeof buttonVariants>['size']

const props = withDefaults(
  defineProps<{
    class?: HTMLAttributes['class']
    variant?: ButtonVariant
    size?: ButtonSize
    type?: ButtonHTMLAttributes['type']
    disabled?: boolean
  }>(),
  {
    variant: 'default',
    size: 'default',
    type: 'button',
    class: undefined,
    disabled: false,
  },
)
</script>

<template>
  <button
    :type="type"
    :class="cn(buttonVariants({ variant, size }), props.class)"
    :disabled="disabled"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>
