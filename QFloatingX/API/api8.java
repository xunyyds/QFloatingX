//此空间api由冷雨开发  点赞/评论由ᗜ×ᗜ改进并适配新版 使用请留名
private final HashSet doneTasks = new HashSet();
private volatile boolean isServiceRunning = false;
private Thread qzoneThread = null;
private static final String DONE_TASKS_STR_KEY = "qzone_done_tasks_str";
private final HashSet blackList = new HashSet();

private void loadDoneTasks() {
    String savedStr = getString("qzone_cfg", DONE_TASKS_STR_KEY, "");
    doneTasks.clear();
    if (!savedStr.equals("")) {
        String[] items = savedStr.split(",");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (!item.equals("")) doneTasks.add(item);
        }
        traceLog("qzone_log", "加载已处理任务数量: " + doneTasks.size());
    }
}

private void saveDoneTasks() {
    if (doneTasks.size() > 100) doneTasks.clear();
    StringBuffer sb = new StringBuffer();
    java.util.Iterator it = doneTasks.iterator();
    while (it.hasNext()) {
        if (sb.length() > 0) sb.append(",");
        sb.append((String) it.next());
    }
    putString("qzone_cfg", DONE_TASKS_STR_KEY, sb.toString());
}

private void loadBlackList() {
    String saved = getString("qzone_cfg", "blacklist", "");
    blackList.clear();
    if (!saved.isEmpty()) {
        String[] items = saved.split(",");
        for (String s : items) {
            String trim = s.trim();
            if (!trim.isEmpty()) blackList.add(trim);
        }
    }
}

private void saveBlackList() {
    StringBuffer sb = new StringBuffer();
    java.util.Iterator it = blackList.iterator();
    while (it.hasNext()) {
        if (sb.length() > 0) sb.append(",");
        sb.append((String) it.next());
    }
    putString("qzone_cfg", "blacklist", sb.toString());
}

