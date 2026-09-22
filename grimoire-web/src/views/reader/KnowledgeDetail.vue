<template>
  <div class="knowledge-detail" v-if="knowledge">
    <div class="detail-container">
      <!-- 顶部信息 -->
      <header class="detail-header">
        <router-link to="/" class="back-link">
          <el-icon><ArrowLeft /></el-icon> 返回
        </router-link>
        <h1 class="detail-title">{{ knowledge.title }}</h1>
        <div class="detail-meta">
          <TypeBadge :type="knowledge.type" :type-desc="knowledge.typeDesc" />
          <TagChip
            v-for="tag in knowledge.tags"
            :key="tag.id"
            :tag-name="tag.tagName"
            :tag-color="tag.tagColor"
          />
          <span class="meta-date">{{ formatDate(knowledge.createTime) }}</span>
          <span v-if="knowledge.categoryPath" class="meta-category">
            <el-icon :size="14"><FolderOpened /></el-icon>
            {{ knowledge.categoryPath }}
          </span>
        </div>
      </header>

      <!-- 摘要 -->
      <div v-if="knowledge.summary" class="detail-summary">
        <p>{{ knowledge.summary }}</p>
      </div>

      <!-- 正文 Markdown -->
      <article v-if="knowledge.content" class="detail-content markdown-body" v-html="renderedContent" />

      <!-- 附件区 -->
      <section v-if="knowledge.attachments?.length" class="detail-attachments">
        <h3 class="section-heading">附件</h3>
        <div class="attachment-list">
          <div
            v-for="att in sortedAttachments"
            :key="att.id"
            class="attachment-item"
          >
            <el-icon :size="20" class="att-icon"><Document /></el-icon>
            <div class="att-info">
              <span class="att-name">{{ att.attachName || '未命名附件' }}</span>
              <span v-if="att.fileSize" class="att-size">{{ formatFileSize(att.fileSize) }}</span>
            </div>
            <div class="att-actions">
              <el-button size="small" text @click="handlePreview(att)">预览</el-button>
              <el-button size="small" text @click="handleDownload(att)">下载</el-button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- TOC 浮动面板 -->
    <aside v-if="tocItems.length && showToc" class="toc-panel">
      <h4 class="toc-title">目录</h4>
      <ul class="toc-list">
        <li
          v-for="item in tocItems"
          :key="item.id"
          class="toc-item"
          :class="{ 'is-active': activeHeading === item.id }"
          :style="{ paddingLeft: (item.level - 1) * 12 + 'px' }"
        >
          <a :href="`#${item.id}`">{{ item.text }}</a>
        </li>
      </ul>
    </aside>
  </div>

  <div v-else class="loading-state">
    <el-skeleton :rows="8" animated />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import dayjs from 'dayjs'
import { findById } from '@/api/knowledge'
import { getBlob } from '@/api/request'
import type { KnowledgeVo, AttachmentVo } from '@/api/types/knowledge'
import TypeBadge from '@/components/knowledge/TypeBadge.vue'
import TagChip from '@/components/common/TagChip.vue'

const route = useRoute()
const knowledge = ref<KnowledgeVo | null>(null)
const showToc = useMediaQuery('(min-width: 1200px)')
const activeHeading = ref('')

// Markdown 渲染器
const md: MarkdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch { /* fallback */ }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  },
})

// TOC 提取
interface TocItem { id: string; text: string; level: number }
const tocItems = ref<TocItem[]>([])

const renderedContent = computed(() => {
  if (!knowledge.value?.content) return ''
  const html = md.render(knowledge.value.content)
  // 提取标题生成 TOC
  const headings: TocItem[] = []
  const withIds = html.replace(/<h([1-4])>(.*?)<\/h\1>/g, (_match: string, level: string, text: string) => {
    const id = `heading-${headings.length}`
    headings.push({ id, text: text.replace(/<[^>]+>/g, ''), level: parseInt(level) })
    return `<h${level} id="${id}">${text}</h${level}>`
  })
  tocItems.value = headings
  return withIds
})

