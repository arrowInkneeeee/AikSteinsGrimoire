# 附件域改造 · 实现计划（仓库级）

> 版本：v1.0 | 更新：2026-09-20 | 状态：生效中 | 权威范围：本次附件域改造的**仓库级执行顺序、门禁、证据形态、提交时点与回滚决策点**
> v1.0 初版：依据已定稿的 `SDD.md` v1.4 / `api-contract.md` v1.2.1 / `frontend/design.md` v2.2、已落地的 `sql/`（t5）与 `backend-audit.md` v1.1，冻结执行顺序与门禁。
>
> **文件名不含版本号**（README §2 约定）；历史正文追加在文末 §变更历史。

---

## 0. 权威边界（先读，防止制造第二个所有者）

**本文件不定义接口形状，也不复制验收命令。** 事实归属如下，冲突时以"唯一权威"列为准：

| 事实 | 唯一权威 | 本文件的关系 |
|---|---|---|
| 接口路径 / 方法 / 入参 / 出参 / 错误模型 | `design-docs/system-module/api-contract.md` | **只指针引用** |
| **接口级**分阶段验收命令（curl / SQL 对账） | `api-contract.md` **§4**（Phase 0–6）+ §4.1 结构断言 | **只指针引用，不搬运** |
| 表结构、迁移 DDL、对账 SQL、迁移后实测状态 | `SDD.md` §2.3 / §2.3.1 / §2.3.2 / §2.3.4 | 指针 |
| 退役台账 / 风险登记 / 改动边界 / **提交策略（含命令与纳入文件清单）** / 回滚命令 | `backend-audit.md` **§10–§13** | 指针（本文件只定"提交**时点**"与"回滚**决策点**"） |
| Knowledge 模块的包结构与表结构 | `design-docs/knowledge-module/design.md` | 指针 |
| 前端页面 / 路由 / 组件 / 附件交互 | `design-docs/frontend/design.md` | 指针 |
| 文档版本与状态登记 | `grimoire-files/README.md` §3 / §4 | 收口时刷新（t16） |
| **执行顺序 / 门禁 / 证据形态 / 提交时点 / 回滚决策点** | **本文件** | — |

**一句话分界**：`api-contract.md §4` 回答"**跑什么命令才算过**"，本文件回答"**什么时候允许跑、过了才能进哪一步、证据留在哪里**"。

---

## 1. 范围与基线

### 1.1 四项已拍板决策（用户裁定，不再重议）

| # | 决策 | 落点 |
|---|---|---|
| **D1** | `url` **全链路退役**：`aik_sys_file.url` 列 + `FileRecordPo.url` + `FileVo.url` + `getUrl()` 接口方法与三个实现 + `FileServiceImpl` 的 `getUrl` / `setUrl` 调用；访问统一走 `/grimoire/file/download`（`preview` 区分内联 / 附件） | SDD §2.5.6〔K1〕、api-contract §2.5.5、backend-audit §10 D1/D2 |
| **D2** | `aik_sys_attachment` 改造为**通用挂载表**（`file_id` / `biz_type` / `biz_id` / `attach_name` / `description` / `sort_order` / `del_flag` + 审计 4 列），删除 `attach_url` / `knowledge_id` | SDD §2.2 / §2.3、api-contract §2.5 |
| **D3** | **md5 秒传 = 甲方案**：查重命中**跳过写盘**直接复用同一文件对象；`md5` **不加任何唯一约束**（应用层约定，非 DB 约束）；`aik_sys_file` 改**物理删除** | SDD §2.5.3 / §2.5.4、backend-audit §11 R3 |
| **D4** | 删除走「删挂载行 → 查是否仅剩此挂载 → 无则删文件记录 + **提交后**删盘」，含并发补偿；**下载文件名取自挂载层** `attach_name` | SDD §2.5.7 / §4.3 / §4.5、api-contract §2.5.5 |

### 1.2 基线（2026-09-20 实测）

