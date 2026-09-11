import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import android.os.Handler;
import android.os.Looper;

String extractValue(String pair, String prefix) {
    try {
        String value = pair.substring(prefix.length()).trim();
        if (value.startsWith("=")) value = value.substring(1).trim();
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)
            value = value.substring(1, value.length() - 1);
        return "null".equals(value) ? "未设置" : value;
    } catch (Throwable e) {
        return "获取失败";
    }
}

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
    try {
        long ts = Long.parseLong(timestamp);
        long currentTime = System.currentTimeMillis() / 1000;
        if (ts == 0) return "未禁言";
        if (ts > 4102358400L) return "永久禁言";
        if (ts > currentTime) return "禁言中，剩余: " + formatRemainingTime(ts - currentTime);
        return "未禁言";
    } catch (Throwable e) {
        return "未知";
    }
}

// formatRemainingTime 已统一到 api.java

public static String prettyPrint(Object obj) {
    if (obj == null) return "null";
    String input = obj.toString();
    StringBuilder output = new StringBuilder();
    int indentLevel = 0;
    final String indent = "  ";          // 每级缩进两个空格
    boolean inQuotes = false;             // 是否在双引号字符串内

    for (int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);

        // 处理双引号转义，切换引号状态（忽略转义过的引号）
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

// 辅助方法：添加指定数量的缩进
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
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
    if (activity == null) {
        traceLog("dialog_log", "[菜单] 菜单调用时无法获取Activity");
        return;
    }
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (!activity.isFinishing()) 长按消息菜单(activity, data);
            } catch (Throwable t) { traceLog("dialog_log", "[菜单] 异常: " + t); }
        }
    });
}


void showShutUpDialog(Activity activity, String qun, String uin, String nickName) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("设置禁言时长");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 8));
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
                LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
                inputRow.addView(input, inputLp);

                TextView btnTime = createButton(activity, "\u23F1", textColor, Color.TRANSPARENT, 16f, 6, 0, 0, false, 0, 0, null);
                btnTime.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { showTimePicker(activity, input, "输入禁言时长"); }
                });
                inputRow.addView(btnTime);

                root.addView(inputRow);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int seconds = parseDurationToSeconds(input.getText().toString());
                        if (ref[0] != null) ref[0].dismiss();
                        shutUp(qun, uin, seconds);
                        Toast(seconds == 0 ? "已解除禁言" : "禁言设置成功: " + formatRemainingTime(seconds));
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

public void showMuteAllDialog(Activity activity, String qun) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("全体禁言设置");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(titleView);

                LinearLayout inputRow = new LinearLayout(activity);
                inputRow.setOrientation(LinearLayout.HORIZONTAL);
                inputRow.setGravity(Gravity.CENTER_VERTICAL);

                final EditText input = makeInput(activity, "秒数 或 点⏱选择", null);
                input.setText("0");
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setTextSize(14);
                LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, -2, 1.0f);
                inputRow.addView(input, inputLp);

                TextView btnTime = createButton(activity, "\u23F1", textColor, Color.TRANSPARENT, 16f, 6, 0, 0, false, 0, 0, null);
                btnTime.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { showTimePicker(activity, input, "输入禁言时长"); }
                });
                inputRow.addView(btnTime);

                root.addView(inputRow);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView unmute = createButton(activity, "解禁", pc("#81C784"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                unmute.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try { shutUpAll(qun, false); Toast("已解除全体禁言"); } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        long totalSec = parseDurationToSeconds(input.getText().toString());
                        
                        if (ref[0] != null) ref[0].dismiss();
                        
                        if (totalSec > 0) {
                            try {
                                shutUpAll(qun, true);
                                Toast("已开启全体禁言: " + formatRemainingTime(totalSec));
                                final Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        try {
                                            shutUpAll(qun, false);
                                            Toast("全体禁言已自动解除");
                                        } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                                    }
                                }, totalSec * 1000L);
                            } catch (Throwable t) { Toast("操作失败"); }
                        } else {
                            try {
                                shutUpAll(qun, true);
                                Toast("已开启全体禁言");
                            } catch (Throwable t) { Toast("操作失败"); }
                        }
                    }
                });

                btnBox.addView(cancel);
                btnBox.addView(unmute);
                btnBox.addView(confirm);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
        }
    });
}

public void showZanDialog(Activity activity, String targetUin) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("为Ta点赞");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 4));
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

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int count = 50;
                        try { count = Integer.parseInt(input.getText().toString().trim()); } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
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

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) { Toast("弹窗显示失败"); }
        }
    });
}

public void showPaiDialog(Activity activity, String targetUin, String peerUin, int chatType) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("拍一拍");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 4));
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

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int count = 1;
                        try { count = Integer.parseInt(input.getText().toString().trim()); } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
                        if (count < 1) count = 1;
                        if (count > 200) count = 200;
                        if (ref[0] != null) ref[0].dismiss();
                        
                        final int finalCount = count;
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                for (int i = 0; i < finalCount; i++) {
                                    try {
                                        sendPai(targetUin, peerUin, chatType);
                                    } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
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

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) { Toast("弹窗显示失败"); }
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
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("加解密工具");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                final EditText input = new EditText(activity);
                input.setText(displayText);
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(13);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                input.setGravity(Gravity.TOP | Gravity.LEFT);
                input.setTypeface(Typeface.MONOSPACE);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(activity, 1), borderColor);
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

                LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 160));
                inputParams.setMargins(0, 0, 0, dp(activity, 12));
                input.setLayoutParams(inputParams);
                root.addView(input);

                String[] btnTags = new String[]{"base64_enc","base64_dec","hex_enc","hex_dec","unicode_enc","unicode_dec","url_enc","url_dec","caesar_enc","caesar_dec","aes_enc","aes_dec","des_enc","des_dec","binary","md5","sha256","xor","reverse","undo","redo","clear","send"};
                String[] btnNames = new String[]{"Base64加密","Base64解密","转Hex","Hex转字符","转Unicode","Unicode还原","URL编码","URL解码","凯撒+3","凯撒-3","AES加密","AES解密","DES加密","DES解密","转二进制","MD5摘要","SHA256","XOR异或","文本倒序","↩撤销","↪重做","清空","发送结果"};

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
                        if (cur.trim().isEmpty()) { Toast("请输入文本"); return; }
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
                                    else if ("caesar_dec".equals(tag)) result = caesarCipher(cur, -3);
                                    else if ("aes_enc".equals(tag)) result = aesEncrypt(cur);
                                    else if ("aes_dec".equals(tag)) result = aesDecrypt(cur);
                                    else if ("des_enc".equals(tag)) result = desEncrypt(cur);
                                    else if ("des_dec".equals(tag)) result = desDecrypt(cur);
                                    else if ("binary".equals(tag)) result = stringToBinary(cur);
                                    else if ("md5".equals(tag)) result = getHash(cur, "MD5");
                                    else if ("sha256".equals(tag)) result = getHash(cur, "SHA-256");
                                    else if ("xor".equals(tag)) result = xorCipher(cur);
                                    else if ("reverse".equals(tag)) result = reverseString(cur);
                                } catch (OutOfMemoryError e) { errTip = "内存不足"; }
                                catch (Throwable e) { errTip = "处理错误: " + e.getMessage(); }
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
                    TextView btn = createButton(activity, btnNames[i], btnTextColor, isDark ? pc("#33FFFFFF") : pc("#F0F0F0"), 11f, 8, 4, 10, false, 1, strokeColor, null);
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

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 16), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                TextView cancel = createButton(activity, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(cancel);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) { Toast("弹窗显示失败: " + e.getMessage()); }
        }
    });
}

public void showCodeConsoleDialog(Activity activity, Object data) {
    if (activity == null || activity.isFinishing()) return;
    if (data == null) { Toast("数据无效"); return; }
    String initialCode = "";
    try { initialCode = String.valueOf(data.getClass().getField("msg").get(data)); } catch (Throwable e) { traceLog("dialog_log", "[showCodeConsoleDialog] 异常: " + e); }
    final String finalCode = initialCode;
    final Object outerInterpreter = this.interpreter;

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);
                int successColor = isDark ? pc("#81C784") : pc("#2E7D32");
                int errorColor = isDark ? pc("#FF8A80") : pc("#C62828");

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("代码控制台");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                final TextView resultView = new TextView(activity);
                resultView.setText("等待执行...");
                resultView.setTextSize(12);
                resultView.setTextColor(textColor);
                resultView.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                GradientDrawable resultBg = new GradientDrawable();
                resultBg.setCornerRadius(dp(activity, 8));
                resultBg.setColor(inputBgColor);
                resultBg.setStroke(dp(activity, 1), borderColor);
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
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 140));
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
                LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 120));
                inputParams.setMargins(0, 0, 0, dp(activity, 16));
                input.setLayoutParams(inputParams);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(activity, 1), borderColor);
                input.setBackground(inputBg);
                root.addView(input);

                final AlertDialog[] ref = new AlertDialog[1];
                TextView execBtn = createButton(activity, "▶  执行代码", Color.WHITE, isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), 14f, 8, 24, 12, false, 0, 0, null);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                btnParams.gravity = Gravity.CENTER_HORIZONTAL;
                btnParams.setMargins(0, 0, 0, dp(activity, 16));
                execBtn.setLayoutParams(btnParams);
                execBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String code = input.getText().toString().trim();
                        if (code.isEmpty()) { Toast("请输入代码"); vibrate(activity, 20); return; }
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
                                    try {
                                        outerInterpreter.set("msg", data.getClass().getField("data").get(data));
                                        outerInterpreter.set("qun", data.getClass().getField("peerUin").get(data));
                                        outerInterpreter.set("uin", data.getClass().getField("userUin").get(data));
                                        outerInterpreter.set("type", data.getClass().getField("type").get(data));
                                        outerInterpreter.set("msgtype", data.getClass().getField("msgType").get(data));
                                    } catch (Throwable t) { traceLog("dialog_log", "[showCodeConsoleDialog] 异常: " + t); }
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
                btnBox.setGravity(Gravity.RIGHT);
                TextView cancel = createButton(activity, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                btnBox.addView(cancel);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) { Toast("弹窗显示失败"); }
        }
    });
}

public void showExpandedResultDialog(Activity activity, String content, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("完整内容");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                TextView contentView = new TextView(activity);
                contentView.setText(content != null ? content : "");
                contentView.setTextSize(13);
                contentView.setTextColor(textColor);
                contentView.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                contentView.setTypeface(Typeface.MONOSPACE);
                contentView.setTextIsSelectable(true);
                GradientDrawable contentBg = new GradientDrawable();
                contentBg.setCornerRadius(dp(activity, 8));
                contentBg.setColor(inputBgColor);
                contentBg.setStroke(dp(activity, 1), borderColor);
                contentView.setBackground(contentBg);

                ScrollView scrollView = new ScrollView(activity);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 400));
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scrollView.setLayoutParams(scrollParams);
                scrollView.addView(contentView);
                root.addView(scrollView);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT);

                TextView copyBtn = createButton(activity, "复制全部", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                copyBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cb != null) { cb.setText(content != null ? content : ""); Toast("已复制到剪贴板"); }
                        } catch (Throwable e) { Toast("复制失败"); }
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

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) { Toast("展开显示失败: " + e.getMessage()); }
        }
    });
}

public void showTitleDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));
                TextView titleView = new TextView(activity);
                titleView.setText("设置群头衔");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 8));
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
                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String t = input.getText().toString().trim();
                        if (ref[0] != null) ref[0].dismiss();
                        try {
                            setGroupMemberTitle(qun, uin, t);
                            Toast(t.isEmpty() ? "头衔已清除" : "头衔设置成功: " + t);
                            vibrate(activity, 32);
                        } catch (Throwable e) { Toast("设置头衔失败: " + e.getMessage()); }
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) { Toast("弹窗显示失败: " + e.getMessage()); }
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
                
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));
                
                TextView titleView = new TextView(activity);
                titleView.setText("设置管理员");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 8));
                root.addView(titleView);
                
                TextView sub = new TextView(activity);
                sub.setText(nickName + "(" + uin + ")");
                sub.setTextSize(14);
                sub.setTextColor(subTextColor);
                sub.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(sub);
                
                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                
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
                        } catch (Throwable e) { Toast("操作失败"); }
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
                        } catch (Throwable e) { Toast("操作失败"); }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                
                btnBox.addView(cancel);
                btnBox.addView(revoke);
                btnBox.addView(confirm);
                root.addView(btnBox);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
        }
    });
}

public void showChangeCardDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int borderColor = adjustAlpha(textColor, 0.3f);
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));
                TextView titleView = new TextView(activity);
                titleView.setText("修改群名片");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 8));
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
                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = createButton(activity, "确定", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String card = input.getText().toString().trim();
                        if (ref[0] != null) ref[0].dismiss();
                        try {
                            changeMemberName(qun, uin, card);
                            Toast(card.isEmpty() ? "群名片已清除" : "群名片修改成功");
                            vibrate(activity, 32);
                        } catch (Throwable e) { Toast("修改失败: " + e.getMessage()); }
                    }
                });
                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable e) { Toast("弹窗显示失败: " + e.getMessage()); }
        }
    });
}

