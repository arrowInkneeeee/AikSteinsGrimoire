# 产品需求文档 (PRD) — system 模块

> 版本：v1.2 | 更新：2026-09-20 | 状态：生效中 | 权威范围：system 模块需求与功能清单
> v1.0 → v1.1：同步 `4f366df` 表名规范 `aik_*` → `aik_sys_*`（4 张表）；补 `aik_sys_attachment`
> v1.1 → v1.2：附件/文件域需求对齐 `SDD.md` v1.2 两层模型与 `api-contract.md` v1.2——文件上传（md5 秒传复用）、文件下载（预览 + 挂载层文件名）、文件删除（引用计数守卫 + 提交后删盘）改写，新增 F009/F010 与 US-009；`aik_sys_attachment` 定位改为**通用附件挂载表**（`file_id` + `biz_type` + `biz_id`）；「文件预览」移出 Won't Have（`preview` 已实现）。**同日**：附件域 DDL 迁移已对真库落地，`aik_sys_attachment` 已迁入 `sql/aik_system_tables.sql`（SDD v1.3 / §2.3.4）
>
> 初次生成：2026-05-15 | 模块：`io.aik.steins.grimoire.system`
> 接口契约权威：[`api-contract.md`](./api-contract.md)（本文档不含接口定义）

---

## 1. 项目概述

### 1.1 背景
AikSteinsGrimoire 是个人知识魔典后端项目，采用 Spring Boot 2.7.18 单体架构。`core` 基础设施层已完成，现需开发 `system` 模块作为公共服务层，为 article、category、tag 等业务模块提供基础能力支撑。

### 1.2 目标
开发 system 模块，提供字典管理、系统参数、文件管理三大基础服务，**以及供各业务模块复用的附件挂载能力**，形成可复用的公共服务层。

---

## 2. 功能清单

| ID | 功能描述 | 类别 | 状态 |
|----|---------|------|------|
| F001 | 字典类型管理（编码、名称、描述、状态 CRUD） | core | 已澄清 |
| F002 | 字典项管理（类型编码、项编码、项名称、排序、状态、备注 CRUD） | core | 已澄清 |
| F003 | 系统参数管理（参数键、值、描述、分组、可编辑标志 CRUD） | core | 已澄清 |
| F004 | 系统参数热更新（运行时修改参数值即时生效） | technical | 已澄清 |
| F005 | 文件上传（通用上传，日期分目录，雪花 ID 命名；**md5 命中则跳过写盘**，复用已有文件对象） | core | 已澄清 |
| F006 | 文件下载 / 预览（流式传输；`preview` 控制 `inline` / `attachment`；文件名取自挂载层 `attach_name`） | core | 已澄清 |
| F007 | 文件删除（**唯一入口**：无有效挂载才允许；物理删除记录 + 事务提交后删盘） | core | 已澄清 |
| F008 | 文件列表分页查询（响应体**不含**存储布局：无 `url` / `filePath` / `storedName` / `md5`） | auxiliary | 已澄清 |
| F009 | 附件挂载管理（通用挂载：`biz_type` + `biz_id` + `file_id`；一条业务可挂多个附件，各自有显示名、说明与排序） | core | 已澄清 |
| F010 | 附件卸载与文件回收（卸载按**差量**执行；文件仍被其它业务挂载时保留，仅剩最后一个有效挂载消失时才删记录 + 删盘） | core | 已澄清 |

---

## 3. 用户故事

### 3.1 Must Have

#### US-001: 字典类型管理
**优先级**: Must Have | **业务价值**: 高 | **用户影响**: 高

As a 系统管理员
I want 管理数据字典类型（如 article_type、file_category）
So that 其他业务模块可以引用标准化的枚举值

**验收标准**:
```gherkin
Given 字典类型表为空
When 创建字典类型 article_type，编码="article_type"，名称="文章类型"，状态=启用
Then 返回成功，数据库中新增一条字典类型记录

Given 已存在字典类型 article_type
When 查询字典类型列表
Then 返回包含 article_type 的分页列表

Given 字典类型 article_type 存在
When 修改其名称为"文章分类"
Then 更新成功，名称已变更

Given 字典类型 article_type 下无字典项
When 删除该字典类型
Then 删除成功，记录被物理删除
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.5 天

---

#### US-002: 字典项管理
**优先级**: Must Have | **业务价值**: 高 | **用户影响**: 高

As a 系统管理员
I want 在字典类型下管理具体的字典项
So that 文章类型等枚举值可以动态配置

**验收标准**:
```gherkin
Given 字典类型 article_type 已存在
When 添加字典项，编码="NOTE"，名称="笔记"，排序=1
Then 字典项添加成功

