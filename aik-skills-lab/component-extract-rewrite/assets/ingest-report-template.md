# 知识摄取运行报告 · {runId}

> 生成时间：{YYYY-MM-DD HH:mm} | 技能：`component-extract-rewrite` | 会话 workspace：{sessionWorkspace}
> 魔典：`{grimoirePath}`（envType={envType}, head={head}, remote={remote}）
> 结论：{成功/部分成功/失败}（新建 {created} / 跳过 {skipped} / 冲突 {conflicts} / 失败 {failed}）

## 1. 定位与环境

| 项 | 值 |
|---|---|
| 环境类型 | {wsl2 / windows / vm-shared / vm-isolated / container} |
| 定位来源 | {cwd / anchor / env / scan / user} |
| 魔典路径 | `{grimoirePath}` |
| 写入策略 | {以魔典为 workspace / 用户审批越界写} |

## 2. 源清单

| # | 类型 | 来源 | 采集时间 | 许可/备注 |
|---|---|---|---|---|
| 1 | {local-repo/workspace-doc/web-page/pasted-text} | {路径或 URL} | {时间} | {许可/限制} |

## 3. 知识条目

| # | 类型 | code | 目标路径 | SHA-256（前 12） | 决策 | 入库状态 |
|---|---|---|---|---|---|---|
| 1 | {NOTE/COMPONENT/SOLUTION/CODE} | {code} | `{target}` | `{sha256:0:12}` | {新建/跳过/冲突} | {pending/generated/inserted/skipped} |

冲突/跳过原因：

- {条目 code}：{原因，例如"目标文件已存在且内容不同，未覆盖"}

## 4. 索引更新

| 索引文件 | 变更 |
|---|---|
| `grimoire-scrolls/README.md` | {追加/无} |
| `src/.../solutions/README.md` | {追加/无} |
| 组件包 README | {追加/无} |

## 5. 元数据入库

- [ ] 已生成 SQL 草案：`grimoire-files/plans/ingest-reports/{runId}.sql`
- [ ] 已执行（`db=apply`）：{是/否}；影响行数 {n}；事务 {成功/失败}
- [ ] 回滚 SQL：{路径/无}

## 6. 校验结果

- [ ] 文件存在且可读
- [ ] SHA-256 与 manifest 一致
- [ ] 无同 code/slug 冲突、无覆盖
- [ ] 索引已更新
- [ ] 无凭据/隐私残留
- [ ] 报告已归档

## 7. 未处置项与风险

- {例如：网页 A 需要登录，已跳过}
- {例如：分类 ID 未确定，等待用户指定}

## 8. 变更清单（本次实际写入）

| 文件 | 操作 | 说明 |
|---|---|---|
| `{path}` | 创建/修改 | {摘要} |
