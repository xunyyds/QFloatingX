import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import android.os.Handler;
import android.os.Looper;
import android.widget.AbsListView;

String convertRole(String role) {
    if ("OWNER".equals(role)) return "群主";
    if ("ADMIN".equals(role)) return "管理员";
    if ("MEMBER".equals(role)) return "普通成员";
    return role != null ? role : "未知";
}

String timestampToDate(long timestamp) {
    try {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    } catch (Throwable e) {
        return "时间错误";
    }
}

String getGagStatus(String timestamp) {
    return getGagStatus(timestamp, System.currentTimeMillis() / 1000);
}

String getGagStatus(String timestamp, long currentTime) {
    try {
        long ts = Long.parseLong(timestamp);
        if (ts == 0) return "未禁言";
        if (ts > 4102358400L) return "禁言中：永久";
        if (ts > currentTime) return "禁言中：剩余" + formatRemainingTime(ts - currentTime);
        return "未禁言";
    } catch (Throwable e) {
        return "未知";
    }
}

public static String prettyPrint(Object obj) {
    if (obj == null) return "null";
    String input = obj.toString();
    StringBuilder output = new StringBuilder();
    int indentLevel = 0;
    final String indent = "  ";
    boolean inQuotes = false;
    for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        if (c == '"' && (i == 0 || input.charAt(i - 1) != '\\')) {
            inQuotes = !inQuotes;
            output.append(c);
            continue;
        }
        if (inQuotes) {
            output.append(c);
            continue;
        }
        switch (c) {
            case '{':
            case '[':
            output.append(c);
            output.append('\n');
            indentLevel++;
            appendIndent(output, indent, indentLevel);
            break;
            case '}':
            case ']':
            output.append('\n');
            indentLevel--;
            appendIndent(output, indent, indentLevel);
            output.append(c);
            break;
            case ',':
            output.append(c);
            output.append('\n');
            appendIndent(output, indent, indentLevel);
            break;
            default:
            output.append(c);
            break;
        }
    }
    return output.toString();
}

private static void appendIndent(StringBuilder sb, String indent, int level) {
    for (int i = 0; i < level; i++) {
        sb.append(indent);
    }
}

void 菜单(Object data) {
    if (!非UI初始化完成) {
        traceLog("dialog_log", "[菜单] 菜单调用时延迟启动未完成");
        return;
    }
    Activity activity = safeActivity();
    if (activity == null) {
        traceLog("dialog_log", "[菜单] 菜单调用时无法获取Activity");
        return;
    }
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (!activity.isFinishing()) 长按消息菜单(activity, data);
            } catch (Throwable t) {
                traceLog("dialog_log", "[菜单] 异常: " + t);
            }
        }
    });
}

void showShutUpDialog(Activity activity, String qun, String uin, String nickName) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "设置禁言时长", 8);
                root.addView(titleView);
                TextView subView = new TextView(activity);
                subView.setText(nickName + "(" + uin + ")");
                subView.setTextSize(14);
                subView.setTextColor(subTextColor);
                subView.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(subView);
                LinearLayout inputRow = new LinearLayout(activity);
                inputRow.setOrientation(LinearLayout.HORIZONTAL);
                inputRow.setGravity(Gravity.CENTER_VERTICAL);
                final EditText input = makeInput(activity, "秒数 或 点⏱选择", null);
                input.setText("0");
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setTextSize(14);
                LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, - 2, 1.0f);
                inputRow.addView(input, inputLp);
                TextView btnTime = createButton(activity, "\u23F1", textColor, Color.TRANSPARENT, 16f, 6, 0, 0, false, 0, 0, null);
                btnTime.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        showTimePicker(activity, input, "输入禁言时长");
                    }
                });
                inputRow.addView(btnTime);
                root.addView(inputRow);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                btnBox.setPadding(0, dp(activity, 4), 0, 0);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final int seconds = parseDurationToSeconds(input.getText().toString());
                        if (ref[0] != null) ref[0].dismiss();
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                try {
                                    shutUp(qun, uin, seconds);
                                    Toast(seconds == 0 ? "已解除禁言" : "禁言设置成功: " + formatRemainingTime(seconds));
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[shutUp] 异常: " + t);
                                    Toast("禁言操作失败");
                                }
                            }
                        });
                        vibrate(activity, 32);
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                traceLog("dialog_log", "[showShutUpDialog] 异常: " + e);
                Toast("弹窗显示失败");
            }
        }
    });
}

public void showMuteAllDialog(Activity activity, String qun) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "全体禁言设置", 16);
                root.addView(titleView);
                LinearLayout inputRow = new LinearLayout(activity);
                inputRow.setOrientation(LinearLayout.HORIZONTAL);
                inputRow.setGravity(Gravity.CENTER_VERTICAL);
                final EditText input = makeInput(activity, "秒数 或 点⏱选择", null);
                input.setText("0");
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setTextSize(14);
                LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, - 2, 1.0f);
                inputRow.addView(input, inputLp);
                TextView btnTime = createButton(activity, "\u23F1", textColor, Color.TRANSPARENT, 16f, 6, 0, 0, false, 0, 0, null);
                btnTime.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        showTimePicker(activity, input, "输入禁言时长");
                    }
                });
                inputRow.addView(btnTime);
                root.addView(inputRow);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView unmute = createButton(activity, "解禁", pc("#81C784"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                unmute.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                try {
                                    shutUpAll(qun, false);
                                    clearMuteAllRecord(qun);
                                    Toast("已解除全体禁言");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[onClick] 异常: " + t);
                                }
                            }
                        });
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final long totalSec = parseDurationToSeconds(input.getText().toString());
                        if (ref[0] != null) ref[0].dismiss();
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                if (totalSec > 0) {
                                    try {
                                        shutUpAll(qun, true);
                                        saveMuteAllRecord(qun, System.currentTimeMillis() + totalSec * 1000L);
                                        Toast("已开启全体禁言: " + formatRemainingTime(totalSec));
                                        final Handler handler = new Handler(Looper.getMainLooper());
                                        handler.postDelayed(new Runnable() {
                                            public void run() {
                                                ThreadPool.execute(new Runnable() {
                                                    public void run() {
                                                        try {
                                                            shutUpAll(qun, false);
                                                            Toast("全体禁言已自动解除");
                                                        } catch (Throwable t) {
                                                            traceLog("dialog_log", "[onClick] 异常: " + t);
                                                        }
                                                    }
                                                });
                                            }
                                        }, totalSec * 1000L);
                                    } catch (Throwable t) {
                                        Toast("操作失败");
                                    }
                                } else {
                                    try {
                                        shutUpAll(qun, true);
                                        Toast("已开启全体禁言");
                                    } catch (Throwable t) {
                                        Toast("操作失败");
                                    }
                                }
                            }
                        });
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(unmute);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable t) {
                traceLog("dialog_log", "[onClick] 异常: " + t);
            }
        }
    });
}

void saveMuteAllRecord(String qun, long expireMs) {
    try {
        JSONObject obj = getMuteAllRecords();
        obj.put(qun, expireMs);
        putString("settings", "muteall_records", obj.toString());
    } catch (Throwable t) {
        traceLog("dialog_log", "[saveMuteAllRecord] " + t);
    }
}

void clearMuteAllRecord(String qun) {
    try {
        JSONObject obj = getMuteAllRecords();
        obj.remove(qun);
        putString("settings", "muteall_records", obj.toString());
    } catch (Throwable t) {
        traceLog("dialog_log", "[clearMuteAllRecord] " + t);
    }
}

JSONObject getMuteAllRecords() {
    try {
        String s = getString("settings", "muteall_records", "");
        if (s != null && !s.isEmpty()) return new JSONObject(s);
    } catch (Throwable t) {
        traceLog("dialog_log", "[getMuteAllRecords] " + t);
    }
    return new JSONObject();
}

void checkMuteAllExpiry() {
    try {
        String s = getString("settings", "muteall_records", "");
        if (s == null || s.isEmpty()) return;
        JSONObject obj = new JSONObject(s);
        long now = System.currentTimeMillis();
        boolean changed = false;
        java.util.Iterator it = obj.keys();
        java.util.List expired = new ArrayList();
        while (it.hasNext()) {
            String qun = (String) it.next();
            if (obj.getLong(qun) <= now) expired.add(qun);
        }
        for (int i = 0; i < expired.size(); i++) {
            String qun = (String) expired.get(i);
            try {
                shutUpAll(qun, false);
                traceLog("dialog_log", "[checkMuteAllExpiry] 到期自动解禁: " + qun);
            } catch (Throwable t) {
            }
            obj.remove(qun);
            changed = true;
        }
        if (changed) putString("settings", "muteall_records", obj.toString());
    } catch (Throwable t) {
        traceLog("dialog_log", "[checkMuteAllExpiry] " + t);
    }
}

public void showZanDialog(Activity activity, String targetUin) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "为Ta点赞", 4);
                root.addView(titleView);
                TextView subView = new TextView(activity);
                subView.setText("范围: 1-50次");
                subView.setTextSize(12);
                subView.setTextColor(subTextColor);
                subView.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(subView);
                final EditText input = makeInput(activity, "请输入点赞次数", null);
                input.setText("50");
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setTextSize(14);
                root.addView(input);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int count = 50;
                        try {
                            count = Integer.parseInt(input.getText().toString().trim());
                        } catch (Throwable ignored) {
                            traceLog("dialog_log", "[onClick] 异常: " + ignored);
                        }
                        if (count < 1) count = 1;
                        if (count > 50) count = 50;
                        if (ref[0] != null) ref[0].dismiss();
                        sendZan(targetUin, count);
                        qqToast(2, "点赞成功: " + count + "次");
                        vibrate(activity, 32);
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                Toast("弹窗显示失败");
            }
        }
    });
}

public void showPaiDialog(Activity activity, String targetUin, String peerUin, int chatType) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "拍一拍", 4);
                root.addView(titleView);
                TextView subView = new TextView(activity);
                subView.setText("范围: 1-200次");
                subView.setTextSize(12);
                subView.setTextColor(subTextColor);
                subView.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(subView);
                final EditText input = makeInput(activity, "请输入拍一拍次数", null);
                input.setText("1");
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setTextSize(14);
                root.addView(input);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int count = 1;
                        try {
                            count = Integer.parseInt(input.getText().toString().trim());
                        } catch (Throwable ignored) {
                            traceLog("dialog_log", "[onClick] 异常: " + ignored);
                        }
                        if (count < 1) count = 1;
                        if (count > 200) count = 200;
                        if (ref[0] != null) ref[0].dismiss();
                        final int finalCount = count;
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                for (int i = 0; i < finalCount; i++) {
                                    try {
                                        sendPai(targetUin, peerUin, chatType);
                                    } catch (Throwable t) {
                                        traceLog("dialog_log", "[onClick] 异常: " + t);
                                    }
                                }
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        qqToast(2, "拍一拍成功: " + finalCount + "次");
                                        vibrate(activity, 32);
                                    }
                                });
                            }
                        });
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                Toast("弹窗显示失败");
            }
        }
    });
}

public void showEncryptDecryptDialog(Activity activity, Object data) {
    if (activity == null || activity.isFinishing()) return;
    int MAX_LEN = 8000;
    String originalText = data != null ? data.msg : "";
    boolean isTooLong = originalText != null && originalText.length() > MAX_LEN;
    String displayText = isTooLong ? originalText.substring(0, MAX_LEN) + "\n\n【文本过长，已截断显示】" : (originalText != null ? originalText : "");
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                java.util.Stack undoStack = new java.util.Stack();
                java.util.Stack redoStack = new java.util.Stack();
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "加解密工具", 12);
                root.addView(titleView);
                final EditText input = new EditText(activity);
                input.setText(displayText);
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(13);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                input.setGravity(Gravity.TOP | Gravity.LEFT);
                input.setTypeface(Typeface.MONOSPACE);
                GradientDrawable inputBg = makeDialogInnerCard(activity, inputBgColor);
                input.setBackground(inputBg);
                if (isTooLong) input.setFocusable(false);
                GestureDetector gd = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
                    public boolean onDoubleTap(MotionEvent e) {
                        showExpandedResultDialog(activity, isTooLong ? originalText : input.getText().toString(), isDark);
                        return true;
                    }
                });
                input.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        gd.onTouchEvent(event);
                        return false;
                    }
                });
                LinearLayout
                    .LayoutParams inputParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, dp(activity, 160));
                inputParams.setMargins(0, 0, 0, dp(activity, 12));
                input.setLayoutParams(inputParams);
                root.addView(input);
                String[]btnTags = new String[] {
                    "base64_enc", "base64_dec", "hex_enc", "hex_dec", "unicode_enc", "unicode_dec", "url_enc", "url_dec", "caesar_enc", "caesar_dec", "aes_enc", "aes_dec", "des_enc", "des_dec", "binary", "md5", "sha256", "xor", "reverse", "undo", "redo", "clear", "send"
                };
                String[]btnNames = new String[] {
                    "Base64加密", "Base64解密", "转Hex", "Hex转字符", "转Unicode", "Unicode还原", "URL编码", "URL解码", "凯撒+3", "凯撒-3", "AES加密", "AES解密", "DES加密", "DES解密", "转二进制", "MD5摘要", "SHA256", "XOR异或", "文本倒序", "↩撤销", "↪重做", "清空", "发送结果"
                };
                GridLayout btnGrid = new GridLayout(activity);
                btnGrid.setColumnCount(3);
                View.OnClickListener clickListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        String tag = (String) v.getTag();
                        String cur = isTooLong ? originalText : input.getText().toString();
                        if ("undo".equals(tag)) {
                            if (!undoStack.isEmpty()) {
                                redoStack.push(cur);
                                String p = (String) undoStack.pop();
                                input.setText(p);
                                input.setSelection(p.length());
                                Toast("已撤销");
                            } else Toast("没有可撤销的操作");
                            vibrate(activity, 20);
                            return;
                        }
                        if ("redo".equals(tag)) {
                            if (!redoStack.isEmpty()) {
                                undoStack.push(cur);
                                String n = (String) redoStack.pop();
                                input.setText(n);
                                input.setSelection(n.length());
                                Toast("已重做");
                            } else Toast("没有可重做的操作");
                            vibrate(activity, 20);
                            return;
                        }
                        if ("clear".equals(tag)) {
                            undoStack.push(cur);
                            redoStack.clear();
                            input.setText("");
                            vibrate(activity, 20);
                            return;
                        }
                        if ("send".equals(tag)) {
                            String content = input.getText().toString();
                            if (content.trim().isEmpty()) {
                                Toast("请输入内容");
                                return;
                            }
                            if (data != null && data.contact != null) {
                                sendMsg(data.contact, content);
                                Toast("已发送");
                                vibrate(activity, 30);
                            } else {
                                Toast("发送失败：无法获取聊天对象");
                            }
                            return;
                        }
                        if (cur.trim().isEmpty()) {
                            Toast("请输入文本");
                            return;
                        }
                        undoStack.push(cur);
                        redoStack.clear();
                        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).withEndAction(new Runnable() {
                            public void run() {
                                v.animate().scaleX(1f).scaleY(1f).setDuration(50).start();
                            }
                        }).start();
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                String result = null;
                                String errTip = null;
                                try {
                                    if ("base64_enc".equals(tag)) result = encryptBase64(cur);
                                    else if ("base64_dec".equals(tag)) result = decryptBase64(cur);
                                    else if ("unicode_enc".equals(tag)) result = encryptUnicode(cur);
                                    else if ("unicode_dec".equals(tag)) result = decryptUnicode(cur);
                                    else if ("hex_enc".equals(tag)) result = stringToHex(cur);
                                    else if ("hex_dec".equals(tag)) result = hexToString(cur);
                                    else if ("url_enc".equals(tag)) result = urlEncode(cur);
                                    else if ("url_dec".equals(tag)) result = urlDecode(cur);
                                    else if ("caesar_enc".equals(tag)) result = caesarCipher(cur, 3);
                                    else if ("caesar_dec".equals(tag)) result = caesarCipher(cur, - 3);
                                    else if ("aes_enc".equals(tag)) result = aesEncrypt(cur);
                                    else if ("aes_dec".equals(tag)) result = aesDecrypt(cur);
                                    else if ("des_enc".equals(tag)) result = desEncrypt(cur);
                                    else if ("des_dec".equals(tag)) result = desDecrypt(cur);
                                    else if ("binary".equals(tag)) result = stringToBinary(cur);
                                    else if ("md5".equals(tag)) result = getHash(cur, "MD5");
                                    else if ("sha256".equals(tag)) result = getHash(cur, "SHA-256");
                                    else if ("xor".equals(tag)) result = xorCipher(cur);
                                    else if ("reverse".equals(tag)) result = reverseString(cur);
                                } catch (OutOfMemoryError e) {
                                    errTip = "内存不足";
                                } catch (Throwable e) {
                                    errTip = "处理错误: " + e.getMessage();
                                }
                                final String fr = result;
                                final String fe = errTip;
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        if (fr != null) {
                                            input.setText(fr);
                                            input.setSelection(0);
                                            vibrate(activity, 28);
                                            Toast("处理完成");
                                        } else {
                                            if (!undoStack.isEmpty()) undoStack.pop();
                                            Toast(fe != null ? fe : "处理失败");
                                            vibrate(activity, 50);
                                        }
                                    }
                                });
                            }
                        });
                    }
                };
                for (int i = 0; i < btnNames.length; i++) {
                    int strokeColor = borderColor;
                    int btnTextColor = textColor;
                    if ("clear".equals(btnTags[i])) {
                        strokeColor = pc("#FF5252");
                        btnTextColor = pc("#FF5252");
                    } else if ("undo".equals(btnTags[i]) || "redo".equals(btnTags[i])) {
                        strokeColor = pc("#FFB74D");
                        btnTextColor = isDark ? pc("#FFB74D") : pc("#EF6C00");
                    } else if ("send".equals(btnTags[i])) {
                        strokeColor = pc("#4CAF50");
                        btnTextColor = pc("#4CAF50");
                    }
                    TextView btn = createButton(activity, btnNames[i], btnTextColor,
                        isDark ? pc("#33FFFFFF") : pc("#F0F0F0"), 11f, 8, 4, 10, false, 1, strokeColor, null);
                    btn.setTag(btnTags[i]);
                    btn.setOnClickListener(clickListener);
                    GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                    p.width = 0;
                    p.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    p.columnSpec = GridLayout.spec(i % 3, 1f);
                    p.rowSpec = GridLayout.spec(i / 3);
                    p.setMargins(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
                    btnGrid.addView(btn, p);
                }
                root.addView(btnGrid);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 16), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(cancel);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                Toast("弹窗显示失败: " + e.getMessage());
            }
        }
    });
}

public void showCodeConsoleDialog(Activity activity, Object data) {
    if (activity == null || activity.isFinishing()) return;
    if (data == null) {
        Toast("数据无效");
        return;
    }
    String initialCode = "";
    Object msgValue = fieldGet(data, "msg");
    if (msgValue != null) initialCode = String.valueOf(msgValue);
    final String finalCode = initialCode;
    final Object outerInterpreter = this.interpreter;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                int successColor = isDark ? pc("#81C784") : pc("#2E7D32");
                int errorColor = isDark ? pc("#FF8A80") : pc("#C62828");
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "代码控制台", 12);
                root.addView(titleView);
                final TextView resultView = new TextView(activity);
                resultView.setText("等待执行...");
                resultView.setTextSize(12);
                resultView.setTextColor(textColor);
                resultView.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                GradientDrawable resultBg = makeDialogInnerCard(activity, inputBgColor);
                resultView.setBackground(resultBg);
                resultView.setTypeface(Typeface.MONOSPACE);
                resultView.setTextIsSelectable(true);
                GestureDetector gd = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
                    public boolean onDoubleTap(MotionEvent e) {
                        showExpandedResultDialog(activity, resultView.getText().toString(), isDark);
                        return true;
                    }
                });
                resultView.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        gd.onTouchEvent(event);
                        return false;
                    }
                });
                ScrollView resultScroll = new ScrollView(activity);
                LinearLayout
                    .LayoutParams scrollParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, dp(activity, 140));
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                resultScroll.setLayoutParams(scrollParams);
                resultScroll.addView(resultView);
                root.addView(resultScroll);
                final EditText input = new EditText(activity);
                input.setText(finalCode);
                input.setHint("请输入要执行的代码 (BeanShell语法)");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(13);
                input.setGravity(Gravity.TOP | Gravity.LEFT);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                input.setTypeface(Typeface.MONOSPACE);
                LinearLayout
                    .LayoutParams inputParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, dp(activity, 120));
                inputParams.setMargins(0, 0, 0, dp(activity, 16));
                input.setLayoutParams(inputParams);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(inputBgColor);
                input.setBackground(inputBg);
                root.addView(input);
                final AlertDialog[]ref = new AlertDialog[1];
                TextView execBtn = createButton(activity, "▶  执行代码", Color.WHITE,
                    isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), 14f, 8, 24, 12, false, 0, 0, null);
                LinearLayout
                    .LayoutParams btnParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                btnParams.gravity = Gravity.CENTER_HORIZONTAL;
                btnParams.setMargins(0, 0, 0, dp(activity, 16));
                execBtn.setLayoutParams(btnParams);
                execBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String code = input.getText().toString().trim();
                        if (code.isEmpty()) {
                            Toast("请输入代码");
                            vibrate(activity, 20);
                            return;
                        }
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(new Runnable() {
                            public void run() {
                                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            }
                        }).start();
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                try {
                                    activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            resultView.setText("⏳ 执行中...");
                                            resultView.setTextColor(textColor);
                                        }
                                    });
                                    outerInterpreter.set("data", data);
                                    outerInterpreter.set("msg", fieldGet(data, "data"));
                                    outerInterpreter.set("qun", fieldGet(data, "peerUin"));
                                    outerInterpreter.set("uin", fieldGet(data, "userUin"));
                                    outerInterpreter.set("type", fieldGet(data, "type"));
                                    outerInterpreter.set("msgtype", fieldGet(data, "msgType"));
                                    outerInterpreter.set("qq", myUin);
                                    Object result = outerInterpreter.eval(code);
                                    String resultStr = result != null ? String.valueOf(result) : "执行成功 (无返回值)";
                                    activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            resultView.setText("✅ " + resultStr);
                                            resultView.setTextColor(successColor);
                                            vibrate(activity, 32);
                                            Toast("执行完成");
                                        }
                                    });
                                } catch (Throwable e) {
                                    String err = e.toString();
                                    String pos = e.getStackTrace().length > 0 ? "\n位置: " + e.getStackTrace()[0] : "";
                                    activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            resultView.setText("❌ 执行出错:\n" + err + pos);
                                            resultView.setTextColor(errorColor);
                                            vibrate(activity, 50);
                                            Toast("执行出错");
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
                root.addView(execBtn);
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(cancel);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                Toast("弹窗显示失败");
            }
        }
    });
}

