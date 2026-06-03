---
name: bug-pattern-analyzer
description: 分析Java项目中的常见缺陷模式，基于代码审查和测试反馈识别问题根因，提供预防性建议和代码改进方案。帮助开发团队建立缺陷预防机制，减少同类问题重复发生。
type: Skill
version: 1.0.0
---

# Bug Pattern Analyzer

## Purpose

分析Java Spring Boot项目中的常见缺陷模式，基于代码审查和测试反馈识别问题根因，提供预防性建议和代码改进方案。

## When to Use

- 代码审查时发现潜在问题
- 测试失败需要分析根因
- 生产环境出现Bug需要复盘
- 需要建立团队缺陷预防清单

## Common Bug Patterns

### 1. 空指针异常 (NullPointerException)

**典型场景：**
```java
// 问题代码
String name = user.getName().trim();  // 如果getName()返回null，NPE

// 正确做法
String name = Optional.ofNullable(user.getName())
    .map(String::trim)
    .orElse("");
```

**预防措施：**
- 使用Optional处理可能为null的值
- 方法返回集合时返回空集合而非null
- 使用Objects.requireNonNull进行前置校验
- 启用IDE的null分析检查

### 2. 并发问题

**典型场景：**
```java
// 问题代码
public class OrderService {
    private int counter = 0;  // 非线程安全
    
    public void increment() {
        counter++;  // 非原子操作
    }
}

// 正确做法
public class OrderService {
    private AtomicInteger counter = new AtomicInteger(0);
    
    public void increment() {
        counter.incrementAndGet();
    }
}
```

**预防措施：**
- 识别共享可变状态
- 使用线程安全类（Atomic*, ConcurrentHashMap）
- 使用synchronized或ReentrantLock
- Spring Bean默认单例，注意状态管理

### 3. 资源泄漏

**典型场景：**
```java
// 问题代码
InputStream is = new FileInputStream("file.txt");
// 使用is...
// 忘记关闭，资源泄漏

// 正确做法
try (InputStream is = new FileInputStream("file.txt")) {
    // 使用is...
}  // 自动关闭
```

**预防措施：**
- 使用try-with-resources
- 使用@Cleanup（Lombok）
- 数据库连接、流、锁确保释放

### 4. SQL注入

**典型场景：**
```java
// 问题代码
@Select("SELECT * FROM user WHERE name = '" + name + "'")
User findByName(String name);  // 可被注入

// 正确做法
@Select("SELECT * FROM user WHERE name = #{name}")
User findByName(@Param("name") String name);
```

**预防措施：**
- 使用#{}参数绑定，避免${}
- 使用MyBatis-Plus条件构造器
- 对用户输入进行校验和转义

### 5. 事务问题

**典型场景：**
```java
// 问题代码
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        orderMapper.insert(order);
        // 异常被捕获，事务不回滚
        try {
            paymentService.pay(order);
        } catch (Exception e) {
            log.error("支付失败", e);  // 异常被吃掉了
        }
    }
}

// 正确做法
@Transactional
public void createOrder(Order order) {
    orderMapper.insert(order);
    paymentService.pay(order);  // 异常抛出，事务回滚
}

// 或明确标记回滚
@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    orderMapper.insert(order);
    try {
        paymentService.pay(order);
    } catch (Exception e) {
        log.error("支付失败", e);
        throw new BusinessException("支付失败", e);  // 包装后抛出
    }
}
```

**预防措施：**
- 理解@Transactional的传播行为和回滚规则
- 不要私自捕获异常而不处理
- 明确指定rollbackFor
- 同类方法调用不走代理，事务不生效

### 6. N+1查询问题

**典型场景：**
```java
// 问题代码
List<Order> orders = orderMapper.selectList();
for (Order order : orders) {
    // 每次循环都查询数据库
    User user = userMapper.selectById(order.getUserId());
    order.setUser(user);
}

// 正确做法：使用JOIN或批量查询
// 方案1：使用MyBatis的resultMap嵌套查询（配置fetchType="lazy"或"eager"）
// 方案2：手动JOIN查询
@Select("SELECT o.*, u.username, u.phone FROM t_order o " +
        "LEFT JOIN t_user u ON o.user_id = u.id")
@ResultMap("orderWithUserResultMap")
List<Order> selectListWithUser();

// 方案3：批量查询
List<Long> userIds = orders.stream()
    .map(Order::getUserId)
    .collect(Collectors.toList());
Map<Long, User> userMap = userMapper.selectBatchIds(userIds)
    .stream()
    .collect(Collectors.toMap(User::getId, Function.identity()));
for (Order order : orders) {
    order.setUser(userMap.get(order.getUserId()));
}
```

