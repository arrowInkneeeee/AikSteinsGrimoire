# MoSCoW 优先级决策矩阵

## MoSCoW 分类定义

| 优先级 | 全称 | 定义 | 决策标准 |
|--------|------|------|---------|
| **Must have** | Must have this | 必须实现，否则项目失败 | 核心功能、法律合规、关键业务流程 |
| **Should have** | Should have this | 应该实现，重要但可妥协 | 重要功能，有替代方案，延期不影响发布 |
| **Could have** | Could have this | 可以实现，有则更好 | 增值功能，用户期望但非必需 |
| **Won't have** | Won't have this time | 本次不实现，未来考虑 | 超出范围、资源不足、低价值 |

## 评估维度权重

| 维度 | 权重 | 描述 |
|------|------|------|
| business_value | **40%** | 对业务目标的贡献程度 |
| user_impact | **30%** | 影响的用户范围和使用频率 |
| technical_dependency | **20%** | 是否被其他功能依赖 |
| implementation_cost | **10%** | 实现成本（复杂度、工期） |

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

## 评估流程

1. **分析业务价值** — 评估对核心业务的贡献、收入影响、战略对齐度
2. **分析用户影响范围** — 受影响用户比例、使用频率、痛点解决程度
3. **分析技术依赖关系** — 识别被依赖的故事、确定基础功能、标记阻塞项
4. **应用 MoSCoW 分类** — 根据多维度评估结果分类，处理冲突和边界情况
5. **生成优先级排序列表** — 按优先级分组，组内按依赖关系排序

## 输出格式

```json
{
  "prioritized_stories": [
    {
      "story_id": "US-001",
      "priority": "must|should|could|wont",
      "moscow_category": "Must have",
      "business_value": "high|medium|low",
      "user_impact": "high|medium|low",
      "technical_dependency": true,
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

## 注意事项

- Must Have 不应超过总工作量的 60%
- 优先级需要与产品负责人确认
- 业务上下文变化时需要重新评估
- 技术依赖关系可能影响实施顺序
