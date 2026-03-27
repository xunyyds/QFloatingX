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
 *             traceLog("response.log", proto.toJSON().toString());
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
 * traceLog("pb.log", "解析结果: " + json.toString(2));
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
 *             traceLog("response.log", response.toJSON().toString());
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
            traceLog("packet_error.log", "发送请求失败: " + e.getMessage());
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
            
            traceLog("pb_received.log", logContent.toString());
            
        } catch (Exception e) {
            traceLog("pb_received.log", "解析PB数据失败: " + e.getMessage() + 
                "\n原始HEX: " + bytesToHex(data));
        }
    }
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
                    traceLog("pb_sent.log", logContent.toString());
                    
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

/**
 * 发送指定表情回应（群聊）
 *
 * @param data      消息数据对象，包含群号、消息序号等信息
 * @param faceIndex 表情ID（QQ表情编号）
 */
void sendSpecifiedFaceReply(Object data, int faceIndex) {
    if (data == null || data.data == null || data.type != 2) {
        qqToast(1, "仅支持群聊表情回应");
        return;
    }

    String groupUin = String.valueOf(data.peerUin);
    long msgSeq = (long) data.data.msgSeq;

    if (Long.parseLong(groupUin) <= 0 || msgSeq <= 0) {
        qqToast(1, "群号或Seq无效");
        return;
    }

    try {
        // Oidb请求结构说明：
        // 1: 固定36994（命令字）
        // 2: 固定1（服务类型）
        // 4: 消息体
        //   2: 群号
        //   3: 消息序号
        //   4: 表情ID（服务端要求字符串形式）
        //   5: 固定1（操作类型）
        //   6: 固定0
        //   7: 固定0
        // 12: 固定1（标志位）
        String jsonStr = "{\"1\":36994,\"2\":1,\"4\":{\"2\":" + groupUin
                + ",\"3\":" + msgSeq + ",\"4\":\"" + faceIndex
                + "\",\"5\":1,\"6\":0,\"7\":0},\"12\":1}";

        JSONObject json = new JSONObject(jsonStr);
        FunProtoData proto = new FunProtoData();
        proto.fromJSON(json);
        byte[] pbData = proto.toBytes();

        PacketHelper.sendRequest("OidbSvcTrpcTcp.0x9082_2", pbData, new IReceiver() {
            public void onReceive(byte[] resp) {
                if (resp != null) {
                    // qqToast(2, "表情回应成功！");
                } else {
                    qqToast(1, "表情回应失败");
                }
            }
        });

    } catch (Exception e) {
        qqToast(1, "发送异常: " + e.getMessage());
    }
}

/**
 * 随机选择一个表情进行回应
 *
 * @param data 消息数据对象
 */
void randomFaceReply(Object data) {
    String cfg = getString("config", "face_reply_config", "1~200");
    List faces = parseFaceConfig(cfg);

    if (faces.isEmpty()) {
        qqToast(1, "表情配置为空，请先设置");
        showFaceReplyConfigDialog(data);
        return;
    }

    int randomFace = (Integer) faces.get(new Random().nextInt(faces.size()));
    sendSpecifiedFaceReply(data, randomFace);
}

/**
 * 解析表情配置字符串
 * <p>
 * 支持格式：
 * <ul>
 *   <li>范围：如 "1~200" 表示1到200之间的所有整数</li>
 *   <li>列表：如 "75,82,355,307" 表示指定表情ID</li>
 * </ul>
 *
 * @param cfg 配置字符串
 * @return 表情ID列表
 */
private List parseFaceConfig(String cfg) {
    List list = new ArrayList();
    cfg = cfg.trim();
    if (cfg.contains("~")) {
        String[] parts = cfg.split("~");
        int min = Integer.parseInt(parts[0].trim());
        int max = Integer.parseInt(parts[1].trim());
        for (int i = Math.max(1, min); i <= Math.min(1000, max); i++) {
            list.add(i);
        }
    } else {
        for (String s : cfg.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                try {
                    list.add(Integer.parseInt(t));
                } catch (Exception ignored) {}
            }
        }
    }
    return list;
}

/**
 * 显示表情回应配置弹窗
 * 展示发送者信息、消息预览，允许用户设置表情范围/列表，发送间隔
 * 点击“保存并使用”后，立即按照配置依次发送表情
 *
 * @param data 消息数据对象
 */
