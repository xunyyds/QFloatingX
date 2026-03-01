import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import android.os.Handler;
import android.os.Looper;

/**
 * 从键值对字符串中提取指定前缀后的值
 * @param pair 键值对字符串
 * @param prefix 前缀
 * @return 提取的值
 */
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

/**
 * 将角色标识转换为中文显示名称
 * @param role 角色标识
 * @return 中文显示名称
 */
String convertRole(String role) {
    if ("OWNER".equals(role)) return "群主";
    if ("ADMIN".equals(role)) return "管理员";
    if ("MEMBER".equals(role)) return "普通成员";
    return role != null ? role : "未知";
}

/**
 * 将时间戳格式化为日期时间字符串
 * @param timestamp 时间戳（毫秒）
 * @return 格式化后的日期时间字符串
 */
String timestampToDate(long timestamp) {
    try {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    } catch (Throwable e) {
        return "时间错误";
    }
}

/**
 * 根据禁言结束时间戳获取禁言状态描述
 * @param timestamp 禁言结束时间戳（秒）
 * @return 禁言状态描述
 */
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

/**
 * 将秒数格式化为可读的时间长度字符串
 * @param seconds 秒数
 * @return 格式化后的时间字符串
 */
String formatRemainingTime(long seconds) {
    if (seconds < 60) return seconds + "秒";
    if (seconds < 3600) return (seconds / 60) + "分钟" + (seconds % 60) + "秒";
    if (seconds < 86400) return (seconds / 3600) + "小时" + ((seconds % 3600) / 60) + "分钟";
    return (seconds / 86400) + "天" + ((seconds % 86400) / 3600) + "小时";
}

/**
 * 简单美化字符串
 * @param obj 要格式化的字符串
 * @return 格式化后的字符串
 */
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
/**
 * 菜单功能入口方法
 * @param data 消息数据对象
 */
void 菜单(Object data) {
    if (!非UI初始化完成) {
        traceLog("api_log.txt", "菜单调用时延迟启动未完成");
        return;
    }
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
    if (activity == null) {
        traceLog("api_log.txt", "菜单调用时无法获取Activity");
        return;
    }
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (!activity.isFinishing()) 长按消息菜单(activity, data);
            } catch (Throwable t) {}
        }
    });
}

/**
 * 显示禁言成员设置弹窗
 * @param activity Activity实例
 * @param qun 群号
 * @param uin 成员QQ号
 * @param nickName 成员昵称
 */
public void showShutUpDialog(Activity activity, String qun, String uin, String nickName) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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

                EditText input = new EditText(activity);
                input.setHint("请输入禁言时长(秒)，0=解除");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(14);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                input.setText("0");
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(activity, 1), borderColor);
                input.setBackground(inputBg);
                root.addView(input);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = new TextView(activity);
                confirm.setText("确定");
                confirm.setTextSize(15);
                confirm.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int seconds = 0;
                        try { seconds = Integer.parseInt(input.getText().toString().trim()); } catch (Throwable ignored) {}
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) {
                Toast("弹窗显示失败");
            }
        }
    });
}

/**
 * 显示全体禁言设置弹窗
 * @param activity Activity实例
 * @param qun 群号
 */
public void showMuteAllDialog(Activity activity, String qun) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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
                
                final EditText hInput = new EditText(activity);
                hInput.setHint("时"); hInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                final EditText mInput = new EditText(activity);
                mInput.setHint("分"); mInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                final EditText sInput = new EditText(activity);
                sInput.setHint("秒"); sInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                
                EditText[] inputs = new EditText[]{hInput, mInput, sInput};
                for (int i = 0; i < inputs.length; i++) {
                    EditText in = inputs[i];
                    in.setTextColor(textColor);
                    in.setHintTextColor(subTextColor);
                    in.setGravity(Gravity.CENTER);
                    GradientDrawable bg = new GradientDrawable();
                    bg.setCornerRadius(dp(activity, 8)); 
                    bg.setColor(inputBgColor); 
                    bg.setStroke(dp(activity, 1), borderColor);
                    in.setBackground(bg);
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    p.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
                    in.setLayoutParams(p);
                    inputRow.addView(in);
                }
                root.addView(inputRow);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = new TextView(activity);
                cancel.setText("取消"); cancel.setTextSize(15); cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView unmute = new TextView(activity);
                unmute.setText("解禁"); unmute.setTextSize(15); unmute.setTextColor(Color.parseColor("#81C784"));
                unmute.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                unmute.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try { shutUpAll(qun, false); Toast("已解除全体禁言"); } catch(Throwable t){}
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = new TextView(activity);
                confirm.setText("确定"); confirm.setTextSize(15); confirm.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        long h = 0, m = 0, s = 0;
                        try { h = Long.parseLong(hInput.getText().toString()); } catch(Throwable t){}
                        try { m = Long.parseLong(mInput.getText().toString()); } catch(Throwable t){}
                        try { s = Long.parseLong(sInput.getText().toString()); } catch(Throwable t){}
                        long totalSec = h * 3600L + m * 60L + s;
                        
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
                                        } catch (Throwable t) {}
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
                applyUiTheme(activity, ref[0]);
            } catch(Throwable t) {}
        }
    });
}

/**
 * 显示点赞次数输入弹窗
 * @param activity Activity实例
 * @param targetUin 目标QQ号
 */
public void showZanDialog(Activity activity, String targetUin) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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

                final EditText input = new EditText(activity);
                input.setHint("请输入点赞次数");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(14);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                input.setText("50");
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(activity, 1), borderColor);
                input.setBackground(inputBg);
                root.addView(input);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = new TextView(activity);
                confirm.setText("确定");
                confirm.setTextSize(15);
                confirm.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int count = 50;
                        try { count = Integer.parseInt(input.getText().toString().trim()); } catch (Throwable ignored) {}
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) { Toast("弹窗显示失败"); }
        }
    });
}

/**
 * 显示拍一拍次数输入弹窗
 * @param activity Activity实例
 * @param targetUin 目标QQ号
 * @param peerUin 聊天对象QQ号
 * @param chatType 聊天类型
 */
public void showPaiDialog(Activity activity, String targetUin, String peerUin, int chatType) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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

                final EditText input = new EditText(activity);
                input.setHint("请输入拍一拍次数");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(14);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                input.setText("1");
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(activity, 1), borderColor);
                input.setBackground(inputBg);
                root.addView(input);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });

                TextView confirm = new TextView(activity);
                confirm.setText("确定");
                confirm.setTextSize(15);
                confirm.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int count = 1;
                        try { count = Integer.parseInt(input.getText().toString().trim()); } catch (Throwable ignored) {}
                        if (count < 1) count = 1;
                        if (count > 200) count = 200;
                        if (ref[0] != null) ref[0].dismiss();
                        
                        final int finalCount = count;
                        ThreadPool.execute(new Runnable() {
                            public void run() {
                                for (int i = 0; i < finalCount; i++) {
                                    try {
                                        sendPai(targetUin, peerUin, chatType);
                                    } catch (Throwable t) {}
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) { Toast("弹窗显示失败"); }
        }
    });
}

/**
 * 显示加解密工具弹窗
 * @param activity Activity实例
 * @param originalText 原始文本
 */
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
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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
                                    if ("base64_enc".equals(tag)) result = encryptBase64Safe(cur);
                                    else if ("base64_dec".equals(tag)) result = decryptBase64Safe(cur);
                                    else if ("unicode_enc".equals(tag)) result = encryptUnicodeSafe(cur);
                                    else if ("unicode_dec".equals(tag)) result = decryptUnicodeSafe(cur);
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
                    TextView btn = new TextView(activity);
                    btn.setText(btnNames[i]);
                    btn.setTextSize(11);
                    btn.setTextColor(textColor);
                    btn.setGravity(Gravity.CENTER);
                    btn.setPadding(dp(activity, 4), dp(activity, 10), dp(activity, 4), dp(activity, 10));
                    btn.setTag(btnTags[i]);
                    btn.setOnClickListener(clickListener);
                    GradientDrawable btnBg = new GradientDrawable();
                    btnBg.setCornerRadius(dp(activity, 8));
                    btnBg.setColor(isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#F0F0F0"));
                    if ("clear".equals(btnTags[i])) {
                        btnBg.setStroke(dp(activity, 1), Color.parseColor("#FF5252"));
                        btn.setTextColor(Color.parseColor("#FF5252"));
                    } else if ("undo".equals(btnTags[i]) || "redo".equals(btnTags[i])) {
                        btnBg.setStroke(dp(activity, 1), Color.parseColor("#FFB74D"));
                        btn.setTextColor(isDark ? Color.parseColor("#FFB74D") : Color.parseColor("#EF6C00"));
                    } else if ("send".equals(btnTags[i])) {
                        btnBg.setStroke(dp(activity, 1), Color.parseColor("#4CAF50"));
                        btn.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        btnBg.setStroke(dp(activity, 1), borderColor);
                    }
                    btn.setBackground(btnBg);
                    GridLayout.LayoutParams p = new GridLayout.LayoutParams();
                    p.width = 0;
                    p.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    p.columnSpec = GridLayout.spec(i % 3, 1f);
                    p.rowSpec = GridLayout.spec(i / 3);
                    p.setMargins(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
                    btn.setLayoutParams(p);
                    btnGrid.addView(btn);
                }
                root.addView(btnGrid);

                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 16), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                TextView cancel = new TextView(activity);
                cancel.setText("关闭");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) { Toast("弹窗显示失败: " + e.getMessage()); }
        }
    });
}

/**
 * 显示代码执行控制台弹窗
 * @param activity Activity实例
 * @param data 消息数据对象
 */
public void showCodeConsoleDialog(Activity activity, Object data) {
    if (activity == null || activity.isFinishing()) return;
    if (data == null) { Toast("数据无效"); return; }
    String initialCode = "";
    try { initialCode = String.valueOf(data.getClass().getField("msg").get(data)); } catch (Throwable e) {}
    final String finalCode = initialCode;
    final Object outerInterpreter = this.interpreter;

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
                int borderColor = adjustAlpha(textColor, 0.3f);
                int successColor = isDark ? Color.parseColor("#81C784") : Color.parseColor("#2E7D32");
                int errorColor = isDark ? Color.parseColor("#FF8A80") : Color.parseColor("#C62828");

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
                TextView execBtn = new TextView(activity);
                execBtn.setText("▶  执行代码");
                execBtn.setTextSize(14);
                execBtn.setTextColor(Color.WHITE);
                execBtn.setGravity(Gravity.CENTER);
                execBtn.setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), dp(activity, 12));
                GradientDrawable execBg = new GradientDrawable();
                execBg.setColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                execBg.setCornerRadius(dp(activity, 8));
                execBtn.setBackground(execBg);
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
                                    try { outerInterpreter.set("msg", data.getClass().getField("data").get(data)); } catch(Throwable t){}
                                    try { outerInterpreter.set("qun", data.getClass().getField("peerUin").get(data)); } catch(Throwable t){}
                                    try { outerInterpreter.set("uin", data.getClass().getField("userUin").get(data)); } catch(Throwable t){}
                                    try { outerInterpreter.set("type", data.getClass().getField("type").get(data)); } catch(Throwable t){}
                                    try { outerInterpreter.set("msgtype", data.getClass().getField("msgType").get(data)); } catch(Throwable t){}
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
                TextView cancel = new TextView(activity);
                cancel.setText("关闭");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) { Toast("弹窗显示失败"); }
        }
    });
}

/**
 * 显示展开完整内容弹窗
 * @param activity Activity实例
 * @param content 要显示的内容
 * @param isDark 是否深色主题
 */
public void showExpandedResultDialog(Activity activity, String content, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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

                TextView copyBtn = new TextView(activity);
                copyBtn.setText("复制全部");
                copyBtn.setTextSize(15);
                copyBtn.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                copyBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                copyBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                            if (cb != null) { cb.setText(content != null ? content : ""); Toast("已复制到剪贴板"); }
                        } catch (Throwable e) { Toast("复制失败"); }
                    }
                });

                TextView closeBtn = new TextView(activity);
                closeBtn.setText("关闭");
                closeBtn.setTextSize(15);
                closeBtn.setTextColor(subTextColor);
                closeBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) { Toast("展开显示失败: " + e.getMessage()); }
        }
    });
}

/**
 * 显示设置群头衔弹窗
 * @param activity Activity实例
 * @param qun 群号
 * @param uin 成员QQ号
 * @param nickName 成员昵称
 */
public void showTitleDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
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
                final EditText input = new EditText(activity);
                input.setHint("请输入头衔（留空则清除）");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(14);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT);
                inputBg.setStroke(dp(activity, 1), borderColor);
                input.setBackground(inputBg);
                root.addView(input);
                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = new TextView(activity);
                confirm.setText("确定");
                confirm.setTextSize(15);
                confirm.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) { Toast("弹窗显示失败: " + e.getMessage()); }
        }
    });
}