void showMsgDataPaeseDialog(Activity act, Object data) {
    if (act == null || act.isFinishing()) return;
    
    ThreadPool.execute(new Runnable() {
        public void run() {
            StringBuilder sb = new StringBuilder();
            
            // ===== MsgData 层 =====
            sb.append("# MsgData 层\n\n");
            
            try { sb.append("**消息文本**：").append(data.msg).append("\n\n**String msg = data.msg;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
            try { sb.append("**发送者QQ**：").append(data.userUin).append("\n\n**String userUin = data.userUin;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
            try { sb.append("**聊天对象QQ**：").append(data.peerUin).append("\n\n**String peerUin = data.peerUin;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
            try { sb.append("**聊天类型**：").append(data.type).append("\n\n**int type = data.type;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
            try { sb.append("**消息类型**：").append(data.msgType).append("\n\n**int msgType = data.msgType;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
            try { sb.append("**消息ID**：").append(data.msgId).append("\n\n**long msgId = data.msgId;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
            try { sb.append("**发送时间**：").append(data.time > 0 ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(data.time * 1000)) : "未知").append("\n\n**long time = data.time;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
            try { sb.append("**本地路径**：").append(data.path).append("\n\n**String path = data.path;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }

            // ===== MsgRecord 层 =====
            try {
                if (data.data != null) {
                    sb.append("# MsgRecord 层\n\n");
                    Object msg = data.data;
                    
                    try { sb.append("**发送者Uid**：").append(msg.senderUid).append("\n\n**String senderUid = data.data.senderUid;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                    try { sb.append("**发送者昵称**：").append(msg.sendNickName).append("\n\n**String sendNickName = data.data.sendNickName;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                    try { sb.append("**群名片**：").append(msg.sendMemberName).append("\n\n**String sendMemberName = data.data.sendMemberName;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                    try { sb.append("**对象Uid**：").append(msg.peerUid).append("\n\n**String peerUid = data.data.peerUid;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                    try { sb.append("**对象名称**：").append(msg.peerName).append("\n\n**String peerName = data.data.peerName;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                    try { sb.append("**消息序列号**：").append(msg.msgSeq).append("\n\n**long msgSeq = data.data.msgSeq;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                    try { sb.append("**发送状态**：").append(msg.sendStatus).append("\n\n**int sendStatus = data.data.sendStatus;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                    
                    // ===== Elements 层 =====
                    if (msg.elements != null && !msg.elements.isEmpty()) {
                        sb.append("# 消息元素 \n\n");
                        int i = 0;
                        for (Object el : msg.elements) {
                            if (el == null) continue;
                            sb.append("元素 [").append(i++).append("]\n\n");
                            
                            try { sb.append("**元素类型**：").append(el.elementType).append("\n\n**int elementType = element.elementType;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }

                            // 文本元素
                            if (el.elementType == 1 && el.textElement != null) {
                                Object text = el.textElement;
                                try { sb.append("**文本内容**：").append(text.content).append("\n\n**String content = element.textElement.content;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                                try { sb.append("**艾特类型**：").append(text.atType).append("\n\n**int atType = element.textElement.atType;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                            }
                            
                            // 图片元素
                            if (el.elementType == 2 && el.picElement != null) {
                                Object pic = el.picElement;
                                try { sb.append("**图片URL**：").append(pic.sourcePath).append("\n\n**String sourcePath = element.picElement.sourcePath;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                                try { sb.append("**图片尺寸**：").append(pic.picWidth).append("x").append(pic.picHeight).append("\n\n**int picWidth = element.picElement.picWidth;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                            }
                            
                            // 语音元素
                            if (el.elementType == 4 && el.pttElement != null) {
                                Object ptt = el.pttElement;
                                try { sb.append("**语音时长**：").append(ptt.duration).append("秒\n\n**int duration = element.pttElement.duration;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                            }
                            
                            // 视频元素
                            if (el.elementType == 5 && el.videoElement != null) {
                                Object video = el.videoElement;
                                try { sb.append("**视频文件**：").append(video.fileName).append("\n\n**String fileName = element.videoElement.fileName;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
                            }
                            
                            // 表情元素
                            if (el.elementType == 6 && el.faceElement != null) {
                                Object face = el.faceElement;
                                try { sb.append("**表情内容**：").append(face.faceText).append("\n\n**String faceText = element.faceElement.faceText;**").append("\n\n"); } catch (Throwable t) { traceLog("dialog_log", "[showMsgDataPaeseDialog] 异常: " + t); }
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
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
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

                GradientDrawable contentBg = new GradientDrawable();
                contentBg.setCornerRadius(dp(act, 8));
                contentBg.setColor(inputBgColor);
                contentBg.setStroke(dp(act, 1), borderColor);
                contentView.setBackground(contentBg);

                ScrollView scroll = new ScrollView(act);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                scrollParams.setMargins(0, 0, 0, dp(act, 12));
                scroll.setLayoutParams(scrollParams);

                int maxHeight = dp(act, 400);
                scroll.addView(contentView);
                root.addView(scroll);

                contentView.post(new Runnable() {
                    public void run() {
                        int measured = contentView.getMeasuredHeight();
                        if (measured > maxHeight) {
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, maxHeight);
                            lp.setMargins(0, 0, 0, dp(act, 12));
                            scroll.setLayoutParams(lp);
                        }
                    }
                });

                final AlertDialog[] ref = new AlertDialog[1];

                LinearLayout btnBox = new LinearLayout(act);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT);

                TextView copyAll = new TextView(act);
                copyAll.setText("复制全部");
                copyAll.setTextSize(15);
                copyAll.setTextColor(isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"));
                copyAll.setPadding(dp(act, 16), dp(act, 12), dp(act, 16), dp(act, 12));
                copyAll.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            ClipboardManager cb = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cb != null) {
                                cb.setPrimaryClip(ClipData.newPlainText("text", finalText));
                                qqToast(2, "已复制到剪贴板");
                            }
                            if (ref[0] != null) ref[0].dismiss();
                        } catch (Throwable e) { Toast("复制失败: " + e.getMessage()); }
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

                AlertDialog.Builder builder = new AlertDialog.Builder(act, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();

                if (!act.isFinishing() && !(Build.VERSION.SDK_INT >= 17 && act.isDestroyed())) {
                    ref[0].show();
                    if (ref[0].getWindow() != null) applyDialogSize(act, ref[0].getWindow());
                    if (ref[0] != null && ref[0].getWindow() != null) applyUiTheme(act, ref[0], 0);
                }
            } catch (Throwable e) { Toast("显示弹窗失败: " + e.getMessage()); }
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
                
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));
                
                TextView titleView = new TextView(activity);
                titleView.setText("确认踢出成员");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 8));
                root.addView(titleView);
                
                TextView sub = new TextView(activity);
                sub.setText(nickName + "(" + uin + ")\n请选择踢出方式");
                sub.setTextSize(14);
                sub.setTextColor(subTextColor);
                sub.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(sub);
                
                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                
                TextView cancel = createButton(activity, "取消", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                
                TextView permanent = createButton(activity, "永久踢出", pc("#FF5252"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                permanent.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try { kickGroup(qun, uin, true); qqToast(2, "永久踢出操作已执行"); } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                
                TextView confirm = createButton(activity, "确认踢出", isDark ? pc("#FFB74D") : pc("#F57C00"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try { kickGroup(qun, uin, false); qqToast(2, "踢出操作已执行"); } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                
                btnBox.addView(cancel);
                btnBox.addView(permanent);
                btnBox.addView(confirm);
                root.addView(btnBox);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);
            } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
        }
    });
}

void showRawMessageDialog(Object msgData) {
    try {
        String rawText = "获取失败";
        if (msgData != null) {
            rawText = prettyPrint(msgData);
        }
		showCopyConfirmDialog(getNowActivity(), "原始消息", rawText, isThemeDark(getNowActivity()));
    } catch (Throwable e) {
        showCopyConfirmDialog(getNowActivity(), "原始消息", "获取失败: " + e.getMessage(), isThemeDark(getNowActivity()));
    }
}

void showAtListDialog(Object atListData) {
    Activity activity = getNowActivity();
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
    
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("艾特列表");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                LinearLayout listContainer = new LinearLayout(activity);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                final LinearLayout loading = createModernLoading(activity, isDark);
                listContainer.addView(loading);

                ScrollView scrollView = new ScrollView(activity);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scrollView.setLayoutParams(scrollParams);
                scrollView.addView(listContainer);
                root.addView(scrollView);

                final AlertDialog[] ref = new AlertDialog[1];
                TextView closeBtn = createButton(activity, "关闭", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT);
                btnBox.addView(closeBtn);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
                                            try { nick = String.valueOf(card.strNick); } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                                        }
                                    } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
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
                                    } catch (Throwable t) { traceLog("dialog_log", "[showAtListDialog] 异常: " + t); }
                                }
                            });
                        } catch (Throwable e) {
                            Toast("获取艾特列表失败");
                        }
                    }
                });
            } catch (Throwable e) { Toast("弹窗显示失败"); }
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
    loadingText.setTextColor(isDark ? pc("#99EFEFEF") : pc("#99000000"));
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
                    try { sb.append("群名称: ").append(String.valueOf(troopInfo.troopname)).append("\n"); } catch (Throwable ignored) { sb.append("群名称: 获取失败\n"); }
                    try { sb.append("群主QQ: ").append(String.valueOf(troopInfo.troopowneruin)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored); }
                    try { sb.append("群人数: ").append(String.valueOf(troopInfo.wMemberNum)).append(" / ").append(String.valueOf(troopInfo.wMemberMax)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored); }
                    try { 
                        long createTime = Long.parseLong(String.valueOf(troopInfo.troopCreateTime));
                        if (createTime > 0) sb.append("创建时间: ").append(timestampToDate(createTime * 1000)).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored); }
                    try { 
                        String q = String.valueOf(troopInfo.joinTroopQuestion);
                        if (q != null && !q.isEmpty() && !q.equals("null")) sb.append("进群问题: ").append(q).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored); }
                    try {
                        String a = String.valueOf(troopInfo.joinTroopAnswer);
                        if (a != null && !a.isEmpty() && !a.equals("null")) sb.append("进群答案: ").append(a).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored); }
                    try {
                        String memo = String.valueOf(troopInfo.troopmemo);
                        if (memo != null && !memo.isEmpty() && !memo.equals("null")) sb.append("\n群公告:\n").append(memo).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showGroupInfoDialog] 异常: " + ignored); }
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
                uiHandler.post(new Runnable() { public void run() { Toast("获取群信息失败"); } });
            }
        }
    });
}

void showMemberInfoDialog(Activity activity, String peerUin, String userUin, int chatType) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                StringBuilder sb = new StringBuilder();
                
                Object card = null;
                Object member = null;
                Object troopInfo = null;
                Object friend = null;
                try {
                    card = GetCard(userUin);
                    if (chatType == 2) {
                        member = getMemberInfo(peerUin, userUin);
                        troopInfo = findTroopInfo(peerUin);
                    }
                    friend = GetCard(userUin);
                } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }

                sb.append("QQ: ").append(userUin).append("\n");
                
                String nick = "未知";
                if (card != null) {
                    try {
                        String cn = String.valueOf(card.strNick);
                        if(cn != null && !cn.contains("*") && !cn.equals("null")) nick = cn;
                    } catch (Throwable t) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + t); }
                }
                if ("未知".equals(nick) && member != null) {
                    try {
                        String mn = String.valueOf(member.uinName);
                        if (mn != null && !mn.isEmpty() && !mn.contains("*") && !mn.equals("null")) nick = mn;
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                }
                sb.append("昵称: ").append(nick).append("\n");
                
                if (friend != null) {
                    try {
                        String rm = String.valueOf(friend.remark);
                        if (rm != null && !rm.isEmpty() && !rm.equals("null")) sb.append("备注: ").append(rm).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                }
                
                if (card != null) {
                    try {
                        String qid = String.valueOf(card.qid);
                        if (qid != null && !qid.isEmpty() && !qid.equals("null")) sb.append("QID: ").append(qid).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        int age = (int) card.age;
                        if (age > 0) sb.append("年龄: ").append(age).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { 
                        String loc = "";
                        String ctry = String.valueOf(card.strCountry);
                        if(ctry != null && !ctry.isEmpty() && !ctry.equals("null")) loc += ctry + " ";
                        String prov = String.valueOf(card.strProvince);
                        if(prov != null && !prov.isEmpty() && !prov.equals("null")) loc += prov + " ";
                        String city = String.valueOf(card.strCity);
                        if(city != null && !city.isEmpty() && !city.equals("null")) loc += city;
                        loc = loc.trim();
                        if (!loc.isEmpty()) sb.append("地区: ").append(loc).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        String school = String.valueOf(card.strSchool);
                        if (school != null && !school.isEmpty() && !school.equals("null")) sb.append("学校: ").append(school).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        String company = String.valueOf(card.strCompany);
                        if (company != null && !company.isEmpty() && !company.equals("null")) sb.append("公司: ").append(company).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        String email = String.valueOf(card.strEmail);
                        if (email != null && !email.isEmpty() && !email.equals("null")) sb.append("邮箱: ").append(email).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        String sign = String.valueOf(card.strSign);
                        if (sign != null && !sign.isEmpty() && !sign.equals("null")) sb.append("签名: ").append(sign).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                }
                
                int qqLevel = 0;
                if (card != null) {
                    try { qqLevel = (int) card.iQQLevel; } catch (Throwable t) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + t); }
                }
                if (qqLevel == 0 && member != null) {
                    try { qqLevel = (int) member.uinLevel; } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                }
                sb.append("QQ等级: Lv.").append(qqLevel).append("\n");
                
                if (card != null) {
                    try {
                        int iQQVipLevel = (int) card.iQQVipLevel;
                        if (iQQVipLevel > 0) sb.append("QQ会员: Lv.").append(iQQVipLevel).append("\n");
                    } catch (Throwable t) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + t); }
                    try {
                        int iSuperVipLevel = (int) card.iSuperVipLevel;
                        if (iSuperVipLevel > 0) sb.append("超级会员: Lv.").append(iSuperVipLevel).append("\n");
                    } catch (Throwable t) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + t); }
                    try {
                        long lVoteCount = (long) card.lVoteCount;
                        sb.append("名片赞: ").append(lVoteCount).append("\n");
                    } catch (Throwable t) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + t); }
                    try {
                        long lLoginDays = (long) card.lLoginDays;
                        sb.append("登录天数: ").append(lLoginDays).append("\n");
                    } catch (Throwable t) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + t); }
                    try {
                        boolean isForbid = (boolean) card.isForbidAccount;
                        if (isForbid) sb.append("账号状态: 已封禁\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                }
                
                if (chatType == 2 && member != null) {
                    try { sb.append("群名片: ").append(String.valueOf(member.uinName)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { sb.append("群等级: Lv.").append(String.valueOf(member.uinLevel)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { sb.append("角色: ").append(convertRole(String.valueOf(member.role))).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { 
                        long joinTime = Long.parseLong(String.valueOf(member.joinGroupTime));
                        if (joinTime > 0) sb.append("入群时间: ").append(timestampToDate(joinTime * 1000)).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        long lastTime = Long.parseLong(String.valueOf(member.lastActiveTime));
                        if (lastTime > 0) sb.append("最后活跃: ").append(timestampToDate(lastTime * 1000)).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        long gagTime = Long.parseLong(String.valueOf(member.gagTimeStamp));
                        sb.append("禁言状态: ").append(getGagStatus(String.valueOf(gagTime))).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                }
                
                if (chatType == 2 && troopInfo != null) {
                    try { sb.append("群号: ").append(peerUin).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { sb.append("群名: ").append(String.valueOf(troopInfo.troopname)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { sb.append("群主: ").append(String.valueOf(troopInfo.troopowneruin)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { sb.append("最大人数: ").append(String.valueOf(troopInfo.wMemberMax)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try { sb.append("当前人数: ").append(String.valueOf(troopInfo.wMemberNum)).append("\n"); } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                    try {
                        long createTime = Long.parseLong(String.valueOf(troopInfo.troopCreateTime));
                        if (createTime > 0) sb.append("创建时间: ").append(timestampToDate(createTime * 1000)).append("\n");
                    } catch (Throwable ignored) { traceLog("dialog_log", "[showMemberInfoDialog] 异常: " + ignored); }
                }
                
                final String result = sb.toString();
                final boolean isDark = isThemeDark(activity);
                uiHandler.post(new Runnable() {
                    public void run() {
                        showCopyConfirmDialog(activity, "详细信息", result, isDark);
                    }
                });
            } catch (Throwable e) { 
                uiHandler.post(new Runnable() {
                    public void run() { Toast("获取失败: " + e.getMessage()); }
                }); 
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
    // 正则提取第一个 [pic=...] 中的 URL
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

private void executeDownloadAndUpload(final String url, final String fileName,
                                      final String startMsg, final String succMsg) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            final String savePath = pluginPath + "/cache/" + fileName;
            traceLog("dialog_log", "[executeDownloadAndUpload] 开始下载: " + url);
            uiHandler.post(new Runnable() {
                public void run() {
                    Toast(startMsg);
                }
            });

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
                uiHandler.post(new Runnable() {
                    public void run() {
                        Toast("下载失败");
                    }
                });
                return;
            }

            traceLog("dialog_log", "[executeDownloadAndUpload] 下载完成，准备上传: " + savePath);
            uiHandler.post(new Runnable() {
                public void run() {
                    Toast("正在上传，请稍候...");
                }
            });

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
                int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                int accentColor = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
                int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                int borderColor = adjustAlpha(textColor, 0.3f);

                ScrollView scrollView = new ScrollView(activity);
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("Cookie & 请求工具");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 8));
                root.addView(titleView);

                final boolean[] isCkMode = new boolean[]{true};

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

                TextView postSwitchBtn = createButton(activity, "POST", accentColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);

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

                LinearLayout.LayoutParams dataParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
                resultScroll.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                resultScroll.addView(resultBody);
                resultScroll.setVisibility(View.GONE);

                resultCard.addView(resultScroll);
                root.addView(resultCard);

                final String[] currentResultText = new String[1];

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
                final CKInfo[] ckInfo = new CKInfo[1];

                String[] ckFormats = {"JSON", "Header", "Curl", "Fetch"};
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
                            if (idx == 0) text = "{\n  \"skey\": \"" + ckInfo[0].skey + "\",\n  \"p_skey\": \"" + ckInfo[0].pskey + "\",\n  \"bkn\": \"" + ckInfo[0].bkn + "\"\n}";
                            else if (idx == 1) text = "Cookie: " + ckInfo[0].fullCookie;
                            else if (idx == 2) text = "curl \"https://" + ckInfo[0].domain + "\" \\\n  -H \"Cookie: " + ckInfo[0].fullCookie + "\"";
                            else if (idx == 3) text = "fetch(\"https://" + ckInfo[0].domain + "\", { headers: { \"Cookie\": \"" + ckInfo[0].fullCookie + "\" } })";

                            resultBody.removeAllViews();
                            TextView tv = new TextView(activity);
                            tv.setTextSize(13);
                            tv.setTextColor(isDark ? pc("#99EFEFEF") : pc("#99000000"));
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

                String[] postTools = {"复制URL", "复制数据"};
                for (int i = 0; i < postTools.length; i++) {
                    final String name = postTools[i];
                    TextView btn = createButton(activity, name, subTextColor, Color.TRANSPARENT, 13f, 0, 12, 8, false, 0, 0, null);
                    btn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            try {
                                if ("复制URL".equals(name)) {
                                    String url = postUrlEt.getText().toString();
                                    if (!url.isEmpty()) {
                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = android.content.ClipData.newPlainText("URL", url);
                                        clipboard.setPrimaryClip(clip);
                                        Toast("URL已复制");
                                    } else {
                                        Toast("URL为空");
                                    }
                                } else if ("复制数据".equals(name)) {
                                    String data = postDataEt.getText().toString();
                                    if (!data.isEmpty()) {
                                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                        android.content.ClipData clip = android.content.ClipData.newPlainText("POST数据", data);
                                        clipboard.setPrimaryClip(clip);
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
                            contentTv.setTextColor(isDark ? pc("#99EFEFEF") : pc("#99000000"));
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

                            try {
                                Object skeyObj = getSkey();
                                String skey = skeyObj != null ? String.valueOf(skeyObj) : "";
                                if (skey.isEmpty()) throw new Exception("skey为空");

                                Object pskeyObj = getPskey(domainEt.getText().toString().trim());
                                String pskey = pskeyObj != null ? String.valueOf(pskeyObj) : "";
                                if (pskey.isEmpty()) throw new Exception("p_skey为空");

                                String cookie = "p_skey=" + pskey +
                                        "; skey=" + skey +
                                        "; uin=o" +qq +" p_uin=o" +qq ;

                                String result = httpPost(url, cookie, data);

                                String display = "POST完整返回：\n\n" + (result.isEmpty() ? "(无返回内容)" : result);
                                currentResultText[0] = display;

                                resultBody.removeAllViews();
                                TextView tv = new TextView(activity);
                                tv.setText(display);
                                tv.setTextSize(13);
                                tv.setTextColor(isDark ? pc("#99EFEFEF") : pc("#99000000"));
                                tv.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
                                tv.setTextIsSelectable(true);
                                resultBody.addView(tv);

                                resultScroll.setVisibility(View.VISIBLE);
                                arrowD.setDirection(true);
                                arrowIv.setImageDrawable(arrowD);

                            } catch (Throwable e) {
                                Toast("POST失败: " + e.getMessage());
                            }
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
    View createAudioPlayer(Activity activity, final MediaPlayer[] player, int textColor, int inputBgColor, int borderColor,
                           final Handler uiHandler, final Runnable[] updateProgressTaskRef) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(activity, 8), 0, dp(activity, 8));

        // 进度条
        final SeekBar seekBar = new SeekBar(activity);
        applyUiSeekBar(seekBar, activity, tc(activity, "primary"));
        seekBar.setMax(1000); // 使用千分比以便精细控制
        container.addView(seekBar);

        // 时间显示
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

        // 播放/暂停按钮（独立于进度条）
        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(activity, 8), 0, 0);

        TextView playBtn = createButton(activity, "播放", textColor, inputBgColor, 14f, 8, 8, 8, false, 1, borderColor, null);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
        playBtn.setLayoutParams(p);
        btnRow.addView(playBtn);
        container.addView(btnRow);

        // 进度更新任务
        final Runnable updateProgress = new Runnable() {
            public void run() {
                if (player[0] != null && player[0].isPlaying()) {
                    int current = player[0].getCurrentPosition();
                    int duration = player[0].getDuration();
                    if (duration > 0) {
                        int progress = (int) ((long) current * 1000 / duration);
                        seekBar.setProgress(progress);
                        currentTime.setText(formatTime(current));
                        totalTime.setText(formatTime(duration));
                    }
                    uiHandler.postDelayed(this, 200);
                } else {
                    // 停止更新
                    uiHandler.removeCallbacks(this);
                }
            }
        };
        if (updateProgressTaskRef != null) {
            updateProgressTaskRef[0] = updateProgress;
        }

        // 播放按钮点击事件
        playBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (player[0] != null && player[0].isPlaying()) {
                    // 暂停
                    player[0].pause();
                    playBtn.setText("播放");
                    uiHandler.removeCallbacks(updateProgress);
                } else {
                    // 如果播放器为空或已释放，重新创建
                    if (player[0] == null) {
                        try {
                            player[0] = new MediaPlayer();
                            player[0].setDataSource(finalUrl); // 注意 finalUrl 需要在此作用域内可访问
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
                        // 继续播放
                        player[0].start();
                        playBtn.setText("暂停");
                        uiHandler.post(updateProgress);
                    }
                }
            }
        });

        // 进度条拖动监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean fromUser = false;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.fromUser = fromUser;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 拖动时暂停进度更新
                uiHandler.removeCallbacks(updateProgress);
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player[0] != null && fromUser) {
                    int duration = player[0].getDuration();
                    if (duration > 0) {
                        int seekTo = (int) ((long) seekBar.getProgress() * duration / 1000);
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

    // 辅助方法：将毫秒格式化为 mm:ss
    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec;
    }
}

public void showExtractAudioDialog(Activity activity, Object data) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String textContent = "";
                if (data != null) {
                    try { textContent = String.valueOf(data.msg); } catch(Throwable t) { textContent = String.valueOf(data); }
                }

                // 优化链接提取：匹配常见音频扩展名
                final List urls = new ArrayList();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("https?://[^\\s\"']+\\.(mp3|m4a|aac|ogg|wav|flac|opus|amr|wma|mpeg)(\\?[^\\s]*)?", Pattern.CASE_INSENSITIVE).matcher(textContent);
                while(m.find()) {
                    String u = m.group().replace("\\/", "/");
                    urls.add(u);
                }
                // 如果没有匹配扩展名，再尝试包含 audio/music 关键词的链接
                if (urls.isEmpty()) {
                    m = java.util.regex.Pattern.compile("https?://[^\\s\"']+(audio|music|fm)[^\\s\"']*", Pattern.CASE_INSENSITIVE).matcher(textContent);
                    while(m.find()) {
                        String u = m.group().replace("\\/", "/");
                        urls.add(u);
                    }
                }

                final String finalUrl = urls.isEmpty() ? "" : (String)urls.get(0);

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            boolean isDark = isThemeDark(activity);
                            int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                            int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
                            int inputBgColor = isDark ? pc("#1AFFFFFF") : pc("#0D000000");
                            int borderColor = adjustAlpha(textColor, 0.3f);

                            LinearLayout root = new LinearLayout(activity);
                            root.setOrientation(LinearLayout.VERTICAL);
                            root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                            TextView titleView = new TextView(activity);
                            titleView.setText("提取音频");
                            titleView.setTextSize(18);
                            titleView.setTypeface(null, Typeface.BOLD);
                            titleView.setTextColor(textColor);
                            titleView.setPadding(0, 0, 0, dp(activity, 12));
                            root.addView(titleView);

                            TextView urlView = new TextView(activity);
                            urlView.setText(finalUrl.isEmpty() ? "未检测到音频链接" : finalUrl);
                            urlView.setTextSize(13);
                            urlView.setTextColor(textColor);
                            urlView.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                            GradientDrawable bg = new GradientDrawable();
                            bg.setCornerRadius(dp(activity, 8));
                            bg.setColor(inputBgColor);
                            bg.setStroke(dp(activity, 1), borderColor);
                            urlView.setBackground(bg);
                            root.addView(urlView);

                            final MediaPlayer[] player = new MediaPlayer[1];
                            final Handler uiHandler = new Handler(Looper.getMainLooper());
                            final Runnable[] updateProgressTask = new Runnable[1]; // 用于更新进度条的循环

                            // 创建播放控制器（包含进度条、时间显示和播放/暂停按钮）
                            AudioBtnAdder adder = new AudioBtnAdder();
                            final View audioControlView;
                            if (!finalUrl.isEmpty()) {
                                audioControlView = adder.createAudioPlayer(activity, player, textColor, inputBgColor, borderColor, uiHandler, updateProgressTask);
                            } else {
                                audioControlView = new View(activity); // 空占位
                            }

                            LinearLayout btnGrid = new LinearLayout(activity);
                            btnGrid.setOrientation(LinearLayout.VERTICAL);
                            btnGrid.setPadding(0, dp(activity, 16), 0, 0);
                            if (!finalUrl.isEmpty()) {
                                btnGrid.addView(audioControlView);
                            }

                            // 添加功能按钮（发链接、发语音）
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
                                        } catch(Throwable t) { Toast("发送失败"); }
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
                                                    String savePath = pluginPath + "/cache/audio_" + System.currentTimeMillis() + ".mp3";
                                                    FutureTask downloadTask = new FutureTask(new Callable() {
                                                        public Boolean call() throws Exception {
                                                            return downloadFile(finalUrl, savePath, new ProgressCallback() {
                                                                public void onProgress(int progressVal) {}
                                                                public void onProgressTip(String tip) {}
                                                            });
                                                        }
                                                    });
                                                    ThreadPool.execute(downloadTask);
                                                    // 等待下载完成，最多15秒
                                                    boolean success = (Boolean) downloadTask.get(15000, TimeUnit.MILLISECONDS);
                                                    if (success) {
                                                        Object contact = data.contact;
                                                        sendPtt(contact, savePath);
                                                        uiHandler.post(new Runnable() { public void run() { Toast("语音发送成功"); } });
                                                    } else {
                                                        uiHandler.post(new Runnable() { public void run() { Toast("下载失败"); } });
                                                    }
                                                } catch (TimeoutException e) {
                                                    uiHandler.post(new Runnable() { public void run() { Toast("下载超时"); } });
                                                } catch (Throwable t) {
                                                    uiHandler.post(new Runnable() { public void run() { Toast("语音发送失败"); } });
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

                            final AlertDialog[] ref = new AlertDialog[1];
                            TextView closeBtn = createButton(activity, "关闭", subTextColor, Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                            closeBtn.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    // 停止播放并释放
                                    if (player[0] != null) {
                                        try { player[0].stop(); player[0].release(); } catch (Throwable t) { traceLog("dialog_log", "[downloadFile] 异常: " + t); }
                                        player[0] = null;
                                    }
                                    // 停止进度更新
                                    if (updateProgressTask[0] != null) {
                                        uiHandler.removeCallbacks(updateProgressTask[0]);
                                    }
                                    if (ref[0] != null) ref[0].dismiss();
                                }
                            });
                            root.addView(closeBtn);

                            AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            builder.setView(root);
                            ref[0] = builder.create();
                            ref[0].setOnDismissListener(new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface d) {
                                    if (player[0] != null) {
                                        try { player[0].stop(); player[0].release(); } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                                        player[0] = null;
                                    }
                                    if (updateProgressTask[0] != null) {
                                        uiHandler.removeCallbacks(updateProgressTask[0]);
                                    }
                                }
                            });
                            ref[0].show();
                            applyUiTheme(activity, ref[0], 0);
                        } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                    }
                });
            } catch(Throwable e) {
                uiHandler.post(new Runnable() { public void run() { Toast("提取音频异常"); } });
            }
        }
    });
}

LinearLayout createInfoRow(Activity activity, String label, String value, int colorText, int colorSubtext, boolean isDark) {
    LinearLayout row = new LinearLayout(activity);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setPadding(dp(activity, 4), dp(activity, 7), dp(activity, 4), dp(activity, 7));
    row.setClickable(true);

    int borderColor = adjustAlpha(colorText, 0.15f);
    final GradientDrawable rowBg = new GradientDrawable();
    rowBg.setCornerRadius(dp(activity, 6));
    rowBg.setColor(Color.TRANSPARENT);
    row.setBackground(rowBg);
    
    final TextView valueView = new TextView(activity);
    valueView.setText(value);
    valueView.setTextSize(13);
    valueView.setTextColor(colorText);
    valueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    row.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: rowBg.setColor(borderColor); row.setBackground(rowBg); return true;
                case MotionEvent.ACTION_UP: rowBg.setColor(Color.TRANSPARENT); row.setBackground(rowBg); v.performClick(); return true;
                case MotionEvent.ACTION_CANCEL: rowBg.setColor(Color.TRANSPARENT); row.setBackground(rowBg); return true;
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

FrameLayout createMemberInfoCard(Activity activity, String userUin, String peerUin, int chatType,
                                 boolean isDark, int colorText, int colorSubtext,
                                 String msgText, String fullMsgText, Object msgRecord, String nickName) {
    FrameLayout cardWrapper = new FrameLayout(activity);
    cardWrapper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    cardWrapper.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 8));

    GradientDrawable cardBg = new GradientDrawable();
    cardBg.setShape(GradientDrawable.RECTANGLE);
    cardBg.setCornerRadius(dp(activity, 16));
    cardBg.setColor(isDark ? pc("#FF252525") : pc("#FFF0F0F0"));
    cardBg.setStroke(dp(activity, 1), isDark ? pc("#33FFFFFF") : pc("#18000000"));

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
    LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 1));
    divParams.setMargins(0, dp(activity, 8), 0, dp(activity, 4));
    divider.setLayoutParams(divParams);
    divider.setBackgroundColor(isDark ? pc("#22FFFFFF") : pc("#15000000"));
    divider.setVisibility(View.GONE);
    cardContent.addView(divider);

    final LinearLayout detailInfo = new LinearLayout(activity);
    detailInfo.setOrientation(LinearLayout.VERTICAL);
    detailInfo.setVisibility(View.GONE);
    detailInfo.setPadding(0, dp(activity, 2), 0, 0);

    final LinearLayout groupRow = createInfoRow(activity, chatType == 2 ? "群昵称" : "备注", "加载中...", colorText, colorSubtext, isDark);
    
    String msgDisplay = (msgText != null && !msgText.trim().isEmpty()) 
        ? (msgText.length() > 100 ? msgText.substring(0, 100) + "..." : msgText) 
        : "(无文本内容)";
    final String finalFullMsgText = fullMsgText != null ? fullMsgText : "(无文本内容)";
    LinearLayout msgRow = new LinearLayout(activity);
    msgRow.setOrientation(LinearLayout.HORIZONTAL);
    msgRow.setPadding(dp(activity, 4), dp(activity, 7), dp(activity, 4), dp(activity, 7));
    msgRow.setClickable(true);
    int borderColor = adjustAlpha(colorText, 0.15f);
    final GradientDrawable msgRowBg = new GradientDrawable();
    msgRowBg.setCornerRadius(dp(activity, 6));
    msgRowBg.setColor(Color.TRANSPARENT);
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
    msgValueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    
    msgRow.addView(msgLabelView);
    msgRow.addView(msgValueView);
    
    msgRow.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: msgRowBg.setColor(borderColor); msgRow.setBackground(msgRowBg); return true;
                case MotionEvent.ACTION_UP: msgRowBg.setColor(Color.TRANSPARENT); msgRow.setBackground(msgRowBg); v.performClick(); return true;
                case MotionEvent.ACTION_CANCEL: msgRowBg.setColor(Color.TRANSPARENT); msgRow.setBackground(msgRowBg); return true;
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

    final boolean[] isExpanded = new boolean[]{false};
    final boolean[] isLoading = new boolean[]{false};

    if (nickName == null || nickName.isEmpty()) {
        ThreadPool.execute(new Runnable() {
            public void run() {
                String nick = userUin;
                try {
                    Object card = GetCard(userUin);
                    if (card != null) {
                        try { 
                            String strNick = String.valueOf(card.strNick); 
                            if (strNick != null && !strNick.isEmpty() && !strNick.contains("*") && !strNick.equals("null")) nick = strNick; 
                        } catch (Throwable t) { traceLog("dialog_log", "[onClick] 异常: " + t); }
                    }
                } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
                if (nick.equals(userUin) && msgRecord != null) {
                    try {
                        String fb = String.valueOf(msgRecord.sendNickName);
                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) nick = fb;
                    } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
                }
                final String finalNick = nick;
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try { nameView.setText(finalNick); } catch (Throwable t) { traceLog("dialog_log", "[createMemberInfoCard] 异常: " + t); }
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
                                        if (mn != null && !mn.isEmpty() && !mn.contains("*") && !mn.equals("null")) groupNick = mn;
                                    }
                                } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
                                if ("未设置".equals(groupNick) && msgRecord != null) {
                                    try {
                                        String fb = String.valueOf(msgRecord.sendMemberName);
                                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) groupNick = fb;
                                    } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
                                }
                                final String finalGroupNick = groupNick;

                                String gName = "获取失败";
                                try {
                                    Object info = findTroopInfo(peerUin);
                                    if (info != null) {
                                        String tn = String.valueOf(info.troopname);
                                        if (tn != null && !tn.equals("null")) gName = tn;
                                    }
                                } catch (Throwable ignored) { traceLog("dialog_log", "[onClick] 异常: " + ignored); }
                                final String finalGroupName = gName;

                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            ((TextView) groupRow.getChildAt(1)).setText(finalGroupNick);
                                        } catch (Throwable ignored) { traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored); }
                                        if (finalGroupNameRow != null) {
                                            try {
                                                ((TextView) finalGroupNameRow.getChildAt(1)).setText(finalGroupName);
                                            } catch (Throwable ignored) { traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored); }
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
                                        if (rm != null && !rm.isEmpty() && !rm.contains("*") && !rm.equals("null")) remark = rm;
                                    }
                                } catch (Throwable ignored) { traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored); }
                                if ("未设置".equals(remark) && msgRecord != null) {
                                    try {
                                        String fb = String.valueOf(msgRecord.sendRemarkName);
                                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) remark = fb;
                                    } catch (Throwable ignored) { traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored); }
                                }
                                final String finalRemark = remark;

                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            ((TextView) groupRow.getChildAt(1)).setText(finalRemark);
                                        } catch (Throwable ignored) { traceLog("dialog_log", "[createMemberInfoCard] 异常: " + ignored); }
                                        isLoading[0] = false;
                                    }
                                });
                            }
                        } catch (Throwable e) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() { isLoading[0] = false; }
                            });
                        }
                    }
                });
            }
        }
    });

    cardWrapper.addView(cardContent);
    return cardWrapper;
}

