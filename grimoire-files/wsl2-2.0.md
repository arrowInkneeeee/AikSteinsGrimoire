
# WSL2 + Ubuntu 26.04 LTS 开发环境搭建方案 v2.2

> **适用系统**：Windows 11（22H2 或更高版本）  
> **目标发行版**：Ubuntu 26.04 LTS（代号 *Resolute Raccoon*）  
> **版本**：2.2  
> **更新日期**：2026-08-19  
> **设计理念**：AI 接管重复劳动，人类保留决策与知识备份

---

## 📋 方案概述

本方案的目标是：**在最短时间内让 Codex 上线，然后用 Codex 接管后续所有环境配置工作。**

| 阶段 | 内容 | 操作者 |
| :--- | :--- | :--- |
| **一、安装 WSL2 + Ubuntu** | 分步安装，避免报错 | 手动 |
| **二、系统基础更新** | 仅必要的系统更新 | 手动 |
| **三、配置代理** | WSL2 代理开关函数 | 手动 |
| **四、安装 Codex** | nvm → Node.js → Codex CLI + auth.json | 手动 |
| **五、交给 Codex** | AI 接管后续配置 | Codex |
| **六、知识备份** | 原始手动命令清单（学习/参考/应急用） | 阅读备份 |

---

## 🚀 阶段一：安装 WSL2 + Ubuntu 26.04

### 1.1 以管理员身份打开 PowerShell

按 `Win + X`，选择 **“终端(管理员)”**。

### 1.2 第一步：安装 WSL 功能

```powershell
wsl --install
```

执行后，看到 `请求的操作成功。直到重新启动系统前更改将不会生效。` 的提示。

### 1.3 第二步：重启电脑

**必须重启**，让 VirtualMachinePlatform 生效。

### 1.4 第三步：安装 Ubuntu 26.04

重启后，以管理员身份打开 PowerShell：

```powershell
wsl --install -d Ubuntu-26.04
```

### 1.5 第四步：首次启动 Ubuntu 26.04

- 在开始菜单搜索 **“Ubuntu 26.04 LTS”** 并打开
- 等待初始化（约 1～2 分钟）
- 设置 **用户名** 和 **密码**（请牢记）

### 1.6 验证安装

```powershell
wsl -l -v
```

确保 `Ubuntu-26.04` 的 `VERSION` 列为 `2`。

---

## ⚙️ 阶段二：系统基础更新（仅必要部分）

打开 Ubuntu 26.04 终端，执行系统更新（这是 Codex 上线前唯一需要手动执行的系统操作）：

```bash
sudo apt update && sudo apt upgrade -y
```

> **说明**：此时**不需要**安装 JDK、Docker、vim 等任何工具，这些交给 Codex 完成。只需确保系统是最新状态即可。

---

## 🌐 阶段三：配置代理（核心难点，必须手动完成）

> **⚠️ 重要**：代理配置是 Codex 能否正常安装和运行的关键。这一关必须自己先过。

### 3.1 确认物理机 IP

在 WSL 终端中执行：

```bash
cat /etc/resolv.conf | grep nameserver | awk '{print $2}'
```

但此方法获取的是 WSL2 虚拟 DNS（如 `10.255.255.254`），代理软件通常不监听该地址。**实际应使用物理机的内网 IP**。

在 Windows PowerShell 中执行：

```powershell
ipconfig | findstr "IPv4"
```

记下你的物理机内网 IP（如 `192.168.9.33`）和代理端口（如 `7892`）。

### 3.2 代理开关函数

编辑 `~/.bashrc`：

```bash
nano ~/.bashrc
```

在文件末尾添加以下内容（**将 IP 和端口替换为你的实际值**）：

```bash
# ===== 代理开关函数 =====
function proxy-on() {
    export hostip="192.168.9.33"   # 改为你的物理机内网 IP
    export http_proxy="http://${hostip}:7892"
    export https_proxy="http://${hostip}:7892"
    echo "✅ 代理已开启 (端口 7892，主机IP: ${hostip})"
}

function proxy-off() {
    unset http_proxy
    unset https_proxy
    echo "❌ 代理已关闭"
}

function proxy-status() {
    if [ -n "$http_proxy" ]; then
        echo "✅ 当前代理: $http_proxy"
    else
        echo "❌ 当前未启用代理"
    fi
}
```

