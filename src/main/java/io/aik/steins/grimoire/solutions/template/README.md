# 方案模板

> 复制此目录作为新方案的起点

## 目录结构

```
{solution-name}/
├── README.md              # 方案说明文档
├── problem.md             # 问题描述
├── steps/                 # 解决步骤
│   ├── 01-step.md
│   └── 02-step.md
└── verification.md        # 验证方式
```

## 元数据关联

方案元数据通过 `knowledge` 模块管理：
- `type` = 3（解决方案）
- `resource_path` = `solution/{solution-name}/`
