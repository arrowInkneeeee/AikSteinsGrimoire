# Knowledge API 契约补齐方案（P0）

> 版本：v1.2.2 | 更新：2026-09-20 | 状态：生效中 | 权威范围：全部 HTTP 接口（路径/方法/入参/出参/错误模型）
> v1.0 → v1.1：迁入 `design-docs/system-module/`（原 `plans/backend-api-contract.md`）；下游文档路径随本次整合更新；工作区行尾噪音计数按实测修正（183，非 186）
> v1.1 → v1.2：**附件契约与 `SDD.md` v1.2 两层模型对齐**（§2.5 整体重写）——三端点收敛为 `findByBiz` / `save` / `remove`；`AttachmentVo` 删 `attachUrl` / `knowledgeId`（6 字段）；`save` 由"整组替换"改**差量**（旧语义会撞 `uk_biz_file`）；删「`attachName` 由 `originalName` 回填」；下载出 `attachId` 规则（挂载层必带）；`FileVo` 删 `url` / `filePath` / `storedName` / `md5`；`/file/remove` 收紧为台账入口。§3 的 M1 由"加列"改写为**破坏性重写**，并按实测纠正 M2/M4 状态；§4 Phase 4 验收与对账 SQL 重写；§6.3 改为反向校准；§1.3 D7 处置更新
> 附注（同日）：起草期间 `SDD.md` 升到 **v1.3**——〔K1〕`getUrl()` 方法链由"保留"改为**删除**；〔K2〕附件域迁移**已落地**，`aik_sys_attachment` 已迁入 `sql/aik_system_tables.sql`。本文件已按 v1.3 同步（§2.5.1 / §2.5.5 / §3 / §3.0 / §4 / §6.3 / §7）。
> v1.2 → v1.2.1（**补注，船长复核后追加**）：§2.5.2 把 `findByBiz` 的两条硬约束**显式化**——**只返回 `del_flag = 0`** 的有效挂载行（`AttachmentVo` 不含 `del_flag`，前端无从自行过滤，否则已卸载附件会重新出现在列表里）、**`ORDER BY sort_order ASC, id ASC`**；§2.5.3 补"不回显 `bizType`/`bizId` **≠** 删列"的澄清与 `AttachmentBizType.KNOWLEDGE` 作为白名单常量唯一来源（供实现直接引用，不必翻 SDD）。
> v1.2.1 → v1.2.2（**契约缺陷修复 R18**）：`AttachmentVo` 由 **6 字段扩为 8 字段**——恢复 `fileSize`（string）与 `fileType`（MIME），二者**由 `aik_sys_file` 关联带出**（`aik_sys_attachment` 仍是 12 列，不新增列）；§2.5.2 增加"**按 `fileId` 批量取**文件元数据、**禁止 N+1**"的实现约束；§2.5.3 把 `originalName` 单独列为**不得出现**（与 `fileSize`/`fileType` 的处置**分开**）。**SDD §4.5 的"6 字段"口径由 t15 跟随同步**（本文件不改 SDD）。
>
> **本文档是 AikSteinsGrimoire 后端 HTTP 契约的唯一权威来源。**
>
> 此前契约分散在三处且全部漂移：`plans/project-architecture.md`（表名/接口已被取代）、
> `design-docs/knowledge-module/design.md`（只拥有包结构，无接口清单）、
> `design-docs/system-module/SDD.md`（只拥有 system 模块的表结构与分层）。
> 本文档收敛这三处来源；`project-architecture.md` 同时标记为 superseded。
>
> 文档体系总入口：[`../../README.md`](../../README.md)

---

## 0. 溯源信息

| 项 | 值 |
|---|---|
| 编制日期 | 2026-09-20 |
| 基线提交 | `a95f1d7`（branch `main`） |
| 工作区状态 | 183 个文件显示已修改，**全部为 CRLF/LF 行尾噪音，零内容变更**（逐文件剥离 CR 后哈希比对，0 个真实差异；见 Phase 0 与 §7.2） |
| 核实方式 | 逐文件对读 `src/main/java`、`sql/`、`_import_temp.sql` 原始内容，非引用历史文档 |

### 0.1 变更必要性

前端方案 `grimoire-files/design-docs/frontend/design.md` 定义了三条核心浏览路径（时间树 / 分类 / 标签），
但其中**两条在 HTTP 层完全不存在**：`KnowledgeCategoryMapper`、`KnowledgeTagMapper`、
`KnowledgeTagRelationMapper`、`SysAttachmentMapper` 只有 Mapper，没有任何 Controller 或 Service。

与此同时，`knowledge-module-design.md` 第 35-42 行**早已规定**这些文件应当存在：

```
knowledge/
├── controller/
│   ├── KnowledgeController.java    # 知识条目 CRUD + 聚合查询
│   ├── CategoryController.java     # 分类树          ← 从未实现
│   └── TagController.java          # 标签管理        ← 从未实现
├── service/
│   ├── CategoryService.java                          ← 从未实现
│   └── TagService.java                               ← 从未实现
```

**因此本方案不是新增功能范围，而是补齐一份已批准设计中的未完成部分。**
在契约固化之前开始前端开发，会导致类型定义、VO 整形、路由三处返工——这是选择"后端先行"的原因。

### 0.2 影响面

| 层 | 影响 |
|---|---|
| `knowledge` 模块 | 新增 3 组 Controller/Service；修改 `KnowledgeQuery` / `KnowledgeDto` / `KnowledgeVo` / `KnowledgeListVo`；`KnowledgeVo.attachments` 由 `List<SysAttachmentPo>` 改为 `List<AttachmentVo>`（§2.5.3、§2.6.2） |
| `system` 模块 | `aik_sys_attachment` **重写为通用挂载表**（`file_id` + `biz_type` + `biz_id` + `del_flag`）；新增 Attachment Controller/Service（三端点，§2.5.2）；`FileVo` **收缩暴露面**（删 `url` / `filePath` / `storedName` / `md5`，§2.5.5）；`FileService.remove` 收紧为管理端台账入口 |
| 数据库 | 附件域 **2 条破坏性 `ALTER TABLE` 已于 2026-09-20 落地**（重写 `aik_sys_attachment` + `aik_sys_file` 删 `url` / `del_flag`，即 M1；现行结构见 SDD §2.3.4）；`aik_knowledge_tag.use_count` 列标注废弃（不 DROP，M3）；分类种子数据**已应用**（实测，§3.1） |
| 前端 | **尚未开工**，破坏性变更（`tags` 整形、`FileVo` 字段删除、附件字段换代）迁移成本均为零 |
| 鉴权 | **不在本方案范围**（见 §5 非目标） |

### 0.3 不在本文档范围

前端代码、鉴权实现、全文检索、`grimoire-files/design-docs/frontend/design.md` 的修订执行。
后者以"下游文档修订清单"形式在 §6 列出。

---

## 1. 现状核实结果

### 1.1 已实现且可用

| 契约 | 位置 | 备注 |
|---|---|---|
| `POST /grimoire/knowledge/{add,update,remove}` | `KnowledgeController` | 动词用 `update`，与其余模块的 `modify` 不一致（§2.2.1） |
| `POST /grimoire/knowledge/page` | 同上 | 正文 `KnowledgeQuery` |
| `GET /grimoire/knowledge/findById?id=` | 同上 | 返回嵌套 `KnowledgeVo` |
| `POST /grimoire/knowledge/toggleStatus` | 同上 | 正文 `ToggleStatusDto` |
| `/grimoire/dictType`、`/grimoire/dictItem`、`/grimoire/systemParam`、`/grimoire/file` | 各自 Controller | 已另行核实，接口清单见 §2.1.7 |

### 1.2 已设计未实现（本方案补齐）

| 契约 | 设计来源 | 缺失的可执行代码 |
|---|---|---|
| `CategoryController` + `CategoryService` | `knowledge-module-design.md` §2 | Controller、Service、DTO、VO |
| `TagController` + `TagService` | 同上 | 同上 |
| 附件读写接口（三端点：`findByBiz` / `save` / `remove`） | **本文档 §2.5**（设计权威：`SDD.md` §2.5；取代 `knowledge-module-design.md` §3.2 的旧附件表描述） | Controller、Service、DTO、VO、统一挂载服务 |
| 分类/标签统计口径 | `frontend-design.md`（分类卡片条目数、标签云加权） | Mapper 聚合查询 |
| 首页/仪表盘统计 | `frontend-design.md`（首页统计摘要、Dashboard） | 新接口 |

### 1.3 契约缺陷（需修正）

| # | 缺陷 | 证据 | 后果 |
|---|---|---|---|
| D1 | `KnowledgeQuery` 无 `tagId` | `KnowledgeQuery.java` 仅 `title/type/categoryId/status` | `/tags/:id` 无法过滤 |
| D2 | `categoryId` 仅精确匹配，不含子孙 | `KnowledgeServiceImpl.findPage` 的 `.eq(KnowledgePo::getCategoryId, ...)` | `/categories/:id` 漏掉子分类条目 |
| D3 | 无跨字段搜索 | 同上，仅 `like(KnowledgePo::getTitle, ...)` | `SearchView` 承诺的"全局搜索"名不副实 |
| D4 | `status` 缺省为 `null` → **不过滤** | `wrapper.eq(query.getStatus() != null, ...)` | **阅读端会显示已禁用/已下架条目** |
| D5 | `tags` 为 `List<String>`，仅标签名 | `KnowledgeListVo.tags`、`KnowledgeVo.tags` | 无 id → 无法跳转 `/tags/:id`；无 color → 无法着色 |
| D6 | `use_count` 是死字段 | 全仓仅 PO 字段声明与 DDL 默认值，无任何写入 | 标签云加权恒为 0 |
| D7 | 附件无 `file_id`，只有 `attach_url` 字符串；`KnowledgeVo.attachments` 直出 PO | `SysAttachmentPo`（实测 10 列，无 `file_id`）；`KnowledgeVo.java:33` 是 `List<SysAttachmentPo>`；下载是 `/grimoire/file/download?id=<aik_sys_file.id>` | 前端拿不到下载 id；`aik_sys_file` 因无人引用而成为死表 |
| D8 | `KnowledgeVo` 嵌套且直接暴露 `KnowledgePo` | `KnowledgeVo.knowledge` 类型为 `KnowledgePo` | 前端类型必错；内部字段外泄 |
| D9 | 无自定义 Mapper XML | `mapper-locations` 指向 `**/dao/mapping/*.xml`，该目录不存在 | 聚合查询无落点（§2.2.4） |

**D7 处置（v1.2 定稿）**：`aik_sys_attachment` **重写为通用挂载表**
（`file_id` + `biz_type` + `biz_id`，删 `knowledge_id` / `attach_url`）；附件读写收敛为**三端点**（§2.5.2）；
`KnowledgeVo.attachments` 改为专用 `List<AttachmentVo>`（§2.5.3）；下载 / 预览统一走
`/grimoire/file/download`（挂载层带 `attachId`，§2.5.5）；`aik_sys_file.url` **全链路删除**。
设计权威 = [`SDD.md` §2.5](./SDD.md)；本节只保留契约形状。
D7 的实现落点：M1（§3）+ §2.5 的三端点 + §2.6.2 的 VO 换代。

---

## 2. 契约规范

### 2.1 通用约定

#### 2.1.1 响应封装

所有接口返回 `ApiResponse<T>`：

```json
{ "code": 200, "success": true, "msg": "操作成功", "data": {} }
```

- `success` 由 `code == ResultCode.SUCCESS.getCode()`（即 200）派生，前端**只需判断 `code`**。
- 分页接口 `data` 为 MyBatis-Plus `IPage<T>` 序列化结果。

#### 2.1.2 HTTP 状态码恒为 200

`GlobalExceptionHandler` 是 `@RestControllerAdvice` 且全部处理方法**没有** `@ResponseStatus` /
`ResponseEntity`（已 grep 验证）。业务异常、参数错误、404、系统异常一律返回 **HTTP 200**，
错误信息在 body 的 `code` / `msg`。