FrameLayout createMenuItem(Activity activity, String title, boolean isDark, int colorPrimary, int colorCardBg, Dialog parentDialog, Runnable callback, boolean skipDismiss) {
    FrameLayout itemWrapper = new FrameLayout(activity);
    itemWrapper.setLayoutParams(new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)));
    itemWrapper.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));

    int borderColor = colorPrimary;
    final GradientDrawable itemBg = new GradientDrawable();
    itemBg.setShape(GradientDrawable.RECTANGLE);
    itemBg.setCornerRadius(dp(activity, 12));
    itemBg.setColor(colorCardBg);
    itemBg.setStroke(dp(activity, 1), borderColor);

    LinearLayout itemContent = new LinearLayout(activity);
    itemContent.setOrientation(LinearLayout.VERTICAL);
    itemContent.setGravity(Gravity.CENTER);
    itemContent.setPadding(dp(activity, 12), dp(activity, 14), dp(activity, 12), dp(activity, 14));
    itemContent.setBackground(itemBg);

    final TextView titleView = new TextView(activity);
    titleView.setText(title);
    titleView.setTextSize(13);
    titleView.setTextColor(colorPrimary);
    titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    titleView.setGravity(Gravity.CENTER);
    titleView.setMaxLines(2);
    itemContent.addView(titleView);
    itemWrapper.addView(itemContent);

    itemWrapper.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: 
                    itemBg.setColor(borderColor); 
                    titleView.setTextColor(colorCardBg); 
                    itemContent.setBackground(itemBg); 
                    return true;
                case MotionEvent.ACTION_UP: 
                    itemBg.setColor(colorCardBg); 
                    titleView.setTextColor(colorPrimary); 
                    itemContent.setBackground(itemBg); 
                    v.performClick(); 
                    return true;
                case MotionEvent.ACTION_CANCEL: 
                    itemBg.setColor(colorCardBg); 
                    titleView.setTextColor(colorPrimary); 
                    itemContent.setBackground(itemBg); 
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

