# Docker 学习卷轴

> 类型标记：NOTE  
> 用途：系统化的 Docker 容器化技术学习资料  
> 编码：docker-learning  
> 最后更新：2026-07-14

---

## 目录

1. [Docker 概述与核心价值](#一docker-概述与核心价值)
2. [核心概念](#二核心概念)
3. [底层技术原理](#三底层技术原理)
4. [安装与环境配置](#四安装与环境配置)
5. [常用命令速查](#五常用命令速查)
6. [Dockerfile 编写与最佳实践](#六dockerfile-编写与最佳实践)
7. [数据持久化](#七数据持久化)
8. [网络管理](#八网络管理)
9. [Docker Compose 编排](#九docker-compose-编排)
10. [生产环境最佳实践](#十生产环境最佳实践)
11. [故障排查指南](#十一故障排查指南)
12. [安全实践](#十二安全实践)

---

## 一、Docker 概述与核心价值

### 1.1 什么是 Docker

Docker 是一个开源的容器化平台，允许开发者将应用及其依赖打包到一个轻量级、可移植的容器中，然后在任何环境中一致地运行。

### 1.2 容器 vs 虚拟机

| 特性 | Docker 容器 | 虚拟机 |
|------|------------|--------|
| 启动速度 | 秒级 | 分钟级 |
| 资源占用 | 共享宿主机内核，轻量 | 需要完整操作系统，笨重 |
| 隔离级别 | 进程级隔离 | 硬件级隔离 |
| 性能 | 接近原生 | 有虚拟化开销 |
| 体积 | 通常 MB 级 | 通常 GB 级 |

### 1.3 核心价值

- **环境一致性**：彻底屏蔽开发、测试、生产环境差异，告别"在我机器上能跑"
- **快速部署**：秒级启动，小时级部署缩短到秒级
- **资源高效**：共享宿主机内核，单机可运行数百个容器
- **弹性伸缩**：快速复制、批量部署，支撑业务高峰期扩容
- **DevOps 友好**：标准化交付物，打通开发运维流程

---

## 二、核心概念

### 2.1 镜像（Image）

镜像是用于创建容器的**只读模板**，包含运行应用所需的一切：代码、运行时、库、环境变量和配置文件。

- 类比：Java 中的 **Class**
- 特性：分层存储（UnionFS），每层代表一个构建步骤
- 存储：镜像本身不包含动态数据，构建后内容不再变动

### 2.2 容器（Container）

容器是镜像的**运行实例**，是一个独立、隔离的运行环境。

- 类比：Java 中的 **对象**（由 Class 实例化）
- 特性：
  - 拥有自己的文件系统、网络和进程空间
  - 容器之间互相隔离
  - 运行时在最上层添加可写层（Container Layer）
- 生命周期：容器消亡时，其存储层也随之消亡（数据需持久化）

### 2.3 仓库（Registry）

仓库是存放和分享镜像的地方。

- 类比：Maven 中央仓库 / GitHub
- 公共仓库：[Docker Hub](https://hub.docker.com)
- 私有仓库：Harbor、阿里云 ACR、自建 Registry

### 2.4 核心关系图

```
Dockerfile  --构建-->  Image  --运行-->  Container
                           ^
                           |
                     Registry (推送/拉取)
```

---

## 三、底层技术原理

### 3.1 Linux Namespace（命名空间）

Namespace 提供了**资源隔离**机制，让容器拥有独立的系统视图：

| Namespace | 隔离资源 | 说明 |
|-----------|---------|------|
| PID | 进程 ID | 容器内进程 PID 独立编号 |
| NET | 网络 | 独立的网络设备、IP、端口 |
| IPC | 进程间通信 | 独立的共享内存、信号量 |
| MNT | 挂载点 | 独立的文件系统挂载视图 |
| UTS | 主机名/域名 | 独立的 hostname |
| USER | 用户/组 ID | 独立的用户权限体系 |
| CGROUP | 控制组 | 独立的 cgroup 视图（Linux 4.6+）|

### 3.2 Cgroup（控制组）

Cgroup 提供了**资源限制**机制，控制容器可使用的系统资源：

- **CPU**：限制 CPU 使用率和权重
- **Memory**：限制内存使用量和 Swap
- **Blkio**：限制块设备 I/O 速率
- **Pids**：限制容器内进程数量

### 3.3 联合文件系统（UnionFS）

Docker 镜像采用**分层存储**结构，每条 Dockerfile 指令产生一个只读层：

```
Dockerfile:
  FROM ubuntu:22.04    --> Layer 1: 基础层（只读）
  RUN apt install jdk  --> Layer 2: 安装 JDK（只读）
  COPY . /app          --> Layer 3: 复制代码（只读）
  RUN mvn package      --> Layer 4: 编译产物（只读）

运行时容器:
  Layer 4（只读）
  Layer 3（只读）
  Layer 2（只读）
  Layer 1（只读）
  Container Layer（可写，Copy-on-Write）
```

**Copy-on-Write（写时复制）**：修改只读层文件时，先复制到可写层再修改，原只读层不变。

**核心优势**：
- 多个容器共享相同基础层，节省磁盘空间
- 拉取镜像时已有层不重复下载，加速部署

### 3.4 容器存储驱动

| 驱动 | 特点 | 适用场景 |
|------|------|---------|
| Overlay2 | 性能优、稳定性好 | **推荐，现代 Linux 默认** |
| Aufs | 早期默认 | 已逐步淘汰 |
| Devicemapper | 块设备映射 | 特定存储需求 |
| Btrfs/ZFS | 快照功能 | 高级文件系统 |

---

## 四、安装与环境配置

### 4.1 Linux 一键安装（推荐）

```bash
# CentOS / RHEL / Rocky
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl start docker
sudo systemctl enable docker

# Ubuntu / Debian
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 4.2 配置镜像加速（国内必配）

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 4.3 非 root 用户使用 Docker

```bash
sudo groupadd docker
sudo usermod -aG docker $USER
newgrp docker  # 重新登录或执行此命令生效
```

---

## 五、常用命令速查

### 5.1 镜像管理

```bash
# 拉取镜像
docker pull nginx:1.25

# 查看本地镜像
docker images

# 删除镜像
docker rmi nginx:1.25

# 构建镜像（当前目录需有 Dockerfile）
docker build -t myapp:1.0 .

# 构建时指定 Dockerfile
docker build -f Dockerfile.prod -t myapp:prod .

# 给镜像打标签
docker tag myapp:1.0 registry.example.com/myapp:1.0

# 推送镜像到仓库
docker push registry.example.com/myapp:1.0

# 导出/导入镜像
docker save -o myapp.tar myapp:1.0
docker load -i myapp.tar

# 查看镜像历史
docker history myapp:1.0

# 清理悬空镜像
docker image prune -f
```

### 5.2 容器管理

```bash
# 运行容器
docker run -d --name mynginx -p 80:80 nginx:1.25

# 交互式运行（常用于调试）
docker run -it --rm ubuntu:22.04 /bin/bash

# 查看运行中容器
docker ps

# 查看所有容器（含已停止）
docker ps -a

# 启动/停止/重启容器
docker start mynginx
docker stop mynginx
docker restart mynginx

# 进入运行中容器
docker exec -it mynginx /bin/bash

# 查看容器日志
docker logs -f --tail 100 mynginx

# 查看容器资源使用
docker stats mynginx

# 复制文件（宿主机 <-> 容器）
docker cp mynginx:/etc/nginx/nginx.conf ./nginx.conf
docker cp ./app.jar myapp:/app/app.jar

# 删除容器
docker rm mynginx

# 强制删除运行中容器
docker rm -f mynginx

# 清理所有已停止容器
docker container prune -f
```

### 5.3 docker run 核心参数详解

```bash
docker run \
  -d                          # 后台运行（detached）
  --name mycontainer          # 指定容器名称
  -p 8080:80                  # 端口映射（宿主机:容器）
  -p 127.0.0.1:8080:80        # 绑定特定 IP
  -v /host/data:/app/data     # 卷挂载（Bind Mount）
  -v myvolume:/app/data       # 命名卷挂载（Named Volume）
  --network mynet             # 指定网络
  --restart unless-stopped    # 重启策略
  -e MYSQL_ROOT_PASSWORD=123  # 环境变量
  --memory=512m               # 内存限制
  --cpus=1.0                  # CPU 限制
  --health-cmd="curl -f http://localhost/"  # 健康检查
  --read-only                 # 只读根文件系统
  nginx:1.25
```

### 5.4 重启策略

| 策略 | 说明 |
|------|------|
| `no` | 不自动重启（默认） |
| `on-failure` | 退出码非 0 时重启 |
| `always` | 总是重启（含手动停止后也重启） |
| `unless-stopped` | 总是重启，但手动停止后不重启（**推荐**） |

---

## 六、Dockerfile 编写与最佳实践

### 6.1 核心指令

| 指令 | 作用 | 示例 |
|------|------|------|
| `FROM` | 指定基础镜像 | `FROM eclipse-temurin:17-jre-alpine` |
| `RUN` | 执行命令并创建新层 | `RUN apt-get update && apt-get install -y curl` |
| `COPY` | 复制本地文件到镜像（推荐） | `COPY target/app.jar /app/app.jar` |
| `ADD` | 复制文件，支持自动解压和 URL | `ADD https://.../file.tar.gz /opt/` |
| `WORKDIR` | 设置工作目录 | `WORKDIR /app` |
| `ENV` | 设置环境变量 | `ENV JAVA_OPTS="-Xms512m -Xmx1024m"` |
| `EXPOSE` | 声明暴露端口 | `EXPOSE 8080` |
| `CMD` | 容器启动默认命令（可被覆盖） | `CMD ["java", "-jar", "app.jar"]` |
| `ENTRYPOINT` | 容器启动入口命令（不易被覆盖） | `ENTRYPOINT ["java", "-jar"]` |
| `VOLUME` | 声明挂载点 | `VOLUME ["/app/data"]` |
| `USER` | 指定运行用户 | `USER appuser` |
| `HEALTHCHECK` | 健康检查 | `HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/actuator/health` |

### 6.2 最佳实践

#### 6.2.1 基础镜像选择

- **优先官方镜像**：经过严格测试，安全性高
- **选择精简版本**：`alpine`、`slim`、`distroless` 显著减小体积
- **固定版本号**：避免使用 `latest`，使用明确版本如 `nginx:1.25.3-alpine`
- **Java 项目推荐**：`eclipse-temurin:17-jre-alpine` 或 `amazoncorretto:17-alpine`

#### 6.2.2 镜像优化原则

1. **减少层数**：合并 RUN 指令，用 `&&` 连接多命令
2. **清理缓存**：同一 RUN 中安装后删除缓存
3. **使用 .dockerignore**：排除不需要的文件
4. **多阶段构建**：分离编译环境和运行环境
5. **显式设置 WORKDIR**：避免在未知目录操作
6. **优先 COPY 而非 ADD**：COPY 语义更明确

#### 6.2.3 优化前后对比

**优化前（问题）**：

```dockerfile
FROM ubuntu:latest
RUN apt-get update
RUN apt-get install -y openjdk-17-jdk
RUN apt-get install -y maven
COPY . /app
RUN cd /app && mvn package
CMD java -jar /app/target/app.jar
```

**优化后（推荐）**：

```dockerfile
# 多阶段构建
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 6.2.4 .dockerignore 示例

```
# Git
.git
.gitignore

# IDE
.idea
*.iml
.vscode

# Build
/target
/build
/node_modules

# Test
test/
*.test.js

# Docs
README.md
docs/

# Local env
.env
.env.local

# Docker itself
Dockerfile*
docker-compose*
.dockerignore
```

---

## 七、数据持久化

### 7.1 三种挂载模式

| 模式 | 命令 | 特点 | 适用场景 |
|------|------|------|---------|
| **Bind Mount** | `-v /host/path:/container/path` | 直接挂载宿主机目录 | 开发环境、配置文件热更新 |
| **Named Volume** | `-v myvolume:/container/path` | Docker 管理，可命名、复用 | 生产环境数据持久化（**推荐**） |
| **tmpfs Mount** | `--tmpfs /container/path` | 存储在内存中，容器停止即消失 | 敏感临时数据 |

### 7.2 常用命令

```bash
# 创建命名卷
docker volume create mysql_data

# 查看卷列表
docker volume ls

# 查看卷详情
docker volume inspect mysql_data

# 删除卷
docker volume rm mysql_data

# 清理未使用卷
docker volume prune -f
```

### 7.3 常见软件挂载清单

| 软件 | 数据路径 | 示例命令 |
|------|---------|---------|
| MySQL | `/var/lib/mysql` | `-v mysql_data:/var/lib/mysql` |
| PostgreSQL | `/var/lib/postgresql/data` | `-v pg_data:/var/lib/postgresql/data` |
| Redis | `/data` | `-v redis_data:/data` |
| MongoDB | `/data/db` | `-v mongo_data:/data/db` |
| Nginx | `/etc/nginx/conf.d` | `-v ./nginx.conf:/etc/nginx/nginx.conf:ro` |

---

## 八、网络管理

### 8.1 网络类型

| 类型 | 说明 | 适用场景 |
|------|------|---------|
| **bridge** | 默认桥接网络，容器间可通过 IP 通信 | 单机多容器通信 |
| **host** | 共享宿主机网络栈，无网络隔离 | 高性能网络需求 |
| **none** | 无网络 | 完全隔离 |
| **overlay** | 跨主机通信（Swarm/K8s） | 集群环境 |
| **macvlan** | 为容器分配独立 MAC 地址 | 传统网络集成 |

### 8.2 自定义网络（推荐）

```bash
# 创建自定义桥接网络
docker network create mynet

# 指定子网和网关
docker network create --subnet=172.20.0.0/16 --gateway=172.20.0.1 mynet

# 运行容器加入网络
docker run -d --name db --network mynet mysql:8.0
docker run -d --name app --network mynet myapp:1.0

# 容器间通过容器名通信（DNS 解析）
# app 容器内可直接访问 db:3306

# 查看网络详情
docker network inspect mynet

# 连接/断开容器网络
docker network connect mynet container_name
docker network disconnect mynet container_name
```

### 8.3 端口映射 vs 容器互联

- **端口映射（-p）**：将容器服务暴露给宿主机外部访问
- **容器互联（自定义网络）**：容器间通过名称通信，无需暴露端口到宿主机

---

## 九、Docker Compose 编排

### 9.1 核心概念

Docker Compose 是用于**定义和运行多容器应用**的工具，使用 YAML 文件配置应用服务。

### 9.2 docker-compose.yml 结构

```yaml
version: "3.9"

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    image: myapp:1.0
    container_name: myapp
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s

  mysql:
    image: mysql:8.0
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: mydb
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "3306:3306"
    networks:
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: redis
    volumes:
      - redis_data:/data
    networks:
      - backend
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:

networks:
  backend:
    driver: bridge
```

### 9.3 常用命令

```bash
# 启动所有服务（后台）
docker compose up -d

# 启动并强制重新构建
docker compose up -d --build

# 停止并移除容器、网络
docker compose down

# 停止并移除容器、网络、卷（慎用）
docker compose down -v

# 查看服务状态
docker compose ps

# 查看服务日志
docker compose logs -f app

# 重启指定服务
docker compose restart app

# 扩展服务实例数
docker compose up -d --scale app=3

# 验证配置文件语法
docker compose config
```

### 9.4 环境变量管理

```bash
# .env 文件
COMPOSE_PROJECT_NAME=myproject
DB_PASSWORD=secret
APP_PORT=8080
```

Compose 自动读取同目录下的 `.env` 文件，可在 `docker-compose.yml` 中使用 `${VAR}` 引用。

---

## 十、生产环境最佳实践

### 10.1 应用容器化 checklist

- [ ] 使用非 root 用户运行容器
- [ ] 固定基础镜像版本，避免 `latest`
- [ ] 多阶段构建分离编译与运行环境
- [ ] 配置健康检查（HEALTHCHECK）
- [ ] 设置资源限制（memory / cpus）
- [ ] 敏感信息通过环境变量或 secrets 注入，不硬编码
- [ ] 日志输出到 stdout/stderr，由外部收集
- [ ] 数据持久化使用命名卷
- [ ] 配置合理的重启策略
- [ ] 使用只读根文件系统（`--read-only`）配合 tmpfs 写目录

### 10.2 Spring Boot 容器化示例

```dockerfile
FROM eclipse-temurin:17-jre-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 使用非 root 用户运行
USER spring:spring

EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 10.3 日志管理

- **容器最佳实践**：应用日志输出到 `stdout`/`stderr`
- **日志收集**：使用 Docker 日志驱动（json-file、journald、fluentd 等）
- **日志轮转**：配置 Docker 守护进程日志选项

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

### 10.4 资源限制

```bash
# 内存限制
docker run -m 512m --memory-swap 512m myapp

# CPU 限制
docker run --cpus=1.5 myapp

# 复合限制
docker run -m 1g --memory-reservation=512m --cpus=2.0 --pids-limit=100 myapp
```

---

## 十一、故障排查指南

### 11.1 常见问题速查

| 现象 | 可能原因 | 解决方法 |
|------|---------|---------|
| 端口冲突 | 宿主机端口已被占用 | 更换 `-p` 映射端口 |
| 权限拒绝 | 卷挂载权限不匹配 | 检查宿主机目录权限，或调整 USER |
| 容器启动后退出 | 主进程执行完毕或报错 | `docker logs` 查看退出原因 |
| 镜像拉取慢 | 无镜像加速 | 配置国内镜像加速地址 |
| 磁盘空间不足 | 镜像/容器/卷未清理 | `docker system prune -a` |
| 容器内无法联网 | DNS 或网络配置问题 | 检查防火墙，配置 Docker DNS |
| OOMKilled | 内存不足被系统杀死 | 增大内存限制或优化应用 |

### 11.2 排查命令

```bash
# 查看容器详情（含退出码、状态、挂载、环境变量等）
docker inspect container_name

# 查看容器实时资源使用
docker stats

# 查看容器进程
docker top container_name

# 查看容器内文件系统变更
docker diff container_name

# 查看容器事件流
docker events --filter container=container_name

# 进入容器调试
docker exec -it container_name /bin/sh

# 导出容器文件系统为 tar
docker export -o backup.tar container_name

# 查看 Docker 系统整体情况
docker system df -v

# 全面清理（删除未使用镜像、容器、卷、网络）
docker system prune -a --volumes
```

---

## 十二、安全实践

### 12.1 镜像安全

- 使用可信基础镜像（官方或经过安全扫描）
- 定期更新基础镜像补丁
- 使用镜像扫描工具（`docker scan`、`Trivy`、`Clair`）
- 最小化镜像，减少攻击面

```bash
# Docker 内置扫描（需登录）
docker scan myapp:1.0

# Trivy 扫描
trivy image myapp:1.0
```

### 12.2 运行时安全

- 不以 root 用户运行容器
- 使用只读根文件系统：`--read-only`
- 限制容器能力（capabilities）：`--cap-drop=ALL --cap-add=NET_BIND_SERVICE`
- 启用 seccomp 和 AppArmor/SELinux
- 限制资源使用，防止拒绝服务

```bash
docker run \
  --read-only \
  --tmpfs /tmp:noexec,nosuid,size=100m \
  --cap-drop=ALL \
  --cap-add=CHOWN \
  --security-opt=no-new-privileges:true \
  myapp:1.0
```

### 12.3 网络与 Secrets 安全

- 生产环境使用 TLS 连接 Registry
- 敏感信息使用 Docker Secrets（Swarm）或外部 Vault
- 不将 `.env` 文件提交到版本控制
- 自定义网络隔离不同服务组

---

## 附录：快速参考卡片

### A. 分层存储记忆口诀

> **镜像如菜谱，容器如菜肴；分层可复用，写时复制妙。**

### B. Dockerfile 编写口诀

> **FROM 定基础，版本要固定；RUN 合并写，缓存清理净。**  
> **COPY 优先用，ADD 解压行；WORKDIR 要设，路径不随性。**  
> **非 root 运行，安全有保证；多阶段构建，镜像瘦又轻。**

### C. 日常维护三板斧

```bash
# 1. 查看运行状态
docker ps && docker stats

# 2. 查看日志定位问题
docker logs -f --tail 200 container_name

# 3. 定期清理空间
docker system prune -f
```

---

## 参考资源

- [Docker 官方文档](https://docs.docker.com)
- [Dockerfile 最佳实践](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Docker Hub](https://hub.docker.com)
- [Distroless 镜像](https://github.com/GoogleContainerTools/distroless)
