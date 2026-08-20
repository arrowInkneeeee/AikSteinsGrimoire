# WSL2 + Ubuntu 26.04 LTS 开发环境搭建方案 v3.0

> 适用系统：Windows 11 + WSL2 + Ubuntu 26.04 LTS
> 版本：3.0
> 更新日期：2026-08-19
> 核心原则：按实际开发需求安装，保持环境精简；不安装未授权的软件和服务。

---

## 方案目标

本版本在 2.0 的基础上，根据实际初始化结果和使用需求进行了收敛。

| 类别 | 安装内容 |
| :--- | :--- |
| Java | 仅 OpenJDK 8 |
| 数据库 | MySQL 客户端、Redis 客户端 |
| 容器 | Docker CE、Compose、Buildx |
| 远程服务 | OpenSSH Server |
| 编译环境 | GCC、G++、Make、Build Essential |
| 辅助工具 | unzip、zip、lsof、rsync、pkg-config、python3-venv 等 |

明确不安装：OpenJDK 17/21/25、MySQL Server、PostgreSQL Server/Client、Maven、Gradle，以及其他未授权的数据库、构建工具或后台服务。

---

## 一、系统与权限检查

~~~bash
uname -a
. /etc/os-release && echo "$PRETTY_NAME"
echo "WSL_DISTRO_NAME=$WSL_DISTRO_NAME"
sudo -n true && echo "sudo 免密已启用"
~~~

如果出现密码提示或 no new privileges，先检查当前终端或 Codex 执行通道的权限状态。

---

## 二、系统更新

~~~bash
sudo apt update
sudo apt upgrade -y
~~~

如果 apt 下载大软件包长时间无响应，可使用 IPv4 和有限重试：

~~~bash
sudo apt -o Acquire::ForceIPv4=true \
         -o Acquire::Retries=3 \
         -o Acquire::http::Timeout=30 update
~~~

必要时使用 curl -4 直接下载卡住的软件包至 /var/cache/apt/archives/，再重新执行 apt 安装。不要因为下载卡顿而改装其他 Java 版本。

---

## 三、代理配置

代理 IP 和端口因环境而异，Codex 不能猜测或擅自写入。确认实际值后，将以下函数加入 ~/.bashrc：

~~~bash
function proxy-on() {
    export hostip="192.168.x.x"
    export http_proxy="http://${hostip}:7892"
    export https_proxy="http://${hostip}:7892"
    echo "代理已开启: ${http_proxy}"
}

function proxy-off() {
    unset hostip http_proxy https_proxy
    echo "代理已关闭"
}

function proxy-status() {
    if [ -n "$http_proxy" ]; then
        echo "当前代理: $http_proxy"
    else
        echo "当前未启用代理"
    fi
}
~~~

加载并验证：

~~~bash
source ~/.bashrc
proxy-on
curl -I https://github.com
~~~

---

## 四、基础开发工具

~~~bash
sudo apt install -y \
  vim net-tools openssh-server curl wget git build-essential \
  ca-certificates gnupg lsb-release \
  dnsutils iputils-ping jq tree htop tig \
  unzip zip lsof rsync pkg-config python3-venv
~~~

工具用途包括基础编辑、编译、网络诊断、压缩包处理、端口排查、文件同步、原生库发现和 Python 虚拟环境。

---

## 五、Java 环境：只安装 JDK 8

~~~bash
sudo apt install -y openjdk-8-jdk
java -version
javac -version
~~~

预期版本为 8.x。不得安装 openjdk-17-jdk、openjdk-21-jdk、openjdk-25-jdk 或其他 Java 版本。

检查当前 Java：

~~~bash
update-alternatives --list java
~~~

本方案不主动配置多版本 Java 切换，因为目标环境只需要 JDK 8。

---

## 六、数据库客户端：只安装必要工具

### MySQL 客户端

只安装客户端，不安装本地数据库服务：

