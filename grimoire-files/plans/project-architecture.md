# AikSteinsGrimoire — Personal Knowledge Grimoire Architecture Plan

## Context

开发者已有一套成熟的 Agent Skills Library（47个技能），现在需要构建配套的**个人知识魔典** — AikSteinsGrimoire。Aik（署名）+ Steins（命运石之门，知识在时间线上的收束）+ Grimoire（魔典，代码即咒语）。

用于存放组件解决方案、代码片段、Markdown 学习笔记等内容。系统采用 Spring Boot 单体架构，纯后端 API，个人部署使用。

## Architecture Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 架构模式 | Spring Boot 单体 | 个人项目，无需微服务的复杂性 |
| 内容存储 | DB `LONGTEXT` | 简单备份、支持 LIKE 搜索、无同步问题 |
| 文件存储 | 本地磁盘，UUID 命名，日期分目录 | 无外部依赖，可直接浏览 |
| 审计字段 | 仅 `create_time` + `modify_time` | 单用户无需记录操作人 |
| 标签更新 | 删除全部 + 重新插入 | 简单，避免 diff 逻辑 |
| 分类树 | 全量加载 + 内存组装 | 预计 <1000 条，单次查询即可 |
| 表命名 | `aik_` 前缀，snake_case | 遵循 aIk-coding-style |
| 删除策略 | 物理删除（不做软删除） | 个人项目，无需回收站 |
| API 路径 | `/grimoire/{module}/{action}` | 遵循 aIk-coding-style 动词式规范 |
| ID 生成 | Hutool `IdUtil.getSnowflakeNextId()` | 遵循规范，IdType.INPUT |
| 认证授权 | 无 | 单用户个人部署 |

## Technology Stack

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 应用框架（2.7 LTS 最终版） |
| MyBatis-Plus | 3.5.5 | ORM + 分页 + Lambda 查询 |
| MySQL | 8.0 | 数据库 |
| Druid | 1.2.20 | 连接池 |
| Hutool | 5.8.25 | 通用工具（ID 生成、字符串、集合） |
| Lombok | latest (managed by Boot) | 代码精简 |
| Fastjson | 2.0.43 | JSON 序列化 |
| SpringDoc OpenAPI | 1.7.0 | Swagger UI 接口文档 |
| Commons IO | 2.11.0 | 文件操作辅助 |
| Commons FileUpload | 1.5 | 文件上传解析 |
| Commons Lang3 | 3.12.0 | 字符串/对象工具 |
| Maven | 3.6+ | 构建工具 |

> 与参照项目主要变化：Boot 从 2.2 → 2.7，Swagger 从 SpringFox → SpringDoc，去掉了 Nacos/Feign/Ribbon（微服务）、Redis、PostgreSQL、PDFBox 等个人项目不需要的依赖。

