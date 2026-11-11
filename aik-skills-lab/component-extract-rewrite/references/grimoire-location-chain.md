# 魔典定位协议（grimoire-locator）

> 本文件是 `component-extract-rewrite` 的 L3 参考：**如何在本机定位到物理机上的 AikSteinsGrimoire 项目目录**。
> 只读、可验证、跨环境；不依赖 PowerShell / Everything，不自动写环境变量，定位失败时不写任何文件。
> 权威方案：`grimoire-files/plans/knowledge-ingestion-rework.md` §4。

## 0. 铁律

1. **只读**：定位过程不写文件、不创建目录、不改环境变量。
2. **不猜测**：找不到就报告 `NEED_PATH` 并停止，绝不把产物写进当前工作区或临时目录。
3. **不复制**：WSL 与 Windows 是同一份物理文件（`/mnt/d/...` ↔ `D:\...`），不需要拷贝或同步。
4. **`aIk` 标识不动**：`AIK_GRIMOIRE_HOME`、`aIk-coding-style`、`AikSteinsGrimoire` 等保持原样。

## 1. 先检测环境，再选路

| 环境 | 检测信号（只读） | 魔典路径形态 | 策略 |
|---|---|---|---|
| **WSL2**（本机默认） | `/proc/version` 含 `microsoft`，或 `WSL_DISTRO_NAME` 存在，或 `wslpath` 可执行 | `/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire` | 直接读写；`wslpath` 与 Windows 路径互转 |
| **Windows 原生** | `uname` 为 MINGW/MSYS/CYGWIN，或纯 PowerShell 会话 | `D:\JeBrainsWorkSpace\AikSteinsGrimoire` | 直接读写；用 `mklink /J` 建技能链接 |
| **独立 VM + 共享目录** | `systemd-detect-virt` 非 none，且存在 `/mnt/hgfs`、`/media/sf_*` | 共享目录下的仓库路径 | 直接读写共享目录 |
| **独立 VM 无共享** | 同上但无共享挂载 | 不可达 | 走"拷贝桥"（离线 bundle + 物理机 merge，见 §7） |
| **容器** | `/.dockerenv` 或 cgroup 含 docker/containerd | 取决于 volume mount | 按挂载点判断 |

> 用户术语约定：说"虚拟机/WSL"即本机 WSL2（Ubuntu）；说"物理机"即本机 Windows。二者同一份文件。

## 2. 魔典身份验证（is_grimoire）

一个目录要被判为"本机魔典"，必须同时满足：

| 检查 | 内容 |
|---|---|
| M1 | `<root>/grimoire-files/README.md` 存在 |
| M2 | `<root>/grimoire-scrolls/README.md` 存在 |
| M3 | `<root>/src/main/java/io/aik/steins/grimoire/knowledge/` 存在 |
| M4 | `<root>/pom.xml` 含 `<name>AikSteinsGrimoire</name>` 或 `<artifactId>aik-steins-grimoire</artifactId>` |
| M5 | `git -C <root> remote get-url origin` 命中 `AikSteinsGrimoire`（大小写不敏感） |

- 扫描候选必须 M1–M5 全过；anchor / 环境变量显式指向时允许只过 M1–M4（并在输出里标记 `weak`）。
- 额外输出 `remote` 与 `head`（短 commit），写入摄取报告供对账；HEAD 落后只告警，不自动 pull。

## 3. 定位顺序（只读，命中即止）

```text
E0  检测环境（§1）
P0  cwd 及其祖先：is_grimoire(cwd)                 → source=cwd
P1  machine anchor（§4）逐个路径 is_grimoire       → source=anchor
P2  环境变量 AIK_GRIMOIRE_HOME（只读）             → source=env
P3  anchor 里的跨宿主路径（wslpath 转换）          → source=anchor
P4  有界扫描（≤3 层、限时 10s、只读）              → source=scan
P5  仍无 → NEED_PATH，停止，等用户给路径
```

**禁止**：全盘搜索、自动 `git pull`、写 `AIK_GRIMOIRE_HOME`、失败时写 `doc/_grimoire_pending/`。

## 4. machine anchor（书签，不是链接、不是拷贝）

**是什么**：一个几行 JSON 的小文件，记录"本机上魔典在哪"，让技能从任意 cwd 调用时不用翻磁盘。
类比 IDE 记住项目路径；只存路径与身份信息，**不含凭据，不复制任何内容**。

推荐位置（按宿主）：

```text
WSL/Linux : ~/.aik/grimoire-anchor.json
Windows   : %USERPROFILE%\.aik\grimoire-anchor.json
DSH 专用  : $DSH_HOME/aik/grimoire-anchor.json（可选备份）
```

格式：

