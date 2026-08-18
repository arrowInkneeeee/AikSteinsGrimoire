# Grimoire Web 前端架构设计方案 (v2)

## 设计理念

**「魔典入口」** — 首页极简留白，三大探索路径（时间树 / 分类 / 标签）构成知识发现的核心骨架。

不是博客，不是 CMS 后台。是一本可以翻阅、检索、沉淀的**个人知识魔典**。

参考风格：月見里式的清新沉浸感（插画背景 + 大面积留白 + 清晰导航），但核心交互围绕知识库场景设计。

## 后端 API 现状（已验证）

| 模块 | 路径前缀 | 接口 |
|------|----------|------|
| Knowledge | `/api/knowledge` | add, update, remove, page, findById, toggleStatus |
| DictType | `/grimoire/dictType` | findPage, findById, add, modify, remove |
| DictItem | `/grimoire/dictItem` | findListByType, findPage, add, modify, remove |
| SystemParam | `/grimoire/systemParam` | findPage, findByKey, add, modify, remove, refreshCache |
| File | `/grimoire/file` | upload, download, findPage, remove |

**注意**: Knowledge 使用 `/api/` 前缀，其他模块使用 `/grimoire/` 前缀。前端需在 Vite proxy 中同时代理两个前缀。

### 知识数据模型

- **Knowledge**: id, title, code, type(1笔记/2组件/3方案/4片段), summary, content(TEXT Markdown), source_project, source_path, resource_path, ext_json(JSON), category_id, status
- **Category**: 树形，parent_id=0 为根，category_name, category_code, sort_order
- **Tag**: tag_name(唯一), tag_color, use_count
- **TagRelation**: tag_id + knowledge_id 关联
- **Attachment**: knowledge_id, attach_name, attach_url, description

---

## 目录位置

```
AikSteinsGrimoire/
├── aik-skills-lab/       # 技能库
├── grimoire-files/       # 文档/设计资料
├── grimoire-web/         # <-- 前端项目（新增）
├── sql/
├── src/                  # 后端 Java
└── pom.xml
```

---

## 前端项目结构

```
grimoire-web/
├── src/
│   ├── api/                        # API 层
│   │   ├── request.ts              # Axios 实例 + 拦截器
│   │   ├── types/                  # TypeScript 类型
│   │   │   ├── api.d.ts            # ApiResponse / PageResult 通用类型
│   │   │   ├── knowledge.d.ts
│   │   │   ├── category.d.ts
│   │   │   ├── tag.d.ts
│   │   │   └── system.d.ts
│   │   ├── knowledge.ts
│   │   ├── category.ts
│   │   ├── tag.ts
│   │   ├── dict.ts
│   │   ├── systemParam.ts
│   │   └── file.ts
│   ├── assets/styles/
│   │   ├── variables.scss          # 主题 CSS 变量（亮/暗双套）
│   │   ├── element-override.scss   # Element Plus 主题覆盖
│   │   ├── markdown.scss           # Markdown 渲染样式
│   │   ├── timeline.scss           # 时间轴专用样式
│   │   └── global.scss
│   ├── components/
│   │   ├── layout/
│   │   │   ├── ReaderLayout.vue    # 阅读端：顶部导航 + 页面内容
│   │   │   ├── AdminLayout.vue     # 管理端：侧栏导航 + 内容区
│   │   │   └── GrimoireNav.vue     # 顶部导航栏（首页/时间树/分类/标签/搜索/管理）
│   │   ├── knowledge/
│   │   │   ├── KnowledgeCard.vue   # 知识条目卡片
│   │   │   ├── KnowledgeItem.vue   # 时间树中的条目行
│   │   │   └── TypeBadge.vue       # 类型胶囊标签
│   │   ├── archive/
│   │   │   └── TimeTree.vue        # 时间轴组件（年份分组 + 纵向线条）
│   │   ├── markdown/
│   │   │   ├── MarkdownViewer.vue  # Markdown 渲染 + 代码高亮
│   │   │   └── TocPanel.vue        # 文章目录浮动面板
│   │   └── common/
│   │       ├── SearchBox.vue       # 搜索框组件（首页大尺寸 + 导航栏小尺寸两种模式）
│   │       ├── TagChip.vue         # 标签胶囊
│   │       └── StatsBar.vue        # 统计摘要条
│   ├── composables/
│   │   ├── usePagination.ts
│   │   ├── useDict.ts
│   │   ├── useCategoryTree.ts
│   │   └── useSearch.ts            # 搜索防抖 + 跳转
│   ├── router/
│   │   ├── index.ts
│   │   ├── reader.ts
│   │   └── admin.ts
│   ├── stores/
│   │   ├── app.ts                  # 全局状态（暗色模式、导航栏状态）
│   │   ├── category.ts             # 分类树缓存
│   │   └── dict.ts                 # 字典项缓存
│   ├── views/
│   │   ├── reader/
│   │   │   ├── HomeView.vue       # 首页：沉浸式背景 + 搜索框 + 三入口
│   │   │   ├── ArchiveView.vue    # 时间树：按年-月分组的纵向时间轴
│   │   │   ├── CategoriesView.vue # 分类总览：分类卡片网格
│   │   │   ├── CategoryDetail.vue # 分类详情：该分类下的条目列表
│   │   │   ├── TagsView.vue       # 标签云：彩色加权胶囊
│   │   │   ├── TagDetail.vue      # 标签详情：该标签关联的条目列表
│   │   │   ├── KnowledgeDetail.vue # 知识详情：Markdown + 代码 + 附件
│   │   │   └── SearchView.vue     # 搜索结果列表
│   │   └── admin/
│   │       ├── DashboardView.vue
│   │       ├── knowledge/
│   │       │   ├── KnowledgeListAdmin.vue
│   │       │   └── KnowledgeEdit.vue
│   │       ├── category/CategoryAdmin.vue
│   │       ├── tag/TagAdmin.vue
│   │       └── system/
│   │           ├── DictTypeAdmin.vue
│   │           ├── DictItemAdmin.vue
│   │           ├── SystemParamAdmin.vue
│   │           └── FileAdmin.vue
│   ├── utils/
│   │   ├── date.ts
│   │   └── tree.ts
│   ├── App.vue
│   └── main.ts
├── .env.development
├── .env.production
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── .gitignore
```

