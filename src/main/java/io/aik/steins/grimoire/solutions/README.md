# solutions 模块

> 解决方案内容存储层

## 职责

存放具体的技术解决方案内容（文档、步骤、验证方式等）。

## 结构说明

```
solutions/
├── README.md              # 模块说明
├── template/              # 方案模板骨架
│   └── README.md
└── {solution-name}/       # 具体方案子包
    ├── README.md          # 方案说明
    ├── problem.md         # 问题描述
    ├── steps/             # 解决步骤
    └── verification.md    # 验证方式
```

## 元数据管理

方案元数据（标题、分类、标签、状态等）**不存放在此模块**，统一通过 `knowledge` 模块管理：
- 数据库表：`aik_knowledge`，`type = 3`
- `resource_path` 指向 `solutions/{solution-name}/`
- 新增/修改/删除/查询全部调用 `KnowledgeController` 接口

## 当前方案列表

| 方案名称 | 路径 | 状态 |
|---------|------|------|
| template | `solutions/template/` | 模板骨架 |
