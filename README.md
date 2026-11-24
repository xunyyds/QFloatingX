# QFloatingX

<div align="center">

![Java | Kotlin](https://img.shields.io/badge/Java%20%7C%20Kotlin-orange.svg) ![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-brightgreen.svg) ![Stars](https://img.shields.io/github/stars/xunyyds/QFloatingX.svg) ![Commits](https://img.shields.io/github/commit-activity/t/xunyyds/QFloatingX.svg)

</div>

## 简介

一个 QFun 模块的增强脚本，用于干掉 QQ 或让 QQ 更便捷。

> 文档：[分支与提交规范](docs/BRANCHES.md) · [QFun Java 开发手册](docs/QFun_Java脚本开发手册.md) · [更新日志](更新日志.txt)

## 项目结构

```
QFloatingX/
├── API/
│   ├── import.java               # 全局 import 声明（唯一入口，最先加载，全模块生效）
│   ├── core/                     # 基础设施层
│   │   ├── Core.java             # 基础工具（HTTP、通知、资料、格式化、加解密、dp、Toast）
│   │   ├── UiTools.java          # UI 工具（主题、控件工厂、图片、缓存、对话框样式）
│   │   ├── Updater.java          # 脚本自更新（镜像下载、校验、解压、更新弹窗）
│   │   └── Entry.java            # 入口交互（跳转页面、打开插件/设置、文件选择器）
│   ├── window/                   # 悬浮窗与弹窗层
│   │   ├── FloatingWindow.java   # 悬浮球（拖拽、吸附、关闭区、GIF 图标）
│   │   ├── Menu.java             # 长按消息菜单（菜单项、分类色板、排序）
│   │   ├── Dialogs.java          # 通用弹窗族（确认、提示、信息卡、成员卡）
│   │   ├── Lists.java            # 列表与选择器（成员/群/好友列表、群选择器）
│   │   └── Markdown.java         # Markdown 渲染与时间选择器
│   ├── settings/                 # 设置页
│   │   ├── Settings.java         # 设置框架（状态机、条目工厂、菜单构建、SettingsActivity）
│   │   ├── SettingsPages.java    # 各功能页构建器（基础/背景/字体/Toast/线程池/悬浮窗/统计/HTML/变量池）
│   │   ├── SettingsSearch.java   # 设置项搜索（索引、拼音、高亮跳转）
│   │   └── Backup.java           # 设置备份导出与导入恢复
│   ├── feature/                  # 功能模块
│   │   ├── Stats.java            # 消息统计（今日/累计/分类/白名单/图表）
│   │   ├── MockLocation.java     # 模拟定位（GPS Hook、监听分发）
│   │   ├── DoubleMsg.java        # 双击消息菜单、翻译、复读、撤回标记
│   │   ├── Proto.java            # Protobuf / OIDB 协议（PB 发送、语音、精华、超级表情）
│   │   └── FaceHistory.java      # QQ 历史头像管理（列表 · 批量保存/删除 · 设回）
│   ├── plugin/                   # 插件自身能力
│   │   ├── HotPlug.java          # 动态热插拔（脚本加载器、定时调度、事件分发）
│   │   ├── KeepAlive.java        # 常驻通知
│   │   └── RuntimeStatus.java    # 运行状态采集（CPU、内存、电量、线程池）
│   ├── widget/                   # 可复用控件
│   │   ├── ColorPicker.java      # 调色盘（色轮、取色、收藏、图片取色）
│   │   └── MultiSelectGrid.java  # 通用多选网格（长按多选 · 拖动连选 · 批量操作）
│   └── browser/                  # 浏览相关
│       ├── Browser.java          # 内置 HTML 浏览器（WebView、文件/网址）
│       └── QZone.java            # QQ空间（自动点赞、评论）
├── main.java                     # 入口主文件（路径常量、线程池、加载清单、生命周期、反射缓存）
├── desc.txt                      # 描述文件
├── info.prop                     # 属性配置
├── icon.zip                      # 图标资源
├── up.json                       # 更新源配置
├── 更新日志.txt                  # 版本更新记录
└── docs/                         # 文档
    ├── BRANCHES.md               # 分支职责与提交规范
    └── QFun_Java脚本开发手册.md  # QFun 插件 Java 开发手册
```

> `API/` 根目录只放 `import.java` 与运行时资源（`icon.png`、`background.png`、
> `icon.zip` 解压产物等）。资源路径在代码中按 `pluginPath + "/API/"` 硬编码，
> 因此**新增代码请放进对应子目录，资源文件勿移入子目录**。

## 安装与使用

### 推荐版本

| 项目 | 版本 |
| --- | --- |
| 推荐 QQ 版本 | 9.3.70 |
| 支持 QQ 版本 | 9.2.35 - 最新 |
| 推荐模块版本 | 1.3.4 |
| 支持模块版本 | 1.3.0 - 最新 |

### 安装步骤

1. **安装**：下载仓库，将解压出的 `QFloatingX` 文件夹完整复制到 QFun 的脚本目录
2. **启用**：打开 QFun 设置，找到并启用 QFloatingX 模块
3. **使用**：
   - **悬浮窗**：长按悬浮球可拖拽，点击打开功能菜单
   - **长按菜单**：长按任意消息弹出功能菜单
   - **双击消息菜单**：双击消息弹出功能菜单（需先在脚本设置里开启开关）

## 联系我们

作者 ᗜ×ᗜ（3069670151@qq.com），欢迎提交 Issue。

> 自行修改、学习均可；搬运请保留版权，点个 star 也行。

<div align="center">

**QFloatingX** — Because Floating is Better Than Crashing™

</div>