void showFaceReplyConfigDialog(Object data) {
    Activity act = getNowActivity();
    if (act == null || act.isFinishing()) return;

    String nick = (data.data != null && data.data.sendNickName != null)
            ? (String) data.data.sendNickName : "未知昵称";
    String qq = (data.userUin != null) ? (String) data.userUin : "未知QQ";
    String msg = (data.msg != null) ? (String) data.msg : "";
    if (msg.length() > 100) msg = msg.substring(0, 97) + "...";
    String savedCfg = getString("config", "face_reply_config", "1~200");
    String savedDelay = getString("config", "face_reply_delay", "200");

    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(act);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int accentColor = isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
                int borderColor = adjustAlpha(textColor, 0.3f);
                int errorColor = Color.parseColor("#FFE53935");

                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(act, 16), dp(act, 20), dp(act, 16), dp(act, 16));

                TextView senderInfo = new TextView(act);
                senderInfo.setText("发送者：" + nick + "(" + qq + ")");
                senderInfo.setTextSize(15);
                senderInfo.setTextColor(textColor);
                senderInfo.setPadding(0, 0, 0, dp(act, 8));
                root.addView(senderInfo);

                TextView msgPreview = new TextView(act);
                msgPreview.setText("消息：" + msg);
                msgPreview.setTextSize(14);
                msgPreview.setTextColor(subTextColor);
                msgPreview.setPadding(0, 0, 0, dp(act, 16));
                root.addView(msgPreview);

                TextView formatTitle = new TextView(act);
                formatTitle.setText("支持格式");
                formatTitle.setTextSize(12);
                formatTitle.setTextColor(subTextColor);
                formatTitle.setPadding(dp(act, 4), 0, 0, dp(act, 4));
                root.addView(formatTitle);

                TextView formatHint = new TextView(act);
                formatHint.setText("• 范围：1~200\n• 列表：75,82,355,307");
                formatHint.setTextSize(13);
                formatHint.setTextColor(subTextColor);
                formatHint.setPadding(dp(act, 4), 0, 0, dp(act, 12));
                root.addView(formatHint);

                final EditText input = new EditText(act);
                input.setHint("输入表情范围或列表");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(13);
                input.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
                input.setText(savedCfg);
                input.setMinLines(2);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(act, 6));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(act, 1), borderColor);
                input.setBackground(inputBg);
                root.addView(input);

                final TextView errorHint = new TextView(act);
                errorHint.setTextSize(12);
                errorHint.setTextColor(errorColor);
                errorHint.setPadding(dp(act, 4), dp(act, 2), dp(act, 4), dp(act, 4));
                errorHint.setVisibility(View.GONE);
                root.addView(errorHint);

                final LinearLayout delayLayout = new LinearLayout(act);
                delayLayout.setOrientation(LinearLayout.HORIZONTAL);
                delayLayout.setGravity(Gravity.CENTER_VERTICAL);
                delayLayout.setPadding(0, dp(act, 8), 0, 0);
                delayLayout.setVisibility(View.GONE);

                TextView delayLabel = new TextView(act);
                delayLabel.setText("间隔(毫秒):");
                delayLabel.setTextSize(13);
                delayLabel.setTextColor(subTextColor);
                delayLabel.setPadding(0, 0, dp(act, 8), 0);
                delayLayout.addView(delayLabel);

                final EditText delayInput = new EditText(act);
                delayInput.setHint("200");
                delayInput.setHintTextColor(subTextColor);
                delayInput.setTextColor(textColor);
                delayInput.setTextSize(13);
                delayInput.setPadding(dp(act, 8), dp(act, 4), dp(act, 8), dp(act, 4));
                delayInput.setText(savedDelay);
                delayInput.setSingleLine(true);
                delayInput.setMinHeight(dp(act, 36));
                delayInput.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 100), LinearLayout.LayoutParams.WRAP_CONTENT));
                GradientDrawable delayBg = new GradientDrawable();
                delayBg.setCornerRadius(dp(act, 4));
                delayBg.setColor(inputBgColor);
                delayBg.setStroke(dp(act, 1), borderColor);
                delayInput.setBackground(delayBg);
                delayLayout.addView(delayInput);

                root.addView(delayLayout);

                LinearLayout btnBox = new LinearLayout(act);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(act, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = new TextView(act);
                cancel.setText("取消");
                cancel.setTextSize(14);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(act, 16), dp(act, 10), dp(act, 16), dp(act, 10));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                final TextView confirm = new TextView(act);
                confirm.setText("保存并使用");
                confirm.setTextSize(14);
                confirm.setTextColor(accentColor);
                confirm.setPadding(dp(act, 16), dp(act, 10), dp(act, 16), dp(act, 10));
                confirm.setEnabled(true);
                confirm.setAlpha(1f);

                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);

                input.addTextChangedListener(new android.text.TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(android.text.Editable s) {
                        String text = s.toString().trim();
                        List faces = null;
                        boolean valid = false;
                        String errorMsg = null;

                        try {
                            faces = parseFaceConfig(text);
                            if (faces != null && !faces.isEmpty()) {
                                valid = true;
                            } else {
                                if (text.isEmpty()) {
                                    errorMsg = "配置不能为空";
                                } else if (!text.matches("[0-9~,\\s]+")) {
                                    errorMsg = "只能包含数字、~ 和 ,";
                                } else {
                                    errorMsg = "无效配置，没有有效表情";
                                }
                                valid = false;
                            }
                        } catch (Exception e) {
                            valid = false;
                            errorMsg = "格式错误：" + e.getMessage();
                        }

                        GradientDrawable bg = (GradientDrawable) input.getBackground();
                        if (valid) {
                            bg.setStroke(dp(act, 1), borderColor);
                            errorHint.setVisibility(View.GONE);
                        } else {
                            bg.setStroke(dp(act, 2), errorColor);
                            errorHint.setText(errorMsg != null ? errorMsg : "格式错误");
                            errorHint.setVisibility(View.VISIBLE);
                        }
                        input.setBackground(bg);

                        if (valid && faces != null && faces.size() > 2) {
                            delayLayout.setVisibility(View.VISIBLE);
                        } else {
                            delayLayout.setVisibility(View.GONE);
                        }

                        confirm.setEnabled(valid);
                        confirm.setAlpha(valid ? 1f : 0.5f);
                    }
                });

                input.setText(input.getText());

                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!confirm.isEnabled()) {
                            qqToast(1, "配置格式错误，请修改");
                            return;
                        }
                        String newCfg = input.getText().toString().trim();
                        if (newCfg.isEmpty()) newCfg = "1~200";
                        putString("config", "face_reply_config", newCfg);

                        List faces = parseFaceConfig(newCfg);
                        if (faces.isEmpty()) {
                            qqToast(1, "配置无效，无可用表情");
                            if (ref[0] != null) ref[0].dismiss();
                            return;
                        }

                        int maxSend = Math.min(faces.size(), 20);
                        final List toSend = faces.subList(0, maxSend);
                        final int faceCount = toSend.size();

                        if (faceCount > 2 && delayLayout.getVisibility() == View.VISIBLE) {
                            String delayStr = delayInput.getText().toString().trim();
                            if (delayStr.isEmpty()) delayStr = "200";
                            putString("config", "face_reply_delay", delayStr);

                            try {
                                final int delayMs = Integer.parseInt(delayStr);
                                if (ref[0] != null) ref[0].dismiss();

                                final int[] index = {0};

                                Runnable sendNext = new Runnable() {
                                    public void run() {
                                        if (index[0] >= faceCount) {
                                            qqToast(2, "已发送 " + faceCount + " 个表情回应");
                                            return;
                                        }
                                        final int currentIdx = index[0];
                                        ThreadPool.execute(new Runnable() {
                                            public void run() {
                                                int faceId = (Integer) toSend.get(currentIdx);
                                                sendSpecifiedFaceReply(data, faceId);
                                                uiHandler.postDelayed(new Runnable() {
                                                    public void run() {
                                                        index[0]++;
                                                        sendNext.run();
                                                    }
                                                }, delayMs);
                                            }
                                        });
                                    }
                                };
                                sendNext.run();

                            } catch (NumberFormatException e) {
                                qqToast(1, "间隔格式错误");
                            }
                        } else {
                            putString("config", "face_reply_delay", "200");
                            if (ref[0] != null) ref[0].dismiss();

                            ThreadPool.execute(new Runnable() {
                                public void run() {
                                    for (int i = 0; i < faceCount; i++) {
                                        int faceId = (Integer) toSend.get(i);
                                        sendSpecifiedFaceReply(data, faceId);
                                    }
                                    act.runOnUiThread(new Runnable() {
                                        public void run() {
                                            qqToast(2, "已发送 " + faceCount + " 个表情回应");
                                        }
                                    });
                                }
                            });
                        }
                    }
                });

                AlertDialog.Builder builder = new AlertDialog.Builder(act,
                        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                final AlertDialog[] ref = new AlertDialog[1];
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(act, ref[0]);
            } catch (Throwable e) {
                qqToast(1, "弹窗显示失败");
            }
        }
    });
}

