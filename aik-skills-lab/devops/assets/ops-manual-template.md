# 运维手册

> 生成时间: {yyyy-MM-dd}
> 版本: v{version}
> 项目: {项目名称}

---

## 一、系统概述

| 项目 | 内容 |
|------|------|
| 应用名称 | {app-name} |
| 技术栈 | Java {version} + Spring Boot {version} |
| 部署方式 | Systemd / Docker |
| 部署目录 | /opt/{app} |
| 端口 | 8080 |
| 运维负责人 | {name} |

---

## 二、日常运维操作

### 2.1 查看应用状态

```bash
# Systemd 方式
systemctl status {app}

# 脚本方式
/opt/{app}/bin/status.sh
```

### 2.2 查看日志

```bash
# 实时查看应用日志
tail -f /opt/{app}/logs/application.log

# 实时查看错误日志
tail -f /opt/{app}/logs/error.log

# 查看最近100行
tail -n 100 /opt/{app}/logs/application.log

# 按关键字搜索
grep "ERROR" /opt/{app}/logs/application.log
grep "订单号" /opt/{app}/logs/application.log

# 按时间范围搜索
grep "2024-03-18 10:" /opt/{app}/logs/application.log
```

### 2.3 重启应用

```bash
# Systemd 方式
systemctl restart {app}

# 脚本方式
/opt/{app}/bin/shutdown.sh && /opt/{app}/bin/startup.sh
```

### 2.4 停止应用

```bash
# Systemd 方式
systemctl stop {app}

# 脚本方式
/opt/{app}/bin/shutdown.sh
```

### 2.5 启动应用

```bash
# Systemd 方式
systemctl start {app}

# 脚本方式
/opt/{app}/bin/startup.sh
```

---

## 三、监控检查

### 3.1 健康检查

```bash
# 基础健康检查
curl http://localhost:8080/actuator/health

# 详细信息
curl http://localhost:8080/actuator/health/details

# 应用信息
curl http://localhost:8080/actuator/info

# 检查应用是否存活
curl -o /dev/null -s -w "%{http_code}" http://localhost:8080/actuator/health
```

### 3.2 资源监控

```bash
# CPU和内存
top -p $(pgrep -f {app}) -bn1
htop

# 内存详情
ps -p $(pgrep -f {app}) -o pid,rss,vsz,pcpu,pmem,cmd

# 磁盘使用
df -h /opt/{app}
du -sh /opt/{app}/logs/

# 端口监听
netstat -tlnp | grep 8080
ss -tlnp | grep 8080

# 网络连接数
netstat -an | grep 8080 | wc -l
```

### 3.3 JVM 监控

```bash
# JVM 进程信息
jps -l

# JVM 堆内存
jstat -gc $(pgrep -f {app}) 1000

# JVM 线程
jstack $(pgrep -f {app})

# JVM 堆转储
jmap -dump:format=b,file=/tmp/heap.hprof $(pgrep -f {app})
```

---

## 四、故障处理

### 4.1 应用无法启动

**症状**: systemctl start 失败或启动后立即退出

**排查步骤**:
1. 查看启动日志：
   ```bash
   tail -n 200 /opt/{app}/logs/application.log
   journalctl -u {app} -n 50
   ```
2. 检查端口是否被占用：
   ```bash
   netstat -tlnp | grep 8080
   lsof -i :8080
   ```
3. 检查配置文件：
   ```bash
   cat /opt/{app}/config/application.yml
   ```
4. 检查 Java 版本：
   ```bash
   java -version
   ```
5. 检查磁盘空间：
   ```bash
   df -h /opt/{app}
   ```
6. 手动启动排查：
   ```bash
   java -jar /opt/{app}/{app}.jar --spring.config.location=/opt/{app}/config/application.yml
   ```

**常见原因**:
- 端口被占用
- 配置文件格式错误
- 数据库连接失败
- 磁盘空间不足
- Java版本不匹配

### 4.2 数据库连接失败

**症状**: 日志出现 "Communications link failure" 或 "Access denied"

**排查步骤**:
1. 检查网络连通性：
   ```bash
   ping {db_host}
   telnet {db_host} 3306
   ```
2. 检查配置文件中的数据库连接信息
3. 检查数据库服务状态
4. 检查数据库连接数：
   ```sql
   SHOW PROCESSLIST;
   SHOW VARIABLES LIKE 'max_connections';
   ```

### 4.3 Redis 连接失败

**症状**: 日志出现 "Cannot get Jedis connection"

**排查步骤**:
1. 检查网络：`telnet {redis_host} 6379`
2. 检查 Redis 服务状态：`redis-cli -h {redis_host} ping`
3. 检查配置文件中 Redis 连接信息

### 4.4 内存溢出 (OOM)

**症状**: 应用崩溃，日志中出现 "OutOfMemoryError"

**排查步骤**:
1. 查看 JVM 堆使用：
   ```bash
   jstat -gcutil $(pgrep -f {app}) 1000
   ```
2. 生成堆转储分析：
   ```bash
   jmap -dump:format=b,file=/tmp/heap.hprof $(pgrep -f {app})
   ```