| 项 | 值 |
|---|---|
| 基线 commit | `a95f1d7`（`feat(eventbus): 添加事件驱动监听器架构组件`） |
| DDL 状态 | 迁移**已落地**（t5）：真库 `aik_sys_file` **13 列** / `aik_sys_attachment` **12 列**，**两表 0 行**（本轮只读复核） |
| 数据库 | 开发库 `aik_steins_grimoire` @ `172.29.48.1:3306`；WSL 内 `127.0.0.1:3306` **不通**（`ERROR 2003`） |
| 代码基线 | 附件域**尚未改造**：`getUrl` 链、`FileVo.url/filePath/storedName/md5`、`KnowledgeServiceImpl` 三处跨模块直连**均仍在**（见 backend-audit §10） |
| 工作区状态 | 未提交。**187** 个文件显示为变更，其中 **173 个是纯 CRLF 行尾噪音**（判定方法见 §6.2 第 1 条），真实内容改动 **14** 个 —— ⚠️ **快照值**，提交前必须重测 |
| 未提交产物 | `sql/attachment_two_layer_migration.sql`、`sql/backup/`（回滚凭据 + 3 次重放日志 + 迁移≡全新安装对照）**尚未提交**，冻结文档基线时必须**一并纳入** |

---

## 2. 执行顺序与依赖（实际任务图）

```
[已完成 · 设计冻结]
  t1  SDD 两层模型定稿 ─┐
  t11 SDD 迁移后同步     ├→ SDD v1.4
  t13 SDD 接口口径同步  ─┘
  t2  api-contract 附件契约            → v1.2.1
  t3  frontend 附件交互                → v2.2
  t4  backend-audit 退役/边界/提交策略 → v1.1
  t5  DDL 落地（真库 + sql/）          → 两表 0 行，结构 = SDD §2.3.4
  t10 下游文档同步（knowledge-module §3.2 + README §4/§3 一行）
  t12 本文件（仓库级执行计划）

[待执行 · 契约收尾 + 代码阶段]
  t14 AttachmentVo 6 → 8 字段（契约 + 前端）   ← gating t15 与 t7
   └→ t15 SDD 跟随同步（§4.5 / §2.5.3 口径）
  t6  文件模块改造（秒传 / url 全链路退役 / 物理删除）   deps: t2, t4, t12
  t7  通用挂载 Service + Mapper                        deps: t2, t4, t5, t12, t14
   └→ t8 knowledge 接入改造（消除跨模块直连）            deps: t2, t7

[收口]
  t9  独立质量验证（运行时行为 + 契约一致性对账）
  t16 README 终版收口（§4 版本表 + §7.4 交叉指针）
```

**关键路径**：`t14 → t7 → t8 → t9`；两条并行支线：`t14 → t15`、`t12 → t6`。

### 2.1 每步的前置条件（进入时校验；不满足就停下回报，不要硬跑）

| 步骤 | 必须已满足 |
|---|---|
| t14 | api-contract §2.5.3 与 frontend 附件交互**已定稿**（t2/t3 完成）——改的是**已定稿内容**，不是新设计 |
| t15 | t14 已把 `AttachmentVo` 的 8 字段写进契约（**先读回确认**再改 SDD） |
| t6 | **t12 完成**（本文件）+ t2 / t4 完成；DDL 已是迁移后状态（G1） |
| t7 | **t12 + t14 完成** + t5 完成；`uk_biz_file` 已存在（差量 / 复活实现的前提） |
| t8 | t7 完成（统一挂载服务可用）；`SysAttachmentPo` 已是新结构 |
| t9 | t5 / t6 / t7 / t8 / t10 / t12 / t13 / t14 / t15 **全部完成**——否则验的是半成品 |
| t16 | t12 / t14 / t15 完成 |

---

## 3. 门禁（gate）

> 每道门禁的**命令本体**在 §0 表格所列的权威文档里；本节的职责是**判定点 + 证据形态 + 不过怎么办**。

### G0 · 前置（设计冻结）
- **证据**：SDD / api-contract / frontend / backend-audit / knowledge-module design 五份文档头的版本，与 `README.md` §4 **逐行一致**。
- **不过则**：先做文档同步，**禁止**开代码。

### G1 · DDL（t5 已通过；**重放时必查**）
- **证据**：① 两表 **0 行**硬断言通过；② 结构备份文件存在（`sql/backup/attachment_pre_migration_2026-09-20.sql`）；
  ③ `SHOW CREATE TABLE` 与 SDD §2.3.4 逐字段一致（13 列 / 12 列 + `uk_biz_file` / `idx_biz` / `idx_file_id`，`idx_knowledge_id` 已消失）；
  ④ 三次重放日志齐全（幂等性）。
- **不过则**：**中止**——DDL 未就绪时改代码，等于对着不存在的列写 SQL。