public void drawqunLuckyChar(String qun) {
        qqToast(1, "空壳");
}

/**
 * 显示语音消息发送配置弹窗
 * <p>
 * 在群聊消息上触发，弹出音色选择和文本输入界面，确认后发送TTS语音消息
 *
 * @param data 消息对象，包含 peerUin（群号）、type（类型）等
 */
void showVoiceSendDialog(Object data) {
    Activity act = getNowActivity();
    if (act == null || act.isFinishing()) return;

    // 仅支持群聊
    if (data == null || data.type != 2) {
        qqToast(1, "仅支持群聊");
        return;
    }

    final String groupUin = String.valueOf(data.peerUin);
    final String msg = String.valueOf(data.msg);
    final String groupName = (data.data != null && data.data.peerName != null)
            ? (String) data.data.peerName : "未知群";

    // 解析音色列表（从 JSON 字符串）
    final JSONArray voiceArray;
    try {
        String jsonStr = "{\n" +
                "  \"code\": 0,\n" +
                "  \"status\": \"success\",\n" +
                "  \"msg\": \"音色列表\",\n" + //此音色列表取自冷雨
                "  \"data\": [\n" +
                "    {\"name\": \"小新\", \"id\": \"lucy-voice-laibixiaoxin\"},\n" +
                "    {\"name\": \"猴哥\", \"id\": \"lucy-voice-houge\"},\n" +
                "    {\"name\": \"四郎\", \"id\": \"lucy-voice-silang\"},\n" +
                "    {\"name\": \"东北老妹儿\", \"id\": \"lucy-voice-guangdong-f1\"},\n" +
                "    {\"name\": \"广西大表哥\", \"id\": \"lucy-voice-guangxi-m1\"},\n" +
                "    {\"name\": \"妲己\", \"id\": \"lucy-voice-daji\"},\n" +
                "    {\"name\": \"霸道总裁\", \"id\": \"lucy-voice-lizeyan\"},\n" +
                "    {\"name\": \"酥心御姐\", \"id\": \"lucy-voice-suxinjiejie\"},\n" +
                "    {\"name\": \"说书先生\", \"id\": \"lucy-voice-m8\"},\n" +
                "    {\"name\": \"憨憨小弟\", \"id\": \"lucy-voice-male1\"},\n" +
                "    {\"name\": \"憨厚老哥\", \"id\": \"lucy-voice-male3\"},\n" +
                "    {\"name\": \"吕布\", \"id\": \"lucy-voice-lvbu\"},\n" +
                "    {\"name\": \"元气少女\", \"id\": \"lucy-voice-xueling\"},\n" +
                "    {\"name\": \"文艺少女\", \"id\": \"lucy-voice-f37\"},\n" +
                "    {\"name\": \"磁性大叔\", \"id\": \"lucy-voice-male2\"},\n" +
                "    {\"name\": \"邻家小妹\", \"id\": \"lucy-voice-female1\"},\n" +
                "    {\"name\": \"低沉男声\", \"id\": \"lucy-voice-m14\"},\n" +
                "    {\"name\": \"傲娇少女\", \"id\": \"lucy-voice-f38\"},\n" +
                "    {\"name\": \"爹系男友\", \"id\": \"lucy-voice-m101\"},\n" +
                "    {\"name\": \"暖心姐姐\", \"id\": \"lucy-voice-female2\"},\n" +
                "    {\"name\": \"温柔妹妹\", \"id\": \"lucy-voice-f36\"},\n" +
                "    {\"name\": \"书香少女\", \"id\": \"lucy-voice-f34\"}\n" +
                "  ]\n" +
                "}";
        JSONObject root = new JSONObject(jsonStr);
        voiceArray = root.getJSONArray("data");
    } catch (Exception e) {
        qqToast(1, "音色列表解析失败");
        return;
    }

    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(act);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int accentColor = isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(act, 16), dp(act, 20), dp(act, 16), dp(act, 16));

                // 群信息
                TextView groupInfo = new TextView(act);
                groupInfo.setText("群聊：" + groupName + "(" + groupUin + ")");
                groupInfo.setTextSize(15);
                groupInfo.setTextColor(textColor);
                groupInfo.setPadding(0, 0, 0, dp(act, 16));
                root.addView(groupInfo);

                // 音色ID输入行
                LinearLayout voiceRow = new LinearLayout(act);
                voiceRow.setOrientation(LinearLayout.HORIZONTAL);
                voiceRow.setGravity(Gravity.CENTER_VERTICAL);
                voiceRow.setPadding(0, 0, 0, dp(act, 12));

                TextView voiceLabel = new TextView(act);
                voiceLabel.setText("音色ID：");
                voiceLabel.setTextSize(14);
                voiceLabel.setTextColor(subTextColor);
                voiceLabel.setPadding(0, 0, dp(act, 8), 0);
                voiceRow.addView(voiceLabel);

                final EditText etVoiceId = new EditText(act);
                etVoiceId.setHint("可手动输入或点击选择");
                etVoiceId.setHintTextColor(subTextColor);
                etVoiceId.setTextColor(textColor);
                etVoiceId.setTextSize(13);
                etVoiceId.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
                etVoiceId.setSingleLine(true);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(act, 6));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(act, 1), borderColor);
                etVoiceId.setBackground(inputBg);
                etVoiceId.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                voiceRow.addView(etVoiceId);

                TextView selectBtn = new TextView(act);
                selectBtn.setText("选择");
                selectBtn.setTextSize(14);
                selectBtn.setTextColor(accentColor);
                selectBtn.setPadding(dp(act, 16), dp(act, 8), dp(act, 16), dp(act, 8));
                selectBtn.setBackgroundDrawable(null); // 纯文本无背景
                voiceRow.addView(selectBtn);

                root.addView(voiceRow);

                // 可选显示选中的音色名称
                final TextView tvVoiceName = new TextView(act);
                tvVoiceName.setTextSize(12);
                tvVoiceName.setTextColor(subTextColor);
                tvVoiceName.setPadding(dp(act, 4), 0, 0, dp(act, 12));
                root.addView(tvVoiceName);

                // 文本输入
                TextView textLabel = new TextView(act);
                textLabel.setText("语音文本：");
                textLabel.setTextSize(14);
                textLabel.setTextColor(subTextColor);
                textLabel.setPadding(0, 0, 0, dp(act, 4));
                root.addView(textLabel);

                final EditText etText = new EditText(act);
                etText.setHint("请输入要转为语音的文本");
                etText.setText(msg);
                etText.setHintTextColor(subTextColor);
                etText.setTextColor(textColor);
                etText.setTextSize(13);
                etText.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
                etText.setMinLines(3);
                GradientDrawable textBg = new GradientDrawable();
                textBg.setCornerRadius(dp(act, 6));
                textBg.setColor(inputBgColor);
                textBg.setStroke(dp(act, 1), borderColor);
                etText.setBackground(textBg);
                root.addView(etText);

                // 按钮行
                LinearLayout btnRow = new LinearLayout(act);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setPadding(0, dp(act, 20), 0, 0);
                btnRow.setGravity(Gravity.RIGHT);

                TextView cancelBtn = new TextView(act);
                cancelBtn.setText("取消");
                cancelBtn.setTextSize(14);
                cancelBtn.setTextColor(subTextColor);
                cancelBtn.setPadding(dp(act, 16), dp(act, 10), dp(act, 16), dp(act, 10));
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView sendBtn = new TextView(act);
                sendBtn.setText("发送");
                sendBtn.setTextSize(14);
                sendBtn.setTextColor(accentColor);
                sendBtn.setPadding(dp(act, 16), dp(act, 10), dp(act, 16), dp(act, 10));

                btnRow.addView(cancelBtn);
                btnRow.addView(sendBtn);
                root.addView(btnRow);

                AlertDialog.Builder builder = new AlertDialog.Builder(act,
                        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                final AlertDialog[] ref = new AlertDialog[1];
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(act, ref[0]);

                // 选择按钮点击：弹出音色列表
                selectBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        showVoiceListDialog(act, voiceArray, etVoiceId, tvVoiceName);
                    }
                });

                // 发送按钮点击
                sendBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String voiceId = etVoiceId.getText().toString().trim();
                        if (voiceId.isEmpty()) {
                            qqToast(1, "请输入或选择音色ID");
                            return;
                        }
                        String text = etText.getText().toString().trim();
                        if (text.isEmpty()) {
                            qqToast(1, "请输入文本");
                            return;
                        }
                        ref[0].dismiss();

                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                sendVoiceMessage(groupUin, voiceId, text);
                            }
                        });
                    }
                });

            } catch (Exception e) {
                qqToast(1, "弹窗显示失败");
            }
        }
    });
}

