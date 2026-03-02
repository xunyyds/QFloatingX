import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;

import android.os.Bundle;
import android.os.SystemClock;

import com.tencent.qphone.base.remote.ToServiceMsg;
import com.tencent.qphone.base.remote.FromServiceMsg;
import mqq.app.NewIntent;
import mqq.app.api.impl.SSOEasyServlet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

/**
 * 增强版发包工具
 * <p>
 * 本工具基于 @QFunDeveloper 的开源发包工具进行功能增强与重构
 * 代码中加入了详尽且规范的注释，旨在为开发者提供学习和交流的参考
 * 同时也便于后续的维护与协作改进
 * <p>
 * 欢迎大家在此基础上进行二次开发、优化和贡献
 *
 */

/**
 * IReceiver接口 - 数据接收回调接口
 * 
 * 用于接收服务器返回的PB数据，在发送请求后通过异步回调方式返回结果
 * 
 * 使用示例：
 * PacketHelper.sendRequest("MessageSvc.PbSendMsg", data, new IReceiver() {
 *     void onReceive(byte[] data) {
 *         if (data != null) {
 *             FunProtoData proto = new FunProtoData();
 *             proto.fromBytes(data);
 *             log("response.log", proto.toJSON().toString());
 *         }
 *     }
 * });
 */
public interface IReceiver {
    
    /**
     * 接收服务器返回数据的回调方法
     * 
     * 当请求成功完成时，此方法会被调用并传入服务器返回的原始字节数据。
     * 如果请求失败或返回数据为空，参数将为null。
     * 
     * 注意：此方法在IO线程中执行，如需操作UI请切换到主线程。
     * 
     * param data 服务器返回的PB格式字节数组，失败时为null
     */
    void onReceive(byte[] data);
}

/**
 * FunProtoData - 动态Protobuf数据处理类
 * 
 * 这是一个强大的Protobuf数据处理工具类，支持动态解析和编码Protobuf格式的数据，
 * 无需预定义.proto文件。通过JSON作为中间格式，可以方便地构建和解析复杂的嵌套PB结构。
 * 
 * 核心功能：
 * - JSON转PB：将JSON对象编码为Protobuf二进制格式
 * - PB转JSON：将Protobuf二进制数据解码为JSON对象
 * - 动态字段：支持任意字段编号和数据类型
 * - 嵌套结构：支持多层嵌套的消息结构
 * - 数组支持：支持repeated字段（数组）
 * 
 * 工作原理：
 * Protobuf使用字段编号（field number）和线类型（wire type）来标识每个字段。
 * 本类通过HashMap存储字段数据，键为字段编号，值为该字段的所有值列表（支持repeated字段）。
 * 
 * 线类型说明：
 * - 0 (Varint)：用于整数类型（int32, int64, uint32, uint64, sint32, sint64, bool, enum）
 * - 1 (64-bit)：用于固定64位类型（fixed64, sfixed64, double）
 * - 2 (Length-delimited)：用于字符串、字节、嵌套消息、packed repeated字段
 * - 5 (32-bit)：用于固定32位类型（fixed32, sfixed32, float）
 * 
 * 使用示例1 - 从JSON构建PB数据：
 * JSONObject json = new JSONObject();
 * json.put("1", 12345);           // 字段1: 整数
 * json.put("2", "Hello World");   // 字段2: 字符串
 * 
 * JSONObject nested = new JSONObject();
 * nested.put("1", 100);
 * json.put("3", nested);          // 字段3: 嵌套消息
 * 
 * FunProtoData proto = new FunProtoData();
 * proto.fromJSON(json);
 * byte[] pbBytes = proto.toBytes();
 * 
 * 使用示例2 - 从PB数据解析为JSON：
 * FunProtoData proto = new FunProtoData();
 * proto.fromBytes(pbBytes);
 * JSONObject json = proto.toJSON();
 * log("pb.log", "解析结果: " + json.toString(2));
 * 
 * 使用示例3 - 直接构建PB数据：
 * FunProtoData proto = new FunProtoData();
 * proto.putValue(1, 12345L);      // 添加整数
 * proto.putValue(2, "Hello");     // 添加字符串
 * 
 * FunProtoData nested = new FunProtoData();
 * nested.putValue(1, 100);
 * proto.putValue(3, nested);      // 添加嵌套消息
 * 
 * byte[] pbBytes = proto.toBytes();
 */
public class FunProtoData {
    
    /**
     * 字段数据存储映射
     * 
     * 键为字段编号（Integer），值为该字段的所有值列表（List）。
     * 使用List是为了支持Protobuf的repeated字段，同一个字段编号可以有多个值。
     * 
     * 值类型说明：
     * - Long：整数类型（int32, int64, uint32, uint64等）
     * - Integer：32位固定整数（从fixed32解析）
     * - String：字符串类型，或无法解析的二进制数据（以"hex->"为前缀）
     * - FunProtoData：嵌套的Protobuf消息
     */
    public HashMap values = new HashMap();
    
