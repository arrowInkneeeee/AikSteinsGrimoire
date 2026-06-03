---
name: user-story-generator
description: 当需要将功能点转化为"As a... I want... So that..."标准格式的用户故事时使用。适用于"生成用户故事"、"写用户故事"、"转成用户故事"、"用户故事化"等场景。
type: Skill
version: 1.0.0
---

# user-story-generator

## 输入

- **功能点列表**: 已解决冲突的功能点

## 输出

用户故事列表

```json
{
  "user_stories": [
    {
      "id": "US-001",
      "role": "用户角色",
      "want": "想要的功能",
      "so_that": "业务价值",
      "related_features": ["F001"],
      "story_points": null
    }
  ]
}
```

## 工作流

1. **分析功能点的用户角色**
   - 识别功能的使用者
   - 定义用户角色（如：普通用户、管理员、访客等）
   - 如角色不明确，使用通用角色

2. **确定用户目标（Want）**
   - 提取功能的核心动作
   - 使用用户视角描述
   - 避免技术实现细节

3. **推导业务价值（So that）**
   - 分析功能带来的收益
   - 连接业务目标
   - 回答"为什么需要这个功能"

4. **生成标准格式用户故事**
   - 使用 As a... I want... So that... 格式
   - 确保故事独立、可测试
   - 关联原始功能点

5. **分配故事编号**
   - 格式：US-XXX
   - 按功能依赖关系排序

## 标准格式

```
Story ID: US-001
As a [用户角色]
I want [功能描述]
So that [业务价值]

Related Features: [F001, F002]
```

## 用户角色定义

| 角色 | 描述 | 典型场景 |
|------|------|---------|
| 用户/客户 | 系统的最终使用者 | 浏览、购买、使用功能 |
| 管理员 | 系统管理人员 | 配置、监控、维护 |
| 运营人员 | 业务运营人员 | 上架商品、处理订单 |
| 访客 | 未登录用户 | 浏览公开内容 |
| VIP/会员 | 付费用户 | 使用高级功能 |

## 调用规则

- 在 conflict-detector 之后调用
- 每个核心功能点生成至少一个用户故事
- 复杂功能可拆分为多个用户故事

## 示例

### 输入

```json
{
  "features": [
    {
      "id": "F001",
      "description": "用户可以浏览商品",
      "category": "core"
    },
    {
      "id": "F002",
      "description": "购物车功能",
      "category": "core"
    },
    {
      "id": "F003",
      "description": "支持微信支付",
      "category": "core"
    },
    {
      "id": "F004",
      "description": "后台管理系统",
      "category": "auxiliary"
    }
  ]
}
```

### 输出

```json
{
  "user_stories": [
    {
      "id": "US-001",
      "role": "顾客",
      "want": "浏览商品列表和详情",
      "so_that": "了解商品信息并决定是否购买",
      "related_features": ["F001"],
      "story_points": null
    },
    {
      "id": "US-002",
      "role": "顾客",
      "want": "将商品添加到购物车",
      "so_that": "统一结算多个商品",
      "related_features": ["F002"],
      "story_points": null
    },
    {
      "id": "US-003",
      "role": "顾客",
      "want": "使用微信支付订单",
      "so_that": "方便快捷地完成付款",
      "related_features": ["F003"],
      "story_points": null
    },
    {
      "id": "US-004",
      "role": "运营人员",
      "want": "通过后台管理系统管理商品",
      "so_that": "高效地上架、更新和下架商品",
      "related_features": ["F004"],
      "story_points": null
    }
  ]
}
```

## 文本格式输出

```
Story ID: US-001
As a 顾客
I want 浏览商品列表和详情
So that 了解商品信息并决定是否购买

Related Features: F001

---

Story ID: US-002
As a 顾客
I want 将商品添加到购物车
So that 统一结算多个商品

Related Features: F002

---

Story ID: US-003
As a 顾客
I want 使用微信支付订单
So that 方便快捷地完成付款

Related Features: F003

---

Story ID: US-004
As a 运营人员
I want 通过后台管理系统管理商品
So that 高效地上架、更新和下架商品

Related Features: F004
```

## 注意事项

- 保持用户故事的独立性（INVEST 原则）
- 避免技术实现细节进入用户故事
- 业务价值要明确且可衡量
- 复杂功能可拆分为多个小用户故事