---

## 页面信息架构

```
Grimoire 魔典
|
|-- [首页]               极简入口：背景 + 搜索框 + 三入口导航
|-- [时间树] /archive    按年-月时间轴浏览知识条目（核心功能）
|-- [分类]   /categories 分类目录 + 各分类下条目数 + 点击进入
|-- [标签]   /tags        标签云 + 按 use_count 加权 + 点击进入
|-- [详情]   /knowledge/:id  知识条目详情（Markdown + 代码 + 附件）
|-- [搜索]   /search?q=  全局搜索结果
|
|-- [管理]   /admin/*     后台管理（独立 Layout，不影响阅读体验）
```

---

## 路由规划

### 阅读端（ReaderLayout）

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | HomeView | 极简首页：沉浸式背景 + 居中搜索框 + 三入口（时间树/分类/标签） |
| `/archive` | ArchiveView | 时间树：按年份分组的时间轴，纵向滚动浏览全部知识条目 |
| `/categories` | CategoriesView | 分类总览：分类列表 + 各分类条目数，点击进入分类详情 |
| `/categories/:id` | CategoryDetailView | 分类详情：该分类下的知识条目列表 |
| `/tags` | TagsView | 标签云：彩色胶囊标签，按 use_count 加权大小 |
| `/tags/:id` | TagDetailView | 标签详情：该标签关联的知识条目列表 |
| `/knowledge/:id` | KnowledgeDetail | 知识详情：Markdown 渲染 + 代码高亮 + TOC + 附件 |
| `/search?q=xxx` | SearchView | 搜索结果：按关键词匹配的条目列表 |

### 管理端（AdminLayout，`/admin` 前缀）

| 路径 | 页面 |
|------|------|
| `/admin` | 重定向到 dashboard |
| `/admin/dashboard` | 统计仪表盘（条目数/分类数/标签数/最近编辑） |
| `/admin/knowledge` | 知识条目管理列表 |
| `/admin/knowledge/create` | 新建知识条目 |
| `/admin/knowledge/:id/edit` | 编辑知识条目 |
| `/admin/category` | 分类树管理 |
| `/admin/tag` | 标签管理 |
| `/admin/system/dict-type` | 字典类型管理 |
| `/admin/system/dict-item` | 字典项管理 |
| `/admin/system/param` | 系统参数管理 |
| `/admin/system/file` | 文件管理 |

---

## 技术选型