    /**
     * 从JSON对象解析并填充数据
     * 
     * 将JSON对象转换为Protobuf字段结构。JSON的键必须是数字字符串（表示字段编号），
     * 值可以是基本类型、JSON对象（嵌套消息）或JSON数组（repeated字段）。
     * 
     * 支持的JSON值类型：
     * - Integer/Long：转换为整数字段
     * - String：转换为字符串字段
     * - JSONObject：递归解析为嵌套消息
     * - JSONArray：遍历每个元素作为repeated字段
     * 
     * param json 要解析的JSON对象，可以为null
     */
    public void fromJSON(JSONObject json) {
        if (json == null) return;
        try {
            Iterator keyIt = json.keys();
            while (keyIt.hasNext()) {
                String key = (String) keyIt.next();
                int fieldNumber = Integer.parseInt(key);
                Object value = json.get(key);
                
                if (value instanceof JSONObject) {
                    FunProtoData nestedProto = new FunProtoData();
                    nestedProto.fromJSON((JSONObject) value);
                    putValue(fieldNumber, nestedProto);
                } else if (value instanceof JSONArray) {
                    JSONArray arr = (JSONArray) value;
                    for (int i = 0; i < arr.length(); i++) {
                        Object arrItem = arr.get(i);
                        if (arrItem instanceof JSONObject) {
                            FunProtoData nestedProto = new FunProtoData();
                            nestedProto.fromJSON((JSONObject) arrItem);
                            putValue(fieldNumber, nestedProto);
                        } else {
                            putValue(fieldNumber, arrItem);
                        }
                    }
                } else {
                    putValue(fieldNumber, value);
                }
            }
        } catch (Exception ignored) {}
    }
    
    /**
     * 添加字段值到内部存储
     * 
     * 将值添加到指定字段编号的值列表中。如果该字段编号不存在，
     * 会自动创建新的值列表。支持同一字段编号多次添加（实现repeated字段）。
     * 
     * param fieldNumber Protobuf字段编号（正整数）
     * param value 字段值（支持Long、Integer、String、FunProtoData等类型）
     */
    public void putValue(int fieldNumber, Object value) {
        List list = (List) values.get(fieldNumber);
        if (list == null) {
            list = new ArrayList();
            values.put(fieldNumber, list);
        }
        list.add(value);
    }
    
    /**
     * 从Protobuf字节数组解析数据
     * 
     * 解析原始的Protobuf二进制数据，自动识别字段编号和线类型，
     * 并将解析结果存储到内部映射中。
     * 
     * 解析逻辑：
     * 1. 读取Tag（字段编号 + 线类型）
     * 2. 根据线类型读取对应的数据
     * 3. 对于Length-delimited类型，尝试先解析为嵌套消息，失败则作为字符串
     * 4. 如果字符串解析也失败，则存储为十六进制表示
     * 
     * 特殊处理：
     * - 如果数据以4个0字节开头，会自动跳过（处理某些QQ协议的头部）
     * - 无法解析的二进制数据会以"hex->"前缀存储
     * 
     * param b Protobuf格式的字节数组，可以为null
     */
    public void fromBytes(byte[] b) throws Exception {
        if (b == null) return;
        
        // 处理可能的协议头（4字节长度前缀）
        if (b.length >= 4 && (b[0] & 0xFF) == 0) {
            b = Arrays.copyOfRange(b, 4, b.length);
        }
        
        CodedInputStream in = CodedInputStream.newInstance(b);
        
        while (in.getBytesUntilLimit() > 0) {
            int tag = in.readTag();
            int fieldNumber = tag >>> 3;
            int wireType = tag & 7;
            
            switch (wireType) {
                case 0:
                    putValue(fieldNumber, in.readInt64());
                    break;
                case 1:
                    putValue(fieldNumber, in.readRawVarint64());
                    break;
                case 2:
                    byte[] subBytes = in.readByteArray();
                    try {
                        FunProtoData subData = new FunProtoData();
                        subData.fromBytes(subBytes);
                        putValue(fieldNumber, subData);
                    } catch (Exception e) {
                        try {
                            String decoded = new String(subBytes, StandardCharsets.UTF_8);
                            putValue(fieldNumber, decoded);
                        } catch (Exception e2) {
                            putValue(fieldNumber, "hex->" + bytesToHex(subBytes));
                        }
                    }
                    break;
                case 5:
                    putValue(fieldNumber, in.readFixed32());
                    break;
            }
        }
    }
    