/**
 * 显示设置管理员弹窗
 * @param activity Activity实例
 * @param qun 群号
 * @param uin 成员QQ号
 * @param nickName 成员昵称
 */
public void showSetAdminDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? Color.parseColor("#FFE0E0E0") : Color.parseColor("#FF202020");
                int subTextColor = isDark ? Color.parseColor("#FFA0A0A0") : Color.parseColor("#FF707070");
                
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
                
                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                
                TextView revoke = new TextView(activity);
                revoke.setText("撤销管理");
                revoke.setTextSize(15);
                revoke.setTextColor(Color.parseColor("#FF5252"));
                revoke.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                confirm.setTextColor(isDark ? Color.parseColor("#81C784") : Color.parseColor("#2E7D32"));
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable t) {}
        }
    });
}

/**
 * 显示修改群名片弹窗
 * @param activity Activity实例
 * @param qun 群号
 * @param uin 成员QQ号
 * @param nickName 成员昵称
 */
public void showChangeCardDialog(Activity activity, String qun, String uin, String nickName) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
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
                final EditText input = new EditText(activity);
                input.setHint("新的群名片（留空则清除）");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(14);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT);
                inputBg.setStroke(dp(activity, 1), borderColor);
                input.setBackground(inputBg);
                root.addView(input);
                final AlertDialog[] ref = new AlertDialog[1];
                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);
                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                TextView confirm = new TextView(activity);
                confirm.setText("确定");
                confirm.setTextSize(15);
                confirm.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable e) { Toast("弹窗显示失败: " + e.getMessage()); }
        }
    });
}

/**
 * 显示原始消息解析弹窗
 * @param act Activity实例
 * @param data 消息数据对象
 */
void showMsgDataPaeseDialog(Activity act, Object data) {
    if (act == null || act.isFinishing()) return;
    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(act);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
                int borderColor = adjustAlpha(textColor, 0.3f);
                int accentColor = isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT;

                StringBuilder sb = new StringBuilder();
                sb.append("══ MsgData 层 ══\n");
                try { sb.append("消息文本: ").append(data.msg != null ? data.msg : "(空)").append("\n"); } catch(Throwable t){}
                try { sb.append("发送者QQ: ").append(data.userUin).append("\n"); } catch(Throwable t){}
                try { sb.append("聊天对象QQ: ").append(data.peerUin).append("\n"); } catch(Throwable t){}
                try { sb.append("聊天类型: ").append(data.type == 1 ? "私聊(1)" : data.type == 2 ? "群聊(2)" : data.type + "").append("\n"); } catch(Throwable t){}
                try { sb.append("消息类型: ").append(data.msgType).append("\n"); } catch(Throwable t){}
                try { sb.append("消息ID: ").append(data.msgId).append("\n"); } catch(Throwable t){}
                try { sb.append("发送时间: ").append(data.time > 0 ? timestampToDate(data.time * 1000) : "未知").append("\n"); } catch(Throwable t){}

                // Object msgRec = null;
                // try { msgRec = data.data; } catch (Throwable ignored) {}

                // if (msgRec != null) {
                    // sb.append("\n══ MsgRecord 层 ══\n");
                    // sb.append(prettyPrint(msgRec));
                // }

                String parseResult = sb.toString();
                showCopyConfirmDialog(getNowActivity(), "解析原始消息", parseResult, isThemeDark(getNowActivity()));

            } catch (Throwable e) { showCopyConfirmDialog(getNowActivity(), "解析原始消息异常", "获取失败: " + e.getMessage(), isThemeDark(getNowActivity()));
			}
        }
    });
}

/**
 * 显示可复制内容的确认弹窗
 * @param act Activity实例
 * @param title 弹窗标题
 * @param text 要显示的文本内容
 * @param isDark 是否深色主题
 */
void showCopyConfirmDialog(Activity act, String title, String text, boolean isDark) {
    if (act == null) return;
    final String finalText = text != null ? text : "获取失败或内容为空";
    act.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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
                copyAll.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
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

                TextView cancel = new TextView(act);
                cancel.setText("关闭");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(act, 16), dp(act, 12), dp(act, 16), dp(act, 12));
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
                    if (ref[0] != null && ref[0].getWindow() != null) applyUiTheme(act, ref[0]);
                }
            } catch (Throwable e) { Toast("显示弹窗失败: " + e.getMessage()); }
        }
    });
}

/**
 * 显示确认踢出成员弹窗
 * @param activity Activity实例
 * @param qun 群号
 * @param uin 成员QQ号
 * @param nickName 成员昵称
 * @param isDark 是否深色主题
 */
void showKickConfirmDialog(Activity activity, String qun, String uin, String nickName, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    uiHandler.post(new Runnable() {
        public void run() {
            try {
                int textColor = isDark ? Color.parseColor("#FFE0E0E0") : Color.parseColor("#FF202020");
                int subTextColor = isDark ? Color.parseColor("#FFA0A0A0") : Color.parseColor("#FF707070");
                
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
                
                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(15);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                
                TextView permanent = new TextView(activity);
                permanent.setText("永久踢出");
                permanent.setTextSize(15);
                permanent.setTextColor(Color.parseColor("#FF5252"));
                permanent.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                permanent.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try { kickGroup(qun, uin, true); qqToast(2, "永久踢出操作已执行"); } catch (Throwable t){}
                        if (ref[0] != null) ref[0].dismiss();
                    }
                });
                
                TextView confirm = new TextView(activity);
                confirm.setText("确认踢出");
                confirm.setTextSize(15);
                confirm.setTextColor(isDark ? Color.parseColor("#FFB74D") : Color.parseColor("#F57C00"));
                confirm.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try { kickGroup(qun, uin, false); qqToast(2, "踢出操作已执行"); } catch (Throwable t){}
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
                applyUiTheme(activity, ref[0]);
            } catch (Throwable t) {}
        }
    });
}

/**
 * 显示原始消息内容弹窗（格式化输出）
 * @param msgData 消息数据对象
 */
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

/**
 * 显示艾特列表弹窗
 * @param atListData 艾特列表数据
 */
void showAtListDialog(Object atListData) {
    Activity activity = getNowActivity();
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);
    int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
    int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
    
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
                TextView closeBtn = new TextView(activity);
                closeBtn.setText("关闭");
                closeBtn.setTextSize(15);
                closeBtn.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                closeBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);

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
                                            try { nick = String.valueOf(card.strNick); } catch(Throwable t){}
                                        }
                                    } catch (Throwable ignored) {}
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
                                    } catch (Throwable t) {}
                                }
                            });
                        } catch (Throwable e) {
                            activity.runOnUiThread(new Runnable() { public void run() { Toast("获取艾特列表失败"); } });
                        }
                    }
                });
            } catch (Throwable e) { Toast("弹窗显示失败"); }
        }
    });
}

/**
 * 创建现代简约风格的加载动画
 * @param activity Activity实例
 * @param isDark 是否深色主题
 * @return 加载动画布局
 */
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
    loadingText.setTextColor(isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT);
    loadingText.setPadding(0, dp(activity, 12), 0, 0);
    loadingText.setGravity(Gravity.CENTER);
    container.addView(loadingText);
    
    return container;
}

/**
 * 显示群详细信息弹窗
 * @param activity Activity实例
 * @param groupUin 群号
 * @param isDark 是否深色主题
 */
void showGroupInfoDialog(Activity activity, String groupUin, boolean isDark) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                Object troopInfo = findTroopInfo(groupUin);
                StringBuilder sb = new StringBuilder();
                sb.append("群号: ").append(groupUin).append("\n\n");
                if (troopInfo != null) {
                    try { sb.append("群名称: ").append(String.valueOf(troopInfo.troopname)).append("\n"); } catch (Throwable ignored) { sb.append("群名称: 获取失败\n"); }
                    try { sb.append("群主QQ: ").append(String.valueOf(troopInfo.troopowneruin)).append("\n"); } catch (Throwable ignored) {}
                    try { sb.append("群人数: ").append(String.valueOf(troopInfo.wMemberNum)).append(" / ").append(String.valueOf(troopInfo.wMemberMax)).append("\n"); } catch (Throwable ignored) {}
                    try { 
                        long createTime = Long.parseLong(String.valueOf(troopInfo.troopCreateTime));
                        if (createTime > 0) sb.append("创建时间: ").append(timestampToDate(createTime * 1000)).append("\n");
                    } catch (Throwable ignored) {}
                    try { 
                        String q = String.valueOf(troopInfo.joinTroopQuestion);
                        if (q != null && !q.isEmpty() && !q.equals("null")) sb.append("进群问题: ").append(q).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        String a = String.valueOf(troopInfo.joinTroopAnswer);
                        if (a != null && !a.isEmpty() && !a.equals("null")) sb.append("进群答案: ").append(a).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        String memo = String.valueOf(troopInfo.troopmemo);
                        if (memo != null && !memo.isEmpty() && !memo.equals("null")) sb.append("\n群介绍:\n").append(memo).append("\n");
                    } catch (Throwable ignored) {}
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

/**
 * 显示成员详细信息弹窗
 * @param activity Activity实例
 * @param peerUin 聊天对象QQ号
 * @param userUin 成员QQ号
 * @param chatType 聊天类型
 */
void showMemberInfoDialog(Activity activity, String peerUin, String userUin, int chatType) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                StringBuilder sb = new StringBuilder();
                
                Object card = null;
                try { card = GetCard(userUin); } catch (Throwable ignored) {}
                
                Object member = null;
                if (chatType == 2) {
                    try { member = getMemberInfo(peerUin, userUin); } catch (Throwable ignored) {}
                }
                
                Object troopInfo = null;
                if (chatType == 2) {
                    try { troopInfo = findTroopInfo(peerUin); } catch (Throwable ignored) {}
                }
                
                Object friend = null;
                try { friend = GetCard(userUin); } catch (Throwable ignored) {}

                sb.append("QQ: ").append(userUin).append("\n");
                
                String nick = "未知";
                if (card != null) {
                    try {
                        String cn = String.valueOf(card.strNick);
                        if(cn != null && !cn.contains("*") && !cn.equals("null")) nick = cn;
                    } catch(Throwable t){}
                }
                if ("未知".equals(nick) && member != null) {
                    try {
                        String mn = String.valueOf(member.uinName);
                        if (mn != null && !mn.isEmpty() && !mn.contains("*") && !mn.equals("null")) nick = mn;
                    } catch (Throwable ignored) {}
                }
                sb.append("昵称: ").append(nick).append("\n");
                
                if (friend != null) {
                    try {
                        String rm = String.valueOf(friend.remark);
                        if (rm != null && !rm.isEmpty() && !rm.equals("null")) sb.append("备注: ").append(rm).append("\n");
                    } catch (Throwable ignored) {}
                }
                
                if (card != null) {
                    try {
                        String qid = String.valueOf(card.qid);
                        if (qid != null && !qid.isEmpty() && !qid.equals("null")) sb.append("QID: ").append(qid).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        int age = (int) card.age;
                        if (age > 0) sb.append("年龄: ").append(age).append("\n");
                    } catch (Throwable ignored) {}
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
                    } catch (Throwable ignored) {}
                    try {
                        String school = String.valueOf(card.strSchool);
                        if (school != null && !school.isEmpty() && !school.equals("null")) sb.append("学校: ").append(school).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        String company = String.valueOf(card.strCompany);
                        if (company != null && !company.isEmpty() && !company.equals("null")) sb.append("公司: ").append(company).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        String email = String.valueOf(card.strEmail);
                        if (email != null && !email.isEmpty() && !email.equals("null")) sb.append("邮箱: ").append(email).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        String sign = String.valueOf(card.strSign);
                        if (sign != null && !sign.isEmpty() && !sign.equals("null")) sb.append("签名: ").append(sign).append("\n");
                    } catch (Throwable ignored) {}
                }
                
                int qqLevel = 0;
                if (card != null) {
                    try { qqLevel = (int) card.iQQLevel; } catch (Throwable t){}
                }
                if (qqLevel == 0 && member != null) {
                    try { qqLevel = (int) member.uinLevel; } catch (Throwable ignored) {}
                }
                sb.append("QQ等级: Lv.").append(qqLevel).append("\n");
                
                if (card != null) {
                    try {
                        int iQQVipLevel = (int) card.iQQVipLevel;
                        if (iQQVipLevel > 0) sb.append("QQ会员: Lv.").append(iQQVipLevel).append("\n");
                    } catch (Throwable t){}
                    try {
                        int iSuperVipLevel = (int) card.iSuperVipLevel;
                        if (iSuperVipLevel > 0) sb.append("超级会员: Lv.").append(iSuperVipLevel).append("\n");
                    } catch (Throwable t){}
                    try {
                        long lVoteCount = (long) card.lVoteCount;
                        sb.append("名片赞: ").append(lVoteCount).append("\n");
                    } catch (Throwable t){}
                    try {
                        long lLoginDays = (long) card.lLoginDays;
                        sb.append("登录天数: ").append(lLoginDays).append("\n");
                    } catch (Throwable t){}
                    try {
                        boolean isForbid = (boolean) card.isForbidAccount;
                        if (isForbid) sb.append("账号状态: 已封禁\n");
                    } catch (Throwable ignored) {}
                }
                
                if (chatType == 2 && member != null) {
                    try { sb.append("群名片: ").append(String.valueOf(member.uinName)).append("\n"); } catch (Throwable ignored) {}
                    try { sb.append("群等级: Lv.").append(String.valueOf(member.uinLevel)).append("\n"); } catch (Throwable ignored) {}
                    try { sb.append("角色: ").append(convertRole(String.valueOf(member.role))).append("\n"); } catch (Throwable ignored) {}
                    try { 
                        long joinTime = Long.parseLong(String.valueOf(member.joinGroupTime));
                        if (joinTime > 0) sb.append("入群时间: ").append(timestampToDate(joinTime * 1000)).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        long lastTime = Long.parseLong(String.valueOf(member.lastActiveTime));
                        if (lastTime > 0) sb.append("最后活跃: ").append(timestampToDate(lastTime * 1000)).append("\n");
                    } catch (Throwable ignored) {}
                    try {
                        long gagTime = Long.parseLong(String.valueOf(member.gagTimeStamp));
                        sb.append("禁言状态: ").append(getGagStatus(String.valueOf(gagTime))).append("\n");
                    } catch (Throwable ignored) {}
                }
                
                if (chatType == 2 && troopInfo != null) {
                    try { sb.append("群号: ").append(peerUin).append("\n"); } catch (Throwable ignored) {}
                    try { sb.append("群名: ").append(String.valueOf(troopInfo.troopname)).append("\n"); } catch (Throwable ignored) {}
                    try { sb.append("群主: ").append(String.valueOf(troopInfo.troopowneruin)).append("\n"); } catch (Throwable ignored) {}
                    try { sb.append("最大人数: ").append(String.valueOf(troopInfo.wMemberMax)).append("\n"); } catch (Throwable ignored) {}
                    try { sb.append("当前人数: ").append(String.valueOf(troopInfo.wMemberNum)).append("\n"); } catch (Throwable ignored) {}
                    try {
                        long createTime = Long.parseLong(String.valueOf(troopInfo.troopCreateTime));
                        if (createTime > 0) sb.append("创建时间: ").append(timestampToDate(createTime * 1000)).append("\n");
                    } catch (Throwable ignored) {}
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

/**
 * 克隆指定 QQ 头像（下载后上传）
 * @param uin QQ号
 */
void handleCloneAvatar(final String uin) {
    String url = "http://q2.qlogo.cn/headimg_dl?dst_uin=" + uin + "&spec=640";
    String fileName = "avatar_" + getTime() + ".png";
    executeDownloadAndUpload(url, fileName, "开始克隆头像", "克隆头像成功");
}

/**
 * 上传群消息中的图片作为头像（从 [pic=xxx] 提取链接）
 * @param quntext 包含图片标记的文本
 */
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

/**
 * 通用下载并上传头像方法
 * @param url      下载地址
 * @param fileName 保存的文件名（不含路径）
 * @param startMsg 开始下载时的提示
 * @param succMsg  上传成功后的提示
 */
private void executeDownloadAndUpload(final String url, final String fileName,
                                      final String startMsg, final String succMsg) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            final String savePath = pluginPath + "/cache/" + fileName;
            traceLog("api_log.txt", "开始下载: " + url);
            uiHandler.post(new Runnable() {
                public void run() {
                    Toast(startMsg);
                }
            });

            boolean downloadOk = downloadFile(url, savePath, new ProgressCallback() {
                public void onProgress(int progress) {
                    traceLog("api_log.txt", "下载进度: " + progress + "%");
                }
                public void onProgressTip(String tip) {
                    traceLog("api_log.txt", tip);
                }
            });

            if (!downloadOk) {
                traceLog("api_log.txt", "下载失败: " + url);
                uiHandler.post(new Runnable() {
                    public void run() {
                        Toast("下载失败");
                    }
                });
                return;
            }

            traceLog("api_log.txt", "下载完成，准备上传: " + savePath);
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
                            traceLog("api_log.txt", succMsg + "，准备延迟删除");
                            uiHandler.postDelayed(new Runnable() {
                                public void run() {
                                    删除(savePath);
                                    traceLog("api_log.txt", "文件已删除");
                                }
                            }, 1000);
                        } else {
                            Toast("上传失败");
                            traceLog("api_log.txt", "上传失败");
                            删除(savePath);
                        }
                    } catch (Throwable e) {
                        traceLog("api_log.txt", "上传异常: " + e.getMessage());
                        Toast("上传异常: " + e.getMessage());
                        删除(savePath);
                    }
                }
            });
        }
    });
}