public void showExpandedResultDialog(Activity activity, String content, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "完整内容", 12);
                root.addView(titleView);
                TextView contentView = new TextView(activity);
                contentView.setText(content != null ? content : "");
                contentView.setTextSize(13);
                contentView.setTextColor(textColor);
                contentView.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                contentView.setTypeface(Typeface.MONOSPACE);
                contentView.setTextIsSelectable(true);
                GradientDrawable contentBg = makeDialogInnerCard(activity, inputBgColor);
                contentView.setBackground(contentBg);
                ScrollView scrollView = new ScrollView(activity);
                LinearLayout
                    .LayoutParams scrollParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, dp(activity, 400));
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scrollView.setLayoutParams(scrollParams);
                scrollView.addView(contentView);
                root.addView(scrollView);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView copyBtn = createButton(activity, "复制全部", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                copyBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        copyToClipboard(activity, content != null ? content : "");
                        Toast("已复制到剪贴板");
                    }
                });
                TextView closeBtn = createButton(activity, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(copyBtn);
                btnBox.addView(closeBtn);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                Toast("展开显示失败: " + e.getMessage());
            }
        }
    });
}

public void showTitleDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "设置群头衔", 8);
                root.addView(titleView);
                TextView subView = new TextView(activity);
                subView.setText(nickName + "(" + uin + ")");
                subView.setTextSize(14);
                subView.setTextColor(subTextColor);
                subView.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(subView);
                final EditText input = makeInput(activity, "请输入头衔（留空则清除）", null);
                input.setTextSize(14);
                root.addView(input);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String t = input.getText().toString().trim();
                        if (ref[0] != null) ref[0].dismiss();
                        try {
                            setGroupMemberTitle(qun, uin, t);
                            Toast(t.isEmpty() ? "头衔已清除" : "头衔设置成功: " + t);
                            vibrate(activity, 32);
                        } catch (Throwable e) {
                            Toast("设置头衔失败: " + e.getMessage());
                        }
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                Toast("弹窗显示失败: " + e.getMessage());
            }
        }
    });
}

public void showSetAdminDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFE0E0E0") : pc("#FF202020");
                int subTextColor = isDark ? pc("#FFA0A0A0") : pc("#FF707070");
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "设置管理员", 8);
                root.addView(titleView);
                TextView sub = new TextView(activity);
                sub.setText(nickName + "(" + uin + ")");
                sub.setTextSize(14);
                sub.setTextColor(subTextColor);
                sub.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(sub);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView revoke = createButton(activity, "撤销管理", pc("#FF5252"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                revoke.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            setGroupAdmin(qun, uin, false);
                            Toast("已撤销管理员");
                            vibrate(activity, 32);
                        } catch (Throwable e) {
                            Toast("操作失败");
                        }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = new TextView(activity);
                confirm.setText("设为管理");
                confirm.setTextSize(15);
                confirm.setTextColor(isDark ? pc("#81C784") : pc("#2E7D32"));
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            setGroupAdmin(qun, uin, true);
                            Toast("已设为管理员");
                            vibrate(activity, 32);
                        } catch (Throwable e) {
                            Toast("操作失败");
                        }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(revoke);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable t) {
                traceLog("dialog_log", "[onClick] 异常: " + t);
            }
        }
    });
}

public void showChangeCardDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "修改群名片", 8);
                root.addView(titleView);
                TextView sub = new TextView(activity);
                sub.setText(nickName + "(" + uin + ")");
                sub.setTextSize(14);
                sub.setTextColor(subTextColor);
                sub.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(sub);
                final EditText input = makeInput(activity, "新的群名片（留空则清除）", null);
                input.setTextSize(14);
                root.addView(input);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String card = input.getText().toString().trim();
                        if (ref[0] != null) ref[0].dismiss();
                        try {
                            changeMemberName(qun, uin, card);
                            Toast(card.isEmpty() ? "群名片已清除" : "群名片修改成功");
                            vibrate(activity, 32);
                        } catch (Throwable e) {
                            Toast("修改失败: " + e.getMessage());
                        }
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) {
                Toast("弹窗显示失败: " + e.getMessage());
            }
        }
    });
}

void showMsgDataPaeseDialog(Activity act, Object data) {
    if (act == null || act.isFinishing()) return;
    ThreadPool.execute(new Runnable() {
        public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append("# MsgData 层\n\n");
            try {
                sb.append("**消息文本**：").append(data.msg).append("\n\n**String msg = data.msg;**").append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                sb
                    .append("**发送者QQ**：")
                    .append(data .userUin)
                    .append("\n\n**String userUin = data.userUin;**")
                    .append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                sb
                    .append("**聊天对象QQ**：")
                    .append(data .peerUin)
                    .append("\n\n**String peerUin = data.peerUin;**")
                    .append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                sb.append("**聊天类型**：").append(data.type).append("\n\n**int type = data.type;**").append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                sb
                    .append("**消息类型**：")
                    .append(data .msgType)
                    .append("\n\n**int msgType = data.msgType;**")
                    .append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                sb.append("**消息ID**：").append(data.msgId).append("\n\n**long msgId = data.msgId;**").append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                sb
                    .append("**发送时间**：")
                    .append(data
                    .time > 0 ? new java
                    .text
                    .SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java .util .Date(data .time * 1000)): "未知")
                    .append("\n\n**long time = data.time;**")
                    .append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                sb.append("**本地路径**：").append(data.path).append("\n\n**String path = data.path;**").append("\n\n");
            } catch (Throwable t) {
                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
            }
            try {
                if (data.data != null) {
                    sb.append("# MsgRecord 层\n\n");
                    Object msg = data.data;
                    try {
                        sb
                            .append("**发送者Uid**：")
                            .append(msg .senderUid)
                            .append("\n\n**String senderUid = data.data.senderUid;**")
                            .append("\n\n");
                    } catch (Throwable t) {
                        traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                    }
                    try {
                        sb
                            .append("**发送者昵称**：")
                            .append(msg .sendNickName)
                            .append("\n\n**String sendNickName = data.data.sendNickName;**")
                            .append("\n\n");
                    } catch (Throwable t) {
                        traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                    }
                    try {
                        sb
                            .append("**群名片**：")
                            .append(msg .sendMemberName)
                            .append("\n\n**String sendMemberName = data.data.sendMemberName;**")
                            .append("\n\n");
                    } catch (Throwable t) {
                        traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                    }
                    try {
                        sb
                            .append("**对象Uid**：")
                            .append(msg .peerUid)
                            .append("\n\n**String peerUid = data.data.peerUid;**")
                            .append("\n\n");
                    } catch (Throwable t) {
                        traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                    }
                    try {
                        sb
                            .append("**对象名称**：")
                            .append(msg .peerName)
                            .append("\n\n**String peerName = data.data.peerName;**")
                            .append("\n\n");
                    } catch (Throwable t) {
                        traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                    }
                    try {
                        sb
                            .append("**消息序列号**：")
                            .append(msg .msgSeq)
                            .append("\n\n**long msgSeq = data.data.msgSeq;**")
                            .append("\n\n");
                    } catch (Throwable t) {
                        traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                    }
                    try {
                        sb
                            .append("**发送状态**：")
                            .append(msg .sendStatus)
                            .append("\n\n**int sendStatus = data.data.sendStatus;**")
                            .append("\n\n");
                    } catch (Throwable t) {
                        traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                    }
                    if (msg.elements != null && !msg.elements.isEmpty()) {
                        sb.append("# 消息元素 \n\n");
                        int i = 0;
                        for (Object el: msg.elements) {
                            if (el == null) continue;
                            sb.append("元素 [").append(i++).append("]\n\n");
                            try {
                                sb
                                    .append("**元素类型**：")
                                    .append(el .elementType)
                                    .append("\n\n**int elementType = element.elementType;**")
                                    .append("\n\n");
                            } catch (Throwable t) {
                                traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                            }
                            if (el.elementType == 1 && el.textElement != null) {
                                Object text = el.textElement;
                                try {
                                    sb
                                        .append("**文本内容**：")
                                        .append(text .content)
                                        .append("\n\n**String content = element.textElement.content;**")
                                        .append("\n\n");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                                }
                                try {
                                    sb
                                        .append("**艾特类型**：")
                                        .append(text .atType)
                                        .append("\n\n**int atType = element.textElement.atType;**")
                                        .append("\n\n");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                                }
                            }
                            if (el.elementType == 2 && el.picElement != null) {
                                Object pic = el.picElement;
                                try {
                                    sb
                                        .append("**图片URL**：")
                                        .append(pic .sourcePath)
                                        .append("\n\n**String sourcePath = element.picElement.sourcePath;**")
                                        .append("\n\n");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                                }
                                try {
                                    sb
                                        .append("**图片尺寸**：")
                                        .append(pic .picWidth)
                                        .append("x")
                                        .append(pic .picHeight)
                                        .append("\n\n**int picWidth = element.picElement.picWidth;**")
                                        .append("\n\n");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                                }
                            }
                            if (el.elementType == 4 && el.pttElement != null) {
                                Object ptt = el.pttElement;
                                try {
                                    sb
                                        .append("**语音时长**：")
                                        .append(ptt .duration)
                                        .append("秒\n\n**int duration = element.pttElement.duration;**")
                                        .append("\n\n");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                                }
                            }
                            if (el.elementType == 5 && el.videoElement != null) {
                                Object video = el.videoElement;
                                try {
                                    sb
                                        .append("**视频文件**：")
                                        .append(video .fileName)
                                        .append("\n\n**String fileName = element.videoElement.fileName;**")
                                        .append("\n\n");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                                }
                            }
                            if (el.elementType == 6 && el.faceElement != null) {
                                Object face = el.faceElement;
                                try {
                                    sb
                                        .append("**表情内容**：")
                                        .append(face .faceText)
                                        .append("\n\n**String faceText = element.faceElement.faceText;**")
                                        .append("\n\n");
                                } catch (Throwable t) {
                                    traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                sb.append("\n\n**解析异常**：").append(e.getMessage());
            }
            final String content = sb.toString();
            mkts(act, "解析原始消息", content);
        }
    });
}

void showCopyConfirmDialog(Activity act, String title, String text, boolean isDark) {
    if (act == null) return;
    final String finalText = text != null ? text : "获取失败或内容为空";
    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(act, 16), dp(act, 20), dp(act, 16), dp(act, 16));
                TextView titleView = new TextView(act);
                titleView.setText(title);
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(act, 12));
                root.addView(titleView);
                TextView contentView = new TextView(act);
                contentView.setText(finalText);
                contentView.setTextSize(13);
                contentView.setTextColor(textColor);
                contentView.setPadding(dp(act, 12), dp(act, 10), dp(act, 12), dp(act, 10));
                contentView.setTextIsSelectable(true);
                GradientDrawable contentBg = makeDialogInnerCard(act, inputBgColor);
                contentView.setBackground(contentBg);
                ScrollView scroll = new ScrollView(act);
                LinearLayout
                    .LayoutParams scrollParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                scrollParams.setMargins(0, 0, 0, dp(act, 12));
                scroll.setLayoutParams(scrollParams);
                int maxHeight = dp(act, 400);
                scroll.addView(contentView);
                root.addView(scroll);
                contentView.post(new Runnable() {
                    public void run() {
                        int measured = contentView.getMeasuredHeight();
                        if (measured > maxHeight) {
                            LinearLayout
                                .LayoutParams lp = new LinearLayout
                                .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, maxHeight);
                            lp.setMargins(0, 0, 0, dp(act, 12));
                            scroll.setLayoutParams(lp);
                        }
                    }
                });
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(act);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView copyAll = new TextView(act);
                copyAll.setText("复制全部");
                copyAll.setTextSize(15);
                copyAll.setTextColor(isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"));
                copyAll.setPadding(dp(act, 16), dp(act, 12), dp(act, 16), dp(act, 12));
                copyAll.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            copyToClipboard(act, finalText);
                            qqToast(2, "已复制到剪贴板");
                            if (ref[0] != null) ref[0].dismiss();
                        } catch (Throwable e) {
                            Toast("复制失败: " + e.getMessage());
                        }
                    }
                });
                TextView cancel = createButton(act, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(copyAll);
                btnBox.addView(cancel);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(act,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                if (!act.isFinishing() && !(Build.VERSION.SDK_INT >= 17 && act.isDestroyed())) {
                    ref[0].show();
                    if (ref[0] != null && ref[0].getWindow() != null) applyUiTheme(act, ref[0], 0);
                }
            } catch (Throwable e) {
                Toast("显示弹窗失败: " + e.getMessage());
            }
        }
    });
}

void showKickConfirmDialog(Activity activity, String qun, String uin, String nickName, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    uiHandler.post(new Runnable() {
        public void run() {
            try {
                int textColor = isDark ? pc("#FFE0E0E0") : pc("#FF202020");
                int subTextColor = isDark ? pc("#FFA0A0A0") : pc("#FF707070");
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "确认踢出成员", 8);
                root.addView(titleView);
                TextView sub = new TextView(activity);
                sub.setText(nickName + "(" + uin + ")\n请选择踢出方式");
                sub.setTextSize(14);
                sub.setTextColor(subTextColor);
                sub.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(sub);
                final AlertDialog[]ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView permanent = createButton(activity, "永久踢出", pc("#FF5252"), Color.TRANSPARENT, 15f, 0, 16, 12,
                    false, 0, 0, null);
                permanent.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            kickGroup(qun, uin, true);
                            qqToast(2, "永久踢出操作已执行");
                        } catch (Throwable t) {
                            traceLog("dialog_log", "[onClick] 异常: " + t);
                        }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确认踢出", isDark ? pc("#FFB74D") : pc("#F57C00"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            kickGroup(qun, uin, false);
                            qqToast(2, "踢出操作已执行");
                        } catch (Throwable t) {
                            traceLog("dialog_log", "[onClick] 异常: " + t);
                        }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(permanent);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable t) {
                traceLog("dialog_log", "[onClick] 异常: " + t);
            }
        }
    });
}

void showRawMessageDialog(Object msgData) {
    Activity activity = safeActivity();
    if (activity == null) return;
    String rawText = "获取失败";
    try {
        if (msgData != null) rawText = prettyPrint(msgData);
    } catch (Throwable e) {
        rawText = "获取失败: " + e.getMessage();
    }
    showCopyConfirmDialog(activity, "原始消息", rawText, isThemeDark(activity));
}

void showAtListDialog(Object atListData) {
    Activity activity = safeActivity();
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);
    int textColor = tc(activity, "on_surface_strong");
    int subTextColor = tc(activity, "on_surface_sub");
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "艾特列表", 12);
                root.addView(titleView);
                LinearLayout listContainer = new LinearLayout(activity);
                listContainer.setOrientation(LinearLayout.VERTICAL);
                final LinearLayout loading = createModernLoading(activity, isDark);
                listContainer.addView(loading);
                ScrollView scrollView = new ScrollView(activity);
                LinearLayout
                    .LayoutParams scrollParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scrollView.setLayoutParams(scrollParams);
                scrollView.addView(listContainer);
                root.addView(scrollView);
                final AlertDialog[]ref = new AlertDialog[1];
                TextView closeBtn = createButton(activity, "关闭", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"),
                    Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                btnBox.addView(closeBtn);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            final List results = new ArrayList();
                            if (atListData instanceof List) {
                                List atList = (List) atListData;
                                for (int i = 0; i < atList.size(); i++) {
                                    Object obj = atList.get(i);
                                    String atUin = (String) obj;
                                    String nick = "获取失败";
                                    try {
                                        Object card = GetCard(atUin);
                                        if (card != null) {
                                            try {
                                                nick = String.valueOf(card.strNick);
                                            } catch (Throwable t) {
                                                traceLog("dialog_log", "[onClick] 异常: " + t);
                                            }
                                        }
                                    } catch (Throwable ignored) {
                                        traceLog("dialog_log", "[onClick] 异常: " + ignored);
                                    }
                                    results.add(nick + "(" + atUin + ")");
                                }
                            }
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    try {
                                        listContainer.removeView(loading);
                                        if (results.isEmpty()) {
                                            TextView emptyView = new TextView(activity);
                                            emptyView.setText("此消息无艾特列表");
                                            emptyView.setTextSize(14);
                                            emptyView.setTextColor(subTextColor);
                                            emptyView.setGravity(Gravity.CENTER);
                                            emptyView.setPadding(dp(activity, 20), dp(activity, 40), dp(activity, 20), dp(activity, 40));
                                            listContainer.addView(emptyView);
                                        } else {
                                            for (int i = 0; i < results.size(); i++) {
                                                String line = (String) results.get(i);
                                                TextView itemView = new TextView(activity);
                                                itemView.setText(line);
                                                itemView.setTextSize(14);
                                                itemView.setTextColor(textColor);
                                                itemView.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
                                                listContainer.addView(itemView);
                                            }
                                        }
                                    } catch (Throwable t) {
                                        traceLog("dialog_log", "[showAtListDialog] 异常: " + t);
                                    }
                                }
                            });
                        } catch (Throwable e) {
                            Toast("获取艾特列表失败");
                        }
                    }
                });
            } catch (Throwable e) {
                Toast("弹窗显示失败");
            }
        }
    });
}

LinearLayout createModernLoading(Activity activity, boolean isDark) {
    LinearLayout container = new LinearLayout(activity);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setGravity(Gravity.CENTER);
    container.setPadding(dp(activity, 20), dp(activity, 40), dp(activity, 20), dp(activity, 40));
    ProgressBar pb = new ProgressBar(activity);
    pb.setIndeterminate(true);
    pb.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 36), dp(activity, 36)));
    container.addView(pb);
    TextView loadingText = new TextView(activity);
    loadingText.setText("加载中...");
    loadingText.setTextSize(13);
    loadingText.setTextColor(tc(activity, "on_surface_sub"));
    loadingText.setPadding(0, dp(activity, 12), 0, 0);
    loadingText.setGravity(Gravity.CENTER);
    container.addView(loadingText);
    return container;
}

void showGroupInfoDialog(Activity activity, String groupUin, boolean isDark) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                Object troopInfo = findTroopInfo(groupUin);
                StringBuilder sb = new StringBuilder();
                sb.append("群号: ").append(groupUin).append("\n\n");
                if (troopInfo != null) {
                    try {
                        sb.append("群名称: ").append(String.valueOf(troopInfo.troopname)).append("\n");
                    } catch (Throwable ignored) {
                        sb.append("群名称: 获取失败\n");
                    }
                    try {
                        sb.append("群主QQ: ").append(String.valueOf(troopInfo.troopowneruin)).append("\n");
                    } catch (Throwable ignored) {
                        traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored);
                    }
                    try {
                        sb
                            .append("群人数: ")
                            .append(String .valueOf(troopInfo .wMemberNum))
                            .append(" / ")
                            .append(String .valueOf(troopInfo .wMemberMax))
                            .append("\n");
                    } catch (Throwable ignored) {
                        traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored);
                    }
                    try {
                        long createTime = Long.parseLong(String.valueOf(troopInfo.troopCreateTime));
                        if (createTime > 0) sb.append("创建时间: ").append(timestampToDate(createTime * 1000)).append("\n");
                    } catch (Throwable ignored) {
                        traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored);
                    }
                    try {
                        String q = String.valueOf(troopInfo.joinTroopQuestion);
                        if (q != null && !q.isEmpty() && !q.equals("null")) sb.append("进群问题: ").append(q).append("\n");
                    } catch (Throwable ignored) {
                        traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored);
                    }
                    try {
                        String a = String.valueOf(troopInfo.joinTroopAnswer);
                        if (a != null && !a.isEmpty() && !a.equals("null")) sb.append("进群答案: ").append(a).append("\n");
                    } catch (Throwable ignored) {
                        traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored);
                    }
                    try {
                        String memo = String.valueOf(troopInfo.troopmemo);
                        if (memo != null && !memo .isEmpty() && !memo .equals("null")) sb
                            .append("\n群公告:\n")
                            .append(memo)
                            .append("\n");
                    } catch (Throwable ignored) {
                        traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored);
                    }
                } else {
                    sb.append("未能获取到群详细信息\n");
                }
                final String result = sb.toString();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        showCopyConfirmDialog(activity, "群详细信息", result, isDark);
                    }
                });
            } catch (Throwable e) {
                Toast("获取群信息失败");
            }
        }
    });
}

