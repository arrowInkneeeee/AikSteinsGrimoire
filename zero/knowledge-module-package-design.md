# Knowledge 模块包目录设计方案（统一化重构版）

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
├── system/                        # 系统管理（保留不变）
├── knowledge/                     # 知识库管理后台（统一化）
│   ├── controller/
│   │   ├── KnowledgeController.java    # 知识条目 CRUD + 聚合查询
│   │   ├── CategoryController.java     # 分类树
│   │   └── TagController.java          # 标签管理
│   ├── service/
│   │   ├── KnowledgeService.java
│   │   ├── CategoryService.java
│   │   ├── TagService.java
│   │   └── impl/
│   ├── mapper/
│   │   ├── KnowledgeMapper.java
│   │   ├── CategoryMapper.java
│   │   ├── KnowledgeTagMapper.java
│   │   └── KnowledgeTagRelationMapper.java
│   └── common/
│       ├── po/
│       │   ├── KnowledgePo.java           # 统一主表
│       │   ├── KnowledgeAttachmentPo.java # 附件表
│       │   ├── CategoryPo.java
│       │   ├── KnowledgeTagPo.java
│       │   └── KnowledgeTagRelationPo.java
│       ├── dto/
│       │   ├── KnowledgeDto.java
│       │   └── KnowledgeQuery.java
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

---

## 3. 数据库表设计

### 3.1 统一知识主表 `aik_knowledge`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| title | VARCHAR(200) | 标题 |
| code | VARCHAR(100) | 编码 |
| type | TINYINT | 1-笔记 2-组件 3-方案 4-片段 |
| summary | VARCHAR(500) | 摘要 |
| content | TEXT | 正文（笔记/代码片段/方案描述） |
| source_project | VARCHAR(200) | 来源项目 |
| source_path | VARCHAR(500) | 来源路径 |
| resource_path | VARCHAR(500) | 资源路径（Java包路径） |
| ext_json | JSON | 扩展字段 |
| category_id | BIGINT | 分类ID |
| status | TINYINT | 1-启用 0-禁用 |

### 3.2 附件表 `aik_knowledge_attachment`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| knowledge_id | BIGINT | 知识条目ID |
| attach_name | VARCHAR(200) | 附件名称 |
| attach_url | VARCHAR(500) | 附件URL/路径 |
| description | VARCHAR(500) | 描述 |
| sort_order | INT | 排序号 |

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
