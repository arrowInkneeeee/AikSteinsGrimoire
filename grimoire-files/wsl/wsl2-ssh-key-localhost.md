# WSL2 NAT 模式 + SSH 密钥认证（Windows 本机工具连接）

> 日期：2026-09-17  
> 适用范围：远程开发工具运行在同一台 Windows 物理机，需稳定连接 WSL。  
> 方案选择：保留 WSL2 默认 NAT 网络，使用 Windows 到 WSL 的 localhost 转发；不启用 mirrored 网络模式。

---

## 目标与边界

本方案解决 Windows 上的 JetBrains Gateway、Remote Development 等 SSH 工具连接 WSL 时的两个问题：连接地址固定，以及不再依赖 Linux 密码登录。

工具统一使用以下连接信息：

| 字段 | 值 |
| --- | --- |
| 主机 | `127.0.0.1` |
| 端口 | `22222` |
| 用户 | `arrowinknee` |
| 认证 | Windows 私钥 `C:\\Users\\arrowinknee\\.ssh\\id_ed25519_wsl` |

不切换 mirrored 的原因：Windows 本机访问 WSL 已可通过 `localhost` 转发，不需要依赖会变化的 WSL IP；保留 NAT 可避免影响现有代理、Redis、防火墙和 VPN 行为。

本方案不适用于局域网其他设备连接 WSL。此类需求应单独设计 Windows 端口转发与防火墙规则，不能直接套用本文。

---

## 密钥登录的工作方式

密钥是一对配套文件：

- **私钥**：保存在 Windows 的 `C:\\Users\\arrowinknee\\.ssh\\id_ed25519_wsl`。它相当于登录凭证，绝不发送给服务器、绝不提交到 Git、绝不通过聊天工具传输。
- **公钥**：写入 WSL 用户的 `~/.ssh/authorized_keys`。它只用于识别对应私钥，可以存放在服务器上。

工具连接时选择私钥文件即可：SSH 客户端用私钥完成认证，WSL 用已登记的公钥校验。因此，关闭密码登录后，工具仍可正常连接；只是没有私钥的客户端会被拒绝。

若私钥设置了口令，工具首次使用时会要求输入“私钥口令”，这不是 Linux 用户密码。通过 Windows OpenSSH Agent 可减少重复输入。本次使用独立的无口令私钥，凭 Windows 用户账户访问控制保护该文件。

---

## 实施前检查

在 Windows PowerShell 中执行：

```powershell
wsl --status
Test-NetConnection 127.0.0.1 -Port 22222
Get-NetTCPConnection -State Listen -LocalPort 22222 -ErrorAction SilentlyContinue
```

端口 `22222` 必须未被 Windows 程序占用。当前机器的 `2222` 已由 VMware NAT 占用，不能使用。若 `22222` 未来被占用，选择未占用的高位端口，并将全文端口保持一致。

---

## 实施步骤

### 1. 在 Windows 创建专用密钥

```powershell
ssh-keygen -t ed25519 -C "wsl-localhost" -f "$env:USERPROFILE\\.ssh\\id_ed25519_wsl"
```

命令提示设置私钥口令时，可设置一个易于管理的口令；若确认仅当前 Windows 账户可访问且工具无法处理口令，再留空。密钥已存在时不要覆盖，应先确认其用途。

### 2. 将公钥登记到 WSL

```powershell
wsl bash -lc "mkdir -p ~/.ssh && chmod 700 ~/.ssh"
type "$env:USERPROFILE\\.ssh\\id_ed25519_wsl.pub" | wsl bash -lc "cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
```

这段命令可能重复追加公钥；重复项不影响认证，但建议实施后清理为一条。

### 3. 配置 SSH 监听端口并保留密码兜底

在 WSL 中新增 `/etc/ssh/sshd_config.d/10-wsl-localhost.conf`：

```text
Port 22222
PubkeyAuthentication yes
PasswordAuthentication yes
KbdInteractiveAuthentication no
```

Ubuntu 当前使用 systemd socket activation，因此还须新增 `/etc/systemd/system/ssh.socket.d/10-wsl-localhost.conf`：

```ini
[Socket]
ListenStream=
ListenStream=22222
```

校验并重启 socket：

```bash
sudo sshd -t
sudo systemctl daemon-reload
sudo systemctl restart ssh.socket
sudo ss -ltnp | grep ':22222'
```

### 4. 从 Windows 验证密钥登录

```powershell
ssh -o PreferredAuthentications=publickey -o PasswordAuthentication=no `
  -i "$env:USERPROFILE\\.ssh\\id_ed25519_wsl" -p 22222 arrowinknee@127.0.0.1
```

必须确认该命令在**没有输入 Linux 密码**的情况下登录成功，才可继续下一步。保留当前已登录的 WSL 终端，不要在验证前关闭它。

### 5. 可选：关闭密码登录

将配置文件中的下面一行改为：

```text
PasswordAuthentication no
```

再次运行：

```bash
sudo sshd -t && sudo systemctl daemon-reload && sudo systemctl restart ssh.socket
```

然后从新的 Windows PowerShell 窗口再次执行步骤 4 的密钥登录命令。若验证失败，仍可使用保留的 WSL 终端把配置改回 `PasswordAuthentication yes` 并重启 `ssh.socket`。

### 6. 配置远程开发工具

在工具的新 SSH 连接中填入：

| 工具字段 | 填写内容 |
| --- | --- |
| Host / 地址 | `127.0.0.1` |
| Port / 端口 | `22222` |
| User / 用户 | `arrowinknee` |
| Authentication type | Key pair / OpenSSH key |
| Private key file | `C:\\Users\\arrowinknee\\.ssh\\id_ed25519_wsl` |
| Password | 留空 |

JetBrains Gateway 等工具若提供“使用 SSH Agent”，可选用；其本质仍是让 Agent 代管同一把私钥。工具绝不需要 WSL 用户密码。

---

## 验收标准

以下全部满足才算完成：

1. Windows 执行 `ssh -i ... -p 22222 arrowinknee@127.0.0.1` 可进入 WSL。
2. 强制仅公钥认证的验证命令成功，且未输入 Linux 密码。
3. 远程开发工具可建立 SSH 连接并打开 WSL 项目目录。
4. `proxy-on` 的现有地址和行为未改变，代理联网仍可按原方式使用。
5. 如已关闭密码认证，执行 `sudo sshd -T | grep passwordauthentication` 返回 `passwordauthentication no`。

---

## 回滚

在仍可打开的 WSL 终端中执行：

```bash
sudo rm -f /etc/ssh/sshd_config.d/10-wsl-localhost.conf
sudo rm -f /etc/systemd/system/ssh.socket.d/10-wsl-localhost.conf
sudo sshd -t && sudo systemctl daemon-reload && sudo systemctl restart ssh.socket
```

这会恢复发行版默认 SSH 设置。Windows 的专用私钥可保留，以便后续重新启用；若确认不再使用，再手动删除私钥及同名 `.pub` 文件。

---

## 代理说明

现有 `proxy-on` 将代理指向 `192.168.9.33:7892`。本方案不改变 WSL 网络模式，故不修改此配置。若未来单独启用 mirrored 模式，应先确认 Windows 代理在 `127.0.0.1:7892` 可访问，再将代理脚本改为 localhost，并复测 Git、npm、Codex 等联网命令。