String[][] CATEGORY_COLOR_STRS = new String[6][3];
CATEGORY_COLOR_STRS[0][0] = "#2196F3";
CATEGORY_COLOR_STRS[0][1] = "#1976D2";
CATEGORY_COLOR_STRS[0][2] = "#BBDEFB";
CATEGORY_COLOR_STRS[1][0] = "#4CAF50";
CATEGORY_COLOR_STRS[1][1] = "#388E3C";
CATEGORY_COLOR_STRS[1][2] = "#C8E6C9";
CATEGORY_COLOR_STRS[2][0] = "#9C27B0";
CATEGORY_COLOR_STRS[2][1] = "#7B1FA2";
CATEGORY_COLOR_STRS[2][2] = "#E1BEE7";
CATEGORY_COLOR_STRS[3][0] = "#FF9800";
CATEGORY_COLOR_STRS[3][1] = "#F57C00";
CATEGORY_COLOR_STRS[3][2] = "#FFE0B2";
CATEGORY_COLOR_STRS[4][0] = "#00BCD4";
CATEGORY_COLOR_STRS[4][1] = "#0097A7";
CATEGORY_COLOR_STRS[4][2] = "#B2EBF2";
CATEGORY_COLOR_STRS[5][0] = "#607D8B";
CATEGORY_COLOR_STRS[5][1] = "#455A64";
CATEGORY_COLOR_STRS[5][2] = "#CFD8DC";

void addMenuItem(List list, String category, String title, Runnable callback) {
    list.add(new Object[]{category, title, callback});
}

int[] getCategoryColorInts(String category, boolean isDark) {
    int index = 5;
    if ("消息操作".equals(category)) index = 0;
    else if ("群管理".equals(category) || "好友信息".equals(category)) index = 1;
    else if ("互动功能".equals(category)) index = 2;
    else if ("工具".equals(category)) index = 3;
    else if ("其他".equals(category)) index = 4;
    else if ("设置".equals(category)) index = 5;
    String[] colorStrs = CATEGORY_COLOR_STRS[index];
    return new int[]{pc(isDark ? colorStrs[1] : colorStrs[0]), pc(isDark ? colorStrs[2] : colorStrs[1])};
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

    void setColor(int color) { paint.setColor(color); invalidateSelf(); }

    private void rebuildPath() {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        if (w <= 0 || h <= 0) return;
        float sw = Math.min(w, h) / 6f;
        paint.setStrokeWidth(sw);
        float cx = w / 2f;
        path.reset();
        if (isUp) { path.moveTo(sw, h * 0.65f); path.lineTo(cx, h * 0.35f); path.lineTo(w - sw, h * 0.65f); }
        else { path.moveTo(sw, h * 0.35f); path.lineTo(cx, h * 0.65f); path.lineTo(w - sw, h * 0.35f); }
    }

    protected void onBoundsChange(Rect bounds) { super.onBoundsChange(bounds); rebuildPath(); }

    public void draw(Canvas canvas) { canvas.drawPath(path, paint); }
    public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
    public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

void applySavedSortOrder(List catOrder, Map catItems, String savedOrder) {
    if (savedOrder == null || savedOrder.isEmpty()) return;
    try {
        List newCatOrder = new ArrayList();
        Map newCatItems = new LinkedHashMap();
        String[] cats = savedOrder.split("\\|");
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
                String[] itemNames = itemsPart.split(",");
                List sortedItems = new ArrayList();
                for (int j = 0; j < itemNames.length; j++) {
                    String targetName = itemNames[j];
                    for (int k = 0; k < origItems.size(); k++) {
                        Object[] item = (Object[]) origItems.get(k);
                        if (targetName.equals(item[1])) {
                            sortedItems.add(item);
                            break;
                        }
                    }
                }
                for (int k = 0; k < origItems.size(); k++) {
                    Object[] item = (Object[]) origItems.get(k);
                    boolean found = false;
                    for (int m = 0; m < sortedItems.size(); m++) {
                        Object[] si = (Object[]) sortedItems.get(m);
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
        return ((List)catOrder).stream()
            .map(new java.util.function.Function() {
                public Object apply(Object cat) {
                    List items = (List) catItems.get(cat);
                    String itemNames = ((List)items).stream()
                        .map(new java.util.function.Function() {
                            public Object apply(Object item) {
                                return (String) ((Object[])item)[1];
                            }
                        })
                        .collect(Collectors.joining(","));
                    return (String)cat + "=" + itemNames;
                }
            })
            .collect(Collectors.joining("|"));
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
    } catch (Throwable t) { traceLog("dialog_log", "[长按消息菜单] 异常: " + t); }
    if (nickName == null) {
        nickName = userUin;
    }

    int colorText = tc(activity, "on_surface");
    int colorSubtext = tc(activity, "on_surface_variant");
    int colorCardBg = getAdaptiveMenuItemBg(activity);
    int sheetBg = getAdaptiveSheetBg(activity);

    final Dialog bottomSheet = new Dialog(activity);
    bottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE);

    LinearLayout rootLayout = new LinearLayout(activity);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setBackgroundColor(sheetBg);

    FrameLayout headerCard = createMemberInfoCard(activity, userUin, peerUin, chatType, isDark, colorText, colorSubtext, quntext, quntext, msgRecord, nickName);
    rootLayout.addView(headerCard);

    ScrollView scrollView = new ScrollView(activity);
    scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 380)));
    scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    scrollView.setVerticalScrollBarEnabled(false);

    final LinearLayout menuContainer = new LinearLayout(activity);
    menuContainer.setOrientation(LinearLayout.VERTICAL);
    menuContainer.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 16));

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
    final Object qqmember = getMemberInfo(finalPeerUin, qq);
    final String role = qqmember.role;
    addMenuItem(menuItems, "消息操作", "复制内容", new Runnable() { public void run() { showCopyConfirmDialog(activity, "消息内容", finalQuntext, isDark); } });
    addMenuItem(menuItems, "消息操作", "复读加一", new Runnable() { public void run() { 复读(data); } });
    addMenuItem(menuItems, "消息操作", "撤回消息", new Runnable() { public void run() { recallMsg(finalChatType, finalPeerUin, finalMsgid); qqToast(2, "撤回操作已执行"); } });
    addMenuItem(menuItems, "消息操作", "原始消息", new Runnable() { public void run() { showRawMessageDialog(finalMsgRecord); } });
    addMenuItem(menuItems, "消息操作", "解析消息", new Runnable() { public void run() { showMsgDataPaeseDialog(activity, data); } });
    addMenuItem(menuItems, "消息操作", "提取音频", new Runnable() { public void run() { showExtractAudioDialog(activity, data); } });
    if (chatType == 2 && ("OWNER".equals(role) || "ADMIN".equals(role))) {
    addMenuItem(menuItems, "消息操作", "设为精华", new Runnable() { public void run() { setMsgEssence(data, true); } });
    addMenuItem(menuItems, "消息操作", "取消精华", new Runnable() { public void run() { setMsgEssence(data, false); } });
    }

    if (chatType == 2) {
        addMenuItem(menuItems, "群管理", "成员信息", new Runnable() { public void run() { showMemberInfoDialog(activity, finalPeerUin, finalUserUin, finalChatType); } });
        addMenuItem(menuItems, "群管理", "群详细信息", new Runnable() { public void run() { showGroupInfoDialog(activity, finalPeerUin, isDark); } });
        addMenuItem(menuItems, "群管理", "全体禁言", new Runnable() { public void run() { showMuteAllDialog(activity, finalPeerUin); } });
        addMenuItem(menuItems, "群管理", "禁言此人", new Runnable() { public void run() { showShutUpDialog(activity, finalPeerUin, finalUserUin, finalNickName); } });
        addMenuItem(menuItems, "群管理", "踢出此人", new Runnable() { public void run() { showKickConfirmDialog(activity, finalPeerUin, finalUserUin, finalNickName, isDark); } });
        addMenuItem(menuItems, "群管理", "设置头衔", new Runnable() { public void run() { showTitleDialog(activity, finalPeerUin, finalUserUin, finalNickName); } });
        addMenuItem(menuItems, "群管理", "设置管理员", new Runnable() { public void run() { showSetAdminDialog(activity, finalPeerUin, finalUserUin, finalNickName); } });
        addMenuItem(menuItems, "群管理", "修改群名片", new Runnable() { public void run() { showChangeCardDialog(activity, finalPeerUin, finalUserUin, finalNickName); } });
        addMenuItem(menuItems, "群管理", "禁言列表", new Runnable() { public void run() { showProhibitListDialog(activity, finalPeerUin, isDark); } });
        addMenuItem(menuItems, "群管理", "群成员列表", new Runnable() { public void run() { showGroupMemberListDialog(activity, finalPeerUin, isDark); } });
    } else {
        addMenuItem(menuItems, "好友信息", "查看资料", new Runnable() { public void run() { showMemberInfoDialog(activity, finalPeerUin, finalUserUin, finalChatType); } });
    }

    addMenuItem(menuItems, "互动功能", "为Ta点赞", new Runnable() { public void run() { showZanDialog(activity, finalUserUin); } });
    if (chatType == 2) {
        addMenuItem(menuItems, "互动功能", "回应表情", new Runnable() { public void run() { showFaceReplyConfigDialog(data); } });
        addMenuItem(menuItems, "互动功能", "AI声聊", new Runnable() { public void run() { showVoiceSendDialog(data); } });
        addMenuItem(menuItems, "互动功能", "艾特全体", new Runnable() { public void run() { sendMsg(finalPeerUin, "[atUin=0]", finalChatType); } });
        addMenuItem(menuItems, "互动功能", "艾特列表", new Runnable() { public void run() { showAtListDialog(finalAtList); } });
    }
    addMenuItem(menuItems, "互动功能", "拍一拍", new Runnable() { public void run() { showPaiDialog(activity, finalUserUin, finalPeerUin, finalChatType); } });
    addMenuItem(menuItems, "工具", "加解密工具", new Runnable() { public void run() { showEncryptDecryptDialog(activity, data); } });
    addMenuItem(menuItems, "工具", "执行代码", new Runnable() { public void run() { showCodeConsoleDialog(activity, data); } });
    addMenuItem(menuItems, "工具", "发送pb", new Runnable() { public void run() { showPBSenderDialog(); } });
    addMenuItem(menuItems, "工具", "获取Cookie", new Runnable() { public void run() { showGetCookieDialog(activity, isDark); } });
    addMenuItem(menuItems, "工具", "群列表", new Runnable() { public void run() { showGroupListDialog(activity, isDark); } });
    addMenuItem(menuItems, "工具", "好友列表", new Runnable() { public void run() { showFriendListDialog(activity, isDark); } });
    addMenuItem(menuItems, "工具", "发送日志", new Runnable() { public void run() {
        Toast("正在打包日志...");
        new Thread(new Runnable() { public void run() {
            try {
                File logDir = new File(pluginPath + "/Log");
                String zipPath = null;
                if (logDir.exists() && logDir.isDirectory()) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
                    zipPath = pluginPath + "/" + sdf.format(new java.util.Date()) + "-Log.zip";
                    java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipPath));
                    String envInfo = "";
                    try { envInfo = String.valueOf(me.yxp.qfun.utils.log.LogUtils.INSTANCE.getEnvironmentInfo()); }
                    catch (Throwable e2) { envInfo = "获取环境信息异常: " + e2; }
                    zos.putNextEntry(new java.util.zip.ZipEntry("info.txt"));
                    zos.write(envInfo.getBytes("UTF-8"));
                    zos.closeEntry();
                    File[] logs = logDir.listFiles();
                    if (logs != null) {
                        for (int i = 0; i < logs.length; i++) {
                            File f = logs[i];
                            if (f == null || !f.isFile()) continue;
                            zos.putNextEntry(new java.util.zip.ZipEntry("log/" + f.getName()));
                            java.io.FileInputStream fis = new java.io.FileInputStream(f);
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
                            fis.close();
                            zos.closeEntry();
                        }
                    }
                    zos.close();
                }
                if (zipPath == null) { Toast("日志打包失败"); return; }
                Toast("日志已打包，正在发送...");
                sendFile(finalPeerUin, zipPath, finalChatType);
                Toast("日志已发送");
                uiHandler.postDelayed(new Runnable() {
                    public void run() {
                        try { new File(zipPath).delete(); }
                        catch (Throwable e3) { traceLog("dialog_log", "[长按消息菜单] 删除日志zip异常: " + e3); }
                    }
                }, 180000);
            } catch (Throwable e) {
                traceLog("dialog_log", "[长按消息菜单] 发送日志异常: " + e);
                Toast("发送日志异常: " + e.getMessage());
            }
        } }).start();
    }});

    addMenuItem(menuItems, "其他", "克隆头像", new Runnable() { public void run() { handleCloneAvatar(finalUserUin); } });
    addMenuItem(menuItems, "其他", "上传头像", new Runnable() { public void run() { handleUploadAvatar(finalQuntext); } });
    if (chatType == 2) {
        addMenuItem(menuItems, "其他", "群打卡", new Runnable() { public void run() { boolean ok = CheckSign(finalPeerUin, myUin); qqToast(ok ? 2 : 1, ok ? "打卡成功" : "打卡失败"); } });
        addMenuItem(menuItems, "其他", "群字符", new Runnable() { public void run() { drawLuckyChar(finalPeerUin); } });
    }
    addMenuItem(menuItems, "其他", "设置", new Runnable() { public void run() { showSettingsMenu(activity, null, null, null); } });
    addMenuItem(menuItems, "其他", "双击消息菜单", new Runnable() { public void run() {                                     fetchRealMsgRecord(finalMsgid, finalChatType, finalPeerUin, new MsgLoadedCallback() {
                                        public void onLoaded(MsgData msgData) {
                                            showActionDialog(activity, msgData, null, null);
                                        }
                                    });
 } });
    // addMenuItem(menuItems, "实验功能", "偷流量红包", new Runnable() { public void run() { showTrafficRedPacketDialog(data); } });
    // addMenuItem(menuItems, "实验功能", "图片转QQ秀", new Runnable() { public void run() { showSuperFaceSendDialog(data); } });

    
    final boolean[] isEditMode = new boolean[]{false};
    String savedOrder = getString("setting", "menuSort", "");
    
    final List catOrder = new ArrayList();
    final Map catItems = new LinkedHashMap();
    
    for (int i = 0; i < menuItems.size(); i++) {
        Object[] item = (Object[]) menuItems.get(i);
        String cat = (String) item[0];
        if (!catOrder.contains(cat)) catOrder.add(cat);
        if (!catItems.containsKey(cat)) catItems.put(cat, new ArrayList());
        ((List)catItems.get(cat)).add(item);
    }
    
    applySavedSortOrder(catOrder, catItems, savedOrder);
    
    Runnable saveOrder = new Runnable() {
        public void run() {
            try {
                String orderStr = buildSortOrderString(catOrder, catItems);
                if (!orderStr.isEmpty()) {
                    putString("setting", "menuSort", orderStr);
                }
            } catch (Throwable t) { traceLog("dialog_log", "[onLoaded] 异常: " + t); }
        }
    };

    final Runnable[] renderMenuWrapper = new Runnable[1];
    
    renderMenuWrapper[0] = new Runnable() {
        public void run() {
            menuContainer.removeAllViews();
            
            View.OnDragListener dragListener = new View.OnDragListener() {
                public boolean onDrag(View v, DragEvent event) {
                    if (event.getAction() == DragEvent.ACTION_DROP) {
                        View source = (View) event.getLocalState();
                        if (source == v) return true;
                        Object[] srcTag = (Object[]) source.getTag();
                        Object[] tgtTag = (Object[]) v.getTag();
                        if (srcTag == null || tgtTag == null) return false;
                        
                        String srcType = (String) srcTag[0];
                        String tgtType = (String) tgtTag[0];
                        
                        if (srcType.equals("cat") && tgtType.equals("cat")) {
                            String srcCat = (String) srcTag[1];
                            String tgtCat = (String) tgtTag[1];
                            int sIdx = catOrder.indexOf(srcCat);
                            int tIdx = catOrder.indexOf(tgtCat);
                            if(sIdx >= 0 && tIdx >= 0) {
                                catOrder.remove(sIdx);
                                catOrder.add(tIdx, srcCat);
                                if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                            }
                        } else if (srcType.equals("item") && tgtType.equals("item")) {
                            String srcCat = (String) srcTag[1];
                            String srcItem = (String) srcTag[2];
                            String tgtCat = (String) tgtTag[1];
                            String tgtItem = (String) tgtTag[2];
                            
                            // 允许跨分类移动
                            if (srcCat.equals(tgtCat)) {
                                // 同分类内移动
                                List iList = (List) catItems.get(srcCat);
                                int sIdx = -1, tIdx = -1;
                                for(int i=0; i<iList.size(); i++) {
                                    Object[] itm = (Object[]) iList.get(i);
                                    if(itm[1].equals(srcItem)) sIdx = i;
                                    if(itm[1].equals(tgtItem)) tIdx = i;
                                }
                                if(sIdx >= 0 && tIdx >= 0) {
                                    Object obj = iList.remove(sIdx);
                                    iList.add(tIdx, obj);
                                    if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                                }
                            } else {
                                // 跨分类移动
                                List srcList = (List) catItems.get(srcCat);
                                List tgtList = (List) catItems.get(tgtCat);
                                if (srcList != null && tgtList != null) {
                                    int sIdx = -1;
                                    Object[] movedItem = null;
                                    for(int i=0; i<srcList.size(); i++) {
                                        Object[] itm = (Object[]) srcList.get(i);
                                        if(itm[1].equals(srcItem)) {
                                            sIdx = i;
                                            movedItem = itm;
                                            break;
                                        }
                                    }
                                    if (movedItem != null) {
                                        // 更新item的分类
                                        movedItem[0] = tgtCat;
                                        srcList.remove(sIdx);
                                        
                                        int tIdx = -1;
                                        for(int i=0; i<tgtList.size(); i++) {
                                            Object[] itm = (Object[]) tgtList.get(i);
                                            if(itm[1].equals(tgtItem)) {
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
            
            // 渲染普通分类
            for (int i=0; i<catOrder.size(); i++) {
                String cat = (String) catOrder.get(i);
                int[] colorInts = getCategoryColorInts(cat, isDark);
                
                LinearLayout catLayout = new LinearLayout(activity);
                catLayout.setOrientation(LinearLayout.VERTICAL);
                catLayout.setTag(new Object[]{"cat", cat});
                
                // 标题行 - 只在文字部分设置拖动
                LinearLayout titleRow = new LinearLayout(activity);
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(Gravity.CENTER_VERTICAL);
                
                TextView titleView = createCategoryTitle(activity, cat, colorInts[0]);
                titleView.setTag(new Object[]{"cat", cat});
                
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
                gridLayout.setPadding(0, 0, 0, dp(activity, 8));
                
                List items = (List) catItems.get(cat);
                for (int j=0; j<items.size(); j++) {
                    Object[] itemObj = (Object[]) items.get(j);
                    String title = (String) itemObj[1];
                    Runnable callback = (Runnable) itemObj[2];
                    
                    FrameLayout menuItem = createMenuItem(activity, title, isDark, colorInts[0], colorCardBg, bottomSheet, callback, false);
                    menuItem.setTag(new Object[]{"item", cat, title});
                    
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
                                finalItem.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
                            }
                        }, delay);
                    }
                    gridLayout.addView(menuItem);
                }
                catLayout.addView(gridLayout);
                menuContainer.addView(catLayout);
            }
            
            // 渲染"设置"分类
            int[] settingColorInts = getCategoryColorInts("设置", isDark);
            LinearLayout settingLayout = new LinearLayout(activity);
            settingLayout.setOrientation(LinearLayout.VERTICAL);
            settingLayout.setTag(new Object[]{"cat", "设置"});
            
            // 设置标题行
            LinearLayout settingTitleRow = new LinearLayout(activity);
            settingTitleRow.setOrientation(LinearLayout.HORIZONTAL);
            settingTitleRow.setGravity(Gravity.CENTER_VERTICAL);
            
            TextView settingTitle = createCategoryTitle(activity, "设置", settingColorInts[0]);
            settingTitle.setTag(new Object[]{"cat", "设置"});
            
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
            
            // 使用GridLayout保持与其他分类一致
            GridLayout settingGrid = new GridLayout(activity);
            settingGrid.setColumnCount(2);
            settingGrid.setPadding(0, 0, 0, dp(activity, 8));
            
            FrameLayout sortMenuItem = createMenuItem(activity, "自定义排序", isDark, settingColorInts[0], colorCardBg, bottomSheet, new Runnable() {
                public void run() {
                    isEditMode[0] = true;
                    if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                }
            }, true);
            sortMenuItem.setTag(new Object[]{"item", "设置", "自定义排序"});
            
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
            
            // 取消和保存按钮 - 单独一行
            if (isEditMode[0]) {
                LinearLayout actionBtnRow = new LinearLayout(activity);
                actionBtnRow.setOrientation(LinearLayout.HORIZONTAL);
                actionBtnRow.setGravity(Gravity.CENTER);
                actionBtnRow.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
                
                TextView cancelBtn = createButton(activity, "取消", Color.WHITE, isDark ? pc("#FF5252") : pc("#F44336"), 14f, 20, 16, 10, false, 0, 0, null);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isEditMode[0] = false;
                        String saved = getString("setting", "menuSort", "");
                        applySavedSortOrder(catOrder, catItems, saved);
                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                    }
                });
                
                TextView saveBtn = createButton(activity, "保存", Color.WHITE, isDark ? pc("#81C784") : pc("#4CAF50"), 14f, 20, 16, 10, false, 0, 0, null);
                saveBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isEditMode[0] = false;
                        saveOrder.run();
                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                        Toast("排序已保存");
                    }
                });
                
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
        bgDrawable.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        rootLayout.setBackground(bgDrawable);
        rootLayout.setTranslationY(dp(activity, 120));
        rootLayout.setAlpha(0f);
        rootLayout.animate().translationY(0f).alpha(1f).setDuration(250).setInterpolator(new DecelerateInterpolator()).start();
        if (!"color".equals(getUiBgType())) {
            applyUiTheme(activity, bottomSheet, 5);
        }
        applyWindowBlurBehind(window);
    }
}

