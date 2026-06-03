---
name: api-test-generator
description: 为Spring Boot项目生成Markdown格式的API接口测试文档，包含请求参数、响应示例、测试用例，用于配合Apipost等工具进行接口测试。不生成代码，仅输出可读的接口文档。
type: Skill
version: 1.0.0
---

# API Test Generator

## Purpose

为Spring Boot项目的Controller接口生成Markdown格式的API测试文档，便于前端开发人员使用Apipost等工具进行接口测试。

## When to Use

- 需要为前端提供接口测试文档
- 接口开发完成后，需要整理接口契约
- 需要记录接口的请求/响应示例
- 需要定义接口的测试用例

## Output Format

仅生成Markdown文档，不生成可执行代码。文档可直接复制到Apipost的描述字段或作为独立接口文档使用。

## Document Structure

每个接口生成如下内容：

```markdown
## 接口名称

### 基本信息
- **接口地址**: POST /api/v1/orders
- **Content-Type**: application/json
- **认证方式**: Bearer Token / 无

### 请求参数

#### Path参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 订单ID |

#### Query参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页大小，默认10 |

#### Body参数
```json
{
  "userId": 100,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

### 响应示例

#### 成功响应 (200)
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 10001,
    "orderNo": "ORD202403180001",
    "totalAmount": 199.99
  }
}
```

#### 错误响应 (400)
```json
{
  "code": 400,
  "message": "参数校验失败: 用户ID不能为空",
  "data": null
}
```

### 测试用例

| 用例编号 | 用例名称 | 请求参数 | 预期结果 |
|----------|----------|----------|----------|
| TC01 | 正常创建订单 | 完整有效参数 | 返回200，orderId不为空 |
| TC02 | 缺少必填参数 | 不传userId | 返回400，提示用户ID不能为空 |
| TC03 | 用户不存在 | userId=99999 | 返回400，提示用户不存在 |
```

## Generation Rules

### 1. 解析Controller

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @PostMapping
    public Result<OrderVO> createOrder(@RequestBody @Valid OrderDTO dto) {
        // ...
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id) {
        // ...
    }

    @GetMapping
    public Result<PageDTO<OrderVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        // ...
    }
}
```

### 2. 提取DTO字段

```java
@Data
public class OrderDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotEmpty(message = "订单项不能为空")
    private List<OrderItemDTO> items;

    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
```

转换为文档：

```markdown
#### Body参数
| 字段名 | 类型 | 必填 | 约束 | 说明 |
|--------|------|------|------|------|
| userId | Long | 是 | - | 用户ID |
| items | Array | 是 | 不能为空 | 订单项列表 |
| items[].productId | Long | 是 | - | 商品ID |
| items[].quantity | Integer | 是 | >0 | 数量 |
| remark | String | 否 | 最大500字 | 备注 |
```

### 3. 生成请求示例

```markdown
### 请求示例

```http
POST /api/v1/orders HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer {token}

{
  "userId": 100,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "remark": "请尽快发货"
}
```
```

### 4. 生成测试用例

基于校验注解和常见场景生成测试用例：

```markdown
### 测试用例

| 用例编号 | 用例名称 | 请求参数 | 预期结果 |
|----------|----------|----------|----------|
| TC01 | 正常创建订单 | 完整有效参数 | HTTP 200，code=200，data.orderId不为空 |
| TC02 | 缺少userId | 不传userId | HTTP 400，message包含"用户ID不能为空" |
| TC03 | items为空数组 | items: [] | HTTP 400，message包含"订单项不能为空" |
| TC04 | items为null | 不传items | HTTP 400，message包含"订单项不能为空" |
| TC05 | quantity为0 | items[0].quantity: 0 | HTTP 400，message包含数量校验失败 |
| TC06 | remark超长 | remark: 501个字符 | HTTP 400，message包含"不能超过500字" |
| TC07 | 用户不存在 | userId: 99999 | HTTP 200，code=400，提示用户不存在 |
```

### 5. 复用项目Result类

```markdown
### 响应结构说明

本项目使用统一的Result包装响应：

```java
public class Result<T> {
    private Integer code;      // 业务状态码，200表示成功
    private String message;    // 提示信息
    private T data;           // 业务数据
}
```

HTTP状态码始终为200，业务错误通过code字段区分。
```

## Output Location

```
docs/api-test/
├── OrderController-API-Test.md
├── UserController-API-Test.md
└── ...
```

## Example Output

```markdown
# OrderController 接口测试文档

## 1. 创建订单

### 基本信息
- **接口地址**: POST /api/v1/orders
- **Content-Type**: application/json
- **认证方式**: Bearer Token

### 请求参数

