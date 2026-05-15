# 产品需求文档 (PRD) — system 模块

> 生成时间: 2026-05-15
> 版本: v1.0
> 模块: `io.aik.steins.grimoire.system`

---

## 1. 项目概述

### 1.1 背景
AikSteinsGrimoire 是个人知识魔典后端项目，采用 Spring Boot 2.7.18 单体架构。`core` 基础设施层已完成，现需开发 `system` 模块作为公共服务层，为 article、category、tag 等业务模块提供基础能力支撑。

### 1.2 目标
开发 system 模块，提供字典管理、系统参数、文件管理三大基础服务，形成可复用的公共服务层。

---

## 2. 功能清单

| ID | 功能描述 | 类别 | 状态 |
|----|---------|------|------|
| F001 | 字典类型管理（编码、名称、描述、状态 CRUD） | core | 已澄清 |
| F002 | 字典项管理（类型编码、项编码、项名称、排序、状态、备注 CRUD） | core | 已澄清 |
| F003 | 系统参数管理（参数键、值、描述、分组、可编辑标志 CRUD） | core | 已澄清 |
| F004 | 系统参数热更新（运行时修改参数值即时生效） | technical | 已澄清 |
| F005 | 文件上传（通用上传，日期分目录，UUID 命名） | core | 已澄清 |
| F006 | 文件下载（流式传输，Content-Disposition） | core | 已澄清 |
| F007 | 文件删除（物理删除文件 + 记录） | core | 已澄清 |
| F008 | 文件列表分页查询 | auxiliary | 已澄清 |

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
So that 文章等可以关联附件

**验收标准**:
```gherkin
Given 文件 test.pdf (2MB)
When 调用上传接口
Then 返回成功，文件存储到 {base-path}/2026/05/15/{uuid}.pdf，数据库记录文件元数据

Given 上传超过 max-size 的文件
When 调用上传接口
Then 返回文件大小超限错误
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.5 天

---

#### US-006: 文件下载
**优先级**: Must Have | **业务价值**: 中 | **用户影响**: 中

As a 用户
I want 下载已上传的文件
So that 获取文件内容

**验收标准**:
```gherkin
Given 文件 test.pdf 已上传成功
When 调用下载接口，传入文件记录 ID
Then 返回文件流，Content-Disposition 为 attachment;filename="test.pdf"
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.3 天

---

#### US-007: 文件删除
**优先级**: Must Have | **业务价值**: 中 | **用户影响**: 中

As a 系统管理员
I want 删除不需要的文件
So that 释放存储空间

**验收标准**:
```gherkin
Given 文件 test.pdf 已存在
When 调用删除接口，传入文件记录 ID
Then 数据库记录被物理删除，磁盘文件也被物理删除
```

**可行性**: 可行 | 复杂度: 低 | 风险: 低
**预估工期**: 0.2 天

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
| 文件预览 | 超出本次范围 |
| 文件批量上传/下载 | 超出本次范围 |

---

## 4. 技术可行性评估

### 4.1 总体评估

| 指标 | 数量 |
|------|------|
| 可行故事数 | 8 |
| 部分可行 | 0 |
| 不可行 | 0 |
| 高风险 | 0 |

### 4.2 风险与缓解

| 故事ID | 风险 | 缓解措施 |
|--------|------|---------|
| US-004 | 参数热更新使用缓存，可能存在并发读取不一致 | 使用 `volatile` 或 `ConcurrentHashMap` 缓存参数 |
| US-005 | 大文件上传可能导致内存溢出 | 配置 multipart 分块，限制单文件 10MB |
| US-007 | 文件删除时磁盘文件可能已被外部删除 | 删除前判断文件是否存在，不存在的只删记录 |

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
| **Must** | US-005, US-006, US-007 | 文件上传、下载、删除 |
| **Should** | US-008 | 文件列表分页查询 |

### 5.2 推荐开发顺序

1. **第一批**：字典管理（表结构 -> PO -> Mapper -> Service -> Controller）
2. **第二批**：系统参数（表结构 -> PO -> Mapper -> Service -> Controller + 热更新缓存）
3. **第三批**：文件管理（表结构 -> PO -> Mapper -> Service -> Controller + 磁盘操作）

### 5.3 关键里程碑

- Milestone 1: 字典管理模块 CRUD 完成
- Milestone 2: 系统参数模块 + 热更新完成
- Milestone 3: 文件管理模块完成

---

## 6. 附录

### 6.1 数据库表规划

| 表名 | 说明 | 继承 |
|------|------|------|
| `aik_dict_type` | 字典类型 | `BaseEntity` |
| `aik_dict_item` | 字典项 | `BaseEntity` |
| `aik_system_param` | 系统参数 | `BaseEntity` |
| `aik_file_record` | 文件记录 | `BaseEntity` |

### 6.2 API 路径规划

| 模块 | 基础路径 |
|------|---------|
| 字典管理 | `/grimoire/dictType`, `/grimoire/dictItem` |
| 系统参数 | `/grimoire/systemParam` |
| 文件管理 | `/grimoire/file` |

### 6.3 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-05-15 | 初始版本 | AI |
