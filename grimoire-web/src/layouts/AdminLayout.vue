<template>
  <div class="admin-layout">
    <el-container>
      <el-aside width="220px" class="admin-sidebar">
        <div class="admin-logo">Grimoire 管理</div>
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/admin/knowledge">
            <el-icon><Document /></el-icon>
            <span>知识管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/category">
            <el-icon><FolderOpened /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/tag">
            <el-icon><PriceTag /></el-icon>
            <span>标签管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
}

.admin-sidebar {
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  padding-top: 20px;
}

.admin-logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-theme);
  text-align: center;
  padding: 16px 0 24px;
  font-family: var(--font-serif);
}

.admin-main {
  background: var(--bg-page);
  min-height: 100vh;
}
</style>
