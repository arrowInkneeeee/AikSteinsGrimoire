# 代码安全审查 测试场景

> 针对技能: `code-security-reviewer`
> 测试类型: 技术型
> 规范来源: [aIk-coding-style](../../aIk-coding-style/SKILL.md)

---

## 测试场景概述

| 测试用例 | 场景描述 | 预期违规数量 | 严重级别 |
|---------|---------|-------------|---------|
| TC-01 | SQL注入：使用${}字符串拼接 | 1 | 严重 |
| TC-02 | 越权访问：未校验数据归属 | 1 | 严重 |
| TC-03 | 敏感数据泄露：日志输出密码等敏感信息 | 2 | 警告 |
| TC-04 | 文件上传：未限制文件类型和大小 | 2 | 警告 |
| TC-05 | 正确代码：安全措施均已到位 | 0 | - |

---

## TC-01: SQL注入

### 违规代码片段

```java
package com.example.order.dao;

import com.example.order.common.po.OrderPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * -anchor 订单Mapper
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderPo> {

    //note 根据订单号查询（存在SQL注入风险）
    @Select("SELECT * FROM t_order WHERE order_no = '${orderNo}'")
    OrderPo selectByOrderNo(@Param("orderNo") String orderNo);

    //note 动态排序查询（存在SQL注入风险）
    @Select("SELECT * FROM t_order ORDER BY ${sortField} ${sortOrder}")
    List<OrderPo> selectOrdered(@Param("sortField") String sortField,
                                 @Param("sortOrder") String sortOrder);
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | `@Select` 中使用 `${orderNo}` | 使用 `$()` 拼接用户输入导致SQL注入。攻击者可注入 `' OR '1'='1` 获取全表数据 | 严重 |
| 2 | `ORDER BY ${sortField}` 直接拼接动态排序字段 | 排序字段应使用白名单校验，防止注入恶意SQL | 严重 |

### 修复建议

```java
@Mapper
public interface OrderMapper extends BaseMapper<OrderPo> {

    //note 使用 #{} 参数化查询防止SQL注入
    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo}")
    OrderPo selectByOrderNo(@Param("orderNo") String orderNo);

    // 动态排序通过Service层白名单校验后传入安全字段名
    // 或使用 MyBatis-Plus 的 orderBy(true, field) 安全排序
}
```

```java
// Service层白名单校验
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("CREATE_TIME", "TOTAL_AMOUNT");