/**
 * 显示获取Cookie弹窗
 * @param activity Activity实例
 * @param isDark 是否深色主题
 */
public void showGetCookieDialog(final Activity activity, final boolean isDark) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int accentColor = isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT;
                int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
                int borderColor = adjustAlpha(textColor, 0.3f);

                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(inputBgColor);
                inputBg.setStroke(dp(activity, 1), borderColor);

                ScrollView scrollView = new ScrollView(activity);
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("QQ Cookie & 请求工具");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 8));
                root.addView(titleView);

                final boolean[] isCkMode = new boolean[]{true};

                // CK 模式布局
                LinearLayout ckModeLayout = new LinearLayout(activity);
                ckModeLayout.setOrientation(LinearLayout.HORIZONTAL);
                ckModeLayout.setGravity(Gravity.CENTER_VERTICAL);
                ckModeLayout.setPadding(0, dp(activity, 8), 0, 0);

                final EditText domainEt = new EditText(activity);
                domainEt.setHint("域名 (如 qzone.qq.com)");
                domainEt.setText("qzone.qq.com");
                domainEt.setHintTextColor(subTextColor);
                domainEt.setTextColor(textColor);
                domainEt.setTextSize(14);
                domainEt.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                domainEt.setBackground(inputBg);
                domainEt.setMaxLines(1);
                domainEt.setSingleLine(true);
                domainEt.setMinHeight(dp(activity, 48));

                TextView getCkBtn = new TextView(activity);
                getCkBtn.setText("获取CK");
                getCkBtn.setTextSize(15);
                getCkBtn.setTextColor(accentColor);
                getCkBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                getCkBtn.setGravity(Gravity.CENTER);

                TextView postSwitchBtn = new TextView(activity);
                postSwitchBtn.setText("POST");
                postSwitchBtn.setTextSize(15);
                postSwitchBtn.setTextColor(accentColor);
                postSwitchBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                postSwitchBtn.setGravity(Gravity.CENTER);

                ckModeLayout.addView(domainEt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                ckModeLayout.addView(getCkBtn);
                ckModeLayout.addView(postSwitchBtn);
                root.addView(ckModeLayout);

                // POST 模式布局
                LinearLayout postModeLayout = new LinearLayout(activity);
                postModeLayout.setOrientation(LinearLayout.VERTICAL);
                postModeLayout.setVisibility(View.GONE);
                postModeLayout.setPadding(0, dp(activity, 8), 0, 0);

                LinearLayout postRow1 = new LinearLayout(activity);
                postRow1.setOrientation(LinearLayout.HORIZONTAL);
                postRow1.setGravity(Gravity.CENTER_VERTICAL);

                final EditText postUrlEt = new EditText(activity);
                postUrlEt.setHint("POST URL");
                postUrlEt.setHintTextColor(subTextColor);
                postUrlEt.setTextColor(textColor);
                postUrlEt.setTextSize(14);
                postUrlEt.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                postUrlEt.setBackground(inputBg);
                postUrlEt.setMaxLines(1);
                postUrlEt.setSingleLine(true);
                postUrlEt.setMinHeight(dp(activity, 48));

                TextView ckSwitchBtn = new TextView(activity);
                ckSwitchBtn.setText("CK");
                ckSwitchBtn.setTextSize(15);
                ckSwitchBtn.setTextColor(accentColor);
                ckSwitchBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                ckSwitchBtn.setGravity(Gravity.CENTER);

                TextView sendPostBtn = new TextView(activity);
                sendPostBtn.setText("发送");
                sendPostBtn.setTextSize(15);
                sendPostBtn.setTextColor(accentColor);
                sendPostBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                sendPostBtn.setGravity(Gravity.CENTER);

                postRow1.addView(postUrlEt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                postRow1.addView(ckSwitchBtn);
                postRow1.addView(sendPostBtn);
                postModeLayout.addView(postRow1);

                final EditText postDataEt = new EditText(activity);
                postDataEt.setHint("POST 数据 (可为空)");
                postDataEt.setHintTextColor(subTextColor);
                postDataEt.setTextColor(textColor);
                postDataEt.setTextSize(14);
                postDataEt.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
                postDataEt.setBackground(inputBg);
                postDataEt.setMinLines(3);
                postDataEt.setMaxLines(6);
                postDataEt.setMinHeight(dp(activity, 100));

                LinearLayout.LayoutParams dataParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                dataParams.topMargin = dp(activity, 12);
                postModeLayout.addView(postDataEt, dataParams);

                root.addView(postModeLayout);

                // 结果区域
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

                final TextView copyBtn = new TextView(activity);
                copyBtn.setText("复制");
                copyBtn.setTextSize(14);
                copyBtn.setTextColor(accentColor);
                copyBtn.setPadding(dp(activity, 16), 0, 0, 0);

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

                // CK 格式工具条
                final LinearLayout ckFormatBar = new LinearLayout(activity);
                ckFormatBar.setOrientation(LinearLayout.HORIZONTAL);
                ckFormatBar.setPadding(0, dp(activity, 12), 0, 0);

                // POST 工具条
                final LinearLayout commonToolBar = new LinearLayout(activity);
                commonToolBar.setOrientation(LinearLayout.HORIZONTAL);
                commonToolBar.setPadding(0, dp(activity, 12), 0, 0);
                commonToolBar.setVisibility(View.GONE);

                // 存储最后一次获取的 CK 信息
                class CKInfo {
                    String skey;
                    String pskey;
                    String bkn;
                    String domain;
                    String fullCookie;
                }
                final CKInfo[] ckInfo = new CKInfo[1];

                // CK 格式按钮（始终可点击，若无数据则提示）
                String[] ckFormats = {"JSON", "Header", "Curl", "Fetch"};
                final TextView[] ckBtns = new TextView[4];
                for (int i = 0; i < 4; i++) {
                    final int idx = i;
                    TextView btn = new TextView(activity);
                    btn.setText(ckFormats[i]);
                    btn.setTextSize(13);
                    btn.setTextColor(subTextColor);
                    btn.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
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
                            tv.setTextColor(isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT);
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
                    ckBtns[i] = btn;
                    ckFormatBar.addView(btn);
                }

                // POST 工具条：复制 URL / 复制数据
                String[] postTools = {"复制URL", "复制数据"};
                for (int i = 0; i < postTools.length; i++) {
                    final String name = postTools[i];
                    TextView btn = new TextView(activity);
                    btn.setText(name);
                    btn.setTextSize(13);
                    btn.setTextColor(subTextColor);
                    btn.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
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

                // 获取CK按钮
                getCkBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (isCkMode[0]) {
                            String domain = domainEt.getText().toString().trim();
                            if (domain.isEmpty()) {
                                Toast("请输入域名");
                                return;
                            }
                            try {
                                final String qq = "3069670151";
                                final String uin = "o" + qq;
                                final String pUin = "o" + qq;

                                String skey = getSkey();
                                String pskey = getPskey(domain);
                                String bkn = getBkn(skey);

                                final String fullCookie = "p_skey=" + pskey + "; skey=" + skey + "; uin=" + uin + "; p_uin=" + pUin;

                                // 保存 CK 信息供格式按钮使用
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
                                contentTv.setTextColor(isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT);
                                contentTv.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
                                contentTv.setTextIsSelectable(true);
                                resultBody.addView(contentTv);

                                resultScroll.setVisibility(View.VISIBLE);
                                arrowD.setDirection(true);
                                arrowIv.setImageDrawable(arrowD);

                            } catch (Throwable e) {
                                Toast("获取失败: " + e.getMessage());
                            }
                        } else {
                            ckModeLayout.setVisibility(View.VISIBLE);
                            postModeLayout.setVisibility(View.GONE);
                            ckFormatBar.setVisibility(View.VISIBLE);
                            commonToolBar.setVisibility(View.GONE);
                            isCkMode[0] = true;
                        }
                    }
                });

                // POST切换/发送按钮
                postSwitchBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (isCkMode[0]) {
                            ckModeLayout.setVisibility(View.GONE);
                            postModeLayout.setVisibility(View.VISIBLE);
                            ckFormatBar.setVisibility(View.GONE);
                            commonToolBar.setVisibility(View.VISIBLE);
                            isCkMode[0] = false;
                        } else {
                            String url = postUrlEt.getText().toString().trim();
                            String data = postDataEt.getText().toString().trim();
                            if (url.isEmpty()) {
                                Toast("请输入POST URL");
                                return;
                            }
                            if (!url.startsWith("http")) url = "https://" + url;

                            try {
                                String cookie = "p_skey=" + getPskey(domainEt.getText().toString().trim()) +
                                        "; skey=" + getSkey() +
                                        "; uin=o3069670151; p_uin=o3069670151";

                                String result = httpPost(url, cookie, data);

                                String display = "POST完整返回：\n\n" + (result.isEmpty() ? "(无返回内容)" : result);
                                currentResultText[0] = display;

                                resultBody.removeAllViews();
                                TextView tv = new TextView(activity);
                                tv.setText(display);
                                tv.setTextSize(13);
                                tv.setTextColor(isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT);
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

                // CK 模式切换按钮（POST布局中的“CK”按钮）
                ckSwitchBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ckModeLayout.setVisibility(View.VISIBLE);
                        postModeLayout.setVisibility(View.GONE);
                        ckFormatBar.setVisibility(View.VISIBLE);
                        commonToolBar.setVisibility(View.GONE);
                        isCkMode[0] = true;
                    }
                });

                // 请求按钮（POST布局中的“发送”）
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
                                String cookie = "p_skey=" + getPskey("https://" + domainEt.getText().toString().trim()) +
                                        "; skey=" + getSkey() +
                                        "; uin=o3069670151; p_uin=o3069670151";

                                String result = httpPost(url, cookie, data);

                                String display = "POST完整返回：\n\n" + (result.isEmpty() ? "(无返回内容)" : result);
                                currentResultText[0] = display;

                                resultBody.removeAllViews();
                                TextView tv = new TextView(activity);
                                tv.setText(display);
                                tv.setTextSize(13);
                                tv.setTextColor(isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT);
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
                applyUiTheme(activity, dialog);

            } catch (Throwable e) {
                Toast("弹窗创建失败: " + e.getMessage());
            }
        }
    });
}

