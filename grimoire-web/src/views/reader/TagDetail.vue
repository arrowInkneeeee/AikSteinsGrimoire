<template>
  <div class="tag-detail">
    <div class="tag-detail__header">
      <router-link to="/tags" class="tag-detail__back">
        <el-icon><ArrowLeft /></el-icon>
        返回标签
      </router-link>
      <h1 class="tag-detail__title" :style="{ color: tagColor }">{{ tagName }}</h1>
      <p class="tag-detail__count">共 {{ total }} 条知识</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <TimeTree v-if="items.length" :items="items" />
        <el-empty v-else description="该标签下暂无知识" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { findPage } from '@/api/knowledge'
import type { KnowledgeListVo } from '@/api/types/knowledge'
import TimeTree from '@/components/archive/TimeTree.vue'

const route = useRoute()
const loading = ref(true)
const items = ref<KnowledgeListVo[]>([])
const total = ref('0')
const tagName = ref('')
const tagColor = ref('#2d6a4f')

async function loadData() {
  loading.value = true
  const id = route.params.id as string
  try {
    const result = await findPage({
      current: 1,
      size: 200,
      tagId: id,
      status: 1,
    })
    items.value = result.records
    total.value = result.total
    // 从第一条记录的标签取名称和颜色
    if (result.records.length > 0 && result.records[0].tags.length > 0) {
      const tag = result.records[0].tags.find(t => t.id === id)
      if (tag) {
        tagName.value = tag.tagName
        tagColor.value = tag.tagColor || '#2d6a4f'
      } else {
        tagName.value = `标签 #${id}`
      }
    } else {
      tagName.value = `标签 #${id}`
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
watch(() => route.params.id, loadData)
</script>

<style scoped lang="scss">
.tag-detail {
  max-width: 860px;
  margin: 0 auto;
  padding: 80px 24px 60px;
}

.tag-detail__header {
  margin-bottom: 32px;
}

.tag-detail__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;

  &:hover {
    color: var(--color-theme);
  }
}

.tag-detail__title {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.tag-detail__count {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
