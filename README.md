# AikSteinsGrimoire

a I k . 的命运石魔典 — 个人知识库后端 API。

## 技术栈

- Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 8.0 + Druid 1.2.20
- Hutool 5.8.11
- SpringDoc OpenAPI 1.7.0

## 快速开始

1. 创建数据库 `aik_steins_grimoire`（字符集 utf8mb4），在该库下依次执行 `sql/aik_system_tables.sql` 与 `sql/aik_knowledge_tables.sql`
2. 修改 `src/main/resources/application.yml` 中的数据库密码
3. 启动：`mvn spring-boot:run`
4. Swagger UI：`http://localhost:18900/swagger-ui.html`（端口与 context-path 见 `src/main/resources/application.yml`）

## 目录说明

- `aik-skills-lab/` — 技能库（独立管理）
- `sql/` — 数据库脚本
- `src/main/java/io/aik/steins/grimoire/` — 后端源码
