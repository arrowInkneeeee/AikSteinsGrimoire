---
name: api-designer
description: 当需要设计RESTful API接口时使用，包括定义请求参数、响应结构、状态码和URL路径。适用于规划新接口、制定接口规范、统一项目API风格等场景。
type: Skill
version: 1.0.0
---

# api-designer

## 输入

- **用户故事**: 需求分析阶段输出的用户故事
- **验收标准**: Gherkin 格式的验收标准

## 输出

API 设计文档

## 工作流

1. **从用户故事提取 API 操作**
   - 识别 CRUD 操作
   - 识别业务操作
   - 确定操作权限

2. **设计 URL 和 HTTP 方法**
   - URL 使用名词复数
   - HTTP 方法区分操作类型

3. **设计请求参数**
   - Path 参数（资源 ID）
   - Query 参数（筛选、分页）
   - Body 参数（创建、更新）

4. **设计响应数据结构**
   - 复用项目已有的 Result 类
   - 定义 VO 结构

5. **定义错误码**
   - HTTP 状态码
   - 业务错误码

6. **输出 API 设计文档**

## RESTful 规范

### URL 设计

| 操作 | HTTP 方法 | URL | 说明 |
|------|----------|-----|------|
| 创建 | POST | /orders | `@RequestBody` DTO 创建资源 |
| 查询列表 | POST | /orders/findPage | `@RequestBody` QueryDTO 分页查询 |
| 查询详情 | GET | /orders/findById | `@RequestParam` 根据 ID 查询 |
| 更新 | POST | /orders/modify | `@RequestBody` DTO 全量更新 |
| 部分更新 | POST | /orders/modifyPartial | `@RequestBody` DTO 部分更新 |
| 删除 | POST | /orders/remove | `@RequestBody` IdDto 删除资源 |
| 业务操作 | POST | /orders/cancel | `@RequestBody` DTO 业务动作 |
| 复杂查询 | POST | /orders/search | `@RequestBody` DTO 复杂条件查询 |

### URL 命名规范

- 使用小写字母：`/orders` 而非 `/Orders`
- 使用动词表示动作：`/orders/findById`、`/orders/findPage`、`/orders/add`
- 参数不放在路径中：不使用 `/orders/{id}`，统一通过 `@RequestParam` 或 `@RequestBody` 传递

## 统一返回格式

**必须复用项目已有的 Result 类**，设计时需确认：

```java
// 常见格式 1
data class Result<T>(
    val code: Int,      // 200 表示成功
    val message: String,
    val data: T
)

// 常见格式 2
data class Result<T>(
    val code: String,   // "0" 表示成功
    val msg: String,
    val data: T
)
```

**确认项**：
- code 字段类型：Int / String
- 成功码：200 / 0 / "0"
- message 字段名：message / msg
- data 字段名：data / result

## 分页规范

**必须复用项目已有的分页参数**，常见形式：

```java
// 形式 1：MyBatis-Plus Page
public class PageDTO {
    private Integer pageNum = 1;   // 或 pageNo / current
    private Integer pageSize = 10; // 或 size / limit
}

// 形式 2：自定义
public class PageDTO {
    private Integer current = 1;
    private Integer size = 10;
    private Integer maxLimit = 100; // 最大限制
}
```

**确认项**：
- 页码字段名：pageNum / pageNo / current
- 每页大小字段名：pageSize / size / limit
- 最大页码限制（如有）
- 返回字段名：list / records / data

## JSR-303 校验

```java
@Data
public class OrderCreateDTO {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotEmpty(message = "商品列表不能为空")
    private List<OrderItemDTO> items;
    
    @NotNull(message = "收货地址不能为空")
    private Long addressId;
    
    @Min(value = 1, message = "数量必须大于0")
    @Max(value = 999, message = "数量不能超过999")
    private Integer quantity;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;
    
    @Email(message = "邮箱格式错误")
    private String email;
    
    @Size(max = 200, message = "备注长度不能超过200")
    private String remark;
}
```

