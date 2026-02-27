# QFun Plugin Java 脚本深度开发手册

> **本手册定位**：QFun 模块 Plugin 系统的完全技术参考手册。基于 Modern BeanShell（支持 Java 8+ 全特性）  
> 的脚本引擎，提供与模块内置功能等价的开发能力。通过本手册，你将掌握从基础 API 到底层 Hook、  
> 从消息结构到 QQ 内部类的全部知识，实现任意功能扩展
>
> **技术背景**：Plugin 脚本运行在 QFun 模块的 BeanShell 解释器中，该解释器的 `classLoader` 直接  
> 持有 QQ 的 HostClassLoader（包含约 36 万个 QQ 内部类），因此脚本可以直接 import 和 Hook QQ  
> 内部方法，能力等同于用 Kotlin/Java 编写的模块内置功能
>
> **模块架构简述**：QFun 采用 KSP 注解处理器自动生成 Hook 注册表，使用 DexKit（C++ 字节码分析）  
> 进行运行时特征匹配定位 Hook 点（抗 R8 混淆），基于 LibXposed API 提供跨框架（LSPosed/EdXposed/  
> 太极）兼容的 Hook 能力  Plugin 系统是 QFun 对外开放的脚本扩展接口，用户可以在不修改模块源码  
> 的情况下，通过 Java 脚本实现自定义功能

---

## 目录

