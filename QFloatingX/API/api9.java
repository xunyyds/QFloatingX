import com.tencent.mobileqq.app.QQAppInterface;
import com.tencent.qphone.base.remote.ToServiceMsg;
import com.tencent.mobileqq.service.MobileQQServiceBase;
import me.yxp.qfun.utils.hook.HookExtensionsKt;

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
    et.setBackground(makeRoundRect(colorBg, dp(ctx,6)));
    et.setPadding(dp(ctx,10), dp(ctx,8), dp(ctx,10), dp(ctx,8));
    et.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return et;
}

TextView makeSubTitleCompact(Activity ctx, String text, int color) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(color);
    tv.setPadding(dp(ctx,4), dp(ctx,16), 0, dp(ctx,6));
    return tv;
}

TextView makeActionBtn(Activity ctx, String text, int textColor, int bgColor) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(14);
    tv.setTextColor(textColor);
    tv.setGravity(Gravity.CENTER);
    tv.setBackground(makeRoundRect(bgColor, dp(ctx,8)));
    return tv;
}

TextView makeChipToggle(Activity ctx, String text, int colorMain, int colorLight, boolean selected) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setPadding(dp(ctx,12), dp(ctx,6), dp(ctx,12), dp(ctx,6));
    setChipSelected(tv, selected, colorMain, colorLight);
    return tv;
}

void setChipSelected(TextView tv, boolean selected, int colorMain, int colorLight) {
    tv.setTextColor(selected ? Color.WHITE : colorMain);
    tv.setBackground(makeRoundRect(selected ? colorMain : colorLight, dp(ctx,50)));
}

/**
 * Protobuf编码器
 * <p>
 * 支持从JSON对象解析并编码为Protobuf二进制格式。
 * 支持嵌套对象、数组、字符串、整数和长整数类型。
 * </p>
 */
public class ProtoData {
    private final Map<Integer, List<Object>> values = new HashMap<>();
    
    /**
     * 从JSON对象解析数据
     *
     * @param jsonObject JSON对象
     */
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
    
    /**
     * 解析单个字段
     *
     * @param key   字段键名
     * @param value 字段值
     */
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
        }
    }
    
    /**
     * 添加字段值
     *
     * @param fieldNumber 字段编号
     * @param value       字段值
     */
    public void putValue(int fieldNumber, Object value) {
        values.computeIfAbsent(fieldNumber, k -> new ArrayList<>()).add(value);
    }
    
    /**
     * 编码为Protobuf字节数组
     *
     * @return Protobuf编码后的字节数组
     */
    public byte[] toBytes() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            return encodeToProtobuf(baos);
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    /**
     * 编码到Protobuf输出流
     *
     * @param baos 字节数组输出流
     * @return 编码后的字节数组
     */
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
    
    /**
     * 编码单个字段
     *
     * @param baos        输出流
     * @param fieldNumber 字段编号
     * @param value       字段值
     */
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
    
    /**
     * 写入Varint编码
     *
     * @param baos  输出流
     * @param value 数值
     */
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

/**
 * PB消息发送器
 * <p>
 * 用于构建和发送Protobuf格式的消息到QQ服务。
 * </p>
 */
public class PBSender {
    private final QQAppInterface qqAppInterface;
    private final String currentUin;
    
    /**
     * 构造函数
     *
     * @param qqAppInterface QQ应用接口
     * @param currentUin     当前用户Uin
     */
    public PBSender(QQAppInterface qqAppInterface, String currentUin) {
        this.qqAppInterface = Objects.requireNonNull(qqAppInterface, "QQAppInterface不能为null");
        this.currentUin = Objects.requireNonNull(currentUin, "当前Uin不能为null");
    }
    
    /**
     * 发送PB消息
     *
     * @param pbData PB数据字符串，格式为 发pb服务名{JSON数据}
     * @return 发送成功返回序列号，失败返回-1
     */
    public int sendPB(String pbData) {
        return Optional.ofNullable(pbData)
            .filter(data -> data.startsWith("发pb"))
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
                    return -1;
                }
            })
            .orElseGet(() -> -1);
    }
    
    /**
     * 从JSON构建字节数组
     *
     * @param json JSON对象
     * @return Protobuf编码后的字节数组
     */
    private byte[] makeBytes(JSONObject json) {
        ProtoData p = new ProtoData(); 
        p.fromJSON(json); 
        return p.toBytes(); 
    }
    
    /**
     * 发送字节数据到服务
     *
     * @param bytes   字节数组
     * @param service 服务名
     * @return 发送成功返回序列号，失败返回-1
     */
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
            return -1;
        }
    }
}

