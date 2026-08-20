# WSL Ubuntu 26.04 初始化快照导入指南

## 适用场景

在另一台 Windows 电脑上导入 `Ubuntu-26.04-init.tar`，快速得到与当前 WSL 基本一致的 Ubuntu 26.04 开发环境。

## 一、准备 WSL 2

在目标电脑使用管理员 PowerShell 执行：

```powershell
wsl --install
```

按提示重启电脑。重启后确认 WSL 版本：

```powershell
wsl --set-default-version 2
wsl --status
```

## 二、导入快照

假设快照已经放到：

```text
D:\WSL\WSL-snapshots\Ubuntu-26.04-init.tar
```

执行：

```powershell
New-Item -ItemType Directory -Force -Path "D:\WSL\Instances\Ubuntu-26.04"

wsl --import Ubuntu-26.04 `
  "D:\WSL\Instances\Ubuntu-26.04" `
  "D:\WSL\WSL-snapshots\Ubuntu-26.04-init.tar" `
  --version 2
```

检查发行版：

```powershell
wsl --list --verbose
```

确认 `Ubuntu-26.04` 的 `VERSION` 为 `2`。

## 三、启动并确认默认用户

启动发行版：

```powershell
wsl -d Ubuntu-26.04
```

在 WSL 中检查当前用户：

```bash
whoami
```

快照中已经包含 `/etc/wsl.conf` 的默认用户设置。如果没有进入 `arrowinknee`，检查：

```bash
cat /etc/wsl.conf
```

应包含：

```ini
[user]
default=arrowinknee
```

修改后，在 PowerShell 中重启 WSL：

```powershell
wsl --shutdown
wsl -d Ubuntu-26.04
```

## 四、设置默认发行版

在 PowerShell 中执行：

```powershell
wsl --set-default Ubuntu-26.04
```

验证：

```powershell
wsl --list --verbose
```

## 五、验证初始化环境

在 WSL 中执行：

```bash
java -version
node --version
npm --version
codex --version
claude --version
gitnexus --version 2>/dev/null || true
dsh --version
redis-server --version
nginx -v
systemctl is-enabled redis-server
systemctl is-enabled nginx
```

需要时启动 Nacos：

```bash
sudo systemctl start nacos-wsl
sudo systemctl status nacos-wsl
```

当前快照中的 Nacos 默认是关闭自启且停止状态。

## 六、同步 Windows 侧文件

快照不会包含 Windows 文件系统中的内容。若要复用当前服务环境，还需要通过其他方式同步：

```text
D:\service
```

特别是当前 Nginx 配置引用的前端资源路径：

```text
/mnt/d/service/nginx-1.31.0/porject/...
```

如果目标电脑没有对应的 `D:\service` 内容，需要修改 Nginx 配置中的静态资源路径。

## 七、传输安全

该快照可能包含 Codex、Claude、DeepSeek、Git 等配置和凭证。只通过私有百度网盘或其他可信渠道传输，不要设置公开分享链接。传输完成后建议删除临时副本，并按需要轮换 API Key。
