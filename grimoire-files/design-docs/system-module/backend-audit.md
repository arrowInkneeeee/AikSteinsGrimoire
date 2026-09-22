# 后端现状审计 — 功能盘点与模块间矛盾清单

> 版本：v1.1 | 更新：2026-09-20 | 状态：生效中 | 权威范围：**现状描述**（非设计意图）+ 本轮改造的**退役台账 / 改动边界 / 提交策略**（§10–§13）
> 权威优先级：本文档描述的是**代码与 DDL 的实测现状**，与本文档冲突时以实际代码为准。
> 冲突裁决顺序见 `../../README.md`。
>
> **范围边界（v1.1 澄清）**：**设计意图**仍归 `SDD.md` / `api-contract.md`（本文档**不定义**设计）。
> §10–§13 是**过程与边界**声明（退役什么、改了哪些文件、怎么提交），在各自范围内为唯一权威；
> 其中"已落地"字样均为**转述 t5 的实测结果**，不是本文档发起的动作。
>
> v1.0 初版：盘点 8 个功能域 / 30 个端点 / 74 个类 / 6027 行，列出 23 项矛盾。
> 方法：静态审查（读代码与 DDL）。**未编译、未运行测试**，运行时行为类结论见 §6。
> v1.1：新增 §10 退役声明（D1–D11）、§11 风险登记（R1–R6）、§12 改动边界、§13 提交策略；
> X19 / X20 标注为**已修复**并指明修复任务。**未改代码、未改库、未提交**。

---

## §0 结论摘要

| 问题 | 回答 |
|---|---|
| 功能是否完整？ | **不完整。** 知识模块定义了 4 张表，只实现了 1 张。分类、标签、附件、统计四个域合计 **15 个契约端点零实现** |
| 是否统一？ | **不统一。** 状态语义、删除语义、缓存所有权、分页形态、默认值兜底这五件事各有 2–3 个并行实现，且**互相不引用** |
| 模块间是否矛盾？ | **是。** 23 项，其中 3 项阻断级、4 项高危、5 项中危、11 项低危。最严重的是契约自身内部矛盾（X2）与知识模块聚合无落点（X1b） |

**一句话**：这不是风格不统一，是**同一种能力存在多个所有者，各自语义不同**。

**本轮（附件改造）的处置台账见 §10–§13**：§10 退役声明（D1–D11）｜§11 风险登记（R1–R6）｜§12 改动边界｜§13 提交策略。
§5 中的 **X19 / X20 已修复**（url 列与本地同值、上传/删除不清理磁盘）。

---

## §1 功能域盘点（8 域 / 30 端点 / 74 类）

| 功能域 | 包路径 | 端点数 | 已实现内容 | 完整度 |
|---|---|---|---|---|
| 知识条目 | `knowledge` | 6 | 增/改/删/分页/详情/切状态 | 🟡 支撑 4 张表，只做 1 张 |
| 字典类型 | `system.dict` | 6 | 分页/详情/类型带项/增/改/删 | 🟢 最完整 |
| 字典项 | `system.dict` | 6 | 按类型/批量映射/分页/增/改/删 | 🟢 完整 |
| 文件管理 | `system.file` | 5 | 上传/下载/分页/改名/删除 | 🟢 完整（3 存储策略） |
| 系统参数 | `system.param` | 7 | 分页/按key/按组/增/改/删/刷缓存 | 🟢 完整 |
| 附件 | `system.attachment` | **0** | 仅 PO + Mapper | 🔴 半成品 |
| 事件总线 | `components.eventbus` | 0 | 三阶段监听器架构 | ⚪ 零业务接入 |
| 线程池 | `components.threadpool` | 0 | 管理器/执行器/拒绝策略 | ⚪ 零业务接入 |
| 文件存储策略 | `core.storage` | — | local / oss / sftp | 🟡 oss/sftp 无依赖无配置 |

**端点基址**：全部为 `/grimoire/*`（`context-path: /`），**无 `/api` 前缀**。
**无 Controller**：category、tag、attachment、stats 四者均不存在。

### 1.1 契约漂移（30 个代码端点 vs 契约 26 个）

| 分类 | 数量 | 明细 |
|---|---|---|
| (a) 完全一致 | 11 | knowledge 全部 6 个 + §2.1.7 的 5 个 |
| (b) 路径/方法/参数不同 | 0 | — |
| (c) **契约有、代码无** | **15** | category 5、tag 6、attachment 3、stats 1 |
| **反向漂移：代码有、契约未列** | **19** | 契约 §1.1 L75 称"见 §2.1.7"，但该节仅列 5 个，4 个 Controller 实际有 24 个 |
| 端点存在但**载荷/响应结构未实现** | 4 | `/page` 缺 `keyword`/`tagId`/`includeChildren`；`/findById` 仍是嵌套 `KnowledgePo`；`/add`、`/update` 无 `attachments` |

---

## §2 阻断级矛盾（不修必返工）

| # | 矛盾 | 证据 |
|---|---|---|
| **X1a** | **前端设计依赖的 10 个接口后端不存在** | `frontend/design.md` 依赖 `/category/*`、`/tag/*`、`statistics`；代码仅 `KnowledgeController` 6 端点，`KnowledgeCategoryMapper`/`KnowledgeTagMapper` **零引用** |
| **X1b** | **契约 §2.2.4 / §2.3.4 的聚合与建树无落点** | 4 个知识 Mapper 的 `@Select/@Update/@Insert/@Delete` **计数全为 0**（纯 `BaseMapper`）；`core/tree/TreeUtils.java`+`TreeNode.java` **仅自引用**，是死代码 |
| **X2** | **契约要求的 `"status": null` 在技术上无法送达** | 契约 §2.2.3(L210) 要求管理端显式传 `"status": null` 查全部；但 `JacksonConfig.java:43` 设 `Include.NON_NULL`，**前端根本发不出该字段**。C2 的配套缓解措施不可实现 |
| **X3** | **附件模块跨模块归属矛盾** | 表 `aik_sys_attachment` 定义在 `sql/aik_knowledge_tables.sql:76`（knowledge 脚本），Java 在 `system/attachment/`，而 `KnowledgeServiceImpl` 跨模块直接注入 `SysAttachmentMapper` |