**预防措施：**
- 使用MyBatis-Plus的@TableField(select = false)控制字段
- 复杂查询使用自定义SQL和JOIN
- 使用分页避免大数据量查询
- 开启SQL日志，监控查询次数

### 7. 日期时间处理错误

**典型场景：**
```java
// 问题代码
Date now = new Date();  // 可读性差，容易有时区问题

// 正确做法（Java 8）
LocalDateTime now = LocalDateTime.now();  // 无时区
ZonedDateTime zdt = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));

// 存储到数据库
LocalDateTime createTime = LocalDateTime.now();

// 格式化输出
String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    .format(createTime);
```

**预防措施：**
- 使用Java 8日期时间API
- 明确时区处理
- 数据库字段使用DATETIME/TIMESTAMP
- 前后端约定统一格式（ISO-8601）

### 8. 类型转换异常

**典型场景：**
```java
// 问题代码
Long id = (Long) request.get("id");  // 可能是Integer，ClassCastException

// 正确做法
Long id = Long.valueOf(request.get("id").toString());
// 或使用MapStruct/Dozer进行类型安全转换
```

**预防措施：**
- 避免强制类型转换
- 使用类型安全的DTO
- 使用MapStruct进行对象映射
- 对输入参数进行校验

## Analysis Process

### 缺陷分析模板

```markdown
## 缺陷分析报告

### 基本信息
- **缺陷ID**: BUG-2024-001
- **发现时间**: 2024-03-18
- **发现阶段**: 测试/生产
- **严重程度**: 高/中/低

### 问题描述
简要描述缺陷现象

### 根因分析
1. 直接原因：
2. 深层原因：
3. 系统性原因：

### 代码定位
```java
// 问题代码位置
```

### 修复方案
```java
// 修复后的代码
```

### 预防措施
1. 代码层面：
2. 流程层面：
3. 工具层面：

### 检查清单更新
- [ ] 更新代码审查检查清单
- [ ] 更新团队规范文档
- [ ] 分享至团队会议
```

## Prevention Checklist

### 代码审查检查项

```markdown
## 代码审查缺陷预防清单

### 空值处理
- [ ] 方法参数是否进行null检查
- [ ] 方法返回值是否可能为null（文档说明）
- [ ] 链式调用是否存在NPE风险
- [ ] Optional使用是否规范

### 并发安全
- [ ] 是否存在共享可变状态
- [ ] 单例Bean是否有实例变量
- [ ] 并发集合使用是否正确
- [ ] 锁的粒度是否合适

### 资源管理
- [ ] 流是否正确关闭
- [ ] 数据库连接是否释放
- [ ] 锁是否正确释放
- [ ] 是否使用try-with-resources

### 数据访问
- [ ] 是否存在N+1查询
- [ ] SQL是否使用参数绑定
- [ ] 事务边界是否合理
- [ ] 分页参数是否正确

### 异常处理
- [ ] 异常是否被正确捕获和处理
- [ ] 是否捕获了过于宽泛的异常
- [ ] 异常信息是否有意义
- [ ] 是否吞掉了异常

### 类型安全
- [ ] 强制类型转换是否安全
- [ ] 泛型使用是否正确
- [ ] 原始类型是否避免使用
- [ ] 数字计算是否存在溢出

### 业务逻辑
- [ ] 边界条件是否处理
- [ ] 并发场景是否考虑
- [ ] 幂等性是否保证
- [ ] 状态流转是否合理
```

## Team Learning

### 缺陷复盘会议模板

```markdown
## 缺陷复盘会议

### 会议目标
从缺陷中学习，建立预防机制

### 议程
1. 缺陷描述（5分钟）
2. 根因分析（15分钟）
   - 5 Whys分析法
3. 影响评估（5分钟）
4. 修复方案（10分钟）
5. 预防措施（15分钟）
6. 行动项分配（5分钟）

### 输出物
1. 缺陷分析报告
2. 代码审查清单更新
3. 团队规范更新
4. 知识库文章
```

## Tool Integration

### 静态分析工具

```xml
<!-- SpotBugs -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.7.3.0</version>
</plugin>

<!-- PMD -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.20.0</version>
</plugin>

<!-- Checkstyle -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.2.0</version>
</plugin>
```

### 运行命令

```bash
# SpotBugs
mvn spotbugs:spotbugs spotbugs:gui

# PMD
mvn pmd:pmd

# Checkstyle
mvn checkstyle:check
```
