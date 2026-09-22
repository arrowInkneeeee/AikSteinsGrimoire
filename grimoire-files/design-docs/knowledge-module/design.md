# Knowledge 模块包目录设计方案（统一化重构版）

> 版本：v1.3 | 更新：2026-09-20 | 状态：生效中 | 权威范围：Knowledge 模块的包结构与表结构（**不含** `aik_sys_attachment`——通用挂载表归属 system 模块，字段权威见 `SDD.md` §2.5）
> v1.0 → v1.1：修正包结构漂移（`mapper/`→`dao/`、包路径改为实际的功能域分包）；表结构对齐真实 DDL；表名 `aik_knowledge_attachment` → `aik_sys_attachment`
> v1.1 → v1.2：更正 §2 的"分类树/标签并入 Knowledge 接口"虚假陈述（此前正文已加标注，**头部版本号漏同步**，本次一并补正）
> v1.2 → v1.3：**附件域两层模型同步**——§3.2 由旧结构（`knowledge_id` + `attach_url`）重写为**通用挂载表**（`file_id` + `biz_type` + `biz_id` + `attach_name` + `description` + `sort_order` + `del_flag`）；补 knowledge 侧**接入边界**（禁止直连 `SysAttachmentMapper`、三处改道统一挂载服务）、`KnowledgeVo.attachments` 改 `AttachmentVo`、附件三端点同步为 `findByBiz` / `save` / `remove`。依据 `SDD.md` v1.4 §2.5 / §2.3.4 与 `api-contract.md` v1.2.1 §2.5
>
> 接口契约权威：[`../system-module/api-contract.md`](../system-module/api-contract.md)（本文档不定义接口）
> 表结构以 `sql/*.sql` 为准（**通用附件挂载表在 `sql/aik_system_tables.sql`**，已不在 `aik_knowledge_tables.sql`）；代码与 DDL 优先于本文档
> ⚠️ 本文档原写于前端项目启动前，"已设计未实现"的部分见 §2 标注。

## 1. 设计背景

Knowledge 模块用于统一管理知识库内容，支持萃取并沉淀解决方案、组件、代码片段、学习笔记等多类型知识。

### 1.1 核心设计原则

**数据库只存元数据，代码产物以真实文件形式存在于项目中可编译运行。**

| 问题 | 修正前（错误） | 修正后（正确） |
|------|--------------|--------------|
| **代码存放位置** | 把代码内容存到数据库 TEXT 字段 | 代码产物以 `.java` 文件存在于 `src/main/java/.../components/` 下 |
| **代码可运行性** | 放在 `library/` 不参与编译 | 放在 `components/` 包下，Spring Boot 扫描注册 |
| **knowledge 模块** | 按类型拆分子包（component/solution/note/snippet） | 统一知识条目管理，通过 `type` 字段区分 |
| **提取方式** | 从数据库跨表拼装 | 直接复制整个目录即可复用 |

### 1.2 架构定位

| 层级 | 职责 | 位置 |
|------|------|------|
| **knowledge 模块** | 纯元数据管理后台（CRUD、检索、统计、分类、标签） | `src/.../knowledge/` |
| **components/ 包** | 可复用组件代码（可编译、当前项目可用） | `src/.../components/` |
| **solutions/ 包** | 解决方案代码（可编译、当前项目可用） | `src/.../solutions/` |

---

## 2. 包目录结构

```
src/main/java/io/aik/steins/grimoire/
├── core/                          # 基础设施（保留不变）
├── system/                        # 系统管理（含附件域 attachment）
├── knowledge/                     # 知识库管理后台（统一化）
│   ├── controller/
│   │   └── KnowledgeController.java     # 知识条目 CRUD + 聚合查询
│   ├── service/
│   │   ├── KnowledgeService.java
│   │   └── impl/
│   │       └── KnowledgeServiceImpl.java
│   ├── dao/                             # 数据访问层（原文档误作 mapper/）
│   │   ├── KnowledgeMapper.java
│   │   ├── KnowledgeCategoryMapper.java
│   │   ├── KnowledgeTagMapper.java
│   │   └── KnowledgeTagRelationMapper.java
│   └── common/
│       ├── po/
│       │   ├── KnowledgePo.java           # 统一主表
│       │   ├── KnowledgeCategoryPo.java   # 分类表
│       │   ├── KnowledgeTagPo.java
│       │   └── KnowledgeTagRelationPo.java
│       ├── dto/
│       │   ├── KnowledgeDto.java
│       │   ├── KnowledgeQuery.java
│       │   └── ToggleStatusDto.java
│       ├── vo/
│       │   ├── KnowledgeVo.java
│       │   └── KnowledgeListVo.java
│       ├── enums/
│       │   └── KnowledgeTypeEnum.java     # NOTE/COMPONENT/SOLUTION/CODE
│       └── constant/
│           └── KnowledgeConstant.java
├── components/                    # 可复用组件代码
│   └── threadpool/
│       ├── ThreadPoolConfig.java
│       ├── ThreadPoolManager.java
│       ├── NamedThreadFactory.java
│       ├── AbstractAsyncTask.java
│       ├── TaskExecutor.java
│       ├── TaskExceptionHandler.java
│       ├── RejectionPolicy.java
│       └── README.md
└── solutions/                     # 解决方案代码（预留）
```

