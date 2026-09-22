<template>
  <div class="reader-layout">
    <GrimoireNav />
    <router-view />
  </div>
</template>

<script setup lang="ts">
import GrimoireNav from '@/components/layout/GrimoireNav.vue'
</script>

<style scoped lang="scss">
.reader-layout {
  min-height: 100vh;
}
</style>
