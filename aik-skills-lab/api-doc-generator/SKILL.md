---
name: api-doc-generator
description: 生成 Markdown 接口文档，便于前端联调
type: Skill
version: 1.0.0
---

# api-doc-generator

## 输入

- **接口设计**: SDD 中的接口设计
- **Controller 代码**: 实际实现的 Controller

## 输出

API 文档（Markdown 格式）

## 文档结构

```markdown
# {模块名} API 文档

## 目录

1. [接口列表](#接口列表)
2. [接口详情](#接口详情)
   - [1. 创建订单](#1-创建订单)
   - [2. 查询订单详情](#2-查询订单详情)
   - ...
3. [错误码说明](#错误码说明)

## 接口列表

| 序号 | 接口 | 方法 | URL | 说明 | 状态 |
|-----|------|------|-----|------|------|
| 1 | 创建订单 | POST | /orders | 创建新订单 | ✅ 已完成 |
| 2 | 查询订单详情 | GET | /orders/{id} | 根据ID查询订单 | ✅ 已完成 |
| 3 | 查询订单列表 | GET | /orders | 分页查询订单列表 | ✅ 已完成 |
| 4 | 取消订单 | PUT | /orders/{id}/cancel | 取消订单 | 🚧 开发中 |

## 接口详情

### 1. 创建订单

#### 基本信息
- **接口URL**: `/orders`
- **请求方法**: POST
- **Content-Type**: application/json
- **接口状态**: ✅ 已完成

#### 请求头
| 参数名 | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | Bearer {token} |
| Content-Type | 是 | application/json |

#### 请求参数

##### Body 参数
| 参数名 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
|--------|------|---------|--------|------|------|
| userId | Long | 是 | - | 用户ID | 1 |
| items | Array | 是 | - | 商品列表 | - |
| items[].productId | Long | 是 | - | 商品ID | 1 |
| items[].quantity | Integer | 是 | - | 数量 | 2 |
| addressId | Long | 是 | - | 收货地址ID | 1 |
| remark | String | 否 | - | 备注 | 请尽快发货 |

#### 请求示例

```json
{
  "userId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ],
  "addressId": 1,
  "remark": "请尽快发货"
}
```

#### 响应参数

##### 成功响应 (HTTP 200)
| 参数名 | 类型 | 说明 | 示例 |
|--------|------|------|------|
| code | Integer | 状态码，200表示成功 | 200 |
| message | String | 提示信息 | "success" |
| data | Object | 订单数据 | - |
| data.id | Long | 订单ID | 1 |
| data.orderNo | String | 订单号 | "202403180001" |
| data.totalAmount | BigDecimal | 订单金额 | 199.99 |
| data.status | Integer | 订单状态：0-待支付 | 0 |
| data.statusDesc | String | 状态描述 | "待支付" |
| data.createTime | String | 创建时间 | "2024-03-18 10:30:00" |

##### 失败响应
| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 错误码 |
| message | String | 错误信息 |

#### 响应示例

##### 成功
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "202403180001",
    "totalAmount": 199.99,
    "status": 0,
    "statusDesc": "待支付",
    "createTime": "2024-03-18 10:30:00"
  }
}
```

##### 参数错误 (400)
```json
{
  "code": 400,
  "message": "参数校验失败：用户ID不能为空"
}
```

##### 库存不足 (1002)
```json
{
  "code": 1002,
  "message": "库存不足"
}
```

#### 时序图

```
前端 -> Controller: POST /orders
Controller -> Service: createOrder(dto)
Service -> StockService: 扣减库存
StockService --> Service: 结果
Service -> Mapper: 保存订单
Mapper --> Service: 订单ID
Service --> Controller: OrderVO
Controller --> 前端: Result<OrderVO>
```

---

### 2. 查询订单详情

#### 基本信息
- **接口URL**: `/orders/{id}`
- **请求方法**: GET
- **接口状态**: ✅ 已完成

#### 请求参数

##### Path 参数
| 参数名 | 类型 | 是否必填 | 说明 | 示例 |
|--------|------|---------|------|------|
| id | Long | 是 | 订单ID | 1 |

#### 请求示例

> 本接口为 GET 请求，无 Body 请求体。请求参数见上方表格，示例值：`id = 1`