void showProhibitListDialog(Activity activity, String groupUin, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
    int borderColor = adjustAlpha(textColor, 0.3f);

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                final TextView titleView = new TextView(activity);
                titleView.setText("禁言列表");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                LinearLayout listContainer = new LinearLayout(activity);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                final LinearLayout loading = createModernLoading(activity, isDark);
                listContainer.addView(loading);

                ScrollView scrollView = new ScrollView(activity);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scrollView.setLayoutParams(scrollParams);
                scrollView.addView(listContainer);
                root.addView(scrollView);

                final AlertDialog[] ref = new AlertDialog[1];
                TextView closeBtn = createButton(activity, "关闭", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT);
                btnBox.addView(closeBtn);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object prohibitList = getProhibitList(groupUin);
                            final List allItems = new ArrayList();
                            
                            if (prohibitList != null && ((List) prohibitList).size() > 0) {
                                for (int i = 0; i < ((List) prohibitList).size(); i++) {
                                    Object f = ((List) prohibitList).get(i);
                                    String uin = "";
                                    String name = "";
                                    String status = "未知";
                                    try {
                                        uin = String.valueOf(f.user);
                                        name = String.valueOf(f.userName);
                                        status = getGagStatus(String.valueOf(f.endTime));
                                    } catch (Throwable t) { traceLog("dialog_log", "[showProhibitListDialog] 异常: " + t); }
                                    allItems.add(new Object[]{uin, name, status});
                                }
                            }
                            
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (allItems.isEmpty()) {
                                        listContainer.removeView(loading);
                                        TextView emptyView = new TextView(activity);
                                        emptyView.setText("当前群无禁言成员");
                                        emptyView.setTextSize(14);
                                        emptyView.setTextColor(subTextColor);
                                        emptyView.setGravity(Gravity.CENTER);
                                        emptyView.setPadding(dp(activity, 20), dp(activity, 40), dp(activity, 20), dp(activity, 40));
                                        listContainer.addView(emptyView);
                                    } else {
                                        titleView.setText("禁言列表 (" + allItems.size() + "人)");
                                        listContainer.removeView(loading);
                                        
                                        // 流式加载：先加载前100个并立即显示
                                        final int batchSize = 100;
                                        int firstBatch = Math.min(batchSize, allItems.size());
                                        
                                        for (int i = 0; i < firstBatch; i++) {
                                            Object[] itemData = (Object[]) allItems.get(i);
                                            String uin = (String) itemData[0];
                                            String name = (String) itemData[1];
                                            String status = (String) itemData[2];

                                            LinearLayout item = new LinearLayout(activity);
                                            item.setOrientation(LinearLayout.VERTICAL);
                                            item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                            item.setClickable(true);
                                            
                                            final GradientDrawable itemBg = new GradientDrawable();
                                            itemBg.setCornerRadius(dp(activity, 10));
                                            itemBg.setColor(Color.TRANSPARENT);
                                            itemBg.setStroke(dp(activity, 1), borderColor);
                                            item.setBackground(itemBg);

                                            TextView nameView = new TextView(activity);
                                            nameView.setText(name + "(" + uin + ")");
                                            nameView.setTextSize(14);
                                            nameView.setTextColor(textColor);
                                            item.addView(nameView);

                                            TextView statusView = new TextView(activity);
                                            statusView.setText(status);
                                            statusView.setTextSize(12);
                                            statusView.setTextColor(subTextColor);
                                            item.addView(statusView);

                                            item.setOnTouchListener(new View.OnTouchListener() {
                                                public boolean onTouch(View v, MotionEvent event) {
                                                    switch (event.getAction()) {
                                                        case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                        case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                        case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                    }
                                                    return false;
                                                }
                                            });
                                            final String finalUin = uin;
                                            final String finalName = name;
                                            item.setOnClickListener(new View.OnClickListener() {
                                                public void onClick(View v) {
                                                    showShutUpDialog(activity, groupUin, finalUin, finalName);
                                                }
                                            });

                                            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                            itemParams.setMargins(0, 0, 0, dp(activity, 8));
                                            item.setLayoutParams(itemParams);
                                            listContainer.addView(item);
                                        }
                                        
                                        // 如果还有剩余，异步后台加载
                                        if (allItems.size() > batchSize) {
                                            final int[] currentIdx = {batchSize};
                                            ThreadPool.execute(new Runnable() {
                                                public void run() {
                                                    while (currentIdx[0] < allItems.size()) {
                                                        final int start = currentIdx[0];
                                                        final int end = Math.min(currentIdx[0] + batchSize, allItems.size());
                                                        
                                                        activity.runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                for (int i = start; i < end; i++) {
                                                                    Object[] itemData = (Object[]) allItems.get(i);
                                                                    String uin = (String) itemData[0];
                                                                    String name = (String) itemData[1];
                                                                    String status = (String) itemData[2];

                                                                    LinearLayout item = new LinearLayout(activity);
                                                                    item.setOrientation(LinearLayout.VERTICAL);
                                                                    item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                                                    item.setClickable(true);
                                                                    
                                                                    final GradientDrawable itemBg = new GradientDrawable();
                                                                    itemBg.setCornerRadius(dp(activity, 10));
                                                                    itemBg.setColor(Color.TRANSPARENT);
                                                                    itemBg.setStroke(dp(activity, 1), borderColor);
                                                                    item.setBackground(itemBg);

                                                                    TextView nameView = new TextView(activity);
                                                                    nameView.setText(name + "(" + uin + ")");
                                                                    nameView.setTextSize(14);
                                                                    nameView.setTextColor(textColor);
                                                                    item.addView(nameView);

                                                                    TextView statusView = new TextView(activity);
                                                                    statusView.setText(status);
                                                                    statusView.setTextSize(12);
                                                                    statusView.setTextColor(subTextColor);
                                                                    item.addView(statusView);

                                                                    item.setOnTouchListener(new View.OnTouchListener() {
                                                                        public boolean onTouch(View v, MotionEvent event) {
                                                                            switch (event.getAction()) {
                                                                                case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                                                case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                                                case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                                            }
                                                                            return false;
                                                                        }
                                                                    });
                                                                    final String finalUin = uin;
                                                                    final String finalName = name;
                                                                    item.setOnClickListener(new View.OnClickListener() {
                                                                        public void onClick(View v) {
                                                                            showShutUpDialog(activity, groupUin, finalUin, finalName);
                                                                        }
                                                                    });

                                                                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                                    itemParams.setMargins(0, 0, 0, dp(activity, 8));
                                                                    item.setLayoutParams(itemParams);
                                                                    listContainer.addView(item);
                                                                }
                                                            }
                                                        });
                                                        
                                                        currentIdx[0] = end;
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        } catch (Throwable e) {
                            uiHandler.post(new Runnable() { public void run() { Toast("获取禁言列表失败: " + e.getMessage()); } });
                        }
                    }
                });
            } catch (Throwable e) { Toast("显示失败"); }
        }
    });
}

void showGroupMemberListDialog(Activity activity, String groupUin, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
    int borderColor = adjustAlpha(textColor, 0.3f);

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                final TextView titleView = new TextView(activity);
                titleView.setText("群成员列表");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                LinearLayout listContainer = new LinearLayout(activity);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                final LinearLayout loading = createModernLoading(activity, isDark);
                listContainer.addView(loading);

                ScrollView scroll = new ScrollView(activity);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 350));
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scroll.setLayoutParams(scrollParams);
                scroll.addView(listContainer);
                root.addView(scroll);

                final AlertDialog[] ref = new AlertDialog[1];
                TextView closeBtn = createButton(activity, "关闭", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT);
                btnBox.addView(closeBtn);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object members = getGroupMemberList(groupUin);
                            final List allItems = new ArrayList();
                            
                            if (members != null && ((List) members).size() > 0) {
                                for (int i = 0; i < ((List) members).size(); i++) {
                                    Object m = ((List) members).get(i);
                                    String uin = "";
                                    String name = "";
                                    String role = "未知";
                                    try {
                                        uin = String.valueOf(m.uin);
                                        name = String.valueOf(m.uinName);
                                        role = convertRole(String.valueOf(m.role));
                                    } catch (Throwable t) { traceLog("dialog_log", "[showGroupMemberListDialog] 异常: " + t); }
                                    allItems.add(new Object[]{uin, name, role});
                                }
                            }
                            
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (allItems.isEmpty()) {
                                        listContainer.removeView(loading);
                                        TextView emptyView = new TextView(activity);
                                        emptyView.setText("当前群无成员");
                                        emptyView.setTextSize(14);
                                        emptyView.setTextColor(subTextColor);
                                        emptyView.setGravity(Gravity.CENTER);
                                        emptyView.setPadding(dp(activity, 20), dp(activity, 40), dp(activity, 20), dp(activity, 40));
                                        listContainer.addView(emptyView);
                                    } else {
                                        titleView.setText("群成员列表 (" + allItems.size() + "人)");
                                        listContainer.removeView(loading);
                                        
                                        final int batchSize = 100;
                                        int firstBatch = Math.min(batchSize, allItems.size());
                                        
                                        for (int i = 0; i < firstBatch; i++) {
                                            Object[] itemData = (Object[]) allItems.get(i);
                                            String uin = (String) itemData[0];
                                            String name = (String) itemData[1];
                                            String role = (String) itemData[2];

                                            LinearLayout item = new LinearLayout(activity);
                                            item.setOrientation(LinearLayout.HORIZONTAL);
                                            item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                            item.setClickable(true);
                                            item.setGravity(Gravity.CENTER_VERTICAL);
                                            
                                            final GradientDrawable itemBg = new GradientDrawable();
                                            itemBg.setCornerRadius(dp(activity, 10));
                                            itemBg.setColor(Color.TRANSPARENT);
                                            itemBg.setStroke(dp(activity, 1), borderColor);
                                            item.setBackground(itemBg);

                                            TextView nameView = new TextView(activity);
                                            nameView.setText(name + "(" + uin + ")");
                                            nameView.setTextSize(14);
                                            nameView.setTextColor(textColor);
                                            nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                                            item.addView(nameView);

                                            TextView roleView = new TextView(activity);
                                            roleView.setText(role);
                                            roleView.setTextSize(12);
                                            roleView.setTextColor(subTextColor);
                                            item.addView(roleView);

                                            item.setOnTouchListener(new View.OnTouchListener() {
                                                public boolean onTouch(View v, MotionEvent event) {
                                                    switch (event.getAction()) {
                                                        case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                        case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                        case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                    }
                                                    return false;
                                                }
                                            });
                                            final String finalUin = uin;
                                            item.setOnClickListener(new View.OnClickListener() {
                                                public void onClick(View v) {
                                                    showMemberInfoDialog(activity, groupUin, finalUin, 2);
                                                }
                                            });

                                            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                            itemParams.setMargins(0, 0, 0, dp(activity, 6));
                                            item.setLayoutParams(itemParams);
                                            listContainer.addView(item);
                                        }
                                        
                                        if (allItems.size() > batchSize) {
                                            final int[] currentIdx = {batchSize};
                                            ThreadPool.execute(new Runnable() {
                                                public void run() {
                                                    while (currentIdx[0] < allItems.size()) {
                                                        final int start = currentIdx[0];
                                                        final int end = Math.min(currentIdx[0] + batchSize, allItems.size());
                                                        
                                                        activity.runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                for (int i = start; i < end; i++) {
                                                                    Object[] itemData = (Object[]) allItems.get(i);
                                                                    String uin = (String) itemData[0];
                                                                    String name = (String) itemData[1];
                                                                    String role = (String) itemData[2];

                                                                    LinearLayout item = new LinearLayout(activity);
                                                                    item.setOrientation(LinearLayout.HORIZONTAL);
                                                                    item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                                                    item.setClickable(true);
                                                                    item.setGravity(Gravity.CENTER_VERTICAL);
                                                                    
                                                                    final GradientDrawable itemBg = new GradientDrawable();
                                                                    itemBg.setCornerRadius(dp(activity, 10));
                                                                    itemBg.setColor(Color.TRANSPARENT);
                                                                    itemBg.setStroke(dp(activity, 1), borderColor);
                                                                    item.setBackground(itemBg);

                                                                    TextView nameView = new TextView(activity);
                                                                    nameView.setText(name + "(" + uin + ")");
                                                                    nameView.setTextSize(14);
                                                                    nameView.setTextColor(textColor);
                                                                    nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                                                                    item.addView(nameView);

                                                                    TextView roleView = new TextView(activity);
                                                                    roleView.setText(role);
                                                                    roleView.setTextSize(12);
                                                                    roleView.setTextColor(subTextColor);
                                                                    item.addView(roleView);

                                                                    item.setOnTouchListener(new View.OnTouchListener() {
                                                                        public boolean onTouch(View v, MotionEvent event) {
                                                                            switch (event.getAction()) {
                                                                                case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                                                case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                                                case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                                            }
                                                                            return false;
                                                                        }
                                                                    });
                                                                    final String finalUin = uin;
                                                                    item.setOnClickListener(new View.OnClickListener() {
                                                                        public void onClick(View v) {
                                                                            showMemberInfoDialog(activity, groupUin, finalUin, 2);
                                                                        }
                                                                    });

                                                                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                                    itemParams.setMargins(0, 0, 0, dp(activity, 6));
                                                                    item.setLayoutParams(itemParams);
                                                                    listContainer.addView(item);
                                                                }
                                                            }
                                                        });
                                                        
                                                        currentIdx[0] = end;
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        } catch (Throwable e) {
                            uiHandler.post(new Runnable() { public void run() { Toast("获取群成员列表失败: " + e.getMessage()); } });
                        }
                    }
                });
            } catch (Throwable e) { Toast("显示失败"); }
        }
    });
}