保存后执行：

```bash
source ~/.bashrc
```

### 3.3 验证代理是否生效

```bash
proxy-on
curl -I https://github.com
```

如果返回 `HTTP/2 200`，说明代理配置成功。

> **⚠️ 如果 `Connection refused`**：
> 1. 检查 Windows 代理软件是否开启 **“允许局域网连接 (Allow LAN)”**
> 2. 确认物理机 IP 和端口是否正确
> 3. 用 `nc -zv 192.168.9.33 7892` 测试端口连通性

---

## 📦 阶段四：安装 Codex

### 4.1 安装 nvm（Node Version Manager）

```bash
proxy-on
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.4/install.sh | bash
source ~/.bashrc
nvm install 22
nvm alias default 22
node -v  # 验证
```

### 4.2 安装 Codex CLI

```bash
npm install -g @openai/codex
codex --version  # 验证
```

如果安装速度慢，可切换镜像源：

```bash
npm config set registry https://registry.npmmirror.com
```

### 4.3 迁移 `auth.json`（从 Windows 到 WSL）

> **⚠️ 注意**：Codex CLI 默认查找位置是 `~/.codex/auth.json`（**带点的隐藏目录**）。

```bash
mkdir -p ~/.codex
cp /mnt/c/Users/你的Windows用户名/.codex/auth.json ~/.codex/auth.json
chmod 600 ~/.codex/auth.json
```

### 4.4 验证 Codex 是否生效

```bash
codex --version
```

输出版本号且无登录提示，即表示认证成功。

---

## 🤖 阶段五：交给 Codex（AI 接管）

**从现在开始，所有后续工作都由 Codex 完成。**

进入你的项目目录（或任意目录），启动 Codex：

```bash
cd /mnt/d/JeBrainsWorkspace/AikSteinsGrimoire  # 以你的实际项目路径为例
codex
```

选择 `Yes, continue` 信任当前目录，然后开始用自然语言指挥 Codex 完成所有环境配置：

### 你可以这样对 Codex 说：

```
帮我安装 Java 8 开发环境
```

```
帮我安装 Docker CE，并配置好用户组
```

```
安装 vim、curl、wget、git 等基础开发工具
```

```
帮我配置 SSH 服务
```

```
安装 Maven 和 Gradle
```

```
把 apt 源换成国内镜像，加快下载速度
```

```
检查一下当前环境还有哪些开发工具缺失，帮我补全
```

---

## 📚 阶段六：知识备份——原始手动命令清单

> **本章目的**：
> - 作为知识备份，让你理解每个工具是如何手动安装的
> - 作为应急参考，当 Codex 不可用时（如无网络、API 限流等），你可以手动执行
> - 作为学习材料，帮助你理解 Linux 环境配置的底层逻辑
> - **AI 是加速器，不是拐杖。保持思考，保持掌控。**

---

### 6.1 系统更新与基础工具

```bash
# 系统更新
sudo apt update && sudo apt upgrade -y

# 基础开发工具
sudo apt install -y vim net-tools openssh-server curl wget git build-essential ca-certificates gnupg lsb-release
```

---

### 6.2 Java 开发环境

```bash
# Java 8（主力，适用于绝大多数生产项目）
sudo apt install -y openjdk-8-jdk
java -version

# Java 17（Spring Boot 3.x 推荐）
sudo apt install -y openjdk-17-jdk

# Java 21（LTS 最新版）
sudo apt install -y openjdk-21-jdk

# Java 25（Ubuntu 26.04 默认，适合尝鲜）
sudo apt install -y openjdk-25-jdk
```

---

### 6.3 Docker CE

```bash
# 卸载旧版本
sudo apt-get remove -y docker docker-engine docker-ce docker.io 2>/dev/null

# 添加 Docker 官方 GPG 密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 添加 Docker 仓库（自动识别 Ubuntu 26.04 代号 plucky）
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker CE
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 启动 Docker 服务（WSL2 用 service 命令）
sudo service docker start

# 将当前用户加入 docker 组（免 sudo）
sudo groupadd docker 2>/dev/null
sudo usermod -aG docker $USER

# 注意：执行完 usermod 后，需要关闭并重新打开终端使组权限生效
```

