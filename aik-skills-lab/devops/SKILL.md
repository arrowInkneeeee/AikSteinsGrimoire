---
name: devops
description: 部署运维全流程技能——多环境配置管理、构建部署包、生成部署脚本、健康检查配置、日志系统配置、故障排查手册，覆盖部署运维全生命周期。适用于"部署方案"、"运维手册"、"配置管理"、"构建部署包"、"健康检查"、"日志配置"、"故障排查"、"上线方案"等场景。
type: Skill
version: 1.0.0
trigger:
  - 部署方案
  - 运维手册
  - 配置管理
  - 构建部署包
  - 健康检查
  - 日志配置
  - 故障排查
  - 上线方案
  - 部署文档
  - 运维指南
  - config-manager
  - deploy-script-generator
  - package-builder
  - health-check-designer
  - log-configurator
  - troubleshooting-guide
  - spec-devops
---

# devops

部署运维全流程技能，覆盖配置管理、构建打包、部署脚本、健康检查、日志系统和故障排查，最终产出部署计划 + 运维手册。

## 子命令

### config

多环境配置管理——区分开发配置和部署配置。

**触发条件**：用户说"配置管理"、"多环境配置"、"application.yml"

**输入**：
- 项目pom.xml
- 环境列表（dev/test/prod）

**输出**：
- 配置文件结构（application.yml + application-{env}.yml）
- 配置项说明文档

**执行步骤**：
1. 分析项目依赖，识别需要配置的组件
2. 检测pom.xml中是否有Nacos依赖
3. 设计配置文件结构：
   - `application.yml`：公共配置
   - `application-dev.yml`：开发环境配置
   - `application-test.yml`：测试环境配置
   - `application-prod.yml`：生产环境配置（外置）
4. 如有Nacos依赖，提供配置中心集成方案
5. 输出配置文件和说明文档

---

### package

构建可部署产物——支持jar包和tar包两种格式。

**触发条件**：用户说"构建部署包"、"打包"、"构建"

**输入**：
- 项目pom.xml
- 部署方式（jar/tar）

**输出**：
- 可部署产物（jar包或tar包）
- 构建脚本

**执行步骤**：
1. 检测项目构建配置
2. 根据部署方式选择构建方案：
   - **jar包**：传统方式，`mvn clean package -DskipTests`
   - **tar包**：包含启动脚本 + 外置配置目录
3. 如选择tar包，生成：
   - `bin/startup.sh`：启动脚本
   - `bin/shutdown.sh`：停止脚本
   - `bin/restart.sh`：重启脚本
   - `conf/`：外置配置文件目录
   - `logs/`：日志目录
4. 输出构建产物和构建命令

---

### deploy

生成部署脚本——支持systemd、Docker、Docker Compose。

**触发条件**：用户说"生成部署脚本"、"部署脚本"、"Dockerfile"

**输入**：
- 部署方式（systemd/docker/docker-compose）
- 项目信息（jar包路径、端口等）

**输出**：
- 部署脚本文件

**执行步骤**：
1. 加载 `references/deployment-decision-matrix.md` 决策矩阵
2. 根据部署方式生成对应脚本：
   - **systemd**：
     - `xx.service`：systemd服务文件
     - 支持 start/stop/restart/status
   - **Docker**：
     - `Dockerfile`：基于OpenJDK 8
     - 多阶段构建（builder + runtime）
   - **Docker Compose**：
     - `docker-compose.yml`：应用 + MySQL + Redis
     - 网络和数据卷配置
3. 输出部署脚本和使用说明

---

### health

配置健康检查和监控端点。

**触发条件**：用户说"健康检查"、"监控配置"、"Actuator"

**输入**：
- 项目pom.xml

**输出**：
- Actuator配置
- 健康检查端点说明

**执行步骤**：
1. 检查pom.xml是否有spring-boot-starter-actuator依赖
2. 如无，添加依赖
3. 配置application.yml：
   - 暴露端点：health, info, metrics
   - 健康检查路径：/actuator/health
4. 输出配置和使用说明：
   - `GET /actuator/health`：健康状态
   - `GET /actuator/info`：应用信息
   - `GET /actuator/metrics`：性能指标

---

### log

配置日志系统——使用Logback实现本地日志输出。

