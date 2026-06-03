---
name: priority-assessor
description: 当需要使用MoSCoW方法（Must/Should/Could/Won't have）对用户故事进行优先级排序并给出推荐实施顺序时使用。适用于"排优先级"、"MoSCoW分析"、"优先级排序"、"哪些先做"等场景。
type: Skill
version: 1.0.0
---

# priority-assessor

## 输入

- **用户故事**: user-story-generator 的输出
- **业务上下文**（可选）: 产品目标、发布计划、资源约束等

## 输出

带优先级的用户故事

```json
{
  "prioritized_stories": [
    {
      "story_id": "US-001",
      "priority": "must|should|could|wont",
      "moscow_category": "Must have",
      "business_value": "high|medium|low",
      "user_impact": "high|medium|low",
      "technical_dependency": true|false,
      "rationale": "优先级判定理由"
    }
  ],
  "moscow_summary": {
    "must_have": ["US-001", "US-002"],
    "should_have": ["US-003"],
    "could_have": ["US-004"],
    "wont_have": ["US-005"]
  },
  "recommended_sequence": ["US-001", "US-002", "US-003", "US-004"]
}
```

## 工作流

1. **分析业务价值**
   - 评估对核心业务的贡献
   - 考虑收入影响
   - 分析战略对齐度

2. **分析用户影响范围**
   - 受影响用户比例
   - 使用频率
   - 用户痛点解决程度

3. **分析技术依赖关系**
   - 识别被依赖的故事
   - 确定基础功能
   - 标记阻塞项

4. **应用 MoSCoW 方法分类**
   - 根据多维度评估结果
   - 应用 MoSCoW 分类
   - 处理冲突和边界情况

5. **生成优先级排序列表**
   - 按优先级分组
   - 组内按依赖关系排序
   - 提供实施建议序列

## MoSCoW 分类

| 优先级 | 全称 | 定义 | 决策标准 |
|--------|------|------|---------|
| **Must have** | Must have this | 必须实现，否则项目失败 | 核心功能、法律合规、关键业务流程 |
| **Should have** | Should have this | 应该实现，重要但可妥协 | 重要功能，有替代方案，延期不影响发布 |
| **Could have** | Could have this | 可以实现，有则更好 | 增值功能，用户期望但非必需 |
| **Won't have** | Won't have this time | 本次不实现，未来考虑 | 超出范围、资源不足、低价值 |

## 评估维度

| 维度 | 权重 | 描述 |
|------|------|------|
| business_value | 40% | 对业务目标的贡献程度 |
| user_impact | 30% | 影响的用户范围和使用频率 |
| technical_dependency | 20% | 是否被其他功能依赖 |
| implementation_cost | 10% | 实现成本（复杂度、工期） |

## 调用规则

- 在 feasibility-checker 之后调用
- 需要业务上下文信息以准确评估
- 优先级应与利益相关者确认

## 示例

### 输入

```json
{
  "user_stories": [
    {
      "id": "US-001",
      "role": "顾客",
      "want": "浏览商品列表和详情",
      "so_that": "了解商品信息并决定是否购买"
    },
    {
      "id": "US-002",
      "role": "顾客",
      "want": "将商品添加到购物车",
      "so_that": "统一结算多个商品"
    },
    {
      "id": "US-003",
      "role": "顾客",
      "want": "使用微信支付订单",
      "so_that": "方便快捷地完成付款"
    },
    {
      "id": "US-004",
      "role": "顾客",
      "want": "收藏喜欢的商品",
      "so_that": "方便以后快速找到"
    },
    {
      "id": "US-005",
      "role": "顾客",
      "want": "分享商品到社交媒体",
      "so_that": "推荐给朋友"
    }
  ],
  "business_context": {
    "product_goal": "MVP 电商应用",
    "timeline": "6 周",
    "target_users": "C端消费者"
  }
}
```

### 输出

