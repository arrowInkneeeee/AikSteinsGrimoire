# IntelliJ IDEA 全局调优方案

> 针对高配开发机（32G 内存 / i9-14900HX 32 线程 / RTX 4060），面向多大型项目场景的 IDEA 完整调优指南，涵盖 JVM 参数（vmoptions）和自定义属性（idea.properties）。

## 适用场景

- 物理机内存 32G，CPU 高核心数（24核/32线程以上）
- 同时维护多个大型项目（如轻骑兵底座 + LIMS 产品 + 项目定制三层架构）
- IDEA 版本 2023.x 及以上，运行在 JBR（JetBrains Runtime，基于 JDK 17+）

## 硬件基线

| 硬件 | 型号/规格 | 调优关联 |
|------|-----------|----------|
| CPU | i9-14900HX（8P+16E，32线程） | GC 线程数、JIT 线程数 |
| 内存 | 32G DDR5 | 堆分配、元空间上限 |
| GPU | RTX 4060 Laptop | Java2D 渲染管线选择 |
| 存储 | NVMe SSD | 索引和缓存 I/O 无瓶颈 |

## 最终配置（已验证）

```properties
# ------------------------------------------------------------------
# IntelliJ IDEA JVM Options (针对 32G 内存高配机优化)
# ------------------------------------------------------------------

# 【核心内存】
-Xms8g
-Xmx12g

# 【代码缓存】
-XX:ReservedCodeCacheSize=2g

# 【元空间】
-XX:MaxMetaspaceSize=2g

# 【垃圾回收】G1 + 大堆配置
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:ConcGCThreads=6
-XX:ParallelGCThreads=16
-XX:+UseStringDeduplication

# 【软引用】
-XX:SoftRefLRUPolicyMSPerMB=100

# 【JIT 编译】
-XX:CICompilerCount=8

# 【容错与诊断】
-XX:+IgnoreUnrecognizedVMOptions
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=$USER_HOME/java_error_in_idea.hprof
-XX:ErrorFile=$USER_HOME/java_error_in_idea_%p.log
-XX:-OmitStackTraceInFastThrow

# 【JDK 模块开放】
--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED
--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.management/sun.management=ALL-UNNAMED

# 【系统属性】
-Dsun.io.useCanonCaches=false
-Djdk.http.auth.tunneling.disabledSchemes=""
-Dkotlinx.coroutines.debug=off
-Dsun.java2d.opengl=true

# 【激活】
-javaagent:D:\JeBrainsWorkSpace\unlock\jetbra\ja-netfilter.jar=jetbrains
```

## 逐项决策理由

### 核心内存

| 参数 | 值 | 理由 |
|------|-----|------|
| `-Xms8g` | 8G | 大型多层项目索引量巨大，启动时直接预分配避免频繁扩容 |
| `-Xmx12g` | 12G | 32G 机器分 12G 给 IDEA，保证索引+PSI缓存+多Tab编辑+Git历史分析 |

### 代码缓存与元空间

| 参数 | 值 | 理由 |
|------|-----|------|
| `ReservedCodeCacheSize` | 2G | 三层架构（框架+产品+项目）类数量级大，JIT 热点方法多，防止 CodeCache 满后 JIT deoptimization |
| `MaxMetaspaceSize` | 2G | 兜底保护。不设上限 = 异常时无限吃内存直到系统崩溃；设上限则异常时抛 OOM 可被 HeapDump 捕获分析 |

### GC 策略

| 参数 | 值 | 理由 |
|------|-----|------|
| `+UseG1GC` | 启用 | JBR 标配，12G 大堆低延迟 |
| `MaxGCPauseMillis` | 200ms | IDE 交互流畅度保障，JetBrains 默认值 |
| `ConcGCThreads` | 6 | 接近 JBR 自动计算值（32线程CPU约算出6），后台并发标记不抢前台 |
| `ParallelGCThreads` | 16 | JBR 默认公式对 32 线程 CPU 算出约 23，写死 16 反而是限制 STW 阶段 CPU 占用 |
| `+UseStringDeduplication` | 启用 | 大项目中重复字符串（包名、类路径）极多，G1 Young GC 顺带去重，开销小收益大 |

**删除 `GCPauseIntervalMillis=500` 的理由**：
- 约束"两次暂停之间最少间隔500ms"，在 12G 堆上逼 G1 提前触发回收
- 与 `MaxGCPauseMillis=200` 组合产生矛盾：堆压力大时想回收被间隔约束卡住，等间隔到了又必须一次回收更多
- 该参数面向持续吞吐服务设计，IDEA 间歇负载不适用

### 缓存与编译

| 参数 | 值 | 理由 |
|------|-----|------|
| `SoftRefLRUPolicyMSPerMB` | 100 | 大项目 PSI 缓存重建代价高（打开一个类要重新解析整棵继承树），保留更久减少跳转卡顿 |
| `CICompilerCount` | 8 | JIT 编译是启动期突发负载（前2-3分钟），16线程同时编译会抢占 UI 导致卡顿；8线程足够快速暖机 |