void showMemberInfoDialog(Activity activity, String peerUin, String userUin, int chatType) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                StringBuilder sb = new StringBuilder();
                Object card = GetCard(userUin);
                Object member = chatType == 2 ? getMemberInfo(peerUin, userUin) : null;
                sb.append("QQ: ").append(userUin).append("\n");

                String nick = "未知";
                String cn = card?.strNick ?: "";
                if (!cn.isEmpty() && !cn.contains("*") && !"null".equals(cn)) nick = cn;
                if ("未知".equals(nick) && member != null) {
                    String mn = member?.uinName ?: "";
                    if (!mn.isEmpty() && !mn.contains("*") && !"null".equals(mn)) nick = mn;
                }
                sb.append("昵称: ").append(nick).append("\n");

                String rm = card?.remark ?: "";
                if (!rm.isEmpty() && !"null".equals(rm)) sb.append("备注: ").append(rm).append("\n");

                if (card != null) {
                    String qid = card?.qid ?: "";
                    if (!qid.isEmpty() && !"null".equals(qid)) sb.append("QID: ").append(qid).append("\n");

                    int age = card?.age ?: 0;
                    if (age > 0) sb.append("年龄: ").append(age).append("\n");

                    String loc = ((card?.strCountry ?: "") + " " + (card?.strProvince ?: "") + " " + (card?.strCity ?: "")).trim();
                    if (!loc.isEmpty() && !"null".equals(loc)) sb.append("地区: ").append(loc).append("\n");

                    String school = card?.strSchool ?: "";
                    if (!school.isEmpty() && !"null".equals(school)) sb.append("学校: ").append(school).append("\n");

                    String company = card?.strCompany ?: "";
                    if (!company.isEmpty() && !"null".equals(company)) sb.append("公司: ").append(company).append("\n");

                    String email = card?.strEmail ?: "";
                    if (!email.isEmpty() && !"null".equals(email)) sb.append("邮箱: ").append(email).append("\n");

                    String sign = card?.strSign ?: "";
                    if (!sign.isEmpty() && !"null".equals(sign)) sb.append("签名: ").append(sign).append("\n");
                }

                int qqLevel = card?.iQQLevel ?: member?.uinLevel ?: 0;
                sb.append("QQ等级: Lv.").append(qqLevel).append("\n");
                if (card != null) {
                    int iQQVipLevel = card?.iQQVipLevel ?: 0;
                    if (iQQVipLevel > 0) sb.append("QQ会员: Lv.").append(iQQVipLevel).append("\n");

                    int iSuperVipLevel = card?.iSuperVipLevel ?: 0;
                    if (iSuperVipLevel > 0) sb.append("超级会员: Lv.").append(iSuperVipLevel).append("\n");

                    long lVoteCount = card?.lVoteCount ?: 0L;
                    sb.append("名片赞: ").append(lVoteCount).append("\n");

                    long lLoginDays = card?.lLoginDays ?: 0L;
                    sb.append("登录天数: ").append(lLoginDays).append("\n");

                    boolean isForbid = card?.isForbidAccount ?: false;
                    if (isForbid) sb.append("账号状态: 已封禁\n");
                }

                if (chatType == 2 && member != null) {
                    String memberName = member?.uinName ?: "";
                    if (!memberName.isEmpty() && !"null".equals(memberName)) sb.append("群名片: ").append(memberName).append("\n");

                    int memberLevel = member?.uinLevel ?: 0;
                    sb.append("群等级: Lv.").append(memberLevel).append("\n");

                    String role = member?.role ?: "";
                    if (!role.isEmpty() && !"null".equals(role)) sb.append("角色: ").append(convertRole(role)).append("\n");

                    long joinTime = member?.joinGroupTime ?: 0L;
                    if (joinTime > 0) sb.append("入群时间: ").append(timestampToDate(joinTime * 1000)).append("\n");

                    long lastTime = member?.lastActiveTime ?: 0L;
                    if (lastTime > 0) sb.append("最后活跃: ").append(timestampToDate(lastTime * 1000)).append("\n");

                    List gagList = (List) getProhibitList(peerUin);
                    String gagStatus = "";
                    if (gagList != null) {
                        for (Object gag : gagList) {
                            if (gag == null) continue;
                            if (userUin.equals(String.valueOf(gag.user))) {
                                gagStatus = getGagStatus(String.valueOf(gag.endTime));
                                break;
                            }
                        }
                    }
                    if (!gagStatus.isEmpty()) sb.append("禁言状态: ").append(gagStatus).append("\n");
                }

                final String result = sb.toString();
                final boolean isDark = isThemeDark(activity);
                uiHandler.post(new Runnable() {
                    public void run() {
                        try {
                            showCopyConfirmDialog(activity, "详细信息", result, isDark);
                        } catch (Throwable e) {
                            traceLog("dialog_log", "[showMemberInfoDialog.ui] 异常: " + e);
                            Toast("弹窗打开失败: " + e.getMessage());
                        }
                    }
                });
            } catch (Throwable e) {
                traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + e);
                uiHandler.post(new Runnable() { public void run() { Toast("获取失败: " + e.getMessage()); } });
            }
        }
    });
}

void handleCloneAvatar(final String uin) {
    String url = "http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640";
    String fileName = "avatar_" + getTime() + ".png";
    executeDownloadAndUpload(url, fileName, "开始克隆头像", "克隆头像成功");
}

void handleUploadAvatar(final String quntext) {
    String url = null;
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[pic=([^\\]]+)\\]").matcher(quntext);
    if (m.find()) {
        url = m.group(1);
    }
    if (url == null) {
        Toast("未找到图片链接");
        return;
    }
    String fileName = "pic_" + getTime() + ".png";
    executeDownloadAndUpload(url, fileName, "开始上传图片", "上传头像成功");
}

private void executeDownloadAndUpload(final String url,
    final String fileName,
    final String startMsg,
    final String succMsg) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            final String savePath = pluginPath + "/cache/" + fileName;
            traceLog("dialog_log", "[executeDownloadAndUpload] 开始下载: " + url);
            Toast(startMsg);
            boolean downloadOk = downloadFile(url, savePath, new ProgressCallback() {
                public void onProgress(int progress) {
                    traceLog("dialog_log", "[executeDownloadAndUpload] 下载进度: " + progress + "%");
                }
                public void onProgressTip(String tip) {
                    traceLog("dialog_log", "[executeDownloadAndUpload] " + tip);
                }
            });
            if (!downloadOk) {
                traceLog("dialog_log", "[executeDownloadAndUpload] 下载失败: " + url);
                Toast("下载失败");
                return;
            }
            traceLog("dialog_log", "[executeDownloadAndUpload] 下载完成，准备上传: " + savePath);
            Toast("正在上传，请稍候...");
            uiHandler.post(new Runnable() {
                public void run() {
                    try {
                        if (上传头像(savePath)) {
                            Toast(succMsg);
                            traceLog("dialog_log", "[executeDownloadAndUpload] " + succMsg + "，准备延迟删除");
                            uiHandler.postDelayed(new Runnable() {
                                public void run() {
                                    删除(savePath);
                                    traceLog("dialog_log", "[executeDownloadAndUpload] 文件已删除");
                                }
                            }, 2500);
                        } else {
                            Toast("上传失败");
                            traceLog("dialog_log", "[executeDownloadAndUpload] 上传失败");
                            删除(savePath);
                        }
                    } catch (Throwable e) {
                        traceLog("dialog_log", "[executeDownloadAndUpload] 上传异常: " + e.getMessage());
                        Toast("上传异常: " + e.getMessage());
                        删除(savePath);
                    }
                }
            });
        }
    });
}

public void showGetCookieDialog(final Activity activity, final boolean isDark) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = tc(activity, "on_surface_strong");
                int subTextColor = tc(activity, "on_surface_sub");
                int accentColor = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
                int inputBgColor = tc(activity, "input_bg");
                int borderColor = adjustAlpha(textColor, 0.3f);
                ScrollView scrollView = new ScrollView(activity);
                LinearLayout root = makeDialogRoot(activity);
                TextView titleView = makeDialogTitle(activity, "Cookie & 请求工具", 8);
                root.addView(titleView);
                final boolean[]isCkMode = new boolean[] {
                    true
                };
                LinearLayout ckModeLayout = new LinearLayout(activity);
                ckModeLayout.setOrientation(LinearLayout.HORIZONTAL);
                ckModeLayout.setGravity(Gravity.CENTER_VERTICAL);
                ckModeLayout.setPadding(0, dp(activity, 8), 0, 0);
                final EditText domainEt = makeInput(activity, "域名 (如 qzone.qq.com)", null);
                domainEt.setText("qzone.qq.com");
                domainEt.setMaxLines(1);
                domainEt.setSingleLine(true);
                domainEt.setMinHeight(dp(activity, 48));
                domainEt.setTextSize(14);
                TextView getCkBtn = createButton(activity, "获取CK", accentColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                TextView postSwitchBtn = createButton(activity, "POST", accentColor, Color.TRANSPARENT, 15f, 0, 16, 12,
                    false, 0, 0, null);
                ckModeLayout.addView(domainEt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                ckModeLayout.addView(getCkBtn);
                ckModeLayout.addView(postSwitchBtn);
                root.addView(ckModeLayout);
                LinearLayout postModeLayout = new LinearLayout(activity);
                postModeLayout.setOrientation(LinearLayout.VERTICAL);
                postModeLayout.setVisibility(View.GONE);
                postModeLayout.setPadding(0, dp(activity, 8), 0, 0);
                LinearLayout postRow1 = new LinearLayout(activity);
                postRow1.setOrientation(LinearLayout.HORIZONTAL);
                postRow1.setGravity(Gravity.CENTER_VERTICAL);
                final EditText postUrlEt = makeInput(activity, "POST URL", null);
                postUrlEt.setMaxLines(1);
                postUrlEt.setSingleLine(true);
                postUrlEt.setMinHeight(dp(activity, 48));
                postUrlEt.setTextSize(14);
                TextView ckSwitchBtn = createButton(activity, "CK", accentColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                TextView sendPostBtn = createButton(activity, "发送", accentColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                postRow1.addView(postUrlEt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                postRow1.addView(ckSwitchBtn);
                postRow1.addView(sendPostBtn);
                postModeLayout.addView(postRow1);
                final EditText postDataEt = makeInput(activity, "POST 数据 (可为空)", null);
                postDataEt.setMinLines(3);
                postDataEt.setMaxLines(6);
                postDataEt.setMinHeight(dp(activity, 100));
                postDataEt.setTextSize(14);
                LinearLayout
                    .LayoutParams dataParams = new LinearLayout
                    .LayoutParams(LinearLayout .LayoutParams .MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                dataParams.topMargin = dp(activity, 12);
                postModeLayout.addView(postDataEt, dataParams);
                root.addView(postModeLayout);
                LinearLayout resultCard = new LinearLayout(activity);
                resultCard.setOrientation(LinearLayout.VERTICAL);
                resultCard.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
                LinearLayout resultHeader = new LinearLayout(activity);
                resultHeader.setOrientation(LinearLayout.HORIZONTAL);
                resultHeader.setGravity(Gravity.CENTER_VERTICAL);
                TextView resultTitleTv = new TextView(activity);
                resultTitleTv.setText("获取结果");
                resultTitleTv.setTextSize(16);
                resultTitleTv.setTextColor(textColor);
                resultTitleTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                final ImageView arrowIv = new ImageView(activity);
                final ArrowDrawable arrowD = new ArrowDrawable(textColor, false);
                arrowIv.setImageDrawable(arrowD);
                arrowIv.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 24), dp(activity, 24)));
                final TextView copyBtn = createButton(activity, "复制", accentColor, Color.TRANSPARENT, 14f, 0, 16, 0, false, 0, 0, null);
                resultHeader.addView(resultTitleTv);
                resultHeader.addView(arrowIv);
                resultHeader.addView(copyBtn);
                resultCard.addView(resultHeader);
                final LinearLayout resultBody = new LinearLayout(activity);
                resultBody.setOrientation(LinearLayout.VERTICAL);
                final ScrollView resultScroll = new ScrollView(activity);
                resultScroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                resultScroll.addView(resultBody);
                resultScroll.setVisibility(View.GONE);
                resultCard.addView(resultScroll);
                root.addView(resultCard);
                final String[]currentResultText = new String[1];
                resultHeader.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean show = resultScroll.getVisibility() == View.GONE;
                        resultScroll.setVisibility(show ? View.VISIBLE : View.GONE);
                        arrowD.setDirection(show);
                        arrowIv.setImageDrawable(arrowD);
                    }
                });
                copyBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (currentResultText[0] != null && !currentResultText[0].isEmpty()) {
                            showCopyConfirmDialog(activity, "结果", currentResultText[0], isDark);
                        } else {
                            Toast("暂无结果可复制");
                        }
                    }
                });
                final LinearLayout ckFormatBar = new LinearLayout(activity);
                ckFormatBar.setOrientation(LinearLayout.HORIZONTAL);
                ckFormatBar.setPadding(0, dp(activity, 12), 0, 0);
                final LinearLayout commonToolBar = new LinearLayout(activity);
                commonToolBar.setOrientation(LinearLayout.HORIZONTAL);
                commonToolBar.setPadding(0, dp(activity, 12), 0, 0);
                commonToolBar.setVisibility(View.GONE);
                class CKInfo {
                    String skey;
                    String pskey;
                    String bkn;
                    String domain;
                    String fullCookie;
                }
                final CKInfo[]ckInfo = new CKInfo[1];
                String[]ckFormats = {
                    "JSON", "Header", "Curl", "Fetch"
                };
                for (int i = 0; i < 4; i++) {
                    final int idx = i;
                    TextView btn = createButton(activity, ckFormats[i], subTextColor, Color.TRANSPARENT, 13f, 0, 12, 8, false, 0, 0, null);
                    btn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (ckInfo[0] == null) {
                                Toast("请先获取CK");
                                return;
                            }
                            String text = "";
                            if (idx == 0) text = "{\n  \"skey\": \"" + ckInfo[0]
                                .skey + "\",\n  \"p_skey\": \"" + ckInfo[0]
                                .pskey + "\",\n  \"bkn\": \"" + ckInfo[0]
                                .bkn + "\"\n}";
                            else if (idx == 1) text = "Cookie: " + ckInfo[0].fullCookie;
                            else if (idx == 2) text = "curl \"https://" + ckInfo[0]
                                .domain + "\" \\\n  -H \"Cookie: " + ckInfo[0]
                                .fullCookie + "\"";
                            else if (idx == 3) text = "fetch(\"https://" + ckInfo[0]
                                .domain + "\", { headers: { \"Cookie\": \"" + ckInfo[0]
                                .fullCookie + "\" } })";
                            resultBody.removeAllViews();
                            TextView tv = new TextView(activity);
                            tv.setTextSize(13);
                            tv.setTextColor(tc(activity, "on_surface_sub"));
                            tv.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
                            tv.setTextIsSelectable(true);
                            tv.setText(text);
                            resultBody.addView(tv);
                            currentResultText[0] = text;
                            resultScroll.setVisibility(View.VISIBLE);
                            arrowD.setDirection(true);
                            arrowIv.setImageDrawable(arrowD);
                        }
                    });
                    ckFormatBar.addView(btn);
                }
                String[]postTools = {
                    "复制URL", "复制数据"
                };
                for (int i = 0; i < postTools.length; i++) {
                    final String name = postTools[i];
                    TextView btn = createButton(activity, name, subTextColor, Color.TRANSPARENT, 13f, 0, 12, 8, false, 0, 0, null);
                    btn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            try {
                                if ("复制URL".equals(name)) {
                                    String url = postUrlEt.getText().toString();
                                    if (!url.isEmpty()) {
                                        copyToClipboard(activity, url);
                                        Toast("URL已复制");
                                    } else {
                                        Toast("URL为空");
                                    }
                                } else if ("复制数据".equals(name)) {
                                    String data = postDataEt.getText().toString();
                                    if (!data.isEmpty()) {
                                        copyToClipboard(activity, data);
                                        Toast("数据已复制");
                                    } else {
                                        Toast("数据为空");
                                    }
                                }
                            } catch (Throwable e) {
                                Toast("操作失败: " + e.getMessage());
                            }
                        }
                    });
                    commonToolBar.addView(btn);
                }
                root.addView(ckFormatBar);
                root.addView(commonToolBar);
                getCkBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            if (!isCkMode[0]) {
                                ckModeLayout.setVisibility(View.VISIBLE);
                                postModeLayout.setVisibility(View.GONE);
                                ckFormatBar.setVisibility(View.VISIBLE);
                                commonToolBar.setVisibility(View.GONE);
                                isCkMode[0] = true;
                            }
                            String domain = domainEt.getText().toString().trim();
                            if (domain.isEmpty()) {
                                Toast("请输入域名");
                                return;
                            }
                            final String uin = "o" + qq;
                            final String pUin = "o" + qq;
                            Object skeyObj = getSkey();
                            String skey = skeyObj != null ? String.valueOf(skeyObj) : "";
                            if (skey.isEmpty()) throw new Exception("skey为空");
                            Object pskeyObj = getPskey(domain);
                            String pskey = pskeyObj != null ? String.valueOf(pskeyObj) : "";
                            if (pskey.isEmpty()) throw new Exception("p_skey为空");
                            Object bknObj = getBkn(skey);
                            String bkn = bknObj != null ? String.valueOf(bknObj) : "";
                            if (bkn.isEmpty()) throw new Exception("bkn为空");
                            final String fullCookie = "p_skey=" + pskey + "; skey=" + skey + "; uin=" + uin + "; p_uin=" + pUin;
                            CKInfo info = new CKInfo();
                            info.skey = skey;
                            info.pskey = pskey;
                            info.bkn = bkn;
                            info.domain = domain;
                            info.fullCookie = fullCookie;
                            ckInfo[0] = info;
                            StringBuilder base = new StringBuilder();
                            base.append("域名: ").append(domain).append("\n");
                            base.append("uin: ").append(uin).append("\n");
                            base.append("p_uin: ").append(pUin).append("\n");
                            base.append("skey: ").append(skey).append("\n");
                            base.append("p_skey: ").append(pskey).append("\n");
                            base.append("bkn: ").append(bkn).append("\n\n");
                            base.append("完整Cookie:\n").append(fullCookie);
                            final String baseText = base.toString();
                            currentResultText[0] = baseText;
                            resultBody.removeAllViews();
                            TextView contentTv = new TextView(activity);
                            contentTv.setText(baseText);
                            contentTv.setTextSize(13);
                            contentTv.setTextColor(tc(activity, "on_surface_sub"));
                            contentTv.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
                            contentTv.setTextIsSelectable(true);
                            resultBody.addView(contentTv);
                            resultScroll.setVisibility(View.VISIBLE);
                            arrowD.setDirection(true);
                            arrowIv.setImageDrawable(arrowD);
                        } catch (Throwable e) {
                            Toast("获取CK失败: " + e.getMessage());
                        }
                    }
                });
                postSwitchBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (isCkMode[0]) {
                            ckModeLayout.setVisibility(View.GONE);
                            postModeLayout.setVisibility(View.VISIBLE);
                            ckFormatBar.setVisibility(View.GONE);
                            commonToolBar.setVisibility(View.VISIBLE);
                            isCkMode[0] = false;
                        }
                    }
                });
                ckSwitchBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ckModeLayout.setVisibility(View.VISIBLE);
                        postModeLayout.setVisibility(View.GONE);
                        ckFormatBar.setVisibility(View.VISIBLE);
                        commonToolBar.setVisibility(View.GONE);
                        isCkMode[0] = true;
                    }
                });
                sendPostBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!isCkMode[0]) {
                            String url = postUrlEt.getText().toString().trim();
                            String data = postDataEt.getText().toString().trim();
                            if (url.isEmpty()) {
                                Toast("请输入POST URL");
                                return;
                            }
                            if (!url.startsWith("http")) url = "https://" + url;
                            ThreadPool.execute(new Runnable() {
                                public void run() {
                                    try {
                                        Object skeyObj = getSkey();
                                        final String skey = skeyObj != null ? String.valueOf(skeyObj) : "";
                                        if (skey.isEmpty()) throw new Exception("skey为空");
                                        Object pskeyObj = getPskey(domainEt.getText().toString().trim());
                                        final String pskey = pskeyObj != null ? String.valueOf(pskeyObj) : "";
                                        if (pskey.isEmpty()) throw new Exception("p_skey为空");
                                        final String cookie = "p_skey=" + pskey + "; skey=" + skey + "; uin=o" + qq + " p_uin=o" + qq;
                                        final String result = httpPost(url, cookie, data);
                                        activity.runOnUiThread(new Runnable() {
                                            public void run() {
                                                try {
                                                    String display = "POST完整返回：\n\n" + (result .isEmpty() ? "(无返回内容)" : result);
                                                    currentResultText[0] = display;
                                                    resultBody.removeAllViews();
                                                    TextView tv = new TextView(activity);
                                                    tv.setText(display);
                                                    tv.setTextSize(13);
                                                    tv.setTextColor(tc(activity, "on_surface_sub"));
                                                    tv.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
                                                    tv.setTextIsSelectable(true);
                                                    resultBody.addView(tv);
                                                    resultScroll.setVisibility(View.VISIBLE);
                                                    arrowD.setDirection(true);
                                                    arrowIv.setImageDrawable(arrowD);
                                                } catch (Throwable uiErr) {
                                                    traceLog("dialog_log", "[httpPost] UI回填异常: " + uiErr);
                                                }
                                            }
                                        });
                                    } catch (Throwable e) {
                                        Toast("POST失败: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(scrollView);
                scrollView.addView(root);
                AlertDialog dialog = builder.create();
                dialog.show();
                applyUiTheme(activity, dialog, 0);
            } catch (Throwable e) {
                Toast("弹窗创建失败: " + e.getMessage());
            }
        }
    });
}
class AudioBtnAdder {
    View createAudioPlayer(Activity activity,
        final MediaPlayer[]player,
        int textColor,
        int inputBgColor,
        int borderColor,
        final Handler uiHandler,
        final Runnable[]updateProgressTaskRef,
        final String audioUrl) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        final SeekBar seekBar = new SeekBar(activity);
        applyUiSeekBar(seekBar, activity, tc(activity, "primary"));
        seekBar.setMax(1000);
        container.addView(seekBar);
        LinearLayout timeLayout = new LinearLayout(activity);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.setPadding(0, dp(activity, 4), 0, 0);
        final TextView currentTime = new TextView(activity);
        currentTime.setText("00:00");
        currentTime.setTextColor(textColor);
        currentTime.setTextSize(12);
        final TextView totalTime = new TextView(activity);
        totalTime.setText("00:00");
        totalTime.setTextColor(textColor);
        totalTime.setTextSize(12);
        View spacer = new View(activity);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        timeLayout.addView(currentTime);
        timeLayout.addView(spacer);
        timeLayout.addView(totalTime);
        container.addView(timeLayout);
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(activity, 8), 0, 0);
        TextView playBtn = createButton(activity, "播放", textColor, inputBgColor, 14f, 8, 8, 8, false, 1, borderColor, null);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
        playBtn.setLayoutParams(p);
        btnRow.addView(playBtn);
        container.addView(btnRow);
        final Runnable updateProgress = new Runnable() {
            public void run() {
                if (player[0] != null && player[0].isPlaying()) {
                    int current = player[0].getCurrentPosition();
                    int duration = player[0].getDuration();
                    if (duration > 0) {
                        int progress = (int)((long) current * 1000 / duration);
                        seekBar.setProgress(progress);
                        currentTime.setText(formatTime(current));
                        totalTime.setText(formatTime(duration));
                    }
                    uiHandler.postDelayed(this, 200);
                } else {
                    uiHandler.removeCallbacks(this);
                }
            }
        };
        if (updateProgressTaskRef != null) {
            updateProgressTaskRef[0] = updateProgress;
        }
        playBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (player[0] != null && player[0].isPlaying()) {
                    player[0].pause();
                    playBtn.setText("播放");
                    uiHandler.removeCallbacks(updateProgress);
                } else {
                    if (player[0] == null) {
                        try {
                            player[0] = new MediaPlayer();
                            player[0].setDataSource(audioUrl);
                            player[0].prepareAsync();
                            player[0].setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer mp) {
                                    mp.start();
                                    playBtn.setText("暂停");
                                    totalTime.setText(formatTime(mp.getDuration()));
                                    uiHandler.post(updateProgress);
                                }
                            });
                            player[0].setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                public boolean onError(MediaPlayer mp, int what, int extra) {
                                    Toast("播放失败，可能不是音频");
                                    mp.release();
                                    player[0] = null;
                                    playBtn.setText("播放");
                                    return true;
                                }
                            });
                            player[0].setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                public void onCompletion(MediaPlayer mp) {
                                    playBtn.setText("播放");
                                    seekBar.setProgress(0);
                                    currentTime.setText("00:00");
                                    uiHandler.removeCallbacks(updateProgress);
                                }
                            });
                        } catch (Throwable t) {
                            Toast("播放器创建失败");
                        }
                    } else {
                        player[0].start();
                        playBtn.setText("暂停");
                        uiHandler.post(updateProgress);
                    }
                }
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser = false;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.fromUser = fromUser;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                uiHandler.removeCallbacks(updateProgress);
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player[0] != null && fromUser) {
                    int duration = player[0].getDuration();
                    if (duration > 0) {
                        int seekTo = (int)((long) seekBar.getProgress() * duration / 1000);
                        player[0].seekTo(seekTo);
                        currentTime.setText(formatTime(seekTo));
                    }
                    if (player[0].isPlaying()) {
                        uiHandler.post(updateProgress);
                    }
                }
            }
        });
        return container;
    }
    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return(min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec;
    }
}