> **前端约束**：axios 拦截器在 `code !== 200` 时弹错并 reject。**不要依赖 HTTP 状态码**。
> 后续接入鉴权时，401/403 同样走 HTTP 200，跳转登录必须在 body 层处理。

#### 2.1.3 Long 一律序列化为 String（含分页元数据）

`JacksonConfig` 对 `Long.class` **和** `Long.TYPE` 都注册了字符串序列化器。因此以下字段全部是字符串：

| 来源 | 字段 |
|---|---|
| 业务 | 所有 `id`、`categoryId`、`tagId`、`parentId`、`fileId`、`attachId`、`bizId` |
| **分页元数据** | `total`、`size`、`current`、`pages`（MyBatis-Plus `Page` 内部是 `long`） |
| 文件 | `fileSize` |

```json
{ "records": [], "total": "20", "size": "10", "current": "1", "pages": "2" }
```

> **前端约束**：TS 类型中上述字段一律 `string`。分页组件与"共 N 条"必须 `Number(x)` 转换后运算。
> 请求方向不受影响：Jackson 可将 JSON 字符串 `"id": "128..."` 反序列化为 `Long`。

#### 2.1.4 null 字段不参与序列化

`JacksonConfig` 设了 `JsonInclude.Include.NON_NULL`。

> **前端约束**：TS 类型中所有字段都是可选的（`?`），且**"字段缺失"与"值为 null"不可区分**。
> 业务上需要区分"未设置"时，约定用空字符串或空数组表达，不要依赖 null。

#### 2.1.5 分页入参与出参

`PageQuery`：`current`（默认 1）、`size`（默认 10，**上限 500**，超出被静默截断为 500）。

> **前端约束**：时间树若需要一次性加载全部条目，`size=500` 是硬上限。超过 500 条必须分页或分批。

#### 2.1.6 新接口一律沿用既有动词

项目既有约定（不一致，本方案不改动既有接口）：

| 动词 | 方法 | Content-Type |
|---|---|---|
| `findPage` | POST | JSON body（继承 `PageQuery`） |
| `findById` | GET | `?id=` |
| `findList*` / `findAll` | GET | query 参数 |
| `add` / `modify` / `remove` | POST | JSON body |
| `remove` | POST | body 为 `IdDto`（`{"id": "..."}`），**不是 DELETE** |

#### 2.1.7 已存在但未在前端文档列出的接口

| 接口 | 方法 |
|---|---|
| `/grimoire/dictItem/findMapByTypes` | POST |
| `/grimoire/dictType/findTypeWithItems?dictCode=` | GET |
| `/grimoire/systemParam/findByGroup?group=` | GET |
| `/grimoire/file/rename` | POST |
| `/grimoire/file/download?id=&attachId=&preview=` | GET |
| `/grimoire/file/remove` | POST |

---

### 2.2 查询语义修正

#### 2.2.1 动词不一致（记录，不修）

`KnowledgeController` 用 `update`；`dictType` / `dictItem` / `systemParam` 用 `modify`。
本方案**不为 `KnowledgeController` 增加 `modify` 别名**（避免双入口），新接口统一用 `modify`。
`/knowledge/update` 作为既成契约保留，后续如需归一另立变更。

#### 2.2.2 `KnowledgeQuery` 目标形态

```java
public class KnowledgeQuery extends PageQuery {
    private String  title;            // 保留：仅标题 LIKE
    private String  keyword;          // 新增：跨 title + summary + content LIKE
    private Integer type;             // 保留
    private Long    categoryId;       // 保留
    private Boolean includeChildren;  // 新增：true 时含所有子孙分类
    private Long    tagId;            // 新增
    private Integer status = 1;       // 语义变更：默认 1（仅启用）
}
```

#### 2.2.3 逐字段语义

| 字段 | 语义 | 实现要点 |
|---|---|---|
| `keyword` | 对 `title`、`summary`、`content` 三列做 `LIKE '%kw%'`，OR 连接 | 与 `title` 同时存在时取 AND（标题命中是更严的子集约束） |
| `includeChildren` | `true` 时把 `categoryId` 替换为其自身 + 全部子孙的 id 集合 | 一次性 `selectList` 全部分类（表极小），内存递归收集 id，再 `IN` |
| `tagId` | 按标签过滤 | 先查关联表取 `knowledge_id` 集合，再 `IN`；集合为空时短路返回空页 |
| `status` | **默认 1** | 阅读端不传即为"仅启用"；管理端要查全部时必须**显式传 `"status": null`** |

> **D4 修复说明**：`status` 默认值改为 1 后，阅读端默认安全。
> 管理端若省略该字段将被默认过滤为"仅启用"，**这是有意的安全性取舍**，需在管理端列表页
> 显式发送 `"status": null` 以查看已禁用条目。

#### 2.2.4 聚合查询的落点（D9）

`application.yml` 的 `mapper-locations` 指向 `classpath:io/aik/steins/grimoire/**/dao/mapping/*.xml`，
但该目录当前**不存在**，全部查询走 `BaseMapper`。

本方案新增的聚合查询数量少且条件固定，**一律使用 `@Select` 注解写在 Mapper 接口上**，
不新建 XML 目录树（少一层需要维护的间接）。

需要新增的聚合：

```sql
-- KnowledgeCategoryMapper
SELECT category_id, COUNT(*) AS cnt
FROM aik_knowledge
WHERE status = 1 AND category_id IS NOT NULL
GROUP BY category_id;

-- KnowledgeTagRelationMapper
SELECT r.tag_id, COUNT(*) AS cnt
FROM aik_knowledge_tag_relation r
JOIN aik_knowledge k ON k.id = r.knowledge_id
WHERE k.status = 1
GROUP BY r.tag_id;

-- KnowledgeMapper（统计）
SELECT type, COUNT(*) AS cnt FROM aik_knowledge WHERE status = 1 GROUP BY type;
```

---

### 2.3 Category 模块

包路径：`io.aik.steins.grimoire.knowledge.controller.CategoryController`
（与 `knowledge-module-design.md` §2 的设计一致）

#### 2.3.1 接口清单

| 接口 | 方法 | 入参 | 出参 |
|---|---|---|---|
| `/grimoire/category/findTree` | GET | — | `ApiResponse<List<CategoryTreeVo>>` |
| `/grimoire/category/findById` | GET | `?id=` | `ApiResponse<CategoryVo>` |
| `/grimoire/category/add` | POST | `CategoryDto` | `ApiResponse<Void>` |
| `/grimoire/category/modify` | POST | `CategoryDto` | `ApiResponse<Void>` |
| `/grimoire/category/remove` | POST | `IdDto` | `ApiResponse<Void>` |

#### 2.3.2 `CategoryTreeVo`

```java
public class CategoryTreeVo {
    private Long    id;
    private Long    parentId;      // 根节点为 "0"
    private String  categoryName;
    private String  categoryCode;
    private Integer sortOrder;
    private Integer status;
    private Integer directCount;   // 直接挂载的启用条目数
    private Integer totalCount;    // 自身 + 所有子孙的启用条目数
    private List<CategoryTreeVo> children;
}
```

#### 2.3.3 计数口径（关键约定）

- `directCount` = `aik_knowledge` 中 `status = 1` 且 `category_id = 本分类` 的行数。
- `totalCount` = 自身 `directCount` + 所有子孙的 `totalCount`，**在内存中自底向上上卷**。
- 口径**只统计 `status = 1`**，禁用条目不计入。前端展示的"12 条知识"即 `totalCount`。

> 对账方式见 §4 的 Phase 2 验收。

#### 2.3.4 分类树深度

`aik_knowledge_category.parent_id` 本身支持任意深度，**不设深度约束**。
树的组装与子孙收集全部在内存完成（分类表规模极小，一次 `selectList` 即可）。
前端按 `children` 递归渲染，不假设层级为 2。

#### 2.3.5 `remove` 删除守卫

必须按顺序校验，任一不满足则抛 `BusinessException`：

1. 存在子分类 → 拒绝，提示"请先删除子分类"。
2. 存在知识条目引用（无论启用/禁用）→ 拒绝，提示引用数量。

> **不做级联删除**。分类被引用时级联会静默丢数据，代价不可逆。

#### 2.3.6 `add` / `modify` 校验

| 字段 | 规则 |
|---|---|
| `categoryName` | 非空，长度 ≤ 128 |
| `parentId` | 可空（默认 0）；非 0 时必须指向存在的分类；**不得指向自身或自身子孙**（成环检测） |
| `categoryCode` | 可空；非空时**在 Service 层校验全局唯一**（见下） |
| `sortOrder` | 默认 0 |
| `status` | 默认 1 |

**`categoryCode` 唯一性说明**：`aik_knowledge_category` 的 DDL **没有** `category_code`
唯一索引（只有 `idx_parent_id` / `idx_status`），与 `dict_code`、`tag_name` 的约定不一致。
本方案**不新增唯一索引**——对已有数据的表加唯一索引存在失败风险，
且当前数据尚未整理。改为在 `CategoryService` 内 `SELECT` 校验后拒绝重复，
并在 §7.2 登记"并发下可能绕过"的已知限制（单用户场景影响可忽略）。
是否补索引待 Phase 6 数据整理完成后另立变更。

---

### 2.4 Tag 模块

包路径：`io.aik.steins.grimoire.knowledge.controller.TagController`

#### 2.4.1 接口清单

| 接口 | 方法 | 入参 | 出参 |
|---|---|---|---|
| `/grimoire/tag/findAll` | GET | — | `ApiResponse<List<TagVo>>` |
| `/grimoire/tag/findPage` | POST | `TagQuery`（继承 `PageQuery`） | `ApiResponse<IPage<TagVo>>` |
| `/grimoire/tag/findById` | GET | `?id=` | `ApiResponse<TagVo>` |
| `/grimoire/tag/add` | POST | `TagDto` | `ApiResponse<Void>` |
| `/grimoire/tag/modify` | POST | `TagDto` | `ApiResponse<Void>` |
| `/grimoire/tag/remove` | POST | `IdDto` | `ApiResponse<Void>` |

`findAll` 同时服务两个消费方：管理端编辑器标签选择器、阅读端标签云。
标签表规模小（预期 < 1000），**不分页**，一次全量返回。

#### 2.4.2 `TagVo`

```java
public class TagVo {
    private Long    id;         // D5 修复：前端据此跳转 /tags/:id
    private String  tagName;
    private String  tagColor;   // D5 修复：前端据此着色
    private Integer useCount;   // 读时聚合，见 2.4.3
}
```

> `aik_knowledge_tag` **没有 `status` 列**（见 `sql/aik_knowledge_tables.sql`），
> 因此不存在"停用标签"的概念，`findAll` 返回全部标签，`TagVo` 不含 `status` 字段。
> 若后续需要停用能力，需另立 DDL 变更，**不在本方案范围**。

#### 2.4.3 `useCount` 口径（D6 修复）

**不在写路径维护 `aik_knowledge_tag_relation` → `aik_knowledge_tag.use_count` 的计数。**
改由 §2.2.4 的 `GROUP BY r.tag_id` 查询**读时聚合**。

理由：`KnowledgeServiceImpl` 的 `saveTagRelations` / `update`（先删后增）/ `delete` 三处
都只操作关联表。要正确维护计数器需要在三处加事务内增减，并在标签改名、知识下架等
边界保持一致——在 20 条数据的规模下这是纯粹的额外复杂度与漂移来源。

- `aik_knowledge_tag.use_count` 列**保留但标注废弃**（DDL 注释更新）。
- **不执行 `DROP COLUMN`**：属破坏性变更，需用户显式确认后另行处理。
- 聚合口径只统计 `k.status = 1` 的知识，与分类计数口径（§2.3.3）保持一致。

#### 2.4.4 `remove` 删除守卫

1. 存在标签关联（`aik_knowledge_tag_relation` 引用该 `tag_id`）→ 拒绝，提示引用数量。

> 与分类一致，**不做级联**。

#### 2.4.5 `add` / `modify` 校验