```json
{
  "repo": "AikSteinsGrimoire",
  "remote": "git@github.com:arrowInkneeeee/AikSteinsGrimoire.git",
  "paths": {
    "wsl": "/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire",
    "windows": "D:\\JeBrainsWorkSpace\\AikSteinsGrimoire"
  },
  "fingerprint": { "pomName": "AikSteinsGrimoire", "head": "<写入时短 commit>" },
  "writtenAt": "2026-11-11T11:00:00+08:00",
  "writtenBy": "AI-assisted (user approved)"
}
```

- anchor 只是缓存提示，**每次仍逐项验证**；失效即忽略并继续 P2–P5。
- 写入必须用户明确同意（一次性）；缺失不影响使用（走 P2–P5）。
- 权限：目录 700、文件 600；只含路径，无凭据。

## 5. 多副本 / 冲突处理

- 多个通过验证的候选：优先 anchor；否则列出全部候选（envType、remote、HEAD）让用户选；**不自动选"最近的"**。
- 同一仓库在 WSL/Windows 两种路径表示：按当前宿主选择，报告注明另一表示。
- 返回 `AMBIGUOUS` 时，必须停下来让用户选，不得继续落盘。

## 6. 输出契约

成功：

```json
{
  "status": "OK",
  "envType": "wsl2",
  "strategy": "direct",
  "path": "/mnt/d/JeBrainsWorkSpace/AikSteinsGrimoire",
  "source": "cwd|anchor|env|scan",
  "remote": "git@github.com:...",
  "head": "3b1c577",
  "paths": { "wsl": "/mnt/...", "windows": "D:\\..." }
}
```

失败：

```json
{ "status": "NEED_PATH", "envType": "wsl2", "attempted": ["..."], "hints": ["..."] }
```

多候选：`{ "status": "AMBIGUOUS", "candidates": ["..."] }`。
退出码：0 命中 / 2 NEED_PATH / 3 AMBIGUOUS / 1 参数错误。

## 7. 拷贝桥（仅未来离机/独立 VM 无共享时）

```text
生成侧（不可达魔典）：ingest --offline → knowledge-bundle/
  ├── payload/<目标相对路径>.md
  ├── manifest.json
  ├── checksums.sha256
  ├── merge.sql
  └── README.md（合并步骤）
传输：共享目录 / SMB / U 盘 / 网盘
接收侧（魔典 workspace）：merge <bundle>
  → 校验 checksums + is_grimoire + 去重
  → 只追加不覆盖 → 生成入库 SQL → merge 报告
```

当前 WSL2 ↔ Windows 同机同盘，**不使用**此路径。

## 8. DSH 写策略门禁（落盘前）

```text
if grimoire.path 不在 session workspace 内:
    停止 → 让用户二选一：
      A) 以魔典为 workspace 重新打开 DSH 会话（推荐）
      B) 对本次落盘显式授权越界写（审批），并记录在报告里
```

理由：DSH 默认 workspace-write，只能写会话工作区；silent failure 比停下来更糟。

## 9. 技能分发形态（单副本，澄清"两份/同步"）

| 宿主 | 形态 | 内容副本 |
|---|---|---|
| Qoder / QoderCN | 整库 Junction/SymbolicLink → `aik-skills-lab/` | 0（同一份） |
| DSH（WSL） | `~/.dsh/skills/<技能>` per-skill symlink | 0（同一份） |
| DSH（Windows 原生） | `C:\Users\<用户>\.dsh\skills\<技能>` per-skill Junction | 0（同一份） |
| 无法建链接的宿主 | 复制 | 1 份/宿主（兜底，需校验/同步） |

- Windows 原生 DSH 建法（管理员或开发者模式）：
  `mklink /J "C:\Users\arrowInknee\.dsh\skills\code-review" "D:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab\code-review"`
- 仓库路径移动后：重跑 `./dsh-migration/install.sh`（DSH symlink）与对应的 Windows junction；**不复制内容**。

## 10. 工具

```bash
# 只读定位（DSH/WSL 下）
bash scripts/locate-grimoire.sh --json

# 从外部目录定位（走 anchor/环境变量/有界扫描）
cd /mnt/d/OtherProject && bash /mnt/d/.../component-extract-rewrite/scripts/locate-grimoire.sh --json

# 指定扫描根、跳过扫描
bash scripts/locate-grimoire.sh --root /mnt/d/JeBrainsWorkSpace --no-scan

# 一次性写入书签（必须先取得用户批准）
bash scripts/locate-grimoire.sh --remember --yes
```

Windows 原生 PowerShell 等价（不依赖 Everything）：

```powershell
$c = @("$env:AIK_GRIMOIRE_HOME", "D:\JeBrainsWorkSpace\AikSteinsGrimoire")
foreach ($p in $c) {
  if ($p -and (Test-Path "$p\grimoire-files\README.md") -and
      (Test-Path "$p\grimoire-scrolls\README.md") -and
      (Test-Path "$p\src\main\java\io\aik\steins\grimoire\knowledge")) {
    & git -C $p remote get-url origin | Select-String AikSteinsGrimoire
  }
}
```
