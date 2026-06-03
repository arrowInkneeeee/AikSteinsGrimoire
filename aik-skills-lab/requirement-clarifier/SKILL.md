---
name: requirement-clarifier
description: 当需要识别需求中的模糊、不完整之处并生成针对性澄清问题时使用。适用于"澄清需求"、"需求有没有不清楚的地方"、"识别模糊点"、"追问需求细节"等场景。
type: Skill
version: 1.0.0
---

# requirement-clarifier

## 输入

- **功能点列表**: requirement-extractor 的输出

## 输出

澄清问题列表

```json
{
  "clarifications": [
    {
      "feature_id": "F001",
      "feature_description": "功能描述",
      "ambiguity_type": "quantitative|range|role|condition|priority",
      "question": "具体问题",
      "suggested_options": ["选项1", "选项2"],
      "priority": "high|medium|low"
    }
  ]
}
```

## 工作流

1. **分析每个功能点的明确性**
   - 逐条检查功能描述
   - 标记潜在模糊点

2. **识别模糊词汇**
   - **量化模糊**: "快速"、"很多"、"大量"、"少量"
   - **范围模糊**: "等"、"等等"、"之类"、"相关"
   - **角色模糊**: 缺少明确的用户角色
   - **条件模糊**: 缺少触发条件或前置条件
   - **优先级模糊**: "可能"、"考虑"、"以后"

3. **识别缺失信息**
   - 用户角色未明确
   - 业务规则不完整
   - 边界条件缺失
   - 性能指标未定义
   - 异常处理未说明

4. **生成针对性澄清问题**
   - 每个模糊点生成 1-3 个具体问题
   - 提供建议选项（如适用）
   - 说明为什么需要澄清

5. **按重要性排序问题**
   - **high**: 影响核心功能设计
   - **medium**: 影响实现细节
   - **low**: 优化类问题

## 模糊类型检测

| 类型 | 描述 | 示例 | 检测关键词 |
|------|------|------|-----------|
| quantitative | 缺少具体数值 | "快速加载" | 快、慢、多、少、大、小 |
| range | 缺少边界定义 | "支持多种格式" | 等、等等、多种、相关 |
| role | 缺少用户角色 | "可以导出报表" | 缺主语的动作描述 |
| condition | 缺少触发条件 | "显示警告信息" | 无条件的状态描述 |
| priority | 缺少重要性标识 | "可以考虑优化" | 可能、考虑、以后、将来 |

## 调用规则

- 接收 requirement-extractor 的输出后自动调用
- 生成的问题需要向用户展示并收集回答
- 用户回答后，更新功能点列表并传递给下一个技能

## 示例

### 输入

```json
{
  "features": [
    {
      "id": "F001",
      "description": "用户可以浏览商品",
      "category": "core",
      "source": "用户可以快速浏览商品"
    },
    {
      "id": "F002",
      "description": "支持多种支付方式",
      "category": "core",
      "source": "要支持微信支付和支付宝"
    }
  ]
}
```

### 输出

```json
{
  "clarifications": [
    {
      "feature_id": "F001",
      "feature_description": "用户可以浏览商品",
      "ambiguity_type": "quantitative",
      "question": "'快速浏览'的具体性能要求是什么？",
      "suggested_options": [
        "页面加载时间 < 2秒",
        "页面加载时间 < 3秒",
        "首屏渲染 < 1秒"
      ],
      "priority": "high"
    },
    {
      "feature_id": "F002",
      "feature_description": "支持多种支付方式",
      "ambiguity_type": "range",
      "question": "除微信和支付宝外，还需要支持哪些支付方式？",
      "suggested_options": [
        "仅微信和支付宝",
        "增加银联支付",
        "增加信用卡支付",
        "后续再扩展"
      ],
      "priority": "medium"
    }
  ]
}
```

## 人机协作节点

此技能触发人机协作，需要：

1. 向用户展示所有澄清问题
2. 收集用户回答
3. 根据回答更新功能点列表
4. 继续执行后续技能

## 注意事项

- 避免过度追问，聚焦关键模糊点
- 提供建议选项可加速澄清过程
- 记录澄清历史便于追溯
