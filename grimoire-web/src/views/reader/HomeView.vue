<template>
  <div class="home-view">
    <!-- 第一屏：Hero -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">AikSteins Grimoire</h1>
        <p class="hero-subtitle">aIk知识魔典</p>
        <SearchBox mode="hero" />
        <div class="hero-entries">
          <router-link to="/archive" class="hero-entry">
            <el-icon :size="28"><Clock /></el-icon>
            <span>时间树</span>
          </router-link>
          <router-link to="/categories" class="hero-entry">
            <el-icon :size="28"><FolderOpened /></el-icon>
            <span>分类</span>
          </router-link>
          <router-link to="/tags" class="hero-entry">
            <el-icon :size="28"><PriceTag /></el-icon>
            <span>标签</span>
          </router-link>
        </div>
      </div>
      <div class="scroll-indicator">
        <el-icon :size="24"><ArrowDown /></el-icon>
      </div>
    </section>

    <!-- 第二屏：知识全景 -->
    <section ref="panoramaRef" class="panorama" :class="{ 'is-visible': isVisible }">
      <div class="panorama-inner">
        <h2 class="section-title">知识全景</h2>

        <!-- 统计卡片 -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-number">{{ animatedKnowledge }}</div>
            <div class="stat-label">知识条目</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ animatedCategory }}</div>
            <div class="stat-label">个分类</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ animatedTag }}</div>
            <div class="stat-label">个标签</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ animatedAttachment }}</div>
            <div class="stat-label">个附件</div>
          </div>
        </div>

        <!-- 类型分布 -->
        <div v-if="statsData?.typeDistribution?.length" class="type-distribution">
          <h3 class="sub-title">类型分布</h3>
          <div class="type-bars">
            <div v-for="item in statsData.typeDistribution" :key="item.type" class="type-bar-row">
              <span class="type-label">{{ item.typeDesc }}</span>
              <div class="type-bar-track">
                <div
                  class="type-bar-fill"
                  :style="{ width: typePercent(item.count) + '%', backgroundColor: typeColor(item.type) }"
                />
              </div>
              <span class="type-count">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <!-- 最近编辑 -->
        <div v-if="statsData?.recentEdited?.length" class="recent-edited">
          <h3 class="sub-title">最近编辑</h3>
          <div class="recent-list">
            <router-link
              v-for="item in statsData.recentEdited"
              :key="item.id"
              :to="`/knowledge/${item.id}`"
              class="recent-item"
            >
              <span class="recent-title">{{ item.title }}</span>
              <span class="recent-time">{{ formatDate(item.modifyTime) }}</span>
            </router-link>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'
import dayjs from 'dayjs'
import { stats as fetchStats } from '@/api/knowledge'
import type { KnowledgeStatsVo } from '@/api/types/knowledge'
import SearchBox from '@/components/common/SearchBox.vue'

const statsData = ref<KnowledgeStatsVo | null>(null)
const panoramaRef = ref<HTMLElement | null>(null)
const isVisible = ref(false)

// 数字动画
const animatedKnowledge = ref(0)
const animatedCategory = ref(0)
const animatedTag = ref(0)
const animatedAttachment = ref(0)

function animateNumber(target: number, setter: (v: number) => void, duration = 1200) {
  const start = performance.now()
  const step = (now: number) => {
    const progress = Math.min((now - start) / duration, 1)
    setter(Math.round(target * progress))
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

// 滚动触发
useIntersectionObserver(panoramaRef, ([{ isIntersecting }]) => {
  if (isIntersecting && !isVisible.value) {
    isVisible.value = true
    if (statsData.value) {
      animateNumber(statsData.value.knowledgeCount, v => animatedKnowledge.value = v)
      animateNumber(statsData.value.categoryCount, v => animatedCategory.value = v)
      animateNumber(statsData.value.tagCount, v => animatedTag.value = v)
      animateNumber(statsData.value.attachmentCount, v => animatedAttachment.value = v)
    }
  }
})

onMounted(async () => {
  try {
    statsData.value = await fetchStats()
  } catch {
    // 静默处理
  }
})

function typePercent(count: number): number {
  if (!statsData.value?.knowledgeCount) return 0
  return Math.round((count / statsData.value.knowledgeCount) * 100)
}

function typeColor(type: number): string {
  const map: Record<number, string> = { 1: '#2d6a4f', 2: '#1565c0', 3: '#7b1fa2', 4: '#e65100' }
  return map[type] || '#999'
}

function formatDate(dateStr: string): string {
  return dayjs(dateStr).format('MM-DD')
}
</script>

<style scoped lang="scss">
.hero {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: url('/images/rei-bg.jpg') center right / cover no-repeat;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, rgba(0, 0, 0, 0.8) 0%, rgba(0, 0, 0, 0.5) 35%, rgba(0, 0, 0, 0.1) 65%, transparent 100%);
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: left;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;
  margin-left: 8vw;
  max-width: 720px;
}

.hero-title {
  font-family: var(--font-serif);
  font-size: 48px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  text-shadow: 0 2px 16px rgba(0, 0, 0, 0.5);
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 3px;
  margin-bottom: 12px;
}

.hero-entries {
  display: flex;
  gap: 40px;
  margin-top: 20px;
}

.hero-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  transition: all 0.3s;

  &:hover {
    color: #fff;
    transform: translateY(-4px);
  }
}

.scroll-indicator {
  position: absolute;
  bottom: 40px;
  color: rgba(255, 255, 255, 0.6);
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(8px); }
}

// 第二屏
.panorama {
  padding: 80px 24px;
  opacity: 0;
  transform: translateY(40px);
  transition: all 0.8s ease;

  &.is-visible {
    opacity: 1;
    transform: translateY(0);
  }
}

.panorama-inner {
  max-width: 800px;
  margin: 0 auto;
}

.section-title {
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 40px;

  &::after {
    content: '';
    display: block;
    width: 40px;
    height: 2px;
    background: var(--color-theme);
    margin: 12px auto 0;
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 48px;
}

.stat-card {
  text-align: center;
  padding: 24px 16px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.stat-number {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-theme);
  font-family: var(--font-serif);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.sub-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.type-distribution {
  margin-bottom: 40px;
}

.type-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.type-label {
  width: 48px;
  font-size: 13px;
  color: var(--text-secondary);
  text-align: right;
}

.type-bar-track {
  flex: 1;
  height: 8px;
  background: #eee;
  border-radius: 4px;
  overflow: hidden;
}

.type-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1.2s ease;
}

.type-count {
  width: 28px;
  font-size: 13px;
  color: var(--text-secondary);
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover {
    background: rgba(45, 106, 79, 0.05);
  }
}

.recent-title {
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 600px;
}

.recent-time {
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
  margin-left: 16px;
}
</style>
