# Grimoire Files — 项目文档资料库

> 存放 AikSteinsGrimoire 项目的所有规划、设计、组件手册及学习笔记。

## 目录结构

```
grimoire-files/
├── plans/                          # 项目规划 & 架构方案
│   ├── aik-dev-agent/              # aik-dev-agent 智能体设计方案
│   ├── project-architecture.md     # 项目整体架构规划
│   └── knowledge-module-design.md  # Knowledge 模块包目录设计方案
├── design-docs/                    # PRD / SDD 设计文档
│   └── system-module/
│       ├── PRD-v1.0.md             # system 模块产品需求文档
│       └── SDD-v1.0.md             # system 模块系统设计文档
├── component-manuals/              # 组件手册（萃取产物）
│   ├── threadpool-executor.md      # 自定义线程池封装组件
│   ├── word-template-export.md     # Word 模板导出引擎
│   └── scene-factory-router.md     # 场景服务工厂路由组件
└── learning-notes/                 # 学习笔记 & 研究总结
    ├── component-extraction-workflow.md  # 组件知识萃取工作流实施计划
    └── skills-design-study.md            # Agent Skills 设计理念深度学习
```

## 分类说明

| 目录 | 用途 | 命名规范 |
|------|------|----------|
| `plans/` | 项目级规划、架构决策、模块设计方案 | 小写短横线，按主题命名 |
| `design-docs/` | 正式 PRD/SDD 文档，按模块分子目录 | `{模块名}/PRD-v{x}.md` |
| `component-manuals/` | 组件萃取手册（Component Manual 产物） | 组件名小写短横线 |
| `learning-notes/` | 学习笔记、技术调研、工作流记录 | 主题小写短横线 |
