# 技能库连接指南

> 本仓库为 `lingma-skills` 技能库源码，需要连接到各 AI IDE 的用户技能目录下使用。

## 技能库位置

```
d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab\
```

## 已连接的 IDE

| IDE | 用户技能目录 | 连接方式 |
|---|---|---|
| Lingma | `C:\Users\arrowInknee\.lingma\skills` | 目录联接（Junction） |
| Qoder | `C:\Users\arrowInknee\.qoder\skills` | 目录联接（Junction） |

> **目录名称规范**：各 IDE 的技能目录标准名称固定为 `skills`，请勿使用 `skillslib` 或其他变体名称。

## 连接方法（Windows）

推荐 **目录联接（Junction）**，实时同步且无需管理员权限。

### 步骤

1. **备份原目录**（如已存在）
   ```cmd
   move "C:\Users\arrowInknee\.lingma\skills" "C:\Users\arrowInknee\.lingma\skills.bak"
   ```

2. **创建 Junction**
   ```cmd
   mklink /J "C:\Users\arrowInknee\.lingma\skills" "d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab"
   ```
3. Windows PowerShell（管理员权限）
    ```cmd
    cmd /c mklink /J "C:\Users\arrowInknee\.lingma\skills" "d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab"
    ```

4. **验证连接**
   ```cmd
   dir "C:\Users\arrowInknee\.lingma"
   ```
   应看到 `skills [d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab]`

### 通用模板（替换路径即可）

```cmd
mklink /J "{IDE_USER_SKILLS_PATH}" "d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab"
```

## 更新维护流程

### 日常开发
1. 在 `d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab` 中修改技能文件
2. 提交并推送至远程仓库
3. 各 IDE 自动实时同步（因使用 Junction，无需额外操作）

### 新环境首次拉取
1. 克隆主仓库
   ```bash
   git clone https://github.com/arrowInkneeeee/AikSteinsGrimoire.git
   ```
2. 进入技能库目录，切换到对应 tag
   ```bash
   cd AikSteinsGrimoire/aik-skills-lab
   git checkout v1.0.0
   ```
3. 按上述 **连接方法** 创建 Junction

### 更新技能库版本
```bash
cd d:\JeBrainsWorkSpace\AikSteinsGrimoire\aik-skills-lab
git fetch --tags
git checkout v{新版本号}
```

## 新增 IDE 连接记录

后续接入新 AI IDE 时，在此表格追加：

| IDE | 用户技能目录 | 连接方式 | 维护人 | 日期 |
|---|---|---|---|---|
| Lingma | `C:\Users\arrowInknee\.lingma\skills` | Junction | a I k | 2026-05-15 |
| Qoder | `C:\Users\arrowInknee\.qoder\skills` | Junction | a I k | 2026-05-15 |
| Claude | `C:\Users\arrowInknee\.claude\skills` | Junction | a I k | 2026-05-15 |
| QoderCN | `C:\Users\syLvate\.qoder-cn\skills` | Junction | a I k | 2026-07-08 |
| {新IDE} | `{路径}` | Junction | {维护人} | {日期} |

## 注意事项

- 不要直接删除 Junction 目标目录（`aik-skills-lab`），否则所有连接的 IDE 技能目录都会失效
- 如需临时断开某个 IDE 的连接，删除该 Junction 即可，不影响技能库本体
- 技能库本身有独立的 git 管理，提交时请确保当前目录在 `aik-skills-lab` 下
