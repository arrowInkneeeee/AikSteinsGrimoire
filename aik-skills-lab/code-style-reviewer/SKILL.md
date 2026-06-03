---
name: code-style-reviewer
description: 代码风格审查（命名、注释、格式），严格遵循 aIk-coding-style 规范
type: Skill
version: 1.0.0
---

# code-style-reviewer

> **重要**：本技能审查代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范。

## 核心规范引用

审查代码前必须阅读并遵循以下规范：

1. **类注释**：使用 `-anchor` 标记的固定格式，`@author a I k .`
2. **行注释**：`//note`（普通）、`//anchor`（关键）
3. **命名规范**：PO用 `XxPo`、DTO用 `XxDto`、VO用 `XxVo`
4. **Service Bean**：`{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式
5. **依赖注入**：统一使用 `private final` + `@RequiredArgsConstructor`
6. **PO注解**：
   - 无继承：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
   - 有继承：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` + 显式无参构造
7. **DTO/VO注解**：
   - DTO（独立）：`@Data + @ApiModel`
   - DTO（继承PO）：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`
   - VO（默认）：`@Data + @ApiModel`，必须包含 `of()` 方法
   - VO（复杂）：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`

## 输入

- **业务代码**: code-implementer 实现的代码

## 输出

风格审查报告

## 检查项

### 命名规范

| 类型 | 规范 | 正确示例 | 错误示例 |
|------|------|---------|---------|
| 类名 | 大驼峰 | OrderServiceImpl | orderServiceImpl |
| 方法名 | 小驼峰 | createOrder | CreateOrder |
| 变量名 | 小驼峰 | orderId | order_id |
| 常量名 | 大写下划线 | MAX_PAGE_SIZE | maxPageSize |
| 包名 | 全小写 | com.xxx.service.impl | com.xxx.Service.Impl |

**检查点**：
- [ ] 类名符合大驼峰规范
- [ ] 方法名符合小驼峰规范
- [ ] 变量名符合小驼峰规范
- [ ] 常量名符合大写下划线规范
- [ ] 包名全小写，无下划线

### 注释规范

**类注释（必须严格遵循）**：
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

**检查点**：
- [ ] 类注释使用 `-anchor` 格式
- [ ] `@author` 固定为 `a I k .`（注意空格和点号）
- [ ] `@implNote` 固定为 `JDK 8`
- [ ] `@since` 使用日期格式 `yyyy/MM/dd`
- [ ] 结尾必须有 `-` 标记

**行注释规范**：
| 注释类型 | 格式 | 使用场景 |
|---------|------|---------|
| 普通注释 | `//note {内容}` | 一般代码说明 |
| 关键注释 | `//anchor {内容}` | 关键逻辑、重要业务点 |

```java
//note 参数校验
if (StrUtil.isBlank(dto.getFieldName())) {
    throw new BusinessException("参数错误：fieldName不能为空");
}

//anchor 构建实体并设置审计字段
{Entity}Po entity = new {Entity}Po();
BeanUtil.copyProperties(dto, entity);
entity.setId(IdUtil.getSnowflakeNextId());
```

**禁止行尾注释**：
```java
// 错误
private Long orderId; // 订单ID

// 正确
/**
 * 主键ID
 */
@ApiModelProperty("主键ID")
private Long orderId;
```

**检查点**：
- [ ] 类注释使用 `-anchor` 格式，`@author a I k .`
- [ ] 普通注释使用 `//note`
- [ ] 关键注释使用 `//anchor`
- [ ] 禁止行尾注释
- [ ] if必须使用大括号 `{}`

### 代码格式

**强制大括号**：
```java
// 错误
if (condition) doSomething();

// 正确
if (condition) {
    doSomething();
}
```

**缩进**：
- 使用 4 个空格（非 Tab）
- 续行缩进 8 个空格

```java
public void longMethodName(
        String param1, String param2, String param3) {
    // 缩进 4 空格
}
```

**行长度**：
- 不超过 120 字符
- 超过则换行

```java
// 错误
OrderVO orderVO = orderService.createOrder(dto, userId, addressId, couponId, remark);

// 正确
OrderVO orderVO = orderService.createOrder(
        dto, userId, addressId, couponId, remark);
```

**空行**：
- 方法之间空一行
- 逻辑块之间空一行
- 导入语句分组（java/javax/第三方/本项目）

```java
package com.xxx.module.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.xxx.module.dto.OrderDTO;
import com.xxx.module.entity.OrderEntity;

@Service
public class OrderServiceImpl implements OrderService {
    
    @Override
    public OrderVO createOrder(OrderDTO dto) {
        // 校验参数
        validateOrder(dto);
        
        // 保存订单
        OrderEntity order = saveOrder(dto);
        
        // 发送通知
        sendNotification(order);
        
        return convertToVO(order);
    }
    
    @Override
    public OrderVO getById(Long id) {
        // ...
    }
}
```

**检查点**：
- [ ] if/for/while 有 {}
- [ ] 使用 4 空格缩进
- [ ] 行长度不超过 120 字符
- [ ] 适当空行分隔

### Lombok 使用

**PO实体类（必须遵循）**：
```java
@Data
@SuperBuilder
@ToString(callSuper = true)
@TableName("{table_name}")
public class {Entity}Po {
    // ...
}
```

**Service 类**：
```java
@Slf4j
@RequiredArgsConstructor
@Service("{module}.{Entity}Service")
public class {Entity}ServiceImpl extends ServiceImpl<{Entity}Mapper, {Entity}Po>
        implements {Entity}Service {
    private final {Entity}Mapper {entity}Mapper;
}
```

