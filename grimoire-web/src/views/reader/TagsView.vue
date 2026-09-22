<template>
  <div class="tags-view">
    <div class="tags-header">
      <h1 class="tags-title">标签</h1>
    </div>

    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <div v-if="tags.length" class="tag-cloud">
          <router-link
            v-for="tag in tags"
            :key="tag.id"
            :to="`/tags/${tag.id}`"
            class="tag-cloud__item"
            :style="tagStyle(tag)"
          >
            <el-tooltip :content="`${tag.useCount} 条知识使用此标签`" placement="top">
              <span>{{ tag.tagName }}</span>
            </el-tooltip>
          </router-link>
        </div>
        <el-empty v-else description="还没有标签" />
      </template>
    </el-skeleton>

    <p v-if="tags.length" class="tags-footer">{{ tags.length }} 个标签 · 按使用频率排列</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { findAll } from '@/api/tag'
import type { TagVo } from '@/api/types/tag'

const loading = ref(true)
const tags = ref<TagVo[]>([])

onMounted(async () => {
  try {
    tags.value = await findAll()
  } finally {
    loading.value = false
  }
})

const maxCount = computed(() => Math.max(...tags.value.map(t => t.useCount), 1))

const MIN_FONT = 14
const MAX_FONT = 26

function tagStyle(tag: TagVo) {
  const ratio = tag.useCount / maxCount.value
  const fontSize = MIN_FONT + ratio * (MAX_FONT - MIN_FONT)
  const color = tag.tagColor || '#2d6a4f'
  return {
    fontSize: `${fontSize}px`,
    color,
    backgroundColor: color + '15',
    borderColor: color + '30',
  }
}
</script>

<style scoped lang="scss">
.tags-view {
  max-width: 860px;
  margin: 0 auto;
  padding: 80px 24px 60px;
}

.tags-header {
  margin-bottom: 32px;
}

.tags-title {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}

.tag-cloud__item {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 16px;
  border: 1px solid;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;
  line-height: 1.6;

  &:hover {
    transform: translateY(-1px);
    opacity: 0.85;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
}

.tags-footer {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 24px;
}
</style>