public void showExtractAudioDialog(Activity activity, Object data) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String textContent = "";
                if (data != null) {
                    try {
                        textContent = String.valueOf(data.msg);
                    } catch (Throwable t) {
                        textContent = String.valueOf(data);
                    }
                }
                final List urls = new ArrayList();
                java
                    .util
                    .regex
                    .Matcher m = java
                    .util
                    .regex
                    .Pattern
                    .compile("https?://[^\\s\"']+\\.(mp3|m4a|aac|ogg|wav|flac|opus|amr|wma|mpeg)(\\?[^\\s]*)?",
                    Pattern.CASE_INSENSITIVE).matcher(textContent);
                while (m.find()) {
                    String u = m.group().replace("\\/", "/");
                    urls.add(u);
                }
                if (urls.isEmpty()) {
                    m = java.util.regex.Pattern.compile("https?://[^\\s\"']+(audio|music|fm)[^\\s\"']*",
                        Pattern.CASE_INSENSITIVE).matcher(textContent);
                    while (m.find()) {
                        String u = m.group().replace("\\/", "/");
                        urls.add(u);
                    }
                }
                final String finalUrl = urls.isEmpty() ? "" : (String) urls.get(0);
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            boolean isDark = isThemeDark(activity);
                            int textColor = tc(activity, "on_surface_strong");
                            int subTextColor = tc(activity, "on_surface_sub");
                            int inputBgColor = tc(activity, "input_bg");
                            int borderColor = adjustAlpha(textColor, 0.3f);
                            LinearLayout root = makeDialogRoot(activity);
                            TextView titleView = makeDialogTitle(activity, "提取音频", 12);
                            root.addView(titleView);
                            TextView urlView = new TextView(activity);
                            urlView.setText(finalUrl.isEmpty() ? "未检测到音频链接" : finalUrl);
                            urlView.setTextSize(13);
                            urlView.setTextColor(textColor);
                            urlView.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                            GradientDrawable bg = makeDialogInnerCard(activity, inputBgColor);
                            urlView.setBackground(bg);
                            root.addView(urlView);
                            final MediaPlayer[]player = new MediaPlayer[1];
                            final Handler uiHandler = new Handler(Looper.getMainLooper());
                            final Runnable[]updateProgressTask = new Runnable[1];
                            AudioBtnAdder adder = new AudioBtnAdder();
                            final View audioControlView;
                            if (!finalUrl.isEmpty()) {
                                audioControlView = adder.createAudioPlayer(activity,
                                    player,
                                    textColor,
                                    inputBgColor,
                                    borderColor,
                                    uiHandler,
                                    updateProgressTask,
                                    finalUrl);
                            } else {
                                audioControlView = new View(activity);
                            }
                            LinearLayout btnGrid = new LinearLayout(activity);
                            btnGrid.setOrientation(LinearLayout.VERTICAL);
                            btnGrid.setPadding(0, dp(activity, 16), 0, 0);
                            if (!finalUrl.isEmpty()) {
                                btnGrid.addView(audioControlView);
                            }
                            LinearLayout actionRow = new LinearLayout(activity);
                            actionRow.setOrientation(LinearLayout.HORIZONTAL);
                            actionRow.setPadding(0, dp(activity, 12), 0, 0);
                            if (!finalUrl.isEmpty()) {
                                TextView sendLinkBtn = createButton(activity, "发链接", textColor, inputBgColor, 14f, 8, 8, 8, false, 1, borderColor, new Runnable() {
                                    public void run() {
                                        try {
                                            Object contact = data.contact;
                                            sendMsg(contact, finalUrl);
                                            Toast("链接已发送");
                                        } catch (Throwable t) {
                                            Toast("发送失败");
                                        }
                                    }
                                });
                                LinearLayout.LayoutParams linkBtnLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                                linkBtnLp.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
                                actionRow.addView(sendLinkBtn, linkBtnLp);
                                TextView sendVoiceBtn = createButton(activity, "发语音", textColor, inputBgColor, 14f, 8, 8, 8, false, 1, borderColor, new Runnable() {
                                    public void run() {
                                        Toast("正在下载并发送语音...");
                                        ThreadPool.execute(new Runnable() {
                                            public void run() {
                                                try {
                                                    String savePath = pluginPath + "/cache/audio_" + System
                                                        .currentTimeMillis() + ".mp3";
                                                    FutureTask downloadTask = new FutureTask(new Callable() {
                                                        public Boolean call() throws Exception {
                                                            return downloadFile(finalUrl, savePath, new ProgressCallback() {
                                                                public void onProgress(int progressVal) {
                                                                }
                                                                public void onProgressTip(String tip) {
                                                                }
                                                            });
                                                        }
                                                    });
                                                    ThreadPool.execute(downloadTask);
                                                    boolean success = (Boolean) downloadTask.get(15000, TimeUnit.MILLISECONDS);
                                                    if (success) {
                                                        Object contact = data.contact;
                                                        sendPtt(contact, savePath);
                                                        Toast("语音发送成功");
                                                    } else {
                                                        Toast("下载失败");
                                                    }
                                                } catch (TimeoutException e) {
                                                    Toast("下载超时");
                                                } catch (Throwable t) {
                                                    Toast("语音发送失败");
                                                }
                                            }
                                        });
                                    }
                                });
                                LinearLayout.LayoutParams voiceBtnLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                                voiceBtnLp.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
                                actionRow.addView(sendVoiceBtn, voiceBtnLp);
                            }
                            btnGrid.addView(actionRow);
                            root.addView(btnGrid);
                            final AlertDialog[]ref = new AlertDialog[1];
                            TextView closeBtn = createButton(activity, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                            closeBtn.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    if (player[0] != null) {
                                        try {
                                            player[0].stop();
                                            player[0].release();
                                        } catch (Throwable t) {
                                            traceLog("dialog_log", "[downloadFile] 异常: " + t);
                                        }
                                        player[0] = null;
                                    }
                                    if (updateProgressTask[0] != null) {
                                        uiHandler.removeCallbacks(updateProgressTask[0]);
                                    }
                                    if (ref[0] != null) ref[0].dismiss();
                                }
                            });
                            root.addView(closeBtn);
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                                isDark ? AlertDialog
                                    .THEME_DEVICE_DEFAULT_DARK : AlertDialog
                                    .THEME_DEVICE_DEFAULT_LIGHT);
                            builder.setView(root);
                            ref[0] = builder.create();
                            ref[0].setOnDismissListener(new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface d) {
                                    if (player[0] != null) {
                                        try {
                                            player[0].stop();
                                            player[0].release();
                                        } catch (Throwable t) {
                                            traceLog("dialog_log", "[onClick] 异常: " + t);
                                        }
                                        player[0] = null;
                                    }
                                    if (updateProgressTask[0] != null) {
                                        uiHandler.removeCallbacks(updateProgressTask[0]);
                                    }
                                }
                            });
                            ref[0].show();
                            applyUiTheme(activity, ref[0], 0);
                        } catch (Throwable t) {
                            traceLog("dialog_log", "[onClick] 异常: " + t);
                        }
                    }
                });
            } catch (Throwable e) {
                Toast("提取音频异常");
            }
        }
    });
}

LinearLayout createInfoRow(Activity activity,
    String label,
    String value,
    int colorText,
    int colorSubtext,
    boolean isDark) {
    LinearLayout row = new LinearLayout(activity);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setPadding(dp(activity, 4), dp(activity, 7), dp(activity, 4), dp(activity, 7));
    row.setClickable(true);
    int normalColor = getAdaptiveMenuItemBg(activity);
    int pressedColor = getAdaptivePressedColor(normalColor);
    final GradientDrawable rowBg = new GradientDrawable();
    rowBg.setCornerRadius(dp(activity, 8));
    rowBg.setColor(normalColor);
    row.setBackground(rowBg);
    final TextView valueView = new TextView(activity);
    valueView.setText(value);
    valueView.setTextSize(13);
    valueView.setTextColor(colorText);
    valueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));
    final boolean[] pressed = new boolean[] { false };
    row.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressed[0] = true;
                    rowBg.setColor(pressedColor);
                    row.setBackground(rowBg);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (pressed[0] && (event.getX() < 0 || event.getY() < 0 || event.getX() > row.getWidth() || event.getY() > row.getHeight())) {
                        pressed[0] = false;
                        rowBg.setColor(normalColor);
                        row.setBackground(rowBg);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    boolean click = pressed[0];
                    pressed[0] = false;
                    rowBg.setColor(normalColor);
                    row.setBackground(rowBg);
                    if (click) v.performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    pressed[0] = false;
                    rowBg.setColor(normalColor);
                    row.setBackground(rowBg);
                    return true;
            }
            return false;
        }
    });
    row.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String currentVal = valueView.getText().toString();
            showCopyConfirmDialog(activity, label, currentVal, isDark);
        }
    });
    TextView labelView = new TextView(activity);
    labelView.setText(label);
    labelView.setTextSize(12);
    labelView.setTextColor(colorSubtext);
    labelView.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 68), LinearLayout.LayoutParams.WRAP_CONTENT));
    row.addView(labelView);
    row.addView(valueView);
    return row;
}

FrameLayout createMemberInfoCard(Activity activity,
    String userUin,
    String peerUin,
    int chatType,
    boolean isDark,
    int colorText,
    int colorSubtext,
    String msgText,
    String fullMsgText,
    Object msgRecord,
    String nickName) {
    FrameLayout cardWrapper = new FrameLayout(activity);
    cardWrapper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));
    cardWrapper.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 8));
    int cardBackgroundColor = getAdaptiveMenuItemBg(activity);
    GradientDrawable cardBg = makeDialogInnerCard(activity, cardBackgroundColor);
    LinearLayout cardContent = new LinearLayout(activity);
    cardContent.setOrientation(LinearLayout.VERTICAL);
    cardContent.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
    cardContent.setBackground(cardBg);
    LinearLayout nameRow = new LinearLayout(activity);
    nameRow.setOrientation(LinearLayout.HORIZONTAL);
    nameRow.setGravity(Gravity.CENTER_VERTICAL);
    final TextView nameView = new TextView(activity);
    nameView.setText(nickName != null && !nickName.isEmpty() ? nickName : "加载中...");
    nameView.setTextSize(18);
    nameView.setTextColor(colorText);
    nameView.setTypeface(null, Typeface.BOLD);
    FrameLayout expandBtn = new FrameLayout(activity);
    expandBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 32), dp(activity, 32)));
    expandBtn.setClickable(true);
    ImageView expandIcon = new ImageView(activity);
    FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(activity, 18), dp(activity, 18));
    iconParams.gravity = Gravity.CENTER;
    expandIcon.setLayoutParams(iconParams);
    final ArrowDrawable arrowDrawable = new ArrowDrawable(colorSubtext, false);
    expandIcon.setImageDrawable(arrowDrawable);
    expandBtn.addView(expandIcon);
    LinearLayout spacer = new LinearLayout(activity);
    spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1.0f));
    nameRow.addView(nameView);
    nameRow.addView(spacer);
    nameRow.addView(expandBtn);
    LinearLayout qqRow = createInfoRow(activity, "QQ号", userUin, colorText, colorSubtext, isDark);
    cardContent.addView(nameRow);
    cardContent.addView(qqRow);
    View divider = new View(activity);
    LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        dp(activity, 1));
    divParams.setMargins(0, dp(activity, 8), 0, dp(activity, 4));
    divider.setLayoutParams(divParams);
    divider.setBackgroundColor(isDark ? pc("#22FFFFFF") : pc("#15000000"));
    divider.setVisibility(View.GONE);
    cardContent.addView(divider);
    final LinearLayout detailInfo = new LinearLayout(activity);
    detailInfo.setOrientation(LinearLayout.VERTICAL);
    detailInfo.setVisibility(View.GONE);
    detailInfo.setPadding(0, dp(activity, 2), 0, 0);
    final LinearLayout groupRow = createInfoRow(activity,
        chatType == 2 ? "群昵称" : "备注",
        "加载中...",
        colorText,
        colorSubtext,
        isDark);
    String msgDisplay = (msgText != null && !msgText.trim().isEmpty()) ? (msgText.length() > 100 ? msgText.substring(0,
        100) + "..." : msgText) : "(无文本内容)";
    final String finalFullMsgText = fullMsgText != null ? fullMsgText : "(无文本内容)";
    LinearLayout msgRow = new LinearLayout(activity);
    msgRow.setOrientation(LinearLayout.HORIZONTAL);
    msgRow.setPadding(dp(activity, 4), dp(activity, 7), dp(activity, 4), dp(activity, 7));
    msgRow.setClickable(true);
    int msgRowNormalColor = getAdaptiveMenuItemBg(activity);
    int msgRowPressedColor = getAdaptivePressedColor(msgRowNormalColor);
    final GradientDrawable msgRowBg = new GradientDrawable();
    msgRowBg.setCornerRadius(dp(activity, 8));
    msgRowBg.setColor(msgRowNormalColor);
    msgRow.setBackground(msgRowBg);
    TextView msgLabelView = new TextView(activity);
    msgLabelView.setText("消息内容");
    msgLabelView.setTextSize(12);
    msgLabelView.setTextColor(colorSubtext);
    msgLabelView.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 68), LinearLayout.LayoutParams.WRAP_CONTENT));
    TextView msgValueView = new TextView(activity);
    msgValueView.setText(msgDisplay);
    msgValueView.setTextSize(13);
    msgValueView.setTextColor(colorText);
    msgValueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));
    msgRow.addView(msgLabelView);
    msgRow.addView(msgValueView);
    final boolean[] msgRowPressed = new boolean[] { false };
    msgRow.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    msgRowPressed[0] = true;
                    msgRowBg.setColor(msgRowPressedColor);
                    msgRow.setBackground(msgRowBg);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (msgRowPressed[0] && (event.getX() < 0 || event.getY() < 0 || event.getX() > msgRow.getWidth() || event.getY() > msgRow.getHeight())) {
                        msgRowPressed[0] = false;
                        msgRowBg.setColor(msgRowNormalColor);
                        msgRow.setBackground(msgRowBg);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    boolean click = msgRowPressed[0];
                    msgRowPressed[0] = false;
                    msgRowBg.setColor(msgRowNormalColor);
                    msgRow.setBackground(msgRowBg);
                    if (click) v.performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    msgRowPressed[0] = false;
                    msgRowBg.setColor(msgRowNormalColor);
                    msgRow.setBackground(msgRowBg);
                    return true;
            }
            return false;
        }
    });
    msgRow.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            showCopyConfirmDialog(activity, "消息内容", finalFullMsgText, isDark);
        }
    });
    detailInfo.addView(groupRow);
    detailInfo.addView(msgRow);
    LinearLayout groupNameRow = null;
    if (chatType == 2) {
        LinearLayout groupUinRow = createInfoRow(activity, "群号", peerUin, colorText, colorSubtext, isDark);
        groupNameRow = createInfoRow(activity, "群名", "加载中...", colorText, colorSubtext, isDark);
        detailInfo.addView(groupUinRow);
        detailInfo.addView(groupNameRow);
    }
    final LinearLayout finalGroupNameRow = groupNameRow;
    cardContent.addView(detailInfo);
    final boolean[]isExpanded = new boolean[] {
        false
    };
    final boolean[]isLoading = new boolean[] {
        false
    };
    if (nickName == null || nickName.isEmpty()) {
        ThreadPool.execute(new Runnable() {
            public void run() {
                String nick = userUin;
                try {
                    Object card = GetCard(userUin);
                    if (card != null) {
                        String strNick = card?.strNick ?: "";
                        if (!strNick.isEmpty() && !strNick.contains("*") && !"null".equals(strNick)) nick = strNick;
                    }
                } catch (Throwable ignored) {
                    traceLog("dialog_log", "[onClick] 异常: " + ignored);
                }
                if (nick.equals(userUin) && msgRecord != null) {
                    try {
                        String fb = String.valueOf(msgRecord.sendNickName);
                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) nick = fb;
                    } catch (Throwable ignored) {
                        traceLog("dialog_log", "[onClick] 异常: " + ignored);
                    }
                }
                final String finalNick = nick;
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            nameView.setText(finalNick);
                        } catch (Throwable t) {
                            traceLog("dialog_log", "[createMemberInfoCard] 异常: " + t);
                        }
                    }
                });
            }
        });
    }
    expandBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            isExpanded[0] = !isExpanded[0];
            expandIcon.animate().rotation(isExpanded[0] ? 90f : 0f).setDuration(200).start();
            divider.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
            detailInfo.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
            if (isExpanded[0] && !isLoading[0]) {
                isLoading[0] = true;
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            if (chatType == 2) {
                                String groupNick = "未设置";
                                try {
                                    Object member = getMemberInfo(peerUin, userUin);
                                    if (member != null) {
                                        String mn = String.valueOf(member.uinName);
                                        if (mn != null && !mn .isEmpty() && !mn .contains("*") && !mn .equals("null")) groupNick = mn;
                                    }
                                } catch (Throwable ignored) {
                                    traceLog("dialog_log", "[onClick] 异常: " + ignored);
                                }
                                if ("未设置".equals(groupNick) && msgRecord != null) {
                                    try {
                                        String fb = String.valueOf(msgRecord.sendMemberName);
                                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) groupNick = fb;
                                    } catch (Throwable ignored) {
                                        traceLog("dialog_log", "[onClick] 异常: " + ignored);
                                    }
                                }
                                final String finalGroupNick = groupNick;
                                String gName = "获取失败";
                                try {
                                    Object info = findTroopInfo(peerUin);
                                    if (info != null) {
                                        String tn = String.valueOf(info.troopname);
                                        if (tn != null && !tn.equals("null")) gName = tn;
                                    }
                                } catch (Throwable ignored) {
                                    traceLog("dialog_log", "[onClick] 异常: " + ignored);
                                }
                                final String finalGroupName = gName;
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            ((TextView) groupRow.getChildAt(1)).setText(finalGroupNick);
                                        } catch (Throwable ignored) {
                                            traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored);
                                        }
                                        if (finalGroupNameRow != null) {
                                            try {
                                                ((TextView) finalGroupNameRow.getChildAt(1)).setText(finalGroupName);
                                            } catch (Throwable ignored) {
                                                traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored);
                                            }
                                        }
                                        isLoading[0] = false;
                                    }
                                });
                            } else {
                                String remark = "未设置";
                                try {
                                    Object f = GetCard(userUin);
                                    if (f != null) {
                                        String rm = String.valueOf(f.remark);
                                        if (rm != null && !rm .isEmpty() && !rm .contains("*") && !rm .equals("null")) remark = rm;
                                    }
                                } catch (Throwable ignored) {
                                    traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored);
                                }
                                if ("未设置".equals(remark) && msgRecord != null) {
                                    try {
                                        String fb = String.valueOf(msgRecord.sendRemarkName);
                                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) remark = fb;
                                    } catch (Throwable ignored) {
                                        traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored);
                                    }
                                }
                                final String finalRemark = remark;
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            ((TextView) groupRow.getChildAt(1)).setText(finalRemark);
                                        } catch (Throwable ignored) {
                                            traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored);
                                        }
                                        isLoading[0] = false;
                                    }
                                });
                            }
                        } catch (Throwable e) {
                            activity.runOnUiThread(new Runnable() { public void run() { isLoading[0] = false; } });
                        }
                    }
                });
            }
        }
    });
    cardWrapper.addView(cardContent);
    return cardWrapper;
}

