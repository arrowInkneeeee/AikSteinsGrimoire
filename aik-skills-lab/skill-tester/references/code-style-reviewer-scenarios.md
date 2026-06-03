# 代码风格审查 测试场景

> 针对技能: `code-style-reviewer`
> 测试类型: 纪律型
> 规范来源: [aIk-coding-style](../../aIk-coding-style/SKILL.md)

---

## 测试场景概述

| 测试用例 | 场景描述 | 预期违规数量 | 严重级别 |
|---------|---------|-------------|---------|
| TC-01 | 错误类注释格式（缺少-anchor，错误@author） | 5 | 错误 |
| TC-02 | 行尾注释（注释与代码同行——禁止） | 3 | 警告 |
| TC-03 | 缺少@RequiredArgsConstructor（使用字段注入） | 2 | 错误 |
| TC-04 | if语句缺少大括号 | 2 | 警告 |
| TC-05 | 正确代码，所有规范均已遵循 | 0 | - |

---

## TC-01: 错误类注释格式

### 违规代码片段

```java
package com.example.order.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现类
 *
 * @author zhangsan
 * @version 1.0
 * @since 2026-01-01
 */
@Slf4j
@Service
public class OrderServiceImpl {

    @Autowired
    private OrderMapper orderMapper;

    public void processOrder(Long orderId) {
        // 处理订单逻辑
        OrderPo order = orderMapper.selectById(orderId);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 类注释缺少 `-anchor` 标记 | 类注释必须以 `-anchor` 开头，以 `-` 结尾 | 错误 |
| 2 | `@author` 错误 | 必须为 `a I k .`（注意空格和点号），而非 `zhangsan` | 错误 |
| 3 | 缺少 `@implNote JDK 8` | 类注释中必须包含 `@implNote JDK 8` | 错误 |
| 4 | 缺少 `@apiNote` | 类注释中必须包含 `@apiNote` 标签 | 错误 |
| 5 | 结尾缺少 `-` 标记 | 类注释结尾必须以 `-` 单独一行收尾 | 错误 |

### 修复后正确代码

```java
/**
 * -anchor 订单服务实现类
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/01/01
 * -
 */
```

---

## TC-02: 行尾注释

### 违规代码片段

```java
@Slf4j
@Service
public class OrderServiceImpl {

    private final OrderMapper orderMapper;  // 订单Mapper

