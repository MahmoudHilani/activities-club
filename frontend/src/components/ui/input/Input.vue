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
        'flex h-12 w-full rounded-2xl border border-input bg-white/80 px-4 py-3 text-sm shadow-[inset_0_1px_0_rgba(255,255,255,0.45)] transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-primary focus-visible:ring-4 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50',
        props.class,
      )
    "
    v-bind="$attrs"
  >
</template>