class AudioBtnAdder {
    void add(Activity activity, LinearLayout btnGrid, String name, int textColor, int inputBgColor, int borderColor, Runnable action) {
        TextView btn = new TextView(activity);
        btn.setText(name);
        btn.setTextColor(textColor);
        btn.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        btn.setGravity(Gravity.CENTER);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(dp(activity, 8));
        btnBg.setColor(inputBgColor);
        btnBg.setStroke(dp(activity, 1), borderColor);
        btn.setBackground(btnBg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
        btn.setLayoutParams(p);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try { action.run(); } catch(Throwable t) {}
            }
        });
        btnGrid.addView(btn);
    }

    // 新增：创建带进度条的播放控制器
    View createAudioPlayer(Activity activity, final MediaPlayer[] player, int textColor, int inputBgColor, int borderColor,
                           final Handler uiHandler, final Runnable[] updateProgressTaskRef) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(activity, 8), 0, dp(activity, 8));

        // 进度条
        final SeekBar seekBar = new SeekBar(activity);
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

        TextView playBtn = new TextView(activity);
        playBtn.setText("播放");
        playBtn.setTextColor(textColor);
        playBtn.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        playBtn.setGravity(Gravity.CENTER);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(dp(activity, 8));
        btnBg.setColor(inputBgColor);
        btnBg.setStroke(dp(activity, 1), borderColor);
        playBtn.setBackground(btnBg);
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
        return String.format("%02d:%02d", min, sec);
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
                            int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                            int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                            int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
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
                                adder.add(activity, actionRow, "发链接", textColor, inputBgColor, borderColor, new Runnable() {
                                    public void run() {
                                        try {
                                            Object contact = data.contact;
                                            sendMsg(contact, finalUrl);
                                            Toast("链接已发送");
                                        } catch(Throwable t) { Toast("发送失败"); }
                                    }
                                });
                                adder.add(activity, actionRow, "发语音", textColor, inputBgColor, borderColor, new Runnable() {
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
                            }
                            btnGrid.addView(actionRow);
                            root.addView(btnGrid);

                            final AlertDialog[] ref = new AlertDialog[1];
                            TextView closeBtn = new TextView(activity);
                            closeBtn.setText("关闭");
                            closeBtn.setTextSize(15);
                            closeBtn.setTextColor(subTextColor);
                            closeBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                            closeBtn.setGravity(Gravity.RIGHT);
                            closeBtn.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    // 停止播放并释放
                                    if (player[0] != null) {
                                        try { player[0].stop(); player[0].release(); } catch(Throwable t){}
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
                                        try { player[0].stop(); player[0].release(); } catch(Throwable t){}
                                        player[0] = null;
                                    }
                                    if (updateProgressTask[0] != null) {
                                        uiHandler.removeCallbacks(updateProgressTask[0]);
                                    }
                                }
                            });
                            ref[0].show();
                            applyUiTheme(activity, ref[0]);
                        } catch(Throwable t) {}
                    }
                });
            } catch(Throwable e) {
                uiHandler.post(new Runnable() { public void run() { Toast("提取音频异常"); } });
            }
        }
    });
}

/**
 * 创建通用信息行视图
 * @param activity Activity实例
 * @param label 标签文本
 * @param value 值文本
 * @param colorText 文字颜色
 * @param colorSubtext 次要文字颜色
 * @param isDark 是否深色主题
 * @return 信息行布局
 */
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

/**
 * 创建群成员信息看板卡片
 * @param activity Activity实例
 * @param userUin 成员QQ号
 * @param peerUin 聊天对象QQ号
 * @param chatType 聊天类型
 * @param isDark 是否深色主题
 * @param colorText 文字颜色
 * @param colorSubtext 次要文字颜色
 * @param msgText 消息文本
 * @param fullMsgText 完整消息文本
 * @param msgRecord 消息记录对象
 * @param nickName 昵称
 * @return 信息卡片布局
 */
FrameLayout createMemberInfoCard(Activity activity, String userUin, String peerUin, int chatType,
                                 boolean isDark, int colorText, int colorSubtext,
                                 String msgText, String fullMsgText, Object msgRecord, String nickName) {
    FrameLayout cardWrapper = new FrameLayout(activity);
    cardWrapper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    cardWrapper.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 8));

    GradientDrawable cardBg = new GradientDrawable();
    cardBg.setShape(GradientDrawable.RECTANGLE);
    cardBg.setCornerRadius(dp(activity, 16));
    cardBg.setColor(isDark ? Color.parseColor("#FF252525") : Color.parseColor("#FFF0F0F0"));
    cardBg.setStroke(dp(activity, 1), isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#18000000"));

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
    divider.setBackgroundColor(isDark ? Color.parseColor("#22FFFFFF") : Color.parseColor("#15000000"));
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
                        } catch(Throwable t){}
                    }
                } catch (Throwable ignored) {}
                if (nick.equals(userUin) && msgRecord != null) {
                    try {
                        String fb = String.valueOf(msgRecord.sendNickName);
                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) nick = fb;
                    } catch (Throwable ignored) {}
                }
                final String finalNick = nick;
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try { nameView.setText(finalNick); } catch(Throwable t){}
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
                                } catch (Throwable ignored) {}
                                if ("未设置".equals(groupNick) && msgRecord != null) {
                                    try {
                                        String fb = String.valueOf(msgRecord.sendMemberName);
                                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) groupNick = fb;
                                    } catch (Throwable ignored) {}
                                }
                                final String finalGroupNick = groupNick;

                                String gName = "获取失败";
                                try {
                                    Object info = findTroopInfo(peerUin);
                                    if (info != null) {
                                        String tn = String.valueOf(info.troopname);
                                        if (tn != null && !tn.equals("null")) gName = tn;
                                    }
                                } catch (Throwable ignored) {}
                                final String finalGroupName = gName;

                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            ((TextView) groupRow.getChildAt(1)).setText(finalGroupNick);
                                        } catch (Throwable ignored) {}
                                        if (finalGroupNameRow != null) {
                                            try {
                                                ((TextView) finalGroupNameRow.getChildAt(1)).setText(finalGroupName);
                                            } catch (Throwable ignored) {}
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
                                } catch (Throwable ignored) {}
                                if ("未设置".equals(remark) && msgRecord != null) {
                                    try {
                                        String fb = String.valueOf(msgRecord.sendRemarkName);
                                        if (fb != null && !fb.isEmpty() && !fb.equals("null")) remark = fb;
                                    } catch (Throwable ignored) {}
                                }
                                final String finalRemark = remark;

                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        try {
                                            ((TextView) groupRow.getChildAt(1)).setText(finalRemark);
                                        } catch (Throwable ignored) {}
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

/**
 * 创建菜单项按钮
 * @param activity Activity实例
 * @param title 按钮标题
 * @param isDark 是否深色主题
 * @param colorPrimary 主色调
 * @param colorCardBg 卡片背景色
 * @param parentDialog 父弹窗
 * @param callback 点击回调
 * @param skipDismiss 是否跳过关闭弹窗
 * @return 菜单项布局
 */
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

/**
 * 创建分类标题视图
 * @param activity Activity实例
 * @param category 分类名称
 * @param colorPrimary 主色调
 * @return 标题视图
 */
TextView createCategoryTitle(Activity activity, String category, int colorPrimary) {
    TextView tv = new TextView(activity);
    tv.setText(category);
    tv.setTextSize(14);
    tv.setTextColor(colorPrimary);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setPadding(dp(activity, 8), dp(activity, 12), dp(activity, 8), dp(activity, 8));
    return tv;
}

String[][] CATEGORY_COLOR_STRS = new String[][]{
    new String[]{"#2196F3","#1976D2","#BBDEFB"},
    new String[]{"#4CAF50","#388E3C","#C8E6C9"},
    new String[]{"#9C27B0","#7B1FA2","#E1BEE7"},
    new String[]{"#FF9800","#F57C00","#FFE0B2"},
    new String[]{"#00BCD4","#0097A7","#B2EBF2"},
    new String[]{"#607D8B","#455A64","#CFD8DC"}
};

/**
 * 添加菜单项到列表
 * @param list 菜单项列表
 * @param category 分类名称
 * @param title 菜单项标题
 * @param callback 点击回调
 */
void addMenuItem(List list, String category, String title, Runnable callback) {
    list.add(new Object[]{category, title, callback});
}

/**
 * 调整颜色透明度
 * @param color 原始颜色
 * @param factor 透明度因子（0-1）
 * @return 调整后的颜色
 */
int adjustAlpha(int color, float factor) {
    return Color.argb(Math.round(Color.alpha(color) * factor), Color.red(color), Color.green(color), Color.blue(color));
}

/**
 * 根据分类名称获取对应颜色数组
 * @param category 分类名称
 * @param isDark 是否深色主题
 * @return 颜色数组[主色, 次色]
 */
int[] getCategoryColorInts(String category, boolean isDark) {
    int index = 5;
    if ("消息操作".equals(category)) index = 0;
    else if ("群管理".equals(category) || "好友信息".equals(category)) index = 1;
    else if ("互动功能".equals(category)) index = 2;
    else if ("工具".equals(category)) index = 3;
    else if ("其他".equals(category)) index = 4;
    else if ("设置".equals(category)) index = 5;
    String[] colorStrs = CATEGORY_COLOR_STRS[index];
    return new int[]{Color.parseColor(isDark ? colorStrs[1] : colorStrs[0]), Color.parseColor(isDark ? colorStrs[2] : colorStrs[1])};
}

class ArrowDrawable extends Drawable {
    private Paint paint;
    private Path path;
    private boolean isUp;

    /**
     * 构造箭头Drawable
     * @param color 箭头颜色
     * @param isUp 是否向上
     */
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

    /**
     * 设置箭头颜色
     * @param color 颜色值
     */
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

/**
 * 应用保存的排序配置到菜单数据
 * @param catOrder 分类顺序列表
 * @param catItems 分类菜单项映射
 * @param savedOrder 保存的排序字符串
 */
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
        traceLog("api_log.txt", "[applySavedSortOrder]排序应用失败: " + t.getMessage());
    }
}