## §3 高危矛盾（真实缺陷）

| # | 矛盾 | 证据 |
|---|---|---|
| **X4** | **三套并行状态枚举 + 一套删除枚举** | `core.enums.StatusEnum`(1/0，**零引用**)、`KnowledgeConstant.STATUS_ENABLE`(:26，`Integer`)、`SystemConstant.STATUS_ENABLE`(:14，`int`)、`DeleteFlagEnum` |
| **X5** | **`status` 默认值语义分裂** | `KnowledgeServiceImpl.add:72` 有 `!= null ? : 1` 兜底；`update:99` 无；`DictTypeServiceImpl.add:89`、`DictItemServiceImpl.add:95` 均无。**注：`SystemParamPo` 无 status 字段，不适用** |
| **X6** | **`download_count` 丢更新** | `FileServiceImpl:101-104` 用"读值+1"而非原子自增；`rename:135-136` 全实体 `updateById` 会连带覆盖 |
| **X7** | **字典项缓存清理恒不生效** | `DictItemServiceImpl:119` 先改 `existing.dictCode`，`:129` 再比较 `existing.getDictCode().equals(dto.getDictCode())` → **恒为 true**，旧 `dictCode` 缓存永不失效 |
| **X8** | **切换状态机制不统一** | 仅 knowledge 有专用 `/toggleStatus` 端点 + 服务；dictType/dictItem 只能通过 `add`/`modify` 改状态；`SystemParamPo:32-81` **无 status 字段** |
| **X9** | **`ResultCode` 定义重复码 500** | `ResultCode.java:20 FAILURE(500,"操作失败")` 与 `:35 INTERNAL_ERROR(500,"服务器内部错误")` |

## §4 中危矛盾（双所有者／半实现）

| # | 矛盾 | 证据 |
|---|---|---|
| **X10** | **软删除两套，第二套是死代码** | `BaseLogicEntity.deleted` 列**在任何 DDL 中都不存在**，且无子类；实际只有 `FileRecordPo.delFlag`(`@TableLogic`) 在用 |
| **X11** | **分页返回形态两套，`PageResult` 是死代码** | 5 处分页全部直接返回 MyBatis-Plus `IPage`；`core/dto/PageResult.java` **零引用**，其 `hasNext`/`hasPrevious` 从未被任何端点输出 |
| **X12** | **删除语义三套** | 知识/字典/参数为**硬删**（`KnowledgeServiceImpl:125`、`DictTypeServiceImpl:125`、`DictItemServiceImpl:138`）；仅文件模块软删 |
| **X13** | **缓存所有权分散** | `SystemParamServiceImpl` 有 3 个刷新入口（`add/modify/remove` + `@PostConstruct` + 端点）；`DictItemServiceImpl` 只有本地 map、**无刷新入口**，且被 `DictTypeServiceImpl.findTypeWithItems:64-68` 绕过 |
| **X14** | **常量定义后被绕过** | `SystemConstant.EDITABLE_YES=1`(:20)、`EDITABLE_NO=0`(:23) **全仓零引用**；`SystemParamServiceImpl:115` 硬编码 `== 1`。且 `editable` 字段语义**不在任何文档中** |
| **X15** | **`findById` 空值策略不一致** | `DictTypeServiceImpl:51-54` 返回 200 + `data:null`；其余模块一律 `AssertUtils.notNull` 抛异常 |

## §5 低危矛盾／缺口

| # | 矛盾 | 证据 |
|---|---|---|
| **X16** | DB 与实体都有 `create_by`/`modify_by`，**任何 VO 都不返回** | `BaseEntity:53,60`，纯写不读 |
| **X17** | 标签颜色无处可达 | `KnowledgeTagPo.tagColor` 存在，但 `KnowledgeListVo.tags:45` 是 `List<String>` |
| **X18** | `use_count` 无聚合 | `KnowledgeTagPo.useCount` 存在，但无任何写入或读取路径 |
| **X19** | `file_path` 与 `url` 本地存储下同值 | `LocalFileStorage.getUrl:83-85` 直接返回 `storedPath` —— **✅ 已修复**：`url` 列已 `DROP`（**t5**，已落地）；`getUrl()` 方法链与 `FileVo.url` 的退役由 **t6** 执行（见 §10 D1/D2） |
| **X20** | 上传/删除文件**不清理磁盘** | `FileServiceImpl.remove:145` 只逻辑删记录 → 存储泄漏 —— **✅ 已修复（设计 + DDL 层）**：`del_flag` 列已 `DROP`、删除契约改为「引用计数 + 提交后删盘」（**t5** DDL / **t6+t7** 代码，见 §10 D5、§11 R2） |
| **X21** | 分页元数据精度保护副作用 | `JacksonConfig:50-61` 把**所有** `Long` 序列化为字符串，`IPage.total/current/size` 也变字符串 |
| **X22** | 知识分页 N+1 | `KnowledgeServiceImpl:151-163` 每条记录各查 1 次分类 + 2 次标签 |
| **X23** | `mapper-locations` 指向不存在的目录 | `application.yml:37` 声明 `dao/mapping/*.xml`，`src` 下**无该目录**（仅 1 个 `rebel.xml`） |

---

## §6 静态审查的盲区（未实测项）

