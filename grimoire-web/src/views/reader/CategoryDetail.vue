<template>
  <div class="category-detail">
    <div class="category-detail__header">
      <router-link to="/categories" class="category-detail__back">
        <el-icon><ArrowLeft /></el-icon>
        返回分类
      </router-link>
      <h1 class="category-detail__title">{{ categoryName }}</h1>
      <p class="category-detail__count">共 {{ total }} 条知识</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <TimeTree v-if="items.length" :items="items" />
        <el-empty v-else description="该分类下暂无知识" />
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
const categoryName = ref('')

async function loadData() {
  loading.value = true
  const id = route.params.id as string
  try {
    const result = await findPage({
      current: 1,
      size: 200,
      categoryId: id,
      includeChildren: true,
      status: 1,
    })
    items.value = result.records
    total.value = result.total
    // 从第一条记录取分类名
    if (result.records.length > 0 && result.records[0].categoryPath) {
      categoryName.value = result.records[0].categoryPath
    } else {
      categoryName.value = `分类 #${id}`
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
watch(() => route.params.id, loadData)
</script>

<style scoped lang="scss">
.category-detail {
  max-width: 860px;
  margin: 0 auto;
  padding: 80px 24px 60px;
}

.category-detail__header {
  margin-bottom: 32px;
}

.category-detail__back {
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

.category-detail__title {
  font-family: var(--font-serif);
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.category-detail__count {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