void showGroupListDialog(Activity activity, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
    int borderColor = adjustAlpha(textColor, 0.3f);

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                final TextView titleView = new TextView(activity);
                titleView.setText("我的群列表");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                LinearLayout listContainer = new LinearLayout(activity);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                final LinearLayout loading = createModernLoading(activity, isDark);
                listContainer.addView(loading);

                ScrollView scroll = new ScrollView(activity);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 350));
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scroll.setLayoutParams(scrollParams);
                scroll.addView(listContainer);
                root.addView(scroll);

                final AlertDialog[] ref = new AlertDialog[1];
                TextView closeBtn = createButton(activity, "关闭", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT);
                btnBox.addView(closeBtn);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object groups = getGroupList();
                            final List allItems = new ArrayList();
                            
                            if (groups != null && ((List) groups).size() > 0) {
                                for (int i = 0; i < ((List) groups).size(); i++) {
                                    Object g = ((List) groups).get(i);
                                    String groupUin = "";
                                    String groupName = "";
                                    try {
                                        groupUin = String.valueOf(g.group);
                                        groupName = String.valueOf(g.groupName);
                                    } catch (Throwable t) { traceLog("dialog_log", "[showGroupListDialog] 异常: " + t); }
                                    allItems.add(new Object[]{groupUin, groupName});
                                }
                            }
                            
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (allItems.isEmpty()) {
                                        listContainer.removeView(loading);
                                        TextView emptyView = new TextView(activity);
                                        emptyView.setText("暂无群聊");
                                        emptyView.setTextSize(14);
                                        emptyView.setTextColor(subTextColor);
                                        emptyView.setGravity(Gravity.CENTER);
                                        emptyView.setPadding(dp(activity, 20), dp(activity, 40), dp(activity, 20), dp(activity, 40));
                                        listContainer.addView(emptyView);
                                    } else {
                                        titleView.setText("我的群列表 (" + allItems.size() + "个)");
                                        listContainer.removeView(loading);
                                        
                                        final int batchSize = 100;
                                        int firstBatch = Math.min(batchSize, allItems.size());
                                        
                                        for (int i = 0; i < firstBatch; i++) {
                                            Object[] itemData = (Object[]) allItems.get(i);
                                            String groupUin = (String) itemData[0];
                                            String groupName = (String) itemData[1];

                                            LinearLayout item = new LinearLayout(activity);
                                            item.setOrientation(LinearLayout.VERTICAL);
                                            item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                            item.setClickable(true);
                                            
                                            final GradientDrawable itemBg = new GradientDrawable();
                                            itemBg.setCornerRadius(dp(activity, 10));
                                            itemBg.setColor(Color.TRANSPARENT);
                                            itemBg.setStroke(dp(activity, 1), borderColor);
                                            item.setBackground(itemBg);

                                            TextView nameView = new TextView(activity);
                                            nameView.setText(groupName);
                                            nameView.setTextSize(14);
                                            nameView.setTextColor(textColor);
                                            item.addView(nameView);

                                            TextView uinView = new TextView(activity);
                                            uinView.setText("群号: " + groupUin);
                                            uinView.setTextSize(12);
                                            uinView.setTextColor(subTextColor);
                                            item.addView(uinView);

                                            item.setOnTouchListener(new View.OnTouchListener() {
                                                public boolean onTouch(View v, MotionEvent event) {
                                                    switch (event.getAction()) {
                                                        case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                        case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                        case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                    }
                                                    return false;
                                                }
                                            });
                                            final String finalGroupUin = groupUin;
                                            item.setOnClickListener(new View.OnClickListener() {
                                                public void onClick(View v) {
                                                    showGroupInfoDialog(activity, finalGroupUin, isDark);
                                                }
                                            });

                                            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                            itemParams.setMargins(0, 0, 0, dp(activity, 6));
                                            item.setLayoutParams(itemParams);
                                            listContainer.addView(item);
                                        }
                                        
                                        if (allItems.size() > batchSize) {
                                            final int[] currentIdx = {batchSize};
                                            ThreadPool.execute(new Runnable() {
                                                public void run() {
                                                    while (currentIdx[0] < allItems.size()) {
                                                        final int start = currentIdx[0];
                                                        final int end = Math.min(currentIdx[0] + batchSize, allItems.size());
                                                        
                                                        activity.runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                for (int i = start; i < end; i++) {
                                                                    Object[] itemData = (Object[]) allItems.get(i);
                                                                    String groupUin = (String) itemData[0];
                                                                    String groupName = (String) itemData[1];

                                                                    LinearLayout item = new LinearLayout(activity);
                                                                    item.setOrientation(LinearLayout.VERTICAL);
                                                                    item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                                                    item.setClickable(true);
                                                                    
                                                                    final GradientDrawable itemBg = new GradientDrawable();
                                                                    itemBg.setCornerRadius(dp(activity, 10));
                                                                    itemBg.setColor(Color.TRANSPARENT);
                                                                    itemBg.setStroke(dp(activity, 1), borderColor);
                                                                    item.setBackground(itemBg);

                                                                    TextView nameView = new TextView(activity);
                                                                    nameView.setText(groupName);
                                                                    nameView.setTextSize(14);
                                                                    nameView.setTextColor(textColor);
                                                                    item.addView(nameView);

                                                                    TextView uinView = new TextView(activity);
                                                                    uinView.setText("群号: " + groupUin);
                                                                    uinView.setTextSize(12);
                                                                    uinView.setTextColor(subTextColor);
                                                                    item.addView(uinView);

                                                                    item.setOnTouchListener(new View.OnTouchListener() {
                                                                        public boolean onTouch(View v, MotionEvent event) {
                                                                            switch (event.getAction()) {
                                                                                case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                                                case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                                                case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                                            }
                                                                            return false;
                                                                        }
                                                                    });
                                                                    final String finalGroupUin = groupUin;
                                                                    item.setOnClickListener(new View.OnClickListener() {
                                                                        public void onClick(View v) {
                                                                            showGroupInfoDialog(activity, finalGroupUin, isDark);
                                                                        }
                                                                    });

                                                                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                                    itemParams.setMargins(0, 0, 0, dp(activity, 6));
                                                                    item.setLayoutParams(itemParams);
                                                                    listContainer.addView(item);
                                                                }
                                                            }
                                                        });
                                                        
                                                        currentIdx[0] = end;
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        } catch (Throwable e) {
                            uiHandler.post(new Runnable() { public void run() { Toast("获取群列表失败: " + e.getMessage()); } });
                        }
                    }
                });
            } catch (Throwable e) { Toast("显示失败"); }
        }
    });
}

void showFriendListDialog(Activity activity, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
    int borderColor = adjustAlpha(textColor, 0.3f);

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                final TextView titleView = new TextView(activity);
                titleView.setText("我的好友列表");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 12));
                root.addView(titleView);

                LinearLayout listContainer = new LinearLayout(activity);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                final LinearLayout loading = createModernLoading(activity, isDark);
                listContainer.addView(loading);

                ScrollView scroll = new ScrollView(activity);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 350));
                scrollParams.setMargins(0, 0, 0, dp(activity, 12));
                scroll.setLayoutParams(scrollParams);
                scroll.addView(listContainer);
                root.addView(scroll);

                final AlertDialog[] ref = new AlertDialog[1];
                TextView closeBtn = createButton(activity, "关闭", isDark ? pc("#FF8AB4F8") : pc("#FF2196F3"), Color.TRANSPARENT, 15f, 0, 16, 12, false, 0, 0, null);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setGravity(Gravity.RIGHT);
                btnBox.addView(closeBtn);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                ref[0] = builder.create();
                ref[0].show();
                applyUiTheme(activity, ref[0], 0);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object friends = getAllFriend();
                            final List allItems = new ArrayList();
                            
                            if (friends != null && ((List) friends).size() > 0) {
                                for (int i = 0; i < ((List) friends).size(); i++) {
                                    Object f = ((List) friends).get(i);
                                    String uin = "";
                                    String name = "";
                                    String remark = "";
                                    try {
                                        uin = f.uin;
                                        name = f.name;
                                        remark = f.remark;
                                    } catch (Throwable t) { traceLog("dialog_log", "[showFriendListDialog] 异常: " + t); }
                                    allItems.add(new Object[]{uin, name, remark});
                                }
                            }
                            
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (allItems.isEmpty()) {
                                        listContainer.removeView(loading);
                                        TextView emptyView = new TextView(activity);
                                        emptyView.setText("暂无好友");
                                        emptyView.setTextSize(14);
                                        emptyView.setTextColor(subTextColor);
                                        emptyView.setGravity(Gravity.CENTER);
                                        emptyView.setPadding(dp(activity, 20), dp(activity, 40), dp(activity, 20), dp(activity, 40));
                                        listContainer.addView(emptyView);
                                    } else {
                                        titleView.setText("我的好友列表 (" + allItems.size() + "人)");
                                        listContainer.removeView(loading);
                                        
                                        final int batchSize = 100;
                                        int firstBatch = Math.min(batchSize, allItems.size());
                                        
                                        for (int i = 0; i < firstBatch; i++) {
                                            Object[] itemData = (Object[]) allItems.get(i);
                                            String uin = (String) itemData[0];
                                            String name = (String) itemData[1];
                                            String remark = (String) itemData[2];
                                            String displayName = (remark != null && !remark.isEmpty() && !remark.equals("null")) 
                                                ? remark + "(" + name + ")" : name;

                                            LinearLayout item = new LinearLayout(activity);
                                            item.setOrientation(LinearLayout.HORIZONTAL);
                                            item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                            item.setClickable(true);
                                            item.setGravity(Gravity.CENTER_VERTICAL);
                                            
                                            final GradientDrawable itemBg = new GradientDrawable();
                                            itemBg.setCornerRadius(dp(activity, 10));
                                            itemBg.setColor(Color.TRANSPARENT);
                                            itemBg.setStroke(dp(activity, 1), borderColor);
                                            item.setBackground(itemBg);

                                            TextView nameView = new TextView(activity);
                                            nameView.setText(displayName);
                                            nameView.setTextSize(14);
                                            nameView.setTextColor(textColor);
                                            nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                                            item.addView(nameView);

                                            TextView uinView = new TextView(activity);
                                            uinView.setText(uin);
                                            uinView.setTextSize(12);
                                            uinView.setTextColor(subTextColor);
                                            item.addView(uinView);

                                            item.setOnTouchListener(new View.OnTouchListener() {
                                                public boolean onTouch(View v, MotionEvent event) {
                                                    switch (event.getAction()) {
                                                        case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                        case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                        case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                    }
                                                    return false;
                                                }
                                            });
                                            final String finalUin = uin;
                                            item.setOnClickListener(new View.OnClickListener() {
                                                public void onClick(View v) {
                                                    showMemberInfoDialog(activity, finalUin, finalUin, 1);
                                                }
                                            });

                                            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                            itemParams.setMargins(0, 0, 0, dp(activity, 6));
                                            item.setLayoutParams(itemParams);
                                            listContainer.addView(item);
                                        }
                                        
                                        if (allItems.size() > batchSize) {
                                            final int[] currentIdx = {batchSize};
                                            ThreadPool.execute(new Runnable() {
                                                public void run() {
                                                    while (currentIdx[0] < allItems.size()) {
                                                        final int start = currentIdx[0];
                                                        final int end = Math.min(currentIdx[0] + batchSize, allItems.size());
                                                        
                                                        activity.runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                for (int i = start; i < end; i++) {
                                                                    Object[] itemData = (Object[]) allItems.get(i);
                                                                    String uin = (String) itemData[0];
                                                                    String name = (String) itemData[1];
                                                                    String remark = (String) itemData[2];
                                                                    String displayName = (remark != null && !remark.isEmpty() && !remark.equals("null")) 
                                                                        ? remark + "(" + name + ")" : name;

                                                                    LinearLayout item = new LinearLayout(activity);
                                                                    item.setOrientation(LinearLayout.HORIZONTAL);
                                                                    item.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
                                                                    item.setClickable(true);
                                                                    item.setGravity(Gravity.CENTER_VERTICAL);
                                                                    
                                                                    final GradientDrawable itemBg = new GradientDrawable();
                                                                    itemBg.setCornerRadius(dp(activity, 10));
                                                                    itemBg.setColor(Color.TRANSPARENT);
                                                                    itemBg.setStroke(dp(activity, 1), borderColor);
                                                                    item.setBackground(itemBg);

                                                                    TextView nameView = new TextView(activity);
                                                                    nameView.setText(displayName);
                                                                    nameView.setTextSize(14);
                                                                    nameView.setTextColor(textColor);
                                                                    nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                                                                    item.addView(nameView);

                                                                    TextView uinView = new TextView(activity);
                                                                    uinView.setText(uin);
                                                                    uinView.setTextSize(12);
                                                                    uinView.setTextColor(subTextColor);
                                                                    item.addView(uinView);

                                                                    item.setOnTouchListener(new View.OnTouchListener() {
                                                                        public boolean onTouch(View v, MotionEvent event) {
                                                                            switch (event.getAction()) {
                                                                                case MotionEvent.ACTION_DOWN: itemBg.setColor(borderColor); item.setBackground(itemBg); return true;
                                                                                case MotionEvent.ACTION_UP: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); v.performClick(); return true;
                                                                                case MotionEvent.ACTION_CANCEL: itemBg.setColor(Color.TRANSPARENT); item.setBackground(itemBg); return true;
                                                                            }
                                                                            return false;
                                                                        }
                                                                    });
                                                                    final String finalUin = uin;
                                                                    item.setOnClickListener(new View.OnClickListener() {
                                                                        public void onClick(View v) {
                                                                            showMemberInfoDialog(activity, finalUin, finalUin, 1);
                                                                        }
                                                                    });

                                                                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                                                    itemParams.setMargins(0, 0, 0, dp(activity, 6));
                                                                    item.setLayoutParams(itemParams);
                                                                    listContainer.addView(item);
                                                                }
                                                            }
                                                        });
                                                        
                                                        currentIdx[0] = end;
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            });
                        } catch (Throwable e) {
                            uiHandler.post(new Runnable() { public void run() { Toast("获取好友列表失败: " + e.getMessage()); } });
                        }
                    }
                });
            } catch (Throwable e) { Toast("显示失败"); }
        }
    });
}

//非常花里胡哨的tips弹窗
public void ts(Activity activity, String title, String content) {
	if (activity == null || activity.isFinishing()) {
		traceLog("dialog_log", "[ts] Activity无效，无法显示弹窗");
		return;
	}
	boolean isDark = isThemeDark(activity);

	final String finalContent = content == null ? "" : content;
	// 多颜色高亮数组（含#的行循环使用）
	final int[] HIGHLIGHT_COLORS = {
		pc("#FF6B6B"), // 红色
		// pc("#4ECDC4"),   // 青色 太淡了不要了
		pc("#45B7D1"), // 蓝色
		// pc("#96CEB4"),   // 绿色 不好看也不要了
		pc("#DDA0DD") // 紫色
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
			textView.setTextColor(isDark ? pc("#FFEFEFEF") : pc("#FF000000"));

			try {
				if (!finalContent.isEmpty()) {
					SpannableStringBuilder ssb = new SpannableStringBuilder();
					String[] lines = finalContent.split("\n");
					int highlightIndex = 0; // 仅计数含#的行

					for (int i = 0; i < lines.length; i++) {
						String line = lines[i];
						SpannableString spannable = new SpannableString(line);
						int targetColor = line.contains("#") ?
							HIGHLIGHT_COLORS[highlightIndex++ % HIGHLIGHT_COLORS.length] :
							NORMAL_LINE_COLOR;

						spannable.setSpan(
							new ForegroundColorSpan(targetColor),
							0,
							line.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
						);

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
			
            // 应用统一主题
			applyUiTheme(activity, alertDialog, 0);
		}
	});
}

import com.tencent.qqnt.aio.markdown.api.IMarkdownFeatureCompatApi;
private Object markdownParser;
private java.lang.reflect.Method markdownParseMethod;
private int markdownParseArgCount = -1;
private java.lang.reflect.Method resolveMarkdownMethod(Object parser) {
    String[] names = new String[] { "b", "a" };
    Class[][] paramVariants = new Class[3][];
    paramVariants[0] = new Class[1];
    paramVariants[0][0] = String.class;
    paramVariants[1] = new Class[1];
    paramVariants[1][0] = CharSequence.class;
    paramVariants[2] = new Class[0];
    for (int i = 0; i < names.length; i++) {
        for (int j = 0; j < paramVariants.length; j++) {
            try {
                java.lang.reflect.Method m = parser.getClass().getMethod(names[i], paramVariants[j]);
                if (CharSequence.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    markdownParseArgCount = paramVariants[j].length;
                    return m;
                }
            } catch (NoSuchMethodException e) {}
        }
    }
    return null;
}
private String parseMarkdownToHtml(String markdown) throws Exception {
    if (markdownParser == null) {
        IMarkdownFeatureCompatApi svc = QRoute.api(IMarkdownFeatureCompatApi.class);
        java.lang.reflect.Field f = svc.getClass().getDeclaredField("$$delegate_1");
        f.setAccessible(true);
        markdownParser = f.get(svc);
        markdownParseMethod = resolveMarkdownMethod(markdownParser);
        if (markdownParseMethod == null) {
            throw new NoSuchMethodException("未找到可用的 Markdown 解析方法 (" + markdownParser.getClass().getName() + ")");
        }
    }
    Object result = markdownParseArgCount == 0
        ? markdownParseMethod.invoke(markdownParser)
        : markdownParseMethod.invoke(markdownParser, markdown);
    if (!(result instanceof CharSequence)) {
        throw new Exception("解析结果不是 CharSequence，实际: " + (result == null ? "null" : result.getClass().getName()));
    }
    CharSequence cs = (CharSequence) result;
    if (cs instanceof Spanned) {
        String html = Html.toHtml((Spanned) cs, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        html = decodeNumericEntities(html);
        html = html.replaceAll("\\n{2,}", "\n");
        return html;
    } else {
        throw new Exception("解析结果不是 Spanned");
    }
}

private String decodeNumericEntities(String input) {
    Pattern pattern = Pattern.compile("&#(\\d+);");
    Matcher matcher = pattern.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
        int code = Integer.parseInt(matcher.group(1));
        String replacement = new String(Character.toChars(code));
        matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
}

public void mkts(final Activity activity, final String title, final String markdownContent) {
    if (activity == null || activity.isFinishing()) {
        traceLog("dialog_log", "[mkts] Activity无效");
        return;
    }
    final String finalMarkdown = markdownContent == null ? "" : markdownContent;
    final boolean isDark = isThemeDark(activity);
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                traceLog("dialog_log", "[mkts] 开始解析，长度: " + finalMarkdown.length());
                String htmlContent = parseMarkdownToHtml(finalMarkdown);
                traceLog("dialog_log", "[mkts] 解析成功，输出长度: " + htmlContent.length());
                final String finalHtml = htmlContent;
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        createMarkdownDialog(activity, title, finalMarkdown, finalHtml, true, isDark);
                    }
                });
            } catch (Throwable e) {
                traceLog("dialog_log", "[mkts] 解析失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                String fallbackHtml = "<pre style='background:" + (isDark ? "#2D2D2D" : "#f4f4f4") + ";padding:6px;border-radius:3px;overflow-x:auto;font-family:monospace;font-size:13px;color:" + (isDark ? "#EFEFEF" : "#333") + ";'>" + escapeHtml(finalMarkdown) + "</pre>";
                final String finalFallback = fallbackHtml;
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        createMarkdownDialog(activity, title, finalMarkdown, finalFallback, false, isDark);
                    }
                });
            }
        }
    });
}

