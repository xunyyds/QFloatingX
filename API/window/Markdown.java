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