**Controller 使用**：

```java
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public Result<OrderVO> create(
            @RequestBody @Validated OrderCreateDTO dto) {
        log.info("创建订单，参数：{}", dto);
        OrderVO vo = orderService.createOrder(dto);
        return Result.success(vo);
    }
    
    @GetMapping("/findById")
    public Result<OrderVO> getById(@RequestParam Long id) {
        OrderVO vo = orderService.getById(id);
        return Result.success(vo);
    }
    
    @PostMapping("/findPage")
    public Result<PageResult<OrderVO>> list(@RequestBody @Validated OrderQueryDTO dto) {
        PageResult<OrderVO> result = orderService.list(dto);
        return Result.success(result);
    }
}
```

## 错误码定义

| 错误码 | 说明 | 场景 |
|--------|------|------|
| 200 | 成功 | 正常返回 |
| 400 | 参数错误 | 参数校验失败 |
| 401 | 未授权 | 未登录或 Token 失效 |
| 403 | 禁止访问 | 无权限 |
| 404 | 资源不存在 | 数据不存在 |
| 429 | 请求过于频繁 | 限流 |
| 500 | 系统错误 | 服务器内部错误 |
| 1001 | 订单不存在 | 业务错误 |
| 1002 | 库存不足 | 业务错误 |
| 1003 | 订单状态不允许操作 | 业务错误 |

## 输出格式

```markdown
## 接口设计

### 3.1 API 列表

| 接口 | 方法 | URL | 说明 | 权限 |
|------|------|-----|------|------|
| 创建订单 | POST | /orders | 创建新订单 | 登录用户 |
| 查询订单列表 | POST | /orders/findPage | 分页查询 | 登录用户 |
| 查询订单详情 | GET | /orders/findById | 根据ID查询 | 登录用户 |
| 取消订单 | POST | /orders/cancel | 取消订单 | 登录用户 |

### 3.2 请求/响应定义

#### 创建订单

**Request**：
```java
@Data
public class OrderCreateDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotEmpty(message = "商品列表不能为空")
    private List<OrderItemDTO> items;
    
    @NotNull(message = "收货地址不能为空")
    private Long addressId;
}

@Data
public class OrderItemDTO {
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;
}
```

**Response**：
```java
Result<OrderVO>

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private LocalDateTime createTime;
}
```

#### 查询订单列表

**Request**：
```java
@Data
public class OrderQueryDTO extends PageDTO {
    private Integer status;        // 状态筛选（可选）
    private LocalDate startDate;   // 开始日期（可选）
    private LocalDate endDate;     // 结束日期（可选）
}
```

**Controller 使用**：
```java
@PostMapping("/findPage")
public Result<PageResult<OrderVO>> list(@RequestBody @Validated OrderQueryDTO dto) {
    PageResult<OrderVO> result = orderService.list(dto);
    return Result.success(result);
}
```

### 3.3 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 404 | 订单不存在 |
| 1001 | 库存不足 |
| 1002 | 订单状态不允许取消 |
```

## 注意事项

- 必须复用项目已有的 Result 和分页参数
- **URL 使用动词表示动作**，如 `/orders/findById`、`/orders/findPage`
- **参数不放在 URL 路径中**：不使用 `@PathVariable`，统一通过 `@RequestParam` 或 `@RequestBody` 传递
- **所有 POST 请求参数通过 `@RequestBody` 传递**（文件上传除外）
- **增删改查统一使用 POST**，仅单参数详情查询使用 GET
- POST 接口 `@RequestBody` 入参必须标注 `@Validated`
- 复杂查询使用 POST + `@RequestBody`
- 敏感操作需要权限校验
- 参数校验使用 JSR-303 注解
- 日志记录使用 `@Slf4j`，核心流程 `log.info`
