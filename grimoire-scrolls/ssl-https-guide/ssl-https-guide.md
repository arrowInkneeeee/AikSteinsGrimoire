# SSL/TLS 与 HTTPS 完全指南

> 基于个人实践整理，结合网络资料补充  
> 覆盖证书生成、Nginx HTTPS 配置、生产环境安全加固

---

## 一、HTTPS 原理概述

### 1.1 为什么需要 HTTPS

HTTP 以明文传输数据，存在三大风险：
- **窃听**：传输内容可被中间人截取
- **篡改**：数据在传输过程中被修改
- **冒充**：无法确认服务器真实身份

HTTPS = HTTP + TLS/SSL，在 HTTP 和 TCP 之间插入加密层，解决上述问题。

### 1.2 核心加密机制

| 加密方式 | 特点 | 用途 |
|----------|------|------|
| **对称加密** | 加解密使用同一密钥，速度快 | 实际数据传输 |
| **非对称加密** | 公钥加密、私钥解密，速度慢 | 密钥交换、身份认证 |
| **哈希（散列）** | 单向计算，验证数据完整性 | 消息摘要、证书指纹 |

TLS 握手的关键：用非对称加密安全地交换对称加密的会话密钥，之后的通信全部使用对称加密。

### 1.3 TLS 握手流程（TLS 1.2）

```
客户端                              服务器
  | ------ ClientHello -------------> |
  |  (支持的TLS版本、加密套件、Client Random) |
  | <----- ServerHello -------------- |
  |  (确认TLS版本、加密套件、Server Random)   |
  | <----- Certificate -------------- |
  |  (服务器证书，含公钥)              |
  | <----- ServerHelloDone ---------- |
  | ------ ClientKeyExchange --------> |
  |  (用公钥加密的 Pre-Master Secret)  |
  | ------ ChangeCipherSpec ---------> |
  | ------ Finished -----------------> |
  | <----- ChangeCipherSpec --------- |
  | <----- Finished ----------------- |
  |                                   |
  | ===== 加密通信开始 ================ |
```

**关键步骤说明**：

1. **ClientHello**：客户端发送支持的 TLS 版本、加密套件列表、32 字节随机数（Client Random）
2. **ServerHello**：服务器选择双方都支持的配置，返回服务器随机数（Server Random）
3. **Certificate**：服务器发送数字证书，客户端验证证书链（从服务器证书逐级验证到根 CA）
4. **ClientKeyExchange**：客户端生成 Pre-Master Secret，用服务器公钥加密后发送
5. **密钥生成**：双方用 Client Random + Server Random + Pre-Master Secret 计算会话密钥
6. **Finished**：双方发送 Finished 消息，验证握手完整性，之后所有通信使用会话密钥加密

### 1.4 TLS 1.3 的改进

| 特性 | TLS 1.2 | TLS 1.3 |
|------|---------|---------|
| 握手 RTT | 2-RTT | 1-RTT（首次）/ 0-RTT（复用） |
| 支持的套件 | 大量（含不安全的） | 仅 5 组安全套件 |
| 握手消息 | 明文传输部分参数 | 除 ClientHello 外全部加密 |
| 前向保密 | 可选 | 强制要求 |

---

## 二、SSL/TLS 证书类型

### 2.1 按验证等级分类

| 类型 | 验证内容 | 颁发时间 | 适用场景 | 浏览器显示 |
|------|----------|----------|----------|------------|
| **DV**（域名验证） | 验证域名所有权 | 分钟级 | 个人站点、博客 | 普通锁图标 |
| **OV**（组织验证） | 验证域名 + 组织身份 | 1-3 天 | 企业官网 | 普通锁图标 |
| **EV**（扩展验证） | 严格验证组织法律身份 | 1-2 周 | 金融、电商 | 绿色地址栏（旧版） |

### 2.2 按覆盖范围分类

| 类型 | 说明 | 示例 |
|------|------|------|
| **单域名证书** | 只保护一个完整域名 | `www.example.com` |
| **多域名证书**（SAN） | 一张证书保护多个不同域名 | `example.com`, `api.example.com` |
| **通配符证书** | 保护一个域名及其所有子域名 | `*.example.com` |

