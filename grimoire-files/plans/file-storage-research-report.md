# 文件存储系统学习报告与改造计划

> 调研时间：2026-08-26
> 参考项目：idp-web-develop（主要）、hussar-web（次要）
> 查询资料：CSDN、掘金、GitHub 开源项目

---

## 一、参考项目学习结果

### 1.1 idp-web-develop（主要参考）

#### 架构设计：策略模式 + 独立模块

```
idp-dio/                    ← 独立文件服务模块（与业务解耦）
  api/
    FileServerService.java   ← 统一接口：upload/download/remove/checkExists
  service/
    AbstractFileServer.java  ← 抽象模板：文件名生成(Snowflake)、响应构建
    LocalFileServer.java     ← 本地存储实现 @ConditionalOnProperty
    OssFileServer.java       ← 阿里云OSS实现 @ConditionalOnProperty
    SftpFileServer.java      ← SFTP实现 @ConditionalOnProperty
```

**核心设计亮点：**

| 设计点 | 实现 | 价值 |
|---|---|---|
| 策略模式 | `FileServerService` 接口 + 多实现 | 切换存储方式只需改配置，零代码改动 |
| 条件装配 | `@ConditionalOnProperty(prefix="idp.file-server", name="use")` | 启动时按配置自动装配对应 Bean |
| 模板方法 | `AbstractFileServer` 封装公共逻辑 | 子类只需实现 `doDownload()` |
| 文件名生成 | Snowflake 雪花 ID + 保留扩展名 | 全局唯一、有序、防重名 |
| 存储方式 | 支持 oss / sftp / uds / local 四种 | 覆盖云存储、网络存储、本地存储 |

**DocFilePo 元数据设计（值得借鉴）：**

```java
DocFilePo {
    id, fileName, filePath,        // 基础信息
    pdfPath, contrastPath,         // 转换后路径（Word→PDF→对比）
    htmlPath,                      // 预览用HTML路径
    fileSize, md5,                 // 大小 + 哈希
    createTime
}
```

#### 文件管理业务层（idp-dm）

`FileController` 提供的业务能力：
- 查询文件列表 / 按目录查询 / 条件搜索
- 修改文件信息 / 删除 / 强制删除
- 获取文件流 / 获取文件基本信息
- **Word 转 HTML**（结构化文档预览）
- 分片上传 `FileSliceUploadController`

---

### 1.2 hussar-web（次要参考）

#### aih 模块（用户封装层）

`AihFileUploadServiceImpl` 基于底层 `AttachmentManagerService` 封装，亮点：

| 能力 | 实现 |
|---|---|
| **MediaType 映射表** | 静态 Map 维护 20+ 种文件的 Content-Type（图片/音频/视频/文档） |
| **预览 vs 下载** | 根据 `download` 参数区分：预览用 `inline`，下载用 `attachment` |
| **视频转码** | FFmpeg 转码 ProRes → WebM/VP9，浏览器兼容播放 |
| **转码缓存** | 同级目录 `transcode_cache/` 缓存，双重检查锁防并发重复转码 |
| **降级策略** | 转码失败时自动降级返回原始文件 |
| **字节数组上传** | 内部类 `ByteArrayMultipartFile` 将 byte[] 包装为 MultipartFile |

#### filemanage 模块（文件管理系统）

`FileRecordServiceImpl` 亮点：

| 能力 | 实现 |
|---|---|
| **逻辑删除** | `delFlag` 字段（0=正常，1=已删除），非物理删除 |
| **权限控制** | 用户级 + 角色级权限检查（view/download 维度） |
| **文件夹管理** | `folderId` 字段支持目录结构 |
| **批量操作** | 批量删除 + 事务保证 |

---

### 1.3 互联网最佳实践汇总

| 来源 | 关键结论 |
|---|---|
| CSDN《基于SpringBoot的微服务文件上传下载组件》 | 策略模式 + 工厂模式 + 模板方法模式三层架构；UploadComponent 作为上下文统一入口 |
| CSDN《Springboot 一行代码实现文件上传 20个平台》 | **spring-file-storage** 开源工具，支持本地/FTP/SFTP/阿里云OSS/腾讯云COS/MinIO 等 20+ 平台 |
| CSDN《通过S3协议实现通用的文件存储服务中间件》 | 基于 S3 协议抽象，兼容多厂商 OSS；门面模型屏蔽底层差异 |
| JavaGuide《Java优质开源实战项目》 | 推荐 qiwen-file（分布式文件系统，支持本地/OSS/FastDFS/MinIO）、zfile（在线网盘，支持 S3/OneDrive/Google Drive/本地等） |
| CSDN《如何自己搭建oss》 | 个人/小团队选 MinIO（最简单、功能全、兼容S3）；企业级选 Ceph |

---

## 二、当前项目（AikSteinsGrimoire）现状分析

### 2.1 现有能力

```
system/file/
  controller/FileController.java      ← 上传/下载/分页/重命名/删除
  service/impl/FileServiceImpl.java   ← 本地存储实现
  po/FileRecordPo.java                ← 文件记录（7个字段）
  dao/FileMapper.java
core/config/FileStorageConfig.java    ← @ConfigurationProperties 配置
```

**已有优点：**
- 配置化：basePath / maxSize / typeCheckEnabled / allowTypes 均支持配置
- 类型白名单校验：MIME + 扩展名双重校验
- 日期分目录：`yyyy/MM/dd` 按天分层，避免单目录膨胀
- UUID 存储名：防重名、防特殊字符
- 下载计数：记录下载次数