Given 字典类型 article_type 下有多个字典项
When 按类型编码查询字典项列表
Then 返回该类型下的所有字典项，按 sort_order 升序排列

Given 字典项 NOTE 存在
When 修改其名称为"学习笔记"
Then 更新成功

Given 字典项 NOTE 存在
When 删除该字典项
Then 删除成功
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.5 天

---

#### US-003: 系统参数管理
**优先级**: Must Have | **业务价值**: 高 | **用户影响**: 高

As a 系统管理员
I want 配置和管理系统运行参数
So that 可以在不修改代码的情况下调整系统行为

**验收标准**:
```gherkin
Given 系统参数表为空
When 添加参数 file.max_size，值="10485760"，分组="file"，可编辑=true
Then 参数添加成功

Given 参数 file.max_size 已存在
When 修改其值为 "20971520"
Then 更新成功，且该参数值在后续业务中即时生效

Given 参数键 file.max_size 已存在
When 尝试添加同名参数键
Then 返回参数键已存在的错误
```

**可行性**: 可行 | 复杂度: 中 | 风险: 低
**预估工期**: 0.5 天

---

#### US-004: 系统参数热更新
**优先级**: Must Have | **业务价值**: 中 | **用户影响**: 中

As a 系统管理员
I want 修改系统参数后无需重启应用即可生效
So that 系统配置调整更加便捷

**验收标准**:
```gherkin
Given 参数 file.max_size 当前值为 "10485760"
When 通过接口修改参数值为 "20971520"
Then 修改成功，且新值即时生效
```

**可行性**: 可行 | 复杂度: 中 | 风险: 低
**预估工期**: 0.3 天

---

#### US-005: 文件上传
**优先级**: Must Have | **业务价值**: 高 | **用户影响**: 高

As a 用户
I want 上传文件到系统中
So that 知识条目等业务可以关联附件

**验收标准**:
```gherkin
Given 文件 test.pdf (2MB)，内容为 X
When 调用上传接口
Then 返回成功，出参含文件对象 id（= fileId，挂载锚点），
     文件存储到 {base-path}/yyyy/MM/dd/{雪花ID}.pdf，数据库记录一份文件元数据

Given 已上传过内容为 X 的文件（fileId = 1）
When 再次上传另一份内容同为 X 的文件
Then 服务端查重命中，【跳过写盘】且不新增文件记录，返回同一个 fileId = 1

Given 上传超过 max-size 的文件，或扩展名不在 allow-types 白名单内
When 调用上传接口
Then 返回文件大小超限 / 类型不允许错误
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.5 天

---

#### US-006: 文件下载
**优先级**: Must Have | **业务价值**: 中 | **用户影响**: 中

As a 用户
I want 下载或预览已上传的文件
So that 获取文件内容

**验收标准**:
```gherkin
Given 知识条目 K 的附件 A（file_id = 1，attach_name = "我的笔记.pdf"）为有效挂载
When 调用下载接口，传入 id = 1 与 attachId = A
Then 返回文件流，Content-Disposition 的文件名为挂载行的 attach_name（"我的笔记.pdf"）

Given 同一 fileId = 1 被两条知识条目以不同 attach_name 挂载
When 分别以各自的 attachId 下载
Then 各自看到自己挂载时的文件名，互不影响

Given 调用下载接口并带 preview = true
Then Content-Disposition 为 inline（浏览器内预览）

Given 只传 id（不带 attachId），如管理端文件台账入口或上传后立即预览
Then 返回文件流，文件名为 aik_sys_file.original_name（台账视角）
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.3 天

---

#### US-007: 文件删除
**优先级**: Must Have | **业务价值**: 中 | **用户影响**: 中

As a 系统管理员
I want 删除不再被任何业务使用的文件
So that 释放存储空间

**验收标准**:
```gherkin
Given 文件记录 1 仍被某知识条目有效挂载
When 调用文件删除接口，传入 id = 1
Then 返回失败并提示"文件仍被其它挂载点引用，不允许删除"，文件记录与磁盘内容均保留

Given 文件记录 1 是某附件的唯一挂载，且该挂载被卸载
Then 文件记录被物理删除，磁盘文件在事务提交后被删除

Given 文件记录 1 同时被两条知识条目挂载
When 卸载其中一条的挂载
Then 文件记录与磁盘文件均保留，另一条仍可正常下载

Given 文件记录已被删除（或从未存在）
When 再次调用删除接口
Then 幂等返回成功，不抛出异常
```

**可行性**: 可行 | **复杂度**: 中 | **风险**: 中
**预估工期**: 0.3 天

---

#### US-009: 附件挂载管理（通用挂载表）