private String jsonGet(String json, String key) {
    try {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\":\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    } catch (Throwable e) { return ""; }
}

private int jsonGetInt(String json, String key) {
    try {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\":(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    } catch (Throwable e) { return -1; }
}

private ArrayList parseActiveFeeds(String jsonResp) {
    ArrayList result = new ArrayList();
    try {
        JSONObject root = new JSONObject(jsonResp);
        if (root.optInt("code", -1) != 0) return result;
        JSONObject data = root.getJSONObject("data");
        JSONArray vFeeds = data.getJSONArray("vFeeds");
        for (int i = 0; i < vFeeds.length(); i++) {
            JSONObject feed = vFeeds.getJSONObject(i);
            JSONObject comm = feed.getJSONObject("comm");
            String orglikekey = comm.optString("orglikekey", "");
            String curlikekey = comm.optString("curlikekey", "");
            String ugckey = comm.optString("ugckey", "");
            JSONObject userinfo = feed.getJSONObject("userinfo");
            JSONObject user = userinfo.getJSONObject("user");
            String uin = user.optString("uin", "");
            if (!orglikekey.isEmpty() && !curlikekey.isEmpty() && !uin.isEmpty()) {
                String[] arr = new String[4];
                arr[0] = orglikekey;
                arr[1] = curlikekey;
                arr[2] = uin;
                arr[3] = ugckey.isEmpty() ? curlikekey : ugckey;
                result.add(arr);
            }
        }
    } catch (Throwable e) {}
    return result;
}

private void delay(long millis) {
    if (millis <= 0 || !isServiceRunning) return;
    Object lock = new Object();
    synchronized (lock) {
        long start = System.currentTimeMillis();
        long remaining = millis;
        while (remaining > 0 && isServiceRunning) {
            try {
                lock.wait(Math.min(remaining, 200));
                remaining = millis - (System.currentTimeMillis() - start);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

public void showQzoneConfig() {
    Activity act = getNowActivity();
    if (act == null) act = 最后Activity;
    if (act == null || act.isFinishing()) {
        Toast("无法打开设置：没有可用 Activity");
        return;
    }
    final Activity finalAct = act;
    finalAct.runOnUiThread(new Runnable() {
        public void run() {
            try {
                // 主题取色（跟随设置页当前主题）
                final int colorSurface = Color.parseColor(getSettingsThemeColor(finalAct, "surface"));
                final int colorOnSurface = Color.parseColor(getSettingsThemeColor(finalAct, "on_surface"));
                final int colorOnSurfaceVariant = Color.parseColor(getSettingsThemeColor(finalAct, "on_surface_variant"));
                final int colorPrimary = Color.parseColor(getSettingsThemeColor(finalAct, "primary"));

                Dialog d = new Dialog(finalAct);
                d.requestWindowFeature(1);
                d.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                FrameLayout outer = new FrameLayout(finalAct);
                int margin = dp(finalAct, 24);
                outer.setPadding(margin, margin, margin, margin);
                ScrollView scroll = new ScrollView(finalAct);
                outer.addView(scroll);
                LinearLayout card = new LinearLayout(finalAct);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(roundRect(colorSurface, dp(finalAct, 16)));
                card.setPadding(dp(finalAct, 20), dp(finalAct, 20), dp(finalAct, 20), dp(finalAct, 20));
                scroll.addView(card);

                TextView title = new TextView(finalAct);
                title.setText("空间操作配置");
                title.setTextSize(18);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(colorOnSurface);
                title.setPadding(0, 0, 0, dp(finalAct, 16));
                card.addView(title);

                // 秒赞 / 秒评 开关（复用设置页开关组件，即时保存并启停）
                card.addView(buildQzoneSwitchRow(finalAct, "秒赞", "switch_like", colorOnSurface));
                card.addView(buildQzoneSwitchRow(finalAct, "秒评", "switch_comment", colorOnSurface));

                // 评论内容
                card.addView(buildQzoneSectionTitle(finalAct, "评论内容", colorOnSurfaceVariant));
                String initText = getString("qzone_cfg", "comment", "我来暖说说啦！");
                EditText commentInput = makeInput(finalAct, "输入评论（≤100字）", colorSurface);
                commentInput.setText(initText);
                commentInput.setTextColor(colorOnSurface);
                commentInput.setHintTextColor(colorOnSurfaceVariant);
                commentInput.setMaxLines(4);
                commentInput.setGravity(Gravity.TOP | Gravity.START);
                card.addView(commentInput);

                // 间隔设置
                card.addView(buildQzoneSectionTitle(finalAct, "间隔设置", colorOnSurfaceVariant));
                LinearLayout intervalRow = new LinearLayout(finalAct);
                intervalRow.setOrientation(LinearLayout.HORIZONTAL);
                intervalRow.setGravity(Gravity.CENTER_VERTICAL);
                int itemSpacing = dp(finalAct, 12);

                LinearLayout fetchDelayLayout = new LinearLayout(finalAct);
                fetchDelayLayout.setOrientation(LinearLayout.VERTICAL);
                fetchDelayLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                fetchDelayLayout.addView(buildQzoneLabel(finalAct, "拉取列表后延迟（秒）", colorOnSurfaceVariant));
                EditText fetchDelayInput = makeSmallInput(finalAct, "3~15秒", colorSurface);
                fetchDelayInput.setText(String.valueOf(getInt("qzone_cfg", "fetch_delay_ms", 5000) / 1000));
                fetchDelayInput.setTextColor(colorOnSurface);
                fetchDelayInput.setHintTextColor(colorOnSurfaceVariant);
                fetchDelayLayout.addView(fetchDelayInput);
                intervalRow.addView(fetchDelayLayout);

                Space space1 = new Space(finalAct);
                space1.setLayoutParams(new LinearLayout.LayoutParams(itemSpacing, -2));
                intervalRow.addView(space1);

                LinearLayout feedIntervalLayout = new LinearLayout(finalAct);
                feedIntervalLayout.setOrientation(LinearLayout.VERTICAL);
                feedIntervalLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                feedIntervalLayout.addView(buildQzoneLabel(finalAct, "每条说说间隔（秒）", colorOnSurfaceVariant));
                EditText feedIntervalInput = makeSmallInput(finalAct, "0.5~5秒", colorSurface);
                feedIntervalInput.setText(String.valueOf(getInt("qzone_cfg", "feed_interval_ms", 1000) / 1000));
                feedIntervalInput.setTextColor(colorOnSurface);
                feedIntervalInput.setHintTextColor(colorOnSurfaceVariant);
                feedIntervalLayout.addView(feedIntervalInput);
                intervalRow.addView(feedIntervalLayout);
                card.addView(intervalRow);

                // 黑名单设置
                card.addView(buildQzoneSectionTitle(finalAct, "黑名单设置", colorOnSurfaceVariant));
                LinearLayout blackRow = new LinearLayout(finalAct);
                blackRow.setOrientation(LinearLayout.HORIZONTAL);
                blackRow.setGravity(Gravity.CENTER_VERTICAL);
                blackRow.setPadding(0, dp(finalAct, 8), 0, dp(finalAct, 12));

                TextView blackBtn = createButton(finalAct, "设置黑名单", colorPrimary, Color.TRANSPARENT, 14f, 24, 0, 0, false, 0, 0, null);
                blackRow.addView(blackBtn, new LinearLayout.LayoutParams(-2, -2));

                Space spacer = new Space(finalAct);
                blackRow.addView(spacer, new LinearLayout.LayoutParams(0, -2, 1.0f));

                final TextView blackLabel = new TextView(finalAct);
                blackLabel.setText("已屏蔽 " + blackList.size() + " 人");
                blackLabel.setTextSize(16);
                blackLabel.setTextColor(colorOnSurface);
                blackRow.addView(blackLabel, new LinearLayout.LayoutParams(-2, -2));
                card.addView(blackRow);

                blackBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final ArrayList initSel = new ArrayList(blackList);
                        showGroupSelector(finalAct, 1, initSel, new GroupSelectCallback() {
                            public void onSelected(ArrayList selected) {
                                blackList.clear();
                                if (selected != null) blackList.addAll(selected);
                                saveBlackList();
                                blackLabel.setText("已屏蔽 " + blackList.size() + " 人");
                            }
                        });
                    }
                });

                // 底部按钮
                LinearLayout btnRow = new LinearLayout(finalAct);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.END);
                btnRow.setPadding(0, dp(finalAct, 24), 0, 0);

                TextView cancel = createButton(finalAct, "取消", colorOnSurfaceVariant, Color.TRANSPARENT, 14f, 24, 24, 0, false, 1, colorOnSurfaceVariant, null);
                LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(-2, dp(finalAct, 48));
                cancelLp.rightMargin = dp(finalAct, 12);
                btnRow.addView(cancel, cancelLp);

                TextView save = createButton(finalAct, "保存", colorSurface, colorPrimary, 14f, 24, 24, 0, false, 0, 0, null);
                btnRow.addView(save, new LinearLayout.LayoutParams(-2, dp(finalAct, 48)));
                card.addView(btnRow);

                save.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String text = commentInput.getText().toString().trim();
                        if (text.length() > 100) text = text.substring(0, 100);
                        putString("qzone_cfg", "comment", text);

                        try {
                            int sec = Integer.parseInt(fetchDelayInput.getText().toString().trim());
                            putInt("qzone_cfg", "fetch_delay_ms", sec*1000);
                        } catch (Throwable e) { putInt("qzone_cfg", "fetch_delay_ms", 5000); }

                        try {
                            int fsec = Integer.parseInt(feedIntervalInput.getText().toString().trim());
                            putInt("qzone_cfg", "feed_interval_ms", fsec*1000);
                        } catch (Throwable e) { putInt("qzone_cfg", "feed_interval_ms", 1000); }

                        Toast("已保存配置");
                        vibrate(finalAct, 40);
                        d.dismiss();
                        checkAndStartOrStopThread();
                    }
                });

                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                    }
                });

                d.setContentView(outer);
                d.getWindow().setLayout((int)(finalAct.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
                d.show();
            } catch (Throwable e) {
                Toast("弹窗创建失败: " + e.getMessage());
            }
        }
    });
}

