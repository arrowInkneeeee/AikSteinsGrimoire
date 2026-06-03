---
name: config-manager
description: 管理Spring Boot项目的多环境配置，区分开发配置（application-dev.yml）和部署配置（外置application.yml）。自动检测pom.xml中的Nacos依赖，如存在则提供配置中心集成方案。
type: Skill
version: 1.0.0
---

# Config Manager

## Purpose

管理Spring Boot项目的多环境配置，实现：
- 开发环境配置（application-dev.yml）
- 部署环境配置（外置application.yml）
- 自动检测Nacos配置中心（pom中存在则提供集成）

## When to Use

- 需要区分开发和部署配置
- 需要外置配置文件便于运维修改
- 需要集成Nacos配置中心
- 需要管理多环境（dev/test/prod）

## Configuration Structure

### 开发环境配置

```
src/main/resources/
├── application.yml          # 通用配置 + 激活dev
├── application-dev.yml      # 开发环境
├── application-test.yml     # 测试环境（可选）
└── bootstrap.yml            # Nacos引导配置（如需要）
```

### 部署环境配置

```
src/main/config/
└── application.yml          # 部署环境模板

deploy/config/
└── application.yml          # 实际部署配置（运维修改）
```

## Configuration Templates

### application.yml（通用配置）

```yaml
# 激活的profile
spring:
  profiles:
    active: dev
  
  # 应用信息
  application:
    name: myapp

---
# 通用配置（所有环境共享）
spring:
  # Jackson配置
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
  
  # 文件上传
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 100MB

# 日志配置
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"
```

### application-dev.yml（开发环境）

```yaml
# 开发环境配置
server:
  port: 8080

spring:
  # 数据库配置
  datasource:
    url: jdbc:mysql://localhost:3306/myapp_dev?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
  
  # Redis配置
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# 日志级别
logging:
  level:
    root: INFO
    com.example.myapp: DEBUG
    com.example.myapp.mapper: DEBUG  # 打印SQL

# 开发工具
spring:
  devtools:
    restart:
      enabled: true
```

### application.yml（部署环境模板）

```yaml
# 部署环境配置（外置）
# 运维人员根据实际情况修改以下配置

server:
  port: ${SERVER_PORT:8080}

spring:
  # 数据库配置
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/myapp?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai}
    username: ${DB_USER:app_user}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
  
  # Redis配置
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    database: ${REDIS_DB:0}
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# 日志配置
logging:
  file:
    name: logs/application.log
  level:
    root: WARN
    com.example.myapp: INFO
```

## Nacos Integration

### 自动检测

检查pom.xml中是否存在Nacos依赖：

```xml
<!-- Nacos配置中心 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>

<!-- Nacos服务发现 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

### Nacos配置（bootstrap.yml）

```yaml
# 如检测到Nacos依赖，生成此文件
spring:
  application:
    name: myapp
  
  cloud:
    nacos:
      # 配置中心
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
        file-extension: yaml
        refresh-enabled: true
      
      # 服务发现
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: DEFAULT_GROUP
        metadata:
          version: 1.0.0
  
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

### Nacos配置数据结构

```yaml
# Data ID: myapp.yaml
# Group: DEFAULT_GROUP

server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp
    username: app_user
    password: encrypted_password
```

## Profile-Specific Configuration

### 多环境配置策略

```yaml
# application.yml - 通用配置
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

---
# application-dev.yml - 开发
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp_dev

---
# application-test.yml - 测试
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://test-server:3306/myapp_test

---
# application-prod.yml - 生产（仅fallback）
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/myapp
```

### 激活指定环境

```bash
# 命令行激活
java -jar myapp.jar --spring.profiles.active=prod

# 或环境变量
export SPRING_PROFILES_ACTIVE=prod
java -jar myapp.jar
```

## Externalized Configuration Priority

Spring Boot配置加载优先级（高到低）：

1. 命令行参数：`--server.port=8080`
2. Java系统属性：`System.getProperties()`
3. 环境变量
4. `application-{profile}.yml`（jar外部）
5. `application.yml`（jar外部）
6. `application-{profile}.yml`（jar内部）
7. `application.yml`（jar内部）

**部署时覆盖策略：**
```bash
# 方式1：外置配置文件
java -jar myapp.jar --spring.config.location=file:/opt/myapp/config/

# 方式2：命令行参数
java -jar myapp.jar --server.port=9090 --spring.datasource.password=secret

# 方式3：环境变量
export SERVER_PORT=9090
export DB_PASSWORD=secret
java -jar myapp.jar
```

## Configuration Validation

### 启动时校验

```java
@Component
@ConfigurationProperties(prefix = "spring.datasource")
@Validated
public class DataSourceProperties {
    
    @NotBlank(message = "数据库URL不能为空")
    private String url;
    
    @NotBlank(message = "数据库用户名不能为空")
    private String username;
    
    @NotBlank(message = "数据库密码不能为空")
    private String password;
    
    // getters/setters
}
```

### 配置类

```java
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        return new HikariDataSource(config);
    }
}
```

## Sensitive Data Handling

### 密码加密（Jasypt）

```xml
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>
```

```yaml
# 加密后的密码
spring:
  datasource:
    password: ENC(encrypted_password_here)

# 启动时指定密钥
java -jar myapp.jar --jasypt.encryptor.password=your_secret_key
```

### 密钥管理

```bash
# 生产环境建议：
# 1. 密钥不写入配置文件
# 2. 通过环境变量或启动参数传入
# 3. 使用KMS（密钥管理系统）

# 启动脚本中设置
export JASYPT_ENCRYPTOR_PASSWORD=$(cat /secure/encrypt_key)
java -jar myapp.jar
```

## Output Location

```
src/main/resources/
├── application.yml              # 通用配置
├── application-dev.yml          # 开发环境
├── application-test.yml         # 测试环境
└── bootstrap.yml                # Nacos引导（如需要）

src/main/config/
└── application.yml              # 部署配置模板

deploy/config/
└── application.yml              # 实际部署配置
```

## Best Practices

### DO

- 敏感信息使用占位符或加密
- 开发配置使用本地/内网地址
- 部署配置使用环境变量占位符
- 配置变更记录到版本控制

### DON'T

- 不要将生产密码提交到Git
- 不要在配置中硬编码IP地址
- 不要混用多种配置方式（保持统一）