### G2 · 编译与单测
- **证据**：构建/测试命令**成功退出**（`BUILD SUCCESS`）+ 测试用例数不低于基线。
- ✅ **本环境该门禁已实测可用**：命令与结果见 §7.1 的「构建与单测」行（`Tests run: 48` / `BUILD SUCCESS`）。
- 若某次运行失败：**不得**把"没跑成"写成"通过"，按 §7.3 的**已知环境坑位**逐条排查
  （`mvn clean` 不可用 / 必须显式传 `-Dmaven.repo.local` / **不要**加 `-o`），并在 t9 结论里登记未通过的原因。

### G3 · 秒传（同内容二次上传）
- **证据形态**：第二次上传返回的 **`fileId` 与第一次相同**；落盘目录文件计数**前后相等**（不新增文件）；
  `aik_sys_file` 行数**不增加**；日志无 md5 冲突异常。
- **不过则**：不进入 G4（秒传是删除路径正确性的前提——**只有复用才需要引用计数**）。

### G4 · 挂载读写（差量 + 复活 + 守卫）
- **证据形态**：同一个 `save` 请求体**原样提交两次**均成功、挂载行数与 id 不变；卸载后重挂同一文件成功（**复活**，`del_flag` 回到 0）；
  `attachName` 缺省被拒；不存在的 `fileId` 被拒；`bizType` 白名单外被拒；`findByBiz` **不返回**已卸载行且按 `sort_order` 升序。

### G5 · 删除路径（引用计数 + 提交后删盘）
- **证据形态**：① 仍有有效挂载时 `/file/remove` **失败**（删除守卫）；
  ② 同一 `fileId` 挂两条业务、卸载其一后**文件记录仍在**且另一条仍能下载（共享不误删）；
  ③ 卸载**唯一**挂载后 `aik_sys_file` **行消失**（不是 `del_flag = 1`）**且**磁盘文件消失；
  ④ SDD §2.3.2 三条对账 SQL 全为空（同 md5 多行 / 悬空挂载 / 无引用文件）。
- **不过则**：t9 判 **fail**，禁止进入收口。

### G6 · 文档零漂移
- **证据形态**：`git diff` 的文档集合落在 backend-audit §12.1 的白名单内；`README §4` 版本与各文档头一致；
  全仓 grep `getUrl` / `.getUrl(` / file 包的 `@TableLogic` / `attach_url` / 附件语义的 `knowledge_id`，**命中只允许出现在"已删除 / 已废弃"语境**；
  `FileVo` 序列化结果 **不含** `url` / `filePath` / `storedName` / `md5`。
- **不过则**：文档层回归，不进 t16。

---

## 4. 验收判据（对应目标的四个验收点）

| # | 目标验收点 | 判定 | 证据形态 |
|---|---|---|---|
| **A1** | **四份权威文档同步一致** | PRD / SDD / api-contract / frontend 对附件域的表述无冲突；`README §3` 权威边界表指向正确 | 五份文档头版本 + `README §4` 的一致性输出（G0 / G6） |
| **A2** | **代码与文档无漂移** | 代码中的接口与字段符合 api-contract §2.5；表结构符合 SDD §2.3.4 | G6 的 grep 结果 + 结构断言输出 |
| **A3** | **上传同内容二次不再崩且不产生孤儿文件** | 二次上传返回**同一** `fileId`；磁盘文件计数不变；`aik_sys_file` 不增行 | G3 的三项输出（接口响应 / 文件计数 / 行数 SQL） |
| **A4** | **删除路径不留墓碑** | 最后一份引用消失时**物理删除**行 + 删盘；文件表无 `del_flag = 1` 残行；无悬空挂载 | G5 ④ 的三条对账 SQL（应为空）+ `SHOW CREATE TABLE` 确认文件表**无** `del_flag` |

> **A3 的措辞说明**：基线上"第二次上传同内容"的**准确**现状表述见 SDD §2.5.4 的作废条款（"重复上传必崩并留孤儿文件"曾是**错误论断**，已作废）。本判据只约束**结果形态**（同一 `fileId` + 不写盘 + 不增行），**不对基线行为另作断言**。

---

## 5. 证据位置索引

