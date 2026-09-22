import type { RouteRecordRaw } from 'vue-router'

const readerRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    children: [
      { path: '', name: 'Home', component: () => import('@/views/reader/HomeView.vue') },
      { path: 'archive', name: 'Archive', component: () => import('@/views/reader/ArchiveView.vue'), meta: { title: '时间树' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/reader/CategoriesView.vue'), meta: { title: '分类' } },
      { path: 'categories/:id', name: 'CategoryDetail', component: () => import('@/views/reader/CategoryDetail.vue'), meta: { title: '分类详情' } },
      { path: 'tags', name: 'Tags', component: () => import('@/views/reader/TagsView.vue'), meta: { title: '标签' } },
      { path: 'tags/:id', name: 'TagDetail', component: () => import('@/views/reader/TagDetail.vue'), meta: { title: '标签详情' } },
      { path: 'knowledge/:id', name: 'KnowledgeDetail', component: () => import('@/views/reader/KnowledgeDetail.vue') },
      { path: 'search', name: 'Search', component: () => import('@/views/reader/SearchView.vue'), meta: { title: '搜索' } },
    ],
  },
]

export default readerRoutes