**已设计未实现**（补齐见 `../system-module/api-contract.md`）：

| 计划项 | 归属 | 状态 |
|---|---|---|
| `CategoryController` / `CategoryService` | knowledge | 未实现。**分类树完全没有暴露**：`KnowledgeController` 仅 6 个端点，无树查询；全仓无 `CategoryService` 符号 |
| `TagController` / `TagService` | knowledge | 未实现。**标签没有暴露任何接口**，仅 `KnowledgeListVo.tags` 以 `List<String>` 间接返回名称 |
| 附件 Controller / Service | `system/attachment` | 未实现：现状**仅** `dao/SysAttachmentMapper` + `po/SysAttachmentPo`。设计见 `SDD.md` §2.5 / §4.5，接口路径见契约 §2.5.2，**knowledge 侧接入边界见本文档 §3.2** |

> **修订说明（v1.2）**：上表前两行原写"分类树并入 Knowledge 接口""标签并入 Knowledge
> 接口"，系 v1.1 修订时引入的**虚假陈述**——代码中不存在此并入。已按实际 6 个端点更正。

> **附件表归属修正**：附件表 `aik_sys_attachment` 归属 **system 模块**（`SysAttachmentPo`
> 位于 `system/attachment/po/`），**不在** `knowledge/common/po/` 下，且类名不是
> `KnowledgeAttachmentPo`。本文档初版此处有误。
>
> **（v1.3）** 该表的**结构与接入边界**见 §3.2：它已被重写为**通用附件挂载表**
> （`file_id` + `biz_type` + `biz_id`），knowledge 侧**禁止**直连 `SysAttachmentMapper`，
> 必须改道统一挂载服务。

---

## 3. 数据库表设计

### 3.1 统一知识主表 `aik_knowledge`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| title | VARCHAR(256) | 标题（组件名/方案名/笔记标题/片段描述） |
| code | VARCHAR(128) | 编码（UK） |
| type | TINYINT | 1-笔记 2-组件 3-方案 4-片段 |
| summary | VARCHAR(512) | 摘要/用途描述 |
| content | TEXT | 正文（笔记/代码片段/方案描述） |
| source_project | VARCHAR(256) | 来源项目 |
| source_path | VARCHAR(512) | 来源路径 |
| resource_path | VARCHAR(512) | 资源路径（指向 components/ 或 solutions/ 下的包路径） |
| ext_json | JSON | 扩展字段（不同类型特有属性） |
| category_id | BIGINT | 分类ID |
| status | TINYINT | 1-启用 0-禁用（默认 1） |
| create_time / modify_time | DATETIME | 审计时间 |
| create_by / modify_by | VARCHAR(64) | 审计人 |

**索引**：`UNIQUE KEY uk_code (code)`、`idx_type`、`idx_category_id`、`idx_status`

> 字段长度与索引以 `sql/aik_knowledge_tables.sql` 为准。

### 3.2 通用附件挂载表 `aik_sys_attachment`

> **归属**：该表归属 **system 模块**（PO = `system/attachment/po/SysAttachmentPo`），**不在**
> `knowledge/common/po/` 下，类名也**不是** `KnowledgeAttachmentPo`；建表语句位于
> **`sql/aik_system_tables.sql`**（`sql/aik_knowledge_tables.sql` **已不含**该表）。
> knowledge 模块**只作为业务使用方**通过 `(biz_type = 'knowledge', biz_id = aik_knowledge.id)` 引用它。
>
> **权威边界**：下表是**现状快照**；字段与索引的**唯一权威**是 `SDD.md` §2.2 / §2.3.4 与
> `sql/aik_system_tables.sql`，不一致时以后者为准（沿用本文档既有约定：代码与 DDL 优先于本文档）。
>
> **v1.3 重写**：v1.2 及之前本节写的是**旧结构**（`knowledge_id` + `attach_url`）。该结构已被
> **破坏性 DDL（M1）** 取代，并已于 **2026-09-20 对真库落地**：`attach_url` 被 `DROP COLUMN`、
> `knowledge_id` 被 `CHANGE` 为 `biz_id`、新增 `file_id` / `biz_type` / `del_flag`。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK NOT NULL | 主键（雪花ID）= **挂载行 id**（即接口契约中的 `attachId`） |
| file_id | BIGINT | NOT NULL | 被挂载的**文件对象**ID，逻辑关联 `aik_sys_file.id`（两层层模型的第 1 层） |
| biz_type | VARCHAR(32) | NOT NULL | 业务类型：`knowledge`（**开放式取值**，常量 `AttachmentBizType.KNOWLEDGE`） |
| biz_id | BIGINT | NOT NULL | 业务主键：`biz_type = 'knowledge'` 时为 `aik_knowledge.id` |
| attach_name | VARCHAR(256) | NOT NULL | **用户可见文件名（权威）**——归**挂载层**所有，**不得**由 `aik_sys_file.original_name` 回填 |
| description | VARCHAR(512) | NULL | 描述（"这份附件是什么"，与业务自身的描述不是一回事） |
| sort_order | INT | NOT NULL DEFAULT 0 | 排序号 |
| del_flag | TINYINT | NOT NULL DEFAULT 0 | 卸载标记：0-有效挂载，1-已卸载（**卸载保行**，供复活与对账） |
| create_time / modify_time | DATETIME | NOT NULL | 审计时间 |
| create_by / modify_by | VARCHAR(64) | NULL | 审计人 |

