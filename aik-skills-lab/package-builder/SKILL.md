---
name: package-builder
description: 为Spring Boot项目构建可部署产物，支持jar包和tar包两种格式。jar包用于传统部署，tar包包含启动脚本、外置配置文件目录结构，便于标准化部署实施。
type: Skill
version: 1.0.0
---

# Package Builder

## Purpose

为Spring Boot项目构建可部署的产物包，支持两种格式：
- **jar包**：标准Spring Boot可执行jar，适用于熟悉Java部署的运维人员
- **tar包**：包含jar、启动脚本、配置文件目录的完整部署包，便于标准化部署

## When to Use

- 项目开发完成，需要生成部署包
- 需要区分开发配置和部署配置
- 需要外置配置文件便于运维修改
- 需要提供启动脚本简化部署操作

## Package Formats

### 1. Jar包（标准格式）

```
target/
└── myapp-1.0.0.jar          # 可执行jar
```

**特点：**
- 标准Spring Boot打包
- 内置所有依赖
- 使用 `java -jar` 启动
- 配置文件可外置覆盖

**构建命令：**
```bash
mvn clean package
```

### 2. Tar包（部署套件）

```
myapp-1.0.0.tar.gz
├── bin/
│   ├── startup.sh           # 启动脚本
│   ├── shutdown.sh          # 停止脚本
│   └── status.sh            # 状态检查脚本
├── config/
│   ├── application.yml      # 外置主配置（运维修改）
│   └── logback-spring.xml   # 外置日志配置
├── lib/
│   └── myapp-1.0.0.jar      # 应用jar
├── logs/
│   └── (空目录，用于存放日志)
└── README.md                # 部署说明
```

**特点：**
- 标准化目录结构
- 配置文件外置，便于运维修改
- 提供启动/停止脚本
- 日志目录预创建

## Build Configuration

### Maven配置（pom.xml）

```xml
<build>
    <finalName>${project.artifactId}-${project.version}</finalName>
    
    <plugins>
        <!-- Spring Boot打包插件 -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <executable>true</executable>
                <layout>ZIP</layout>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        
        <!-- Tar包打包插件 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.6.0</version>
            <configuration>
                <descriptors>
                    <descriptor>src/main/assembly/assembly.xml</descriptor>
                </descriptors>
            </configuration>
            <executions>
                <execution>
                    <id>make-assembly</id>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Assembly配置（src/main/assembly/assembly.xml）

```xml
<assembly>
    <id>deploy</id>
    <formats>
        <format>tar.gz</format>
    </formats>
    
    <includeBaseDirectory>true</includeBaseDirectory>
    
    <fileSets>
        <!-- 启动脚本 -->
        <fileSet>
            <directory>src/main/bin</directory>
            <outputDirectory>bin</outputDirectory>
            <fileMode>0755</fileMode>
        </fileSet>
        
        <!-- 配置文件 -->
        <fileSet>
            <directory>src/main/config</directory>
            <outputDirectory>config</outputDirectory>
        </fileSet>
        
        <!-- 日志目录 -->
        <fileSet>
            <directory>src/main/logs</directory>
            <outputDirectory>logs</outputDirectory>
        </fileSet>
        
        <!-- README -->
        <fileSet>
            <directory>src/main/assembly</directory>
            <outputDirectory>.</outputDirectory>
            <includes>
                <include>README.md</include>
            </includes>
        </fileSet>
    </fileSets>
    
    <files>
        <!-- 应用jar -->
        <file>
            <source>target/${project.artifactId}-${project.version}.jar</source>
            <outputDirectory>lib</outputDirectory>
        </file>
    </files>
</assembly>
```

## Startup Script（startup.sh）

```bash
#!/bin/bash

# 应用名称
APP_NAME="myapp"
APP_JAR="lib/myapp-1.0.0.jar"

# 目录设置
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_DIR="$APP_HOME/config"
LOGS_DIR="$APP_HOME/logs"

# JVM参数（可根据服务器配置调整）
JAVA_OPTS="-server -Xms512m -Xmx1024m -XX:+UseG1GC"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=$LOGS_DIR/heap_dump.hprof"

# Spring Boot参数
SPRING_OPTS="--spring.config.location=file:$CONFIG_DIR/"
SPRING_OPTS="$SPRING_OPTS --logging.config=file:$CONFIG_DIR/logback-spring.xml"

# 检查jar是否存在
if [ ! -f "$APP_HOME/$APP_JAR" ]; then
    echo "Error: $APP_JAR not found!"
    exit 1
fi

# 检查是否已启动
PID=$(pgrep -f "$APP_JAR")
if [ -n "$PID" ]; then
    echo "Warning: $APP_NAME is already running (PID: $PID)"
    exit 1
fi