### 删除 `-ea` 的理由

断言检查是 JDK 开发者调试用，普通 IDE 用户开着有少量运行时开销无实际收益。

### GPU 渲染说明

`-Dsun.java2d.opengl=true` 利用独显加速 UI 渲染。注意：
- JBR 2024+ 版本默认使用 Direct2D 管线，此配置可能覆盖更优默认行为
- 如 IDEA >= 2024.1 且 UI 已流畅，可尝试删除或改为 `-Dsun.java2d.d3d=true`
- 当前体验良好则保留不动

## 常见误区澄清

| 误区 | 正确理解 |
|------|----------|
| "MaxMetaspaceSize 不设上限更稳" | 不设 = 异常时吃光系统内存至整机崩溃，设上限 + HeapDump 才是可观测的防御策略 |
| "GC 线程数交给 JBR 自动" | 32 线程 CPU 自动算出 ParallelGCThreads≈23，比写死16更多更抢CPU，手动限制才正确 |
| "12G 堆太大了" | 单看小项目是大，但跨多个大型屎山项目使用时 12G 是合理分配 |
| "换 ZGC 更好" | ZGC 对超大堆（32G+）设计，12G 用 G1 足够好，且 JBR 对 G1 兼容性最优 |
| "2G CodeCache 太大" | 虚拟地址空间不值钱，大项目长时间运行后 JIT 产物确实可达 1G+，2G 保守安全 |

---

## idea.properties 自定义属性配置

### 最终配置

```properties
# custom IntelliJ IDEA properties

# 核心路径（迁移至 D 盘统一管理）
idea.system.path=D:/JeBrainsWorkSpace/IdeaSystemCache
idea.log.path=D:/JeBrainsWorkSpace/IdeaSystemCache/log

# Local History
idea.history.days.limit=30
idea.history.size.limit=1073741824

# 大文件与索引性能
idea.max.intellisense.filesize=5000
idea.max.content.load.filesize=30000

# 构建进程
compiler.process.heap.size=2048

# 网络
java.net.useSystemProxies=false

# UI
idea.true.smooth.scrolling=true
```

### 逐项决策理由

| 参数 | 值 | 理由 |
|------|-----|------|
| `idea.system.path` | D盘路径 | 索引缓存迁出 C 盘，避免 C 盘空间紧张，路径统一便于管理 |
| `idea.log.path` | 与 system 同级 | 消除启动警告，日志集中管理 |
| `idea.history.days.limit` | 30天 | 默认 5 天太短，大项目需更长回溯窗口 |
| `idea.history.size.limit` | 1GB | 大项目改动频繁，1G 配合 30 天足够覆盖 |
| `idea.max.intellisense.filesize` | 5000KB | LIMS 项目有巨型生成文件/SQL，跳过智能分析防卡顿。不影响编译和 JRebel 热部署（JRebel 监控 .class 文件，在 classloader 级别工作，与 IDEA PSI 索引完全独立） |
| `idea.max.content.load.filesize` | 30000KB | 偶尔需编辑大型 SQL 脚本或日志 |
| `compiler.process.heap.size` | 2048MB | 32G 内存充足，大项目编译进程给 2G 避免构建 OOM |
| `java.net.useSystemProxies` | false | 存在代理环境，避免 IDEA 自动检测与手动代理配置冲突 |
| `idea.true.smooth.scrolling` | true | 显式声明确保平滑滚动生效 |

### 不建议添加的属性

| 参数 | 为什么不加 |
|------|-----------|
| `idea.config.path` | 没有迁移配置目录的需求，保持默认避免插件路径问题 |
| `idea.plugins.path` | 插件目录跟着 config 走即可 |
| `idea.cycle.buffer.size` | 控制台缓冲区默认 1MB 足够，改大导致内存攀升 |
| `idea.popup.weight=heavy` | 老版本 Linux 适用，Windows + JBR 17+ 无需 |

---

## 配置文件位置

| 配置类型 | 文件 | 修改方式 |
|----------|------|----------|
| JVM 参数 | `<IDEA安装目录>/bin/idea64.vmoptions` | `Help → Edit Custom VM Options`（推荐） |
| 自定义属性 | `<IDEA安装目录>/bin/idea.properties` | `Help → Edit Custom Properties`（推荐） |
| 本方案源文件 | `scrolls/idea-jvm-tuning/idea64.vmoptions` | 直接编辑 |
| 本方案源文件 | `scrolls/idea-jvm-tuning/idea.properties` | 直接编辑 |

## 版本记录

| 日期 | 变更 |
|------|------|
| 2025-07-22 | 初始版本：JVM 参数调优，基于多轮分析和对比评审（含豆包方案对比）确定 |
| 2025-07-22 | 新增 idea.properties 自定义属性配置，含大文件/构建/代理/UI 优化 |