**检查点**：
- [ ] **PO（无继承）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
- [ ] **PO（有继承）**：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` + 显式无参构造
- [ ] **DTO（独立）**：`@Data + @ApiModel`
- [ ] **DTO（继承PO）**：`@Data + @EqualsAndHashCode(callSuper = true) + @ApiModel`
- [ ] **VO（默认）**：`@Data + @ApiModel`，包含 `of()` 转换方法
- [ ] **VO（复杂）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`
- [ ] Service/Controller 使用 `private final` + `@RequiredArgsConstructor` 注入

### 导入规范

**顺序**：
1. java.*
2. javax.*
3. 第三方库
4. 本项目类

**静态导入**：
```java
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.when;
```

**禁用通配符导入**：
```java
// 错误
import java.util.*;

// 正确
import java.util.List;
import java.util.Map;
```

**检查点**：
- [ ] 导入分组有序
- [ ] 无通配符导入
- [ ] 无未使用的导入

## 输出格式

```markdown
## 代码风格审查报告

### 审查概览

| 检查项 | 通过 | 警告 | 失败 |
|--------|------|------|------|
| 命名规范 | 45 | 2 | 0 |
| 注释规范 | 38 | 5 | 1 |
| 代码格式 | 42 | 3 | 0 |
| Lombok 使用 | 20 | 0 | 0 |
| 导入规范 | 50 | 1 | 0 |

### 命名规范

✅ **通过**
- OrderServiceImpl：符合大驼峰
- createOrder：符合小驼峰
- MAX_PAGE_SIZE：符合常量命名

⚠️ **警告**
- `OrderServiceimpl`：类名应为 OrderServiceImpl（第 15 行）
- `order_DTO`：变量名应为 orderDTO（第 28 行）

### 注释规范

✅ **通过**
- OrderServiceImpl 类有注释
- createOrder 方法有 Javadoc

⚠️ **警告**
- cancelOrder 方法缺少 Javadoc（第 45 行）
- getById 方法参数缺少 @param（第 52 行）

❌ **失败**
- OrderService 接口缺少类注释（第 10 行）

### 代码格式

✅ **通过**
- 所有 if/for/while 有 {}
- 缩进使用 4 空格

⚠️ **警告**
- 第 78 行超过 120 字符（实际 135 字符）
- 第 85-90 行缺少空行分隔逻辑块

### Lombok 使用

✅ **通过**
- OrderEntity 正确使用 @Data、@Builder
- OrderServiceImpl 正确使用 @RequiredArgsConstructor

### 导入规范

✅ **通过**
- 导入分组有序
- 无通配符导入

⚠️ **警告**
- 第 12 行 java.util.Date 未使用

### 修复建议

**高优先级**：
1. 添加 OrderService 接口类注释
2. 删除未使用的导入 java.util.Date

**中优先级**：
1. 修复类名 OrderServiceimpl -> OrderServiceImpl
2. 为 cancelOrder 方法添加 Javadoc

**低优先级**：
1. 拆分第 78 行长代码
2. 第 85-90 行添加空行
```

## 命名规范检查表

| 类型 | 规范 | 示例 | 检查项 |
|------|------|------|--------|
| PO实体 | `XxPo` | `OrderPo` | 后缀必须是 Po |
| DTO | `XxDto` | `OrderCreateDto` | 后缀必须是 Dto |
| VO | `XxVo` | `OrderVo` | 后缀必须是 Vo |
| 常量类 | `XxConstant` | `CacheKeyConstant` | 后缀必须是 Constant |
| Service Bean | `{module}.{Name}` | `order.OrderService` | 必须包含模块前缀 |
| 表名 | 下划线小写 | `lb_order` | 小写下划线 |
| 字段名 | 下划线小写 | `order_no` | 小写下划线 |

## 严重级别

| 级别 | 说明 | 示例 |
|------|------|------|
| **错误** | 必须修复 | 类名不规范、类注释格式错误、@author不正确 |
| **警告** | 建议修复 | 方法缺少注释、行过长、未使用 //note 或 //anchor |
| **提示** | 可选修复 | 空行不足、导入顺序 |

## 注意事项

- **所有代码必须严格遵循 [aIk-coding-style](../aIk-coding-style/SKILL.md)（绝对路径：file:///c:/Users/arrowInknee/.lingma/skills/aIk-coding-style/SKILL.md） 规范**
- **类注释必须使用 `-anchor` 格式，`@author a I k .`**
- **代码注释使用 `//note` 和 `//anchor` 标记**
- **PO（无继承）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`
- **PO（有继承）**：`@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper) + @EqualsAndHashCode(callSuper)` + 显式无参构造
- **DTO（独立）**：`@Data + @ApiModel`
- **DTO（继承PO）**：`@Data + @EqualsAndHashCode(callSuper) + @ApiModel`
- **VO（默认）**：`@Data + @ApiModel`，必须包含 `of()` 转换方法
- **VO（复杂）**：`@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor + @ApiModel`
- **Service Bean名称使用 `{module}.{ServiceName}` 或 `{module}.{subModule}.{ServiceName}` 格式**
- **注入统一使用 `private final` + `@RequiredArgsConstructor`**
- **禁止行尾注释，if必须使用大括号**
- 命名规范是硬性要求，必须遵守
- 注释要清晰说明用途，而非重复代码
- 代码格式影响可读性，要保持一致