    /**
     * 将数据转换为JSON对象
     * 
     * 将内部存储的Protobuf字段数据转换为JSON格式，便于查看和调试。
     * 嵌套的FunProtoData会递归转换为JSON对象。
     * 
     * 输出格式：
     * - 单值字段：直接输出值
     * - 多值字段（repeated）：输出为JSON数组
     * - 嵌套消息：递归输出为JSON对象
     * 
     * return 转换后的JSON对象
     */
    public JSONObject toJSON() throws Exception {
        JSONObject obj = new JSONObject();
        
        for (Object kObj : values.keySet()) {
            Integer fieldNumber = (Integer) kObj;
            List list = (List) values.get(fieldNumber);
            
            if (list.size() > 1) {
                JSONArray arr = new JSONArray();
                for (Object value : list) {
                    arr.put(valueToJSON(value));
                }
                obj.put(String.valueOf(fieldNumber), arr);
            } else {
                for (Object value : list) {
                    obj.put(String.valueOf(fieldNumber), valueToJSON(value));
                }
            }
        }
        
        return obj;
    }
    
    private Object valueToJSON(Object value) throws Exception {
        if (value instanceof FunProtoData) {
            return ((FunProtoData) value).toJSON();
        }
        return value;
    }
    
    /**
     * 将数据编码为Protobuf字节数组
     * 
     * 将内部存储的字段数据编码为标准的Protobuf二进制格式。
     * 支持整数、字符串、嵌套消息等类型的编码。
     * 
     * 编码规则：
     * - Long/Integer：编码为Varint（线类型0）
     * - String：编码为Length-delimited（线类型2）
     * - FunProtoData：递归编码为嵌套消息（线类型2）
     * - hex->前缀字符串：解码十六进制后编码为字节
     * 
     * return Protobuf编码后的字节数组，失败时返回空数组
     */
    public byte[] toBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CodedOutputStream out = CodedOutputStream.newInstance(bos);
        