| 类别 | 方案 | 说明 |
|------|------|------|
| 框架 | Vue 3.4+ + TypeScript 5 | Composition API, `<script setup>` |
| 构建 | Vite 5 | 快速 HMR |
| UI | Element Plus 2.7+ | 按需自动导入 |
| 路由 | Vue Router 4 | |
| 状态 | Pinia 2 | 仅缓存分类树/字典等低频数据 |
| HTTP | Axios | 响应拦截器解包 ApiResponse |
| Markdown 渲染 | markdown-it + highlight.js | 插件：anchor、toc、task-lists |
| Markdown 编辑 | md-editor-v3 | Vue 3 原生，内置工具栏/预览/分屏 |
| 工具 | @vueuse/core, dayjs | 防抖/暗色模式/日期处理 |
| 图标 | @iconify/vue (MDI) | 按需加载 |
| 样式 | SCSS | Element Plus 主题覆盖 |

### API 封装要点

- 响应拦截器：`code===200 && success===true` 时解包返回 `data`，否则 `ElMessage.error(msg)`
- Vite 代理：`/api` 和 `/grimoire` 两个前缀均代理到 `http://localhost:18900`
- 雪花 ID 精度：后端返回 JSON 时 Long 需序列化为 String（或使用 BigInt），避免 JS Number 精度丢失

### 状态管理（轻量）

- **App Store**: sidebarCollapsed, readerMode(card/list), darkMode
- **Category Store**: tree 缓存，selectedId；启动时加载一次，管理端修改后标记失效
- **Dict Store**: Map<string, DictItem[]> 缓存，按 dictCode 懒加载

---

## 视觉风格 — 「命运石魔典」

### 设计语言

融合月見里式的清新沉浸感与魔典主题的神秘质感：
- 大面积留白/背景 + 清晰的内容层级
- 亮色模式为主（日常阅读舒适），暗色模式可选
- 卡片轻微阴影 + 微妙边框，保持轻盈感
- 类型标签用彩色胶囊区分：笔记=绿, 组件=蓝, 方案=紫, 片段=橙

### 亮色模式（默认）

| 用途 | 色值 | 说明 |
|------|------|------|
| 页面背景 | `#f8f6f1` | 羊皮纸暖白（魔典质感） |
| 卡片背景 | `#ffffff` | 纯白 |
| 主文字 | `#2c2c2c` | 深灰近黑 |
| 次文字 | `#8a8a8a` | 辅助信息 |
| 主题色 | `#2d6a4f` | 古典墨绿（魔典主题） |
| 强调色 | `#b8860b` | 金色点缀 |
| 时间轴线 | `#2d6a4f` → `#d4a574` 渐变 | 从近到远由深变浅 |
| 边框色 | `#e8e4dc` | 柔和分割 |

### 暗色模式

| 用途 | 色值 | 说明 |
|------|------|------|
| 页面背景 | `#0a0e17` | 深邃星空黑 |
| 卡片背景 | `#121a2b` | 深蓝色面板 |
| 主文字 | `#e0e6f0` | 浅灰白 |
| 主题色 | `#00d4aa` | Steins;Gate 磷光绿 |
| 强调色 | `#4fc3f7` | 科技蓝 |

### 字体

- 标题: `"Noto Sans SC", sans-serif` — 清晰现代
- 正文: `"Noto Serif SC", Georgia, serif` — 阅读友好（魔典翻阅感）
- 代码: `"JetBrains Mono", "Fira Code", monospace`

### 首页背景方案

首页使用沉浸式背景，可选方案：
1. **CSS 渐变** — 墨绿到深蓝的柔和渐变 + 微粒子动画（最轻量）
2. **自定义插画** — 魔典/星空/符文主题的插画（需设计素材）
3. **纯色 + 纹理** — 羊皮纸纹理背景（契合魔典主题，加载最快）

---

## 核心页面详细设计

### 首页 (HomeView) — 「魔典封面」

极简沉浸式入口，不展示任何知识条目列表。

```
+-------------------------------------------------------------------+
|  [Grimoire]                                        [管理入口 >]    |
+-------------------------------------------------------------------+
|                                                                    |
|                                                                    |
|              ~~~ 沉浸式背景（可自定义插画/渐变）~~~                |
|                                                                    |
|                                                                    |
|                    AikSteins Grimoire                              |
|                    个 人 知 识 魔 典                               |
|                                                                    |
|          +------------------------------------------------+        |
|          |  🔍  搜索你的知识...                            |        |
|          +------------------------------------------------+        |
|                                                                    |
|         [ 📅 时间树 ]     [ 📂 分类 ]     [ 🏷️ 标签 ]            |
|                                                                    |
|                    ↓ 向下探索                                      |
|                                                                    |
+-------------------------------------------------------------------+
```

设计要点：
- 页面进入时只有背景 + 标题 + 搜索框 + 三个入口图标
- 搜索框居中，宽度约 600px，回车或点击跳转 `/search?q=xxx`
- 三个入口图标是导航锚点，点击分别跳转到 /archive、/categories、/tags
- 向下滚动时淡入显示统计摘要（如「已沉淀 42 条知识 · 9 个分类 · 51 个标签」）
- 背景风格可自定义：渐变色 / 静态插画 / 粒子动画（契合魔典主题）

