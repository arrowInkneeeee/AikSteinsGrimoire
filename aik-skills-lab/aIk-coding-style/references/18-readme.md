# README规范

> 来源：aIk-coding-style 规范

## 文件位置
`{module}/README.md`

## 内容结构
```markdown
# {模块中文名称}模块

## 模块简介
{模块功能概述}

## 功能特性
- **功能1**：{描述}
- **功能2**：{描述}

## 技术栈
- {技术1}
- {技术2}

## 目录结构

```
{module}/
├── api/                    # API接口文档
├── common/                 # 公共包
│   ├── constant/           # 常量定义
│   ├── dto/                # 数据传输对象
│   ├── po/                 # 持久化对象
│   ├── vo/                 # 视图对象
│   ├── config/             # 配置类
│   ├── exception/          # 自定义异常
│   └── utils/              # 工具类
├── controller/             # 控制器层
├── dao/                    # 数据访问层
│   ├── {Entity}Mapper.java
│   └── mapping/            # XML映射文件(可选)
├── service/                # 服务层
│   ├── {Entity}Service.java
│   └── impl/               # 服务实现
├── sql/                    # 数据库脚本
└── README.md
```

## 数据库表
**表名：** `{table_name}`
**核心字段：**
- {字段说明}

## 接口列表
| 接口 | 地址 | 说明 |
|------|------|------|
| {名称} | {方法} {路径} | {说明} |

## 快速开始
1. 执行SQL脚本：`sql/{file}.sql`
2. 查看接口文档：`api/{file}.md`

## 作者
@author a I k .
```
