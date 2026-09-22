# WSL2 Mirrored 网络模式 + SSH 密钥认证改造方案

> 日期：2026-09-17
> 背景：WSL2 NAT 模式下 IP 每次重启变化，远程开发工具需要固定地址 + 密钥认证

---

## 适用场景

- 仅物理机访问 WSL 虚拟机，无外部第三方机器访问需求
- MySQL 服务在物理机上，WSL 内只有操作 MySQL 的客户端服务
- Redis 已迁移到 WSL 上，暂无外部设备访问需求
- 远程开发工具（如 JetBrains Gateway / Remote Development）通过 SSH 连接 WSL

---

## 改造前后对比

| 项目 | 改造前 | 改造后 |
|------|--------|--------|
| WSL IP | 每次重启变化（如 192.168.9.33） | 不再需要，用 `127.0.0.1` |
| SSH 端口 | 22（NAT 模式） | 2222（mirrored 模式，避免与 Windows SSH 冲突） |
| 认证方式 | 密码 | SSH 密钥免密 |
| Redis 访问 | 需要知道 WSL 动态 IP | `127.0.0.1:6379` 固定不变 |
| MySQL 访问 | 不受影响 | 不受影响（MySQL 在物理机） |

---

## 第一步：开启 Mirrored 网络模式

在 Windows PowerShell 中执行：

```powershell
# 备份原配置
if (Test-Path "$env:USERPROFILE\.wslconfig") {
    Copy-Item "$env:USERPROFILE\.wslconfig" "$env:USERPROFILE\.wslconfig.bak"
}

# 写入 mirrored 配置
@"
[wsl2]
networkingMode=mirrored
"@ | Set-Content "$env:USERPROFILE\.wslconfig" -Encoding UTF8

# 关闭并重启 WSL
wsl --shutdown
```

然后重新打开 WSL 终端。

### 验证

```powershell
# 在 WSL 内启动一个测试服务
python3 -m http.server 8080 &

# 在 Windows 浏览器访问 http://localhost:8080，能打开即成功
```

---

## 第二步：修改 WSL 内 SSH 端口为 2222

进入 WSL 终端执行：

```bash
# 1. 修改 SSH 配置
sudo sed -i 's/^#*Port .*/Port 2222/' /etc/ssh/sshd_config

# 如果没有 Port 行，手动追加
grep -q "^Port 2222" /etc/ssh/sshd_config || echo "Port 2222" | sudo tee -a /etc/ssh/sshd_config

# 2. 重启 SSH 服务
sudo service ssh restart

# 3. 确认监听在 2222
sudo ss -tlnp | grep 2222
```

预期输出：
```
LISTEN  0  128  0.0.0.0:2222  0.0.0.0:*  users:(("sshd",...))
LISTEN  0  128  [::]:2222     [::]:*     users:(("sshd",...))
```

---

## 第三步：生成 SSH 密钥并配置免密登录

回到 Windows PowerShell：

```powershell
# 1. 生成密钥对（如果已有可以跳过）
ssh-keygen -t ed25519 -C "wsl-mirrored" -f "$env:USERPROFILE\.ssh\id_ed25519_wsl" -N ""

# 2. 把公钥写入 WSL（mirrored 模式下 localhost 直达）
type "$env:USERPROFILE\.ssh\id_ed25519_wsl.pub" | wsl bash -c "
    mkdir -p ~/.ssh && chmod 700 ~/.ssh
    cat >> ~/.ssh/authorized_keys
    chmod 600 ~/.ssh/authorized_keys
"

# 3. 验证免密登录
ssh -i "$env:USERPROFILE\.ssh\id_ed25519_wsl" -p 2222 arrowinknee@localhost
```

能直接进 WSL 终端即成功。输入 `exit` 退出。

---

## 第四步：配置远程开发工具

在工具的 WSL 主机配置中修改：

| 字段 | 值 |
|------|-----|
| 地址 | `127.0.0.1` |
| 端口 | `2222` |
| 用户 | `arrowinknee` |
| 密码 | **留空**（走 key 认证） |

如果工具有私钥路径选项，填：`C:\Users\arrowinknee\.ssh\id_ed25519_wsl`

---

## 第五步：验证 Redis 连通性

Redis 在 WSL 上，mirrored 模式下 Windows 直接用 localhost 访问：

```powershell
# 在 Windows PowerShell 中测试
redis-cli -h 127.0.0.1 -p 6379 ping
```

返回 `PONG` 即正常。物理机上操作 MySQL 的服务连 WSL 里的 Redis 也用 `127.0.0.1:6379`，无需额外配置。

---

## 注意事项

### Mirrored 模式的影响

- **端口冲突**：WSL 里监听的服务会直接暴露到 Windows localhost。如果 WSL 和 Windows 有同名服务监听同一端口，先启动的占端口
- **防火墙**：WSL 流量走 Windows 防火墙，规则需重新考虑
- **外部访问**：局域网其他机器无法直接通过 Windows IP 访问 WSL 服务（本场景不涉及）
- **IPv6**：mirrored 默认启用 IPv6，某些只绑 IPv4 的服务可能异常

### 不受影响的

- WSL 访问外网（下载、curl 等）
- Windows 访问 WSL 服务（通过 localhost）
- 文件共享（`\\wsl$`）
- VS Code Remote-WSL 等直接走 WSL 协议的工具

---

## 一键还原

如果出问题，执行以下命令恢复到 NAT 模式：

```powershell
# 删除 mirrored 配置
Remove-Item "$env:USERPROFILE\.wslconfig" -ErrorAction SilentlyContinue

# 或者恢复备份
if (Test-Path "$env:USERPROFILE\.wslconfig.bak") {
    Copy-Item "$env:USERPROFILE\.wslconfig.bak" "$env:USERPROFILE\.wslconfig"
}

wsl --shutdown
```

`.wslconfig` 只是文本配置文件，不影响 WSL 内任何数据，还原零风险。