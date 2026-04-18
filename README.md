# jxc-master

仓库根目录提供一套 Windows 启动脚本，统一管理后端 `jxc` 和前端 `jxc-w`。

## 一键启动

- 双击 `start-dev.cmd`
- 或运行 `powershell -ExecutionPolicy Bypass -File .\dev.ps1 -Action Start`

## 停止

- 双击 `stop-dev.cmd`
- 或运行 `powershell -ExecutionPolicy Bypass -File .\dev.ps1 -Action Stop`

## 重启

- 双击 `restart-dev.cmd`
- 或运行 `powershell -ExecutionPolicy Bypass -File .\dev.ps1 -Action Restart`

## 默认端口

- 后端：`8080`
- 前端：`5173`

脚本会把进程 PID 写到 `.dev\pids`，启动后后端和前端会各自弹出控制台窗口，日志直接打印在窗口里。
