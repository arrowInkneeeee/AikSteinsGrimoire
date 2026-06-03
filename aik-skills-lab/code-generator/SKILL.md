---
name: code-generator
description: 根据 SDD 生成代码骨架，严格遵循 aIk-coding-style 规范
type: Skill
version: 1.0.0
---

# code-generator

> **重要**：本技能生成代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范。

## 核心规范引用

生成代码前必须阅读并遵循以下规范：

1. **目录结构**：`common/po/`、`common/dto/`、`common/vo/`、`common/constant/`、`dao/mapping/`
2. **PO实体**：根据场景选择注解
   - 无继承：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
   - 有继承：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper = true) + @EqualsAndHashCode(callSuper = true)` + 显式无参构造
3. **类注释**：使用 `-anchor` 标记的固定格式
4. **Service Bean**：`@Service("{module}.{ServiceName}")` 或 `@Service("{module}.{subModule}.{ServiceName}")`
5. **依赖注入**：统一使用 `private final` + `@RequiredArgsConstructor`
6. **XML生成规则**：
   - 优先使用 MyBatis-Plus API，不生成空 XML
   - 复杂 SQL 才需要 XML，放在 `dao/mapping/` 目录下

## 输入

- **SDD**: 系统设计文档（架构设计 + 数据库设计 + 接口设计）

## 输出

代码骨架（Entity/Mapper/Service/Controller/DTO/VO/常量类）

## 工作流

1. **解析 SDD 数据库设计**
   - 提取表结构
   - 生成 Entity 类

2. **生成 Mapper 接口和 XML**
   - 继承 BaseMapper
   - 复杂 SQL 生成 XML 文件框架

3. **生成 Service 接口和实现**
   - 接口继承 IService
   - 实现类继承 ServiceImpl

4. **生成 Controller、DTO、VO**
   - 根据接口设计生成
   - DTO 添加 JSR-303 校验

5. **生成常量类**
   - CacheKey
   - ErrorCode

6. **检查项目已有组件**
   - 复用 BaseEntity、Result 等

## Entity 生成规范

**PO实体命名**：`XxPo`（如 `OrderPo`）

```java
package {package}.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * -anchor {实体描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Data
@SuperBuilder
@ToString(callSuper = true)
@ApiModel("{实体描述}")
@TableName("{table_name}")
public class {Entity}Po {

    /**
     * 主键ID
     */
    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 业务字段
     */
    @ApiModelProperty("业务字段描述")
    @TableField("field_name")
    private String fieldName;

    /**
     * 创建人id
     */
    @ApiModelProperty("创建人id")
    @TableField("create_user_id")
    private Long createUserId;

    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    @TableField("create_user")
    private String createUser;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
    @TableField("modify_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifyTime;
}
```

**PO 注解规范（分场景）**：

| 场景 | 核心注解 | 说明 |
|------|---------|------|
| **无继承** | `@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor` | 最常用的场景 |
| **有继承** | `@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` | 继承 BasePo 等父类 |

**有继承 PO 必须显式定义无参构造：**
```java
public {Entity}Po() {
    super();
}
```

**规范说明**：
- 类名使用 `XxPo` 后缀（如 `OrderPo`）
- 位置：`common/po/` 目录下
- 绑定数据库时添加 `@TableName("表名")`
- API文档注解根据项目选择：`@ApiModel`（Swagger）或 `@Schema`（SpringDoc）
- 时间字段使用 `@JsonFormat + @DateTimeFormat` 格式化
- 数据库字段下划线命名，Java属性驼峰命名，使用 `@TableField` 显式映射

## Mapper 生成规范

**Mapper命名**：`XxMapper`（如 `OrderMapper`）

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

    /**
     * -anchor 查询订单统计
     */
    List<OrderStatisticsVo> selectStatistics(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
```

**规范说明**：
- 类名使用 `XxMapper` 后缀
- 继承 `BaseMapper<XxPo>`
- 方法注释使用 `-anchor` 标记重要方法

**XML 生成**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{package}.dao.{Entity}Mapper">

    <select id="selectStatistics" resultType="{package}.common.vo.{Entity}StatisticsVo">
        SELECT
            DATE_FORMAT(create_time, '%Y-%m-%d') as date,
            COUNT(*) as orderCount,
            SUM(total_amount) as totalAmount
        FROM {table_name}
        WHERE create_time BETWEEN #{startDate} AND #{endDate}
        GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
    </select>

</mapper>
```

**XML 生成规则**：
- **不生成空 XML 文件** - 优先使用 MyBatis-Plus API
- 仅在复杂 SQL（多表关联、动态 SQL）时才生成 XML
- XML文件名使用 `XxMapping.xml`（如 `OrderMapping.xml`）
- **必须放在 `dao/mapping/` 目录下（Java源码目录，非resources）**
- resultType使用Vo类（如 `OrderStatisticsVo`）
- SQL使用小写字段名（与数据库一致）

## Service 生成规范

**接口**：
```java
/**
 * -anchor
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/3/19 16:18
 * -
 **/
public interface OrderService extends IService<OrderPo> {

    /**
     * -anchor 创建订单
     */
    OrderVo createOrder(OrderCreateDto dto);

    /**
     * -anchor 根据ID查询订单
     */
    OrderVo getById(Long id);

    /**
     * -anchor 分页查询订单列表
     */
    PageResult<OrderVo> list(OrderQueryDto dto);

    /**
     * -anchor 取消订单
     */
    boolean cancelOrder(Long orderId);
}
```

**实现类**：
```java
package {package}.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * -anchor {Service实现类描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Slf4j
@RequiredArgsConstructor
@Service("{module}.{Entity}Service")
public class {Entity}ServiceImpl extends ServiceImpl<{Entity}Mapper, {Entity}Po>
        implements {Entity}Service {

    //note 默认页码
    private static final int DEFAULT_PAGE = 1;
    
    //note 最大分页大小
    private static final int MAX_PAGE_SIZE = 100;

    private final {Entity}Mapper {entity}Mapper;

    @Override
    public {Entity}Vo createOrder({Entity}CreateDto dto) {
        //anchor TODO: 待实现
        log.info("创建订单，参数：{}", dto);
        throw new UnsupportedOperationException("待实现");
    }

    @Override
    public {Entity}Vo getById(Long id) {
        //anchor TODO: 待实现
        throw new UnsupportedOperationException("待实现");
    }

    @Override
    public Page<{Entity}Vo> findPage({Entity}QueryDto dto) {
        //anchor TODO: 待实现
        throw new UnsupportedOperationException("待实现");
    }

    @Override
    public Boolean remove(Long id) {
        //anchor TODO: 待实现
        throw new UnsupportedOperationException("待实现");
    }
}
```

**规范说明**：
- 接口和实现类都使用 `-anchor` 类注释
- 方法注释使用 `-anchor` 标记
- **Service Bean名称必须使用 `{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式**（如 `@Service("order.OrderService")`）
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- 返回类型使用 `XxVo`，入参使用 `XxDto`
- Controller层使用 `ApiResponse` 封装，Service层返回原始类型

## Controller 生成规范

```java
package {package}.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * -anchor {Controller描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Slf4j
@RequiredArgsConstructor
//note 老项目使用：@Api(tags = "{模块}管理")
//note 新项目使用：@Tag(name = "{模块}管理")
@RestController
@RequestMapping("/{主模块}/{功能}")
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    //note POST 分页查询，使用 DTO 传参支持查询条件扩展
    //note 老项目使用：@ApiOperation("分页查询{Entity}")
    //note 新项目使用：@Operation(summary = "分页查询{Entity}")
    @PostMapping("/findPage")
    public ApiResponse<Page<{Entity}Vo>> findPage(@RequestBody {Entity}QueryDto queryDto) {
        log.info("分页查询{Entity}，参数：{}", queryDto);
        Page<{Entity}Vo> result = {entity}Service.findPage(queryDto);
        return ApiResponse.success(result);
    }

    //note GET 查询详情
    //note 老项目使用：@ApiOperation("查询{Entity}详情")
    //note 新项目使用：@Operation(summary = "查询{Entity}详情")
    @GetMapping("/findById")
    public ApiResponse<{Entity}Vo> findById(@RequestParam Long id) {
        log.info("查询{Entity}详情，ID：{}", id);
        {Entity}Vo result = {entity}Service.findById(id);
        return ApiResponse.success(result);
    }

    //note POST 新增数据，DTO 传参
    //note 老项目使用：@ApiOperation("新增{Entity}")
    //note 新项目使用：@Operation(summary = "新增{Entity}")
    @PostMapping("/add")
    public ApiResponse<{Entity}Vo> add(@RequestBody {Entity}Dto dto) {
        log.info("新增{Entity}，参数：{}", dto);
        {Entity}Vo result = {entity}Service.add(dto);
        return ApiResponse.success(result);
    }

    //note POST 修改数据，DTO 传参
    //note 老项目使用：@ApiOperation("修改{Entity}")
    //note 新项目使用：@Operation(summary = "修改{Entity}")
    @PostMapping("/modify")
    public ApiResponse<{Entity}Vo> modify(@RequestBody {Entity}Dto dto) {
        log.info("修改{Entity}，参数：{}", dto);
        {Entity}Vo result = {entity}Service.modify(dto);
        return ApiResponse.success(result);
    }

    //note POST 删除数据
    //note 老项目使用：@ApiOperation("删除{Entity}")
    //note 新项目使用：@Operation(summary = "删除{Entity}")
    @PostMapping("/remove")
    public ApiResponse<Boolean> remove(@RequestParam Long id) {
        log.info("删除{Entity}，ID：{}", id);
        Boolean result = {entity}Service.remove(id);
        return ApiResponse.success(result);
    }
}
```

**规范说明**：
- 使用 `-anchor` 类注释
- 方法注释使用 `-anchor` 标记
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **请求路径格式：`/{主模块}/{功能}`**（如 `/sample/history`、`/xray/detection`）
- **请求方式**：
  - GET：用于简单查询（如根据 ID 查询详情）
  - POST：用于分页查询、新增、修改、删除、文件上传等操作
- **参数传递**：
  - GET 请求：使用 `@RequestParam`
  - POST 请求：使用 `@RequestBody` DTO（文件上传除外）
- **不使用 `/api` 前缀**
- **Controller 层使用 `ApiResponse` 封装 Service 返回结果**

## DTO 生成规范

**DTO命名**：`XxDto`（如 `OrderCreateDto`）

```java
package {package}.common.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * -anchor {DTO描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class {Entity}Dto extends {Entity}Po {

    @ApiModelProperty("查询关键字")
    private String keyword;
}

/**
 * -anchor {QueryDto描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class {Entity}QueryDto extends BaseQueryCondition {

    @ApiModelProperty("查询关键字")
    private String keyword;

    @ApiModelProperty("字段筛选")
    private String fieldName;
}
```

**DTO 注解规范（分场景）**：

| 场景 | 注解 | 说明 |
|------|------|------|
| **独立 DTO** | `@Data + @ApiModel` | 不继承任何类 |
| **继承 PO** | `@Data + @EqualsAndHashCode(callSuper = true) + @ApiModel` | 复用 PO 字段 |

**规范说明**：
- 类名使用 `XxDto` 后缀（如 `OrderCreateDto`）
- **位置：`common/dto/` 目录下**
- 使用 `-anchor` 类注释
- DTO继承PO时必须加 `@EqualsAndHashCode(callSuper = true)`
- `QueryDto`继承`BaseQueryCondition`

## VO 生成规范

**VO命名**：`XxVo`（如 `OrderVo`）

```java
package {package}.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * -anchor {VO描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Data
@ApiModel("{VO描述}")
public class {Entity}Vo {

    @ApiModelProperty("字段描述")
    private String fieldName;
}
```

**VO 注解规范（分场景）**：

| 场景 | 注解 | 说明 |
|------|------|------|
| **简单 VO（默认）** | `@Data + @ApiModel` | 通过 `of()` 方法从 PO 转换 |
| **复杂 VO** | `@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel` | 多数据源组装 |

**VO 必须包含 `of()` 转换方法：**
```java
public static {Entity}Vo of({Entity}Po po) {
    if (po == null) {
        return null;
    }
    {Entity}Vo vo = new {Entity}Vo();
    BeanUtil.copyProperties(po, vo);
    return vo;
}
```

**规范说明**：
- 类名使用 `XxVo` 后缀（如 `OrderVo`）
- **位置：`common/vo/` 目录下**
- 使用 `-anchor` 类注释
- API文档注解根据项目选择

## 常量类生成规范

**常量类命名**：`XxConstant`（如 `CacheKeyConstant`、`ErrorCodeConstant`）

**单模块项目**：
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
// com.xxx.module.common.constant.CacheKeyConstant
public final class CacheKeyConstant {

    private CacheKeyConstant() {}

    public static final String ORDER = "order:";
    public static final String USER = "user:";
    public static final String PRODUCT = "product:";

    public static String orderKey(Long orderId) {
        return ORDER + orderId;
    }

    public static String userKey(Long userId) {
        return USER + userId;
    }

    public static String productKey(Long productId) {
        return PRODUCT + productId;
    }
}

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
// com.xxx.module.common.constant.ErrorCodeConstant
public final class ErrorCodeConstant {

    private ErrorCodeConstant() {}

    public static final int SUCCESS = 200;
    public static final int PARAM_ERROR = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SYSTEM_ERROR = 500;

    // 业务错误码
    public static final int ORDER_NOT_FOUND = 1001;
    public static final int STOCK_NOT_ENOUGH = 1002;
    public static final int ORDER_STATUS_ERROR = 1003;
    public static final int PRODUCT_NOT_FOUND = 1004;
}
```

**多模块项目**（放在 common 模块）：
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
// com.xxx.common.constant.CacheKeyConstant
public final class CacheKeyConstant {
    // ...
}
```

**规范说明**：
- 类名使用 `XxConstant` 后缀
- 使用 `-anchor` 类注释
- 私有构造方法防止实例化

## 项目已有组件检查

生成代码前检查：

```bash
# 检查 BaseEntity
grep -r "class BaseEntity" --include="*.java" src/

# 检查 Result
grep -r "class Result" --include="*.java" src/

# 检查 PageDTO
grep -r "class PageDTO" --include="*.java" src/
```

**复用原则**：
- 如项目已有 BaseEntity，生成的 Entity 继承它
- 如项目已有 Result，Controller 返回它
- 如项目已有 PageDTO，DTO 继承它
- 如项目无，询问用户是否创建

## 输出示例

```
生成代码/
├── src/
│   └── main/
│       └── java/
│           └── com/xxx/module/
│               ├── controller/
│               │   └── OrderController.java
│               ├── service/
│               │   ├── OrderService.java
│               │   └── impl/
│               │       └── OrderServiceImpl.java
│               ├── dao/
│               │   └── mapping/
│               │       └── OrderMapping.xml
│               ├── entity/
│               │   └── OrderPo.java
│               ├── dto/
│               │   ├── OrderCreateDto.java
│               │   ├── OrderItemDto.java
│               │   └── OrderQueryDto.java
│               ├── vo/
│               │   ├── OrderVo.java
│               │   └── OrderItemVo.java
│               └── common/
│                   ├── constant/
│                   │   ├── CacheKeyConstant.java
│                   │   └── ErrorCodeConstant.java
│                   └── utils/
│                       └── OrderUtil.java
└── src/
    └── main/
        └── resources/
            └── dao/
                └── mapping/
                    └── OrderMapping.xml
```

**目录结构规范**：
- `common/constant/` - 常量类
- `common/dto/` - DTO类
- `common/po/` - PO实体类
- `common/vo/` - VO视图对象
- `common/enums/` - 枚举类
- `common/utils/` - 工具类
- `controller/` - 控制器
- `dao/mapping/` - Mapper XML文件（Java源码目录）
- `service/` - Service接口
- `service/impl/` - Service实现类
- `sql/` - 数据库脚本

## 命名规范汇总

| 类型 | 命名格式 | 示例 | 位置 |
|------|----------|------|------|
| PO实体 | XxPo | OrderPo | common/po/ |
| DTO | XxDto | OrderCreateDto | common/dto/ |
| VO | XxVo | OrderVo | common/vo/ |
| 常量类 | XxConstant | CacheKeyConstant | common/constant/ |
| 工具类 | XxUtil | OrderUtil | common/utils/ |
| Controller | XxController | OrderController | controller/ |
| Service接口 | XxService | OrderService | service/ |
| Service实现 | XxServiceImpl | OrderServiceImpl | service/impl/ |
| Mapper | XxMapper | OrderMapper | dao/ |
| XML | XxMapping.xml | OrderMapping.xml | dao/mapping/ |

## 代码注释规范

### 类注释模板（必须严格遵循）
```java
/**
 * -anchor {类描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
```

### 行注释规范
| 注释类型 | 格式 | 使用场景 |
|---------|------|---------|
| 普通注释 | `//note {内容}` | 一般代码说明 |
| 关键注释 | `//anchor {内容}` | 关键逻辑、重要业务点 |

**规则**：
- **禁止行尾注释**：注释必须单独一行
- **if必须使用大括号**：即使单行也要用 `{}`
- **关键代码必须有注释**

## 注意事项

- **所有代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范**
- 所有代码使用 Java 8 语法
- 使用 Lombok 注解简化代码
- 复用项目已有组件，不重复创建
- 生成的 Service 方法抛出 UnsupportedOperationException，待实现
- 复杂 SQL 生成 XML 框架，具体 SQL 待填充
- 常量类位置根据项目结构（单模块/多模块）决定
- **所有类必须使用 `-anchor` 类注释模板，@author 固定为 `a I k .`**
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **Service Bean名称格式：`{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}`（如 `@Service("order.OrderService")`）**
- **PO实体注解（无继承）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
- **PO实体注解（有继承）**：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` + 显式无参构造
- **DTO（独立）**：`@Data + @ApiModel`
- **DTO（继承PO）**：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`
- **VO（默认）**：`@Data + @ApiModel`，必须包含 `of()` 转换方法
- **VO（复杂）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`
- **XML生成规则**：不生成空 XML，复杂 SQL 才需要，放在 `dao/mapping/` 目录
- **代码注释使用 `//note` 和 `//anchor` 标记**
- **禁止行尾注释，if必须使用大括号**