#### Body参数
| 字段名 | 类型 | 必填 | 约束 | 说明 |
|--------|------|------|------|------|
| userId | Long | 是 | - | 用户ID |
| items | Array | 是 | 非空 | 订单商品列表 |
| items[].productId | Long | 是 | - | 商品ID |
| items[].quantity | Integer | 是 | >0 | 购买数量 |
| remark | String | 否 | 最大500字 | 订单备注 |

### 请求示例

```http
POST /api/v1/orders HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

{
  "userId": 100,
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
  "remark": "请尽快发货"
}
```

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 10001,
    "orderNo": "ORD202403180001",
    "totalAmount": 299.97,
    "status": 1,
    "createTime": "2024-03-18T10:30:00"
  }
}
```

#### 错误响应 - 参数校验失败
```json
{
  "code": 400,
  "message": "参数校验失败: 用户ID不能为空",
  "data": null
}
```

#### 错误响应 - 业务异常
```json
{
  "code": 400,
  "message": "用户不存在",
  "data": null
}
```

### 测试用例

| 用例编号 | 用例名称 | 前置条件 | 请求参数 | 预期结果 |
|----------|----------|----------|----------|----------|
| TC01 | 正常创建订单 | 用户ID=100存在 | 完整有效参数 | code=200，data.orderId>0，data.orderNo不为空 |
| TC02 | 缺少userId | - | 不传userId | code=400，message包含"用户ID不能为空" |
| TC03 | items为空数组 | - | items: [] | code=400，message包含"订单项不能为空" |
| TC04 | quantity为负数 | - | quantity: -1 | code=400，message包含数量校验失败 |
| TC05 | 用户不存在 | - | userId: 99999 | code=400，message="用户不存在" |
| TC06 | 商品不存在 | - | productId: 99999 | code=400，message="商品不存在" |
| TC07 | 库存不足 | 商品1库存=1 | quantity: 2 | code=400，message="库存不足" |

---

## 2. 查询订单详情

### 基本信息
- **接口地址**: GET /api/v1/orders/{id}
- **Content-Type**: application/json
- **认证方式**: Bearer Token

### 请求参数

#### Path参数
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 订单ID |

### 响应示例

#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 10001,
    "orderNo": "ORD202403180001",
    "userId": 100,
    "totalAmount": 299.97,
    "status": 1,
    "items": [
      {
        "productId": 1,
        "productName": "iPhone 15",
        "quantity": 2,
        "unitPrice": 99.99
      }
    ],
    "createTime": "2024-03-18T10:30:00"
  }
}
```

### 测试用例

| 用例编号 | 用例名称 | 请求参数 | 预期结果 |
|----------|----------|----------|----------|
| TC01 | 查询存在的订单 | id: 10001 | code=200，data不为null |
| TC02 | 查询不存在的订单 | id: 99999 | code=200，data=null |
| TC03 | id为负数 | id: -1 | code=400，参数校验失败 |

---

## 3. 分页查询订单列表

### 基本信息
- **接口地址**: GET /api/v1/orders
- **Content-Type**: application/json
- **认证方式**: Bearer Token

### 请求参数

#### Query参数
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码，从1开始 |
| size | Integer | 否 | 10 | 每页大小，最大100 |
| status | Integer | 否 | - | 订单状态筛选 |
| startTime | String | 否 | - | 开始时间，格式：yyyy-MM-dd HH:mm:ss |
| endTime | String | 否 | - | 结束时间，格式：yyyy-MM-dd HH:mm:ss |

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "orderId": 10001,
        "orderNo": "ORD202403180001",
        "totalAmount": 299.97,
        "status": 1,
        "createTime": "2024-03-18T10:30:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

### 测试用例

| 用例编号 | 用例名称 | 请求参数 | 预期结果 |
|----------|----------|----------|----------|
| TC01 | 默认分页查询 | 无参数 | current=1，size=10，records不为null |
| TC02 | 指定页码和大小 | page=2，size=20 | current=2，size=20 |
| TC03 | 按状态筛选 | status=1 | records中所有status=1 |
| TC04 | 时间范围筛选 | startTime=2024-01-01，endTime=2024-12-31 | records在范围内 |
| TC05 | page为0 | page=0 | code=400，页码不能小于1 |
| TC06 | size超过100 | size=200 | code=400，每页大小不能超过100 |
```

---

## 使用说明

1. 将本文档导入Apipost：复制接口信息到Apipost的接口描述中
2. 根据测试用例创建测试集合
3. 设置环境变量（如baseUrl、token）
4. 按顺序执行测试用例
```

## Notes

- 文档基于Controller代码和DTO校验注解自动生成
- 测试用例覆盖正常场景、参数校验场景、业务异常场景
- 响应示例基于实际返回类型（VO）生成
- 分页参数复用项目中的PageDTO结构
