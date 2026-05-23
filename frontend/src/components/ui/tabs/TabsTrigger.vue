<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { TabsTrigger, type TabsTriggerProps } from 'reka-ui'

import { cn } from '@/lib/utils'

const props = defineProps<TabsTriggerProps & { class?: HTMLAttributes['class'] }>()
</script>

<template>
  <TabsTrigger
    v-bind="props"
    :class="
      cn(
        'inline-flex items-center justify-center rounded-full px-4 text-sm font-bold tracking-tight text-primary/70 transition-all duration-200 outline-none focus-visible:ring-4 focus-visible:ring-ring data-[state=active]:bg-primary data-[state=active]:text-primary-foreground data-[state=active]:shadow-[2px_2px_0_var(--color-ochre)]',
        props.class,
      )
    "
  >
    <slot />
  </TabsTrigger>
</template>