以下结论**未经运行时验证**，需编译或测试才能升级为事实：

- **X6** 丢更新 — 并发行为，需压测或代码审查确认
- **X7** 缓存恒不生效 — 逻辑推演确定，但未实测
- **X12** 硬删 vs 软删 — 需确认表是否有 `del_flag` 列
- **X21** `IPage` 具体 JSON 键 — `mybatis-plus-core` jar 本地不存在，**证据不足**，契约 L138 只列 5 字段，是否还有 `orders`/`optimizeCountSql`/`searchCount` 未确认
- 未执行 `mvn compile`、未运行现有 4 个测试

---

## §7 文档自身与代码的矛盾

| 文档位置 | 问题 | 处置 |
|---|---|---|
| `knowledge-module/design.md` v1.1 L87-88 | 原写"分类树/标签并入 Knowledge 接口"——**代码无此并入** | ✅ 已于 v1.2 更正 |
| `frontend/design.md` §后端 API 现状 L21-25 | 遗漏 4 个**实际存在**的端点：`file/rename`、`dictItem/findMapByTypes`、`dictType/findTypeWithItems`、`systemParam/findByGroup` | 待补（登记于该文档 §待修订） |
| `frontend/design.md` L216 vs L511 | Vite 代理 `/api`，后端无任何路径以 `/api` 开头 | 已登记为 R06 |
| `api-contract.md` §1.1 L75 | 称接口清单见 §2.1.7，但该节仅 5 条，实际 24 条 | 待补 |

---

## §8 修复优先级建议

| 顺序 | 项 | 理由 | 授权要求 |
|---|---|---|---|
| **0（本轮）** | **X19 / X20** | 附件改造成本已覆盖：`url` 列删除（t5）+ 物理删除与提交后删盘（t6/t7）—— 见 §10 台账 | ✅ 已授权（附件改造范围） |
| 1 | **X2** | 直接决定前端能否正确查列表；且是契约内部自相矛盾 | 需用户选定方向（改契约 or 改 Jackson） |
| 2 | **X1a/X1b** | 前端阻塞项；补齐 category/tag/attachment/stats | 属新增功能，需用户确认范围 |
| 3 | **X4/X5/X10/X11/X12** | 五件"统一化"——**anti-entropy 正式范围** | ⚠️ **任何删除动作必须先获明确授权** |
| 4 | X6/X7/X9/X14 | 独立可修的真实缺陷，无破坏性 | 常规修复 |
| 5 | X20/X23 | 存储泄漏与配置缺陷 | 常规修复 |

---

## §9 证据基准

- **代码**：`src/main/java/io/aik/steins/grimoire/`（包名 `io.aik.steins.grimoire`）
- **DDL**：`sql/aik_knowledge_tables.sql`、`sql/aik_system_tables.sql`
- **配置**：`src/main/resources/application.yml`
- **审查范围**：5 个 Controller、`core/**`、`knowledge/**`、`system/**`、`components/**`
- **未覆盖**：运行期行为、`target/` 编译产物、前端（`grimoire-web/` 不存在）

**§10–§13（v1.1 新增章节）的采集基准**（2026-09-20 实测快照，随任务推进必然变化）：

| 基准 | 来源 / 命令 | 快照值 |
|---|---|---|
| 代码行号 | 直接读 `src/main/java/**`（本文档 §10 所有行号均为实测） | 例：`FileServiceImpl:65/77/98/144/145`、`FileVo:26/29/41/44`、`FileRecordPo:101-102/109-110` |
| 存储策略 | `core/storage/*.java` | `getUrl` 声明 `FileStorageStrategy:85-91`；三实现 `Local:83-85` / `Oss:97-101` / `Sftp:113-116`；`exists` 0 调用方 |
| 未跟踪产物 | `git status --porcelain \| grep '^??'` | `sql/attachment_two_layer_migration.sql`、`sql/backup/`、`sql/insert_eventbus_solution.sql`、两条 `grimoire-files/wsl/*.md`、`.agent-teams/` |
| 行尾噪音规模 | `git diff HEAD --name-only \| wc -l` + 逐文件 `--ignore-cr-at-eol` 判定 | **187 个变更文件中 173 个为纯行尾噪音**（真实内容改动 14 个） |
| R6 依据 | `application.yml:53`、`.gitignore` 全文、`git check-ignore -v grimoire-files/2026/09/20/x.pdf`（exit 1）、`ls grimoire-files/20*`（不存在） | 见 §11 R6 |
| R6 收窄依据 | `src/test/**`：`@ExtendWith(MockitoExtension.class)`、无 `src/test/resources`、`base-path`/`grimoire-files` 0 命中 | 见 §11 R6 适用范围 |
| DDL 现行结构 | 转述 `sql/` 与 SDD §2.3.4（本文档不重复采集库） | `aik_sys_file` 13 列 / `aik_sys_attachment` 12 列，两表 0 行 |

---

## §10 退役声明（Deletion Class 台账，v1.1 新增）

> **本节是本轮附件改造「删除什么 / 谁接手 / 凭什么敢删」的唯一权威台账。**
> 依据：`SDD.md` **v1.4** §2.5 / §2.5.8 / §2.3.4、`api-contract.md` **v1.2.1** §2.5、
> `frontend/design.md` v2.2，以及 **2026-09-20 实测**（代码行号、`sql/`、`git`、`.gitignore`）。
> 字段取自 anti-entropy 治理框架（`Deletion Class` / `Anti-Entropy Declaration`），
> **每一行只描述一个失效职责**——避免"同一载体上一个职责失效、另一个仍有效"时被整体误删。
>
> ⚠️ **本文档只登记，不执行**。任何删除动作（DDL / 列 / 磁盘文件）都必须先有**用户显式授权**；
> 表中"已落地"字样一律是**转述 t5 的实测结果**，不是本节发起的动作。

