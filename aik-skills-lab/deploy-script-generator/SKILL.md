---
name: deploy-script-generator
description: 为Spring Boot项目生成多种部署方式的脚本和配置，包括systemd服务、Dockerfile、docker-compose.yml。支持jar部署和tar部署两种模式，便于不同场景的运维实施。
type: Skill
version: 1.0.0
---

# Deploy Script Generator

## Purpose

为Spring Boot项目生成多种部署方式的脚本和配置文件，支持：
- **Systemd服务**：传统服务器部署，开机自启
- **Docker容器化**：Docker单容器部署
- **Docker Compose**：多服务编排部署

## When to Use

- 需要生成systemd服务配置
- 需要容器化部署
- 需要多服务编排（应用+数据库+Redis等）
- 需要标准化部署流程

## Deployment Options

### 1. Systemd服务部署（推荐传统部署）

适用于：物理机/虚拟机部署，需要开机自启、服务管理

**生成文件：**
```
deploy/
├── systemd/
│   ├── myapp.service          # systemd服务配置
│   ├── install.sh             # 安装服务脚本
│   └── uninstall.sh           # 卸载服务脚本
```

**myapp.service：**
```ini
[Unit]
Description=MyApp Spring Boot Service
After=network.target

[Service]
Type=simple
User=appuser
Group=appgroup

# 应用目录
WorkingDirectory=/opt/myapp

# 启动命令
ExecStart=/usr/bin/java \
    -server \
    -Xms512m \
    -Xmx1024m \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/opt/myapp/logs/heap_dump.hprof \
    -jar /opt/myapp/lib/myapp-1.0.0.jar \
    --spring.config.location=file:/opt/myapp/config/

# 停止命令
ExecStop=/bin/kill -15 $MAINPID

# 重启策略
Restart=on-failure
RestartSec=30

# 资源限制
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

**install.sh：**
```bash
#!/bin/bash

APP_NAME="myapp"
SERVICE_FILE="$APP_NAME.service"
INSTALL_DIR="/opt/$APP_NAME"

# 检查root权限
if [ "$EUID" -ne 0 ]; then
    echo "Please run as root"
    exit 1
fi

# 创建应用用户（如不存在）
if ! id -u appuser &>/dev/null; then
    useradd -r -s /bin/false appuser
    echo "Created user: appuser"
fi

# 复制服务文件
cp "$SERVICE_FILE" /etc/systemd/system/

# 创建目录
mkdir -p "$INSTALL_DIR"/{bin,config,lib,logs}
chown -R appuser:appuser "$INSTALL_DIR"

# 重载systemd
systemctl daemon-reload

# 启用开机自启
systemctl enable "$APP_NAME"

echo "Installation completed!"
echo "Next steps:"
echo "  1. Copy application files to $INSTALL_DIR"
echo "  2. Update config in $INSTALL_DIR/config/"
echo "  3. Start service: systemctl start $APP_NAME"
```

**systemd常用命令：**
```bash
# 启动服务
systemctl start myapp

# 停止服务
systemctl stop myapp

# 重启服务
systemctl restart myapp

# 查看状态
systemctl status myapp

# 查看日志
journalctl -u myapp -f

# 开机自启
systemctl enable myapp

# 禁用开机自启
systemctl disable myapp
```

### 2. Docker单容器部署

适用于：容器化环境，快速部署

**生成文件：**
```
deploy/
├── docker/
│   ├── Dockerfile             # Docker镜像构建
│   ├── docker-build.sh        # 构建脚本
│   └── docker-run.sh          # 运行脚本
```

**Dockerfile：**
```dockerfile
# 构建阶段
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM openjdk:8-jre-alpine

# 安装必要工具
RUN apk add --no-cache curl

# 创建应用目录
WORKDIR /app

