import com.tencent.mobileqq.app.QQAppInterface;
import com.tencent.qphone.base.remote.ToServiceMsg;
import com.tencent.mobileqq.service.MobileQQServiceBase;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.*;
import org.json.JSONObject;
import org.json.JSONArray;
import com.tencent.mobileqq.app.BaseApplicationImpl;
import java.util.concurrent.ThreadLocalRandom;
import me.yxp.qfun.utils.hook.HookExtensionsKt;

// ==================== UI组件工厂 ====================

GradientDrawable makeRoundRect(int color, int radiusPx) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radiusPx);
    return drawable;
}

EditText makeInputCompact(Activity ctx, String val, String hint, int colorBg) {
    EditText et = new EditText(ctx);
    et.setText(val);
    et.setHint(hint);
    et.setTextSize(13);
    et.setTextColor(Color.parseColor("#222222"));
    et.setHintTextColor(Color.parseColor("#BBBBBB"));
    et.setBackground(makeRoundRect(colorBg, dpToPx(6)));
    et.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
    et.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return et;
}

TextView makeSubTitleCompact(Activity ctx, String text, int color) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(color);
    tv.setPadding(dpToPx(4), dpToPx(16), 0, dpToPx(6));
    return tv;
}

TextView makeActionBtn(Activity ctx, String text, int textColor, int bgColor) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(14);
    tv.setTextColor(textColor);
    tv.setGravity(Gravity.CENTER);
    tv.setBackground(makeRoundRect(bgColor, dpToPx(8)));
    return tv;
}

TextView makeChipToggle(Activity ctx, String text, int colorMain, int colorLight, boolean selected) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
    setChipSelected(tv, selected, colorMain, colorLight);
    return tv;
}

void setChipSelected(TextView tv, boolean selected, int colorMain, int colorLight) {
    tv.setTextColor(selected ? Color.WHITE : colorMain);
    tv.setBackground(makeRoundRect(selected ? colorMain : colorLight, dpToPx(50)));
}

// ==================== QFUN 工具类 ====================

int dpToPx(int dp) {
    Activity act = getNowActivity();
    return act != null ? (int)(dp * act.getResources().getDisplayMetrics().density) : dp * 3;
}

GradientDrawable createCardDrawable() {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(Color.parseColor("#FFFFFF"));
    gd.setCornerRadius(dpToPx(16));
    return gd;
}

GradientDrawable createSearchDrawable() {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(Color.parseColor("#F7F8FA"));
    gd.setCornerRadius(dpToPx(6));
    return gd;
}

// ==================== 现代化Protobuf编码器 ====================

public class ProtoData {
    private final Map<Integer, List<Object>> values = new HashMap<>();
    
