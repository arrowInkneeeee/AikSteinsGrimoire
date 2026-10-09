---
name: code-implementation
description: 代码实现全流程技能——从SDD生成代码骨架、填充业务逻辑、生成API文档和数据库迁移脚本，最终产出完整可运行代码。适用于"实现代码"、"开发功能"、"写代码"、"根据设计编码"、"代码实施"、"生成代码骨架"、"生成API文档"、"生成迁移脚本"等场景。
type: Skill
version: 1.0.0
trigger:
  - 实现代码
  - 开发功能
  - 写代码
  - 根据设计编码
  - 代码实施
  - 生成代码骨架
  - 生成API文档
  - 生成迁移脚本
  - code-generator
  - code-implementer
  - api-doc-generator
  - db-migration-generator
  - spec-implementer
---

# code-implementation

代码实现全流程技能，覆盖代码骨架生成、业务逻辑填充、API文档生成和数据库迁移脚本生成，最终产出完整可运行代码。

> **重要**：本技能所有代码产出必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md) 规范。

## 核心规范摘要

1. **目录结构**：`common/po/`、`common/dto/`、`common/vo/`、`dao/mapping/`
2. **类注释**：使用 `-anchor` 标记，`@author a I k .`
3. **代码注释**：`//note`（普通）、`//anchor`（关键）
4. **Service Bean**：`{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式
5. **依赖注入**：统一使用 `private final` + `@RequiredArgsConstructor`
6. **PO实体注解**：
   - 无继承：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
   - 有继承：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` + 显式无参构造
7. **DTO/VO注解**：
   - DTO（独立）：`@Data + @ApiModel`
   - DTO（继承PO）：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`
   - VO（默认）：`@Data + @ApiModel`，必须包含 `of()` 方法
   - VO（复杂）：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`
8. **SQL实现方式**：优先 MyBatis-Plus API，复杂 SQL 才用 XML，不生成空 XML
9. **XML位置**：放在 Java源代码目录的 `dao/mapping/` 下，禁止放在 resources 目录
10. **文件编码**：UTF-8 无 BOM

## 子命令

### skeleton

根据SDD生成代码骨架（Entity、Mapper、Service、Controller、DTO、VO、常量类）。

**触发条件**：用户说"生成代码骨架"、"生成基础代码"

**输入**：
- SDD文档（来自 system-design 的输出）

**输出**：
- 代码骨架文件（Controller/Service/Mapper/Entity/DTO/VO/Constant）

**执行步骤**：
1. 解析SDD中的数据库设计和API设计
2. 检查项目已有通用组件（BaseEntity, Result, PageDTO）
3. 生成PO实体类（XxPo，放在 common/po/）
4. 生成Mapper接口（继承BaseMapper）
5. 生成Service接口和实现类骨架
6. 生成Controller骨架
7. 生成DTO类（XxDto，放在 common/dto/）
8. 生成VO类（XxVo，放在 common/vo/，含of()方法）
9. 生成常量类（CacheKeyConstant, ErrorCodeConstant，放在 common/constant/）
10. 所有类使用 `-anchor` 类注释，`@author a I k .`

**人机协作节点**（如项目缺少通用组件）：
- 询问是否创建全局异常处理类
- 询问是否创建配置类（RedisConfig/RabbitConfig等）

---

### implement

填充业务逻辑，实现完整功能。

**触发条件**：用户说"实现业务逻辑"、"填充代码"、"写代码"

**输入**：
- SDD文档
- 代码骨架（来自 skeleton 子命令或已有代码）

**输出**：
- 完整业务逻辑代码（Service实现类、Controller方法）

**执行步骤**：
1. 根据SDD中的流程设计，实现Service层业务逻辑
2. 实现事务管理（@Transactional，标注传播行为）
3. 实现缓存逻辑（如有技术选型）
4. 实现消息队列逻辑（如有技术选型）
5. 实现Controller层参数校验和调用
6. 遵循 aIk-coding-style 注释规范（//note, //anchor）
7. 确保代码符合 Java 8 编码规范

---

### api-doc

生成Markdown格式的API接口文档。

**触发条件**：用户说"生成API文档"、"生成接口文档"

**输入**：
- SDD文档中的API设计部分
- 或已有Controller代码

**输出**：
- API文档（Markdown格式），使用 `assets/api-doc-template.md` 模板

