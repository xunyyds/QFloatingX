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

public interface IReceiver {
    void onReceive(byte[] data);
}

public class FunProtoData {
    public HashMap values = new HashMap();

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
        } catch (Throwable ignored) { traceLog("api9_log", "[fromJSON] 异常: " + ignored); }
    }

    public void putValue(int fieldNumber, Object value) {
        List list = (List) values.get(fieldNumber);
        if (list == null) {
            list = new ArrayList();
            values.put(fieldNumber, list);
        }
        list.add(value);
    }

    public void fromBytes(byte[] b) throws Exception {
        if (b == null) return;

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
            sb.append(hexByte(b).toUpperCase());
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

    public Object getFirstValue(int fieldNumber) {
        List list = (List) values.get(fieldNumber);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public List getValues(int fieldNumber) {
        return (List) values.get(fieldNumber);
    }

    public boolean hasField(int fieldNumber) {
        return values.containsKey(fieldNumber);
    }

    public void clear() {
        values.clear();
    }
}

public class PacketHelper {

    public static byte[] compressGzip(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        gos.write(data);
        gos.close();
        return bos.toByteArray();
    }

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

    public static byte[] packet(byte[] data) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(data.length + 4);
        dos.write(data);
        return bos.toByteArray();
    }

    public static void sendRequest(byte[] rawData, IReceiver receiver) {
        sendRequest(null, rawData, receiver);
    }

    public static void sendRequest(String serviceCmd, byte[] rawData, IReceiver receiver) {
        if (receiver == null) return;
        if (rawData == null || rawData.length == 0) {
            receiver.onReceive(null);
            return;
        }

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
            traceLog("api9_log", "发送请求失败: " + e.getMessage());
            receiver.onReceive(null);
        }
    }

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

            traceLog("api9_log", logContent.toString());

        } catch (Exception e) {
            traceLog("api9_log", "解析PB数据失败: " + e.getMessage() +
                "\n原始HEX: " + bytesToHex(data));
        }
    }
}

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
        card.setBackground(roundRect(pc("#FFFFFF"), dp(act, 16)));
        card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
        scroll.addView(card);
        
        TextView title = new TextView(act);
        title.setText("PB发包工具");
        title.setTextSize(17);
        title.setTextColor(pc("#222222"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(act, 20));
        card.addView(title);
        
        // 服务名输入
        card.addView(makeSubTitleCompact(act, "服务名", pc("#666666")));
        EditText etService = makeInput(act, "MessageSvc.PbSendMsg", null);
        card.addView(etService);
        
        // PB数据输入
        card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", pc("#666666")));
        EditText etPB = makeInput(act, "{\"1\":123,\"2\":\"示例数据\"}", null);
        etPB.setMinLines(4);
        card.addView(etPB);
        
        // 模板管理区域
        card.addView(makeSubTitleCompact(act, "模板管理", pc("#666666")));
        
        LinearLayout templateContainer = new LinearLayout(act);
        templateContainer.setOrientation(LinearLayout.HORIZONTAL);
        templateContainer.setGravity(Gravity.CENTER);
        templateContainer.setPadding(0, 0, 0, dp(act, 12));
        card.addView(templateContainer);
        
        TextView btnSave = createButton(act, "保存模板", Color.WHITE, pc("#3B71FE"), 14f, 8, 16, 10, false, 0, 0, null);
        TextView btnLoad = createButton(act, "使用模板", pc("#3B71FE"), pc("#E8EEFF"), 14f, 8, 16, 10, false, 0, 0, null);
        TextView btnPreview = createButton(act, "预览", pc("#3B71FE"), pc("#E8EEFF"), 14f, 8, 16, 10, false, 0, 0, null);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, -2, 1);
        btnParams.setMargins(0, 0, dp(act, 8), 0);
        btnSave.setLayoutParams(btnParams);
        btnLoad.setLayoutParams(btnParams);
        btnPreview.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        
        templateContainer.addView(btnSave);
        templateContainer.addView(btnLoad);
        templateContainer.addView(btnPreview);
        
        EditText etTemplateName = makeInput(act, "模板名称", null);
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
        
        TextView btnCancel = createButton(act, "取消", pc("#666666"), pc("#F7F8FA"), 14f, 8, 16, 10, false, 0, 0, null);
        TextView btnConfirm = createButton(act, "发送", Color.WHITE, pc("#3B71FE"), 14f, 8, 16, 10, false, 0, 0, null);
        
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
                    traceLog("api9_log", logContent.toString());
                    
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
    card.setBackground(roundRect(pc("#FFFFFF"), dp(act, 16)));
    card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
    scroll.addView(card);
    
    TextView title = new TextView(act);
    title.setText("选择模板");
    title.setTextSize(17);
    title.setTextColor(pc("#222222"));
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
    } catch (Throwable e) { traceLog("api9_log", "[showTemplateSelectorDialog] 异常: " + e); }
    
    if (templates.isEmpty()) {
        TextView empty = new TextView(act);
        empty.setText("暂无保存的模板");
        empty.setTextSize(14);
        empty.setTextColor(pc("#BBBBBB"));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dp(act, 40), 0, dp(act, 40));
        card.addView(empty);
    } else {
        for (Object entry : templates.entrySet()) {
            Map.Entry e = (Map.Entry) entry;
            String name = (String) e.getKey();
            String templateData = (String) e.getValue();
            
            TextView templateItem = createButton(act, name, pc("#222222"), pc("#F7F8FA"), 14f, 8, 16, 12, false, 0, 0, null);
            templateItem.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
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
    
    TextView btnClose = createButton(act, "关闭", pc("#666666"), pc("#F7F8FA"), 14f, 8, 0, 20, false, 0, 0, null);
    card.addView(btnClose);
    btnClose.setOnClickListener(v -> dialog.dismiss());
    
    dialog.setContentView(outer);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
    }
    dialog.show();
}

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
    card.setBackground(roundRect(pc("#FFFFFF"), dp(act, 16)));
    card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
    scroll.addView(card);
    
    TextView title = new TextView(act);
    title.setText("PB数据预览");
    title.setTextSize(17);
    title.setTextColor(pc("#222222"));
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 0, 0, dp(act, 20));
    card.addView(title);
    
    card.addView(makeSubTitleCompact(act, "服务名", pc("#666666")));
    TextView tvService = new TextView(act);
    tvService.setText(service.isEmpty() ? "MessageSvc.PbSendMsg (默认)" : service);
    tvService.setTextSize(13);
    tvService.setTextColor(pc("#222222"));
    tvService.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
    tvService.setBackground(roundRect(pc("#F7F8FA"), dp(act, 6)));
    card.addView(tvService);
    
    card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", pc("#666666")));
    TextView tvPB = new TextView(act);
    tvPB.setText(pbData);
    tvPB.setTextSize(13);
    tvPB.setTextColor(pc("#222222"));
    tvPB.setPadding(dp(act, 12), dp(act, 12), dp(act, 12), dp(act, 12));
    tvPB.setBackground(roundRect(pc("#F7F8FA"), dp(act, 6)));
    tvPB.setMinLines(6);
    card.addView(tvPB);
    
    card.addView(makeSubTitleCompact(act, "编码预览", pc("#666666")));
    TextView tvEncoded = new TextView(act);
    tvEncoded.setTextColor(tc(act, "on_surface"));
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
    tvEncoded.setTextColor(pc("#666666"));
    tvEncoded.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
    tvEncoded.setBackground(roundRect(pc("#F7F8FA"), dp(act, 6)));
    card.addView(tvEncoded);
    
    TextView btnClose = createButton(act, "关闭", pc("#666666"), pc("#F7F8FA"), 14f, 8, 0, 20, false, 0, 0, null);
    card.addView(btnClose);
    btnClose.setOnClickListener(v -> dialog.dismiss());
    
    dialog.setContentView(outer);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
    }
    dialog.show();
}

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
                } catch (Throwable ignored) { traceLog("api9_log", "[parseFaceConfig] 异常: " + ignored); }
            }
        }
    }
    return list;
}

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
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int accentColor = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);
                int errorColor = pc("#FFE53935");

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

                final EditText input = makeInput(act, "输入表情范围或列表", null);
                input.setText(savedCfg);
                input.setMinLines(2);
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

                final EditText delayInput = makeInput(act, "200", null);
                delayInput.setText(savedDelay);
                delayInput.setSingleLine(true);
                delayInput.setMinHeight(dp(act, 36));
                delayInput.setPadding(dp(act, 8), dp(act, 4), dp(act, 8), dp(act, 4));
                delayInput.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 100), LinearLayout.LayoutParams.WRAP_CONTENT));
                delayLayout.addView(delayInput);

                root.addView(delayLayout);

                LinearLayout btnBox = new LinearLayout(act);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(act, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = createButton(act, "取消", subTextColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                final TextView confirm = createButton(act, "保存并使用", accentColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);
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
                applyUiTheme(act, ref[0], 0);
            } catch (Throwable e) {
                qqToast(1, "弹窗显示失败");
            }
        }
    });
}