/**
 * 构建排序配置字符串
 * @param catOrder 分类顺序列表
 * @param catItems 分类菜单项映射
 * @return 排序字符串
 */
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

/**
 * 长按消息显示的主菜单入口
 * @param activity Activity实例
 * @param data 消息数据对象
 */
public void 长按消息菜单(Activity activity, Object data) {
    if (activity == null || activity.isFinishing()) return;

    boolean isDark = isThemeDark(activity);
    String quntext = "";
    try { quntext = String.valueOf(data.msg); } catch(Throwable t){}
    String peerUin = "";
    try { peerUin = String.valueOf(data.peerUin); } catch(Throwable t){}
    String userUin = "";
    try { userUin = String.valueOf(data.userUin); } catch(Throwable t){}
    int msgtype = 0;
    try { msgtype = (int) data.msgType; } catch(Throwable t){}
    long msgid = 0;
    try { msgid = (long) data.msgId; } catch(Throwable t){}
    int chatType = 0;
    try { chatType = (int) data.type; } catch(Throwable t){}
    
    Object msgRecord = null;
    try { msgRecord = data.data; } catch(Throwable t){}
    Object atList = null;
    try { atList = data.atList; } catch(Throwable t){}
    
    String nickName = "";
    try { nickName = String.valueOf(msgRecord.sendNickName); } catch(Throwable t){}
    if (nickName == null || nickName.isEmpty() || nickName.equals("null")) {
        nickName = userUin;
    }

    int colorText = isDark ? Color.parseColor("#FFE0E0E0") : Color.parseColor("#FF202020");
    int colorSubtext = isDark ? Color.parseColor("#FFA0A0A0") : Color.parseColor("#FF707070");
    int colorCardBg = isDark ? Color.parseColor("#FF2D2D2D") : Color.parseColor("#FFF5F5F5");

    final Dialog bottomSheet = new Dialog(activity);
    bottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE);

    LinearLayout rootLayout = new LinearLayout(activity);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setBackgroundColor(isDark ? Color.parseColor("#FF1E1E1E") : Color.parseColor("#FFFFFFFF"));

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

    addMenuItem(menuItems, "消息操作", "复制内容", new Runnable() { public void run() { showCopyConfirmDialog(activity, "消息内容", finalQuntext, isDark); } });
    addMenuItem(menuItems, "消息操作", "复读加一", new Runnable() { public void run() { 复读(data); } });
    addMenuItem(menuItems, "消息操作", "撤回消息", new Runnable() { public void run() { recallMsg(finalChatType, finalPeerUin, finalMsgid); qqToast(2, "撤回操作已执行"); } });
    addMenuItem(menuItems, "消息操作", "原始消息", new Runnable() { public void run() { showRawMessageDialog(finalMsgRecord); } });
    addMenuItem(menuItems, "消息操作", "解析消息", new Runnable() { public void run() { showMsgDataPaeseDialog(activity, data); } });
    addMenuItem(menuItems, "消息操作", "提取音频", new Runnable() { public void run() { showExtractAudioDialog(activity, data); } });

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
    addMenuItem(menuItems, "互动功能", "回应表情", new Runnable() { public void run() { 
    // showEmojiPickerDialog(activity, finalMsgRecord); 
    Toast("敬请期待");
    } });
    if (chatType == 2) {
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

    addMenuItem(menuItems, "其他", "克隆头像", new Runnable() { public void run() { handleCloneAvatar(finalUserUin); } });
    addMenuItem(menuItems, "其他", "上传头像", new Runnable() { public void run() { handleUploadAvatar(finalQuntext); } });
    if (chatType == 2) {
        addMenuItem(menuItems, "其他", "群打卡", new Runnable() { public void run() { boolean ok = CheckSign(finalPeerUin, myUin); qqToast(ok ? 2 : 1, ok ? "打卡成功" : "打卡失败"); } });
        addMenuItem(menuItems, "其他", "群字符", new Runnable() { public void run() { triggerLuckyCharacter(finalPeerUin); } });
    }

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
            } catch(Throwable t) {}
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
                
                TextView cancelBtn = new TextView(activity);
                cancelBtn.setText("取消");
                cancelBtn.setTextSize(14);
                cancelBtn.setTextColor(Color.WHITE);
                cancelBtn.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 16), dp(activity, 10));
                GradientDrawable cancelBg = new GradientDrawable();
                cancelBg.setColor(isDark ? Color.parseColor("#FF5252") : Color.parseColor("#F44336"));
                cancelBg.setCornerRadius(dp(activity, 20));
                cancelBtn.setBackground(cancelBg);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isEditMode[0] = false;
                        String saved = getString("setting", "menuSort", "");
                        applySavedSortOrder(catOrder, catItems, saved);
                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                    }
                });
                
                TextView saveBtn = new TextView(activity);
                saveBtn.setText("保存");
                saveBtn.setTextSize(14);
                saveBtn.setTextColor(Color.WHITE);
                saveBtn.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 16), dp(activity, 10));
                GradientDrawable saveBg = new GradientDrawable();
                saveBg.setColor(isDark ? Color.parseColor("#81C784") : Color.parseColor("#4CAF50"));
                saveBg.setCornerRadius(dp(activity, 20));
                saveBtn.setBackground(saveBg);
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
        bgDrawable.setColor(isDark ? Color.parseColor("#FF1E1E1E") : Color.parseColor("#FFFFFFFF"));
        float cr = dp(activity, 20);
        bgDrawable.setCornerRadii(new float[]{cr, cr, cr, cr, 0, 0, 0, 0});
        rootLayout.setBackground(bgDrawable);
        rootLayout.setTranslationY(dp(activity, 120));
        rootLayout.setAlpha(0f);
        rootLayout.animate().translationY(0f).alpha(1f).setDuration(250).setInterpolator(new DecelerateInterpolator()).start();
    }
}

/**
 * 显示禁言列表弹窗（流式加载）
 * @param activity Activity实例
 * @param groupUin 群号
 * @param isDark 是否深色主题
 */