```json
{
  "prioritized_stories": [
    {
      "story_id": "US-001",
      "priority": "must",
      "moscow_category": "Must have",
      "business_value": "high",
      "user_impact": "high",
      "technical_dependency": true,
      "rationale": "核心功能，所有其他功能的基础，用户必须能浏览商品才能购买"
    },
    {
      "story_id": "US-002",
      "priority": "must",
      "moscow_category": "Must have",
      "business_value": "high",
      "user_impact": "high",
      "technical_dependency": true,
      "rationale": "核心购买流程的一部分，购物车是电商的基础功能"
    },
    {
      "story_id": "US-003",
      "priority": "must",
      "moscow_category": "Must have",
      "business_value": "high",
      "user_impact": "high",
      "technical_dependency": false,
      "rationale": "支付是电商闭环的关键，没有支付无法完成交易"
    },
    {
      "story_id": "US-004",
      "priority": "could",
      "moscow_category": "Could have",
      "business_value": "medium",
      "user_impact": "medium",
      "technical_dependency": false,
      "rationale": "增值功能，提升用户体验，但非 MVP 必需"
    },
    {
      "story_id": "US-005",
      "priority": "wont",
      "moscow_category": "Won't have this time",
      "business_value": "low",
      "user_impact": "low",
      "technical_dependency": false,
      "rationale": "超出 MVP 范围，可作为二期功能考虑"
    }
  ],
  "moscow_summary": {
    "must_have": ["US-001", "US-002", "US-003"],
    "should_have": [],
    "could_have": ["US-004"],
    "wont_have": ["US-005"]
  },
  "recommended_sequence": [
    "US-001",
    "US-002",
    "US-003",
    "US-004"
  ]
}
```

## 文本格式输出

```markdown
# 需求优先级评估报告

## MoSCoW 分类

### Must Have (必须有)

| 故事ID | 用户故事 | 业务价值 | 用户影响 | 理由 |
|--------|---------|---------|---------|------|
| US-001 | 浏览商品列表和详情 | 高 | 高 | 核心功能，所有其他功能的基础 |
| US-002 | 将商品添加到购物车 | 高 | 高 | 核心购买流程的一部分 |
| US-003 | 使用微信支付订单 | 高 | 高 | 支付是电商闭环的关键 |

### Should Have (应该有)

无

### Could Have (可以有)

| 故事ID | 用户故事 | 业务价值 | 用户影响 | 理由 |
|--------|---------|---------|---------|------|
| US-004 | 收藏喜欢的商品 | 中 | 中 | 增值功能，提升用户体验 |

### Won't Have (本次不做)

| 故事ID | 用户故事 | 业务价值 | 用户影响 | 理由 |
|--------|---------|---------|---------|------|
| US-005 | 分享商品到社交媒体 | 低 | 低 | 超出 MVP 范围，二期考虑 |

## 推荐实施顺序

1. **US-001** - 浏览商品列表和详情
2. **US-002** - 将商品添加到购物车
3. **US-003** - 使用微信支付订单
4. **US-004** - 收藏喜欢的商品（资源允许时）

## 关键依赖

- US-002 依赖 US-001（需要先浏览才能添加购物车）
- US-003 依赖 US-002（需要购物车才能支付）

## 建议

- 优先完成 Must Have 功能，确保 MVP 可用
- Could Have 功能在 Must Have 完成后评估资源情况
- Won't Have 功能记录到二期需求池
```

## 优先级判定矩阵

| 业务价值 | 用户影响 | 技术依赖 | 建议优先级 |
|---------|---------|---------|-----------|
| 高 | 高 | 是 | Must |
| 高 | 高 | 否 | Must |
| 高 | 中 | 是 | Must |
| 高 | 中 | 否 | Should |
| 中 | 高 | 是 | Should |
| 中 | 高 | 否 | Could |
| 中 | 中 | 否 | Could |
| 低 | 任意 | 任意 | Won't |

## 注意事项

- Must Have 不应超过总工作量的 60%
- 优先级需要与产品负责人确认
- 业务上下文变化时需要重新评估
- 技术依赖关系可能影响实施顺序