        try {
            for (Object kObj : values.keySet()) {
                Integer fieldNumber = (Integer) kObj;
                List list = (List) values.get(fieldNumber);
                
                for (Object value : list) {
                    if (value instanceof Long) {
                        out.writeInt64(fieldNumber, (Long) value);
                    } else if (value instanceof Integer) {
                        out.writeInt32(fieldNumber, (Integer) value);
                    } else if (value instanceof String) {
                        String str = (String) value;
                        if (str.startsWith("hex->")) {
                            out.writeByteArray(fieldNumber, hexToBytes(str.substring(5)));
                        } else {
                            out.writeByteArray(fieldNumber, str.getBytes(StandardCharsets.UTF_8));
                        }
                    } else if (value instanceof FunProtoData) {
                        byte[] nestedBytes = ((FunProtoData) value).toBytes();
                        out.writeByteArray(fieldNumber, nestedBytes);
                    }
                }
            }
            
            out.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
    
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 获取指定字段的第一个值
     */
    public Object getFirstValue(int fieldNumber) {
        List list = (List) values.get(fieldNumber);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
    
    /**
     * 获取指定字段的所有值
     */
    public List getValues(int fieldNumber) {
        return (List) values.get(fieldNumber);
    }
    
    /**
     * 检查是否包含指定字段
     */
    public boolean hasField(int fieldNumber) {
        return values.containsKey(fieldNumber);
    }
    
    /**
     * 清空所有字段数据
     */
    public void clear() {
        values.clear();
    }
}

/**
 * PacketHelper - QQ协议发包工具类
 * 
 * 这是QFun插件的核心发包工具类，提供了完整的QQ协议数据包发送和接收功能。
 * 封装了QQ内部的SSO通信机制，支持发送Protobuf格式的数据到QQ服务器。
 * 
 * 核心功能：
 * - 数据打包：将原始数据封装为QQ协议格式
 * - 请求发送：通过SSO通道发送数据到指定服务
 * - 响应接收：异步接收服务器返回的数据
 * - GZIP压缩：支持数据压缩传输
 * - 格式转换：字节数组与十六进制字符串互转
 * 
 * 工作原理：
 * 
 * 1. QQ SSO通信机制：
 * QQ使用SSO（Single Sign-On）服务进行内部通信。每个请求通过ToServiceMsg封装，
 * 指定目标服务名（serviceCmd），由MobileQQService处理路由和发送。
 * 
 * 2. 数据包结构：
 * +----------------+------------------+------------------+
 * | 4字节长度头    | PB数据体         | ...              |
 * +----------------+------------------+------------------+
 * 长度头 = 数据体长度 + 4（包含自身）
 * 
 * 3. 请求流程：
 * (1) 构建PB数据（使用FunProtoData或原始字节）
 * (2) 调用packet()方法添加长度头
 * (3) 创建ToServiceMsg，设置服务名和数据
 * (4) 通过SSOEasyServlet发送请求
 * (5) 在回调中接收FromServiceMsg响应
 * 
 * 使用示例1 - 发送简单请求：
 * FunProtoData proto = new FunProtoData();
 * proto.putValue(1, 12345L);
 * proto.putValue(2, "Hello");
 * byte[] pbData = proto.toBytes();
 * 
 * PacketHelper.sendRequest("MessageSvc.PbSendMsg", pbData, new IReceiver() {
 *     void onReceive(byte[] data) {
 *         if (data != null) {
 *             FunProtoData response = new FunProtoData();
 *             response.fromBytes(data);
 *             log("response.log", response.toJSON().toString());
 *         }
 *     }
 * });
 * 
 * 使用示例2 - 发送JSON格式的PB数据：
 * JSONObject json = new JSONObject();
 * json.put("1", 100);
 * json.put("2", "test message");
 * 
 * FunProtoData proto = new FunProtoData();
 * proto.fromJSON(json);
 * 
 * PacketHelper.sendRequest("SomeService.Cmd", proto.toBytes(), receiver);
 * 
 * 使用示例3 - 使用十六进制数据：
 * String hexData = "0801120568656C6C6F";
 * byte[] pbData = PacketHelper.hexToBytes(hexData);
 * PacketHelper.sendRequest("TestService.Cmd", pbData, receiver);
 * 
 * 使用示例4 - 自动判断服务名发送：
 * PacketHelper.sendRequest(null, pbData, receiver);
 * // 或使用便捷方法
 * PacketHelper.sendRequest(pbData, receiver);
 * 
 * 常用服务名：
 * - MessageSvc.PbSendMsg - 发送消息
 * - MessageSvc.PbGetMsg - 获取消息
 * - friendlist.getFriendGroupList - 获取好友列表
 * - troop_member_card.get_group_member_card - 获取群成员卡片
 * 
 * 注意事项：
 * - 所有网络操作都是异步的，回调在IO线程执行
 * - 回调中操作UI需要切换到主线程
 * - 服务名需要与QQ协议匹配，否则无法收到响应
 * - 部分服务需要特定的权限或登录状态
 */
public class PacketHelper {
    
    /**
     * GZIP压缩数据
     * 
     * 将字节数组使用GZIP算法进行压缩，用于减少网络传输数据量。
     * QQ协议中部分大数据包会使用GZIP压缩。
     * 
     * 使用场景：
     * - 发送大量文本消息
     * - 传输图片、文件等二进制数据
     * - 减少网络流量消耗
     * 
     * param data 要压缩的原始字节数组
     * return 压缩后的字节数组
     */
    public static byte[] compressGzip(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        gos.write(data);
        gos.close();
        return bos.toByteArray();
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * 
     * 将二进制数据转换为可读的十六进制格式，便于调试、日志记录和数据展示。
     * 每个字节转换为两个十六进制字符（小写字母）。
     * 
     * 转换示例：
     * 输入: [0x08, 0x01, 0x12, 0x05, 0x68, 0x65, 0x6C, 0x6C, 0x6F]
     * 输出: "0801120568656c6c6f"
     * 
     * 使用场景：
     * - 记录原始PB数据到日志
     * - 在UI中显示二进制数据
     * - 数据传输和存储
     * 
     * param bytes 要转换的字节数组
     * return 十六进制字符串，输入为null时返回空字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     * 
     * 将十六进制格式的字符串还原为二进制数据。
     * 输入字符串长度必须是偶数，每个字符对代表一个字节。
     * 
     * 转换示例：
     * 输入: "0801120568656c6c6f"
     * 输出: [0x08, 0x01, 0x12, 0x05, 0x68, 0x65, 0x6C, 0x6C, 0x6F]
     * 
     * 使用场景：
     * - 从日志或配置中恢复PB数据
     * - 解析用户输入的十六进制数据
     * - 测试和调试
     * 
     * param hex 十六进制字符串（长度必须为偶数）
     * return 转换后的字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) return new byte[0];
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 将原始数据封装为QQ协议数据包格式
     * 
     * 在原始数据前添加4字节的大端序长度头，这是QQ协议的标准数据包格式。
     * 长度头的值 = 原始数据长度 + 4（包含长度头自身）。
     * 
     * 数据包结构：
     * +------------+------------------------+
     * | 4字节长度  | 原始数据               |
     * +------------+------------------------+
     * | 00 00 00 N | D0 D1 D2 D3 ... D(n-1) |
     * +------------+------------------------+
     * 
     * 示例：
     * byte[] pbData = new byte[] {0x08, 0x01, 0x12, 0x05};
     * byte[] packet = PacketHelper.packet(pbData);
     * // packet = [0x00, 0x00, 0x00, 0x08, 0x08, 0x01, 0x12, 0x05]
     * // 长度头 = 4 + 4 = 8
     * 
     * param data 原始数据字节数组
     * return 封装后的数据包
     */
    public static byte[] packet(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(data.length + 4);
        dos.write(data);
        return bos.toByteArray();
    }
    
    /**
     * 发送请求到QQ服务（自动判断服务名）
     * 
     * 当服务名为null或空时，会尝试根据数据内容自动判断目标服务。
     * 默认使用MessageSvc.PbSendMsg。
     * 
     * param rawData PB格式的原始数据
     * param receiver 响应接收器
     */
    public static void sendRequest(byte[] rawData, IReceiver receiver) {
        sendRequest(null, rawData, receiver);
    }
    
    /**
     * 发送请求到指定的QQ服务
     * 
     * 这是核心的发包方法，通过QQ的SSO通道发送Protobuf格式的数据到指定服务。
     * 支持异步响应，结果通过IReceiver回调返回。
     * 
     * 执行流程：
     * 1. 检查参数有效性
     * 2. 如果服务名为空，尝试自动判断
     * 3. 将原始数据封装为协议格式（添加长度头）
     * 4. 创建ToServiceMsg，设置服务名和数据
     * 5. 创建NewIntent，设置观察者回调
     * 6. 通过QQAppInterface发送请求
     * 
     * 自动判断服务名规则：
     * - 如果数据包含消息发送特征，使用MessageSvc.PbSendMsg
     * - 默认使用MessageSvc.PbSendMsg作为后备
     * 
     * param serviceCmd 服务命令名，如"MessageSvc.PbSendMsg"，为null时自动判断
     * param rawData PB格式的原始数据（不需要添加长度头，方法内部会自动处理）
     * param receiver 响应接收器，用于接收服务器返回的数据
     */
    public static void sendRequest(String serviceCmd, byte[] rawData, IReceiver receiver) {
        if (receiver == null) return;
        if (rawData == null || rawData.length == 0) {
            receiver.onReceive(null);
            return;
        }
        
        // 自动判断服务名
        String finalServiceCmd = serviceCmd;
        if (finalServiceCmd == null || finalServiceCmd.trim().isEmpty()) {
            finalServiceCmd = "MessageSvc.PbSendMsg";
        }
        
        try {
            byte[] reqBytes = packet(rawData);
            
            NewIntent intent = new NewIntent((android.content.Context) context, SSOEasyServlet.class);
            
            ToServiceMsg toServiceMsg = new ToServiceMsg("mobileqq.service", myUin, finalServiceCmd);
            toServiceMsg.wupBuffer = reqBytes;
            
            intent.setObserver((type, isSuccess, bundle) -> {
                if (isSuccess && bundle != null) {
                    FromServiceMsg fromMsg = bundle.getParcelable("FromServiceMsg");
                    if (fromMsg != null && fromMsg.wupBuffer != null) {
                        logReceivedPB(finalServiceCmd, fromMsg.wupBuffer);
                        receiver.onReceive(fromMsg.wupBuffer);
                    } else {
                        receiver.onReceive(null);
                    }
                } else {
                    receiver.onReceive(null);
                }
            });
            
            intent.putExtra("ToServiceMsg", toServiceMsg);
            QQCurrentEnv.INSTANCE.getQQAppInterface().startServlet(intent);
            
        } catch (Exception e) {
            log("packet_error.log", "发送请求失败: " + e.getMessage());
            receiver.onReceive(null);
        }
    }
    
    /**
     * 记录收到的PB数据到日志
     * 
     * 将服务器返回的PB数据解析并记录到日志文件，便于调试和分析协议。
     * 日志包含时间戳、服务名、原始十六进制数据和解析后的JSON格式。
     * 
     * param serviceCmd 服务命令名
     * param data PB数据
     */
    private static void logReceivedPB(String serviceCmd, byte[] data) {
        try {
            FunProtoData proto = new FunProtoData();
            proto.fromBytes(data);
            JSONObject json = proto.toJSON();
            
            StringBuilder logContent = new StringBuilder();
            logContent.append("\n========== 收到PB响应 ==========\n");
            logContent.append("时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date())).append("\n");
            logContent.append("服务: ").append(serviceCmd).append("\n");
            logContent.append("数据长度: ").append(data.length).append(" 字节\n");
            logContent.append("原始HEX: ").append(bytesToHex(data)).append("\n");
            logContent.append("解析JSON:\n").append(json.toString(2)).append("\n");
            logContent.append("================================\n");
            
            log("pb_received.log", logContent.toString());
            
        } catch (Exception e) {
            log("pb_received.log", "解析PB数据失败: " + e.getMessage() + 
                "\n原始HEX: " + bytesToHex(data));
        }
    }
}

// ==================== UI弹窗代码 ====================

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.FileReader;

/**
 * 创建圆角矩形背景Drawable
 */
GradientDrawable makeRoundRect(int color, int radiusPx) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radiusPx);
    return drawable;
}

/**
 * 创建紧凑型输入框
 */
EditText makeInputCompact(Activity ctx, String val, String hint, int colorBg) {
    EditText et = new EditText(ctx);
    et.setText(val);
    et.setHint(hint);
    et.setTextSize(13);
    et.setTextColor(Color.parseColor("#222222"));
    et.setHintTextColor(Color.parseColor("#BBBBBB"));
    et.setBackground(makeRoundRect(colorBg, dp(ctx, 6)));
    et.setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8));
    et.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return et;
}