**触发条件**：用户说"日志配置"、"Logback"、"日志轮转"

**输入**：
- 项目结构

**输出**：
- logback-spring.xml配置文件

**执行步骤**：
1. 生成logback-spring.xml配置：
   - 控制台输出（开发环境）
   - 文件输出（生产环境）
   - 按时间和大小轮转（每日 + 100MB）
   - 保留天数（30天）
   - 总大小限制（3GB）
2. 按环境区分日志级别：
   - dev：DEBUG
   - test：INFO
   - prod：WARN（业务日志INFO）
3. 输出配置文件和说明

---

### troubleshoot

生成故障排查手册和应急响应指南。

**触发条件**：用户说"故障排查"、"应急手册"、"运维手册"

**输入**：
- 项目信息
- 部署方式

**输出**：
- 故障排查手册（Markdown格式）

**执行步骤**：
1. 加载 `assets/ops-manual-template.md` 运维手册模板
2. 生成故障排查手册，包含：
   - **常见问题诊断**：
     - 启动失败（端口占用、配置错误、依赖服务不可达）
     - 内存溢出（JVM参数调优、内存泄漏排查）
     - 数据库连接池耗尽
     - 接口响应慢（慢SQL、N+1查询）
   - **日志分析方法**：
     - 错误日志关键词
     - 链路追踪方法
   - **应急回滚方案**：
     - 快速回滚步骤
     - 数据恢复方案
   - **监控告警配置**
3. 输出故障排查手册

---

### full

完整部署方案，产出部署计划 + 运维手册。

**触发条件**：用户说"完整部署方案"、"部署方案"、"上线方案"

**输入**：
- 项目信息
- 部署环境信息

**输出**：
- 部署计划文档（使用 `assets/deploy-plan-template.md` 模板）
- 运维手册文档（使用 `assets/ops-manual-template.md` 模板）
- 所有配置文件和脚本

**执行步骤**：
1. 加载 `assets/deploy-plan-template.md` 部署计划模板
2. 加载 `references/deployment-decision-matrix.md` 决策矩阵
3. 执行 `config` → 配置管理
4. 执行 `package` → 构建部署包
5. 执行 `deploy` → 生成部署脚本
6. 执行 `health` → 健康检查配置
7. 执行 `log` → 日志系统配置
8. 执行 `troubleshoot` → 故障排查手册
9. 整合输出部署计划文档
10. 生成部署检查清单（前/中/后）

**部署检查清单**：
- **部署前**：环境检查、配置确认、数据库初始化、依赖服务确认
- **部署中**：服务启动、健康检查、日志验证、接口验证
- **部署后**：监控确认、告警配置、回滚方案确认

---

## 输入契约

| 输入项 | 路径/来源 | 必须 | 降级策略 |
|--------|----------|------|---------|
| 项目pom.xml | 项目根目录 | 是 | 无 |
| 部署环境 | 用户指定 | 否 | 默认Linux + systemd |
| 数据库类型 | pom.xml | 否 | 默认 MySQL |

## 输出契约

| 输出项 | 路径 | 格式 |
|--------|------|------|
| 部署计划 | `docs/ops/deploy-plan.md` | Markdown |
| 运维手册 | `docs/ops/ops-manual.md` | Markdown |
| 配置文件 | `src/main/resources/` | YAML |
| 部署脚本 | `deploy/` | Shell/Dockerfile |
| 日志配置 | `src/main/resources/logback-spring.xml` | XML |

## 工作流上下文

**上游技能**：
- `testing`：测试通过后进入部署阶段

**下游技能**：无（本技能为流程终点）

## 质量门禁

| 检查点 | 条件 | 动作 |
|--------|------|------|
| 配置检查 | 所有环境配置完整 | 继续 |
| 构建检查 | 构建成功无错误 | 继续 |
| 健康检查 | /actuator/health 返回 UP | 继续 |

## 约束

- 配置文件使用 YAML 格式
- 生产环境配置外置（不打包进jar）
- 脚本使用 Unix 风格换行符（LF）
- Docker镜像基于 OpenJDK 8
- 日志轮转策略：按日 + 100MB，保留30天
- 所有脚本提供 start/stop/restart/status 命令