private void createMarkdownDialog(final Activity activity, final String title, final String originalMarkdown, final String initialHtml, final boolean parseSuccess, final boolean isDark) {
    if (activity == null) return;
    final boolean[] isReversed = {false};
    final WebView webView = new WebView(activity);
    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.setVerticalScrollBarEnabled(true);
    webView.setHorizontalScrollBarEnabled(true);
    webView.getSettings().setJavaScriptEnabled(false);
    webView.getSettings().setDefaultTextEncodingName("UTF-8");
    final Runnable loadContent = new Runnable() {
        public void run() {
            String htmlToShow;
            if (!parseSuccess) {
                htmlToShow = initialHtml;
            } else {
                String markdownToRender = originalMarkdown;
                if (isReversed[0]) {
                    markdownToRender = reverseMarkdownBlocks(originalMarkdown);
                }
                try {
                    htmlToShow = parseMarkdownToHtml(markdownToRender);
                } catch (Exception e) {
                    traceLog("dialog_log", "[createMarkdownDialog] 倒序解析失败: " + e.getMessage());
                    htmlToShow = "<pre style='background:" + (isDark ? "#2D2D2D" : "#f4f4f4") + ";padding:6px;border-radius:3px;overflow-x:auto;font-family:monospace;font-size:13px;color:" + (isDark ? "#EFEFEF" : "#333") + ";'>" + escapeHtml(originalMarkdown) + "</pre>";
                }
            }
            String wrappedHtml = wrapHtmlWithCss(htmlToShow, isDark);
            webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null);
        }
    };
    int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
    int webViewHeight = Math.min(screenHeight * 2 / 3, dp(activity, 800));
    webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, webViewHeight));
    LinearLayout container = new LinearLayout(activity);
    container.setOrientation(LinearLayout.VERTICAL);
    container.addView(webView);
    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.END);
    btnRow.setPadding(0, dp(activity, 12), 0, dp(activity, 4));
    final TextView btnClose = createButton(activity, "我知道了", tc(activity, "on_surface_variant"), Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
    btnRow.addView(btnClose);
    container.addView(btnRow);
    LinearLayout titleLayout = new LinearLayout(activity);
    titleLayout.setOrientation(LinearLayout.HORIZONTAL);
    titleLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    int paddingPx = dp(activity, 16);
    titleLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
    TextView titleText = new TextView(activity);
    titleText.setText(parseSuccess ? title : title + " (显示异常)");
    titleText.setTextSize(18);
    titleText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    titleText.setTextColor(isDark ? Color.WHITE : Color.BLACK);
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    TextView orderButton = createButton(activity, "⇅", isDark ? pc("#BBBBBB") : pc("#666666"), Color.TRANSPARENT, 20f, 0, 8, 0, false, 0, 0, null);
    orderButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            isReversed[0] = !isReversed[0];
            loadContent.run();
        }
    });
    titleLayout.addView(titleText);
    titleLayout.addView(orderButton);
    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setCustomTitle(titleLayout);
    builder.setView(container);

    builder.setCancelable(false);
    final AlertDialog alertDialog = builder.create();
    btnClose.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try {
                alertDialog.dismiss();
            } catch (Exception e) {
                traceLog("dialog_log", "[createMarkdownDialog] 关闭弹窗异常: " + e.getMessage());
            }
        }
    });
    alertDialog.show();
    applyUiTheme(activity, alertDialog, 1);
    loadContent.run();
    traceLog("dialog_log", "[createMarkdownDialog] Dialog显示成功，解析状态: " + parseSuccess);
}

private String reverseMarkdownBlocks(String markdown) {
    if (markdown == null || markdown.isEmpty()) return markdown;
    Pattern pattern = Pattern.compile("(?m)^### \\*\\*\\[v\\d+\\.\\d+\\.\\d+(?:\\.\\d+)?\\] - \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}\\*\\*");
    Matcher matcher = pattern.matcher(markdown);
    ArrayList blocks = new ArrayList();
    int lastPos = 0;
    while (matcher.find()) {
        if (lastPos < matcher.start()) {
            String textBefore = markdown.substring(lastPos, matcher.start());
            if (!textBefore.trim().isEmpty()) {
                blocks.add(textBefore);
            }
        }
        int start = matcher.start();
        int end = markdown.length();
        Matcher next = pattern.matcher(markdown);
        if (next.find(start + 1)) {
            end = next.start();
        }
        String block = markdown.substring(start, end);
        blocks.add(block);
        lastPos = end;
    }
    if (lastPos < markdown.length()) {
        String tail = markdown.substring(lastPos);
        if (!tail.trim().isEmpty()) {
            blocks.add(tail);
        }
    }
    ArrayList reversedBlocks = new ArrayList();
    for (int i = blocks.size() - 1; i >= 0; i--) {
        reversedBlocks.add(blocks.get(i));
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < reversedBlocks.size(); i++) {
        sb.append(reversedBlocks.get(i));
    }
    return sb.toString();
}

private String wrapHtmlWithCss(String htmlContent, boolean isDark) {
    String titleColor = isDark ? "#FFFFFF" : "#000000";
    String textColor = isDark ? "#E9EDF0" : "#1C1B1F";
    String codeBgColor = isDark ? "#1C1C1E" : "#F5F5F5";
    String borderColor = isDark ? "#3A3A3C" : "#E2E2E6";
    return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
        "<style>" +
        "*{margin:0;padding:0;box-sizing:border-box;}" +
        "body{font-family:'Google Sans','Roboto Flex',Roboto,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;font-size:15px;line-height:1.2;color:" + textColor + ";background:transparent;padding-left:2em;padding-right:2em;}" +
        "h1,h2,h3,h4{font-family:'Google Sans','Roboto Flex',Roboto,sans-serif;font-weight:bold;letter-spacing:-0.01em;margin-top:10px;margin-bottom:6px;color:" + titleColor + ";}" +
        "h1{font-size:20px;margin-top:4px;}h2{font-size:18px;}h3{font-size:16px;font-weight:bold;}" +
        "p{margin-bottom:8px;color:" + textColor + ";}" +
        "ul,ol{padding-left:0;margin:4px 0 8px 0;}" +
        "li{margin:3px 0;line-height:1.2;}" +
        "code{font-family:'JetBrains Mono','SF Mono','Fira Code',monospace;font-size:12px;background:" + codeBgColor + ";padding:1px 4px;border-radius:6px;color:" + (isDark ? "#FFB86C" : "#C01C2E") + ";}" +
        "pre{background:" + codeBgColor + ";padding:10px;border-radius:12px;overflow-x:auto;margin:8px 0;border:0.5px solid " + borderColor + ";}" +
        "pre code{background:transparent;padding:0;color:" + textColor + ";font-size:12px;}" +
        "blockquote{margin:8px 0;padding-left:0;border-left:3px solid " + (isDark ? "#5C5CFF" : "#6750A4") + ";color:" + (isDark ? "#BDBDC2" : "#4A4A4F") + ";font-style:normal;}" +
        "a{color:" + (isDark ? "#8AB4F8" : "#6750A4") + ";text-decoration:none;font-weight:500;}" +
        "img{max-width:100%;border-radius:12px;margin:8px 0;}" +
        "table{border-collapse:collapse;width:100%;margin:8px 0;border-radius:12px;overflow:hidden;}th,td{border:0.5px solid " + borderColor + ";padding:8px 10px;text-align:left;}th{background:" + (isDark ? "#1C1C1E" : "#F5F5F5") + ";font-weight:500;}" +
        "hr{margin:12px 0;border:none;height:1px;background:" + borderColor + ";}" +
        "</style></head><body>" + htmlContent + "</body></html>";
}

private String escapeHtml(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
}

interface GroupSelectCallback {
    void onSelected(List selected);
}

void showGroupSelector(final Activity act, final int mode, final List initSelected, final GroupSelectCallback callback) {
    if (act == null || act.isFinishing()) return;
    
    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final boolean isDark = isThemeDark(act);
                final int colorAccent = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
                final int colorOnSurface = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
                final int colorOnSurfaceVar = isDark ? pc("#99EFEFEF") : pc("#99000000");
                final int colorOutline = isDark ? pc("#33FFFFFF") : pc("#1A000000");
                final int colorInputBg = isDark ? pc("#1AFFFFFF") : pc("#0D000000");

                AlertDialog.Builder builder = new AlertDialog.Builder(act, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                
                LinearLayout root = new LinearLayout(act);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(act, 24), dp(act, 24), dp(act, 24), dp(act, 24));
                
                builder.setView(root);

                TextView title = new TextView(act);
                title.setText(mode == 1 ? "选择好友" : mode == 2 ? "选择群聊" : "选择群聊与好友");
                title.setTextSize(20);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setTextColor(colorOnSurface);
                root.addView(title);

                final TextView summary = new TextView(act);
                summary.setTextSize(14);
                summary.setTextColor(colorOnSurfaceVar);
                summary.setPadding(0, dp(act, 4), 0, dp(act, 16));
                root.addView(summary);

                EditText search = makeInput(act, mode == 1 ? "搜索昵称或QQ号" : mode == 2 ? "搜索群名或群号" : "搜索昵称或群名或QQ号", null);
                root.addView(search);

                ListView lv = new ListView(act);
                lv.setDivider(null);
                lv.setDividerHeight(0);
                lv.setClipToPadding(false);
                lv.setPadding(0, dp(act, 8), 0, dp(act, 8));
                root.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1f));

                LinearLayout row1 = new LinearLayout(act);
                row1.setOrientation(LinearLayout.HORIZONTAL);
                row1.setGravity(Gravity.END);
                row1.setPadding(0, dp(act, 12), 0, dp(act, 16));
                
                TextView bCancelAll = makeChip(act, "取消全选", false, 1);
                row1.addView(bCancelAll);
                
                TextView bReverse = makeChip(act, "反选", false, 1);
                LinearLayout.LayoutParams lpRev = new LinearLayout.LayoutParams(-2, -2);
                lpRev.leftMargin = dp(act, 8);
                row1.addView(bReverse, lpRev);
                
                TextView bSelectAll = makeChip(act, "全选", false, 1);
                LinearLayout.LayoutParams lpAll = new LinearLayout.LayoutParams(-2, -2);
                lpAll.leftMargin = dp(act, 8);
                row1.addView(bSelectAll, lpAll);
                
                root.addView(row1);

                LinearLayout row2 = new LinearLayout(act);
                row2.setOrientation(LinearLayout.HORIZONTAL);
                row2.setGravity(Gravity.END);
                row2.setPadding(0, dp(act, 8), 0, 0);
                
                TextView btnCancel = createButton(act, "取消", colorAccent, Color.TRANSPARENT, 14f, 0, 16, 12, false, 0, 0, null);
                row2.addView(btnCancel);

                TextView btnConfirm = createButton(act, "确定", colorAccent, Color.TRANSPARENT, 14f, 0, 16, 12, true, 0, 0, null);
                row2.addView(btnConfirm);
                
                root.addView(row2);

                final AlertDialog[] ref = new AlertDialog[1];
                ref[0] = builder.create();
                Window w = ref[0].getWindow();
                if (w != null) {
                    w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    w.setLayout((int)(act.getResources().getDisplayMetrics().widthPixels * 0.85), (int)(act.getResources().getDisplayMetrics().heightPixels * 0.85));
                }
                
                applyUiTheme(act, ref[0], 0);
                ref[0].show();
                
                final List allItems = new ArrayList();
                
                ThreadPool.submit(new Runnable() {
                    public void run() {
                        try {
                            if (mode == 1 || mode == 3) {
                                List friendList = (List) getAllFriend();
                                if (friendList != null) {
                                    for (Object obj : friendList) {
                                        if (obj == null) continue;
                                        String displayName = (obj.remark != null && !obj.remark.isEmpty()) ? obj.remark : obj.name;
                                        Map m = new HashMap();
                                        m.put("uin", obj.uin);
                                        m.put("name", displayName);
                                        allItems.add(m);
                                    }
                                }
                            }

                            if (mode == 2 || mode == 3) {
                                List groupList = (List) getGroupList();
                                if (groupList != null) {
                                    for (Object obj : groupList) {
                                        if (obj == null) continue;
                                        Map m = new HashMap();
                                        m.put("uin", obj.group);
                                        m.put("name", obj.groupName);
                                        allItems.add(m);
                                    }
                                }
                            }
                            
                            act.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (allItems.isEmpty()) {
                                        Map test = new HashMap();
                                        test.put("uin", "000000");
                                        test.put("name", "未获取到数据 (请检查权限)");
                                        allItems.add(test);
                                    }

                                    final List display = new ArrayList();
                                    final Set selected = new HashSet(initSelected != null ? initSelected : new ArrayList());

                                    for (Object o : allItems) {
                                        Map m = (Map) o;
                                        if (selected.contains(m.get("uin"))) display.add(m);
                                    }
                                    for (Object o : allItems) {
                                        Map m = (Map) o;
                                        if (!selected.contains(m.get("uin"))) display.add(m);
                                    }

                                    BaseAdapter adapter = new BaseAdapter() {
                                        public int getCount() { return display.size(); }
                                        public Object getItem(int p) { return display.get(p); }
                                        public long getItemId(int p) { return p; }

                                        public View getView(int pos, View cv, ViewGroup parent) {
                                            LinearLayout item;
                                            if (cv == null) {
                                                item = new LinearLayout(act);
                                                item.setOrientation(LinearLayout.HORIZONTAL);
                                                item.setGravity(Gravity.CENTER_VERTICAL);
                                                item.setPadding(dp(act, 8), dp(act, 12), dp(act, 8), dp(act, 12));
                                                
                                                item.setBackground(createRippleBg(act, Color.TRANSPARENT, 0));
                                            } else {
                                                item = (LinearLayout) cv;
                                                item.removeAllViews();
                                            }

                                            final Map m = (Map) display.get(pos);
                                            final String uin = (String) m.get("uin");
                                            final boolean isChecked = selected.contains(uin);

                                            FrameLayout checkContainer = new FrameLayout(act);
                                            checkContainer.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 24), dp(act, 24)));

                                            View box = new View(act);
                                            GradientDrawable boxBg = new GradientDrawable();
                                            boxBg.setCornerRadius(dp(act, 4));
                                            boxBg.setStroke(dp(act, 2), isChecked ? colorAccent : colorOutline);
                                            boxBg.setColor(isChecked ? colorAccent : Color.TRANSPARENT);
                                            box.setBackground(boxBg);
                                            checkContainer.addView(box, new FrameLayout.LayoutParams(-1, -1));

                                            View checkMark = createCheckMarkView(act, isChecked, colorAccent);
                                            checkContainer.addView(checkMark, new FrameLayout.LayoutParams(-1, -1));

                                            item.addView(checkContainer);

                                            TextView tv = new TextView(act);
                                            tv.setText(m.get("name") + " (" + uin + ")");
                                            tv.setTextSize(16);
                                            tv.setTextColor(colorOnSurface);
                                            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, -2, 1);
                                            tvLp.leftMargin = dp(act, 16);
                                            item.addView(tv, tvLp);

                                            View.OnClickListener clickListener = new View.OnClickListener() {
                                                public void onClick(View v) {
                                                    boolean nowChecked = !selected.contains(uin);
                                                    if (nowChecked) {
                                                        selected.add(uin);
                                                        
                                                        boxBg.setStroke(dp(act, 2), colorAccent);
                                                        boxBg.setColor(colorAccent);
                                                        
                                                        animateCheckMark(checkMark, true);
                                                    } else {
                                                        selected.remove(uin);
                                                        
                                                        boxBg.setStroke(dp(act, 2), colorOutline);
                                                        boxBg.setColor(Color.TRANSPARENT);
                                                        
                                                        animateCheckMark(checkMark, false);
                                                    }
                                                    summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                                                }
                                            };

                                            item.setOnClickListener(clickListener);
                                            return item;
                                        }
                                    };
                                    
                                    try {
                                        lv.setAdapter(adapter);
                                        summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                                    } catch (Throwable e) { traceLog("dialog_log", "[getView] 异常: " + e); }

                                    final Runnable searchRunnable = new Runnable() {
                                        public void run() {
                                            String q = search.getText().toString().trim().toLowerCase();
                                            display.clear();
                                            for (Object o : allItems) {
                                                Map m = (Map) o;
                                                if (q.isEmpty() || ((String)m.get("name")).toLowerCase().contains(q) || ((String)m.get("uin")).contains(q)) {
                                                    display.add(m);
                                                }
                                            }
                                            adapter.notifyDataSetChanged();
                                            summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                                        }
                                    };
                                    
                                    search.addTextChangedListener(new TextWatcher() {
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                        public void afterTextChanged(Editable s) {
                                            search.removeCallbacks(searchRunnable);
                                            search.postDelayed(searchRunnable, 300);
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
                                            Set temp = new HashSet(selected);
                                            selected.clear();
                                            for (Object o : display) {
                                                Map m = (Map) o;
                                                String u = (String) m.get("uin");
                                                if (!temp.contains(u)) selected.add(u);
                                            }
                                            adapter.notifyDataSetChanged();
                                            summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                                        }
                                    });

                                    bSelectAll.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            for (Object o : display) {
                                                Map m = (Map) o;
                                                selected.add(m.get("uin"));
                                            }
                                            adapter.notifyDataSetChanged();
                                            summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                                        }
                                    });

                                    btnCancel.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            if (ref[0] != null) ref[0].dismiss();
                                        }
                                    });

                                    btnConfirm.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            if (callback != null) {
                                                callback.onSelected(new ArrayList(selected));
                                            }
                                            if (ref[0] != null) ref[0].dismiss();
                                        }
                                    });
                                }
                            });
                        } catch (Exception dataEx) {
                            act.runOnUiThread(new Runnable() {
                                public void run() {
                                    qqToast(1, "获取数据失败: " + dataEx.getMessage());
                                }
                            });
                        }
                    }
                });
                
            } catch (Exception e) {
                qqToast(1, "构建选择器失败: " + e.getMessage());
            }
        }
    });
}

