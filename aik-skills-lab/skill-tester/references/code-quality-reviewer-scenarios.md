# 代码质量审查 测试场景

> 针对技能: `code-quality-reviewer`
> 测试类型: 技术型 + 纪律型
> 规范来源: [aIk-coding-style](../../aIk-coding-style/SKILL.md)

---

## 测试场景概述

| 测试用例 | 场景描述 | 预期违规数量 | 严重级别 |
|---------|---------|-------------|---------|
| TC-01 | N+1查询：循环内查询数据库 | 1 | 严重 |
| TC-02 | 空指针风险：未进行null检查直接调用 | 2 | 严重 |
| TC-03 | 事务吞异常：catch后未抛出导致事务不回滚 | 1 | 严重 |
| TC-04 | 深度分页：未限制分页最大值 | 1 | 警告 |
| TC-05 | 正确代码：使用正确的质量模式 | 0 | - |

---

## TC-01: N+1查询

### 违规代码片段

```java
@Service
public class OrderServiceImpl {

    private final OrderMapper orderMapper;
    private final OrderItemService itemService;

    //note 获取订单及商品列表（存在N+1问题）
    public List<OrderVo> getOrdersWithItems(List<Long> orderIds) {
        List<OrderVo> result = new ArrayList<>();
        for (Long orderId : orderIds) {
            // 第1次查询：查订单
            OrderPo order = orderMapper.selectById(orderId);
            // 第2次查询：查订单商品（N+1问题核心）
            List<OrderItemVo> items = itemService.getByOrderId(orderId);

            OrderVo vo = convertToVo(order);
            vo.setItems(items);
            result.add(vo);
        }
        return result;
    }

    private OrderVo convertToVo(OrderPo order) {
        OrderVo vo = new OrderVo();
        BeanUtil.copyProperties(order, vo);
        return vo;
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | for循环内逐条查询数据库 | 应使用批量查询（`lambdaQuery().in(ids).list()`），避免循环内每次迭代都访问数据库 | 严重 |

### 修复建议

```java
//note 批量查询订单及商品
public List<OrderVo> getOrdersWithItems(List<Long> orderIds) {
    // 1. 批量查询订单
    List<OrderPo> orders = lambdaQuery()
            .in(OrderPo::getId, orderIds)
            .list();

    // 2. 批量查询订单商品
    Map<Long, List<OrderItemVo>> itemMap = itemService.getMapByOrderIds(orderIds);

    // 3. 组装数据
    return orders.stream()
            .map(order -> {
                OrderVo vo = convertToVo(order);
                vo.setItems(itemMap.getOrDefault(order.getId(), Collections.emptyList()));
                return vo;
            })
            .collect(Collectors.toList());
}
```

---

## TC-02: 空指针风险

### 违规代码片段

```java
@Service
public class OrderServiceImpl {

    private final OrderMapper orderMapper;

    //note 根据ID查询订单（存在空指针风险）
    public OrderVo getById(Long id) {
        OrderPo order = orderMapper.selectById(id);
        OrderVo vo = new OrderVo();
        BeanUtil.copyProperties(order, vo);  // order 可能为 null，导致 NPE
        vo.setDisplayAmount(order.getAmount().toString());  // 再次解引用
        return vo;
    }