| 证据 | 位置 |
|---|---|
| 回滚凭据（迁移前结构备份） | `sql/backup/attachment_pre_migration_2026-09-20.sql`（`mysqldump --no-data`） |
| 幂等性证据（3 次重放日志） | `sql/backup/migration_run{1,2,3}_2026-09-20.log` |
| 迁移路径 ≡ 全新安装路径（逐字段等价） | `sql/backup/migration_vs_fresh_install_2026-09-20.txt`（diff **为空**） |
| 迁移后实测结构（现行基准） | `SDD.md` §2.3.4（13 列 / 12 列 / 索引清单 / 两表 0 行） |
| 可执行迁移脚本（重放权威） | `sql/attachment_two_layer_migration.sql`（幂等；重放前必过 SDD §3.0 断言） |
| 退役台账 / 风险 R1–R6 / 改动边界 / 提交策略 | `backend-audit.md` §10 / §11 / §12 / §13 |
| 接口级验收命令 | `api-contract.md` §4（Phase 0–6）+ §4.1 |
| 对账 SQL（三条） | `SDD.md` §2.3.2 |

---

## 6. 提交时点（提交**策略内容**见 backend-audit §13）

### 6.1 提交时点表（本文件拥有的部分）

| 时点 | 提交内容 | 前置门禁 |
|---|---|---|
| **T-A** | **文档基线冻结**一次：五份设计文档 + `sql/` 的 DDL 与 `sql/backup/` 证据 | G0 通过后、**任何代码改动之前** |
| T-B | t14 + t15 的契约 / SDD 字段同步 | G0 重跑通过 |
| T-C | t6 文件模块 | G2 + G3 通过 |
| T-D | t7 + t8 挂载模块与 knowledge 接入 | G4 通过 |
| T-E | 收口（t9 结论 + t16 README 终版） | G5 + G6 通过 |

**硬约束**：`git commit` **不加 `-a`**，不用 `git add -A`，只能**显式列 pathspec** —— 否则会把**大量纯 CRLF 行尾噪音文件**一并冻结、把真实 diff 淹没。
**命令与纳入文件清单的内容**见 `backend-audit.md` §13（唯一权威）。

**规则溯源**：`grimoire-agent/agent/decision-log.md` 中同一规则已记录**两次**——
2026-07-31「AikSteinsGrimoire 项目：文件存入后必须 `git add` 暂存保护」(confirmed)、
2026-08-26「修改后自动 `git add` 暂存（执行遗漏被纠正，规则不变）」(corrected)。
→ 规则本意是**不丢改动**；**显式 pathspec 的 `git add` 同样满足该规则**且不引入噪音。

### 6.2 提交前自检（时点级）

1. `git diff --cached --stat --ignore-cr-at-eol` 中**不得**出现纯行尾文件（这是"173 个噪音 / 14 个真实改动"的判定方法）。
2. `grimoire-files/20*` 与 `grimoire-files/tmp` **不得存在**（风险 R6）；若出现，说明有人按默认 base-path **真跑过应用**。
3. 仓库根**不得**出现字面名 `D:\service\maven\repository` 的目录（Maven `localRepository` 为 Windows 路径时的环境陷阱，见 §7.3）。

---

## 7. 已知限制（**未验证的结论不写成事实**）

### 7.1 已实测（事实）

| 项 | 实测值 |
|---|---|
| 数据库连通 | 仅 `mysql -h172.29.48.1 -uroot -proot aik_steins_grimoire` 可用；`127.0.0.1:3306` **不通**（`ERROR 2003`）；两表均 **0 行**（本轮**只读**复核） |
| **构建与单测** | **✅ 已通过**：`rm -rf target && mvn -Dmaven.repo.local=/mnt/d/service/maven/repository test` → `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`；**BUILD SUCCESS**；`Total time: 03:52 min`（AssertUtilsTest 15 / JsonUtilsTest 8 / FileServiceImplTest 11 / SystemParamServiceImplTest 14） |
| **代码改造落盘状态** | **✅ 已改造**（2026-09-20 收口时实测）：`getUrl` 全仓 **0 命中**；`FileVo` 收缩为 **7 字段**（无 `url` / `filePath` / `storedName` / `md5`）；`system/attachment/{controller,service,impl,vo,dto,constant}` 已建；`AttachmentVo` 为 **8 字段**；`AttachmentServiceImpl` 已实现**差量与复活**、卸载后按引用计数调 `fileService.remove`；`FileServiceImpl` 已调用 `fileStorageStrategy.remove(...)`（**提交后删盘**） |
| `~/.m2` | **不存在**（构建依赖显式 `-Dmaven.repo.local` 指向下条目录） |
| Maven / JDK | Maven `3.6.3`（`/mnt/d/service/apache/apache-maven-3.6.3`）；JDK `1.8.0_502` |
| Maven `localRepository` | `conf/settings.xml:8` = `D:\service\maven\repository`（**Windows 路径**，WSL 下会生成字面目录）；镜像 = `123.232.10.234:8212`（`mirrorOf: central,nexus-public`）。**因此构建命令必须显式传 `-Dmaven.repo.local`** |
| 依赖目录 | `/mnt/d/service/maven/repository` 存在（132 个顶层条目） |
| 文件系统 | `/mnt/d` = **9p (v9fs)** → 大量小文件操作极慢；全量构建/测试约 **4 分钟**，**不可**按本地盘估计 |
| 测试形态 | 纯 Mockito 单测（`@ExtendWith(MockitoExtension.class)`，无 Spring 上下文；`src/test/resources` 不存在） |

