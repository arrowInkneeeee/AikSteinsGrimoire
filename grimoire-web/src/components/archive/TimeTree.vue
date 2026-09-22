<template>
  <div class="time-tree">
    <div
      v-for="(group, index) in yearGroups"
      :key="group.year"
      class="time-tree__year"
    >
      <!-- 年份节点 -->
      <div class="time-tree__node" @click="toggleYear(group.year)">
        <span class="time-tree__dot" />
        <span class="time-tree__year-label">{{ group.year }}</span>
        <span class="time-tree__count">{{ group.items.length }} 条</span>
        <el-icon class="time-tree__arrow" :class="{ 'is-collapsed': !expandedYears.has(group.year) }">
          <ArrowDown />
        </el-icon>
      </div>

      <!-- 条目列表 -->
      <Transition name="expand">
        <div v-if="expandedYears.has(group.year)" class="time-tree__items">
          <div
            v-for="item in group.items"
            :key="item.id"
            class="time-tree__item"
          >
            <div class="time-tree__item-line" />
            <div class="time-tree__item-content">
              <div class="time-tree__item-header">
                <span class="time-tree__date">{{ formatDate(item.createTime) }}</span>
                <TypeBadge :type="item.type" :type-desc="item.typeDesc" />
                <router-link
                  :to="`/knowledge/${item.id}`"
                  class="time-tree__title"
                >
                  {{ item.title }}
                </router-link>
              </div>
              <div v-if="item.categoryPath || item.tags.length" class="time-tree__item-meta">
                <span v-if="item.categoryPath" class="time-tree__category">
                  <el-icon><Folder /></el-icon>
                  {{ item.categoryPath }}
                </span>
                <div v-if="item.tags.length" class="time-tree__tags">
                  <TagChip
                    v-for="tag in item.tags"
                    :key="tag.id"
                    :tag-name="tag.tagName"
                    :tag-color="tag.tagColor"
                  />
                </div>
              </div>
            </div>
          </div>
          <!-- 末项封闭线 -->
          <div v-if="index < yearGroups.length - 1" class="time-tree__item">
            <div class="time-tree__item-line time-tree__item-line--last" />
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowDown, Folder } from '@element-plus/icons-vue'
import TypeBadge from '@/components/knowledge/TypeBadge.vue'
import TagChip from '@/components/common/TagChip.vue'
import type { KnowledgeListVo } from '@/api/types/knowledge'

const props = defineProps<{
  items: KnowledgeListVo[]
}>()

/** 按年份分组（降序） */
const yearGroups = computed(() => {
  const map = new Map<string, KnowledgeListVo[]>()
  for (const item of props.items) {
    const year = item.createTime.slice(0, 4)
    if (!map.has(year)) map.set(year, [])
    map.get(year)!.push(item)
  }
  return Array.from(map.entries())
    .map(([year, items]) => ({ year, items }))
    .sort((a, b) => Number(b.year) - Number(a.year))
})

/** 默认展开最近一年 */
const expandedYears = ref(new Set<string>([yearGroups.value[0]?.year].filter(Boolean)))

function toggleYear(year: string) {
  if (expandedYears.value.has(year)) {
    expandedYears.value.delete(year)
  } else {
    expandedYears.value.add(year)
  }
}

function formatDate(dateStr: string) {
  return dateStr.slice(5, 10) // MM-DD
}
</script>

<style scoped lang="scss">
.time-tree {
  padding: 20px 0;
}

.time-tree__year {
  margin-bottom: 8px;
}

.time-tree__node {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  cursor: pointer;
  user-select: none;

  &:hover .time-tree__year-label {
    color: var(--color-theme);
  }
}

.time-tree__dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--color-theme);
  flex-shrink: 0;
  box-shadow: 0 0 0 3px rgba(45, 106, 79, 0.15);
}

.time-tree__year-label {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  transition: color 0.2s;
}

.time-tree__count {
  font-size: 13px;
  color: var(--text-secondary);
}

.time-tree__arrow {
  margin-left: auto;
  font-size: 14px;
  color: var(--text-secondary);
  transition: transform 0.3s;

  &.is-collapsed {
    transform: rotate(-90deg);
  }
}

.time-tree__items {
  position: relative;
  padding-left: 6px;
}

.time-tree__item {
  display: flex;
  position: relative;
}

.time-tree__item-line {
  width: 2px;
  background: linear-gradient(180deg, var(--color-theme) 0%, var(--border-color) 100%);
  margin-left: 6px;
  flex-shrink: 0;
}

.time-tree__item-line--last {
  height: 24px;
  background: var(--border-color);
}

.time-tree__item-content {
  flex: 1;
  padding: 10px 0 10px 20px;
}

.time-tree__item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.time-tree__date {
  font-size: 13px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
  min-width: 42px;
}

.time-tree__title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  transition: color 0.2s;

  &:hover {
    color: var(--color-theme);
  }
}

.time-tree__item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.time-tree__category {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);

  .el-icon {
    font-size: 14px;
  }
}

.time-tree__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 2000px;
}
</style>
