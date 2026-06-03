---
name: conflict-detector
description: 当需要检测需求项之间的矛盾、重复或依赖缺失时使用。适用于"检查需求冲突"、"检测重复需求"、"需求有没有矛盾"、"核对需求一致性"等场景。输出冲突报告及解决建议。
type: Skill
version: 1.0.0
---

# conflict-detector

## 输入

- **功能点列表**: 已澄清的功能点

## 输出

冲突报告

```json
{
  "conflicts": [
    {
      "type": "logical|duplicate|dependency|scope",
      "severity": "high|medium|low",
      "feature_ids": ["F001", "F002"],
      "description": "冲突描述",
      "suggestion": "解决建议"
    }
  ],
  "summary": {
    "total_conflicts": 5,
    "high_severity": 2,
    "medium_severity": 2,
    "low_severity": 1
  }
}
```

## 工作流

1. **两两比对功能点**
   - 遍历所有功能点组合
   - 计算语义相似度
   - 检查逻辑关系

2. **检测逻辑冲突**
   - 识别互斥需求
   - 检测矛盾的业务规则
   - 标记无法同时满足的需求

3. **检测重复需求**
   - 语义相似度 > 80% 标记为重复
   - 检查描述重叠
   - 识别同一功能的不同表述

4. **检测依赖缺失**
   - 识别前置依赖
   - 检查依赖功能是否存在
   - 标记循环依赖

5. **生成冲突报告及解决建议**
   - 分类冲突类型
   - 评估严重程度
   - 提供解决建议

## 冲突类型

| 类型 | 描述 | 示例 |
|------|------|------|
| logical | 逻辑冲突：A 需求与 B 需求无法同时满足 | "所有用户可见" vs "仅 VIP 可见" |
| duplicate | 重复冲突：A 需求与 B 需求描述的是同一功能 | "添加购物车" vs "加入购物车" |
| dependency | 依赖冲突：A 需求依赖 B 需求，但 B 不存在 | "订单支付" 依赖 "订单创建" |
| scope | 范围冲突：A 需求超出项目范围 | 在项目范围外的新功能 |

## 调用规则

- 在 requirement-clarifier 之后调用
- 如存在 high severity 冲突，需要解决后才能继续
- 报告中的 suggestion 可直接用于指导用户决策

## 示例

### 输入

```json
{
  "features": [
    {
      "id": "F001",
      "description": "所有商品对所有用户可见",
      "category": "core"
    },
    {
      "id": "F002",
      "description": "VIP 专属商品仅 VIP 用户可见",
      "category": "core"
    },
    {
      "id": "F003",
      "description": "用户可以添加商品到购物车",
      "category": "core"
    },
    {
      "id": "F004",
      "description": "购物车功能支持加入商品",
      "category": "core"
    },
    {
      "id": "F005",
      "description": "用户可以直接购买商品",
      "category": "core"
    }
  ]
}
```

### 输出

```json
{
  "conflicts": [
    {
      "type": "logical",
      "severity": "high",
      "feature_ids": ["F001", "F002"],
      "description": "F001 要求所有商品对所有用户可见，F002 要求 VIP 商品仅 VIP 可见，存在逻辑矛盾",
      "suggestion": "明确商品可见性策略：1) 所有商品公开 2) 支持 VIP 专属商品 3) 混合模式（部分公开部分专属）"
    },
    {
      "type": "duplicate",
      "severity": "medium",
      "feature_ids": ["F003", "F004"],
      "description": "F003 和 F004 描述的是同一购物车功能",
      "suggestion": "合并为一个功能点，保留 F003 的表述"
    },
    {
      "type": "dependency",
      "severity": "low",
      "feature_ids": ["F005"],
      "description": "F005 直接购买功能可能需要购物车功能作为前置",
      "suggestion": "明确直接购买流程：1) 跳过购物车直接结算 2) 需要购物车支持"
    }
  ],
  "summary": {
    "total_conflicts": 3,
    "high_severity": 1,
    "medium_severity": 1,
    "low_severity": 1
  }
}
```

## 严重级别定义

- **high**: 必须解决，否则影响系统设计
- **medium**: 建议解决，影响实现质量
- **low**: 可选解决，优化类问题

## 注意事项

- 相似度阈值可根据项目调整（默认 80%）
- 逻辑冲突需要人工介入决策
- 保留冲突历史便于追溯变更
