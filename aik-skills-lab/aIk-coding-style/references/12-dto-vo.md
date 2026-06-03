# DTO/VO规范

> 来源：aIk-coding-style 规范

## 12.1 数据对象分类

| 对象类型 | 用途 | 继承关系 | 典型场景 |
|---------|------|---------|---------|
| **DTO** | 数据传输 | 可继承 PO | 新增/修改参数、查询条件 |
| **VO** | 视图展示 | 独立类 | 返回给前端的数据 |
| **QueryDto** | 查询条件 | 继承分页基类 | 分页查询参数 |

## 12.2 DTO 规范

DTO 用于数据传输，通常作为接口的入参。

### DTO 注解使用规范

| 场景 | 注解 | 说明 |
|------|------|------|
| **独立 DTO** | `@Data` | 不继承任何类 |
| **继承 PO 的 DTO** | `@Data + @EqualsAndHashCode(callSuper = true)` | 继承 PO 字段 |
| **需要 Builder 的 DTO** | 根据 PO 类型选择 `@Builder` 或 `@SuperBuilder` | 手动构建场景 |

### 场景一：独立 DTO（不继承）

```java
package {package}.common.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

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
@ApiModel("{DTO描述}")
public class {Entity}Dto {

    @ApiModelProperty("业务字段")
    private String fieldName;

    @ApiModelProperty("状态")
    private Integer status;
}
```

### 场景二：继承 PO 的 DTO

复用 PO 字段，添加额外传输字段。

```java
package {package}.common.dto;

import io.swagger.annotations.ApiModel;
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
@ApiModel("{DTO描述}")
public class {Entity}Dto extends {Entity}Po {

    @ApiModelProperty("额外字段，非数据库字段")
    private String extraField;
}
```

**注意：**
- 继承 PO 时必须加 `@EqualsAndHashCode(callSuper = true)`
- 如果 PO 使用 `@SuperBuilder`，DTO 也需要 `@SuperBuilder`

### 场景三：QueryDto（查询条件）

```java
package {package}.common.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@ApiModel("{QueryDto描述}")
public class {Entity}QueryDto extends BaseQueryCondition {

    @ApiModelProperty("查询关键字")
    private String keyword;

    @ApiModelProperty("状态筛选")
    private Integer status;

    @ApiModelProperty("开始日期")
    private LocalDate startDate;

    @ApiModelProperty("结束日期")
    private LocalDate endDate;
}
```

## 12.3 VO 规范

VO 用于视图展示，通常是 Controller 返回给前端的数据结构。

### VO 注解使用规范

| 场景 | 注解 | 说明 |
|------|------|------|
| **简单 VO（默认）** | `@Data + @ApiModel` | 通过 `of()` 方法创建 |
| **复杂 VO（需手动构建）** | `@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel` | 多数据源组装 |

### 场景一：简单 VO（默认推荐）

通过 `of()` 方法从 PO 转换，不需要 Builder。

```java
package {package}.common.vo;

import cn.hutool.core.bean.BeanUtil;
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

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("字段描述")
    private String fieldName;

    /**
     * 从PO转换为VO
     *
     * @param po PO对象
     * @return VO对象
     */
    public static {Entity}Vo of({Entity}Po po) {
        if (po == null) {
            return null;
        }
        {Entity}Vo vo = new {Entity}Vo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
```

### 场景二：复杂 VO（多数据源）

需要从多个数据源组装，使用 Builder 模式。

```java
package {package}.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("{VO描述}")
public class {Entity}Vo {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("字段描述")
    private String fieldName;

    @ApiModelProperty("关联对象名称")
    private String relatedName;  // 来自其他数据源
}

// 使用示例
{Entity}Vo vo = {Entity}Vo.builder()
    .id(po.getId())
    .fieldName(po.getFieldName())
    .relatedName(relatedPo.getName())
    .build();
```

## 12.4 对象转换规范

### VO 转换（推荐 `of()` 方法）

**定义：**
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

**使用：**
```java
// 单个转换
{Entity}Vo vo = {Entity}Vo.of(po);

// 列表转换
List<{Entity}Vo> voList = poList.stream()
    .map({Entity}Vo::of)
    .collect(Collectors.toList());

// 分页转换
Page<{Entity}Vo> voPage = poPage.convert({Entity}Vo::of);
```

### DTO 转 PO

在 Service 中使用 `BeanUtil.copyProperties`：
```java
{Entity}Po entity = new {Entity}Po();
BeanUtil.copyProperties(dto, entity);
entity.setId(IdUtil.getSnowflakeNextId());
```

## 12.5 总结

| 对象 | 默认注解 | 特殊情况 |
|------|---------|---------|
| **DTO（独立）** | `@Data + @ApiModel` | 需要 Builder 时加 `@Builder` |
| **DTO（继承 PO）** | `@Data + @EqualsAndHashCode(callSuper) + @ApiModel` | PO 用 `@SuperBuilder` 时 DTO 也用 |
| **VO（默认）** | `@Data + @ApiModel` | 多数据源时用 `@Builder` |
| **QueryDto** | `@Data + @EqualsAndHashCode(callSuper) + @ApiModel` | 继承分页基类 |
