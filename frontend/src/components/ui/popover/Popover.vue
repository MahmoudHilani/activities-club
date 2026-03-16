<script setup lang="ts">
import type { PopoverRootEmits, PopoverRootProps } from 'reka-ui'
import { PopoverRoot, useForwardPropsEmits } from 'reka-ui'

defineOptions({
  name: 'UiPopover',
})

const props = defineProps<PopoverRootProps>()
const emits = defineEmits<PopoverRootEmits>()

const forwardedProps = useForwardPropsEmits(props, emits)
</script>

<template>
  <PopoverRoot v-slot="slotProps" data-slot="popover" v-bind="forwardedProps">
    <slot v-bind="slotProps" />
  </PopoverRoot>
</template>
