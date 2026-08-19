好的，既然你已决定使用 **Ubuntu 26.04 LTS**，下面是你从零开始在 WSL2 上搭建开发环境的**最终完整方案**。整个过程分为六个阶段，按顺序操作即可。

---

## 📋 方案总览

| 阶段 | 内容 | 预计时间 |
| :--- | :--- | :--- |
| **一、安装 WSL2 + Ubuntu 26.04** | 开启功能、安装系统、验证版本 | 10 分钟 |
| **二、系统初始化** | 更新系统、安装基础工具、JDK、SSH | 10 分钟 |
| **三、安装 Docker（含代理配置）** | 安装 Docker、用户组、代理 | 10 分钟 |
| **四、导出黄金镜像（快照）** | 备份干净的初始化环境 | 3 分钟 |
| **五、配置网络与代理** | WSL2 专用网络配置 | 5 分钟 |
| **六、（可选）DeepSeek Harness** | 安装 AI 工具 | 5 分钟 |

---

## 🚀 阶段一：安装 WSL2 + Ubuntu 26.04

> **⚠️ 重要**：必须分步操作！如果 `wsl --install` 后不重启就直接安装 Ubuntu，会导致 Ubuntu 安装失败。

### 1.1 以管理员身份打开 PowerShell

按 `Win + X`，选择 **"终端(管理员)"** 或 **"Windows PowerShell (管理员)"**。

### 1.2 第一步：安装并启用 WSL2

```powershell
wsl --install
```

这条命令会自动完成：
- 启用"适用于 Linux 的 Windows 子系统"
- 启用"虚拟机平台"
- 下载并安装最新 Linux 内核
- 将 WSL2 设为默认版本

**🔴 安装完成后，必须重启电脑！** 重启是为了让"虚拟机平台"等内核级变更生效。

### 1.3 第二步：（可选）验证 WSL 状态

重启后，重新以管理员身份打开 PowerShell，确认 WSL 2 已成为默认版本：

```powershell
wsl --set-default-version 2
```

### 1.4 第三步：安装 Ubuntu 26.04 LTS

系统已经准备好安装具体的发行版了。运行以下命令来安装 Ubuntu 26.04：

```powershell
wsl --install -d Ubuntu-26.04
```

这次安装应该会顺利进行。

### 1.5 首次启动 Ubuntu 26.04

- 在开始菜单搜索 **"Ubuntu 26.04 LTS"** 并打开
- 等待初始化（约 1-2 分钟）
- 设置你的 **用户名** 和 **密码**（记住它，后面要用）

### 1.6 验证安装

在 PowerShell 中运行：
```powershell
wsl -l -v
```

确保输出中 `Ubuntu-26.04` 的 `VERSION` 列为 `2`。

---

## ⚙️ 阶段二：系统初始化

打开你的 Ubuntu 26.04 终端，按顺序执行以下命令：

### 2.1 更新系统

```bash
sudo apt update && sudo apt upgrade -y
```

### 2.2 安装基础工具

```bash
sudo apt install -y vim net-tools openssh-server curl wget git build-essential ca-certificates gnupg lsb-release
```

**说明**：26.04 的软件源默认包含最新工具链（GCC 16、Rust 工具链等），基础工具安装方式与之前一致。

### 2.3 安装 OpenJDK

Ubuntu 26.04 默认仓库包含 OpenJDK 25，你也可以安装其他版本：

```bash
# 安装默认 JDK（OpenJDK 8）
sudo apt install openjdk-8-jdk

#或安装 java 25（最新）
#sudo apt install -y openjdk-25-jdk

# 或安装 Java 17（更稳定）
# sudo apt install -y openjdk-17-jdk

# 或安装 Java 21
# sudo apt install -y openjdk-21-jdk

java -version
```

**提示**：如果你是 Java 8 用户，建议升级到 17 或 21，26.04 的软件源对旧版本支持可能有限。

### 2.4 启动 SSH 服务（可选）

```bash
sudo service ssh start
ps -e | grep ssh
```

> WSL2 里不需要 `ufw allow 22`，防火墙由 Windows 统一管理。

---

## 🐳 阶段三：安装 Docker（含代理配置）

### 3.1 卸载旧版 Docker（如果有）

```bash
sudo apt-get remove -y docker docker-engine docker-ce docker.io 2>/dev/null
```

### 3.2 添加 Docker 官方仓库

```bash
# 添加 Docker 的 GPG 密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 添加 Docker 稳定版仓库（Ubuntu 26.04 的代号是 "plucky"）
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu plucky stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

**注意**：Ubuntu 26.04 的版本代号是 `plucky`，不要填成旧版本的代号。

### 3.3 安装 Docker CE

```bash
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 3.4 启动 Docker 服务（WSL2 用 service 命令）

```bash
sudo service docker start
```

### 3.5 将当前用户加入 docker 组

```bash
sudo groupadd docker 2>/dev/null
sudo usermod -aG docker $USER
```

**⚠️ 重要**：执行完这一步后，需要**关闭并重新打开** Ubuntu 终端，组权限才会生效。

### 3.6 验证 Docker

重新打开终端后：
```bash
docker run hello-world
```

如果能正常输出 Hello World 信息，说明 Docker 安装成功。

### 3.7 配置 Docker 代理（WSL2 专用）

**方案 A：使用 Windows 11 镜像网络模式（最推荐，一劳永逸）**

在 Windows 的 `C:\Users\你的用户名\.wslconfig` 文件中添加：

