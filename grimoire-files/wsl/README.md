# WSL 初始化快照资料

本目录保存当前 WSL Ubuntu 26.04 初始化快照及其导入说明。

## 快照

- `Ubuntu-26.04-init.tar`：当前 WSL Ubuntu 26.04 的完整初始化快照。
- 文件来源：`D:\WSL\WSL-snapshots\Ubuntu-26.04-before-services-2026-08-20.tar`
- 文件大小：`5,848,791,040` bytes
- SHA-256：`b84691dcfa6c6d89bfb534e7ff7667f0d9157200bb1d21c8a6d54b31af460f32`

## 文档

- `wsl-init-import.md`：使用初始化快照导入 WSL、设置默认用户和验证环境。
- `wsl2.md`：WSL2 初始安装方案。
- `wsl2-2.0.md`：WSL2 开发环境方案 v2.2。
- `wsl2-3.0.md`：WSL2 开发环境方案 v3.0。

## 注意事项

Tar 快照只包含 WSL Linux 文件系统，不包含 Windows 的 `D:\service`、Windows 版数据库和 Windows 应用。快照中可能包含 Codex、Claude、DeepSeek 等配置及凭证，应通过可信的私有渠道传输，不要公开分享。

本目录及快照文件暂不加入 Git，适合使用百度网盘等私有方式传输。