    public void processOrder(Long orderId) {
        if (orderId == null) {  // 参数校验
            throw new BusinessException("参数错误");
        }

        OrderPo order = orderMapper.selectById(orderId);  // 查询订单
        if (order == null) {
            log.error("订单不存在，ID：{}", orderId);  // 记录错误日志
            throw new BusinessException("订单不存在");
        }
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `private final OrderMapper orderMapper;  // 订单Mapper` | 字段注释必须单独一行，禁止行尾注释 | 警告 |
| 2 | `if (orderId == null) {  // 参数校验` | 注释应放在代码上方单独一行，使用 `//note` 格式 | 警告 |
| 3 | `OrderPo order = orderMapper.selectById(orderId);  // 查询订单` | 注释应单独一行，使用 `//note` 格式 | 警告 |
| 4 | `log.error("订单不存在...");  // 记录错误日志` | 注释应单独一行，使用 `//note` 格式 | 警告 |

### 修复后正确代码

```java
//note 订单Mapper
private final OrderMapper orderMapper;

//note 参数校验
if (orderId == null) {
    throw new BusinessException("参数错误");
}

//note 查询订单
OrderPo order = orderMapper.selectById(orderId);
if (order == null) {
    //note 记录错误日志
    log.error("订单不存在，ID：{}", orderId);
    throw new BusinessException("订单不存在");
}
```

---

## TC-03: 缺少@RequiredArgsConstructor（字段注入）

### 违规代码片段

```java
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserService userService;

    public OrderVo createOrder(OrderDto dto) {
        // 业务逻辑
        return null;
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 使用 `@Autowired` 字段注入 | 必须使用 `private final` + `@RequiredArgsConstructor` 构造器注入 | 错误 |
| 2 | 缺少 `@RequiredArgsConstructor` 注解 | 类上需添加 `@RequiredArgsConstructor`，字段声明为 `private final` | 错误 |

### 修复后正确代码

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    private final UserService userService;

    public OrderVo createOrder(OrderDto dto) {
        // 业务逻辑
        return null;
    }
}
```

---

## TC-04: if语句缺少大括号

### 违规代码片段

```java
@Service
public class OrderServiceImpl {

    public void validateOrder(OrderDto dto) {
        if (dto == null)
            throw new BusinessException("参数不能为空");

        if (dto.getAmount() == null)
            throw new BusinessException("金额不能为空");

        for (OrderItemDto item : dto.getItems())
            if (item.getQuantity() <= 0)
                throw new BusinessException("数量必须大于0");
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `if (dto == null)` 缺少大括号 | 即使单行 if 也必须使用 `{}` | 警告 |
| 2 | `if (dto.getAmount() == null)` 缺少大括号 | 即使单行 if 也必须使用 `{}` | 警告 |
| 3 | 嵌套 `if` 缺少大括号 | 循环内 if 也必须使用 `{}` | 警告 |

### 修复后正确代码

```java
public void validateOrder(OrderDto dto) {
    if (dto == null) {
        throw new BusinessException("参数不能为空");
    }

    if (dto.getAmount() == null) {
        throw new BusinessException("金额不能为空");
    }

    for (OrderItemDto item : dto.getItems()) {
        if (item.getQuantity() <= 0) {
            throw new BusinessException("数量必须大于0");
        }
    }
}
```

---

## TC-05: 正确代码（所有规范均已遵循）

### 正确代码片段

```java
package com.example.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.order.common.dto.OrderDto;
import com.example.order.common.po.OrderPo;
import com.example.order.common.vo.OrderVo;
import com.example.order.dao.OrderMapper;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    //note 默认分页大小
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final OrderMapper orderMapper;

    @Override
    public OrderVo createOrder(OrderDto dto) {
        //note 参数校验
        if (dto == null) {
            throw new BusinessException("参数不能为空");
        }

        //anchor 构建实体并设置审计字段
        OrderPo entity = new OrderPo();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(IdUtil.getSnowflakeNextId());
        entity.setCreateTime(LocalDateTime.now());
        entity.setModifyTime(LocalDateTime.now());

        //note 保存订单
        save(entity);

        //note 转换为VO返回
        return OrderVo.of(entity);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| - | 无违规 | 所有规范均已正确遵循 | - |

### 通过项清单

- [x] 类注释使用 `-anchor` 格式，`@author a I k .`
- [x] 包含 `@implNote JDK 8` 和 `@apiNote`
- [x] 使用 `private final` + `@RequiredArgsConstructor` 注入
- [x] Service Bean 名称使用 `{module}.{ServiceName}` 格式
- [x] 普通注释使用 `//note`
- [x] 关键注释使用 `//anchor`
- [x] 无行尾注释
- [x] if 语句均有大括号 `{}`
- [x] 类继承 `ServiceImpl<Mapper, PO>` 正确
- [x] 常量使用 `private static final`

---

## RED-GREEN-REFACTOR 执行参考

### RED阶段（无技能）

```
预期结果: 模型可能仅检测到 1-2 项违规
- 通常能发现明显的格式问题（如缺少大括号）
- 容易忽略 -anchor 格式、@author 规范、行尾注释等细节
- 漏检率预估: 60-80%
```

### GREEN阶段（加载技能）

```
验证标准:
- 违规检测率 > 90%
- 能准确识别所有 5 类违规类型
- 误报率 < 10%
- 能提供正确的修复建议
```

### REFACTOR阶段（迭代收紧）

```
常见遗漏:
- 对 "// XXX"（非 //note 或 //anchor 格式）的注释检测
- 对 @Service 无模块前缀的检测
- 对类继承结构正确性的检测
```