### 10.1 台账（D1–D11）

| # | Deletion Class | 旧路径（实测行号） | 失效职责 | 新权威所有者 | 预期保留 | 外部边界 | 真源风险 | 用户确认 |
|---|---|---|---|---|---|---|---|---|
| **D1** | contract-carrying code + persistent-state | `aik_sys_file.url`；`FileRecordPo:101-102`；`FileVo:44`；`FileServiceImpl:65`（getUrl）/`:77`（setUrl） | "对外访问地址"——local 模式下它与 `file_path` **同值**（＝ X19） | `FileController#download`（`AbstractFileStorage:137-155` 决定 `Content-Type` / `Content-Disposition`） | 上传 / 下载 / 预览 / 改名 / 分页**全部不变** | yes —— `FileVo` 字段集（前端契约） | none —— 迁移前两表 0 行（SDD §2.3.0） | ✅ **已获**（用户原话"url 去掉吧"+「url 全链路删除」）；DDL 由 **t5** 执行 |
| **D2** | code-retirement | `FileStorageStrategy:85-91`（接口声明）；三实现 `LocalFileStorage:83-85`、`OssFileStorage:97-101`、`SftpFileStorage:113-116` | "由存储层给出访问地址"——摘除 `FileServiceImpl:65` 后**零调用方** | **无** —— 职责**消失**（不是转移）；需直连时按 Reopen Trigger 新增 `presignedUrl`（**不得**复活持久化字段） | 其余 5 个接口方法（`upload` / `download`×3 / `remove` / `exists`）不变 | no —— `core.storage` 为内部包 | none | ✅ **已获**（船长裁定**甲**：确认发生在含 `getUrl` 与三实现的完整删除链之后；SDD v1.3 §2.5.6〔K1〕） |
| **D3** | persistent-state + contract-carrying（DDL 文件） | `aik_sys_attachment.attach_url` | "附件自持存储路径"——与 `aik_sys_file.file_path` 是**同一事实的第二个所有者**（真冗余） | `aik_sys_attachment.file_id` → `aik_sys_file.file_path` | `attach_name` / `description` / `sort_order` 全部保留 | yes —— `AttachmentVo.attachUrl` 已删 | none —— 0 行 | ✅ **已获** |
| **D4** | persistent-state | `aik_sys_attachment.knowledge_id`（+ `idx_knowledge_id`） | "把知识业务硬编码进 system 模块的表结构"——与"通用挂载表"目标直接冲突 | `biz_type` + `biz_id`（常量 `AttachmentBizType.KNOWLEDGE`；`VARCHAR(32)` 开放式取值） | 知识附件读写（**经统一挂载服务**） | yes —— `AttachmentVo.knowledgeId`；`findByKnowledgeId` → `findByBiz` | none —— 0 行 | ✅ **已获** |
| **D5** | persistent-state + code-retirement | `aik_sys_file.del_flag` + `idx_del_flag`；`FileRecordPo:109-110`（`@TableLogic`） | "文件记录的逻辑删除"——软删墓碑让"同一内容再上传"查不到行却又插不进（历史曾**误述**为 `uk_md5` 冲突） | **物理删除**：`FileService.remove` 唯一入口 + 删除守卫 + 引用计数 | 业务事实（谁在何时挂了什么）留在**挂载层**，不依赖文件表留档 | no —— `del_flag` 从未出现在任何 VO | none —— 0 行 | ✅ **已获** |
| **D6** | code-retirement | `FileServiceImpl:144` 注释「逻辑删除（@TableLogic 自动处理）」 | "记录逻辑删除语义"——与物理删除冲突的**文档型残留**（留着就是熵） | — | — | no | none | 否（纯注释） |
| **D7** | contract-carrying code | `FileVo:26 storedName`、`:29 filePath`、`:41 md5`（`url` 见 D1） | "对外暴露存储布局与内容指纹"——`FileVo.of()` 是 `BeanUtil.copyProperties` **全量拷贝**，字段在 VO 上即外泄 | **无**（暴露面收缩）；`storageType` **保留**为策略标签（不泄露路径、管理页要按它筛选） | 管理端台账视图（`originalName` / `fileSize` / `fileType` / `downloadCount`） | yes —— 前端契约 | none | 否（api-contract v1.2 §2.5.5 已定稿） |
| **D8** | code-retirement | `KnowledgeServiceImpl:52`（注入 `SysAttachmentMapper`）、`:120-122`（按 `knowledgeId` 删附件）、`:190-194`（selectList → VO） | "knowledge 模块自己拥有附件读写"——与挂载服务构成**双所有者**；且 `knowledge_id` 列已不存在（现状**编译期即断**） | 统一挂载服务（**t7**）；knowledge 侧只调它，且与知识写入**同一事务**（SDD §4.5 缺陷 A） | 详情页附件列表、删知识时的附件卸载 | no —— 内部调用 | none —— 0 行 | 否 |
| **D9** | contract-carrying code | `KnowledgeVo:33` `List<SysAttachmentPo>` | "把 PO 内部列（`biz_type` / `biz_id` / `del_flag`）直出给前端"，且响应结构随 PO 漂移 | `List<AttachmentVo>`（api-contract §2.5.3：6 字段，无 `bizType`/`bizId`） | 附件所需字段全部可表达 | yes —— `KnowledgeVo` 是前端主数据源 | none | 否（已在契约定稿） |
| **D10** | —（审计条目，见 §5） | **X19** / **X20** | 见 §5 两行（已就地标注「✅ 已修复」+ 修复任务） | — | — | — | — | 已含在附件改造授权内 |
| **D11** | code-retirement | `FileServiceImpl:98` 无条件以 `po.getOriginalName()` 作响应文件名 | "响应文件名 = 文件台账名"——md5 秒传后会把**首次上传者**的显示名给第二个挂载者（用户可见错误） | 挂载层 `attach_name`（带 `attachId` 时）；**保留** `original_name` 仅作台账模式回落 | 台账模式（管理端 `findPage` 下载、上传后立即预览）仍用 `original_name` | yes —— `Content-Disposition` | none | 否（api-contract §2.5.5 已裁决） |