# 启动应用
echo "Starting $APP_NAME ..."
nohup java $JAVA_OPTS -jar "$APP_HOME/$APP_JAR" $SPRING_OPTS > "$LOGS_DIR/startup.log" 2>&1 &

# 等待启动
sleep 3
PID=$(pgrep -f "$APP_JAR")
if [ -n "$PID" ]; then
    echo "$APP_NAME started successfully (PID: $PID)"
    echo $PID > "$APP_HOME/bin/application.pid"
else
    echo "Failed to start $APP_NAME"
    exit 1
fi
```

## Shutdown Script（shutdown.sh）

```bash
#!/bin/bash

APP_NAME="myapp"
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
PID_FILE="$APP_HOME/bin/application.pid"

# 获取PID
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
else
    PID=$(pgrep -f "myapp-1.0.0.jar")
fi

if [ -z "$PID" ]; then
    echo "Warning: $APP_NAME is not running"
    exit 0
fi

echo "Stopping $APP_NAME (PID: $PID) ..."
kill -15 $PID

# 等待进程结束
for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "$APP_NAME stopped successfully"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# 强制结束
echo "Force stopping $APP_NAME ..."
kill -9 $PID
rm -f "$PID_FILE"
```

## Status Script（status.sh）

```bash
#!/bin/bash

APP_NAME="myapp"
APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
PID_FILE="$APP_HOME/bin/application.pid"

# 获取PID
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "$APP_NAME is running (PID: $PID)"
        
        # 显示内存使用
        MEM=$(ps -o rss= -p $PID | awk '{print int($1/1024)}')
        echo "Memory usage: ${MEM}MB"
        
        # 显示启动时间
        START_TIME=$(ps -o lstart= -p $PID)
        echo "Start time: $START_TIME"
        
        exit 0
    else
        echo "$APP_NAME is not running (stale PID file)"
        rm -f "$PID_FILE"
        exit 1
    fi
else
    PID=$(pgrep -f "myapp-1.0.0.jar")
    if [ -n "$PID" ]; then
        echo "$APP_NAME is running (PID: $PID, no PID file)"
        exit 0
    else
        echo "$APP_NAME is not running"
        exit 1
    fi
fi
```

## External Configuration

### 开发配置（src/main/resources/）

```yaml
# application.yml - 通用配置
spring:
  profiles:
    active: dev

---
# application-dev.yml - 开发环境
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb_dev
    username: root
    password: root
  
server:
  port: 8080

logging:
  level:
    root: INFO
    com.example: DEBUG
```

### 部署配置（src/main/config/）

```yaml
# application.yml - 部署环境（外置）
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb
    username: ${DB_USER:app_user}
    password: ${DB_PASSWORD:}
  
server:
  port: ${SERVER_PORT:8080}

logging:
  file:
    name: logs/application.log
  level:
    root: WARN
    com.example: INFO
```

## Build Commands

```bash
# 构建jar包
mvn clean package
# 输出: target/myapp-1.0.0.jar

# 构建tar包
mvn clean package
# 输出: target/myapp-1.0.0-deploy.tar.gz

# 跳过测试构建
mvn clean package -DskipTests

# 构建并安装到本地仓库
mvn clean install
```

## Deployment Instructions

### Jar包部署

```bash
# 1. 上传jar到服务器
scp target/myapp-1.0.0.jar user@server:/opt/myapp/

# 2. 外置配置文件（可选）
mkdir -p /opt/myapp/config
cp application.yml /opt/myapp/config/

# 3. 启动
java -jar /opt/myapp/myapp-1.0.0.jar \
  --spring.config.location=file:/opt/myapp/config/
```

### Tar包部署

```bash
# 1. 上传tar包
scp target/myapp-1.0.0-deploy.tar.gz user@server:/opt/

# 2. 解压
cd /opt
tar -xzf myapp-1.0.0-deploy.tar.gz
mv myapp-1.0.0 myapp

# 3. 修改配置（如需）
vim /opt/myapp/config/application.yml

# 4. 启动
cd /opt/myapp
bin/startup.sh

# 5. 检查状态
bin/status.sh

# 6. 停止
bin/shutdown.sh
```

## Output Location

```
target/
├── myapp-1.0.0.jar                  # 可执行jar
├── myapp-1.0.0.jar.original         # 原始jar（不含依赖）
└── myapp-1.0.0-deploy.tar.gz        # 部署套件

src/main/
├── assembly/
│   └── assembly.xml                 # 打包配置
├── bin/
│   ├── startup.sh                   # 启动脚本
│   ├── shutdown.sh                  # 停止脚本
│   └── status.sh                    # 状态脚本
├── config/
│   ├── application.yml              # 外置配置模板
│   └── logback-spring.xml           # 外置日志配置
└── logs/                            # 日志目录（空）
```