public void drawqunLuckyChar(String qun) {
        qqToast(1, "空壳");
}

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
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int accentColor = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
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

                final EditText etVoiceId = makeInput(act, "可手动输入或点击选择", null);
                etVoiceId.setSingleLine(true);
                etVoiceId.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                voiceRow.addView(etVoiceId);

                TextView selectBtn = createButton(act, "选择", accentColor, Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
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

                final EditText etText = makeInput(act, "请输入要转为语音的文本", null);
                etText.setText(msg);
                etText.setMinLines(3);
                root.addView(etText);

                // 按钮行
                LinearLayout btnRow = new LinearLayout(act);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setPadding(0, dp(act, 20), 0, 0);
                btnRow.setGravity(Gravity.RIGHT);

                TextView cancelBtn = createButton(act, "取消", subTextColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView sendBtn = createButton(act, "发送", accentColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);

                btnRow.addView(cancelBtn);
                btnRow.addView(sendBtn);
                root.addView(btnRow);

                AlertDialog.Builder builder = new AlertDialog.Builder(act,
                        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                final AlertDialog[] ref = new AlertDialog[1];
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(act, ref[0], 0);

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

void showVoiceListDialog(final Activity act, final JSONArray voiceArray,
                          final EditText etVoiceId, final TextView tvVoiceName) {
    if (act == null || act.isFinishing()) return;

    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(act);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int accentColor = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");

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
                        stateListDrawable.addState(new int[0], new ColorDrawable(Color.TRANSPARENT));
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

                TextView closeBtn = createButton(act, "关闭", subTextColor, Color.TRANSPARENT, 14f, 0, 0, 16, false, 0, 0, null);
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
                applyUiTheme(act, dialogRef[0], 0);

            } catch (Exception e) {
                qqToast(1, "列表弹窗显示失败");
            }
        }
    });
}

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
            card.setBackground(roundRect(isThemeDark(act) ? pc("#FF2D2D2D") : Color.WHITE, dp(act, 16)));
            card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
            scroll.addView(card);

            TextView tvTitle = new TextView(act);
            tvTitle.setText("正在偷取你们的流量");
            tvTitle.setTextSize(18);
            tvTitle.setTextColor(isThemeDark(act) ? pc("#FFEFEFEF") : pc("#FF000000"));
            tvTitle.setGravity(Gravity.CENTER);
            tvTitle.setPadding(0, 0, 0, dp(act, 16));
            card.addView(tvTitle);

            boolean dark = isThemeDark(act);
            int subColor = dark ? pc("#99EFEFEF") : pc("#99000000");

            card.addView(makeSubTitleCompact(act, "外显链接", subColor));
            EditText et1 = makeInput(act, "", null);
            et1.setText("www.10086.cn");
            card.addView(et1);

            card.addView(makeSubTitleCompact(act, "标题", subColor));
            EditText et2 = makeInput(act, "", null);
            et2.setText("中国移动");
            card.addView(et2);

            card.addView(makeSubTitleCompact(act, "描述", subColor));
            EditText et3 = makeInput(act, "", null);
            et3.setText("正在给你发送流量红包");
            card.addView(et3);

            card.addView(makeSubTitleCompact(act, "预览链接", subColor));
            EditText et4 = makeInput(act, "", null);
            et4.setText("https://autopatchcn.yuanshen.com/client_app/update/hk4e_cn/game_5.3.0_5.4.0_hdiff_pMLdaxlPCASusOeB.zip");
            card.addView(et4);

            LinearLayout btnLayout = new LinearLayout(act);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setGravity(Gravity.CENTER);
            btnLayout.setPadding(0, dp(act, 20), 0, 0);

            TextView cancel = createButton(act, "取消", pc("#666666"), dark ? pc("#FF3D3D3D") : pc("#F7F8FA"), 14f, 8, 16, 10, false, 0, 0, null);
            TextView send = createButton(act, "发送", Color.WHITE, pc("#3B71FE"), 14f, 8, 16, 10, false, 0, 0, null);

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

                        String jsonStr = inner.toString();
                        byte[] jsonBytes = jsonStr.getBytes(StandardCharsets.UTF_8);
                        byte[] compressedBytes = PacketHelper.compressGzip(jsonBytes);
                        String hexCompressed = PacketHelper.bytesToHex(compressedBytes);

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
                        f3_1_2_37.put("7", hexCompressed);

                        f3_1_2.put("37", f3_1_2_37);
                        f3_1.put("2", f3_1_2);
                        f3.put("1", f3_1);
                        root.put("3", f3);

                        root.put("4", 4100116396L);
                        root.put("5", 0);

                        FunProtoData proto = new FunProtoData();
                        proto.fromJSON(root);
                        byte[] pb = proto.toBytes();
                        traceLog("api9_log", "" + root);

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
                        traceLog("api9_log",""+e);
                    }
                }
            });

            dialog.setContentView(outer);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.88), -2);
            }
            dialog.show();
            applyUiTheme(act, dialog, 0);
        }
    });
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

                traceLog("api9_log", json.toString());

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

