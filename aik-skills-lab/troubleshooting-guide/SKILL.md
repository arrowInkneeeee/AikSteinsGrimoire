---
name: troubleshooting-guide
description: 为Spring Boot项目提供故障排查手册和应急响应指南，包含常见问题诊断步骤、日志分析方法、应急回滚方案。帮助运维人员快速定位和解决问题，降低故障影响时间。
type: Skill
version: 1.0.0
---

# Troubleshooting Guide

## Purpose

为Spring Boot项目提供故障排查手册，包括：
- 常见问题诊断步骤
- 日志分析方法
- 应急回滚方案
- 性能问题排查

## When to Use

- 应用无法启动
- 应用运行异常
- 性能下降
- 需要紧急回滚

## Quick Diagnostics

### 1. 应用无法启动

**检查清单：**

```bash
# 1. 检查Java版本
java -version
# 应为Java 8

# 2. 检查端口占用
netstat -tlnp | grep 8080
# 或
lsof -i:8080

# 3. 检查配置文件
ls -la config/application.yml
# 确认文件存在且权限正确

# 4. 查看启动日志
tail -n 100 logs/application.log

# 5. 检查磁盘空间
df -h

# 6. 检查内存
free -h
```

**常见错误及解决：**

| 错误信息 | 原因 | 解决 |
|----------|------|------|
| `Port 8080 was already in use` | 端口被占用 | 修改端口或停止占用进程 |
| `Cannot determine embedded database` | 缺少数据库配置 | 检查application.yml数据库配置 |
| `Failed to configure a DataSource` | 数据库连接失败 | 检查数据库URL、用户名、密码 |
| `OutOfMemoryError` | 内存不足 | 调整JVM堆内存参数 |
| `NoClassDefFoundError` | 缺少依赖 | 检查jar包完整性，重新打包 |

### 2. 应用运行中异常

**诊断步骤：**

```bash
# 1. 检查应用状态
bin/status.sh
# 或
systemctl status myapp

# 2. 查看实时日志
tail -f logs/application.log

# 3. 检查错误日志
tail -f logs/error.log

# 4. 检查健康状态
curl http://localhost:8080/actuator/health

# 5. 查看资源使用
top -p $(pgrep -f myapp)
```

## Log Analysis

### 日志分析命令

```bash
# 查看最近错误
grep "ERROR" logs/application.log | tail -20

# 统计各级别日志数量
grep -c "ERROR" logs/application.log
grep -c "WARN" logs/application.log
grep -c "INFO" logs/application.log

# 查看特定时间段日志
sed -n '/2024-03-18 10:00/,/2024-03-18 11:00/p' logs/application.log

# 查看异常堆栈
grep -A 20 "Exception" logs/application.log

# 查看特定请求
grep "requestId=xxx" logs/application.log
```

### 常见异常模式

**数据库连接池耗尽：**
```
ERROR c.z.h.p.HikariPool - HikariPool-1 - Thread starvation or clock leap detected
WARN  c.z.h.p.PoolBase - HikariPool-1 - Failed to validate connection
```
**解决：** 增加连接池大小或检查慢查询

**内存溢出：**
```
ERROR java.lang.OutOfMemoryError: Java heap space
ERROR java.lang.OutOfMemoryError: GC overhead limit exceeded
```
**解决：** 增加堆内存，检查内存泄漏

**线程阻塞：**
```
WARN  o.s.c.s.ResourceBundleMessageSource - ResourceBundle [messages] not found
ERROR o.a.t.u.n.NioEndpoint - Error running socket processor
```
**解决：** 检查死锁，使用jstack分析线程

## Performance Troubleshooting

### 1. CPU过高

```bash
# 找到Java进程ID
jps -l

# 查看线程CPU使用
top -H -p <pid>

# 导出线程堆栈
jstack <pid> > thread_dump.txt

# 将线程ID转换为16进制
printf "%x\n" <thread_id>

# 在thread_dump.txt中查找对应线程
```

### 2. 内存问题

```bash
# 查看内存使用
jmap -heap <pid>

# 查看GC情况
jstat -gc <pid> 1000 10

# 导出堆内存（谨慎使用，会暂停应用）
jmap -dump:format=b,file=heap_dump.hprof <pid>

# 分析堆文件（使用Eclipse MAT）
```

### 3. 慢查询排查

```bash
# 开启MySQL慢查询日志（临时）
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

# 或在my.cnf中配置
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 1
```

## Emergency Rollback

### 回滚方案

#### 方案1：快速回滚到上一版本（推荐）

```bash
#!/bin/bash
# rollback.sh - 快速回滚脚本

APP_NAME="myapp"
APP_DIR="/opt/$APP_NAME"
BACKUP_DIR="$APP_DIR/backup"

# 停止当前版本
bin/shutdown.sh
sleep 5

# 备份当前版本（可选）
mv "$APP_DIR/lib" "$BACKUP_DIR/lib-$(date +%Y%m%d%H%M%S)"

# 恢复上一版本
LATEST_BACKUP=$(ls -t $BACKUP_DIR | head -1)
cp -r "$BACKUP_DIR/$LATEST_BACKUP" "$APP_DIR/lib"

# 启动上一版本
bin/startup.sh

echo "Rollback completed!"
```

