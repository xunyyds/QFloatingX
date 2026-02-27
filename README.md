

Based on the provided original README and code map, I'll create an enhanced README.md that maintains the original style while adding more technical clarity:

```markdown
# QFloatingX | 漂浮吧，脚本君！

<div align="center">

**状态**: 🟢 在BeanShell的废墟上野蛮生长 | **版本**: v9.1+ | **作者**: ᗜ×ᗜ

*（QFun群里的那个"又双叒叕在反射"的家伙）*

</div>

---

## 这是个啥？

这是一个在**QQ内置的BeanShell解释器**里，用**纯Java代码**手搓出来的QFun功能增强模块。

如果说写标准Java是在迪拜盖大楼，那写这个就是在**地震带的蒙古包里搭空间站**——没泛型、没注解、`onTouch`返回`null`就崩溃，但我们还是把18个功能塞进了长按菜单🤷‍♂️

---

## ✨ 功能清单（aka. "我们怎么把QQ玩坏的"）

| 功能 | 描述 | 黑科技等级 |
|------|------|------------|
| ⚡ **快捷入口** | 一键打开QFun设置/Java脚本页面 | ★☆☆☆☆ |
| 📍 **模拟定位** | 让你的QQ在火星上线 | ★★★☆☆ |
| 📊 **消息统计** | 统计消息（前提：Java没崩溃） | ★★☆☆☆ |
| 🖥️ **运行状态** | 实时查看脚本存活状态 | ★★☆☆☆ |
| 🎯 **长按菜单** | **18项功能**塞进消息长按菜单 | ★★★★★ |
| 🌐 **HTML浏览器** | 在QQ里看网页，就问你怕不怕 | ★★★☆☆ |

### 18项长按菜单功能

1. 查看消息详情
2. 复制纯文本
3. 提取音频
4. 消息解密/加密
5. 发送表情包
6. 点赞用户
7. 戳一戳
8. 查看聊天记录
9. 群管理员设置
10. 群名片修改
11. 群成员列表
12. 群列表
13. 好友列表
14. 禁言操作
15. 踢人操作
16. 获取Cookie
17. 头像相关
18. 更多...

---

## 📂 项目结构

```
QFloatingX/
├── API/                      # 核心API模块
│   ├── api.java              # 基础API（HTTP、文件、加解密、定位等）
│   ├── api2.java             # 模拟定位功能
│   ├── api3.java             # 消息统计功能
│   ├── api4.java             # 悬浮窗功能
│   ├── api5.java             # 页面跳转（预留）
│   ├── api6.java             # 运行状态/系统信息
│   ├── api7.java             # HTML浏览器
│   ├── api8.java             # QQ空间辅助
│   ├── api9.java             # Protobuf发送器
│   ├── ColorPicker.java      # 调色盘组件
│   ├── Dialog.java           # 对话框工具
│   ├── function.java         # 动态热插拔功能
│   ├── import.java            # 导入类声明
│   ├── setwindow.java         # 窗口设置
│   └── uitools.java           # UI工具集
├── main.java                  # 入口主文件
├── desc.txt                   # 描述文件
├── info.prop                  # 属性配置
└── icon.zip                   # 图标资源
```

### 核心模块说明

| 模块 | 功能描述 |
|------|----------|
| **api.java** | 基础工具：HTTP请求、文件操作、加解密算法、URL编解码、定位服务、Toast提示等 |
| **api2.java** | 模拟定位：通过Hook LocationManager实现GPS欺骗 |
| **api3.java** | 消息统计：接收/发送消息计数，支持按时间范围筛选 |
| **api4.java** | 悬浮窗：可拖拽的悬浮按钮，支持GIF动画、长按关闭 |
| **api6.java** | 运行状态：显示QQ状态、模块信息、系统资源、电池状态等 |
| **api7.java** | HTML浏览器：在QQ内嵌WebView加载HTML文件 |
| **api9.java** | Protobuf发送：构造并发送Protobuf协议数据 |
| **function.java** | 动态热插拔：脚本的加载、运行、循环、定时任务管理 |

---

## 🔧 技术栈（aka. "我们踩过的坑"）

```java
// 环境
- 解释器: BeanShell（对，就是那个没有字节码的）
- 限制: 无泛型、无Lambda（线程里）、onTouch必须返回boolean
- 哲学: 防御性编程 or 死亡

// 黑科技
- 反射调用成功率 >90%（原生API，禁止MethodHandle）
- UI线程包裹率 100%（runOnUiThread是命）
- 日志系统: traceLog("/Log/不崩溃.txt", "又活过了一行")
- Hook技术: XposedBridge（在蒙古包里装核弹）

// 设计模式
- 拓扑序排列法（方法必须按调用链物理前置）
- 成员变量内聚模式（构造即准备，反射最爱）
- 日志驱动开发（因为无法断点调试）
```

---

## 📦 安装与使用

### 前置要求
- QFun插件（支持BeanShell脚本）
- 已安装QQ应用

### 安装步骤

1. **下载与解压**
   - 下载 `QFloatingX 2.3.0.zip` 并解压

2. **放置脚本**
   - 将解压后的 `QFloatingX` 文件夹完整复制到QFun的脚本目录

3. **启用模块**
   - 打开QFun设置
   - 找到并启用QFloatingX模块

4. **使用功能**
   - **快捷入口**: 在QQ设置中一键跳转
   - **悬浮窗**: 长按悬浮球可拖拽，点击打开功能菜单
   - **长按菜单**: 长按任意消息弹出18项功能菜单
   - **模拟定位**: 在设置中开启/关闭定位模拟

---

## 💬 社区与讨论

**作者**: ᗜ×ᗜ  
**联系方式**: QFun群里吼一声，那个在讨论`NoSuchMethodException`怎么绕过的就是我

**脚本状态**: 🔓 **完全开源，无加密**

欢迎：
- ✅ 学习讨论（建议备好降压药）
- ✅ 功能建议（请先确认BeanShell支持）
- ✅ Bug反馈（请附带`/Log/`目录下的崩溃日记）
- ❌ 问我为什么不用lambda （问就是不喜欢）

---

## 📜 许可证

"BeanShell受害者联盟"公共协议

你可以：
- 自行修改、学习（搬运一定要给我留版权！！！写这些真的很累的！）
- 在README里吐槽这破环境
- 在代码注释里写"这里曾崩溃387次"

不可以：
- 加密后声称是自己原创
- 问作者为什么不用Spring Boot

---

## 🎓 给后来者的忠告

> "在BeanShell里，日志是唯一的真理，反射是唯一的武器，UI线程是唯一的神。"

如果你也在写QFun模块，记住：
1. 每次`onTouch`都`return true`
2. 每次反射都`catch Throwable`
3. 每次UI操作都`runOnUiThread`
4. 每次崩溃都`traceLog("wtf.txt", e)`

**祝你在解释器的夹缝中，也能漂浮起来。**

---

<div align="center">

**QFloatingX** - Because Floating is Better Than Crashing™

</div>
```