# Controller 层规范

> 来源：aIk-coding-style 规范

## 请求方式规范

| 场景 | 请求方式 | 参数传递 | 返回值 |
|------|---------|---------|--------|
| 分页查询 | POST | `@RequestBody` QueryDTO | VO 对象 |
| 查询详情 | GET | `@RequestParam` | VO 对象 |
| 新增数据 | POST | `@RequestBody` DTO | VO 对象 |
| 修改数据 | POST | `@RequestBody` DTO | VO 对象 |
| 删除数据 | POST | `@RequestBody` IdDto | Boolean |
| 文件上传 | POST | `@RequestParam MultipartFile` | VO 对象 |

**规则说明：**
- **POST 请求**：所有 POST 接口参数必须通过 `@RequestBody` 传递对象（文件上传除外，MultipartFile 受 Spring 限制必须用 `@RequestParam`）
- **GET 请求**：仅用于不涉及数据库操作且参数单一的查询接口，参数使用 `@RequestParam`
- **涉及数据库操作**：增删改、分页查询、多参数查询一律使用 POST + `@RequestBody`

**分页查询说明：**
- 使用 POST 请求，支持复杂查询条件扩展
- QueryDTO 继承项目通用分页基类（如有），或自行定义包含 `current` 和 `size` 字段
- 示例：
```java
@Data
public class {Entity}QueryDto {
    //note 分页参数（通常继承项目通用基类）
    private Long current = 1L;
    private Long size = 20L;
    
    //note 查询条件字段
    private String keyword;
    private LocalDate startDate;
    private LocalDate endDate;
}
```

## Controller 示例

```java
package {package}.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * -anchor {Controller描述}
 *
 * @author a I k .
 * @version 1.0.0
 * @implNote JDK 8
 * @apiNote
 * @since {yyyy/MM/dd}
 * -
 */
@Slf4j
@RequiredArgsConstructor
//note 老项目使用：@Api(tags = "{模块}管理")
//note 新项目使用：@Tag(name = "{模块}管理")
@RestController
@RequestMapping("/{主模块}/{功能}")
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    //note POST 分页查询，使用 DTO 传参支持查询条件扩展
    //note 老项目使用：@ApiOperation("分页查询{Entity}")
    //note 新项目使用：@Operation(summary = "分页查询{Entity}")
    @PostMapping("/findPage")
    public ApiResponse<Page<{Entity}Vo>> findPage(@RequestBody @Validated {Entity}QueryDto queryDto) {
        log.info("分页查询{Entity}，参数：{}", queryDto);
        Page<{Entity}Vo> result = {entity}Service.findPage(queryDto);
        return ApiResponse.success(result);
    }

    //note GET 查询详情
    //note 老项目使用：@ApiOperation("查询{Entity}详情")
    //note 新项目使用：@Operation(summary = "查询{Entity}详情")
    @GetMapping("/findById")
    public ApiResponse<{Entity}Vo> findById(@RequestParam Long id) {
        log.info("查询{Entity}详情，ID：{}", id);
        {Entity}Vo result = {entity}Service.findById(id);
        return ApiResponse.success(result);
    }

    //note POST 新增数据，DTO 传参
    //note 老项目使用：@ApiOperation("新增{Entity}")
    //note 新项目使用：@Operation(summary = "新增{Entity}")
    @PostMapping("/add")
    public ApiResponse<{Entity}Vo> add(@RequestBody @Validated {Entity}Dto dto) {
        log.info("新增{Entity}，参数：{}", dto);
        {Entity}Vo result = {entity}Service.add(dto);
        return ApiResponse.success(result);
    }

    //note POST 修改数据，DTO 传参
    //note 老项目使用：@ApiOperation("修改{Entity}")
    //note 新项目使用：@Operation(summary = "修改{Entity}")
    @PostMapping("/modify")
    public ApiResponse<{Entity}Vo> modify(@RequestBody @Validated {Entity}Dto dto) {
        log.info("修改{Entity}，参数：{}", dto);
        {Entity}Vo result = {entity}Service.modify(dto);
        return ApiResponse.success(result);
    }

    //note POST 删除数据
    //note 老项目使用：@ApiOperation("删除{Entity}")
    //note 新项目使用：@Operation(summary = "删除{Entity}")
    @PostMapping("/remove")
    public ApiResponse<Boolean> remove(@RequestBody @Validated IdDto idDto) {
        log.info("删除{Entity}，ID：{}", idDto.getId());
        Boolean result = {entity}Service.remove(idDto.getId());
        return ApiResponse.success(result);
    }

    //note POST 文件上传，MultipartFile 接收
    //note 老项目使用：@ApiOperation("上传文件")
    //note 新项目使用：@Operation(summary = "上传文件")
    @PostMapping("/upload")
    public ApiResponse<{Entity}Vo> upload(@RequestParam("file") MultipartFile file) {
        log.info("上传文件，文件名：{}", file.getOriginalFilename());
        {Entity}Vo result = {entity}Service.upload(file);
        return ApiResponse.success(result);
    }
}
```

## Controller层规范

- 使用 `@RestController` + `@RequestMapping`
- **请求路径格式：** `/{主模块}/{功能}`，如 `/sample/history`、`/xray/detection`
- **请求方式：**
  - GET：仅用于不涉及数据库操作且参数单一的查询（如根据 ID 查询详情）
  - POST：用于分页查询、新增、修改、删除、文件上传、多参数查询等所有涉及数据库操作
- **参数传递：**
  - POST 请求：统一使用 `@RequestBody` 传递对象（文件上传除外，`MultipartFile` 受 Spring 限制必须用 `@RequestParam`）
  - GET 请求：使用 `@RequestParam` 传递单一参数
  - 分页查询：使用 `@RequestBody` QueryDTO，支持复杂查询条件扩展
  - 批量操作：使用 `@RequestBody` 传递 ID 列表对象
  - **不使用 `@PathVariable`**，所有参数通过 `@RequestParam` 或 `@RequestBody` 传递
- **参数校验：** POST 接口 `@RequestBody` 入参必须标注 `@Validated`，配合 DTO 上的 JSR-303 注解
- **返回值：** 使用 VO 对象封装，避免使用 Map
- **依赖注入：** 使用 `private final` + `@RequiredArgsConstructor`（Lombok 构造器注入）
- **API 文档注解（根据项目选择）：**
  - 老项目：`@Api(tags = "模块名称")` + `@ApiOperation("接口描述")`
  - 新项目：`@Tag(name = "模块名称")` + `@Operation(summary = "接口描述")`
- 不使用 `/api` 前缀
- **Controller 层使用 `ApiResponse` 封装 Service 返回结果**
- **注意：** 不同项目可能使用不同的响应封装类（如 `Result`、`Response` 等），需根据项目规范调整