---

### 6.4 SSH 服务

```bash
# 安装 SSH 服务
sudo apt install -y openssh-server

# 启动 SSH
sudo service ssh start

# 检查 SSH 是否运行
ps -e | grep ssh
```

> WSL2 中无需配置 `ufw` 防火墙，由 Windows Defender 统一管理。

---

### 6.5 Maven 与 Gradle

```bash
# Maven
sudo apt install -y maven
mvn -version

# Gradle
sudo apt install -y gradle
gradle -version
```

---

### 6.6 其他常用工具

```bash
# 网络诊断工具
sudo apt install -y net-tools dnsutils iputils-ping

# 文本处理工具
sudo apt install -y jq tree htop

# 数据库客户端（如需要）
sudo apt install -y mysql-client postgresql-client redis-tools

# 版本控制增强
sudo apt install -y tig
```

---

### 6.7 Docker 代理配置（WSL2 专用）

```bash
# 获取物理机 IP
hostip=$(cat /etc/resolv.conf | grep nameserver | awk '{print $2}')

# 创建代理配置文件
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/proxy.conf > /dev/null <<EOF
[Service]
Environment="HTTP_PROXY=http://${hostip}:7892"
Environment="HTTPS_PROXY=http://${hostip}:7892"
EOF

# 重启 Docker
sudo systemctl daemon-reload
sudo service docker restart
```

---

### 6.8 一键初始化脚本（备份）

如果你希望在任何情况下都能快速重建环境，可以将以下内容保存为 `init.sh`：

```bash
#!/bin/bash
set -e

echo ">>> 开始初始化 Ubuntu 26.04 开发环境..."

# 更新系统
sudo apt update && sudo apt upgrade -y

# 基础工具
sudo apt install -y vim net-tools openssh-server curl wget git build-essential ca-certificates gnupg lsb-release

# Java 8
sudo apt install -y openjdk-8-jdk

# SSH
sudo service ssh start

# Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo service docker start
sudo usermod -aG docker $USER

# Maven & Gradle
sudo apt install -y maven gradle

echo ">>> 初始化完成！请关闭并重新打开终端使 docker 组权限生效。"
```

执行方式：

```bash
chmod +x init.sh
./init.sh
```

---

## 🗂️ 附录：踩坑记录速查

| 问题 | 解决方案 |
| :--- | :--- |
| `wsl --install -d Ubuntu-26.04` 报错 | 拆分为 `wsl --install` → 重启 → `wsl --install -d Ubuntu-26.04` |
| `Connection refused` | 开启代理软件“允许局域网连接”，使用物理机 IP 而非 `127.0.0.1` |
| `curl 28 Failed to connect to github.com` | 先执行 `proxy-on` 开启代理 |
| `auth.json` 找不到 | 确保文件位于 `~/.codex/auth.json`（带点的隐藏目录） |
| Codex 提示未登录 | 检查 `~/.codex/auth.json` 是否存在且有效 |
| `sudo: systemctl: command not found` | WSL2 默认无 systemd，用 `sudo service docker start` 替代 |
| `docker: permission denied` | 执行 `sudo usermod -aG docker $USER` 后**重新打开终端** |

---

## 🎯 总结

| 阶段 | 操作者 | 耗时 |
| :--- | :--- | :--- |
| 安装 WSL2 + Ubuntu | 手动 | ~15 分钟 |
| 配置代理 | 手动 | ~5 分钟 |
| 安装 Codex | 手动 | ~10 分钟 |
| **所有环境配置（日常）** | **Codex（AI）** | ~5 分钟（对话时间） |
| **应急/无网络/学习** | **手动（阶段六命令）** | 随时可查 |

---

**v2.2 设计理念：**

> **AI 是副驾驶，不是自动驾驶。**  
> 让 AI 承担重复劳动，让人类保留决策权、理解力和知识备份。  
> 这才是 AI 时代开发者该有的状态。😎
