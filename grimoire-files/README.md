# AikSteinsGrimoire — 系统文档总览

> 本文件是 AikSteinsGrimoire **系统自身**（需求 / 设计 / 契约 / 计划 / 决策）的**唯一入口**。
> 任何"这条事实该写在哪"的问题，先查 [§3 权威边界](#3-权威边界防漂移核心)。
>
> 版本：v2.0 | 更新：2026-09-20 | 状态：生效中 | 权威范围：文档结构与权威归属

---

## 1. 范围界定

**包含**：系统本身的设计相关文档 —— 需求、后端设计、前端设计、接口契约、计划、决策。

**不包含**（明确排除，避免再次混为一锅粥）：

| 排除对象 | 实际归属 |
|---|---|
| `component-manuals/` | 知识卷轴产物（组件萃取手册） |
| `learning-notes/` | 知识卷轴产物（学习笔记） |
| `wsl/` | 环境搭建操作手册，非项目设计 |
| `.qoder/repowiki/`（86 份） | AI 生成的派生文档，**不作权威** |
| `grimoire-agent/` | 智能体自身的演进档案，独立体系 |
| 文件存储调研与重构记录 | 历史记录，仅在 §4 登记指针 |

---

## 2. 目录结构

```
grimoire-files/
├── README.md                       # ← 本文件：唯一入口
├── design-docs/                    # 设计文档（按模块分子目录）
│   ├── system-module/              # system 模块
│   │   ├── PRD.md                  #   需求：字典 / 参数 / 文件
│   │   ├── SDD.md                  #   后端设计：分层、表结构、实体
│   │   ├── api-contract.md         #   接口契约：HTTP 与字段定义
│   │   └── backend-audit.md        #   后端现状审计：功能盘点与矛盾清单
│   ├── knowledge-module/
│   │   └── design.md               # Knowledge 模块包与表结构设计
│   └── frontend/
│       └── design.md               # Grimoire Web 前端设计
├── plans/                          # 计划与历史记录
│   ├── execution-plan.md           #   仓库级执行计划：顺序 / 门禁 / 证据索引 / 回滚
│   ├── project-architecture.md     # ⚠️ 已取代（保留溯源）
│   ├── file-storage-research-report.md
│   └── file-storage-strategy-refactor.md
├── component-manuals/              # 知识卷轴产物（不在本体系）
└── learning-notes/                 # 知识卷轴产物（不在本体系）
```

> **文件名不含版本号**。版本只存在于文档头的 `版本：` 字段。
> 历史上 `-v1.0` / `-v2` 这种命名已导致一次真实分叉（见 §5），不再沿用。

---

## 3. 权威边界（防漂移核心）

**每一个事实只有一个权威归属方。** 修改任何事实前，先在此表定位它的权威文档。

| 事实类别 | 唯一权威 | 消费方 |
|---|---|---|
| system 模块的**需求与功能清单** | `design-docs/system-module/PRD.md` | 全部 |
| system 模块的**分层、表结构、实体定义** | `design-docs/system-module/SDD.md` | 后端 |
| **所有 HTTP 接口**（路径 / 方法 / 入参 / 出参 / 错误模型） | `design-docs/system-module/api-contract.md` | 前端、后端 |
| Knowledge 模块的**包结构与表结构** | `design-docs/knowledge-module/design.md` | 后端 |
| **前端**页面、路由、组件、视觉 | `design-docs/frontend/design.md` | 前端 |
| **执行阶段划分与验收** | `design-docs/system-module/api-contract.md` §4 | 全部 |
| **仓库级执行顺序、门禁、证据索引、提交时点、回滚决策点** | `plans/execution-plan.md` | 全部 |
| **代码与 DDL 的实测现状**、模块间矛盾清单；本轮改造的**退役台账 / 风险登记 / 改动边界 / 提交策略**（§10–§13） | `design-docs/system-module/backend-audit.md` | 后端、全部 |
| 跨模块**决策记录** | `grimoire-agent/agent/decision-log.md` | 全部 |

### 3.1 冲突裁决顺序

同一事实出现分歧时，按此顺序裁决：

```
实际代码与 DDL  >  api-contract.md  >  模块 design.md  >  SDD.md  >  PRD.md
```

**代码与 DDL 永远优先。** 文档与代码冲突时，是文档错了，不是代码错了。

### 3.2 变更时必须做的同步

| 如果你改了… | 必须同步检查 |
|---|---|
| `sql/*.sql`（表名、字段） | `SDD.md`、`knowledge-module/design.md`、`api-contract.md` |
| `src/` 下的 Controller | `api-contract.md`，再检查 `frontend/design.md` |
| 前端页面或路由 | `frontend/design.md` |
| 任何架构性取舍 | `grimoire-agent/agent/decision-log.md` |

> **本体系的建立动机**：`4f366df`（2026-08-26）把表名统一改为 `aik_sys_*`，同步了
> `frontend/design.md`，但漏掉了 `PRD.md` / `SDD.md` / `project-architecture.md`。
> 缺的就是上面这张表。**同步的责任方是改代码的人，不是读文档的人。**

---

## 4. 文档状态

| 文档 | 版本 | 更新 | 状态 | 备注 |
|---|---|---|---|---|
| `system-module/PRD.md` | v1.2 | 2026-09-20 | 生效中 | 附件/文件域需求已对齐两层模型（F005~F010、US-005~007/009、§3.3 Won't Have、§6.1/§6.2） |
| `system-module/SDD.md` | v1.5 | 2026-09-20 | 生效中 | **附件域两层模型的唯一权威**（§2.5 / §2.3.4）；v1.3 记录迁移已落地，v1.4 同步 `findByBiz` 与不回显裁决，v1.5 随契约把 `AttachmentVo` 收敛为 **8 字段**并澄清暴露面边界与行区间约定 |
| `system-module/api-contract.md` | v1.2.2 | 2026-09-20 | 生效中 | 附件三端点与 `AttachmentVo`（**8 字段**）的权威（§2.5）；`findByBiz` 只返回 `del_flag = 0`、按 `sort_order` 排序、**按 `fileId` 批量取禁 N+1**；待执行项见其 §8 |
| `system-module/backend-audit.md` | v1.1 | 2026-09-20 | 生效中 | §0–§9 = 现状与 23 项矛盾清单；**§10–§13 = 退役台账 / 风险登记（R1–R6）/ 改动边界 / 提交策略**（其中「上传目录撞文档树」的**权威登记**是 §11 **R6**，见 §3 与 §7 第 4 条） |
| `knowledge-module/design.md` | v1.3 | 2026-09-20 | 生效中 | 包结构与表结构；§3.2 已重写为**通用附件挂载表** + knowledge 侧接入边界；**不含** `aik_sys_attachment`（归属 system 模块） |
| `frontend/design.md` | v2.3 | 2026-09-20 | 生效中 | §附件交互 已定稿（零 URL 拼接 / blob 约定 / **8 字段**）；R18 已解决；§待修订项 18 项中 **15 项**未处置（含 1 项需用户决策 R09） |
| `plans/execution-plan.md` | v1.0 | 2026-09-20 | 生效中 | **仓库级执行计划**：执行顺序 / 门禁 G0–G6 / 验收判据 A1–A4 / 证据索引 / 提交时点 / 回滚决策点（**接口级**验收命令仍归 `api-contract.md` §4） |
| `plans/project-architecture.md` | — | — | **已取代** | 仅保留溯源，勿引用 |

**状态取值**：`起草中` / `生效中` / `已取代` / `已归档`

> **本表为终态**（2026-09-20 收口：逐份打开文档头实测填写，非照抄）。本轮附件域改造的设计文档链已全部同步完毕。
> 之后任何文档版本变更，**必须同时改本表与文档头两处**（§7 第 2 条）。

---

## 5. 进化历史机制

每个设计文档头部统一携带这四行：

```markdown
> 版本：v1.3 | 更新：2026-09-20 | 状态：生效中 | 权威范围：<本文档拥有的唯一事实>
> v1.2 → v1.3：<本次变更一句话>（依据 <提交号>）
```

**三条规则**：

1. **历史正文追加**，写在文档末尾的「附录：变更历史」表格里，不新建文件。
2. **不产生新文件名**。禁止 `xxx-v2.md`、`xxx-new.md`、`xxx-final.md`。
3. **不保留平行副本**。新版本直接覆盖，旧版本交给 git 保管。

> **反面教材**：`agent-expansion-plan.md` 曾同时存在于
> `grimoire-files/plans/`（v0.1）与 `grimoire-agent/plan/`（v0.2），
> 两份都被 git 跟踪。v0.2 自己的头部写着「v0.1 → v0.2」却没人删 v0.1。
> 已于 2026-09-20 合并为单一 v0.3。

---

## 6. 已失效路径登记

以下路径在本次整合中被移动或删除，供从旧笔记跳转时对照：

| 旧路径 | 现路径 / 处置 |
|---|---|
| `design-docs/system-module/PRD-v1.0.md` | → `system-module/PRD.md` |
| `design-docs/system-module/SDD-v1.0.md` | → `system-module/SDD.md` |
| `plans/backend-api-contract.md` | → `system-module/api-contract.md` |
| `plans/frontend-design.md` | → `design-docs/frontend/design.md` |
| `plans/knowledge-module-design.md` | → `design-docs/knowledge-module/design.md` |
| `plans/agent-expansion-plan.md`（v0.1） | 已合并入 `grimoire-agent/plan/agent-expansion-plan.md` v0.3 |
| `plans/aik-dev-agent/`（曾被本 README 引用） | 该目录**从不存在**，引用已移除 |

> 文档内部若留有指向旧路径的链接，按上表替换。

---

## 7. 维护约定

1. **改代码的人负责同步文档**，同步项见 §3.2。
2. **本文档的 §4 状态表与每个文档头部的版本字段必须一致**，改一处就改两处。
3. **新增设计文档前先问**：这条事实能否归入现有文档的权威范围？能则不要新建。
4. **`grimoire-files/` 是 git 跟踪的正式文档目录**；注意后端上传目录当前也配置在
   `./grimoire-files`（见 `api-contract.md` §7.1），二者重合是一个待处理风险。
   > 该风险的**权威登记与完整实测**见 `design-docs/system-module/backend-audit.md` §11 **R6**
   > （三项实测 + 适用范围收窄 + 三选一修正建议 + Reopen Trigger）；本节仅保留索引，
   > 详细分析与修正建议**以 R6 为准**。