/**
 * 创建小标题TextView
 */
TextView makeSubTitleCompact(Activity ctx, String text, int color) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(color);
    tv.setPadding(dp(ctx, 4), dp(ctx, 16), 0, dp(ctx, 6));
    return tv;
}

/**
 * 创建操作按钮
 */
TextView makeActionBtn(Activity ctx, String text, int textColor, int bgColor) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(14);
    tv.setTextColor(textColor);
    tv.setGravity(Gravity.CENTER);
    tv.setBackground(makeRoundRect(bgColor, dp(ctx, 8)));
    return tv;
}

/**
 * dp转px
 */
int dp(Context ctx, int dp) {
    return (int) (dp * ctx.getResources().getDisplayMetrics().density);
}

/**
 * 显示PB发包工具弹窗
 * 
 * 这是PB发包工具的主入口方法，创建并显示一个完整的发包界面。
 * 用户可以在界面中输入服务名和PB数据，然后发送到QQ服务器。
 * 
 * 界面组成：
 * - 服务名输入框（默认提示：MessageSvc.PbSendMsg）
 * - PB数据输入框（JSON格式）
 * - 模板管理按钮（保存/使用/预览）
 * - 发送和取消按钮
 * 
 * 使用方法：
 * 在菜单回调中调用：
 * void onSendPBClick(int chatType, String peerUin, String name) {
 *     showPBSenderDialog();
 * }
 * 
 * 或者添加菜单项：
 * addItem("PB发包工具", "showPBSenderDialog");
 */