```ini
[wsl2]
networkingMode=mirrored
dnsTunneling=true
firewall=true
autoProxy=true
```

然后在 PowerShell 中重启 WSL：
```powershell
wsl --shutdown
```

重新打开 Ubuntu 终端，Docker 会自动继承 Windows 的代理设置。

**方案 B：手动配置代理（备选）**

```bash
# 获取物理机 IP
hostip=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}')

# 创建代理配置（假设你的代理端口是 7890，按实际情况修改）
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/proxy.conf > /dev/null <<EOF
[Service]
Environment="HTTP_PROXY=http://${hostip}:7890"
Environment="HTTPS_PROXY=http://${hostip}:7890"
EOF

# 重启 Docker
sudo systemctl daemon-reload
sudo service docker restart
```

### 3.8 设置 Docker 开机自启

WSL2 没有 systemd，但可以通过配置 `~/.bashrc` 实现登录时自动启动：

```bash
echo 'sudo service docker start 2>/dev/null || true' >> ~/.bashrc
```

---

## 📸 阶段四：导出黄金镜像（快照）

### 4.1 创建快照目录

在 **PowerShell** 中运行：
```powershell
New-Item -ItemType Directory -Force D:\WSL-Snapshots
```

### 4.2 导出黄金镜像

```powershell
wsl --shutdown
wsl --export Ubuntu-26.04 D:\WSL-Snapshots\Ubuntu-26.04-init-2026-08-19.tar
```

这个 `.tar` 文件就是你的**黄金镜像**，包含了：
- Ubuntu 26.04 LTS 基础系统
- 所有基础工具（vim、gcc、git 等）
- OpenJDK 25
- Docker CE（含用户组配置）
- SSH 服务

**文件大小约 3-5 GB。**

### 4.3 恢复方法（如果以后搞崩了）

```powershell
wsl --import Ubuntu-26.04-Restore D:\WSL-Restore D:\WSL-Snapshots\Ubuntu-26.04-init-2026-08-19.tar
```

---

## 🌐 阶段五：网络与代理配置（补充）

除了 Docker 代理，如果你希望 WSL2 里的终端命令（`curl`、`wget`、`apt` 等）也走代理，可以在 `~/.bashrc` 中添加：

```bash
# 获取 Windows 主机 IP
export hostip=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}')

# 设置代理（端口按你的代理软件实际端口修改，如 Clash 默认 7890）
export http_proxy="http://${hostip}:7890"
export https_proxy="http://${hostip}:7890"
export all_proxy="socks5://${hostip}:7891"  # SOCKS5 代理（可选）
```

然后执行 `source ~/.bashrc` 使其生效。

---

## 🤖 阶段六：（可选）安装 DeepSeek Harness

### 6.1 安装 Node.js

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
node -v
```

### 6.2 启动 DeepSeek Harness

```bash
npx @deepseek-ai/dsh web
```

启动成功后，在 Windows 浏览器访问 `http://127.0.0.1:3080`，填入你的 DeepSeek API Key 即可使用。

---

## 📝 你的新 VM 手册（WSL2 + Ubuntu 26.04 专用版）

把以下内容保存为 `wsl2-ubuntu-26.04-init.md`，以后重装直接用：

```markdown
# WSL2 Ubuntu 26.04 LTS 初始化手册

## 系统更新
sudo apt update && sudo apt upgrade -y

## 基础工具
sudo apt install -y vim net-tools openssh-server curl wget git build-essential ca-certificates gnupg lsb-release

## JDK
sudo apt install -y openjdk-25-jdk
java -version

## SSH（可选）
sudo service ssh start

## Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu plucky stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo service docker start
sudo usermod -aG docker $USER
# 重启终端后验证
docker run hello-world

## Docker 代理（WSL2 专用）
# 方案一：使用 .wslconfig 镜像网络（推荐）
# 方案二：手动配置代理
hostip=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}')
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/proxy.conf > /dev/null <<EOF
[Service]
Environment="HTTP_PROXY=http://${hostip}:7890"
Environment="HTTPS_PROXY=http://${hostip}:7890"
EOF
sudo service docker restart

## 终端代理
echo 'export hostip=$(cat /etc/resolv.conf | grep nameserver | awk "{print \$2}")' >> ~/.bashrc
echo 'export http_proxy="http://${hostip}:7890"' >> ~/.bashrc
echo 'export https_proxy="http://${hostip}:7890"' >> ~/.bashrc

## 快照
# New-Item -ItemType Directory -Force D:\WSL-Snapshots
# wsl --shutdown
# wsl --export Ubuntu-26.04 D:\WSL-Snapshots\Ubuntu-26.04-init-2026-08-19.tar

## 恢复
# wsl --import Ubuntu-26.04-Restore D:\WSL-Restore D:\WSL-Snapshots\Ubuntu-26.04-init-2026-08-19.tar
```

---

## 🎯 最终总结

| 项目 | 方案 |
| :--- | :--- |
| **系统** | Windows 11 + WSL2 |
| **发行版** | Ubuntu 26.04 LTS（代号 plucky） |
| **内核** | Linux 7.0 |
| **JDK** | OpenJDK 25（或 17/21） |
| **Docker** | Docker CE 最新版 |
| **代理** | 镜像网络模式（Win11 专属）或手动配置 |
| **备份** | `wsl --export` 导出 `.tar` 快照 |

**你现在拥有了一份完整的、从零到一的开发环境搭建方案。** 按这个顺序操作，不会有遗漏。如果在某一步卡住了，随时回来问我。😎
</final>