**优先级**: Must Have | **业务价值**: 高 | **用户影响**: 高

As a 用户
I want 给一条业务（如知识条目）挂载多个附件，并各自命名、说明与排序
So that 相关文件能被组织成有序的附件列表

**验收标准**:
```gherkin
Given 知识条目 K 与已上传的文件 fileId = 1
When 提交 K 的附件列表 [{ fileId: 1, attachName: "第三版修订稿", description: "终稿", sortOrder: 1 }]
Then 挂载成功，详情返回该附件（含挂载行 id、fileId、attachName、description、sortOrder）

Given 附件元素未提供 attachName
When 提交附件列表
Then 返回参数错误（attachName 必填），后端【不得】用文件台账的 original_name 回填

Given 附件元素提供的 fileId 不存在
When 提交附件列表
Then 返回失败（禁止产生悬空挂载）

Given 知识条目 K 已有附件 A，再次提交【完全相同】的附件列表
When 重复保存
Then 两次均成功，挂载行数量与挂载行 id 均不变（差量语义，不得出现唯一键冲突）

Given 知识条目 K 原有附件 A、B，新列表只含 B、C
When 保存
Then A 被卸载（保留挂载行并标记为已卸载）、C 被新增、B 被保留；再挂回 A 时复用原挂载行

Given 通用挂载入口：GET /grimoire/attachment/findByBiz?bizType=&bizId=、POST /save、POST /remove
When 以 bizType = knowledge、bizId = K 查询
Then 返回 K 的有效挂载列表；bizType 不在白名单内时返回失败
```

**可行性**: 可行 | **复杂度**: 中 | **风险**: 中
**预估工期**: 1 天

---

### 3.2 Should Have

#### US-008: 文件列表分页查询
**优先级**: Should Have | **业务价值**: 低 | **用户影响**: 低

As a 系统管理员
I want 查看已上传文件的列表
So that 管理系统中的文件资源