    public void fromJSON(JSONObject jsonObject) {
        Optional.ofNullable(jsonObject)
            .map(json -> {
                try { return json.keys(); } catch (Exception e) { return null; }
            })
            .ifPresent(keys -> {
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    parseField(key, jsonObject.opt(key));
                }
            });
    }
    
    private void parseField(String key, Object value) {
        try {
            int fieldNumber = Integer.parseInt(key);
            Optional.ofNullable(value).ifPresent(v -> {
                if (v instanceof JSONObject) {
                    ProtoData nested = new ProtoData();
                    nested.fromJSON((JSONObject) v);
                    putValue(fieldNumber, nested);
                } else if (v instanceof JSONArray) {
                    Arrays.stream(((JSONArray) v).toArray())
                        .forEach(item -> {
                            if (item instanceof JSONObject) {
                                ProtoData nested = new ProtoData();
                                nested.fromJSON((JSONObject) item);
                                putValue(fieldNumber, nested);
                            } else {
                                putValue(fieldNumber, item);
                            }
                        });
                } else {
                    putValue(fieldNumber, v);
                }
            });
        } catch (Exception e) {
            log("pb_sender.log", "字段解析失败: " + e.getMessage());
        }
    }
    
    public void putValue(int fieldNumber, Object value) {
        values.computeIfAbsent(fieldNumber, k -> new ArrayList<>()).add(value);
    }
    
    public byte[] toBytes() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            return encodeToProtobuf(baos);
        } catch (Exception e) {
            log("pb_sender.log", "Protobuf编码失败: " + e.getMessage());
            return new byte[0];
        }
    }
    
    private byte[] encodeToProtobuf(ByteArrayOutputStream baos) throws IOException {
        values.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(value -> new Object[]{entry.getKey(), value}))
            .forEach(pair -> {
                try {
                    Object[] p = (Object[]) pair;
                    encodeField(baos, (int) p[0], p[1]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        return baos.toByteArray();
    }
    
    private void encodeField(ByteArrayOutputStream baos, int fieldNumber, Object value) throws IOException {
        Optional.ofNullable(value).ifPresent(v -> {
            try {
                if (v instanceof ProtoData) {
                    byte[] nestedBytes = ((ProtoData) v).toBytes();
                    writeVarint(baos, (fieldNumber << 3) | 2);
                    writeVarint(baos, nestedBytes.length);
                    baos.write(nestedBytes);
                } else if (v instanceof Long) {
                    writeVarint(baos, (fieldNumber << 3) | 0);
                    writeVarint(baos, (Long) v);
                } else if (v instanceof Integer) {
                    writeVarint(baos, (fieldNumber << 3) | 0);
                    writeVarint(baos, ((Integer) v).longValue());
                } else if (v instanceof String) {
                    byte[] strBytes = ((String) v).getBytes();
                    writeVarint(baos, (fieldNumber << 3) | 2);
                    writeVarint(baos, strBytes.length);
                    baos.write(strBytes);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private void writeVarint(ByteArrayOutputStream baos, long value) throws IOException {
        BiConsumer<ByteArrayOutputStream, Long> writeByte = (stream, b) -> {
            try { stream.write(b.intValue()); } catch (IOException e) { throw new RuntimeException(e); }
        };
        
        while ((value & 0x7FL) != 0) {
            writeByte.accept(baos, (value & 0x7F) | 0x80);
            value >>>= 7;
        }
        writeByte.accept(baos, value);
    }
}

// ==================== 接口层 ====================

public class PBSender {
    private final QQAppInterface qqAppInterface;
    private final String currentUin;
    
    public PBSender(QQAppInterface qqAppInterface, String currentUin) {
        this.qqAppInterface = Objects.requireNonNull(qqAppInterface, "QQAppInterface不能为null");
        this.currentUin = Objects.requireNonNull(currentUin, "当前Uin不能为null");
    }
    
    public int sendPB(String pbData) {
        return Optional.ofNullable(pbData)
            .filter(data -> data.startsWith("pb#"))
            .map(data -> {
                int jsonStart = data.indexOf("{");
                return jsonStart > 0 ? new String[]{data.substring(3, jsonStart), data.substring(jsonStart)} : null;
            })
            .map(parts -> {
                try {
                    JSONObject json = new JSONObject(parts[1]);
                    byte[] bytes = makeBytes(json);
                    return bytes.length > 0 ? sendBuffer(bytes, parts[0]) : -1;
                } catch (Exception e) {
                    log("pb_sender.log", "PB发送失败: " + e.getMessage());
                    return -1;
                }
            })
            .orElseGet(() -> {
                log("pb_sender.log", "PB数据格式错误");
                return -1;
            });
    }
    
    private byte[] makeBytes(JSONObject json) {
        ProtoData p = new ProtoData(); 
        p.fromJSON(json); 
        return p.toBytes(); 
    }
    
    private int sendBuffer(byte[] bytes, String service) {
        if (bytes == null || bytes.length == 0) return -1;
        
        try {
            ToServiceMsg toServiceMsg = new ToServiceMsg("com.tencent.mobileqq.msf.service.MsfService", currentUin, service);
            
            int seq = ThreadLocalRandom.current().nextInt(60000, 70001);
            
            java.lang.reflect.Method setSeqMethod = toServiceMsg.getClass().getDeclaredMethod("setRequestSsoSeq", int.class);
            setSeqMethod.setAccessible(true);
            setSeqMethod.invoke(toServiceMsg, seq);
            
            java.lang.reflect.Method putBufferMethod = toServiceMsg.getClass().getDeclaredMethod("putWupBuffer", byte[].class);
            putBufferMethod.setAccessible(true);
            putBufferMethod.invoke(toServiceMsg, new byte[][]{bytes});
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("appTimeoutReq", "371");
            attributes.put("req_pb_protocol_flag", true);
            attributes.put("to_SenderProcessName", "com.tencent.mobileqq");
            attributes.put("to_SendTime", System.currentTimeMillis());
            attributes.put("fastresend", false);
            attributes.put("binder_start_send_time", SystemClock.elapsedRealtime());
            
            java.lang.reflect.Method addAttrMethod = toServiceMsg.getClass().getDeclaredMethod("addAttribute", String.class, Object.class);
            addAttrMethod.setAccessible(true);
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                addAttrMethod.invoke(toServiceMsg, entry.getKey(), entry.getValue());
            }
            
            MobileQQServiceBase mobileQQService = qqAppInterface.getMobileQQService();
            mobileQQService.handleRequest(toServiceMsg);
            
            return seq;
        } catch (Exception e) {
            log("pb_sender.log", "发送失败: " + e.getMessage());
            return -1;
        }
    }
}

// ==================== 发包工具弹窗UI ====================

void showPBSenderDialog(int ft, String gid, String uname) {
    Activity act = getNowActivity();
    if (act == null) return;
    
    act.runOnUiThread(() -> {
        Dialog dialog = new Dialog(act);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        FrameLayout outer = new FrameLayout(act);
        int m = dpToPx(24);
        outer.setPadding(m, m, m, m);
        
        ScrollView scroll = new ScrollView(act);
        outer.addView(scroll);
        
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeRoundRect(Color.parseColor("#FFFFFF"), dpToPx(16)));
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        scroll.addView(card);
        
        TextView title = new TextView(act);
        title.setText("PB发包工具");
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#222222"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(20));
        card.addView(title);
        
        LinearLayout modeContainer = new LinearLayout(act);
        modeContainer.setOrientation(LinearLayout.HORIZONTAL);
        modeContainer.setGravity(Gravity.CENTER);
        modeContainer.setPadding(0, 0, 0, dpToPx(16));
        card.addView(modeContainer);
        
        final int[] currentMode = {0};
        
        TextView chipPut = makeChipToggle(act, "Put版", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"), true);
        TextView chipDual = makeChipToggle(act, "双输入框", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"), false);
        
        modeContainer.addView(chipPut);
        modeContainer.addView(chipDual);
        
        card.addView(makeSubTitleCompact(act, "服务名", Color.parseColor("#666666")));
        EditText etService = makeInputCompact(act, "", "例如: trpc.qq_lbs.qq_lbs_ark.LocationArk.SsoSendMessage", Color.parseColor("#F7F8FA"));
        card.addView(etService);
        
        card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", Color.parseColor("#666666")));
        EditText etPB = makeInputCompact(act, "", "例如: {\"1\":123,\"2\":1}", Color.parseColor("#F7F8FA"));
        etPB.setMinLines(4);
        card.addView(etPB);
        
        LinearLayout dualContainer = new LinearLayout(act);
        dualContainer.setOrientation(LinearLayout.VERTICAL);
        dualContainer.setPadding(0, dpToPx(12), 0, 0);
        card.addView(dualContainer);
        
        dualContainer.addView(makeSubTitleCompact(act, "字段1", Color.parseColor("#666666")));
        EditText etField1 = makeInputCompact(act, "", "字段1内容", Color.parseColor("#F7F8FA"));
        dualContainer.addView(etField1);
        
        dualContainer.addView(makeSubTitleCompact(act, "字段2", Color.parseColor("#666666")));
        EditText etField2 = makeInputCompact(act, "", "字段2内容", Color.parseColor("#F7F8FA"));
        dualContainer.addView(etField2);
        
        dualContainer.setVisibility(View.GONE);
        
        card.addView(makeSubTitleCompact(act, "模版管理", Color.parseColor("#666666")));
        
        LinearLayout templateContainer = new LinearLayout(act);
        templateContainer.setOrientation(LinearLayout.HORIZONTAL);
        templateContainer.setGravity(Gravity.CENTER);
        templateContainer.setPadding(0, 0, 0, dpToPx(12));
        card.addView(templateContainer);
        
        TextView btnSave = makeActionBtn(act, "保存模版", Color.WHITE, Color.parseColor("#3B71FE"));
        TextView btnLoad = makeActionBtn(act, "使用模版", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
        TextView btnPreview = makeActionBtn(act, "预览", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, -2, 1);
        btnParams.setMargins(0, 0, dpToPx(8), 0);
        btnSave.setLayoutParams(btnParams);
        btnLoad.setLayoutParams(btnParams);
        btnPreview.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        
        templateContainer.addView(btnSave);
        templateContainer.addView(btnLoad);
        templateContainer.addView(btnPreview);
        
        EditText etTemplateName = makeInputCompact(act, "", "模版名称", Color.parseColor("#F7F8FA"));
        etTemplateName.setVisibility(View.GONE);
        card.addView(etTemplateName);
        
        chipPut.setOnClickListener(v -> {
            currentMode[0] = 0;
            setChipSelected(chipPut, true, Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
            setChipSelected(chipDual, false, Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
            dualContainer.setVisibility(View.GONE);
            etPB.setVisibility(View.VISIBLE);
        });
        
        chipDual.setOnClickListener(v -> {
            currentMode[0] = 1;
            setChipSelected(chipDual, true, Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
            setChipSelected(chipPut, false, Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
            dualContainer.setVisibility(View.VISIBLE);
            etPB.setVisibility(View.GONE);
        });
        
        btnSave.setOnClickListener(v -> {
            if (etTemplateName.getVisibility() == View.GONE) {
                etTemplateName.setVisibility(View.VISIBLE);
                btnSave.setText("确认保存");
            } else {
                String name = etTemplateName.getText().toString().trim();
                if (name.isEmpty()) {
                    qqToast(1, "请输入模版名称");
                    return;
                }
                
                String service = etService.getText().toString().trim();
                String pbData = currentMode[0] == 0 ? etPB.getText().toString().trim() : 
                    "{\"1\":\"" + etField1.getText().toString().trim() + "\",\"2\":\"" + etField2.getText().toString().trim() + "\"}";
                
                if (service.isEmpty() || pbData.isEmpty()) {
                    qqToast(1, "请填写完整信息");
                    return;
                }
                
                String templateKey = "pb_template_" + name;
                putString("templates", templateKey, service + "|||" + pbData + "|||" + currentMode[0]);
                
                etTemplateName.setVisibility(View.GONE);
                btnSave.setText("保存模版");
                qqToast(2, "模版已保存");
                log("pb_sender.log", "保存模版: " + name);
            }
        });
        
        btnLoad.setOnClickListener(v -> {
            showTemplateSelectorDialog(act, etService, etPB, etField1, etField2, dualContainer, currentMode, dialog);
        });
        
        btnPreview.setOnClickListener(v -> {
            String service = etService.getText().toString().trim();
            String pbData = currentMode[0] == 0 ? etPB.getText().toString().trim() : 
                "{\"1\":\"" + etField1.getText().toString().trim() + "\",\"2\":\"" + etField2.getText().toString().trim() + "\"}";
            
            if (service.isEmpty() || pbData.isEmpty()) {
                qqToast(1, "请填写完整信息");
                return;
            }
            
            showPreviewDialog(act, service, pbData);
        });
        
        LinearLayout bottomContainer = new LinearLayout(act);
        bottomContainer.setOrientation(LinearLayout.HORIZONTAL);
        bottomContainer.setGravity(Gravity.CENTER);
        bottomContainer.setPadding(0, dpToPx(20), 0, 0);
        card.addView(bottomContainer);
        
        TextView btnCancel = makeActionBtn(act, "取消", Color.parseColor("#666666"), Color.parseColor("#F7F8FA"));
        TextView btnConfirm = makeActionBtn(act, "发送", Color.WHITE, Color.parseColor("#3B71FE"));
        
        LinearLayout.LayoutParams bottomBtnParams = new LinearLayout.LayoutParams(0, dpToPx(44), 1);
        bottomBtnParams.setMargins(0, 0, dpToPx(12), 0);
        btnCancel.setLayoutParams(bottomBtnParams);
        btnConfirm.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(44), 1));
        
        bottomContainer.addView(btnCancel);
        bottomContainer.addView(btnConfirm);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            String service = etService.getText().toString().trim();
            String pbData = currentMode[0] == 0 ? etPB.getText().toString().trim() : 
                "{\"1\":\"" + etField1.getText().toString().trim() + "\",\"2\":\"" + etField2.getText().toString().trim() + "\"}";
            
            if (service.isEmpty() || pbData.isEmpty()) {
                qqToast(1, "请填写完整信息");
                return;
            }
            
            dialog.dismiss();
            
            new Thread(() -> {
                try {
                    if (app == null) {
                        act.runOnUiThread(() -> qqToast(1, "获取app失败"));
                        return;
                    }
                    
                    PBSender sender = new PBSender(app, myUin);
                    String fullPB = "pb#" + service + pbData;
                    int seq = sender.sendPB(fullPB);
                    
                    final int finalSeq = seq;
                    act.runOnUiThread(() -> {
                        if (finalSeq > 0) {
                            qqToast(2, "发送成功，seq=" + finalSeq);
                            log("pb_sender.log", "发送成功: " + service + " seq=" + finalSeq);
                        } else {
                            qqToast(1, "发送失败");
                            log("pb_sender.log", "发送失败: " + service);
                        }
                    });
                } catch (Exception e) {
                    act.runOnUiThread(() -> qqToast(1, "发送异常: " + e.getMessage()));
                    log("pb_sender.log", "发送异常: " + e.toString());
                }
            }).start();
        });
        
        dialog.setContentView(outer);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
        }
        dialog.show();
    });
}

void showTemplateSelectorDialog(Activity act, EditText etService, EditText etPB, EditText etField1, EditText etField2, LinearLayout dualContainer, int[] currentMode, Dialog parentDialog) {
    Dialog dialog = new Dialog(act);   // 独立的弹窗对象
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    
    FrameLayout outer = new FrameLayout(act);
    int m = dpToPx(24);
    outer.setPadding(m, m, m, m);
    
    ScrollView scroll = new ScrollView(act);
    outer.addView(scroll);
    
    LinearLayout card = new LinearLayout(act);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(makeRoundRect(Color.parseColor("#FFFFFF"), dpToPx(16)));
    card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
    scroll.addView(card);
    
    TextView title = new TextView(act);
    title.setText("选择模版");
    title.setTextSize(17);
    title.setTextColor(Color.parseColor("#222222"));
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 0, 0, dpToPx(20));
    card.addView(title);
    
    Map templates = new HashMap();
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
                templates.put(key.substring(11), value);
            }
        }
    } catch (Exception e) {
        log("pb_sender.log", "读取模版失败: " + e.getMessage());
    }
    
    if (templates.isEmpty()) {
        TextView empty = new TextView(act);
        empty.setText("暂无保存的模版");
        empty.setTextSize(14);
        empty.setTextColor(Color.parseColor("#BBBBBB"));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dpToPx(40), 0, dpToPx(40));
        card.addView(empty);
    } else {
        Iterator iter = templates.keySet().iterator();
        while (iter.hasNext()) {
            final String name = (String) iter.next();
            final String namedata = (String) templates.get(name);
            
            TextView templateItem = makeActionBtn(act, name, Color.parseColor("#222222"), Color.parseColor("#F7F8FA"));
            templateItem.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            templateItem.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
            card.addView(templateItem);
            
            templateItem.setOnClickListener(v -> {
                String[] parts = namedata.split("\\|\\|\\|");
                if (parts.length >= 3) {
                    etService.setText(parts[0]);
                    int mode = Integer.parseInt(parts[2]);
                    
                    if (mode == 0) {
                        etPB.setText(parts[1]);
                    } else {
                        try {
                            JSONObject json = new JSONObject(parts[1]);
                            etField1.setText(json.optString("1", ""));
                            etField2.setText(json.optString("2", ""));
                        } catch (Exception e) {
                            log("pb_sender.log", "解析模版失败: " + e.getMessage());
                        }
                    }
                    
                    currentMode[0] = mode;
                    if (mode == 1) {
                        dualContainer.setVisibility(View.VISIBLE);
                        etPB.setVisibility(View.GONE);
                    } else {
                        dualContainer.setVisibility(View.GONE);
                        etPB.setVisibility(View.VISIBLE);
                    }
                    
                    dialog.dismiss();
                    qqToast(2, "已加载模版: " + name);
                    log("pb_sender.log", "加载模版: " + name);
                }
            });
        }
    }
    
    TextView btnClose = makeActionBtn(act, "关闭", Color.parseColor("#666666"), Color.parseColor("#F7F8FA"));
    btnClose.setPadding(0, dpToPx(20), 0, 0);
    card.addView(btnClose);
    
    // 关键修复：明确使用当前 dialog（而非父弹窗）
    btnClose.setOnClickListener(v -> {
        dialog.dismiss();   // 只关闭模版弹窗
    });
    
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
    int m = dpToPx(24);
    outer.setPadding(m, m, m, m);
    
    ScrollView scroll = new ScrollView(act);
    outer.addView(scroll);
    
    LinearLayout card = new LinearLayout(act);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(makeRoundRect(Color.parseColor("#FFFFFF"), dpToPx(16)));
    card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
    scroll.addView(card);
    
    TextView title = new TextView(act);
    title.setText("PB数据预览");
    title.setTextSize(17);
    title.setTextColor(Color.parseColor("#222222"));
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 0, 0, dpToPx(20));
    card.addView(title);
    
    card.addView(makeSubTitleCompact(act, "服务名", Color.parseColor("#666666")));
    TextView tvService = new TextView(act);
    tvService.setText(service);
    tvService.setTextSize(13);
    tvService.setTextColor(Color.parseColor("#222222"));
    tvService.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
    tvService.setBackground(makeRoundRect(Color.parseColor("#F7F8FA"), dpToPx(6)));
    card.addView(tvService);
    
    card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", Color.parseColor("#666666")));
    TextView tvPB = new TextView(act);
    tvPB.setText(pbData);
    tvPB.setTextSize(13);
    tvPB.setTextColor(Color.parseColor("#222222"));
    tvPB.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
    tvPB.setBackground(makeRoundRect(Color.parseColor("#F7F8FA"), dpToPx(6)));
    tvPB.setMinLines(6);
    card.addView(tvPB);
    
    card.addView(makeSubTitleCompact(act, "完整数据", Color.parseColor("#666666")));
    TextView tvFull = new TextView(act);
    tvFull.setText("pb#" + service + pbData);
    tvFull.setTextSize(11);
    tvFull.setTextColor(Color.parseColor("#666666"));
    tvFull.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
    tvFull.setBackground(makeRoundRect(Color.parseColor("#F7F8FA"), dpToPx(6)));
    card.addView(tvFull);
    
    TextView btnClose = makeActionBtn(act, "关闭", Color.parseColor("#666666"), Color.parseColor("#F7F8FA"));
    btnClose.setPadding(0, dpToPx(20), 0, 0);
    card.addView(btnClose);
    
    btnClose.setOnClickListener(v -> dialog.dismiss());
    
    dialog.setContentView(outer);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
    }
    dialog.show();
}

void showPBSenderMenu(int chatType, String peerUin, String name) {
    showPBSenderDialog(chatType, peerUin, name);
}
addItem("PB发包工具", "showPBSenderMenu");
