---
name: handoff-bundle
description: 当需要把当前任务上下文、工作状态和散落资产冻结为可携带、可核验、可恢复的交接包，交给另一个 GPT/Codex/Agent 继续时使用。适用于"交接任务""保存进度""换新对话继续""打包附件给另一个Agent"等场景。产出 handoff_bundle/ 目录，含 HANDOFF.md、资产清单、SHA-256 校验和持久化资产副本。技术栈与工具无关。
type: Skill
version: 1.1.0
---

# handoff-bundle  

把当前对话中的临时上下文、工作状态和散落资产转换为可携带、可核验、可恢复的 `handoff_bundle/`，让另一个 GPT、Codex 任务或 AI Agent 能在不重新猜测背景的情况下继续工作。

## 技术栈与工具无关性声明

本技能是跨 Agent 通用交接工具，**不绑定**：

- 编程语言/框架（非 Java/Spring Boot 专属）；
- AI 模型族（GPT/Claude/Lingma/Codex 等均可作为发送方或接收方）；
- 宿主工具（不要求 gitnexus、不要求特定 IDE 的 subagent 机制）。

契约层只依赖三类跨平台标准：

1. 文件系统读写（无文件系统时降级为"对话模式"，只输出文本）；
2. SHA-256 哈希（任何语言标准库均可算）；
3. 标准 Git 命令（非 Git 仓库时两文件仍生成并标注"不可用"）。

与 aik-skills-lab 其他技能的联动（如"建议技能"字段引用 `$spec-implementer`）是**可选增益**，仅当接收方确认处于 aik 生态内时启用；外部 GPT 接收时该字段留空或写通用描述。

## 使用原则

1. 事实优先于聊天叙事。工作区、实际文件、Git 状态和验证结果与聊天记忆冲突时，以可验证事实为准，并记录冲突。
2. 只收集本次任务明确上传、引用、读取、修改或生成的资产。禁止扫描整个临时目录、用户目录、下载目录或浏览器资料目录猜测资产。
3. 不得打包密码、API 密钥、Cookie、会话令牌、私钥、凭据文件或无关个人文件。
4. 不得声称不存在的能力。如果当前环境没有文件系统、命令执行、哈希或创建新任务权限，必须说明未执行的部分，并输出可供用户手动保存的内容。
5. 只有全部必需资产存在且校验通过，才能宣布交接包完整。
6. 默认只生成交接包。只有用户明确要求创建、打开或转移到新任务时，才允许新建任务并发送。

## 能力模式

开始前判断当前环境：

- **完整模式**：可读写本地文件、复制附件、运行 Git、计算 SHA-256，并能使用任务管理工具。执行完整流程。
- **文件模式**：可读写文件和计算哈希，但不能新建任务。生成并验证交接包，把路径和启动提示词交给用户。
- **对话模式**：不能访问本地文件系统。仅生成 `HANDOFF.md`、资产候选清单和启动提示词的文本内容；要求用户重新上传必需资产。不得声称已经复制文件、读取 Git 或验证哈希。

## 多模态能力与降级

除上述环境能力外，开始前还必须判断当前模型是否支持图片、视频、音频的读取与理解：

- **多模态可用**：模型可直接处理图片、视频、音频。正常收集、复制和描述这些资产。
- **多模态不可用**：模型不支持非文本资产。执行以下降级策略：
  1. 不尝试读取、分析或内联引用图片/视频/音频内容。
  2. 若对话或工作区中存在此类资产，通过文件名、扩展名、用户提供的描述或路径来识别其存在，并在资产候选表中记录；`category` 仍按原类型标注（`images`/`references`），`status` 设为 `unavailable`，`purpose` 中注明"当前模型不具备多模态能力，无法内联处理该资产"。
  3. 在 `HANDOFF.md` 的"资产要求"中明确列出这些资产，并提示"需接收方环境支持多模态，或由用户手动补充"。
  4. 这些资产不参与 SHA-256 校验（因当前模型无法读取内容），但必须在 `ASSET_MANIFEST.json` 中保留记录以提醒接收方。
  5. 多模态资产标记为 `unavailable` 时，不阻断交接包生成，但必须在 `warnings` 和最终回复中明确告知用户。

## 工作流

### 1. 确定交接范围

从完整对话和可用工作区中提取：主目标与验收标准、已完成/进行中/未开始/推迟事项、用户纠正与偏好、关键决策与被否决方案、失败尝试与踩坑、修改或生成的文件与验证结果、阻塞与风险、下一任务可能调用的技能。

不要平均压缩所有历史。优先保留会影响继续工作的内容，尤其是用户纠正、未完成状态、精确标识符、文件路径、数值和失败原因。

**多模态降级**：若当前模型不具备多模态能力，遇到图片/视频/音频资产时不要求读取其内容，只需识别其存在和用途（由用户描述或文件名推断），并在后续资产候选表中按降级策略标记。

### 2. 编写 HANDOFF.md

按 [handoff-template.md](references/handoff-template.md) 骨架填写。某部分为空时写"无"，不要删除标题。已有规格、计划、ADR、Issue、提交、差异文件和成品只引用路径或 URL，不重复粘贴全部内容。凭据只能写成"凭据已配置"或"需要用户重新提供"。

### 3. 建立资产候选表