void showPBSenderDialog() {
    Activity act = getNowActivity();
    if (act == null) return;
    
    act.runOnUiThread(() -> {
        Dialog dialog = new Dialog(act);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        FrameLayout outer = new FrameLayout(act);
        int m = dp(act, 24);
        outer.setPadding(m, m, m, m);
        
        ScrollView scroll = new ScrollView(act);
        outer.addView(scroll);
        
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeRoundRect(Color.parseColor("#FFFFFF"), dp(act, 16)));
        card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
        scroll.addView(card);
        
        TextView title = new TextView(act);
        title.setText("PB发包工具");
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#222222"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(act, 20));
        card.addView(title);
        
        // 服务名输入
        card.addView(makeSubTitleCompact(act, "服务名", Color.parseColor("#666666")));
        EditText etService = makeInputCompact(act, "", "MessageSvc.PbSendMsg", Color.parseColor("#F7F8FA"));
        card.addView(etService);
        
        // PB数据输入
        card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", Color.parseColor("#666666")));
        EditText etPB = makeInputCompact(act, "", "{\"1\":123,\"2\":\"示例数据\"}", Color.parseColor("#F7F8FA"));
        etPB.setMinLines(4);
        card.addView(etPB);
        
        // 模板管理区域
        card.addView(makeSubTitleCompact(act, "模板管理", Color.parseColor("#666666")));
        
        LinearLayout templateContainer = new LinearLayout(act);
        templateContainer.setOrientation(LinearLayout.HORIZONTAL);
        templateContainer.setGravity(Gravity.CENTER);
        templateContainer.setPadding(0, 0, 0, dp(act, 12));
        card.addView(templateContainer);
        
        TextView btnSave = makeActionBtn(act, "保存模板", Color.WHITE, Color.parseColor("#3B71FE"));
        TextView btnLoad = makeActionBtn(act, "使用模板", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
        TextView btnPreview = makeActionBtn(act, "预览", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, -2, 1);
        btnParams.setMargins(0, 0, dp(act, 8), 0);
        btnSave.setLayoutParams(btnParams);
        btnLoad.setLayoutParams(btnParams);
        btnPreview.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        
        templateContainer.addView(btnSave);
        templateContainer.addView(btnLoad);
        templateContainer.addView(btnPreview);
        
        EditText etTemplateName = makeInputCompact(act, "", "模板名称", Color.parseColor("#F7F8FA"));
        etTemplateName.setVisibility(View.GONE);
        card.addView(etTemplateName);
        
        // 保存模板
        btnSave.setOnClickListener(v -> {
            if (etTemplateName.getVisibility() == View.GONE) {
                etTemplateName.setVisibility(View.VISIBLE);
                btnSave.setText("确认保存");
            } else {
                String name = etTemplateName.getText().toString().trim();
                if (name.isEmpty()) {
                    qqToast(1, "请输入模板名称");
                    return;
                }
                
                String service = etService.getText().toString().trim();
                String pbData = etPB.getText().toString().trim();
                
                if (pbData.isEmpty()) {
                    qqToast(1, "请填写PB数据");
                    return;
                }
                
                String templateKey = "pb_template_" + name;
                putString("templates", templateKey, service + "|||" + pbData);
                
                etTemplateName.setVisibility(View.GONE);
                btnSave.setText("保存模板");
                etTemplateName.setText("");
                qqToast(2, "模板已保存");
            }
        });
        
        // 使用模板
        btnLoad.setOnClickListener(v -> showTemplateSelectorDialog(act, etService, etPB));
        
        // 预览
        btnPreview.setOnClickListener(v -> {
            String service = etService.getText().toString().trim();
            String pbData = etPB.getText().toString().trim();
            if (pbData.isEmpty()) {
                qqToast(1, "请填写PB数据");
                return;
            }
            showPreviewDialog(act, service, pbData);
        });
        
        // 底部按钮
        LinearLayout bottomContainer = new LinearLayout(act);
        bottomContainer.setOrientation(LinearLayout.HORIZONTAL);
        bottomContainer.setGravity(Gravity.CENTER);
        bottomContainer.setPadding(0, dp(act, 20), 0, 0);
        card.addView(bottomContainer);
        
        TextView btnCancel = makeActionBtn(act, "取消", Color.parseColor("#666666"), Color.parseColor("#F7F8FA"));
        TextView btnConfirm = makeActionBtn(act, "发送", Color.WHITE, Color.parseColor("#3B71FE"));
        
        LinearLayout.LayoutParams bottomBtnParams = new LinearLayout.LayoutParams(0, dp(act, 44), 1);
        bottomBtnParams.setMargins(0, 0, dp(act, 12), 0);
        btnCancel.setLayoutParams(bottomBtnParams);
        btnConfirm.setLayoutParams(new LinearLayout.LayoutParams(0, dp(act, 44), 1));
        
        bottomContainer.addView(btnCancel);
        bottomContainer.addView(btnConfirm);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // 发送
        btnConfirm.setOnClickListener(v -> {
            String service = etService.getText().toString().trim();
            String pbData = etPB.getText().toString().trim();
            
            if (pbData.isEmpty()) {
                qqToast(1, "请填写PB数据");
                return;
            }
            
            if (service.isEmpty()) {
                service = "MessageSvc.PbSendMsg";
            }
            
            dialog.dismiss();
            
            ThreadPool.execute(() -> {
                try {
                    JSONObject json = new JSONObject(pbData);
                    FunProtoData proto = new FunProtoData();
                    proto.fromJSON(json);
                    byte[] pbBytes = proto.toBytes();
                    
                    if (pbBytes.length == 0) {
                        act.runOnUiThread(() -> qqToast(1, "PB数据编码失败"));
                        return;
                    }
                    
                    // 记录发送数据
                    StringBuilder logContent = new StringBuilder();
                    logContent.append("\n========== 发送PB请求 ==========\n");
                    logContent.append("时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date())).append("\n");
                    logContent.append("服务: ").append(service).append("\n");
                    logContent.append("JSON:\n").append(pbData).append("\n");
                    logContent.append("HEX: ").append(PacketHelper.bytesToHex(pbBytes)).append("\n");
                    logContent.append("================================\n");
                    log("pb_sent.log", logContent.toString());
                    
                    // 发送
                    PacketHelper.sendRequest(service, pbBytes, new IReceiver() {
                        void onReceive(byte[] data) {
                            act.runOnUiThread(() -> {
                                if (data != null) {
                                    qqToast(2, "发送成功，已收到响应");
                                } else {
                                    qqToast(0, "请求已发送");
                                }
                            });
                        }
                    });
                    
                } catch (Exception e) {
                    act.runOnUiThread(() -> qqToast(1, "发送异常: " + e.getMessage()));
                }
            });
        });
        
        dialog.setContentView(outer);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
        }
        dialog.show();
    });
}

