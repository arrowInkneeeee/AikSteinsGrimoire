# Git 操作指南

> 结合项目实践经验与 Pro Git 官方文档整理  
> 适用场景：日常开发、团队协作、版本回退、高级操作

---

## 一、核心概念

Git 是分布式版本控制系统，理解以下四个区域是掌握 Git 的基础：

| 区域 | 英文 | 说明 | 对应命令 |
|------|------|------|----------|
| 工作区 | Working Directory | 你实际编辑的本地文件 | 直接编辑 |
| 暂存区 | Staging Area / Index | 准备提交的改动快照 | `git add` |
| 本地仓库 | Local Repository | `.git` 目录，保存完整历史 | `git commit` |
| 远程仓库 | Remote Repository | 如 GitHub、GitLab、Gitee 等 | `git push` / `git pull` |

### 数据流转图

```
工作区  --add-->  暂存区  --commit-->  本地仓库  --push-->  远程仓库
  ^                                               |
  |_________________pull/clone/fetch______________|
```

### 三种对象

- **Blob**：文件内容的二进制大对象
- **Tree**：目录结构快照，指向多个 Blob 或子 Tree
- **Commit**：提交记录，包含作者、时间、提交信息，指向一个 Tree 和父 Commit

---

## 二、日常开发流程

### 2.1 仓库初始化与克隆

```bash
# 新建本地仓库
git init

# 克隆远程仓库（HTTPS）
git clone https://github.com/username/repo.git

# 克隆指定分支
git clone -b main https://github.com/username/repo.git

# 克隆到指定目录
git clone https://github.com/username/repo.git my-folder
```

### 2.2 查看状态与差异

```bash
# 查看当前状态（最常用）
git status

# 查看已暂存与未暂存的差异
git diff                  # 工作区 vs 暂存区
git diff --cached        # 暂存区 vs 最新提交
git diff HEAD            # 工作区 vs 最新提交

# 简洁状态（分支名 + 文件列表）
git status -sb
```

### 2.3 添加与提交

```bash
# 添加单个文件
git add filename.txt

# 添加所有改动
git add .

# 交互式添加（按块选择）
git add -p

# 提交（进入编辑器写信息）
git commit

# 提交并直接写信息
git commit -m "fix: 修复空指针异常"

# 修改最后一次提交（未 push 时）
git commit --amend

# 跳过暂存区直接提交已跟踪文件（谨慎使用）
git commit -am "docs: 更新 README"
```

### 2.4 推送到远程

```bash
# 首次推送并建立上游分支关联
git push -u origin main

# 后续推送
git push

# 推送所有分支
git push --all origin

# 推送标签
git push origin --tags
```

### 2.5 拉取与同步

```bash
# 拉取远程更新并合并（fetch + merge）
git pull

# 拉取但不合并（推荐：先查看再决定）
git fetch origin

# 拉取特定分支
git fetch origin feature-x

# 查看远程分支
git branch -r

# 查看所有分支（本地 + 远程）
git branch -a
```

### 2.6 查看历史

```bash
# 简洁日志（单行）
git log --oneline

# 图形化分支历史
git log --oneline --graph --all

# 查看某文件的修改历史
git log -p filename.txt

# 查看某作者的提交
git log --author="张三"

# 查看最近 N 条
git log -5

# 统计每个人的提交数
git shortlog -sn
```

---

## 三、分支管理

### 3.1 基础分支操作

```bash
# 查看本地分支
git branch

# 查看远程分支
git branch -r

# 创建新分支
git branch feature-x

# 创建并切换到新分支
git checkout -b feature-x
# 或（Git 2.23+ 推荐）
git switch -c feature-x

# 切换分支
git checkout main
# 或
git switch main

# 删除已合并的分支
git branch -d feature-x

# 强制删除未合并的分支
git branch -D feature-x

# 重命名分支
git branch -m old-name new-name
```

### 3.2 合并（Merge）

```bash
# 将 feature-x 合并到当前分支
git merge feature-x

# 快进合并（Fast-forward）
# 当前分支无新提交时，直接移动指针

# 非快进合并（保留分支历史）
git merge --no-ff feature-x

# 中止合并（解决冲突前想放弃）
git merge --abort
```

### 3.3 变基（Rebase）

