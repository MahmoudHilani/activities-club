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
        'grid h-12 w-full grid-cols-2 rounded-full bg-[color:color-mix(in_srgb,var(--secondary)_82%,white)] p-1 text-muted-foreground',
        props.class,
      )
    "
  >
    <slot />
  </TabsList>
</template>
