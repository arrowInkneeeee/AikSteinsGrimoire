SET NAMES utf8mb4;
-- DRY-RUN 草案：未执行。按 uk_code 幂等；执行前请先备份。
INSERT INTO aik_knowledge
  (id, title, code, type, summary, content, source_project, source_path, resource_path, ext_json, category_id, status)
VALUES
  (1288834972714496020, 'Conventional Commits 速记', 'conventional-commits', 1, '提交信息采用 <type>(<scope>): <description> 结构；fix→PATCH、feat→MINOR、BREAKING CHANGE→MAJOR，可据此自动生成 CHANGELOG 与推导版本号。',
   '# Conventional Commits 速记

> 类型：NOTE | 来源：https://www.conventionalcommits.org/en/v1.0.0/ | 采集：2026-11-11 | 许可：CC BY 3.0

## 核心结论

- 提交信息结构：`<type>[optional scope]: <description>`，其后可选 body 与 footer。
- `fix` 对应 SemVer 的 PATCH，`feat` 对应 MINOR；`BREAKING CHANGE`（footer 或 type 后的 `!`）对应 MAJOR。
- 其他 type（`build`/`chore`/`ci`/`docs`/`style`/`refactor`/`perf`/`test` 等）允许使用，但不隐含版本语义。
- scope 放在括号中，例如 `fix(parser):`；footer 采用 git trailer 风格，token 用 `-` 代替空格（`BREAKING CHANGE` 例外）。

## 要点 / 步骤

1. 每次提交只做一件事，选一个最贴切的 type。
2. 描述用一句话说清"改了什么"，细节放 body。
3. 破坏性变更在 type 后加 `!`，或在 footer 写 `BREAKING CHANGE: <说明>`。
4. 工具可据此自动生成 CHANGELOG、推导版本号、触发发布流程。

## 适用场景与限制

- 适合：多人协作、需要自动生成变更日志/版本号的项目。
- 限制：规范本身不定义 revert 的提交格式；type 大小写需团队自行约定并保持一致。

## 参考

- [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/)（采集于 2026-11-11，CC BY 3.0）
', 'conventionalcommits.org',
   'https://www.conventionalcommits.org/en/v1.0.0/',
   'grimoire-scrolls/conventional-commits/',
   '{"sourceUrl":"https://www.conventionalcommits.org/en/v1.0.0/","fetchedAt":"2026-11-11T11:26:00+08:00","license":"CC BY 3.0","tags":["git","commit","versioning"]}', NULL, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title), type = VALUES(type), summary = VALUES(summary),
  content = VALUES(content), source_project = VALUES(source_project),
  source_path = VALUES(source_path), resource_path = VALUES(resource_path),
  ext_json = VALUES(ext_json), category_id = VALUES(category_id), status = 1;