| 字段 | 规则 |
|---|---|
| `tagName` | 非空，长度 ≤ 64，**全局唯一**（DB 已有 `uk_tag_name`）；重名需转成可读的 `BusinessException`，不能把 `DuplicateKeyException` 直接抛给前端 |
| `tagColor` | 可空；非空时校验为 `#RGB` 或 `#RRGGBB` |

---

### 2.5 Attachment 模块（通用挂载表）

包路径：`io.aik.steins.grimoire.system.attachment`

> **设计权威**：附件域的**两层模型**、字段归属、删除契约、秒传语义由
> [`SDD.md` §2.5](./SDD.md)（"附件两层模型与归属边界"，附件域唯一权威）拥有。
> **本节只定义 HTTP 形状**（路径 / 方法 / 入参 / 出参 / 错误），且**不得**与 SDD §2.5 冲突。
>
> 两层模型速览：`aik_sys_file` = **文件对象**（一行 = 磁盘上的一份字节，`md5` 内容寻址，**物理删除**）；
> `aik_sys_attachment` = **通用挂载点**（一行 = 某业务以某名义用某文件：`file_id` + `biz_type` + `biz_id`）。
> 本次改造的删除清单：`aik_sys_file.url`、`aik_sys_attachment.attach_url`、`aik_sys_attachment.knowledge_id`。

#### 2.5.1 数据库变更（M1）

本模块依赖 §3 的 **M1**：**重写** `aik_sys_attachment`（通用挂载表）+ **删除** `aik_sys_file.url`。
**M1 已于 2026-09-20 对真库落地**——下表是变更记录与重放前提，不是待办计划。

| 项 | 内容 |
|---|---|
| **状态** | ✅ **已落地（2026-09-20）**：真库结构 = SDD §2.3.4（`aik_sys_file` **13 列**、`aik_sys_attachment` **12 列** + `uk_biz_file` / `idx_biz` / `idx_file_id`），两表仍为 0 行 |
| 变更类型 | **破坏性 DDL**：`DROP COLUMN` + `CHANGE COLUMN` + 新增 `NOT NULL` 无默认值列 |
| 可执行脚本 | **唯一权威 = `sql/attachment_two_layer_migration.sql`**（SDD §2.3.1 是文档内对照；本文件不复制）。clause 顺序是硬约束：`CHANGE COLUMN` 必须早于 `DROP INDEX idx_knowledge_id`；**不得**显式 `DROP INDEX idx_del_flag`（随列自动消失） |
| 重放前提 | 两表必须 **0 行**（迁移后实测仍为 0）；**任一 > 0 行必须中止**（校验见 §3.0） |
| 表物理归属 | `aik_sys_attachment` 建表语句**已迁入** `sql/aik_system_tables.sql`（`sql/aik_knowledge_tables.sql` 已不含该表；SDD §2.5.8） |
| 回滚 | 两表 0 行 → 回滚 = 按 SDD §2.3 的 `CREATE TABLE` 重建（**无数据损失**） |

**变更形状**（细节以 SDD §2.3.1 为准）：

```sql
-- 仅列 clause【形状】（完整可执行 ALTER TABLE 语句见 SDD §2.3.1，本文件不复制）
-- aik_sys_file（第 1 层：文件对象）
DROP COLUMN url,                  -- 访问统一走 download，不再持久化任何访问地址（SDD §2.5.6）
DROP COLUMN del_flag;             -- 改【物理删除】，摘除 @TableLogic（SDD §2.5.3）

-- aik_sys_attachment（第 2 层：通用挂载点）
DROP COLUMN attach_url,                            -- 真冗余：字节定位由 file_id → aik_sys_file.file_path 承担
CHANGE COLUMN knowledge_id biz_id BIGINT NOT NULL, -- 业务归属改为 (biz_type, biz_id)
ADD COLUMN file_id  BIGINT      NOT NULL COMMENT '被挂载的文件对象ID，逻辑关联 aik_sys_file.id',
ADD COLUMN biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：knowledge（取值登记见 SDD §2.5.5）',
ADD COLUMN del_flag TINYINT     NOT NULL DEFAULT 0 COMMENT '卸载标记：0-有效挂载，1-已卸载',
ADD UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id),
ADD KEY idx_biz (biz_type, biz_id, del_flag, sort_order),
ADD KEY idx_file_id (file_id);
```

**已作废的旧条款**（v1.1 的 M1，**不得再执行、不得再引用**）：

> ~~`ALTER TABLE aik_sys_attachment ADD COLUMN file_id BIGINT NULL COMMENT '关联 aik_sys_file.id' AFTER knowledge_id, ADD KEY idx_file_id (file_id);`~~
>
> ~~`file_id` 允许为 NULL，以兼容既有数据与"仅登记外部链接"的用法。~~

作废理由（三条各自独立成立）：

1. **保留 `knowledge_id`** 与"通用挂载表"目标直接冲突——它把知识业务硬编码进 system 模块的表结构。
2. **`file_id` 必须 `NOT NULL`**：挂载层的存在意义就是"引用一份**真实**文件对象"。可空列会让
   "悬空挂载"（挂载行不指向任何文件）变成**合法状态**，而 SDD §2.5.7 写入第 1 条要求以加锁读校验 `fileId` 存在。
3. **"仅登记外部链接"的用法已废弃**：外部链接型附件（无文件对象的 `attach_url`）不在本次范围（§3.2）。
   `attach_url` 列本身也已 `DROP COLUMN`。

#### 2.5.2 接口清单（三端点的最终形态）

| 接口 | 方法 | 入参 | 出参 |
|---|---|---|---|
| `/grimoire/attachment/findByBiz` | GET | `?bizType=knowledge&bizId=<id>` | `ApiResponse<List<AttachmentVo>>` |
| `/grimoire/attachment/save` | POST | `AttachmentSaveDto`（JSON body） | `ApiResponse<Void>` |
| `/grimoire/attachment/remove` | POST | `IdDto`（`{"id":"<attachId>"}`，即**挂载行** id） | `ApiResponse<Void>` |

**路径泛化决策**（SDD §2.5.8 明确把该决定交给本文件）：采用**通用路径** `findByBiz`，
**废弃** v1.1 的 `/grimoire/attachment/findByKnowledgeId`。

| 方案 | 内容 | 裁决 |
|---|---|---|
| **A（采用）** | `GET /grimoire/attachment/findByBiz?bizType=&bizId=` | 附件模块是**通用挂载层**，`biz_type` 是**开放式取值**；路径里出现业务名 = 把刚删掉的 `knowledge_id` 硬编码**从列搬到 URL** |
| B（废弃） | `/grimoire/attachment/findByKnowledgeId?knowledgeId=` | 每新增一个业务域都要在附件模块**新增 Controller 方法并重新编译**，与 SDD §2.5.5 选择 `VARCHAR(32)` 开放式取值的理由直接冲突 |

- `bizType` **必填**且在**白名单**内（当前仅 `knowledge`；常量 = `AttachmentBizType.KNOWLEDGE`，登记表见 SDD §2.5.5）；未知值 → `BusinessException`。
- `bizId` **必填**。
- **`findByBiz` 只返回 `del_flag = 0` 的有效挂载行**——已卸载的**墓碑行**仅供复活契约（§2.5.2 `save`）与
  对账 SQL（SDD §2.3.2）使用，**不得**进入业务列表。这条是**硬约束**：`AttachmentVo`（§2.5.3）不含 `del_flag`，
  前端**无从自行过滤**；把卸载行一并返回等于"用户删掉的附件重新出现在列表里"。
- **排序**：`ORDER BY sort_order ASC, id ASC`（与 `KnowledgeServiceImpl:193` 的
  `orderByAsc(SortOrder)` 现状口径一致；`sort_order` 可重复，故以 `id` 兜底保证稳定序）。
- **`findByBiz` 必须一并返回 `fileSize` / `fileType`（v1.2.2 起）**，且**按 `fileId` 批量取**文件元数据：
  先查挂载行，再用**一次** `aik_sys_file` 的 `WHERE id IN (:fileIds)` 取回，在内存里合并——
  **禁止**逐条 `selectById`（N+1）。已卸载行（`del_flag = 1`）同样**不参与**该查询。
- **知识条目的挂载读写不依赖本端点**：`KnowledgeDto.attachments` / `KnowledgeVo.attachments`（§2.6）是
  knowledge 侧入口，`bizType` 由 knowledge 服务固定为 `AttachmentBizType.KNOWLEDGE`（**不由前端传**）。
  两个入口**必须**调用同一个统一挂载服务，**禁止**在 knowledge 模块内直连 `SysAttachmentMapper`
  （SDD §2.5.7、§4.5）。

**`save` 的语义：差量（⚠️ 替代 v1.1 的"整组替换 = 先删再插"）**

```java
public class AttachmentSaveDto {
    @NotBlank private String bizType;                     // 白名单（当前 knowledge）
    @NotNull  private Long   bizId;
    @NotNull  private List<AttachmentDto> attachments;    // 见 §2.5.3；空列表 = 卸载该业务全部挂载
}
```

服务端**必须**实现为**差量**（SDD §2.5.7 第 5 条、§4.5）：

```
oldSet = 该 (bizType, bizId) 下 del_flag = 0 的挂载行
newSet = 入参 attachments（按 id 优先、id 缺失时按 fileId 匹配）

① 仅 oldSet − newSet → 卸载：UPDATE del_flag = 1（**保留挂载行**，不物理删除）
② 仅 newSet − oldSet → upsert：查（**含已卸载行**）→ 命中则复活（UPDATE del_flag = 0，
                          刷新 attach_name / description / sort_order / modify_time / modify_by）
                          → 未命中才 insert；并发撞 DuplicateKeyException → 复活并重试一次
③ newSet ∩ oldSet    → 按需 UPDATE attach_name / description / sort_order
```

❌ **作废**：v1.1 的「`save` 采用**整组替换**语义（先按 `knowledgeId` 删除、再批量插入），
与 `KnowledgeServiceImpl` 处理标签关联的既有方式一致」。

作废理由（**按旧文本实现会直接产生运行时故障**）：`uk_biz_file (biz_type, biz_id, file_id)`
**不含 `del_flag`**，一行的卸载墓碑（`del_flag = 1`）仍**占用键值**；"全删"产生的卸载行会让紧接着的
"全插"**必然撞唯一键** → **同一知识第二次保存即报重复键**（用户可见的功能回归，不是角落情形）。
差量语义是"同一知识再次保存时保留原附件"能正常工作的**前提**。

#### 2.5.3 `AttachmentVo` / `AttachmentDto`

```java
public class AttachmentVo {
    private Long          id;          // 挂载行主键 = attachId（下载 / 卸载都用它）
    private Long          fileId;      // 必填；文件对象 id，下载 / 预览的唯一定位锚点
    private String        attachName;  // 用户可见文件名（权威归属【挂载层】，SDD §2.5.2）
    private String        fileSize;    // 字节数（Long→String，§2.1.3）——【文件层】属性，由 fileId 关联 aik_sys_file 带出
    private String        fileType;    // MIME 类型——【文件层】属性，同上（前端据此选类型图标 / 判断能否内联预览）
    private String        description;
    private Integer       sortOrder;
    private LocalDateTime createTime;
}
```

> **字段集 = 8 个，以上面这个类为唯一可抄来源**（SDD §4.5 的"恰为 N 字段"口径由 `t15` 跟随本版同步）。
> 命名与项目其它 VO 一致（`id`、camelCase）；**不要**另造 `attachId` 字段（见本节末「字段名对照」）。

**明确不含**（v1.1 有、v1.2 起删除 / 或不得出现）：

| 字段 | 处置 | 理由 |
|---|---|---|
| `attachUrl` | **删除** | 列已 `DROP COLUMN`（§3 M1）；字节定位由 `fileId` → `aik_sys_file.file_path` 承担 |
| `knowledgeId` | **删除** | 列已不存在；业务归属由 `bizType` + `bizId` 表达 |
| `url` / `filePath` / `storedName` / `md5` | **不得出现** | 契约中**不得有任何路径/访问地址形态**（SDD §2.5.6） |
| `originalName` | **不得出现** | **语义上不能用**：它是**首次上传者**的名字，md5 秒传后会把别人的文件名给第二个挂载者（§2.5.4）。附件显示名**只**认挂载层的 `attachName` |

