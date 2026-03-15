<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { TabsRoot, type TabsRootProps } from 'reka-ui'

import { cn } from '@/lib/utils'

const modelValue = defineModel<string>()

const props = defineProps<
  Omit<TabsRootProps, 'modelValue'> & {
    class?: HTMLAttributes['class']
  }
>()
</script>

<template>
  <TabsRoot v-model="modelValue" v-bind="props" :class="cn('w-full', props.class)">
    <slot />
  </TabsRoot>
</template>