#### 方案2：使用systemd回滚

```bash
# 1. 停止服务
systemctl stop myapp

# 2. 恢复备份jar
cp /opt/backup/myapp-0.9.0.jar /opt/myapp/lib/myapp.jar

# 3. 启动服务
systemctl start myapp

# 4. 检查状态
systemctl status myapp
```

#### 方案3：Docker回滚

```bash
# 使用上一版本镜像
docker stop myapp
docker rm myapp
docker run -d --name myapp myapp:0.9.0

# 或使用docker-compose
docker-compose down
docker-compose up -d
```

### 回滚检查清单

- [ ] 停止当前版本
- [ ] 数据库是否需要回滚（谨慎！）
- [ ] 配置文件是否需要恢复
- [ ] 启动旧版本
- [ ] 验证功能正常
- [ ] 通知相关人员

## Common Issues

### 1. 数据库连接问题

**症状：** 应用启动后无法访问数据库

**排查：**
```bash
# 测试数据库连接
mysql -h <host> -u <user> -p -e "SELECT 1"

# 检查网络
telnet <db_host> 3306

# 检查防火墙
iptables -L | grep 3306
```

**解决：**
- 检查数据库服务状态
- 检查连接配置
- 检查网络连通性
- 检查防火墙规则

### 2. 内存不足

**症状：** 应用频繁GC或OOM

**排查：**
```bash
# 查看内存使用
free -h

# 查看JVM内存
jmap -heap <pid>

# 查看GC日志
tail -f logs/gc.log
```

**解决：**
- 增加物理内存
- 调整JVM参数：`-Xms2g -Xmx2g`
- 检查内存泄漏
- 优化代码减少内存占用

### 3. 磁盘空间不足

**症状：** 无法写入日志，应用异常

**排查：**
```bash
# 查看磁盘使用
df -h

# 查看大文件
find /opt/myapp -type f -size +100M

# 查看日志目录大小
du -sh logs/
```

**解决：**
- 清理旧日志
- 调整日志保留策略
- 扩容磁盘

### 4. 高CPU使用

**症状：** 系统负载高，响应慢

**排查：**
```bash
# 查看CPU使用
top

# 查看Java线程
top -H -p <java_pid>

# 线程堆栈分析
jstack <pid> | grep -A 10 "0x<hex_thread_id>"
```

**解决：**
- 检查死循环
- 优化算法
- 增加缓存
- 扩容服务器

## Prevention Measures

### 1. 监控告警

```bash
# 磁盘监控脚本（加入crontab）
#!/bin/bash
USAGE=$(df -h /opt/myapp | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$USAGE" -gt 80 ]; then
    echo "Disk usage is ${USAGE}%" | mail -s "MyApp Alert" admin@example.com
fi
```

### 2. 定期维护

```bash
# 维护脚本
#!/bin/bash
# maintenance.sh

# 清理旧日志
find /opt/myapp/logs -name "*.log.*" -mtime +30 -delete

# 检查磁盘
./disk-check.sh

# 备份当前版本
cp -r /opt/myapp/lib /opt/backup/lib-$(date +%Y%m%d)

# 保留最近10个备份
ls -t /opt/backup | tail -n +11 | xargs rm -rf
```

### 3. 应急预案

```markdown
## 应急响应流程

### 级别1：应用不可用
1. 立即通知团队
2. 尝试重启：bin/shutdown.sh && bin/startup.sh
3. 如无效，执行回滚
4. 记录故障时间、现象、处理过程

### 级别2：性能严重下降
1. 收集性能数据（top, jstack, jmap）
2. 尝试重启缓解
3. 分析根因
4. 实施修复或扩容

### 级别3：部分功能异常
1. 隔离故障功能（如可能）
2. 查看错误日志
3. 尝试热修复或配置调整
4. 计划版本修复
```

## Output Location

```
bin/
├── troubleshooting.sh       # 诊断脚本
├── rollback.sh              # 回滚脚本
└── maintenance.sh           # 维护脚本

docs/
└── TROUBLESHOOTING.md       # 故障排查手册
```

## Emergency Contacts

```markdown
## 应急联系人

| 角色 | 姓名 | 电话 | 职责 |
|------|------|------|------|
| 运维负责人 | XXX | 138xxxx | 部署、回滚决策 |
| 开发负责人 | XXX | 139xxxx | 代码修复 |
| 业务负责人 | XXX | 137xxxx | 业务影响评估 |

## 常用命令速查

```bash
# 查看状态
systemctl status myapp
bin/status.sh

# 查看日志
tail -f logs/application.log
tail -f logs/error.log

# 重启
bin/shutdown.sh && bin/startup.sh
systemctl restart myapp

# 回滚
bin/rollback.sh
```
