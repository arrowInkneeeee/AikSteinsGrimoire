# API 接口契约

> 生成时间: {yyyy-MM-dd}
> 模块: {模块名}
> 基础路径: /api/{module}

---

## 一、接口概览

| 序号 | 接口名称 | 方法 | URL | 说明 | 权限 |
|------|---------|------|-----|------|------|
| 1 | {接口名称} | {GET/POST/PUT/DELETE} | {URL路径} | {说明} | {权限} |
| 2 | {接口名称} | {GET/POST/PUT/DELETE} | {URL路径} | {说明} | {权限} |

---

## 二、通用约定

### 2.1 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码，200 表示成功 |
| message | String | 提示信息 |
| data | Object | 返回数据（成功时） |

### 2.2 分页请求参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 10 | 每页大小 |

### 2.3 分页返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 10
  }
}
```

---

## 三、接口详细定义

### 3.1 {接口名称}

#### 基本信息

| 项目 | 内容 |
|------|------|
| **URL** | {Method} {URL路径} |
| **说明** | {接口说明} |
| **权限** | {权限要求} |
| **Content-Type** | application/json |

#### 请求参数

##### Path 参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| {param} | {Type} | 是 | {说明} | {示例} |

##### Query 参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 | 示例 |
|--------|------|------|--------|------|------|
| {param} | {Type} | 否 | {default} | {说明} | {示例} |

##### Body 参数

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| {field} | {Type} | 是/否 | {说明} | {示例} |

#### 请求示例

```json
{
  "field1": "value1",
  "field2": 123
}
```

#### 请求 DTO

```java
@Data
@ApiModel("{DTO说明}")
public class {Xx}Dto {

    @ApiModelProperty(value = "{字段说明}", required = true)
    @NotNull(message = "{字段}不能为空")
    private {Type} {fieldName};

    @ApiModelProperty(value = "{字段说明}")
    private {Type} {fieldName};
}
```

#### 响应参数

##### 成功响应 (HTTP 200)

| 参数路径 | 类型 | 说明 | 示例 |
|---------|------|------|------|
| data.id | Long | ID | 1 |
| data.{field} | {Type} | {说明} | {示例} |

##### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "field1": "value1"
  }
}
```

##### 响应 VO

```java
@Data
@ApiModel("{VO说明}")
public class {Xx}Vo {

    @ApiModelProperty("{字段说明}")
    private {Type} {fieldName};

    public static {Xx}Vo of({Entity}Po po) {
        {Xx}Vo vo = new {Xx}Vo();
        BeanUtil.copyProperties(po, vo);
        return vo;
    }
}
```

#### 错误响应

| 场景 | HTTP状态码 | 业务错误码 | 说明 |
|------|-----------|-----------|------|
| 参数校验失败 | 400 | 400 | {说明} |
| 未授权 | 401 | 401 | Token无效或过期 |
| 资源不存在 | 404 | {业务码} | {说明} |
| 业务异常 | 200 | {业务码} | {说明} |

##### 错误响应示例

```json
{
  "code": {业务码},
  "message": "{错误信息}"
}
```

---

### 3.2 {接口名称2}

{重复上述结构}

---

## 四、错误码汇总

### 系统错误码

| 错误码 | HTTP状态码 | 说明 | 处理建议 |
|--------|-----------|------|---------|
| 200 | 200 | 成功 | - |
| 400 | 400 | 参数错误 | 检查请求参数 |
| 401 | 401 | 未授权 | 检查Token |
| 403 | 403 | 禁止访问 | 检查权限 |
| 404 | 404 | 资源不存在 | 检查ID |
| 500 | 500 | 系统错误 | 联系后端 |

### 业务错误码

| 错误码 | 说明 | 场景 |
|--------|------|------|
| {code} | {说明} | {场景} |
| {code} | {说明} | {场景} |

---

## 五、接口变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v{version} | {yyyy-MM-dd} | 初始版本 | {author} |

---

> 接口契约由 api-designer 生成，前后端据此进行开发和联调。
