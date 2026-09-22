<template>
  <nav class="grimoire-nav" :class="{ 'is-scrolled': isScrolled }">
    <div class="nav-inner">
      <router-link to="/" class="nav-logo">Grimoire</router-link>
      <div class="nav-links">
        <router-link to="/">首页</router-link>
        <router-link to="/archive">时间树</router-link>
        <router-link to="/categories">分类</router-link>
        <router-link to="/tags">标签</router-link>
        <router-link to="/search" class="nav-search">
          <el-icon :size="16"><Search /></el-icon>
          <span>搜索</span>
        </router-link>
        <router-link to="/admin" class="nav-admin">管理</router-link>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useWindowScroll } from '@vueuse/core'
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const { y } = useWindowScroll()
// 首页：滚动后才变毛玻璃；其他页面：始终毛玻璃
const isScrolled = computed(() => route.path === '/' ? y.value > 60 : true)
</script>

<style scoped lang="scss">
.grimoire-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 60px;
  transition: all 0.3s ease;
  background: transparent;

  &.is-scrolled {
    background: rgba(248, 246, 241, 0.85);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border-color);
    box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
  }
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.nav-logo {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 700;
  color: var(--color-theme);
  letter-spacing: 1px;

  // Hero 区域白色
  .grimoire-nav:not(.is-scrolled) & {
    color: #fff;
    text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  }
}

.nav-links {
  display: flex;
  gap: 28px;

  a {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-secondary);
    transition: color 0.2s;

    // Hero 区域白色
    .grimoire-nav:not(.is-scrolled) & {
      color: rgba(255, 255, 255, 0.9);
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);

      &:hover,
      &.router-link-active {
        color: #fff;
      }
    }

    &:hover,
    &.router-link-active {
      color: var(--color-theme);
    }
  }

  .nav-admin {
    opacity: 0.7;

    &:hover {
      opacity: 1;
    }
  }

  .nav-search {
    display: inline-flex;
    align-items: center;
    gap: 3px;
  }
}
</style>