1. [快速开始](#一快速开始)
2. [脚本执行机制与生命周期](#二脚本执行机制与生命周期)
3. [全局变量与运行环境](#三全局变量与运行环境)
4. [回调方法完全解析](#四回调方法完全解析)
5. [消息数据结构完全解析（MsgRecord 深度剖析）](#五消息数据结构完全解析msgrecord-深度剖析)
6. [API 完全手册](#六api-完全手册)
7. [Hook 开发完全指南](#七hook-开发完全指南)
8. [反射与 QQ 内部类深度分析](#八反射与-qq-内部类深度分析)
9. [ClassLoader 体系与类加载机制](#九classloader-体系与类加载机制)
10. [实战案例](#十实战案例)

---

## 一、快速开始

### 1.1 脚本目录结构

```
QFun/[你的QQ号]/plugin/
  └─ your_plugin_id/
       ├─ main.java      # 入口文件（必须）
       ├─ info.prop      # 配置文件（必须）
       ├─ desc.txt       # 描述文本（可选）
       └─ config/        # 数据存储目录（自动创建）
```

### 1.2 info.prop 配置格式

```properties
id=your_plugin_id
pluginName=我的第一个插件
author=你的名字
versionCode=1
```

**说明**：
- `id`：插件唯一标识符，必须与目录名一致，建议用英文小写+下划线
- `pluginName`：显示在插件列表中的名称
- `author`：作者名
- `versionCode`：版本号，整数，用于版本管理

### 1.3 最简单的 main.java

```java
// 顶层代码：插件加载时立即执行
log("plugin.log", "插件已启动");
addItem("测试菜单", "onTestClick");

// 菜单回调
void onTestClick(int chatType, String peerUin, String name) {
    qqToast(2, "你点击了菜单，当前聊天：" + name);
}

// 生命周期：插件卸载时触发
void unLoadPlugin() {
    log("plugin.log", "插件已卸载");
}
```

---

## 二、脚本执行机制与生命周期

### 2.1 加载流程（框架视角）

```
QQ 启动 → LSPosed 注入 QFun 模块
  └─ QFun XposedInit.handleLoadPackage()
       ├─ 初始化 HybridClassLoader
       ├─ 创建 BeanShell Interpreter，注入所有 Plugin API
       ├─ 扫描 plugin/ 目录，找到所有含 info.prop 的子目录
       └─ 为每个 Plugin 创建独立的 Interpreter 实例
            ├─ 设置 classLoader = sHostClassLoader（QQ 的 ClassLoader）
            ├─ 注入全局变量（context/classLoader/myUin/pluginPath/pluginId）
            ├─ eval 执行 main.java 的顶层代码
            └─ 保存对 Interpreter 的引用，用于后续回调
```

### 2.2 脚本执行顺序

```java
// ===== 阶段 1：顶层代码（插件加载时立即执行一次）=====
import android.app.Activity;
import android.widget.Toast;
import me.yxp.qfun.utils.hook.HookExtensionsKt;
import java.util.HashSet;

log("plugin.log", "初始化开始");
Object unhookSet = new HashSet();
boolean enabled = getBoolean("config", "enabled", true);
addItem("功能开关", "toggleFeature");

// 注册 Hook（顶层执行，插件加载时立即生效）
if (enabled) {
    // Hook 代码...
}

// ===== 阶段 2：回调方法（事件触发时被框架反射调用）=====
void onMsg(Object data) {
    // 收到消息时触发
}

void joinGroup(String groupUin, String memberUin) {
    // 成员入群时触发
}

// ===== 阶段 3：自定义方法（可被回调或菜单调用）=====
void toggleFeature(int chatType, String peerUin, String name) {
    enabled = !enabled;
    putBoolean("config", "enabled", enabled);
    qqToast(2, enabled ? "已开启" : "已关闭");
}

// ===== 阶段 4：生命周期（插件卸载/QQ退出时触发）=====
void unLoadPlugin() {
    for (Object unhook : unhookSet) {
        unhook.unhook();
    }
    unhookSet.clear();
    log("plugin.log", "清理完成");
}
```

### 2.3 线程模型

**默认线程**：所有回调方法（`onMsg`/`joinGroup`/`chatInterface` 等）默认在 **IO 线程** 执行。

**操作 UI 必须切换主线程**：

```java
// ❌ 错误：直接在回调线程操作 UI 会崩溃
void onMsg(Object data) {
    Toast.makeText(context, data.msg, Toast.LENGTH_SHORT).show(); // 崩溃！
}

// ✅ 正确：切换到主线程
void onMsg(Object data) {
    Activity act = getNowActivity();
    if (act != null) {
        act.runOnUiThread(() -> {
            Toast.makeText(context, data.msg, Toast.LENGTH_SHORT).show();
        });
    }
}

// ✅ 推荐：使用框架提供的方法（已自动处理线程切换）
void onMsg(Object data) {
    qqToast(2, data.msg);  // 内部已处理，直接用
}
```

### 2.4 变量作用域

```java
// 顶层变量：全脚本共享，插件生命周期内持久
Object unhookSet = new HashSet();
int globalCount = 0;

void onMsg(Object data) {
    // 方法内局部变量：仅当前调用有效
    int localCount = 0;
    localCount++;  // 每次都是 1
    
    // 访问顶层变量：累加，值保持
    globalCount++;
}
```

---

## 三、全局变量与运行环境

### 3.1 框架注入的全局变量

Plugin 脚本启动时，QFun 框架会自动注入以下全局变量，在脚本任意位置（含 Lambda、内部类）均可直接使用：

```java
// context - android.content.Context
// QQ App 的 ApplicationContext，可用于获取系统服务、资源等
Activity act = getNowActivity();
if (act == null) act = context;  // 备用方案
PackageManager pm = context.getPackageManager();

// classLoader - ClassLoader
// QQ 的 HostClassLoader，持有 QQ 全部内部类（约 36 万个）
// 用于 import QQ 内部类或 Class.forName 动态加载
import com.tencent.mobileqq.aio.animation.AIOAnimationContainer;
Class cls = Class.forName("com.tencent.mobileqq.xxx.Yyy", true, classLoader);

// myUin - String
// 当前登录的 QQ 号
if (data.userUin.equals(myUin)) {
    // 是自己发的消息
}

// pluginId - String
// 当前插件 ID（来自 info.prop 的 id 字段，注意大小写）
log(pluginId + ".log", "写入插件专属日志");

// pluginPath - String
// 当前插件目录的绝对路径（无末尾 /）
String cachePath = pluginPath + "/cache/data.json";
```

### 3.2 开发环境规范

**引擎**：Modern BeanShell，支持完整 Java 8 特性（Lambda、Stream、方法引用、try-with-resources）

**属性访问**：推荐用 `.` 直接访问对象属性，无需 Getter 方法：

```java
void onMsg(Object data) {
    // ✅ 推荐：直接访问
    String msg = data.msg;
    long msgId = data.msgId;
    
}
```

**Lambda 与 Stream**：

```java
// Lambda
new Thread(() -> {
    log("thread.log", "后台执行");
}).start();

// Stream
Object groups = getGroupList();
groups.stream()
    .filter(g -> g.groupName.contains("技术"))
    .forEach(g -> log("groups.log", g.group));

// 方法引用
groups.forEach(this::logGroup);
void logGroup(Object g) {
    log("groups.log", g.group + " - " + g.groupName);
}
```

---

## 四、回调方法完全解析

### 4.1 onMsg - 消息回调

**触发时机**：收到消息时（包括群聊、私聊、陌生人消息）

**参数**：`Object data` - 消息数据对象（MsgData，见第五节详解）

**字段速查**：
```java
void onMsg(Object data) {
    // === 基础字段 ===
    int type = data.type;           // 聊天类型：1=私聊, 2=群聊, 100=陌生人
    int msgType = data.msgType;     // 消息类型：1=文本, 2=图片, 5=视频, 6=语音, 7=文件...
    String peerUin = data.peerUin;  // 对方 Uin（群号/好友 QQ）
    String peerUid = data.peerUid;  // 对方 Uid
    String userUin = data.userUin;  // 发送者 Uin（发消息的人的 QQ）
    String userUid = data.userUid;  // 发送者 Uid
    long time = data.time;          // 发送时间戳（秒）
    long msgId = data.msgId;        // 消息 ID（用于撤回/引用）
    String msg = data.msg;          // 文本内容（QFun 解析后的纯文本）
    String path = data.path;        // 文件/语音/视频的本地保存路径
    
    // === 艾特相关 ===
    Object atList = data.atList;    // 艾特的 QQ 号列表（List<String>）
    Object atMap = data.atMap;      // 艾特映射（Map<String,String>，Key=Uin, Value=艾特文本）
    
    // === 原始对象（用于发消息、深度解析）===
    Object contact = data.contact;  // Contact 对象（直接传给 sendMsg 等 API）
    Object msgRecord = data.data;   // MsgRecord 原始对象（见第五节完整结构）
}
```

**使用示例**：

```java
void onMsg(Object data) {
    // 快速判断
    if (data.type == 2 && data.msg.equals("ping")) {
        sendMsg(data.peerUin, "pong", 2);
    }
    
    // 使用 Contact 对象发消息（推荐）
    if (data.msg.equals("test")) {
        sendMsg(data.contact, "收到测试消息");
    }
    
    // 判断是否是自己发的
    if (data.userUin.equals(myUin)) {
        log("self.log", "我发送了：" + data.msg);
    }
    
    // 检测是否被艾特
    if (data.atList != null && data.atList.contains(myUin)) {
        sendMsg(data.contact, "[atUin=" + data.userUin + "] 我看到了");
    }
}
```

### 4.2 joinGroup - 成员入群回调

**触发时机**：群成员加入时

**参数**：
- `String groupUin` - 群号
- `String memberUin` - 入群成员的 QQ 号

```java
void joinGroup(String groupUin, String memberUin) {
    // 欢迎新人
    String welcome = "[atUin=" + memberUin + "] 欢迎加入本群！\n" +
                     "请查看群公告了解群规。";
    sendMsg(groupUin, welcome, 2);
    
    // 记录日志
    log("join.log", memberUin + " 加入了群 " + groupUin);
}
```

### 4.3 quitGroup - 成员退群回调

**触发时机**：群成员退出时（主动退群或被踢）

**参数**：
- `String groupUin` - 群号
- `String memberUin` - 退群成员的 QQ 号

```java
void quitGroup(String groupUin, String memberUin) {
    log("quit.log", memberUin + " 退出了群 " + groupUin);
    
    // 检查是否是被踢（需要结合其他信息判断，这里仅记录）
    Object member = getMemberInfo(groupUin, memberUin);
    if (member == null) {
        log("quit.log", "成员信息已不存在，可能是被踢");
    }
}
```

### 4.4 shutUpGroup - 禁言事件回调

**触发时机**：群内发生禁言变动（禁言或解除禁言）

**参数**：
- `String groupUin` - 群号
- `String memberUin` - 被禁言/解除禁言的成员 QQ 号
- `long time` - 禁言时长（秒），**0 表示解除禁言**
- `String operator` - 操作者 QQ 号（执行禁言/解禁的管理员/群主）

```java
void shutUpGroup(String groupUin, String memberUin, long time, String operator) {
    if (time > 0) {
        // 被禁言
        String msg = memberUin + " 被 " + operator + " 禁言 " + time + " 秒";
        sendMsg(groupUin, msg, 2);
        
        // 记录到日志
        log("shutup.log", "[禁言] 群:" + groupUin + " 成员:" + memberUin + 
            " 时长:" + time + "秒 操作者:" + operator);
    } else {
        // 解除禁言
        sendMsg(groupUin, memberUin + " 被解除禁言", 2);
        log("shutup.log", "[解禁] 群:" + groupUin + " 成员:" + memberUin);
    }
}
```

### 4.5 chatInterface - 进入聊天界面回调

**触发时机**：用户打开某个聊天窗口时

**参数**：
- `int chatType` - 聊天类型：1=私聊, 2=群聊
- `String peerUin` - 群号或好友 QQ
- `String name` - 群名或好友昵称

**用途**：自动签到、读取当前聊天信息等

```java
void chatInterface(int chatType, String peerUin, String name) {
    // 自动签到示例
    if (chatType == 2 && peerUin.equals("123456789")) {
        clockIn(peerUin);
        log("sign.log", "已在群 " + name + " 自动签到");
    }
    
    // 记录打开聊天的时间
    long now = System.currentTimeMillis() / 1000;
    putLong("chatlog", peerUin, now);
}
```

### 4.6 onPaiYiPai - 拍一拍事件回调

**触发时机**：发生拍一拍事件时

**参数**：
- `String peerUin` - 群号或好友 QQ（拍一拍发生的地点）
- `int chatType` - 聊天类型：1=私聊, 2=群聊
- `String operator` - 拍人者的 QQ 号

```java
void onPaiYiPai(String peerUin, int chatType, String operator) {
    if (chatType == 2 && operator.equals("123456")) {
        sendMsg(peerUin, "别拍了！", 2);
    }
    
    // 记录所有拍一拍事件
    log("pai.log", "[拍一拍] 地点:" + peerUin + " 操作者:" + operator);
}
```

### 4.7 getMsg - 发送预处理回调

**触发时机**：本机发送文本消息之前

**参数**：`String original` - 原始消息内容

**返回**：`String` - 修改后的内容（不修改则返回原样）

**用途**：添加小尾巴、内容过滤、格式转换等

```java
String getMsg(String original) {
    // 自动加小尾巴
    if (getBoolean("config", "addTail", true)) {
        return original + " [Sent by QFun]";
    }
    
    // 敏感词过滤
    String filtered = original.replace("敏感词", "***");
    
    // 格式转换
    if (filtered.startsWith("/code ")) {
        String code = filtered.substring(6);
        return "```\n" + code + "\n```";
    }
    
    return filtered;
}
```

### 4.8 unLoadPlugin - 生命周期回调（必须实现）

**触发时机**：插件卸载/重新加载/QQ 退出时

**用途**：清理资源、卸载 Hook、保存数据

**重要性**：如果不实现或不正确卸载 Hook，会导致插件卸载后 Hook 仍然生效，可能引发崩溃。

```java
import java.util.HashSet;
import me.yxp.qfun.hook.api.OnSendMsg;
import me.yxp.qfun.hook.api.SendMsgListener;

Object unhookSet = new HashSet();
SendMsgListener sendListener = (elements) -> {
    // 处理发送...
};

// 插件启动时注册
OnSendMsg.INSTANCE.addListener(sendListener);

void unLoadPlugin() {
    // 1. 卸载所有 Hook
    for (Object unhook : unhookSet) {
        unhook.unhook();
    }
    unhookSet.clear();
    
    // 2. 移除监听器
    OnSendMsg.INSTANCE.removeListener(sendListener);
    
    // 3. 保存数据
    putInt("config", "runCount", runCount);
    
    // 4. 记录日志
    log("plugin.log", "插件已安全卸载");
}
```

---

## 五、消息数据结构完全解析（MsgRecord 深度剖析）

### 5.1 数据层级关系

```
onMsg(Object data)  ← QFun 封装的 MsgData 对象
  └─ data.data  ← MsgRecord 原始对象（QQ 内部消息结构）
       ├─ msgId / msgSeq / chatType / msgType / ...（消息基础字段）
       ├─ elements（消息元素列表，核心！）
       │   └─ List<MsgElement>（每个元素代表消息的一部分）
       │        ├─ elementType（元素类型：1=文本, 2=图片, 5=视频, 6=表情...）
       │        ├─ textElement（文本元素，含艾特信息）
       │        ├─ picElement（图片元素）
       │        ├─ videoElement（视频元素）
       │        ├─ pttElement（语音元素）
       │        ├─ faceElement（表情元素）
       │        └─ replyElement（回复元素）
       ├─ records（引用的原始消息，仅在引用回复时存在）
       └─ msgAttrs（消息属性，含 VIP 气泡、群荣誉等）
```

### 5.2 MsgRecord 顶层字段完全解析

```java
void onMsg(Object data) {
    Object msg = data.data;  // MsgRecord 原始对象
    
    // === 消息标识 ===
    long msgId = msg.msgId;           // 消息 ID（唯一标识）
    long msgRandom = msg.msgRandom;   // 消息随机数
    long msgSeq = msg.msgSeq;         // 消息序列号
    long cntSeq = msg.cntSeq;         // 计数序列号
    
    // === 消息类型 ===
    int chatType = msg.chatType;      // 聊天类型：1=私聊, 2=群聊
    int msgType = msg.msgType;        // 消息类型：1=文本, 2=图片, 5=视频, 6=语音, 7=文件, 9=回复消息...
    int subMsgType = msg.subMsgType;  // 子消息类型
    int sendType = msg.sendType;      // 发送类型
    
    // === 发送者信息 ===
    String senderUid = msg.senderUid; // 发送者 Uid（新版标识）
    String senderUin = msg.senderUin; // 发送者 Uin（QQ 号，需转 String）
    String sendNickName = msg.sendNickName;     // 发送者昵称
    String sendMemberName = msg.sendMemberName; // 群名片
    String sendRemarkName = msg.sendRemarkName; // 备注名
    
    // === 接收者信息 ===
    String peerUid = msg.peerUid;     // 对方 Uid（群/好友）
    String peerUin = msg.peerUin;     // 对方 Uin（群号/好友 QQ）
    String peerName = msg.peerName;   // 对方名称（群名/好友昵称）
    
    // === 时间与状态 ===
    long msgTime = msg.msgTime;       // 发送时间戳（秒）
    int sendStatus = msg.sendStatus;  // 发送状态：2=已发送
    long recallTime = msg.recallTime; // 撤回时间戳（0=未撤回）
    
    // === 消息元素（核心！）===
    Object elements = msg.elements;   // List<MsgElement>（见下节）
    
    // === 引用消息（回复时存在）===
    Object records = msg.records;     // List<MsgRecord>（被引用的原始消息）
    
    // === 消息属性 ===
    Object msgAttrs = msg.msgAttrs;   // Map<Integer, MsgAttributeInfo>（VIP 气泡、群荣誉等）
    
    // === 其他 ===
    int atType = msg.atType;          // 艾特类型：0=无, 2=被艾特
    boolean isOnlineMsg = msg.isOnlineMsg; // 是否在线消息
    int sourceType = msg.sourceType;  // 来源类型：0=自己发的, 1=别人发的
}
```

### 5.3 MsgElement 完全解析（消息元素）

消息的实际内容存储在 `elements` 列表中，每个 `MsgElement` 代表消息的一部分（文本/图片/视频/表情等）。

**元素类型（elementType）**：
- `1` - 文本元素（textElement）
- `2` - 图片元素（picElement）
- `4` - 语音元素（pttElement）
- `5` - 视频元素（videoElement）
- `6` - 表情元素（faceElement）
- `7` - 回复元素（replyElement）

#### 5.3.1 文本元素（elementType = 1）

```java
void onMsg(Object data) {
    Object elements = data.data.elements;
    if (elements == null) return;
    
    StringBuilder fullText = new StringBuilder();
    boolean isAtMe = false;
    
    int size = elements.size();
    for (int i = 0; i < size; i++) {
        Object element = elements.get(i);
        if (element == null) continue;
        
        if (element.elementType == 1 && element.textElement != null) {
            Object textEl = element.textElement;
            
            // 普通文本内容
            String content = textEl.content;
            if (content != null) {
                fullText.append(content);
            }
            
            // 艾特信息
            int atType = textEl.atType;        // 0=普通文本, 2=艾特
            long atUid = textEl.atUid;         // 被艾特的 Uid
            String atNtUid = textEl.atNtUid;   // 被艾特的 NtUid（字符串）
            
            if (atType == 2) {
                // 检测是否艾特了自己
                if (String.valueOf(atUid).equals(myUin)) {
                    isAtMe = true;
                }
            }
            
            // 其他字段
            int subElementType = textEl.subElementType; // 子元素类型
            Object linkInfo = textEl.linkInfo;          // 链接信息
        }
    }
    
    if (isAtMe) {
        sendMsg(data.contact, "检测到艾特我");
    }
    
    log("text.log", "完整文本：" + fullText.toString());
}
```

#### 5.3.2 图片元素（elementType = 2）

```java
void onMsg(Object data) {
    Object elements = data.data.elements;
    if (elements == null) return;
    
    for (Object element : elements) {
        if (element == null) continue;
        
        if (element.elementType == 2 && element.picElement != null) {
            Object picEl = element.picElement;
            
            // 图片 URL
            String sourcePath = picEl.sourcePath;   // 原图 URL
            String thumbPath = picEl.thumbPath;     // 缩略图路径
            
            // 图片尺寸
            int picWidth = picEl.picWidth;
            int picHeight = picEl.picHeight;
            
            // 文件信息
            long fileSize = picEl.fileSize;         // 文件大小（字节）
            String md5 = picEl.md5HexStr;           // MD5 值
            String fileName = picEl.fileName;       // 文件名
            
            // 图片类型
            int picType = picEl.picType;            // 图片类型
            int picSubType = picEl.picSubType;      // 子类型
            
            log("pic.log", "收到图片：" + sourcePath + " 尺寸:" + picWidth + "x" + picHeight);
            
            // 下载图片示例
            if (sourcePath != null) {
                String savePath = pluginPath + "/images/" + md5 + ".jpg";
                urltofile(sourcePath, savePath);
            }
        }
    }
}
```

#### 5.3.3 视频元素（elementType = 5）

```java
void onMsg(Object data) {
    Object elements = data.data.elements;
    if (elements == null) return;
    
    for (Object element : elements) {
        if (element == null) continue;
        
        if (element.elementType == 5 && element.videoElement != null) {
            Object videoEl = element.videoElement;
            
            // 视频文件信息
            String filePath = videoEl.filePath;     // 本地路径（可能为空）
            String fileName = videoEl.fileName;     // 文件名
            String videoMd5 = videoEl.videoMd5;     // 视频 MD5
            long fileSize = videoEl.fileSize;       // 文件大小
            int fileTime = videoEl.fileTime;        // 视频时长（秒）
            
            // 封面信息
            String thumbPath = videoEl.thumbPath;   // 封面路径
            String thumbMd5 = videoEl.thumbMd5;     // 封面 MD5
            long thumbSize = videoEl.thumbSize;     // 封面大小
            int thumbWidth = videoEl.thumbWidth;    // 封面宽度
            int thumbHeight = videoEl.thumbHeight;  // 封面高度
            
            // 其他
            int videoFrom = videoEl.videoFrom;      // 视频来源
            boolean original = videoEl.original;    // 是否原片
            
            log("video.log", "收到视频：" + fileName + " 时长:" + fileTime + "秒 大小:" + fileSize);
        }
    }
}
```

#### 5.3.4 语音元素（elementType = 4）

```java
void onMsg(Object data) {
    Object elements = data.data.elements;
    if (elements == null) return;
    
    for (Object element : elements) {
        if (element == null) continue;
        
        if (element.elementType == 4 && element.pttElement != null) {
            Object pttEl = element.pttElement;
            
            // 语音文件信息
            String filePath = pttEl.filePath;       // 本地路径
            String fileName = pttEl.fileName;       // 文件名
            String md5 = pttEl.md5;                 // MD5
            long fileSize = pttEl.fileSize;         // 文件大小
            int duration = pttEl.duration;          // 时长（秒）
            
            // 其他
            int voiceType = pttEl.voiceType;        // 语音类型
            int voiceChangeType = pttEl.voiceChangeType; // 变声类型
            
            log("ptt.log", "收到语音：" + fileName + " 时长:" + duration + "秒");
        }
    }
}
```

#### 5.3.5 表情元素（elementType = 6）

```java
void onMsg(Object data) {
    Object elements = data.data.elements;
    if (elements == null) return;
    
    for (Object element : elements) {
        if (element == null) continue;
        
        if (element.elementType == 6 && element.faceElement != null) {
            Object faceEl = element.faceElement;
            
            // 表情 ID
            int faceIndex = faceEl.faceIndex;       // QQ 表情 ID
            String faceText = faceEl.faceText;      // 表情文本（如 [微笑]）
            
            // 大表情信息
            String faceId = faceEl.faceId;          // 大表情 ID
            int faceType = faceEl.faceType;         // 表情类型
            
            log("face.log", "收到表情：" + faceText + " ID:" + faceIndex);
        }
    }
}
```

#### 5.3.6 回复元素（elementType = 7）

```java
void onMsg(Object data) {
    Object elements = data.data.elements;
    if (elements == null) return;
    
    for (Object element : elements) {
        if (element == null) continue;
        
        if (element.elementType == 7 && element.replyElement != null) {
            Object replyEl = element.replyElement;
            
            // 被回复的消息信息
            long replayMsgId = replyEl.replayMsgId;         // 被回复消息的 ID
            long replayMsgSeq = replyEl.replayMsgSeq;       // 被回复消息的序列号
            String sourceMsgText = replyEl.sourceMsgText;   // 被回复消息的文本
            
            // 被回复消息的发送者
            String senderUid = replyEl.senderUid;
            String senderUidStr = replyEl.senderUidStr;
            
            // 被回复消息的时间
            long replyMsgTime = replyEl.replyMsgTime;
            
            log("reply.log", "这是一条回复消息，回复了：" + sourceMsgText);
            
            // 获取被回复消息的完整内容（在 data.data.records 中）
            Object records = data.data.records;
            if (records != null && records.size() > 0) {
                Object originalMsg = records.get(0);
                // originalMsg 也是一个 MsgRecord，结构同 data.data
            }
        }
    }
}
```

### 5.4 完整消息解析示例

```java
void onMsg(Object data) {
    Object msg = data.data;
    
    // 基础信息
    log("msg.log", "===== 收到消息 =====");
    log("msg.log", "消息ID: " + msg.msgId);
    log("msg.log", "发送者: " + msg.sendNickName + "(" + msg.senderUin + ")");
    log("msg.log", "时间: " + msg.msgTime);
    log("msg.log", "消息类型: " + msg.msgType);
    
    // 解析元素
    Object elements = msg.elements;
    if (elements != null) {
        log("msg.log", "元素数量: " + elements.size());
        
        StringBuilder fullContent = new StringBuilder();
        int picCount = 0, videoCount = 0, pttCount = 0;
        
        for (int i = 0; i < elements.size(); i++) {
            Object el = elements.get(i);
            if (el == null) continue;
            
            log("msg.log", "  元素[" + i + "] 类型: " + el.elementType);
            
            switch (el.elementType) {
                case 1: // 文本
                    if (el.textElement != null && el.textElement.content != null) {
                        fullContent.append(el.textElement.content);
                    }
                    break;
                case 2: // 图片
                    picCount++;
                    if (el.picElement != null) {
                        log("msg.log", "    图片: " + el.picElement.sourcePath);
                    }
                    break;
                case 5: // 视频
                    videoCount++;
                    if (el.videoElement != null) {
                        log("msg.log", "    视频: " + el.videoElement.fileName);
                    }
                    break;
                case 4: // 语音
                    pttCount++;
                    if (el.pttElement != null) {
                        log("msg.log", "    语音: " + el.pttElement.duration + "秒");
                    }
                    break;
            }
        }
        
        log("msg.log", "完整文本: " + fullContent.toString());
        log("msg.log", "包含: " + picCount + "张图片, " + videoCount + "个视频, " + pttCount + "条语音");
    }
    
    log("msg.log", "==================\n");
}
```

---

## 六、API 完全手册

### 6.1 消息发送

#### sendMsg - 发送消息

**方法签名**：
```java
sendMsg(String peerUin, String content, int type);
sendMsg(Object contact, String content);
```

**参数说明**：
- `peerUin`：对方 Uin（群号/好友 QQ）
- `content`：消息内容，支持以下特殊格式：
  - `[atUin=QQ号]` - 艾特指定 QQ，QQ号=0 表示艾特全体
  - `[pic=url]` - 发送图片，url 可以是网址或本地绝对路径
- `type`：聊天类型，1=私聊, 2=群聊, 100=陌生人
- `contact`：Contact 对象（从 `data.contact` 获取）

**返回值**：无

**示例**：
```java
void onMsg(Object data) {
    if (data.msg.equals("test")) {
        // 方式 1：指定 Uin 和类型
        sendMsg(data.peerUin, "收到测试", data.type);
        
        // 方式 2：使用 Contact 对象（推荐）
        sendMsg(data.contact, "收到测试");
        
        // 艾特发送者
        String reply = "[atUin=" + data.userUin + "] 你好！";
        sendMsg(data.contact, reply);
        
        // 艾特全体
        sendMsg(data.contact, "[atUin=0] 大家好！");
        
        // 发送图片（网址）
        sendMsg(data.contact, "[pic=https://example.com/image.jpg]");
        
        // 发送图片（本地路径）
        sendMsg(data.contact, "[pic=/sdcard/Pictures/test.png]");
        
        // 混合内容
        sendMsg(data.contact, "[atUin=" + data.userUin + "] 你的图片：[pic=https://xxx.jpg]");
    }
}
```

#### sendPic - 发送图片

**方法签名**：
```java
sendPic(String peerUin, String path, int type);
sendPic(Object contact, String path);
```

**参数说明**：
- `path`：图片路径，可以是本地绝对路径或网络 URL

**示例**：
```java
sendPic(data.contact, "https://example.com/image.jpg");
sendPic(data.contact, "/sdcard/Pictures/test.png");
sendPic(data.contact, pluginPath + "/cache/output.jpg");
```

#### sendPtt - 发送语音

**方法签名**：
```java
sendPtt(String peerUin, String path, int type);
sendPtt(Object contact, String path);
```

**参数说明**：
- `path`：语音文件路径，必须是 silk 格式

**示例**：
```java
sendPtt(data.contact, pluginPath + "/voice/greeting.silk");
```

#### sendCard - 发送卡片

**方法签名**：
```java
sendCard(String peerUin, String json, int type);
sendCard(Object contact, String json);
```

**参数说明**：
- `json`：JSON 或 XML 格式的卡片数据（需要知道 QQ 的卡片协议）

**示例**：
```java
String cardJson = "{\"app\":\"com.tencent.structmsg\",...}";
sendCard(data.contact, cardJson);
```

#### sendFile - 发送文件

**方法签名**：
```java
sendFile(String peerUin, String path, int type);
sendFile(Object contact, String path);
```

**示例**：
```java
sendFile(data.contact, pluginPath + "/document.pdf");
sendFile(data.contact, "/sdcard/Download/data.zip");
```

#### sendVideo - 发送视频

**方法签名**：
```java
sendVideo(String peerUin, String path, int type);
sendVideo(Object contact, String path);
```

**示例**：
```java
sendVideo(data.contact, "/sdcard/Movies/video.mp4");
```

#### sendReplyMsg - 引用回复

**方法签名**：
```java
sendReplyMsg(String peerUin, long replyMsgId, String content, int type);
sendReplyMsg(Object contact, long replyMsgId, String content);
```

**参数说明**：
- `replyMsgId`：被引用消息的 ID（从 `data.msgId` 获取）

**示例**：
```java
void onMsg(Object data) {
    // 引用回复收到的消息
    sendReplyMsg(data.contact, data.msgId, "我看到这条消息了");
}
```

#### recallMsg - 撤回消息

**方法签名**：
```java
recallMsg(int type, String peerUin, long msgId);
recallMsg(Object contact, long msgId);
```

**参数说明**：
- `msgId`：要撤回的消息 ID

**注意**：只能撤回自己发送的消息，且有时间限制（通常 2 分钟内）

**示例**：
```java
// 需要保存发送的消息 ID
long sentMsgId = ...; // 发送消息后保存 ID
// 撤回
recallMsg(data.contact, sentMsgId);
```

#### sendPai - 拍一拍

**方法签名**：
```java
sendPai(String toUin, String peerUin, int type);
```

**参数说明**：
- `toUin`：被拍的人的 QQ 号
- `peerUin`：群聊=群号，私聊=好友 QQ
- `type`：1=私聊, 2=群聊

**示例**：
```java
// 在群里拍某人
sendPai("被拍者QQ", "群号", 2);

// 在私聊中拍对方
sendPai("好友QQ", "好友QQ", 1);
```

---

### 6.2 好友操作

#### getAllFriend - 获取好友列表

**方法签名**：
```java
Object getAllFriend();  // 返回 List<FriendInfo>
```

**返回值**：好友列表，每个元素包含以下字段：
- `uin` - QQ 号
- `uid` - UID
- `name` - 昵称
- `remark` - 备注

**示例**：
```java
Object friends = getAllFriend();
for (Object f : friends) {
    String uin = f.uin;
    String name = f.name;
    String remark = f.remark;
    log("friends.log", uin + " - " + (remark != null ? remark : name));
}
```

#### isFriend - 判断是否好友

**方法签名**：
```java
boolean isFriend(String uin);
```

**示例**：
```java
if (isFriend("123456")) {
    sendMsg("123456", "你好朋友！", 1);
}
```

#### sendZan - 点赞

**方法签名**：
```java
sendZan(String uin, int count);
```

**参数说明**：
- `count`：点赞次数，通常最多 20 次/天  非好友可以点50次

**示例**：
```java
sendZan("123456", 10);  // 给好友点 10 个赞
```

#### getUidFromUin / getUinFromUid - Uin/Uid 转换

**方法签名**：
```java
String getUidFromUin(String uin);
String getUinFromUid(String uid);
```

**示例**：
```java
String uid = getUidFromUin("123456");
String uin = getUinFromUid("u_xxxxxx");
```

---

### 6.3 群管理

#### getGroupList - 获取群列表

**方法签名**：
```java
Object getGroupList();  // 返回 List<GroupInfo>
```

**返回值**：群列表，每个元素包含以下字段：
- `group` - 群号
- `groupName` - 群名
- `groupOwner` - 群主 QQ
- `groupInfo` - 原始 TroopInfo 对象

**示例**：
```java
Object groups = getGroupList();
for (Object g : groups) {
    String groupUin = g.group;
    String groupName = g.groupName;
    String ownerUin = g.groupOwner;
    log("groups.log", groupUin + " - " + groupName + " (群主:" + ownerUin + ")");
}
```

#### getGroupInfo - 获取群信息

**方法签名**：
```java
Object getGroupInfo(String groupUin);  // 返回 TroopInfo
```

**返回值**：TroopInfo 对象，包含群的详细信息

**示例**：
```java
Object info = getGroupInfo("群号");
if (info != null) {
    String name = info.troopName;
    int memberCount = info.memberNum;
    int maxMemberCount = info.maxMemberNum;
    log("group.log", "群名:" + name + " 成员数:" + memberCount + "/" + maxMemberCount);
}
```

#### getGroupMemberList - 获取群成员列表

**方法签名**：
```java
Object getGroupMemberList(String groupUin);  // 返回 List<MemberInfo>
```

**返回值**：成员列表，每个元素包含：
- `uin` - QQ 号
- `uinName` - 群名片/昵称
- `uinLevel` - 群等级
- `joinGroupTime` - 入群时间戳
- `lastActiveTime` - 最后发言时间戳
- `role` - 角色（OWNER/ADMIN/MEMBER）
- `memberInfo` - 原始 TroopMemberInfo 对象

**示例**：
```java
Object members = getGroupMemberList("群号");
int adminCount = 0;
for (Object m : members) {
    if (m.role.equals("ADMIN") || m.role.equals("OWNER")) {
        adminCount++;
    }
}
log("members.log", "群管理员数量：" + adminCount);
```

#### getMemberInfo - 获取单个成员信息

**方法签名**：
```java
Object getMemberInfo(String groupUin, String memberUin);
```

**示例**：
```java
Object member = getMemberInfo("群号", "成员QQ");
if (member != null) {
    String name = member.uinName;
    int level = member.uinLevel;
    String role = member.role;
    log("member.log", name + " 群等级:" + level + " 角色:" + role);
}
```

#### getProhibitList - 获取禁言列表

**方法签名**：
```java
Object getProhibitList(String groupUin);  // 返回 List<ForbidInfo>
```

**返回值**：禁言列表，每个元素包含：
- `user` - 被禁言 QQ
- `userName` - 被禁言昵称
- `time` - 剩余禁言时长（秒）
- `endTime` - 禁言结束时间戳

**示例**：
```java
Object forbids = getProhibitList("群号");
for (Object f : forbids) {
    log("forbid.log", f.userName + " 剩余禁言:" + f.time + "秒");
}
```

#### isShutUp - 判断是否全员禁言

**方法签名**：
```java
boolean isShutUp(String groupUin);
```

**示例**：
```java
if (isShutUp("群号")) {
    log("status.log", "当前群处于全员禁言");
}
```

#### shutUp - 禁言成员

**方法签名**：
```java
shutUp(String groupUin, String memberUin, int seconds);
```

**参数说明**：
- `seconds`：禁言时长（秒），0=解除禁言

**示例**：
```java
// 禁言 10 分钟
shutUp("群号", "成员QQ", 600);

// 解除禁言
shutUp("群号", "成员QQ", 0);
```

#### shutUpAll - 全员禁言

**方法签名**：
```java
shutUpAll(String groupUin, boolean enable);
```

**示例**：
```java
shutUpAll("群号", true);   // 开启全员禁言
shutUpAll("群号", false);  // 关闭全员禁言
```

#### kickGroup - 踢出群成员

**方法签名**：
```java
kickGroup(String groupUin, String memberUin, boolean blackList);
```

**参数说明**：
- `blackList`：true=拉黑（不再接收此人申请）

**示例**：
```java
kickGroup("群号", "成员QQ", false);  // 仅踢出
kickGroup("群号", "成员QQ", true);   // 踢出并拉黑
```

#### setGroupAdmin - 设置管理员

**方法签名**：
```java
setGroupAdmin(String groupUin, String memberUin, boolean enable);
```

**示例**：
```java
setGroupAdmin("群号", "成员QQ", true);   // 设为管理
setGroupAdmin("群号", "成员QQ", false);  // 取消管理
```

#### setGroupMemberTitle - 设置头衔（仅群主）

**方法签名**：
```java
setGroupMemberTitle(String groupUin, String memberUin, String title);
```

**示例**：
```java
setGroupMemberTitle("群号", "成员QQ", "技术大佬");
```

#### changeMemberName - 修改群名片

**方法签名**：
```java
changeMemberName(String groupUin, String memberUin, String card);
```

**示例**：
```java
changeMemberName("群号", "成员QQ", "新名片");
```

#### clockIn - 群打卡

**方法签名**：
```java
clockIn(String groupUin);
```

**示例**：
```java
clockIn("群号");
```

---

### 6.4 Cookie & Token

#### getSkey / getRealSkey
```java
String skey = getSkey();
String realSkey = getRealSkey();
```

#### getPskey / getPt4Token
```java
String pskey = getPskey("qzone.qq.com");
String pt4token = getPt4Token("mail.qq.com");
```

#### getStweb
```java
String stweb = getStweb();
```

#### getGTK
```java
String gtk = getGTK("qzone.qq.com");
// 用于 QZone 等需要 g_tk 参数的接口
```

#### getGroupRKey / getFriendRKey
```java
String groupRkey = getGroupRKey();   // 群聊图片 RKey
String friendRkey = getFriendRKey(); // 私聊图片 RKey
```

#### getBkn
```java
long bkn = getBkn(getSkey());
```

---

### 6.5 数据存储

插件配置保存在 `plugin/脚本ID/config/` 目录下的 JSON 文件中。

#### 写入数据
```java
putString(String config, String key, String value);
putInt(String config, String key, int value);
putLong(String config, String key, long value);
putBoolean(String config, String key, boolean value);
```

**参数说明**：
- `config`：配置文件名（不含扩展名）
- `key`：键
- `value`：值

**示例**：
```java
putBoolean("config", "enabled", true);
putInt("config", "count", 100);
putString("config", "username", "张三");
putLong("config", "lastTime", System.currentTimeMillis());
```

#### 读取数据
```java
String getString(String config, String key, String defaultValue);
int getInt(String config, String key, int defaultValue);
long getLong(String config, String key, long defaultValue);
boolean getBoolean(String config, String key, boolean defaultValue);
```

**示例**：
```java
boolean enabled = getBoolean("config", "enabled", false);
int count = getInt("config", "count", 0);
String name = getString("config", "username", "匿名");
long lastTime = getLong("config", "lastTime", 0);
```

---

### 6.6 日志与提示

#### log - 写日志
```java
log(String fileName, String content);
```

**说明**：追加写入到 `pluginPath` 目录下的指定文件

**示例**：
```java
log("debug.log", "变量值：" + value);
log("error.log", "发生错误：" + e.getMessage());
log(pluginId + ".log", "插件运行中");
```

#### toast - 系统 Toast
```java
toast(Object content);
```

**示例**：
```java
toast("操作成功");
```

#### qqToast - QQ 风格顶部弹窗
```java
qqToast(int iconType, Object content);
```

**参数说明**：
- `iconType`：0=警告, 1=错误/失败, 2=成功

**示例**：
```java
qqToast(2, "操作成功");
qqToast(1, "操作失败");
qqToast(0, "警告信息");
```

---

### 6.7 动态加载

#### loadJava - 加载 Java 源文件
```java
loadJava(String path);
```

**示例**：
```java
loadJava(pluginPath + "/Utils.java");
// 加载后可直接使用其中定义的类
```

#### loadJar - 加载 Jar 包
```java
loadJar(String path);
```

**示例**：
```java
loadJar(pluginPath + "/library.jar");
// 加载后可 import jar 中的类
import com.example.MyClass;
```

#### loadDex - 加载 Dex 文件
```java
loadDex(String path);
```

**说明**：加载后可 import dex 中的类，用于绕开 BeanShell 语法限制

**示例**：
```java
loadDex(pluginPath + "/classes.dex");
import com.mypackage.MyClass;
MyClass.doWork();
```

#### registerActivity - 注册 Activity
```java
registerActivity(Class cls);
```

**说明**：注册后可用 `startActivity` 启动

**示例**：
```java
import com.mypackage.MyActivity;
registerActivity(MyActivity.class);
// 之后可以
// startActivity(new Intent(context, MyActivity.class));
```

---

### 6.8 界面工具

#### getNowActivity - 获取当前 Activity
```java
Activity getNowActivity();
```

**返回值**：当前顶层 Activity，可能为 null

**示例**：
```java
Activity act = getNowActivity();
if (act != null) {
    act.runOnUiThread(() -> {
        // 主线程操作
        Toast.makeText(act, "消息", Toast.LENGTH_SHORT).show();
    });
}
```

---

## 七、Hook 开发完全指南

### 7.1 Hook 工具引入

```java
import me.yxp.qfun.utils.hook.HookExtensionsKt;
import me.yxp.qfun.utils.reflect.ReflectDSLKt;
import java.util.HashSet;

Object unhookSet = new HashSet();
```

### 7.2 HookExtensionsKt - 四大 Hook 方法

#### 7.2.1 returnConstant - 让方法返回固定值

**方法签名**：
```java
Object returnConstant(java.lang.reflect.Method method, Object before, Object returnValue);
```

**说明**：Hook 后，目标方法被调用时直接返回 `returnValue`，跳过原方法逻辑

**返回值**：Unhooker 对象，调用 `unhook.unhook()` 可卸载

**示例**：
```java
import com.tencent.mobileqq.aio.animation.util.b;
import org.xmlpull.v1.XmlPullParser;
import java.util.ArrayList;

try {
    java.lang.reflect.Method method = b.class.getDeclaredMethod("a", XmlPullParser.class);
    Object unhook = HookExtensionsKt.returnConstant(method, null, new ArrayList());
    unhookSet.add(unhook);
    log("hook.log", "已 Hook 方法 b.a()");
} catch (Exception e) {
    log("error.log", "Hook 失败: " + e.getMessage());
}
```

#### 7.2.2 hookReplace - 完全替换方法逻辑

**方法签名**：
```java
Object hookReplace(java.lang.reflect.Method method, Object before, Function callback);
```

**说明**：完全替换目标方法的逻辑，callback 的返回值作为方法返回值

**callback 参数**：
- `param.args` - 方法参数数组（Object[]）
- `param.thisObject` - 调用该方法的对象实例
- 返回值作为方法的返回值

**示例**：
```java
import com.tencent.mobileqq.aio.animation.AIOAnimationContainer;

Object methods = ReflectDSLKt.findMethods(AIOAnimationContainer.class, s -> {
    s.setReturnType(boolean.class);
    s.paramTypes(new Class[]{int.class, int.class, Object[].class});
    return null;
});

for (Object m : methods) {
    Object unhook = HookExtensionsKt.hookReplace(m, null, param -> {
        // 获取参数
        int arg0 = (Integer) param.args[0];
        int arg1 = (Integer) param.args[1];
        Object[] arg2 = (Object[]) param.args[2];
        
        // 获取调用对象
        Object self = param.thisObject;
        
        // 自定义逻辑
        log("hook.log", "方法被调用，参数:" + arg0 + ", " + arg1);
        
        // 返回值
        return true;
    });
    unhookSet.add(unhook);
}
```

#### 7.2.3 hookBefore - 方法执行前插入

**方法签名**：
```java
Object hookBefore(java.lang.reflect.Method method, Object before, Function callback);
```

**说明**：在原方法执行前插入逻辑，可以修改参数或阻止方法执行

**callback 功能**：
- 修改参数：`param.args[0] = newValue;`
- 阻止方法执行：`param.setResult(value);`

**示例**：
```java
Object unhook = HookExtensionsKt.hookBefore(method, null, param -> {
    // 修改第一个参数
    String original = (String) param.args[0];
    param.args[0] = original + " [已修改]";
    
    // 如果需要阻止方法执行
    if (original.equals("blocked")) {
        param.setResult(null);  // 设置返回值并阻止方法执行
    }
    
    return null;
});
unhookSet.add(unhook);
```

#### 7.2.4 hookAfter - 方法执行后插入

**方法签名**：
```java
Object hookAfter(java.lang.reflect.Method method, Object before, Function callback);
```

**说明**：在原方法执行后插入逻辑，可以读取或修改返回值

**callback 功能**：
- 获取返回值：`param.getResult()`
- 修改返回值：`param.setResult(newValue);`

**示例**：
```java
Object unhook = HookExtensionsKt.hookAfter(method, null, param -> {
    // 获取方法返回值
    Object result = param.getResult();
    
    // 修改返回值
    if (result instanceof String) {
        String newResult = result + " [已增强]";
        param.setResult(newResult);
    }
    
    log("hook.log", "方法执行完成，返回值: " + result);
    
    return null;
});
unhookSet.add(unhook);
```

### 7.3 ReflectDSLKt - 特征搜索方法

**核心原理**：QQ 经过 R8 混淆后，方法名会变成 `a()`、`b()`，但方法签名（返回类型+参数类型）不会改变。通过特征搜索可以稳定定位方法。

**方法签名**：
```java
Object findMethods(Class cls, Function searcher);
```

**searcher 可用方法**：
- `s.setReturnType(Class)` - 设置返回类型
- `s.paramTypes(Class[])` - 设置参数类型数组
- `s.setParamCount(int)` - 设置参数数量

**示例**：
```java
// 按返回类型 + 参数类型搜索
Object methods = ReflectDSLKt.findMethods(TargetClass.class, s -> {
    s.setReturnType(boolean.class);
    s.paramTypes(new Class[]{int.class, String.class});
    return null;
});

// 只按参数数量搜索
Object methods2 = ReflectDSLKt.findMethods(TargetClass.class, s -> {
    s.setParamCount(2);
    return null;
});

// 只按返回类型搜索
Object methods3 = ReflectDSLKt.findMethods(TargetClass.class, s -> {
    s.setReturnType(void.class);
    return null;
});

// 批量 Hook
if (methods.isEmpty()) {
    qqToast(1, "未找到目标方法");
} else {
    for (Object m : methods) {
        unhookSet.add(HookExtensionsKt.hookReplace(m, null, p -> false));
    }
    log("hook.log", "已 Hook " + unhookSet.size() + " 个方法");
}
```

### 7.4 OnSendMsg - 官方消息发送监听

QFun 提供的官方消息发送监听接口，可以在消息发送前修改内容

**引入**：
```java
import me.yxp.qfun.hook.api.OnSendMsg;
import me.yxp.qfun.hook.api.SendMsgListener;
```

**使用**：
```java
SendMsgListener listener = (elements) -> {
    // elements 是消息元素列表（List<MsgElement>）
    // 可以修改其中的元素，实现发送前处理
    
    for (Object el : elements) {
        // 修改图片尺寸
        if (el.elementType == 2 && el.picElement != null) {
            el.picElement.picWidth = 1024;
            el.picElement.picHeight = 768;
        }
        
        // 修改文本内容
        if (el.elementType == 1 && el.textElement != null) {
            String content = el.textElement.content;
            if (content != null) {
                el.textElement.content = content + " [已处理]";
            }
        }
    }
};

// 注册监听器
OnSendMsg.INSTANCE.addListener(listener);

// 卸载时移除
void unLoadPlugin() {
    OnSendMsg.INSTANCE.removeListener(listener);
}
```

### 7.5 XposedBridge 直接使用

在某些特殊情况下，可以直接使用原始 XposedBridge API：(记得添加到一个集合里面进行保存方便卸载)

```java
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import android.app.Activity;
import android.content.Intent;

boolean isHooked = false;

void hookActivityResult() {
    if (isHooked) return;
    
    try {
        Class activityClass = Class.forName("android.app.Activity");
        java.lang.reflect.Method onResult = activityClass.getDeclaredMethod(
            "onActivityResult", int.class, int.class, Intent.class);
        onResult.setAccessible(true);
        
        XposedBridge.hookMethod(onResult, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    Activity target = (Activity) param.thisObject;
                    int requestCode = ((Integer) param.args[0]).intValue();
                    int resultCode = ((Integer) param.args[1]).intValue();
                    Intent data = (Intent) param.args[2];
                    
                    if (requestCode == 1006 && resultCode == Activity.RESULT_OK) {
                        log("hook.log", "捕获到 ActivityResult");
                        // 处理结果...
                    }
                } catch (Throwable e) {
                    log("error.log", "Hook 回调异常: " + e.getMessage());
                }
            }
        });
        
        isHooked = true;
        log("hook.log", "成功 Hook Activity.onActivityResult");
    } catch (Exception e) {
        log("error.log", "Hook 失败: " + e.getMessage());
    }
}

// 在顶层调用
hookActivityResult();

void unLoadPlugin() {
    // XposedBridge 的 Hook 无法直接 unhook
    // 建议使用 HookExtensionsKt 代替
}
```

### 7.6 完整 Hook 示例

```java
import com.tencent.mobileqq.aio.animation.AIOAnimationContainer;
import me.yxp.qfun.utils.hook.HookExtensionsKt;
import me.yxp.qfun.utils.reflect.ReflectDSLKt;
import me.yxp.qfun.hook.api.OnSendMsg;
import me.yxp.qfun.hook.api.SendMsgListener;
import java.util.HashSet;

Object unhookSet = new HashSet();

// === Hook 1：屏蔽动画 ===
Object methods = ReflectDSLKt.findMethods(AIOAnimationContainer.class, s -> {
    s.setReturnType(boolean.class);
    s.paramTypes(new Class[]{int.class, int.class, Object[].class});
    return null;
});

if (!methods.isEmpty()) {
    for (Object m : methods) {
        unhookSet.add(HookExtensionsKt.hookReplace(m, null, p -> true));
    }
    log("hook.log", "已屏蔽动画，Hook 数: " + unhookSet.size());
}

// === Hook 2：监听消息发送 ===
SendMsgListener sendListener = (elements) -> {
    int picCount = 0;
    for (Object el : elements) {
        if (el.elementType == 2) picCount++;
    }
    if (picCount > 0) {
        log("send.log", "准备发送 " + picCount + " 张图片");
    }
};
OnSendMsg.INSTANCE.addListener(sendListener);

// === 菜单 ===
addItem("Hook 状态", "showStatus");

void showStatus(int chatType, String peerUin, String name) {
    qqToast(2, "当前 Hook 数：" + unhookSet.size());
}

// === 卸载 ===
void unLoadPlugin() {
    for (Object unhook : unhookSet) {
        unhook.unhook();
    }
    unhookSet.clear();
    OnSendMsg.INSTANCE.removeListener(sendListener);
    log("hook.log", "所有 Hook 已卸载");
}
```

---

## 八、反射与 QQ 内部类深度分析

### 8.1 直接 import QQ 类（推荐）

Plugin 的 `classLoader` 是 QQ 的 HostClassLoader，因此可以直接 import：

```java
import com.tencent.mobileqq.aio.animation.AIOAnimationContainer;
import com.tencent.mobileqq.aio.helper.EmojiReplyHelper;
import com.tencent.mobileqq.troop.utils.TroopUtils;
import com.tencent.mobileqq.app.QQAppInterface;

// 直接使用
EmojiReplyHelper helper = ...;
```

### 8.2 Class.forName 动态加载

当类名在运行时才知道时，使用 `Class.forName`：

```java
String className = "com.tencent.mobileqq.xxx.Yyy";
Class cls = Class.forName(className, true, classLoader);

// 获取方法
java.lang.reflect.Method method = cls.getDeclaredMethod("methodName", String.class, int.class);
method.setAccessible(true);

// 调用
Object result = method.invoke(instance, "参数1", 123);
```

### 8.3 打印类的所有字段和方法

**以 EmojiReplyHelper 为例**：

```java
import com.tencent.mobileqq.aio.helper.EmojiReplyHelper; //仅作为示例，实际需自己查找正确的类

void dumpClass() {
    Class cls = EmojiReplyHelper.class;
    log("dump.log", "========================================");
    log("dump.log", "类名: " + cls.getName());
    log("dump.log", "========================================\n");
    
    // === 打印所有字段 ===
    log("dump.log", "===== 字段列表 =====");
    for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
        String modifier = java.lang.reflect.Modifier.toString(f.getModifiers());
        String type = f.getType().getSimpleName();
        String name = f.getName();
        log("dump.log", modifier + " " + type + " " + name);
    }
    
    log("dump.log", "\n");
    
    // === 打印所有方法 ===
    log("dump.log", "===== 方法列表 =====");
    for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
        String modifier = java.lang.reflect.Modifier.toString(m.getModifiers());
        String returnType = m.getReturnType().getSimpleName();
        String name = m.getName();
        
        StringBuilder params = new StringBuilder();
        for (Class p : m.getParameterTypes()) {
            if (params.length() > 0) params.append(", ");
            params.append(p.getSimpleName());
        }
        
        log("dump.log", modifier + " " + returnType + " " + name + "(" + params + ")");
    }
    
    log("dump.log", "\n========================================");
}

// 在菜单中调用
addItem("Dump EmojiReplyHelper", "onDumpClick");
void onDumpClick(int chatType, String peerUin, String name) {
    dumpClass();
    qqToast(2, "已写入 dump.log");
}
```

### 8.4 获取单例的通用方法

QQ 的大多数 Manager 类都是单例，常见获取方式：

```java
// === 方式 1：静态 getInstance() 方法 ===
Object getSingletonByMethod(Class cls) {
    try {
        java.lang.reflect.Method method = cls.getDeclaredMethod("getInstance");
        method.setAccessible(true);
        return method.invoke(null);
    } catch (Exception e) {
        return null;
    }
}

// === 方式 2：静态 INSTANCE 字段 ===
Object getSingletonByField(Class cls) {
    try {
        java.lang.reflect.Field field = cls.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return field.get(null);
    } catch (Exception e) {
        return null;
    }
}

// === 通用搜索：找返回自身类型的静态无参方法 ===
Object getSingleton(Class cls) {
    for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
        if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
            && m.getParameterTypes().length == 0
            && m.getReturnType() == cls) {
            try {
                m.setAccessible(true);
                return m.invoke(null);
            } catch (Exception e) {
                return null;
            }
        }
    }
    return null;
}

// 使用
import com.tencent.mobileqq.app.QQAppInterface;
Object qqAppInterface = getSingleton(QQAppInterface.class);
```

### 8.5 按特征搜索混淆后的方法

```java
// 找 QQ 踢人的内部方法（混淆后方法名未知）
import com.tencent.mobileqq.troop.utils.TroopUtils;

java.lang.reflect.Method findKickMethod() {
    for (java.lang.reflect.Method m : TroopUtils.class.getDeclaredMethods()) {
        Class[] params = m.getParameterTypes();
        if (params.length == 2
            && params[0].getSimpleName().contains("Contact")
            && params[1] == String.class
            && m.getReturnType() == void.class) {
            return m;
        }
    }
    return null;
}

// 使用
java.lang.reflect.Method kickMethod = findKickMethod();
if (kickMethod != null) {
    kickMethod.setAccessible(true);
    Object instance = getSingleton(TroopUtils.class);
    kickMethod.invoke(instance, contactObj, memberUin);
}
```

### 8.6 反射调用完整示例

```java
import com.tencent.mobileqq.troop.utils.TroopUtils;

addItem("执行反射", "onReflectClick");

void onReflectClick(int chatType, String peerUin, String name, Object contact) {
    if (chatType != 2) {
        qqToast(1, "仅在群聊中使用");
        return;
    }
    
    try {
        // 1. 获取类
        Class cls = TroopUtils.class;
        
        // 2. 获取单例
        Object instance = getSingleton(cls);
        if (instance == null) {
            qqToast(1, "获取单例失败");
            return;
        }
        
        // 3. 查找方法
        java.lang.reflect.Method method = null;
        for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
            Class[] params = m.getParameterTypes();
            if (params.length == 2
                && params[0].getSimpleName().contains("Contact")
                && params[1] == String.class
                && m.getReturnType() == void.class) {
                method = m;
                break;
            }
        }
        
        if (method == null) {
            qqToast(1, "方法未找到");
            return;
        }
        
        // 4. 调用方法
        method.setAccessible(true);
        method.invoke(instance, contact, myUin);  // 踢出自己（测试）
        
        qqToast(2, "反射调用成功");
    } catch (Exception e) {
        qqToast(1, "反射失败: " + e.getMessage());
        log("error.log", "反射异常: " + e.toString());
    }
}
```

---

## 九、ClassLoader 体系与类加载机制

### 9.1 完整 ClassLoader 链（运行时实测）

```
Plugin 脚本的 classLoader（即 sHostClassLoader）
  └─ dalvik.system.PathClassLoader（QQ 本体）
       └─ DexPathList
            ├─ dexElements[0]（QQ 主 dex）
            ├─ dexElements[1]（QQ 功能 dex）
            └─ ... 共约 36 万个类

QFun 模块的完整 ClassLoader 链：
  a.d（BeanShell 动态类 ClassLoader）
    └─ me.yxp.qfun.common.HybridClassLoader
         ├─ 字段 INSTANCE: HybridClassLoader 单例
         ├─ 字段 sBootClassLoader: BootClassLoader（系统类）
         ├─ 字段 sHostClassLoader: PathClassLoader（QQ 本体）
         ├─ 字段 sLoaderParentClassLoader: InMemoryDexClassLoader（libxposed，约 167 类）
         ├─ 字段 allocator: long（ART 运行时内存地址）
         └─ 字段 classTable: long（ART 类表地址）
```

### 9.2 HybridClassLoader 的特殊性

**重要**：QFun 模块自身的类（`me.yxp.qfun.*`）不在任何标准 ClassLoader 的 dex 文件中。

QFun 使用 `allocator` 和 `classTable` 字段直接操作 ART 虚拟机运行时内存，将模块类注入到 ART 类表，绕过标准 BaseDexClassLoader 加载流程。

这意味着：
- ❌ `Class.forName("me.yxp.qfun.utils.hook.HookExtensionsKt")` → 能用但是麻烦
- ✅ `import me.yxp.qfun.utils.hook.HookExtensionsKt` 后直接调用 → 完美成功

### 9.3 枚举 QQ 全部类名（导出到文件）

有时需要查找某个功能对应的混淆后类名，可以导出全部类名后搜索：

```java
addItem("导出QQ类", "onExportClasses");

void onExportClasses(int chatType, String peerUin, String name) {
    qqToast(2, "开始导出，请稍候...");
    
    new Thread(() -> {
        try {
            // 1. 获取 PathList
            java.lang.reflect.Field pathListField = null;
            Class tmp = classLoader.getClass();
            while (tmp != null) {
                try {
                    pathListField = tmp.getDeclaredField("pathList");
                    break;
                } catch (Throwable e) {
                    tmp = tmp.getSuperclass();
                }
            }
            pathListField.setAccessible(true);
            Object pathList = pathListField.get(classLoader);
            
            // 2. 获取 dexElements
            java.lang.reflect.Field dexElemsField = 
                pathList.getClass().getDeclaredField("dexElements");
            dexElemsField.setAccessible(true);
            Object[] dexElems = (Object[]) dexElemsField.get(pathList);
            
            // 3. 遍历每个 dex
            for (int i = 0; i < dexElems.length; i++) {
                java.lang.reflect.Field dexFileField = null;
                Class ec = dexElems[i].getClass();
                while (ec != null) {
                    try {
                        dexFileField = ec.getDeclaredField("dexFile");
                        break;
                    } catch (Throwable e) {
                        ec = ec.getSuperclass();
                    }
                }
                if (dexFileField == null) continue;
                
                dexFileField.setAccessible(true);
                Object dexFile = dexFileField.get(dexElems[i]);
                if (dexFile == null) continue;
                
                // 4. 枚举类名
                java.util.Enumeration entries = (java.util.Enumeration)
                    dexFile.getClass().getMethod("entries").invoke(dexFile);
                
                StringBuilder sb = new StringBuilder();
                int count = 0, total = 0;
                while (entries.hasMoreElements()) {
                    sb.append(entries.nextElement()).append("\n");
                    if (++count >= 1000) {
                        java.io.FileWriter fw = new java.io.FileWriter(
                            pluginPath + "/qq_dex" + i + ".txt", true);
                        fw.write(sb.toString());
                        fw.close();
                        sb = new StringBuilder();
                        count = 0;
                    }
                    total++;
                }
                
                if (sb.length() > 0) {
                    java.io.FileWriter fw = new java.io.FileWriter(
                        pluginPath + "/qq_dex" + i + ".txt", true);
                    fw.write(sb.toString());
                    fw.close();
                }
                
                final int ftot = total, fi = i;
                Activity act = getNowActivity();
                if (act != null) {
                    act.runOnUiThread(() ->
                        qqToast(2, "dex[" + fi + "] " + ftot + " 个类")
                    );
                }
            }
            
            Activity act = getNowActivity();
            if (act != null) {
                act.runOnUiThread(() ->
                    qqToast(2, "导出完成，文件位于: " + pluginPath)
                );
            }
        } catch (Throwable e) {
            Activity act = getNowActivity();
            if (act != null) {
                act.runOnUiThread(() ->
                    qqToast(1, "导出失败: " + e.getMessage())
                );
            }
            log("error.log", "导出失败: " + e.toString());
        }
    }).start();
}

void unLoadPlugin() {}
```

---

## 十、实战案例

### 10.1 图片尺寸修改（发送前处理）

```java
import me.yxp.qfun.hook.api.OnSendMsg;
import me.yxp.qfun.hook.api.SendMsgListener;

String CFG_ENABLE = "is_enable";
String CFG_SIZE = "target_size";
int DEFAULT_SIZE = 1024;

SendMsgListener listener = (elements) -> {
    if (!getBoolean("config", CFG_ENABLE, false)) return;
    int targetSize = getInt("config", CFG_SIZE, DEFAULT_SIZE);
    
    for (Object el : elements) {
        if (el.elementType == 2 && el.picElement != null) {
            Object pic = el.picElement;
            pic.picSubType = 0;
            
            if (targetSize <= 1) {
                pic.picWidth = 1;
                pic.picHeight = 1;
                continue;
            }
            
            long oldW = pic.picWidth > 0 ? pic.picWidth : 1920;
            long oldH = pic.picHeight > 0 ? pic.picHeight : 1080;
            double ratio = (double) oldW / (double) oldH;
            
            if (oldW > oldH) {
                pic.picWidth = targetSize;
                pic.picHeight = (int) (targetSize / ratio);
            } else {
                pic.picWidth = (int) (targetSize * ratio);
                pic.picHeight = targetSize;
            }
        }
    }
};

OnSendMsg.INSTANCE.addListener(listener);
addItem("图片尺寸设置", "showUI");

void showUI(int chatType, String peerUin, String name) {
    boolean enabled = getBoolean("config", CFG_ENABLE, false);
    int size = getInt("config", CFG_SIZE, DEFAULT_SIZE);
    qqToast(2, "当前状态: " + (enabled ? "已开启" : "已关闭") + "\n尺寸: " + size);
}

void unLoadPlugin() {
    OnSendMsg.INSTANCE.removeListener(listener);
}
```

### 10.2 关键词自动回复

```java
import java.util.HashMap;

Object replyMap = new HashMap();

// 初始化回复词典
replyMap.put("ping", "pong");
replyMap.put("在吗", "在的，有什么可以帮到你？");
replyMap.put("帮助", "可用命令：\nping - 测试连通性\n在吗 - 查看在线状态\n帮助 - 显示此消息");
replyMap.put("时间", "当前时间：" + new java.util.Date().toString());

void onMsg(Object data) {
    if (data.type != 2) return;  // 只处理群聊
    
    String msg = data.msg.trim();
    String reply = (String) replyMap.get(msg);
    
    if (reply != null) {
        // 艾特发送者
        sendMsg(data.contact, "[atUin=" + data.userUin + "] " + reply);
    }
}

void unLoadPlugin() {}
```

### 10.3 打开群聊时自动群打卡

```java
import java.util.HashSet;

Object signedGroups = new HashSet();
Object signedToday = new HashSet();

void chatInterface(int chatType, String peerUin, String name) {
    if (chatType != 2) return;  // 只处理群聊
    
    // 检查今天是否已签到
    String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
    String key = peerUin + "_" + today;
    
    if (!signedToday.contains(key)) {
        clockIn(peerUin);
        signedToday.add(key);
        signedGroups.add(peerUin);
        log("sign.log", "[" + today + "] 已签到群: " + peerUin + " (" + name + ")");
    }
}

addItem("查看签到", "showSignStatus");
void showSignStatus(int chatType, String peerUin, String name) {
    String msg = "今日已签到群数: " + signedGroups.size();
    qqToast(2, msg);
}

void unLoadPlugin() {}
```

### 10.4 群管理助手

```java
boolean isAdmin(String groupUin, String memberUin) {
    Object member = getMemberInfo(groupUin, memberUin);
    if (member == null) return false;
    String role = member.role;
    return role.equals("OWNER") || role.equals("ADMIN");
}

void onMsg(Object data) {
    if (data.type != 2) return;  // 只处理群聊
    if (!isAdmin(data.peerUin, data.userUin)) return;  // 只响应管理员
    
    String msg = data.msg.trim();
    
    // 禁言命令：/mute QQ号 时长(秒)
    if (msg.startsWith("/mute ")) {
        String[] parts = msg.split(" ");
        if (parts.length >= 3) {
            String target = parts[1];
            int duration = Integer.parseInt(parts[2]);
            shutUp(data.peerUin, target, duration);
            sendMsg(data.contact, "已禁言 " + target + " " + duration + " 秒");
            log("admin.log", "[禁言] 操作者:" + data.userUin + " 目标:" + target + " 时长:" + duration);
        }
    }
    
    // 解禁命令：/unmute QQ号
    if (msg.startsWith("/unmute ")) {
        String target = msg.substring(8).trim();
        shutUp(data.peerUin, target, 0);
        sendMsg(data.contact, "已解除 " + target + " 的禁言");
    }
    
    // 踢人命令：/kick QQ号
    if (msg.startsWith("/kick ")) {
        String target = msg.substring(6).trim();
        kickGroup(data.peerUin, target, false);
        sendMsg(data.contact, "已踢出 " + target);
        log("admin.log", "[踢人] 操作者:" + data.userUin + " 目标:" + target);
    }
    
    // 设置管理命令：/admin QQ号
    if (msg.startsWith("/admin ")) {
        Object operator = getMemberInfo(data.peerUin, data.userUin);
        if (operator == null || !operator.role.equals("OWNER")) {
            sendMsg(data.contact, "仅群主可使用此命令");
            return;
        }
        String target = msg.substring(7).trim();
        setGroupAdmin(data.peerUin, target, true);
        sendMsg(data.contact, "已设置 " + target + " 为管理员");
    }
}

void unLoadPlugin() {}
```

### 10.5 消息统计

```java
import java.util.HashMap;

Object msgCount = new HashMap();  // QQ号 -> 消息数
Object lastResetTime = System.currentTimeMillis();

void onMsg(Object data) {
    if (data.type != 2) return;  // 只统计群聊
    
    String key = data.peerUin + "_" + data.userUin;
    int count = msgCount.containsKey(key) ? (Integer) msgCount.get(key) : 0;
    msgCount.put(key, count + 1);
    
    // 每 100 条消息记录一次
    if ((count + 1) % 100 == 0) {
        log("stats.log", data.userUin + " 在群 " + data.peerUin + " 发送了 " + (count + 1) + " 条消息");
    }
    
    // 每天自动重置统计
    long now = System.currentTimeMillis();
    if (now - lastResetTime > 86400000) { // 24小时
        msgCount.clear();
        lastResetTime = now;
        log("stats.log", "统计已重置");
    }
}

addItem("查看统计", "showStats");

void showStats(int chatType, String peerUin, String name) {
    if (chatType != 2) {
        qqToast(1, "仅在群聊中使用");
        return;
    }
    
    Object members = getGroupMemberList(peerUin);
    StringBuilder sb = new StringBuilder("本群消息统计（前10）:\n");
    
    // 收集本群数据
    Object groupData = new java.util.HashMap();
    for (Object entry : msgCount.entrySet()) {
        String key = (String) entry.getKey();
        if (key.startsWith(peerUin + "_")) {
            String uin = key.split("_")[1];
            int count = (Integer) entry.getValue();
            groupData.put(uin, count);
        }
    }
    
    // 排序
    Object sortedList = new java.util.ArrayList(groupData.entrySet());
    sortedList.sort((a, b) -> ((Integer) b.getValue()).compareTo((Integer) a.getValue()));
    
    // 输出前10
    int rank = 1;
    for (Object entry : sortedList) {
        if (rank > 10) break;
        String uin = (String) entry.getKey();
        int count = (Integer) entry.getValue();
        sb.append(rank).append(". ").append(uin).append(": ").append(count).append(" 条\n");
        rank++;
    }
    
    sendMsg(peerUin, sb.toString(), 2);
}

void unLoadPlugin() {}
```

---

*本手册持续更新中 如有疑问，请咨询 ᗜ×ᗜ 获取帮助*
*联系方式：3069670151@qq.com (发送骚扰信息直接云黑) * 
