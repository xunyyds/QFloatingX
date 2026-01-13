

# QFloatingX

## 介绍

QFloatingX 是一个优雅的 iOS/macOS 浮动物价组件库，为您的应用提供流畅、美观的浮动窗口体验。该组件设计简洁，易于集成，支持自定义样式和交互行为。

## 功能特点

- **流畅动画**: 采用 Core Animation 优化，提供 60fps 流畅的浮动效果
- **易于集成**: 简单 API 设计，快速集成到现有项目
- **高度可定制**: 支持自定义外观、位置、大小和交互行为
- **手势支持**: 支持拖拽、点击等常见手势操作
- **安全稳定**: 经过严格测试，确保在各种场景下稳定运行

## 环境要求

- iOS 9.0+ / macOS 10.11+
- Swift 5.0+
- Xcode 11.0+

## 安装方式

### Swift Package Manager

```swift
dependencies: [
    .package(url: "https://gitee.com/ovoxiaomo/qfloating-x.git", from: "1.0.0")
]
```

### CocoaPods

```ruby
pod 'QFloatingX'
```

### 手动集成

1. 将 `Sources` 文件夹中的文件添加到您的项目中
2. 确保您的项目已经链接了必要的系统框架

## 快速开始

```swift
import QFloatingX

// 创建浮动窗口
let floatingView = QFloatingX(frame: CGRect(x: 100, y: 200, width: 60, height: 60))

// 配置外观
floatingView.configure {
    $0.cornerRadius = 30
    $0.backgroundColor = .systemBlue
    $0.shadowOpacity = 0.3
}

// 添加到视图层级
window.addSubview(floatingView)

// 启动浮动动画
floatingView.startFloating()
```

## API 文档

### 初始化方法

```swift
// 使用默认配置初始化
let floatingView = QFloatingX()

// 使用自定义帧初始化
let floatingView = QFloatingX(frame: CGRect(x: 100, y: 200, width: 60, height: 60))
```

### 配置选项

```swift
floatingView.configure {
    $0.cornerRadius = 30          // 圆角半径
    $0.backgroundColor = .blue    // 背景颜色
    $0.shadowOpacity = 0.3        // 阴影透明度
    $0.shadowRadius = 10          // 阴影半径
    $0.shadowOffset = CGSize(width: 0, height: 2)  // 阴影偏移
}
```

### 控制方法

```swift
// 开始浮动动画
floatingView.startFloating()

// 停止浮动动画
floatingView.stopFloating()

// 显示/隐藏
floatingView.show()
floatingView.hide()
```

## 示例项目

项目中包含完整的示例应用，位于 `Example` 目录下。您可以运行示例来查看各种使用场景和效果。

## 贡献指南

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建一个 Pull Request

## 许可证

本项目采用 MIT 许可证，详情请参阅 [LICENSE](LICENSE) 文件。

## 联系方式

- 项目主页：https://gitee.com/ovoxiaomo/qfloating-x
- 问题反馈：https://gitee.com/ovoxiaomo/qfloating-x/issues

---

感谢您选择 QFloatingX！如果这个项目对您有帮助，请给我们一个 Star ⭐️。