~~~bash
sudo apt install -y mysql-client
mysql --version
~~~

允许：mysql-client、mysql-client-core、mysql-common。禁止：mysql-server、mysqld。

连接远程数据库：

~~~bash
mysql -h 数据库地址 -u 用户名 -p
~~~

### Redis 客户端

~~~bash
sudo apt install -y redis-tools
redis-cli --version
~~~

只安装 redis-cli，不安装或启动 Redis 服务端。

### PostgreSQL

当前环境不需要 PostgreSQL，因此不安装 PostgreSQL Server 或 Client。

---

## 七、Docker CE

使用 Docker 官方仓库：

~~~bash
sudo apt-get remove -y docker docker-engine docker-ce docker.io 2>/dev/null || true
sudo install -d -m 0755 /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor \
  | sudo tee /etc/apt/keyrings/docker.gpg >/dev/null
sudo chmod 0644 /etc/apt/keyrings/docker.gpg

printf 'Types: deb\nURIs: https://download.docker.com/linux/ubuntu\nSuites: %s\nComponents: stable\nArchitectures: amd64\nSigned-By: /etc/apt/keyrings/docker.gpg\n' \
  "$( . /etc/os-release && echo "$VERSION_CODENAME" )" \
  | sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin
sudo groupadd docker 2>/dev/null || true
sudo usermod -aG docker "$USER"
sudo systemctl enable --now docker
~~~

验证：

~~~bash
docker --version
docker compose version
docker info
~~~

重新打开 WSL 终端后，docker 组权限才会在当前会话中生效。

---

## 八、SSH 服务

~~~bash
sudo apt install -y openssh-server
sudo systemctl enable --now ssh
sudo ss -ltnp | grep ':22'
~~~

在启用 systemd 的 WSL2 中，也可能看到 ssh.socket 监听 22 端口，这是正常的 socket 激活模式。

---

## 九、Codex 执行约束

1. Java 只允许安装 openjdk-8-jdk。
2. 不得安装 OpenJDK 17、21、25 或其他 Java 版本。
3. 数据库只允许安装 mysql-client 和 redis-tools。
4. 不得安装 mysql-server、PostgreSQL Server 或 PostgreSQL Client。
5. 不得安装 Maven 或 Gradle。
6. 不得因为“常用”或“备用”理由扩展安装范围。
7. 安装前检查 command -v 和 dpkg 状态，避免重复安装。
8. 新增工具或服务前，必须先说明用途并获得明确授权。
9. 卸载前确认精确包名，避免误删用户已有环境。
10. 不擅自修改代理、SSH 安全策略或数据库连接配置。

---

## 十、最终验收

~~~bash
java -version
javac -version
gcc --version | head -1
git --version
docker --version
docker compose version
mysql --version
redis-cli --version
python3 --version
pkg-config --version
command -v unzip zip lsof rsync

command -v mvn || echo 'Maven 未安装'
command -v gradle || echo 'Gradle 未安装'

dpkg-query -W -f='${binary:Package}\t${Status}\n' \
  openjdk-8-jdk mysql-client redis-tools 2>/dev/null
~~~

最终环境应满足：

- java 和 javac 为 1.8.x
- MySQL 命令可用，但本机没有 mysqld 服务
- Redis 客户端命令可用
- Docker 服务处于运行状态
- Maven、Gradle、PostgreSQL 和多余 Java 版本不存在

---

## 变更记录

### v3.0

- Java 安装范围收敛为 JDK 8
- 明确区分 MySQL 客户端与服务端
- 移除 PostgreSQL 客户端安装要求
- 保留 Redis 客户端
- 明确跳过 Maven 和 Gradle
- 增加 unzip、zip、lsof、rsync、pkg-config、python3-venv
- 增加 sudo、Docker、Java、数据库客户端和排除项验收
- 增加 apt 下载异常时的 IPv4 和直接下载回退方案
- 增加 Codex 安装边界和新增软件授权规则