/**
 * 显示PB发包工具弹窗
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
        card.setBackground(createRoundRectDrawable(Color.parseColor("#FFFFFF"), dp(act, 16)));
        card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
        scroll.addView(card);
        
        TextView title = new TextView(act);
        title.setText("PB发包工具");
        title.setTextSize(17);
        title.setTextColor(Color.parseColor("#222222"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(act, 20));
        card.addView(title);
        
        card.addView(makeSubTitleCompact(act, "服务名", Color.parseColor("#666666")));
        EditText etService = makeInputCompact(act, "", "例如: trpc.qq_lbs.qq_lbs_ark.LocationArk.SsoSendMessage", Color.parseColor("#F7F8FA"));
        card.addView(etService);
        
        card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", Color.parseColor("#666666")));
        EditText etPB = makeInputCompact(act, "", "例如: {\"1\":123,\"2\":1}", Color.parseColor("#F7F8FA"));
        etPB.setMinLines(4);
        card.addView(etPB);
        
        card.addView(makeSubTitleCompact(act, "模版管理", Color.parseColor("#666666")));
        
        LinearLayout templateContainer = new LinearLayout(act);
        templateContainer.setOrientation(LinearLayout.HORIZONTAL);
        templateContainer.setGravity(Gravity.CENTER);
        templateContainer.setPadding(0, 0, 0, dp(act, 12));
        card.addView(templateContainer);
        
        TextView btnSave = makeActionBtn(act, "保存模版", Color.WHITE, Color.parseColor("#3B71FE"));
        TextView btnLoad = makeActionBtn(act, "使用模版", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
        TextView btnPreview = makeActionBtn(act, "预览", Color.parseColor("#3B71FE"), Color.parseColor("#E8EEFF"));
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, -2, 1);
        btnParams.setMargins(0, 0, dp(act, 8), 0);
        btnSave.setLayoutParams(btnParams);
        btnLoad.setLayoutParams(btnParams);
        btnPreview.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        
        templateContainer.addView(btnSave);
        templateContainer.addView(btnLoad);
        templateContainer.addView(btnPreview);
        
        EditText etTemplateName = makeInputCompact(act, "", "模版名称", Color.parseColor("#F7F8FA"));
        etTemplateName.setVisibility(View.GONE);
        card.addView(etTemplateName);
        
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
                String pbData = etPB.getText().toString().trim();
                
                if (service.isEmpty() || pbData.isEmpty()) {
                    qqToast(1, "请填写完整信息");
                    return;
                }
                
                String templateKey = "pb_template_" + name;
                putString("templates", templateKey, service + "|||" + pbData);
                
                etTemplateName.setVisibility(View.GONE);
                btnSave.setText("保存模版");
                qqToast(2, "模版已保存");
            }
        });
        
        btnLoad.setOnClickListener(v -> {
            showTemplateSelectorDialog(act, etService, etPB, dialog);
        });
        
        btnPreview.setOnClickListener(v -> {
            String service = etService.getText().toString().trim();
            String pbData = etPB.getText().toString().trim();
            
            if (service.isEmpty() || pbData.isEmpty()) {
                qqToast(1, "请填写完整信息");
                return;
            }
            
            showPreviewDialog(act, service, pbData);
        });
        
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
        
        btnConfirm.setOnClickListener(v -> {
            String service = etService.getText().toString().trim();
            String pbData = etPB.getText().toString().trim();
            
            if (service.isEmpty() || pbData.isEmpty()) {
                qqToast(1, "请填写完整信息");
                return;
            }
            
            dialog.dismiss();
            
            ThreadPool.execute(() -> {
                try {
                    if (app == null) {
                        act.runOnUiThread(() -> qqToast(1, "获取app失败"));
                        return;
                    }
                    
                    PBSender sender = new PBSender(app, myUin);
                    String fullPB = "发pb" + service + pbData;
                    int seq = sender.sendPB(fullPB);
                    
                    final int finalSeq = seq;
                    act.runOnUiThread(() -> {
                        if (finalSeq > 0) {
                            qqToast(2, "发送成功，seq=" + finalSeq);
                        } else {
                            qqToast(1, "发送失败");
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
 * 显示模版选择弹窗
 *
 * @param act          Activity上下文
 * @param etService    服务名输入框
 * @param etPB         PB数据输入框
 * @param parentDialog 父对话框
 */
