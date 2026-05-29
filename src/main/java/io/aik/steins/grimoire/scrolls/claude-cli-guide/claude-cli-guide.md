# Claude CLI（Claude Code）Windows 安装与配置指南

> 本指南涵盖 Claude Code 命令行工具在 Windows 环境下的完整安装流程，基于官方一键安装脚本，并适配 DeepSeek V4 模型作为后端推理服务。
>
> 适用平台：Windows 10 / 11。

---

## 一、前置准备（管理员 PowerShell 执行）

### 1.1 强制启用 TLS 1.2（解决 SSL 连接失败问题）

PowerShell 默认 TLS 版本可能不兼容，执行以下命令强制启用 TLS 1.2，避免安装脚本连接超时：

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
```

### 1.2 配置代理（仅安装脚本时需要，后续直连 DeepSeek 可省略）

安装 Claude 官方脚本需要访问 `claude.ai`，必须通过代理；后续使用 DeepSeek 时国内可直连，无需再配置代理。

> **代理节点选择建议**：优先选择**新加坡节点**。避免使用中国香港和中国台湾的节点，这些地区可能因区域限制或风控策略导致访问异常或安装中断。

```powershell
# 配置 HTTP/HTTPS 代理（替换为实际代理端口）
$env:HTTP_PROXY="http://127.0.0.1:7890"
$env:HTTPS_PROXY="http://127.0.0.1:7890"
```

### 1.3 测试代理连通性（可选，确认代理正常）

避免 PowerShell 内置 `curl` 别名报错，使用 `curl.exe` 或 `Invoke-WebRequest` 测试：

```powershell
# 方式1：使用 curl.exe（推荐）
curl.exe -I https://claude.ai

