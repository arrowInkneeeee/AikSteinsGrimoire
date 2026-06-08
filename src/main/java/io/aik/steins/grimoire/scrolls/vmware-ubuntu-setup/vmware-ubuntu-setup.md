# VMware 安装 Ubuntu 与初始化配置指南

> 类型标记: **NOTE**
> 资料来源: [CSDN 博客](https://blog.csdn.net/m0_51913750/article/details/131604868) + 本地实践笔记
> 适用版本: VMware Workstation 15.x / 16.x / 17.x, Ubuntu 20.04 / 22.04

---

## 1. 概述

本文档记录从 VMware 虚拟机创建到 Ubuntu 系统安装完成，再到系统初始化、JDK 与 Docker 环境配置的完整流程。适用于开发测试环境的快速搭建。

---

## 2. 准备工作

### 2.1 下载 Ubuntu 镜像

| 来源 | 地址 | 说明 |
|------|------|------|
| 官方下载 | [ubuntu.com/download](https://ubuntu.com/download) | 速度可能较慢 |
| 清华大学镜像站 | [mirrors.tuna.tsinghua.edu.cn](https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/) | 国内推荐 |
| 阿里云镜像站 | [mirrors.aliyun.com](https://mirrors.aliyun.com/ubuntu-releases/) | 国内推荐 |

建议选择 **Ubuntu 22.04 LTS** 或 **Ubuntu 20.04 LTS** 版本，下载 `.iso` 镜像文件到本地。后续创建虚拟机时通过浏览本地文件的方式选择该 ISO 镜像即可。

---

## 3. VMware 创建虚拟机

### 3.1 新建虚拟机

1. 打开 VMware，点击 **【创建新的虚拟机】**，或点击左上角 **【文件】→【新建虚拟机】**（快捷键 `Ctrl + N`）
2. 勾选 **【自定义(高级)】**，点击 **【下一步】**
3. **硬件兼容性** 选择 `Workstation 15.x` 或更高版本，点击 **【下一步】**
4. 勾选 **【稍后安装操作系统】**，点击 **【下一步】**

### 3.2 选择操作系统

- **客户机操作系统**：选择 **Linux**
- **版本**：选择 **Ubuntu 64 位**
- 点击 **【下一步】**

### 3.3 命名与位置

- **虚拟机名称**：自定义，如 `Ubuntu-22.04`
- **位置**：建议在系统盘外新建独立文件夹存放，如 `D:\VMware\Virtual Machines\Ubuntu-22.04`
- 点击 **【下一步】**

### 3.4 处理器配置

| 配置项 | 建议值 |
|--------|--------|
| 处理器数量 | 2 |
| 每个处理器的内核数量 | 2 |

点击 **【下一步】**

### 3.5 内存配置

- **此虚拟机的内存**：基础使用建议 **4096 MB（4GB）**；若主机内存充裕（如 16GB 以上），建议分配 **8192 MB（8GB）**，运行 IDE、Docker 等工具更流畅
- 点击 **【下一步】**

### 3.6 网络类型

- 勾选 **【使用网络地址转换(NAT)】**
- 点击 **【下一步】**

### 3.7 I/O 控制器与磁盘类型

- I/O 控制器类型：选择默认的 **【LSI Logic（推荐）】**
- 磁盘类型：选择默认的 **【SCSI（推荐）】**
- 依次点击 **【下一步】**

### 3.8 创建虚拟磁盘

- 勾选 **【创建新虚拟磁盘】**，点击 **【下一步】**
- **最大磁盘大小**：基础使用建议 **20 GB**；若磁盘空间充裕，建议分配 **50 GB**，避免后续安装软件、拉取镜像时空间不足
- 勾选 **【将虚拟磁盘存储为单个文件】**（不常迁移虚拟机时性能更好）
- 点击 **【下一步】**

### 3.9 挂载 ISO 镜像

1. 点击 **【自定义硬件】**
2. 选择 **【新 CD/DVD (SATA) 自动检测】**
3. 右侧勾选 **【使用 ISO 映像文件】**
4. 点击 **【浏览】** 选择第 2 步下载好的 Ubuntu ISO 镜像
5. 点击 **【关闭】**

### 3.10 完成创建

确认配置无误后，点击 **【完成】**，虚拟机列表中将出现新建的虚拟机。

---

## 4. 安装 Ubuntu 系统

### 4.1 启动安装

1. 选中新建的虚拟机，点击 **【开启此虚拟机】**
2. 启动界面按 **回车键** 进入安装流程
3. 等待加载完成

### 4.2 安装向导

1. **语言选择**：强烈建议选择 **【English】**，后续使用 Linux 命令更方便
2. 点击 **【Install Ubuntu】**
3. 点击 **【Continue】**

### 4.3 键盘布局

- 默认 **English (US)** 即可
- 点击 **【Continue】**

### 4.4 安装类型

- 勾选 **【Erase disk and install Ubuntu】**（擦除磁盘并安装 Ubuntu）
- 点击 **【Install Now】**
- 确认弹窗中点击 **【Continue】**

### 4.5 时区设置

- **时区** 选择 **Shanghai**
- 点击 **【Continue】**

### 4.6 用户配置

| 配置项 | 说明 |
|--------|------|
| Your name | 你的姓名 |
| Your computer's name | 机器名（可留空自动根据用户名生成） |
| Pick a username | 登录用户名 |
| Choose a password | 登录密码 |
| Confirm your password | 确认密码 |

- 建议勾选 **【Require my password to log in】**
- 点击 **【Continue】**

### 4.7 等待安装完成

系统将自动安装，期间不要点击 **【Skip】**。安装完成后点击 **【Restart Now】**。

### 4.8 首次登录

1. 重启后若提示移除安装介质，按 **回车** 继续
2. 点击用户头像，输入密码登录
3. 首次进入系统会弹出欢迎向导，依次点击 **【Skip】→【Next】→【Next】→【Next】→【Done】**

### 4.9 创建快照（强烈推荐）

**建议时机**：在初始化配置全部完成（系统更新、基础工具、JDK、Docker 等安装完毕）后再创建快照。这样快照是一个可直接投入使用的「高可用版本」，而非裸系统。

操作步骤：

1. 确保虚拟机处于关闭或挂起状态
2. 点击 VMware 菜单 **【虚拟机】→【快照】→【拍摄快照】**
3. 输入快照名称，如 `Ready-To-Use`
4. 点击 **【拍摄快照】**

> 每次进行重大改动前（如升级内核、大规模安装软件），养成先拍快照的习惯。

---

## 5. Ubuntu 初始化配置

登录系统后，打开 Terminal（快捷键 `Ctrl + Alt + T`），依次执行以下初始化操作。

### 5.1 VMware NAT 网络与固定 IP 配置

NAT 模式下虚拟机默认通过 DHCP 获取 IP，重启后可能变化。建议先配置固定 IP，确保网络稳定后再进行后续更新和安装。

#### 步骤 1：VMware 虚拟网络编辑器配置

1. 在 VMware 主界面点击 **【编辑】→【虚拟网络编辑器】**
2. 选择 **【VMnet8 (NAT 模式)】**，点击 **【更改设置】**（需管理员权限）
3. 配置子网信息：

| 配置项 | 示例值 |
|--------|--------|
| 子网 IP | `192.168.33.0` |
| 子网掩码 | `255.255.255.0` |

4. 点击 **【NAT 设置】**，配置网关：

| 配置项 | 示例值 |
|--------|--------|
| 网关 IP | `192.168.33.2` |

5. （可选）点击 **【添加】** 配置端口转发，方便主机通过固定端口访问虚拟机 SSH：

| 配置项 | 示例值 |
|--------|--------|
| 主机端口 | `2222` |
| 类型 | `TCP` |
| 虚拟机 IP 地址 | `192.168.33.3:22` |

> 配置端口转发后，主机可通过 `ssh -p 2222 <username>@localhost` 连接虚拟机。

6. 点击 **【确定】** 保存

#### 步骤 2：Ubuntu 中设置固定 IP

1. 打开 Ubuntu **【设置】→【网络】→【有线连接】→【设置】**
2. 切换到 **【IPv4】** 选项卡
3. 选择 **【手动】**，填写：

| 配置项 | 示例值 |
|--------|--------|
| 地址 | `192.168.33.3` |
| 子网掩码 | `255.255.255.0` |
| 网关 | `192.168.33.2` |
| DNS | `223.5.5.5` |

4. 点击 **【应用】**，关闭再重新开启有线连接使配置生效
5. 验证固定 IP：

```bash
ip addr
# 确认 eth0/ens33 接口显示 192.168.33.3
```

6. 测试网络连通性：

```bash
ping -c 4 baidu.com
```

网络确认正常后，继续以下初始化操作。

### 5.2 更换国内软件源（强烈推荐）

默认官方源在国内访问速度较慢，建议先更换为清华或阿里镜像源，可大幅提升后续更新和安装速度。

**以 Ubuntu 22.04 更换清华源为例：**

```bash
# 备份原配置文件
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak

# 编辑软件源列表（系统自带 nano，vim 将在 5.6 节安装）
sudo nano /etc/apt/sources.list
```

将文件内容替换为（根据你的 Ubuntu 版本选择）：

```
# Ubuntu 22.04 (jammy)
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ jammy main restricted universe multiverse
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ jammy-updates main restricted universe multiverse
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ jammy-backports main restricted universe multiverse
deb https://mirrors.tuna.tsinghua.edu.cn/ubuntu/ jammy-security main restricted universe multiverse
```

> 其他版本将 `jammy` 替换为对应代号（20.04 为 `focal`）。阿里云源将 `mirrors.tuna.tsinghua.edu.cn` 替换为 `mirrors.aliyun.com` 即可。

保存后执行更新：

```bash
sudo apt update
```

### 5.3 系统更新

```bash
sudo apt upgrade -y
sudo apt full-upgrade -y
```

### 5.4 安装 VMware Tools

安装 `open-vm-tools-desktop` 以支持屏幕自适应、剪贴板共享等功能：

```bash
sudo apt install open-vm-tools-desktop -y
```

> 安装完成后建议重启虚拟机使功能完全生效。

### 5.5 配置 VMware 共享文件夹

共享文件夹可让主机与虚拟机之间直接传输文件，无需 SSH 或 U 盘：

1. VMware 菜单点击 **【虚拟机】→【设置】→【选项】→【共享文件夹】**
2. 选择 **【总是启用】**，点击 **【添加】**
3. 浏览选择主机上的一个文件夹（如 `D:\VMware\VMShareFiles`），设置共享名称
4. 勾选 **【启用此共享】**，点击 **【完成】**
5. 在 Ubuntu 中访问共享文件夹：

```bash
# 共享文件夹默认挂载在 /mnt/hgfs/ 下
ls /mnt/hgfs/

# 如果目录为空，手动挂载
sudo vmhgfs-fuse .host:/ /mnt/hgfs -o allow_other
```

> 若 `/mnt/hgfs/` 不存在，先创建：`sudo mkdir -p /mnt/hgfs`

### 5.6 安装基础工具

```bash
# 文本编辑器
sudo apt install vim -y

# 网络工具（ifconfig 等）
sudo apt install net-tools -y
```

### 5.7 配置 SSH 远程连接

```bash
# 安装 OpenSSH 服务端
sudo apt-get install openssh-server -y

# 启动 SSH 服务
sudo service ssh start

# 检查 SSH 服务是否运行
ps -e | grep ssh

# 防火墙放行 22 端口
sudo ufw allow 22
```

配置完成后，即可通过主机 SSH 客户端连接到虚拟机：

```bash
ssh <username>@<虚拟机IP>
```

> 虚拟机 IP 可通过 `ifconfig` 或 `ip addr` 查看。若已配置固定 IP（5.1 节），直接连接该固定 IP：

```bash
ssh <username>@192.168.33.3

# 若配置了端口转发，也可通过主机端口连接
ssh -p 2222 <username>@localhost
```

### 5.8 时区与时间同步

虚拟机可能出现与主机时间不同步的情况，建议检查并配置：

```bash
# 查看当前时区与时间状态
timedatectl status

# 若时区不正确，设置为上海时区
sudo timedatectl set-timezone Asia/Shanghai

# 开启 NTP 时间同步
sudo timedatectl set-ntp true

# 手动同步时间（如已安装 ntpdate）
sudo apt install ntpdate -y
sudo ntpdate cn.pool.ntp.org
```

### 5.9 安装编译依赖（可选）

如需编译安装 Nginx 等软件，预先安装依赖：

```bash
sudo apt-get install -y gcc zlib1g zlib1g-dev libpcre3-dev openssl libssl-dev
```

### 5.10 中文输入法（可选）

安装时选择了 English，若后续需要中文输入，可安装 ibus-pinyin：

```bash
# 安装 ibus 拼音输入法
sudo apt install ibus-pinyin -y

# 重启 ibus
ibus restart

# 在系统设置中添加输入法：Settings -> Keyboard -> Input Sources -> + -> Chinese (Pinyin)
```

> 也可选择 `fcitx` 框架：`sudo apt install fcitx-googlepinyin -y`

---

### 5.11 创建快照（提醒）

初始化配置全部完成后（系统更新、基础工具、JDK、Docker 等安装完毕），**别忘了创建快照**！这样快照是一个可直接投入使用的「高可用版本」，而非裸系统。

操作步骤：

1. 关闭虚拟机（或在虚拟机菜单中）
2. 点击 VMware 菜单 **【虚拟机】→【快照】→【拍摄快照】**
3. 输入快照名称，如 `Ready-To-Use`
4. 点击 **【拍摄快照】**

> 每次进行重大改动前（如升级内核、大规模安装软件），养成先拍快照的习惯。

---

## 6. 安装 JDK

以 OpenJDK 8 为例：

```bash
sudo apt install openjdk-8-jdk -y
```

验证安装：

```bash
java -version
```

---

## 7. 安装 Docker

### 7.1 卸载旧版本（如有）

```bash
sudo apt-get remove -y docker docker-engine docker.io containerd runc
```

### 7.2 安装依赖工具

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release
```

### 7.3 添加 Docker 官方 GPG 密钥

Ubuntu 22.04+ 弃用了 `apt-key`，官方推荐将密钥存放到 `/etc/apt/keyrings/`：

```bash
# 创建密钥目录
sudo install -m 0755 -d /etc/apt/keyrings

# 下载并导入 GPG 密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 设置密钥可读权限
sudo chmod a+r /etc/apt/keyrings/docker.gpg
```

> 若官方地址下载失败，可替换为国内镜像源，如阿里云：`https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg`

### 7.4 添加 Docker 软件源

```bash
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

> 同样，国内用户可将 `https://download.docker.com` 替换为阿里云镜像：`https://mirrors.aliyun.com/docker-ce`

### 7.5 安装 Docker Engine

```bash
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### 7.6 验证与自启动

```bash
# 查看 Docker 版本
sudo docker version

# 查看服务状态
systemctl status docker

# 设置开机自启
sudo systemctl enable docker

# 启动 Docker（如未运行）
sudo systemctl start docker
```

### 7.7 配置用户组（免 sudo）

将当前用户加入 `docker` 组，避免每次执行 Docker 命令都需要 `sudo`：

```bash
# 添加 docker 用户组（通常已存在）
sudo groupadd docker

# 将当前用户加入 docker 组
sudo gpasswd -a $USER docker

# 更新用户组
newgrp docker

# 测试
docker images
```

### 7.8 配置代理（可选）

若网络环境需要通过代理拉取镜像，为 Docker 守护进程配置代理：

```bash
# 创建配置目录
sudo mkdir -p /etc/systemd/system/docker.service.d

# 创建代理配置文件
sudo touch /etc/systemd/system/docker.service.d/proxy.conf
```

编辑 `proxy.conf`，写入：

```ini
[Service]
Environment="HTTP_PROXY=http://127.0.0.1:7892"
Environment="HTTPS_PROXY=http://127.0.0.1:7892"
Environment="NO_PROXY=localhost,127.0.0.1,.docker.internal,192.168.33.0/24"
```

> 请将 `127.0.0.1:7897` 替换为你的实际代理地址。

重载配置并重启 Docker：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 7.9 验证安装

```bash
# 运行测试容器
    docker pull library/hello-world
    docker images
    docker run hello-world
```

出现 `Hello from Docker!` 即表示安装成功。

---

## 8. 常见问题与注意事项

| 问题 | 解决方案 |
|------|----------|
| 安装界面卡顿/无响应 | 检查虚拟机内存分配是否充足，建议至少 4GB |
| 安装完成后屏幕分辨率无法调整 | 确认 `open-vm-tools-desktop` 已安装并重启 |
| SSH 连接失败 | 检查虚拟机 IP 是否正确、SSH 服务是否启动、防火墙 22 端口是否放行 |
| Docker 拉取镜像超时 | 配置国内镜像加速器或检查代理设置 |
| `docker` 命令提示权限不足 | 确认当前用户已加入 `docker` 组并执行 `newgrp docker` |

---

## 9. 初始化清单速查

首次搭建完成后，建议按以下顺序执行：

```bash
# 1. 固定 IP（先配置网络，确保后续更新和安装能正常访问外网）
# 参考 5.1 节完成 VMware NAT 子网和 Ubuntu 固定 IP 配置
# 验证网络：ping -c 4 baidu.com

# 2. 更换国内软件源（先备份再替换 /etc/apt/sources.list）
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak
# （使用 nano 编辑 sources.list 替换为清华/阿里源后执行）
sudo apt update

# 3. 系统更新
sudo apt upgrade -y && sudo apt full-upgrade -y

# 4. VMware 工具
sudo apt install open-vm-tools-desktop -y

# 5. 基础工具
sudo apt install vim net-tools -y

# 6. SSH
sudo apt-get install openssh-server -y
sudo service ssh start
sudo ufw allow 22

# 7. JDK
sudo apt install openjdk-8-jdk -y

# 8. Docker（按第 7 章步骤执行）
```

---

> 本文档为学习笔记，内容整合自网络教程与个人实践，仅供参考。
