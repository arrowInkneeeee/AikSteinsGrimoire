# WSL 初始化快照资料

本目录保存 WSL Ubuntu 26.04 初始化、导入与连接相关文档。

## 快照

- 本仓库**不保存** tar 快照（体积大，且可能包含凭证）；需要时从私有备份恢复。
- 原始来源：`D:\\WSL\\WSL-snapshots\\Ubuntu-26.04-before-services-2026-08-20.tar`。
- 不要把快照放进 git；只通过可信的私有渠道传输。

## 文档

- `wsl-init-import.md`：使用初始化快照导入 WSL、设置默认用户和验证环境。
- `wsl2.md`：WSL2 初始安装方案。
- `wsl2-2.0.md`：WSL2 开发环境方案 v2.2。
- `wsl2-3.0.md`：WSL2 开发环境方案 v3.0。
- `wsl2-mirrored-ssh.md`：WSL2 镜像网络模式下的 SSH 配置。
- `wsl2-ssh-key-localhost.md`：WSL2 下 SSH key 与 localhost 连接配置。
- `dsh-plugin-market-warm.sh`：DSH 插件市场预热脚本。

## 注意事项

Tar 快照只包含 WSL Linux 文件系统，不包含 Windows 的 `D:\\service`、Windows 版数据库和 Windows 应用。快照中可能包含 Codex、Claude、DeepSeek 等配置及凭证，应通过可信的私有渠道传输，不要公开分享。
