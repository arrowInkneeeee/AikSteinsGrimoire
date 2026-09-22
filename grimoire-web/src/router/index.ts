import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import ReaderLayout from '@/layouts/ReaderLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const readerRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: ReaderLayout,
    children: [
      { path: '', name: 'Home', component: () => import('@/views/reader/HomeView.vue') },
      { path: 'archive', name: 'Archive', component: () => import('@/views/reader/PlaceholderView.vue'), meta: { title: '时间树' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/reader/PlaceholderView.vue'), meta: { title: '分类' } },
      { path: 'categories/:id', name: 'CategoryDetail', component: () => import('@/views/reader/PlaceholderView.vue'), meta: { title: '分类详情' } },
      { path: 'tags', name: 'Tags', component: () => import('@/views/reader/PlaceholderView.vue'), meta: { title: '标签' } },
      { path: 'tags/:id', name: 'TagDetail', component: () => import('@/views/reader/PlaceholderView.vue'), meta: { title: '标签详情' } },
      { path: 'knowledge/:id', name: 'KnowledgeDetail', component: () => import('@/views/reader/KnowledgeDetail.vue') },
      { path: 'search', name: 'Search', component: () => import('@/views/reader/PlaceholderView.vue'), meta: { title: '搜索' } },
    ],
  },
]

const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '仪表盘' } },
      { path: 'knowledge', name: 'KnowledgeAdmin', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '知识管理' } },
      { path: 'category', name: 'CategoryAdmin', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '分类管理' } },
      { path: 'tag', name: 'TagAdmin', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '标签管理' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [...readerRoutes, ...adminRoutes],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
