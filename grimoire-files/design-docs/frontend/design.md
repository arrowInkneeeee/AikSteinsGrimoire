# Grimoire Web 前端架构设计方案 (v2)

> 版本：v2.3 | 更新：2026-09-20 | 状态：生效中 | 权威范围：前端页面、路由、组件划分、视觉风格、附件交互
> v2 → v2.1：路径迁移（`plans/` → `design-docs/frontend/`）、补权威边界、登记 15 项待修订
> v2.1 → v2.2：**附件交互定稿**——附件数据模型改两层映射（去 `knowledge_id` / `attach_url`）、新增 §附件交互（零 URL 拼接，下载/预览统一走 `/grimoire/file/download`）、消清全部 `url` 字段引用；处置 R01（附件子集）/ R02 / R16，新登记 R17–R18
> v2.2 → v2.3：**R18 解决**——`AttachmentVo` 由 6 字段扩为 **8 字段**（补 `fileSize` / `fileType`，随附件列表返回、**无需**额外请求），对齐 api-contract **v1.2.2** §2.5.3；§附件交互 字段表、§知识数据模型 Attachment 行、§知识详情页 附件区同步
>
> 接口契约权威：[`../system-module/api-contract.md`](../system-module/api-contract.md)（本文档不定义接口，§后端 API 现状仅为索引）
> 附件域设计权威：[`../system-module/SDD.md`](../system-module/SDD.md) §2.5《附件两层模型与归属边界》
> ✅ **附件相关章节已于 v2.3 定稿，可作为实施依据**：§后端 API 现状（Attachment 行）、§知识数据模型、§附件交互、§核心页面详细设计 › 知识详情页（附件区）、§注意事项 4。
> ⚠️ 其余章节仍存在 18 项已确认待修订缺陷中的 15 项未处置或部分处置（R02 / R16 / R18 已修订，R01 部分处置），清单见文末 §待修订项。未处置项完成前，相关章节不得作为实施依据。

## 设计理念

**「魔典入口」** — 首页极简留白，三大探索路径（时间树 / 分类 / 标签）构成知识发现的核心骨架。

不是博客，不是 CMS 后台。是一本可以翻阅、检索、沉淀的**个人知识魔典**。

参考风格：月見里式的清新沉浸感（插画背景 + 大面积留白 + 清晰导航），但核心交互围绕知识库场景设计。

## 后端 API 现状（已验证）

| 模块 | 路径前缀 | 接口 |
|------|----------|------|
| Knowledge | `/grimoire/knowledge` | add, update, remove, page, findById, toggleStatus |
| DictType | `/grimoire/dictType` | findPage, findById, add, modify, remove |
| DictItem | `/grimoire/dictItem` | findListByType, findPage, add, modify, remove |
| SystemParam | `/grimoire/systemParam` | findPage, findByKey, add, modify, remove, refreshCache |
| File | `/grimoire/file` | upload, download, findPage, rename, remove |
| Attachment | `/grimoire/attachment` | **findByBiz, save, remove**（v2.2 新增行） |

**注意**: 所有模块统一使用 `/grimoire/` 前缀，Vite proxy 只需代理 `/grimoire` 即可。