View buildQzoneSwitchRow(Activity act, String labelText, final String keyName, int textColor) {
    LinearLayout row = new LinearLayout(act);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(act, 12), 0, dp(act, 12));

    TextView label = new TextView(act);
    label.setText(labelText);
    label.setTextSize(16);
    label.setTextColor(textColor);
    row.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));

    boolean initVal = getBoolean("qzone_cfg", keyName, false);
    View sw = createSettingsSwitchView(act, initVal, "qzone_cfg", keyName, labelText, new Runnable() {
        public void run() {
            checkAndStartOrStopThread();
        }
    });
    row.addView(sw);
    return row;
}

TextView buildQzoneSectionTitle(Activity act, String text, int color) {
    TextView tv = new TextView(act);
    tv.setText(text);
    tv.setTextSize(14);
    tv.setTextColor(color);
    tv.setPadding(0, dp(act, 16), 0, dp(act, 8));
    return tv;
}

TextView buildQzoneLabel(Activity act, String text, int color) {
    TextView tv = new TextView(act);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(color);
    tv.setPadding(0, 0, 0, dp(act, 4));
    return tv;
}

private void checkAndStartOrStopThread() {
    loadDoneTasks();
    loadBlackList();

    boolean likeOn = getBoolean("qzone_cfg", "switch_like", false);
    boolean commentOn = getBoolean("qzone_cfg", "switch_comment", false);

    if (likeOn || commentOn) {
        if (!isServiceRunning) startQzoneAutoThread();
    } else {
        stopQzoneAutoThread();
    }
}