**验收标准**:
```gherkin
Given 已上传多个文件
When 查询文件列表，支持按文件名、上传时间筛选
Then 返回分页结果
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.2 天

---

### 3.3 Won't Have (This Time)

| 用户故事 | 原因 |
|---------|------|
| 操作日志管理 | 个人项目，单用户，操作日志价值低 |
| ~~文件预览~~ | **已移出 Won't Have**：`/grimoire/file/download?id=&preview=true` 已实现（US-006） |
| 文件批量上传/下载 | 超出本次范围 |
| 外部链接型附件（只登记 URL、不含文件对象） | 挂载**必须**引用真实文件对象（`file_id NOT NULL`）；`attach_url` 列已删除（SDD §2.5.7） |
| 客户端 md5 预检（先传摘要探测、命中则整包不传） | 本次只做**服务端复用**（US-005）；需要节省上行带宽时另立变更，且**不得**把 `md5` 加回通用 `FileVo` |
| 挂载/卸载的完整事件时间线 | 卸载保留挂载行但不记录事件历史（`create_time` 始终是最初挂载时间）；需要时另建 append-only 事件表（SDD §2.5.7 登记为 W06） |

---

## 4. 技术可行性评估

### 4.1 总体评估

| 指标 | 数量 |
|------|------|
| 可行故事数 | 9 |
| 部分可行 | 0 |
| 不可行 | 0 |
| 高风险 | 0 |

### 4.2 风险与缓解

| 故事ID | 风险 | 缓解措施 |
|--------|------|---------|
| US-004 | 参数热更新使用缓存，可能存在并发读取不一致 | 使用 `volatile` 或 `ConcurrentHashMap` 缓存参数 |
| US-005 | 大文件上传可能导致内存溢出 | 配置 multipart 分块，限制单文件 10MB、请求 50MB |
| US-005 | md5 复用后 `aik_sys_file.original_name` 只代表**首次上传者**，若被当作显示名会跨用户串名 | 显示名权威归属挂载层 `attach_name`；**禁止**回填 `original_name`（`api-contract.md` §2.5.4） |
| US-006 | 不带 `attachId` 时文件名退回 `original_name` | 属设计内的**台账模式**；挂载层调用必须带 `attachId`（`api-contract.md` §2.5.5） |
| US-007 | 删除时磁盘文件可能已被外部删除；或共享文件被误删 | 删除前判断存在性（不存在只删记录）；**先校验是否仍有有效挂载**，有则拒绝；删盘在事务提交后（最坏只留孤儿文件，可对账清理） |
| US-009 | "全删再全插"式保存会撞 `uk_biz_file` 唯一键（`uk` 不含 `del_flag`） | 强制**差量**语义：仅卸载缺失项、仅新增/复活新增项；重复保存同一列表必须幂等（`api-contract.md` §2.5.2） |
| US-009 | 卸载最后一个挂载即删文件，与并发挂载交叉会误删或在建挂载变悬空 | 写入与删除两条路径**都以 `SELECT ... FOR UPDATE` 先锁文件行**（统一锁序，SDD §2.5.7） |

### 4.3 外部依赖

- `FileStorageConfig`（已存在）
- `BaseEntity`、`ApiResponse`、`PageQuery`（已存在）
- MySQL 8.0 数据库
- Hutool 工具库

---

## 5. 实施建议

### 5.1 推荐实施序列（MoSCoW）

| 优先级 | 用户故事 | 功能 |
|--------|---------|------|
| **Must** | US-001, US-002 | 字典类型 + 字典项管理 |
| **Must** | US-003, US-004 | 系统参数 + 热更新 |
| **Must** | US-005, US-006, US-007 | 文件上传（含 md5 秒传）、下载/预览、删除（引用计数守卫） |
| **Must** | US-009 | 附件挂载管理（通用挂载表 + 差量保存 + 引用计数回收） |
| **Should** | US-008 | 文件列表分页查询 |

### 5.2 推荐开发顺序

1. **第一批**：字典管理（表结构 -> PO -> Mapper -> Service -> Controller）
2. **第二批**：系统参数（表结构 -> PO -> Mapper -> Service -> Controller + 热更新缓存）
3. **第三批**：文件管理（表结构 -> PO -> Mapper -> Service -> Controller + 磁盘操作 + md5 秒传 + 删除引用计数）
4. **第四批**：附件挂载（通用挂载表 `aik_sys_attachment` -> PO -> Mapper -> **统一挂载服务** -> Controller；差量保存 + 唯一删除入口；knowledge 侧挂载读写改道统一服务）

### 5.3 关键里程碑

- Milestone 1: 字典管理模块 CRUD 完成
- Milestone 2: 系统参数模块 + 热更新完成
- Milestone 3: 文件管理模块完成（含秒传与删除守卫）
- Milestone 4: 附件挂载模块完成（两层模型闭合：文件对象 + 通用挂载点）

---

## 6. 附录

### 6.1 数据库表规划

| 表名 | 说明 | 继承 |
|------|------|------|
| `aik_sys_dict_type` | 字典类型 | `BaseEntity` |
| `aik_sys_dict_item` | 字典项 | `BaseEntity` |
| `aik_sys_param` | 系统参数 | `BaseEntity` |
| `aik_sys_file` | 文件记录（**第 1 层：文件对象**；`md5` 内容寻址；**物理删除**，无 `del_flag`；无 `url`） | `BaseEntity` |
| `aik_sys_attachment` | **通用附件挂载表**（**第 2 层：挂载点**；`file_id` + `biz_type` + `biz_id` + `attach_name` + `description` + `sort_order` + `del_flag`；删 `knowledge_id` / `attach_url`；**迁移已落地**） | `BaseEntity`（`del_flag` 在类内以 `@TableLogic` 显式声明，**不得**改用 `BaseLogicEntity`） |

> 表名以 `sql/aik_system_tables.sql` 为准（`4f366df` 于 2026-08-26 统一为 `aik_sys_*` 前缀）。
> `aik_sys_attachment` 由"知识条目附件"改造为**通用挂载表**后**归属 system 模块**（建表语句已于
> 2026-09-20 **迁入** `sql/aik_system_tables.sql`；SDD §2.5.8 / §2.3.4）；表结构与字段权威见 `SDD.md` §2.2 / §2.3。

### 6.2 API 路径规划

| 模块 | 基础路径 |
|------|---------|
| 字典管理 | `/grimoire/dictType`, `/grimoire/dictItem` |
| 系统参数 | `/grimoire/systemParam` |
| 文件管理 | `/grimoire/file` |
| 附件挂载 | `/grimoire/attachment`（`findByBiz` / `save` / `remove`，契约见 `api-contract.md` §2.5） |

### 6.3 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-05-15 | 初始版本 | AI |
| v1.1 | 2026-09-20 | 同步 `4f366df` 表名规范（4 张表），补 `aik_sys_attachment`，补权威范围与契约指针 | AI |
| v1.2 | 2026-09-20 | 附件/文件域需求对齐 SDD v1.2 两层模型：F005~F008 改写、新增 F009/F010；US-005/006/007 验收标准重写、新增 US-009；§3.3 移出「文件预览」并新增 3 条 Won't Have；§4.1/§4.2/§5.1/§5.2/§5.3/§6.1/§6.2 同步；同日附件域 DDL 迁移落地，`aik_sys_attachment` 迁入 `sql/aik_system_tables.sql` | AI |
