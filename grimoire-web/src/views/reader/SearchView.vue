<template>
  <div class="search-view">
    <div class="search-header">
      <h1 class="search-title">搜索</h1>
      <div class="search-input-wrap">
        <el-input
          v-model="keyword"
          placeholder="输入关键词搜索..."
          size="large"
          clearable
          @keyup.enter="doSearch"
          @clear="clearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="doSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <div v-if="hasSearched" class="search-results">
      <el-skeleton :loading="loading" animated :rows="5">
        <template #default>
          <p v-if="total !== '0'" class="search-result-info">
            找到 <strong>{{ total }}</strong> 条结果
          </p>

          <div v-if="records.length" class="search-list">
            <router-link
              v-for="item in records"
              :key="item.id"
              :to="`/knowledge/${item.id}`"
              class="search-item"
            >
              <div class="search-item__header">
                <TypeBadge :type="item.type" :type-desc="item.typeDesc" />
                <h3 class="search-item__title" v-html="highlightTitle(item.title)" />
              </div>
              <p v-if="item.summary" class="search-item__summary" v-html="highlightText(item.summary)" />
              <div v-if="item.categoryPath || item.tags.length" class="search-item__meta">
                <span v-if="item.categoryPath" class="search-item__category">
                  <el-icon><Folder /></el-icon>
                  {{ item.categoryPath }}
                </span>
                <div v-if="item.tags.length" class="search-item__tags">
                  <TagChip
                    v-for="tag in item.tags"
                    :key="tag.id"
                    :tag-name="tag.tagName"
                    :tag-color="tag.tagColor"
                  />
                </div>
              </div>
            </router-link>
          </div>

          <el-empty v-else description="没有找到相关知识" />

          <div v-if="pages > 1" class="search-pagination">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="Number(total)"
              layout="prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Folder } from '@element-plus/icons-vue'
import { findPage } from '@/api/knowledge'
import type { KnowledgeListVo } from '@/api/types/knowledge'
import TypeBadge from '@/components/knowledge/TypeBadge.vue'
import TagChip from '@/components/common/TagChip.vue'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const loading = ref(false)
const records = ref<KnowledgeListVo[]>([])
const total = ref('0')
const currentPage = ref(1)
const pageSize = 10
const hasSearched = ref(false)
const pages = ref(0)

function doSearch() {
  const q = keyword.value.trim()
  if (q) {
    router.push({ path: '/search', query: { q } })
  }
}

function clearSearch() {
  keyword.value = ''
  records.value = []
  total.value = '0'
  hasSearched.value = false
  router.push({ path: '/search' })
}

async function fetchData(page = 1) {
  const q = (route.query.q as string) || ''
  keyword.value = q
  if (!q) {
    hasSearched.value = false
    return
  }
  hasSearched.value = true
  loading.value = true
  try {
    currentPage.value = page
    const result = await findPage({
      current: page,
      size: pageSize,
      keyword: q,
      status: 1,
    })
    records.value = result.records
    total.value = result.total
    pages.value = Number(result.pages)
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  fetchData(page)
}

function highlightTitle(text: string) {
  return highlight(text, (route.query.q as string) || '')
}

function highlightText(text: string) {
  const q = (route.query.q as string) || ''
  if (!q) return text
  // 摘要只高亮第一个匹配并截取上下文
  const idx = text.toLowerCase().indexOf(q.toLowerCase())
  if (idx === -1) return text
  const start = Math.max(0, idx - 40)
  const end = Math.min(text.length, idx + q.length + 80)
  let snippet = (start > 0 ? '...' : '') + text.slice(start, end) + (end < text.length ? '...' : '')
  return highlight(snippet, q)
}

function highlight(text: string, keyword: string) {
  if (!keyword) return text
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

onMounted(() => fetchData(1))
watch(() => route.query.q, () => fetchData(1))
</script>

<style scoped lang="scss">
.search-view {
  max-width: 860px;
  margin: 0 auto;
  padding: 80px 24px 60px;
}

.search-header {
  margin-bottom: 32px;
}

.search-title {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.search-input-wrap {
  max-width: 560px;

  :deep(.el-input-group__append) {
    background: var(--color-theme);
    color: #fff;
    border-color: var(--color-theme);

    .el-button {
      color: #fff;
    }
  }
}

.search-result-info {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 20px;

  strong {
    color: var(--color-theme);
  }
}

.search-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-item {
  display: block;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px 24px;
  text-decoration: none;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--color-theme);
    box-shadow: 0 2px 12px rgba(45, 106, 79, 0.08);
  }
}

.search-item__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.search-item__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;

  :deep(mark) {
    background: rgba(184, 134, 11, 0.25);
    color: inherit;
    padding: 0 2px;
    border-radius: 2px;
  }
}

.search-item__summary {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 10px;

  :deep(mark) {
    background: rgba(184, 134, 11, 0.25);
    color: inherit;
    padding: 0 2px;
    border-radius: 2px;
  }
}

.search-item__meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.search-item__category {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);

  .el-icon {
    font-size: 14px;
  }
}

.search-item__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.search-pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