**两处行号／分类校正（实测，供 t6 直接使用）**：

1. `getUrl` 三实现的**实际行区间** = `LocalFileStorage:83-85`、`OssFileStorage:97-101`、`SftpFileStorage:113-116`
   （任务描述写作 82-85 / 96-101 / 112-116，签名行各差 1）；接口声明 `FileStorageStrategy:85-91`。
2. 任务描述把 `AbstractFileStorage:162-167` 与 `:172-176` 并列为"**两个**不在接口内的 `download` 重载"——
   **实测不成立**：`:162-167` 是 `@Override download(response, storedPath)`，**正是**接口方法（`FileStorageStrategy:53`）；
   **只有 `:172-176`（`download(response, storedPath, preview)`）在接口之外**。
   两者（含 `:53` 那条）**都没有外部调用方**，故都登记为待退役候选（见 §10.3），但**分类不同**，不要混为一谈。

### 10.2 相邻但**不在**本轮删除清单的对象

- `BaseLogicEntity`（列名 `deleted`）：SDD §2.4.1 对它的裁定是**禁用**（新实体不得继承），
  **不是**删除类；其列在**任何 DDL 中都不存在**且零子类（X10）——X10 的处置优先级未变。
- `SystemConstant` / `StatusEnum` / `PageResult` 等 X4 / X11 对象：属"统一化"议题，**不因附件改造顺带删除**。

### 10.3 有意保留、非退役（含 Reopen Trigger）

| 对象 | 保留理由（实测） | Reopen Trigger |
|---|---|---|
| `FileStorageStrategy#exists(String)`（`:83`） | **不属 url 链路**；实测 0 调用方（全仓 `exists(` 仅 4 处：接口声明 + 三个实现；另有 `LocalFileStorage:64` 的 `java.io.File#exists`，与此无关） | 将来需要"文件存在性对账"时**优先复用**它而不是新写一套；若长期无调用方 → 转入待退役 |
| `AbstractFileStorage:172-176` `download(response, storedPath, preview)` | 不在接口内、0 调用方；删除它属**另一个**退役议题，本轮不动 | 同上；**本轮改造中不得新增对它的依赖** |
| `FileStorageStrategy:53`（接口方法 `download(response, storedPath)`）与 `AbstractFileStorage:163`（其唯一实现） | 同上（0 外部调用方；`AbstractFileStorage` 自己的 `:172/:181` 也不调用它） | 同上 |
| `FileVo.storageType` | 策略标签，不泄露路径；管理页按存储类型筛选需要 | 见 **R1**（`storage_type` 语义本身是死列） |
| `sql/insert_eventbus_solution.sql` | 与本轮无关的历史未跟踪产物（2026-09-16） | 由事件总线相关工作处置 |

### 10.4 Data Destruction Guard（DDL 侧 · 状态）

```text
Data Destruction Guard:
- Target Class:            persistent-state（表结构）+ live-state mutation surface（迁移脚本）
- Exact Target(s):         aik_sys_file.url            DROP COLUMN
                           aik_sys_file.del_flag        DROP COLUMN（idx_del_flag 随之消失）
                           aik_sys_attachment.attach_url      DROP COLUMN
                           aik_sys_attachment.knowledge_id    CHANGE COLUMN → biz_id
                           aik_sys_attachment            ADD file_id / biz_type / del_flag
                                                          + uk_biz_file / idx_biz / idx_file_id
- Environment:             开发库 aik_steins_grimoire（172.29.48.1:3306；WSL 内 127.0.0.1 连不上）
- Why Irreversible:        删列 / 改名后，库内不再保留原值
- Backup / Rollback Note:  迁移前两表 0 行（SDD §2.3.0）；回滚凭据 = sql/backup/attachment_pre_migration_2026-09-20.sql
                           （mysqldump --no-data）；回滚方式 = 按 SDD §2.3 的 CREATE TABLE 重建
- Allowed Read-Only Steps: 读代码 / 读 DDL / 写设计文档 / 跑 SDD §2.3.2 三条**只读**对账 SQL
- Blocked Destructive:     再次执行任何 ALTER/DROP（重放需先过 SDD §3.0 前置硬断言）；
                           删除磁盘文件；DROP TABLE；改动 application.yml（见 R6）
- Confirmation Required:   yes —— **已获并已消费**（2026-09-20，用户在完整 url 删除链语境下确认）
- Status:                  ✅ 已执行（t5，2026-09-20）：真库 = SDD §2.3.4（aik_sys_file 13 列 / aik_sys_attachment 12 列），两表仍 0 行
```

### 10.5 Retirement Decision（选择与拒绝项）

```text
Retirement Decision:
- Path:    delete-first（D1–D9、D11；D3–D5 的持久化部分已由用户显式授权后执行）
- Why:     每一项的新权威所有者都已存在并被文档化（D2 除外——其职责是**消失**而非转移）；
           不存在"外部未知消费者"：前端尚未开工（grimoire-web/ 不存在，见 §9），core.storage 为内部包，
           且实测 `git grep` 层面无仓外调用面。故不需要 compat-exception。
- Non-edits（有意不删）: BaseLogicEntity（仅禁用）、exists()、AbstractFileStorage:172-176 重载、
           storageType、sql/insert_eventbus_solution.sql、application.yml（见 R6）；
           且**不得**借本轮删除 X4 / X10 / X11 等其他统一化对象（避免范围蔓延）
```

