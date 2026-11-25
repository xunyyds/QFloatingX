# PB 协议与底层工具

本文说明本项目 Protobuf/PB 底层工具的使用方式，以及现有功能使用的协议字段。
新增 PB 功能时，按「工具使用 → 字段登记」两节追加，保持字段映射集中可查。

## 一、底层工具

工具集中在 `API/feature/Proto.java`（PB 编解码、发包、数值转换）。

### FunProtoData（PB 编解码）

通用 PB 容器，字段号与值用 `HashMap<Integer, List>` 存储，同一字段可重复出现。

| 方法 | 用途 |
| --- | --- |
| `fromJSON(JSONObject)` | 用 JSON 按字段号构造 PB 对象（嵌套对象递归；数组逐项写入同字段） |
| `toJSON()` | 解析结果转 JSON（字段号为键），便于日志与字段核对 |
| `toBytes()` | 编码为 PB 字节串（Integer→int32，Long→int64，String→bytes，嵌套对象→子消息） |
| `fromBytes(byte[])` | 解析响应字节串（自动跳过 4 字节长度头；长度前缀型字符串会尝试递归解析） |
| `putValue(int, Object)` | 手动写入字段（重复调用即重复字段） |
| `getValues(int)` | 读取字段的所有值（返回 List） |
| `clear()` | 清空 |

构造请求通常用 `fromJSON`：

```java
JSONObject body = new JSONObject();
body.put("1", groupUin);
body.put("2", msgSeq);
FunProtoData proto = new FunProtoData();
proto.fromJSON(body);
byte[] pb = proto.toBytes();
```

解析响应通常用 `fromBytes` + `toJSON`：

```java
FunProtoData proto = new FunProtoData();
proto.fromBytes(resp);
JSONObject json = proto.toJSON();
```

### PacketHelper（发包与编码）

| 方法 | 用途 |
| --- | --- |
| `packet(byte[])` | 加 4 字节大端长度前缀（SSO 帧格式） |
| `sendRequest(byte[], IReceiver)` | 发包，默认 `MessageSvc.PbSendMsg` |
| `sendRequest(String cmd, byte[], IReceiver)` | 发包到指定 serviceCmd |
| `compressGzip(byte[])` / `hexToBytes` / `bytesToHex` / `bytesToHexLimit` | Gzip 压缩、HEX 编解码、日志截断 |

`IReceiver.onReceive(byte[] data)` 是回调；失败或空响应时 data 为 null。

### OIDB 高层封装

| 方法 | 用途 |
| --- | --- |
| `oidbEnvelope(command, serviceType, body)` | 构造 OIDB 信封 JSON |
| `oidbEncode(command, serviceType, body)` | 直接得到信封的 PB 字节串 |
| `oidbSend(logTag, cmd, command, serviceType, body, receiver)` | 组包 → 发包 → 回调，内部处理 observer 与失败回调 |
| `oidbParse(resp)` | 解析信封，返回 `OidbResult` |
| `oidbNumToInt(Object, def)` / `oidbNumToLong(Object, def)` | 数值转换（借 org.json 完成，避免 BeanShell 拆箱异常） |

OIDB 信封结构：

| 字段 | 含义 |
| --- | --- |
| 1 | command（命令号，如 0x5f1） |
| 2 | serviceType（服务类型，对应命令下划线后的数字） |
| 3 | 结果位 |
| 4 | body（业务请求体） |

`OidbResult`：`resultCode`（结果码）、`body`（业务体 FunProtoData）、`raw`（原始字节）、`errorMsg`。

标准用法：

```java
JSONObject body = new JSONObject();
body.put("1", value);
oidbSend("log_tag", "OidbSvc.0x5f1_6", 0x5f1, 6, body, new IReceiver() {
    public void onReceive(byte[] resp) {
        OidbResult result = oidbParse(resp);
        if (result.ok() && result.resultCode == 0) {
            JSONObject json = result.body.toJSON();
        }
    }
});
```

注意：BeanShell 环境下，数值字段读取一律用 `oidbNumToInt/oidbNumToLong`，不要直接对存储对象调 `.intValue()/.longValue()` 再赋给基本类型。

## 二、已登记协议字段

新增功能在对应小节表格追加字段；命令号写在小节标题。

### 历史头像

入口：`https://ti.qq.com/avatarlist/public/index.html`。旧 CGI（`/mqqbase/cgi/history/face/*`）已下线。

| 功能 | serviceCmd | command | serviceType |
| --- | --- | --- | --- |
| 列表 | `OidbSvc.0x5f1_6` | 0x5f1 | 6 |
| 设回 | `OidbSvc.0x8de_4` | 0x8de | 4 |
| 删除 | `OidbSvc.0x5f2_3` | 0x5f2 | 3 |

列表请求（0x5f1_6）：

| 字段 | 含义 | 示例 |
| --- | --- | --- |
| 1 | 时间戳游标 | 当前秒 / 上一页游标 |
| 2 | 数量 | 首页 24，后续 30 |

列表响应：

| 字段 | 含义 | 示例 |
| --- | --- | --- |
| 1 | 头像列表（单条时为对象） | 24/30 条 |
| 2 | 错误码 | 0 正常 |
| 3 | 翻页游标 | 1751194459 |
| 4 | 到底标志 | 0 / 1 |

列表项：

| 字段 | 含义 | 示例 |
| --- | --- | --- |
| 1 | fileid | `3069670151_xxx` |
| 2 | 图片基址 | `http://q1.qlogo.cn/qhis/…/` |
| 4 | face_flag | 63（40/100/140/640/原图可用） |
| 6 | 使用时间（Unix 秒） | 1777210294 |

图片 URL = 基址 + 尺寸后缀；face_flag 位：0x10→640、0x08→140、0x04→100、0x02→40、0x20→原图(0)。

设回请求（0x8de_4）：字段 1 = fileid，字段 2 = is_need_sync_qz（1）。
删除请求（0x5f2_3）：字段 1 = fileid 数组（支持批量）。

### 精华消息

`OidbSvc.0xeac_1`（设精华）/ `OidbSvc.0xeac_2`（取消精华），信封 command=3756、serviceType=1。

信封字段：1=3756、2=1、3=结果位、4=body。
body：1=群号、2=msgSeq、3=msgRandom。

### 表情回应

`OidbSvcTrpcTcp.0x9082_2`，信封 command=36994、serviceType=1。

信封字段：1=36994、2=1、4=body、12=1。
body：2=群号、3=msgSeq、4=表情序号、5=1、6=0、7=0。

### AI 声聊语音

`OidbSvcTrpcTcp.0x11ca_0`，信封 command=37531、serviceType=0。

body：1=群号、2=voiceId、3=文本、4=1、5={1:1773685283}、12=0。

### 偷流量消息

`MessageSvc.PbSendMsg`（通用发消息命令，body 按自定义元素构造）。

信封字段：1=目标群元素、2=发送元素、3=业务元素、4=4100116396L、5=0；
业务元素中的链接/文本字段见 `Proto.java` 的 `showTrafficRedPacketDialog`。

### PB 发送器（调试工具）

设置页「调试」里的 PB 发送器可自定义 serviceCmd 与 JSON 字段后发包，用于探测新协议：
输入 JSON → `FunProtoData.fromJSON` → `PacketHelper.sendRequest` → 响应 HEX/JSON 显示。
探测到字段映射后，按上表格式登记到本文件。