3. 检查 JVM 参数：
   ```bash
   ps -ef | grep {app} | grep -o 'Xm[sx][^ ]*'
   ```

**解决措施**:
- 临时：重启应用
- 长期：分析堆转储，修复内存泄漏，调整 JVM 参数

### 4.5 接口响应慢

**症状**: 接口响应时间 > 3秒

**排查步骤**:
1. 查看慢 SQL：
   ```bash
   grep "slow" /opt/{app}/logs/application.log
   ```
2. 查看数据库慢查询：
   ```sql
   SHOW FULL PROCESSLIST;
   ```
3. 查看线程状态：
   ```bash
   jstack $(pgrep -f {app}) > /tmp/thread_dump.txt
   ```
4. 检查 CPU/内存：
   ```bash
   top -p $(pgrep -f {app})
   ```

---

## 五、日志管理

### 5.1 日志文件

| 日志文件 | 路径 | 说明 |
|---------|------|------|
| 应用日志 | /opt/{app}/logs/application.log | 全部日志 |
| 错误日志 | /opt/{app}/logs/error.log | 仅ERROR级别 |
| 慢SQL日志 | /opt/{app}/logs/slow-sql.log | 慢SQL记录 |

### 5.2 日志轮转

```bash
# 查看日志配置
cat /opt/{app}/config/logback-spring.xml

# 手动清理旧日志
find /opt/{app}/logs/ -name "*.log.*" -mtime +30 -delete

# 查看日志大小
du -sh /opt/{app}/logs/
```

### 5.3 定时清理脚本

```bash
#!/bin/bash
# 清理30天前的日志文件
LOG_DIR=/opt/{app}/logs
find $LOG_DIR -name "*.log.*" -mtime +30 -exec rm -f {} \;
find $LOG_DIR -name "*.gz" -mtime +30 -exec rm -f {} \;
echo "$(date): 日志清理完成" >> $LOG_DIR/cleanup.log
```

---

## 六、应急回滚

### 6.1 回滚触发条件

- 部署后出现 P0 级别缺陷
- 核心业务不可用超过 5 分钟
- 数据出现异常

### 6.2 回滚命令

```bash
# 快速回滚脚本
cd /opt/{app}

# 1. 停止当前版本
systemctl stop {app}

# 2. 恢复上一个版本
BACKUP_DIR=/opt/backup
LATEST_BACKUP=$(ls -t $BACKUP_DIR/{app}-*.tar.gz | head -1)
rm -rf /opt/{app}/
tar -xzf $LATEST_BACKUP -C /opt/{app}/

# 3. 启动服务
systemctl start {app}

# 4. 验证
sleep 10
curl -f http://localhost:8080/actuator/health && echo "回滚成功" || echo "回滚失败，请检查"
```

### 6.3 回滚后验证

- [ ] 应用启动成功
- [ ] 健康检查通过
- [ ] 核心功能正常
- [ ] 日志无异常

---

## 七、常见问题速查

| 问题 | 可能原因 | 快速解决 |
|------|---------|---------|
| 端口占用 | 旧进程未停止 | `lsof -i :8080` 找到进程 kill |
| OOM | 内存泄漏或配置不当 | 重启应用，后续分析堆转储 |
| 数据库连接超时 | 网络问题或连接池耗尽 | 重启应用，检查数据库负载 |
| 接口500 | 代码异常 | 查看 error.log，根据堆栈分析 |
| CPU 100% | 死循环或GC频繁 | jstack 导出线程，jstat 查看GC |

---

## 八、系统架构速查

### 8.1 端口列表

| 服务 | 端口 | 说明 |
|------|------|------|
| 应用 | 8080 | HTTP服务 |
| Actuator | 8080 | 监控端点 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| RabbitMQ | 5672 | 消息队列 |

### 8.2 目录结构

```
/opt/{app}/
├── bin/
│   ├── startup.sh           # 启动脚本
│   ├── shutdown.sh          # 停止脚本
│   └── status.sh            # 状态检查
├── config/
│   └── application.yml      # 外置配置文件
├── lib/
│   └── {app}.jar            # 应用 Jar 包
├── logs/
│   ├── application.log      # 应用日志
│   └── error.log            # 错误日志
└── backup/                  # 版本备份
```

---

## 九、联系信息

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 运维负责人 | {name} | {phone} | {email} |
| 开发负责人 | {name} | {phone} | {email} |
| DBA | {name} | {phone} | {email} |
| 项目经理 | {name} | {phone} | {email} |

### 升级联系顺序

1. 一线：运维负责人
2. 二线：开发负责人
3. 三线：技术总监

---

## 十、附录

### 变更历史

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v{version} | {yyyy-MM-dd} | 初始版本 | {author} |

### Systemd 服务配置

```ini
[Unit]
Description={app} Application
After=network.target

[Service]
Type=simple
User={user}
WorkingDirectory=/opt/{app}
ExecStart=/usr/bin/java -jar /opt/{app}/lib/{app}.jar --spring.config.location=/opt/{app}/config/application.yml
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

---

> 本文档由 spec-devops 生成，供运维团队日常使用。请根据实际情况更新和维护。
