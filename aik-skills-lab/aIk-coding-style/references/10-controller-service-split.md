# Controller 与 Service 拆分规范

> 来源：aIk-coding-style 规范

## 拆分原则

一个业务模块包含多个子功能时，应按职责拆分不同的 Controller 和 Service，保持代码清晰。

## 拆分场景

| 场景 | 拆分方式 | 示例 |
|------|---------|------|
| 基础 CRUD | 单独 Controller + Service | `{Entity}Controller` + `{Entity}Service` |
| 导入功能 | 单独 Controller + Service | `{Entity}ImportController` + `{Entity}ImportService` |
| 导出功能 | 单独 Controller + Service | `{Entity}ExportController` + `{Entity}ExportService` |
| 树形结构 | 单独 Controller + Service | `{Entity}TreeController` + `{Entity}TreeService` |
| 批量操作 | 单独 Controller + Service | `{Entity}BatchController` + `{Entity}BatchService` |

## 目录结构示例（多子功能模块）

```
{module}/
├── controller/
│   ├── {Entity}Controller.java           # 基础CRUD
│   ├── {Entity}ImportController.java     # 导入功能
│   └── {Entity}ExportController.java     # 导出功能
├── service/
│   ├── {Entity}Service.java              # 基础服务接口
│   ├── {Entity}ImportService.java        # 导入服务接口
│   ├── {Entity}ExportService.java        # 导出服务接口
│   └── impl/
│       ├── {Entity}ServiceImpl.java      # 基础服务实现
│       ├── {Entity}ImportServiceImpl.java # 导入服务实现
│       └── {Entity}ExportServiceImpl.java # 导出服务实现
```

## 拆分原则说明

1. **单一职责**：每个 Controller/Service 只负责一类功能
2. **接口隔离**：不同功能使用不同的 Service 接口
3. **避免臃肿**：单个类代码行数不超过 300 行
4. **命名清晰**：通过类名即可知道职责范围