### 2.3 按来源分类

| 类型 | 费用 | 信任度 | 适用场景 |
|------|------|--------|----------|
| **商业 CA 证书** | 付费 | 高 | 生产环境 |
| **Let's Encrypt** | 免费 | 高 | 生产环境（90 天有效期） |
| **自签名证书** | 免费 | 低（需手动信任） | 开发/测试环境 |

---

## 三、证书生成与管理

### 3.1 安装 OpenSSL

**Windows**：
- 下载地址：https://slproweb.com/products/Win32OpenSSL.html
- 选择对应系统的安装包（Win64 OpenSSL-3.x.x.exe）
- 安装时选择将 OpenSSL 的 bin 目录添加到系统 PATH

**Linux**：
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openssl

# CentOS/RHEL
sudo yum install openssl
```

验证安装：
```bash
openssl version
```

### 3.2 生成自签名证书（开发测试用）

```bash
# 1. 创建证书存放目录
mkdir ~/ssl-certs && cd ~/ssl-certs

# 2. 生成私钥（带密码保护）
openssl genpkey -algorithm RSA -out mykey.key -aes256

# 2'. 生成私钥（不带密码，适合自动化部署）
openssl genpkey -algorithm RSA -out mykey.key

# 3. 生成证书签名请求（CSR）
openssl req -new -key mykey.key -out mycert.csr
# 按提示填写信息（Country、State、Organization、Common Name 等）
# Common Name 必须填写域名或 IP 地址

# 4. 生成自签名证书（有效期 365 天）
openssl x509 -req -days 365 -in mycert.csr -signkey mykey.key -out mycert.crt
```

**一步生成私钥 + 自签名证书**（快速测试）：
```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout mykey.key -out mycert.crt \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=MyOrg/CN=localhost"
```

参数说明：
- `-x509`：直接生成自签名证书，不生成 CSR
- `-nodes`：不加密私钥（No DES）
- `-days 365`：证书有效期
- `-newkey rsa:2048`：生成 2048 位 RSA 密钥
- `-subj`：非交互式填写证书信息

### 3.3 去除私钥密码

```bash
# 将带密码的私钥转换为无密码版本
openssl rsa -in file.key -out nopassword.key
```

### 3.4 证书格式转换

| 格式 | 扩展名 | 说明 | 转换命令 |
|------|--------|------|----------|
| **PEM** | .pem, .crt, .key | Base64 编码文本，最常见 | 默认格式 |
| **DER** | .der, .cer | 二进制格式 | `openssl x509 -in cert.pem -outform der -out cert.der` |
| **PFX/P12** | .pfx, .p12 | 包含证书和私钥的加密容器（Windows 常用） | `openssl pkcs12 -export -in cert.crt -inkey key.key -out cert.pfx` |

**PEM 转 PFX（Java KeyStore 常用）**：
```bash
openssl pkcs12 -export -in mycert.crt -inkey mykey.key \
  -out keystore.pfx -name myalias
```

**查看证书信息**：
```bash
# 查看 PEM 证书内容
openssl x509 -in mycert.crt -text -noout

# 查看证书过期时间
openssl x509 -in mycert.crt -dates -noout

# 查看证书 Subject 和 Issuer
openssl x509 -in mycert.crt -subject -issuer -noout
```

### 3.5 使用 Let's Encrypt 申请免费证书（生产环境）

**使用 Certbot（推荐）**：

```bash
# 安装 Certbot（Ubuntu + Nginx）
sudo apt update
sudo apt install certbot python3-certbot-nginx

# 自动申请证书并配置 Nginx
sudo certbot --nginx -d example.com -d www.example.com

