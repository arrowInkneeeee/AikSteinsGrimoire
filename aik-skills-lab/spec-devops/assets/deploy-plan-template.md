# 部署计划

> 生成时间: {yyyy-MM-dd}
> 版本: v{version}
> 项目: {项目名称}

---

## 一、项目信息

| 项目 | 内容 |
|------|------|
| 项目名称 | {项目名称} |
| 部署版本 | v{version} |
| 部署日期 | {yyyy-MM-dd} |
| 部署负责人 | {name} |
| 回滚负责人 | {name} |

---

## 二、部署方式

| 方案 | 部署方式 | 适用环境 | 优先级 |
|------|---------|---------|--------|
| 主方案 | Systemd 服务部署 (Jar) | 生产、测试 | 主 |
| 备选方案 | Docker 容器化部署 | 生产、测试 | 备 |
| 开发方案 | IDEA 直接运行 | 开发 | - |

---

## 三、环境信息

| 环境 | 服务器IP | 部署目录 | 端口 | 服务名 | 配置来源 |
|------|---------|---------|------|--------|---------|
| 测试 | {IP} | /opt/{app} | 8080 | {app}-test | 外置application.yml |
| 生产 | {IP} | /opt/{app} | 8080 | {app}-prod | 外置application.yml |

---

## 四、依赖服务

| 服务 | 版本 | 地址 | 说明 |
|------|------|------|------|
| MySQL | 8.0 | {host:port} | 数据库 |
| Redis | 7.0 | {host:port} | 缓存 |
| RabbitMQ | 3.x | {host:port} | 消息队列 |
| Nacos | 2.x | {host:port} | 配置中心（如有） |

---

## 五、部署产物

| 产物 | 路径 | 说明 |
|------|------|------|
| Jar包 | target/{app}-{version}.jar | 可执行 Jar |
| Tar包 | target/{app}-{version}.tar.gz | 包含启动脚本和配置 |
| 配置文件 | config/application.yml | 外置配置文件 |
| 启动脚本 | bin/startup.sh | 启动脚本 |
| 停止脚本 | bin/shutdown.sh | 停止脚本 |

---

## 六、部署步骤

### 6.1 部署前检查

- [ ] 代码已合并到发布分支
- [ ] 版本号已更新（pom.xml）
- [ ] 所有测试通过
- [ ] 覆盖率达标
- [ ] 数据库迁移脚本已准备
- [ ] 配置文件已审核
- [ ] 备份策略已确认
- [ ] 维护窗口已通知

### 6.2 备份现有版本

```bash
# 备份当前版本
tar -czf /opt/backup/{app}-{old_version}-{date}.tar.gz /opt/{app}/

# 备份数据库（如有结构变更）
mysqldump -h {host} -u {user} -p {database} > /opt/backup/db_{date}.sql
```

### 6.3 部署新版本

```bash
# 1. 上传部署包
scp {app}-{version}.tar.gz {user}@{server}:/opt/releases/

# 2. 解压
cd /opt/releases/
tar -xzf {app}-{version}.tar.gz -C /opt/{app}/

# 3. 更新配置文件
vim /opt/{app}/config/application.yml

# 4. 停止旧版本
systemctl stop {app}
# 或
/opt/{app}/bin/shutdown.sh

# 5. 启动新版本
systemctl start {app}
# 或
/opt/{app}/bin/startup.sh
```

### 6.4 部署后验证

- [ ] 应用启动成功（查看日志）
- [ ] 健康检查通过：`curl http://localhost:8080/actuator/health`
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] 核心接口可访问
- [ ] 日志无ERROR

### 6.5 功能验证

| 功能 | 验证方式 | 预期结果 | 实际结果 | 状态 |
|------|---------|---------|---------|------|
| {功能1} | {验证方法} | {预期} | - | [ ] |
| {功能2} | {验证方法} | {预期} | - | [ ] |

---

## 七、回滚计划

