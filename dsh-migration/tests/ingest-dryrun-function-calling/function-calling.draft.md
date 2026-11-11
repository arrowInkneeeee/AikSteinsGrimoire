# Function Calling：模型与工具之间的桥

> 类型：NOTE | 来源：https://nengyongai.cn/learn/01-vibecoding-learning/01-what-is-vibecoding/function-calling | 站点：能用AI | 采集：2026-11-11 | 说明：免费章节，本文为中文总结归纳，非全文搬运

## 核心结论

- 模型本身只会"文字进、文字出"，不能读磁盘、不能执行命令；Function Calling 是模型与真实执行之间的**结构化出口**。
- 模型不执行工具：它输出「工具名 + 参数」（`tool_use`），真正的执行在客户端程序里；权限、校验、执行都由客户端负责。
- 工具清单（`tools` + `input_schema`）是**每次请求的一部分**，不是模型记忆；不在清单里的工具，模型不知道它存在。
- 一次调用要走两轮请求：带工具清单 → 拿 `tool_use` → 客户端执行 → 带 `tool_result` 再请求 → 拿最终回答；`tool_use_id` 必须一一配对。
- Agent = 模型 + 工具 + 循环 + 上下文管理 + 权限；Function Calling 只是其中"桥"的一段，不是 Agent 本身。

## 要点

1. **请求结构**：`tools` 里每个工具包含 `name`、`description`、`input_schema`；描述决定模型何时选它。
2. **模型回复**：可能是"一句人话 + 一个/多个 `tool_use`"；流式输出时参数要拼完整、校验通过后才执行。
3. **客户端四步**：校验参数 → 过权限 → 真正执行 → 把结果包成 `tool_result` 发回；少一条配对结果，下一次请求会报错。
4. **并发**：一次回复可包含多个工具调用；只读工具可并行，会改东西的通常串行（具体由客户端决定）。
5. **Claude Code 里一切皆工具**：读文件 `Read`、改文件 `Edit/Write`、跑命令 `Bash`、搜内容 `Grep/Glob`、查网页 `WebFetch/WebSearch`、MCP 工具 `mcp__*`，对模型而言都是清单上的同一种东西。
6. **上下文成本**：工具定义本身占上下文（如 `/context` 里的工具定义一栏），工具越多、描述越重叠，模型越容易选错。
7. **可靠性**：模型会挑错工具、编出不存在的工具名、填错参数；需要参数校验、失败重试，并让它看到错误后自我修正。
8. **历史**：2023 年 OpenAI API 引入 function calling，Anthropic 用 `tools` + `tool_use` 实现同一思路，2024 年底 MCP 推进工具的发现与共享；Function Calling / Tool Calling / Tool Use 常指同一件事。

## 适用场景与限制

- 适用：理解 Agent 工具机制、排查"工具选错/做了不该做的事"、理解上下文与权限边界。
- 限制：这是概念性拓展阅读，不是实现指南；具体工具名、schema、权限模型以各客户端实际实现为准。
- 排错方向：工具选错 → 先看描述与工具数量；做了不该做的事 → 先看权限与工具清单，而不是只改提示词。

## 参考

- [Function Calling（拓展阅读）· 能用AI](https://nengyongai.cn/learn/01-vibecoding-learning/01-what-is-vibecoding/function-calling)（采集于 2026-11-11）
