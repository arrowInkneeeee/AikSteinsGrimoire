# core 包

## 定位

技术基础设施层。无业务语义，被所有业务模块依赖。

## 目录结构

```
core/
├── po/               # 基础实体
│   ├── BaseEntity           # 审计字段（createTime/modifyTime/createBy/modifyBy）
│   └── BaseLogicEntity      # 继承 BaseEntity，加 deleted 软删除标志
├── dto/              # 统一数据传输对象
│   ├── ApiResponse<T>       # 接口统一返回（code/success/msg/data）
│   └── PageQuery            # 分页查询参数（自动处理默认值和最大值限制）
├── exception/        # 异常体系
│   ├── BusinessException    # 业务异常（支持 IResultCode / 自定义消息）
│   └── GlobalExceptionHandler  # 全局异常拦截（@RestControllerAdvice）
├── constant/         # 常量
│   └── PageConstant         # 分页默认值和最大值限制
├── enums/            # 枚举及契约接口
│   ├── IResultCode          # 响应码契约接口
│   ├── ResultCode           # 通用响应码枚举
│   ├── StatusEnum           # 通用状态枚举（ENABLE / DISABLE）
│   └── DeleteFlagEnum       # 删除标志枚举（NOT_DELETED / DELETED）
├── utils/            # 工具类（仅封装 Spring 上下文、业务断言、统一入口）
│   ├── SpringUtils          # Spring 上下文工具
│   ├── AssertUtils          # 业务断言（不满足抛 BusinessException）
│   ├── JsonUtils            # JSON 统一入口（封装 Fastjson）
│   ├── ServletUtils         # Web 请求上下文
│   └── IpUtils              # 客户端真实 IP（处理代理头）
└── config/           # 配置类
    ├── MyBatisPlusConfig    # 分页插件拦截器
    ├── JacksonConfig        # ObjectMapper 定制（日期格式/时区/忽略 null）
    ├── WebMvcConfig         # CORS 跨域配置
    ├── FileStorageConfig    # 文件存储配置项（@ConfigurationProperties）
    └── BaseMetaObjectHandler # MyBatis-Plus 自动填充处理器
```

## 使用示例

### 1. 实体继承 BaseEntity

```java
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ArticlePo extends BaseEntity {
    private Long id;
    private String title;
}
```

### 2. 接口返回 ApiResponse

```java
@PostMapping("/findPage")
public ApiResponse<Page<ArticleVo>> findPage(@RequestBody ArticleQuery query) {
    Page<ArticleVo> page = articleService.findPage(query.toPage(), query);
    return ApiResponse.success(page);
}
```

### 3. 业务断言

```java
public ArticleVo findById(Long id) {
    ArticlePo po = articleMapper.selectById(id);
    AssertUtils.notNull(po, "文章不存在");
    return ArticleVo.of(po);
}
```

### 4. 抛业务异常

```java
if (!hasPermission) {
    throw new BusinessException(ResultCode.FORBIDDEN);
}
```

## 设计原则

1. **无业务**：core 只放技术基础设施，不涉及任何业务规则。
2. **不造轮子**：字符串/集合/日期/文件等操作直接用 Hutool / Commons，不在 utils 里重复封装。
3. **统一入口**：JSON 序列化、异常体系、分页参数等通过 core 统一，便于后续替换实现。
4. **自动填充**：BaseEntity 的审计字段由 BaseMetaObjectHandler 自动处理，Service 层无需手动 set。