```bash
# 将当前分支变基到 main 最新提交之上
git rebase main

# 交互式变基（整理提交历史）
git rebase -i HEAD~3

# 常用交互式命令：
# p, pick     = 保留提交
# r, reword   = 修改提交信息
# e, edit     = 暂停修改
# s, squash   = 合并到上一个提交
# d, drop     = 删除提交
# f, fixup    = 类似 squash 但丢弃提交信息

# 中止变基
git rebase --abort

# 继续变基（解决冲突后）
git rebase --continue
```

**Merge vs Rebase 的选择**：

| 场景 | 推荐方式 | 原因 |
|------|----------|------|
| 个人功能分支整合到主分支 | Rebase | 保持线性历史，便于阅读 |
| 公共分支（多人协作） | Merge | 不修改已推送的历史 |
| 需要保留完整上下文 | Merge | 保留分支存在过的痕迹 |
| 代码审查前整理提交 | Rebase | 压缩、重排、清理提交 |

### 3.4 拣选（Cherry-pick）

```bash
# 将某次提交应用到当前分支
git cherry-pick abc1234

# 拣选多次提交
git cherry-pick abc1234 def5678

# 拣选但不自动提交
git cherry-pick -n abc1234
```

### 3.5 临时储藏（Stash）

```bash
# 储藏当前未提交的改动
git stash

# 储藏并写备注
git stash push -m "WIP: 用户模块重构"

# 查看储藏列表
git stash list

# 应用最近一次储藏（不删除）
git stash apply

# 应用最近一次储藏（删除）
git stash pop

# 应用指定储藏
git stash apply stash@{2}

# 删除指定储藏
git stash drop stash@{1}

# 清空所有储藏
git stash clear
```

---

## 四、团队协作

### 4.1 远程分支管理

```bash
# 查看远程仓库信息
git remote -v

# 添加远程仓库
git remote add upstream https://github.com/original/repo.git

# 更新远程分支列表
git remote prune origin

# 跟踪远程分支
git branch --set-upstream-to=origin/main main

# 删除远程分支
git push origin --delete feature-x
# 或简写
git push origin :feature-x
```

### 4.2 PR / MR 工作流程

```
1. 从主分支拉取最新代码
   git checkout main && git pull origin main

2. 创建功能分支
   git checkout -b feat/user-auth

3. 开发并提交
   git add . && git commit -m "feat(auth): 添加用户认证"

4. 推送到远程
   git push -u origin feat/user-auth

5. 在 GitHub/GitLab 上发起 Pull Request

6. 审查通过后合并到主分支

7. 删除本地和远程功能分支
   git branch -d feat/user-auth
   git push origin --delete feat/user-auth
```

### 4.3 冲突解决

当 `git merge` 或 `git pull` 遇到冲突时：

```bash
# 1. 查看冲突文件
git status

# 2. 编辑冲突文件，查找冲突标记 <<<<<<< ======= >>>>>>>
# 手动保留需要的代码，删除标记

# 3. 标记冲突已解决
git add <resolved-file>

# 4. 完成合并
git commit  # 或使用 git merge --continue

# 使用合并工具（如 VS Code）
git mergetool
```

### 4.4 回滚操作

```bash
# 撤销工作区的修改（未 add）
git checkout -- filename.txt

# 撤销暂存区的修改（已 add 未 commit）
git reset HEAD filename.txt

# 回退到指定版本（保留工作区修改）
git reset --soft HEAD~1

# 回退到指定版本（丢弃工作区修改）
git reset --hard abc1234

# 查看所有操作记录（用于找回）
git reflog
```

---

## 五、高级操作

### 5.1 Reset 三种模式详解

`git reset` 是 Git 中最强大也最危险的命令之一，三种模式的核心区别在于影响的范围：

| 模式 | HEAD | 暂存区 | 工作区 | 数据丢失风险 | 典型场景 |
|------|------|--------|--------|--------------|----------|
| `--soft` | 移动 | 保留 | 保留 | 无 | 撤销提交但保留修改，重新组织提交 |
| `--mixed`（默认） | 移动 | 重置 | 保留 | 无 | 取消暂存，重新选择要提交的文件 |
| `--hard` | 移动 | 重置 | 重置 | **高** | 彻底回退到某个历史状态 |

```bash
# Soft：撤销最后一次提交，修改回到暂存区
git reset --soft HEAD~1

# Mixed：撤销最后一次提交和暂存，修改保留在工作区
git reset --mixed HEAD~1
# 等价于
git reset HEAD~1

# Hard：彻底丢弃所有未提交的修改（慎用！）
git reset --hard HEAD~1
```

