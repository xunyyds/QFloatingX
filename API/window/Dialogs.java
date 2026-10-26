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