> **构建证据的归属**：上表「构建与单测」行按**船长提供的实测输出**登记（本文档作者未复跑；`target/`
> 下未保留 surefire 报告）。需要独立复核时，按该行命令原样重跑即可（约 4 分钟）。
>
> **与 §2 任务图的关系**：§2 的任务图是**计划期快照**（其中 t6 / t7 / t8 标为「待执行」）；
> **实际落盘状态以本节实测为准**（代码改造已落盘）。

### 7.2 已实测（与 R6 相关）
单元测试**不会**写 `grimoire-files/`（存储层是 mock，不落盘；`src/test` 树中 `base-path` / `grimoire-files` **0 命中**）。R6 **只对"启动应用 / E2E"成立**。

### 7.3 已知环境坑位（已实测，**不是**猜测）

| 坑位 | 表现 | 规避 |
|---|---|---|
| `mvn clean` **不可用** | maven-clean-plugin 在 `/mnt/d`(9p) 上 `Failed to delete .../target` | 用 `rm -rf target` 代替 `clean`（见 §7.1 命令） |
| `localRepository` 是 Windows 路径 | 不加参数时 Maven 会把 `D:\service\maven\repository` 当成 CWD 下的**字面目录名**（历史上仓库根确实出现过同名目录并被清理） | **必须**显式 `-Dmaven.repo.local=/mnt/d/service/maven/repository`；提交前按 §6.2 第 3 条自检 |
| **不要**加 `-o`（离线） | 离线模式在缺件时会直接失败 | 私有 nexus 与 Central 均可达，保持在线 |
| 陈旧 `target/` 造成**幻影测试数** | 不清理直接 `mvn test` 会报 `Tests run: 50, Errors: 2`（残留的历史测试类）；真实基线是 **48** | 先 `rm -rf target`；`50 - 48 = 2` 即幻影数 |
| `.lastUpdated` read-only 警告 | 构建日志中的告警 | **无害**，不阻塞 `BUILD SUCCESS` |

### 7.4 验证路径（**主路径 + 补充证据**，按证据强度）

1. **【主路径 · 运行时，最强】接口级黑盒**：启动应用后跑 `api-contract.md` §4 Phase 4 的 ①~⑩
   —— 直接对应 **A3 / A4**，判据与命令见该文件（本文件不搬运）；
2. **【主路径 · 构建】构建 + 单测**：`rm -rf target && mvn -Dmaven.repo.local=... test`（§7.1 已通过；回归时重跑）；
3. **【补充 · 静态等价】**：全仓 grep（`getUrl` / `.getUrl(` / file 包 `@TableLogic` / `attach_url` / 附件语义 `knowledge_id`）
   + `SHOW CREATE TABLE` 断言 —— 对 **A2** 有效，对运行时行为**无效**；
4. **【补充 · DB 对账】**：跑 SDD §2.3.2 三条对账 SQL —— 对 **A4** 有效，对 A1 / A2 无效。

> 第 2 条（构建）**已实测可用**，故**不再**作为"兜底路径"；不得把已可用的主路径降级为 fallback。

### 7.5 残余不确定性（收窄后仍存在）