FrameLayout createMenuItem(Activity activity,
    String title,
    boolean isDark,
    int colorPrimary,
    int colorContainer,
    Dialog parentDialog,
    Runnable callback,
    boolean skipDismiss) {
    FrameLayout itemWrapper = new FrameLayout(activity);
    itemWrapper.setLayoutParams(new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f),
        GridLayout.spec(GridLayout.UNDEFINED, 1f)));
    itemWrapper.setPadding(dp(activity, 7), dp(activity, 6), dp(activity, 7), dp(activity, 6));
    itemWrapper.setClipToPadding(false);
    itemWrapper.setClipChildren(false);
    itemWrapper.setElevation(dp(activity, 3.5f));

    int backgroundColor = pc(getSettingsThemeColor(activity, "background"));
    if (Color.alpha(backgroundColor) < 255) {
        backgroundColor = getAdaptiveMenuItemBg(activity);
    }
    final int normalColor = isDark
        ? blendColor(backgroundColor, Color.WHITE, 0.065f)
        : blendColor(backgroundColor, Color.BLACK, 0.038f);
    final int pressedColor = isDark
        ? blendColor(normalColor, Color.WHITE, 0.055f)
        : blendColor(normalColor, Color.BLACK, 0.065f);
    final GradientDrawable itemBg = new GradientDrawable();
    itemBg.setShape(GradientDrawable.RECTANGLE);
    itemBg.setCornerRadius(dp(activity, getUiCornerDp()));
    itemBg.setColor(normalColor);

    LinearLayout itemContent = new LinearLayout(activity);
    itemContent.setOrientation(LinearLayout.HORIZONTAL);
    itemContent.setGravity(Gravity.CENTER);
    itemContent.setMinimumHeight(dp(activity, 54));
    itemContent.setPadding(dp(activity, 12), dp(activity, 5), dp(activity, 12), dp(activity, 5));
    itemContent.setBackground(itemBg);
    itemContent.setElevation(dp(activity, 4.5f));

    final TextView titleView = new TextView(activity);
    titleView.setText(title);
    titleView.setTextSize(14);
    titleView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    titleView.setGravity(Gravity.CENTER);
    titleView.setMaxLines(2);
    titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
    titleView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    itemContent.addView(titleView);

    itemWrapper.addView(itemContent);
    final boolean[] itemPressed = new boolean[] { false };
    itemWrapper.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    itemPressed[0] = true;
                    itemBg.setColor(pressedColor);
                    itemContent.setBackground(itemBg);
                    itemContent.animate().scaleX(0.992f).scaleY(0.992f).setDuration(70).start();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (itemPressed[0] && (event.getX() < 0 || event.getY() < 0 || event.getX() > itemWrapper.getWidth() || event.getY() > itemWrapper.getHeight())) {
                        itemPressed[0] = false;
                        itemBg.setColor(normalColor);
                        itemContent.setBackground(itemBg);
                        itemContent.animate().scaleX(1f).scaleY(1f).setDuration(70).start();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    boolean click = itemPressed[0];
                    itemPressed[0] = false;
                    itemBg.setColor(normalColor);
                    itemContent.setBackground(itemBg);
                    itemContent.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    if (click) v.performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    itemPressed[0] = false;
                    itemBg.setColor(normalColor);
                    itemContent.setBackground(itemBg);
                    itemContent.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    return true;
            }
            return false;
        }
    });
    itemWrapper.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (!skipDismiss && parentDialog != null && parentDialog.isShowing()) parentDialog.dismiss();
            if (callback != null) callback.run();
        }
    });
    return itemWrapper;
}
TextView createCategoryTitle(Activity activity, String category, int colorPrimary) {
    TextView tv = new TextView(activity);
    tv.setText(category);
    tv.setTextSize(14);
    tv.setTextColor(colorPrimary);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setPadding(dp(activity, 8), dp(activity, 12), dp(activity, 8), dp(activity, 8));
    return tv;
}
String[][]CATEGORY_COLOR_STRS = new String[6][3];
CATEGORY_COLOR_STRS[0][0] = "#26384A";
CATEGORY_COLOR_STRS[0][1] = "#D7E4F3";
CATEGORY_COLOR_STRS[0][2] = "#A7B5C7";
CATEGORY_COLOR_STRS[1][0] = "#304634";
CATEGORY_COLOR_STRS[1][1] = "#D9E9DC";
CATEGORY_COLOR_STRS[1][2] = "#A9B9AB";
CATEGORY_COLOR_STRS[2][0] = "#48365E";
CATEGORY_COLOR_STRS[2][1] = "#E5D9F2";
CATEGORY_COLOR_STRS[2][2] = "#B4A9C0";
CATEGORY_COLOR_STRS[3][0] = "#563E24";
CATEGORY_COLOR_STRS[3][1] = "#F0DFCA";
CATEGORY_COLOR_STRS[3][2] = "#C4B69E";
CATEGORY_COLOR_STRS[4][0] = "#2D5558";
CATEGORY_COLOR_STRS[4][1] = "#D8ECEE";
CATEGORY_COLOR_STRS[4][2] = "#A6B9BB";
CATEGORY_COLOR_STRS[5][0] = "#30353C";
CATEGORY_COLOR_STRS[5][1] = "#E0E3E8";
CATEGORY_COLOR_STRS[5][2] = "#A6A9AF";

void addMenuItem(List list, String category, String title, Runnable callback) {
    list.add(new Object[] { category, title, callback });
}
int[]getCategoryColorInts(String category, boolean isDark) {
    int index = 5;
    if ("消息操作".equals(category)) index = 0;
    else if ("群管理".equals(category) || "好友信息".equals(category)) index = 1;
    else if ("互动功能".equals(category)) index = 2;
    else if ("工具".equals(category)) index = 3;
    else if ("其他".equals(category)) index = 4;
    else if ("设置".equals(category)) index = 5;
    String[]colorStrs = CATEGORY_COLOR_STRS[index];
    int titleColor = pc(isDark ? colorStrs[1] : colorStrs[0]);
    return new int[] {
        titleColor, titleColor
    };
}
class ArrowDrawable extends Drawable {
    private Paint paint;
    private Path path;
    private boolean isUp;
    ArrowDrawable(int color, boolean isUp) {
        this.isUp = isUp;
        this.paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
        this.path = new Path();
    }
    void setColor(int color) {
        paint.setColor(color);
        invalidateSelf();
    }
    private void rebuildPath() {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        if (w <= 0 || h <= 0) return;
        float sw = Math.min(w, h) / 6f;
        paint.setStrokeWidth(sw);
        float cx = w / 2f;
        path.reset();
        if (isUp) {
            path.moveTo(sw, h * 0.65f);
            path.lineTo(cx, h * 0.35f);
            path.lineTo(w - sw, h * 0.65f);
        } else {
            path.moveTo(sw, h * 0.35f);
            path.lineTo(cx, h * 0.65f);
            path.lineTo(w - sw, h * 0.35f);
        }
    }
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        rebuildPath();
    }
    public void draw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

void applySavedSortOrder(List catOrder, Map catItems, String savedOrder) {
    if (savedOrder == null || savedOrder.isEmpty()) return;
    try {
        List newCatOrder = new ArrayList();
        Map newCatItems = new LinkedHashMap();
        String[]cats = savedOrder.split("\\|");
        for (int i = 0; i < cats.length; i++) {
            String cStr = cats[i];
            int eqIdx = cStr.indexOf("=");
            String catName = eqIdx >= 0 ? cStr.substring(0, eqIdx) : cStr;
            if (!catOrder.contains(catName)) continue;
            newCatOrder.add(catName);
            List origItems = (List) catItems.get(catName);
            if (origItems == null) continue;
            if (eqIdx >= 0 && cStr.length() > eqIdx + 1) {
                String itemsPart = cStr.substring(eqIdx + 1);
                String[]itemNames = itemsPart.split(",");
                List sortedItems = new ArrayList();
                for (int j = 0; j < itemNames.length; j++) {
                    String targetName = itemNames[j];
                    for (int k = 0; k < origItems.size(); k++) {
                        Object[]item = (Object[]) origItems.get(k);
                        if (targetName.equals(item[1])) {
                            sortedItems.add(item);
                            break;
                        }
                    }
                }
                for (int k = 0; k < origItems.size(); k++) {
                    Object[]item = (Object[]) origItems.get(k);
                    boolean found = false;
                    for (int m = 0; m < sortedItems.size(); m++) {
                        Object[]si = (Object[]) sortedItems.get(m);
                        if (si[1].equals(item[1])) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) sortedItems.add(item);
                }
                newCatItems.put(catName, sortedItems);
            } else {
                newCatItems.put(catName, new ArrayList(origItems));
            }
        }
        for (int i = 0; i < catOrder.size(); i++) {
            String catName = (String) catOrder.get(i);
            if (!newCatOrder.contains(catName)) {
                newCatOrder.add(catName);
                newCatItems.put(catName, catItems.get(catName));
            }
        }
        catOrder.clear();
        catOrder.addAll(newCatOrder);
        catItems.clear();
        catItems.putAll(newCatItems);
    } catch (Throwable t) {
        traceLog("dialog_log", "[applySavedSortOrder]排序应用失败: " + t.getMessage());
    }
}

String buildSortOrderString(List catOrder, Map catItems) {
    try {
        return((List) catOrder).stream().map(new java.util.function.Function() {
            public Object apply(Object cat) {
                List items = (List) catItems.get(cat);
                String itemNames = ((List) items).stream().map(new java.util.function.Function() {
                    public Object apply(Object item) {
                        return(String)((Object[]) item)[1];
                    }
                }).collect(Collectors.joining(","));
                return(String) cat + "=" + itemNames;
            }
        }).collect(Collectors.joining("|"));
    } catch (Throwable t) {
        return "";
    }
}

public void 长按消息菜单(Activity activity, Object data) {
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);
    String quntext = "";
    String peerUin = "";
    String userUin = "";
    int msgtype = 0;
    long msgid = 0;
    int chatType = 0;
    Object msgRecord = null;
    Object atList = null;
    String nickName = "";
    try {
        quntext = String.valueOf(data.msg);
        peerUin = String.valueOf(data.peerUin);
        userUin = String.valueOf(data.userUin);
        msgtype = (int) data.msgType;
        msgid = (long) data.msgId;
        chatType = (int) data.type;
        msgRecord = data.data;
        atList = data.atList;
        nickName = String.valueOf(msgRecord.sendNickName);
    } catch (Throwable t) {
        traceLog("dialog_log", "[长按消息菜单] 异常: " + t);
    }
    if (nickName == null) {
        nickName = userUin;
    }
    int colorText = tc(activity, "on_surface");
    int colorSubtext = tc(activity, "on_surface_variant");
    int sheetBg = getAdaptiveSheetBg(activity);
    final Dialog bottomSheet = new Dialog(activity);
    bottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout rootLayout = new LinearLayout(activity);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setBackgroundColor(sheetBg);
    FrameLayout headerCard = createMemberInfoCard(activity,
        userUin,
        peerUin,
        chatType,
        isDark,
        colorText,
        colorSubtext,
        quntext,
        quntext,
        msgRecord,
        nickName);
    rootLayout.addView(headerCard);
    ScrollView scrollView = new ScrollView(activity);
    scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 380)));
    scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    scrollView.setVerticalScrollBarEnabled(false);
    scrollView.setClipChildren(true);
    scrollView.setClipToPadding(true);
    final LinearLayout menuContainer = new LinearLayout(activity);
    menuContainer.setOrientation(LinearLayout.VERTICAL);
    menuContainer.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 20));
    menuContainer.setClipChildren(false);
    List menuItems = new ArrayList();
    final String finalQuntext = quntext;
    final int finalChatType = chatType;
    final String finalPeerUin = peerUin;
    final long finalMsgid = msgid;
    final Object finalMsgRecord = msgRecord;
    final String finalUserUin = userUin;
    final Object finalAtList = atList;
    final int finalMsgtype = msgtype;
    final String finalNickName = nickName;
    String roleTmp = "MEMBER";
    try {
        Object qqmember = getMemberInfo(finalPeerUin, qq);
        if (qqmember != null && qqmember.role != null) roleTmp = qqmember.role;
    } catch (Throwable t) {
        traceLog("dialog_log", "[长按菜单] 获取成员角色失败: " + t);
    }
    final String role = roleTmp;
    addMenuItem(menuItems, "消息操作", "复制内容", new Runnable() {
        public void run() {
            showCopyConfirmDialog(activity, "消息内容", finalQuntext, isDark);
        }
    });
    addMenuItem(menuItems, "消息操作", "复读加一", new Runnable() { public void run() { 复读(data, null); } });
    addMenuItem(menuItems, "消息操作", "撤回消息", new Runnable() {
        public void run() {
            recallMsg(finalChatType, finalPeerUin, finalMsgid);
            qqToast(2, "撤回操作已执行");
        }
    });
    addMenuItem(menuItems, "消息操作", "原始消息", new Runnable() { public void run() { showRawMessageDialog(finalMsgRecord); } });
    addMenuItem(menuItems, "消息操作", "解析消息", new Runnable() {
        public void run() {
            showMsgDataPaeseDialog(activity, data);
        }
    });
    addMenuItem(menuItems, "消息操作", "提取音频", new Runnable() {
        public void run() {
            showExtractAudioDialog(activity, data);
        }
    });
    if (chatType == 2 && ("OWNER".equals(role) || "ADMIN".equals(role))) {
        addMenuItem(menuItems, "消息操作", "设为精华", new Runnable() { public void run() { setMsgEssence(data, true); } });
        addMenuItem(menuItems, "消息操作", "取消精华", new Runnable() { public void run() { setMsgEssence(data, false); } });
    }
    if (chatType == 2) {
        addMenuItem(menuItems, "群管理", "成员信息", new Runnable() {
            public void run() {
                showMemberInfoDialog(activity, finalPeerUin, finalUserUin, finalChatType);
            }
        });
        addMenuItem(menuItems, "群管理", "群详细信息", new Runnable() {
            public void run() {
                showGroupInfoDialog(activity, finalPeerUin, isDark);
            }
        });
        addMenuItem(menuItems, "群管理", "全体禁言", new Runnable() {
            public void run() {
                showMuteAllDialog(activity, finalPeerUin);
            }
        });
        addMenuItem(menuItems, "群管理", "禁言此人", new Runnable() {
            public void run() {
                showShutUpDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "踢出此人", new Runnable() {
            public void run() {
                showKickConfirmDialog(activity, finalPeerUin, finalUserUin, finalNickName, isDark);
            }
        });
        addMenuItem(menuItems, "群管理", "设置头衔", new Runnable() {
            public void run() {
                showTitleDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "设置管理员", new Runnable() {
            public void run() {
                showSetAdminDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "修改群名片", new Runnable() {
            public void run() {
                showChangeCardDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "禁言列表", new Runnable() {
            public void run() {
                showProhibitListDialog(activity, finalPeerUin, isDark);
            }
        });
        addMenuItem(menuItems, "群管理", "群成员列表", new Runnable() {
            public void run() {
                showGroupMemberListDialog(activity, finalPeerUin, isDark);
            }
        });
    } else {
        addMenuItem(menuItems, "好友信息", "查看资料", new Runnable() {
            public void run() {
                showMemberInfoDialog(activity, finalPeerUin, finalUserUin, finalChatType);
            }
        });
    }
    addMenuItem(menuItems, "互动功能", "为Ta点赞", new Runnable() {
        public void run() {
            showZanDialog(activity, finalUserUin);
        }
    });
    if (chatType == 2) {
        addMenuItem(menuItems, "互动功能", "回应表情", new Runnable() { public void run() { showFaceReplyConfigDialog(data); } });
        addMenuItem(menuItems, "互动功能", "AI声聊", new Runnable() { public void run() { showVoiceSendDialog(data); } });
        addMenuItem(menuItems, "互动功能", "艾特全体", new Runnable() {
            public void run() {
                sendMsg(finalPeerUin, "[atUin=0]", finalChatType);
            }
        });
        addMenuItem(menuItems, "互动功能", "艾特列表", new Runnable() { public void run() { showAtListDialog(finalAtList); } });
    }
    addMenuItem(menuItems, "互动功能", "拍一拍", new Runnable() {
        public void run() {
            showPaiDialog(activity, finalUserUin, finalPeerUin, finalChatType);
        }
    });
    addMenuItem(menuItems, "工具", "加解密工具", new Runnable() {
        public void run() {
            showEncryptDecryptDialog(activity, data);
        }
    });
    addMenuItem(menuItems, "工具", "执行代码", new Runnable() { public void run() { showCodeConsoleDialog(activity, data); } });
    addMenuItem(menuItems, "工具", "发送pb", new Runnable() { public void run() { showPBSenderDialog(); } });
    addMenuItem(menuItems, "工具", "获取Cookie", new Runnable() {
        public void run() {
            showGetCookieDialog(activity, isDark);
        }
    });
    addMenuItem(menuItems, "工具", "群列表", new Runnable() { public void run() { showGroupListDialog(activity, isDark); } });
    addMenuItem(menuItems, "工具", "好友列表", new Runnable() { public void run() { showFriendListDialog(activity, isDark); } });
    addMenuItem(menuItems, "工具", "发送日志", new Runnable() {
        public void run() {
            Toast("正在打包日志...");
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        File logDir = new File(pluginPath + "/Log");
                        String zipPath = null;
                        if (logDir.exists() && logDir.isDirectory()) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
                            zipPath = pluginPath + "/" + sdf.format(new java.util.Date()) + "-Log.zip";
                            String envInfo = "";
                            try {
                                envInfo = String.valueOf(me.yxp.qfun.utils.log.LogUtils.INSTANCE.getEnvironmentInfo());
                            } catch (Throwable e2) {
                                envInfo = "获取环境信息异常: " + e2;
                            }
                            if (!zipDirectory(logDir.getAbsolutePath(), zipPath, "log", "info.txt", envInfo, null)) {
                                zipPath = null;
                            }
                        }
                        if (zipPath == null) {
                            Toast("日志打包失败");
                            return;
                        }
                        Toast("日志已打包，正在发送...");
                        sendFile(finalPeerUin, zipPath, finalChatType);
                        Toast("日志已发送");
                        final String delPath = zipPath;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                try {
                                    new File(delPath).delete();
                                } catch (Throwable e3) {
                                    traceLog("dialog_log", "[长按消息菜单] 删除日志zip异常: " + e3);
                                }
                            }
                        }, 180000);
                    } catch (Throwable e) {
                        traceLog("dialog_log", "[长按消息菜单] 发送日志异常: " + e);
                        Toast("发送日志异常: " + e.getMessage());
                    }
                }
            });
        }
    });
    addMenuItem(menuItems, "其他", "克隆头像", new Runnable() { public void run() { handleCloneAvatar(finalUserUin); } });
    addMenuItem(menuItems, "其他", "上传头像", new Runnable() { public void run() { handleUploadAvatar(finalQuntext); } });
    if (chatType == 2) {
        addMenuItem(menuItems, "其他", "群打卡", new Runnable() {
            public void run() {
                boolean ok = CheckSign(finalPeerUin, myUin);
                qqToast(ok ? 2 : 1, ok ? "打卡成功" : "打卡失败");
            }
        });
        addMenuItem(menuItems, "其他", "群字符", new Runnable() { public void run() { drawLuckyChar(finalPeerUin); } });
    }
    addMenuItem(menuItems, "其他", "设置", new Runnable() {
        public void run() {
            launchSettingsActivity(activity, finalChatType, finalPeerUin, (finalQuntext != null) ? finalQuntext : "");
        }
    });
    addMenuItem(menuItems, "其他", "双击消息菜单", new Runnable() {
        public void run() {
            fetchRealMsgRecord(finalMsgid, finalChatType, finalPeerUin, new MsgLoadedCallback() {
                public void onLoaded(MsgData msgData) {
                    Activity target = activity;
                    if (target == null || target.isFinishing()) target = getNowActivity();
                    showActionDialog(target, msgData, null, null);
                }
            });
        }
    });
    final boolean[]isEditMode = new boolean[] {
        false
    };
    String savedOrder = getString("setting", "menuSort", "");
    final List catOrder = new ArrayList();
    final Map catItems = new LinkedHashMap();
    for (int i = 0; i < menuItems.size(); i++) {
        Object[]item = (Object[]) menuItems.get(i);
        String cat = (String) item[0];
        if (!catOrder.contains(cat)) catOrder.add(cat);
        if (!catItems.containsKey(cat)) catItems.put(cat, new ArrayList());
        ((List) catItems.get(cat)).add(item);
    }
    applySavedSortOrder(catOrder, catItems, savedOrder);
    Runnable saveOrder = new Runnable() {
        public void run() {
            try {
                String orderStr = buildSortOrderString(catOrder, catItems);
                if (!orderStr.isEmpty()) {
                    putString("setting", "menuSort", orderStr);
                }
            } catch (Throwable t) {
                traceLog("dialog_log", "[onLoaded] 异常: " + t);
            }
        }
    };
    final Runnable[]renderMenuWrapper = new Runnable[1];
    renderMenuWrapper[0] = new Runnable() {
        public void run() {
            menuContainer.removeAllViews();
            View.OnDragListener dragListener = new View.OnDragListener() {
                public boolean onDrag(View v, DragEvent event) {
                    if (event.getAction() == DragEvent.ACTION_DROP) {
                        View source = (View) event.getLocalState();
                        if (source == v) return true;
                        Object[]srcTag = (Object[]) source.getTag();
                        Object[]tgtTag = (Object[]) v.getTag();
                        if (srcTag == null || tgtTag == null) return false;
                        String srcType = (String) srcTag[0];
                        String tgtType = (String) tgtTag[0];
                        if (srcType.equals("cat") && tgtType.equals("cat")) {
                            String srcCat = (String) srcTag[1];
                            String tgtCat = (String) tgtTag[1];
                            int sIdx = catOrder.indexOf(srcCat);
                            int tIdx = catOrder.indexOf(tgtCat);
                            if (sIdx >= 0 && tIdx >= 0) {
                                catOrder.remove(sIdx);
                                catOrder.add(tIdx, srcCat);
                                if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                            }
                        } else if (srcType.equals("item") && tgtType.equals("item")) {
                            String srcCat = (String) srcTag[1];
                            String srcItem = (String) srcTag[2];
                            String tgtCat = (String) tgtTag[1];
                            String tgtItem = (String) tgtTag[2];
                            if (srcCat.equals(tgtCat)) {
                                List iList = (List) catItems.get(srcCat);
                                int sIdx = - 1, tIdx = - 1;
                                for (int i = 0; i < iList.size(); i++) {
                                    Object[]itm = (Object[]) iList.get(i);
                                    if (itm[1].equals(srcItem)) sIdx = i;
                                    if (itm[1].equals(tgtItem)) tIdx = i;
                                }
                                if (sIdx >= 0 && tIdx >= 0) {
                                    Object obj = iList.remove(sIdx);
                                    iList.add(tIdx, obj);
                                    if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                                }
                            } else {
                                List srcList = (List) catItems.get(srcCat);
                                List tgtList = (List) catItems.get(tgtCat);
                                if (srcList != null && tgtList != null) {
                                    int sIdx = - 1;
                                    Object[]movedItem = null;
                                    for (int i = 0; i < srcList.size(); i++) {
                                        Object[]itm = (Object[]) srcList.get(i);
                                        if (itm[1].equals(srcItem)) {
                                            sIdx = i;
                                            movedItem = itm;
                                            break;
                                        }
                                    }
                                    if (movedItem != null) {
                                        movedItem[0] = tgtCat;
                                        srcList.remove(sIdx);
                                        int tIdx = - 1;
                                        for (int i = 0; i < tgtList.size(); i++) {
                                            Object[]itm = (Object[]) tgtList.get(i);
                                            if (itm[1].equals(tgtItem)) {
                                                tIdx = i;
                                                break;
                                            }
                                        }
                                        if (tIdx >= 0) {
                                            tgtList.add(tIdx, movedItem);
                                        } else {
                                            tgtList.add(movedItem);
                                        }
                                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            };
            for (int i = 0; i < catOrder.size(); i++) {
                String cat = (String) catOrder.get(i);
                int[]colorInts = getCategoryColorInts(cat, isDark);
                LinearLayout catLayout = new LinearLayout(activity);
                catLayout.setOrientation(LinearLayout.VERTICAL);
                catLayout.setClipChildren(false);
                catLayout.setClipToPadding(false);
                catLayout.setTag(new Object[] { "cat", cat });
                LinearLayout titleRow = new LinearLayout(activity);
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(Gravity.CENTER_VERTICAL);
                TextView titleView = createCategoryTitle(activity, cat, colorInts[0]);
                titleView.setTag(new Object[] { "cat", cat });
                if (isEditMode[0]) {
                    titleView.setOnDragListener(dragListener);
                    titleView.setOnTouchListener(new View.OnTouchListener() {
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                                v.startDragAndDrop(null, shadow, v, 0);
                                return true;
                            }
                            return false;
                        }
                    });
                    titleView.setText(cat + " (可拖动排序)");
                }
                titleRow.addView(titleView);
                catLayout.addView(titleRow);
                GridLayout gridLayout = new GridLayout(activity);
                gridLayout.setColumnCount(2);
                gridLayout.setClipChildren(false);
                gridLayout.setClipToPadding(false);
                gridLayout.setPadding(0, 0, 0, dp(activity, 8));
                List items = (List) catItems.get(cat);
                for (int j = 0; j < items.size(); j++) {
                    Object[]itemObj = (Object[]) items.get(j);
                    String title = (String) itemObj[1];
                    Runnable callback = (Runnable) itemObj[2];
                    FrameLayout menuItem = createMenuItem(activity, title, isDark, colorInts[1], isDark ? colorInts[1] : colorInts[0], bottomSheet, callback, false);
                    menuItem.setTag(new Object[] { "item", cat, title });
                    if (isEditMode[0]) {
                        menuItem.setOnDragListener(dragListener);
                        menuItem.setOnTouchListener(new View.OnTouchListener() {
                            public boolean onTouch(View v, MotionEvent event) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                                    v.startDragAndDrop(null, shadow, v, 0);
                                    return true;
                                }
                                return false;
                            }
                        });
                    }
                    if (!isEditMode[0]) {
                        menuItem.setAlpha(0f);
                        menuItem.setScaleX(0.9f);
                        menuItem.setScaleY(0.9f);
                        int delay = (i * 10 + j) * 25;
                        final FrameLayout finalItem = menuItem;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                finalItem
                                    .animate()
                                    .alpha(1f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(180)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .start();
                            }
                        }, delay);
                    }
                    gridLayout.addView(menuItem);
                }
                catLayout.addView(gridLayout);
                menuContainer.addView(catLayout);
            }
            int[]settingColorInts = getCategoryColorInts("设置", isDark);
            LinearLayout settingLayout = new LinearLayout(activity);
            settingLayout.setOrientation(LinearLayout.VERTICAL);
            settingLayout.setClipChildren(false);
            settingLayout.setClipToPadding(false);
            settingLayout.setTag(new Object[] { "cat", "设置" });
            LinearLayout settingTitleRow = new LinearLayout(activity);
            settingTitleRow.setOrientation(LinearLayout.HORIZONTAL);
            settingTitleRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView settingTitle = createCategoryTitle(activity, "设置", settingColorInts[0]);
            settingTitle.setTag(new Object[] { "cat", "设置" });
            if (isEditMode[0]) {
                settingTitle.setOnDragListener(dragListener);
                settingTitle.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                            v.startDragAndDrop(null, shadow, v, 0);
                            return true;
                        }
                        return false;
                    }
                });
                settingTitle.setText("设置 (可拖动排序)");
            }
            settingTitleRow.addView(settingTitle);
            settingLayout.addView(settingTitleRow);
            GridLayout settingGrid = new GridLayout(activity);
            settingGrid.setColumnCount(2);
            settingGrid.setClipChildren(false);
            settingGrid.setClipToPadding(false);
            settingGrid.setPadding(0, 0, 0, dp(activity, 8));
            FrameLayout sortMenuItem = createMenuItem(activity, "自定义排序", isDark, settingColorInts[1], isDark ? settingColorInts[1] : settingColorInts[0], bottomSheet, new Runnable() {
                public void run() {
                    isEditMode[0] = true;
                    if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                }
            }, true);
            sortMenuItem.setTag(new Object[] { "item", "设置", "自定义排序" });
            if (isEditMode[0]) {
                sortMenuItem.setOnDragListener(dragListener);
                sortMenuItem.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                            v.startDragAndDrop(null, shadow, v, 0);
                            return true;
                        }
                        return false;
                    }
                });
            }
            settingGrid.addView(sortMenuItem);
            settingLayout.addView(settingGrid);
            if (isEditMode[0]) {
                LinearLayout actionBtnRow = new LinearLayout(activity);
                actionBtnRow.setOrientation(LinearLayout.HORIZONTAL);
                actionBtnRow.setGravity(Gravity.CENTER);
                actionBtnRow.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
                TextView cancelBtn = createButton(activity, "取消", Color.WHITE, isDark ? pc("#FF5252") : pc("#F44336"),
                    14f, 20, 16, 10, false, 0, 0, null);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isEditMode[0] = false;
                        String saved = getString("setting", "menuSort", "");
                        applySavedSortOrder(catOrder, catItems, saved);
                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                    }
                });
                TextView saveBtn = createButton(activity, "保存", Color.WHITE, isDark ? pc("#81C784") : pc("#4CAF50"),
                    14f, 20, 16, 10, false, 0, 0, null);
                saveBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isEditMode[0] = false;
                        saveOrder.run();
                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                        Toast("排序已保存");
                    }
                });
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                btnLp.setMargins(dp(activity, 16), 0, dp(activity, 16), 0);
                cancelBtn.setLayoutParams(btnLp);
                saveBtn.setLayoutParams(btnLp);
                actionBtnRow.addView(cancelBtn);
                actionBtnRow.addView(saveBtn);
                settingLayout.addView(actionBtnRow);
            }
            menuContainer.addView(settingLayout);
        }
    };
    renderMenuWrapper[0].run();
    scrollView.addView(menuContainer);
    rootLayout.addView(scrollView);
    bottomSheet.setContentView(rootLayout);
    bottomSheet.setCancelable(true);
    bottomSheet.setCanceledOnTouchOutside(true);
    bottomSheet.show();
    Window window = bottomSheet.getWindow();
    if (window != null) {
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.BOTTOM);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(sheetBg);
        float cr = dp(activity, getUiCornerDp());
        bgDrawable.setCornerRadii(new float[] { cr, cr, cr, cr, 0, 0, 0, 0 });
        rootLayout.setBackground(bgDrawable);
        rootLayout.setTranslationY(dp(activity, 120));
        rootLayout.setAlpha(0f);
        rootLayout
            .animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(new DecelerateInterpolator())
            .start();
        if (!"color".equals(getString("settings", "ui_bg_type", "color"))) {
            applyUiTheme(activity, bottomSheet, 5);
        }
        applyWindowBlurBehind(window);
    }
}

