# 决策档案

> 记录实际发生的决策，不做抽象推断。每条带日期，可追溯演变。
> 协议详见 [evolution-protocol.md](./evolution-protocol.md)

---

## 后端领域

| 日期 | 决策 | 结果 |
|------|------|------|
| 2026-07-30 | 订单状态机用枚举管理，不用状态字段 | confirmed |
| 2026-08-26 | KnowledgeController 路径前缀 `/api` → `/grimoire` 统一 | confirmed |
| 2026-08-26 | JacksonConfig 全局 Long 序列化为 String（雪花ID精度保护） | confirmed |
| 2026-08-26 | scrolls 从 `src/main/java/` 迁移到根目录 `grimoire-scrolls/` | confirmed |
| 2026-09-20 | 附件域采**两层模型**：`aik_sys_file` = 文件对象（一行 = 一份字节，`md5` 内容寻址），`aik_sys_attachment` = 通用挂载点（`file_id` + `biz_type` + `biz_id`） | confirmed |
| 2026-09-20 | `url` **全链路删除**：`aik_sys_file.url` 与 `aik_sys_attachment.attach_url` 两列、`FileStorageStrategy#getUrl` 接口方法与三个实现、两处持久化调用一并退役；访问统一走 `/grimoire/file/download` 下载/预览接口 | confirmed |
| 2026-09-20 | `aik_sys_attachment.knowledge_id` 删除，业务归属改由 `(biz_type, biz_id)` 表达；`aik_sys_file.del_flag` 摘除、文件表改**物理删除** | confirmed |
| 2026-09-20 | 删除契约：**删挂载行 → 查是否仍有有效挂载 → 无则删文件记录 + 删盘**（含行锁串行化；**提交后**才删盘） | confirmed |
| 2026-09-20 | md5 去重采**甲方案**：`idx_md5` 保持**非唯一**，去重是**应用层约定而非数据库约束**（否决乙方案：并发补偿需删已落盘孤儿，而删盘与插入不同事务，补偿失败反而留孤儿）〔船长裁定〕 | observed |
| 2026-09-20 | 下载响应文件名**取自挂载层** `attach_name`：`GET /file/download?id=&attachId=`，挂载模式强校（存在 / `del_flag=0` / `file_id==id`），台账模式 `attachId` 可省〔船长裁定方案 1〕 | observed |
| 2026-09-20 | 附件列表路径泛化为 `GET /grimoire/attachment/findByBiz?bizType=&bizId=`，**废弃** `findByKnowledgeId`（理由：路径含业务名 = 把刚删的 `knowledge_id` 硬编码从列搬到 URL）〔船长裁决①〕 | observed |
| 2026-09-20 | `AttachmentVo` **不回显** `bizType` / `bizId`（无消费方就不暴露，与对 `md5` 的判据一致）；但**表列不删**——不回显只是 API 暴露面决策〔船长裁决②〕 | observed |
| 2026-09-20 | R18：`AttachmentVo` **恢复** `fileSize` / `fileType`（知识详情页一直要求显示大小与类型图标 → 消费方始终存在，v1.2 以"无消费方"删除属**契约缺陷**）；数据由 `aik_sys_file` 关联带出、**按 fileId 批量取禁 N+1**〔船长裁定〕 | observed |

---

## 前端领域

| 日期 | 决策 | 结果 |
|------|------|------|
| 2026-07-31 | 新项目 Vue 3 + Element Plus + Pinia（不选 Vue 2） | confirmed |
| 2026-07-31 | 前端信任等级起步 Lv.2（用户明确不熟悉前端） | confirmed |

---

## 通用领域

| 日期 | 决策 | 结果 |
|------|------|------|
| 2026-07-30 | 交互风格：自然不矫情（不刻意正式，不强加礼仪） | confirmed |
| 2026-07-30 | 命名原则：功能性优先（文件名/变量名要一眼看出作用） | confirmed |
| 2026-07-31 | 讨论性措辞（讨论/分析/评估）= 先给方案不动手 | confirmed |
| 2026-07-31 | 权限受限操作：上报而非变通（不擅自替换方案） | confirmed |
| 2026-07-31 | AikSteinsGrimoire 项目：文件存入后必须 git add 暂存保护 | confirmed |
| 2026-07-31 | 严格按用户指令执行，不擅自扩大改动范围 | confirmed |
| 2026-08-26 | 项目简称体系扩展：asl / gf / ga / gs | confirmed |
| 2026-08-26 | 修改后自动 git add 暂存（执行遗漏被纠正，规则不变） | corrected |
| 2026-09-20 | 文档中引用代码行区间时，约定**含方法上 `@Override` 注解行**（如 `LocalFileStorage:82-85` = 注解行 + 声明 + 方法体）〔船长裁定；与 body-only 约定不得混用，归一化属集成收口〕 | observed |

---

## 状态标记说明

- **confirmed**：用户明确说"好"/"对"/"就这样"，或长期一致行为被多次观察
- **observed**：单次出现，未被明确确认
- **corrected**：用户明确说"不对"/"不是这样"/"以后别..."

## 领域说明

- **后端领域**：Java Spring Boot + MyBatis-Plus 后端开发相关决策
- **前端领域**：Vue 3 + Element Plus + Pinia 前端开发相关决策
- **通用领域**：交互风格、命名原则、工作流约束等跨领域决策