### 5.2 Reflog：操作保险箱

```bash
# 查看所有 HEAD 移动记录（本地操作日志）
git reflog

# 查看特定分支的 reflog
git reflog show main

# 找回误删的提交
git reset --hard abc1234  # 误操作
git reflog                  # 找到之前的 HEAD 位置
git reset --hard HEAD@{1}   # 恢复

# 找回误删的分支
git reflog | grep branch-name
git checkout -b recovered-branch abc1234
```

> Reflog 默认保留 90 天（未引用的提交 30 天），是找回误操作的最后防线。

### 5.3 交互式 Rebase

```bash
# 整理最近 5 次提交
git rebase -i HEAD~5

# 常用场景示例：
# 1. 合并多个小提交为一个
git rebase -i HEAD~3
# 将后两个 pick 改为 squash 或 fixup

# 2. 修改历史提交信息
git rebase -i HEAD~3
# 将目标提交的 pick 改为 reword

# 3. 拆分一个提交
git rebase -i HEAD~2
# 将目标提交的 pick 改为 edit
git reset HEAD^      # 取消该提交
git add -p           # 分块添加
git commit -m "第一部分"
git add .
git commit -m "第二部分"
git rebase --continue
```

### 5.4 子模块（Submodule）

```bash
# 添加子模块
git submodule add https://github.com/user/lib.git libs/lib

# 克隆包含子模块的仓库
git clone --recurse-submodules https://github.com/user/project.git

# 拉取主仓库 + 子模块更新
git pull && git submodule update --init --recursive

# 更新子模块到远程最新
git submodule update --remote

# 删除子模块（需多步）
# 1. 编辑 .gitmodules 删除对应节
# 2. git rm --cached libs/lib
# 3. rm -rf .git/modules/libs/lib
# 4. git commit -am "移除子模块"
```

### 5.5 大文件处理（Git LFS）

```bash
# 安装 Git LFS
git lfs install

# 追踪大文件类型
git lfs track "*.psd"
git lfs track "*.zip"
git lfs track "models/*.bin"

# 查看追踪规则
git lfs track

# 推送 LFS 对象
git lfs push origin main

# 查看 LFS 文件列表
git lfs ls-files
```

---

## 六、最佳实践

### 6.1 提交信息规范（Conventional Commits）

格式：`<type>(<scope>): <subject>`

**常用类型**：

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(auth): 添加 OAuth2 登录` |
| `fix` | 修复 Bug | `fix(api): 修复空指针异常` |
| `docs` | 文档修改 | `docs: 更新 API 文档` |
| `style` | 代码格式 | `style: 统一缩进为 4 空格` |
| `refactor` | 重构 | `refactor(service): 提取验证逻辑` |
| `perf` | 性能优化 | `perf(db): 添加索引优化查询` |
| `test` | 测试相关 | `test: 添加用户模块单元测试` |
| `chore` | 构建/工具 | `chore(ci): 更新 GitHub Actions` |
| `revert` | 回滚 | `revert: 撤销 feat(auth) 提交` |

**规范要点**：
- `type` 必须小写，使用预定义值
- `scope` 可选，用英文括号包裹，如 `(auth,api)`
- `subject` 首字母小写，结尾不加句号，长度建议不超过 50 字符
- 冒号后必须跟一个空格：`feat: add user` ✓，`feat:add user` ✗

### 6.2 分支策略

#### Git Flow

适合发布周期明确的项目：

```
main      ●────────●────────●────────●────────●
            ↑                                  ↑
develop   ●──●──●──●──●──●──●──●──●──●──●──●
               ↑        ↑        ↑