/**
 * 显示音色选择列表弹窗
 *
 * @param act        Activity
 * @param voiceArray 音色 JSON 数组
 * @param etVoiceId  主弹窗中音色ID输入框
 * @param tvVoiceName 主弹窗中显示音色名称的TextView
 */
void showVoiceListDialog(final Activity act, final JSONArray voiceArray,
                          final EditText etVoiceId, final TextView tvVoiceName) {
    if (act == null || act.isFinishing()) return;

    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(act);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int accentColor = isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT;

                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(act, 16), dp(act, 20), dp(act, 16), dp(act, 16));

                TextView title = new TextView(act);
                title.setText("选择音色");
                title.setTextSize(17);
                title.setTextColor(textColor);
                title.setGravity(Gravity.CENTER);
                title.setPadding(0, 0, 0, dp(act, 16));
                root.addView(title);

                ScrollView scroll = new ScrollView(act);
                int maxHeight = (int) (act.getResources().getDisplayMetrics().heightPixels * 0.5);
                scroll.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, maxHeight));

                LinearLayout listContainer = new LinearLayout(act);
                listContainer.setOrientation(LinearLayout.VERTICAL);
                scroll.addView(listContainer);

                for (int i = 0; i < voiceArray.length(); i++) {
                    try {
                        JSONObject item = voiceArray.getJSONObject(i);
                        final String name = item.getString("name");
                        final String id = item.getString("id");

                        TextView itemView = new TextView(act);
                        itemView.setText(name);
                        itemView.setTextSize(14);
                        itemView.setTextColor(textColor);
                        itemView.setPadding(dp(act, 16), dp(act, 12), dp(act, 16), dp(act, 12));
                        
                        StateListDrawable stateListDrawable = new StateListDrawable();
                        ColorDrawable pressedDrawable = new ColorDrawable(adjustAlpha(Color.BLACK, 0.1f));
                        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
                        stateListDrawable.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
                        itemView.setBackground(stateListDrawable);

                        itemView.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                etVoiceId.setText(id);
                                tvVoiceName.setText("已选择：" + name);
                                if (dialogRef[0] != null) dialogRef[0].dismiss();
                            }
                        });

                        listContainer.addView(itemView);

                    } catch (Exception e) {
                        // 跳过错误项
                    }
                }

                root.addView(scroll);

                TextView closeBtn = new TextView(act);
                closeBtn.setText("关闭");
                closeBtn.setTextSize(14);
                closeBtn.setTextColor(subTextColor);
                closeBtn.setGravity(Gravity.CENTER);
                closeBtn.setPadding(0, dp(act, 16), 0, 0);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (dialogRef[0] != null) dialogRef[0].dismiss();
                    }
                });
                root.addView(closeBtn);

                AlertDialog.Builder builder = new AlertDialog.Builder(act,
                        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                final AlertDialog[] dialogRef = new AlertDialog[1];
                dialogRef[0] = builder.create();
                dialogRef[0].show();
                applyUiTheme(act, dialogRef[0]);

            } catch (Exception e) {
                qqToast(1, "列表弹窗显示失败");
            }
        }
    });
}

