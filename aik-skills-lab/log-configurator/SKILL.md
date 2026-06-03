---
name: log-configurator
description: 为Spring Boot项目配置日志系统，使用Logback实现本地日志文件输出、按时间和大小轮转、定时清理。适用于不需要集中式日志收集的场景，满足基本运维需求。
type: Skill
version: 1.0.0
---

# Log Configurator

## Purpose

为Spring Boot项目配置本地日志系统，实现：
- 日志文件本地存储
- 按时间和大小自动轮转
- 定时清理过期日志
- 不同级别日志分离

## When to Use

- 需要本地日志文件记录
- 需要日志自动轮转防止磁盘占满
- 需要定期清理历史日志
- 不需要集中式日志收集（ELK等）

## Logback Configuration

### Maven依赖

Spring Boot已内置，无需额外添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- 已包含logback依赖 -->
```

### 开发环境配置（resources/logback-spring.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 彩色日志输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{50}) - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 根日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

    <!-- 开发环境SQL日志 -->
    <logger name="com.example.myapp.mapper" level="DEBUG"/>
</configuration>
```

### 部署环境配置（config/logback-spring.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 日志文件路径 -->
    <property name="LOG_PATH" value="logs"/>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"/>
    <property name="MAX_HISTORY" value="30"/>
    <property name="MAX_FILE_SIZE" value="100MB"/>
    <property name="TOTAL_SIZE_CAP" value="10GB"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <!-- 主日志文件（INFO及以上） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/application.log</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 按天轮转 -->
            <fileNamePattern>${LOG_PATH}/application.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <!-- 保留天数 -->
            <maxHistory>${MAX_HISTORY}</maxHistory>
            <!-- 总大小限制 -->
            <totalSizeCap>${TOTAL_SIZE_CAP}</totalSizeCap>
            <!-- 按大小轮转触发器 -->
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <!-- 错误日志（ERROR级别） -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>${MAX_HISTORY}</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <!-- 异步日志（提升性能） -->
    <appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="FILE"/>
        <queueSize>512</queueSize>
    </appender>

    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>

    <!-- 第三方库日志级别 -->
    <logger name="org.springframework" level="WARN"/>
    <logger name="org.apache" level="WARN"/>
    <logger name="com.zaxxer.hikari" level="INFO"/>
    <logger name="io.lettuce" level="INFO"/>
</configuration>
```

## Log Rotation Strategy

### 轮转策略说明

| 参数 | 说明 | 建议值 |
|------|------|--------|
| `maxHistory` | 保留天数 | 30天 |
| `maxFileSize` | 单文件大小 | 100MB |
| `totalSizeCap` | 总大小限制 | 10GB |

### 轮转文件名格式

```
logs/
├── application.log          # 当前日志
├── application.2024-03-18.0.log   # 当天的第1个轮转文件
├── application.2024-03-18.1.log   # 当天的第2个轮转文件
├── application.2024-03-17.0.log   # 前一天的日志
└── error.log                # 错误日志（当前）
```

## Log Cleanup

### 自动清理

Logback的`maxHistory`会自动清理过期日志，无需额外配置。

### 手动清理脚本（备用）

```bash
#!/bin/bash
# log-cleanup.sh - 日志清理脚本

LOG_DIR="/opt/myapp/logs"
RETENTION_DAYS=30

# 删除N天前的日志
find "$LOG_DIR" -name "*.log.*" -mtime +$RETENTION_DAYS -type f -delete

# 记录清理日志
echo "$(date '+%Y-%m-%d %H:%M:%S') - Cleaned logs older than $RETENTION_DAYS days" >> "$LOG_DIR/cleanup.log"
```

### Crontab定时任务

```bash
# 编辑crontab
crontab -e

# 每天凌晨3点执行清理
0 3 * * * /opt/myapp/bin/log-cleanup.sh

# 查看crontab
crontab -l
```

## Application Configuration

### application.yml

```yaml
logging:
  config: classpath:logback-spring.xml  # 开发环境
  # config: file:config/logback-spring.xml  # 部署环境
  level:
    root: INFO
    com.example.myapp: INFO
    com.example.myapp.mapper: DEBUG  # 开发环境打印SQL
```

### 部署环境覆盖

```bash
# 启动时指定日志配置
java -jar myapp.jar \
  --logging.config=file:/opt/myapp/config/logback-spring.xml
```

## Log Levels Guide

### 日志级别使用规范

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 系统错误，需要处理 | 数据库连接失败、业务异常 |
| WARN | 警告，需要注意 | 参数校验失败、资源不足 |
| INFO | 关键业务节点 | 订单创建成功、支付完成 |
| DEBUG | 调试信息 | 方法入参、执行步骤 |
| TRACE | 最详细跟踪 | SQL语句、循环内部 |

### 代码示例

```java
@Slf4j
@Service
public class OrderService {

    public Order createOrder(OrderDTO dto) {
        log.info("开始创建订单, userId={}, itemCount={}", 
            dto.getUserId(), dto.getItems().size());
        
        try {
            // 业务逻辑
            Order order = doCreate(dto);
            
            log.info("订单创建成功, orderId={}, orderNo={}", 
                order.getId(), order.getOrderNo());
            return order;
            
        } catch (BusinessException e) {
            log.warn("订单创建失败, userId={}, reason={}", 
                dto.getUserId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("订单创建异常, userId={}", dto.getUserId(), e);
            throw e;
        }
    }
}
```

## Log Viewing Commands

### 常用日志查看命令

```bash
# 实时查看日志
tail -f logs/application.log

# 查看最后100行
tail -n 100 logs/application.log

# 查看错误日志
tail -f logs/error.log

# 搜索关键字
grep "订单创建失败" logs/application.log

# 查看特定日期日志
cat logs/application.2024-03-18.0.log

# 统计错误数量
grep -c "ERROR" logs/application.log
```

## Disk Space Monitoring

### 磁盘检查脚本

```bash
#!/bin/bash
# disk-check.sh - 磁盘空间检查

LOG_DIR="/opt/myapp/logs"
THRESHOLD=80  # 告警阈值80%

# 检查日志目录磁盘使用率
USAGE=$(df -h "$LOG_DIR" | awk 'NR==2 {print $5}' | sed 's/%//')

if [ "$USAGE" -gt "$THRESHOLD" ]; then
    echo "WARNING: Log directory disk usage is ${USAGE}%"
    echo "Consider cleaning old logs or increasing disk space"
    
    # 可选：发送告警（邮件/钉钉/企业微信）
    # curl -X POST ...
fi
```

## Output Location

```
src/main/resources/
└── logback-spring.xml           # 开发环境日志配置

src/main/config/
└── logback-spring.xml           # 部署环境日志配置模板

deploy/config/
└── logback-spring.xml           # 实际部署日志配置

bin/
├── log-cleanup.sh               # 日志清理脚本
└── disk-check.sh                # 磁盘检查脚本
```

## Best Practices

### DO

- 使用SLF4J + Logback（Spring Boot默认）
- 使用占位符`{}`而不是字符串拼接
- 异常日志传递异常对象`log.error("msg", e)`
- 生产环境关闭DEBUG级别
- 定期检查和清理日志

### DON'T

- 不要记录敏感信息（密码、身份证号）
- 不要在循环中打印大量日志
- 不要使用System.out.println
- 不要依赖日志作为数据存储