> **v1.2.2 变更**：`fileSize` / `fileType` v1.2 曾在本表中标为"删除"，**已依 R18 恢复为正式字段**——
> 处置理由与备选方案裁决见下一小节。

**为什么恢复 `fileSize` / `fileType`（v1.2.2，R18 处置）**

v1.2 以"无消费方"为由把这两个字段从 `AttachmentVo` 删除——**该判断有误**：`frontend/design.md` 的
知识详情页**先于契约**就要求显示"文件大小 + 类型图标"，**消费方一直在**。故这属**契约缺陷**，不是产品变更。

| 备选 | 裁决 | 理由 |
|---|---|---|
| **A（采用）：后端在 `AttachmentVo` 补这两个字段** | ✅ | 二者是**内容属性**——不泄露存储布局、不随上传者变化，与 SDD §2.5.6 的两条判据（"不暴露路径形态"、"不暴露无消费方字段"）**均不冲突** |
| B：前端按 `fileId` 走 `/file/findPage` 补取 | ❌ | **N+1 请求**；且该端点语义是**管理端分页**，不是"按 id 批量取元数据" |
| C：附件区不显示大小 / 类型 | ❌ | 砍掉**已定稿**的前端设计 |

- **数据来源（实现约束）**：这两个字段**由 `aik_sys_file` 关联带出**——`AttachmentVo` 只新增**暴露面**，
  **不新增** `aik_sys_attachment` 的列（挂载表仍是 12 列，SDD §2.3.4）。
- **禁止 N+1**：实现必须**按 `fileId` 批量取**文件元数据（`WHERE id IN (...)` 一次查回），
  见 §2.5.2 的 `findByBiz` 约束条。
- 对照：`originalName` **不因本裁决恢复**（它是"首次上传者的名字"，语义上不可用于附件显示，§2.5.4）。

**为什么 `AttachmentVo` 不回显 `bizType` / `bizId`**（与 SDD §2.5.8 下游清单的一处偏离，已报船长）：
三端点都是**按业务发起**的（列表带 `bizType`+`bizId`、卸载带挂载行 id），响应里回显业务归属
**没有任何消费方**；`KnowledgeVo.attachments` 的元素更不需要——它本身就在某条知识里。
SDD §2.5.6 为 `md5` 立的判据同样适用：「**不暴露没有消费方的字段**」（否则就是"暴露了但无人使用"的半退休字段）。
若将来出现"跨业务附件总览"端点，届时按该端点的**实际消费方**定义专用 VO，
**不得**把这两个字段加进通用 `AttachmentVo`。

**澄清（防误读）**：不回显**只是 API 暴露面**决策——`aik_sys_attachment` **表仍保留**
`biz_type` / `biz_id` 两列（存储与查询需要它们，`uk_biz_file` / `idx_biz` 也依赖它们），
**不得**据此删列；`findByBiz` 的 `bizType` 白名单常量来源 = `AttachmentBizType.KNOWLEDGE`（§2.5.2、SDD §2.5.5）。

**字段名对照**：SDD §4.5 把挂载行主键称作 `attachId`，本契约的 VO 字段名为 **`id`**
（项目 VO 统一用 `id`，与 `IdDto` / 其它 VO 一致）；二者指向**同一个值**，
实现时**不要**同时定义 `id` 与 `attachId` 两个字段。

```java
public class AttachmentDto {
    private Long    id;           // 已存在挂载行的 id；新增挂载时为空
    private Long    fileId;       // 必填
    private String  attachName;   // 必填（不得缺省回填，见 §2.5.4）
    private String  description;  // 可空
    private Integer sortOrder;    // 缺省 0
}
```

#### 2.5.4 上传到入库的完整链路（D7 修复）

```
1. POST /grimoire/file/upload            (multipart, 字段名 file)
   → ApiResponse<FileVo>.data.id          ← 这就是 **fileId**（Long→String，前端按 string 收）
   → 出参字段集见 §2.5.5（**不含** url / filePath / storedName / md5）
2. 挂载（把 fileId 作为【绑定锚点】提交）
   ├─ 知识条目：KnowledgeDto.attachments = [ { fileId, attachName, description, sortOrder } ]（§2.6.4）
   └─ 通用入口：POST /grimoire/attachment/save（差量语义，§2.5.2）
3. 详情页渲染：GET /grimoire/knowledge/findById → attachments: List<AttachmentVo>（含 id 与 fileId）
4. 预览：GET /grimoire/file/download?id={fileId}&attachId={attachId}&preview=true
5. 下载：GET /grimoire/file/download?id={fileId}&attachId={attachId}
```

**上传接口的出参就是 D7 的落点**：`fileId` = `FileVo.id`，它是挂载的**唯一绑定锚点**。
上传接口**不产生挂载行**（文件对象与挂载点分层，SDD §4.2）——调用方拿 `id` 去第 2 步建挂载。

约束：

- **`attachName` 必填**（`@NotBlank`，长度 ≤ 256）。其**权威唯一归属挂载层**（SDD §2.5.2）。
  ❌ **作废**：v1.1 的「`attachName` 缺省时由后端用 `FileVo.originalName` 回填」。
  **作废理由**：md5 秒传命中后 `aik_sys_file` 只保留**第一个上传者**的文件名（`original_name` 语义
  已降级为"首次上传者的名字"，仅台账用）；回填会让第二个挂载者看到**别人的文件名**——
  这正是本次"下载文件名取自挂载层"要消灭的现象。
  **后端不得以任何 `aik_sys_file.original_name` 作为 `attachName` 的兜底来源**（边界澄清：`POST /file/rename`
  改的是文件**台账**的 `original_name`，用户可见名的改名落在**挂载行** `attach_name`，两者不得互相回填）。
- **`fileId` 必填且必须存在**：以 **`SELECT ... FOR UPDATE`（加锁读）**校验 `aik_sys_file` 行存在；
  不存在 → `BusinessException`（悬空挂载禁止产生，SDD §2.5.7 第 1 条）。
  为什么必须是加锁读：无锁的 `selectById` 能读到"即将被并发删除的文件行"，会插入指向已删文件的挂载行（SDD §2.5.7）。
- 上传白名单由 `grimoire.file.type-check-enabled` + `allow-types` 控制
  （当前为 `jpg,jpeg,png,gif,webp,pdf,txt,md,zip,json,doc,docx,xls,xlsx,ppt,pptx`），
  单文件 10MB、请求 50MB。
- 前端**不得**用任何 `attachUrl` / `url` 拼下载地址（字段已不存在）；统一走 `fileId`（挂载层再加 `attachId`）。

#### 2.5.5 File 端点契约（附件链路第 1 层）

**`FileVo`（对外唯一的文件视图）**：由 `/grimoire/file/upload` 与 `/grimoire/file/findPage` 返回。
（`FileService.findById` 存在但**没有对应 HTTP 端点**，不作为契约端点。）

| 字段 | JSON 类型 | 说明 |
|---|---|---|
| `id` | string | 文件对象 id，即 `fileId`（挂载锚点） |
| `originalName` | string | **首次上传者**的文件名（管理台账视图用；**不得**作为附件显示名来源） |
| `fileSize` | string | 字节数（Long→String，§2.1.3） |
| `fileType` | string | MIME 类型 |
| `storageType` | string | `local` / `oss` / `sftp`（**策略标签，不泄露任何路径**；保留） |
| `downloadCount` | number | 下载次数 |
| `createTime` | string | 创建时间 |

❌ **不得出现**的字段：`url`、`filePath`、`storedName`、`md5`。

| 字段 | 处置 | 理由 |
|---|---|---|
| `url` | **删除** | 访问统一走 download；且 OSS 实现的 `getUrl()` 返回的是**预签名临时 URL（1 小时过期）**，持久化必然变死链（SDD §2.5.6） |
| `filePath` | **删除** | 它**本身就是**存储布局；`FileVo.of()` 是 `BeanUtil.copyProperties` 全量拷贝，字段只要在 VO 上就会外泄——只删 `url` 等于只完成一半 |
| `storedName` | **删除** | 同属存储布局（磁盘上的存储名） |
| `md5` | **删除** | 内容指纹属内部锚点；本次不做客户端预检端点，暴露它就是"无消费方字段"（两阶段秒传的**有意缺口**登记见 SDD §2.5.6） |
| `storageType` | **保留** | 策略标签，不泄露路径；管理页按存储类型筛选需要（SDD §2.5.6 显式记录该选择） |

> ⚠️ **`getUrl()` 方法链一并删除**（甲方案 — 船长裁决 2026-09-20，SDD §2.5.6 〔K1〕）：
> 随 `url` 列退役的不只是持久化点，还包括 `FileStorageStrategy#getUrl(String)` **接口方法**与三个实现
> （`LocalFileStorage` / `OssFileStorage` / `SftpFileStorage`）。理由：摘掉 `FileServiceImpl:65` 的调用后
> 它**零调用方**，留存即"有定义、有实现、无调用方"的半退休状态。
> 本次 `url` 链路的完整删除清单 = `aik_sys_file.url` 列 + `aik_sys_attachment.attach_url` 列
> + `getUrl()` 方法 + 三个实现 + `FileServiceImpl:65` / `:77` + `FileRecordPo.url` + `FileVo.url`。
> **Reopen Trigger**：需要"由存储层直接给出预签名地址且**不落库**"的直连下载时，**新增语义明确的方法**
> （如 `presignedUrl`），**不得**复活任何持久化地址字段。
> 另注：`FileStorageStrategy#exists(String)` 与 `AbstractFileStorage` 的两个接口外 `download` 重载
> 是**零调用方的待退役候选**（不属 url 链路，本次不删）——**改造中不得新增对它们的依赖**（SDD §2.5.6）。

**`GET /grimoire/file/download` 的 `attachId`（本文件的裁决）**

```
GET /grimoire/file/download?id={fileId}[&attachId={attachId}][&preview={true|false}]
```

**裁决**：`attachId` 是「**挂载层必带、台账层可省**」的**条件必填**参数——服务端不强判"必带"，但**带则强校**。

| 调用方 | `attachId` | 服务端行为 |
|---|---|---|
| **挂载层发起**（附件列表 / 预览 / 知识详情里的附件）——**必须带** | **必带** | 校验该挂载行：**存在**、`del_flag = 0`、且 `file_id == id`；通过后**响应文件名 = 该行 `attach_name`**；任一不满足 → `BusinessException` |
| **管理端文件台账**（`/grimoire/file/findPage` 的下载入口）、**上传后立即预览**（尚未建挂载） | 省略 | 不校验挂载；**响应文件名 = `aik_sys_file.original_name`**（台账视角） |

- `preview`：`true` → `Content-Disposition: inline`；否则 `attachment`（`AbstractFileStorage:141`）。
  一个接口同时服务下载与预览，前端不需要知道底层是 local 还是 OSS。
- **文件名来源必须由服务端决定，不接受前端传名**：不得把前端传入的字符串写进 `Content-Disposition`
  （否则是**响应头注入**面——SDD §2.5.6 否决方案 2 的理由）。
- **已知残余风险（登记，不修）**：`attachId` 可省略，意味着"凭 `fileId` 下载任意文件"在**台账模式**下仍可行，
  SDD §2.5.6 方案 1 的"只有被挂载的文件才可下载"**只在挂载模式成立**。
  本项目当前**无鉴权、单用户**（§7.1），台账模式不构成隐私边界。
  **Reopen Trigger**：接入鉴权且需要防"按 fileId 遍历下载"时 → 改为**挂载必填** + 另开管理端台账下载端点。

**`POST /grimoire/file/remove` 的处置（SDD §2.5.6 方案 b：保留并收紧为管理端台账入口）**