// 附件按 sortOrder 排序
const sortedAttachments = computed(() => {
  if (!knowledge.value?.attachments) return []
  return [...knowledge.value.attachments].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
})

function formatDate(dateStr: string): string {
  return dayjs(dateStr).format('YYYY-MM-DD')
}

function formatFileSize(sizeStr: string): string {
  const bytes = Number(sizeStr)
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i]
}

async function handlePreview(att: AttachmentVo) {
  try {
    const resp = await getBlob('/file/download', { id: att.fileId, attachId: att.id, preview: true })
    const blob = new Blob([resp.data])
    const url = URL.createObjectURL(blob)
    window.open(url)
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch { /* error handled by interceptor */ }
}

async function handleDownload(att: AttachmentVo) {
  try {
    const resp = await getBlob('/file/download', { id: att.fileId, attachId: att.id })
    const blob = new Blob([resp.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.attachName || 'download'
    a.click()
    URL.revokeObjectURL(url)
  } catch { /* error handled by interceptor */ }
}

onMounted(async () => {
  const id = route.params.id as string
  try {
    knowledge.value = await findById(id)
  } catch {
    // error handled
  }
})
</script>

<style scoped lang="scss">
.knowledge-detail {
  padding-top: 80px;
  min-height: 100vh;
}

.detail-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

.loading-state {
  max-width: 860px;
  margin: 120px auto;
  padding: 0 24px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;

  &:hover {
    color: var(--color-theme);
  }
}

.detail-title {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
  margin-bottom: 16px;
}

.detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
}

.meta-date {
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-category {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.detail-summary {
  padding: 16px 20px;
  background: rgba(45, 106, 79, 0.04);
  border-left: 3px solid var(--color-theme);
  border-radius: 0 8px 8px 0;
  margin-bottom: 32px;

  p {
    font-size: 15px;
    color: var(--text-secondary);
    line-height: 1.8;
  }
}

.detail-content {
  font-family: var(--font-serif);
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);

  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    font-family: var(--font-sans);
    margin-top: 2em;
    margin-bottom: 0.5em;
  }

  :deep(p) {
    margin-bottom: 1em;
  }

  :deep(pre.hljs) {
    border-radius: 8px;
    padding: 16px;
    margin: 1em 0;
    overflow-x: auto;
    font-family: var(--font-mono);
    font-size: 14px;
  }

  :deep(code) {
    font-family: var(--font-mono);
    font-size: 0.9em;
    background: rgba(0,0,0,0.04);
    padding: 2px 6px;
    border-radius: 4px;
  }

  :deep(pre.hljs code) {
    background: none;
    padding: 0;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    margin: 1em 0;
  }

  :deep(blockquote) {
    border-left: 3px solid var(--border-color);
    padding-left: 16px;
    color: var(--text-secondary);
    margin: 1em 0;
  }
}

// 附件区
.section-heading {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 40px 0 16px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  transition: border-color 0.2s;

  &:hover {
    border-color: var(--color-theme);
  }
}

.att-icon {
  color: var(--text-secondary);
}

.att-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.att-name {
  font-size: 14px;
  color: var(--text-primary);
}

.att-size {
  font-size: 12px;
  color: var(--text-secondary);
}

// TOC 浮动面板
.toc-panel {
  position: fixed;
  top: 100px;
  right: 24px;
  width: 220px;
  max-height: calc(100vh - 140px);
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.toc-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.toc-list {
  list-style: none;
  padding: 0;
}

.toc-item {
  margin-bottom: 4px;

  a {
    font-size: 12px;
    color: var(--text-secondary);
    line-height: 1.6;
    display: block;
    padding: 2px 0;

    &:hover {
      color: var(--color-theme);
    }
  }

  &.is-active a {
    color: var(--color-theme);
    font-weight: 600;
  }
}
</style>