# 方式2：使用 PowerShell 原生命令
Invoke-WebRequest -Uri "https://claude.ai" -Method Head -UseBasicParsing
```

返回 `HTTP/1.1 200 OK` 或 `StatusDescription : OK` 说明代理正常，可继续下一步。

---

## 二、安装 Claude CLI（官方脚本方式）

### 2.1 执行安装命令

官方推荐的一键安装脚本，自动下载、解压并配置基础环境：

```powershell
irm https://claude.ai/install.ps1 | iex
```

安装成功后会显示版本号（如 `Claude Code successfully installed! Version: 2.1.156`），并给出安装路径：

```
C:\Users\<your-username>\.local\bin\claude.exe
```

### 2.2 解决 `claude` 不是内部命令问题（配置 PATH 环境变量）

安装提示 `C:\Users\<your-username>\.local\bin` 不在你的 PATH 中，需配置环境变量，确保任意终端都能识别 `claude` 命令。

**方式1：临时生效（当前 PowerShell 会话）**

```powershell
$env:PATH += ";$env:USERPROFILE\.local\bin"
```

**方式2：永久生效（推荐，所有终端生效）**

1. 按 `Win+R` 输入 `sysdm.cpl` 回车，打开「系统属性」。
2. 切换到「高级」→「环境变量」。
3. 在「用户变量」中找到 `Path`，双击打开。
4. 点击「新建」，粘贴路径：`C:\Users\<your-username>\.local\bin`。
5. 一路点击「确定」保存，**关闭所有 PowerShell 窗口并重新打开**。

---

## 三、核心配置：适配 DeepSeek V4（无代理版）

DeepSeek 官方提供 Anthropic API 兼容接口，无需第三方中转，直接配置即可。以下配置同时支持保留插件和 MCP 服务。

### 3.1 打开配置文件

```powershell
notepad $env:USERPROFILE\.claude\settings.json
```

如果文件不存在，会提示新建，选择「是」即可。

### 3.2 写入完整配置（通用模板）

清空文件原有内容，粘贴以下配置模板（需替换占位符为实际值）：

```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "your-deepseek-api-key",
    "ANTHROPIC_BASE_URL": "https://api.deepseek.com/anthropic",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "deepseek-v4-pro",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "deepseek-v4-pro",
    "ANTHROPIC_SMALL_FAST_MODEL": "deepseek-v4-flash",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "deepseek-v4-flash",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1"
  },
  "alwaysThinkingEnabled": true,
  "autoUpdatesChannel": "latest"
}
```

**配置项说明**：

| 配置项 | 说明 |
|--------|------|
| `ANTHROPIC_AUTH_TOKEN` | DeepSeek API Key，替换为你的实际密钥 |
| `ANTHROPIC_BASE_URL` | DeepSeek Anthropic 兼容接口地址 |
| `ANTHROPIC_DEFAULT_*_MODEL` | 模型映射，将 Claude 模型名映射到 DeepSeek 模型 |
| `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC` | 设置为 `"1"` 关闭后台非必要流量上报，国内环境更稳定 |
| `alwaysThinkingEnabled` | 开启持续思考模式 |
| `autoUpdatesChannel` | `"latest"` 自动跟随官方更新；如需稳定版可改为 `"stable"` |

### 3.3 可选：保留 MCP 插件扩展

如果你正在使用 MCP 服务（如 GitNexus 等），可在配置中追加 `mcpServers` 节点：

```json
{
  "env": { ... },
  "mcpServers": {
    "gitnexus": {
      "args": ["mcp"],
      "command": "<your-gitness-cmd-full-path>",
      "type": "stdio"
    }
  },
  "autoUpdatesChannel": "latest"
}
```

### 3.4 可选：配置代理（直连 DeepSeek 失败时使用）

若后续直连 DeepSeek 失败，可在 `env` 中添加代理：

```json
"HTTP_PROXY": "http://127.0.0.1:7892",
"HTTPS_PROXY": "http://127.0.0.1:7892"
```

---

## 四、验证安装与配置（必做）

### 4.1 验证命令是否生效

新开 PowerShell，执行以下命令，确认能输出版本号：

```powershell
claude --version
# 预期输出：Claude Code 2.1.156
```

### 4.2 检查配置是否正常加载

```powershell
claude doctor
```

无红色报错，说明配置文件、环境变量均正常。

### 4.3 进入交互界面测试

```powershell
claude
```

1. 按提示选择主题（推荐直接按回车确认默认的 Dark mode）。
2. 阅读安全提示后按回车继续。
3. 信任当前文件夹（按回车确认 `Yes, I trust this folder`）。
4. 输入测试指令（如「你现在使用的是什么模型？」），能正常回复说明 DeepSeek V4 调用成功。

---

## 五、日常使用说明

### 5.1 项目根目录直接启动

进入项目文件夹后执行 `claude`，Claude 会自动读取项目文件，无需额外配置：

```powershell
cd D:\your-project
claude
```

### 5.2 常用命令

| 命令 | 说明 |
|------|------|
| `/help` | 查看所有可用命令 |
| `/exit` | 退出 Claude CLI |
| `/clear` | 清空当前对话上下文 |
| `/theme` | 修改终端显示主题 |

### 5.3 更新管理

`autoUpdatesChannel: "latest"` 会自动跟随官方更新。如需稳定版，将配置改为 `"stable"`。

---

## 六、常见问题排查

| 问题 | 解决方法 |
|------|---------|
| 安装脚本超时 | 1. 确认代理已启用且端口正确；2. 重新执行 TLS 1.2 启用命令 |
| `claude` 不是内部命令 | 检查 PATH 配置，或使用全路径 `$env:USERPROFILE\.local\bin\claude.exe` |
| 调用模型失败 | 1. 检查 `ANTHROPIC_BASE_URL` 和 API Key 是否正确；2. 确认 DeepSeek 账户余额充足；3. 直连失败时在 `env` 中添加代理配置 |
| 终端 `curl` 报错 | 使用 `curl.exe` 代替内置 `curl` 别名，或使用 `Invoke-WebRequest` |
| 代理正常但安装失败 | 更换代理节点，优先使用新加坡节点，避免中国香港和台湾节点 |

---

## 参考资源

- Claude Code 官方安装脚本：`https://claude.ai/install.ps1`
- DeepSeek API 文档：`https://api-docs.deepseek.com/`
- 原始资料来源：`https://www.doubao.com/thread/w43e554f3a51111bd`
