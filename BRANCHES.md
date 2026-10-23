# 分支与发布说明

本仓库同时承载**源码**与**发布通道**，两条线分工明确，互不干扰。

## 分支职责

| 分支 | 用途 | 目录布局 | 维护方式 |
|---|---|---|---|
| `main` | 源码主分支 | 仓库根 **即** 插件根（`API/api.java`、`main.java`） | 日常开发在此提交 |
| `QF` | 发布通道（**不要手动改**） | 仓库根下多一层 `QFloatingX/` 前缀（`QFloatingX/API/api.java`） | 由「qfx更新推送」工具写入 |

## 为什么 `QF` 不能手动改

`QF` 分支的根目录下有 `up.json`，它是客户端更新器的数据源：

```json
"files": ["QFloatingX/API/api3.java", "QFloatingX/API/api.java", ...]
```

更新器 `buildMirrorUrls()` 会拼接成
`raw.githubusercontent.com/xunyyds/QFloatingX/QF/<path>` 去下载，
再按 `checksums` 里的 SHA-256 校验后覆盖到插件目录。

因此 `QF` 分支上：

- **目录前缀不能动**：`QFloatingX/` 这一层是 `up.json` 路径的一部分，少一层就会 404
- **文件字节不能动**：校验按 SHA-256，任何行尾改写都会导致「完整性校验失败，已中止」
- **`up.json` 的 `files`/`checksums` 不能手工编辑**：应通过「qfx更新推送」生成

## 字节保护机制

`.gitattributes` 里统一 `* -text`，关闭全部行尾自动转换；本地配 `core.autocrlf=false`。

原因是插件源码含大量内嵌字符串与生成式拼接，CRLF↔LF 转换会真实改变文件内容，
既破坏更新校验，也可能改变脚本行为。**关闭转换不影响 `git diff` 的按行比较**，代码审查照常。

## 日常工作流

```bash
git checkout main
git add -A
git commit -m "fix: xxx"
git push origin main
```

需要发布时，另走「qfx更新推送」：它读取本机插件目录 → 以原始字节提交到 `QF` → 同步更新 `up.json`。

## 不入库的内容

运行时由插件在真机上自行生成，已在 `.gitignore` 中排除：

`Log/`、`config/`、`cache/`、`HTML/`、`HotPlug/`、`info/`、`outputs/`、`_backups/`，
以及更新中转文件 `*.new`、由 `icon.zip` 下发的运行时图标。
