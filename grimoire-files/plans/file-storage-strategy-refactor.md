# 文件存储系统策略化改造计划

> 设计参考：idp-web-develop（策略模式 + 条件装配 + 配置分层）
> 技术栈：Spring Boot 2.7.18 + MyBatis-Plus 3.5.5 + Hutool 5.8.11
> 原则：参考设计理念，不照搬实现

---

## 一、设计目标

1. **存储策略可插拔**：local / oss / sftp 通过配置切换，业务代码零改动
2. **配置分层**：通用配置（允许格式、临时目录）与策略专属配置分离
3. **统一接口层**：Controller/Service 只依赖策略接口，不感知底层实现
4. **元数据完整**：MD5、存储类型、访问 URL、删除标记全部落库
5. **体验增强**：Content-Type 自动识别、预览/下载区分、逻辑删除

---

## 二、架构设计

### 2.1 包结构

```
core/
  storage/
    FileStorageStrategy.java        -- 策略接口
    AbstractFileStorage.java        -- 抽象模板（文件名生成、响应构建、Content-Type映射）
    LocalFileStorage.java           -- 本地存储 @ConditionalOnProperty
    OssFileStorage.java             -- 阿里云OSS @ConditionalOnProperty
    SftpFileStorage.java            -- SFTP存储 @ConditionalOnProperty

system/file/
  controller/FileController.java    -- 业务入口（下载增加preview参数）
  service/impl/FileServiceImpl.java -- 重构：注入 FileStorageStrategy
  po/FileRecordPo.java              -- 增强：新增字段
  dao/FileMapper.java               -- 不变

core/config/
  FileStorageConfig.java            -- 重构：参考idp配置分层设计
```

### 2.2 策略接口（参考 idp FileServerService 理念）

```java
public interface FileStorageStrategy {
    /** InputStream 上传 -- 最底层 */
    String upload(InputStream inputStream, String originalFilename) throws Exception;

    /** MultipartFile 上传 -- 便捷方法 */
    String upload(MultipartFile file) throws Exception;

    /** 下载为字节数组 */
    byte[] download(String storedPath) throws Exception;

    /** 直接写入 HttpServletResponse（由抽象模板封装公共逻辑） */
    void download(HttpServletResponse response, String storedPath) throws Exception;

    /** 删除文件 */
    boolean remove(String storedPath) throws Exception;

    /** 检查文件是否存在 */
    boolean exists(String storedPath) throws Exception;

    /** 获取访问URL */
    String getUrl(String storedPath);
}
```

### 2.3 抽象模板（参考 idp AbstractFileServer 理念）

```java
public abstract class AbstractFileStorage implements FileStorageStrategy {

    /** Snowflake 生成存储文件名，保留原始扩展名 */
    protected String generateStoredName(String originalFilename);

    /** 统一路径分隔符为 /，提取文件名 */
    protected String extractFileName(String filePath);

    /** Content-Type 映射表（图片/音频/视频/文档） */
    protected String resolveContentType(String filename);

    /** 构建下载响应：Content-Type + Content-Disposition */
    protected void buildDownloadResponse(HttpServletResponse response,
                                         byte[] data, String filename,
                                         boolean preview);

    /** MultipartFile 转 InputStream 的便捷实现 */
    @Override
    public String upload(MultipartFile file) throws Exception {
        return upload(file.getInputStream(), file.getOriginalFilename());
    }

    /** 下载到 Response 的公共逻辑（子类只需实现 doDownload） */
    @Override
    public void download(HttpServletResponse response, String storedPath) throws Exception {
        byte[] data = download(storedPath);
        // 设置 Content-Type / Content-Disposition 等
    }

    /** 子类只需实现真正的下载逻辑 */
    protected abstract byte[] doDownload(String storedPath) throws Exception;
}
```

### 2.4 条件装配

```java
@ConditionalOnProperty(
    prefix = "grimoire.file", name = "use",
    havingValue = "local", matchIfMissing = true
)
@Component
public class LocalFileStorage extends AbstractFileStorage { ... }

@ConditionalOnProperty(
    prefix = "grimoire.file", name = "use", havingValue = "oss"
)
@Component
public class OssFileStorage extends AbstractFileStorage { ... }

@ConditionalOnProperty(
    prefix = "grimoire.file", name = "use", havingValue = "sftp"
)
@Component
public class SftpFileStorage extends AbstractFileStorage { ... }
```

---

## 三、配置设计（参考 idp 配置分层）

