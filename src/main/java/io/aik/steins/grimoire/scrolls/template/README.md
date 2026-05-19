# 卷轴模板

> 复制此目录作为新卷轴的起点

## 目录结构

```
{scroll-name}/
└── README.md              # 卷轴正文（Markdown）
```

## 元数据关联

卷轴元数据通过 `knowledge` 模块管理：
- `type` = 1（卷轴）
- `resource_path` = `scrolls/{scroll-name}/`
- `content` 字段可直接存储 Markdown 正文，或引用此目录下的 README.md