/**
 * 发送语音消息（Oidb 0x11ca_0）
 *
 * @param groupUin 群号
 * @param voiceId  音色ID
 * @param text     要转换的文本
 */
void sendVoiceMessage(String groupUin, String voiceId, String text) {
    try {
        // 构造 JSON 结构
        // {
        //   "1": 37531,
        //   "2": 0,
        //   "4": {
        //     "1": 群号,
        //     "2": "音色ID",
        //     "3": "文本",
        //     "4": 1,
        //     "5": { "1": 1773685283 },
        //     "12": 0
        //   }
        // }
        JSONObject body = new JSONObject();
        body.put("1", Long.parseLong(groupUin));
        body.put("2", voiceId);
        body.put("3", text);
        body.put("4", 1);

        JSONObject field5 = new JSONObject();
        field5.put("1", 1773685283L);
        body.put("5", field5);
        body.put("12", 0);

        JSONObject root = new JSONObject();
        root.put("1", 37531);
        root.put("2", 0);
        root.put("4", body);

        FunProtoData proto = new FunProtoData();
        proto.fromJSON(root);
        byte[] pbData = proto.toBytes();

        PacketHelper.sendRequest("OidbSvcTrpcTcp.0x11ca_0", pbData, new IReceiver() {
            public void onReceive(byte[] resp) {
                if (resp != null) {
                    qqToast(2, "AI声聊语音发送成功");
                } else {
                    qqToast(1, "AI声聊语音发送失败");
                }
            }
        });

    } catch (Exception e) {
        qqToast(1, "发送异常: " + e.getMessage());
    }
}


