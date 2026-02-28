

Based on the provided code map, I can see this is a QQ (Tencent QQ) plugin/Xposed module project called "QFloatingX". Let me create a comprehensive README.md for this project.

# QFloatingX

QQ 增强悬浮窗插件 - 一个功能丰富的 Xposed 模块

## 项目简介

QFloatingX 是一款针对腾讯 QQ 开发的 Xposed 悬浮窗插件，提供丰富的增强功能，包括消息统计、悬浮窗控制、模拟定位、颜色选择器等多种实用功能。

## 功能特性

### 核心功能

- **悬浮窗管理** - 支持悬浮窗的显示、隐藏、拖拽、关闭等功能
- **消息统计** - 统计每日/每周/每月的收发消息数量，支持多种消息类型分类统计
- **模拟定位** - 支持 GPS 位置模拟功能
- **颜色选择器** - 提供 RGB/HSV/色轮/色条等多种颜色选择方式

### 消息处理

- **消息复读** - 支持消息重复发送功能
- **消息转发** - 通过服务器转发消息
- **双击消息** - 双击消息触发快捷操作
- **消息菜单** - 长按消息显示扩展菜单

### 工具功能

- **HTML 预览** - 内置 HTML 文件预览器，支持底部拖拽面板
- **QZone 助手** - QQ 空间自动点赞、评论功能
- **PB 发送器** - 支持 Protocol Buffers 原始数据发送
- **图片编辑** - 支持图片裁剪、缩放、模糊等处理

### 脚本系统

- **热插拔脚本** - 支持动态加载和执行 BeanShell 脚本
- **预设功能** - 内置多种预设功能模板
- **循环任务** - 支持定时循环执行任务
- **消息队列** - 支持批量发送消息

### UI 增强

- **多样化对话框** - 多种现代化对话框样式
- **深色模式** - 支持明暗主题切换
- **自定义背景** - 支持图片背景和模糊效果
- **圆角窗口** - 支持窗口圆角设置

## 文件结构

```
QFloatingX/
├── API/
│   ├── api.java         # 基础 API
│   ├── api2.java        # 模拟定位
│   ├── api3.java        # 消息统计
│   ├── api4.java        # 悬浮窗管理
│   ├── api5.java        # 消息处理
│   ├── api6.java        # 运行状态
│   ├── api7.java        # HTML 预览
│   ├── api8.java        # QZone 助手
│   ├── api9.java        # PB 发送器
│   ├── ColorPicker.java # 颜色选择器
│   ├── Dialog.java      # 对话框工具
│   ├── function.java   # 功能与脚本
│   ├── import.java     # 导入声明
│   ├── setwindow.java  # 窗口设置
│   └── uitools.java    # UI 工具
├── main.java            # 入口文件
├── info.prop            # 模块信息
└── desc.txt             # 描述文件
```

## 技术栈

- **语言**: Java
- **框架**: Xposed Framework
- **目标应用**: 腾讯 QQ
- **最低 Android 版本**: 支持 QQ 兼容的 Android 版本

## 使用说明

### 前提条件

1. 已安装 Xposed Framework 或 LSPosed 等兼容框架
2. 已获取 Root 权限
3. 安装了腾讯 QQ 应用

### 安装步骤

1. 下载并安装 QFloatingX 模块
2. 在 Xposed/LSPosed 框架中启用该模块
3. 重启设备或 QQ 应用
4. 在 QQ 中通过悬浮窗或相关入口访问功能

### 主要功能入口

- **悬浮窗**: 在聊天界面显示悬浮图标，支持拖拽和点击操作
- **消息统计**: 通过模块提供的入口查看统计数据
- **设置**: 可在模块中配置各项功能参数

## 功能模块详解

### 悬浮窗 (api4.java)

支持拖拽、吸附边角、长按关闭、点击操作等功能，可自定义图标和透明度。

### 消息统计 (api3.java)

- 统计收发的文本、图片、文件、语音、表情等消息类型
- 支持今日、昨日、本周、本月、自定义日期范围
- 可设置每日消息目标，显示完成进度

### 模拟定位 (api2.java)

通过 Hook LocationManager 实现 GPS 位置模拟，支持开关控制。

### QZone 助手 (api8.java)

- 自动遍历好友空间动态
- 支持自动点赞、评论
- 黑名单过滤功能

### PB 发送器 (api9.java)

支持发送原始 Protocol Buffers 数据，可用于高级调试和测试。

## 配置说明

模块配置文件位于 `/config/` 目录下，主要包括：

- `msg_stats.json` - 消息统计数据存储
- 其他功能配置文件

## 注意事项

1. 本模块仅供学习交流使用
2. 使用某些功能可能违反 QQ 用户协议，请谨慎使用
3. 部分功能需要 Root 权限
4. 模拟定位等功能请仅用于测试目的

## 版本信息

- 当前版本: 2.3.1
- 兼容 QQ 版本: 请参考模块公告

## 许可证

本项目仅供学习交流使用，请勿用于商业用途。