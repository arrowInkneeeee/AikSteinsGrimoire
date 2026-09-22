<template>
  <span class="type-badge" :style="{ backgroundColor: typeColor }">
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  type: number
  typeDesc?: string
}>()

const typeMap: Record<number, { label: string; color: string }> = {
  1: { label: '笔记', color: 'var(--type-note)' },
  2: { label: '组件', color: 'var(--type-component)' },
  3: { label: '方案', color: 'var(--type-solution)' },
  4: { label: '片段', color: 'var(--type-snippet)' },
}

const label = computed(() => props.typeDesc || typeMap[props.type]?.label || '未知')
const typeColor = computed(() => typeMap[props.type]?.color || '#999')
</script>

<style scoped lang="scss">
.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  line-height: 1.6;
}
</style>