void showTrafficRedPacketDialog(Object data) {
    if (data == null || data.type != 2) {
        qqToast(1, "仅支持群聊使用");
        return;
    }
    String groupUin = String.valueOf(data.peerUin);
    if (groupUin.equals("0") || groupUin.equals("-1")) {
        qqToast(1, "无法获取群号");
        return;
    }
    Activity act = getNowActivity();
    if (act == null) return;
    act.runOnUiThread(new Runnable() {
        public void run() {
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
            card.setBackground(makeRoundRect(isThemeDark(act) ? Color.parseColor("#FF2D2D2D") : Color.WHITE, dp(act, 16)));
            card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
            scroll.addView(card);

            TextView tvTitle = new TextView(act);
            tvTitle.setText("正在偷取你们的流量");
            tvTitle.setTextSize(18);
            tvTitle.setTextColor(isThemeDark(act) ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT);
            tvTitle.setGravity(Gravity.CENTER);
            tvTitle.setPadding(0, 0, 0, dp(act, 16));
            card.addView(tvTitle);

            boolean dark = isThemeDark(act);
            int bg = dark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
            int subColor = dark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;

            card.addView(makeSubTitleCompact(act, "外显链接", subColor));
            EditText et1 = makeInputCompact(act, "www.10086.cn", "", bg);
            card.addView(et1);

            card.addView(makeSubTitleCompact(act, "标题", subColor));
            EditText et2 = makeInputCompact(act, "中国移动", "", bg);
            card.addView(et2);

            card.addView(makeSubTitleCompact(act, "描述", subColor));
            EditText et3 = makeInputCompact(act, "正在给你发送流量红包", "", bg);
            card.addView(et3);

            card.addView(makeSubTitleCompact(act, "预览链接", subColor));
            EditText et4 = makeInputCompact(act, "https://autopatchcn.yuanshen.com/client_app/update/hk4e_cn/game_5.3.0_5.4.0_hdiff_pMLdaxlPCASusOeB.zip", "", bg);
            card.addView(et4);

            LinearLayout btnLayout = new LinearLayout(act);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.CENTER);
            btnLayout.setPadding(0, dp(act, 20), 0, 0);

            TextView cancel = makeActionBtn(act, "取消", Color.parseColor("#666666"), dark ? Color.parseColor("#FF3D3D3D") : Color.parseColor("#F7F8FA"));
            TextView send = makeActionBtn(act, "发送", Color.WHITE, Color.parseColor("#3B71FE"));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(act, 44), 1f);
            lp.setMargins(0, 0, dp(act, 12), 0);
            cancel.setLayoutParams(lp);
            send.setLayoutParams(new LinearLayout.LayoutParams(0, dp(act, 44), 1f));

            btnLayout.addView(cancel);
            btnLayout.addView(send);
            card.addView(btnLayout);

            cancel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { dialog.dismiss(); }
            });

            send.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String v1 = et1.getText().toString().trim();
                    String v2 = et2.getText().toString().trim();
                    String v3 = et3.getText().toString().trim();
                    String v4 = et4.getText().toString().trim();

                    dialog.dismiss();
                    try {
                        JSONObject inner = new JSONObject();
                        inner.put("1", v1.isEmpty() ? "www.10086.cn" : v1);

                        JSONObject inner14 = new JSONObject();
                        inner14.put("1", v2.isEmpty() ? "中国移动" : v2);
                        inner14.put("2", v3.isEmpty() ? "正在给你发送流量红包" : v3);
                        inner14.put("3", v4.isEmpty() ? "https://autopatchcn.yuanshen.com/client_app/update/hk4e_cn/game_5.3.0_5.4.0_hdiff_pMLdaxlPCASusOeB.zip" : v4);

                        JSONObject inner12 = new JSONObject();
                        inner12.put("14", inner14);
                        inner.put("12", inner12);

                        String compressed = compressToField7(inner.toString());

                        JSONObject root = new JSONObject();
                        JSONObject f1 = new JSONObject();
                        JSONObject f1_2 = new JSONObject();
                        f1_2.put("1", Long.parseLong(groupUin));
                        f1.put("2", f1_2);
                        root.put("1", f1);

                        JSONObject f2 = new JSONObject();
                        f2.put("1", 1); f2.put("2", 0); f2.put("3", 0);
                        root.put("2", f2);

                        JSONObject f3 = new JSONObject();
                        JSONObject f3_1 = new JSONObject();
                        JSONObject f3_1_2 = new JSONObject();
                        JSONObject f3_1_2_37 = new JSONObject();
                        f3_1_2_37.put("17", 0);

                        JSONObject f19 = new JSONObject();
                        f19.put("41", 0);
                        f19.put("15", 0);
                        f19.put("31", 0);
                        f3_1_2_37.put("19", f19);

                        f3_1_2_37.put("6", 1);
                        f3_1_2_37.put("7", compressed);

                        f3_1_2.put("37", f3_1_2_37);
                        f3_1.put("2", f3_1_2);
                        f3.put("1", f3_1);
                        root.put("3", f3);

                        root.put("4", 4100116396);
                        root.put("5", 0);

                        FunProtoData proto = new FunProtoData();
                        proto.fromJSON(root);
                        byte[] pb = proto.toBytes();
                        traceLog(" _log.txt", "" + root);

                        PacketHelper.sendRequest("MessageSvc.PbSendMsg", pb, new IReceiver() {
                            public void onReceive(byte[] resp) {
                                act.runOnUiThread(new Runnable() {
                                    public void run() {
                                        qqToast(resp != null ? 2 : 1, resp != null ? "偷流量成功！" : "发送失败");
                                    }
                                });
                            }
                        });
                    } catch (Exception e) {
                        qqToast(1, "异常: " + e.getMessage());
                        traceLog(" _log.txt",""+e);
                    }
                }
            });

            dialog.setContentView(outer);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.88), -2);
            }
            dialog.show();
            applyUiTheme(act, dialog);
        }
    });
}
String compressToField7(String jsonStr) throws Exception {
    JSONObject json = new JSONObject(jsonStr);
    FunProtoData proto = new FunProtoData();
    proto.fromJSON(json);
    byte[] protoBytes = proto.toBytes();

    return java.util.Base64.getEncoder().withoutPadding().encodeToString(protoBytes);
}