void showProhibitListDialog(Activity activity, String groupUin, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
    int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
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
                TextView closeBtn = new TextView(activity);
                closeBtn.setText("关闭");
                closeBtn.setTextSize(15);
                closeBtn.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                closeBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object prohibitList = getProhibitList(groupUin);
                            final List allItems = new ArrayList();
                            
                            if (prohibitList != null && ((List) prohibitList).size() > 0) {
                                for (int i = 0; i < ((List) prohibitList).size(); i++) {
                                    Object f = ((List) prohibitList).get(i);
                                    String uin = "";
                                    try { uin = String.valueOf(f.user); } catch(Throwable t){}
                                    String name = "";
                                    try { name = String.valueOf(f.userName); } catch(Throwable t){}
                                    String status = "未知";
                                    try { status = getGagStatus(String.valueOf(f.endTime)); } catch(Throwable t){}
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

/**
 * 显示群成员列表弹窗（流式加载）
 * @param activity Activity实例
 * @param groupUin 群号
 * @param isDark 是否深色主题
 */
void showGroupMemberListDialog(Activity activity, String groupUin, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
    int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
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
                TextView closeBtn = new TextView(activity);
                closeBtn.setText("关闭");
                closeBtn.setTextSize(15);
                closeBtn.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                closeBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object members = getGroupMemberList(groupUin);
                            final List allItems = new ArrayList();
                            
                            if (members != null && ((List) members).size() > 0) {
                                for (int i = 0; i < ((List) members).size(); i++) {
                                    Object m = ((List) members).get(i);
                                    String uin = "";
                                    try { uin = String.valueOf(m.uin); } catch(Throwable t){}
                                    String name = "";
                                    try { name = String.valueOf(m.uinName); } catch(Throwable t){}
                                    String role = "未知";
                                    try { role = convertRole(String.valueOf(m.role)); } catch(Throwable t){}
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

/**
 * 显示我的群列表弹窗（流式加载）
 * @param activity Activity实例
 * @param isDark 是否深色主题
 */
void showGroupListDialog(Activity activity, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
    int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
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
                TextView closeBtn = new TextView(activity);
                closeBtn.setText("关闭");
                closeBtn.setTextSize(15);
                closeBtn.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                closeBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object groups = getGroupList();
                            final List allItems = new ArrayList();
                            
                            if (groups != null && ((List) groups).size() > 0) {
                                for (int i = 0; i < ((List) groups).size(); i++) {
                                    Object g = ((List) groups).get(i);
                                    String groupUin = "";
                                    try { groupUin = String.valueOf(g.group); } catch(Throwable t){}
                                    String groupName = "";
                                    try { groupName = String.valueOf(g.groupName); } catch(Throwable t){}
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

/**
 * 显示我的好友列表弹窗（流式加载）
 * @param activity Activity实例
 * @param isDark 是否深色主题
 */
void showFriendListDialog(Activity activity, boolean isDark) {
    if (activity == null || activity.isFinishing()) return;
    int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
    int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
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
                TextView closeBtn = new TextView(activity);
                closeBtn.setText("关闭");
                closeBtn.setTextSize(15);
                closeBtn.setTextColor(isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT);
                closeBtn.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
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
                applyUiTheme(activity, ref[0]);

                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Object friends = getAllFriend();
                            final List allItems = new ArrayList();
                            
                            if (friends != null && ((List) friends).size() > 0) {
                                for (int i = 0; i < ((List) friends).size(); i++) {
                                    Object f = ((List) friends).get(i);
                                    String uin = "";
                                    try { uin = f.uin; } catch(Throwable t){}
                                    String name = "";
                                    try { name = f.name; } catch(Throwable t){}
                                    String remark = "";
                                    try { remark = f.remark; } catch(Throwable t){}
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

void 显示菜单(final Activity activity) {
    if (activity == null || activity.isFinishing()) {
        traceLog("api_log.txt", "显示菜单异常: Activity无效或正在关闭");
        OK = false;
        return;
    }

    try {
        final String[] itemTexts = {"Java脚本", "设置界面", "开/关模拟定位", "设置经纬度", "开/关输入框提示", "设置输入框提示词", "消息统计", "空间操作", "取消/重载", "运行状态", "HTML浏览器"};
        final String[] itemIcons = {"📜", "⚙️", "📍", "🌍", "🕹", "🍭", "📊", "🍡", "🔄", "📈", "🌐"};

        final boolean isDarkMode = isThemeDark(activity);
        final String COLOR_PRIMARY, COLOR_SURFACE, COLOR_ON_SURFACE, COLOR_OUTLINE, COLOR_RIPPLE;

        if (isDarkMode) {
            COLOR_PRIMARY = "#FF8AB4F8";
            COLOR_SURFACE = "#FF121212";
            COLOR_ON_SURFACE = "#FFEFEFEF";
            COLOR_OUTLINE = "#FF333333";
            COLOR_RIPPLE = "#268AB4F8";
        } else {
            final String[] COLOR_POOL = {"#2196F3", "#4CAF50", "#9C27B0", "#E91E63", "#FF9800", "#00BCD4", "#3F51B5", "#8BC34A", "#FFC107"};
            COLOR_PRIMARY = COLOR_POOL[new Random().nextInt(COLOR_POOL.length)];
            COLOR_SURFACE = "#FFFFFFFF";
            COLOR_ON_SURFACE = "#FF000000";
            COLOR_OUTLINE = "#1AFFFFFF";
            COLOR_RIPPLE = "#26" + COLOR_PRIMARY.substring(2);
        }

        final int colorInt = Color.parseColor(COLOR_PRIMARY);

        int adjustAlpha(int color, float factor) {
            int alpha = Math.round(Color.alpha(color) * factor);
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }

        final List itemList = new ArrayList();

        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    LinearLayout rootLayout = new LinearLayout(activity);
                    rootLayout.setOrientation(LinearLayout.VERTICAL);
                    rootLayout.setPadding(0, dp(activity, 12), 0, dp(activity, 2));
                    rootLayout.setBackgroundColor(Color.parseColor(COLOR_SURFACE));

                    LinearLayout titleBarLayout = new LinearLayout(activity);
                    titleBarLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    titleBarLayout.setOrientation(LinearLayout.HORIZONTAL);
                    titleBarLayout.setGravity(Gravity.CENTER_VERTICAL);
                    titleBarLayout.setPadding(dp(activity, 12) + 8, dp(activity, 12), dp(activity, 12), dp(activity, 12));

                    TextView titleView = new TextView(activity);
                    titleView.setText("功能菜单");
                    titleView.setTextSize(17);
                    titleView.setTypeface(null, Typeface.BOLD);
                    titleView.setLetterSpacing(0.01f);
                    titleView.setTextColor(Color.parseColor(COLOR_ON_SURFACE));
                    titleView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                    // 更新日志按钮
                    final TextView updateLogButton = new TextView(activity);
                    updateLogButton.setText("📝");
                    updateLogButton.setTextSize(22);
                    updateLogButton.setTextColor(isDarkMode ? Color.parseColor("#E0E0E0") : Color.parseColor("#333333"));
                    updateLogButton.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28));
                    updateParams.rightMargin = dp(activity, 27);
                    updateLogButton.setLayoutParams(updateParams);
                    updateLogButton.setClickable(true);
                    updateLogButton.setBackground(null);
                    updateLogButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            vibrate(activity, 32);
                            if (v.getTag() instanceof AlertDialog) ((AlertDialog) v.getTag()).dismiss();
                            try {
                                String logContent = 读(pluginPath + "/更新日志.txt");
                                mkts(activity, "更新日志", logContent);
                            } catch (Exception e) {
                                Toast("读取更新日志失败: " + e.getMessage());
                            }
                        }
                    });

                    View themeToggle;
                    boolean imageLoaded = false;
                    int tintColor = isDarkMode ? Color.parseColor("#E0E0E0") : Color.parseColor("#333333");
                    ImageView imgToggle = new ImageView(activity);
                    try {
                        String imgName = isDarkMode ? "黑.png" : "白.png";
                        String imgPath = rootPath + imgName;
                        File imgFile = new File(imgPath);
                        if (imgFile.exists()) {
                            Bitmap bmp = BitmapFactory.decodeFile(imgPath);
                            if (bmp != null) {
                                imgToggle.setColorFilter(tintColor);
                                imgToggle.setImageBitmap(bmp);
                                imgToggle.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                imageLoaded = true;
                                traceLog("api_log.txt", "主题图标加载成功: " + imgName);
                            }
                        }
                    } catch (Throwable e) { /* ignore */ }

                    if (imageLoaded) {
                        themeToggle = imgToggle;
                    } else {
                        TextView txtToggle = new TextView(activity);
                        txtToggle.setText(isDarkMode ? "☾" : "☀");
                        txtToggle.setTextSize(22);
                        txtToggle.setTextColor(isDarkMode ? Color.parseColor("#E0E0E0") : Color.parseColor("#333333"));
                        txtToggle.setGravity(Gravity.CENTER);
                        themeToggle = txtToggle;
                    }

                    LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28));
                    themeParams.rightMargin = dp(activity, 15);
                    themeToggle.setLayoutParams(themeParams);
                    themeToggle.setClickable(true);
                    themeToggle.setBackground(null);
                    themeToggle.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            try {
                                vibrate(activity, 32);
                                boolean current = getBoolean("settings", "黑白", false);
                                putBoolean("settings", "黑白", !current);
                                Toast("主题已切换");
                                Object tag = v.getTag();
                                if (tag instanceof AlertDialog) ((AlertDialog) tag).dismiss();
                            } catch (Exception e) {
                                Toast("切换失败: " + e.getMessage());
                            }
                        }
                    });

                    final ImageView settingsIcon = new ImageView(activity);
                    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28));
                    iconParams.leftMargin = dp(activity, 12);
                    settingsIcon.setLayoutParams(iconParams);
                    settingsIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    settingsIcon.setClickable(true);
                    settingsIcon.setFocusable(true);

                    try {
                        String iconPathStr = null;
                        try { iconPathStr = settingiconPath; } catch (Exception e) {}
                        if (iconPathStr != null && new File(iconPathStr).exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(iconPathStr);
                            if (bitmap != null) {
                                settingsIcon.setImageBitmap(bitmap);
                                settingsIcon.setColorFilter(tintColor);
                                traceLog("api_log.txt", "设置图标加载成功");
                            } else throw new Exception("Bitmap解码失败");
                        } else throw new Exception("文件不存在: " + iconPathStr);
                    } catch (Throwable loadError) {
                        traceLog("api_log.txt", "设置图标加载失败: " + loadError.getMessage());
                        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
                        Bitmap bmp = Bitmap.createBitmap(dp(activity, 28), dp(activity, 28), conf);
                        Canvas canvas = new Canvas(bmp);
                        Paint paint = new Paint();
                        paint.setColor(colorInt);
                        paint.setTextSize(dp(activity, 12));
                        paint.setAntiAlias(true);
                        paint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText("设置", dp(activity, 28) / 2, dp(activity, 28) / 2 + dp(activity, 2), paint);
                        settingsIcon.setImageBitmap(bmp);
                    }

                    settingsIcon.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            vibrate(activity, 48);
                            if (v.getTag() instanceof AlertDialog) ((AlertDialog) v.getTag()).dismiss();
                            showStyleSettingsDialog(activity);
                        }
                    });

                    titleBarLayout.addView(titleView);
                    titleBarLayout.addView(updateLogButton);
                    titleBarLayout.addView(themeToggle);
                    titleBarLayout.addView(settingsIcon);
                    rootLayout.addView(titleBarLayout);

                    LinearLayout itemsContainer = new LinearLayout(activity);
                    itemsContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
                    itemsContainer.setOrientation(LinearLayout.VERTICAL);
                    itemsContainer.setPadding(dp(activity, 12), dp(activity, 2), dp(activity, 12), dp(activity, 2));
                    itemsContainer.setBackgroundColor(Color.parseColor(COLOR_SURFACE));

                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    builder.setView(rootLayout);
                    builder.setCancelable(true);
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            OK = false;
                            vibrate(activity, 48);
                        }
                    });
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            OK = false;
                        }
                    });
                    final AlertDialog customDialog = builder.create();

                    updateLogButton.setTag(customDialog);
                    themeToggle.setTag(customDialog);
                    settingsIcon.setTag(customDialog);

                    for (int i = 0; i < itemTexts.length; i++) {
                        final int index = i;
                        final FrameLayout itemWrapper = new FrameLayout(activity);
                        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(dp(activity, 40) * 1.15f));
                        wrapperParams.bottomMargin = dp(activity, 4) + 1;
                        itemWrapper.setLayoutParams(wrapperParams);

                        final LinearLayout itemLayout = new LinearLayout(activity);
                        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
                        itemLayout.setPadding(dp(activity, 12), dp(activity, 2), dp(activity, 12), dp(activity, 2));
                        itemLayout.setClickable(false);

                        if (isDarkMode) {
                            itemLayout.setElevation(dp(activity, 1));
                            itemLayout.setTranslationZ(dp(activity, 1));
                        } else {
                            itemLayout.setElevation(dp(activity, 2));
                            itemLayout.setTranslationZ(dp(activity, 2));
                        }

                        final GradientDrawable glassBg = new GradientDrawable();
                        glassBg.setShape(GradientDrawable.RECTANGLE);
                        glassBg.setCornerRadius(dp(activity, 16));
                        glassBg.setColor(isDarkMode ? Color.parseColor("#FF2D2D2D") : adjustAlpha(colorInt, 0.05f));
                        glassBg.setStroke(dp(activity, 1), Color.parseColor(isDarkMode ? COLOR_OUTLINE : COLOR_OUTLINE));
                        itemLayout.setBackground(glassBg);

                        TextView iconView = new TextView(activity);
                        iconView.setText(itemIcons[i]);
                        iconView.setTextSize(18);
                        iconView.setTextColor(colorInt);
                        LinearLayout.LayoutParams iconParamsItem = new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28));
                        iconParamsItem.gravity = Gravity.CENTER_VERTICAL;
                        iconView.setLayoutParams(iconParamsItem);
                        iconView.setGravity(Gravity.CENTER);

                        TextView textView = new TextView(activity);
                        textView.setText(itemTexts[i]);
                        textView.setTextSize(14);
                        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                        textView.setTextColor(Color.parseColor(COLOR_ON_SURFACE));
                        textView.setPadding(dp(activity, 12), 0, 0, 0);
                        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                        TextView arrowView = new TextView(activity);
                        arrowView.setText("›");
                        arrowView.setTextSize(20);
                        arrowView.setTextColor(isDarkMode ? Color.parseColor("#FF888888") : adjustAlpha(colorInt, 0.3f));
                        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(activity, 28) - dp(activity, 8), LinearLayout.LayoutParams.MATCH_PARENT);
                        arrowParams.gravity = Gravity.CENTER;
                        arrowView.setLayoutParams(arrowParams);
                        arrowView.setGravity(Gravity.CENTER);

                        final FrameLayout rippleOverlay = new FrameLayout(activity);
                        rippleOverlay.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                        itemLayout.addView(iconView);
                        itemLayout.addView(textView);
                        itemLayout.addView(arrowView);
                        itemWrapper.addView(itemLayout);
                        itemWrapper.addView(rippleOverlay);
                        itemWrapper.setTag(customDialog);

                        itemWrapper.setOnTouchListener(new View.OnTouchListener() {
                            private float touchX, touchY;
                            private View rippleView = null;
                            private ValueAnimator syncAnimator = null;
                            private Runnable cleanupTask = null;
                            private boolean isMovedOut = false;

                            private void resetBackgroundState() {
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        glassBg.setColor(isDarkMode ? Color.parseColor("#FF2D2D2D") : adjustAlpha(colorInt, 0.05f));
                                        itemLayout.setBackground(glassBg);
                                    }
                                });
                            }

                            public boolean onTouch(View v, MotionEvent event) {
                                try {
                                    switch (event.getAction()) {
                                        case MotionEvent.ACTION_DOWN:
                                            isMovedOut = false;
                                            touchX = event.getX();
                                            touchY = event.getY();

                                            if (rippleView == null) {
                                                rippleView = new View(activity);
                                                int rippleSize = dp(activity, 8);
                                                FrameLayout.LayoutParams rippleParams = new FrameLayout.LayoutParams(rippleSize, rippleSize);
                                                rippleParams.leftMargin = (int) touchX - rippleSize / 2;
                                                rippleParams.topMargin = (int) touchY - rippleSize / 2;
                                                rippleView.setLayoutParams(rippleParams);

                                                GradientDrawable rippleDrawable = new GradientDrawable();
                                                rippleDrawable.setShape(GradientDrawable.OVAL);
                                                rippleDrawable.setColor(isDarkMode ? Color.parseColor(COLOR_RIPPLE) : adjustAlpha(colorInt, 0.25f));
                                                rippleView.setBackground(rippleDrawable);
                                                rippleOverlay.addView(rippleView);
                                            }

                                            syncAnimator = ValueAnimator.ofFloat(0f, 1f);
                                            syncAnimator.setDuration(600);
                                            syncAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                                            syncAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                                public void onAnimationUpdate(ValueAnimator animation) {
                                                    float progress = (Float) animation.getAnimatedValue();
                                                    rippleView.setScaleX(1f + progress * 9f);
                                                    rippleView.setScaleY(1f + progress * 9f);
                                                    rippleView.setAlpha(1f - progress);
                                                    glassBg.setColor(isDarkMode ? Color.parseColor("#FF383838") : adjustAlpha(colorInt, 0.05f + progress * 0.08f));
                                                    itemLayout.setBackground(glassBg);
                                                }
                                            });
                                            syncAnimator.addListener(new Animator.AnimatorListener() {
                                                public void onAnimationEnd(Animator animator) {
                                                    if (rippleView != null && rippleView.getParent() != null) {
                                                        rippleOverlay.removeView(rippleView);
                                                        rippleView = null;
                                                    }
                                                    resetBackgroundState();
                                                }
                                                public void onAnimationStart(Animator animator) {}
                                                public void onAnimationCancel(Animator animator) {}
                                                public void onAnimationRepeat(Animator animator) {}
                                            });
                                            syncAnimator.start();

                                            cleanupTask = new Runnable() {
                                                public void run() {
                                                    if (rippleView != null && rippleView.getParent() != null) {
                                                        rippleOverlay.removeView(rippleView);
                                                        rippleView = null;
                                                    }
                                                }
                                            };
                                            return true;

                                        case MotionEvent.ACTION_MOVE:
                                            if (event.getX() < 0 || event.getX() > v.getWidth() || event.getY() < 0 || event.getY() > v.getHeight()) {
                                                if (!isMovedOut) isMovedOut = true;
                                                if (syncAnimator != null) syncAnimator.cancel();
                                                if (cleanupTask != null) cleanupTask.run();
                                                resetBackgroundState();
                                            }
                                            return true;

                                        case MotionEvent.ACTION_UP:
                                            if (!isMovedOut && event.getX() >= 0 && event.getX() <= v.getWidth() && event.getY() >= 0 && event.getY() <= v.getHeight()) {
                                                v.performClick();
                                            }
                                            if (syncAnimator != null) syncAnimator.cancel();
                                            if (cleanupTask != null) cleanupTask.run();
                                            resetBackgroundState();
                                            return true;

                                        case MotionEvent.ACTION_CANCEL:
                                            isMovedOut = true;
                                            if (syncAnimator != null) syncAnimator.cancel();
                                            if (cleanupTask != null) cleanupTask.run();
                                            resetBackgroundState();
                                            return true;

                                        default: return false;
                                    }
                                } catch (Throwable t) {
                                    return false;
                                }
                            }
                        });

                        itemWrapper.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                try {
                                    for (int j = 0; j < itemList.size(); j++) {
                                        final FrameLayout item = (FrameLayout) itemList.get(j);
                                        int indexDiff = Math.abs(j - index);
                                        int pushDistance = dp(activity, 6) + (indexDiff * 2);
                                        if (pushDistance > dp(activity, 12)) pushDistance = dp(activity, 12);

                                        final float targetTransY = (j == index) ? 0f : (j < index ? -pushDistance : pushDistance);
                                        final float targetScale = (j == index) ? 1.15f : 1.0f;

                                        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
                                        animator.setDuration(180);
                                        animator.setInterpolator(new DecelerateInterpolator());
                                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                            public void onAnimationUpdate(ValueAnimator animation) {
                                                float progress = (Float) animation.getAnimatedValue();
                                                if (j == index) {
                                                    item.setScaleX(1f + (targetScale - 1f) * progress);
                                                    item.setScaleY(1f + (targetScale - 1f) * progress);
                                                }
                                                item.setTranslationY(targetTransY * progress);
                                            }
                                        });
                                        animator.start();
                                    }

                                    new Handler().postDelayed(new Runnable() {
                                        public void run() {
                                            for (int j = 0; j < itemList.size(); j++) {
                                                final FrameLayout item = (FrameLayout) itemList.get(j);
                                                int indexDiff = Math.abs(j - index);
                                                int pushDistance = dp(activity, 6) + (indexDiff * 2);
                                                if (pushDistance > dp(activity, 12)) pushDistance = dp(activity, 12);

                                                final float targetTransY = (j == index) ? 0f : (j < index ? -pushDistance : pushDistance);
                                                final float targetScale = (j == index) ? 1.15f : 1.0f;

                                                ValueAnimator resetAnim = ValueAnimator.ofFloat(1f, 0f);
                                                resetAnim.setDuration(150);
                                                resetAnim.setInterpolator(new AccelerateInterpolator());
                                                resetAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                                    public void onAnimationUpdate(ValueAnimator animation) {
                                                        float progress = (Float) animation.getAnimatedValue();
                                                        if (j == index) {
                                                            item.setScaleX(1f + (targetScale - 1f) * progress);
                                                            item.setScaleY(1f + (targetScale - 1f) * progress);
                                                        }
                                                        item.setTranslationY(targetTransY * progress);
                                                    }
                                                });
                                                resetAnim.start();
                                            }

                                            AlertDialog dialog = (AlertDialog) v.getTag();
                                            if (dialog != null && dialog.isShowing()) dialog.dismiss();

                                            switch (index) {
                                                case 0: 跳转到页面("me.yxp.qfun.activity.PluginActivity"); break;
                                                case 1: 跳转到页面("me.yxp.qfun.activity.SettingActivity"); break;
                                                case 2: 模拟定位开关(); break;
                                                case 3: showLocationDialog(activity); break;
                                                case 4: 输入框提示开关(); break;
                                                case 5: showInputDialog(activity); break;
                                                case 6: showStatsDialog(activity); break;
                                                case 7: showQzoneConfig(); break;
                                                case 8:
                                                    vibrate(activity, 48);
                                                    showSelectionDialog(activity, "你想选哪个呢？", "取消加载脚本", "重新加载脚本");
                                                    break;
                                                case 9: 运行状态Dialog(activity); break;
                                                case 10: showHtmlOptionDialog(activity); break;
                                            }
                                            OK = false;
                                        }
                                    }, 220);
                                } catch (Throwable clickError) {
                                    traceLog("api_log.txt", "点击事件异常: " + clickError.getMessage());
                                }
                            }
                        });

                        itemWrapper.setAlpha(0f);
                        itemWrapper.setTranslationY(dp(activity, 12));
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                itemWrapper.animate().alpha(1f).translationY(0f).setDuration(200).setInterpolator(new DecelerateInterpolator());
                            }
                        }, i * 40);

                        itemsContainer.addView(itemWrapper);
                    }

                    rootLayout.addView(itemsContainer);
                    customDialog.show();

                    Window window = customDialog.getWindow();
                    if (window != null) applyDialogSize(activity, window);

                    GradientDrawable dialogBg = new GradientDrawable();
                    dialogBg.setColor(Color.parseColor(COLOR_SURFACE));
                    dialogBg.setCornerRadius(dp(activity, 16));
                    customDialog.getWindow().setBackgroundDrawable(dialogBg);

                    Button cancelBtn = customDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                    if (cancelBtn != null) {
                        cancelBtn.setMinHeight(dp(activity, 20));
                        cancelBtn.setMinWidth(0);
                        cancelBtn.setPadding(dp(activity, 12), dp(activity, 2), dp(activity, 12), dp(activity, 2));
                        cancelBtn.setTextColor(Color.parseColor(isDarkMode ? COLOR_PRIMARY : "#FF2196F3"));

                        ViewGroup.MarginLayoutParams btnLayoutParams = (ViewGroup.MarginLayoutParams) cancelBtn.getLayoutParams();
                        btnLayoutParams.bottomMargin = dp(activity, 4);
                        btnLayoutParams.topMargin = 0;
                        btnLayoutParams.leftMargin = 0;
                        btnLayoutParams.rightMargin = 0;
                        cancelBtn.setLayoutParams(btnLayoutParams);
                        customDialog.getWindow().setLayout(windowParams.width, WindowManager.LayoutParams.WRAP_CONTENT);
                    }

                    traceLog("api_log.txt", "菜单显示成功");
                } catch (Throwable uiError) {
                    traceLog("api_log.txt", "UI构建异常: " + uiError.getMessage());
                    OK = false;
                }
            }
        });
    } catch (Exception e) {
        OK = false;
        traceLog("api_log.txt", "显示菜单异常: " + e.getMessage());
    }
}