```
FileService.remove(id)                      ← 文件删除的【唯一入口】
    ├── 查文件记录（不存在 → 幂等 no-op，不抛异常）
    ├── 存在 del_flag = 0 的挂载行 → BusinessException「文件仍被其它挂载点引用，不允许删除」
    └── 无 → 物理删除记录（同事务）→ 【提交后】删盘
```

- 端点**保留**（文件台账仍需管理端删除入口），但**必须**带"无有效挂载"前置校验。
- ❌ **已作废的现实现**：`FileServiceImpl:140-146` 只做 `deleteById`（逻辑删除，**不校验挂载、不删盘**）。
  它在新模型下会**误删其它业务正在共享的文件**，并让 B 的挂载行变成悬空引用。
- **挂载卸载**（`/grimoire/attachment/remove`、或删除知识条目时的卸载）走 SDD §4.3 的完整删除契约：
  同一事务内「① `SELECT ... FOR UPDATE` 锁文件行 → ② 标记卸载挂载行 → ③ 数 `del_flag = 0` 的挂载 →
  ④ 为 0 才**物理删除** `aik_sys_file` 行（行已不存在按幂等 no-op）」，**事务提交后**才
  `fileStorageStrategy.remove(file_path)` 删盘。**顺序不能反**：先删盘再回滚会造成不可恢复的悬空引用。

---

### 2.6 Knowledge 契约修订

#### 2.6.1 唯一破坏性变更：`tags` 整形（D5）

```diff
- private List<String> tags;      // 只有标签名
+ private List<TagBriefVo> tags;  // 含 id / tagName / tagColor
```

```java
public class TagBriefVo {
    private Long   id;
    private String tagName;
    private String tagColor;
}
```

`KnowledgeListVo` 与 `KnowledgeVo` 同时改造。此时前端尚未开工，**迁移成本为零**。

#### 2.6.2 `KnowledgeVo` 扁平化（D8）

移除 `KnowledgeVo.knowledge : KnowledgePo` 这层嵌套，不再向上暴露 PO；
字段与 `KnowledgeListVo` 保持命名一致，仅多出正文类字段。

```java
public class KnowledgeVo {
    private Long    id;
    private String  title;
    private String  code;
    private Integer type;
    private String  typeDesc;        // 由 KnowledgeTypeEnum 派生
    private String  summary;
    private String  content;
    private String  sourceProject;
    private String  sourcePath;
    private String  resourcePath;
    private String  extJson;         // 原样透传；解析归前端
    private Long    categoryId;
    private String  categoryName;
    private String  categoryPath;    // "Java > Spring" 形式，根到当前
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
    private List<TagBriefVo>  tags;
    private List<AttachmentVo> attachments;
}
```

`categoryPath` 由后端组装（分类表在内存中，成本可忽略），避免前端为面包屑再发一次请求。

> ⚠️ **`attachments` 的现状与目标**：实测 `KnowledgeVo.java:33` 当前是
> `private List<SysAttachmentPo> attachments;`——**PO 实体直出**，会把 `biz_type` / `biz_id` / `del_flag`
> 等内部列暴露给前端，且响应结构随 PO 漂移。本契约要求改为 `List<AttachmentVo>`（§2.5.3 的 8 字段）。
> **`SysAttachmentPo` 不得出现在任何 VO 中**（SDD §4.5）。

#### 2.6.3 `KnowledgeListVo` 增补

```diff
+ private Long   categoryId;
+ private String categoryPath;   // 时间树条目行的"分类: Java > Spring"
```
`tags` 按 2.6.1 整形。其余字段不变。

#### 2.6.4 `KnowledgeDto` 增补

```diff
+ private List<AttachmentDto> attachments;   // 【差量】语义（⚠️ 不是"整组替换"，见 §2.5.2）
```
`tagIds`（`List<Long>`）保持不变。`attachments` 的元素结构 = `{ id?, fileId, attachName, description, sortOrder }`
（`AttachmentDto`，§2.5.3）：

- `fileId` **必填**，且必须以**加锁读**校验对应 `aik_sys_file` 行存在（§2.5.4）。
- **`attachName` 必填**——后端**不得**用 `aik_sys_file.original_name` 回填（§2.5.4 的作废条款）。
- `description` 可空，`sortOrder` 缺省 0。
- `bizType` 由 knowledge 服务固定为 `AttachmentBizType.KNOWLEDGE`，**不由前端传**。
- **禁止**实现为"全删再全插"（`uk_biz_file` 不含 `del_flag`，会撞唯一键，§2.5.2）。
  挂载变更必须与知识条目写入在**同一事务**内，并调用统一挂载服务（禁止直连 `SysAttachmentMapper`，SDD §4.5）。

> **XSS 提醒**：`content` 是 Markdown 原文。前端渲染必须 `DOMPurify` 过滤，
> 或在 `markdown-it` 上禁用 `html` 选项。后端不做 HTML 转义（会破坏 Markdown 语义）。

---

### 2.7 Stats 接口

| 接口 | 方法 | 出参 |
|---|---|---|
| `/grimoire/knowledge/stats` | GET | `ApiResponse<KnowledgeStatsVo>` |

```java
public class KnowledgeStatsVo {
    private Integer knowledgeCount;    // status = 1
    private Integer categoryCount;     // status = 1
    private Integer tagCount;          // 全部（标签表无 status 字段，见下）
    private Integer attachmentCount;
    private List<TypeCountVo> typeDistribution;  // [{type, typeDesc, count}]
    private List<RecentItemVo> recentEdited;     // 最近修改 5 条 {id, title, modifyTime}
}
```

设计说明：

- **一个接口服务首页统计摘要 + StatsBar + Dashboard**，避免多次往返。
- `tagCount` 统计 `aik_knowledge_tag` 全表——该表**没有 `status` 字段**
  （见 `sql/aik_knowledge_tables.sql`，与 §2.4.2 的说明一致）。
  若后续需要"停用标签"能力，需另立 DDL 变更，**不在本方案范围**。
- `recentEdited` 按 `modify_time DESC` 取 5 条，`status` 不限（管理视角）。
- `attachmentCount` 统计 `aik_sys_attachment` 中 **`del_flag = 0`** 的挂载行
  （卸载行不计入，SDD §2.5.7）；口径与附件列表（§2.5.2 的 `findByBiz`）一致，
  **不是** `aik_sys_file` 的行数（同一份文件可被多个业务挂载，两者不相等）。

---

## 3. 数据库变更清单

> 状态列采用**实测口径**（2026-09-20 只读 `SELECT` 直连运行时库复核），不是历史文档的转述。
> **M1 是破坏性 DDL，已于 2026-09-20 执行**；若需重放，必须先通过 §3.0 的硬断言。

| # | 变更 | 类型 | 破坏性 | 状态 | 可逆 |
|---|---|---|---|---|---|
| **M1** | **重写附件域**：`aik_sys_attachment` 改造为**通用挂载表**（删 `knowledge_id` / `attach_url`；加 `file_id` / `biz_type` / `del_flag` + `uk_biz_file` / `idx_biz` / `idx_file_id`）＋ `aik_sys_file` **删 `url`、删 `del_flag`（改物理删除）** | 改结构 | **是** | ✅ **已执行（2026-09-20）**；脚本 `sql/attachment_two_layer_migration.sql`；现行结构 SDD §2.3.4 | 是（重放/回滚见 §3.0、§7.3） |
| M2 | 分类种子数据（建 5 个分类，修复悬空 `category_id`） | 数据 | 否 | ✅ **已应用**（实测 5 行存在，**勿重复执行**，§3.1） | — |
| M3 | `UPDATE` `aik_knowledge_tag.use_count` 列注释为"已废弃，读时聚合" | 注释 | 否 | 待执行 | 是 |
| M4 | `UPDATE` 悬空 `aik_knowledge.category_id` | 数据 | 否 | ✅ **无需执行**（实测 0 悬空） | — |

### 3.0 M1 前置硬断言（**重放时**必跑；原始执行已完成）

> ✅ M1 已于 **2026-09-20 执行完毕**（SDD §2.3.4）。本节保留作**重放 / 幂等性核查**之用。

```sql
SELECT COUNT(*) FROM aik_sys_file;         -- 断言 = 0（迁移前 0；迁移后实测仍为 0）
SELECT COUNT(*) FROM aik_sys_attachment;   -- 断言 = 0（迁移前 0；迁移后实测仍为 0）
```

任一结果 > 0 → **立即中止**：脚本对 `file_id` / `biz_type` 采用 `ADD COLUMN ... NOT NULL`（无默认值），
在**非空表上不可执行**。已有数据时必须先按 SDD §2.3.1 的注记改走三段式
（加可空列 → 按 `biz_type='knowledge'`、`biz_id=knowledge_id` 回填 → 再置 `NOT NULL`）。

**可执行脚本不在本文件**：唯一权威是 **`sql/attachment_two_layer_migration.sql`**（SDD §2.3.1 为文档内对照）。
不在此复制的原因：它的两处 clause 顺序是**硬约束**——`CHANGE COLUMN knowledge_id → biz_id` 必须在
`DROP INDEX idx_knowledge_id` **之前**；`DROP COLUMN del_flag` 会**自动连带删除** `idx_del_flag`，
因此**不得**显式 `DROP INDEX idx_del_flag`。同一脚本两个 owner 必然漂移；本文件只定义
**变更形状（§2.5.1）、前置断言（本节）与验收（§4 Phase 4）**。

### 3.1 M2 分类种子数据（**已应用**，勿重复执行）

> ⚠️ **本节在 v1.2 由"必须执行"改为"已应用"**。v1.1 的论断
> 「`INSERT INTO aik_knowledge_category` 行数: 0 …… 分类名与父子关系由用户确认后填入」
> **与运行时实测不符**，照其执行会**撞主键**（5 个 `id` 已存在）。

实测（2026-09-20，只读 `SELECT`）：

| 表 / 口径 | v1.1 的写法 | **实测** |
|---|---|---|
| `aik_knowledge_category` | 0 行，中文名为空 | **5 行，中文名与 `category_code` 均已填入** |
| `aik_knowledge` | 17 条（8/3/2/6/1） | **20 条**，按 `category_id` 为 **8 / 3 / 2 / 6 / 1**（"17"为笔误：该组数字本身合计 20） |
| 悬空 `category_id` | 可能残留 | **0**（20 行的 `category_id` 全部命中已存在的分类） |
| `aik_knowledge_tag` / `_relation` | 0 / 0 | 0 / 0（一致，无需变更） |

| id | category_name | category_code | parent_id | 条目数 |
|---|---|---|---|---|
| `1288834972714496001` | 魔典卷轴 | `scrolls` | 0 | 8 |
| `1288834972714496002` | 组件手册 | `component` | 0 | 3 |
| `1288834972714496003` | 学习沉淀 | `learning` | 0 | 2 |
| `1288834972714496004` | 架构规划 | `planning` | 0 | 6 |
| `1288834972714496005` | 技术方案 | `solution` | 0 | 1 |

结论：

- **M2 / M4 无需执行**（§8 的 **C1 随之关闭**：分类中文名与父子关系已实测填入）。
- `KnowledgeListVo.categoryName` **不会为空**，§2.3.3 的分类计数有真实数据可用。
- 另注：5 个分类实测**全部 `parent_id = 0`（扁平，无层级）**。§2.3.4 的树深度设计当前
  **无数据形态支撑**——该设计保留（对未来层级数据兼容），但**不得**据此推断当前存在父子分类。
- `ImportKnowledge.java`、`_import_temp.sql` 仍在 `.gitignore` 中，且前者硬编码
  `D:\JeBrainsWorkSpace\...` 绝对路径 → 不可移植，重跑需先改造为相对路径。

### 3.2 明确不做