/**
 * 显示模板选择弹窗
 */
void showTemplateSelectorDialog(Activity act, EditText etService, EditText etPB) {
    Dialog dialog = new Dialog(act);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    
    FrameLayout outer = new FrameLayout(act);
    int m = dp(act, 24);
    outer.setPadding(m, m, m, m);
    
    ScrollView scroll = new ScrollView(act);
    outer.addView(scroll);
    
    LinearLayout card = new LinearLayout(act);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(makeRoundRect(Color.parseColor("#FFFFFF"), dp(act, 16)));
    card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
    scroll.addView(card);
    
    TextView title = new TextView(act);
    title.setText("选择模板");
    title.setTextSize(17);
    title.setTextColor(Color.parseColor("#222222"));
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 0, 0, dp(act, 20));
    card.addView(title);
    
    // 加载模板
    HashMap templates = new HashMap();
    try {
        String configPath = pluginPath + "/config/templates.json";
        FileReader fr = new FileReader(configPath);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int len;
        while ((len = fr.read(buffer)) != -1) {
            sb.append(buffer, 0, len);
        }
        fr.close();
        
        JSONObject config = new JSONObject(sb.toString());
        Iterator keys = config.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (key.startsWith("pb_template_")) {
                String value = config.getString(key);
                String displayName = key.substring(12);
                templates.put(displayName, value);
            }
        }
    } catch (Exception e) {}
    
    if (templates.isEmpty()) {
        TextView empty = new TextView(act);
        empty.setText("暂无保存的模板");
        empty.setTextSize(14);
        empty.setTextColor(Color.parseColor("#BBBBBB"));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dp(act, 40), 0, dp(act, 40));
        card.addView(empty);
    } else {
        for (Object entry : templates.entrySet()) {
            Map.Entry e = (Map.Entry) entry;
            String name = (String) e.getKey();
            String templateData = (String) e.getValue();
            
            TextView templateItem = makeActionBtn(act, name, Color.parseColor("#222222"), Color.parseColor("#F7F8FA"));
            templateItem.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            templateItem.setPadding(dp(act, 16), dp(act, 12), dp(act, 16), dp(act, 12));
            card.addView(templateItem);
            
            templateItem.setOnClickListener(v -> {
                String[] parts = templateData.split("\\|\\|\\|");
                if (parts.length >= 2) {
                    etService.setText(parts[0]);
                    etPB.setText(parts[1]);
                } else if (parts.length == 1) {
                    etPB.setText(parts[0]);
                }
                dialog.dismiss();
                qqToast(2, "已加载模板: " + name);
            });
        }
    }
    
    TextView btnClose = makeActionBtn(act, "关闭", Color.parseColor("#666666"), Color.parseColor("#F7F8FA"));
    btnClose.setPadding(0, dp(act, 20), 0, 0);
    card.addView(btnClose);
    btnClose.setOnClickListener(v -> dialog.dismiss());
    
    dialog.setContentView(outer);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
    }
    dialog.show();
}