//非常花里胡哨的tips弹窗
public void ts(Activity activity, String title, String content) {
	if (activity == null || activity.isFinishing()) {
		traceLog("api_log.txt", "Activity无效，无法显示弹窗");
		return;
	}
	boolean isDark = isThemeDark(activity);

	final String finalContent = content == null ? "" : content;
	// 多颜色高亮数组（含#的行循环使用）
	final int[] HIGHLIGHT_COLORS = {
		Color.parseColor("#FF6B6B"), // 红色
		// Color.parseColor("#4ECDC4"),   // 青色 太淡了不要了
		Color.parseColor("#45B7D1"), // 蓝色
		// Color.parseColor("#96CEB4"),   // 绿色 不好看也不要了
		Color.parseColor("#DDA0DD") // 紫色
	};
	// 普通行颜色
	final int NORMAL_LINE_COLOR = isDark ? Color.parseColor("#AAAAAA") : Color.parseColor("#666666");

	activity.runOnUiThread(new Runnable() {
		public void run() {
			try {
				vibrate(activity, 48);
			} catch (Exception e) {
				traceLog("api_log.txt", "震动执行异常: " + e.getMessage());
			}
			/* 根布局：圆角 + 55% 透明 */
			GradientDrawable bg = new GradientDrawable();
			bg.setColor(Color.parseColor(isDark ? UI_COLOR_BG_DARK : "#BFFFFFFF"));
			bg.setCornerRadius(dp(activity, 16));

			LinearLayout layout = new LinearLayout(activity);
			layout.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
			layout.setOrientation(LinearLayout.VERTICAL);

			TextView textView = new TextView(activity);
			textView.setTextSize(17);
			textView.setTextIsSelectable(true);
			textView.setSingleLine(false);
			textView.setMaxLines(Integer.MAX_VALUE);
			textView.setEllipsize(null);
			// 默认文字颜色
			textView.setTextColor(isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT);

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
				traceLog("api_log.txt", "文本高亮处理异常: " + e.getMessage());
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
						traceLog("api_log.txt", "按钮点击异常: " + e.getMessage());
					}
				}
			});
			builder.setCancelable(false);

			AlertDialog alertDialog = builder.create();
			alertDialog.show();
			
            // 应用统一主题
			applyUiTheme(activity, alertDialog);

			// alertDialog.getWindow().setBackgroundDrawable(bg);

			TextView titleView = (TextView) alertDialog.findViewById(android.R.id.title);
			if (titleView != null) {
				titleView.setTextSize(22);
			}

		}
	});
}

loadJar(rootPath + "commonmark-0.21.0.jar");

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;


public void mkts(Activity activity, String title, String markdownContent) {
	if (activity == null || activity.isFinishing()) {
		traceLog("api_log.txt", "Activity无效");
		return;
	}

	final String finalMarkdown = markdownContent == null ? "" : markdownContent;
	final boolean isDark = isThemeDark(activity);

	ThreadPool.execute(new Runnable() {
		public void run() {
			try {
				traceLog("api_log.txt", "开始解析，长度: " + finalMarkdown.length());

				Parser parser = Parser.builder().build();
				Node document = parser.parse(finalMarkdown);
				HtmlRenderer renderer = HtmlRenderer.builder().build();
				String htmlContent = renderer.render(document);

				traceLog("api_log.txt", "解析成功，输出长度: " + htmlContent.length());

				final String finalHtml = htmlContent;

				traceLog("api_log.txt", "HTML片段: " + finalHtml.substring(0, Math.min(500, finalHtml.length())));

				activity.runOnUiThread(new Runnable() {
					public void run() {
						createMarkdownDialog(activity, title, finalHtml, true, isDark);
					}
				});

			} catch (Throwable e) {
				traceLog("api_log.txt", "解析失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());

				final String fallbackHtml = generateFallbackHtml(finalMarkdown, isDark);

				activity.runOnUiThread(new Runnable() {
					public void run() {
						createMarkdownDialog(activity, title, fallbackHtml, false, isDark);
					}
				});
			}
		}
	});
}

private void createMarkdownDialog(final Activity activity, String title, String html, boolean parseSuccess, boolean isDark) {
	try {
		vibrate(activity, 48);
	} catch (Exception e) {
		traceLog("api_log.txt", "震动执行异常: " + e.getMessage());
	}

	GradientDrawable bg = new GradientDrawable();
	bg.setColor(Color.parseColor(isDark ? UI_COLOR_BG_DARK : "#BFFFFFFF"));
	bg.setCornerRadius(dp(activity, 16));

	final String wrappedHtml = wrapHtmlWithCss(html, isDark);

	WebView webView = new WebView(activity);

	LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
		LinearLayout.LayoutParams.MATCH_PARENT,
		dp(activity, 800)
	);
	webView.setLayoutParams(webParams);

	// 启用WebView原生滚动
	webView.setVerticalScrollBarEnabled(true);
	webView.setHorizontalScrollBarEnabled(true);
	webView.setScrollContainer(true);

	// webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

	// 透明背景
	webView.setBackgroundColor(Color.TRANSPARENT);

	webView.getSettings().setJavaScriptEnabled(false);
	webView.getSettings().setDefaultTextEncodingName("UTF-8");

	webView.setWebViewClient(new WebViewClient() {
		public void onPageFinished(WebView view, String url) {
			traceLog("api_log.txt", "页面加载完成，内容高度: " + view.getContentHeight());

			view.post(new Runnable() {
				public void run() {
					view.requestLayout();
					view.invalidate();
				}
			});
		}

		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			traceLog("api_log.txt", "加载失败: " + errorCode + " - " + description);
		}
	});

	webView.loadDataWithBaseURL(null, wrappedHtml, "text/html", "UTF-8", null);

	AlertDialog.Builder builder = new AlertDialog.Builder(activity,
		isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
	builder.setTitle(parseSuccess ? title : title + " (显示异常)");
	builder.setView(webView);

	builder.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			try {
				Toast("你知道啥了");
				vibrate(activity, 48);
			} catch (Exception e) {
				traceLog("api_log.txt", "按钮点击异常: " + e.getMessage());
			}
		}
	});
	builder.setCancelable(false);

	AlertDialog alertDialog = builder.create();
	alertDialog.show();
	
    // 应用统一主题
	applyUiTheme(activity, alertDialog);

	TextView titleView = (TextView) alertDialog.findViewById(android.R.id.title);
	if (titleView != null) {
		titleView.setTextSize(22);
	}

	traceLog("api_log.txt", "Dialog显示成功，解析状态: " + parseSuccess);
}