    //note 计算订单总额（未判空list）
    public BigDecimal calculateTotal(List<OrderItemDto> items) {
        return items.stream()  // items 可能为 null
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `orderMapper.selectById(id)` 返回值未做 null 判断 | 数据库查询结果可能为 null，直接使用会导致 NullPointerException | 严重 |
| 2 | `items.stream()` 未对集合做 null 判断 | 若调用方传入 null 集合，将直接抛出 NPE | 严重 |

### 修复建议

```java
//note 正确判空处理
public OrderVo getById(Long id) {
    OrderPo order = orderMapper.selectById(id);
    if (order == null) {
        throw new BusinessException("订单不存在");
    }
    OrderVo vo = new OrderVo();
    BeanUtil.copyProperties(order, vo);
    vo.setDisplayAmount(order.getAmount().toString());
    return vo;
}

//note 空集合安全处理
public BigDecimal calculateTotal(List<OrderItemDto> items) {
    if (CollectionUtil.isEmpty(items)) {
        return BigDecimal.ZERO;
    }
    return items.stream()
            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

---

## TC-03: 事务吞异常

### 违规代码片段

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final OrderMapper orderMapper;
    private final StockService stockService;

    //note 创建订单（事务异常被吞没）
    @Transactional
    public void createOrder(OrderDto dto) {
        try {
            //note 保存订单
            OrderPo order = new OrderPo();
            BeanUtil.copyProperties(dto, order);
            order.setId(IdUtil.getSnowflakeNextId());
            save(order);

            //note 扣减库存
            stockService.deduct(dto.getItemId(), dto.getQuantity());
        } catch (Exception e) {
            // 异常被捕获但未重新抛出，事务不会回滚！
            log.error("创建订单失败", e);
        }
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | catch块中仅记录日志，未重新抛出异常 | `@Transactional` 仅在抛出 RuntimeException 时回滚。catch 后未抛出意味着事务不会回滚，导致数据不一致 | 严重 |

### 修复建议

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderDto dto) {
    try {
        OrderPo order = new OrderPo();
        BeanUtil.copyProperties(dto, order);
        order.setId(IdUtil.getSnowflakeNextId());
        save(order);

        stockService.deduct(dto.getItemId(), dto.getQuantity());
    } catch (Exception e) {
        log.error("创建订单失败", e);
        // 必须重新抛出异常，确保事务回滚
        throw new BusinessException("创建订单失败：" + e.getMessage(), e);
    }
}
```

---

## TC-04: 深度分页

### 违规代码片段

```java
@Service
public class OrderServiceImpl {

    private final OrderMapper orderMapper;

    //note 分页查询订单（未限制最大页码）
    public PageResult<OrderVo> listOrder(OrderQueryDto dto) {
        // 未限制分页，可能查询第10000页，性能极差
        Page<OrderPo> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        lambdaQuery()
                .orderByDesc(OrderPo::getCreateTime)
                .page(page);

        List<OrderVo> voList = page.getRecords().stream()
                .map(OrderVo::of)
                .collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal());
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 未限制分页最大页码 | 深度分页（如第10000页）会导致MySQL扫描大量行后丢弃，性能急剧下降。应限制最大页码或改用游标分页 | 警告 |

### 修复建议

```java
//note 分页最大页码限制
private static final int MAX_PAGE_NUM = 100;

public PageResult<OrderVo> listOrder(OrderQueryDto dto) {
    //note 限制最大页码，防止深度分页
    if (dto.getPageNum() > MAX_PAGE_NUM) {
        throw new BusinessException("最多查询前" + MAX_PAGE_NUM + "页");
    }

    Page<OrderPo> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    lambdaQuery()
            .orderByDesc(OrderPo::getCreateTime)
            .page(page);

    List<OrderVo> voList = page.getRecords().stream()
            .map(OrderVo::of)
            .collect(Collectors.toList());

    return new PageResult<>(voList, page.getTotal());
}
```

---

## TC-05: 正确代码（使用正确的质量模式）

### 正确代码片段

```java
package com.example.order.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.order.common.dto.OrderDto;
import com.example.order.common.dto.OrderQueryDto;
import com.example.order.common.po.OrderPo;
import com.example.order.common.vo.OrderVo;
import com.example.order.dao.OrderMapper;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public OrderVo createOrder(OrderDto dto) {
        //note 参数校验
        if (dto == null || dto.getItemId() == null) {
            throw new BusinessException("参数不能为空");
        }

        //anchor 构建订单实体
        OrderPo entity = new OrderPo();
        BeanUtil.copyProperties(dto, entity);
        entity.setCreateTime(LocalDateTime.now());

        //note 保存订单
        save(entity);

        //note 转换为VO返回
        return OrderVo.of(entity);
    }

    @Override
    public OrderVo getById(Long id) {
        //note 查询并判空
        OrderPo entity = lambdaQuery()
                .eq(OrderPo::getId, id)
                .one();

        if (entity == null) {
            throw new BusinessException("订单不存在，ID：" + id);
        }

        return OrderVo.of(entity);
    }

    @Override
    public Page<OrderVo> findPage(OrderQueryDto dto) {
        //note 限制最大页码防止深度分页
        if (dto.getPageNum() > MAX_PAGE_NUM) {
            throw new BusinessException("最多查询前" + MAX_PAGE_NUM + "页");
        }

        Page<OrderPo> poPage = lambdaQuery()
                .orderByDesc(OrderPo::getCreateTime)
                .page(new Page<>(dto.getPageNum(), dto.getPageSize()));

        return poPage.convert(OrderVo::of);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| - | 无违规 | 所有质量模式均已正确遵循 | - |

### 通过项清单

- [x] 无 N+1 查询（使用 MyBatis-Plus 分页查询）
- [x] 数据库查询结果判空
- [x] 事务异常正确抛出（`@Transactional(rollbackFor = Exception.class)`）
- [x] 分页最大页码已限制
- [x] 使用 `private final` + `@RequiredArgsConstructor` 注入
- [x] 方法入参有校验
- [x] 事务范围合理（仅包含数据库操作）

---

## RED-GREEN-REFACTOR 执行参考

### RED阶段（无技能）

```
预期结果: 模型可能仅检测到最明显的 N+1 和空指针问题
- TC-01 (N+1): 通常能检测到，但可能忽略
- TC-02 (空指针): 容易检测到
- TC-03 (事务吞异常): 容易遗漏
- TC-04 (深度分页): 容易被忽略
- 漏检率预估: 40-60%
```

### GREEN阶段（加载技能）

```
验证标准:
- 违规检测率 > 90%
- 能准确识别 N+1、空指针、事务异常吞噬、无分页限制
- 误报率 < 10%
- 能提供具体的修复方案和性能影响分析
```

### REFACTOR阶段（迭代收紧）

```
常见遗漏:
- 对 Collector.toList() 返回 null 的判空
- 对同类方法调用导致事务失效的检测
- 对 Stream 中空集合的中间操作判空
- 对锁释放正确性的检测
```
