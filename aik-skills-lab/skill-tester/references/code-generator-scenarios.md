# 代码生成 测试场景

> 针对技能: `code-generator`
> 测试类型: 技术型
> 规范来源: [aIk-coding-style](../../aIk-coding-style/SKILL.md)

---

## 测试场景概述

| 测试用例 | 场景描述 | 预期违规数量 | 严重级别 |
|---------|---------|-------------|---------|
| TC-01 | 错误的目录结构（缺少 common/ 子目录） | 4 | 错误 |
| TC-02 | 错误的 PO 注解（无继承PO缺少 @Builder） | 2 | 错误 |
| TC-03 | 缺少 VO 的 of() 转换方法 | 1 | 错误 |
| TC-04 | Controller 返回 Map 而非 VO | 1 | 警告 |
| TC-05 | 正确的模块结构（所有规范均已遵循） | 0 | - |

---

## TC-01: 错误的目录结构

### 违规目录结构

```
order-module/
├── controller/
│   └── OrderController.java
├── service/
│   ├── OrderService.java
│   └── impl/
│       └── OrderServiceImpl.java
├── dao/
│   └── OrderMapper.java
├── model/                    ← 错误：应改为 common/po/
│   └── Order.java
├── dto/                      ← 错误：应放在 common/dto/ 下
│   ├── OrderDto.java
│   └── OrderQueryDto.java
├── vo/                       ← 错误：应放在 common/vo/ 下
│   └── OrderVo.java
├── constant/                 ← 错误：应放在 common/constant/ 下
│   └── ErrorCode.java
└── utils/
    └── OrderUtil.java
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `model/` 目录命名错误 | PO 实体类应放在 `common/po/` 目录下，不能使用 `model/` | 错误 |
| 2 | `dto/` 未放在 `common/` 下 | DTO 类应放在 `common/dto/` 下，与 controller 同级 | 错误 |
| 3 | `vo/` 未放在 `common/` 下 | VO 类应放在 `common/vo/` 下，与 controller 同级 | 错误 |
| 4 | `constant/` 未放在 `common/` 下 | 常量类应放在 `common/constant/` 下 | 错误 |

### 修复后目录结构

```
order-module/
├── controller/
│   └── OrderController.java
├── service/
│   ├── OrderService.java
│   └── impl/
│       └── OrderServiceImpl.java
├── dao/
│   ├── OrderMapper.java
│   └── mapping/              ← 可选：复杂SQL的XML文件
│       └── OrderMapping.xml
├── common/
│   ├── constant/
│   │   └── ErrorCodeConstant.java
│   ├── dto/
│   │   ├── OrderDto.java
│   │   └── OrderQueryDto.java
│   ├── po/
│   │   └── OrderPo.java
│   ├── vo/
│   │   └── OrderVo.java
│   ├── enums/
│   │   └── OrderStatusEnum.java
│   ├── exception/
│   │   └── OrderException.java
│   └── utils/
│       └── OrderUtil.java
├── sql/
│   └── t_order.sql
└── README.md
```

---

## TC-02: 错误的 PO 注解

### 违规代码片段

```java
package com.example.order.common.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * -anchor 订单实体
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Data  // 仅使用 @Data，缺少其他必需注解
// 缺少 @Builder
// 缺少 @NoArgsConstructor
// 缺少 @AllArgsConstructor
@TableName("t_order")
@ApiModel("订单实体")
public class OrderPo {

    @ApiModelProperty("主键ID")
    @TableField("id")
    private Long id;

