---
name: aIk-coding-style
description: 当编写基于Spring Boot和MyBatis-Plus的Java后端代码时使用，确保模块间编码风格统一——涵盖目录结构、命名规范、分层架构、API设计、DTO/VO/PO定义和数据库设计标准。参考references/目录获取各主题的详细规范。
type: Reference
version: 1.0.0
---

# Java后端模块编码规范

## 核心原则

本规范适用于Spring Boot + MyBatis-Plus技术栈的Java后端模块开发。

**技术栈：**
- Spring Boot
- MyBatis-Plus
- Lombok
- Hutool工具类

**设计理念：**
- **约定优于配置**：通过统一的编码规范减少团队沟通成本
- **分层清晰**：Controller → Service → Mapper 职责分明，边界明确
- **类型安全优先**：LambdaQueryWrapper 替代字符串拼接，构造器注入替代字段注入
- **可维护性**：规范化的注释、日志、异常处理，让代码自文档化

---

## 项目通用约定

### 响应封装规范

**分层职责：**
- **Service 层**：返回原始数据类型或业务对象
- **Controller 层**：使用统一响应类封装 Service 返回结果

**常见响应封装类：**
不同项目可能使用不同的响应封装类，常见命名包括：
- `ApiResponse<T>`
- `Result<T>`
- `Response<T>`
- `ApiResult<T>`
- `R<T>`

**使用示例：**
```java
// Service 层：返回原始类型
public Page<EntityPo> findPage(QueryDto dto) {
    return resultPage;
}

// Controller 层：封装响应
public ApiResponse<Page<EntityPo>> findPage(QueryDto dto) {
    Page<EntityPo> result = service.findPage(dto);
    return ApiResponse.success(result);
}
```

**注意：** 开发前需确认项目使用的具体响应封装类名称和规范。

### 异常处理规范

**通用原则：**
- 使用项目统一的异常处理机制
- 避免直接抛出原生 `RuntimeException`，优先使用项目自定义异常
- 异常信息应清晰明了，便于问题定位

**常见异常类命名：**
不同项目可能使用不同的异常类，常见命名包括：
- `BusinessException` - 业务异常
- `SystemException` / `SystemRuntimeException` - 系统异常
- `ServiceException` - 服务层异常
- `BizException` - 业务异常（简写）
- `ApiException` - API 异常

**异常枚举/常量：**
部分项目使用枚举或常量定义异常码：
- `ErrorCode.PARAM_ERROR` / `ErrorCodeEnum.PARAM_ERROR`
- `ErrorCode.DATA_NOT_EXIST` / `ErrorCodeEnum.DATA_NOT_EXIST`

**使用示例：**
```java
// 方式一：直接抛出异常（带消息）
throw new BusinessException("参数错误");
throw new BusinessException("数据不存在");

// 方式二：使用异常码（如项目支持）
throw new BusinessException(ErrorCode.PARAM_ERROR);
throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
```

**注意：** 开发前需确认项目使用的具体异常类和异常码规范。

### 依赖注入规范

**推荐使用 `private final` + `@RequiredArgsConstructor`（Lombok 构造器注入）：**

Controller 注入 Service：
```java
private final EntityService entityService;
```

Service 注入 Mapper：
```java
private final EntityMapper entityMapper;
```

**说明：**
- 使用 Lombok 的 `@RequiredArgsConstructor` 自动生成包含所有 `final` 字段的构造函数
- `@RequiredArgsConstructor` 加在 Controller 和 ServiceImpl 类上，Mapper 接口不需要加
- 推荐构造器注入，字段声明为 `private final`，确保不可变性和必要依赖的强制注入
- 对于已有项目中的存量代码，保持现有注入方式不变；自主开发的新模块需遵循此规范

---

## 规范参考索引

各主题的详细规范和代码示例已拆分到 `references/` 目录。在代码生成、审查或设计时，按需加载对应文件：