- ✅ **已消除**：「构建可用性未知」——构建与单测均已通过（§7.1 / §7.3）。
- ⚠️ **仍不确定：运行时行为**。构建证据只证明"编译 + 单测通过"；**秒传命中不写盘**、**删除的引用计数**、
  **提交后删盘**这些**运行时**行为**尚未实测**（需真端点，见 §7.4 第 1 条），由 **t9** 判定 A3 / A4。
  其未实测的原因**不是"代码未改造"**（代码改造已落盘，见 §7.1 的「代码改造落盘状态」行），而是**尚未启动应用做黑盒验证**。
- ⚠️ 因此：**t9 之前，A3 / A4 只能判"待验证"**；若最终无法启动应用，必须判"**未验证**"而**不是**"通过"。

---

## 8. 回滚决策点

> **命令 / 备份文件 / 前提断言的完整内容**在 `backend-audit.md` §13 与 `sql/backup/`；本节只定"**在哪个时点回滚到什么**"。

| 时点 | 回滚对象 | 方式 | 前提 |
|---|---|---|---|
| T-A 之前 | 无 | — | — |
| T-A 之后、T-C 之前 | **文档基线** | 按提交粒度 `git revert` | — |
| 代码阶段（T-C / T-D） | 代码提交 | `git revert` / 重写 | 不涉及 DDL |
| **任一时刻** | **DDL** | **不可 `git revert`**：用 `sql/backup/attachment_pre_migration_2026-09-20.sql`，或按 git 中 `sql/` 的 HEAD 版本重建 | **必须先过 SDD §3.0 的两表 0 行断言** |

**顺序约束（不可反）**：删除路径必须"**先提交事务、后删盘**"。先删盘再回滚会产生**不可恢复**的悬空引用（SDD §2.5.7、backend-audit §11 R2）。

---

## 9. 收口检查单（t9 与 t16 共用）

- [ ] G0 文档版本一致（`README §4` ↔ 五份文档头）
- [ ] G1 结构断言 = SDD §2.3.4；两表行数与预期一致
- [ ] G2 构建 / 测试（§7.1 已实测通过：48 tests / `BUILD SUCCESS`；回归时按 §7.3 的坑位重跑）
- [ ] G3 秒传不写盘、返回同一 `fileId`
- [ ] G4 差量 / 复活 / 白名单 / 排序
- [ ] G5 删除守卫 / 引用计数 / 提交后删盘 / 三条对账为空
- [ ] G6 文档零漂移 + `FileVo` 暴露面收缩
- [ ] A1–A4 逐条判定并给出证据形态（**A3 / A4 的运行时部分需 §7.4 第 1 条**，否则判"未验证"）
- [ ] 无 `grimoire-files/20*`、无 `grimoire-files/tmp`、无字面 `D:\service\maven\repository`
- [ ] `README §4` 终版刷新（t16）

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1.0 | 2026-09-20 | 初版：权威边界声明（与 `api-contract.md §4`、`backend-audit.md §13` 的四方分工）；四项已拍板决策与基线；实际任务图与前置条件；门禁 G0–G6；验收点 A1–A4 与证据形态；证据位置索引；提交时点表与自检；已知限制（Maven 环境实测 + 未验证项 + 替代验证路径）；回滚决策点；收口检查单。**未改代码、未改 sql/、未执行 DDL、未提交** |
| v1.1 | 2026-09-20 | **§7 事实更正（收口）**：①「构建可用性未验证」→ **已验证**（§7.1 新增「构建与单测」行：`Tests run: 48, Failures: 0, Errors: 0` / `BUILD SUCCESS` / `03:52 min`，并标注证据归属）；② 新增「**代码改造落盘状态**」实测行（getUrl 0 命中 / FileVo 7 字段 / attachment 包已建 / AttachmentVo 8 字段 / 差量与复活 / 提交后删盘）；③ §7.3「风险（未验证）」→「**已知环境坑位（已实测）**」表（`clean` 不可用、必须显式 `-Dmaven.repo.local`、不加 `-o`、9p 慢约 4 分钟、50→48 的幻影测试数、`.lastUpdated` 警告无害）；④ §7.4「替代验证路径（当 G2 不可用）」→「**验证路径（主路径 + 补充证据）**」，构建回到主路径、不降级；⑤ 新增 §7.5「残余不确定性（收窄后）」——**仍不确定的只剩运行时行为**（需启动应用，由 t9 判 A3/A4）。同步交叉引用：§3 G2 门禁与 §9 检查单（不再指向"替代路径"）。**未改其它章节、未改代码、未改其它文档** |
