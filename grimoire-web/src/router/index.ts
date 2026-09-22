import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import ReaderLayout from '@/layouts/ReaderLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import readerRoutes from './reader'
import adminRoutes from './admin'

// 为阅读端路由包裹 ReaderLayout
const readerWithLayout: RouteRecordRaw[] = [
  {
    path: '/',
    component: ReaderLayout,
    children: readerRoutes[0].children!,
  },
]

// 为管理端路由包裹 AdminLayout
const adminWithLayout: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: AdminLayout,
    redirect: adminRoutes[0].redirect,
    children: adminRoutes[0].children!,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [...readerWithLayout, ...adminWithLayout],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