String groupDisplayName(String qun) {
    String gName = getGroupName(qun);
    String gRemark = getGroupRemark(qun);
    if (gRemark == null || gRemark.isEmpty()) return gName;
    if (gName == null || gName.isEmpty() || gRemark.equals(gName)) return gRemark;
    return gRemark + "(" + gName + ")";
}

View buildSimpleListItem(Activity activity,
    Object[] itemData,
    int listMode,
    String groupUin,
    boolean isDark) {
    String uin = String.valueOf(itemData[0]);
    String name = String.valueOf(itemData[1]);
    LinearLayout item = new LinearLayout(activity);
    item.setOrientation(LinearLayout.HORIZONTAL);
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setMinimumHeight(dp(activity, 58));
    item.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 10), dp(activity, 8));
    item.setClickable(true);
    item.setBackground(makeAdaptiveRippleBg(activity, Color.TRANSPARENT, 12));
    item.setClipToOutline(true);

    LinearLayout textBox = new LinearLayout(activity);
    textBox.setOrientation(LinearLayout.VERTICAL);
    textBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

    TextView titleView = new TextView(activity);
    titleView.setText(name);
    titleView.setTextSize(16);
    titleView.setMaxLines(1);
    titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
    titleView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    textBox.addView(titleView);

    TextView uinView = new TextView(activity);
    uinView.setText(uin);
    uinView.setTextSize(11);
    uinView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    uinView.setSingleLine(true);
    uinView.setIncludeFontPadding(false);
    LinearLayout.LayoutParams uinParams = new LinearLayout.LayoutParams(-1, -2);
    uinParams.topMargin = dp(activity, 2);
    textBox.addView(uinView, uinParams);

    TextView infoView = new TextView(activity);
    infoView.setTextSize(11);
    infoView.setMaxLines(1);
    infoView.setIncludeFontPadding(false);
    infoView.setEllipsize(android.text.TextUtils.TruncateAt.END);
    infoView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-2, -2);
    infoParams.topMargin = dp(activity, 2);
    textBox.addView(infoView, infoParams);

    item.addView(textBox);

    TextView roleView = new TextView(activity);
    roleView.setTextSize(11);
    roleView.setGravity(Gravity.CENTER);
    roleView.setIncludeFontPadding(false);
    roleView.setPadding(dp(activity, 9), dp(activity, 4), dp(activity, 9), dp(activity, 4));
    LinearLayout.LayoutParams roleParams = new LinearLayout.LayoutParams(-2, -2);
    roleParams.leftMargin = dp(activity, 10);
    item.addView(roleView, roleParams);

    item.setTag(new Object[] {titleView, uinView, infoView, roleView});
    bindSimpleListItem(activity, item, itemData, listMode, groupUin, isDark);
    return item;
}

void scheduleProgressiveListRender(final BaseAdapter adapter,
    final List display,
    final int[] visibleLimit,
    final boolean[] closed,
    final AlertDialog dialog,
    final ListView listView,
    final int batchSize,
    final Runnable[] taskRef,
    final String logName) {
    if (closed[0] || !dialog.isShowing() || display.isEmpty()) return;
    if (visibleLimit[0] >= display.size()) return;
    if (taskRef[0] != null && uiHandler != null) uiHandler.removeCallbacks(taskRef[0]);
    taskRef[0] = new Runnable() {
        public void run() {
            if (closed[0] || !dialog.isShowing()) return;
            try {
                if (visibleLimit[0] < display.size()) {
                    int first = listView == null ? 0 : listView.getFirstVisiblePosition();
                    int visible = listView == null ? 8 : Math.max(listView.getChildCount(), 8);
                    int target = Math.min(display.size(), first + visible + batchSize * 3);
                    int next = Math.min(target, visibleLimit[0] + batchSize);
                    if (next > visibleLimit[0]) {
                        visibleLimit[0] = next;
                        adapter.notifyDataSetChanged();
                    }
                    if (visibleLimit[0] < target) uiHandler.postDelayed(this, 16);
                }
            } catch (Throwable e) {
                traceLog("dialog_log", "[" + logName + "] 单批渲染异常: " + e);
            }
        }
    };
    uiHandler.post(taskRef[0]);
}

void bindSimpleListItem(Activity activity,
    View cv,
    Object[] itemData,
    int listMode,
    String groupUin,
    boolean isDark) {
    LinearLayout item = (LinearLayout) cv;
    Object tag = item.getTag();
    if (!(tag instanceof Object[])) return;
    Object[] holder = (Object[]) tag;
    if (holder.length < 4) return;
    if (!(holder[0] instanceof TextView) || !(holder[1] instanceof TextView) || !(holder[2] instanceof TextView) || !(holder[3] instanceof TextView)) return;

    TextView titleView = (TextView) holder[0];
    TextView uinView = (TextView) holder[1];
    TextView infoView = (TextView) holder[2];
    TextView roleView = (TextView) holder[3];
    String uin = String.valueOf(itemData[0]);
    String name = String.valueOf(itemData[1]);
    String info = itemData.length > 2 && itemData[2] != null ? String.valueOf(itemData[2]) : "";
    String role = itemData.length > 3 && itemData[3] != null ? String.valueOf(itemData[3]) : "";

    titleView.setText(name);
    uinView.setText("QQ · " + uin);
    if (listMode == 0 && info != null && !info.isEmpty()) {
        infoView.setText(info);
        infoView.setTextColor(tc(activity, "error"));
        infoView.setBackground(null);
        infoView.setPadding(0, 0, 0, 0);
        infoView.setVisibility(View.VISIBLE);
    } else {
        infoView.setText("");
        infoView.setBackground(null);
        infoView.setPadding(0, 0, 0, 0);
        infoView.setVisibility(View.GONE);
    }

    if (listMode == 1 && role != null && !role.isEmpty()) {
        applyRolePill(activity, roleView, role);
    } else {
        roleView.setText("");
        roleView.setVisibility(View.GONE);
    }

    final String finalUin = uin;
    final String finalName = name;
    item.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (listMode == 0) showShutUpDialog(activity, groupUin, finalUin, finalName);
            else if (listMode == 1) showMemberInfoDialog(activity, groupUin, finalUin, 2);
            else if (listMode == 2) showGroupInfoDialog(activity, finalUin, isDark);
            else showMemberInfoDialog(activity, finalUin, finalUin, 1);
        }
    });
}

void showSimpleListDialog(Activity act0,
    final String title,
    final int listMode,
    final String groupUin,
    final boolean isDark) {
    Activity dlgAct = act0;
    if (dlgAct == null || dlgAct.isFinishing()) {
        try {
            dlgAct = getNowActivity();
        } catch (Throwable ignored) {}
    }
    if (dlgAct == null || dlgAct.isFinishing()) {
        try {
            dlgAct = getSettingsCurrentActivity();
        } catch (Throwable ignored) {
            dlgAct = null;
        }
    }
    if (dlgAct == null || dlgAct.isFinishing()) {
        traceLog("dialog_log", "[showSimpleListDialog] 无可用 Activity，弹窗放弃显示");
        return;
    }
    try {
        final int textColor = pc(getSettingsThemeColor(dlgAct, "on_surface"));
        final int subTextColor = pc(getSettingsThemeColor(dlgAct, "on_surface_variant"));
        final String emptyText = listMode == 0 ? "当前群无禁言成员" : listMode == 1 ? "当前群无成员" : listMode == 2 ? "暂无群聊" : "暂无好友";
        final String countUnit = listMode == 2 ? "个" : "人";
        LinearLayout root = new LinearLayout(dlgAct);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(dlgAct, 14), dp(dlgAct, 14), dp(dlgAct, 14), dp(dlgAct, 10));
        final TextView titleView = new TextView(dlgAct);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, dp(dlgAct, 4));
        root.addView(titleView);
        final EditText search = makeInput(dlgAct,
            listMode == 0 || listMode == 1 ? "搜索昵称或QQ号" : listMode == 2 ? "搜索群名或群号" : "搜索昵称或QQ号",
            null);
        search.setSingleLine(true);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(dlgAct, 42)));
        search.setVisibility(View.GONE);
        FrameLayout listFrame = new FrameLayout(dlgAct);
        LinearLayout.LayoutParams listFrameParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        listFrameParams.topMargin = dp(dlgAct, 8);
        listFrameParams.bottomMargin = dp(dlgAct, 2);
        root.addView(listFrame, listFrameParams);
        final ListView lv = new ListView(dlgAct);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        listFrame.addView(lv, new FrameLayout.LayoutParams(-1, -1));
        final LinearLayout loading = createModernLoading(dlgAct, isDark);
        listFrame.addView(loading, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        final TextView emptyView = new TextView(dlgAct);
        emptyView.setText(emptyText);
        emptyView.setTextSize(14);
        emptyView.setTextColor(subTextColor);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        listFrame.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout btnBox = new LinearLayout(dlgAct);
        btnBox.setOrientation(LinearLayout.HORIZONTAL);
        btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        btnBox.setMinimumHeight(dp(dlgAct, 44));
        TextView closeBtn = createButton(dlgAct, "关闭", pc(getSettingsThemeColor(dlgAct, "primary")), Color.TRANSPARENT,
            15f, 0, 16, 12, false, 0, 0, null);
        btnBox.addView(closeBtn);
        root.addView(btnBox);
        final AlertDialog dialog = new AlertDialog.Builder(dlgAct,
            isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
            .setView(root)
            .create();
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
        applyUiTheme(dlgAct, dialog, 0);
        try {
            Window win = dialog.getWindow();
            if (win != null) {
                int screenW = dlgAct.getResources().getDisplayMetrics().widthPixels;
                int screenH = dlgAct.getResources().getDisplayMetrics().heightPixels;
                int width = Math.min(dp(dlgAct, 420), screenW - dp(dlgAct, 32));
                int maxHeight = Math.min(dp(dlgAct, 400), (int)(screenH * 0.52f));
                int minHeight = Math.min(dp(dlgAct, 280), Math.max(1, screenH - dp(dlgAct, 64)));
                int height = Math.max(minHeight, maxHeight);
                if (height > 0) win.setLayout(width, height);
            }
        } catch (Throwable e) {
            traceLog("dialog_log", "[showSimpleListDialog.window] 异常: " + e);
        }
        final ArrayList allData = new ArrayList();
        final ArrayList displayData = new ArrayList();
        final boolean[] closed = new boolean[] {false};
        final boolean[] loadFinished = new boolean[] {false};
        final int[] visibleLimit = new int[] {0};
        final int initialBatch = 64;
        final int batchSize = 48;
        final int dataBatchSize = 160;
        final Runnable[] renderTask = new Runnable[1];
        final int[] filterGeneration = new int[] {0};
        final Runnable updateListState = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                boolean hasData = !allData.isEmpty();
                if (hasData) {
                    if (search.getVisibility() != View.VISIBLE) {
                        search.setAlpha(0f);
                        search.setVisibility(View.VISIBLE);
                        search.animate().alpha(1f).setDuration(180).start();
                    }
                } else {
                    search.setVisibility(View.GONE);
                }
                if (displayData.isEmpty()) {
                    if (!loadFinished[0]) {
                        loading.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else if (!hasData) {
                        loading.setVisibility(View.GONE);
                        emptyView.setText(emptyText);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        loading.setVisibility(View.GONE);
                        emptyView.setText("无匹配结果");
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    loading.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                }
                SpannableStringBuilder titleSpan = new SpannableStringBuilder(title + " (" + displayData.size() + countUnit + ")");
                titleSpan.setSpan(new ForegroundColorSpan(pc(getSettingsThemeColor(dlgAct, "primary"))), title.length(), titleSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleView.setText((CharSequence) titleSpan);
            }
        };
        final BaseAdapter adapter = new BaseAdapter() {
            public int getCount() {
                return Math.min(displayData.size(), visibleLimit[0]);
            }
            public Object getItem(int p) {
                return displayData.get(p);
            }
            public long getItemId(int p) {
                return p;
            }
            public View getView(int pos, View cv, ViewGroup parent) {
                try {
                    if (cv instanceof LinearLayout) {
                        Object t = ((LinearLayout) cv).getTag();
                        if (!(t instanceof Object[]) || ((Object[]) t).length < 4 || !(((Object[]) t)[0] instanceof TextView) || !(((Object[]) t)[1] instanceof TextView) || !(((Object[]) t)[2] instanceof TextView) || !(((Object[]) t)[3] instanceof TextView)) cv = null;
                    } else {
                        cv = null;
                    }
                    if (cv == null) {
                        cv = buildSimpleListItem(dlgAct, (Object[]) displayData.get(pos), listMode, groupUin, isDark);
                    } else {
                        bindSimpleListItem(dlgAct, cv, (Object[]) displayData.get(pos), listMode, groupUin, isDark);
                    }
                    return cv;
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showSimpleListDialog.getView] 异常: " + e);
                    return cv == null ? new TextView(dlgAct) : cv;
                }
            }
        };
        lv.setAdapter(adapter);
        final Runnable applyFilter = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                final String q = search.getText().toString().trim().toLowerCase();
                final int generation = ++filterGeneration[0];
                final ArrayList snapshot = new ArrayList(allData);
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            final ArrayList result = new ArrayList();
                            if (q.isEmpty()) {
                                result.addAll(snapshot);
                            } else {
                                for (Object obj : snapshot) {
                                    Object[] item = (Object[]) obj;
                                    String text = String.valueOf(item[0]) + " " + String.valueOf(item[1]) + " " + String.valueOf(item[2]);
                                    if (text.toLowerCase().contains(q)) result.add(item);
                                }
                            }
                            uiHandler.post(new Runnable() {
                                public void run() {
                                    if (closed[0] || !dialog.isShowing() || generation != filterGeneration[0]) return;
                                    if (snapshot.size() != allData.size()) {
                                        applyFilter.run();
                                        return;
                                    }
                                    displayData.clear();
                                    displayData.addAll(result);
                                    visibleLimit[0] = Math.min(displayData.size(), initialBatch);
                                    adapter.notifyDataSetChanged();
                                    updateListState.run();
                                    if (visibleLimit[0] < displayData.size()) scheduleProgressiveListRender(adapter, displayData, visibleLimit, closed, dialog, lv, batchSize, renderTask, "showSimpleListDialog.render");
                                }
                            });
                        } catch (Throwable e) {
                            traceLog("dialog_log", "[showSimpleListDialog.filter] 异常: " + e);
                        }
                    }
                });
            }
        };
        final Runnable searchTask = new Runnable() {
            public void run() {
                applyFilter.run();
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }
            public void afterTextChanged(Editable e) {
                search.removeCallbacks(searchTask);
                search.postDelayed(searchTask, 120);
            }
        });
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int state) {
            }
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (closed[0] || visible <= 0) return;
                if (first + visible >= visibleLimit[0] - 3 && visibleLimit[0] < displayData.size()) {
                    visibleLimit[0] = Math.min(displayData.size(), visibleLimit[0] + batchSize);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        ThreadPool.execute(new Runnable() {
            public void run() {
                try {
                    List source = null;
                    final long listNow = System.currentTimeMillis() / 1000;
                    if (listMode == 0) source = (List) getProhibitList(groupUin);
                    else if (listMode == 1) source = (List) getGroupMemberList(groupUin);
                    else if (listMode == 2) source = (List) getGroupList();
                    else source = (List) getAllFriend();
                    if (source == null) source = new ArrayList();
                    ArrayList readyBatch = new ArrayList(dataBatchSize);
                    int batchIndex = 0;
                    for (Object o : source) {
                        if (o == null) continue;
                        try {
                            Object[] item;
                            if (listMode == 0) {
                                String gUin = String.valueOf(o.user);
                                String gName = String.valueOf(o.userName);
                                String gRemark = getUserRemark(gUin);
                                item = new Object[] {gUin, (gRemark == null || gRemark.isEmpty()) ? gName : gRemark + "(" + gName + ")", getGagStatus(String.valueOf(o.endTime), listNow), ""};
                            } else if (listMode == 1) {
                                String mUin = String.valueOf(o.uin);
                                String mName = String.valueOf(o.uinName);
                                String mRemark = getUserRemark(String.valueOf(o.uin));
                                String role = convertRole(o.role == null ? "" : String.valueOf(o.role));
                                item = new Object[] {mUin, (mRemark == null || mRemark.isEmpty()) ? mName : mRemark + "(" + mName + ")", "", role};
                            } else if (listMode == 2) {
                                String gUin = String.valueOf(o.group);
                                item = new Object[] {gUin, groupDisplayName(gUin), "", ""};
                            } else {
                                String uin = String.valueOf(o.uin);
                                String uName = String.valueOf(o.name);
                                String uRemark = getUserRemark(uin);
                                item = new Object[] {uin, (uRemark == null || uRemark.isEmpty()) ? uName : uRemark + "(" + uName + ")", "", ""};
                            }
                            readyBatch.add(item);
                            if (readyBatch.size() >= dataBatchSize) {
                                final ArrayList postBatch = new ArrayList(readyBatch);
                                readyBatch.clear();
                                final long batchDelay = batchIndex++ * 8L;
                                uiHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        if (closed[0] || !dialog.isShowing()) return;
                                        try {
                                            allData.addAll(postBatch);
                                            String q = search.getText().toString().trim().toLowerCase();
                                            if (q.isEmpty()) {
                                                displayData.addAll(postBatch);
                                            } else {
                                                for (Object obj : postBatch) {
                                                    Object[] item = (Object[]) obj;
                                                    String text = String.valueOf(item[0]) + " " + String.valueOf(item[1]) + " " + String.valueOf(item[2]);
                                                    if (text.toLowerCase().contains(q)) displayData.add(item);
                                                }
                                            }
                                            if (visibleLimit[0] < initialBatch) visibleLimit[0] = Math.min(displayData.size(), initialBatch);
                                            adapter.notifyDataSetChanged();
                                            updateListState.run();
                                            if (visibleLimit[0] < displayData.size()) scheduleProgressiveListRender(adapter, displayData, visibleLimit, closed, dialog, lv, batchSize, renderTask, "showSimpleListDialog.render");
                                        } catch (Throwable e) {
                                            traceLog("dialog_log", "[showSimpleListDialog.add] 批量异常: " + e);
                                        }
                                    }
                                }, batchDelay);
                            }
                        } catch (Throwable e) {
                            traceLog("dialog_log", "[showSimpleListDialog.data] 单条异常: " + e);
                        }
                    }
                    if (!readyBatch.isEmpty()) {
                        final ArrayList postBatch = new ArrayList(readyBatch);
                        final long lastBatchDelay = batchIndex++ * 8L;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (closed[0] || !dialog.isShowing()) return;
                                        try {
                                            allData.addAll(postBatch);
                                            String q = search.getText().toString().trim().toLowerCase();
                                            if (q.isEmpty()) {
                                                displayData.addAll(postBatch);
                                            } else {
                                                for (Object obj : postBatch) {
                                                    Object[] item = (Object[]) obj;
                                                    String text = String.valueOf(item[0]) + " " + String.valueOf(item[1]) + " " + String.valueOf(item[2]);
                                                    if (text.toLowerCase().contains(q)) displayData.add(item);
                                                }
                                            }
                                            if (visibleLimit[0] < initialBatch) visibleLimit[0] = Math.min(displayData.size(), initialBatch);
                                            adapter.notifyDataSetChanged();
                                            updateListState.run();
                                            if (visibleLimit[0] < displayData.size()) scheduleProgressiveListRender(adapter, displayData, visibleLimit, closed, dialog, lv, batchSize, renderTask, "showSimpleListDialog.render");
                                        } catch (Throwable e) {
                                            traceLog("dialog_log", "[showSimpleListDialog.add] 批量异常: " + e);
                                        }
                            }
                        }, lastBatchDelay);
                    }
                    final long finishDelay = batchIndex * 8L + 8L;
                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (closed[0] || !dialog.isShowing()) return;
                            loadFinished[0] = true;
                            updateListState.run();
                        }
                    }, finishDelay);
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showSimpleListDialog.load] 异常: " + e);
                }
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                closed[0] = true;
                search.removeCallbacks(searchTask);
                if (renderTask[0] != null && uiHandler != null) uiHandler.removeCallbacks(renderTask[0]);
                allData.clear();
                displayData.clear();
            }
        });
    } catch (Throwable e) {
        traceLog("dialog_log", "[showSimpleListDialog] 异常: " + e);
    }
}
void showProhibitListDialog(Activity activity, String groupUin, boolean isDark) {
    showSimpleListDialog(activity, "禁言列表", 0, groupUin, isDark);
}