候选来源仅限：当前对话明确提供的附件路径；用户点名要求保留的文件；本任务中实际读取、编辑或生成的文件；为继续工作不可缺少的局部代码、数据、图片、视频、音频或文档。

为每项资产记录 `path`、`purpose`（不能只写"附件"）、`priority`（required/reference）、`category`（images/documents/references）、`destination_name`（普通文件名，禁止目录穿越片段）。

**多模态降级**：若当前模型不具备多模态能力，对图片/视频/音频资产不尝试打开或解析，仅记录其元数据（文件名、路径、用户描述）；`priority` 和 `category` 正常标注，后续在 `ASSET_MANIFEST.json` 中标记 `status` 为 `unavailable`，并在 `purpose` 中说明能力限制。

### 4. 复制并持久化资产

按 [bundle-structure.md](references/bundle-structure.md) 的分类规则复制到 `assets/` 子目录。临时目录附件必须复制到包内，清单不能只留随时可能失效的临时路径。禁止复制 `.env`、私钥、Cookie、credential、token、password 或 secret 文件。

### 5. 生成 ASSET_MANIFEST.json

按 [asset-manifest-schema.json](references/asset-manifest-schema.json) 生成清单。每个实际复制的文件必须记录源路径、相对目标路径、用途、优先级、分类、媒体类型、字节数和 SHA-256。目录输入展开成单文件记录并保留 `source_root`。

### 6. 捕获 Git 状态

工作区是 Git 仓库时，在创建交接目录之前捕获：

- `git status --short --branch --untracked-files=all` → `git-status.txt`
- `git diff --binary --no-ext-diff HEAD --` → `changes.patch`

写入前检查补丁是否含密码/令牌/密钥；发现疑似敏感值时脱敏并在 `warnings` 说明补丁不再适合直接应用。非 Git 仓库或命令不可用时两文件仍必须存在并标注"不可用"原因。

### 7. 生成 checksums.sha256

对交接包内除 `checksums.sha256` 自身之外的每个文件计算 SHA-256，格式：`<64位小写哈希><两个空格><相对路径，使用正斜杠>`。

### 8. 生成 NEXT_TASK_PROMPT.md

写入接收方启动提示词，强制执行"先核验、再复述、后继续"。完整模板见 [verification-protocol.md](references/verification-protocol.md)。

### 9. 生成 README.md

说明创建时间和源工作区、每个固定文件用途、资产总数与分类统计、实际使用的校验命令、`changes.patch` 对未跟踪文件的限制、敏感文件检测只是防线不替代人工审核、必需资产或校验失败时不得继续。

## 交接包结构

固定输出结构详见 [bundle-structure.md](references/bundle-structure.md)。核心目录树：

```text
handoff_bundle/
├── HANDOFF.md
├── ASSET_MANIFEST.json
├── NEXT_TASK_PROMPT.md
├── git-status.txt
├── changes.patch
├── checksums.sha256
├── assets/{images,documents,references}/
└── README.md
```

## 验证与接收协议

**铁律：先核验、再复述、后继续。**

- 生成后完整阅读 `HANDOFF.md`、`ASSET_MANIFEST.json`、`README.md`，检查用途、分类、路径和警告是否真实。
- 校验命令跨平台：Windows PowerShell `Get-FileHash -Algorithm SHA256`；Linux `sha256sum`；macOS `shasum -a 256`。
- 校验失败时不得宣布完成；列出具体失败文件和原因；补齐或移除错误资产后重新生成校验表；重新验证整个包。
- 接收方收到交接包时：先验证固定目录、校验表和所有必需资产；完整读取四个固定文件；在修改任何文件前先复述目标、状态、资产校验结果、风险和第一步。
- 任何必需资产缺失、哈希不匹配、路径不可读或工作区状态与交接文档明显冲突，必须停止并报告。

详细复述协议与创建新任务流程见 [verification-protocol.md](references/verification-protocol.md)。

## 完成标准

- 固定输出全部存在；
- `checksums.sha256` 覆盖所有包内文件并校验通过；
- 每个资产都有来源、相对目标、用途、优先级、字节数和 SHA-256；
- 所有必需资产均已复制，缺失项没有被静默忽略；
- Git 状态和补丁明确说明是否来自有效 Git 工作区；
- `NEXT_TASK_PROMPT.md` 强制执行"先核验、再复述、后继续"；
- 敏感信息和无关文件未进入交接包；
- 最终回复给出交接包路径、资产数量、必需/参考分类、校验结果，以及是否创建了新任务；
- 环境能力不足时（包括非多模态模型无法处理图片/视频/音频），最终回复必须精确区分"已生成""仅提供文本""尚未复制""尚未验证""尚未发送"，并明确说明哪些资产因模型能力限制未被处理。

## 便携版分发

需要把本技能交给没有安装 aik-skills-lab 的外部 GPT 时，使用 [assets/handoff-skill-portable.md](assets/handoff-skill-portable.md) 单文件版。它是本技能的派生便携副本，自包含全部工作流、模板、安全规则和接收协议，不依赖 references 目录，可原样上传给任意 GPT/Codex/Claude 学习。

**维护规则**：便携版是派生产物，规则变更只在 SKILL.md + references 里改，再同步便携版。便携版头部注释标注派生版本号，外部接收方据此判断版本是否滞后。