#### 响应示例

##### 成功
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "202403180001",
    "userId": 1,
    "totalAmount": 199.99,
    "status": 1,
    "statusDesc": "已支付",
    "items": [
      {
        "productId": 1,
        "productName": "iPhone 15",
        "price": 99.99,
        "quantity": 2,
        "subTotal": 199.98
      }
    ],
    "createTime": "2024-03-18 10:30:00",
    "payTime": "2024-03-18 10:35:00"
  }
}
```

##### 订单不存在 (404)
```json
{
  "code": 404,
  "message": "订单不存在"
}
```

---

### 3. 查询订单列表

#### 基本信息
- **接口URL**: `/orders`
- **请求方法**: GET
- **接口状态**: ✅ 已完成

#### 请求参数

##### Query 参数
| 参数名 | 类型 | 是否必填 | 默认值 | 说明 | 示例 |
|--------|------|---------|--------|------|------|
| pageNum | Integer | 否 | 1 | 页码 | 1 |
| pageSize | Integer | 否 | 10 | 每页大小 | 10 |
| status | Integer | 否 | - | 状态筛选 | 0 |
| startDate | String | 否 | - | 开始日期 | "2024-03-01" |
| endDate | String | 否 | - | 结束日期 | "2024-03-18" |

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "orderNo": "202403180001",
        "totalAmount": 199.99,
        "status": 0,
        "createTime": "2024-03-18 10:30:00"
      }
    ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

---

## 错误码说明

### 系统错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|---------|
| 200 | 成功 | - |
| 400 | 参数错误 | 检查请求参数 |
| 401 | 未授权 | 检查 Token 是否有效 |
| 403 | 禁止访问 | 检查用户权限 |
| 404 | 资源不存在 | 检查资源ID是否正确 |
| 429 | 请求过于频繁 | 稍后重试 |
| 500 | 系统错误 | 联系后端开发 |

### 业务错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|---------|
| 1001 | 订单不存在 | 检查订单ID |
| 1002 | 库存不足 | 减少购买数量 |
| 1003 | 订单状态不允许操作 | 检查订单当前状态 |
| 1004 | 商品不存在 | 检查商品ID |
| 1005 | 收货地址不存在 | 检查地址ID |

---

## 更新日志

| 版本 | 日期 | 更新内容 | 作者 |
|------|------|---------|------|
| v1.0.0 | 2024-03-18 | 初始版本，包含订单基础接口 | developer |
| v1.0.1 | 2024-03-20 | 新增取消订单接口 | developer |
```

## 生成规则

1. **从 Controller 提取信息**
   - URL：@RequestMapping
   - 方法：@GetMapping/@PostMapping 等
   - 参数：@PathVariable/@RequestParam/@RequestBody

2. **从 DTO 提取字段**
   - 字段名、类型
   - 校验注解：@NotNull/@NotBlank 等
   - 说明从注释提取

3. **从代码提取示例**
   - 请求示例：构造示例数据（所有必填字段必须出现，值必须为真实可用数据）
   - 响应示例：根据 VO 构造（必须覆盖成功场景，建议覆盖常见错误场景）
   - **强制要求：每个接口必须同时包含请求示例和响应示例，缺一不可**
   - GET 单参数接口无 Body 时，在参数表格的"示例"列填写示例值，并必须提供响应示例

4. **从常量提取错误码**
   - ErrorCode 类

## 输出位置

```
docs/
└── api/
    ├── order-api.md
    ├── user-api.md
    └── README.md
```

或放在模块内：

```
src/main/resources/docs/
└── order-api.md
```

## 注意事项

- 文档使用 Markdown，便于版本控制
- 包含接口列表，便于快速查找
- 请求/响应参数使用表格，清晰明了
- **每个接口必须同时提供请求示例和响应示例，GET 单参数接口请求示例可简化为参数表格中的示例值，但响应示例不可省略**
- 请求示例中的字段值必须是真实可用的测试数据，禁止使用无意义占位符
- 响应示例必须覆盖成功场景，建议同时覆盖常见错误场景
- 提供完整示例，便于前端理解
- 包含错误码说明，便于问题排查
- 记录更新日志，便于追踪变更