void showTemplateSelectorDialog(Activity act, EditText etService, EditText etPB, Dialog parentDialog) {
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
    card.setBackground(createRoundRectDrawable(Color.parseColor("#FFFFFF"), dp(act, 16)));
    card.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));
    scroll.addView(card);
    
    TextView title = new TextView(act);
    title.setText("选择模版");
    title.setTextSize(17);
    title.setTextColor(Color.parseColor("#222222"));
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, 0, 0, dp(act, 20));
    card.addView(title);
    
    Map<String, String> templates = new HashMap<>();
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
        Iterator<String> keys = config.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith("pb_template_")) {
                String value = config.getString(key);
                String displayName = key.substring(12);
                templates.put(displayName, value);
            }
        }
    } catch (Exception e) {
    }
    
    if (templates.isEmpty()) {
        TextView empty = new TextView(act);
        empty.setText("暂无保存的模版");
        empty.setTextSize(14);
        empty.setTextColor(Color.parseColor("#BBBBBB"));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dp(act, 40), 0, dp(act, 40));
        card.addView(empty);
    } else {
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            final String name = entry.getKey();
            final String namedata = entry.getValue();
            
            TextView templateItem = makeActionBtn(act, name, Color.parseColor("#222222"), Color.parseColor("#F7F8FA"));
            templateItem.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            templateItem.setPadding(dp(act, 16), dp(act, 12), dp(act, 16), dp(act, 12));
            card.addView(templateItem);
            
            templateItem.setOnClickListener(v -> {
                String[] parts = namedata.split("\\|\\|\\|");
                if (parts.length >= 2) {
                    etService.setText(parts[0]);
                    etPB.setText(parts[1]);
                    dialog.dismiss();
                    qqToast(2, "已加载模版: " + name);
                }
            });
        }
    }
    
    TextView btnClose = makeActionBtn(act, "关闭", Color.parseColor("#666666"), Color.parseColor("#F7F8FA"));
    btnClose.setPadding(0, dp(act, 20), 0, 0);
    card.addView(btnClose);
    
    btnClose.setOnClickListener(v -> {
        dialog.dismiss();
    });
    
    dialog.setContentView(outer);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
    }
    dialog.show();
}

/**
 * 显示PB数据预览弹窗
 *
 * @param act     Activity上下文
 * @param service 服务名
 * @param pbData  PB数据
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
    card.setBackground(createRoundRectDrawable(Color.parseColor("#FFFFFF"), dp(act, 16)));
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
    tvService.setText(service);
    tvService.setTextSize(13);
    tvService.setTextColor(Color.parseColor("#222222"));
    tvService.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
    tvService.setBackground(createRoundRectDrawable(Color.parseColor("#F7F8FA"), dp(act, 6)));
    card.addView(tvService);
    
    card.addView(makeSubTitleCompact(act, "PB数据 (JSON)", Color.parseColor("#666666")));
    TextView tvPB = new TextView(act);
    tvPB.setText(pbData);
    tvPB.setTextSize(13);
    tvPB.setTextColor(Color.parseColor("#222222"));
    tvPB.setPadding(dp(act, 12), dp(act, 12), dp(act, 12), dp(act, 12));
    tvPB.setBackground(createRoundRectDrawable(Color.parseColor("#F7F8FA"), dp(act, 6)));
    tvPB.setMinLines(6);
    card.addView(tvPB);
    
    card.addView(makeSubTitleCompact(act, "完整数据", Color.parseColor("#666666")));
    TextView tvFull = new TextView(act);
    tvFull.setText("发pb" + service + pbData);
    tvFull.setTextSize(11);
    tvFull.setTextColor(Color.parseColor("#666666"));
    tvFull.setPadding(dp(act, 12), dp(act, 8), dp(act, 12), dp(act, 8));
    tvFull.setBackground(createRoundRectDrawable(Color.parseColor("#F7F8FA"), dp(act, 6)));
    card.addView(tvFull);
    
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