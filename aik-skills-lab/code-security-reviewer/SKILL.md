---
name: code-security-reviewer
description: 代码安全审查（SQL注入、越权）
type: Skill
version: 1.0.0
---

# code-security-reviewer

## 输入

- **业务代码**: code-implementer 实现的代码

## 输出

安全审查报告

## 检查项

### SQL 注入

#### ${} 拼接

```java
// 错误：使用 ${} 拼接 SQL
@Select("SELECT * FROM T_ORDER WHERE ORDER_NO = '${orderNo}'")
OrderEntity selectByOrderNo(String orderNo);

// 攻击：orderNo = "' OR '1'='1"
// 结果：SELECT * FROM T_ORDER WHERE ORDER_NO = '' OR '1'='1'

// 正确：使用 #{} 参数化
@Select("SELECT * FROM T_ORDER WHERE ORDER_NO = #{orderNo}")
OrderEntity selectByOrderNo(String orderNo);
```

#### 动态排序

```java
// 错误：动态排序字段直接拼接
public List<OrderEntity> list(OrderQueryDTO dto) {
    String sql = "SELECT * FROM T_ORDER ORDER BY " + dto.getSortField();
    // 攻击：sortField = "1; DROP TABLE T_ORDER; --"
}

// 正确：白名单校验
private static final Set<String> ALLOWED_SORT_FIELDS = 
        Set.of("CREATE_TIME", "TOTAL_AMOUNT");

public List<OrderEntity> list(OrderQueryDTO dto) {
    String sortField = dto.getSortField();
    if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
        throw new BusinessException("非法排序字段");
    }
    String sql = "SELECT * FROM T_ORDER ORDER BY " + sortField;
}
```

#### 动态表名

```java
// 错误：动态表名直接拼接
@Select("SELECT * FROM ${tableName} WHERE ID = #{id}")
Object selectById(String tableName, Long id);

// 正确：使用枚举限制
public enum TableName {
    T_ORDER("T_ORDER"),
    T_USER("T_USER");
    
    private final String name;
    // ...
}

@Select("SELECT * FROM #{tableName} WHERE ID = #{id}")
Object selectById(@Param("tableName") String tableName, Long id);
```

**检查点**：
- [ ] 无 ${} 拼接参数
- [ ] 动态排序字段白名单校验
- [ ] 动态表名使用枚举限制

### 越权访问

#### 数据归属校验

```java
// 错误：未校验数据归属
@Override
public OrderVO getById(Long id) {
    OrderEntity entity = orderMapper.selectById(id);
    return convertToVO(entity);  // 可能查到其他用户的订单
}

// 正确：校验数据归属
@Override
public OrderVO getById(Long id, Long userId) {
    OrderEntity entity = lambdaQuery()
            .eq(OrderEntity::getId, id)
            .eq(OrderEntity::getUserId, userId)  // 校验归属
            .one();
    if (entity == null) {
        throw new BusinessException("订单不存在");
    }
    return convertToVO(entity);
}

// 或在 Service 层校验
@Override
public OrderVO getById(Long id) {
    Long currentUserId = UserContext.getCurrentUserId();
    OrderEntity entity = lambdaQuery()
            .eq(OrderEntity::getId, id)
            .eq(OrderEntity::getUserId, currentUserId)
            .one();
    // ...
}
```

#### 权限校验

```java
// 错误：未校验权限
@DeleteMapping("/{id}")
public Result<Boolean> delete(@PathVariable Long id) {
    boolean success = orderService.deleteById(id);  // 任何人可删除
    return Result.success(success);
}

// 正确：校验权限
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")  // 只有管理员可删除
public Result<Boolean> delete(@PathVariable Long id) {
    boolean success = orderService.deleteById(id);
    return Result.success(success);
}

// 或在 Service 层校验
@Override
public boolean deleteById(Long id) {
    Long currentUserId = UserContext.getCurrentUserId();
    OrderEntity order = getById(id);
    
    // 只能删除自己的订单，或管理员可删除所有
    if (!order.getUserId().equals(currentUserId) 
            && !UserContext.isAdmin()) {
        throw new BusinessException("无权限删除此订单");
    }
    
    return removeById(id);
}
```

**检查点**：
- [ ] 查询操作校验数据归属
- [ ] 修改/删除操作校验权限
- [ ] 敏感操作二次确认

### 敏感数据

#### 密码存储

```java
// 错误：明文存储
public void createUser(String username, String password) {
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setPassword(password);  // 明文存储！
    userMapper.insert(user);
}

// 正确：加密存储
@Autowired
private PasswordEncoder passwordEncoder;

public void createUser(String username, String password) {
    UserEntity user = new UserEntity();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));  // BCrypt 加密
    userMapper.insert(user);
}
```

#### 日志脱敏

```java
// 错误：日志打印敏感信息
log.info("用户登录，手机号：{}，密码：{}", phone, password);

// 正确：敏感信息脱敏
log.info("用户登录，手机号：{}，密码：{}", 
        DesensitizeUtil.mobilePhone(phone),  // 138****1234
        "******");

// 或使用注解脱敏
@Data
public class UserVO {
    @Desensitize(type = DesensitizeType.MOBILE)
    private String phone;
    
    @Desensitize(type = DesensitizeType.PASSWORD)
    private String password;
}
```

#### 返回数据脱敏

```java
// 错误：返回敏感信息
@Data
public class UserVO {
    private Long id;
    private String username;
    private String password;  // 返回密码！
    private String idCard;    // 返回身份证号！
}

// 正确：敏感字段不返回
@Data
public class UserVO {
    private Long id;
    private String username;
    // 密码不返回
    
    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;  // 返回脱敏后的身份证号
}
```

