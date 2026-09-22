<template>
  <div class="archive-view">
    <div class="archive-header">
      <h1 class="archive-title">时间树</h1>
      <p class="archive-subtitle">全部知识 · {{ items.length }} 条</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="10">
      <template #default>
        <TimeTree v-if="items.length" :items="items" />
        <el-empty v-else description="还没有知识条目" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { findAll } from '@/api/knowledge'
import type { KnowledgeListVo } from '@/api/types/knowledge'
import TimeTree from '@/components/archive/TimeTree.vue'

const loading = ref(true)
const items = ref<KnowledgeListVo[]>([])

onMounted(async () => {
  try {
    items.value = await findAll()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.archive-view {
  max-width: 860px;
  margin: 0 auto;
  padding: 80px 24px 60px;
}

.archive-header {
  margin-bottom: 32px;
}

.archive-title {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.archive-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