private void startQzoneAutoThread() {
    if (isServiceRunning) return;
    isServiceRunning = true;

    qzoneThread = new Thread(new Runnable() {
        public void run() {
            while (isServiceRunning) {
                try {
                    String pskey = getPskey("qzone.qq.com");
                    String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin + ";p_skey=" + pskey;
                    String gtk = getGTK("qzone.qq.com");

                    String url = "https://h5.qzone.qq.com/webapp/json/mqzone_feeds/getActiveFeeds?g_tk=" + gtk
                            + "&res_type=0&refresh_type=1&format=json";

                    String resp = qzoneGet(url, cookie);

                    if (jsonGetInt(resp, "code") != 0) {
                        delay(20000);
                        continue;
                    }

                    ArrayList feeds = parseActiveFeeds(resp);
                    delay(getInt("qzone_cfg", "fetch_delay_ms", 5000));

                    boolean likeEnabled = getBoolean("qzone_cfg", "switch_like", false);
                    boolean commentEnabled = getBoolean("qzone_cfg", "switch_comment", false);

                    for (int i = 0; i < feeds.size(); i++) {
                        if (!isServiceRunning) break;

                        String[] item = (String[]) feeds.get(i);
                        String orgKey = item[0];
                        String curKey = item[1];
                        String userUin = item[2];
                        String ugcKey = item[3];

                        if (userUin.equals(myUin)) continue;
                        if (blackList.contains(userUin)) continue;
                        if (doneTasks.contains(curKey)) continue;

                        boolean didSomething = false;

                        if (likeEnabled) {
                            sendQzoneZan(orgKey, curKey);
                            didSomething = true;
                        }
                        if (commentEnabled) {
                            sendQzoneComment(orgKey, curKey, userUin, ugcKey);
                            didSomething = true;
                        }

                        if (didSomething) {
                            doneTasks.add(curKey);
                            saveDoneTasks();
                        }

                        int feedIntervalMs = getInt("qzone_cfg", "feed_interval_ms", 1000);
                        long sleepMs = (long) (feedIntervalMs * (0.8f + Math.random() * 0.4f));
                        delay(sleepMs);
                    }

                    delay(10000 + (long)(Math.random() * 5000));

                } catch (Throwable e) {
                    if (e instanceof InterruptedException) break;
                    delay(30000);
                }
            }
            isServiceRunning = false;
        }
    });

    ThreadPool.execute(qzoneThread);
    Toast("空间操作已启动");
}

private void stopQzoneAutoThread() {
    if (!isServiceRunning) return;
    isServiceRunning = false;
    if (qzoneThread != null && qzoneThread.isAlive()) qzoneThread.interrupt();
    qzoneThread = null;
    Toast("空间操作已停止");
}

private void sendQzoneZan(String orglikekey, String curlikekey) {
    if (!getBoolean("qzone_cfg", "switch_like", false)) return;

    String pskey = getPskey("qzone.qq.com");
    String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin + ";p_skey=" + pskey;
    String gtk = getGTK("qzone.qq.com");

    String url = "https://h5.qzone.qq.com/proxy/domain/w.qzone.qq.com/cgi-bin/likes/internal_dolike_app?g_tk=" + gtk;
    String data = "opuin=" + myUin + "&unikey=" + orglikekey + "&curkey=" + curlikekey + "&appid=311&opr_type=like&format=purejson";

    try {
        String resp = httppost1(url, cookie, data);
        if (jsonGetInt(resp, "ret") == 0) {
            traceLog("qzone_log", "点赞成功: " + curlikekey);
        } else {
            traceLog("qzone_log", "点赞失败: " + curlikekey);
        }
    } catch (Throwable e) {}
}

private void sendQzoneComment(String orglikekey, String curlikekey, String userUin, String ugckey) {
    if (!getBoolean("qzone_cfg", "switch_comment", false)) return;

    String content = getString("qzone_cfg", "comment", "我来暖说说啦！");
    if (content.length() > 100) content = content.substring(0, 100);

    String srcId = curlikekey;
    int lastSlash = srcId.lastIndexOf('/');
    if (lastSlash != -1 && lastSlash < srcId.length() - 1) {
        srcId = srcId.substring(lastSlash + 1);
    }

    String pskey = getPskey("qzone.qq.com");
    String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin + ";p_skey=" + pskey;
    String gtk = getGTK("qzone.qq.com");

    String url = "https://h5.qzone.qq.com/webapp/json/qzoneOperation/addComment?g_tk=" + gtk;

    String body = "{\"appid\":311,\"uin\":" + myUin
            + ",\"ownuin\":\"" + userUin
            + "\",\"srcId\":\"" + srcId
            + "\",\"content\":\"" + content.replace("\"", "\\\"")
            + "\",\"isPrivateComment\":0,\"busi_param\":{},\"bypass_param\":{}}";

    traceLog("qzone_log", "准备评论 → 用户:" + userUin + " srcId:" + srcId + " 内容:" + content);

    try {
        String resp = httppost1(url, cookie, body);
        int ret = jsonGetInt(resp, "ret");
        if (ret == 0) {
            traceLog("qzone_log", "评论成功: " + userUin);
        } else {
            traceLog("qzone_log", "评论失败: " + userUin + " ret=" + ret + " resp=" + resp);
        }
    } catch (Throwable e) {
        traceLog("qzone_log", "评论异常: " + userUin + " " + e.getMessage());
    }
}
checkAndStartOrStopThread();