| 项 | 原因 |
|---|---|
| `DROP COLUMN use_count` | 破坏性，需显式确认 |
| `aik_knowledge.content` 加 FULLTEXT 索引 | 见 §3.3 迁移触发条件 |
| 给 `aik_knowledge_tag` 加 `status` 列 | 超出 P0 范围 |
| `aik_knowledge.title` / `summary` 列宽对齐设计文档 | 无害漂移，随 §6 文档修订修正文档而非改表 |
| 给 `aik_sys_file.md5` 加**唯一**约束 | md5 去重是**应用层约定**（甲方案）：唯一键会把并发补偿（落盘成功 / 插入失败的孤儿文件回收）塞进删除路径，反而违背"不产生孤儿文件"的验收目标（SDD §2.5.4）。`idx_md5` 保持**非唯一**且**不得删除**（删了秒传查重退化为全表扫描） |
| 外部链接型附件（只有 `attach_url`、无文件对象） | 挂载**必须**引用真实文件对象（`file_id NOT NULL`，SDD §2.5.7）；该列已 `DROP COLUMN` |
| 恢复 `aik_sys_file.del_flag` / 文件层逻辑删除 | 文件对象层改**物理删除**：它是可由 `exists(file_path)` 重建的派生台账；保留 `del_flag` 会让"删除"出现**两个所有者**（挂载行 + 文件行，SDD §2.5.3） |

### 3.3 搜索方案的迁移触发条件

P0 采用 `LIKE '%kw%'` 跨 `title` + `summary` + `content`。该方案在下列任一条件成立时应迁移到
MySQL FULLTEXT（中文需 `ngram` 分词器，`ngram_token_size` 默认 2 → **单字搜索无效**）：

- `aik_knowledge` 启用条目数 > 1000；或
- 搜索接口 p95 延迟 > 200ms；或
- 出现"按相关度排序"或"搜索结果摘要片段"的产品需求。

> 补充权衡：`LIKE` 方案在**高亮片段**上反而更简单——命中位置可用 `INSTR` 计算，
> 而 FULLTEXT 不返回 offset。

---

## 4. 分阶段实施与验收

### Phase 0：溯源前置（必须先做）

**问题**：工作区 186 个文件显示已修改，全部为 CRLF/LF 行尾噪音。

```
工作区 README.md 含 CR: 24 行     HEAD 版本含 CR: 0 行
core.autocrlf: 未设置             .gitattributes: 不存在
```

**风险**：此后任何一次提交都会把 186 个文件的换行噪音与真实改动混在一起，
`git log` 与 diff review 全部失真——**"便于溯源"这一要求当前无法满足**。

**动作**：

1. 新增 `.gitattributes`：`* text=auto eol=lf`
2. 单独提交一次行尾归一化（提交信息明确标注为纯格式变更，无内容改动）
3. 确认 `git status` 干净后再开始 Phase 1

**验收**：

```bash
git status --porcelain | wc -l        # 期望 0
grep -c $'\r' README.md               # 期望 0
```

### Phase 1：查询语义 + VO 契约（`knowledge` 模块）

改造 `KnowledgeQuery`（§2.2）、`KnowledgeListVo`（§2.6.3）、`KnowledgeVo`（§2.6.2）、
`KnowledgeDto`（§2.6.4），新增 `TagBriefVo`。

**验收**：

```bash
# 缺省 status → 禁用条目不出现（D4）
curl -s 'http://localhost:18900/grimoire/knowledge/page' \
  -H 'Content-Type: application/json' -d '{"current":1,"size":50}' | grep -c '"status":0'
# 期望 0

# keyword 命中正文（D3）
curl -s 'http://localhost:18900/grimoire/knowledge/page' \
  -H 'Content-Type: application/json' -d '{"keyword":"TLS"}' | grep -o '"total":"[0-9]*"'
# 期望 > 0（"Claude CLI 安装指南"正文含 TLS）

# tags 为对象数组（D5）
curl -s 'http://localhost:18900/grimoire/knowledge/findById?id=<已知id>' | grep -o '"tags":\[[^]]*\]'
# 期望形如 [{"id":"...","tagName":"...","tagColor":"..."}]

# 分页元数据为字符串（§2.1.3）
# 期望 "total":"20" 而非 "total":20
```

### Phase 2：Category 模块（§2.3）

**验收**：

```bash
# 计数与本方案口径对账（§2.3.3）
curl -s 'http://localhost:18900/grimoire/category/findTree'
# 对账 SQL：
#   SELECT category_id, COUNT(*) FROM aik_knowledge
#   WHERE status = 1 AND category_id IS NOT NULL GROUP BY category_id;
# 要求：每个分类的 directCount 与对账结果逐一相等；
#       每个父分类的 totalCount == 自身 directCount + Σ 子节点 totalCount

# 删除守卫
curl -s 'http://localhost:18900/grimoire/category/remove' \
  -H 'Content-Type: application/json' -d '{"id":"<有子节点的分类id>"}'
# 期望：code != 200，msg 提示"请先删除子分类"

# 成环检测
# modify 把某分类的 parentId 指向自己的子孙 → 期望 code != 200
```

### Phase 3：Tag 模块（§2.4）

**验收**：

```bash
curl -s 'http://localhost:18900/grimoire/tag/findAll'
# 对账：每个标签的 useCount 与下述查询相等
#   SELECT r.tag_id, COUNT(*) FROM aik_knowledge_tag_relation r
#   JOIN aik_knowledge k ON k.id = r.knowledge_id
#   WHERE k.status = 1 GROUP BY r.tag_id;

# 重名守卫：add 一个已存在的 tagName → 期望 code != 200 且 msg 可读（非 DuplicateKeyException 堆栈）
# 引用守卫：remove 被引用的标签 → 期望 code != 200
```

### Phase 4：Attachment / File 契约（§2.5）

**前置（硬性）**：真库结构必须是**迁移后状态**——M1 已于 **2026-09-20** 落地（SDD §2.3.4）。
开始验收前先跑 §4.1 的 `SHOW CREATE TABLE` 断言：`aik_sys_file` **13 列**（无 `url` / `del_flag`）、
`aik_sys_attachment` **12 列**（`file_id` / `biz_type` / `biz_id` / `del_flag` + `uk_biz_file` / `idx_biz` / `idx_file_id`）。
结构不符时**先修结构**，不要在本 Phase 判定"实现有 bug"。

**验收**：

```bash
API=http://localhost:18900
UPLOAD_DIR=./grimoire-files          # = grimoire.file.base-path（application.yml:53）

# ① FileVo 不得暴露存储布局：url / filePath / storedName / md5 必须全部缺席
curl -s -X POST "$API/grimoire/file/upload" -F 'file=@/tmp/test.pdf' | tee /tmp/up.json
grep -Eo '"(url|filePath|storedName|md5)"' /tmp/up.json   # 期望：无任何输出
FILE_ID=$(grep -Eo '"id":"[0-9]+"' /tmp/up.json | head -1 | cut -d'"' -f4)

# ② md5 秒传：同内容二次上传 → 返回【同一个 fileId】，且磁盘【不新增文件】（SDD §4.2）
find "$UPLOAD_DIR" -type f | wc -l
curl -s -X POST "$API/grimoire/file/upload" -F 'file=@/tmp/test.pdf' | grep -Eo '"id":"[0-9]+"' | head -1
find "$UPLOAD_DIR" -type f | wc -l    # 期望：与上次数值相同；且 id 与 ① 相同

# ③ 挂载 + 通用列表（findByBiz）
curl -s -X POST "$API/grimoire/attachment/save" -H 'Content-Type: application/json' \
  -d "{\"bizType\":\"knowledge\",\"bizId\":\"<知识id>\",\"attachments\":[{\"fileId\":\"$FILE_ID\",\"attachName\":\"我的笔记.pdf\",\"description\":\"v2\",\"sortOrder\":1}]}"
curl -s "$API/grimoire/attachment/findByBiz?bizType=knowledge&bizId=<知识id>"
# 期望：元素含 id / fileId / attachName / fileSize / fileType / description / sortOrder / createTime（8 字段）
# 期望：fileSize 是字符串形式的字节数；fileType 是 MIME 类型（均由 aik_sys_file 关联带出）
# 期望：**不含** url / attachUrl / knowledgeId / originalName / bizType / bizId（§2.5.3）
# 对账 SQL：SELECT id, file_id, biz_type, biz_id, attach_name, del_flag FROM aik_sys_attachment
#           WHERE biz_type = 'knowledge' AND biz_id = <知识id> AND del_flag = 0 ORDER BY sort_order;

# ④ ⚠️ 差量语义（旧"整组替换"会在此失败）：把同一个 save 请求体【原样提交两次】
#    期望：两次都 code = 200；挂载行数与 id 都不变；不得出现 DuplicateKeyException
# 对账 SQL：SELECT COUNT(*) FROM aik_sys_attachment
#           WHERE biz_type = 'knowledge' AND biz_id = <知识id>;     -- 期望恒为 1（不含卸载行）
# 再验证"复活"：删掉该挂载（attachment/remove）后重发同一请求 → 期望 code = 200 且 del_flag 回到 0

# ⑤ attachName 必填（不得由 originalName 回填）
#    入参 attachments 元素缺 attachName → 期望 code != 200（msg 提示不能为空）
#    **不得**返回 200 并显示 aik_sys_file.original_name

# ⑥ 悬空 fileId 守卫：attachments 里塞一个不存在的 fileId → 期望 code != 200
# ⑦ bizType 白名单：GET findByBiz?bizType=unknown&bizId=1 → 期望 code != 200

# ⑧ 下载文件名【取自挂载层】
ATTACH_ID=$(curl -s "$API/grimoire/attachment/findByBiz?bizType=knowledge&bizId=<知识id>" | grep -Eo '"id":"[0-9]+"' | head -1 | cut -d'"' -f4)
curl -s -D - -o /dev/null "$API/grimoire/file/download?id=$FILE_ID&attachId=$ATTACH_ID"
# 期望 Content-Disposition 的文件名为挂载行 attach_name（"我的笔记.pdf"），**不是** original_name
curl -s -D - -o /dev/null "$API/grimoire/file/download?id=$FILE_ID"
# 台账模式（不带 attachId）→ 期望文件名 = aik_sys_file.original_name
curl -s "$API/grimoire/file/download?id=$FILE_ID&attachId=<别的挂载id>"   # 期望 code/异常：挂载与文件不匹配
# 另测：attachId 指向 del_flag = 1 的行 → 期望拒绝

# ⑨ 删除守卫 + 引用计数（§2.5.5、SDD §4.3）
curl -s -X POST "$API/grimoire/file/remove" -H 'Content-Type: application/json' -d "{\"id\":\"$FILE_ID\"}"
# 期望 code != 200（仍有有效挂载）
# 把同一 fileId 挂到【两个】知识条目，卸载其一 →
#   期望：文件记录仍在；另一条知识的附件仍能下载（共享不误删）
curl -s -X POST "$API/grimoire/attachment/remove" -H 'Content-Type: application/json' -d "{\"id\":\"$ATTACH_ID\"}"
# 卸载【唯一】挂载后 → 期望：aik_sys_file 行与磁盘文件【均消失】
ls "$UPLOAD_DIR"/*/*/*/"$(basename <file_path>)"     # 期望：No such file

# ⑩ 对账 SQL（SDD §2.3.2，三条都必须为空）
#   ① 同 md5 多行；② 悬空挂载；③ 无引用文件
#   期望：① 空（并发窗口下非空可接受，需登记）；② 空；③ 空（非空仅表示"删盘失败"）
```

### Phase 5：Stats 模块（§2.7）

**验收**：

```bash
curl -s 'http://localhost:18900/grimoire/knowledge/stats'
# 对账：knowledgeCount == SELECT COUNT(*) FROM aik_knowledge WHERE status = 1
#       categoryCount  == SELECT COUNT(*) FROM aik_knowledge_category WHERE status = 1
#       tagCount       == SELECT COUNT(*) FROM aik_knowledge_tag
#       Σ typeDistribution[].count == knowledgeCount
#       recentEdited 长度为 5，modifyTime 降序
```

### Phase 6：种子数据与全量对账

**M2 已应用、M4 无需执行**（§3.1 实测：5 个分类存在、0 悬空）。本 Phase 只剩 M3，然后执行全量对账：

