<template>
  <div class="search-box" :class="[`search-box--${mode}`]">
    <el-input
      v-model="keyword"
      :placeholder="mode === 'hero' ? '搜索你的知识...' : '搜索...'"
      :size="mode === 'hero' ? 'large' : 'default'"
      clearable
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

defineProps<{
  mode?: 'hero' | 'nav'
}>()

const router = useRouter()
const keyword = ref('')

function handleSearch() {
  const q = keyword.value.trim()
  if (q) {
    router.push({ path: '/search', query: { q } })
  }
}
</script>

<style scoped lang="scss">
.search-box {
  width: 100%;

  &--hero {
    max-width: 600px;
    margin: 0 auto;

    :deep(.el-input__wrapper) {
      border-radius: 24px;
      padding: 8px 20px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
      background: rgba(255, 255, 255, 0.95);
    }
  }

  &--nav {
    max-width: 240px;

    :deep(.el-input__wrapper) {
      border-radius: 16px;
    }
  }
}
</style>