public List<OrderPo> selectOrdered(String sortField, String sortOrder) {
    if (sortField == null || !ALLOWED_SORT_FIELDS.contains(sortField)) {
        throw new BusinessException("非法的排序字段");
    }
    if (!"ASC".equalsIgnoreCase(sortOrder) && !"DESC".equalsIgnoreCase(sortOrder)) {
        throw new BusinessException("非法的排序方向");
    }
    return lambdaQuery()
            .orderBy(true, "ASC".equalsIgnoreCase(sortOrder), 
                     sortField.equals("CREATE_TIME") ? OrderPo::getCreateTime : OrderPo::getTotalAmount)
            .list();
}
```

---

## TC-02: 越权访问

### 违规代码片段

```java
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    //note 查询订单详情（未校验数据归属）
    @GetMapping("/findById")
    public ApiResponse<OrderVo> findById(@RequestParam Long id) {
        // 任何用户都可以查询任意订单，存在越权风险
        OrderVo result = orderService.findById(id);
        return ApiResponse.success(result);
    }

    //note 删除订单（未校验权限）
    @PostMapping("/remove")
    public ApiResponse<Boolean> remove(@RequestParam Long id) {
        // 任何用户都可以删除任意订单
        Boolean result = orderService.remove(id);
        return ApiResponse.success(result);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 查询操作未校验数据归属 | 用户可能查询到其他用户的订单数据，造成数据泄露 | 严重 |
| 2 | 删除操作未校验权限 | 任意用户可删除他人订单，造成数据破坏 | 严重 |

### 修复建议

```java
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    //note 查询时校验数据归属
    @GetMapping("/findById")
    public ApiResponse<OrderVo> findById(@RequestParam Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        OrderVo result = orderService.findByIdAndUserId(id, currentUserId);
        return ApiResponse.success(result);
    }

    //note 删除时校验权限
    @PostMapping("/remove")
    public ApiResponse<Boolean> remove(@RequestParam Long id) {
        Long currentUserId = UserContext.getCurrentUserId();
        Boolean result = orderService.removeByIdAndUserId(id, currentUserId);
        return ApiResponse.success(result);
    }
}
```

```java
// Service层增加归属校验
@Override
public OrderVo findByIdAndUserId(Long id, Long userId) {
    OrderPo entity = lambdaQuery()
            .eq(OrderPo::getId, id)
            .eq(OrderPo::getUserId, userId)  // 校验归属
            .one();
    if (entity == null) {
        throw new BusinessException("订单不存在");
    }
    return OrderVo.of(entity);
}
```

---

## TC-03: 敏感数据日志输出

### 违规代码片段

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserMapper userMapper;

    //note 用户登录（日志泄露敏感信息）
    public UserVo login(String phone, String password) {
        // 日志中明文输出了手机号和密码
        log.info("用户登录，手机号：{}，密码：{}", phone, password);

        UserPo user = lambdaQuery()
                .eq(UserPo::getPhone, phone)
                .eq(UserPo::getPassword, password)  // 明文密码比较
                .one();

        if (user == null) {
            log.warn("登录失败，手机号：{}，输入的密码：{}", phone, password);
            throw new BusinessException("用户名或密码错误");
        }

        log.info("登录成功，用户信息：{}", user);  // 用户对象可能包含敏感字段
        return UserVo.of(user);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 日志中明文输出密码 `log.info("...密码：{}", password)` | 密码等敏感信息不应出现在日志中，应脱敏处理或使用 `******` 替代 | 警告 |
| 2 | 日志中输出完整用户对象 `log.info("...用户信息：{}", user)` | 用户对象可能包含手机号、身份证号等敏感信息，应使用脱敏后的 VO 输出 | 警告 |
| 3 | 明文存储密码 | 密码应使用 BCrypt 等算法加密存储，不应直接比较明文 | 严重 |

### 修复建议

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    //note 用户登录（安全处理）
    public UserVo login(String phone, String password) {
        // 脱敏输出：手机号部分隐藏
        log.info("用户登录，手机号：{}", DesensitizeUtil.mobilePhone(phone));

        UserPo user = lambdaQuery()
                .eq(UserPo::getPhone, phone)
                .one();

        if (user == null) {
            log.warn("登录失败，手机号：{}", DesensitizeUtil.mobilePhone(phone));
            throw new BusinessException("用户名或密码错误");
        }

        // 使用密码编码器比对
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("密码错误，手机号：{}", DesensitizeUtil.mobilePhone(phone));
            throw new BusinessException("用户名或密码错误");
        }

        log.info("登录成功，用户ID：{}", user.getId());
        return UserVo.of(user);  // VO已做脱敏处理
    }
}
```

---

## TC-04: 文件上传无类型校验

### 违规代码片段

```java
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    //note 文件上传（无类型限制）
    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        // 未校验文件类型，攻击者可上传JSP/EXE等恶意文件
        // 未校验文件大小，可能导致存储耗尽

        // 使用原始文件名，可能导致路径穿越
        String path = "/uploads/" + file.getOriginalFilename();

        try {
            file.transferTo(new File(path));
            log.info("文件上传成功，路径：{}", path);
            return ApiResponse.success(path);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| 1 | 未校验文件类型 | 攻击者可上传 JSP、war、exe 等可执行文件，获取服务器权限 | 严重 |
| 2 | 未校验文件大小 | 可能导致存储耗尽，造成拒绝服务攻击 | 警告 |
| 3 | 直接使用原始文件名 | 可能被利用进行路径穿越攻击（如 `../../etc/passwd`） | 严重 |

### 修复建议

```java
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    //note 允许上传的文件类型白名单
    private static final Set<String> ALLOWED_TYPES = 
            Set.of("image/jpeg", "image/png", "image/gif", "application/pdf");

    //note 最大文件大小：10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        //note 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        //note 校验文件类型
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            log.warn("不支持的文件类型：{}", file.getContentType());
            throw new BusinessException("不支持的文件类型");
        }

        //note 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件大小超过限制：{}", file.getSize());
            throw new BusinessException("文件大小不能超过10MB");
        }

        //note 生成随机文件名防止覆盖和路径穿越
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!FilenameUtils.isExtension(file.getOriginalFilename(), 
                Set.of("jpg", "jpeg", "png", "gif", "pdf"))) {
            throw new BusinessException("不支持的文件扩展名");
        }
        String filename = UUID.randomUUID().toString() + "." + ext;

        //note 保存到安全目录
        String safeDir = "/uploads/secure/";
        String path = safeDir + filename;

        try {
            file.transferTo(new File(path));
            log.info("文件上传成功，路径：{}", path);
            return ApiResponse.success(path);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }
    }
}
```

---

## TC-05: 正确代码（安全措施均已到位）

### 正确代码片段

```java
package com.example.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.desensitized.DesensitizeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.order.common.dto.OrderDto;
import com.example.order.common.po.OrderPo;
import com.example.order.common.vo.OrderVo;
import com.example.order.dao.OrderMapper;
import com.example.order.service.OrderService;
import com.example.order.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * -anchor 订单服务实现类
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since 2026/05/13
 * -
 */
@Slf4j
@Service("order.OrderService")
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderPo>
        implements OrderService {

    //note 分页排序字段白名单
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("CREATE_TIME", "TOTAL_AMOUNT");

    private final OrderMapper orderMapper;

    @Override
    public OrderVo findById(Long id) {
        //note 获取当前用户并校验数据归属
        Long currentUserId = UserContext.getCurrentUserId();

        OrderPo entity = lambdaQuery()
                .eq(OrderPo::getId, id)
                .eq(OrderPo::getUserId, currentUserId)  // 校验归属
                .one();

        if (entity == null) {
            log.warn("订单不存在或无权访问，ID：{}，用户ID：{}", id, currentUserId);
            throw new BusinessException("订单不存在");
        }

        //note 脱敏后记录日志
        log.info("查询订单详情，订单号：{}", 
                DesensitizeUtil.bankCard(entity.getOrderNo()));

        return OrderVo.of(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo createOrder(OrderDto dto) {
        Long currentUserId = UserContext.getCurrentUserId();

        //note 日志脱敏，不输出敏感字段
        log.info("创建订单，用户ID：{}，商品ID：{}", 
                currentUserId, dto.getItemId());

        OrderPo entity = new OrderPo();
        BeanUtil.copyProperties(dto, entity);
        entity.setUserId(currentUserId);
        save(entity);

        return OrderVo.of(entity);
    }
}
```

### 预期违规

| # | 违规项 | 说明 | 严重级别 |
|---|--------|------|---------|
| - | 无违规 | 所有安全措施均已正确到位 | - |

### 通过项清单

- [x] 无 SQL 注入（使用 MyBatis-Plus lambdaQuery 参数化）
- [x] 查询操作校验数据归属（`eq(OrderPo::getUserId, currentUserId)`）
- [x] 日志无敏感信息泄露（使用 `DesensitizeUtil` 脱敏）
- [x] 密码等敏感字段不输出日志
- [x] 使用 `UserContext` 获取当前用户，避免参数传递用户标识
- [x] 排序字段使用白名单校验

---

## RED-GREEN-REFACTOR 执行参考

### RED阶段（无技能）

```
预期结果: 模型可能仅检测到 SQL 注入和明文密码
- TC-01 (SQL注入): 通常能检测到
- TC-02 (越权): 容易遗漏
- TC-03 (日志泄露): 容易被忽略
- TC-04 (文件上传): 可能检测到缺少类型校验，但细节遗漏
- 漏检率预估: 40-55%
```

### GREEN阶段（加载技能）

```
验证标准:
- 违规检测率 > 90%
- 能准确识别 SQL注入、越权、日志泄露、上传安全等
- 误报率 < 10%
- 能区分不同敏感级别的安全风险
```

### REFACTOR阶段（迭代收紧）

```
常见遗漏:
- 对间接SQL拼接的检测（如 StringBuilder 拼接）
- 对文件上传路径穿越的检测
- 对返回VO中敏感字段未脱敏的检测
- 对防重放机制缺失的检测
- 对幂等性缺失的检测
```
