---
name: health-check-designer
description: 为Spring Boot项目配置轻量级健康检查和监控端点，使用Spring Boot Actuator提供应用健康状态、基本信息和运行时指标。适用于简单的应用状态监控需求。
type: Skill
version: 1.0.0
---

# Health Check Designer

## Purpose

为Spring Boot项目配置轻量级健康检查和监控端点，使用Spring Boot Actuator提供：
- 应用健康状态检查
- 基本信息展示
- 运行时指标监控

## When to Use

- 需要检查应用是否正常运行
- 需要简单的监控端点
- 需要与负载均衡器配合（健康检查）
- 需要基本的运行时信息

## Actuator Configuration

### Maven依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 基础配置（application.yml）

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 暴露的端点
      base-path: /actuator            # 基础路径
  endpoint:
    health:
      show-details: when_authorized   # 健康详情显示策略
      probes:
        enabled: true                 # 启用k8s探针
    info:
      enabled: true
    metrics:
      enabled: true
```

### 安全配置

```yaml
management:
  server:
    port: 8081                      # 独立端口（可选）
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never           # 生产环境不显示详情
```

## Available Endpoints

### 1. Health端点（/actuator/health）

**用途：** 检查应用健康状态

**响应示例：**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 250790436864,
        "free": 12345678901,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

**状态说明：**
- `UP`：健康
- `DOWN`：不健康（数据库连接失败等）
- `OUT_OF_SERVICE`：停止服务（手动标记）
- `UNKNOWN`：未知状态

### 2. Info端点（/actuator/info）

**用途：** 展示应用基本信息

**配置（application.yml）：**
```yaml
info:
  app:
    name: @project.name@
    description: @project.description@
    version: @project.version@
    encoding: @project.build.sourceEncoding@
    java:
      version: @java.version@
  build:
    time: @maven.build.timestamp@
```

**响应示例：**
```json
{
  "app": {
    "name": "myapp",
    "description": "My Application",
    "version": "1.0.0",
    "encoding": "UTF-8",
    "java": {
      "version": "1.8.0_202"
    }
  },
  "build": {
    "time": "2024-03-18T10:30:00Z"
  }
}
```

### 3. Metrics端点（/actuator/metrics）

**用途：** 展示运行时指标

**可用指标：**
```
/actuator/metrics
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "jvm.threads.live",
    "jvm.threads.peak",
    "process.cpu.usage",
    "system.cpu.usage",
    "http.server.requests"
  ]
}
```

**具体指标（/actuator/metrics/jvm.memory.used）：**
```json
{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 123456789
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": ["heap", "nonheap"]
    }
  ]
}
```

## Custom Health Indicators

### 自定义健康检查

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("status", "Connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("status", "Invalid connection")
                    .build();
            }
        } catch (SQLException e) {
            return Health.down()
                .withDetail("database", "MySQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 业务健康检查

```java
@Component
public class BusinessHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // 检查业务关键依赖
        boolean orderServiceOk = checkOrderService();
        boolean paymentServiceOk = checkPaymentService();

        if (orderServiceOk && paymentServiceOk) {
            return Health.up()
                .withDetail("orderService", "Available")
                .withDetail("paymentService", "Available")
                .build();
        } else {
            return Health.down()
                .withDetail("orderService", orderServiceOk ? "Available" : "Unavailable")
                .withDetail("paymentService", paymentServiceOk ? "Available" : "Unavailable")
                .build();
        }
    }
}
```

## Kubernetes Probes

### 探针配置

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState,ping
        readiness:
          include: readinessState,db,redis
```

### 探针端点

- **Liveness**（存活探针）：`/actuator/health/liveness`
  - 应用是否存活，失败则重启容器
  
- **Readiness**（就绪探针）：`/actuator/health/readiness`
  - 应用是否就绪接收流量，失败则从负载均衡移除

### K8s Deployment配置

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: myapp
          image: myapp:1.0.0
          ports:
            - containerPort: 8080
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
```

## Health Check Usage

### 负载均衡器检查

```nginx
# Nginx健康检查
upstream myapp {
    server 192.168.1.10:8080;
    server 192.168.1.11:8080;
    
    check interval=3000 rise=2 fall=3 timeout=1000 type=http;
    check_http_send "GET /actuator/health HTTP/1.0\r\n\r\n";
    check_http_expect_alive http_2xx http_3xx;
}
```

### 监控脚本

```bash
#!/bin/bash
# 健康检查脚本

HEALTH_URL="http://localhost:8080/actuator/health"

response=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL")

if [ "$response" == "200" ]; then
    echo "Health check passed"
    exit 0
else
    echo "Health check failed: HTTP $response"
    exit 1
fi
```

### Docker HEALTHCHECK

```dockerfile
# Dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

## Security Considerations

### 生产环境安全配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info  # 仅暴露必要端点
      base-path: /actuator
  endpoint:
    health:
      show-details: never     # 不显示详情
      show-components: never  # 不显示组件详情
```

### 安全加固

```java
@Configuration
public class ActuatorSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests()
            .requestMatchers(EndpointRequest.to("health")).permitAll()
            .requestMatchers(EndpointRequest.to("info")).permitAll()
            .anyRequest().authenticated()
            .and()
            .httpBasic();
    }
}
```

## Output Location

```
src/main/java/com/example/myapp/actuator/
├── DatabaseHealthIndicator.java      # 数据库健康检查
├── BusinessHealthIndicator.java      # 业务健康检查
└── CustomInfoContributor.java        # 自定义信息

src/main/resources/
└── application.yml                   # Actuator配置
```

## Best Practices

### DO

- 启用health端点用于负载均衡检查
- 自定义关键依赖的健康检查
- 生产环境限制端点暴露范围
- 使用独立端口管理端点（可选）

### DON'T

- 不要暴露敏感端点到公网
- 不要在健康检查中执行耗时操作
- 不要过度依赖Actuator替代专业监控
