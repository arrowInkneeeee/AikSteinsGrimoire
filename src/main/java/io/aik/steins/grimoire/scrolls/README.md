# scrolls 模块

> 卷轴内容存储层

## 职责

存放学习笔记、文章内容（Markdown 格式）。

## 结构说明

```
scrolls/
├── README.md              # 模块说明
├── template/              # 卷轴模板骨架
│   └── template.md
└── {scroll-name}/         # 具体卷轴子包
    └── {scroll-name}.md   # 卷轴正文（Markdown）
```

## 元数据管理

卷轴元数据（标题、分类、标签、状态等）**不存放在此模块**，统一通过 `knowledge` 模块管理：
- 数据库表：`aik_knowledge`，`type = 1`
- `resource_path` 指向 `scrolls/{scroll-name}/`
- 新增/修改/删除/查询全部调用 `KnowledgeController` 接口

## 当前卷轴列表

| 卷轴名称 | 路径 | 状态 |
|---------|------|------|
| template | `scrolls/template/` | 模板骨架 |
| gitnexus-guide | `scrolls/gitnexus-guide/` | GitNexus 学习笔记 |
| skills-guide | `scrolls/skills-guide/` | Skills 从入门到精通学习笔记 |
| git-guide | `scrolls/git-guide/` | Git 操作指南 |
| ssl-https-guide | `scrolls/ssl-https-guide/` | SSL/TLS 与 HTTPS 完全指南 |
| claude-cli-guide | `scrolls/claude-cli-guide/` | Claude CLI（Claude Code）Windows 安装指南 |
| vmware-ubuntu-setup | `scrolls/vmware-ubuntu-setup/` | VMware 安装 Ubuntu 与初始化配置指南 |
| idea-jvm-tuning | `scrolls/idea-jvm-tuning/` | IntelliJ IDEA 全局调优方案（vmoptions + properties） |
| docker-learning | `scrolls/docker-learning/` | Docker 容器化技术系统化学习资料 |
