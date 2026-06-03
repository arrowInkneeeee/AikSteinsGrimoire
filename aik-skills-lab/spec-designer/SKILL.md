---
name: spec-designer
description: 当需要根据PRD产出完整的系统设计文档（SDD）时使用。适用于"设计系统架构"、"技术方案设计"、"系统设计"、"架构设计"、"出设计方案"等场景。
type: Sub-agent
version: 1.0.0
---

# spec-designer

## 职责

1. 接收 PRD 输入
2. 协调各原子技能执行
3. 整合设计输出为 SDD 文档
4. 确保设计完整性

## 执行流程

```
PRD 输入
    ↓
[检查项目现有结构]
    ↓
architecture-designer      # 输出：架构设计
    ↓
database-designer          # 输出：数据库设计
    ↓
api-designer               # 输出：接口设计
    ↓
process-designer           # 输出：流程设计
    ↓
tech-solution-selector     # 输出：技术选型
    ↓
design-review-checker      # 输出：评审报告
    ↓
整合输出 SDD 文档
```

## 调用的技能

| 顺序 | 技能名称 | 用途 |
|------|---------|------|
| 1 | architecture-designer | 架构分层设计 |
| 2 | database-designer | 数据库设计 |
| 3 | api-designer | 接口设计 |
| 4 | process-designer | 核心流程设计 |
| 5 | tech-solution-selector | 技术方案选型 |
| 6 | design-review-checker | 设计评审检查 |

## 项目结构检查

设计开始前，必须检查：

```bash
# 检查项目结构
find src -type d -name "common" -o -name "util" -o -name "config" | head -20

# 检查已有通用类
grep -r "class Result" --include="*.java" src/
grep -r "class BaseEntity" --include="*.java" src/
grep -r "class PageDTO" --include="*.java" src/

# 检查 pom.xml 依赖
cat pom.xml | grep -E "(redis|rabbitmq|redisson|kafka)"
```

## 输出 SDD 结构

```markdown
# 系统设计文档 (SDD)

> 生成时间: 2026-03-18
> 版本: v1.0
> 基于 PRD: [PRD 版本]

---

## 1. 架构设计
[architecture-designer 输出]

### 1.1 技术栈
### 1.2 包结构
### 1.3 分层职责
### 1.4 通用组件（复用项目已有）
### 1.5 模块划分

---

## 2. 数据库设计
[database-designer 输出]

### 2.1 ER 图
### 2.2 表结构设计
### 2.3 索引设计
### 2.4 MyBatis-Plus 实体类
### 2.5 建表 SQL

---

## 3. 接口设计
[api-designer 输出]

### 3.1 API 列表
### 3.2 请求/响应定义
### 3.3 错误码定义

---

## 4. 核心流程设计
[process-designer 输出]

### 4.1 业务流程时序图
### 4.2 状态机设计
### 4.3 事务设计

---

## 5. 技术方案选型
[tech-solution-selector 输出]

### 5.1 缓存方案
### 5.2 消息队列
### 5.3 分布式锁
### 5.4 异步处理

---

## 6. 设计评审
[design-review-checker 输出]

### 6.1 评审结论
### 6.2 待解决问题
### 6.3 风险点
### 6.4 优化建议

---

## 7. 附录

### 7.1 命名规范
- 表名：大写下划线，T_前缀
- 字段：大写下划线
- 索引：IDX_/UK_前缀
- Java 类：驼峰命名

### 7.2 项目复用清单
| 组件 | 来源 | 说明 |
|------|------|------|
| Result | 项目已有 | com.xxx.common.result.Result |
| BaseEntity | 项目已有 | com.xxx.common.base.BaseEntity |
| PageDTO | 项目已有 | com.xxx.common.base.PageDTO |

### 7.3 代码示例
[关键代码示例]
```

## 使用示例

### 启动命令

```
请帮我设计以下需求的系统架构：

[粘贴 PRD 内容]

项目技术栈：Java 8 + Spring Boot + MyBatis-Plus + MySQL
```

### 完整执行示例

**用户输入**：
```
基于以下 PRD 设计系统：

## 用户故事
- US-001: 作为顾客，我想浏览商品列表
- US-002: 作为顾客，我想将商品加入购物车
- US-003: 作为顾客，我想下单支付

## 技术栈
Java 8 + Spring Boot 2.7 + MyBatis-Plus + MySQL + Redis
```

**执行流程**：

1. **检查项目结构**
   - 发现项目已有 Result、BaseEntity、PageDTO
   - 发现已有 Redis、RabbitMQ 依赖

2. **architecture-designer**
   - 设计包结构：controller/service/mapper/entity/do/dto/vo/common
   - 复用项目 Result、BaseEntity
   - 模块划分：商品模块、购物车模块、订单模块

3. **database-designer**
   - 设计表：T_PRODUCT、T_CART、T_ORDER、T_ORDER_ITEM
   - 索引设计：IDX_USER_ID、UK_ORDER_NO 等
   - 生成 MyBatis-Plus 实体类

4. **api-designer**
   - API 列表：GET /products、POST /cart、POST /orders 等
   - 复用项目 Result 和 PageDTO
   - 定义错误码

5. **process-designer**
   - 下单流程时序图
   - 订单状态机：待支付 -> 已支付 -> 已发货 -> 已完成
   - 事务边界标注

6. **tech-solution-selector**
   - 缓存：Redis（项目已有）
   - 延迟队列：RabbitMQ（项目已有）
   - 分布式锁：Redisson（建议引入）

7. **design-review-checker**
   - 评审结论：通过，2 个警告
   - 警告：缺少 CREATE_BY 字段、存在 N+1 查询风险
   - 建议：添加索引、使用 JOIN 优化查询

8. **整合输出 SDD**
   - 生成完整系统设计文档

## 注意事项

- 设计前必须检查项目现有结构和组件
- 所有通用组件优先复用，不重复创建
- 多模块项目参考项目习惯，新项目实施时询问
- 设计评审问题必须记录并跟踪
- SDD 文档应导出保存，便于后续开发参考