class CheckMarkView extends View {
    private float drawProgress;
    private int checkColor;

    public CheckMarkView(Activity act, boolean isChecked, boolean backgroundIsBlue) {
        super(act);
        
        final boolean isDark = isThemeDark(act);
        
        // 核心逻辑：蓝色背景时强制高对比色
        if (backgroundIsBlue) {
            this.checkColor = isDark ? Color.BLACK : Color.WHITE;
        } else {
            // 非蓝色背景时，可用主题色或其他（这里默认白色，防止意外）
            this.checkColor = Color.WHITE;
        }
        
        this.drawProgress = isChecked ? 1f : 0f;
        setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        
        int size = dp(act, 24);
        setLayoutParams(new FrameLayout.LayoutParams(size, size));
    }

    public float getDrawProgress() {
        return drawProgress;
    }

    public void setDrawProgress(float progress) {
        drawProgress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        if (drawProgress <= 0f) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float scale = Math.min(w, h) / 24f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(checkColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * scale);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        Path path = new Path();
        path.moveTo(5f * scale, 12f * scale);
        path.lineTo(10f * scale, 17f * scale);
        path.lineTo(19f * scale, 6f * scale);

        PathMeasure measure = new PathMeasure(path, false);
        float length = measure.getLength();
        Path dst = new Path();
        measure.getSegment(0, length * drawProgress, dst, true);

        canvas.drawPath(dst, paint);
    }
}

View createCheckMarkView(Activity act, boolean isChecked, boolean backgroundIsBlue) {
    return new CheckMarkView(act, isChecked, backgroundIsBlue);
}
void animateCheckMark(View checkMark, boolean show) {
    if (checkMark == null) return;

    if (show) {
        checkMark.setVisibility(View.VISIBLE);
    }

    float target = show ? 1f : 0f;
    ObjectAnimator anim = ObjectAnimator.ofFloat(checkMark, "drawProgress", new float[]{target});
    anim.setDuration(200);
    anim.setInterpolator(new AccelerateDecelerateInterpolator());

    if (!show) {
        anim.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) {}
            public void onAnimationEnd(Animator animation) {
                checkMark.setVisibility(View.INVISIBLE);
            }
            public void onAnimationCancel(Animator animation) {}
            public void onAnimationRepeat(Animator animation) {}
        });
    }

    anim.start();
}

/**
 * 解析时长字符串为秒数
 * 纯数字直接当秒数；含冒号按 日:时:分:秒:毫秒 解析（忽略毫秒）
 */
int parseDurationToSeconds(String input) {
    if (input == null || input.trim().isEmpty()) return 0;
    String s = input.trim();
    if (s.matches("\\d+")) {
        try { return Integer.parseInt(s); } catch (Throwable e) { return 0; }
    }
    String[] parts = s.split(":");
    int len = parts.length;
    int seconds = 0;
    try {
        if (len >= 2) seconds += Integer.parseInt(parts[len-2]);
        if (len >= 3) seconds += Integer.parseInt(parts[len-3]) * 60;
        if (len >= 4) seconds += Integer.parseInt(parts[len-4]) * 3600;
        if (len >= 5) seconds += Integer.parseInt(parts[len-5]) * 86400;
    } catch (Throwable e) { traceLog("dialog_log", "[parseDurationToSeconds] 异常: " + e); }
    return seconds;
}

/**
 * 格式化时间显示
 * @param raw String: 原始时间串
 * @return String: 格式化后的时间串
 */
String formatTimeDisplay(String raw) {
    if (raw == null || raw.equals("")) return "";
    String n = raw.replaceAll("[^0-9]", "");
    if (n.length() < 4) return raw;
    while (n.length() < 10) n = "0" + n;
    int d = Integer.parseInt(n.substring(0, 2));
    int h = Integer.parseInt(n.substring(2, 4));
    int min = Integer.parseInt(n.substring(4, 6));
    int s = Integer.parseInt(n.substring(6, 8));
    int ms = Integer.parseInt(n.substring(8, 10));
    StringBuilder sb = new StringBuilder();
    if (d > 0) sb.append(d).append(":");
    if (h > 0 || sb.length() > 0) sb.append(h).append(":");
    if (min > 0 || sb.length() > 0) sb.append(min).append(":");
    sb.append(s).append(":").append(ms);
    return sb.toString();
}

/**
 * 显示时间选择器
 * 支持键盘输入、钟表选择、快捷换算、单项输入四种模式
 * @param a Activity
 * @param target 目标输入框，选择结果写入此输入框
 */
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

                final String[] val = {""};
                String current = target.getText().toString().replaceAll("[^0-9]", "");
                int totalSec = 0;
                try { if (current.length() > 0) totalSec = Integer.parseInt(current); } catch (Throwable e) { traceLog("dialog_log", "[showTimePicker] 异常: " + e); }
                int initD = totalSec / 86400;
                int initH = (totalSec % 86400) / 3600;
                int initM = (totalSec % 3600) / 60;
                int initS = totalSec % 60;
                if (initD > 30) initD = 30;
                val[0] = (initD < 10 ? "0" + initD : "" + initD) + (initH < 10 ? "0" + initH : "" + initH) + (initM < 10 ? "0" + initM : "" + initM) + (initS < 10 ? "0" + initS : "" + initS) + "00";
                final int[] vs = {initD, initH, initM, initS, 0};
                final android.widget.NumberPicker[] pickers = new android.widget.NumberPicker[5];

                final int[] mode = {0};
                final String[] names = {"键盘输入", "钟表选择", "快捷换算", "单项输入"};
                final Runnable[] build = new Runnable[1];
                build[0] = new Runnable() {
                    public void run() {
                        container.removeAllViews();
                        if (mode[0] == 0) {
                            EditText e = makeInput(a, "输入秒数(如3600=1小时)", null);
                            int initSec = 0;
                            try {
                                String n = val[0].replaceAll("[^0-9]", "");
                                while (n.length() < 10) n = "0" + n;
                                initSec = Integer.parseInt(n.substring(0,2))*86400 + Integer.parseInt(n.substring(2,4))*3600 + Integer.parseInt(n.substring(4,6))*60 + Integer.parseInt(n.substring(6,8));
                            } catch (Throwable e1) { traceLog("dialog_log", "[showTimePicker] 异常: " + e1); }
                            e.setText(String.valueOf(initSec));
                            e.addTextChangedListener(new android.text.TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                public void afterTextChanged(android.text.Editable s) {
                                    try {
                                        int sec = Integer.parseInt(s.toString().replaceAll("[^0-9]", ""));
                                        int d = sec / 86400;
                                        int h = (sec % 86400) / 3600;
                                        int m = (sec % 3600) / 60;
                                        int sc = sec % 60;
                                        if (d > 30) d = 30;
                                        val[0] = (d < 10 ? "0" + d : "" + d) + (h < 10 ? "0" + h : "" + h) + (m < 10 ? "0" + m : "" + m) + (sc < 10 ? "0" + sc : "" + sc) + "00";
                                    } catch (Throwable e) { traceLog("dialog_log", "[showTimePicker] 异常: " + e); }
                                }
                            });
                            container.addView(e);
                        } else if (mode[0] == 1) {
                            HorizontalScrollView hs = new HorizontalScrollView(a);
                            LinearLayout row = new LinearLayout(a);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            hs.addView(row);
                            container.addView(hs);
                            String[] lbls = {"日", "时", "分", "秒", "毫秒"};
                            try {
                                vs[0] = Integer.parseInt(val[0].substring(0,2));
                                vs[1] = Integer.parseInt(val[0].substring(2,4));
                                vs[2] = Integer.parseInt(val[0].substring(4,6));
                                vs[3] = Integer.parseInt(val[0].substring(6,8));
                                vs[4] = Integer.parseInt(val[0].substring(8,10));
                            } catch (Throwable e) { traceLog("dialog_log", "[showTimePicker] 异常: " + e); }
                            for (int i=0; i<5; i++) {
                                LinearLayout col = new LinearLayout(a);
                                col.setOrientation(LinearLayout.VERTICAL);
                                col.setPadding(dp(a,4),0,dp(a,4),0);
                                TextView l = new TextView(a);
                                l.setText(lbls[i]);
                                l.setTextSize(11);
                                l.setGravity(Gravity.CENTER);
                                l.setTextColor(tc(a, "on_surface_variant"));
                                col.addView(l);
                                NumberPicker p = new NumberPicker(a);
                                int max = (i==0)?30:(i==1)?23:(i==4)?99:59;
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
                            int initSec2 = 0;
                            try {
                                String n2 = val[0].replaceAll("[^0-9]", "");
                                while (n2.length() < 10) n2 = "0" + n2;
                                initSec2 = Integer.parseInt(n2.substring(0,2))*86400 + Integer.parseInt(n2.substring(2,4))*3600 + Integer.parseInt(n2.substring(4,6))*60 + Integer.parseInt(n2.substring(6,8));
                            } catch (Throwable e2) { traceLog("dialog_log", "[onValueChange] 异常: " + e2); }
                            e.setText(String.valueOf(initSec2));
                            e.addTextChangedListener(new android.text.TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                public void afterTextChanged(android.text.Editable s) {
                                    try {
                                        int sec = Integer.parseInt(s.toString());
                                        int h = sec / 3600;
                                        int m = (sec % 3600) / 60;
                                        int sc = sec % 60;
                                        val[0] = "00" + (h < 10 ? "0" + h : "" + h) + (m < 10 ? "0" + m : "" + m) + (sc < 10 ? "0" + sc : "" + sc) + "00";
                                    } catch (Throwable e) { traceLog("dialog_log", "[onValueChange] 异常: " + e); }
                                }
                            });
                            container.addView(e);
                        } else {
                            GridLayout grid = new GridLayout(a);
                            grid.setColumnCount(2);
                            container.addView(grid);
                            String[] lbls = {"日:", "时:", "分:", "秒:", "毫秒:"};
                            final EditText[] ets = new EditText[5];
                            final boolean[] isSettingInitial = {true};
                            for (int i=0; i<5; i++) {
                                TextView l = new TextView(a);
                                l.setText(lbls[i]);
                                l.setTextColor(tc(a, "on_surface"));
                                grid.addView(l);
                                final EditText et = makeInput(a, i==4?"000":"00", null);
                                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                                et.setGravity(Gravity.CENTER);
                                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                                lp.width = dp(a, 60);
                                et.setLayoutParams(lp);
                                grid.addView(et);
                                ets[i] = et;
                                et.addTextChangedListener(new android.text.TextWatcher() {
                                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                    public void afterTextChanged(android.text.Editable s) {
                                        if (isSettingInitial[0]) return;
                                        try {
                                            int d = Integer.parseInt(ets[0].getText().toString().isEmpty() ? "0" : ets[0].getText().toString());
                                            int h = Integer.parseInt(ets[1].getText().toString().isEmpty() ? "0" : ets[1].getText().toString());
                                            int m = Integer.parseInt(ets[2].getText().toString().isEmpty() ? "0" : ets[2].getText().toString());
                                            int sc = Integer.parseInt(ets[3].getText().toString().isEmpty() ? "0" : ets[3].getText().toString());
                                            int ms = Integer.parseInt(ets[4].getText().toString().isEmpty() ? "0" : ets[4].getText().toString());
                                            val[0] = (d < 10 ? "0" + d : "" + d) + (h < 10 ? "0" + h : "" + h) + (m < 10 ? "0" + m : "" + m) + (sc < 10 ? "0" + sc : "" + sc) + (ms < 10 ? "0" + ms : "" + ms);
                                        } catch (Throwable e) { traceLog("dialog_log", "[afterTextChanged] 异常: " + e); }
                                    }
                                });
                            }
                            try {
                                ets[0].setText(val[0].substring(0,2));
                                ets[1].setText(val[0].substring(2,4));
                                ets[2].setText(val[0].substring(4,6));
                                ets[3].setText(val[0].substring(6,8));
                                ets[4].setText(val[0].substring(8,10));
                            } catch (Throwable e) { traceLog("dialog_log", "[afterTextChanged] 异常: " + e); }
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

                TextView btnMode = createButton(a, " " + names[0], tc(a, "primary"), Color.TRANSPARENT, 12f, 0, 12, 8, false, 0, 0, null);
                ctrl.addView(btnMode);
                View sp = new View(a);
                sp.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1.0f));
                ctrl.addView(sp);
                TextView btnCancel = createButton(a, "取消", tc(a, "on_surface_variant"), Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
                ctrl.addView(btnCancel);
                TextView btnOk = createButton(a, "确定", Color.WHITE, tc(a, "primary"), 14f, 6, 16, 8, false, 0, 0, null);
                ctrl.addView(btnOk);

                btnCancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        animateDialogOut(d, null);
                    }
                });
                btnMode.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mode[0] = (mode[0] + 1) % 4;
                        ((TextView)v).setText(" " + names[mode[0]]);
                        build[0].run();
                    }
                });
                btnOk.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            if (mode[0] == 1 && pickers[0] != null) {
                                for (int i=0; i<5; i++) {
                                    if (pickers[i] != null) {
                                        try { vs[i] = pickers[i].getValue(); } catch (Throwable e) { traceLog("dialog_log", "[showTimePicker] picker[" + i + "] 错误: " + e); }
                                    }
                                }
                                val[0] = (vs[0] < 10 ? "0" + vs[0] : "" + vs[0]) + (vs[1] < 10 ? "0" + vs[1] : "" + vs[1]) + (vs[2] < 10 ? "0" + vs[2] : "" + vs[2]) + (vs[3] < 10 ? "0" + vs[3] : "" + vs[3]) + (vs[4] < 10 ? "0" + vs[4] : "" + vs[4]);
                            }
                            String n = val[0].replaceAll("[^0-9]", "");
                            while (n.length() < 10) n = "0" + n;
                            int days = Integer.parseInt(n.substring(0, 2));
                            int hours = Integer.parseInt(n.substring(2, 4));
                            int mins = Integer.parseInt(n.substring(4, 6));
                            int secs = Integer.parseInt(n.substring(6, 8));
                            int totalSeconds = days * 86400 + hours * 3600 + mins * 60 + secs;
                            traceLog("dialog_log", "[showTimePicker] 确定点击, mode=" + mode[0] + ", val=" + val[0] + ", seconds=" + totalSeconds + ", targetNull=" + (target == null));
                            target.setText(String.valueOf(totalSeconds));
                            animateDialogOut(d, null);
                        } catch (Throwable e) {
                            traceLog("dialog_log", "[showTimePicker] ok click 错误: " + e);
                            try { d.dismiss(); } catch (Throwable e2) { traceLog("dialog_log", "[onClick] 异常: " + e2); }
                        }
                    }
                });

                d.setContentView(outer);
                d.getWindow().setLayout(Math.min(dp(a, 400), a.getResources().getDisplayMetrics().widthPixels - dp(a, 32)), -2);
                d.show();
                applyUiTheme(a, d, 1);
                animateDialogIn(d);
            } catch (Throwable e) {
                traceLog("dialog_log", "[showTimePicker]" + e);
            }
        }
    });
}

