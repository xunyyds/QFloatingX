# tools

QFloatingX 仓库的本地开发工具，不参与插件运行与发布包。

## check_types.py

静态类型检查。把 `API/` 下的脚本合并为一个编译单元，用 `javac` 检查语法与类型，
并与 HEAD 基线逐条对比，输出「新增 / 减少」的结构错误与未解析符号。

QFun 脚本是松散的 `.java` 片段，不能直接编译。检查器会做这些方言还原：

- 顶层松散语句收进合成的 `__looseInit()`，保留其符号校验
- `import` / `package` 统一提到编译单元开头
- `catch (e)` 补成 `catch (Throwable e)`
- `?.` 安全调用与 `?:` 默认值还原为标准 Java
- 脚本类参数在松散方法边界写作 `Object ctx`，检查时回写为 `SettingsBuildContext ctx`
- 宿主注入的无名包 helper（`traceLog` / `qqToast` 等）补合成方法桩

宿主与第三方包按需打桩，未覆盖的部分表现为「package ... does not exist」，
属于环境缺口而非代码缺陷——以与 HEAD 基线的差异为准。

### 用法

```bash
python tools/check_types.py
```

依赖：

- JDK 17 及以上（用于 `--enable-preview --release 21` 的未命名类语法）
- Android SDK 的 `android.jar`

路径通过环境变量指定，未设置时自动探测常见安装位置：

| 变量 | 含义 |
|---|---|
| `QFX_JAVAC` | `javac` 可执行文件路径 |
| `QFX_ANDROID_JAR` | `android.jar` 路径 |

## update_checksums.py

重算 `up.json` 里各文件的 SHA-256 校验和，并列出本次发生变化的文件。
发版前需要运行，客户端按校验和做增量更新。

```bash
python tools/update_checksums.py
```
