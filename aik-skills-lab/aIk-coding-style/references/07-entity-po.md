# 实体类规范 (PO)

> 来源：aIk-coding-style 规范

## 7.1 PO 分类说明

根据使用场景，PO 分为以下情况：

| 场景 | 是否绑定数据库 | 是否有继承 | 核心注解区别 |
|------|--------------|-----------|-------------|
| **简单PO** | 否 | 否 | 无 `@TableName` |
| **数据库PO（无继承）** | 是 | 否 | 有 `@TableName` |
| **数据库PO（有继承）** | 是 | 是 | 有 `@TableName`，使用 `@SuperBuilder` |

## 7.2 注解使用规范

### 7.2.1 通用注解（所有PO都需要）

| 注解 | 说明 | 用途 |
|------|------|------|
| `@Data` | Lombok 自动生成 getter/setter/toString/equals/hashCode | 简化代码 |
| `@AllArgsConstructor` | 生成全参构造方法 | Builder 模式需要 |
| `@ApiModel` / `@Schema` | API 文档类描述 | 接口文档 |

### 7.2.2 场景特定注解

**无继承的PO：**
| 注解 | 说明 | 用途 |
|------|------|------|
| `@Builder` | Lombok 构建器模式 | 创建对象 |
| `@NoArgsConstructor` | 生成无参构造方法 | MyBatis-Plus 需要 |
| `@TableName("表名")` | 数据库表名映射（仅绑定数据库时需要） | 数据库映射 |

**有继承的PO：**
| 注解 | 说明 | 用途 |
|------|------|------|
| `@SuperBuilder` | 支持父类字段的构建器模式 | 创建对象（含父类字段） |
| `@ToString(callSuper = true)` | toString 包含父类字段 | 日志输出 |
| `@EqualsAndHashCode(callSuper = true)` | equals/hashCode 包含父类字段 | 对象比较 |
| `@TableName("表名")` | 数据库表名映射（仅绑定数据库时需要） | 数据库映射 |

## 7.3 PO 代码示例

### 场景一：简单PO（不绑定数据库，无继承）

用于临时数据传输对象或配置类，不映射数据库表。

```java
package {package}.common.po;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("{实体描述}")
public class {Entity}Po {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("业务字段")
    private String fieldName;
}

// 使用示例
{Entity}Po entity = {Entity}Po.builder()
    .id(IdUtil.getSnowflakeNextId())
    .fieldName(dto.getFieldName())
    .build();
```

### 场景二：数据库PO（无继承）

最常用的场景，映射数据库表，无父类继承。

```java
package {package}.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

// 使用示例
{Entity}Po entity = {Entity}Po.builder()
    .id(IdUtil.getSnowflakeNextId())
    .fieldName(dto.getFieldName())
    .createUserId(getCurrentUserId())
    .createUser(getCurrentUserName())
    .createTime(LocalDateTime.now())
    .modifyTime(LocalDateTime.now())
    .build();
```

### 场景三：数据库PO（有继承）

继承 `BasePo` 等父类，复用公共字段（如创建人、创建时间等）。

```java
package {package}.common.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel("{实体描述}")
@TableName("{table_name}")
public class {Entity}Po extends BasePo {

    /**
     * 业务字段
     */
    @ApiModelProperty("业务字段描述")
    private String fieldName;

    /**
     * 生效日期
     */
    @ApiModelProperty("生效日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectDate;

    /**
     * 无参构造方法（必须显式定义）
     */
    public {Entity}Po() {
        super();
    }
}

// 使用示例
{Entity}Po entity = {Entity}Po.builder()
    .id(IdUtil.getSnowflakeNextId())
    .fieldName(dto.getFieldName())
    .effectDate(dto.getEffectDate())
    .createUserId(getCurrentUserId())  // 父类字段
    .createUser(getCurrentUserName())  // 父类字段
    .createTime(LocalDateTime.now())   // 父类字段
    .build();
```

**有继承PO的注意事项：**
1. 使用 `@SuperBuilder` 替代 `@Builder`，支持父类字段构建
2. 必须显式定义无参构造方法 `public {Entity}Po() { super(); }`
3. 添加 `@ToString(callSuper = true)` 和 `@EqualsAndHashCode(callSuper = true)` 确保包含父类字段
4. 父类 `BasePo` 也需要使用 `@SuperBuilder` 和 `@Data` 注解

## 字段注解规范

| 注解 | 使用场景 | 示例 |
|------|---------|------|
| `@TableField("字段名")` | 数据库字段映射 | 所有字段 |
| `@TableId` | 主键字段 | id 字段 |
| `@ApiModelProperty("描述")` / `@Schema` | API 文档字段说明 | 所有字段 |

## 时间字段格式化

**LocalDateTime 类型（带时分秒）：**
```java
@ApiModelProperty("创建时间")
@TableField("create_time")
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

**LocalDate 类型（仅日期）：**
```java
@ApiModelProperty("生效日期")
@TableField("effect_date")
@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
@DateTimeFormat(pattern = "yyyy-MM-dd")
private LocalDate effectDate;
```

## 字段规范

- Java属性：驼峰命名（`fieldName`）
- 数据库字段：下划线命名（`field_name`）
- 使用 `@TableField` 显式指定映射关系
- 所有字段添加 JavaDoc 注释

## 主键策略

- 使用 `IdType.INPUT` 或 `IdType.AUTO`
- 类型：`Long`
- ID生成：使用项目统一的ID生成工具

## 图片字段规范

图片字段需要成对存储ID和名称：
```java
// ID字段：多张用逗号分隔
@TableField("image_ids")
private String imageIds;

// 名称字段：多张用逗号分隔
@TableField("image_names")
private String imageNames;
```
