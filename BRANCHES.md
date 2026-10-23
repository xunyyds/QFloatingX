# 分支与发布说明

本仓库同时承载**源码**与**发布通道**，两条线分工明确，互不干扰。

## 分支职责

| 分支 | 用途 | 目录布局 | 维护方式 |
|---|---|---|---|
| `main` | 源码主分支（**唯一开发线**） | 仓库根 **即** 插件根（`API/api.java`、`main.java`） | 日常开发在此提交 |
| `QF` | 旧版客户端的更新通道（**临时保留**） | 仓库根下多一层 `QFloatingX/` 前缀（`QFloatingX/API/api.java`） | 由「qfx更新推送」工具写入 |

## 分支生命周期

`main` 是唯一正式管理的分支。`QF` 属于**过渡期兼容分支**：

- 保留原因：已发布的老版本客户端内置的更新器只认 `QF` 分支的 `up.json`，
  只要还有人停留在旧版本，这条通道就必须继续提供文件下载。
- 退出条件：当所有用户都更新到新版本（客户端更新源切换到 `main`）之后，
  `QF` 分支连同 `up.json` 一起废弃，此后不再推送。
- 两条分支的历史**不合并**：`main` 是从 v2.6.3 起重新建立的历史，
  `QF` 保留了它自己的 635 条提交。合并会让 `main` 的历史被旧结构污染，因此刻意保持独立。

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

## 提交信息规范（约定式提交）

`main` 与 `QF` 均采用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 格式：

```
<type>(<scope>): <subject>
```

- `type`（必须）：`feat` 新功能 / `fix` 修复 / `docs` 文档 / `style` 格式 /
  `refactor` 重构 / `chore` 杂项
- `scope`（可选）：影响范围，如 `api5`、`setwindow`、`revoke_mark`
- `subject`（必须）：简短描述，中文建议 20 字以内，动词开头，**结尾不加句号**

示例：

```
feat(revoke_mark): 新增消息视图监听开关
fix(api5): 修复宿主缺失监听接口导致的加载失败
docs(branches): 补充 QF 分支退出条件
```

`QF` 分支由「qfx更新推送」自动提交，标题固定为 `chore(release): 发布 vX.Y.Z`，
**正文留空**。发布说明只写入 `up.json` 的 `changelog` 与 `更新日志.txt`，
不重复写进 commit body——客户端展示更新内容时读的是 `up.json`，与 commit 无关。

## 字节保护机制

`.gitattributes` 里统一 `* -text`，关闭全部行尾自动转换；本地配 `core.autocrlf=false`。

原因是插件源码含大量内嵌字符串与生成式拼接，CRLF↔LF 转换会真实改变文件内容，
既破坏更新校验，也可能改变脚本行为。**关闭转换不影响 `git diff` 的按行比较**，代码审查照常。

> 注意：`* -text` 也意味着编辑器保存文件时用什么行尾，git 就原样存什么。
> 同一文件在 `main` 与 `QF` 上可能行尾不同（例如 `ColorPicker.java` 在 `main` 是 CRLF、
> 在 `QF` 是 LF），这类文件在跨分支比对时会整篇显示为差异，属于预期现象，不是内容改动。

## 日常工作流

```bash
git checkout main
git add -A
git commit -m "fix(api5): 修复宿主缺失监听接口导致的加载失败"
git push origin main
```

需要发布时，另走「qfx更新推送」：它读取本机插件目录 → 以原始字节提交到 `QF` → 同步更新 `up.json`。

## 两个分支共有的根级文件

以下文件在 `main` 与 `QF` 上**各存一份**，内容保持逐字节一致，不要只在单边维护：

| 文件 | 用途 |
|---|---|
| `LICENSE`、`中文版LICENSE` | 开源许可证 |
| `QFun_Java脚本开发手册.md` | 开发手册 |
| `README.md` | 项目说明（两边同一份） |
| `icon.zip` | 图标资源包 |

`icon.zip` 虽然**不进 `up.json` 的 `files` 清单**，但**会被客户端主动下载**：
`api.java` 的 `performDownloadAndUnzip()` 走 `buildMirrorUrlsOn("QF", "icon.zip", true)`，
直接拼出 `raw.githubusercontent.com/xunyyds/QFloatingX/QF/icon.zip`。
所以 `icon.zip` 必须在 **`QF` 分支根目录**常驻，且 `main` 同步保留一份作为源，
两边内容必须一致——否则图标缺失时下到的包与源码不符。

## 不入库的内容

运行时由插件在真机上自行生成，已在 `.gitignore` 中排除：

`Log/`、`config/`、`cache/`、`HTML/`、`HotPlug/`、`info/`、`outputs/`、`_backups/`，
以及更新中转文件 `*.new`、由 `icon.zip` 下发的运行时图标（`/API/icon.png` 等，
注意只排除 `API/` 下的产物，根目录的 `icon.zip` 源包要入库）。
