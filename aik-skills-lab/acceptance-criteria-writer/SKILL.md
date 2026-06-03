---
name: acceptance-criteria-writer
description: 当需要为每个用户故事编写清晰可测的Gherkin格式验收标准（Given-When-Then）时使用。适用于"写验收标准"、"编写AC"、"验收条件"、"Gherkin场景"等场景。
type: Skill
version: 1.0.0
---

# acceptance-criteria-writer

## 输入

- **用户故事**: user-story-generator 的输出

## 输出

Gherkin 格式验收标准

```json
{
  "acceptance_criteria": [
    {
      "story_id": "US-001",
      "feature_name": "功能名称",
      "scenarios": [
        {
          "type": "normal|error|boundary",
          "description": "场景描述",
          "given": ["前置条件"],
          "when": ["操作步骤"],
          "then": ["预期结果"]
        }
      ]
    }
  ]
}
```

## 工作流

1. **分析用户故事的功能边界**
   - 理解功能范围
   - 识别输入输出
   - 确定状态变化

2. **识别正常流程场景**
   - 主流程（Happy Path）
   - 标准用户操作序列
   - 预期成功结果

3. **识别异常流程场景**
   - 错误输入处理
   - 权限不足场景
   - 系统异常处理
   - 网络中断等边界情况

4. **识别边界条件**
   - 空值处理
   - 最大值/最小值
   - 并发场景
   - 数据量限制

5. **生成 Gherkin 格式验收标准**
   - 使用 Given-When-Then 格式
   - 每个场景独立可测试
   - 语言清晰无歧义

## Gherkin 格式规范

```gherkin
Feature: [功能名称]

  Background:
    Given [共享前置条件]

  Scenario: [正常场景描述]
    Given [前置条件]
    And [更多前置条件]
    When [用户操作]
    And [更多操作]
    Then [预期结果]
    And [更多结果]

  Scenario: [异常场景描述]
    Given [前置条件]
    When [错误操作]
    Then [错误提示或处理结果]
```

## 场景类型

| 类型 | 描述 | 数量建议 |
|------|------|---------|
| normal | 正常流程，主路径 | 1-2 个 |
| error | 错误处理，异常路径 | 2-3 个 |
| boundary | 边界条件，极限情况 | 1-2 个 |

## 调用规则

- 为每个用户故事生成验收标准
- 场景要覆盖主要功能和边界情况
- 保持 Given-When-Then 的单一职责

## 示例

### 输入

```json
{
  "user_stories": [
    {
      "id": "US-001",
      "role": "顾客",
      "want": "将商品添加到购物车",
      "so_that": "统一结算多个商品",
      "related_features": ["F002"]
    }
  ]
}
```

### 输出

```json
{
  "acceptance_criteria": [
    {
      "story_id": "US-001",
      "feature_name": "添加商品到购物车",
      "scenarios": [
        {
          "type": "normal",
          "description": "成功添加商品到购物车",
          "given": [
            "用户已登录",
            "商品库存充足",
            "用户位于商品详情页"
          ],
          "when": [
            "用户点击"加入购物车"按钮",
            "用户选择数量为 1"
          ],
          "then": [
            "商品被添加到购物车",
            "显示"添加成功"提示",
            "购物车数量增加 1"
          ]
        },
        {
          "type": "normal",
          "description": "添加多个相同商品",
          "given": [
            "用户已登录",
            "商品库存充足",
            "购物车中已有该商品 1 件"
          ],
          "when": [
            "用户再次点击"加入购物车"按钮",
            "用户选择数量为 2"
          ],
          "then": [
            "购物车中该商品数量变为 3",
            "显示"添加成功"提示"
          ]
        },
        {
          "type": "error",
          "description": "库存不足",
          "given": [
            "用户已登录",
            "商品库存为 0"
          ],
          "when": [
            "用户点击"加入购物车"按钮"
          ],
          "then": [
            "显示"商品库存不足"提示",
            "商品未被添加到购物车"
          ]
        },
        {
          "type": "error",
          "description": "未登录用户",
          "given": [
            "用户未登录"
          ],
          "when": [
            "用户点击"加入购物车"按钮"
          ],
          "then": [
            "弹出登录对话框",
            "提示用户先登录"
          ]
        },
        {
          "type": "boundary",
          "description": "超出最大购买限制",
          "given": [
            "用户已登录",
            "商品限购 5 件",
            "购物车中已有该商品 4 件"
          ],
          "when": [
            "用户点击"加入购物车"按钮",
            "用户选择数量为 2"
          ],
          "then": [
            "显示"超出购买限制"提示",
            "只添加 1 件到购物车",
            "购物车中该商品数量变为 5"
          ]
        }
      ]
    }
  ]
}
```

## 文本格式输出

```gherkin
Feature: 添加商品到购物车
  Story ID: US-001

  Scenario: 成功添加商品到购物车
    Given 用户已登录
    And 商品库存充足
    And 用户位于商品详情页
    When 用户点击"加入购物车"按钮
    And 用户选择数量为 1
    Then 商品被添加到购物车
    And 显示"添加成功"提示
    And 购物车数量增加 1

  Scenario: 添加多个相同商品
    Given 用户已登录
    And 商品库存充足
    And 购物车中已有该商品 1 件
    When 用户再次点击"加入购物车"按钮
    And 用户选择数量为 2
    Then 购物车中该商品数量变为 3
    And 显示"添加成功"提示

  Scenario: 库存不足
    Given 用户已登录
    And 商品库存为 0
    When 用户点击"加入购物车"按钮
    Then 显示"商品库存不足"提示
    And 商品未被添加到购物车

  Scenario: 未登录用户
    Given 用户未登录
    When 用户点击"加入购物车"按钮
    Then 弹出登录对话框
    And 提示用户先登录

  Scenario: 超出最大购买限制
    Given 用户已登录
    And 商品限购 5 件
    And 购物车中已有该商品 4 件
    When 用户点击"加入购物车"按钮
    And 用户选择数量为 2
    Then 显示"超出购买限制"提示
    And 只添加 1 件到购物车
    And 购物车中该商品数量变为 5
```

## 注意事项

- 每个场景应该是独立的，不依赖其他场景
- Given 描述状态，When 描述动作，Then 描述结果
- 避免在 When 中使用"如果"，用不同场景表示分支
- 验收标准应该是可测试的，避免模糊表述