> **附件链路的入参形状（v2.2 补充）**：下载与预览**共用同一个端点**
> `GET /grimoire/file/download`，入参 = `id`（= `fileId`，必填）、`attachId`（**从附件列表发起时必带**）、
> `preview`（`true` = 内联预览）。前端**不拼接任何访问地址**，完整约定见 [§附件交互](#附件交互v22-定稿)。
> 本表仍缺 **HTTP 方法列**与 **Category / Tag 行**（未处置的 R01），见文末 §待修订项。

### 知识数据模型

> **命名约定（v2.2）**：本清单按**接口字段名**（camelCase，即前端实际消费的形态）书写；数据库列的 snake_case
> 形态（`file_id` / `biz_type` / `biz_id` / `attach_name` / `description` / `sort_order`）与出参形状对照见
> [`SDD.md`](../system-module/SDD.md) §2.3、[`api-contract.md`](../system-module/api-contract.md) §2.5 / §2.6。

- **Knowledge**: id, title, code, type(1笔记/2组件/3方案/4片段), typeDesc, summary, content(TEXT Markdown), sourceProject, sourcePath, resourcePath, extJson(JSON), categoryId, categoryName, categoryPath, status, tags(List<TagBriefVo>), **attachments(List&lt;AttachmentVo&gt;)**
- **Category**: 树形，parentId=0 为根，categoryName, categoryCode, sortOrder
- **Tag**: tagName(唯一), tagColor, useCount
- **TagRelation**: tagId + knowledgeId 关联
- **Attachment（挂载行 `aik_sys_attachment`，v2.2 重写）**
  - **后端承载列**：`file_id` / `biz_type` / `biz_id` / `attach_name` / `description` / `sort_order`（+ `del_flag` + 审计 4 列）
  - **已删除列**：`knowledge_id`、`attach_url`（破坏性 DDL **[M1]** 已于 2026-09-20 落地；**表归属 system 模块**，不是 knowledge 模块的表）
  - **前端可见字段**（= `AttachmentVo`，**8 项**，见 §附件交互）：`id`（= `attachId`，挂载行主键）/ `fileId` / `attachName` / `fileSize` / `fileType` / `description` / `sortOrder` / `createTime`
    —— 其中 `fileSize` / `fileType` 属**文件层**（由 `fileId` 关联 `aik_sys_file` 带出，**不落在挂载表列上**）
  - **明确不含**：`url` / `attachUrl` / `knowledgeId` / `filePath` / `storedName` / `md5` / `originalName`——契约里**不得出现任何路径或访问地址形态**
  - ⚠️ `biz_type` / `biz_id` 是**表列**（后端存储与查询需要它们），但**不回显给前端**：这是 **API 暴露面**决策，**不是删列**
  - 挂载是**多对多**：同一份文件可被多条业务引用；显示名 `attachName` **每条挂载各自记录**（权威归属**挂载层**）
- **File（文件对象 `aik_sys_file`）**: id（= `fileId`，挂载锚点）, originalName（**首次上传者**的台账名，**不得**用作附件显示名）, fileSize(string), fileType(MIME), storageType(local/oss/sftp 策略标签), downloadCount, createTime
  - **不得出现**：`url` / `filePath` / `storedName` / `md5`（v2.2 删除；`getUrl()` 方法链一并删除）

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

## 附件交互（v2.2 定稿）

> **设计权威**：附件域**两层模型**与字段归属 = [`SDD.md`](../system-module/SDD.md) §2.5；
> HTTP 形状 = [`api-contract.md`](../system-module/api-contract.md) §2.5。本文档只描述**前端如何调用**，不定义接口。
> 本节为 v2.2 新增，取代原 §注意事项 4 的一句话表述。

### 附件数据流（三段）

```
① 上传   POST /grimoire/file/upload          (multipart，字段名 file)
          → ApiResponse<FileVo>.data.id       ← 记作 fileId
          上传【不产生挂载行】：fileId 只是【绑定锚点】，必须先拿到它

② 挂载   KnowledgeDto.attachments = [ { fileId, attachName, description, sortOrder } ]
          （或通用入口 POST /grimoire/attachment/save）
          服务端在【同一事务】内写挂载行；保存成功后需重新拉取详情

③ 展示   GET /grimoire/knowledge/findById → data.attachments : AttachmentVo[]
```

- 上传出参**不含** `url` / `filePath` / `storedName` / `md5`（api-contract §2.5.5）——**锚点只有 `fileId`**。
- 上传白名单与体积上限由后端配置控制（当前 `jpg,jpeg,png,gif,webp,pdf,txt,md,zip,json,doc,docx,xls,xlsx,ppt,pptx`；
  单文件 10MB、请求 50MB），前端应在选择文件时先行校验并给出提示。

### `AttachmentVo`（附件列表元素的唯一形态）

| 前端字段 | TS 类型 | 用途 |
|---|---|---|
| `id` | `string` | 挂载行主键（= `attachId`）：下载、预览、卸载都用它 |
| `fileId` | `string` | 文件对象 id：下载与预览的**定位锚点** |
| `attachName` | `string?` | **用户可见文件名**（权威归属挂载层；服务端据此设置响应文件名） |
| `fileSize` | `string?` | **字节数的字符串形式**（Long→String，见 api-contract §2.1.3，与 §API 封装要点 的 ID 约定同源）——**文件层**属性，由服务端按 `fileId` 关联 `aik_sys_file` 带出；用于附件区的"2.3MB" |
| `fileType` | `string?` | **MIME 类型**——同为文件层属性；用于类型图标与"能否内联预览"的判断 |
| `description` | `string?` | 附件说明（如"第三版修订稿"） |
| `sortOrder` | `number?` | 排序，缺省 0 |
| `createTime` | `string?` | 绑定建立时间 |

- **共 8 个字段**（`AttachmentVo` 的字段集以 api-contract §2.5.3 为准）。
- **不含** `url` / `attachUrl` / `knowledgeId` / `filePath` / `storedName` / `md5` / `originalName`。
- `fileSize` 展示时用 `Number(fileSize)` 换算单位（字符串只用于传输，避免 Long 精度丢失）。
- `fileSize` / `fileType` 是**内容属性**：不泄露存储布局、不随上传者变化，故可安全暴露（见 §待修订项 R18 的处置）。
- `bizType` / `bizId` 是挂载表的**列**（后端存储与查询用），**不回显**；知识附件列表**不需要**前端传 `bizType`
  （由 knowledge 服务固定为 `AttachmentBizType.KNOWLEDGE`）。
- TS 类型按 §API 封装要点：可选字段一律带 `?`，`id` / `fileId` / `fileSize` 是 `string`。

### 下载 / 预览：**零 URL 拼接**

**前端任何时候都不得拼接访问地址。** 下载与预览是同一个端点，只差 `preview` 参数：

```
预览   GET /grimoire/file/download?id={fileId}&attachId={attachId}&preview=true
下载   GET /grimoire/file/download?id={fileId}&attachId={attachId}
```

| 参数 | 必填性 | 说明 |
|---|---|---|
| `id` | **必填** | = `fileId` |
| `attachId` | **附件列表场景必带** | 服务端据此把响应文件名设为该挂载行的 `attach_name`；**不带**则回落为 `aik_sys_file.original_name`（首次上传者的名字，md5 秒传后会串名） |
| `preview` | 可省（默认 `false`） | `true` → `Content-Disposition: inline`（图片 / PDF 浏览器内渲染）；缺省 → `attachment`（触发下载框） |

- **文件名由服务端决定**：前端**不传**文件名，也**不**自己拼 `Content-Disposition`。
- `attachId` 不存在、已卸载（`del_flag = 1`）、或与 `id` 不匹配 → 服务端返回 `code != 200`，走统一错误模型。
- **必须带 `attachId` 的真实原因**：md5 秒传让同一份字节只存一行，`original_name` 只属于**第一个上传者**；
  不带 `attachId` 就等于让第二个挂载者下载到**别人的文件名**——这正是本次改造要消灭的现象。

### blob 处理约定（前端**必须**遵守）

```ts
// api/file.ts
export function downloadFile(params: { id: string; attachId?: string; preview?: boolean }) {
  return request.get('/grimoire/file/download', {
    params,                // { id, attachId, preview }
    responseType: 'blob',  // ← 关键：该请求不走 ApiResponse 解包分支
  })
}
```

1. **`responseType: 'blob'`**：文件流不是 `ApiResponse<T>` JSON，必须让这个请求跳过响应拦截器的解包逻辑
   （拦截器按 `code === 200` 解包 JSON，而 blob 响应没有 `code`）。
2. **错误体也是 blob**：`GlobalExceptionHandler` 让**所有错误都是 HTTP 200**（契约 §2.1.2），
   下载失败时拿到的仍是一个 `Blob`。
   **判定成功的权威依据 = 响应头存在 `Content-Disposition`**（成功路径必设，错误路径不设）；
   **不能**只看 `Content-Type`——`.json` 附件的正确响应同样是 `application/json`。
   兜底：无 `Content-Disposition` 时 `await blob.text()` → `JSON.parse` → 取 `msg` 弹错。
3. **文件名**优先取 `Content-Disposition` 的 `filename*`（UTF-8 百分号编码，需 `decodeURIComponent`），
   取不到再回落组件里的 `attachName`。
4. 落地：`URL.createObjectURL(blob)` → 临时 `<a download={name}>` → `click()` → **必须** `URL.revokeObjectURL()`（否则内存泄漏）。
5. **预览**：与下载走同一套 blob 约定（`preview=true` + `responseType: 'blob'`），再 `window.open(URL.createObjectURL(blob))`。
   ⚠️ **不要**让 `window.open` / `<iframe src>` 直接指向端点：失败时同样是 HTTP 200 的 JSON，
   浏览器会把 JSON 原文渲染成页面，绕过统一错误提示。
6. `downloadCount` 由**服务端**累加，前端不传、不维护。

### 卸载 / 删除的前端契约

| 动作 | 调用 | 前端注意 |
|---|---|---|
| 卸载单个附件 | `POST /grimoire/attachment/remove`，body `{"id":"<attachId>"}` | 服务端顺带做**引用计数**：文件仍被别处挂载则保留；**最后一份**引用消失才删文件记录与磁盘文件 |
| 附件随知识条目一起改 | `KnowledgeDto.attachments`（**差量**语义） | 前端**不得**用"全删再全插"模拟，也**不得**假设后端"整组替换"——`uk_biz_file` 不含 `del_flag`，整组替换必撞唯一键（契约 §2.5.2）。传空数组 = 卸载该业务全部挂载 |
| 文件台账删除 | `POST /grimoire/file/remove`，body `{"id":"<fileId>"}` | 仍有有效挂载时**会失败**（删除守卫），属预期行为：前端展示 `msg`，**不要**当作 bug 重试 |

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
|    [icon] 我的笔记.pdf (2.3MB)  [预览] [下载]                      |
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
- **附件区（v2.2 定稿；v2.3 补齐大小 / 类型来源）**：
  - 逐条渲染 `AttachmentVo`，显示名用 **`attachName`**（**不得**用 `originalName`），并按 `sortOrder` 升序排列
  - 「预览」= 同端点的 `preview=true`；「下载」= 缺省 `attachment`。两者**都调 `/grimoire/file/download`，页面内不出现任何 URL/路径字符串**
  - 列表为空时显示空状态（"暂无附件"），不显示占位文件名
  - ✅ 条目上的"(2.3MB)"与**类型图标**直接取自响应的 **`fileSize` / `fileType`**（v2.3 起已在 `AttachmentVo` 内，**无需**额外请求；R18 已解决）
  - 类型图标按 MIME 映射（图片 / PDF / 文档 / 压缩包 / 其它）；是否可内联预览同样由 `fileType` 判定

## 设计文档落地

本文档位于 `grimoire-files/design-docs/frontend/design.md`，是前端页面、路由、组件与视觉风格的权威文档。

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

1. **API 前缀已统一**: 所有模块均使用 `/grimoire/` 前缀，Vite proxy 只需配置 `/grimoire` 代理。
2. **雪花 ID 精度**: 已通过 Jackson 全局配置将 Long 序列化为 String，前端所有 ID 字段按 `string` 类型处理
3. **Markdown XSS**: 渲染时需使用 DOMPurify 过滤，防止注入
4. **附件下载 / 预览**: 后端返回**二进制流**（blob），前端**不拼接任何访问地址**——统一走 `GET /grimoire/file/download`（完整约定见 §附件交互）。

---

## 待修订项

> 本表登记已确认的缺陷：**R01–R15** 来源 [`api-contract.md`](../system-module/api-contract.md) §6.1，
> **R16–R18** 为 v2.2 附件交互定稿时在本文档内新登记。
> **状态列**：`✅ 已修订` = 已按契约定稿；`⚠️ 部分修订` = 仅附件子集已定稿；`⚠️ 需决策` = 阻塞在决策上；`待修订` = 未执行。
> **未处置项（状态非 `✅`）对应的章节，不得作为实施依据。**

| # | 位置 | 问题 | 修订方向 | 状态 |
|---|---|---|---|---|
| R01 | §后端 API 现状 | 表头写"已验证"，但缺 Category / Tag / Attachment，且无 HTTP 方法列 | 补方法列、标注已实现/未实现、改为引用契约 | ⚠️ 部分修订（v2.2 已补 Attachment 行、File 的 `rename` 与下载入参；**Category / Tag 行与方法列仍缺**） |
| R02 | §知识数据模型 | `Knowledge` 写成扁平模型 | 改为契约中的实际形态（含 `attachments`） | ✅ 已修订（v2.2：按 api-contract §2.6.2 / §2.6.3 改为 camelCase 接口字段名，补 `tags` / `attachments`；同节增 `Attachment` / `File` 两层模型） |
| R03 | §API 封装要点 | 只说"ID 字段按 string" | 补：分页元数据 `total`/`size`/`current`/`pages` 也是字符串 | 待修订 |
| R04 | §API 封装要点 | 未提错误模型 | 补：所有错误均 HTTP 200，判定依据是 `code` | 待修订 |
| R05 | §API 封装要点 | 未提 null 语义 | 补：`NON_NULL` 序列化，字段全可选 | 待修订 |
| R06 | §API 封装要点 vs §注意事项 1 | **自相矛盾**（`/api` + `/grimoire` vs 仅 `/grimoire`） | 统一为仅代理 `/grimoire` | 待修订 |
| R07 | §视觉风格 色板 | `#8a8a8a`(3.20:1)、`#b8860b`(3.01:1)、`#d4a574`(2.06:1) 均不达 WCAG AA | 次文字降至约 `#6b6b6b`；金色降至 `#8a6209` 以下或限定为装饰 | 待修订 |
| R08 | §暗色模式 | 卡片 `#121a2b` 对页面 `#0a0e17` 仅 1.11:1，"轻微阴影"不可见 | 暗色改用可见边框替代阴影 | 待修订 |
| R09 | §首页设计要点 | **自相矛盾**："不展示任何知识条目列表" vs "向下滚动淡入统计" | 单屏封面 或 滚动落地页，二选一 | ⚠️ 需决策 |
| R10 | §类型标签配色 | 只有"绿/蓝/紫/橙"，无色值 | 补亮/暗两套具体 hex | 待修订 |
| R11 | §分阶段实施 | 5 个 Phase 无后端前置，Phase 1 交付三个死链 | 插入契约 §4 的 Phase 0-6 为前置，重排前端 Phase | 待修订 |
| R12 | §注意事项 3 | 只说用 DOMPurify | 补：或在 markdown-it 禁 `html`（更省的防线） | 待修订 |
| R13 | §目录结构 | 缺 `vite-env.d.ts` / `auto-imports.d.ts` / `components.d.ts` / `tsconfig.node.json` | 补齐 | 待修订 |
| R14 | §目录结构 `api/types/*.d.ts` | 承载被 import 的业务类型应为 `.ts` | 改为 `.ts` | 待修订 |
| R15 | §分阶段实施 | 无测试 / lint / CI / 包管理器声明 | 补充 | 待修订 |
| R16 | §注意事项 4 | 附件下载只有一句"后端返回文件流，前端用 blob 处理"——缺 `responseType`、拦截器绕过、错误体判定、文件名取值 | 展开为完整约定 | ✅ 已修订（v2.2：新增 §附件交互 › blob 处理约定，含 6 条硬约束） |
| R17 | §目录结构 | 附件链路**缺文件**：`api/attachment.ts`、`api/types/attachment.ts`、附件列表组件（如 `components/attachment/AttachmentList.vue` / `AttachmentUploader.vue`） | 补入 `api/`（按 R14 用 `.ts`）与 `components/attachment/`，并在 §附件交互 的约定下实现 | 待修订 |
| R18 | §核心页面详细设计 › 知识详情页 附件区 | 条目展示"名称 (2.3MB) + 类型图标"，但 v1.2 / v2.2 时 `AttachmentVo` **不含 `fileSize` / `fileType`** → **无数据来源** | 三选一（裁决与理由见 api-contract **v1.2.2** §2.5.3）：① 附件区不展示大小/类型；② 前端按 `fileId` 走 `/grimoire/file/findPage` 补取；③ 后端补字段 | ✅ **已解决（v2.3）**：采用 **③ 后端补字段** —— api-contract §2.5.3 把 `AttachmentVo` 6 → **8 字段**（补 `fileSize` / `fileType`，由 `aik_sys_file` 关联带出、**不新增挂载表列**）。**否决 ①**（砍掉先于契约已定的前端设计）、**否决 ②**（**N+1 请求**，且 `/file/findPage` 语义是管理端分页而非按 id 批量取元数据）。前端侧**无需额外请求**，字段随附件列表一并返回 |

**执行前置**：R01–R05 依赖契约定稿（已定稿），R11 依赖契约 §4 的 Phase 划分；**R09（首页形态）需用户决策**。
R18 已于 v2.3 由船长裁决解决（采用"后端补字段"，契约 api-contract v1.2.2 §2.5.3 已落地）。
R17 只依赖本文档 §附件交互，可随时执行。

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v2 | — | 初版（含现状核实） |
| v2.1 | 2026-09-20 | 迁移至 `design-docs/frontend/`；补标准头与权威边界；登记 15 项待修订 |
| v2.2 | 2026-09-20 | **附件交互定稿**（对应 api-contract v1.2 / v1.2.1 §2.5 / §2.6、SDD v1.3 §2.5）：§知识数据模型 `Attachment` 行由 `knowledge_id` / `attach_name` / `attach_url` 改为两层映射（后端列 `file_id` / `biz_type` / `biz_id` / `attach_name` / `description` / `sort_order`，前端字段 `id` / `fileId` / `attachName` / `description` / `sortOrder` / `createTime`），并补 `File` 行；§后端 API 现状 补 Attachment 行与下载入参；**新增 §附件交互**（零 URL 拼接、`attachId` 必带、blob 6 条约定、卸载/删除契约）；§知识详情页 附件区补预览与数据来源告警；**消清全部 `url` 字段引用**；§待修订项 增状态列并登记 R16–R18（R02 / R16 已修订，R01 部分修订）；同步 api-contract §2.5.2 / §2.5.3 的 `findByBiz` 过滤与排序口径 |
| v2.3 | 2026-09-20 | **R18 解决（`AttachmentVo` 6 → 8 字段，对齐 api-contract v1.2.2）**：§附件交互 的字段表补 `fileSize`（bytes 的字符串形式，`Number()` 换算展示）与 `fileType`（MIME，用于类型图标与可预览性判定），并写明二者属**文件层**、由服务端按 `fileId` 关联 `aik_sys_file` 带出；§知识数据模型 `Attachment` 行的前端可见字段由 6 项改 **8 项**；§知识详情页 附件区去掉"无数据来源"告警、改为直接取自响应；§待修订项 R18 状态改 **✅ 已解决**（采用"后端补字段"，并写明否决 N+1 补取与砍设计两项的理由）；「执行前置」同步。**未改接口定义、未改 SDD（由 t15 跟随）、未改代码** |