### 时间树 (ArchiveView) — 「知识年轮」

按时间轴纵向组织全部知识条目，是最核心的浏览方式。

```
+-------------------------------------------------------------------+
|  [Grimoire]   首页    时间树    分类    标签         [管理入口]    |
+-------------------------------------------------------------------+
|                                                                    |
|    时间树                                                          |
|    ───────                                                         |
|                                                                    |
|    全部知识 · 42 条                                                |
|                                                                    |
|    ● 2026                                                          |
|    |                                                               |
|    |  07-30  [笔记]  Spring Boot 自动配置原理深度解析              |
|    |         分类: Java > Spring  |  标签: Java, Spring            |
|    |                                                               |
|    |  07-15  [组件]  ThreadPoolExecutor 线程池封装方案             |
|    |         分类: 组件手册  |  标签: Java, 并发                   |
|    |                                                               |
|    |  06-20  [方案]  分布式锁选型：Redis vs ZooKeeper             |
|    |         分类: 技术方案  |  标签: 分布式, Redis                |
|    |                                                               |
|    ● 2025                                                          |
|    |                                                               |
|    |  12-10  [片段]  MyBatis-Plus 自定义 SQL 注入器代码片段        |
|    |         分类: 代码片段  |  标签: MyBatis, Java                |
|    |                                                               |
|    |  11-05  [笔记]  Go 语言 GMP 调度模型学习笔记                 |
|    |         分类: 编程语言 > Go  |  标签: Go, 底层原理            |
|    |                                                               |
|    ● 2024                                                          |
|    |                                                               |
|    |  ...                                                          |
|                                                                    |
+-------------------------------------------------------------------+
```

设计要点：
- 左侧纵向时间轴贯穿页面，年份节点用主题色圆点标记
- 每个条目显示：日期 + 类型标签(TypeBadge) + 标题 + 分类路径 + 标签
- 年份节点可折叠/展开（默认展开最近一年）
- 点击条目跳转到知识详情页
- 顶部显示「全部知识 · N 条」统计
- 时间轴线条使用主题色渐变（从亮到暗，暗示时间远近）

### 分类总览 (CategoriesView) — 「知识图谱」

展示分类树结构 + 各分类下条目数量。

```
+-------------------------------------------------------------------+
|  [Grimoire]   首页    时间树    分类    标签         [管理入口]    |
+-------------------------------------------------------------------+
|                                                                    |
|    分类                                                            |
|    ─────                                                           |
|                                                                    |
|    +-------------------+  +-------------------+                    |
|    | 📂 Java           |  | 📂 前端            |                   |
|    |    12 条知识       |  |    8 条知识        |                   |
|    |  > Spring (5)     |  |  > Vue (4)        |                   |
|    |  > MyBatis (3)    |  |  > CSS (2)        |                   |
|    |  > 并发 (4)       |  |  > 工具 (2)       |                   |
|    +-------------------+  +-------------------+                    |
|                                                                    |
|    +-------------------+  +-------------------+                    |
|    | 📂 组件手册       |  | 📂 技术方案        |                   |
|    |    6 条知识        |  |    5 条知识        |                   |
|    +-------------------+  +-------------------+                    |
|                                                                    |
|    +-------------------+                                           |
|    | 📂 代码片段       |                                           |
|    |    11 条知识       |                                           |
|    +-------------------+                                           |
|                                                                    |
+-------------------------------------------------------------------+
```

设计要点：
- 分类以卡片形式展示，每个卡片显示分类名、条目数、子分类列表
- 卡片网格布局（2-3 列），hover 时微放大 + 边框发光
- 点击分类卡片进入该分类下的知识条目列表（复用时间树布局，但过滤到该分类）
- 支持树形展示：有子分类的分类，卡片内展示子分类及各自条目数

### 标签云 (TagsView) — 「知识索引」

所有标签的可视化展示，按使用频率加权。

```
+-------------------------------------------------------------------+
|  [Grimoire]   首页    时间树    分类    标签         [管理入口]    |
+-------------------------------------------------------------------+
|                                                                    |
|    标签                                                            |
|    ─────                                                           |
|                                                                    |
|    [Java]  [Spring]  [Go]  [Docker]  [Vue]  [MySQL]               |
|         [MyBatis]      [Redis]     [并发]   [分布式]              |
|    [LLM]   [Agent]  [设计模式]  [数据结构]  [算法]                |
|       [Kubernetes]  [Nginx]   [Linux]   [Git]                     |
|    [底层原理]  [TypeScript]  [React]  [Python]  [SQL]             |
|         [微服务]   [REST API]   [测试]   [DevOps]                 |
|                                                                    |
|    ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─                         |
|                                                                    |
|    51 个标签 · 按使用频率排列                                      |
|                                                                    |
+-------------------------------------------------------------------+
```