**索引（实测）**：`PRIMARY KEY (id)`、`UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id)`、
`KEY idx_biz (biz_type, biz_id, del_flag, sort_order)`、`KEY idx_file_id (file_id)`。
旧索引 `idx_knowledge_id` **已随列删除**；`TABLE_COMMENT = '通用附件挂载表'`（12 列）。

> ⚠️ `uk_biz_file` **不含 `del_flag`** → 卸载保行产生的**墓碑行仍占用键值**，因此
> "同一业务重新挂载同一文件"**必须走 upsert / 复活**，**不得**裸 `insert`
> （权威：`SDD.md` §2.5.7；差量语义见 `api-contract.md` §2.5.2）。

**knowledge 模块的接入边界（v1.3 新增）**

**规则**：挂载的读写**必须**经 system 模块的**统一挂载服务**；knowledge 模块**不得**直连
`SysAttachmentMapper`，也**不得**自行拼 `biz_type` / `biz_id`（`bizType` 由服务固定为
`AttachmentBizType.KNOWLEDGE`，不由前端传）。权威：`SDD.md` §2.5.7 / §4.5。

**现存三处跨模块直连必须改道**（实测 `KnowledgeServiceImpl`）：

| 位置 | 现状 | 改道后 |
|---|---|---|
| `:52` | `private final SysAttachmentMapper sysAttachmentMapper;` | 改为注入**统一挂载服务** |
| `:120-122` | 删除知识条目时 inline `sysAttachmentMapper.delete(... eq(knowledgeId, id))` | 调统一服务的**卸载**入口，且与知识删除处于**同一事务**（`knowledge_id` 列已不存在 → 现状**编译期即断**） |
| `:190-194` | 详情页 inline `selectList(... eq(knowledgeId, id).orderByAsc(sortOrder))` | 调统一服务的**按业务查询**（只返回 `del_flag = 0`，按 `sort_order` 升序） |

**响应形状**：`KnowledgeVo.attachments` 由 `List<SysAttachmentPo>` 改为专用 **`AttachmentVo`**
（`KnowledgeVo.java:33` 现状是 **PO 实体直出**，会把 `biz_type` / `biz_id` / `del_flag` 等内部列
暴露给前端，且响应结构随 PO 漂移）：

```java
public class AttachmentVo {           // 形状权威 = api-contract.md §2.5.3
    private Long          id;          // 挂载行主键（= attachId）
    private Long          fileId;      // 下载 / 预览的定位锚点
    private String        attachName;  // 用户可见文件名
    private String        description;
    private Integer       sortOrder;
    private LocalDateTime createTime;
}
```

**不含** `bizType` / `bizId`（**不回显**：三端点均按业务发起，无消费方；**不是删列**——两列仍存在于
表中）、`url` / `attachUrl` / `filePath` / `storedName` / `md5` / `knowledgeId`。

**接口（指针，不在此重复定义）**：路径 / 入参 / 出参的权威 = `api-contract.md` §2.5.2。

| 接口 | 方法 | 最终形态 |
|---|---|---|
| `/grimoire/attachment/findByBiz` | GET | `?bizType=knowledge&bizId=` —— **已废弃** v1.1 的 `findByKnowledgeId`（路径里写业务名 = 把刚删掉的 `knowledge_id` 硬编码从列搬到 URL） |
| `/grimoire/attachment/save` | POST | **差量**语义（**不是**"整组替换"——整组替换必撞 `uk_biz_file`） |
| `/grimoire/attachment/remove` | POST | `IdDto` = **挂载行** id（不是 `fileId`） |