## Key Configuration Files

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <groupId>io.aik</groupId>
    <artifactId>aik-steins-grimoire</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>AikSteinsGrimoire</name>
    <description>a I k 的命运石魔典 — 个人知识库</description>

    <properties>
        <java.version>8</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <hutool.version>5.8.25</hutool.version>
        <springdoc.version>1.7.0</springdoc.version>
    </properties>

    <dependencies>
        <!-- Spring Boot 基础 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 数据库 -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
            <version>1.2.20</version>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- 工具库 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>2.0.43</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.12.0</version>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.11.0</version>
        </dependency>
        <dependency>
            <groupId>commons-fileupload</groupId>
            <artifactId>commons-fileupload</artifactId>
            <version>1.5</version>
        </dependency>

        <!-- API 文档 -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <fork>true</fork>
                    <addResources>true</addResources>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-deploy-plugin</artifactId>
                <configuration>
                    <skip>true</skip>
                </configuration>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>${project.basedir}/src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
            <resource>
                <directory>${project.basedir}/src/main/java</directory>
                <targetPath>${project.build.outputDirectory}/</targetPath>
                <includes>
                    <include>**/*.xml</include>
                </includes>
                <filtering>false</filtering>
            </resource>
        </resources>
    </build>
</project>
```

### application.yml

```yaml
server:
  servlet:
    context-path: /grimoire
  port: 18900
  tomcat:
    port-header: HEAD,DELETE,OPTIONS,TRACE,COPY,SEARCH,PROPFIND,BOGUS
    min-spare-threads: 20
    max-threads: 500
    max-connections: 10000

spring:
  jmx:
    enabled: false
  datasource:
    driverClassName: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://127.0.0.1:3306/aik_grimoire?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 50MB

mybatis-plus:
  mapper-locations: classpath:io/aik/steins/grimoire/**/dao/mapping/*.xml
  global-config:
    db-config:
      id-type: input
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

# --- 魔典自定义配置 ---
grimoire:
  file:
    base-path: ./grimoire-files
    max-size: 10MB
```

## Package Structure

项目采用**模块化架构**，每个模块自包含完整的 MVC 分层和通用组件，根包下不再放置公共目录。

```
io.aik.steins.grimoire/
├── AikSteinsGrimoireApplication.java
├── core/                               # 基础设施层（无业务，纯技术通用）
│   ├── po/                             # BaseEntity 等基础实体
│   ├── dto/                            # Result<T>, PageDTO 等统一返回
│   ├── exception/                      # BusinessException, GlobalExceptionHandler
│   ├── constant/                       # 通用常量（响应码等）
│   ├── enums/                          # 通用枚举（删除标志、状态等）
│   ├── utils/                          # SpringUtils, AssertUtils 等工具
│   └── config/                         # MyBatisPlusConfig, JacksonConfig 等
├── system/                             # 系统管理模块（有业务）
│   ├── controller/
│   ├── service/
│   │   └── impl/
│   ├── dao/
│   │   └── mapping/
│   └── common/
│       ├── po/
│       ├── dto/
│       ├── vo/
│       ├── constant/
│       ├── enums/
│       └── utils/
├── article/                            # 文章知识模块（示例）
│   ├── controller/
│   ├── service/
│   │   └── impl/
│   ├── dao/
│   │   └── mapping/
│   └── common/
│       ├── po/
│       ├── dto/
│       ├── vo/
│       ├── constant/
│       ├── enums/
│       └── utils/
└── sql/aik_tables.sql
```

### core 与 system 的设计区别

| | **core** | **system** |
|---|---|---|
| **定位** | 技术基础设施，无业务语义 | 业务模块，负责系统管理功能 |
| **对外暴露** | 不暴露 Controller（纯内部支撑） | 对外暴露 REST 接口 |
| **被依赖关系** | 被所有模块依赖 | 依赖 core，被其他业务模块可选依赖 |
| **典型内容** | BaseEntity、Result、全局异常、工具类、配置类 | 字典管理、参数配置、操作日志、文件管理 |

**划分原则**：被多个模块复用 → 下沉到 core；仅单个模块自用 → 留在该模块的 common 下。

core 包详细内容：
- **po/**：`BaseEntity`（含 `id`、`createTime`、`modifyTime` 等通用字段）
- **dto/**：`Result<T>`、`PageDTO`
- **exception/**：`BusinessException`、`GlobalExceptionHandler`
- **constant/**：通用响应码常量、通用状态常量
- **enums/**：
  - `IResultCode` — 响应码契约接口
  - `ResultCode` — 通用响应码枚举（SUCCESS、FAILURE、PARAM_ERROR、UNAUTHORIZED、FORBIDDEN、NOT_FOUND、INTERNAL_ERROR）
  - `StatusEnum` — 通用状态枚举（ENABLE、DISABLE）
  - `DeleteFlagEnum` — 删除标志枚举（NOT_DELETED、DELETED），配合 `BaseLogicEntity` 使用
- **utils/**：
  - `SpringUtils` — Spring 上下文工具（获取 Bean、发布事件）
  - `AssertUtils` — 业务断言工具（抛 `BusinessException`）
  - `JsonUtils` — JSON 序列化/反序列化统一入口（封装 Fastjson）
  - `ServletUtils` — Web 请求工具（获取 Request/Response、请求参数）
  - `IpUtils` — 客户端 IP 获取工具（处理代理头）
- **config/**：
  - `MyBatisPlusConfig` — MyBatis-Plus 分页插件拦截器
  - `JacksonConfig` — 定制 `ObjectMapper`（日期格式、时区、忽略 null）
  - `WebMvcConfig` — CORS 跨域、拦截器注册
  - `FileStorageConfig` — 文件存储配置项（`@ConfigurationProperties`）

## Database Design (6 tables)

### aik_article (knowledge entry)
| Field | Type | Description |
|-------|------|-------------|
| id | bigint PK | Snowflake ID |
| title | varchar(255) NOT NULL | title |
| content | longtext | Markdown body |
| article_type | varchar(32) NOT NULL | NOTE/COMPONENT/SOLUTION/CODE |
| category_id | bigint, INDEX | FK -> aik_category |
| summary | varchar(512) | short description for list |
| view_count | int DEFAULT 0 | read counter |
| status | tinyint DEFAULT 1 | 1=published, 0=draft |
| create_time / modify_time | datetime | timestamps |

### aik_category (tree structure)
| Field | Type | Description |
|-------|------|-------------|
| id | bigint PK | Snowflake ID |
| name | varchar(128) NOT NULL | category name |
| parent_id | bigint DEFAULT 0 | 0=root |
| sort_order | int DEFAULT 0 | display order |
| description | varchar(512) | optional |
| create_time / modify_time | datetime | timestamps |

### aik_tag
| Field | Type | Description |
|-------|------|-------------|
| id | bigint PK | Snowflake ID |
| name | varchar(64) NOT NULL UNIQUE | tag name |
| color | varchar(16) | hex color |
| create_time | datetime | timestamp |

### aik_article_tag (join table)
| Field | Type | Description |
|-------|------|-------------|
| id | bigint PK | Snowflake ID |
| article_id | bigint NOT NULL | FK |
| tag_id | bigint NOT NULL | FK |
| UNIQUE(article_id, tag_id) | | prevent duplicates |

### aik_code_snippet
| Field | Type | Description |
|-------|------|-------------|
| id | bigint PK | Snowflake ID |
| article_id | bigint NOT NULL | FK |
| language | varchar(32) | java/python/sql... |
| code_content | text NOT NULL | source code |
| description | varchar(255) | what this snippet does |
| sort_order | int DEFAULT 0 | order within article |

### aik_attachment (file metadata)
| Field | Type | Description |
|-------|------|-------------|
| id | bigint PK | Snowflake ID |
| article_id | bigint NOT NULL | FK |
| original_name | varchar(255) NOT NULL | original filename |
| stored_name | varchar(255) NOT NULL | UUID filename on disk |
| file_path | varchar(512) NOT NULL | relative path |
| file_size | bigint NOT NULL | bytes |
| file_type | varchar(128) | MIME type |
| download_count | int DEFAULT 0 | counter |
| create_time | datetime | upload time |

## API Design

### Article `/grimoire/article`
- `POST /grimoire/article/findPage` - paginated list (keyword, type, category, tags filter)
- `GET /grimoire/article/findById?id=` - full detail with tags, snippets, attachments
- `POST /grimoire/article/add` - create with tag IDs and code snippets
- `POST /grimoire/article/modify` - update (delete-and-re-insert tags/snippets)
- `POST /grimoire/article/remove?id=` - physical delete with cascade

### Category `/grimoire/category`
- `POST /grimoire/category/findTree` - full tree with children
- `POST /grimoire/category/add` / `modify` / `remove`

### Tag `/grimoire/tag`
- `POST /grimoire/tag/findPage` - paginated list
- `GET /grimoire/tag/findAll` - all tags (for selector)
- `POST /grimoire/tag/add` / `modify` / `remove`

### File `/grimoire/file`
- `POST /grimoire/file/upload?articleId=` - upload file
- `GET /grimoire/file/download?id=` - download (stream + Content-Disposition)
- `POST /grimoire/file/findByArticleId?articleId=` - list files
- `POST /grimoire/file/remove?id=` - delete file + record

## File Storage Strategy

```yaml
# application.yml
grimoire:
  file:
    base-path: ./grimoire-files
    max-size: 10MB
```

Directory: `{base-path}/yyyy/MM/dd/{uuid}.{ext}`

## Implementation Steps

### Phase 1: Foundation
1. Initialize Maven project (`aik-steins-grimoire`) with pom.xml (all dependencies)
2. Create `AikSteinsGrimoireApplication.java` + application.yml (MySQL, file config)
3. Create ApiResponse, BusinessException, GlobalExceptionHandler
4. Create MyBatisPlusConfig (pagination plugin)
5. Create FileStorageConfig (@ConfigurationProperties)
6. Create DDL script (aik_tables.sql) and execute

### Phase 2: Core Domain
7. Category module: Po -> Mapper -> Service -> Controller (tree assembly)
8. Tag module: Po -> Mapper -> Service -> Controller
9. Article module: Po + ArticleTagRelationPo + CodeSnippetPo -> Mappers
10. ArticleServiceImpl with full CRUD + tag/snippet management + @Transactional
11. ArticleController with all endpoints
12. Attachment module: Po -> Mapper -> Service
13. FileStorageUtil + FileController (upload/download/delete)

### Phase 3: Polish
14. Add SpringDoc OpenAPI annotations
15. Manual integration testing with Swagger UI
16. Verify no N+1 queries

### Phase 4: V2 (Future)
- Content versioning (aik_article_version table)
- Export as .md / batch export as .zip
- Full-text search (MySQL FULLTEXT or Elasticsearch)

## Verification

1. Start application, verify no startup errors
2. Swagger UI accessible at `/swagger-ui.html`
3. Test category CRUD via Swagger: create tree -> verify findTree returns nested structure
4. Test tag CRUD: add tags -> verify unique constraint
5. Test article full lifecycle: add with tags + snippets -> findPage with filters -> findById returns full detail -> modify -> remove cascades
6. Test file upload -> verify file on disk at correct path -> download -> remove deletes both record and file
7. Verify @Transactional rollback: corrupt a tag ID in add request -> verify no partial article record persists