```sql
-- ① 悬空外键必须为 0（实测已为 0）
SELECT COUNT(*) FROM aik_knowledge k
LEFT JOIN aik_knowledge_category c ON c.id = k.category_id
WHERE k.category_id IS NOT NULL AND c.id IS NULL;   -- 期望 0

-- ② 分类名为空的知识条目必须为 0（status = 1 且有 category_id 的）
SELECT COUNT(*) FROM aik_knowledge k
JOIN aik_knowledge_category c ON c.id = k.category_id
WHERE k.status = 1 AND (c.category_name IS NULL OR c.category_name = '');   -- 期望 0

-- ③ 分类条目数与实测基线对账（§3.1：8 / 3 / 2 / 6 / 1，合计 20）
SELECT c.category_name, COUNT(k.id)
FROM aik_knowledge_category c
LEFT JOIN aik_knowledge k ON k.category_id = c.id
GROUP BY c.id, c.category_name ORDER BY c.id;
```

> ⚠️ **不要重复执行 M2**：5 个分类的 `id` 已存在，`INSERT` 会撞主键。

### 4.1 全局回归

```bash
# 既有接口未被破坏（Phase 1 改了 4 个类，必须回归）
mvn -q clean test          # 若尚无测试，至少 mvn -q clean package 通过
curl -s 'http://localhost:18900/v3/api-docs' | head -c 200   # SpringDoc 仍可生成
curl -s -o /dev/null -w '%{http_code}' 'http://localhost:18900/swagger-ui.html'  # 期望 200

# M1 之后：运行时结构与"全新安装"脚本【逐字段等价】（列集合 / 列序 / 索引 / COMMENT，SDD §2.3）
SHOW CREATE TABLE aik_sys_file;
SHOW CREATE TABLE aik_sys_attachment;
# 断言 aik_sys_file      ：无 url、无 del_flag、无 idx_del_flag；保留 idx_md5 且 NON_UNIQUE = 1
# 断言 aik_sys_attachment：无 knowledge_id、无 attach_url、无 idx_knowledge_id；
#                          有 file_id / biz_type / biz_id / del_flag + uk_biz_file / idx_biz / idx_file_id；
#                          TABLE_COMMENT = '通用附件挂载表'
# （2026-09-20 迁移后实测已满足以上全部断言；SDD §2.3.4 为同一基准）
```

### 4.2 非回归风险点

| 改动 | 风险 | 缓解 |
|---|---|---|
| `KnowledgeQuery.status` 默认 1 | 管理端列表默认看不到禁用条目 | 管理端页显式发送 `"status": null`；写入 §2.2.3 并同步到前端文档 |
| `KnowledgeVo` 扁平化 + `tags` 整形 | 破坏既有响应结构 | 前端尚未开工，无消费方；Swagger 手测覆盖 |
| `findPage` 增加 3 个过滤条件 | 现有 `findPage` 的 N+1 查询被放大 | §4.3 |
| `@Select` 聚合查询 | 新增 SQL 需自行保证 `status` 口径 | 每个聚合都有对应验收对账 SQL |
| `FileVo` 删 `url` / `filePath` / `storedName` / `md5` | 破坏既有响应结构；`BeanUtil.copyProperties` 是全量拷贝，**删字段才阻断泄漏** | 前端尚未开工，无消费方；Phase 4 ① 复核字段集 |
| 附件 `save` 改**差量** | 若误按 v1.1 的"全删再全插"实现 → 每次编辑都撞 `uk_biz_file` | Phase 4 ④ 的"重复提交同一请求"验收；契约已显式作废旧语义（§2.5.2） |
| `aik_sys_attachment` 结构重写（M1） | **破坏性 DDL**；`NOT NULL` 无默认值列在非空表上不可执行 | §3.0 硬断言（两表 0 行）+ 执行前备份；回滚见 §7.3 |
| `attachId` 可省略 | 台账模式下仍可按 `fileId` 直接下载（方案 1 的守卫只在挂载模式生效） | 登记为已知残余风险（§2.5.5）；接入鉴权时改为挂载必填 + 独立台账端点 |
| `md5` 去重是应用层约定 | 极端并发可能落两行同 `md5`（无数据损坏，仅存储冗余） | SDD §2.3.2 查询① 对账检出；`idx_md5` 保持非唯一（§3.2） |

### 4.3 已知未修问题（记录，P0 不处理）

`KnowledgeServiceImpl.findPage` 对**每一行**执行：1 次分类查询 + 1 次标签关联查询 +
1 次标签批量查询。`size=20` 时约 61 次查询。

- 在 20 条数据的规模下可接受。
- **列为独立技术债**，修复方向是 `findById` 式批量回填（收集全页 `categoryId` / `knowledgeId`，
  各查一次）。P0 不处理，因为时间树的 `size` 若设到 500 会显著放大该问题——
  **前端文档必须约束时间树的单次 `size`**（建议 ≤ 100，配合分页）。

---

## 5. 非目标

| 非目标 | 原因 |
|---|---|
| **鉴权 / 权限** | 用户决策：当前仅本人访问，功能优先，后续补充。**已知风险**见 §7 |
| FULLTEXT / ngram 全文检索 | 规模不匹配，已定迁移触发条件（§3.3） |
| `use_count` 列删除 | 破坏性，需显式确认 |
| 标签 `status` 字段 | 超出 P0 |
| 前端代码与 `frontend-design.md` 的执行修订 | 见 §6，本方案只列清单 |
| 分类/标签的级联删除 | 静默丢数据，代价不可逆 |
| `findPage` 的 N+1 优化 | 独立技术债（§4.3） |
| 接口动词归一（`update` vs `modify`） | 会破坏既有契约，另立变更 |
| `ImportKnowledge.java` 的可移植化 | 该文件在 `.gitignore` 中，属一次性工具 |

---

## 6. 下游文档修订清单

本方案定稿后，以下文档需原地修订（**不新建平行文档**）：

### 6.1 `grimoire-files/design-docs/frontend/design.md`

| 位置 | 问题 | 修订 |
|---|---|---|
| §后端 API 现状（第 11-21 行） | 表头写"已验证"，但缺 Category/Tag/Attachment，且无 HTTP 方法列 | 重写：补方法列、标注已实现/未实现、引用本文档 |
| §知识数据模型（第 23-31 行） | `Knowledge` 写成扁平模型 | 改为本文档 §2.6.2 的实际形态 |
| §API 封装要点（第 207-211 行） | 只说"ID 字段按 string" | 补：分页元数据（`total`/`size`/`current`/`pages`）也是字符串（§2.1.3） |
| 同上 | 未提错误模型 | 补：所有错误 HTTP 200，判 `code`（§2.1.2） |
| 同上 | 未提 null 语义 | 补：`NON_NULL`，字段全可选（§2.1.4） |
| §API 封装要点 第 210 行 vs §注意事项 1 第 505 行 | 自相矛盾（`/api` + `/grimoire` vs 仅 `/grimoire`） | 统一为仅代理 `/grimoire` |
| §视觉风格 色板 | `#8a8a8a` 对比度 3.20:1、`#b8860b` 3.01:1、时间轴线远端 `#d4a574` 2.06:1，均不达 WCAG AA | 次文字降至约 `#6b6b6b`；金色降至约 `#8a6209` 以下或限定为装饰 |
| §暗色模式 | 卡片 `#121a2b` 对页面 `#0a0e17` 仅 1.11:1，"轻微阴影"不可见 | 暗色改用可见边框替代阴影 |
| §首页设计要点（第 298-303 行） | "不展示任何知识条目列表"与"向下滚动淡入统计"矛盾 | 二选一：单屏封面 或 滚动落地页 |
| §类型标签配色（第 229 行） | 只有"绿/蓝/紫/橙"，无色值 | 补亮/暗两套具体 hex |
| §分阶段实施 | 5 个 Phase 无后端工作，Phase 1 交付三个死链 | 插入本文档 Phase 0-6 为前置；重排前端 Phase |
| §注意事项 3 | 只说用 DOMPurify | 补：或在 markdown-it 禁 `html`（更省的防线） |
| §目录结构 | 缺 `vite-env.d.ts` / `auto-imports.d.ts` / `components.d.ts` / `tsconfig.node.json` | 补齐 |
| §目录结构 `api/types/*.d.ts` | 承载被 import 的业务类型应为 `.ts` | 改为 `.ts` |
| §分阶段实施 | 无测试/lint/CI/包管理器 | 补充 |
| §知识数据模型（第 23-31 行）与 §后端 API 现状 | **附件字段仍是旧模型**：`Attachment: knowledge_id, attach_name, attach_url`（实测该文件第 35 行） | 改为 `fileId`（挂载锚点）+ `attachId` + `attachName` + `description` + `sortOrder`；**不得**拼任何路径/URL；下载统一走 `/grimoire/file/download?id=&attachId=`（§2.5.3、§2.5.5，SDD §2.5.8） |

### 6.2 `grimoire-files/design-docs/knowledge-module/design.md`

| 位置 | 漂移 | 修订 |
|---|---|---|
| §2 第 44 行 | `mapper/` | 实际是 `dao/` |
| §2 第 53 行 | `CategoryPo.java` | 实际是 `KnowledgeCategoryPo.java` |
| §2 第 52 行 | `KnowledgeAttachmentPo.java` 在 `knowledge/common/po/` | 实际是 `SysAttachmentPo`，在 `system/attachment/po/` |
| §2 第 37-42 行 | 列出了 `CategoryController` / `TagController` / `CategoryService` / `TagService` | 标注为"已设计未实现，补齐见 `backend-api-contract.md`" |
| §3.1 | `title VARCHAR(200)` / `code VARCHAR(100)` / `summary VARCHAR(500)` 等 | 实际为 256 / 128 / 512 / 256 / 512 / 512 |
| §3.2 | 表名 `aik_knowledge_attachment` | 实际是 `aik_sys_attachment` |
| §3.2 | 字段 `knowledge_id` / `attach_url`（实测该文件第 134 行仍在） | **两列均已不存在**（M1 后 `DROP`）；该处改为指向 `SDD.md` §2.5 的**通用挂载表**（`file_id` + `biz_type` + `biz_id`），并说明本表**归属 system 模块**、不是 knowledge 模块的表 |
| 新增 | 无接口契约 | 引用 `backend-api-contract.md` 作为契约权威 |

### 6.3 `grimoire-files/design-docs/system-module/SDD.md`

**状态：本节的修订对象已自行完成附件域定稿，因此改为"反向校准"，避免两文档互相漂移。**