# 创建非root用户
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# 复制jar
COPY --from=builder /build/target/*.jar app.jar

# 创建配置和日志目录
RUN mkdir -p /app/config /app/logs && \
    chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# JVM参数（可通过环境变量覆盖）
ENV JAVA_OPTS="-server -Xms512m -Xmx1024m -XX:+UseG1GC"
ENV SPRING_OPTS="--spring.config.location=file:/app/config/"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar $SPRING_OPTS"]
```

**docker-build.sh：**
```bash
#!/bin/bash

APP_NAME="myapp"
VERSION="1.0.0"
IMAGE_NAME="$APP_NAME:$VERSION"

echo "Building Docker image: $IMAGE_NAME"
docker build -t "$IMAGE_NAME" -f Dockerfile ..

echo "Tagging as latest"
docker tag "$IMAGE_NAME" "$APP_NAME:latest"

echo "Build completed!"
echo "To push to registry:"
echo "  docker tag $IMAGE_NAME your-registry.com/$IMAGE_NAME"
echo "  docker push your-registry.com/$IMAGE_NAME"
```

**docker-run.sh：**
```bash
#!/bin/bash

APP_NAME="myapp"
CONTAINER_NAME="myapp-container"

# 停止并删除旧容器
if docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo "Stopping existing container..."
    docker stop "$CONTAINER_NAME"
    docker rm "$CONTAINER_NAME"
fi

# 运行新容器
docker run -d \
    --name "$CONTAINER_NAME" \
    --restart unless-stopped \
    -p 8080:8080 \
    -v /opt/myapp/config:/app/config \
    -v /opt/myapp/logs:/app/logs \
    -e JAVA_OPTS="-server -Xms512m -Xmx1024m" \
    -e DB_USER=myapp \
    -e DB_PASSWORD=secret \
    "$APP_NAME:latest"

echo "Container started!"
echo "Logs: docker logs -f $CONTAINER_NAME"
```

### 3. Docker Compose编排部署

适用于：多服务场景（应用+MySQL+Redis等）

**生成文件：**
```
deploy/
├── docker-compose/
│   ├── docker-compose.yml     # 编排配置
│   ├── .env                   # 环境变量
│   └── start.sh               # 启动脚本
```

**docker-compose.yml：**
```yaml
version: '3.8'

services:
  # 应用服务
  myapp:
    build:
      context: ../..
      dockerfile: deploy/docker/Dockerfile
    container_name: myapp
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-server -Xms512m -Xmx1024m -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=myapp
      - DB_USER=myapp
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    networks:
      - myapp-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # MySQL数据库
  mysql:
    image: mysql:8.0
    container_name: myapp-mysql
    restart: unless-stopped
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=myapp
      - MYSQL_USER=myapp
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-sql:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    networks:
      - myapp-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: myapp-redis
    restart: unless-stopped
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    networks:
      - myapp-network
    command: redis-server --appendonly yes

volumes:
  mysql-data:
  redis-data:

networks:
  myapp-network:
    driver: bridge
```

**.env：**
```bash
# 数据库密码（部署前修改）
DB_PASSWORD=your_app_password
MYSQL_ROOT_PASSWORD=your_root_password

# JVM参数
JAVA_OPTS=-server -Xms512m -Xmx1024m
```

**start.sh：**
```bash
#!/bin/bash

# 加载环境变量
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# 创建必要目录
mkdir -p logs config init-sql

# 启动服务
docker-compose up -d

echo "Services starting..."
echo "Check status: docker-compose ps"
echo "View logs: docker-compose logs -f myapp"
```

**Docker Compose常用命令：**
```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v

# 查看日志
docker-compose logs -f myapp

# 重启服务
docker-compose restart myapp

# 构建并启动
docker-compose up -d --build
```

## Deployment Comparison

| 方式 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| Systemd | 传统服务器 | 简单稳定，资源可控 | 需要Java环境 |
| Docker | 容器化环境 | 环境隔离，快速部署 | 需要Docker知识 |
| Compose | 多服务开发 | 一键启动全套环境 | 不适合生产高可用 |

## Selection Guide

**选择Systemd如果：**
- 已有物理机/虚拟机基础设施
- 运维团队熟悉Linux服务管理
- 需要精细的资源控制

**选择Docker如果：**
- 已有容器化平台（K8s/OpenShift）
- 需要快速扩缩容
- 多环境一致性要求高

**选择Compose如果：**
- 本地开发测试
- 小型项目快速验证
- 演示/POC环境

## Output Location

```
deploy/
├── systemd/
│   ├── myapp.service
│   ├── install.sh
│   └── uninstall.sh
├── docker/
│   ├── Dockerfile
│   ├── docker-build.sh
│   └── docker-run.sh
└── docker-compose/
    ├── docker-compose.yml
    ├── .env
    └── start.sh
```