void showGroupMemberListDialog(Activity activity, String groupUin, boolean isDark) {
    showSimpleListDialog(activity, "群成员列表", 1, groupUin, isDark);
}

void showGroupListDialog(Activity activity, boolean isDark) {
    showSimpleListDialog(activity, "我的群列表", 2, null, isDark);
}

void showFriendListDialog(Activity activity, boolean isDark) {
    showSimpleListDialog(activity, "我的好友列表", 3, null, isDark);
}

public void ts(Activity activity, String title, String content) {
    if (activity == null || activity.isFinishing()) {
        traceLog("dialog_log", "[ts] Activity无效，无法显示弹窗");
        return;
    }
    boolean isDark = isThemeDark(activity);
    final String finalContent = content == null ? "" : content;
    final int[]HIGHLIGHT_COLORS = {
        pc("#FF6B6B"), pc("#45B7D1"), pc("#DDA0DD")
    };
    final int NORMAL_LINE_COLOR = isDark ? pc("#AAAAAA") : pc("#666666");
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                vibrate(activity, 48);
            } catch (Exception e) {
                traceLog("dialog_log", "[ts] 震动执行异常: " + e.getMessage());
            }
            LinearLayout layout = new LinearLayout(activity);
            layout.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
            layout.setOrientation(LinearLayout.VERTICAL);
            TextView textView = new TextView(activity);
            textView.setTextSize(22);
            textView.setTextIsSelectable(true);
            textView.setSingleLine(false);
            textView.setMaxLines(Integer.MAX_VALUE);
            textView.setEllipsize(null);
            textView.setTextColor(tc(activity, "on_surface_strong"));
            try {
                if (!finalContent.isEmpty()) {
                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    String[]lines = finalContent.split("\n");
                    int highlightIndex = 0;
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        SpannableString spannable = new SpannableString(line);
                        int targetColor = line
                            .contains("#") ? HIGHLIGHT_COLORS[highlightIndex++ % HIGHLIGHT_COLORS
                            .length] : NORMAL_LINE_COLOR;
                        spannable.setSpan(new ForegroundColorSpan(targetColor), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.append(spannable);
                        if (i != lines.length - 1) {
                            ssb.append("\n");
                        }
                    }
                    textView.setText(ssb);
                } else {
                    textView.setTextColor(NORMAL_LINE_COLOR);
                    textView.setText(finalContent);
                }
            } catch (Throwable e) {
                traceLog("dialog_log", "[ts] 文本高亮处理异常: " + e.getMessage());
                textView.setTextColor(NORMAL_LINE_COLOR);
                textView.setText(finalContent);
            }
            layout.addView(textView);
            ScrollView scrollView = new ScrollView(activity);
            scrollView.addView(layout);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            builder.setTitle(title);
            builder.setView(scrollView);
            builder.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Toast("你知道啥了");
                        vibrate(activity, 48);
                    } catch (Exception e) {
                        traceLog("dialog_log", "[ts] 按钮点击异常: " + e.getMessage());
                    }
                }
            });
            builder.setCancelable(false);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            applyUiTheme(activity, alertDialog, 0);
        }
    });
}
public void mkts(final Activity activity, final String title, final String markdownContent) {
    if (activity == null || activity.isFinishing()) {
        traceLog("dialog_log", "[mkts] Activity无效");
        return;
    }
    final String markdown = markdownContent == null ? "" : markdownContent;
    final boolean isDark = isThemeDark(activity);
    Runnable parseTask = new Runnable() {
        public void run() {
            final String htmlContent;
            try {
                htmlContent = markdownToHtml(markdown);
                traceLog("dialog_log", "[mkts] 解析完成，输入长度: " + markdown.length());
            } catch (Throwable e) {
                traceLog("dialog_log", "[mkts] 解析异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (!activity.isFinishing()) createMarkdownDialog(activity, title, markdown, "<pre>" + markdown.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;") + "</pre>", isDark);
                    }
                });
                return;
            }
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if (!activity.isFinishing()) createMarkdownDialog(activity, title, markdown, htmlContent, isDark);
                }
            });
        }
    };
    try {
        if (ThreadPool != null && !ThreadPool.isShutdown()) ThreadPool.execute(parseTask);
        else parseTask.run();
    } catch (Throwable e) {
        traceLog("dialog_log", "[mkts] 线程调度失败，直接解析: " + e.getMessage());
        parseTask.run();
    }
}

private void createMarkdownDialog(final Activity activity, final String title, final String originalMarkdown, final String initialHtml, final boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    final boolean[] isReversed = {false};
    final WebView webView = new WebView(activity);
    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.setVerticalScrollBarEnabled(true);
    webView.setHorizontalScrollBarEnabled(false);
    webView.getSettings().setJavaScriptEnabled(false);
    webView.getSettings().setDefaultTextEncodingName("UTF-8");

    final Runnable loadContent = new Runnable() {
        public void run() {
            String htmlContent = isReversed[0] ? markdownToHtml(reverseMarkdownBlocks(originalMarkdown)) : initialHtml;
            webView.loadDataWithBaseURL(null, wrapHtmlWithCss(htmlContent, isDark), "text/html", "UTF-8", null);
        }
    };

    int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
    int webViewHeight = Math.min(screenHeight * 2 / 3, dp(activity, 800));
    LinearLayout container = new LinearLayout(activity);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
    container.addView(webView, new LinearLayout.LayoutParams(-1, webViewHeight));

    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    btnRow.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    TextView btnClose = createButton(activity, "我知道了", tc(activity, "primary"), Color.TRANSPARENT, 14f, 12, 18, 10, false, 0, 0, null);
    btnRow.addView(btnClose, new LinearLayout.LayoutParams(-2, -2));
    container.addView(btnRow, new LinearLayout.LayoutParams(-1, -2));

    LinearLayout titleLayout = new LinearLayout(activity);
    titleLayout.setOrientation(LinearLayout.HORIZONTAL);
    titleLayout.setGravity(Gravity.CENTER_VERTICAL);
    int titlePadding = dp(activity, 16);
    titleLayout.setPadding(titlePadding, titlePadding, titlePadding, titlePadding);
    TextView titleText = new TextView(activity);
    titleText.setText(title == null ? "内容" : title);
    titleText.setTextSize(18);
    titleText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    titleText.setTextColor(isDark ? Color.WHITE : Color.BLACK);
    titleLayout.addView(titleText, new LinearLayout.LayoutParams(0, -2, 1));
    final TextView orderButton = createButton(activity, "⇅", tc(activity, "on_surface_variant"), Color.TRANSPARENT, 20f, 8, 8, 0, false, 0, 0, null);
    orderButton.setContentDescription(isReversed[0] ? "按原顺序显示" : "反转更新日志顺序");
    orderButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            isReversed[0] = !isReversed[0];
            orderButton.setContentDescription(isReversed[0] ? "按原顺序显示" : "反转更新日志顺序");
            loadContent.run();
        }
    });
    titleLayout.addView(orderButton, new LinearLayout.LayoutParams(-2, -2));

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setCustomTitle(titleLayout);
    builder.setView(container);
    builder.setCancelable(true);
    final AlertDialog dialog = builder.create();
    btnClose.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            Toast("你知道啥了");
            try { dialog.dismiss(); } catch (Throwable e) { traceLog("dialog_log", "[mkts] 关闭弹窗失败: " + e.getMessage()); }
        }
    });
    dialog.show();
    applyUiTheme(activity, dialog, 1);
    loadContent.run();
    traceLog("dialog_log", "[mkts] 弹窗显示成功");
}

private String reverseMarkdownBlocks(String markdown) {
    if (markdown == null || markdown.trim().isEmpty()) return markdown;
    Pattern headerPattern = Pattern.compile("(?m)^###\\s+\\*\\*\\[v?[^\\]]+\\]\\s*-\\s*\\d{4}-\\d{2}-\\d{2}(?:\\s+\\d{2}:\\d{2})?\\*\\*.*$");
    Matcher matcher = headerPattern.matcher(markdown);
    ArrayList starts = new ArrayList();
    while (matcher.find()) starts.add(Integer.valueOf(matcher.start()));
    if (starts.size() < 2) return markdown;
    ArrayList blocks = new ArrayList();
    int prefixEnd = ((Integer) starts.get(0)).intValue();
    if (prefixEnd > 0 && !markdown.substring(0, prefixEnd).trim().isEmpty()) blocks.add(markdown.substring(0, prefixEnd));
    for (int i = 0; i < starts.size(); i++) {
        int start = ((Integer) starts.get(i)).intValue();
        int end = i + 1 < starts.size() ? ((Integer) starts.get(i + 1)).intValue() : markdown.length();
        blocks.add(markdown.substring(start, end));
    }
    StringBuilder result = new StringBuilder(markdown.length());
    for (int i = blocks.size() - 1; i >= 0; i--) result.append((String) blocks.get(i));
    return result.toString();
}

private String wrapHtmlWithCss(String htmlContent, boolean isDark) {
    String textColor = isDark ? "#E9EDF0" : "#1C1B1F";
    String titleColor = isDark ? "#FFFFFF" : "#111111";
    String codeBg = isDark ? "#252529" : "#F2F3F5";
    String border = isDark ? "#424249" : "#DADCE0";
    String link = isDark ? "#8AB4F8" : "#1769AA";
    return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><style>" +
        "*{box-sizing:border-box;}body{font-family:Roboto,'Noto Sans',sans-serif;font-size:15px;line-height:1.55;color:" + textColor + ";background:transparent;padding:8px 12px;margin:0;overflow-wrap:anywhere;}" +
        "h1,h2,h3{color:" + titleColor + ";line-height:1.3;margin:14px 0 7px;font-weight:700;}h1{font-size:21px;}h2{font-size:19px;}h3{font-size:17px;}p{margin:5px 0 9px;}" +
        "ul,ol{padding-left:23px;margin:6px 0 10px;}li{padding-left:2px;margin:3px 0;}blockquote{margin:8px 0;padding:2px 0 2px 10px;border-left:3px solid " + link + ";color:" + textColor + ";}hr{border:0;border-top:1px solid " + border + ";margin:12px 0;}" +
        "code{font-family:monospace;font-size:12px;background:" + codeBg + ";padding:2px 5px;border-radius:5px;}pre{white-space:pre-wrap;word-break:break-word;background:" + codeBg + ";padding:10px;border-radius:9px;overflow-x:auto;}pre code{background:transparent;padding:0;}a{color:" + link + ";text-decoration:none;}strong{font-weight:700;}" +
        "</style></head><body>" + htmlContent + "</body></html>";
}


interface GroupSelectCallback {
    void onSelected(List selected);
}