/**
 * 显示PB数据预览弹窗
 */
void showPreviewDialog(Activity act, String service, String pbData) {
    Dialog dialog = new Dialog(act);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    
    FrameLayout outer = new FrameLayout(act);
    int m = dp(act, 24);
    outer.setPadding(m, m, m, m);
    
    ScrollView scroll = new ScrollView(act);
    outer.addView(scroll);
    
    LinearLayout card = new LinearLayout(act);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(makeRoundRect(Color.parseColor("#FFFFFF"), dp(act, 16)));
    card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
    scroll.addView(card);
    
    TextView title = new TextView(act);
    title.setText("PB数据预览");
    title.setTextSize(17);
    title.setTextColor(Color.parseColor("#222222"));
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 0, 0, dp(act, 20));
    card.addView(title);
    
    card.addView(makeSubTitleCompact(act, "服务名", Color.parseColor("#666666")));
    TextView tvService = new TextView(act);
    tvService.setText(service.isEmpty() ? "MessageSvc.PbSendMsg (默认)" : service);
    tvService.setTextSize(13);
    tvService.setTextColor(Color.parseColor("#222222"));
    tvService.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
    tvService.setBackground(makeRoundRect(Color.parseColor("#F7F8FA"), dp(act, 6)));
    card.addView(tvService);
    
    card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", Color.parseColor("#666666")));
    TextView tvPB = new TextView(act);
    tvPB.setText(pbData);
    tvPB.setTextSize(13);
    tvPB.setTextColor(Color.parseColor("#222222"));
    tvPB.setPadding(dp(act, 12), dp(act, 12), dp(act, 12), dp(act, 12));
    tvPB.setBackground(makeRoundRect(Color.parseColor("#F7F8FA"), dp(act, 6)));
    tvPB.setMinLines(6);
    card.addView(tvPB);
    
    card.addView(makeSubTitleCompact(act, "编码预览", Color.parseColor("#666666")));
    TextView tvEncoded = new TextView(act);
    try {
        JSONObject json = new JSONObject(pbData);
        FunProtoData proto = new FunProtoData();
        proto.fromJSON(json);
        byte[] encoded = proto.toBytes();
        tvEncoded.setText("长度: " + encoded.length + " 字节\nHEX: " + PacketHelper.bytesToHex(encoded));
    } catch (Exception e) {
        tvEncoded.setText("解析失败: " + e.getMessage());
    }
    tvEncoded.setTextSize(11);
    tvEncoded.setTextColor(Color.parseColor("#666666"));
    tvEncoded.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
    tvEncoded.setBackground(makeRoundRect(Color.parseColor("#F7F8FA"), dp(act, 6)));
    card.addView(tvEncoded);
    
    TextView btnClose = makeActionBtn(act, "关闭", Color.parseColor("#666666"), Color.parseColor("#F7F8FA"));
    btnClose.setPadding(0, dp(act, 20), 0, 0);
    card.addView(btnClose);
    btnClose.setOnClickListener(v -> dialog.dismiss());
    
    dialog.setContentView(outer);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
    }
    dialog.show();
}