| 位置 | 原登记 | 现状 / 需反向同步的内容 |
|---|---|---|
| §2.2 四张表名（`aik_dict_type` / `aik_dict_item` / `aik_system_param` / `aik_file_record`） | 实际为 `aik_sys_*` | ✅ 已同步 |
| §2.2 补 `aik_sys_attachment` | "新增表结构（含 M1 的 `file_id`）" | ✅ 已完成，且已升级为 **v1.2 两层模型**（文件对象 / 挂载点）——**取代** v1.1 的"加 `file_id` 列"方案 |
| §3.1 API 列表 补附件接口 | 补本文档 §2.5.2 | ✅ 已补；**但路径需反向同步**：`/grimoire/attachment/findByKnowledgeId` → **`/grimoire/attachment/findByBiz`**（本文件的路径泛化裁决，§2.5.2；SDD 该节已声明路径属契约决策、以本文件为准） |
| §2.5.8 下游清单第 1 行（`api-contract.md` §2.5.1 / §3 M1） | 「`M1`（`ADD COLUMN file_id BIGINT NULL`）**作废**，改为完整改造 DDL」 | ✅ 已执行：本文件 §2.5.1 给出**变更形状 + 脚本指针**，§3 M1 标注"破坏性 / 已执行"，脚本权威 = `sql/attachment_two_layer_migration.sql`（本文件不复制脚本，避免两处 owner 漂移） |
| §2.5.8 下游同步清单第 2 行 | 「`AttachmentVo` 增 `bizType` / `bizId`」 | ⚠️ **该句不再适用**：本契约裁决 `AttachmentVo` **不回显** `bizType` / `bizId`（无消费方，§2.5.3）。建议该行收敛为指向本文档 §2.5.3 |
| §2.5.4 / §2.5.6（秒传、下载文件名） | — | ✅ 已与本文件一致（甲方案 = 应用层约定、非 DB 约束；下载文件名取自挂载层 `attach_name`） |
| §2.5.6 〔K1〕（SDD **v1.3** 新裁决） | v1.2 曾把 `getUrl()` 方法链登记为"有意保留（反熵例外）" | ⚠️ **v1.3 已改为删除**（接口方法 + 三个实现）。本文件 §2.5.5 已同步为"一并删除"，并记录 Reopen Trigger：改用新增的 `presignedUrl`，**不得**复活持久化地址字段 |
| §2.3.4 / §2.5.8 〔K2〕（SDD **v1.3** 新事实） | 迁移"进行中" | ✅ **已落地（2026-09-20）**：真库 = 迁移后结构（`aik_sys_file` 13 列 / `aik_sys_attachment` 12 列），表已迁入 `sql/aik_system_tables.sql`。本文件 §2.5.1 / §3 M1 / §3.0 / §4 Phase 4 / §7.2 / §7.3 已按"已执行 + 重放前提"改写 |
| §4.2 / §4.3 / §4.5（上传 / 删除 / 挂载卸载流程） | — | ✅ 已与本文件 §2.5.2（差量）、§2.5.5（引用计数、提交后删盘）一致 |

> 本文件**不修改** `SDD.md`（跨文档 owner）；上表两条"反向同步项"按 SDD §2.5.8 的移交规则**报船长**处置。

### 6.4 `grimoire-files/plans/project-architecture.md`

整份基于已被取代的 `aik_article` / `aik_category` / `aik_tag` / `aik_article_tag` /
`aik_code_snippet` / `aik_attachment` 表名，以及 `/grimoire/article` 接口。

**动作**：文件头加 superseded 标注，指向 `knowledge-module-design.md`（包与表结构）与
`backend-api-contract.md`（HTTP 契约）。**不删除**，保留决策溯源。

---

## 7. 风险与已知问题

### 7.1 已知安全风险（用户已确认接受）

| 项 | 现状 | 处置 |
|---|---|---|
| 无鉴权 | 所有 `add` / `modify` / `remove` / `upload` 接口公开可写 | 用户决策：仅本人访问，后续补充。**本文档登记为已知风险** |
| CORS 全开 | `WebMvcConfig`：`allowedOriginPatterns("*")` + `allowCredentials(true)` | 建议改为从配置读取白名单，默认仅 `localhost:5173`；**不改变功能行为** |
| 操作人写死 | `BaseMetaObjectHandler.getCurrentUser()` 返回 `"system"` | 鉴权接入后替换（既有 TODO） |
| 上传目录与文档目录重合 | `grimoire.file.base-path: ./grimoire-files`，与 git 跟踪的文档目录相同 | 建议改为独立目录；不在 P0 代码范围。**权威登记与完整实测见 [`backend-audit.md`](./backend-audit.md) §11 R6**（含 `.gitignore` 无规则、`FileStorageConfig:52` 兜底永不生效的旁证、三选一修正建议与 Reopen Trigger）；本行仅保留索引，详细分析与修正建议以 R6 为准 |

> `allowedOriginPatterns("*")` 配合 `allowCredentials(true)` 是允许任意源的凭据请求。
> 在无鉴权状态下影响有限，但鉴权一旦接入，这两项组合会立即变成实际漏洞。
> **接入鉴权时必须同步收紧 CORS。**

### 7.2 实施风险

| 风险 | 缓解 |
|---|---|
| Phase 0 的行尾归一化提交体量大（183 文件） | 单独提交、提交信息标注纯格式；不与任何逻辑改动混合 |
| Phase 1 改动 4 个既有类，可能破坏现有手测流程 | §4.1 全局回归；`/v3/api-docs` 与 Swagger UI 可用性检查 |
| ~~分类种子数据的中文分类名需用户确认~~ **已关闭（v1.2）** | 实测已填入（§3.1），无需用户确认；**不得重复执行 M2**（会撞主键） |
| **重放** M1 脚本在非空表上不可执行（`ADD COLUMN ... NOT NULL` 无默认值） | 重放前跑 §3.0 硬断言（两表必须 0 行；迁移后实测仍为 0）；备份证据见 `sql/backup/`（SDD §2.3.4）。M1 本身**已执行完毕**，正常路径无需重放 |
| 附件 `save` 被误实现为"全删再全插" → 每次编辑撞 `uk_biz_file` | 契约已显式作废旧语义（§2.5.2）；Phase 4 ④ 以"同一请求提交两次"验收 |
| 删除路径不校验引用计数 → 误删其它业务共享的文件；或先删盘后回滚 → 悬空引用 | 唯一删除入口 + 引用计数 + **提交后删盘**（§2.5.5、SDD §4.3）；对账 SQL（SDD §2.3.2 三条）定期执行 |
| 下载不传 `attachId` 时文件名退回 `original_name` | 属**设计内行为**（台账模式，§2.5.5）；挂载层调用**必须**带 `attachId`，Phase 4 ⑧ 验收 |
| `findPage` N+1 在 `size` 增大时放大 | §4.3：约束前端时间树单次 `size` ≤ 100 |
| `categoryCode` 唯一性只在 Service 层校验，无 DB 唯一索引 | DDL 缺 `uk_category_code`（§2.3.6）。并发写入可绕过；单用户场景影响可忽略。补索引待 Phase 6 数据整理后另立变更 |
| `aik_knowledge_tag` 无 `status` 列 | 无法实现"停用标签"；`findAll` 恒返回全表（§2.4.2、§2.7） |

### 7.3 回滚

| Phase | 回滚方式 |
|---|---|
| Phase 0 | `git revert` 归一化提交 + 删除 `.gitattributes` |
| Phase 1 | `git revert`（纯 Java 改动，无 DDL） |
| Phase 2 / 3 / 5 | `git revert`（纯新增，无 DDL） |
| Phase 4（含 M1） | M1 **已于 2026-09-20 落地**；回滚 = `git revert` + **重建两表**：`DROP TABLE aik_sys_file, aik_sys_attachment` 后按 SDD §2.3 的 `CREATE TABLE` 重建（**前提：两表仍为 0 行**，无数据损失）。⚠️ **不要**用"反向 ALTER 把 `url` / `knowledge_id` 加回来"——列序与 COMMENT 不会还原到实测形态 |
| Phase 6 | M2 已应用、M4 无需执行（§3.1）→ **无回滚动作**；M3 是列注释变更，`git revert` 不覆盖 DB，需手动还原注释 |

---

## 8. 待用户确认项

| # | 项 | 阻塞的 Phase |
|---|---|---|
| ~~C1~~ | **已关闭（v1.2）**：实测 5 个分类的中文名与 `category_code` 均已填入，且 5 个分类全部 `parent_id = 0`（无父子关系）——**无需用户确认**，见 §3.1 | — |
| C2 | 是否接受 `status` 默认值改为 1 的安全取向（管理端需显式传 `null`） | Phase 1 |
| C3 | CORS 是否收紧为配置化白名单 | 可独立 |
| C4 | 上传目录是否从 `grimoire-files/` 迁出 | 可独立 |
| C5 | 何时执行 `DROP COLUMN use_count` | 不阻塞 |

---

## 9. 变更历史

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-09-20 | 初版：基于 `a95f1d7` 的现状核实，确立 P0 契约与 6 阶段实施 |
| v1.1 | 2026-09-20 | 文档整合：迁入 `design-docs/system-module/api-contract.md`（原 `plans/backend-api-contract.md`）；§0.1 / §6.1–6.4 / §3.1 中的下游文档路径更新为新结构；§0 与 §7.2 的行尾噪音计数按逐文件哈希实测修正为 183；§6.4 的 `SDD-v1.0.md` 引用同步为 `SDD.md`；§6.3 所列 SDD 修订**已执行**（另额外发现并修正包结构漂移） |
| v1.2 | 2026-09-20 | **附件契约定稿（对齐 SDD v1.2 两层模型 + SDD v1.3 的 K1/K2）**：§2.5 整体重写为 5 个子节（M1 形状 / 三端点 / VO 与 DTO / 挂载链路 / File 端点与 `attachId`）；`save` 改差量并作废"整组替换"；删「`attachName` 回填」；`FileVo` 收缩暴露面；`/file/remove` 收紧；`getUrl()` 方法链**一并删除**（v1.3 K1）。§1.3 D7 处置落地；§2.1.3 / §2.1.7 / §2.6.2 / §2.6.4 / §2.7 同步；§3 的 M1 改写为破坏性重写（**已执行**，脚本 `sql/attachment_two_layer_migration.sql`）、新增 §3.0 硬断言（重放前提）、§3.1 按实测改判 M2/M4（C1 关闭）；§4 Phase 4 / Phase 6 / §4.1 / §4.2 重写；§6.1 / §6.2 / §6.3 校准；§7.2 / §7.3 更新。**未改代码、未改库、未执行任何 DDL** |
| v1.2.1 | 2026-09-20 | **契约完整性补注（船长复核后追加，仅 §2.5.2 / §2.5.3）**：`findByBiz` 显式声明**只返回 `del_flag = 0`**并给出理由（`AttachmentVo` 无 `del_flag`，前端无从过滤）；显式声明排序 `ORDER BY sort_order ASC, id ASC`（与 `KnowledgeServiceImpl:193` 现状口径一致）；§2.5.3 补"不回显 ≠ 删列"澄清 + `AttachmentBizType.KNOWLEDGE` 常量来源。**未改 SDD、未改代码、未改库** |
| v1.2.2 | 2026-09-20 | **契约缺陷修复（R18：`AttachmentVo` 6 → 8 字段）**：v1.2 以"无消费方"为由删除 `fileSize` / `fileType`，但 `frontend/design.md` 的知识详情页**先于契约**就要求显示"文件大小 + 类型图标"→ 消费方一直在，属**契约缺陷**（另两选项 N+1 补取 / 砍设计均被否决，理由见 §2.5.3）。恢复 `fileSize`（string，Long→String §2.1.3）与 `fileType`（MIME），并写明**数据来源 = `aik_sys_file` 关联带出**、挂载表**不新增列**、**按 `fileId` 批量取禁 N+1**（§2.5.2）。「明确不含」表把 `originalName` 单列（理由不变：首次上传者的名字，§2.5.4）。§2.6.2 的"6 字段"改"8 字段"；§4 Phase 4 ③ 的期望字段集同步。**未改 SDD（由 t15 跟随）、未改代码、未改库** |

> **§6 执行状态**（2026-09-20）：
> - ✅ §6.3 `SDD.md` — 已同步（20 处表名 + 补附件表 + 修正按层分包→功能域分包 + 补 `aik_sys_file` 7 字段）
> - ⚠️ §6.3 `SDD.md` — **v1.2 起改为反向校准**：附件域已由 SDD v1.2 **自行定稿**，本文件不再提出修订，
>   只保留两条反向同步项（SDD §3.1 的路径 `findByKnowledgeId` → `findByBiz`；SDD §2.5.8 的
>   「`AttachmentVo` 增 `bizType` / `bizId`」不再适用）——两条均按 §2.5.8 移交规则**报船长**处置
> - ✅ §6.4 `project-architecture.md` — 已加 superseded 标注
> - ✅ §6.1 `frontend/design.md` — **附件部分已定稿**（v2.2 的 §附件交互 + 附件数据模型；v2.3 起 `AttachmentVo` 含 `fileSize`/`fileType`）；该文档其余的 15 项待修订与本契约无关（含 1 项需用户决策 R09）
> - ✅ §6.2 `knowledge-module/design.md` — §3.2 已重写为通用挂载表（含 knowledge 侧接入边界）；`knowledge_id` / `attach_url` 两列换代已完成