void showGroupSelector(final Activity act, final int mode, final List initSelected, final GroupSelectCallback callback) {
    if (act == null || act.isFinishing()) return;
    try {
        final boolean isDark = isThemeDark(act);
        final int colorAccent = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
        final int colorOnSurface = tc(activity, "on_surface_strong");
        final int colorOnSurfaceVar = tc(activity, "on_surface_sub");
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(act, 24), dp(act, 24), dp(act, 24), dp(act, 24));
        TextView title = new TextView(act);
        title.setText(mode == 1 ? "选择好友" : mode == 2 ? "选择群聊" : "选择群聊与好友");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(colorOnSurface);
        root.addView(title);
        final TextView summary = new TextView(act);
        summary.setText("正在加载…");
        summary.setTextSize(14);
        summary.setTextColor(colorOnSurfaceVar);
        summary.setPadding(0, dp(act, 4), 0, dp(act, 16));
        root.addView(summary);
        final EditText search = makeInput(act, mode == 1 ? "搜索昵称或QQ号" : mode == 2 ? "搜索群名或群号" : "搜索昵称或群名或QQ号", null);
        root.addView(search);
        search.setVisibility(View.GONE);
        FrameLayout listFrame = new FrameLayout(act);
        root.addView(listFrame, new LinearLayout.LayoutParams( - 1, 0, 1f));
        final ListView lv = new ListView(act);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setClipToPadding(false);
        lv.setPadding(0, dp(act, 8), 0, dp(act, 8));
        listFrame.addView(lv, new FrameLayout.LayoutParams( - 1, - 1));
        final LinearLayout loading = createModernLoading(act, isDark);
        listFrame.addView(loading, new FrameLayout.LayoutParams( - 2, - 2, Gravity.CENTER));
        final TextView emptyView = new TextView(act);
        emptyView.setTextSize(14);
        emptyView.setTextColor(colorOnSurfaceVar);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        listFrame.addView(emptyView, new FrameLayout.LayoutParams( - 1, - 1));
        LinearLayout row1 = new LinearLayout(act);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.END);
        row1.setPadding(0, dp(act, 12), 0, dp(act, 16));
        TextView bCancelAll = makeChip(act, "取消全选", false, 1);
        TextView bReverse = makeChip(act, "反选", false, 1);
        TextView bSelectAll = makeChip(act, "全选", false, 1);
        row1.addView(bCancelAll);
        LinearLayout.LayoutParams lpRev = new LinearLayout.LayoutParams( - 2, - 2);
        lpRev.leftMargin = dp(act, 8);
        row1.addView(bReverse, lpRev);
        LinearLayout.LayoutParams lpAll = new LinearLayout.LayoutParams( - 2, - 2);
        lpAll.leftMargin = dp(act, 8);
        row1.addView(bSelectAll, lpAll);
        root.addView(row1);
        LinearLayout row2 = new LinearLayout(act);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.END);
        row2.setPadding(0, dp(act, 8), 0, 0);
        TextView btnCancel = createButton(act, "取消", colorAccent, Color.TRANSPARENT, 14f, 0, 16, 12, false, 0, 0, null);
        TextView btnConfirm = createButton(act, "确定", colorAccent, Color.TRANSPARENT, 14f, 0, 16, 12, true, 0, 0, null);
        row2.addView(btnCancel);
        row2.addView(btnConfirm);
        root.addView(row2);
        final AlertDialog dialog = new AlertDialog.Builder(act,
            isDark ? AlertDialog
                .THEME_DEVICE_DEFAULT_DARK : AlertDialog
                .THEME_DEVICE_DEFAULT_LIGHT)
                .setView(root)
                .create();
        dialog.show();
        applyUiTheme(act, dialog, 0);
        final ArrayList allItems = new ArrayList();
        final ArrayList display = new ArrayList();
        final Set selected = new HashSet(initSelected != null ? initSelected : new ArrayList());
        final boolean[]closed = new boolean[] {
            false
        };
        final Runnable[]renderTask = new Runnable[1];
        final int[]visibleLimit = new int[] {
            64
        };
        final int[]fullDisplayCount = new int[] {
            0
        };
        final boolean[] loadFinished = new boolean[] {false};
        final int[] pending = new int[] {(mode == 1 || mode == 2) ? 1 : 2};
        final int listBatchSize = 48;
        BaseAdapter adapter = new BaseAdapter() {
            public int getCount() {
                return Math.min(display.size(), visibleLimit[0]);
            }
            public Object getItem(int p) {
                return display.get(p);
            }
            public long getItemId(int p) {
                return p;
            }
            public View getView(final int pos, View cv, ViewGroup parent) {
                try {
                    LinearLayout item;
                    LinearLayout box;
                    CheckBox cb;
                    TextView tv;
                    TextView sub;
                    if (!(cv instanceof LinearLayout)) cv = null;
                    if (cv == null) {
                        item = new LinearLayout(act);
                        item.setOrientation(LinearLayout.HORIZONTAL);
                        item.setGravity(Gravity.CENTER_VERTICAL);
                        item.setPadding(dp(act, 8), dp(act, 12), dp(act, 8), dp(act, 12));
                        item.setBackground(createRippleBg(act, Color.TRANSPARENT, 0));
                        cb = new CheckBox(context);
                        cb.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 32), dp(act, 32)));
                        item.addView(cb);
                        box = new LinearLayout(act);
                        box.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(0, -2, 1);
                        boxLp.leftMargin = dp(act, 16);
                        tv = new TextView(act);
                        tv.setTextSize(16);
                        tv.setTextColor(colorOnSurface);
                        box.addView(tv);
                        sub = new TextView(act);
                        sub.setTextSize(12);
                        sub.setTextColor(colorOnSurfaceVar);
                        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
                        subLp.topMargin = dp(act, 2);
                        box.addView(sub, subLp);
                        item.addView(box, boxLp);
                        item.setTag(new Object[] { cb, tv, sub });
                    } else {
                        item = (LinearLayout) cv;
                        View checkChild = item.getChildAt(0);
                        View boxChild = item.getChildAt(1);
                        if (!(checkChild instanceof CheckBox) || !(boxChild instanceof LinearLayout) || ((LinearLayout) boxChild).getChildCount() < 2) {
                            item = new LinearLayout(act);
                            item.setOrientation(LinearLayout.HORIZONTAL);
                            item.setGravity(Gravity.CENTER_VERTICAL);
                            item.setPadding(dp(act, 8), dp(act, 12), dp(act, 8), dp(act, 12));
                            item.setBackground(createRippleBg(act, Color.TRANSPARENT, 0));
                            cb = new CheckBox(context);
                            cb.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 32), dp(act, 32)));
                            item.addView(cb);
                            box = new LinearLayout(act);
                            box.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(0, -2, 1);
                            boxLp.leftMargin = dp(act, 16);
                            tv = new TextView(act);
                            tv.setTextSize(16);
                            tv.setTextColor(colorOnSurface);
                            box.addView(tv);
                            sub = new TextView(act);
                            sub.setTextSize(12);
                            sub.setTextColor(colorOnSurfaceVar);
                            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
                            subLp.topMargin = dp(act, 2);
                            box.addView(sub, subLp);
                            item.addView(box, boxLp);
                            item.setTag(new Object[] { cb, tv, sub });
                        } else {
                            cb = (CheckBox) checkChild;
                            box = (LinearLayout) boxChild;
                            tv = (TextView) box.getChildAt(0);
                            sub = (TextView) box.getChildAt(1);
                        }
                    }
                    final Map m = (Map) display.get(pos);
                    final String uin = String.valueOf(m.get("uin"));
                    tv.setText(String.valueOf(m.get("show")));
                    sub.setText(uin);
                    cb.setOnCheckedChangeListener(null);
                    cb.setChecked(selected.contains(uin));
                    cb.setTag(uin);
                    cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        public void onCheckedChanged(CompoundButton b, boolean checked) {
                            String u = String.valueOf(b.getTag());
                            if (checked) selected.add(u);
                            else selected.remove(u);
                            summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                        }
                    });
                    item.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            Object[]h = (Object[]) v.getTag();
                            CheckBox c = (CheckBox) h[0];
                            c.setChecked(!c.isChecked());
                        }
                    });
                    return item;
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.getView] 异常: " + e);
                    return cv == null ? new TextView(act) : cv;
                }
            }
        };
        lv.setAdapter(adapter);
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int state) {
            }
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (total > 0 && first + visible >= visibleLimit[0] - 2 && visibleLimit[0] < display.size()) {
                    visibleLimit[0] = Math.min(display.size(), visibleLimit[0] + listBatchSize);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        final Runnable updateState = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                boolean hasData = !allItems.isEmpty();
                if (hasData) {
                    if (search.getVisibility() != View.VISIBLE) {
                        search.setAlpha(0f);
                        search.setVisibility(View.VISIBLE);
                        search.animate().alpha(1f).setDuration(180).start();
                    }
                } else {
                    search.setVisibility(View.GONE);
                }
                summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                if (display.isEmpty()) {
                    if (!loadFinished[0]) {
                        loading.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else if (!hasData) {
                        loading.setVisibility(View.GONE);
                        emptyView.setText("暂无数据");
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        loading.setVisibility(View.GONE);
                        emptyView.setText("无匹配结果");
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    loading.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                }
            }
        };
        final Runnable markDone = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                pending[0]--;
                if (pending[0] <= 0) {
                    loadFinished[0] = true;
                    updateState.run();
                }
            }
        };
        final Runnable refreshDisplay = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                try {
                    String q = search.getText().toString().trim().toLowerCase();
                    if (q.isEmpty() && selected.isEmpty() && fullDisplayCount[0] == display.size()) {
                        for (int i = display.size(); i < allItems.size(); i++) display.add(allItems.get(i));
                        fullDisplayCount[0] = allItems.size();
                    } else {
                        display.clear();
                        if (q.isEmpty()) {
                            if (selected.isEmpty()) {
                                display.addAll(allItems);
                                fullDisplayCount[0] = allItems.size();
                            } else {
                                ArrayList selectedFirst = new ArrayList();
                                ArrayList normal = new ArrayList();
                                for (Object o : allItems) {
                                    Map m = (Map) o;
                                    if (selected.contains(m.get("uin"))) selectedFirst.add(o);
                                    else normal.add(o);
                                }
                                display.addAll(selectedFirst);
                                display.addAll(normal);
                                fullDisplayCount[0] = -1;
                            }
                        } else {
                        for (Object o : allItems) {
                            Map m = (Map) o;
                            String n = String.valueOf(m.get("name"));
                            String u = String.valueOf(m.get("uin"));
                            String r = String.valueOf(m.get("remark"));
                            if (n.toLowerCase().contains(q) || u.contains(q) || r.toLowerCase().contains(q)) display.add(m);
                        }
                        ArrayList selectedFirst = new ArrayList();
                        ArrayList normal = new ArrayList();
                        for (Object o : display) {
                            Map m = (Map) o;
                            if (selected.contains(m.get("uin"))) selectedFirst.add(o);
                            else normal.add(o);
                        }
                        display.clear();
                        display.addAll(selectedFirst);
                        display.addAll(normal);
                        fullDisplayCount[0] = -1;
                        }
                    }
                    visibleLimit[0] = Math.min(64, display.size());
                    adapter.notifyDataSetChanged();
                    updateState.run();
                    if (!display.isEmpty() && visibleLimit[0] < display.size()) scheduleProgressiveListRender(adapter,
                        display,
                        visibleLimit,
                        closed,
                        dialog,
                        lv,
                        listBatchSize,
                        renderTask,
                        "showGroupSelector.render");
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.refresh] 异常: " + e);
                }
            }
        };
        final Runnable addReady = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                if (!allItems.isEmpty()) refreshDisplay.run();
            }
        };
        final Runnable addFriends = new Runnable() {
            public void run() {
                try {
                    List friends = (List) getAllFriend();
                    if (friends == null) friends = new ArrayList();
                    final int dataBatchSize = 160;
                    ArrayList batch = new ArrayList(dataBatchSize);
                    int batchIndex = 0;
                    for (Object obj : friends) {
                        if (obj == null) continue;
                        Map m = new HashMap();
                        String fUin = String.valueOf(obj.uin);
                        String fName = getUserName(fUin);
                        String fRemark = getUserRemark(fUin);
                        m.put("uin", fUin);
                        m.put("name", fName);
                        m.put("remark", fRemark);
                        m.put("show", (fRemark == null || fRemark.isEmpty()) ? fName : fRemark + "(" + fName + ")");
                        batch.add(m);
                        if (batch.size() >= dataBatchSize) {
                            final ArrayList postBatch = new ArrayList(batch);
                            batch.clear();
                            final long batchDelay = batchIndex++ * 8L;
                            uiHandler.postDelayed(new Runnable() {
                                public void run() {
                                    if (closed[0] || !dialog.isShowing()) return;
                                    try {
                                        allItems.addAll(postBatch);
                                        addReady.run();
                                    } catch (Throwable e) {
                                        traceLog("dialog_log", "[showGroupSelector.friends.add] 批量异常: " + e);
                                    }
                                }
                            }, batchDelay);
                        }
                    }
                    if (!batch.isEmpty()) {
                        final ArrayList postBatch = new ArrayList(batch);
                        final long lastBatchDelay = batchIndex++ * 8L;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (closed[0] || !dialog.isShowing()) return;
                                try {
                                    allItems.addAll(postBatch);
                                    addReady.run();
                                } catch (Throwable e) {
                                    traceLog("dialog_log", "[showGroupSelector.friends.add] 末批异常: " + e);
                                }
                            }
                        }, lastBatchDelay);
                    }
                    uiHandler.postDelayed(markDone, batchIndex * 8L + 16L);
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.friends] 异常: " + e);
                }
            }
        };
        final Runnable addGroups = new Runnable() {
            public void run() {
                try {
                    List groups = (List) getGroupList();
                    if (groups == null) groups = new ArrayList();
                    final int dataBatchSize = 160;
                    ArrayList batch = new ArrayList(dataBatchSize);
                    int batchIndex = 0;
                    for (Object obj : groups) {
                        if (obj == null) continue;
                        Map m = new HashMap();
                        String gUin = String.valueOf(obj.group);
                        String gShow = groupDisplayName(gUin);
                        m.put("uin", gUin);
                        m.put("name", gShow);
                        m.put("remark", getGroupRemark(gUin));
                        m.put("show", gShow);
                        batch.add(m);
                        if (batch.size() >= dataBatchSize) {
                            final ArrayList postBatch = new ArrayList(batch);
                            batch.clear();
                            final long batchDelay = batchIndex++ * 8L;
                            uiHandler.postDelayed(new Runnable() {
                                public void run() {
                                    if (closed[0] || !dialog.isShowing()) return;
                                    try {
                                        allItems.addAll(postBatch);
                                        addReady.run();
                                    } catch (Throwable e) {
                                        traceLog("dialog_log", "[showGroupSelector.groups.add] 批量异常: " + e);
                                    }
                                }
                            }, batchDelay);
                        }
                    }
                    if (!batch.isEmpty()) {
                        final ArrayList postBatch = new ArrayList(batch);
                        final long lastBatchDelay = batchIndex++ * 8L;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (closed[0] || !dialog.isShowing()) return;
                                try {
                                    allItems.addAll(postBatch);
                                    addReady.run();
                                } catch (Throwable e) {
                                    traceLog("dialog_log", "[showGroupSelector.groups.add] 末批异常: " + e);
                                }
                            }
                        }, lastBatchDelay);
                    }
                    uiHandler.postDelayed(markDone, batchIndex * 8L + 16L);
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.groups] 异常: " + e);
                }
            }
        };
        if (mode == 1 || mode == 3) ThreadPool.execute(addFriends);
        if (mode == 2 || mode == 3) ThreadPool.execute(addGroups);
        final Runnable searchRunnable = new Runnable() {
            public void run() {
                refreshDisplay.run();
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }
            public void afterTextChanged(Editable e) {
                search.removeCallbacks(searchRunnable);
                search.postDelayed(searchRunnable, 120);
            }
        });
        bCancelAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selected.clear();
                adapter.notifyDataSetChanged();
                summary.setText("已选 0 / 总 " + display.size());
            }
        });
        bReverse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Set old = new HashSet(selected);
                for (Object o: display) {
                    Map m = (Map) o;
                    String u = String.valueOf(m.get("uin"));
                    if (old.contains(u)) selected.remove(u);
                    else selected.add(u);
                }
                adapter.notifyDataSetChanged();
                summary.setText("已选 " + selected.size() + " / 总 " + display.size());
            }
        });
        bSelectAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (Object o: display) selected.add(String.valueOf(((Map) o).get("uin")));
                adapter.notifyDataSetChanged();
                summary.setText("已选 " + selected.size() + " / 总 " + display.size());
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (callback != null) callback.onSelected(new ArrayList(selected));
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                closed[0] = true;
                if (searchRunnable != null) search.removeCallbacks(searchRunnable);
                if (renderTask[0] != null && uiHandler != null) uiHandler.removeCallbacks(renderTask[0]);
                allItems.clear();
                display.clear();
            }
        });
    } catch (Throwable e) {
        traceLog("dialog_log", "[showGroupSelector] 异常: " + e);
    }
}

int parseDurationToSeconds(String input) {
    if (input == null || input.trim().isEmpty()) return 0;
    String s = input.trim();
    if (s.matches("\\d+")) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return 0;
        }
    }
    String[]parts = s.split(":");
    int len = parts.length;
    int seconds = 0;
    try {
        if (len >= 2) seconds += Integer.parseInt(parts[len - 2]);
        if (len >= 3) seconds += Integer.parseInt(parts[len - 3]) * 60;
        if (len >= 4) seconds += Integer.parseInt(parts[len - 4]) * 3600;
        if (len >= 5) seconds += Integer.parseInt(parts[len - 5]) * 86400;
    } catch (Throwable e) {
        traceLog("dialog_log", "[parseDurationToSeconds] 异常: " + e);
    }
    return seconds;
}

int timeValToSec(String val) {
    try {
        String n = val.replaceAll("[^0-9]", "");
        while (n.length() < 10) n = "0" + n;
        return Integer.parseInt(n.substring(0, 2)) * 86400 + Integer
                .parseInt(n .substring(2, 4)) * 3600 + Integer
                .parseInt(n .substring(4, 6)) * 60 + Integer
                .parseInt(n .substring(6, 8));
    } catch (Throwable e) {
        traceLog("dialog_log", "[timeValToSec] 异常: " + e);
        return 0;
    }
}

String secToTimeVal(int sec) {
    int d = sec / 86400;
    int h = (sec % 86400) / 3600;
    int m = (sec % 3600) / 60;
    int sc = sec % 60;
    if (d > 30) d = 30;
    return(d < 10 ? "0" + d : "" + d) + (h < 10 ? "0" + h : "" + h) + (m < 10 ? "0" + m : "" + m) + (sc < 10 ? "0" + sc : "" + sc) + "00";
}

void showTimePicker(Activity a, final EditText target, final String title) {
    a.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
                d.requestWindowFeature(1);
                d.getWindow().setBackgroundDrawable(new GradientDrawable());
                Window window = d.getWindow();
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.CENTER;
                window.setAttributes(params);
                FrameLayout outer = new FrameLayout(a);
                outer.setPadding(dp(a, 24), dp(a, 40), dp(a, 24), dp(a, 24));
                LinearLayout card = new LinearLayout(a);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(roundRect(Color.TRANSPARENT, dp(a, 16)));
                card.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), dp(a, 20));
                outer.addView(card);
                traceLog("dialog_log", "[showTimePicker] 开始, title=" + title);
                String finalTitle = (title == null || title.isEmpty()) ? "设置执行时间" : title;
                TextView titleTv = new TextView(a);
                titleTv.setText(finalTitle);
                titleTv.setTextSize(17);
                titleTv.setTextColor(tc(a, "on_surface"));
                titleTv.setPadding(0, 0, 0, dp(a, 12));
                card.addView(titleTv);
                final LinearLayout container = new LinearLayout(a);
                container.setOrientation(LinearLayout.VERTICAL);
                card.addView(container);
                final String[]val = {
                    ""
                };
                String current = target.getText().toString().replaceAll("[^0-9]", "");
                int totalSec = 0;
                try {
                    if (current.length() > 0) totalSec = Integer.parseInt(current);
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showTimePicker] 异常: " + e);
                }
                int initD = totalSec / 86400;
                int initH = (totalSec % 86400) / 3600;
                int initM = (totalSec % 3600) / 60;
                int initS = totalSec % 60;
                if (initD > 30) initD = 30;
                val[0] = (initD < 10 ? "0" + initD : "" + initD) + (initH < 10 ? "0" + initH : "" + initH) + (initM < 10 ? "0" + initM : "" + initM) + (initS < 10 ? "0" + initS : "" + initS) + "00";
                final int[]vs = {
                    initD, initH, initM, initS, 0
                };
                final android.widget.NumberPicker[]pickers = new android.widget.NumberPicker[5];
                final int[]mode = {
                    0
                };
                final String[]names = {
                    "键盘输入", "钟表选择", "快捷换算", "单项输入"
                };
                final Runnable[]build = new Runnable[1];
                build[0] = new Runnable() {
                    public void run() {
                        container.removeAllViews();
                        if (mode[0] == 0) {
                            EditText e = makeInput(a, "输入秒数(如3600=1小时)", null);
                            int initSec = timeValToSec(val[0]);
                            e.setText(String.valueOf(initSec));
                            e.addTextChangedListener(new android.text.TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }
                                public void afterTextChanged(android.text.Editable s) {
                                    try {
                                        val[0] = secToTimeVal(Integer.parseInt(s.toString().replaceAll("[^0-9]", "")));
                                    } catch (Throwable e) {
                                        traceLog("dialog_log", "[showTimePicker] 异常: " + e);
                                    }
                                }
                            });
                            container.addView(e);
                        } else if (mode[0] == 1) {
                            HorizontalScrollView hs = new HorizontalScrollView(a);
                            LinearLayout row = new LinearLayout(a);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            hs.addView(row);
                            container.addView(hs);
                            String[]lbls = {
                                "日", "时", "分", "秒", "毫秒"
                            };
                            try {
                                vs[0] = Integer.parseInt(val[0].substring(0, 2));
                                vs[1] = Integer.parseInt(val[0].substring(2, 4));
                                vs[2] = Integer.parseInt(val[0].substring(4, 6));
                                vs[3] = Integer.parseInt(val[0].substring(6, 8));
                                vs[4] = Integer.parseInt(val[0].substring(8, 10));
                            } catch (Throwable e) {
                                traceLog("dialog_log", "[showTimePicker] 异常: " + e);
                            }
                            for (int i = 0; i < 5; i++) {
                                LinearLayout col = new LinearLayout(a);
                                col.setOrientation(LinearLayout.VERTICAL);
                                col.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                                TextView l = new TextView(a);
                                l.setText(lbls[i]);
                                l.setTextSize(11);
                                l.setGravity(Gravity.CENTER);
                                l.setTextColor(tc(a, "on_surface_variant"));
                                col.addView(l);
                                NumberPicker p = new NumberPicker(a);
                                int max = (i == 0) ? 30 : (i == 1) ? 23 : (i == 4) ? 99 : 59;
                                p.setMinValue(0);
                                p.setMaxValue(max);
                                p.setValue(vs[i]);
                                pickers[i] = p;
                                final int idx = i;
                                p.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                                        vs[idx] = newVal;
                                        val[0] = (vs[0] < 10 ? "0" + vs[0] : "" + vs[0]) + (vs[1] < 10 ? "0" + vs[1] : "" + vs[1]) + (vs[2] < 10 ? "0" + vs[2] : "" + vs[2]) + (vs[3] < 10 ? "0" + vs[3] : "" + vs[3]) + (vs[4] < 10 ? "0" + vs[4] : "" + vs[4]);
                                    }
                                });
                                col.addView(p);
                                row.addView(col);
                            }
                        } else if (mode[0] == 2) {
                            EditText e = makeInput(a, "输入秒数(如3600=1小时)", null);
                            e.setInputType(InputType.TYPE_CLASS_NUMBER);
                            int initSec2 = timeValToSec(val[0]);
                            e.setText(String.valueOf(initSec2));
                            e.addTextChangedListener(new android.text.TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }
                                public void afterTextChanged(android.text.Editable s) {
                                    try {
                                        val[0] = secToTimeVal(Integer.parseInt(s.toString().replaceAll("[^0-9]", "")));
                                    } catch (Throwable e) {
                                        traceLog("dialog_log", "[onValueChange] 异常: " + e);
                                    }
                                }
                            });
                            container.addView(e);
                        } else {
                            GridLayout grid = new GridLayout(a);
                            grid.setColumnCount(2);
                            container.addView(grid);
                            String[]lbls = {
                                "日:", "时:", "分:", "秒:", "毫秒:"
                            };
                            final EditText[]ets = new EditText[5];
                            final boolean[]isSettingInitial = {
                                true
                            };
                            for (int i = 0; i < 5; i++) {
                                TextView l = new TextView(a);
                                l.setText(lbls[i]);
                                l.setTextColor(tc(a, "on_surface"));
                                grid.addView(l);
                                final EditText et = makeInput(a, i == 4 ? "000" : "00", null);
                                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                                et.setGravity(Gravity.CENTER);
                                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                                lp.width = dp(a, 60);
                                et.setLayoutParams(lp);
                                grid.addView(et);
                                ets[i] = et;
                                et.addTextChangedListener(new android.text.TextWatcher() {
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                    }
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                                    }
                                    public void afterTextChanged(android.text.Editable s) {
                                        if (isSettingInitial[0]) return;
                                        try {
                                            int d = Integer
                                                .parseInt(ets[0] .getText() .toString() .isEmpty() ? "0" : ets[0] .getText() .toString());
                                            int h = Integer
                                                .parseInt(ets[1] .getText() .toString() .isEmpty() ? "0" : ets[1] .getText() .toString());
                                            int m = Integer
                                                .parseInt(ets[2] .getText() .toString() .isEmpty() ? "0" : ets[2] .getText() .toString());
                                            int sc = Integer
                                                .parseInt(ets[3] .getText() .toString() .isEmpty() ? "0" : ets[3] .getText() .toString());
                                            int ms = Integer
                                                .parseInt(ets[4] .getText() .toString() .isEmpty() ? "0" : ets[4] .getText() .toString());
                                            val[0] = (d < 10 ? "0" + d : "" + d) + (h < 10 ? "0" + h : "" + h) + (m < 10 ? "0" + m : "" + m) + (sc < 10 ? "0" + sc : "" + sc) + (ms < 10 ? "0" + ms : "" + ms);
                                        } catch (Throwable e) {
                                            traceLog("dialog_log", "[afterTextChanged] 异常: " + e);
                                        }
                                    }
                                });
                            }
                            try {
                                ets[0].setText(val[0].substring(0, 2));
                                ets[1].setText(val[0].substring(2, 4));
                                ets[2].setText(val[0].substring(4, 6));
                                ets[3].setText(val[0].substring(6, 8));
                                ets[4].setText(val[0].substring(8, 10));
                            } catch (Throwable e) {
                                traceLog("dialog_log", "[afterTextChanged] 异常: " + e);
                            }
                            isSettingInitial[0] = false;
                        }
                    }
                };
                build[0].run();
                LinearLayout ctrl = new LinearLayout(a);
                ctrl.setOrientation(LinearLayout.HORIZONTAL);
                ctrl.setGravity(Gravity.CENTER_VERTICAL);
                ctrl.setPadding(0, dp(a, 16), 0, 0);
                card.addView(ctrl);
                TextView btnMode = createButton(a, " " + names[0], tc(a, "primary"), Color.TRANSPARENT, 12f, 0, 12, 8,
                    false, 0, 0, null);
                ctrl.addView(btnMode);
                View sp = new View(a);
                sp.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1.0f));
                ctrl.addView(sp);
                TextView btnCancel = createButton(a, "取消", tc(a, "on_surface_variant"), Color.TRANSPARENT, 14f, 0, 16,
                    8, false, 0, 0, null);
                ctrl.addView(btnCancel);
                TextView btnOk = createButton(a, "确定", Color.WHITE, tc(a, "primary"), 14f, 6, 16, 8, false, 0, 0, null);
                ctrl.addView(btnOk);
                btnCancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { animateDialogOut(d, null); } });
                btnMode.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mode[0] = (mode[0] + 1) % 4;
                        ((TextView) v).setText(" " + names[mode[0]]);
                        build[0].run();
                    }
                });
                btnOk.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            if (mode[0] == 1 && pickers[0] != null) {
                                for (int i = 0; i < 5; i++) {
                                    if (pickers[i] != null) {
                                        try {
                                            vs[i] = pickers[i].getValue();
                                        } catch (Throwable e) {
                                            traceLog("dialog_log", "[showTimePicker] picker[" + i + "] 错误: " + e);
                                        }
                                    }
                                }
                                val[0] = (vs[0] < 10 ? "0" + vs[0] : "" + vs[0]) + (vs[1] < 10 ? "0" + vs[1] : "" + vs[1]) + (vs[2] < 10 ? "0" + vs[2] : "" + vs[2]) + (vs[3] < 10 ? "0" + vs[3] : "" + vs[3]) + (vs[4] < 10 ? "0" + vs[4] : "" + vs[4]);
                            }
                            int totalSeconds = timeValToSec(val[0]);
                            traceLog("dialog_log",
                                "[showTimePicker] 确定点击, mode=" + mode[0] + ", val=" + val[0] + ", seconds=" + totalSeconds + ", targetNull=" + (target == null));
                            target.setText(String.valueOf(totalSeconds));
                            animateDialogOut(d, null);
                        } catch (Throwable e) {
                            traceLog("dialog_log", "[showTimePicker] ok click 错误: " + e);
                            try {
                                d.dismiss();
                            } catch (Throwable e2) {
                                traceLog("dialog_log", "[onClick] 异常: " + e2);
                            }
                        }
                    }
                });
                d.setContentView(outer);
                d.getWindow().setLayout(Math.min(dp(a, 400), a.getResources().getDisplayMetrics().widthPixels - dp(a, 32)), - 2);
                d.show();
                applyUiTheme(a, d, 1);
                animateDialogIn(d);
            } catch (Throwable e) {
                traceLog("dialog_log", "[showTimePicker]" + e);
            }
        }
    });
}
