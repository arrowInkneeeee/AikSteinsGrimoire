---
name: architecture-designer
description: 当需要为Java Spring Boot模块设计分层架构，定义包结构、各层职责和组件交互规则，并识别可复用的通用组件时使用。适用于"设计分层架构"、"包结构设计"、"分层设计"、"项目架构设计"等场景。
type: Skill
version: 1.0.0
---

# architecture-designer

## 输入

- **PRD**: 需求分析阶段输出的产品需求文档
- **技术栈信息**: 项目使用的技术栈（Java 8 + Spring Boot + MyBatis-Plus + Lombok）

## 输出

架构设计文档

## 工作流

1. **检查项目现有结构**
   - 查看已有包结构（controller/service/mapper 等）
   - 检查 common 包内容及内部结构
   - 检查是否有 BaseEntity、Result 等通用组件
   - 记录项目现有规范

2. **分析 PRD 功能模块**
   - 识别核心业务模块
   - 确定子系统划分
   - 识别模块间依赖关系

3. **设计包结构**
   - 遵循项目已有规范
   - 标准分层：controller/service/mapper/entity/do/dto/vo
   - common 包细分：base/config/constant/enums/exception/result/util

4. **定义各层职责和交互规则**
   - Controller：接收请求、参数校验、调用 Service
   - Service：业务逻辑编排、事务控制
   - Mapper：数据访问
   - Entity：带 @TableName 的数据库实体
   - DO：无数据库注解的领域对象
   - DTO：入参对象
   - VO：出参对象

5. **复用通用组件**
   - Result：统一返回结果（必须复用项目已有）
   - PageResult/PageDTO：分页参数（必须复用项目已有）
   - BaseEntity：基础实体（优先复用）
   - BusinessException：业务异常（优先复用）
   - 工具类：DateUtil、JsonUtil 等（优先复用）

6. **输出架构设计文档**

## 分层规范

```
src/main/java/com/xxx/
├── controller/          # Controller 层，处理 HTTP 请求
├── service/             # Service 接口层
│   └── impl/            # Service 实现层
├── dao/                 # 数据访问层
│   └── mapping/         # Mapper XML文件
├── entity/              # 数据库实体类（带 @TableName/@TableField 注解）- XxPo
├── dto/                 # 数据传输对象（入参）- XxDto
├── vo/                  # 视图对象（出参）- XxVo
└── common/              # 公共组件
    ├── config/          # 配置类
    ├── constant/        # 常量定义 - XxConstant
    ├── dto/             # 公共DTO
    ├── entity/          # 公共PO实体
    ├── utils/           # 工具类 - XxUtil
    └── base/            # 基础类（BaseEntity, BaseController 等）
```

**命名规范**：
- PO实体：`XxPo`（如 `OrderPo`）
- DTO：`XxDto`（如 `OrderCreateDto`）
- VO：`XxVo`（如 `OrderVo`）
- 常量类：`XxConstant`（如 `CacheKeyConstant`）
- 工具类：`XxUtil`（如 `OrderUtil`）
- XML：`XxMapping.xml`（如 `OrderMapping.xml`）

## Service 层规范

- Service 必须定义接口（`XxxService`）
- 实现类放在 `service.impl` 包下（`XxxServiceImpl`）
- 与数据库 PO 直接绑定的 Service 必须继承 `IService<T>`
- 实现类同时实现 `ServiceImpl<XxxMapper, XxxEntity>` 和自定义接口

```java
public interface OrderService extends IService<OrderEntity> {
    OrderVO createOrder(OrderCreateDTO dto);
}

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> 
        implements OrderService {
    
    @Override
    public OrderVO createOrder(OrderCreateDTO dto) {
        // 业务逻辑
    }
}
```

## Mapper 层规范

- Mapper 必须定义接口（`XxMapper`）
- 继承 `BaseMapper<XxPo>` 使用 MyBatis-Plus API
- 复杂 SQL 在 XML 中实现
- XML文件放在 `dao/mapping/` 目录，命名 `XxMapping.xml`