**检查点**：
- [ ] 密码加密存储（BCrypt）
- [ ] 日志无敏感信息
- [ ] 返回数据脱敏

### 其他安全

#### 防重放

```java
// 错误：无防重放机制
@PostMapping
public Result<OrderVO> create(@RequestBody OrderCreateDTO dto) {
    // 可重复提交，产生重复订单
    return Result.success(orderService.createOrder(dto));
}

// 正确：Token 防重放
@PostMapping
public Result<OrderVO> create(@RequestBody OrderCreateDTO dto) {
    // 1. 校验 Token
    String token = dto.getToken();
    if (!tokenService.validateAndConsume(token)) {
        throw new BusinessException("重复提交");
    }
    
    // 2. 创建订单
    return Result.success(orderService.createOrder(dto));
}

// 或使用幂等键
@PostMapping
public Result<OrderVO> create(@RequestBody OrderCreateDTO dto) {
    String idempotentKey = dto.getIdempotentKey();
    
    // 1. 检查是否已处理
    if (redisTemplate.hasKey("idempotent:" + idempotentKey)) {
        throw new BusinessException("重复提交");
    }
    
    // 2. 创建订单
    OrderVO vo = orderService.createOrder(dto);
    
    // 3. 标记已处理
    redisTemplate.opsForValue().set("idempotent:" + idempotentKey, "1", 24, TimeUnit.HOURS);
    
    return Result.success(vo);
}
```

#### 文件上传

```java
// 错误：无类型限制
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    String path = "/uploads/" + file.getOriginalFilename();
    file.transferTo(new File(path));  // 可上传任意文件！
    return Result.success(path);
}

// 正确：限制文件类型
private static final Set<String> ALLOWED_TYPES = 
        Set.of("image/jpeg", "image/png", "image/gif");

@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    // 1. 校验文件类型
    if (!ALLOWED_TYPES.contains(file.getContentType())) {
        throw new BusinessException("不支持的文件类型");
    }
    
    // 2. 校验文件大小
    if (file.getSize() > 10 * 1024 * 1024) {
        throw new BusinessException("文件大小超过限制");
    }
    
    // 3. 生成随机文件名，防止覆盖
    String ext = FilenameUtils.getExtension(file.getOriginalFilename());
    String filename = UUID.randomUUID() + "." + ext;
    
    // 4. 保存到安全目录
    String path = "/uploads/images/" + filename;
    file.transferTo(new File(path));
    
    return Result.success(path);
}
```

#### 防刷

```java
// 错误：无防刷机制
@PostMapping("/send-sms")
public Result<Boolean> sendSms(@RequestParam String phone) {
    smsService.send(phone);  // 可无限发送
    return Result.success(true);
}

// 正确：限流防刷
@PostMapping("/send-sms")
@RateLimiter(key = "#phone", limit = 1, period = 60)  // 每分钟1次
public Result<Boolean> sendSms(@RequestParam String phone) {
    smsService.send(phone);
    return Result.success(true);
}

// 或使用 Redis 计数
public void sendSms(String phone) {
    String key = "sms:" + phone;
    Long count = redisTemplate.opsForValue().increment(key);
    
    if (count == 1) {
        // 第一次，设置过期时间
        redisTemplate.expire(key, 1, TimeUnit.MINUTES);
    }
    
    if (count > 3) {
        throw new BusinessException("发送过于频繁，请稍后再试");
    }
    
    // 发送短信
}
```

**检查点**：
- [ ] 敏感操作防重放
- [ ] 文件上传限制类型和大小
- [ ] 接口限流防刷

## 输出格式

```markdown
## 代码安全审查报告

### 审查概览

| 检查项 | 通过 | 警告 | 失败 |
|--------|------|------|------|
| SQL 注入 | 15 | 1 | 0 |
| 越权访问 | 12 | 0 | 1 |
| 敏感数据 | 8 | 2 | 0 |
| 其他安全 | 10 | 1 | 0 |

### SQL 注入

✅ **通过**
- 所有查询使用 #{} 参数化
- 无 ${} 拼接

⚠️ **警告**
- OrderMapper.list() 动态排序字段未白名单校验（第 45 行）

### 越权访问

✅ **通过**
- 查询操作校验数据归属

❌ **失败**
- OrderController.delete() 未校验权限（第 78 行）
- 任何用户可删除任意订单

### 敏感数据

✅ **通过**
- 密码加密存储

⚠️ **警告**
- OrderService.createOrder() 日志打印用户手机号（第 56 行）
- UserVO 返回身份证号未脱敏（第 89 行）

### 其他安全

✅ **通过**
- 文件上传限制类型

⚠️ **警告**
- OrderController.create() 无防重放机制（第 102 行）

### 修复建议

**高优先级**：
1. OrderController.delete() 添加权限校验（第 78 行）

**中优先级**：
1. OrderMapper.list() 添加排序字段白名单（第 45 行）
2. OrderController.create() 添加防重放机制（第 102 行）

**低优先级**：
1. 日志脱敏手机号（第 56 行）
2. UserVO 身份证号脱敏（第 89 行）
```

## 严重级别

| 级别 | 说明 | 示例 |
|------|------|------|
| **严重** | 必须立即修复 | SQL 注入、越权访问 |
| **警告** | 建议修复 | 敏感信息泄露、无防重放 |
| **提示** | 可选修复 | 日志脱敏 |

## 注意事项

- SQL 注入是严重安全漏洞，必须杜绝
- 越权访问会导致数据泄露，要严格校验
- 敏感数据要加密存储、脱敏展示
- 防重放、防刷是业务安全的基础