//CSS
private String wrapHtmlWithCss(String htmlContent, boolean isDark) {
	String textColor = isDark ? "#EFEFEF" : "#333";
	String bgColor = isDark ? "#2D2D2D" : "#f4f4f4";

	return "<html><head>" +
		"<meta charset='UTF-8'><style>" +
		// 强制所有元素继承
		"*{color:" + textColor + " !important;}" +
		// body
		"body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;padding:8px;line-height:1.4;background:transparent;font-size:14px;}" +
		// 块级元素样式
		"pre{background:" + bgColor + " !important;padding:6px !important;border-radius:3px !important;overflow-x:auto !important;font-size:13px !important;}" +
		"code{background:" + bgColor + " !important;padding:1px 3px !important;border-radius:2px !important;font-family:monospace !important;font-size:13px !important;}" +
		"h1,h2,h3{margin:8px 0 4px 0 !important;font-weight:600 !important;}" +
		"h1{font-size:18px !important;}h2{font-size:16px !important;}h3{font-size:15px !important;}" +
		"p{margin:4px 0 !important;}" +
		"ul,ol{padding-left:16px !important;margin:4px 0 !important;}" +
		"li{margin:2px 0 !important;}" +
		"</style></head><body>" + htmlContent + "</body></html>";
}

private String generateFallbackHtml(String markdown, boolean isDark) {
	String escaped = escapeHtml(markdown);
	String bgColor = isDark ? "#2D2D2D" : "#f4f4f4";
	String textColor = isDark ? "#EFEFEF" : "#333";
	return "<pre style='background:" + bgColor + ";padding:6px;border-radius:3px;overflow-x:auto;font-family:monospace;font-size:13px;color:" + textColor + ";'>" + escaped + "</pre>";
}

private String escapeHtml(String text) {
	if (text == null) return "";
	return text.replace("&", "&amp;")
		.replace("<", "&lt;")
		.replace(">", "&gt;")
		.replace("\"", "&quot;")
		.replace("'", "&#39;");
}

public interface GroupSelectCallback {
    void onSelected(List selected);
}

/**
 * 显示群组/好友选择对话框。
 * <p>
 * 根据指定的模式显示好友列表、群聊列表或两者的组合，支持多选、搜索、全选、反选等操作。
 * 选择完成后通过回调返回选中的群号或好友QQ号列表。
 * </p>
 *
 * @param act          当前 Activity，用于创建对话框
 * @param mode         选择模式：
 *                     <ul>
 *                       <li>1 - 仅选择好友</li>
 *                       <li>2 - 仅选择群聊</li>
 *                       <li>3 - 选择好友和群聊（混合列表）</li>
 *                     </ul>
 * @param initSelected 初始已选中的项列表（元素为 String 类型的 QQ/群号），可为空
 * @param callback     选择完成后的回调接口，{@link GroupSelectCallback#onSelected(List)} 
 *                     会在用户点击“确定”时被调用，参数为最终选中的列表
 */
public void showGroupSelector(final Activity act, final int mode, final List initSelected, final GroupSelectCallback callback) {
    final int COLOR_PRIMARY = Color.parseColor("#6750A4");
    final int COLOR_ON_PRIMARY = Color.WHITE;
    final int COLOR_SURFACE = Color.parseColor("#FFFFFF");
    final int COLOR_SURFACE_VARIANT = Color.parseColor("#F5F7FA");
    final int COLOR_OUTLINE = Color.parseColor("#79747E");
    final int COLOR_ON_SURFACE = Color.parseColor("#FF000000");
    final int COLOR_ON_SURFACE_VAR = Color.parseColor("#FF333333");

    final Dialog d = new Dialog(act);
    d.requestWindowFeature(Window.FEATURE_NO_TITLE);
    Window w = d.getWindow();
    if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    w.setLayout(dp(act, 360), (int)(act.getResources().getDisplayMetrics().heightPixels * 0.85));

    LinearLayout root = new LinearLayout(act);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackground(createBg(act, COLOR_SURFACE, 28));
    root.setPadding(dp(act, 20), dp(act, 20), dp(act, 20), dp(act, 20));

    TextView title = new TextView(act);
    title.setText(mode == 1 ? "选择好友" : mode == 2 ? "选择群聊" : "选择群聊与好友");
    title.setTextSize(20);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(COLOR_ON_SURFACE);
    root.addView(title);

    final TextView summary = new TextView(act);
    summary.setTextSize(14);
    summary.setTextColor(COLOR_ON_SURFACE_VAR);
    root.addView(summary);

    EditText search = new EditText(act);
    search.setHint("搜索群名称或群号");
    search.setBackground(createInputBg(act, COLOR_SURFACE_VARIANT, COLOR_OUTLINE, COLOR_PRIMARY));
    search.setPadding(dp(act, 16), dp(act, 12), dp(act, 16), dp(act, 12));
    root.addView(search);

    ListView lv = new ListView(act);
    lv.setDivider(null);
    lv.setDividerHeight(0);
    root.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1f));

    LinearLayout row1 = new LinearLayout(act);
    row1.setOrientation(LinearLayout.HORIZONTAL);
    row1.setGravity(Gravity.END);
    row1.setPadding(0, dp(act, 12), 0, dp(act, 8));
    Button bCancelAll = makeSmallBtn(act, "取消全选", COLOR_PRIMARY);
    row1.addView(bCancelAll);
    Button bReverse = makeSmallBtn(act, "反选", COLOR_PRIMARY);
    row1.addView(bReverse);
    Button bSelectAll = makeSmallBtn(act, "全选", COLOR_PRIMARY);
    row1.addView(bSelectAll);
    root.addView(row1);

    LinearLayout row2 = new LinearLayout(act);
    row2.setOrientation(LinearLayout.HORIZONTAL);
    row2.setGravity(Gravity.END);
    row2.setPadding(0, dp(act, 8), 0, 0);
    Button btnCancel = new Button(act);
    btnCancel.setText("取消");
    // btnCancel.setTextColor(COLOR_PRIMARY);
    row2.addView(btnCancel);
    Button btnConfirm = new Button(act);
    btnConfirm.setText("确定");
    // btnConfirm.setTextColor(COLOR_PRIMARY);
    row2.addView(btnConfirm);
    root.addView(row2);

    d.setContentView(root);
    d.show();

    final List allItems = new ArrayList();

    if (mode == 1 || mode == 3) {
        List friendList = (List) getAllFriend();
        for (Object obj : friendList) {
            String displayName = (obj.remark != null && !obj.remark.isEmpty()) ? obj.remark : obj.name;
            Map m = new HashMap();
            m.put("uin", obj.uin);
            m.put("name", displayName);
            allItems.add(m);
        }
    }

    if (mode == 2 || mode == 3) {
        List groupList = (List) getGroupList();
        for (Object obj : groupList) {
            GroupInfo g = (GroupInfo) obj;
            Map m = new HashMap();
            m.put("uin", g.group);
            m.put("name", g.groupName);
            allItems.add(m);
        }
    }

    if (allItems.isEmpty()) {
        Map test = new HashMap();
        test.put("uin", "000000");
        test.put("name", "未获取到数据 (请检查权限)");
        allItems.add(test);
    }

    final List display = new ArrayList();
    final Set selected = new HashSet(initSelected);

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
                item.setPadding(dp(act, 12), dp(act, 12), dp(act, 12), dp(act, 12));
            } else {
                item = (LinearLayout) cv;
                item.removeAllViews();
            }

            final Map m = (Map) display.get(pos);
            final String uin = (String) m.get("uin");
            final boolean isChecked = selected.contains(uin);

            final FrameLayout checkContainer = new FrameLayout(act);
            checkContainer.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 28), dp(act, 28)));

            final View box = new View(act);
            final GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(act, 6));
            box.setBackground(bg);
            checkContainer.addView(box);

            final FrameLayout checkContent = new FrameLayout(act);
            checkContent.setLayoutParams(new FrameLayout.LayoutParams(dp(act, 28), dp(act, 28)));

            View checkLine1 = new View(act);
            GradientDrawable line1Bg = new GradientDrawable();
            line1Bg.setShape(GradientDrawable.RECTANGLE);
            line1Bg.setCornerRadius(dp(act, 1));
            line1Bg.setColor(Color.WHITE);
            checkLine1.setBackground(line1Bg);
            FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(dp(act, 3), dp(act, 8));
            lp1.gravity = Gravity.CENTER;
            lp1.leftMargin = dp(act, -6);
            lp1.topMargin = dp(act, 4);
            checkLine1.setLayoutParams(lp1);
            checkLine1.setRotation(-45);
            checkContent.addView(checkLine1);

            View checkLine2 = new View(act);
            GradientDrawable line2Bg = new GradientDrawable();
            line2Bg.setShape(GradientDrawable.RECTANGLE);
            line2Bg.setCornerRadius(dp(act, 1));
            line2Bg.setColor(Color.WHITE);
            checkLine2.setBackground(line2Bg);
            FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(dp(act, 3), dp(act, 14));
            lp2.gravity = Gravity.CENTER;
            lp2.leftMargin = dp(act, 4);
            lp2.topMargin = dp(act, -2);
            checkLine2.setLayoutParams(lp2);
            checkLine2.setRotation(45);
            checkContent.addView(checkLine2);

            checkContent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            checkContainer.addView(checkContent);

            if (isChecked) {
                bg.setColor(COLOR_PRIMARY);
                bg.setStroke(dp(act, 2), COLOR_PRIMARY);
            } else {
                bg.setColor(Color.TRANSPARENT);
                bg.setStroke(dp(act, 2), COLOR_OUTLINE);
            }

            item.addView(checkContainer);

            TextView tv = new TextView(act);
            tv.setText(m.get("name") + " (" + uin + ")");
            tv.setTextSize(16);
            tv.setTextColor(COLOR_ON_SURFACE);
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, -2, 1);
            tvLp.leftMargin = dp(act, 12);
            item.addView(tv, tvLp);

            View.OnClickListener clickListener = new View.OnClickListener() {
                public void onClick(View v) {
                    if (selected.contains(uin)) {
                        selected.remove(uin);
                        bg.setColor(Color.TRANSPARENT);
                        bg.setStroke(dp(act, 2), COLOR_OUTLINE);
                        checkContent.setVisibility(View.GONE);
                    } else {
                        selected.add(uin);
                        bg.setColor(COLOR_PRIMARY);
                        bg.setStroke(dp(act, 2), COLOR_PRIMARY);
                        checkContent.setVisibility(View.VISIBLE);
                    }
                    summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                }
            };

            item.setOnClickListener(clickListener);
            checkContainer.setOnClickListener(clickListener);

            return item;
        }
    };
    lv.setAdapter(adapter);
    summary.setText("已选 " + selected.size() + " / 总 " + display.size());

    search.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable s) {
            String q = s.toString().trim().toLowerCase();
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
            d.dismiss();
        }
    });

    btnConfirm.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (callback != null) callback.onSelected(new ArrayList(selected));
            d.dismiss();
        }
    });
}

private Drawable createBg(Context ctx, int color, int radius) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(color);
    gd.setCornerRadius(dp(ctx, radius));
    return gd;
}

private Drawable createRippleBg(Context ctx, int bgColor, int radius) {
    GradientDrawable content = new GradientDrawable();
    content.setColor(bgColor);
    content.setCornerRadius(dp(ctx, radius));
    return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#1A000000")), content, content);
}

private Drawable createButtonBg(Context ctx, int bgColor, int radius) {
    GradientDrawable content = new GradientDrawable();
    content.setColor(bgColor);
    content.setCornerRadius(dp(ctx, radius));
    return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")), content, content);
}

private StateListDrawable createInputBg(Context ctx, int surfaceVariant, int outline, int primary) {
    int r = dp(ctx, 12);
    GradientDrawable normal = new GradientDrawable();
    normal.setColor(surfaceVariant);
    normal.setCornerRadius(r);
    normal.setStroke(dp(ctx, 1), outline);
    GradientDrawable focused = new GradientDrawable();
    focused.setColor(surfaceVariant);
    focused.setCornerRadius(r);
    focused.setStroke(dp(ctx, 2), primary);
    StateListDrawable sld = new StateListDrawable();
    sld.addState(new int[]{android.R.attr.state_focused}, focused);
    sld.addState(new int[]{}, normal);
    return sld;
}

private Button makeSmallBtn(Context ctx, String text, int color) {
    Button b = new Button(ctx);
    b.setText(text);
    b.setTextColor(color);
    b.setBackground(createRippleBg(ctx, Color.TRANSPARENT, 20));
    b.setMinHeight(dp(ctx, 40));
    b.setPadding(dp(ctx, 16), dp(ctx, 8), dp(ctx, 16), dp(ctx, 8));
    // 注意：此处省略了缩放动画，如需添加可自行实现
    return b;
}