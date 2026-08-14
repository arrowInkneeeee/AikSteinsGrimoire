---
name: bundle-structure
description: 交接包固定目录结构、资产分类规则与持久化复制规范，供 handoff-bundle 按需加载
---

# 交接包结构与资产分类

## 固定输出结构

```text
handoff_bundle/
├── HANDOFF.md
├── ASSET_MANIFEST.json
├── NEXT_TASK_PROMPT.md
├── git-status.txt
├── changes.patch
├── checksums.sha256
├── assets/
│   ├── images/
│   ├── documents/
│   └── references/
└── README.md
```

## 分类规则

| 分类 | 存放位置 | 适用文件 |
|------|---------|---------|
| images | `assets/images/` | 常见图片（png/jpg/jpeg/gif/webp/svg/bmp 等） |
| documents | `assets/documents/` | PDF、Word、Excel、PPT、Markdown、TXT、CSV、JSON、HTML 等 |
| references | `assets/references/` | 视频、音频、源码、数据目录和其他文件 |

分类只影响存放位置；真实用途和媒体类型必须写入 ASSET_MANIFEST.json。

## 资产优先级

| 优先级 | 含义 | 缺失处理 |
|--------|------|---------|
| `required` | 没有它就无法可靠继续或验收任务 | 阻断交接，status 标为 missing |
| `reference` | 用于佐证、比较、风格参考或回溯 | 可交接，但必须在 warnings 中警告 |

## 复制与持久化规范

1. 在工作区或用户指定的持久化目录中创建新的 `handoff_bundle/`。
2. 目标已存在时不覆盖；使用带时间戳的新目录，或询问用户如何处理旧包。
3. 资产复制到相应分类目录，保留原始扩展名。
4. 同名不同内容的文件使用短哈希后缀避免覆盖。
5. 内容完全相同的文件可以去重，但清单中必须保留每个来源与用途；建议在清单中显式标 `deduplicated_from` 指向首个副本，避免校验表"一行一文件"语义模糊。
6. 临时目录中的附件必须复制到包内；清单不能只保留随时可能失效的临时路径。
7. 禁止复制名称明显属于敏感凭据的文件：`.env`、私钥、Cookie、credential、token、password 或 secret 文件。
8. 目录可以作为资产，但只能递归复制正常文件，并排除 `.git`、`.svn`、`.hg`、`node_modules`、虚拟环境和缓存目录。不得跟随符号链接进入未知目录。
9. `destination_name` 必须是普通文件名，禁止包含 `..`、绝对路径或目录穿越片段。

## 敏感文件检测边界

自动敏感文件检测（按文件名匹配 `.env`/私钥/Cookie/token/password/secret）只是第一道防线，不能替代人工审核。生成 README.md 时必须声明此限制。