```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:24
 * -
 **/
@Mapper
public interface OrderMapper extends BaseMapper<OrderPo> {
    // 复杂查询在 XML 中实现
}
```

## Entity (PO) 规范

- **PO**：带 MyBatis-Plus 注解（`@TableName`、`@TableField` 等），与数据库表直接映射
- 命名：`XxPo`（如 `OrderPo`）
- 使用 `-anchor` 类注释
- 数据库字段全大写，使用 `@TableField` 注解映射

```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:09
 * -
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("T_ORDER")
public class OrderPo extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ORDER_NO")
    private String orderNo;

    @TableField("STATUS")
    private Integer status;
}
```

## Common 包规范

- `common.config`：RedisConfig、MybatisPlusConfig 等 - **复用项目已有**
- `common.constant`：业务常量、缓存 Key 常量 - `XxConstant`
- `common.enums`：枚举类
- `common.exception`：BusinessException、GlobalExceptionHandler - **复用项目已有**
- `common.result`：统一返回结果类（Result、PageResult）- **必须复用项目已有**
- `common.utils`：工具类（DateUtil、JsonUtil 等）- **复用项目已有**，命名 `XxUtil`
- `common.base`：基础实体（BaseEntity）、基础 Controller（BaseController）- **复用项目已有**
- `common.dto`：公共DTO类 - `XxDto`
- `common.entity`：公共PO实体

## 设计原则

1. **优先复用**：检查项目已有 common 包、util 类、配置类
2. **遵循习惯**：遵循项目现有命名规范和包结构
3. **不重复造轮子**：全新空项目时才创建新的通用组件

## 项目检查清单（设计前必须执行）

- [ ] 检查 common/constant 包是否有 CacheKey、ResultCode 等
- [ ] 检查 common/util 包已有工具类，不重复创建
- [ ] 检查 common/enums 包已有枚举
- [ ] 检查 config 包已有配置类，不重复配置
- [ ] 检查 exception 包已有异常体系
- [ ] 检查 pom.xml 已有依赖，推荐方案时优先匹配
- [ ] 检查项目是否多模块结构

## 输出格式

```markdown
## 架构设计

### 1.1 技术栈
- Java 8
- Spring Boot 2.7.x
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 6.x
- Lombok

### 1.2 包结构
```
com.xxx.module
├── controller
├── service
│   └── impl
├── mapper
├── entity
├── do
├── dto
├── vo
├── converter
└── common
    ├── base
    ├── config
    ├── constant
    ├── enums
    ├── exception
    ├── result
    └── util
```

### 1.3 分层职责
| 层 | 职责 | 规范 |
|---|------|------|
| Controller | 接收请求、参数校验、调用 Service | 禁止业务逻辑 |
| Service | 业务逻辑编排、事务控制 | 必须定义接口，继承 IService |
| Mapper | 数据访问 | 继承 BaseMapper |
| Entity | 数据库实体 | 带 @TableName 注解 |
| DO | 领域对象 | 无数据库注解 |
| DTO | 入参对象 | 使用 JSR-303 校验 |
| VO | 出参对象 | 仅包含展示字段 |
| Converter | 对象转换 | 实施时决定 |

### 1.4 通用组件（复用项目已有）
- **BaseEntity**：基础实体（审计字段、逻辑删除）
- **Result**：统一返回结果
- **PageResult/PageDTO**：分页参数
- **BusinessException**：业务异常
- **GlobalExceptionHandler**：全局异常处理
- **工具类**：DateUtil、JsonUtil 等

### 1.5 模块划分
[根据 PRD 功能模块划分子系统]

### 1.6 项目结构说明
- [ ] 单模块 / 多模块（参考项目习惯）
- [ ] 已有组件复用情况
```

## 注意事项

- 多模块项目参考项目已有习惯，新项目实施时询问
- 所有通用组件优先复用，不重复创建
- 遵循项目现有命名规范（如 Result 的 code 字段类型、分页参数名等）