**执行步骤**：
1. 解析Controller中的接口定义
2. 提取请求参数（Query/Path/Body）
3. 提取响应结构（VO）
4. 生成请求/响应示例
5. 使用 `assets/api-doc-template.md` 模板输出
6. 保存至 `docs/api/` 目录

---

### db-migration

生成数据库迁移脚本（DDL）。

**触发条件**：用户说"生成数据库脚本"、"生成迁移脚本"、"建表SQL"

**输入**：
- SDD文档中的数据库设计部分
- 数据库类型（MySQL/PostgreSQL/Oracle）

**输出**：
- SQL迁移脚本文件

**执行步骤**：
1. 解析SDD中的表结构设计
2. 生成建表SQL（CREATE TABLE）
3. 生成索引SQL（CREATE INDEX）
4. 生成注释SQL（COMMENT）
5. 按版本号命名（V1.0.0__create_xxx_table.sql）
6. 保存至 `db/migration/` 或 `sql/` 目录

**人机协作节点**（如未明确数据库类型）：
- 自动检测：`grep -E "mysql|postgresql|oracle" pom.xml`
- 如检测失败，询问用户确认数据库类型

---

### full

从SDD到完整可运行代码的全流程。

**触发条件**：用户说"完整实现"、"从设计到代码"、"实现所有功能"

**输入**：
- SDD文档（来自 system-design 的输出）

**输出**：
- 完整代码结构 + API文档 + 数据库脚本 + README.md

**执行步骤**：
1. 检查项目已有组件（BaseEntity, Result, PageDTO, 全局异常处理, 配置类）
2. **【人机协作】** 确认缺失组件是否需要创建
3. 执行 `skeleton` → 生成代码骨架
4. 执行 `implement` → 填充业务逻辑
5. 执行 `db-migration` → 生成数据库脚本
6. 执行 `api-doc` → 生成API文档
7. 生成 README.md（模块说明、技术栈、目录结构、接口列表）
8. 输出完整代码结构

**输出结构**：
```
生成代码/
├── src/main/java/com/xxx/module/
│   ├── controller/
│   ├── service/
│   │   └── impl/
│   ├── dao/
│   │   └── mapping/          # XML在Java源码目录
│   ├── common/
│   │   ├── po/               # XxPo
│   │   ├── dto/              # XxDto
│   │   ├── vo/               # XxVo
│   │   ├── constant/         # XxConstant
│   │   ├── enums/
│   │   └── utils/            # XxUtil
├── src/test/java/com/xxx/module/
│   └── service/
│       └── XxServiceTest.java
├── docs/api/
│   └── xx-api.md
└── README.md
```

---

## 输入契约

| 输入项 | 路径/来源 | 必须 | 降级策略 |
|--------|----------|------|---------|
| SDD文档 | `docs/sdd/` 或用户粘贴 | 是 | 无，无SDD则建议先执行 system-design |
| 数据库类型 | pom.xml 或用户指定 | 否 | 默认 MySQL |
| 项目通用组件 | 项目源码 | 否 | 如缺失则询问是否创建 |

## 输出契约

| 输出项 | 路径 | 格式 |
|--------|------|------|
| Java代码 | `src/main/java/com/xxx/module/` | Java源文件 |
| Mapper XML | `src/main/java/com/xxx/module/dao/mapping/` | XML文件 |
| API文档 | `docs/api/xx-api.md` | Markdown |
| 数据库脚本 | `db/migration/` 或 `sql/` | SQL文件 |
| README | 模块根目录 | Markdown |

## 工作流上下文

**上游技能**：
- `system-design`：产出SDD文档

**下游技能**：
- `code-review`：对生成的代码进行审查
- `testing`：对生成的代码进行测试

## 质量门禁

| 检查点 | 条件 | 动作 |
|--------|------|------|
| 通用组件检查 | 已确认 BaseEntity/Result/PageDTO 存在 | 继续 |
| 编码规范 | 所有类使用 -anchor 注释 | 继续 |
| 文件编码 | UTF-8 无 BOM | 继续 |

## 约束

- 所有代码必须严格遵循 `aIk-coding-style` 规范
- 所有类使用 `-anchor` 类注释，`@author a I k .`
- 代码注释使用 `//note` 和 `//anchor` 标记
- Service Bean名称格式：`{module}.{ServiceName}`
- 注入统一使用 `private final` + `@RequiredArgsConstructor`
- XML文件放在 Java源代码目录的 `dao/mapping/` 下，禁止放在 resources
- 所有文件使用 UTF-8 无 BOM 编码
- 优先复用项目已有组件