void sendSuperFacePB(Object data, String faceName) {
    try {
        Object pic = null;
        for (Object el : data.data.elements) {
            if (el.picElement != null) {
                pic = el.picElement;
                break;
            }
        }
        if (pic == null) return;

        String md5 = pic.md5HexStr;
        String fileName = pic.fileName;
        String uuid = pic.fileUuid;
        int width = pic.picWidth;
        int height = pic.picHeight;

        String rkey;
        if (data.type == 2) {
            rkey = getGroupRKey();
        } else {
            rkey = getFriendRKey();
        }
        if (rkey == null || rkey.isEmpty()) return;

        long peerUin = Long.parseLong(data.peerUin);
        long selfUin = Long.parseLong(myUin);
        String selfUid = getUidFromUin(myUin);
        if (selfUid == null) selfUid = "";

        JSONArray elements = new JSONArray();

        JSONObject elem37 = new JSONObject();
        elem37.put("1", 19);
        elem37.put("6", 2);
        elem37.put("7", "aQoAJ330Au8x79xu3tAI4Ntf0clrbrn66Fux3TbeiYZp6YAiHKgGu+VjtVuHsKjA");
        elem37.put("12", 1);
        elem37.put("16", 0);
        elem37.put("17", 161824);

        JSONObject f19 = new JSONObject();
        f19.put("1", 5);
        JSONObject f65 = new JSONObject();
        f65.put("1", 1);
        f65.put("2", 20);
        f19.put("65", f65);
        f19.put("66", 33554560);
        f19.put("34", 2000);
        f19.put("4", 10315);
        f19.put("71", 3);
        f19.put("72", 0);
        JSONObject f73 = new JSONObject();
        f73.put("1", 45);
        f73.put("2", 0);
        f73.put("3", 113);
        f73.put("6", 5);
        f73.put("7", 2);
        f19.put("73", f73);
        f19.put("41", 0);
        f19.put("107", 828);
        f19.put("79", 131136);
        f19.put("15", 161494);
        f19.put("80", 37);
        f19.put("81", 16);
        f19.put("51", 339);
        f19.put("116", -444136893304753334L);
        f19.put("52", 8);
        f19.put("54", 1);
        f19.put("55", 1);
        f19.put("56", 0);
        f19.put("25", 0);
        JSONObject f90 = new JSONObject();
        JSONArray f90Arr = new JSONArray();
        JSONObject f90Item = new JSONObject();
        f90Item.put("1", selfUin);
        f90Item.put("2", selfUid);
        f90Arr.put(f90Item);
        f90.put("3", f90Arr);
        f19.put("90", f90);
        f19.put("58", 0);
        f19.put("30", 0);
        f19.put("31", 0);
        elem37.put("19", f19);
        elements.put(elem37);

        JSONObject elem9 = new JSONObject();
        elem9.put("1", 2141485);
        elements.put(elem9);

        JSONObject elem53 = new JSONObject();
        elem53.put("1", 48);
        elem53.put("3", 20);
        JSONObject sub53 = new JSONObject();
        JSONObject sub53_1 = new JSONObject();
        JSONObject sub53_1_1 = new JSONObject();

        JSONObject imgAttr = new JSONObject();
        imgAttr.put("1", 1956563);
        imgAttr.put("2", md5);
        imgAttr.put("3", "11314e9ab6d9233bb5a78fc4e6b22a728f817b1c");
        imgAttr.put("4", fileName);
        JSONObject reso = new JSONObject();
        reso.put("1", 1);
        reso.put("2", 2000);
        imgAttr.put("5", reso);
        imgAttr.put("6", width);
        imgAttr.put("7", height);
        imgAttr.put("8", 0);
        imgAttr.put("9", 0);
        sub53_1_1.put("1", imgAttr);
        sub53_1_1.put("2", rkey);
        sub53_1_1.put("3", 1);
        sub53_1_1.put("4", System.currentTimeMillis() / 1000 + 86400);
        sub53_1_1.put("5", 2678400);
        sub53_1_1.put("6", 0);
        sub53_1.put("1", sub53_1_1);

        JSONObject urlInfo = new JSONObject();
        urlInfo.put("1", "/download?appid=1407&fileid=" + uuid);
        JSONObject urlSpec = new JSONObject();
        urlSpec.put("1", "&spec=0");
        urlSpec.put("2", "&spec=720");
        urlSpec.put("3", "&spec=198");
        urlInfo.put("2", urlSpec);
        urlInfo.put("3", "multimedia.nt.qq.com.cn");
        sub53_1.put("2", urlInfo);
        sub53_1.put("5", 0);
        JSONObject hexData = new JSONObject();
        hexData.put("2", "hex->E6417C37C58CD6208F835F631E931730EE1A596C");
        sub53_1.put("6", hexData);
        sub53.put("1", sub53_1);

        JSONObject sub53_2 = new JSONObject();
        JSONObject textElem = new JSONObject();
        textElem.put("1", 0);
        textElem.put("2", "[" + faceName + "]");
        textElem.put("1001", 2);
        textElem.put("1002", 2);
        textElem.put("1003", 3712448771L);
        JSONObject inner12 = new JSONObject();
        inner12.put("1", 1);
        inner12.put("34", 0);
        inner12.put("18", new JSONObject());
        inner12.put("19", new JSONObject());
        inner12.put("3", 0);
        inner12.put("4", 0);
        JSONObject inner21 = new JSONObject();
        inner21.put("1", 6740);
        inner21.put("2", faceName);
        inner21.put("3", 1);
        inner21.put("4", 100);
        inner21.put("5", 0);
        inner21.put("7", new JSONObject());
        inner12.put("21", inner21);
        inner12.put("9", "[" + faceName + "]");
        inner12.put("10", 0);
        inner12.put("12", new JSONObject());
        inner12.put("30", "&rkey=" + rkey);
        inner12.put("31", new JSONObject());
        textElem.put("12", inner12);
        sub53_2.put("1", textElem);
        sub53_2.put("2", new JSONObject().put("3", new JSONObject()));
        sub53_2.put("3", new JSONObject().put("11", new JSONObject()).put("12", new JSONObject()));
        sub53_2.put("10", 0);
        sub53.put("2", sub53_2);
        elem53.put("2", sub53);
        elements.put(elem53);

        JSONObject elem16 = new JSONObject();
        elem16.put("1", "x");
        elem16.put("3", 1);
        elem16.put("4", 8);
        elem16.put("7", new JSONObject());
        elements.put(elem16);

        JSONObject routing = new JSONObject();
        if (data.type == 2) {
            JSONObject grp = new JSONObject();
            grp.put("1", peerUin);
            routing.put("2", grp);
        } else {
            JSONObject c2c = new JSONObject();
            c2c.put("2", String.valueOf(peerUin));
            routing.put("1", c2c);
        }

        JSONObject field2 = new JSONObject();
        field2.put("1", 1);
        field2.put("2", 0);
        field2.put("3", 0);

        JSONObject body = new JSONObject();
        JSONObject inner1 = new JSONObject();
        inner1.put("2", elements);
        body.put("1", inner1);

        JSONObject pb = new JSONObject();
        pb.put("1", routing);
        pb.put("2", field2);
        pb.put("3", body);
        pb.put("4", (int) (Math.random() * 1000000));
        pb.put("5", (int) (Math.random() * 1000000));

        FunProtoData proto = new FunProtoData();
        proto.fromJSON(pb);
        PacketHelper.sendRequest("MessageSvc.PbSendMsg", proto.toBytes(), res -> {});
    } catch (Throwable e) { traceLog("api9_log", "[sendSuperFacePB] 异常: " + e); }
}
void showSuperFaceSendDialog(Object data) {
    Activity act = getNowActivity();
    String savedFaceName = getString("config", "super_face_display_name", "");
    act?.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(act);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int accentColor = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(act, 16), dp(act, 20), dp(act, 16), dp(act, 16));

                TextView title = new TextView(act);
                title.setText("图片转超级QQ秀");
                title.setTextSize(18);
                title.setTextColor(textColor);
                title.setPadding(0, 0, 0, dp(act, 12));
                root.addView(title);

                TextView hint = new TextView(act);
                hint.setText("外显");
                hint.setTextSize(13);
                hint.setTextColor(subTextColor);
                hint.setPadding(0, 0, 0, dp(act, 8));
                root.addView(hint);

                final EditText input = makeInput(act, "例如：嘿壳 若未输入则默认使用嘿壳", null);
                input.setText(savedFaceName);
                input.setSingleLine(false);
                input.setMinLines(2);
                input.setTextSize(14);
                root.addView(input);

                LinearLayout btnBox = new LinearLayout(act);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(act, 24), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = createButton(act, "取消", subTextColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                final TextView send = createButton(act, "发送", accentColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);

                btnBox.addView(cancel);
                btnBox.addView(send);
                root.addView(btnBox);

                send.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String faceName = input.getText().toString().trim();
                        if (faceName.isEmpty()) {
                        faceName = "嘿壳";
                        } else {
                        putString("config", "super_face_display_name", faceName);
                        }
                        ref[0]?.dismiss();
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                sendSuperFacePB(data, faceName);
                            }
                        });
                        qqToast(2, "正在发送超级QQ秀…");
                    }
                });

                AlertDialog.Builder builder = new AlertDialog.Builder(act,
                        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                final AlertDialog[] ref = new AlertDialog[1];
                ref[0] = builder?.create();
                ref[0]?.show();
                applyUiTheme(act, ref[0], 0);
            } catch (Throwable e) {
                qqToast(1, "弹窗显示失败");
            }
        }
    });
}