void RecallMessage(Object data, long seq) {
    if (data == null || data.data == null) {
        qqToast(1, "数据无效");
        return;
    }

    final int chatType = data.type;
    final String peerUid;
    final long msgId = data.data.msgId;

    if (chatType == 2) { // 群聊
        peerUid = String.valueOf(data.peerUin);
    } else if (chatType == 1) { // 私聊
        peerUid = (String) data.peerUid;
    } else {
        qqToast(1, "不支持的聊天类型");
        return;
    }

    if (msgId == 0) {
        qqToast(1, "消息ID无效");
        return;
    }

    // 获取真实消息记录
    fetchRealMsgRecord(msgId, chatType, peerUid, new MsgLoadedCallback() {
        public void onLoaded(MsgData realData) {
            if (realData == null || realData.data == null) {
                qqToast(1, "获取消息数据失败");
                return;
            }

            try {
                String serviceCmd;
                JSONObject json = new JSONObject();

                if (chatType == 2) { // 群聊撤回


                    if (groupUin <= 0 || msgSeq <= 0 || msgRandom <= 0) {
                        qqToast(1, "群聊参数无效");
                        return;
                    }

                    json.put("1", 1);
                    json.put("2", groupUin);

                    JSONObject field3 = new JSONObject();
                    field3.put("1", msgSeq);
                    field3.put("2", msgRandom);
                    field3.put("3", 0);
                    json.put("3", field3);

                    JSONObject field4 = new JSONObject();
                    field4.put("1", 0);
                    json.put("4", field4);

                    serviceCmd = "trpc.msg.msg_svc.MsgService.SsoGroupRecallMsg";

                } else if (chatType == 2) { // 私聊撤回
                    String peerUidStr = peerUid;
                    long clientSeq = realData.data.clientSeq;
                    long msgRandom = realData.data.msgRandom;
                    long realMsgId = realData.data.msgId;
                    long timestamp = realData.time * 1000L;
                    long msgSeq = realData.data.msgSeq;

                    if (peerUidStr == null || peerUidStr.isEmpty() || clientSeq <= 0 || msgRandom <= 0 ||
                            realMsgId <= 0 || timestamp <= 0 || msgSeq <= 0) {
                        qqToast(1, "私聊参数无效");
                        return;
                    }

                    json.put("1", 1);
                    json.put("2", Long.parseLong(peerUidStr));

                    JSONObject field4 = new JSONObject();
                    field4.put("1", clientSeq);
                    field4.put("2", msgRandom);
                    field4.put("3", realMsgId);
                    field4.put("4", timestamp);
                    field4.put("5", 0);
                    field4.put("6", msgSeq+1);
                    json.put("4", field4);

                    JSONObject field5 = new JSONObject();
                    field5.put("1", 0);
                    field5.put("2", 0);
                    json.put("5", field5);

                    json.put("6", 0);

                    serviceCmd = "trpc.msg.msg_svc.MsgService.SsoC2CRecallMsg";

                } else {
                    qqToast(1, "不支持的聊天类型");
                    return;
                }

                traceLog("recall_error.log", json.toString());

                FunProtoData proto = new FunProtoData();
                proto.fromJSON(json);
                byte[] pbData = proto.toBytes();

                PacketHelper.sendRequest(serviceCmd, pbData, new IReceiver() {
                    public void onReceive(byte[] resp) {
                        if (resp != null) {
                            qqToast(2, "撤回成功");
                        } else {
                            qqToast(1, "撤回失败");
                        }
                    }
                });

            } catch (Exception e) {
                qqToast(1, "发送异常: " + e.getMessage());
            }
        }
    });
}
void setMsgEssence(Object data, boolean isEssence) {
    try {
        long groupUin = Long.parseLong(data.peerUid);
        long msgSeq = data.data.msgSeq;
        long msgRandom = data.data.msgRandom;

        JSONObject body = new JSONObject();
        body.put("1", groupUin);
        body.put("2", msgSeq);
        body.put("3", msgRandom);

        // 构造根JSON
        JSONObject root = new JSONObject();
        root.put("1", 3756);
        root.put("2", 1);
        root.put("3", 0);
        root.put("4", body);

        String cmd = isEssence ? "OidbSvc.0xeac_1" : "OidbSvc.0xeac_2";

        FunProtoData proto = new FunProtoData();
        proto.fromJSON(root);
        byte[] pbData = proto.toBytes();

        PacketHelper.sendRequest(cmd, pbData, new IReceiver() {
            public void onReceive(byte[] resp) {
                if (resp != null) {
                    qqToast(2, isEssence ? "设为精华成功" : "取消精华成功");
                } else {
                    qqToast(1, isEssence ? "设为精华失败" : "取消精华失败");
                }
            }
        });

    } catch (Exception e) {
        qqToast(1, (isEssence ? "设置" : "取消") + "精华异常: " + e.getMessage());
    }
}