# 测试自动续期
sudo certbot renew --dry-run
```

Certbot 会自动：
1. 验证域名所有权（HTTP-01 挑战）
2. 申请并下载证书
3. 修改 Nginx 配置启用 HTTPS
4. 设置定时任务自动续期（systemd timer 或 cron）

**证书文件位置**：
```
/etc/letsencrypt/live/example.com/
├── fullchain.pem    # 证书 + 中间 CA 链
├── privkey.pem      # 私钥
├── cert.pem         # 证书
└── chain.pem        # 中间 CA 链
```

---

## 四、Nginx HTTPS 配置

### 4.1 基础 HTTPS 配置

基于用户实践整理的配置模板：

```nginx
server {
    listen 443 ssl;
    server_name example.com;

    # SSL 证书路径
    ssl_certificate     /path/to/mycert.crt;
    ssl_certificate_key /path/to/mykey.key;

    # SSL 协议版本（禁用不安全的旧版本）
    ssl_protocols TLSv1.2 TLSv1.3;

    # 加密套件（优先使用 ECDHE 实现前向保密）
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # 静态文件服务示例
    root /var/www/html;
    index index.html index.htm;

    location / {
        try_files $uri $uri/ =404;
    }
}
```

### 4.2 HTTP 跳转 HTTPS

```nginx
# HTTP 服务器：301 重定向到 HTTPS
server {
    listen 80;
    server_name example.com www.example.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 服务器
server {
    listen 443 ssl http2;
    server_name example.com www.example.com;
    # ... SSL 配置
}
```

### 4.3 生产环境安全加固配置（A+ 评级）

```nginx
server {
    listen 443 ssl http2;
    server_name example.com;

    # 证书配置
    ssl_certificate     /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

    # 协议版本：仅 TLS 1.2/1.3
    ssl_protocols TLSv1.2 TLSv1.3;

    # 加密套件（Mozilla 现代配置）
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # 会话缓存（减少 TLS 握手开销）
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;

    # OCSP Stapling（提升证书验证性能）
    ssl_stapling on;
    ssl_stapling_verify on;
    resolver 8.8.8.8 8.8.4.4 valid=300s;
    resolver_timeout 5s;

    # HSTS（强制浏览器使用 HTTPS）
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;

    # 安全响应头
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 隐藏 Nginx 版本号
    server_tokens off;

    root /var/www/html;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }
}
```

### 4.4 配置项详解

| 配置项 | 作用 | 推荐值 |
|--------|------|--------|
| `listen 443 ssl http2` | 监听 HTTPS 端口，启用 HTTP/2 | `443 ssl http2` |
| `ssl_protocols` | 允许的 TLS 版本 | `TLSv1.2 TLSv1.3` |
| `ssl_ciphers` | 加密套件列表 | Mozilla 现代配置 |
| `ssl_session_cache` | 会话缓存 | `shared:SSL:10m` |
| `ssl_stapling` | OCSP Stapling | `on` |
| `HSTS` | 强制 HTTPS | `max-age=63072000` |
| `server_tokens` | 隐藏版本号 | `off` |

### 4.5 HTTP/2 配置

```nginx
server {
    listen 443 ssl http2;
    # ... 其他配置
}
```

HTTP/2 优势：
- **多路复用**：单一 TCP 连接可并发传输多个请求/响应
- **头部压缩**（HPACK）：减少冗余头部传输
- **服务器推送**：服务器可主动推送资源（较少使用）

**注意**：反向代理到后端时，建议保持 `proxy_http_version 1.1`，后端服务走 HTTP/1.1 + keepalive 已足够高效。

---

## 五、Java / Spring Boot 场景的 HTTPS

### 5.1 将 PEM 证书转为 Java KeyStore

Spring Boot 通常使用 `PKCS12` 或 `JKS` 格式的密钥库：

```bash
# PEM 转 PKCS12
openssl pkcs12 -export -in mycert.crt -inkey mykey.key \
  -out keystore.p12 -name myalias \
  -CAfile ca.crt -caname root

# 输入导出密码（Spring Boot 配置中需要）
```

### 5.2 Spring Boot HTTPS 配置

```yaml
# application.yml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: your-password
    key-store-type: PKCS12
    key-alias: myalias
    # 强制只使用 TLS 1.2+
    enabled-protocols: TLSv1.2,TLSv1.3
