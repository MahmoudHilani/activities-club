<script setup lang="ts">
import type { HTMLAttributes, InputTypeHTMLAttribute } from 'vue'

import { cn } from '@/lib/utils'

defineOptions({
  inheritAttrs: false,
})

const modelValue = defineModel<string>()

const props = withDefaults(
  defineProps<{
    class?: HTMLAttributes['class']
    type?: InputTypeHTMLAttribute
    placeholder?: string
    disabled?: boolean
  }>(),
  {
    class: undefined,
    type: 'text',
    placeholder: '',
    disabled: false,
  },
)
</script>

<template>
  <input
    v-model="modelValue"
    :type="type"
    :placeholder="placeholder"
    :disabled="disabled"
    :class="
      cn(
        'flex h-12 w-full rounded-2xl border-2 border-primary bg-white px-4 py-3 text-sm font-medium text-primary transition-all duration-150 outline-none placeholder:text-muted-foreground placeholder:font-normal focus-visible:border-[var(--color-coral)] focus-visible:shadow-[3px_3px_0_var(--color-coral)] disabled:cursor-not-allowed disabled:opacity-50',
        props.class,
      )
    "
    v-bind="$attrs"
  >
</template>