### 2.2 现有问题

| 问题 | 影响 | 严重程度 |
|---|---|---|
| **仅支持本地存储** | 无法切换到 OSS/MinIO，扩展性为零 | 高 |
| **物理删除** | 误删不可恢复，无回收站机制 | 中 |
| **无 MD5/哈希** | 无法实现秒传、重复文件检测 | 中 |
| **下载无 Content-Type 识别** | 所有文件都是 `application/octet-stream`，浏览器无法预览图片/视频/PDF | 中 |
| **无预览能力** | 只能下载，不能在线预览 | 中 |
| **无权限控制** | 任何用户可下载任何文件 | 低（当前无用户体系） |
| **路径分隔符硬编码** | `File.separator` 在 Windows 下是 `\`，拼接 URL 时可能出问题 | 低 |
| **无分片上传** | 大文件上传容易失败/超时 | 低 |

---

## 三、改造计划

### Phase 1：存储抽象与基础增强（推荐先做）

**目标**：建立策略模式架构，增强下载体验，补齐元数据。

| 任务 | 说明 |
|---|---|
| 1. 抽象 `FileStorageStrategy` 接口 | 定义 `upload(InputStream, filename)` / `download(path)` / `remove(path)` / `exists(path)` |
| 2. 实现 `LocalStorageStrategy` | 将现有本地存储逻辑迁移为策略实现 |
| 3. 重构 `FileServiceImpl` | 注入 `FileStorageStrategy`，不再直接操作文件系统 |
| 4. 增强 `FileRecordPo` | 新增 `md5`、`storageType`、`delFlag`、`url` 字段 |
| 5. 上传增加 MD5 计算 | 为秒传和重复检测打基础 |
| 6. 下载支持 Content-Type 自动识别 | 参考 aih 模块的 MEDIA_TYPE_MAP，图片/视频/PDF 可预览 |
| 7. 逻辑删除改造 | `remove()` 改为设置 `delFlag=1`，新增物理清理任务 |
| 8. 路径规范化 | 统一使用正斜杠 `/` 作为相对路径分隔符 |

### Phase 2：云存储与高级功能

| 任务 | 说明 |
|---|---|
| 9. 实现 `OssStorageStrategy` | 阿里云 OSS 支持，`@ConditionalOnProperty` 条件装配 |
| 10. 实现 `MinioStorageStrategy` | MinIO 支持（私有化 S3 兼容） |
| 11. 配置增强 | `grimoire.file.storage-type=local/oss/minio` 切换 |
| 12. 分片上传 | 大文件分片上传 + 断点续传 |
| 13. 秒传 | 基于 MD5 的秒传：上传前检查 MD5，已存在直接返回 |

### Phase 3：扩展能力（按需）

| 任务 | 说明 |
|---|---|
| 14. 文件夹管理 | 支持目录结构（参考 filemanage 的 `folderId`） |
| 15. 权限控制 | 用户/角色级文件权限（view/download） |
| 16. 视频转码 | FFmpeg 转码缓存（参考 aih 模块） |
| 17. 缩略图生成 | 图片缩略图、PDF 封面图 |
| 18. Word/PPT 转 HTML | 在线文档预览（参考 idp-dm） |

---

## 四、技术选型建议

| 场景 | 建议方案 | 理由 |
|---|---|---|
| 快速接入多平台 | **spring-file-storage**（开源） | 一行代码支持 20+ 平台，省去自己写策略模式 |
| 自己掌控架构 | 手写策略模式（参考 idp-dio） | 更贴合现有代码风格，无额外依赖 |
| 私有化部署 | **MinIO** | 兼容 S3 协议，单机部署简单，Docker 一键启动 |
| 云存储 | 阿里云 OSS / 腾讯云 COS | 稳定可靠，按量付费 |

**对本项目的建议：**

考虑到当前项目规模和技术栈统一性，**推荐 Phase 1 手写策略模式**（不引入 spring-file-storage），原因：
1. 当前代码量小，手写策略模式足够简单
2. 保持与 `aIk-coding-style` 编码风格一致
3. 避免引入第三方依赖增加复杂度
4. 为后续 knowledge 模块的附件管理提供统一入口

---

## 五、与 knowledge 模块的关联

当前 knowledge 模块的附件通过 `SysAttachmentMapper` 直接操作，没有走 file 模块的文件服务。改造后：

```
KnowledgeServiceImpl ──→ FileService（统一文件服务）
                              │
                              ├──→ LocalStorageStrategy
                              ├──→ OssStorageStrategy
                              └──→ MinioStorageStrategy
```

这样 knowledge 的附件也能享受：
- 统一的 Content-Type 识别和预览
- 云存储切换能力
- 逻辑删除保护
- 秒传能力

---

## 六、总结

| 维度 | 现状 | 目标 |
|---|---|---|
| 架构 | 单体本地存储 | 策略模式，支持本地/OSS/MinIO |
| 元数据 | 7 字段 | 11+ 字段（含 MD5、存储类型、删除标记） |
| 删除 | 物理删除 | 逻辑删除 + 定期清理 |
| 下载 | 全部二进制流 | 智能 Content-Type，支持预览 |
| 扩展 | 0 | 分片上传、秒传、转码、缩略图 |

**推荐下一步**：先执行 Phase 1，建立策略模式骨架并增强基础能力。预计改动 5-7 个文件，1-2 小时完成。
