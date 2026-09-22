<template>
  <span class="tag-chip" :style="chipStyle">
    {{ tagName }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  tagName: string
  tagColor?: string
}>()

const chipStyle = computed(() => {
  const color = props.tagColor || '#2d6a4f'
  return {
    backgroundColor: color + '18',
    color: color,
    borderColor: color + '30',
  }
})
</script>

<style scoped lang="scss">
.tag-chip {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid;
  line-height: 1.6;
  cursor: pointer;
  transition: opacity 0.2s;

  &:hover {
    opacity: 0.8;
  }
}
</style>
