# dsh-grimoire-trinity

DSH bundle：注册 agent preset **`grimoire-trinity`**（显示名「月見里 & 綾雾」）。

- 一个 Agent、两个人设：**綾雾彌奈**（默认/日常）与 **月見里緋緖**（开发）；
- persona 前缀来源：`persona-prefix.txt`；`cordis.patch.yml` 由该文件 + 当前 standard preset 组合生成；
- 共享契约：仓库根 `AGENTS.md`；详细人设：`../personas.md`。

## 安装（web profile）

```bash
dsh plugin --profile web add /path/to/repo/grimoire-agent/dsh-preset
```

安装后重启 DSH（或由 DSH profile 重载），在 Web 设置的 Agent 预设中选 `grimoire-trinity`。
先保持“可选”，测试稳定后再设默认。

## 更新 persona

1. 改 `persona-prefix.txt`（以及 `../personas.md`）；
2. 重新生成 `cordis.patch.yml`（见下）；
3. `dsh plugin --profile web add` 重装或更新后重启。

生成方式：

```bash
cd grimoire-agent/dsh-preset
node build.mjs
```

它会读取 `persona-prefix.txt`，以当前 `dsh --profile web --dump-config` 的 standard preset
为底，重新生成 `cordis.patch.yml`。standard preset 随 DSH 升级变化时，重新跑一次即可。

## 卸载

```bash
dsh plugin --profile web remove dsh-grimoire-trinity
```

卸载不会删除 `personas.md` / `AGENTS.md` 等仓库文件。
