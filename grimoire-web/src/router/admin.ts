import type { RouteRecordRaw } from 'vue-router'

const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '仪表盘' } },
      { path: 'knowledge', name: 'KnowledgeAdmin', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '知识管理' } },
      { path: 'category', name: 'CategoryAdmin', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '分类管理' } },
      { path: 'tag', name: 'TagAdmin', component: () => import('@/views/admin/PlaceholderView.vue'), meta: { title: '标签管理' } },
    ],
  },
]

export default adminRoutes