knowledge 侧的挂载读写**不走**上述三端点，而走 `KnowledgeDto.attachments` / `KnowledgeVo.attachments`
（`bizType` 不由前端传），但两者**必须落到同一个统一挂载服务**。

### 3.3 保留的表

- `aik_knowledge_category` — 分类树
- `aik_knowledge_tag` — 标签
- `aik_knowledge_tag_relation` — 知识-标签关联

---

## 4. 萃取流程（含类型自动识别）

```
源项目分析 → 生成萃取规范文档（含类型标记）
                   ↓
            读取类型标记（COMPONENT/SOLUTION）
                   ↓
            确定生成路径：components/ 或 solutions/
                   ↓
            在本项目复写标准化代码
                   ↓
            代码审查（质量/安全/风格）
                   ↓
            询问用户：是否将元数据入库？
                   ↓
          ┌────────┴────────┐
          ↓                 ↓
        是(Y)              否(N)
          ↓                 ↓
    创建 KnowledgePo    仅保留代码文件
    记录分类、标签
    完成入库
```

---

## 5. 代码产物存放规范

### 5.1 组件代码

- 路径：`src/main/java/io/aik/steins/grimoire/components/{component-code}/`
- 包名：`io.aik.steins.grimoire.components.{component-code}`
- 示例：`components/threadpool/` 下存放线程池组件全部代码

### 5.2 方案代码

- 路径：`src/main/java/io/aik/steins/grimoire/solutions/{solution-code}/`
- 包名：`io.aik.steins.grimoire.solutions.{solution-code}`

### 5.3 resource_path 约定

- 组件：`io.aik.steins.grimoire.components.threadpool`
- 方案：`io.aik.steins.grimoire.solutions.order-timeout-cancel`

---

## 6. 元数据入库示例

### 组件入库

```
标题：线程池管理器
编码：thread-pool-manager
type：2（组件）
摘要：基于 ThreadPoolExecutor 的轻量级封装
resource_path：io.aik.steins.grimoire.components.threadpool
ext_json：{"purpose":"统一线程池管理","applicableScene":"高并发异步任务","dependencies":["lombok","spring-boot-starter"],"classCount":7}
```

### 方案入库

```
标题：订单超时自动取消方案
编码：order-timeout-cancel
type：3（方案）
摘要：基于延时队列的订单超时处理机制
resource_path：io.aik.steins.grimoire.solutions.order-timeout-cancel
ext_json：{"problemDesc":"订单支付后30分钟未支付自动取消","steps":[{"stepNo":1,"title":"创建延时队列","content":"..."},{"stepNo":2,"title":"监听超时事件","content":"..."}]}
```

> **注**：「订单超时自动取消」只是 `ext_json.steps` 结构的**示意样例**，
> 本仓库无订单模块，勿据此推断存在订单业务。
> 与 `grimoire-agent/agent/decision-log.md` 中那条「订单状态机用枚举管理」同源，
> 均为示例内容。

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | — | 初版（统一化重构方案） |
| v1.1 | 2026-09-20 | 迁移至 `design-docs/knowledge-module/`；`mapper/` → `dao/`；`CategoryPo` → `KnowledgeCategoryPo`；附件表由 `knowledge/common/po/KnowledgeAttachmentPo` 修正为 `system/attachment/po/SysAttachmentPo`，表名 `aik_knowledge_attachment` → `aik_sys_attachment`；主表字段长度对齐真实 DDL（256/128/512/256/512/512）；标注三项"已设计未实现"；补契约指针 |
| v1.2 | 2026-09-20 | **更正 v1.1 引入的虚假陈述**：删除"分类树/标签并入 Knowledge 接口"——代码中不存在此并入，`KnowledgeController` 仅 6 端点。与 `system-module/backend-audit.md` 的 X1 同源 |
| v1.3 | 2026-09-20 | **附件域两层模型同步**：§3.2 重写为**通用附件挂载表**（12 列 + 索引 `uk_biz_file` / `idx_biz` / `idx_file_id`，旧 `idx_knowledge_id` 已删；`TABLE_COMMENT='通用附件挂载表'`），删除旧结构（`knowledge_id` / `attach_url`）与已作废的"待变更 M1（加 `file_id` 列）"；新增 knowledge 侧**接入边界**（三处跨模块直连改道统一挂载服务 + 同事务 + 禁止直连 Mapper）与 `AttachmentVo` 形状；§2 附件行与归属修正块补指针；文档头 v1.1 → v1.3（**补正 v1.2 头部版本号此前漏同步**）并把表结构基准由 `aik_knowledge_tables.sql` 改为 `sql/*.sql`。依据 `SDD.md` v1.4 §2.5 / §2.3.4、`api-contract.md` v1.2.1 §2.5。**未改接口定义、未改 sql/、未改代码** |