```

### 5.3 信任自签名证书

**开发环境**：将自签名证书导入 Java 信任库

```bash
# 导出证书到 truststore
keytool -import -alias myca -file mycert.crt \
  -keystore $JAVA_HOME/lib/security/cacerts \
  -storepass changeit
```

**或者在 HTTP 客户端中配置**：

```java
// 信任所有证书（仅开发测试用！）
TrustManager[] trustAllCerts = new TrustManager[]{
    new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
};

SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(null, trustAllCerts, new SecureRandom());
```

---

## 六、常见问题与排查

### 6.1 浏览器提示"证书不安全"

| 原因 | 解决方法 |
|------|----------|
| 自签名证书 | 开发环境手动信任，生产环境使用正规 CA 证书 |
| 证书过期 | 续期证书并重启 Nginx |
| 域名不匹配 | 检查证书 CN/SAN 是否包含访问域名 |
| 证书链不完整 | 使用 `fullchain.pem` 而非仅 `cert.pem` |
| 系统时间错误 | 校准客户端系统时间 |

### 6.2 证书过期处理

```bash
# 查看证书过期时间
openssl x509 -in mycert.crt -dates -noout

# Let's Encrypt 手动续期
sudo certbot renew

# 续期后重载 Nginx
sudo nginx -s reload
```

### 6.3 Mixed Content（混合内容）

**现象**：HTTPS 页面加载 HTTP 资源被浏览器阻止。

**解决**：
- 将所有资源链接改为 `https://` 或 `//`（协议相对 URL）
- Nginx 添加响应头：`add_header Content-Security-Policy "upgrade-insecure-requests" always;`

### 6.4 Nginx HTTPS 无法访问

排查清单：
1. `netstat -tlnp | grep 443` — 确认 Nginx 已监听 443 端口
2. `sudo firewall-cmd --list-ports` / `sudo ufw status` — 检查防火墙
3. `sudo nginx -t` — 验证配置文件语法
4. 查看 Nginx 错误日志：`/var/log/nginx/error.log`
5. 确认证书文件路径正确且 Nginx 进程有读取权限

### 6.5 SNI（Server Name Indication）

当一台服务器托管多个 HTTPS 站点时，Nginx 通过 SNI 根据请求的域名返回对应证书。

```nginx
server {
    listen 443 ssl;
    server_name site-a.com;
    ssl_certificate /path/to/site-a.crt;
    ssl_certificate_key /path/to/site-a.key;
}

server {
    listen 443 ssl;
    server_name site-b.com;
    ssl_certificate /path/to/site-b.crt;
    ssl_certificate_key /path/to/site-b.key;
}
```

**注意**：非常旧的客户端（如 Windows XP IE8）不支持 SNI。

---

## 七、配置检查与验证工具

### 7.1 在线检测

| 工具 | 用途 | 链接 |
|------|------|------|
| SSL Labs | 全面的 HTTPS 配置评分 | https://www.ssllabs.com/ssltest/ |
| SSL Checker | 证书过期、链完整性检查 | https://www.sslchecker.com/ |
| Security Headers | HTTP 安全响应头检测 | https://securityheaders.com/ |

### 7.2 命令行检测

```bash
# 测试 TLS 握手
openssl s_client -connect example.com:443 -tls1_2

# 查看服务器证书链
openssl s_client -connect example.com:443 -showcerts

# 测试 HTTP/2 支持
curl -I --http2 https://example.com

# 查看证书详细信息
echo | openssl s_client -servername example.com -connect example.com:443 2>/dev/null | openssl x509 -noout -text
```

---

## 参考资源

- [Pro Git 官方文档](https://git-scm.com/book/zh/v2)（用户原始资料）
- [Let's Encrypt 官方文档](https://letsencrypt.org/docs/)
- [Certbot 官方指南](https://certbot.eff.org/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [Nginx HTTPS 官方文档](https://nginx.org/en/docs/http/ngx_http_ssl_module.html)
