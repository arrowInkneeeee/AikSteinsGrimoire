# AIK_GRIMOIRE_HOME 定位链

## 定位链（按优先级，命中即止）

```powershell
# Step 1 — 读环境变量缓存（命中即止，日常零开销）
$g = [Environment]::GetEnvironmentVariable('AIK_GRIMOIRE_HOME','User')
# 验证 = ($g 非空) AND (Test-Path "$g\grimoire-files\component-manuals")
# 有效 → 基址 = $g，结束
# 无效 → 进 Step 2

# Step 2 — 当前工作空间直查 + 自动写缓存
# 检查 {workspace} 的父级目录中是否存在 AikSteinsGrimoire
# 或检查 {workspace} 自身是否是 AikSteinsGrimoire
# 存在 → 基址 = 命中路径
#   自动写缓存：[Environment]::SetEnvironmentVariable('AIK_GRIMOIRE_HOME', '{基址}', 'User')
#   结束

# Step 3 — 全局搜索兜底 + 自动写缓存
# 优先：utools.everythingfind 搜 "grimoire-files" 目录名（需 Everything）
# 备选：在常见开发目录搜（D:\JeBrainsWorkSpace\ 等）
# 命中后验证 $g\grimoire-files\component-manuals 存在
# 基址 = 命中目录
#   自动写缓存（同 Step 2）
#   结束

# Step 4 — 全失败 → 上报
# 不擅自猜测路径，告知"定位失败，请提供真实目录或手动设 AIK_GRIMOIRE_HOME"
```

## 4 种内容类型

| 类型标记 | 含义 | 对应 KnowledgeType | Phase 1 输入 |
|---------|------|-------------------|-------------|
| **NOTE** | 笔记/文章/学习资料/使用说明 | NOTE(1) | 非代码资料来源（URL/文档/已有资料） |
| **COMPONENT** | 可复用组件 | COMPONENT(2) | 代码范围（文件/包/类/方法）+ 粒度 |
| **SOLUTION** | 解决方案 | SOLUTION(3) | 代码范围（文件/包/类/方法）+ 粒度 |
| **CODE** | 代码片段 | CODE(4) | 代码范围（文件/包/类/方法）+ 粒度 |

### NOTE 类型的特殊性

- Phase 1 不需要 gitnexus 索引（非代码资料）
- Phase 2 不需要脱敏和代码审查（非代码产物）
- 产物直接保存为 `src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md`

## 产物路径解析表

定位成功后，所有产物路径基于 `$GRIMOIRE` 解析：

| 阶段 | 类型 | 产物路径 |
|------|------|---------|
| Phase 1 | NOTE | `$GRIMOIRE/src/main/java/io/aik/steins/grimoire/scrolls/{name}/README.md` |
| Phase 1 | COMPONENT/SOLUTION/CODE | `$GRIMOIRE/grimoire-files/component-manuals/{code}.md` |
| Phase 2 | NOTE | 同 Phase 1（NOTE 类型 Phase 2 仅做元数据补充） |
| Phase 2 | COMPONENT | `$GRIMOIRE/src/main/java/io/aik/steins/grimoire/components/{code}/` |
| Phase 2 | SOLUTION | `$GRIMOIRE/src/main/java/io/aik/steins/grimoire/solutions/{code}/` |
| Phase 2 | CODE | 存入 `aik_knowledge.content` 字段，不生成文件 |
| Phase 3 | 全部 | `$GRIMOIRE/sql/insert_{code}.sql` |

## 索引更新

产物落地后，同步更新以下索引文件（均在 `$GRIMOIRE` 下）：

| 索引文件 | 更新内容 |
|---------|---------|
| `grimoire-files/README.md` | component-manuals 目录树追加新条目 |
| `src/.../solutions/README.md` | 当前方案列表追加新条目 |
| `src/.../scrolls/README.md` | 卷轴列表追加新条目（NOTE 类型） |

## 定位失败的处理

Step 4 全失败时，产物临时保存在当前工作空间 `doc/_grimoire_pending/` 下，并告知：
- "AIK_GRIMOIRE_HOME 定位失败，产物暂存在 doc/_grimoire_pending/，请提供真实目录或设置环境变量"

## 自动写缓存的门禁

- 作用域：User（不要管理员，不碰系统级）
- 可逆：`[Environment]::SetEnvironmentVariable('AIK_GRIMOIRE_HOME',$null,'User')`
- 生效时机：新进程读到（当前终端不立即生效）
- 事后告知：在阶段完成报告中注明"本次自动设置/更新了 AIK_GRIMOIRE_HOME"

## 类型判定规则

- 若用户明确说"笔记"、"文章"、"学习资料"、"整理到 scrolls" → 标记为 **NOTE**
- 若用户指定代码范围并要求生成组件 → 标记为 **COMPONENT**
- 若用户指定代码范围并要求生成方案 → 标记为 **SOLUTION**
- 若用户指定代码范围并要求提取片段 → 标记为 **CODE**