| # | 参考文件 | 主题 | 适用场景 |
|---|---------|------|---------|
| 1 | [references/01-api-doc-annotations.md](references/01-api-doc-annotations.md) | API 文档注解规范 | 选择 Swagger/SpringDoc 注解方案 |
| 2 | [references/02-module-directory.md](references/02-module-directory.md) | 模块目录结构规范 | 创建新模块、搭建包结构 |
| 3 | [references/03-class-comment.md](references/03-class-comment.md) | 类注释规范 | 所有 Java 类的注释格式 |
| 4 | [references/04-code-comment.md](references/04-code-comment.md) | 代码注释规范 | //note / //anchor 注释规则 |
| 5 | [references/05-logging.md](references/05-logging.md) | 日志规范 | @Slf4j 使用、日志级别选择 |
| 6 | [references/06-constants.md](references/06-constants.md) | 常量规范 | 魔法值处理、常量/枚举存放 |
| 7 | [references/07-entity-po.md](references/07-entity-po.md) | 实体类规范 (PO) | PO 注解组合、继承策略、字段规范 |
| 8 | [references/08-database.md](references/08-database.md) | 数据库规范 | 建表语句、字段长度、索引策略 |
| 9 | [references/09-service-layer.md](references/09-service-layer.md) | Service 层规范 | 接口/实现类继承、CRUD 模板 |
| 10 | [references/10-controller-service-split.md](references/10-controller-service-split.md) | Controller/Service 拆分规范 | 多子功能模块的职责拆分 |
| 11 | [references/11-controller-layer.md](references/11-controller-layer.md) | Controller 层规范 | 请求方式、参数传递、返回值封装 |
| 12 | [references/12-dto-vo.md](references/12-dto-vo.md) | DTO/VO 规范 | DTO/VO/QueryDto 定义和转换 |
| 13 | [references/13-mapper.md](references/13-mapper.md) | Mapper 规范 | SQL 编写原则、API vs XML 选择 |
| 14 | [references/14-common-utils.md](references/14-common-utils.md) | 常用工具类 | Hutool IdUtil/StrUtil/BeanUtil/CollUtil |
| 15 | [references/15-business-logic.md](references/15-business-logic.md) | 业务逻辑规范 | 删除策略、分页、时间处理、Stream/重构 |
| 16 | [references/16-naming.md](references/16-naming.md) | 命名规范 | 类名/方法/变量/包/表名/字段命名 |
| 17 | [references/17-api-doc.md](references/17-api-doc.md) | API 文档规范 | 文档位置、文件名、内容要求 |
| 18 | [references/18-readme.md](references/18-readme.md) | README 规范 | 模块 README 模板和内容结构 |

**使用方式：** 在技能指令中通过 `Read references/{编号}-{主题}.md` 加载所需的详细规范。

---

## 快速检查清单

生成代码后按类别检查：

### 代码风格
- [ ] 类注释使用 -anchor 格式，@author 为 `a I k .`
- [ ] 普通注释用 `//note`，关键注释用 `//anchor`
- [ ] if 必须使用大括号 `{}`
- [ ] 禁止行尾注释
- [ ] 魔法值定义为常量 `private static final` 或放在 `common/constant/`

### 实体类 (PO)
- [ ] **无继承PO**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
- [ ] **有继承PO**：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper = true) + @EqualsAndHashCode(callSuper = true)` + 显式无参构造
- [ ] **绑定数据库**：添加 `@TableName("表名")`
- [ ] 字段使用 `@TableField`，API 文档注解根据项目选择
- [ ] 时间字段使用 `@JsonFormat` + `@DateTimeFormat` 格式化

### 日志规范
- [ ] Controller 和 ServiceImpl 添加 `@Slf4j` 注解
- [ ] 关键信息使用 `log.info()`，异常使用 `log.error()`

### 分层规范
- [ ] Controller/Service 按功能拆分，保持单一职责
- [ ] 数据库实体 Service 接口继承 `IService<PO>`，实现类继承 `ServiceImpl<Mapper, PO>`
- [ ] `ApiResponse` 在 Controller 层封装，Service 层返回原始类型
- [ ] Controller 返回 VO 对象，避免使用 Map
- [ ] 依赖注入使用 `private final` + `@RequiredArgsConstructor`

### DTO/VO 规范
- [ ] **DTO（独立）**：`@Data + @ApiModel`
- [ ] **DTO（继承 PO）**：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`
- [ ] **VO（默认）**：`@Data + @ApiModel`，包含 `of()` 转换方法
- [ ] **VO（复杂）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`
- [ ] **QueryDto**：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`，继承分页基类

### 数据操作
- [ ] 使用项目统一的ID生成方式
- [ ] 使用项目统一的用户信息获取方式
- [ ] 使用 `BeanUtil.copyProperties` 进行对象复制
- [ ] VO 类包含 `of()` 静态方法用于 PO 到 VO 的转换
- [ ] 优先使用 `Wrapper` 构建 SQL 查询条件
- [ ] Mapper 接口添加 `@Mapper` 注解
- [ ] **SQL 实现方式**：优先 MyBatis-Plus API，复杂 SQL 用 XML
- [ ] **不生成空 XML**，需要时才创建
- [ ] 异常使用项目统一的异常类

### 业务逻辑
- [ ] 简单数据处理使用 Stream API，复杂逻辑使用传统循环
- [ ] 类内复用逻辑抽取为 `private` 方法，跨类复用逻辑抽取到 `utils`

### 事务规范
- [ ] **使用 `@Transactional` 前必须经用户确认**，获得明确许可后方可使用
- [ ] **单表单条操作不加 `@Transactional`**（数据库隐式事务已保证原子性）
- [ ] **仅在多表写入、批量操作、跨表依赖写入时加 `@Transactional`**
- [ ] **事务内严禁混入缓存、MQ、文件 IO、RPC 等非事务操作**
- [ ] **竞态条件靠数据库唯一索引解决，不靠 `@Transactional`**

### Controller 规范
- [ ] GET 用于简单查询，POST 用于分页查询/新增/修改/删除/文件上传
- [ ] GET 请求使用 `@RequestParam`，POST 请求使用 `@RequestBody` DTO
- [ ] 文件上传使用 `@RequestParam MultipartFile`

### 模块结构
- [ ] SQL文件放在模块包下的 `sql/` 目录
- [ ] 图片字段 IDs + Names 成对出现（如有）
- [ ] 模块根目录包含 `README.md` 文件