```yaml
grimoire:
  file:
    # ---- 通用配置 ----
    use: local                       # local | oss | sftp
    local-tmp: ./grimoire-files/tmp  # 临时文件目录
    max-size: 10485760               # 10MB
    type-check-enabled: true         # 是否启用文件类型校验
    allow-types: jpg,png,gif,pdf,doc,docx,xls,xlsx,mp4,mp3,txt

    # ---- 策略专属配置（method 分层） ----
    method:
      local:
        base-path: ./grimoire-files/storage

      oss:
        endpoint: oss-cn-hangzhou.aliyuncs.com
        access-key-id: ${OSS_ACCESS_KEY_ID}
        access-key-secret: ${OSS_ACCESS_KEY_SECRET}
        bucket-name: aik-grimoire
        base-path: files/

      sftp:
        host: 192.168.229.128
        port: 22
        username: usftp
        password: ${SFTP_PASSWORD}
        private-key:                    # 可选：密钥路径
        base-path: /home/data/usftp/fileStorage
```

### 3.1 配置类设计

```java
@ConfigurationProperties(prefix = "grimoire.file")
public class FileStorageConfig {
    private String use;                 // local / oss / sftp
    private String localTmp;
    private Long maxSize;
    private Boolean typeCheckEnabled;
    private List<String> allowTypes;

    private MethodConfig method;

    @Data
    public static class MethodConfig {
        private LocalConfig local;
        private OssConfig oss;
        private SftpConfig sftp;
    }
}
```

---

## 四、数据模型变更

### aik_sys_file 表新增字段

| 字段名 | 类型 | 说明 |
|---|---|---|
| `storage_type` | VARCHAR(16) | 存储类型：local/oss/sftp |
| `md5` | VARCHAR(32) | 文件MD5哈希 |
| `url` | VARCHAR(512) | 访问URL |
| `del_flag` | TINYINT(1) | 删除标记：0=正常，1=已删除 |

---

## 五、变更清单

### 新建文件（5个）

| 路径 | 说明 |
|---|---|
| `core/storage/FileStorageStrategy.java` | 策略接口（含MultipartFile便捷方法） |
| `core/storage/AbstractFileStorage.java` | 抽象模板（文件名生成、响应构建、Content-Type） |
| `core/storage/LocalFileStorage.java` | 本地存储策略 |
| `core/storage/OssFileStorage.java` | 阿里云OSS策略 |
| `core/storage/SftpFileStorage.java` | SFTP策略（JSch实现） |

### 修改文件（5个）

| 路径 | 变更 |
|---|---|
| `core/config/FileStorageConfig.java` | 重构：use + method分层配置 |
| `system/file/po/FileRecordPo.java` | 新增storageType/md5/url/delFlag |
| `system/file/service/impl/FileServiceImpl.java` | 注入策略接口，重写上传/下载/删除 |
| `system/file/controller/FileController.java` | 下载增加preview参数，改为void写response |
| `sql/aik_system_tables.sql` | aik_sys_file增加4个字段 |

### 新增Maven依赖

- `com.aliyun.oss:aliyun-sdk-oss` -- OSS客户端
- `com.jcraft:jsch` -- SFTP客户端

---

## 六、改造步骤

1. 创建 `core/storage/` 策略抽象层（5个文件）
2. 重构 `FileStorageConfig` 为分层配置结构
3. 增强 `FileRecordPo` + SQL表结构
4. 重构 `FileServiceImpl` 注入策略接口
5. 重构 `FileController` 下载接口支持预览
6. pom.xml 引入 aliyun-sdk-oss + jsch 依赖
7. 编译、启动、验证三种存储模式

---

## 七、验证清单

- [ ] LocalFileStorage 上传/下载/删除正常
- [ ] OssFileStorage 上传/下载/删除正常
- [ ] SftpFileStorage 上传/下载/删除正常
- [ ] use=local 时装配 LocalFileStorage
- [ ] use=oss 时装配 OssFileStorage
- [ ] use=sftp 时装配 SftpFileStorage
- [ ] 下载图片返回 image/jpeg，PDF返回 application/pdf
- [ ] ?preview=true 返回 inline，默认返回 attachment
- [ ] 删除后 del_flag=1，物理文件保留
- [ ] MD5正确计算并落库
- [ ] 切换 use 配置后重启，策略自动切换

---

## 八、风险

1. 数据库需 ALTER TABLE，已有数据填充默认值
2. 下载接口从 ResponseEntity<Resource> 改为 void（写response），前端需适配
3. SFTP 连接池需考虑（JSch 每次新建连接，高并发需优化）
4. 路径统一使用正斜杠 /