feature/*      ●──●──●  ●──●──●  ●──●──●
                          ↑
release/*                 ●──●
                                   ↑
hotfix/*                           ●──●
```

- `main`：生产分支，只接受 merge
- `develop`：开发主线
- `feature/*`：功能分支，从 develop 创建，完成后合并回 develop
- `release/*`：发布分支，从 develop 创建，完成后合并到 main 和 develop
- `hotfix/*`：紧急修复，从 main 创建，完成后合并到 main 和 develop

#### GitHub Flow

适合持续部署的项目：

```
main      ●────────●────────●────────●────────●
               ↑                 ↑
feature/*      ●──●──●          ●──●──●
```

- 只有一个长期分支 `main`
- 功能分支从 `main` 创建，通过 PR 审查后合并回 `main`
- 简洁、适合快速迭代

#### Trunk-based Development

适合高频发布的团队：

- 所有人直接在 `main` 或极短生命周期的分支上工作
- 提交频率极高（每天多次）
- 配合特性开关（Feature Flags）控制功能发布
- 对持续集成要求很高

### 6.3 .gitignore 配置

```gitignore
# 编译输出
/target/
/build/
/out/
/dist/
*.class

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo

# 依赖
/node_modules/
/vendor/

# 日志与临时文件
*.log
*.tmp
*.cache

# 操作系统
.DS_Store
Thumbs.db

# 环境配置（含敏感信息）
.env
.env.local
application-local.yml

# 测试报告
/coverage/
*.lcov
```

**忽略已跟踪的文件**：

```bash
# 1. 添加到 .gitignore
# 2. 从暂存区移除但保留本地文件
git rm --cached filename
# 3. 提交
git commit -m "chore: 移除已跟踪的敏感配置文件"
```

---

## 七、常见问题速查

### 7.1 误删恢复

```bash
# 误删工作区文件（未 commit）
git checkout -- filename

# 误删分支
# 先找到分支最后的 commit
git reflog
git checkout -b recovered-branch <commit-hash>

# 误用 reset --hard
# reflog 找到重置前的 HEAD
git refloc
git reset --hard HEAD@{1}

# 误删已 push 的提交（需团队协调）
# 找到 commit hash，然后 cherry-pick 或 reset
```

### 7.2 修改提交信息

```bash
# 修改最后一次提交信息
git commit --amend

# 修改历史提交信息（未 push）
git rebase -i HEAD~3
# 将 pick 改为 reword

# 修改作者信息
git commit --amend --author="张三 <zhangsan@example.com>"
```

### 7.3 撤销已 push 的提交

```bash
# 方式1：revert（推荐，安全）
# 创建一个新提交来撤销指定提交的改动
git revert abc1234

# 方式2：reset + force push（危险，仅个人分支）
git reset --hard HEAD~1
git push --force-with-lease origin main
# 绝对不要使用 --force，使用 --force-with-lease
```

### 7.4 凭证管理

```bash
# 缓存凭证（15 分钟）
git config --global credential.helper cache

# 长期缓存
git config --global credential.helper store

# macOS 使用钥匙串
git config --global credential.helper osxkeychain

# Windows 使用凭据管理器
git config --global credential.helper manager
```

### 7.5 空目录提交

Git 不跟踪空目录，如需提交空目录：

```bash
mkdir empty-dir
touch empty-dir/.gitkeep
git add empty-dir/.gitkeep
```

### 7.6 忽略文件权限变更

```bash
# 忽略文件模式（权限）变更
git config --global core.fileMode false
```

### 7.7 大小写敏感

```bash
# 让 Git 对文件名大小写敏感
git config --global core.ignorecase false

# 重命名大小写（如 User.java -> user.java）
git mv User.java user.java
```

---

## 八、命令速查表

| 操作 | 命令 |
|------|------|
| 初始化仓库 | `git init` |
| 克隆仓库 | `git clone <url>` |
| 查看状态 | `git status` |
| 添加文件 | `git add <file>` / `git add .` |
| 提交改动 | `git commit -m "msg"` |
| 推送 | `git push` |
| 拉取 | `git pull` / `git fetch` |
| 创建分支 | `git branch <name>` / `git switch -c <name>` |
| 切换分支 | `git checkout <name>` / `git switch <name>` |
| 合并分支 | `git merge <branch>` |
| 变基 | `git rebase <branch>` |
| 储藏 | `git stash` / `git stash pop` |
| 查看日志 | `git log --oneline` |
| 查看差异 | `git diff` |
| 撤销工作区 | `git checkout -- <file>` |
| 撤销暂存区 | `git reset HEAD <file>` |
| 软回退 | `git reset --soft HEAD~1` |
| 硬回退 | `git reset --hard <commit>` |
| 安全回退 | `git revert <commit>` |
| 找回记录 | `git reflog` |
| 标签 | `git tag -a v1.0 -m "版本 1.0"` |

---

## 参考资源

- [Pro Git 官方文档（中文版）](https://git-scm.com/book/zh/v2)
- [Conventional Commits 规范](https://www.conventionalcommits.org/zh-hans/v1.0.0/)
- [Git 官方文档](https://git-scm.com/docs)
- [Oh Shit, Git!?!](https://ohshitgit.com/) — 常见 Git 问题的快速修复指南
