<script setup lang="ts">
import type { SelectRootEmits, SelectRootProps } from 'reka-ui'
import { SelectRoot, useForwardPropsEmits } from 'reka-ui'

defineOptions({
  name: 'UiSelect',
})

const props = defineProps<SelectRootProps>()
const emits = defineEmits<SelectRootEmits>()

const forwardedProps = useForwardPropsEmits(props, emits)
</script>

<template>
  <SelectRoot v-slot="slotProps" data-slot="select" v-bind="forwardedProps">
    <slot v-bind="slotProps" />
  </SelectRoot>
</template>