### 回滚触发条件

- 部署后核心功能不可用
- 出现 P0 级别缺陷
- 系统性能严重下降
- 数据库迁移失败

### 回滚步骤

```bash
# 1. 停止新版本
systemctl stop {app}

# 2. 恢复旧版本
rm -rf /opt/{app}/
tar -xzf /opt/backup/{app}-{old_version}-{date}.tar.gz -C /opt/{app}/

# 3. 回滚数据库（如有需要）
mysql -h {host} -u {user} -p {database} < /opt/backup/db_{date}.sql

# 4. 启动旧版本
systemctl start {app}

# 5. 验证
curl http://localhost:8080/actuator/health
```

### 回滚时间预估

| 步骤 | 预计时间 |
|------|---------|
| 停止服务 | 10秒 |
| 恢复旧版本文件 | 30秒 |
| 回滚数据库（如需要） | 2-5分钟 |
| 启动服务 | 30秒 |
| 验证 | 1分钟 |
| **合计** | **约 5分钟** |

---

## 八、监控与告警

### 健康检查

```bash
# 健康检查端点
curl http://localhost:8080/actuator/health

# 详细信息
curl http://localhost:8080/actuator/info
```

### 日志监控

```bash
# 查看应用日志
tail -f /opt/{app}/logs/application.log

# 查看错误日志
tail -f /opt/{app}/logs/error.log

# 搜索错误
grep -i "error" /opt/{app}/logs/application.log
```

### 资源监控

```bash
# CPU/内存
top -p $(pgrep -f {app})
htop

# 磁盘
df -h /opt/{app}

# 端口
netstat -tlnp | grep 8080
```

---

## 九、应急预案

### 场景1：应用无法启动

1. 查看启动日志：`tail -n 200 /opt/{app}/logs/application.log`
2. 检查端口占用：`netstat -tlnp | grep 8080`
3. 检查配置文件：`cat /opt/{app}/config/application.yml`
4. 检查Java版本：`java -version`
5. 尝试手动启动：`java -jar {app}.jar`

### 场景2：数据库连接失败

1. 检查网络：`ping {db_host}`
2. 检查端口：`telnet {db_host} 3306`
3. 检查配置文件中数据库连接信息
4. 检查数据库服务状态

### 场景3：CPU/内存异常

1. 查看线程：`top -H -p $(pgrep -f {app})`
2. 导出线程转储：`jstack {pid} > thread_dump.txt`
3. 导出堆转储：`jmap -dump:format=b,file=heap.hprof {pid}`
4. 分析日志中的异常

---

## 十、部署检查清单

### 部署前

- [ ] 发布版本确认
- [ ] 数据库脚本备份
- [ ] 配置文件审核
- [ ] 维护窗口通知
- [ ] 回滚方案确认

### 部署中

- [ ] 旧版本备份成功
- [ ] 新版本上传成功
- [ ] 配置文件更新
- [ ] 应用启动成功
- [ ] 健康检查通过

### 部署后

- [ ] 功能验证通过
- [ ] 日志无异常
- [ ] 监控数据正常
- [ ] 通知相关人员
- [ ] 部署记录归档

---

## 十一、部署记录

| 版本 | 部署日期 | 环境 | 部署人 | 结果 | 备注 |
|------|---------|------|--------|------|------|
| v{version} | {yyyy-MM-dd} | 测试 | {name} | 成功 | - |
| v{version} | {yyyy-MM-dd} | 生产 | {name} | 成功 | - |

---

## 十二、联系信息

| 角色 | 姓名 | 电话 | 职责 |
|------|------|------|------|
| 运维负责人 | {name} | {phone} | 部署执行 |
| 开发负责人 | {name} | {phone} | 问题修复 |
| DBA | {name} | {phone} | 数据库变更 |
| 项目经理 | {name} | {phone} | 协调沟通 |

---

> 本文档由 spec-devops 生成，用于指导部署实施工作。
