# AikSteinsGrimoire

a I k . 的命运石魔典 — 个人知识库：Spring Boot 后端 API + Vue 3 前端 + 魔典文档/卷轴 + SDLC 技能库 + DSH 智能体。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.7.18 / MyBatis-Plus 3.5.5 / MySQL 8 / Druid 1.2.20 / Hutool 5.8.11 / SpringDoc OpenAPI 1.7.0 |
| 前端 | Vue 3 / Element Plus / Vite / Pinia（`grimoire-web/`） |
| 文档 | Markdown（`grimoire-files/` 系统文档、`grimoire-scrolls/` 学习卷轴） |
| 技能 | `aik-skills-lab/`：10 个 SDLC 技能，单一来源，通过文件系统链接接入 DSH |
| 智能体 | `grimoire-agent/`：决策档案、双人设文档与 DSH preset `grimoire-trinity` |

## 快速开始（后端）

1. 创建数据库 `aik_steins_grimoire`（字符集 utf8mb4），在该库下依次执行 `sql/aik_system_tables.sql` 与 `sql/aik_knowledge_tables.sql`
2. 修改 `src/main/resources/application.yml` 中的数据库密码（或使用本地不提交的配置覆盖）
3. 启动：`mvn spring-boot:run`
4. Swagger UI：`http://localhost:18900/swagger-ui.html`（端口与 context-path 见 `src/main/resources/application.yml`）

## 快速开始（前端）

```bash
cd grimoire-web
npm install
npm run dev
```

Vite 默认开发地址为 `http://localhost:5173`，以 `grimoire-web/vite.config.ts` 与实际输出为准。

## 目录说明

- `src/main/java/io/aik/steins/grimoire/` — Spring Boot 后端源码（core / system / knowledge / components / solutions / work）
- `src/main/resources/` — 配置、模板与静态资源
- `grimoire-web/` — Vue 3 + Element Plus 前端（读者端 + 管理端）
- `grimoire-files/` — 系统设计、计划、决策与 `history/` 归档
- `grimoire-scrolls/` — 学习卷轴与文章内容
- `aik-skills-lab/` — SDLC 技能库（单一来源，不复制到宿主）
- `grimoire-agent/` — 智能体决策档案、双人设文档与 `dsh-preset/`
- `dsh-migration/` — DSH 技能迁移记录与工具（部分内容为历史快照）
- `sql/` — 数据库建表与迁移脚本
- `lib/` — 本地运行的第三方 jar（`*.jar` 不进 git）
- `config/` — 本地配置与许可证（敏感文件不进 git）

## 文档入口

- [grimoire-files/README.md](grimoire-files/README.md) — 系统文档总览与权威边界
- [grimoire-agent/personas.md](grimoire-agent/personas.md) — `grimoire-trinity` 双人设
- [aik-skills-lab/README.md](aik-skills-lab/README.md) — 技能库总览
- [dsh-migration/README.md](dsh-migration/README.md) — DSH 迁移记录

## 本地依赖与约定

- `lib/aspose-words-21.11-jdk17.jar` 不随仓库分发；缺失时按 `pom.xml` 注释自行获取。
- `config/aspose-license.xml` 是本地商业许可证，已被 `.gitignore` 忽略。
- `src/main/java/io/aik/steins/grimoire/work/` 是个人快速开发/实验目录，不属于主业务模块。