    @ApiModelProperty("订单号")
    @TableField("order_no")
    private String orderNo;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 缺少 `@Builder` 注解 | 无继承的 PO 必须包含 `@Builder`，用于构建器模式创建对象 | 错误 |
| 2 | 缺少 `@NoArgsConstructor` 注解 | MyBatis-Plus 需要无参构造方法进行对象映射 | 错误 |
| 3 | 缺少 `@AllArgsConstructor` 注解 | `@Builder` 需要全参构造方法配合使用 | 错误 |

### 修复后正确代码

```java
/**
 * -anchor 订单实体
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单实体")
@TableName("t_order")
public class OrderPo {
    // ... 字段定义
}
```

### PO 注解速查表

| 场景 | 必须注解 | 额外说明 |
|------|---------|---------|
| **无继承PO** | `@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor` | 绑定数据库时加 `@TableName` |
| **有继承PO** | `@Data + @SuperBuilder + @AllArgsConstructor + @ToString(callSuper = true) + @EqualsAndHashCode(callSuper = true)` | 必须显式定义无参构造 `public XxPo() { super(); }` |

---

## TC-03: 缺少 VO 的 of() 转换方法

### 违规代码片段

```java
package com.example.order.common.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * -anchor 订单视图对象
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Data
@ApiModel("订单视图对象")
public class OrderVo {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("订单总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    // 缺少 of() 静态转换方法！
    // 应包含：
    // public static OrderVo of(OrderPo po) { ... }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 缺少 `of()` 静态转换方法 | 默认VO必须包含 `public static XxVo of(XxPo po)` 方法，用于从 PO 到 VO 的安全转换 | 错误 |

### 修复后正确代码

```java
@Data
@ApiModel("订单视图对象")
public class OrderVo {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("订单总金额")
    private BigDecimal totalAmount;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 从PO转换为VO
     *
     * @param po PO对象
     * @return VO对象，PO为null时返回null
     */
    public static OrderVo of(OrderPo po) {
        if (po == null) {
            return null;
        }
        OrderVo vo = new OrderVo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
```

### of() 方法使用示例

```java
// Service 层使用
OrderVo vo = OrderVo.of(po);

// 列表转换
List<OrderVo> voList = poList.stream()
    .map(OrderVo::of)
    .collect(Collectors.toList());

// 分页转换
Page<OrderVo> voPage = poPage.convert(OrderVo::of);
```

---

## TC-04: Controller 返回 Map 而非 VO

### 违规代码片段

```java
package com.example.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * -anchor 订单控制器
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    //note 查询订单详情（返回 Map 而非 VO）
    @GetMapping("/findById")
    public ApiResponse<Map<String, Object>> findById(@RequestParam Long id) {
        OrderPo order = orderService.getById(id);  // 错误：Controller直接使用Po
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", order.getTotalAmount());
        result.put("createTime", order.getCreateTime());
        return ApiResponse.success(result);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | Controller 返回 `Map<String, Object>` 而非 VO | 必须使用具体的 VO 类封装返回数据，Map 缺乏类型安全和API文档信息 | 警告 |
| 2 | Controller 直接使用 PO 对象 | Service 层应返回原始类型/VO，Controller 不应直接操作 PO | 警告 |

### 修复后正确代码

```java
@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    //note 查询订单详情
    //note 老项目使用：@ApiOperation("查询订单详情")
    //note 新项目使用：@Operation(summary = "查询订单详情")
    @GetMapping("/findById")
    public ApiResponse<OrderVo> findById(@RequestParam Long id) {
        log.info("查询订单详情，ID：{}", id);
        OrderVo result = orderService.findById(id);
        return ApiResponse.success(result);
    }
}
```

```java
// Service 层正确实现
@Override
public OrderVo findById(Long id) {
    OrderPo po = lambdaQuery()
            .eq(OrderPo::getId, id)
            .one();
    if (po == null) {
        throw new BusinessException("订单不存在");
    }
    return OrderVo.of(po);  // 使用 VO 的 of() 方法转换
}
```

---

## TC-05: 正确的模块结构（所有规范均已遵循）

### 完整规范目录结构

```
order-module/
├── api/
│   └── order-api.md                              # API接口文档
├── common/
│   ├── constant/
│   │   └── OrderCacheKeyConstant.java            # 缓存Key常量
│   ├── dto/
│   │   ├── OrderCreateDto.java                   # 新增DTO
│   │   ├── OrderModifyDto.java                   # 修改DTO
│   │   └── OrderQueryDto.java                    # 查询DTO（继承分页基类）
│   ├── enums/
│   │   └── OrderStatusEnum.java                  # 状态枚举
│   ├── exception/
│   │   └── OrderException.java                   # 自定义异常（可选）
│   ├── po/
│   │   └── OrderPo.java                          # 持久化对象
│   ├── vo/
│   │   └── OrderVo.java                          # 视图对象
│   └── utils/
│       └── OrderUtil.java                        # 工具类
├── controller/
│   └── OrderController.java                      # 控制器
├── dao/
│   ├── OrderMapper.java                          # Mapper接口
│   └── mapping/                                  # XML映射（仅复杂SQL）
│       └── OrderMapping.xml
├── service/
│   ├── OrderService.java                         # 服务接口
│   └── impl/
│       └── OrderServiceImpl.java                 # 服务实现
├── sql/
│   └── t_order.sql                               # 建表SQL
└── README.md                                     # 模块说明
```

### 正确代码示例 - PO

```java
package com.example.order.common.po;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * -anchor 订单实体
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单实体")
@TableName("t_order")
public class OrderPo {

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty("订单号")
    @TableField("order_no")
    private String orderNo;

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private Long userId;

    @ApiModelProperty("订单总金额")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @ApiModelProperty("状态")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @TableField("modify_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifyTime;
}
```

### 正确代码示例 - Service 实现

```java
package com.example.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.order.common.dto.OrderCreateDto;
import com.example.order.common.dto.OrderQueryDto;
import com.example.order.common.po.OrderPo;
import com.example.order.common.vo.OrderVo;
import com.example.order.dao.OrderMapper;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * -anchor 订单服务实现类
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Slf4j
@Service("order.OrderService")
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderPo>
        implements OrderService {

    //note 最大分页页码
    private static final int MAX_PAGE_NUM = 100;

    private final OrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo createOrder(OrderCreateDto dto) {
        //note 参数校验
        if (dto == null) {
            throw new BusinessException("参数不能为空");
        }

        //anchor 构建实体并设置审计字段
        OrderPo entity = OrderPo.builder()
                .id(IdUtil.getSnowflakeNextId())
                .orderNo(dto.getOrderNo())
                .userId(getCurrentUserId())
                .totalAmount(dto.getTotalAmount())
                .status(0)
                .createTime(LocalDateTime.now())
                .modifyTime(LocalDateTime.now())
                .build();

        //note 保存订单
        save(entity);

        return OrderVo.of(entity);
    }

    @Override
    public OrderVo findById(Long id) {
        OrderPo po = lambdaQuery()
                .eq(OrderPo::getId, id)
                .one();

        if (po == null) {
            throw new BusinessException("订单不存在");
        }

        return OrderVo.of(po);
    }

    @Override
    public Page<OrderVo> findPage(OrderQueryDto dto) {
        //note 限制最大页码
        if (dto.getCurrent() > MAX_PAGE_NUM) {
            throw new BusinessException("最多查询前" + MAX_PAGE_NUM + "页");
        }

        Page<OrderPo> poPage = lambdaQuery()
                .orderByDesc(OrderPo::getCreateTime)
                .page(new Page<>(dto.getCurrent(), dto.getSize()));

        return poPage.convert(OrderVo::of);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| - | 无违规 | 所有代码生成规范均已正确遵循 | - |

### 通过项清单

- [x] 目录结构符合规范（common/po/、common/dto/、common/vo/ 等）
- [x] PO 使用 `@Data + @Builder + @NoArgsConstructor + @AllArgsConstructor`（无继承场景）
- [x] PO 类名使用 `XxPo` 后缀
- [x] DTO 类名使用 `XxDto` 后缀，位于 `common/dto/`
- [x] VO 类名使用 `XxVo` 后缀，位于 `common/vo/`，包含 `of()` 方法
- [x] Controller 返回 VO 对象，不返回 Map
- [x] Service Bean 名称使用 `{module}.{ServiceName}` 格式
- [x] 依赖注入使用 `private final` + `@RequiredArgsConstructor`
- [x] 类注释使用 `-anchor` 格式，`@author a I k .`
- [x] 代码注释使用 `//note` 和 `//anchor`
- [x] 事务注解指定 `rollbackFor = Exception.class`
- [x] 分层职责清晰：Controller 封装响应，Service 返回原始类型/VO

---

## RED-GREEN-REFACTOR 执行参考

### RED阶段（无技能）

```
预期结果: 模型可能生成基本可用的代码，但大量不符合规范
- TC-01 (目录结构): 容易犯错，将 dto/vo 放在顶层而非 common/ 下
- TC-02 (PO注解): 容易遗漏 @Builder 或 @NoArgsConstructor
- TC-03 (VO of()): 几乎必然遗漏
- TC-04 (返回Map): 新手容易犯此错误
- 违规率预估: 80-100%
```

### GREEN阶段（加载技能）

```
验证标准:
- 生成代码的目录结构 100% 符合规范
- PO 注解组合正确率 > 95%
- VO 类均包含 of() 方法
- Controller 返回 VO 而非 Map
- 误报率 < 5%
```

### REFACTOR阶段（迭代收紧）

```
常见遗漏:
- 对 @TableField 显式映射的检查（字段全大写）
- 对有继承PO需要显式无参构造的检查
- 对 XML 位置（dao/mapping/）的检查
- 对 SQL 文件位置的检查
- 对 @Service Bean 名称格式的检查
- 对常量类位置的检查（common/constant/ vs 顶层 constant/）
```
