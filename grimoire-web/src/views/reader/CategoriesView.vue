<template>
  <div class="categories-view">
    <div class="categories-header">
      <h1 class="categories-title">分类</h1>
      <p class="categories-subtitle">{{ categories.length }} 个根分类</p>
    </div>

    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <div v-if="categories.length" class="categories-grid">
          <router-link
            v-for="cat in categories"
            :key="cat.id"
            :to="`/categories/${cat.id}`"
            class="category-card"
          >
            <div class="category-card__header">
              <el-icon class="category-card__icon"><FolderOpened /></el-icon>
              <h3 class="category-card__name">{{ cat.categoryName }}</h3>
            </div>
            <p class="category-card__count">{{ cat.totalCount }} 条知识</p>
            <div v-if="cat.children?.length" class="category-card__children">
              <span
                v-for="child in cat.children"
                :key="child.id"
                class="category-card__child"
              >
                {{ child.categoryName }} ({{ child.totalCount }})
              </span>
            </div>
          </router-link>
        </div>
        <el-empty v-else description="还没有分类" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { FolderOpened } from '@element-plus/icons-vue'
import { findTree } from '@/api/category'
import type { CategoryTreeVo } from '@/api/types/category'

const loading = ref(true)
const categories = ref<CategoryTreeVo[]>([])

onMounted(async () => {
  try {
    categories.value = await findTree()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.categories-view {
  max-width: 960px;
  margin: 0 auto;
  padding: 80px 24px 60px;
}

.categories-header {
  margin-bottom: 32px;
}

.categories-title {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.categories-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.category-card {
  display: block;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 24px;
  transition: all 0.25s ease;
  text-decoration: none;

  &:hover {
    transform: translateY(-2px);
    border-color: var(--color-theme);
    box-shadow: 0 4px 16px rgba(45, 106, 79, 0.1);
  }
}

.category-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.category-card__icon {
  font-size: 22px;
  color: var(--color-theme);
}

.category-card__name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.category-card__count {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 12px;
}

.category-card__children {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-card__child {
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(45, 106, 79, 0.06);
  padding: 2px 8px;
  border-radius: 8px;
}
</style>
