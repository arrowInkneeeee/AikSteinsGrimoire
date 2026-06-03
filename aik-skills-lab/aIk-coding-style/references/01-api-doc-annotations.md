# API 文档注解规范

> 来源：aIk-coding-style 规范 · Swagger/SpringDoc注解使用规范

根据项目情况选择合适的 API 文档方案：

| 项目类型 | 依赖 | 注解方案 |
|---------|------|---------|
| 老项目 | 已有 Swagger 依赖 | 沿用 Swagger 注解 |
| 新项目 | 无 API 文档依赖 | 使用 SpringDoc OpenAPI |

### 方案一：Swagger（老项目）

如果项目已存在 Swagger 依赖（`springfox-swagger2`），沿用原有注解风格。

**实体类 (PO)：**
```java
@ApiModel("类描述")
public class {Entity}Po {
    @ApiModelProperty("字段描述")
    private String fieldName;
}
```

**Controller：**
```java
@Api(tags = "模块名称")
@RestController
public class {Entity}Controller {
    
    @ApiOperation("接口描述")
    @PostMapping("/findPage")
    public ApiResponse<Page<EntityPo>> findPage(@RequestBody QueryDto dto) {
        // ...
    }
}
```

### 方案二：SpringDoc OpenAPI（新项目）

新项目推荐使用 SpringDoc OpenAPI（Swagger 的继任者）。

**添加依赖：**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>
```

**添加配置（`application.yml`）：**
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs
  show-actuator: true
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
```

**配置说明：**

| 配置项 | 说明 |
|--------|------|
| `swagger-ui.path` | Swagger UI 访问路径，默认 `/swagger-ui.html` |
| `swagger-ui.tags-sorter: alpha` | 标签按字母顺序排序 |
| `swagger-ui.operations-sorter: alpha` | 接口按字母顺序排序 |
| `api-docs.path` | API 文档 JSON 路径，默认 `/v3/api-docs` |
| `show-actuator: true` | 显示 Actuator 端点 |
| `default-consumes-media-type` | 默认请求 Content-Type |
| `default-produces-media-type` | 默认响应 Content-Type |

**访问地址：**
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- API Docs：`http://localhost:8080/v3/api-docs`

**实体类 (PO)：**
```java
@Schema(description = "类描述")
public class {Entity}Po {
    @Schema(description = "字段描述")
    private String fieldName;
}
```

**Controller：**
```java
@Tag(name = "模块名称")
@RestController
public class {Entity}Controller {
    
    @Operation(summary = "接口描述")
    @PostMapping("/findPage")
    public ApiResponse<Page<EntityPo>> findPage(@RequestBody QueryDto dto) {
        // ...
    }
}
```

### 注解对照表

| 功能 | Swagger | SpringDoc |
|------|---------|-----------|
| 类描述 | `@ApiModel` | `@Schema` |
| 字段描述 | `@ApiModelProperty` | `@Schema` |
| 模块标签 | `@Api(tags = "")` | `@Tag(name = "")` |
| 接口描述 | `@ApiOperation` | `@Operation` |
| 参数描述 | `@ApiParam` | `@Parameter` |
