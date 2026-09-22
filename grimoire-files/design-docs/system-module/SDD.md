# 系统设计文档 (SDD) — system 模块

> 版本：v1.5 | 更新：2026-09-20 | 状态：生效中 | 权威范围：system 模块分层、包结构、表结构、实体定义；**附件与文件域的「两层模型」唯一权威见 [§2.5](#25-附件两层模型与归属边界附件域唯一权威)**
> v1.4 → v1.5：**O 收尾精确化**。① `AttachmentVo` 由 6 字段改为 **8 字段**（+`fileSize` / `fileType`，**文件层属性**，来源与批量取法以 `api-contract.md` §2.5.3 为准）；② §2.5.8「AttachmentVo 暴露面」补**暴露面边界澄清**；③ 修正 §2.5.6 两处对 `AbstractFileStorage` `download` 重载的**分类错误**（`:162-167` 是接口 `:53` 的 `@Override`，在接口内）；④ 新增**行区间引用约定**声明（含 `@Override` 注解行）；⑤ 修正 v1.3 压缩头措辞。**§2.5 的裁决实体未改动**。
> v1.3 → v1.4：**跟随 `api-contract.md` v1.2 的两处裁决做接口口径同步**。① §3.1 附件三端点同步为 `findByBiz` / `save` / `remove`，`findByKnowledgeId` 标注废弃，`file/download` 行补 `attachId` 条件必填；② §2.5.8 下游清单第 2 行「`AttachmentVo` 增 `bizType` / `bizId`」**作废并收敛**为指向 `api-contract.md` §2.5.3（不回显，含 Reopen Trigger）。**§2.5 的设计裁决实体未改动**。
> v1.2 → v1.3：**迁移后同步**。附件域 DDL 已于 2026-09-20 对真库 `aik_steins_grimoire` 落地；本版把文档里所有"迁移中 / 迁移落地前"类**现状陈述**改为已落地事实，并新增 §2.3.4 迁移后实测状态。**§2.5 其余设计裁决未改动**——本版**确实改了 §2.5.6**：`getUrl()` 方法链由"有意保留"改为**删除**（见 §7.3 的 v1.3 行）。
> v1.1 → v1.2：附件域改造定稿。`aik_sys_file` = 文件对象（**物理删除**，摘除 `del_flag` 与 `@TableLogic`）；`aik_sys_attachment` = 通用挂载点（`file_id` + `biz_type` + `biz_id` + `del_flag`，删除 `knowledge_id` / `attach_url`）；删除 `url` 列及其持久化调用链（`getUrl()` 方法与三个实现当时登记为按需保留，**v1.3 依船长裁决改为一并删除**，见 §2.5.6），访问统一走下载/预览接口；md5 秒传定为**应用层约定，非数据库约束**（`idx_md5` 保持非唯一）；修订删除契约（唯一入口 + 挂载引用计数 + 提交后删盘 + 并发串行化）；纠正"运行时存在 `uk_md5` 唯一键"这一错误前提
>
> 初次生成：2026-05-15 | 基于 PRD：v1.0
> 表结构以 `sql/aik_system_tables.sql`、`sql/aik_knowledge_tables.sql` 为准（代码与 DDL 优先于本文档）
> ✅ **附件域迁移已落地（2026-09-20）**：`aik_sys_attachment` 现位于 `sql/aik_system_tables.sql`，
> 并已从 `sql/aik_knowledge_tables.sql` 移除（归属裁决见 [§2.5.8](#258-物理归属与文档同步)）。
> 可执行迁移脚本：`sql/attachment_two_layer_migration.sql`（幂等、可重放）；
> 迁移后实测结构见 [§2.3.4](#234-迁移后实测状态2026-09-20已落地)，迁移前基线见 §2.3.0。
> 接口契约权威：[`api-contract.md`](./api-contract.md)（本文档 §3.1 仅为索引）

---

## 1. 架构设计

### 1.1 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| Web | Spring Boot | 2.7.18 | Web MVC |
| ORM | MyBatis-Plus | 3.5.5 | CRUD + 分页 |
| DB | MySQL | 8.0 | 关系型数据库 |
| Pool | Druid | 1.2.20 | 连接池 |
| Tools | Hutool | 5.8.25 | 工具库 |
| API Doc | SpringDoc | 1.7.0 | OpenAPI |
| JSON | Fastjson | 2.0.43 | 序列化 |
| Validation | JSR-303 | - | 参数校验 |

### 1.2 包结构

> 按**功能域**分包（非按层分包）。每个功能域内自带 `controller` / `service` / `dao` / `po` / `dto` / `vo`。

```
io.aik.steins.grimoire.system/
├── common/                        # 模块内公共组件
│   └── constant/
│       └── SystemConstant.java
├── dict/                          # 功能域：字典
│   ├── controller/
│   │   ├── DictTypeController.java
│   │   └── DictItemController.java
│   ├── service/
│   │   ├── DictTypeService.java
│   │   ├── DictItemService.java
│   │   └── impl/
│   │       ├── DictTypeServiceImpl.java
│   │       └── DictItemServiceImpl.java
│   ├── dao/                       # 数据访问层（Mapper）
│   │   ├── DictTypeMapper.java
│   │   └── DictItemMapper.java
│   ├── po/
│   │   ├── DictTypePo.java
│   │   └── DictItemPo.java
│   ├── dto/
│   │   ├── DictTypeQuery.java
│   │   ├── DictTypeDto.java
│   │   ├── DictItemQuery.java
│   │   ├── DictItemDto.java
│   │   └── DictCodesDto.java
│   └── vo/
│       ├── DictTypeVo.java
│       ├── DictItemVo.java
│       └── DictTypeItemsVo.java
├── param/                         # 功能域：系统参数
│   ├── controller/
│   │   └── SystemParamController.java
│   ├── service/
│   │   ├── SystemParamService.java
│   │   └── impl/SystemParamServiceImpl.java
│   ├── dao/
│   │   └── SystemParamMapper.java
│   ├── po/
│   │   └── SystemParamPo.java
│   ├── dto/
│   │   ├── SystemParamQuery.java
│   │   └── SystemParamDto.java
│   ├── vo/
│   │   └── SystemParamVo.java
│   └── enums/
│       └── SystemParamGroupEnum.java
├── file/                          # 功能域：文件
│   ├── controller/
│   │   └── FileController.java
│   ├── service/
│   │   ├── FileService.java
│   │   └── impl/FileServiceImpl.java
│   ├── dao/
│   │   └── FileMapper.java
│   ├── po/
│   │   └── FileRecordPo.java
│   ├── dto/
│   │   ├── FileQuery.java
│   │   └── FileRenameDto.java
│   └── vo/
│       └── FileVo.java
└── attachment/                    # 功能域：通用附件挂载（供全部业务模块消费）
    ├── constant/                  # ★ 新增
    │   └── AttachmentBizType.java #   biz_type 取值常量（§2.5.5）
    ├── controller/                # ★ 新增（接口清单见 api-contract.md §2.5）
    ├── service/                   # ★ 新增（同上）
    ├── dao/
    │   └── SysAttachmentMapper.java
    └── po/
        └── SysAttachmentPo.java
```

> **注**：`attachment` 域当前仅有 `dao` / `po`；`constant` / `service` / `controller`
> 按本文档 §2.5 与 `api-contract.md` §2.5 补齐。挂载行通过 `file_id` 关联
> `aik_sys_file`，业务归属通过 `(biz_type, biz_id)` 表达——**该域不再有 `knowledge_id`**，
> 它是通用挂载表，knowledge 只是当前唯一的 `biz_type` 取值。

### 1.3 分层职责

| 层级 | 职责 | 约束 |
|------|------|------|
| Controller | 接收请求、参数校验、调用 Service、返回响应 | 仅做参数校验和结果封装，不含业务逻辑 |
| Service | 业务逻辑编排、事务控制、缓存管理 | 不允许直接操作 Mapper，通过接口调用 |
| Mapper | 数据访问、SQL 执行 | 复杂查询写 XML，简单查询用注解 |
| PO | 数据库实体映射 | 仅用于数据持久化，禁止跨层传递 |
| DTO | 接口入参封装 | 含校验注解，Service 层入参 |
| VO | 接口出参封装 | 仅用于响应，可含嵌套结构 |

### 1.4 通用组件复用清单

| 组件 | 来源 | 说明 |
|------|------|------|
| BaseEntity | core/po | id, createTime, modifyTime |
| BaseMetaObjectHandler | core/config | 自动填充时间字段 |
| ApiResponse | core/dto | 统一返回封装 |
| PageQuery | core/dto | 分页查询参数 |
| ResultCode | core/enums | 响应码枚举 |
| BusinessException | core/exception | 业务异常 |
| AssertUtils | core/utils | 业务断言 |
| FileStorageConfig | core/config | 文件存储配置项 |

---

## 2. 数据库设计

### 2.1 ER 关系

```
aik_sys_dict_type (1) --------< (N) aik_sys_dict_item
     | dict_code                 | dict_code (逻辑外键)
     |                           |
     | id PK                     | id PK
     | dict_code UK              | dict_code + item_code UK
     | dict_name                 | item_code
     | description               | item_name
     | status                    | sort_order
     | create_time               | status
     | modify_time               | remark
                               | create_time
                               | modify_time

aik_sys_param                 aik_sys_file  【第 1 层：文件对象】一行 = 一份字节
     | id PK                       | id PK
     | param_key UK                | original_name
     | param_value                 | stored_name
     | description                 | file_path
     | param_group                 | file_size
     | editable                    | file_type
     | create_time                 | storage_type
     | modify_time                 | md5          (KEY idx_md5，内容寻址锚点)
                                   | download_count
                                   | create_time
                                   | modify_time
                                   | create_by
                                   | modify_by
                                        ▲
                                        │  file_id（逻辑外键，1:N）
                                        │  一份文件可被 N 个挂载点复用
                                        │  —— 这正是 md5 秒传的目的
aik_sys_attachment  【第 2 层：挂载点】一行 = 某业务以某名义用某文件
     | id PK
     | file_id        (KEY idx_file_id)
     | biz_type       (取值登记见 §2.5.5，当前仅 knowledge)
     | biz_id         (biz_type 域内主键；knowledge 时为 aik_knowledge.id)
     | attach_name    (用户可见文件名，权威归属见 §2.5.2)
     | description
     | sort_order
     | del_flag       (0-有效挂载，1-已卸载；卸载保行以保留元数据，见 §2.5.7)
     | create_time
     | modify_time
     | create_by
     | modify_by
     | UNIQUE uk_biz_file (biz_type, biz_id, file_id)
     | KEY idx_biz        (biz_type, biz_id, del_flag, sort_order)
```

**关系说明**:
- `aik_sys_dict_type` 与 `aik_sys_dict_item` 为 **一对多** 关系，通过 `dict_code` 逻辑关联（无外键约束，应用层校验）
- `aik_sys_param` 为独立表，无关联关系
- `aik_sys_file` 与 `aik_sys_attachment` 为 **一对多** 关系，通过 `aik_sys_attachment.file_id` 逻辑关联（无外键约束，应用层校验）。
  **一份文件可被 N 个挂载点复用**——这是 md5 秒传的落地形态，也是删除必须走引用计数的原因（§2.5.7）
- `aik_sys_attachment` **不再持有 `knowledge_id`**：业务归属统一由 `(biz_type, biz_id)` 表达，附件表因此成为**通用挂载表**（归属 system 模块），knowledge 只是当前唯一的 `biz_type` 取值
- 两层模型的职责边界、`attach_name` 权威归属、删除语义见 [§2.5](#25-附件两层模型与归属边界附件域唯一权威)——本节仅画结构
- **已删除的节点**：`aik_sys_file.url`（访问地址）、`aik_sys_file.del_flag`（改物理删除）、
  `aik_sys_attachment.knowledge_id`（被 `biz_type`+`biz_id` 取代）、`aik_sys_attachment.attach_url`（真冗余）

### 2.2 表结构设计

#### aik_sys_dict_type（字典类型表）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| dict_code | VARCHAR | 64 | 否 | - | 字典编码，UK |
| dict_name | VARCHAR | 128 | 否 | - | 字典名称 |
| description | VARCHAR | 512 | 是 | - | 描述 |
| status | TINYINT | 1 | 否 | 1 | 状态：1-启用，0-禁用 |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |

**索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_dict_code (dict_code),
KEY idx_status (status)
```

#### aik_sys_dict_item（字典项表）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| dict_code | VARCHAR | 64 | 否 | - | 字典类型编码 |
| item_code | VARCHAR | 64 | 否 | - | 字典项编码 |
| item_name | VARCHAR | 128 | 否 | - | 字典项名称 |
| sort_order | INT | 11 | 否 | 0 | 排序号 |
| status | TINYINT | 1 | 否 | 1 | 状态：1-启用，0-禁用 |
| remark | VARCHAR | 512 | 是 | - | 备注 |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |

**索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_dict_item (dict_code, item_code),
KEY idx_dict_code (dict_code),
KEY idx_status (status),
KEY idx_sort_order (sort_order)
```

#### aik_sys_param（系统参数表）

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| param_key | VARCHAR | 128 | 否 | - | 参数键，UK |
| param_value | VARCHAR | 2048 | 否 | - | 参数值 |
| description | VARCHAR | 512 | 是 | - | 描述 |
| param_group | VARCHAR | 64 | 是 | - | 分组 |
| editable | TINYINT | 1 | 否 | 1 | 是否可编辑：1-是，0-否 |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |

**索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_param_key (param_key),
KEY idx_param_group (param_group)
```

#### aik_sys_file（文件记录表 · **第 1 层：文件对象**）

> 语义：**一行 = 磁盘上的一份字节**。内容寻址（`md5`），**物理删除**（无 `del_flag`）。
> 完整论证见 [§2.5.3](#253-aik_sys_file-的删除语义物理删除)。

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID（`@TableId(type = IdType.INPUT)`，非自增） |
| original_name | VARCHAR | 255 | 否 | - | 原始文件名＝**首次上传者**的文件名。仅用于管理视图/台账，**不是**用户可见名的权威（§2.5.2） |
| stored_name | VARCHAR | 255 | 否 | - | 存储文件名（Snowflake ID + 扩展名）；**不对外暴露**（§2.5.6） |
| file_path | VARCHAR | 512 | 否 | - | 相对存储路径，存储层内部标识；**不对外暴露**（§2.5.6） |
| file_size | BIGINT | 20 | 否 | - | 文件大小（字节） |
| file_type | VARCHAR | 128 | 是 | - | MIME 类型 |
| storage_type | VARCHAR | 16 | 否 | local | 存储类型：local/oss/sftp。⚠️ **当前为只写不读的死列**（见 §6.4 R1），本次不修 |
| md5 | VARCHAR | 32 | 是 | - | 文件 MD5 哈希，**内容寻址锚点**（秒传/复用依据，§2.5.4） |
| download_count | INT | 11 | 否 | 0 | 下载次数（应原子自增，见 §6.2 W07） |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间 |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |
| create_by | VARCHAR | 64 | 是 | - | 创建人 |
| modify_by | VARCHAR | 64 | 是 | - | 修改人 |

**已删除的列**：`url`（见 §2.5.6）、`del_flag`（本表改物理删除，见 §2.5.3）。

**索引**:
```sql
PRIMARY KEY (id),
KEY idx_original_name (original_name),
KEY idx_create_time (create_time),
KEY idx_storage_type (storage_type),
KEY idx_md5 (md5)          -- 非唯一，必须保留：秒传查重走此索引（§2.5.4）
```

> ⚠️ **`idx_md5` 是非唯一索引**（运行时 `SHOW CREATE TABLE` 实测，见 §2.3 实测基线）。
> 本次**不新增任何唯一约束**，也**不得顺手删除该索引**——删了它秒传查重就退化为全表扫描。

#### aik_sys_attachment（通用附件挂载表 · **第 2 层：挂载点**）

> **表语义已变更**：由"知识附件表"改造为**通用挂载表**。
> 语义：**一行 = 某业务（`biz_type`, `biz_id`）以某名义（`attach_name`）用某文件（`file_id`）**。
> 归属 system 模块（物理归属论证见 [§2.5.8](#258-物理归属与文档同步)）；**卸载时保留该行**（语义见 §2.5.7 第 6 条，
> 注意：保留的是**元数据与当前挂载状态**，**不是**挂载/卸载事件历史）。
> ⚠️ 运行时该表 COMMENT 实测仍是 `'知识附件表'`，改造时应改为 `'通用附件挂载表'`。

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|---------|--------|------|
| id | BIGINT | 20 | 否 | - | 主键，Snowflake ID |
| file_id | BIGINT | 20 | **否** | - | 被挂载的文件对象 ID，逻辑指向 `aik_sys_file.id`。挂载必须有文件，故 `NOT NULL` |
| biz_type | VARCHAR | 32 | 否 | - | 业务类型，取值登记见 §2.5.5（当前仅 `knowledge`） |
| biz_id | BIGINT | 20 | 否 | - | 业务主键；`biz_type = knowledge` 时指向 `aik_knowledge.id` |
| attach_name | VARCHAR | 256 | 否 | - | **用户可见文件名的唯一权威**；不得回退取 `aik_sys_file.original_name`（§2.5.2） |
| description | VARCHAR | 512 | 是 | - | 描述 |
| sort_order | INT | 11 | 否 | 0 | 排序号（同一挂载域内排序） |
| del_flag | TINYINT | 1 | 否 | 0 | 卸载标记：0-有效挂载，1-已卸载（卸载保行以保留元数据，便于重新挂载时复活，§2.5.7） |
| create_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 创建时间（首次挂载时间） |
| modify_time | DATETIME | - | 否 | CURRENT_TIMESTAMP | 修改时间 |
| create_by | VARCHAR | 64 | 是 | - | 创建人 |
| modify_by | VARCHAR | 64 | 是 | - | 修改人 |

**已删除的列**：`knowledge_id`（被 `biz_type` + `biz_id` 取代）、`attach_url`（真冗余——
字节定位由 `file_id` → `aik_sys_file.file_path` 承担，见 §2.5.2）。
**新增的列**：`file_id`、`biz_type`、`del_flag`。

**索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id),   -- 同一业务对同一文件最多一行
KEY idx_biz (biz_type, biz_id, del_flag, sort_order), -- 挂载域列表查询
KEY idx_file_id (file_id)                             -- 删除路径反查引用计数（§2.5.7）
```

**删除**：`idx_knowledge_id`（列已不存在）。

> ⚠️ **`uk_biz_file` 与逻辑删除存在张力，写入必须遵守 §2.5.7 的 upsert/复活契约**：
> 该唯一键**不含 `del_flag`**，一行的卸载墓碑（`del_flag=1`）仍占用键值。
> 若写入用裸 `insert`，"同一业务重新挂载同一文件"会永久撞唯一键失败。
> 正确做法：查（含已卸载行）→ 命中则复活（`UPDATE del_flag=0`）→ 未命中才 `insert`。

### 2.3 建表 SQL

> 与 `sql/aik_system_tables.sql`、`sql/aik_knowledge_tables.sql` 保持一致。
> 可执行脚本以上述文件为准，本节仅作文档内对照。
>
> ✅ **附件域迁移已落地（2026-09-20）**：`aik_sys_attachment` 现位于 `sql/aik_system_tables.sql`（§2.5.8），
> 本节 DDL 与已落地的库结构一致；迁移前基线见 §2.3.0，迁移后实测状态见 §2.3.4。

#### 2.3.0 迁移前实测基线（2026-09-20，历史留档）

> ⚠️ **本节是【迁移前】快照，不是现状。** 附件域 DDL 已于 2026-09-20 对真库执行完毕
> （脚本 `sql/attachment_two_layer_migration.sql`）。**现行结构与本节相反：`url` / `del_flag` /
> `attach_url` / `knowledge_id` 均已不存在**，现行结构见 [§2.3.4](#234-迁移后实测状态2026-09-20已落地)。
> 保留本节的理由：它是"旧文档错在哪、本轮改了什么"的证据链，供溯源与回滚对照。

本节（迁移前）以**运行时库的实测结构**为基线，而非历史文档的转述。
采集方式：直连 `mysql -h172.29.48.1 -uroot -p aik_steins_grimoire` 执行 `SHOW CREATE TABLE`。
下表"实测值"一列描述**迁移前**结构，"处置"一列是当时计划的变更（除注明者外均已执行）。

| 事实 | 实测值 | 旧文档／旧计划的写法 | 处置 |
|---|---|---|---|
| `aik_sys_file.md5` 索引 | `KEY idx_md5 (md5)` —— **非唯一** | 曾写作 `uk_md5` UNIQUE | **旧描述有误**：该唯一约束**从不存在**。本版按实测修正，并保持非唯一（§2.5.4 甲方案） |
| 仓库 `sql/` vs 运行时索引 | 两边都是非唯一 `idx_md5` | 曾描述为"仓库 `idx_md5` / 运行时 `uk_md5` 漂移" | **无漂移**，两边一致；该"漂移"论断作废 |
| `aik_sys_file.url` | 迁移前：存在，可空 | 存在 | 本轮 `DROP COLUMN`（**已执行**，§2.5.6） |
| `aik_sys_file.del_flag` | 迁移前：存在，`NOT NULL DEFAULT 0` | 存在 | 本轮随 `idx_del_flag` 一并 `DROP`（**已执行**，§2.5.3） |
| `aik_sys_file.original_name` | `VARCHAR(255)` | 计划稿曾写 256 | 定稿用 **255**（实测值） |
| `aik_sys_file.stored_name` 注释 | `'存储文件名（UUID）'` | 文档写"Snowflake ID" | 列名语义以实测注释为准，注释本次顺带对齐为"Snowflake ID" |
| `aik_sys_attachment.attach_url` | 迁移前：`VARCHAR(512) **NOT NULL**` | 曾描述为"可空" | 本轮 `DROP COLUMN`（**已执行**）；其职责由 `file_id`（**`NOT NULL`**）承接 |
| `aik_sys_attachment.attach_name` | `VARCHAR(256) NOT NULL` | 一致 | 保留 `NOT NULL`（必填，§2.5.2） |
| `aik_sys_attachment.del_flag` | **迁移前不存在** | — | 本轮新增 `TINYINT NOT NULL DEFAULT 0`（**已执行**） |
| `aik_sys_attachment` COMMENT | `'知识附件表'` | — | 已改为 `'通用附件挂载表'`（**已执行**） |
| `aik_sys_attachment` 索引 | 仅 `idx_knowledge_id` | — | 已删除该索引；已新增 `uk_biz_file` / `idx_biz` / `idx_file_id`（**已执行**） |
| 两表行数 | 均为 **0** | — | 删列／改结构**无数据损失**（迁移已在 0 行状态执行完毕，迁移后两表仍 0 行） |

> **代码侧同步实测**（供 §4 流程与下游任务引用）：
> `FileServiceImpl:61` 算 md5 → `:64` **无条件下盘** → `:79` **无条件 insert**，三步之间**零查重**。
> 因 `idx_md5` 非唯一，重复上传同内容**不会报错**，而是静默写入第二份磁盘文件 + 第二行记录
> （**静默重复存储**，不是崩溃——"必崩 + 孤儿文件"的旧论断同样作废）。
> `FileMapper` 仅有 `BaseMapper`，**零自定义方法**（查重选型见 §2.5.4）。

```sql
-- 字典类型表
CREATE TABLE aik_sys_dict_type (
    id BIGINT NOT NULL COMMENT '主键',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    description VARCHAR(512) COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_code (dict_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 字典项表
CREATE TABLE aik_sys_dict_item (
    id BIGINT NOT NULL COMMENT '主键',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典类型编码',
    item_code VARCHAR(64) NOT NULL COMMENT '字典项编码',
    item_name VARCHAR(128) NOT NULL COMMENT '字典项名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    remark VARCHAR(512) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_item (dict_code, item_code),
    KEY idx_dict_code (dict_code),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- 系统参数表
CREATE TABLE aik_sys_param (
    id BIGINT NOT NULL COMMENT '主键',
    param_key VARCHAR(128) NOT NULL COMMENT '参数键',
    param_value VARCHAR(2048) NOT NULL COMMENT '参数值',
    description VARCHAR(512) COMMENT '描述',
    param_group VARCHAR(64) COMMENT '参数分组',
    editable TINYINT NOT NULL DEFAULT 1 COMMENT '是否可编辑：1-是，0-否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_param_key (param_key),
    KEY idx_param_group (param_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- 文件记录表（第 1 层：文件对象。物理删除，无 del_flag；md5 内容寻址）
CREATE TABLE IF NOT EXISTS aik_sys_file (
    id BIGINT NOT NULL COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名（首次上传者）',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名（Snowflake ID）',
    file_path VARCHAR(512) NOT NULL COMMENT '相对存储路径（存储层内部标识，不对外暴露）',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(128) COMMENT 'MIME类型',
    storage_type VARCHAR(16) NOT NULL DEFAULT 'local' COMMENT '存储类型：local/oss/sftp',
    md5 VARCHAR(32) COMMENT '文件MD5哈希（内容寻址锚点）',
    download_count INT NOT NULL DEFAULT 0 COMMENT '下载次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_original_name (original_name),
    KEY idx_create_time (create_time),
    KEY idx_storage_type (storage_type),
    KEY idx_md5 (md5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

-- 通用附件挂载表（第 2 层：挂载点。卸载保行以保留元数据；复活契约见 SDD §2.5.7）
-- 位置：sql/aik_system_tables.sql（迁移已于 2026-09-20 落地，见 §2.5.8 与 §2.3.4）
CREATE TABLE IF NOT EXISTS aik_sys_attachment (
    id BIGINT NOT NULL COMMENT '主键',
    file_id BIGINT NOT NULL COMMENT '被挂载的文件对象ID，逻辑关联 aik_sys_file.id',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：knowledge（取值登记见 SDD §2.5.5）',
    biz_id BIGINT NOT NULL COMMENT '业务主键：biz_type=knowledge 时为 aik_knowledge.id',
    attach_name VARCHAR(256) NOT NULL COMMENT '用户可见文件名（权威）',
    description VARCHAR(512) COMMENT '描述',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '卸载标记：0-有效挂载，1-已卸载',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_by VARCHAR(64) COMMENT '修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id),
    KEY idx_biz (biz_type, biz_id, del_flag, sort_order),
    KEY idx_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用附件挂载表';
```

#### 2.3.1 迁移 DDL（迁移前实测现状 → 目标结构）

> ✅ **状态：已于 2026-09-20 对真库 `aik_steins_grimoire` 执行完毕**（执行时两表 0 行，无数据损失；
> 迁移前后两表均为 0 行）。
> **可执行脚本的权威位置是 `sql/attachment_two_layer_migration.sql`**（幂等、可重放，含
> `information_schema` 预检、两道硬断言与兜底分支）。本节内嵌 SQL 仅为**文档内对照**，
> **不得**作为执行依据；两者不一致时以 `sql/` 下的脚本为准。迁移后实测结构见 §2.3.4。
>
> 前置：两表实测 **0 行**（§2.3.0 迁移前基线，见 §2.3.3 硬断言）；仍属**破坏性 DDL**，重放前需备份。
> 本脚本按 0 行状态编写；若迁移前已有数据，`NOT NULL` 列的添加必须改为
> "先加可空列 → 回填 → 再置 `NOT NULL`"，并在回填时按 `biz_type='knowledge'`、`biz_id=knowledge_id` 转换。
> 幂等性由执行方用 `information_schema` 预检保证（MySQL 8 不支持 `DROP COLUMN IF EXISTS`）。

```sql
-- ⚠️ 本脚本【仅适用于 0 行表】。执行前必须通过 §2.3.3 的硬断言 ①（两表行数均为 0），否则中止。
-- MySQL 8 不支持 DROP COLUMN IF EXISTS；幂等性由 §2.3.3 的 information_schema 预检保证，
-- 【不要】试图用"再删一次索引"之类的手法伪造幂等（见下方 idx_del_flag 说明）。

-- ① 文件对象层：删 url、删 del_flag
--    注意：MySQL 删除列时会【自动连带删除只依赖该列的索引】，因此不要显式写 DROP INDEX idx_del_flag。
--    显式写会在重跑时直接报 ERROR 1091 (Can't DROP 'idx_del_flag'; check that column/key exists)。
ALTER TABLE aik_sys_file
    DROP COLUMN url,
    DROP COLUMN del_flag,          -- idx_del_flag 随列自动消失，无需（也不得）显式 DROP INDEX
    MODIFY COLUMN original_name VARCHAR(255) NOT NULL COMMENT '原始文件名（首次上传者）',
    MODIFY COLUMN stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名（Snowflake ID）',
    MODIFY COLUMN file_path VARCHAR(512) NOT NULL COMMENT '相对存储路径（存储层内部标识，不对外暴露）',
    MODIFY COLUMN md5 VARCHAR(32) COMMENT '文件MD5哈希（内容寻址锚点）';
-- 保留 idx_md5（非唯一，NON_UNIQUE=1）：秒传查重依赖它，不得删除

-- ② 挂载点层：删 attach_url、knowledge_id 更名 biz_id、新增 file_id / biz_type / del_flag
--    子句顺序有两处硬约束：
--      (i)  CHANGE COLUMN 必须【在 DROP INDEX idx_knowledge_id 之前】执行改名。
--          该索引建在 knowledge_id 上；CHANGE COLUMN 保留索引（索引名不变，指向新列 biz_id），
--          索引不会随列改名自动消失，必须显式删除。放在 CHANGE 之后可让"名字→列"的对应关系始终成立。
--      (ii) 新增的 file_id / biz_type 是 NOT NULL 且无默认值 —— 仅在 0 行表上可行（本脚本前提，
--          由 §2.3.3 硬断言 ① 保证）。对非空表必须先加可空列 → 回填 → 再置 NOT NULL，本脚本不覆盖该场景。
ALTER TABLE aik_sys_attachment
    DROP COLUMN attach_url,
    CHANGE COLUMN knowledge_id biz_id BIGINT NOT NULL COMMENT '业务主键：biz_type=knowledge 时为 aik_knowledge.id',
    DROP INDEX idx_knowledge_id,
    ADD COLUMN file_id BIGINT NOT NULL COMMENT '被挂载的文件对象ID，逻辑关联 aik_sys_file.id' AFTER id,
    ADD COLUMN biz_type VARCHAR(32) NOT NULL COMMENT '业务类型：knowledge（取值登记见 SDD §2.5.5）' AFTER file_id,
    ADD COLUMN del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '卸载标记：0-有效挂载，1-已卸载' AFTER sort_order,
    MODIFY COLUMN attach_name VARCHAR(256) NOT NULL COMMENT '用户可见文件名（权威）',
    COMMENT='通用附件挂载表',
    ADD UNIQUE KEY uk_biz_file (biz_type, biz_id, file_id),
    ADD KEY idx_biz (biz_type, biz_id, del_flag, sort_order),
    ADD KEY idx_file_id (file_id);
```

> **COMMENT 等价性**：本迁移脚本已对 `original_name` / `stored_name` / `file_path` / `md5`（文件表）
> 与 `biz_id` / `attach_name` / `biz_type`（挂载表）做 `MODIFY`，使迁移后的 `SHOW CREATE TABLE`
> 与 §2.3 的全新安装 `CREATE TABLE` **逐字段 COMMENT 等价**；其余列的注释与实测一致，无需变更。
> 验收时（t9）应以 `SHOW CREATE TABLE` 逐行比对两种安装路径，不得依赖"结构相同"的目测结论。

> **决策记录（本节的显式选择）**：对 `NOT NULL` 无默认值列采**(a) 保持 NOT NULL + 前置硬断言**，
> 不写三段式（加可空列 → 回填 → 置 NOT NULL）。理由：实测两表确认 **0 行**，
> 三段式会让脚本复杂度翻倍却只服务于一个不存在的场景；一旦 §2.3.3 硬断言失败即中止迁移，
> 不存在"静默按错前提执行"的路径。

**兜底**：两表 0 行时，直接 `DROP TABLE` + 重建（即上方 §2.3 的 `CREATE TABLE` 语句）等价且更省事；
迁移脚本存在的意义是"万一有数据"时的等价路径。

#### 2.3.2 对账 SQL（运维定期执行）

`md5` 去重是**应用层约定，不是数据库约束**（§2.5.4），并发窗口下可能落多行同 `md5`；
删除路径也依赖"无有效挂载才删文件"。以下三条查询是唯一能把这两类问题**检出**的手段：

```sql
-- ① 内容重复行（同一 md5 落多行；并发秒传窗口的典型后果）
SELECT md5,
       COUNT(*)                     AS file_rows,
       GROUP_CONCAT(id ORDER BY id) AS file_ids
FROM aik_sys_file
WHERE md5 IS NOT NULL
GROUP BY md5
HAVING COUNT(*) > 1;

-- ② 悬空挂载（挂载行指向已不存在的文件记录 → 下载 404）
SELECT a.id AS attachment_id, a.biz_type, a.biz_id, a.file_id
FROM aik_sys_attachment a
LEFT JOIN aik_sys_file f ON f.id = a.file_id
WHERE a.del_flag = 0
  AND f.id IS NULL;

-- ③ 无引用文件（文件记录存在但已无任何有效挂载 → 文件层墓碑 + 磁盘泄漏）
SELECT f.id AS file_id, f.file_path, f.create_time
FROM aik_sys_file f
LEFT JOIN aik_sys_attachment a ON a.file_id = f.id AND a.del_flag = 0
WHERE a.id IS NULL;
```

> ③ 的期望值：**仅在"删盘失败"时非空**（提交后删盘失败只留孤儿磁盘文件，见 §2.5.7）。
> 若 ③ 长期非空，说明删除路径未按契约实现。

#### 2.3.3 迁移前置校验（重放前必跑）

> ✅ 迁移已于 2026-09-20 执行完毕；本节校验脚本保留作**重放 / 幂等性核查**之用：
> 重放时 ① 仍应满足（改造后两表仍为 0 行），②③ 用于确认当前结构已处于"迁移后"状态。

```sql
-- ① 【硬断言】两表必须为空（迁移前实测 2026-09-20 均为 0；迁移后仍为 0）。
--    任一结果 > 0 → 立即中止迁移：§2.3.1 的 ADD COLUMN ... NOT NULL（无默认值）在非空表上不可执行。
SELECT COUNT(*) AS file_rows       FROM aik_sys_file;         -- 断言：= 0
SELECT COUNT(*) AS attachment_rows FROM aik_sys_attachment;   -- 断言：= 0

-- ② 确认列现状（url / del_flag 是否已不存在 → 判断迁移是否已执行或部分执行）
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('aik_sys_file', 'aik_sys_attachment')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- ③ 确认索引现状（idx_md5 必须存在且 NON_UNIQUE=1；uk_biz_file 迁移后才出现）
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('aik_sys_file', 'aik_sys_attachment')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE;
```

#### 2.3.4 迁移后实测状态（2026-09-20，已落地）

本节是附件域的**现行结构**（真库 `SHOW CREATE TABLE` / `information_schema` 实测，迁移后采集），
即"代码与文档无漂移"验收在 SQL 侧的基准。

**`aik_sys_file`（13 列，`TABLE_COMMENT='文件记录表'`）**

| 事实 | 实测值 |
|---|---|
| 列（按序） | `id`, `original_name`, `stored_name`, `file_path`, `file_size`, `file_type`, `storage_type`, `md5`, `download_count`, `create_time`, `modify_time`, `create_by`, `modify_by` |
| 已不存在的列 | `url`（本轮删除）、`del_flag`（本轮删除，`idx_del_flag` 随之消失） |
| 索引 | `PRIMARY`, `idx_original_name`, `idx_create_time`, `idx_storage_type`, **`idx_md5`（`NON_UNIQUE=1`，非唯一）** |
| 唯一约束 | **无**（§2.5.4 甲方案：不为 `md5` 新增任何唯一约束） |
| 行数 | 0 |

**`aik_sys_attachment`（12 列，`TABLE_COMMENT='通用附件挂载表'`）**

| 事实 | 实测值 |
|---|---|
| 列（按序） | `id`, `file_id`, `biz_type`, `biz_id`, `attach_name`, `description`, `sort_order`, `del_flag`, `create_time`, `modify_time`, `create_by`, `modify_by` |
| 已不存在的列 | `attach_url`（本轮删除）、`knowledge_id`（本轮改名为 `biz_id`） |
| 索引 | `PRIMARY`, **`UNIQUE uk_biz_file (biz_type, biz_id, file_id)`**, `idx_biz (biz_type, biz_id, del_flag, sort_order)`, `idx_file_id (file_id)`；`idx_knowledge_id` 已删除 |
| 行数 | 0 |

**未受影响的相邻数据**：`aik_knowledge` 仍 20 行、`aik_knowledge_category` 仍 5 行。

**两条安装路径的等价性（交叉验证）**：真库"迁移路径"与 scratch 库按更新后的
`sql/aik_system_tables.sql` **全新安装**，`SHOW CREATE TABLE` 归一化后逐字段等价（含列序 / 索引 / `COMMENT`）。
证据留档于 `sql/backup/`：

| 文件 | 内容 |
|---|---|
| `sql/backup/migration_vs_fresh_install_2026-09-20.txt` | 迁移路径 vs 全新安装路径的结构 diff（为空） |
| `sql/backup/migration_run{1,2,3}_2026-09-20.log` | 三次重放日志（幂等性证据） |
| `sql/backup/attachment_pre_migration_2026-09-20.sql` | 回滚凭据（迁移前结构备份，`mysqldump --no-data`） |

> **漂移处置**：若真库或 `sql/` 与本表不符，以 `sql/` 与真库实测为准并**回报船长**；
> **不得**按本节反推去改 `sql/`（`sql/` 的改动属 DDL 落地任务的边界）。

### 2.4 MyBatis-Plus 实体类

#### DictTypePo

```java
package io.aik.steins.grimoire.system.dict.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 字典类型 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("aik_sys_dict_type")
public class DictTypePo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String dictCode;

    private String dictName;

    private String description;

    private Integer status;
}
```

#### DictItemPo

```java
package io.aik.steins.grimoire.system.dict.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 字典项 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("aik_sys_dict_item")
public class DictItemPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String dictCode;

    private String itemCode;

    private String itemName;

    private Integer sortOrder;

    private Integer status;

    private String remark;
}
```

#### SystemParamPo

```java
package io.aik.steins.grimoire.system.param.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 系统参数 -anchor
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("aik_sys_param")
public class SystemParamPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String paramKey;

    private String paramValue;

    private String description;

    private String paramGroup;

    private Integer editable;
}
```

#### FileRecordPo（第 1 层：文件对象 · `aik_sys_file`）

```java
package io.aik.steins.grimoire.system.file.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * -anchor 文件记录
 *
 * <p>文件对象层：一行 = 磁盘上的一份字节。md5 内容寻址，物理删除（无 del_flag）</p>
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件记录")
@TableName("aik_sys_file")
public class FileRecordPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    // 注：Lombok @SuperBuilder 需要显式无参构造，删除字段时不要误删
    public FileRecordPo() {
        super();
    }

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @Schema(description = "原始文件名（首次上传者）")
    @TableField("original_name")
    private String originalName;

    @Schema(description = "存储文件名")
    @TableField("stored_name")
    private String storedName;

    @Schema(description = "文件路径")
    @TableField("file_path")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    @TableField("file_size")
    private Long fileSize;

    @Schema(description = "文件类型")
    @TableField("file_type")
    private String fileType;

    @Schema(description = "存储类型")
    @TableField("storage_type")
    private String storageType;

    @Schema(description = "文件MD5")
    @TableField("md5")
    private String md5;

    @Schema(description = "下载次数")
    @TableField("download_count")
    private Integer downloadCount;

    // 已删除字段（本次改造）：
    //   url      ← @TableField("url")                      访问地址不可持久化（§2.5.6）
    //   delFlag  ← @TableField("del_flag") + @TableLogic   本表改物理删除（§2.5.3）
}
```

**本次改造的四处要点**（对应实测行号）：

| # | 位置 | 处置 |
|---|---|---|
| 1 | `:33` `extends BaseEntity` | **保持**，不得改为 `BaseLogicEntity`（§2.4.1） |
| 2 | `:100-102` `url` 字段 | 删除 |
| 3 | `:104-110` `delFlag` + `@TableField("del_flag")` + `@TableLogic` | 删除（物理删除，§2.5.3） |
| 4 | `:37-39` 显式无参构造 | **保留**（Lombok `@SuperBuilder` 需要） |

> 摘除 `@TableLogic` 后，`FileServiceImpl:145` 的 `deleteById` 变为物理删除，
> 同处 `:144` 的注释「逻辑删除（@TableLogic 自动处理）」**同步消灭**。

#### SysAttachmentPo（第 2 层：挂载点 · `aik_sys_attachment`）

```java
package io.aik.steins.grimoire.system.attachment.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.aik.steins.grimoire.core.po.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * -anchor 通用附件挂载
 *
 * <p>挂载点层：一行 = 某业务（bizType, bizId）以某名义（attachName）用某文件（fileId）。
 * 卸载时保留该行以保留元数据（便于重新挂载时复活），不提供事件历史；
 * 有效挂载判定只统计 del_flag = 0</p>
 *
 * @author a I k .
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通用附件挂载")
@TableName("aik_sys_attachment")
public class SysAttachmentPo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    // 注：Lombok @SuperBuilder 需要显式无参构造，删除字段时不要误删
    public SysAttachmentPo() {
        super();
    }

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @Schema(description = "被挂载的文件对象ID")
    @TableField("file_id")
    private Long fileId;

    @Schema(description = "业务类型：knowledge")
    @TableField("biz_type")
    private String bizType;

    @Schema(description = "业务主键")
    @TableField("biz_id")
    private Long bizId;

    @Schema(description = "用户可见文件名（权威）")
    @TableField("attach_name")
    private String attachName;

    @Schema(description = "描述")
    @TableField("description")
    private String description;

    @Schema(description = "排序号")
    @TableField("sort_order")
    private Integer sortOrder;

    @Schema(description = "卸载标记：0-有效挂载，1-已卸载")
    @TableField("del_flag")
    @TableLogic
    private Integer delFlag;
}
```

**本次改造要点**（对应实测行号）：`:52-54` `knowledgeId` → `fileId` + `bizType` + `bizId`；
`:66-68` `attachUrl` 删除；新增 `delFlag`（置于 `:80-82` `sortOrder` 之后）；
`:18` javadoc「知识条目的附属文件」改为通用挂载语义；`:38-40` 显式无参构造保留。

#### 2.4.1 实体继承与逻辑删除的强制约定

- 全部实体继承 `core/po/BaseEntity`。
- **禁止**改用 `core/po/BaseLogicEntity`。它的逻辑删除列名是 `deleted`
  （`@TableField(value = "deleted")`），而本次新增的列是 `del_flag`；实测
  `grep -rn "extends BaseLogicEntity" src/main/java/` → **0 结果**，即它是**从未被使用的死基础设施**。
  若 `SysAttachmentPo` 改为继承它，MyBatis-Plus 会去找不存在的 `aik_sys_attachment.deleted` 列，
  **应用启动即 SQL 报错**。
- 需要逻辑删除的实体在**自己类内**显式声明（本仓库唯一先例 = 改造前的 `FileRecordPo:104-110`）：

  ```java
  @TableField("del_flag")
  @TableLogic
  private Integer delFlag;
  ```

- 改造后**唯一**带 `@TableLogic` 的实体是 `SysAttachmentPo`；`FileRecordPo` 摘除（§2.5.3）。
- 是否删除 `BaseLogicEntity` 自身：属独立议题，登记为 §6.2 **W05**，**本次不删**（超出附件改造边界）。

---

## 2.5 附件两层模型与归属边界（附件域唯一权威）

> 本节是附件与文件域的**唯一设计权威**。凡涉及"文件 / 挂载 / 附件归属 / 删除 / 秒传 / 文件名"的事实以本节为准；
> `api-contract.md` 只拥有 HTTP 形状，**不得**与本节的设计约束冲突。

### 2.5.1 两层模型

| | 第 1 层：文件对象 | 第 2 层：挂载点 |
|---|---|---|
| 表 | `aik_sys_file` | `aik_sys_attachment` |
| 一行代表 | **磁盘上的一份字节** | **某业务以某名义用某文件** |
| 标识 | 内容（`md5`）+ 存储位置（`file_path`） | 关系（`biz_type`, `biz_id`, `file_id`） |
| 生命周期 | 字节存在即存在 | 业务挂载 / 卸载 |
| 删除方式 | **物理删除**（无 `del_flag`） | **卸载保行**（`del_flag = 0/1`） |
| 可否重建 | **可**（`exists(file_path)` 可验证磁盘事实）→ **派生台账** | **不可**（挂载关系与显示元数据无第二来源）→ **业务事实** |
| 权威内容 | `md5`、`file_size`、`file_type`、`file_path`、`storage_type` | `attach_name`、`description`、`sort_order`、归属关系 |
| 数量关系 | 1 | N（一份文件可被 N 个业务复用） |

**术语：墓碑（tombstone）** —— 本文件中的"墓碑"专指 **`aik_sys_file` 中被逻辑删除、因而永久占用
存储台账位置的行**。本次改造通过**物理删除**消灭文件层墓碑。
**挂载层的 `del_flag = 1` 行不属墓碑**：它是显式的卸载记录（语义见 §2.5.7 第 6 条），
且复活契约保证每个 `(biz_type, biz_id, file_id)` 三元组**永远只有一行**，
不占用任何唯一键的"有效"位置。验收时（t9）不得把挂载层的卸载行判为墓碑违规。

**为什么必须是两层**：秒传的语义是"md5 一致则把已有 `file_id` 绑定到另一个业务"——
这句话里有两个动作：创建**文件记录**（一份字节）与创建**绑定关系**（谁在用）。
一行数据无法同时"属于 A 业务"又"属于 B 业务"，除非这一行同时知道 A 和 B——那就退回"一行一个业务"，
**md5 复用随即失去意义**。因此：**合并两表 = 杀死秒传**。

### 2.5.2 `attach_name` 的权威职责

- `attach_name` = **用户可见文件名**，权威唯一归属**挂载层**。
- `aik_sys_file.original_name` 的语义**降级**为"首次上传者的文件名"，仅供管理视图 / 台账使用；
  **任何**附件响应或下载响应文件名的来源**不得**是它。
- **必须删除的旧契约**（`api-contract.md` §2.5.4 现状）：
  > `attachName` 缺省时由后端用 `FileVo.originalName` 回填。

  **删除理由**：md5 命中复用后，文件层只保留**第一个**上传者的名字。A 上传 `我的笔记.pdf`（md5 = X）
  产生 `file_id = 1`；B 上传 `会议记录.pdf`（内容相同，md5 = X）复用 `file_id = 1` —— 若回填，
  **B 会看到 A 的文件名**。md5 只证明"字节相同"，不证明"语义相同"（同名不同内容、同内容不同名都是常态）。
  该回填会把**跨用户的错误文件名**固化进契约。
- 结论：`attach_name` 为 `NOT NULL` 必填，上传 → 挂载链路必须由调用方**显式提供**；
  下载响应的 `Content-Disposition` 文件名取自**挂载行**（§2.5.6）。
- 边界澄清：`POST /grimoire/file/rename` 改的是**文件台账**的 `original_name`（管理视图）；
  用户可见名的改名落在**挂载行** `attach_name`。两者**不得互相回填**。

### 2.5.3 `aik_sys_file` 的删除语义（物理删除）

- 决策：`aik_sys_file` **物理删除**，摘除 `del_flag` 列与 `@TableLogic`，并删除 `idx_del_flag`。
- 理由（**语义正当性**，不是"修复阻塞"）：
  1. 该表登记的是"磁盘上现有哪些字节"，是可由 `exists(file_path)` 重建的**派生台账**；
     业务审计事实（谁何时以何名义挂了哪份文件）归属挂载层。
  2. 保留 `del_flag` 会让"删除"出现**两个所有者**（挂载行 + 文件行）——这正是本次改造要消灭的重复归属。
  3. 消除文件层墓碑行，使 md5 查重总能回到同一行（查重只按 `md5`，见 §2.5.4）。
- ⚠️ **明确作废的错误论断**：本次改造**不是**为了"释放 md5 唯一键"。实测 `idx_md5` 是**普通索引**，
  唯一约束**从不存在**（§2.3.0），软删行从未阻塞任何唯一键。
  旧计划中"物理删除以释放 `uk_md5`"的表述**已作废，不得再引用**。
- 连带改造项：`FileServiceImpl:145` 的 `deleteById` 由逻辑删除变**物理删除**，
  同处 `:144` 的注释 `//anchor 逻辑删除（@TableLogic 自动处理）` 随之**过时**，
  改造时必须同步修改或删除（反熵：不留过时注释）。
- 实体约定：`FileRecordPo` 保持 `extends BaseEntity`，**不要**为软删改继承 `BaseLogicEntity`（§2.4.1）。

### 2.5.4 md5 秒传：应用层约定，非数据库约束

> **md5 去重是【应用层约定，非数据库约束】。**
> `aik_sys_file` 保持 `KEY idx_md5 (md5)` 普通索引，**不新增任何唯一约束**（甲方案）。

- **为什么不加 `UNIQUE uk_md5`（乙方案）**：唯一键要成立，就必须处理并发的 `DuplicateKeyException` 补偿——
  落盘成功、插入失败时回头删掉刚落盘的文件；而**删盘与插入不在同一事务**，补偿本身失败就留下孤儿文件，
  恰好违背本次"不产生孤儿文件"的验收目标。甲方案下第二次上传在查重阶段即命中并**直接跳过写盘**，
  孤儿无从产生。
- **已知代价（必须显式接受）**：极端并发（两请求同时通过查重、都落盘）可能落两行同 `md5`。
  对附件域可接受：两行指向内容完全相同的字节，**无数据损坏**，代价是存储冗余 + 两条文件记录。
  可对账检出（§2.3.2 查询①）。
- **实现选型（决定）**：`FileMapper` 现仅有 `BaseMapper`、零自定义方法。选择**在 Mapper 增加只读查询方法**：

  ```java
  @Select("SELECT * FROM aik_sys_file WHERE md5 = #{md5} ORDER BY id ASC LIMIT 1")
  FileRecordPo selectLatestByMd5(@Param("md5") String md5);
  ```

  理由：① §1.3 约定"简单查询用注解"；② 非唯一索引下多行是常态，`selectOne` 会抛
  `TooManyResultsException`，必须把 `ORDER BY id ASC LIMIT 1` 钉在 SQL 里（取最早那行作规范行）；
  ③ 查重路径与删除路径共用同一查询；④ 显式方法比内联 `LambdaQueryWrapper` 更可审计。
  **不采用** Service 内联 Wrapper：需要额外约定排序与截断，容易在后续改动中丢掉 `LIMIT`。

### 2.5.5 `biz_type` 取值枚举与常量

- 列定义：`biz_type VARCHAR(32) NOT NULL`。取值登记表（**下表即权威**）：

| `biz_type` | 业务域 | `biz_id` 指向 | 状态 |
|---|---|---|---|
| `knowledge` | knowledge 模块的知识条目 | `aik_knowledge.id` | 生效（当前唯一取值） |

- 常量类（新增）`io.aik.steins.grimoire.system.attachment.constant.AttachmentBizType`：

  ```java
  public final class AttachmentBizType {

      private AttachmentBizType() {
      }

      /** 知识条目 */
      public static final String KNOWLEDGE = "knowledge";
  }
  ```

- **选型理由：不用 Java `enum`。** `biz_type` 是**开放取值**——未来新增业务域挂载不应要求修改
  附件模块代码并重新编译；DDL 本身就是 `VARCHAR(32)`。写入时校验白名单（当前仅 `knowledge`），
  未知值抛 `BusinessException`（§3.3）。若取值增多，再把登记表下沉为字典
  （`aik_sys_dict_type` / `aik_sys_dict_item`），届时以字典为准。
- 常量类**不放进** `SystemConstant`：后者是字典 / 参数域的常量容器，混装会让职责再次交叉。

### 2.5.6 对外访问与下载文件名

> **行区间约定（全文适用）**：本文档引用的行区间**含方法上的 `@Override` 注解行**（如有）。
> 例如 `LocalFileStorage:82-85` 指 `@Override`(:82) + 方法声明(:83) + 方法体(:84-85)；`AbstractFileStorage:162-167`
> 同理含 `:162` 的 `@Override`。
> 对照：`backend-audit.md` 的三处 `getUrl` 行区间采用"仅声明 + 方法体"约定（如 `83-85`）。
> 两者**各自内部一致、不冲突**，但**不得混用**；归一化属集成收口项，不在本节处置。

**访问唯一入口**：`GET /grimoire/file/download?id={fileId}[&attachId={attachId}]&preview={true|false}`
（`FileController:46-52` 已存在；`attachId` 是方案 1 新增的**条件必填**参数——挂载层必带、台账层可省，
形状权威 = `api-contract.md` §2.5.5）。
`preview = true` → `Content-Disposition: inline`；否则 `attachment`（`AbstractFileStorage:141`）。
契约里只有 `fileId`，**没有任何路径形态**，前端不得自行拼地址。

**删除 `url` 的真实理由**：`FileServiceImpl:65` 在**上传时刻**调用 `getUrl()` 并把结果**持久化**进
`aik_sys_file.url`（`:77`）。三个实现行为不同：

| 实现 | `getUrl` 行为 | 结论 |
|---|---|---|
| `LocalFileStorage:82-85` | `return storedPath` —— 与 `file_path` 同值 | 死角色（同一问题的另一种表现） |
| `OssFileStorage:96-101` | `generatePresignedUrl(...)`，**1 小时有效期** | 临时凭证，**不可持久化** → `url` 列必须删；方法本身亦随之删除（下方登记） |
| `SftpFileStorage:112-116` | `"sftp://" + host + ":" + port + storedPath` | 拼接地址、不承载凭证；随方法一并删除 |

因此 OSS 模式下 `url` 列存的是一个**1 小时后必然过期的签名 URL**，读出来就是死链。
"删 `url` 列"不是删一个没用的字段，而是**删掉一个语义上无法成立的持久化设计（临时凭证不能当持久字段）**。
`LocalFileStorage` 的同值现象只是同一问题的另一种表现。

**`getUrl()` 接口方法：一并删除（甲方案 — 船长裁决 2026-09-20）**

```
Deletion Class:       unused-code（随 url 链路一并退役，不再是例外）
Target:               core/storage/FileStorageStrategy#getUrl(String) 接口方法及三个实现
                      （LocalFileStorage:82-85、OssFileStorage:96-101、SftpFileStorage:112-116）
Reason（方法本身）:     其唯一消费点是 FileServiceImpl:65 的持久化调用，而该调用随 aik_sys_file.url
                      列一并删除 → 删完后方法【零调用方】，留存即"有定义、有实现、无调用方"的
                      半退休状态，正是本次改造要消灭的熵增形态
Reason（连带设计）:     删 url 列与删 getUrl() 是同一判断的两面：
                      · url 列：OSS 预签名地址是【临时凭证（1 小时过期）】，不可持久化（上表论证不变）
                      · getUrl()：为"取对外地址并落库"而生，持久化被否后其存在理由随之消失
Sign-off:             船长裁决（采甲）。用户拍板「url 全链路删除」的确认对象，正是
                      「DROP COLUMN url + getUrl() 接口方法 + 三个实现 + FileServiceImpl 两处赋值
                      + FileRecordPo.url + FileVo.url」这条完整链
Verified:             全库 grep 实测 getUrl 的唯一调用方是 FileServiceImpl:65（该调用在下方删除清单内）
Reopen Trigger:       需要"由存储层直接给出预签名地址、且【不落库】"的直连下载时，应新增语义明确的
                      按需方法（如 presignedUrl），不得复活持久化字段
```

> **与反熵原则的关系**：v1.2 曾把该方法登记为"有意豁免"，但其 Sign-off 栏原文即"船长裁决"——
> 即该例外**从未被单方面放行**。用户指令与"实测零调用方"两项证据同向，故 v1.3 改为**删除**。

**本次删除的清单**（`url` 列的消费链，全部删干净，不留半退休状态）：

| # | 位置 | 处置 |
|---|---|---|
| 1 | `aik_sys_file.url`（DDL，`sql/aik_system_tables.sql`） | `DROP COLUMN`（**已执行**，§2.3.4） |
| 2 | `aik_sys_attachment.attach_url`（DDL） | `DROP COLUMN`（**已执行**，§2.3.4） |
| 3 | `FileStorageStrategy#getUrl(String)`（接口方法，`FileStorageStrategy.java:91`） | **删除**（甲方案；删后零调用方，见上方登记） |
| 4 | `LocalFileStorage:82-85` / `OssFileStorage:96-101` / `SftpFileStorage:112-116`（三个实现） | **删除**（`OssFileStorage` 中仅供其使用的 `generatePresignedUrl` 调用点与随之失效的未用导入一并清理） |
| 5 | `FileServiceImpl:65`（`getUrl` 调用）与 `:77`（`setUrl`） | 删除 |
| 6 | `FileRecordPo:100-102`（`url` 字段） | 删除 |
| 7 | `FileVo`（`url` 字段） | 删除 |
| 8 | `api-contract.md` §2.5.3（`AttachmentVo.attachUrl`）、§2.5.4、§3 `M1` | 同步修订（契约权威方执行，§2.5.8） |

> 1–4 项构成**完整的 url 链路**（列 → 持久化点 → 取值方法 → 三个实现），删完后**零残留、零半退休**。
> **不在本次删除范围**（不顺手扩大边界）：
> - `FileStorageStrategy#exists(String)`：全库 grep 实测 **0 调用方**，但**不属 url 链路**，本次不删，
>   登记为待退役候选（见本节末「次要登记」）
> - `AbstractFileStorage:162-167`（= 接口方法 `FileStorageStrategy:53` 的 `@Override`，**在接口内**）
>   与 `:172-176`（**不在接口内**）：两者均 **0 调用方**，本次**均不删**，登记为待退役候选；
>   **分类不同**，权威分类见 `backend-audit.md` §10.3

**`FileVo` 的暴露面（决定）**：删除 `url`，并**同时删除 `filePath` 与 `storedName`**。
理由：`FileVo.of()` 是 `BeanUtil.copyProperties(po, vo)` 全量拷贝，只要字段在 VO 上，
服务端相对存储路径（如 `2026/09/20/xxx.pdf`）就会直接返回给前端——
只删 `url` 只完成"不给前端任何访问路径形态"这个目标的一半。
`storageType` **保留**（显式选择：它是 `local` / `oss` / `sftp` 的**策略标签**，不泄露任何路径形态；移除它 Management 页就无法按存储类型筛选）；`md5` **不对外暴露**（内容指纹属内部锚点）。

**两阶段秒传（客户端预检）本次不做，已登记为有意缺口**：

```
Deletion Class:       none（这是【未实现的能力】，不是待删除的对象）
Target:               客户端预检端点（如 POST /grimoire/file/checkMd5）与 FileVo.md5 字段
Decision:             本次仅做【服务端复用】（上传时查重命中即跳过写盘，§2.5.4）；
                      客户端"先传 md5 探测 → 命中则整包不传"不在本次范围
Reason:               不做预检端点就不该暴露 md5 —— 否则它是一个"暴露了但无人使用"的半退休字段
Closed-loop Check:    FileVo 不含 md5；不存在"字段暴露但无消费方"的状态
Reopen Trigger:       需要节省上行带宽时启用；届时新增独立探测端点 + 服务端幂等确认，
                      【仍不】把 md5 加回通用 FileVo
```
若管理页确需看到路径，另开管理专用 VO（如 `FileManageVo`），**不得**混入通用 `FileVo`。
契约最终形状以 `api-contract.md` 为准；本节只确立约束：「**`FileVo` 不得暴露存储布局**」。

**下载文件名契约（决策 4 的落地）**

现状链路：`FileController:46-52`（只有 `id`）→ `FileServiceImpl:98`
`fileStorageStrategy.download(response, po.getFilePath(), po.getOriginalName(), preview)`
—— 文件名来自**文件层**（`po.getOriginalName()`）。md5 复用下 B 用户会拿到 A 的名字，
而现有签名**没有任何入口**能把挂载层的 `attach_name` 传进来。

关键事实：**存储层不需要改**。`FileStorageStrategy:64-65` 与 `AbstractFileStorage:181`
本就把 `originalName` 当参数（按调用方给的名字设置响应头），语义正确。
缺的只是 Controller → Service 这一段没有传 `attachName` 的路径。

裁决（**方案 1**）：

```
GET /grimoire/file/download?id={fileId}&attachId={attachId}&preview={bool}
```

- Service 查 `aik_sys_attachment` 校验：该挂载行存在、`del_flag = 0`、且其 `file_id == id`；
  校验通过后用该行的 `attach_name` 作为响应文件名。
- 附加收益：**杜绝"凭 fileId 下载任意文件"**——只有被有效挂载的文件才可下载。
- `attachId` 是否强制由 `api-contract.md` 定（管理端"文件台账下载"可能不需要挂载）；
  但**凡经挂载层发起的下载必须带 `attachId`**，且响应文件名必须取自该挂载行。

被否方案：

| 方案 | 内容 | 否决理由 |
|---|---|---|
| 2 | `&attachName={name}` 由前端传名 | 文件名由不可信输入决定，且 `AbstractFileStorage:142` 把它直接编码进 `Content-Disposition` 响应头 —— 至少需剥离 CR/LF，否则构成**响应头注入**面 |
| 3 | 下载移到 `/knowledge/{id}/attachment/{attachId}/download` | 语义最准，但把文件模块耦合进业务模块，与"通用挂载层"目标冲突 |

> HTTP 形状的**最终权威仍是 `api-contract.md`**；本节给出设计裁决与理由，契约方须照此固化（§2.5.8）。

**`POST /grimoire/file/remove` 的处置（决定：方案 b）**

现状 `FileController:67-72` 暴露按 `fileId` 直删，`FileServiceImpl:140-146` **不校验任何挂载**：
在新模型下会绕过"是否还有其它业务挂载"的判定 → **误删其它业务正在共享的文件**。

| 方案 | 内容 | 裁决 |
|---|---|---|
| a | 删除该端点，删除一律走挂载层入口 | 过度：文件台账本身仍需要管理端删除入口 |
| **b** | **保留，降级为管理端文件台账入口，且内部强制"无有效挂载才允许删除"校验** | **采用**（即 §2.5.7 的统一删除契约） |
| c | 保留并登记为已知危险端点，本次不修 | 不采用：该风险本次就有能力消除，登记等于明知可用而放任 |

**次要登记（本次不修）** —— 三项待退役候选，均为"零调用方但**不属 url 链路**"，故不随本次删除：

| 候选 | 实测状态 | 本次处置 | Reopen Trigger |
|---|---|---|---|
| `FileStorageStrategy#exists(String)`（`FileStorageStrategy.java:83`）及三实现（`LocalFileStorage:77` / `OssFileStorage:92` / `SftpFileStorage:95`） | 全库 grep **0 调用方**（唯一 `exists` 命中是 `LocalFileStorage:64` 的 `java.io.File#exists`，不是本接口方法） | **不删**，仅登记 | 需要"上传前探测磁盘是否已有该字节"或台账对账（§2.3.2）时启用 |
| `AbstractFileStorage:162-167`（两参 `download`） | **是接口方法 `FileStorageStrategy:53` 的 `@Override`**（在接口内）；0 外部调用方 | **不删**，仅登记（与 `:53` 同属一个对象） | 见 `backend-audit.md` §10.3 |
| `AbstractFileStorage:172-176`（三参 `download`，带 `preview`） | **不在 `FileStorageStrategy` 接口内**；0 调用方 | **不删**，仅登记 | 需要"以存储名为参数"的下载时启用 |

> 共同处置理由：本次删除的边界是**用户点名的 url 链路**（`url` 列 + `attach_url` + `getUrl()` 方法链）。
> 把"零调用方"直接等同于"该删"会顺手扩大改造边界，并让 `exists()` 这类**存储能力探测**方法失去回滚空间。
> 改造中**不得新增对上述三项的依赖**。

### 2.5.7 挂载写入与卸载契约

**写入（挂载）**

1. **加锁校验 `fileId` 存在**：`SELECT * FROM aik_sys_file WHERE id = #{fileId} FOR UPDATE`；
   查不到 → `BusinessException`（悬空挂载禁止产生）。
   ⚠️ **必须是加锁读，不能是无锁的 `selectById`**——理由见本节末尾「并发与加锁顺序」。
2. 校验 `biz_type` 在白名单内（§2.5.5）。
3. **必须 upsert，禁止裸 `insert`**：`uk_biz_file (biz_type, biz_id, file_id)` **不含 `del_flag`**，
   一行卸载行（`del_flag = 1`）仍占用键值。裸 `insert` 会让"同一业务重新挂载同一文件"**永久撞唯一键失败**——
   这是用户可见的功能回归，不是角落情形。正确路径：查（**含已卸载行**）→ 命中则**复活**
   （`UPDATE del_flag = 0`，同时刷新 `attach_name` / `description` / `sort_order` / `modify_time` / `modify_by`）
   → 未命中才 `insert`。
4. 并发撞 `DuplicateKeyException` → 降级为"复活并重试一次"。
5. 业务侧的"整组替换"（`KnowledgeDto.attachments`）必须实现为**差量**：仅卸载"原有但新列表已无"的行，
   仅新增 / 复活"新列表有"的行。**禁止"全删再全插"**——后者在每次编辑时产生卸载行并触发第 3 条的失败路径。
6. **`del_flag` 的真实收益（准确表述，勿再称"保留审计"）**：
   - 标记该挂载点**当前是否有效**（有效挂载判定只统计 `del_flag = 0`）；
   - 卸载时**保留该行**，从而保留 `attach_name` / `description` / `sort_order` 元数据，
     便于**重新挂载时原样恢复**（复活契约，第 3 条）；
   - 保留"该三元组**曾经**被挂载"这一存在性记录。

   ❌ **不成立的主张**：它**不**提供事件历史——不记录"何时被卸载过""卸载了几次"，
   因为复活复用同一行，`create_time` 始终是最初挂载时间。
   若需完整挂载 / 卸载时间线，须另建 **append-only 事件表**，且届时 `uk_biz_file` 需让位于它——
   **本次范围外**，登记为已知缺口（§6.2 **W06**）。

**卸载（删除）——唯一删除入口契约**

```
业务侧（如 KnowledgeServiceImpl.delete）
   └→ 只调用统一挂载服务 / FileService，禁止 inline 直连 SysAttachmentMapper
        └→ 同一事务内：
             ① SELECT ... FROM aik_sys_file WHERE id = ? FOR UPDATE     ← 【先】锁文件行
             ② 标记卸载该挂载行（del_flag = 1）
             ③ SELECT COUNT(*) FROM aik_sys_attachment
                   WHERE file_id = ? AND del_flag = 0                    ← 只数【有效】挂载
             ④ count = 0 → 物理删除 aik_sys_file 行（文件层只有一个删除所有者）
                该行已不存在 → 幂等 no-op，不抛异常
             ⑤ 批量卸载多个文件时：按 file_id **升序**逐个加锁（避免多事务交叉锁序死锁）
        └→ 事务提交后（TransactionSynchronization#afterCommit）
             ⑥ fileStorageStrategy.remove(file_path) 删盘
```

契约要点：

- **`FileService.remove(Long id)` 是文件删除的唯一入口**，内部**先**校验是否还有有效挂载（`del_flag = 0`），
  有则拒绝（`BusinessException`）。`POST /grimoire/file/remove` 只是它的 HTTP 外壳（§2.5.6 方案 b）。
- **事务边界**：删挂载行 + 删文件记录**必须在同一事务**。否则中途失败会留下
  "挂载已删、文件记录还在"（文件层墓碑，§2.3.2 查询③）或反向的错误状态。
- **删盘必须在事务提交之后**：若先删盘再回滚，DB 仍引用已消失的字节 → **悬空引用，不可恢复**；
  反之（提交后删盘失败）只留**孤儿磁盘文件**，可日志 + 对账清理。两者的可恢复性不对称，**顺序不能反**。
- **并发**：两个事务并发卸载同一文件的最后两个挂载点时，②`FOR UPDATE` 使其串行；
  后者拿到锁后重数得 0，尝试物理删行时行已不存在 → 按**幂等 no-op** 处理
  （不得抛异常、不得把"文件不存在"报给用户）。
**并发与加锁顺序（写入路径与删除路径必须对称）**

两条路径的**第一条语句都是"锁定文件行"**（`SELECT ... FOR UPDATE`），因此：

```
T1（卸载 F 的最后引用）                T2（新业务 C 挂载 F）
① SELECT F FOR UPDATE  ← 先拿到锁
② 卸载挂载行 / 计数 = 0
③ 物理删除 F 行
④ COMMIT                              ① SELECT F FOR UPDATE ← 阻塞在 T1 的行锁上
                                       T1 提交后：查不到 F → BusinessException「文件不存在」
                                       ⇒ 【不产生悬空挂载】

反向时序（T2 先拿锁）：T2 插入挂载行并提交 → T1 拿到锁后计数 = 1 → count > 0
                       ⇒ 【不误删 C 仍在使用的文件】
```

- **若写入路径用无锁的 `selectById` 做存在性校验**，它能读到一个"即将被删除"的文件行，
  于是 T2 会成功插入一个指向已被物理删除文件的挂载行 → **悬空挂载**（§2.3.2 查询② 能检出，
  但检出 ≠ 防止，且用户下载立即 404）。**这是本契约必须用加锁读堵住的洞。**
- **加锁顺序统一为"先锁文件行，再动挂载行"**，同时消除了"写入先插挂载行、删除先改挂载行"
  这种相反锁序在 `uk_biz_file` 间隙锁参与下的**死锁理论面**。
- 残余风险：批量卸载与并发挂载交叉时，InnoDB 仍可能检测到死锁并回滚其中一个事务
  （调用方收到异常）。按 file_id 升序加锁已把概率降到工程可接受，登记为 §6.4 **R4**（业务重试）。
- 代价（显式接受）：同一文件的并发挂载会**串行化**。本项目单实例、低并发，代价可忽略。

- ⚠️ **禁止**沿用 `KnowledgeServiceImpl.delete` 现状的 inline 直连
  （`:120-122` `sysAttachmentMapper.delete(...eq knowledgeId)`）承担删除：它**绕开统一入口**，
  直接违反本节契约。该路径在新模型下会产生"挂载行没了、文件记录和磁盘文件都还在"的**文件层墓碑**；
  若有人图省事把它简化成"删挂载即删文件"，又会**误删其它业务正在共享的同一 `file_id`**
  （B 的挂载行变成指向不存在文件的**悬空引用**）。改造后必须改道统一挂载服务
  （该直连同时属审计 X3 的跨模块直连问题）。

### 2.5.8 物理归属与文档同步

**结论（归属裁决）**：`aik_sys_attachment` 是**通用挂载表**，物理归属 **system 模块**。
迁移前三者矛盾：DDL 在 `sql/aik_knowledge_tables.sql:76`（knowledge 脚本）、Java 在 `system/attachment/`、
而 `KnowledgeServiceImpl` 跨模块直接注入其 Mapper（审计 X3）。
处置：把该表的建表语句**迁往 `sql/aik_system_tables.sql`**（与 `aik_sys_file` 同处 system 脚本），
并从 `sql/aik_knowledge_tables.sql` 删除。
✅ **已落地（2026-09-20）**：建表语句现位于 `sql/aik_system_tables.sql`，`sql/aik_knowledge_tables.sql`
已不含该表，真库结构同步完成（§2.3.4）。
残余的**跨模块直连**（`KnowledgeServiceImpl` 注入并使用 `SysAttachmentMapper`）属**代码侧**退役项，
由挂载层实现任务承担，**不在 DDL 迁移范围内**，也不因本次迁移自动消解。

**`README.md` §3 权威边界需要更新的一行**

现状该表有一行宣称 knowledge 设计文档拥有"表结构"权威，而其 §3.2 描述的正是 `aik_sys_attachment`；
迁移后该表归属 system 模块，此行会**再次制造跨模块归属矛盾**：

| 事实类别 | 唯一权威 | 消费方 |
|---|---|---|
| Knowledge 模块的**包结构与表结构** | `design-docs/knowledge-module/design.md` | 后端 |

应改为：

| 事实类别 | 唯一权威 | 消费方 |
|---|---|---|
| Knowledge 模块的**包结构与表结构**（**不含** `aik_sys_attachment`——通用挂载表归属 system 模块，见 `SDD.md` §2.5） | `design-docs/knowledge-module/design.md` | 后端 |

（同一处收口：`README.md` §4 文档状态表中 `system-module/SDD.md` 的版本 / 备注需同步为 **v1.4 / 附件域两层模型定稿 + 迁移后同步 + 接口口径同步**。）

**下游同步清单**（本版定稿后必须执行）

| 下游文档 | 需同步的内容 |
|---|---|
| `api-contract.md` §2.5.1 | `M1`（`ADD COLUMN file_id BIGINT NULL`）**作废**，改为本文档 §2.3 的完整改造 DDL（`file_id` 为 `NOT NULL`） |
| `api-contract.md` §2.5.3 | `AttachmentVo` 形状以该节为准（**v1.2.2 已定稿**）：**共 8 字段** `{id, fileId, attachName, fileSize, fileType, description, sortOrder, createTime}`，删 `attachUrl` / `knowledgeId`；`fileId` 由可空改必填；`id` = 挂载行主键（正文称 `attachId`，**不得**在 VO 上同时定义 `id` 与 `attachId`）。其中 `fileSize` / `fileType` 是**文件层属性**（由 `fileId` 关联 `aik_sys_file` 带出，**不新增挂载表列**），类型为 `String`（Long→String，见 `api-contract.md` §2.1.3）；**数据来源与批量取法（按 `fileId` 一次 `WHERE id IN (...)`，禁 N+1）以 `api-contract.md` §2.5.3 为准**。~~「增 `bizType` / `bizId`」~~ **该要求已作废**——契约裁决 `AttachmentVo` **不回显** `bizType` / `bizId`（无消费方，理由与 Reopen Trigger 见本节「AttachmentVo 暴露面」）。仍强制：改用专用 VO，**不得**直接暴露 `SysAttachmentPo`（`KnowledgeVo.java:33` 现状是 PO 实体直出） |
| `api-contract.md` §2.5.4 | **删除**「`attachName` 缺省由 `originalName` 回填」；下载文件名改由挂载层决定（§2.5.6 方案 1，含 `attachId`） |
| `api-contract.md` §2.4 / §2.5 | `FileVo` 删 `url` / `filePath` / `storedName`（§2.5.6） |
| `api-contract.md` §3 `M1` | 同 §2.5.1 |
| `knowledge-module/design.md` §3.2 | 删除 / 改写附件表定义，改为指向本文档 §2.5 的指针 |
| `PRD.md`（数据模型表） | `aik_sys_attachment` 语义改为"通用附件挂载表" |
| `frontend/design.md` | 附件列表 / 预览只用 `fileId`（+ `attachId`），不得拼路径；`url` / `filePath` 字段消失 |
| `README.md` §3 / §4 | 见上一小节 |
| `backend-audit.md` | 补记 §6.4 **R1**（`storage_type` 死列：已存在、本次不修）——**不在本任务改动边界内，需另派任务** |

**接口泛化的边界（路径决策已裁决，2026-09-20）**：v1.1 曾把「路径是否由 `findByKnowledgeId` 泛化为
`findByBiz`（带 `bizType` / `bizId`）」列为**待定**的契约决策；现已由 `api-contract.md` §2.5.2
**裁决：采用 `findByBiz`、废弃 `findByKnowledgeId`**（HTTP 形状的最终权威仍在 `api-contract.md`）。
裁决依据两条，均源自本 SDD：
- 路径中出现业务名，等于把刚删除的 `knowledge_id` 硬编码**从【列】搬到【URL】**，与本轮"消除知识专属
  硬编码、改为 `(biz_type, biz_id)` 通用归属"的目标自相矛盾；
- 每新增一个业务域都要在附件模块**新增 Controller 方法并重新编译**，与 §2.5.5 选择开放式
  `VARCHAR(32)` 取值（不用 Java `enum`）的理由**直接冲突**。

本节另强制两条设计约束（不受契约决策影响）：
① 挂载读写**必须**经统一挂载服务，业务模块**不得**直连 `SysAttachmentMapper`；
② 响应体**不得**直接暴露 `SysAttachmentPo` 实体。

**`AttachmentVo` 暴露面（决定，与 `api-contract.md` §2.5.3 一致）**：`AttachmentVo` = `{id, fileId,
attachName, fileSize, fileType, description, sortOrder, createTime}`（**共 8 个**；`fileSize` / `fileType`
是**文件层属性**，由 `fileId` 关联 `aik_sys_file` 带出，**不新增挂载表列**），**不回显** `bizType` / `bizId`。
理由：三个端点**都按业务发起**——`findByBiz` 的调用方本身已知 `bizType` / `bizId`，`save` 由调用方提供，
`remove` 用挂载行 id —— 这两个字段**没有任何消费方**；这与 §2.5.6 对 `md5` 采用的"不暴露无消费方字段"
是**同一判据**，必须保持一致。
⚠️ 注意：`aik_sys_attachment` **表仍保留** `biz_type` / `biz_id` 两列（存储与查询必需）；
"不回显"只是 **API 暴露面**决策，**不是删列**。

**暴露面的边界（别把"最小字段集"当教条）**：本决定**只**排除三类字段——
① **业务归属**（`bizType` / `bizId`：调用方已知，无消费方）；
② **存储布局**（`url` / `filePath` / `storedName` / `md5`：会泄露存储形态）；
③ **首次上传者名**（`originalName`：md5 复用后会串名，谁上传的谁的名字）。
而 `fileSize` / `fileType` 属**内容属性**（不随上传者变化、不泄露存储布局），且**有真实消费方**
（知识详情页附件区的"文件大小 + 类型图标"），故**属于暴露范围**——它们由 R18 裁决恢复为正式字段
（见 `api-contract.md` §2.5.3）。

> **Reopen Trigger**：若将来出现**通用附件管理界面**（需跨业务展示附件归属），再评估是否回显
> `bizType` / `bizId`——且应落在**专用管理 VO**（如 `AttachmentManageVo`）上，
> **不得**把这两个字段加进通用 `AttachmentVo`。

---

## 3. 接口设计

### 3.1 API 列表

| 接口 | 方法 | URL | 说明 |
|------|------|-----|------|
| **字典类型** |
| 分页查询 | POST | /grimoire/dictType/findPage | 分页列表，支持模糊搜索 |
| 查询详情 | GET | /grimoire/dictType/findById | 根据 ID 查询 |
| 新增 | POST | /grimoire/dictType/add | 创建字典类型 |
| 修改 | POST | /grimoire/dictType/modify | 修改字典类型 |
| 删除 | POST | /grimoire/dictType/remove | 物理删除（需检查是否有字典项） |
| **字典项** |
| 按类型查询 | GET | /grimoire/dictItem/findListByType | 不分页，返回全部启用项 |
| 分页查询 | POST | /grimoire/dictItem/findPage | 分页列表，支持模糊搜索 |
| 新增 | POST | /grimoire/dictItem/add | 创建字典项 |
| 修改 | POST | /grimoire/dictItem/modify | 修改字典项 |
| 删除 | POST | /grimoire/dictItem/remove | 物理删除 |
| **系统参数** |
| 分页查询 | POST | /grimoire/systemParam/findPage | 分页列表，支持模糊搜索 |
| 根据键查询 | GET | /grimoire/systemParam/findByKey | 根据 param_key 查询 |
| 新增 | POST | /grimoire/systemParam/add | 创建系统参数 |
| 修改 | POST | /grimoire/systemParam/modify | 修改系统参数（热更新） |
| 删除 | POST | /grimoire/systemParam/remove | 物理删除 |
| 刷新缓存 | POST | /grimoire/systemParam/refreshCache | 手动刷新参数缓存 |
| **文件管理**（第 1 层：文件对象；对外访问只走 download） |
| 上传 | POST | /grimoire/file/upload | multipart/form-data；md5 命中则**跳过写盘**（§4.2） |
| 下载/预览 | GET | /grimoire/file/download | `?id={fileId}[&attachId={attachId}][&preview=true]` 流式输出；`preview=true` → `inline`；`attachId` **挂载层必带、台账层可省**（带则强校：挂载行存在、`del_flag=0` 且 `file_id == id`），响应文件名取自该挂载行 `attach_name`（§2.5.6 方案 1；形状权威 = `api-contract.md` §2.5.5） |
| 分页查询 | POST | /grimoire/file/findPage | 分页列表（响应体不得含存储布局，§2.5.6） |
| 重命名 | POST | /grimoire/file/rename | 改文件台账 `original_name`（管理视图，**不是**用户可见附件名，§2.5.2） |
| 删除 | POST | /grimoire/file/remove | **唯一文件删除入口**；仍有有效挂载则拒绝（§2.5.6 方案 b、§2.5.7） |
| **附件挂载**（第 2 层：挂载点；通用挂载，`biz_type` + `biz_id`） |
| 按业务查询 | GET | /grimoire/attachment/findByBiz | `?bizType=&bizId=` → `ApiResponse<List<AttachmentVo>>`；路径已泛化，**`/grimoire/attachment/findByKnowledgeId` 已废弃**（`api-contract.md` §2.5.2 裁决，依据见 §2.5.8） |
| 保存挂载 | POST | /grimoire/attachment/save | 入参 `AttachmentSaveDto` → `ApiResponse<Void>`；**差量**语义：仅卸载缺失项、仅新增/复活新增项；**禁止全删再全插**（§2.5.7） |
| 卸载挂载 | POST | /grimoire/attachment/remove | 入参 `IdDto`（`{"id":"<attachId>"}`，即**挂载行 id**）→ `ApiResponse<Void>`；走统一删除入口，触发引用计数判定（§2.5.7） |

> ⚠️ 本表只是**文档内索引**。所有接口的路径 / 方法 / 入参 / 出参 / 错误模型以
> [`api-contract.md`](./api-contract.md) 为准；但契约**不得**违反本文档 §2.5 的设计约束
> （尤其：响应体不得暴露存储布局、不得直接暴露 `SysAttachmentPo`、下载文件名必须取自挂载层）。

### 3.2 DTO / VO 定义

#### DictTypeQuery（分页查询入参）

```java
@Data
@Schema(description = "字典类型查询参数")
public class DictTypeQuery extends PageQuery {
    @Schema(description = "字典编码（模糊）")
    private String dictCode;
    
    @Schema(description = "字典名称（模糊）")
    private String dictName;
    
    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
}
```

#### DictTypeDto（新增/修改入参）

```java
@Data
@Schema(description = "字典类型")
public class DictTypeDto {
    @Schema(description = "ID，新增时为空")
    private Long id;
    
    @NotBlank(message = "字典编码不能为空")
    @Size(max = 64, message = "字典编码不能超过64字符")
    @Schema(description = "字典编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictCode;
    
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 128, message = "字典名称不能超过128字符")
    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictName;
    
    @Size(max = 512, message = "描述不能超过512字符")
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;
}
```

#### DictTypeVo（出参）

```java
@Data
@Schema(description = "字典类型视图")
public class DictTypeVo {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "字典编码")
    private String dictCode;
    
    @Schema(description = "字典名称")
    private String dictName;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "修改时间")
    private LocalDateTime modifyTime;
}
```

#### DictItemQuery

```java
@Data
@Schema(description = "字典项查询参数")
public class DictItemQuery extends PageQuery {
    @Schema(description = "字典类型编码（精确）")
    private String dictCode;
    
    @Schema(description = "字典项编码（模糊）")
    private String itemCode;
    
    @Schema(description = "字典项名称（模糊）")
    private String itemName;
    
    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;
}
```

#### DictItemDto

```java
@Data
@Schema(description = "字典项")
public class DictItemDto {
    @Schema(description = "ID，新增时为空")
    private Long id;
    
    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictCode;
    
    @NotBlank(message = "字典项编码不能为空")
    @Schema(description = "字典项编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemCode;
    
    @NotBlank(message = "字典项名称不能为空")
    @Schema(description = "字典项名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemName;
    
    @Schema(description = "排序号", example = "0")
    private Integer sortOrder;
    
    @Schema(description = "状态：1-启用 0-禁用", example = "1")
    private Integer status;
    
    @Schema(description = "备注")
    private String remark;
}
```

#### DictItemVo

```java
@Data
@Schema(description = "字典项视图")
public class DictItemVo {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "字典类型编码")
    private String dictCode;
    
    @Schema(description = "字典项编码")
    private String itemCode;
    
    @Schema(description = "字典项名称")
    private String itemName;
    
    @Schema(description = "排序号")
    private Integer sortOrder;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

#### DictTypeItemsVo（类型+项聚合）

```java
@Data
@Schema(description = "字典类型及字典项聚合")
public class DictTypeItemsVo {
    @Schema(description = "字典类型编码")
    private String dictCode;
    
    @Schema(description = "字典类型名称")
    private String dictName;
    
    @Schema(description = "字典项列表")
    private List<DictItemVo> items;
}
```

#### SystemParamQuery

```java
@Data
@Schema(description = "系统参数查询参数")
public class SystemParamQuery extends PageQuery {
    @Schema(description = "参数键（模糊）")
    private String paramKey;
    
    @Schema(description = "参数分组")
    private String paramGroup;
}
```

#### SystemParamDto

```java
@Data
@Schema(description = "系统参数")
public class SystemParamDto {
    @Schema(description = "ID，新增时为空")
    private Long id;
    
    @NotBlank(message = "参数键不能为空")
    @Schema(description = "参数键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paramKey;
    
    @NotBlank(message = "参数值不能为空")
    @Schema(description = "参数值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String paramValue;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "参数分组")
    private String paramGroup;
    
    @Schema(description = "是否可编辑：1-是 0-否", example = "1")
    private Integer editable;
}
```

#### SystemParamVo

```java
@Data
@Schema(description = "系统参数视图")
public class SystemParamVo {
    @Schema(description = "ID")
    private Long id;
    
    @Schema(description = "参数键")
    private String paramKey;
    
    @Schema(description = "参数值")
    private String paramValue;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "参数分组")
    private String paramGroup;
    
    @Schema(description = "是否可编辑")
    private Integer editable;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

#### FileQuery

```java
@Data
@Schema(description = "文件查询参数")
public class FileQuery extends PageQuery {
    @Schema(description = "原始文件名（模糊）")
    private String originalName;
}
```

#### FileVo（文件视图 · **对外**）

> 由 `FileController` 直接返回（`upload` / `findPage` / `findById` 的响应体），
> 因此**它就是"对外"的定义**。字段集受 §2.5.6 约束：**不得暴露存储布局**。

```java
@Data
@Schema(description = "文件视图")
public class FileVo {
    @Schema(description = "ID（即 fileId）")
    private Long id;

    @Schema(description = "原始文件名（首次上传者的名字，管理视图用；不得作为附件显示名的来源）")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MIME类型")
    private String fileType;

    @Schema(description = "存储类型：local / oss / sftp（策略标签，不泄露路径）")
    private String storageType;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

**相对现状（代码 `FileVo.java`）的字段变化**：

| 字段 | 处置 | 理由 |
|---|---|---|
| `url` | **删除** | 访问地址不可持久化，访问统一走 download（§2.5.6） |
| `filePath` | **删除** | "访问路径"本身；删除它才能兑现"不给前端任何访问路径形态"（§2.5.6） |
| `storedName` | **删除** | 同属存储布局（磁盘上的存储名） |
| `md5` | **删除** | 内容指纹属内部锚点，不对外；需要秒传预检时另开探测端点 |
| `storageType` | **保留** | 策略标签，不泄露路径；管理页筛选需要（§2.5.6 显式记录该选择） |

> ⚠️ `FileVo.of()` 是 `BeanUtil.copyProperties(po, vo)` 全量拷贝 —— 删字段即阻断泄漏，
> **不需要**改 `of()` 的实现；但**新增字段时**必须重新审视是否属于存储布局。
> 管理端若确需路径，另开管理专用 VO（`FileManageVo`），不得混入本 VO。

### 3.3 错误码定义

复用 `ResultCode` 枚举，新增业务错误码在 Service 层通过 `BusinessException` 抛出：

| 错误码 | 说明 | 使用场景 |
|--------|------|---------|
| 200 | 操作成功 | 通用成功 |
| 400 | 操作失败 / 参数错误 | 参数校验失败、业务规则违反 |
| 404 | 资源不存在 | 根据 ID 查询不到记录 |
| 500 | 服务器内部错误 | 异常未捕获 |

**业务异常场景**（msg 自定义）：
- 字典编码已存在
- 字典项编码在同一类型下已存在
- 参数键已存在
- 字典类型下存在字典项，不允许删除
- 文件不存在
- 文件存储失败
- 不支持的文件类型（白名单校验，`FileServiceImpl:47-54`）
- **文件仍被其它挂载点引用，不允许删除**（§2.5.7 引用计数守卫；`FileService.remove` 前置校验）
- **挂载的 fileId 不存在**（悬空挂载守卫；挂载写入与下载校验均需）
- **未识别的 biz_type**（§2.5.5 白名单校验）
- **挂载行不存在 / 已卸载 / 与 fileId 不匹配**（`attachId` 下载校验，§2.5.6 方案 1）

> 同一业务重复挂载同一文件**不抛异常**：按 §2.5.7 的复活契约吸收（命中已卸载行 → 复活）。

---

## 4. 核心流程设计

### 4.1 系统参数热更新流程

```
@PostConstruct
    |
    ▼
加载所有参数到 ConcurrentHashMap
    |
    ▼
┌────────────────────────────────────────┐
│         SystemParamService             │
│  ┌─────────────────────────────────┐   │
│  │  paramCache: ConcurrentHashMap  │   │
│  │  <String, String>               │   │
│  └─────────────────────────────────┘   │
└────────────────────────────────────────┘
    |
    ├── findByKey(key) ──► 直接从缓存读取 O(1)
    |
    ├── add(param) ──► 写入 DB ──► 刷新缓存
    |
    ├── modify(param) ──► 更新 DB ──► 刷新缓存
    |
    ├── remove(id) ──► 删除 DB ──► 刷新缓存
    |
    └── refreshCache() ──► 清空缓存 ──► 重新加载
```

**关键设计**：
- 使用 `ConcurrentHashMap` 保证线程安全
- `@PostConstruct` 初始化时全量加载
- 增删改操作：先写数据库，再刷新缓存（保证最终一致性）
- 无分布式锁（单实例应用，无需 Redis）

### 4.2 文件上传流程（含 md5 秒传）

```
Client
    |
    ▼
POST /grimoire/file/upload (MultipartFile)
    |
    ▼
FileController.upload() → FileServiceImpl.upload()
    |
    ├── ① 校验：文件非空 / 大小 ≤ max-size（10MB）/ MIME + 扩展名白名单
    |
    ├── ② 计算 md5 = DigestUtil.md5Hex(...)
    |
    ├── ③ 查重：fileMapper.selectLatestByMd5(md5)   ← 应用层约定，非 DB 约束（§2.5.4）
    │        └── 命中 → 【跳过写盘】，直接返回已有 FileVo（秒传，磁盘零写入）
    |
    ├── ④ 未命中 → 生成 storedName = 雪花ID + 原扩展名、relativePath = yyyy/MM/dd/
    │             → 磁盘写入 {base-path}/{relativePath}/{storedName}
    |
    ├── ⑤ 记录：插入 aik_sys_file（IdType.INPUT，Snowflake ID）
    |
    └── ⑥ 返回 FileVo（id 即 fileId）
    |
    ▼
（挂载是另一步：业务侧拿 fileId 建挂载行，见 §4.5）
```

**关键设计**：

- 秒传的收益是**少写字节**，不是"少写一行库"：③ 命中即跳过 ④，**磁盘零写入**，孤儿文件无从产生。
- **现状三步之间零查重**（`FileServiceImpl:61` 算 md5 → `:64` 无条件下盘 → `:79` 无条件 insert）。
  因 `idx_md5` 非唯一，第二次上传同内容**不报错**，而是静默写入第二份磁盘文件 + 第二行记录
  （静默重复存储，§2.3.0）。
- **没有 `DuplicateKeyException` 补偿路径**：`idx_md5` 非唯一，插入不会因 md5 冲突失败（甲方案）。
  并发窗口下可能落两行同 md5，由 §2.3.2 查询① 对账检出。
- `file.getInputStream()` 现状被调用两次（`:61` 算 md5、`:64` 上传）：`StandardMultipartFile`
  每次返回新流所以当前可用，但这是**隐式依赖**；改造时应显式化（先读入 `byte[]` 或复用同一流），
  避免实现换代后静默失效。
- 上传接口**不产生挂载行**：文件对象与挂载点分层，上传只登记字节。

### 4.3 文件删除流程（挂载引用计数 + 提交后删盘）

**（A）卸载请求只经唯一合法入口**（统一挂载服务 / `FileService`；**禁止** `KnowledgeServiceImpl`
用 inline `sysAttachmentMapper.delete` 直连，§2.5.7）

**（B）以下全部在【同一事务】内（含 (A) 的挂载行变更）**

```
SELECT ... FROM aik_sys_file WHERE id = ? FOR UPDATE      ← 【先】锁文件行（与写入路径锁序一致）
UPDATE aik_sys_attachment SET del_flag = 1 WHERE ...      ← 再标记卸载该挂载行
SELECT COUNT(*) FROM aik_sys_attachment
     WHERE file_id = ? AND del_flag = 0                    ← 只数【有效】挂载
├── count > 0 → 停止：只卸载挂载，文件记录与磁盘文件保留（其它业务仍在共享）
└── count = 0 → DELETE FROM aik_sys_file WHERE id = ?       ← 物理删除（文件层唯一删除所有者）
                行已不存在 → 幂等 no-op，不抛异常
```

**（C）事务提交之后**（`TransactionSynchronization#afterCommit`）

```
fileStorageStrategy.remove(file_path) 删盘
└── 失败 → 记录日志 + §2.3.2 查询③ 对账清理（只留孤儿磁盘文件，不产生悬空引用）
```

**管理端直接删文件（`POST /grimoire/file/remove`）走同一契约的前置校验分支**：

```
FileService.remove(id)
    ├── 查询 FileRecordPo（不存在 → 幂等 no-op）
    ├── 校验：是否还有 del_flag = 0 的挂载行
    │        └── 有 → BusinessException「文件仍被其它挂载点引用，不允许删除」
    └── 无 → 物理删记录（同事务）+ 提交后删盘（与 (B)(C) 相同）
```

> **旧实现已作废**：原 `FileServiceImpl:140-146` 只做 `deleteById`（逻辑删除，不删盘、不校验挂载），
> 以及本节旧稿的"先删磁盘再删记录、无事务"——后者会在共享场景误删其它业务的文件，
> 且"先删盘再回滚"会造成**不可恢复的悬空引用**。

### 4.4 事务设计

| 场景 | 事务边界 | 说明 |
|------|---------|------|
| 字典类型删除 | `@Transactional` | 检查字典项数量 -> 删除类型，单操作 |
| 附件挂载保存 | `@Transactional` | **差量**：卸载缺失项 + 复活/新增（§2.5.7 第 5 条）；禁止全删再全插 |
| 挂载卸载 + 文件层删除 | `@Transactional`（**必须**） | 删挂载行与删文件记录同事务；文件行 `FOR UPDATE` 串行化并发卸载；**删盘在提交后** |
| 文件删除（`FileService.remove`） | `@Transactional` | 同上；仍有有效挂载则拒绝 |
| 文件上传（写盘 + 插入） | 无事务（写盘不可回滚） | 查重命中前置跳过写盘，孤儿风险由此消除；插入失败需删掉刚落盘的文件（尽力而为 + 日志） |
| 删除知识条目 | `@Transactional` | 标签关联 + 挂载卸载 + 主表删除同事务；挂载卸载**必须**经统一服务（§4.5） |
| 系统参数修改 | `@Transactional` | 更新 DB + 刷新缓存 |

> **已删除的旧行**：「文件删除 \| 无事务 \| 先删磁盘再删记录，磁盘操作无法回滚」
> —— 该设计已被 §2.5.7 取代，顺序**反过来**：先提交 DB，后删盘。

### 4.5 附件挂载与卸载流程

**挂载（差量 upsert）**

```
POST /grimoire/attachment/save（或 KnowledgeDto.attachments）
    |
    ▼
统一挂载服务 AttachmentService.save(bizType, bizId, List<AttachmentDto>)
    |
    ├── ① 校验 bizType 白名单（§2.5.5）；bizId 非空
    ├── ② 逐项【加锁】校验 fileId 存在：SELECT ... FOR UPDATE（aik_sys_file）
│        ← 悬空挂载守卫；与 §2.5.7 写入第 1 条同，且与删除路径锁序一致（防 §6.4 R4 死锁与悬空挂载）
    ├── ③ 差量：oldSet = 该 (bizType, bizId) 下 del_flag = 0 的挂载行
    │           newSet = 入参列表
    ├── ④ 仅对 oldSet − newSet：UPDATE del_flag = 1（卸载）
    ├── ⑤ 仅对 newSet − oldSet：upsert
    │        └── 查（含已卸载行）→ 命中则复活（UPDATE del_flag = 0）→ 未命中则 insert
    │            并发撞 DuplicateKeyException → 复活并重试一次
    └── ⑥ 仅对 newSet ∩ oldSet：按需 UPDATE sort_order / attach_name / description
```

**卸载（业务侧删除知识条目）**

```
KnowledgeServiceImpl.delete(id)（现状 :109-126）
    |
    ├── 删标签关联（保持现状）
    ├── 挂载卸载：调用统一挂载服务 / FileService   ← 禁止 inline sysAttachmentMapper.delete(:120-122)
    └── baseMapper.deleteById(id)

（挂载服务内部 → §4.3 的 (A)(B)(C) 三步）
```

**关键设计**

- **禁止"全删再全插"**：`uk_biz_file` 不含 `del_flag`，全删产生的卸载行会让紧接着的全插撞唯一键；
  且每次编辑都产生无意义的状态翻转（§2.5.7 第 5 条）。
- **业务模块不得直连挂载 Mapper**：现状 `KnowledgeServiceImpl:52`（注入）、`:120-122`（inline 删除）、
  `:190-194`（inline 查询）三处直连必须改道统一服务；这同时消解审计 X3 的跨模块直连问题。
- **响应体不得直接暴露 PO**：`KnowledgeVo.attachments`（`KnowledgeVo.java:33`）当前是
  `List<SysAttachmentPo>`，改造后必须换成专用 `AttachmentVo`。形状权威 = `api-contract.md` §2.5.3，
  **共 8 字段** `{id, fileId, attachName, fileSize, fileType, description, sortOrder, createTime}`
  （`fileSize` / `fileType` 是**文件层属性**，由 `fileId` 关联 `aik_sys_file` 带出；来源与**批量取法
  （按 `fileId` 一次 `WHERE id IN`，禁 N+1）以 `api-contract.md` §2.5.3 为准**）：挂载行主键的
  **字段名是 `id`**（本文件正文称其为 `attachId`，是同一事物的两个叫法，**不要**在 VO 上同时定义
  `id` 与 `attachId`）；**不含** `bizType` / `bizId`（理由与边界见 §2.5.8「AttachmentVo 暴露面」）。
- 删除知识条目**必须**经统一服务卸载挂载：inline 删除会留下"挂载行没了、文件记录和磁盘文件都还在"的
  文件层墓碑；若有人图省事改成"删挂载即删文件"，又会误删其它业务共享的同一 `file_id`（§2.5.7）。

---

## 5. 技术方案选型

### 5.1 参数缓存

| 方案 | 选择 | 理由 |
|------|------|------|
| ConcurrentHashMap | 采用 | 单实例应用，无需分布式缓存；读写 O(1) |
| @Cacheable (Caffeine) | 不采用 | 引入额外依赖，本项目轻量为主 |
| Redis | 不采用 | 个人项目，无需外部依赖 |

### 5.2 文件存储

| 方案 | 选择 | 理由 |
|------|------|------|
| 本地磁盘 | 采用 | 项目已配置，无外部依赖，可直接浏览 |
| 对象存储(OSS) | 不采用 | 个人项目，无需云服务依赖 |
| FastDFS/MinIO | 不采用 | 增加部署复杂度 |

**内容寻址与存储策略的关系**：

- `aik_sys_file` 是**内容寻址的派生台账**：一行 = 一份字节，`md5` 是复用锚点（§2.5.4）。
- **对外地址不再是存储能力的职责**：`url` 列已删除，访问统一走 `/grimoire/file/download`（§2.5.6）。
  `FileStorageStrategy#getUrl` 及其三个实现**已随该列一并删除**（甲方案，§2.5.6）；若将来需要直连地址，
  应新增语义明确的按需方法（如 `presignedUrl`），且**不得落库**。
- `storage_type` 目前是**只写不读的死列**（唯一写入点 `FileServiceImpl:75`，全库无任何按它选择策略的代码）：
  策略是启动时按 `grimoire.file.use` 注入的**单一 bean**，其"预留多存储共存"的设计意图**未实现**。
  登记为风险 **R1**（§6.4），本次**不修**。

### 5.3 异步处理

| 方案 | 选择 | 理由 |
|------|------|------|
| Spring @Async | 不采用 | 当前无异步场景 |
| CompletableFuture | 预留 | 未来扩展使用 |

---

## 6. 设计评审

### 6.1 评审结论

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 表结构合理性 | 通过 | 5 张表（dict_type / dict_item / param / file / attachment）；附件域为两层模型，字段精简，索引覆盖查询场景 |
| 关联关系正确性 | 通过 | 字典类型-字典项一对多；**文件对象-挂载点一对多**（一份文件可被 N 个业务复用，§2.5.1） |
| 附件域归属边界 | 通过 | `aik_sys_file` = 文件对象（派生台账，**物理删除**）；`aik_sys_attachment` = 挂载点（业务事实，卸载保行）；删除走唯一入口 + 引用计数（§2.5） |
| 对外暴露面 | 通过 | 无访问路径形态外泄：`FileVo` 不含 `url` / `filePath` / `storedName` / `md5`；访问统一走 download（§2.5.6） |
| 索引设计 | 通过 | 主键、唯一键、查询索引齐全；`idx_md5` 为**非唯一**且必须保留（秒传查重，§2.5.4） |
| API 设计规范性 | 通过 | 符合 aIk-coding-style 动词式规范 |
| DTO/VO 分离 | 通过 | 入参用 DTO，出参用 VO；**新增约束**：`KnowledgeVo.attachments` 不得直接暴露 `SysAttachmentPo`（§4.5） |
| 复用组件检查 | 通过 | 复用 BaseEntity、ApiResponse、PageQuery 等；**不得**改用 `BaseLogicEntity`（§2.4.1） |
| 事务边界 | 通过 | 关键操作有事务标注；删除路径为"同一事务内改挂载+删记录，**提交后**删盘"（§2.5.7、§4.4） |

### 6.2 Warnings

| # | 级别 | 问题 | 说明 |
|---|------|------|------|
| W01 | warning | 字典类型删除未做级联 | 仅检查是否有字典项，不级联删除字典项（符合物理删除策略） |
| W02 | warning | 文件上传无 MIME 白名单 | 个人项目不做限制，部署时由 Nginx 或防火墙限制 |
| W03 | warning | md5 去重靠**应用层约定** | `idx_md5` 非唯一，无 DB 约束兜底；极端并发可能落两行同 md5。靠 §2.3.2 查询① 对账检出（§2.5.4） |
| W04 | warning | 挂载唯一键与"卸载保行"存在张力 | `uk_biz_file` 不含 `del_flag`，卸载行仍占键值 → 写入**必须** upsert/复活；裸 `insert` 会让"同一业务重挂同一文件"永久失败（§2.5.7 第 3 条） |
| W05 | warning | `BaseLogicEntity` 是未使用的死基础设施 | 其逻辑删除列名是 `deleted`，全项目**零子类**。本次不引入、不删除；建议独立议题处置，避免下一位作者误用（列名不匹配 → 启动即 SQL 报错） |
| W06 | warning | 挂载/卸载**事件历史**缺失 | 复活语义复用同一行，不记录"何时被卸载过 / 几次"（§2.5.7 第 6 条）。完整时间线需 append-only 事件表，本次范围外 |
| W07 | warning | `download_count` 丢更新 | `FileServiceImpl:101-104` 是"读值 + 1"而非原子自增；并发丢更新，且 `rename` 的全实体 `updateById` 会连带覆盖。应改原子自增（审计 X6） |

### 6.3 Failures

无。

### 6.4 风险点

| # | 风险 | 缓解措施 |
|---|------|---------|
| — | 系统参数缓存与数据库不一致 | 所有写操作后刷新缓存；提供手动刷新接口 |
| — | 文件删除时磁盘文件已不存在 | 删盘操作须**幂等**（`remove` 对不存在的路径返回 false / 不抛异常），不因缺文件而使删除失败 |
| — | 大文件上传内存溢出 | 配置文件大小限制 10MB；Spring multipart 配置 |
| **R1** | `storage_type` 是**只写不读的死列**：策略为启动时按 `grimoire.file.use` 注入的**单一 bean**。一旦该配置从 `local` 改为 `oss`/`sftp`，历史记录的 `file_path` 在新策略下**全部失效**（local 是相对路径、OSS 是 objectKey、SFTP 是远端绝对路径，三者互不兼容）→ 历史文件无法下载 | **已存在、本次不修**（超出附件改造边界）。要么将来实现策略路由，要么登记为待退役列；本次**只如实记录**。⚠️ 需另派任务补记进 `backend-audit.md` |
| **R2** | 事务提交后删盘失败 → **孤儿磁盘文件** | 记录日志 + §2.3.2 查询③ 对账清理。注意顺序**不可反**：先删盘再回滚会产生**不可恢复**的悬空引用（§2.5.7） |
| **R3** | 并发秒传落两行同 md5 | 两行内容完全相同，**无数据损坏**；§2.3.2 查询① 对账检出（§2.5.4） |
| **R4** | 批量卸载与并发挂载交叉时，InnoDB 死锁检测会回滚其中一个事务 | 加锁顺序已统一为"先锁文件行、批量按 `file_id` 升序"，概率降到工程可接受；残余情形按**业务重试**处理（§2.5.7 并发与加锁顺序） |
| **R5** | 「卸载最后引用」与「新业务挂载同一文件」并发 → **悬空挂载**（指向已物理删除的文件，下载 404） | **已闭合（非登记项）**：写入与卸载路径的第一条语句都是 `SELECT ... FOR UPDATE` 锁定文件行（§2.5.7 写入第 1 条 / §4.5 步骤② / §4.3 (B)），T2 会阻塞在 T1 的行锁上，T1 提交后查不到文件行 → 抛「文件不存在」。**若实现方漏掉加锁读**，该竞态立即复活 → 由 §2.3.2 查询② 对账检出 |

---

## 7. 附录

### 7.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 表名 | `aik_sys_` 前缀，snake_case | `aik_sys_dict_type` |
| 字段 | snake_case | dict_code, create_time |
| Java 类 | 驼峰 | DictTypePo, DictTypeVo |
| Mapper | XxxMapper | DictTypeMapper |
| Service | XxxService / XxxServiceImpl | DictTypeService |
| Controller | XxxController | DictTypeController |

### 7.2 项目复用清单

| 组件 | 来源 | 说明 |
|------|------|------|
| BaseEntity | core/po | id, createTime, modifyTime |
| ApiResponse | core/dto | 统一返回封装 |
| PageQuery | core/dto | 分页查询参数 |
| ResultCode | core/enums | 响应码枚举 |
| BusinessException | core/exception | 业务异常 |
| AssertUtils | core/utils | 业务断言 |
| FileStorageConfig | core/config | 文件存储配置 |

### 7.3 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-05-15 | 初始版本 |
| v1.1 | 2026-09-20 | 同步 `4f366df` 表名规范 `aik_*` → `aik_sys_*`（20 处，含 §2.1 ER 图 / §2.2 表结构 / §2.3 DDL / §2.4 实体注解 / §7.1 命名示例）；新增 `aik_sys_attachment` 表结构与 DDL；§1.2 包结构由"按层分包"修正为实际的"按功能域分包"；补 `aik_sys_file` 缺失的 7 个字段（`storage_type` / `md5` / `url` / `del_flag` / `create_by` / `modify_by` 等）；补 §3.1 附件三接口与契约指针 |
| v1.2 | 2026-09-20 | **附件域两层模型定稿**。新增 §2.5（§2.5.1 两层模型与"墓碑"术语 / §2.5.2 `attach_name` 权威归属 / §2.5.3 文件表物理删除 / §2.5.4 md5 秒传＝应用层约定 / §2.5.5 `biz_type` 取值与常量 / §2.5.6 访问与下载文件名 / §2.5.7 挂载写入与卸载契约 / §2.5.8 物理归属与文档同步）；§2.1 ER 删除 `url` 与 `knowledge_id` 节点、补 1:N 关系；§2.2 `aik_sys_file` 删 `url` / `del_flag`，`aik_sys_attachment` 改为通用挂载表（12 列）；§2.3 DDL 重写 + 新增 §2.3.0 实测基线 / §2.3.1 迁移 DDL / §2.3.2 对账 SQL / §2.3.3 前置硬断言；§2.4 重写 `FileRecordPo`、新增 `SysAttachmentPo` 与 §2.4.1 实体继承强制约定（**不得**用 `BaseLogicEntity`），并修正 5 处 `package` 声明与实际代码对齐；§3.1 接口索引按两层模型重写；§3.2 `FileVo` 删 `url` / `filePath` / `storedName` / `md5`（不暴露存储布局）；§3.3 补删除守卫与 `biz_type` 错误场景；§4.2 上传流程补 md5 秒传（命中跳过写盘）；§4.3 删除流程改为"挂载引用计数 + 行锁串行化 + 提交后删盘"；§4.4 事务表重写；新增 §4.5 挂载/卸载流程；§5.2 补内容寻址与存储策略关系；§6.1/§6.2/§6.4 更新并新增 W03-W07、R1-R4（含写入/删除路径加锁顺序契约）；**作废三处错误论断**（"运行时存在 `uk_md5` 唯一键"、"重复上传必崩并留孤儿文件"、"物理删除以释放唯一键"）；`getUrl()` 登记为**有意保留**（只删列与持久化调用）—— **该保留已于 v1.3 依船长裁决改为删除**，见 §2.5.6 |
| v1.3 | 2026-09-20 | **K1 `getUrl` 裁决 + K2 迁移后同步**。〔K1〕`FileStorageStrategy#getUrl(String)` 接口方法与三个实现（`LocalFileStorage:82-85` / `OssFileStorage:96-101` / `SftpFileStorage:112-116`）由"有意保留（反熵例外，Sign-off=船长裁决）"改为**删除**，并入 §2.5.6 删除清单第 3–4 项；文档头与 §5.2 的"按需能力保留"口径同步校正；**反向登记** `exists()`（0 调用方、但不属 url 链路、本次不删）与两个接口外 `download` 重载为待退役候选。〔K2〕文档头「⚠️ 附件域迁移中」告示改为**已落地**陈述（`aik_sys_attachment` 现位于 `sql/aik_system_tables.sql`）；§2.3.0 更名「迁移前实测基线（2026-09-20，历史留档）」并加"本节非现状"警示；§2.3.1 标注"已于 2026-09-20 执行完毕"并指明可执行脚本权威位置为 `sql/attachment_two_layer_migration.sql`（本节内嵌 SQL 仅作文档内对照）；§2.3.3 改标"重放前必跑"；**新增 §2.3.4「迁移后实测状态」**（`aik_sys_file` 13 列 / `aik_sys_attachment` 12 列 + 索引 + `TABLE_COMMENT` + 迁移路径≡全新安装路径的等价性 + `sql/backup/` 证据清单 + 漂移处置）；§2.5.8 归属裁决由"处置中"改为**已落地事实**，并区分残余的代码侧跨模块直连退役项。**§2.5 其余设计裁决（两层模型、`attach_name` 权威、文件表物理删除、md5 甲方案、`attachId` 方案 1、加锁顺序契约）未改动** |
| v1.4 | 2026-09-20 | **跟随 `api-contract.md` v1.2 的两处裁决（船长已批准）做接口口径同步**。〔裁决① 路径泛化〕§3.1 附件三端点同步为 `GET /grimoire/attachment/findByBiz?bizType=&bizId=` → `ApiResponse<List<AttachmentVo>>`、`POST /grimoire/attachment/save`（入参 `AttachmentSaveDto`，**差量**）、`POST /grimoire/attachment/remove`（入参 `IdDto` = 挂载行 id），**`/grimoire/attachment/findByKnowledgeId` 标注废弃**；§2.5.8「接口泛化的边界」由"属契约决策、由契约定"改为**已裁决**，并写入两条依据（路径中出现业务名 = 把 `knowledge_id` 硬编码从【列】搬到【URL】；每新增业务域都要改附件模块 Controller 并重编译，与 §2.5.5 开放式 `VARCHAR(32)` 的理由直接冲突）；§2.5.6 入口行与 §3.1 `file/download` 行补 `attachId` **条件必填**（挂载层必带 / 台账层可省 / 带则强校）。〔裁决② 暴露面〕`AttachmentVo` **恰为 6 字段** `{id, fileId, attachName, description, sortOrder, createTime}` 且**不回显** `bizType` / `bizId`（三端点均按业务发起、无消费方，与 §2.5.6 对 `md5` 的同一判据）；§2.5.8 下游清单第 2 行「增 `bizType` / `bizId`」**划删除线作废**并收敛为指向 `api-contract.md` §2.5.3，新增 **Reopen Trigger**（出现通用附件管理界面 → 落在专用管理 VO，不得进通用 `AttachmentVo`）；明确表列 `biz_type` / `biz_id` **保留**、不回显仅是 API 暴露面决策；§4.5「响应体不得直接暴露 PO」补正 VO 字段口径（主键字段名是 `id`，**不得**同时定义 `id` 与 `attachId`）。**§2.5 的设计裁决实体（两层模型、`attach_name` 权威、文件表物理删除、md5 甲方案、加锁顺序契约、`getUrl` 删除）未改动** |
| v1.5 | 2026-09-20 | **O 收尾精确化（跟随 R18 与两处实测校正）**。① **`AttachmentVo` 由 6 字段改为 8 字段**（+`fileSize` / `fileType`）：改的是**两处现行口径**——§2.5.8 下游清单第 2 行（"v1.2 已定稿"→**v1.2.2**）与 §4.5「响应体不得直接暴露 PO」；均注明 `fileSize` / `fileType` 是**文件层属性**（由 `fileId` 关联 `aik_sys_file` 带出、**不新增挂载表列**），类型 `String`（Long→String，`api-contract.md` §2.1.3），**批量取法（按 `fileId` 一次 `WHERE id IN`，禁 N+1）以 `api-contract.md` §2.5.3 为准**；**§7.3 的历史行一律保留历史值，不回溯**（v1.4 行仍写"恰为 6 字段"，因其描述的是 v1.4 当时的真实状态）。② §2.5.8「`AttachmentVo` 暴露面（决定）」补**暴露面边界澄清**：不暴露的只有三类——业务归属（`bizType` / `bizId`）、存储布局（`url` / `filePath` / `storedName` / `md5`）、首次上传者名（`originalName`）；`fileSize` / `fileType` 属**内容属性且有真实消费方**，**在暴露范围内**——防止"最小字段集"被误用成教条。③ 修正 §2.5.6 对 `AbstractFileStorage` 两个 `download` 重载的**分类错误**（2 处：删除清单注脚 + 「次要登记」表）：`:162-167` **是接口方法 `FileStorageStrategy:53` 的 `@Override`（在接口内）**，仅 `:172-176`（三参、带 `preview`）不在接口内；权威分类见 `backend-audit.md` §10.3。**不改变 t6 范围**（两者均不删）。④ 新增**行区间引用约定**声明（§2.5.6 开头）：本文档行区间**含 `@Override` 注解行**，故 `LocalFileStorage:82-85` / `OssFileStorage:96-101` / `SftpFileStorage:112-116` / `AbstractFileStorage:162-167` **行号无需改动**。⑤ 修正 v1.3 压缩头措辞（"**§2.5 的设计裁决内容未改动**" → "**§2.5 其余设计裁决未改动**"，并显式指出本版改了 §2.5.6 的 `getUrl`），与 §7.3 的 v1.3 详细条目一致。**§2.5 已定稿的裁决实体（两层模型 / `attach_name` 权威 / 文件表物理删除 / md5 甲方案 / 加锁顺序 / `getUrl` 删除）未改动** |