### 10.6 Verification Plan（判据，由 t9 执行）

```text
Verification Plan:
- Main-path check:        上传 → 挂载 → 详情 → 预览 → 下载 五步用真端点跑通（api-contract §4 Phase 4 ①~⑩）
- Lingering-reference:    ① grep 全仓 `getUrl` / `.getUrl(` 应为 0 命中；
                          ② `@TableLogic` 在 file 包应为 0 命中；
                          ③ 两表 `SHOW CREATE TABLE` 列集合/索引 = SDD §2.3.4（url / attach_url / knowledge_id / del_flag 全不在）
- Negative check:         ① 上传出参**不得**含 url / filePath / storedName / md5（`grep -Eo '"(url|filePath|storedName|md5)"'` 无输出）；
                          ② 物理删除后 `aik_sys_file` **行真的消失**（不是 del_flag = 1）；
                          ③ 不带 attachId 下载 → 文件名回落 original_name（台账模式仍可用）
- Boundary check:         前端契约（FileVo / AttachmentVo / KnowledgeVo.attachments）与 api-contract 一致；
                          管理端文件台账的下载入口未被误删
```

---

## §11 风险登记（SDD §6.4 全量补记 + R6 新增）

> 依据 `SDD.md` v1.4 §6.4。R1–R5 与 §6.4 **逐条对应**（R1 的"⚠️ 需另派任务补记进 `backend-audit.md`"即本节），
> **R6 为本轮新增登记**（船长指派）。本节只登记，**不含任何修复动作**。

| # | 风险 | 实测／依据 | 缓解措施 | 处置 | Reopen Trigger |
|---|---|---|---|---|---|
| **R1** | `storage_type` 是**只写不读的死列**：策略 bean 由启动时的 `grimoire.file.use` **单一注入**；该配置从 `local` 改为 `oss`/`sftp` 后，历史 `file_path` 在新策略下**全部失效**（local 相对路径 / OSS objectKey / SFTP 远端绝对路径，三者互不兼容）→ 历史文件无法下载 | `FileServiceImpl:75`（`po.setStorageType(...)`）写入；全仓**无读取点**；`FileStorageConfig` 按 `use` 注入策略 bean | 无（本轮不修） | **已存在、本次不修**（超出附件改造边界）：要么将来做策略路由，要么登记为待退役列 | 首次出现"切换存储策略"或"多策略共存"需求时 |
| **R2** | 事务提交后**删盘失败** → 孤儿磁盘文件 | SDD §2.5.7 / §4.3；`FileStorageStrategy#remove` 已存在但原实现从未调用 | 记录日志 + 只读对账 SQL（SDD §2.3.2 查询③）；**顺序不可反**：先删盘再回滚会产生**不可恢复**的悬空引用 | 设计已覆盖（t6/t7 实现） | 出现查询③ 非空时（＝ 有删盘失败） |
| **R3** | 并发秒传可落**两行同 md5** | 甲方案 = 应用层约定、`idx_md5` **非唯一**（SDD §2.3.4） | 两行内容**完全相同**，无数据损坏；SDD §2.3.2 查询① 检出 | 接受（有意设计） | 出现存储成本或统计口径问题时 |
| **R4** | 批量卸载与并发挂载交叉 → InnoDB 死锁面 | SDD §2.5.7 并发与加锁顺序 | 统一锁序：**先锁文件行**、批量按 `file_id` **升序**；残余情形按**业务重试** | 设计已覆盖（t7/t8 实现） | 压测出现死锁回滚率超预期时 |
| **R5** | 「卸载最后引用」与「新业务挂载同一文件」并发 → **悬空挂载**（指向已物理删除的文件，下载 404） | SDD §2.5.7 写入第 1 条 / §4.5 步骤② / §4.3 (B) | **已闭合（非登记项）**：写入与卸载路径的**第一条语句都是 `SELECT ... FOR UPDATE`** 锁文件行——T2 阻塞在 T1 的行锁上，T1 提交后 T2 查不到文件行 → 抛「文件不存在」 | ✅ 已闭合 | **若实现方漏掉加锁读，该竞态立即复活** → 由 SDD §2.3.2 **查询②**（悬空挂载）检出 |
| **R6** | **`grimoire.file.base-path` 撞文档树**：`application.yml:53` 为 `grimoire.file.base-path: ./grimoire-files`（**相对路径**），且 yml 中**无** `method.local.base-path`，故 `LocalFileStorage:90-101` 回落到该值。以仓库根为 CWD **启动应用**时，上传落点 = `<repo>/grimoire-files/yyyy/MM/dd/…`，而 `grimoire-files/` **正是本文档树根**（README、`design-docs/`、`plans/` 同层）→ 上传产物与文档目录混居 | ① `.gitignore` **无任何相关规则**（实测无 `grimoire*` / `storage` / `tmp` / `upload` 条目；`.gitignore` 本身与 HEAD 只差行尾）；② `git check-ignore -v grimoire-files/2026/09/20/x.pdf` → **exit 1，不被忽略**；③ `grimoire-files/20*` 与 `grimoire-files/tmp` **当前均不存在** → 尚无上传发生；④ 旁证（**实测**）：`LocalFileStorage:100` 的**最后兜底字面量**是 `./grimoire-files/storage`（带后缀 = 安全），但先命中的是 `FileStorageConfig:52` 的字段默认值 `./grimoire-files`（**与 yml 同值**）→ 兜底**永不生效**。两个默认值不一致，看着是**配置遗漏**而非有意设计 | 建议（三选一，均需单独授权）：① 把 `application.yml:53` **与** `FileStorageConfig:52` 一起改为带后缀的 `./grimoire-files/storage`；② `.gitignore` 加 `grimoire-files/20*/`；③ 执行契约 §8 的 **C4**（上传目录迁出） | **已存在、本次不修**（改 `application.yml` 属配置变更，需用户单独授权） | 首次真跑上传 / E2E / 接入 CI 时 |

**R6 的适用范围（重要，防止误读）**：R6 **只适用于「启动应用 / E2E 真跑」**，**不适用于单元测试**。
实测依据：`FileServiceImplTest` 与 `SystemParamServiceImplTest` 都是 `@ExtendWith(MockitoExtension.class)`
（无 Spring 上下文）、`FileStorageStrategy` 是 `@Mock`（**存储层不落盘**）、`src/test/resources` **不存在**、
`src/test/` 树中 `grimoire-files` / `base-path` / `basePath` **0 命中** ⇒ 含 `rm -rf target && mvn test` 也**不会**
往 `grimoire-files/` 写入任何东西。（该收窄由 backend-design 提供证据、船长复核。）

**另记（同源发现）**：`FileStorageConfig:32` 的 `localTmp = "./grimoire-files/tmp"` 实测 **0 处引用** → 死配置。

**SDD §6.4 中未编号的三行**（非附件域专属，一并转述以免丢项）：系统参数缓存与库不一致（写后刷新 + 手动刷新入口）；
**文件删除时磁盘已不存在 → `remove` 必须幂等**（不因缺文件而让删除失败，直接支撑 R2）；
大文件上传（10MB 单文件 / 50MB 请求体限制）。

---

## §12 改动边界声明（本轮）

**原则**：本轮只改"附件/文件域"的事实所有者与其下游指针。**任何未列入白名单的改动都是越界**，
发现越界一律回报船长，不回滚自行处置。

### 12.1 白名单（允许改动的文件）

| 区域 | 文件 | 任务 |
|---|---|---|
| DDL | `sql/aik_system_tables.sql`、`sql/aik_knowledge_tables.sql` | t5 ✅ |
| DDL 新增 | `sql/attachment_two_layer_migration.sql`（迁移脚本）、`sql/backup/*`（回滚凭据 + 3 次重放日志 + 全新安装对照） | t5 ✅ |
| 存储策略 | `core/storage/{FileStorageStrategy,AbstractFileStorage,LocalFileStorage,OssFileStorage,SftpFileStorage}.java` | t6 |
| 文件模块 | `system/file/{controller/FileController,service/FileService,service/impl/FileServiceImpl,po/FileRecordPo,vo/FileVo,dto/**,dao/FileMapper}.java` | t6 |
| 附件模块 | `system/attachment/**`（**现状仅 `dao/SysAttachmentMapper` 与 `po/SysAttachmentPo`**，`controller`/`service`/`vo`/`dto` 为新建）；`biz_type` 取值常量（`AttachmentBizType`，**全仓尚未存在**） | t7 |
| knowledge 接入 | `knowledge/common/vo/{KnowledgeVo,KnowledgeListVo}.java`、`knowledge/common/dto/KnowledgeDto.java`、`knowledge/service/KnowledgeService.java`、`knowledge/service/impl/KnowledgeServiceImpl.java` | t8 |
| 测试 | `src/test/java/io/aik/steins/grimoire/system/service/impl/FileServiceImplTest.java` + 附件/挂载相关新增测试 | t6/t7 |
| 文档 | `design-docs/system-module/{PRD,SDD,api-contract,backend-audit}.md`、`design-docs/frontend/design.md`、`design-docs/knowledge-module/design.md`、`grimoire-files/README.md`、`grimoire-files/plans/execution-plan.md`（新建） | t1–t4 / t10–t15 |
| 决策日志 | `grimoire-agent/agent/decision-log.md`（**只追加**，不重排既有条目） | 全员 |

### 12.2 明确不动（out of scope）

`grimoire-agent/{agent-zero,plan,evolution-mechanism-redesign}`、`aik-skills-lab/**`、`canvases/**`、
`grimoire-files/{component-manuals,learning-notes,wsl}/**`、`grimoire-scrolls/**`、`LICENSE`、`pom.xml`、
`src/main/resources/{application.yml,banner.txt,rebel.xml}`、
`src/main/java/**/{system/dict,system/param,components}/**`、`sql/insert_eventbus_solution.sql`、
`.gitignore`（**如需为 R6 加规则 → 单独授权**）、`.agent-teams/**`（团队运行状态，**不得提交**）。

> `grimoire-files/plans/project-architecture.md`：它是**文档整合阶段**的 superseded 标注产物，
> 随"文档基线"一并冻结（§13 第 1 步），**不是**本轮附件改造新增的设计改动。

### 12.3 三条边界声明（可直接用于提交前自检）

1. **工作区里的 173 个纯 CRLF 行尾噪音文件不属本次改动。**
   实测（`2026-09-20`）：`git diff HEAD --name-only` 共 **187** 个文件，其中 **173** 个在
   `--ignore-cr-at-eol` 下**零差异** → 其差异 100% 是行尾，由环境（`core.autocrlf` 未设置）造成，
   **不是任何人写的内容**，**不得**随本次提交。**真实内容改动只有 14 个文件**（快照值，随任务推进变化）。
2. **`grimoire-files/20*` 与 `grimoire-files/tmp` 不得出现**（R6）。
   若出现，说明有人按默认 `base-path` **真跑过应用**：立即停止提交、按 R6 处置，并检查是否已被误提交。
3. **仓库根不得出现字面名 `D:\service\maven\repository` 的目录。**
   它曾因 Maven `settings.xml` 写 Windows 路径、在 WSL 下被当成合法相对路径而创建（船长已清理）。
   本次实测**不存在**（根目录另有一个与本轮无关的历史文件 `nacos-wsl.service`，不动）。
   若复现 → 属同类环境陷阱：删除该目录并在提交前确认未混入。

---

## §13 提交策略

**目标**：让"本次改造的真实 diff"在 `git log` 里可读。当前工作区被行尾噪音淹没（§12.3 第 1 条），
**任何 `git add -a` / `-A` / `git commit -a` 都会把 173 个噪音文件一起冻结**，把真实改动埋掉。

**规则溯源**（`grimoire-agent/agent/decision-log.md`，同一规则已记录两次）：

| 日期 | 记录 | 状态 |
|---|---|---|
| 2026-07-31 | 「AikSteinsGrimoire 项目：文件存入后必须 `git add` 暂存保护」 | confirmed |
| 2026-08-26 | 「修改后自动 `git add` 暂存（执行遗漏被纠正，规则不变）」 | corrected |

→ 规则的本意是**不丢改动**。**显式 pathspec 的 `git add` 同样满足该规则**，且不引入噪音；
因此本轮的合规形态是"逐个列文件名"，不是"全量 `-a`"。

**策略（由船长执行；本文件只定义步骤）**：

```bash
# 第 0 步 · 预检（只读，先确认噪音规模与未跟踪物）
git diff HEAD --name-only | wc -l                      # 187（快照）
git status --porcelain | grep '^??'                    # 未跟踪：.agent-teams/、sql/attachment_two_layer_migration.sql、
                                                       #   sql/backup/、sql/insert_eventbus_solution.sql、两条 wsl/*.md

# 第 1 步 · 单独冻结【文档基线】一次（显式路径，绝不加 -a）
git add grimoire-files/README.md \
        grimoire-files/design-docs/system-module/PRD.md \
        grimoire-files/design-docs/system-module/SDD.md \
        grimoire-files/design-docs/system-module/api-contract.md \
        grimoire-files/design-docs/system-module/backend-audit.md \
        grimoire-files/design-docs/frontend/design.md \
        grimoire-files/design-docs/knowledge-module/design.md \
        grimoire-files/plans/project-architecture.md \
        sql/aik_system_tables.sql sql/aik_knowledge_tables.sql \
        sql/attachment_two_layer_migration.sql sql/backup
git diff --cached --stat --ignore-cr-at-eol             # 复核：暂存内容里不得出现纯行尾文件
git commit -m "docs(attachment): 附件两层模型定稿（SDD v1.4 / 契约 v1.2.1 / 前端 v2.2）+ DDL 落地与重放证据"

# 第 2 步 · 代码阶段：每个任务一次提交，各自显式 pathspec
#   t6 文件模块   → src/main/java/io/aik/steins/grimoire/core/storage/
#                   src/main/java/io/aik/steins/grimoire/system/file/
#                   src/test/java/io/aik/steins/grimoire/system/service/impl/FileServiceImplTest.java
#   t7 附件模块   → src/main/java/io/aik/steins/grimoire/system/attachment/  （+ AttachmentBizType 常量）
#   t8 knowledge  → src/main/java/io/aik/steins/grimoire/knowledge/
#   t9 验证       → 不放代码；对账输出追加进 sql/backup/ 或由船长指定的证据文件

# 第 3 步 · 收尾复核
git log --oneline -5
git show --stat HEAD                                    # 每次提交都应"只有真实改动"
```

**必须遵守的四条**：

1. **先冻结文档基线**（第 1 步）再进代码阶段——否则代码 diff 会与文档改动混在同一次提交里，无法单独回滚。
2. **`sql/` 下 t5 的新产物尚未提交**（实测 `?? sql/attachment_two_layer_migration.sql`、`?? sql/backup/`）：
   迁移脚本、回滚凭据（`attachment_pre_migration_2026-09-20.sql`）、3 次重放日志、全新安装对照
   **一并纳入第 1 步**——它们是 DDL 已落地的唯一证据，且回滚凭据**不能晚于**任何后续 `ALTER`。
3. **不得提交**：`.agent-teams/**`（团队运行状态）、`grimoire-files/wsl/wsl2-*.md`（与本轮无关的未跟踪笔记）、
   `sql/insert_eventbus_solution.sql`、`grimoire-agent/plan/agent-expansion-plan.md`、
   `grimoire-files/plans/agent-expansion-plan.md`。
4. 两个"-v1.0"文件（`PRD-v1.0.md` / `SDD-v1.0.md`）的**重命名**（`R`）随第 1 步固化，不要单独提交重命名。

**回滚路径**：文档/代码层按提交粒度 `git revert`；**DDL 层不可 `git revert`**——回滚须用
`sql/backup/attachment_pre_migration_2026-09-20.sql`，前提仍是 SDD §3.0 的两表 0 行断言。

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-09-20 | 初版。8 域盘点 + 23 项矛盾 + 4 项文档矛盾 + 盲区声明 |
| v1.1 | 2026-09-20 | **新增 §10 退役声明（D1–D11 Deletion Class 台账 + Data Destruction Guard + Retirement Decision + Verification Plan）、§11 风险登记（SDD §6.4 的 R1–R5 全量补记 + 新增 R6 `base-path` 撞文档树，含"仅适用于启动/E2E、不适用于单元测试"的适用范围收窄）、§12 改动边界声明（白名单 / 不动清单 / 三条自检声明，含实测 187 文件里 173 个为纯行尾噪音）、§13 提交策略（显式 pathspec 分阶段提交 + `decision-log` 两次规则溯源 + `sql/` 未提交产物的纳入）**；§5 的 **X19 / X20 就地标注「✅ 已修复」并指明修复任务**；§9 补本节新增章节的采集基准。**未改代码、未改库、未执行任何 DDL、未提交任何内容** |