设计要点：
- 标签以彩色胶囊形式展示，颜色取自 tag_color 字段
- 字号按 use_count 加权：使用次数越多字号越大（类似标签云效果）
- 点击标签进入该标签关联的知识条目列表
- 底部显示标签总数统计
- hover 标签时显示 tooltip：「N 条知识使用此标签」

### 知识详情页 (KnowledgeDetail) — 「翻开魔典」

```
+-------------------------------------------------------------------+
|  < 返回                                          [编辑]           |
+-------------------------------------------------------------------+
|                                                                    |
|    标题 (H1)                                                       |
|    [笔记]  [Java]  [Spring]                                        |
|    2026-07-30  |  分类: Java > Spring                              |
|                                                                    |
|    ── 摘要 ──                                                      |
|    简要描述...                                                     |
|                                                                    |
|    ── 正文 (Markdown, max-width: 860px 居中) ──                    |
|    ## 章节标题                                                     |
|    段落内容...                                                     |
|    ```java [复制]                                                  |
|    // 代码高亮块                                                   |
|    ```                                                             |
|                                                                    |
|    ── 附件 ──                                                      |
|    [icon] file.pdf (2.3MB) [下载]                                  |
|                                                                    |
|    ── 相关知识 ──                                                  |
|    · 相关文章1                                                     |
|    · 相关文章2                                                     |
|                                                                    |
+-------------------------------------------------------------------+
```

设计要点：
- 正文区最大宽度 860px 居中，保证阅读体验
- 代码块支持一键复制 + 语法高亮
- 屏幕宽度 >= 1200px 时右侧显示 TOC 浮动面板
- 底部「相关知识」：基于相同分类/标签推荐 2-3 条关联条目

## 设计文档落地

本方案保存至 `grimoire-files/plans/frontend-design.md`，作为前端项目的持久化设计参考文档。

（注：gf = grimoire-files，后续对话中 gf 均指代 grimoire-files 目录）

---

## 分阶段实施

### Phase 1: 基础骨架 + 首页（3-4 天）
1. Vite + Vue 3 + TS 初始化，Element Plus 按需导入 + 主题覆盖
2. Axios 封装 + ApiResponse 拦截器 + 类型定义
3. 路由配置（Reader + Admin 双 Layout）+ GrimoireNav 导航栏
4. 首页 HomeView：沉浸式背景 + 搜索框 + 三入口导航
5. 知识详情页 KnowledgeDetail：Markdown 渲染 + 代码高亮 + TOC

### Phase 2: 三大核心浏览路径（4-5 天）
1. 时间树 ArchiveView + TimeTree 组件（年份分组 + 纵向时间轴 + 折叠展开）
2. 分类总览 CategoriesView + CategoryDetail（分类卡片 + 条目列表）
3. 标签云 TagsView + TagDetail（加权标签云 + 关联条目列表）
4. 搜索结果 SearchView（关键词匹配 + 高亮展示）

### Phase 3: 管理后台（4-5 天）
1. 知识条目管理列表 + 编辑器（md-editor-v3 + 分类选择 + 标签输入 + 附件上传）
2. 分类树管理
3. 标签管理

### Phase 4: 体验增强（2-3 天）
1. 暗色/亮色模式切换
2. TOC 浮动面板 + 代码块一键复制
3. 页面切换动画 + NProgress + 响应式适配
4. 首页向下滚动淡入统计摘要

### Phase 5: 系统管理 + 收尾（2 天）
1. 字典/参数/文件管理页面
2. Dashboard 统计
3. 空状态/骨架屏/错误边界

---

## 注意事项

1. **API 前缀不一致**: Knowledge 用 `/api/`，其他用 `/grimoire/`。Vite proxy 需同时代理两个前缀，建议后续统一后端为 `/grimoire/knowledge`
2. **雪花 ID 精度**: JS Number 上限 2^53，雪花 ID 为 64 位。后端 JSON 序列化需将 Long 转为 String，或前端使用 BigInt 处理
3. **Markdown XSS**: 渲染时需使用 DOMPurify 过滤，防止注入
4. **附件下载**: 后端返回文件流，前端需用 blob 方式处理
