<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { TabsList } from 'reka-ui'

import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <TabsList
    :class="
      cn(
        'grid h-12 w-full grid-cols-2 rounded-full border-2 border-primary bg-white p-1 text-primary shadow-[3px_3px_0_var(--color-coral)]',
        props.class,
      )
    "
  >
    <slot />
  </TabsList>
</template>
